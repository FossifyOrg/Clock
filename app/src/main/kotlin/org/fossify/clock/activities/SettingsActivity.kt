package org.fossify.clock.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import org.fossify.clock.R
import org.fossify.clock.databinding.ActivitySettingsBinding
import org.fossify.clock.dialogs.ExportDataDialog
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.timerDb
import org.fossify.clock.helpers.DBHelper
import org.fossify.clock.helpers.DEFAULT_MAX_ALARM_REMINDER_SECS
import org.fossify.clock.helpers.DEFAULT_MAX_TIMER_REMINDER_SECS
import org.fossify.clock.helpers.EXPORT_BACKUP_MIME_TYPE
import org.fossify.clock.helpers.ExportHelper
import org.fossify.clock.helpers.IMPORT_BACKUP_MIME_TYPES
import org.fossify.clock.helpers.ImportHelper
import org.fossify.clock.helpers.TAB_ALARM
import org.fossify.clock.helpers.TAB_CLOCK
import org.fossify.clock.helpers.TAB_STOPWATCH
import org.fossify.clock.helpers.TAB_TIMER
import org.fossify.clock.helpers.TimerHelper
import org.fossify.clock.models.AlarmTimerBackup
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.formatMinutesToTimeString
import org.fossify.commons.extensions.formatSecondsToTimeString
import org.fossify.commons.extensions.getCustomizeColorsString
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.isOrWasThankYouInstalled
import org.fossify.commons.extensions.launchPurchaseThankYouIntent
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.showPickSecondsDialog
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ExportResult
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.TAB_LAST_USED
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.models.RadioItem
import java.io.IOException
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val binding: ActivitySettingsBinding by viewBinding(ActivitySettingsBinding::inflate)
    private val exportActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_BACKUP_MIME_TYPE)) { uri ->
            if (uri == null) return@registerForActivityResult
            ensureBackgroundThread {
                try {
                    exportDataTo(uri)
                } catch (e: IOException) {
                    showErrorToast(e)
                }
            }
        }

    private val importActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            ensureBackgroundThread {
                try {
                    importData(uri)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateMaterialActivityViews(
            mainCoordinatorLayout = binding.settingsCoordinator,
            nestedView = binding.settingsHolder,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupPurchaseThankYou()
        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupHourFormat()
        setupDefaultTab()
        setupPreventPhoneFromSleeping()
        setupStartWeekOn()
        setupAlarmMaxReminder()
        setupUseSameSnooze()
        setupSnoozeTime()
        setupTimerMaxReminder()
        setupIncreaseVolumeGradually()
        setupCustomizeWidgetColors()
        setupExportData()
        setupImportData()
        updateTextColors(binding.settingsHolder)

        arrayOf(
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsAlarmTabLabel,
            binding.settingsTimerTabLabel,
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupPurchaseThankYou() {
        binding.settingsPurchaseThankYouHolder.beGoneIf(isOrWasThankYouInstalled())
        binding.settingsPurchaseThankYouHolder.setOnClickListener {
            launchPurchaseThankYouIntent()
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationLabel.text = getCustomizeColorsString()
        binding.settingsColorCustomizationHolder.setOnClickListener {
            handleCustomizeColorsClick()
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        if (isTiramisuPlus()) {
            binding.settingsLanguageHolder.beVisible()
            binding.settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        } else {
            binding.settingsLanguageHolder.beGone()
        }
    }

    private fun setupDefaultTab() {
        binding.settingsDefaultTab.text = getDefaultTabText()
        binding.settingsDefaultTabHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(TAB_CLOCK, getString(R.string.clock)),
                RadioItem(TAB_ALARM, getString(org.fossify.commons.R.string.alarm)),
                RadioItem(TAB_STOPWATCH, getString(R.string.stopwatch)),
                RadioItem(TAB_TIMER, getString(R.string.timer)),
                RadioItem(TAB_LAST_USED, getString(org.fossify.commons.R.string.last_used_tab))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.defaultTab) {
                config.defaultTab = it as Int
                binding.settingsDefaultTab.text = getDefaultTabText()
            }
        }
    }

    private fun setupHourFormat() = binding.apply {
        settingsHourFormat.isChecked = config.use24HourFormat
        settingsHourFormatHolder.setOnClickListener {
            settingsHourFormat.toggle()
            config.use24HourFormat = settingsHourFormat.isChecked
            updateWidgets()
        }
    }

    private fun getDefaultTabText() = getString(
        when (config.defaultTab) {
            TAB_CLOCK -> R.string.clock
            TAB_ALARM -> org.fossify.commons.R.string.alarm
            TAB_STOPWATCH -> R.string.stopwatch
            TAB_TIMER -> R.string.timer
            else -> org.fossify.commons.R.string.last_used_tab
        }
    )

    private fun setupPreventPhoneFromSleeping() {
        binding.settingsPreventPhoneFromSleeping.isChecked = config.preventPhoneFromSleeping
        binding.settingsPreventPhoneFromSleepingHolder.setOnClickListener {
            binding.settingsPreventPhoneFromSleeping.toggle()
            config.preventPhoneFromSleeping = binding.settingsPreventPhoneFromSleeping.isChecked
        }
    }

    private fun setupStartWeekOn() {
        val items = arrayListOf(
            RadioItem(6, getString(org.fossify.commons.R.string.sunday)),
            RadioItem(0, getString(org.fossify.commons.R.string.monday)),
            RadioItem(1, getString(org.fossify.commons.R.string.tuesday)),
            RadioItem(2, getString(org.fossify.commons.R.string.wednesday)),
            RadioItem(3, getString(org.fossify.commons.R.string.thursday)),
            RadioItem(4, getString(org.fossify.commons.R.string.friday)),
            RadioItem(5, getString(org.fossify.commons.R.string.saturday)),
        )

        binding.settingsStartWeekOn.text = resources.getStringArray(org.fossify.commons.R.array.week_days)[config.firstDayOfWeek]
        binding.settingsStartWeekOnHolder.setOnClickListener {
            RadioGroupDialog(this@SettingsActivity, items, config.firstDayOfWeek) { day ->
                val firstDayOfWeek = day as Int
                config.firstDayOfWeek = firstDayOfWeek
                binding.settingsStartWeekOn.text = resources.getStringArray(org.fossify.commons.R.array.week_days)[config.firstDayOfWeek]
            }
        }
    }

    private fun setupAlarmMaxReminder() {
        updateAlarmMaxReminderText()
        binding.settingsAlarmMaxReminderHolder.setOnClickListener {
            showPickSecondsDialog(config.alarmMaxReminderSecs, true, true) {
                config.alarmMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_ALARM_REMINDER_SECS
                updateAlarmMaxReminderText()
            }
        }
    }

    private fun setupUseSameSnooze() {
        binding.settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        binding.settingsUseSameSnooze.isChecked = config.useSameSnooze
        binding.settingsUseSameSnoozeHolder.setOnClickListener {
            binding.settingsUseSameSnooze.toggle()
            config.useSameSnooze = binding.settingsUseSameSnooze.isChecked
            binding.settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeText()
        binding.settingsSnoozeTimeHolder.setOnClickListener {
            showPickSecondsDialog(config.snoozeTime * MINUTE_SECONDS, true) {
                config.snoozeTime = it / MINUTE_SECONDS
                updateSnoozeText()
            }
        }
    }

    private fun setupTimerMaxReminder() {
        updateTimerMaxReminderText()
        binding.settingsTimerMaxReminderHolder.setOnClickListener {
            showPickSecondsDialog(config.timerMaxReminderSecs, true, true) {
                config.timerMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_TIMER_REMINDER_SECS
                updateTimerMaxReminderText()
            }
        }
    }

    private fun setupIncreaseVolumeGradually() {
        binding.settingsIncreaseVolumeGradually.isChecked = config.increaseVolumeGradually
        binding.settingsIncreaseVolumeGraduallyHolder.setOnClickListener {
            binding.settingsIncreaseVolumeGradually.toggle()
            config.increaseVolumeGradually = binding.settingsIncreaseVolumeGradually.isChecked
        }
    }

    private fun updateSnoozeText() {
        binding.settingsSnoozeTime.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun updateAlarmMaxReminderText() {
        binding.settingsAlarmMaxReminder.text =
            formatSecondsToTimeString(config.alarmMaxReminderSecs)
    }

    private fun updateTimerMaxReminderText() {
        binding.settingsTimerMaxReminder.text =
            formatSecondsToTimeString(config.timerMaxReminderSecs)
    }

    private fun setupCustomizeWidgetColors() {
        binding.settingsWidgetColorCustomizationHolder.setOnClickListener {
            Intent(this, WidgetDigitalConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun setupExportData() {
        binding.settingsExportDataHolder.setOnClickListener {
            tryExportData()
        }
    }

    private fun setupImportData() {
        binding.settingsImportDataHolder.setOnClickListener {
            tryImportData()
        }
    }

    private fun exportDataTo(outputUri: Uri) {
        val alarms = dbHelper.getAlarms()
        val timers = timerDb.getTimers()
        if (alarms.isEmpty() && timers.isEmpty()) {
            toast(org.fossify.commons.R.string.no_entries_for_exporting)
        } else {
            ExportHelper(this).exportData(
                backup = AlarmTimerBackup(alarms, timers),
                outputUri = outputUri,
            ) {
                toast(
                    when (it) {
                        ExportResult.EXPORT_OK -> org.fossify.commons.R.string.exporting_successful
                        else -> org.fossify.commons.R.string.exporting_failed
                    }
                )
            }
        }
    }

    private fun tryExportData() {
        ExportDataDialog(this, config.lastDataExportPath) { file ->
            try {
                exportActivityResultLauncher.launch(file.name)
            } catch (e: ActivityNotFoundException) {
                toast(
                    id = org.fossify.commons.R.string.system_service_disabled,
                    length = Toast.LENGTH_LONG
                )
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun tryImportData() {
        try {
            importActivityResultLauncher.launch(IMPORT_BACKUP_MIME_TYPES.toTypedArray())
        } catch (e: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun importData(uri: Uri) {
        val result = ImportHelper(
            context = this,
            dbHelper = DBHelper.dbInstance!!,
            timerHelper = TimerHelper(this)
        ).importData(uri)

        toast(
            when (result) {
                ImportHelper.ImportResult.IMPORT_OK -> org.fossify.commons.R.string.importing_successful
                ImportHelper.ImportResult.IMPORT_INCOMPLETE -> org.fossify.commons.R.string.no_new_entries_for_importing
                ImportHelper.ImportResult.IMPORT_FAIL -> org.fossify.commons.R.string.no_items_found
            }
        )
    }
}
