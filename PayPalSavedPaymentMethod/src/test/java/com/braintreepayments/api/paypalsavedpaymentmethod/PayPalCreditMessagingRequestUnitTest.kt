package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalCreditMessagingRequestUnitTest {

    @Test
    fun build_withRequiredFieldsOnly_buildsExpectedBody() {
        val request = PayPalCreditMessagingRequest(amount = "55.00", currencyCode = "USD")

        val json = request.build()

        val flowContext = json.getJSONObject("flow_context")
        assertEquals("MOBILE_APP", flowContext.getString("channel"))
        assertEquals("EARLY_PRESENTMENT", flowContext.getString("flow_specifier"))
        assertEquals("CHECKOUT", flowContext.getString("page_type"))
        assertTrue(flowContext.getJSONArray("attributes").toString().contains("BRAND_BRAINTREE"))
        assertTrue(flowContext.getJSONArray("attributes").toString().contains("EXPERIENCE_ANDROID_SDK"))
        assertFalse(flowContext.has("page_uri"))
        assertFalse(flowContext.has("version"))

        val placement = json.getJSONArray("message_placements").getJSONObject(0)
        val amount = placement.getJSONObject("amount")
        assertEquals("USD", amount.getString("currency_code"))
        assertEquals("55.00", amount.getString("value"))
        val contentAttributes = placement.getJSONArray("content_attributes").toString()
        assertTrue(contentAttributes.contains("ALTERNATIVE_PREFIX_UPPERCASE_OR"))
        assertTrue(contentAttributes.contains("MESSAGE_LENGTH_COMPACT"))
        assertFalse(placement.has("offer_types"))
        assertFalse(placement.has("configuration_id"))

        assertEquals("en_US", json.getString("locale"))
    }

    @Test
    fun build_withOptionalFields_includesThemInBody() {
        val request = PayPalCreditMessagingRequest(
            amount = "100.00",
            currencyCode = "USD",
            locale = "en_GB",
            pageType = "PRODUCT",
            pageUri = "https://merchant.example/product/1",
            clientVersion = "1.2.3",
            offerTypes = listOf("PAY_LATER_LONG_TERM"),
            configurationId = "config-1"
        )

        val json = request.build()

        val flowContext = json.getJSONObject("flow_context")
        assertEquals("PRODUCT", flowContext.getString("page_type"))
        assertEquals("https://merchant.example/product/1", flowContext.getString("page_uri"))
        assertEquals("1.2.3", flowContext.getString("version"))

        val placement = json.getJSONArray("message_placements").getJSONObject(0)
        assertTrue(placement.getJSONArray("offer_types").toString().contains("PAY_LATER_LONG_TERM"))
        assertEquals("config-1", placement.getString("configuration_id"))

        assertEquals("en_GB", json.getString("locale"))
    }
}
