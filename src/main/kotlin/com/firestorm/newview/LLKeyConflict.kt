package com.firestorm.newview

private const val FILENAME_DEFAULT = "key_bindings.xml"
private const val FILENAME_TEMPORARY = "key_bindings_tmp.xml"

private val SAVED_SETTINGS_KEY_CONTROLS: List<String> = listOf("placeholder")

fun stringFromMask(mask: UInt): String {
    val maskControl: UInt = 0x1u
    val maskAlt: UInt = 0x2u
    val maskShift: UInt = 0x4u
    val maskNone: UInt = 0x0u

    val parts = mutableListOf<String>()
    if ((mask and maskControl) != 0u) parts.add("CTL")
    if ((mask and maskAlt) != 0u) parts.add("ALT")
    if ((mask and maskShift) != 0u) parts.add("SHIFT")
    return if (mask == maskNone) "NONE" else parts.joinToString("_")
}

data class LLKeyData(
    val mouse: Int = 0,
    val key: Int = 0,
    val mask: UInt = 0u,
    val ignoreMask: Boolean = false
)

class LLKeyBind {
    private val keyDataList: MutableList<LLKeyData> = mutableListOf()

    fun getKeyData(index: Int): LLKeyData = keyDataList.getOrElse(index) { LLKeyData() }
    fun replaceKeyData(data: LLKeyData, index: Int) {
        while (keyDataList.size <= index) keyDataList.add(LLKeyData())
        keyDataList[index] = data
    }
    fun resetKeyData(index: Int) { replaceKeyData(LLKeyData(), index) }
    fun addKeyData(mouse: Int, key: Int, mask: UInt, ignoreMask: Boolean) {
        keyDataList.add(LLKeyData(mouse, key, mask, ignoreMask))
    }
    fun getDataCount(): Int = keyDataList.size
    fun isEmpty(): Boolean = keyDataList.all { it == LLKeyData() }
    fun trimEmpty() {
        while (keyDataList.isNotEmpty() && keyDataList.last() == LLKeyData()) {
            keyDataList.removeAt(keyDataList.size - 1)
        }
    }
    fun clear() { keyDataList.clear() }
    fun findKeyData(data: LLKeyData): Int = keyDataList.indexOfFirst { it == data }
    fun canHandle(mouse: Int, key: Int, mask: UInt): Boolean =
        keyDataList.any { it.mouse == mouse && it.key == key && it.mask == mask }
}

class LLKeyConflict(
    var mKeyBind: LLKeyBind = LLKeyBind(),
    var mAssignable: Boolean = true,
    var mConflictMask: UInt = UInt.MAX_VALUE
) {
    constructor(assignable: Boolean, conflictMask: UInt) : this(LLKeyBind(), assignable, conflictMask)
    constructor(bind: LLKeyBind, assignable: Boolean, conflictMask: UInt) : this(bind, assignable, conflictMask)

    fun getPrimaryKeyData(): LLKeyData = mKeyBind.getKeyData(0)
    fun getKeyData(index: Int): LLKeyData = mKeyBind.getKeyData(index)
    fun setPrimaryKeyData(data: LLKeyData) { mKeyBind.replaceKeyData(data, 0) }
    fun setKeyData(data: LLKeyData, index: Int) { mKeyBind.replaceKeyData(data, index) }
    fun canHandle(mouse: Int, key: Int, mask: UInt): Boolean = mKeyBind.canHandle(mouse, key, mask)
}

class LLKeyConflictHandler {

    enum class ESourceMode {
        MODE_FIRST_PERSON,
        MODE_THIRD_PERSON,
        MODE_EDIT_AVATAR,
        MODE_SITTING,
        MODE_SAVED_SETTINGS,
        MODE_COUNT
    }

    val CONFLICT_NOTHING: UInt = 0u
    val CONFLICT_LMOUSE: UInt = 0x1u shl 1
    val CONFLICT_ANY: UInt = UInt.MAX_VALUE

    private val mControlsMap: MutableMap<String, LLKeyConflict> = mutableMapOf()
    private val mDefaultsMap: MutableMap<String, LLKeyConflict> = mutableMapOf()
    private var mHasUnsavedChanges: Boolean = false
    private var mLoadMode: ESourceMode = ESourceMode.MODE_COUNT
    private var mUsesTemporaryFile: Boolean = false

