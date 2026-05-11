package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d
import kotlin.math.roundToInt

const val PARCEL_BAN_LINES_DRAW_SECS_ON_COLLISION: Float = 10f
const val PARCEL_COLLISION_DRAW_SECS_ON_PROXIMITY: Float = 1f

const val SOUTH_MASK: UByte = 0x01u
const val WEST_MASK: UByte  = 0x02u

abstract class ParcelObserver {
    abstract fun changed()
}

data class ParcelBuyInfo(
    val agentId: LLUUID,
    val sessionId: LLUUID,
    val groupId: LLUUID,
    val isGroupOwned: Boolean,
    val isClaim: Boolean,
    val removeContribution: Boolean,
)

typealias TeleportFinishedCallback = (pos: Vector3d, local: Boolean) -> Unit
typealias TeleportFailedCallback = () -> Unit
typealias CollisionUpdateCallback = (region: ViewerRegion?) -> Unit

object ViewerParcelMgr {
    const val PARCEL_BAN_LINES_HIDE: Int = 0
    const val PARCEL_BAN_LINES_ON_COLLISION: Int = 1
    const val PARCEL_BAN_LINES_ON_PROXIMITY: Int = 2

    private var selected: Boolean = false
    private val currentParcel: Parcel = Parcel()
    private var currentParcelSelection: ParcelSelection = ParcelSelection(currentParcel)
    private var floatingParcelSelection: ParcelSelection = ParcelSelection(currentParcel)
    private val agentParcel: Parcel = Parcel()
    private val hoverParcel: Parcel = Parcel()
    private val collisionParcel: Parcel = Parcel()

    private var requestResult: Int = 0
    private var westSouth: Vector3d = Vector3d.ZERO
    private var eastNorth: Vector3d = Vector3d.ZERO
    private var selectedDwell: Float = DWELL_NAN

    private var agentParcelSequenceId: Int = -1
    private var hoverRequestResult: Int = 0
    private var hoverWestSouth: Vector3d = Vector3d.ZERO
    private var hoverEastNorth: Vector3d = Vector3d.ZERO

    private val observers: MutableList<ParcelObserver> = mutableListOf()

    private var teleportInProgress: Boolean = true
    private var teleportWithinRegion: Boolean = false
    private var teleportInProgressPosition: Vector3d = Vector3d.ZERO

    private val teleportFinishedCallbacks: MutableList<TeleportFinishedCallback> = mutableListOf()
    private val teleportFailedCallbacks: MutableList<TeleportFailedCallback> = mutableListOf()
    private val collisionUpdateCallbacks: MutableList<CollisionUpdateCallback> = mutableListOf()

    private var parcelsPerEdge: Int = (256f / PARCEL_GRID_STEP_METERS).toInt()

    private var highlightSegments: ByteArray = ByteArray((parcelsPerEdge + 1) * (parcelsPerEdge + 1))
    private var collisionSegments: ByteArray = ByteArray((parcelsPerEdge + 1) * (parcelsPerEdge + 1))
    private var agentParcelOverlay: ByteArray = ByteArray(parcelsPerEdge * parcelsPerEdge)
    private var collisionBitmap: ByteArray = ByteArray(parcelsPerEdge * parcelsPerEdge / 8)

    private var renderCollision: Boolean = false
    private var renderSelection: Boolean = true
    private var collisionBanned: Int = 0
    private var collisionRegionHandle: ULong = 0uL
    private var collisionTimer: Long = 0L

    private var mediaParcelId: Int = 0
    private var mediaRegionId: ULong = 0uL

    fun init(regionSize: Float) {
        parcelsPerEdge = (regionSize / PARCEL_GRID_STEP_METERS).toInt()
        highlightSegments = ByteArray((parcelsPerEdge + 1) * (parcelsPerEdge + 1))
        collisionSegments = ByteArray((parcelsPerEdge + 1) * (parcelsPerEdge + 1))
        agentParcelOverlay = ByteArray(parcelsPerEdge * parcelsPerEdge)
        collisionBitmap = ByteArray(parcelsPerEdge * parcelsPerEdge / 8)
        resetSegments(highlightSegments)
        resetSegments(collisionSegments)
    }

    fun cleanupGlobals() { }

    fun selectionEmpty(): Boolean = !selected

    fun getSelectionWidth(): Float = (eastNorth.x - westSouth.x).toFloat()

