package com.firestorm.llui

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

const val APP_HEADER_REGEX =
    "(((hop|x-grid-location-info)://[-\\w.:\\@]+/app)|((hop|secondlife):///app))"

typealias UrlLabelCallback = (url: String, label: String, icon: String) -> Unit

data class UrlEntryObserver(
    val url: String,
    val callbacks: MutableList<UrlLabelCallback> = mutableListOf()
)

abstract class UrlEntryBase {
    var pattern: Regex = Regex("")
        protected set
    protected var icon: String = ""
    var menuName: String = ""
        protected set
    protected var tooltip: String = ""

    protected val observers: MutableMap<String, MutableList<UrlEntryObserver>> = mutableMapOf()

    open fun getUrl(string: String): String = escapeUrl(string)

    open fun getLabel(url: String, cb: UrlLabelCallback): String = url

    open fun getQuery(url: String): String = ""

    open fun getIcon(url: String): String = icon

    open fun getStyle(url: String): StyleParams = StyleParams(
        color = htmlLinkColor(),
        readonlyColor = htmlLinkColor(),
        fontStyle = "UNDERLINE",
        isLink = true
    )

    open fun getTooltip(string: String): String = tooltip

    open fun getLocation(url: String): String = ""

    open fun getUnderline(string: String): UnderlineLink = UnderlineLink.UNDERLINE_ALWAYS

    open fun isTrusted(): Boolean = false

    open fun getSkipProfileIcon(string: String): Boolean = false

    open fun getId(string: String): String = ""

    open fun isAgentId(url: String): Boolean = false

    open fun isSlurlValid(url: String): Boolean = true

    fun isLinkDisabled(): Boolean = false

    fun isWikiLinkCorrect(url: String): Boolean {
        var label = getLabelFromWikiLink(url)
        label = label.replace("​", "")
        label = label.map { c ->
            when (c) {
                '․', '﹒', '．', 'ׅ' -> '.'
                'ː', '：', '∶', '﹕' -> ':'
                '／' -> '/'
                'Һ', 'һ' -> 'h'
                else -> c
            }
        }.joinToString("")

        if ((label.contains(".com") || label.contains("www.")) && !label.contains("://")) {
            label = "https://$label"
        }
        return !UrlRegistry.hasUrl(label)
    }

    protected fun getIdStringFromUrl(url: String): String {
        val parts = uriPathParts(url)
        return if (parts.size == 4) parts[2] else ""
    }

    protected fun escapeUrl(url: String): String {
        val noEscape = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyz" +
                "0123456789" +
                "-._~!\$?&()*+,@:;=/%#"
        return buildString {
            for (ch in url) {
                if (ch in noEscape) append(ch)
                else {
                    ch.toString().toByteArray(Charsets.UTF_8).forEach { b ->
                        append("%%%02X".format(b.toInt() and 0xFF))
                    }
                }
            }
        }
    }

    protected fun unescapeUrl(url: String): String =
        URLDecoder.decode(url, "UTF-8")

    protected fun getLabelFromWikiLink(url: String): String {
        var start = 0
        while (start < url.length && !url[start].isWhitespace()) start++
        while (start < url.length && (url[start] == ' ' || url[start] == '\t')) start++
        return unescapeUrl(url.substring(start, url.length - 1))
    }

    protected fun getUrlFromWikiLink(string: String): String {
        var end = 0
        while (end < string.length && !string[end].isWhitespace()) end++
        return escapeUrl(string.substring(1, end))
    }

    protected fun addObserver(id: String, url: String, cb: UrlLabelCallback) {
        val list = observers.getOrPut(id) { mutableListOf() }
        list.add(UrlEntryObserver(url, mutableListOf(cb)))
    }

    protected fun urlToLabelWithGreyQuery(url: String): String {
        if (url.isEmpty()) return url
        return try {
            val uri = URI(escapeUrl(url))
            val sb = StringBuilder()
            if (uri.scheme != null) sb.append(uri.scheme).append("://")
            if (uri.host != null) sb.append(uri.host)
            if (uri.port != -1) sb.append(":").append(uri.port)
            if (uri.path != null) sb.append(uri.path)
            unescapeUrl(sb.toString())
        } catch (_: Exception) {
            ""
        }
    }

