package com.supercilex.robotscouter.ui.scouting

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.model.ScoutsHolder
import com.supercilex.robotscouter.util.isPolynomial
import kotterknife.bindView

abstract class TabPagerAdapterBase(
        protected val fragment: Fragment,
        private val tabLayout: TabLayout,
        protected val dataRef: CollectionReference
) : FragmentStatePagerAdapter(fragment.childFragmentManager),
        TabLayout.OnTabSelectedListener, View.OnLongClickListener, DefaultLifecycleObserver,
        ChangeEventListenerBase {
    @get:StringRes protected abstract val editTabNameRes: Int
    private val noContentHint: View by fragment.bindView(R.id.no_content_hint)

    protected val holder: ScoutsHolder = ViewModelProviders.of(fragment).get(ScoutsHolder::class.java)
    protected var oldScouts: List<Scout> = emptyList()

    var currentTabId: String? = null
        set(value) {
            field = value
            holder.scouts.indexOfFirst { it.id == field }.let { if (it != -1) selectTab(it) }
        }
    val currentTab: TabLayout.Tab?
        get() = tabLayout.getTabAt(holder.scouts.indexOfFirst { it.id == currentTabId })

    init {
        fragment.lifecycle.addObserver(this)
    }

    override fun getCount() = holder.scouts.size

    override fun getItemPosition(any: Any) = PagerAdapter.POSITION_NONE

    override fun onTabSelected(tab: TabLayout.Tab) {
        currentTabId = holder.scouts[tab.position].id
    }

    override fun onDataChanged() {
        if (holder.scouts == oldScouts) return

        val prevTabId = currentTabId

        noContentHint.visibility = if (holder.scouts.isEmpty()) View.VISIBLE else View.GONE

        tabLayout.removeOnTabSelectedListener(this)
        notifyDataSetChanged()
        tabLayout.addOnTabSelectedListener(this)

        (0 until tabLayout.tabCount).map {
            tabLayout.getTabAt(it)!!
        }.forEachIndexed { index, tab ->
            tab.text = holder.scouts[index].name ?: getPageTitle(index)

            val tabView = (tabLayout.getChildAt(0) as LinearLayout).getChildAt(index)
            tabView.setOnLongClickListener(this@TabPagerAdapterBase)
            tabView.id = index
        }

        if (holder.scouts.isNotEmpty()) {
            if (TextUtils.isEmpty(prevTabId)) {
                currentTabId = holder.scouts[0].id
            } else {
                holder.scouts.find { it.id == prevTabId }?.let {
                    selectTab(holder.scouts.indexOfFirst { it.id == currentTabId })
                } ?: run {
                    val index = oldScouts.indexOfFirst { it.id == prevTabId }
                    currentTabId = if (oldScouts.isPolynomial) {
                        (if (oldScouts.lastIndex > index) {
                            oldScouts[index + 1]
                        } else {
                            oldScouts[index - 1]
                        }).id
                    } else {
                        null
                    }
                }
            }
        }

        oldScouts = ArrayList(holder.scouts)
    }

    fun onSaveInstanceState(outState: Bundle) = outState.putAll(getTabIdBundle(currentTabId))

    private fun selectTab(index: Int) = tabLayout.getTabAt(index)?.select()

    override fun onStart(owner: LifecycleOwner) {
        holder.scouts.addChangeEventListener(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        holder.scouts.removeChangeEventListener(this)
    }

    override fun onLongClick(v: View): Boolean {
        TabNameDialog.show(
                fragment.childFragmentManager,
                dataRef.document(holder.scouts[v.id].id),
                editTabNameRes,
                tabLayout.getTabAt(v.id)!!.text!!.toString()
        )
        return true
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabReselected(tab: TabLayout.Tab) = Unit
}
