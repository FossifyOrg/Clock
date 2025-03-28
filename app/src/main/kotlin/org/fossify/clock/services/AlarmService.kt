package org.fossify.clock.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import org.fossify.clock.R
import org.fossify.clock.activities.ReminderActivity
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.extensions.getSnoozePendingIntent
import org.fossify.clock.extensions.getStopAlarmPendingIntent
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.ALARM_NOTIFICATION_CHANNEL_ID
import org.fossify.clock.helpers.ALARM_NOTIF_ID
import org.fossify.clock.models.Alarm
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.helpers.SILENT
import org.fossify.commons.helpers.isOreoPlus

/**
 * Service responsible for sounding the alarms and vibrations.
 * It also shows a notification with actions to dismiss or snooze an alarm.
 * Totally based on the previous implementation in the [ReminderActivity].
 */
class AlarmService : Service() {

    companion object {
        private const val DEFAULT_ALARM_VOLUME = 7
        private const val INCREASE_VOLUME_DELAY = 300L
        private const val MIN_ALARM_VOLUME_FOR_INCREASING_ALARMS = 1
    }

    private var alarm: Alarm? = null
    private var audioManager: AudioManager? = null
    private var initialAlarmVolume = DEFAULT_ALARM_VOLUME
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val autoDismissHandler = Handler(Looper.getMainLooper())
    private val increaseVolumeHandler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra(ALARM_ID, -1) ?: -1
        if (alarmId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        alarm = applicationContext.dbHelper.getAlarmWithId(alarmId)
        if (alarm == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(alarm!!)
        startForeground(ALARM_NOTIF_ID, notification)
        startAlarmEffects(alarm!!)
        startAutoDismiss(config.alarmMaxReminderSecs)
        return START_STICKY
    }

    private fun buildNotification(alarm: Alarm): Notification {
        val channelId = ALARM_NOTIFICATION_CHANNEL_ID
        if (isOreoPlus()) {
            val channel = NotificationChannel(
                channelId,
                getString(org.fossify.commons.R.string.alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
        }

        val contentTitle = if (alarm.label.isEmpty()) {
            getString(org.fossify.commons.R.string.alarm)
        } else {
            alarm.label
        }

        val contentText = getFormattedTime(
            passedSeconds = alarm.timeInMinutes * 60,
            showSeconds = false,
            makeAmPmSmaller = false
        )

        val reminderIntent = Intent(this, ReminderActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, reminderIntent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )

        val dismissIntent = applicationContext.getStopAlarmPendingIntent(alarm)
        val snoozeIntent = applicationContext.getSnoozePendingIntent(alarm)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_alarm_vector)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .addAction(
                org.fossify.commons.R.drawable.ic_snooze_vector,
                getString(org.fossify.commons.R.string.snooze),
                snoozeIntent
            )
            .addAction(
                org.fossify.commons.R.drawable.ic_cross_vector,
                getString(org.fossify.commons.R.string.dismiss),
                dismissIntent
            )
            .setDeleteIntent(dismissIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .build()
    }

    private fun startAlarmEffects(alarm: Alarm) {
        if (alarm.soundUri != SILENT) {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@AlarmService, alarm.soundUri.toUri())
                    isLooping = true
                    prepare()
                    start()
                }

                if (config.increaseVolumeGradually) {
                    initialAlarmVolume = audioManager?.getStreamVolume(
                        AudioManager.STREAM_ALARM
                    ) ?: DEFAULT_ALARM_VOLUME

                    scheduleVolumeIncrease(
                        lastVolume = MIN_ALARM_VOLUME_FOR_INCREASING_ALARMS.toFloat(),
                        maxVolume = initialAlarmVolume.toFloat(),
                        delay = 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (alarm.vibrate && isOreoPlus()) {
            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(500, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun scheduleVolumeIncrease(lastVolume: Float, maxVolume: Float, delay: Long) {
        increaseVolumeHandler.postDelayed({
            val newVolume = (lastVolume + 0.1f).coerceAtMost(maxVolume)
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, newVolume.toInt(), 0)
            if (newVolume < maxVolume) {
                scheduleVolumeIncrease(newVolume, maxVolume, INCREASE_VOLUME_DELAY)
            }
        }, delay)
    }

    private fun resetVolumeToInitialValue() {
        if (config.increaseVolumeGradually) {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, initialAlarmVolume, 0)
        }
    }

    private fun startAutoDismiss(durationSecs: Int) {
        autoDismissHandler.postDelayed({
            stopSelf()
        }, durationSecs * 1000L)
    }

    @SuppressLint("InlinedApi")
    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)

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

    override fun onBind(intent: Intent?) = null
}
