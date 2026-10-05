package com.firestorm.newview

import java.util.UUID

enum class Badge { BADGE_OK, BADGE_NOTE, BADGE_WARN, BADGE_ERROR }

object LLFloaterSellLand {

    fun sellLand(region: LLViewerRegion, parcel: LLParcelSelectionHandle) {
        val ui = LLFloaterReg.showTypedInstance<LLFloaterSellLandUI>("sell_land")
        ui.setParcel(region, parcel)
    }

    fun buildFloater(key: Any): LLFloater {
        return LLFloaterSellLandUI(key)
    }
}

class LLFloaterSellLandUI(key: Any) : LLFloater(key) {

    private inner class SelectionObserver : LLParcelObserver {
        override fun changed() {
            if (LLViewerParcelMgr.getInstance().selectionEmpty()) {
                closeFloater()
            } else if (isVisible()) {
                setParcel(
                    LLViewerParcelMgr.getInstance().getSelectionRegion(),
                    LLViewerParcelMgr.getInstance().getParcelSelection()
                )
            }
        }
    }

    private var mRegion: LLViewerRegion? = null
    private var mParcelSelection: LLParcelSelectionHandle? = null
    private var mParcelIsForSale: Boolean = false
    private var mSellToBuyer: Boolean = false
    private var mChoseSellTo: Boolean = false
    private var mParcelPrice: Int = 0
    private var mParcelActualArea: Int = 0
    private var mParcelSnapshot: UUID = UUID(0, 0)
    private var mAuthorizedBuyer: UUID = UUID(0, 0)
    private var mParcelSoldWithObjects: Boolean = false

    private val mParcelSelectionObserver: SelectionObserver = SelectionObserver()
    private var mAvatarNameCacheConnection: ((LLAvatarName) -> Unit)? = null

    init {
        LLViewerParcelMgr.getInstance().addObserver(mParcelSelectionObserver)
    }

    fun destroy() {
        mAvatarNameCacheConnection = null
        LLViewerParcelMgr.getInstance().removeObserver(mParcelSelectionObserver)
    }

    fun onClose(appQuitting: Boolean) {
        mParcelSelection = null
    }

    override fun postBuild(): Boolean {
        childSetCommitCallback("sell_to") { ctrl, _ -> onChangeValue(ctrl) }
        childSetCommitCallback("price") { ctrl, _ -> onChangeValue(ctrl) }
        getChild<LLLineEditor>("price").setPrevalidate(LLTextValidate::validateNonNegativeS32)
        childSetCommitCallback("sell_objects") { ctrl, _ -> onChangeValue(ctrl) }
        childSetAction("sell_to_select_agent") { doSelectAgent() }
        childSetAction("cancel_btn") { doCancel() }
        childSetAction("sell_btn") { doSellLand() }
        childSetAction("show_objects") { doShowObjects() }
        center()
        getChild<LLUICtrl>("profile_scroll").setTabStop(true)
        return true
    }

    fun setParcel(region: LLViewerRegion?, parcel: LLParcelSelectionHandle?): Boolean {
        if (parcel?.getParcel() == null) return false

        mRegion = region
        mParcelSelection = parcel
        mChoseSellTo = false

        updateParcelInfo()
        refreshUI()
        return true
    }

    private fun updateParcelInfo() {
        val parcelp = mParcelSelection?.getParcel() ?: return

        mParcelActualArea = parcelp.getArea()
        mParcelIsForSale = parcelp.getForSale()
        if (mParcelIsForSale) mChoseSellTo = true

        mParcelPrice = if (mParcelIsForSale) parcelp.getSalePrice() else 0
        mParcelSoldWithObjects = parcelp.getSellWithObjects()

        if (mParcelIsForSale) {
            getChild<LLUICtrl>("price").setValue(mParcelPrice)
            getChild<LLUICtrl>("sell_objects").setValue(if (mParcelSoldWithObjects) "yes" else "no")
        } else {
            getChild<LLUICtrl>("price").setValue("")
            getChild<LLUICtrl>("sell_objects").setValue("none")
        }

        mParcelSnapshot = parcelp.getSnapshotID()
        mAuthorizedBuyer = parcelp.getAuthorizedBuyerID()
        mSellToBuyer = mAuthorizedBuyer != UUID(0, 0)

        if (mSellToBuyer) {
            mAvatarNameCacheConnection = null
            val cb: (LLAvatarName) -> Unit = { avName -> onBuyerNameCache(avName) }
            mAvatarNameCacheConnection = cb
            LLAvatarNameCache.get(mAuthorizedBuyer, cb)
        }
    }

