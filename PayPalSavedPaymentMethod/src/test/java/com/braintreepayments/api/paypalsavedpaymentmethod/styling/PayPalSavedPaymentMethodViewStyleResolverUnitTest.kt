package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodViewStyleResolverUnitTest {

    @Test
    fun resolve_whenAllFieldsNull_usesSdkDefaults() {
        val resolved = PayPalSavedPaymentMethodViewStyle().resolve()

        assertEquals(true, resolved.showPayPalLogo)
        assertEquals(true, resolved.showPayPalLabel)
        assertEquals(true, resolved.showPayPalCreditMessaging)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.BACKGROUND_COLOR, resolved.backgroundColor)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.TEXT_COLOR, resolved.textColor)
        assertNull(resolved.fontResId)
        assertNull(resolved.heightDp)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.HORIZONTAL_PADDING_DP, resolved.horizontalPaddingDp)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.VERTICAL_PADDING_DP, resolved.verticalPaddingDp)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.CORNER_RADIUS_DP, resolved.cornerRadiusDp)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.BORDER_COLOR, resolved.borderColor)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.BORDER_WIDTH_DP, resolved.borderWidthDp)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.LOGO_WIDTH_DP, resolved.logoWidthDp)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.LABEL_FONT_SIZE_SP, resolved.labelFontSizeSp)
        assertEquals(PayPalSavedPaymentMethodStyleDefaults.LABEL_MARGIN_START_DP, resolved.labelMarginStartDp)
        assertEquals(
            PayPalSavedPaymentMethodStyleDefaults.FUNDING_INSTRUMENT_TEXT_FONT_SIZE_SP,
            resolved.fundingInstrumentTextFontSizeSp
        )
        assertEquals(
            PayPalSavedPaymentMethodStyleDefaults.FUNDING_INSTRUMENT_EDIT_ICON_SIZE_DP,
            resolved.fundingInstrumentEditIconSizeDp
        )
        assertEquals(
            PayPalSavedPaymentMethodStyleDefaults.FUNDING_INSTRUMENT_MARGIN_START_DP,
            resolved.fundingInstrumentMarginStartDp
        )
        assertEquals(
            PayPalSavedPaymentMethodStyleDefaults.CREDIT_MESSAGING_FONT_SIZE_SP,
            resolved.creditMessagingFontSizeSp
        )
        assertNull(resolved.creditMessagingLinkColor)
    }

    @Test
    fun resolve_whenVisibilityTogglesSet_usesMerchantValues() {
        val style = PayPalSavedPaymentMethodViewStyle(
            showPayPalLogo = false,
            showPayPalLabel = false,
            showPayPalCreditMessaging = false
        )

        val resolved = style.resolve()

        assertEquals(false, resolved.showPayPalLogo)
        assertEquals(false, resolved.showPayPalLabel)
        assertEquals(false, resolved.showPayPalCreditMessaging)
    }

    @Test
    fun resolve_whenComponentAppearanceSet_usesMerchantValues() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(
                backgroundColor = Color.RED,
                textColor = Color.BLUE,
                fontResId = 42
            )
        )

        val resolved = style.resolve()

        assertEquals(Color.RED, resolved.backgroundColor)
        assertEquals(Color.BLUE, resolved.textColor)
        assertEquals(42, resolved.fontResId)
    }

    @Test
    fun resolve_whenContainerFieldsSet_usesMerchantValues() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = ContainerStyle(
                heightDp = 60f,
                horizontalPaddingDp = 12f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 4f,
                borderColor = Color.GREEN,
                borderWidthDp = 2f
            )
        )

        val resolved = style.resolve()

        assertEquals(60f, resolved.heightDp)
        assertEquals(12f, resolved.horizontalPaddingDp)
        assertEquals(8f, resolved.verticalPaddingDp)
        assertEquals(4f, resolved.cornerRadiusDp)
        assertEquals(Color.GREEN, resolved.borderColor)
        assertEquals(2f, resolved.borderWidthDp)
    }

    @Test
    fun resolve_whenLogoWidthSet_usesMerchantValue() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = ContainerStyle(logo = PayPalLogoStyle(widthDp = 32f))
        )

        val resolved = style.resolve()

        assertEquals(32f, resolved.logoWidthDp)
    }

    @Test
    fun resolve_labelFontSize_whenElementValueSet_ignoresBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f),
            container = ContainerStyle(label = PayPalLabelStyle(fontSizeSp = 18f))
        )

        assertEquals(18f, style.resolve().labelFontSizeSp)
    }

    @Test
    fun resolve_labelFontSize_whenElementNullAndBaseSet_usesBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f)
        )

        assertEquals(30f, style.resolve().labelFontSizeSp)
    }

    @Test
    fun resolve_labelFontSize_whenNeitherSet_usesElementDefault() {
        val resolved = PayPalSavedPaymentMethodViewStyle().resolve()

        assertEquals(PayPalSavedPaymentMethodStyleDefaults.LABEL_FONT_SIZE_SP, resolved.labelFontSizeSp)
    }

    @Test
    fun resolve_labelMarginStart_whenSet_usesMerchantValue() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = ContainerStyle(label = PayPalLabelStyle(marginStartDp = 5f))
        )

        assertEquals(5f, style.resolve().labelMarginStartDp)
    }

    @Test
    fun resolve_fundingInstrumentTextFontSize_whenElementValueSet_ignoresBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f),
            container = ContainerStyle(fundingInstrument = FundingInstrumentStyle(textFontSizeSp = 22f))
        )

        assertEquals(22f, style.resolve().fundingInstrumentTextFontSizeSp)
    }

    @Test
    fun resolve_fundingInstrumentTextFontSize_whenElementNullAndBaseSet_usesBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f)
        )

        assertEquals(30f, style.resolve().fundingInstrumentTextFontSizeSp)
    }

    @Test
    fun resolve_fundingInstrumentTextFontSize_whenNeitherSet_usesElementDefault() {
        val resolved = PayPalSavedPaymentMethodViewStyle().resolve()

        assertEquals(
            PayPalSavedPaymentMethodStyleDefaults.FUNDING_INSTRUMENT_TEXT_FONT_SIZE_SP,
            resolved.fundingInstrumentTextFontSizeSp
        )
    }

    @Test
    fun resolve_fundingInstrumentEditIconSizeAndMarginStart_whenSet_usesMerchantValues() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = ContainerStyle(
                fundingInstrument = FundingInstrumentStyle(editIconSizeDp = 20f, marginStartDp = 10f)
            )
        )

        val resolved = style.resolve()

        assertEquals(20f, resolved.fundingInstrumentEditIconSizeDp)
        assertEquals(10f, resolved.fundingInstrumentMarginStartDp)
    }

    @Test
    fun resolve_creditMessagingFontSize_whenElementValueSet_ignoresBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f),
            container = ContainerStyle(creditMessaging = CreditMessagingStyle(fontSizeSp = 12f))
        )

        assertEquals(12f, style.resolve().creditMessagingFontSizeSp)
    }

    @Test
    fun resolve_creditMessagingFontSize_whenElementNullAndBaseSet_usesBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f)
        )

        assertEquals(30f, style.resolve().creditMessagingFontSizeSp)
    }

    @Test
    fun resolve_creditMessagingFontSize_whenNeitherSet_usesElementDefault() {
        val resolved = PayPalSavedPaymentMethodViewStyle().resolve()

        assertEquals(
            PayPalSavedPaymentMethodStyleDefaults.CREDIT_MESSAGING_FONT_SIZE_SP,
            resolved.creditMessagingFontSizeSp
        )
    }

    @Test
    fun resolve_creditMessagingLinkColor_whenSet_usesMerchantValue() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = ContainerStyle(creditMessaging = CreditMessagingStyle(linkColor = Color.MAGENTA))
        )

        assertEquals(Color.MAGENTA, style.resolve().creditMessagingLinkColor)
    }

    @Test
    fun resolve_creditMessagingLinkColor_whenNull_staysNull() {
        val resolved = PayPalSavedPaymentMethodViewStyle().resolve()

        assertNull(resolved.creditMessagingLinkColor)
    }
}
