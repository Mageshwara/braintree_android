package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.CancellationException

/**
 * Renders the funding-instrument icon at [url] via [ImageLoader], falling back to [placeholder]
 * until the load completes or if it fails.
 */
@Composable
internal fun LoaderImage(
    url: String,
    placeholder: Painter,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = remember { ImageLoader.getInstance() }
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(url) {
        bitmap = try {
            imageLoader.load(url)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(bitmap = loadedBitmap.asImageBitmap(), contentDescription = null, modifier = modifier)
    } else {
        Image(painter = placeholder, contentDescription = null, modifier = modifier)
    }
}
