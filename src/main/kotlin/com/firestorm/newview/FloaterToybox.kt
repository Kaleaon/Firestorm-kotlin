package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Button

class FloaterToybox(key: Any) : Floater(key) {

    var toolBar: ToolBar? = null
        private set

    init {
        registerCommitCallback("Toybox.RestoreDefaults") { _, _ -> onBtnRestoreDefaults() }
        registerCommitCallback("Toybox.ClearAll")        { _, _ -> onBtnClearAll() }
    }

    override fun postBuild(): Boolean {
        toolBar = getChild("toybox_toolbar")

        toolBar?.setStartDragCallback  { a, b, c    -> ToolBarView.startDragTool(a, b, c) }
        toolBar?.setHandleDragCallback { a, b, c, d -> ToolBarView.handleDragTool(a, b, c, d) }
        toolBar?.setHandleDropCallback { a, b, c, d, e -> ToolBarView.handleDropTool(a, b, c, d, e) }
        toolBar?.setButtonEnterCallback { btn -> onToolBarButtonEnter(btn) }

        val cmdMgr = CommandManager.instance
        val alphabetized = (0u until cmdMgr.commandCount())
            .map { cmdMgr.getCommand(it) }
            .filter { it.availableInToybox() }
            .sortedWith { a, b ->
                Trans.getString(a.labelRef()).compareTo(Trans.getString(b.labelRef()))
            }

        for (cmd in alphabetized) {
            toolBar?.addCommand(cmd.id())
        }

        return true
    }

    override fun draw() {
        val tb = toolBar ?: run { super.draw(); return }

        for (id in tb.getCommandsList()) {
            val commandNotPresent = ToolBarView.hasCommand(id) == ToolbarLocation.NONE
            tb.enableCommand(id, commandNotPresent)
        }

        super.draw()
    }

    override fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: EAcceptance?, tooltipMsg: StringBuilder
    ): Boolean {
        val tb = toolBar ?: return false
        val rect = tb.getRect()
        val localX = x - rect.mLeft
        val localY = y - rect.mBottom
        return tb.handleDragAndDrop(localX, localY, mask, drop, cargoType, cargoData, accept, tooltipMsg)
    }

    protected fun onBtnClearAll() {
        NotificationsUtil.add("ConfirmClearAllToybox")
    }

    protected fun onBtnRestoreDefaults() {
        NotificationsUtil.add("ConfirmRestoreToybox")
    }

    protected fun onToolBarButtonEnter(button: View) {
        val commandId = CommandId(button.getName())
        val command = CommandManager.instance.getCommand(commandId)
        val suffix = if (command != null) {
            when (ToolBarView.hasCommand(commandId)) {
                ToolbarLocation.BOTTOM -> Trans.getString("Toolbar_Bottom_Tooltip")
                ToolbarLocation.LEFT   -> Trans.getString("Toolbar_Left_Tooltip")
                ToolbarLocation.RIGHT  -> Trans.getString("Toolbar_Right_Tooltip")
                else -> ""
            }
        } else ""
        toolBar?.setTooltipButtonSuffix(suffix)
    }

    companion object {
        fun finishRestoreToybox(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
            if (NotificationsUtil.getSelectedOption(notification, response) == 0) {
                ToolBarView.loadDefaultToolbars()
            }
            return false
        }

        fun finishClearAllToybox(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
            if (NotificationsUtil.getSelectedOption(notification, response) == 0) {
                ToolBarView.clearAllToolbars()
            }
            return false
        }
    }
}
