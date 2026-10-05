package com.firestorm.newview

import java.util.UUID

enum class ChatType {
    CHAT_TYPE_NORMAL,
    CHAT_TYPE_SHOUT,
    CHAT_TYPE_WHISPER,
    CHAT_TYPE_OOC,
}

open class FSNearbyChatControl(
    val isDefault: Boolean = false,
    private var textPadLeft: Int = 0,
    private var textPadRight: Int = 0,
    private val backgroundPad: Int = 1,
    private val bgImage: Any? = null,
    private val bgImageDisabled: Any? = null,
    private val bgImageFocused: Any? = null,
) : FSChatParticipants {

    private var rlvBehaviorCallbackConnection: (() -> Unit)? = null
    private var emojiHelperSettingConnection: (() -> Unit)? = null

    private var showChatMentionPicker: Boolean = true
    private var showEmojiHelper: Boolean = false
    private var focused: Boolean = false
    private var readOnly: Boolean = false
    private var text: String = ""

    init {
        System.err.println("FSNearbyChatControl: register widget type fs_nearby_chat_control with UI factory not yet implemented")
        System.err.println("FSNearbyChatControl: setAutoreplaceCallback not yet implemented")
        System.err.println("FSNearbyChatControl: register keystroke callback via setKeystrokeCallback not yet implemented")
        System.err.println("FSNearbyChatControl: FSNearbyChat.instance().registerChatBar(this) not yet implemented")
        System.err.println("FSNearbyChatControl: setFont from LLViewerChat.getChatFont() not yet implemented")

        System.err.println("FSNearbyChatControl: read RlvActions.isRlvEnabled / canShowName to set showChatMentionPicker not yet implemented")
        System.err.println("FSNearbyChatControl: gRlvHandler.setBehaviourToggleCallback not yet implemented")

        System.err.println("FSNearbyChatControl: read saved setting FSEnableEmojiWindowPopupWhileTyping for showEmojiHelper not yet implemented")
        System.err.println("FSNearbyChatControl: subscribe to FSEnableEmojiWindowPopupWhileTyping signal not yet implemented")

        System.err.println("FSNearbyChatControl: LLViewerChat.setFontChangedCallback not yet implemented")
    }

    open fun onFocusReceived() {
        focused = true
        System.err.println("FSNearbyChatControl: setFocusedInputEditor(this, true) not yet implemented")
        System.err.println("FSNearbyChatControl: LLFloaterChatMentionPicker.updateParticipantSource(this) not yet implemented")
    }

    open fun onFocusLost() {
        focused = false
        System.err.println("FSNearbyChatControl: setFocusedInputEditor(this, false) not yet implemented")
        System.err.println("FSNearbyChatControl: LLFloaterChatMentionPicker.removeParticipantSource(this) not yet implemented")
    }

    open fun setFocus(focus: Boolean) {
        focused = focus
        System.err.println("FSNearbyChatControl: setFocusedInputEditor(this, focus) not yet implemented")
        System.err.println("FSNearbyChatControl: update LLFloaterChatMentionPicker participant source based on focus not yet implemented")
    }

    open fun draw() {
        applyTextPadding()
        drawBackground()
        System.err.println("FSNearbyChatControl: delegate to LLChatEntry.draw() not yet implemented")
    }

    open fun paste() {
        System.err.println("FSNearbyChatControl: call super paste() not yet implemented")
        // Flatten paragraph markers introduced by paste so chat stays on one visual line.
        System.err.println("FSNearbyChatControl: replace wchar 182 with newline in pasted content, restore cursor pos not yet implemented")
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        System.err.println("FSNearbyChatControl: handleKeyHere not yet implemented")
        return false
    }

    fun setTextPadding(left: Int, right: Int) {
        textPadLeft = left
        textPadRight = right
        applyTextPadding()
    }

    override fun getSessionParticipants(): MutableList<UUID> {
        System.err.println("FSNearbyChatControl: getSessionParticipants not yet implemented")
        return mutableListOf()
    }

    protected fun onKeystroke(caller: Any?) {
        System.err.println("FSNearbyChatControl: FSNearbyChat.handleChatBarKeystroke(caller) not yet implemented")
    }

    private fun drawBackground() {
        // no-op
    }

    private fun applyTextPadding() {
        System.err.println("FSNearbyChatControl: applyTextPadding not yet implemented")
    }

    private fun autohide(afterSend: Boolean) {
        if (!isDefault) return
        System.err.println("FSNearbyChatControl: autohide not yet implemented")
    }

    private fun updateRlvRestrictions(behavior: Int) {
        System.err.println("FSNearbyChatControl: updateRlvRestrictions not yet implemented")
    }

    private fun updateEmojiHelperSetting(data: Any?) {
        System.err.println("FSNearbyChatControl: updateEmojiHelperSetting not yet implemented")
    }

    protected fun finalize() {
        rlvBehaviorCallbackConnection = null
        emojiHelperSettingConnection = null
        System.err.println("FSNearbyChatControl: LLFloaterChatMentionPicker.removeParticipantSource(this) not yet implemented")
    }
}
