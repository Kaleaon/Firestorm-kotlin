package migration

import java.io.File
import java.nio.file.Path

object RepositoryImport {
    fun cloneIfMissing(repositoryUrl: String, destination: Path) {
        val destinationFile = destination.toFile()
        if (destinationFile.exists()) return

        destinationFile.parentFile?.mkdirs()
        val process = ProcessBuilder(
            "git",
            "clone",
            "--depth",
            "1",
            repositoryUrl,
            destination.toString()
        )
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                println(line)
            }
        }

        val code = process.waitFor()
        check(code == 0) { "git clone failed with exit code $code" }
    }

    fun listFiles(root: Path): List<String> {
        val files = mutableListOf<String>()
        walk(root.toFile(), root.toFile(), files)
        return files.sorted()
    }

    private fun walk(base: File, current: File, files: MutableList<String>) {
        current.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                walk(base, child, files)
            } else {
                files += child.relativeTo(base).invariantSeparatorsPath
            }
        }
    }
}
