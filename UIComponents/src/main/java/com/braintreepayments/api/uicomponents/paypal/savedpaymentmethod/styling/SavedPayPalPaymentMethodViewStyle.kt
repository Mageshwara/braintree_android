package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.styling

import androidx.annotation.ColorInt
import androidx.annotation.FontRes

/**
 * Merchant-facing styling for
 * [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.SavedPayPalPaymentMethodView].
 *
 * Mirrors the platform-neutral styling contract in the Edit FI styling spec (§6.1). The four
 * groups map to the web `styles` object — [root] (global type & color), [component] (the outer
 * container box), [layout] (per-zone visibility, spacing & sizing) and [creditMessaging] (the
 * Pay Later credit-messaging line beneath the FI chip).
 *
 * Types follow the spec's Android mapping so a single object drives both the Compose and (future)
 * XML View renderers: colors are `@ColorInt Int?` (`null` ⇒ unset / no fill), dimensions are `Float`
 * `dp`, and text sizes are `Float` `sp` (so text respects the system font-scale setting).
 */
class SavedPayPalPaymentMethodViewStyle(
    val root: RootStyle = RootStyle(),
    val component: ComponentStyle = ComponentStyle(),
    val layout: LayoutStyle = LayoutStyle(),
    val creditMessaging: CreditMessagingStyle = CreditMessagingStyle(),
)

/**
 * Global type & color (spec §4.1).
 *
 * @property backgroundColor container fill; `null` ⇒ no fill. Default `#FFFFFF`.
 * @property textColorBase   color for the label, FI text and credit messaging. Default `#222222`.
 * @property primaryColor    accent for the "Learn more" link; `null` ⇒ the link is distinguished by
 * bold + underline in [textColorBase] instead. No default.
 * @property fontResId       merchant font resource; `null` ⇒ system default.
 */
@Suppress("MagicNumber")
data class RootStyle(
    @ColorInt val backgroundColor: Int? = 0xFFFFFFFF.toInt(),
    @ColorInt val textColorBase: Int? = 0xFF222222.toInt(),
    @ColorInt val primaryColor: Int? = null,
    @FontRes val fontResId: Int? = null,
)

/**
 * The outer container box (spec §4.2).
 *
 * @property heightDp                container height in `dp`; `null` ⇒ wrap content (the default).
 * @property horizontalPaddingDp     start/end padding in `dp`. Default `0`.
 * @property verticalPaddingDp       top/bottom padding in `dp`. Default `10`.
 * @property cornerRadiusDp          container corner radius in `dp`. Default `0`.
 * @property borderColor             container border color; `null` ⇒ no visible border (default).
 * @property borderWidthDp           container border width in `dp`. Default `0`.
 * @property cardIconBackgroundColor fill behind the funding-instrument icon; `null` ⇒ no fill.
 * @property cardIconCornerRadiusDp  funding-instrument icon corner radius in `dp`. Default `3`.
 * @property fiClusterBackgroundColor fill of the FI "chip" pill; `null` ⇒ no fill. Default
 * `#F5F7FA`.
 */
@Suppress("MagicNumber")
data class ComponentStyle(
    val heightDp: Float? = null,
    val horizontalPaddingDp: Float = 0f,
    val verticalPaddingDp: Float = 10f,
    val cornerRadiusDp: Float = 0f,
    @ColorInt val borderColor: Int? = null,
    val borderWidthDp: Float = 0f,
    @ColorInt val cardIconBackgroundColor: Int? = null,
    val cardIconCornerRadiusDp: Float = 3f,
    @ColorInt val fiClusterBackgroundColor: Int? = 0xFFF5F7FA.toInt(),
)

/**
 * Per-zone visibility, spacing & sizing (spec §4.3).
 *
 * @property showLogo          show the PayPal brand mark. Default `true`.
 * @property showLabel         show the "PayPal" text label. Default `true`.
 * @property logoLabelGapDp    gap between the logo and the label in `dp`. Default `6`.
 * @property labelFiGapDp      gap between the label and the FI in `dp`. Default `12`.
 * @property labelFontSizeSp   "PayPal" label text size in `sp`. Default `20`.
 * @property fiTextFontSizeSp  FI text (masked number / product name) size in `sp`. Default `14`.
 * @property iconSizeDp        edit (pencil) affordance size in `dp`. Default `16`.
 */
@Suppress("MagicNumber")
data class LayoutStyle(
    val showLogo: Boolean = true,
    val showLabel: Boolean = true,
    val logoLabelGapDp: Float = 6f,
    val labelFiGapDp: Float = 12f,
    val labelFontSizeSp: Float = 20f,
    val fiTextFontSizeSp: Float = 14f,
    val iconSizeDp: Float = 16f,
)

/**
 * The Pay Later credit-messaging line (spec §6.1), rendered by
 * [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.CreditMessagingView].
 * The message and link copy are not customizable here — they come from the Credit Presentment API
 * response (see `CreditMessagingDisplayState`).
 *
 * @property enabled         gates whether the row is fetched and shown. Default `true`.
 * @property fontSizeSp      messaging text size in `sp`. Default `16`; min is the default, max `24`.
 * @property lineHeightRatio line height as a multiple of [fontSizeSp], so wrapped lines aren't
 * cramped together. Default `1.5` (e.g. 24sp line height over 16sp text).
 */
@Suppress("MagicNumber")
data class CreditMessagingStyle(
    val enabled: Boolean = true,
    val fontSizeSp: Float = 16f,
    val lineHeightRatio: Float = 1.5f,
)
