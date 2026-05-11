package com.firestorm.newview

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Vector2
import com.firestorm.llmath.Vector3

private const val HORIZONTAL_PADDING: Float      = 15f
private const val VERTICAL_PADDING: Float        = 12f
private const val BUFFER_SIZE: Float             = 2f
private const val HUD_TEXT_MAX_WIDTH: Float      = 190f
private const val HUD_TEXT_MAX_WIDTH_NO_BUBBLE: Float = 1000f
private const val MAX_DRAW_DISTANCE: Float       = 300f
private const val LINE_PADDING: Float            = 3f

private const val SHOW_BACKGROUND_NONE              = 0
private const val SHOW_BACKGROUND_ONLY_HIGHLIGHTED  = 1
private const val SHOW_BACKGROUND_ALL               = 2

enum class TextAlignment { LEFT, CENTER }
enum class VertAlignment  { TOP, CENTER }

class HUDText(type: UByte) : HUDObject(type) {

    inner class TextSegment(
        private val text: String,
        val style: Int,
        var color: Color4,
        val font: Any?   // opaque font handle
    ) {
        val isBlank: Boolean = text.isEmpty() || text.all { it == ' ' }
        private val fontWidthCache: MutableMap<Any, Float> = mutableMapOf()

        fun getText(): String = text

        fun getWidth(fontKey: Any): Float {
            return fontWidthCache.getOrPut(fontKey) {
                TODO("GPU: measure text width for '$text' using fontKey")
            }
        }

        fun clearFontWidthMap() { fontWidthCache.clear() }
    }

    private var onHUDAttachment: Boolean = false
    private var doFade: Boolean = true
    private var fadeRange: Float = 4f
    private var fadeDistance: Float = 8f
    private var lastDistance: Float = 0f
    private var zCompare: Boolean = true
    private var offscreen: Boolean = false
    private var color: Color4 = Color4(1f, 1f, 1f, 1f)
    private var mScale: Vector3 = Vector3(0f, 0f, 0f)
    private var width: Float = 0f
    private var height: Float = 0f
    private var fontp: Any? = TODO("GPU: LLFontGL.getFontSansSerifSmall()")
    private var boldFontp: Any? = TODO("GPU: LLFontGL.getFontSansSerifBold()")
    private var positionAgent: Vector3 = Vector3(0f, 0f, 0f)
    private var positionOffset: Vector2 = Vector2(0f, 0f)
    private var targetPositionOffset: Vector2 = Vector2(0f, 0f)
    private var mass: Float = 1f
    private var maxLines: Int = 10
    private var offsetY: Int = 0
    private var radius: Float = 0.1f
    private val textSegments: MutableList<TextSegment> = mutableListOf()
    private var textAlignment: TextAlignment = TextAlignment.CENTER
    private var vertAlignment: VertAlignment = VertAlignment.CENTER
    private var hidden: Boolean = false
    private var objText: String = ""

    private var backgroundHeight: Float = 0f
    private var backgroundWidth: Float = 0f
    private var backgroundOffsetY: Float = 0f
    private var luminance: Float = 1f

    init {
        TODO("APR: read FSHudTextFadeDistance and FSHudTextFadeRange from gSavedSettings; " +
             "add this to sTextObjects; load 'Rounded_Rect' UI image for background")
    }

    fun setString(textUtf8: String) {
        textSegments.clear()
        addLine(textUtf8, color)
    }

    fun clearString() {
        textSegments.clear()
    }

    fun addLine(textUtf8: String, lineColor: Color4, style: Int = 0, font: Any? = null) {
        if (textUtf8.isEmpty()) return
        val effectiveFont = font ?: fontp
        val lines = textUtf8.split('\r', '\n')
        for (line in lines) {
            if (line.isEmpty()) {
                textSegments.add(TextSegment("", style, lineColor, effectiveFont))
                continue
            }
            var offset = 0
            while (offset < line.length) {
                val segmentLength: Int = TODO("GPU: effectiveFont.maxDrawableChars(line.substring(offset), HUD_TEXT_MAX_WIDTH_NO_BUBBLE, WORD_BOUNDARY_IF_POSSIBLE)")
                @Suppress("UNREACHABLE_CODE")
                val chunk = line.substring(offset, minOf(offset + segmentLength, line.length))
                textSegments.add(TextSegment(chunk, style, lineColor, effectiveFont))
                offset += chunk.length
                if (offset >= line.length) break
            }
        }
    }

    fun setFont(font: Any?) { fontp = font }

    fun setColor(newColor: Color4) {
        color = newColor
        luminance = 0.299f * color.r + 0.587f * color.g + 0.114f * color.b
        textSegments.forEach { it.color = newColor }
    }

    fun setAlpha(alpha: Float) {
        color.a = alpha
        textSegments.forEach { it.color.a = alpha }
    }

    fun setZCompare(zcompare: Boolean) { zCompare = zcompare }
    fun setDoFade(dFade: Boolean)      { doFade = dFade }
    fun setMaxLines(lines: Int)        { maxLines = lines }
    fun setFadeDistance(dist: Float, range: Float) { fadeDistance = dist; fadeRange = range }
    fun setMass(m: Float)              { mass = maxOf(0.1f, m) }
    fun setTextAlignment(a: TextAlignment) { textAlignment = a }
    fun setVertAlignment(a: VertAlignment) { vertAlignment = a }
    fun getVisible(): Boolean = mVisible
    fun getHidden(): Boolean  = hidden
    fun setHidden(hide: Boolean) { hidden = hide }
    fun setOnHUDAttachment(onHud: Boolean) { onHUDAttachment = onHud }
    override fun getDistance(): Float = lastDistance
    fun getObjectText(): String = objText
    fun setObjectText(s: String) { objText = s }

