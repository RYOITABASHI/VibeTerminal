plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "com.vibeterminal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vibeterminal"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
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
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Window Size Class (for adaptive layouts)
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")

    // Coil (for image loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Markdown rendering with syntax highlighting
    // TODO: Re-enable when dependency issues are resolved
    // implementation("com.halilibo.compose-richtext:richtext-ui:0.17.0")
    // implementation("com.halilibo.compose-richtext:richtext-ui-material3:0.17.0")
    // implementation("com.halilibo.compose-richtext:richtext-commonmark:0.17.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Serialization (for JSON patterns)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // DataStore (for settings)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Room Database (for chat history)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Security (for encrypted storage)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Browser (for Custom Tabs - OAuth)
    implementation("androidx.browser:browser:1.8.0")

    // Gemini AI (for advanced translation)
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

    // OkHttp (for network requests)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Retrofit (for REST API)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Termux Libraries (via JitPack)
    // Note: Using GitHub releases instead of JitPack due to 401 errors
    // implementation("com.github.termux.termux-app:termux-shared:v0.118.0")
    // implementation("com.github.termux.termux-app:terminal-view:v0.118.0")
    // TODO: Add Termux libraries when JitPack issue is resolved
    // For now, build without Termux terminal emulator

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
