package org.fossify.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemAlarmBinding
import org.fossify.clock.extensions.*
import org.fossify.clock.helpers.TODAY_BIT
import org.fossify.clock.helpers.TOMORROW_BIT
import org.fossify.clock.helpers.getCurrentDayMinutes
import org.fossify.clock.interfaces.ToggleAlarmInterface
import org.fossify.clock.models.Alarm
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.toast
import org.fossify.commons.views.MyRecyclerView

class AlarmsAdapter(
    activity: SimpleActivity, var alarms: ArrayList<Alarm>, val toggleAlarmInterface: ToggleAlarmInterface,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit,
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_alarms

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = alarms.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = alarms.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = alarms.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemAlarmBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.bindView(alarm, true, true) { itemView, layoutPosition ->
            setupView(itemView, alarm)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = alarms.size

    fun updateItems(newItems: ArrayList<Alarm>) {
        alarms = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    private fun deleteItems() {
        val alarmsToRemove = ArrayList<Alarm>()
        val positions = getSelectedItemPositions()
        getSelectedItems().forEach {
            alarmsToRemove.add(it)
        }

        alarms.removeAll(alarmsToRemove)
        removeSelectedItems(positions)
        activity.dbHelper.deleteAlarms(alarmsToRemove)
    }

    private fun getSelectedItems() = alarms.filter { selectedKeys.contains(it.id) } as ArrayList<Alarm>

    private fun setupView(view: View, alarm: Alarm) {
        val isSelected = selectedKeys.contains(alarm.id)
        ItemAlarmBinding.bind(view).apply {
            alarmHolder.isSelected = isSelected
            alarmTime.text = activity.getFormattedTime(alarm.timeInMinutes * 60, false, true)
            alarmTime.setTextColor(textColor)

            alarmDays.text = activity.getAlarmSelectedDaysString(alarm.days)
            alarmDays.setTextColor(textColor)

            alarmLabel.text = alarm.label
            alarmLabel.setTextColor(textColor)
            alarmLabel.beVisibleIf(alarm.label.isNotEmpty())

            alarmSwitch.isChecked = alarm.isEnabled
            alarmSwitch.setColors(textColor, properPrimaryColor, backgroundColor)
            alarmSwitch.setOnClickListener {
                if (alarm.days > 0) {
                    if (activity.config.wasAlarmWarningShown) {
                        toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                    } else {
                        ConfirmationDialog(
                            activity,
                            messageId = org.fossify.commons.R.string.alarm_warning,
                            positive = org.fossify.commons.R.string.ok,
                            negative = 0
                        ) {
                            activity.config.wasAlarmWarningShown = true
                            toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                        }
                    }
                } else if (alarm.days == TODAY_BIT) {
                    if (alarm.timeInMinutes <= getCurrentDayMinutes()) {
                        alarm.days = TOMORROW_BIT
                        alarmDays.text = resources.getString(org.fossify.commons.R.string.tomorrow)
                    }
                    activity.dbHelper.updateAlarm(alarm)
                    root.context.scheduleNextAlarm(alarm, true)
                    toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                } else if (alarm.days == TOMORROW_BIT) {
                    toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                } else if (alarmSwitch.isChecked) {
                    activity.toast(R.string.no_days_selected)
                    alarmSwitch.isChecked = false
                } else {
                    toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                }
            }
        }
    }
}
