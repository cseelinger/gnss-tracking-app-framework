package de.hhn.gnsstrackingapp.ui.screens.map

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class MapViewModel(private val context: Context) : ViewModel() {
    var zoomLevel: Double = 12.0
    var centerLocation = mutableStateOf(GeoPoint(48.947410, 9.144216))
    var mapOrientation: Float = 0f
    var isAnimating = mutableStateOf(false)

    private val brokerUrl = "tcp://eu1.cloud.thethings.network:1883"
    private val clientId = MqttClient.generateClientId()
    private val topic = "v3/test-ttn-celina/devices/my-esp-ttn/up"
    private val apiKey = "NNSXS.PPIXT2VSCX5IJ2U2RXME6U4QNFPLWOB5XBTFWFI.RUACTCTCZBGLMQVEDNPT4IBVQAPYNJBK4OZFQDTQZRKEXIL4EF6A"

    //private lateinit var mqttClient: MqttAndroidClient
    private var mqttClient = MqttAndroidClient(context, brokerUrl, clientId)

    init {
        connectToMqttBroker()
    }

    private fun connectToMqttBroker() {
        mqttClient = MqttAndroidClient(
            context, // Ersetze App.context durch den richtigen Kontext
            brokerUrl,
            clientId
        )

        val options = MqttConnectOptions().apply {
            userName = "test-ttn-celina"
            password = apiKey.toCharArray()
        }

        try {
            mqttClient.connect(options, null, object : org.eclipse.paho.client.mqttv3.IMqttActionListener {
                override fun onSuccess(asyncActionToken: org.eclipse.paho.client.mqttv3.IMqttToken?) {
                    mqttClient.setCallback(object : MqttCallback {
                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            message?.let { handleIncomingMessage(String(it.payload)) }
                        }

                        override fun connectionLost(cause: Throwable?) {}
                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                    })
                    mqttClient.subscribe(topic, 1)
                }

                override fun onFailure(asyncActionToken: org.eclipse.paho.client.mqttv3.IMqttToken?, exception: Throwable?) {
                    exception?.printStackTrace()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleIncomingMessage(payload: String) {
        // Beispiel-Payload parsen
        /*
          {
            "uplink_message": {
              "decoded_payload": {
                "latitude": 48.123456,
                "longitude": 9.654321
              }
            }
          }
        */
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val json = org.json.JSONObject(payload)
                val uplinkMessage = json.getJSONObject("uplink_message")
                val decodedPayload = uplinkMessage.getJSONObject("decoded_payload")
                val latitude = decodedPayload.getDouble("latitude")
                val longitude = decodedPayload.getDouble("longitude")
                centerLocation.value = GeoPoint(latitude, longitude)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
