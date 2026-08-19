pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

import java.util.Properties

val localProperties = Properties().apply {
    rootDir.resolve("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            credentials {
                username = ""
                password = System.getenv("GITHUB_TOKEN") ?: localProperties.getProperty("github_token") ?: localProperties.getProperty("github_tokens")
            }
        }
    }
}

rootProject.name = "tenant-leaf-android"
include(":app")
