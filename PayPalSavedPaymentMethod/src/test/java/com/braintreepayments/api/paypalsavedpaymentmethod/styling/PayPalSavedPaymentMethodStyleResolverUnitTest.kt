package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodStyleResolverUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun resolve(style: PayPalSavedPaymentMethodViewStyle) =
        PayPalSavedPaymentMethodStyleResolver(style, context)

    private fun colorDefault(colorRes: Int) = ContextCompat.getColor(context, colorRes)

    private fun dimenDpDefault(dimenRes: Int) =
        context.resources.getDimension(dimenRes) / context.resources.displayMetrics.density

    @Suppress("DEPRECATION")
    private fun dimenSpDefault(dimenRes: Int) =
        context.resources.getDimension(dimenRes) / context.resources.displayMetrics.scaledDensity

    @Test
    fun resolve_whenAllFieldsNull_usesSdkVisibilityAndColorDefaults() {
        val resolved = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(true, resolved.showLogo)
        assertEquals(true, resolved.showLabel)
        assertEquals(true, resolved.showCreditMessaging)
        assertEquals(
            colorDefault(R.color.paypal_saved_payment_method_style_default_background),
            resolved.backgroundColor
        )
        assertEquals(
            colorDefault(R.color.paypal_saved_payment_method_style_default_text_color),
            resolved.textColor
        )
        assertNull(resolved.fontResId)
        assertNull(resolved.heightDp)
        assertEquals(
            colorDefault(R.color.paypal_saved_payment_method_style_default_border_color),
            resolved.borderColor
        )
        assertNull(resolved.creditMessagingLinkColor)
    }

    @Test
    fun resolve_whenAllFieldsNull_usesSdkContainerDimensionDefaults() {
        val resolved = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_horizontal_padding),
            resolved.horizontalPaddingDp
        )
        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_vertical_padding),
            resolved.verticalPaddingDp
        )
        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_corner_radius),
            resolved.cornerRadiusDp
        )
        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_border_width),
            resolved.borderWidthDp
        )
        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_logo_width),
            resolved.logoWidthDp
        )
    }

    @Test
    fun resolve_whenAllFieldsNull_usesSdkTextAndCreditMessagingDefaults() {
        val resolved = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(
            dimenSpDefault(R.dimen.paypal_saved_payment_method_style_default_label_font_size),
            resolved.labelFontSizeSp
        )
        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_label_margin_start),
            resolved.labelMarginStartDp
        )
        assertEquals(
            dimenSpDefault(R.dimen.paypal_saved_payment_method_style_default_funding_instrument_text_font_size),
            resolved.fundingInstrumentTextFontSizeSp
        )
        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_edit_icon_size),
            resolved.editIconSizeDp
        )
        assertEquals(
            dimenDpDefault(R.dimen.paypal_saved_payment_method_style_default_funding_instrument_margin_start),
            resolved.fundingInstrumentMarginStartDp
        )
        assertEquals(
            dimenSpDefault(R.dimen.paypal_saved_payment_method_style_default_credit_messaging_font_size),
            resolved.creditMessagingFontSizeSp
        )
    }

    @Test
    fun resolve_whenVisibilityTogglesSet_usesMerchantValues() {
        val style = PayPalSavedPaymentMethodViewStyle(
            showPayPalLogo = false,
            showPayPalLabel = false,
            showPayPalCreditMessaging = false
        )

        val resolved = resolve(style)

        assertEquals(false, resolved.showLogo)
        assertEquals(false, resolved.showLabel)
        assertEquals(false, resolved.showCreditMessaging)
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

        val resolved = resolve(style)

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

        val resolved = resolve(style)

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

        assertEquals(32f, resolve(style).logoWidthDp)
    }

    @Test
    fun resolve_labelFontSize_whenElementValueSet_ignoresBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f),
            container = ContainerStyle(label = PayPalLabelStyle(fontSizeSp = 18f))
        )

        assertEquals(18f, resolve(style).labelFontSizeSp)
    }

    @Test
    fun resolve_labelFontSize_whenElementNullAndBaseSet_usesBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f)
        )

        assertEquals(30f, resolve(style).labelFontSizeSp)
    }

    @Test
    fun resolve_labelFontSize_whenNeitherSet_usesElementDefault() {
        val resolved = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(
            dimenSpDefault(R.dimen.paypal_saved_payment_method_style_default_label_font_size),
            resolved.labelFontSizeSp
        )
    }

    @Test
    fun resolve_labelMarginStart_whenSet_usesMerchantValue() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = ContainerStyle(label = PayPalLabelStyle(marginStartDp = 5f))
        )

        assertEquals(5f, resolve(style).labelMarginStartDp)
    }

    @Test
    fun resolve_fundingInstrumentTextFontSize_whenElementValueSet_ignoresBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f),
            container = ContainerStyle(fundingInstrument = FundingInstrumentStyle(textFontSizeSp = 22f))
        )

        assertEquals(22f, resolve(style).fundingInstrumentTextFontSizeSp)
    }

    @Test
    fun resolve_fundingInstrumentTextFontSize_whenElementNullAndBaseSet_usesBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f)
        )

        assertEquals(30f, resolve(style).fundingInstrumentTextFontSizeSp)
    }

    @Test
    fun resolve_fundingInstrumentTextFontSize_whenNeitherSet_usesElementDefault() {
        val resolved = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(
            dimenSpDefault(R.dimen.paypal_saved_payment_method_style_default_funding_instrument_text_font_size),
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

        val resolved = resolve(style)

        assertEquals(20f, resolved.editIconSizeDp)
        assertEquals(10f, resolved.fundingInstrumentMarginStartDp)
    }

    @Test
    fun resolve_creditMessagingFontSize_whenElementValueSet_ignoresBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f),
            container = ContainerStyle(creditMessaging = CreditMessagingStyle(fontSizeSp = 12f))
        )

        assertEquals(12f, resolve(style).creditMessagingFontSizeSp)
    }

    @Test
    fun resolve_creditMessagingFontSize_whenElementNullAndBaseSet_usesBaseFontSize() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = ComponentAppearance(baseFontSizeSp = 30f)
        )

        assertEquals(30f, resolve(style).creditMessagingFontSizeSp)
    }

    @Test
    fun resolve_creditMessagingFontSize_whenNeitherSet_usesElementDefault() {
        val resolved = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(
            dimenSpDefault(R.dimen.paypal_saved_payment_method_style_default_credit_messaging_font_size),
            resolved.creditMessagingFontSizeSp
        )
    }

    @Test
    fun resolve_creditMessagingLinkColor_whenSet_usesMerchantValue() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = ContainerStyle(creditMessaging = CreditMessagingStyle(linkColor = Color.MAGENTA))
        )

        assertEquals(Color.MAGENTA, resolve(style).creditMessagingLinkColor)
    }

    @Test
    fun resolve_creditMessagingLinkColor_whenNull_staysNull() {
        val resolved = resolve(PayPalSavedPaymentMethodViewStyle())

        assertNull(resolved.creditMessagingLinkColor)
    }
}
