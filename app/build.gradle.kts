plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.appbuilder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.appbuilder"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { isMinifyEnabled = false }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging { resources { excludes += "/META-INF/{LICENSE*.md,AL2.0,LGPL2.1}" } }
}

dependencies {
    // Compose BOM (gestisce versioni coerenti)
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))

    // Compose UI & Material3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Foundation (layout, gestures, scroll, ecc.)
    implementation("androidx.compose.foundation:foundation")

    // Icone estese (per tutte le icone usate nell’Editor)
    implementation("androidx.compose.material:material-icons-extended")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.3")

    // AppCompat & Material (tema base legacy solo per compatibilità manifest)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Core KTX
    implementation("androidx.core:core-ktx:1.13.1")
}
