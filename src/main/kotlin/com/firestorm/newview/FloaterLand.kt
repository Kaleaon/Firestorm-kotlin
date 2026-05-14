package com.firestorm.newview

import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.io.File

private const val CACHE_REFRESH_TIME = 2.5f
private const val COVENANT_REFRESH_TIME_SEC = 60.0

// Return-type constants matching C++ viewer values
private const val RETURN_TYPE_OWNER: UInt = 0u
private const val RETURN_TYPE_GROUP: UInt = 1u
private const val RETURN_TYPE_OTHER: UInt = 2u
private const val RETURN_TYPE_LIST:  UInt = 4u

// Singleton-style instance registry (replaces LLFloaterReg lookups)
internal object FloaterLandRegistry {
    private var landInstance: FloaterLand? = null
    fun getLandInstance(): FloaterLand? = landInstance
    fun setLandInstance(f: FloaterLand?) { landInstance = f }
}

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

        /** Refresh the active FloaterLand instance (mirrors LLFloaterLand::refreshAll). */
        fun refreshAll() {
            FloaterLandRegistry.getLandInstance()?.refresh()
        }

        /** Return the PanelLandObjects from the active instance. */
        fun getCurrentPanelLandObjects(): PanelLandObjects? =
            FloaterLandRegistry.getLandInstance()?.panelObjects

        /** Return the PanelLandCovenant from the active instance. */
        fun getCurrentPanelLandCovenant(): PanelLandCovenant? =
            FloaterLandRegistry.getLandInstance()?.panelCovenant
    }

    /** Return the currently selected parcel (mirrors LLFloaterLand::getCurrentSelectedParcel). */
    fun getCurrentSelectedParcel(): Any? = parcelSelection

    /**
     * Build the floater: register as the active instance and select last-used tab.
     * Mirrors LLFloaterLand::postBuild.
     */
    fun postBuild(): Boolean {
        FloaterLandRegistry.setLandInstance(this)
        sObserver = Runnable { refreshAll() }
        tabLand = sLastTab
        return true
    }

    /**
     * Called when the floater is opened.
     * Mirrors LLFloaterLand::onOpen.
     */
    fun onOpen(key: Any?) {
        if (parcelSelection == null) {
            parcelSelection = key
        }
        refresh()
    }

    /** Refresh all sub-panels. Mirrors LLFloaterLand::refresh. */
    fun refresh() {
        panelGeneral?.refresh()
        panelObjects?.refresh()
        panelOptions?.refresh()
        panelAccess?.refresh()
        panelCovenant?.refresh()
        panelExperiences?.refresh()
        panelEnvironment?.refresh()
    }

    /**
     * Called when visibility changes.
     * On hide: save the current tab index. Mirrors LLFloaterLand::onVisibilityChanged.
     */
    fun onVisibilityChanged(visible: Boolean) {
        if (!visible) {
            sLastTab = (tabLand as? Int) ?: 0
        }
    }
}

/**
 * Send a ParcelSelectObjects UDP message to the region simulator.
 *
 * Packet layout (little-endian):
 *   AgentData block  : AgentID (UUID 16 B) + SessionID (UUID 16 B)
 *   ParcelData block : LocalID (S32 4 B) + ReturnType (U32 4 B)
 *   ReturnIDs blocks : ReturnID (UUID 16 B) per id, or one null UUID when empty
 */
