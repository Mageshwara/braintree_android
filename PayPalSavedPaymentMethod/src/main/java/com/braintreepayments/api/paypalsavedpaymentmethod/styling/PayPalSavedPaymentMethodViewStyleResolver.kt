package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FontRes

/**
 * [PayPalSavedPaymentMethodViewStyle] with every field resolved to a concrete value the renderer
 * can use directly — no more nulls to check.
 *
 * [heightDp] is the one exception: null here still means wrap-content, it is never defaulted to a
 * number.
 */
@Suppress("LongParameterList")
internal class ResolvedPayPalSavedPaymentMethodViewStyle(
    val showPayPalLogo: Boolean,
    val showPayPalLabel: Boolean,
    val showPayPalCreditMessaging: Boolean,
    @ColorInt val backgroundColor: Int,
    @ColorInt val textColor: Int,
    @FontRes val fontResId: Int?,
    val heightDp: Float?,
    val horizontalPaddingDp: Float,
    val verticalPaddingDp: Float,
    val cornerRadiusDp: Float,
    @ColorInt val borderColor: Int,
    val borderWidthDp: Float,
    val logoWidthDp: Float,
    val labelFontSizeSp: Float,
    val labelMarginStartDp: Float,
    val fundingInstrumentTextFontSizeSp: Float,
    val fundingInstrumentEditIconSizeDp: Float,
    val fundingInstrumentMarginStartDp: Float,
    val creditMessagingFontSizeSp: Float,
    @ColorInt val creditMessagingLinkColor: Int?
)

/**
 * Resolves a merchant-supplied [PayPalSavedPaymentMethodViewStyle] against the SDK defaults.
 *
 * Element-specific font sizes follow the documented fallback chain: the element's own value, then
 * [ComponentAppearance.baseFontSizeSp] only if the merchant explicitly set it, then that element's
 * own SDK default — never the resolved base size, since each element's default differs from the
 * generic base default.
 */
internal fun PayPalSavedPaymentMethodViewStyle.resolve(): ResolvedPayPalSavedPaymentMethodViewStyle {
    val appearance = componentAppearance
    val container = container
    val logo = container?.logo
    val label = container?.label
    val fundingInstrument = container?.fundingInstrument
    val creditMessaging = container?.creditMessaging

    return ResolvedPayPalSavedPaymentMethodViewStyle(
        showPayPalLogo = showPayPalLogo,
        showPayPalLabel = showPayPalLabel,
        showPayPalCreditMessaging = showPayPalCreditMessaging,
        backgroundColor = appearance?.backgroundColor ?: PayPalSavedPaymentMethodStyleDefaults.BACKGROUND_COLOR,
        textColor = appearance?.textColor ?: PayPalSavedPaymentMethodStyleDefaults.TEXT_COLOR,
        fontResId = appearance?.fontResId,
        heightDp = container?.heightDp,
        horizontalPaddingDp = container?.horizontalPaddingDp
            ?: PayPalSavedPaymentMethodStyleDefaults.HORIZONTAL_PADDING_DP,
        verticalPaddingDp = container?.verticalPaddingDp
            ?: PayPalSavedPaymentMethodStyleDefaults.VERTICAL_PADDING_DP,
        cornerRadiusDp = container?.cornerRadiusDp ?: PayPalSavedPaymentMethodStyleDefaults.CORNER_RADIUS_DP,
        borderColor = container?.borderColor ?: PayPalSavedPaymentMethodStyleDefaults.BORDER_COLOR,
        borderWidthDp = container?.borderWidthDp ?: PayPalSavedPaymentMethodStyleDefaults.BORDER_WIDTH_DP,
        logoWidthDp = logo?.widthDp ?: PayPalSavedPaymentMethodStyleDefaults.LOGO_WIDTH_DP,
        labelFontSizeSp = resolveFontSize(
            label?.fontSizeSp,
            appearance?.baseFontSizeSp,
            PayPalSavedPaymentMethodStyleDefaults.LABEL_FONT_SIZE_SP
        ),
        labelMarginStartDp = label?.marginStartDp
            ?: PayPalSavedPaymentMethodStyleDefaults.LABEL_MARGIN_START_DP,
        fundingInstrumentTextFontSizeSp = resolveFontSize(
            fundingInstrument?.textFontSizeSp,
            appearance?.baseFontSizeSp,
            PayPalSavedPaymentMethodStyleDefaults.FUNDING_INSTRUMENT_TEXT_FONT_SIZE_SP
        ),
        fundingInstrumentEditIconSizeDp = fundingInstrument?.editIconSizeDp
            ?: PayPalSavedPaymentMethodStyleDefaults.FUNDING_INSTRUMENT_EDIT_ICON_SIZE_DP,
        fundingInstrumentMarginStartDp = fundingInstrument?.marginStartDp
            ?: PayPalSavedPaymentMethodStyleDefaults.FUNDING_INSTRUMENT_MARGIN_START_DP,
        creditMessagingFontSizeSp = resolveFontSize(
            creditMessaging?.fontSizeSp,
            appearance?.baseFontSizeSp,
            PayPalSavedPaymentMethodStyleDefaults.CREDIT_MESSAGING_FONT_SIZE_SP
        ),
        creditMessagingLinkColor = creditMessaging?.linkColor
    )
}

/**
 * Font-size fallback chain shared by every text element: its own value, then the merchant's
 * explicit [ComponentAppearance.baseFontSizeSp] override, then that element's own SDK default.
 */
private fun resolveFontSize(elementFontSizeSp: Float?, baseFontSizeSp: Float?, defaultFontSizeSp: Float): Float =
    elementFontSizeSp ?: baseFontSizeSp ?: defaultFontSizeSp

/**
 * SDK default values — used only when the merchant leaves the corresponding field null. Mirrors
 * [com.braintreepayments.api.paypalsavedpaymentmethod.compose.PayPalSavedPaymentMethodView]'s
 * existing constant defaults.
 */
internal object PayPalSavedPaymentMethodStyleDefaults {
    @ColorInt val BACKGROUND_COLOR: Int = Color.WHITE
    @ColorInt val TEXT_COLOR: Int = Color.parseColor("#222222")
    @ColorInt val BORDER_COLOR: Int = Color.TRANSPARENT

    const val BASE_FONT_SIZE_SP = 14f
    const val HORIZONTAL_PADDING_DP = 0f
    const val VERTICAL_PADDING_DP = 10f
    const val CORNER_RADIUS_DP = 0f
    const val BORDER_WIDTH_DP = 0f

    const val LOGO_WIDTH_DP = 48f

    const val LABEL_FONT_SIZE_SP = 20f
    const val LABEL_MARGIN_START_DP = 13f

    const val FUNDING_INSTRUMENT_TEXT_FONT_SIZE_SP = 14f
    const val FUNDING_INSTRUMENT_EDIT_ICON_SIZE_DP = 16f
    const val FUNDING_INSTRUMENT_MARGIN_START_DP = 8f

    const val CREDIT_MESSAGING_FONT_SIZE_SP = 16f
}
