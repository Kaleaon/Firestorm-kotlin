package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Panel
import com.firestorm.llui.TabContainer
import com.firestorm.llui.Button
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.ComboBox
import com.firestorm.llui.LineEditor
import com.firestorm.llui.TextBox
import com.firestorm.llui.TextEditor
import com.firestorm.llui.TextureCtrl
import com.firestorm.llui.NameListCtrl
import com.firestorm.llui.UIImage
import com.firestorm.llui.UICtrl
import com.firestorm.llprimitive.Parcel
import com.firestorm.llmessage.MessageSystem

const val CACHE_REFRESH_TIME: Float = 2.5f

class FloaterLand(seed: Any) : Floater(seed) {

    var tabLand: TabContainer? = null
    var panelGeneral: PanelLandGeneral? = null
    var panelObjects: PanelLandObjects? = null
    var panelOptions: PanelLandOptions? = null
    var panelAudio: Panel? = null
    var panelMedia: Panel? = null
    var panelAccess: PanelLandAccess? = null
    var panelCovenant: PanelLandCovenant? = null
    var panelExperiences: Panel? = null
    var panelEnvironment: Panel? = null
    var parcel: Any? = null  // SafeHandle<ParcelSelection>

    companion object {
        var observer: ParcelSelectionObserver? = null
        var lastTab: Int = 0
        var requestReplyOnUpdate: Boolean = true

        fun refreshAll() {
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterLand>")
        }

        fun getCurrentPanelLandObjects(): PanelLandObjects? {
            TODO("APR: use JVM equivalent - LLFloaterReg::getTypedInstance<FloaterLand>")
        }

        fun getCurrentPanelLandCovenant(): PanelLandCovenant? {
            TODO("APR: use JVM equivalent - LLFloaterReg::getTypedInstance<FloaterLand>")
        }
    }

    fun getCurrentSelectedParcel(): Parcel? {
        TODO("APR: use JVM equivalent - mParcel->getParcel()")
    }

    override fun onOpen(key: Any) {
        TODO("APR: use JVM equivalent - LLViewerParcelMgr::getInstance()->selectParcelAt")
        parcel = TODO("APR: use JVM equivalent - LLViewerParcelMgr::getInstance()->getFloatingParcelSelection()")
        refresh()
    }

    override fun postBuild(): Boolean {
        setVisibleCallback { visible -> onVisibilityChanged(visible) }
        tabLand = getChild<TabContainer>("landtab")
        tabLand?.selectTab(lastTab)
        return true
    }

    protected fun refresh() {
        panelGeneral?.refresh()
        panelObjects?.refresh()
        panelOptions?.refresh()
        panelAudio?.let { TODO("APR: refresh panelAudio") }
        panelMedia?.let { TODO("APR: refresh panelMedia") }
        panelAccess?.refresh()
        panelCovenant?.refresh()
        panelExperiences?.let { TODO("APR: refresh panelExperiences") }
        panelEnvironment?.let { TODO("APR: refresh panelEnvironment") }
    }

    private fun onVisibilityChanged(visible: Any) {
        if (!(visible as? Boolean ?: true)) {
            TODO("APR: use JVM equivalent - LLSelectMgr::getInstance()->unhighlightAll()")
            lastTab = tabLand?.getCurrentPanelIndex() ?: 0
        }
    }
}

class ParcelSelectionObserver {
    fun changed() = FloaterLand.refreshAll()
}

fun sendParcelSelectObjects(parcelLocalId: Int, returnType: UInt, returnIds: Set<String>? = null) {
    TODO("APR: use JVM equivalent - gMessageSystem ParcelSelectObjects")
}

class PanelLandGeneral(var parcel: Any) : Panel(), RemoteParcelInfoObserver {

    private var uncheckedSell: Boolean = false
    private var lastParcelLocalId: Int = 0

