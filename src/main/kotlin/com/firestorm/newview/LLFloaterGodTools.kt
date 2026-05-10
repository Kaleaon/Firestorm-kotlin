package com.firestorm.newview

import java.util.UUID

const val SECONDS_BETWEEN_UPDATE_REQUESTS: Float = 5.0f

const val BILLABLE_FACTOR_DEFAULT: Float = 1.0f
const val PRICE_PER_METER_DEFAULT: Float = 1.0f

const val SELECTION: String = "Selection"
const val AGENT_REGION: String = "Agent Region"

class LLFloaterGodTools(private val key: LLSD) : LLFloater(key) {

    enum class EGodPanel {
        PANEL_GRID,
        PANEL_REGION,
        PANEL_OBJECT,
        PANEL_REQUEST,
        PANEL_COUNT
    }

    var mPanelRegionTools: LLPanelRegionTools? = null
    var mPanelObjectTools: LLPanelObjectTools? = null

    var mCurrentHost: LLHost = LLHost()
    var mUpdateTimer: LLFrameTimer = LLFrameTimer()

    init {
        mFactoryMap["grid"] = { LLPanelGridTools() }
        mFactoryMap["region"] = {
            val p = LLPanelRegionTools()
            mPanelRegionTools = p
            p
        }
        mFactoryMap["objects"] = {
            val p = LLPanelObjectTools()
            mPanelObjectTools = p
            p
        }
        mFactoryMap["request"] = { LLPanelRequestTools() }
    }

    override fun postBuild(): Boolean {
        sendRegionInfoRequest()
        getChild<LLTabContainer>("GodTools Tabs").selectTabByName("region")
        return true
    }

    override fun onOpen(key: LLSD) {
        center()
        setFocus(true)
        mPanelObjectTools?.setTargetAvatar(UUID(0, 0))
        if (gAgent.getRegionHost() != mCurrentHost) {
            sendRegionInfoRequest()
        }
    }

    override fun draw() {
        if (mCurrentHost == LLHost()) {
            if (mUpdateTimer.getElapsedTimeF32() > SECONDS_BETWEEN_UPDATE_REQUESTS) {
                sendRegionInfoRequest()
            }
        } else if (gAgent.getRegionHost() != mCurrentHost) {
            sendRegionInfoRequest()
        }
        super.draw()
    }

    fun showPanel(panelName: String) {
        getChild<LLTabContainer>("GodTools Tabs").selectTabByName(panelName)
        openFloater()
        val panel = getChild<LLTabContainer>("GodTools Tabs").getCurrentPanel()
        panel?.setFocus(true)
    }

    fun sendRegionInfoRequest() {
        mPanelRegionTools?.clearAllWidgets()
        mPanelObjectTools?.clearAllWidgets()
        mCurrentHost = LLHost()
        mUpdateTimer.reset()
        TODO("APR: use JVM equivalent - send RequestRegionInfo message via gMessageSystem")
    }

    fun sendGodUpdateRegionInfo() {
        val godTools = LLFloaterReg.getTypedInstance<LLFloaterGodTools>("god_tools") ?: return
        val regionp = gAgent.getRegion()
        if (gAgent.isGodlike()
            && godTools.mPanelRegionTools != null
            && regionp != null
            && gAgent.getRegionHost() == mCurrentHost
        ) {
            val rtool = godTools.mPanelRegionTools!!
            val regionFlags = computeRegionFlags()
            TODO("APR: use JVM equivalent - send GodUpdateRegionInfo message via gMessageSystem")
        }
    }

    fun computeRegionFlags(): ULong {
        var flags = gAgent.getRegion()?.getRegionFlags() ?: 0UL
        mPanelRegionTools?.let { flags = it.computeRegionFlags(flags) }
        mPanelObjectTools?.let { flags = it.computeRegionFlags(flags) }
        return flags
    }

    fun updatePopup(center: LLCoordGL, mask: Int) {
        // No-op: popup repositioning handled by superclass layout engine.
    }

