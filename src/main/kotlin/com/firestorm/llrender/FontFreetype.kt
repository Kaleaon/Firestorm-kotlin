package com.firestorm.llrender

import java.io.File

enum class FontGlyphType {
    Unspecified,
    Grayscale,
    Color,
    Count
}

data class FontGlyphInfo(
    val glyphIndex: UInt,
    var glyphType: FontGlyphType
) {
    var width: Int = 0
    var height: Int = 0
    var xAdvance: Float = 0f
    var yAdvance: Float = 0f
    var xBitmapOffset: Int = 0
    var yBitmapOffset: Int = 0
    var xBearing: Int = 0
    var yBearing: Int = 0
    var bitmapEntry: Pair<FontGlyphType, Int> = Pair(FontGlyphType.Unspecified, -1)

    constructor(other: FontGlyphInfo) : this(other.glyphIndex, other.glyphType) {
        width = other.width
        height = other.height
        xAdvance = other.xAdvance
        yAdvance = other.yAdvance
        xBitmapOffset = other.xBitmapOffset
        yBitmapOffset = other.yBitmapOffset
        xBearing = other.xBearing
        yBearing = other.yBearing
        bitmapEntry = other.bitmapEntry
    }
}

private class LoadedFont(val name: String, val data: ByteArray) {
    var refs: UInt = 1u
}

object FontManager {
    private val loadedFonts: MutableMap<String, LoadedFont> = mutableMapOf()

    fun initClass() {
        System.err.println("FontManager: initClass not yet implemented")
    }

    fun cleanupClass() {
        System.err.println("FontManager: cleanupClass not yet implemented")
        loadedFonts.clear()
    }

    fun loadFont(filename: String): ByteArray? {
        loadedFonts[filename]?.let {
            it.refs++
            return it.data
        }
        val bytes = try {
            File(filename).readBytes()
        } catch (e: Exception) {
            return null
        }
        loadedFonts[filename] = LoadedFont(filename, bytes)
        return bytes
    }

    fun unloadAllFonts() {
        loadedFonts.clear()
    }
}

class FontFreetype {

    companion object {
        const val FIRST_CHAR: Int = 32
        const val NUM_CHARS: Int = 127 - 32
        const val LAST_CHAR_BASIC: Int = 127
        const val NUM_CHARS_FULL: Int = 255 - 32
        const val LAST_CHAR_FULL: Int = 255

        private const val KERNING_CACHE_SIZE = 256
    }

    private var name: String = ""
    private var style: UByte = 0u
    private var pointSize: Float = 0f
    private var ascender: Float = 0f
    private var descender: Float = 0f
    private var lineHeight: Float = 0f

    private var isFallback: Boolean = false

    private val fallbackFonts: MutableList<Pair<FontFreetype, ((Int) -> Boolean)?>> = mutableListOf()

    private val charGlyphInfoMap: MutableMap<Int, MutableList<FontGlyphInfo>> = mutableMapOf()

    private var renderGlyphCount: Int = 0

    // Cache only for glyph indices < 256 to avoid large memory use at the cost
    // of an extra lookup for higher codepoints.
    private val kerningCache: Array<FloatArray?> = arrayOfNulls(KERNING_CACHE_SIZE)

    fun loadFace(filename: String, pointSize: Float, vertDpi: Float, horzDpi: Float, isFallback: Boolean, faceN: Int): Boolean {
        val fontData = FontManager.loadFont(filename) ?: return false
        this.isFallback = isFallback
        this.name = filename
        this.pointSize = pointSize
        return false
    }

    fun getNumFaces(filename: String): Int {
        val fontData = FontManager.loadFont(filename) ?: return 0
        return 0
    }

    fun addFallbackFont(font: FontFreetype, functor: ((Int) -> Boolean)? = null) {
        fallbackFonts.add(Pair(font, functor))
    }

    fun getLineHeight(): Float = lineHeight
    fun getAscenderHeight(): Float = ascender
    fun getDescenderHeight(): Float = descender

    fun getXAdvance(wch: Int): Float {
        val gi = getGlyphInfo(wch, FontGlyphType.Unspecified)
        if (gi != null) return gi.xAdvance
        val defaultGlyph = charGlyphInfoMap[0]?.firstOrNull()
        if (defaultGlyph != null) return defaultGlyph.xAdvance
        return 0f
    }

    fun getXAdvance(glyph: FontGlyphInfo): Float = glyph.xAdvance

