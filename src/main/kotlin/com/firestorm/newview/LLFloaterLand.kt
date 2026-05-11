package com.firestorm.newview

import java.util.UUID

val CACHE_REFRESH_TIME: Float = 2.5f

fun sendParcelSelectObjects(parcelLocalId: Int, returnType: UInt, returnIds: MutableSet<UUID>? = null) {
    TODO("APR: use JVM equivalent for LLMessageSystem::send ParcelSelectObjects")
}

fun sendOtherCleanTimeMessage(parcelLocalId: Int, otherCleanTime: Int) {
    TODO("APR: use JVM equivalent for LLMessageSystem::send ParcelSetOtherCleanTime")
}

fun sendReturnObjectsMessage(parcelLocalId: Int, returnType: Int, ownerIds: MutableSet<UUID>? = null) {
    TODO("APR: use JVM equivalent for LLMessageSystem::send ParcelReturnObjects")
}

class LLFloaterLand(seed: LLSD) : LLFloater(seed) {

    companion object {
        var sObserver: LLParcelSelectionObserver? = null
        var sLastTab: Int = 0
        var sRequestReplyOnUpdate: Boolean = true

        fun refreshAll() {
            LLFloaterReg.findTypedInstance<LLFloaterLand>("about_land")?.refresh()
        }

        fun getCurrentPanelLandObjects(): LLPanelLandObjects? =
            LLFloaterReg.getTypedInstance<LLFloaterLand>("about_land")?.mPanelObjects

        fun getCurrentPanelLandCovenant(): LLPanelLandCovenant? =
            LLFloaterReg.getTypedInstance<LLFloaterLand>("about_land")?.mPanelCovenant
    }

    var mTabLand: LLTabContainer? = null
    var mPanelGeneral: LLPanelLandGeneral? = null
    var mPanelObjects: LLPanelLandObjects? = null
    var mPanelOptions: LLPanelLandOptions? = null
    var mPanelAudio: LLPanelLandAudio? = null
    var mPanelMedia: LLPanelLandMedia? = null
    var mPanelAccess: LLPanelLandAccess? = null
    var mPanelCovenant: LLPanelLandCovenant? = null
    var mPanelExperiences: LLPanelLandExperiences? = null
    var mPanelEnvironment: LLPanelLandEnvironment? = null

    var mParcel: LLParcelSelection? = null

    init {
        mFactoryMap["land_general_panel"]      = { createPanelLandGeneral() }
        mFactoryMap["land_covenant_panel"]     = { createPanelLandCovenant() }
        mFactoryMap["land_objects_panel"]      = { createPanelLandObjects() }
        mFactoryMap["land_options_panel"]      = { createPanelLandOptions() }
        mFactoryMap["land_audio_panel"]        = { createPanelLandAudio() }
        mFactoryMap["land_media_panel"]        = { createPanelLandMedia() }
        mFactoryMap["land_access_panel"]       = { createPanelLandAccess() }
        mFactoryMap["land_experiences_panel"]  = { createPanelLandExperiences() }
        mFactoryMap["land_environment_panel"]  = { createPanelLandEnvironment() }

        sObserver = LLParcelSelectionObserver()
        LLViewerParcelMgr.getInstance().addObserver(sObserver!!)
    }

    override fun postBuild(): Boolean {
        setVisibleCallback { visible -> onVisibilityChanged(visible) }
        mTabLand = getChild<LLTabContainer>("landtab")
        mTabLand?.selectTab(sLastTab)
        return true
    }

    override fun onDestroy() {
        LLViewerParcelMgr.getInstance().removeObserver(sObserver!!)
        sObserver = null
    }

    override fun onOpen(key: LLSD) {
        if (LLViewerParcelMgr.getInstance().selectionEmpty()) {
            LLViewerParcelMgr.getInstance().selectParcelAt(gAgent.getPositionGlobal())
        }
        mParcel = LLViewerParcelMgr.getInstance().getFloatingParcelSelection()

        val selectedRegion = LLViewerParcelMgr.getInstance().getSelectionRegion()
        if (selectedRegion == null || !selectedRegion.isCapabilityAvailable("RegionExperiences")) {
            mTabLand?.removeTabPanel(mTabLand?.getPanelByName("land_experiences_panel"))
        }

        refresh()
    }

    private fun onVisibilityChanged(visible: Boolean) {
        if (!visible) {
            LLSelectMgr.getInstance().unhighlightAll()
            sLastTab = mTabLand?.getCurrentPanelIndex() ?: 0
        }
    }

    open fun refresh() {
        mPanelGeneral?.refresh()
        mPanelObjects?.refresh()
        mPanelOptions?.refresh()
        mPanelAudio?.refresh()
        mPanelMedia?.refresh()
        mPanelAccess?.refresh()
        mPanelCovenant?.refresh()
        mPanelExperiences?.refresh()
        mPanelEnvironment?.refresh()
    }

    fun getCurrentSelectedParcel(): LLParcel? = mParcel?.getParcel()

    private fun createPanelLandGeneral(): LLPanelLandGeneral {
        mPanelGeneral = LLPanelLandGeneral(mParcel)
        return mPanelGeneral!!
    }

    private fun createPanelLandCovenant(): LLPanelLandCovenant {
        mPanelCovenant = LLPanelLandCovenant(mParcel)
        return mPanelCovenant!!
    }

    private fun createPanelLandObjects(): LLPanelLandObjects {
        mPanelObjects = LLPanelLandObjects(mParcel)
        return mPanelObjects!!
    }

    private fun createPanelLandOptions(): LLPanelLandOptions {
        mPanelOptions = LLPanelLandOptions(mParcel)
        return mPanelOptions!!
    }

    private fun createPanelLandAudio(): LLPanelLandAudio {
        mPanelAudio = LLPanelLandAudio(mParcel)
        return mPanelAudio!!
    }

    private fun createPanelLandMedia(): LLPanelLandMedia {
        mPanelMedia = LLPanelLandMedia(mParcel)
        return mPanelMedia!!
    }

    private fun createPanelLandAccess(): LLPanelLandAccess {
        mPanelAccess = LLPanelLandAccess(mParcel)
        return mPanelAccess!!
    }

    private fun createPanelLandExperiences(): LLPanelLandExperiences {
        mPanelExperiences = LLPanelLandExperiences(mParcel)
        return mPanelExperiences!!
    }

    private fun createPanelLandEnvironment(): LLPanelLandEnvironment {
        mPanelEnvironment = LLPanelLandEnvironment(mParcel)
        return mPanelEnvironment!!
    }
}

class LLParcelSelectionObserver : LLParcelObserver() {
    override fun changed() { LLFloaterLand.refreshAll() }
}

