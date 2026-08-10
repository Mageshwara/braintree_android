package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalCreditMessagingResultUnitTest {

    @Test
    fun fromJson_parsesPreferredMessageContentAndSelectionReasons() {
        val messageJson = JSONObject(
            """
            {"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ",
              "content":{
                "main_items":[
                  {"type":"TEXT","text":"As low as ${'$'}10/mo"},
                  {"type":"IMAGE","name":"paypal_logo","source_url":"https://paypal.com/logo.png",
                   "alternative_text":"PayPal"}
                ],
                "action_items":[
                  {"type":"LINK","text":"Learn more","click_url":"https://paypal.com/learn","embeddable":true}
                ]
              },
              "analytics":{"impression_url":"https://paypal.com/impression"}},
             "selection_reasons":[{"code":"DEFAULT_PREFERRED","description":"default"}]}
            """.trimIndent()
        )

        val message = PayPalCreditMessage.fromJson(messageJson)

        assertEquals("msg-1", message.id)
        assertEquals("PLLT_MQ_GZ", message.type)
        assertEquals(2, message.mainItems.size)
        assertEquals("TEXT", message.mainItems[0].type)
        assertEquals("As low as \$10/mo", message.mainItems[0].text)
        assertEquals("paypal_logo", message.mainItems[1].name)
        assertEquals("https://paypal.com/logo.png", message.mainItems[1].sourceUrl)
        assertEquals("PayPal", message.mainItems[1].alternativeText)

        assertEquals(1, message.actionItems.size)
        assertEquals("Learn more", message.actionItems[0].text)
        assertEquals("https://paypal.com/learn", message.actionItems[0].clickUrl)
        assertTrue(message.actionItems[0].embeddable)

        assertEquals("https://paypal.com/impression", message.impressionUrl)
        assertEquals(1, message.selectionReasons.size)
        assertEquals("DEFAULT_PREFERRED", message.selectionReasons[0].code)
        assertEquals("default", message.selectionReasons[0].description)
    }

    @Test
    fun fromJson_withMissingOptionalFields_returnsDefaults() {
        val messageJson = JSONObject(
            """{"preferred_message":{"id":"msg-2","type":"PLLT_MQ_GZ"}}"""
        )

        val message = PayPalCreditMessage.fromJson(messageJson)

        assertEquals("msg-2", message.id)
        assertTrue(message.mainItems.isEmpty())
        assertTrue(message.actionItems.isEmpty())
        assertEquals(null, message.impressionUrl)
        assertTrue(message.selectionReasons.isEmpty())
    }
}
