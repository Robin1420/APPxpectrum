package com.example.xpectrum

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

// Modelo de pasajero
// Ajusta los campos según la respuesta real de la API
 data class Pasajero(
    val nombre: String,
    val apellido: String,
    val documento: String?,
    val asiento: String?
)

// Función suspendida para obtener pasajeros por código de vuelo
suspend fun obtenerPasajerosPorCodigoVuelo(codigoVuelo: String): List<Pasajero> = withContext(Dispatchers.IO) {
    val url = URL("http://www.apiswagger.somee.com/api/vuelos/ObtenerPasajerosPorCodigoVuelo?codigoVuelo=$codigoVuelo")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    val pasajeros = mutableListOf<Pasajero>()
    try {
        val response = connection.inputStream.bufferedReader().readText()
        Log.d("PASAJEROS_API", "Respuesta cruda: $response")
        val array = try { JSONArray(response) } catch (e: Exception) {
            val obj = JSONObject(response)
            obj.optJSONArray("pasajeros") ?: JSONArray()
        }
        for (i in 0 until array.length()) {
            val p = array.getJSONObject(i)
            pasajeros.add(
                Pasajero(
                    nombre = p.optString("nombre"),
                    apellido = p.optString("apellido"),
                    documento = p.optString("documento"),
                    asiento = p.optString("asiento")
                )
            )
        }
    } catch (e: Exception) {
        Log.e("PASAJEROS_API", "Error general: ${e.message}")
    } finally {
        connection.disconnect()
    }
    pasajeros
}
