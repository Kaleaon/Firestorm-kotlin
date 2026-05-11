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

    // The pure-Kotlin renderer + math libraries live at the repository root.
    // They are not yet broken out into a Gradle module, so we pull them into
    // the Android compilation as additional source roots. Keep this list
    // tight — every directory added here must compile cleanly against the
    // Android classpath.
    sourceSets.getByName("main") {
        java.srcDirs(
            "../src/main/kotlin/com/firestorm/llrender",
            "../src/main/kotlin/com/firestorm/llmath",
            "../src/main/kotlin/com/firestorm/llcommon"
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
}