class LLPanelLandGeneral(
    private val mParcel: LLParcelSelection?
) : LLPanel(), LLRemoteParcelInfoObserver {

    companion object {
        var sSelectionForBuyPass: LLParcelSelection? = null
        var sBuyPassDialogHandle: LLHandle<LLFloater>? = null

        fun onClickDeed() {
            LLViewerParcelMgr.getInstance().startDeedLandToGroup()
        }

        fun onClickBuyLand(forGroup: Boolean) {
            LLViewerParcelMgr.getInstance().startBuyLand(forGroup)
        }

        fun onClickScriptLimits(panel: LLPanelLandGeneral) {
            if (panel.mParcel?.getParcel() != null) {
                LLFloaterReg.showInstance("script_limits")
            }
        }

        fun onClickRelease() {
            LLViewerParcelMgr.getInstance().startReleaseLand()
        }

        fun onClickReclaim() {
            LLViewerParcelMgr.getInstance().reclaimParcel()
        }

        fun onClickBuyPass(panel: LLPanelLandGeneral) {
            val parcel = panel.mParcel?.getParcel() ?: return
            val args = mutableMapOf(
                "COST" to parcel.getPassPrice().toString(),
                "PARCEL_NAME" to parcel.getName(),
                "TIME" to "%.2f".format(parcel.getPassHours())
            )
            sSelectionForBuyPass = LLViewerParcelMgr.getInstance().getParcelSelection()
            LLNotificationsUtil.add("LandBuyPass", args, emptyMap()) { notification, response ->
                cbBuyPass(notification, response)
            }
        }

        fun enableBuyPass(panel: LLPanelLandGeneral?): Boolean {
            val parcel = panel?.mParcel?.getParcel()
                ?: LLViewerParcelMgr.getInstance().getParcelSelection()?.getParcel()
            return parcel != null && parcel.getParcelFlag(PF_USE_PASS_LIST) &&
                !LLViewerParcelMgr.getInstance().isCollisionBanned()
        }

        fun cbBuyPass(notification: LLSD, response: LLSD): Boolean {
            if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
                LLViewerParcelMgr.getInstance().buyPass()
            }
            sSelectionForBuyPass = null
            return false
        }

        fun onClickSellLand(panel: LLPanelLandGeneral) {
            LLViewerParcelMgr.getInstance().startSellLand()
            panel.refresh()
        }

        fun onClickStopSellLand(panel: LLPanelLandGeneral) {
            val parcel = panel.mParcel?.getParcel() ?: return
            parcel.setParcelFlag(PF_FOR_SALE, false)
            parcel.setSalePrice(0)
            parcel.setAuthorizedBuyerID(null)
            LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(parcel)
        }

        fun onClickStartAuction(panel: LLPanelLandGeneral) {
            val parcel = panel.mParcel?.getParcel() ?: return
            if (parcel.getForSale()) {
                LLNotificationsUtil.add("CannotStartAuctionAlreadyForSale")
            } else {
                LLFloaterReg.showInstance("auction")
            }
        }

        fun onCommitAny(panel: LLPanelLandGeneral) {
            val parcel = panel.mParcel?.getParcel() ?: return
            parcel.setName(panel.mEditName?.getText() ?: "")
            parcel.setDesc(panel.mEditDesc?.getText() ?: "")
            parcel.setParcelFlag(PF_ALLOW_DEED_TO_GROUP, panel.mCheckDeedToGroup?.get() == true)
            parcel.setContributeWithDeed(panel.mCheckContributeWithDeed?.get() == true)
            LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(parcel)
            panel.refresh()
        }

        fun confirmSaleChange(
            landSize: Int, salePrice: Int, authorizedName: String,
            callback: () -> Unit
        ) {
            TODO("APR: use JVM equivalent for sale-change confirmation dialog")
        }
    }

    var mUncheckedSell: Boolean = false
    var mLastParcelLocalID: Int = 0

    var mEditName: LLLineEditor? = null
    var mEditDesc: LLTextEditor? = null
    var mEditUUID: LLLineEditor? = null
    var mTextSalePending: LLTextBox? = null
    var mBtnDeedToGroup: LLButton? = null
    var mBtnSetGroup: LLButton? = null
    var mTextOwnerLabel: LLTextBox? = null
    var mTextOwner: LLTextBox? = null
    var mContentRating: LLTextBox? = null
    var mLandType: LLTextBox? = null
    var mTextGroup: LLTextBox? = null
    var mTextGroupLabel: LLTextBox? = null
    var mTextClaimDateLabel: LLTextBox? = null
    var mTextClaimDate: LLTextBox? = null
    var mTextPriceLabel: LLTextBox? = null
    var mTextPrice: LLTextBox? = null
    var mCheckDeedToGroup: LLCheckBoxCtrl? = null
    var mCheckContributeWithDeed: LLCheckBoxCtrl? = null
    var mSaleInfoForSale1: LLTextBox? = null
    var mSaleInfoForSale2: LLTextBox? = null
    var mSaleInfoForSaleObjects: LLTextBox? = null
    var mSaleInfoForSaleNoObjects: LLTextBox? = null
    var mSaleInfoNotForSale: LLTextBox? = null
    var mBtnSellLand: LLButton? = null
    var mBtnStopSellLand: LLButton? = null
    var mTextDwell: LLTextBox? = null
    var mBtnBuyLand: LLButton? = null
    var mBtnScriptLimits: LLButton? = null
    var mBtnBuyGroupLand: LLButton? = null
    var mBtnReleaseLand: LLButton? = null
    var mBtnReclaimLand: LLButton? = null
    var mBtnBuyPass: LLButton? = null
    var mBtnStartAuction: LLButton? = null

    override fun postBuild(): Boolean {
        mEditName = getChild<LLLineEditor>("Name")
        mEditName?.setCommitCallback { onCommitAny(this) }
        mEditName?.setPrevalidate(LLTextValidate::validateASCIIPrintableNoPipe)

        mEditUUID = getChild<LLLineEditor>("UUID")
        mEditUUID?.setEnabled(false)

        mEditDesc = getChild<LLTextEditor>("Description")
        mEditDesc?.setCommitOnFocusLost(true)
        mEditDesc?.setCommitCallback { onCommitAny(this) }
        mEditDesc?.setContentTrusted(false)

        mTextSalePending   = getChild<LLTextBox>("SalePending")
        mTextOwnerLabel    = getChild<LLTextBox>("Owner:")
        mTextOwner         = getChild<LLTextBox>("OwnerText")
        mTextOwner?.setIsFriendCallback(LLAvatarActions::isFriend)
        mContentRating     = getChild<LLTextBox>("ContentRatingText")
        mLandType          = getChild<LLTextBox>("LandTypeText")
        mTextGroupLabel    = getChild<LLTextBox>("Group:")
        mTextGroup         = getChild<LLTextBox>("GroupText")

        mBtnSetGroup = getChild<LLButton>("Set...")
        mBtnSetGroup?.setCommitCallback { onClickSetGroup() }

        mCheckDeedToGroup = getChild<LLCheckBoxCtrl>("check deed")
        mCheckDeedToGroup?.setCommitCallback { onCommitAny(this) }

        mBtnDeedToGroup = getChild<LLButton>("Deed...")
        mBtnDeedToGroup?.setClickedCallback { onClickDeed() }

        mCheckContributeWithDeed = getChild<LLCheckBoxCtrl>("check contrib")
        mCheckContributeWithDeed?.setCommitCallback { onCommitAny(this) }

        mSaleInfoNotForSale     = getChild<LLTextBox>("Not for sale.")
        mSaleInfoForSale1       = getChild<LLTextBox>("For Sale: Price L\$[PRICE].")
        mBtnSellLand            = getChild<LLButton>("Sell Land...")
        mBtnSellLand?.setClickedCallback { onClickSellLand(this) }
        mSaleInfoForSale2       = getChild<LLTextBox>("For sale to")
        mSaleInfoForSaleObjects = getChild<LLTextBox>("Sell with landowners objects in parcel.")
        mSaleInfoForSaleNoObjects = getChild<LLTextBox>("Selling with no objects in parcel.")
        mBtnStopSellLand        = getChild<LLButton>("Cancel Land Sale")
        mBtnStopSellLand?.setClickedCallback { onClickStopSellLand(this) }

        mTextClaimDateLabel = getChild<LLTextBox>("Claimed:")
        mTextClaimDate      = getChild<LLTextBox>("DateClaimText")
        mTextPriceLabel     = getChild<LLTextBox>("PriceLabel")
        mTextPrice          = getChild<LLTextBox>("PriceText")
        mTextDwell          = getChild<LLTextBox>("DwellText")

        mBtnBuyLand      = getChild<LLButton>("Buy Land...")
        mBtnBuyLand?.setClickedCallback { onClickBuyLand(false) }
        mBtnBuyGroupLand = getChild<LLButton>("Buy For Group...")
        mBtnBuyGroupLand?.setClickedCallback { onClickBuyLand(true) }
        mBtnBuyPass      = getChild<LLButton>("Buy Pass...")
        mBtnBuyPass?.setClickedCallback { onClickBuyPass(this) }
        mBtnReleaseLand  = getChild<LLButton>("Abandon Land...")
        mBtnReleaseLand?.setClickedCallback { onClickRelease() }
        mBtnReclaimLand  = getChild<LLButton>("Reclaim Land...")
        mBtnReclaimLand?.setClickedCallback { onClickReclaim() }
        mBtnStartAuction = getChild<LLButton>("Linden Sale...")
        mBtnStartAuction?.setClickedCallback { onClickStartAuction(this) }
        mBtnStartAuction?.setEnabled(LLGridManager.instance().isInSecondLife())

        mBtnScriptLimits = getChild<LLButton>("Scripts...")
        val url = gAgent.getRegionCapability("LandResources")
        if (url.isNotEmpty()) {
            mBtnScriptLimits?.setClickedCallback { onClickScriptLimits(this) }
        } else {
            mBtnScriptLimits?.setVisible(false)
        }

        return true
    }

    open fun refresh() {
        mEditName?.setEnabled(false)
        mEditName?.setText("")
        mEditUUID?.setText("")
        mEditDesc?.setEnabled(false)
        mEditDesc?.setText(getString("no_selection_text"))
        mTextSalePending?.setText("")
        mTextSalePending?.setEnabled(false)
        mBtnDeedToGroup?.setEnabled(false)
        mBtnSetGroup?.setEnabled(false)
        mBtnStartAuction?.setEnabled(false)
        mCheckDeedToGroup?.set(false)
        mCheckDeedToGroup?.setEnabled(false)
        mCheckContributeWithDeed?.set(false)
        mCheckContributeWithDeed?.setEnabled(false)
        mTextOwner?.setText("")
        mContentRating?.setText("")
        mLandType?.setText("")
        mTextClaimDate?.setText("")
        mTextGroup?.setText("")
        mTextPrice?.setText("")
        mSaleInfoForSale1?.setVisible(false)
        mSaleInfoForSale2?.setVisible(false)
        mSaleInfoForSaleObjects?.setVisible(false)
        mSaleInfoForSaleNoObjects?.setVisible(false)
        mSaleInfoNotForSale?.setVisible(false)
        mBtnSellLand?.setVisible(false)
        mBtnStopSellLand?.setVisible(false)
        mTextPriceLabel?.setText("")
        mTextDwell?.setText("")
        mBtnBuyLand?.setEnabled(false)
        mBtnScriptLimits?.setEnabled(false)
        mBtnBuyGroupLand?.setEnabled(false)
        mBtnReleaseLand?.setEnabled(false)
        mBtnReclaimLand?.setEnabled(false)
        mBtnBuyPass?.setEnabled(false)

        if (gDisconnected) return

        mBtnStartAuction?.setVisible(gAgent.isGodlike())
        val regionp = LLViewerParcelMgr.getInstance().getSelectionRegion()
        val regionOwner = regionp != null && regionp.getOwner() == gAgent.getID()

        if (regionOwner) {
            mBtnReleaseLand?.setVisible(false)
            mBtnReclaimLand?.setVisible(true)
        } else {
            mBtnReleaseLand?.setVisible(true)
            mBtnReclaimLand?.setVisible(false)
        }

        val parcel = mParcel?.getParcel() ?: return

        val isLeased = parcel.getOwnershipStatus() == LLParcel.OS_LEASED
        val regionXfer = regionp != null && !regionp.getRegionFlag(REGION_FLAGS_BLOCK_LAND_RESELL)

        if (regionp != null) {
            insertMaturityIntoTextbox(mContentRating, MATURITY)
            mLandType?.setText(regionp.getLocalizedSimProductName())
        }

        val estateManagerSellable = !parcel.getAuctionID().isValid() &&
            gAgent.canManageEstate() && regionp != null &&
            parcel.getOwnerID() == regionp.getOwner()
        val ownerSellable = regionXfer && !parcel.getAuctionID().isValid() &&
            LLViewerParcelMgr.isParcelModifiableByAgent(parcel, GP_LAND_SET_SALE_INFO)
        val canBeSold = ownerSellable || estateManagerSellable

        val ownerId = parcel.getOwnerID()
        val isPublic = parcel.isPublic()

        if (isPublic) {
            mTextSalePending?.setText("")
            mTextSalePending?.setEnabled(false)
            mTextOwner?.setText(getString("public_text"))
            mTextOwner?.setEnabled(false)
            mTextClaimDate?.setText("")
            mTextClaimDate?.setEnabled(false)
            mTextGroup?.setText(getString("none_text"))
            mTextGroup?.setEnabled(false)
            mBtnStartAuction?.setEnabled(false)
        } else {
            when {
                !isLeased && ownerId == gAgent.getID() -> {
                    mTextSalePending?.setText(getString("need_tier_to_modify"))
                    mTextSalePending?.setEnabled(true)
                }
                parcel.getAuctionID().isValid() -> {
                    mTextSalePending?.setText(getString("auction_id_text"))
                    mTextSalePending?.setTextArg("[ID]", parcel.getAuctionID().toString())
                    mTextSalePending?.setEnabled(true)
                }
                else -> {
                    mTextSalePending?.setText("")
                    mTextSalePending?.setEnabled(false)
                }
            }
            mTextOwner?.setEnabled(true)

            if (parcel.getGroupID() == null) {
                mTextGroup?.setText(getString("none_text"))
                mTextGroup?.setEnabled(false)
            } else {
                mTextGroup?.setEnabled(true)
            }

            val claimDate = parcel.getClaimDate()
            val use24h = gSavedSettings.getBool("Use24HourClock")
            val claimDateTemplate = if (use24h) getString("time_stamp_template") else getString("time_stamp_template_ampm")
            mTextClaimDate?.setText(formatDateTime(claimDateTemplate, claimDate))
            mTextClaimDate?.setEnabled(isLeased)

            val enableAuction = gAgent.getGodLevel() >= GOD_LIAISON &&
                ownerId == GOVERNOR_LINDEN_ID && !parcel.getAuctionID().isValid()
            mBtnStartAuction?.setEnabled(enableAuction)
        }

        val canEditIdentity = LLViewerParcelMgr.isParcelModifiableByAgent(parcel, GP_LAND_CHANGE_IDENTITY)
        mEditName?.setEnabled(canEditIdentity)
        mEditDesc?.setEnabled(canEditIdentity)
        mEditDesc?.setParseURLs(!canEditIdentity)

        val canEditAgentOnly = LLViewerParcelMgr.isParcelModifiableByAgent(parcel, GP_NO_POWERS)
        mBtnSetGroup?.setEnabled(canEditAgentOnly && !parcel.getIsGroupOwned())

        val groupId = parcel.getGroupID()
        val enableDeed = ownerId == gAgent.getID() && groupId != null && gAgent.isInGroup(groupId)
        mCheckDeedToGroup?.setEnabled(enableDeed)
        mCheckDeedToGroup?.set(parcel.getAllowDeedToGroup())
        mCheckContributeWithDeed?.setEnabled(enableDeed && parcel.getAllowDeedToGroup())
        mCheckContributeWithDeed?.set(parcel.getContributeWithDeed())

        val canDeed = gAgent.hasPowerInGroup(groupId, GP_LAND_DEED)
        mBtnDeedToGroup?.setEnabled(
            parcel.getAllowDeedToGroup() && groupId != null && canDeed && !parcel.getIsGroupOwned()
        )

        mEditName?.setText(parcel.getName())
        mEditDesc?.setText(parcel.getDesc())
        mEditUUID?.setText("")

        var forSale = parcel.getForSale()
        mBtnSellLand?.setVisible(false)
        mBtnStopSellLand?.setVisible(false)

        var area = 0; var claimPrice = 0; var rentPrice = 0; var dwell = DWELL_NAN
        LLViewerParcelMgr.getInstance().getDisplayInfo(area, claimPrice, rentPrice, forSale, dwell)

        val areaStr = getString("area_size_text").replace("[AREA]", area.toString())
        mTextPriceLabel?.setText(getString("area_text"))
        mTextPrice?.setText(areaStr)

        if (dwell == DWELL_NAN) {
            mTextDwell?.setText(LLTrans.getString("LoadingData"))
        } else {
            mTextDwell?.setText("%.0f".format(dwell))
        }

        if (forSale) {
            mSaleInfoForSale1?.setVisible(true)
            mSaleInfoForSale2?.setVisible(true)
            if (parcel.getSellWithObjects()) {
                mSaleInfoForSaleObjects?.setVisible(true)
                mSaleInfoForSaleNoObjects?.setVisible(false)
            } else {
                mSaleInfoForSaleObjects?.setVisible(false)
                mSaleInfoForSaleNoObjects?.setVisible(true)
            }
            mSaleInfoNotForSale?.setVisible(false)
            val costPerSqm = if (area > 0) parcel.getSalePrice().toFloat() / area.toFloat() else 0f
            mSaleInfoForSale1?.setTextArg("[PRICE]", LLResMgr.getInstance().getMonetaryString(parcel.getSalePrice()))
            mSaleInfoForSale1?.setTextArg("[PRICE_PER_SQM]", "%.1f".format(costPerSqm))
            if (canBeSold) mBtnStopSellLand?.setVisible(true)
        } else {
            mSaleInfoForSale1?.setVisible(false)
            mSaleInfoForSale2?.setVisible(false)
            mSaleInfoForSaleObjects?.setVisible(false)
            mSaleInfoForSaleNoObjects?.setVisible(false)
            mSaleInfoNotForSale?.setVisible(true)
            if (canBeSold) mBtnSellLand?.setVisible(true)
        }

        refreshNames()

        mBtnBuyLand?.setEnabled(LLViewerParcelMgr.getInstance().canAgentBuyParcel(parcel, false))
        mBtnScriptLimits?.setEnabled(true)
        mBtnBuyGroupLand?.setEnabled(LLViewerParcelMgr.getInstance().canAgentBuyParcel(parcel, true))

        if (regionOwner) {
            mBtnReclaimLand?.setEnabled(!isPublic && parcel.getOwnerID() != gAgent.getID())
        } else {
            val isOwnerRelease    = LLViewerParcelMgr.isParcelOwnedByAgent(parcel, GP_LAND_RELEASE)
            val isManagerRelease  = gAgent.canManageEstate() && regionp != null &&
                parcel.getOwnerID() != regionp.getOwner()
            mBtnReleaseLand?.setEnabled(isOwnerRelease || isManagerRelease)
        }

        val usePass = parcel.getOwnerID() != gAgent.getID() &&
            parcel.getParcelFlag(PF_USE_PASS_LIST) &&
            !LLViewerParcelMgr.getInstance().isCollisionBanned()
        mBtnBuyPass?.setEnabled(usePass)

        if (regionp != null && gAgent.getRegion()?.getRegionID() == regionp.getRegionID() &&
            (mLastParcelLocalID == 0 || mLastParcelLocalID != parcel.getLocalID())) {
            mLastParcelLocalID = parcel.getLocalID()
            val capUrl = regionp.getCapability("RemoteParcelRequest")
            if (capUrl.isNotEmpty()) {
                val regionId    = regionp.getRegionID()
                val posGlobal   = regionp.getOriginGlobal()
                LLRemoteParcelInfoProcessor.instance().requestRegionParcelInfo(
                    capUrl, regionId, parcel.getCenterpoint(), posGlobal, getObserverHandle()
                )
            } else {
                mEditUUID?.setText(getString("error_resolving_uuid"))
            }
        }
    }

    fun refreshNames() {
        val parcel = mParcel?.getParcel()
        if (parcel == null) {
            mTextOwner?.setText("")
            mTextGroup?.setText("")
            return
        }
        val owner = if (parcel.getIsGroupOwned()) {
            getString("group_owned_text")
        } else {
            LLSLURL("agent", parcel.getOwnerID(), "inspect").getSLURLString()
        }.let { name ->
            if (parcel.getOwnershipStatus() == LLParcel.OS_LEASE_PENDING) name + getString("sale_pending_text")
            else name
        }
        mTextOwner?.setText(owner)

        val group = parcel.getGroupID()?.let { LLSLURL("group", it, "inspect").getSLURLString() } ?: ""
        mTextGroup?.setText(group)

        if (parcel.getForSale()) {
            val authBuyerId = parcel.getAuthorizedBuyerID()
            val buyerName = if (authBuyerId != null) {
                LLSLURL("agent", authBuyerId, "inspect").getSLURLString()
            } else {
                getString("anyone")
            }
            mSaleInfoForSale2?.setTextArg("[BUYER]", buyerName)
        }
    }

    open fun draw() { super.draw() }

    fun onClickSetGroup() {
        val fg = LLFloaterReg.showTypedInstance<LLFloaterGroupPicker>("group_picker", LLSD(gAgent.getID()))
        fg?.setSelectGroupCallback { groupId -> setGroup(groupId) }
    }

    fun setGroup(groupId: UUID) {
        val parcel = mParcel?.getParcel() ?: return
        parcel.setGroupID(groupId)
        LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(parcel)
        refresh()
    }

    override fun processParcelInfo(parcelData: LLParcelData) {}

    override fun setParcelID(parcelId: UUID) {
        mEditUUID?.setText(parcelId.toString())
    }

    override fun setErrorStatus(status: Int, reason: String) {
        mEditUUID?.setText(getString("error_resolving_uuid"))
        mLastParcelLocalID = 0
    }
}

