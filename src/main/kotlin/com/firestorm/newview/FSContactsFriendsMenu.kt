package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.UrlAction

class FSContactsFriendsMenu : LLListContextMenu() {

    var uuids: MutableList<LLUUID> = mutableListOf()

    override fun createMenu(): LLContextMenu? {
        if (uuids.size == 1) {
            System.err.println("FSContactsFriendsMenu: createMenu not yet implemented")
        } else {
            System.err.println("FSContactsFriendsMenu: createMenu not yet implemented")
        }
        return null
    }

    private fun enableContextMenuItem(userdata: Any?): Boolean {
        val item = userdata?.toString() ?: return false
        return when (item) {
            "remove_friend" -> uuids.isNotEmpty() && uuids.all { AvatarActions.isFriend(it) }
            "teleport_to" -> {
                uuids.size == 1 && FSRadar.getEntry(uuids.first()) != null
            }
            "offer_teleport" -> return false
            "request_teleport" -> {
                uuids.size == 1 && AvatarActions.canRequestTeleport(uuids.first())
            }
            "track_avatar" -> {
                uuids.size == 1 && FSRadar.getEntry(uuids.first()) != null
            }
            "can_callog" -> {
                if (uuids.size == 1) return false
                else false
            }
            "FSFriendListColumnShowUserName" -> {
                gSavedSettings.getBool("FSFriendListColumnShowDisplayName") ||
                    gSavedSettings.getBool("FSFriendListColumnShowFullName")
            }
            "FSFriendListColumnShowDisplayName" -> {
                gSavedSettings.getBool("FSFriendListColumnShowUserName") ||
                    gSavedSettings.getBool("FSFriendListColumnShowFullName")
            }
            "FSFriendListColumnShowFullName" -> {
                gSavedSettings.getBool("FSFriendListColumnShowUserName") ||
                    gSavedSettings.getBool("FSFriendListColumnShowDisplayName")
            }
            "FSFriendListFullNameFormat" -> gSavedSettings.getBool("FSFriendListColumnShowFullName")
            else -> false
        }
    }

    private fun offerTeleport() {
        AvatarActions.offerTeleport(uuids)
    }

    private fun teleportToAvatar() {
        AvatarActions.teleportTo(uuids.first())
    }

    private fun onTrackAvatarMenuItemClick() {
        AvatarActions.track(uuids.first())
    }

    private fun addToContactSet() {
        System.err.println("FSContactsFriendsMenu: addToContactSet not yet implemented")
    }

    private fun copyNameToClipboard(id: LLUUID) {
        val avName = AvatarName()
        AvatarNameCache.get(id, avName)
        UrlAction.copyUrlToClipboard(avName.getAccountName())
    }

    private fun copySLURLToClipboard(id: LLUUID) {
        val slurl = LLSLURL("agent", id.uuid, "about").getSLURLString()
        UrlAction.copyUrlToClipboard(slurl)
    }

    private fun selectOption(userdata: Any?) {
        val option = userdata?.toString() ?: return
        when (option) {
            "sort_by_username"          -> gSavedSettings.setS32("FSFriendListSortOrder", 0)
            "sort_by_displayname"       -> gSavedSettings.setS32("FSFriendListSortOrder", 1)
            "format_username_displayname" -> gSavedSettings.setS32("FSFriendListFullNameFormat", 0)
            "format_displayname_username" -> gSavedSettings.setS32("FSFriendListFullNameFormat", 1)
        }
    }

    private fun checkOption(userdata: Any?): Boolean {
        val option = userdata?.toString() ?: return false
        return when (option) {
            "sort_by_username"            -> gSavedSettings.getS32("FSFriendListSortOrder") == 0
            "sort_by_displayname"         -> gSavedSettings.getS32("FSFriendListSortOrder") == 1
            "format_username_displayname" -> gSavedSettings.getS32("FSFriendListFullNameFormat") == 0
            "format_displayname_username" -> gSavedSettings.getS32("FSFriendListFullNameFormat") == 1
            else -> false
        }
    }

    private fun copyURLToClipboard() {
        UrlAction.copyUrlToClipboard(
            "secondlife:///app/agent/${uuids.first().uuid}/mention"
        )
    }

    companion object {
        val instance = FSContactsFriendsMenu()
    }
}

val gFSContactsFriendsMenu: FSContactsFriendsMenu = FSContactsFriendsMenu.instance
