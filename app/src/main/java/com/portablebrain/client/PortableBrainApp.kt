package com.portablebrain.client

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class PortableBrainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channelId = getString(R.string.notification_channel_id)
        val channelName = getString(R.string.notification_channel_name)
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Portable Brain is actively tracking UI state"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
