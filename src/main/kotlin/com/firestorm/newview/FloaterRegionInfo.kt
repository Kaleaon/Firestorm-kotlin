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
            System.err.println("FloaterRegionInfo: dispatch EstateOwnerMessage not yet implemented")
        }

        fun processRegionInfo(msg: MessageSystem) {
            System.err.println("FloaterRegionInfo: parse RegionInfo message and refresh panels not yet implemented")
        }

        fun sRefreshFromRegion(region: Any?) {
            System.err.println("FloaterRegionInfo: refresh floater from region not yet implemented")
        }

        fun getPanelEstate(): PanelEstateInfo? = null
        fun getPanelAccess(): PanelEstateAccess? = null
        fun getPanelCovenant(): PanelEstateCovenant? = null
        fun getPanelRegionTerrain(): PanelRegionTerrainInfo? = null
        fun getPanelExperiences(): PanelRegionExperiences? = null
        fun getPanelGeneral(): PanelRegionGeneralInfo? = null
        fun getPanelEnvironment(): PanelRegionEnvironment? = null
        fun getPanelOpenSettings(): PanelRegionOpenSettingsInfo? = null
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

        val openSettingsUrl: String = ""
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

        val experiencesCap: String = ""
        if (experiencesCap.isNotEmpty()) {
            val experiencesPanel = PanelRegionExperiences()
            infoPanels.add(experiencesPanel)
            tab?.addTabPanel(experiencesPanel)
        }

        System.err.println("FloaterRegionInfo: send RequestRegionInfo message handler registration not yet implemented")
        regionChangedCallback = null

        return true
    }

    override fun onOpen(key: Any) {
        val disconnected: Boolean = false
        if (disconnected) {
            disableTabCtrls()
            return
        }
        refreshFromRegion(null)
        requestRegionInfo()
        if (godLevelChangeSlot == null) {
            godLevelChangeSlot = null
        }
    }

    override fun onClose(appQuitting: Boolean) {
        System.err.println("FloaterRegionInfo: disconnect godLevelChangeSlot not yet implemented")
    }

    override fun refresh() {
        infoPanels.forEach { it.refreshFromRegion(null) }
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
        System.err.println("FloaterRegionInfo: send RequestRegionInfo message not yet implemented")
    }

    fun enableTopButtons() { System.err.println("FloaterRegionInfo: enable top buttons not yet implemented") }
    fun disableTopButtons() { System.err.println("FloaterRegionInfo: disable top buttons not yet implemented") }

    protected fun onTabSelected(param: Any) { System.err.println("FloaterRegionInfo: handle tab selection not yet implemented") }
    protected fun disableTabCtrls() { infoPanels.forEach { it.setCtrlsEnabled(false) } }
    protected fun refreshFromRegion(region: Any?) { infoPanels.forEach { it.refreshFromRegion(region) } }
    protected fun onGodLevelChange(godLevel: UByte) { System.err.println("FloaterRegionInfo: handle god level change not yet implemented") }
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

    fun onClickManageTelehub() { System.err.println("FloaterRegionInfo: show telehub floater not yet implemented") }
    fun onClickManageRestartSchedule() { System.err.println("FloaterRegionInfo: show restart schedule floater not yet implemented") }

    protected fun initCtrl(name: String) {
        getChild<UICtrl>(name)?.setCommitCallback { _ -> onChangeAnything() }
    }

    protected fun sendEstateOwnerMessage(msg: MessageSystem, request: String, invoice: String, strings: List<String>) {
        System.err.println("FloaterRegionInfo: send EstateOwnerMessage via message system not yet implemented")
    }

    protected open fun sendUpdate(): Boolean = true
}

class PanelRegionOpenSettingsInfo : PanelRegionInfo() {

    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh open region settings from region not yet implemented")
        return false
    }

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind open settings panel widgets not yet implemented")
        return false
    }

    companion object {
        fun onClickOrs(userdata: Any?) { System.err.println("FloaterRegionInfo: apply open region settings not yet implemented") }
        fun onClickHelp(data: Any?) { System.err.println("FloaterRegionInfo: show help not yet implemented") }
    }
}

