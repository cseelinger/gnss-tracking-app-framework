package de.hhn.gnsstrackingapp.ui.screens.map

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint

class MapViewModel : ViewModel() {
    var zoomLevel: Double = 12.0
    var centerLocation: GeoPoint = GeoPoint(48.947410, 9.144216)
    var mapOrientation: Float = 0f
    var isAnimating = mutableStateOf(false)

    var isDrawingMode = mutableStateOf(false)

    var isDrawingCircle = mutableStateOf(false)
}
