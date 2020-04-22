package me.jameshunt.walkhistory.track

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*
import kotlinx.android.synthetic.main.fragment_track_walk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import me.jameshunt.walkhistory.R
import me.jameshunt.walkhistory.repo.AppDatabase
import org.koin.android.viewmodel.scope.viewModel
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.koin.android.scope.lifecycleScope as kLifecycleScope


class TrackWalkFragment : ServiceAwareFragment() {

    private val viewModel by kLifecycleScope.viewModel<TrackWalkViewModel>(this)
    private var canUseLocation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canUseLocation = requireActivity().permissionManager().canUseLocation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_track_walk, container, false)

    override fun onResume() {
        super.onResume()

        ifPermissionHandleStatus {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.updateServiceStatus(isServiceRunning())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!canUseLocation) {
            button.text = "Start"
        }
        button.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                button.text = "Working on it"
                val serviceRunning = isServiceRunning()
                when (serviceRunning) {
                    true -> activity?.stopLocationService()
                    false -> activity?.startLocationService(onPermissionFailure = {
                        button.text = "Permission Required Plz"
                    })
                }
                viewModel.updateServiceStatus(!serviceRunning)
            }
        }

        viewModel.uiInfo.observe(this) {
            // TODO: use other values
            button.text = it.buttonText
        }

        viewModel.elapsedTime.observe(this) {
            elapsedTime.text = it
        }
    }

    // handle resume after android permission dialog goes away when granted/denied
    private fun ifPermissionHandleStatus(action: () -> Unit) {
        if (canUseLocation) {
            action()
        } else if (requireActivity().permissionManager().canUseLocation()) {
            canUseLocation = true
        }
    }
}


class TrackWalkViewModel(private val db: AppDatabase) : ViewModel() {

    private val serviceStatus = MutableLiveData<Boolean>()

    private val currentWalkInfo = db
        .walkDao()
        .getCurrentWalk()
        .mapNotNull { it }
        .map {
            val locationTimeStampInfo = db
                .locationTimestampDao()
                .getInitialLocationTimestamp(it.walkId)

            it.walkId to locationTimeStampInfo.timestamp
        }

    val uiInfo = currentWalkInfo
        .combine(serviceStatus.asFlow()) { (walkId, startTime), running ->
            val buttonText = when (running) {
                true -> "Stop"
                false -> "Start"
            }
            UIInfo(
                serviceRunning = running,
                walkId = walkId,
                startTime = startTime,
                buttonText = buttonText
            )
        }
        .asLiveData(viewModelScope.coroutineContext)

    fun updateServiceStatus(running: Boolean) {
        serviceStatus.value = running
    }

    val elapsedTime: LiveData<String> = let {
        val everySecond = callbackFlow<OffsetDateTime> {
            while (true) {
                this.send(OffsetDateTime.now())
                delay(1000)
            }
        }
        return@let currentWalkInfo
            .combine(everySecond) { (_, startTime), currentTime ->
                val secondsDiff = startTime.until(currentTime, ChronoUnit.SECONDS)

                val renderedSeconds = secondsDiff % 60
                val minutes = secondsDiff / 60
                val renderedMinutes = minutes % 60
                val hours = minutes / 60

                listOf(hours, renderedMinutes, renderedSeconds).joinToString(" : ") {
                    when (it.toString().length == 1) {
                        true -> "0$it"
                        false -> "$it"
                    }
                }
            }
            .combine(serviceStatus.asFlow()) { elapsedTime, isServiceRunning ->
                when (isServiceRunning) {
                    true -> elapsedTime
                    false -> "blaaah"
                }
            }
            .asLiveData(viewModelScope.coroutineContext)
    }
}

data class UIInfo(
    val serviceRunning: Boolean,
    val walkId: Int,
    val startTime: OffsetDateTime,
    val buttonText: String
)