class PanelRegionGeneralInfo : PanelRegionInfo() {

    private var objBonusFactor: Float = 0f

    fun setObjBonusFactor(objectBonusFactor: Float) { objBonusFactor = objectBonusFactor }

    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh general info from region not yet implemented")
        return false
    }

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind general panel widgets not yet implemented")
        return false
    }

    fun onBtnSet() { if (sendUpdate()) disableButton("apply_btn") }

    override fun sendUpdate(): Boolean {
        System.err.println("FloaterRegionInfo: send region general update message not yet implemented")
        return false
    }

    private fun onClickKick() { System.err.println("FloaterRegionInfo: open avatar picker for kick not yet implemented") }
    private fun onKickCommit(ids: List<String>) { System.err.println("FloaterRegionInfo: kick selected avatar not yet implemented") }
    private fun onMessageCommit(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: send region-wide message not yet implemented")
        return false
    }
    private fun onChangeObjectBonus(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: confirm object bonus change not yet implemented")
        return false
    }

    companion object {
        fun onClickKickAll(userdata: Any?) { System.err.println("FloaterRegionInfo: kick all avatars confirm not yet implemented") }
        fun onClickMessage(userdata: Any?) { System.err.println("FloaterRegionInfo: open region message dialog not yet implemented") }
    }
}

class PanelRegionDebugInfo : PanelRegionInfo() {

    private var targetAvatar: String = ""

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind debug panel widgets not yet implemented")
        return false
    }
    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh debug panel from region not yet implemented")
        return false
    }
    override fun sendUpdate(): Boolean {
        System.err.println("FloaterRegionInfo: send debug update not yet implemented")
        return false
    }

    private fun onClickChooseAvatar() { System.err.println("FloaterRegionInfo: open avatar picker not yet implemented") }
    private fun callbackAvatarID(ids: List<String>, names: List<Any>) { System.err.println("FloaterRegionInfo: set target avatar not yet implemented") }
    private fun callbackReturn(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: return objects not yet implemented")
        return false
    }
    private fun callbackRestart(notification: Any, response: Any, seconds: Any): Boolean {
        System.err.println("FloaterRegionInfo: confirm region restart not yet implemented")
        return false
    }

    companion object {
        fun onClickReturn(data: Any?) { System.err.println("FloaterRegionInfo: return all objects not yet implemented") }
        fun onClickTopColliders(data: Any?) { System.err.println("FloaterRegionInfo: show top colliders not yet implemented") }
        fun onClickTopScripts(data: Any?) { System.err.println("FloaterRegionInfo: show top scripts not yet implemented") }
        fun onClickRestart(data: Any?) { System.err.println("FloaterRegionInfo: confirm region restart not yet implemented") }
        fun onClickCancelRestart(data: Any?) { System.err.println("FloaterRegionInfo: cancel region restart not yet implemented") }
        fun onClickDebugConsole(data: Any?) { System.err.println("FloaterRegionInfo: show debug console not yet implemented") }
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

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind terrain panel widgets not yet implemented")
        return false
    }
    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh terrain from region not yet implemented")
        return false
    }
    override fun sendUpdate(): Boolean {
        System.err.println("FloaterRegionInfo: send terrain update not yet implemented")
        return false
    }

    fun setEnvControls(available: Boolean) { System.err.println("FloaterRegionInfo: toggle environment controls not yet implemented") }
    fun validateTextureSizes(): Boolean {
        System.err.println("FloaterRegionInfo: validate texture sizes not yet implemented")
        return false
    }
    fun validateMaterials(): Boolean {
        System.err.println("FloaterRegionInfo: validate materials not yet implemented")
        return false
    }
    fun validateTextureHeights(): Boolean {
        System.err.println("FloaterRegionInfo: validate texture heights not yet implemented")
        return false
    }
    fun onSelectMaterialType() { System.err.println("FloaterRegionInfo: handle material type selection not yet implemented") }
    fun updateForMaterialType() { System.err.println("FloaterRegionInfo: update UI for material type not yet implemented") }

    fun onDownloadRawFilepickerCB(filenames: List<String>) { System.err.println("FloaterRegionInfo: download raw terrain not yet implemented") }
    fun onUploadRawFilepickerCB(filenames: List<String>) { System.err.println("FloaterRegionInfo: upload raw terrain not yet implemented") }
    fun callbackBakeTerrain(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: bake terrain confirm not yet implemented")
        return false
    }
    fun callbackTextureHeights(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: texture heights callback not yet implemented")
        return false
    }
    fun callbackMaterialCommit(index: Int) { System.err.println("FloaterRegionInfo: material commit callback not yet implemented") }

    private fun initMaterialCtrl(ctrl: TextureCtrl?, name: String, index: Int) {
        System.err.println("FloaterRegionInfo: bind material texture control not yet implemented")
    }

    companion object {
        fun onClickDownloadRaw(data: Any?) { System.err.println("FloaterRegionInfo: open file picker to download raw terrain not yet implemented") }
        fun onClickUploadRaw(data: Any?) { System.err.println("FloaterRegionInfo: open file picker to upload raw terrain not yet implemented") }
        fun onClickBakeTerrain(data: Any?) { System.err.println("FloaterRegionInfo: confirm bake terrain not yet implemented") }
    }
}