    companion object {
        private var sTemporaryFileUseCount: Int = 0

        fun isReservedByMenu(key: Int, mask: UInt): Boolean {
            if (key == 0) return false
            TODO("APR: use JVM equivalent - check gMenuBarView for accelerator key+mask")
        }

        fun isReservedByMenu(data: LLKeyData): Boolean {
            if (data.mouse != 0 || data.key == 0) return false
            TODO("APR: use JVM equivalent - check gMenuBarView for accelerator data.key+data.mask")
        }

        fun getStringFromKeyData(keydata: LLKeyData): String {
            TODO("APR: use JVM equivalent - build human-readable string from key+mask+mouse using LLKeyboard string helpers")
        }

        fun resetKeyboardBindings() {
            TODO("APR: use JVM equivalent - load key_bindings.xml from user settings or fall back to app settings and apply to gViewerInput")
        }

        private fun clearTemporaryFile(): Boolean {
            TODO("APR: use JVM equivalent - delete key_bindings_tmp.xml from user settings if it exists; return true if deleted")
        }
    }

    constructor()

    constructor(mode: ESourceMode) {
        mLoadMode = mode
        loadFromSettings(mode)
    }

    fun canHandleControl(controlName: String, mouseInd: Int, key: Int, mask: UInt): Boolean =
        mControlsMap[controlName]?.canHandle(mouseInd, key, mask) ?: false

    fun canHandleKey(controlName: String, key: Int, mask: UInt): Boolean =
        canHandleControl(controlName, 0, key, mask)

    fun canHandleMouse(controlName: String, mouseInd: Int, mask: UInt): Boolean =
        canHandleControl(controlName, mouseInd, 0, mask)

    fun canAssignControl(controlName: String): Boolean =
        mControlsMap[controlName]?.mAssignable ?: true

    fun registerControl(controlName: String, dataIndex: UInt, mouse: Int, key: Int, mask: UInt, ignoreMask: Boolean): Boolean {
        if (controlName.isEmpty()) return false
        val typeData = mControlsMap.getOrPut(controlName) { LLKeyConflict() }
        if (!typeData.mAssignable) return false
        val data = LLKeyData(mouse, key, mask, ignoreMask)
        if (typeData.mKeyBind.getKeyData(dataIndex.toInt()) == data) return true
        if (isReservedByMenu(data)) return false
        if (removeConflicts(data, typeData.mConflictMask)) {
            typeData.mKeyBind.replaceKeyData(data, dataIndex.toInt())
            mHasUnsavedChanges = true
            return true
        }
        return false
    }

    fun clearControl(controlName: String, dataIndex: UInt): Boolean {
        if (controlName.isEmpty()) return false
        val typeData = mControlsMap[controlName] ?: return false
        if (!typeData.mAssignable) return false
        typeData.mKeyBind.resetKeyData(dataIndex.toInt())
        mHasUnsavedChanges = true
        return true
    }

    fun getControl(controlName: String, index: UInt): LLKeyData {
        if (controlName.isEmpty()) return LLKeyData()
        return mControlsMap.getOrPut(controlName) { LLKeyConflict() }.getKeyData(index.toInt())
    }

    fun isControlEmpty(controlName: String): Boolean {
        if (controlName.isEmpty()) return true
        return mControlsMap[controlName]?.mKeyBind?.isEmpty() ?: true
    }

    fun getControlString(controlName: String, index: UInt): String {
        if (controlName.isEmpty()) return ""
        return getStringFromKeyData(mControlsMap.getOrPut(controlName) { LLKeyConflict() }.getKeyData(index.toInt()))
    }

    fun loadFromControlSettings(name: String) {
        TODO("APR: use JVM equivalent - read LLKeyBind from gSavedSettings control 'name' and store in mControlsMap")
    }

    fun loadFromSettings(loadMode: ESourceMode) {
        mControlsMap.clear()
        mDefaultsMap.clear()
        generatePlaceholders(loadMode)

        if (loadMode == ESourceMode.MODE_SAVED_SETTINGS) {
            for (name in SAVED_SETTINGS_KEY_CONTROLS) {
                loadFromControlSettings(name)
            }
        } else {
            TODO("APR: use JVM equivalent - parse app-settings key_bindings.xml into mDefaultsMap, then user-settings key_bindings.xml into mControlsMap; merge defaults into mControlsMap for any missing entries")
        }
        mLoadMode = loadMode
    }

