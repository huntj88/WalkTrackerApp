package me.jameshunt.walkhistory.repo

import android.content.Context
import android.os.Looper
import androidx.room.Room
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationService(applicationContext: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    private val locationRequest = LocationRequest().apply {
        numUpdates = 1
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val db = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java, "walk-tracker"
    ).build()

    suspend fun collectLocationData() {
        withContext(Dispatchers.IO) {
            while (true) {
                val location = getLocation()
                db.userDao().insertAll(LocationUpdate(
                    walkNumber = 1,
                    timestamp = Instant.now(),
                    latitude = location.latitude,
                    longitude = location.longitude
                ))
                delay(10_000)
            }
        }
    }

    data class LatLong(val latitude: Double, val longitude: Double)

    private suspend fun getLocation(): LatLong = suspendCoroutine {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation.run {
                    val latLng = LatLong(latitude, longitude)
                    it.resume(latLng)
                }

                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
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