class LLPanelLandObjects(private val mParcel: LLParcelSelection?) : LLPanel() {

    var mParcelObjectBonus: LLTextBox? = null
    var mSWTotalObjects: LLTextBox? = null
    var mObjectContribution: LLTextBox? = null
    var mTotalObjects: LLTextBox? = null
    var mOwnerObjects: LLTextBox? = null
    var mBtnShowOwnerObjects: LLButton? = null
    var mBtnReturnOwnerObjects: LLButton? = null
    var mGroupObjects: LLTextBox? = null
    var mBtnShowGroupObjects: LLButton? = null
    var mBtnReturnGroupObjects: LLButton? = null
    var mOtherObjects: LLTextBox? = null
    var mBtnShowOtherObjects: LLButton? = null
    var mBtnReturnOtherObjects: LLButton? = null
    var mSelectedObjects: LLTextBox? = null
    var mCleanOtherObjectsTime: LLLineEditor? = null
    var mOtherTime: Int = 0
    var mBtnRefresh: LLButton? = null
    var mBtnReturnOwnerList: LLButton? = null
    var mOwnerList: LLNameListCtrl? = null

    var mIconAvatarOnline: LLUIImage? = null
    var mIconAvatarOffline: LLUIImage? = null
    var mIconGroup: LLUIImage? = null

