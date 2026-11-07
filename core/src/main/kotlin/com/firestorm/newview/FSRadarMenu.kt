package com.firestorm.newview

import java.util.UUID

object FSFloaterRadarMenu {

    val gFSRadarMenu = FSRadarMenu()

    class FSRadarMenu : com.firestorm.newview.LLListContextMenu() {

        fun createMenu(): Any {
            if (mUUIDs.size == 1) {
                val id = mUUIDs.first()
                val registrar = ActionRegistrar()
                val enableRegistrar = EnableRegistrar()

                registrar.add("Avatar.Profile")             { LLAvatarActions.showProfile(id) }
                registrar.add("Avatar.AddFriend")           { LLAvatarActions.requestFriendshipDialog(id) }
                registrar.add("Avatar.RemoveFriend")        { LLAvatarActions.removeFriendDialog(id) }
                registrar.add("Avatar.IM")                  { LLAvatarActions.startIM(id) }
                registrar.add("Avatar.Call")                { LLAvatarActions.startCall(id) }
                registrar.add("Avatar.OfferTeleport")       { offerTeleport() }
                registrar.add("Avatar.TeleportRequest")     { LLAvatarActions.teleportRequest(id) }
                registrar.add("Avatar.GroupInvite")         { LLAvatarActions.inviteToGroup(id) }
                registrar.add("Avatar.getScriptInfo")       { LLAvatarActions.getScriptInfo(id) }
                registrar.add("Avatar.ShowOnMap")           { LLAvatarActions.showOnMap(id) }
                registrar.add("Avatar.Share")               { LLAvatarActions.share(id) }
                registrar.add("Avatar.Pay")                 { LLAvatarActions.pay(id) }
                registrar.add("Avatar.BlockUnblock")        { LLAvatarActions.toggleBlock(id) }
                registrar.add("Avatar.ZoomIn")              { LLAvatarActions.zoomIn(id) }
                registrar.add("Avatar.Report")              { LLAvatarActions.report(id) }
                registrar.add("Avatar.Eject")               { LLAvatarActions.landEject(id) }
                registrar.add("Avatar.Freeze")              { LLAvatarActions.landFreeze(id) }
                registrar.add("Avatar.Kick")                { LLAvatarActions.estateKick(id) }
                registrar.add("Avatar.TeleportHome")        { LLAvatarActions.estateTeleportHome(id) }
                registrar.add("Avatar.EstateBan")           { LLAvatarActions.estateBan(id) }
                registrar.add("Avatar.Derender")            { LLAvatarActions.derender(id, false) }
                registrar.add("Avatar.DerenderPermanent")   { LLAvatarActions.derender(id, true) }
                registrar.add("Avatar.AddToContactSet")     { addToContactSet() }
                registrar.add("Avatar.Calllog")             { LLAvatarActions.viewChatHistory(id) }
                registrar.add("Nearby.People.TeleportToAvatar")   { teleportToAvatar() }
                registrar.add("Nearby.People.TrackAvatar")        { onTrackAvatarMenuItemClick() }
                registrar.add("Nearby.People.FaceTowardsAvatar")  { onFaceTowardsAvatarMenuItemClick() }
                registrar.add("Nearby.People.SetRenderMode")      { userdata -> onSetRenderMode(userdata) }
                registrar.add("Nearby.People.SetAvatarMarkColor") { userdata -> LLNetMap.setAvatarMarkColor(id, userdata) }
                registrar.add("Nearby.People.ClearAvatarMarkColor")     { LLNetMap.clearAvatarMarkColor(id) }
                registrar.add("Nearby.People.ClearAllAvatarMarkColors") { LLNetMap.clearAvatarMarkColors() }

                enableRegistrar.add("Avatar.EnableItem")              { userdata -> enableContextMenuItem(userdata) }
                enableRegistrar.add("Avatar.CheckItem")               { userdata -> checkContextMenuItem(userdata) }
                enableRegistrar.add("Avatar.VisibleZoomIn")           { LLAvatarActions.canZoomIn(id) }
                enableRegistrar.add("Avatar.VisibleFreezeEject")      { LLAvatarActions.canLandFreezeOrEject(id) }
                enableRegistrar.add("Avatar.VisibleKickTeleportHome") { LLAvatarActions.canEstateKickOrTeleportHome(id) }
                enableRegistrar.add("Nearby.People.CheckRenderMode")  { userdata -> checkSetRenderMode(userdata) }
                enableRegistrar.add("Nearby.People.CanFaceTowardsAvatar") { canFaceTowardsAvatar() }

                return createFromFile("menu_fs_radar.xml", registrar, enableRegistrar)
            } else {
                val registrar = ActionRegistrar()
                val enableRegistrar = EnableRegistrar()

                registrar.add("Avatar.IM")               { LLAvatarActions.startConference(mUUIDs, null) }
                registrar.add("Avatar.Call")             { LLAvatarActions.startAdhocCall(mUUIDs, null) }
                registrar.add("Avatar.OfferTeleport")    { offerTeleport() }
                registrar.add("Avatar.RemoveFriend")     { LLAvatarActions.removeFriendsDialog(mUUIDs) }
                registrar.add("Avatar.Eject")            { LLAvatarActions.landEjectMultiple(mUUIDs) }
                registrar.add("Avatar.Freeze")           { LLAvatarActions.landFreezeMultiple(mUUIDs) }
                registrar.add("Avatar.Kick")             { LLAvatarActions.estateKickMultiple(mUUIDs) }
                registrar.add("Avatar.TeleportHome")     { LLAvatarActions.estateTeleportHomeMultiple(mUUIDs) }
                registrar.add("Avatar.EstateBan")        { LLAvatarActions.estateBanMultiple(mUUIDs) }
                registrar.add("Avatar.Derender")         { LLAvatarActions.derenderMultiple(mUUIDs, false) }
                registrar.add("Avatar.DerenderPermanent"){ LLAvatarActions.derenderMultiple(mUUIDs, true) }
                registrar.add("Avatar.AddToContactSet")  { addToContactSet() }
                registrar.add("Nearby.People.SetRenderMode")            { userdata -> onSetRenderMode(userdata) }
                registrar.add("Nearby.People.SetAvatarMarkColor")       { userdata -> LLNetMap.setAvatarMarkColors(mUUIDs, userdata) }
                registrar.add("Nearby.People.ClearAvatarMarkColor")     { LLNetMap.clearAvatarMarkColors(mUUIDs) }
                registrar.add("Nearby.People.ClearAllAvatarMarkColors") { LLNetMap.clearAvatarMarkColors() }

                enableRegistrar.add("Avatar.EnableItem")              { userdata -> enableContextMenuItem(userdata) }
                enableRegistrar.add("Avatar.VisibleFreezeEject")      { LLAvatarActions.canLandFreezeOrEjectMultiple(mUUIDs, false) }
                enableRegistrar.add("Avatar.VisibleKickTeleportHome") { LLAvatarActions.canEstateKickOrTeleportHomeMultiple(mUUIDs, false) }

                return createFromFile("menu_fs_radar_multiselect.xml", registrar, enableRegistrar)
            }
        }

