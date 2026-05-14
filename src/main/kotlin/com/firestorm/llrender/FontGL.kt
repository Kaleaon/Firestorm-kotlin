package com.firestorm.llrender

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Color4u
import com.firestorm.llmath.Rect
import com.firestorm.llmath.RectF
import kotlin.math.ceil
import kotlin.math.floor

class FontGL {

    // -------------------------------------------------------------------------
    // Enumerations
    // -------------------------------------------------------------------------

    enum class HAlign { LEFT, RIGHT, HCENTER }
    enum class VAlign { TOP, VCENTER, BASELINE, BOTTOM }
    enum class ShadowType { NO_SHADOW, DROP_SHADOW, DROP_SHADOW_SOFT }
    enum class WordWrapStyle { ONLY_WORD_BOUNDARIES, WORD_BOUNDARY_IF_POSSIBLE, ANYWHERE }

    // StyleFlags are bit-masks, kept as a companion-object Int constants so
    // they can be combined with 'or'/'and' just like the C++ U8 bit flags.
    companion object {
        const val STYLE_NORMAL:    Int = 0x00
        const val STYLE_BOLD:      Int = 0x01
        const val STYLE_ITALIC:    Int = 0x02
        const val STYLE_UNDERLINE: Int = 0x04

        private const val BOLD_OFFSET = 1
        private const val PAD_UVY = 0.5f
        private const val DROP_SHADOW_SOFT_STRENGTH = 0.3f

        // ---- Global state (mirrors static members of LLFontGL) --------------

        var vertDpi: Float = 96f
        var horizDpi: Float = 96f
        var scaleX: Float = 1f
        var scaleY: Float = 1f
        var resolutionGeneration: Int = 0
        var displayFont: Boolean = true
        var appDir: String = ""
        var shadowColor: Color4 = Color4(0f, 0f, 0f, 1f)

        // Origin stack used for nested UI matrix pushes.
        var curOriginX: Int = 0
        var curOriginY: Int = 0
        var curDepth: Float = 0f
        val originStack: MutableList<Triple<Int, Int, Float>> = mutableListOf()

        // Font registry (opaque — actual loading is platform-specific).
        private var fontRegistry: Any? = null

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
            System.err.println("FontGL: initialise font registry from '$fontsFile' under '$appDir' not yet implemented")
        }

        fun loadDefaultFonts(): Boolean {
            System.err.println("FontGL: pre-load default font faces not yet implemented")
            return false
        }

        fun loadCommonFonts() {
            System.err.println("FontGL: load common font faces not yet implemented")
        }

        fun destroyDefaultFonts() {
            fontRegistry = null
        }

        fun destroyAllGl() {
            // no-op
        }

        // ---- Well-known font accessors (mirrors getFontXxx statics) ---------

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

        // ---- Alignment name helpers -----------------------------------------

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

        // ---- Style string helpers -------------------------------------------

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

        // ---- System / local font path helpers (platform stubs) --------------

        fun getFontPathSystem(): String {
            System.err.println("FontGL: locate system font directory not yet implemented")
            return ""
        }

        fun getFontPathLocal(): String =
            if (appDir.isNotEmpty()) "$appDir/fonts/" else "./fonts/"

        // ---- Utility --------------------------------------------------------

        fun nameFromFont(font: FontGL): String = font.descriptor.name
        fun sizeFromFont(font: FontGL):  String = font.descriptor.size

        fun dumpFonts()        { System.err.println("FontGL: log all registered font descriptors not yet implemented") }
        fun dumpFontTextures() { /* no-op */ }

        // Internal: load-or-create a FontGL from the registry.
        private fun getOrLoad(family: String, size: String, style: Int): FontGL {
            val desc = FontDescriptor(family, size, style)
            System.err.println("FontGL: look up or create FontGL for descriptor $desc not yet implemented")
            return FontGL().also { it.descriptor = desc }
        }
    }

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    data class FontDescriptor(val name: String, val size: String, val style: Int)

    var descriptor: FontDescriptor = FontDescriptor("SansSerif", "Medium", STYLE_NORMAL)
        private set

    // The underlying FreeType face is opaque; all calls that require it are stubbed.
    private var freetypeFace: Any? = null

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
        System.err.println("FontGL: load FreeType face from '$filename' at ${pointSize}pt not yet implemented")
        return false
    }

    fun getNumFaces(filename: String): Int {
        System.err.println("FontGL: query number of faces in font file '$filename' not yet implemented")
        return 0
    }

    fun getCacheGeneration(): Int {
        return 0
    }

    fun reset() {
        // no-op
    }

    fun destroyGl() {
        // no-op
    }

    fun generateAsciiGlyphs() {
        // no-op
    }

    fun dumpTextures() {
        // no-op
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    fun getAscenderHeight(): Float {
        return 0f
    }

    fun getDescenderHeight(): Float {
        return 0f
    }

    fun getLineHeight(): Int {
        return 0
    }

    // -------------------------------------------------------------------------
    // Width measurement
    // -------------------------------------------------------------------------

    fun getWidth(utf8text: String): Int = getWidth(utf8text, 0, Int.MAX_VALUE)
    fun getWidth(utf8text: String, offset: Int, maxChars: Int): Int =
        getWidthF32(utf8text, offset, maxChars).let { f -> (f + 0.5f).toInt() }

    fun getWidthF32(utf8text: String): Float = getWidthF32(utf8text, 0, Int.MAX_VALUE)
    fun getWidthF32(utf8text: String, offset: Int, maxChars: Int): Float {
        return 0f
    }

    fun getWidthF32(codePoints: IntArray, offset: Int, maxChars: Int, noPadding: Boolean = false): Float {
        return 0f
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
        return 0
    }

    fun firstDrawableChar(
        codePoints: IntArray,
        maxPixels: Float,
        textLen: Int,
        startPos: Int = Int.MAX_VALUE,
        maxChars: Int = Int.MAX_VALUE
    ): Int {
        return 0
    }

    fun charFromPixelOffset(
        codePoints: IntArray,
        charOffset: Int,
        x: Float,
        maxPixels: Float = Float.MAX_VALUE,
        maxChars: Int = Int.MAX_VALUE,
        round: Boolean = true
    ): Int {
        return 0
    }

    // -------------------------------------------------------------------------
    // Rendering — all GPU paths are stubbed
    // -------------------------------------------------------------------------

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
        // no-op
        return 0
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
    // Private glyph-level helpers (stubbed — require GPU vertex buffers)
    // -------------------------------------------------------------------------

    private fun renderTriangle(
        screenRect: RectF,
        uvRect: RectF,
        color: Color4u,
        slantAmt: Float
    ) {
        // no-op
    }

    private fun drawGlyph(
        screenRect: RectF,
        uvRect: RectF,
        color: Color4u,
        style: Int,
        shadow: ShadowType,
        dropShadowStrength: Float
    ) {
        val slant = if (style and STYLE_ITALIC != 0) {
            // no-op
            0f
        } else {
            0f
        }
        when {
            style and STYLE_BOLD != 0 -> {
                // Bold: render the glyph twice, shifted by BOLD_OFFSET on the second pass.
                // no-op
            }
            shadow == ShadowType.DROP_SHADOW_SOFT -> {
                // Soft shadow: 5 offset passes at reduced alpha, then the main glyph.
                // no-op
            }
            shadow == ShadowType.DROP_SHADOW -> {
                // Hard shadow: one offset pass, then the main glyph.
                // no-op
            }
            else -> {
                // Normal: single quad.
                // no-op
            }
        }
    }
}
