package ar.edu.utn.frsf.canchas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth

/**
 * Lista de canchas disponibles. Tocar una abre su detalle. Requiere sesión iniciada.
 * Si el usuario da permiso de ubicación, la lista se ordena de la más cercana a la más lejana.
 */
class MainActivity : CicloDeVidaActivity() {

    private lateinit var lista: ListView

    // Orden en que se muestran las canchas: el de los datos, o por distancia si hay ubicación.
    private var canchas = Canchas.todas
    private var distanciasKm: Map<Int, Float> = emptyMap()

    // Permiso en tiempo de ejecución: Android muestra su diálogo y avisa la respuesta acá.
    // Se piden la precisa y la aproximada juntas; desde Android 12 el usuario elige cuál dar.
    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        ordenarPorCercania()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.subtitle = FirebaseAuth.getInstance().currentUser?.email

        lista = findViewById(R.id.listaCanchas)
        mostrarLista()

        lista.setOnItemClickListener { _, _, posicion, _ ->
            // Intent explícito: indicamos la clase exacta de la Activity a abrir
            // y le pasamos el id de la cancha como "extra".
            val intent = Intent(this, DetalleCanchaActivity::class.java)
            intent.putExtra(DetalleCanchaActivity.EXTRA_CANCHA_ID, canchas[posicion].id)
            startActivity(intent)
        }

        // Sólo se pregunta la primera vez (no al rotar) y si todavía no hay permiso.
        if (hayUbicacionPrecisa()) ordenarPorCercania()
        else if (savedInstanceState == null) pedirPermiso.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

        mostrarPublicidad()

        // TP6: suscripción al tema general de novedades (push).
        Notificaciones.suscribirATemas()
    }

    /** TP4: pide un anuncio para el banner. El SDK se inicializa una sola vez, en CanchasApp. */
    private fun mostrarPublicidad() {
        findViewById<AdView>(R.id.banner).loadAd(AdRequest.Builder().build())
    }

    private fun hayUbicacionPrecisa() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun mostrarLista() {
        val filas = canchas.map { cancha ->
            val distancia = distanciasKm[cancha.id]?.let { " · " + getString(R.string.distancia_km, it) } ?: ""
            "${cancha.nombre}\n${cancha.club} · $${cancha.precioHora}/hora$distancia"
        }
        lista.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filas)
    }

    /**
     * Pide la ubicación actual a Google Play Services y reordena la lista. Sólo con ubicación
     * precisa: la aproximada se corre a propósito 1 a 2 km, lo mismo que separa a las canchas, y
     * el orden saldría mal. Sin permiso, sin ubicación o sin Play Services, la lista queda como está.
     */
    @SuppressLint("MissingPermission")
    private fun ordenarPorCercania() {
        if (!hayUbicacionPrecisa()) return
        LocationServices.getFusedLocationProviderClient(this)
            // Alta precisión deja usar el GPS: es lo que permite probar con "geo fix" en el emulador.
            // En un teléfono real la prioridad balanceada (wifi y red) alcanzaría y gasta menos batería.
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnFailureListener { Log.w(TAG, "No se pudo obtener la ubicación", it) }
            .addOnSuccessListener { ubicacion ->
                if (ubicacion == null || isFinishing) return@addOnSuccessListener
                val resultado = FloatArray(1)
                distanciasKm = Canchas.todas.associate { cancha ->
                    Location.distanceBetween(ubicacion.latitude, ubicacion.longitude,
                        cancha.latitud, cancha.longitud, resultado)
                    cancha.id to resultado[0] / 1000f
                }
                canchas = Canchas.todas.sortedBy { distanciasKm[it.id] }
                mostrarLista()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.cerrarSesion) {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
