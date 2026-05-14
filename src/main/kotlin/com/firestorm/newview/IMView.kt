package com.firestorm.newview

import java.security.MessageDigest
import java.util.TreeSet
import java.util.UUID

// ─── SessionTimeoutTimer ──────────────────────────────────────────────────────

class SessionTimeoutTimer(private val sessionId: UUID, periodSecs: Float) {
    private val periodMs: Long = (periodSecs * 1000).toLong()
    private val startTime: Long = System.currentTimeMillis()

    fun tick(): Boolean {
        if (sessionId == UUID(0, 0)) return true
        if (System.currentTimeMillis() - startTime < periodMs) return false
        val session = IMModel.findIMSession(sessionId) ?: return true
        if (!session.sessionInitialized) {
            IMMgr.showSessionStartError("session_initialization_timed_out_error", sessionId)
        }
        return true
    }
}

// ─── IMModel ─────────────────────────────────────────────────────────────────

object IMModel {

    private const val ADHOC_NAME_SUFFIX = " Conference"
    private const val SESSION_INITIALIZATION_TIMEOUT_SECS = 30f

    val id2SessionMap: MutableMap<UUID, IMSession> = mutableMapOf()

    var activeSessionID: UUID = UUID(0, 0)
        private set

    val newMsgListeners: MutableList<(Map<String, Any>) -> Unit> = mutableListOf()
    val noUnreadMsgListeners: MutableList<(Map<String, Any>) -> Unit> = mutableListOf()

    fun setActiveSessionID(sessionId: UUID) {
        if (findIMSession(sessionId) == null) return
        activeSessionID = sessionId
    }

    fun resetActiveSessionID() { activeSessionID = UUID(0, 0) }

    fun findIMSession(sessionId: UUID): IMSession? = id2SessionMap[sessionId]

    fun findAdHocIMSession(ids: List<UUID>): IMSession? {
        if (ids.isEmpty()) return null
        return id2SessionMap.values.firstOrNull { session ->
            session.isAdHoc() &&
                session.initialTargetIDs.size == ids.size &&
                session.initialTargetIDs.toSet() == ids.toSet()
        }
    }

    fun processSessionInitializedReply(oldSessionId: UUID, newSessionId: UUID) {
        val session = findIMSession(oldSessionId) ?: return
        session.sessionInitReplyReceived(newSessionId)
        if (oldSessionId != newSessionId) {
            id2SessionMap.remove(oldSessionId)
            id2SessionMap[newSessionId] = session
        }
        val imFloater = FloaterIMSession.findInstance(oldSessionId)
        imFloater?.sessionInitReplyReceived(newSessionId)
        if (oldSessionId != newSessionId) {
            IMMgr.notifyObserverSessionIDUpdated(oldSessionId, newSessionId)
        }
        if (session.startCallOnInitialize) {
            System.err.println("IMModel: startCall on initialize not yet implemented")
        }
    }

    fun newSession(
        sessionId: UUID,
        name: String,
        type: InstantMessageType,
        otherParticipantId: UUID,
        ids: List<UUID> = emptyList(),
        voiceChannelInfo: Map<String, Any> = emptyMap(),
        hasOfflineMsg: Boolean = false,
    ): Boolean {
        if (name.isEmpty()) return false
        if (findIMSession(sessionId) != null) return false
        val session = IMSession(sessionId, name, type, otherParticipantId, voiceChannelInfo, ids, hasOfflineMsg)
        id2SessionMap[sessionId] = session
        return true
    }

    fun clearSession(sessionId: UUID): Boolean {
        id2SessionMap.remove(sessionId) ?: return false
        return true
    }

    fun addMessage(
        sessionId: UUID,
        from: String,
        otherParticipantId: UUID,
        utf8Text: String,
        log2file: Boolean = true,
        isRegionMsg: Boolean = false,
        timeStamp: UInt = 0u,
        isAnnouncement: Boolean = false,
        keywordAlertPerformed: Boolean = false,
    ) {
        val fromId = otherParticipantId
        processAddingMessage(sessionId, from, fromId, utf8Text, log2file, isRegionMsg, timeStamp, isAnnouncement, keywordAlertPerformed)
    }

    fun processAddingMessage(
        sessionId: UUID,
        from: String,
        fromId: UUID,
        utf8Text: String,
        log2file: Boolean,
        isRegionMsg: Boolean,
        timeStamp: UInt,
        isAnnouncement: Boolean,
        keywordAlertPerformed: Boolean = false,
    ) {
        val session = findIMSession(sessionId) ?: return
        val time = formatTimestamp(timeStamp)
        session.addMessage(from, fromId, utf8Text, time, CHAT_STYLE_NORMAL, isRegionMsg, timeStamp)

        if (log2file && shouldTranslate(utf8Text, sessionId)) {
            System.err.println("IMModel: translate then logToFile not yet implemented")
        } else if (log2file) {
            logToFile(getHistoryFileName(sessionId), from, fromId, utf8Text)
        }

        val msgData = mutableMapOf<String, Any>(
            "session_id" to sessionId,
            "from" to from,
            "from_id" to fromId,
            "message" to utf8Text,
            "time" to time,
            "num_unread" to (session.numUnread + 1),
            "is_announcement" to isAnnouncement,
        )
        session.numUnread++
        newMsgListeners.forEach { it(msgData) }
    }

