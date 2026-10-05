package com.firestorm.newview

import com.firestorm.floater.Floater
import java.util.UUID

enum class Badge { OK, NOTE, WARN, ERROR }

object FloaterSellLand {
    fun sellLand(region: ViewerRegion, parcel: ParcelSelectionHandle) {
        val ui = FloaterReg.showTypedInstance<FloaterSellLandUI>("sell_land")
        ui?.setParcel(region, parcel)
    }

    fun buildFloater(key: Any?): Floater = FloaterSellLandUI(key)
}

class FloaterSellLandUI(key: Any?) : Floater(key) {

    private var region: ViewerRegion? = null
    private var parcelSelection: ParcelSelectionHandle? = null
    private var parcelIsForSale: Boolean = false
    private var sellToBuyer: Boolean = false
    private var choseSellTo: Boolean = false
    private var parcelPrice: Int = 0
    private var parcelActualArea: Int = 0
    private var parcelSnapshot: UUID = UUID(0, 0)
    private var authorizedBuyer: UUID = UUID(0, 0)
    private var parcelSoldWithObjects: Boolean = false

    private val selectionObserver = SelectionObserver(this)
    private var avatarNameCacheConnection: Connection? = null

    init {
        ViewerParcelMgr.instance.addObserver(selectionObserver)
    }

    override fun onClose(appQuitting: Boolean) {
        parcelSelection = null
    }

    fun finalize() {
        avatarNameCacheConnection?.disconnect()
        ViewerParcelMgr.instance.removeObserver(selectionObserver)
    }

    override fun postBuild(): Boolean {
        childSetCommitCallback("sell_to")      { _, _ -> onChangeValue() }
        childSetCommitCallback("price")        { _, _ -> onChangeValue() }
        getChild<LineEditor>("price").setPrevalidate(TextValidate::validateNonNegativeS32)
        childSetCommitCallback("sell_objects") { _, _ -> onChangeValue() }
        childSetAction("sell_to_select_agent") { doSelectAgent() }
        childSetAction("cancel_btn")           { doCancel() }
        childSetAction("sell_btn")             { doSellLand() }
        childSetAction("show_objects")         { doShowObjects() }
        center()
        getChild<Any>("profile_scroll").setTabStop(true)
        return true
    }

    fun setParcel(region: ViewerRegion, parcel: ParcelSelectionHandle): Boolean {
        if (parcel.parcel == null) return false
        this.region = region
        this.parcelSelection = parcel
        choseSellTo = false
        updateParcelInfo()
        refreshUI()
        return true
    }

    private fun updateParcelInfo() {
        val parcelp = parcelSelection?.parcel ?: return

        parcelActualArea = parcelp.area
        parcelIsForSale  = parcelp.isForSale
        if (parcelIsForSale) choseSellTo = true

        parcelPrice           = if (parcelIsForSale) parcelp.salePrice else 0
        parcelSoldWithObjects = parcelp.sellWithObjects

        if (parcelIsForSale) {
            getChild<UICtrl>("price").setValue(parcelPrice)
            getChild<UICtrl>("sell_objects").setValue(if (parcelSoldWithObjects) "yes" else "no")
        } else {
            getChild<UICtrl>("price").setValue("")
            getChild<UICtrl>("sell_objects").setValue("none")
        }

        parcelSnapshot  = parcelp.snapshotId
        authorizedBuyer = parcelp.authorizedBuyerId
        sellToBuyer     = authorizedBuyer != UUID(0, 0)

        if (sellToBuyer) {
            avatarNameCacheConnection?.disconnect()
            avatarNameCacheConnection =
                AvatarNameCache.get(authorizedBuyer) { avName -> onBuyerNameCache(avName) }
        }
    }

    private fun onBuyerNameCache(avName: AvatarName) {
        avatarNameCacheConnection?.disconnect()
        getChild<UICtrl>("sell_to_agent").setValue(avName.completeName)
        getChild<UICtrl>("sell_to_agent").setToolTip(avName.userName)
    }

    private fun setBadge(id: String, badge: Badge) {
        val badgeName = when (badge) {
            Badge.OK    -> "badge_ok.j2c"
            Badge.NOTE  -> "badge_note.j2c"
            Badge.WARN,
            Badge.ERROR -> "badge_warn.j2c"
        }
        getChild<UICtrl>(id).setValue(badgeName)
    }