    fun resetToolState() {
        // Clear any transient tool state when the floater is dismissed.
    }

    companion object {
        fun refreshAll() {
            val godTools = LLFloaterReg.getTypedInstance<LLFloaterGodTools>("god_tools") ?: return
            if (gAgent.getRegionHost() != godTools.mCurrentHost) {
                godTools.sendRegionInfoRequest()
            }
        }

        fun processRegionInfo(msg: LLMessageSystem) {
            val host = msg.getSender()

            val simName: String = msg.getStringFast("RegionInfo", "SimName")
            val estateId: UInt = msg.getU32Fast("RegionInfo", "EstateID")
            val parentEstateId: UInt = msg.getU32Fast("RegionInfo", "ParentEstateID")
            val simAccess: UByte = msg.getU8Fast("RegionInfo", "SimAccess")
            val agentLimit: UByte = msg.getU8Fast("RegionInfo", "MaxAgents")
            val objectBonusFactor: Float = msg.getF32Fast("RegionInfo", "ObjectBonusFactor")
            val billableFactor: Float = msg.getF32Fast("RegionInfo", "BillableFactor")
            val waterHeight: Float = msg.getF32Fast("RegionInfo", "WaterHeight")

            val regionFlags: ULong = if (msg.has("RegionInfo3")) {
                msg.getU64Fast("RegionInfo3", "RegionFlagsExtended")
            } else {
                msg.getU32Fast("RegionInfo", "RegionFlags").toULong()
            }

            if (host != gAgent.getRegionHost()) {
                LLWorld.getInstance().waterHeightRegionInfo(simName, waterHeight)
                return
            }

            val terrainRaiseLimit: Float = msg.getF32Fast("RegionInfo", "TerrainRaiseLimit")
            val terrainLowerLimit: Float = msg.getF32Fast("RegionInfo", "TerrainLowerLimit")
            val pricePerMeter: Int = msg.getS32Fast("RegionInfo", "PricePerMeter")
            val redirectGridX: Int = msg.getS32Fast("RegionInfo", "RedirectGridX")
            val redirectGridY: Int = msg.getS32Fast("RegionInfo", "RedirectGridY")

            val regionp = gAgent.getRegion()
            regionp?.apply {
                setRegionNameAndZone(simName)
                setRegionFlags(regionFlags)
                setSimAccess(simAccess)
                setWaterHeight(waterHeight)
                setBillableFactor(billableFactor)
            }

            val godTools = LLFloaterReg.getTypedInstance<LLFloaterGodTools>("god_tools") ?: return

            if (gAgent.isGodlike()
                && LLFloaterReg.instanceVisible("god_tools")
                && godTools.mPanelRegionTools != null
                && godTools.mPanelObjectTools != null
            ) {
                val rtool = godTools.mPanelRegionTools!!
                godTools.mCurrentHost = host

                rtool.setSimName(simName)
                rtool.setEstateID(estateId)
                rtool.setParentEstateID(parentEstateId)
                rtool.setCheckFlags(regionFlags)
                rtool.setBillableFactor(billableFactor)
                rtool.setPricePerMeter(pricePerMeter)
                rtool.setRedirectGridX(redirectGridX)
                rtool.setRedirectGridY(redirectGridY)
                rtool.enableAllWidgets()

                val otool = godTools.mPanelObjectTools!!
                otool.setCheckFlags(regionFlags)
                otool.enableAllWidgets()

                if (regionp == null) {
                    rtool.setGridPosX(-1)
                    rtool.setGridPosY(-1)
                } else {
                    val globalPos = regionp.getPosGlobalFromRegion(LLVector3.zero)
                    val gridPosX = (globalPos.x / 256.0f).toInt()
                    val gridPosY = (globalPos.y / 256.0f).toInt()
                    rtool.setGridPosX(gridPosX)
                    rtool.setGridPosY(gridPosY)
                }
            }
        }
    }
}


class LLPanelRegionTools : LLPanel() {

