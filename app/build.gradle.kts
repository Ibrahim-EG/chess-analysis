plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "app.lumen.chess"
    compileSdk = 34
    defaultConfig {
        applicationId = "app.lumen.chess"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.0-Premium"
    }
    buildFeatures { compose = true }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.github.bhlangonijr:chesslib:1.3.3")
    
    // NATIVE STOCKFISH 18 BUNDLED!
    implementation("fr.axl-lvy:stockfish-multiplatform:0.1.0")
}
