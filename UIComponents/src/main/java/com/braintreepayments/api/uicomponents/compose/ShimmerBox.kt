package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import com.braintreepayments.api.uicomponents.R

/**
 * A skeleton placeholder block for loading states — a two-tone gradient bar matching the Figma
 * "Glimmer" node.
 *
 * Reusable across components: the caller sizes it via [modifier] (e.g. `Modifier.width(..).height(..)`)
 * and, optionally, matches the surrounding surface with [shape]. Multiple boxes placed in a
 * row/column form a skeleton of the eventual layout (see the FI loading chip in
 * [com.braintreepayments.api.uicomponents.paypal.savedpaymentmethod.compose.SavedPayPalPaymentMethodView]).
 *
 * @param modifier   sizing / layout for the block. Give it an explicit width and height.
 * @param shape      the block's shape; defaults to a plain rectangle.
 * @param startColor the gradient's leading color; defaults to [R.color.shimmer_gradient_start].
 * @param endColor   the gradient's trailing color; defaults to [R.color.shimmer_gradient_end].
 */
@Composable
internal fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    startColor: Color = colorResource(R.color.shimmer_gradient_start),
    endColor: Color = colorResource(R.color.shimmer_gradient_end),
) {
    Spacer(
        modifier = modifier.background(
            brush = Brush.horizontalGradient(colors = listOf(startColor, endColor)),
            shape = shape,
        ),
    )
}
