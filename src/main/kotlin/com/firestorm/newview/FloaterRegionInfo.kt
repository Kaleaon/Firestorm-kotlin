package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Panel
import com.firestorm.llui.TabContainer
import com.firestorm.llui.Button
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.ComboBox
import com.firestorm.llui.LineEditor
import com.firestorm.llui.TextBox
import com.firestorm.llui.TextureCtrl
import com.firestorm.llui.SliderCtrl
import com.firestorm.llui.SpinCtrl
import com.firestorm.llui.NameListCtrl
import com.firestorm.llui.UICtrl
import com.firestorm.llmessage.MessageSystem

class FloaterRegionInfo(seed: Any) : Floater(seed) {

    private var tab: TabContainer? = null
    private val infoPanels: MutableList<PanelRegionInfo> = mutableListOf()
    private var environmentPanel: PanelRegionEnvironment? = null
    private var godLevelChangeSlot: Any? = null
    private var regionChangedCallback: Any? = null

    companion object {
        var requestInvoice: String = ""

        fun getLastInvoice(): String = requestInvoice
        fun nextInvoice() { requestInvoice = java.util.UUID.randomUUID().toString() }

        fun processEstateOwnerRequest(msg: MessageSystem, params: Any?) {
            TODO("APR: use JVM equivalent - dispatch EstateOwnerMessage")
        }

        fun processRegionInfo(msg: MessageSystem) {
            TODO("APR: use JVM equivalent - parse RegionInfo message and refresh panels")
        }

        fun sRefreshFromRegion(region: Any?) {
            TODO("APR: use JVM equivalent - refresh floater from region")
        }

        fun getPanelEstate(): PanelEstateInfo? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
        fun getPanelAccess(): PanelEstateAccess? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
        fun getPanelCovenant(): PanelEstateCovenant? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
        fun getPanelRegionTerrain(): PanelRegionTerrainInfo? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
        fun getPanelExperiences(): PanelRegionExperiences? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
        fun getPanelGeneral(): PanelRegionGeneralInfo? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
        fun getPanelEnvironment(): PanelRegionEnvironment? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
        fun getPanelOpenSettings(): PanelRegionOpenSettingsInfo? =
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterRegionInfo>")
    }

    override fun postBuild(): Boolean {
        tab = getChild<TabContainer>("region_panels")
        tab?.setCommitCallback { param -> onTabSelected(param) }

        val estatePanel = PanelEstateInfo()
        infoPanels.add(estatePanel)
        tab?.addTabPanel(estatePanel)

        val accessPanel = PanelEstateAccess()
        infoPanels.add(accessPanel)
        tab?.addTabPanel(accessPanel)

        val covenantPanel = PanelEstateCovenant()
        infoPanels.add(covenantPanel)
        tab?.addTabPanel(covenantPanel)

        val generalPanel = PanelRegionGeneralInfo()
        infoPanels.add(generalPanel)
        tab?.addTabPanel(generalPanel)

        val openSettingsUrl = TODO("APR: use JVM equivalent - gAgent.getRegionCapability(DispatchOpenRegionSettings)") as? String ?: ""
        if (openSettingsUrl.isNotEmpty()) {
            val openSettingsPanel = PanelRegionOpenSettingsInfo()
            infoPanels.add(openSettingsPanel)
            tab?.addTabPanel(openSettingsPanel)
        }

        val terrainPanel = PanelRegionTerrainInfo()
        infoPanels.add(terrainPanel)
        tab?.addTabPanel(terrainPanel)

        environmentPanel = PanelRegionEnvironment()
        tab?.addTabPanel(environmentPanel!!)

        val debugPanel = PanelRegionDebugInfo()
        infoPanels.add(debugPanel)
        tab?.addTabPanel(debugPanel)

        val experiencesCap = TODO("APR: use JVM equivalent - gAgent.getRegionCapability(RegionExperiences)") as? String ?: ""
        if (experiencesCap.isNotEmpty()) {
            val experiencesPanel = PanelRegionExperiences()
            infoPanels.add(experiencesPanel)
            tab?.addTabPanel(experiencesPanel)
        }

        TODO("APR: use JVM equivalent - gMessageSystem->setHandlerFunc(EstateOwnerMessage)")
        regionChangedCallback = TODO("APR: use JVM equivalent - gAgent.addRegionChangedCallback")

        return true
    }