    fun addMessageSilently(
        sessionId: UUID,
        from: String,
        fromId: UUID,
        utf8Text: String,
        log2file: Boolean = true,
        isRegionMsg: Boolean = false,
        timestamp: UInt = 0u,
        isAnnouncement: Boolean = false,
    ): IMSession? {
        val session = findIMSession(sessionId) ?: return null
        val time = formatTimestamp(timestamp)
        session.addMessage(from, fromId, utf8Text, time, CHAT_STYLE_NORMAL, isRegionMsg, timestamp)
        if (log2file) logToFile(getHistoryFileName(sessionId), from, fromId, utf8Text)
        return session
    }

    fun proccessOnlineOfflineNotification(sessionId: UUID, utf8Text: String) {
        addMessageSilently(sessionId, SYSTEM_FROM, UUID(0, 0), utf8Text, log2file = false)
    }

    fun getMessages(
        sessionId: UUID,
        messages: MutableList<Map<String, Any>>,
        startIndex: Int = 0,
        sendNoUnreadMsgs: Boolean = true,
    ) {
        getMessagesSilently(sessionId, messages, startIndex)
        if (sendNoUnreadMsgs) sendNoUnreadMessages(sessionId)
    }

    fun getMessagesSilently(sessionId: UUID, messages: MutableList<Map<String, Any>>, startIndex: Int = 0) {
        val session = findIMSession(sessionId) ?: return
        val list = session.msgs.drop(startIndex)
        messages.addAll(list)
    }

    fun sendNoUnreadMessages(sessionId: UUID) {
        val session = findIMSession(sessionId) ?: return
        session.participantUnreadMessageCount = 0
        val data = mapOf<String, Any>("session_id" to sessionId, "num_unread" to 0)
        noUnreadMsgListeners.forEach { it(data) }
    }

    fun getName(sessionId: UUID): String = findIMSession(sessionId)?.name ?: ""

    fun getNumUnread(sessionId: UUID): Int = findIMSession(sessionId)?.numUnread ?: -1

    fun getOtherParticipantID(sessionId: UUID): UUID =
        findIMSession(sessionId)?.otherParticipantID ?: UUID(0, 0)

    fun getType(sessionId: UUID): InstantMessageType =
        findIMSession(sessionId)?.type ?: InstantMessageType.IM_COUNT

    fun getVoiceChannel(sessionId: UUID): Any? = findIMSession(sessionId)?.voiceChannel

    fun getSpeakerManager(sessionId: UUID): Any? = findIMSession(sessionId)?.speakers

    fun getHistoryFileName(sessionId: UUID): String =
        findIMSession(sessionId)?.historyFileName ?: ""

    fun logToFile(fileName: String, from: String, fromId: UUID, utf8Text: String): Boolean {
        System.err.println("IMModel: logToFile not yet implemented")
        return false
    }

    fun addNewMsgCallback(callback: (Map<String, Any>) -> Unit) { newMsgListeners += callback }
    fun addNoUnreadMsgsCallback(callback: (Map<String, Any>) -> Unit) { noUnreadMsgListeners += callback }

    fun sendLeaveSession(sessionId: UUID, otherParticipantId: UUID) {
        System.err.println("IMModel: sendLeaveSession not yet implemented")
    }

    fun sendStartSession(
        tempSessionId: UUID,
        otherParticipantId: UUID,
        ids: List<UUID>,
        dialog: InstantMessageType,
        p2pAsAdhocCall: Boolean,
    ): Boolean {
        System.err.println("IMModel: sendStartSession not yet implemented")
        return false
    }

    fun sendTypingState(sessionId: UUID, otherParticipantId: UUID, typing: Boolean) {
        System.err.println("IMModel: sendTypingState not yet implemented")
    }

    fun sendMessage(utf8Text: String, imSessionId: UUID, otherParticipantId: UUID, dialog: InstantMessageType) {
        System.err.println("IMModel: sendMessage not yet implemented")
    }

    fun addSpeakersToRecent(imSessionId: UUID) {
        System.err.println("IMModel: addSpeakersToRecent not yet implemented")
    }

    private fun shouldTranslate(text: String, sessionId: UUID): Boolean {
        System.err.println("IMModel: shouldTranslate not yet implemented")
        return false
    }

    private fun formatTimestamp(timestamp: UInt): String {
        if (timestamp == 0u) {
            System.err.println("IMModel: formatTimestamp (current time) not yet implemented")
            return ""
        }
        System.err.println("IMModel: formatTimestamp not yet implemented")
        return ""
    }

    private fun addToHistory(
        sessionId: UUID,
        from: String,
        fromId: UUID,
        utf8Text: String,
        isRegionMsg: Boolean,
        timestamp: UInt,
        isAnnouncement: Boolean,
    ): Boolean {
        val session = findIMSession(sessionId) ?: return false
        val time = formatTimestamp(timestamp)
        session.addMessage(from, fromId, utf8Text, time, CHAT_STYLE_NORMAL, isRegionMsg, timestamp)
        return true
    }

