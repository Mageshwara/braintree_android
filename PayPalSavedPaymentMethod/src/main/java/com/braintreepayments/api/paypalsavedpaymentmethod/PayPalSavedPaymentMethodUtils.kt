package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * UI-ready Pay Later / credit-messaging content, derived from a [PayPalCreditMessagingResult] via
 * [toCreditMessagingContent]. Callers render [message] followed by a tappable [learnMoreText] link
 * that opens [learnMoreUrl].
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property message concatenated copy from the result's text message blocks.
 * @property learnMoreText display text for the "Learn more" action.
 * @property learnMoreUrl URL to open when [learnMoreText] is tapped.
 */
@ExperimentalBetaApi
data class CreditMessagingContent(
    val message: String,
    val learnMoreText: String,
    val learnMoreUrl: String
)

/**
 * Builds a [PayPalCreditMessagingRequest] for a single order amount, using the default
 * [FlowContext] and Treatment A [MessagePlacement.contentAttributes] - the shape every caller of
 * [PayPalSavedPaymentMethodClient.fetchCreditPresentmentMessages] needs, so callers don't have to
 * assemble [FlowContext]/[MessagePlacement]/[Amount] themselves.
 *
 * @param currencyCode ISO currency code, e.g. "USD".
 * @param amount the order amount, e.g. "55.00".
 */
@ExperimentalBetaApi
fun creditMessagingRequestFor(currencyCode: String, amount: String): PayPalCreditMessagingRequest =
    PayPalCreditMessagingRequest(
        flowContext = FlowContext(),
        messagePlacements = listOf(MessagePlacement(amount = Amount(currencyCode = currencyCode, value = amount)))
    )

/**
 * Maps this result into UI-ready [CreditMessagingContent], or null if there's no complete
 * message + "Learn more" link to show - callers should hide the messaging row in that case.
 *
 * [MessageItem.Image] blocks are intentionally skipped when concatenating [message] - the FI row
 * already shows the payment logo, so the message is copy-only (see [MessageItem.Image]).
 */
@ExperimentalBetaApi
fun PayPalCreditMessagingResult.toCreditMessagingContent(): CreditMessagingContent? {
    val message = messageItems.filterIsInstance<MessageItem.Text>().joinToString(separator = "") { it.text }
    val learnMore = learnMoreText
    val url = learnMoreUrl
    return if (message.isBlank() || learnMore.isNullOrBlank() || url.isNullOrBlank()) {
        null
    } else {
        CreditMessagingContent(message = message, learnMoreText = learnMore, learnMoreUrl = url)
    }
}