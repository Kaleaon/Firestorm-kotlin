package com.firestorm.llui

object TextUtil {

    private val filterTextColor: UIColor = UIColor(java.awt.Color(0f, 0.8f, 0f, 1f))
    private val greyedTextColor: UIColor = UIColor.GREY

    fun textboxSetHighlightedVal(
        txtbox: TextBox,
        normalStyle: Style.Params,
        text: String,
        hl: String
    ) {
        val textUpper = text.uppercase()
        val hlLen = hl.length
        val hlBegin = if (hlLen == 0) -1 else textUpper.indexOf(hl)

        if (hlLen == 0 || hlBegin < 0) {
            txtbox.setText(text, normalStyle)
            return
        }

        val hlStyle = normalStyle.copy(color = filterTextColor)

        txtbox.setText("", normalStyle)
        txtbox.appendText(text.substring(0, hlBegin), false, normalStyle)
        txtbox.appendText(text.substring(hlBegin, hlBegin + hlLen), false, hlStyle)
        txtbox.appendText(text.substring(hlBegin + hlLen), false, normalStyle)
    }

    fun textboxSetGreyedVal(
        txtbox: TextBox,
        normalStyle: Style.Params,
        text: String,
        greyed: String
    ) {
        val greyedLen = greyed.length
        val greyedBegin = if (greyedLen == 0) -1 else text.indexOf(greyed)

        if (greyedLen == 0 || greyedBegin < 0) {
            txtbox.setText(text, normalStyle)
            return
        }

        val greyedStyle = normalStyle.copy(color = greyedTextColor)

        txtbox.setText("", normalStyle)
        txtbox.appendText(text.substring(0, greyedBegin), false, normalStyle)
        txtbox.appendText(text.substring(greyedBegin, greyedBegin + greyedLen), false, greyedStyle)
        txtbox.appendText(text.substring(greyedBegin + greyedLen), false, normalStyle)
    }

    fun processUrlMatch(match: UrlMatch?, textBase: TextBase?, isContentTrusted: Boolean): Boolean {
        if (match == null || textBase == null) return false

        if (match.id.isNotEmpty() && TextHelpers.iconCallbackCreationFunction != null) {
            val segmentCreated = TextHelpers.iconCallbackCreationFunction!!(match, textBase)
            if (segmentCreated) return true
        }

        if (isContentTrusted && match.icon.isNotEmpty()) {
            val image = UIImage(match.icon)
            val icon = Style.Params(image = image)
            textBase.appendImageSegment(icon)
            return true
        }

        return false
    }

    object TextHelpers {
        var iconCallbackCreationFunction: ((UrlMatch, TextBase) -> Boolean)? = null
    }

    data class UrlMatch(
        val id: String = "",
        val icon: String = "",
        val label: String = "",
        val url: String = ""
    )
}
