package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.extensions.config

object ReceiverUtils {

    fun isMessageFilteredOut(context: Context, body: String): Boolean {
        return findBlockedKeyword(context, body) != null
    }

    fun findBlockedKeyword(context: Context, body: String): String? {
        return context.config.blockedKeywords.firstOrNull { body.contains(it, ignoreCase = true) }
    }
}
