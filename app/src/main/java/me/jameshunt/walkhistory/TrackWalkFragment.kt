package me.jameshunt.walkhistory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment

class TrackWalkFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_track_walk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.button).setOnClickListener {
            activity?.startLocationService()
        }
    }

    private fun Activity.startLocationService() {
        permissionManager().onLocationGranted {
            when (it) {
                PermissionResult.Granted -> {
                    startForegroundService(this, Intent(this, WalkLocationService::class.java))
                }
                PermissionResult.Denied -> {
                    // TODO: take them to a screen to go to permissions in android settings
                }
            }
        }
    }
}