    fun getSelectionHeight(): Float = (eastNorth.y - westSouth.y).toFloat()

    fun getSelection(min: Vector3d, max: Vector3d): Boolean {
        // caller receives copies via out params; here we just signal
        return !selectionEmpty()
    }

    fun getSelectionBounds(): Pair<Vector3d, Vector3d> = Pair(westSouth, eastNorth)

    fun getSelectionRegion(): ViewerRegion? { TODO("look up region from World by westSouth pos") }

    fun getDwelling(): Float = selectedDwell

    fun getDisplayInfo(): Map<String, Any> {
        var area = 0
        var price = 0
        var rent = 0
        var forSale = false
        var dwell = DWELL_NAN

        if (selected) {
            area = if (currentParcelSelection.selectedMultipleOwners)
                currentParcelSelection.getClaimableArea()
            else
                getSelectedArea()

            if (currentParcel.getForSale()) {
                price = currentParcel.getSalePrice()
                forSale = true
            } else {
                price = area * currentParcel.getClaimPricePerMeter()
            }
            rent = currentParcel.getTotalRent()
            dwell = selectedDwell
        }
        return mapOf("area" to area, "claim" to price, "rent" to rent, "forSale" to forSale, "dwell" to dwell)
    }

    fun getSelectedArea(): Int {
        if (!selected) return 0
        if (currentParcel != null && currentParcelSelection.wholeParcelSelected) {
            return currentParcel.getArea()
        }
        val width = eastNorth.x - westSouth.x
        val height = eastNorth.y - westSouth.y
        return (width * height).roundToInt()
    }

    fun resetSegments(segments: ByteArray) {
        segments.fill(0)
    }

    fun writeHighlightSegments(west: Float, south: Float, east: Float, north: Float) {
        val stride = parcelsPerEdge + 1
        val minX = (west / PARCEL_GRID_STEP_METERS).roundToInt()
        val maxX = (east / PARCEL_GRID_STEP_METERS).roundToInt()
        val minY = (south / PARCEL_GRID_STEP_METERS).roundToInt()
        val maxY = (north / PARCEL_GRID_STEP_METERS).roundToInt()

        var y = minY
        for (x in minX until maxX) {
            highlightSegments[x + y * stride] = (highlightSegments[x + y * stride].toInt() xor SOUTH_MASK.toInt()).toByte()
        }
        var x = minX
        for (yy in minY until maxY) {
            highlightSegments[x + yy * stride] = (highlightSegments[x + yy * stride].toInt() xor WEST_MASK.toInt()).toByte()
        }
        y = maxY
        for (xx in minX until maxX) {
            highlightSegments[xx + y * stride] = (highlightSegments[xx + y * stride].toInt() xor SOUTH_MASK.toInt()).toByte()
        }
        x = maxX
        for (yy in minY until maxY) {
            highlightSegments[x + yy * stride] = (highlightSegments[x + yy * stride].toInt() xor WEST_MASK.toInt()).toByte()
        }
    }

    fun writeSegmentsFromBitmap(bitmap: ByteArray, segments: ByteArray) {
        val inStride = parcelsPerEdge
        val outStride = parcelsPerEdge + 1

        for (y in 0 until inStride) {
            var x = 0
            while (x < inStride) {
                val byte = bitmap[(x + y * inStride) / 8].toInt() and 0xFF
                for (bit in 0 until 8) {
                    if (byte and (1 shl bit) != 0) {
                        val out = x + y * outStride
                        segments[out] = (segments[out].toInt() xor SOUTH_MASK.toInt()).toByte()
                        segments[out + outStride] = (segments[out + outStride].toInt() xor SOUTH_MASK.toInt()).toByte()
                        segments[out] = (segments[out].toInt() xor WEST_MASK.toInt()).toByte()
                        segments[out + 1] = (segments[out + 1].toInt() xor WEST_MASK.toInt()).toByte()
                    }
                    x++
                }
            }
        }
    }

    fun writeAgentParcelFromBitmap(bitmap: ByteArray) {
        val inStride = parcelsPerEdge
        for (y in 0 until inStride) {
            var x = 0
            while (x < inStride) {
                val byte = bitmap[(x + y * inStride) / 8].toInt() and 0xFF
                for (bit in 0 until 8) {
                    agentParcelOverlay[x + y * inStride] = if (byte and (1 shl bit) != 0) 1 else 0
                    x++
                }
            }
        }
    }

