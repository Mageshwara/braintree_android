package com.braintreepayments.api.uicomponents

/**
 * What the [com.braintreepayments.api.uicomponents.compose.CreditMessagingView] should render — the
 * Pay Later ("BNPL") credit-messaging row shown beneath the FI chip.
 *
 * The copy is server-driven (passthrough from the CFS upstream messaging product), so it is passed
 * in as data rather than composed from SDK strings. The owning component (a later pass) fetches /
 * resolves the messaging and maps it into this state.
 */
sealed interface CreditMessagingDisplayState {

    /**
     * No pay-later messaging is shown — e.g. Pay in Full with no BNPL, a second BNPL radio already
     * present, or a non-BNPL / non-transaction context (design: "No Messaging" variants).
     */
    data object Hidden : CreditMessagingDisplayState

    /**
     * Pay-later messaging is shown as a `<message> <link>` row (design "approach B" /
     * "BEFORE SELECTION"): e.g. message "Want more time to pay?" + link "Pay Later options", or
     * message "Or pay in 4 interest-free payments of $324.50." + link "Learn more".
     *
     * @property message   the leading prompt / messaging copy (server-driven).
     * @property linkLabel the trailing tappable link label (server-driven).
     */
    data class Content(val message: String, val linkLabel: String) : CreditMessagingDisplayState
}
