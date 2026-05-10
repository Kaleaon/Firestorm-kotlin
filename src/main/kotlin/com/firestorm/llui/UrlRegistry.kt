package com.firestorm.llui

val nullUrlLabelCallback: UrlLabelCallback = { _, _, _ -> }

object UrlRegistry {
    private val urlEntries: MutableList<UrlEntryBase> = mutableListOf()

    private lateinit var urlEntryNoLink: UrlEntryBase
    private lateinit var urlEntryIcon: UrlEntryBase
    private lateinit var urlEntryInvalidSlurl: UrlEntryBase
    private lateinit var urlEntryHttpLabel: UrlEntryBase
    private lateinit var urlEntrySlLabel: UrlEntryBase
    private lateinit var urlEntryAgentMention: UrlEntryAgentMention
    private lateinit var urlEntryTrustedUrl: UrlEntryBase
    private lateinit var urlEntryWear: UrlEntryBase
    private lateinit var urlEntryKeybinding: UrlEntryKeybinding

    init {
        urlEntryNoLink = UrlEntryNoLink()
        registerUrl(urlEntryNoLink)
        urlEntryIcon = UrlEntryIcon()
        registerUrl(urlEntryIcon)
        urlEntryInvalidSlurl = UrlEntryInvalidSlurl()
        registerUrl(urlEntryInvalidSlurl)
        registerUrl(UrlEntrySlurl())

        urlEntryTrustedUrl = UrlEntrySecondlifeUrl()
        registerUrl(urlEntryTrustedUrl)
        registerUrl(UrlEntrySimpleSecondlifeUrl())

        registerUrl(UrlEntryHttp())
        urlEntryHttpLabel = UrlEntryHttpLabel()
        registerUrl(urlEntryHttpLabel)
        registerUrl(UrlEntryAgentCompleteName())
        registerUrl(UrlEntryAgentLegacyName())
        registerUrl(UrlEntryAgentDisplayName())
        registerUrl(UrlEntryAgentUserName())
        registerUrl(UrlEntryAgentRlvAnonymizedName())
        registerUrl(FsUrlEntryAgentSelf())
        urlEntryAgentMention = UrlEntryAgentMention()
        registerUrl(urlEntryAgentMention)
        registerUrl(UrlEntryAgent())
        registerUrl(UrlEntryChat())
        registerUrl(UrlEntryGroup())
        registerUrl(UrlEntryParcel())
        registerUrl(UrlEntryTeleport())
        registerUrl(UrlEntryRegion())
        registerUrl(UrlEntryWorldMap())
        registerUrl(UrlEntryObjectIm())
        registerUrl(UrlEntryPlace())
        registerUrl(UrlEntryInventory())
        registerUrl(UrlEntryExperienceProfile())
        urlEntryKeybinding = UrlEntryKeybinding()
        registerUrl(urlEntryKeybinding)
        registerUrl(FsHelpDebugUrlEntrySl())
        urlEntryWear = FsUrlEntryWear()
        registerUrl(urlEntryWear)
        registerUrl(UrlEntrySl())
        urlEntrySlLabel = UrlEntrySlLabel()
        registerUrl(urlEntrySlLabel)
        registerUrl(UrlEntryHttpNoProtocol())
        registerUrl(UrlEntryEmail())
        registerUrl(UrlEntryIPv6())
        registerUrl(UrlEntryJira())
    }

    fun registerUrl(entry: UrlEntryBase, forceFront: Boolean = false) {
        if (forceFront) urlEntries.add(0, entry) else urlEntries.add(entry)
    }

