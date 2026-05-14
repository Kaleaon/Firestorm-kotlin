package com.firestorm.newview

import java.io.File

class LLExternalEditor {

    enum class EErrorCode {
        EC_SUCCESS,
        EC_NOT_SPECIFIED,
        EC_PARSE_ERROR,
        EC_BINARY_NOT_FOUND,
        EC_FAILED_TO_RUN
    }

    companion object {
        private const val FILENAME_MARKER = "%s"
        private const val SETTING = "ExternalEditor"

        fun getErrorMessage(code: EErrorCode): String {
            return when (code) {
                EErrorCode.EC_SUCCESS          -> "ok"
                EErrorCode.EC_NOT_SPECIFIED    -> "ExternalEditorNotSet"
                EErrorCode.EC_PARSE_ERROR      -> "ExternalEditorCommandParseError"
                EErrorCode.EC_BINARY_NOT_FOUND -> "ExternalEditorNotFound"
                EErrorCode.EC_FAILED_TO_RUN    -> "ExternalEditorFailedToRun"
            }
        }

        private fun findCommand(envVar: String, override: String): String {
            if (override.isNotEmpty()) return override

            val fromSetting = lookupSetting(SETTING)
            if (fromSetting.isNotEmpty()) return fromSetting

            if (envVar.isNotEmpty()) {
                val fromEnv = System.getenv(envVar)
                if (!fromEnv.isNullOrEmpty()) return fromEnv
            }

            return ""
        }

        private fun lookupSetting(key: String): String {
            System.err.println("LLExternalEditor: lookupSetting not yet implemented")
            return ""
        }

        private fun tokenize(str: String): MutableList<String> {
            val tokens = mutableListOf<String>()
            var insideQuotes = false
            val current = StringBuilder()

            for (ch in str) {
                when {
                    ch == '"' -> insideQuotes = !insideQuotes
                    ch == ' ' && !insideQuotes -> {
                        if (current.isNotEmpty()) {
                            tokens.add(current.toString())
                            current.clear()
                        }
                    }
                    else -> current.append(ch)
                }
            }
            if (current.isNotEmpty()) tokens.add(current.toString())
            return tokens
        }
    }

    private var executable: String = ""
    private var args: MutableList<String> = mutableListOf()

    fun setCommand(envVar: String, override: String = ""): EErrorCode {
        var cmd = findCommand(envVar, override)

        if (cmd.isEmpty()) {
            val osCmd = when {
                System.getProperty("os.name").startsWith("Windows") -> {
                    val sysRoot = System.getenv("SystemRoot") ?: ""
                    if (sysRoot.isNotEmpty()) "$sysRoot\\explorer.exe \"%s\"" else ""
                }
                System.getProperty("os.name").startsWith("Mac") -> "/usr/bin/open -t \"%s\""
                else -> "/usr/bin/xdg-open \"%s\""
            }
            cmd = findCommand("", osCmd)
            if (cmd.isEmpty()) return EErrorCode.EC_NOT_SPECIFIED
        }

        val tokens = tokenize(cmd)
        if (tokens.isEmpty()) return EErrorCode.EC_PARSE_ERROR

        val binPath = tokens[0]
        if (!File(binPath).isFile) return EErrorCode.EC_BINARY_NOT_FOUND

        executable = binPath
        args = tokens.drop(1).toMutableList()

        if (!cmd.contains(FILENAME_MARKER)) {
            args.add(FILENAME_MARKER)
        }

        return EErrorCode.EC_SUCCESS
    }

    fun run(filePath: String): EErrorCode {
        if (executable.isEmpty() || args.isEmpty()) return EErrorCode.EC_NOT_SPECIFIED

        val resolvedArgs = args.map { it.replace(FILENAME_MARKER, filePath) }

        return try {
            val command = listOf(executable) + resolvedArgs
            ProcessBuilder(command)
                .inheritIO()
                .start()
            EErrorCode.EC_SUCCESS
        } catch (e: Exception) {
            EErrorCode.EC_FAILED_TO_RUN
        }
    }
}
