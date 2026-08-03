package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model

import androidx.annotation.DrawableRes
import com.braintreepayments.api.uicomponents.R

/**
 * A summary of the funding instrument (FI) that PayPal will charge for a vaulted buyer.
 *
 * Used by [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.SavePayPalPaymentMethodView]
 * to render the FI chip. Maps 1:1 to the BT backend `fetch_fi` / `fetch_selected_fi` (Atmosphere
 * `getSavedPaypalPaymentMethod`) response fields (`label`, `imageUrl`, `lastDigits`, `type`).
 *
 * @property type       the FI category (e.g. "CARD", "BANK") returned by the backend. Drives the
 * tile icon; unrecognized values (e.g. Pay Later products) render no icon.
 * @property label      the FI display name (e.g. "Visa", "CREDIT UNION 1", "Pay in 4"). Shown as
 * the chip text when [lastDigits] is unavailable (Pay Later products).
 * @property lastDigits the last digits of the FI; `null` for products with no masked number (Pay
 * Later). When present, the chip shows `••<lastDigits>` instead of [label].
 * @property imageUrl   remote brand-art URL returned by the backend; optional. Not loaded yet — the
 * remote image loader (and brand-specific art / customization) lands in a future PR.
 */
data class FiSummary(
    val type: String,
    val label: String,
    val lastDigits: String? = null,
    val imageUrl: String? = null,
) {

    /**
     * The tile icon drawable, or `null` when the tile shows no icon (Pay Later products render the
     * product name only).
     *
     * Cards and banks show the generic placeholder glyph. Brand-specific art (from [imageUrl]) is a
     * future PR that adds the remote image loader and styling customization.
     */
    @get:DrawableRes
    internal val iconRes: Int?
        get() = when (type.uppercase()) {
            TYPE_CARD -> R.drawable.edit_fi_generic_card
            TYPE_BANK -> R.drawable.edit_fi_generic_bank
            // Pay Later products (Pay in 4 / Pay Monthly): product name only, no icon.
            else -> null
        }

    internal companion object {
        private const val TYPE_CARD = "CARD"
        private const val TYPE_BANK = "BANK"
    }
}
