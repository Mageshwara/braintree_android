package com.braintreepayments.api.paypalpaymentmethod.styling

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FontRes

/**
 * Style contract for
 * [com.braintreepayments.api.paypalpaymentmethod.compose.SavedPayPalPaymentMethodView] — v1, happy
 * path.
 *
 * Plain Kotlin data classes only — no UI-toolkit types. Single source of truth across
 * implementations; unit conversion happens only inside each renderer's own drawing code, never
 * here.
 *
 * Scope: single-row layout, light theme only. Dark theme, responsive stacked layout, and logo
 * box-scaling are deliberately deferred.
 */

// ── Root ─────────────────────────────────────────────────────────────
data class SavedPayPalPaymentMethodViewStyle(
    val showLogo: Boolean = true,
    val showLabel: Boolean = true,
    val showCreditMessaging: Boolean = true,
    val theme: ThemeStyle = ThemeStyle(),
    val container: ContainerStyle = ContainerStyle()
)

// ── Colors, fonts, brand identity ──────────────────────────────────────
data class ThemeStyle(
    @ColorInt val backgroundColor: Int = Color.WHITE,
    @ColorInt val textColorBase: Int = Color.parseColor("#222222"),
    val baseFontSizeSp: Float = 14f, // currently unused — kept for later
    @FontRes val fontResId: Int? = null,
    @ColorInt val linkColor: Int? = null // null -> bold+underline fallback in textColorBase
)

// ── Outer box (own shape) + its four children's styles ────────────────
@Suppress("MagicNumber")
data class ContainerStyle(
    val heightDp: Float? = null, // null -> wrap-content; never clamped
    val horizontalPaddingDp: Float = 0f, // floor 0, no max
    val verticalPaddingDp: Float = 10f, // floor 0, no max
    val cornerRadiusDp: Float = 0f, // floor 0, no max
    @ColorInt val borderColor: Int = Color.TRANSPARENT,
    val borderWidthDp: Float = 0f, // floor 0, no max

    val logo: LogoStyle = LogoStyle(),
    val label: LabelStyle = LabelStyle(),
    val fiCluster: FiClusterStyle = FiClusterStyle(),
    val creditMessaging: CreditMessagingStyle = CreditMessagingStyle()
)

@Suppress("MagicNumber")
data class LogoStyle(
    val widthDp: Float = 24f
    // no marginStart — Logo is the first child; its start offset is
    // ContainerStyle.horizontalPaddingDp, not its own margin.
)

@Suppress("MagicNumber")
data class LabelStyle(
    val fontSizeSp: Float = 20f, // Figma-sourced default (intentionally differs from web's 14px)
    val marginStartDp: Float = 6f // gap from Logo
)

@Suppress("MagicNumber")
data class FiClusterStyle(
    val textFontSizeSp: Float = 14f,
    val editIconSizeDp: Float = 16f,
    val marginStartDp: Float = 12f, // gap from Label; collapses toward Logo if Label is hidden
    @ColorInt val backgroundColor: Int = Color.parseColor("#F0F0F0"), // DTC — pill background
    val cornerRadiusDp: Float = 999f, // DTC — pill shape
    val paddingDp: Float = 4f // DTC — pill internal padding
)

@Suppress("MagicNumber")
data class CreditMessagingStyle(
    val fontSizeSp: Float = 16f
    // messageText / learnMoreText / learnMoreUrl deliberately absent —
    // compliance content from the API (see CreditMessagingState), never
    // merchant-authored via style.
)