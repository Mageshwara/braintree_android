@file:Suppress("TooManyFunctions")

package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DimenRes
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedpaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.wrapContentHeight

/**
 * Entry point for merchants — owns fetching the sticky FI and launching the edit-FI PayPal
 * payment auth flow, then renders the [displayState]-driven content view.
 *
 * State/client wiring (fetch on first composition, edit-pencil launching the auth flow, tokenize
 * result delivery) is stubbed for now and lands in a follow-up pass.
 *
 * @param payPalCheckoutRequest a [PayPalCheckoutRequest] used to customize the edit-FI checkout.
 * @param authorization a Tokenization Key or Client Token used to authenticate.
 * @param appLinkReturnUrl a [Uri] containing the Android App Link used to return to your app.
 * @param deepLinkFallbackUrlScheme a return url scheme used as a deep link fallback.
 * @param style merchant styling (see [PayPalSavedPaymentMethodViewStyle]).
 * @param paypalTokenizeCallback invoked with the result of the edit-FI tokenization.
 */
@ExperimentalBetaApi
@Composable
fun PayPalSavedPaymentMethodView(
    payPalCheckoutRequest: PayPalCheckoutRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle(),
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    // TODO: own PayPalSavedPaymentMethodClient/PayPalLauncher, fetch the sticky FI on first
    // composition, and launch the edit auth flow on pencil-click — deferred to a follow-up PR.
    var displayState by remember {
        mutableStateOf<PayPalSavedPaymentMethodDisplayState>(PayPalSavedPaymentMethodDisplayState.Loading)
    }

    PayPalSavedPaymentMethodViewContent(
        displayState = displayState,
        style = style
    )
}

private fun shouldShowCreditMessaging(
    hideFiRow: Boolean,
    style: PayPalSavedPaymentMethodViewStyle,
    isCreditMessageLoading: Boolean,
    creditMessage: String?,
    creditMessageLinkLabel: String?
): Boolean = !hideFiRow && style.showCreditMessaging &&
    (isCreditMessageLoading || (creditMessage != null && creditMessageLinkLabel != null))

/**
 * Presentational view for the saved/editable PayPal funding instrument row — logo, "PayPal"
 * label, an FI cluster (icon + masked instrument + edit affordance) that reflects [displayState],
 * and an optional Pay Later credit-messaging line below it. Fully driven by [style]; contains no
 * networking or business logic.
 *
 * Real card-art loading is not yet wired — the FI cluster always falls back to a generic type
 * icon regardless of [PayPalSavedpaymentMethod.imageUrl].
 *
 * @param isCreditMessageLoading true while the credit-messaging fetch is in flight; renders a
 * shimmer placeholder in place of the row.
 * @param creditMessage the compliance-provided messaging copy (e.g. "As low as $10/mo"); the
 * credit-messaging row is hidden when this is null or [PayPalSavedPaymentMethodViewStyle.showCreditMessaging]
 * is false.
 * @param creditMessageLinkLabel the trailing link text (e.g. "Learn more").
 * @param onCreditMessageLinkClick invoked when the credit-messaging link is tapped.
 */
