package de.hhn.gnsstrackingapp.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        val geofencingEvent = p1?.let { GeofencingEvent.fromIntent(it) }

        if (geofencingEvent != null) {
            if(geofencingEvent.hasError()) {
                val errorMessage = geofencingEvent.errorCode
                return
            }
        }
        val geofenceTransition = geofencingEvent?.geofenceTransition

        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val triggeringGeofences = geofencingEvent?.triggeringGeofences
            val geofenceIds = triggeringGeofences?.map { it.requestId }

            if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                geofenceIds?.forEach { id ->
                    Toast.makeText(p0, "Geofence betreten: $id", Toast.LENGTH_SHORT).show()
                }
            } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                geofenceIds?.forEach { id ->
                    Toast.makeText(p0, "Geofence verlassen: $id", Toast.LENGTH_SHORT).show()
                }
            } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                geofenceIds?.forEach { id ->
                    Toast.makeText(p0, "Im Geofence: $id", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(p0, "Im Geofence:", Toast.LENGTH_SHORT).show()
        }
    }

}