package com.simplemobiletools.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.extensions.formatAlarmTime
import com.simplemobiletools.clock.interfaces.ToggleAlarmInterface
import com.simplemobiletools.clock.models.Alarm
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getSelectedDaysString
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.item_alarm.view.*
import java.util.*

class AlarmsAdapter(activity: SimpleActivity, var alarms: ArrayList<Alarm>, val toggleAlarmInterface: ToggleAlarmInterface,
                    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {
    private val adjustedPrimaryColor = activity.getAdjustedPrimaryColor()

    override fun getActionMenuId() = R.menu.cab_alarms

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.alarm_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        if (selectedPositions.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = alarms.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_alarm, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val alarm = alarms[position]
        val view = holder.bindView(alarm, true) { itemView, layoutPosition ->
            setupView(itemView, alarm)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = alarms.size

    fun updateItems(newItems: ArrayList<Alarm>) {
        alarms = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    private fun deleteItems() {
        val alarmsToRemove = ArrayList<Alarm>()
        selectedPositions.sortedDescending().forEach {
            val alarm = alarms[it]
            alarmsToRemove.add(alarm)
        }

        alarms.removeAll(alarmsToRemove)
        removeSelectedItems()
    }

    private fun setupView(view: View, alarm: Alarm) {
        view.apply {
            alarm_time.text = alarm.timeInMinutes.formatAlarmTime()
            alarm_time.setTextColor(textColor)

            alarm_days.text = activity.getSelectedDaysString(alarm.days)
            alarm_days.setTextColor(textColor)

            alarm_switch.isChecked = alarm.isEnabled
            alarm_switch.setColors(textColor, adjustedPrimaryColor, backgroundColor)
            alarm_switch.setOnClickListener {
                if (alarm.days > 0) {
                    toggleAlarmInterface.alarmToggled(alarm.id, alarm_switch.isChecked)
                } else {
                    activity.toast(R.string.no_days_selected)
                    alarm_switch.isChecked = false
                }
            }
        }
    }
}
