package com.firestorm.newview

import java.util.UUID

private const val SECONDS_BETWEEN_UPDATE_REQUESTS = 5.0f

class FloaterGodTools(private val key: Any?) {

    enum class EGodPanel {
        PANEL_GRID,
        PANEL_REGION,
        PANEL_OBJECT,
        PANEL_REQUEST,
        PANEL_COUNT
    }

    var panelRegionTools: PanelRegionTools? = null
    var panelObjectTools: PanelObjectTools? = null

    var currentHost: String = ""
    private var updateTimerElapsed: Float = 0f

    fun postBuild(): Boolean {
        sendRegionInfoRequest()
        return true
    }

    fun onOpen(key: Any?) {
        if (panelObjectTools != null) {
            panelObjectTools!!.setTargetAvatar(null)
        }
        if (agentGetRegionHost() != currentHost) {
            sendRegionInfoRequest()
        }
    }

    fun draw(deltaTime: Float) {
        updateTimerElapsed += deltaTime
        if (currentHost.isEmpty()) {
            if (updateTimerElapsed > SECONDS_BETWEEN_UPDATE_REQUESTS) {
                sendRegionInfoRequest()
            }
        } else if (agentGetRegionHost() != currentHost) {
            sendRegionInfoRequest()
        }
    }

    fun showPanel(panelName: String) {
        System.err.println("FloaterGodTools: showPanel not yet implemented")
    }

    fun updatePopup(centerX: Int, centerY: Int, mask: Int) {
    }

    fun sendRegionInfoRequest() {
        panelRegionTools?.clearAllWidgets()
        panelObjectTools?.clearAllWidgets()
        currentHost = ""
        updateTimerElapsed = 0f
        System.err.println("FloaterGodTools: sendRegionInfoRequest not yet implemented")
    }

    fun sendGodUpdateRegionInfo() {
        val rtool = panelRegionTools ?: return
        System.err.println("FloaterGodTools: sendGodUpdateRegionInfo not yet implemented")
    }

    fun computeRegionFlags(): ULong {
        var flags = agentGetRegionFlags()
        panelRegionTools?.let { flags = it.computeRegionFlags(flags) }
        panelObjectTools?.let { flags = it.computeRegionFlags(flags) }
        return flags
    }

    companion object {
        fun refreshAll() {
            System.err.println("FloaterGodTools: refreshAll not yet implemented")
        }

        fun processRegionInfo(msg: Any?) {
            System.err.println("FloaterGodTools: processRegionInfo not yet implemented")
        }
    }
}

private fun agentGetRegionHost(): String = ""
private fun agentGetRegionFlags(): ULong = 0uL

class PanelRegionTools {

    private val BILLABLE_FACTOR_DEFAULT = 1.0f
    private val PRICE_PER_METER_DEFAULT = 1.0f

    private var simName: String = "unknown"
    private var estateId: UInt = 0u
    private var parentEstateId: UInt = 0u
    private var gridPosX: Int = 0
    private var gridPosY: Int = 0
    private var redirectGridX: Int = 0
    private var redirectGridY: Int = 0
    private var billableFactor: Float = 1.0f
    private var pricePerMeter: Int = 1

    private var checkPrelude: Boolean = false
    private var checkFixedSun: Boolean = false
    private var checkResetHome: Boolean = false
    private var checkDamage: Boolean = false
    private var checkVisible: Boolean = false
    private var blockTerraform: Boolean = false
    private var blockDwell: Boolean = false
    private var isSandbox: Boolean = false

    private var widgetsEnabled: Boolean = false
    private var applyEnabled: Boolean = false

    fun postBuild(): Boolean = true

    fun refresh() {}

    fun getSimName(): String = simName
    fun getEstateID(): UInt = estateId
    fun getParentEstateID(): UInt = parentEstateId
    fun getGridPosX(): Int = gridPosX
    fun getGridPosY(): Int = gridPosY
    fun getRedirectGridX(): Int = redirectGridX
    fun getRedirectGridY(): Int = redirectGridY
    fun getBillableFactor(): Float = billableFactor
    fun getPricePerMeter(): Int = pricePerMeter