class PanelEstateInfo : PanelRegionInfo() {

    private var estateId: UInt = 0u
    private var estateInfoCommitConnection: Any? = null
    private var estateInfoUpdateConnection: Any? = null

    companion object {
        fun initDispatch(dispatch: Any) { System.err.println("FloaterRegionInfo: register estate dispatch handlers not yet implemented") }
        fun updateEstateName(name: String) { System.err.println("FloaterRegionInfo: update estate name in panel not yet implemented") }
        fun updateEstateOwnerName(name: String) { System.err.println("FloaterRegionInfo: update estate owner name in panel not yet implemented") }
        fun isLindenEstate(): Boolean {
            System.err.println("FloaterRegionInfo: check if this is a Linden estate not yet implemented")
            return false
        }
        fun onClickMessageEstate(data: Any?) { System.err.println("FloaterRegionInfo: open estate message dialog not yet implemented") }
    }

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind estate panel widgets not yet implemented")
        return false
    }
    override fun updateChild(childCtrl: UICtrl?) { System.err.println("FloaterRegionInfo: handle child control change not yet implemented") }
    override fun refresh() { System.err.println("FloaterRegionInfo: refresh estate panel not yet implemented") }
    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh estate info from region not yet implemented")
        return false
    }
    override fun estateUpdate(msg: MessageSystem): Boolean {
        System.err.println("FloaterRegionInfo: handle estate update message not yet implemented")
        return false
    }
    override fun sendUpdate(): Boolean {
        System.err.println("FloaterRegionInfo: send estate update not yet implemented")
        return false
    }

    fun updateControls(region: Any?) { System.err.println("FloaterRegionInfo: update estate controls per region not yet implemented") }
    fun refreshFromEstate() { System.err.println("FloaterRegionInfo: refresh from estate info model not yet implemented") }
    fun getOwnerName(): String {
        System.err.println("FloaterRegionInfo: get owner name string not yet implemented")
        return ""
    }
    fun setOwnerName(name: String) { System.err.println("FloaterRegionInfo: set owner name not yet implemented") }

    fun onChangeFixedSun() { System.err.println("FloaterRegionInfo: fixed sun change handler not yet implemented") }
    fun onChangeUseGlobalTime() { System.err.println("FloaterRegionInfo: use global time change handler not yet implemented") }
    fun onChangeAccessOverride() { System.err.println("FloaterRegionInfo: access override change handler not yet implemented") }
    fun onClickEditSky() { System.err.println("FloaterRegionInfo: open sky editor not yet implemented") }
    fun onClickEditSkyHelp() { System.err.println("FloaterRegionInfo: show sky editor help not yet implemented") }
    fun onClickEditDayCycle() { System.err.println("FloaterRegionInfo: open day cycle editor not yet implemented") }
    fun onClickEditDayCycleHelp() { System.err.println("FloaterRegionInfo: show day cycle editor help not yet implemented") }
    fun onClickKickUser() { System.err.println("FloaterRegionInfo: open avatar picker to kick not yet implemented") }
    fun kickUserConfirm(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: confirm user kick not yet implemented")
        return false
    }
    fun onKickUserCommit(ids: List<String>) { System.err.println("FloaterRegionInfo: execute user kick not yet implemented") }
    fun onMessageCommit(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: send estate message not yet implemented")
        return false
    }

    private fun callbackChangeLindenEstate(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: confirm Linden estate change not yet implemented")
        return false
    }
    private fun commitEstateAccess() { System.err.println("FloaterRegionInfo: commit estate access changes not yet implemented") }
    private fun commitEstateManagers() { System.err.println("FloaterRegionInfo: commit estate manager changes not yet implemented") }
    private fun checkSunHourSlider(childCtrl: UICtrl?): Boolean {
        System.err.println("FloaterRegionInfo: validate sun hour slider not yet implemented")
        return false
    }
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
    fun getEstateName(): String {
        System.err.println("FloaterRegionInfo: get estate name text not yet implemented")
        return ""
    }
    fun setEstateName(name: String) { System.err.println("FloaterRegionInfo: set estate name text not yet implemented") }
    fun getOwnerName(): String {
        System.err.println("FloaterRegionInfo: get owner name text not yet implemented")
        return ""
    }
    fun setOwnerName(name: String) { System.err.println("FloaterRegionInfo: set owner name text not yet implemented") }
    fun setCovenantTextEditor(text: String) { System.err.println("FloaterRegionInfo: set covenant text editor content not yet implemented") }

    companion object {
        fun updateCovenant(source: Any, assetId: String) { System.err.println("FloaterRegionInfo: update covenant from source not yet implemented") }
        fun updateCovenantText(text: String, assetId: String) { System.err.println("FloaterRegionInfo: update covenant text not yet implemented") }
        fun updateEstateName(name: String) { System.err.println("FloaterRegionInfo: update estate name label not yet implemented") }
        fun updateLastModified(text: String) { System.err.println("FloaterRegionInfo: update last modified label not yet implemented") }
        fun updateEstateOwnerName(name: String) { System.err.println("FloaterRegionInfo: update estate owner label not yet implemented") }
        fun confirmChangeCovenantCallback(notification: Any, response: Any): Boolean {
            System.err.println("FloaterRegionInfo: confirm covenant change not yet implemented")
            return false
        }
        fun resetCovenantId(userdata: Any?) { System.err.println("FloaterRegionInfo: reset covenant id not yet implemented") }
        fun confirmResetCovenantCallback(notification: Any, response: Any): Boolean {
            System.err.println("FloaterRegionInfo: confirm covenant reset not yet implemented")
            return false
        }
        fun onLoadComplete(assetUuid: String, type: Any, userData: Any?, status: Int, extStatus: Any) {
            System.err.println("FloaterRegionInfo: asset load complete callback not yet implemented")
        }
    }

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind covenant panel widgets not yet implemented")
        return false
    }
    override fun updateChild(childCtrl: UICtrl?) { System.err.println("FloaterRegionInfo: handle child control change not yet implemented") }
    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh covenant from region not yet implemented")
        return false
    }
    override fun estateUpdate(msg: MessageSystem): Boolean {
        System.err.println("FloaterRegionInfo: handle estate covenant update not yet implemented")
        return false
    }
    override fun sendUpdate(): Boolean {
        System.err.println("FloaterRegionInfo: send covenant update not yet implemented")
        return false
    }

    fun sendChangeCovenantId(assetId: String) { System.err.println("FloaterRegionInfo: send covenant id change message not yet implemented") }
    fun loadInvItem(item: Any?) { System.err.println("FloaterRegionInfo: load inventory item as covenant not yet implemented") }
    fun handleDragAndDrop(x: Int, y: Int, mask: Int, drop: Boolean, cargoType: Any,
                          cargoData: Any?, accept: Any?, tooltipMsg: String): Boolean {
        System.err.println("FloaterRegionInfo: handle drag and drop onto covenant panel not yet implemented")
        return false
    }
}

