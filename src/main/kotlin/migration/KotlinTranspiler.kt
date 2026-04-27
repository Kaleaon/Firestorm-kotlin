package migration

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.nameWithoutExtension

object KotlinTranspiler {
    private val supportedExtensions = setOf(
        "c", "cc", "cpp", "cxx",
        "h", "hh", "hpp", "hxx",
        "cs", "java", "py", "js", "ts",
        "glsl", "vert", "frag", "cmake", "txt", "xml", "json", "ini"
    )

    fun transpileAll(sourceRoot: Path, outputRoot: Path): Int {
        outputRoot.createDirectories()
        var generated = 0

        Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .filterNot { it.invariantSeparatorsPathString.contains("/.git/") }
                .forEach { file ->
                    if (isLikelyBinary(file)) return@forEach
                    if (!shouldProcess(file)) return@forEach

                    val relative = sourceRoot.relativize(file).invariantSeparatorsPathString
                    val packageName = buildPackageName(relative)
                    val objectName = buildObjectName(file)
                    val outputFile = outputRoot.resolve(relative.replace('/', '_') + ".kt")
                    outputFile.parent?.createDirectories()

                    val content = Files.readString(file)
                    val kotlinFile = buildKotlinFile(packageName, objectName, relative, file.extension, content)
                    Files.writeString(outputFile, kotlinFile)
                    generated += 1
                }
        }

        return generated
    }

    private fun shouldProcess(file: Path): Boolean {
        val ext = file.extension.lowercase()
        return ext in supportedExtensions || ext.isEmpty()
    }

    private fun isLikelyBinary(file: Path): Boolean {
        val bytes = Files.newInputStream(file).use { input ->
            input.readNBytes(4096)
        }
        return bytes.any { it == 0.toByte() }
    }

    private fun buildPackageName(relativePath: String): String {
        val parts = relativePath.split('/').dropLast(1)
            .map { it.lowercase().replace(Regex("[^a-z0-9_]"), "_") }
            .filter { it.isNotBlank() }

        return if (parts.isEmpty()) {
            "firestorm.imported"
        } else {
            "firestorm.imported." + parts.joinToString(".")
        }
    }

    private fun buildObjectName(file: Path): String {
        val base = file.nameWithoutExtension
            .split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotBlank() }
            .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Source" }

        return "${base}Port"
    }

    private fun buildKotlinFile(
        packageName: String,
        objectName: String,
        relativePath: String,
        extension: String,
        source: String
    ): String {
        val escaped = source.replace("\"\"\"", "\\\"\\\"\\\"")

        return """
            |package $packageName
            |
            |/**
            | * Auto-generated from `$relativePath`.
            | * Original extension: `${extension.ifBlank { "none" }}`.
            | */
            |object $objectName {
            |    const val sourcePath: String = "$relativePath"
            |    const val originalExtension: String = "${extension.ifBlank { "none" }}"
            |
            |    val originalCode: String = """
            |$escaped
            |    """.trimIndent()
            |}
            |
        """.trimMargin()
    }
}
