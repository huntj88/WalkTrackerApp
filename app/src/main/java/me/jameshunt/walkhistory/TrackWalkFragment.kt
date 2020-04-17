package me.jameshunt.walkhistory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_track_walk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class TrackWalkFragment : Fragment(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + coroutineJob

    private val broadcaster by lazy { FragmentServiceBroadCaster(this.activity!!) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcaster.register()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_track_walk, container, false)
    }

    override fun onResume() {
        super.onResume()

        launch {
            broadcaster.ping(
                running = {
                    button.text = "Stop"
                },
                notRunning = {
                    button.text = "Start"
                }
            )
        }

        // TODO
        // check service running
        // if not, then show start run

        // if service is running query start time
        // show elapsed time
        // show button to stop run
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        button.setOnClickListener {
            launch {
                broadcaster.ping(
                    running = {
                        Log.d("service", "Stopping")
                        activity?.stopService(
                            Intent(
                                this@TrackWalkFragment.context,
                                WalkLocationService::class.java
                            )
                        )
                        button.text = "Start"
                    },
                    notRunning = {
                        Log.d("service", "Starting")
                        activity?.startLocationService()
                        button.text = "Stop"
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcaster.destroy()
    }

    private fun Activity.startLocationService() {
        permissionManager().onLocationGranted {
            when (it) {
                PermissionResult.Granted -> {
                    ContextCompat.startForegroundService(
                        this,
                        Intent(this, WalkLocationService::class.java)
                    )
                }
                PermissionResult.Denied -> {
                    // TODO: take them to a screen to go to permissions in android settings
                }
            }
        }
    }
}