fun sendParcelSelectObjects(
    parcelLocalId: Int,
    returnType: UInt,
    returnIds: Set<UUID>? = null,
    regionHost: String = "127.0.0.1",
    regionPort: Int = 13000,
    agentId: UUID = UUID(0L, 0L),
    sessionId: UUID = UUID(0L, 0L)
) {
    val idCount = returnIds?.size?.coerceAtLeast(1) ?: 1
    val buf = ByteBuffer.allocate(32 + 8 + 16 * idCount).order(ByteOrder.LITTLE_ENDIAN)
    fun putUUID(uuid: UUID) { buf.putLong(uuid.mostSignificantBits); buf.putLong(uuid.leastSignificantBits) }
    putUUID(agentId); putUUID(sessionId)
    buf.putInt(parcelLocalId); buf.putInt(returnType.toInt())
    if (returnIds.isNullOrEmpty()) putUUID(UUID(0L, 0L)) else returnIds.forEach { putUUID(it) }
    val data = buf.array()
    try {
        DatagramSocket().use { socket ->
            socket.send(DatagramPacket(data, data.size, InetSocketAddress(regionHost, regionPort)))
        }
    } catch (e: Exception) {
        System.err.println("sendParcelSelectObjects: UDP send failed: ${e.message}")
    }
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
        System.err.println("PanelLandGeneral: postBuild not yet implemented")
        return true
    }

    fun refresh() {
        System.err.println("PanelLandGeneral: refresh not yet implemented")
    }

    fun refreshNames() {
        System.err.println("PanelLandGeneral: refreshNames not yet implemented")
    }

    open fun draw() {
        System.err.println("PanelLandGeneral: draw not yet implemented")
    }

    fun setGroup(groupId: UUID) {
        System.err.println("PanelLandGeneral: setGroup not yet implemented")
    }

    fun onClickSetGroup() {
        System.err.println("PanelLandGeneral: onClickSetGroup not yet implemented")
    }

    fun processParcelInfo(parcelData: Any?) {
        System.err.println("PanelLandGeneral: processParcelInfo not yet implemented")
    }

    fun setParcelID(parcelId: UUID) {
        System.err.println("PanelLandGeneral: setParcelID not yet implemented")
    }

    fun setErrorStatus(status: Int, reason: String) {
        System.err.println("PanelLandGeneral: setErrorStatus not yet implemented")
    }

    companion object {
        fun onClickDeed() {
            System.err.println("PanelLandGeneral: onClickDeed not yet implemented")
        }
        fun onClickBuyLand(buyGroupLand: Boolean) {
            System.err.println("PanelLandGeneral: onClickBuyLand not yet implemented")
        }
        fun onClickScriptLimits() {
            System.err.println("PanelLandGeneral: onClickScriptLimits not yet implemented")
        }
        fun onClickRelease() {
            System.err.println("PanelLandGeneral: onClickRelease not yet implemented")
        }
        fun onClickReclaim() {
            System.err.println("PanelLandGeneral: onClickReclaim not yet implemented")
        }
        fun onClickBuyPass() {
            System.err.println("PanelLandGeneral: onClickBuyPass not yet implemented")
        }
        fun enableBuyPass(): Boolean {
            System.err.println("PanelLandGeneral: enableBuyPass not yet implemented")
            return false
        }
        fun onCommitAny() {
            System.err.println("PanelLandGeneral: onCommitAny not yet implemented")
        }
        fun finalizeCommit() {
            System.err.println("PanelLandGeneral: finalizeCommit not yet implemented")
        }
        fun onForSaleChange() {
            System.err.println("PanelLandGeneral: onForSaleChange not yet implemented")
        }
        fun finalizeSetSellChange() {
            System.err.println("PanelLandGeneral: finalizeSetSellChange not yet implemented")
        }
        fun onSalePriceChange() {
            System.err.println("PanelLandGeneral: onSalePriceChange not yet implemented")
        }
        fun cbBuyPass(selectedOption: Int): Boolean {
            System.err.println("PanelLandGeneral: cbBuyPass not yet implemented")
            return false
        }
        fun onClickSellLand() {
            System.err.println("PanelLandGeneral: onClickSellLand not yet implemented")
        }
        fun onClickStopSellLand() {
            System.err.println("PanelLandGeneral: onClickStopSellLand not yet implemented")
        }
        fun onClickSet() {
            System.err.println("PanelLandGeneral: onClickSet not yet implemented")
        }
        fun onClickClear() {
            System.err.println("PanelLandGeneral: onClickClear not yet implemented")
        }
        fun onClickShow() {
            System.err.println("PanelLandGeneral: onClickShow not yet implemented")
        }
        fun callbackAvatarPick(names: List<String>, ids: List<UUID>) {
            System.err.println("PanelLandGeneral: callbackAvatarPick not yet implemented")
        }
        fun finalizeAvatarPick() {
            System.err.println("PanelLandGeneral: finalizeAvatarPick not yet implemented")
        }
        fun callbackHighlightTransferable(option: Int) {
            System.err.println("PanelLandGeneral: callbackHighlightTransferable not yet implemented")
        }
        fun onClickStartAuction() {
            System.err.println("PanelLandGeneral: onClickStartAuction not yet implemented")
        }
        fun confirmSaleChange(landSize: Int, salePrice: Int, authorizedName: String, callback: () -> Unit) {
            System.err.println("PanelLandGeneral: confirmSaleChange not yet implemented")
        }
        fun callbackConfirmSaleChange(option: Int) {
            System.err.println("PanelLandGeneral: callbackConfirmSaleChange not yet implemented")
        }
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
        System.err.println("PanelLandObjects: postBuild not yet implemented")
        return true
    }

    fun refresh() {
        System.err.println("PanelLandObjects: refresh not yet implemented")
    }

    open fun draw() {
        System.err.println("PanelLandObjects: draw not yet implemented")
    }

    fun callbackReturnOwnerObjects(selectedOption: Int): Boolean {
        System.err.println("PanelLandObjects: callbackReturnOwnerObjects not yet implemented")
        return false
    }

    fun callbackReturnGroupObjects(selectedOption: Int): Boolean {
        System.err.println("PanelLandObjects: callbackReturnGroupObjects not yet implemented")
        return false
    }

    fun callbackReturnOtherObjects(selectedOption: Int): Boolean {
        System.err.println("PanelLandObjects: callbackReturnOtherObjects not yet implemented")
        return false
    }

    fun callbackReturnOwnerList(selectedOption: Int): Boolean {
        System.err.println("PanelLandObjects: callbackReturnOwnerList not yet implemented")
        return false
    }

    companion object {
        fun clickShowCore(panel: PanelLandObjects, returnType: Int, list: Set<UUID>? = null) {
            System.err.println("PanelLandObjects: clickShowCore not yet implemented")
        }

        fun onClickShowOwnerObjects(panel: PanelLandObjects) { clickShowCore(panel, 0) }
        fun onClickShowGroupObjects(panel: PanelLandObjects) { clickShowCore(panel, 1) }
        fun onClickShowOtherObjects(panel: PanelLandObjects) { clickShowCore(panel, 2) }
        fun onClickReturnOwnerObjects(panel: PanelLandObjects) {
            System.err.println("PanelLandObjects: onClickReturnOwnerObjects not yet implemented")
        }
        fun onClickReturnGroupObjects(panel: PanelLandObjects) {
            System.err.println("PanelLandObjects: onClickReturnGroupObjects not yet implemented")
        }
        fun onClickReturnOtherObjects(panel: PanelLandObjects) {
            System.err.println("PanelLandObjects: onClickReturnOtherObjects not yet implemented")
        }
        fun onClickReturnOwnerList(panel: PanelLandObjects) {
            System.err.println("PanelLandObjects: onClickReturnOwnerList not yet implemented")
        }
        fun onClickRefresh(panel: PanelLandObjects) { panel.refresh() }
        fun onDoubleClickOwner(panel: PanelLandObjects) {
            System.err.println("PanelLandObjects: onDoubleClickOwner not yet implemented")
        }
        fun processParcelObjectOwnersReply(msg: Any?) {
            System.err.println("PanelLandObjects: processParcelObjectOwnersReply not yet implemented")
        }
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
        System.err.println("PanelLandOptions: postBuild not yet implemented")
        return true
    }

    open fun draw() {
        System.err.println("PanelLandOptions: draw not yet implemented")
    }

    fun refresh() {
        System.err.println("PanelLandOptions: refresh not yet implemented")
    }

    private fun refreshSearch() {
        System.err.println("PanelLandOptions: refreshSearch not yet implemented")
    }

    private fun getDirectoryFee(): Int {
        System.err.println("PanelLandOptions: getDirectoryFee not yet implemented")
        return 0
    }

    fun onClickTeleport() {
        System.err.println("PanelLandOptions: onClickTeleport not yet implemented")
    }

    companion object {
        fun onCommitAny() {
            System.err.println("PanelLandOptions: onCommitAny not yet implemented")
        }
        fun onClickSet() {
            System.err.println("PanelLandOptions: onClickSet not yet implemented")
        }
        fun onClickClear() {
            System.err.println("PanelLandOptions: onClickClear not yet implemented")
        }
        fun toggleSeeAvatars() {
            System.err.println("PanelLandOptions: toggleSeeAvatars not yet implemented")
        }
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
        System.err.println("PanelLandAccess: postBuild not yet implemented")
        return true
    }

    fun refresh() {
        System.err.println("PanelLandAccess: refresh not yet implemented")
    }

    fun refreshUi() {
        System.err.println("PanelLandAccess: refreshUi not yet implemented")
    }

    fun refreshNames() {
        System.err.println("PanelLandAccess: refreshNames not yet implemented")
    }

    open fun draw() {
        System.err.println("PanelLandAccess: draw not yet implemented")
    }

    fun onClickAddAccess() {
        System.err.println("PanelLandAccess: onClickAddAccess not yet implemented")
    }

    fun onClickAddBanned() {
        System.err.println("PanelLandAccess: onClickAddBanned not yet implemented")
    }

    fun onClickRemoveAccess() {
        System.err.println("PanelLandAccess: onClickRemoveAccess not yet implemented")
    }

    fun onClickRemoveBanned() {
        System.err.println("PanelLandAccess: onClickRemoveBanned not yet implemented")
    }

    fun callbackAvatarCBBanned(ids: List<UUID>) {
        System.err.println("PanelLandAccess: callbackAvatarCBBanned not yet implemented")
    }

    fun callbackAvatarCBBanned2(ids: List<UUID>, duration: Int) {
        System.err.println("PanelLandAccess: callbackAvatarCBBanned2 not yet implemented")
    }

    fun callbackAvatarCBAccess(ids: List<UUID>) {
        System.err.println("PanelLandAccess: callbackAvatarCBAccess not yet implemented")
    }

    fun onClickExportAccess() {
        System.err.println("PanelLandAccess: onClickExportAccess not yet implemented")
    }
    fun onClickExportBanned() {
        System.err.println("PanelLandAccess: onClickExportBanned not yet implemented")
    }
    fun onClickExportList(list: Any?, filename: String) {
        System.err.println("PanelLandAccess: onClickExportList not yet implemented")
    }
    fun exportListCallback(list: Any?, filenames: List<String>) {
        System.err.println("PanelLandAccess: exportListCallback not yet implemented")
    }
    fun onClickImportAccess() {
        System.err.println("PanelLandAccess: onClickImportAccess not yet implemented")
    }
    fun onClickImportBanned() {
        System.err.println("PanelLandAccess: onClickImportBanned not yet implemented")
    }
    fun onClickImportList(list: Any?) {
        System.err.println("PanelLandAccess: onClickImportList not yet implemented")
    }
    fun importListCallback(list: Any?, filenames: List<String>) {
        System.err.println("PanelLandAccess: importListCallback not yet implemented")
    }

    companion object {
        fun onCommitPublicAccess() {
            System.err.println("PanelLandAccess: onCommitPublicAccess not yet implemented")
        }
        fun onCommitAny() {
            System.err.println("PanelLandAccess: onCommitAny not yet implemented")
        }
        fun onCommitGroupCheck() {
            System.err.println("PanelLandAccess: onCommitGroupCheck not yet implemented")
        }
    }
}

