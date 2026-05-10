package com.firestorm.llwindow

typealias KEY = UByte
typealias MASK = UInt

enum class EKeystate { KEYSTATE_DOWN, KEYSTATE_LEVEL, KEYSTATE_UP }

enum class EKeyboardInsertMode { LL_KIM_INSERT, LL_KIM_OVERWRITE }

object KeyConstants {
    const val KEY_COUNT: Int = 256

    val KEY_NONE: KEY       = 0xFFu
    val KEY_SPECIAL: KEY    = 0x80u
    val KEY_RETURN: KEY     = 0x81u
    val KEY_LEFT: KEY       = 0x82u
    val KEY_RIGHT: KEY      = 0x83u
    val KEY_UP: KEY         = 0x84u
    val KEY_DOWN: KEY       = 0x85u
    val KEY_ESCAPE: KEY     = 0x86u
    val KEY_BACKSPACE: KEY  = 0x87u
    val KEY_DELETE: KEY     = 0x88u
    val KEY_SHIFT: KEY      = 0x89u
    val KEY_CONTROL: KEY    = 0x8Au
    val KEY_ALT: KEY        = 0x8Bu
    val KEY_HOME: KEY       = 0x8Cu
    val KEY_END: KEY        = 0x8Du
    val KEY_PAGE_UP: KEY    = 0x8Eu
    val KEY_PAGE_DOWN: KEY  = 0x8Fu
    val KEY_HYPHEN: KEY     = 0x90u
    val KEY_EQUALS: KEY     = 0x91u
    val KEY_INSERT: KEY     = 0x92u
    val KEY_CAPSLOCK: KEY   = 0x93u
    val KEY_TAB: KEY        = 0x94u
    val KEY_ADD: KEY        = 0x95u
    val KEY_SUBTRACT: KEY   = 0x96u
    val KEY_MULTIPLY: KEY   = 0x97u
    val KEY_DIVIDE: KEY     = 0x98u
    val KEY_F1: KEY         = 0xA1u
    val KEY_F2: KEY         = 0xA2u
    val KEY_F3: KEY         = 0xA3u
    val KEY_F4: KEY         = 0xA4u
    val KEY_F5: KEY         = 0xA5u
    val KEY_F6: KEY         = 0xA6u
    val KEY_F7: KEY         = 0xA7u
    val KEY_F8: KEY         = 0xA8u
    val KEY_F9: KEY         = 0xA9u
    val KEY_F10: KEY        = 0xAAu
    val KEY_F11: KEY        = 0xABu
    val KEY_F12: KEY        = 0xACu
    val KEY_PAD_UP: KEY     = 0xC0u
    val KEY_PAD_DOWN: KEY   = 0xC1u
    val KEY_PAD_LEFT: KEY   = 0xC2u
    val KEY_PAD_RIGHT: KEY  = 0xC3u
    val KEY_PAD_HOME: KEY   = 0xC4u
    val KEY_PAD_END: KEY    = 0xC5u
    val KEY_PAD_PGUP: KEY   = 0xC6u
    val KEY_PAD_PGDN: KEY   = 0xC7u
    val KEY_PAD_CENTER: KEY = 0xC8u
    val KEY_PAD_INS: KEY    = 0xC9u
    val KEY_PAD_DEL: KEY    = 0xCAu
    val KEY_PAD_RETURN: KEY = 0xCBu
    val KEY_PAD_DIVIDE: KEY = 0xCFu
    val KEY_BUTTON0: KEY    = 0xD0u
    val KEY_BUTTON1: KEY    = 0xD1u
    val KEY_BUTTON2: KEY    = 0xD2u
    val KEY_BUTTON3: KEY    = 0xD3u
    val KEY_BUTTON4: KEY    = 0xD4u
    val KEY_BUTTON5: KEY    = 0xD5u
    val KEY_BUTTON6: KEY    = 0xD6u
    val KEY_BUTTON7: KEY    = 0xD7u
    val KEY_BUTTON8: KEY    = 0xD8u
    val KEY_BUTTON9: KEY    = 0xD9u
    val KEY_BUTTON10: KEY   = 0xDAu
    val KEY_BUTTON11: KEY   = 0xDBu
    val KEY_BUTTON12: KEY   = 0xDCu
    val KEY_BUTTON13: KEY   = 0xDDu
    val KEY_BUTTON14: KEY   = 0xDEu
    val KEY_BUTTON15: KEY   = 0xDFu

