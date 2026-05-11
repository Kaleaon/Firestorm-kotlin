package com.firestorm.newview

import com.firestorm.llui.FloaterWebContent
import com.firestorm.llui.MediaCtrl

private val SEARCH_TYPES = setOf("standard", "land", "classified")
private val COLLECTION_TYPES = setOf("events", "destinations", "places", "groups", "people")

class SearchHandler : CommandHandler("search", trusted = false) {
    override fun handle(tokens: LLSD, queryMap: LLSD, grid: String, web: MediaCtrl?): Boolean {
        val parts = tokens.size()
        val collection = if (parts > 0) tokens[0].asString() else ""
        val searchText = if (parts > 1) tokens[1].asString() else ""
        FloaterReg.showInstance("search", llsdMapOf("collection" to collection, "query" to searchText))
        return true
    }
}

val gSearchHandler = SearchHandler()

class FloaterSearch(key: LLSD) : FloaterWebContent(key) {

    private val searchType: MutableSet<String> = mutableSetOf("standard", "land", "classified")
    private val collectionType: MutableSet<String> = mutableSetOf("events", "destinations", "places", "groups", "people")

    override fun onOpen(key: LLSD) {
        initiateSearch(key)
        webBrowser?.setFocus(true)
    }

    override fun onClose(appQuitting: Boolean) {
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        webBrowser?.setErrorPageURL(SavedSettings.getString("GenericErrorPageURL"))
        ViewerMedia.getInstance()?.getOpenIDCookie(webBrowser)

        getChildView("address")?.isEnabled = false
        getChildView("popexternal")?.isEnabled = false

        initiateSearch(LLSD())

        setHelpTopic("floater_search")
        return true
    }

    private fun initiateSearch(tokens: LLSD) {
        val url: String = LFSimFeatureHandler.instance().searchURL()

        val category = if (tokens.has("category")) tokens["category"].asString() else ""
        val searchText = if (tokens.has("query")) tokens["query"].asString() else ""
        val collection = if (tokens.has("collection")) tokens["collection"].asString() else ""

        val type = if (searchType.contains(category)) category else "standard"

        val escapedQuery = LLURI.escape(searchText)

        val collectionArg = if (type == "standard") {
            if (collectionType.contains(collection)) {
                "&collection_chosen=$collection"
            } else {
                collectionType.joinToString("") { "&collection_chosen=$it" }
            }
        } else {
            ""
        }

        val maturity = when {
            Agent.prefersAdult() -> "gma"
            Agent.prefersMature() -> "gm"
            else -> "g"
        }

        val godlike = if (Agent.isGodlike()) "1" else "0"

        val subs = mapOf(
            "TYPE" to type,
            "QUERY" to escapedQuery,
            "COLLECTION" to collectionArg,
            "MATURITY" to maturity,
            "GODLIKE" to godlike
        )

        val expandedUrl = Web.expandURLSubstitutions(url, subs)
        webBrowser?.navigateTo(expandedUrl, HTTP_CONTENT_TEXT_HTML)
    }
}
