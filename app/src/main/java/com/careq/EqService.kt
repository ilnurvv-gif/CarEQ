package com.careq

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class EqService : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("eq", "Эквалайзер", NotificationManager.IMPORTANCE_LOW))
        }
        val n = NotificationCompat.Builder(this, "eq")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Car EQ работает")
            .setOngoing(true).build()
        startForeground(1, n)
        EqEngine.init(this)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
}
