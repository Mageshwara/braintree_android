package com.braintreepayments.api.paypalpaymentmethod.model

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalpaymentmethod.PayPalPaymentMethod

/**
 * Drives [com.braintreepayments.api.paypalpaymentmethod.compose.SavedPayPalPaymentMethodView] —
 * the caller maps a [com.braintreepayments.api.paypalpaymentmethod.PayPalPaymentMethodSummary] (or
 * an in-flight/error condition) to one of these states.
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
     */
    data class Content(val paymentMethod: PayPalPaymentMethod) : SavedPayPalPaymentMethodDisplayState

    /**
     * A display-only response was returned — no vaulted instrument. The view renders the buyer's
     * email in place of the FI cluster, still with an edit affordance.
     */
    data class NoFi(val buyerEmail: String) : SavedPayPalPaymentMethodDisplayState

    /**
     * The fetch failed or returned nothing displayable; the view renders nothing.
     */
    data object Error : SavedPayPalPaymentMethodDisplayState
}