        private fun enableContextMenuItem(userdata: Any?): Boolean {
            val item = userdata?.toString() ?: return false

            return when (item) {
                "can_block" -> LLAvatarActions.canBlock(mUUIDs.first())

                "can_add" -> {
                    if (mUUIDs.size > 1) return false
                    mUUIDs.isNotEmpty() && mUUIDs.none { LLAvatarActions.isFriend(it) }
                }

                "can_delete" -> {
                    mUUIDs.isNotEmpty() && mUUIDs.all { LLAvatarActions.isFriend(it) }
                }

                "can_call"         -> LLAvatarActions.canCall()
                "can_show_on_map"  -> {
                    val id = mUUIDs.first()
                    (LLAvatarTracker.isBuddyOnline(id) && isAgentMappable(id)) || gAgent.isGodlike()
                }
                "can_offer_teleport"   -> LLAvatarActions.canOfferTeleport(mUUIDs)
                "can_request_teleport" -> mUUIDs.size == 1 && LLAvatarActions.canRequestTeleport(mUUIDs.first())
                "can_open_inventory"   -> !rlvHasBehaviourShowInv()
                "can_pay"              -> RlvActions.canPayAvatar(mUUIDs.first())
                "can_callog"           -> LLLogChat.isTranscriptExist(mUUIDs.first())
                else                   -> false
            }
        }

