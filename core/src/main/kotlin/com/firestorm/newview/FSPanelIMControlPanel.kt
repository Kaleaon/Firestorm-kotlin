package com.firestorm.newview

import java.util.UUID

open class FSPanelChatControlPanel {
    private var sessionId: UUID = UUID(0, 0)

    open fun setSessionId(id: UUID) {
        // Called twice for AdHoc/Group chat — second call arrives after the server init reply.
        sessionId = id
    }

    fun getSessionId(): UUID = sessionId

    open fun getParticipants(): MutableList<UUID> {
        return mutableListOf()
    }

    open fun draw() {
        // no-op
    }
}


class FSPanelIMControlPanel : FSPanelChatControlPanel()


open class FSPanelGroupControlPanel(sessionId: UUID) : FSPanelChatControlPanel() {
    protected var groupID: UUID = UUID(0, 0)
    protected var participantList: FSParticipantList? = null

    init {
        groupID = sessionId
    }

    override fun draw() {
        participantList?.update()
        super.draw()
    }

    override fun setSessionId(id: UUID) {
        super.setSessionId(id)
        groupID = id

        if (participantList == null) {
            System.err.println("FSPanelGroupControlPanel: setSessionId not yet implemented")
            participantList?.insertMentionCallback = { avatarId -> insertMentionAtCursor(avatarId) }
        }
    }

    override fun getParticipants(): MutableList<UUID> =
        participantList?.getAvatarIds() ?: mutableListOf()

    fun insertMentionAtCursor(avatarId: UUID) {
        System.err.println("FSPanelGroupControlPanel: insertMentionAtCursor not yet implemented")
    }
}


class FSPanelAdHocControlPanel(sessionId: UUID) : FSPanelGroupControlPanel(sessionId)
