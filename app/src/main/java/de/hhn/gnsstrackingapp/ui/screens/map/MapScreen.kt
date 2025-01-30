package de.hhn.gnsstrackingapp.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.hhn.gnsstrackingapp.ui.theme.Purple40
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
// new imports
import androidx.compose.runtime.*

@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    locationViewModel: LocationViewModel,
) {
    val mapView = rememberMapViewWithLifecycle()
    val locationData by locationViewModel.locationData.collectAsState()

    // state for the selected location
    var selectedLocation by remember { mutableStateOf<LocationData?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            mapView = mapView,
            mapViewModel = mapViewModel,
            locationViewModel = locationViewModel,
            onCircleClick = { clickedLocation ->
                selectedLocation = clickedLocation // changed signature
            }
        )

        // show the LocationCard for the clicked point
        selectedLocation?.let {
            LocationCard(locationData = it)
        }

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxSize()
        ) {
            GetOwnLocationButton(onClick = {
                val targetLocation = locationViewModel.locationData.value.firstOrNull()?.location
                if (targetLocation != null) {
                    val distance = calculateDistance(
                        mapViewModel.centerLocation.latitude,
                        mapViewModel.centerLocation.longitude,
                        targetLocation.latitude,
                        targetLocation.longitude
                    )
                    val animationDuration = calculateAnimationDuration(
                        distance, mapViewModel.zoomLevel
                    )

                    mapViewModel.centerLocation = targetLocation
                    mapViewModel.zoomLevel = 20.0

                    mapViewModel.isAnimating.value = true
                    mapView.controller.animateTo(
                        mapViewModel.centerLocation, mapViewModel.zoomLevel, animationDuration, 0f
                    )
                    mapViewModel.isAnimating.value = false

                    // set the new location as active
                    selectedLocation = locationData.firstOrNull()
                }
            })
        }
    }
}

@Composable
fun GetOwnLocationButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = { onClick() },
        modifier = Modifier.padding(16.dp),
        containerColor = Purple40,
        contentColor = Color.White
    ) {
        Icon(Icons.Filled.LocationOn, "Floating action button.")
    }
}

@Composable
fun LocationCard(locationData: LocationData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.scale(1.2f)
                )
                Text(
                    text = locationData.locationName,
                    fontSize = 22.sp,
                    fontWeight = Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${locationData.location.latitude}° ${
                        getLatitudeDirection(
                            locationData.location.latitude
                        )
                    }", fontSize = 16.sp
                )
                Text(
                    text = "${locationData.location.longitude}° ${
                        getLongitudeDirection(
                            locationData.location.longitude
                        )
                    }", fontSize = 16.sp
                )
            }
        }
    }
}

fun getLatitudeDirection(latitude: Double): String {
    return if (latitude >= 0) "N" else "S" // North if positive, South if negative
}

fun getLongitudeDirection(longitude: Double): String {
    return if (longitude >= 0) "E" else "W" // East if positive, West if negative
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371e3 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a =
        sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(
            dLon / 2
        ) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c // Distance in meters
}

fun calculateAnimationDuration(distance: Double, zoomLevel: Double): Long {
    val baseDuration = 2000L
    val distanceFactor = distance / 1000.0
    val zoomFactor = 1.0 / (zoomLevel + 1.0)
    val calculatedDuration =
        baseDuration + (distanceFactor * 500).toLong() + (zoomFactor * 1000).toLong()
    val maxDuration = 5000L

    return calculatedDuration.coerceAtMost(maxDuration)
}
