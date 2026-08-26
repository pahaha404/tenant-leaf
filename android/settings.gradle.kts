import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val localProperties = Properties().apply {
    rootDir.resolve("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://devrepo.kakao.com/nexus/repository/kakaomap-releases/") }
        maven {
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: localProperties.getProperty("github_username")
                    ?: "user"
                password = System.getenv("GITHUB_TOKEN")
                    ?: localProperties.getProperty("github_token")
                    ?: localProperties.getProperty("github_tokens")
            }
        }
    }
}

rootProject.name = "tenant-leaf-android"
include(":app", ":core", ":feature:property", ":feature:inspection", ":feature:media")
