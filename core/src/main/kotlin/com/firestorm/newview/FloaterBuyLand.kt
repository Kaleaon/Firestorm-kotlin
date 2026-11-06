package com.firestorm.newview

import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

private const val GROUP_LAND_BONUS_FACTOR = 1.1f

object FloaterBuyLand {

    fun buyLand(region: ViewerRegion, parcel: ParcelSelectionHandle, isForGroup: Boolean) {
        if (isForGroup && !Agent.hasPowerInActiveGroup(GroupPowers.GP_LAND_DEED)) {
            NotificationsUtil.add("OnlyOfficerCanBuyLand")
            return
        }

        val ui = FloaterReg.showTypedInstance<FloaterBuyLandUI>("buy_land") ?: return
        ui.setForGroup(isForGroup)
        ui.setParcel(region, parcel)
    }

    fun updateCovenant(source: TextBase, assetId: String) {
        FloaterReg.findTypedInstance<FloaterBuyLandUI>("buy_land")
            ?.updateFloaterCovenant(source, assetId)
    }

    fun updateCovenantText(text: String, assetId: String) {
        FloaterReg.findTypedInstance<FloaterBuyLandUI>("buy_land")
            ?.updateFloaterCovenantText(text, assetId)
    }

    fun updateEstateName(name: String) {
        FloaterReg.findTypedInstance<FloaterBuyLandUI>("buy_land")
            ?.updateFloaterEstateName(name)
    }

    fun updateLastModified(text: String) {
        FloaterReg.findTypedInstance<FloaterBuyLandUI>("buy_land")
            ?.updateFloaterLastModified(text)
    }

    fun updateEstateOwnerName(name: String) {
        FloaterReg.findTypedInstance<FloaterBuyLandUI>("buy_land")
            ?.updateFloaterEstateOwnerName(name)
    }

    fun buildFloater(key: Any): Floater = FloaterBuyLandUI(key)
}

class FloaterBuyLandUI(key: Any) : Floater(key) {

    companion object {
        const val ICON_PAD = 2
    }

    private inner class SelectionObserver : ParcelObserver {
        override fun changed() {
            if (ViewerParcelMgr.instance.selectionEmpty()) {
                closeFloater()
            } else {
                setParcel(
                    ViewerParcelMgr.instance.getSelectionRegion(),
                    ViewerParcelMgr.instance.getParcelSelection()
                )
            }
        }
    }

    private val parcelSelectionObserver = SelectionObserver()
    private var region: ViewerRegion? = null
    private var parcel: ParcelSelectionHandle? = null
    private var isClaim = false
    private var isForGroup = false

    private var canBuy = false
    private var cannotBuyIsError = false
    private var cannotBuyReason = ""
    private var cannotBuyUri = ""

    private var bought = false

    private var agentCommittedTier = 0
    private var agentCashBalance = 0
    private var agentHasNeverOwnedLand = false

    private var parcelValid = false
    private var parcelIsForSale = false
    private var parcelIsGroupLand = false
    private var parcelGroupContribution = 0
    private var parcelPrice = 0
    private var parcelActualArea = 0
    private var parcelBillableArea = 0
    private var parcelSupportedObjects = 0
    private var parcelSoldWithObjects = false
    private var parcelLocation = ""
    private var parcelSnapshot = ""
    private var parcelSellerName = ""

    private var userPlanChoice = 0

    private var siteValid = false
    private var siteMembershipUpgrade = false
    private var siteMembershipAction = ""
    private val siteMembershipPlanIds: MutableList<String> = mutableListOf()
    private val siteMembershipPlanNames: MutableList<String> = mutableListOf()
    private var siteLandUseUpgrade = false
    private var siteLandUseAction = ""
    private var siteConfirm = ""

    private var preflightAskBillableArea = 0
    private var preflightAskCurrencyBuy = 0

    private val currency = CurrencyUIManager(this)

    private enum class TransactionType { Preflight, Currency, Buy }

    private var transaction: XmlRpcTransaction? = null
    private var transactionType = TransactionType.Preflight

    private var parcelBuyInfo: ParcelBuyInfo? = null