    const val MASK_NONE: MASK        = 0x0000u
    const val MASK_CONTROL: MASK     = 0x0001u
    const val MASK_ALT: MASK         = 0x0002u
    const val MASK_SHIFT: MASK       = 0x0004u
    const val MASK_NORMALKEYS: MASK  = 0x0007u
    const val MASK_MAC_CONTROL: MASK = 0x0008u
}

abstract class LLKeyboard {
    // SDL2 widens native key codes to U32; the base type is U16 otherwise.
    // Kotlin uses Int for the widened platform; callers cast as appropriate.
    typealias NativeKeyType = UInt

    protected var mCallbacks: LLWindowCallbacks? = null

    private val mKeyLevelTimer: LongArray = LongArray(KeyConstants.KEY_COUNT)
    protected val mKeyLevelFrameCount: IntArray = IntArray(KeyConstants.KEY_COUNT)
    protected val mKeyLevel: BooleanArray = BooleanArray(KeyConstants.KEY_COUNT)
    protected val mKeyRepeated: BooleanArray = BooleanArray(KeyConstants.KEY_COUNT)
    protected val mKeyUp: BooleanArray = BooleanArray(KeyConstants.KEY_COUNT)
    protected val mKeyDown: BooleanArray = BooleanArray(KeyConstants.KEY_COUNT)
    protected var mCurTranslatedKey: KEY = KeyConstants.KEY_NONE
    protected var mCurScanKey: KEY = KeyConstants.KEY_NONE

    protected val mTranslateKeyMap: MutableMap<NativeKeyType, KEY> = mutableMapOf()
    protected val mInvTranslateKeyMap: MutableMap<KEY, NativeKeyType> = mutableMapOf()

    var mInsertMode: EKeyboardInsertMode = EKeyboardInsertMode.LL_KIM_INSERT
        private set

