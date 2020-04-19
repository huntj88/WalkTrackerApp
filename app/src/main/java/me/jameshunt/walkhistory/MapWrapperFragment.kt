package me.jameshunt.walkhistory

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.libraries.maps.model.PolylineOptions
import kotlinx.coroutines.flow.map
import me.jameshunt.walkhistory.repo.AppDatabase
import org.koin.android.viewmodel.scope.viewModel
import org.koin.android.scope.lifecycleScope as kLifecycleScope

class MapWrapperFragment : Fragment() {

    // TODO: do not expose, make private
    val viewModel by kLifecycleScope.viewModel<MapWrapperViewModel>(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_map_wrapper, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapFragment = when (val existing = getExistingMapFragment()) {
            null -> SupportMapFragment().also {
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, it, "map")
                    .commit()
            }
            else -> existing
        }

        viewModel.coordinatesForSelectedWalk.observe(this) { locations ->
            mapFragment.getMapAsync { map ->
                map ?: return@getMapAsync

                map.clear()
                val path: List<LatLng> = locations.map { LatLng(it.latitude, it.longitude) }

                if (path.isNotEmpty()) {
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(path)
                            .color(Color.BLUE)
                            .width(5f)
                    )

                    map.addMarker(MarkerOptions().position(path.first()))
                    map.addMarker(MarkerOptions().position(path.last()))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(path.first(), 16f))
                }

                map.uiSettings.isZoomControlsEnabled = true
            }
        }

        // hardcoded walkId
        viewModel.setSelectedWalk(1)
    }

    private fun getExistingMapFragment(): SupportMapFragment? {
        return childFragmentManager.findFragmentByTag("map") as? SupportMapFragment
    }
}

class MapWrapperViewModel(private val db: AppDatabase) : ViewModel() {

    private val selectedWalk = MutableLiveData<Int>()
    val coordinatesForSelectedWalk = selectedWalk
        .asFlow()
        .map { db.locationTimestampDao().getLocationTimestampsForWalk(it) }
        .asLiveData(viewModelScope.coroutineContext)

    fun setSelectedWalk(walkId: Int) {
        selectedWalk.value = walkId
    }
}