import java.util.Properties
import java.io.File

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasKeystore = keystorePropertiesFile.exists() && keystorePropertiesFile.isFile
if (hasKeystore) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.dafamsemarang.dhtv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dafamsemarang.dhtv"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                keyAlias = keystoreProperties["keyAlias"] as? String
                keyPassword = keystoreProperties["keyPassword"] as? String
                val storePath = keystoreProperties["storeFile"] as? String
                storeFile = storePath?.let {
                    if (File(it).isAbsolute) file(it) else rootProject.file(it)
                }
                storePassword = keystoreProperties["storePassword"] as? String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation (libs.androidx.core.ktx)
    implementation (libs.androidx.appcompat)
    implementation (libs.ui)
    implementation (libs.androidx.material3)
    implementation (libs.androidx.tv.foundation)
    implementation (libs.androidx.tv.material)
    implementation (libs.androidx.lifecycle.runtime.ktx)
    implementation (libs.androidx.activity.compose)
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.coil.compose)
    implementation ("io.coil-kt:coil-svg:2.7.0")
    implementation (libs.androidx.lifecycle.runtime.compose)
    implementation (libs.androidx.navigation.compose)
    implementation (libs.androidx.animation)
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation (platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation ("com.google.firebase:firebase-database-ktx")
    implementation ("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.zxing:core:3.4.1")
    implementation ("com.github.bumptech.glide:glide:4.15.0")
    implementation ("com.github.bumptech.glide:compose:1.0.0-alpha.2")
    implementation ("com.airbnb.android:lottie-compose:6.6.1")
    implementation("dev.chrisbanes.haze:haze:1.1.1")

    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.ktor:ktor-client-android:3.0.2")
    implementation("io.ktor:ktor-client-logging:3.0.2")

    implementation(libs.androidx.media3.ui)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}