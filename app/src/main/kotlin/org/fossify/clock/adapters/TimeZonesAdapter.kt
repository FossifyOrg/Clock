package org.fossify.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemTimeZoneBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getFormattedDate
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.models.MyTimeZone
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.views.MyRecyclerView
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class TimeZonesAdapter(activity: SimpleActivity, var timeZones: ArrayList<MyTimeZone>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    var todayDateString = activity.getFormattedDate(Calendar.getInstance())

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_timezones

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = timeZones.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = timeZones.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = timeZones.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemTimeZoneBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val timeZone = timeZones[position]
        holder.bindView(timeZone, true, true) { itemView, layoutPosition ->
            setupView(itemView, timeZone)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = timeZones.size

    fun updateItems(newItems: ArrayList<MyTimeZone>) {
        timeZones = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    fun updateTimes() {
        notifyDataSetChanged()
    }

    private fun deleteItems() {
        val timeZonesToRemove = ArrayList<MyTimeZone>(selectedKeys.size)
        val timeZoneIDsToRemove = ArrayList<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()
        getSelectedItems().forEach {
            timeZonesToRemove.add(it)
            timeZoneIDsToRemove.add(it.id.toString())
        }

        timeZones.removeAll(timeZonesToRemove)
        removeSelectedItems(positions)

        val selectedTimeZones = activity.config.selectedTimeZones
        val newTimeZones = selectedTimeZones.filter { !timeZoneIDsToRemove.contains(it) }.toHashSet()
        activity.config.selectedTimeZones = newTimeZones
    }

    private fun getSelectedItems() = timeZones.filter { selectedKeys.contains(it.id) } as ArrayList<MyTimeZone>

    private fun setupView(view: View, timeZone: MyTimeZone) {
        val currTimeZone = TimeZone.getTimeZone(timeZone.zoneName)
        val calendar = Calendar.getInstance(currTimeZone)
        var offset = calendar.timeZone.rawOffset
        val isDaylightSavingActive = currTimeZone.inDaylightTime(Date())
        if (isDaylightSavingActive) {
            offset += currTimeZone.dstSavings
        }
        val passedSeconds = ((calendar.timeInMillis + offset) / 1000).toInt()
        val formattedTime = activity.getFormattedTime(passedSeconds, false, false)
        val formattedDate = activity.getFormattedDate(calendar)

        val isSelected = selectedKeys.contains(timeZone.id)
        ItemTimeZoneBinding.bind(view).apply {
            timeZoneFrame.isSelected = isSelected
            timeZoneTitle.text = timeZone.title
            timeZoneTitle.setTextColor(textColor)

            timeZoneTime.text = formattedTime
            timeZoneTime.setTextColor(textColor)

            if (formattedDate != todayDateString) {
                timeZoneDate.beVisible()
                timeZoneDate.text = formattedDate
                timeZoneDate.setTextColor(textColor)
            } else {
                timeZoneDate.beGone()
            }
        }
    }
}
