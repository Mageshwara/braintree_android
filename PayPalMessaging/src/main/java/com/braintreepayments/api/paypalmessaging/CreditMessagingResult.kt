package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject

/**
 * A single copy or logo block making up a presentment message, from `preferred_message.content.main_items`.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property type Block type, e.g. "TEXT", "TEXT_VARIABLE", or "IMAGE".
 * @property text Copy for a text block.
 * @property name Variable name for the block, e.g. "periodic_payment_count" or "paypal_logo".
 * @property sourceUrl Image URL for an image block.
 * @property alternativeText Accessibility text for an image block.
 */
@ExperimentalBetaApi
data class CreditMessagingContentItem(
    val type: String,
    val text: String? = null,
    val name: String? = null,
    val sourceUrl: String? = null,
    val alternativeText: String? = null
) {
    companion object {
        internal fun fromJson(json: JSONObject): CreditMessagingContentItem {
            return CreditMessagingContentItem(
                type = json.optString("type"),
                text = json.optString("text").takeIf { json.has("text") },
                name = json.optString("name").takeIf { json.has("name") },
                sourceUrl = json.optString("source_url").takeIf { json.has("source_url") },
                alternativeText = json.optString("alternative_text").takeIf {
                    json.has("alternative_text")
                }
            )
        }
    }
}

/**
 * A tappable action on a presentment message, e.g. the "Learn more" link, from
 * `preferred_message.content.action_items`.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property type Action type, e.g. "LINK".
 * @property text Display text for the action, e.g. "Learn more".
 * @property clickUrl URL to open when the action is tapped.
 * @property embeddable Whether [clickUrl] should be opened in-app rather than an external browser.
 */
@ExperimentalBetaApi
data class CreditMessagingActionItem(
    val type: String,
    val text: String? = null,
    val clickUrl: String? = null,
    val embeddable: Boolean = false
) {
    companion object {
        internal fun fromJson(json: JSONObject): CreditMessagingActionItem {
            return CreditMessagingActionItem(
                type = json.optString("type"),
                text = json.optString("text").takeIf { json.has("text") },
                clickUrl = json.optString("click_url").takeIf { json.has("click_url") },
                embeddable = json.optBoolean("embeddable", false)
            )
        }
    }
}

/**
 * Why a given message was selected as the preferred message, from `messages[].selection_reasons`.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property code Reason code, e.g. "DEFAULT_PREFERRED".
 * @property description Human-readable description of the reason.
 */
@ExperimentalBetaApi
data class CreditMessageSelectionReason(
    val code: String? = null,
    val description: String? = null
) {
    companion object {
        internal fun fromJson(json: JSONObject): CreditMessageSelectionReason {
            return CreditMessageSelectionReason(
                code = json.optString("code").takeIf { json.has("code") },
                description = json.optString("description").takeIf { json.has("description") }
            )
        }
    }
}

/**
 * The preferred presentment message returned by `/v2/credit/fetch-presentment-messages`.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property id The message id.
 * @property type The message template id, e.g. "PLLT_MQ_GZ".
 * @property mainItems Logo + copy blocks to render inline.
 * @property actionItems CTA blocks, e.g. the "Learn more" link.
 * @property impressionUrl Analytics URL to fire when the message is displayed.
 * @property selectionReasons Why this message was chosen, from `messages[].selection_reasons`.
 */
@ExperimentalBetaApi
data class CreditMessage(
    val id: String,
    val type: String,
    val mainItems: List<CreditMessagingContentItem>,
    val actionItems: List<CreditMessagingActionItem>,
    val impressionUrl: String?,
    val selectionReasons: List<CreditMessageSelectionReason>
) {
    companion object {
        internal fun fromJson(messageJson: JSONObject): CreditMessage {
            val preferredMessage = messageJson.optJSONObject("preferred_message") ?: JSONObject()
            val content = preferredMessage.optJSONObject("content")
            val mainItems = content?.optJSONArray("main_items")?.let { items ->
                (0 until items.length()).map { CreditMessagingContentItem.fromJson(items.getJSONObject(it)) }
            } ?: emptyList()
            val actionItems = content?.optJSONArray("action_items")?.let { items ->
                (0 until items.length()).map { CreditMessagingActionItem.fromJson(items.getJSONObject(it)) }
            } ?: emptyList()
            val impressionUrl = preferredMessage.optJSONObject("analytics")
                ?.optString("impression_url")
                ?.takeIf { it.isNotEmpty() }
            val selectionReasons = messageJson.optJSONArray("selection_reasons")?.let { items ->
                (0 until items.length()).map { CreditMessageSelectionReason.fromJson(items.getJSONObject(it)) }
            } ?: emptyList()

            return CreditMessage(
                id = preferredMessage.optString("id"),
                type = preferredMessage.optString("type"),
                mainItems = mainItems,
                actionItems = actionItems,
                impressionUrl = impressionUrl,
                selectionReasons = selectionReasons
            )
        }
    }
}

/**
 * The result of fetching credit presentment messaging.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
@ExperimentalBetaApi
sealed class CreditMessagingResult {

    /**
     * The request succeeded and returned a [CreditMessage] to render.
     */
    data class Success(val message: CreditMessage) : CreditMessagingResult()

    /**
     * The request failed, or returned no `preferred_message`. Callers should hide the
     * messaging row; the FI card / rest of the UI still renders.
     */
    data class Failure(val error: CreditMessagingError) : CreditMessagingResult()
}
