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


class MainActivity : ComponentActivity() {
    private lateinit var serviceManager: ServiceManager
    private lateinit var webServicesProvider: WebServicesProvider

    private val mapViewModel: MapViewModel by viewModel()
    private val locationViewModel: LocationViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val statisticsViewModel: StatisticsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        LocationService.onLocationUpdateList = { latitude, longitude, locationName, accuracy, message ->
            locationViewModel.addLocation(GeoPoint(latitude, longitude), locationName, accuracy, message)
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

                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
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
}
