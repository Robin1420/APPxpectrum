package com.example.xpectrum

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

// Modelo de vuelo que contiene solo los campos que queremos mostrar en la UI
// Cada propiedad representa un dato relevante del vuelo
 data class Vuelo(
    val codigoVuelo: String,         // Código identificador del vuelo (ej: LH401)
    val fechaSalida: String,         // Fecha de salida del vuelo
    val horaSalida: String,          // Hora de salida del vuelo
    val fechaLlegada: String,        // Fecha de llegada del vuelo
    val horaLlegada: String,         // Hora de llegada del vuelo
    val aeropuertoOrigen: String,    // Nombre del aeropuerto de origen
    val paisOrigen: String,          // País de origen
    val estadoVuelo: String          // Estado del vuelo (ej: Disponible, Demorado)
)

// Función suspendida que obtiene la lista de vuelos desde la API externa
// Se ejecuta en un contexto de IO para no bloquear el hilo principal

suspend fun obtenerVuelos(): List<Vuelo> = withContext(Dispatchers.IO) {
    val url = URL("http://www.apiswagger.somee.com/api/vuelos/getvuelos")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    val vuelos = mutableListOf<Vuelo>()
    try {
        val response = connection.inputStream.bufferedReader().readText()
        Log.d("VUELOS_API", "Respuesta cruda: $response") // Log para debug
        try {
            // Intentamos parsear como array
            val vuelosArray = JSONArray(response)
            for (i in 0 until vuelosArray.length()) {
                val vueloObj = vuelosArray.getJSONObject(i)
                vuelos.add(
                    Vuelo(
                        codigoVuelo = vueloObj.optString("codigoVuelo"),
                        fechaSalida = vueloObj.optString("fechaSalida"),
                        horaSalida = vueloObj.optString("horaSalida"),
                        fechaLlegada = vueloObj.optString("fechaLlegada"),
                        horaLlegada = vueloObj.optString("horaLlegada"),
                        aeropuertoOrigen = vueloObj.optString("aeropuertoOrigen"),
                        paisOrigen = vueloObj.optString("paisOrigen"),
                        estadoVuelo = vueloObj.optString("estadoVuelo")
                    )
                )
            }
        } catch (e: Exception) {
            // Si falla, intentamos parsear como objeto con propiedad de lista
            try {
                val obj = JSONObject(response)
                // Cambia "vuelos" por el nombre real de la propiedad si es distinto
                val vuelosArray = obj.optJSONArray("vuelos") ?: JSONArray()
                for (i in 0 until vuelosArray.length()) {
                    val vueloObj = vuelosArray.getJSONObject(i)
                    vuelos.add(
                        Vuelo(
                            codigoVuelo = vueloObj.optString("codigoVuelo"),
                            fechaSalida = vueloObj.optString("fechaSalida"),
                            horaSalida = vueloObj.optString("horaSalida"),
                            fechaLlegada = vueloObj.optString("fechaLlegada"),
                            horaLlegada = vueloObj.optString("horaLlegada"),
                            aeropuertoOrigen = vueloObj.optString("aeropuertoOrigen"),
                            paisOrigen = vueloObj.optString("paisOrigen"),
                            estadoVuelo = vueloObj.optString("estadoVuelo")
                        )
                    )
                }
            } catch (ex: Exception) {
                Log.e("VUELOS_API", "Error parseando la respuesta: ${ex.message}")
            }
        }
    } catch (e: Exception) {
        Log.e("VUELOS_API", "Error general: ${e.message}")
    } finally {
        connection.disconnect()
    }
    vuelos
}

