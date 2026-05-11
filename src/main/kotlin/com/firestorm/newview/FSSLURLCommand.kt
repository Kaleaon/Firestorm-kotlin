package com.firestorm.newview

import java.util.UUID

abstract class LLCommandHandler(val name: String, val trustLevel: TrustLevel) {
    enum class TrustLevel { UNTRUSTED_BLOCK, UNTRUSTED_THROTTLE, UNTRUSTED_CLICK_ONLY }
    abstract fun handle(params: List<String>, queryMap: Map<String, String>, grid: String): Boolean
}

private class FSSlurlCommandHandler : LLCommandHandler("firestorm", TrustLevel.UNTRUSTED_BLOCK) {

    override fun handle(params: List<String>, queryMap: Map<String, String>, grid: String): Boolean {
        if (params.size < 2) return false

        val targetId: UUID = try { UUID.fromString(params[0]) } catch (_: IllegalArgumentException) { return false }
        val verb = params[1]

        when (verb) {
            "zoom" -> {
                if (canZoomIn(targetId)) {
                    zoomIn(targetId)
                } else {
                    showNotification("ZoomToAvatarNotPossible")
                }
                return true
            }
            "offerteleport" -> {
                if (targetId != agentId()) offerTeleport(targetId)
                return true
            }
            "requestteleport" -> {
                if (targetId != agentId()) teleportRequest(targetId)
                return true
            }
            "teleportto" -> {
                if (targetId != agentId()) teleportTo(targetId)
                return true
            }
            "track" -> {
                if (targetId != agentId()) track(targetId)
                return true
            }
            "addtocontactset" -> {
                if (targetId != agentId()) addToContactSet(targetId)
                return true
            }
            "blockavatar" -> {
                if (targetId != agentId()) toggleBlock(targetId)
                return true
            }
            "viewlog" -> {
                if (targetId != agentId() && transcriptExists(targetId)) viewChatHistory(targetId)
                return true
            }
            "groupjoin" -> { groupJoin(targetId); return true }
            "groupleave" -> { groupLeave(targetId); return true }
            "groupactivate" -> { groupActivate(targetId); return true }
        }

        val groupId: UUID = getFocusedGroupImSessionId() ?: return true
        if (!isGroupImSession(groupId)) return true

        return when (verb) {
            "groupchatallow" -> { allowTextChat(groupId, targetId, allow = true); true }
            "groupchatforbid" -> { allowTextChat(groupId, targetId, allow = false); true }
            "groupeject" -> { ejectFromGroup(groupId, targetId); true }
            "groupban" -> { banFromGroup(groupId, targetId); true }
            else -> false
        }
    }
}

private class FSHelpSlurlCommandHandler : LLCommandHandler("fshelp", TrustLevel.UNTRUSTED_THROTTLE) {

    override fun handle(params: List<String>, queryMap: Map<String, String>, grid: String): Boolean {
        if (params.size < 2) return false
        if (params[0] == "showdebug") {
            showDebugControl(params[1])
            return true
        }
        return false
    }
}

val gFSSlurlHandler = FSSlurlCommandHandler()
val gFSHelpSlurlCommandHandler = FSHelpSlurlCommandHandler()

private fun agentId(): UUID = TODO("APR: use JVM equivalent — gAgentID")
private fun canZoomIn(id: UUID): Boolean = TODO("APR: use JVM equivalent — LLAvatarActions.canZoomIn")
private fun zoomIn(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.zoomIn")
private fun showNotification(name: String): Unit = TODO("APR: use JVM equivalent — LLNotificationsUtil.add")
private fun offerTeleport(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.offerTeleport")
private fun teleportRequest(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.teleportRequest")
private fun teleportTo(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.teleportTo")
private fun track(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.track")
private fun addToContactSet(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.addToContactSet")
private fun toggleBlock(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.toggleBlock")
private fun transcriptExists(id: UUID): Boolean = TODO("APR: use JVM equivalent — LLLogChat.isTranscriptExist")
private fun viewChatHistory(id: UUID): Unit = TODO("APR: use JVM equivalent — LLAvatarActions.viewChatHistory")
private fun groupJoin(id: UUID): Unit = TODO("APR: use JVM equivalent — LLGroupActions.join")
private fun groupLeave(id: UUID): Unit = TODO("APR: use JVM equivalent — LLGroupActions.leave")
private fun groupActivate(id: UUID): Unit = TODO("APR: use JVM equivalent — LLGroupActions.activate")
private fun getFocusedGroupImSessionId(): UUID? = TODO("APR: use JVM equivalent — gFloaterView focused floater → FSFloaterIM/FSFloaterIMContainer → session UUID")
private fun isGroupImSession(groupId: UUID): Boolean = TODO("APR: use JVM equivalent — LLIMModel session lookup + isGroupSessionType()")
private fun allowTextChat(groupId: UUID, targetId: UUID, allow: Boolean): Unit = TODO("APR: use JVM equivalent — LLIMSpeakerMgr.allowTextChat")
private fun ejectFromGroup(groupId: UUID, targetId: UUID): Unit = TODO("APR: use JVM equivalent — LLGroupActions.ejectFromGroup")
private fun banFromGroup(groupId: UUID, targetId: UUID): Unit = TODO("APR: use JVM equivalent — LLGroupMgr ban+eject+refresh sequence")
private fun showDebugControl(settingName: String): Unit = TODO("APR: use JVM equivalent — LLFloaterSettingsDebug.showControl")
