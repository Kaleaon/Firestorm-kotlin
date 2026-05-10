package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Panel
import com.firestorm.llui.TabContainer
import com.firestorm.llui.LineEditor
import com.firestorm.llui.UICtrl
import com.firestorm.llmessage.MessageSystem
import com.firestorm.llcommon.LLUUID

// Region-flag bit constants (mirror llregionflags.h).
private const val REGION_FLAGS_ALLOW_DAMAGE            = 0x0000000000000001uL
private const val REGION_FLAGS_EXTERNALLY_VISIBLE      = 0x0000000000000002uL
private const val REGION_FLAGS_RESET_HOME_ON_TELEPORT  = 0x0000000000000400uL
private const val REGION_FLAGS_SUN_FIXED               = 0x0000000000000800uL
private const val REGION_FLAGS_BLOCK_TERRAFORM         = 0x0000000000020000uL
private const val REGION_FLAGS_SANDBOX                 = 0x0000000000040000uL
private const val REGION_FLAGS_BLOCK_DWELL             = 0x0000000001000000uL
private const val REGION_FLAGS_SKIP_SCRIPTS            = 0x0000000004000000uL
private const val REGION_FLAGS_SKIP_COLLISIONS         = 0x0000000008000000uL
private const val REGION_FLAGS_SKIP_PHYSICS            = 0x0000000010000000uL

// Sim-wide-delete flag constants.
private const val SWD_SCRIPTED_ONLY    = 0x00000001u
private const val SWD_OTHERS_LAND_ONLY = 0x00000002u

private const val SECONDS_BETWEEN_UPDATE_REQUESTS = 5.0f
private const val REGION_WIDTH_METERS             = 256.0

// Godlike tool floater with panels for Grid, Region, Objects, and Request tools.
class FloaterGodTools(seed: Any) : Floater(seed) {

    enum class GodPanel { GRID, REGION, OBJECT, REQUEST }

    var panelRegionTools: PanelRegionTools? = null
    var panelObjectTools: PanelObjectTools? = null

    private var currentHost: Any?  = null   // LLHost
    private var updateTimer: Float = 0f     // elapsed seconds since last request

    override fun postBuild(): Boolean {
        panelRegionTools = PanelRegionTools()
        panelObjectTools = PanelObjectTools()
        sendRegionInfoRequest()
        findChild<TabContainer>("GodTools Tabs")?.selectTabByName("region")
        return true
    }

    override fun onOpen(key: Any) {
        center()
        setFocus(true)
        panelObjectTools?.setTargetAvatar(LLUUID.NULL)

        if (TODO<Any?>("APR: gAgent.getRegionHost()") != currentHost) {
            sendRegionInfoRequest()
        }
    }

    override fun draw() {
        if (currentHost == null) {
            updateTimer += TODO("APR: frame delta seconds")
            if (updateTimer > SECONDS_BETWEEN_UPDATE_REQUESTS) {
                sendRegionInfoRequest()
            }
        } else if (TODO<Any?>("APR: gAgent.getRegionHost()") != currentHost) {
            sendRegionInfoRequest()
        }
        TODO("APR: Floater.draw()")
    }

    fun showPanel(panelName: String) {
        findChild<TabContainer>("GodTools Tabs")?.selectTabByName(panelName)
        TODO("APR: openFloater()")
        TODO("APR: getCurrentPanel().setFocus(true)")
    }

    fun sendRegionInfoRequest() {
        panelRegionTools?.clearAllWidgets()
        panelObjectTools?.clearAllWidgets()
        currentHost  = null
        updateTimer  = 0f

        TODO("APR: send RequestRegionInfo message via gMessageSystem with agent/session IDs")
    }

    fun sendGodUpdateRegionInfo() {
        val region = TODO<Any?>("APR: gAgent.getRegion()")
        if (!TODO<Boolean>("APR: gAgent.isGodlike()") || panelRegionTools == null || region == null) return
        if (TODO<Any?>("APR: gAgent.getRegionHost()") != currentHost) return

        val regionFlags = computeRegionFlags()

        TODO("APR: send GodUpdateRegionInfo message: " +
             "simName, estateId, parentEstateId, regionFlags(U32 legacy), billableFactor, " +
             "pricePerMeter, redirectGridX, redirectGridY, RegionInfo2.RegionFlagsExtended(U64)")
    }

