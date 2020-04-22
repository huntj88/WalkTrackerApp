package me.jameshunt.walkhistory.track

import android.os.Looper
import android.util.Log
import androidx.room.withTransaction
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import me.jameshunt.walkhistory.repo.AppDatabase
import me.jameshunt.walkhistory.repo.LocationTimestamp
import me.jameshunt.walkhistory.repo.WalkId
import java.time.OffsetDateTime

class LocationManager(
    private val locationCollector: LocationCollector,
    private val db: AppDatabase
) {

    suspend fun startCollectingWalkData() {
        var walkId: WalkId? = null
        locationCollector.getLocationUpdates().collect { latLng ->
            walkId
                ?.let { insertLocation(it, latLng) }
                ?: db.withTransaction {
                    walkId = db.walkDao().startAndGetNewWalk("").walkId
                    Log.d("starting walk", "$walkId")

                    insertLocation(walkId!!, latLng)
                }
        }
    }

    private suspend fun insertLocation(walkId: WalkId, latLng: LocationCollector.LatLong) {
        Log.d("inserting location", "$walkId: $latLng")
        db.locationTimestampDao().insert(
            LocationTimestamp(
                walkId = walkId,
                timestamp = OffsetDateTime.now(),
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        )
    }
}

class LocationCollector(private val locationClient: FusedLocationProviderClient) {
    private val locationRequest = LocationRequest().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 3_000
    }

    data class LatLong(val latitude: Double, val longitude: Double)

    fun getLocationUpdates() = callbackFlow<LatLong> {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation.run {
                    val latLng = LatLong(latitude, longitude)
                    this@callbackFlow.sendBlocking(latLng)
                }
            }
        }

        try {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("location requests", "started")
        } catch (e: SecurityException) {
            e.printStackTrace()
            throw e
        }

        awaitClose { locationClient.removeLocationUpdates(locationCallback) }
    }
}