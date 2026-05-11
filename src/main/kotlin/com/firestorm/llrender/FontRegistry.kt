package com.firestorm.llrender

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class FontFileInfo(
    val fileName: String,
    val charFunctor: ((Int) -> Boolean)? = null
)

data class FontDescriptor(
    val name: String = "",
    val size: String = "",
    val style: UByte = 0u,
    val fontFiles: List<FontFileInfo> = emptyList(),
    val fontCollectionFiles: List<FontFileInfo> = emptyList()
) : Comparable<FontDescriptor> {

    override fun compareTo(other: FontDescriptor): Int {
        val nameCmp = name.compareTo(other.name)
        if (nameCmp != 0) return nameCmp
        val styleCmp = style.compareTo(other.style)
        if (styleCmp != 0) return styleCmp
        return size.compareTo(other.size)
    }

    fun isTemplate(): Boolean = size == TEMPLATE_STRING

    fun normalize(): FontDescriptor {
        var newName = name
        var newSize = size
        var newStyle = (style.toInt() and (BOLD or ITALIC)).toUByte()

        fun removeAndSet(sub: String, sizeValue: String): Boolean {
            return if (sub in newName) { newName = newName.replace(sub, ""); newSize = sizeValue; true } else false
        }

        removeAndSet("Small", "Small")
        removeAndSet("Big", "Large")
        removeAndSet("Medium", "Medium")
        removeAndSet("Large", "Large")
        removeAndSet("Huge", "Huge")

        if (newSize != TEMPLATE_STRING && newSize.isEmpty() && "Monospace" in newName) newSize = "Monospace"
        if (newSize != TEMPLATE_STRING && newSize.isEmpty() && "Scripting" in newName) newSize = "Scripting"
        if (newSize != TEMPLATE_STRING && newSize.isEmpty() && "Cascadia" in newName) newSize = "Cascadia"
        if (newSize.isEmpty()) newSize = "Medium"

        if ("Bold" in newName) { newName = newName.replace("Bold", ""); newStyle = (newStyle.toInt() or BOLD).toUByte() }
        if ("Italic" in newName) { newName = newName.replace("Italic", ""); newStyle = (newStyle.toInt() or ITALIC).toUByte() }

        return FontDescriptor(newName, newSize, newStyle, fontFiles, fontCollectionFiles)
    }

    fun withFontFiles(files: List<FontFileInfo>): FontDescriptor = copy(fontFiles = files)
    fun withFontCollectionFiles(files: List<FontFileInfo>): FontDescriptor = copy(fontCollectionFiles = files)
    fun withSize(newSize: String): FontDescriptor = copy(size = newSize)

    companion object {
        const val TEMPLATE_STRING = "TEMPLATE"
        const val BOLD = 0x01
        const val ITALIC = 0x02

        val charFunctors: Map<String, (Int) -> Boolean> = mapOf(
            "is_emoji" to { ch: Int -> isEmoji(ch) },
            "is_emoji_use_color" to { ch: Int -> isEmojiUseColor(ch) },
            "is_emoji_use_bw" to { ch: Int -> isEmojiUseBW(ch) }
        )

        /** True if the codepoint is in one of Unicode's emoji blocks. */
        private fun isEmoji(codePoint: Int): Boolean = when (codePoint) {
            in 0x1F600..0x1F64F,   // Emoticons
            in 0x1F300..0x1F5FF,   // Misc symbols & pictographs
            in 0x1F680..0x1F6FF,   // Transport & map
            in 0x1F700..0x1F77F,   // Alchemical
            in 0x1F780..0x1F7FF,   // Geometric shapes ext
            in 0x1F800..0x1F8FF,   // Supplemental arrows
            in 0x1F900..0x1F9FF,   // Supplemental symbols & pictographs
            in 0x1FA00..0x1FA6F,   // Chess + misc
            in 0x1FA70..0x1FAFF,   // Symbols & pictographs extended-A
            in 0x2600..0x26FF,     // Misc symbols
            in 0x2700..0x27BF      // Dingbats
            -> true
            else -> false
        }

        private fun isEmojiUseColor(codePoint: Int): Boolean = !sUseEmojiBW && isEmoji(codePoint)
        private fun isEmojiUseBW(codePoint: Int): Boolean = sUseEmojiBW && isEmoji(codePoint)

        /** Toggled by user preference; affects which emoji font is selected. */
        var sUseEmojiBW: Boolean = false

        fun fromXml(name: String, size: String, style: UByte, fontFiles: List<FontFileInfo> = emptyList(), fontCollectionFiles: List<FontFileInfo> = emptyList()): FontDescriptor =
            FontDescriptor(name, size, style, fontFiles, fontCollectionFiles)

        fun resolveFunctor(functorName: String): ((Int) -> Boolean)? = charFunctors[functorName]
    }
}

