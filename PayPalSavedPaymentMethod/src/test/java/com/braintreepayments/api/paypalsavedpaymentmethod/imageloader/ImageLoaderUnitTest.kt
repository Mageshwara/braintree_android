package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ImageLoaderUnitTest {

    private lateinit var server: MockWebServer
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        val localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(localhostCertificate.certificate)
            .build()

        server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()

        okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun load_returnsDecodedBitmap() = runTest {
        server.enqueue(MockResponse().setBody(Buffer().write(MINIMAL_PNG_BYTES)))
        val loader = newLoader(StandardTestDispatcher(testScheduler))

        val bitmap = loader.load(server.url("/icon.png").toString())

        assertEquals(1, bitmap.width)
        assertEquals(1, bitmap.height)
    }

    @Test
    fun load_cachesResultInMemory_secondCallDoesNotHitNetwork() = runTest {
        server.enqueue(MockResponse().setBody(Buffer().write(MINIMAL_PNG_BYTES)))
        val loader = newLoader(StandardTestDispatcher(testScheduler))
        val url = server.url("/icon.png").toString()

        val first = loader.load(url)
        val second = loader.load(url)

        assertSame(first, second)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun load_deduplicatesConcurrentRequestsForSameUrl() = runBlocking(Dispatchers.Default) {
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(MINIMAL_PNG_BYTES))
                .setBodyDelay(200, TimeUnit.MILLISECONDS)
        )
        val loader = newLoader(Dispatchers.IO)
        val url = server.url("/icon.png").toString()

        val first = async { loader.load(url) }
        val second = async { loader.load(url) }
        val results = awaitAll(first, second)

        assertSame(results[0], results[1])
        assertEquals(1, server.requestCount)
    }

    @Test
    fun load_onHttpError_throwsIOException() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val loader = newLoader(StandardTestDispatcher(testScheduler))

        assertFailsWith<IOException> { loader.load(server.url("/icon.png").toString()) }
    }

    @Test
    fun load_onNonHttpsUrl_throwsIllegalArgumentException() = runTest {
        val loader = newLoader(StandardTestDispatcher(testScheduler))

        assertFailsWith<IllegalArgumentException> { loader.load("http://example.com/icon.png") }
    }

    @Test
    fun load_downsamplesLargeImageToMaxDecodeDimension() = runTest {
        val largeBitmap = Bitmap.createBitmap(
            LARGE_IMAGE_DIMENSION_PX,
            LARGE_IMAGE_DIMENSION_PX,
            Bitmap.Config.ARGB_8888
        )
        val largePngBytes = ByteArrayOutputStream().use { stream ->
            largeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
        server.enqueue(MockResponse().setBody(Buffer().write(largePngBytes)))
        val loader = newLoader(StandardTestDispatcher(testScheduler))

        val bitmap = loader.load(server.url("/icon.png").toString())

        assertTrue(bitmap.width <= MAX_DECODE_DIMENSION_PX)
        assertTrue(bitmap.height <= MAX_DECODE_DIMENSION_PX)
    }

    @Test
    fun load_downsamplesOneMegabyteImage() = runTest {
        val pngBytes = randomNoisePngBytes(dimensionPx = ONE_MB_IMAGE_DIMENSION_PX)
        println("1MB test image: ${pngBytes.size} bytes at ${ONE_MB_IMAGE_DIMENSION_PX}x$ONE_MB_IMAGE_DIMENSION_PX")
        server.enqueue(MockResponse().setBody(Buffer().write(pngBytes)))
        val loader = newLoader(StandardTestDispatcher(testScheduler))

        val bitmap = loader.load(server.url("/icon.png").toString())

        assertTrue(bitmap.width <= MAX_DECODE_DIMENSION_PX)
        assertTrue(bitmap.height <= MAX_DECODE_DIMENSION_PX)
    }

    @Test
    fun load_downsamplesTwoMegabyteImage() = runTest {
        val pngBytes = randomNoisePngBytes(dimensionPx = TWO_MB_IMAGE_DIMENSION_PX)
        println("2MB test image: ${pngBytes.size} bytes at ${TWO_MB_IMAGE_DIMENSION_PX}x$TWO_MB_IMAGE_DIMENSION_PX")
        server.enqueue(MockResponse().setBody(Buffer().write(pngBytes)))
        val loader = newLoader(StandardTestDispatcher(testScheduler))

        val bitmap = loader.load(server.url("/icon.png").toString())

        assertTrue(bitmap.width <= MAX_DECODE_DIMENSION_PX)
        assertTrue(bitmap.height <= MAX_DECODE_DIMENSION_PX)
    }

    /**
     * A solid-color bitmap compresses to a tiny PNG regardless of pixel dimensions, so it can't
     * stand in for a real backend-served photo. Filling every pixel with random noise defeats
     * PNG's compression and produces a byte size proportional to the pixel count, close to the
     * raw ARGB_8888 size (width * height * 4 bytes).
     */
    private fun randomNoisePngBytes(dimensionPx: Int): ByteArray {
        val random = Random(dimensionPx)
        val pixels = IntArray(dimensionPx * dimensionPx) { random.nextInt() }
        val bitmap = Bitmap.createBitmap(pixels, dimensionPx, dimensionPx, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }

    private fun newLoader(dispatcher: CoroutineDispatcher) = ImageLoader(
        okHttpClient = okHttpClient,
        ioDispatcher = dispatcher,
        defaultDispatcher = dispatcher,
        mainDispatcher = dispatcher,
        memoryCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        },
        decodeSemaphore = Semaphore(MAX_PARALLEL_DECODES)
    )

    private companion object {
        const val CACHE_SIZE_BYTES = 4 * 1024 * 1024
        const val MAX_PARALLEL_DECODES = 4
        const val MAX_DECODE_DIMENSION_PX = 128
        const val LARGE_IMAGE_DIMENSION_PX = 512
        const val ONE_MB_IMAGE_DIMENSION_PX = 512 // 512*512*4 bytes (ARGB_8888) = 1 MB raw
        const val TWO_MB_IMAGE_DIMENSION_PX = 724 // 724*724*4 bytes (ARGB_8888) ~= 2 MB raw

        // A minimal valid 1x1 transparent PNG.
        val MINIMAL_PNG_BYTES = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x04, 0x00, 0x00, 0x00, 0xB5.toByte(), 0x1C, 0x0C,
            0x02, 0x00, 0x00, 0x00, 0x0B, 0x49, 0x44, 0x41,
            0x54, 0x78, 0xDA.toByte(), 0x63, 0x64, 0xF8.toByte(), 0x0F, 0x00,
            0x01, 0x05, 0x01, 0x01, 0x27, 0x18, 0xE3.toByte(), 0x66,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
    }
}
