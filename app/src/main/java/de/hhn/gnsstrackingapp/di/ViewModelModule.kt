package de.hhn.gnsstrackingapp.di

import de.hhn.gnsstrackingapp.ui.screens.map.LocationViewModel
import de.hhn.gnsstrackingapp.ui.screens.map.MapViewModel
import de.hhn.gnsstrackingapp.ui.screens.settings.SettingsViewModel
import de.hhn.gnsstrackingapp.ui.screens.statistics.StatisticsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val viewModelModule = module {
    viewModel { MapViewModel(androidContext()) }
    viewModel { LocationViewModel() }
    viewModel { SettingsViewModel() }
    viewModel { StatisticsViewModel() }
}