    var mFirstReply: Boolean = true
    val mSelectedOwners: MutableSet<UUID> = mutableSetOf()
    var mSelectedName: String = ""
    var mSelectedCount: Int = 0
    var mSelectedIsGroup: Boolean = false

    override fun postBuild(): Boolean {
        mFirstReply           = true
        mParcelObjectBonus    = getChild<LLTextBox>("parcel_object_bonus")
        mSWTotalObjects       = getChild<LLTextBox>("objects_available")
        mObjectContribution   = getChild<LLTextBox>("object_contrib_text")
        mTotalObjects         = getChild<LLTextBox>("total_objects_text")
        mOwnerObjects         = getChild<LLTextBox>("owner_objects_text")

        mBtnShowOwnerObjects    = getChild<LLButton>("ShowOwner")
        mBtnShowOwnerObjects?.setClickedCallback { onClickShowOwnerObjects(this) }
        mBtnReturnOwnerObjects  = getChild<LLButton>("ReturnOwner...")
        mBtnReturnOwnerObjects?.setClickedCallback { onClickReturnOwnerObjects(this) }

        mGroupObjects           = getChild<LLTextBox>("group_objects_text")
        mBtnShowGroupObjects    = getChild<LLButton>("ShowGroup")
        mBtnShowGroupObjects?.setClickedCallback { onClickShowGroupObjects(this) }
        mBtnReturnGroupObjects  = getChild<LLButton>("ReturnGroup...")
        mBtnReturnGroupObjects?.setClickedCallback { onClickReturnGroupObjects(this) }

        mOtherObjects           = getChild<LLTextBox>("other_objects_text")
        mBtnShowOtherObjects    = getChild<LLButton>("ShowOther")
        mBtnShowOtherObjects?.setClickedCallback { onClickShowOtherObjects(this) }
        mBtnReturnOtherObjects  = getChild<LLButton>("ReturnOther...")
        mBtnReturnOtherObjects?.setClickedCallback { onClickReturnOtherObjects(this) }

        mSelectedObjects        = getChild<LLTextBox>("selected_objects_text")
        mCleanOtherObjectsTime  = getChild<LLLineEditor>("clean other time")
        mCleanOtherObjectsTime?.setFocusLostCallback { onLostFocus(this) }
        mCleanOtherObjectsTime?.setCommitCallback   { onCommitClean(this) }
        mCleanOtherObjectsTime?.setPrevalidate(LLTextValidate::validateNonNegativeS32)

        mBtnRefresh          = getChild<LLButton>("Refresh List")
        mBtnRefresh?.setClickedCallback { onClickRefresh(this) }
        mBtnReturnOwnerList  = getChild<LLButton>("Return objects...")
        mBtnReturnOwnerList?.setClickedCallback { onClickReturnOwnerList(this) }

        mIconAvatarOnline  = LLUIImageList.getInstance().getUIImage("icon_avatar_online.tga", 0)
        mIconAvatarOffline = LLUIImageList.getInstance().getUIImage("icon_avatar_offline.tga", 0)
        mIconGroup         = LLUIImageList.getInstance().getUIImage("icon_group.tga", 0)

        mOwnerList = getChild<LLNameListCtrl>("owner list")
        mOwnerList?.setIsFriendCallback(LLAvatarActions::isFriend)
        mOwnerList?.sortByColumnIndex(3, false)
        mOwnerList?.setCommitCallback { onCommitList(this) }
        mOwnerList?.setDoubleClickCallback { onDoubleClickOwner(this) }
        mOwnerList?.setContextMenu(gFSNameListAvatarMenu)

        return true
    }

