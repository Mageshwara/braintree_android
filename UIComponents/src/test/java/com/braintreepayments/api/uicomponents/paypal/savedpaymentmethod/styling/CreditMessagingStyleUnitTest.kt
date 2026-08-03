package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.styling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditMessagingStyleUnitTest {

    // region defaults

    @Test
    fun `enabled defaults to true`() {
        assertTrue(CreditMessagingStyle().enabled)
    }

    @Test
    fun `fontSizeSp defaults to 16`() {
        assertEquals(16f, CreditMessagingStyle().fontSizeSp)
    }

    @Test
    fun `lineHeightRatio defaults to 1_5`() {
        assertEquals(1.5f, CreditMessagingStyle().lineHeightRatio)
    }

    // endregion

    // region overrides

    @Test
    fun `enabled can be overridden to false`() {
        assertEquals(false, CreditMessagingStyle(enabled = false).enabled)
    }

    @Test
    fun `fontSizeSp can be overridden`() {
        assertEquals(20f, CreditMessagingStyle(fontSizeSp = 20f).fontSizeSp)
    }

    @Test
    fun `lineHeightRatio can be overridden`() {
        assertEquals(1.2f, CreditMessagingStyle(lineHeightRatio = 1.2f).lineHeightRatio)
    }

    // endregion

    // region equality (data class contract other code may rely on, e.g. recomposition skipping)

    @Test
    fun `equal property values produce equal instances`() {
        val a = CreditMessagingStyle(enabled = true, fontSizeSp = 18f, lineHeightRatio = 1.4f)
        val b = CreditMessagingStyle(enabled = true, fontSizeSp = 18f, lineHeightRatio = 1.4f)
        assertEquals(a, b)
    }

    @Test
    fun `differing lineHeightRatio produces unequal instances`() {
        val a = CreditMessagingStyle(lineHeightRatio = 1.5f)
        val b = CreditMessagingStyle(lineHeightRatio = 1.6f)
        assertTrue(a != b)
    }

    // endregion
}
