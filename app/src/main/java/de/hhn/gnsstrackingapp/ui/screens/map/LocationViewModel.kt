package de.hhn.gnsstrackingapp.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class LocationData(
    val location: GeoPoint = GeoPoint(49.122666, 9.209987),
    val locationName: String = "Heilbronn",
    val accuracy: Float = 0.0f,
    val message: String = ""
)

class LocationViewModel : ViewModel() {

    private val point1 = LocationData(GeoPoint(49.0, 9.0), "Point 1", 0.0f, "Dead Person")
    private val point2 = LocationData(GeoPoint(48.9, 8.9), "Point 2", 0.0f, "")
    private val point3 = LocationData(GeoPoint(48.8, 8.8), "Point 3", 0.0f, "Injured Person")
    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData
    private val locations = mutableListOf(point1, point2, point3)

    fun updateLocation(location: GeoPoint, locationName: String, accuracy: Float) {
        viewModelScope.launch {
            _locationData.emit(LocationData(location, locationName, accuracy))
        }
    }

    fun addLocation(location: GeoPoint, locationName: String, accuracy: Float, message: String) {
        // new LocationData object
        val newLocation = LocationData(location, locationName, accuracy, message)

        // add the new object to the list
        locations.add(newLocation)
    }

    // get all locations from the array
    fun getAllLocations(): List<LocationData> {
        return locations
    }
}