    fun selectCollisionParcel() {
        val region = getSelectionRegion() ?: return
        westSouth = region.origin
        eastNorth = Vector3d(
            westSouth.x + (region.width / 256f) * PARCEL_GRID_STEP_METERS,
            westSouth.y + (region.width / 256f) * PARCEL_GRID_STEP_METERS,
            westSouth.z
        )
        currentParcel.setName(collisionParcel.getName())
        currentParcel.setDesc(collisionParcel.getDesc())
        currentParcel.setPassPrice(collisionParcel.getPassPrice())
        currentParcel.setPassHours(collisionParcel.getPassHours())
        resetSegments(highlightSegments)
        floatingParcelSelection.setParcel(currentParcel)
        currentParcelSelection.setParcel(null)
        currentParcelSelection = ParcelSelection(currentParcel)
        selected = true
        currentParcelSelection.wholeParcelSelected = true
        notifyObservers()
        TODO("APR: use JVM equivalent - send ParcelPropertiesRequestByID message")
    }

    fun selectParcelAt(posGlobal: Vector3d): ParcelSelection {
        var southwest = Vector3d(posGlobal.x - PARCEL_GRID_STEP_METERS / 2.0,
                                 posGlobal.y - PARCEL_GRID_STEP_METERS / 2.0,
                                 posGlobal.z)
        southwest = Vector3d(
            (southwest.x / PARCEL_GRID_STEP_METERS).roundToInt() * PARCEL_GRID_STEP_METERS.toDouble(),
            (southwest.y / PARCEL_GRID_STEP_METERS).roundToInt() * PARCEL_GRID_STEP_METERS.toDouble(),
            southwest.z
        )
        var northeast = Vector3d(posGlobal.x + PARCEL_GRID_STEP_METERS / 2.0,
                                  posGlobal.y + PARCEL_GRID_STEP_METERS / 2.0,
                                  posGlobal.z)
        northeast = Vector3d(
            (northeast.x / PARCEL_GRID_STEP_METERS).roundToInt() * PARCEL_GRID_STEP_METERS.toDouble(),
            (northeast.y / PARCEL_GRID_STEP_METERS).roundToInt() * PARCEL_GRID_STEP_METERS.toDouble(),
            northeast.z
        )
        return selectLand(southwest, northeast, true)!!
    }

    fun selectParcelInRectangle(): ParcelSelection? = selectLand(westSouth, eastNorth, true)

    fun selectLand(corner1: Vector3d, corner2: Vector3d, snapToParcel: Boolean): ParcelSelection? {
        westSouth = sanitizeCornerMin(corner1, corner2)
        eastNorth = sanitizeCornerMax(corner1, corner2)

        val dx = getSelectionWidth()
        if (dx * dx <= 1f) {
            selected = false
            notifyObservers()
            return null
        }
        val dy = getSelectionHeight()
        if (dy * dy <= 1f) {
            selected = false
            notifyObservers()
            return null
        }

        floatingParcelSelection.setParcel(currentParcel)
        currentParcelSelection.setParcel(null)
        currentParcelSelection = ParcelSelection(currentParcel)
        selected = true
        currentParcelSelection.wholeParcelSelected = snapToParcel
        notifyObservers()
        TODO("APR: use JVM equivalent - send ParcelPropertiesRequest message to simulator")
    }

    private fun sanitizeCornerMin(c1: Vector3d, c2: Vector3d): Vector3d =
        Vector3d(minOf(c1.x, c2.x), minOf(c1.y, c2.y), minOf(c1.z, c2.z))

    private fun sanitizeCornerMax(c1: Vector3d, c2: Vector3d): Vector3d =
        Vector3d(maxOf(c1.x, c2.x), maxOf(c1.y, c2.y), maxOf(c1.z, c2.z))

    fun deselectLand() {
        if (!selected) return
        selected = false
        currentParcel.setLocalID(-1)
        currentParcel.accessList.clear()
        currentParcel.banList.clear()
        selectedDwell = DWELL_NAN
        currentParcelSelection.setParcel(null)
        floatingParcelSelection.setParcel(null)
        currentParcelSelection = ParcelSelection(currentParcel)
        notifyObservers()
    }

    fun deselectUnused() {
        deselectLand()
    }

    fun addObserver(observer: ParcelObserver) { observers.add(observer) }

