package com.firestorm.newview

import kotlin.math.*

class PullButton(val direction: String = "down") {

    private var lastMouseDown = Vector2f(0f, 0f)
    private val draggingDirection: Vector2f = directionVector(direction)

    private val clickDraggingCallbacks: MutableList<() -> Unit> = mutableListOf()

    fun setClickDraggingCallback(cb: () -> Unit) {
        clickDraggingCallbacks.add(cb)
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        lastMouseDown = Vector2f(x.toFloat(), y.toFloat())
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        lastMouseDown = Vector2f(0f, 0f)
        return true
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        val cursor = Vector2f(x.toFloat(), y.toFloat())
        val dx = cursor.x - lastMouseDown.x
        val dy = cursor.y - lastMouseDown.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag > 0f) {
            val nx = dx / mag; val ny = dy / mag
            val dot = nx * draggingDirection.x + ny * draggingDirection.y
            val angle = acos(dot.coerceIn(-1f, 1f))
            if (angle < PI.toFloat() / 4f) {
                clickDraggingCallbacks.forEach { it() }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        TODO("APR: set button enabled state to $enabled")
    }

    fun setForcePressedState(pressed: Boolean) {
        TODO("APR: force button pressed visual state = $pressed")
    }

    private companion object {
        fun directionVector(name: String): Vector2f = when (name) {
            "left"  -> Vector2f(-1f, 0f)
            "right" -> Vector2f(0f, 1f)
            "up"    -> Vector2f(0f, 1f)
            else    -> Vector2f(0f, -1f)   // "down"
        }
    }
}

object NavigationBar {

    private var teleportHistoryMenu: Any? = null

    private var btnBack: PullButton?       = null
    private var btnForward: PullButton?    = null
    private var btnHome: Any?              = null
    private var btnLandmarks: Any?         = null
    private var cmbLocation: LocationInputCtrl? = null
    private var searchComboBox: Any?       = null
    private var navigationPanel: Any?      = null
    private var favoritePanel: Any?        = null
    private var view: Any?                 = null

    private var navPanWidth = 0
    private var saveToLocationHistory = false

    private val teleportFailedListeners: MutableList<() -> Unit>        = mutableListOf()
    private val teleportFinishedListeners: MutableList<(Vector3d) -> Unit> = mutableListOf()
    private val historyMenuListeners: MutableList<() -> Unit>           = mutableListOf()
    private val rlvBehaviorListeners: MutableList<(Any, Any) -> Unit>   = mutableListOf()

    fun setupPanel() {
        TODO("APR: bind UI child controls from navigation_bar XUI view")
    }

    fun handleLoginComplete() {
        TODO("APR: notify TeleportHistory, StatusBar, and LocationInputCtrl of login completion")
    }

    fun clearHistoryCache() {
        cmbLocation?.removeAll()
        TODO("APR: clear LLLocationHistory and TeleportHistory items, save history")
    }

    fun isRebakeNavMeshAvailable(): Boolean =
        cmbLocation?.isNavMeshDirty() ?: false

    fun refreshLocationCtrl() {
        cmbLocation?.refresh()
    }

    fun clearHistory() {
        TODO("APR: clear search combo box history")
    }

    fun getView(): Any? = view

    private fun fillSearchComboBox() {
        TODO("APR: load search history entries into the search combo box")
    }

    private fun rebuildTeleportHistoryMenu() {
        TODO("APR: rebuild teleport history popup menu from LLTeleportHistory items")
    }

    private fun showTeleportHistoryMenu(btnCtrl: Any) {
        if (teleportHistoryMenu == null) rebuildTeleportHistoryMenu()
        TODO("APR: show teleport history popup near btnCtrl and capture mouse")
    }

    private fun invokeSearch(searchText: String) {
        TODO("APR: open search floater with query='$searchText'")
    }

    private fun resizeLayoutPanel() {
        TODO("APR: resize navigation layout panel based on saved ratio setting")
    }

    private fun onTeleportHistoryMenuItemClicked(userdata: Any) {
        TODO("APR: navigate TeleportHistory to item index from userdata")
    }

    private fun onTeleportHistoryChanged() {
        TODO("APR: update back/forward button enabled states from TeleportHistory cursor")
    }

    private fun onBackButtonClicked(ctrl: Any) {
        TODO("APR: LLTeleportHistory.goBack() and release focus")
    }

    private fun onBackOrForwardButtonHeldDown(ctrl: Any, param: Any) {
        TODO("APR: show teleport history menu on first held-down event")
    }

    private fun onNavigationButtonHeldUp(navButton: Any) {
        TODO("APR: clear forced-pressed state and release mouse capture")
    }

    private fun onForwardButtonClicked(ctrl: Any) {
        TODO("APR: LLTeleportHistory.goForward() and release focus")
    }

    private fun onHomeButtonClicked(ctrl: Any) {
        TODO("APR: agent.teleportHome() and release focus")
    }

    private fun onLandmarksButtonClicked() {
        TODO("APR: toggle/show places floater on landmarks tab")
    }

    private fun onLocationSelection() {
        val typedLocation = cmbLocation?.getSimple()?.trim() ?: return
        if (typedLocation.isEmpty()) return
        TODO("APR: resolve typedLocation as SLURL/landmark/region-name and teleport")
    }

    private fun onLocationPrearrange(data: Any) {
        TODO("APR: pre-arrange location dropdown list based on current input")
    }

    private fun onSearchCommit() {
        TODO("APR: add search query to history and invoke search floater")
    }

    private fun onTeleportFinished(globalAgentPos: Vector3d) {
        if (!saveToLocationHistory) return
        TODO("APR: build location string and add to LLLocationHistory, then save")
    }

    private fun onTeleportFailed() {
        saveToLocationHistory = false
    }

    private fun onNavbarResized() {
        TODO("APR: recalculate and persist NavigationBarRatio setting")
    }

    private fun onRegionNameResponse(
        typedLocation: String,
        regionName: String,
        localCoords: Vector3,
        regionHandle: ULong,
        url: String,
        snapshotId: String,
        teleport: Boolean
    ) {
        if (regionHandle != 0UL) {
            TODO("APR: convert regionHandle to global pos, teleport agent there")
        } else if (typedLocation.isNotEmpty()) {
            invokeSearch(typedLocation)
        }
    }

    private fun onRightMouseDown(x: Int, y: Int, mask: Int) {
        TODO("APR: show_navbar_context_menu at ($x, $y)")
    }

    private fun onClickedLightingBtn() {
        TODO("APR: open env_adjust_snapshot floater")
    }

    private fun updateRlvRestrictions(behavior: Any, type: Any) {
        TODO("APR: enable/disable PersonalLighting button based on RLV SETENV restriction")
    }
}
