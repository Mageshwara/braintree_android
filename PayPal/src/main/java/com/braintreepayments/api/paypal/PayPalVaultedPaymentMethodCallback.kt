package com.braintreepayments.api.paypal

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving the result of [PayPalClient.fetchFI] / [PayPalClient.refetchFI].
 */
@ExperimentalBetaApi
fun interface PayPalVaultedPaymentMethodCallback {

    /**
     * @param result a success or failure result from the vaulted payment method fetch
     */
    fun onPayPalVaultedPaymentMethodResult(result: PayPalVaultedPaymentMethodResult)
}