    private fun refreshUI() {
        val parcelp = parcelSelection?.parcel ?: return

        getChild<TextureCtrl>("info_image").setImageAssetId(parcelSnapshot)
        getChild<UICtrl>("info_parcel").setValue(parcelp.name)
        getChild<UICtrl>("info_size").setTextArg("[AREA]", parcelActualArea.toString())

        val priceStr   = getChild<UICtrl>("price").value.asString()
        val validPrice = priceStr.isNotEmpty() && TextValidate.validateNonNegativeS32(priceStr)

        if (validPrice && parcelActualArea > 0) {
            val perMeter = parcelPrice.toFloat() / parcelActualArea.toFloat()
            getChild<UICtrl>("price_per_m").setTextArg("[PER_METER]", "%.2f".format(perMeter))
            getChildView("price_per_m").setVisible(true)
            setBadge("step_price", Badge.OK)
        } else {
            getChildView("price_per_m").setVisible(false)
            setBadge("step_price", if (priceStr.isEmpty()) Badge.NOTE else Badge.ERROR)
        }

        if (sellToBuyer) {
            getChild<UICtrl>("sell_to").setValue("user")
            getChildView("sell_to_agent").setVisible(true)
            getChildView("sell_to_select_agent").setVisible(true)
        } else {
            getChild<UICtrl>("sell_to").setValue(if (choseSellTo) "anyone" else "select")
            getChildView("sell_to_agent").setVisible(false)
            getChildView("sell_to_select_agent").setVisible(false)
        }

        val sellTo      = getChild<UICtrl>("sell_to").value.asString()
        val validSellTo = sellTo != "select" && !(sellTo == "user" && authorizedBuyer == UUID(0, 0))
        setBadge("step_sell_to", if (validSellTo) Badge.OK else Badge.NOTE)

        val validSellObjects = getChild<UICtrl>("sell_objects").value.asString() != "none"
        setBadge("step_sell_objects", if (validSellObjects) Badge.OK else Badge.NOTE)

        getChildView("sell_btn").setEnabled(validSellTo && validPrice && validSellObjects)
    }

    private fun onChangeValue() {
        val sellTo = getChild<UICtrl>("sell_to").value.asString()
        when (sellTo) {
            "user" -> {
                choseSellTo = true
                sellToBuyer = true
                if (authorizedBuyer == UUID(0, 0)) doSelectAgent()
            }
            "anyone" -> {
                choseSellTo = true
                sellToBuyer = false
            }
        }

        parcelPrice = getChild<UICtrl>("price").value.asInt()
        parcelSoldWithObjects = getChild<UICtrl>("sell_objects").value.asString() == "yes"
        refreshUI()
    }

    private fun doSelectAgent() {
        val button = findChild<View>("sell_to_select_agent")
        val picker = AvatarPicker.show(
            callback = { ids, names -> callbackAvatarPick(ids, names) },
            allowMultiple = false,
            closeOnSelect = true,
            button = button
        )
        if (picker != null) addDependentFloater(picker)
    }

    private fun callbackAvatarPick(ids: List<UUID>, names: List<AvatarName>) {
        if (names.isEmpty() || ids.isEmpty()) return
        parcelSelection?.parcel?.setAuthorizedBuyerId(ids[0])
        authorizedBuyer = ids[0]
        getChild<UICtrl>("sell_to_agent").setValue(names[0].completeName)
        refreshUI()
    }

    private fun doCancel() = closeFloater()

    private fun doShowObjects() {
        val parcel = parcelSelection?.parcel ?: return
        sendParcelSelectObjects(parcel.localId, ReturnType.SELL)
        NotificationsUtil.add("TransferObjectsHighlighted")
    }

    private fun doSellLand() {
        val parcel = parcelSelection?.parcel ?: return

        val salePrice = getChild<UICtrl>("price").value.asInt()
        val area      = parcel.area
        val sellTo    = getChild<UICtrl>("sell_to").value.asString()
        val sellToAnyone = sellTo != "user"
        val authorizedBuyerName =
            if (sellToAnyone) Trans.getString("Anyone")
            else              getChild<UICtrl>("sell_to_agent").value.asString()

        if (!parcel.isForSale && salePrice == 0 && sellToAnyone) {
            NotificationsUtil.add("SalePriceRestriction")
            return
        }

        val args = mapOf(
            "LAND_SIZE"  to area.toString(),
            "SALE_PRICE" to salePrice.toString(),
            "NAME"       to authorizedBuyerName
        )

        val notificationName =
            if (sellToAnyone) "ConfirmLandSaleToAnyoneChange" else "ConfirmLandSaleChange"

        if (parcel.isForSale) {
            onConfirmSale(notification = null, response = mapOf("option" to 0))
        } else {
            NotificationsUtil.add(notificationName, args) { notification, response ->
                onConfirmSale(notification, response)
            }
        }
    }

    fun onConfirmSale(notification: Any?, response: Any?): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option != 0) return false

        val salePrice = getChild<UICtrl>("price").value.asInt()
        if (salePrice < 0) return false

        val parcel = parcelSelection?.parcel ?: return false

        parcel.setParcelFlag(ParcelFlag.FOR_SALE, true)
        parcel.setSalePrice(salePrice)
        parcel.setSellWithObjects(getChild<UICtrl>("sell_objects").value.asString() == "yes")

        if (getChild<UICtrl>("sell_to").value.asString() == "user") {
            parcel.setAuthorizedBuyerId(authorizedBuyer)
        } else {
            parcel.setAuthorizedBuyerId(UUID(0, 0))
        }

        ViewerParcelMgr.instance.sendParcelPropertiesUpdate(parcel)
        closeFloater()
        return false
    }

    companion object {
        fun callbackHighlightTransferable(notification: Any?, data: Any?): Boolean {
            SelectMgr.instance.unhighlightAll()
            return false
        }
    }

    private class SelectionObserver(private val floater: FloaterSellLandUI) : ParcelObserver {
        override fun changed() {
            if (ViewerParcelMgr.instance.selectionEmpty()) {
                floater.closeFloater()
            } else if (floater.getVisible()) {
                floater.setParcel(
                    ViewerParcelMgr.instance.selectionRegion,
                    ViewerParcelMgr.instance.parcelSelection
                )
            }
        }
    }
}
