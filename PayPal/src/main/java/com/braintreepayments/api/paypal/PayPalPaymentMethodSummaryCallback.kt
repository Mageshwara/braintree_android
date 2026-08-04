package com.braintreepayments.api.paypal

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving the result of [PayPalClient.fetchFI] / [PayPalClient.refetchFI].
 */
@ExperimentalBetaApi
fun interface PayPalPaymentMethodSummaryCallback {

    /**
     * @param result a success or failure result from the saved payment method fetch
     */
    fun onPayPalPaymentMethodSummaryResult(result: PayPalPaymentMethodSummaryResult)
}
