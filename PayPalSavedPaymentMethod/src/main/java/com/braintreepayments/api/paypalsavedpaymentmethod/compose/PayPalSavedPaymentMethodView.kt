@file:Suppress("TooManyFunctions")

package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.annotation.DimenRes
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummaryResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.LoaderImage
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.toDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch

/**
 * Entry point for merchants — owns fetching the sticky FI and launching the edit-FI PayPal
 * payment auth flow, then renders the [displayState]-driven content view.
 *
 * The FI fetch and credit-messaging fetch both show their loading shimmer from first composition,
 * but the calls themselves are sequenced, not parallel: credit messaging is only fetched after the
 * FI fetch succeeds. If the FI fetch fails, credit messaging is never called and never shown —
 * the messaging row is meaningless without a valid funding instrument to attach it to.
 *
 * Fetching, edit-FI launching, and return-flow handling live as shared (non-Compose) functions in
 * the module's root package, so a future non-Compose entry point can reuse them without depending
 * on this file.
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
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val payPalSavedPaymentMethodClient = remember {
        PayPalSavedPaymentMethodClient(context, authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
    }

    var displayState by remember {
        mutableStateOf<PayPalSavedPaymentMethodDisplayState>(PayPalSavedPaymentMethodDisplayState.Loading)
    }
    var isCreditMessageLoading by remember { mutableStateOf(true) }
    var creditMessagingContent by remember { mutableStateOf<PayPalCreditMessagingContent?>(null) }
    var editEnabled by remember { mutableStateOf(true) }
    var isEditFiLoading by remember { mutableStateOf(false) }

    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    if (registry == null) {
        paypalTokenizeCallback.onPayPalResult(
            PayPalResult.Failure(BraintreeException(ACTIVITY_RESULT_REGISTRY_IS_NULL_MESSAGE))
        )
        return
    }
    val payPalLauncher = remember { PayPalLauncher(registry) }
    val editFiLauncher = remember {
        PayPalSavedPaymentMethodLauncher(
            payPalLauncher,
            PendingRequestRepository(context, "paypal_saved_payment_method")
        )
    }

    LaunchedEffect(Unit) {
        when (val result = payPalSavedPaymentMethodClient.fetchFI()) {
            is PayPalSavedPaymentMethodSummaryResult.Success -> {
                displayState = result.paymentMethodSummary.toDisplayState()
                if (style.showPayPalCreditMessaging) {
                    creditMessagingContent = payPalSavedPaymentMethodClient.fetchCreditPresentmentMessages(
                        amount = payPalCheckoutRequest.amount,
                        currency = payPalCheckoutRequest.currencyCode
                    )
                }
                isCreditMessageLoading = false
            }

            is PayPalSavedPaymentMethodSummaryResult.Failure -> {
                // Credit messaging is intentionally never called here — it has nothing meaningful
                // to attach to without a successfully fetched funding instrument.
                displayState = PayPalSavedPaymentMethodDisplayState.Error
                isCreditMessageLoading = false
            }
        }
    }

    PayPalSavedPaymentMethodViewContent(
        displayState = displayState,
        style = style,
        editEnabled = editEnabled,
        onEditClick = {
            editEnabled = false
            isEditFiLoading = true
            startEditFiFlow(
                context = context,
                coroutineScope = coroutineScope,
                payPalSavedPaymentMethodClient = payPalSavedPaymentMethodClient,
                payPalCheckoutRequest = payPalCheckoutRequest,
                editFiLauncher = editFiLauncher,
                paypalTokenizeCallback = paypalTokenizeCallback,
                onEditEnabledChanged = { editEnabled = it },
                onLoadingChanged = { isEditFiLoading = it }
            )
        },
        isCreditMessageLoading = isCreditMessageLoading,
        creditMessagingContent = creditMessagingContent,
        onCreditMessageLinkClick = {
            creditMessagingContent?.learnMoreUrl?.let { url -> context.launchCreditMessagingLander(url) }
        }
    )

    if (isEditFiLoading) {
        FullScreenLoadingDialog()
    }

    // A reentrancy guard, not UI state - checked/set synchronously so two onResume calls in quick
    // succession (e.g. a system dialog interrupting the return) can't both pass the pending-request
    // check before either sets isEditFiLoading, which would let both launch handleEditFiReturn
    // concurrently. Deliberately not a MutableState: it must never trigger recomposition or affect
    // what's rendered, only guard against a second concurrent read/tokenize.
    val isProcessingEditFiReturn = remember { AtomicBoolean(false) }

    LifecycleResumeEffect(Unit) {
        if (!isEditFiLoading && isProcessingEditFiReturn.compareAndSet(false, true)) {
            coroutineScope.launch {
                try {
                    val pendingRequestStr = editFiLauncher.pendingRequestRepository.getPendingRequest()
                    if (pendingRequestStr.isNotEmpty()) {
                        isEditFiLoading = true
                        val intent = activity?.intent
                        if (intent != null) {
                            val tokenizeResult = handleEditFiReturn(
                                editFiLauncher = editFiLauncher,
                                payPalSavedPaymentMethodClient = payPalSavedPaymentMethodClient,
                                pendingRequestString = pendingRequestStr,
                                intent = intent,
                                paypalTokenizeCallback = paypalTokenizeCallback
                            )
                            editFiLauncher.pendingRequestRepository.clearPendingRequest()
                            editEnabled = true
                            isEditFiLoading = false

                            if (tokenizeResult is PayPalResult.Success) {
                                tokenizeResult.nonce.paymentId?.let { orderId ->
                                    val lastKnownDisplayState = displayState
                                    displayState = PayPalSavedPaymentMethodDisplayState.Loading
                                    refetchFiAfterEdit(
                                        payPalSavedPaymentMethodClient,
                                        orderId,
                                        lastKnownDisplayState
                                    ) { refreshedDisplayState ->
                                        displayState = refreshedDisplayState
                                    }
                                }
                            }
                        } else {
                            paypalTokenizeCallback.onPayPalResult(
                                PayPalResult.Failure(BraintreeException(ACTIVITY_OR_INTENT_IS_NULL_MESSAGE))
                            )
                            editEnabled = true
                            isEditFiLoading = false
                        }
                    }
                } finally {
                    isProcessingEditFiReturn.set(false)
                }
            }
        }
        onPauseOrDispose { }
    }
}

/**
 * Blocks the full screen (not just this component) while an edit-FI network call is in flight -
 * either the initial `createPaymentAuthRequest` (before the browser switch away from the host
 * screen), or, after returning from that browser switch, the tokenize in [handleEditFiReturn].
 * The best-effort FI refresh that follows a successful edit ([refetchFiAfterEdit]) runs
 * separately and shows its own shimmer via [PayPalSavedPaymentMethodDisplayState.Loading] instead
 * of blocking the full screen.
 */
