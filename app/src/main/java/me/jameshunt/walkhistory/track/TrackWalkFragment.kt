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
import me.jameshunt.walkhistory.repo.WalkId
import org.koin.android.viewmodel.scope.viewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
            button.text = getString(R.string.start)
        }
        button.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                button.text = getString(R.string.working_on_it)
                val serviceRunning = isServiceRunning()
                when (serviceRunning) {
                    true -> activity?.stopLocationService()
                    false -> activity?.startLocationService(onPermissionFailure = {
                        button.text = getString(R.string.permission_required_plz)
                    })
                }
                viewModel.updateServiceStatus(!serviceRunning)
            }
        }

        viewModel.uiInfo.observe(this) {
            when(it.serviceRunning) {
                true -> {
                    startTime.visibility = View.VISIBLE
                    walkNumber.visibility = View.VISIBLE
                }
                false -> {
                    startTime.visibility = View.INVISIBLE
                    walkNumber.visibility = View.INVISIBLE
                }
            }
            button.text = when (it.serviceRunning) {
                true -> getString(R.string.stop)
                false -> getString(R.string.start)
            }

            walkNumber.text = getString(R.string.walk_number, it.walkId.toString())

            startTime.text = getString(
                R.string.started_at, DateTimeFormatter
                    .ofLocalizedTime(FormatStyle.SHORT)
                    .format(it.startTime)
            )
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
            UIInfo(
                serviceRunning = running,
                walkId = walkId,
                startTime = startTime
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
                startTime.until(currentTime, ChronoUnit.SECONDS).elapsedTimeString()
            }
            .combine(serviceStatus.asFlow()) { elapsedTime, isServiceRunning ->
                when (isServiceRunning) {
                    true -> elapsedTime
                    false -> ""
                }
            }
            .asLiveData(viewModelScope.coroutineContext)
    }
}

data class UIInfo(
    val serviceRunning: Boolean,
    val walkId: WalkId,
    val startTime: OffsetDateTime
)