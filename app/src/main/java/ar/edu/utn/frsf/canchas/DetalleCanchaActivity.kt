package ar.edu.utn.frsf.canchas

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

/** Datos de una cancha y las acciones sobre ella: llamar, ver en el mapa y reservar. */
class DetalleCanchaActivity : CicloDeVidaActivity() {

    private lateinit var cancha: Cancha

    // Abre ReservaActivity y recibe de vuelta el turno elegido (Intent con resultado).
    private val reservar = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val turno = resultado.data?.getStringExtra(ReservaActivity.EXTRA_TURNO)
            findViewById<TextView>(R.id.textoUltimaReserva).text = getString(R.string.ultima_reserva, turno)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_cancha)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val id = intent.getIntExtra(EXTRA_CANCHA_ID, -1)
        cancha = Canchas.porId(id) ?: run {
            finish()
            return
        }

        title = cancha.nombre
        findViewById<TextView>(R.id.textoClub).text = cancha.club
        findViewById<TextView>(R.id.textoDeporte).text = cancha.deporte
        findViewById<TextView>(R.id.textoDireccion).text = cancha.direccion
        findViewById<TextView>(R.id.textoPrecio).text = getString(R.string.precio_hora, cancha.precioHora)

        // Sólo la primera vez: al rotar la pantalla la Activity se recrea y contaría doble.
        if (savedInstanceState == null) {
            Medicion.evento(this, Medicion.VER_CANCHA, "cancha" to cancha.nombre)
        }

        // Intent implícito: no decimos qué app abrir, sólo la acción. Android elige el marcador.
        findViewById<Button>(R.id.botonLlamar).setOnClickListener {
            Medicion.evento(this, Medicion.LLAMAR_CLUB, "cancha" to cancha.nombre)
            abrir(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cancha.telefono}")))
        }

        // Intent implícito con esquema geo: lo atiende la app de mapas que haya instalada.
        findViewById<Button>(R.id.botonMapa).setOnClickListener {
            Medicion.evento(this, Medicion.VER_MAPA, "cancha" to cancha.nombre)
            val uri = Uri.parse("geo:${cancha.latitud},${cancha.longitud}?q=${Uri.encode(cancha.direccion + ", San Francisco, Córdoba")}")
            abrir(Intent(Intent.ACTION_VIEW, uri))
        }

        findViewById<Button>(R.id.botonReservar).setOnClickListener {
            Medicion.evento(this, Medicion.INICIAR_RESERVA, "cancha" to cancha.nombre)
            val intent = Intent(this, ReservaActivity::class.java)
            intent.putExtra(EXTRA_CANCHA_ID, cancha.id)
            reservar.launch(intent)
        }
    }

    private fun abrir(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_app_para_accion, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_CANCHA_ID = "cancha_id"
    }
}
