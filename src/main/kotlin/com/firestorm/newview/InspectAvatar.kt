package com.firestorm.newview

import java.util.UUID

data class AvatarName(
    val displayName: String,
    val userName: String,
    val completeName: String
)

data class AvatarData(
    val hideAge: Boolean,
    val bornOn: Long,
    val aboutText: String,
    val flAboutText: String,
    val accountType: String,
    val paymentInfo: String
)

abstract class Inspect {
    open fun onOpen(data: Map<String, Any?>) {}
    open fun repositionInspector(data: Map<String, Any?>) {}
}

class FetchAvatarData(
    val avatarId: UUID,
    private val inspector: InspectAvatar
) {
    init {
        System.err.println("FetchAvatarData: init not yet implemented")
    }

    fun processProperties(data: Any?, type: String) {
        when (type) {
            "APT_PROPERTIES", "APT_PROPERTIES_LEGACY" -> {
                if (data is AvatarData) inspector.processAvatarData(data)
            }
        }
    }

    fun destroy() {
        System.err.println("FetchAvatarData: destroy not yet implemented")
    }
}

class InspectAvatar(avatarIdData: Map<String, Any?>) : Inspect() {

    private var avatarId: UUID = UUID(0, 0)
    private var avatarName: AvatarName = AvatarName("", "", "")
    private var propertiesRequest: FetchAvatarData? = null

    private val commitCallbacks: MutableMap<String, () -> Unit> = mutableMapOf()
    private val enableCallbacks: MutableMap<String, () -> Boolean> = mutableMapOf()

    init {
        commitCallbacks["InspectAvatar.ViewProfile"] = ::onClickViewProfile
        commitCallbacks["InspectAvatar.AddFriend"] = ::onClickAddFriend
        commitCallbacks["InspectAvatar.IM"] = ::onClickIM
        commitCallbacks["InspectAvatar.Call"] = ::onClickCall
        commitCallbacks["InspectAvatar.Teleport"] = ::onClickTeleport
        commitCallbacks["InspectAvatar.TeleportRequest"] = ::onClickTeleportRequest
        commitCallbacks["InspectAvatar.InviteToGroup"] = ::onClickInviteToGroup
        commitCallbacks["InspectAvatar.FaceTowards"] = ::onClickFaceTowards
        commitCallbacks["InspectAvatar.Pay"] = ::onClickPay
        commitCallbacks["InspectAvatar.Share"] = ::onClickShare
        commitCallbacks["InspectAvatar.ToggleMute"] = ::onToggleMute
        commitCallbacks["InspectAvatar.Freeze"] = ::onClickFreeze
        commitCallbacks["InspectAvatar.Eject"] = ::onClickEject
        commitCallbacks["InspectAvatar.Kick"] = ::onClickKick
        commitCallbacks["InspectAvatar.CSR"] = ::onClickCSR
        commitCallbacks["InspectAvatar.Report"] = ::onClickReport
        commitCallbacks["InspectAvatar.FindOnMap"] = ::onClickFindOnMap
        commitCallbacks["InspectAvatar.ZoomIn"] = ::onClickZoomIn
        commitCallbacks["InspectAvatar.DisableVoice"] = { toggleSelectedVoice(false) }
        commitCallbacks["InspectAvatar.EnableVoice"] = { toggleSelectedVoice(true) }

        enableCallbacks["InspectAvatar.EnableGod"] = ::godModeEnabled
        enableCallbacks["InspectAvatar.VisibleFindOnMap"] = ::onVisibleFindOnMap
        enableCallbacks["InspectAvatar.VisibleEject"] = ::onVisibleEject
        enableCallbacks["InspectAvatar.VisibleFreeze"] = ::onVisibleFreeze
        enableCallbacks["InspectAvatar.VisibleZoomIn"] = ::onVisibleZoomIn
        enableCallbacks["InspectAvatar.Gear.Enable"] = ::isNotFriend
        enableCallbacks["InspectAvatar.Gear.EnableCall"] = { System.err.println("InspectAvatar: EnableCall not yet implemented"); false }
        enableCallbacks["InspectAvatar.Gear.EnableTeleportOffer"] = ::enableTeleportOffer
        enableCallbacks["InspectAvatar.Gear.EnableTeleportRequest"] = ::enableTeleportRequest
        enableCallbacks["InspectAvatar.Gear.EnablePay"] = ::enablePay
        enableCallbacks["InspectAvatar.EnableMute"] = ::enableMute
        enableCallbacks["InspectAvatar.EnableUnmute"] = ::enableUnmute

        System.err.println("InspectAvatar: init registration not yet implemented")
    }

    fun postBuild(): Boolean {
        System.err.println("InspectAvatar: postBuild not yet implemented")
        return false
    }

    override fun onOpen(data: Map<String, Any?>) {
        super.onOpen(data)
        avatarId = data["avatar_id"] as? UUID ?: UUID(0, 0)

        System.err.println("InspectAvatar: onOpen gear_btn logic not yet implemented")

        repositionInspector(data)
        requestUpdate()
        updateVolumeSlider()
        updateModeratorPanel()
    }

    fun onClose(appQuitting: Boolean) {
        System.err.println("InspectAvatar: onClose not yet implemented")
    }

    fun onMouseLeave(x: Int, y: Int, mask: Int) {
        System.err.println("InspectAvatar: onMouseLeave not yet implemented")
    }

    private fun requestUpdate() {
        if (avatarId == UUID(0, 0)) {
            System.err.println("InspectAvatar: requestUpdate null-avatar close not yet implemented")
            return
        }

        System.err.println("InspectAvatar: requestUpdate UI clear not yet implemented")

        propertiesRequest?.destroy()
        propertiesRequest = FetchAvatarData(avatarId, this)

        System.err.println("InspectAvatar: requestUpdate friend/self UI not yet implemented")
    }