    init {
        ViewerParcelMgr.instance.addObserver(parcelSelectionObserver)
    }

    override fun onClose(appQuitting: Boolean) {
        setVisible(false)
        destroy()
    }

    override fun onDestroy() {
        ViewerParcelMgr.instance.removeObserver(parcelSelectionObserver)
        ViewerParcelMgr.instance.deleteParcelBuy(parcelBuyInfo)
        parcelBuyInfo = null
        transaction = null
    }

    fun setForGroup(forGroup: Boolean) {
        isForGroup = forGroup
    }

    fun setParcel(region: ViewerRegion, parcel: ParcelSelectionHandle) {
        if (transaction != null && transactionType == TransactionType.Buy) return

        this.region = region
        this.parcel = parcel

        updateAgentInfo()
        updateParcelInfo()
        updateCovenantInfo()
        if (canBuy) updateWebSiteInfo()
        refreshUI()
    }

    fun updateAgentInfo() {
        agentCommittedTier = StatusBar.getSquareMetersCommitted()
        agentCashBalance = StatusBar.getBalance()
        agentHasNeverOwnedLand = agentCommittedTier == 0
    }

    fun updateParcelInfo() {
        val parcelObj = parcel?.getParcel()
        parcelValid = parcelObj != null && region != null
        parcelIsForSale = false
        parcelIsGroupLand = false
        parcelGroupContribution = 0
        parcelPrice = 0
        parcelActualArea = 0
        parcelBillableArea = 0
        parcelSupportedObjects = 0
        parcelSoldWithObjects = false
        parcelLocation = ""
        parcelSnapshot = ""
        parcelSellerName = ""
        canBuy = false
        cannotBuyIsError = false

        if (!parcelValid) {
            cannotBuyReason = getString("no_land_selected")
            return
        }

        if (parcel!!.getMultipleOwners()) {
            cannotBuyReason = getString("multiple_parcels_selected")
            return
        }

        val parcelOwner = parcelObj!!.getOwnerID()

        isClaim = parcelObj.isPublic()
        if (!isClaim) {
            parcelActualArea = parcelObj.getArea()
            parcelIsForSale = parcelObj.getForSale()
            parcelIsGroupLand = parcelObj.getIsGroupOwned()
            parcelPrice = if (parcelIsForSale) parcelObj.getSalePrice() else 0

            if (parcelIsGroupLand) {
                val groupId = parcelObj.getGroupID()
                parcelGroupContribution = Agent.getGroupContribution(groupId)
            }
        } else {
            parcelActualArea = parcel!!.getClaimableArea()
            parcelIsForSale = true
            parcelPrice = parcelActualArea * parcelObj.getClaimPricePerMeter()
        }

        parcelBillableArea = (region!!.getBillableFactor() * parcelActualArea).roundToInt()
        parcelSupportedObjects = (parcelObj.getMaxPrimCapacity() * parcelObj.getParcelPrimBonus()).roundToInt()

        val selectionRegion = ViewerParcelMgr.instance.getSelectionRegion()
        if (selectionRegion != null) {
            val maxTasksPerRegion = selectionRegion.getMaxTasks()
            parcelSupportedObjects = min(parcelSupportedObjects, maxTasksPerRegion)
        }

        parcelSoldWithObjects = parcelObj.getSellWithObjects()

        val center = parcelObj.getCenterpoint()
        parcelLocation = "${region!!.getName()} ${center.x.toInt()},${center.y.toInt()}"
        parcelSnapshot = parcelObj.getSnapshotID()

        updateNames()

        val haveEnoughCash = parcelPrice <= agentCashBalance
        val cashBuy = if (haveEnoughCash) 0 else parcelPrice - agentCashBalance
        currency.setAmount(cashBuy, true)
        currency.setZeroMessage(if (haveEnoughCash) getString("none_needed") else "")

        if (isForGroup && !Agent.hasPowerInActiveGroup(GroupPowers.GP_LAND_DEED)) {
            cannotBuyReason = getString("cant_buy_for_group")
            return
        }

        if (!isClaim) {
            val authorizedBuyer = parcelObj.getAuthorizedBuyerID()
            val buyer = Agent.id
            val newOwner = if (isForGroup) Agent.getGroupID() else buyer

            if (!parcelIsForSale || (parcelPrice == 0 && authorizedBuyer.isNullOrEmpty())) {
                cannotBuyReason = getString("parcel_not_for_sale")
                return
            }

            if (parcelOwner == newOwner) {
                cannotBuyReason = if (isForGroup) getString("group_already_owns") else getString("you_already_own")
                return
            }

            if (!authorizedBuyer.isNullOrEmpty() && buyer != authorizedBuyer) {
                val authorizedGroup = Agent.hasPowerInGroup(authorizedBuyer, GroupPowers.GP_LAND_DEED)
                        && Agent.hasPowerInGroup(authorizedBuyer, GroupPowers.GP_LAND_SET_SALE_INFO)
                if (!authorizedGroup) {
                    cannotBuyReason = getString("set_to_sell_to_other")
                    return
                }
            }
        } else {
            if (parcelActualArea == 0) {
                cannotBuyReason = getString("no_public_land")
                return
            }
            if (parcel!!.hasOthersSelected()) {
                cannotBuyReason = getString("not_owned_by_you")
                return
            }
        }

        canBuy = true
    }

