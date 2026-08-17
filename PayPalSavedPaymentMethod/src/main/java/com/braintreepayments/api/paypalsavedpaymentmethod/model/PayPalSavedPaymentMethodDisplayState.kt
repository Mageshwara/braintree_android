package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedpaymentMethod

@ExperimentalBetaApi
sealed interface PayPalSavedPaymentMethodDisplayState {
    data object Loading : PayPalSavedPaymentMethodDisplayState
    data class Content(val paymentMethod: PayPalSavedpaymentMethod) : PayPalSavedPaymentMethodDisplayState
    data class NoFi(val buyerEmail: String) : PayPalSavedPaymentMethodDisplayState

    /**
     * No network connection to read the FI. The FI row is hidden, but the PayPal brand mark
     * still renders.
     */
    data object NoNetwork : PayPalSavedPaymentMethodDisplayState
    data object Error : PayPalSavedPaymentMethodDisplayState
}
