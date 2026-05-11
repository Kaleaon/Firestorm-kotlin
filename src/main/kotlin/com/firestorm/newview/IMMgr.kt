package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmessage.IMType
import com.firestorm.llmessage.InstantMessage

object IMMgr {

    data class IMSession(
        val sessionId: LLUUID,
        val targetId: LLUUID,
        val name: String,
        val type: IMType,
        val messages: MutableList<IMMessage> = mutableListOf(),
    )

    data class IMMessage(
        val fromId: LLUUID,
        val fromName: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    interface IMObserver {
        fun newSession(sessionId: LLUUID)
        fun sessionAdded(sessionId: LLUUID, name: String, otherParticipantId: LLUUID, hasOfflineMessages: Boolean)
        fun sessionRemoved(sessionId: LLUUID)
        fun newMessages(sessionId: LLUUID)
    }

    val sessions: MutableMap<LLUUID, IMSession> = mutableMapOf()
    private val observers: MutableList<IMObserver> = mutableListOf()

    fun addSession(sessionId: LLUUID, targetId: LLUUID, name: String, type: IMType): IMSession {
        val session = IMSession(sessionId, targetId, name, type)
        sessions[sessionId] = session
        notifyObservers { it.newSession(sessionId) }
        notifyObservers { it.sessionAdded(sessionId, name, targetId, false) }
        return session
    }

    fun getSession(sessionId: LLUUID): IMSession? = sessions[sessionId]

    fun removeSession(sessionId: LLUUID) {
        sessions.remove(sessionId)
        notifyObservers { it.sessionRemoved(sessionId) }
    }

    fun addMessage(sessionId: LLUUID, fromId: LLUUID, fromName: String, message: String) {
        val session = sessions[sessionId] ?: return
        session.messages.add(IMMessage(fromId, fromName, message))
        notifyObservers { it.newMessages(sessionId) }
    }

    fun sendMessage(sessionId: LLUUID, message: String) {}

    fun startIM(targetId: LLUUID, name: String) {}

    fun onIncomingIM(msg: InstantMessage) {
        val sessionId = msg.sessionId
        val session = sessions[sessionId]
            ?: addSession(sessionId, msg.fromId, msg.fromName, msg.imType)
        session.messages.add(IMMessage(msg.fromId, msg.fromName, msg.message))
        notifyObservers { it.newMessages(sessionId) }
    }

    fun addObserver(obs: IMObserver) {
        if (obs !in observers) observers.add(obs)
    }

    fun removeObserver(obs: IMObserver) {
        observers.remove(obs)
    }

    private fun notifyObservers(fn: (IMObserver) -> Unit) {
        observers.toList().forEach(fn)
    }
}