    fun saveToSettings(applyTemporary: Boolean = false) {
        if (mControlsMap.isEmpty()) return

        if (mLoadMode == ESourceMode.MODE_SAVED_SETTINGS) {
            for ((name, key) in mControlsMap) {
                if (name.isEmpty() || !key.mAssignable) continue
                key.mKeyBind.trimEmpty()
                TODO("APR: use JVM equivalent - persist key.mKeyBind to gSavedSettings control '$name'")
            }
        } else {
            TODO("APR: use JVM equivalent - read existing key_bindings.xml, replace the relevant mode's bindings with mControlsMap entries, write back to user settings (or tmp file if applyTemporary), then reload gViewerInput bindings")
        }

        if (mLoadMode == ESourceMode.MODE_THIRD_PERSON && mHasUnsavedChanges) {
            val value = canHandleMouse("teleport_to", 3 /* CLICK_DOUBLELEFT */, 0u)
            TODO("APR: use JVM equivalent - gSavedSettings.setBOOL(\"DoubleClickTeleport\", value)")
        }

        if (!applyTemporary) {
            clearUnsavedChanges()
        }
    }

    fun getDefaultControl(controlName: String, index: UInt): LLKeyData {
        if (controlName.isEmpty()) return LLKeyData()
        if (mLoadMode == ESourceMode.MODE_SAVED_SETTINGS) {
            TODO("APR: use JVM equivalent - return LLKeyBind(gSavedSettings.getControl(controlName).default).getKeyData(index)")
        }
        return mDefaultsMap[controlName]?.mKeyBind?.getKeyData(index.toInt()) ?: LLKeyData()
    }

    fun resetToDefault(controlName: String, index: UInt) {
        if (controlName.isEmpty()) return
        val typeData = mControlsMap.getOrPut(controlName) { LLKeyConflict() }
        if (!typeData.mAssignable) return
        val data = getDefaultControl(controlName, index)
        if (data != typeData.getKeyData(index.toInt())) {
            removeConflicts(data, mControlsMap[controlName]!!.mConflictMask)
            mControlsMap[controlName]!!.setKeyData(data, index.toInt())
            mHasUnsavedChanges = true
        }
    }

    fun resetToDefault(controlName: String) {
        resetToDefaultAndResolve(controlName, ignoreConflicts = false)
    }

    fun resetToDefaults() {
        if (!empty()) {
            resetToDefaultsAndResolve()
        } else {
            loadFromSettings(mLoadMode)
            resetToDefaultsAndResolve()
        }
    }

    fun empty(): Boolean = mControlsMap.isEmpty()

    fun clear() {
        if (clearUnsavedChanges()) {
            resetKeyboardBindings()
        }
        mControlsMap.clear()
        mDefaultsMap.clear()
    }

    fun hasUnsavedChanges(): Boolean = mHasUnsavedChanges
    fun setLoadMode(mode: ESourceMode) { mLoadMode = mode }
    fun getLoadMode(): ESourceMode = mLoadMode

    private fun resetToDefaultAndResolve(controlName: String, ignoreConflicts: Boolean) {
        if (controlName.isEmpty()) return
        if (mLoadMode == ESourceMode.MODE_SAVED_SETTINGS) {
            TODO("APR: use JVM equivalent - load default LLKeyBind from gSavedSettings, optionally remove conflicts, store in mControlsMap")
        } else {
            val defaultEntry = mDefaultsMap[controlName]
            if (defaultEntry != null) {
                if (!ignoreConflicts) {
                    for (i in 0 until defaultEntry.mKeyBind.getDataCount()) {
                        removeConflicts(defaultEntry.mKeyBind.getKeyData(i), mControlsMap[controlName]?.mConflictMask ?: UInt.MAX_VALUE)
                    }
                }
                mControlsMap.getOrPut(controlName) { LLKeyConflict() }.mKeyBind = defaultEntry.mKeyBind
            } else {
                mControlsMap[controlName]?.mKeyBind?.clear()
            }
        }
    }

