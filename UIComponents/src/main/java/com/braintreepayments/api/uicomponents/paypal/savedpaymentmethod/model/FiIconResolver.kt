package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model

import androidx.annotation.DrawableRes
import com.braintreepayments.api.paypal.PayPalPaymentMethodSummary
import com.braintreepayments.api.uicomponents.R

/**
 * The §13.2 fallback glyph for [PayPalPaymentMethodSummary.type] — shown until remote brand art
 * ([PayPalPaymentMethodSummary.imageUrl]) is wired up in a future PR. `null` when the tile shows no
 * icon (e.g. Pay Later products, which render the product name only).
 */
@get:DrawableRes
internal val PayPalPaymentMethodSummary.fallbackIconRes: Int?
    get() = when (type.uppercase()) {
        TYPE_CARD -> R.drawable.edit_fi_generic_card
        TYPE_BANK -> R.drawable.edit_fi_generic_bank
        else -> null
    }

private const val TYPE_CARD = "CARD"
private const val TYPE_BANK = "BANK"
