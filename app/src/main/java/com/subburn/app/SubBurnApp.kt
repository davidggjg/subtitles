package com.subburn.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.subburn.app.core.FontLibrary

class SubBurnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // הפונט העברי חייב להיות על הדיסק לפני הצריבה הראשונה.
        FontLibrary.configure(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_desc) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "burn_progress"
    }
}
