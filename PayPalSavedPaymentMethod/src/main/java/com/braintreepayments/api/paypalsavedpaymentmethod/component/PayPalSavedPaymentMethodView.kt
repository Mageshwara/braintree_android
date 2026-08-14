package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.callback.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentThemeStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.FundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.LabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.LogoStyle
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
 * Public API is intentionally narrow -- five members total: the constructor, [initialize],
 * [setStyle], [setPayPalRequest], and [handleReturnToApp]. Everything else ([FiClusterState] /
 * [CreditMessagingState], the internal [client], sub-views, coroutine scope/jobs) is owned and
 * mutated by this view itself and never exposed.
 *
 * [initialize] is the only member that triggers network activity -- it starts the FI (and, if
 * enabled, credit-messaging) fetch and wires [callback] for the entire lifetime of the edit-FI
 * flow. There is no separate merchant-triggered refresh method.
 *
 * Style resolution precedence: `attrs` parsed via `obtainStyledAttributes` establish the initial
 * style at construction time; a later programmatic [setStyle] call fully replaces it.
 *
 * Phase 1 scope note: [client] is a mock implementation.
 * // TODO: Phase 2 -- back [client] with a real network implementation, and construct
 * `PayPalLauncher`/`PayPalClient` in [initialize] the way `PayPalButton.initialize` does.
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

    /** Mock in Phase 1, real network client in Phase 2. Never exposed publicly -- reachable only
     * from inside this view, per the finalized public API. */
    internal var client: PayPalSavedPaymentMethodClient = PayPalSavedPaymentMethodClient()

    /** Set once via [initialize]; delivers the edit-FI flow's launch + final result events. */
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

    /**
     * One-shot setup: wires the request/callback needed for the edit-FI flow and starts this
     * view's FI (and, if enabled, credit-messaging) fetch. The only member that triggers network
     * activity -- there is no separate merchant-triggered refresh method.
     *
     * @param authorization a Tokenization Key or Client Token used to authenticate.
     * @param payPalRequest the PayPal request configuration used by the edit-FI flow.
     * @param callback      receives the edit-FI flow's launch + final result events.
     */
    fun initialize(
        authorization: String,
        payPalRequest: PayPalCheckoutRequest,
        callback: PayPalSavedPaymentMethodLaunchCallback
    ) {
        // TODO: Phase 2 -- construct a real network-backed client(context, authorization) here,
        // the way PayPalButton.initialize builds PayPalClient. Phase 1's mock
        // PayPalSavedPaymentMethodClient needs none of these parameters, so authorization is
        // accepted (to keep the real public API shape) but unused for now.
        this.payPalRequest = payPalRequest
        this.callback = callback
        startFetches()
    }

    /**
     * Updates the PayPal request configuration used by the edit-FI flow, independently of
     * [initialize]. Read fresh at edit-tap time, not cached here -- the merchant's current
     * [payPalRequest] is what gets forwarded, exactly like `PayPalButton.setPayPalRequest`.
     */
    fun setPayPalRequest(payPalRequest: PayPalCheckoutRequest) {
        this.payPalRequest = payPalRequest
    }

    /**
     * Handles the return from the PayPal authentication flow after a real browser-switch launch.
     * The final outcome is delivered via [initialize]'s `callback`.
     *
     * // TODO: Phase 2 -- real signature/contract, but not exercised yet: this build's Phase 1
     * edit-tap flow ([startEditFlow]) never performs a real browser-switch launch, so there is no
     * real [Intent] for this to parse. Real implementation mirrors
     * `PayPalButton.handleReturnToApp` (PayPalLauncher.handleReturnToApp -> tokenize ->
     * fetch_selected_fi refresh -> re-render).
     */
    fun handleReturnToApp(intent: Intent) {
        // Intentionally a no-op stub in Phase 1 -- see TODO above.
    }

    /** Sets the view's visual style. Fully replaces any style parsed from XML attrs. Safe to
     * call at any time. */
    fun setStyle(style: PayPalSavedPaymentMethodViewStyle) {
        this.style = style
        applyStyle(style)
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

    /** Triggers this view's own FI + credit-messaging fetches. Never called by the merchant --
     * per the ownership model, fetches are lifecycle-triggered, not caller-triggered. */
    private fun startFetches() {
        fiSection.setState(FiClusterState.Loading)
        fiFetchJob = scope().launch {
            // TODO: Phase 2 -- pass the real paymentMethodIdJwt (from configuration/authorization)
            val state = client.fetchFI(paymentMethodIdJwt = "").toFiClusterState()
            lastFiClusterState = state
            fiSection.setState(state)
        }

        if (!style.showCreditMessaging) {
            creditMessagingView.setState(CreditMessagingState.Hidden)
            return
        }
        creditMessagingView.setState(CreditMessagingState.Loading)
        creditMessagingFetchJob = scope().launch {
            // TODO: Phase 2 -- pass the real amount/contentAttributes for
            // fetchCreditPresentmentMessages
            val state = client.fetchCreditMessaging(amount = "", contentAttributes = emptyList())
                .toCreditMessagingState()
            creditMessagingView.setState(state)
        }
    }

    private fun Result<PayPalSavedPaymentMethodSummary>.toFiClusterState(): FiClusterState {
        val summary = getOrNull() ?: return FiClusterState.NoNetworkLoad
        val method = summary.paypalSavedPaymentMethods.firstOrNull()
        val payer = summary.paypalPayer
        return when {
            method != null -> FiClusterState.Available(method)
            payer != null -> FiClusterState.NoFiLoad(payer.email)
            else -> FiClusterState.NoNetworkLoad
        }
    }

    private fun Result<CreditMessagingContent>.toCreditMessagingState(): CreditMessagingState {
        val content = getOrNull() ?: return CreditMessagingState.Hidden
        return if (content.message.isBlank()) CreditMessagingState.Hidden else CreditMessagingState.Content(content)
    }

    /**
     * Edit pencil tap -> loading -> stubbed result -> re-render. Real client wiring
     * (`create_payment_resource`, tokenize, app-switch) is Phase 2 -- see inline TODOs.
     */
    private fun startEditFlow() {
        val request = payPalRequest ?: return

        // TODO: Phase 2 -- this Started value is a Phase 1 stub, not a result of a real
        // PayPalLauncher.launch call (see PayPalSavedPaymentMethodLaunchCallback's own TODO).
        callback?.onSavedPaymentMethodLaunch(
            PayPalPendingRequest.Started("phase1-stub-pending-request")
        )

        showFullScreenLoader()
        fiSection.setState(FiClusterState.Loading)
        editFlowJob = scope().launch {
            // TODO: Phase 2 -- real call adds edit_billing_agreement_jwt + app-switch params,
            // launches the PayPal app or in-app browser per the parsed redirectType, then this
            // whole method's remainder (tokenize + refresh) runs from handleReturnToApp instead,
            // driven by a real return Intent -- not inline here.
            val result = client.createPaymentResource(request)
            result.fold(
                onSuccess = { onEditFlowResourceCreated() },
                onFailure = { error -> onEditFlowFailed(error) }
            )
        }
    }

    private suspend fun onEditFlowResourceCreated() {
        // Phase 1 stub: re-fetch FI to prove the re-render path works. Cannot invoke
        // callback.onSavedPaymentMethodResult with PayPalResult.Success here --
        // PayPalAccountNonce's constructor is internal to the PayPal module, so this build can't
        // fabricate one; only Cancel/Failure are exercised end-to-end in Phase 1.
        // TODO: Phase 2 -- tokenize the real paymentAuthResult (-> PayPalAccountNonce), refresh FI
        // via fetch_selected_fi, then callback?.onSavedPaymentMethodResult(PayPalResult.Success(nonce)).
        val state = client.fetchFI(paymentMethodIdJwt = "").toFiClusterState()
        lastFiClusterState = state
        fiSection.setState(state)
        hideFullScreenLoader()
    }

    private fun onEditFlowFailed(error: Throwable) {
        hideFullScreenLoader()
        fiSection.setState(lastFiClusterState)
        val payPalResult = if (error is PayPalSavedPaymentMethodClient.EditFlowCancelException) {
            PayPalResult.Cancel
        } else {
            PayPalResult.Failure(error as? Exception ?: Exception(error))
        }
        callback?.onSavedPaymentMethodResult(payPalResult)
    }

    /**
     * This view reaches outside its own bounds and dims/blocks the entire host screen while the
     * edit flow's async work is in flight, rather than asking the merchant to render their own
     * full-page loader.
     */
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

    /**
     * Credit messaging anchors under the label's start position (`logo width +
     * label.marginStartDp`), regardless of the label's own visibility -- except when both logo
     * and label are hidden, where it follows normal flow and aligns with FiSection instead
     * (margin 0, since `horizontalPaddingDp` is already applied as this view's own padding).
     */
    private fun creditMessagingAnchorMarginPx(resolvedStyle: PayPalSavedPaymentMethodStyleResolver): Int {
        if (!resolvedStyle.showLogo && !resolvedStyle.showLabel) return 0
        val logoWidth = if (resolvedStyle.showLogo) resolvedStyle.logoWidthDp else 0f
        return (logoWidth + resolvedStyle.labelMarginStartDp).dpToPx().toInt()
    }

    private fun Float.dpToPx(): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics)
}

/**
 * Parses [attrs] into a [PayPalSavedPaymentMethodViewStyle]. Mirrors that class's field/nesting
 * shape 1:1 -- an attr with no value in the XML leaves its style field `null`, which the
 * [PayPalSavedPaymentMethodStyleResolver] then resolves to the SDK default, exactly as a
 * programmatically-built style with that field unset would.
 */
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

        val componentTheme = ComponentThemeStyle(
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
            logo = LogoStyle(
                widthDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_logoWidthDp, density)
            ),
            label = LabelStyle(
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
            showLogo = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showLogo, true),
            showLabel = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showLabel, true),
            showCreditMessaging = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_showCreditMessaging,
                true
            ),
            componentTheme = componentTheme,
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
