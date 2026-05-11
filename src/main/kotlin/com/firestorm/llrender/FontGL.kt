package com.firestorm.llrender

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Color4u
import com.firestorm.llmath.Rect
import com.firestorm.llmath.RectF
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Pure-Kotlin replacement for the C++ `LLFontGL` viewer class. Glyph
 * rasterisation and FreeType bindings are out of scope at this layer; this
 * implementation maintains font metrics, descriptor bookkeeping, and emits
 * rendering commands through [GpuBackend.current] / [TextRenderer].
 *
 * For the Android app, the production path is `LLFontGL` (see
 * `LLFontGL.kt`); this class exists so the converted llrender code base
 * compiles standalone and exposes a deterministic API for unit tests.
 */
class FontGL {

    enum class HAlign { LEFT, RIGHT, HCENTER }
    enum class VAlign { TOP, VCENTER, BASELINE, BOTTOM }
    enum class ShadowType { NO_SHADOW, DROP_SHADOW, DROP_SHADOW_SOFT }
    enum class WordWrapStyle { ONLY_WORD_BOUNDARIES, WORD_BOUNDARY_IF_POSSIBLE, ANYWHERE }

    companion object {
        const val STYLE_NORMAL:    Int = 0x00
        const val STYLE_BOLD:      Int = 0x01
        const val STYLE_ITALIC:    Int = 0x02
        const val STYLE_UNDERLINE: Int = 0x04

        private const val BOLD_OFFSET = 1
        private const val PAD_UVY = 0.5f
        private const val DROP_SHADOW_SOFT_STRENGTH = 0.3f

        // ---- Global state --------------------------------------------------

        var vertDpi: Float = 96f
        var horizDpi: Float = 96f
        var scaleX: Float = 1f
        var scaleY: Float = 1f
        var resolutionGeneration: Int = 0
        var displayFont: Boolean = true
        var appDir: String = ""
        var shadowColor: Color4 = Color4(0f, 0f, 0f, 1f)

        var curOriginX: Int = 0
        var curOriginY: Int = 0
        var curDepth: Float = 0f
        val originStack: MutableList<Triple<Int, Int, Float>> = mutableListOf()

        private val fontCache: MutableMap<FontDescriptor, FontGL> = mutableMapOf()

        // ---- Lifecycle ------------------------------------------------------

        fun initClass(
            screenDpi: Float,
            xScale: Float,
            yScale: Float,
            appDir: String,
            fontsFile: String,
            sizeMod: Float = 0f,
            createGlTextures: Boolean = true
        ) {
            vertDpi   = floor(screenDpi * yScale)
            horizDpi  = floor(screenDpi * xScale)
            scaleX    = xScale
            scaleY    = yScale
            Companion.appDir = appDir
            // Defer file-driven font registry to FontRegistry. We always have
            // a working set of synthetic descriptors so accessors below never
            // return null.
            fontCache.clear()
            resolutionGeneration++
        }

        fun loadDefaultFonts(): Boolean {
            sequenceOf(
                FontDescriptor("SansSerif", "Medium", STYLE_NORMAL),
                FontDescriptor("Monospace", "Monospace", STYLE_NORMAL),
                FontDescriptor("Scripting", "Scripting", STYLE_NORMAL)
            ).forEach { getOrLoad(it.name, it.size, it.style) }
            return true
        }

        fun loadCommonFonts() {
            sequenceOf(
                "SansSerif" to listOf("Medium", "Large", "Huge"),
                "Monospace" to listOf("Medium")
            ).flatMap { (name, sizes) ->
                sizes.flatMap { sz ->
                    sequenceOf(STYLE_NORMAL, STYLE_BOLD).map { style ->
                        FontDescriptor(name, sz, style)
                    }
                }
            }.forEach { getOrLoad(it.name, it.size, it.style) }
        }

        fun destroyDefaultFonts() {
            fontCache.values.forEach { it.destroyGl() }
            fontCache.clear()
        }

        fun destroyAllGl() {
            fontCache.values.forEach { it.destroyGl() }
        }

        fun getFontSansSerifSmall():      FontGL = getOrLoad("SansSerif",  "Small",      STYLE_NORMAL)
        fun getFontSansSerifSmallBold():  FontGL = getOrLoad("SansSerif",  "Small",      STYLE_BOLD)
        fun getFontSansSerifSmallItalic():FontGL = getOrLoad("SansSerif",  "Small",      STYLE_ITALIC)
        fun getFontSansSerif():           FontGL = getOrLoad("SansSerif",  "Medium",     STYLE_NORMAL)
        fun getFontSansSerifBig():        FontGL = getOrLoad("SansSerif",  "Large",      STYLE_NORMAL)
        fun getFontSansSerifHuge():       FontGL = getOrLoad("SansSerif",  "Huge",       STYLE_NORMAL)
        fun getFontSansSerifBold():       FontGL = getOrLoad("SansSerif",  "Medium",     STYLE_BOLD)
        fun getFontMonospace():           FontGL = getOrLoad("Monospace",  "Monospace",  STYLE_NORMAL)
        fun getFontScripting():           FontGL = getOrLoad("Scripting",  "Scripting",  STYLE_NORMAL)
        fun getFontOcra():                FontGL = getOrLoad("OCRA",       "Monospace",  STYLE_NORMAL)
        fun getFontCascadia():            FontGL = getOrLoad("Cascadia",   "Cascadia",   STYLE_NORMAL)
        fun getFontEmojiSmall(bw: Boolean  = false): FontGL = getOrLoad(if (bw) "EmojiBW" else "Emoji", "Small",  STYLE_NORMAL)
        fun getFontEmojiMedium(bw: Boolean = false): FontGL = getOrLoad(if (bw) "EmojiBW" else "Emoji", "Medium", STYLE_NORMAL)
        fun getFontEmojiLarge(bw: Boolean  = false): FontGL = getOrLoad(if (bw) "EmojiBW" else "Emoji", "Large",  STYLE_NORMAL)
        fun getFontEmojiHuge(bw: Boolean   = false): FontGL = getOrLoad(if (bw) "EmojiBW" else "Emoji", "Huge",   STYLE_NORMAL)
        fun getFontDefault(): FontGL = getFontSansSerif()

        fun getFontByName(name: String): FontGL? = when (name) {
            "SANSSERIF"       -> getFontSansSerif()
            "SANSSERIF_SMALL" -> getFontSansSerifSmall()
            "SANSSERIF_BIG"   -> getFontSansSerifBig()
            "SMALL"           -> getFontMonospace()
            "OCRA"            -> getFontOcra()
            "Scripting"       -> getFontScripting()
            "Monospace"       -> getFontMonospace()
            "Cascadia"        -> getFontCascadia()
            else              -> null
        }

        fun nameFromHAlign(align: HAlign): String = when (align) {
            HAlign.LEFT    -> "left"
            HAlign.RIGHT   -> "right"
            HAlign.HCENTER -> "center"
        }

        fun hAlignFromName(name: String): HAlign = when (name) {
            "right"  -> HAlign.RIGHT
            "center" -> HAlign.HCENTER
            else     -> HAlign.LEFT
        }

        fun nameFromVAlign(align: VAlign): String = when (align) {
            VAlign.TOP      -> "top"
            VAlign.VCENTER  -> "center"
            VAlign.BASELINE -> "baseline"
            VAlign.BOTTOM   -> "bottom"
        }

        fun vAlignFromName(name: String): VAlign = when (name) {
            "top"      -> VAlign.TOP
            "center"   -> VAlign.VCENTER
            "bottom"   -> VAlign.BOTTOM
            else       -> VAlign.BASELINE
        }

        fun getStyleFromString(style: String): Int {
            var ret = STYLE_NORMAL
            if ("BOLD"      in style) ret = ret or STYLE_BOLD
            if ("ITALIC"    in style) ret = ret or STYLE_ITALIC
            if ("UNDERLINE" in style) ret = ret or STYLE_UNDERLINE
            return ret
        }

        fun getStringFromStyle(style: Int): String = buildString {
            if (style == STYLE_NORMAL)       append("|NORMAL")
            if (style and STYLE_BOLD      != 0) append("|BOLD")
            if (style and STYLE_ITALIC    != 0) append("|ITALIC")
            if (style and STYLE_UNDERLINE != 0) append("|UNDERLINE")
        }

        /** OS-conventional system font directory. */
        fun getFontPathSystem(): String {
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            return when {
                "win" in osName -> System.getenv("WINDIR")?.let { "$it/Fonts/" } ?: "C:/Windows/Fonts/"
                "mac" in osName || "darwin" in osName -> "/System/Library/Fonts/"
                else -> "/usr/share/fonts/"
            }
        }

        fun getFontPathLocal(): String =
            if (appDir.isNotEmpty()) "$appDir/fonts/" else "./fonts/"

        fun nameFromFont(font: FontGL): String = font.descriptor.name
        fun sizeFromFont(font: FontGL):  String = font.descriptor.size

        fun dumpFonts() {
            fontCache.values.forEach { println("FontGL ${it.descriptor}") }
        }

        fun dumpFontTextures() {
            fontCache.values.forEach { it.dumpTextures() }
        }

        private fun getOrLoad(family: String, size: String, style: Int): FontGL {
            val desc = FontDescriptor(family, size, style)
            return fontCache.getOrPut(desc) { FontGL().also { it.descriptor = desc } }
        }
    }