    protected fun urlToGreyQuery(url: String): String {
        return try {
            val uri = URI(escapeUrl(url))
            val base = buildString {
                if (uri.scheme != null) append(uri.scheme).append("://")
                if (uri.host != null) append(uri.host)
                if (uri.port != -1) append(":").append(uri.port)
                if (uri.path != null) append(uri.path)
            }
            val escaped = escapeUrl(url)
            val pos = escaped.indexOf(base)
            if (pos == -1) return ""
            unescapeUrl(escaped.substring(pos + base.length))
        } catch (_: Exception) {
            ""
        }
    }

    open fun callObservers(id: String, label: String, icon: String) {
        val list = observers.remove(id) ?: return
        for (obs in list) {
            for (cb in obs.callbacks) {
                cb(obs.url, label, icon)
            }
        }
    }

    protected fun uriPathParts(url: String): List<String> = try {
        URI(url).path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    protected fun getStringAfterToken(str: String, token: String): String {
        val pos = str.indexOf(token)
        return if (pos == -1) "" else str.substring(pos + token.length)
    }

    companion object {
        var agentId: String = ""

        private fun htmlLinkColor(): Int = 0xFF0099FF.toInt()
    }
}

class UrlEntryHttp : UrlEntryBase() {
    init {
        pattern = Regex("""(https?|ftp)://([^\s/?\.#]+\.?)+\.\w+(:\d+)?(/[^\s]*)?""",
            setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_http.xml"
        tooltip = "TooltipHttpUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String = urlToLabelWithGreyQuery(url)
    override fun getQuery(url: String): String = urlToGreyQuery(url)
    override fun getTooltip(string: String): String = tooltip

    override fun getUrl(string: String): String =
        if (!string.contains("://")) "https://${escapeUrl(string)}" else escapeUrl(string)
}

class UrlEntryHttpLabel : UrlEntryBase() {
    init {
        pattern = Regex("""\[(https?|ftp)://\S+[ \t]+[^\]]+\]""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_http.xml"
        tooltip = "TooltipHttpUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val label = getLabelFromWikiLink(url)
        return if (!UrlRegistry.hasUrl(label)) label else getUrl(url)
    }

    override fun getTooltip(string: String): String = getUrl(string)
    override fun getUrl(string: String): String = getUrlFromWikiLink(string)
}

class UrlEntryHttpNoProtocol : UrlEntryBase() {
    init {
        pattern = Regex("""\b(www|ftp)\.\S+\.([^\s<]*)?\b""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_http.xml"
        tooltip = "TooltipHttpUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String = urlToLabelWithGreyQuery(url)
    override fun getQuery(url: String): String = urlToGreyQuery(url)
    override fun getTooltip(url: String): String = tooltip

    override fun getUrl(string: String): String =
        if (!string.contains("://")) "http://${escapeUrl(string)}" else escapeUrl(string)
}

class UrlEntryInvalidSlurl : UrlEntryBase() {
    init {
        pattern = Regex(
            """(https?://(maps\.secondlife\.com|slurl\.com)/secondlife/|secondlife://(/app/(worldmap|teleport)/)?)[^ /]+(/-?[0-9]+){1,3}(/?(\\?title|\\?img|\\?msg)=\S*)?/?""",
            setOf(RegexOption.IGNORE_CASE)
        )
        menuName = "menu_url_http.xml"
        tooltip = "TooltipHttpUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String = escapeUrl(url)
    override fun getUrl(string: String): String = escapeUrl(string)
    override fun getTooltip(url: String): String = unescapeUrl(url)

    override fun isSlurlValid(url: String): Boolean {
        val actualParts = when {
            url.contains(".com/secondlife/") -> 5
            url.contains("/app/") -> 6
            else -> 3
        }
        val parts = uriPathParts(url)
        val n = parts.size

        fun toInt(s: String) = s.toIntOrNull() ?: -1

        return when (n) {
            actualParts -> {
                val x = toInt(parts[n - 3]); val y = toInt(parts[n - 2]); val z = toInt(parts[n - 1])
                x in 0..256 && y in 0..256 && z >= 0
            }
            actualParts - 1 -> {
                val x = toInt(parts[n - 2]); val y = toInt(parts[n - 1])
                x in 0..256 && y in 0..256
            }
            actualParts - 2 -> {
                val x = toInt(parts[n - 1])
                x in 0..256
            }
            else -> false
        }
    }
}

class UrlEntrySlurl : UrlEntryBase() {
    init {
        pattern = Regex(
            """https?://(maps\.secondlife\.com|slurl\.com)/secondlife/[^ /]+(/\d+){0,3}(/?(\\?title|\\?img|\\?msg)=\S*)?/?""",
            setOf(RegexOption.IGNORE_CASE)
        )
        icon = "Hand"
        menuName = "menu_url_slurl.xml"
        tooltip = "TooltipSLURL"
    }

    override fun isTrusted(): Boolean = true

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val parts = uriPathParts(url)
        val n = parts.size
        return when (n) {
            5 -> "${unescapeUrl(parts[n - 4])} (${parts[n-3]},${parts[n-2]},${parts[n-1]})"
            4 -> "${unescapeUrl(parts[n - 3])} (${parts[n-2]},${parts[n-1]})"
            3 -> "${unescapeUrl(parts[n - 2])} (${parts[n-1]})"
            2 -> unescapeUrl(parts[n - 1])
            else -> url
        }
    }

    override fun getLocation(url: String): String {
        var search = "/secondlife"
        var pos = url.indexOf(search)
        if (pos == -1) {
            search = "/region"
            pos = url.indexOf(search)
            if (pos == -1) return ""
        }
        return url.substring(pos + search.length + 1)
    }
}

class UrlEntrySecondlifeUrl : UrlEntryBase() {
    init {
        pattern = Regex(
            """((http://([-\w\.]*\.)?(secondlife|lindenlab|tilia-inc)\.com)""" +
            """|(http://([-\w\.]*\.)?secondlifegrid\.net)""" +
            """|(https://([-\w\.]*\.)?(secondlife|lindenlab|tilia-inc)\.com(:\d{1,5})?)""" +
            """|(https://([-\w\.]*\.)?secondlifegrid\.net(:\d{1,5})?)""" +
            """|(https?://([-\w\.]*\.)?secondlife\.io(:\d{1,5})?))/\S*""",
            setOf(RegexOption.IGNORE_CASE)
        )
        icon = "Hand"
        menuName = "menu_url_http.xml"
        tooltip = "TooltipHttpUrl"
    }

    override fun isTrusted(): Boolean = true

    override fun getUrl(string: String): String =
        if (!string.contains("://")) "https://${escapeUrl(string)}" else escapeUrl(string)

    override fun getLabel(url: String, cb: UrlLabelCallback): String = urlToLabelWithGreyQuery(url)
    override fun getQuery(url: String): String = urlToGreyQuery(url)
    override fun getTooltip(url: String): String = tooltip
}

class UrlEntrySimpleSecondlifeUrl : UrlEntrySecondlifeUrl() {
    init {
        pattern = Regex(
            """https?://([-\w\.]*\.)?(secondlife|lindenlab|tilia-inc)\.com(?!\S)""" +
            """|https?://([-\w\.]*\.)?secondlifegrid\.net(?!\S)""",
            setOf(RegexOption.IGNORE_CASE)
        )
        icon = "Hand"
        menuName = "menu_url_http.xml"
    }
}

open class UrlEntryAgent : UrlEntryBase() {
    private val avatarNameCacheCallbacks: MutableMap<String, MutableList<UrlLabelCallback>> = mutableMapOf()

    init {
        pattern = Regex("""$APP_HEADER_REGEX/agent/[\da-f-]+/\w+""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_agent.xml"
        icon = "Generic_Person"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val agentIdStr = getIdStringFromUrl(url)
        if (agentIdStr.isEmpty()) return unescapeUrl(url)
        if (!UrlAction.isValidUuid(agentIdStr)) return "Nobody"
        addObserver(agentIdStr, url, cb)
        return "..."
    }

    override fun getIcon(url: String): String = icon

    override fun getTooltip(string: String): String {
        val url = getUrl(string)
        return when {
            url.endsWith("/inspect") -> "TooltipAgentInspect"
            url.endsWith("/mute") -> "TooltipAgentMute"
            url.endsWith("/unmute") -> "TooltipAgentUnmute"
            url.endsWith("/im") -> "TooltipAgentIM"
            url.endsWith("/pay") -> "TooltipAgentPay"
            url.endsWith("/offerteleport") -> "TooltipAgentOfferTeleport"
            url.endsWith("/requestfriend") -> "TooltipAgentRequestFriend"
            else -> "TooltipAgentUrl"
        }
    }

    override fun getStyle(url: String): StyleParams {
        val base = super.getStyle(url)
        return base.copy(color = htmlLinkColor(), readonlyColor = htmlLinkColor())
    }

    override fun getUnderline(string: String): UnderlineLink {
        val url = getUrl(string)
        return if (url.endsWith("/about") || url.endsWith("/inspect"))
            UnderlineLink.UNDERLINE_ON_HOVER
        else
            UnderlineLink.UNDERLINE_ALWAYS
    }

    override fun getId(string: String): String = getIdStringFromUrl(string)

    override fun isAgentId(url: String): Boolean = agentId == getId(url)

    override fun callObservers(id: String, label: String, icon: String) {
        val list = observers.remove(id) ?: return
        for (obs in list) {
            val finalLabel = localizeSlappLabel(obs.url, label)
            for (cb in obs.callbacks) cb(obs.url, finalLabel, icon)
        }
    }

    private fun htmlLinkColor(): Int = 0xFF0099FF.toInt()

    companion object {
        fun localizeSlappLabel(url: String, fullName: String): String = when {
            url.endsWith("/mute") -> "Mute $fullName"
            url.endsWith("/unmute") -> "Unmute $fullName"
            url.endsWith("/im") -> "IM $fullName"
            url.endsWith("/pay") -> "Pay $fullName"
            url.endsWith("/offerteleport") -> "Offer Teleport $fullName"
            url.endsWith("/requestfriend") -> "Request Friend $fullName"
            url.endsWith("/removefriend") -> "Remove Friend $fullName"
            url.endsWith("/mention") -> "@$fullName"
            else -> fullName
        }
    }
}

class UrlEntryAgentMention : UrlEntryAgent() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/agent/[\da-f-]+/mention""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_agent.xml"
        icon = ""
    }

    override fun getUnderline(string: String): UnderlineLink = UnderlineLink.UNDERLINE_NEVER

    override fun getStyle(url: String): StyleParams {
        val base = super.getStyle(url)
        return base.copy(
            color = chatMentionColor(),
            readonlyColor = chatMentionColor(),
            fontStyle = "NORMAL",
            drawHighlightBg = true,
            highlightBgColor = if (getIdStringFromUrl(url) == agentId) selfMentionHighlight() else mentionHighlight()
        )
    }

    override fun getSkipProfileIcon(string: String): Boolean = true

    private fun chatMentionColor(): Int = 0xFF8800FF.toInt()
    private fun mentionHighlight(): Int = 0x33AAFFFF.toInt()
    private fun selfMentionHighlight(): Int = 0x33FF88FF.toInt()
}

abstract class UrlEntryAgentName : UrlEntryBase() {
    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val agentIdStr = getIdStringFromUrl(url)
        if (agentIdStr.isEmpty()) return unescapeUrl(url)
        if (!UrlAction.isValidUuid(agentIdStr)) return "Nobody"
        addObserver(agentIdStr, url, cb)
        return "..."
    }

    override fun getStyle(url: String): StyleParams = StyleParams(isLink = false)

    protected abstract fun getName(avatarDisplayName: String, avatarAccountName: String, avatarCompleteName: String, avatarLegacyName: String): String
}

class UrlEntryAgentCompleteName : UrlEntryAgentName() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/agent/[\da-f-]+/completename""", setOf(RegexOption.IGNORE_CASE))
    }

    override fun getName(avatarDisplayName: String, avatarAccountName: String, avatarCompleteName: String, avatarLegacyName: String): String =
        avatarCompleteName
}

class UrlEntryAgentLegacyName : UrlEntryAgentName() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/agent/[\da-f-]+/legacyname""", setOf(RegexOption.IGNORE_CASE))
    }

    override fun getName(avatarDisplayName: String, avatarAccountName: String, avatarCompleteName: String, avatarLegacyName: String): String =
        avatarLegacyName
}

class UrlEntryAgentDisplayName : UrlEntryAgentName() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/agent/[\da-f-]+/displayname""", setOf(RegexOption.IGNORE_CASE))
    }

    override fun getName(avatarDisplayName: String, avatarAccountName: String, avatarCompleteName: String, avatarLegacyName: String): String =
        avatarDisplayName
}

class UrlEntryAgentUserName : UrlEntryAgentName() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/agent/[\da-f-]+/username""", setOf(RegexOption.IGNORE_CASE))
    }

    override fun getName(avatarDisplayName: String, avatarAccountName: String, avatarCompleteName: String, avatarLegacyName: String): String =
        avatarAccountName
}

class UrlEntryAgentRlvAnonymizedName : UrlEntryAgentName() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/agent/[\da-f-]+/rlvanonym""", setOf(RegexOption.IGNORE_CASE))
    }

    override fun getName(avatarDisplayName: String, avatarAccountName: String, avatarCompleteName: String, avatarLegacyName: String): String =
        "An individual"
}