class FontRegistry(
    private val createGlTextures: Boolean,
    private val fontSizeMod: Float
) {
    private val fontMap: MutableMap<FontDescriptor, FontGL?> = mutableMapOf()
    private val fontSizes: MutableMap<String, Float> = mutableMapOf()
    private val ultimateFallbackList: List<String> = getDynamicFallbackFontList()

    /**
     * Parse `fonts.xml`-style configuration into the registry. The schema we
     * support is a relaxed subset of the C++ viewer's `<font>` definitions:
     * each `<font>` declares a `name`, `style` and one or more `<file>` lines;
     * `<font_size>` entries map size names to point sizes.
     */
    fun parseFontInfo(xmlFilename: String): Boolean {
        val file = File(xmlFilename)
        if (!file.exists()) return false
        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val root = doc.documentElement
            val fontNodes = root.getElementsByTagName("font")
            for (i in 0 until fontNodes.length) {
                val el = fontNodes.item(i) as? Element ?: continue
                val name = el.getAttribute("name") ?: continue
                val styleStr = el.getAttribute("style")
                val style = (when (styleStr.uppercase()) {
                    "BOLD" -> FontDescriptor.BOLD
                    "ITALIC" -> FontDescriptor.ITALIC
                    else -> 0
                }).toUByte()
                val files = collectFiles(el, "file")
                val collection = collectFiles(el, "file_collection")
                val desc = FontDescriptor(name, FontDescriptor.TEMPLATE_STRING, style, files, collection)
                fontMap[desc] = null
            }
            val sizeNodes = root.getElementsByTagName("font_size")
            for (i in 0 until sizeNodes.length) {
                val el = sizeNodes.item(i) as? Element ?: continue
                val n = el.getAttribute("name").orEmpty()
                val s = el.getAttribute("size")?.toFloatOrNull() ?: continue
                fontSizes[n] = s + fontSizeMod
            }
            true
        } catch (e: Exception) {
            System.err.println("parseFontInfo($xmlFilename) failed: ${e.message}")
            false
        }
    }

    private fun collectFiles(el: Element, tag: String): List<FontFileInfo> {
        val items = el.getElementsByTagName(tag)
        return (0 until items.length).mapNotNull { idx ->
            val node = items.item(idx) as? Element ?: return@mapNotNull null
            val path = node.textContent?.trim().orEmpty()
            if (path.isEmpty()) null
            else FontFileInfo(path, FontDescriptor.resolveFunctor(node.getAttribute("functor")))
        }
    }

    fun reset() {
        fontMap.values.filterNotNull().forEach { it.reset() }
    }

    fun clear() {
        fontMap.clear()
    }

    fun destroyGL() {
        fontMap.values.filterNotNull().forEach { it.destroyGl() }
    }

    fun getFont(desc: FontDescriptor): FontGL? {
        fontMap[desc]?.let { return it }
        val font = createFont(desc)
        if (font == null) {
            System.err.println("getFont failed, name=${desc.name} style=[${desc.style}] size=[${desc.size}]")
        } else {
            font.generateAsciiGlyphs()
        }
        return font
    }

    fun getMatchingFontDesc(desc: FontDescriptor): FontDescriptor? {
        val norm = desc.normalize()
        return if (fontMap.containsKey(norm)) norm else null
    }

    fun getClosestFontTemplate(desc: FontDescriptor): FontDescriptor? {
        getMatchingFontDesc(desc)?.let { return it }
        val norm = desc.normalize()
        var bestMatch: FontDescriptor? = null
        for (candidate in fontMap.keys) {
            if (!candidate.isTemplate()) continue
            if (candidate.name != norm.name) continue
            if (candidate.style.toInt() and norm.style.toInt().inv() != 0) continue
            if (bestMatch == null) { bestMatch = candidate; continue }
            val bestBits = bitCount((norm.style.toInt() and bestMatch!!.style.toInt()).toUByte())
            val currBits = bitCount((norm.style.toInt() and candidate.style.toInt()).toUByte())
            if (currBits > bestBits) { bestMatch = candidate; continue }
            if (candidate.style.toInt() and FontDescriptor.BOLD != 0) { bestMatch = candidate }
        }
        return bestMatch
    }

    fun nameToSize(sizeName: String): Float? = fontSizes[sizeName]

    fun dump() {
        println("FontRegistry dump:")
        fontSizes.forEach { (k, v) -> println("  Size: $k => $v") }
        fontMap.keys.forEach { desc ->
            println("  Font: name=${desc.name} style=[${desc.style}] size=[${desc.size}]")
            desc.fontFiles.forEach { println("    file: ${it.fileName}") }
        }
    }

    fun dumpTextures() {
        fontMap.values.filterNotNull().forEach { it.dumpTextures() }
    }

    fun getUltimateFallbackList(): List<String> = ultimateFallbackList

    private fun createFont(desc: FontDescriptor): FontGL? {
        val norm = desc.normalize()
        val pointSize = nameToSize(norm.size) ?: run {
            System.err.println("createFont unrecognized size ${norm.size}")
            return null
        }

        val templateDesc = norm.withSize(FontDescriptor.TEMPLATE_STRING)
        val matchDesc = getClosestFontTemplate(templateDesc) ?: run {
            System.err.println("createFont failed, no template found for ${norm.name} style [${norm.style}]")
            return null
        }

        val nearestExact = matchDesc.withSize(norm.size)
        fontMap[nearestExact]?.let { existing ->
            val font = FontGL()
            fontMap[desc] = font
            return font
        }

        val fontFiles = matchDesc.fontFiles.toMutableList()
        val fontCollectionFiles = matchDesc.fontCollectionFiles.toMutableList()

        val defaultDesc = FontDescriptor("default", FontDescriptor.TEMPLATE_STRING, 0u)
        getMatchingFontDesc(defaultDesc)?.let { defMatch ->
            fontFiles.addAll(defMatch.fontFiles)
            fontCollectionFiles.addAll(defMatch.fontCollectionFiles)
        }

        ultimateFallbackList.mapTo(fontFiles) { FontFileInfo(it) }

        if (fontFiles.isEmpty()) {
            System.err.println("createFont failed, no file names specified")
            return null
        }

        val searchPaths = listOf(
            FontGL.getFontPathLocal(),
            FontGL.getFontPathSystem(),
            "${System.getProperty("user.home")}/user_settings/fonts/",
            "${System.getProperty("user.dir")}/fonts/"
        )

        val font = FontGL()
        var loaded = false
        for (info in fontFiles) {
            for (path in searchPaths) {
                val full = if (info.fileName.startsWith("/")) info.fileName else "$path${info.fileName}"
                if (File(full).exists()) {
                    if (font.loadFace(full, pointSize, FontGL.vertDpi, FontGL.horizDpi, isFallback = loaded, faceIndex = 0)) {
                        loaded = true
                        break
                    }
                }
            }
            if (loaded) break
        }
        if (!loaded) {
            // No font file actually exists in the search path. Fall back to a
            // metric-only FontGL (we still report sensible widths from
            // pointSize); render() will draw replacement glyphs.
            font.loadFace("synthetic:${norm.name}/${norm.size}", pointSize, FontGL.vertDpi, FontGL.horizDpi, isFallback = false, faceIndex = 0)
        }
        fontMap[desc] = font
        return font
    }

    companion object {
        /**
         * Best-effort discovery of the platform's fallback font list. On Linux
         * this normally requires `fc-list`; we approximate by listing well-
         * known directories. Failures yield an empty list.
         */
        private fun getDynamicFallbackFontList(): List<String> {
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            val dirs = when {
                "win" in osName -> listOf(System.getenv("WINDIR")?.let { "$it/Fonts" } ?: "C:/Windows/Fonts")
                "mac" in osName || "darwin" in osName -> listOf("/System/Library/Fonts", "/Library/Fonts")
                else -> listOf("/usr/share/fonts", "/usr/local/share/fonts")
            }
            val results = mutableListOf<String>()
            for (dir in dirs) {
                val f = File(dir)
                if (!f.exists()) continue
                f.walkTopDown().filter {
                    val n = it.name.lowercase()
                    it.isFile && (n.endsWith(".ttf") || n.endsWith(".otf") || n.endsWith(".ttc"))
                }.take(16).mapTo(results) { it.absolutePath }
            }
            return results
        }

        private fun bitCount(v: UByte): Int = Integer.bitCount(v.toInt())
    }
}
