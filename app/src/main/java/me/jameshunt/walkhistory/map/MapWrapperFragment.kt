package me.jameshunt.walkhistory.map

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
import kotlinx.android.synthetic.main.fragment_map_wrapper.*
import kotlinx.coroutines.flow.map
import me.jameshunt.walkhistory.R
import me.jameshunt.walkhistory.repo.AppDatabase
import org.koin.android.viewmodel.scope.viewModel
import org.koin.android.scope.lifecycleScope as kLifecycleScope

class MapWrapperFragment : Fragment() {

    private val viewModel by kLifecycleScope.viewModel<MapWrapperViewModel>(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_map_wrapper, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectWalkButton.setOnClickListener {
            WalkHistoryFragment().show(fragmentManager!!, WalkHistoryFragment::class.simpleName)
        }

        val mapFragment = when (val existing = getExistingMapFragment()) {
            null -> SupportMapFragment().also {
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, it, SupportMapFragment::class.simpleName)
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
    }

    private fun getExistingMapFragment(): SupportMapFragment? = childFragmentManager
        .findFragmentByTag(SupportMapFragment::class.simpleName) as? SupportMapFragment
}

class MapWrapperViewModel(
    private val db: AppDatabase,
    selectedWalkService: SelectedWalkService
) : ViewModel() {

    val coordinatesForSelectedWalk = selectedWalkService
        .selectedWalkId
        .map { db.locationTimestampDao().getLocationTimestampsForWalk(it) }
        .asLiveData(viewModelScope.coroutineContext)
}