class PanelRegionExperiences : PanelRegionInfo() {

    private var trusted: Any? = null  // PanelExperienceListEditor
    private var allowed: Any? = null  // PanelExperienceListEditor
    private var blocked: Any? = null  // PanelExperienceListEditor
    private var defaultExperience: String = ""

    companion object {
        fun experienceCoreConfirm(notification: Any, response: Any): Boolean {
            System.err.println("FloaterRegionInfo: confirm experience core change not yet implemented")
            return false
        }
        fun sendEstateExperienceDelta(flags: UInt, agentId: String) {
            System.err.println("FloaterRegionInfo: send estate experience delta message not yet implemented")
        }
        fun infoCallback(handle: Any, content: Any) {
            System.err.println("FloaterRegionInfo: experience info callback not yet implemented")
        }
    }

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind experience panel widgets not yet implemented")
        return false
    }
    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh experiences from region not yet implemented")
        return false
    }
    override fun sendUpdate(): Boolean {
        System.err.println("FloaterRegionInfo: send experiences update not yet implemented")
        return false
    }

    fun sendPurchaseRequest() { System.err.println("FloaterRegionInfo: send experience purchase request not yet implemented") }
    fun processResponse(content: Any) { System.err.println("FloaterRegionInfo: process experience response not yet implemented") }

    private fun refreshRegionExperiences() { System.err.println("FloaterRegionInfo: refresh region experience lists not yet implemented") }
    private fun itemChanged(eventType: UInt, id: String) { System.err.println("FloaterRegionInfo: handle experience item change not yet implemented") }
}