    data class FontDescriptor(val name: String, val size: String, val style: Int)

    var descriptor: FontDescriptor = FontDescriptor("SansSerif", "Medium", STYLE_NORMAL)
        private set

    private var pointSize: Float = pointSizeFor(descriptor.size)
    private var ascender: Float = pointSize * 0.78f
    private var descender: Float = pointSize * 0.22f
    private var lineHeight: Int = (pointSize * 1.2f).toInt()
    private var maxCharWidth: Float = pointSize * 0.6f
    private var cacheGeneration: Int = 0
    private var glyphTextures: IntArray = IntArray(0)

    // -------------------------------------------------------------------------
    // Face loading
    // -------------------------------------------------------------------------

    fun loadFace(
        filename: String,
        pointSize: Float,
        vertDpi: Float,
        horzDpi: Float,
        isFallback: Boolean,
        faceIndex: Int
    ): Boolean {
        this.pointSize = pointSize
        ascender = pointSize * vertDpi / 72f * 0.78f
        descender = pointSize * vertDpi / 72f * 0.22f
        lineHeight = ceil(ascender + descender).toInt()
        maxCharWidth = pointSize * horzDpi / 72f * 0.6f
        cacheGeneration++
        return true
    }

    fun getNumFaces(filename: String): Int = 1

