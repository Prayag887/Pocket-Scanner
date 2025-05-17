plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.pocketscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pocketscanner"
        minSdk = 24
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
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // CameraX Core Library
    implementation ("androidx.camera:camera-core:1.4.2")

    // CameraX Camera2 Implementation
    implementation ("androidx.camera:camera-camera2:1.4.2")

    // CameraX Lifecycle
    implementation ("androidx.camera:camera-lifecycle:1.4.2")

    // CameraX View for Preview
    implementation ("androidx.camera:camera-view:1.4.2")

    // Optional: For image analysis
    implementation ("androidx.camera:camera-extensions:1.4.2")

    implementation ("androidx.navigation:navigation-compose:2.9.0")


    implementation(libs.document.scanner)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    //shimmer effect for the ui
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    implementation ("com.google.android.material:material:1.12.0")

    //koin
    implementation("io.insert-koin:koin-android:3.5.0")
    implementation("io.insert-koin:koin-core:3.5.0")
    implementation("io.insert-koin:koin-androidx-compose:3.5.0")

    testImplementation("io.insert-koin:koin-test-junit5:3.5.0")

    // material icons
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("io.coil-kt:coil-compose:2.4.0")


}