    const val SYSTEM_FROM = "Second Life"
    const val CHAT_STYLE_NORMAL = 0
    const val CHAT_STYLE_HISTORY = 1
    const val CHAT_STYLE_SERVER_HISTORY = 2

    // ─── IMSession ────────────────────────────────────────────────────────────

    class IMSession(
        var sessionID: UUID,
        var name: String,
        val type: InstantMessageType,
        val otherParticipantID: UUID,
        voiceChannelInfoInit: Map<String, Any>,
        val initialTargetIDs: List<UUID>,
        val hasOfflineMessage: Boolean,
    ) {
        enum class SType { P2P_SESSION, GROUP_SESSION, ADHOC_SESSION, NONE_SESSION }
        enum class SCloseAction { CLOSE_DEFAULT, CLOSE_LEAVE, CLOSE_SNOOZE }

        var sessionType: SType = SType.P2P_SESSION
        var closeAction: SCloseAction = SCloseAction.CLOSE_DEFAULT
        var snoozeTime: Int = -1
        var historyFileName: String = ""
        var lastHistoryCacheDateTime: String = ""
        val lastHistoryCacheMsgs: ArrayDeque<Map<String, Any>> = ArrayDeque()

        var participantUnreadMessageCount: Int = 0
        var numUnread: Int = 0

        val msgs: ArrayDeque<Map<String, Any>> = ArrayDeque()

        var voiceChannel: Any? = null
        var speakers: Any? = null
        var p2pAsAdhocCall: Boolean = false
        var sessionInitialized: Boolean = false
        var callBackEnabled: Boolean = true
        var textIMPossible: Boolean = true
        var startCallOnInitialize: Boolean = false
        var startedAsIMCall: Boolean = false
        var isDNDSend: Boolean = false

        init {
            sessionType = when {
                type == InstantMessageType.IM_NOTHING_SPECIAL ||
                type == InstantMessageType.IM_SESSION_P2P_INVITE -> SType.P2P_SESSION
                else -> if (agentIsInGroup(sessionID)) SType.GROUP_SESSION else SType.ADHOC_SESSION
            }
            initVoiceChannel(voiceChannelInfoInit)

            val needsServerReply = sendStartSession(sessionID, otherParticipantID, initialTargetIDs, type, p2pAsAdhocCall)
            if (!needsServerReply) {
                sessionInitialized = true
            } else {
                SessionTimeoutTimer(sessionID, SESSION_INITIALIZATION_TIMEOUT_SECS)
            }

            callBackEnabled = voiceSessionCallbackPossible(sessionID)
            textIMPossible = voiceSessionTextIMPossible(sessionID)

            buildHistoryFileName()
            loadHistory()

            if (isAdHocSessionType() && type == InstantMessageType.IM_SESSION_INVITE) {
                System.err.println("IMSession: subscribe to avatar name cache for ad-hoc title not yet implemented")
            }
        }

        fun initVoiceChannel(voiceChannelInfo: Map<String, Any>) {
            System.err.println("IMSession: initVoiceChannel not yet implemented")
        }

        fun sessionInitReplyReceived(newSessionId: UUID) {
            sessionInitialized = true
            if (newSessionId != sessionID) {
                sessionID = newSessionId
                System.err.println("IMSession: update voice channel session id not yet implemented")
            }
        }

        fun addMessage(
            from: String,
            fromId: UUID,
            utf8Text: String,
            time: String,
            isHistory: Int,
            isRegionMsg: Boolean,
            timestamp: UInt,
        ) {
            val message = mapOf(
                "from" to from,
                "from_id" to fromId,
                "message" to utf8Text,
                "time" to time,
                "timestamp" to timestamp.toLong(),
                "index" to msgs.size,
                "is_history" to isHistory,
                "is_region_msg" to isRegionMsg,
            )
            msgs.addFirst(message)
            System.err.println("IMSession: speakerChatted/setSpeakerTyping not yet implemented")
        }

        fun addMessagesFromHistoryCache(history: List<Map<String, Any>>) {
            for (msg in history) {
                val from = msg["from"] as? String ?: ""
                val fromId = resolveFromId(msg)
                val dateTime = msg["datetime"] as? String ?: ""
                if (lastHistoryCacheDateTime != dateTime) {
                    lastHistoryCacheDateTime = dateTime
                    lastHistoryCacheMsgs.clear()
                }
                lastHistoryCacheMsgs.addFirst(msg)
                addMessage(from, fromId, msg["message"] as? String ?: "", msg["time"] as? String ?: "",
                    CHAT_STYLE_HISTORY, false, 0u)
            }
        }

        fun addMessagesFromServerHistory(
            history: List<Map<String, Any>>,
            targetFrom: String,
            targetMessage: String,
            timestamp: UInt,
        ) {
            if (history.isEmpty()) return
            if (history.size == 1 && targetFrom.isNotEmpty() && msgs.isNotEmpty()) return

            var matchTimestamp = 0L
            val shiftMsgs: ArrayDeque<Map<String, Any>> = ArrayDeque()
            if (msgs.isNotEmpty() && targetFrom.isNotEmpty() && targetMessage.isNotEmpty()) {
                while (msgs.isNotEmpty()) {
                    val cur = msgs.first()
                    matchTimestamp = (cur["timestamp"] as? Long) ?: 0L
                    if (timestamp.toLong() > matchTimestamp) break
                    shiftMsgs.addFirst(cur)
                    msgs.removeFirst()
                }
            }

            for (serverMsg in history) {
                val histTs = (serverMsg["time"] as? Long)?.toUInt() ?: 0u
                if ((matchTimestamp > 0 && matchTimestamp <= histTs.toLong()) ||
                    (timestamp > 0u && timestamp <= histTs)) break

                val histDateTime = serverMsg["datetime"] as? String ?: ""
                if (lastHistoryCacheDateTime.isNotEmpty() && histDateTime < lastHistoryCacheDateTime) continue

                val sender = serverMsg["from"] as? String ?: ""
                val senderId = serverMsg["from_id"] as? UUID ?: UUID(0, 0)
                val msgText = serverMsg["message"] as? String ?: ""
                System.err.println("IMSession: Conversation.createTimestamp not yet implemented")
                val chatTimeStr = ""
                addMessage(sender, senderId, msgText, chatTimeStr, CHAT_STYLE_SERVER_HISTORY, false, histTs)
            }

            while (shiftMsgs.isNotEmpty()) {
                val newer = shiftMsgs.removeFirst().toMutableMap()
                newer["index"] = msgs.size
                msgs.addFirst(newer)
            }

            lastHistoryCacheDateTime = ""
            lastHistoryCacheMsgs.clear()
        }

        fun loadHistory() {
            msgs.clear()
            lastHistoryCacheMsgs.clear()
            lastHistoryCacheDateTime = ""
            System.err.println("IMSession: gSavedPerAccountSettings.getBOOL(LogShowHistory) not yet implemented")
            val logShowHistory = false
            if (!logShowHistory) return
            System.err.println("IMSession: LLLogChat.loadChatHistory not yet implemented")
            val chatHistory = emptyList<Map<String, Any>>()
            addMessagesFromHistoryCache(chatHistory)
        }

        fun buildHistoryFileName() {
            historyFileName = when {
                isAdHoc() -> {
                    if (initialTargetIDs.isNotEmpty()) {
                        val sortedUuids = TreeSet(initialTargetIDs)
                        "$name hash${generateHash(sortedUuids)}"
                    } else {
                        System.err.println("IMSession: LLLogChat.timestamp2LogString not yet implemented")
                    val ts = ""
                        val shortId = sessionID.toString().take(4)
                        "$name $ts $shortId"
                    }
                }
                isP2P() -> {
                    System.err.println("IMSession: LLAvatarNameCache.get not yet implemented")
                    val avName = null as Pair<String, String>?
                    val userName = avName?.first ?: name
                    buildUsername(userName)
                }
                isGroupChat() -> "$name${GROUP_CHAT_SUFFIX}"
                else -> name
            }
        }

        fun isOutgoingAdHoc(): Boolean = type == InstantMessageType.IM_SESSION_CONFERENCE_START

        fun isAdHoc(): Boolean =
            type == InstantMessageType.IM_SESSION_CONFERENCE_START ||
                (type == InstantMessageType.IM_SESSION_INVITE && !agentIsInGroup(sessionID))

        fun isP2P(): Boolean = type == InstantMessageType.IM_NOTHING_SPECIAL

        fun isGroupChat(): Boolean =
            type == InstantMessageType.IM_SESSION_GROUP_START ||
                (type == InstantMessageType.IM_SESSION_INVITE && agentIsInGroup(sessionID))

        fun isP2PSessionType(): Boolean = sessionType == SType.P2P_SESSION
        fun isAdHocSessionType(): Boolean = sessionType == SType.ADHOC_SESSION
        fun isGroupSessionType(): Boolean = sessionType == SType.GROUP_SESSION

        fun generateOutgoingAdHocHash(): UUID {
            if (initialTargetIDs.isEmpty()) return UUID(0, 0)
            return generateHash(TreeSet(initialTargetIDs))
        }

        fun onVoiceChannelStateChanged(oldState: VoiceChannelState, newState: VoiceChannelState, direction: VoiceChannelDirection) {
            val youJoinedCall = translate("you_joined_call")
            val youStartedCall = translate("you_started_call")
            when (sessionType) {
                SType.P2P_SESSION -> {
                    System.err.println("IMSession: get cached avatar username not yet implemented")
                    val otherName = ""
                    if (direction == VoiceChannelDirection.INCOMING_CALL) {
                        when (newState) {
                            VoiceChannelState.STATE_CALL_STARTED ->
                                addMessage(SYSTEM_FROM, UUID(0, 0), translate("name_started_call", "NAME" to otherName), "", 0, false, 0u)
                            VoiceChannelState.STATE_CONNECTED ->
                                addMessage(SYSTEM_FROM, UUID(0, 0), youJoinedCall, "", 0, false, 0u)
                            else -> {}
                        }
                    } else {
                        when (newState) {
                            VoiceChannelState.STATE_CALL_STARTED ->
                                addMessage(SYSTEM_FROM, UUID(0, 0), youStartedCall, "", 0, false, 0u)
                            VoiceChannelState.STATE_CONNECTED ->
                                addMessage(SYSTEM_FROM, UUID(0, 0), translate("answered_call"), "", 0, false, 0u)
                            else -> {}
                        }
                    }
                }
                SType.GROUP_SESSION, SType.ADHOC_SESSION -> {
                    if (direction == VoiceChannelDirection.INCOMING_CALL && newState == VoiceChannelState.STATE_CONNECTED) {
                        addMessage(SYSTEM_FROM, UUID(0, 0), youJoinedCall, "", 0, false, 0u)
                    } else if (direction == VoiceChannelDirection.OUTGOING_CALL && newState == VoiceChannelState.STATE_CALL_STARTED) {
                        addMessage(SYSTEM_FROM, UUID(0, 0), youStartedCall, "", 0, false, 0u)
                    }
                }
                else -> {}
            }
            if (newState == VoiceChannelState.STATE_CONNECTED) {
                System.err.println("IMSession: speakers.update not yet implemented")
            }
        }

        private fun onAdHocNameCache(displayName: String, isValid: Boolean) {
            if (!isValid) {
                val parts = name.split(" ")
                if (parts.lastOrNull() == "Conference") {
                    name = translate("conference-title-incoming", "AGENT_NAME" to parts.dropLast(1).joinToString(" "))
                }
            } else {
                name = translate("conference-title-incoming", "AGENT_NAME" to displayName)
            }
        }

        private fun resolveFromId(msg: Map<String, Any>): UUID {
            val id = msg["from_id"]
            if (id is UUID) return id
            System.err.println("IMSession: LLAvatarNameCache.findIdByName not yet implemented")
            return UUID(0, 0)
        }

        private fun buildUsername(name: String): String {
            System.err.println("IMSession: buildUsername not yet implemented")
            return ""
        }

        private fun translate(key: String, vararg args: Pair<String, String>): String {
            System.err.println("IMSession: translate not yet implemented")
            return ""
        }

        private fun agentIsInGroup(groupId: UUID): Boolean {
            System.err.println("IMSession: agentIsInGroup not yet implemented")
            return false
        }

        private fun voiceSessionCallbackPossible(sessionId: UUID): Boolean {
            System.err.println("IMSession: voiceSessionCallbackPossible not yet implemented")
            return false
        }

        private fun voiceSessionTextIMPossible(sessionId: UUID): Boolean {
            System.err.println("IMSession: voiceSessionTextIMPossible not yet implemented")
            return false
        }

        companion object {
            const val SYSTEM_FROM = "Second Life"
            const val GROUP_CHAT_SUFFIX = " (group)"
            const val SESSION_INITIALIZATION_TIMEOUT_SECS = 30f

            fun chatFromLogFile(type: Int, msg: Map<String, Any>, session: IMSession) {
                when (type) {
                    LOG_LINE -> session.addMessage("", UUID(0, 0), msg["message"] as? String ?: "", "", CHAT_STYLE_HISTORY, false, 0u)
                    LOG_LLSD -> session.addMessage(
                        msg["from"] as? String ?: "",
                        msg["from_id"] as? UUID ?: UUID(0, 0),
                        msg["message"] as? String ?: "",
                        msg["time"] as? String ?: "",
                        CHAT_STYLE_HISTORY, false, 0u,
                    )
                }
            }

            fun generateHash(sortedUuids: TreeSet<UUID>): UUID {
                val md = MessageDigest.getInstance("MD5")
                for (uuid in sortedUuids) {
                    val bytes = ByteArray(16)
                    val msb = uuid.mostSignificantBits
                    val lsb = uuid.leastSignificantBits
                    for (i in 7 downTo 0) { bytes[7 - i] = ((msb shr (i * 8)) and 0xFF).toByte() }
                    for (i in 7 downTo 0) { bytes[15 - i] = ((lsb shr (i * 8)) and 0xFF).toByte() }
                    md.update(bytes)
                }
                val digest = md.digest()
                var msb = 0L; var lsb = 0L
                for (i in 0..7) msb = (msb shl 8) or (digest[i].toLong() and 0xFF)
                for (i in 8..15) lsb = (lsb shl 8) or (digest[i].toLong() and 0xFF)
                return UUID(msb, lsb)
            }

            private const val LOG_LINE = 0
            private const val LOG_LLSD = 1
            private const val CHAT_STYLE_HISTORY = 1
            private const val CHAT_STYLE_SERVER_HISTORY = 2
        }
    }
}

