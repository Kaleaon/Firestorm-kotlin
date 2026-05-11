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
        TODO("GPU: LLHUDText::refreshAllObjectText()")
    }

    private fun onToggleMovement() {
        TODO("GPU: enforce fly/alwaysrun/temprun restrictions via agent and update movement panel")
    }

    private fun onToggleShowLoc() {
        val canShow = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)

        TODO("GPU: refresh navigation bar location control")

        if (!canShow) {
            // Hide location-revealing floaters
            floaterHideIfVisible("about_land")
            floaterHideIfVisible("region_info")
            floaterHideIfVisible("god_tools")

            // Remove the most recent persistent teleport history entry if it
            // matches the current in-session entry, so the location is not exposed.
            TODO("APR: use JVM equivalent for LLTeleportHistory / LLTeleportHistoryStorage manipulation")

            if (connFloaterShowLoc == null) {
                connFloaterShowLoc = { name, _ -> filterFloaterShowLoc(name) }
                connPanelShowLoc   = { floater, _, key -> filterPanelShowLoc(floater, key) }
                floaterRegisterValidateCallback(connFloaterShowLoc!!)
                sidePanelRegisterValidateCallback(connPanelShowLoc!!)
            }
        } else {
            // Reset current location in teleport history (also persists it)
            TODO("APR: use JVM equivalent to update teleport history current location")

            connFloaterShowLoc?.let { floaterUnregisterValidateCallback(it) }
            connPanelShowLoc?.let   { sidePanelUnregisterValidateCallback(it) }
            connFloaterShowLoc = null
            connPanelShowLoc   = null
        }
    }

    private fun onToggleShowNames() {
        val canShow = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWNAMES)
        if (!canShow) {
            TODO("GPU: LLChatMentionHelper::instance().hideHelper()")
        }
    }

    private fun onToggleShowMinimap() {
        val canShow = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWMINIMAP)

        if (!canShow) {
            addGenericFloaterFilter("mini_map")
        } else {
            removeGenericFloaterFilter("mini_map")
        }

        TODO("GPU: hide/restore mini_map floater and break/reestablish visibility bindings in people panel and radar panel")
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
        TODO("GPU: set navigation bar home_btn enabled = !tpRestricted")
    }

    private fun onToggleUnsit() {
        val canUnsit = !gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_UNSIT)
        TODO("GPU: set stand_btn enabled = canUnsit in LLPanelStandStopFlying")
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
        TODO("APR: use JVM equivalent to check parcel ownership / group land-return power via LLViewerParcelMgr")
    }

    fun canViewRegionProperties(): Boolean {
        if (!gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)) return true
        TODO("APR: use JVM equivalent to check region estate manager status via LLViewerRegion")
    }

    fun hasOpenIM(agentId: UUID): Boolean {
        TODO("APR: use JVM equivalent to check whether an IM session panel is open for agentId")
    }

    fun hasOpenProfile(agentId: UUID): Boolean {
        TODO("APR: use JVM equivalent to LLAvatarActions::profileVisible(agentId)")
    }

    // -------------------------------------------------------------------------
    // Stubs for viewer-layer integration points
    // -------------------------------------------------------------------------

    private fun isAppExiting(): Boolean {
        TODO("APR: use JVM equivalent to LLApp::isExiting()")
    }

    private fun floaterHideIfVisible(name: String) {
        TODO("GPU: LLFloaterReg::hideInstance($name)")
    }

    private fun floaterRegisterValidateCallback(cb: (String, Any?) -> Boolean) {
        TODO("GPU: LLFloaterReg::setValidateCallback")
    }

    private fun floaterUnregisterValidateCallback(cb: (String, Any?) -> Boolean) {
        TODO("GPU: disconnect LLFloaterReg validate callback")
    }

    private fun sidePanelRegisterValidateCallback(cb: (String, String, Any?) -> Boolean) {
        TODO("GPU: LLFloaterSidePanelContainer::setValidateCallback")
    }

    private fun sidePanelUnregisterValidateCallback(cb: (String, String, Any?) -> Boolean) {
        TODO("GPU: disconnect LLFloaterSidePanelContainer validate callback")
    }
}
