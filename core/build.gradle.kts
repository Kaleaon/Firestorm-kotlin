plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-language-version=1.9")
    }
}

sourceSets {
    main {
        kotlin {
            srcDir("../src/main/kotlin")
            include("com/firestorm/llprimitive/**")
            include("com/firestorm/llmath/**")
            include("com/firestorm/llcommon/**")
            include("com/firestorm/llrender/VertexBuffer.kt")
        }
    }
    test {
        kotlin {
            srcDir("../src/test/kotlin")
            include("com/firestorm/llprimitive/**")
            include("com/firestorm/llmath/**")
            include("com/firestorm/llcommon/**")
            include("com/firestorm/llrender/VertexBuffer.kt")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}
