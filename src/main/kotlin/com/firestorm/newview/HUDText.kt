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
                0f
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
    private var fontp: Any? = null
    private var boldFontp: Any? = null
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
        System.err.println("HUDText: init not yet implemented")
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
                val segmentLength: Int = line.length - offset
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
        System.err.println("HUDText: markDead not yet implemented")
        super.markDead()
    }

    override fun render() {
        if (!onHUDAttachment && sDisplayText) {
            // no-op
        }
    }

    fun renderText() {
        if (!mVisible || hidden) return
        if (textSegments.size == 1 && textSegments[0].isBlank) return

        // no-op
    }

    fun updateVisibility() {
        System.err.println("HUDText: updateVisibility not yet implemented")
    }

    fun updateScreenPos(offsetTarget: Vector2): Vector2 {
        return Vector2(0f, 0f)
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
            val lineHeight: Float = 0f
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
            // no-op
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
                    val isHudAttach = (filter == ObjectTextFilter.HUD_ATTACHMENTS) && false
                    if (filter == ObjectTextFilter.NONE || isHudAttach) {
                        text.setString(text.objText)
                    }
                }
            }
        }

        fun onFadeSettingsChanged() {
            for (text in sTextObjects) {
                System.err.println("HUDText: onFadeSettingsChanged not yet implemented")
            }
        }
    }
}

enum class ObjectTextFilter { NONE, HUD_ATTACHMENTS }