    init {
        addKeyName(' '.code.toUByte(), "Space")
        addKeyName(KeyConstants.KEY_RETURN, "Enter")
        addKeyName(KeyConstants.KEY_LEFT, "Left")
        addKeyName(KeyConstants.KEY_RIGHT, "Right")
        addKeyName(KeyConstants.KEY_UP, "Up")
        addKeyName(KeyConstants.KEY_DOWN, "Down")
        addKeyName(KeyConstants.KEY_ESCAPE, "Esc")
        addKeyName(KeyConstants.KEY_HOME, "Home")
        addKeyName(KeyConstants.KEY_END, "End")
        addKeyName(KeyConstants.KEY_PAGE_UP, "PgUp")
        addKeyName(KeyConstants.KEY_PAGE_DOWN, "PgDn")
        addKeyName(KeyConstants.KEY_F1, "F1")
        addKeyName(KeyConstants.KEY_F2, "F2")
        addKeyName(KeyConstants.KEY_F3, "F3")
        addKeyName(KeyConstants.KEY_F4, "F4")
        addKeyName(KeyConstants.KEY_F5, "F5")
        addKeyName(KeyConstants.KEY_F6, "F6")
        addKeyName(KeyConstants.KEY_F7, "F7")
        addKeyName(KeyConstants.KEY_F8, "F8")
        addKeyName(KeyConstants.KEY_F9, "F9")
        addKeyName(KeyConstants.KEY_F10, "F10")
        addKeyName(KeyConstants.KEY_F11, "F11")
        addKeyName(KeyConstants.KEY_F12, "F12")
        addKeyName(KeyConstants.KEY_TAB, "Tab")
        addKeyName(KeyConstants.KEY_ADD, "Add")
        addKeyName(KeyConstants.KEY_SUBTRACT, "Subtract")
        addKeyName(KeyConstants.KEY_MULTIPLY, "Multiply")
        addKeyName(KeyConstants.KEY_DIVIDE, "Divide")
        addKeyName(KeyConstants.KEY_PAD_DIVIDE, "PAD_DIVIDE")
        addKeyName(KeyConstants.KEY_PAD_LEFT, "PAD_LEFT")
        addKeyName(KeyConstants.KEY_PAD_RIGHT, "PAD_RIGHT")
        addKeyName(KeyConstants.KEY_PAD_DOWN, "PAD_DOWN")
        addKeyName(KeyConstants.KEY_PAD_UP, "PAD_UP")
        addKeyName(KeyConstants.KEY_PAD_HOME, "PAD_HOME")
        addKeyName(KeyConstants.KEY_PAD_END, "PAD_END")
        addKeyName(KeyConstants.KEY_PAD_PGUP, "PAD_PGUP")
        addKeyName(KeyConstants.KEY_PAD_PGDN, "PAD_PGDN")
        addKeyName(KeyConstants.KEY_PAD_CENTER, "PAD_CENTER")
        addKeyName(KeyConstants.KEY_PAD_INS, "PAD_INS")
        addKeyName(KeyConstants.KEY_PAD_DEL, "PAD_DEL")
        addKeyName(KeyConstants.KEY_PAD_RETURN, "PAD_Enter")
        addKeyName(KeyConstants.KEY_BUTTON0, "PAD_BUTTON0")
        addKeyName(KeyConstants.KEY_BUTTON1, "PAD_BUTTON1")
        addKeyName(KeyConstants.KEY_BUTTON2, "PAD_BUTTON2")
        addKeyName(KeyConstants.KEY_BUTTON3, "PAD_BUTTON3")
        addKeyName(KeyConstants.KEY_BUTTON4, "PAD_BUTTON4")
        addKeyName(KeyConstants.KEY_BUTTON5, "PAD_BUTTON5")
        addKeyName(KeyConstants.KEY_BUTTON6, "PAD_BUTTON6")
        addKeyName(KeyConstants.KEY_BUTTON7, "PAD_BUTTON7")
        addKeyName(KeyConstants.KEY_BUTTON8, "PAD_BUTTON8")
        addKeyName(KeyConstants.KEY_BUTTON9, "PAD_BUTTON9")
        addKeyName(KeyConstants.KEY_BUTTON10, "PAD_BUTTON10")
        addKeyName(KeyConstants.KEY_BUTTON11, "PAD_BUTTON11")
        addKeyName(KeyConstants.KEY_BUTTON12, "PAD_BUTTON12")
        addKeyName(KeyConstants.KEY_BUTTON13, "PAD_BUTTON13")
        addKeyName(KeyConstants.KEY_BUTTON14, "PAD_BUTTON14")
        addKeyName(KeyConstants.KEY_BUTTON15, "PAD_BUTTON15")
        addKeyName(KeyConstants.KEY_BACKSPACE, "Backsp")
        addKeyName(KeyConstants.KEY_DELETE, "Del")
        addKeyName(KeyConstants.KEY_SHIFT, "Shift")
        addKeyName(KeyConstants.KEY_CONTROL, "Ctrl")
        addKeyName(KeyConstants.KEY_ALT, "Alt")
        addKeyName(KeyConstants.KEY_HYPHEN, "-")
        addKeyName(KeyConstants.KEY_EQUALS, "=")
        addKeyName(KeyConstants.KEY_INSERT, "Ins")
        addKeyName(KeyConstants.KEY_CAPSLOCK, "CapsLock")
    }

    fun setCallbacks(cbs: LLWindowCallbacks) { mCallbacks = cbs }

    fun getCurKeyElapsedTime(): Float =
        if (getKeyDown(mCurScanKey)) getKeyElapsedTime(mCurScanKey) else 0f

    fun getCurKeyElapsedFrameCount(): Float =
        if (getKeyDown(mCurScanKey)) getKeyElapsedFrameCount(mCurScanKey).toFloat() else 0f