class FsUrlEntryAgentSelf : UrlEntryAgent() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/agentself/[\da-f-]+/\w+""", setOf(RegexOption.IGNORE_CASE))
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        return "You"
    }
}

class UrlEntryExperienceProfile : UrlEntryBase() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/experience/[\da-f-]+/profile""", setOf(RegexOption.IGNORE_CASE))
        icon = "Generic_Experience"
        menuName = "menu_url_experience.xml"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val idStr = getIdStringFromUrl(url)
        if (idStr.isEmpty()) return unescapeUrl(url)
        if (!UrlAction.isValidUuid(idStr)) return "ExperienceNameNull"
        addObserver(idStr, url, cb)
        return "LoadingData"
    }
}

class UrlEntryGroup : UrlEntryBase() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/group/[\da-f-]+/\w+""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_group.xml"
        icon = "Generic_Group"
        tooltip = "TooltipGroupUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val groupIdStr = getIdStringFromUrl(url)
        if (groupIdStr.isEmpty()) return unescapeUrl(url)
        if (!UrlAction.isValidUuid(groupIdStr)) return "GroupNameNone"
        addObserver(groupIdStr, url, cb)
        return "..."
    }

    override fun getStyle(url: String): StyleParams {
        val base = super.getStyle(url)
        return base.copy(color = 0xFF0099FF.toInt(), readonlyColor = 0xFF0099FF.toInt())
    }

    override fun getId(string: String): String = getIdStringFromUrl(string)
}