    private var labelName: TextBox? = null
    private var editName: LineEditor? = null
    private var labelDesc: TextBox? = null
    private var editDesc: TextEditor? = null
    private var editUUID: LineEditor? = null
    private var textSalePending: TextBox? = null
    private var btnDeedToGroup: Button? = null
    private var btnSetGroup: Button? = null
    private var textOwnerLabel: TextBox? = null
    private var textOwner: TextBox? = null
    private var contentRating: TextBox? = null
    private var landType: TextBox? = null
    private var textGroup: TextBox? = null
    private var textGroupLabel: TextBox? = null
    private var textClaimDateLabel: TextBox? = null
    private var textClaimDate: TextBox? = null
    private var textPriceLabel: TextBox? = null
    private var textPrice: TextBox? = null
    private var checkDeedToGroup: CheckBoxCtrl? = null
    private var checkContributeWithDeed: CheckBoxCtrl? = null
    private var saleInfoForSale1: TextBox? = null
    private var saleInfoForSale2: TextBox? = null
    private var saleInfoForSaleObjects: TextBox? = null
    private var saleInfoForSaleNoObjects: TextBox? = null
    private var saleInfoNotForSale: TextBox? = null
    private var btnSellLand: Button? = null
    private var btnStopSellLand: Button? = null
    private var textDwell: TextBox? = null
    private var btnBuyLand: Button? = null
    private var btnScriptLimits: Button? = null
    private var btnBuyGroupLand: Button? = null
    private var btnReleaseLand: Button? = null
    private var btnReclaimLand: Button? = null
    private var btnBuyPass: Button? = null
    private var btnStartAuction: Button? = null

