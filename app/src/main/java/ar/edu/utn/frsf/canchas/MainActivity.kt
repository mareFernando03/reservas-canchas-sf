package ar.edu.utn.frsf.canchas

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView

/** Lista de canchas disponibles. Tocar una abre su detalle. */
class MainActivity : CicloDeVidaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lista = findViewById<ListView>(R.id.listaCanchas)
        val filas = Canchas.todas.map { "${it.nombre}\n${it.club} · $${it.precioHora}/hora" }
        lista.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filas)

        lista.setOnItemClickListener { _, _, posicion, _ ->
            // Intent explícito: indicamos la clase exacta de la Activity a abrir
            // y le pasamos el id de la cancha como "extra".
            val intent = Intent(this, DetalleCanchaActivity::class.java)
            intent.putExtra(DetalleCanchaActivity.EXTRA_CANCHA_ID, Canchas.todas[posicion].id)
            startActivity(intent)
        }
    }
}
