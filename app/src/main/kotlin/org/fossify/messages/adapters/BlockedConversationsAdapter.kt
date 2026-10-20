package org.fossify.messages.adapters

import android.view.Menu
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.extensions.deleteBlockedMessagesFromAddresses
import org.fossify.messages.extensions.getBlockedMessagesFromAddresses
import org.fossify.messages.extensions.restoreBlockedMessages

class BlockedConversationsAdapter(
    activity: SimpleActivity,
    recyclerView: MyRecyclerView,
    onRefresh: () -> Unit,
    private val onChanged: () -> Unit,
    itemClick: (Any) -> Unit,
) : BaseConversationsAdapter(activity, recyclerView, onRefresh, itemClick) {
    override fun getActionMenuId() = R.menu.cab_blocked_conversations

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_blocked_delete -> askConfirmDelete()
            R.id.cab_blocked_restore -> restoreSelected()
            R.id.cab_select_all -> selectAll()
        }
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val items = resources.getQuantityString(R.plurals.delete_conversations, itemsCnt, itemsCnt)
        val baseString = org.fossify.commons.R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(activity, question) {
            val addresses = getSelectedItems().map { it.phoneNumber }
            ensureBackgroundThread {
                activity.deleteBlockedMessagesFromAddresses(addresses)
                finishAndReload()
            }
        }
    }

    private fun restoreSelected() {
        val addresses = getSelectedItems().map { it.phoneNumber }
        ensureBackgroundThread {
            val messages = activity.getBlockedMessagesFromAddresses(addresses)
            val restored = activity.restoreBlockedMessages(messages)
            activity.runOnUiThread {
                activity.toast(
                    if (restored > 0) R.string.blocked_restored else org.fossify.commons.R.string.unknown_error_occurred
                )
            }
            finishAndReload()
        }
    }

    private fun finishAndReload() {
        activity.runOnUiThread {
            finishActMode()
            onChanged()
        }
    }
}
