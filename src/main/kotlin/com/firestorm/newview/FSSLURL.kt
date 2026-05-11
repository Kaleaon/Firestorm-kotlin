package com.firestorm.newview

import java.util.UUID

data class Vector3(val x: Float, val y: Float, val z: Float) {
    companion object {
        fun fromArray(arr: List<Any>): Vector3 {
            val x = arr.getOrNull(0)?.toString()?.toFloatOrNull() ?: 0f
            val y = arr.getOrNull(1)?.toString()?.toFloatOrNull() ?: 0f
            val z = arr.getOrNull(2)?.toString()?.toFloatOrNull() ?: 0f
            return Vector3(x, y, z)
        }
    }
}

data class Vector3d(val x: Double, val y: Double, val z: Double)

class LLSLURL {

    enum class SlurlType {
        INVALID, LOCATION, HOME_LOCATION, LAST_LOCATION, APP, HELP, EMPTY
    }

    var type: SlurlType = SlurlType.INVALID
        private set

    var appCmd: String = ""
        private set
    var appPath: List<Any> = emptyList()
        private set
    var appQueryMap: Map<String, String> = emptyMap()
        private set
    var appQuery: String = ""
        private set

    var grid: String = ""
        private set
    var region: String = ""
        private set
    var position: Vector3 = Vector3(0f, 0f, 0f)
        private set
    var hypergrid: Boolean = false
        private set

    constructor() {
        type = SlurlType.INVALID
    }

    constructor(slurl: String) {
        hypergrid = false
        type = SlurlType.INVALID
        parseSlurl(slurl)
    }

    constructor(grid: String, region: String, isHypergrid: Boolean = false) {
        hypergrid = isHypergrid
        this.grid = grid
        this.region = region
        type = SlurlType.LOCATION
        position = Vector3(REGION_WIDTH_METERS / 2f, REGION_WIDTH_METERS / 2f, 0f)
    }

    constructor(region: String, pos: Vector3, isHypergrid: Boolean = false) {
        hypergrid = isHypergrid
        this.region = region
        this.position = pos
        this.grid = TODO("APR: use JVM equivalent — LLGridManager.getInstance().getGrid()")
        type = SlurlType.LOCATION
    }

    constructor(grid: String, region: String, pos: Vector3, isHypergrid: Boolean = false) {
        hypergrid = isHypergrid
        this.grid = grid
        this.region = region
        type = SlurlType.LOCATION
        position = if (!isInOpenSim()) {
            val x = Math.round(pos.x % REGION_WIDTH_METERS).toFloat()
            val y = Math.round(pos.y % REGION_WIDTH_METERS).toFloat()
            val z = Math.round(pos.z).toFloat()
            Vector3(x, y, z)
        } else {
            pos
        }
    }

    constructor(grid: String, region: String, regionOrigin: Vector3d, globalPosition: Vector3d, isHypergrid: Boolean = false) {
        hypergrid = isHypergrid
        val local = Vector3(
            (globalPosition.x - regionOrigin.x).toFloat(),
            (globalPosition.y - regionOrigin.y).toFloat(),
            (globalPosition.z - regionOrigin.z).toFloat(),
        )
        val src = LLSLURL(grid, region, local, isHypergrid)
        copyFrom(src)
    }

    constructor(region: String, regionOrigin: Vector3d, globalPosition: Vector3d, isHypergrid: Boolean = false) {
        hypergrid = isHypergrid
        val g: String = TODO("APR: use JVM equivalent — LLGridManager.getInstance().getGrid()")
        val src = LLSLURL(g, region, regionOrigin, globalPosition, isHypergrid)
        copyFrom(src)
    }

    constructor(command: String, id: UUID, verb: String) {
        hypergrid = false
        type = SlurlType.APP
        appCmd = command
        appPath = listOf(id.toString(), verb)
    }

    constructor(pathArray: List<String>, fromApp: Boolean) {
        hypergrid = false
        if (pathArray.isNotEmpty()) {
            val query = "hop://" + pathArray.joinToString("/") + "/"
            val result = LLSLURL(query)
            if (result.type == SlurlType.APP && fromApp) {
                type = SlurlType.INVALID
            } else {
                copyFrom(result)
            }
        } else {
            type = SlurlType.INVALID
        }
    }

    val isValid: Boolean get() = type != SlurlType.INVALID
    val isSpatial: Boolean get() = type == SlurlType.LAST_LOCATION || type == SlurlType.HOME_LOCATION || type == SlurlType.LOCATION