// ─── IMSessionObserver ────────────────────────────────────────────────────────

interface IMSessionObserver {
    fun sessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean)
    fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID)
    fun sessionVoiceOrIMStarted(sessionId: UUID)
    fun sessionRemoved(sessionId: UUID)
    fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID)
}

// ─── IMMgr ────────────────────────────────────────────────────────────────────

object IMMgr {
    enum class InvitationType { INSTANT_MESSAGE, VOICE, IMMEDIATE }

    private val sessionObservers: MutableList<IMSessionObserver> = mutableListOf()
    private val notifiedNonFriendSessions: MutableSet<UUID> = mutableSetOf()
    private val pendingInvitations: MutableMap<UUID, Any> = mutableMapOf()
    private val pendingAgentListUpdates: MutableMap<UUID, Any> = mutableMapOf()
    private val snoozedSessions: MutableMap<UUID, Double> = mutableMapOf()

    fun addMessage(
        sessionId: UUID,
        targetId: UUID,
        from: String,
        msg: String,
        isOfflineMsg: Boolean = false,
        sessionName: String = "",
        dialog: InstantMessageType = InstantMessageType.IM_NOTHING_SPECIAL,
        parentEstateId: UInt = 0u,
        regionId: UUID = UUID(0, 0),
        isRegionMsg: Boolean = false,
        timestamp: UInt = 0u,
        displayId: UUID = UUID(0, 0),
        displayName: String = "",
        isAnnouncement: Boolean = false,
        keywordAlertPerformed: Boolean = false,
    ) {
        val session = IMModel.findIMSession(sessionId)
        if (session == null) {
            if (sessionName.isNotEmpty()) {
                IMModel.newSession(sessionId, sessionName, dialog, targetId)
                notifyObserverSessionAdded(sessionId, sessionName, targetId, isOfflineMsg)
            }
        }
        IMModel.addMessage(sessionId, from, targetId, msg, true, isRegionMsg, timestamp, isAnnouncement, keywordAlertPerformed)
    }

