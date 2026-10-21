plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "ar.edu.utn.frsf.canchas"
    compileSdk = 36

    defaultConfig {
        applicationId = "ar.edu.utn.frsf.canchas"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2"

        // Sólo para probar: con -PauthEmulator=true el login usa el emulador local de Firebase Auth
        // en vez del proyecto real, así las cuentas de prueba no quedan en Firebase.
        val authEmulator = (project.findProperty("authEmulator") as String?) == "true"
        buildConfigField("boolean", "AUTH_EMULATOR", authEmulator.toString())

        // Sólo para probar (TP6): con -PrecordatorioPrueba=true los recordatorios salen a los pocos
        // segundos en vez de dos horas antes del turno o a los 7 días de inactividad.
        val recordatorioPrueba = (project.findProperty("recordatorioPrueba") as String?) == "true"
        buildConfigField("boolean", "RECORDATORIO_PRUEBA", recordatorioPrueba.toString())
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")

    // Firebase: la BoM fija versiones compatibles entre sí, por eso auth y analytics van sin versión.
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging") // TP6: notificaciones push

    // TP6: recordatorios programados que se disparan aunque la app esté cerrada.
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    // TP6: saber cuándo la app entera pasa a segundo plano (recordatorio por inactividad).
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")

    // TP7: mapa embebido con OpenStreetMap (no pide API key) y ubicación del usuario.
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.google.android.gms:play-services-location:21.4.0")

    // TP4: anuncios con AdMob (SDK de Google Mobile Ads).
    implementation("com.google.android.gms:play-services-ads:25.5.0")
}
