package com.firestorm.newview

class LLFloaterSearchReplace(sdKey: Any) : LLFloater(sdKey) {

    private var mSearchEditor: LLLineEditor? = null
    private var mReplaceEditor: LLLineEditor? = null
    private var mCaseInsensitiveCheck: LLCheckBoxCtrl? = null
    private var mSearchUpCheck: LLCheckBoxCtrl? = null

    // Nullable handle; set when a text editor is bound to this floater
    private var mEditorHandle: LLTextEditor? = null

    override fun postBuild(): Boolean {
        mSearchEditor = getChild<LLLineEditor>("search_text")
        mSearchEditor?.setCommitCallback { onSearchClick() }
        mSearchEditor?.setCommitOnFocusLost(false)
        mSearchEditor?.setKeystrokeCallback { refreshHighlight() }

        mReplaceEditor = getChild<LLLineEditor>("replace_text")

        mCaseInsensitiveCheck = getChild<LLCheckBoxCtrl>("case_text")
        mCaseInsensitiveCheck?.setCommitCallback { refreshHighlight() }

        mSearchUpCheck = getChild<LLCheckBoxCtrl>("find_previous")

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
        var pView: LLView? = mEditorHandle
        while (pView != null) {
            if (pView.hasAccelerators()) return true
            pView = pView.getParent()
        }
        return false
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        // Pass keys to the bound editor hierarchy so that Ctrl-F works when this floater has focus
        var handled = super_handleKeyHere(key, mask)
        if (!handled) {
            if (gFocusMgr.childHasKeyboardFocus(this)) {
                val pEditView = LLEditMenuHandler.gEditMenuHandler as? LLView
                if (pEditView != null && pEditView.hasAncestor(this) && gEditMenu?.handleAcceleratorKey(key, mask) == true) {
                    return true
                }
            }

            var pView: LLView? = mEditorHandle
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
            mSearchEditor?.getText() ?: "",
            mCaseInsensitiveCheck?.get() ?: false,
            wrap = true,
            searchUp = mSearchUpCheck?.get() ?: false
        )
    }

    protected fun onReplaceClick() {
        getEditor()?.replaceText(
            mSearchEditor?.getText() ?: "",
            mReplaceEditor?.getText() ?: "",
            mCaseInsensitiveCheck?.get() ?: false,
            wrap = true,
            searchUp = mSearchUpCheck?.get() ?: false
        )
    }

    protected fun onReplaceAllClick() {
        getEditor()?.replaceTextAll(
            mSearchEditor?.getText() ?: "",
            mReplaceEditor?.getText() ?: "",
            mCaseInsensitiveCheck?.get() ?: false
        )
    }

    companion object {
        fun show(pEditor: LLTextEditor?): LLFloaterSearchReplace? {
            val pSelf = LLFloaterReg.findTypedInstance<LLFloaterSearchReplace>("search_replace")
            if (pSelf == null || pEditor == null) return null

            var pDependeeNew: LLFloater? = null
            val pDependeeOld = pSelf.getDependee()
            var pView: LLView? = pEditor.getParent()
            while (pView != null) {
                val candidate = pView as? LLFloater
                if (candidate != null) {
                    pDependeeNew = candidate
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
            LLFloaterReg.findTypedInstance<LLFloaterSearchReplace>("search_replace")
    }

    private fun <T> getChild(name: String): T? = TODO("UI: resolve child widget '$name'")
    private fun setDefaultBtn(btn: LLButton?) { TODO("UI: set default button") }
    private fun getDependee(): LLFloater? = TODO("UI: getDependee")
    private fun openFloater() { TODO("UI: openFloater") }
    private fun super_handleKeyHere(key: Int, mask: Int): Boolean = TODO("UI: LLFloater base handleKeyHere")
}

// ============================================================================
// Stub types for referenced UI / focus-manager APIs
//

open class LLView {
    fun getParent(): LLView? = TODO("UI: getParent")
    fun hasAccelerators(): Boolean = TODO("UI: hasAccelerators")
    fun hasAncestor(view: LLView): Boolean = TODO("UI: hasAncestor")
    fun handleKeyHere(key: Int, mask: Int): Boolean = TODO("UI: handleKeyHere")
}

open class LLTextEditor : LLView() {
    fun canCopy(): Boolean = TODO("UI: canCopy")
    fun getSelectionString(): String = TODO("UI: getSelectionString")
    fun getReadOnly(): Boolean = TODO("UI: getReadOnly")
    fun clearHighlights() { TODO("UI: clearHighlights") }
    fun setHighlightWord(word: String, caseInsensitive: Boolean) { TODO("UI: setHighlightWord") }
    fun selectNext(search: String, caseInsensitive: Boolean, wrap: Boolean, searchUp: Boolean) { TODO("UI: selectNext") }
    fun replaceText(search: String, replace: String, caseInsensitive: Boolean, wrap: Boolean, searchUp: Boolean) { TODO("UI: replaceText") }
    fun replaceTextAll(search: String, replace: String, caseInsensitive: Boolean) { TODO("UI: replaceTextAll") }
}

class LLCheckBoxCtrl : LLView() {
    fun get(): Boolean = TODO("UI: get checkbox value")
    fun setCommitCallback(cb: () -> Unit) { TODO("UI: setCommitCallback on checkbox") }
}

open class LLFloaterBase : LLView() {
    fun getDependee(): LLFloater? = TODO("UI: getDependee")
    fun removeDependentFloater(floater: LLFloater) { TODO("UI: removeDependentFloater") }
    fun addDependentFloater(floater: LLFloater) { TODO("UI: addDependentFloater") }
    fun getHost(): LLFloater? = TODO("UI: getHost")
}

object gFocusMgr {
    fun childHasKeyboardFocus(view: LLView): Boolean = TODO("UI: childHasKeyboardFocus")
}

object LLEditMenuHandler {
    var gEditMenuHandler: Any? = null
}

object gEditMenu {
    fun handleAcceleratorKey(key: Int, mask: Int): Boolean = TODO("UI: handleAcceleratorKey")
}
