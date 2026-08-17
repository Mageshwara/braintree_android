package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import androidx.annotation.ColorInt
import androidx.annotation.FontRes

/**
 * Style and visibility configuration for the PayPal Saved Payment Method component.
 *
 * All styling properties default to `null`, meaning the SDK-provided default is used. See the
 * "EditFiComponent(SavedPaymentMethodViewStyle) - Styling" spec (Confluence page 3024081184) for
 * the full styling contract, defaults, and guard semantics.
 *
 * @param paypalShowLogo whether the PayPal logo is displayed. Default: true.
 * @param paypalShowLabel whether the "PayPal" text label is displayed. Default: true.
 * @param paypalShowCreditMessaging whether eligible Pay Later / credit messaging is displayed. Default: true.
 * @param paypalComponentTheme shared colors and typography used across the component. Null uses the SDK
 * default theme values.
 * @param paypalContainer dimensions, spacing, shape, border, and internal layout of the component. Null
 * uses the SDK default container styling.
 */
data class PayPalSavedPaymentMethodViewStyle(
    val paypalShowLogo: Boolean = true,
    val paypalShowLabel: Boolean = true,
    val paypalShowCreditMessaging: Boolean = true,
    val paypalComponentTheme: ComponentThemeStyle? = null,
    val paypalContainer: ContainerStyle? = null
)

/**
 * Shared colors and typography used across the PayPal Saved Payment Method component.
 *
 * @param backgroundColor background color of the entire component, including its internal padding
 * area. Null uses the SDK default.
 * @param textColor text color used for the PayPal label, funding instrument text, and credit
 * messaging. Null uses the SDK default.
 * @param baseFontSizeSp base font size used as a fallback for text elements in the component. If
 * an element-specific font size is not provided, this value is used. If this is also null, the
 * SDK default font size for that element is used.
 * @param fontResId font used for text across the component. Null uses the SDK default font.
 */
data class ComponentThemeStyle(
    @ColorInt val backgroundColor: Int? = null,
    @ColorInt val textColor: Int? = null,
    val baseFontSizeSp: Float? = null,
    @FontRes val fontResId: Int? = null
)

/**
 * Dimensions, spacing, shape, border, and internal layout of the Saved Payment Method component.
 *
 * @param heightDp height of the container, in dp. Null defaults to wrap-content.
 * @param horizontalPaddingDp horizontal padding between the container edges and its content. Null
 * uses the SDK default.
 * @param verticalPaddingDp vertical padding between the container edges and its content. Null
 * uses the SDK default.
 * @param cornerRadiusDp corner radius of the container, in dp. Null uses the SDK default.
 * @param borderColor color of the border around the container. Null uses the SDK default.
 * @param borderWidthDp width of the border around the container, in dp. Null uses the SDK
 * default.
 * @param logo sizing of the PayPal logo. Null uses the SDK default logo styling.
 * @param label typography and positioning of the "PayPal" text label. Null uses the SDK default
 * label styling.
 * @param fundingInstrument styling and positioning of the saved payment method and its edit
 * affordance. Null uses the SDK default funding instrument styling.
 * @param creditMessaging styling of the Pay Later / credit messaging. Null uses the SDK default
 * credit messaging styling.
 */
data class ContainerStyle(
    val heightDp: Float? = null,
    val horizontalPaddingDp: Float? = null,
    val verticalPaddingDp: Float? = null,
    val cornerRadiusDp: Float? = null,
    @ColorInt val borderColor: Int? = null,
    val borderWidthDp: Float? = null,
    val logo: LogoStyle? = null,
    val label: LabelStyle? = null,
    val fundingInstrument: FundingInstrumentStyle? = null,
    val creditMessaging: CreditMessagingStyle? = null
)

/**
 * Sizing of the PayPal logo.
 *
 * @param widthDp width of the PayPal logo, in dp. Null uses the SDK default.
 */
data class LogoStyle(
    val widthDp: Float? = null
)

/**
 * Typography and positioning of the "PayPal" text label.
 *
 * @param fontSizeSp font size of the "PayPal" label, in sp. Null uses
 * [ComponentThemeStyle.baseFontSizeSp].
 * @param marginStartDp start margin of the label relative to the preceding PayPal logo, in dp.
 * Null uses the SDK default.
 */
data class LabelStyle(
    val fontSizeSp: Float? = null,
    val marginStartDp: Float? = null
)

/**
 * Styling and positioning of the saved payment method and its edit affordance.
 *
 * @param textFontSizeSp font size of the saved payment method text, in sp. Null uses
 * [ComponentThemeStyle.baseFontSizeSp].
 * @param editIconSizeDp size of the edit icon for the saved payment method, in dp. Null uses the
 * SDK default.
 * @param marginStartDp start margin of the funding instrument relative to the preceding "PayPal"
 * label, in dp. Null uses the SDK default.
 */
data class FundingInstrumentStyle(
    val textFontSizeSp: Float? = null,
    val editIconSizeDp: Float? = null,
    val marginStartDp: Float? = null
)

/**
 * Styling of the Pay Later / credit messaging.
 *
 * @param fontSizeSp font size of the credit messaging text, in sp. Null uses
 * [ComponentThemeStyle.baseFontSizeSp].
 * @param linkColor color of the "Learn more" link within credit messaging. Null uses the SDK
 * default.
 */
data class CreditMessagingStyle(
    val fontSizeSp: Float? = null,
    @ColorInt val linkColor: Int? = null
)