    fun getKeyDown(key: KEY): Boolean = mKeyLevel[key.toInt()]
    fun getKeyRepeated(key: KEY): Boolean = mKeyRepeated[key.toInt()]

    fun translateKey(osKey: NativeKeyType, outKey: UByteArray): Boolean {
        val found = mTranslateKeyMap[osKey]
        return if (found != null) {
            outKey[0] = found
            true
        } else {
            outKey[0] = 0u
            false
        }
    }

    fun inverseTranslateKey(translatedKey: KEY): NativeKeyType =
        mInvTranslateKeyMap[translatedKey] ?: 0u

    fun handleTranslatedKeyDown(translatedKey: KEY, translatedMask: MASK): Boolean {
        val idx = translatedKey.toInt()
        val repeated: Boolean
        if (!mKeyLevel[idx]) {
            mKeyLevel[idx] = true
            mKeyLevelTimer[idx] = System.nanoTime()
            mKeyLevelFrameCount[idx] = 0
            mKeyRepeated[idx] = false
            repeated = false
        } else {
            repeated = true
            mKeyRepeated[idx] = true
        }
        mKeyDown[idx] = true
        mCurTranslatedKey = translatedKey
        return mCallbacks?.handleTranslatedKeyDown(translatedKey, translatedMask, repeated) ?: false
    }

    fun handleTranslatedKeyUp(translatedKey: KEY, translatedMask: MASK): Boolean {
        val idx = translatedKey.toInt()
        if (!mKeyLevel[idx]) return false
        mKeyLevel[idx] = false
        mKeyUp[idx] = true
        return mCallbacks?.handleTranslatedKeyUp(translatedKey, translatedMask) ?: false
    }

    abstract fun handleKeyUp(key: NativeKeyType, mask: MASK): Boolean
    abstract fun handleKeyDown(key: NativeKeyType, mask: MASK): Boolean

    abstract fun resetMaskKeys()
    abstract fun scanKeyboard()
    abstract fun currentMask(forMouseEvent: Boolean): MASK
    open fun currentKey(): KEY = mCurTranslatedKey

    fun getInsertMode(): EKeyboardInsertMode = mInsertMode

    fun toggleInsertMode() {
        mInsertMode = if (mInsertMode == EKeyboardInsertMode.LL_KIM_INSERT)
            EKeyboardInsertMode.LL_KIM_OVERWRITE
        else
            EKeyboardInsertMode.LL_KIM_INSERT
    }

    fun getKeyElapsedTime(key: KEY): Float {
        val startNanos = mKeyLevelTimer[key.toInt()]
        if (startNanos == 0L) return 0f
        return (System.nanoTime() - startNanos) / 1_000_000_000f
    }

    fun getKeyElapsedFrameCount(key: KEY): Int = mKeyLevelFrameCount[key.toInt()]

    fun resetKeyDownAndHandle() {
        val mask = currentMask(false)
        for (i in 0 until KeyConstants.KEY_COUNT) {
            if (mKeyLevel[i]) {
                mKeyDown[i] = false
                mKeyLevel[i] = false
                mKeyUp[i] = true
                mCurTranslatedKey = i.toUByte()
                mCallbacks?.handleTranslatedKeyUp(i.toUByte(), mask)
            }
        }
    }

    fun resetKeys() {
        for (i in 0 until KeyConstants.KEY_COUNT) mKeyLevel[i] = false
        for (i in 0 until KeyConstants.KEY_COUNT) mKeyUp[i] = false
        for (i in 0 until KeyConstants.KEY_COUNT) mKeyDown[i] = false
        for (i in 0 until KeyConstants.KEY_COUNT) mKeyRepeated[i] = false
    }

    protected fun addKeyName(key: KEY, name: String) {
        sKeysToNames[key] = name
        sNamesToKeys[name.uppercase()] = key
    }

