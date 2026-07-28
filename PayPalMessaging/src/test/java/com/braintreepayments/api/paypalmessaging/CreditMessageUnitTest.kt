package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class CreditMessageUnitTest {

    @Test
    fun `fromJson parses main_items name and selection_reasons`() {
        val json = JSONObject(
            """
            {
              "preferred_message": {
                "id": "1d8435e1-61ee-4585-8b05-a989de205c3e",
                "type": "PLST_SQ",
                "analytics": { "impression_url": "https://example.com/impression" },
                "content": {
                  "main_items": [
                    { "type": "TEXT", "text": "Or " },
                    {
                      "type": "TEXT",
                      "name": "periodic_payment_count",
                      "text": "4 interest-free payments of ${'$'}13.75 with "
                    },
                    {
                      "type": "IMAGE",
                      "name": "paypal_logo",
                      "source_url": "https://example.com/paypal_badge_inline.svg",
                      "alternative_text": "PayPal"
                    }
                  ],
                  "action_items": [
                    { "type": "LINK", "text": "Learn more", "click_url": "https://example.com/learn-more", "embeddable": true }
                  ]
                }
              },
              "selection_reasons": [
                { "code": "DEFAULT_PREFERRED", "description": "The standard messages have been returned." }
              ]
            }
            """.trimIndent()
        )

        val message = CreditMessage.fromJson(json)

        assertEquals("periodic_payment_count", message.mainItems[1].name)
        assertEquals("paypal_logo", message.mainItems[2].name)

        assertEquals(1, message.selectionReasons.size)
        assertEquals("DEFAULT_PREFERRED", message.selectionReasons[0].code)
        assertEquals(
            "The standard messages have been returned.",
            message.selectionReasons[0].description
        )
    }
}