    fun addSystemMessage(sessionId: UUID, messageName: String, args: Map<String, Any>) {
        System.err.println("IMMgr: addSystemMessage not yet implemented")
    }

    fun addSession(
        name: String,
        dialog: InstantMessageType,
        otherParticipantId: UUID,
        voiceChannelInfo: Map<String, Any> = emptyMap(),
    ): UUID {
        val sessionId = computeSessionID(dialog, otherParticipantId)
        if (IMModel.findIMSession(sessionId) == null) {
            IMModel.newSession(sessionId, name, dialog, otherParticipantId, voiceChannelInfo = voiceChannelInfo)
            notifyObserverSessionAdded(sessionId, name, otherParticipantId, false)
        } else {
            notifyObserverSessionActivated(sessionId, name, otherParticipantId)
        }
        return sessionId
    }

    fun addSession(
        name: String,
        dialog: InstantMessageType,
        otherParticipantId: UUID,
        ids: List<UUID>,
        voiceChannelInfo: Map<String, Any> = emptyMap(),
        floaterId: UUID = UUID(0, 0),
    ): UUID {
        val sessionId = computeSessionID(dialog, otherParticipantId)
        if (IMModel.findIMSession(sessionId) == null) {
            IMModel.newSession(sessionId, name, dialog, otherParticipantId, ids, voiceChannelInfo)
            notifyObserverSessionAdded(sessionId, name, otherParticipantId, false)
        }
        return sessionId
    }

