package com.braintreepayments.api.paypal

/**
 * A summary of the funding instrument (FI) that PayPal will charge for a vaulted buyer.
 *
 * Maps 1:1 to the BT backend `fetch_fi` / `fetch_selected_fi` (Atmosphere
 * `getSavedPaypalPaymentMethod`) response fields (`label`, `imageUrl`, `lastDigits`, `type`).
 *
 * @property type       the FI category (e.g. "CARD", "BANK") returned by the backend. Unrecognized
 * values (e.g. Pay Later products) render no icon.
 * @property label      the FI display name (e.g. "Visa", "CREDIT UNION 1", "Pay in 4"). Shown as
 * the chip text when [lastDigits] is unavailable (Pay Later products).
 * @property lastDigits the last digits of the FI; `null` for products with no masked number (Pay
 * Later). When present, the chip shows `••<lastDigits>` instead of [label].
 * @property imageUrl   remote brand-art URL returned by the backend; optional. Not loaded yet — the
 * remote image loader (and brand-specific art / customization) lands in a future PR.
 */
data class PayPalPaymentMethodSummary(
    val type: String,
    val label: String,
    val lastDigits: String? = null,
    val imageUrl: String? = null,
)
