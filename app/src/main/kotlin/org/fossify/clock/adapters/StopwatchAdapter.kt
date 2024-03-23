package org.fossify.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemLapBinding
import org.fossify.clock.extensions.formatStopwatchTime
import org.fossify.clock.helpers.SORT_BY_LAP
import org.fossify.clock.helpers.SORT_BY_LAP_TIME
import org.fossify.clock.helpers.SORT_BY_TOTAL_TIME
import org.fossify.clock.models.Lap
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.views.MyRecyclerView

class StopwatchAdapter(activity: SimpleActivity, var laps: ArrayList<Lap>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    private var lastLapTimeView: TextView? = null
    private var lastTotalTimeView: TextView? = null
    private var lastLapId = 0

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

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val lap = laps[position]
        holder.bindView(lap, false, false) { itemView, layoutPosition ->
            setupView(itemView, lap)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = laps.size

    fun updateItems(newItems: ArrayList<Lap>) {
        lastLapId = 0
        laps = newItems.clone() as ArrayList<Lap>
        laps.sort()
        notifyDataSetChanged()
        finishActMode()
    }

    fun updateLastField(lapTime: Long, totalTime: Long) {
        lastLapTimeView?.text = lapTime.formatStopwatchTime(false)
        lastTotalTimeView?.text = totalTime.formatStopwatchTime(false)
    }

    private fun setupView(view: View, lap: Lap) {
        ItemLapBinding.bind(view).apply {
            lapOrder.text = lap.id.toString()
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

            if (lap.id > lastLapId) {
                lastLapTimeView = lapLapTime
                lastTotalTimeView = lapTotalTime
                lastLapId = lap.id
            }
        }
    }
}
