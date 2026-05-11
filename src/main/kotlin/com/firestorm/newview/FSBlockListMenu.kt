package com.firestorm.newview

import com.firestorm.llui.LLView

class FSBlockListMenu : LLListContextMenu() {

    private var spawningCtrl: LLView? = null

    override fun createMenu(): LLContextMenu? {
        TODO(
            "UI: register Block.Action→onContextMenuItemClick, Block.Check→onContextMenuItemCheck, " +
            "Block.Enable→onContextMenuItemEnable, Block.Visible→onContextMenuItemVisible; " +
            "load menu_fs_block_list.xml"
        )
    }

    fun show(spawningView: LLView, uuids: MutableList<Any>, x: Int, y: Int) {
        spawningCtrl = spawningView
        TODO("UI: delegate to LLListContextMenu.show(spawningView, uuids, x, y)")
    }

    private fun onContextMenuItemClick(userdata: Any?) {
        val blocklist = findBlockListPanel() ?: return
        blocklist.onCustomAction(userdata)
    }

    private fun onContextMenuItemCheck(userdata: Any?): Boolean {
        return findBlockListPanel()?.isActionChecked(userdata) ?: false
    }

    private fun onContextMenuItemEnable(userdata: Any?): Boolean {
        return findBlockListPanel()?.isActionEnabled(userdata) ?: false
    }

    private fun onContextMenuItemVisible(userdata: Any?): Boolean {
        return findBlockListPanel()?.isActionVisible(userdata) ?: false
    }

    private fun findBlockListPanel(): FSPanelBlockListContract? {
        TODO("UI: spawningCtrl.getParentByType<FSPanelBlockList>()")
    }

    companion object {
        val instance = FSBlockListMenu()
    }
}

val gFSBlockListMenu: FSBlockListMenu = FSBlockListMenu.instance

interface FSPanelBlockListContract {
    fun onCustomAction(userdata: Any?)
    fun isActionChecked(userdata: Any?): Boolean
    fun isActionEnabled(userdata: Any?): Boolean
    fun isActionVisible(userdata: Any?): Boolean
}