    fun addP2PSession(name: String, otherParticipantId: UUID, voiceCallInfo: Map<String, Any>): UUID {
        return addSession(name, InstantMessageType.IM_NOTHING_SPECIAL, otherParticipantId, voiceChannelInfo = voiceCallInfo)
    }

    fun leaveSession(sessionId: UUID): Boolean {
        val session = IMModel.findIMSession(sessionId) ?: return false
        IMModel.sendLeaveSession(sessionId, session.otherParticipantID)
        removeSession(sessionId)
        return true
    }

    fun inviteToSession(
        sessionId: UUID,
        sessionName: String,
        caller: UUID,
        callerName: String,
        type: InstantMessageType,
        invType: InvitationType,
        voiceChannelInfo: Map<String, Any> = emptyMap(),
    ) {
        System.err.println("IMMgr: inviteToSession not yet implemented")
    }

    fun processIMTypingStart(fromId: UUID, imType: InstantMessageType) = processIMTypingCore(fromId, imType, true)
    fun processIMTypingStop(fromId: UUID, imType: InstantMessageType) = processIMTypingCore(fromId, imType, false)

    private fun processIMTypingCore(fromId: UUID, imType: InstantMessageType, typing: Boolean) {
        val sessionId = computeSessionID(imType, fromId)
        val floater = FloaterIMSession.findInstance(sessionId) ?: return
        floater.processIMTyping(fromId, typing)
    }

    fun autoStartCallOnStartup(sessionId: UUID) {
        val session = IMModel.findIMSession(sessionId) ?: return
        session.startCallOnInitialize = true
    }

    fun getNumberOfUnreadIM(): Int = IMModel.id2SessionMap.values.sumOf { it.numUnread }

    fun getNumberOfUnreadParticipantMessages(): Int =
        IMModel.id2SessionMap.values.sumOf { it.participantUnreadMessageCount }

    fun disconnectAllSessions() {
        IMModel.id2SessionMap.keys.toList().forEach { removeSession(it) }
    }

    fun hasSession(sessionId: UUID): Boolean = IMModel.findIMSession(sessionId) != null

