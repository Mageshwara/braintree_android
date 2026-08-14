package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * A minimal, dependency-free bitmap loader for the FI pill's card-art icon.
 *
 * No caching or request de-duplication beyond a single in-flight load per [FiSection] instance --
 * this is intentionally small in scope for a single small icon per view instance, not a
 * general-purpose image pipeline. Never throws; a failed load (network error, bad URL, decode
 * failure) resolves to `null` so the caller can fall back to a generic glyph.
 */
internal object SimpleBitmapLoader {

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
}
