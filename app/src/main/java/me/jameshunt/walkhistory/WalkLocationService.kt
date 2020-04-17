package me.jameshunt.walkhistory

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import me.jameshunt.walkhistory.repo.LocationService
import kotlin.coroutines.CoroutineContext


class WalkLocationService : Service(), CoroutineScope {

    private val locationService by lazy { LocationService(applicationContext) }

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + coroutineJob

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "WalkTrackerChannel"
        createNotificationChannel(channelId, "Walk Tracker Service");

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        //Build a notification
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Walk Tracker Service")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        //A notification HAS to be passed for the foreground service to be started.
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("service", "created")

        launch {
            locationService.collectLocationData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String) {
        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        getSystemService(Context.NOTIFICATION_SERVICE)
            .let { it as NotificationManager }
            .createNotificationChannel(chan)
    }
}