    init {
        registerCommitCallback("RegionTools.ChangeAnything") { onChangeAnything() }
        registerCommitCallback("RegionTools.ChangePrelude") { onChangePrelude() }
        registerCommitCallback("RegionTools.BakeTerrain") { onBakeTerrain() }
        registerCommitCallback("RegionTools.RevertTerrain") { onRevertTerrain() }
        registerCommitCallback("RegionTools.SwapTerrain") { onSwapTerrain() }
        registerCommitCallback("RegionTools.Refresh") { onRefresh() }
        registerCommitCallback("RegionTools.ApplyChanges") { onApplyChanges() }
        registerCommitCallback("RegionTools.SelectRegion") { onSelectRegion() }
        registerCommitCallback("RegionTools.SaveState") { onSaveState() }
    }

    override fun postBuild(): Boolean {
        getChild<LLLineEditor>("region name").setKeystrokeCallback { onChangeSimName() }
        getChild<LLLineEditor>("region name").setPrevalidate(LLTextValidate::validateASCIIPrintableNoPipe)
        getChild<LLLineEditor>("estate").setPrevalidate(LLTextValidate::validatePositiveS32)
        getChild<LLLineEditor>("parentestate").apply {
            setPrevalidate(LLTextValidate::validatePositiveS32)
            setEnabled(false)
        }
        getChild<LLLineEditor>("gridposx").apply {
            setPrevalidate(LLTextValidate::validatePositiveS32)
            setEnabled(false)
        }
        getChild<LLLineEditor>("gridposy").apply {
            setPrevalidate(LLTextValidate::validatePositiveS32)
            setEnabled(false)
        }
        getChild<LLLineEditor>("redirectx").setPrevalidate(LLTextValidate::validatePositiveS32)
        getChild<LLLineEditor>("redirecty").setPrevalidate(LLTextValidate::validatePositiveS32)
        return true
    }

    open fun refresh() {}

    fun getSimName(): String = getChild<LLUICtrl>("region name").getValue()
    fun getEstateID(): UInt = getChild<LLUICtrl>("estate").getValue().asInteger().toUInt()
    fun getParentEstateID(): UInt = getChild<LLUICtrl>("parentestate").getValue().asInteger().toUInt()
    fun getRedirectGridX(): Int = getChild<LLUICtrl>("redirectx").getValue().asInteger()
    fun getRedirectGridY(): Int = getChild<LLUICtrl>("redirecty").getValue().asInteger()
    fun getGridPosX(): Int = getChild<LLUICtrl>("gridposx").getValue().asInteger()
    fun getGridPosY(): Int = getChild<LLUICtrl>("gridposy").getValue().asInteger()

    fun getRegionFlags(): ULong {
        var flags = 0UL
        flags = if (getChild<LLUICtrl>("check prelude").getValue().asBoolean())
            setPreludeFlags(flags) else unsetPreludeFlags(flags)
        if (getChild<LLUICtrl>("check fixed sun").getValue().asBoolean()) flags = flags or REGION_FLAGS_SUN_FIXED
        if (getChild<LLUICtrl>("check reset home").getValue().asBoolean()) flags = flags or REGION_FLAGS_RESET_HOME_ON_TELEPORT
        if (getChild<LLUICtrl>("check visible").getValue().asBoolean()) flags = flags or REGION_FLAGS_EXTERNALLY_VISIBLE
        if (getChild<LLUICtrl>("check damage").getValue().asBoolean()) flags = flags or REGION_FLAGS_ALLOW_DAMAGE
        if (getChild<LLUICtrl>("block terraform").getValue().asBoolean()) flags = flags or REGION_FLAGS_BLOCK_TERRAFORM
        if (getChild<LLUICtrl>("block dwell").getValue().asBoolean()) flags = flags or REGION_FLAGS_BLOCK_DWELL
        if (getChild<LLUICtrl>("is sandbox").getValue().asBoolean()) flags = flags or REGION_FLAGS_SANDBOX
        return flags
    }