    private fun resetToDefaultsAndResolve() {
        if (mLoadMode == ESourceMode.MODE_SAVED_SETTINGS) {
            for (name in mControlsMap.keys.toList()) {
                resetToDefaultAndResolve(name, ignoreConflicts = true)
            }
        } else {
            mControlsMap.clear()
            mControlsMap.putAll(mDefaultsMap)
            generatePlaceholders(mLoadMode)
        }
        mHasUnsavedChanges = true
    }

    private fun registerTemporaryControl(controlName: String, mouse: Int, key: Int, mask: UInt, conflictMask: UInt) {
        val typeData = mControlsMap.getOrPut(controlName) { LLKeyConflict() }
        typeData.mAssignable = false
        typeData.mConflictMask = conflictMask
        typeData.mKeyBind.addKeyData(mouse, key, mask, false)
    }

    private fun registerTemporaryControl(controlName: String, conflictMask: UInt = 0u) {
        val typeData = mControlsMap.getOrPut(controlName) { LLKeyConflict() }
        typeData.mAssignable = false
        typeData.mConflictMask = conflictMask
    }

    private fun generatePlaceholders(loadMode: ESourceMode) {
        if (loadMode == ESourceMode.MODE_FIRST_PERSON) {
            for (name in listOf(
                "look_up", "look_down", "move_forward", "move_backward",
                "move_forward_fast", "move_backward_fast", "spin_over", "spin_under",
                "pan_up", "pan_down", "pan_left", "pan_right", "pan_in", "pan_out",
                "spin_around_ccw", "spin_around_cw",
                "roll_left", "roll_right",
                "edit_avatar_spin_ccw", "edit_avatar_spin_cw",
                "edit_avatar_spin_over", "edit_avatar_spin_under",
                "edit_avatar_move_forward", "edit_avatar_move_backward",
                "walk_to", "teleport_to"
            )) {
                registerTemporaryControl(name)
            }
        }

        if (loadMode == ESourceMode.MODE_EDIT_AVATAR) {
            registerTemporaryControl("walk_to")
            registerTemporaryControl("teleport_to")
        }

        if (loadMode == ESourceMode.MODE_SITTING) {
            registerTemporaryControl("walk_to")
        } else {
            for (name in listOf(
                "move_forward_sitting", "move_backward_sitting",
                "spin_over_sitting", "spin_under_sitting",
                "spin_around_ccw_sitting", "spin_around_cw_sitting"
            )) {
                registerTemporaryControl(name)
            }
        }

        val scriptMouseHandlerName = "script_mouse_handler"
        val typeData = mControlsMap.getOrPut(scriptMouseHandlerName) { LLKeyConflict() }
        typeData.mAssignable = true
        typeData.mConflictMask = UInt.MAX_VALUE - CONFLICT_LMOUSE
    }

    private fun removeConflicts(data: LLKeyData, conflictMask: UInt): Boolean {
        if (conflictMask == CONFLICT_NOTHING) return true

        var effectiveMask = conflictMask
        val clickLeft = 1
        val maskNone: UInt = 0u
        val keyNone = 0

        if (data.mouse == clickLeft && data.mask == maskNone && data.key == keyNone) {
            if ((conflictMask and CONFLICT_LMOUSE) == 0u) return true
            effectiveMask = CONFLICT_LMOUSE
        } else {
            effectiveMask = conflictMask and CONFLICT_LMOUSE.inv()
        }

        val conflictList = mutableMapOf<String, Int>()
        for ((name, conflict) in mControlsMap) {
            if ((conflict.mConflictMask and effectiveMask) == 0u) continue
            val index = conflict.mKeyBind.findKeyData(data)
            if (index >= 0) {
                if (conflict.mAssignable) {
                    conflictList[name] = index
                } else {
                    return false
                }
            }
        }

        for ((name, index) in conflictList) {
            mControlsMap[name]?.mKeyBind?.resetKeyData(index)
        }
        return true
    }

    private fun clearUnsavedChanges(): Boolean {
        mHasUnsavedChanges = false
        if (mUsesTemporaryFile) {
            mUsesTemporaryFile = false
            sTemporaryFileUseCount--
            if (sTemporaryFileUseCount == 0) {
                return clearTemporaryFile()
            }
        }
        return false
    }
}