    override fun onOpen(key: Any) {
        val disconnected = TODO("APR: use JVM equivalent - gDisconnected") as? Boolean ?: false
        if (disconnected) {
            disableTabCtrls()
            return
        }
        refreshFromRegion(TODO("APR: use JVM equivalent - gAgent.getRegion()"))
        requestRegionInfo()
        if (godLevelChangeSlot == null) {
            godLevelChangeSlot = TODO("APR: use JVM equivalent - gAgent.registerGodLevelChangeListener")
        }
    }

    override fun onClose(appQuitting: Boolean) {
        TODO("APR: use JVM equivalent - disconnect godLevelChangeSlot")
    }

    override fun refresh() {
        infoPanels.forEach { it.refreshFromRegion(TODO("APR: use JVM equivalent - gAgent.getRegion()")) }
    }

    fun onRegionChanged() {
        if (getVisible()) requestRegionInfo()
    }

    fun requestRegionInfo() {
        tab?.getChild<Panel>("General")?.setCtrlsEnabled(false)
        tab?.getChild<Panel>("Debug")?.setCtrlsEnabled(false)
        tab?.getChild<Panel>("Terrain")?.setAllChildrenEnabled(false, true)
        tab?.getChild<Panel>("Estate")?.setCtrlsEnabled(false)
        tab?.getChild<Panel>("Access")?.setCtrlsEnabled(false)
        TODO("APR: use JVM equivalent - send RequestRegionInfo message")
    }

    fun enableTopButtons() { TODO("APR: use JVM equivalent - enable top buttons") }
    fun disableTopButtons() { TODO("APR: use JVM equivalent - disable top buttons") }

    protected fun onTabSelected(param: Any) { TODO("APR: use JVM equivalent - handle tab selection") }
    protected fun disableTabCtrls() { infoPanels.forEach { it.setCtrlsEnabled(false) } }
    protected fun refreshFromRegion(region: Any?) { infoPanels.forEach { it.refreshFromRegion(region) } }
    protected fun onGodLevelChange(godLevel: UByte) { TODO("APR: use JVM equivalent - handle god level change") }
}

abstract class PanelRegionInfo : Panel() {

    protected var host: Any? = null  // LLHost
    protected var floaterRestartScheduleHandle: Any? = null

    fun onBtnSet() { if (sendUpdate()) disableButton("apply_btn") }
    fun onChangeChildCtrl(ctrl: UICtrl?) { enableButton("apply_btn") }
    fun onChangeAnything() { enableButton("apply_btn") }

    open fun refreshFromRegion(region: Any?): Boolean = true
    open fun estateUpdate(msg: MessageSystem): Boolean = true

    override fun postBuild(): Boolean {
        getChild<Button>("apply_btn")?.setClickedCallback { onBtnSet() }
        return true
    }

    open fun updateChild(childCtrl: UICtrl?) {}

    fun enableButton(btnName: String, enable: Boolean = true) {
        getChild<Button>(btnName)?.setEnabled(enable)
    }

    fun disableButton(btnName: String) = enableButton(btnName, false)

    fun onClickManageTelehub() { TODO("APR: use JVM equivalent - show telehub floater") }
    fun onClickManageRestartSchedule() { TODO("APR: use JVM equivalent - show restart schedule floater") }

    protected fun initCtrl(name: String) {
        getChild<UICtrl>(name)?.setCommitCallback { _ -> onChangeAnything() }
    }

    protected fun sendEstateOwnerMessage(msg: MessageSystem, request: String, invoice: String, strings: List<String>) {
        TODO("APR: use JVM equivalent - send EstateOwnerMessage via message system")
    }

    protected open fun sendUpdate(): Boolean = true
}

class PanelRegionOpenSettingsInfo : PanelRegionInfo() {

    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh open region settings from region")
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind open settings panel widgets") }

    companion object {
        fun onClickOrs(userdata: Any?) { TODO("APR: use JVM equivalent - apply open region settings") }
        fun onClickHelp(data: Any?) { TODO("APR: use JVM equivalent - show help") }
    }
}

class PanelRegionGeneralInfo : PanelRegionInfo() {

    private var objBonusFactor: Float = 0f

    fun setObjBonusFactor(objectBonusFactor: Float) { objBonusFactor = objectBonusFactor }

    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh general info from region")
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind general panel widgets") }

    fun onBtnSet() { if (sendUpdate()) disableButton("apply_btn") }

    override fun sendUpdate(): Boolean {
        TODO("APR: use JVM equivalent - send region general update message")
    }

