package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Error returned when `/v2/credit/fetch-presentment-messages` fails or returns no usable message.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
class CreditMessagingError internal constructor(
    message: String,
    cause: Throwable? = null
) : BraintreeException(message, cause)
