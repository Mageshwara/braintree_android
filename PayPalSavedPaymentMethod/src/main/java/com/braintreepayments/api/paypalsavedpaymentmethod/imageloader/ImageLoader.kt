package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal class ImageLoader internal constructor(
    private val okHttpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val memoryCache: LruCache<String, Bitmap>,
    private val decodeSemaphore: Semaphore,
) {
    private val loaderScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<Bitmap>>()
    private val jobs = Collections.synchronizedMap(WeakHashMap<ImageView, Job>())

    internal constructor() : this(
        okHttpClient = sharedOkHttpClient,
        ioDispatcher = Dispatchers.IO,
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        memoryCache = object : LruCache<String, Bitmap>(DEFAULT_CACHE_SIZE_BYTES) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        },
        decodeSemaphore = Semaphore(DEFAULT_MAX_PARALLEL_DECODES),
    )

    /** Shared core. Returns a Bitmap. */
    suspend fun load(url: String): Bitmap {
        require(isHttpsUrl(url)) { "url must be a non-blank https URL" }
        memoryCache.get(url)?.let { return it }

        // ConcurrentHashMap#computeIfAbsent requires API 24; minSdk here is 23, so the
        // check-and-insert is done via double-checked locking instead.
        val deferred = inFlightRequests[url] ?: synchronized(inFlightRequests) {
            inFlightRequests[url] ?: loaderScope.async {
                try {
                    fetchAndDecode(url)
                } finally {
                    inFlightRequests.remove(url)
                }
            }.also { inFlightRequests[url] = it }
        }
        return deferred.await()
    }

    /** XML. Job / placeholder / cancel live in the loader. */
    fun load(
        url: String,
        imageView: ImageView,
        @DrawableRes placeholder: Int,
    ) {
        imageView.setImageResource(placeholder)
        cancel(imageView)

        if (!isHttpsUrl(url)) return

        val target = WeakReference(imageView)
        val job = loaderScope.launch(mainDispatcher) {
            try {
                val bitmap = load(url)
                val view = target.get() ?: return@launch
                view.setImageBitmap(bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // leave placeholder
            }
        }
        jobs[imageView] = job

        imageView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                cancel(imageView)
                imageView.removeOnAttachStateChangeListener(this)
            }
        })
    }

    fun cancel(imageView: ImageView) {
        jobs.remove(imageView)?.cancel()
    }

    private suspend fun fetchAndDecode(url: String): Bitmap {
        val bytes = withContext(ioDispatcher) { fetchBytes(url) }
        val bitmap = withContext(defaultDispatcher) {
            decodeSemaphore.withPermit { decodeBitmap(bytes, url) }
        }
        memoryCache.put(url, bitmap)
        return bitmap
    }

    private fun fetchBytes(url: String): ByteArray {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response while loading image from $url")
            }
            return response.body?.bytes()
                ?: throw IOException("Empty response body while loading image from $url")
        }
    }

    /**
     * Two-pass decode: read the image bounds first (no bitmap allocated), then decode a
     * subsampled bitmap close to [MAX_DECODE_DIMENSION_PX] so a large backend-served icon
     * doesn't waste RAM or blow the memory cache budget.
     * See https://developer.android.com/topic/performance/graphics/load-bitmap
     */
    private fun decodeBitmap(bytes: ByteArray, url: String): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, MAX_DECODE_DIMENSION_PX, MAX_DECODE_DIMENSION_PX)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IOException("Unable to decode image from $url")
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        while (height / inSampleSize > reqHeight || width / inSampleSize > reqWidth) {
            inSampleSize *= 2
        }

        return inSampleSize
    }

    companion object {
        private const val DEFAULT_CACHE_SIZE_BYTES = 4 * 1024 * 1024
        private const val DEFAULT_MAX_PARALLEL_DECODES = 4
        private const val MAX_DECODE_DIMENSION_PX = 128

        private val sharedOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Volatile
        private var instance: ImageLoader? = null

        fun getInstance(): ImageLoader =
            instance ?: synchronized(this) {
                instance ?: ImageLoader().also { instance = it }
            }

        fun isHttpsUrl(url: String): Boolean =
            url.isNotBlank() && url.startsWith("https://")
    }
}
