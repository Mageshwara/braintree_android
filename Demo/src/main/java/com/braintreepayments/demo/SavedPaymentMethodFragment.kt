package com.braintreepayments.demo

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.callback.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.component.PayPalSavedPaymentMethodView
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle

/**
 * Demonstrates [PayPalSavedPaymentMethodView] wired the same way [UIComponentsFragment] wires
 * `PayPalButton`/`VenmoButton`: one `initialize()` call per view instance, no merchant-triggered
 * refresh method other than calling `initialize()` again.
 *
 * Two instances are shown side by side to cover both style-configuration paths from M3/M3.1:
 * one left at its default style and then overridden via [PayPalSavedPaymentMethodView.setStyle],
 * the other styled entirely through XML attrs declared in `fragment_saved_payment_method.xml`.
 *
 * Scenario-switching (forcing the mock client's FI/credit-messaging/edit-flow outcome) is not
 * exposed here: `PayPalSavedPaymentMethodClient` is `internal` to the `PayPalSavedPaymentMethod`
 * module, so this separate `Demo` module cannot reach it. The refresh button below only
 * re-triggers a fetch of the mock's default scenario via a second `initialize()` call -- it does
 * not pick a different mock outcome.
 */
class SavedPaymentMethodFragment : BaseFragment() {

    private lateinit var kotlinStyleView: PayPalSavedPaymentMethodView
    private lateinit var xmlStyleView: PayPalSavedPaymentMethodView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_saved_payment_method, container, false)
        kotlinStyleView = view.findViewById(R.id.saved_payment_method_view_kotlin_style)
        xmlStyleView = view.findViewById(R.id.saved_payment_method_view_xml_style)

        kotlinStyleView.setStyle(
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = ComponentAppearance(
                    backgroundColor = Color.parseColor("#E8F0FE"),
                    textColor = Color.parseColor("#1A3E7A")
                ),
                container = ContainerStyle(
                    cornerRadiusDp = 12f,
                    borderColor = Color.parseColor("#90CAF9"),
                    borderWidthDp = 1f,
                    horizontalPaddingDp = 16f,
                    verticalPaddingDp = 12f,
                    logo = PayPalLogoStyle(widthDp = 56f)
                )
            )
        )

        initializeSavedPaymentMethodViews()

        view.findViewById<Button>(R.id.saved_payment_method_refresh_button).setOnClickListener {
            initializeSavedPaymentMethodViews()
        }

        return view
    }

    private fun initializeSavedPaymentMethodViews() {
        val payPalRequest = PayPalRequestFactory.createPayPalCheckoutRequest(
            /* context = */ requireContext(),
            /* amount = */ "10.0",
            /* buyerEmailAddress = */ null,
            /* buyerPhoneCountryCode = */ null,
            /* buyerPhoneNationalNumber = */ null,
            /* isContactInformationEnabled = */ false,
            /* shopperInsightsSessionId = */ null,
            /* offerPayLater = */ false,
            /* offerCredit = */ false,
            /* isAmountBreakdownEnabled = */ false
        )

        kotlinStyleView.initialize(getAuthStringArg(), payPalRequest, savedPaymentMethodLaunchCallback())
        xmlStyleView.initialize(getAuthStringArg(), payPalRequest, savedPaymentMethodLaunchCallback())
    }

    private fun savedPaymentMethodLaunchCallback() = object : PayPalSavedPaymentMethodLaunchCallback {
        override fun onSavedPaymentMethodLaunch(payPalPendingRequest: PayPalPendingRequest) {
            if (payPalPendingRequest is PayPalPendingRequest.Failure) {
                handleError(payPalPendingRequest.error)
            }
        }

        override fun onSavedPaymentMethodResult(result: PayPalResult) {
            when (result) {
                is PayPalResult.Success -> {
                    onPaymentMethodNonceCreated(result.nonce)
                    val action = SavedPaymentMethodFragmentDirections
                        .actionSavedPaymentMethodFragmentToDisplayNonceFragment(result.nonce)
                    NavHostFragment.findNavController(this@SavedPaymentMethodFragment).navigate(action)
                }
                is PayPalResult.Cancel -> handleError(Exception("User did not complete payment flow"))
                is PayPalResult.Failure -> handleError(result.error)
            }
        }
    }
}