    companion object {
        var selectionForBuyPass: Any? = null  // Pointer<ParcelSelection>
        var buyPassDialogHandle: Any? = null  // Handle<Floater>

        fun onClickDeed(data: Any?) { TODO("APR: use JVM equivalent - deed to group action") }
        fun onClickBuyLand(data: Any?) { TODO("APR: use JVM equivalent - buy land action") }
        fun onClickScriptLimits(data: Any?) { TODO("APR: use JVM equivalent - script limits floater") }
        fun onClickRelease(data: Any?) { TODO("APR: use JVM equivalent - release land action") }
        fun onClickReclaim(data: Any?) { TODO("APR: use JVM equivalent - reclaim land action") }
        fun onClickBuyPass(data: Any?) { TODO("APR: use JVM equivalent - buy pass action") }
        fun enableBuyPass(data: Any?): Boolean = TODO("APR: use JVM equivalent - enable buy pass check")
        fun onCommitAny(ctrl: UICtrl?, userdata: Any?) { TODO("APR: use JVM equivalent - commit any change") }
        fun finalizeCommit(userdata: Any?) { TODO("APR: use JVM equivalent - finalize commit") }
        fun onForSaleChange(ctrl: UICtrl?, userdata: Any?) { TODO("APR: use JVM equivalent - for sale change") }
        fun finalizeSetSellChange(userdata: Any?) { TODO("APR: use JVM equivalent - finalize sell change") }
        fun onSalePriceChange(ctrl: UICtrl?, userdata: Any?) { TODO("APR: use JVM equivalent - sale price change") }
        fun cbBuyPass(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent - buy pass callback")
        fun onClickSellLand(data: Any?) { TODO("APR: use JVM equivalent - sell land action") }
        fun onClickStopSellLand(data: Any?) { TODO("APR: use JVM equivalent - stop sell land action") }
        fun onClickSet(data: Any?) { TODO("APR: use JVM equivalent - set action") }
        fun onClickClear(data: Any?) { TODO("APR: use JVM equivalent - clear action") }
        fun onClickShow(data: Any?) { TODO("APR: use JVM equivalent - show action") }
        fun callbackAvatarPick(names: List<String>, ids: List<String>, data: Any?) { TODO("APR: use JVM equivalent - avatar pick callback") }
        fun finalizeAvatarPick(data: Any?) { TODO("APR: use JVM equivalent - finalize avatar pick") }
        fun callbackHighlightTransferable(option: Int, userdata: Any?) { TODO("APR: use JVM equivalent - highlight transferable callback") }
        fun onClickStartAuction(data: Any?) { TODO("APR: use JVM equivalent - start auction action") }
        fun confirmSaleChange(landSize: Int, salePrice: Int, authorizedName: String, callback: (Any?) -> Unit, userdata: Any?) {
            TODO("APR: use JVM equivalent - confirm sale change")
        }
        fun callbackConfirmSaleChange(option: Int, userdata: Any?) { TODO("APR: use JVM equivalent - confirm sale change callback") }
    }

    override fun postBuild(): Boolean {
        editName = getChild<LineEditor>("Name")
        editName?.setCommitCallback { ctrl, userdata -> onCommitAny(ctrl, userdata) }

        editUUID = getChild<LineEditor>("UUID")
        editUUID?.setEnabled(false)

        editDesc = getChild<TextEditor>("Description")
        editDesc?.setCommitOnFocusLost(true)
        editDesc?.setCommitCallback { ctrl, userdata -> onCommitAny(ctrl, userdata) }

        textSalePending  = getChild<TextBox>("SalePending")
        textOwnerLabel   = getChild<TextBox>("Owner:")
        textOwner        = getChild<TextBox>("OwnerText")
        contentRating    = getChild<TextBox>("ContentRatingText")
        landType         = getChild<TextBox>("LandTypeText")
        textGroupLabel   = getChild<TextBox>("Group:")
        textGroup        = getChild<TextBox>("GroupText")

        btnSetGroup = getChild<Button>("Set...")
        btnSetGroup?.setCommitCallback { onClickSetGroup() }

        checkDeedToGroup = getChild<CheckBoxCtrl>("check deed")
        btnDeedToGroup = getChild<Button>("Deed...")
        btnDeedToGroup?.setClickedCallback { onClickDeed(this) }

        checkContributeWithDeed = getChild<CheckBoxCtrl>("check contrib")

        saleInfoNotForSale      = getChild<TextBox>("Not for sale.")
        saleInfoForSale1        = getChild<TextBox>("For Sale: Price L\$[PRICE].")
        saleInfoForSale2        = getChild<TextBox>("For sale to")
        saleInfoForSaleObjects  = getChild<TextBox>("Sell with landowners objects in parcel.")
        saleInfoForSaleNoObjects= getChild<TextBox>("Selling with no objects in parcel.")

        btnSellLand = getChild<Button>("Sell Land...")
        btnSellLand?.setClickedCallback { onClickSellLand(this) }

        btnStopSellLand = getChild<Button>("Cancel Land Sale")
        btnStopSellLand?.setClickedCallback { onClickStopSellLand(this) }

        textClaimDateLabel = getChild<TextBox>("Claimed:")
        textClaimDate      = getChild<TextBox>("DateClaimText")
        textPriceLabel     = getChild<TextBox>("PriceLabel")
        textPrice          = getChild<TextBox>("PriceText")
        textDwell          = getChild<TextBox>("DwellText")

        btnBuyLand = getChild<Button>("Buy Land...")
        btnBuyLand?.setClickedCallback { onClickBuyLand(false) }

        btnBuyGroupLand = getChild<Button>("Buy For Group...")
        btnBuyGroupLand?.setClickedCallback { onClickBuyLand(true) }

        btnBuyPass = getChild<Button>("Buy Pass...")
        btnBuyPass?.setClickedCallback { onClickBuyPass(this) }

        btnReleaseLand = getChild<Button>("Abandon Land...")
        btnReleaseLand?.setClickedCallback { onClickRelease(null) }

        btnReclaimLand = getChild<Button>("Reclaim Land...")
        btnReclaimLand?.setClickedCallback { onClickReclaim(null) }

        btnStartAuction = getChild<Button>("Linden Sale...")
        btnStartAuction?.setClickedCallback { onClickStartAuction(this) }

        btnScriptLimits = getChild<Button>("Scripts...")
        val url = TODO("APR: use JVM equivalent - gAgent.getRegionCapability(LandResources)") as? String ?: ""
        if (url.isNotEmpty()) {
            btnScriptLimits?.setClickedCallback { onClickScriptLimits(this) }
        } else {
            btnScriptLimits?.setVisible(false)
        }

        return true
    }

    fun refresh() { TODO("APR: use JVM equivalent - refresh land general panel from parcel") }
    fun refreshNames() { TODO("APR: use JVM equivalent - refresh owner/group names") }
    override fun draw() { TODO("APR: use JVM equivalent - draw panel") }

    fun setGroup(groupId: String) { TODO("APR: use JVM equivalent - set group") }
    fun onClickSetGroup() { TODO("APR: use JVM equivalent - open group picker") }

    override fun processParcelInfo(parcelData: Any) { TODO("APR: use JVM equivalent - process parcel info") }
    override fun setParcelId(parcelId: String) { TODO("APR: use JVM equivalent - set parcel id") }
    override fun setErrorStatus(status: Int, reason: String) { TODO("APR: use JVM equivalent - set error status") }
}

class PanelLandObjects(var parcel: Any) : Panel() {

