plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.workouttracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.workouttracker"
        minSdk = 21
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
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.compose.animation.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))

    // UI Toolkit
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3
    implementation("androidx.compose.material3:material3:1.3.2")

    // Material 3 Icons
    implementation("androidx.compose.material:material-icons-core:1.4.3")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")

    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Coil для загрузки изображений
    implementation("io.coil-kt:coil-compose:2.2.2")

    // (можно опционально оставить для отладки)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

}