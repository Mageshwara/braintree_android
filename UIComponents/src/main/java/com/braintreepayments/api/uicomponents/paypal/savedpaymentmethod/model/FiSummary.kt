package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model

import androidx.annotation.DrawableRes
import com.braintreepayments.api.uicomponents.R

/**
 * A summary of the funding instrument (FI) that PayPal will charge for a vaulted buyer.
 *
 * Used by [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.SavedPaymentMethodView]
 * to render the FI chip. Populated from the BT backend `fetch_fi` / `fetch_selected_fi` response.
 *
 * @property brand    the FI brand (e.g. "Visa", "Mastercard"); `null` when unknown.
 * @property last4    the last four digits of the FI; `null` when unavailable.
 * @property type     the FI category (card / bank / Pay Later). Drives the tile icon and, for
 * Pay Later products, whether a product name is shown instead of a masked number.
 * @property displayName the product name to show instead of the masked number, e.g. "Pay in 4",
 * "Pay Monthly". Used by [FiType.PAY_LATER] tiles; `null` for card / bank tiles (which show
 * `••last4`).
 * @property imageUrl remote brand-art URL returned by the backend; optional. Not loaded yet — the
 * remote image loader (and brand-specific art / customization) lands in a future PR.
 */
data class FiSummary(
    val brand: String? = null,
    val last4: String? = null,
    val type: FiType = FiType.CARD,
    val displayName: String? = null,
    val imageUrl: String? = null,
) {

    /**
     * The tile icon drawable, or `null` when the tile shows no icon (Pay Later products render the
     * product name only).
     *
     * Cards and banks show the generic placeholder glyph. Brand-specific art (from [brand] /
     * [imageUrl]) is a future PR that adds the remote image loader and styling customization.
     */
    @get:DrawableRes
    internal val iconRes: Int?
        get() = when (type) {
            FiType.CARD -> R.drawable.edit_fi_generic_card
            FiType.BANK -> R.drawable.edit_fi_generic_bank
            // Pay Later products (Pay in 4 / Pay Monthly): product name only, no icon.
            FiType.PAY_LATER -> null
        }
}

/**
 * The category of a [FiSummary], used to pick the tile icon and label style.
 *
 * - [CARD] / [BANK]: show the generic card / bank placeholder glyph + `••last4`.
 * - [PAY_LATER]: Pay Later product (e.g. Pay in 4, Pay Monthly) — show the product
 *   [FiSummary.displayName] only, with no icon.
 */
enum class FiType {
    CARD,
    BANK,
    PAY_LATER,
}
