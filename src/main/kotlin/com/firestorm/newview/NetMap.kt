package com.firestorm.newview

import kotlin.math.*

private const val MAP_SCALE_ZOOM_FACTOR = 1.04f
private const val MIN_DOT_RADIUS = 3.5f
private const val DOT_SCALE = 0.75f
private const val MIN_PICK_SCALE = 2f
private const val MOUSE_DRAG_SLOP = 2
private const val COARSEUPDATE_MAX_Z = 1020.0
private const val WIDTH_PIXELS = 2f
private const val CIRCLE_STEPS = 100

class NetMap(bgColor: Color4 = Color4(0f, 0f, 0f, 1f)) {

    companion object {
        const val MAP_SCALE_MIN: Float        = 32f
        const val MAP_SCALE_FAR: Float        = 32f
        const val MAP_SCALE_MEDIUM: Float     = 128f
        const val MAP_SCALE_CLOSE: Float      = 256f
        const val MAP_SCALE_VERY_CLOSE: Float = 1024f
        const val MAP_SCALE_MAX: Float        = 4096f

        var sScale: Float = MAP_SCALE_MEDIUM

        val sAvatarMarksMap: MutableMap<String, Color4> = mutableMapOf()

        fun hasAvatarMarkColor(avatarId: String): Boolean = sAvatarMarksMap.containsKey(avatarId)

        fun getAvatarMarkColor(avatarId: String): Color4? = sAvatarMarksMap[avatarId]

        fun setAvatarMarkColor(avatarId: String, color: Color4) {
            sAvatarMarksMap[avatarId] = color
        }

        fun setAvatarMarkColors(avatarIds: List<String>, color: Color4) {
            avatarIds.forEach { sAvatarMarksMap[it] = color }
        }

        fun clearAvatarMarkColor(avatarId: String) {
            sAvatarMarksMap.remove(avatarId)
        }

        fun clearAvatarMarkColors(avatarIds: List<String>) {
            avatarIds.forEach { sAvatarMarksMap.remove(it) }
        }

        fun clearAvatarMarkColors() {
            sAvatarMarksMap.clear()
        }

        fun getAvatarColor(avatarId: String): Color4 =
            sAvatarMarksMap[avatarId] ?: Color4.white

        private fun outsideSlop(x: Int, y: Int, startX: Int, startY: Int, slop: Int): Boolean =
            abs(x - startX) > slop || abs(y - startY) > slop

        fun showAvatarInspector(avatarId: String) {
            TODO("APR: open avatar inspector panel for $avatarId")
        }
    }

    private val backgroundColor: Color4 = bgColor

    private var scale: Float = MAP_SCALE_MEDIUM
    private var pixelsPerMeter: Float = MAP_SCALE_MEDIUM / 256f
    private var objectMapTpm: Float = 0f
    private var objectMapPixels: Float = 0f
    private var dotRadius: Float = max(DOT_SCALE * pixelsPerMeter, MIN_DOT_RADIUS)

    private var panning = false
    private var centering = false
    private var curPan   = Vector2f(0f, 0f)
    private var startPan = Vector2f(0f, 0f)
    private var popupWorldPos = Vector3d(0.0, 0.0, 0.0)
    private var mouseDown = Pair(0, 0)

    private var objectImageCenterGlobal = Vector3d(0.0, 0.0, 0.0)
    private var parcelImageCenterGlobal = Vector3d(0.0, 0.0, 0.0)

    private var updateObjectImage = false
    private var updateParcelImage = false

    var closestAgentToCursor: String = ""
    var closestAgentPosition: Vector3d = Vector3d(0.0, 0.0, 0.0)

    var closestAgentsToCursor: MutableList<String> = mutableListOf()
    var closestAgentRightClick: String = ""
    var closestAgentsRightClick: MutableList<String> = mutableListOf()

    private var toolTipMsg       = ""
    private var parcelNameMsg    = ""
    private var parcelSalePriceMsg = ""
    private var parcelSaleAreaMsg  = ""
    private var parcelOwnerMsg   = ""
    private var regionNameMsg    = ""
    private var toolTipHintMsg   = ""
    private var altToolTipHintMsg = ""

    private var selected: MutableList<String> = mutableListOf()

    private val parcelMgrListeners: MutableList<() -> Unit> = mutableListOf()
    private val parcelOverlayListeners: MutableList<() -> Unit> = mutableListOf()

    private var rectWidth  = 0
    private var rectHeight = 0

    fun postBuild(): Boolean {
        TODO("APR: register context-menu callbacks and parcel-manager signal connections")
    }

