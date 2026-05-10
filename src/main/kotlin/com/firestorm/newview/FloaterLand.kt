package com.firestorm.newview

import java.util.UUID

private const val CACHE_REFRESH_TIME = 2.5f
private const val COVENANT_REFRESH_TIME_SEC = 60.0

class FloaterLand(private val seed: Any?) {

    private var tabLand: Any? = null
    var panelGeneral: PanelLandGeneral? = null
    var panelObjects: PanelLandObjects? = null
    var panelOptions: PanelLandOptions? = null
    var panelAudio: Any? = null
    var panelMedia: Any? = null
    var panelAccess: PanelLandAccess? = null
    var panelCovenant: PanelLandCovenant? = null
    var panelExperiences: PanelLandExperiences? = null
    var panelEnvironment: PanelLandEnvironment? = null

    private var parcelSelection: Any? = null

    companion object {
        var sObserver: Any? = null
        var sLastTab: Int = 0
        var sRequestReplyOnUpdate: Boolean = true

        fun refreshAll() {
            TODO("APR: use JVM equivalent")
        }

        fun getCurrentPanelLandObjects(): PanelLandObjects? = TODO("APR: use JVM equivalent")
        fun getCurrentPanelLandCovenant(): PanelLandCovenant? = TODO("APR: use JVM equivalent")
    }

    fun getCurrentSelectedParcel(): Any? = TODO("APR: use JVM equivalent")

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun onOpen(key: Any?) {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        panelGeneral?.refresh()
        panelObjects?.refresh()
        panelOptions?.refresh()
        TODO("APR: use JVM equivalent")
    }

    fun onVisibilityChanged(visible: Boolean) {
        if (!visible) {
            TODO("APR: use JVM equivalent")
        }
    }
}

fun sendParcelSelectObjects(parcelLocalId: Int, returnType: UInt, returnIds: Set<UUID>? = null) {
    TODO("APR: use JVM equivalent")
}

class PanelLandGeneral(private val parcel: Any?) {

    private var uncheckedSell: Boolean = false
    private var lastParcelLocalId: Int = 0

