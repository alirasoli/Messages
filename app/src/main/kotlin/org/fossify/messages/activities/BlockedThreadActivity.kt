package org.fossify.messages.activities

import android.os.Bundle
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.adapters.BlockedMessagesAdapter
import org.fossify.messages.databinding.ActivityArchivedConversationsBinding
import org.fossify.messages.extensions.blockedMessagesDB
import org.fossify.messages.helpers.BLOCKED_ADDRESS
import org.fossify.messages.helpers.BLOCKED_TITLE
import org.fossify.messages.models.BlockedMessage

class BlockedThreadActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityArchivedConversationsBinding::inflate)
    private var address = ""
    private var scrolledToEnd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        address = intent.getStringExtra(BLOCKED_ADDRESS).orEmpty()
        binding.archiveToolbar.title = intent.getStringExtra(BLOCKED_TITLE).orEmpty().ifEmpty { address }

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList))
        setupMaterialScrollListener(
            scrollingView = binding.conversationsList,
            topAppBar = binding.archiveAppbar
        )
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.archiveAppbar, NavigationIcon.Arrow)
        loadMessages()
    }

    private fun loadMessages() {
        ensureBackgroundThread {
            val messages = try {
                ArrayList(blockedMessagesDB.getForAddress(address))
            } catch (e: Exception) {
                ArrayList()
            }

            runOnUiThread {
                setupMessages(messages)
            }
        }
    }

    private fun setupMessages(messages: ArrayList<BlockedMessage>) {
        if (messages.isEmpty()) {
            finish()
            return
        }

        binding.conversationsList.adapter = BlockedMessagesAdapter(
            activity = this,
            messages = messages,
            recyclerView = binding.conversationsList,
            onChanged = { loadMessages() },
            itemClick = {}
        )

        if (!scrolledToEnd) {
            scrolledToEnd = true
            binding.conversationsList.scrollToPosition(messages.size - 1)
        }
    }
}
