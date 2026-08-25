import java.net.URI

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.openapi.generator")
}

fun String.asBuildConfigString() = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
fun String.asApiBaseUrl(name: String): String {
    val normalized = trim().let { if (it.endsWith('/')) it else "$it/" }
    val uri = runCatching { URI(normalized) }.getOrNull()
    require(uri?.scheme in setOf("http", "https") && !uri?.host.isNullOrBlank()) {
        "$name must be a complete http:// or https:// URL"
    }
    return normalized
}

val debugApiBaseUrl = providers.gradleProperty("TENANT_LEAF_DEBUG_API_BASE_URL")
    .orElse("http://127.0.0.1:8080/api/v1/")
    .map { it.asApiBaseUrl("TENANT_LEAF_DEBUG_API_BASE_URL") }
val releaseApiBaseUrl = providers.gradleProperty("TENANT_LEAF_RELEASE_API_BASE_URL")
    .orElse("https://api.tenant-leaf.invalid/api/v1/")
    .map { it.asApiBaseUrl("TENANT_LEAF_RELEASE_API_BASE_URL") }

android {
    namespace = "com.seipseip.core"
    compileSdk = 35
    defaultConfig { minSdk = 24; consumerProguardFiles("consumer-rules.pro") }
    buildFeatures { buildConfig = true }
    buildTypes {
        debug { buildConfigField("String", "API_BASE_URL", debugApiBaseUrl.get().asBuildConfigString()) }
        release { buildConfigField("String", "API_BASE_URL", releaseApiBaseUrl.get().asBuildConfigString()) }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin"))
    }
}

val openApiSpec = rootProject.file("../server/shared-types/openapi/openapi.yaml")
openApiValidate { inputSpec.set(openApiSpec.absolutePath) }
openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-retrofit2")
    inputSpec.set(openApiSpec.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("com.seipseip.core.network.generated.api")
    modelPackage.set("com.seipseip.core.network.generated.model")
    globalProperties.set(mapOf(
        "apis" to "Properties,Inspections,Media,Observations,Reports",
        "models" to "CreatePropertyRequest,UpdatePropertyRequest,Property,PropertyPage,PageMetadata,Inspection,InspectionPage,InspectionStatus,InspectionAnalysisStatus,UpdateInspectionStatusRequest,CreateMediaUploadBatchRequest,CreateMediaUploadRequest,CreateMediaUploadBatchResponse,MediaUploadInstruction,Media,MediaPage,FinalizeInspectionMediaRequest,FinalizeInspectionMediaResponse,Zone,MediaType,CaptureSource,FrameOrigin,MediaUploadStatus,MediaAnalysisStatus,Observation,ObservationPage,ObservationEvidence,EvidenceDetection,ImageDimensions,Bbox,BboxCoordinateSystem,ObservationType,ObservationStatus,AiLabel,UpdateObservationStatusRequest,ReportSummary,ReportRepresentativePhoto,ReportDetail,ReportPage,ReportStatus,ReportFailureCode,ErrorResponse,FieldError",
        "supportingFiles" to "CollectionFormats.kt", "apiDocs" to "false", "apiTests" to "false",
        "modelDocs" to "false", "modelTests" to "false",
    ))
    configOptions.set(mapOf(
        "dateLibrary" to "java8", "enumPropertyNaming" to "original", "moshiCodeGen" to "true",
        "serializationLibrary" to "moshi", "sourceFolder" to "src/main/kotlin", "useCoroutines" to "true",
    ))
}
tasks.named("openApiGenerate") { dependsOn("openApiValidate") }
tasks.named("preBuild") { dependsOn("openApiGenerate") }

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.dagger:hilt-android:2.60.1")
    kapt("com.google.dagger:hilt-compiler:2.60.1")
    api("com.squareup.retrofit2:retrofit:2.11.0")
    api("com.squareup.retrofit2:converter-moshi:2.11.0")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    api("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
kapt { correctErrorTypes = true }
