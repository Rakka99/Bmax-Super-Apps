plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "id.bmax.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "id.bmax.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
        val supabaseUrl = project.findProperty("SUPABASE_URL")?.toString()?.takeIf { it.isNotBlank() } ?: "https://vgnynrzhanfnbifjedga.supabase.co"
        val supabaseKey = project.findProperty("SUPABASE_PUBLISHABLE_KEY")?.toString()?.takeIf { it.isNotBlank() } ?: ""
        val mapsKey = project.findProperty("MAPS_API_KEY")?.toString()?.takeIf { it.isNotBlank() } ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
    }
    buildTypes { getByName("debug") { } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin { jvmToolchain(17) }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom); androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui"); implementation("androidx.compose.ui:ui-tooling-preview"); debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.room:room-runtime:2.6.1"); implementation("androidx.room:room-ktx:2.6.1"); ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.dagger:hilt-android:2.54"); ksp("com.google.dagger:hilt-compiler:2.54"); implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("io.ktor:ktor-client-android:3.0.3"); implementation("io.ktor:ktor-client-content-negotiation:3.0.3"); implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.1"); implementation("io.github.jan-tennert.supabase:auth-kt:3.1.1"); implementation("io.github.jan-tennert.supabase:realtime-kt:3.1.1"); implementation("io.github.jan-tennert.supabase:storage-kt:3.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0"); implementation("io.coil-kt:coil-compose:2.7.0"); implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:6.4.1")
}