    fun getRegionFlags(): ULong {
        var flags = 0uL
        if (checkPrelude) flags = setPreludeFlags(flags) else flags = unsetPreludeFlags(flags)
        if (checkFixedSun) flags = flags or REGION_FLAGS_SUN_FIXED
        if (checkResetHome) flags = flags or REGION_FLAGS_RESET_HOME_ON_TELEPORT
        if (checkVisible) flags = flags or REGION_FLAGS_EXTERNALLY_VISIBLE
        if (checkDamage) flags = flags or REGION_FLAGS_ALLOW_DAMAGE
        if (blockTerraform) flags = flags or REGION_FLAGS_BLOCK_TERRAFORM
        if (blockDwell) flags = flags or REGION_FLAGS_BLOCK_DWELL
        if (isSandbox) flags = flags or REGION_FLAGS_SANDBOX
        return flags
    }

    fun getRegionFlagsMask(): ULong {
        var flags = 0xFFFFFFFFFFFFFFFFuL
        if (checkPrelude) flags = setPreludeFlags(flags) else flags = unsetPreludeFlags(flags)
        if (!checkFixedSun) flags = flags and REGION_FLAGS_SUN_FIXED.inv()
        if (!checkResetHome) flags = flags and REGION_FLAGS_RESET_HOME_ON_TELEPORT.inv()
        if (!checkVisible) flags = flags and REGION_FLAGS_EXTERNALLY_VISIBLE.inv()
        if (!checkDamage) flags = flags and REGION_FLAGS_ALLOW_DAMAGE.inv()
        if (!blockTerraform) flags = flags and REGION_FLAGS_BLOCK_TERRAFORM.inv()
        if (!blockDwell) flags = flags and REGION_FLAGS_BLOCK_DWELL.inv()
        if (!isSandbox) flags = flags and REGION_FLAGS_SANDBOX.inv()
        return flags
    }

    fun computeRegionFlags(initialFlags: ULong): ULong {
        var flags = initialFlags and getRegionFlagsMask()
        flags = flags or getRegionFlags()
        return flags
    }

    fun setSimName(name: String) { simName = name }
    fun setEstateID(id: UInt) { estateId = id }
    fun setParentEstateID(id: UInt) { parentEstateId = id }
    fun setGridPosX(pos: Int) { gridPosX = pos }
    fun setGridPosY(pos: Int) { gridPosY = pos }
    fun setRedirectGridX(pos: Int) { redirectGridX = pos }
    fun setRedirectGridY(pos: Int) { redirectGridY = pos }
    fun setBillableFactor(factor: Float) { billableFactor = factor }
    fun setPricePerMeter(price: Int) { pricePerMeter = price }

    fun setCheckFlags(flags: ULong) {
        checkPrelude = isPrelude(flags)
        checkFixedSun = isFlagSet(flags, REGION_FLAGS_SUN_FIXED)
        checkResetHome = isFlagSet(flags, REGION_FLAGS_RESET_HOME_ON_TELEPORT)
        checkDamage = isFlagSet(flags, REGION_FLAGS_ALLOW_DAMAGE)
        checkVisible = isFlagSet(flags, REGION_FLAGS_EXTERNALLY_VISIBLE)
        blockTerraform = isFlagSet(flags, REGION_FLAGS_BLOCK_TERRAFORM)
        blockDwell = isFlagSet(flags, REGION_FLAGS_BLOCK_DWELL)
        isSandbox = isFlagSet(flags, REGION_FLAGS_SANDBOX)
    }

    fun clearAllWidgets() {
        simName = "unknown"
        checkPrelude = false
        checkFixedSun = false
        checkResetHome = false
        checkDamage = false
        checkVisible = false
        blockTerraform = false
        blockDwell = false
        isSandbox = false
        billableFactor = BILLABLE_FACTOR_DEFAULT
        pricePerMeter = PRICE_PER_METER_DEFAULT.toInt()
        widgetsEnabled = false
        applyEnabled = false
    }