@Composable
private fun FullScreenLoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // The platform dialog theme both dims the screen behind the dialog window and gives the
        // window itself an opaque background by default. Either one alone is enough to hide the
        // screen instead of letting it show through per the design, so both must be cleared -
        // dim to zero and the window background to transparent - leaving only our own translucent
        // background in control of how much of the screen is visible.
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect {
            dialogWindowProvider?.window?.let { window ->
                window.setDimAmount(0f)
                window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.paypal_saved_payment_method_loading_dialog_background)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.paypal_saved_payment_method_loading_dialog_spinner_text_spacing)
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.paypal_saved_payment_method_loading_dialog_spinner_size)
                    ),
                    color = colorResource(R.color.paypal_saved_payment_method_loading_dialog_spinner_color),
                    strokeWidth = dimensionResource(
                        R.dimen.paypal_saved_payment_method_loading_dialog_spinner_stroke_width
                    )
                )
                Text(
                    text = stringResource(R.string.paypal_saved_payment_method_loading_dialog_message),
                    color = colorResource(R.color.paypal_saved_payment_method_loading_dialog_text_color),
                    fontSize = spDimensionResource(
                        R.dimen.paypal_saved_payment_method_loading_dialog_text_font_size
                    ),
                    lineHeight = spDimensionResource(
                        R.dimen.paypal_saved_payment_method_loading_dialog_text_line_height
                    ),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

private const val ACTIVITY_RESULT_REGISTRY_IS_NULL_MESSAGE =
    "ActivityResultRegistry is null. ActivityResultRegistry cannot be null for this flow."
private const val ACTIVITY_OR_INTENT_IS_NULL_MESSAGE = "Activity or Intent is null. Unable to restore state."

/**
 * Presentational view for the saved/editable PayPal funding instrument row — logo, "PayPal"
 * label, an FI cluster (icon + masked instrument + edit affordance) that reflects [displayState],
 * and an optional Pay Later credit-messaging line below it. Fully driven by [style]; contains no
 * networking or business logic.
 *
 * Real card-art loading is not yet wired — the FI cluster always falls back to a generic type
 * icon regardless of [PayPalSavedPaymentMethod.imageUrl].
 *
 * @param isCreditMessageLoading true while the credit-messaging fetch is in flight; renders a
 * shimmer placeholder in place of the row.
 * @param creditMessagingContent the resolved Pay Later / Credit presentment messaging; the
 * credit-messaging row is hidden when this is null or [PayPalSavedPaymentMethodViewStyle.showPayPalCreditMessaging]
 * is false.
 * @param onCreditMessageLinkClick invoked when the credit-messaging link is tapped.
 */
@ExperimentalBetaApi
@Composable
fun PayPalSavedPaymentMethodViewContent(
    displayState: PayPalSavedPaymentMethodDisplayState,
    modifier: Modifier = Modifier,
    editContentDescription: String? = null,
    style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle(),
    editEnabled: Boolean = true,
    onEditClick: () -> Unit = {},
    isCreditMessageLoading: Boolean = false,
    creditMessagingContent: PayPalCreditMessagingContent? = null,
    onCreditMessageLinkClick: () -> Unit = {}
) {
    // NoNetwork and Error both hide the FI row but keep the PayPal brand mark visible.
    val hideFiRow = displayState is PayPalSavedPaymentMethodDisplayState.Error ||
        displayState is PayPalSavedPaymentMethodDisplayState.NoNetwork
    val context = LocalContext.current
    val resolvedStyle = remember(style) { PayPalSavedPaymentMethodStyleResolver(style, context) }
    val textColor = Color(resolvedStyle.textColor)
    val showCreditMessaging = shouldShowCreditMessaging(
        hideFiRow = hideFiRow,
        showPayPalCreditMessaging = resolvedStyle.showCreditMessaging,
        isCreditMessageLoading = isCreditMessageLoading,
        creditMessagingContent = creditMessagingContent
    )

    Card(
        modifier = modifier
            .let {
                if (resolvedStyle.heightDp != null) it.height(resolvedStyle.heightDp.dp) else it.wrapContentHeight()
            },
        shape = RoundedCornerShape(resolvedStyle.cornerRadiusDp.dp),
        colors = CardDefaults.cardColors(containerColor = Color(resolvedStyle.backgroundColor)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (resolvedStyle.borderWidthDp > 0f) {
            BorderStroke(resolvedStyle.borderWidthDp.dp, Color(resolvedStyle.borderColor))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = resolvedStyle.horizontalPaddingDp.dp,
                    vertical = resolvedStyle.verticalPaddingDp.dp
                ),
            verticalAlignment = Alignment.Top
        ) {
            if (resolvedStyle.showLogo) {
                Image(
                    painter = painterResource(R.drawable.ic_paypal_brand_logo),
                    contentDescription = stringResource(R.string.paypal_saved_payment_method_label),
                    modifier = Modifier.width(resolvedStyle.logoWidthDp.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (resolvedStyle.showLabel) {
                        Text(
                            text = stringResource(R.string.paypal_saved_payment_method_label),
                            color = textColor,
                            fontSize = resolvedStyle.labelFontSizeSp.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = resolvedStyle.fontFamily,
                            modifier = Modifier.padding(start = resolvedStyle.labelMarginStartDp.dp)
                        )
                    }

                    if (!hideFiRow) {
                        FiCluster(
                            displayState = displayState,
                            resolvedStyle = resolvedStyle,
                            editContentDescription = editContentDescription,
                            editEnabled = editEnabled,
                            onEditClick = onEditClick,
                            modifier = Modifier.padding(start = resolvedStyle.fundingInstrumentMarginStartDp.dp)
                        )
                    }
                }

                if (showCreditMessaging) {
                    CreditMessagingSection(
                        isLoading = isCreditMessageLoading,
                        creditMessagingContent = creditMessagingContent,
                        resolvedStyle = resolvedStyle,
                        onLinkClick = onCreditMessageLinkClick,
                        modifier = Modifier
                            .padding(
                                start = resolvedStyle.labelMarginStartDp.dp,
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

@OptIn(ExperimentalBetaApi::class)
@Composable
private fun CreditMessagingSection(
    isLoading: Boolean,
    creditMessagingContent: PayPalCreditMessagingContent?,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        CreditMessagingPlaceholder(modifier = modifier)
    } else {
        CreditMessagingRow(
            message = creditMessagingContent?.message.orEmpty(),
            linkLabel = creditMessagingContent?.learnMoreText.orEmpty(),
            resolvedStyle = resolvedStyle,
            onLinkClick = onLinkClick,
            modifier = modifier
        )
    }
}

@Composable
private fun CreditMessagingRow(
    message: String,
    linkLabel: String,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(resolvedStyle.textColor)
    val linkColor = resolvedStyle.creditMessagingLinkColor?.let { Color(it) } ?: baseColor

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) { append(message) }
            append(" ")
            withLink(
                LinkAnnotation.Clickable(
                    tag = LEARN_MORE_LINK_TAG,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = { onLinkClick() }
                )
            ) {
                append(linkLabel)
            }
        },
        fontSize = resolvedStyle.creditMessagingFontSizeSp.sp,
        lineHeight = spDimensionResource(R.dimen.paypal_saved_payment_method_credit_messaging_line_height),
        fontFamily = resolvedStyle.fontFamily,
        modifier = modifier
    )
}

private const val LEARN_MORE_LINK_TAG = "paypal_saved_payment_method_learn_more_link"

@OptIn(ExperimentalBetaApi::class)
@Composable
private fun FiCluster(
    displayState: PayPalSavedPaymentMethodDisplayState,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    editContentDescription: String?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    editEnabled: Boolean = true
) {
    val textColor = Color(resolvedStyle.textColor)
    val isLoading = displayState is PayPalSavedPaymentMethodDisplayState.Loading
    val iconMargin = dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_margin)
    val chipBackground = colorResource(R.color.paypal_saved_payment_method_funding_instrument_chip_background)
    val chipCornerRadius = dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_chip_corner_radius)

    Row(
        modifier = modifier
            .let {
                if (isLoading) {
                    it
                } else {
                    it.clip(RoundedCornerShape(chipCornerRadius)).background(chipBackground)
                }
            }
            .padding(
                horizontal = dimensionResource(
                    R.dimen.paypal_saved_payment_method_funding_instrument_chip_horizontal_padding
                ),
                vertical = dimensionResource(
                    R.dimen.paypal_saved_payment_method_funding_instrument_chip_vertical_padding
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (displayState) {
            is PayPalSavedPaymentMethodDisplayState.Loading -> {
                FiClusterPlaceholder()
            }
            is PayPalSavedPaymentMethodDisplayState.Content -> {
                val fiIconRes = fiIconFor(displayState.paymentMethod.type)
                val imageUrl = displayState.paymentMethod.imageUrl
                val iconModifier = Modifier
                    .width(dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_width))
                    .padding(end = iconMargin)
                if (imageUrl.isNotBlank()) {
                    LoaderImage(
                        url = imageUrl,
                        placeholder = painterResource(fiIconRes ?: R.drawable.ic_paypal_brand_logo),
                        modifier = iconModifier
                    )
                } else if (fiIconRes != null) {
                    Image(
                        painter = painterResource(fiIconRes),
                        contentDescription = null,
                        modifier = iconModifier
                    )
                }
                Text(
                    text = fiClusterText(displayState),
                    color = textColor,
                    fontSize = resolvedStyle.fundingInstrumentTextFontSizeSp.sp,
                    fontFamily = resolvedStyle.fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is PayPalSavedPaymentMethodDisplayState.NoFi -> {
                Text(
                    text = displayState.buyerEmail,
                    color = textColor,
                    fontSize = resolvedStyle.fundingInstrumentTextFontSizeSp.sp,
                    fontFamily = resolvedStyle.fontFamily,
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
                    .padding(start = iconMargin)
                    .size(resolvedStyle.editIconSizeDp.dp)
                    .clickable(enabled = editEnabled, onClick = onEditClick)
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

@OptIn(ExperimentalBetaApi::class)
@Composable
private fun fiClusterText(content: PayPalSavedPaymentMethodDisplayState.Content): String {
    val method = content.paymentMethod
    val maskableLastDigits = maskableLastDigitsOrNull(method.type, method.lastDigits)
    return if (maskableLastDigits != null) {
        stringResource(
            R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number,
            maskableLastDigits
        )
    } else {
        method.label
    }
}

private val PayPalSavedPaymentMethodStyleResolver.fontFamily: FontFamily
    get() = fontResId?.let { FontFamily(Font(it)) } ?: FontFamily.Default

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
            paymentMethod = PayPalSavedPaymentMethod(
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
            paymentMethod = PayPalSavedPaymentMethod(
                label = "",
                imageUrl = "",
                lastDigits = "3339",
                type = "CARD",
                subtype = null
            )
        ),
        creditMessagingContent = PayPalCreditMessagingContent(
            message = "Or 4 interest-free payments of $324.50.",
            learnMoreText = "Learn more",
            learnMoreUrl = ""
        )
    )
}
