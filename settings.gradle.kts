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

rootProject.name = "firestorm-kotlin"
include(":app")
include(":llsd-kotlin")
project(":llsd-kotlin").projectDir = listOf(
    file("Unified-LLSD/kotlin"),
    file("../Unified-LLSD/kotlin"),
    file("/app/Unified-LLSD/kotlin")
).firstOrNull { it.exists() } ?: file("Unified-LLSD/kotlin")
