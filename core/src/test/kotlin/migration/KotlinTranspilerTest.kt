package migration

import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinTranspilerTest {
    @Test
    fun `transpiles native file and emits native signatures`() {
        val sourceRoot = kotlin.io.path.createTempDirectory("firestorm-source")
        val outputRoot = kotlin.io.path.createTempDirectory("firestorm-output")
        val inputFile = sourceRoot.resolve("llrender/ll_draw_pool.cpp")
        inputFile.parent.createDirectories()
        inputFile.writeText(
            """
            class LLDrawPool {
            public:
                void renderPass(int pass);
            };
            """.trimIndent()
        )

        val generatedCount = KotlinTranspiler.transpileAll(sourceRoot, outputRoot)
        assertEquals(1, generatedCount)

        val generated = outputRoot.resolve("llrender_ll_draw_pool.cpp.kt").readText()
        assertTrue(generated.contains("package firestorm.imported.llrender"))
        assertTrue(generated.contains("object LlDrawPoolPort"))
        assertTrue(generated.contains("class LLDrawPool"))
        assertTrue(generated.contains("fun renderPass(pass: String): Unit"))
    }

    @Test
    fun `skips binary files`() {
        val sourceRoot = kotlin.io.path.createTempDirectory("firestorm-source-bin")
        val outputRoot = kotlin.io.path.createTempDirectory("firestorm-output-bin")
        val inputFile = sourceRoot.resolve("textures/logo.bin")
        inputFile.parent.createDirectories()
        inputFile.toFile().writeBytes(byteArrayOf(0, 1, 2, 3, 4))

        val generatedCount = KotlinTranspiler.transpileAll(sourceRoot, outputRoot)
        assertEquals(0, generatedCount)
    }
}
