package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodStyleResolverUnitTest {

    @Test
    fun `fully null style resolves to all SDK defaults`() {
        val resolved = PayPalSavedPaymentMethodStyleResolver(PayPalSavedPaymentMethodViewStyle())

        assertEquals(true, resolved.showLogo)
        assertEquals(true, resolved.showLabel)
        assertEquals(true, resolved.showCreditMessaging)

        assertEquals(Color.WHITE, resolved.backgroundColor)
        assertEquals(Color.parseColor("#222222"), resolved.textColor)
        assertNull(resolved.fontResId)

        assertNull(resolved.heightDp)
        assertEquals(0f, resolved.horizontalPaddingDp)
        assertEquals(10f, resolved.verticalPaddingDp)
        assertEquals(0f, resolved.cornerRadiusDp)
        assertEquals(Color.TRANSPARENT, resolved.borderColor)
        assertEquals(0f, resolved.borderWidthDp)

        assertEquals(24f, resolved.logoWidthDp)

        assertEquals(20f, resolved.labelFontSizeSp)
        assertEquals(6f, resolved.labelMarginStartDp)

        assertEquals(14f, resolved.fundingInstrumentTextFontSizeSp)
        assertEquals(16f, resolved.editIconSizeDp)
        assertEquals(12f, resolved.fundingInstrumentMarginStartDp)

        assertEquals(16f, resolved.creditMessagingFontSizeSp)
        assertNull(resolved.creditMessagingLinkColor)
    }

    @Test
    fun `explicit values override SDK defaults`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            showPayPalLogo = false,
            componentAppearance = ComponentAppearance(
                backgroundColor = Color.BLUE,
                textColor = Color.RED
            ),
            container = ContainerStyle(
                horizontalPaddingDp = 5f,
                logo = PayPalLogoStyle(widthDp = 32f),
                label = PayPalLabelStyle(fontSizeSp = 18f, marginStartDp = 4f),
                fundingInstrument = FundingInstrumentStyle(textFontSizeSp = 13f),
                creditMessaging = CreditMessagingStyle(linkColor = Color.GREEN)
            )
        )

        val resolved = PayPalSavedPaymentMethodStyleResolver(style)

        assertEquals(false, resolved.showLogo)
        assertEquals(Color.BLUE, resolved.backgroundColor)
        assertEquals(Color.RED, resolved.textColor)
        assertEquals(5f, resolved.horizontalPaddingDp)
        assertEquals(32f, resolved.logoWidthDp)
        assertEquals(18f, resolved.labelFontSizeSp)
        assertEquals(4f, resolved.labelMarginStartDp)
        assertEquals(13f, resolved.fundingInstrumentTextFontSizeSp)
        assertEquals(Color.GREEN, resolved.creditMessagingLinkColor)

        // untouched fields still resolve to their own SDK defaults
        assertEquals(10f, resolved.verticalPaddingDp)
        assertEquals(16f, resolved.editIconSizeDp)
    }

    @Test
    fun `baseFontSizeSp fills in for font fields left unset, but not non-font fields`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 22f),
            container = ContainerStyle(
                // labelFontSizeSp explicitly set -> wins over baseFontSizeSp
                label = PayPalLabelStyle(fontSizeSp = 15f)
                // fundingInstrument + creditMessaging font sizes left unset -> fall back to baseFontSizeSp
            )
        )

        val resolved = PayPalSavedPaymentMethodStyleResolver(style)

        assertEquals(15f, resolved.labelFontSizeSp)
        assertEquals(22f, resolved.fundingInstrumentTextFontSizeSp)
        assertEquals(22f, resolved.creditMessagingFontSizeSp)

        // baseFontSizeSp must never affect non-font-size fields
        assertEquals(12f, resolved.fundingInstrumentMarginStartDp)
    }

    @Test
    fun `no baseFontSizeSp and no explicit value falls back to SDK per-element default`() {
        val resolved = PayPalSavedPaymentMethodStyleResolver(
            PayPalSavedPaymentMethodViewStyle(componentAppearance = ComponentAppearance())
        )

        assertEquals(20f, resolved.labelFontSizeSp)
        assertEquals(14f, resolved.fundingInstrumentTextFontSizeSp)
        assertEquals(16f, resolved.creditMessagingFontSizeSp)
    }
}
