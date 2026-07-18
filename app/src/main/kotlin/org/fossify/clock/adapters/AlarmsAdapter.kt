package org.fossify.clock.adapters

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fossify.clock.R
import org.fossify.clock.activities.GroupActivity
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemAlarmBinding
import org.fossify.clock.databinding.ItemAlarmGroupBinding
import org.fossify.clock.dialogs.AddToGroupDialog
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.helpers.updateNonRecurringAlarmDay
import org.fossify.clock.interfaces.ToggleAlarmInterface
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.clock.models.AlarmListItem
import org.fossify.clock.models.Group
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getSelectedDaysString
import org.fossify.commons.extensions.move
import org.fossify.commons.extensions.moveLastItemToFront
import org.fossify.commons.helpers.EVERY_DAY_BIT
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.interfaces.ItemMoveCallback
import org.fossify.commons.interfaces.ItemTouchHelperContract
import org.fossify.commons.interfaces.StartReorderDragListener
import org.fossify.commons.views.MyRecyclerView
import org.greenrobot.eventbus.EventBus

class AlarmsAdapter(
    activity: SimpleActivity,
    private var items: ArrayList<AlarmListItem>,
    private val toggleAlarmInterface: ToggleAlarmInterface,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit,
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick), ItemTouchHelperContract {

     companion object {
        private const val VIEW_TYPE_ALARM = 0
        private const val VIEW_TYPE_GROUP = 1
    }

    private var startReorderDragListener: StartReorderDragListener

    init {
        setupDragListener(true)
        val touchHelper = ItemTouchHelper(ItemMoveCallback(this))
        touchHelper.attachToRecyclerView(recyclerView)

        startReorderDragListener = object : StartReorderDragListener {
            override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_alarms

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_add_to_group -> addSelectedToGroup()
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = items.count { it is AlarmListItem.AlarmRow }

    override fun getIsItemSelectable(position: Int) = items.getOrNull(position) is AlarmListItem.AlarmRow

    override fun getItemSelectionKey(position: Int) =
        (items.getOrNull(position) as? AlarmListItem.AlarmRow)?.alarm?.id

    override fun getItemKeyPosition(key: Int): Int {
        return items.indexOfFirst { it is AlarmListItem.AlarmRow && it.alarm.id == key }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeCreated() {
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeDestroyed() {
        notifyDataSetChanged()
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {}

    override fun onRowSelected(myViewHolder: ViewHolder?) {}

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AlarmListItem.GroupRow -> VIEW_TYPE_GROUP
            is AlarmListItem.AlarmRow -> VIEW_TYPE_ALARM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == VIEW_TYPE_GROUP) {
            createViewHolder(ItemAlarmGroupBinding.inflate(layoutInflater, parent, false).root)
        } else {
            createViewHolder(ItemAlarmBinding.inflate(layoutInflater, parent, false).root)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AlarmListItem.GroupRow -> {
                holder.bindView(
                    any = item.group,
                    allowSingleClick = true,
                    allowLongClick = false
                ) { itemView, _ ->
                    setupGroupView(itemView, item)
                }
            }

            is AlarmListItem.AlarmRow -> {
                holder.bindView(
                    any = item.alarm,
                    allowSingleClick = true,
                    allowLongClick = true
                ) { itemView, _ ->
                    setupView(itemView, item.alarm, holder)
                }
            }
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: ArrayList<AlarmListItem>) {
        items = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    private fun addSelectedToGroup() {
        val selectedIds = getSelectedAlarms().map { it.id }
        AddToGroupDialog(activity as SimpleActivity) { groupId ->
            activity.dbHelper.assignAlarmsToGroup(selectedIds, groupId)
            finishActMode()
            EventBus.getDefault().post(AlarmEvent.Refresh)
        }
    }

    private fun deleteItems() {
        val positions = getSelectedItemPositions()
        val alarmsToRemove = getSelectedAlarms()
        items.removeAll { it is AlarmListItem.AlarmRow && alarmsToRemove.contains(it.alarm) }
        removeSelectedItems(positions)
        activity.dbHelper.deleteAlarms(alarmsToRemove)
        EventBus.getDefault().post(AlarmEvent.Refresh)
    }

    private fun getSelectedAlarms(): ArrayList<Alarm> {
        return items.filterIsInstance<AlarmListItem.AlarmRow>()
            .map { it.alarm }
            .filter { selectedKeys.contains(it.id) }
            .toCollection(ArrayList())
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView(view: View, alarm: Alarm, holder: ViewHolder) {
        val isSelected = selectedKeys.contains(alarm.id)
        ItemAlarmBinding.bind(view).apply {
            alarmHolder.isSelected = isSelected
            alarmDragHandle.beVisibleIf(selectedKeys.isNotEmpty())
            alarmDragHandle.applyColorFilter(textColor)
            alarmDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    startReorderDragListener.requestDrag(holder)
                }
                false
            }
            alarmTime.text = activity.getFormattedTime(
                passedSeconds = alarm.timeInMinutes * 60,
                showSeconds = false,
                makeAmPmSmaller = true
            )
            alarmTime.setTextColor(textColor)

            alarmDays.text = getAlarmSelectedDaysString(alarm)
            alarmDays.setTextColor(textColor)

            alarmLabel.text = alarm.label
            alarmLabel.setTextColor(textColor)
            alarmLabel.beVisibleIf(alarm.label.isNotEmpty())

            alarmSwitch.isChecked = alarm.isEnabled
            alarmSwitch.setColors(textColor, properPrimaryColor, backgroundColor)
            alarmSwitch.setOnClickListener {
                toggleAlarm(binding = this, alarm = alarm)
            }
        }
    }

    private fun setupGroupView(view: View, groupRow: AlarmListItem.GroupRow) {
        ItemAlarmGroupBinding.bind(view).apply {
            val group = groupRow.group
            val alarms = groupRow.alarms

            groupIcon.applyColorFilter(textColor)
            groupTitle.text = group.title
            groupTitle.setTextColor(textColor)

            groupSubtitle.text = resources.getQuantityString(
                R.plurals.group_alarm_count, alarms.size, alarms.size
            )
            groupSubtitle.setTextColor(textColor)

            val anyEnabled = alarms.isNotEmpty() && alarms.any { it.isEnabled }
            groupSwitch.setColors(textColor, properPrimaryColor, backgroundColor)
            groupSwitch.isChecked = anyEnabled
            groupSwitch.setOnClickListener {
                toggleGroup(group.ref, groupSwitch.isChecked)
            }
        }
    }

    private fun toggleGroup(groupId: Int, isEnabled: Boolean) {
        (activity as SimpleActivity).handleFullScreenNotificationsPermission { granted ->
            if (granted) {
                val updatedAlarms = activity.dbHelper.updateGroupEnabledState(groupId, isEnabled)
                updatedAlarms.forEach { toggleAlarmInterface.alarmToggled(it.id, isEnabled) }
            }
        }
    }

    private fun toggleAlarm(binding: ItemAlarmBinding, alarm: Alarm) {
        when {
            alarm.isRecurring() -> {
                if (activity.config.wasAlarmWarningShown) {
                    toggleAlarmInterface.alarmToggled(alarm.id, binding.alarmSwitch.isChecked)
                } else {
                    ConfirmationDialog(
                        activity = activity,
                        messageId = org.fossify.commons.R.string.alarm_warning,
                        positive = org.fossify.commons.R.string.ok,
                        negative = 0
                    ) {
                        activity.config.wasAlarmWarningShown = true
                        toggleAlarmInterface.alarmToggled(alarm.id, binding.alarmSwitch.isChecked)
                    }
                }
            }

            else -> {
                updateNonRecurringAlarmDay(alarm)
                activity.dbHelper.updateAlarm(alarm)
                binding.alarmDays.text = getAlarmSelectedDaysString(
                    alarm = alarm, isEnabled = binding.alarmSwitch.isChecked
                )
                toggleAlarmInterface.alarmToggled(alarm.id, binding.alarmSwitch.isChecked)
            }
        }
    }

    private fun getAlarmSelectedDaysString(
        alarm: Alarm, isEnabled: Boolean = alarm.isEnabled,
    ): String {
        if (alarm.isRecurring()) {
            return if (alarm.days == EVERY_DAY_BIT) {
                activity.getString(org.fossify.commons.R.string.every_day)
            } else {
                activity.getSelectedDaysString(alarm.days)
            }
        }

        return when {
            !isEnabled -> resources.getString(R.string.not_scheduled)
            alarm.isToday() -> resources.getString(org.fossify.commons.R.string.today)
            else -> resources.getString(org.fossify.commons.R.string.tomorrow)
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        items.move(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        saveAlarmsCustomOrder()
        if (activity.config.alarmSort != SORT_BY_CUSTOM) {
            activity.config.alarmSort = SORT_BY_CUSTOM
        }
    }

    private fun saveAlarmsCustomOrder() {
        val ids = items.filterIsInstance<AlarmListItem.AlarmRow>().map { it.alarm.id }
        activity.config.alarmsCustomSorting = ids.joinToString { it.toString() }
    }
}
