package com.supercilex.robotscouter.feature.teams

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.selection.SelectionTracker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.supercilex.robotscouter.DrawerToggler
import com.supercilex.robotscouter.TeamExporter
import com.supercilex.robotscouter.core.data.isFullUser
import com.supercilex.robotscouter.core.ui.DrawerMenuHelperBase
import com.supercilex.robotscouter.shared.TeamDetailsDialog
import com.supercilex.robotscouter.shared.TeamSharer
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

internal class TeamMenuHelper(
        private val fragment: TeamListFragment,
        private val tracker: SelectionTracker<String>
) : DrawerMenuHelperBase<String>(fragment.requireActivity() as AppCompatActivity, tracker) {
    private val activity = fragment.requireActivity() as AppCompatActivity

    private val fab = activity.find<FloatingActionButton>(RC.id.fab)
    private val drawerLayout = activity.find<DrawerLayout>(RC.id.drawerLayout)

    private var isMenuReady = false
    private val teamMenuItems = mutableListOf<MenuItem>()
    private val teamsMenuItems = mutableListOf<MenuItem>()
    private val normalMenuItems = mutableListOf<MenuItem>()

    override fun handleNavigationClick(hasSelection: Boolean) {
        if (!hasSelection) drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun createMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.team_options, menu)

        teamMenuItems.clear()
        teamMenuItems.addAll(menu.children.filter { teamMenuIds.contains(it.itemId) })
        teamsMenuItems.clear()
        teamsMenuItems.addAll(menu.children.filter { teamsMenuIds.contains(it.itemId) })

        normalMenuItems.clear()
        normalMenuItems.addAll(menu.children.filterNot { menuIds.contains(it.itemId) })

        isMenuReady = true
    }

    override fun handleItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_export_teams -> (activity as TeamExporter).export()
            R.id.action_share -> if (TeamSharer.shareTeams(fragment, fragment.selectedTeams)) {
                tracker.clearSelection()
            }
            R.id.action_edit_team_details -> TeamDetailsDialog.show(
                    fragment.childFragmentManager, fragment.selectedTeams.first())
            R.id.action_delete -> DeleteTeamDialog.show(
                    fragment.childFragmentManager, fragment.selectedTeams)
            else -> return false
        }
        return true
    }

    override fun updateNormalMenu(visible: Boolean) {
        for (item in normalMenuItems) {
            item.isVisible =
                    visible && if (item.itemId == RC.id.action_sign_in) !isFullUser else true
        }

        if (visible) {
            fab.show()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED)
        } else {
            fab.hide()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        setupIcon(visible)
    }

    override fun updateSingleSelectMenu(visible: Boolean) {
        for (item in teamMenuItems) item.isVisible = visible
    }

    override fun updateMultiSelectMenu(visible: Boolean) {
        for (item in teamsMenuItems) item.isVisible = visible
    }

    private fun setupIcon(visible: Boolean) {
        val toggler = activity as DrawerToggler
        if (visible) {
            toggler.toggle(true)
        } else {
            checkNotNull(activity.supportActionBar).apply {
                // Replace hamburger icon with back button
                setDisplayHomeAsUpEnabled(false)
                toggler.toggle(false)
                setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private companion object {
        val teamMenuIds = listOf(R.id.action_edit_team_details)
        val teamsMenuIds = listOf(
                R.id.action_export_teams,
                R.id.action_share,
                R.id.action_delete
        )

        val menuIds = teamMenuIds + teamsMenuIds
    }
}