        private fun checkContextMenuItem(userdata: Any?): Boolean {
            val item = userdata?.toString() ?: return false
            val id = mUUIDs.first()
            return when (item) {
                "is_blocked" -> LLAvatarActions.isBlocked(id)
                else         -> false
            }
        }

        private fun offerTeleport() {
            LLAvatarActions.offerTeleport(mUUIDs)
        }

        private fun teleportToAvatar() {
            LLAvatarActions.teleportTo(mUUIDs.first())
        }

        private fun onTrackAvatarMenuItemClick() {
            LLAvatarActions.track(mUUIDs.first())
        }

        private fun onFaceTowardsAvatarMenuItemClick() {
            val avatar = findVOAvatar(mUUIDs.first())
            val floater = FSAvatarAlignBase.getActive()
            if (floater != null && avatar != null) {
                floater.faceAvatar(avatar)
            }
        }

        private fun canFaceTowardsAvatar(): Boolean {
            val avatar = findVOAvatar(mUUIDs.first()) ?: return false
            return !avatar.isDead() &&
                    distVec(avatar.getPositionAgent(), gAgent.getPositionAgent()) <= FSAvatarAlignBase.MAX_FACE_DISTANCE
        }

        private fun addToContactSet() {
            LLAvatarActions.addToContactSet(mUUIDs)
        }

        private fun onSetRenderMode(userdata: Any?) {
            val mode = userdata?.toString()?.toIntOrNull() ?: return
            val renderSetting = when (mode) {
                0 -> VisualMuteSettings.AV_RENDER_NORMALLY
                1 -> VisualMuteSettings.AV_DO_NOT_RENDER
                2 -> VisualMuteSettings.AV_ALWAYS_RENDER
                else -> {
                    System.err.println("FSRadarMenu: use JVM equivalent - log warn unknown visual mute setting value $mode not yet implemented")
                    return
                }
            }

            var needsCulling = false
            for (avatarId in mUUIDs) {
                val avatarp = findVOAvatar(avatarId)
                if (avatarp != null) {
                    avatarp.setVisualMuteSettings(renderSetting)
                    needsCulling = true
                } else {
                    FSAvatarRenderPersistence.setAvatarRenderSettings(avatarId, renderSetting)
                }
            }

            if (needsCulling) {
                cullAvatarsByPixelArea()
            }
        }

        private fun checkSetRenderMode(userdata: Any?): Boolean {
            val mode = userdata?.toString()?.toIntOrNull() ?: return false
            val renderSetting = when (mode) {
                0 -> VisualMuteSettings.AV_RENDER_NORMALLY
                1 -> VisualMuteSettings.AV_DO_NOT_RENDER
                2 -> VisualMuteSettings.AV_ALWAYS_RENDER
                else -> {
                    System.err.println("FSRadarMenu: use JVM equivalent - log warn unknown visual mute setting value $mode not yet implemented")
                    return false
                }
            }
            return FSAvatarRenderPersistence.getAvatarRenderSettings(mUUIDs.first()) == renderSetting
        }

        private fun rlvHasBehaviourShowInv(): Boolean {
            System.err.println("FSRadarMenu: use JVM equivalent - query RLV handler for RLV_BHVR_SHOWINV not yet implemented")
            return false
        }

        private fun findVOAvatar(id: UUID): LLVOAvatar? {
            System.err.println("FSRadarMenu: use JVM equivalent - look up live avatar object by UUID from object list not yet implemented")
            return null
        }

        private fun distVec(a: Vector3, b: Vector3): Float {
            System.err.println("FSRadarMenu: use JVM equivalent - euclidean distance between two 3D positions not yet implemented")
            return 0f
        }

        private fun cullAvatarsByPixelArea(): Unit {
            System.err.println("FSRadarMenu: cull/re-evaluate avatar render queue by pixel area not yet implemented")
        }

        private fun createFromFile(xmlFile: String, registrar: ActionRegistrar, enableRegistrar: EnableRegistrar): Any {
            System.err.println("FSRadarMenu: use JVM equivalent - load XUI context menu from file with registered callbacks not yet implemented")
            return Any()
        }
    }

    class ActionRegistrar {
        fun add(name: String, action: () -> Unit) {}
        fun add(name: String, action: (Any?) -> Unit) {}
    }

    class EnableRegistrar {
        fun add(name: String, check: () -> Boolean) {}
        fun add(name: String, check: (Any?) -> Boolean) {}
    }

