package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Drives [com.braintreepayments.api.paypalsavedpaymentmethod.compose.SavedPayPalPaymentMethodView] —
 * the caller maps a payment-summary response (or an in-flight/error condition) to one of these
 * states.
 */
@ExperimentalBetaApi
sealed interface SavedPayPalPaymentMethodDisplayState {

    /**
     * The fetch is in flight; the view renders a placeholder in place of the FI cluster.
     */
    data object Loading : SavedPayPalPaymentMethodDisplayState

    /**
     * A vaulted funding instrument was returned; the view renders its icon, label, and masked
     * digits alongside an edit affordance.
     *
     * TODO: [label]/[lastDigits]/[type] are a stand-in for the real payment-summary model
     * (e.g. PayPalSavedPaymentMethod) landing in a follow-up PR — replace this field-by-field
     * shape with that type once it exists.
     */
    data class Content(
        val label: String,
        val lastDigits: String?,
        val type: String?
    ) : SavedPayPalPaymentMethodDisplayState

    /**
     * A display-only response was returned — no vaulted instrument. The view renders the buyer's
     * email in place of the FI cluster, still with an edit affordance.
     */
    data class NoFi(val buyerEmail: String) : SavedPayPalPaymentMethodDisplayState

    /**
     * The fetch failed or returned nothing displayable; the view renders only the PayPal logo and
     * label, hiding the FI cluster and credit-messaging row.
     */
    data object Error : SavedPayPalPaymentMethodDisplayState
}