class UrlEntryInventory : UrlEntryBase() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/inventory/[\da-f-]+/\w+\S*""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_inventory.xml"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val label = getStringAfterToken(url, "name=")
        return unescapeUrl(if (label.isEmpty()) url else label)
    }
}

class UrlEntryObjectIm : UrlEntryBase() {
    init {
        pattern = Regex("""(hop|secondlife):///app/objectim/[\da-f-]+\?[^ \t\r\n\v\f]*""",
            setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_objectim.xml"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val name = uriQueryParam(url, "name")
        return if (name != null) name else unescapeUrl(url)
    }

    override fun getLocation(url: String): String {
        return uriQueryParam(url, "slurl") ?: super.getLocation(url)
    }

    private fun uriQueryParam(url: String, key: String): String? = try {
        val query = URI(url).query ?: return null
        query.split("&").firstNotNullOfOrNull { pair ->
            val idx = pair.indexOf('=')
            if (idx == -1) null
            else {
                val k = pair.substring(0, idx)
                val v = pair.substring(idx + 1)
                if (k == key) v else null
            }
        }
    } catch (_: Exception) {
        null
    }
}

class UrlEntryChat : UrlEntryBase() {
    init {
        pattern = Regex("""secondlife:///app/chat/\d+/\S+""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_slapp.xml"
        tooltip = "TooltipSLAPP"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String = unescapeUrl(url)
}

data class ParcelData(
    val parcelId: String,
    val name: String,
    val simName: String,
    val globalX: Float,
    val globalY: Float,
    val globalZ: Float
)

class UrlEntryParcel : UrlEntryBase() {
    init {
        pattern = Regex("""$APP_HEADER_REGEX/parcel/[\da-f-]+/about""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_parcel.xml"
        tooltip = "TooltipParcelUrl"
        parcelInfoObservers.add(this)
    }

    protected fun finalize() {
        parcelInfoObservers.remove(this)
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val parts = uriPathParts(url)
        if (parts.size < 3) return url
        val parcelIdStr = unescapeUrl(parts[2])
        addObserver(parcelIdStr, url, cb)
        sendParcelInfoRequest(parcelIdStr)
        return unescapeUrl(url)
    }

    fun sendParcelInfoRequest(parcelId: String) {
        TODO("APR: use JVM equivalent — send ParcelInfoRequest message via network layer")
    }

    fun onParcelInfoReceived(id: String, label: String) {
        callObservers(id, label.ifEmpty { "RegionInfoError" }, icon)
    }

    companion object {
        var sessionId: String = ""
        var regionHost: String = ""
        var isDisconnected: Boolean = false

        private val parcelInfoObservers: MutableSet<UrlEntryParcel> = mutableSetOf()
        private val parcelPos: MutableMap<String, Triple<Double, Double, Double>> = mutableMapOf()

        fun processParcelInfo(data: ParcelData) {
            val label = when {
                data.name.isNotEmpty() -> data.name
                data.simName.isNotEmpty() -> {
                    val rx = data.globalX.toInt() % 256
                    val ry = data.globalY.toInt() % 256
                    val rz = data.globalZ.toInt()
                    "${data.simName} ($rx, $ry, $rz)"
                }
                else -> ""
            }
            for (obs in parcelInfoObservers) {
                obs.onParcelInfoReceived(data.parcelId, label)
            }
            if (!parcelPos.containsKey(data.parcelId)) {
                parcelPos[data.parcelId] = Triple(
                    data.globalX.toDouble(),
                    data.globalY.toDouble(),
                    data.globalZ.toDouble()
                )
            }
        }

        fun getParcelPos(parcelId: String): Triple<Double, Double, Double> =
            parcelPos[parcelId] ?: Triple(0.0, 0.0, 0.0)
    }
}

class UrlEntryPlace : UrlEntryBase() {
    init {
        pattern = Regex(
            """(((hop://[-\w.\:\@]+/)|((x-grid-location-info://[-\w.]+/region/)|(secondlife://)))\S+/?(\d+/\d+/-?\d+|\d+/-?\d+)/?)|(hop://[-\w.\:\@]+/[^\s/]+/?(?![^\s]))""",
            setOf(RegexOption.IGNORE_CASE)
        )
        menuName = "menu_url_slurl.xml"
        tooltip = "TooltipSLURL"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val uri = try { URI(url) } catch (_: Exception) { return url }
        val location = unescapeUrl(uri.host ?: "")
        val parts = uri.path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
        return when (parts.size) {
            3 -> "$location (${parts[0]},${parts[1]},${parts[2]})"
            2 -> "$location (${parts[0]},${parts[1]})"
            else -> url
        }
    }

