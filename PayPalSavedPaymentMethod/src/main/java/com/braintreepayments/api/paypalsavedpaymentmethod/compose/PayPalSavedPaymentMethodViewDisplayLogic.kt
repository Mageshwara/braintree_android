package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * View-rendering support for [PayPalSavedPaymentMethodView] - credit-messaging visibility, FI
 * type icon lookup, and opening the credit-messaging landing page.
 */

/**
 * Whether the credit-messaging row should render - hidden entirely when the FI row itself is
 * hidden (No-network / Error) or merchant styling opts out, otherwise shown either as a shimmer
 * (while loading) or once both a message and a "Learn more" label have resolved.
 */
internal fun shouldShowCreditMessaging(
    hideFiRow: Boolean,
    showPayPalCreditMessaging: Boolean,
    isCreditMessageLoading: Boolean,
    creditMessage: String?,
    creditMessageLinkLabel: String?
): Boolean = !hideFiRow && showPayPalCreditMessaging &&
    (isCreditMessageLoading || (creditMessage != null && creditMessageLinkLabel != null))

/**
 * The generic FI-type icon for [type] ("CARD"/"BANK"), or null when the type has no icon (e.g.
 * PayPal-branded products render their name only).
 */
internal fun fiIconFor(type: String?) = when (type?.uppercase()) {
    "CARD" -> R.drawable.ic_fi_card_placeholder
    "BANK" -> R.drawable.ic_fi_bank_placeholder
    else -> null
}

internal fun Context.launchCreditMessagingLander(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        putExtras(Bundle().apply { putBinder(EXTRA_CUSTOM_TABS_SESSION, null) })
    }
    startActivity(intent)
}

private const val EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION"
