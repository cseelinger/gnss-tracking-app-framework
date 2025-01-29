package de.hhn.gnsstrackingapp.ui.screens.map

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
import de.hhn.gnsstrackingapp.R
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    mapView: MapView,
    mapViewModel: MapViewModel,
    locationViewModel: LocationViewModel,
    onCircleClick: () -> Unit = {}
) {
    val locationData by locationViewModel.locationData.collectAsState()

    DisposableEffect(mapView) {
        initializeMapView(mapView, mapViewModel)
        onDispose {
            mapView.overlays.removeIf { it is CircleOverlay }
        }
    }

    val mapListener = object : MapListener {
        override fun onScroll(event: ScrollEvent?): Boolean {
            event?.let {
                mapViewModel.mapOrientation = it.source.mapOrientation
            }

            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
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

    val circleOverlay = CircleOverlay(
        locationData.location, 0.03f, locationData.accuracy, onCircleClick
    )
    mapView.overlays.add(circleOverlay)

    if (mapViewModel.isAnimating.value && mapViewModel.centerLocation != locationData.location) {
        mapViewModel.centerLocation = locationData.location

        mapView.controller.animateTo(mapViewModel.centerLocation, mapViewModel.zoomLevel, 500)

        mapViewModel.isAnimating.value = false
    }
}
