package com.firestorm.newview

import java.util.UUID

// ============================================================================
// RlvUIEnabler — singleton that tracks RLV behaviour toggles and applies the
// corresponding viewer-UI changes (hide floaters, disable buttons, etc.).
// Kotlin object replaces LLSingleton<RlvUIEnabler>.
// ============================================================================

object RlvUIEnabler {

    // Signature matches RlvHandler::rlv_behaviour_signal_t: (behaviour, paramType) -> Unit
    private typealias BehaviourHandler = (isQuitting: Boolean) -> Unit

    // Multimap: one behaviour may map to several handlers
    private val handlers: MutableMap<ERlvBehaviour, MutableList<BehaviourHandler>> = mutableMapOf()

    // Nullable lambda slots replace boost::signals2::connection
    private var connFloaterGeneric: ((String, Any?) -> Boolean)? = null
    private var connFloaterShowLoc: ((String, Any?) -> Boolean)? = null
    private var connFloaterViewXXX: ((String, Any?) -> Boolean)? = null
    private var connPanelShowLoc:   ((String, String, Any?) -> Boolean)? = null

    // floater name -> optional notification callback
    private val filteredFloaterMap: MutableMap<String, (() -> Unit)?> = mutableMapOf()

    init {
        // onRefreshHoverText — showloc, showhovertext*
        register(ERlvBehaviour.RLV_BHVR_SHOWLOC)           { onRefreshHoverText() }
        register(ERlvBehaviour.RLV_BHVR_SHOWHOVERTEXTALL)  { onRefreshHoverText() }
        register(ERlvBehaviour.RLV_BHVR_SHOWHOVERTEXTWORLD){ onRefreshHoverText() }
        register(ERlvBehaviour.RLV_BHVR_SHOWHOVERTEXTHUD)  { onRefreshHoverText() }

        // onToggleMovement — fly, alwaysrun, temprun
        register(ERlvBehaviour.RLV_BHVR_FLY)       { onToggleMovement() }
        register(ERlvBehaviour.RLV_BHVR_ALWAYSRUN)  { onToggleMovement() }
        register(ERlvBehaviour.RLV_BHVR_TEMPRUN)    { onToggleMovement() }

        // onToggleViewXXX — viewnote, viewscript, viewtexture
        register(ERlvBehaviour.RLV_BHVR_VIEWNOTE)   { onToggleViewXXX() }
        register(ERlvBehaviour.RLV_BHVR_VIEWSCRIPT) { onToggleViewXXX() }
        register(ERlvBehaviour.RLV_BHVR_VIEWTEXTURE){ onToggleViewXXX() }

        // onToggleXXX — individual behaviour handlers
        register(ERlvBehaviour.RLV_BHVR_SHOWLOC)      { onToggleShowLoc() }
        register(ERlvBehaviour.RLV_BHVR_SHOWNAMES)     { onToggleShowNames() }
        register(ERlvBehaviour.RLV_BHVR_SHOWMINIMAP)   { onToggleShowMinimap() }
        register(ERlvBehaviour.RLV_BHVR_SHOWWORLDMAP)  { onToggleShowWorldMap() }
        register(ERlvBehaviour.RLV_BHVR_UNSIT)         { onToggleUnsit() }

        // onToggleTp
        register(ERlvBehaviour.RLV_BHVR_TPLOC) { onToggleTp() }
        register(ERlvBehaviour.RLV_BHVR_TPLM)  { onToggleTp() }

        // onUpdateLoginLastLocation — receives the quitting flag
        registerWithFlag(ERlvBehaviour.RLV_BHVR_TPLOC) { q -> onUpdateLoginLastLocation(q) }
        registerWithFlag(ERlvBehaviour.RLV_BHVR_UNSIT)  { q -> onUpdateLoginLastLocation(q) }
    }

    private fun register(bhvr: ERlvBehaviour, handler: () -> Unit) {
        handlers.getOrPut(bhvr) { mutableListOf() }.add { _ -> handler() }
    }

    private fun registerWithFlag(bhvr: ERlvBehaviour, handler: BehaviourHandler) {
        handlers.getOrPut(bhvr) { mutableListOf() }.add(handler)
    }

