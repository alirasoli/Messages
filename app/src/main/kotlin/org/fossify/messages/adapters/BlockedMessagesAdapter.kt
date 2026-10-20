package org.fossify.messages.adapters

import android.view.Menu
import android.view.ViewGroup
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.formatDateOrTime
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.databinding.ItemBlockedMessageBinding
import org.fossify.messages.extensions.deleteBlockedMessages
import org.fossify.messages.extensions.restoreBlockedMessages
import org.fossify.messages.helpers.BLOCK_REASON_KEYWORD
import org.fossify.messages.helpers.BLOCK_REASON_NUMBER
import org.fossify.messages.models.BlockedMessage

class BlockedMessagesAdapter(
    activity: SimpleActivity,
    private val messages: ArrayList<BlockedMessage>,
    recyclerView: MyRecyclerView,
    private val onChanged: () -> Unit,
    itemClick: (Any) -> Unit,
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_blocked_messages

    override fun prepareActionMode(menu: Menu) {
        menu.findItem(R.id.cab_blocked_copy).isVisible = isOneItemSelected()
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_blocked_copy -> copyText()
            R.id.cab_blocked_restore -> restoreSelected()
            R.id.cab_blocked_delete -> askConfirmDelete()
            R.id.cab_select_all -> selectAll()
        }
    }

    override fun getSelectableItemCount() = messages.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = messages.getOrNull(position)?.id?.hashCode()

    override fun getItemKeyPosition(key: Int) = messages.indexOfFirst { it.id.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun getItemCount() = messages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlockedMessageBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.bindView(message, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            ItemBlockedMessageBinding.bind(itemView).apply {
                root.setupViewBackground(activity)
                root.isSelected = selectedKeys.contains(message.id.hashCode())
                blockedMessageBody.text = message.body.ifEmpty { "…" }
                blockedMessageBody.setTextColor(textColor)
                val date = message.date.formatDateOrTime(
                    context = activity,
                    hideTimeOnOtherDays = false,
                    showCurrentYear = true
                )
                blockedMessageMeta.text = "${getReasonText(message)} · $date"
                blockedMessageMeta.setTextColor(textColor)
            }
        }
        bindViewHolder(holder)
    }

    private fun getReasonText(message: BlockedMessage) = when (message.reason) {
        BLOCK_REASON_KEYWORD -> activity.getString(R.string.blocked_reason_keyword, message.matchedKeyword.orEmpty())
        BLOCK_REASON_NUMBER -> activity.getString(R.string.blocked_reason_number)
        else -> activity.getString(R.string.blocked_reason_unknown)
    }

    private fun getSelectedItems() = messages.filter { selectedKeys.contains(it.id.hashCode()) }

    private fun copyText() {
        val message = getSelectedItems().firstOrNull() ?: return
        activity.copyToClipboard(message.body)
        finishActMode()
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val items = resources.getQuantityString(R.plurals.delete_messages, itemsCnt, itemsCnt)
        val baseString = org.fossify.commons.R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(activity, question) {
            deleteSelected()
        }
    }

    private fun restoreSelected() {
        val selected = getSelectedItems()
        ensureBackgroundThread {
            val restored = activity.restoreBlockedMessages(selected)
            activity.runOnUiThread {
                activity.toast(
                    if (restored > 0) R.string.blocked_restored else org.fossify.commons.R.string.unknown_error_occurred
                )
                finishAndReload()
            }
        }
    }

    private fun deleteSelected() {
        val ids = getSelectedItems().map { it.id }
        ensureBackgroundThread {
            activity.deleteBlockedMessages(ids)
            activity.runOnUiThread { finishAndReload() }
        }
    }

    private fun finishAndReload() {
        finishActMode()
        onChanged()
    }
}
