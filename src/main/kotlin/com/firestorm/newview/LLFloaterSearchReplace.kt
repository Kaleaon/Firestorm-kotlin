package com.firestorm.newview

class LLFloaterSearchReplace(sdKey: Any) : LLFloater(sdKey) {

    private var mSearchEditor: LLLineEditor? = null
    private var mReplaceEditor: LLLineEditor? = null
    private var mCaseInsensitiveCheck: LLCheckBoxCtrl? = null
    private var mSearchUpCheck: LLCheckBoxCtrl? = null

    // Nullable direct reference; cleared when the bound editor is destroyed
    private var mEditorHandle: LLTextEditor? = null

    open fun postBuild(): Boolean {
        mSearchEditor = getChild("search_text")
        mSearchEditor?.setCommitCallback { onSearchClick() }
        mSearchEditor?.setCommitOnFocusLost(false)
        mSearchEditor?.setKeystrokeCallback { refreshHighlight() }

        mReplaceEditor = getChild("replace_text")

        mCaseInsensitiveCheck = getChild("case_text")
        mCaseInsensitiveCheck?.setCommitCallback { refreshHighlight() }

        mSearchUpCheck = getChild("find_previous")

        val pSearchBtn = getChild<LLButton>("search_btn")
        pSearchBtn?.setCommitCallback { onSearchClick() }
        setDefaultBtn(pSearchBtn)

        getChild<LLButton>("replace_btn")?.setCommitCallback { onReplaceClick() }
        getChild<LLButton>("replace_all_btn")?.setCommitCallback { onReplaceAllClick() }

        return true
    }

    open fun onOpen(sdKey: Any) {
        val pEditor = getEditor()
        if (pEditor != null) {
            // canCopy() is used as a synonym for hasSelection() since hasSelection() is inaccessible
            if (pEditor.canCopy()) {
                mSearchEditor?.setText(pEditor.getSelectionString())
                mSearchEditor?.setCursorToEnd()
            }
            pEditor.setHighlightWord(mSearchEditor?.getText() ?: "", mCaseInsensitiveCheck?.get() ?: false)

            val editorWritable = !pEditor.getReadOnly()
            mReplaceEditor?.setEnabled(editorWritable)
            getChild<LLButton>("replace_btn")?.setEnabled(editorWritable)
            getChild<LLButton>("replace_all_btn")?.setEnabled(editorWritable)
        }
        mSearchEditor?.setFocus(true)
    }

    open fun onClose(fQuiting: Boolean) {
        getEditor()?.clearHighlights()
    }

    open fun hasAccelerators(): Boolean {
        var pView: LLViewBase? = mEditorHandle
        while (pView != null) {
            if (pView.hasAccelerators()) return true
            pView = pView.getParent()
        }
        return false
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        // Pass unhandled keys up through the bound editor hierarchy so Ctrl-F still works when this floater has focus
        var handled = super_handleKeyHere(key, mask)
        if (!handled) {
            if (gFocusMgr.childHasKeyboardFocus(this)) {
                val pEditView = LLEditMenuHandler.gEditMenuHandler as? LLViewBase
                if (pEditView != null && pEditView.hasAncestor(this) && gEditMenu.handleAcceleratorKey(key, mask)) {
                    return true
                }
            }
            var pView: LLViewBase? = mEditorHandle
            while (pView != null) {
                if (pView.hasAccelerators() && pView.handleKeyHere(key, mask)) return true
                pView = pView.getParent()
            }
        }
        return handled
    }

    fun setCanReplace(canReplace: Boolean) {
        mReplaceEditor?.setEnabled(canReplace)
        getChild<LLButton>("replace_btn")?.setEnabled(canReplace)
        getChild<LLButton>("replace_all_btn")?.setEnabled(canReplace)
    }

    fun getEditor(): LLTextEditor? = mEditorHandle

    protected fun refreshHighlight() {
        getEditor()?.setHighlightWord(mSearchEditor?.getText() ?: "", mCaseInsensitiveCheck?.get() ?: false)
    }

    protected fun onSearchClick() {
        getEditor()?.selectNext(
            search = mSearchEditor?.getText() ?: "",
            caseInsensitive = mCaseInsensitiveCheck?.get() ?: false,
            wrap = true,
            searchUp = mSearchUpCheck?.get() ?: false
        )
    }

