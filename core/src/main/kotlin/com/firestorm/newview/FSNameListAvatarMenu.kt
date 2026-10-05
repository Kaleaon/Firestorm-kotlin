package com.firestorm.newview

import java.util.UUID

val gFSNameListAvatarMenu = FSNameListAvatarMenu()

class FSNameListAvatarMenu : LLListContextMenu() {

    override fun createMenu(): LLContextMenu {
        return if (uuids.size == 1) {
            val id = uuids.first()
            val registrar = ScopedCommitRegistrar()
            val enableRegistrar = ScopedEnableRegistrar()

            registrar.add("Namelist.ShowProfile")           { LLAvatarActions.showProfile(id) }
            registrar.add("Namelist.SendIM")                { LLAvatarActions.startIM(id) }
            registrar.add("Namelist.Calllog")               { LLAvatarActions.viewChatHistory(id) }
            registrar.add("Namelist.AddFriend")             { LLAvatarActions.requestFriendshipDialog(id) }
            registrar.add("Namelist.AddToContactSet")       { addToContactSet() }
            registrar.add("Namelist.ZoomIn")                { LLAvatarActions.zoomIn(id) }
            registrar.add("Namelist.TeleportToTarget")      { teleportToAvatar() }
            registrar.add("Namelist.OfferTeleport")         { offerTeleport() }
            registrar.add("Namelist.RequestTeleport")       { LLAvatarActions.teleportRequest(id) }
            registrar.add("Namelist.TrackAvatar")           { onTrackAvatarMenuItemClick() }
            registrar.add("Namelist.RemoveFriend")          { LLAvatarActions.removeFriendDialog(id) }
            registrar.add("Namelist.BlockAvatar")           { LLAvatarActions.toggleBlock(id) }
            registrar.add("Namelist.CopyLabel")             { copyNameToClipboard(id) }
            registrar.add("Namelist.CopyUrl")               { copySLURLToClipboard(id) }

            enableRegistrar.add("Namelist.EnableItem")      { userdata -> enableContextMenuItem(userdata) }
            enableRegistrar.add("Namelist.EnableZoomIn")    { LLAvatarActions.canZoomIn(id) }
            enableRegistrar.add("Namelist.CheckIsAgentBlocked") { LLAvatarActions.isBlocked(id) }

            createFromFile("menu_fs_namelist_avatar.xml")
        } else {
            val registrar = ScopedCommitRegistrar()
            val enableRegistrar = ScopedEnableRegistrar()

            registrar.add("Namelist.SendIM")                { LLAvatarActions.startConference(uuids, null) }
            registrar.add("Namelist.AddToContactSet")       { addToContactSet() }
            registrar.add("Namelist.OfferTeleport")         { offerTeleport() }
            registrar.add("Namelist.RemoveFriend")          { LLAvatarActions.removeFriendsDialog(uuids) }

            enableRegistrar.add("Namelist.EnableItem")      { userdata -> enableContextMenuItem(userdata) }

            createFromFile("menu_fs_namelist_avatar_multiselect.xml")
        }
    }

    private fun enableContextMenuItem(userdata: LLSD): Boolean {
        val item = userdata.asString()
        val isSelf = uuids.isNotEmpty() && uuids.first() == agentId

        return when (item) {
            "remove_friend" -> {
                uuids.isNotEmpty() && uuids.all { LLAvatarActions.isFriend(it) }
            }
            "can_add_friend" -> {
                uuids.size == 1 && !isSelf && !LLAvatarActions.isFriend(uuids.first())
            }
            "can_add_set" -> {
                if (uuids.size == 1) !isSelf else true
            }
            "can_send_im" -> {
                when {
                    uuids.size == 1 -> !isSelf && RlvActions.canStartIM(uuids.first())
                    uuids.size > 1  -> uuids.none { it != agentId && !RlvActions.canStartIM(it) }
                    else            -> false
                }
            }
            "teleport_to" -> {
                uuids.size == 1 && !isSelf && FSRadar.getInstance().getEntry(uuids.first()) != null
            }
            "offer_teleport" -> {
                !isSelf && LLAvatarActions.canOfferTeleport(uuids)
            }
            "request_teleport" -> {
                uuids.size == 1 && !isSelf && LLAvatarActions.canRequestTeleport(uuids.first())
            }
            "track_avatar" -> {
                uuids.size == 1 && !isSelf && FSRadar.getInstance().getEntry(uuids.first()) != null
            }
            "can_callog" -> {
                uuids.size == 1 && !isSelf && LLLogChat.isTranscriptExist(uuids.first())
            }
            "can_block" -> {
                uuids.size == 1 && !isSelf
            }
            else -> false
        }
    }

    private fun offerTeleport() {
        val targets = uuids.filter { it != agentId }
        LLAvatarActions.offerTeleport(targets)
    }

    private fun teleportToAvatar() {
        LLAvatarActions.teleportTo(uuids.first())
    }

    private fun onTrackAvatarMenuItemClick() {
        LLAvatarActions.track(uuids.first())
    }

    private fun addToContactSet() {
        val targets = uuids.filter { it != agentId }
        LLAvatarActions.addToContactSet(targets)
    }

    private fun copyNameToClipboard(id: UUID) {
        val avName = LLAvatarNameCache.get(id)
        LLUrlAction.copyURLToClipboard(avName.getAccountName())
    }

    private fun copySLURLToClipboard(id: UUID) {
        LLUrlAction.copyURLToClipboard(LLSLURL("agent", id, "about").getSLURLString())
    }
}
