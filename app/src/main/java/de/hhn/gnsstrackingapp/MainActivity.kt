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

import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.room.*
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.ItemizedIconOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

class MainActivity : ComponentActivity() {

    private lateinit var mqttClient: MqttAndroidClient
    private lateinit var map: MapView
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Context holen
            val context = LocalContext.current

            // MapView in Compose mit AndroidView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setBuiltInZoomControls(true)
                        setMultiTouchControls(true)
                    }
                }
            )
        }

        initializeMqttClient()
    }


    private fun initializeMqttClient() {
        val serverUri = "tcp://<TTN_SERVER>:1883" // TTN-Broker-URI ersetzen
        val clientId = "AndroidClient"
        mqttClient = MqttAndroidClient(applicationContext, serverUri, clientId)

        try {
            val options = MqttConnectOptions()
            options.userName = "<USERNAME>" // TTN-Application-User
            options.password = "<PASSWORD>".toCharArray()

            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    subscribeToTopic("<TTN_TOPIC>") // Topic für GNSS-Daten
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    exception?.printStackTrace()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopic(topic: String) {
        mqttClient.subscribe(topic, 1, object : IMqttMessageListener {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.payload?.let {
                    val payload = String(it)
                    processGnssMessage(payload)
                }
            }
        })
    }

    private fun processGnssMessage(payload: String) {
        val data = parseJson(payload)
        val position = GeoPoint(data.latitude, data.longitude)
        val message = data.message

        runOnUiThread {
            val marker = Marker(map)
            marker.position = position
            marker.title = "Nachricht"
            marker.snippet = message

            // Verknüpfe die GNSS-Daten mit dem Marker über 'relatedObject'
            marker.relatedObject = data

            // Setze den OnClickListener für den Marker
            /*
            marker.setOnMarkerClickListener { marker ->
                // Sichere Typumwandlung
                val gnssData = marker.relatedObject as? GnssData
                gnssData?.let {
                    // Zeige die Nachricht an, wenn das Objekt vom Typ GnssData ist
                    Toast.makeText(this, "Nachricht: ${it.message}", Toast.LENGTH_LONG).show()
                }
                true // Markieren als "verarbeitet"
            }*/

            map.overlayManager.add(marker)

            // Kamera auf die Position bewegen
            map.controller.setCenter(position)

            // Optional: Speichern in der Datenbank
            //saveMarkerToDatabase(data)
        }
    }

    private fun saveMarkerToDatabase(data: GnssData) {
        CoroutineScope(Dispatchers.IO).launch {
            db.markerDao().insertMarker(MarkerEntity(0, data.latitude, data.longitude, data.message))
        }
    }

    private fun loadMarkersFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val markers = db.markerDao().getAllMarkers()
            runOnUiThread {
                markers.forEach { entity ->
                    val position = GeoPoint(entity.latitude, entity.longitude)
                    val marker = Marker(map)
                    marker.position = position
                    marker.title = "Nachricht"
                    marker.snippet = entity.message
                    marker.relatedObject = GnssData(entity.latitude, entity.longitude, entity.message)
                    map.overlayManager.add(marker)
                }
            }
        }
    }

    private fun parseJson(payload: String): GnssData {
        return try {
            val jsonObject = JSONObject(payload)
            GnssData(
                latitude = jsonObject.getDouble("latitude"),
                longitude = jsonObject.getDouble("longitude"),
                message = jsonObject.getString("message")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            GnssData(0.0, 0.0, "Fehler beim Parsen")
        }
    }

    data class GnssData(val latitude: Double, val longitude: Double, val message: String)

    @Entity(tableName = "markers")
    data class MarkerEntity(
        @PrimaryKey(autoGenerate = true) val id: Int,
        val latitude: Double,
        val longitude: Double,
        val message: String
    )

    @Dao
    interface MarkerDao {
        @Insert
        suspend fun insertMarker(marker: MarkerEntity)

        @Query("SELECT * FROM markers")
        suspend fun getAllMarkers(): List<MarkerEntity>
    }

    @Database(entities = [MarkerEntity::class], version = 1)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun markerDao(): MarkerDao

        companion object {
            @Volatile
            private var INSTANCE: AppDatabase? = null

            fun getDatabase(context: android.content.Context): AppDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "marker_database"
                    ).build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}
/*
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
*/