    enum class VisualMuteSettings {
        AV_RENDER_NORMALLY,
        AV_DO_NOT_RENDER,
        AV_ALWAYS_RENDER,
    }

    object LLAvatarActions {
        fun showProfile(id: UUID): Unit { System.err.println("LLAvatarActions: showProfile not yet implemented") }
        fun requestFriendshipDialog(id: UUID): Unit { System.err.println("LLAvatarActions: requestFriendshipDialog not yet implemented") }
        fun removeFriendDialog(id: UUID): Unit { System.err.println("LLAvatarActions: removeFriendDialog not yet implemented") }
        fun removeFriendsDialog(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: removeFriendsDialog not yet implemented") }
        fun startIM(id: UUID): Unit { System.err.println("LLAvatarActions: startIM not yet implemented") }
        fun startCall(id: UUID): Unit { System.err.println("LLAvatarActions: startCall not yet implemented") }
        fun teleportRequest(id: UUID): Unit { System.err.println("LLAvatarActions: teleportRequest not yet implemented") }
        fun inviteToGroup(id: UUID): Unit { System.err.println("LLAvatarActions: inviteToGroup not yet implemented") }
        fun getScriptInfo(id: UUID): Unit { System.err.println("LLAvatarActions: getScriptInfo not yet implemented") }
        fun showOnMap(id: UUID): Unit { System.err.println("LLAvatarActions: showOnMap not yet implemented") }
        fun share(id: UUID): Unit { System.err.println("LLAvatarActions: share not yet implemented") }
        fun pay(id: UUID): Unit { System.err.println("LLAvatarActions: pay not yet implemented") }
        fun toggleBlock(id: UUID): Unit { System.err.println("LLAvatarActions: toggleBlock not yet implemented") }
        fun zoomIn(id: UUID): Unit { System.err.println("LLAvatarActions: zoomIn not yet implemented") }
        fun report(id: UUID): Unit { System.err.println("LLAvatarActions: report not yet implemented") }
        fun landEject(id: UUID): Unit { System.err.println("LLAvatarActions: landEject not yet implemented") }
        fun landFreeze(id: UUID): Unit { System.err.println("LLAvatarActions: landFreeze not yet implemented") }
        fun estateKick(id: UUID): Unit { System.err.println("LLAvatarActions: estateKick not yet implemented") }
        fun estateTeleportHome(id: UUID): Unit { System.err.println("LLAvatarActions: estateTeleportHome not yet implemented") }
        fun estateBan(id: UUID): Unit { System.err.println("LLAvatarActions: estateBan not yet implemented") }
        fun derender(id: UUID, permanent: Boolean): Unit { System.err.println("LLAvatarActions: derender not yet implemented") }
        fun viewChatHistory(id: UUID): Unit { System.err.println("LLAvatarActions: viewChatHistory not yet implemented") }
        fun offerTeleport(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: offerTeleport not yet implemented") }
        fun teleportTo(id: UUID): Unit { System.err.println("LLAvatarActions: teleportTo not yet implemented") }
        fun track(id: UUID): Unit { System.err.println("LLAvatarActions: track not yet implemented") }
        fun addToContactSet(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: addToContactSet not yet implemented") }
        fun startConference(ids: List<UUID>, sessionId: UUID?): Unit { System.err.println("LLAvatarActions: startConference not yet implemented") }
        fun startAdhocCall(ids: List<UUID>, sessionId: UUID?): Unit { System.err.println("LLAvatarActions: startAdhocCall not yet implemented") }
        fun landEjectMultiple(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: landEjectMultiple not yet implemented") }
        fun landFreezeMultiple(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: landFreezeMultiple not yet implemented") }
        fun estateKickMultiple(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: estateKickMultiple not yet implemented") }
        fun estateTeleportHomeMultiple(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: estateTeleportHomeMultiple not yet implemented") }
        fun estateBanMultiple(ids: List<UUID>): Unit { System.err.println("LLAvatarActions: estateBanMultiple not yet implemented") }
        fun derenderMultiple(ids: List<UUID>, permanent: Boolean): Unit { System.err.println("LLAvatarActions: derenderMultiple not yet implemented") }
        fun canBlock(id: UUID): Boolean { System.err.println("LLAvatarActions: canBlock not yet implemented"); return false }
        fun isFriend(id: UUID): Boolean { System.err.println("LLAvatarActions: isFriend not yet implemented"); return false }
        fun canCall(): Boolean { System.err.println("LLAvatarActions: canCall not yet implemented"); return false }
        fun canZoomIn(id: UUID): Boolean { System.err.println("LLAvatarActions: canZoomIn not yet implemented"); return false }
        fun canLandFreezeOrEject(id: UUID): Boolean { System.err.println("LLAvatarActions: canLandFreezeOrEject not yet implemented"); return false }
        fun canEstateKickOrTeleportHome(id: UUID): Boolean { System.err.println("LLAvatarActions: canEstateKickOrTeleportHome not yet implemented"); return false }
        fun canLandFreezeOrEjectMultiple(ids: List<UUID>, strict: Boolean): Boolean { System.err.println("LLAvatarActions: canLandFreezeOrEjectMultiple not yet implemented"); return false }
        fun canEstateKickOrTeleportHomeMultiple(ids: List<UUID>, strict: Boolean): Boolean { System.err.println("LLAvatarActions: canEstateKickOrTeleportHomeMultiple not yet implemented"); return false }
        fun canOfferTeleport(ids: List<UUID>): Boolean { System.err.println("LLAvatarActions: canOfferTeleport not yet implemented"); return false }
        fun canRequestTeleport(id: UUID): Boolean { System.err.println("LLAvatarActions: canRequestTeleport not yet implemented"); return false }
        fun isBlocked(id: UUID): Boolean { System.err.println("LLAvatarActions: isBlocked not yet implemented"); return false }
    }

    object LLNetMap {
        fun setAvatarMarkColor(id: UUID, color: Any?): Unit { System.err.println("LLNetMap: setAvatarMarkColor not yet implemented") }
        fun clearAvatarMarkColor(id: UUID): Unit { System.err.println("LLNetMap: clearAvatarMarkColor not yet implemented") }
        fun clearAvatarMarkColors(): Unit { System.err.println("LLNetMap: clearAvatarMarkColors not yet implemented") }
        fun setAvatarMarkColors(ids: List<UUID>, color: Any?): Unit { System.err.println("LLNetMap: setAvatarMarkColors not yet implemented") }
        fun clearAvatarMarkColors(ids: List<UUID>): Unit { System.err.println("LLNetMap: clearAvatarMarkColors(ids) not yet implemented") }
    }

    object LLAvatarTracker {
        fun isBuddyOnline(id: UUID): Boolean { System.err.println("LLAvatarTracker: isBuddyOnline not yet implemented"); return false }
    }

    object RlvActions {
        fun canPayAvatar(id: UUID): Boolean { System.err.println("RlvActions: canPayAvatar not yet implemented"); return false }
    }

    object LLLogChat {
        fun isTranscriptExist(id: UUID): Boolean { System.err.println("LLLogChat: isTranscriptExist not yet implemented"); return false }
    }

    object FSAvatarRenderPersistence {
        fun setAvatarRenderSettings(id: UUID, setting: VisualMuteSettings): Unit { System.err.println("FSAvatarRenderPersistence: setAvatarRenderSettings not yet implemented") }
        fun getAvatarRenderSettings(id: UUID): VisualMuteSettings { System.err.println("FSAvatarRenderPersistence: getAvatarRenderSettings not yet implemented"); return VisualMuteSettings.AV_RENDER_NORMALLY }
    }

    object FSAvatarAlignBase {
        const val MAX_FACE_DISTANCE = 20.0f
        fun getActive(): FSAvatarAlignInstance? { System.err.println("FSAvatarAlignBase: getActive not yet implemented"); return null }
    }

    class FSAvatarAlignInstance {
        fun faceAvatar(avatar: LLVOAvatar): Unit { System.err.println("FSAvatarAlignInstance: faceAvatar not yet implemented") }
    }

    object gAgent {
        fun isGodlike(): Boolean { System.err.println("gAgent: isGodlike not yet implemented"); return false }
        fun getPositionAgent(): Vector3 { System.err.println("gAgent: getPositionAgent not yet implemented"); return Vector3(0f, 0f, 0f) }
    }

    fun isAgentMappable(id: UUID): Boolean { System.err.println("FSFloaterRadarMenu: isAgentMappable not yet implemented"); return false }
}
