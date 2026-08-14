package com.braintreepayments.api.paypalsavedpaymentmethod.callback

import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult

/**
 * Callback supplied to `PayPalSavedPaymentMethodView.initialize` for the entire lifetime of the
 * edit-FI flow started by the view's edit pencil, analogous to
 * [com.braintreepayments.api.uicomponents.PayPalLaunchCallback] +
 * `PayPalTokenizeCallback` for `PayPalButton`, merged into one listener since the view's public
 * API only wires a single callback at [initialize] time.
 *
 * A merchant that needs to survive a process kill mid-flow should store
 * [PayPalPendingRequest.Started.pendingRequestString] from [onSavedPaymentMethodLaunch] and resume
 * it via `PayPalSavedPaymentMethodView.handleReturnToApp`, whose outcome is then delivered to
 * [onSavedPaymentMethodResult].
 *
 * // TODO: Phase 2 -- in this Phase 1 build, [onSavedPaymentMethodLaunch] fires with a stub
 * [PayPalPendingRequest.Started] value at edit-tap time, not a real launch result from
 * `PayPalLauncher.launch`, and [onSavedPaymentMethodResult] never delivers
 * [PayPalResult.Success] since fabricating a real `PayPalAccountNonce` requires Phase 2's real
 * tokenize call. The signatures and merchant-facing contract are real; the values behind them are
 * not until Phase 2 wires the actual `create_payment_resource` + app-switch/browser launch +
 * tokenize.
 */
interface PayPalSavedPaymentMethodLaunchCallback {

    /**
     * @param payPalPendingRequest a request used to launch the edit-FI PayPal payment
     * authorization flow, or a [PayPalPendingRequest.Failure] if the launch could not start.
     */
    fun onSavedPaymentMethodLaunch(payPalPendingRequest: PayPalPendingRequest)

    /**
     * @param result the edit-FI flow's final outcome: a new funding instrument tokenized, the
     * buyer cancelled, or the flow failed.
     */
    fun onSavedPaymentMethodResult(result: PayPalResult)
}