    fun getRegionFlagsMask(): ULong {
        var flags = 0xFFFFFFFFFFFFFFFFUL
        flags = if (getChild<LLUICtrl>("check prelude").getValue().asBoolean())
            setPreludeFlags(flags) else unsetPreludeFlags(flags)
        if (!getChild<LLUICtrl>("check fixed sun").getValue().asBoolean()) flags = flags and REGION_FLAGS_SUN_FIXED.inv()
        if (!getChild<LLUICtrl>("check reset home").getValue().asBoolean()) flags = flags and REGION_FLAGS_RESET_HOME_ON_TELEPORT.inv()
        if (!getChild<LLUICtrl>("check visible").getValue().asBoolean()) flags = flags and REGION_FLAGS_EXTERNALLY_VISIBLE.inv()
        if (!getChild<LLUICtrl>("check damage").getValue().asBoolean()) flags = flags and REGION_FLAGS_ALLOW_DAMAGE.inv()
        if (!getChild<LLUICtrl>("block terraform").getValue().asBoolean()) flags = flags and REGION_FLAGS_BLOCK_TERRAFORM.inv()
        if (!getChild<LLUICtrl>("block dwell").getValue().asBoolean()) flags = flags and REGION_FLAGS_BLOCK_DWELL.inv()
        if (!getChild<LLUICtrl>("is sandbox").getValue().asBoolean()) flags = flags and REGION_FLAGS_SANDBOX.inv()
        return flags
    }

    fun getBillableFactor(): Float = getChild<LLUICtrl>("billable factor").getValue().asReal().toFloat()
    fun getPricePerMeter(): Int = getChild<LLUICtrl>("land cost").getValue().asInteger()

    fun setSimName(name: String) { getChild<LLUICtrl>("region name").setValue(name) }
    fun setEstateID(id: UInt) { getChild<LLUICtrl>("estate").setValue(id.toInt()) }
    fun setParentEstateID(id: UInt) { getChild<LLUICtrl>("parentestate").setValue(id.toInt()) }
    fun setGridPosX(pos: Int) { getChild<LLUICtrl>("gridposx").setValue(pos) }
    fun setGridPosY(pos: Int) { getChild<LLUICtrl>("gridposy").setValue(pos) }
    fun setRedirectGridX(pos: Int) { getChild<LLUICtrl>("redirectx").setValue(pos) }
    fun setRedirectGridY(pos: Int) { getChild<LLUICtrl>("redirecty").setValue(pos) }
    fun setBillableFactor(billableFactor: Float) { getChild<LLUICtrl>("billable factor").setValue(billableFactor) }
    fun setPricePerMeter(price: Int) { getChild<LLUICtrl>("land cost").setValue(price) }

    fun setCheckFlags(flags: ULong) {
        getChild<LLUICtrl>("check prelude").setValue(isPrelude(flags))
        getChild<LLUICtrl>("check fixed sun").setValue(isFlagSet(flags, REGION_FLAGS_SUN_FIXED))
        getChild<LLUICtrl>("check reset home").setValue(isFlagSet(flags, REGION_FLAGS_RESET_HOME_ON_TELEPORT))
        getChild<LLUICtrl>("check damage").setValue(isFlagSet(flags, REGION_FLAGS_ALLOW_DAMAGE))
        getChild<LLUICtrl>("check visible").setValue(isFlagSet(flags, REGION_FLAGS_EXTERNALLY_VISIBLE))
        getChild<LLUICtrl>("block terraform").setValue(isFlagSet(flags, REGION_FLAGS_BLOCK_TERRAFORM))
        getChild<LLUICtrl>("block dwell").setValue(isFlagSet(flags, REGION_FLAGS_BLOCK_DWELL))
        getChild<LLUICtrl>("is sandbox").setValue(isFlagSet(flags, REGION_FLAGS_SANDBOX))
    }

    fun computeRegionFlags(initialFlags: ULong): ULong {
        return (initialFlags and getRegionFlagsMask()) or getRegionFlags()
    }

