package de.hhn.gnsstrackingapp.ui.screens.map

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

class MapViewModel : ViewModel() {
    var zoomLevel: Double = 12.0
    var centerLocation: GeoPoint = GeoPoint(48.947410, 9.144216)
    var mapOrientation: Float = 0f
    var isAnimating = mutableStateOf(false)

    var isDrawingMode = mutableStateOf(false)

    var isDrawingCircle = mutableStateOf(false)

    var circles by mutableStateOf<List<Triple<Double, Double, Float>>>(emptyList())
        private set

    fun enableDrawMode() {
        isDrawingMode.value = true
    }

    fun disableDrawMode() {
        isDrawingMode.value = false
    }

    fun enableDrawCircleMode() {
        isDrawingCircle.value = true
    }

    fun disableDrawCircleMode() {
        isDrawingCircle.value = false
    }

    fun addCircle(lat: Double, lon: Double, radius: Float) {
        circles = circles + Triple(lat, lon, radius)
    }
}
