package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

/**
 * SDK-internal client used to fetch PayPal Pay Later / Credit presentment messaging for
 * Treatment A via `POST /v2/credit/fetch-presentment-messages`. Called by the EditFi component
 * on appear — merchants never call this directly.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
internal class CreditMessagingClient(
    private val braintreeClient: BraintreeClient
) {

    /**
     * Fetches a presentment message for the given [request].
     *
     * @param request [CreditMessagingRequest]
     * @return [CreditMessagingResult]
     */
    suspend fun fetchCreditMessages(request: CreditMessagingRequest): CreditMessagingResult {
        return try {
            braintreeClient.getAuthorization().bearer
                ?: throw CreditMessagingError("Credit messaging requires a client token authorization.")

            val configuration = braintreeClient.getConfiguration()
            val baseUrl = when (configuration.environment) {
                "production" -> PRODUCTION_BASE_URL
                else -> SANDBOX_BASE_URL
            }
            val url = "$baseUrl/v2/credit/fetch-presentment-messages"

            val responseBody = braintreeClient.sendPOST(
                url = url,
                data = request.build().toString()
            )

            parseResponse(responseBody)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            fetchFailure(e)
        }
    }

    private fun parseResponse(responseBody: String): CreditMessagingResult {
        val messageJson = JSONObject(responseBody)
            .optJSONArray("messages")
            ?.optJSONObject(0)
            ?.takeIf { it.has("preferred_message") }
            ?: return fetchFailure(CreditMessagingError("No preferred_message returned"))

        val message = CreditMessage.fromJson(messageJson)
        return CreditMessagingResult.Success(message)
    }

    private fun fetchFailure(error: Exception): CreditMessagingResult.Failure {
        val creditMessagingError = error as? CreditMessagingError
            ?: CreditMessagingError(error.message ?: "Credit messaging failed", error)
        return CreditMessagingResult.Failure(creditMessagingError)
    }

    companion object {
        private const val PRODUCTION_BASE_URL = "https://api.paypal.com"
        private const val SANDBOX_BASE_URL = "https://api.sandbox.paypal.com"
    }
}
