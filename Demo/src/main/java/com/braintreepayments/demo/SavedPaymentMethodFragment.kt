package com.braintreepayments.demo

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.net.toUri
import com.braintreepayments.api.core.ExperimentalBetaApi
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
    private lateinit var nonceSection: View
    private lateinit var nonceText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_saved_payment_method, container, false)
        kotlinStyleView = view.findViewById(R.id.saved_payment_method_view_kotlin_style)
        xmlStyleView = view.findViewById(R.id.saved_payment_method_view_xml_style)
        nonceSection = view.findViewById(R.id.saved_payment_method_nonce_section)
        nonceText = view.findViewById(R.id.saved_payment_method_nonce_text)
        view.findViewById<Button>(R.id.saved_payment_method_nonce_clear_button).setOnClickListener {
            nonceSection.visibility = View.GONE
        }


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

    override fun onResume() {
        super.onResume()
        val intent = requireActivity().intent ?: return
        //kotlinStyleView.handleReturnToApp(intent)
        xmlStyleView.handleReturnToApp(intent)
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun initializeSavedPaymentMethodViews() {
        val payPalRequest = PayPalRequestFactory.createPayPalCheckoutRequest(
            /* context = */ requireContext(),
            /* amount = */ "1.0",
            /* buyerEmailAddress = */ null,
            /* buyerPhoneCountryCode = */ null,
            /* buyerPhoneNationalNumber = */ null,
            /* isContactInformationEnabled = */ false,
            /* shopperInsightsSessionId = */ null,
            /* offerPayLater = */ false,
            /* offerCredit = */ false,
            /* isAmountBreakdownEnabled = */ false
        )
        payPalRequest.currencyCode = "USD"

        val hardCodedClientToken = "eyJ2ZXJzaW9uIjoyLCJhdXRob3JpemF0aW9uRmluZ2VycHJpbnQiOiJleUpyYVdRaU9pSXlNREU0TURReU5qRTJMWE5oYm1SaWIzZ2lMQ0pwYzNNaU9pSm9kSFJ3Y3pvdkwyRndhUzV6WVc1a1ltOTRMbUp5WVdsdWRISmxaV2RoZEdWM1lYa3VZMjl0SWl3aVlXeG5Jam9pUlZNeU5UWWlmUS5leUpsZUhBaU9qRTNPRGN5TURFNU9EZ3NJbXAwYVNJNklqTXpaamhsTnpKa0xXSTRaR0l0TkdOa05pMWhZak0wTFRReU9XTXlNakV3TldWalpTSXNJbk4xWWlJNkluSXpibnAwTmpSamRtWTFlR3Q0Y25RaUxDSnBjM01pT2lKb2RIUndjem92TDJGd2FTNXpZVzVrWW05NExtSnlZV2x1ZEhKbFpXZGhkR1YzWVhrdVkyOXRJaXdpYldWeVkyaGhiblFpT25zaWNIVmliR2xqWDJsa0lqb2ljak51ZW5RMk5HTjJaalY0YTNoeWRDSXNJblpsY21sbWVWOWpZWEprWDJKNVgyUmxabUYxYkhRaU9tWmhiSE5sTENKMlpYSnBabmxmZDJGc2JHVjBYMko1WDJSbFptRjFiSFFpT21aaGJITmxmU3dpY21sbmFIUnpJanBiSW0xaGJtRm5aVjkyWVhWc2RDSmRMQ0p6WTI5d1pTSTZXeUpDY21GcGJuUnlaV1U2Vm1GMWJIUWlMQ0pDY21GcGJuUnlaV1U2UTJ4cFpXNTBVMFJMSWwwc0ltOXdkR2x2Ym5NaU9uc2ljR0Y1Y0dGc1gyTnNhV1Z1ZEY5cFpDSTZJa0ZVZFdrd01YSkhURlJVYTFkZlFXODJaMFpuWjBoR05YVTJkMXBmWnpKWVVteENiR2xMVTI5eldteElRWEY0ZDFWRFNuZ3RWVzkyTjFsSFJEQnFXWE5mUzJkSVQwTnNhR05IVEVJeWJubGFJbjE5Lm15dktkMk1qVE9HdWR4NXg1Wk5LajFtcDJjR1owQkdEV2ItQWFxUURBa3FJYW5NRFJSVW1VdmhMcldDMDNvR3RxZkJsbzJfc0xmSGIxZmtkTlVhWGh3IiwiY29uZmlnVXJsIjoiaHR0cHM6Ly9hcGkuc2FuZGJveC5icmFpbnRyZWVnYXRld2F5LmNvbTo0NDMvbWVyY2hhbnRzL3Izbnp0NjRjdmY1eGt4cnQvY2xpZW50X2FwaS92MS9jb25maWd1cmF0aW9uIiwiZ3JhcGhRTCI6eyJ1cmwiOiJodHRwczovL3BheW1lbnRzLnNhbmRib3guYnJhaW50cmVlLWFwaS5jb20vZ3JhcGhxbCIsImRhdGUiOiIyMDE4LTA1LTA4IiwiZmVhdHVyZXMiOlsidG9rZW5pemVfY3JlZGl0X2NhcmRzIl19LCJjbGllbnRBcGlVcmwiOiJodHRwczovL2FwaS5zYW5kYm94LmJyYWludHJlZWdhdGV3YXkuY29tOjQ0My9tZXJjaGFudHMvcjNuenQ2NGN2ZjV4a3hydC9jbGllbnRfYXBpIiwiZW52aXJvbm1lbnQiOiJzYW5kYm94IiwibWVyY2hhbnRJZCI6InIzbnp0NjRjdmY1eGt4cnQiLCJhc3NldHNVcmwiOiJodHRwczovL2Fzc2V0cy5icmFpbnRyZWVnYXRld2F5LmNvbSIsImF1dGhVcmwiOiJodHRwczovL2F1dGgudmVubW8uc2FuZGJveC5icmFpbnRyZWVnYXRld2F5LmNvbSIsInZlbm1vIjoib2ZmIiwiY2hhbGxlbmdlcyI6W10sInRocmVlRFNlY3VyZUVuYWJsZWQiOnRydWUsImFuYWx5dGljcyI6eyJ1cmwiOiJodHRwczovL29yaWdpbi1hbmFseXRpY3Mtc2FuZC5zYW5kYm94LmJyYWludHJlZS1hcGkuY29tL3Izbnp0NjRjdmY1eGt4cnQifSwicGF5cGFsRW5hYmxlZCI6dHJ1ZSwicGF5cGFsIjp7ImJpbGxpbmdBZ3JlZW1lbnRzRW5hYmxlZCI6dHJ1ZSwiZW52aXJvbm1lbnROb05ldHdvcmsiOmZhbHNlLCJ1bnZldHRlZE1lcmNoYW50IjpmYWxzZSwiYWxsb3dIdHRwIjp0cnVlLCJkaXNwbGF5TmFtZSI6IlBheVBhbCIsImNsaWVudElkIjoiQVR1aTAxckdMVFRrV19BbzZnRmdnSEY1dTZ3Wl9nMlhSbEJsaUtTb3NabEhBcXh3VUNKeC1Vb3Y3WUdEMGpZc19LZ0hPQ2xoY0dMQjJueVoiLCJiYXNlVXJsIjoiaHR0cHM6Ly9hc3NldHMuYnJhaW50cmVlZ2F0ZXdheS5jb20iLCJhc3NldHNVcmwiOiJodHRwczovL2NoZWNrb3V0LnBheXBhbC5jb20iLCJkaXJlY3RCYXNlVXJsIjpudWxsLCJlbnZpcm9ubWVudCI6Im9mZmxpbmUiLCJicmFpbnRyZWVDbGllbnRJZCI6Im1hc3RlcmNsaWVudDMiLCJtZXJjaGFudEFjY291bnRJZCI6InBheXBhbCIsImN1cnJlbmN5SXNvQ29kZSI6IlVTRCJ9LCJwYXltZW50TWV0aG9kSWRKd3QiOiJleUpoYkdjaU9pSkZVekkxTmlJc0ltdHBaQ0k2SW1KMExYTmhibVF0Y0hKbFpuQnRMVGRoWlRVeE5tWWlmUS5leUpxZEdraU9pSTVNelJpT0dJeU9DMWhPRFU0TFRRME5tTXRZamczTUMwd01tUTJaakZrTnpnMk16QWlMQ0pwYzNNaU9pSm9kSFJ3Y3pvdkwzQmhlVzFsYm5SekxuTmhibVJpYjNndVluSmhhVzUwY21WbExXRndhUzVqYjIwaUxDSnpkV0lpT2lKeU0yNTZkRFkwWTNabU5YaHJlSEowSWl3aVpYaHdJam94TnpnM01qQXhPVGc0TENKd2JXbGtJam9pYm5ZeWJuRjJhak1pZlEudURaRm5zdGJMSVk3Z1pTRC0yVWdzSk9oUG5iZ0FMeWJlTFZsOElLNVljUHFLczZWbHAzdml6U2tGS3dFY2Z3TTh0a3VIVVA1Q1VwdWhfclF0NmM5NVEifQ=="
        val authString = hardCodedClientToken

        kotlinStyleView.initialize(
            this, authString, appLinkReturnUrl(), payPalRequest, savedPaymentMethodLaunchCallback(),
            deepLinkFallbackUrlScheme = "com.braintreepayments.demo.braintree"
        )
        xmlStyleView.initialize(
            this, authString, appLinkReturnUrl(), payPalRequest, savedPaymentMethodLaunchCallback(),
            deepLinkFallbackUrlScheme = "com.braintreepayments.demo.braintree"
        )
    }

    private fun appLinkReturnUrl() =
        "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri()

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
                    nonceText.text = "Nonce updated: ${result.nonce.string}"
                    nonceSection.visibility = View.VISIBLE
                }
                is PayPalResult.Cancel -> handleError(Exception("User did not complete payment flow"))
                is PayPalResult.Failure -> handleError(result.error)
            }
        }
    }
}
