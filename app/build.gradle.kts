plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.example.habittrackerr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.habittrackerr"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android Dependencies
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose BOM and UI
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

    // Firebase BOM and Services with targeted exclusions for OnePlus compatibility
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

    implementation("com.google.firebase:firebase-auth-ktx") {
        exclude(group = "com.google.android.gms", module = "play-services-safetynet")
        exclude(group = "com.google.android.gms", module = "play-services-auth-base")
    }

    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-config-ktx")

    // Modern Credential Manager API (NO legacy GoogleSignInClient)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0") {
        exclude(group = "com.google.android.gms", module = "play-services-safetynet")
        exclude(group = "com.google.android.gms", module = "play-services-auth-base")
    }
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Essential Google Play Services dependencies (keep minimal for OnePlus compatibility)
    implementation("com.google.android.gms:play-services-auth:21.2.0") {
        exclude(group = "com.google.android.gms", module = "play-services-safetynet")
    }
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.gms:play-services-tasks:18.2.0")

    // Google Fit Integration (Fallback) - Fix missing AuthProxy
    implementation("com.google.android.gms:play-services-fitness:21.1.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.1.0")

    // Add missing core GMS dependencies for AuthProxy
    implementation("com.google.android.gms:play-services-basement:18.4.0")
    implementation("com.google.android.gms:play-services-auth-base:18.0.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Date and Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Data Store for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Phone number validation
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.45")

    // Health Connect Integration (Primary)
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // Google Fit Integration (Fallback)
    implementation("com.google.android.gms:play-services-fitness:21.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Charts for Statistics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

configurations.all {
    // Only exclude the specific OnePlus-problematic modules, not essential dependencies
    exclude(group = "com.google.android.gms", module = "play-services-safetynet")
    exclude(group = "com.google.android.gms", module = "play-services-auth-base")

    // Force specific versions to avoid conflicts
    resolutionStrategy {
        force("com.google.android.gms:play-services-base:18.5.0")
        force("com.google.android.gms:play-services-tasks:18.2.0")
        force("com.google.android.gms:play-services-auth:21.2.0")
    }
}
