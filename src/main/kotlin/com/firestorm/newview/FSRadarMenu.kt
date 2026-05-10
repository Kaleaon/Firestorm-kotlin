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
                    TODO("APR: use JVM equivalent - log warn unknown visual mute setting value $mode")
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
                    TODO("APR: use JVM equivalent - log warn unknown visual mute setting value $mode")
                }
            }
            return FSAvatarRenderPersistence.getAvatarRenderSettings(mUUIDs.first()) == renderSetting
        }

        private fun rlvHasBehaviourShowInv(): Boolean {
            TODO("APR: use JVM equivalent - query RLV handler for RLV_BHVR_SHOWINV")
        }

        private fun findVOAvatar(id: UUID): LLVOAvatar? {
            TODO("APR: use JVM equivalent - look up live avatar object by UUID from object list")
        }

        private fun distVec(a: Vector3, b: Vector3): Float {
            TODO("APR: use JVM equivalent - euclidean distance between two 3D positions")
        }

        private fun cullAvatarsByPixelArea(): Unit {
            TODO("GPU: cull/re-evaluate avatar render queue by pixel area")
        }

        private fun createFromFile(xmlFile: String, registrar: ActionRegistrar, enableRegistrar: EnableRegistrar): Any {
            TODO("APR: use JVM equivalent - load XUI context menu from file with registered callbacks")
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
        fun showProfile(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun requestFriendshipDialog(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun removeFriendDialog(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun removeFriendsDialog(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun startIM(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun startCall(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun teleportRequest(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun inviteToGroup(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun getScriptInfo(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun showOnMap(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun share(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun pay(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun toggleBlock(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun zoomIn(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun report(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun landEject(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun landFreeze(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun estateKick(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun estateTeleportHome(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun estateBan(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun derender(id: UUID, permanent: Boolean): Unit = TODO("APR: use JVM equivalent")
        fun viewChatHistory(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun offerTeleport(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun teleportTo(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun track(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun addToContactSet(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun startConference(ids: List<UUID>, sessionId: UUID?): Unit = TODO("APR: use JVM equivalent")
        fun startAdhocCall(ids: List<UUID>, sessionId: UUID?): Unit = TODO("APR: use JVM equivalent")
        fun landEjectMultiple(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun landFreezeMultiple(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun estateKickMultiple(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun estateTeleportHomeMultiple(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun estateBanMultiple(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
        fun derenderMultiple(ids: List<UUID>, permanent: Boolean): Unit = TODO("APR: use JVM equivalent")
        fun canBlock(id: UUID): Boolean = TODO("APR: use JVM equivalent")
        fun isFriend(id: UUID): Boolean = TODO("APR: use JVM equivalent")
        fun canCall(): Boolean = TODO("APR: use JVM equivalent")
        fun canZoomIn(id: UUID): Boolean = TODO("APR: use JVM equivalent")
        fun canLandFreezeOrEject(id: UUID): Boolean = TODO("APR: use JVM equivalent")
        fun canEstateKickOrTeleportHome(id: UUID): Boolean = TODO("APR: use JVM equivalent")
        fun canLandFreezeOrEjectMultiple(ids: List<UUID>, strict: Boolean): Boolean = TODO("APR: use JVM equivalent")
        fun canEstateKickOrTeleportHomeMultiple(ids: List<UUID>, strict: Boolean): Boolean = TODO("APR: use JVM equivalent")
        fun canOfferTeleport(ids: List<UUID>): Boolean = TODO("APR: use JVM equivalent")
        fun canRequestTeleport(id: UUID): Boolean = TODO("APR: use JVM equivalent")
        fun isBlocked(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    }

    object LLNetMap {
        fun setAvatarMarkColor(id: UUID, color: Any?): Unit = TODO("APR: use JVM equivalent")
        fun clearAvatarMarkColor(id: UUID): Unit = TODO("APR: use JVM equivalent")
        fun clearAvatarMarkColors(): Unit = TODO("APR: use JVM equivalent")
        fun setAvatarMarkColors(ids: List<UUID>, color: Any?): Unit = TODO("APR: use JVM equivalent")
        fun clearAvatarMarkColors(ids: List<UUID>): Unit = TODO("APR: use JVM equivalent")
    }

    object LLAvatarTracker {
        fun isBuddyOnline(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    }

    object RlvActions {
        fun canPayAvatar(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    }

    object LLLogChat {
        fun isTranscriptExist(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    }

    object FSAvatarRenderPersistence {
        fun setAvatarRenderSettings(id: UUID, setting: VisualMuteSettings): Unit = TODO("APR: use JVM equivalent")
        fun getAvatarRenderSettings(id: UUID): VisualMuteSettings = TODO("APR: use JVM equivalent")
    }

    object FSAvatarAlignBase {
        const val MAX_FACE_DISTANCE = 20.0f
        fun getActive(): FSAvatarAlignInstance? = TODO("APR: use JVM equivalent")
    }

    class FSAvatarAlignInstance {
        fun faceAvatar(avatar: LLVOAvatar): Unit = TODO("APR: use JVM equivalent")
    }

    object gAgent {
        fun isGodlike(): Boolean = TODO("APR: use JVM equivalent")
        fun getPositionAgent(): Vector3 = TODO("APR: use JVM equivalent")
    }

    fun isAgentMappable(id: UUID): Boolean = TODO("APR: use JVM equivalent")
}