    fun enableAllWidgets() {
        widgetsEnabled = true
        applyEnabled = false
    }

    fun onChangeAnything() {
        applyEnabled = true
    }

    fun onChangePrelude() {
        if (checkPrelude) {
            checkFixedSun = true
            checkResetHome = true
            onChangeAnything()
        }
    }

    fun onRefresh() {
        System.err.println("PanelRegionTools: onRefresh not yet implemented")
    }

    fun onApplyChanges() {
        applyEnabled = false
        System.err.println("PanelRegionTools: onApplyChanges not yet implemented")
    }

    fun onBakeTerrain() {
        PanelRequestTools.sendRequest("terrain", "bake", agentGetRegionHost())
    }

    fun onRevertTerrain() {
        PanelRequestTools.sendRequest("terrain", "revert", agentGetRegionHost())
    }

    fun onSwapTerrain() {
        PanelRequestTools.sendRequest("terrain", "swap", agentGetRegionHost())
    }

    fun onSelectRegion() {
        System.err.println("PanelRegionTools: onSelectRegion not yet implemented")
    }

    fun onSaveState() {
        System.err.println("PanelRegionTools: onSaveState not yet implemented")
    }
}

class PanelGridTools {

    private var kickMessage: String = ""

    fun postBuild(): Boolean = true

    fun refresh() {}

    fun onClickFlushMapVisibilityCaches() {
        System.err.println("PanelGridTools: onClickFlushMapVisibilityCaches not yet implemented")
    }

    fun flushMapVisibilityCachesConfirm(selectedOption: Int): Boolean {
        if (selectedOption != 0) return false
        System.err.println("PanelGridTools: flushMapVisibilityCachesConfirm not yet implemented")
        return false
    }
}

class PanelObjectTools {

    private var targetAvatar: UUID? = null
    private var simWideDeletesFlags: UInt = 0u

    private var disableScripts: Boolean = false
    private var disableCollisions: Boolean = false
    private var disablePhysics: Boolean = false
    private var widgetsEnabled: Boolean = false
    private var applyEnabled: Boolean = false

    fun postBuild(): Boolean {
        refresh()
        return true
    }

    fun refresh() {
        System.err.println("PanelObjectTools: refresh not yet implemented")
    }

    fun setTargetAvatar(targetId: UUID?) {
        targetAvatar = targetId
    }

    fun computeRegionFlags(flags: ULong): ULong {
        var result = flags
        result = if (disableScripts) result or REGION_FLAGS_SKIP_SCRIPTS else result and REGION_FLAGS_SKIP_SCRIPTS.inv()
        result = if (disableCollisions) result or REGION_FLAGS_SKIP_COLLISIONS else result and REGION_FLAGS_SKIP_COLLISIONS.inv()
        result = if (disablePhysics) result or REGION_FLAGS_SKIP_PHYSICS else result and REGION_FLAGS_SKIP_PHYSICS.inv()
        return result
    }

    fun setCheckFlags(flags: ULong) {
        disableScripts = isFlagSet(flags, REGION_FLAGS_SKIP_SCRIPTS)
        disableCollisions = isFlagSet(flags, REGION_FLAGS_SKIP_COLLISIONS)
        disablePhysics = isFlagSet(flags, REGION_FLAGS_SKIP_PHYSICS)
    }

    fun clearAllWidgets() {
        disableScripts = false
        widgetsEnabled = false
        applyEnabled = false
    }

    fun enableAllWidgets() {
        widgetsEnabled = true
        applyEnabled = false
    }

    fun onChangeAnything() {
        applyEnabled = true
    }

    fun onApplyChanges() {
        applyEnabled = false
        System.err.println("PanelObjectTools: onApplyChanges not yet implemented")
    }

    fun onClickSet() {
        System.err.println("PanelObjectTools: onClickSet not yet implemented")
    }

