package com.braintreepayments.api.paypalsavedpaymentmethod

/**
 * The buyer's PayPal account details, returned when no funding instrument is available to
 * display and the component falls back to showing the buyer's email address.
 *
 * @property email the buyer's PayPal account email address
 * @property editable whether the edit pencil should still be shown alongside the fallback email
 */
data class Payer(
    val email: String,
    val editable: Boolean
)
