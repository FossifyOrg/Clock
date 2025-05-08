package org.fossify.clock.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.STREAM_ALARM
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.net.toUri
import org.fossify.clock.activities.AlarmActivity
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.ALARM_NOTIFICATION_ID
import org.fossify.clock.helpers.AlarmNotificationHelper
import org.fossify.clock.models.Alarm
import org.fossify.commons.helpers.SILENT
import kotlin.time.Duration.Companion.seconds

/**
 * Service responsible for sounding the alarms and vibrations.
 * It also shows a notification with actions to dismiss or snooze an alarm.
 * Totally based on the previous implementation in the [AlarmActivity].
 */
class AlarmService : Service() {

    companion object {
        private const val DEFAULT_ALARM_VOLUME = 7
        private const val INCREASE_VOLUME_DELAY = 300L
        private const val MIN_ALARM_VOLUME_FOR_INCREASING_ALARMS = 1
        private const val VIBRATION_PATTERN_TIMING = 500L
        private const val VOLUME_INCREASE_STEP = 0.1f

        const val ACTION_START_ALARM = "org.fossify.clock.START_ALARM"
        const val ACTION_STOP_ALARM = "org.fossify.clock.STOP_ALARM"
    }

    private var activeAlarm: Alarm? = null
    private var initialAlarmVolume = DEFAULT_ALARM_VOLUME
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private lateinit var audioManager: AudioManager
    private lateinit var notificationHelper: AlarmNotificationHelper

    private val autoDismissHandler = Handler(Looper.getMainLooper())
    private val increaseVolumeHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        notificationHelper = AlarmNotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        val alarmId = intent?.getIntExtra(ALARM_ID, -1) ?: -1
        val newAlarm = applicationContext.dbHelper.getAlarmWithId(alarmId)
        if (alarmId == -1 || newAlarm == null) {
            stopSelfIfIdle()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_START_ALARM -> startNewAlarm(newAlarm)
            ACTION_STOP_ALARM -> stopActiveAlarm(alarmId)
            else -> throw IllegalArgumentException("Unknown action: $action")
        }

        return START_STICKY
    }

    private fun startNewAlarm(newAlarm: Alarm) {
        startForeground(
            ALARM_NOTIFICATION_ID,
            notificationHelper.buildActiveAlarmNotification(newAlarm)
        )

        val currentAlarm = activeAlarm
        activeAlarm = newAlarm

        if (currentAlarm?.id == newAlarm.id) {
            // No action needed, same alarm
            return
        }

        val replaceActiveAlarm = currentAlarm != null
        if (replaceActiveAlarm) {
            stopPlayerAndCleanup()
            notificationHelper.postReplacedAlarmNotification(currentAlarm!!)
            alarmController.stopAlarm(currentAlarm.id)
        }

        startAlarmEffects(newAlarm)
        startAutoDismiss(config.alarmMaxReminderSecs)
    }

    private fun stopActiveAlarm(alarmIdToStop: Int) {
        if (activeAlarm?.id == alarmIdToStop) {
            stopSelf()
        } else {
            stopSelfIfIdle()
        }
    }

    private fun startAlarmEffects(alarm: Alarm) {
        if (alarm.soundUri != SILENT) {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@AlarmService, alarm.soundUri.toUri())
                    isLooping = true
                    prepare()
                    start()
                }

                if (config.increaseVolumeGradually) {
                    initialAlarmVolume = audioManager.getStreamVolume(STREAM_ALARM)
                    scheduleVolumeIncrease(
                        lastVolume = MIN_ALARM_VOLUME_FOR_INCREASING_ALARMS.toFloat(),
                        maxVolume = initialAlarmVolume.toFloat(),
                        delay = 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }

        if (alarm.vibrate) {
            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(VIBRATION_PATTERN_TIMING, VIBRATION_PATTERN_TIMING), 0
                )
            )
        }
    }

    // Revisit this. We are directly changing the system alarm volume here.
    private fun scheduleVolumeIncrease(lastVolume: Float, maxVolume: Float, delay: Long) {
        increaseVolumeHandler.postDelayed({
            val newVolume = (lastVolume + VOLUME_INCREASE_STEP).coerceAtMost(maxVolume)
            audioManager.setStreamVolume(STREAM_ALARM, newVolume.toInt(), 0)
            if (newVolume < maxVolume) {
                scheduleVolumeIncrease(newVolume, maxVolume, INCREASE_VOLUME_DELAY)
            }
        }, delay)
    }

    private fun resetVolumeToInitialValue() {
        if (config.increaseVolumeGradually && initialAlarmVolume != DEFAULT_ALARM_VOLUME) {
            audioManager.setStreamVolume(STREAM_ALARM, initialAlarmVolume, 0)
        }

        initialAlarmVolume = DEFAULT_ALARM_VOLUME
    }

    private fun startAutoDismiss(durationSecs: Int) {
        val alarmId = activeAlarm?.id ?: return
        autoDismissHandler.postDelayed({
            val missedAlarm = activeAlarm
            if (missedAlarm?.id == alarmId) {
                notificationHelper.postMissedAlarmNotification(missedAlarm)
                alarmController.stopAlarm(alarmId)
            }
        }, durationSecs.seconds.inWholeMilliseconds)
    }

    private fun stopPlayerAndCleanup() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null

        // Clear any scheduled volume changes or auto-dismiss messages
        increaseVolumeHandler.removeCallbacksAndMessages(null)
        autoDismissHandler.removeCallbacksAndMessages(null)
        resetVolumeToInitialValue()
    }

    private fun stopSelfIfIdle() {
        if (activeAlarm == null) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopPlayerAndCleanup()
    }

    override fun onBind(intent: Intent?) = null
}