    fun callbackAvatarID(ids: List<UUID>, names: List<String>) {
        if (ids.isEmpty() || names.isEmpty()) return
        targetAvatar = ids[0]
        refresh()
    }

    fun onClickDeletePublicOwnedBy() {
        if (targetAvatar == null) return
        simWideDeletesFlags = SWD_SCRIPTED_ONLY or SWD_OTHERS_LAND_ONLY
        System.err.println("PanelObjectTools: onClickDeletePublicOwnedBy not yet implemented")
    }

    fun onClickDeleteAllScriptedOwnedBy() {
        if (targetAvatar == null) return
        simWideDeletesFlags = SWD_SCRIPTED_ONLY
        System.err.println("PanelObjectTools: onClickDeleteAllScriptedOwnedBy not yet implemented")
    }

    fun onClickDeleteAllOwnedBy() {
        if (targetAvatar == null) return
        simWideDeletesFlags = 0u
        System.err.println("PanelObjectTools: onClickDeleteAllOwnedBy not yet implemented")
    }

    fun onGetTopColliders() {
        System.err.println("PanelObjectTools: onGetTopColliders not yet implemented")
    }

    fun onGetTopScripts() {
        System.err.println("PanelObjectTools: onGetTopScripts not yet implemented")
    }

    fun onGetScriptDigest() {
        PanelRequestTools.sendRequest("scriptdigest", "0", agentGetRegionHost())
    }

    companion object {
        fun callbackSimWideDeletes(avatarId: UUID?, flags: UInt): Boolean {
            if (avatarId != null) {
                sendSimWideDeletes(avatarId, flags)
            }
            return false
        }
    }
}

class PanelRequestTools {

    private val SELECTION = "Selection"
    private val AGENT_REGION = "Agent Region"

    fun postBuild(): Boolean {
        refresh()
        return true
    }

    fun refresh() {
        System.err.println("PanelRequestTools: refresh not yet implemented")
    }

    fun onClickRequest() {
        System.err.println("PanelRequestTools: onClickRequest not yet implemented")
    }

    fun sendRequest(host: String) {
        System.err.println("PanelRequestTools: sendRequest not yet implemented")
    }

    companion object {
        fun sendRequest(request: String, parameter: String, host: String) {
            System.err.println("PanelRequestTools: sendRequest not yet implemented")
        }
    }
}

fun sendSimWideDeletes(ownerId: UUID, flags: UInt) {
    System.err.println("FloaterGodTools: sendSimWideDeletes not yet implemented")
}

private val REGION_FLAGS_SUN_FIXED: ULong = 0x0000000000000010uL
private val REGION_FLAGS_RESET_HOME_ON_TELEPORT: ULong = 0x0000000000000040uL
private val REGION_FLAGS_EXTERNALLY_VISIBLE: ULong = 0x0000000000000080uL
private val REGION_FLAGS_ALLOW_DAMAGE: ULong = 0x0000000000000001uL
private val REGION_FLAGS_BLOCK_TERRAFORM: ULong = 0x0000000000000400uL
private val REGION_FLAGS_BLOCK_DWELL: ULong = 0x0000000020000000uL
private val REGION_FLAGS_SANDBOX: ULong = 0x0000000000000100uL
private val REGION_FLAGS_SKIP_SCRIPTS: ULong = 0x0000000000040000uL
private val REGION_FLAGS_SKIP_COLLISIONS: ULong = 0x0000000000080000uL
private val REGION_FLAGS_SKIP_PHYSICS: ULong = 0x0000000000100000uL

private val SWD_SCRIPTED_ONLY: UInt = 0x00000001u
private val SWD_OTHERS_LAND_ONLY: UInt = 0x00000002u

private fun isFlagSet(flags: ULong, flag: ULong): Boolean = (flags and flag) != 0uL
private fun isPrelude(flags: ULong): Boolean = false
private fun setPreludeFlags(flags: ULong): ULong = flags
private fun unsetPreludeFlags(flags: ULong): ULong = flags
