package ar.edu.utn.frsf.canchas

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth

/** Lista de canchas disponibles. Tocar una abre su detalle. Requiere sesión iniciada. */
class MainActivity : CicloDeVidaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.subtitle = FirebaseAuth.getInstance().currentUser?.email

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
