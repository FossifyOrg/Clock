package org.fossify.clock.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.fossify.clock.activities.GroupActivity
import org.fossify.clock.activities.MainActivity
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.adapters.AlarmsAdapter
import org.fossify.clock.databinding.FragmentAlarmBinding
import org.fossify.clock.dialogs.ChangeAlarmSortDialog
import org.fossify.clock.dialogs.EditAlarmDialog
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.createNewAlarm
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.firstDayOrder
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.DEFAULT_ALARM_MINUTES
import org.fossify.clock.helpers.SORT_BY_ALARM_TIME
import org.fossify.clock.helpers.SORT_BY_DATE_AND_TIME
import org.fossify.clock.helpers.getTomorrowBit
import org.fossify.clock.interfaces.ToggleAlarmInterface
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.clock.models.AlarmListItem
import org.fossify.clock.models.Group
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.helpers.SORT_BY_DATE_CREATED
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AlarmFragment : Fragment(), ToggleAlarmInterface {
    private var alarmItems = ArrayList<AlarmListItem>()
    private var currentEditAlarmDialog: EditAlarmDialog? = null

    private lateinit var binding: FragmentAlarmBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        setupViews()
    }

    fun showSortingDialog() {
        ChangeAlarmSortDialog(activity as SimpleActivity) {
            setupAlarms()
        }
    }

    private fun setupViews() {
        binding.apply {
            requireContext().updateTextColors(alarmFragment)
            alarmFab.setOnClickListener {
                val newAlarm = root.context.createNewAlarm(DEFAULT_ALARM_MINUTES, 0)
                newAlarm.isEnabled = true
                newAlarm.days = getTomorrowBit()
                openEditAlarm(newAlarm)
            }
        }

        setupAlarms()
    }

    private fun getSortedItems(callback: (items: ArrayList<AlarmListItem>) -> Unit) {
        val safeContext = context ?: return
        ensureBackgroundThread {
            val allAlarms = context?.dbHelper?.getAlarms()
            if (allAlarms == null) {
                activity?.runOnUiThread { callback(arrayListOf()) }
                return@ensureBackgroundThread
            }

            val groups = safeContext.dbHelper.getGroups()
            val groupedAlarms = allAlarms.filter { it.groupId != 0 }.groupBy { it.groupId }
            var ungroupedAlarms = allAlarms.filter { it.groupId == 0 } as ArrayList<Alarm>

            when (safeContext.config.alarmSort) {
                SORT_BY_ALARM_TIME -> ungroupedAlarms.sortBy { it.timeInMinutes }
                SORT_BY_DATE_CREATED -> ungroupedAlarms.sortBy { it.id }
                SORT_BY_DATE_AND_TIME -> ungroupedAlarms.sortWith(compareBy<Alarm> {
                    safeContext.firstDayOrder(it.days)
                }.thenBy {
                    it.timeInMinutes
                })

                SORT_BY_CUSTOM -> {
                    val customAlarmsSortOrderString = activity?.config?.alarmsCustomSorting
                    if (customAlarmsSortOrderString.isNullOrEmpty()) {
                        ungroupedAlarms.sortBy { it.id }
                    } else {
                        val customAlarmsSortOrder: List<Int> =
                            customAlarmsSortOrderString.split(", ").map { it.toInt() }
                        val alarmsIdValueMap = ungroupedAlarms.associateBy { it.id }

                        val sortedAlarms: ArrayList<Alarm> = ArrayList()
                        customAlarmsSortOrder.map { id ->
                            if (alarmsIdValueMap[id] != null) {
                                sortedAlarms.add(alarmsIdValueMap[id] as Alarm)
                            }
                        }

                        ungroupedAlarms =
                            (sortedAlarms + ungroupedAlarms.filter { it !in sortedAlarms }) as ArrayList<Alarm>
                    }
                }
            }

            val groupItems = groups
                .sortedBy { it.title.lowercase() }
                .map { group ->
                    AlarmListItem.GroupRow(
                        group = group,
                        alarms = groupedAlarms[group.id].orEmpty().sortedBy { it.timeInMinutes }
                    )
                }

            val combined = ArrayList<AlarmListItem>()
            combined.addAll(groupItems)
            combined.addAll(ungroupedAlarms.map { AlarmListItem.AlarmRow(it) })

            activity?.runOnUiThread {
                callback(combined)
            }
        }
    }

    private fun setupAlarms() {
        getSortedItems { items ->
            alarmItems = items
            val safeActivity = activity as? SimpleActivity ?: return@getSortedItems
            var currAdapter = binding.alarmsList.adapter as? AlarmsAdapter
            if (currAdapter == null) {
                currAdapter = AlarmsAdapter(
                    activity = safeActivity,
                    items = items,
                    toggleAlarmInterface = this,
                    recyclerView = binding.alarmsList
                ) {
                    when (it) {
                        is Alarm -> openEditAlarm(it)
                        is Group -> GroupActivity.start(safeActivity, it.id)
                    }
                }.apply {
                    binding.alarmsList.adapter = this
                }
            } else {
                currAdapter.apply {
                    updatePrimaryColor()
                    updateBackgroundColor(safeActivity.getProperBackgroundColor())
                    updateTextColor(safeActivity.getProperTextColor())
                    updateItems(items)
                }
            }
            binding.alarmsPlaceholder.beVisibleIf(items.isEmpty())
        }
    }

    private fun openEditAlarm(alarm: Alarm) {
        currentEditAlarmDialog = EditAlarmDialog(activity as SimpleActivity, alarm) {
            alarm.id = it
            currentEditAlarmDialog = null
            setupAlarms()
            checkAlarmState(alarm)
        }
    }

    override fun alarmToggled(id: Int, isEnabled: Boolean) {
        (activity as SimpleActivity).handleFullScreenNotificationsPermission { granted ->
            if (granted) {
                if (requireContext().dbHelper.updateAlarmEnabledState(id, isEnabled)) {
                    val alarm = findAlarmById(id) ?: return@handleFullScreenNotificationsPermission
                    alarm.isEnabled = isEnabled
                    checkAlarmState(alarm)
                    if (!alarm.isEnabled && alarm.oneShot) {
                        requireContext().dbHelper.deleteAlarms(arrayListOf(alarm))
                        setupAlarms()
                    }
                } else {
                    requireActivity().toast(org.fossify.commons.R.string.unknown_error_occurred)
                }
                requireContext().updateWidgets()
            } else {
                setupAlarms()
            }
        }
    }

    private fun findAlarmById(id: Int): Alarm? {
        alarmItems.forEach { item ->
            when (item) {
                is AlarmListItem.AlarmRow -> if (item.alarm.id == id) return item.alarm
                is AlarmListItem.GroupRow -> item.alarms.firstOrNull { it.id == id }?.let { return it }
            }
        }
        return null
    }

    private fun checkAlarmState(alarm: Alarm) {
        val activity = activity as? MainActivity ?: return
        if (alarm.isEnabled) {
            activity.alarmController.scheduleNextOccurrence(alarm = alarm, showToasts = true)
        } else {
            activity.cancelAlarmClock(alarm)
        }
        activity.updateClockTabAlarm()
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        currentEditAlarmDialog?.updateSelectedAlarmSound(alarmSound)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("unused") event: AlarmEvent.Refresh) {
        setupAlarms()
    }
}
