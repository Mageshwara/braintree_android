package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model

import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FiSummaryUnitTest {

    // Sanity: R drawable ids must be distinct here, otherwise the assertions below are meaningless.
    @Test
    fun `drawable ids are distinct`() {
        assertNotEquals(R.drawable.edit_fi_generic_bank, R.drawable.edit_fi_generic_card)
    }

    // region iconRes - type resolution

    @Test
    fun `iconRes returns generic bank glyph for BANK`() {
        assertEquals(
            R.drawable.edit_fi_generic_bank,
            FiSummary(type = "BANK", label = "Bank", lastDigits = "3339").iconRes
        )
    }

    @Test
    fun `iconRes is case-insensitive`() {
        assertEquals(
            R.drawable.edit_fi_generic_bank,
            FiSummary(type = "bank", label = "Bank", lastDigits = "3339").iconRes
        )
    }

    @Test
    fun `iconRes returns null for unrecognized type`() {
        assertNull(FiSummary(type = "PAY_LATER", label = "Pay in 4").iconRes)
    }

    // endregion

    // region iconRes - card

    @Test
    fun `iconRes returns generic card glyph for CARD`() {
        assertEquals(
            R.drawable.edit_fi_generic_card,
            FiSummary(type = "CARD", label = "Visa", lastDigits = "3339").iconRes
        )
    }

    // endregion
}
