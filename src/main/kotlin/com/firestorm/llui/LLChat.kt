package com.firestorm.llui

import java.util.UUID

enum class ChatSourceType(val value: Int) {
    SYSTEM(0),
    AGENT(1),
    OBJECT(2),
    TELEPORT(3),
    UNKNOWN(4),
    REGION(5)
}

enum class ChatType(val value: Int) {
    WHISPER(0),
    NORMAL(1),
    SHOUT(2),
    OOC(3),
    START(4),
    STOP(5),
    DEBUG_MSG(6),
    REGION(7),
    OWNER(8),
    DIRECT(9),
    IM(10),
    IM_GROUP(11),
    RADAR(12)
}

enum class ChatAudible(val value: Int) {
    NOT(-1),
    BARELY(0),
    FULLY(1)
}

enum class ChatStyle {
    NORMAL,
    IRC,
    HISTORY,
    TELEPORT_SEP,
    MODERATOR,
    SERVER_HISTORY
}

data class Chat(
    var text: String = "",
    var fromName: String = "",
    var fromNameGroup: String = "",
    var fromId: UUID = UUID.randomUUID(),
    var notifId: UUID = UUID.randomUUID(),
    var ownerId: UUID = UUID.randomUUID(),
    var sourceType: ChatSourceType = ChatSourceType.AGENT,
    var chatType: ChatType = ChatType.NORMAL,
    var audible: ChatAudible = ChatAudible.FULLY,
    var muted: Boolean = false,
    var rlvLocFiltered: Boolean = false,
    var rlvNamesFiltered: Boolean = false,
    var time: Double = 0.0,
    var timeStr: String = "",
    var posAgent: FloatArray = FloatArray(3),
    var url: String = "",
    var chatStyle: ChatStyle = ChatStyle.NORMAL,
    var sessionId: UUID = UUID.randomUUID()
)
