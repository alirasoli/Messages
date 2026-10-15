package org.fossify.messages.dialogs

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.getPopupMenuTheme
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.databinding.ItemManageKeywordExceptionBinding
import org.fossify.messages.extensions.config

class ManageKeywordExceptionsAdapter(
    activity: BaseSimpleActivity, var keywordExceptions: ArrayList<String>, val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_blocked_keywords

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_copy_keyword).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_copy_keyword -> copyKeywordToClipboard()
            R.id.cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = keywordExceptions.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = keywordExceptions.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = keywordExceptions.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManageKeywordExceptionBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val keywordException = keywordExceptions[position]
        holder.bindView(keywordException, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, keywordException)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = keywordExceptions.size

    private fun getSelectedItems() = keywordExceptions.filter { selectedKeys.contains(it.hashCode()) }

    private fun setupView(view: View, keywordException: String) {
        ItemManageKeywordExceptionBinding.bind(view).apply {
            root.setupViewBackground(activity)
            manageKeywordExceptionHolder.isSelected = selectedKeys.contains(keywordException.hashCode())
            manageKeywordExceptionTitle.apply {
                text = keywordException
                setTextColor(textColor)
            }

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, keywordException)
            }
        }
    }

    private fun showPopupMenu(view: View, keywordException: String) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val keywordId = keywordException.hashCode()
                when (item.itemId) {
                    R.id.cab_copy_keyword -> {
                        executeItemMenuOperation(keywordId) {
                            copyKeywordToClipboard()
                        }
                    }

                    R.id.cab_delete -> {
                        executeItemMenuOperation(keywordId) {
                            deleteSelection()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(keywordId: Int, callback: () -> Unit) {
        selectedKeys.add(keywordId)
        callback()
        selectedKeys.remove(keywordId)
    }

    private fun copyKeywordToClipboard() {
        val selectedKeyword = getSelectedItems().firstOrNull() ?: return
        activity.copyToClipboard(selectedKeyword)
        finishActMode()
    }

    private fun deleteSelection() {
        val deletedKeywords = HashSet<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            deletedKeywords.add(it)
            activity.config.removeKeywordException(it)
        }

        keywordExceptions.removeAll(deletedKeywords)
        removeSelectedItems(positions)
        if (keywordExceptions.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