    override fun getLocation(url: String): String = getStringAfterToken(url, "://")
}

class UrlEntryRegion : UrlEntryBase() {
    init {
        pattern = Regex(
            """secondlife:///app/region/[A-Za-z0-9()_%]+(/\d+)?(/\d+)?(/\d+)?/?""",
            setOf(RegexOption.IGNORE_CASE)
        )
        menuName = "menu_url_slurl.xml"
        tooltip = "TooltipSLURL"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val parts = uriPathParts(url)
        if (parts.size < 3) return url
        var label = unescapeUrl(parts[2])
        if (parts.size > 3) {
            label += " (${parts[3]}"
            if (parts.size > 4) {
                label += ",${parts[4]}"
                if (parts.size > 5) label += ",${parts[5]}"
            }
            label += ")"
        }
        return label
    }

    override fun getLocation(url: String): String {
        val parts = uriPathParts(url)
        return if (parts.size >= 3) unescapeUrl(parts[2]) else ""
    }
}

class UrlEntryTeleport : UrlEntryBase() {
    init {
        pattern = Regex(
            """$APP_HEADER_REGEX/teleport/\S+(/\d+)?(/\d+)?(/\d+)?/?\S*""",
            setOf(RegexOption.IGNORE_CASE)
        )
        menuName = "menu_url_teleport.xml"
        tooltip = "TooltipTeleportUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val uri = try { URI(url) } catch (_: Exception) { return url }
        val parts = uriPathParts(url)
        val n = parts.size
        val host = uri.host ?: ""
        val base = "Teleport" + if (host.isNotEmpty()) " $host" else ""
        return when (n) {
            6 -> "$base ${unescapeUrl(parts[n-4])} (${parts[n-3]},${parts[n-2]},${parts[n-1]})"
            5 -> "$base ${unescapeUrl(parts[n-3])} (${parts[n-2]},${parts[n-1]})"
            4 -> "$base ${unescapeUrl(parts[n-2])} (${parts[n-1]})"
            3 -> "$base ${unescapeUrl(parts[n-1])}"
            else -> url
        }
    }

