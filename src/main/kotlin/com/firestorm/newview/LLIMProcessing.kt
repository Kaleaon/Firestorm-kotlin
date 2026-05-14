package com.firestorm.newview

import java.util.UUID

enum class EInstantMessage {
    IM_NOTHING_SPECIAL,
    IM_MESSAGEBOX,
    IM_GROUP_INVITATION,
    IM_INVENTORY_OFFERED,
    IM_TASK_INVENTORY_OFFERED,
    IM_INVENTORY_ACCEPTED,
    IM_INVENTORY_DECLINED,
    IM_GROUP_VOTE,
    IM_GROUP_MESSAGE_DEPRECATED,
    IM_NEW_USER_DEFAULT,
    IM_SESSION_INVITE,
    IM_SESSION_P2P_INVITE,
    IM_SESSION_GROUP_START,
    IM_SESSION_CONFERENCE_START,
    IM_SESSION_SEND,
    IM_SESSION_LEAVE,
    IM_FROM_TASK,
    IM_FROM_TASK_AS_ALERT,
    IM_DO_NOT_DISTURB_AUTO_RESPONSE,
    IM_CONSOLE_AND_CHAT_HISTORY,
    IM_LURE_USER,
    IM_LURE_ACCEPTED,
    IM_LURE_DECLINED,
    IM_GODLIKE_LURE_USER,
    IM_TELEPORT_REQUEST,
    IM_GROUP_ELECTION_DEPRECATED,
    IM_GOTO_URL,
    IM_GROUP_NOTICE,
    IM_GROUP_NOTICE_REQUESTED,
    IM_GROUP_NOTICE_INVENTORY_ACCEPTED,
    IM_GROUP_NOTICE_INVENTORY_DECLINED,
    IM_GROUP_INVITATION_ACCEPT,
    IM_GROUP_INVITATION_DECLINE,
    IM_FRIENDSHIP_OFFERED,
    IM_FRIENDSHIP_ACCEPTED,
    IM_FRIENDSHIP_DECLINED_DEPRECATED,
    IM_TYPING_START,
    IM_TYPING_STOP,
}

object LLIMProcessing {

    fun getAutoresponseTextForAvatar(
        fromId: UUID,
        isDoNotDisturb: Boolean,
        isAutorespond: Boolean,
        isAutorespondNonFriends: Boolean,
        isAfk: Boolean,
        sendAwayResponse: Boolean,
        isFriend: Boolean
    ): String {
        return ""
    }

    fun processNewMessage(
        fromId: UUID,
        fromGroup: Boolean,
        toId: UUID,
        offline: UByte,
        dialog: EInstantMessage,
        sessionId: UUID,
        timestamp: UInt,
        agentName: String,
        message: String,
        parentEstateId: UInt,
        regionId: UUID,
        position: FloatArray,
        binaryBucket: ByteArray,
        binaryBucketSize: Int,
        senderAddress: String,
        metadata: Map<String, Any?>,
        auxId: UUID = UUID(0L, 0L)
    ) {
        System.err.println("LLIMProcessing: processNewMessage not yet implemented")
    }

    fun requestOfflineMessages() {
        System.err.println("LLIMProcessing: requestOfflineMessages not yet implemented")
    }

    private fun requestOfflineMessagesCoro(url: String) {
        System.err.println("LLIMProcessing: requestOfflineMessagesCoro not yet implemented")
    }

    private fun requestOfflineMessagesLegacy() {
        System.err.println("LLIMProcessing: requestOfflineMessagesLegacy not yet implemented")
    }
}
