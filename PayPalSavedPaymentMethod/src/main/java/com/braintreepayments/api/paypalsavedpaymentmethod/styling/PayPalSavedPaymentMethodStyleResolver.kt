package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.graphics.Color

// SDK-provided defaults, sourced from the Figma-confirmed v1 happy-path spec.
// Centralized here so no resolved value is ever an inline literal in view code.
private const val DEFAULT_BACKGROUND_COLOR = Color.WHITE
private val DEFAULT_TEXT_COLOR = Color.parseColor("#222222")
private val DEFAULT_BORDER_COLOR = Color.TRANSPARENT
private const val DEFAULT_HORIZONTAL_PADDING_DP = 0f
private const val DEFAULT_VERTICAL_PADDING_DP = 10f
private const val DEFAULT_CORNER_RADIUS_DP = 0f
private const val DEFAULT_BORDER_WIDTH_DP = 0f

// TODO: patch's shipped LogoStyle.widthDp default was 48f; styling.md/Figma says 24f.
// Using 24f per the Styling doc's source-of-truth call-out (§1 open item) — flag to
// design if this needs to change.
private const val DEFAULT_LOGO_WIDTH_DP = 24f

private const val DEFAULT_LABEL_FONT_SIZE_SP = 20f
private const val DEFAULT_LABEL_MARGIN_START_DP = 6f
private const val DEFAULT_FUNDING_INSTRUMENT_TEXT_FONT_SIZE_SP = 14f
private const val DEFAULT_EDIT_ICON_SIZE_DP = 16f
private const val DEFAULT_FUNDING_INSTRUMENT_MARGIN_START_DP = 12f
private const val DEFAULT_CREDIT_MESSAGING_FONT_SIZE_SP = 16f

/**
 * Resolves a (possibly-partial) [PayPalSavedPaymentMethodViewStyle] into concrete values a
 * renderer can use directly, with no further null-fallback logic needed downstream.
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
internal class PayPalSavedPaymentMethodStyleResolver(style: PayPalSavedPaymentMethodViewStyle) {

    val showLogo: Boolean = style.showPayPalLogo
    val showLabel: Boolean = style.showPayPalLabel
    val showCreditMessaging: Boolean = style.showPayPalCreditMessaging

    private val baseFontSizeSp: Float? = style.componentAppearance?.baseFontSizeSp

    val backgroundColor: Int =
        style.componentAppearance?.backgroundColor ?: DEFAULT_BACKGROUND_COLOR
    val textColor: Int =
        style.componentAppearance?.textColor ?: DEFAULT_TEXT_COLOR
    val fontResId: Int? = style.componentAppearance?.fontResId

    val heightDp: Float? = style.container?.heightDp
    val horizontalPaddingDp: Float =
        style.container?.horizontalPaddingDp ?: DEFAULT_HORIZONTAL_PADDING_DP
    val verticalPaddingDp: Float =
        style.container?.verticalPaddingDp ?: DEFAULT_VERTICAL_PADDING_DP
    val cornerRadiusDp: Float =
        style.container?.cornerRadiusDp ?: DEFAULT_CORNER_RADIUS_DP
    val borderColor: Int =
        style.container?.borderColor ?: DEFAULT_BORDER_COLOR
    val borderWidthDp: Float =
        style.container?.borderWidthDp ?: DEFAULT_BORDER_WIDTH_DP

    val logoWidthDp: Float =
        style.container?.logo?.widthDp ?: DEFAULT_LOGO_WIDTH_DP

    val labelFontSizeSp: Float =
        style.container?.label?.fontSizeSp
            ?: baseFontSizeSp
            ?: DEFAULT_LABEL_FONT_SIZE_SP
    val labelMarginStartDp: Float =
        style.container?.label?.marginStartDp ?: DEFAULT_LABEL_MARGIN_START_DP

    val fundingInstrumentTextFontSizeSp: Float =
        style.container?.fundingInstrument?.textFontSizeSp
            ?: baseFontSizeSp
            ?: DEFAULT_FUNDING_INSTRUMENT_TEXT_FONT_SIZE_SP
    val editIconSizeDp: Float =
        style.container?.fundingInstrument?.editIconSizeDp ?: DEFAULT_EDIT_ICON_SIZE_DP
    val fundingInstrumentMarginStartDp: Float =
        style.container?.fundingInstrument?.marginStartDp
            ?: DEFAULT_FUNDING_INSTRUMENT_MARGIN_START_DP

    val creditMessagingFontSizeSp: Float =
        style.container?.creditMessaging?.fontSizeSp
            ?: baseFontSizeSp
            ?: DEFAULT_CREDIT_MESSAGING_FONT_SIZE_SP
    val creditMessagingLinkColor: Int? = style.container?.creditMessaging?.linkColor
}
