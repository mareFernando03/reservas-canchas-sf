plugins {
    id("com.android.application")
}

android {
    namespace = "ar.edu.utn.frsf.canchas"
    compileSdk = 36

    defaultConfig {
        applicationId = "ar.edu.utn.frsf.canchas"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
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
}
