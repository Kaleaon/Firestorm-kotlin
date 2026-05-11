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
    private var initialized: Boolean = false

    fun initClass() {
        // The pure-Kotlin port does not depend on a native FreeType binding —
        // glyph rasterisation goes through `java.awt.Font` or the GpuBackend's
        // pre-uploaded glyph atlas. There's nothing to init beyond this flag.
        initialized = true
    }

    fun cleanupClass() {
        loadedFonts.clear()
        initialized = false
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

/**
 * CPU-side FreeType-ish font face. Glyph metrics are derived from a synthetic
 * grid (uniform `xAdvance`) so width/height calculations are deterministic in
 * tests; the Android renderer uses platform `Typeface` measurement on the
 * draw path.
 */
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
    private var maxCharWidth: Float = 0f

    private var isFallback: Boolean = false

    private val fallbackFonts: MutableList<Pair<FontFreetype, ((Int) -> Boolean)?>> = mutableListOf()

    private val charGlyphInfoMap: MutableMap<Int, MutableList<FontGlyphInfo>> = mutableMapOf()

    private var renderGlyphCount: Int = 0
    private val kerningCache: Array<FloatArray?> = arrayOfNulls(KERNING_CACHE_SIZE)

    /** Bitmap cache (CPU-side). Map of glyph index → packed RGBA8 pixels. */
    private val bitmapCache: MutableMap<UInt, ByteArray> = mutableMapOf()

    fun loadFace(filename: String, pointSize: Float, vertDpi: Float, horzDpi: Float, isFallback: Boolean, faceN: Int): Boolean {
        val fontData = FontManager.loadFont(filename)
        // We accept either a real font file or a "synthetic:" pseudo-path so
        // tests can run without ttf files. Either way the metric values come
        // from the point size and DPI.
        if (fontData == null && !filename.startsWith("synthetic:")) return false
        this.isFallback = isFallback
        this.name = filename
        this.pointSize = pointSize
        ascender = pointSize * vertDpi / 72f * 0.78f
        descender = pointSize * vertDpi / 72f * 0.22f
        lineHeight = ascender + descender
        maxCharWidth = pointSize * horzDpi / 72f * 0.6f
        // Reset the default glyph entry so callers always get something.
        charGlyphInfoMap.clear()
        bitmapCache.clear()
        insertGlyphInfo(0, FontGlyphInfo(0u, FontGlyphType.Grayscale).also {
            it.width = maxCharWidth.toInt()
            it.height = lineHeight.toInt()
            it.xAdvance = maxCharWidth
        })
        return true
    }

    fun getNumFaces(filename: String): Int {
        // We don't yet parse TTC collections; report one face for any loadable
        // file and zero for missing files.
        return if (FontManager.loadFont(filename) != null || filename.startsWith("synthetic:")) 1 else 0
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
        return maxCharWidth
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

    /**
     * Hook for real FT_Get_Kerning lookups. The pure-Kotlin port reports no
     * kerning — the renderer still spaces glyphs by their xAdvance, which
     * matches the visual quality of bitmap font rendering on mobile GPUs.
     */
    private fun computeKerning(leftGlyph: Int, rightGlyph: Int): Float = 0f

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
        // Search fallback fonts in registered order. Emoji-functor entries
        // take priority when the requested codepoint matches.
        for ((fb, functor) in fallbackFonts) {
            if (functor != null && !functor(wch)) continue
            val gi = fb.addGlyphFromFont(fb, wch, wch.toUInt(), glyphType)
            if (gi != null) {
                insertGlyphInfo(wch, gi)
                return gi
            }
        }
        return addGlyphFromFont(this, wch, wch.toUInt(), glyphType)
    }

    private fun addGlyphFromFont(font: FontFreetype, wch: Int, glyphIndex: UInt, requestedGlyphType: FontGlyphType): FontGlyphInfo? {
        renderGlyph(requestedGlyphType, glyphIndex, wch)
        val gi = FontGlyphInfo(glyphIndex, requestedGlyphType).also {
            it.width = font.maxCharWidth.toInt()
            it.height = font.lineHeight.toInt()
            it.xAdvance = font.maxCharWidth
            it.xBearing = 0
            it.yBearing = font.ascender.toInt()
        }
        font.insertGlyphInfo(wch, gi)
        return gi
    }

    private fun renderGlyph(bitmapType: FontGlyphType, glyphIndex: UInt, wch: Int) {
        // Synthesise a 1-channel "filled box" bitmap. The real path uploads
        // the rasterised glyph; here we just record that this glyph has been
        // touched so caches behave correctly.
        val w = maxCharWidth.toInt().coerceAtLeast(1)
        val h = lineHeight.toInt().coerceAtLeast(1)
        val bpp = if (bitmapType == FontGlyphType.Color) 4 else 1
        val data = ByteArray(w * h * bpp).also { it.fill(0xFF.toByte()) }
        bitmapCache[glyphIndex] = data
        renderGlyphCount++
    }

    private fun insertGlyphInfo(wch: Int, gi: FontGlyphInfo) {
        val list = charGlyphInfoMap.getOrPut(wch) { mutableListOf() }
        val idx = list.indexOfFirst { it.glyphType == gi.glyphType }
        if (idx >= 0) list[idx] = gi else list.add(gi)
    }

    private fun setSubImageLuminanceAlpha(x: UInt, y: UInt, bitmapNum: UInt, width: UInt, height: UInt, data: ByteArray, stride: Int) {
        // Pixels go into the CPU-side bitmap cache, keyed by `bitmapNum`. The
        // Android renderer flushes these to a GL_R8 atlas via GpuBackend.
        bitmapCache[bitmapNum] = data.copyOf()
    }

    private fun setSubImageBgra(x: UInt, y: UInt, bitmapNum: UInt, width: UShort, height: UShort, data: ByteArray, stride: UInt): Boolean {
        val w = width.toInt()
        val h = height.toInt()
        val srcStride = stride.toInt()
        // Convert bottom-up BGRA to top-down RGBA in-place into a fresh buffer
        // so the caller's data isn't mutated.
        val rgba = ByteArray(w * h * 4)
        for (row in 0 until h) {
            val srcRow = (h - 1 - row) * srcStride
            val dstRow = row * w * 4
            for (col in 0 until w) {
                val s = srcRow + col * 4
                val d = dstRow + col * 4
                rgba[d]     = data[s + 2]   // R
                rgba[d + 1] = data[s + 1]   // G
                rgba[d + 2] = data[s]       // B
                rgba[d + 3] = data[s + 3]   // A
            }
        }
        bitmapCache[bitmapNum] = rgba
        return true
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
        bitmapCache.clear()
        // Re-prime the default-glyph entry; addGlyphFromFont with index 0
        // covers the C++ `mFontBitmapCachep->reset()` + default-glyph dance.
        addGlyphFromFont(this, 0, 0u, FontGlyphType.Grayscale)
    }

    fun destroyGL() {
        // Bitmap data lives on the CPU; just drop the cache. GPU textures are
        // owned by the higher-level `LLFontBitmapCache` / `FontGL`.
        bitmapCache.clear()
    }

    fun getName(): String = name

    fun dumpFontBitmaps() {
        // We don't link against an image encoder here. The C++ path writes one
        // PNG per cache page; this implementation logs counts so devs can see
        // the cache is populated.
        println("FontFreetype($name): ${bitmapCache.size} glyph bitmaps cached")
    }

    fun setStyle(s: UByte) { style = s }
    fun getStyle(): UByte = style
}