    open fun refresh() {
        mBtnShowOwnerObjects?.setEnabled(false)
        mBtnShowGroupObjects?.setEnabled(false)
        mBtnShowOtherObjects?.setEnabled(false)
        mBtnReturnOwnerObjects?.setEnabled(false)
        mBtnReturnGroupObjects?.setEnabled(false)
        mBtnReturnOtherObjects?.setEnabled(false)
        mCleanOtherObjectsTime?.setEnabled(false)
        mBtnRefresh?.setEnabled(false)
        mBtnReturnOwnerList?.setEnabled(false)
        mSelectedOwners.clear()
        mOwnerList?.deleteAllItems()
        mOwnerList?.setEnabled(false)

        val parcel = mParcel?.getParcel()
        if (parcel == null || gDisconnected) {
            for (ctrl in listOf(mSWTotalObjects, mObjectContribution, mTotalObjects,
                mOwnerObjects, mGroupObjects, mOtherObjects, mSelectedObjects)) {
                ctrl?.setTextArg("[COUNT]", "0")
            }
            mSWTotalObjects?.setTextArg("[TOTAL]", "0")
            mSWTotalObjects?.setTextArg("[AVAILABLE]", "0")
            return
        }

        val swMax   = parcel.getSimWideMaxPrimCapacity()
        val swTotal = parcel.getSimWidePrimCount()
        var max     = Math.round(parcel.getMaxPrimCapacity() * parcel.getParcelPrimBonus())
        val total   = parcel.getPrimCount()
        val owned   = parcel.getOwnerPrimCount()
        val group   = parcel.getGroupPrimCount()
        val other   = parcel.getOtherPrimCount()
        val selected = parcel.getSelectedPrimCount()
        val bonus   = parcel.getParcelPrimBonus()
        mOtherTime  = parcel.getCleanOtherTime()

        val region = LLViewerParcelMgr.getInstance().getSelectionRegion()
        if (region != null) {
            val maxTasksPerRegion = region.getMaxTasks()
            val effectiveSwMax = minOf(swMax, maxTasksPerRegion)
            max = minOf(max, maxTasksPerRegion)
        }

        if (bonus != 1.0f) {
            mParcelObjectBonus?.setVisible(true)
            mParcelObjectBonus?.setTextArg("[BONUS]", "%.2f".format(bonus))
        } else {
            mParcelObjectBonus?.setVisible(false)
        }

        if (swTotal > swMax) {
            mSWTotalObjects?.setText(getString("objects_deleted_text"))
            mSWTotalObjects?.setTextArg("[DELETED]", (swTotal - swMax).toString())
        } else {
            mSWTotalObjects?.setText(getString("objects_available_text"))
            mSWTotalObjects?.setTextArg("[AVAILABLE]", (swMax - swTotal).toString())
        }
        mSWTotalObjects?.setTextArg("[COUNT]", swTotal.toString())
        mSWTotalObjects?.setTextArg("[MAX]", swMax.toString())
        mObjectContribution?.setTextArg("[COUNT]", max.toString())
        mTotalObjects?.setTextArg("[COUNT]", total.toString())
        mOwnerObjects?.setTextArg("[COUNT]", owned.toString())
        mGroupObjects?.setTextArg("[COUNT]", group.toString())
        mOtherObjects?.setTextArg("[COUNT]", other.toString())
        mSelectedObjects?.setTextArg("[COUNT]", selected.toString())
        mCleanOtherObjectsTime?.setText(mOtherTime.toString())

        val canReturnOwned     = LLViewerParcelMgr.isParcelModifiableByAgent(parcel, GP_LAND_RETURN_GROUP_OWNED)
        val canReturnGroupSet  = LLViewerParcelMgr.isParcelModifiableByAgent(parcel, GP_LAND_RETURN_GROUP_SET)
        val canReturnOther     = LLViewerParcelMgr.isParcelModifiableByAgent(parcel, GP_LAND_RETURN_NON_GROUP)

        if (canReturnOwned || canReturnGroupSet || canReturnOther) {
            if (owned > 0 && canReturnOwned) {
                mBtnShowOwnerObjects?.setEnabled(true)
                mBtnReturnOwnerObjects?.setEnabled(true)
            }
            if (group > 0 && canReturnGroupSet) {
                mBtnShowGroupObjects?.setEnabled(true)
                mBtnReturnGroupObjects?.setEnabled(true)
            }
            if (other > 0 && canReturnOther) {
                mBtnShowOtherObjects?.setEnabled(true)
                mBtnReturnOtherObjects?.setEnabled(true)
            }
            mCleanOtherObjectsTime?.setEnabled(true)
            mBtnRefresh?.setEnabled(true)
        }
    }