class PanelEstateAccess : PanelRegionInfo() {

    private var pendingUpdate: Boolean = false
    private var ctrlsEnabled: Boolean = true

    fun setPendingUpdate(pending: Boolean) { pendingUpdate = pending }
    fun getPendingUpdate(): Boolean = pendingUpdate

    companion object {
        fun sendEstateAccessDelta(flags: UInt, agentId: String) {
            System.err.println("FloaterRegionInfo: send estate access delta message not yet implemented")
        }
    }

    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind estate access panel widgets not yet implemented")
        return false
    }
    override fun updateChild(childCtrl: UICtrl?) { System.err.println("FloaterRegionInfo: handle child control change not yet implemented") }
    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh estate access from region not yet implemented")
        return false
    }

    fun updateControls(region: Any?) { System.err.println("FloaterRegionInfo: update access controls per region not yet implemented") }
    fun updateLists() { System.err.println("FloaterRegionInfo: refresh allowed/banned/manager lists not yet implemented") }

    fun onClickExportEstateManagerList() { System.err.println("FloaterRegionInfo: export estate manager list not yet implemented") }
    fun onClickExportAllowedList() { System.err.println("FloaterRegionInfo: export allowed list not yet implemented") }
    fun onClickExportAllowedGroupList() { System.err.println("FloaterRegionInfo: export allowed group list not yet implemented") }
    fun onClickExportBannedList() { System.err.println("FloaterRegionInfo: export banned list not yet implemented") }
    fun onClickExportList(list: NameListCtrl, filename: String) { System.err.println("FloaterRegionInfo: export named list not yet implemented") }
    fun exportListCallback(list: NameListCtrl, filenames: List<String>) { System.err.println("FloaterRegionInfo: export list callback not yet implemented") }
    fun onClickImportEstateManagerList() { System.err.println("FloaterRegionInfo: import estate manager list not yet implemented") }
    fun onClickImportAllowedList() { System.err.println("FloaterRegionInfo: import allowed list not yet implemented") }
    fun onClickImportAllowedGroupList() { System.err.println("FloaterRegionInfo: import allowed group list not yet implemented") }
    fun onClickImportBannedList() { System.err.println("FloaterRegionInfo: import banned list not yet implemented") }
    fun onClickImportList(list: NameListCtrl) { System.err.println("FloaterRegionInfo: import named list not yet implemented") }
    fun importListCallback(list: NameListCtrl, filenames: List<String>) { System.err.println("FloaterRegionInfo: import list callback not yet implemented") }

    private fun onClickAddAllowedAgent() { System.err.println("FloaterRegionInfo: add allowed agent not yet implemented") }
    private fun onClickRemoveAllowedAgent() { System.err.println("FloaterRegionInfo: remove allowed agent not yet implemented") }
    private fun onClickCopyAllowedList() { System.err.println("FloaterRegionInfo: copy allowed list to clipboard not yet implemented") }
    private fun onClickAddAllowedGroup() { System.err.println("FloaterRegionInfo: add allowed group not yet implemented") }
    private fun onClickRemoveAllowedGroup() { System.err.println("FloaterRegionInfo: remove allowed group not yet implemented") }
    private fun onClickCopyAllowedGroupList() { System.err.println("FloaterRegionInfo: copy allowed group list not yet implemented") }
    private fun onClickAddBannedAgent() { System.err.println("FloaterRegionInfo: add banned agent not yet implemented") }
    private fun onClickRemoveBannedAgent() { System.err.println("FloaterRegionInfo: remove banned agent not yet implemented") }
    private fun onClickCopyBannedList() { System.err.println("FloaterRegionInfo: copy banned list not yet implemented") }
    private fun onClickAddEstateManager() { System.err.println("FloaterRegionInfo: add estate manager not yet implemented") }
    private fun onClickRemoveEstateManager() { System.err.println("FloaterRegionInfo: remove estate manager not yet implemented") }
    private fun onAllowedSearchEdit(searchString: String) { System.err.println("FloaterRegionInfo: filter allowed list not yet implemented") }
    private fun onAllowedGroupsSearchEdit(searchString: String) { System.err.println("FloaterRegionInfo: filter allowed groups list not yet implemented") }
    private fun onBannedSearchEdit(searchString: String) { System.err.println("FloaterRegionInfo: filter banned list not yet implemented") }
    private fun addAllowedGroup(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: add group confirm not yet implemented")
        return false
    }
    private fun addAllowedGroup2(id: String) { System.err.println("FloaterRegionInfo: execute group add not yet implemented") }
    private fun searchAgent(listCtrl: NameListCtrl, searchString: String) { System.err.println("FloaterRegionInfo: search agent in list not yet implemented") }
    private fun copyListToClipboard(listName: String) { System.err.println("FloaterRegionInfo: copy list to clipboard not yet implemented") }
}

