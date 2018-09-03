package com.supercilex.robotscouter.feature.trash

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.LiveData
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.supercilex.robotscouter.core.ui.ItemDetailsBase
import com.supercilex.robotscouter.core.ui.ItemDetailsLookupBase
import com.supercilex.robotscouter.core.ui.ToolbarMenuHelperBase

internal class TrashKeyProvider(
        private val observable: LiveData<List<Trash>?>
) : ItemKeyProvider<String>(SCOPE_MAPPED) {
    override fun getKey(position: Int) = observable.value?.get(position)?.id

    override fun getPosition(key: String) =
            observable.value.orEmpty().indexOfFirst { it.id == key }
}

internal class TrashDetailsLookup(recyclerView: RecyclerView) :
        ItemDetailsLookupBase<String, TrashViewHolder, TrashDetails>(recyclerView, ::TrashDetails)

internal class TrashDetails(
        private val holder: TrashViewHolder
) : ItemDetailsBase<String, TrashViewHolder>(holder) {
    override fun getSelectionKey() = holder.trash.id
}

internal class TrashMenuHelper(
        private val fragment: TrashFragment,
        tracker: SelectionTracker<String>
) : ToolbarMenuHelperBase<String>(fragment.requireActivity() as AppCompatActivity, tracker) {
    private val menuItems = mutableListOf<MenuItem>()

    override fun handleNavigationClick(hasSelection: Boolean) {
        if (!hasSelection) fragment.requireActivity().onBackPressed()
    }

    override fun createMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.trash_options, menu)

        menuItems.clear()
        menuItems.addAll(menu.children.filter { menuIds.contains(it.itemId) })
    }

    override fun handleItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_restore -> fragment.restoreSelected()
            R.id.action_delete -> fragment.emptySelected()
            else -> return false
        }
        return true
    }

    override fun updateMultiSelectMenu(visible: Boolean) {
        for (item in menuItems) item.isVisible = visible
    }

    private companion object {
        val menuIds = listOf(R.id.action_restore, R.id.action_delete)
    }
}
