package com.example.xpectrum

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

// Modelo para la información del boleto
 data class TicketInfo(
    val nombre: String,
    val email: String,
    val telefono: String,
    val codigoVuelo: String,
    val fechaReserva: String?,
    val fechaSalida: String?,
    val horaSalida: String?,
    val fechaLlegada: String?,
    val horaLlegada: String?,
    val precioUSD: Double?,
    val precioPEN: Double?,
    val tipoPago: String?
)

// Función suspendida para obtener información del boleto por código de vuelo
suspend fun obtenerTicketInfoPorCodigoVuelo(codigoVuelo: String): TicketInfo? = withContext(Dispatchers.IO) {
    val url = URL("http://www.apiswagger.somee.com/api/vuelos/ObtenerPasajerosPorCodigoVuelo?codigoVuelo=$codigoVuelo")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    try {
        val responseCode = connection.responseCode
        Log.d("TICKET_API", "Código de respuesta HTTP: $responseCode")
        if (responseCode != 200) {
            Log.e("TICKET_API", "Respuesta HTTP no exitosa: $responseCode")
            return@withContext null
        }
        val response = connection.inputStream.bufferedReader().readText()
        Log.d("TICKET_API", "Respuesta cruda: $response")
        try {
            // Si la respuesta es un objeto (caso correcto)
            if (response.trim().startsWith("{")) {
                val obj = JSONObject(response)
                return@withContext TicketInfo(
                    nombre = obj.optString("nombre"),
                    email = obj.optString("email"),
                    telefono = obj.optString("telefono"),
                    codigoVuelo = obj.optString("codigoVuelo"),
                    fechaSalida = obj.optString("fechaSalida"),
                    horaSalida = obj.optString("horaSalida"),
                    fechaLlegada = obj.optString("fechaLlegada"),
                    horaLlegada = obj.optString("horaLlegada"),
                    precioUSD = obj.optDouble("precioUSD"),
                    precioPEN = obj.optDouble("precioPEN"),
                    tipoPago = obj.optString("tipoPago"),
                    fechaReserva = obj.optString("fechaReserva")
                )
            } else if (response.trim().startsWith("[")) {
                // Si por error la API devuelve un array, busca el objeto correcto
                val arr = org.json.JSONArray(response)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("codigoVuelo") == codigoVuelo) {
                        return@withContext TicketInfo(
                            nombre = obj.optString("nombre"),
                            email = obj.optString("email"),
                            telefono = obj.optString("telefono"),
                            codigoVuelo = obj.optString("codigoVuelo"),
                            fechaSalida = obj.optString("fechaSalida"),
                            horaSalida = obj.optString("horaSalida"),
                            fechaLlegada = obj.optString("fechaLlegada"),
                            horaLlegada = obj.optString("horaLlegada"),
                            precioUSD = obj.optDouble("precioUSD"),
                            precioPEN = obj.optDouble("precioPEN"),
                            tipoPago = obj.optString("tipoPago"),
                            fechaReserva = obj.optString("fechaReserva")
                        )
                    }
                }
                Log.e("TICKET_API", "No se encontró el vuelo en el array para el código: $codigoVuelo")
                return@withContext null
            } else {
                Log.e("TICKET_API", "Respuesta inesperada: $response")
                return@withContext null
            }
        } catch (ex: Exception) {
            Log.e("TICKET_API", "Error de parseo JSON: ${ex.message}")
            return@withContext null
        }
    } catch (e: Exception) {
        Log.e("TICKET_API", "Error general: ${e.message}")
        null
    } finally {
        connection.disconnect()
    }
}