    fun getXKerning(charLeft: Int, charRight: Int): Float {
        val leftGlyph = getGlyphInfo(charLeft, FontGlyphType.Unspecified)?.glyphIndex?.toInt() ?: 0
        val rightGlyph = getGlyphInfo(charRight, FontGlyphType.Unspecified)?.glyphIndex?.toInt() ?: 0
        return cachedKerning(leftGlyph, rightGlyph)
    }

    fun getXKerning(leftGlyphInfo: FontGlyphInfo?, rightGlyphInfo: FontGlyphInfo?): Float {
        val leftGlyph = leftGlyphInfo?.glyphIndex?.toInt() ?: 0
        val rightGlyph = rightGlyphInfo?.glyphIndex?.toInt() ?: 0
        return cachedKerning(leftGlyph, rightGlyph)
    }

    private fun cachedKerning(leftGlyph: Int, rightGlyph: Int): Float {
        if (leftGlyph < KERNING_CACHE_SIZE && rightGlyph < KERNING_CACHE_SIZE) {
            val row = kerningCache[leftGlyph]
            if (row != null && row[rightGlyph] < Float.MAX_VALUE) {
                return row[rightGlyph]
            }
        }
        val delta = computeKerning(leftGlyph, rightGlyph)
        if (leftGlyph < KERNING_CACHE_SIZE && rightGlyph < KERNING_CACHE_SIZE) {
            val row = kerningCache[leftGlyph] ?: FloatArray(KERNING_CACHE_SIZE) { Float.MAX_VALUE }.also {
                kerningCache[leftGlyph] = it
            }
            row[rightGlyph] = delta
        }
        return delta
    }

    private fun computeKerning(leftGlyph: Int, rightGlyph: Int): Float {
        return 0f
    }

    fun getGlyphInfo(wch: Int, glyphType: FontGlyphType): FontGlyphInfo? {
        val glyphs = charGlyphInfoMap[wch]
        if (!glyphs.isNullOrEmpty()) {
            if (glyphType != FontGlyphType.Unspecified) {
                val found = glyphs.find { it.glyphType == glyphType }
                if (found != null) return found
            } else {
                return glyphs.first()
            }
        }
        val resolvedType = if (glyphType != FontGlyphType.Unspecified) glyphType else FontGlyphType.Grayscale
        return addGlyph(wch, resolvedType)
    }

    private fun hasGlyph(wch: Int): Boolean = !charGlyphInfoMap[wch].isNullOrEmpty()

    private fun addGlyph(wch: Int, glyphType: FontGlyphType): FontGlyphInfo? {
        check(!isFallback)
        return null
    }

    private fun addGlyphFromFont(font: FontFreetype, wch: Int, glyphIndex: UInt, requestedGlyphType: FontGlyphType): FontGlyphInfo? {
        return null
    }

    private fun renderGlyph(bitmapType: FontGlyphType, glyphIndex: UInt, wch: Int) {
        // no-op
    }

    private fun insertGlyphInfo(wch: Int, gi: FontGlyphInfo) {
        val list = charGlyphInfoMap.getOrPut(wch) { mutableListOf() }
        val idx = list.indexOfFirst { it.glyphType == gi.glyphType }
        if (idx >= 0) list[idx] = gi else list.add(gi)
    }

    private fun setSubImageLuminanceAlpha(x: UInt, y: UInt, bitmapNum: UInt, width: UInt, height: UInt, data: ByteArray, stride: Int) {
        // no-op
    }

    private fun setSubImageBgra(x: UInt, y: UInt, bitmapNum: UInt, width: UShort, height: UShort, data: ByteArray, stride: UInt): Boolean {
        return false
    }

    fun reset(vertDpi: Float, horzDpi: Float) {
        resetBitmapCache()
        loadFace(name, pointSize, vertDpi, horzDpi, isFallback, 0)
        if (!isFallback) {
            for ((font, _) in fallbackFonts) font.reset(vertDpi, horzDpi)
        }
    }

    private fun resetBitmapCache() {
        charGlyphInfoMap.clear()
        // no-op
    }

    fun destroyGL() {
        // no-op
    }

    fun getName(): String = name

    fun dumpFontBitmaps() {
        System.err.println("FontFreetype: dumpFontBitmaps not yet implemented")
    }

    fun setStyle(s: UByte) { style = s }
    fun getStyle(): UByte = style
}