    private fun onClickKick() { TODO("APR: use JVM equivalent - open avatar picker for kick") }
    private fun onKickCommit(ids: List<String>) { TODO("APR: use JVM equivalent - kick selected avatar") }
    private fun onMessageCommit(notification: Any, response: Any): Boolean =
        TODO("APR: use JVM equivalent - send region-wide message")
    private fun onChangeObjectBonus(notification: Any, response: Any): Boolean =
        TODO("APR: use JVM equivalent - confirm object bonus change")

    companion object {
        fun onClickKickAll(userdata: Any?) { TODO("APR: use JVM equivalent - kick all avatars confirm") }
        fun onClickMessage(userdata: Any?) { TODO("APR: use JVM equivalent - open region message dialog") }
    }
}

class PanelRegionDebugInfo : PanelRegionInfo() {

    private var targetAvatar: String = ""

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind debug panel widgets") }
    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh debug panel from region")
    }
    override fun sendUpdate(): Boolean { TODO("APR: use JVM equivalent - send debug update") }

    private fun onClickChooseAvatar() { TODO("APR: use JVM equivalent - open avatar picker") }
    private fun callbackAvatarID(ids: List<String>, names: List<Any>) { TODO("APR: use JVM equivalent - set target avatar") }
    private fun callbackReturn(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent - return objects")
    private fun callbackRestart(notification: Any, response: Any, seconds: Any): Boolean =
        TODO("APR: use JVM equivalent - confirm region restart")

    companion object {
        fun onClickReturn(data: Any?) { TODO("APR: use JVM equivalent - return all objects") }
        fun onClickTopColliders(data: Any?) { TODO("APR: use JVM equivalent - show top colliders") }
        fun onClickTopScripts(data: Any?) { TODO("APR: use JVM equivalent - show top scripts") }
        fun onClickRestart(data: Any?) { TODO("APR: use JVM equivalent - confirm region restart") }
        fun onClickCancelRestart(data: Any?) { TODO("APR: use JVM equivalent - cancel region restart") }
        fun onClickDebugConsole(data: Any?) { TODO("APR: use JVM equivalent - show debug console") }
    }
}

class PanelRegionTerrainInfo : PanelRegionInfo() {