    private var editName: Any? = null
    private var editDesc: Any? = null
    private var editUUID: Any? = null
    private var textSalePending: Any? = null
    private var textOwnerLabel: Any? = null
    private var textOwner: Any? = null
    private var contentRating: Any? = null
    private var landType: Any? = null
    private var textGroup: Any? = null
    private var textGroupLabel: Any? = null
    private var textClaimDateLabel: Any? = null
    private var textClaimDate: Any? = null
    private var textPriceLabel: Any? = null
    private var textPrice: Any? = null
    private var checkDeedToGroup: Any? = null
    private var checkContributeWithDeed: Any? = null
    private var saleInfoForSale1: Any? = null
    private var saleInfoForSale2: Any? = null
    private var saleInfoForSaleObjects: Any? = null
    private var saleInfoForSaleNoObjects: Any? = null
    private var saleInfoNotForSale: Any? = null
    private var btnSellLand: Any? = null
    private var btnStopSellLand: Any? = null
    private var textDwell: Any? = null
    private var btnBuyLand: Any? = null
    private var btnScriptLimits: Any? = null
    private var btnBuyGroupLand: Any? = null
    private var btnReleaseLand: Any? = null
    private var btnReclaimLand: Any? = null
    private var btnBuyPass: Any? = null
    private var btnStartAuction: Any? = null
    private var btnDeedToGroup: Any? = null
    private var btnSetGroup: Any? = null

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent")
    }

    fun refreshNames() {
        TODO("APR: use JVM equivalent")
    }

    open fun draw() {
        TODO("APR: use JVM equivalent")
    }

    fun setGroup(groupId: UUID) {
        TODO("APR: use JVM equivalent")
    }

    fun onClickSetGroup() {
        TODO("APR: use JVM equivalent")
    }

    fun processParcelInfo(parcelData: Any?) {
        TODO("APR: use JVM equivalent")
    }

    fun setParcelID(parcelId: UUID) {
        TODO("APR: use JVM equivalent")
    }

    fun setErrorStatus(status: Int, reason: String) {
        TODO("APR: use JVM equivalent")
    }

    companion object {
        fun onClickDeed() { TODO("APR: use JVM equivalent") }
        fun onClickBuyLand(buyGroupLand: Boolean) { TODO("APR: use JVM equivalent") }
        fun onClickScriptLimits() { TODO("APR: use JVM equivalent") }
        fun onClickRelease() { TODO("APR: use JVM equivalent") }
        fun onClickReclaim() { TODO("APR: use JVM equivalent") }
        fun onClickBuyPass() { TODO("APR: use JVM equivalent") }
        fun enableBuyPass(): Boolean = TODO("APR: use JVM equivalent")
        fun onCommitAny() { TODO("APR: use JVM equivalent") }
        fun finalizeCommit() { TODO("APR: use JVM equivalent") }
        fun onForSaleChange() { TODO("APR: use JVM equivalent") }
        fun finalizeSetSellChange() { TODO("APR: use JVM equivalent") }
        fun onSalePriceChange() { TODO("APR: use JVM equivalent") }
        fun cbBuyPass(selectedOption: Int): Boolean = TODO("APR: use JVM equivalent")
        fun onClickSellLand() { TODO("APR: use JVM equivalent") }
        fun onClickStopSellLand() { TODO("APR: use JVM equivalent") }
        fun onClickSet() { TODO("APR: use JVM equivalent") }
        fun onClickClear() { TODO("APR: use JVM equivalent") }
        fun onClickShow() { TODO("APR: use JVM equivalent") }
        fun callbackAvatarPick(names: List<String>, ids: List<UUID>) { TODO("APR: use JVM equivalent") }
        fun finalizeAvatarPick() { TODO("APR: use JVM equivalent") }
        fun callbackHighlightTransferable(option: Int) { TODO("APR: use JVM equivalent") }
        fun onClickStartAuction() { TODO("APR: use JVM equivalent") }
        fun confirmSaleChange(landSize: Int, salePrice: Int, authorizedName: String, callback: () -> Unit) {
            TODO("APR: use JVM equivalent")
        }
        fun callbackConfirmSaleChange(option: Int) { TODO("APR: use JVM equivalent") }
    }
}

class PanelLandObjects(private val parcel: Any?) {

    private var parcelObjectBonus: Any? = null
    private var swTotalObjects: Any? = null
    private var objectContribution: Any? = null
    private var totalObjects: Any? = null
    private var ownerObjects: Any? = null
    private var btnShowOwnerObjects: Any? = null
    private var btnReturnOwnerObjects: Any? = null
    private var groupObjects: Any? = null
    private var btnShowGroupObjects: Any? = null
    private var btnReturnGroupObjects: Any? = null
    private var otherObjects: Any? = null
    private var btnShowOtherObjects: Any? = null
    private var btnReturnOtherObjects: Any? = null
    private var selectedObjects: Any? = null
    private var cleanOtherObjectsTime: Any? = null
    private var otherTime: Int = 0
    private var btnRefresh: Any? = null
    private var btnReturnOwnerList: Any? = null
    private var ownerList: Any? = null

