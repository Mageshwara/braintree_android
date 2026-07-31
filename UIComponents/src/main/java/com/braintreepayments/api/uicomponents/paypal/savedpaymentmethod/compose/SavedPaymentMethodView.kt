@file:Suppress("TooManyFunctions", "UnusedPrivateMember")

package com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose

import androidx.annotation.DimenRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
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
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.compose.PayPalMark
import com.braintreepayments.api.uicomponents.compose.ShimmerBox
import com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model.FiSummary
import com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model.FiType
import com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.model.SavedPaymentMethodDisplayState
import com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.styling.SavedPaymentMethodViewStyle

/**
 * The presentational surface for the SavedPaymentMethod component — renders the PayPal brand
 * [PayPalMark] logo next to the sticky funding-instrument (FI) "chip" (PayPal label + brand art +
 * masked number + edit pencil, all inside one rounded pill), plus loading and fallback states.
 *
 * Purely presentational: holds no PayPal client, performs no network calls, and drives everything
 * from [state]. The owning component (added in a later pass) fetches the FI, launches the
 * edit flow, and maps results into [state].
 *
 * The Pay Later credit-messaging row is a separate view (its own PR) and is not part of this
 * component; embedding the two together is a later pass.
 *
 * @param state      what to render — loading skeleton, an FI chip, the no-FI fallback, the add-card
 * prompt, or nothing (on [SavedPaymentMethodDisplayState.Error]).
 * @param editContentDescription accessibility label for the edit pencil (backend-driven copy).
 * @param modifier   Compose modifier for the outer container (mark + chip).
 * @param style      merchant styling (see [SavedPaymentMethodViewStyle]).
 * @param onEditClick invoked when the buyer taps the edit pencil.
 * @param onAddCardClick invoked when the buyer taps the "add a card" link in the
 * [SavedPaymentMethodDisplayState.AddCard] state. UI hook only — launching the add-card flow is
 * wired in a later pass.
 */
