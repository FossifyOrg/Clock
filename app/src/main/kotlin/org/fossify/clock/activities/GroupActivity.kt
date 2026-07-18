package org.fossify.clock.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.fossify.clock.R
import org.fossify.clock.adapters.AlarmsAdapter
import org.fossify.clock.databinding.ActivityGroupBinding
import org.fossify.clock.dialogs.EditAlarmDialog
import org.fossify.clock.dialogs.RenameGroupDialog
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.createNewAlarm
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.DEFAULT_ALARM_MINUTES
import org.fossify.clock.helpers.PICK_AUDIO_FILE_INTENT_ID
import org.fossify.clock.helpers.getTomorrowBit
import org.fossify.clock.interfaces.ToggleAlarmInterface
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.clock.models.AlarmListItem
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.storeNewYourAlarmSound
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class GroupActivity : SimpleActivity(), ToggleAlarmInterface {
    companion object {
        private const val GROUP_REF = "group_ref"

        fun start(context: Context, groupRef: Int) {
            context.startActivity(
                Intent(context, GroupActivity::class.java).putExtra(GROUP_REF, groupRef)
            )
        }
    }

    private val binding by viewBinding(ActivityGroupBinding::inflate)
    private var groupRef = -1
    private var currentEditAlarmDialog: EditAlarmDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        groupRef = intent.getIntExtra(GROUP_REF, -1)
        if (groupRef == -1) {
            finish()
            return
        }

        setupEdgeToEdge(padBottomSystem = listOf(binding.groupList))
        EventBus.getDefault().register(this)

        binding.groupToolbar.inflateMenu(R.menu.menu_group)
        binding.groupToolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.delete_group) {
                ConfirmationDialog(this, messageId = R.string.delete_group_confirmation) {
                    dbHelper.deleteGroup(groupRef)
                    EventBus.getDefault().post(AlarmEvent.Refresh)
                    finish()
                }
                true
            } else {
                false
            }
        }

        binding.groupFab.setOnClickListener {
            val newAlarm = createNewAlarm(DEFAULT_ALARM_MINUTES, 0)
            newAlarm.isEnabled = true
            newAlarm.days = getTomorrowBit()
            newAlarm.groupRef = groupRef
            openEditAlarm(newAlarm)
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.groupAppbar, NavigationIcon.Arrow, topBarColor = getProperBackgroundColor())
        updateTextColors(binding.groupHolder)
        if (!setupGroupTitle()) {
            return
        }
        refreshAlarms()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_AUDIO_FILE_INTENT_ID && resultCode == RESULT_OK && resultData != null) {
            currentEditAlarmDialog?.updateSelectedAlarmSound(storeNewYourAlarmSound(resultData))
        }
    }

    private fun setupGroupTitle(): Boolean {
        val group = dbHelper.getGroups().firstOrNull { it.ref == groupRef }
        if (group == null) {
            finish()
            return false
        }

        binding.groupToolbarTitle.text = group.title
        binding.groupToolbarTitle.setTextColor(getProperTextColor())
        binding.groupToolbarTitle.setOnClickListener {
            RenameGroupDialog(this, group) {
                setupGroupTitle()
            }
        }
        return true
    }

    private fun refreshAlarms() {
        ensureBackgroundThread {
            val alarms = dbHelper.getAlarms()
                .filter { it.groupRef == groupRef }
                .sortedBy { it.timeInMinutes }

            runOnUiThread {
                val items = ArrayList<AlarmListItem>(alarms.map { AlarmListItem.AlarmRow(it) })
                var currAdapter = binding.groupList.adapter as? AlarmsAdapter
                if (currAdapter == null) {
                    currAdapter = AlarmsAdapter(
                        activity = this,
                        items = items,
                        toggleAlarmInterface = this,
                        recyclerView = binding.groupList
                    ) {
                        if (it is Alarm) {
                            openEditAlarm(it)
                        }
                    }.apply {
                        binding.groupList.adapter = this
                    }
                } else {
                    currAdapter.apply {
                        updatePrimaryColor()
                        updateBackgroundColor(getProperBackgroundColor())
                        updateTextColor(getProperTextColor())
                        updateItems(items)
                    }
                }
                binding.groupPlaceholder.beVisibleIf(items.isEmpty())
            }
        }
    }

    private fun openEditAlarm(alarm: Alarm) {
        currentEditAlarmDialog = EditAlarmDialog(this, alarm) {
            alarm.id = it
            currentEditAlarmDialog = null
            refreshAlarms()
            checkAlarmState(alarm)
        }
    }

    private fun checkAlarmState(alarm: Alarm) {
        if (alarm.isEnabled) {
            alarmController.scheduleNextOccurrence(alarm = alarm, showToasts = true)
        } else {
            cancelAlarmClock(alarm)
        }
    }

    override fun alarmToggled(id: Int, isEnabled: Boolean) {
        handleFullScreenNotificationsPermission { granted ->
            if (granted) {
                if (dbHelper.updateAlarmEnabledState(id, isEnabled)) {
                    dbHelper.getAlarmWithId(id)?.let { alarm ->
                        alarm.isEnabled = isEnabled
                        checkAlarmState(alarm)
                        if (!alarm.isEnabled && alarm.oneShot) {
                            dbHelper.deleteAlarms(arrayListOf(alarm))
                        }
                    }
                    refreshAlarms()
                } else {
                    toast(org.fossify.commons.R.string.unknown_error_occurred)
                }
                updateWidgets()
            } else {
                refreshAlarms()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("unused") event: AlarmEvent.Refresh) {
        refreshAlarms()
    }
}