class PanelLandCovenant(private val parcel: Any?) {

    private var textEstateOwner: Any? = null
    private var lastRegionId: UUID? = null
    private var nextUpdateTime: Double = 0.0

    fun postBuild(): Boolean {
        System.err.println("PanelLandCovenant: postBuild not yet implemented")
        return true
    }

    fun refresh() {
        System.err.println("PanelLandCovenant: refresh not yet implemented")
    }

    companion object {
        fun updateCovenant(source: Any?) {
            System.err.println("PanelLandCovenant: updateCovenant not yet implemented")
        }
        fun updateCovenantText(text: String) {
            System.err.println("PanelLandCovenant: updateCovenantText not yet implemented")
        }
        fun updateEstateName(name: String) {
            System.err.println("PanelLandCovenant: updateEstateName not yet implemented")
        }
        fun updateLastModified(text: String) {
            System.err.println("PanelLandCovenant: updateLastModified not yet implemented")
        }
        fun updateEstateOwnerName(name: String) {
            System.err.println("PanelLandCovenant: updateEstateOwnerName not yet implemented")
        }
    }
}

class PanelLandExperiences(private val parcel: Any?) {

    private var allowed: Any? = null
    private var blocked: Any? = null

    fun postBuild(): Boolean {
        System.err.println("PanelLandExperiences: postBuild not yet implemented")
        return true
    }

