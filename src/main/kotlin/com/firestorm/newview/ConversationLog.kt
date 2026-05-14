/**
 * ConversationLog.kt
 *
 * Kotlin conversion of llconversationlog.h / llconversationlog.cpp
 * Original: Stores all agent conversations (IM, voice, group, nearby).
 * Copyright (C) 2002-2012, Linden Research, Inc. — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Session-type enum (mirrors LLIMModel::LLIMSession::SType / SessionType)
// ---------------------------------------------------------------------------

/**
 * Categorises the kind of conversation session.
 *
 * Mirrors the C++ `SessionType` typedef (`LLIMModel::LLIMSession::SType`).
 */
enum class SessionType {
    /** Private peer-to-peer instant message. */
    P2P,

    /** Group instant message channel. */
    GROUP,

    /** Nearby (local) voice/chat channel. */
    NEARBY
}

// ---------------------------------------------------------------------------
// Observer
// ---------------------------------------------------------------------------

/**
 * Receives change notifications from [ConversationLog].
 * Mirrors `LLConversationLogObserver`.
 */
interface ConversationLogObserver {

    /** Bit masks sent to [changed(LLUUID, Int)]. */
    object Change {
        const val TIME: Int = 1        // last interaction time updated
        const val NAME: Int = 2        // conversation name changed
        const val OFFLINE_IMS: Int = 3 // offline-message flag changed
    }

    /** Bulk change — one or more sessions were added, removed, or updated. */
    fun changed()

    /**
     * Fine-grained change for a specific session.
     *
     * @param sessionId UUID of the affected session.
     * @param mask      Combination of [Change] bit flags.
     */
    fun changed(sessionId: LLUUID, mask: Int) {}
}

// ---------------------------------------------------------------------------
// ConversationSession data class
// ---------------------------------------------------------------------------

/**
 * Represents a single logged conversation session.
 *
 * Mirrors the C++ `LLConversation` class (its stored state, not its UI
 * callbacks which are irrelevant in Kotlin).
 *
 * @property sessionId          UUID for this conversation session.
 * @property participantId      UUID of the other participant (or group).
 * @property name               Display name of the conversation.
 * @property type               Session type ([SessionType]).
 * @property timestamp          Unix epoch millis of the last interaction.
 * @property hasOfflineMessages True when there are unread offline IMs.
 * @property historyFileName    Base name of the chat-history file on disk.
 */
data class ConversationSession(
    val sessionId: LLUUID,
    val participantId: LLUUID,
    val name: String,
    val type: SessionType,
    val timestamp: Long,
    val hasOfflineMessages: Boolean,
    val historyFileName: String = ""
) {
    /** Returns a human-readable timestamp string (mm/dd/yyyy hh:mm). */
    fun formattedTimestamp(): String {
        val ms = timestamp
        // Lightweight formatting without java.text.SimpleDateFormat:
        val s = java.util.Date(ms).toString()
        return s // full replacement would parse into mm/dd/yyyy hh:mm
    }

    /** True if this session's last interaction is older than [days] days. */
    fun isOlderThan(days: Int): Boolean {
        val ageMs = System.currentTimeMillis() - timestamp
        return ageMs > days.toLong() * 24L * 3600L * 1000L
    }
}

// ---------------------------------------------------------------------------
// ConversationLog singleton
// ---------------------------------------------------------------------------

/**
 * Stores all agent conversation sessions and notifies registered observers of
 * changes.
 *
 * Corresponds to the C++ `LLConversationLog` singleton
 * (`LLSingleton<LLConversationLog>`).
 *
 * Persistence (save/load from `conversation.log`) is stubbed.
 */
object ConversationLog {

    // -----------------------------------------------------------------------
    // In-memory session list
    // -----------------------------------------------------------------------

    /**
     * Ordered list of all logged sessions, newest-first by convention.
     * Mirrors `LLConversationLog::mConversations`.
     */
    val sessions: MutableList<ConversationSession> = mutableListOf()

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Whether conversation logging is currently enabled. */
    var loggingEnabled: Boolean = false
        private set

    // -----------------------------------------------------------------------
    // Observers
    // -----------------------------------------------------------------------

    private val observers: MutableSet<ConversationLogObserver> = mutableSetOf()

