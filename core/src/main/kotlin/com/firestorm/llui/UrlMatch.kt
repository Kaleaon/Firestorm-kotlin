package com.firestorm.llui

data class StyleParams(
    var color: Int = 0,
    var readonlyColor: Int = 0,
    var fontStyle: String = "",
    var isLink: Boolean = true,
    var linkHref: String = "",
    var drawHighlightBg: Boolean = false,
    var highlightBgColor: Int = 0
)

enum class UnderlineLink {
    UNDERLINE_ALWAYS,
    UNDERLINE_ON_HOVER,
    UNDERLINE_NEVER
}

class UrlMatch {
    var start: UInt = 0u
        private set
    var end: UInt = 0u
        private set
    var url: String = ""
        private set
    var label: String = ""
        private set
    var query: String = ""
        private set
    var tooltip: String = ""
        private set
    var icon: String = ""
        private set
    var style: StyleParams = StyleParams()
        private set
    var menuName: String = ""
        private set
    var location: String = ""
        private set
    var matchedText: String = ""
        private set
    var id: String = ""
        private set
    var underline: UnderlineLink = UnderlineLink.UNDERLINE_ALWAYS
        private set
    var isTrusted: Boolean = false
        private set
    var skipProfileIcon: Boolean = false
        private set

    fun isEmpty(): Boolean = url.isEmpty()

    fun setValues(
        start: UInt,
        end: UInt,
        url: String,
        label: String,
        query: String,
        tooltip: String,
        icon: String,
        style: StyleParams,
        menuName: String,
        location: String,
        matchedText: String,
        id: String,
        underline: UnderlineLink = UnderlineLink.UNDERLINE_ALWAYS,
        trusted: Boolean = false,
        skipIcon: Boolean = false
    ) {
        this.start = start
        this.end = end
        this.url = url
        this.label = label
        this.query = query
        this.tooltip = tooltip
        this.icon = icon
        this.style = style.copy(linkHref = url)
        this.menuName = menuName
        this.location = location
        this.matchedText = matchedText
        this.id = id
        this.underline = underline
        this.isTrusted = trusted
        this.skipProfileIcon = skipIcon
    }
}
