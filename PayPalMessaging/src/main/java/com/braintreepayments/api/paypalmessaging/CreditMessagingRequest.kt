package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * Used to request PayPal Pay Later / Credit presentment messaging from
 * `POST /v2/credit/fetch-presentment-messages`.
 *
 * Always requests Treatment A copy directly via `content_attributes`; there is no arm
 * resolution at the network layer.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property amount Order amount driving the messaging copy (e.g. "55.00").
 * @property currencyCode ISO currency code for [amount] (e.g. "USD").
 * @property locale Buyer locale, formatted `xx_XX` (e.g. "en_US").
 * @property pageType The page the message is rendered on. Defaults to "CHECKOUT".
 * @property pageUri Optional URI of the page the message is rendered on.
 * @property clientVersion Optional client-version signal sent on `flow_context.version`.
 * @property offerTypes Optional filter on offer categories.
 * @property configurationId Optional merchant messaging config id. Defaults to "DEFAULT" server-side.
 */
@ExperimentalBetaApi
data class CreditMessagingRequest(
    val amount: String,
    val currencyCode: String,
    val locale: String = "en_US",
    val pageType: String = "CHECKOUT",
    val pageUri: String? = null,
    val clientVersion: String? = null,
    val offerTypes: List<String>? = null,
    val configurationId: String? = null
) {

    /**
     * @suppress
     */
    fun build(): JSONObject {
        val flowContext = JSONObject()
            .put("channel", "MOBILE_APP")
            .put("flow_specifier", "EARLY_PRESENTMENT")
            .put("attributes", JSONArray(listOf("BRAND_BRAINTREE", "EXPERIENCE_ANDROID_SDK")))
            .put("page_type", pageType)
        pageUri?.let { flowContext.put("page_uri", it) }
        clientVersion?.let { flowContext.put("version", it) }

        val amountJson = JSONObject()
            .put("currency_code", currencyCode)
            .put("value", amount)

        val messagePlacement = JSONObject().put("amount", amountJson)
        messagePlacement.put(
            "content_attributes",
            JSONArray(listOf("ALTERNATIVE_PREFIX_UPPERCASE_OR", "MESSAGE_LENGTH_COMPACT"))
        )
        offerTypes?.let { messagePlacement.put("offer_types", JSONArray(it)) }
        configurationId?.let { messagePlacement.put("configuration_id", it) }

        return JSONObject()
            .put("flow_context", flowContext)
            .put("message_placements", JSONArray().put(messagePlacement))
            .put("locale", locale)
    }
}