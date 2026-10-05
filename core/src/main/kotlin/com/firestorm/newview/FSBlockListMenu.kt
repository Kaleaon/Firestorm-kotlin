package com.firestorm.newview

import com.firestorm.llui.LLView

class FSBlockListMenu : LLListContextMenu() {

    private var spawningCtrl: LLView? = null

    override fun createMenu(): LLContextMenu? {
        System.err.println("FSBlockListMenu: createMenu not yet implemented")
        return null
    }

    fun show(spawningView: LLView, uuids: MutableList<Any>, x: Int, y: Int) {
        spawningCtrl = spawningView
        System.err.println("FSBlockListMenu: show not yet implemented")
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
        System.err.println("FSBlockListMenu: findBlockListPanel not yet implemented")
        return null
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