    fun updateCovenantInfo() {
        val selRegion = ViewerParcelMgr.instance.getSelectionRegion() ?: return

        val simAccess = selRegion.getSimAccess()
        val rating = ViewerRegion.accessToString(simAccess)
        val regionNameTxt = "${selRegion.getName()} ($rating)"
        getChildTextBox("region_name_text").setText(regionNameTxt)

        val ratingIcon = getChildIconCtrl("rating_icon")
        val regionNameCtrl = getChildTextBox("region_name_text")
        val regionNameWidth = min(regionNameCtrl.getRect().width, regionNameCtrl.getTextBoundingRect().width)
        val iconLeftPad = regionNameCtrl.getRect().left + regionNameWidth + ICON_PAD
        regionNameCtrl.setToolTip(regionNameCtrl.getText())
        ratingIcon.setOriginX(iconLeftPad)

        when (simAccess) {
            SimAccess.SIM_ACCESS_PG -> ratingIcon.setValue(getString("icon_PG"))
            SimAccess.SIM_ACCESS_ADULT -> ratingIcon.setValue(getString("icon_R"))
            else -> ratingIcon.setValue(getString("icon_M"))
        }

        getChildTextBox("region_type_text").apply {
            setText(selRegion.getLocalizedSimProductName())
            setToolTip(selRegion.getLocalizedSimProductName())
        }

        val canResellKey = if (selRegion.getRegionFlag(RegionFlags.REGION_FLAGS_BLOCK_LAND_RESELL)) "can_not_resell" else "can_resell"
        getChildTextBox("resellable_clause").setText(getString(canResellKey))

        val canChangeKey = if (selRegion.getRegionFlag(RegionFlags.REGION_FLAGS_ALLOW_PARCEL_CHANGES)) "can_change" else "can_not_change"
        getChildTextBox("changeable_clause").setText(getString(canChangeKey))

        getChildCheckBox("agree_covenant").apply {
            set(false)
            setEnabled(true)
            setCommitCallback { onChangeAgreeCovenant() }
        }

        getChildTextBox("covenant_text").setVisible(false)

        System.err.println("FloaterBuyLand: updateCovenantInfo not yet implemented")
    }

    private fun onChangeAgreeCovenant() {
        refreshUI()
    }

    fun updateFloaterCovenant(source: TextBase, assetId: String) {
        getChildViewerTextEditor("covenant_editor").copyContents(source)
        onCovenantTextUpdated(assetId)
    }

    fun updateFloaterCovenantText(text: String, assetId: String) {
        getChildViewerTextEditor("covenant_editor").setText(text)
        onCovenantTextUpdated(assetId)
    }

    private fun onCovenantTextUpdated(assetId: String) {
        val check = getChildCheckBox("agree_covenant")
        val box = getChildTextBox("covenant_text")
        if (assetId.isEmpty()) {
            check.set(true)
            check.setEnabled(false)
            refreshUI()
            box.setVisible(false)
        } else {
            check.setEnabled(true)
            box.setVisible(true)
        }
    }

