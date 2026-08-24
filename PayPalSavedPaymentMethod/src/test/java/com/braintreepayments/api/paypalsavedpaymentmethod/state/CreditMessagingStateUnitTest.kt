package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.MessageItem
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalCreditMessagingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class CreditMessagingStateUnitTest {

    @Test
    fun `null result maps to Hidden`() {
        val result: PayPalCreditMessagingResult? = null

        assertTrue(result.toCreditMessagingState() is CreditMessagingState.Hidden)
    }

    @Test
    fun `result with blank joined message text maps to Hidden`() {
        val result = creditMessagingResult(messageItems = listOf(MessageItem.Text(text = "  ")))

        assertTrue(result.toCreditMessagingState() is CreditMessagingState.Hidden)
    }

    @Test
    fun `result with empty message items maps to Hidden`() {
        val result = creditMessagingResult(messageItems = emptyList())

        assertTrue(result.toCreditMessagingState() is CreditMessagingState.Hidden)
    }

    @Test
    fun `result with non-blank message maps to Content with message and learn-more fields`() {
        val result = creditMessagingResult(
            messageItems = listOf(MessageItem.Text(text = "Pay in 4 of \$25")),
            learnMoreText = "Learn more",
            learnMoreUrl = "https://paypal.com/pay-later"
        )

        val state = result.toCreditMessagingState()

        assertTrue(state is CreditMessagingState.Content)
        val content = (state as CreditMessagingState.Content).content
        assertEquals("Pay in 4 of \$25", content.message)
        assertEquals("Learn more", content.learnMoreText)
        assertEquals("https://paypal.com/pay-later", content.learnMoreUrl)
    }

    @Test
    fun `null learnMoreText and learnMoreUrl map to empty strings, not null`() {
        val result = creditMessagingResult(
            messageItems = listOf(MessageItem.Text(text = "Pay in 4")),
            learnMoreText = null,
            learnMoreUrl = null
        )

        val content = (result.toCreditMessagingState() as CreditMessagingState.Content).content

        assertEquals("", content.learnMoreText)
        assertEquals("", content.learnMoreUrl)
    }

    @Test
    fun `multiple text items are joined with a single space and image items are ignored`() {
        val result = creditMessagingResult(
            messageItems = listOf(
                MessageItem.Text(text = "Pay in 4"),
                MessageItem.Image(sourceUrl = "https://example.com/logo.png"),
                MessageItem.Text(text = "of \$25")
            )
        )

        val content = (result.toCreditMessagingState() as CreditMessagingState.Content).content

        assertEquals("Pay in 4 of \$25", content.message)
    }

    private fun creditMessagingResult(
        messageItems: List<MessageItem>,
        learnMoreText: String? = "Learn more",
        learnMoreUrl: String? = "https://paypal.com"
    ) = PayPalCreditMessagingResult(
        messageId = "message-id",
        messageType = "PLLT_MQ_GZ",
        messageItems = messageItems,
        learnMoreText = learnMoreText,
        learnMoreUrl = learnMoreUrl,
        impressionUrl = null
    )
}
