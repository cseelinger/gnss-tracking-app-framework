package de.hhn.gnsstrackingapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import de.hhn.gnsstrackingapp.R
import de.hhn.gnsstrackingapp.ui.theme.Purple40

@Composable
fun NavigationBarComponent(
    navController: NavController,
) {
    val selectedItem = remember { mutableIntStateOf(0) }
    val navigationItems = listOf(
        NavigationItem(
            label = stringResource(R.string.map),
            screen = Screen.MapScreen,
            selectedIcon = Icons.Filled.LocationOn,
            unselectedIcon = Icons.Outlined.LocationOn
        ), NavigationItem(
            label = stringResource(R.string.statistics),
            screen = Screen.StatisticsScreen,
            selectedIcon = Icons.Filled.Info,
            unselectedIcon = Icons.Outlined.Info
        ), NavigationItem(
            label = stringResource(R.string.settings),
            screen = Screen.SettingsScreen,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    NavigationBar {
        navigationItems.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selectedItem.intValue == index) item.selectedIcon
                        else item.unselectedIcon, contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = selectedItem.intValue == index,
                onClick = {
                    selectedItem.intValue = index
                    navController.navigate(item.screen.route)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White, indicatorColor = Purple40
                )
            )
        }
    }
}