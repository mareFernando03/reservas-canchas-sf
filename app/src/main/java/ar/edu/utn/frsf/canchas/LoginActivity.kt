package ar.edu.utn.frsf.canchas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Registro e inicio de sesión con email y contraseña (Firebase Authentication).
 * Es la pantalla de inicio: si ya hay una sesión abierta pasa directo a la lista de canchas.
 */
class LoginActivity : CicloDeVidaActivity() {

    private val auth = FirebaseAuth.getInstance().apply {
        // 10.0.2.2 es la máquina de desarrollo vista desde el emulador de Android.
        if (BuildConfig.AUTH_EMULATOR) useEmulator("10.0.2.2", 9099)
    }

    private lateinit var campoEmail: EditText
    private lateinit var campoClave: EditText
    private lateinit var textoError: TextView
    private lateinit var botones: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase recuerda la sesión entre aperturas de la app: no hace falta volver a loguearse.
        if (auth.currentUser != null) {
            irALista()
            return
        }

        setContentView(R.layout.activity_login)
        campoEmail = findViewById(R.id.campoEmail)
        campoClave = findViewById(R.id.campoClave)
        textoError = findViewById(R.id.textoError)
        val botonIngresar = findViewById<Button>(R.id.botonIngresar)
        val botonCrearCuenta = findViewById<Button>(R.id.botonCrearCuenta)
        botones = listOf(botonIngresar, botonCrearCuenta)

        botonIngresar.setOnClickListener { ingresar(crearCuenta = false) }
        botonCrearCuenta.setOnClickListener { ingresar(crearCuenta = true) }
    }

    private fun ingresar(crearCuenta: Boolean) {
        val email = campoEmail.text.toString().trim()
        val clave = campoClave.text.toString()
        if (email.isEmpty() || clave.isEmpty()) {
            textoError.setText(R.string.error_campos_vacios)
            return
        }

        textoError.text = ""
        botones.forEach { it.isEnabled = false }

        // Las dos llamadas son asíncronas: Firebase avisa en el listener cuando termina.
        val tarea = if (crearCuenta) auth.createUserWithEmailAndPassword(email, clave)
                    else auth.signInWithEmailAndPassword(email, clave)
        tarea.addOnCompleteListener(this) { resultado ->
            botones.forEach { it.isEnabled = true }
            if (resultado.isSuccessful) {
                val evento = if (crearCuenta) FirebaseAnalytics.Event.SIGN_UP else FirebaseAnalytics.Event.LOGIN
                Medicion.evento(this, evento, FirebaseAnalytics.Param.METHOD to "email")
                irALista()
            } else {
                textoError.setText(mensajeDeError(resultado.exception))
            }
        }
    }

    private fun mensajeDeError(error: Exception?): Int = when (error) {
        is FirebaseAuthWeakPasswordException -> R.string.error_clave_debil
        is FirebaseAuthUserCollisionException -> R.string.error_email_en_uso
        is FirebaseAuthInvalidCredentialsException -> R.string.error_credenciales
        is FirebaseNetworkException -> R.string.error_sin_conexion
        else -> R.string.error_desconocido
    }

    private fun irALista() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()  // Así "atrás" desde la lista cierra la app en vez de volver al login.
    }
}
