package org.fossify.clock.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.AlarmClock
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.core.net.toUri
import org.fossify.clock.R
import org.fossify.clock.databinding.ActivityReminderBinding
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.extensions.scheduleNextAlarm
import org.fossify.clock.extensions.setupAlarmClock
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.ALARM_NOTIF_ID
import org.fossify.clock.helpers.getPassedSeconds
import org.fossify.clock.models.Alarm
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.commons.extensions.performHapticFeedback
import org.fossify.commons.extensions.showPickSecondsDialog
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.SILENT
import org.fossify.commons.helpers.isOreoMr1Plus
import org.fossify.commons.helpers.isOreoPlus
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class ReminderActivity : SimpleActivity() {
    companion object {
        private const val MIN_ALARM_VOLUME_FOR_INCREASING_ALARMS = 1
        private const val INCREASE_VOLUME_DELAY = 300L
    }

    private val increaseVolumeHandler = Handler(Looper.getMainLooper())
    private val maxReminderDurationHandler = Handler(Looper.getMainLooper())
    private val swipeGuideFadeHandler = Handler()
    private val vibrationHandler = Handler(Looper.getMainLooper())
    private var isAlarmReminder = false
    private var didVibrate = false
    private var wasAlarmSnoozed = false
    private var alarm: Alarm? = null
    private var audioManager: AudioManager? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var initialAlarmVolume: Int? = null
    private var dragDownX = 0f
    private val binding: ActivityReminderBinding by viewBinding(ActivityReminderBinding::inflate)
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        showOverLockscreen()
        updateTextColors(binding.root)
        updateStatusbarColor(getProperBackgroundColor())

        val id = intent.getIntExtra(ALARM_ID, -1)
        isAlarmReminder = id != -1
        if (id != -1) {
            alarm = dbHelper.getAlarmWithId(id) ?: return
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

        val maxDuration = if (isAlarmReminder) {
            config.alarmMaxReminderSecs
        } else {
            config.timerMaxReminderSecs
        }

        maxReminderDurationHandler.postDelayed({
            finishActivity()
            cancelNotification()
        }, maxDuration * 1000L)

        setupButtons()
        setupEffects()
    }

    private fun setupButtons() {
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
            AnimationUtils.loadAnimation(
                this,
                R.anim.pulsing_animation
            )
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

        binding.reminderDraggable.setOnTouchListener { v, event ->
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

                        cancelNotification()
                    } else if (binding.reminderDraggable.x <= minDragX + 50f) {
                        if (!didVibrate) {
                            binding.reminderDraggable.performHapticFeedback()
                            didVibrate = true
                            snoozeAlarm()
                        }

                        cancelNotification()
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

    private fun setupEffects() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        initialAlarmVolume = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 7

        val doVibrate = alarm?.vibrate ?: config.timerVibrate
        if (doVibrate && isOreoPlus()) {
            val pattern = LongArray(2) { 500 }
            vibrationHandler.postDelayed({
                vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }, 500)
        }

        val soundUri = if (alarm != null) {
            alarm!!.soundUri
        } else {
            config.timerSoundUri
        }

        if (soundUri != SILENT) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setDataSource(this@ReminderActivity, soundUri.toUri())
                    isLooping = true
                    prepare()
                    start()
                }

                if (config.increaseVolumeGradually) {
                    scheduleVolumeIncrease(
                        lastVolume = MIN_ALARM_VOLUME_FOR_INCREASING_ALARMS.toFloat(),
                        maxVolume = initialAlarmVolume!!.toFloat(),
                        delay = 0
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun scheduleVolumeIncrease(lastVolume: Float, maxVolume: Float, delay: Long) {
        increaseVolumeHandler.postDelayed({
            val newLastVolume = (lastVolume + 0.1f).coerceAtMost(maxVolume)
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, newLastVolume.toInt(), 0)
            scheduleVolumeIncrease(newLastVolume, maxVolume, INCREASE_VOLUME_DELAY)
        }, delay)
    }

    private fun resetVolumeToInitialValue() {
        initialAlarmVolume?.apply {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, this, 0)
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
        increaseVolumeHandler.removeCallbacksAndMessages(null)
        maxReminderDurationHandler.removeCallbacksAndMessages(null)
        swipeGuideFadeHandler.removeCallbacksAndMessages(null)
        vibrationHandler.removeCallbacksAndMessages(null)
        if (!finished) {
            finishActivity()
            cancelNotification()
        } else {
            destroyEffects()
        }
    }

    private fun destroyEffects() {
        if (config.increaseVolumeGradually) {
            resetVolumeToInitialValue()
        }

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun snoozeAlarm(overrideSnoozeDuration: Int? = null) {
        destroyEffects()
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
        setupAlarmClock(
            alarm = alarm!!,
            triggerTimeMillis = Calendar.getInstance()
                .apply { add(Calendar.MINUTE, snoozeMinutes) }
                .timeInMillis
        )

        wasAlarmSnoozed = true
        finishActivity()
    }

    private fun finishActivity() {
        if (!wasAlarmSnoozed && alarm != null) {
            cancelAlarmClock(alarm!!)
            if (alarm!!.days > 0) {
                scheduleNextAlarm(alarm!!, false)
            }
            if (alarm!!.days < 0) {
                if (alarm!!.oneShot) {
                    alarm!!.isEnabled = false
                    dbHelper.deleteAlarms(arrayListOf(alarm!!))
                } else {
                    dbHelper.updateAlarmEnabledState(alarm!!.id, false)
                }
                updateWidgets()
            }
        }

        finished = true
        destroyEffects()
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

    private fun cancelNotification() {
        notificationManager.cancel(ALARM_NOTIF_ID)
    }
}
