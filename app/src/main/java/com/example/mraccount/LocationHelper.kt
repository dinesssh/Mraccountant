package com.example.mraccount

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class LocationHelper(private val context: Context) {

    // Functional interface for Java-friendly callbacks
    fun interface CityCallback {
        fun onCityDetected(city: String?)
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getCurrentCity(callback: CityCallback) {
        if (!hasLocationPermissions()) {
            callback.onCityDetected(null)
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    getCityName(location.latitude, location.longitude) { city ->
                        Handler(Looper.getMainLooper()).post { callback.onCityDetected(city) }
                    }
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            getCityName(lastLoc.latitude, lastLoc.longitude) { city ->
                                Handler(Looper.getMainLooper()).post { callback.onCityDetected(city) }
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post { callback.onCityDetected(null) }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Handler(Looper.getMainLooper()).post { callback.onCityDetected(null) }
            }
    }

    private fun getCityName(lat: Double, lng: Double, callback: (String?) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        callback(address.locality ?: address.subAdminArea ?: address.adminArea)
                    } else {
                        callback(null)
                    }
                }
                override fun onError(errorMessage: String?) {
                    callback(null)
                }
            })
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    callback(address.locality ?: address.subAdminArea ?: address.adminArea)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }
}
