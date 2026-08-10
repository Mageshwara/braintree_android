package com.braintreepayments.api.paypalpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving the result of [PayPalPaymentMethodClient.fetchFI] /
 * [PayPalPaymentMethodClient.refetchFI].
 */
@ExperimentalBetaApi
fun interface PayPalPaymentMethodSummaryCallback {

    /**
     * @param result a success or failure result from the saved payment method fetch
     */
    fun onPayPalPaymentMethodSummaryResult(result: PayPalPaymentMethodSummaryResult)
}
