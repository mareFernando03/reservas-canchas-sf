package ar.edu.utn.frsf.canchas

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Elegir día, hora y duración del turno. Al confirmar devuelve el turno a la pantalla
 * anterior (setResult) y ofrece compartirlo por otra app (Intent implícito ACTION_SEND).
 */
class ReservaActivity : CicloDeVidaActivity() {

    private lateinit var cancha: Cancha

    // La duración no la guarda ninguna vista, así que si la pantalla se recrea
    // (por ejemplo al rotar) se perdería. Por eso se guarda en onSaveInstanceState.
    private var horas = 1

    // El día de la grilla se calcula una sola vez: si la pantalla queda abierta pasada la medianoche,
    // el turno elegido sigue siendo el que se ve en pantalla.
    private val hoy = LocalDate.now()

    private val pedirPermisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserva)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        cancha = Canchas.porId(intent.getIntExtra(DetalleCanchaActivity.EXTRA_CANCHA_ID, -1)) ?: run {
            finish()
            return
        }
        horas = savedInstanceState?.getInt(ESTADO_HORAS, 1) ?: 1

        // TP6: el permiso de notificaciones se pide acá, donde se entiende para qué es (el recordatorio
        // del turno), y no al abrir la app. Si lo rechaza, reservar funciona igual.
        if (savedInstanceState == null && !Notificaciones.hayPermiso(this) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        title = getString(R.string.reservar_titulo, cancha.nombre)

        val formato = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.forLanguageTag("es-AR"))
        val dias = (0L..6L).map { hoy.plusDays(it).format(formato) }
        val spinnerDia = findViewById<Spinner>(R.id.spinnerDia)
        spinnerDia.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dias)

        val horarios = (17..23).map { "%02d:00".format(it) }
        val spinnerHora = findViewById<Spinner>(R.id.spinnerHora)
        spinnerHora.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, horarios)

        // Cada vez que cambia el día o la hora se pide el pronóstico para ese turno (Open-Meteo).
        val alCambiar = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(padre: AdapterView<*>?, vista: View?, posicion: Int, id: Long) =
                mostrarClima(hoy.plusDays(spinnerDia.selectedItemPosition.toLong()),
                    17 + spinnerHora.selectedItemPosition)
            override fun onNothingSelected(padre: AdapterView<*>?) {}
        }
        spinnerDia.onItemSelectedListener = alCambiar
        spinnerHora.onItemSelectedListener = alCambiar

        findViewById<Button>(R.id.botonMenos).setOnClickListener { cambiarHoras(-1) }
        findViewById<Button>(R.id.botonMas).setOnClickListener { cambiarHoras(+1) }
        mostrarDuracion()

        findViewById<Button>(R.id.botonConfirmar).setOnClickListener {
            val turno = "${spinnerDia.selectedItem} ${spinnerHora.selectedItem} ($horas h)"
            Medicion.evento(this, Medicion.CONFIRMAR_RESERVA, "cancha" to cancha.nombre, "horas" to horas.toLong())

            // TP6: recordatorio local antes del partido y suscripción al tema push de su deporte.
            Notificaciones.programarRecordatorioTurno(this, cancha,
                hoy.plusDays(spinnerDia.selectedItemPosition.toLong()), 17 + spinnerHora.selectedItemPosition)
            Notificaciones.suscribirATemas(cancha.deporte)

            val compartir = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.mensaje_compartir, cancha.nombre, cancha.club, turno))
            }
            startActivity(Intent.createChooser(compartir, getString(R.string.compartir_con)))

            setResult(RESULT_OK, Intent().putExtra(EXTRA_TURNO, turno))
            finish()
        }
    }

    private fun mostrarClima(fecha: LocalDate, hora: Int) {
        val texto = findViewById<TextView>(R.id.textoClima)
        texto.setText(R.string.clima_cargando)
        Clima.pronostico(cancha.latitud, cancha.longitud, fecha, hora) { pronostico ->
            // La respuesta puede llegar tarde: si mientras tanto se cerró la pantalla, no se toca nada.
            if (isFinishing || isDestroyed) return@pronostico
            texto.text = if (pronostico == null) getString(R.string.clima_no_disponible)
            else getString(R.string.clima_turno, pronostico.temperatura.roundToInt(), pronostico.probabilidadLluvia)
        }
    }

    private fun cambiarHoras(delta: Int) {
        horas = (horas + delta).coerceIn(1, 3)
        mostrarDuracion()
    }

    private fun mostrarDuracion() {
        findViewById<TextView>(R.id.textoDuracion).text =
            getString(R.string.duracion_total, horas, horas * cancha.precioHora)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ESTADO_HORAS, horas)
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_TURNO = "turno"
        private const val ESTADO_HORAS = "horas"
    }
}