    fun findUrl(
        text: String,
        match: UrlMatch,
        cb: UrlLabelCallback = nullUrlLabelCallback,
        isContentTrusted: Boolean = false,
        skipNonMentions: Boolean = false
    ): Boolean {
        if (!stringHasUrl(text) && !stringHasJira(text)) return false

        var matchStart = Int.MAX_VALUE
        var matchEnd = 0
        var matchEntry: UrlEntryBase? = null

        for (entry in urlEntries) {
            if (entry === urlEntryIcon && (text.contains("Hand") || !isContentTrusted)) continue
            if (skipNonMentions && entry !== urlEntryAgentMention) continue

            val result = matchRegex(text, entry.pattern) ?: continue
            val (start, end) = result

            if (matchEntry == null || start < matchStart) {
                if (entry === urlEntryInvalidSlurl) {
                    if (entry.isSlurlValid(text.substring(start, end + 1))) continue
                }
                if (entry === urlEntryHttpLabel || entry === urlEntrySlLabel) {
                    if (!entry.isWikiLinkCorrect(text.substring(start, end + 1))) continue
                }

                matchStart = start
                matchEnd = end
                matchEntry = entry

                if (entry === urlEntryWear) break
            }
        }

        val entry = matchEntry ?: return false

        if (matchStart > 0 && text[matchStart - 1] == '@') return false

        var url = text.substring(matchStart, matchEnd + 1)

        if (entry !== urlEntryNoLink && entry === urlEntryTrustedUrl) {
            url = normalizeUri(url)
        }

        match.setValues(
            start = matchStart.toUInt(),
            end = matchEnd.toUInt(),
            url = entry.getUrl(url),
            label = entry.getLabel(url, cb),
            query = entry.getQuery(url),
            tooltip = entry.getTooltip(url),
            icon = entry.getIcon(url),
            style = entry.getStyle(url),
            menuName = entry.menuName,
            location = entry.getLocation(url),
            matchedText = text.substring(matchStart, matchEnd + 1),
            id = entry.getId(url),
            underline = entry.getUnderline(url),
            trusted = entry.isTrusted(),
            skipIcon = entry.getSkipProfileIcon(url)
        )
        return true
    }

    fun findUrl(text: String, match: UrlMatch, cb: UrlLabelCallback = nullUrlLabelCallback): Boolean {
        if (!findUrl(text, match, cb, false, false)) return false

        val wideMatched = match.matchedText
        val start = text.indexOf(wideMatched)
        if (start == -1) return false
        val end = start + wideMatched.length - 1

        match.setValues(
            start = start.toUInt(),
            end = end.toUInt(),
            url = match.url,
            label = match.label,
            query = match.query,
            tooltip = match.tooltip,
            icon = match.icon,
            style = match.style,
            menuName = match.menuName,
            location = match.location,
            matchedText = match.matchedText,
            id = match.id,
            underline = match.underline,
            trusted = false,
            skipIcon = match.skipProfileIcon
        )
        return true
    }

    fun hasUrl(text: String): Boolean {
        val match = UrlMatch()
        return findUrl(text, match)
    }

    fun isUrl(text: String): Boolean {
        val match = UrlMatch()
        if (findUrl(text, match)) {
            return match.start == 0u && match.end >= (text.length - 1).toUInt()
        }
        return false
    }

    fun containsAgentMention(text: String): Boolean {
        if (!stringHasUrl(text)) return false
        return try {
            urlEntryAgentMention.pattern.findAll(text).any { result ->
                urlEntryAgentMention.isAgentId(result.value)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun setKeybindingHandler(handler: KeyBindingHandler) {
        urlEntryKeybinding.handler = handler
    }

    private fun matchRegex(text: String, pattern: Regex): Pair<Int, Int>? {
        val result = pattern.find(text) ?: return null
        val start = result.range.first
        var end = result.range.last

        when {
            text[end] == '.' || text[end] == ',' -> end--
            text[end] == ')' && !text.substring(start, end).contains('(') -> end--
            text[end] == ']' && !text.substring(start, end).contains('[') -> end--
        }

        return Pair(start, end)
    }

    private fun stringHasUrl(text: String): Boolean {
        val lower = text.lowercase()
        return text.contains("://") ||
                lower.contains("www.") ||
                lower.contains(".com") ||
                lower.contains(".net") ||
                lower.contains(".edu") ||
                lower.contains(".org") ||
                text.contains("<nolink>") ||
                text.contains("<icon") ||
                text.contains("@")
    }

    private fun stringHasJira(text: String): Boolean {
        val jiraPrefixes = listOf(
            "ARVD", "BUG", "CHOP", "CHUIBUG", "CTS", "DOC", "DN", "ECC", "EXP",
            "FIRE", "FITMESH", "LEAP", "LLSD", "MATBUG", "MISC", "OPEN", "PATHBUG",
            "PLAT", "PYO", "SCR", "SH", "SINV", "SLS", "SNOW", "SOCIAL", "STORM",
            "SUN", "SUP", "SVC", "TPV", "VWR", "WEB"
        )
        return jiraPrefixes.any { text.contains(it) }
    }

    private fun normalizeUri(url: String): String {
        return try {
            val uri = java.net.URI(url).normalize()
            uri.toString()
        } catch (_: Exception) {
            url
        }
    }
}