    fun processAvatarData(data: AvatarData) {
        System.err.println("InspectAvatar: processAvatarData not yet implemented")
    }

    private fun updateVolumeSlider() {
        System.err.println("InspectAvatar: updateVolumeSlider not yet implemented")
    }

    private fun updateModeratorPanel() {
        System.err.println("InspectAvatar: updateModeratorPanel not yet implemented")
    }

    private fun toggleSelectedVoice(enabled: Boolean) {
        TODO("APR: POST mute-update to ChatSessionRequest capability; close floater")
    }

    private fun onClickAddFriend() {
        TODO("APR: LLAvatarActions.requestFriendshipDialog(avatarId, displayName); close floater")
    }

    private fun onClickViewProfile() {
        TODO("APR: LLAvatarActions.showProfile(avatarId); close floater")
    }

    private fun isNotFriend(): Boolean {
        TODO("APR: return !LLAvatarActions.isFriend(avatarId)")
    }

    private fun onVisibleFindOnMap(): Boolean {
        TODO("APR: return gAgent.isGodlike() || is_agent_mappable(avatarId)")
    }

    private fun onVisibleEject(): Boolean {
        TODO("APR: return enable_freeze_eject(avatarId)")
    }

    private fun onVisibleFreeze(): Boolean {
        TODO("APR: return gAgent.isGodlike() || enable_freeze_eject(avatarId)")
    }

    private fun onVisibleZoomIn(): Boolean {
        TODO("APR: return gObjectList.findObject(avatarId) != null")
    }

    private fun onClickIM() {
        TODO("APR: LLAvatarActions.startIM(avatarId); close floater")
    }

    private fun onClickCall() {
        TODO("APR: LLAvatarActions.startCall(avatarId); close floater")
    }

    private fun onClickTeleport() {
        TODO("APR: LLAvatarActions.offerTeleport(avatarId); close floater")
    }

    private fun onClickTeleportRequest() {
        TODO("APR: LLAvatarActions.teleportRequest(avatarId); close floater")
    }

    private fun onClickInviteToGroup() {
        TODO("APR: LLAvatarActions.inviteToGroup(avatarId); close floater")
    }

    private fun onClickFaceTowards() {
        TODO("APR: find LLVOAvatar for avatarId and call FSAvatarAlignBase.getActive().faceAvatar(); close floater")
    }

    private fun onClickPay() {
        TODO("APR: LLAvatarActions.pay(avatarId); close floater")
    }

    private fun onClickShare() {
        TODO("APR: LLAvatarActions.share(avatarId); close floater")
    }

    private fun onToggleMute() {
        TODO("APR: toggle full mute via LLMuteList for avatarId; show blocked list panel; close floater")
    }

    fun onClickMuteVolume() {
        TODO("APR: toggle voice-chat-only mute via LLMuteList for avatarId; call updateVolumeSlider()")
    }

    fun onVolumeChange(volume: Float) {
        TODO("APR: LLVoiceClient.setUserVolume(avatarId, volume)")
    }

    private fun onClickReport() {
        TODO("APR: LLFloaterReporter.showFromAvatar(avatarId, completeName); close floater")
    }

    private fun onClickFreeze() {
        TODO("APR: if godlike show FreezeAvatar confirmation with freeze/unfreeze options; else handle_avatar_freeze(avatarId); close floater")
    }

    private fun onClickEject() {
        TODO("APR: handle_avatar_eject(avatarId); close floater")
    }

    private fun onClickKick() {
        TODO("APR: LLAvatarActions.kick(avatarId); close floater")
    }

    private fun onClickCSR() {
        TODO("APR: fetch avatar name, then LLAvatarActions.csr(avatarId, userName); close floater")
    }

    private fun onClickZoomIn() {
        TODO("APR: handle_zoom_to_object(avatarId); close floater")
    }

    private fun onClickFindOnMap() {
        TODO("APR: gFloaterWorldMap.trackAvatar(avatarId, displayName); show world_map floater")
    }

    private fun enableMute(): Boolean {
        val isLinden = avatarName.displayName.endsWith(" Linden")
        TODO("APR: return !isLinden && avatarId != agentId && !LLMuteList.isMuted(avatarId, displayName)")
    }

    private fun enableUnmute(): Boolean {
        val isLinden = avatarName.displayName.endsWith(" Linden")
        TODO("APR: return !isLinden && avatarId != agentId && LLMuteList.isMuted(avatarId, displayName)")
    }

    private fun enableTeleportOffer(): Boolean {
        TODO("APR: return LLAvatarActions.canOfferTeleport(avatarId)")
    }

    private fun enableTeleportRequest(): Boolean {
        TODO("APR: return LLAvatarActions.canRequestTeleport(avatarId)")
    }

    private fun enablePay(): Boolean {
        TODO("APR: return RlvActions.canPayAvatar(avatarId)")
    }

    private fun godModeEnabled(): Boolean {
        TODO("APR: return gAgent.isGodlike()")
    }

    fun onAvatarNameCache(agentId: UUID, avName: AvatarName) {
        if (agentId != avatarId) return
        TODO("APR: set user_name, user_name_small, user_slid controls; show small name if text too wide for control rect")
    }

    companion object {
        fun registerFloater() {
            TODO("APR: LLFloaterReg.add(\"inspect_avatar\", \"inspect_avatar.xml\", build<InspectAvatar>)")
        }
    }
}

object InspectAvatarUtil {
    fun registerFloater() = InspectAvatar.registerFloater()
}
