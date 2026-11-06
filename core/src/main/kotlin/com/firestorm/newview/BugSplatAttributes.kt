package com.firestorm.newview

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object BugSplatAttributes {

    private val attributes: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    private val lock = Any()

    var crashContextFileName: String = ""

    fun setAttribute(key: String, value: String, category: String = "FS") {
        synchronized(lock) {
            attributes.getOrPut(category) { mutableMapOf() }[toXmlToken(key)] = value
        }
    }

    fun setAttribute(key: String, value: Boolean, category: String = "FS") {
        setAttribute(key, if (value) "true" else "false", category)
    }

    fun setAttribute(key: String, value: Int, category: String = "FS") {
        setAttribute(key, value.toString(), category)
    }

    fun setAttribute(key: String, value: Long, category: String = "FS") {
        setAttribute(key, value.toString(), category)
    }

    fun setAttribute(key: String, value: Float, category: String = "FS") {
        setAttribute(key, value.toString(), category)
    }

    fun setAttribute(key: String, value: Double, category: String = "FS") {
        setAttribute(key, value.toString(), category)
    }

    fun writeToFile(filePath: String): Boolean {
        synchronized(lock) {
            val tmpPath = "$filePath.tmp"
            val tmpFile = File(tmpPath)

            return try {
                tmpFile.bufferedWriter(Charsets.UTF_8).use { w ->
                    w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    w.write("<XmlCrashContext>\n")

                    // Top-level (empty-category) attributes written first.
                    attributes[""]?.forEach { (key, value) ->
                        w.write("    <$key>$value</$key>\n")
                    }

                    // BugSplat dropped strict category XML; prefix the category to each attribute name.
                    for ((category, entries) in attributes) {
                        if (category.isEmpty()) continue
                        for ((key, value) in entries) {
                            w.write("    <$category-$key>$value</$category-$key>\n")
                        }
                    }

                    w.write("</XmlCrashContext>\n")
                }

                Files.move(
                    tmpFile.toPath(),
                    File(filePath).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                true
            } catch (e: Exception) {
                tmpFile.delete()
                false
            }
        }
    }

    // Converts an arbitrary string into a legal XML element-name token.
    // Examples: "Bandwidth (kbit/s)" → "Bandwidth_kbit_per_s"
    private fun toXmlToken(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when {
                ch == '/'                        -> sb.append("_per_")
                ch.isLetterOrDigit() || ch == '_' || ch == '-' -> sb.append(ch)
                ch == ' ' || ch == '(' || ch == ')' -> sb.append('_')
                else                             -> sb.append('_')
            }
        }

        // XML element names must begin with a letter or underscore.
        if (sb.isEmpty() || !(sb.first().isLetter() || sb.first() == '_')) {
            sb.insert(0, '_')
        }

        // Trailing underscores are noise; strip them.
        while (sb.isNotEmpty() && sb.last() == '_') {
            sb.deleteCharAt(sb.length - 1)
        }

        return sb.toString()
    }
}
