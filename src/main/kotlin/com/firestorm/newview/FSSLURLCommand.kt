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

private fun agentId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
private fun canZoomIn(id: UUID): Boolean = false
private fun zoomIn(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: zoomIn not yet implemented") }
private fun showNotification(name: String): Unit { System.err.println("FSSlurlCommandHandler: showNotification not yet implemented") }
private fun offerTeleport(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: offerTeleport not yet implemented") }
private fun teleportRequest(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: teleportRequest not yet implemented") }
private fun teleportTo(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: teleportTo not yet implemented") }
private fun track(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: track not yet implemented") }
private fun addToContactSet(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: addToContactSet not yet implemented") }
private fun toggleBlock(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: toggleBlock not yet implemented") }
private fun transcriptExists(id: UUID): Boolean = false
private fun viewChatHistory(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: viewChatHistory not yet implemented") }
private fun groupJoin(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: groupJoin not yet implemented") }
private fun groupLeave(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: groupLeave not yet implemented") }
private fun groupActivate(id: UUID): Unit { System.err.println("FSSlurlCommandHandler: groupActivate not yet implemented") }
private fun getFocusedGroupImSessionId(): UUID? = null
private fun isGroupImSession(groupId: UUID): Boolean = false
private fun allowTextChat(groupId: UUID, targetId: UUID, allow: Boolean): Unit { System.err.println("FSSlurlCommandHandler: allowTextChat not yet implemented") }
private fun ejectFromGroup(groupId: UUID, targetId: UUID): Unit { System.err.println("FSSlurlCommandHandler: ejectFromGroup not yet implemented") }
private fun banFromGroup(groupId: UUID, targetId: UUID): Unit { System.err.println("FSSlurlCommandHandler: banFromGroup not yet implemented") }
private fun showDebugControl(settingName: String): Unit { System.err.println("FSHelpSlurlCommandHandler: showDebugControl not yet implemented") }
