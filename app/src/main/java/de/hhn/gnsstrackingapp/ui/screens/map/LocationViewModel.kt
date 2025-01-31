package de.hhn.gnsstrackingapp.ui.screens.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.util.GeoPoint

data class LocationData(
    val location: GeoPoint = GeoPoint(49.122666, 9.209987),
    val locationName: String = "Heilbronn",
    val accuracy: Float = 0.0f
)

class LocationViewModel : ViewModel() {
    // changed the location data to a list to show all points
    private val _locationData = MutableStateFlow<List<LocationData>>(emptyList())
    val locationData: StateFlow<List<LocationData>> = _locationData

    // add some demo points to show the functionality
    init {
        updateLocation(GeoPoint(48.755757, 9.190172), "Here lies an injured person. She needs help.", 0.0f)
        updateLocation(GeoPoint(48.858222, 2.2945), "Paris", 0.0f)
        updateLocation(GeoPoint(52.516389, 13.377778), "Berlin", 0.0f)
    }

    fun updateLocation(location: GeoPoint, locationName: String, accuracy: Float) {

        // new LocationData object
        val newLocation = LocationData(location, locationName, accuracy)

        // add the new object to the list
        val updatedList = _locationData.value.toMutableList()
        updatedList.add(newLocation)

        _locationData.value = updatedList
    }
}
