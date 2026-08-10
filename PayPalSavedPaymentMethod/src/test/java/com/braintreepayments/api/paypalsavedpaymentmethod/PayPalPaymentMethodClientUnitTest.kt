package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalBetaApi::class)
@RunWith(RobolectricTestRunner::class)
class PayPalPaymentMethodClientUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val payPalClient = mockk<PayPalClient>(relaxed = true)

    private fun mockConfiguration(env: String = "production"): Configuration =
        mockk(relaxed = true) {
            every { environment } returns env
        }

    @Test
    fun fetchFI_withJwt_postsStickyFiBodyAndReturnsSuccess() = runTest(testDispatcher) {
        val responseJson = """{"data":{"paypalFundingInstrumentDetails":{"payer":null,"paymentMethods":[]}}}"""
        val bodySlot = slot<JSONObject>()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(capture(bodySlot)) } returns responseJson

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("pmid-jwt")

        val input = bodySlot.captured.getJSONObject("variables").getJSONObject("input")
        assertEquals("STICKY_FI", input.getString("fetchPaymentMethodType"))
        assertEquals("pmid-jwt", input.getString("paymentMethodIdJwt"))
        assertTrue(result is PayPalPaymentMethodSummaryResult.Success)
    }

    @Test
    fun fetchFI_whenJwtMissing_returnsFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("")

        assertTrue(result is PayPalPaymentMethodSummaryResult.Failure)
        val error = (result as PayPalPaymentMethodSummaryResult.Failure).error
        assertTrue(error is PayPalPaymentMethodSummaryException)
        assertEquals(
            PayPalPaymentMethodSummaryException.MISSING_PAYMENT_METHOD_ID_JWT,
            error.message
        )
    }

    @Test
    fun refetchFI_postsApprovedCheckoutBodyAndReturnsSuccess() = runTest(testDispatcher) {
        val responseJson = """{"data":{"paypalFundingInstrumentDetails":{"payer":null,"paymentMethods":[]}}}"""
        val bodySlot = slot<JSONObject>()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(capture(bodySlot)) } returns responseJson

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.refetchFI("order-123")

        val input = bodySlot.captured.getJSONObject("variables").getJSONObject("input")
        assertEquals("FI_FROM_APPROVED_CHECKOUT", input.getString("fetchPaymentMethodType"))
        assertEquals("order-123", input.getString("orderId"))
        assertTrue(result is PayPalPaymentMethodSummaryResult.Success)
    }

    @Test
    fun fetchFI_whenGraphQLReturnsErrors_returnsFailurePreservingErrorClass() = runTest(testDispatcher) {
        val responseJson = """
            {"errors":[{"message":"PayPal access token not found for merchant account.",
              "extensions":{"errorClass":"AUTHENTICATION","errorType":"developer_error"}}],
             "data":{"paypalFundingInstrumentDetails":null}}
        """.trimIndent()
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(any()) } returns responseJson

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("pmid-jwt")

        assertTrue(result is PayPalPaymentMethodSummaryResult.Failure)
        val error = (result as PayPalPaymentMethodSummaryResult.Failure).error
        assertTrue(error is PayPalPaymentMethodSummaryException)
        assertEquals("AUTHENTICATION", (error as PayPalPaymentMethodSummaryException).errorClass)
    }

    @Test
    fun fetchFI_whenNetworkError_returnsFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        coEvery { braintreeClient.sendGraphQLPOST(any()) } throws IOException("network down")

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)

        val result = sut.fetchFI("pmid-jwt")

        assertTrue(result is PayPalPaymentMethodSummaryResult.Failure)
        assertTrue((result as PayPalPaymentMethodSummaryResult.Failure).error is IOException)
    }

    @Test
    fun fetchCreditPresentmentMessages_postsRequestBodyToCreditUrlAndReturnsSuccess() =
        runTest(testDispatcher) {
            val responseJson = """
                {"messages":[{"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ",
                  "content":{"main_items":[{"type":"TEXT","text":"As low as \${'$'}10/mo"}],
                  "action_items":[{"type":"LINK","text":"Learn more","click_url":"https://paypal.com/learn"}]},
                  "analytics":{"impression_url":"https://paypal.com/impression"}},
                  "selection_reasons":[{"code":"DEFAULT_PREFERRED","description":"default"}]}]}
            """.trimIndent()
            val urlSlot = slot<String>()
            val bodySlot = slot<String>()
            val braintreeClient = MockkBraintreeClientBuilder()
                .configurationSuccess(mockConfiguration())
                .build()
            coEvery {
                braintreeClient.sendPOST(url = capture(urlSlot), data = capture(bodySlot))
            } returns responseJson

            val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)
            val request = PayPalCreditMessagingRequest(amount = "55.00", currencyCode = "USD")

            val result = sut.fetchCreditPresentmentMessages(request)

            assertEquals(
                "https://api.paypal.com/v2/credit/fetch-presentment-messages",
                urlSlot.captured
            )
            assertEquals(request.build().toString(), bodySlot.captured)
            assertTrue(result is PayPalCreditMessagingResult.Success)
            val message = (result as PayPalCreditMessagingResult.Success).message
            assertEquals("msg-1", message.id)
            assertEquals("PLLT_MQ_GZ", message.type)
            assertEquals("Learn more", message.actionItems.first().text)
            assertEquals("https://paypal.com/impression", message.impressionUrl)
            assertEquals("DEFAULT_PREFERRED", message.selectionReasons.first().code)
        }

    @Test
    fun fetchCreditPresentmentMessages_whenEnvironmentIsNotProduction_postsToSandboxUrl() =
        runTest(testDispatcher) {
            val responseJson = """{"messages":[{"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ"}}]}"""
            val urlSlot = slot<String>()
            val braintreeClient = MockkBraintreeClientBuilder()
                .configurationSuccess(mockConfiguration(env = "sandbox"))
                .build()
            coEvery {
                braintreeClient.sendPOST(url = capture(urlSlot), data = any())
            } returns responseJson

            val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)
            val request = PayPalCreditMessagingRequest(amount = "55.00", currencyCode = "USD")

            sut.fetchCreditPresentmentMessages(request)

            assertEquals(
                "https://api.sandbox.paypal.com/v2/credit/fetch-presentment-messages",
                urlSlot.captured
            )
        }

    @Test
    fun fetchCreditPresentmentMessages_whenNoPreferredMessage_returnsFailure() = runTest(testDispatcher) {
        val responseJson = """{"messages":[{}]}"""
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(mockConfiguration())
            .build()
        coEvery { braintreeClient.sendPOST(url = any(), data = any()) } returns responseJson

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)
        val request = PayPalCreditMessagingRequest(amount = "55.00", currencyCode = "USD")

        val result = sut.fetchCreditPresentmentMessages(request)

        assertTrue(result is PayPalCreditMessagingResult.Failure)
        val error = (result as PayPalCreditMessagingResult.Failure).error
        assertEquals("No preferred_message returned", error.message)
    }

    @Test
    fun fetchCreditPresentmentMessages_whenNetworkError_returnsFailure() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(mockConfiguration())
            .build()
        coEvery { braintreeClient.sendPOST(url = any(), data = any()) } throws IOException("network down")

        val sut = PayPalPaymentMethodClient(braintreeClient, payPalClient)
        val request = PayPalCreditMessagingRequest(amount = "55.00", currencyCode = "USD")

        val result = sut.fetchCreditPresentmentMessages(request)

        assertTrue(result is PayPalCreditMessagingResult.Failure)
        val error = (result as PayPalCreditMessagingResult.Failure).error
        assertEquals("network down", error.message)
    }
}
