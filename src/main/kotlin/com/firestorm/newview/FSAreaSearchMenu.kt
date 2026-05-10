package com.firestorm.newview

import java.util.UUID

open class LLListContextMenu {
    open fun createMenu(): LLContextMenu? = null
}

class LLContextMenu

class FSAreaSearchMenu : LLListContextMenu() {

    override fun createMenu(): LLContextMenu? {
        TODO("UI: register AreaSearch.Action, AreaSearch.Enable, AreaSearch.RLV callbacks and load menu_fs_area_search.xml")
    }

    private fun onContextMenuItemClick(userdata: Any?) {
        val search = FloaterReg.findTypedInstance<FSAreaSearch>("area_search") ?: return
        search.getPanelList()?.onContextMenuItemClick(userdata)
    }

    private fun onContextMenuItemEnable(userdata: Any?): Boolean {
        val search = FloaterReg.findTypedInstance<FSAreaSearch>("area_search") ?: return false
        return search.getPanelList()?.onContextMenuItemEnable(userdata) ?: false
    }

    private fun onContextMenuItemVisibleRLV(userdata: Any?): Boolean {
        val search = FloaterReg.findTypedInstance<FSAreaSearch>("area_search") ?: return false
        return search.getPanelList()?.onContextMenuItemVisibleRLV(userdata) ?: false
    }

    companion object {
        val instance = FSAreaSearchMenu()
    }
}

val gFSAreaSearchMenu = FSAreaSearchMenu.instance