    fun updateFloaterEstateName(name: String) {
        getChildTextBox("estate_name_text").apply {
            setText(name)
            setToolTip(name)
        }
    }

    fun updateFloaterLastModified(text: String) {
        getChildTextBox("covenant_timestamp_text").setText(text)
    }

    fun updateFloaterEstateOwnerName(name: String) {
        getChildTextBox("estate_owner_text").setText(name)
    }

    fun updateWebSiteInfo() {
        val askBillableArea = if (isForGroup) 0 else parcelBillableArea
        val askCurrencyBuy = currency.getAmount()

        if (transaction != null
            && transactionType == TransactionType.Preflight
            && preflightAskBillableArea == askBillableArea
            && preflightAskCurrencyBuy == askCurrencyBuy
        ) return

        preflightAskBillableArea = askBillableArea
        preflightAskCurrencyBuy = askCurrencyBuy

        val params = mapOf(
            "agentId" to Agent.id,
            "secureSessionId" to Agent.getSecureSessionID(),
            "language" to UILanguage.get(),
            "billableArea" to preflightAskBillableArea,
            "currencyBuy" to preflightAskCurrencyBuy
        )

        startTransaction(TransactionType.Preflight, params)
    }

    fun finishWebSiteInfo() {
        val result = transaction!!.response()

        siteValid = result["success"] as? Boolean ?: false
        if (!siteValid) {
            tellUserError(
                result["errorMessage"] as? String ?: "",
                result["errorURI"] as? String ?: ""
            )
            return
        }

        val membership = result["membership"] as Map<*, *>
        siteMembershipUpgrade = membership["upgrade"] as? Boolean ?: false
        siteMembershipAction = membership["action"] as? String ?: ""
        siteMembershipPlanIds.clear()
        siteMembershipPlanNames.clear()
        @Suppress("UNCHECKED_CAST")
        val levels = membership["levels"] as? List<Map<*, *>> ?: emptyList()
        for (level in levels) {
            siteMembershipPlanIds.add(level["id"] as? String ?: "")
            siteMembershipPlanNames.add(level["description"] as? String ?: "")
        }
        userPlanChoice = 0

        val landUse = result["landUse"] as Map<*, *>
        siteLandUseUpgrade = landUse["upgrade"] as? Boolean ?: false
        siteLandUseAction = landUse["action"] as? String ?: ""

        val currency = result["currency"] as? Map<*, *>
        currency?.get("estimatedCost")?.let { currency.setUsdEstimate(it as Int) }
        currency?.get("estimatedLocalCost")?.let { currency.setLocalEstimate(it as String) }

        siteConfirm = result["confirm"] as? String ?: ""
    }

    fun runWebSitePrep(password: String) {
        if (!canBuy) return

        val removeContribution = getChildCtrl("remove_contribution").getValue() as? Boolean ?: false
        parcelBuyInfo = ViewerParcelMgr.instance.setupParcelBuy(
            Agent.id, Agent.getSessionID(),
            Agent.getGroupID(), isForGroup, isClaim, removeContribution
        )

        if (parcelBuyInfo != null
            && !siteMembershipUpgrade
            && !siteLandUseUpgrade
            && currency.getAmount() == 0
            && siteConfirm != "password"
        ) {
            sendBuyLand()
            return
        }

        var newLevel = "noChange"
        if (siteMembershipUpgrade) {
            val levels = getChildComboBox("account_level")
            if (levels != null) {
                userPlanChoice = levels.getCurrentIndex()
                newLevel = siteMembershipPlanIds[userPlanChoice]
            }
        }

        val params = mutableMapOf(
            "agentId" to Agent.id,
            "secureSessionId" to Agent.getSecureSessionID(),
            "language" to UILanguage.get(),
            "levelId" to newLevel,
            "billableArea" to (if (isForGroup) 0 else parcelBillableArea),
            "currencyBuy" to currency.getAmount(),
            "estimatedCost" to currency.getUsdEstimate(),
            "estimatedLocalCost" to currency.getLocalEstimate(),
            "confirm" to siteConfirm
        )
        if (password.isNotEmpty()) params["password"] = password

        startTransaction(TransactionType.Buy, params)
    }

