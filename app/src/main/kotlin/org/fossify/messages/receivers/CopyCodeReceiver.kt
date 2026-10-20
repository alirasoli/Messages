package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.extensions.copyVerificationCode
import org.fossify.messages.helpers.CODE
import org.fossify.messages.helpers.COPY_CODE

class CopyCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            COPY_CODE -> {
                val code = intent.getStringExtra(CODE)
                if (!code.isNullOrEmpty()) {
                    context.copyVerificationCode(code)
                }
            }
        }
    }
}