    fun getSLURLString(): String {
        return when (type) {
            SlurlType.HOME_LOCATION -> SIM_LOCATION_HOME
            SlurlType.LAST_LOCATION -> SIM_LOCATION_LAST
            SlurlType.LOCATION -> {
                val x = Math.round(position.x)
                val y = Math.round(position.y)
                val z = Math.round(position.z)
                val base: String = TODO("APR: use JVM equivalent — LLGridManager.getInstance().getSLURLBase(grid)")
                "$base${uriEscape(region)}/$x/$y/$z"
            }
            SlurlType.APP -> {
                val sb = StringBuilder()
                val appBase: String = TODO("APR: use JVM equivalent — LLGridManager.getInstance().getAppSLURLBase()")
                sb.append(appBase).append("/").append(appCmd)
                for (part in appPath) sb.append("/").append(part)
                if (appQuery.isNotEmpty()) sb.append("?").append(appQuery)
                sb.toString()
            }
            else -> ""
        }
    }

    fun getLoginString(): String {
        return when (type) {
            SlurlType.LOCATION -> {
                val raw = "uri:$region&${Math.round(position.x)}&${Math.round(position.y)}&${Math.round(position.z)}"
                xmlEncode(raw)
            }
            SlurlType.HOME_LOCATION -> "home"
            SlurlType.LAST_LOCATION -> "last"
            else -> ""
        }
    }

    fun getLocationString(): String =
        "$region/${Math.round(position.x)}/${Math.round(position.y)}/${Math.round(position.z)}"

    fun asString(): String =
        "mAppCmd:$appCmd mAppPath:$appPath mAppQueryMap:$appQueryMap mAppQuery:$appQuery " +
        "mGrid:$grid mRegion:$region mPosition:$position mType:$type mHypergrid:$hypergrid"

    override fun equals(other: Any?): Boolean {
        if (other !is LLSLURL) return false
        if (other.type != type) return false
        return when (type) {
            SlurlType.LOCATION -> grid == other.grid && region == other.region && position == other.position
            SlurlType.APP -> getSLURLString() == other.getSLURLString()
            SlurlType.HOME_LOCATION, SlurlType.LAST_LOCATION -> true
            else -> false
        }
    }

    override fun hashCode(): Int = when (type) {
        SlurlType.LOCATION -> listOf(grid, region, position).hashCode()
        SlurlType.APP -> getSLURLString().hashCode()
        else -> type.hashCode()
    }

    private fun copyFrom(src: LLSLURL) {
        type = src.type
        appCmd = src.appCmd
        appPath = src.appPath
        appQueryMap = src.appQueryMap
        appQuery = src.appQuery
        grid = src.grid
        region = src.region
        position = src.position
        hypergrid = src.hypergrid
    }

    private fun parseSlurl(slurl: String) {
        if (slurl.isEmpty() || slurl == SIM_LOCATION_LAST) { type = SlurlType.LAST_LOCATION; return }
        if (slurl == SIM_LOCATION_HOME) { type = SlurlType.HOME_LOCATION; return }
        if (slurl.startsWith("mailto:")) return

        TODO("APR: use JVM equivalent — full SLURL parse using URI/LLURI logic (scheme detection, path parsing, grid probing via LLGridManager)")
    }

    companion object {
        const val HOP_SCHEME = "hop"
        const val SLURL_HTTP_SCHEME = "http"
        const val SLURL_HTTPS_SCHEME = "https"
        const val SLURL_SL_SCHEME = "sl"
        const val SLURL_SECONDLIFE_SCHEME = "secondlife"
        const val SLURL_SECONDLIFE_PATH = "secondlife"
        const val SLURL_COM = "slurl.com"
        const val WWW_SLURL_COM = "www.slurl.com"
        const val SECONDLIFE_COM = "secondlife.com"
        const val MAPS_SECONDLIFE_COM = "maps.secondlife.com"
        const val SLURL_X_GRID_LOCATION_INFO_SCHEME = "x-grid-location-info"
        const val SIM_LOCATION_HOME = "home"
        const val SIM_LOCATION_LAST = "last"
        const val SLURL_APP_PATH = "app"
        const val SLURL_REGION_PATH = "region"

        const val REGION_WIDTH_METERS = 256f

        val START_LOCATION = LLSLURL()

        fun getTypeHumanReadable(type: SlurlType): String = when (type) {
            SlurlType.INVALID -> "INVALID"
            SlurlType.LOCATION -> "LOCATION"
            SlurlType.HOME_LOCATION -> "HOME_LOCATION"
            SlurlType.LAST_LOCATION -> "LAST_LOCATION"
            SlurlType.APP -> "APP"
            SlurlType.HELP -> "HELP"
            SlurlType.EMPTY -> "EMPTY"
        }

        private fun uriEscape(s: String): String =
            java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

        private fun xmlEncode(s: String): String =
            s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

        private fun isInOpenSim(): Boolean =
            TODO("APR: use JVM equivalent — LLGridManager.getInstance().isInOpenSim()")
    }

    fun getTypeHumanReadable(): String = Companion.getTypeHumanReadable(type)
}
