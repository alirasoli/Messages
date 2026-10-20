package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.isSavedContact

object ReceiverUtils {

    fun isMessageFilteredOut(context: Context, body: String): Boolean {
        return findBlockedKeyword(context, body) != null
    }

    /**
     * Returns the blocked keyword matching [body], or null if the message should not be blocked
     * because of keywords. A match is ignored when one of the keyword exceptions applies.
     */
    fun findBlockedKeyword(context: Context, body: String, address: String? = null): String? {
        val config = context.config
        val keyword = config.blockedKeywords.firstOrNull { body.contains(it, ignoreCase = true) }
            ?: return null

        val hasExceptionKeyword = config.keywordExceptions.any { body.contains(it, ignoreCase = true) }
        if (hasExceptionKeyword) return null

        if (config.allowKeywordsFromContacts && !address.isNullOrBlank() && context.isSavedContact(address)) {
            return null
        }

        return keyword
    }
}