    private fun onBuyerNameCache(avName: LLAvatarName) {
        mAvatarNameCacheConnection = null
        getChild<LLUICtrl>("sell_to_agent").setValue(avName.getCompleteName())
        getChild<LLUICtrl>("sell_to_agent").setToolTip(avName.getUserName())
    }

    private fun setBadge(id: String, badge: Badge) {
        val badgeName = when (badge) {
            Badge.BADGE_OK -> "badge_ok.j2c"
            Badge.BADGE_NOTE -> "badge_note.j2c"
            Badge.BADGE_WARN, Badge.BADGE_ERROR -> "badge_warn.j2c"
        }
        getChild<LLUICtrl>(id).setValue(badgeName)
    }

    private fun refreshUI() {
        val parcelp = mParcelSelection?.getParcel() ?: return

        getChild<LLTextureCtrl>("info_image").setImageAssetID(mParcelSnapshot)
        getChild<LLUICtrl>("info_parcel").setValue(parcelp.getName())
        getChild<LLUICtrl>("info_size").setTextArg("[AREA]", "$mParcelActualArea")

        val priceStr = getChild<LLUICtrl>("price").getValue().toString()
        val validPrice = priceStr.isNotEmpty() && LLTextValidate.validateNonNegativeS32(priceStr)

        if (validPrice && mParcelActualArea > 0) {
            val perMeterPrice = mParcelPrice.toFloat() / mParcelActualArea.toFloat()
            getChild<LLUICtrl>("price_per_m").setTextArg("[PER_METER]", "%.2f".format(perMeterPrice))
            getChildView("price_per_m").setVisible(true)
            setBadge("step_price", Badge.BADGE_OK)
        } else {
            getChildView("price_per_m").setVisible(false)
            setBadge("step_price", if (priceStr.isEmpty()) Badge.BADGE_NOTE else Badge.BADGE_ERROR)
        }

        val sellTo = getChild<LLUICtrl>("sell_to").getValue().toString()
        if (mSellToBuyer) {
            getChild<LLUICtrl>("sell_to").setValue("user")
            getChildView("sell_to_agent").setVisible(true)
            getChildView("sell_to_select_agent").setVisible(true)
        } else {
            getChild<LLUICtrl>("sell_to").setValue(if (mChoseSellTo) "anyone" else "select")
            getChildView("sell_to_agent").setVisible(false)
            getChildView("sell_to_select_agent").setVisible(false)
        }

        val validSellTo = sellTo != "select" && (sellTo != "user" || mAuthorizedBuyer != UUID(0, 0))
        setBadge("step_sell_to", if (!validSellTo) Badge.BADGE_NOTE else Badge.BADGE_OK)

        val validSellObjects = getChild<LLUICtrl>("sell_objects").getValue().toString() != "none"
        setBadge("step_sell_objects", if (!validSellObjects) Badge.BADGE_NOTE else Badge.BADGE_OK)

        getChildView("sell_btn").setEnabled(validSellTo && validPrice && validSellObjects)
    }