    fun finishWebSitePrep() {
        val result = transaction!!.response()
        val success = result["success"] as? Boolean ?: false
        if (!success) {
            tellUserError(
                result["errorMessage"] as? String ?: "",
                result["errorURI"] as? String ?: ""
            )
            return
        }
        sendBuyLand()
    }

    fun sendBuyLand() {
        parcelBuyInfo?.let {
            ViewerParcelMgr.instance.sendParcelBuy(it)
            ViewerParcelMgr.instance.deleteParcelBuy(it)
            parcelBuyInfo = null
            bought = true
        }
    }

    fun updateNames() {
        val parcelObj = parcel?.getParcel() ?: run {
            parcelSellerName = ""
            return
        }

        parcelSellerName = when {
            isClaim -> "Linden Lab"
            parcelObj.getIsGroupOwned() -> {
                CacheName.getGroup(parcelObj.getGroupID()) { id, name, _ ->
                    if (parcelObj.getGroupID() == id) parcelSellerName = name
                }
                parcelSellerName
            }
            else -> SLURL("agent", parcelObj.getOwnerID(), "completename").getSLURLString()
        }
    }

    fun startTransaction(type: TransactionType, params: Map<String, Any>) {
        transaction = null
        transactionType = type

        val transactionUri = GridManager.instance.getHelperURI() + "landtool.php"
        val method = when (type) {
            TransactionType.Preflight -> "preflightBuyLandPrep"
            TransactionType.Buy -> "buyLandPrep"
            else -> return
        }

        transaction = XmlRpcTransaction(transactionUri, method, params)
    }

    fun checkTransaction(): Boolean {
        val tx = transaction ?: return false
        if (!tx.process()) return false

        if (tx.status() != XmlRpcTransaction.Status.Complete) {
            tellUserError(tx.statusMessage(), tx.statusURI())
        } else {
            when (transactionType) {
                TransactionType.Preflight -> finishWebSiteInfo()
                TransactionType.Buy -> finishWebSitePrep()
                else -> Unit
            }
        }

        transaction = null
        return true
    }

    fun tellUserError(message: String, uri: String) {
        canBuy = false
        cannotBuyIsError = true
        cannotBuyReason = getString("fetching_error") + message
        cannotBuyUri = uri
    }

    override fun postBuild(): Boolean {
        setVisibleCallback { newVisibility -> if (newVisibility) refreshUI() }

        currency.prepare()

        getChildView("buy_btn").setCommitCallback { onClickBuy() }
        getChildView("cancel_btn").setCommitCallback { onClickCancel() }
        getChildView("error_web").setCommitCallback { onClickErrorWeb() }

        center()
        return true
    }

    override fun draw() {
        super.draw()

        var needsUpdate = checkTransaction()
        needsUpdate = currency.process() || needsUpdate

        if (bought) {
            closeFloater()
        } else if (needsUpdate) {
            if (canBuy && currency.hasError()) {
                tellUserError(currency.errorMessage(), currency.errorURI())
            }
            refreshUI()
        }
    }

    override fun canClose(): Boolean {
        val canClose = transaction == null && (transactionType != TransactionType.Buy || currency.canCancel())
        if (!canClose) {
            NotificationsUtil.add("CannotCloseFloaterBuyLand")
        }
        return canClose
    }