    private var firstReply: Boolean = true
    private val selectedOwners: MutableSet<UUID> = mutableSetOf()
    private var selectedName: String = ""
    private var selectedCount: Int = 0
    private var selectedIsGroup: Boolean = false

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent")
    }

    open fun draw() {
        TODO("APR: use JVM equivalent")
    }

    fun callbackReturnOwnerObjects(selectedOption: Int): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun callbackReturnGroupObjects(selectedOption: Int): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun callbackReturnOtherObjects(selectedOption: Int): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun callbackReturnOwnerList(selectedOption: Int): Boolean {
        TODO("APR: use JVM equivalent")
    }

    companion object {
        fun clickShowCore(panel: PanelLandObjects, returnType: Int, list: Set<UUID>? = null) {
            TODO("APR: use JVM equivalent")
        }

        fun onClickShowOwnerObjects(panel: PanelLandObjects) { clickShowCore(panel, 0) }
        fun onClickShowGroupObjects(panel: PanelLandObjects) { clickShowCore(panel, 1) }
        fun onClickShowOtherObjects(panel: PanelLandObjects) { clickShowCore(panel, 2) }
        fun onClickReturnOwnerObjects(panel: PanelLandObjects) { TODO("APR: use JVM equivalent") }
        fun onClickReturnGroupObjects(panel: PanelLandObjects) { TODO("APR: use JVM equivalent") }
        fun onClickReturnOtherObjects(panel: PanelLandObjects) { TODO("APR: use JVM equivalent") }
        fun onClickReturnOwnerList(panel: PanelLandObjects) { TODO("APR: use JVM equivalent") }
        fun onClickRefresh(panel: PanelLandObjects) { panel.refresh() }
        fun onDoubleClickOwner(panel: PanelLandObjects) { TODO("APR: use JVM equivalent") }
        fun processParcelObjectOwnersReply(msg: Any?) { TODO("APR: use JVM equivalent") }
    }
}

class PanelLandOptions(private val parcel: Any?) {

    private var checkEditObjects: Any? = null
    private var checkEditGroupObjects: Any? = null
    private var checkAllObjectEntry: Any? = null
    private var checkGroupObjectEntry: Any? = null
    private var checkEditLand: Any? = null
    private var checkSafe: Any? = null
    private var checkFly: Any? = null
    private var checkGroupScripts: Any? = null
    private var checkOtherScripts: Any? = null
    private var checkShowDirectory: Any? = null
    private var categoryCombo: Any? = null
    private var landingTypeCombo: Any? = null
    private var snapshotCtrl: Any? = null
    private var locationText: Any? = null
    private var seeAvatarsText: Any? = null
    private var setBtn: Any? = null
    private var clearBtn: Any? = null
    private var teleportToLandingPointBtn: Any? = null
    private var matureCtrl: Any? = null
    private var pushRestrictionCtrl: Any? = null
    private var seeAvatarsCtrl: Any? = null

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    open fun draw() {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent")
    }

    private fun refreshSearch() {
        TODO("APR: use JVM equivalent")
    }

    private fun getDirectoryFee(): Int = TODO("APR: use JVM equivalent")

    fun onClickTeleport() {
        TODO("APR: use JVM equivalent")
    }

    companion object {
        fun onCommitAny() { TODO("APR: use JVM equivalent") }
        fun onClickSet() { TODO("APR: use JVM equivalent") }
        fun onClickClear() { TODO("APR: use JVM equivalent") }
        fun toggleSeeAvatars() { TODO("APR: use JVM equivalent") }
    }
}

class PanelLandAccess(private val parcel: Any?) {

