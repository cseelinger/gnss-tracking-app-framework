package de.hhn.gnsstrackingapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import de.hhn.gnsstrackingapp.network.WebServicesProvider
import de.hhn.gnsstrackingapp.services.LocationService
import de.hhn.gnsstrackingapp.services.ServiceManager
import de.hhn.gnsstrackingapp.ui.navigation.MainNavigation
import de.hhn.gnsstrackingapp.ui.navigation.NavigationBarComponent
import de.hhn.gnsstrackingapp.ui.screens.map.LocationViewModel
import de.hhn.gnsstrackingapp.ui.screens.map.MapViewModel
import de.hhn.gnsstrackingapp.ui.screens.settings.SettingsViewModel
import de.hhn.gnsstrackingapp.ui.screens.statistics.StatisticsViewModel
import de.hhn.gnsstrackingapp.ui.screens.statistics.parseGnssJson
import de.hhn.gnsstrackingapp.ui.theme.GNSSTrackingAppTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.util.GeoPoint
// new imports
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.MapView
import androidx.compose.runtime.collectAsState
import org.osmdroid.views.overlay.Marker
import de.hhn.gnsstrackingapp.ui.screens.map.LocationData
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {
    private lateinit var serviceManager: ServiceManager
    private lateinit var webServicesProvider: WebServicesProvider

    private val mapViewModel: MapViewModel by viewModel()
    private val locationViewModel: LocationViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val statisticsViewModel: StatisticsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // add more points of locations to the list
        addPredefinedLocations()

        serviceManager = ServiceManager(this)

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = serviceManager.requiredPermissions.all { permissions[it] == true }
                if (allGranted) {
                    serviceManager.startLocationService()
                } else {
                    Log.e("MainActivity", "Permissions not granted: $permissions")
                }
            }

        if (!serviceManager.arePermissionsGranted()) {
            requestPermissionLauncher.launch(serviceManager.requiredPermissions)
        } else {
            serviceManager.startLocationService()
        }

        LocationService.onLocationUpdate = { latitude, longitude, locationName, accuracy ->
            locationViewModel.updateLocation(GeoPoint(latitude, longitude), locationName, accuracy)
        }

        val webServicesProvider = WebServicesProvider("ws://${webSocketIp.value}:80")
        lifecycleScope.launch {
            webServicesProvider.startSocket()
        }
        lifecycleScope.launch {
            for (socketUpdate in webServicesProvider.socketEventChannel) {
                socketUpdate.text?.let { jsonData ->
                    statisticsViewModel.updateGnssOutput(parseGnssJson(jsonData))
                }
            }
        }

        setContent {
            GNSSTrackingAppTheme {
                val navHostController = rememberNavController()
                // location data
                val locationData = locationViewModel.locationData.collectAsState().value

                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        // Add the markers
                        AndroidView(
                            factory = { context ->
                                MapView(context).apply {
                                    setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { mapView ->
                            // Add predefined locations to map
                            locationViewModel.getAllLocations().forEach { location ->
                                addMarkerToMap(mapView, location, isCurrentLocation = false)
                            }

                            // If locationData changes, add new marker for current location
                            locationData?.let { data ->
                                addMarkerToMap(mapView, data, isCurrentLocation = true)
                            }
                        }
                    }
                    Scaffold(bottomBar = {
                        NavigationBarComponent(navHostController)
                    }, content = { padding ->
                        Column(Modifier.padding(padding)) {
                            MainNavigation(
                                navHostController,
                                mapViewModel,
                                locationViewModel,
                                statisticsViewModel,
                                settingsViewModel,
                                webServicesProvider
                            )
                        }
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceManager.stopLocationService()
        webServicesProvider.stopSocket()
    }

    // method to add markers to map
    private fun addMarkerToMap(mapView: MapView, location: LocationData, isCurrentLocation: Boolean) {
        val marker = Marker(mapView).apply {
            position = location.location
            title = location.locationName
            snippet = location.message

            // another icon for the recent location or the standard icon for other locations
            val iconResource = if (isCurrentLocation) {
                android.R.drawable.star_on
            } else {
                android.R.drawable.ic_menu_mapmode
            }
            icon = ContextCompat.getDrawable(this@MainActivity, iconResource)
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    // method to define two more locations
    private fun addPredefinedLocations() {
        locationViewModel.addLocation(
            GeoPoint(48.9, 8.9),
            "Point 2",
            0.0f,
            ""
        )
        locationViewModel.addLocation(
            GeoPoint(48.8, 8.8),
            "Point 3",
            0.0f,
            "Injured Person"
        )
    }

}
