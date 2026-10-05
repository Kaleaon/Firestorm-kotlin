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
project(":llsd-kotlin").projectDir = file("/app/Unified-LLSD/kotlin")
