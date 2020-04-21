package me.jameshunt.walkhistory.track

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    suspend fun Activity.startLocationService(onPermissionFailure: () -> Unit) =
        when (permissionManager().getLocationPermission()) {
            PermissionResult.Granted -> {
                Log.d("permission", "denied")
                ContextCompat.startForegroundService(
                    this@startLocationService,
                    Intent(this@startLocationService, TrackerForegroundService::class.java)
                )
            }
            PermissionResult.Denied -> {
                Log.d("permission", "denied")
                onPermissionFailure()
            }
        }

    fun Activity.stopLocationService() {
        this.stopService(Intent(
            this@ServiceAwareFragment.context,
            TrackerForegroundService::class.java
        ))
    }
}