    fun clearAllWidgets() {
        getChild<LLUICtrl>("region name").setValue("unknown")
        getChild<LLUICtrl>("region name").setFocus(false)
        listOf("check prelude", "check fixed sun", "check reset home", "check damage",
            "check visible", "block terraform", "block dwell", "is sandbox").forEach {
            getChild<LLUICtrl>(it).setValue(false)
            getChildView(it).setEnabled(false)
        }
        getChild<LLUICtrl>("billable factor").setValue(BILLABLE_FACTOR_DEFAULT)
        getChildView("billable factor").setEnabled(false)
        getChild<LLUICtrl>("land cost").setValue(PRICE_PER_METER_DEFAULT)
        getChildView("land cost").setEnabled(false)
        getChildView("Apply").setEnabled(false)
        getChildView("Bake Terrain").setEnabled(false)
        getChildView("Autosave now").setEnabled(false)
    }

    fun enableAllWidgets() {
        listOf("check prelude", "check fixed sun", "check reset home", "check damage",
            "block terraform", "block dwell", "is sandbox").forEach {
            getChildView(it).setEnabled(true)
        }
        getChildView("check visible").setEnabled(false)
        getChildView("billable factor").setEnabled(true)
        getChildView("land cost").setEnabled(true)
        getChildView("Apply").setEnabled(false)
        getChildView("Bake Terrain").setEnabled(true)
        getChildView("Autosave now").setEnabled(true)
    }

    fun onChangeAnything() {
        if (gAgent.isGodlike()) {
            getChildView("Apply").setEnabled(true)
        }
    }

    fun onChangePrelude() {
        if (getChild<LLUICtrl>("check prelude").getValue().asBoolean()) {
            getChild<LLUICtrl>("check fixed sun").setValue(true)
            getChild<LLUICtrl>("check reset home").setValue(true)
            onChangeAnything()
        }
    }

    private fun onChangeSimName() {
        if (gAgent.isGodlike()) {
            getChildView("Apply").setEnabled(true)
        }
    }

    fun onRefresh() {
        val godTools = LLFloaterReg.getTypedInstance<LLFloaterGodTools>("god_tools") ?: return
        if (gAgent.getRegion() != null && gAgent.isGodlike()) {
            godTools.sendRegionInfoRequest()
        }
    }

    fun onApplyChanges() {
        val godTools = LLFloaterReg.getTypedInstance<LLFloaterGodTools>("god_tools") ?: return
        if (gAgent.getRegion() != null && gAgent.isGodlike()) {
            getChildView("Apply").setEnabled(false)
            godTools.sendGodUpdateRegionInfo()
        }
    }

    fun onBakeTerrain() {
        LLPanelRequestTools.sendRequest("terrain", "bake", gAgent.getRegionHost())
    }

    fun onRevertTerrain() {
        LLPanelRequestTools.sendRequest("terrain", "revert", gAgent.getRegionHost())
    }

    fun onSwapTerrain() {
        LLPanelRequestTools.sendRequest("terrain", "swap", gAgent.getRegionHost())
    }

    fun onSelectRegion() {
        val regionp = LLWorld.getInstance().getRegionFromPosGlobal(gAgent.getPositionGlobal()) ?: return
        val northEast = LLVector3d(REGION_WIDTH_METERS, REGION_WIDTH_METERS, 0.0)
        LLViewerParcelMgr.getInstance().selectLand(
            regionp.getOriginGlobal(),
            regionp.getOriginGlobal() + northEast,
            false
        )
    }

    private fun onSaveState() {
        if (gAgent.isGodlike()) {
            TODO("APR: use JVM equivalent - send StateSave message via gMessageSystem")
        }
    }

    private fun updateCurrentRegion() {
        TODO("APR: use JVM equivalent - push UI values back to current viewer region")
    }
}


class LLPanelGridTools : LLPanel() {

    private var mKickMessage: String = ""

    init {
        registerCommitCallback("GridTools.FlushMapVisibilityCaches") { onClickFlushMapVisibilityCaches() }
    }

    override fun postBuild(): Boolean = true

    fun refresh() {}