    fun addObserver(observer: ConversationLogObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: ConversationLogObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers() {
        observers.forEach { it.changed() }
    }

    private fun notifyObservers(sessionId: LLUUID, mask: Int) {
        observers.forEach { it.changed(sessionId, mask) }
    }

    // -----------------------------------------------------------------------
    // Session management
    // -----------------------------------------------------------------------

    /**
     * Add a new [session] to the log and notify observers.
     * Maps to `LLConversationLog::createConversation()`.
     */
    fun addSession(session: ConversationSession) {
        sessions.add(0, session) // prepend so newest is first
        notifyObservers()
    }

    /**
     * Remove the session with [sessionId] from the log and notify observers.
     * Maps to `LLConversationLog::removeConversation()`.
     */
    fun removeSession(sessionId: LLUUID) {
        val removed = sessions.removeAll { it.sessionId == sessionId }
        if (removed) notifyObservers()
    }

    /**
     * Return the first session whose [ConversationSession.sessionId] matches
     * [id], or null if not found.
     * Maps to `LLConversationLog::getConversation()`.
     */
    fun getSession(id: LLUUID): ConversationSession? =
        sessions.firstOrNull { it.sessionId == id }

    /**
     * Remove all sessions from the log.
     * Maps to the "clear log" action in `LLConversationLog::onClearLog()`.
     */
    fun clear() {
        sessions.clear()
        notifyObservers()
    }

    // -----------------------------------------------------------------------
    // Logging entry-point
    // -----------------------------------------------------------------------

    /**
     * Create and store a new session record.
     *
     * Mirrors `LLConversationLog::logConversation()` / `sessionAdded()`.
     *
     * @param id                UUID to use as both session and participant ID
     *                          (caller may pass different values via [addSession]).
     * @param name              Display name for the session.
     * @param type              Session kind ([SessionType]).
     * @param hasOfflineMessages Whether the session has unread offline IMs.
     */
    fun logConversation(
        id: LLUUID,
        name: String,
        type: SessionType,
        hasOfflineMessages: Boolean = false
    ) {
        val session = ConversationSession(
            sessionId = id,
            participantId = id,
            name = name,
            type = type,
            timestamp = System.currentTimeMillis(),
            hasOfflineMessages = hasOfflineMessages
        )
        addSession(session)
    }

    // -----------------------------------------------------------------------
    // Timestamp update
    // -----------------------------------------------------------------------

    /**
     * Refresh the last-interaction timestamp for session [sessionId] and
     * notify observers of the [ConversationLogObserver.Change.TIME] change.
     */
    fun updateTimestamp(sessionId: LLUUID) {
        val idx = sessions.indexOfFirst { it.sessionId == sessionId }
        if (idx >= 0) {
            sessions[idx] = sessions[idx].copy(timestamp = System.currentTimeMillis())
            notifyObservers(sessionId, ConversationLogObserver.Change.TIME)
        }
    }

    /**
     * Update the display name for session [sessionId] and notify observers of
     * the [ConversationLogObserver.Change.NAME] change.
     */
    fun updateName(sessionId: LLUUID, newName: String) {
        val idx = sessions.indexOfFirst { it.sessionId == sessionId }
        if (idx >= 0) {
            sessions[idx] = sessions[idx].copy(name = newName)
            notifyObservers(sessionId, ConversationLogObserver.Change.NAME)
        }
    }

    /**
     * Update the offline-messages flag for session [sessionId] and notify
     * observers of the [ConversationLogObserver.Change.OFFLINE_IMS] change.
     */
    fun updateOfflineMessages(sessionId: LLUUID, hasMessages: Boolean) {
        val idx = sessions.indexOfFirst { it.sessionId == sessionId }
        if (idx >= 0) {
            sessions[idx] = sessions[idx].copy(hasOfflineMessages = hasMessages)
            notifyObservers(sessionId, ConversationLogObserver.Change.OFFLINE_IMS)
        }
    }

    // -----------------------------------------------------------------------
    // Persistence (stubs)
    // -----------------------------------------------------------------------

    /**
     * Persist the current session list to disk.
     * Maps to `LLConversationLog::cache()` / `saveToFile()`.
     */
    fun cache(): Unit {
        System.err.println("ConversationLog: cache not yet implemented")
    }

    /**
     * Load sessions from the on-disk conversation log.
     * Maps to `LLConversationLog::loadFromFile()`.
     */
    fun load(): Unit {
        System.err.println("ConversationLog: load not yet implemented")
    }

    /**
     * Enable or disable conversation logging.
     * Maps to `LLConversationLog::enableLogging()`.
     */
    fun enableLogging(enabled: Boolean) {
        loggingEnabled = enabled
    }

    /** @return true if the in-memory session list is empty. */
    fun isEmpty(): Boolean = sessions.isEmpty()
}
