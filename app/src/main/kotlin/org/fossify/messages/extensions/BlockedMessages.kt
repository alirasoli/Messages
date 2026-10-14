package org.fossify.messages.extensions

import android.content.Context
import android.provider.Telephony.Sms
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.interfaces.BlockedMessagesDao
import org.fossify.messages.models.BlockedMessage
import org.fossify.messages.models.Conversation
import java.util.concurrent.TimeUnit

private const val SQL_CHUNK_SIZE = 500

val Context.blockedMessagesDB: BlockedMessagesDao
    get() = getMessagesDB().BlockedMessagesDao()

fun Context.keepBlockedMessage(
    address: String,
    body: String,
    date: Long,
    subscriptionId: Int,
    reason: Int,
    matchedKeyword: String? = null,
) {
    if (!config.keepBlockedMessages) {
        return
    }

    try {
        purgeExpiredBlockedMessages()
        blockedMessagesDB.insert(
            BlockedMessage(
                address = address,
                body = body,
                date = date,
                subscriptionId = subscriptionId,
                reason = reason,
                matchedKeyword = matchedKeyword
            )
        )
    } catch (_: Exception) {
    }
}

fun Context.purgeExpiredBlockedMessages() {
    val days = config.blockedMessagesRetentionDays
    if (days > 0) {
        blockedMessagesDB.deleteOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong()))
    }
}

fun Context.getBlockedConversations(): ArrayList<Conversation> {
    val helper = SimpleContactsHelper(this)
    val conversations = blockedMessagesDB.getAll().groupBy { it.address }.map { (address, messages) ->
        val latest = messages.first()
        val name = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            getNameFromAddress(address, it)
        }

        Conversation(
            threadId = address.hashCode().toLong(),
            snippet = latest.body,
            date = (latest.date / 1000).toInt(),
            read = false,
            title = name,
            photoUri = helper.getPhotoUriFromPhoneNumber(address),
            isGroupConversation = false,
            phoneNumber = address,
            unreadCount = messages.size
        )
    }

    return ArrayList(conversations)
}

fun Context.deleteBlockedMessages(ids: List<Long>) {
    ids.chunked(SQL_CHUNK_SIZE).forEach { blockedMessagesDB.deleteByIds(it) }
}

fun Context.deleteBlockedMessagesFromAddresses(addresses: List<String>) {
    addresses.chunked(SQL_CHUNK_SIZE).forEach { blockedMessagesDB.deleteByAddresses(it) }
}

fun Context.getBlockedMessagesFromAddresses(addresses: List<String>): List<BlockedMessage> {
    return addresses.chunked(SQL_CHUNK_SIZE).flatMap { blockedMessagesDB.getForAddresses(it) }
}

fun Context.restoreBlockedMessages(messages: List<BlockedMessage>): Int {
    var restored = 0
    messages.forEach { message ->
        val threadId = getThreadId(message.address)
        if (threadId == 0L) {
            return@forEach
        }

        val newId = insertNewSMS(
            address = message.address,
            subject = "",
            body = message.body,
            date = message.date,
            read = 0,
            threadId = threadId,
            type = Sms.MESSAGE_TYPE_INBOX,
            subscriptionId = message.subscriptionId
        )

        if (newId == 0L) {
            return@forEach
        }

        getConversations(threadId).firstOrNull()?.let { conversation ->
            runCatching { insertOrUpdateConversation(conversation) }
        }

        if (shouldUnarchive()) {
            updateConversationArchivedStatus(threadId, false)
        }

        blockedMessagesDB.deleteByIds(listOf(message.id))
        restored++
    }

    if (restored > 0) {
        refreshMessages()
        refreshConversations()
    }

    return restored
}