    fun refresh() {
        System.err.println("PanelLandExperiences: refresh not yet implemented")
    }

    fun experienceAdded(id: UUID, xpType: UInt, accessType: UInt) {
        System.err.println("PanelLandExperiences: experienceAdded not yet implemented")
    }

    fun experienceRemoved(id: UUID, accessType: UInt) {
        System.err.println("PanelLandExperiences: experienceRemoved not yet implemented")
    }

    private fun setupList(controlName: String, xpType: UInt, accessType: UInt): Any? {
        System.err.println("PanelLandExperiences: setupList not yet implemented")
        return null
    }

    private fun refreshPanel(panel: Any?, xpType: UInt) {
        System.err.println("PanelLandExperiences: refreshPanel not yet implemented")
    }
}

class PanelLandEnvironment(private val parcel: Any?) {

    private var lastParcelId: Int = 0

    fun postBuild(): Boolean {
        System.err.println("PanelLandEnvironment: postBuild not yet implemented")
        return true
    }

    fun refresh() {
        System.err.println("PanelLandEnvironment: refresh not yet implemented")
    }

    fun isRegion(): Boolean = false

    fun isLargeEnough(): Boolean {
        System.err.println("PanelLandEnvironment: isLargeEnough not yet implemented")
        return false
    }

    fun getParcel(): Any? {
        System.err.println("PanelLandEnvironment: getParcel not yet implemented")
        return null
    }
    fun canEdit(): Boolean {
        System.err.println("PanelLandEnvironment: canEdit not yet implemented")
        return false
    }
    fun getParcelId(): Int {
        System.err.println("PanelLandEnvironment: getParcelId not yet implemented")
        return 0
    }

    private fun refreshFromSource() {
        System.err.println("PanelLandEnvironment: refreshFromSource not yet implemented")
    }

    private fun isSameRegion(): Boolean {
        System.err.println("PanelLandEnvironment: isSameRegion not yet implemented")
        return false
    }
}