    fun onClickFlushMapVisibilityCaches() {
        LLNotificationsUtil.add("FlushMapVisibilityCaches", LLSD(), LLSD()) { notification, response ->
            flushMapVisibilityCachesConfirm(notification, response)
        }
    }

    private fun flushMapVisibilityCachesConfirm(notification: LLSD, response: LLSD): Boolean {
        val option = LLNotificationsUtil.getSelectedOption(notification, response)
        if (option != 0) return false
        TODO("APR: use JVM equivalent - send EstateOwnerMessage/refreshmapvisibility via gMessageSystem")
    }
}


class LLPanelObjectTools : LLPanel() {

    private var mTargetAvatar: UUID = UUID(0, 0)
    private var mSimWideDeletesFlags: UInt = 0u

    init {
        registerCommitCallback("ObjectTools.ChangeAnything") { onChangeAnything() }
        registerCommitCallback("ObjectTools.DeletePublicOwnedBy") { onClickDeletePublicOwnedBy() }
        registerCommitCallback("ObjectTools.DeleteAllScriptedOwnedBy") { onClickDeleteAllScriptedOwnedBy() }
        registerCommitCallback("ObjectTools.DeleteAllOwnedBy") { onClickDeleteAllOwnedBy() }
        registerCommitCallback("ObjectTools.ApplyChanges") { onApplyChanges() }
        registerCommitCallback("ObjectTools.Set") { onClickSet() }
        registerCommitCallback("ObjectTools.GetTopColliders") { onGetTopColliders() }
        registerCommitCallback("ObjectTools.GetTopScripts") { onGetTopScripts() }
        registerCommitCallback("ObjectTools.GetScriptDigest") { onGetScriptDigest() }
    }

    override fun postBuild(): Boolean {
        refresh()
        return true
    }

    open fun refresh() {
        val regionp = gAgent.getRegion()
        if (regionp != null) {
            getChild<LLUICtrl>("region name").setValue(regionp.getName())
        }
    }

    fun setTargetAvatar(targetId: UUID) {
        mTargetAvatar = targetId
        if (targetId == UUID(0, 0)) {
            getChild<LLUICtrl>("target_avatar_name").setValue(getString("no_target"))
        }
    }

    fun computeRegionFlags(flags: ULong): ULong {
        var f = flags
        f = if (getChild<LLUICtrl>("disable scripts").getValue().asBoolean())
            f or REGION_FLAGS_SKIP_SCRIPTS else f and REGION_FLAGS_SKIP_SCRIPTS.inv()
        f = if (getChild<LLUICtrl>("disable collisions").getValue().asBoolean())
            f or REGION_FLAGS_SKIP_COLLISIONS else f and REGION_FLAGS_SKIP_COLLISIONS.inv()
        f = if (getChild<LLUICtrl>("disable physics").getValue().asBoolean())
            f or REGION_FLAGS_SKIP_PHYSICS else f and REGION_FLAGS_SKIP_PHYSICS.inv()
        return f
    }

    fun setCheckFlags(flags: ULong) {
        getChild<LLUICtrl>("disable scripts").setValue(isFlagSet(flags, REGION_FLAGS_SKIP_SCRIPTS))
        getChild<LLUICtrl>("disable collisions").setValue(isFlagSet(flags, REGION_FLAGS_SKIP_COLLISIONS))
        getChild<LLUICtrl>("disable physics").setValue(isFlagSet(flags, REGION_FLAGS_SKIP_PHYSICS))
    }

    fun clearAllWidgets() {
        getChild<LLUICtrl>("disable scripts").setValue(false)
        getChildView("disable scripts").setEnabled(false)
        getChildView("Apply").setEnabled(false)
        getChildView("Set Target").setEnabled(false)
        getChildView("Delete Target's Scripted Objects On Others Land").setEnabled(false)
        getChildView("Delete Target's Scripted Objects On *Any* Land").setEnabled(false)
        getChildView("Delete *ALL* Of Target's Objects").setEnabled(false)
    }