class PanelRegionEnvironment : PanelRegionInfo() {

    companion object {
        val DIRTY_FLAG_OVERRIDE: UInt = 1u
    }

    override fun refresh() { System.err.println("FloaterRegionInfo: refresh environment panel not yet implemented") }
    override fun refreshFromRegion(region: Any?): Boolean {
        System.err.println("FloaterRegionInfo: refresh environment from region not yet implemented")
        return false
    }
    override fun postBuild(): Boolean {
        System.err.println("FloaterRegionInfo: bind environment panel widgets not yet implemented")
        return false
    }
    override fun sendUpdate(): Boolean {
        System.err.println("FloaterRegionInfo: send environment update not yet implemented")
        return false
    }

    fun getParcel(): Any? = null
    fun canEdit(): Boolean {
        System.err.println("FloaterRegionInfo: LLEnvironment::canAgentUpdateRegionEnvironment() not yet implemented")
        return false
    }
    fun isLargeEnough(): Boolean = true
    fun isRegion(): Boolean = true
    fun getParcelId(): Int = -1  // INVALID_PARCEL_ID

    private fun confirmUpdateEstateEnvironment(notification: Any, response: Any): Boolean {
        System.err.println("FloaterRegionInfo: confirm estate environment update not yet implemented")
        return false
    }
    private fun onChkAllowOverride(value: Boolean) { System.err.println("FloaterRegionInfo: allow override toggle not yet implemented") }
    private fun refreshFromSource() { System.err.println("FloaterRegionInfo: refresh environment from source not yet implemented") }
}
