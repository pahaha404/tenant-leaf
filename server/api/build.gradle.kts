plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.3.21"
	id("org.openapi.generator") version "7.24.0"
}

group = "com.tenantleaf"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

val openApiSpec = file("../shared-types/openapi/openapi.yaml")
val openApiOutput = layout.buildDirectory.dir("generated/openapi")

openApiValidate {
	inputSpec.set(openApiSpec.absolutePath)
}

openApiGenerate {
	generatorName.set("kotlin-spring")
	inputSpec.set(openApiSpec.absolutePath)
	outputDir.set(openApiOutput.get().asFile.absolutePath)
	apiPackage.set("com.tenantleaf.api.generated.api")
	modelPackage.set("com.tenantleaf.api.generated.model")

	globalProperties.set(
		mapOf(
			"apis" to "",
			"models" to "",
			"supportingFiles" to "false",
			"apiDocs" to "false",
			"apiTests" to "false",
			"modelDocs" to "false",
			"modelTests" to "false",
		),
	)
	configOptions.set(
		mapOf(
			"annotationLibrary" to "none",
			"documentationProvider" to "none",
			"interfaceOnly" to "true",
			"requestMappingMode" to "api_interface",
			"skipDefaultInterface" to "true",
			"sourceFolder" to "src/main/kotlin",
			"useBeanValidation" to "true",
			"useJackson3" to "true",
			"useResponseEntity" to "true",
			"useSpringBoot4" to "true",
			"useSwaggerUI" to "false",
			"useTags" to "true",
		),
	)
}

kotlin.sourceSets.named("main") {
	kotlin.srcDir(openApiOutput.map { it.dir("src/main/kotlin") })
}

tasks.named("openApiGenerate") {
	dependsOn("openApiValidate")
}

tasks.named("compileKotlin") {
	dependsOn("openApiGenerate")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
