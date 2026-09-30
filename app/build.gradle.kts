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
}