@Composable
fun SavedPaymentMethodView(
    state: SavedPaymentMethodDisplayState,
    editContentDescription: String,
    modifier: Modifier = Modifier,
    style: SavedPaymentMethodViewStyle = SavedPaymentMethodViewStyle(),
    onEditClick: () -> Unit = {},
    onAddCardClick: () -> Unit = {},
) {
    // Network error / API failure — the component renders nothing (blank); it simply doesn't load.
    if (state is SavedPaymentMethodDisplayState.Error) return

    val component = style.component
    val containerShape = RoundedCornerShape(component.cornerRadiusDp.dp)

    // The PayPal Mark (logo) and the "PayPal" text label sit outside the FI "chip" — only the
    // funding-instrument cluster (icon + masked number + edit pencil) gets the chip's fill (Figma
    // "Edit FI Chip" node). The messaging row is separate — embedded with this component in a later
    // pass. The outer container box (height, background, border, corner, padding) is driven by
    // ComponentStyle / RootStyle.
    Row(
        modifier = modifier
            .then(component.heightDp?.let { Modifier.height(it.dp) } ?: Modifier)
            .clip(containerShape)
            .then(style.root.backgroundColor?.let { Modifier.background(Color(it)) } ?: Modifier)
            .then(
                if (component.borderColor != null && component.borderWidthDp > 0f) {
                    Modifier.border(component.borderWidthDp.dp, Color(component.borderColor), containerShape)
                } else {
                    Modifier
                },
            )
            .padding(
                horizontal = component.horizontalPaddingDp.dp,
                vertical = component.verticalPaddingDp.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (style.layout.showLogo) {
            PayPalMark()
            Spacer(modifier = Modifier.width(style.layout.logoLabelGapDp.dp))
        }
        if (style.layout.showLabel) {
            PayPalLabel(style = style)
            Spacer(modifier = Modifier.width(style.layout.labelFiGapDp.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            ChipContent(
                state = state,
                style = style,
                editContentDescription = editContentDescription,
                onEditClick = onEditClick,
                onAddCardClick = onAddCardClick,
            )
        }
    }
}

/** Renders the state-specific FI content inside the colored chip (the "PayPal" label is drawn by
 * [SavedPaymentMethodView], outside the chip). [SavedPaymentMethodDisplayState.Error] is handled by
 * the caller (renders nothing), so it is a no-op here. */
@Composable
private fun ChipContent(
    state: SavedPaymentMethodDisplayState,
    style: SavedPaymentMethodViewStyle,
    editContentDescription: String,
    onEditClick: () -> Unit,
    onAddCardClick: () -> Unit,
) {
    when (state) {
        // While the FI loads, a single shimmer bar stands in for the FI chip.
        is SavedPaymentMethodDisplayState.Loading -> LoadingChip()

        is SavedPaymentMethodDisplayState.Content ->
            FiChip(
                fiSummary = state.fiSummary,
                style = style,
                editContentDescription = editContentDescription,
                onEditClick = onEditClick,
            )

        is SavedPaymentMethodDisplayState.NoFi ->
            NoFiChip(
                buyerEmail = state.buyerEmail,
                style = style,
                editContentDescription = editContentDescription,
                onEditClick = onEditClick,
            )

        is SavedPaymentMethodDisplayState.AddCard ->
            AddCardChip(content = state, style = style, onAddCardClick = onAddCardClick)

        is SavedPaymentMethodDisplayState.Error -> Unit // handled by the caller
    }
}

/**
 * The FI chip for a resolved instrument. Renders one of:
 * - card / bank: `[ placeholder glyph ] ••last4 [ edit pencil ]`
 * - Pay Later product: `<product name> [ edit pencil ]` (no icon)
 *
 * Long labels (e.g. "Pay Monthly") ellipsize at `edit_fi_label_max_width`.
 */
@Composable
private fun FiChip(
    fiSummary: FiSummary,
    style: SavedPaymentMethodViewStyle,
    editContentDescription: String,
    onEditClick: () -> Unit,
) {
    // The chip's icon is the funding-instrument art (card/bank), drawn in the standard 28x20dp tile
    // box. The PayPal brand Mark (45x30) is a separate view — see [PayPalMark].
    val iconWidth = dimensionResource(R.dimen.edit_fi_icon_width)
    val iconHeight = dimensionResource(R.dimen.edit_fi_icon_height)
    val iconLabelSpacing = dimensionResource(R.dimen.edit_fi_icon_label_spacing)
    val iconShape = RoundedCornerShape(style.component.cardIconCornerRadiusDp.dp)
    val labelMaxWidth = dimensionResource(R.dimen.edit_fi_label_max_width)
    val labelEditSpacing = dimensionResource(R.dimen.edit_fi_label_edit_spacing)

    Chip(color = style.component.fiClusterBackgroundColor?.let { Color(it) } ?: Color.Transparent) {
        // Pay Later tiles (Pay in 4 / Pay Monthly) show the product name only — no icon.
        fiSummary.iconRes?.let { iconRes ->
            // Image + ContentScale.Fit so the card/bank art fits the icon box without distortion,
            // centered within it. Rounded #CCC border matches the PayPal Mark box (Figma "Funding
            // Icon" node); clip keeps the art inside the rounded corners.
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(iconWidth)
                    .height(iconHeight)
                    .clip(iconShape)
                    .then(
                        style.component.cardIconBackgroundColor
                            ?.let { Modifier.background(Color(it), iconShape) } ?: Modifier,
                    )
                    .border(
                        width = dimensionResource(R.dimen.edit_fi_icon_border_width),
                        color = colorResource(R.color.edit_fi_icon_border),
                        shape = iconShape,
                    ),
            )
            Spacer(modifier = Modifier.width(iconLabelSpacing))
        }
        Text(
            text = fiLabel(fiSummary),
            color = textColor(style),
            fontSize = style.layout.fiTextFontSizeSp.sp,
            fontFamily = fontFamily(style),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = labelMaxWidth),
        )
        Spacer(modifier = Modifier.width(labelEditSpacing))
        EditButton(style = style, contentDescription = editContentDescription, onClick = onEditClick)
    }
}

/** The no-FI fallback chip: "<buyer email>" [ edit pencil ]. Fills width to ellipsize. */
@Composable
private fun NoFiChip(
    buyerEmail: String,
    style: SavedPaymentMethodViewStyle,
    editContentDescription: String,
    onEditClick: () -> Unit,
) {
    val labelEditSpacing = dimensionResource(R.dimen.edit_fi_label_edit_spacing)

    Chip(
        modifier = Modifier.fillMaxWidth(),
        color = style.component.fiClusterBackgroundColor?.let { Color(it) } ?: Color.Transparent,
    ) {
        Text(
            text = buyerEmail,
            color = textColor(style),
            fontSize = style.layout.fiTextFontSizeSp.sp,
            fontFamily = fontFamily(style),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(labelEditSpacing))
        EditButton(style = style, contentDescription = editContentDescription, onClick = onEditClick)
    }
}

/**
 * The empty/disallowed-wallet chip: "⚠ <message><actionLabel>" on an amber background, where
 * [SavedPaymentMethodDisplayState.AddCard.actionLabel] is styled as an underlined link. The whole
 * chip is the tap target ([onAddCardClick]); the "PayPal" label sits outside it (see
 * [SavedPaymentMethodView]). Copy is backend-driven (carried on [content]).
 */
@Composable
private fun AddCardChip(
    content: SavedPaymentMethodDisplayState.AddCard,
    style: SavedPaymentMethodViewStyle,
    onAddCardClick: () -> Unit,
) {
    val warningIconSize = dimensionResource(R.dimen.edit_fi_warning_icon_size)
    val iconLabelSpacing = dimensionResource(R.dimen.edit_fi_icon_label_spacing)
    val addCardTextSize = spDimensionResource(R.dimen.edit_fi_add_card_text_size)

    Chip(
        modifier = Modifier.clickable(onClick = onAddCardClick),
        color = colorResource(R.color.edit_fi_add_card_background),
    ) {
        Icon(
            painter = painterResource(R.drawable.edit_fi_warning),
            contentDescription = null,
            tint = colorResource(R.color.edit_fi_warning_icon),
            modifier = Modifier.size(warningIconSize),
        )
        Spacer(modifier = Modifier.width(iconLabelSpacing))
        Text(
            text = buildAnnotatedString {
                append(content.message)
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(content.actionLabel)
                }
            },
            color = textColor(style),
            fontSize = addCardTextSize,
            fontFamily = fontFamily(style),
            maxLines = 1,
        )
    }
}

