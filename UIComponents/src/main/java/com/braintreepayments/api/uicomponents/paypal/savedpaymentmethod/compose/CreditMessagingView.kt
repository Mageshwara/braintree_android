@file:Suppress("MagicNumber", "UnusedPrivateMember")

package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model.CreditMessagingDisplayState
import com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.styling.RootStyle

/**
 * The presentational surface for the Pay Later ("BNPL") credit-messaging row shown beneath the FI
 * chip.
 *
 * Purely presentational: renders whatever [state] carries and exposes a single [onLearnMoreClick]
 * hook for the trailing link — it performs no network calls and launches nothing (wired in a later
 * pass). The copy is server-driven (Credit Presentment API response), so it arrives via [state],
 * not SDK strings. Only "Treatment A" copy is ever requested/rendered — there is no arm resolution.
 *
 * Renders nothing for [CreditMessagingDisplayState.Hidden] (fetch failed, no `preferred_message`,
 * or `style.enabled == false`).
 *
 * @param state            what to render — a messaging row or nothing.
 * @param modifier         Compose modifier for the row.
 * @param style            theming (see [CreditMessagingStyle]).
 * @param rootStyle        the shared root styling — base text color and accent color for the link.
 * @param onLearnMoreClick invoked when the buyer taps the row's "Learn more" link.
 */
@Composable
fun CreditMessagingView(
    state: CreditMessagingDisplayState,
    modifier: Modifier = Modifier,
    style: CreditMessagingStyle = CreditMessagingStyle(),
    rootStyle: RootStyle = RootStyle(),
    onLearnMoreClick: () -> Unit = {},
) {
    if (!style.enabled) return
    when (state) {
        is CreditMessagingDisplayState.Hidden -> Unit
        is CreditMessagingDisplayState.Content ->
            MessagingRow(
                content = state,
                style = style,
                rootStyle = rootStyle,
                onLearnMoreClick = onLearnMoreClick,
                modifier = modifier,
            )
    }
}

/** The messaging row: `<messageText> <learnMoreText>`, where the link is underlined. The row is the
 * tap target. */
@Composable
private fun MessagingRow(
    content: CreditMessagingDisplayState.Content,
    style: CreditMessagingStyle,
    rootStyle: RootStyle,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageTextColor = rootStyle.textColorBase?.let { Color(it) } ?: Color.Black
    val learnMoreTextColor = rootStyle.primaryColor?.let { Color(it) } ?: messageTextColor
    val textSize = style.fontSizeSp.sp

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = messageTextColor)) {
                append(content.messageText)
            }
            append(" ")
            withStyle(
                SpanStyle(
                    color = learnMoreTextColor,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(content.learnMoreText)
            }
        },
        fontSize = textSize,
        lineHeight = textSize * style.lineHeightRatio,
        modifier = modifier.clickable(onClick = onLearnMoreClick),
    )
}

// region Previews
// Preview mock data — for design review only; not used at runtime (real copy is server-driven).
// Wrapped in a white Box so the transparent row renders on the merchant-checkout (white) surface.

@Preview(name = "Messaging — Pay in 4", showBackground = true, widthDp = 360)
@Composable
private fun PreviewCreditMessagingPayIn4() {
    MessagingPreview(
        messageText = "Or pay in 4 interest-free payments of $324.50.",
        learnMoreText = "Learn more",
    )
}

@Preview(name = "Messaging — Pay Monthly", showBackground = true, widthDp = 360)
@Composable
private fun PreviewCreditMessagingPayMonthly() {
    MessagingPreview(
        messageText = "Or pay in full or as low as $324.50/mo.",
        learnMoreText = "Learn more",
    )
}

@Composable
private fun MessagingPreview(messageText: String, learnMoreText: String) {
    Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
        CreditMessagingView(
            state = CreditMessagingDisplayState.Content(messageText = messageText, learnMoreText = learnMoreText),
        )
    }
}

// endregion
