package com.firestorm.newview

import java.util.UUID

class InspectObject(objectIdData: Map<String, Any?>) : Inspect() {

    private var objectId: UUID = UUID(0, 0)
    private var previousObjectId: UUID = UUID(0, 0)
    private var objectFace: Int = 0

    private val commitCallbacks: MutableMap<String, () -> Unit> = mutableMapOf()

    init {
        commitCallbacks["InspectObject.Buy"] = ::onClickBuy
        commitCallbacks["InspectObject.Pay"] = ::onClickPay
        commitCallbacks["InspectObject.TakeFreeCopy"] = ::onClickTakeFreeCopy
        commitCallbacks["InspectObject.Touch"] = ::onClickTouch
        commitCallbacks["InspectObject.Sit"] = ::onClickSit
        commitCallbacks["InspectObject.Open"] = ::onClickOpen
        commitCallbacks["InspectObject.MoreInfo"] = ::onClickMoreInfo
        commitCallbacks["InspectObject.ZoomIn"] = ::onClickZoomIn
    }

    fun postBuild(): Boolean {
        TODO("APR: clear object_name, object_creator, object_description, object_media_url controls; hideButtons(); wire URL-click on object_creator to closeFloater; wire all button commit callbacks; subscribe to LLSelectMgr.mUpdateSignal -> update()")
    }

    override fun onOpen(data: Map<String, Any?>) {
        super.onOpen(data)
        objectId = data["object_id"] as? UUID ?: UUID(0, 0)
        objectFace = (data["object_face"] as? Int) ?: 0

        repositionInspector(data)

        TODO("APR: find object in gObjectList; clear media focus; deselectAll; selectObjectAndFamily; mark selection as transient; resolve media entry and impl for objectFace")
    }

    fun onClose(appQuitting: Boolean) {
        TODO("APR: release mObjectSelection = null; save previousObjectId = objectId; hide gear_btn menu")
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        TODO("APR: only unpause fade timer if gear menu is not visible and no child popup menu is visible")
    }

    private fun update() {
        TODO("APR: bail if not visible; get first root node from LLSelectMgr selection; if !nodep.mValid and same as previous object, return; call updateButtons, updateName, updateDescription, updateCreator, updatePrice, updateMediaCurrentURL, updateSecureBrowsing")
    }

    private fun hideButtons() {
        TODO("APR: setVisible(false) on buy_btn, pay_btn, take_free_copy_btn, touch_btn, sit_btn, open_btn")
    }

    private fun updateButtons(nodeValid: Boolean, objectFlags: Int, parentFlags: Int, forCopy: Boolean, forSale: Boolean, price: Int, clickAction: UByte) {
        hideButtons()
        TODO("APR: choose the highest-priority visible button: free-copy > buy > pay > sit(click-action) > touch(handleTouch flag) > open > sit(default); also apply RLV canTouch/canSit checks on touch_btn and sit_btn; call focusFirstItem(false, false)")
    }

    private fun updateSitLabel(sitName: String) {
        TODO("APR: set sit_btn label to sitName if non-empty else getString('Sit'); if RLV enabled, enable/disable based on RlvActions.canSit(pick.object, pick.offset)")
    }

    private fun updateTouchLabel(touchName: String) {
        TODO("APR: set touch_btn label to touchName if non-empty else getString('Touch')")
    }

    private fun updateName(name: String) {
        val displayName = name.ifEmpty { "Tooltip_No_Name" }
        TODO("APR: set object_name control value to displayName")
    }

    private fun updateDescription(description: String) {
        val desc = if (description == "(No Description)") "" else description
        TODO("APR: set object_description textbox value to desc")
    }

    private fun updatePrice(forCopy: Boolean, forSale: Boolean, price: Int) {
        TODO("APR: display 'Free' if forCopy or price==0; display formatted price if forSale; show/hide price_icon accordingly")
    }

    private fun updateCreator(creatorId: UUID, ownerId: UUID, groupOwned: Boolean, nodeValid: Boolean) {
        TODO("APR: if nodeValid, build SLURL links for creator and owner (RLV-anonym if canShowName() returns false for nearby avatars); choose 'Creator' or 'CreatorAndOwner' string; set object_creator control value")
    }

    private fun updateMediaCurrentURL() {
        TODO("APR: determine current URL from media plugin (location or mediaURL depending on pluginSupportsMediaTime); fall back to mediaEntry.currentURL; set object_media_url textbox text and tooltip")
    }

    private fun updateSecureBrowsing() {
        TODO("APR: determine current URL from media impl/plugin; set secure_browsing control visible iff URL starts with 'https://'")
    }

    private fun onClickBuy() {
        TODO("APR: handle_buy(); close floater")
    }

    private fun onClickPay() {
        TODO("APR: handle_give_money_dialog(); close floater")
    }

    private fun onClickTakeFreeCopy() {
        TODO("APR: if forCopy call handle_take_copy() else call handle_buy(); close floater")
    }

    private fun onClickTouch() {
        TODO("APR: handle_object_touch(); close floater")
    }

    private fun onClickSit() {
        TODO("APR: if currently sitting on this object, standUp() (check RLV canStand); else handle_object_sit(objectId); close floater")
    }

    private fun onClickOpen() {
        TODO("APR: LLFloaterReg.showInstance('openobject'); close floater")
    }

    private fun onClickMoreInfo() {
        TODO("APR: LLFloaterReg.showInstance('task_properties'); close floater")
    }

    private fun onClickZoomIn() {
        TODO("APR: handle_look_at_selection('zoom'); close floater")
    }

    companion object {
        fun registerFloater() {
            TODO("APR: LLFloaterReg.add(\"inspect_object\", \"inspect_object.xml\", build<InspectObject>)")
        }
    }
}

object InspectObjectUtil {
    fun registerFloater() = InspectObject.registerFloater()
}
