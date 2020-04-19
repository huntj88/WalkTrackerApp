package me.jameshunt.walkhistory.track

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

abstract class ServiceAwareFragment : Fragment() {

    private val broadcaster by lazy { FragmentBroadCaster(requireActivity()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcaster.register()
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcaster.destroy()
    }

    suspend fun isServiceRunning(): Boolean {
        return broadcaster.isServiceRunning()
    }

    fun Activity.startLocationService() {
        permissionManager().onLocationGranted {
            when (it) {
                PermissionResult.Granted -> {
                    ContextCompat.startForegroundService(
                        this,
                        Intent(this, TrackerForegroundService::class.java)
                    )
                }
                PermissionResult.Denied -> {
                    // TODO: take them to a screen to go to permissions in android settings
                }
            }
        }
    }

    fun Activity.stopLocationService() {
        this.stopService(Intent(
            this@ServiceAwareFragment.context,
            TrackerForegroundService::class.java
        ))
    }
}