    fun refreshUI() {
        getChildTextureCtrl("info_image")?.setImageAssetID(
            if (parcelValid) parcelSnapshot else ""
        )

        if (parcelValid) {
            getChildCtrl("info_parcel").setValue(parcelLocation)

            val costPerSqm = if (parcelActualArea > 0) parcelPrice.toFloat() / parcelActualArea.toFloat() else 0f
            getChildCtrl("info_size").setValue(
                getString("meters_supports_object")
                    .replace("[AMOUNT]", parcelActualArea.toString())
                    .replace("[AMOUNT2]", parcelSupportedObjects.toString())
            )

            val soldWithStr = if (parcelSoldWithObjects) getString("sold_with_objects") else getString("sold_without_objects")
            getChildCtrl("info_price").setValue(
                getString("info_price_string")
                    .replace("[PRICE]", parcelPrice.toString())
                    .replace("[PRICE_PER_SQM]", "%.1f".format(costPerSqm))
                    .replace("[SOLD_WITH_OBJECTS]", soldWithStr)
            )
            getChildView("info_price").setVisible(parcelIsForSale)
        } else {
            getChildCtrl("info_parcel").setValue(getString("no_parcel_selected"))
            getChildCtrl("info_size").setValue("")
            getChildCtrl("info_price").setValue("")
        }

        val infoAction = when {
            canBuy -> if (isForGroup) getString("buying_for_group") else getString("buying_will")
            cannotBuyIsError -> getString("cannot_buy_now")
            else -> getString("not_for_sale")
        }
        getChildCtrl("info_action").setValue(infoAction)

        val showingError = !canBuy || !siteValid

        if (showingError) {
            setBadge("step_error", if (cannotBuyIsError) BadgeType.ERROR else BadgeType.WARN)
            getChildTextBox("error_message")?.apply {
                setVisible(true)
                setValue(if (!canBuy) cannotBuyReason else "(waiting for data)")
            }
            getChildView("error_web").setVisible(cannotBuyIsError && cannotBuyUri.isNotEmpty())
        } else {
            getChildView("step_error").setVisible(false)
            getChildView("error_message").setVisible(false)
            getChildView("error_web").setVisible(false)
        }

        if (!showingError) {
            setBadge("step_1", if (siteMembershipUpgrade) BadgeType.NOTE else BadgeType.OK)
            getChildCtrl("account_action").setValue(siteMembershipAction)
            getChildCtrl("account_reason").setValue(
                if (siteMembershipUpgrade) getString("must_upgrade") else getString("cant_own_land")
            )

            val levels = getChildComboBox("account_level")
            if (levels != null) {
                levels.setVisible(siteMembershipUpgrade)
                levels.removeAll()
                for (planName in siteMembershipPlanNames) levels.add(planName)
                levels.setCurrentByIndex(userPlanChoice)
            }

            getChildView("step_1").setVisible(true)
            getChildView("account_action").setVisible(true)
            getChildView("account_reason").setVisible(true)
        } else {
            getChildView("step_1").setVisible(false)
            getChildView("account_action").setVisible(false)
            getChildView("account_reason").setVisible(false)
            getChildView("account_level").setVisible(false)
        }

        if (!showingError) {
            setBadge("step_2", if (siteLandUseUpgrade) BadgeType.NOTE else BadgeType.OK)
            getChildCtrl("land_use_action").setValue(siteLandUseAction)

            var message = if (isForGroup) {
                getString("insufficient_land_credits").replace("[GROUP]", Agent.getGroupName())
            } else {
                getString("land_holdings").replace("[BUYER]", agentCommittedTier.toString())
            }

            message += when {
                !parcelValid -> Trans.getSentencesSeparator() + getString("no_parcel_selected")
                parcelBillableArea == parcelActualArea ->
                    Trans.getSentencesSeparator() + getString("parcel_meters").replace("[AMOUNT]", "$parcelActualArea ")
                parcelBillableArea > parcelActualArea ->
                    Trans.getSentencesSeparator() + getString("premium_land").replace("[AMOUNT]", "$parcelBillableArea ")
                else ->
                    Trans.getSentencesSeparator() + getString("discounted_land").replace("[AMOUNT]", "$parcelBillableArea ")
            }

            getChildCtrl("land_use_reason").setValue(message)
            getChildView("step_2").setVisible(true)
            getChildView("land_use_action").setVisible(true)
            getChildView("land_use_reason").setVisible(true)
        } else {
            getChildView("step_2").setVisible(false)
            getChildView("land_use_action").setVisible(false)
            getChildView("land_use_reason").setVisible(false)
        }

        val finalBalance = agentCashBalance + currency.getAmount() - parcelPrice
        val willHaveEnough = finalBalance >= 0
        val haveEnough = agentCashBalance >= parcelPrice
        val minContribution = ceil(parcelBillableArea.toFloat() / GROUP_LAND_BONUS_FACTOR).toInt()
        val groupContributionEnough = parcelGroupContribution >= minContribution

        currency.updateUI(!showingError && !haveEnough)

        if (!showingError) {
            val badge3 = when {
                !willHaveEnough -> BadgeType.WARN
                currency.getAmount() > 0 -> BadgeType.NOTE
                else -> BadgeType.OK
            }
            setBadge("step_3", badge3)

            getChildCtrl("purchase_action").setValue(
                getString("pay_to_for_land")
                    .replace("[AMOUNT]", parcelPrice.toString())
                    .replace("[SELLER]", parcelSellerName)
            )
            getChildView("purchase_action").setVisible(parcelValid)

            if (haveEnough) {
                getChildCtrl("currency_reason").setValue(
                    getString("have_enough_lindens").replace("[AMOUNT]", agentCashBalance.toString())
                )
            } else {
                getChildCtrl("currency_reason").setValue(
                    getString("not_enough_lindens")
                        .replace("[AMOUNT]", agentCashBalance.toString())
                        .replace("[AMOUNT2]", (parcelPrice - agentCashBalance).toString())
                )
                getChildCtrl("currency_est").setTextArg("[LOCAL_AMOUNT]", currency.getLocalEstimate())
            }

            if (willHaveEnough) {
                getChildCtrl("currency_balance").setValue(
                    getString("balance_left").replace("[AMOUNT]", finalBalance.toString())
                )
            } else {
                getChildCtrl("currency_balance").setValue(
                    getString("balance_needed").replace("[AMOUNT]", (parcelPrice - agentCashBalance).toString())
                )
            }

            getChildCtrl("remove_contribution").setValue(groupContributionEnough)
            getChildView("remove_contribution").setEnabled(groupContributionEnough)
            val showRemoveContribution = parcelIsGroupLand && parcelGroupContribution > 0
            getChildView("remove_contribution").setLabelArg("[AMOUNT]", minContribution.toString())
            getChildView("remove_contribution").setVisible(showRemoveContribution)

            getChildView("step_3").setVisible(true)
            getChildView("purchase_action").setVisible(true)
            getChildView("currency_reason").setVisible(true)
            getChildView("currency_balance").setVisible(true)
        } else {
            getChildView("step_3").setVisible(false)
            getChildView("purchase_action").setVisible(false)
            getChildView("currency_reason").setVisible(false)
            getChildView("currency_balance").setVisible(false)
            getChildView("remove_group_donation").setVisible(false)
        }

        val agreesToCovenant = getChildCheckBox("agree_covenant")?.get() ?: false
        getChildView("buy_btn").setEnabled(
            canBuy && siteValid && willHaveEnough && transaction == null && agreesToCovenant
        )
    }

