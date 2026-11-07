package com.firestorm.newview

open class DragAndDropButton : Button() {

    typealias DragDropHandler = (
        x: Int,
        y: Int,
        mask: Int,
        drop: Boolean,
        cargoType: DragAndDropType,
        cargoData: Any?,
        accept: AcceptanceHolder,
        tooltipMsg: StringBuilder
    ) -> Boolean

    private var mDragDropHandler: DragDropHandler? = null

    fun setDragAndDropHandler(handler: DragDropHandler) {
        mDragDropHandler = handler
    }

    override fun handleDragAndDrop(
        x: Int,
        y: Int,
        mask: Int,
        drop: Boolean,
        cargoType: DragAndDropType,
        cargoData: Any?,
        accept: AcceptanceHolder,
        tooltipMsg: StringBuilder
    ): Boolean {
        return mDragDropHandler?.invoke(x, y, mask, drop, cargoType, cargoData, accept, tooltipMsg)
            ?: false
    }
}
