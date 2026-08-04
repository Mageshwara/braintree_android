package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model

import com.braintreepayments.api.paypal.PayPalPaymentMethodSummary

/**
 * What the [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.SavedPayPalPaymentMethodView]
 * should render. Mapped from the FI fetch / edit result by the owning component.
 */
sealed interface SavedPayPalPaymentMethodDisplayState {

    /** Initial FI fetch is in flight — show the shimmer skeleton. */
    data object Loading : SavedPayPalPaymentMethodDisplayState

    /** An FI is available — show its brand art, masked number, and the edit pencil. */
    data class Content(val fiSummary: PayPalPaymentMethodSummary) : SavedPayPalPaymentMethodDisplayState

    /**
     * No FI could be resolved (fetch failed / no vaulted instrument) — show the no-FI fallback
     * pill: "<buyer email>" with the edit pencil, no icon, no last4.
     */
    data class NoFi(val buyerEmail: String) : SavedPayPalPaymentMethodDisplayState

    /**
     * The FI fetch failed — network error, API failure, or any other error. On a no-network load,
     * the FI chip is hidden but the PayPal brand mark and label still render.
     */
    data object Error : SavedPayPalPaymentMethodDisplayState
}
