package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.Amount
import com.braintreepayments.api.paypalsavedpaymentmethod.FlowContext
import com.braintreepayments.api.paypalsavedpaymentmethod.MessagePlacement
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalCreditMessagingRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.callback.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toCreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toFiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toCreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toFiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.FundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Root component: shows a returning buyer's saved PayPal funding instrument (View FI), lets them
 * change it via the edit pencil (Edit FI), and shows Pay Later credit messaging.
 *
 * TODO: full documentation pass once the design is finalized.
 */
class PayPalSavedPaymentMethodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val logoView: ImageView
    private val labelView: TextView
    private val fiSection: FiSection
    private val creditMessagingView: CreditMessagingView
    private val containerBackground = GradientDrawable()

    private var style: PayPalSavedPaymentMethodViewStyle

    /** Constructed in [initialize]. */
    internal lateinit var client: PayPalSavedPaymentMethodClient

    /** Constructed in [initialize], mirrors `PayPalButton`'s [PayPalLauncher] ownership. */
    private lateinit var payPalLauncher: PayPalLauncher
    private var pendingRequestString: String? = null

    private var callback: PayPalSavedPaymentMethodLaunchCallback? = null

    private var payPalRequest: PayPalCheckoutRequest? = null
    private var lastFiClusterState: FiClusterState = FiClusterState.Loading

    private var viewScope: CoroutineScope? = null
    private var fiFetchJob: Job? = null
    private var creditMessagingFetchJob: Job? = null
    private var editFlowJob: Job? = null

    private var fullScreenLoaderOverlay: View? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.saved_paypal_payment_method_view, this, true)

        logoView = findViewById(R.id.psp_logo)
        labelView = findViewById(R.id.psp_label)
        fiSection = findViewById(R.id.psp_fi_section)
        creditMessagingView = findViewById(R.id.psp_credit_messaging)

        fiSection.setOnEditClickListener { startEditFlow() }

        containerBackground.shape = GradientDrawable.RECTANGLE
        background = containerBackground

        style = styleFromAttrs(context, attrs, defStyleAttr)
        applyStyle(style)
    }

    /** Wires the edit-FI request/callback and starts the FI (+ credit-messaging) fetch. */
    fun initialize(
        activityResultCaller: ActivityResultCaller,
        authorization: String,
        appLinkReturnUrl: Uri,
        payPalRequest: PayPalCheckoutRequest,
        callback: PayPalSavedPaymentMethodLaunchCallback,
        deepLinkFallbackUrlScheme: String? = null
    ) {
        payPalLauncher = PayPalLauncher(activityResultCaller)
        client = PayPalSavedPaymentMethodClient(context, authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
        this.payPalRequest = payPalRequest
        this.callback = callback
        startFetches()
    }

    /** Fully replaces any style parsed from XML attrs. */
    fun setStyle(style: PayPalSavedPaymentMethodViewStyle) {
        this.style = style
        applyStyle(style)
    }

    /** Handles the return from the PayPal auth flow browser switch. Call from `onResume`/`onNewIntent`. */
    @OptIn(ExperimentalBetaApi::class)
    fun handleReturnToApp(intent: Intent) {
        val pendingRequest = pendingRequestString?.let { PayPalPendingRequest.Started(it) } ?: return
        pendingRequestString = null

        val authResult = payPalLauncher.handleReturnToApp(pendingRequest, intent)
        when (authResult) {
            is PayPalPaymentAuthResult.Success -> client.tokenize(authResult, ::onTokenizeResult)
            is PayPalPaymentAuthResult.NoResult -> onEditFlowResult(PayPalResult.Cancel)
            is PayPalPaymentAuthResult.Failure -> onEditFlowResult(PayPalResult.Failure(authResult.error))
        }
    }

    private fun onTokenizeResult(result: PayPalResult) {
        when (result) {
            is PayPalResult.Success -> onEditFlowSuccess(result)
            else -> onEditFlowResult(result)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.cancel()
        viewScope = null
        fiFetchJob = null
        creditMessagingFetchJob = null
        editFlowJob = null
        hideFullScreenLoader()
    }

    private fun scope(): CoroutineScope =
        viewScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { viewScope = it }

    /** Triggers this view's own FI + credit-messaging fetches. */
    private fun startFetches() {
        fetchFI()
        fetchCreditMessage()
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun fetchFI() {
        fiSection.setState(FiClusterState.Loading)
        fiFetchJob = scope().launch {
            val state = client.fetchFI(paymentMethodIdJwt = mockJwt()).toFiClusterState()
            lastFiClusterState = state
            fiSection.setState(state)
        }
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun fetchCreditMessage() {
        if (!style.showPayPalCreditMessaging) {
            creditMessagingView.setState(CreditMessagingState.Hidden)
            return
        }
        creditMessagingView.setState(CreditMessagingState.Loading)
        creditMessagingFetchJob = scope().launch {
            val request = payPalRequest
            //TODO-GA - revist request mapper to belong where?
            val creditRequest = PayPalCreditMessagingRequest(
                flowContext = FlowContext(),
                messagePlacements = listOf(
                    MessagePlacement(
                        amount = Amount(
                            currencyCode = request?.currencyCode.orEmpty(),
                            value = request?.amount.orEmpty()
                        )
                    )
                )
            )
            val state = client.fetchCreditPresentmentMessages(creditRequest).toCreditMessagingState()
            creditMessagingView.setState(state)
        }
    }

    /** Edit pencil tap -> auth request -> browser/app switch. Outcome arrives via [handleReturnToApp]. */
    @OptIn(ExperimentalBetaApi::class)
    private fun startEditFlow() {
        val request = payPalRequest ?: return
        val activity = findActivity() as? ComponentActivity ?: return


        showFullScreenLoader()

        client.createPaymentAuthRequest(context, request) { paymentAuthRequest ->
            when (paymentAuthRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch -> launchEditFlow(activity, paymentAuthRequest)
                is PayPalPaymentAuthRequest.Failure -> onAuthRequestFailure(paymentAuthRequest.error)
            }
        }
    }

    private fun launchEditFlow(
        activity: ComponentActivity,
        paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch
    ) {
        when (val pendingRequest = payPalLauncher.launch(activity, paymentAuthRequest)) {
            is PayPalPendingRequest.Started -> {
                pendingRequestString = pendingRequest.pendingRequestString
                callback?.onSavedPaymentMethodLaunch(pendingRequest)
            }
            is PayPalPendingRequest.Failure -> onAuthRequestFailure(pendingRequest.error)
        }
    }

    /**
     * Auth-request creation or launch failed before ever reaching the browser switch -- surfaced
     * via the launch callback (matching `PayPalButton`), not the final result callback.
     */
    private fun onAuthRequestFailure(error: Exception) {
        fiSection.setState(lastFiClusterState)
        hideFullScreenLoader()
        callback?.onSavedPaymentMethodLaunch(PayPalPendingRequest.Failure(error))
    }

    /** Refetches the FI keyed by the just-approved order id, then reports success to the merchant. */
    @OptIn(ExperimentalBetaApi::class)
    private fun onEditFlowSuccess(result: PayPalResult.Success) {
        editFlowJob = scope().launch {
            hideFullScreenLoader()
            callback?.onSavedPaymentMethodResult(result)
            val state = client.refetchFI(orderId = result.nonce.paymentId.orEmpty()).toFiClusterState()
            lastFiClusterState = state
            fiSection.setState(state)
        }
    }

    /** Cancel/failure outcome of the edit-FI flow: restore the last known FI state and notify the merchant. */
    @OptIn(ExperimentalBetaApi::class)
    private fun onEditFlowResult(result: PayPalResult) {
        editFlowJob = scope().launch {
            fiSection.setState(lastFiClusterState)
            hideFullScreenLoader()
            callback?.onSavedPaymentMethodResult(result)
        }
    }

    /** Dims/blocks the entire host screen while edit-flow async work is in flight. */
    private fun showFullScreenLoader() {
        if (fullScreenLoaderOverlay != null) return
        val decorView = findActivity()?.window?.decorView as? ViewGroup ?: return

        val overlay = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(153, 0, 0, 0))
            isClickable = true
            isFocusable = true
        }
        val spinner = ProgressBar(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        overlay.addView(spinner)

        decorView.addView(overlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        fullScreenLoaderOverlay = overlay
    }

    private fun hideFullScreenLoader() {
        val overlay = fullScreenLoaderOverlay ?: return
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        fullScreenLoaderOverlay = null
    }

    private fun findActivity(): Activity? {
        var current: Context = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private fun applyStyle(style: PayPalSavedPaymentMethodViewStyle) {
        val resolvedStyle = PayPalSavedPaymentMethodStyleResolver(style)

        logoView.isVisible = resolvedStyle.showLogo
        labelView.isVisible = resolvedStyle.showLabel

        // LogoStyle's contract fixes the logo's bounding box at 1:1 (width == height). The actual
        // ic_paypal_brand_logo asset is a non-square 48x30 baked-in shape, so it's drawn with
        // FIT_CENTER inside that square footprint -- undistorted, letterboxed -- rather than
        // stretched to fill it.
        val logoSizePx = resolvedStyle.logoWidthDp.dpToPx().toInt()
        logoView.layoutParams = logoView.layoutParams.apply {
            width = logoSizePx
            height = logoSizePx
        }
        logoView.requestLayout()

        labelView.setTextColor(resolvedStyle.textColor)
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedStyle.labelFontSizeSp)
        resolvedStyle.fontResId?.let { fontResId ->
            runCatching { ResourcesCompat.getFont(context, fontResId) }
                .getOrNull()
                ?.let { labelView.typeface = it }
        }
        (labelView.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
            if (resolvedStyle.showLogo) resolvedStyle.labelMarginStartDp.dpToPx().toInt() else 0

        (fiSection.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
            resolvedStyle.fundingInstrumentMarginStartDp.dpToPx().toInt()
        fiSection.applyStyle(resolvedStyle)

        creditMessagingView.applyStyle(resolvedStyle)
        (creditMessagingView.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
            creditMessagingAnchorMarginPx(resolvedStyle)

        containerBackground.setColor(resolvedStyle.backgroundColor)
        containerBackground.cornerRadius = resolvedStyle.cornerRadiusDp.dpToPx()
        containerBackground.setStroke(resolvedStyle.borderWidthDp.dpToPx().toInt(), resolvedStyle.borderColor)
        resolvedStyle.heightDp?.let {
            layoutParams = layoutParams.apply { height = it.dpToPx().toInt() }
        }
        setPadding(
            resolvedStyle.horizontalPaddingDp.dpToPx().toInt(),
            resolvedStyle.verticalPaddingDp.dpToPx().toInt(),
            resolvedStyle.horizontalPaddingDp.dpToPx().toInt(),
            resolvedStyle.verticalPaddingDp.dpToPx().toInt()
        )
    }

    /** Anchors under the label's start position; falls back to FiSection when logo+label hidden. */
    private fun creditMessagingAnchorMarginPx(resolvedStyle: PayPalSavedPaymentMethodStyleResolver): Int {
        if (!resolvedStyle.showLogo && !resolvedStyle.showLabel) return 0
        val logoWidth = if (resolvedStyle.showLogo) resolvedStyle.logoWidthDp else 0f
        return (logoWidth + resolvedStyle.labelMarginStartDp).dpToPx().toInt()
    }

    private fun Float.dpToPx(): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics)
}

/** Parses [attrs] into a [PayPalSavedPaymentMethodViewStyle]; unset attrs resolve to SDK defaults. */
private fun styleFromAttrs(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
): PayPalSavedPaymentMethodViewStyle {
    val typedArray = context.obtainStyledAttributes(
        attrs,
        R.styleable.PayPalSavedPaymentMethodView,
        defStyleAttr,
        0
    )
    return try {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        val componentTheme = ComponentAppearance(
            backgroundColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_componentBackgroundColor),
            textColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_componentTextColor),
            baseFontSizeSp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_componentBaseFontSizeSp, scaledDensity),
            fontResId = typedArray.resourceIdOrNull(R.styleable.PayPalSavedPaymentMethodView_componentFontResId)
        )

        val container = ContainerStyle(
            heightDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_containerHeightDp, density),
            horizontalPaddingDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_containerHorizontalPaddingDp, density),
            verticalPaddingDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_containerVerticalPaddingDp, density),
            cornerRadiusDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_containerCornerRadiusDp, density),
            borderColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_containerBorderColor),
            borderWidthDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_containerBorderWidthDp, density),
            logo = PayPalLogoStyle(
                widthDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_logoWidthDp, density)
            ),
            label = PayPalLabelStyle(
                fontSizeSp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_labelFontSizeSp, scaledDensity),
                marginStartDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_labelMarginStartDp, density)
            ),
            fundingInstrument = FundingInstrumentStyle(
                textFontSizeSp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentTextFontSizeSp, scaledDensity),
                editIconSizeDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentEditIconSizeDp, density),
                marginStartDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentMarginStartDp, density)
            ),
            creditMessaging = CreditMessagingStyle(
                fontSizeSp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_creditMessagingFontSizeSp, scaledDensity),
                linkColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_creditMessagingLinkColor)
            )
        )

        PayPalSavedPaymentMethodViewStyle(
            showPayPalLogo = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showLogo, true),
            showPayPalLabel = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showLabel, true),
            showPayPalCreditMessaging = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_showCreditMessaging,
                true
            ),
            componentAppearance = componentTheme,
            container = container
        )
    } finally {
        typedArray.recycle()
    }
}

