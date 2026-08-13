pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tenant-leaf"

// Each module remains independently deployable. Add a module here when its
// Gradle build file is created, for example:
include(":apps:android:tenantleaf")
// include(":services:api")
// include(":services:ai-worker")