    fun enableAllWidgets() {
        getChildView("disable scripts").setEnabled(true)
        getChildView("Apply").setEnabled(false)
        getChildView("Set Target").setEnabled(true)
        getChildView("Delete Target's Scripted Objects On Others Land").setEnabled(true)
        getChildView("Delete Target's Scripted Objects On *Any* Land").setEnabled(true)
        getChildView("Delete *ALL* Of Target's Objects").setEnabled(true)
        getChildView("Get Top Colliders").setEnabled(true)
        getChildView("Get Top Scripts").setEnabled(true)
    }

    fun onChangeAnything() {
        if (gAgent.isGodlike()) {
            getChildView("Apply").setEnabled(true)
        }
    }

    fun onApplyChanges() {
        val godTools = LLFloaterReg.getTypedInstance<LLFloaterGodTools>("god_tools") ?: return
        if (gAgent.getRegion() != null && gAgent.isGodlike()) {
            getChildView("Apply").setEnabled(false)
            godTools.sendGodUpdateRegionInfo()
        }
    }

    fun onClickSet() {
        val button = findChild<LLButton>("Set Target")
        val rootFloater = gFloaterView.getParentFloater(this)
        val picker = LLFloaterAvatarPicker.show({ ids, names ->
            callbackAvatarID(ids, names)
        }, false, false, false, rootFloater?.getName() ?: "", button)
        picker?.let { rootFloater?.addDependentFloater(it) }
    }

    fun callbackAvatarID(ids: List<UUID>, names: List<LLAvatarName>) {
        if (ids.isEmpty() || names.isEmpty()) return
        mTargetAvatar = ids[0]
        getChild<LLUICtrl>("target_avatar_name").setValue(names[0].getCompleteName())
        refresh()
    }

    fun onClickDeletePublicOwnedBy() {
        if (mTargetAvatar != UUID(0, 0)) {
            mSimWideDeletesFlags = SWD_SCRIPTED_ONLY or SWD_OTHERS_LAND_ONLY
            val args = LLSD()
            args["AVATAR_NAME"] = getChild<LLUICtrl>("target_avatar_name").getValue().asString()
            val payload = LLSD()
            payload["avatar_id"] = mTargetAvatar
            payload["flags"] = mSimWideDeletesFlags.toInt()
            LLNotificationsUtil.add("GodDeleteAllScriptedPublicObjectsByUser", args, payload) { n, r ->
                callbackSimWideDeletes(n, r)
            }
        }
    }

    fun onClickDeleteAllScriptedOwnedBy() {
        if (mTargetAvatar != UUID(0, 0)) {
            mSimWideDeletesFlags = SWD_SCRIPTED_ONLY
            val args = LLSD()
            args["AVATAR_NAME"] = getChild<LLUICtrl>("target_avatar_name").getValue().asString()
            val payload = LLSD()
            payload["avatar_id"] = mTargetAvatar
            payload["flags"] = mSimWideDeletesFlags.toInt()
            LLNotificationsUtil.add("GodDeleteAllScriptedObjectsByUser", args, payload) { n, r ->
                callbackSimWideDeletes(n, r)
            }
        }
    }

    fun onClickDeleteAllOwnedBy() {
        if (mTargetAvatar != UUID(0, 0)) {
            mSimWideDeletesFlags = 0u
            val args = LLSD()
            args["AVATAR_NAME"] = getChild<LLUICtrl>("target_avatar_name").getValue().asString()
            val payload = LLSD()
            payload["avatar_id"] = mTargetAvatar
            payload["flags"] = mSimWideDeletesFlags.toInt()
            LLNotificationsUtil.add("GodDeleteAllObjectsByUser", args, payload) { n, r ->
                callbackSimWideDeletes(n, r)
            }
        }
    }

    fun onGetTopColliders() {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterTopObjects>("top_objects") ?: return
        if (gAgent.isGodlike()) {
            LLFloaterReg.showInstance("top_objects")
            LLFloaterTopObjects.setMode(STAT_REPORT_TOP_COLLIDERS)
            instance.onRefresh()
        }
    }

