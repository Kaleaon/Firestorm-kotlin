import migration.KotlinTranspiler
import migration.RepositoryImport
import java.nio.file.Path

fun main(args: Array<String>) {
    val source = args.getOrNull(0) ?: "https://github.com/FirestormViewer/phoenix-firestorm"
    val workspace = Path.of(args.getOrNull(1) ?: "build/firestorm-migration/phoenix-firestorm")
    val output = Path.of(args.getOrNull(2) ?: "build/firestorm-migration/kotlin")

    println("Starting Firestorm import and Kotlin conversion pipeline")
    println("Source repository: $source")
    println("Workspace: $workspace")
    println("Kotlin output: $output")
    println()

    val shouldImport = args.contains("--import") || args.contains("--import-convert")
    val shouldConvert = args.contains("--convert") || args.contains("--import-convert")

    if (shouldImport) {
        RepositoryImport.cloneIfMissing(source, workspace)
        val totalFiles = RepositoryImport.listFiles(workspace).size
        println("Imported repository file count: $totalFiles")
    }

    if (shouldConvert) {
        val converted = KotlinTranspiler.transpileAll(workspace, output)
        println("Generated Kotlin files: $converted")
    }

    if (!shouldImport && !shouldConvert) {
        println(
            """
            Usage:
              --import           Clone upstream repository into workspace.
              --convert          Convert workspace files into Kotlin artifacts.
              --import-convert   Clone (if needed) and convert in one run.

            Example:
              kotlin MainKt --import-convert
            """.trimIndent()
        )
    }
}
