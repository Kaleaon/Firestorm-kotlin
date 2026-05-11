package com.firestorm.llui

private const val DEFAULT_EMOJI_HELPER_FLOATER = "emoji_picker"
private const val HELPER_FLOATER_OFFSET_X = 0
private const val HELPER_FLOATER_OFFSET_Y = 0

object EmojiHelper {

    // Weak references because the helper must not keep UI objects alive
    private var hostCtrl: UICtrl? = null
    private var helperFloater: Floater? = null

    private var hostCtrlFocusLostConn: Connection? = null
    private var helperCommitConn: Connection? = null
    private var helperCloseConn: Connection? = null
    private var emojiCommitCb: ((WChar) -> Unit)? = null
    private var isHideDisabled: Boolean = false

    private val closeSignal: Signal<(UICtrl?, Any?) -> Unit> = Signal()

    fun setIsHideDisabled(disabled: Boolean) {
        isHideDisabled = disabled
    }

    fun getToolTip(ch: WChar): String = EmojiDictionary.getNameFromEmoji(ch)

    fun isActive(ctrlP: UICtrl?): Boolean = hostCtrl === ctrlP

    // Checks whether the cursor is sitting inside a shortcode token (":word").
    // Returns the position of the leading ':' in shortCodePos when true.
    // Suppressing the popup while typing is a user-facing feature toggle (FSEnableEmojiWindowPopupWhileTyping).
    fun isCursorInEmojiCode(wtext: String, cursorPos: Int, shortCodePosOut: IntArray? = null): Boolean {
        if (cursorPos < 0 || wtext.length < cursorPos) return false

        var shortCodePos = if (cursorPos == 0 || wtext[cursorPos - 1] != ':') cursorPos else cursorPos - 1

        fun isPartOfShortcode(ch: Char): Boolean =
            ch == '-' || ch == '_' || ch == '+' || ch.isLetterOrDigit()

        while (shortCodePos > 1 && isPartOfShortcode(wtext[shortCodePos - 1])) {
            shortCodePos--
        }

        var isShortCode = (cursorPos - shortCodePos >= 2) && (wtext[shortCodePos - 1] == ':')
        // Avoid triggering the picker when typing times like "12:30"
        if (isShortCode && shortCodePos >= 2 && wtext[shortCodePos - 2].isDigit()) {
            isShortCode = false
        }

        shortCodePosOut?.set(0, if (isShortCode) shortCodePos - 1 else -1)
        return isShortCode
    }

    fun showHelper(
        hostctrlP: UICtrl?,
        localX: Int,
        localY: Int,
        shortCode: String,
        cb: (WChar) -> Unit
    ) {
        // Commit immediately when the user has typed a complete shortcode
        val matched = EmojiDictionary.getDescriptorFromShortCode(shortCode)
        if (matched != null) {
            cb(matched.character)
            hideHelper()
            return
        }

        if (helperFloater == null || helperFloater!!.isDead()) {
            val floater = FloaterReg.getInstance(DEFAULT_EMOJI_HELPER_FLOATER)
            if (floater == null) {
                return
            }
            helperFloater = floater
            helperCommitConn = floater.setCommitCallback { _, sdValue ->
                val codePoint = sdValue.asString().codePointAt(0)
                onCommitEmoji(codePoint)
            }
            helperCloseConn = floater.setCloseCallback { ctrl, param -> onCloseHelper(ctrl, param) }
        }

        setHostCtrl(hostctrlP)
        emojiCommitCb = cb

        val floater = helperFloater ?: return

        val floaterPos = hostctrlP?.localPointToOtherView(localX, localY, gFloaterView)
        if (hostctrlP != null && floaterPos == null) {
            return
        }

        val floaterX = floaterPos?.x ?: 0
        val floaterY = floaterPos?.y ?: 0

        val rect = floater.getRect()
        val left = floaterX - HELPER_FLOATER_OFFSET_X
        val top = floaterY - HELPER_FLOATER_OFFSET_Y + rect.height
        floater.setRect(rect.copy(left = left, top = top))

        val savedSoundFlags = floater.getSoundFlags()
        floater.setSoundFlags(View.SILENT)
        floater.openFloater(LLSD().with("hint", shortCode))
        floater.setSoundFlags(savedSoundFlags)
    }

    fun hideHelper(ctrlP: UICtrl? = null, strict: Boolean = false) {
        if (strict) isHideDisabled = false
        if (isHideDisabled || (ctrlP != null && !isActive(ctrlP))) return
        setHostCtrl(null)
    }

    fun handleKey(ctrlP: UICtrl?, key: Int, mask: Int): Boolean {
        if (helperFloater == null || helperFloater!!.isDead() || !isActive(ctrlP)) return false
        return helperFloater!!.handleKey(key, mask, true)
    }

    fun onCommitEmoji(emoji: WChar) {
        if (hostCtrl != null && emojiCommitCb != null) {
            emojiCommitCb!!(emoji)
        }
    }

    fun onCloseHelper(ctrl: UICtrl?, param: Any?) {
        closeSignal.emit(ctrl, param)
    }

    fun setCloseCallback(cb: (UICtrl?, Any?) -> Unit): Connection = closeSignal.connect(cb)

    private fun setHostCtrl(hostctrlP: UICtrl?) {
        if (hostCtrl !== hostctrlP) {
            hostCtrlFocusLostConn?.disconnect()
            hostCtrl = null
            emojiCommitCb = null

            helperFloater?.takeUnless { it.isDead() }?.closeFloater()

            if (hostctrlP != null) {
                hostCtrl = hostctrlP
                hostCtrlFocusLostConn = hostctrlP.setFocusLostCallback {
                    hideHelper(hostCtrl)
                }
            }
        }
    }
}