    fun onGetTopScripts() {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterTopObjects>("top_objects") ?: return
        if (gAgent.isGodlike()) {
            LLFloaterReg.showInstance("top_objects")
            LLFloaterTopObjects.setMode(STAT_REPORT_TOP_SCRIPTS)
            instance.onRefresh()
        }
    }

    fun onGetScriptDigest() {
        if (gAgent.isGodlike()) {
            LLPanelRequestTools.sendRequest("scriptdigest", "0", gAgent.getRegionHost())
        }
    }

    companion object {
        fun callbackSimWideDeletes(notification: LLSD, response: LLSD): Boolean {
            val option = LLNotificationsUtil.getSelectedOption(notification, response)
            if (option == 0) {
                val avatarId = notification["payload"]["avatar_id"].asUUID()
                if (avatarId != UUID(0, 0)) {
                    sendSimWideDeletes(avatarId, notification["payload"]["flags"].asInteger().toUInt())
                }
            }
            return false
        }

        fun onClickSetBySelection(panel: LLPanelObjectTools?) {
            panel ?: return
            val node = LLSelectMgr.getInstance().getSelection().getFirstRootNode(null, true) ?: return
            val ownerId = UUID(0, 0)
            val ownerName = ""
            LLSelectMgr.getInstance().selectGetOwner(ownerId, ownerName)
            panel.mTargetAvatar = ownerId
            val name = LLTrans.getString("GodToolsObjectOwnedBy")
                .replace("[OBJECT]", node.mName)
                .replace("[OWNER]", ownerName)
            panel.getChild<LLUICtrl>("target_avatar_name").setValue(name)
        }
    }
}


class LLPanelRequestTools : LLPanel() {

    init {
        registerCommitCallback("GodTools.Request") { onClickRequest() }
    }

    override fun postBuild(): Boolean {
        refresh()
        return true
    }

    fun refresh() {
        val buffer = getChild<LLUICtrl>("destination").getValue().asString()
        val list = childGetListInterface("destination") ?: return
        val lastItem = list.getItemCount()
        if (lastItem >= 3) {
            list.selectItemRange(2, lastItem)
            list.operateOnSelection(LLCtrlListInterface.OP_DELETE)
        }
        for (regionp in LLWorld.getInstance().getRegionList()) {
            val name = regionp.getName()
            if (name.isNotEmpty()) {
                list.addSimpleElement(name)
            }
        }
        if (buffer.isNotEmpty()) {
            list.selectByValue(buffer)
        } else {
            list.operateOnSelection(LLCtrlListInterface.OP_DESELECT)
        }
    }

    private fun onClickRequest() {
        val dest = getChild<LLUICtrl>("destination").getValue().asString()
        when (dest) {
            SELECTION -> {
                var req = getChild<LLUICtrl>("request").getValue().asString()
                req = req.substringBefore(" ")
                val param = getChild<LLUICtrl>("parameter").getValue().asString()
                LLSelectMgr.getInstance().sendGodlikeRequest(req, param)
            }
            AGENT_REGION -> sendRequest(gAgent.getRegionHost())
            else -> {
                for (regionp in LLWorld.getInstance().getRegionList()) {
                    if (dest == regionp.getName()) {
                        sendRequest(regionp.getHost())
                    }
                }
            }
        }
    }

    private fun sendRequest(host: LLHost) {
        val req = getChild<LLUICtrl>("request").getValue().asString()
        if (req == "terrain download") {
            TODO("APR: use JVM equivalent - request terrain file via gXferManager")
        } else {
            val trimmedReq = req.substringBefore(" ")
            sendRequest(trimmedReq, getChild<LLUICtrl>("parameter").getValue().asString(), host)
        }
    }

    companion object {
        fun sendRequest(request: String, parameter: String, host: LLHost) {
            TODO("APR: use JVM equivalent - send GodlikeMessage via gMessageSystem to $host")
        }
    }
}


fun sendSimWideDeletes(ownerId: UUID, flags: UInt) {
    TODO("APR: use JVM equivalent - send SimWideDeletes message via gMessageSystem")
}
