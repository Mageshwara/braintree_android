package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummaryResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.toDisplayState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The edit-FI browser-switch launch/return orchestration for [PayPalSavedPaymentMethodView] -
 * starting the PayPal payment auth flow, completing the app switch, and handling the return
 * (tokenize + best-effort FI refresh).
 */
internal class PayPalSavedPaymentMethodLauncher(
    val payPalLauncher: PayPalLauncher,
    val pendingRequestRepository: PendingRequestRepository
)

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalBetaApi::class)
internal fun startEditFiFlow(
    context: Context,
    coroutineScope: CoroutineScope,
    payPalSavedPaymentMethodClient: PayPalSavedPaymentMethodClient,
    payPalCheckoutRequest: PayPalCheckoutRequest,
    editFiLauncher: PayPalSavedPaymentMethodLauncher,
    paypalTokenizeCallback: PayPalTokenizeCallback,
    onEditEnabledChanged: (Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    payPalSavedPaymentMethodClient.createPaymentAuthRequest(
        context = context,
        payPalRequest = payPalCheckoutRequest
    ) { paymentAuthRequest ->
        onLoadingChanged(false)
        when (paymentAuthRequest) {
            is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                val activity = context.findActivity()
                val componentActivity = activity as? ComponentActivity
                when {
                    activity == null -> {
                        onEditEnabledChanged(true)
                        paypalTokenizeCallback.onPayPalResult(
                            PayPalResult.Failure(BraintreeException(ACTIVITY_IS_NULL_MESSAGE))
                        )
                    }

                    componentActivity == null -> {
                        onEditEnabledChanged(true)
                        paypalTokenizeCallback.onPayPalResult(
                            PayPalResult.Failure(BraintreeException(PARENT_ACTIVITY_NOT_COMPONENT_ACTIVITY_MESSAGE))
                        )
                    }

                    else -> {
                        coroutineScope.launch {
                            val payPalPendingRequest = editFiLauncher.payPalLauncher.launch(
                                activity = componentActivity,
                                paymentAuthRequest = paymentAuthRequest
                            )
                            when (payPalPendingRequest) {
                                is PayPalPendingRequest.Started -> {
                                    editFiLauncher.pendingRequestRepository.storePendingRequest(
                                        payPalPendingRequest.pendingRequestString
                                    )
                                }

                                is PayPalPendingRequest.Failure -> {
                                    onEditEnabledChanged(true)
                                    paypalTokenizeCallback.onPayPalResult(
                                        PayPalResult.Failure(payPalPendingRequest.error)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is PayPalPaymentAuthRequest.Failure -> {
                onEditEnabledChanged(true)
                paypalTokenizeCallback.onPayPalResult(PayPalResult.Failure(paymentAuthRequest.error))
            }
        }
    }
}

/**
 * Completes the browser-switch return and tokenizes the PayPal account. Delivers the result to
 * [paypalTokenizeCallback] and returns it so the caller can decide when to stop showing a loading
 * indicator - independently of [refetchFiAfterEdit], which the caller runs afterward as a
 * separate step so its own (shimmer) loading state doesn't block on the tokenize result already
 * delivered.
 */
@OptIn(ExperimentalBetaApi::class)
internal suspend fun handleEditFiReturn(
    editFiLauncher: PayPalSavedPaymentMethodLauncher,
    payPalSavedPaymentMethodClient: PayPalSavedPaymentMethodClient,
    pendingRequestString: String,
    intent: Intent,
    paypalTokenizeCallback: PayPalTokenizeCallback
): PayPalResult {
    if (pendingRequestString.isEmpty()) {
        val failure = PayPalResult.Failure(
            BraintreeException(UNABLE_TO_RECOVER_PENDING_REQUEST_MESSAGE)
        )
        paypalTokenizeCallback.onPayPalResult(failure)
        return failure
    }
    val paymentAuthResult = editFiLauncher.payPalLauncher.handleReturnToApp(
        pendingRequest = PayPalPendingRequest.Started(pendingRequestString),
        intent = intent
    )

    val result = when (paymentAuthResult) {
        is PayPalPaymentAuthResult.Success -> payPalSavedPaymentMethodClient.tokenize(paymentAuthResult)
        is PayPalPaymentAuthResult.NoResult -> PayPalResult.Cancel
        is PayPalPaymentAuthResult.Failure -> PayPalResult.Failure(paymentAuthResult.error)
    }
    paypalTokenizeCallback.onPayPalResult(result)
    return result
}

/**
 * Refreshes the vaulted FI after a successful edit-FI tokenize. Best-effort - a failure here
 * doesn't affect the tokenize result already delivered by [handleEditFiReturn]; [onFiRefreshed]
 * is always invoked, falling back to [lastKnownDisplayState] on failure so the caller never gets
 * stuck on the Loading state set right before this call.
 */
@OptIn(ExperimentalBetaApi::class)
internal suspend fun refetchFiAfterEdit(
    payPalSavedPaymentMethodClient: PayPalSavedPaymentMethodClient,
    orderId: String,
    lastKnownDisplayState: PayPalSavedPaymentMethodDisplayState,
    onFiRefreshed: (PayPalSavedPaymentMethodDisplayState) -> Unit
) {
    val refreshResult = payPalSavedPaymentMethodClient.refetchFI(orderId)
    val refreshedDisplayState = if (refreshResult is PayPalSavedPaymentMethodSummaryResult.Success) {
        refreshResult.paymentMethodSummary.toDisplayState()
    } else {
        lastKnownDisplayState
    }
    onFiRefreshed(refreshedDisplayState)
}

private const val ACTIVITY_IS_NULL_MESSAGE = "Activity is null"
private const val PARENT_ACTIVITY_NOT_COMPONENT_ACTIVITY_MESSAGE =
    "Parent activity is expected to be a ComponentActivity"
private const val UNABLE_TO_RECOVER_PENDING_REQUEST_MESSAGE =
    "Unable to recover pending request. Cannot complete flow."
