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
            System.err.println("NetMap: showAvatarInspector not yet implemented")
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
        System.err.println("NetMap: postBuild not yet implemented")
        return false
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
    }

    fun viewPosToGlobal(x: Int, y: Int): Vector3d {
        return Vector3d(0.0, 0.0, 0.0)
    }

    fun getClosestAgentToCursor(): String = closestAgentToCursor
    fun getClosestAgentPosition(): Vector3d = closestAgentPosition

    fun draw() {
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
        System.err.println("NetMap: handleHover not yet implemented")
        return false
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("NetMap: handleToolTip not yet implemented")
        return false
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        rectWidth  = width
        rectHeight = height
        updateObjectImage = true
        updateParcelImage = true
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        popupWorldPos = viewPosToGlobal(x, y)
        System.err.println("NetMap: handleRightMouseDown not yet implemented")
        return false
    }

    fun handleClick(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("NetMap: handleClick not yet implemented")
        return false
    }

    fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("NetMap: handleDoubleClick not yet implemented")
        return false
    }

    fun performDoubleClickAction(posGlobal: Vector3d) {
        System.err.println("NetMap: performDoubleClickAction not yet implemented")
    }

    fun handleShowProfile(sdParam: Any) {
        System.err.println("NetMap: handleShowProfile not yet implemented")
    }

    private fun globalPosToView(globalPos: Vector3d): Vector3 {
        return Vector3(0f, 0f, 0f)
    }

    private fun drawTracking(posGlobal: Vector3d, color: Color4, drawArrow: Boolean = true) {
    }

    private fun drawRing(radius: Float, posMap: Vector3, color: Color4) {
    }

    private fun renderPoint(pos: Vector3, color: Color4, diameter: Int, relativeHeight: Int = 0) {
    }

    private fun createObjectImage() {
    }

    private fun createParcelImage() {
    }

    private fun renderPropertyLinesForRegion(region: Any, color: Color4) {
    }

    private fun isMouseOnPopupMenu(): Boolean {
        System.err.println("NetMap: isMouseOnPopupMenu not yet implemented")
        return false
    }

    private fun updateAboutLandPopupButton() {
        System.err.println("NetMap: updateAboutLandPopupButton not yet implemented")
    }

    private fun handleToolTipAgent(avatarId: String): Boolean {
        System.err.println("NetMap: handleToolTipAgent not yet implemented")
        return false
    }

    private fun isZoomChecked(userdata: Any): Boolean {
        System.err.println("NetMap: isZoomChecked not yet implemented")
        return false
    }

    private fun setZoom(userdata: Any) {
        System.err.println("NetMap: setZoom not yet implemented")
    }

    private fun handleStopTracking(userdata: Any) {
        System.err.println("NetMap: handleStopTracking not yet implemented")
    }

    private fun activateCenterMap(userdata: Any) {
        centering = true
    }

    private fun isMapOrientationChecked(userdata: Any): Boolean {
        System.err.println("NetMap: isMapOrientationChecked not yet implemented")
        return false
    }

    private fun setMapOrientation(userdata: Any) {
        System.err.println("NetMap: setMapOrientation not yet implemented")
    }

    private fun popupShowAboutLand(userdata: Any) {
        System.err.println("NetMap: popupShowAboutLand not yet implemented")
    }

    private fun handleStartTracking() {
        System.err.println("NetMap: handleStartTracking not yet implemented")
    }

    private fun handleMark(userdata: Any) {
        System.err.println("NetMap: handleMark not yet implemented")
    }

    private fun handleClearMark() {
        clearAvatarMarkColor(closestAgentRightClick)
    }

    private fun handleClearMarks() {
        clearAvatarMarkColors()
    }

    private fun handleCam() {
        System.err.println("NetMap: handleCam not yet implemented")
    }

    private fun handleFaceTowards() {
        System.err.println("NetMap: handleFaceTowards not yet implemented")
    }

    private fun canFaceTowards(): Boolean {
        System.err.println("NetMap: canFaceTowards not yet implemented")
        return false
    }

    private fun handleOverlayToggle(sdParam: Any) {
        System.err.println("NetMap: handleOverlayToggle not yet implemented")
    }

    private fun canAddFriend(): Boolean      { System.err.println("NetMap: canAddFriend not yet implemented"); return false }
    private fun canRemoveFriend(): Boolean   { System.err.println("NetMap: canRemoveFriend not yet implemented"); return false }
    private fun canCall(): Boolean           { System.err.println("NetMap: canCall not yet implemented"); return false }
    private fun canMap(): Boolean            { System.err.println("NetMap: canMap not yet implemented"); return false }
    private fun canShare(): Boolean          { System.err.println("NetMap: canShare not yet implemented"); return false }
    private fun canOfferTeleport(): Boolean  { System.err.println("NetMap: canOfferTeleport not yet implemented"); return false }
    private fun canBlock(): Boolean          { System.err.println("NetMap: canBlock not yet implemented"); return false }
    private fun canFreezeEject(): Boolean    { System.err.println("NetMap: canFreezeEject not yet implemented"); return false }
    private fun canKickTeleportHome(): Boolean { System.err.println("NetMap: canKickTeleportHome not yet implemented"); return false }
    private fun isBlocked(): Boolean         { System.err.println("NetMap: isBlocked not yet implemented"); return false }
    private fun canRequestTeleport(): Boolean { System.err.println("NetMap: canRequestTeleport not yet implemented"); return false }

    private fun handleAddFriend()          { System.err.println("NetMap: handleAddFriend not yet implemented") }
    private fun handleAddToContactSet()    { System.err.println("NetMap: handleAddToContactSet not yet implemented") }
    private fun handleRemoveFriend()       { System.err.println("NetMap: handleRemoveFriend not yet implemented") }
    private fun handleIM()                 { System.err.println("NetMap: handleIM not yet implemented") }
    private fun handleCall()               { System.err.println("NetMap: handleCall not yet implemented") }
    private fun handleMap()                { System.err.println("NetMap: handleMap not yet implemented") }
    private fun handleShare()              { System.err.println("NetMap: handleShare not yet implemented") }
    private fun handlePay()                { System.err.println("NetMap: handlePay not yet implemented") }
    private fun handleOfferTeleport()      { System.err.println("NetMap: handleOfferTeleport not yet implemented") }
    private fun handleRequestTeleport()    { System.err.println("NetMap: handleRequestTeleport not yet implemented") }
    private fun handleTeleportToAvatar()   { System.err.println("NetMap: handleTeleportToAvatar not yet implemented") }
    private fun handleGroupInvite()        { System.err.println("NetMap: handleGroupInvite not yet implemented") }
    private fun handleGetScriptInfo()      { System.err.println("NetMap: handleGetScriptInfo not yet implemented") }
    private fun handleBlockUnblock()       { System.err.println("NetMap: handleBlockUnblock not yet implemented") }
    private fun handleReport()             { System.err.println("NetMap: handleReport not yet implemented") }
    private fun handleFreeze()             { System.err.println("NetMap: handleFreeze not yet implemented") }
    private fun handleEject()              { System.err.println("NetMap: handleEject not yet implemented") }
    private fun handleKick()               { System.err.println("NetMap: handleKick not yet implemented") }
    private fun handleTeleportHome()       { System.err.println("NetMap: handleTeleportHome not yet implemented") }
    private fun handleEstateBan()          { System.err.println("NetMap: handleEstateBan not yet implemented") }
    private fun handleDerender(permanent: Boolean) { System.err.println("NetMap: handleDerender not yet implemented") }
}
