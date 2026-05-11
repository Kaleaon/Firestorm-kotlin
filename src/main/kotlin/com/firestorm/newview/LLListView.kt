package com.firestorm.newview

// Stub UI base types.
open class LLUIColor(val value: FloatArray = floatArrayOf(0f, 0f, 0f, 1f))
open class LLUICtrl(val params: LLUICtrl.Params = LLUICtrl.Params()) {
    open class Params
    protected val children: MutableList<LLUICtrl> = mutableListOf()
    protected fun addChild(child: LLUICtrl) { children.add(child) }
}

open class LLTextBox(val params: Params = Params()) : LLUICtrl(params) {
    class Params : LLUICtrl.Params() {
        var rect: IntArray = intArrayOf(0, 0, 0, 0)
        var initialValue: String = ""
    }
    private var text: String = params.initialValue
    fun setValue(v: String) { text = v }
}

class LLListView(private val p: Params) : LLUICtrl(p) {

    class Params : LLUICtrl.Params() {
        var bgColor: LLUIColor = LLUIColor()
        var fgSelectedColor: LLUIColor = LLUIColor()
        var bgSelectedColor: LLUIColor = LLUIColor()
    }

    private val mLabel: LLTextBox
    private val mBgColor: LLUIColor = p.bgColor
    private val mFgSelectedColor: LLUIColor = p.fgSelectedColor
    private val mBgSelectedColor: LLUIColor = p.bgSelectedColor

    init {
        val textParams = LLTextBox.Params().apply {
            rect = intArrayOf(0, 20, 300, 0)
            initialValue = "This is a list-view"
        }
        mLabel = LLTextBox(textParams)
        addChild(mLabel)
    }

    fun setString(s: String) {
        mLabel.setValue(s)
    }
}
