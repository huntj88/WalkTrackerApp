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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.jameshunt.walkhistory.repo.LocationService
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.KoinContextHandler
import org.koin.core.context.startKoin
import kotlin.coroutines.CoroutineContext


class WalkLocationService : Service(), CoroutineScope {

    private val locationService: LocationService by inject()

    private val broadcaster by lazy { ServiceBroadCaster(this) }

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
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Walk Tracker Service")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        // A notification HAS to be passed for the foreground service to be started.
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        broadcaster.register()

        KoinContextHandler.getOrNull() ?: startKoin {
            androidContext(this@WalkLocationService)
            modules(appModule(applicationContext))
        }

        Log.d("service", "starting")

        launch {
            locationService.collectLocationData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcaster.destroy()
        coroutineJob.cancel()
        Log.d("service", "stopping")
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