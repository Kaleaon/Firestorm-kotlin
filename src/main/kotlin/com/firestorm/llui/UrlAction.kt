package com.firestorm.llui

import java.net.URI

object UrlAction {
    var openUrlCallback: ((String) -> Unit)? = null
    var openUrlInternalCallback: ((String) -> Unit)? = null
    var openUrlExternalCallback: ((String) -> Unit)? = null
    var executeSlurlCallback: ((String, Boolean) -> Boolean)? = null

    fun openUrl(url: String) {
        openUrlCallback?.invoke(url)
    }

    fun openUrlInternal(url: String) {
        openUrlInternalCallback?.invoke(url)
    }

    fun openUrlExternal(url: String) {
        openUrlExternalCallback?.invoke(url)
    }

    fun executeSlurl(url: String, trustedContent: Boolean = true): Boolean {
        return executeSlurlCallback?.invoke(url, trustedContent) ?: false
    }

    fun clickAction(url: String, trustedContent: Boolean) {
        if (executeSlurlCallback != null && !executeSlurlCallback!!.invoke(url, trustedContent)) {
            openUrlCallback?.invoke(url)
        }
    }

    fun teleportToLocation(url: String) {
        val match = UrlMatch()
        if (UrlRegistry.findUrl(url, match)) {
            if (match.location.isNotEmpty()) {
                executeSlurl("secondlife:///app/teleport/" + match.location)
            }
        }
    }

    fun zoomInObject(url: String) {
        val objectId = getObjectId(url)
        val match = UrlMatch()
        if (isValidUuid(objectId) && UrlRegistry.findUrl(url, match)) {
            executeSlurl("secondlife:///app/object/$objectId/zoomin/" + match.location)
        }
    }

    fun showLocationOnMap(url: String) {
        val match = UrlMatch()
        if (UrlRegistry.findUrl(url, match)) {
            if (match.location.isNotEmpty()) {
                executeSlurl("secondlife:///app/worldmap/" + match.location)
            }
        }
    }

    fun showParcelOnMap(url: String) {
        val pathParts = uriPathParts(url)
        if (pathParts.size < 3) return
        val parcelId = uriUnescape(pathParts[2])
        TODO("APR: use JVM equivalent — look up parcel position by id, then execute worldmap_global SLURL")
    }

    fun copyUrlToClipboard(url: String) {
        TODO("APR: use JVM equivalent — copy url string to system clipboard")
    }

    fun copyLabelToClipboard(url: String) {
        val match = UrlMatch()
        if (UrlRegistry.findUrl(url, match)) {
            TODO("APR: use JVM equivalent — copy match.label to system clipboard")
        }
    }

    fun getUrlLabel(url: String): String {
        val match = UrlMatch()
        return if (UrlRegistry.findUrl(url, match)) match.label else ""
    }

    fun showProfile(url: String) {
        val parts = uriPathParts(url)
        if (parts.size == 4) {
            val idStr = parts[2]
            if (isValidUuid(idStr)) {
                val cmd = parts[1]
                executeSlurl("secondlife:///app/$cmd/$idStr/about")
            }
        }
    }

    fun getUserId(url: String): String {
        val parts = uriPathParts(url)
        return if (parts.size == 4) parts[2] else ""
    }

    fun getObjectId(url: String): String {
        val parts = uriPathParts(url)
        return if (parts.size >= 3) parts[2] else ""
    }

    fun getObjectName(url: String): String {
        val query = uriQueryParam(url, "name")
        return query ?: ""
    }

    fun sendIm(url: String) {
        val idStr = getUserId(url)
        if (isValidUuid(idStr)) {
            executeSlurl("secondlife:///app/agent/$idStr/im")
        }
    }

    fun addFriend(url: String) {
        val idStr = getUserId(url)
        if (isValidUuid(idStr)) {
            executeSlurl("secondlife:///app/agent/$idStr/requestfriend")
        }
    }

    fun removeFriend(url: String) {
        val idStr = getUserId(url)
        if (isValidUuid(idStr)) {
            executeSlurl("secondlife:///app/agent/$idStr/removefriend")
        }
    }

    fun reportAbuse(url: String) {
        val idStr = getUserId(url)
        if (isValidUuid(idStr)) {
            executeSlurl("secondlife:///app/agent/$idStr/reportAbuse")
        }
    }

    fun blockObject(url: String) {
        val objectId = getObjectId(url)
        val objectName = getObjectName(url)
        if (isValidUuid(objectId)) {
            executeSlurl("secondlife:///app/agent/$objectId/block/${uriEscape(objectName)}")
        }
    }

    fun unblockObject(url: String) {
        val objectId = getObjectId(url)
        val objectName = getObjectName(url)
        if (isValidUuid(objectId)) {
            executeSlurl("secondlife:///app/agent/$objectId/unblock/$objectName")
        }
    }

    fun extractUuidFromSlurl(url: String): String {
        val parts = uriPathParts(url)
        if (parts.size >= 3) {
            val idStr = parts[2]
            if (isValidUuid(idStr)) return idStr
        }
        return ""
    }

    private fun uriPathParts(url: String): List<String> {
        return try {
            val path = URI(url).path ?: return emptyList()
            path.split("/").filter { it.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun uriQueryParam(url: String, key: String): String? {
        return try {
            val query = URI(url).query ?: return null
            query.split("&").firstNotNullOfOrNull { pair ->
                val (k, v) = pair.split("=", limit = 2).let {
                    it[0] to (it.getOrNull(1) ?: "")
                }
                if (k == key) v else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun uriUnescape(s: String): String = java.net.URLDecoder.decode(s, "UTF-8")

    private fun uriEscape(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    private val UUID_REGEX = Regex(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    )

    fun isValidUuid(s: String): Boolean = UUID_REGEX.matches(s)
}
