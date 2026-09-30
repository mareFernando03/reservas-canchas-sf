package ar.edu.utn.frsf.canchas

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Elegir día, hora y duración del turno. Al confirmar devuelve el turno a la pantalla
 * anterior (setResult) y ofrece compartirlo por otra app (Intent implícito ACTION_SEND).
 */
class ReservaActivity : CicloDeVidaActivity() {

    private lateinit var cancha: Cancha

    // La duración no la guarda ninguna vista, así que si la pantalla se recrea
    // (por ejemplo al rotar) se perdería. Por eso se guarda en onSaveInstanceState.
    private var horas = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserva)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        cancha = Canchas.porId(intent.getIntExtra(DetalleCanchaActivity.EXTRA_CANCHA_ID, -1)) ?: run {
            finish()
            return
        }
        horas = savedInstanceState?.getInt(ESTADO_HORAS, 1) ?: 1

        title = getString(R.string.reservar_titulo, cancha.nombre)

        val formato = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.forLanguageTag("es-AR"))
        val dias = (0L..6L).map { LocalDate.now().plusDays(it).format(formato) }
        val spinnerDia = findViewById<Spinner>(R.id.spinnerDia)
        spinnerDia.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dias)

        val horarios = (17..23).map { "%02d:00".format(it) }
        val spinnerHora = findViewById<Spinner>(R.id.spinnerHora)
        spinnerHora.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, horarios)

        findViewById<Button>(R.id.botonMenos).setOnClickListener { cambiarHoras(-1) }
        findViewById<Button>(R.id.botonMas).setOnClickListener { cambiarHoras(+1) }
        mostrarDuracion()

        findViewById<Button>(R.id.botonConfirmar).setOnClickListener {
            val turno = "${spinnerDia.selectedItem} ${spinnerHora.selectedItem} ($horas h)"
            Medicion.evento(this, Medicion.CONFIRMAR_RESERVA, "cancha" to cancha.nombre, "horas" to horas.toLong())

            val compartir = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.mensaje_compartir, cancha.nombre, cancha.club, turno))
            }
            startActivity(Intent.createChooser(compartir, getString(R.string.compartir_con)))

            setResult(RESULT_OK, Intent().putExtra(EXTRA_TURNO, turno))
            finish()
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