    fun setScale(scale: Float) {
        val clamped = scale.coerceIn(MAP_SCALE_MIN, MAP_SCALE_MAX)
        curPan = Vector2f(curPan.x * clamped / this.scale, curPan.y * clamped / this.scale)
        this.scale = clamped
        sScale = clamped
        pixelsPerMeter = clamped / 256f
        dotRadius = max(DOT_SCALE * pixelsPerMeter, MIN_DOT_RADIUS)
        updateObjectImage = true
        updateParcelImage = true
    }

    fun setToolTipMsg(msg: String)         { toolTipMsg = msg }
    fun setParcelNameMsg(msg: String)      { parcelNameMsg = msg }
    fun setParcelSalePriceMsg(msg: String) { parcelSalePriceMsg = msg }
    fun setParcelSaleAreaMsg(msg: String)  { parcelSaleAreaMsg = msg }
    fun setParcelOwnerMsg(msg: String)     { parcelOwnerMsg = msg }
    fun setRegionNameMsg(msg: String)      { regionNameMsg = msg }
    fun setToolTipHintMsg(msg: String)     { toolTipHintMsg = msg }
    fun setAltToolTipHintMsg(msg: String)  { altToolTipHintMsg = msg }

    fun setSelected(uuids: List<String>) { selected = uuids.toMutableList() }

    fun refreshParcelOverlay() { updateParcelImage = true }

    fun renderScaledPointGlobal(pos: Vector3d, color: Color4, radius: Float) {
        TODO("GPU: render scaled point at global position $pos radius=$radius")
    }

    fun viewPosToGlobal(x: Int, y: Int): Vector3d {
        TODO("GPU: invert mini-map projection to get global world position")
    }

    fun getClosestAgentToCursor(): String = closestAgentToCursor
    fun getClosestAgentPosition(): Vector3d = closestAgentPosition

    fun draw() {
        TODO("GPU: full minimap draw: regions, objects, parcels, agents, tracking, rings")
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        val newScale = (scale * MAP_SCALE_ZOOM_FACTOR.pow(-clicks.toFloat()))
            .coerceIn(MAP_SCALE_MIN, MAP_SCALE_MAX)
        setScale(newScale)
        return true
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        mouseDown = Pair(x, y)
        startPan = curPan
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        panning = false
        return true
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (panning || outsideSlop(x, y, mouseDown.first, mouseDown.second, MOUSE_DRAG_SLOP)) {
            if (!panning) panning = true
            val dx = (x - mouseDown.first).toFloat()
            val dy = (y - mouseDown.second).toFloat()
            curPan = Vector2f(startPan.x + dx, startPan.y + dy)
        }
        TODO("GPU: update cursor and closest-agent-to-cursor")
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: show parcel/region tooltip for minimap position")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        rectWidth  = width
        rectHeight = height
        updateObjectImage = true
        updateParcelImage = true
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        popupWorldPos = viewPosToGlobal(x, y)
        TODO("APR: populate and show context menu at ($x, $y)")
    }

    fun handleClick(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: handle single click on minimap (start tracking, etc.)")
    }

    fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        TODO("APR: perform double-click action (teleport to position)")
    }

    fun performDoubleClickAction(posGlobal: Vector3d) {
        TODO("APR: teleport or show destination for $posGlobal")
    }

    fun handleShowProfile(sdParam: Any) {
        TODO("APR: open profile floater for agent in sdParam")
    }

    private fun globalPosToView(globalPos: Vector3d): Vector3 {
        TODO("GPU: project global position onto minimap view coordinates")
    }

    private fun drawTracking(posGlobal: Vector3d, color: Color4, drawArrow: Boolean = true) {
        TODO("GPU: draw tracking indicator on minimap")
    }

    private fun drawRing(radius: Float, posMap: Vector3, color: Color4) {
        TODO("GPU: draw whisper/chat/shout range ring on minimap")
    }

    private fun renderPoint(pos: Vector3, color: Color4, diameter: Int, relativeHeight: Int = 0) {
        TODO("GPU: render a single coloured point on the object image")
    }

    private fun createObjectImage() {
        TODO("GPU: allocate raw image buffer for object overlay texture")
    }

    private fun createParcelImage() {
        TODO("GPU: allocate raw image buffer for parcel overlay texture")
    }

    private fun renderPropertyLinesForRegion(region: Any, color: Color4) {
        TODO("GPU: rasterise parcel boundary lines into the parcel raw image")
    }

    private fun isMouseOnPopupMenu(): Boolean {
        TODO("APR: check whether the mouse is currently over the popup menu")
    }

    private fun updateAboutLandPopupButton() {
        TODO("APR: enable/disable About Land menu item based on current parcel")
    }

    private fun handleToolTipAgent(avatarId: String): Boolean {
        TODO("APR: show avatar name tooltip for $avatarId")
    }

    private fun isZoomChecked(userdata: Any): Boolean {
        TODO("APR: check whether the given zoom level is currently active")
    }

    private fun setZoom(userdata: Any) {
        TODO("APR: apply zoom level from menu userdata")
    }

    private fun handleStopTracking(userdata: Any) {
        TODO("APR: stop tracking the current target")
    }

    private fun activateCenterMap(userdata: Any) {
        centering = true
    }

    private fun isMapOrientationChecked(userdata: Any): Boolean {
        TODO("APR: check whether the given map orientation is active")
    }

    private fun setMapOrientation(userdata: Any) {
        TODO("APR: set map rotation orientation from menu userdata")
    }

    private fun popupShowAboutLand(userdata: Any) {
        TODO("APR: open About Land floater for popup world position")
    }

    private fun handleStartTracking() {
        TODO("APR: begin tracking the closest agent to cursor")
    }

    private fun handleMark(userdata: Any) {
        TODO("APR: apply avatar mark colour from userdata")
    }

    private fun handleClearMark() {
        clearAvatarMarkColor(closestAgentRightClick)
    }

    private fun handleClearMarks() {
        clearAvatarMarkColors()
    }

    private fun handleCam() {
        TODO("APR: move camera to the right-clicked agent's position")
    }

    private fun handleFaceTowards() {
        TODO("APR: open compass/face-towards floater")
    }

    private fun canFaceTowards(): Boolean {
        TODO("APR: return whether face-towards action is available")
    }

    private fun handleOverlayToggle(sdParam: Any) {
        TODO("APR: toggle minimap overlay layer identified by sdParam")
    }

    private fun canAddFriend(): Boolean      = TODO("APR: can add friend check")
    private fun canRemoveFriend(): Boolean   = TODO("APR: can remove friend check")
    private fun canCall(): Boolean           = TODO("APR: can call check")
    private fun canMap(): Boolean            = TODO("APR: can open map check")
    private fun canShare(): Boolean          = TODO("APR: can share check")
    private fun canOfferTeleport(): Boolean  = TODO("APR: can offer teleport check")
    private fun canBlock(): Boolean          = TODO("APR: can block check")
    private fun canFreezeEject(): Boolean    = TODO("APR: can freeze/eject check")
    private fun canKickTeleportHome(): Boolean = TODO("APR: can kick/teleport home check")
    private fun isBlocked(): Boolean         = TODO("APR: is agent blocked check")
    private fun canRequestTeleport(): Boolean = TODO("APR: can request teleport check")

    private fun handleAddFriend()          { TODO("APR: send add-friend request") }
    private fun handleAddToContactSet()    { TODO("APR: add to contact set") }
    private fun handleRemoveFriend()       { TODO("APR: send remove-friend request") }
    private fun handleIM()                 { TODO("APR: open IM session with agent") }
    private fun handleCall()               { TODO("APR: initiate voice call") }
    private fun handleMap()                { TODO("APR: open world map tracking agent") }
    private fun handleShare()              { TODO("APR: open share inventory") }
    private fun handlePay()                { TODO("APR: open pay dialog") }
    private fun handleOfferTeleport()      { TODO("APR: offer teleport to agent") }
    private fun handleRequestTeleport()    { TODO("APR: request teleport from agent") }
    private fun handleTeleportToAvatar()   { TODO("APR: teleport to agent's position") }
    private fun handleGroupInvite()        { TODO("APR: open group invite panel") }
    private fun handleGetScriptInfo()      { TODO("APR: request script info from agent") }
    private fun handleBlockUnblock()       { TODO("APR: toggle block/unblock for agent") }
    private fun handleReport()             { TODO("APR: open abuse report for agent") }
    private fun handleFreeze()             { TODO("APR: freeze agent (god action)") }
    private fun handleEject()              { TODO("APR: eject agent") }
    private fun handleKick()               { TODO("APR: kick agent") }
    private fun handleTeleportHome()       { TODO("APR: teleport agent home") }
    private fun handleEstateBan()          { TODO("APR: add agent to estate ban list") }
    private fun handleDerender(permanent: Boolean) { TODO("APR: derender agent permanent=$permanent") }
}
