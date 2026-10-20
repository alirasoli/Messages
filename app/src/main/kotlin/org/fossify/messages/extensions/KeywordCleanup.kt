package org.fossify.messages.extensions

import android.content.Context
import android.provider.Telephony.Sms
import org.fossify.commons.extensions.getIntValueOr
import org.fossify.commons.extensions.getLongValue
import org.fossify.commons.extensions.getStringValue
import org.fossify.commons.extensions.queryCursor
import org.fossify.messages.helpers.BLOCK_REASON_KEYWORD
import org.fossify.messages.helpers.ReceiverUtils.findBlockedKeyword
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.models.BlockedMessage

data class KeywordCleanupMatch(
    val id: Long,
    val isMms: Boolean,
    val threadId: Long,
    val address: String,
    val body: String,
    val dateMillis: Long,
    val subscriptionId: Int,
    val keyword: String,
)

/**
 * Finds received messages that contain a blocked keyword. Keyword exceptions are respected, and
 * messages from saved contacts are always left alone.
 */
fun Context.findKeywordCleanupMatches(): List<KeywordCleanupMatch> {
    if (config.blockedKeywords.isEmpty()) {
        return emptyList()
    }

    val matches = ArrayList<KeywordCleanupMatch>()
    val projection = arrayOf(
        Sms._ID,
        Sms.BODY,
        Sms.ADDRESS,
        Sms.DATE,
        Sms.THREAD_ID,
        Sms.SUBSCRIPTION_ID,
    )
    val selection = "${Sms.TYPE} = ?"
    val selectionArgs = arrayOf(Sms.MESSAGE_TYPE_INBOX.toString())
    queryCursor(Sms.CONTENT_URI, projection, selection, selectionArgs, showErrors = true) { cursor ->
        val body = cursor.getStringValue(Sms.BODY) ?: return@queryCursor
        val address = cursor.getStringValue(Sms.ADDRESS) ?: return@queryCursor
        val keyword = findBlockedKeyword(this, body, address, respectContacts = true)
            ?: return@queryCursor

        matches.add(
            KeywordCleanupMatch(
                id = cursor.getLongValue(Sms._ID),
                isMms = false,
                threadId = cursor.getLongValue(Sms.THREAD_ID),
                address = address,
                body = body,
                dateMillis = cursor.getLongValue(Sms.DATE),
                subscriptionId = cursor.getIntValueOr(Sms.SUBSCRIPTION_ID, -1),
                keyword = keyword
            )
        )
    }

    getMMS().filter { it.isReceivedMessage() && it.body.isNotEmpty() }.forEach { mms ->
        val keyword = findBlockedKeyword(this, mms.body, mms.senderPhoneNumber, respectContacts = true)
            ?: return@forEach

        matches.add(
            KeywordCleanupMatch(
                id = mms.id,
                isMms = true,
                threadId = mms.threadId,
                address = mms.senderPhoneNumber,
                body = mms.body,
                dateMillis = mms.millis(),
                subscriptionId = mms.subscriptionId,
                keyword = keyword
            )
        )
    }

    return matches
}

/**
 * Removes the matched messages from the message list. With "Keep blocked messages" on they are
 * moved to Blocked messages (as already seen), otherwise they are deleted for good.
 * Returns how many messages were cleaned up.
 */
fun Context.applyKeywordCleanup(matches: List<KeywordCleanupMatch>): Int {
    val keepMessages = config.keepBlockedMessages
    val affectedThreads = HashSet<Long>()
    var cleaned = 0

    matches.forEach { match ->
        if (keepMessages) {
            val isKept = runCatching {
                blockedMessagesDB.insert(
                    BlockedMessage(
                        address = match.address,
                        body = match.body,
                        date = match.dateMillis,
                        subscriptionId = match.subscriptionId,
                        reason = BLOCK_REASON_KEYWORD,
                        matchedKeyword = match.keyword,
                        seen = true
                    )
                )
            }.isSuccess

            // never delete a message that could not be saved
            if (!isKept) return@forEach
        }

        deleteMessage(match.id, match.isMms)
        affectedThreads.add(match.threadId)
        cleaned++
    }

    val remainingThreads = affectedThreads.filter { threadId ->
        runCatching { getConversations(threadId).isNotEmpty() }.getOrDefault(false)
    }
    affectedThreads.minus(remainingThreads.toSet()).forEach { conversationsDB.deleteThreadId(it) }
    updateLastConversationMessage(remainingThreads)

    if (cleaned > 0) {
        refreshMessages()
        refreshConversations()
    }

    return cleaned
}
