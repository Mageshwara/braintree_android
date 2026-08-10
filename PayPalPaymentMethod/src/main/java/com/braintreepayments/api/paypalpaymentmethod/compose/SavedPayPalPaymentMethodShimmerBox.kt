package com.braintreepayments.api.paypalpaymentmethod.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import com.braintreepayments.api.paypalpaymentmethod.R

/**
 * Local port of UIComponents' `ShimmerBox` — duplicated here because this module does not depend
 * on `:UIComponents`. Keep in sync with
 * `com.braintreepayments.api.uicomponents.compose.ShimmerBox` if that implementation changes.
 */
@Composable
internal fun SavedPayPalPaymentMethodShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    brush: Brush = Brush.horizontalGradient(
        listOf(colorResource(R.color.ppm_shimmer_start), colorResource(R.color.ppm_shimmer_end))
    ),
    minAlpha: Float = SavedPayPalPaymentMethodShimmerBoxDefaults.MinAlpha,
    maxAlpha: Float = SavedPayPalPaymentMethodShimmerBoxDefaults.MaxAlpha,
    durationMillis: Int = SavedPayPalPaymentMethodShimmerBoxDefaults.DurationMillis,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer-alpha",
    )
    Spacer(modifier = modifier.alpha(alpha).background(brush = brush, shape = shape))
}

internal object SavedPayPalPaymentMethodShimmerBoxDefaults {
    const val MinAlpha = 0.5f
    const val MaxAlpha = 1f
    const val DurationMillis = 800
}