    fun shift(offset: Vector3) {
        positionAgent = Vector3(positionAgent.x + offset.x, positionAgent.y + offset.y, positionAgent.z + offset.z)
    }

    override fun markDead() {
        TODO("APR: remove this from sTextObjects, then call super.markDead()")
    }

    override fun render() {
        if (!onHUDAttachment && sDisplayText) {
            TODO("GPU: set LLGLDepthTest based on hover-highlight state, then call renderText()")
        }
    }

    fun renderText() {
        if (!mVisible || hidden) return
        if (textSegments.size == 1 && textSegments[0].isBlank) return

        TODO("GPU: render text segments and optional background rect; " +
             "compute alpha_factor from lastDistance/fadeDistance/fadeRange; " +
             "compute border scale, pixel vectors (x_pixel_vec/y_pixel_vec), render_position; " +
             "optionally draw mRoundedRectImgp background if showBackground flag is set; " +
             "iterate visible text segments calling hud_render_text() for each; " +
             "reset color to (1,1,1,1)")
    }

    fun updateVisibility() {
        TODO("APR: call mSourceObject?.updateText(); " +
             "convert mPositionGlobal to agent coords; " +
             "skip dead source objects; " +
             "handle HUD attachment path; " +
             "push text toward camera; update lastDistance; " +
             "check max draw distance and frustum; " +
             "add to sVisibleTextObjects or sVisibleHUDTextObjects")
    }

    fun updateScreenPos(offsetTarget: Vector2): Vector2 {
        TODO("GPU: project positionAgent to screen; clamp to world view rect; " +
             "update mSoftScreenRect; return adjusted offset delta")
    }

    fun updateSize() {
        var h = 0f
        var w = 0f
        backgroundWidth = 0f
        var firstNoneBlankPos = 0f
        var lastNoneBlankPos = 0f
        var firstNoneBlank = true

        val startSegment = if (maxLines < 0) 0 else maxOf(0, textSegments.size - maxLines)
        for (seg in textSegments.drop(startSegment)) {
            val lineHeight: Float = TODO("GPU: seg.font.getLineHeight() - 1")
            @Suppress("UNREACHABLE_CODE")
            if (!seg.isBlank) {
                if (firstNoneBlank) {
                    firstNoneBlankPos = h
                    firstNoneBlank = false
                }
                lastNoneBlankPos = h + lineHeight
            }
            backgroundWidth = maxOf(backgroundWidth, minOf(seg.getWidth(seg.font ?: Any()), HUD_TEXT_MAX_WIDTH_NO_BUBBLE))
            h += lineHeight
            w = maxOf(w, minOf(seg.getWidth(seg.font ?: Any()), HUD_TEXT_MAX_WIDTH))
        }

        if (w == 0f) return

        w += HORIZONTAL_PADDING
        h += VERTICAL_PADDING

        width  = maxOf(width,  w)
        height = maxOf(height, h)

        backgroundOffsetY = firstNoneBlankPos + VERTICAL_PADDING * 0.5f
        backgroundHeight  = (lastNoneBlankPos + VERTICAL_PADDING * 1.5f) - firstNoneBlankPos
        backgroundWidth  += HORIZONTAL_PADDING
    }

    private fun getMaxLines(): Int = maxLines

    companion object {
        var sDisplayText: Boolean = true
        private val sTextObjects: MutableSet<HUDText> = mutableSetOf()
        private val sVisibleTextObjects: MutableList<HUDText> = mutableListOf()
        private val sVisibleHUDTextObjects: MutableList<HUDText> = mutableListOf()

        fun updateAll() {
            sVisibleTextObjects.clear()
            sVisibleHUDTextObjects.clear()
            for (text in sTextObjects) {
                text.targetPositionOffset = Vector2(0f, 0f)
                text.updateSize()
                text.updateVisibility()
            }
            sVisibleTextObjects.sortByDescending { it.getDistance() }
            sVisibleHUDTextObjects.sortByDescending { it.getDistance() }
        }

        fun renderAllHUD() {
            TODO("GPU: disable depth test; iterate sVisibleHUDTextObjects calling renderText(); " +
                 "unbind vertex buffer; check GL states")
        }

        fun shiftAll(offset: Vector3) {
            sTextObjects.forEach { it.shift(offset) }
        }

        fun reshape() {
            sTextObjects.forEach { text ->
                text.textSegments.forEach { it.clearFontWidthMap() }
            }
        }

        fun setDisplayText(flag: Boolean) { sDisplayText = flag }

        fun refreshAllObjectText(filter: ObjectTextFilter = ObjectTextFilter.NONE) {
            for (text in sTextObjects) {
                if (text.objText.isNotEmpty() && text.mSourceObject != null) {
                    val isHudAttach = (filter == ObjectTextFilter.HUD_ATTACHMENTS) &&
                        TODO("APR: check text.mSourceObject.isHUDAttachment()")
                    @Suppress("UNREACHABLE_CODE")
                    if (filter == ObjectTextFilter.NONE || isHudAttach) {
                        text.setString(text.objText)
                    }
                }
            }
        }

        fun onFadeSettingsChanged() {
            for (text in sTextObjects) {
                TODO("APR: text.fadeDistance = gSavedSettings.getF32('FSHudTextFadeDistance'); " +
                     "text.fadeRange = gSavedSettings.getF32('FSHudTextFadeRange')")
            }
        }
    }
}

enum class ObjectTextFilter { NONE, HUD_ATTACHMENTS }
