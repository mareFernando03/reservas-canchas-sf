package ar.edu.utn.frsf.canchas

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * TP6: estrategias de retención con notificaciones.
 * - Locales (no necesitan servidor): recordatorio del turno y recordatorio por inactividad.
 * - Push (Firebase Cloud Messaging): campañas segmentadas por tema desde la consola de Firebase.
 *
 * Los recordatorios se programan con WorkManager, que los dispara aunque la app esté cerrada
 * o el teléfono se haya reiniciado.
 */
object Notificaciones {
    const val CANAL_RECORDATORIOS = "recordatorios"
    const val CANAL_NOVEDADES = "novedades"
    const val EXTRA_ORIGEN = "origen"
    private const val TAG = "Notificaciones"
    private const val TRABAJO_INACTIVIDAD = "recordatorio_inactividad"

    // Con -PrecordatorioPrueba=true los recordatorios salen a los pocos segundos, para probarlos.
    private val anticipacionTurno = if (BuildConfig.RECORDATORIO_PRUEBA) null else Duration.ofHours(2)

    // Los turnos son en hora de San Francisco, aunque el teléfono esté configurado en otra zona.
    private val zona = ZoneId.of("America/Argentina/Cordoba")

    /** A los 7 días, pero a las 19: el mensaje invita a jugar "esta noche" y no tiene que llegar de madrugada. */
    private fun esperaInactividad(): Duration =
        if (BuildConfig.RECORDATORIO_PRUEBA) Duration.ofSeconds(20)
        else Duration.between(LocalDateTime.now(zona), LocalDate.now(zona).plusDays(7).atTime(19, 0))

    /** Desde Android 8 cada notificación pertenece a un canal, que el usuario puede silenciar por separado. */
    fun crearCanales(context: Context) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_RECORDATORIOS, context.getString(R.string.canal_recordatorios), NotificationManager.IMPORTANCE_HIGH)
        )
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_NOVEDADES, context.getString(R.string.canal_novedades), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Desde Android 13 hay que pedir permiso para mostrar notificaciones (antes no hacía falta). */
    fun hayPermiso(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun mostrar(context: Context, canal: String, id: Int, titulo: String, texto: String, origen: String) {
        if (!hayPermiso(context)) return
        // Tocar la notificación abre la app y le avisa de dónde vino, para medirlo en Analytics.
        // CLEAR_TASK: si la app ya estaba abierta, arranca de nuevo en vez de apilar otra lista encima.
        val abrir = Intent(context, LoginActivity::class.java).putExtra(EXTRA_ORIGEN, origen)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendiente = PendingIntent.getActivity(context, id, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notificacion)
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso para notificar", e)
        }
    }

    /** Recordatorio local dos horas antes del turno. */
    fun programarRecordatorioTurno(context: Context, cancha: Cancha, fecha: LocalDate, hora: Int) {
        val turno = LocalDateTime.of(fecha, java.time.LocalTime.of(hora, 0))
        val espera = anticipacionTurno?.let { Duration.between(LocalDateTime.now(zona), turno.minus(it)) } ?: Duration.ofSeconds(10)
        if (espera.isNegative) return // el turno es dentro de menos de dos horas: no tiene sentido recordarlo
        val datos = workDataOf(
            "titulo" to context.getString(R.string.recordatorio_turno_titulo),
            "texto" to context.getString(R.string.recordatorio_turno_texto, cancha.nombre, cancha.club, hora),
            "id" to cancha.id * 100 + hora,
            "origen" to "recordatorio_turno",
            "canal" to CANAL_RECORDATORIOS,
        )
        // Trabajo único por turno: si se reserva dos veces el mismo, queda un solo recordatorio.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "turno_${cancha.id}_${fecha}_$hora", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RecordatorioWorker>().setInitialDelay(espera).setInputData(datos).build()
        )
        Log.d(TAG, "Recordatorio del turno en $espera")
    }

    /**
     * Recordatorio por inactividad, como propone el libro: se programa cuando el usuario deja la app
     * y se cancela cuando vuelve. Sólo llega si pasan 7 días sin abrirla.
     */
    fun programarRecordatorioInactividad(context: Context) {
        val datos = workDataOf(
            "titulo" to context.getString(R.string.inactividad_titulo),
            "texto" to context.getString(R.string.inactividad_texto),
            "id" to 1,
            "origen" to "recordatorio_inactividad",
            // No avisa de un turno, invita a volver: va con las novedades, que se pueden silenciar.
            "canal" to CANAL_NOVEDADES,
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            TRABAJO_INACTIVIDAD, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RecordatorioWorker>().setInitialDelay(esperaInactividad()).setInputData(datos).build()
        )
    }

    fun cancelarRecordatorioInactividad(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_INACTIVIDAD)
    }

    /**
     * Segmentación por temas de FCM: todos reciben "novedades" y, al reservar, cada usuario se suma
     * al tema de su deporte, para que una promo de pádel no le llegue a quien sólo juega al fútbol.
     */
    fun suscribirATemas(deporte: String? = null) {
        val mensajeria = FirebaseMessaging.getInstance()
        mensajeria.subscribeToTopic("novedades")
        if (deporte != null) mensajeria.subscribeToTopic(temaDeporte(deporte))
        mensajeria.token.addOnSuccessListener { Log.d(TAG, "Token FCM de este dispositivo: $it") }
    }

    /** "Fútbol 5" -> "deporte_futbol5". FCM sólo admite [a-zA-Z0-9-_.~%] en el nombre del tema: se sacan acentos y espacios. */
    fun temaDeporte(deporte: String) = "deporte_" + java.text.Normalizer.normalize(deporte, java.text.Normalizer.Form.NFD)
        .replace(Regex("[^A-Za-z0-9]"), "").lowercase()
}

/** Muestra un recordatorio programado. WorkManager lo ejecuta cuando se cumple la espera. */
class RecordatorioWorker(context: Context, parametros: WorkerParameters) : Worker(context, parametros) {
    override fun doWork(): Result {
        Notificaciones.mostrar(
            applicationContext, inputData.getString("canal") ?: Notificaciones.CANAL_RECORDATORIOS,
            inputData.getInt("id", 0), inputData.getString("titulo") ?: "", inputData.getString("texto") ?: "",
            inputData.getString("origen") ?: "recordatorio",
        )
        return Result.success()
    }
}

/**
 * Recibe los mensajes push de Firebase Cloud Messaging. Si la app está en segundo plano, Android
 * muestra la notificación solo; este método se ejecuta cuando la app está abierta.
 */
class MensajesPush : FirebaseMessagingService() {
    override fun onMessageReceived(mensaje: RemoteMessage) {
        val notificacion = mensaje.notification ?: return
        Notificaciones.mostrar(
            this, Notificaciones.CANAL_NOVEDADES, mensaje.messageId.hashCode(),
            notificacion.title ?: getString(R.string.app_name), notificacion.body ?: "", "push",
        )
    }

    // El token identifica a este dispositivo; cambia si se reinstala la app.
    override fun onNewToken(token: String) {
        Log.d("Notificaciones", "Token FCM nuevo: $token")
    }
}
