package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.styling

import androidx.annotation.ColorInt
import androidx.annotation.FontRes

/**
 * Merchant-facing styling for
 * [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.SavedPaymentMethodView].
 *
 * Mirrors the platform-neutral styling contract in the Edit FI styling spec (§6.1). The three
 * groups map to the web `styles` object — [root] (global type & color), [component] (the outer
 * container box) and [layout] (per-zone visibility, spacing & sizing) — plus [creditMessaging] for
 * the separate Pay Later messaging line.
 *
 * Types follow the spec's Android mapping so a single object drives both the Compose and (future)
 * XML View renderers: colors are `@ColorInt Int?` (`null` ⇒ unset / no fill), dimensions are `Float`
 * `dp`, and text sizes are `Float` `sp` (so text respects the system font-scale setting).
 */
data class SavedPaymentMethodViewStyle(
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
 */
@Suppress("MagicNumber")
data class RootStyle(
    @ColorInt val backgroundColor: Int? = 0xFFFFFFFF.toInt(),
    @ColorInt val textColorBase: Int? = 0xFF222222.toInt(),
    @ColorInt val primaryColor: Int? = null,
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
 * @property fontResId         merchant font resource; `null` ⇒ system default.
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
    @FontRes val fontResId: Int? = null,
)

/**
 * The Pay Later credit-messaging line (spec §6.1). Rendered by a separate messaging view — defined
 * here for API completeness; not consumed by [SavedPaymentMethodViewStyle]'s FI chip.
 *
 * @property enabled       show the messaging line. Default `true`.
 * @property messageText   the offer copy; empty until supplied (backend-driven).
 * @property learnMoreText the "Learn more" link copy; empty until supplied (backend-driven).
 * @property fontSizeSp    messaging text size in `sp`. Default `16`.
 */
@Suppress("MagicNumber")
data class CreditMessagingStyle(
    val enabled: Boolean = true,
    val messageText: String = "",
    val learnMoreText: String = "",
    val fontSizeSp: Float = 16f,
)
