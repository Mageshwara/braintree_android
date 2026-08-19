package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Pill background/padding are fixed SDK constants, not merchant-configurable -- the current
// PayPalSavedPaymentMethodViewStyle contract (see FundingInstrumentStyle) intentionally has no
// backgroundColor/cornerRadiusDp/paddingDp fields for this cluster.
private val PILL_BACKGROUND_COLOR = Color.parseColor("#F0F0F0")
private const val PILL_CORNER_RADIUS_DP = 999f
private const val PILL_PADDING_DP = 4f

/**
 * The funding-instrument pill: card-art icon, masked FI text (or fallback email), and an edit
 * pencil. Renders all states of [FiClusterState].
 *
 * Internal to the module -- not part of `PayPalSavedPaymentMethodView`'s public API -- so it
 * consumes [PayPalSavedPaymentMethodStyleResolver] directly rather than the raw nullable style
 * contract.
 *
 * This view owns one piece of async work itself: loading the funding instrument's image into
 * [iconView], since whether that succeeds is a rendering-layer concern local to this view, not
 * something the FI fetch can know in advance. That load is cancelled on detach and not restarted
 * until the next [setState] call, so a recycled/re-attached instance never resolves into a stale
 * render.
 */
internal class FiSection @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val textView: TextView
    private val editIconView: ImageView
    private val shimmerView: View
    private val pillBackground = GradientDrawable()

    private var onEditClickListener: (() -> Unit)? = null
    private var imageLoadJob: Job? = null
    private var viewScope: CoroutineScope? = null
    private var shimmerAnimator: ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.fi_section, this, true)

        iconView = findViewById(R.id.psp_fi_icon)
        textView = findViewById(R.id.psp_fi_text)
        editIconView = findViewById(R.id.psp_fi_edit_icon)
        shimmerView = findViewById(R.id.psp_fi_shimmer)

        background = pillBackground
        editIconView.setOnClickListener { onEditClickListener?.invoke() }

        pillBackground.shape = GradientDrawable.RECTANGLE
        pillBackground.setColor(PILL_BACKGROUND_COLOR)
        pillBackground.cornerRadius = PILL_CORNER_RADIUS_DP.dpToPx()
        val padding = PILL_PADDING_DP.dpToPx().toInt()
        setPadding(padding, padding, padding, padding)

        renderLoading()
    }

    /** Applies resolved style values to the pill. Safe to call before or after [setState]. */
    fun applyStyle(resolvedStyle: PayPalSavedPaymentMethodStyleResolver) {
        textView.setTextColor(resolvedStyle.textColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedStyle.fundingInstrumentTextFontSizeSp)
        resolvedStyle.fontResId?.let { fontResId ->
            runCatching { ResourcesCompat.getFont(context, fontResId) }
                .getOrNull()
                ?.let { textView.typeface = it }
        }

        val editIconSize = resolvedStyle.editIconSizeDp.dpToPx().toInt()
        editIconView.layoutParams = editIconView.layoutParams.apply {
            width = editIconSize
            height = editIconSize
        }
        editIconView.requestLayout()
    }

    /** Registers a listener for taps on the edit pencil. */
    fun setOnEditClickListener(listener: () -> Unit) {
        onEditClickListener = listener
    }

    /**
     * Renders [state]. `PayPalSavedPaymentMethodView` calls this in response to its own FI fetch;
     * this view never triggers that fetch itself.
     */
    fun setState(state: FiClusterState) {
        imageLoadJob?.cancel()
        iconView.setImageDrawable(null)

        when (state) {
            is FiClusterState.Loading -> renderLoading()
            is FiClusterState.Available -> renderAvailable(state)
            is FiClusterState.NoFiLoad -> renderNoFiLoad(state)
            is FiClusterState.NoNetworkLoad -> {
                stopShimmer()
                isVisible = false
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.cancel()
        viewScope = null
        imageLoadJob = null
        stopShimmer()
    }

    private fun renderLoading() {
        isVisible = true
        iconView.isVisible = false
        editIconView.isVisible = false
        shimmerView.isVisible = true
        textView.isVisible = false
        textView.text = null
        startShimmer()
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun renderAvailable(state: FiClusterState.Available) {
        isVisible = true
        stopShimmer()
        shimmerView.isVisible = false
        textView.isVisible = true
        editIconView.isVisible = true
        iconView.isVisible = true

        val method = state.paymentMethod
        textView.text = resources.getString(
            R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number,
            method.lastDigits.orEmpty()
        )
        iconView.setImageResource(fallbackIconRes(method.type))

        val scope = viewScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            .also { viewScope = it }
        imageLoadJob = scope.launch {
            val bitmap = SimpleBitmapLoader.load(method.imageUrl)
            if (bitmap != null) {
                iconView.setImageBitmap(bitmap)
            }
            // null -> leave the fallback glyph already set above; this is the
            // "FI available, image failed" render outcome, not a distinct state.
        }
    }

    private fun renderNoFiLoad(state: FiClusterState.NoFiLoad) {
        isVisible = true
        stopShimmer()
        shimmerView.isVisible = false
        textView.isVisible = true
        editIconView.isVisible = true
        iconView.isVisible = false
        textView.text = state.email
    }

    private fun startShimmer() {
        if (shimmerAnimator != null) return
        shimmerAnimator = ObjectAnimator.ofFloat(shimmerView, "alpha", 1f, 0.4f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        shimmerView.alpha = 1f
    }

    private fun fallbackIconRes(type: String): Int = if (type.equals("BANK", ignoreCase = true)) {
        R.drawable.ic_fi_bank_placeholder
    } else {
        R.drawable.ic_fi_card_placeholder
    }

    private fun Float.dpToPx(): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics)
}
