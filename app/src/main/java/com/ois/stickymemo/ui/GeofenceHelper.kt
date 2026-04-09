package com.ois.stickymemo.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.*
import com.ois.stickymemo.GeofenceBroadcastReceiver
import com.ois.stickymemo.data.Memo

class GeofenceHelper(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    @SuppressLint("MissingPermission")
    fun addGeofence(memo: Memo) {
        if (memo.latitude == null || memo.longitude == null) return

        val geofence = Geofence.Builder()
            .setRequestId(memo.id.toString())
            .setCircularRegion(memo.latitude, memo.longitude, memo.locationRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    fun removeGeofence(memoId: Int) {
        geofencingClient.removeGeofences(listOf(memoId.toString()))
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = "com.ois.stickymemo.ACTION_GEOFENCE_EVENT"
        }
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(
    context: Context,
    onSuccess: (Double, Double) -> Unit,
    onFailure: () -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onSuccess(location.latitude, location.longitude)
            } else {
                onFailure()
            }
        }
        .addOnFailureListener {
            onFailure()
        }
}