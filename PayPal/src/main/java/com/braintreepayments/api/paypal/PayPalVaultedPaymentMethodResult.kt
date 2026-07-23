package com.braintreepayments.api.paypal

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Result of [PayPalClient.fetchFI] / [PayPalClient.refetchFI].
 */
@ExperimentalBetaApi
sealed class PayPalVaultedPaymentMethodResult {

    /**
     * The fetch succeeded. A No-FI read is still a [Success] with empty
     * [PayPalVaultedPaymentMethod.paymentMethods] and a null [PayPalVaultedPaymentMethod.payer].
     */
    class Success internal constructor(
        val vaultedPaymentMethod: PayPalVaultedPaymentMethod
    ) : PayPalVaultedPaymentMethodResult()

    /**
     * The fetch failed. [error] is a [PayPalVaultedPaymentMethodException] for a missing JWT or a
     * server `errors[]` response (carrying `errorClass`), or the underlying network exception.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultedPaymentMethodResult()
}
