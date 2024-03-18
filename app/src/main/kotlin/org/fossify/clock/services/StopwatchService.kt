package org.fossify.clock.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import org.fossify.clock.R
import org.fossify.clock.extensions.getFormattedDuration
import org.fossify.clock.extensions.getOpenStopwatchTabIntent
import org.fossify.clock.helpers.STOPWATCH_RUNNING_NOTIF_ID
import org.fossify.clock.helpers.Stopwatch
import org.fossify.clock.helpers.Stopwatch.State
import org.fossify.clock.helpers.Stopwatch.UpdateListener
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.isOreoPlus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class StopwatchService : Service() {
    private val bus = EventBus.getDefault()
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var isStopping = false

    override fun onCreate() {
        super.onCreate()
        bus.register(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = getServiceNotificationBuilder(
            getString(R.string.app_name),
            getString(R.string.stopwatch)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isStopping = false
        startForeground(
            STOPWATCH_RUNNING_NOTIF_ID,
            notificationBuilder.build()
        )
        Stopwatch.addUpdateListener(updateListener)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        bus.unregister(this)
        Stopwatch.removeUpdateListener(updateListener)
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: StopwatchStopService) {
        isStopping = true
        stopForegroundService()
    }

    private fun getServiceNotificationBuilder(
        title: String,
        contentText: String,
    ): NotificationCompat.Builder {
        val channelId = "simple_alarm_stopwatch"
        val label = getString(R.string.stopwatch)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        if (isOreoPlus()) {
            NotificationChannel(channelId, label, importance).apply {
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stopwatch_vector)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(null)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(getOpenStopwatchTabIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun updateNotification(totalTime: Long) {
        val formattedDuration = totalTime.getFormattedDuration()
        notificationBuilder.setContentTitle(formattedDuration).setContentText(getString(R.string.stopwatch))
        notificationManager.notify(STOPWATCH_RUNNING_NOTIF_ID, notificationBuilder.build())
    }

    private val updateListener = object : UpdateListener {
        private val MIN_NOTIFICATION_UPDATE_INTERVAL = 500L
        private var lastUpdateTime = 0L
        override fun onUpdate(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
            if (!isStopping && shouldNotificationBeUpdated()) {
                lastUpdateTime = System.currentTimeMillis()
                updateNotification(totalTime)
            }
        }

        override fun onStateChanged(state: State) {
            if (state == State.STOPPED) {
                stopForegroundService()
            }
        }

        private fun shouldNotificationBeUpdated(): Boolean {
            return (System.currentTimeMillis() - lastUpdateTime) > MIN_NOTIFICATION_UPDATE_INTERVAL
        }
    }

    private fun stopForegroundService() {
        ServiceCompat.stopForeground(this@StopwatchService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}

fun startStopwatchService(context: Context) {
    Handler(Looper.getMainLooper()).post {
        try {
            ContextCompat.startForegroundService(context, Intent(context, StopwatchService::class.java))
        } catch (e: Exception) {
            context.showErrorToast(e)
        }
    }
}

object StopwatchStopService