    private var listAccess: Any? = null
    private var listBanned: Any? = null
    private var allowText: Any? = null
    private var banText: Any? = null
    private var publicAccessCheck: Any? = null
    private var groupAccessCheck: Any? = null
    private var paymentInfoCheck: Any? = null
    private var ageVerifiedCheck: Any? = null
    private var temporaryPassCheck: Any? = null
    private var temporaryPassCombo: Any? = null
    private var temporaryPassPriceSpin: Any? = null
    private var temporaryPassHourSpin: Any? = null
    private var btnAddAllowed: Any? = null
    private var btnRemoveAllowed: Any? = null
    private var btnAddBanned: Any? = null
    private var btnRemoveBanned: Any? = null
    private var btnExportAccess: Any? = null
    private var btnExportBanned: Any? = null
    private var btnImportAccess: Any? = null
    private var btnImportBanned: Any? = null

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent")
    }

    fun refreshUi() {
        TODO("APR: use JVM equivalent")
    }

    fun refreshNames() {
        TODO("APR: use JVM equivalent")
    }

    open fun draw() {
        TODO("APR: use JVM equivalent")
    }

    fun onClickAddAccess() {
        TODO("APR: use JVM equivalent")
    }

    fun onClickAddBanned() {
        TODO("APR: use JVM equivalent")
    }

    fun onClickRemoveAccess() {
        TODO("APR: use JVM equivalent")
    }

    fun onClickRemoveBanned() {
        TODO("APR: use JVM equivalent")
    }

    fun callbackAvatarCBBanned(ids: List<UUID>) {
        TODO("APR: use JVM equivalent")
    }

    fun callbackAvatarCBBanned2(ids: List<UUID>, duration: Int) {
        TODO("APR: use JVM equivalent")
    }

    fun callbackAvatarCBAccess(ids: List<UUID>) {
        TODO("APR: use JVM equivalent")
    }

    fun onClickExportAccess() { TODO("APR: use JVM equivalent") }
    fun onClickExportBanned() { TODO("APR: use JVM equivalent") }
    fun onClickExportList(list: Any?, filename: String) { TODO("APR: use JVM equivalent") }
    fun exportListCallback(list: Any?, filenames: List<String>) { TODO("APR: use JVM equivalent") }
    fun onClickImportAccess() { TODO("APR: use JVM equivalent") }
    fun onClickImportBanned() { TODO("APR: use JVM equivalent") }
    fun onClickImportList(list: Any?) { TODO("APR: use JVM equivalent") }
    fun importListCallback(list: Any?, filenames: List<String>) { TODO("APR: use JVM equivalent") }

    companion object {
        fun onCommitPublicAccess() { TODO("APR: use JVM equivalent") }
        fun onCommitAny() { TODO("APR: use JVM equivalent") }
        fun onCommitGroupCheck() { TODO("APR: use JVM equivalent") }
    }
}

class PanelLandCovenant(private val parcel: Any?) {

    private var textEstateOwner: Any? = null
    private var lastRegionId: UUID? = null
    private var nextUpdateTime: Double = 0.0

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent")
    }

    companion object {
        fun updateCovenant(source: Any?) { TODO("APR: use JVM equivalent") }
        fun updateCovenantText(text: String) { TODO("APR: use JVM equivalent") }
        fun updateEstateName(name: String) { TODO("APR: use JVM equivalent") }
        fun updateLastModified(text: String) { TODO("APR: use JVM equivalent") }
        fun updateEstateOwnerName(name: String) { TODO("APR: use JVM equivalent") }
    }
}

class PanelLandExperiences(private val parcel: Any?) {

    private var allowed: Any? = null
    private var blocked: Any? = null

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent")
    }

    fun experienceAdded(id: UUID, xpType: UInt, accessType: UInt) {
        TODO("APR: use JVM equivalent")
    }

    fun experienceRemoved(id: UUID, accessType: UInt) {
        TODO("APR: use JVM equivalent")
    }

    private fun setupList(controlName: String, xpType: UInt, accessType: UInt): Any? {
        TODO("APR: use JVM equivalent")
    }

    private fun refreshPanel(panel: Any?, xpType: UInt) {
        TODO("APR: use JVM equivalent")
    }
}

class PanelLandEnvironment(private val parcel: Any?) {

    private var lastParcelId: Int = 0

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun refresh() {
        TODO("APR: use JVM equivalent")
    }

    fun isRegion(): Boolean = false

    fun isLargeEnough(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun getParcel(): Any? = TODO("APR: use JVM equivalent")
    fun canEdit(): Boolean = TODO("APR: use JVM equivalent")
    fun getParcelId(): Int = TODO("APR: use JVM equivalent")

    private fun refreshFromSource() {
        TODO("APR: use JVM equivalent")
    }

    private fun isSameRegion(): Boolean = TODO("APR: use JVM equivalent")
}
