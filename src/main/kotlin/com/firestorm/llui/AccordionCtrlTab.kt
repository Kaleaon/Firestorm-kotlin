package com.firestorm.llui

import kotlin.math.max
import kotlin.math.min

private const val HEADER_HEIGHT = 23
private const val HEADER_IMAGE_LEFT_OFFSET = 5
private const val HEADER_TEXT_LEFT_OFFSET = 30
private const val AUTO_OPEN_TIME = 1f
private const val VERTICAL_MULTIPLE = 16
private const val PARENT_BORDER_MARGIN = 5

class AccordionCtrlTab(
    displayChildren: Boolean = true,
    collapsible: Boolean = true,
    title: String = "",
    headerHeight: Int = HEADER_HEIGHT,
    val minWidth: Int = 0,
    val minHeight: Int = 0,
    val paddingLeft: Int = 2,
    val paddingRight: Int = 2,
    val paddingTop: Int = 2,
    val paddingBottom: Int = 2,
    val headerVisible: Boolean = true,
    val fitPanel: Boolean = true,
    val selectionEnabled: Boolean = false
) {
    inner class TabHeader(private val tab: AccordionCtrlTab) {
        var title: String = tab.headerTitle
        var needsHighlight: Boolean = false
        var isSelected: Boolean = false
        private var autoOpenElapsed: Float = 0f
        private var autoOpenStarted: Boolean = false

        fun getTitle(): String = title

        fun setTitle(newTitle: String, highlight: String = "") {
            title = newTitle
        }

        fun setTitleFontStyle(style: String) {
            System.err.println("TabHeader: GPU: restyle text in header textbox not yet implemented")
        }

        fun setTitleColor(color: Any) {
            System.err.println("TabHeader: GPU: set color on header textbox not yet implemented")
        }

        fun setSelected(selected: Boolean) {
            isSelected = selected
        }

        fun setFocus(focus: Boolean) {
            System.err.println("TabHeader: GPU: route keyboard focus to header not yet implemented")
        }

        fun hasFocus(): Boolean {
            return false
        }

        fun draw(width: Int, height: Int) {
            // no-op
        }

        fun reshape(width: Int, height: Int) {
            System.err.println("TabHeader: GPU: reposition header textbox within new bounds not yet implemented")
        }

        fun onMouseEnter(x: Int, y: Int, mask: Int) {
            needsHighlight = true
        }

        fun onMouseLeave(x: Int, y: Int, mask: Int) {
            needsHighlight = false
            autoOpenStarted = false
            autoOpenElapsed = 0f
        }

        fun handleKey(key: Int, mask: Int, calledFromParent: Boolean): Boolean {
            return false
        }

        fun handleDragAndDrop(
            x: Int, y: Int, mask: Int, drop: Boolean,
            cargoType: Int, cargoData: Any?,
            tooltipMsg: StringBuilder,
            frameDelta: Float
        ): Boolean {
            if (!tab.displayChildren && tab.collapsible && tab.canOpenClose) {
                if (autoOpenStarted) {
                    autoOpenElapsed += frameDelta
                    if (autoOpenElapsed > AUTO_OPEN_TIME) {
                        tab.changeOpenClose(false)
                        autoOpenStarted = false
                        autoOpenElapsed = 0f
                        return true
                    }
                } else {
                    autoOpenStarted = true
                }
            }
            return false
        }
    }

    private val header: TabHeader = TabHeader(this)

    var displayChildren: Boolean = displayChildren
        set(value) {
            field = value
            val newHeight = if (value) expandedHeight else getHeaderHeight()
            rectHeight = rectTop - (rectTop - newHeight)
            containerPanel?.visible = value
            if (value) adjustContainerPanel()
            else scrollbarVisible = false
        }

    var collapsible: Boolean = collapsible
    var canOpenClose: Boolean = true
    var expandedHeight: Int = 0

    var onDropDownStateChanged: (() -> Unit)? = null

    var accordionView: ViewStub? = null
        private set

    internal var headerTitle: String = title

    var rectLeft: Int = 0
    var rectTop: Int = 0
    var rectWidth: Int = 100
    var rectHeight: Int = 200

    var visible: Boolean = true

    private var containerPanel: ViewStub? = null
    private var scrollbarVisible: Boolean = false
    private var scrollbarDocPos: Int = 0
    private var containerPanelHeight: Int = 0

    var skipChangesOnNotifyParent: Boolean = false

    private var storedOpenCloseState: Boolean = false
    private var wasStateStored: Boolean = false

    val isExpanded: Boolean get() = displayChildren

    fun getHeaderHeight(): Int = if (headerVisible) HEADER_HEIGHT else 0

    fun postBuild(): Boolean {
        header.setTitle(headerTitle)
        if (!fitPanel) {
            scrollbarVisible = false
        }
        containerPanel?.visible = displayChildren
        return true
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        rectWidth = width
        rectHeight = height

        val headerRect = Rect(0, height, width, HEADER_HEIGHT)
        header.reshape(headerRect.width, headerRect.height)

        if (!displayChildren) return

        val childRect = Rect(
            paddingLeft,
            height - getHeaderHeight() - paddingTop,
            width - paddingLeft - paddingRight,
            height - getHeaderHeight() - paddingTop - paddingBottom
        )
        adjustContainerPanel(childRect)
    }

    fun draw() {
        // no-op
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (collapsible && headerVisible && canOpenClose) {
            if (y >= rectHeight - HEADER_HEIGHT) {
                header.setFocus(true)
                changeOpenClose(displayChildren)
                wasStateStored = false
                return true
            }
        }
        return false
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = false

    fun handleKey(key: Int, mask: Int, calledFromParent: Boolean): Boolean {
        if (!header.hasFocus()) return false

        val KEY_RETURN = 13; val KEY_ADD = 107; val KEY_RIGHT = 39
        val KEY_SUBTRACT = 109; val KEY_LEFT = 37
        val KEY_DOWN = 40; val KEY_UP = 38
        val MASK_NONE = 0

        if (key == KEY_RETURN && mask == MASK_NONE) { changeOpenClose(displayChildren); return true }
        if ((key == KEY_ADD || key == KEY_RIGHT) && mask == MASK_NONE && !displayChildren) {
            changeOpenClose(displayChildren); return true
        }
        if ((key == KEY_SUBTRACT || key == KEY_LEFT) && mask == MASK_NONE && displayChildren) {
            changeOpenClose(displayChildren); return true
        }
        if (key == KEY_DOWN && mask == MASK_NONE) {
            if (!displayChildren) {
                System.err.println("AccordionCtrlTab: APR: use JVM equivalent - notify parent select_next not yet implemented")
            } else {
                accordionView?.notify(mapOf("action" to "select_first"))
            }
            return true
        }
        if (key == KEY_UP && mask == MASK_NONE) {
            System.err.println("AccordionCtrlTab: APR: use JVM equivalent - notify parent select_prev not yet implemented")
        }
        return false
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        if (y >= rectHeight - HEADER_HEIGHT - HEADER_HEIGHT / 2) {
            System.err.println("AccordionCtrlTab: GPU: delegate to header handleToolTip not yet implemented")
        }
        return false
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (scrollbarVisible) return true
        return false
    }

    fun addChild(child: ViewStub, tabGroup: Int = 0): Boolean {
        if (child.name != "dd_header") {
            reshape(child.width, child.height + HEADER_HEIGHT)
            expandedHeight = rectHeight
        }
        if (child.name != "dd_header") {
            if (!collapsible) displayChildren = true
            else displayChildren = displayChildren
        }
        if (containerPanel == null) containerPanel = findContainerView()
        return true
    }

    fun setAccordionView(panel: ViewStub) {
        accordionView = panel
        addChild(panel, 0)
    }

    fun getTitle(): String = header.getTitle()

    fun setTitle(title: String, highlight: String = "") {
        header.setTitle(title, highlight)
    }

    fun setTitleFontStyle(style: String) {
        header.setTitleFontStyle(style)
    }

    fun setTitleColor(color: Any) {
        header.setTitleColor(color)
    }

    fun setSelected(isSelected: Boolean) {
        header.setSelected(isSelected)
    }

    fun setDropDownStateChangedCallback(cb: () -> Unit) {
        onDropDownStateChanged = cb
    }

    fun changeOpenClose(isOpen: Boolean) {
        if (isOpen) expandedHeight = rectHeight
        displayChildren = !isOpen
        reshape(rectWidth, rectHeight, false)
        onDropDownStateChanged?.invoke()
    }

    fun onVisibilityChange(newVisibility: Boolean) {
        System.err.println("AccordionCtrlTab: onVisibilityChange notify parent child_visibility_change not yet implemented")
    }

    fun onUpdateScrollToChild(ctrl: UICtrlStub) {
        if (scrollbarVisible) {
            // no-op
        }
    }

    fun notifyChildren(info: Map<String, Any>): Boolean {
        val action = info["action"] as? String
        if (action == "store_state") { storeOpenCloseState(); return true }
        if (action == "restore_state") { restoreOpenCloseState(); return true }
        return false
    }

    fun notifyParent(info: Map<String, Any>): Int {
        val action = info["action"] as? String
        if (action != null) {
            if (action == "size_changes") {
                val height = max((info["height"] as? Int ?: 0), 10) +
                    HEADER_HEIGHT + paddingTop + paddingBottom
                expandedHeight = height
                if (isExpanded && !skipChangesOnNotifyParent) {
                    reshape(rectWidth, height)
                }
                System.err.println("AccordionCtrlTab: notifyParent size_changes notify parent not yet implemented")
            }
            if (action == "select_prev") { showAndFocusHeader(); return 1 }
        } else if (info.containsKey("scrollToShowRect")) {
            if (!displayChildren) return 1
        }
        return 0
    }

    fun notify(info: Map<String, Any>): Int {
        val action = info["action"] as? String ?: return 0
        if (action == "select_first") { showAndFocusHeader(); return 1 }
        if (action == "select_last") {
            if (!displayChildren) showAndFocusHeader()
            else accordionView?.notify(mapOf("action" to "select_last"))
        }
        return 0
    }

    fun hasFocus(): Boolean = header.hasFocus()

    fun showAndFocusHeader() {
        header.setFocus(true)
        header.setSelected(selectionEnabled)
        System.err.println("AccordionCtrlTab: showAndFocusHeader notify parent scrollToShowRect not yet implemented")
    }

    fun storeOpenCloseState() {
        if (wasStateStored) return
        storedOpenCloseState = displayChildren
        wasStateStored = true
    }

    fun restoreOpenCloseState() {
        if (!wasStateStored) return
        if (displayChildren != storedOpenCloseState) changeOpenClose(displayChildren)
        wasStateStored = false
    }

    fun setHeaderVisible(value: Boolean) {
        // no-op
    }

    fun setIgnoreResizeNotification(ignore: Boolean) {
        skipChangesOnNotifyParent = ignore
    }

    private fun adjustContainerPanel() {
        val childRect = Rect(
            paddingLeft,
            rectHeight - getHeaderHeight() - paddingTop,
            rectWidth - paddingLeft - paddingRight,
            rectHeight - getHeaderHeight() - paddingTop - paddingBottom
        )
        adjustContainerPanel(childRect)
    }

    private fun adjustContainerPanel(childRect: Rect) {
        val panel = containerPanel ?: return
        if (!fitPanel) {
            showHideScrollbar(childRect)
            updateLayout(childRect)
        } else {
            panel.reshape(childRect.width, childRect.height)
        }
    }

    private fun getChildViewHeight(): Int = containerPanelHeight

    private fun showHideScrollbar(childRect: Rect) {
        if (getChildViewHeight() > childRect.height) showScrollbar(childRect)
        else hideScrollbar(childRect)
    }

    private fun showScrollbar(childRect: Rect) {
        val wasVisible = scrollbarVisible
        scrollbarVisible = true
        if (!wasVisible) updateLayout(childRect)
    }

    private fun hideScrollbar(childRect: Rect) {
        if (!scrollbarVisible) return
        scrollbarVisible = false
        scrollbarDocPos = 0
        updateLayout(childRect)
    }

    private fun onScrollPosChangeCallback(pos: Int) {
        val childRect = Rect(
            paddingLeft,
            rectHeight - getHeaderHeight() - paddingTop,
            rectWidth - paddingLeft - paddingRight,
            rectHeight - getHeaderHeight() - paddingTop - paddingBottom
        )
        updateLayout(childRect)
    }

    private fun updateLayout(childRect: Rect) {
        val panel = containerPanel ?: return
        var panelTop = childRect.height
        var panelWidth = childRect.width
        if (scrollbarVisible) {
            panelTop += scrollbarDocPos
            // no-op
        }
        panel.reshape(panelWidth, containerPanelHeight)
    }

    private fun findContainerView(): ViewStub? {
        // no-op
        return null
    }

    private fun selectOnFocusReceived() {
        System.err.println("AccordionCtrlTab: selectOnFocusReceived notify parent select_current not yet implemented")
    }

    private fun deselectOnFocusLost() {
        System.err.println("AccordionCtrlTab: deselectOnFocusLost notify parent deselect_current not yet implemented")
    }

    data class Rect(val left: Int, val top: Int, val width: Int, val height: Int)
}

interface ViewStub {
    val name: String
    val width: Int
    val height: Int
    var visible: Boolean
    fun reshape(width: Int, height: Int)
    fun notify(info: Map<String, Any>): Int
    val requiredRectHeight: Int
}