    fun getCacheGeneration(): Int = cacheGeneration

    fun reset() {
        cacheGeneration++
        destroyGl()
    }

    fun destroyGl() {
        if (glyphTextures.isNotEmpty()) {
            GpuBackend.current.deleteTextures(glyphTextures)
            glyphTextures = IntArray(0)
        }
    }

    fun generateAsciiGlyphs() {
        // Pre-rasterising is the LLFontFreetype responsibility; this layer
        // just tracks that ASCII has been requested so unit tests can assert.
        cacheGeneration++
    }

    fun dumpTextures() {
        println("FontGL($descriptor): ${glyphTextures.size} glyph textures")
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    fun getAscenderHeight(): Float = ascender
    fun getDescenderHeight(): Float = descender
    fun getLineHeight(): Int = lineHeight

    // -------------------------------------------------------------------------
    // Width measurement
    // -------------------------------------------------------------------------

    fun getWidth(utf8text: String): Int = getWidth(utf8text, 0, Int.MAX_VALUE)
    fun getWidth(utf8text: String, offset: Int, maxChars: Int): Int =
        getWidthF32(utf8text, offset, maxChars).let { f -> (f + 0.5f).toInt() }

    fun getWidthF32(utf8text: String): Float = getWidthF32(utf8text, 0, Int.MAX_VALUE)
    fun getWidthF32(utf8text: String, offset: Int, maxChars: Int): Float {
        val end = minOf(utf8text.length, offset + maxChars)
        if (offset >= end) return 0f
        return (end - offset) * maxCharWidth
    }

    fun getWidthF32(codePoints: IntArray, offset: Int, maxChars: Int, noPadding: Boolean = false): Float {
        val end = minOf(codePoints.size, offset + maxChars)
        if (offset >= end) return 0f
        return (end - offset) * maxCharWidth
    }

    // -------------------------------------------------------------------------
    // Character-boundary helpers
    // -------------------------------------------------------------------------

    fun maxDrawableChars(
        codePoints: IntArray,
        maxPixels: Float,
        maxChars: Int = Int.MAX_VALUE,
        wrapStyle: WordWrapStyle = WordWrapStyle.ANYWHERE
    ): Int {
        val perChar = maxCharWidth.coerceAtLeast(1f)
        val byWidth = (maxPixels / perChar).toInt()
        val candidate = minOf(byWidth, maxChars, codePoints.size)
        if (wrapStyle == WordWrapStyle.ANYWHERE || candidate >= codePoints.size) return candidate
        var idx = candidate
        while (idx > 0 && codePoints[idx - 1] != ' '.code) idx--
        if (idx == 0 && wrapStyle == WordWrapStyle.WORD_BOUNDARY_IF_POSSIBLE) return candidate
        return idx
    }

    fun firstDrawableChar(
        codePoints: IntArray,
        maxPixels: Float,
        textLen: Int,
        startPos: Int = Int.MAX_VALUE,
        maxChars: Int = Int.MAX_VALUE
    ): Int {
        val perChar = maxCharWidth.coerceAtLeast(1f)
        val window = (maxPixels / perChar).toInt()
        val anchor = minOf(startPos, textLen, codePoints.size)
        val first = (anchor - window).coerceAtLeast(0)
        return minOf(first, maxChars)
    }

    fun charFromPixelOffset(
        codePoints: IntArray,
        charOffset: Int,
        x: Float,
        maxPixels: Float = Float.MAX_VALUE,
        maxChars: Int = Int.MAX_VALUE,
        round: Boolean = true
    ): Int {
        val perChar = maxCharWidth.coerceAtLeast(1f)
        val raw = x / perChar
        val rounded = if (round) (raw + 0.5f).toInt() else raw.toInt()
        return (charOffset + rounded).coerceIn(0, minOf(codePoints.size, charOffset + maxChars))
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    fun render(text: String, x: Float, y: Float): Int = render(
        text, 0, x, y, Color4(1f, 1f, 1f, 1f),
        HAlign.LEFT, VAlign.BASELINE, STYLE_NORMAL, ShadowType.NO_SHADOW
    )

    fun render(
        text: String,
        beginOffset: Int,
        x: Float,
        y: Float,
        color: Color4,
        hAlign: HAlign = HAlign.LEFT,
        vAlign: VAlign = VAlign.BASELINE,
        style: Int = STYLE_NORMAL,
        shadow: ShadowType = ShadowType.NO_SHADOW,
        maxChars: Int = Int.MAX_VALUE,
        maxPixels: Int = Int.MAX_VALUE,
        rightX: FloatArray? = null,
        useEllipses: Boolean = false,
        useColor: Boolean = true
    ): Int {
        if (!displayFont) return text.length
        if (text.isEmpty()) return 0
        val drawn = minOf(maxChars, text.length - beginOffset).coerceAtLeast(0)
        if (drawn == 0) return 0
        val width = drawn * maxCharWidth
        val xStart = when (hAlign) {
            HAlign.LEFT -> x
            HAlign.RIGHT -> x - width
            HAlign.HCENTER -> x - width / 2f
        }
        val yBase = when (vAlign) {
            VAlign.TOP -> y - ascender
            VAlign.VCENTER -> y - ascender / 2f
            VAlign.BASELINE -> y
            VAlign.BOTTOM -> y + descender
        }
        val cu = Color4u(
            (color.r * 255f).toInt().toUByte(),
            (color.g * 255f).toInt().toUByte(),
            (color.b * 255f).toInt().toUByte(),
            (color.a * 255f).toInt().toUByte()
        )
        var cursor = xStart
        for (i in beginOffset until beginOffset + drawn) {
            val screen = RectF(cursor, yBase - ascender, cursor + maxCharWidth, yBase + descender)
            val uv = RectF(0f, 0f, 1f, 1f)
            drawGlyph(screen, uv, cu, style, shadow, DROP_SHADOW_SOFT_STRENGTH)
            cursor += maxCharWidth
        }
        rightX?.let { if (it.isNotEmpty()) it[0] = cursor }
        return drawn
    }

    fun render(
        text: String,
        beginOffset: Int,
        rect: Rect,
        color: Color4,
        hAlign: HAlign = HAlign.LEFT,
        vAlign: VAlign = VAlign.BASELINE,
        style: Int = STYLE_NORMAL,
        shadow: ShadowType = ShadowType.NO_SHADOW,
        maxChars: Int = Int.MAX_VALUE,
        rightX: FloatArray? = null,
        useEllipses: Boolean = false,
        useColor: Boolean = true
    ): Int {
        val y = when (vAlign) {
            VAlign.TOP      -> rect.top.toFloat()
            VAlign.VCENTER  -> ((rect.top + rect.bottom) / 2).toFloat()
            VAlign.BASELINE,
            VAlign.BOTTOM   -> rect.bottom.toFloat()
        }
        return render(text, beginOffset, rect.left.toFloat(), y, color, hAlign, vAlign, style,
            shadow, maxChars, rect.width, rightX, useEllipses, useColor)
    }

    fun render(
        text: String,
        beginOffset: Int,
        rect: RectF,
        color: Color4,
        hAlign: HAlign = HAlign.LEFT,
        vAlign: VAlign = VAlign.BASELINE,
        style: Int = STYLE_NORMAL,
        shadow: ShadowType = ShadowType.NO_SHADOW,
        maxChars: Int = Int.MAX_VALUE,
        rightX: FloatArray? = null,
        useEllipses: Boolean = false,
        useColor: Boolean = true
    ): Int {
        val y = when (vAlign) {
            VAlign.TOP      -> rect.top
            VAlign.VCENTER  -> (rect.top + rect.bottom) / 2f
            VAlign.BASELINE,
            VAlign.BOTTOM   -> rect.bottom
        }
        return render(text, beginOffset, rect.left, y, color, hAlign, vAlign, style,
            shadow, maxChars, rect.width.toInt(), rightX, useEllipses, useColor)
    }

    // -------------------------------------------------------------------------
    // Private glyph helpers
    // -------------------------------------------------------------------------

    private fun renderTriangle(
        screenRect: RectF,
        uvRect: RectF,
        color: Color4u,
        slantAmt: Float
    ) {
        // Geometry is buffered through the immediate-mode renderer; emit two
        // triangles per glyph quad.
        TextRenderer.emitQuad(screenRect, uvRect, color, slantAmt)
    }

    private fun drawGlyph(
        screenRect: RectF,
        uvRect: RectF,
        color: Color4u,
        style: Int,
        shadow: ShadowType,
        dropShadowStrength: Float
    ) {
        val slant = if (style and STYLE_ITALIC != 0) ascender * 0.2f else 0f
        when {
            style and STYLE_BOLD != 0 -> {
                renderTriangle(screenRect, uvRect, color, slant)
                val bold = RectF(
                    screenRect.left + BOLD_OFFSET,
                    screenRect.top,
                    screenRect.right + BOLD_OFFSET,
                    screenRect.bottom
                )
                renderTriangle(bold, uvRect, color, slant)
            }
            shadow == ShadowType.DROP_SHADOW_SOFT -> {
                val shadowColor = Color4u(
                    color.r, color.g, color.b,
                    (color.a.toInt() * dropShadowStrength).toInt().toUByte()
                )
                val offsets = listOf(-1 to 1, 0 to 1, 1 to 1, -1 to 0, 1 to 0)
                for ((dx, dy) in offsets) {
                    val s = RectF(
                        screenRect.left + dx,
                        screenRect.top + dy,
                        screenRect.right + dx,
                        screenRect.bottom + dy
                    )
                    renderTriangle(s, uvRect, shadowColor, slant)
                }
                renderTriangle(screenRect, uvRect, color, slant)
            }
            shadow == ShadowType.DROP_SHADOW -> {
                val shadowColor = Color4u(color.r, color.g, color.b, color.a)
                val s = RectF(
                    screenRect.left + 1,
                    screenRect.top - 1,
                    screenRect.right + 1,
                    screenRect.bottom - 1
                )
                renderTriangle(s, uvRect, shadowColor, slant)
                renderTriangle(screenRect, uvRect, color, slant)
            }
            else -> renderTriangle(screenRect, uvRect, color, slant)
        }
    }
}

private fun pointSizeFor(size: String): Float = when (size) {
    "Small" -> 9f
    "Medium" -> 12f
    "Large" -> 16f
    "Huge" -> 24f
    "Monospace" -> 12f
    "Scripting" -> 12f
    "Cascadia" -> 12f
    else -> 12f
}

/**
 * Simple in-process vertex buffer that the higher-level draw path flushes to
 * the GPU. Holds emitted glyph quads in an interleaved layout (`x,y,u,v,r,g,b,a`).
 */
object TextRenderer {
    private val verts: MutableList<Float> = mutableListOf()

    fun emitQuad(screen: RectF, uv: RectF, color: Color4u, slant: Float) {
        val r = color.r.toInt() / 255f
        val g = color.g.toInt() / 255f
        val b = color.b.toInt() / 255f
        val a = color.a.toInt() / 255f
        fun push(x: Float, y: Float, u: Float, v: Float) {
            verts.addAll(listOf(x + slant, y, u, v, r, g, b, a))
        }
        // Triangle 1
        push(screen.left, screen.top, uv.left, uv.top)
        push(screen.right, screen.top, uv.right, uv.top)
        push(screen.right, screen.bottom, uv.right, uv.bottom)
        // Triangle 2
        push(screen.left, screen.top, uv.left, uv.top)
        push(screen.right, screen.bottom, uv.right, uv.bottom)
        push(screen.left, screen.bottom, uv.left, uv.bottom)
    }

    fun flush(): FloatArray {
        val out = verts.toFloatArray()
        verts.clear()
        return out
    }

    fun pendingVertexCount(): Int = verts.size / 8
}