private fun TypedArray.colorOrNull(index: Int): Int? =
    if (hasValue(index)) getColor(index, 0) else null

private fun TypedArray.resourceIdOrNull(index: Int): Int? =
    if (hasValue(index)) getResourceId(index, 0) else null

private fun TypedArray.dimensionOrNull(index: Int, density: Float): Float? =
    if (hasValue(index)) getDimension(index, 0f) / density else null

private fun mockJwt(): String {
    // TODO: Phase 2 -- pass the real paymentMethodIdJwt (from configuration/authorization)
    //val jwt ="eyJhbGciOiJFUzI1NiIsImtpZCI6ImJ0LXNhbmQtcHJlZnBtLTdhZTUxNmYifQ.eyJqdGkiOiJlYWE0N2UwOS02ZjYyLTRkNTAtYTdkYy00MDVlYjA1YmExMTAiLCJpc3MiOiJodHRwczovL3BheW1lbnRzLnNhbmRib3guYnJhaW50cmVlLWFwaS5jb20iLCJzdWIiOiJ2N3gycmIyMjZkeDRwcjdiIiwiZXhwIjoxNzg2MDgwMTU3LCJwbWlkIjoiMmhnM2hjZXkifQ.maoM82NC5uUInBykyIZ-xPxrTwtdOzYj6BKls0aUq7c4zqNDmRsM8l55MEOtMOFmBcdcwSza-IWE2gmX_SSXOA"
    //val jwt ="eyJhbGciOiJFUzI1NiIsImtpZCI6ImJ0LXNhbmQtcHJlZnBtLTdhZTUxNmYifQ.eyJqdGkiOiI5MzRiOGIyOC1hODU4LTQ0NmMtYjg3MC0wMmQ2ZjFkNzg2MzAiLCJpc3MiOiJodHRwczovL3BheW1lbnRzLnNhbmRib3guYnJhaW50cmVlLWFwaS5jb20iLCJzdWIiOiJyM256dDY0Y3ZmNXhreHJ0IiwiZXhwIjoxNzg3MjAxOTg4LCJwbWlkIjoibnYybnF2ajMifQ.uDZFnstbLIY7gZSD-2UgsJOhPnbgALybeLVl8IK5YcPqKs6Vlp3vizSkFKwEcfwM8tkuHUP5CUpuh_rQt6c95Q"
    val jwt ="eyJhbGciOiJFUzI1NiIsImtpZCI6ImJ0LXNhbmQtcHJlZnBtLTdhZTUxNmYifQ.eyJqdGkiOiI4MGZhNTc4Zi02ZWU5LTQ2ZjctYTA2NC1jYTAzMDJkMjFhMzciLCJpc3MiOiJodHRwczovL3BheW1lbnRzLnNhbmRib3guYnJhaW50cmVlLWFwaS5jb20iLCJzdWIiOiJyM256dDY0Y3ZmNXhreHJ0IiwiZXhwIjoxNzg3MTQ4MTcwLCJwbWlkIjoibnYybnF2ajMifQ.OWfc4Guoa40JegvUOGTUvhmgTwkx83wHbz6xX8ko9nF_b7XxS4zsEl5mtD_JfiWasHgJGVVMHijTgu8ieS767A"
    return jwt
}