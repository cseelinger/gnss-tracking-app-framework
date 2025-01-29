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
    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData

    // example point to create
    private val point1 = LocationData(GeoPoint(49.0, 9.0), "Point 1", 0.0f, "Dead Person")
    // list of locations
    private val locations = mutableListOf(point1)

    fun updateLocation(location: GeoPoint, locationName: String, accuracy: Float) {
        viewModelScope.launch {
            _locationData.emit(LocationData(location, locationName, accuracy))
        }
    }

    // method to add a new location to the list
    fun addLocation(location: GeoPoint, locationName: String, accuracy: Float, message: String) {
        // new LocationData object
        val newLocation = LocationData(location, locationName, accuracy, message)

        // add the new object to the list
        locations.add(newLocation)
    }

    // get all locations from the location list
    fun getAllLocations(): List<LocationData> {
        println(locations)
        return locations
    }
}