    private var parcelObjectBonus: TextBox? = null
    private var swTotalObjects: TextBox? = null
    private var objectContribution: TextBox? = null
    private var totalObjects: TextBox? = null
    private var ownerObjects: TextBox? = null
    private var btnShowOwnerObjects: Button? = null
    private var btnReturnOwnerObjects: Button? = null
    private var groupObjects: TextBox? = null
    private var btnShowGroupObjects: Button? = null
    private var btnReturnGroupObjects: Button? = null
    private var otherObjects: TextBox? = null
    private var btnShowOtherObjects: Button? = null
    private var btnReturnOtherObjects: Button? = null
    private var selectedObjects: TextBox? = null
    private var cleanOtherObjectsTime: LineEditor? = null
    private var otherTime: Int = 0
    private var btnRefresh: Button? = null
    private var btnReturnOwnerList: Button? = null
    private var ownerList: NameListCtrl? = null
    private var iconAvatarOnline: UIImage? = null
    private var iconAvatarOffline: UIImage? = null
    private var iconGroup: UIImage? = null
    private var firstReply: Boolean = true
    private var selectedOwners: MutableSet<String> = mutableSetOf()
    private var selectedName: String = ""
    private var selectedCount: Int = 0
    private var selectedIsGroup: Boolean = false

