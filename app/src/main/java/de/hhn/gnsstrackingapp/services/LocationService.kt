package de.hhn.gnsstrackingapp.services

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import de.hhn.gnsstrackingapp.CHANNEL_ID
import de.hhn.gnsstrackingapp.MainActivity
import de.hhn.gnsstrackingapp.R
import java.util.Locale

class LocationService : Service() {
    companion object {
        var onLocationUpdate: ((Double, Double, String, Float) -> Unit)? = null
    }

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        .setWaitForAccurateLocation(false).setIntervalMillis(3000).build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val latitude = locationResult.lastLocation?.latitude
            val longitude = locationResult.lastLocation?.longitude
            val accuracy = locationResult.lastLocation?.accuracy ?: 0f

            latitude?.let { lat ->
                longitude?.let { lon ->
                    getLocationName(lat, lon) { locationName ->
                        startForegroundNotification(lat, lon, locationName, accuracy)
                        onLocationUpdate?.invoke(lat, lon, locationName, accuracy)
                    }
                }
            }
        }
    }


    private var lastKnownLocationName: String = "Unknown Location"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Location permissions are not granted.")
            return
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun startForegroundNotification(
        latitude: Double, longitude: Double, locationName: String, accuracy: Float = 0f
    ) {
        if (locationName != "Unknown Location") {
            lastKnownLocationName = locationName
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Tracking Location")
            .setContentText("$lastKnownLocationName ($latitude, $longitude) - Accuracy: $accuracy m")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).setContentIntent(pendingIntent)
            .setOngoing(true).build()

        startForeground(1, notification)
    }


    private fun getLocationName(latitude: Double, longitude: Double, callback: (String) -> Unit) {
        val geocoder = Geocoder(this, Locale.getDefault())

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val locationName = if (addresses.isNotEmpty()) {
                        addresses[0].locality ?: "Unknown Location"
                    } else {
                        "Unknown Location"
                    }
                    callback(locationName)
                }

                override fun onError(errorMessage: String?) {
                    Log.e("LocationService", "Geocode error: $errorMessage")
                    callback("Unknown Location")
                }
            })
        } else {
            try {
                @Suppress("DEPRECATION") val addresses: MutableList<Address>? =
                    geocoder.getFromLocation(latitude, longitude, 1)
                val locationName = if (addresses?.isNotEmpty() == true) {
                    addresses[0].locality ?: "Unknown Location"
                } else {
                    "Unknown Location"
                }
                callback(locationName)
            } catch (e: Exception) {
                e.printStackTrace()
                callback("Unknown Location")
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}