    fun removeObserver(observer: ParcelObserver) { observers.remove(observer) }

    fun notifyObservers() {
        val snapshot = observers.toList()
        snapshot.forEach { it.changed() }
    }

    fun setSelectionVisible(visible: Boolean) { renderSelection = visible }

    fun isOwnedAt(posGlobal: Vector3d): Boolean { TODO("check parcel owner at position") }
    fun isOwnedSelfAt(posGlobal: Vector3d): Boolean { TODO("check if parcel owned by agent") }
    fun isOwnedOtherAt(posGlobal: Vector3d): Boolean { TODO("check if parcel owned by other") }
    fun isSoundLocal(posGlobal: Vector3d): Boolean { TODO("check parcel sound local flag") }
    fun canHearSound(posGlobal: Vector3d): Boolean { TODO("check if position is acoustically reachable") }

    fun getParcelSelection(): ParcelSelection = currentParcelSelection
    fun getFloatingParcelSelection(): ParcelSelection = floatingParcelSelection

    fun getAgentParcel(): Parcel = agentParcel
    fun getAgentOrSelectedParcel(): Parcel {
        val sel = floatingParcelSelection.getParcel()
        return sel ?: agentParcel
    }

    fun inAgentParcel(posGlobal: Vector3d): Boolean { TODO("check if position is within agent parcel bounds") }

    fun getHoverParcel(): Parcel? = if (hoverRequestResult != 0) hoverParcel else null

    fun getCollisionParcel(): Parcel = collisionParcel

    fun getCollisionBitmap(): ByteArray = collisionBitmap
    fun getCollisionBitmapSize(): Int = parcelsPerEdge * parcelsPerEdge / 8
    fun getCollisionRegionHandle(): ULong = collisionRegionHandle

    fun addCollisionUpdateCallback(cb: CollisionUpdateCallback): CollisionUpdateCallback {
        collisionUpdateCallbacks.add(cb); return cb
    }

    fun allowAgentBuild(): Boolean { TODO("check parcel modify permissions for agent") }
    fun allowAgentBuild(parcel: Parcel): Boolean { TODO("check parcel build permission flag") }
    fun allowAgentVoice(): Boolean { TODO("check region/parcel voice flags") }
    fun allowAgentVoice(region: ViewerRegion, parcel: Parcel): Boolean { TODO("check voice flag") }
    fun allowAgentFly(region: ViewerRegion, parcel: Parcel): Boolean { TODO("check block-fly flag") }
    fun allowAgentPush(region: ViewerRegion, parcel: Parcel): Boolean { TODO("check restrict-push flag") }
    fun allowAgentScripts(region: ViewerRegion, parcel: Parcel): Boolean { TODO("check other-scripts flag") }
    fun allowAgentDamage(region: ViewerRegion, parcel: Parcel): Boolean { TODO("check allow-damage flag") }

    fun getHoverParcelWidth(): Float = (hoverEastNorth.x - hoverWestSouth.x).toFloat()
    fun getHoverParcelHeight(): Float = (hoverEastNorth.y - hoverWestSouth.y).toFloat()

    fun render() { TODO("GPU: render parcel selection highlight") }
    fun renderParcelCollision() { TODO("GPU: render parcel collision boundary") }
    fun renderRect(westSouthBottom: Vector3d, eastNorthTop: Vector3d) { TODO("GPU: render bounding rect") }
    fun renderOneSegment(x1: Float, y1: Float, x2: Float, y2: Float, height: Float,
                         direction: UByte, region: ViewerRegion, absoluteHeight: Boolean = false) {
        TODO("GPU: render one parcel edge segment")
    }
    fun renderHighlightSegments(segments: ByteArray, region: ViewerRegion) {
        TODO("GPU: render highlight segment array")
    }
    fun renderCollisionSegments(segments: ByteArray, usePass: Boolean, region: ViewerRegion) {
        TODO("GPU: render collision segment array")
    }

    fun resetCollisionTimer() { collisionTimer = System.currentTimeMillis() }

