plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
}

import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val datDeveloperMode = localProperties.getProperty("mwdat_developer_mode") == "true"
fun projectSecret(name: String): String =
    providers.gradleProperty(name).orNull ?: localProperties.getProperty(name, "")

android {
    namespace = "com.seipseip.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.seipseip.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // The SDK reads these manifest entries as strings. Keep development-mode values empty,
        // matching Meta's CameraAccess sample; XML would otherwise coerce a bare 0 to an integer.
        manifestPlaceholders["mwdat_application_id"] = if (datDeveloperMode) "" else localProperties.getProperty("mwdat_application_id", "")
        manifestPlaceholders["mwdat_client_token"] = if (datDeveloperMode) "" else localProperties.getProperty("mwdat_client_token", "")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "KAKAO_NATIVE_APP_KEY",
            "\"${projectSecret("KAKAO_NATIVE_APP_KEY")}\"",
        )
        buildConfigField(
            "String",
            "KAKAO_REST_API_KEY",
            "\"${projectSecret("KAKAO_REST_API_KEY")}\"",
        )
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    implementation(project(":core"))
    implementation(project(":feature:property"))
    implementation(project(":feature:inspection"))
    implementation(project(":feature:media"))
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("com.meta.wearable:mwdat-core:0.9.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.60.1")
    kapt("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("com.kakao.maps.open:android:2.15.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

kapt { correctErrorTypes = true }
