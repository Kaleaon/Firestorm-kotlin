package com.firestorm.newview

import java.util.UUID

open class LLListContextMenu {
    protected val mUUIDs: MutableList<UUID> = mutableListOf()
    open fun show(parent: Any, uuids: List<UUID>, x: Int, y: Int) {
        TODO("APR: use JVM equivalent - show context menu at screen position")
    }
}

enum class EDragAndDropType {
    DAD_PERSON,
    DAD_NONE,
}

enum class EAcceptance {
    ACCEPT_NO,
    ACCEPT_YES_SINGLE,
    ACCEPT_YES_MULTI,
}

open class LLScrollListItem {
    open fun getUUID(): UUID = TODO("APR: use JVM equivalent")
    open fun getValue(): UUID = TODO("APR: use JVM equivalent")
}

open class LLScrollListCtrl {
    open fun getAllSelected(): List<LLScrollListItem> = TODO("APR: use JVM equivalent")
    open fun hitItem(x: Int, y: Int): LLScrollListItem? = TODO("APR: use JVM equivalent")
    open fun selectByID(value: UUID): Unit = TODO("APR: use JVM equivalent")
    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean = TODO("APR: use JVM equivalent")
    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean = TODO("APR: use JVM equivalent")
    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = TODO("APR: use JVM equivalent")
    open fun handleHover(x: Int, y: Int, mask: Int): Boolean = TODO("APR: use JVM equivalent")
    open fun setLineHeight(height: Int): Unit = TODO("APR: use JVM equivalent")
    open fun updateLineHeight(): Unit = TODO("APR: use JVM equivalent")
    open fun updateLayout(): Unit = TODO("APR: use JVM equivalent")
    open fun localPointToScreen(x: Int, y: Int, screenXOut: IntArray, screenYOut: IntArray): Unit = TODO("APR: use JVM equivalent")
    open fun hasMouseCapture(): Boolean = TODO("APR: use JVM equivalent")
    open fun setContextMenu(menu: LLListContextMenu?) {}
}

open class FSScrollListCtrl(params: Params) : LLScrollListCtrl() {

    enum class EContentType {
        AGENTS,
        MISC,
    }

    data class Params(
        val desiredLineHeight: Int = -1,
        val contentType: EContentType = EContentType.MISC,
    )

    protected var mContextMenu: LLListContextMenu? = null
    protected val mDesiredLineHeight: Int = params.desiredLineHeight
    protected val mContentType: EContentType = params.contentType

    var handleDaDCallback: ((Int, Int, Int, Boolean, EDragAndDropType, Any?, EAcceptance?, String) -> Boolean)? = null

    override fun setContextMenu(menu: LLListContextMenu?) {
        mContextMenu = menu
    }

    fun refreshLineHeight() {
        if (mDesiredLineHeight > -1) {
            setLineHeight(mDesiredLineHeight)
        } else {
            updateLineHeight()
        }
        updateLayout()
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val contextMenu = mContextMenu
        return if (contextMenu != null) {
            val handled = super.handleRightMouseDown(x, y, mask)
            val selectedItems = getAllSelected()
            if (selectedItems.size > 1) {
                val selectedUuids = selectedItems.map { it.getUUID() }
                contextMenu.show(this, selectedUuids, x, y)
            } else {
                val hitItem = hitItem(x, y)
                if (hitItem != null) {
                    val value = hitItem.getValue()
                    selectByID(value)
                    contextMenu.show(this, listOf(value), x, y)
                }
            }
            handled
        } else {
            super.handleRightMouseDown(x, y, mask)
        }
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (mContentType == EContentType.AGENTS) {
            TODO("APR: use JVM equivalent - capture mouse focus and record drag start screen position")
        }
        return super.handleMouseDown(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (mContentType == EContentType.AGENTS && hasMouseCapture()) {
            TODO("APR: use JVM equivalent - release mouse capture focus")
        }
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (mContentType == EContentType.AGENTS) {
            if (hasMouseCapture()) {
                val screenX = IntArray(1)
                val screenY = IntArray(1)
                localPointToScreen(x, y, screenX, screenY)
                TODO("APR: use JVM equivalent - check drag threshold; if exceeded, collect selected item UUIDs and begin multi-drag with DAD_PERSON type from SOURCE_PEOPLE")
            }
            return super.handleHover(x, y, mask)
        }
        return super.handleHover(x, y, mask)
    }

    open fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: EDragAndDropType, cargoData: Any?,
        accept: EAcceptance?, tooltipMsg: String,
    ): Boolean {
        val cb = handleDaDCallback ?: return false
        return cb(x, y, mask, drop, cargoType, cargoData, accept, tooltipMsg)
    }
}
