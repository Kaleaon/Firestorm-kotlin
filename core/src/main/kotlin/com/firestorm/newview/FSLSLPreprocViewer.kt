package com.firestorm.newview

open class LLScriptEditor(params: FSLSLPreProcViewer.Params) {
    open fun handleNavigationKey(key: Int, mask: Int): Boolean = false
    open fun handleSelectionKey(key: Int, mask: Int): Boolean = false
    open fun handleControlKey(key: Int, mask: Int): Boolean = false
    open fun selectAll() {}
    open fun resetCursorBlink() {}
    open fun needsScroll() {}
}

class FSLSLPreProcViewer(params: Params) : LLScriptEditor(params) {

    class Params

    override fun handleNavigationKey(key: Int, mask: Int): Boolean = super.handleNavigationKey(key, mask)
    override fun handleSelectionKey(key: Int, mask: Int): Boolean = super.handleSelectionKey(key, mask)
    override fun handleControlKey(key: Int, mask: Int): Boolean = super.handleControlKey(key, mask)

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        // Ctrl/Cmd+A → select all; all other editing keys are suppressed (read-only view)
        if (key == 'A'.code && (mask and MASK_MODIFIERS) == MASK_CONTROL) {
            selectAll()
            return true
        }

        val handled = handleNavigationKey(key, mask)
                || handleSelectionKey(key, mask)
                || handleControlKey(key, mask)

        if (handled) {
            resetCursorBlink()
            needsScroll()
        }

        return handled
    }

    fun handleUnicodeCharHere(@Suppress("UNUSED_PARAMETER") uniChar: Int): Boolean = false

    fun canCut(): Boolean = false
    fun canPaste(): Boolean = false
    fun canUndo(): Boolean = false
    fun canRedo(): Boolean = false
    fun canPastePrimary(): Boolean = false
    fun canDoDelete(): Boolean = false

    companion object {
        const val MASK_CONTROL = 0x0001
        const val MASK_MODIFIERS = 0x000F
    }
}
