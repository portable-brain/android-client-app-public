package com.portablebrain.client.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.portablebrain.client.MainActivity
import com.portablebrain.client.R
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Foreground service that periodically captures UI state via the AccessibilityService
 * and posts it to the Portable Brain backend for context-aware processing.
 *
 * This is a simplified stub for the public build. The full implementation includes
 * proprietary state-change detection, interaction filtering, and context delivery
 * logic that determines what UI data is sent to the backend and when.
 *
 * To build a complete implementation, you would:
 *   1. Poll [PortableBrainAccessibilityService.getCurrentSnapshot] on a regular interval
 *   2. Construct a [UIStateSnapshot] from the accessibility data
 *   3. POST it to the backend via [RetrofitClient.service.postState]
 *   4. Call startTracking/stopTracking on the backend at session boundaries
 *
 * See the data models in data/Models.kt and the API surface in network/ApiService.kt
 * for the expected request/response formats.
 */
class TrackingForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.portablebrain.client.START_TRACKING"
        const val ACTION_STOP = "com.portablebrain.client.STOP_TRACKING"
        private const val NOTIFICATION_ID = 1001

        val isRunning = MutableStateFlow(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning.value = true

        // TODO: Implement your polling loop here.
        // The full Portable Brain client polls the AccessibilityService every second,
        // detects meaningful state changes, and posts context to the backend.
    }

    private fun stopTracking() {
        isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val channelId = getString(R.string.notification_channel_id)
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text_active))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }
}
