package com.braintreepayments.api.uicomponents

import androidx.compose.ui.graphics.Color

/**
 * Theming for the [com.braintreepayments.api.uicomponents.compose.CreditMessagingView] — the Pay Later
 * credit-messaging row.
 *
 * Defaults match the design (grey subtext + a dark, underlined link). Merchants may override.
 * (Per LLD §13.2 the final component unifies this with `EditFiComponentStyle` when the FI chip and
 * messaging are embedded together — that reconciliation happens in the embed pass.)
 *
 * @property messageTextColor color of the leading messaging copy.
 * @property linkTextColor    color of the trailing tappable link.
 */
@Suppress("MagicNumber")
data class CreditMessagingStyle(
    val messageTextColor: Color = Color(0xFF6C7378),
    val linkTextColor: Color = Color(0xFF000000),
)