    fun startBuyPreConfirm() {
        var action = ""
        if (siteMembershipUpgrade) {
            action += "$siteMembershipAction\n"
            val levels = getChildComboBox("account_level")
            if (levels != null) {
                action += " * ${siteMembershipPlanNames[levels.getCurrentIndex()]}\n"
            }
        }
        if (siteLandUseUpgrade) {
            action += "$siteLandUseAction\n"
        }
        if (currency.getAmount() > 0) {
            action += getString("buy_for_US")
                .replace("[AMOUNT]", currency.getAmount().toString())
                .replace("[LOCAL_AMOUNT]", currency.getLocalEstimate())
        }
        action += getString("pay_to_for_land")
            .replace("[AMOUNT]", parcelPrice.toString())
            .replace("[SELLER]", parcelSellerName)

        ConfirmationManager.confirm(siteConfirm, action, this, ::startBuyPostConfirm)
    }

    fun startBuyPostConfirm(password: String) {
        runWebSitePrep(password)
        canBuy = false
        cannotBuyReason = getString("processing")
        refreshUI()
    }

    private fun onClickBuy() {
        startBuyPreConfirm()
    }

    private fun onClickCancel() {
        closeFloater()
    }

    private fun onClickErrorWeb() {
        Web.loadURLExternal(cannotBuyUri)
        closeFloater()
    }
}
