package com.supercilex.robotscouter.feature.trash

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.supercilex.robotscouter.common.DeletionType
import com.supercilex.robotscouter.common.FIRESTORE_DELETION_QUEUE
import com.supercilex.robotscouter.core.data.model.untrashTeam
import com.supercilex.robotscouter.core.data.model.untrashTemplate
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutListener
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.fragment_trash.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

internal class TrashFragment : FragmentBase(), View.OnClickListener,
        OnBackPressedListener, KeyboardShortcutListener {
    private val holder by unsafeLazy { ViewModelProviders.of(this).get<TrashHolder>() }

    private lateinit var selectionTracker: SelectionTracker<String>
    private lateinit var menuHelper: TrashMenuHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        holder.init(null)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_trash, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        action_empty_trash.setOnClickListener(this)
        (activity as AppCompatActivity).apply {
            setSupportActionBar(find(RC.id.toolbar))
            checkNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        }

        trashList.layoutManager = LinearLayoutManager(requireContext())
        trashList.addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        val adapter = TrashListAdapter()
        trashList.adapter = adapter

        selectionTracker = SelectionTracker.Builder<String>(
                FIRESTORE_DELETION_QUEUE,
                trashList,
                TrashKeyProvider(holder.trashListener),
                TrashDetailsLookup(trashList),
                StorageStrategy.createStringStorage()
        ).build().apply {
            adapter.selectionTracker = this
            addObserver(TrashMenuHelper(this@TrashFragment, this).also { menuHelper = it })
            onRestoreInstanceState(savedInstanceState)
        }

        holder.trashListener.observe(viewLifecycleOwner, Observer {
            val hasTrash = it.orEmpty().isNotEmpty()

            noTrashHint.animatePopReveal(!hasTrash)
            notice.isVisible = hasTrash

            if (it != null) for (i in 0 until adapter.itemCount) {
                val item = adapter.getItem(i)
                if (!it.contains(item)) selectionTracker.deselect(item.id)
            }
            adapter.submitList(it)
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuHelper.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = menuHelper.onOptionsItemSelected(item)

    override fun onShortcut(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL ->
                if (selectionTracker.hasSelection()) emptySelected() else emptyAll()
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        emptyAll()
    }

    override fun onBackPressed() = selectionTracker.clearSelection()

    fun restoreSelected() {
        val trashes = selectionTracker.selection.toList().map { key ->
            holder.trashListener.value.orEmpty().single { it.id == key }
        }.onEach { (id, _, type) ->
            when (type) {
                DeletionType.TEAM -> untrashTeam(id)
                DeletionType.TEMPLATE -> untrashTemplate(id)
                else -> error("Unsupported type: $type")
            }
        }

        longSnackbar(checkNotNull(view), resources.getQuantityString(
                R.plurals.trash_restored_message, trashes.size, trashes.size))
    }

    fun emptySelected() {
        showEmptyTrashDialog(selectionTracker.selection.toList())
    }

    private fun emptyAll() {
        showEmptyTrashDialog(holder.trashListener.value.orEmpty().map { it.id }, true)
    }

    private fun showEmptyTrashDialog(ids: List<String>, emptyAll: Boolean = false) {
        EmtpyTrashDialog.show(childFragmentManager, ids, emptyAll)
    }

    companion object {
        const val TAG = "TrashFragment"

        fun newInstance() = TrashFragment()
    }
}
