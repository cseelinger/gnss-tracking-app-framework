package de.hhn.gnsstrackingapp.ui.screens.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import de.hhn.gnsstrackingapp.databinding.FragmentMapBinding
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val locationViewModel: LocationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        // Initialisiere die MapView
        val mapView: MapView = binding.mapView
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)

        // Verwenden von collect, um auf Änderungen von StateFlow zu reagieren
        lifecycleScope.launch {
            locationViewModel.locationData.collect { locationData ->
                // Aktuelle Position des Nutzers auf der Karte
                val userLocation = locationData.location
                mapView.controller.setCenter(userLocation)
                mapView.controller.setZoom(16.0)

                // Füge Marker für alle gespeicherten Locations hinzu
                addMarkers(mapView)
            }
        }

        // Marker für alle Locations hinzufügen
        addMarkers(mapView)

        return binding.root
    }

    private fun addMarkers(mapView: MapView) {
        // Lösche vorherige Marker
        mapView.overlays.clear()

        // Alle Locations aus dem ViewModel holen
        val locations = locationViewModel.getAllLocations()

        // Debug-Ausgabe
        println("Locations size: ${locations.size}")

        // Füge Marker für jede Location hinzu
        for (locationData in locations) {
            println("Adding marker at: ${locationData.location.latitude}, ${locationData.location.longitude}")

            val marker = Marker(mapView)
            marker.position = locationData.location
            marker.title = locationData.locationName
            marker.snippet = locationData.message
            mapView.overlays.add(marker)
        }

        // Aktualisiere die MapView
        mapView.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

