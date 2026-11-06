package com.firestorm.newview

typealias LLMediaCtrl = Any

object LLURLDispatcher {

    fun dispatch(slurl: String, navType: String, web: LLMediaCtrl?, trustedBrowser: Boolean): Boolean =
        LLURLDispatcherImpl.dispatch(LLSLURL(slurl), navType, web, trustedBrowser)

    fun dispatchRightClick(slurl: String): Boolean =
        LLURLDispatcherImpl.dispatchRightClick(LLSLURL(slurl))

    fun dispatchFromTextEditor(slurl: String, trustedContent: Boolean): Boolean {
        val web: LLMediaCtrl? = null
        return LLURLDispatcherImpl.dispatch(LLSLURL(slurl), NAV_TYPE_CLICKED, web, trustedContent)
    }

    private const val NAV_TYPE_CLICKED = "clicked"
}

data class LLSLURL(val raw: String) {
    enum class SLURLType { EMPTY, APP, LOCATION, OTHER }

    fun getType(): SLURLType { System.err.println("LLSLURL: getType not yet implemented"); return SLURLType.EMPTY }
    fun getAppCmd(): String { System.err.println("LLSLURL: getAppCmd not yet implemented"); return "" }
    fun getAppPath(): List<String> { System.err.println("LLSLURL: getAppPath not yet implemented"); return emptyList() }
    fun getAppQuery(): String { System.err.println("LLSLURL: getAppQuery not yet implemented"); return "" }
    fun getAppQueryMap(): Map<String, Any> { System.err.println("LLSLURL: getAppQueryMap not yet implemented"); return emptyMap() }
    fun getRegion(): String { System.err.println("LLSLURL: getRegion not yet implemented"); return "" }
    fun getPosition(): Triple<Float, Float, Float> { System.err.println("LLSLURL: getPosition not yet implemented"); return Triple(0f, 0f, 0f) }
    fun getSLURLString(): String = raw
    fun getLocationString(): String { System.err.println("LLSLURL: getLocationString not yet implemented"); return "" }
    fun getGrid(): String { System.err.println("LLSLURL: getGrid not yet implemented"); return "" }
    fun getHypergrid(): Boolean { System.err.println("LLSLURL: getHypergrid not yet implemented"); return false }
}

private object LLURLDispatcherImpl {

    fun dispatch(slurl: LLSLURL, navType: String, web: LLMediaCtrl?, trustedBrowser: Boolean): Boolean {
        if (slurl.getType() == LLSLURL.SLURLType.EMPTY) return true
        return dispatchCore(slurl, navType, rightMouse = false, web = web, trustedBrowser = trustedBrowser)
    }

    fun dispatchRightClick(slurl: LLSLURL): Boolean =
        dispatchCore(slurl, NAV_TYPE_CLICKED, rightMouse = true, web = null, trustedBrowser = false)

    private fun dispatchCore(
        slurl: LLSLURL,
        navType: String,
        rightMouse: Boolean,
        web: LLMediaCtrl?,
        trustedBrowser: Boolean
    ): Boolean = when (slurl.getType()) {
        LLSLURL.SLURLType.APP -> dispatchApp(slurl, navType, rightMouse, web, trustedBrowser)
        LLSLURL.SLURLType.LOCATION -> dispatchRegion(slurl, navType, rightMouse)
        else -> false
    }

    private fun dispatchApp(
        slurl: LLSLURL,
        navType: String,
        rightMouse: Boolean,
        web: LLMediaCtrl?,
        trustedBrowser: Boolean
    ): Boolean {
        val queryMap = slurl.getAppQueryMap()
        val path = slurl.getAppPath()

        System.err.println("LLURLDispatcherImpl: dispatchApp not yet implemented")
        return false
    }

    private fun dispatchRegion(slurl: LLSLURL, navType: String, rightMouse: Boolean): Boolean {
        if (slurl.getType() != LLSLURL.SLURLType.LOCATION) return false

        System.err.println("LLURLDispatcherImpl: dispatchRegion not yet implemented")
        return false
    }

    fun regionNameCallback(regionHandle: Long, slurl: LLSLURL, snapshotId: java.util.UUID, teleport: Boolean) {
        if (slurl.getType() == LLSLURL.SLURLType.LOCATION) {
            regionHandleCallback(regionHandle, slurl, snapshotId, teleport)
        }
    }

    fun regionHandleCallback(regionHandle: Long, slurl: LLSLURL, snapshotId: java.util.UUID, teleport: Boolean) {
        System.err.println("LLURLDispatcherImpl: regionHandleCallback not yet implemented")
    }

    private fun handleGrid(slurl: LLSLURL): Boolean {
        System.err.println("LLURLDispatcherImpl: handleGrid not yet implemented")
        return false
    }

    private const val NAV_TYPE_CLICKED = "clicked"
}

class LLTeleportHandler {

    fun handle(tokens: List<String>, queryMap: Map<String, Any>, grid: String, web: LLMediaCtrl?): Boolean {
        if (tokens.isEmpty()) return false

        val regionName: String
        val callbackUrl: String

        if (tokens.size >= 4) {
            val coords = Triple(tokens[1].toFloatOrNull() ?: 128f, tokens[2].toFloatOrNull() ?: 128f, tokens[3].toFloatOrNull() ?: 0f)
            regionName = java.net.URLDecoder.decode(tokens[0], "UTF-8")
            System.err.println("LLTeleportHandler: LLSLURL(grid, regionName, coords) and TeleportViaSLAPP notification not yet implemented")
            return false
        } else {
            regionName = java.net.URLDecoder.decode(tokens[0], "UTF-8")
            System.err.println("LLTeleportHandler: LLSLURL construction and TeleportViaSLAPP notification not yet implemented")
            return false
        }
    }

    fun fromEvent(params: Map<String, Any>) {
        if (params.containsKey("regionname")) {
            val regionName = params["regionname"] as String
            val localX = (params["x"] as? Number)?.toFloat() ?: 128f
            val localY = (params["y"] as? Number)?.toFloat() ?: 128f
            val localZ = (params["z"] as? Number)?.toFloat() ?: 0f
            System.err.println("LLTeleportHandler: LLSLURL(regionName, localPos) and LLWorldMapMessage.sendNamedRegionRequest not yet implemented")
        } else {
            if (!params.containsKey("x") || !params.containsKey("y")) {
                error("Specify either regionname or global (x, y)")
            }
            val x = (params["x"] as Number).toDouble()
            val y = (params["y"] as Number).toDouble()
            val z = (params["z"] as? Number)?.toDouble() ?: 0.0
            System.err.println("LLTeleportHandler: gAgent.teleportViaLocation(globalPos) and LLFloaterWorldMap.trackLocation not yet implemented")
        }
    }

    companion object {
        fun teleportViaSlapp(regionName: String, callbackUrl: String) {
            System.err.println("LLTeleportHandler: teleportViaSlapp not yet implemented")
        }

        fun teleportViaSlappCallback(notification: Map<String, Any>, response: Map<String, Any>): Boolean {
            val option = (response["option"] as? Int) ?: return false
            val regionName = (notification["payload"] as? Map<*, *>)?.get("region_name") as? String ?: return false
            val callbackUrl = (notification["payload"] as? Map<*, *>)?.get("callback_url") as? String ?: return false
            if (option == 0) {
                teleportViaSlapp(regionName, callbackUrl)
                return true
            }
            return false
        }
    }
}