    companion object {
        private val sKeysToNames: MutableMap<KEY, String> = mutableMapOf()
        private val sNamesToKeys: MutableMap<String, KEY> = mutableMapOf()
        var mStringTranslator: ((String) -> String)? = null

        fun setStringTranslatorFunc(transFunc: (String) -> String) {
            mStringTranslator = transFunc
        }

        fun keyFromString(str: String, keyOut: UByteArray): Boolean {
            if (str.isEmpty()) return false
            if (str.length == 1) {
                val ch = str[0].uppercaseChar()
                if (ch in '0'..'9' || ch in 'A'..'Z' ||
                    ch in '!'..'/' || ch in ':'..'@' ||
                    ch in '['..'`' || ch in '{'..'~'
                ) {
                    keyOut[0] = ch.code.toUByte()
                    return true
                }
            }
            val found = sNamesToKeys[str.uppercase()]
            if (found != null) {
                keyOut[0] = found
                return true
            }
            return false
        }

        fun stringFromKey(key: KEY, translate: Boolean = true): String {
            var res = sKeysToNames[key] ?: key.toInt().toChar().toString()
            if (translate) {
                res = mStringTranslator?.invoke(res) ?: res
            }
            return res
        }

        fun stringFromMouse(click: EMouseClickType, translate: Boolean = true): String {
            var res = when (click) {
                EMouseClickType.CLICK_LEFT       -> "LMB"
                EMouseClickType.CLICK_MIDDLE     -> "MMB"
                EMouseClickType.CLICK_RIGHT      -> "RMB"
                EMouseClickType.CLICK_BUTTON4    -> "MB4"
                EMouseClickType.CLICK_BUTTON5    -> "MB5"
                EMouseClickType.CLICK_DOUBLELEFT -> "Double LMB"
                else                             -> ""
            }
            if (translate && res.isNotEmpty()) {
                res = mStringTranslator?.invoke(res) ?: res
            }
            return res
        }

        fun stringFromAccelerator(accelMask: MASK): String {
            val trans = mStringTranslator ?: return ""
            val sb = StringBuilder()
            if (accelMask and KeyConstants.MASK_CONTROL != 0u) sb.append(trans("accel-win-control"))
            if (accelMask and KeyConstants.MASK_ALT != 0u) sb.append(trans("accel-win-alt"))
            if (accelMask and KeyConstants.MASK_SHIFT != 0u) sb.append(trans("accel-win-shift"))
            return sb.toString()
        }

        fun stringFromAccelerator(accelMask: MASK, key: KEY): String {
            if (key == KeyConstants.KEY_NONE) return ""
            val sb = StringBuilder(stringFromAccelerator(accelMask))
            val keyStr = stringFromKey(key)
            if ((accelMask and KeyConstants.MASK_NORMALKEYS) != 0u &&
                keyStr.isNotEmpty() && keyStr[0] in listOf('-', '=', '+')
            ) {
                sb.append(' ')
            }
            sb.append(keyStr)
            return sb.toString()
        }

        fun stringFromAccelerator(accelMask: MASK, click: EMouseClickType): String {
            if (click == EMouseClickType.CLICK_NONE) return ""
            return stringFromAccelerator(accelMask) + stringFromMouse(click)
        }

        fun maskFromString(str: String, maskOut: UIntArray): Boolean {
            maskOut[0] = when (str) {
                "NONE"          -> KeyConstants.MASK_NONE
                "SHIFT"         -> KeyConstants.MASK_SHIFT
                "CTL"           -> KeyConstants.MASK_CONTROL
                "ALT"           -> KeyConstants.MASK_ALT
                "CTL_SHIFT"     -> KeyConstants.MASK_CONTROL or KeyConstants.MASK_SHIFT
                "ALT_SHIFT"     -> KeyConstants.MASK_ALT or KeyConstants.MASK_SHIFT
                "CTL_ALT"       -> KeyConstants.MASK_CONTROL or KeyConstants.MASK_ALT
                "CTL_ALT_SHIFT" -> KeyConstants.MASK_CONTROL or KeyConstants.MASK_ALT or KeyConstants.MASK_SHIFT
                else            -> return false
            }
            return true
        }
    }
}

interface LLKeyBindingToStringHandler {
    fun getKeyBindingAsString(mode: String, control: String): String
}

var gKeyboard: LLKeyboard? = null
