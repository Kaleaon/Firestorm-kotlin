package com.firestorm.llui

import kotlin.math.max

enum class Bevel { BEVEL_IN, BEVEL_OUT, BEVEL_BRIGHT, BEVEL_NONE }
enum class BorderStyle { STYLE_LINE, STYLE_TEXTURE }

data class Color4(val r: Float, val g: Float, val b: Float, val a: Float = 1f)

class UIColor(var color: Color4 = Color4(0f, 0f, 0f)) {
    fun get(): Color4 = color
}

open class ViewBorder(
    var bevel: Bevel = Bevel.BEVEL_OUT,
    var style: BorderStyle = BorderStyle.STYLE_LINE,
    var borderWidth: Int = 1,
    highlightLightColor: UIColor = UIColor(),
    highlightDarkColor: UIColor = UIColor(),
    shadowLightColor: UIColor = UIColor(),
    shadowDarkColor: UIColor = UIColor()
) {

    protected var highlightLight: UIColor = highlightLightColor
    protected var highlightDark: UIColor = highlightDarkColor
    protected var shadowLight: UIColor = shadowLightColor
    protected var shadowDark: UIColor = shadowDarkColor
    protected var texture: Any? = null
    var hasKeyboardFocus: Boolean = false

    var rect: Rect = Rect()

    fun setColors(shadowDark: UIColor, highlightLight: UIColor) {
        this.shadowDark = shadowDark
        this.highlightLight = highlightLight
    }

    fun setColorsExtended(
        shadowLight: UIColor,
        shadowDark: UIColor,
        highlightLight: UIColor,
        highlightDark: UIColor
    ) {
        this.shadowDark = shadowDark
        this.shadowLight = shadowLight
        this.highlightLight = highlightLight
        this.highlightDark = highlightDark
    }

    fun setTexture(imageId: Any) {
        System.err.println("ViewBorder: setTexture not yet implemented")
    }

    fun getHighlightLight(): Color4 = highlightLight.get()
    fun getShadowDark(): Color4 = highlightDark.get()

    open fun draw() {
        if (style == BorderStyle.STYLE_LINE) {
            when (borderWidth) {
                0 -> Unit
                1 -> drawOnePixelLines()
                2 -> drawTwoPixelLines()
                else -> error("ViewBorder: unsupported border width $borderWidth")
            }
        }
    }

    private fun drawOnePixelLines() {
        // no-op

        val topColor: Color4
        val bottomColor: Color4
        when (bevel) {
            Bevel.BEVEL_OUT -> {
                topColor = highlightLight.get()
                bottomColor = shadowDark.get()
            }
            Bevel.BEVEL_IN -> {
                topColor = shadowDark.get()
                bottomColor = highlightLight.get()
            }
            else -> {
                topColor = highlightLight.get()
                bottomColor = highlightLight.get()
            }
        }

        val left = 0
        val top = rect.height
        val right = rect.width
        val bottom = 0

        // no-op
    }

    private fun drawTwoPixelLines() {
        // no-op

        val topInColor: Color4
        val topOutColor: Color4
        val bottomInColor: Color4
        val bottomOutColor: Color4

        when (bevel) {
            Bevel.BEVEL_OUT -> {
                topInColor = highlightLight.get()
                topOutColor = highlightDark.get()
                bottomInColor = shadowLight.get()
                bottomOutColor = shadowDark.get()
            }
            Bevel.BEVEL_IN -> {
                topInColor = shadowDark.get()
                topOutColor = shadowLight.get()
                bottomInColor = highlightDark.get()
                bottomOutColor = highlightLight.get()
            }
            Bevel.BEVEL_BRIGHT -> {
                topInColor = highlightLight.get()
                topOutColor = highlightLight.get()
                bottomInColor = highlightLight.get()
                bottomOutColor = highlightLight.get()
            }
            Bevel.BEVEL_NONE -> {
                topInColor = shadowDark.get()
                topOutColor = shadowDark.get()
                bottomInColor = shadowDark.get()
                bottomOutColor = shadowDark.get()
            }
        }

        // no-op
    }

    companion object {
        fun bevelFromString(s: String): Bevel? = when (s.lowercase()) {
            "none"   -> Bevel.BEVEL_NONE
            "in"     -> Bevel.BEVEL_IN
            "out"    -> Bevel.BEVEL_OUT
            "bright" -> Bevel.BEVEL_BRIGHT
            else     -> null
        }
    }
}

data class Rect(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = top - bottom
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (bottom + top) / 2
}
