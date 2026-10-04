package com.example.invernaderoiot

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.client.mqttv3.*
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var clienteMqtt: MqttAsyncClient
    private lateinit var db: DatabaseHelper

    private lateinit var textoHumedad: TextView
    private lateinit var textoEstadoBomba: TextView
    private lateinit var lblValorBarra: TextView
    private lateinit var textoHistorialBD: TextView
    private lateinit var barraHumedad: SeekBar

    private val brokerUri = "tcp://broker.hivemq.com:1883"
    private val topicHumedad = "inacap/invernadero/humedad"
    private val topicControl = "inacap/invernadero/control"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)

        textoHumedad = findViewById(R.id.textoHumedad)
        textoEstadoBomba = findViewById(R.id.textoEstadoBomba)
        lblValorBarra = findViewById(R.id.lblValorBarra)
        textoHistorialBD = findViewById(R.id.textoHistorialBD)
        barraHumedad = findViewById(R.id.barraHumedad)

        val botonEncender = findViewById<Button>(R.id.botonEncender)
        val botonApagar = findViewById<Button>(R.id.botonApagar)
        val botonConsultarBD = findViewById<Button>(R.id.botonConsultarBD)

        conectarMqtt()

        // Control manual desde los botones de la app
        botonEncender.setOnClickListener {
            publicarMensaje(topicControl, "1")
            db.guardarLecturaSensor("Control Manual: Bomba Encendida")
        }

        botonApagar.setOnClickListener {
            publicarMensaje(topicControl, "0")
            db.guardarLecturaSensor("Control Manual: Bomba Apagada")
        }

        // Consulta de la base de datos local SQLite (ISO 27400)
        botonConsultarBD.setOnClickListener {
            val ultimo = db.obtenerUltimoRegistro()
            textoHistorialBD.text = "Último registro en BD: $ultimo"
        }

        // Emulador de sensor desde la app (para pruebas entre 2 celulares)
        barraHumedad.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lblValorBarra.text = "Transmitiendo valor: $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val valor = seekBar?.progress.toString()
                publicarMensaje(topicHumedad, valor)
                db.guardarLecturaSensor("Humedad simulada móvil: $valor")
            }
        })
    }

    private fun conectarMqtt() {
        val clientId = "Android_" + UUID.randomUUID().toString().substring(0, 6)
        clienteMqtt = MqttAsyncClient(brokerUri, clientId, null)

        val options = MqttConnectOptions().apply {
            isCleanSession = true
        }

        clienteMqtt.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                clienteMqtt.subscribe(topicHumedad, 0)
                clienteMqtt.subscribe(topicControl, 0)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                runOnUiThread {
                    textoEstadoBomba.text = "Error de conexión con Broker MQTT"
                }
            }
        })

        clienteMqtt.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val valor = message?.toString() ?: ""
                runOnUiThread {
                    // Actualización de Humedad recibida (desde ESP32 o segundo celular)
                    if (topic == topicHumedad) {
                        textoHumedad.text = "Humedad actual: $valor"
                        db.guardarLecturaSensor("Telemetría sensor: $valor")
                    }
                    // Actualización de estado de Bomba
                    else if (topic == topicControl) {
                        when (valor) {
                            "1_AUTO" -> {
                                textoEstadoBomba.text = "Estado Bomba: ENCENDIDA (Automático)"
                                db.guardarLecturaSensor("Auto: Bomba Activada por sequedad")
                            }
                            "0_AUTO" -> {
                                textoEstadoBomba.text = "Estado Bomba: APAGADA (Automático)"
                                db.guardarLecturaSensor("Auto: Bomba Apagada por humedad óptima")
                            }
                            "1" -> {
                                textoEstadoBomba.text = "Estado Bomba: ENCENDIDA (Manual)"
                            }
                            "0" -> {
                                textoEstadoBomba.text = "Estado Bomba: APAGADA (Manual)"
                            }
                        }
                    }
                }
            }

            override fun connectionLost(cause: Throwable?) {}
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
    }

    private fun publicarMensaje(topic: String, mensaje: String) {
        if (clienteMqtt.isConnected) {
            val mqttMessage = MqttMessage(mensaje.toByteArray()).apply { qos = 0 }
            clienteMqtt.publish(topic, mqttMessage)
        }
    }
}