    open fun draw() { super.draw() }

    fun callbackReturnOwnerObjects(notification: LLSD, response: LLSD): Boolean {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            val parcel = mParcel?.getParcel()
            if (parcel != null) {
                val ownerId = parcel.getOwnerID()
                if (ownerId == gAgentID) {
                    LLNotificationsUtil.add("OwnedObjectsReturned")
                } else {
                    val args = mapOf("NAME" to LLSLURL("agent", ownerId, "completename").getSLURLString())
                    LLNotificationsUtil.add("OtherObjectsReturned", args)
                }
                sendReturnObjectsMessage(parcel.getLocalID(), RT_OWNER)
            }
        }
        LLSelectMgr.getInstance().unhighlightAll()
        LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(mParcel?.getParcel())
        refresh()
        return false
    }

    fun callbackReturnGroupObjects(notification: LLSD, response: LLSD): Boolean {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            val parcel = mParcel?.getParcel()
            if (parcel != null) {
                val groupName = gCacheName.getGroupName(parcel.getGroupID())
                LLNotificationsUtil.add("GroupObjectsReturned", mapOf("GROUPNAME" to groupName))
                sendReturnObjectsMessage(parcel.getLocalID(), RT_GROUP)
            }
        }
        LLSelectMgr.getInstance().unhighlightAll()
        LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(mParcel?.getParcel())
        refresh()
        return false
    }

    fun callbackReturnOtherObjects(notification: LLSD, response: LLSD): Boolean {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            val parcel = mParcel?.getParcel()
            if (parcel != null) {
                LLNotificationsUtil.add("OtherObjectsReturned2")
                sendReturnObjectsMessage(parcel.getLocalID(), RT_OTHER)
            }
        }
        LLSelectMgr.getInstance().unhighlightAll()
        LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(mParcel?.getParcel())
        refresh()
        return false
    }

    fun callbackReturnOwnerList(notification: LLSD, response: LLSD): Boolean {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            val parcel = mParcel?.getParcel()
            if (parcel != null) {
                for (ownerId in mSelectedOwners) {
                    val args = mapOf("NAME" to LLSLURL("agent", ownerId, "completename").getSLURLString())
                    LLNotificationsUtil.add("OtherObjectsReturned", args)
                }
                sendReturnObjectsMessage(parcel.getLocalID(), RT_LIST, mSelectedOwners.toMutableSet())
            }
        }
        LLSelectMgr.getInstance().unhighlightAll()
        LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(mParcel?.getParcel())
        refresh()
        return false
    }

    companion object {
        fun clickShowCore(panel: LLPanelLandObjects, returnType: Int, list: MutableSet<UUID>? = null) {
            val parcel = panel.mParcel?.getParcel() ?: return
            sendParcelSelectObjects(parcel.getLocalID(), returnType.toUInt(), list)
        }

        fun onClickShowOwnerObjects(panel: LLPanelLandObjects) = clickShowCore(panel, RT_OWNER)
        fun onClickShowGroupObjects(panel: LLPanelLandObjects) = clickShowCore(panel, RT_GROUP)
        fun onClickShowOtherObjects(panel: LLPanelLandObjects) = clickShowCore(panel, RT_OTHER)

        fun onClickReturnOwnerObjects(panel: LLPanelLandObjects) {
            val parcel = panel.mParcel?.getParcel() ?: return
            clickShowCore(panel, RT_OWNER)
            LLNotificationsUtil.add("ReturnObjectsOwnedByOwner", emptyMap()) { n, r ->
                panel.callbackReturnOwnerObjects(n, r)
            }
        }

        fun onClickReturnGroupObjects(panel: LLPanelLandObjects) {
            val parcel = panel.mParcel?.getParcel() ?: return
            clickShowCore(panel, RT_GROUP)
            LLNotificationsUtil.add("ReturnObjectsOwnedByGroup", emptyMap()) { n, r ->
                panel.callbackReturnGroupObjects(n, r)
            }
        }

        fun onClickReturnOtherObjects(panel: LLPanelLandObjects) {
            val parcel = panel.mParcel?.getParcel() ?: return
            clickShowCore(panel, RT_OTHER)
            LLNotificationsUtil.add("ReturnObjectsOwnedByOther", emptyMap()) { n, r ->
                panel.callbackReturnOtherObjects(n, r)
            }
        }

        fun onClickReturnOwnerList(panel: LLPanelLandObjects) {
            val parcel = panel.mParcel?.getParcel() ?: return
            clickShowCore(panel, RT_LIST, panel.mSelectedOwners)
            LLNotificationsUtil.add("ReturnObjectsOwnedByUser", emptyMap()) { n, r ->
                panel.callbackReturnOwnerList(n, r)
            }
        }

        fun onClickRefresh(panel: LLPanelLandObjects) {
            TODO("APR: use JVM equivalent for send_places_query to refresh parcel object owners list")
        }

        fun onDoubleClickOwner(panel: LLPanelLandObjects) {
            val item = panel.mOwnerList?.getFirstSelected() ?: return
            val ownerId = item.getUUID()
            val isGroup = item.getColumn(1)?.getValue()?.asString() == OWNER_GROUP
            if (isGroup) LLGroupActions.show(ownerId)
            else LLAvatarActions.showProfile(ownerId)
        }

        fun onCommitList(panel: LLPanelLandObjects) {
            TODO("APR: use JVM equivalent for parcel object list commit selection handling")
        }

        fun onLostFocus(panel: LLPanelLandObjects) {
            val parcel = panel.mParcel?.getParcel() ?: return
            val newTime = panel.mCleanOtherObjectsTime?.getText()?.toIntOrNull() ?: return
            if (newTime != panel.mOtherTime) {
                parcel.setCleanOtherTime(newTime)
                sendOtherCleanTimeMessage(parcel.getLocalID(), newTime)
                panel.refresh()
            }
        }

        fun onCommitClean(panel: LLPanelLandObjects) {
            onLostFocus(panel)
        }

        fun processParcelObjectOwnersReply(msg: LLMessageSystem) {
            TODO("APR: use JVM equivalent for processing ParcelObjectOwnersReply network message")
        }
    }
}

class LLPanelLandOptions(private val mParcel: LLParcelSelection?) : LLPanel() {

    private var mCheckEditObjects: LLCheckBoxCtrl? = null
    private var mCheckEditGroupObjects: LLCheckBoxCtrl? = null
    private var mCheckAllObjectEntry: LLCheckBoxCtrl? = null
    private var mCheckGroupObjectEntry: LLCheckBoxCtrl? = null
    private var mCheckEditLand: LLCheckBoxCtrl? = null
    private var mCheckSafe: LLCheckBoxCtrl? = null
    private var mCheckFly: LLCheckBoxCtrl? = null
    private var mCheckGroupScripts: LLCheckBoxCtrl? = null
    private var mCheckOtherScripts: LLCheckBoxCtrl? = null
    private var mCheckShowDirectory: LLCheckBoxCtrl? = null
    private var mCategoryCombo: LLComboBox? = null
    private var mLandingTypeCombo: LLComboBox? = null
    private var mSnapshotCtrl: LLTextureCtrl? = null
    private var mLocationText: LLTextBox? = null
    private var mSeeAvatarsText: LLTextBox? = null
    private var mSetBtn: LLButton? = null
    private var mClearBtn: LLButton? = null
    private var mTeleportToLandingPointBtn: LLButton? = null
    private var mMatureCtrl: LLCheckBoxCtrl? = null
    private var mPushRestrictionCtrl: LLCheckBoxCtrl? = null
    private var mSeeAvatarsCtrl: LLCheckBoxCtrl? = null

