@file:Suppress("MagicNumber", "UnusedPrivateMember")

package com.braintreepayments.api.uicomponents.compose

import androidx.annotation.DimenRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.uicomponents.CreditMessagingDisplayState
import com.braintreepayments.api.uicomponents.CreditMessagingStyle
import com.braintreepayments.api.uicomponents.R

/**
 * The presentational surface for the Pay Later ("BNPL") credit-messaging row shown beneath the FI
 * chip (design "approach B" / "BEFORE SELECTION").
 *
 * Purely presentational: renders whatever [state] carries and exposes a single [onLinkClick] hook
 * for the trailing link — it performs no network calls and launches nothing (wired in a later
 * pass). The copy is server-driven (CFS passthrough), so it arrives via [state], not SDK strings.
 *
 * Renders nothing for [CreditMessagingDisplayState.Hidden] (e.g. Pay-in-Full / non-BNPL contexts).
 *
 * @param state       what to render — a messaging row or nothing.
 * @param modifier    Compose modifier for the row.
 * @param style       theming (see [CreditMessagingStyle]).
 * @param onLinkClick invoked when the buyer taps the row's link (e.g. "Pay Later options").
 */
@Composable
fun CreditMessagingView(
    state: CreditMessagingDisplayState,
    modifier: Modifier = Modifier,
    style: CreditMessagingStyle = CreditMessagingStyle(),
    onLinkClick: () -> Unit = {},
) {
    when (state) {
        is CreditMessagingDisplayState.Hidden -> Unit
        is CreditMessagingDisplayState.Content ->
            MessagingRow(content = state, style = style, onLinkClick = onLinkClick, modifier = modifier)
    }
}

/** The messaging row: `<message> <link>`, where the link is underlined. The row is the tap target. */
@Composable
private fun MessagingRow(
    content: CreditMessagingDisplayState.Content,
    style: CreditMessagingStyle,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messagingTextSize = spDimensionResource(R.dimen.credit_messaging_text_size)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = style.messageTextColor)) {
                append(content.message)
            }
            append(" ")
            withStyle(
                SpanStyle(
                    color = style.linkTextColor,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(content.linkLabel)
            }
        },
        fontSize = messagingTextSize,
        modifier = modifier.clickable(onClick = onLinkClick),
    )
}

/**
 * Reads an `sp` font-size dimension from resources (Compose has no direct `sp` equivalent of
 * [dimensionResource], which returns a `Dp`). Reading the value as a `Dp` and converting it back
 * with `Density.toSp` yields the declared `sp` size while keeping the dimension in
 * `res/values/dimens.xml` per the module convention (dp/sp live in dimens.xml, colors in colors.xml
 * — see PayPalButtonView / CardFields).
 */
@Composable
private fun spDimensionResource(@DimenRes id: Int): TextUnit =
    with(LocalDensity.current) { dimensionResource(id).toSp() }

// region Previews
// Preview mock data — for design review only; not used at runtime (real copy is server-driven).
// Wrapped in a white Box so the transparent row renders on the merchant-checkout (white) surface.

@Preview(name = "Messaging — Pay in 4", showBackground = true, widthDp = 360)
@Composable
private fun PreviewCreditMessagingPayIn4() {
    MessagingPreview(
        message = "Or pay in 4 interest-free payments of $324.50.",
        linkLabel = "Learn more",
    )
}

@Preview(name = "Messaging — Pay Monthly", showBackground = true, widthDp = 360)
@Composable
private fun PreviewCreditMessagingPayMonthly() {
    MessagingPreview(
        message = "Pay in full or as low as $324.50/mo.",
        linkLabel = "Learn more",
    )
}

@Preview(name = "Messaging — Pay Later (approach B)", showBackground = true, widthDp = 360)
@Composable
private fun PreviewCreditMessagingPayLaterOptions() {
    MessagingPreview(
        message = "Want more time to pay?",
        linkLabel = "Pay Later options",
    )
}

@Composable
private fun MessagingPreview(message: String, linkLabel: String) {
    Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
        CreditMessagingView(
            state = CreditMessagingDisplayState.Content(message = message, linkLabel = linkLabel),
        )
    }
}

// endregion