    private fun computeRegionFlags(): ULong {
        var flags = TODO<ULong>("APR: gAgent.getRegion().getRegionFlags()")
        panelRegionTools?.let { flags = it.computeRegionFlags(flags) }
        panelObjectTools?.let { flags = it.computeRegionFlags(flags) }
        return flags
    }

    // Inlined stubs for inherited Floater members used above.
    @Suppress("UNCHECKED_CAST")
    private fun <T> findChild(name: String): T? = null
    private fun center()         { TODO("APR: Floater.center()") }
    private fun setFocus(b: Boolean) { TODO("APR: Floater.setFocus(b)") }

    companion object {
        fun refreshAll() {
            val godTools = TODO<FloaterGodTools?>("APR: FloaterReg::getTypedInstance<FloaterGodTools>(\"god_tools\")")
            godTools ?: return
            if (TODO<Any?>("APR: gAgent.getRegionHost()") != godTools.currentHost) {
                godTools.sendRegionInfoRequest()
            }
        }

        fun processRegionInfo(msg: MessageSystem) {
            val host             = TODO<Any?>("APR: msg.getSender()")
            val simName          = TODO<String>("APR: msg.getString(\"RegionInfo\", \"SimName\")")
            val estateId         = TODO<UInt>("APR: msg.getU32(\"RegionInfo\", \"EstateID\")")
            val parentEstateId   = TODO<UInt>("APR: msg.getU32(\"RegionInfo\", \"ParentEstateID\")")
            val simAccess        = TODO<UByte>("APR: msg.getU8(\"RegionInfo\", \"SimAccess\")")
            val agentLimit       = TODO<UByte>("APR: msg.getU8(\"RegionInfo\", \"MaxAgents\")")
            val objectBonusFactor = TODO<Float>("APR: msg.getF32(\"RegionInfo\", \"ObjectBonusFactor\")")
            val billableFactor   = TODO<Float>("APR: msg.getF32(\"RegionInfo\", \"BillableFactor\")")
            val waterHeight      = TODO<Float>("APR: msg.getF32(\"RegionInfo\", \"WaterHeight\")")

            // Extended flags block supersedes the legacy U32 field when present.
            val regionFlags: ULong = if (TODO<Boolean>("APR: msg.has(\"RegionInfo3\")")) {
                TODO("APR: msg.getU64(\"RegionInfo3\", \"RegionFlagsExtended\")")
            } else {
                TODO<UInt>("APR: msg.getU32(\"RegionInfo\", \"RegionFlags\")").toULong()
            }

            if (TODO<Boolean>("APR: msg.has(\"RegionInfo5\")")) {
                // Chat-range data present – log it but it requires no action here.
                TODO("APR: read and log chat whisper/normal/shout ranges and flags")
            }

            val agentRegionHost = TODO<Any?>("APR: gAgent.getRegionHost()")
            if (host != agentRegionHost) {
                TODO("APR: LLWorld::getInstance().waterHeightRegionInfo(simName, waterHeight)")
                return
            }

            val terrainRaiseLimit  = TODO<Float>("APR: msg.getF32(\"RegionInfo\", \"TerrainRaiseLimit\")")
            val terrainLowerLimit  = TODO<Float>("APR: msg.getF32(\"RegionInfo\", \"TerrainLowerLimit\")")
            val pricePerMeter      = TODO<Int>("APR: msg.getS32(\"RegionInfo\", \"PricePerMeter\")")
            val redirectGridX      = TODO<Int>("APR: msg.getS32(\"RegionInfo\", \"RedirectGridX\")")
            val redirectGridY      = TODO<Int>("APR: msg.getS32(\"RegionInfo\", \"RedirectGridY\")")

            val regionp = TODO<Any?>("APR: gAgent.getRegion()")
            regionp?.let {
                TODO("APR: regionp.setRegionNameAndZone(simName)")
                TODO("APR: regionp.setRegionFlags(regionFlags)")
                TODO("APR: regionp.setSimAccess(simAccess)")
                TODO("APR: regionp.setWaterHeight(waterHeight)")
                TODO("APR: regionp.setBillableFactor(billableFactor)")
            }

            val godTools = TODO<FloaterGodTools?>("APR: FloaterReg::getTypedInstance<FloaterGodTools>(\"god_tools\")")
            godTools ?: return

            val isGodVisible = TODO<Boolean>("APR: gAgent.isGodlike() && FloaterReg::instanceVisible(\"god_tools\")")
            if (isGodVisible && godTools.panelRegionTools != null && godTools.panelObjectTools != null) {
                val rtool = godTools.panelRegionTools!!
                godTools.currentHost = host

                rtool.setSimName(simName)
                rtool.setEstateId(estateId)
                rtool.setParentEstateId(parentEstateId)
                rtool.setCheckFlags(regionFlags)
                rtool.setBillableFactor(billableFactor)
                rtool.setPricePerMeter(pricePerMeter)
                rtool.setRedirectGridX(redirectGridX)
                rtool.setRedirectGridY(redirectGridY)
                rtool.enableAllWidgets()

                val otool = godTools.panelObjectTools!!
                otool.setCheckFlags(regionFlags)
                otool.enableAllWidgets()

                if (regionp == null) {
                    rtool.setGridPosX(-1)
                    rtool.setGridPosY(-1)
                } else {
                    val globalPos = TODO<DoubleArray>("APR: regionp.getPosGlobalFromRegion(Vector3.ZERO)")
                    rtool.setGridPosX((globalPos[0] / 256.0).toInt())
                    rtool.setGridPosY((globalPos[1] / 256.0).toInt())
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// PanelRegionTools
// -------------------------------------------------------------------------

class PanelRegionTools : Panel() {

    override fun postBuild(): Boolean {
        setChildKeystrokeCallback("region name") { onChangeSimName() }
        setChildPrevalidate("region name", "ascii_printable_no_pipe")
        setChildPrevalidate("estate",       "positive_s32")
        setChildPrevalidate("parentestate", "positive_s32")
        setChildEnabled("parentestate", false)
        setChildPrevalidate("gridposx", "positive_s32")
        setChildEnabled("gridposx", false)
        setChildPrevalidate("gridposy", "positive_s32")
        setChildEnabled("gridposy", false)
        setChildPrevalidate("redirectx", "positive_s32")
        setChildPrevalidate("redirecty", "positive_s32")
        return true
    }

    override fun refresh() {}

    fun clearAllWidgets() {
        setChildValue("region name",   "unknown")
        setChildFocus("region name",   false)
        setChildValue("check prelude",   false);  setChildEnabled("check prelude",   false)
        setChildValue("check fixed sun", false);  setChildEnabled("check fixed sun", false)
        setChildValue("check reset home",false);  setChildEnabled("check reset home",false)
        setChildValue("check damage",    false);  setChildEnabled("check damage",    false)
        setChildValue("check visible",   false);  setChildEnabled("check visible",   false)
        setChildValue("block terraform", false);  setChildEnabled("block terraform", false)
        setChildValue("block dwell",     false);  setChildEnabled("block dwell",     false)
        setChildValue("is sandbox",      false);  setChildEnabled("is sandbox",      false)
        setChildValue("billable factor", BILLABLE_FACTOR_DEFAULT)
        setChildEnabled("billable factor", false)
        setChildValue("land cost",       PRICE_PER_METER_DEFAULT)
        setChildEnabled("land cost",     false)
        setChildEnabled("Apply",         false)
        setChildEnabled("Bake Terrain",  false)
        setChildEnabled("Autosave now",  false)
    }

    fun enableAllWidgets() {
        setChildEnabled("check prelude",   true)
        setChildEnabled("check fixed sun", true)
        setChildEnabled("check reset home",true)
        setChildEnabled("check damage",    true)
        setChildEnabled("check visible",   false)  // controlled via estate, not directly
        setChildEnabled("block terraform", true)
        setChildEnabled("block dwell",     true)
        setChildEnabled("is sandbox",      true)
        setChildEnabled("billable factor", true)
        setChildEnabled("land cost",       true)
        setChildEnabled("Apply",           false)  // only enabled after a change
        setChildEnabled("Bake Terrain",    true)
        setChildEnabled("Autosave now",    true)
    }

    // ---- Getters ----

    fun getSimName(): String    = getChildValue("region name") as? String ?: ""
    fun getEstateId(): UInt     = (getChildInt("estate")).toUInt()
    fun getParentEstateId(): UInt = (getChildInt("parentestate")).toUInt()
    fun getGridPosX(): Int      = getChildInt("gridposx")
    fun getGridPosY(): Int      = getChildInt("gridposy")
    fun getRedirectGridX(): Int = getChildInt("redirectx")
    fun getRedirectGridY(): Int = getChildInt("redirecty")
    fun getBillableFactor(): Float = (getChildValue("billable factor") as? Number)?.toFloat() ?: BILLABLE_FACTOR_DEFAULT
    fun getPricePerMeter(): Int    = getChildInt("land cost")

    fun getRegionFlags(): ULong {
        var flags = 0uL
        if (isPrelude()) {
            flags = setPreludeFlags(flags)
        } else {
            flags = unsetPreludeFlags(flags)
        }
        if (getChildBool("check fixed sun"))   flags = flags or REGION_FLAGS_SUN_FIXED
        if (getChildBool("check reset home"))  flags = flags or REGION_FLAGS_RESET_HOME_ON_TELEPORT
        if (getChildBool("check visible"))     flags = flags or REGION_FLAGS_EXTERNALLY_VISIBLE
        if (getChildBool("check damage"))      flags = flags or REGION_FLAGS_ALLOW_DAMAGE
        if (getChildBool("block terraform"))   flags = flags or REGION_FLAGS_BLOCK_TERRAFORM
        if (getChildBool("block dwell"))       flags = flags or REGION_FLAGS_BLOCK_DWELL
        if (getChildBool("is sandbox"))        flags = flags or REGION_FLAGS_SANDBOX
        return flags
    }

    fun getRegionFlagsMask(): ULong {
        var flags = 0xFFFFFFFFFFFFFFFFuL
        if (isPrelude()) {
            flags = setPreludeFlags(flags)
        } else {
            flags = unsetPreludeFlags(flags)
        }
        if (!getChildBool("check fixed sun"))   flags = flags and REGION_FLAGS_SUN_FIXED.inv()
        if (!getChildBool("check reset home"))  flags = flags and REGION_FLAGS_RESET_HOME_ON_TELEPORT.inv()
        if (!getChildBool("check visible"))     flags = flags and REGION_FLAGS_EXTERNALLY_VISIBLE.inv()
        if (!getChildBool("check damage"))      flags = flags and REGION_FLAGS_ALLOW_DAMAGE.inv()
        if (!getChildBool("block terraform"))   flags = flags and REGION_FLAGS_BLOCK_TERRAFORM.inv()
        if (!getChildBool("block dwell"))       flags = flags and REGION_FLAGS_BLOCK_DWELL.inv()
        if (!getChildBool("is sandbox"))        flags = flags and REGION_FLAGS_SANDBOX.inv()
        return flags
    }

    fun computeRegionFlags(initial: ULong): ULong = (initial and getRegionFlagsMask()) or getRegionFlags()

    // ---- Setters ----

    fun setSimName(name: String)            { setChildValue("region name",    name) }
    fun setEstateId(id: UInt)               { setChildValue("estate",         id.toInt()) }
    fun setParentEstateId(id: UInt)         { setChildValue("parentestate",   id.toInt()) }
    fun setGridPosX(pos: Int)               { setChildValue("gridposx",       pos) }
    fun setGridPosY(pos: Int)               { setChildValue("gridposy",       pos) }
    fun setRedirectGridX(pos: Int)          { setChildValue("redirectx",      pos) }
    fun setRedirectGridY(pos: Int)          { setChildValue("redirecty",      pos) }
    fun setBillableFactor(f: Float)         { setChildValue("billable factor", f) }
    fun setPricePerMeter(price: Int)        { setChildValue("land cost",      price) }

    fun setCheckFlags(flags: ULong) {
        setChildValue("check prelude",   isPrelude(flags))
        setChildValue("check fixed sun", flags.isFlagSet(REGION_FLAGS_SUN_FIXED))
        setChildValue("check reset home",flags.isFlagSet(REGION_FLAGS_RESET_HOME_ON_TELEPORT))
        setChildValue("check damage",    flags.isFlagSet(REGION_FLAGS_ALLOW_DAMAGE))
        setChildValue("check visible",   flags.isFlagSet(REGION_FLAGS_EXTERNALLY_VISIBLE))
        setChildValue("block terraform", flags.isFlagSet(REGION_FLAGS_BLOCK_TERRAFORM))
        setChildValue("block dwell",     flags.isFlagSet(REGION_FLAGS_BLOCK_DWELL))
        setChildValue("is sandbox",      flags.isFlagSet(REGION_FLAGS_SANDBOX))
    }

    // ---- Event handlers ----

    fun onChangeAnything() {
        if (TODO<Boolean>("APR: gAgent.isGodlike()")) {
            setChildEnabled("Apply", true)
        }
    }

    fun onChangePrelude() {
        if (getChildBool("check prelude")) {
            setChildValue("check fixed sun",  true)
            setChildValue("check reset home", true)
            onChangeAnything()
        }
    }

    private fun onChangeSimName() {
        if (TODO<Boolean>("APR: gAgent.isGodlike()")) setChildEnabled("Apply", true)
    }

    fun onRefresh() {
        val godTools = TODO<FloaterGodTools?>("APR: FloaterReg::getTypedInstance<FloaterGodTools>(\"god_tools\")")
        godTools ?: return
        if (TODO<Any?>("APR: gAgent.getRegion()") != null && TODO<Boolean>("APR: gAgent.isGodlike()")) {
            godTools.sendRegionInfoRequest()
        }
    }

    fun onApplyChanges() {
        val godTools = TODO<FloaterGodTools?>("APR: FloaterReg::getTypedInstance<FloaterGodTools>(\"god_tools\")")
        godTools ?: return
        if (TODO<Any?>("APR: gAgent.getRegion()") != null && TODO<Boolean>("APR: gAgent.isGodlike()")) {
            setChildEnabled("Apply", false)
            godTools.sendGodUpdateRegionInfo()
        }
    }

    fun onBakeTerrain()   { PanelRequestTools.sendRequest("terrain", "bake",   TODO("APR: gAgent.getRegionHost()")) }
    fun onRevertTerrain() { PanelRequestTools.sendRequest("terrain", "revert", TODO("APR: gAgent.getRegionHost()")) }
    fun onSwapTerrain()   { PanelRequestTools.sendRequest("terrain", "swap",   TODO("APR: gAgent.getRegionHost()")) }

    fun onSelectRegion() {
        val regionp = TODO<Any?>("APR: LLWorld::getInstance().getRegionFromPosGlobal(gAgent.getPositionGlobal())")
        regionp ?: return
        TODO("APR: LLViewerParcelMgr::getInstance().selectLand(origin, origin + (256,256,0), false)")
    }

    fun onSaveState() {
        if (TODO<Boolean>("APR: gAgent.isGodlike()")) {
            TODO("APR: send StateSave message with blank filename via gMessageSystem")
        }
    }

    // ---- Helpers ----

    private fun isPrelude(): Boolean = getChildBool("check prelude")
    private fun isPrelude(flags: ULong): Boolean = TODO("APR: is_prelude(flags)")
    private fun setPreludeFlags(flags: ULong): ULong  = TODO("APR: set_prelude_flags(flags)")
    private fun unsetPreludeFlags(flags: ULong): ULong = TODO("APR: unset_prelude_flags(flags)")
    private fun ULong.isFlagSet(flag: ULong): Boolean = (this and flag) != 0uL

    private fun setChildKeystrokeCallback(name: String, cb: () -> Unit) {
        TODO("APR: getChild<LineEditor>(name).setKeystrokeCallback { cb() }")
    }
    private fun setChildPrevalidate(name: String, rule: String) {
        TODO("APR: getChild<LineEditor>(name).setPrevalidate(LLTextValidate::$rule)")
    }
    private fun setChildFocus(name: String, f: Boolean) {
        TODO("APR: getChildView(name).setFocus(f)")
    }
    private fun setChildEnabled(name: String, e: Boolean) {
        TODO("APR: getChildView(name).setEnabled(e)")
    }
    private fun setChildValue(name: String, v: Any) {
        TODO("APR: getChild<UICtrl>(name).setValue(v)")
    }
    private fun getChildValue(name: String): Any = TODO("APR: getChild<UICtrl>(name).getValue()")
    private fun getChildBool(name: String): Boolean = TODO("APR: getChild<UICtrl>(name).getValue().asBoolean()")
    private fun getChildInt(name: String): Int = TODO("APR: getChild<UICtrl>(name).getValue().asInteger()")

    companion object {
        const val BILLABLE_FACTOR_DEFAULT  = 1.0f
        const val PRICE_PER_METER_DEFAULT  = 1.0f
    }
}

// -------------------------------------------------------------------------
// PanelGridTools
// -------------------------------------------------------------------------

class PanelGridTools : Panel() {

    private var kickMessage: String = ""

    override fun postBuild(): Boolean = true

    override fun refresh() {}

    fun onClickFlushMapVisibilityCaches() {
        TODO("APR: LLNotificationsUtil::add(\"FlushMapVisibilityCaches\", LLSD(), LLSD(), ::flushMapVisibilityCachesConfirm)")
    }

    companion object {
        fun flushMapVisibilityCachesConfirm(notification: Any, response: Any): Boolean {
            val option = TODO<Int>("APR: LLNotificationsUtil::getSelectedOption(notification, response)")
            if (option != 0) return false
            TODO("APR: send EstateOwnerMessage with method=\"refreshmapvisibility\", " +
                 "parameter=gAgent.getID().asString() via gMessageSystem")
            return false
        }
    }
}

// -------------------------------------------------------------------------
// PanelObjectTools
// -------------------------------------------------------------------------

class PanelObjectTools : Panel() {

    private var targetAvatar: LLUUID = LLUUID.NULL
    private var simWideDeletesFlags: UInt = 0u

    override fun postBuild(): Boolean {
        refresh()
        return true
    }

    override fun refresh() {
        val region = TODO<Any?>("APR: gAgent.getRegion()")
        if (region != null) {
            setChildValue("region name", TODO("APR: region.getName()"))
        }
    }

    fun setTargetAvatar(targetId: LLUUID) {
        targetAvatar = targetId
        if (targetId == LLUUID.NULL) {
            setChildValue("target_avatar_name", getString("no_target"))
        }
    }

    fun setCheckFlags(flags: ULong) {
        setChildValue("disable scripts",    flags.isFlagSet(REGION_FLAGS_SKIP_SCRIPTS))
        setChildValue("disable collisions", flags.isFlagSet(REGION_FLAGS_SKIP_COLLISIONS))
        setChildValue("disable physics",    flags.isFlagSet(REGION_FLAGS_SKIP_PHYSICS))
    }

    fun computeRegionFlags(initial: ULong): ULong {
        var flags = initial
        flags = if (getChildBool("disable scripts"))
            flags or REGION_FLAGS_SKIP_SCRIPTS    else flags and REGION_FLAGS_SKIP_SCRIPTS.inv()
        flags = if (getChildBool("disable collisions"))
            flags or REGION_FLAGS_SKIP_COLLISIONS else flags and REGION_FLAGS_SKIP_COLLISIONS.inv()
        flags = if (getChildBool("disable physics"))
            flags or REGION_FLAGS_SKIP_PHYSICS    else flags and REGION_FLAGS_SKIP_PHYSICS.inv()
        return flags
    }

    fun clearAllWidgets() {
        setChildValue("disable scripts", false);   setChildEnabled("disable scripts", false)
        setChildEnabled("Apply",                                                       false)
        setChildEnabled("Set Target",                                                  false)
        setChildEnabled("Delete Target's Scripted Objects On Others Land",             false)
        setChildEnabled("Delete Target's Scripted Objects On *Any* Land",              false)
        setChildEnabled("Delete *ALL* Of Target's Objects",                            false)
    }

    fun enableAllWidgets() {
        setChildEnabled("disable scripts",                                             true)
        setChildEnabled("Apply",                                                       false)
        setChildEnabled("Set Target",                                                  true)
        setChildEnabled("Delete Target's Scripted Objects On Others Land",             true)
        setChildEnabled("Delete Target's Scripted Objects On *Any* Land",              true)
        setChildEnabled("Delete *ALL* Of Target's Objects",                            true)
        setChildEnabled("Get Top Colliders",                                           true)
        setChildEnabled("Get Top Scripts",                                             true)
    }

    fun onChangeAnything() {
        if (TODO<Boolean>("APR: gAgent.isGodlike()")) setChildEnabled("Apply", true)
    }

    fun onApplyChanges() {
        val godTools = TODO<FloaterGodTools?>("APR: FloaterReg::getTypedInstance<FloaterGodTools>(\"god_tools\")")
        godTools ?: return
        if (TODO<Any?>("APR: gAgent.getRegion()") != null && TODO<Boolean>("APR: gAgent.isGodlike()")) {
            setChildEnabled("Apply", false)
            godTools.sendGodUpdateRegionInfo()
        }
    }

    fun onClickSet() {
        TODO("APR: show LLFloaterAvatarPicker, callback = ::callbackAvatarId")
    }

    fun callbackAvatarId(ids: List<LLUUID>, names: List<Any>) {
        if (ids.isEmpty() || names.isEmpty()) return
        targetAvatar = ids[0]
        setChildValue("target_avatar_name", TODO("APR: names[0].getCompleteName()"))
        refresh()
    }

    fun onClickDeletePublicOwnedBy() {
        if (targetAvatar == LLUUID.NULL) return
        simWideDeletesFlags = SWD_SCRIPTED_ONLY or SWD_OTHERS_LAND_ONLY
        val avatarName = getChildValue("target_avatar_name") as? String ?: ""
        TODO("APR: LLNotificationsUtil::add(\"GodDeleteAllScriptedPublicObjectsByUser\", " +
             "args=[AVATAR_NAME=avatarName], payload=[avatar_id=targetAvatar, flags=simWideDeletesFlags], " +
             "callback=::callbackSimWideDeletes)")
    }

    fun onClickDeleteAllScriptedOwnedBy() {
        if (targetAvatar == LLUUID.NULL) return
        simWideDeletesFlags = SWD_SCRIPTED_ONLY
        TODO("APR: LLNotificationsUtil::add(\"GodDeleteAllScriptedObjectsByUser\", ..., ::callbackSimWideDeletes)")
    }

    fun onClickDeleteAllOwnedBy() {
        if (targetAvatar == LLUUID.NULL) return
        simWideDeletesFlags = 0u
        TODO("APR: LLNotificationsUtil::add(\"GodDeleteAllObjectsByUser\", ..., ::callbackSimWideDeletes)")
    }

    fun onGetTopColliders() {
        if (TODO<Boolean>("APR: gAgent.isGodlike()")) {
            TODO("APR: FloaterReg::showInstance(\"top_objects\"); FloaterTopObjects.setMode(COLLIDERS); instance.onRefresh()")
        }
    }

    fun onGetTopScripts() {
        if (TODO<Boolean>("APR: gAgent.isGodlike()")) {
            TODO("APR: FloaterReg::showInstance(\"top_objects\"); FloaterTopObjects.setMode(SCRIPTS); instance.onRefresh()")
        }
    }

    fun onGetScriptDigest() {
        if (TODO<Boolean>("APR: gAgent.isGodlike()")) {
            PanelRequestTools.sendRequest("scriptdigest", "0", TODO("APR: gAgent.getRegionHost()"))
        }
    }

    // ---- Helpers ----

    private fun ULong.isFlagSet(flag: ULong): Boolean = (this and flag) != 0uL
    private fun getString(key: String): String = TODO("APR: XUI string lookup for key=$key")
    private fun setChildEnabled(name: String, e: Boolean) { TODO("APR: getChildView(name).setEnabled(e)") }
    private fun setChildValue(name: String, v: Any) { TODO("APR: getChild<UICtrl>(name).setValue(v)") }
    private fun getChildValue(name: String): Any = TODO("APR: getChild<UICtrl>(name).getValue()")
    private fun getChildBool(name: String): Boolean = TODO("APR: getChild<UICtrl>(name).getValue().asBoolean()")

    companion object {
        fun callbackSimWideDeletes(notification: Any, response: Any): Boolean {
            val option   = TODO<Int>("APR: LLNotificationsUtil::getSelectedOption(notification, response)")
            if (option == 0) {
                val avatarId = TODO<LLUUID>("APR: notification[\"payload\"][\"avatar_id\"].asUUID()")
                val flags    = TODO<UInt>("APR: notification[\"payload\"][\"flags\"].asInteger().toUInt()")
                if (avatarId != LLUUID.NULL) sendSimWideDeletes(avatarId, flags)
            }
            return false
        }
    }
}

// -------------------------------------------------------------------------
// PanelRequestTools
// -------------------------------------------------------------------------

class PanelRequestTools : Panel() {

    companion object {
        private const val SELECTION    = "Selection"
        private const val AGENT_REGION = "Agent Region"

        fun sendRequest(request: String, parameter: String, host: Any?) {
            TODO("APR: send GodlikeMessage with Method=$request, Parameter=$parameter to host=$host")
        }
    }

    override fun postBuild(): Boolean {
        refresh()
        return true
    }

    override fun refresh() {
        val buffer = getChildValue("destination") as? String ?: ""
        TODO("APR: rebuild destination combo: keep first 2 items (Selection, Agent Region), " +
             "then add all LLWorld region names; restore selection to buffer if non-empty")
    }

    fun onClickRequest() {
        val dest = (getChildValue("destination") as? String) ?: ""
        when (dest) {
            SELECTION -> {
                var req   = (getChildValue("request") as? String) ?: ""
                req       = req.substringBefore(' ')
                val param = (getChildValue("parameter") as? String) ?: ""
                TODO("APR: LLSelectMgr::getInstance().sendGodlikeRequest(req, param)")
            }
            AGENT_REGION -> sendRequest(TODO("APR: gAgent.getRegionHost()"))
            else -> {
                TODO("APR: find LLViewerRegion by name=dest in LLWorld::getRegionList(), " +
                     "then call sendRequest(region.getHost())")
            }
        }
    }

    private fun sendRequest(host: Any?) {
        val req = (getChildValue("request") as? String) ?: ""
        if (req == "terrain download") {
            TODO("APR: gXferManager.requestFile(\"terrain.raw\", \"terrain.raw\", LL_PATH_NONE, host, false, ::terrainDownloadDone)")
        } else {
            val trimmed = req.substringBefore(' ')
            val param   = (getChildValue("parameter") as? String) ?: ""
            sendRequest(trimmed, param, host)
        }
    }

    private fun getChildValue(name: String): Any = TODO("APR: getChild<UICtrl>(name).getValue()")
}

// -------------------------------------------------------------------------
// Free-standing message helpers (mirror C++ file-scope functions).
// -------------------------------------------------------------------------

fun sendSimWideDeletes(ownerId: LLUUID, flags: UInt) {
    TODO("APR: send SimWideDeletes message: AgentID, SessionID, TargetID=ownerId, Flags=flags")
}

fun terrainDownloadDone(status: Int) {
    TODO("APR: LLNotificationsUtil::add(\"TerrainDownloaded\")")
}