    override fun postBuild(): Boolean {
        mCheckEditObjects      = getChild<LLCheckBoxCtrl>("edit objects check")
        mCheckEditGroupObjects = getChild<LLCheckBoxCtrl>("edit group objects check")
        mCheckAllObjectEntry   = getChild<LLCheckBoxCtrl>("all object entry check")
        mCheckGroupObjectEntry = getChild<LLCheckBoxCtrl>("group object entry check")
        mCheckEditLand         = getChild<LLCheckBoxCtrl>("edit land check")
        mCheckSafe             = getChild<LLCheckBoxCtrl>("safe check")
        mCheckFly              = getChild<LLCheckBoxCtrl>("fly check")
        mCheckGroupScripts     = getChild<LLCheckBoxCtrl>("group scripts check")
        mCheckOtherScripts     = getChild<LLCheckBoxCtrl>("other scripts check")
        mCheckShowDirectory    = getChild<LLCheckBoxCtrl>("show directory check")
        mCategoryCombo         = getChild<LLComboBox>("land category")
        mLandingTypeCombo      = getChild<LLComboBox>("landing type")
        mSnapshotCtrl          = getChild<LLTextureCtrl>("snapshot_ctrl")
        mLocationText          = getChild<LLTextBox>("landing_point_text")
        mSeeAvatarsText        = getChild<LLTextBox>("see_avatars_text")
        mSetBtn                = getChild<LLButton>("Set")
        mSetBtn?.setClickedCallback { onClickSet(this) }
        mClearBtn              = getChild<LLButton>("Clear")
        mClearBtn?.setClickedCallback { onClickClear(this) }
        mTeleportToLandingPointBtn = getChild<LLButton>("teleport_landing_point_btn")
        mTeleportToLandingPointBtn?.setClickedCallback { onClickTeleport() }
        mMatureCtrl            = getChild<LLCheckBoxCtrl>("mature_check")
        mPushRestrictionCtrl   = getChild<LLCheckBoxCtrl>("push_restriction_check")
        mSeeAvatarsCtrl        = getChild<LLCheckBoxCtrl>("see_avatars_check")
        mSeeAvatarsCtrl?.setClickedCallback { toggleSeeAvatars(this) }

        for (ctrl in listOf(mCheckEditObjects, mCheckEditGroupObjects, mCheckAllObjectEntry,
            mCheckGroupObjectEntry, mCheckEditLand, mCheckSafe, mCheckFly,
            mCheckGroupScripts, mCheckOtherScripts, mCheckShowDirectory,
            mCategoryCombo, mLandingTypeCombo, mSnapshotCtrl, mMatureCtrl, mPushRestrictionCtrl)) {
            ctrl?.setCommitCallback { onCommitAny(this) }
        }

        return true
    }

    open fun draw() {
        refreshSearch()
        super.draw()
    }

    open fun refresh() {
        TODO("APR: use JVM equivalent for full land options panel refresh from parcel data")
    }

    private fun refreshSearch() {
        TODO("APR: use JVM equivalent for refreshing show-in-search checkbox and category selector")
    }

    private fun getDirectoryFee(): Int {
        TODO("APR: use JVM equivalent for LLAgentBenefits directory fee lookup")
    }

    private fun onClickTeleport() {
        val parcel = mParcel?.getParcel() ?: return
        val landingPoint = parcel.getUserLocation()
        gAgent.teleportViaLocation(landingPoint.toGlobal())
    }

    companion object {
        fun onCommitAny(panel: LLPanelLandOptions) {
            TODO("APR: use JVM equivalent for committing land option changes to parcel and sending update")
        }

        fun onClickSet(panel: LLPanelLandOptions) {
            TODO("APR: use JVM equivalent for setting landing point to current agent position")
        }

        fun onClickClear(panel: LLPanelLandOptions) {
            TODO("APR: use JVM equivalent for clearing landing point from parcel")
        }

        fun toggleSeeAvatars(panel: LLPanelLandOptions) {
            TODO("APR: use JVM equivalent for toggling see-avatars parcel flag")
        }
    }
}

class LLPanelLandAccess(private val mParcel: LLParcelSelection?) : LLPanel() {

    var mListAccess: LLNameListCtrl? = null
    var mListBanned: LLNameListCtrl? = null
    var mAllowText: LLUICtrl? = null
    var mBanText: LLUICtrl? = null
    var mPublicAccessCheck: LLUICtrl? = null
    var mGroupAccessCheck: LLUICtrl? = null
    var mPaymentInfoCheck: LLUICtrl? = null
    var mAgeVerifiedCheck: LLUICtrl? = null
    var mTemporaryPassCheck: LLUICtrl? = null
    var mTemporaryPassCombo: LLComboBox? = null
    var mTemporaryPassPriceSpin: LLUICtrl? = null
    var mTemporaryPassHourSpin: LLUICtrl? = null
    var mBtnAddAllowed: LLButton? = null
    var mBtnRemoveAllowed: LLButton? = null
    var mBtnAddBanned: LLButton? = null
    var mBtnRemoveBanned: LLButton? = null
    var mBtnExportAccess: LLButton? = null
    var mBtnExportBanned: LLButton? = null
    var mBtnImportAccess: LLButton? = null
    var mBtnImportBanned: LLButton? = null

    override fun postBuild(): Boolean {
        mListAccess          = getChild<LLNameListCtrl>("AccessList")
        mListBanned          = getChild<LLNameListCtrl>("BannedList")
        mAllowText           = getChild<LLUICtrl>("allow_text")
        mBanText             = getChild<LLUICtrl>("ban_text")
        mPublicAccessCheck   = getChild<LLUICtrl>("public_access")
        mGroupAccessCheck    = getChild<LLUICtrl>("group_access_check")
        mPaymentInfoCheck    = getChild<LLUICtrl>("payment_info_check")
        mAgeVerifiedCheck    = getChild<LLUICtrl>("age_verified_check")
        mTemporaryPassCheck  = getChild<LLUICtrl>("temporary_pass_check")
        mTemporaryPassCombo  = getChild<LLComboBox>("pass_combo")
        mTemporaryPassPriceSpin = getChild<LLUICtrl>("pass_price")
        mTemporaryPassHourSpin  = getChild<LLUICtrl>("pass_hours")
        mBtnAddAllowed       = getChild<LLButton>("add_allowed")
        mBtnRemoveAllowed    = getChild<LLButton>("remove_allowed")
        mBtnAddBanned        = getChild<LLButton>("add_banned")
        mBtnRemoveBanned     = getChild<LLButton>("remove_banned")
        mBtnExportAccess     = getChild<LLButton>("export_access_btn")
        mBtnExportBanned     = getChild<LLButton>("export_banned_btn")
        mBtnImportAccess     = getChild<LLButton>("import_access_btn")
        mBtnImportBanned     = getChild<LLButton>("import_banned_btn")

        mPublicAccessCheck?.setCommitCallback   { onCommitPublicAccess(this) }
        mGroupAccessCheck?.setCommitCallback    { onCommitGroupCheck(this) }
        mPaymentInfoCheck?.setCommitCallback    { onCommitAny(this) }
        mAgeVerifiedCheck?.setCommitCallback    { onCommitAny(this) }
        mTemporaryPassCheck?.setCommitCallback  { onCommitAny(this) }
        mTemporaryPassCombo?.setCommitCallback  { onCommitAny(this) }
        mTemporaryPassPriceSpin?.setCommitCallback { onCommitAny(this) }
        mTemporaryPassHourSpin?.setCommitCallback  { onCommitAny(this) }

        mBtnAddAllowed?.setClickedCallback    { onClickAddAccess() }
        mBtnRemoveAllowed?.setClickedCallback { onClickRemoveAccess() }
        mBtnAddBanned?.setClickedCallback     { onClickAddBanned() }
        mBtnRemoveBanned?.setClickedCallback  { onClickRemoveBanned() }
        mBtnExportAccess?.setClickedCallback  { onClickExportAccess() }
        mBtnExportBanned?.setClickedCallback  { onClickExportBanned() }
        mBtnImportAccess?.setClickedCallback  { onClickImportAccess() }
        mBtnImportBanned?.setClickedCallback  { onClickImportBanned() }

        return true
    }

