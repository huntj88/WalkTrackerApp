package me.jameshunt.walkhistory.repo

import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationService(
    private val db: AppDatabase,
    private val locationClient: FusedLocationProviderClient
) {

    private val locationRequest = LocationRequest().apply {
        numUpdates = 1
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    suspend fun collectLocationData() {
        val walkId = db.walkDao().startAndGetNewWalk("").walkId
        withContext(Dispatchers.IO) {
            while (true) {
                val location = getLocation()
                Log.d("location", "got location")
                db.locationUpdateDao().insert(
                    LocationUpdate(
                        walkId = walkId,
                        timestamp = Instant.now(),
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
                delay(10_000)
            }
        }
    }

    private data class LatLong(val latitude: Double, val longitude: Double)

    private suspend fun getLocation(): LatLong = suspendCoroutine {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation.run {
                    val latLng = LatLong(latitude, longitude)
                    it.resume(latLng)
                }

                locationClient.removeLocationUpdates(this)
            }
        }

        try {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            throw e
        }
    }
}