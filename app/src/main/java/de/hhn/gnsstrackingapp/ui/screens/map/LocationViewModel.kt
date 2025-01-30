package de.hhn.gnsstrackingapp.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

import android.util.Log

data class LocationData(
    val location: GeoPoint = GeoPoint(49.122666, 9.209987),
    val locationName: String = "Heilbronn",
    val accuracy: Float = 0.0f
)

class LocationViewModel : ViewModel() {
    private val _locationData = MutableStateFlow<List<LocationData>>(emptyList())
    val locationData: StateFlow<List<LocationData>> = _locationData

    init {
        updateLocation(GeoPoint(48.755757, 9.190172), "Stuttgart", 0.0f)
        updateLocation(GeoPoint(48.858222, 2.2945), "Eiffelturm", 0.0f,)
        updateLocation(GeoPoint(52.516389, 13.377778), "Brandenburger Tor", 0.0f)
    }

    fun updateLocation(location: GeoPoint, locationName: String, accuracy: Float) {

        // new LocationData object
        val newLocation = LocationData(location, locationName, accuracy)

        // add the new object to the list
        val updatedList = _locationData.value.toMutableList()
        updatedList.add(newLocation)

        _locationData.value = updatedList
    }

    // get all locations from the location list
    fun getAllLocations(): List<LocationData> {
        Log.d("LocationViewModel", "Locations: ${_locationData}")
        return _locationData.value
    }
}