    fun checkSnoozeExpiration(sessionId: UUID): Boolean {
        val wakeTime = snoozedSessions[sessionId] ?: return false
        return elapsedSeconds() >= wakeTime
    }

    fun isSnoozedSession(sessionId: UUID): Boolean = sessionId in snoozedSessions

    fun restoreSnoozedSession(sessionId: UUID): Boolean {
        snoozedSessions.remove(sessionId) ?: return false
        System.err.println("IMMgr: restoreSnoozedSession not yet implemented")
        return true
    }

    fun computeSessionID(dialog: InstantMessageType, otherParticipantId: UUID): UUID {
        System.err.println("IMMgr: computeSessionID not yet implemented")
        return UUID(0, 0)
    }

    fun clearPendingInvitation(sessionId: UUID) { pendingInvitations.remove(sessionId) }

    fun processAgentListUpdates(sessionId: UUID, body: Map<String, Any>) {
        val session = IMModel.findIMSession(sessionId)
        val pending = pendingAgentListUpdates[sessionId]
        if (session != null) {
            System.err.println("IMMgr: processAgentListUpdates not yet implemented")
        } else {
            addPendingAgentListUpdates(sessionId, body)
        }
    }

    fun getPendingAgentListUpdates(sessionId: UUID): Map<String, Any>? =
        pendingAgentListUpdates[sessionId] as? Map<String, Any>

    fun addPendingAgentListUpdates(sessionId: UUID, updates: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val existing = pendingAgentListUpdates.getOrPut(sessionId) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        existing.putAll(updates)
    }

    fun clearPendingAgentListUpdates(sessionId: UUID) { pendingAgentListUpdates.remove(sessionId) }

    fun addSessionObserver(observer: IMSessionObserver) { sessionObservers += observer }
    fun removeSessionObserver(observer: IMSessionObserver) { sessionObservers -= observer }

    fun showSessionStartError(errorString: String, sessionId: UUID) {
        System.err.println("IMMgr: showSessionStartError not yet implemented")
    }

    fun showSessionEventError(eventString: String, errorString: String, sessionId: UUID) {
        System.err.println("IMMgr: showSessionEventError not yet implemented")
    }

    fun showSessionForceClose(reason: String, sessionId: UUID) {
        System.err.println("IMMgr: showSessionForceClose not yet implemented")
    }

    fun startCall(sessionId: UUID, direction: VoiceChannelDirection = VoiceChannelDirection.OUTGOING_CALL, voiceChannelInfo: Map<String, Any> = emptyMap()): Boolean {
        val channel = IMModel.getVoiceChannel(sessionId) ?: return false
        System.err.println("IMMgr: startCall not yet implemented")
        return false
    }

    fun endCall(sessionId: UUID): Boolean {
        val channel = IMModel.getVoiceChannel(sessionId) ?: return false
        System.err.println("IMMgr: endCall not yet implemented")
        return false
    }

    fun isVoiceCall(sessionId: UUID): Boolean {
        val session = IMModel.findIMSession(sessionId) ?: return false
        return session.startedAsIMCall
    }

    fun updateDNDMessageStatus() {
        for (session in IMModel.id2SessionMap.values) {
            if (!session.isDNDSend) {
                System.err.println("IMMgr: updateDNDMessageStatus not yet implemented")
            }
        }
    }

    fun isDNDMessageSend(sessionId: UUID): Boolean = IMModel.findIMSession(sessionId)?.isDNDSend ?: false

    fun setDNDMessageSent(sessionId: UUID, isSend: Boolean) {
        IMModel.findIMSession(sessionId)?.isDNDSend = isSend
    }

    fun addNotifiedNonFriendSessionID(sessionId: UUID) { notifiedNonFriendSessions += sessionId }

    fun isNonFriendSessionNotified(sessionId: UUID): Boolean = sessionId in notifiedNonFriendSessions

    private fun removeSession(sessionId: UUID) {
        val session = IMModel.findIMSession(sessionId) ?: return
        IMModel.clearSession(sessionId)
        notifyObserverSessionRemoved(sessionId)
    }

    private fun noteOfflineUsers(sessionId: UUID, ids: List<UUID>) {
        TODO("APR: use JVM equivalent - for each id in ids, if offline, add system message to session $sessionId")
    }

    private fun noteMutedUsers(sessionId: UUID, ids: List<UUID>) {
        TODO("APR: use JVM equivalent - for each muted id in ids, add system message to session $sessionId")
    }

    internal fun notifyObserverSessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean) {
        sessionObservers.forEach { it.sessionAdded(sessionId, name, otherParticipantId, hasOfflineMsg) }
    }

    internal fun notifyObserverSessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) {
        sessionObservers.forEach { it.sessionActivated(sessionId, name, otherParticipantId) }
    }

    internal fun notifyObserverSessionVoiceOrIMStarted(sessionId: UUID) {
        sessionObservers.forEach { it.sessionVoiceOrIMStarted(sessionId) }
    }

    internal fun notifyObserverSessionRemoved(sessionId: UUID) {
        sessionObservers.forEach { it.sessionRemoved(sessionId) }
    }

    internal fun notifyObserverSessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {
        sessionObservers.forEach { it.sessionIDUpdated(oldSessionId, newSessionId) }
    }
}

