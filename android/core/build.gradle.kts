import java.net.URI

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.openapi.generator)
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun String.asApiBaseUrl(propertyName: String): String {
    val normalized = trim().let { if (it.endsWith('/')) it else "$it/" }
    val uri = runCatching { URI(normalized) }.getOrNull()
    require(uri?.scheme in setOf("http", "https") && !uri?.host.isNullOrBlank()) {
        "$propertyName must be a complete http:// or https:// URL"
    }
    return normalized
}

val debugApiBaseUrl = providers.gradleProperty("TENANT_LEAF_DEBUG_API_BASE_URL")
    .orElse("http://10.0.2.2:8080/api/v1/")
    .map { it.asApiBaseUrl("TENANT_LEAF_DEBUG_API_BASE_URL") }

val releaseApiBaseUrl = providers.gradleProperty("TENANT_LEAF_RELEASE_API_BASE_URL")
    .orElse("https://api.tenant-leaf.invalid/api/v1/")
    .map { it.asApiBaseUrl("TENANT_LEAF_RELEASE_API_BASE_URL") }

android {
    namespace = "com.seipseip.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", debugApiBaseUrl.get().asBuildConfigString())
        }
        release {
            buildConfigField("String", "API_BASE_URL", releaseApiBaseUrl.get().asBuildConfigString())
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets.named("main") {
        java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin"))
    }
}

val openApiSpec = rootProject.file("../server/shared-types/openapi/openapi.yaml")

openApiValidate {
    inputSpec.set(openApiSpec.absolutePath)
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-retrofit2")
    inputSpec.set(openApiSpec.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("com.seipseip.core.network.generated.api")
    modelPackage.set("com.seipseip.core.network.generated.model")
    globalProperties.set(
        mapOf(
            "apis" to "Properties,Inspections",
            "models" to "CreatePropertyRequest,UpdatePropertyRequest,Property,PropertyPage,PageMetadata,Inspection,InspectionPage,InspectionStatus,InspectionAnalysisStatus,UpdateInspectionStatusRequest,ErrorResponse,FieldError",
            "supportingFiles" to "CollectionFormats.kt",
            "apiDocs" to "false",
            "apiTests" to "false",
            "modelDocs" to "false",
            "modelTests" to "false",
        ),
    )
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "enumPropertyNaming" to "original",
            "moshiCodeGen" to "true",
            "serializationLibrary" to "moshi",
            "sourceFolder" to "src/main/kotlin",
            "useCoroutines" to "true",
        ),
    )
}

tasks.named("openApiGenerate") {
    dependsOn("openApiValidate")
}

tasks.named("preBuild") {
    dependsOn("openApiGenerate")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    api(libs.retrofit.core)
    api(libs.retrofit.converter.moshi)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    api(libs.moshi.kotlin)
    kapt(libs.moshi.kotlin.codegen)
    compileOnly(libs.javax.annotation)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

kapt {
    correctErrorTypes = true
}
