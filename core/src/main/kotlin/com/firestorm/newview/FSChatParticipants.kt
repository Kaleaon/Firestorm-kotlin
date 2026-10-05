package com.firestorm.newview

import java.util.UUID

interface FSChatParticipants {
    fun getSessionParticipants(): MutableList<UUID>
}
