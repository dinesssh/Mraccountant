package com.example.mraccount

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class LocationHelper(private val context: Context) {

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
    fun getCurrentCity(callback: (String?) -> Unit) {
        if (!hasLocationPermissions()) {
            callback(null)
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    getCityName(location.latitude, location.longitude) { city ->
                        callback(city)
                    }
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            getCityName(lastLoc.latitude, lastLoc.longitude) { city ->
                                callback(city)
                            }
                        } else {
                            callback(null)
                        }
                    }
                }
            }
            .addOnFailureListener {
                callback(null)
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
