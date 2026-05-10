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

    fun getType(): SLURLType = TODO("APR: use JVM equivalent for LLSLURL type parsing")
    fun getAppCmd(): String = TODO("APR: use JVM equivalent for LLSLURL app command extraction")
    fun getAppPath(): List<String> = TODO("APR: use JVM equivalent for LLSLURL app path extraction")
    fun getAppQuery(): String = TODO("APR: use JVM equivalent for LLSLURL app query string extraction")
    fun getAppQueryMap(): Map<String, Any> = TODO("APR: use JVM equivalent for LLSLURL query map")
    fun getRegion(): String = TODO("APR: use JVM equivalent for LLSLURL region name extraction")
    fun getPosition(): Triple<Float, Float, Float> = TODO("APR: use JVM equivalent for LLSLURL position extraction")
    fun getSLURLString(): String = raw
    fun getLocationString(): String = TODO("APR: use JVM equivalent for LLSLURL location string")
    fun getGrid(): String = TODO("APR: use JVM equivalent for LLSLURL grid extraction")
    fun getHypergrid(): Boolean = TODO("APR: use JVM equivalent for LLSLURL hypergrid flag")
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

        TODO("APR: use JVM equivalent for LLCommandDispatcher::dispatch; show UnsupportedCommandSLURL notification if unhandled")
    }

    private fun dispatchRegion(slurl: LLSLURL, navType: String, rightMouse: Boolean): Boolean {
        if (slurl.getType() != LLSLURL.SLURLType.LOCATION) return false

        TODO("APR: use JVM equivalent for LLStartUp state check (login screen → FSPanelLogin.setLocation), grid mismatch notification, and LLWorldMapMessage.sendNamedRegionRequest")
    }

    fun regionNameCallback(regionHandle: Long, slurl: LLSLURL, snapshotId: java.util.UUID, teleport: Boolean) {
        if (slurl.getType() == LLSLURL.SLURLType.LOCATION) {
            regionHandleCallback(regionHandle, slurl, snapshotId, teleport)
        }
    }

    fun regionHandleCallback(regionHandle: Long, slurl: LLSLURL, snapshotId: java.util.UUID, teleport: Boolean) {
        TODO("APR: use JVM equivalent for grid guard, from_region_handle, gAgent.teleportViaLocation or FSFloaterPlaceDetails.showPlaceDetails")
    }

    private fun handleGrid(slurl: LLSLURL): Boolean {
        TODO("APR: use JVM equivalent for LLGridManager grid comparison; show CantTeleportToGrid notification on mismatch")
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
            TODO("APR: use JVM equivalent for LLSLURL(grid, regionName, coords).getSLURLString() and TeleportViaSLAPP notification")
        } else {
            regionName = java.net.URLDecoder.decode(tokens[0], "UTF-8")
            TODO("APR: use JVM equivalent for LLSLURL construction and TeleportViaSLAPP notification")
        }
    }

    fun fromEvent(params: Map<String, Any>) {
        if (params.containsKey("regionname")) {
            val regionName = params["regionname"] as String
            val localX = (params["x"] as? Number)?.toFloat() ?: 128f
            val localY = (params["y"] as? Number)?.toFloat() ?: 128f
            val localZ = (params["z"] as? Number)?.toFloat() ?: 0f
            TODO("APR: use JVM equivalent for LLSLURL(regionName, localPos).getSLURLString() and LLWorldMapMessage.sendNamedRegionRequest")
        } else {
            if (!params.containsKey("x") || !params.containsKey("y")) {
                error("Specify either regionname or global (x, y)")
            }
            val x = (params["x"] as Number).toDouble()
            val y = (params["y"] as Number).toDouble()
            val z = (params["z"] as? Number)?.toDouble() ?: 0.0
            TODO("APR: use JVM equivalent for gAgent.teleportViaLocation(globalPos) and LLFloaterWorldMap.trackLocation")
        }
    }

    companion object {
        fun teleportViaSlapp(regionName: String, callbackUrl: String) {
            TODO("APR: use JVM equivalent for LLWorldMapMessage.sendNamedRegionRequest with regionHandleCallback and teleport=true")
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
