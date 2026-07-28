package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class CreditMessagingRequestUnitTest {

    @Test
    fun `build adds compact-or content_attributes`() {
        val request = CreditMessagingRequest(
            amount = "55.00",
            currencyCode = "USD"
        )

        val json = request.build()
        val messagePlacement = json.getJSONArray("message_placements").getJSONObject(0)

        assertTrue(messagePlacement.has("content_attributes"))
        val contentAttributes = messagePlacement.getJSONArray("content_attributes")
        assertEquals("ALTERNATIVE_PREFIX_UPPERCASE_OR", contentAttributes.getString(0))
        assertEquals("MESSAGE_LENGTH_COMPACT", contentAttributes.getString(1))
        assertEquals("USD", messagePlacement.getJSONObject("amount").getString("currency_code"))
        assertEquals("55.00", messagePlacement.getJSONObject("amount").getString("value"))
        assertEquals("en_US", json.getString("locale"))

        val flowContext = json.getJSONObject("flow_context")
        assertEquals("MOBILE_APP", flowContext.getString("channel"))
        assertEquals("EARLY_PRESENTMENT", flowContext.getString("flow_specifier"))
        assertEquals("CHECKOUT", flowContext.getString("page_type"))
        assertEquals("BRAND_BRAINTREE", flowContext.getJSONArray("attributes").getString(0))
        assertEquals("EXPERIENCE_ANDROID_SDK", flowContext.getJSONArray("attributes").getString(1))
    }

    @Test
    fun `build includes optional page_uri and version only when provided`() {
        val withoutOptionals = CreditMessagingRequest(
            amount = "55.00",
            currencyCode = "USD"
        ).build().getJSONObject("flow_context")
        assertFalse(withoutOptionals.has("page_uri"))
        assertFalse(withoutOptionals.has("version"))

        val withOptionals = CreditMessagingRequest(
            amount = "55.00",
            currencyCode = "USD",
            pageUri = "https://merchant.com/checkout",
            clientVersion = "6.21.0"
        ).build().getJSONObject("flow_context")
        assertEquals("https://merchant.com/checkout", withOptionals.getString("page_uri"))
        assertEquals("6.21.0", withOptionals.getString("version"))
    }

    @Test
    fun `build includes optional offer_types and configuration_id only when provided`() {
        val withoutOptionals = CreditMessagingRequest(
            amount = "55.00",
            currencyCode = "USD"
        ).build().getJSONArray("message_placements").getJSONObject(0)
        assertFalse(withoutOptionals.has("offer_types"))
        assertFalse(withoutOptionals.has("configuration_id"))

        val withOptionals = CreditMessagingRequest(
            amount = "55.00",
            currencyCode = "USD",
            offerTypes = listOf("PAY_LATER_LONG_TERM", "PAY_LATER_SHORT_TERM"),
            configurationId = "DEFAULT"
        ).build().getJSONArray("message_placements").getJSONObject(0)
        val offerTypes = withOptionals.getJSONArray("offer_types")
        assertEquals("PAY_LATER_LONG_TERM", offerTypes.getString(0))
        assertEquals("PAY_LATER_SHORT_TERM", offerTypes.getString(1))
        assertEquals("DEFAULT", withOptionals.getString("configuration_id"))
    }
}
