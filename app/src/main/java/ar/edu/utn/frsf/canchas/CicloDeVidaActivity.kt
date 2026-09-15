package ar.edu.utn.frsf.canchas

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Base de todas las pantallas: escribe en Logcat cada callback del ciclo de vida.
 * Filtrar Logcat por el tag "CicloDeVida" para ver el orden en que Android los llama
 * al navegar entre pantallas, rotar el teléfono o mandar la app a segundo plano.
 */
open class CicloDeVidaActivity : AppCompatActivity() {

    private val nombre get() = this::class.java.simpleName

    private fun log(evento: String) = Log.d(TAG, "$nombre.$evento")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log(if (savedInstanceState == null) "onCreate (nueva)" else "onCreate (recreada, con estado guardado)")
    }

    // Cada layout trae su propia Toolbar, que se usa como barra superior de la pantalla.
    // Además, desde Android 15 las apps dibujan detrás de las barras del sistema (edge-to-edge):
    // se corre el contenido para que no quede tapado por la barra de estado ni la de navegación.
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        val raiz: View = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onStart() { super.onStart(); log("onStart") }
    override fun onResume() { super.onResume(); log("onResume") }
    override fun onPause() { log("onPause"); super.onPause() }
    override fun onStop() { log("onStop"); super.onStop() }
    override fun onRestart() { super.onRestart(); log("onRestart") }
    override fun onDestroy() { log("onDestroy"); super.onDestroy() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        log("onSaveInstanceState")
    }

    companion object {
        const val TAG = "CicloDeVida"
    }
}
