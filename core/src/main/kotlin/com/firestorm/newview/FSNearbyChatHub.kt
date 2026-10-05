/**
 * @file FSNearbyChatHub.kt
 * @brief Central hub for nearby-chat handling across multiple input controls.
 *
 * Ported from fsnearbychathub.h / fsnearbychathub.cpp
 * Original copyright (C) 2010 Linden Research, Inc. / 2012 Zi Ree — LGPL v2.1
 *
 * The C++ singleton `FSNearbyChat` is renamed `FSNearbyChatHub` here to avoid
 * confusion with the floater class `FSFloaterNearbyChat` and to better
 * reflect its role as a pure message-routing hub.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Enumerations  (EChatType / EChatSourceType from llchat.h)
// ---------------------------------------------------------------------------

/**
 * Chat volume / type, mirroring `EChatType` in the C++ viewer.
 *
 * - [WHISPER]  Short-range (10 m) chat — `CHAT_TYPE_WHISPER`.
 * - [NORMAL]   Standard chat range (20 m) — `CHAT_TYPE_NORMAL`.
 * - [SHOUT]    Long-range (100 m) chat — `CHAT_TYPE_SHOUT`.
 * - [START]    Typing-start notification — `CHAT_TYPE_START`.
 * - [STOP]     Typing-stop notification — `CHAT_TYPE_STOP`.
 * - [OOC]      Firestorm out-of-character wrapper — `CHAT_TYPE_OOC`.
 */
enum class ChatType {
    WHISPER,
    NORMAL,
    SHOUT,
    START,
    STOP,
    OOC,
}

/**
 * Origin of a chat message, mirroring `EChatSourceType` in the C++ viewer.
 *
 * - [SYSTEM]   Viewer-generated system message.
 * - [AGENT]    Message from a human avatar.
 * - [OBJECT]   Message from an in-world scripted object.
 * - [UNKNOWN]  Source could not be determined.
 */
enum class ChatSource {
    SYSTEM,
    AGENT,
    OBJECT,
    UNKNOWN,
}

// ---------------------------------------------------------------------------
// Data class
// ---------------------------------------------------------------------------

/**
 * Represents a single nearby-chat message delivered to observers.
 *
 * @param fromId     UUID of the sender (avatar or object).
 * @param fromName   Display name of the sender.
 * @param message    Chat text (UTF-8).
 * @param chatType   Volume / type of the message.
 * @param sourceType Origin category of the message.
 */
data class ChatMessage(
    val fromId: LLUUID,
    val fromName: String,
    val message: String,
    val chatType: ChatType,
    val sourceType: ChatSource,
)

// ---------------------------------------------------------------------------
// Observer interface
// ---------------------------------------------------------------------------

/**
 * Implement this interface to receive nearby-chat messages from [FSNearbyChatHub].
 */
fun interface ChatObserver {
    /** Called on the thread that calls [FSNearbyChatHub.broadcast]. */
    fun onChat(msg: ChatMessage)
}

// ---------------------------------------------------------------------------
// Singleton hub  (C++ LLSingleton<FSNearbyChat> → Kotlin object)
// ---------------------------------------------------------------------------

/**
 * Singleton dispatcher for nearby-chat messages.
 *
 * UI panels and other consumers register as [ChatObserver]s; the network
 * layer (or the agent) calls [broadcast] to fan-out each message.
 *
 * Key behaviours from the C++ implementation preserved here:
 * - Observer registration / deregistration is thread-safe via `synchronized`.
 * - [broadcast] iterates a snapshot of the observer list to avoid
 *   ConcurrentModificationException if an observer removes itself during delivery.
 * - Chat-type triggers ("/whisper", "/shout") and channel-number parsing
 *   (`stripChannelNumber`) are provided as utility methods and stubbed where
 *   they depend on the agent or gesture manager.
 *
 * C++ equivalent: `FSNearbyChat` singleton.
 */
object FSNearbyChatHub {

    /** Last special chat channel used with the "//" repeat-channel syntax. */
    var lastSpecialChatChannel: Int = 0
        private set

    private val observers: MutableList<ChatObserver> = mutableListOf()

    // -----------------------------------------------------------------------
    // Observer management
    // -----------------------------------------------------------------------

    /**
     * Register [observer] to receive future [ChatMessage]s.
     *
     * Safe to call more than once with the same instance; duplicates are
     * silently ignored.
     */
    fun addObserver(observer: ChatObserver) {
        synchronized(observers) {
            if (observer !in observers) observers.add(observer)
        }
    }

    /**
     * Unregister [observer] so it no longer receives messages.
     *
     * No-op if [observer] was not registered.
     */
    fun removeObserver(observer: ChatObserver) {
        synchronized(observers) { observers.remove(observer) }
    }

    // -----------------------------------------------------------------------
    // Message dispatch
    // -----------------------------------------------------------------------

    /**
     * Deliver [msg] to all currently-registered [ChatObserver]s.
     *
     * Iterates a snapshot so observers may safely remove themselves during
     * the callback without causing a [ConcurrentModificationException].
     *
     * C++ equivalent: the signal fired inside `LLNotificationsUI` / viewer chat pipeline.
     */
    fun broadcast(msg: ChatMessage) {
        val snapshot = synchronized(observers) { observers.toList() }
        for (obs in snapshot) {
            obs.onChat(msg)
        }
    }

    // -----------------------------------------------------------------------
    // Chat sending (stubs — depend on LLAgent / LLMessageSystem)
    // -----------------------------------------------------------------------

    /**
     * Send [text] as [chatType] on channel 0, with optional avatar animation.
     *
     * C++ equivalent: `FSNearbyChat::sendChat(text, type)` →
     * `sendChatFromViewer(text, type, animate)`.
     */
    fun sendChat(text: String, chatType: ChatType, animate: Boolean = true) {
        System.err.println("FSNearbyChatHub: sendChat not yet implemented")
    }

    /**
     * Parse "/N text" or "// text" channel syntax from [mesg].
     *
     * Returns a pair of (stripped message text, resolved channel number).
     * Updates [lastSpecialChatChannel] as a side-effect for "//" repeats.
     *
     * C++ equivalent: `FSNearbyChat::stripChannelNumber`.
     */
    fun stripChannelNumber(mesg: String): Pair<String, Int> {
        System.err.println("FSNearbyChatHub: stripChannelNumber not yet implemented")
        return Pair("", 0)
    }

    /**
     * Examine [str] for leading "/whisper" or "/shout" tokens and return the
     * adjusted [ChatType], consuming the trigger prefix from [str].
     *
     * C++ equivalent: `FSNearbyChat::processChatTypeTriggers`.
     */
    fun processChatTypeTriggers(type: ChatType, str: StringBuilder): ChatType {
        System.err.println("FSNearbyChatHub: processChatTypeTriggers not yet implemented")
        return type
    }
}
