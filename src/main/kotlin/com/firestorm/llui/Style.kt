package com.firestorm.llui

import java.awt.Color

enum class ShadowType { NONE, DROP_SHADOW_SOFT, DROP_SHADOW }

enum class UnderlineLink {
    UNDERLINE_ALWAYS,
    UNDERLINE_ON_HOVER,
    UNDERLINE_NEVER
}

data class UIColor(val color: Color = Color.BLACK) {
    companion object {
        val BLACK = UIColor(Color.BLACK)
        val WHITE = UIColor(Color.WHITE)
        val GREEN = UIColor(Color.GREEN)
        val GREY  = UIColor(Color.GRAY)
    }
    operator fun get(): Color = color
}

data class UIImage(val name: String)

class Style(params: Params = Params()) {

    data class Params(
        var visible: Boolean = true,
        var dropShadow: ShadowType = ShadowType.NONE,
        var color: UIColor = UIColor.BLACK,
        var readonlyColor: UIColor = UIColor.BLACK,
        var selectedColor: UIColor = UIColor.BLACK,
        var highlightBgColor: UIColor = UIColor.GREEN,
        var alpha: Float = 1f,
        var font: Any? = null,
        var image: UIImage? = null,
        var linkHref: String = "",
        var isLink: Boolean? = null,
        var drawHighlightBg: Boolean = false,
        var useDefaultLinkStyle: Boolean = true,
        var canUnderlineOnHover: Boolean = true
    )

    var dropShadow: ShadowType = params.dropShadow

    private var fontName: String = ""
    private var link: String = params.linkHref
    private var color: UIColor = params.color
    private var readOnlyColor: UIColor = params.readonlyColor
    private var selectedColor: UIColor = params.selectedColor
    private var highlightBgColor: UIColor = params.highlightBgColor
    private var font: Any? = params.font
    private var image: UIImage? = params.image
    private var alpha: Float = params.alpha
    private var visible: Boolean = params.visible
    private var isLink: Boolean = params.isLink ?: params.linkHref.isNotEmpty()
    private var drawHighlightBg: Boolean = params.drawHighlightBg

    fun getColor(): UIColor = color
    fun setColor(c: UIColor) { color = c }

    fun getReadOnlyColor(): UIColor = readOnlyColor
    fun setReadOnlyColor(c: UIColor) { readOnlyColor = c }

    fun getSelectedColor(): UIColor = selectedColor
    fun setSelectedColor(c: UIColor) { selectedColor = c }

    fun getAlpha(): Float = alpha
    fun setAlpha(a: Float) { alpha = a }

    fun isVisible(): Boolean = visible
    fun setVisible(v: Boolean) { visible = v }

    fun getShadowType(): ShadowType = dropShadow

    fun setFont(f: Any?) { font = f }
    fun getFont(): Any? = font

    fun getLinkHREF(): String = link
    fun setLinkHREF(href: String) { link = href }
    fun isLink(): Boolean = isLink

    fun getImage(): UIImage? = image
    fun setImage(name: String) { image = UIImage(name) }
    fun setImageById(id: String) { image = UIImage(id) }
    fun isImage(): Boolean = image != null

    fun getDrawHighlightBg(): Boolean = drawHighlightBg
    fun getHighlightBgColor(): UIColor = highlightBgColor

    companion object {
        fun getDefaultFont(): Any? = TODO("GPU: return default monospace font handle")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Style) return false
        return visible == other.visible
            && color == other.color
            && readOnlyColor == other.readOnlyColor
            && selectedColor == other.selectedColor
            && highlightBgColor == other.highlightBgColor
            && font == other.font
            && link == other.link
            && image == other.image
            && dropShadow == other.dropShadow
            && alpha == other.alpha
            && drawHighlightBg == other.drawHighlightBg
    }

    override fun hashCode(): Int = arrayOf(
        visible, color, readOnlyColor, selectedColor, highlightBgColor,
        font, link, image, dropShadow, alpha, drawHighlightBg
    ).contentHashCode()
}
