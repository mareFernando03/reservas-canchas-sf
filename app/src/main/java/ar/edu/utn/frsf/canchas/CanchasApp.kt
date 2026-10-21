package ar.edu.utn.frsf.canchas

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds

/**
 * Clase de aplicación (TP6). ProcessLifecycleOwner avisa cuando la app entera pasa a primer o a
 * segundo plano, no cada pantalla: así el recordatorio por inactividad se programa al salir de la
 * app y se cancela al volver, sin importar en qué pantalla esté el usuario.
 */
class CanchasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TP4: el SDK de anuncios se inicializa una sola vez, en un hilo aparte, como recomienda Google.
        Thread { MobileAds.initialize(this) {} }.start()
        Notificaciones.crearCanales(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = Notificaciones.cancelarRecordatorioInactividad(this@CanchasApp)
            override fun onStop(owner: LifecycleOwner) = Notificaciones.programarRecordatorioInactividad(this@CanchasApp)
        })
    }
}