    fun sendParcelGodForceOwner(ownerId: LLUUID) { TODO("APR: use JVM equivalent") }
    fun sendParcelGodForceToContent() { TODO("APR: use JVM equivalent") }
    fun sendParcelPropertiesUpdate(parcel: Parcel) { TODO("APR: use JVM equivalent") }
    fun sendParcelAccessListUpdate(which: UInt) { TODO("APR: use JVM equivalent") }
    fun sendParcelAccessListRequest(flags: UInt) { TODO("APR: use JVM equivalent") }
    fun sendParcelDwellRequest() { TODO("APR: use JVM equivalent") }
    fun sendParcelDeed(groupId: LLUUID) { TODO("APR: use JVM equivalent") }
    fun sendParcelRelease() { TODO("APR: use JVM equivalent") }
    fun sendParcelBuy(info: ParcelBuyInfo) { TODO("APR: use JVM equivalent") }

    fun setHoverParcel(posGlobal: Vector3d) { TODO("request hover parcel from simulator") }

    fun canAgentBuyParcel(parcel: Parcel, forGroup: Boolean): Boolean {
        TODO("check price, group membership, and parcel flags")
    }

    fun startBuyLand(isForGroup: Boolean = false) { TODO("open buy land floater") }
    fun startSellLand() { TODO("open sell land floater") }
    fun startReleaseLand() { TODO("open release land dialog") }
    fun startDivideLand() { TODO("open divide land dialog") }
    fun startJoinLand() { TODO("open join land dialog") }
    fun startDeedLandToGroup() { TODO("open deed land dialog") }
    fun reclaimParcel() { TODO("APR: use JVM equivalent") }

    fun buyPass() { TODO("APR: use JVM equivalent") }

    fun setupParcelBuy(agentId: LLUUID, sessionId: LLUUID, groupId: LLUUID,
                       isGroupOwned: Boolean, isClaim: Boolean,
                       removeContribution: Boolean): ParcelBuyInfo =
        ParcelBuyInfo(agentId, sessionId, groupId, isGroupOwned, isClaim, removeContribution)

    fun getAgentParcelName(): String = agentParcel.getName()
    fun getAgentParcelId(): Int = agentParcel.getLocalID()

    fun isCollisionBanned(): Boolean = collisionBanned != 0

    fun addTeleportFinishedCallback(cb: TeleportFinishedCallback): TeleportFinishedCallback {
        teleportFinishedCallbacks.add(cb); return cb
    }
    fun removeTeleportFinishedCallback(cb: TeleportFinishedCallback) { teleportFinishedCallbacks.remove(cb) }

    fun addTeleportFailedCallback(cb: TeleportFailedCallback): TeleportFailedCallback {
        teleportFailedCallbacks.add(cb); return cb
    }
    fun removeTeleportFailedCallback(cb: TeleportFailedCallback) { teleportFailedCallbacks.remove(cb) }

    fun onTeleportFinished(local: Boolean, newPos: Vector3d) {
        teleportInProgress = false
        teleportWithinRegion = local
        teleportInProgressPosition = newPos
        teleportFinishedCallbacks.forEach { it(newPos, local) }
    }

    fun onTeleportFailed() {
        teleportInProgress = false
        teleportFailedCallbacks.forEach { it() }
    }

    fun getTeleportInProgress(): Boolean = teleportInProgress

    fun postTeleportFinished(local: Boolean) {
        teleportInProgress = true
        teleportWithinRegion = local
    }

    fun isParcelOwnedByAgent(parcel: Parcel, groupProxyPower: ULong): Boolean {
        TODO("check parcel ownership against agent groups and proxy powers")
    }

    fun isParcelModifiableByAgent(parcel: Parcel, groupProxyPower: ULong): Boolean {
        TODO("check parcel modify permission against agent groups")
    }

    fun dump() {
        println("ViewerParcelMgr: selected=$selected westSouth=$westSouth eastNorth=$eastNorth")
        println("  currentParcel: ${currentParcel.getName()}")
        println("  banning ${currentParcel.banList.size} entries")
        println("  agentParcel: ${agentParcel.getName()}")
        println("  hoverParcel: ${hoverParcel.getName()}")
    }
}

fun sanitizeCorners(corner1: Vector3d, corner2: Vector3d): Pair<Vector3d, Vector3d> {
    val westSouth = Vector3d(minOf(corner1.x, corner2.x), minOf(corner1.y, corner2.y), minOf(corner1.z, corner2.z))
    val eastNorth = Vector3d(maxOf(corner1.x, corner2.x), maxOf(corner1.y, corner2.y), maxOf(corner1.z, corner2.z))
    return Pair(westSouth, eastNorth)
}
