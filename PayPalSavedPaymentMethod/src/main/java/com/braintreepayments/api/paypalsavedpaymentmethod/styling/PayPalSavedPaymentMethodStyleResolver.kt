package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import androidx.core.content.ContextCompat
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * Resolves a (possibly-partial) [PayPalSavedPaymentMethodViewStyle] into concrete values a
 * renderer can use directly, with no further null-fallback logic needed downstream.
 *
 * SDK-provided defaults (colors + dp/sp sizes) are sourced from Android resources rather than
 * inline literals, so they automatically pick up `values-night/` overrides and react to the
 * system light/dark theme.
 *
 * Two resolution shapes are used, per the Styling doc's "Key semantics":
 * - Most fields are 2-step: explicit value if set, else the SDK-provided default.
 * - The three font-size fields (label, funding instrument text, credit messaging) are 3-step:
 *   explicit value if set, else [ComponentAppearance.baseFontSizeSp] if set, else the
 *   SDK-provided default for that element.
 *
 * A few fields are intentionally left nullable in the resolved output rather than defaulted,
 * because "unset" is itself meaningful rendering behavior, not a missing value:
 * - [heightDp] — `null` means wrap-content, not a numeric default.
 * - [fontResId] — `null` means the system default font.
 * - [creditMessagingLinkColor] — `null` means the link renders bold + underlined in [textColor]
 *   instead of a distinct link color; this is a rendering-mode switch, not a color substitution.
 */
internal class PayPalSavedPaymentMethodStyleResolver(
    private val context: Context,
    style: PayPalSavedPaymentMethodViewStyle
) {

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private fun dp(resId: Int): Float = context.resources.getDimension(resId) / density
    private fun sp(resId: Int): Float = context.resources.getDimension(resId) / scaledDensity
    private fun color(resId: Int): Int = ContextCompat.getColor(context, resId)

    val showPayPalLogo: Boolean = style.showPayPalLogo
    val showLabel: Boolean = style.showPayPalLabel
    val showCreditMessaging: Boolean = style.showPayPalCreditMessaging

    private val baseFontSizeSp: Float? = style.componentAppearance?.baseFontSizeSp

    val backgroundColor: Int =
        style.componentAppearance?.backgroundColor
            ?: color(R.color.paypal_saved_payment_method_default_background_color)
    val textColor: Int =
        style.componentAppearance?.textColor ?: color(R.color.paypal_saved_payment_method_default_text_color)
    val fontResId: Int? = style.componentAppearance?.fontResId

    val heightDp: Float? = style.container?.heightDp
    val horizontalPaddingDp: Float =
        style.container?.horizontalPaddingDp ?: dp(R.dimen.paypal_saved_payment_method_default_horizontal_padding)
    val verticalPaddingDp: Float =
        style.container?.verticalPaddingDp ?: dp(R.dimen.paypal_saved_payment_method_default_vertical_padding)
    val cornerRadiusDp: Float =
        style.container?.cornerRadiusDp ?: dp(R.dimen.paypal_saved_payment_method_default_corner_radius)
    val borderColor: Int =
        style.container?.borderColor ?: color(R.color.paypal_saved_payment_method_default_border_color)
    val borderWidthDp: Float =
        style.container?.borderWidthDp ?: dp(R.dimen.paypal_saved_payment_method_default_border_width)

    val logoWidthDp: Float =
        style.container?.logo?.widthDp ?: dp(R.dimen.paypal_saved_payment_method_default_logo_width)

    val labelFontSizeSp: Float =
        style.container?.label?.fontSizeSp
            ?: baseFontSizeSp
            ?: sp(R.dimen.paypal_saved_payment_method_default_label_font_size)
    val labelMarginStartDp: Float =
        style.container?.label?.marginStartDp ?: dp(R.dimen.paypal_saved_payment_method_default_label_margin_start)

    val fundingInstrumentTextFontSizeSp: Float =
        style.container?.fundingInstrument?.textFontSizeSp
            ?: baseFontSizeSp
            ?: sp(R.dimen.paypal_saved_payment_method_default_funding_instrument_text_font_size)
    val editIconSizeDp: Float =
        style.container?.fundingInstrument?.editIconSizeDp
            ?: dp(R.dimen.paypal_saved_payment_method_default_edit_icon_size)
    val fundingInstrumentMarginStartDp: Float =
        style.container?.fundingInstrument?.marginStartDp
            ?: dp(R.dimen.paypal_saved_payment_method_default_funding_instrument_margin_start)

    val creditMessagingFontSizeSp: Float =
        style.container?.creditMessaging?.fontSizeSp
            ?: baseFontSizeSp
            ?: sp(R.dimen.paypal_saved_payment_method_default_credit_messaging_font_size)
    val creditMessagingLinkColor: Int? = style.container?.creditMessaging?.linkColor
}
