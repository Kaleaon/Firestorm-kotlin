package com.firestorm.newview

/**
 * Small "Go to line" floater for the LSL script editor.
 *
 * Only one instance exists at a time (tracked via [sInstance]).  The floater
 * attaches itself as a dependent of whichever top-level floater contains the
 * supplied [LLScriptEdCore], so it moves and closes with that parent.
 */
class LLFloaterGotoLine(
    private val mEditorCore: LLScriptEdCore?
) : LLFloater(LLSD()) {

    var mGotoBox: LLLineEditor? = null

    companion object {
        var sInstance: LLFloaterGotoLine? = null
            private set

        fun show(editorCore: LLScriptEdCore?) {
            val existing = sInstance
            if (existing != null && existing.mEditorCore != null && existing.mEditorCore != editorCore) {
                existing.closeFloater()
            }

            if (sInstance == null) {
                LLFloaterGotoLine(editorCore)  // constructor sets sInstance
            }

            sInstance?.openFloater()
        }

        fun onBtnGoto(userdata: Any?) {
            (userdata as? LLFloaterGotoLine)?.handleBtnGoto()
        }
    }

    init {
        buildFromFile("floater_goto_line.xml")
        sInstance = this

        // Attach as a dependent floater of the editor's parent top-level window
        var viewp: LLView? = mEditorCore as? LLView
        while (viewp != null) {
            val floaterp = viewp as? LLFloater
            if (floaterp != null) {
                floaterp.addDependentFloater(this)
                break
            }
            viewp = viewp.getParent()
        }
    }

    override fun postBuild(): Boolean {
        mGotoBox = getChild<LLLineEditor>("goto_line")
        mGotoBox?.setCommitCallback { _, _ -> onGotoBoxCommit() }
        mGotoBox?.setCommitOnFocusLost(false)
        getChild<LLLineEditor>("goto_line")?.setPrevalidate(LLTextValidate::validateNonNegativeS32)
        childSetAction("goto_btn", ::onBtnGoto, this)
        setDefaultBtn("goto_btn")
        return true
    }

    override fun onClose(appQuitting: Boolean) {
        sInstance = null
        super.onClose(appQuitting)
    }

    fun handleBtnGoto() {
        val row = getChild<LLUICtrl>("goto_line")?.getValue()?.asInteger() ?: return
        if (row < 0) return
        val column = 0
        val editor = mEditorCore?.mCurrentEditor ?: return
        editor.deselect()
        editor.setCursor(row, column)
        editor.setFocus(true)
    }

    override fun hasAccelerators(): Boolean = mEditorCore?.hasAccelerators() ?: false

    override fun handleKeyHere(key: Int, mask: Int): Boolean = mEditorCore?.handleKeyHere(key, mask) ?: false

    private fun onGotoBoxCommit() {
        val row = getChild<LLUICtrl>("goto_line")?.getValue()?.asInteger() ?: return
        if (row < 0) return
        val column = 0
        val editor = mEditorCore?.mCurrentEditor ?: return
        editor.setCursor(row, column)

        var rownew    = 0
        var columnnew = 0
        editor.getCurrentLineAndColumn(intArrayOf(rownew), intArrayOf(columnnew), false)

        // Only close if the cursor actually landed on the requested line; otherwise
        // the user likely typed a line number beyond the end of the file – leave
        // the floater open so they can correct the value.
        if (rownew == row && columnnew == column) {
            editor.deselect()
            editor.setFocus(true)
            sInstance?.closeFloater()
        }
    }
}
