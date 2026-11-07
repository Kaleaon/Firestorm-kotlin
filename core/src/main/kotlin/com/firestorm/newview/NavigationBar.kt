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
        System.err.println("PullButton: set button enabled state not yet implemented")
    }

    fun setForcePressedState(pressed: Boolean) {
        System.err.println("PullButton: force button pressed visual state not yet implemented")
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
        System.err.println("NavigationBar: bind UI child controls from navigation_bar XUI view not yet implemented")
    }

    fun handleLoginComplete() {
        System.err.println("NavigationBar: notify TeleportHistory, StatusBar, and LocationInputCtrl of login completion not yet implemented")
    }

    fun clearHistoryCache() {
        cmbLocation?.removeAll()
        System.err.println("NavigationBar: clear LLLocationHistory and TeleportHistory items, save history not yet implemented")
    }

    fun isRebakeNavMeshAvailable(): Boolean =
        cmbLocation?.isNavMeshDirty() ?: false

    fun refreshLocationCtrl() {
        cmbLocation?.refresh()
    }

    fun clearHistory() {
        System.err.println("NavigationBar: clear search combo box history not yet implemented")
    }

    fun getView(): Any? = view

    private fun fillSearchComboBox() {
        System.err.println("NavigationBar: load search history entries into the search combo box not yet implemented")
    }

    private fun rebuildTeleportHistoryMenu() {
        System.err.println("NavigationBar: rebuild teleport history popup menu from LLTeleportHistory items not yet implemented")
    }

    private fun showTeleportHistoryMenu(btnCtrl: Any) {
        if (teleportHistoryMenu == null) rebuildTeleportHistoryMenu()
        System.err.println("NavigationBar: show teleport history popup near btnCtrl and capture mouse not yet implemented")
    }

    private fun invokeSearch(searchText: String) {
        System.err.println("NavigationBar: open search floater with query not yet implemented")
    }

    private fun resizeLayoutPanel() {
        System.err.println("NavigationBar: resize navigation layout panel based on saved ratio setting not yet implemented")
    }

    private fun onTeleportHistoryMenuItemClicked(userdata: Any) {
        System.err.println("NavigationBar: navigate TeleportHistory to item index from userdata not yet implemented")
    }

    private fun onTeleportHistoryChanged() {
        System.err.println("NavigationBar: update back/forward button enabled states from TeleportHistory cursor not yet implemented")
    }

    private fun onBackButtonClicked(ctrl: Any) {
        System.err.println("NavigationBar: LLTeleportHistory.goBack() and release focus not yet implemented")
    }

    private fun onBackOrForwardButtonHeldDown(ctrl: Any, param: Any) {
        System.err.println("NavigationBar: show teleport history menu on first held-down event not yet implemented")
    }

    private fun onNavigationButtonHeldUp(navButton: Any) {
        System.err.println("NavigationBar: clear forced-pressed state and release mouse capture not yet implemented")
    }

    private fun onForwardButtonClicked(ctrl: Any) {
        System.err.println("NavigationBar: LLTeleportHistory.goForward() and release focus not yet implemented")
    }

    private fun onHomeButtonClicked(ctrl: Any) {
        System.err.println("NavigationBar: agent.teleportHome() and release focus not yet implemented")
    }

    private fun onLandmarksButtonClicked() {
        System.err.println("NavigationBar: toggle/show places floater on landmarks tab not yet implemented")
    }

    private fun onLocationSelection() {
        val typedLocation = cmbLocation?.getSimple()?.trim() ?: return
        if (typedLocation.isEmpty()) return
        System.err.println("NavigationBar: resolve typedLocation as SLURL/landmark/region-name and teleport not yet implemented")
    }

    private fun onLocationPrearrange(data: Any) {
        System.err.println("NavigationBar: pre-arrange location dropdown list based on current input not yet implemented")
    }

    private fun onSearchCommit() {
        System.err.println("NavigationBar: add search query to history and invoke search floater not yet implemented")
    }

    private fun onTeleportFinished(globalAgentPos: Vector3d) {
        if (!saveToLocationHistory) return
        System.err.println("NavigationBar: build location string and add to LLLocationHistory, then save not yet implemented")
    }

    private fun onTeleportFailed() {
        saveToLocationHistory = false
    }

    private fun onNavbarResized() {
        System.err.println("NavigationBar: recalculate and persist NavigationBarRatio setting not yet implemented")
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
            System.err.println("NavigationBar: convert regionHandle to global pos, teleport agent there not yet implemented")
        } else if (typedLocation.isNotEmpty()) {
            invokeSearch(typedLocation)
        }
    }

    private fun onRightMouseDown(x: Int, y: Int, mask: Int) {
        System.err.println("NavigationBar: show_navbar_context_menu not yet implemented")
    }

    private fun onClickedLightingBtn() {
        System.err.println("NavigationBar: open env_adjust_snapshot floater not yet implemented")
    }

    private fun updateRlvRestrictions(behavior: Any, type: Any) {
        System.err.println("NavigationBar: enable/disable PersonalLighting button based on RLV SETENV restriction not yet implemented")
    }
}
