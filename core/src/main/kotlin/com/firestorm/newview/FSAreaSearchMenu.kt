package com.firestorm.newview

open class LLListContextMenu {
    open fun createMenu(): LLContextMenu? = null
}

class LLContextMenu

class FSAreaSearchMenu : LLListContextMenu() {

    override fun createMenu(): LLContextMenu? {
        return null
    }

    private fun onContextMenuItemClick(userdata: Any?) {
        val search = FloaterReg.findTypedInstance<FSAreaSearch>("area_search") ?: return
        search.panelList?.onContextMenuItemClick(userdata)
    }

    private fun onContextMenuItemEnable(userdata: Any?): Boolean {
        val search = FloaterReg.findTypedInstance<FSAreaSearch>("area_search") ?: return false
        return search.panelList?.onContextMenuItemEnable(userdata) ?: false
    }

    private fun onContextMenuItemVisibleRLV(userdata: Any?): Boolean {
        val search = FloaterReg.findTypedInstance<FSAreaSearch>("area_search") ?: return false
        return search.panelList?.onContextMenuItemVisibleRLV(userdata) ?: false
    }

    companion object {
        val instance = FSAreaSearchMenu()
    }
}

val gFSAreaSearchMenu: FSAreaSearchMenu = FSAreaSearchMenu.instance