    private fun onChangeValue(ctrl: LLUICtrl) {
        val sellTo = getChild<LLUICtrl>("sell_to").getValue().toString()
        when (sellTo) {
            "user" -> {
                mChoseSellTo = true
                mSellToBuyer = true
                if (mAuthorizedBuyer == UUID(0, 0)) doSelectAgent()
            }
            "anyone" -> {
                mChoseSellTo = true
                mSellToBuyer = false
            }
        }

        mParcelPrice = getChild<LLUICtrl>("price").getValue().toString().toIntOrNull() ?: 0
        mParcelSoldWithObjects = getChild<LLUICtrl>("sell_objects").getValue().toString() == "yes"

        refreshUI()
    }

    private fun doSelectAgent() {
        val button = findChild<LLView>("sell_to_select_agent")
        val picker = LLFloaterAvatarPicker.show(
            { ids, names -> callbackAvatarPick(ids, names) },
            false, true, false, getName(), button
        )
        if (picker != null) addDependentFloater(picker)
    }

    private fun callbackAvatarPick(ids: MutableList<UUID>, names: MutableList<LLAvatarName>) {
        val parcel = mParcelSelection?.getParcel() ?: return
        if (names.isEmpty() || ids.isEmpty()) return

        val id = ids[0]
        parcel.setAuthorizedBuyerID(id)
        mAuthorizedBuyer = ids[0]
        getChild<LLUICtrl>("sell_to_agent").setValue(names[0].getCompleteName())
        refreshUI()
    }

    private fun doCancel() {
        closeFloater()
    }

    private fun doShowObjects() {
        val parcel = mParcelSelection?.getParcel() ?: return
        sendParcelSelectObjects(parcel.getLocalID(), RT_SELL)
        LLNotificationsUtil.add("TransferObjectsHighlighted", emptyMap<String, Any?>(), emptyMap<String, Any?>())
    }

    private fun doSellLand() {
        val parcel = mParcelSelection?.getParcel() ?: return

        val salePrice = getChild<LLUICtrl>("price").getValue().toString().toIntOrNull() ?: 0
        val area = parcel.getArea()
        val sellToAnyone = getChild<LLUICtrl>("sell_to").getValue().toString() != "user"
        val authorizedBuyerName = if (!sellToAnyone)
            getChild<LLUICtrl>("sell_to_agent").getValue().toString()
        else
            LLTrans.getString("Anyone")

        if (!parcel.getForSale() && salePrice == 0 && sellToAnyone) {
            LLNotificationsUtil.add("SalePriceRestriction")
            return
        }

        val args = mapOf(
            "LAND_SIZE" to "$area",
            "SALE_PRICE" to "$salePrice",
            "NAME" to authorizedBuyerName
        )

        val notifName = if (sellToAnyone) "ConfirmLandSaleToAnyoneChange" else "ConfirmLandSaleChange"

        if (parcel.getForSale()) {
            onConfirmSale(emptyMap(), mapOf("option" to 0))
        } else {
            LLNotificationsUtil.add(notifName, args) { notification, response ->
                onConfirmSale(notification, response)
            }
        }
    }

    private fun onConfirmSale(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
        val option = (response["option"] as? Number)?.toInt() ?: -1
        if (option != 0) return false

        val salePrice = getChild<LLUICtrl>("price").getValue().toString().toIntOrNull() ?: return false
        if (salePrice < 0) return false

        val parcel = mParcelSelection?.getParcel() ?: return false

        parcel.setParcelFlag(PF_FOR_SALE, true)
        parcel.setSalePrice(salePrice)
        parcel.setSellWithObjects(getChild<LLUICtrl>("sell_objects").getValue().toString() == "yes")

        if (getChild<LLUICtrl>("sell_to").getValue().toString() == "user") {
            parcel.setAuthorizedBuyerID(mAuthorizedBuyer)
        } else {
            parcel.setAuthorizedBuyerID(UUID(0, 0))
        }

        LLViewerParcelMgr.getInstance().sendParcelPropertiesUpdate(parcel)
        closeFloater()
        return false
    }

    companion object {
        fun callbackHighlightTransferable(notification: Map<String, Any?>, data: Map<String, Any?>): Boolean {
            LLSelectMgr.getInstance().unhighlightAll()
            return false
        }
    }
}
