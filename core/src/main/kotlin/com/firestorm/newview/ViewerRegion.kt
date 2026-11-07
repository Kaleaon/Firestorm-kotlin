package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d
import com.firestorm.llmessage.Host
import kotlin.math.roundToInt

const val MAX_OBJECT_CACHE_ENTRIES: UInt = 50000u
const val REGION_HANDSHAKE_SUPPORTS_SELF_APPEARANCE: ULong = 1uL shl 2
const val IL_MODE_DEFAULT: String = "default"
const val IL_MODE_360: String = "360"

enum class ObjectPartition {
    HUD,
    TERRAIN,
    VOIDWATER,
    WATER,
    TREE,
    PARTICLE,
    GRASS,
    VOLUME,
    BRIDGE,
    AVATAR,
    CONTROL_AV,
    HUD_PARTICLE,
    VO_CACHE,
    NONE;
}

enum class CacheMissType { TOTAL, CRC, NONE }
enum class CacheUpdateResult { DUPE, CHANGED, ADDED, REPLACED }
enum class CapabilitiesState { INIT, ERROR, RECEIVED }

typealias CapsReceivedCallback = (regionId: LLUUID, region: ViewerRegion) -> Unit

class ViewerRegion(
    val handle: ULong,
    val host: Host,
    surfaceGridWidth: UInt,
    patchGridWidth: UInt,
    val width: Float,
) {
    var regionId: LLUUID = LLUUID.NULL
    var name: String = ""
    var zoning: String = ""
    var origin: Vector3d = Vector3d.ZERO
    var waterHeight: Float = 0f
    var timeDilation: Float = 1f
    var billableFactor: Float = 1f
    var maxTasks: UInt = 0u
    var simAccess: UByte = 0u
    var regionFlags: ULong = 0uL
    var regionProtocols: ULong = 0uL
    var centralBakeVersion: UByte = 0u
    var cacheLoaded: Boolean = false
    var cacheDirty: Boolean = false
    var alive: Boolean = true
    var simulatorFeaturesReceived: Boolean = false
    var releaseNotesRequested: Boolean = false
    var dead: Boolean = false
    var paused: Boolean = false
    var isEstateManager: Boolean = false
    var lastUpdate: Int = 0

    var widthScaleFactor: Float = 1f
    var minSimHeight: Float = 0f
    var maxBakes: Int = 8
    var maxTEs: Int = 8

    var classID: Int = 0
    var cpuRatio: Int = 1
    var coloName: String = ""
    var productSKU: String = ""
    var productName: String = ""
    var httpUrl: String = ""
    var viewerAssetUrl: String = ""

    var interestListMode: String = IL_MODE_DEFAULT

    var packetsIn: UInt = 0u
    var packetsOut: UInt = 0u
    var packetsLost: Int = 0
    var pingDelay: UInt = 0u

    var cameraDistanceSquared: Float = 0f
    var regionCacheHitCount: ULong = 0uL
    var regionCacheMissCount: ULong = 0uL

    val mapAvatars: MutableList<UInt> = mutableListOf()
    val mapAvatarIDs: MutableList<LLUUID> = mutableListOf()

    private val capabilities: MutableMap<String, String> = mutableMapOf()
    private val capabilitiesDebug: MutableMap<String, String> = mutableMapOf()
    private var capabilitiesState: CapabilitiesState = CapabilitiesState.INIT

    private val capsReceivedCallbacks: MutableList<CapsReceivedCallback> = mutableListOf()
    private val simFeaturesReceivedCallbacks: MutableList<CapsReceivedCallback> = mutableListOf()

    private val orphanMap: MutableMap<UInt, MutableList<UInt>> = mutableMapOf()
    private val cacheMissList: MutableList<Pair<UInt, CacheMissType>> = mutableListOf()

    companion object {
        var voCacheCullingEnabled: Boolean = true
        var lastCameraUpdated: Int = 0
        var newObjectCreationThrottle: Int = -1

        fun isNewObjectCreationThrottleDisabled(): Boolean = newObjectCreationThrottle < 0

        fun regionFlagsToString(flags: ULong): String = "flags=0x${flags.toString(16)}"

        fun accessToString(simAccess: UByte): String = when (simAccess.toInt()) {
            13 -> "PG"
            21 -> "Mature"
            42 -> "Adult"
            else -> "Unknown"
        }

        fun accessToShortString(simAccess: UByte): String = when (simAccess.toInt()) {
            13 -> "PG"
            21 -> "M"
            42 -> "A"
            else -> "?"
        }

        fun shortStringToAccess(s: String): UByte = when (s.uppercase()) {
            "PG" -> 13u
            "M"  -> 21u
            "A"  -> 42u
            else -> 0u
        }

        fun getAccessIcon(simAccess: UByte): String = when (simAccess.toInt()) {
            13 -> "Parcel_PG_Light"
            21 -> "Parcel_M_Light"
            42 -> "Parcel_R_Light"
            else -> ""
        }

        fun isViewerCameraStatic(): Boolean {
            System.err.println("query camera motion state")
            return false
        }
        fun calcNewObjectCreationThrottle() {
            System.err.println("recalculate based on bandwidth")
        }
        fun idleCleanup(maxUpdateTime: Float) {
            System.err.println("APR: use JVM equivalent")
        }

        val regionCacheCleanup: MutableMap<UInt, Any> = mutableMapOf()
    }

    fun loadObjectCache() {
        System.err.println("APR: use JVM equivalent")
    }
    fun saveObjectCache() {
        System.err.println("APR: use JVM equivalent")
    }

    fun setOriginGlobal(o: Vector3d) { origin = o }

    fun getOriginGlobal(): Vector3d = origin

    fun getOriginAgent(): Vector3 =
        Vector3(origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat())

    fun getCenterGlobal(): Vector3d =
        Vector3d(origin.x + width / 2.0, origin.y + width / 2.0, origin.z + waterHeight)

    fun getCenterAgent(): Vector3 {
        val c = getCenterGlobal()
        return Vector3(c.x.toFloat(), c.y.toFloat(), c.z.toFloat())
    }

    fun localToGlobal(local: Vector3): Vector3d =
        Vector3d(origin.x + local.x, origin.y + local.y, origin.z + local.z)

    fun globalToLocal(global: Vector3d): Vector3 =
        Vector3(
            (global.x - origin.x).toFloat(),
            (global.y - origin.y).toFloat(),
            (global.z - origin.z).toFloat()
        )

    fun getPosRegionFromGlobal(global: Vector3d): Vector3 = globalToLocal(global)

    fun getPosRegionFromAgent(agentPos: Vector3): Vector3 {
        System.err.println("APR: use JVM equivalent - subtract agent region origin")
        return Vector3.ZERO
    }

    fun getPosAgentFromRegion(regionPos: Vector3): Vector3 {
        System.err.println("APR: use JVM equivalent - add agent region origin")
        return Vector3.ZERO
    }

    fun getPosGlobalFromRegion(offset: Vector3): Vector3d = localToGlobal(offset)

    fun pointInRegionGlobal(pointGlobal: Vector3d): Boolean {
        val local = globalToLocal(pointGlobal)
        return local.x >= 0f && local.x < width &&
               local.y >= 0f && local.y < width
    }

    fun setRegionNameAndZone(nameAndZone: String) {
        val parts = nameAndZone.split(" - ", limit = 2)
        name = parts.getOrElse(0) { "" }
        zoning = parts.getOrElse(1) { "" }
    }

    fun setOwner(ownerId: LLUUID) { regionId = ownerId }
    fun getOwner(): LLUUID = regionId

    fun getSimAccessString(): String = accessToString(simAccess)

    fun getLocalizedSimProductName(): String = productName.ifEmpty { productSKU }

    fun setWaterHeight(level: Float) { waterHeight = level }
    fun getWaterHeight(): Float = waterHeight
    fun rebuildWater() {
        System.err.println("GPU: rebuild water surface mesh")
    }

    fun isVoiceEnabled(): Boolean = (regionFlags and 0x0000000020000000uL) != 0uL

    fun setTimeDilation(td: Float) { timeDilation = td }
    fun getTimeDilation(): Float = timeDilation

    fun setRegionFlag(flag: ULong, on: Boolean) {
        regionFlags = if (on) regionFlags or flag else regionFlags and flag.inv()
    }

    fun getRegionFlag(flag: ULong): Boolean = (regionFlags and flag) != 0uL

    fun setRegionFlags(f: ULong) { regionFlags = f }

    fun setRegionProtocol(protocol: ULong, on: Boolean) {
        regionProtocols = if (on) regionProtocols or protocol else regionProtocols and protocol.inv()
    }

    fun getRegionProtocol(protocol: ULong): Boolean = (regionProtocols and protocol) != 0uL

    fun setRegionProtocols(p: ULong) { regionProtocols = p }

    fun getAllowDamage(): Boolean = getRegionFlag(0x0000000000000001uL)
    fun getAllowLandmark(): Boolean = getRegionFlag(0x0000000000000002uL)
    fun getAllowSetHome(): Boolean = getRegionFlag(0x0000000000000004uL)
    fun getResetHomeOnTeleport(): Boolean = getRegionFlag(0x0000000000000008uL)
    fun getSunFixed(): Boolean = getRegionFlag(0x0000000000000010uL)
    fun getBlockFly(): Boolean = getRegionFlag(0x0000000000000020uL)
    fun getAllowDirectTeleport(): Boolean = getRegionFlag(0x0000000000000040uL)
    fun getAllowTerraform(): Boolean = !getRegionFlag(0x0000000000000400uL)
    fun getRestrictPushObject(): Boolean = getRegionFlag(0x0000000000000800uL)
    fun getAllowEnvironmentOverride(): Boolean = getRegionFlag(0x0000000002000000uL)
    fun getReleaseNotesRequested(): Boolean = releaseNotesRequested

    fun isAlive(): Boolean = alive

    fun setAllowDamage(b: Boolean) = setRegionFlag(0x0000000000000001uL, b)
    fun setAllowLandmark(b: Boolean) = setRegionFlag(0x0000000000000002uL, b)
    fun setAllowSetHome(b: Boolean) = setRegionFlag(0x0000000000000004uL, b)
    fun setResetHomeOnTeleport(b: Boolean) = setRegionFlag(0x0000000000000008uL, b)
    fun setSunFixed(b: Boolean) = setRegionFlag(0x0000000000000010uL, b)
    fun setAllowDirectTeleport(b: Boolean) = setRegionFlag(0x0000000000000040uL, b)

    fun canManageEstate(): Boolean = isEstateManager

    fun isCapabilityAvailable(name: String): Boolean = name in capabilities

    fun getCapability(name: String): String = capabilities[name] ?: ""

    fun getCapabilityDebug(name: String): String = capabilitiesDebug[name] ?: ""

    fun setCapability(name: String, url: String) { capabilities[name] = url }

    fun setCapabilityDebug(name: String, url: String) { capabilitiesDebug[name] = url }

    fun setSeedCapability(url: String) { capabilities["Seed"] = url }

    fun getNumSeedCapRetries(): Int = 0

    fun capabilitiesReceived(): Boolean = capabilitiesState == CapabilitiesState.RECEIVED

    fun capabilitiesError(): Boolean = capabilitiesState == CapabilitiesState.ERROR

    fun setCapabilitiesReceived(received: Boolean) {
        capabilitiesState = if (received) CapabilitiesState.RECEIVED else CapabilitiesState.INIT
        if (received) capsReceivedCallbacks.forEach { it(regionId, this) }
    }

    fun setCapabilitiesError() { capabilitiesState = CapabilitiesState.ERROR }

    fun addCapsReceivedCallback(cb: CapsReceivedCallback): CapsReceivedCallback {
        capsReceivedCallbacks.add(cb); return cb
    }

    fun setSimulatorFeaturesReceived(received: Boolean) {
        simulatorFeaturesReceived = received
        if (received) simFeaturesReceivedCallbacks.forEach { it(regionId, this) }
    }

    fun addSimFeaturesReceivedCallback(cb: CapsReceivedCallback): CapsReceivedCallback {
        simFeaturesReceivedCallbacks.add(cb); return cb
    }

    fun getSimulatorFeatures(): Map<String, Any> = emptyMap()

    fun setSimulatorFeatures(info: Map<String, Any>) {
        System.err.println("store simulator features LLSD")
    }

    fun requestSimulatorFeatures() {
        System.err.println("APR: use JVM equivalent - HTTP GET SimulatorFeatures cap")
    }

    fun meshUploadEnabled(): Boolean = isCapabilityAvailable("MeshUploadFlag")

    fun bakesOnMeshEnabled(): Boolean = isCapabilityAvailable("BakesOnMeshEnabled")

    fun dynamicPathfindingEnabled(): Boolean = isCapabilityAvailable("NavMeshGenerationStatus")

    fun avatarHoverHeightEnabled(): Boolean = isCapabilityAvailable("AgentPreferences")

    fun getLandHeightRegion(regionPos: Vector3): Float {
        System.err.println("query terrain surface height")
        return 0f
    }

    fun getCompositionXY(x: Int, y: Int): Float {
        System.err.println("query terrain composition texture")
        return 0f
    }

    fun isOwnedSelf(pos: Vector3): Boolean {
        System.err.println("check parcel ownership against agent")
        return false
    }

    fun isOwnedGroup(pos: Vector3): Boolean {
        System.err.println("check parcel group ownership")
        return false
    }

    fun updateCoarseLocations() {
        System.err.println("APR: use JVM equivalent")
    }

    fun dirtyHeights() {
        System.err.println("GPU: mark height patches dirty")
    }

    fun dirtyAllPatches() {
        System.err.println("GPU: mark all terrain patches dirty")
    }

    fun renderPropertyLines() {
        System.err.println("GPU: render parcel ownership lines")
    }

    fun renderPropertyLinesOnMinimap(scalePixelsPerMeter: Float, parcelOutlineColor: FloatArray) {
        System.err.println("GPU: render property lines on minimap")
    }

    fun updateRenderMatrix() {
        System.err.println("GPU: recompute region render matrix")
    }

    fun idleUpdate(maxUpdateTime: Float) {
        System.err.println("step region idle update")
    }

    fun lightIdleUpdate() {
        System.err.println("step region light idle update")
    }

    fun forceUpdate() {
        System.err.println("force-complete all pending region updates")
    }

    fun connectNeighbor(neighbor: ViewerRegion, direction: UInt) {
        System.err.println("APR: use JVM equivalent")
    }

    fun updateNetStats() {
        System.err.println("APR: use JVM equivalent")
    }

    fun getPacketsLost(): UInt = packetsLost.toUInt()

    fun sendMessage() {
        System.err.println("APR: use JVM equivalent")
    }

    fun sendReliableMessage() {
        System.err.println("APR: use JVM equivalent")
    }

    fun requestPostCapability(capName: String, postData: Map<String, Any>,
                              onSuccess: (() -> Unit)? = null,
                              onFailure: (() -> Unit)? = null): Boolean {
        if (!isCapabilityAvailable(capName)) return false
        System.err.println("APR: use JVM equivalent - coroutine HTTP POST to cap URL")
        return false
    }

    fun requestGetCapability(capName: String, onSuccess: (() -> Unit)? = null,
                             onFailure: (() -> Unit)? = null): Boolean {
        if (!isCapabilityAvailable(capName)) return false
        System.err.println("APR: use JVM equivalent - coroutine HTTP GET to cap URL")
        return false
    }

    fun setCacheID(id: LLUUID) {
        System.err.println("store VO cache id")
    }

    fun probeCache(localId: UInt, crc: UInt, flags: UInt): CacheMissType {
        System.err.println("APR: use JVM equivalent - check VO cache")
        return CacheMissType.NONE
    }

    fun getCacheEntry(localId: UInt, valid: Boolean = true): Any? {
        System.err.println("APR: use JVM equivalent")
        return null
    }

    fun requestCacheMisses() {
        System.err.println("APR: use JVM equivalent")
    }

    fun addCacheMissFull(localId: UInt) { cacheMissList.add(Pair(localId, CacheMissType.TOTAL)) }

    fun clearCachedVisibleObjects() {
        System.err.println("clear VO cache visible objects")
    }

    fun killCacheEntry(localId: UInt) {
        System.err.println("remove VO cache entry")
    }

    fun dumpCache() { println("ViewerRegion cache: hits=$regionCacheHitCount misses=$regionCacheMissCount") }

    fun clearVOCacheFromMemory() {
        System.err.println("release VO cache memory")
    }

    fun unpackRegionHandshake() {
        System.err.println("APR: use JVM equivalent")
    }

    fun calculateCenterGlobal() { /* center is derived from origin + width/2 */ }

    fun calculateCameraDistance() {
        System.err.println("query camera position and compute distance")
    }

    fun findOrphans(parentId: UInt) { orphanMap.remove(parentId) }

    fun objectIsReturnable(pos: Vector3, boxes: List<Any>): Boolean {
        System.err.println("check parcel return policy")
        return false
    }

    fun getSimHostName(): String {
        System.err.println("APR: resolve host DNS name")
        return ""
    }

    fun setSeedCapabilityAndRequest(url: String) { setSeedCapability(url); requestSimulatorFeatures() }

    fun setInterestListMode(mode: String) { interestListMode = mode }

    fun resetInterestList() {
        System.err.println("APR: use JVM equivalent")
    }

    fun materialsCapThrottled(): Boolean = false

    fun resetMaterialsCapThrottle() {
        System.err.println("reset throttle timer")
    }

    fun getMaxMaterialsPerTransaction(): UInt = 50u

    fun removeFromCreatedList(localId: UInt) {
        System.err.println("remove from non-cacheable object set")
    }

    fun addToCreatedList(localId: UInt) {
        System.err.println("add to non-cacheable object set")
    }

    fun updateReflectionProbes(fullUpdate: Boolean) {
        System.err.println("GPU: rebuild reflection probe list")
    }

    fun showReleaseNotes() { releaseNotesRequested = true }

    fun logActiveCapabilities() {
        println("Region $name capabilities: ${capabilities.keys.joinToString()}")
    }

    fun getDescription(): String = "ViewerRegion($name, handle=$handle)"

    fun getInfo(): Map<String, Any> = mapOf(
        "Name" to name,
        "Handle" to handle,
        "SimAccess" to simAccess,
        "RegionFlags" to regionFlags,
    )

    override fun toString(): String = "ViewerRegion(name=$name, handle=$handle, host=$host)"
}