    override fun getLocation(url: String): String = getStringAfterToken(url, "app/teleport/")
}

class FsUrlEntryWear : UrlEntryBase() {
    init {
        pattern = Regex("""(hop|secondlife):///app/wear_folder/\S+""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_slapp.xml"
        tooltip = "TooltipFSUrlEntryWear"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String = "Wear Folder"
}

class FsHelpDebugUrlEntrySl : UrlEntryBase() {
    init {
        pattern = Regex("""(hop|secondlife):///app/fshelp/showdebug/\S+""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_slapp.xml"
        tooltip = "TooltipFSHelpDebugSLUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val pos = url.indexOf("showdebug/")
        return if (pos == -1) url else url.substring(pos + 10)
    }
}

class UrlEntrySl : UrlEntryBase() {
    init {
        pattern = Regex("""(hop|secondlife)://(\w+)?(:\d+)?/\S+""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_slapp.xml"
        tooltip = "TooltipSLAPP"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String = unescapeUrl(url)
}

class UrlEntrySlLabel : UrlEntryBase() {
    init {
        pattern = Regex("""\[(hop|secondlife)://\S+[ \t]+[^\]]+\]""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_slapp.xml"
        tooltip = "TooltipSLAPP"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val label = getLabelFromWikiLink(url)
        return if (!UrlRegistry.hasUrl(label)) label else getUrl(url)
    }

    override fun getUrl(string: String): String = getUrlFromWikiLink(string)

    override fun getTooltip(string: String): String {
        val url = getUrl(string)
        val match = UrlMatch()
        return if (UrlRegistry.findUrl(url, match)) match.tooltip else super.getTooltip(string)
    }

    override fun getUnderline(string: String): UnderlineLink {
        val url = getUrl(string)
        val match = UrlMatch()
        return if (UrlRegistry.findUrl(url, match)) match.underline else super.getUnderline(string)
    }
}

class UrlEntryWorldMap : UrlEntryBase() {
    init {
        pattern = Regex(
            """$APP_HEADER_REGEX/worldmap/\S+/?(\d+)?/?(\d+)?/?(\d+)?/?\S*""",
            setOf(RegexOption.IGNORE_CASE)
        )
        menuName = "menu_url_map.xml"
        tooltip = "TooltipMapUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val parts = uriPathParts(url)
        if (parts.size < 3) return url
        val location = unescapeUrl(parts[2])
        val x = if (parts.size > 3) parts[3] else "128"
        val y = if (parts.size > 4) parts[4] else "128"
        val z = if (parts.size > 5) parts[5] else "0"
        return "Show on Map $location ($x,$y,$z)"
    }

    override fun getLocation(url: String): String = getStringAfterToken(url, "app/worldmap/")
}

class UrlEntryNoLink : UrlEntryBase() {
    init {
        pattern = Regex("""<nolink>.*?</nolink>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    }

    override fun getUrl(string: String): String = string.substring(8, string.length - 9)
    override fun getLabel(url: String, cb: UrlLabelCallback): String = getUrl(url)
    override fun getStyle(url: String): StyleParams = StyleParams(isLink = false)
}

class UrlEntryIcon : UrlEntryBase() {
    init {
        pattern = Regex("""<icon\s*>\s*([^<]*)?\s*</icon\s*>""", setOf(RegexOption.IGNORE_CASE))
    }

    override fun getUrl(string: String): String = ""
    override fun getLabel(url: String, cb: UrlLabelCallback): String = ""

    override fun getIcon(url: String): String {
        val match = Regex("""<icon\s*>\s*([^<]*)?\s*</icon\s*>""", setOf(RegexOption.IGNORE_CASE)).find(url)
        icon = match?.groupValues?.getOrNull(1)?.trim() ?: ""
        return icon
    }
}

class UrlEntryEmail : UrlEntryBase() {
    init {
        pattern = Regex("""(mailto:)?[\w.\-]+@[\w.\-]+\.[a-z]{2,63}""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_email.xml"
        tooltip = "TooltipEmail"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val pos = url.indexOf("mailto:")
        return if (pos == -1) escapeUrl(url)
        else escapeUrl(url.substring(pos + 7))
    }

    override fun getUrl(string: String): String =
        if (!string.contains("mailto:")) "mailto:${escapeUrl(string)}" else escapeUrl(string)
}

class UrlEntryIPv6 : UrlEntryBase() {
    private val hostPath = """https?://\[([a-f0-9:]+:+)+[a-f0-9]+]"""

    init {
        pattern = Regex("""$hostPath(:\d{1,5})?(/\S*)?""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_http.xml"
        tooltip = "TooltipHttpUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val match = Regex(hostPath, setOf(RegexOption.IGNORE_CASE)).find(url)
        return if (match != null) url.substring(0, match.value.length) else url
    }

    override fun getQuery(url: String): String =
        Regex(hostPath, setOf(RegexOption.IGNORE_CASE)).replace(url, "")

    override fun getUrl(string: String): String = string
}

class UrlEntryKeybinding : UrlEntryBase() {
    var handler: KeyBindingHandler? = null
    private val localizations: MutableMap<String, LocalizationData> = mutableMapOf()

    init {
        pattern = Regex("""$APP_HEADER_REGEX/keybinding/\w+(\?mode=\w+)?$""", setOf(RegexOption.IGNORE_CASE))
        menuName = "menu_url_experience.xml"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String {
        val control = getControlName(url)
        val keybind = handler?.getKeyBindingAsString(getMode(url), control) ?: ""
        val locData = localizations[control]
        return if (locData != null) "${locData.localization}: $keybind" else "$control: $keybind"
    }

    override fun getTooltip(url: String): String {
        val control = getControlName(url)
        return localizations[control]?.tooltip ?: url
    }

    private fun getControlName(url: String): String {
        val search = "/keybinding/"
        val start = url.indexOf(search).takeIf { it != -1 }?.let { it + search.length } ?: return ""
        val end = url.indexOf("?mode=").takeIf { it != -1 } ?: url.length
        return url.substring(start, end)
    }

    private fun getMode(url: String): String {
        val search = "?mode="
        val start = url.indexOf(search).takeIf { it != -1 }?.let { it + search.length } ?: return ""
        return url.substring(start)
    }

    data class LocalizationData(val localization: String, val tooltip: String)
}

interface KeyBindingHandler {
    fun getKeyBindingAsString(mode: String, control: String): String
}

class UrlEntryJira : UrlEntryBase() {
    init {
        pattern = Regex(
            """((?:ARVD|BUG|CHOP|CHUIBUG|CTS|DOC|DN|ECC|EXP|FIRE|FITMESH|LEAP|LLSD|MATBUG|MISC|OPEN|PATHBUG|PLAT|PYO|SCR|SH|SINV|SLS|SNOW|SOCIAL|STORM|SUN|SVC|SPOT|SUN|SUP|TPV|VWR|WEB)-\d+)"""
        )
        menuName = "menu_url_http.xml"
        tooltip = "TooltipHttpUrl"
    }

    override fun getLabel(url: String, cb: UrlLabelCallback): String = unescapeUrl(url)
    override fun getTooltip(string: String): String = getUrl(string)

    override fun getUrl(string: String): String {
        return if (string.contains("FIRE") || string.contains("SLS") || string.contains("SUP")) {
            "https://jira.firestormviewer.org/browse/$string"
        } else {
            "https://jira.secondlife.com/browse/$string"
        }
    }
}