/**
 * The loading skeleton — a single rounded shimmer bar shown in place of the FI chip while the
 * instrument is fetched (Figma "Loading/Shimmer" node). No chip fill and no icon/label split: one
 * bar, per design.
 */
@Composable
private fun LoadingChip() {
    val barWidth = dimensionResource(R.dimen.edit_fi_shimmer_bar_width)
    val barHeight = dimensionResource(R.dimen.edit_fi_shimmer_bar_height)
    val shimmerShape = RoundedCornerShape(dimensionResource(R.dimen.edit_fi_shimmer_corner_radius))

    ShimmerBox(
        modifier = Modifier
            .width(barWidth)
            .height(barHeight),
        shape = shimmerShape,
    )
}

/**
 * The shared rounded chip container (Figma "Edit FI Chip" node's shape/spacing) — reused for both
 * the FI cluster's fill and the amber add-card prompt, which only differ by [color] and whether the
 * chip itself is clickable ([modifier]).
 */
@Composable
private fun Chip(
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    content: @Composable RowScope.() -> Unit,
) {
    val cornerRadius = dimensionResource(R.dimen.edit_fi_chip_corner_radius)
    val paddingHorizontal = dimensionResource(R.dimen.edit_fi_chip_padding_horizontal)
    val paddingVertical = dimensionResource(R.dimen.edit_fi_chip_padding_vertical)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = color,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = paddingHorizontal,
                vertical = paddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** The static "PayPal" brand text shown before the FI chip. */
@Composable
private fun PayPalLabel(style: SavedPaymentMethodViewStyle) {
    Text(
        text = stringResource(R.string.edit_fi_paypal_label),
        color = textColor(style),
        fontSize = style.layout.labelFontSizeSp.sp,
        fontFamily = fontFamily(style),
        fontWeight = FontWeight.Bold,
    )
}

/**
 * The edit (pencil) affordance inside the FI chip. Sized to [LayoutStyle.iconSizeDp] (16dp per the
 * Figma "pencil" node) so the chip keeps its ~30dp height — the clickable area matches the glyph
 * rather than expanding to a 48dp touch target, which would inflate the pill.
 */
@Composable
private fun EditButton(style: SavedPaymentMethodViewStyle, contentDescription: String, onClick: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.edit_fi_edit_pencil),
        contentDescription = contentDescription,
        tint = textColor(style),
        modifier = Modifier
            .size(style.layout.iconSizeDp.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

/**
 * The chip label: the product [FiSummary.displayName] for Pay Later products, otherwise the masked
 * number (e.g. "••3339"). Empty when neither is available.
 */
@Composable
private fun fiLabel(fiSummary: FiSummary): String =
    fiSummary.displayName ?: fiSummary.last4?.let {
        stringResource(R.string.edit_fi_masked_number, it)
    } ?: ""

/** The shared base text color from [SavedPaymentMethodViewStyle.root]; black when unset. */
private fun textColor(style: SavedPaymentMethodViewStyle): Color =
    style.root.textColorBase?.let { Color(it) } ?: Color.Black

/** The merchant font family from [SavedPaymentMethodViewStyle.root]; system default when unset. */
@Composable
private fun fontFamily(style: SavedPaymentMethodViewStyle): FontFamily =
    style.root.fontResId?.let { FontFamily(Font(it)) } ?: FontFamily.Default

/**
 * Reads an `sp` font-size dimension from resources (Compose has no direct `sp` equivalent of
 * [dimensionResource], which returns a `Dp`). Reading the value as a `Dp`
 * and converting it back with `Density.toSp` yields the declared `sp` size
 * while keeping the dimension in `res/values/dimens.xml` per the module convention (dp/sp live in
 * dimens.xml, colors in colors.xml — see PayPalButtonView / CardFields).
 */
@Composable
private fun spDimensionResource(@DimenRes id: Int): TextUnit =
    with(LocalDensity.current) { dimensionResource(id).toSp() }

// region Previews
// Preview mock data — for design review only; not used at runtime.

/**
 * Gallery of every FI tag version from the design spec, stacked to mirror the "Tag Versions"
 * column. Design-review only.
 */
@Preview(name = "All tag versions", showBackground = true, widthDp = 360)
@Composable
private fun PreviewEditFiAllTagVersions() {
    Column(modifier = Modifier.padding(16.dp)) {
        val tags = listOf(
            SavedPaymentMethodDisplayState.Loading,
            SavedPaymentMethodDisplayState.Content(FiSummary(brand = "Visa", last4 = "3339", type = FiType.CARD)),
            SavedPaymentMethodDisplayState.Content(FiSummary(brand = "Mastercard", last4 = "3434", type = FiType.CARD)),
            SavedPaymentMethodDisplayState.Content(FiSummary(type = FiType.PAY_LATER, displayName = "Pay in 4")),
            SavedPaymentMethodDisplayState.Content(FiSummary(type = FiType.PAY_LATER, displayName = "Pay Monthly")),
            SavedPaymentMethodDisplayState.Content(FiSummary(brand = null, last4 = "3339", type = FiType.CARD)),
            SavedPaymentMethodDisplayState.Content(FiSummary(last4 = "3339", type = FiType.BANK)),
            SavedPaymentMethodDisplayState.NoFi(buyerEmail = "alex.burgos@gmail.com"),
            SavedPaymentMethodDisplayState.NoFi(
                buyerEmail = "a.very.long.buyer.email.address@somelongdomainname.example.com",
            ),
            SavedPaymentMethodDisplayState.AddCard(message = "To continue, ", actionLabel = "add a card"),
            SavedPaymentMethodDisplayState.Error,
        )
        tags.forEach { tag ->
            SavedPaymentMethodView(state = tag, editContentDescription = "Edit funding instrument")
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** The component: one chip pill — "PayPal" label + card art + ••number + edit pencil. */
@Preview(name = "PayPal + FI chip", showBackground = true, widthDp = 420)
@Composable
private fun PreviewEditFiPayPalRow() {
    SavedPaymentMethodView(
        modifier = Modifier.padding(16.dp),
        state = SavedPaymentMethodDisplayState.Content(FiSummary(brand = "Visa", last4 = "3339", type = FiType.CARD)),
        editContentDescription = "Edit funding instrument",
    )
}

// endregion
