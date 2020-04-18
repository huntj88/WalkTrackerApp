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
import androidx.lifecycle.*
import kotlinx.android.synthetic.main.fragment_track_walk.*
import kotlinx.coroutines.launch
import me.jameshunt.walkhistory.repo.AppDatabase
import org.koin.android.viewmodel.scope.viewModel
import java.time.Instant
import org.koin.android.scope.lifecycleScope as kLifecycleScope

abstract class ServiceAwareFragment : Fragment() {

    private val broadcaster by lazy { FragmentServiceBroadCaster(this.activity!!) }

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
                        Intent(this, WalkLocationService::class.java)
                    )
                }
                PermissionResult.Denied -> {
                    // TODO: take them to a screen to go to permissions in android settings
                }
            }
        }
    }

    fun Activity.stopLocationService() {
        this.stopService(Intent(this@ServiceAwareFragment.context, WalkLocationService::class.java))
    }
}

class TrackWalkFragment : ServiceAwareFragment() {

    private val viewModel by kLifecycleScope.viewModel<TrackWalkViewModel>(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_track_walk, container, false)

    override fun onResume() {
        super.onResume()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateServiceStatus(isServiceRunning(), shouldStartOrStop = false)
        }

        viewModel.uiInfo.observe(this) {
            button.text = it.buttonText

            if(it.shouldStartOrStop) {
                when (it.serviceRunning) {
                    true -> activity?.stopLocationService()
                    false -> activity?.startLocationService()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        button.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val serviceRunning = isServiceRunning()

                viewModel.updateServiceStatus(serviceRunning, shouldStartOrStop = true)
            }
        }
    }
}


class TrackWalkViewModel(
    private val db: AppDatabase
) : ViewModel() {

    private val serviceStatus = MutableLiveData<Pair<Boolean, Boolean>>()

    val uiInfo = liveData {
        val walk = db
            .walkDao()
            .getCurrentWalk()

        val startTime = db
            .locationUpdateDao()
            .getFirstLocationDataForWalk(walk.walkId)
            .timestamp

        serviceStatus
            .map { (running, shouldStartOrStop) ->
                val buttonText = when (running) {
                    true -> "Stop"
                    false -> "Start"
                }
                Triple(running, shouldStartOrStop, buttonText)
            }
            .map { (running, shouldStartOrStop, buttonText) ->
                UIInfo(
                    serviceRunning = running,
                    shouldStartOrStop = shouldStartOrStop,
                    walkId = walk.walkId,
                    startTime = startTime,
                    buttonText = buttonText
                )
            }
            .let { emitSource(it) }
    }

    fun updateServiceStatus(running: Boolean, shouldStartOrStop: Boolean) {
        serviceStatus.value = running to shouldStartOrStop
    }
}

data class UIInfo(
    val serviceRunning: Boolean,
    val shouldStartOrStop: Boolean,
    val walkId: Int,
    val startTime: Instant,
    val buttonText: String
)