// ─── CallDialogManager ────────────────────────────────────────────────────────

object CallDialogManager {
    private var previousSessionName: String = ""
    private var currentSessionName: String = ""
    private var currentSession: IMModel.IMSession? = null
    private var oldState: VoiceChannelState? = null

    fun onVoiceChannelChanged(sessionId: UUID) {
        val session = IMModel.findIMSession(sessionId)
        previousSessionName = currentSessionName
        currentSessionName = session?.name ?: ""
        currentSession = session
        TODO("APR: use JVM equivalent - update call dialog UI for voice channel change to $sessionId")
    }

    fun onVoiceChannelStateChanged(
        previousState: VoiceChannelState,
        newState: VoiceChannelState,
        direction: VoiceChannelDirection,
        endedByAgent: Boolean,
    ) {
        TODO("APR: use JVM equivalent - show/hide incoming-call or outgoing-call dialog based on state transition")
    }
}

// ─── CallDialog ───────────────────────────────────────────────────────────────

abstract class CallDialog(protected val payload: Map<String, Any>) {
    protected var lifetimeTimer: Long = System.currentTimeMillis()
    protected var lifetimeSecs: Int = DEFAULT_LIFETIME

    open fun postBuild(): Boolean = true

    open fun onOpen(key: Any) {
        lifetimeTimer = System.currentTimeMillis()
    }

    open fun draw() {
        if (lifetimeHasExpired()) onLifetimeExpired()
        TODO("GPU: draw call dialog UI elements")
    }

    fun dockToToolbarButton(toolbarButtonName: String) {
        TODO("APR: use JVM equivalent - dock this floater to toolbar button named $toolbarButtonName")
    }

    protected open fun lifetimeHasExpired(): Boolean =
        (System.currentTimeMillis() - lifetimeTimer) / 1000 >= lifetimeSecs

    protected open fun onLifetimeExpired() {
        TODO("APR: use JVM equivalent - close this floater after lifetime expired")
    }

    protected fun setIcon(sessionId: Any, participantId: Any) {
        TODO("APR: use JVM equivalent - show group icon if sessionId is a group, else avatar icon for participantId")
    }

    companion object {
        const val DEFAULT_LIFETIME = 5
    }
}

// ─── IncomingCallDialog ───────────────────────────────────────────────────────

class IncomingCallDialog(payload: Map<String, Any>) : CallDialog(payload) {

    override fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent - wire accept/reject/start-IM buttons; set caller name and icon")
        return true
    }

    override fun onOpen(key: Any) {
        super.onOpen(key)
        TODO("APR: use JVM equivalent - fetch avatar name then update dialog UI with caller info and call type")
    }

    override fun onLifetimeExpired() {
        processCallResponse(DECLINE, payload)
    }

    companion object {
        const val ACCEPT = 0
        const val DECLINE = 1
        const val START_IM = 2

        fun processCallResponse(response: Int, payload: Map<String, Any>) {
            val sessionId = payload["session_id"] as? UUID ?: return
            val callerId = payload["caller_id"] as? UUID ?: return
            val invType = (payload["inv_type"] as? Int)?.let { IMMgr.InvitationType.values().getOrNull(it) }
                ?: IMMgr.InvitationType.INSTANT_MESSAGE
            when (response) {
                ACCEPT -> {
                    TODO("APR: use JVM equivalent - accept chatter-box invitation for $sessionId via HTTP coroutine")
                }
                DECLINE -> {
                    TODO("APR: use JVM equivalent - send rejection to server and clear pending invitation for $sessionId")
                }
                START_IM -> {
                    TODO("APR: use JVM equivalent - open IM session with $callerId without starting call")
                }
            }
        }
    }
}

// ─── OutgoingCallDialog ───────────────────────────────────────────────────────

class OutgoingCallDialog(payload: Map<String, Any>) : CallDialog(payload) {

    companion object {
        val OCD_KEY: UUID = UUID.fromString("7CF78E11-0CFE-498D-ADB9-1417BF03DDB4")

        fun onCancel(userData: Any) {
            TODO("APR: use JVM equivalent - end the voice call in progress when user cancels")
        }
    }

    override fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent - wire cancel button; hide all text boxes initially")
        return true
    }

    fun show(key: Any) {
        TODO("APR: use JVM equivalent - update outgoing call dialog with session/caller info and show")
    }
}

// ─── Platform stubs ──────────────────────────────────────────────────────────

private fun elapsedSeconds(): Double {
    TODO("APR: use JVM equivalent - return elapsed seconds since viewer startup (monotonic clock)")
}

private fun agentIsInGroup(groupId: UUID): Boolean {
    TODO("APR: use JVM equivalent - gAgent.isInGroup($groupId)")
}

private fun sendStartSession(
    sessionId: UUID, otherParticipantId: UUID, ids: List<UUID>, dialog: InstantMessageType, p2pAsAdhocCall: Boolean,
): Boolean {
    TODO("APR: use JVM equivalent - IMModel.sendStartSession call, returns true if async reply needed")
}
