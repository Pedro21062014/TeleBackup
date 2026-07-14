plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties
import java.io.FileInputStream

android {
    namespace = "com.telebackup.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.telebackup.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.3.2"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // Prefer CI env / keystore.properties; fall back to local telebackup.jks
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            }

            val storePath = System.getenv("KEYSTORE_PATH")
                ?: keystoreProperties.getProperty("storeFile")
                ?: "telebackup.jks"
            val storeFileCandidate = rootProject.file(storePath)
            if (storeFileCandidate.exists()) {
                storeFile = storeFileCandidate
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: keystoreProperties.getProperty("storePassword")
                    ?: "telebackup123"
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: keystoreProperties.getProperty("keyAlias")
                    ?: "telebackup"
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: keystoreProperties.getProperty("keyPassword")
                    ?: "telebackup123"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            // Same key so installs update cleanly during testing
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Skip lintVital on CI to avoid Metaspace OOM; APK is still fully signed
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
