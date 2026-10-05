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
        System.err.println("InspectObject: postBuild not yet implemented")
        return false
    }

    override fun onOpen(data: Map<String, Any?>) {
        super.onOpen(data)
        objectId = data["object_id"] as? UUID ?: UUID(0, 0)
        objectFace = (data["object_face"] as? Int) ?: 0

        repositionInspector(data)

        System.err.println("InspectObject: onOpen not yet implemented")
    }

    fun onClose(appQuitting: Boolean) {
        System.err.println("InspectObject: onClose not yet implemented")
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        System.err.println("InspectObject: onMouseLeave not yet implemented")
    }

    private fun update() {
        System.err.println("InspectObject: update not yet implemented")
    }

    private fun hideButtons() {
        System.err.println("InspectObject: hideButtons not yet implemented")
    }

    private fun updateButtons(nodeValid: Boolean, objectFlags: Int, parentFlags: Int, forCopy: Boolean, forSale: Boolean, price: Int, clickAction: UByte) {
        hideButtons()
        System.err.println("InspectObject: updateButtons not yet implemented")
    }

    private fun updateSitLabel(sitName: String) {
        System.err.println("InspectObject: updateSitLabel not yet implemented")
    }

    private fun updateTouchLabel(touchName: String) {
        System.err.println("InspectObject: updateTouchLabel not yet implemented")
    }

    private fun updateName(name: String) {
        val displayName = name.ifEmpty { "Tooltip_No_Name" }
        System.err.println("InspectObject: updateName not yet implemented")
    }

    private fun updateDescription(description: String) {
        val desc = if (description == "(No Description)") "" else description
        System.err.println("InspectObject: updateDescription not yet implemented")
    }

    private fun updatePrice(forCopy: Boolean, forSale: Boolean, price: Int) {
        System.err.println("InspectObject: updatePrice not yet implemented")
    }

    private fun updateCreator(creatorId: UUID, ownerId: UUID, groupOwned: Boolean, nodeValid: Boolean) {
        System.err.println("InspectObject: updateCreator not yet implemented")
    }

    private fun updateMediaCurrentURL() {
        System.err.println("InspectObject: updateMediaCurrentURL not yet implemented")
    }

    private fun updateSecureBrowsing() {
        System.err.println("InspectObject: updateSecureBrowsing not yet implemented")
    }

    private fun onClickBuy() {
        System.err.println("InspectObject: onClickBuy not yet implemented")
    }

    private fun onClickPay() {
        System.err.println("InspectObject: onClickPay not yet implemented")
    }

    private fun onClickTakeFreeCopy() {
        System.err.println("InspectObject: onClickTakeFreeCopy not yet implemented")
    }

    private fun onClickTouch() {
        System.err.println("InspectObject: onClickTouch not yet implemented")
    }

    private fun onClickSit() {
        System.err.println("InspectObject: onClickSit not yet implemented")
    }

    private fun onClickOpen() {
        System.err.println("InspectObject: onClickOpen not yet implemented")
    }

    private fun onClickMoreInfo() {
        System.err.println("InspectObject: onClickMoreInfo not yet implemented")
    }

    private fun onClickZoomIn() {
        System.err.println("InspectObject: onClickZoomIn not yet implemented")
    }

    companion object {
        fun registerFloater() {
            System.err.println("InspectObject: registerFloater not yet implemented")
        }
    }
}

object InspectObjectUtil {
    fun registerFloater() = InspectObject.registerFloater()
}
