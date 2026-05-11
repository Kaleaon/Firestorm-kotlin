/**
 * @file FSFloaterIM.kt
 * @brief Instant-message floater for a single IM session.
 *
 * Ported from fsfloaterim.h / fsfloaterim.cpp
 * Original copyright (C) 2009 Linden Research, Inc. / Firestorm Project — LGPL v2.1
 *
 * The C++ class inherits from LLTransientDockableFloater, LLVoiceClientStatusObserver,
 * LLFriendObserver, LLEventTimer, and FSChatParticipants — a heavyweight UI base
 * hierarchy that has no direct Kotlin equivalent until the UI layer is ported.
 * Those concerns are modelled as interfaces / companion-object statics below.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Data types
// ---------------------------------------------------------------------------

/**
 * A single chat message recorded in an IM conversation.
 *
 * @param from      Display name of the sender.
 * @param message   Message body (UTF-8).
 * @param timestamp Unix epoch milliseconds.
 */
data class Message(
    val from: String,
    val message: String,
    val timestamp: Long,
)

// ---------------------------------------------------------------------------
// FSFloaterIM
// ---------------------------------------------------------------------------

/**
 * Window representing one IM session, identified by [sessionId].
 *
 * In the C++ viewer this class owns a large portion of the IM UI (chat history,
 * input editor, voice controls, emoji picker, snooze timer).  The Kotlin port
 * captures the essential state and public API while stubbing the UI wiring.
 *
 * @param sessionId Unique identifier for the IM session (matches LLUUID in C++).
 */
class FSFloaterIM(val sessionId: LLUUID) {

    /** Human-readable title shown in the floater's title bar. */
    var sessionName: String = ""

    /** Ordered log of all messages received in this session. */
    val messages: MutableList<Message> = mutableListOf()

    /** Index of the last message that has been rendered, mirrors `mLastMessageIndex`. */
    var lastMessageIndex: Int = -1

    /** True when this session is a peer-to-peer (non-group, non-ad-hoc) chat. */
    var isP2PChat: Boolean = false

    /** Unread message count; used for notification badge. */
    var unreadMessageCount: Int = 0
        private set

    // -----------------------------------------------------------------------
    // Message handling
    // -----------------------------------------------------------------------

    /**
     * Append a new message to [messages] and update bookkeeping counters.
     *
     * C++ equivalent: `FSFloaterIM::updateMessages()` / `newIMCallback`.
     *
     * @param from      Sender display name.
     * @param msg       Message text.
     * @param timestamp Message creation time (Unix epoch ms).
     */
    fun addMessage(from: String, msg: String, timestamp: Long) {
        messages.add(Message(from = from, message = msg, timestamp = timestamp))
        lastMessageIndex = messages.lastIndex
        unreadMessageCount++
        // TODO: Scroll chat history to bottom; update unread-message badge UI.
    }

    /**
     * Mark all pending messages as read and reset the unread counter.
     *
     * C++ equivalent: `updateUnreadMessageNotification(0)`.
     */
    fun markAllRead() {
        unreadMessageCount = 0
        // TODO: Hide unread-message notification panel.
    }

    /**
     * Send a text message from the local user into this session.
     *
     * C++ equivalent: `FSFloaterIM::sendMsg(const std::string& msg)`.
     */
    fun sendMsg(msg: String) {
        TODO("Route '$msg' through LLIMMgr to session $sessionId")
    }

    /**
     * Reload the full message log from the LLIMModel, optionally clearing
     * previously rendered content first.
     *
     * C++ equivalent: `FSFloaterIM::reloadMessages(clean_messages)`.
     */
    fun reloadMessages(cleanMessages: Boolean = false) {
        if (cleanMessages) messages.clear()
        TODO("Re-fetch message history from LLIMModel for session $sessionId")
    }

    // -----------------------------------------------------------------------
    // Companion object — static factory helpers
    // -----------------------------------------------------------------------

    companion object {

        /** Registry of all open IM floaters keyed by session UUID. */
        private val instances: MutableMap<LLUUID, FSFloaterIM> = mutableMapOf()

        /**
         * Make the IM floater for [sessionId] visible, creating it if needed.
         *
         * C++ equivalent: `FSFloaterIM::show(session_id)`.
         *
         * @return The floater instance that was shown.
         */
        fun show(sessionId: LLUUID): FSFloaterIM {
            val floater = instances.getOrPut(sessionId) { FSFloaterIM(sessionId) }
            // TODO: Bring floater to front / add to tabbed IM container.
            return floater
        }

        /**
         * Hide the IM floater for [sessionId] if it is open.
         *
         * C++ equivalent: part of `FSFloaterIM::setVisible(false)`.
         */
        fun hide(sessionId: LLUUID) {
            instances[sessionId]
                ?: return  // Nothing to do if not open.
            // TODO: Detach from UI hierarchy / minimise in IM container.
        }

        /**
         * Toggle visibility of the floater for [sessionId].
         *
         * C++ equivalent: `FSFloaterIM::toggle(session_id)`.
         *
         * @return `true` if the floater is now visible, `false` otherwise.
         */
        fun toggle(sessionId: LLUUID): Boolean {
            TODO("Check current visibility state and call show() or hide() accordingly for $sessionId")
        }

        /**
         * Return an existing floater instance without creating one.
         *
         * C++ equivalent: `FSFloaterIM::findInstance(session_id)`.
         */
        fun findInstance(sessionId: LLUUID): FSFloaterIM? = instances[sessionId]

        /**
         * Return or create a floater instance.
         *
         * C++ equivalent: `FSFloaterIM::getInstance(session_id)`.
         */
        fun getInstance(sessionId: LLUUID): FSFloaterIM =
            instances.getOrPut(sessionId) { FSFloaterIM(sessionId) }
    }
}

// ---------------------------------------------------------------------------
// Timer helper  (C++ FSFloaterIMTimer : LLEventTimer)
// ---------------------------------------------------------------------------

/**
 * Periodic timer associated with an [FSFloaterIM] instance.
 *
 * In C++ this subclasses `LLEventTimer` and fires a callback on each tick.
 * In Kotlin the timer logic should be driven by a coroutine or
 * `java.util.Timer`; the callback type is preserved for API compatibility.
 *
 * @param callback Invoked on each timer tick.
 */
class FSFloaterIMTimer(private val callback: () -> Unit) {

    /** Fire the callback — called by the scheduler on each interval. */
    fun tick(): Boolean {
        callback()
        // Return false to keep the timer running (matches C++ LLEventTimer contract).
        return false
    }
}
