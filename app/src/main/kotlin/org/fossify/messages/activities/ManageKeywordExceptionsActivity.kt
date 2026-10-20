package org.fossify.messages.activities

import android.os.Bundle
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.underlineText
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivityManageKeywordExceptionsBinding
import org.fossify.messages.dialogs.AddKeywordExceptionDialog
import org.fossify.messages.dialogs.ManageKeywordExceptionsAdapter
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.toArrayList

class ManageKeywordExceptionsActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityManageKeywordExceptionsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateKeywordExceptions()
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.manageKeywordExceptionsList))
        setupMaterialScrollListener(
            scrollingView = binding.manageKeywordExceptionsList,
            topAppBar = binding.keywordExceptionsAppbar
        )
        updateTextColors(binding.manageKeywordExceptionsWrapper)

        binding.manageKeywordExceptionsPlaceholder2.apply {
            underlineText()
            setTextColor(getProperPrimaryColor())
            setOnClickListener {
                addOrEditKeywordException()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.keywordExceptionsAppbar, NavigationIcon.Arrow)
    }

    private fun setupOptionsMenu() {
        binding.keywordExceptionsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_keyword_exception -> {
                    addOrEditKeywordException()
                    true
                }

                else -> false
            }
        }
    }

    override fun refreshItems() {
        updateKeywordExceptions()
    }

    private fun updateKeywordExceptions() {
        ensureBackgroundThread {
            val keywordExceptions = config.keywordExceptions.sorted().toArrayList()
            runOnUiThread {
                ManageKeywordExceptionsAdapter(
                    activity = this,
                    keywordExceptions = keywordExceptions,
                    listener = this,
                    recyclerView = binding.manageKeywordExceptionsList
                ) {
                    addOrEditKeywordException(it as String)
                }.apply {
                    binding.manageKeywordExceptionsList.adapter = this
                }

                binding.manageKeywordExceptionsPlaceholder.beVisibleIf(keywordExceptions.isEmpty())
                binding.manageKeywordExceptionsPlaceholder2.beVisibleIf(keywordExceptions.isEmpty())
            }
        }
    }

    private fun addOrEditKeywordException(keyword: String? = null) {
        AddKeywordExceptionDialog(this, keyword) {
            updateKeywordExceptions()
        }
    }
}
