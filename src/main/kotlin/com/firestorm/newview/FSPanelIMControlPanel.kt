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
        TODO("APR: LLIMModel.instance().findIMSession(sessionId); if P2P return listOf(otherParticipantID, gAgentID) else empty")
    }

    open fun draw() {
        TODO("GPU: delegate to LLPanel.draw()")
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
            TODO("APR: speakerManager = LLIMModel.getInstance().getSpeakerManager(id); return early if null")
            TODO("APR: avatarList = getChild<LLAvatarList>(\"grp_speakers_list\")")
            TODO("APR: participantList = FSParticipantList(speakerManager, avatarList, useContextMenu=true, excludeAgent=false)")
            @Suppress("UNREACHABLE_CODE")
            participantList?.insertMentionCallback = { avatarId -> insertMentionAtCursor(avatarId) }
        }
    }

    override fun getParticipants(): MutableList<UUID> =
        participantList?.getAvatarIds() ?: mutableListOf()

    fun insertMentionAtCursor(avatarId: UUID) {
        TODO("APR: FSFloaterIM.getInstance(getSessionId()).findChild<LLChatEntry>(\"chat_editor\").insertMentionAtCursor(\"secondlife:///app/agent/$avatarId/mention\")")
    }
}


class FSPanelAdHocControlPanel(sessionId: UUID) : FSPanelGroupControlPanel(sessionId)
