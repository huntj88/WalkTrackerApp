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
import kotlinx.coroutines.*
import me.jameshunt.walkhistory.repo.AppDatabase
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class TrackWalkFragment : Fragment(), CoroutineScope {

    private val db: AppDatabase by inject()

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
    ): View = inflater.inflate(R.layout.fragment_track_walk, container, false)

    override fun onResume() {
        super.onResume()

        launch {
            val startTimeDef = async {
                val walkId = db.walkDao().getCurrentWalk().walkId
                db.locationUpdateDao().getFirstLocationDataForWalk(walkId).timestamp
            }

            val isServiceRunningDef = async { broadcaster.isServiceRunning() }

            val startTime = startTimeDef.await()
            val isServiceRunning = isServiceRunningDef.await()

            when (isServiceRunning) {
                true -> {
                    button.text = "Stop"
                }
                false -> {
                    button.text = "Start"
                }
            }
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
                when (broadcaster.isServiceRunning()) {
                    true -> {
                        activity?.stopLocationService()
                        button.text = "Start"
                    }
                    false -> {
                        activity?.startLocationService()
                        button.text = "Stop"
                    }
                }
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

    private fun Activity.stopLocationService() {
        this.stopService(Intent(this@TrackWalkFragment.context, WalkLocationService::class.java))
    }
}
