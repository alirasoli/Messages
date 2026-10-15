package org.fossify.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.value
import org.fossify.messages.databinding.DialogAddKeywordExceptionBinding
import org.fossify.messages.extensions.config

class AddKeywordExceptionDialog(
    val activity: BaseSimpleActivity,
    private val originalKeyword: String? = null,
    val callback: () -> Unit
) {
    init {
        val binding = DialogAddKeywordExceptionBinding.inflate(activity.layoutInflater).apply {
            if (originalKeyword != null) {
                addKeywordExceptionEdittext.setText(originalKeyword)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.addKeywordExceptionEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newKeyword = binding.addKeywordExceptionEdittext.value
                        if (originalKeyword != null && newKeyword != originalKeyword) {
                            activity.config.removeKeywordException(originalKeyword)
                        }

                        if (newKeyword.isNotEmpty()) {
                            activity.config.addKeywordException(newKeyword)
                        }

                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
