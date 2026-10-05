package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

class FSAvatarSearchMenu : LLListContextMenu() {

    var uuids: MutableList<LLUUID> = mutableListOf()

    override fun createMenu(): LLContextMenu? {
        if (uuids.size == 1) {
            System.err.println("FSAvatarSearchMenu: createMenu not yet implemented")
        } else {
            System.err.println("FSAvatarSearchMenu: createMenu not yet implemented")
        }
        return null
    }

    private fun onContextMenuItemEnable(userdata: Any?): Boolean {
        val item = userdata?.toString() ?: return false
        return when (item) {
            "can_block" -> return false
            "can_add" -> {
                // EXT-7389: disabled for multiple selection
                if (uuids.size > 1) return false
                uuids.isNotEmpty() && uuids.none { AvatarActions.isFriend(it) }
            }
            "can_delete" -> uuids.isNotEmpty() && uuids.all { AvatarActions.isFriend(it) }
            "can_call" -> AvatarActions.canCall()
            "can_show_on_map" -> {
                val id = uuids.first()
                (AvatarTracker.isOnline(id) && ViewerMenu.isAgentMappable(id)) || gAgent.isGodlike()
            }
            "can_offer_teleport" -> return false
            "can_request_teleport" -> {
                if (uuids.size == 1) AvatarActions.canRequestTeleport(uuids.first()) else false
            }
            "can_open_inventory" -> !RlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWINV)
            else -> false
        }
    }

    private fun onContextMenuItemCheck(userdata: Any?): Boolean {
        val item = userdata?.toString() ?: return false
        val id = uuids.first()
        return when (item) {
            "is_blocked" -> AvatarActions.isBlocked(id)
            else -> false
        }
    }

    private fun offerTeleport() {
        AvatarActions.offerTeleport(uuids)
    }

    private fun addToContactSet() {
        System.err.println("FSAvatarSearchMenu: addToContactSet not yet implemented")
    }

    companion object {
        val instance = FSAvatarSearchMenu()
    }
}

val gFSAvatarSearchMenu: FSAvatarSearchMenu = FSAvatarSearchMenu.instance
