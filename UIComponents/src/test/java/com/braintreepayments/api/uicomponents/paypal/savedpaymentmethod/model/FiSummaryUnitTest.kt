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
            FiSummary(last4 = "3339", type = FiType.BANK).iconRes
        )
    }

    @Test
    fun `iconRes returns null for PAY_LATER`() {
        assertNull(FiSummary(type = FiType.PAY_LATER, displayName = "Pay in 4").iconRes)
    }

    // endregion

    // region iconRes - card

    @Test
    fun `iconRes returns generic card glyph for CARD regardless of brand`() {
        // Brand-specific art (from brand / imageUrl) is a future PR; cards show the generic glyph.
        assertEquals(
            R.drawable.edit_fi_generic_card,
            FiSummary(brand = "Visa", last4 = "3339", type = FiType.CARD).iconRes
        )
    }

    @Test
    fun `iconRes returns generic card glyph for null brand`() {
        assertEquals(
            R.drawable.edit_fi_generic_card,
            FiSummary(brand = null, last4 = "4242", type = FiType.CARD).iconRes
        )
    }

    // endregion
}
