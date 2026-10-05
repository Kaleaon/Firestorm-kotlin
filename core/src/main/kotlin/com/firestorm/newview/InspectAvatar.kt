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
        System.err.println("InspectAvatar: toggleSelectedVoice not yet implemented")
    }

    private fun onClickAddFriend() {
        System.err.println("InspectAvatar: onClickAddFriend not yet implemented")
    }

    private fun onClickViewProfile() {
        System.err.println("InspectAvatar: onClickViewProfile not yet implemented")
    }

    private fun isNotFriend(): Boolean {
        System.err.println("InspectAvatar: isNotFriend not yet implemented")
        return false
    }

    private fun onVisibleFindOnMap(): Boolean {
        System.err.println("InspectAvatar: onVisibleFindOnMap not yet implemented")
        return false
    }

    private fun onVisibleEject(): Boolean {
        System.err.println("InspectAvatar: onVisibleEject not yet implemented")
        return false
    }

    private fun onVisibleFreeze(): Boolean {
        System.err.println("InspectAvatar: onVisibleFreeze not yet implemented")
        return false
    }

    private fun onVisibleZoomIn(): Boolean {
        System.err.println("InspectAvatar: onVisibleZoomIn not yet implemented")
        return false
    }

    private fun onClickIM() {
        System.err.println("InspectAvatar: onClickIM not yet implemented")
    }

    private fun onClickCall() {
        System.err.println("InspectAvatar: onClickCall not yet implemented")
    }

    private fun onClickTeleport() {
        System.err.println("InspectAvatar: onClickTeleport not yet implemented")
    }

    private fun onClickTeleportRequest() {
        System.err.println("InspectAvatar: onClickTeleportRequest not yet implemented")
    }

    private fun onClickInviteToGroup() {
        System.err.println("InspectAvatar: onClickInviteToGroup not yet implemented")
    }

    private fun onClickFaceTowards() {
        System.err.println("InspectAvatar: onClickFaceTowards not yet implemented")
    }

    private fun onClickPay() {
        System.err.println("InspectAvatar: onClickPay not yet implemented")
    }

    private fun onClickShare() {
        System.err.println("InspectAvatar: onClickShare not yet implemented")
    }

    private fun onToggleMute() {
        System.err.println("InspectAvatar: onToggleMute not yet implemented")
    }

    fun onClickMuteVolume() {
        System.err.println("InspectAvatar: onClickMuteVolume not yet implemented")
    }

    fun onVolumeChange(volume: Float) {
        System.err.println("InspectAvatar: onVolumeChange not yet implemented")
    }

    private fun onClickReport() {
        System.err.println("InspectAvatar: onClickReport not yet implemented")
    }

    private fun onClickFreeze() {
        System.err.println("InspectAvatar: onClickFreeze not yet implemented")
    }

    private fun onClickEject() {
        System.err.println("InspectAvatar: onClickEject not yet implemented")
    }

    private fun onClickKick() {
        System.err.println("InspectAvatar: onClickKick not yet implemented")
    }

    private fun onClickCSR() {
        System.err.println("InspectAvatar: onClickCSR not yet implemented")
    }

    private fun onClickZoomIn() {
        System.err.println("InspectAvatar: onClickZoomIn not yet implemented")
    }

    private fun onClickFindOnMap() {
        System.err.println("InspectAvatar: onClickFindOnMap not yet implemented")
    }

    private fun enableMute(): Boolean {
        val isLinden = avatarName.displayName.endsWith(" Linden")
        System.err.println("InspectAvatar: enableMute not yet implemented")
        return false
    }

    private fun enableUnmute(): Boolean {
        val isLinden = avatarName.displayName.endsWith(" Linden")
        System.err.println("InspectAvatar: enableUnmute not yet implemented")
        return false
    }

    private fun enableTeleportOffer(): Boolean {
        System.err.println("InspectAvatar: enableTeleportOffer not yet implemented")
        return false
    }

    private fun enableTeleportRequest(): Boolean {
        System.err.println("InspectAvatar: enableTeleportRequest not yet implemented")
        return false
    }

    private fun enablePay(): Boolean {
        System.err.println("InspectAvatar: enablePay not yet implemented")
        return false
    }

    private fun godModeEnabled(): Boolean {
        System.err.println("InspectAvatar: godModeEnabled not yet implemented")
        return false
    }

    fun onAvatarNameCache(agentId: UUID, avName: AvatarName) {
        if (agentId != avatarId) return
        System.err.println("InspectAvatar: onAvatarNameCache not yet implemented")
    }

    companion object {
        fun registerFloater() {
            System.err.println("InspectAvatar: registerFloater not yet implemented")
        }
    }
}

object InspectAvatarUtil {
    fun registerFloater() = InspectAvatar.registerFloater()
}
