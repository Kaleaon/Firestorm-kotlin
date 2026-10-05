package com.firestorm.newview

import java.util.UUID

class FSRadarListCtrl(params: FSScrollListCtrl.Params) : FSScrollListCtrl(params) {

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleRightMouseDown(x, y, mask)

        val contextMenu = mContextMenu
        if (contextMenu != null && !rlvHasBehaviourShowNames()) {
            val selectedItems = getAllSelected()
            if (selectedItems.size > 1) {
                val selectedUuids = selectedItems.map { it.getUUID() }
                contextMenu.show(this, selectedUuids, x, y)
            } else {
                val hitItem = hitItem(x, y)
                if (hitItem != null) {
                    val av = hitItem.getValue()
                    selectByID(av)
                    contextMenu.show(this, listOf(av), x, y)
                }
            }
        }

        return handled
    }

    private fun rlvHasBehaviourShowNames(): Boolean {
        return false
    }
}
