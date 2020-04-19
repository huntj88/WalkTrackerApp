package me.jameshunt.walkhistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions

class MapWrapperFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_map_wrapper, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapFragment = getMapFragment().also { fragment ->
            fragment.getMapAsync { map ->
                map?.run {
                    moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, ZOOM_LEVEL))
                    addMarker(MarkerOptions().position(SYDNEY))
                }
            }
        }

        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, mapFragment, "map")
            .commit()
    }

    private fun getMapFragment(): SupportMapFragment {
        return childFragmentManager
            .findFragmentByTag("map") as? SupportMapFragment
            ?: SupportMapFragment()
    }

    val SYDNEY = LatLng(-33.862, 151.21)
    val ZOOM_LEVEL = 13f
}