package org.fossify.clock.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AnimationUtils
import org.fossify.clock.R
import org.fossify.clock.databinding.ActivityReminderBinding
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.disableExpiredAlarm
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.extensions.scheduleNextAlarm
import org.fossify.clock.extensions.setupAlarmClock
import org.fossify.clock.extensions.stopAlarmService
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.getPassedSeconds
import org.fossify.clock.models.Alarm
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.commons.extensions.performHapticFeedback
import org.fossify.commons.extensions.showPickSecondsDialog
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.isOreoMr1Plus
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class ReminderActivity : SimpleActivity() {
    private val swipeGuideFadeHandler = Handler(Looper.getMainLooper())
    private var alarm: Alarm? = null
    private var didVibrate = false
    private var dragDownX = 0f
    private var wasAlarmSnoozed = false

    private val binding: ActivityReminderBinding by viewBinding(ActivityReminderBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        showOverLockscreen()
        updateTextColors(binding.root)
        updateStatusbarColor(getProperBackgroundColor())

        val id = intent.getIntExtra(ALARM_ID, -1)
        val isAlarmReminder = id != -1
        if (isAlarmReminder) {
            alarm = dbHelper.getAlarmWithId(id)
            if (alarm == null) {
                finish()
                return
            }
        }

        val label = if (isAlarmReminder) {
            if (alarm!!.label.isEmpty()) {
                getString(org.fossify.commons.R.string.alarm)
            } else {
                alarm!!.label
            }
        } else {
            getString(R.string.timer)
        }

        binding.reminderTitle.text = label
        binding.reminderText.text = if (isAlarmReminder) {
            getFormattedTime(
                passedSeconds = getPassedSeconds(),
                showSeconds = false,
                makeAmPmSmaller = false
            )
        } else {
            getString(R.string.time_expired)
        }

        setupButtons(isAlarmReminder)
    }

    private fun setupButtons(isAlarmReminder: Boolean) {
        if (isAlarmReminder) {
            setupAlarmButtons()
        } else {
            setupTimerButtons()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAlarmButtons() {
        binding.reminderStop.beGone()
        binding.reminderDraggableBackground.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.pulsing_animation)
        )
        binding.reminderDraggableBackground.applyColorFilter(getProperPrimaryColor())

        val textColor = getProperTextColor()
        binding.reminderDismiss.applyColorFilter(textColor)
        binding.reminderDraggable.applyColorFilter(textColor)
        binding.reminderSnooze.applyColorFilter(textColor)

        var minDragX = 0f
        var maxDragX = 0f
        var initialDraggableX = 0f

        binding.reminderDismiss.onGlobalLayout {
            minDragX = binding.reminderSnooze.left.toFloat()
            maxDragX = binding.reminderDismiss.left.toFloat()
            initialDraggableX = binding.reminderDraggable.left.toFloat()
        }

        binding.reminderDraggable.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragDownX = event.x
                    binding.reminderDraggableBackground.animate().alpha(0f)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragDownX = 0f
                    if (!didVibrate) {
                        binding.reminderDraggable.animate().x(initialDraggableX).withEndAction {
                            binding.reminderDraggableBackground.animate().alpha(0.2f)
                        }

                        binding.reminderGuide.animate().alpha(1f).start()
                        swipeGuideFadeHandler.removeCallbacksAndMessages(null)
                        swipeGuideFadeHandler.postDelayed({
                            binding.reminderGuide.animate().alpha(0f).start()
                        }, 2000L)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    binding.reminderDraggable.x = min(
                        a = maxDragX,
                        b = max(minDragX, event.rawX - dragDownX)
                    )

                    if (binding.reminderDraggable.x >= maxDragX - 50f) {
                        if (!didVibrate) {
                            binding.reminderDraggable.performHapticFeedback()
                            didVibrate = true
                            finishActivity()
                        }
                    } else if (binding.reminderDraggable.x <= minDragX + 50f) {
                        if (!didVibrate) {
                            binding.reminderDraggable.performHapticFeedback()
                            didVibrate = true
                            snoozeAlarm()
                        }
                    }
                }
            }
            true
        }
    }

    private fun setupTimerButtons() {
        binding.reminderStop.background = resources.getColoredDrawableWithColor(
            drawableId = R.drawable.circle_background_filled,
            color = getProperPrimaryColor()
        )
        arrayOf(
            binding.reminderSnooze,
            binding.reminderDraggableBackground,
            binding.reminderDraggable,
            binding.reminderDismiss
        ).forEach {
            it.beGone()
        }

        binding.reminderStop.setOnClickListener {
            finishActivity()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupAlarmButtons()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == AlarmClock.ACTION_SNOOZE_ALARM) {
            val durationMinutes = intent.getIntExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, -1)
            if (durationMinutes == -1) {
                snoozeAlarm()
            } else {
                snoozeAlarm(durationMinutes)
            }
        } else {
            finishActivity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        swipeGuideFadeHandler.removeCallbacksAndMessages(null)
    }

    private fun snoozeAlarm(overrideSnoozeDuration: Int? = null) {
        stopAlarmService()
        if (overrideSnoozeDuration != null) {
            scheduleSnoozedAlarm(overrideSnoozeDuration)
        } else if (config.useSameSnooze) {
            scheduleSnoozedAlarm(config.snoozeTime)
        } else {
            showPickSecondsDialog(
                curSeconds = config.snoozeTime * MINUTE_SECONDS,
                isSnoozePicker = true,
                cancelCallback = {
                    finishActivity()
                },
                callback = {
                    config.snoozeTime = it / MINUTE_SECONDS
                    scheduleSnoozedAlarm(config.snoozeTime)
                }
            )
        }
    }

    private fun scheduleSnoozedAlarm(snoozeMinutes: Int) {
        if (alarm != null) {
            setupAlarmClock(
                alarm = alarm!!,
                triggerTimeMillis = Calendar.getInstance()
                    .apply { add(Calendar.MINUTE, snoozeMinutes) }
                    .timeInMillis
            )

            wasAlarmSnoozed = true
        }

        finishActivity()
    }

    private fun finishActivity() {
        stopAlarmService()
        if (!wasAlarmSnoozed && alarm != null) {
            cancelAlarmClock(alarm!!)
            if (alarm!!.days > 0) {
                scheduleNextAlarm(alarm!!, false)
            }

            disableExpiredAlarm(alarm!!)
        }

        finish()
        overridePendingTransition(0, 0)
    }

    private fun showOverLockscreen() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (isOreoMr1Plus()) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }
}
