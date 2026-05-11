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
        TODO("GPU: register widget type fs_nearby_chat_control with UI factory")
        TODO("APR: setAutoreplaceCallback -> wire LLAutoReplace equivalent")
        TODO("APR: register keystroke callback via setKeystrokeCallback")
        TODO("APR: FSNearbyChat.instance().registerChatBar(this)")
        TODO("APR: setFont from LLViewerChat.getChatFont()")

        TODO("APR: read RlvActions.isRlvEnabled / canShowName to set showChatMentionPicker")
        TODO("APR: gRlvHandler.setBehaviourToggleCallback -> store in rlvBehaviorCallbackConnection")

        TODO("APR: read saved setting FSEnableEmojiWindowPopupWhileTyping for showEmojiHelper")
        TODO("APR: subscribe to FSEnableEmojiWindowPopupWhileTyping signal -> store in emojiHelperSettingConnection")

        TODO("APR: LLViewerChat.setFontChangedCallback -> update font on change")
    }

    open fun onFocusReceived() {
        focused = true
        TODO("APR: FSNearbyChat.instance().setFocusedInputEditor(this, true)")
        TODO("APR: LLFloaterChatMentionPicker.updateParticipantSource(this)")
    }

    open fun onFocusLost() {
        focused = false
        TODO("APR: FSNearbyChat.instance().setFocusedInputEditor(this, false)")
        TODO("APR: LLFloaterChatMentionPicker.removeParticipantSource(this)")
    }

    open fun setFocus(focus: Boolean) {
        focused = focus
        TODO("APR: FSNearbyChat.instance().setFocusedInputEditor(this, focus)")
        TODO("APR: update LLFloaterChatMentionPicker participant source based on focus")
    }

    open fun draw() {
        applyTextPadding()
        drawBackground()
        TODO("GPU: delegate to LLChatEntry.draw()")
    }

    open fun paste() {
        TODO("APR: call super paste()")
        // Flatten paragraph markers introduced by paste so chat stays on one visual line.
        TODO("APR: replace wchar 182 with newline in pasted content, restore cursor pos")
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("APR: check LLChatMentionHelper / LLEmojiHelper active states and delegate if so")
        TODO("APR: KEY_ESCAPE -> autohide(false); gAgent.stopTyping()")
        TODO("APR: KEY_RETURN + MASK_CONTROL -> shout; MASK_SHIFT -> whisper; MASK_ALT -> OOC; MASK_SHIFT|MASK_CONTROL -> insert linefeed char 182; else normal say")
        TODO("APR: on handled send: updateHistory(), FSNearbyChat.instance().sendChat(text, type), clear text, autohide(true)")
        TODO("APR: fallback to LLChatEntry.handleKeyHere(key, mask)")
    }

    fun setTextPadding(left: Int, right: Int) {
        textPadLeft = left
        textPadRight = right
        applyTextPadding()
    }

    override fun getSessionParticipants(): MutableList<UUID> {
        TODO("APR: guard isAgentAvatarValid && LLWorld.instanceExists && LFSimFeatureHandler.instanceExists")
        TODO("APR: LLWorld.instance().getAvatars() within sayRange -> return as MutableList<UUID>")
    }

    protected fun onKeystroke(caller: Any?) {
        TODO("APR: FSNearbyChat.handleChatBarKeystroke(caller)")
    }

    private fun drawBackground() {
        TODO("GPU: select bgImage / bgImageDisabled / bgImageFocused based on readOnly / focused state, draw with alpha and optional focus-flash border, plus inner shade ring")
    }

    private fun applyTextPadding() {
        TODO("APR: compute visible text rect from scroller content window or local rect, clamp with textPadLeft / textPadRight, call needsReflow if changed")
    }

    private fun autohide(afterSend: Boolean) {
        if (!isDefault) return
        TODO("APR: read gAgentCamera.cameraMouselook, saved settings CloseChatOnReturn, FSCloseChatOnReturnInMouselook, AutohideChatBar, FSShowInterfaceInMouselook; setFocus(false) / showDefaultChatBar(false) accordingly")
    }

    private fun updateRlvRestrictions(behavior: Int) {
        TODO("APR: if behavior == RLV_BHVR_SHOWNAMES, refresh showChatMentionPicker from RlvActions")
    }

    private fun updateEmojiHelperSetting(data: Any?) {
        TODO("APR: setShowEmojiHelper(data.asBoolean())")
    }

    protected fun finalize() {
        rlvBehaviorCallbackConnection = null
        emojiHelperSettingConnection = null
        TODO("APR: LLFloaterChatMentionPicker.removeParticipantSource(this)")
    }
}
