package migration

object CLikeKotlinStubGenerator {
    private val classRegex = Regex("""\b(class|struct)\s+([A-Za-z_][A-Za-z0-9_]*)""")
    private val functionRegex = Regex(
        """(?:^|\s)(?:inline\s+|static\s+|virtual\s+|constexpr\s+|extern\s+)*[A-Za-z_][A-Za-z0-9_:<>,\s*&~]*\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(([^;{}()]*)\)\s*[;{]"""
    )

    fun buildStubBlock(source: String): String {
        val classes = classRegex.findAll(source)
            .map { it.groupValues[2] }
            .distinct()
            .toList()

        val functions = functionRegex.findAll(source)
            .map { it.groupValues[1] to it.groupValues[2] }
            .filterNot { (name, _) -> name in setOf("if", "for", "while", "switch") }
            .distinctBy { it.first }
            .toList()

        val classLines = classes.joinToString("\n") { "    class ${sanitizeIdentifier(it)}" }
        val functionLines = functions.joinToString("\n\n") { (name, params) ->
            val mappedParams = mapParameters(params)
            "    fun ${sanitizeIdentifier(name)}($mappedParams): Unit = TODO(\"Port from native source\")"
        }

        return listOf(classLines, functionLines)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun mapParameters(params: String): String {
        if (params.isBlank() || params.trim() == "void") return ""

        return params.split(',')
            .mapIndexed { index, raw ->
                val token = raw.trim()
                val candidate = token.substringAfterLast(' ').substringAfterLast('*').substringAfterLast('&')
                    .ifBlank { "arg${index + 1}" }
                "${sanitizeIdentifier(candidate)}: String"
            }
            .joinToString(", ")
    }

    private fun sanitizeIdentifier(input: String): String {
        val cleaned = input.replace(Regex("[^A-Za-z0-9_]"), "_")
            .replace(Regex("^[0-9]+"), "")
            .ifBlank { "generated" }
        return if (cleaned in kotlinKeywords) "${cleaned}_" else cleaned
    }

    private val kotlinKeywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface",
        "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
        "typeof", "val", "var", "when", "while"
    )
}
