package ar.edu.utn.frsf.canchas

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.Executors

/**
 * Pronóstico por hora de Open-Meteo (https://open-meteo.com), una API REST pública que no pide
 * API key. Sirve para saber si va a llover a la hora del turno en una cancha al aire libre.
 *
 * La red no se puede usar desde el hilo principal (Android tira NetworkOnMainThreadException),
 * así que el pedido corre en un hilo aparte y el resultado vuelve al hilo principal para
 * poder tocar las vistas.
 */
object Clima {

    data class Pronostico(val temperatura: Double, val probabilidadLluvia: Int)

    private val hilo = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())

    /** Llama a [listo] con el pronóstico de esa fecha y hora, o con null si no se pudo obtener. */
    fun pronostico(latitud: Double, longitud: Double, fecha: LocalDate, hora: Int, listo: (Pronostico?) -> Unit) {
        hilo.execute {
            val resultado = try {
                consultar(latitud, longitud, fecha, hora)
            } catch (e: Exception) {
                null // sin conexión, timeout o respuesta inesperada: la pantalla sigue sin el dato
            }
            principal.post { listo(resultado) }
        }
    }

    private fun consultar(latitud: Double, longitud: Double, fecha: LocalDate, hora: Int): Pronostico? {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitud&longitude=$longitud" +
                "&hourly=temperature_2m,precipitation_probability" +
                "&timezone=America%2FArgentina%2FCordoba&start_date=$fecha&end_date=$fecha"
        )
        val conexion = url.openConnection() as HttpURLConnection
        conexion.connectTimeout = 5000
        conexion.readTimeout = 5000
        try {
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(conexion.inputStream.bufferedReader().readText())
            val horario = json.getJSONObject("hourly")
            // Las horas vienen como "2026-10-21T19:00", una por posición en cada arreglo.
            val horas = horario.getJSONArray("time")
            val buscada = "%sT%02d:00".format(fecha, hora)
            for (i in 0 until horas.length()) {
                if (horas.getString(i) == buscada) {
                    return Pronostico(
                        temperatura = horario.getJSONArray("temperature_2m").getDouble(i),
                        probabilidadLluvia = horario.getJSONArray("precipitation_probability").getInt(i),
                    )
                }
            }
            return null
        } finally {
            conexion.disconnect()
        }
    }
}
