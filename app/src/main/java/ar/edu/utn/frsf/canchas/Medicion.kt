package ar.edu.utn.frsf.canchas

import android.content.Context
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Eventos de uso que se mandan a Firebase Analytics: la parte "Measure" del ciclo
 * Build-Measure-Learn. Se ven en la consola de Firebase, en Analytics > Eventos
 * (o al instante en DebugView si el dispositivo está en modo depuración).
 */
object Medicion {
    const val VER_CANCHA = "ver_cancha"
    const val LLAMAR_CLUB = "llamar_club"
    const val VER_MAPA = "ver_mapa"
    const val INICIAR_RESERVA = "iniciar_reserva"
    const val CONFIRMAR_RESERVA = "confirmar_reserva"

    fun evento(context: Context, nombre: String, vararg parametros: Pair<String, Any?>) {
        FirebaseAnalytics.getInstance(context).logEvent(nombre, bundleOf(*parametros))
    }
}
