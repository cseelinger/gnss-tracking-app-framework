package de.hhn.gnsstrackingapp.ui.screens.map

import android.annotation.SuppressLint
import android.location.Location
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import de.hhn.gnsstrackingapp.R
import de.hhn.gnsstrackingapp.data.CircleData
import de.hhn.gnsstrackingapp.services.GeofenceBroadcastReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val circleOverlays = mutableListOf<Overlay>()


@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    mapView: MapView,
    mapViewModel: MapViewModel,
    locationViewModel: LocationViewModel,
    onCircleClick: () -> Unit = {}
) {
    val locationData by locationViewModel.locationData.collectAsState()
    val context = LocalContext.current

    val geofencingClient = LocationServices.getGeofencingClient(context)

    DisposableEffect(mapView) {
        initializeMapView(mapView, mapViewModel)
        onDispose {
            mapView.overlays.removeIf { it is CircleOverlay }
        }
    }

    val mapListener = object : MapListener {
        override fun onScroll(event: ScrollEvent?): Boolean {
            if (mapViewModel.isDrawingMode.value) {
                return false
            }
            event?.let {
                mapViewModel.mapOrientation = it.source.mapOrientation
            }

            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
            if (mapViewModel.isDrawingMode.value) {
                return false
            }
            event?.let {
                mapViewModel.zoomLevel = it.source.zoomLevelDouble
            }

            return true
        }

    }

    AndroidView(factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { mapViewUpdate ->
            mapViewUpdate.apply {
                if (!hasMapListener(mapListener)) {
                    addMapListener(mapListener)
                }

                updateMapViewState(mapView, mapViewModel, locationData, onCircleClick)

                invalidate()
            }
        })

    mapView.setOnTouchListener(object : OnTouchListener {
        private var startPoint: GeoPoint? = null
        private var currentCircle: CircleData? = null
        private val MIN_DISTANCE = 10
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (!mapViewModel.isDrawingMode.value) return false
            event?.let {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startPoint = mapView.projection.fromPixels(it.x.toInt(), it.y.toInt()) as GeoPoint?
                        startPoint?.let { point ->
                            if(mapViewModel.isDrawingCircle.value) {
                                currentCircle = CircleData(point, point)
                                val circle = CircleDrawingOverlay(currentCircle, { circleData ->
                                    mapView.overlays.removeIf{ it is CircleDrawingOverlay && it.getCurrentCircle() == circleData }
                                    circleOverlays.removeIf{ it is CircleDrawingOverlay && it.getCurrentCircle() == circleData }
                                    mapView.invalidate()
                                }, context)
                                circleOverlays.add(circle)
                                mapView.overlays.add(circle)
                            }
                            mapView.invalidate()
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        startPoint?.let { start ->
                            val currentPoint = mapView.projection.fromPixels(it.x.toInt(), it.y.toInt())
                            currentPoint?.let { current ->
                                if(mapViewModel.isDrawingCircle.value) {
                                    if(distanceBetweenPoints(start, current as GeoPoint) > MIN_DISTANCE) {
                                        currentCircle?.radiusPoint = current
                                    }
                                }
                                mapView.invalidate()
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if(mapViewModel.isDrawingCircle.value) {
                            currentCircle?.let { circleData ->
                                val circle = CircleDrawingOverlay(circleData, { circleData ->
                                    mapView.overlays.removeIf { it is CircleDrawingOverlay && it.getCurrentCircle() == circleData }
                                    circleOverlays.removeIf { it is CircleDrawingOverlay && it.getCurrentCircle() == circleData }
                                    mapView.invalidate()
                                }, context)
                                showNameDialog(circle) { ok, name ->
                                    if(ok) {
                                        circle.setName(name)
                                        createGeofenceForCircle(circle)
                                    }
                                }
                            }
                        } else {
                            currentCircle = null
                        }
                    }
                    else -> {}
                }
            }

            return true
        }
        private fun distanceBetweenPoints(point1: GeoPoint, point2: GeoPoint): Float {
            val result = FloatArray(1)
            Location.distanceBetween( point1.latitude, point1.longitude, point2.latitude, point2.longitude, result )
            return result[0]
        }

        @SuppressLint("MissingPermission")
        private fun createGeofenceForCircle(circle: CircleDrawingOverlay) {
            val radius = calculateRadius(circle.getCurrentCircle()!!.center, circle.getCurrentCircle()!!.radiusPoint)
            val geofence = Geofence.Builder()
                .setRequestId("circle_${circle.getCurrentCircle()?.center?.latitude}_${circle.getCurrentCircle()?.center?.longitude}")
                .setCircularRegion(
                    circle.getCurrentCircle()?.center!!.latitude,
                    circle.getCurrentCircle()?.center!!.longitude,
                    radius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(30000)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener {
                    Toast.makeText(context, "GeoFence erstellt", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {

                }
        }

        private fun calculateRadius(center: GeoPoint, radiusPoint: GeoPoint): Float {
            val earthRadius = 6371000.0 // Erdradius in Metern

            val lat1 = Math.toRadians(center.latitude)
            val lon1 = Math.toRadians(center.longitude)
            val lat2 = Math.toRadians(radiusPoint.latitude)
            val lon2 = Math.toRadians(radiusPoint.longitude)

            val dLat = lat2 - lat1
            val dLon = lon2 - lon1

            val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            val distance = earthRadius * c // Entfernung in Metern
            return distance.toFloat()
        }

        private fun getGeofencePendingIntent(): PendingIntent {
            val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun showNameDialog(overlay: Overlay, onResult: (Boolean, String) -> Unit) {
            val builder = AlertDialog.Builder(mapView.context)
            builder.setTitle("Kreis benennen")

            val input = EditText(mapView.context)
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                val name = input.text.toString()
                if(name.isBlank()) {
                    if(overlay is CircleDrawingOverlay) {
                        mapView.overlays.removeIf { it is CircleDrawingOverlay && it.getCurrentCircle() == overlay.getCurrentCircle() }
                        circleOverlays.removeIf { it is CircleDrawingOverlay && it.getCurrentCircle() == overlay.getCurrentCircle() }
                    }
                    onResult(false, name)
                    mapView.invalidate()
                } else {
                    onResult(true, name)
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("Abbrechen") { dialog, _ ->
                if(overlay is CircleDrawingOverlay) {
                    mapView.overlays.removeIf { it is CircleDrawingOverlay && it.getCurrentCircle() == overlay.getCurrentCircle() }
                    circleOverlays.removeIf { it is CircleDrawingOverlay && it.getCurrentCircle() == overlay.getCurrentCircle() }
                }
                onResult(false, "")
                mapView.invalidate()
                dialog.cancel()
            }

            builder.show()
        }
    })
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
        }
    }

    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver = remember(mapView) {
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            else -> {}
        }
    }
}

private fun MapView.hasMapListener(listener: MapListener): Boolean {
    return overlays.any { it == listener }
}

private fun initializeMapView(
    mapView: MapView, mapViewModel: MapViewModel,
) {
    mapView.apply {
        setUseDataConnection(true)
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val rotationGestureOverlay = RotationGestureOverlay(this).apply { isEnabled = true }
        val scaleOverlay = ScaleBarOverlay(this).apply {
            setCentred(true)
            setScaleBarOffset(300, 450)
        }

        overlays.add(rotationGestureOverlay)
        overlays.add(scaleOverlay)

        mapView.controller.animateTo(mapViewModel.centerLocation, mapViewModel.zoomLevel, 500)
    }
}

private fun updateMapViewState(
    mapView: MapView,
    mapViewModel: MapViewModel,
    locationData: LocationData,
    onCircleClick: () -> Unit
) {
    mapView.mapOrientation = mapViewModel.mapOrientation
    mapView.controller.setZoom(mapViewModel.zoomLevel)

    mapView.overlays.removeIf { it is CircleOverlay }
    mapView.overlays.removeAll{ it is CircleDrawingOverlay }

    val circleOverlay = CircleOverlay(
        locationData.location, 0.03f, locationData.accuracy, onCircleClick
    )
    mapView.overlays.add(circleOverlay)

    circleOverlays.forEach { circleOverlay ->
        mapView.overlays.add(circleOverlay)
        val circle = circleOverlay as? CircleDrawingOverlay
        val circleData = circle?.getCurrentCircle()
        if(circleData?.let { isLocationInsideCircle(locationData.location, it) } == true) {
            circle.isInside = true
            val currentTime = System.currentTimeMillis()
            if(currentTime - circle.lastCheckTime >= circle.checkInterval || circle.isNear) {
                onInside()
                circle.lastCheckTime = currentTime
            }
            circle.isNear = false
        } else if(circleData?.let { isLocationNearCircle(locationData.location, it, 50.0) } == true) {
            circle.isNear = true
            val currentTime = System.currentTimeMillis()
            if(currentTime - circle.lastCheckTime >= circle.checkInterval || circle.isInside) {
                onNear()
                circle.lastCheckTime = currentTime
            }
            circle.isInside = false
        } else {
            circle?.isInside = false
            circle?.isNear = false
        }

    }
}

private fun isLocationInsideCircle(location: GeoPoint, circle: CircleData): Boolean {
    val centerPoint = circle.center
    val radiusPoint = circle.radiusPoint
    val radius = centerPoint.distanceToAsDouble(radiusPoint)
    val distance = centerPoint.distanceToAsDouble(location)
    return distance <= radius
}

private fun isLocationNearCircle(location: GeoPoint, circle: CircleData, proximityDistance: Double): Boolean {
    val centerPoint = circle.center
    val radiusPoint = circle.radiusPoint
    val radius = centerPoint.distanceToAsDouble(radiusPoint)
    val distance = centerPoint.distanceToAsDouble(location)
    return distance <= (radius + proximityDistance)
}

private fun onInside() {

}

private fun onNear() {

}
