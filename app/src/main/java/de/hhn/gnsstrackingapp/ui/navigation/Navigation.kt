package de.hhn.gnsstrackingapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.hhn.gnsstrackingapp.network.WebServicesProvider
import de.hhn.gnsstrackingapp.ui.screens.map.LocationViewModel
import de.hhn.gnsstrackingapp.ui.screens.map.MapScreen
import de.hhn.gnsstrackingapp.ui.screens.map.MapViewModel
import de.hhn.gnsstrackingapp.ui.screens.settings.SettingsScreen
import de.hhn.gnsstrackingapp.ui.screens.settings.SettingsViewModel
import de.hhn.gnsstrackingapp.ui.screens.statistics.StatisticsScreen
import de.hhn.gnsstrackingapp.ui.screens.statistics.StatisticsViewModel

@Composable
fun MainNavigation(
    navHostController: NavHostController = rememberNavController(),
    mapViewModel: MapViewModel,
    locationViewModel: LocationViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    webServicesProvider: WebServicesProvider
) {
    NavHost(navController = navHostController, startDestination = Screen.MapScreen.route) {
        composable(Screen.MapScreen.route) { MapScreen(mapViewModel, locationViewModel) }
        composable(Screen.StatisticsScreen.route) {
            StatisticsScreen(
                statisticsViewModel, webServicesProvider
            )
        }
        composable(Screen.SettingsScreen.route) { SettingsScreen(settingsViewModel) }
    }
}