    // -------------------------------------------------------------------------
    // Signal entry point: called by RlvHandler on every behaviour toggle
    // -------------------------------------------------------------------------

    fun onBehaviourToggle(eBhvr: ERlvBehaviour, eType: ERlvParamType) {
        val isQuitting = isAppExiting()
        handlers[eBhvr]?.forEach { it(isQuitting) }
    }

    // -------------------------------------------------------------------------
    // Behaviour handlers
    // -------------------------------------------------------------------------

    private fun onRefreshHoverText() {
        System.err.println("RlvUIEnabler: onRefreshHoverText not yet implemented")
    }

    private fun onToggleMovement() {
        System.err.println("RlvUIEnabler: onToggleMovement not yet implemented")
    }

    private fun onToggleShowLoc() {
        val canShow = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)

        System.err.println("RlvUIEnabler: onToggleShowLoc navigation bar refresh not yet implemented")

        if (!canShow) {
            // Hide location-revealing floaters
            floaterHideIfVisible("about_land")
            floaterHideIfVisible("region_info")
            floaterHideIfVisible("god_tools")

            // Remove the most recent persistent teleport history entry if it
            // matches the current in-session entry, so the location is not exposed.
            System.err.println("RlvUIEnabler: onToggleShowLoc teleport history manipulation not yet implemented")

            if (connFloaterShowLoc == null) {
                connFloaterShowLoc = { name, _ -> filterFloaterShowLoc(name) }
                connPanelShowLoc   = { floater, _, key -> filterPanelShowLoc(floater, key) }
                floaterRegisterValidateCallback(connFloaterShowLoc!!)
                sidePanelRegisterValidateCallback(connPanelShowLoc!!)
            }
        } else {
            // Reset current location in teleport history (also persists it)
            System.err.println("RlvUIEnabler: onToggleShowLoc teleport history current location update not yet implemented")

            connFloaterShowLoc?.let { floaterUnregisterValidateCallback(it) }
            connPanelShowLoc?.let   { sidePanelUnregisterValidateCallback(it) }
            connFloaterShowLoc = null
            connPanelShowLoc   = null
        }
    }

    private fun onToggleShowNames() {
        val canShow = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWNAMES)
        if (!canShow) {
            System.err.println("RlvUIEnabler: onToggleShowNames hideHelper not yet implemented")
        }
    }

    private fun onToggleShowMinimap() {
        val canShow = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWMINIMAP)

        if (!canShow) {
            addGenericFloaterFilter("mini_map")
        } else {
            removeGenericFloaterFilter("mini_map")
        }

        System.err.println("RlvUIEnabler: onToggleShowMinimap floater visibility not yet implemented")
    }

    private fun onToggleShowWorldMap() {
        val canShow = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWWORLDMAP)

        if (!canShow) {
            floaterHideIfVisible("world_map")
            addGenericFloaterFilter("world_map")
        } else {
            removeGenericFloaterFilter("world_map")
        }
    }

    private fun onToggleTp() {
        val tpRestricted =
            gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_TPLM) &&
            gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_TPLOC)
        // Disable the navigation bar "Home" button only when both @tplm=n and @tploc=n are active.
        System.err.println("RlvUIEnabler: onToggleTp navigation bar home_btn not yet implemented")
    }

    private fun onToggleUnsit() {
        val canUnsit = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_UNSIT)
        System.err.println("RlvUIEnabler: onToggleUnsit stand_btn not yet implemented")
    }

    private fun onToggleViewXXX() {
        val hasViewRestriction =
            gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_VIEWNOTE) ||
            gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_VIEWSCRIPT) ||
            gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_VIEWTEXTURE)

        if (hasViewRestriction && connFloaterViewXXX == null) {
            connFloaterViewXXX = { name, _ -> filterFloaterViewXXX(name) }
            floaterRegisterValidateCallback(connFloaterViewXXX!!)
        } else if (!hasViewRestriction && connFloaterViewXXX != null) {
            floaterUnregisterValidateCallback(connFloaterViewXXX!!)
            connFloaterViewXXX = null
        }
    }

    private fun onUpdateLoginLastLocation(isQuitting: Boolean) {
        if (!isQuitting) {
            RlvSettings.updateLoginLastLocation()
        }
    }

    // -------------------------------------------------------------------------
    // Generic floater filter management
    // -------------------------------------------------------------------------

    fun addGenericFloaterFilter(floaterName: String, notificationKey: String): Boolean =
        addGenericFloaterFilter(floaterName) { RlvUtil.notifyBlocked(notificationKey) }

    fun addGenericFloaterFilter(floaterName: String, fn: (() -> Unit)? = null): Boolean {
        if (filteredFloaterMap.containsKey(floaterName)) return false
        filteredFloaterMap[floaterName] = fn
        if (connFloaterGeneric == null) {
            connFloaterGeneric = { name, _ -> filterFloaterGeneric(name) }
            floaterRegisterValidateCallback(connFloaterGeneric!!)
        }
        return true
    }

    fun removeGenericFloaterFilter(floaterName: String): Boolean {
        if (!filteredFloaterMap.containsKey(floaterName)) return false
        filteredFloaterMap.remove(floaterName)
        if (filteredFloaterMap.isEmpty()) {
            connFloaterGeneric?.let { floaterUnregisterValidateCallback(it) }
            connFloaterGeneric = null
        }
        return true
    }

    // -------------------------------------------------------------------------
    // Floater / panel filter predicates
    // -------------------------------------------------------------------------

    private fun filterFloaterGeneric(floaterName: String): Boolean {
        val entry = filteredFloaterMap[floaterName] ?: return true
        // Entry present — block the floater and optionally fire notification
        entry.invoke()
        return false
    }

    private fun filterFloaterShowLoc(name: String): Boolean = when (name) {
        "about_land"  -> canViewParcelProperties()
        "region_info" -> canViewRegionProperties()
        "god_tools"   -> false
        else          -> true
    }

    private fun filterPanelShowLoc(floater: String, key: Any?): Boolean {
        if (floater == "places") {
            val type = (key as? Map<*, *>)?.get("type")?.toString() ?: ""
            if (type == "create_landmark") return false
            if (type == "agent")           return canViewParcelProperties()
        }
        return true
    }

    private fun filterFloaterViewXXX(name: String): Boolean {
        if (gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_VIEWNOTE) && name == "preview_notecard") {
            RlvUtil.notifyBlockedViewXXX("AT_NOTECARD")
            return false
        }
        if (gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_VIEWSCRIPT) &&
            (name == "preview_script" || name == "preview_scriptedit")) {
            RlvUtil.notifyBlockedViewXXX("AT_SCRIPT")
            return false
        }
        if (gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_VIEWTEXTURE) && name == "preview_texture") {
            RlvUtil.notifyBlockedViewXXX("AT_TEXTURE")
            return false
        }
        return true
    }

    // -------------------------------------------------------------------------
    // Static helper predicates
    // -------------------------------------------------------------------------

    fun canViewParcelProperties(): Boolean {
        if (!gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)) return true
        return false
    }

    fun canViewRegionProperties(): Boolean {
        if (!gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)) return true
        return false
    }

    fun hasOpenIM(agentId: UUID): Boolean {
        return false
    }

    fun hasOpenProfile(agentId: UUID): Boolean {
        System.err.println("RlvUIEnabler: hasOpenProfile not yet implemented")
        return false
    }

    // -------------------------------------------------------------------------
    // Stubs for viewer-layer integration points
    // -------------------------------------------------------------------------

    private fun isAppExiting(): Boolean {
        System.err.println("RlvUIEnabler: isAppExiting not yet implemented")
        return false
    }

    private fun floaterHideIfVisible(name: String) {
        System.err.println("RlvUIEnabler: floaterHideIfVisible not yet implemented")
    }

    private fun floaterRegisterValidateCallback(cb: (String, Any?) -> Boolean) {
        // no-op
    }

    private fun floaterUnregisterValidateCallback(cb: (String, Any?) -> Boolean) {
        // no-op
    }

    private fun sidePanelRegisterValidateCallback(cb: (String, String, Any?) -> Boolean) {
        // no-op
    }

    private fun sidePanelUnregisterValidateCallback(cb: (String, String, Any?) -> Boolean) {
        // no-op
    }
}
