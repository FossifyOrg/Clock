package org.fossify.clock.adapters

import android.annotation.SuppressLint
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemLapBinding
import org.fossify.clock.extensions.formatStopwatchTime
import org.fossify.clock.helpers.SORT_BY_LAP
import org.fossify.clock.helpers.SORT_BY_LAP_TIME
import org.fossify.clock.helpers.SORT_BY_TOTAL_TIME
import org.fossify.clock.helpers.isLive
import org.fossify.clock.models.Lap
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.views.MyRecyclerView

class StopwatchAdapter(
    activity: SimpleActivity,
    private var laps: ArrayList<Lap>,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit,
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    init {
        recyclerView.itemAnimator = null
    }

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = laps.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = laps.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = laps.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemLapBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lap = laps[position]
        holder.bindView(lap, false, false) { itemView, layoutPosition ->
            setupView(itemView, lap)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = laps.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: ArrayList<Lap>) {
        laps = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    fun updateLiveLap(totalTime: Long, lapTime: Long) {
        val oldIndex = laps.indexOfFirst { it.isLive() }
        if (oldIndex == -1) return
        laps[oldIndex].lapTime = lapTime
        laps[oldIndex].totalTime = totalTime
        laps.sort()

        // the live lap might have changed position
        val newIndex = laps.indexOfFirst { it.isLive() }
        if (oldIndex == newIndex) {
            notifyItemChanged(newIndex)
        } else {
            notifyItemMoved(oldIndex, newIndex)
            notifyItemChanged(newIndex)
        }
    }

    private fun setupView(view: View, lap: Lap) {
        ItemLapBinding.bind(view).apply {
            lapOrder.text = if (lap.isLive()) laps.size.toString() else lap.id.toString()
            lapOrder.setTextColor(textColor)
            lapOrder.setOnClickListener {
                itemClick(SORT_BY_LAP)
            }

            lapLapTime.text = lap.lapTime.formatStopwatchTime(false)
            lapLapTime.setTextColor(textColor)
            lapLapTime.setOnClickListener {
                itemClick(SORT_BY_LAP_TIME)
            }

            lapTotalTime.text = lap.totalTime.formatStopwatchTime(false)
            lapTotalTime.setTextColor(textColor)
            lapTotalTime.setOnClickListener {
                itemClick(SORT_BY_TOTAL_TIME)
            }
        }
    }
}
