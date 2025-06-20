package org.fossify.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemLapBinding
import org.fossify.clock.extensions.formatStopwatchTime
import org.fossify.clock.helpers.SORT_BY_LAP
import org.fossify.clock.helpers.SORT_BY_LAP_TIME
import org.fossify.clock.helpers.SORT_BY_TOTAL_TIME
import org.fossify.clock.extensions.isLive
import org.fossify.clock.models.Lap
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.views.MyRecyclerView

class StopwatchAdapter(
    activity: SimpleActivity,
    recyclerView: MyRecyclerView,
    private val onItemClick: (Any) -> Unit,
) : MyRecyclerViewListAdapter<Lap>(activity, recyclerView, LapDiffCallback(), {}) {

    init {
        setHasStableIds(true)
        recyclerView.itemAnimator = null
    }

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = currentList.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = currentList.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = currentList.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id?.toLong() ?: 0L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemLapBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lap = getItem(position)
        holder.bindView(lap, false, false) { itemView, layoutPosition ->
            setupView(itemView, lap)
        }
        bindViewHolder(holder)
    }

    private fun setupView(view: View, lap: Lap) {
        ItemLapBinding.bind(view).apply {
            lapOrder.text = if (lap.isLive()) currentList.size.toString() else lap.id.toString()
            lapOrder.setTextColor(textColor)
            lapOrder.setOnClickListener {
                onItemClick(SORT_BY_LAP)
            }

            lapLapTime.text = lap.lapTime.formatStopwatchTime(false)
            lapLapTime.setTextColor(textColor)
            lapLapTime.setOnClickListener {
                onItemClick(SORT_BY_LAP_TIME)
            }

            lapTotalTime.text = lap.totalTime.formatStopwatchTime(false)
            lapTotalTime.setTextColor(textColor)
            lapTotalTime.setOnClickListener {
                onItemClick(SORT_BY_TOTAL_TIME)
            }
        }
    }

    private class LapDiffCallback : DiffUtil.ItemCallback<Lap>() {
        override fun areItemsTheSame(oldItem: Lap, newItem: Lap) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Lap, newItem: Lap) = oldItem == newItem
    }
}
