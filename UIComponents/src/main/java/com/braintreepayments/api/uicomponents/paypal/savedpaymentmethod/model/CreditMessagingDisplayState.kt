package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model

/**
 * What the
 * [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.CreditMessagingView]
 * should render — the Pay Later ("BNPL") credit-messaging row shown beneath the FI chip.
 *
 * The copy is server-driven (the Credit Presentment API's `preferred_message` content), so it is
 * passed in as data rather than composed from SDK strings.
 */
sealed interface CreditMessagingDisplayState {

    /**
     * No pay-later messaging is shown — e.g. the fetch failed, returned no `preferred_message`, or
     * messaging is disabled via `CreditMessagingStyle.enabled`.
     */
    data object Hidden : CreditMessagingDisplayState

    /**
     * Pay-later messaging is shown as a `<messageText> <learnMoreText>` row, e.g. messageText
     * "Or pay in 4 interest-free payments of $324.50." + learnMoreText "Learn more".
     *
     * @property messageText   the leading messaging copy, assembled from the response's
     * `content.main_items` (server-driven).
     * @property learnMoreText the trailing tappable "Learn more" link label.
     */
    data class Content(val messageText: String, val learnMoreText: String) : CreditMessagingDisplayState
}