    protected fun onReplaceClick() {
        getEditor()?.replaceText(
            search = mSearchEditor?.getText() ?: "",
            replace = mReplaceEditor?.getText() ?: "",
            caseInsensitive = mCaseInsensitiveCheck?.get() ?: false,
            wrap = true,
            searchUp = mSearchUpCheck?.get() ?: false
        )
    }

    protected fun onReplaceAllClick() {
        getEditor()?.replaceTextAll(
            search = mSearchEditor?.getText() ?: "",
            replace = mReplaceEditor?.getText() ?: "",
            caseInsensitive = mCaseInsensitiveCheck?.get() ?: false
        )
    }

    companion object {
        fun show(pEditor: LLTextEditor?): LLFloaterSearchReplace? {
            val pSelf = LLFloaterReg.findTypedInstance<LLFloaterSearchReplace>("search_replace")
            if (pSelf == null || pEditor == null) return null

            val pDependeeOld = pSelf.getDependee()
            var pView: LLViewBase? = pEditor.getParent()
            while (pView != null) {
                val candidate = pView as? LLFloater
                if (candidate != null) {
                    if (candidate != pDependeeOld) {
                        if (pDependeeOld != null) {
                            pSelf.getEditor()?.clearHighlights()
                            pDependeeOld.removeDependentFloater(pSelf)
                        }
                        val host = candidate.getHost()
                        if (host == null) {
                            candidate.addDependentFloater(pSelf)
                        } else {
                            host.addDependentFloater(pSelf)
                        }
                    }
                    break
                }
                pView = pView.getParent()
            }

            pSelf.mEditorHandle = pEditor
            pSelf.openFloater()
            return pSelf
        }

        fun findInstance(): LLFloaterSearchReplace? =
            LLFloaterReg.findTypedInstance("search_replace")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = null
    private fun setDefaultBtn(btn: LLButton?) { System.err.println("LLFloaterSearchReplace: set default button not yet implemented") }
    private fun getDependee(): LLFloater? = null
    private fun openFloater() { System.err.println("LLFloaterSearchReplace: openFloater not yet implemented") }
    private fun super_handleKeyHere(key: Int, mask: Int): Boolean = false
    private fun removeDependentFloater(floater: LLFloater) { System.err.println("LLFloaterSearchReplace: removeDependentFloater not yet implemented") }
    private fun addDependentFloater(floater: LLFloater) { System.err.println("LLFloaterSearchReplace: addDependentFloater not yet implemented") }
    private fun getHost(): LLFloater? = null
}

// ============================================================================
// Stubs not defined elsewhere in the package
// ============================================================================

open class LLViewBase {
    open fun getParent(): LLViewBase? = null
    open fun hasAccelerators(): Boolean = false
    fun hasAncestor(view: LLViewBase): Boolean = false
    fun handleKeyHere(key: Int, mask: Int): Boolean = false
}

open class LLTextEditor : LLViewBase() {
    fun canCopy(): Boolean = false
    fun getSelectionString(): String = ""
    fun getReadOnly(): Boolean = false
    fun clearHighlights() { System.err.println("LLTextEditor: clearHighlights not yet implemented") }
    fun setHighlightWord(word: String, caseInsensitive: Boolean) { System.err.println("LLTextEditor: setHighlightWord not yet implemented") }
    fun selectNext(search: String, caseInsensitive: Boolean, wrap: Boolean, searchUp: Boolean) { System.err.println("LLTextEditor: selectNext not yet implemented") }
    fun replaceText(search: String, replace: String, caseInsensitive: Boolean, wrap: Boolean, searchUp: Boolean) { System.err.println("LLTextEditor: replaceText not yet implemented") }
    fun replaceTextAll(search: String, replace: String, caseInsensitive: Boolean) { System.err.println("LLTextEditor: replaceTextAll not yet implemented") }
}

class LLCheckBoxCtrl : LLViewBase() {
    fun get(): Boolean = false
    fun setCommitCallback(cb: () -> Unit) { System.err.println("LLCheckBoxCtrl: setCommitCallback not yet implemented") }
}

object gFocusMgr {
    fun childHasKeyboardFocus(view: Any): Boolean = false
}

object LLEditMenuHandler {
    var gEditMenuHandler: Any? = null
}

object gEditMenu {
    fun handleAcceleratorKey(key: Int, mask: Int): Boolean = false
}
