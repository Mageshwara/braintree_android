package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.Payer
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalSavedPaymentMethodDisplayStateUnitTest {

    @Test
    fun toDisplayState_whenInstrumentPresent_returnsContent() {
        val instrument = PayPalSavedPaymentMethod(
            label = "CREDIT UNION 1",
            imageUrl = "https://x/generic_bank.png",
            lastDigits = "3357",
            type = "BANK",
            subtype = null
        )
        val summary = PayPalSavedPaymentMethodSummary(
            paypalPayer = null,
            paypalSavedPaymentMethods = listOf(instrument)
        )

        val displayState = summary.toDisplayState()

        assertTrue(displayState is PayPalSavedPaymentMethodDisplayState.Content)
        assertEquals(instrument, (displayState as PayPalSavedPaymentMethodDisplayState.Content).paymentMethod)
    }

    @Test
    fun toDisplayState_whenNoInstrumentButPayerPresent_returnsNoFi() {
        val summary = PayPalSavedPaymentMethodSummary(
            paypalPayer = Payer(email = "buyer@example.com", editable = true),
            paypalSavedPaymentMethods = emptyList()
        )

        val displayState = summary.toDisplayState()

        assertTrue(displayState is PayPalSavedPaymentMethodDisplayState.NoFi)
        assertEquals(
            "buyer@example.com",
            (displayState as PayPalSavedPaymentMethodDisplayState.NoFi).buyerEmail
        )
    }

    @Test
    fun toDisplayState_whenNoInstrumentAndNoPayer_returnsError() {
        val summary = PayPalSavedPaymentMethodSummary(
            paypalPayer = null,
            paypalSavedPaymentMethods = emptyList()
        )

        val displayState = summary.toDisplayState()

        assertTrue(displayState is PayPalSavedPaymentMethodDisplayState.Error)
    }
}
