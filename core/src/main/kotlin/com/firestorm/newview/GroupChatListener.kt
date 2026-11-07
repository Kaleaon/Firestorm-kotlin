package com.firestorm.newview

import java.util.UUID

class GroupChatListener : EventApi(
    name = "GroupChat",
    description = "API to enter, leave, send and intercept group chat messages"
) {

    init {
        add(
            name = "startGroupChat",
            description = "Enter a group chat in group with UUID [\"group_id\"]\n" +
                "Assumes the logged-in agent is already a member of this group.",
            handler = ::startGroupChat,
            requiredParams = mapOf("group_id" to null)
        )
        add(
            name = "leaveGroupChat",
            description = "Leave a group chat in group with UUID [\"group_id\"]\n" +
                "Assumes a prior successful startIM request.",
            handler = ::leaveGroupChat,
            requiredParams = mapOf("group_id" to null)
        )
        add(
            name = "sendGroupIM",
            description = "send a [\"message\"] to group with UUID [\"group_id\"]",
            handler = ::sendGroupIM,
            requiredParams = mapOf("message" to null, "group_id" to null)
        )
    }

    private fun startGroupChat(data: LLSD) {
        val response = Response(LLSD(), data)
        if (!assertInGroup(response, data)) return

        val sessionId = groupActionsStartIm(data["group_id"].asUUID())
        if (sessionId == null) {
            response.error("Failed to start group chat session \"${data["group_id"].asString()}\"")
        }
    }

    private fun leaveGroupChat(data: LLSD) {
        val response = Response(LLSD(), data)
        if (assertInGroup(response, data)) {
            groupActionsEndIm(data["group_id"].asUUID())
        }
    }

    private fun sendGroupIM(data: LLSD) {
        val response = Response(LLSD(), data)
        if (!assertInGroup(response, data)) return

        val groupId = data["group_id"].asUUID()
        val sessionId = imMgrComputeSessionId(IM_SESSION_GROUP_START, groupId)
        imModelSendMessage(data["message"].asString(), sessionId, groupId, IM_SESSION_SEND)
    }

    private fun assertInGroup(response: Response, data: LLSD): Boolean {
        if (!groupActionsIsInGroup(data["group_id"].asUUID())) {
            response.error("You are not the member of the group: \"${data["group_id"].asString()}\"")
            return false
        }
        return true
    }

    private fun groupActionsIsInGroup(groupId: UUID): Boolean {
        return false
    }

    private fun groupActionsStartIm(groupId: UUID): UUID? {
        return null
    }

    private fun groupActionsEndIm(groupId: UUID): Unit {
        System.err.println("GroupChatListener: groupActionsEndIm not yet implemented")
    }

    private fun imMgrComputeSessionId(type: Int, groupId: UUID): UUID {
        return UUID(0L, 0L)
    }

    private fun imModelSendMessage(message: String, sessionId: UUID, groupId: UUID, imType: Int): Unit {
        System.err.println("GroupChatListener: imModelSendMessage not yet implemented")
    }
}
