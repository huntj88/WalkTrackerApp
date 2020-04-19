package me.jameshunt.walkhistory.repo

import android.os.Looper
import android.util.Log
import androidx.room.withTransaction
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.*
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
        val walkId = startNewWalk()

        while (true) {
            delay(10_000)

            val location = getLocation()
            Log.d("location", "got location")
            db.locationTimestampDao().insert(
                LocationTimestamp(
                    walkId = walkId,
                    timestamp = Instant.now(),
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }

    private suspend fun startNewWalk(): Int = coroutineScope {
        val locationAsync = async { getLocation() }
        db.withTransaction {
            val walkId = db.walkDao().startAndGetNewWalk("").walkId

            Log.d("starting walk", "$walkId")

            val location = locationAsync.await()
            db.locationTimestampDao().insert(
                LocationTimestamp(
                    walkId = walkId,
                    timestamp = Instant.now(),
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )

            return@withTransaction walkId
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