@ExperimentalBetaApi
@Composable
fun PayPalSavedPaymentMethodViewContent(
    displayState: PayPalSavedPaymentMethodDisplayState,
    modifier: Modifier = Modifier,
    editContentDescription: String? = null,
    style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle(),
    onEditClick: () -> Unit = {},
    isCreditMessageLoading: Boolean = false,
    creditMessage: String? = null,
    creditMessageLinkLabel: String? = null,
    onCreditMessageLinkClick: () -> Unit = {}
) {
    // NoNetwork and Error both hide the FI row but keep the PayPal brand mark visible.
    val hideFiRow = displayState is PayPalSavedPaymentMethodDisplayState.Error ||
        displayState is PayPalSavedPaymentMethodDisplayState.NoNetwork
    val container = style.container
    val textColor = Color(style.theme.textColorBase)
    val showCreditMessaging = shouldShowCreditMessaging(
        hideFiRow = hideFiRow,
        style = style,
        isCreditMessageLoading = isCreditMessageLoading,
        creditMessage = creditMessage,
        creditMessageLinkLabel = creditMessageLinkLabel
    )

    Card(
        modifier = modifier
            .let { if (container.heightDp != null) it.height(container.heightDp.dp) else it.wrapContentHeight() },
        shape = RoundedCornerShape(container.cornerRadiusDp.dp),
        colors = CardDefaults.cardColors(containerColor = Color(style.cardColor)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (container.borderWidthDp > 0f) {
            BorderStroke(container.borderWidthDp.dp, Color(container.borderColor))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = container.horizontalPaddingDp.dp, vertical = container.verticalPaddingDp.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (style.showLogo) {
                Image(
                    painter = painterResource(R.drawable.ic_paypal_brand_logo),
                    contentDescription = stringResource(R.string.paypal_saved_payment_method_label),
                    modifier = Modifier.width(container.logo.widthDp.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (style.showLabel) {
                        Text(
                            text = stringResource(R.string.paypal_saved_payment_method_label),
                            color = textColor,
                            fontSize = container.label.fontSizeSp.sp,
                            lineHeight = spDimensionResource(R.dimen.paypal_saved_payment_method_label_line_height),
                            fontWeight = FontWeight.Medium,
                            fontFamily = style.theme.fontResId?.let { FontFamily(Font(it)) } ?: FontFamily.Default,
                            modifier = Modifier.padding(start = container.label.marginStartDp.dp)
                        )
                    }

                    if (!hideFiRow) {
                        FiCluster(
                            displayState = displayState,
                            style = style,
                            editContentDescription = editContentDescription,
                            onEditClick = onEditClick,
                            modifier = Modifier.padding(start = container.fiCluster.marginStartDp.dp)
                        )
                    }
                }

                if (showCreditMessaging) {
                    CreditMessagingSection(
                        isLoading = isCreditMessageLoading,
                        message = creditMessage,
                        linkLabel = creditMessageLinkLabel,
                        style = style,
                        onLinkClick = onCreditMessageLinkClick,
                        modifier = Modifier
                            .padding(
                                start = container.label.marginStartDp.dp,
                                top = dimensionResource(
                                    R.dimen.paypal_saved_payment_method_credit_messaging_spacing
                                ),
                                end = dimensionResource(
                                    R.dimen.paypal_saved_payment_method_credit_messaging_end_padding
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditMessagingSection(
    isLoading: Boolean,
    message: String?,
    linkLabel: String?,
    style: PayPalSavedPaymentMethodViewStyle,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        CreditMessagingPlaceholder(modifier = modifier)
    } else {
        CreditMessagingRow(
            message = message.orEmpty(),
            linkLabel = linkLabel.orEmpty(),
            style = style,
            onLinkClick = onLinkClick,
            modifier = modifier
        )
    }
}

@Composable
private fun CreditMessagingRow(
    message: String,
    linkLabel: String,
    style: PayPalSavedPaymentMethodViewStyle,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(style.theme.textColorBase)
    val linkColor = style.theme.linkColor?.let { Color(it) } ?: baseColor

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) { append(message) }
            append(" ")
            withStyle(
                SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(linkLabel)
            }
        },
        fontSize = style.container.creditMessaging.fontSizeSp.sp,
        lineHeight = spDimensionResource(R.dimen.paypal_saved_payment_method_credit_messaging_line_height),
        modifier = modifier.clickable(onClick = onLinkClick)
    )
}

@OptIn(ExperimentalBetaApi::class)
@Composable
private fun FiCluster(
    displayState: PayPalSavedPaymentMethodDisplayState,
    style: PayPalSavedPaymentMethodViewStyle,
    editContentDescription: String?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fiClusterStyle = style.container.fiCluster
    val textColor = Color(style.theme.textColorBase)
    val isLoading = displayState is PayPalSavedPaymentMethodDisplayState.Loading

    Row(
        modifier = modifier
            .let {
                if (isLoading) {
                    it
                } else {
                    it
                        .clip(RoundedCornerShape(fiClusterStyle.cornerRadiusDp.dp))
                        .background(Color(fiClusterStyle.backgroundColor))
                }
            }
            .padding(
                horizontal = fiClusterStyle.horizontalPaddingDp.dp,
                vertical = fiClusterStyle.verticalPaddingDp.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (displayState) {
            is PayPalSavedPaymentMethodDisplayState.Loading -> {
                FiClusterPlaceholder()
            }
            is PayPalSavedPaymentMethodDisplayState.Content -> {
                fiIconFor(displayState.paymentMethod.type)?.let { iconRes ->
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .width(fiClusterStyle.iconWidthDp.dp)
                            .padding(end = fiClusterStyle.horizontalPaddingDp.dp)
                    )
                }
                Text(
                    text = fiClusterText(displayState),
                    color = textColor,
                    fontSize = fiClusterStyle.textFontSizeSp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is PayPalSavedPaymentMethodDisplayState.NoFi -> {
                Text(
                    text = displayState.buyerEmail,
                    color = textColor,
                    fontSize = fiClusterStyle.textFontSizeSp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is PayPalSavedPaymentMethodDisplayState.NoNetwork,
            is PayPalSavedPaymentMethodDisplayState.Error -> Unit
        }

        if (!isLoading) {
            Image(
                painter = painterResource(R.drawable.ic_edit_pencil),
                contentDescription = editContentDescription,
                modifier = Modifier
                    .padding(start = fiClusterStyle.horizontalPaddingDp.dp)
                    .size(fiClusterStyle.editIconSizeDp.dp)
                    .clickable(onClick = onEditClick)
            )
        }
    }
}

@Composable
private fun FiClusterPlaceholder() {
    PayPalSavedPaymentMethodShimmerBox(
        modifier = Modifier.size(
            width = dimensionResource(R.dimen.paypal_saved_payment_method_placeholder_width),
            height = dimensionResource(R.dimen.paypal_saved_payment_method_placeholder_height)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.paypal_saved_payment_method_placeholder_corner_radius))
    )
}

@Composable
private fun CreditMessagingPlaceholder(modifier: Modifier = Modifier) {
    PayPalSavedPaymentMethodShimmerBox(
        modifier = modifier.size(
            width = dimensionResource(R.dimen.paypal_saved_payment_method_credit_messaging_placeholder_width),
            height = dimensionResource(R.dimen.paypal_saved_payment_method_credit_messaging_placeholder_height)
        ),
        shape = RoundedCornerShape(
            dimensionResource(R.dimen.paypal_saved_payment_method_credit_messaging_placeholder_corner_radius)
        )
    )
}

private fun fiIconFor(type: String?) = when (type?.uppercase()) {
    "CARD" -> R.drawable.ic_fi_card_placeholder
    "BANK" -> R.drawable.ic_fi_bank_placeholder
    else -> null
}

@OptIn(ExperimentalBetaApi::class)
@Composable
private fun fiClusterText(content: PayPalSavedPaymentMethodDisplayState.Content): String {
    val masked = content.paymentMethod.lastDigits?.let {
        stringResource(R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number, it)
    }
    return when {
        masked != null -> "${content.paymentMethod.label} $masked"
        else -> content.paymentMethod.label
    }
}

@Composable
private fun spDimensionResource(@DimenRes id: Int): TextUnit {
    val dp = dimensionResource(id)
    return with(LocalDensity.current) { dp.toSp() }
}

@OptIn(ExperimentalBetaApi::class)
@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewPayPalSavedPaymentMethodViewContent() {
    PayPalSavedPaymentMethodViewContent(
        displayState = PayPalSavedPaymentMethodDisplayState.Content(
            paymentMethod = PayPalSavedpaymentMethod(
                label = "",
                imageUrl = "",
                lastDigits = "3339",
                type = "CARD",
                subtype = null
            )
        )
    )
}

@OptIn(ExperimentalBetaApi::class)
@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewPayPalSavedPaymentMethodViewNoFi() {
    PayPalSavedPaymentMethodViewContent(
        displayState = PayPalSavedPaymentMethodDisplayState.NoFi(buyerEmail = "buyer@example.com")
    )
}

@OptIn(ExperimentalBetaApi::class)
@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewPayPalSavedPaymentMethodViewLoading() {
    PayPalSavedPaymentMethodViewContent(
        displayState = PayPalSavedPaymentMethodDisplayState.Loading,
        isCreditMessageLoading = true
    )
}

@OptIn(ExperimentalBetaApi::class)
@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewPayPalSavedPaymentMethodViewNoNetwork() {
    PayPalSavedPaymentMethodViewContent(displayState = PayPalSavedPaymentMethodDisplayState.NoNetwork)
}

@OptIn(ExperimentalBetaApi::class)
@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewPayPalSavedPaymentMethodViewCreditMessaging() {
    PayPalSavedPaymentMethodViewContent(
        displayState = PayPalSavedPaymentMethodDisplayState.Content(
            paymentMethod = PayPalSavedpaymentMethod(
                label = "",
                imageUrl = "",
                lastDigits = "3339",
                type = "CARD",
                subtype = null
            )
        ),
        creditMessage = "Or 4 interest-free payments of $324.50.",
        creditMessageLinkLabel = "Learn more"
    )
}