    fun refresh() { TODO("APR: use JVM equivalent for access panel refresh from parcel data") }
    fun refreshUi() { TODO("APR: use JVM equivalent for access panel UI state refresh") }
    fun refreshNames() { TODO("APR: use JVM equivalent for access panel name list refresh") }
    open fun draw() { super.draw() }

    fun onClickAddAccess() { TODO("APR: use JVM equivalent for showing avatar picker for access list") }
    fun onClickAddBanned() { TODO("APR: use JVM equivalent for showing ban duration floater") }
    fun onClickRemoveAccess() { TODO("APR: use JVM equivalent for removing selected entry from access list") }
    fun onClickRemoveBanned() { TODO("APR: use JVM equivalent for removing selected entry from ban list") }
    fun callbackAvatarCBBanned(ids: List<UUID>) { TODO("APR: use JVM equivalent for processing banned avatars") }
    fun callbackAvatarCBBanned2(ids: List<UUID>, duration: Int) { TODO("APR: use JVM equivalent for processing banned avatars with duration") }
    fun callbackAvatarCBAccess(ids: List<UUID>) { TODO("APR: use JVM equivalent for processing allowed avatars") }

    fun onClickExportAccess() { onClickExportList(mListAccess, "access_list.csv") }
    fun onClickExportBanned() { onClickExportList(mListBanned, "ban_list.csv") }

    fun onClickExportList(list: LLNameListCtrl?, filename: String) {
        TODO("APR: use JVM equivalent for exporting name list to file")
    }

    fun exportListCallback(list: LLNameListCtrl?, filenames: List<String>) {
        TODO("APR: use JVM equivalent for writing name list export file")
    }

    fun onClickImportAccess() { onClickImportList(mListAccess) }
    fun onClickImportBanned() { onClickImportList(mListBanned) }

    fun onClickImportList(list: LLNameListCtrl?) {
        TODO("APR: use JVM equivalent for importing name list from file")
    }

    fun importListCallback(list: LLNameListCtrl?, filenames: List<String>) {
        TODO("APR: use JVM equivalent for reading name list import file and applying to parcel")
    }

    companion object {
        fun onCommitPublicAccess(panel: LLPanelLandAccess) {
            TODO("APR: use JVM equivalent for committing public-access flag change")
        }

        fun onCommitAny(panel: LLPanelLandAccess) {
            TODO("APR: use JVM equivalent for committing any access panel change to parcel")
        }

        fun onCommitGroupCheck(panel: LLPanelLandAccess) {
            TODO("APR: use JVM equivalent for committing group-access check change")
        }
    }
}

class LLPanelLandCovenant(private val mParcel: LLParcelSelection?) : LLPanel() {

    companion object {
        fun updateCovenant(source: LLTextBase) {
            TODO("APR: use JVM equivalent for updating covenant text from source text base")
        }

        fun updateCovenantText(text: String) {
            LLFloaterLand.getCurrentPanelLandCovenant()?.getChild<LLTextEditor>("covenant_editor")
                ?.setText(text)
        }

        fun updateEstateName(name: String) {
            LLFloaterLand.getCurrentPanelLandCovenant()?.getChild<LLTextBox>("estate_name_text")
                ?.setText(name)
        }

        fun updateLastModified(text: String) {
            LLFloaterLand.getCurrentPanelLandCovenant()?.getChild<LLTextBox>("last_modified_text")
                ?.setText(text)
        }

        fun updateEstateOwnerName(name: String) {
            LLFloaterLand.getCurrentPanelLandCovenant()?.mTextEstateOwner?.setText(name)
        }
    }

    private var mLastRegionID: UUID? = null
    private var mNextUpdateTime: Double = 0.0
    var mTextEstateOwner: LLTextBox? = null

    override fun postBuild(): Boolean {
        mTextEstateOwner = getChild<LLTextBox>("estate_owner_text")
        return true
    }

    fun refresh() {
        TODO("APR: use JVM equivalent for covenant panel refresh, including region-change detection and timed re-fetch")
    }
}

class LLPanelLandExperiences(private val mParcel: LLParcelSelection?) : LLPanel() {

    private var mAllowed: LLPanelExperienceListEditor? = null
    private var mBlocked: LLPanelExperienceListEditor? = null

    override fun postBuild(): Boolean {
        mAllowed = setupList("allowed_experience_list", XP_TYPE_ALLOW, EXPERIENCE_KEY_TYPE_ALLOWED)
        mBlocked = setupList("blocked_experience_list", XP_TYPE_BLOCK, EXPERIENCE_KEY_TYPE_BLOCKED)
        return true
    }

    fun refresh() {
        refreshPanel(mAllowed, XP_TYPE_ALLOW)
        refreshPanel(mBlocked, XP_TYPE_BLOCK)
    }

    fun experienceAdded(id: UUID, xpType: UInt, accessType: UInt) {
        TODO("APR: use JVM equivalent for adding an experience to the parcel")
    }

    fun experienceRemoved(id: UUID, accessType: UInt) {
        TODO("APR: use JVM equivalent for removing an experience from the parcel")
    }

    private fun setupList(controlName: String, xpType: UInt, accessType: UInt): LLPanelExperienceListEditor {
        TODO("APR: use JVM equivalent for setting up experience list editor panel")
    }

    private fun refreshPanel(panel: LLPanelExperienceListEditor?, xpType: UInt) {
        TODO("APR: use JVM equivalent for refreshing experience list editor panel from parcel data")
    }
}

abstract class LLPanelLandEnvironment(private val mParcel: LLParcelSelection?) : LLPanelEnvironmentInfo() {

    private var mLastParcelId: Int = 0

    override fun isRegion(): Boolean = false

    override fun isLargeEnough(): Boolean {
        val parcel = mParcel?.getParcel() ?: return false
        return parcel.getArea() >= MINIMUM_PARCEL_SIZE
    }

    override fun postBuild(): Boolean = super.postBuild()

    override fun refresh() = super.refresh()

    override fun getParcel(): LLParcel? = mParcel?.getParcel()

    override fun canEdit(): Boolean {
        TODO("APR: use JVM equivalent for checking if the current agent can edit the parcel environment")
    }

    override fun getParcelId(): Int {
        TODO("APR: use JVM equivalent for getting the current parcel's local ID")
    }

    override fun refreshFromSource() {
        if (!isSameRegion()) return
        TODO("APR: use JVM equivalent for requesting parcel environment info from the server")
    }

    private fun isSameRegion(): Boolean {
        TODO("APR: use JVM equivalent for checking if the selected parcel is in the agent's current region")
    }
}
