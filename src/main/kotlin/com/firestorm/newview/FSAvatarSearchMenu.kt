package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

class FSAvatarSearchMenu : LLListContextMenu() {

    var uuids: MutableList<LLUUID> = mutableListOf()

    override fun createMenu(): LLContextMenu? {
        if (uuids.size == 1) {
            val id = uuids.first()
            TODO(
                "UI: register single-select callbacks for id=$id — " +
                "Avatar.Profile→AvatarActions.showProfile, Avatar.AddFriend→requestFriendshipDialog, " +
                "Avatar.RemoveFriend→removeFriendDialog, Avatar.IM→startIM, Avatar.Call→startCall, " +
                "Avatar.OfferTeleport→offerTeleport, Avatar.TeleportRequest→AvatarActions.teleportRequest, " +
                "Avatar.GroupInvite→inviteToGroup, Avatar.Share→share, Avatar.Pay→pay, " +
                "Avatar.BlockUnblock→toggleBlock; " +
                "Avatar.EnableItem→onContextMenuItemEnable, Avatar.CheckItem→onContextMenuItemCheck; " +
                "load menu_fs_avatar_search.xml"
            )
        } else {
            TODO(
                "UI: register multi-select callbacks — " +
                "Avatar.IM→AvatarActions.startConference(uuids), Avatar.Call→startAdhocCall(uuids), " +
                "Avatar.OfferTeleport→offerTeleport, Avatar.RemoveFriend→removeFriendsDialog(uuids), " +
                "Avatar.AddToContactSet→addToContactSet; " +
                "Avatar.EnableItem→onContextMenuItemEnable; " +
                "load menu_fs_avatar_search_multiselect.xml"
            )
        }
    }

    private fun onContextMenuItemEnable(userdata: Any?): Boolean {
        val item = userdata?.toString() ?: return false
        return when (item) {
            "can_block" -> TODO("APR: AvatarActions.canBlock(uuids.first())")
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
            "can_offer_teleport" -> TODO("APR: AvatarActions.canOfferTeleport(uuids)")
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
        TODO("APR: AvatarActions.addToContactSet(uuids)")
    }

    companion object {
        val instance = FSAvatarSearchMenu()
    }
}

val gFSAvatarSearchMenu: FSAvatarSearchMenu = FSAvatarSearchMenu.instance
