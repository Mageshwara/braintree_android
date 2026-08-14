package com.braintreepayments.api.paypalsavedpaymentmethod

/**
 * A single funding instrument on the buyer's PayPal account, used to render the FI pill
 * (card art, masked last four, brand label).
 *
 * @property label the funding instrument's display label (e.g. "Visa")
 * @property imageUrl a URL for the funding instrument's brand art; if this fails to load, the
 * component falls back to a generic bank/card glyph rather than treating the load failure as a
 * distinct state
 * @property lastDigits the last four digits of the funding instrument, already unmasked from the
 * server; the component is responsible for applying its own masking prefix when rendering
 * @property type the funding instrument's type (e.g. "CARD", "BANK")
 * @property subtype the funding instrument's subtype, when applicable (e.g. a card's network)
 */
data class PayPalSavedpaymentMethod(
    val label: String,
    val imageUrl: String,
    val lastDigits: String?,
    val type: String,
    val subtype: String?
)
