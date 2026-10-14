package org.fossify.messages.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.areSystemAnimationsEnabled
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.adapters.BlockedConversationsAdapter
import org.fossify.messages.databinding.ActivityArchivedConversationsBinding
import org.fossify.messages.extensions.blockedMessagesDB
import org.fossify.messages.extensions.getBlockedConversations
import org.fossify.messages.extensions.purgeExpiredBlockedMessages
import org.fossify.messages.helpers.BLOCKED_ADDRESS
import org.fossify.messages.helpers.BLOCKED_TITLE
import org.fossify.messages.models.Conversation

class BlockedMessagesActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityArchivedConversationsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.archiveToolbar.title = getString(R.string.blocked_messages)
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList))
        setupMaterialScrollListener(
            scrollingView = binding.conversationsList,
            topAppBar = binding.archiveAppbar
        )
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.archiveAppbar, NavigationIcon.Arrow)
        loadBlockedConversations()
    }

    private fun setupOptionsMenu() {
        binding.archiveToolbar.inflateMenu(R.menu.blocked_messages_menu)
        binding.archiveToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.blocked_delete_all -> deleteAll()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun loadBlockedConversations() {
        ensureBackgroundThread {
            val conversations = try {
                purgeExpiredBlockedMessages()
                getBlockedConversations()
            } catch (e: Exception) {
                ArrayList()
            }

            runOnUiThread {
                setupConversations(conversations)
            }
        }
    }

    private fun deleteAll() {
        ConfirmationDialog(
            activity = this,
            message = "",
            messageId = R.string.blocked_delete_all_confirmation,
            positive = org.fossify.commons.R.string.yes,
            negative = org.fossify.commons.R.string.no
        ) {
            ensureBackgroundThread {
                blockedMessagesDB.deleteAll()
                loadBlockedConversations()
            }
        }
    }

    private fun getOrCreateConversationsAdapter(): BlockedConversationsAdapter {
        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = BlockedConversationsAdapter(
                activity = this,
                recyclerView = binding.conversationsList,
                onRefresh = { notifyDatasetChanged() },
                onChanged = { loadBlockedConversations() },
                itemClick = { handleConversationClick(it) }
            )

            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) {
                binding.conversationsList.scheduleLayoutAnimation()
            }
        }
        return currAdapter as BlockedConversationsAdapter
    }

    private fun setupConversations(conversations: ArrayList<Conversation>) {
        showOrHidePlaceholder(conversations.isEmpty())
        binding.archiveToolbar.menu.findItem(R.id.blocked_delete_all).isVisible = conversations.isNotEmpty()

        try {
            getOrCreateConversationsAdapter().updateConversations(conversations)
        } catch (ignored: Exception) {
        }
    }

    private fun showOrHidePlaceholder(show: Boolean) {
        binding.conversationsFastscroller.beGoneIf(show)
        binding.noConversationsPlaceholder.beVisibleIf(show)
        binding.noConversationsPlaceholder.setTextColor(getProperTextColor())
        binding.noConversationsPlaceholder.text = getString(R.string.blocked_no_messages)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDatasetChanged() {
        getOrCreateConversationsAdapter().notifyDataSetChanged()
    }

    private fun handleConversationClick(any: Any) {
        Intent(this, BlockedThreadActivity::class.java).apply {
            val conversation = any as Conversation
            putExtra(BLOCKED_ADDRESS, conversation.phoneNumber)
            putExtra(BLOCKED_TITLE, conversation.title)
            startActivity(this)
        }
    }
}