    companion object {
        fun clickShowCore(panelp: PanelLandObjects, returnType: Int, list: MutableSet<String>? = null) {
            TODO("APR: use JVM equivalent - ParcelSelectObjects message")
        }
        fun onClickShowOwnerObjects(data: Any?) { TODO("APR: use JVM equivalent - show owner objects") }
        fun onClickShowGroupObjects(data: Any?) { TODO("APR: use JVM equivalent - show group objects") }
        fun onClickShowOtherObjects(data: Any?) { TODO("APR: use JVM equivalent - show other objects") }
        fun onClickReturnOwnerObjects(data: Any?) { TODO("APR: use JVM equivalent - return owner objects") }
        fun onClickReturnGroupObjects(data: Any?) { TODO("APR: use JVM equivalent - return group objects") }
        fun onClickReturnOtherObjects(data: Any?) { TODO("APR: use JVM equivalent - return other objects") }
        fun onClickReturnOwnerList(data: Any?) { TODO("APR: use JVM equivalent - return owner list") }
        fun onClickRefresh(data: Any?) { TODO("APR: use JVM equivalent - refresh objects") }
        fun onDoubleClickOwner(data: Any?) { TODO("APR: use JVM equivalent - double click owner") }
        fun onCommitList(ctrl: UICtrl?, data: Any?) { TODO("APR: use JVM equivalent - commit list") }
        fun onLostFocus(caller: Any?, userData: Any?) { TODO("APR: use JVM equivalent - lost focus") }
        fun onCommitClean(caller: UICtrl?, userData: Any?) { TODO("APR: use JVM equivalent - commit clean") }
        fun processParcelObjectOwnersReply(msg: MessageSystem, params: Any?) {
            TODO("APR: use JVM equivalent - process parcel object owners reply")
        }
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind all object panel widgets") }
    fun refresh() { TODO("APR: use JVM equivalent - refresh objects panel from parcel") }
    override fun draw() { TODO("APR: use JVM equivalent - draw panel") }
    fun callbackReturnOwnerObjects(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent")
    fun callbackReturnGroupObjects(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent")
    fun callbackReturnOtherObjects(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent")
    fun callbackReturnOwnerList(notification: Any, response: Any): Boolean = TODO("APR: use JVM equivalent")
}

class PanelLandOptions(var parcel: Any) : Panel() {

    private var checkEditObjects: CheckBoxCtrl? = null
    private var checkEditGroupObjects: CheckBoxCtrl? = null
    private var checkAllObjectEntry: CheckBoxCtrl? = null
    private var checkGroupObjectEntry: CheckBoxCtrl? = null
    private var checkEditLand: CheckBoxCtrl? = null
    private var checkSafe: CheckBoxCtrl? = null
    private var checkFly: CheckBoxCtrl? = null
    private var checkGroupScripts: CheckBoxCtrl? = null
    private var checkOtherScripts: CheckBoxCtrl? = null
    private var checkShowDirectory: CheckBoxCtrl? = null
    private var categoryCombo: ComboBox? = null
    private var landingTypeCombo: ComboBox? = null
    private var snapshotCtrl: TextureCtrl? = null
    private var locationText: TextBox? = null
    private var seeAvatarsText: TextBox? = null
    private var setBtn: Button? = null
    private var clearBtn: Button? = null
    private var teleportToLandingPointBtn: Button? = null
    private var matureCtrl: CheckBoxCtrl? = null
    private var pushRestrictionCtrl: CheckBoxCtrl? = null
    private var seeAvatarsCtrl: CheckBoxCtrl? = null

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind options panel widgets") }
    override fun draw() { TODO("APR: use JVM equivalent - draw panel") }
    fun refresh() { TODO("APR: use JVM equivalent - refresh options from parcel") }

    private fun refreshSearch() { TODO("APR: use JVM equivalent - refresh search checkbox/category") }
    private fun getDirectoryFee(): Int = TODO("APR: use JVM equivalent - get directory fee")
    private fun onClickTeleport() { TODO("APR: use JVM equivalent - teleport to landing point") }

    companion object {
        fun onCommitAny(ctrl: UICtrl?, userdata: Any?) { TODO("APR: use JVM equivalent - commit any change") }
        fun onClickSet(userdata: Any?) { TODO("APR: use JVM equivalent - set landing point") }
        fun onClickClear(userdata: Any?) { TODO("APR: use JVM equivalent - clear landing point") }
        fun toggleSeeAvatars(userdata: Any?) { TODO("APR: use JVM equivalent - toggle see avatars") }
    }
}

class PanelLandAccess(var parcel: Any) : Panel() {

    private var listAccess: NameListCtrl? = null
    private var listBanned: NameListCtrl? = null
    private var allowText: UICtrl? = null
    private var banText: UICtrl? = null
    private var publicAccessCheck: UICtrl? = null
    private var groupAccessCheck: UICtrl? = null
    private var paymentInfoCheck: UICtrl? = null
    private var ageVerifiedCheck: UICtrl? = null
    private var temporaryPassCheck: UICtrl? = null
    private var temporaryPassCombo: ComboBox? = null
    private var temporaryPassPriceSpin: UICtrl? = null
    private var temporaryPassHourSpin: UICtrl? = null
    private var btnAddAllowed: Button? = null
    private var btnRemoveAllowed: Button? = null
    private var btnAddBanned: Button? = null
    private var btnRemoveBanned: Button? = null
    private var btnExportAccess: Button? = null
    private var btnExportBanned: Button? = null
    private var btnImportAccess: Button? = null
    private var btnImportBanned: Button? = null

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind access panel widgets") }
    fun refresh() { TODO("APR: use JVM equivalent - refresh access panel from parcel") }
    fun refreshUi() { TODO("APR: use JVM equivalent - refresh access UI controls") }
    fun refreshNames() { TODO("APR: use JVM equivalent - refresh allowed/banned names") }
    override fun draw() { TODO("APR: use JVM equivalent - draw panel") }

    fun onClickAddAccess() { TODO("APR: use JVM equivalent - add to access list") }
    fun onClickAddBanned() { TODO("APR: use JVM equivalent - add to ban list") }
    fun onClickRemoveAccess() { TODO("APR: use JVM equivalent - remove from access list") }
    fun onClickRemoveBanned() { TODO("APR: use JVM equivalent - remove from ban list") }
    fun callbackAvatarCBBanned(ids: List<String>) { TODO("APR: use JVM equivalent - ban avatar callback") }
    fun callbackAvatarCBBanned2(ids: List<String>, duration: Int) { TODO("APR: use JVM equivalent - timed ban callback") }
    fun callbackAvatarCBAccess(ids: List<String>) { TODO("APR: use JVM equivalent - access avatar callback") }

    fun onClickExportAccess() { TODO("APR: use JVM equivalent - export access list") }
    fun onClickExportBanned() { TODO("APR: use JVM equivalent - export ban list") }
    fun onClickExportList(list: NameListCtrl, filename: String) { TODO("APR: use JVM equivalent - export named list") }
    fun exportListCallback(list: NameListCtrl, filenames: List<String>) { TODO("APR: use JVM equivalent - export list callback") }
    fun onClickImportAccess() { TODO("APR: use JVM equivalent - import access list") }
    fun onClickImportBanned() { TODO("APR: use JVM equivalent - import ban list") }
    fun onClickImportList(list: NameListCtrl) { TODO("APR: use JVM equivalent - import named list") }
    fun importListCallback(list: NameListCtrl, filenames: List<String>) { TODO("APR: use JVM equivalent - import list callback") }

    companion object {
        fun onCommitPublicAccess(ctrl: UICtrl?, userdata: Any?) { TODO("APR: use JVM equivalent - public access commit") }
        fun onCommitAny(ctrl: UICtrl?, userdata: Any?) { TODO("APR: use JVM equivalent - commit any access change") }
        fun onCommitGroupCheck(ctrl: UICtrl?, userdata: Any?) { TODO("APR: use JVM equivalent - group check commit") }
    }
}

class PanelLandCovenant(var parcel: Any) : Panel() {

    private var lastRegionId: String = ""
    private var nextUpdateTime: Double = 0.0
    private var textEstateOwner: TextBox? = null

    companion object {
        fun updateCovenant(source: Any) { TODO("APR: use JVM equivalent - update covenant from text source") }
        fun updateCovenantText(text: String) { TODO("APR: use JVM equivalent - update covenant text") }
        fun updateEstateName(name: String) { TODO("APR: use JVM equivalent - update estate name") }
        fun updateLastModified(text: String) { TODO("APR: use JVM equivalent - update last modified text") }
        fun updateEstateOwnerName(name: String) { TODO("APR: use JVM equivalent - update estate owner name") }
    }

    override fun postBuild(): Boolean { TODO("APR: use JVM equivalent - bind covenant panel widgets") }
    fun refresh() { TODO("APR: use JVM equivalent - refresh covenant panel") }
}

interface RemoteParcelInfoObserver {
    fun processParcelInfo(parcelData: Any)
    fun setParcelId(parcelId: String)
    fun setErrorStatus(status: Int, reason: String)
}
