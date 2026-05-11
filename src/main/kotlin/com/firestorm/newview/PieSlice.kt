package com.firestorm.newview

// A single clickable wedge in the pie menu. Stores the label and the callbacks
// to invoke on click, enable-check, and visibility-check.
class PieSlice(params: Params) : LLUICtrl(params), PieAutoHide(params.autohide, params.startAutohide) {

    class Params : LLUICtrl.Params() {
        var onClickCallbacks: MutableList<(LLUICtrl) -> Unit> = mutableListOf()
        var onEnableCallbacks: MutableList<(LLUICtrl) -> Boolean> = mutableListOf()
        var onVisibleCallbacks: MutableList<(LLUICtrl) -> Boolean> = mutableListOf()
        var startAutohide: Boolean = false
        var autohide: Boolean = false
        // When true the enable check runs only once, avoiding repeated queries
        var checkEnableOnce: Boolean = false

        init {
            name = "pie_slice"
        }
    }

    var label: String = params.label ?: ""
    private val checkEnableOnce: Boolean = params.checkEnableOnce
    private var doUpdateEnabled: Boolean = true

    private val enableSignal: MutableList<(LLUICtrl) -> Boolean> = mutableListOf()
    private val visibleSignal: MutableList<(LLUICtrl) -> Boolean> = mutableListOf()

    init {
        params.onClickCallbacks.forEach { addCommitCallback(it) }
        params.onVisibleCallbacks.forEach { visibleSignal.add(it) }
        params.onEnableCallbacks.forEach { enableSignal.add(it) }
    }

    fun getValue(): String = label
    fun setValue(value: String) { label = value }

    fun setClickCallback(cb: (LLUICtrl) -> Unit) = addCommitCallback(cb)
    fun setEnableCallback(cb: (LLUICtrl) -> Boolean) { enableSignal.add(cb) }

    fun updateEnabled() {
        if (doUpdateEnabled && enableSignal.isNotEmpty()) {
            // Last registered callback wins; mirrors how boost::signals2 accumulates booleans
            val enabled = enableSignal.fold(true) { _, cb -> cb(this) }
            setEnabled(enabled)
            doUpdateEnabled = !checkEnableOnce
        }
    }

    fun updateVisible() {
        if (visibleSignal.isNotEmpty()) {
            val visible = visibleSignal.fold(true) { _, cb -> cb(this) }
            setVisible(visible)
        }
    }

    fun resetUpdateEnabledCheck() {
        doUpdateEnabled = true
    }
}
