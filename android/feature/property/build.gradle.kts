plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.kapt"); id("com.google.dagger.hilt.android") }
android {
    namespace = "com.seipseip.feature.property"; compileSdk = 35
    defaultConfig { minSdk = 24; consumerProguardFiles("consumer-rules.pro") }
    compileOptions { isCoreLibraryDesugaringEnabled = true; sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2"); implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.13.1"); implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"); implementation("com.google.dagger:hilt-android:2.60.1"); kapt("com.google.dagger:hilt-compiler:2.60.1")
    testImplementation("junit:junit:4.13.2"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"); testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
kapt { correctErrorTypes = true }