    private var confirmedTextureHeights: Boolean = false
    private var askedTextureHeights: Boolean = false
    private var materialTypeCtrl: CheckBoxCtrl? = null
    private val textureDetailCtrl: Array<TextureCtrl?> = arrayOfNulls(4)
    private val materialDetailCtrl: Array<TextureCtrl?> = arrayOfNulls(4)
    private val lastSetTextures: Array<String> = Array(4) { "" }
    private val lastSetMaterials: Array<String> = Array(4) { "" }
    private val materialScaleUCtrl: Array<SpinCtrl?> = arrayOfNulls(4)
    private val materialScaleVCtrl: Array<SpinCtrl?> = arrayOfNulls(4)
    private val materialRotationCtrl: Array<SpinCtrl?> = arrayOfNulls(4)
    private val materialOffsetUCtrl: Array<SpinCtrl?> = arrayOfNulls(4)
    private val materialOffsetVCtrl: Array<SpinCtrl?> = arrayOfNulls(4)

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind terrain panel widgets") }
    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh terrain from region")
    }
    override fun sendUpdate(): Boolean { TODO("APR: use JVM equivalent - send terrain update") }

    fun setEnvControls(available: Boolean) { TODO("APR: use JVM equivalent - toggle environment controls") }
    fun validateTextureSizes(): Boolean = TODO("APR: use JVM equivalent - validate texture sizes")
    fun validateMaterials(): Boolean = TODO("APR: use JVM equivalent - validate materials")
    fun validateTextureHeights(): Boolean = TODO("APR: use JVM equivalent - validate texture heights")
    fun onSelectMaterialType() { TODO("APR: use JVM equivalent - handle material type selection") }
    fun updateForMaterialType() { TODO("APR: use JVM equivalent - update UI for material type") }

    fun onDownloadRawFilepickerCB(filenames: List<String>) { TODO("APR: use JVM equivalent - download raw terrain") }
    fun onUploadRawFilepickerCB(filenames: List<String>) { TODO("APR: use JVM equivalent - upload raw terrain") }
    fun callbackBakeTerrain(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent - bake terrain confirm")
    fun callbackTextureHeights(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent - texture heights callback")
    fun callbackMaterialCommit(index: Int) { TODO("APR: use JVM equivalent - material commit callback") }

    private fun initMaterialCtrl(ctrl: TextureCtrl?, name: String, index: Int) {
        TODO("APR: use JVM equivalent - bind material texture control")
    }

    companion object {
        fun onClickDownloadRaw(data: Any?) { TODO("APR: use JVM equivalent - open file picker to download raw terrain") }
        fun onClickUploadRaw(data: Any?) { TODO("APR: use JVM equivalent - open file picker to upload raw terrain") }
        fun onClickBakeTerrain(data: Any?) { TODO("APR: use JVM equivalent - confirm bake terrain") }
    }
}

class PanelEstateInfo : PanelRegionInfo() {

    private var estateId: UInt = 0u
    private var estateInfoCommitConnection: Any? = null
    private var estateInfoUpdateConnection: Any? = null

    companion object {
        fun initDispatch(dispatch: Any) { TODO("APR: use JVM equivalent - register estate dispatch handlers") }
        fun updateEstateName(name: String) { TODO("APR: use JVM equivalent - update estate name in panel") }
        fun updateEstateOwnerName(name: String) { TODO("APR: use JVM equivalent - update estate owner name in panel") }
        fun isLindenEstate(): Boolean = TODO("APR: use JVM equivalent - check if this is a Linden estate")
        fun onClickMessageEstate(data: Any?) { TODO("APR: use JVM equivalent - open estate message dialog") }
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind estate panel widgets") }
    override fun updateChild(childCtrl: UICtrl?) { TODO("APR: use JVM equivalent - handle child control change") }
    override fun refresh() { TODO("APR: use JVM equivalent - refresh estate panel") }
    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh estate info from region")
    }
    override fun estateUpdate(msg: MessageSystem): Boolean {
        TODO("APR: use JVM equivalent - handle estate update message")
    }
    override fun sendUpdate(): Boolean { TODO("APR: use JVM equivalent - send estate update") }

    fun updateControls(region: Any?) { TODO("APR: use JVM equivalent - update estate controls per region") }
    fun refreshFromEstate() { TODO("APR: use JVM equivalent - refresh from estate info model") }
    fun getOwnerName(): String = TODO("APR: use JVM equivalent - get owner name string")
    fun setOwnerName(name: String) { TODO("APR: use JVM equivalent - set owner name") }

    fun onChangeFixedSun() { TODO("APR: use JVM equivalent - fixed sun change handler") }
    fun onChangeUseGlobalTime() { TODO("APR: use JVM equivalent - use global time change handler") }
    fun onChangeAccessOverride() { TODO("APR: use JVM equivalent - access override change handler") }
    fun onClickEditSky() { TODO("APR: use JVM equivalent - open sky editor") }
    fun onClickEditSkyHelp() { TODO("APR: use JVM equivalent - show sky editor help") }
    fun onClickEditDayCycle() { TODO("APR: use JVM equivalent - open day cycle editor") }
    fun onClickEditDayCycleHelp() { TODO("APR: use JVM equivalent - show day cycle editor help") }
    fun onClickKickUser() { TODO("APR: use JVM equivalent - open avatar picker to kick") }
    fun kickUserConfirm(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent - confirm user kick")
    fun onKickUserCommit(ids: List<String>) { TODO("APR: use JVM equivalent - execute user kick") }
    fun onMessageCommit(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent - send estate message")

    private fun callbackChangeLindenEstate(notification: Any, response: Any): Boolean =
        TODO("APR: use JVM equivalent - confirm Linden estate change")
    private fun commitEstateAccess() { TODO("APR: use JVM equivalent - commit estate access changes") }
    private fun commitEstateManagers() { TODO("APR: use JVM equivalent - commit estate manager changes") }
    private fun checkSunHourSlider(childCtrl: UICtrl?): Boolean = TODO("APR: use JVM equivalent - validate sun hour slider")
}

class PanelEstateCovenant : PanelRegionInfo() {

    enum class AssetStatus { ERROR, UNLOADED, LOADING, LOADED }

    private var estateNameText: TextBox? = null
    private var estateOwnerText: TextBox? = null
    private var lastModifiedText: TextBox? = null
    private var covenantId: String = ""
    private var editor: Any? = null  // ViewerTextEditor
    private var assetStatus: AssetStatus = AssetStatus.UNLOADED

    fun getCovenantId(): String = covenantId
    fun setCovenantId(id: String) { covenantId = id }
    fun getEstateName(): String = TODO("APR: use JVM equivalent - get estate name text")
    fun setEstateName(name: String) { TODO("APR: use JVM equivalent - set estate name text") }
    fun getOwnerName(): String = TODO("APR: use JVM equivalent - get owner name text")
    fun setOwnerName(name: String) { TODO("APR: use JVM equivalent - set owner name text") }
    fun setCovenantTextEditor(text: String) { TODO("APR: use JVM equivalent - set covenant text editor content") }

    companion object {
        fun updateCovenant(source: Any, assetId: String) { TODO("APR: use JVM equivalent - update covenant from source") }
        fun updateCovenantText(text: String, assetId: String) { TODO("APR: use JVM equivalent - update covenant text") }
        fun updateEstateName(name: String) { TODO("APR: use JVM equivalent - update estate name label") }
        fun updateLastModified(text: String) { TODO("APR: use JVM equivalent - update last modified label") }
        fun updateEstateOwnerName(name: String) { TODO("APR: use JVM equivalent - update estate owner label") }
        fun confirmChangeCovenantCallback(notification: Any, response: Any): Boolean =
            TODO("APR: use JVM equivalent - confirm covenant change")
        fun resetCovenantId(userdata: Any?) { TODO("APR: use JVM equivalent - reset covenant id") }
        fun confirmResetCovenantCallback(notification: Any, response: Any): Boolean =
            TODO("APR: use JVM equivalent - confirm covenant reset")
        fun onLoadComplete(assetUuid: String, type: Any, userData: Any?, status: Int, extStatus: Any) {
            TODO("APR: use JVM equivalent - asset load complete callback")
        }
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind covenant panel widgets") }
    override fun updateChild(childCtrl: UICtrl?) { TODO("APR: use JVM equivalent - handle child control change") }
    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh covenant from region")
    }
    override fun estateUpdate(msg: MessageSystem): Boolean {
        TODO("APR: use JVM equivalent - handle estate covenant update")
    }
    override fun sendUpdate(): Boolean { TODO("APR: use JVM equivalent - send covenant update") }

    fun sendChangeCovenantId(assetId: String) { TODO("APR: use JVM equivalent - send covenant id change message") }
    fun loadInvItem(item: Any?) { TODO("APR: use JVM equivalent - load inventory item as covenant") }
    fun handleDragAndDrop(x: Int, y: Int, mask: Int, drop: Boolean, cargoType: Any,
                          cargoData: Any?, accept: Any?, tooltipMsg: String): Boolean {
        TODO("APR: use JVM equivalent - handle drag and drop onto covenant panel")
    }
}

class PanelRegionExperiences : PanelRegionInfo() {

    private var trusted: Any? = null  // PanelExperienceListEditor
    private var allowed: Any? = null  // PanelExperienceListEditor
    private var blocked: Any? = null  // PanelExperienceListEditor
    private var defaultExperience: String = ""

    companion object {
        fun experienceCoreConfirm(notification: Any, response: Any): Boolean =
            TODO("APR: use JVM equivalent - confirm experience core change")
        fun sendEstateExperienceDelta(flags: UInt, agentId: String) {
            TODO("APR: use JVM equivalent - send estate experience delta message")
        }
        fun infoCallback(handle: Any, content: Any) {
            TODO("APR: use JVM equivalent - experience info callback")
        }
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind experience panel widgets") }
    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh experiences from region")
    }
    override fun sendUpdate(): Boolean { TODO("APR: use JVM equivalent - send experiences update") }

    fun sendPurchaseRequest() { TODO("APR: use JVM equivalent - send experience purchase request") }
    fun processResponse(content: Any) { TODO("APR: use JVM equivalent - process experience response") }

    private fun refreshRegionExperiences() { TODO("APR: use JVM equivalent - refresh region experience lists") }
    private fun itemChanged(eventType: UInt, id: String) { TODO("APR: use JVM equivalent - handle experience item change") }
}

class PanelEstateAccess : PanelRegionInfo() {

    private var pendingUpdate: Boolean = false
    private var ctrlsEnabled: Boolean = true

    fun setPendingUpdate(pending: Boolean) { pendingUpdate = pending }
    fun getPendingUpdate(): Boolean = pendingUpdate

    companion object {
        fun sendEstateAccessDelta(flags: UInt, agentId: String) {
            TODO("APR: use JVM equivalent - send estate access delta message")
        }
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind estate access panel widgets") }
    override fun updateChild(childCtrl: UICtrl?) { TODO("APR: use JVM equivalent - handle child control change") }
    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh estate access from region")
    }

    fun updateControls(region: Any?) { TODO("APR: use JVM equivalent - update access controls per region") }
    fun updateLists() { TODO("APR: use JVM equivalent - refresh allowed/banned/manager lists") }

    fun onClickExportEstateManagerList() { TODO("APR: use JVM equivalent - export estate manager list") }
    fun onClickExportAllowedList() { TODO("APR: use JVM equivalent - export allowed list") }
    fun onClickExportAllowedGroupList() { TODO("APR: use JVM equivalent - export allowed group list") }
    fun onClickExportBannedList() { TODO("APR: use JVM equivalent - export banned list") }
    fun onClickExportList(list: NameListCtrl, filename: String) { TODO("APR: use JVM equivalent - export named list") }
    fun exportListCallback(list: NameListCtrl, filenames: List<String>) { TODO("APR: use JVM equivalent - export list callback") }
    fun onClickImportEstateManagerList() { TODO("APR: use JVM equivalent - import estate manager list") }
    fun onClickImportAllowedList() { TODO("APR: use JVM equivalent - import allowed list") }
    fun onClickImportAllowedGroupList() { TODO("APR: use JVM equivalent - import allowed group list") }
    fun onClickImportBannedList() { TODO("APR: use JVM equivalent - import banned list") }
    fun onClickImportList(list: NameListCtrl) { TODO("APR: use JVM equivalent - import named list") }
    fun importListCallback(list: NameListCtrl, filenames: List<String>) { TODO("APR: use JVM equivalent - import list callback") }

    private fun onClickAddAllowedAgent() { TODO("APR: use JVM equivalent - add allowed agent") }
    private fun onClickRemoveAllowedAgent() { TODO("APR: use JVM equivalent - remove allowed agent") }
    private fun onClickCopyAllowedList() { TODO("APR: use JVM equivalent - copy allowed list to clipboard") }
    private fun onClickAddAllowedGroup() { TODO("APR: use JVM equivalent - add allowed group") }
    private fun onClickRemoveAllowedGroup() { TODO("APR: use JVM equivalent - remove allowed group") }
    private fun onClickCopyAllowedGroupList() { TODO("APR: use JVM equivalent - copy allowed group list") }
    private fun onClickAddBannedAgent() { TODO("APR: use JVM equivalent - add banned agent") }
    private fun onClickRemoveBannedAgent() { TODO("APR: use JVM equivalent - remove banned agent") }
    private fun onClickCopyBannedList() { TODO("APR: use JVM equivalent - copy banned list") }
    private fun onClickAddEstateManager() { TODO("APR: use JVM equivalent - add estate manager") }
    private fun onClickRemoveEstateManager() { TODO("APR: use JVM equivalent - remove estate manager") }
    private fun onAllowedSearchEdit(searchString: String) { TODO("APR: use JVM equivalent - filter allowed list") }
    private fun onAllowedGroupsSearchEdit(searchString: String) { TODO("APR: use JVM equivalent - filter allowed groups list") }
    private fun onBannedSearchEdit(searchString: String) { TODO("APR: use JVM equivalent - filter banned list") }
    private fun addAllowedGroup(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent - add group confirm")
    private fun addAllowedGroup2(id: String) { TODO("APR: use JVM equivalent - execute group add") }
    private fun searchAgent(listCtrl: NameListCtrl, searchString: String) { TODO("APR: use JVM equivalent - search agent in list") }
    private fun copyListToClipboard(listName: String) { TODO("APR: use JVM equivalent - copy list to clipboard") }
}

class PanelRegionEnvironment : PanelRegionInfo() {

    companion object {
        val DIRTY_FLAG_OVERRIDE: UInt = 1u
    }

    override fun refresh() { TODO("APR: use JVM equivalent - refresh environment panel") }
    override fun refreshFromRegion(region: Any?): Boolean {
        TODO("APR: use JVM equivalent - refresh environment from region")
    }
    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind environment panel widgets") }
    override fun sendUpdate(): Boolean { TODO("APR: use JVM equivalent - send environment update") }

    fun getParcel(): Any? = null
    fun canEdit(): Boolean = TODO("APR: use JVM equivalent - LLEnvironment::canAgentUpdateRegionEnvironment()")
    fun isLargeEnough(): Boolean = true
    fun isRegion(): Boolean = true
    fun getParcelId(): Int = -1  // INVALID_PARCEL_ID

    private fun confirmUpdateEstateEnvironment(notification: Any, response: Any): Boolean =
        TODO("APR: use JVM equivalent - confirm estate environment update")
    private fun onChkAllowOverride(value: Boolean) { TODO("APR: use JVM equivalent - allow override toggle") }
    private fun refreshFromSource() { TODO("APR: use JVM equivalent - refresh environment from source") }
}
