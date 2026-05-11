package com.firestorm.llrender

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

        private fun isEmoji(codePoint: Int): Boolean {
            TODO("APR: use JVM equivalent for emoji detection (Character.getType or ICU4J)")
        }

        private fun isEmojiUseColor(codePoint: Int): Boolean {
            TODO("APR: use JVM equivalent; return isEmoji(codePoint) when FSUseEmojiBW is false")
        }

        private fun isEmojiUseBW(codePoint: Int): Boolean {
            TODO("APR: use JVM equivalent; return isEmoji(codePoint) when FSUseEmojiBW is true")
        }

        fun fromXml(name: String, size: String, style: UByte, fontFiles: List<FontFileInfo> = emptyList(), fontCollectionFiles: List<FontFileInfo> = emptyList()): FontDescriptor =
            FontDescriptor(name, size, style, fontFiles, fontCollectionFiles)

        fun resolveFunctor(functorName: String): ((Int) -> Boolean)? = charFunctors[functorName]
    }
}

class FontGL {
    var fontDescriptor: FontDescriptor = FontDescriptor()

    fun reset() { TODO("GPU: reset cached glyph textures") }
    fun destroyGL() { TODO("GPU: destroy OpenGL glyph texture resources") }
    fun generateAsciiGlyphs() { TODO("GPU: pre-render ASCII glyphs into texture atlas") }
    fun dumpTextures() { TODO("GPU: log texture atlas info") }
    fun getNumFaces(fontPath: String): Int { TODO("APR: use JVM font loading to count faces") }
    fun loadFace(fontPath: String, pointSize: Float, vertDpi: Float, horizDpi: Float, isFallback: Boolean, faceIndex: Int): Boolean {
        TODO("APR: use JVM font loading (e.g. java.awt.Font or FreeType JNI)")
    }

    companion object {
        var sVertDPI: Float = 96f
        var sHorizDPI: Float = 96f

        fun getFontPathLocal(): String { TODO("APR: use JVM equivalent for local font path") }
        fun getFontPathSystem(): String { TODO("APR: use JVM equivalent for system font path") }
        fun getStyleFromString(style: String): UByte { TODO("APR: parse style string to bitmask") }
    }
}

class FontRegistry(
    private val createGlTextures: Boolean,
    private val fontSizeMod: Float
) {
    private val fontMap: MutableMap<FontDescriptor, FontGL?> = mutableMapOf()
    private val fontSizes: MutableMap<String, Float> = mutableMapOf()
    private val ultimateFallbackList: List<String> = getDynamicFallbackFontList()

    fun parseFontInfo(xmlFilename: String): Boolean {
        TODO("APR: use JVM XML parser; populate fontMap templates and fontSizes from <font>/<font_size> elements; apply fontSizeMod to size values")
    }

    fun reset() {
        fontMap.values.filterNotNull().forEach { it.reset() }
    }

    fun clear() {
        fontMap.clear()
    }

    fun destroyGL() {
        fontMap.values.filterNotNull().forEach { it.destroyGL() }
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
            font.fontDescriptor = desc
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

        val fontSearchPaths = mutableListOf(
            FontGL.getFontPathLocal(),
            FontGL.getFontPathSystem()
        )
        TODO("APR: use JVM equivalent for user_settings/fonts and executable paths; load each FontFileInfo; assemble FreeType fallback chain; store result in fontMap[desc]")
    }

    companion object {
        private fun getDynamicFallbackFontList(): List<String> {
            TODO("APR: use JVM equivalent for platform dynamic font fallback list (Linux fc-list, etc.)")
        }

        private fun bitCount(v: UByte): Int = Integer.bitCount(v.toInt())
    }
}
