/**
 * ChatBar.kt
 * Converted from llchatbar.h / llchatbar.cpp
 *
 * Chat input bar — handles text entry, gesture expansion, channel routing, and
 * dispatching chat messages to the network.
 *
 * Note: The original llchatbar.h is wrapped in `#if 0 … #endif`, meaning the
 * class was disabled in the Firestorm fork (nearby-chat was redesigned).  This
 * Kotlin port preserves the public API as documented in the header so that
 * dependent code can reference it, but marks obsolete helpers accordingly.
 *
 * Equivalent to LLChatBar : LLPanel in the C++ viewer.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

/**
 * Chat type, mirroring EChatType from llchat.h.
 */
enum class ChatType {
    /** Normal spoken text (triggers nodding animation when [animate] is true). */
    NORMAL,
    /** Whisper — shorter range. */
    WHISPER,
    /** Shout — longer range. */
    SHOUT,
    /** Owner-say (object-to-owner, not user-generated). */
    OWNER_SAY,
    /** Region-say (LSL llRegionSay). */
    REGION_SAY,
    /** Direct / private channel message. */
    DIRECT,
    /** Debug output (channel 2147483647). */
    DEBUG_MSG,
}

/**
 * The chat input bar that appears at the bottom of the viewer window.
 *
 * ```kotlin
 * val bar = ChatBar()
 * bar.startChat("Hello, ")
 * bar.sendChatFromViewer("Hello, world!", ChatType.NORMAL, animate = true)
 * ChatBar.stopChat()
 * ```
 *
 * C++ lineage: LLChatBar : LLPanel  (disabled in Firestorm; API preserved)
 */
class ChatBar {

    // ── Input state ───────────────────────────────────────────────────────

    /** Current text in the input editor. */
    private var inputText: String = ""

    /** Whether the chat bar's input widget has keyboard focus. */
    private var keyboardFocused: Boolean = false

    /** Whether arrow keys should be ignored (tutorial mode). */
    private var ignoreArrowKeys: Boolean = false

    /** The last channel used for a non-zero channel chat (e.g. /20 foo). */
    private var lastSpecialChatChannel: Int = 0

    private var isBuilt: Boolean = false

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Called after the UI panel is constructed.
     * Mirrors LLChatBar::postBuild().
     */
    fun postBuild(): Boolean {
        isBuilt = true
        return true
    }

    // ── Input helpers ─────────────────────────────────────────────────────

    /** @return the text currently in the input editor. */
    fun getCurrentChat(): String = inputText

    /** @return true if the input editor widget has keyboard focus. */
    fun inputEditorHasFocus(): Boolean = keyboardFocused

    /** Move keyboard focus into (or out of) the input editor. */
    fun setKeyboardFocus(focused: Boolean) {
        keyboardFocused = focused
    }

    /** When [ignore] is true the arrow keys won't scroll chat history. */
    fun setIgnoreArrowKeys(ignore: Boolean) {
        ignoreArrowKeys = ignore
    }

    /** Pre-fill the input editor with [text] and grant it keyboard focus. */
    fun startChat(line: String) {
        inputText = line
        keyboardFocused = true
    }

    // ── Sending ───────────────────────────────────────────────────────────

    /**
     * Parse [text] for a channel prefix (e.g. "/20 Hello") and dispatch the
     * chat message on the appropriate channel.
     *
     * @param text    UTF-8 chat text, possibly prefixed with "/channel ".
     * @param type    [ChatType] determining range and animation.
     * @param animate If true and [type] is [ChatType.NORMAL], the avatar nods.
     *
     * Mirrors LLChatBar::sendChatFromViewer(const std::string&, EChatType, bool).
     */
    fun sendChatFromViewer(text: String, type: ChatType, animate: Boolean) {
        val (message, channel) = stripChannelNumber(text)
        if (channel != 0) lastSpecialChatChannel = channel
        dispatchChat(message, channel, type, animate)
        inputText = ""
    }

    /**
     * Strip a leading "/channel " prefix from [message].
     *
     * Returns the message body and extracted channel number (0 if none).
     * Mirrors LLChatBar::stripChannelNumber().
     */
    fun stripChannelNumber(message: String): Pair<String, Int> {
        val channelRegex = Regex("""^/(\d+)\s*(.*)$""", RegexOption.DOT_MATCHES_ALL)
        val match = channelRegex.find(message.trimStart())
        return if (match != null) {
            val channel = match.groupValues[1].toIntOrNull() ?: 0
            val body    = match.groupValues[2]
            body to channel
        } else {
            message to 0
        }
    }

    // ── Gestures ──────────────────────────────────────────────────────────

    /**
     * Re-populate the gesture combo from the current gesture manager state.
     * Mirrors LLChatBar::refreshGestures().
     */
    fun refreshGestures() {
        // Full implementation queries LLGestureMgr for active gestures.
    }

    /** General UI refresh (label timers, gesture state, etc.). */
    fun refresh() {
        refreshGestures()
    }

    // ── Private dispatch ──────────────────────────────────────────────────

    private fun dispatchChat(
        text: String,
        channel: Int,
        type: ChatType,
        animate: Boolean,
    ) {
        if (text.isBlank()) return
        // Full implementation: expand gestures, build the CHAT_FROM_VIEWER
        // network message, and send it via LLMessageSystem.
        // Trigger animation when animate == true && type == NORMAL.
    }

    // ── Companion (static) ────────────────────────────────────────────────

    companion object {
        /** Global chat bar instance, mirroring `gChatBar` in the C++ code. */
        @JvmField
        var instance: ChatBar? = null

        /**
         * Focus the global chat bar and pre-fill it with [line].
         * Mirrors static LLChatBar::startChat(const char* line).
         */
        @JvmStatic
        fun startChat(line: String) {
            instance?.startChat(line)
        }

        /**
         * Remove focus from the global chat bar.
         * Mirrors static LLChatBar::stopChat().
         */
        @JvmStatic
        fun stopChat() {
            instance?.setKeyboardFocus(false)
        }
    }

    override fun toString(): String =
        "ChatBar(focused=$keyboardFocused, channel=$lastSpecialChatChannel, text='$inputText')"
}
