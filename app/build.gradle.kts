plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.firestorm.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.firestorm.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("../src/main/kotlin", "src/main/java")
        }
        getByName("test") {
            java.srcDirs("../src/test/kotlin")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude { element ->
        if (element.isDirectory) return@exclude false
        val path = element.path.replace('\\', '/')
        val isPreproc = path.contains("FSLSLPreproc")
        val isAndroid = path.contains("android") || path.contains("Firestorm") || path.contains("MainActivity")
        val broken = path.contains("LLApp.kt") || path.contains("LLProcess.kt") ||
                     path.contains("LLQueuedThread.kt") || path.contains("LLThread.kt") ||
                     path.contains("LLWorkerThread.kt") || path.contains("LLMutex.kt") ||
                     path.contains("LLError.kt") || path.contains("LLApr.kt") ||
                     path.contains("LLCoros.kt") || path.contains("LLCommonNewTest.kt")
        if (broken) return@exclude true
        val isCommon = path.contains("llcommon")
        !(isPreproc || isAndroid || isCommon)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}
