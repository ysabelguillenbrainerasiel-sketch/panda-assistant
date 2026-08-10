plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.panda"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.panda"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-skeleton"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // El modelo de Vosk (carpeta model-wakeword/) puede pesar varias decenas de MB.
    // Se guarda sin comprimir para poder abrirlo directo desde assets en tiempo de ejecución.
    androidResources {
        noCompress += "tflite"
    }
    packaging {
        resources {
            excludes += "META-INF/*"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Motor de voz 100% offline (wake word + reconocimiento de comandos).
    // No requiere API key ni conexión a internet en tiempo de ejecución.
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
}
