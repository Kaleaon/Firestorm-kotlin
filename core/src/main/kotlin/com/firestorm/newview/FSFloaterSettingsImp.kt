/**
 * FSFloaterSettingsImp.kt
 * Kotlin conversion of llfloatersettingsdebug.h / llfloatersettingsdebug.cpp
 * (no FS-specific fsfloatersettingsimp.* or fsfloatersiminfo.* exists;
 *  the nearest upstream equivalent is LLFloaterSettingsDebug, which Firestorm
 *  extends with non-default highlighting, sanity-check integration, and RLVa
 *  hide-from-editor support)
 *
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLSD

// ---------------------------------------------------------------------------
// Typed value union for a control variable — mirrors eControlType
// ---------------------------------------------------------------------------

sealed class ControlValue {
    data class Bool(val v: Boolean)     : ControlValue()
    data class IntVal(val v: Int)       : ControlValue()
    data class UIntVal(val v: UInt)     : ControlValue()
    data class FloatVal(val v: Float)   : ControlValue()
    data class StringVal(val v: String) : ControlValue()
    data class Vec3(val x: Float, val y: Float, val z: Float) : ControlValue()
    data class Vec3d(val x: Double, val y: Double, val z: Double) : ControlValue()
    data class Quat(val x: Float, val y: Float, val z: Float, val s: Float) : ControlValue()
    data class Rect(val left: Int, val right: Int, val bottom: Int, val top: Int) : ControlValue()
    data class Color4(val r: Float, val g: Float, val b: Float, val a: Float) : ControlValue()
    data class Color3(val r: Float, val g: Float, val b: Float) : ControlValue()
    data class RawLLSD(val sd: LLSD)    : ControlValue()
    object Unknown                      : ControlValue()
}

// ---------------------------------------------------------------------------
// ControlVariable — mirrors LLControlVariable
// ---------------------------------------------------------------------------

/**
 * Represents a single viewer control variable (gSavedSettings entry).
 *
 * Instances are created by [FSFloaterSettingsImp.buildSettingsMap] and come
 * from the viewer's control-group registry.
 */
class ControlVariable(
    val name: String,
    val comment: String
) {
    var value: ControlValue = ControlValue.Unknown

    var isDefault: Boolean = true
        private set

    var isHiddenFromSettingsEditor: Boolean = false
        private set

    /** Returns `false` when the current value violates a sanity constraint. */
    var isSane: Boolean = true
        private set

    private val commitListeners: MutableList<() -> Unit> = mutableListOf()

    fun onCommit(listener: () -> Unit): () -> Unit {
        commitListeners += listener
        return listener
    }

    fun disconnectCommit(listener: () -> Unit) {
        commitListeners -= listener
    }

    fun set(newValue: ControlValue) {
        value = newValue
        isDefault = false
        commitListeners.forEach { it() }
        System.err.println("Platform: persist new value to gSavedSettings / gSavedPerAccountSettings")
    }

    fun resetToDefault() {
        System.err.println("Platform: reset this control to its compiled-in default value")
    }
}

// ---------------------------------------------------------------------------
// FSFloaterSettingsImp — mirrors LLFloaterSettingsDebug (with FS additions)
// ---------------------------------------------------------------------------

/**
 * Debug/implementation settings floater.
 *
 * Displays a searchable, scrollable list of all viewer control variables
 * (gSavedSettings and gSavedPerAccountSettings).  Selecting a variable shows
 * its current value in the appropriate editor widget (spinner, color swatch,
 * radio, or text box) and lets the user change or reset it.
 *
 * Firestorm additions over the LL base:
 * - Non-default settings are marked with a `*` column indicator.
 * - A "hide default" toggle reduces clutter.
 * - Sanity-check integration via [SanityCheck].
 * - RLVa `isHiddenFromSettingsEditor` support disables editing of RLVa-locked controls.
 *
 * Mirrors [LLFloaterSettingsDebug] from `llfloatersettingsdebug.h`.
 *
 * @param seed LLSD key; expected value is one of `"all"`, `"base"`, `"account"`, `"skin"`.
 */
class FSFloaterSettingsImp(val seed: LLSD) {

    // ------------------------------------------------------------------
    // Settings map — populated in [postBuild]
    // ------------------------------------------------------------------

    private val settingsMap: MutableMap<String, ControlVariable> = mutableMapOf()

    // ------------------------------------------------------------------
    // Selection state
    // ------------------------------------------------------------------

    private var currentControl: ControlVariable? = null
    private var previousControl: ControlVariable? = null
    private var previousControlListener: (() -> Unit)? = null
    private var oldVisibility: Boolean = false

    // ------------------------------------------------------------------
    // Filter state
    // ------------------------------------------------------------------

    private var oldSearchTerm: String = "---"   // sentinel forces first refresh

    // ------------------------------------------------------------------
    // Child widget stubs — populated in [postBuild]
    // ------------------------------------------------------------------

    private var searchInput:    SearchEditor?    = null
    private var settingsList:   ScrollListCtrl?  = null
    private var commentText:    TextEditor?      = null
    private var spinner1:       SpinCtrl?        = null
    private var spinner2:       SpinCtrl?        = null
    private var spinner3:       SpinCtrl?        = null
    private var spinner4:       SpinCtrl?        = null
    private var colorSwatch:    ColorSwatchCtrl? = null
    private var valText:        LineEditor?      = null
    private var booleanCombo:   RadioGroup?      = null
    private var copyButton:     Button?          = null
    private var defaultButton:  Button?          = null
    private var sanityButton:   Button?          = null

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Wire up child widgets, populate [settingsMap], and show the initial list.
     *
     * @return `true` on success.
     */
    fun postBuild(): Boolean {
        searchInput   = getChild<SearchEditor>("search_settings_input")?.also { ed ->
            ed.setFocus(true)                   // FS: auto-focus search box on open
            ed.setKeystrokeCallback { onUpdateFilter() }
        }
        settingsList  = getChild<ScrollListCtrl>("settings_scroll_list")
        commentText   = getChild<TextEditor>("comment_text")
        spinner1      = getChild<SpinCtrl>("val_spinner_1")
        spinner2      = getChild<SpinCtrl>("val_spinner_2")
        spinner3      = getChild<SpinCtrl>("val_spinner_3")
        spinner4      = getChild<SpinCtrl>("val_spinner_4")
        colorSwatch   = getChild<ColorSwatchCtrl>("val_color_swatch")
        valText       = getChild<LineEditor>("val_text")
        booleanCombo  = getChild<RadioGroup>("boolean_combo")
        copyButton    = getChild<Button>("copy_btn")
        defaultButton = getChild<Button>("default_btn")
        sanityButton  = getChild<Button>("sanity_warning_btn")

        buildSettingsMap()

        System.err.println("Platform: subscribe to DebugSettingsHideDefault control change → onUpdateFilter()")

        onUpdateFilter()
        settingsList?.sortByColumn(columnIndex = 1, ascending = true)   // FS: sort by name

        System.err.println("Platform: LLNotificationsUtil.add(\"DebugSettingsWarning\")")

        return true
    }

    /**
     * Per-frame update: detect if the selected control's hidden-from-editor
     * flag changed (RLVa can toggle this at runtime).
     */
    fun draw() {
        val ctrl = currentControl ?: return
        if (ctrl.isHiddenFromSettingsEditor != oldVisibility) {
            updateControl()
        }
        System.err.println("Platform: delegate to LLFloater.draw()")
    }

    // ------------------------------------------------------------------
    // Filter / list management
    // ------------------------------------------------------------------

    /**
     * Rebuild the visible rows based on the current search term and the
     * "hide default settings" preference.
     * Mirrors [LLFloaterSettingsDebug::onUpdateFilter].
     */
    fun onUpdateFilter() {
        val hideDefault = getSetting<Boolean>("DebugSettingsHideDefault") ?: false
        val searchTerm  = searchInput?.getValue().orEmpty()

        if (searchTerm == oldSearchTerm) return
        oldSearchTerm = searchTerm

        val lowerSearch = searchTerm.lowercase()

        settingsList?.deleteAllItems()

        for ((_, ctrl) in settingsMap) {
            if (hideDefault && ctrl.isDefault) continue

            val matches = lowerSearch.isEmpty()
                || ctrl.name.lowercase().contains(lowerSearch)
                || ctrl.comment.lowercase().contains(lowerSearch)

            if (!matches) continue

            settingsList?.addRow(
                ScrollListRow(
                    changedIndicator = if (!ctrl.isDefault) "*" else "",
                    settingName      = ctrl.name,
                    userData         = ctrl
                )
            )
        }

        if ((settingsList?.itemCount ?: 0) > 0 && searchTerm.isNotEmpty()) {
            settingsList?.sortByColumn(columnIndex = 1, ascending = true)
            settingsList?.selectFirstItem()
        }

        onSettingSelect()
    }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    /**
     * Called when the user selects a different row in the settings list.
     * Subscribes to the newly selected control's commit signal so that live
     * changes are reflected in the editor widgets.
     */
    fun onSettingSelect() {
        val newCtrl = getControlVariable()

        if (newCtrl !== previousControl) {
            previousControlListener?.let { previousControl?.disconnectCommit(it) }

            val listener: (() -> Unit)? = newCtrl?.onCommit { onSettingSelect() }
            previousControlListener = listener
            previousControl = newCtrl
        }

        currentControl = newCtrl
        updateControl()
    }

    // ------------------------------------------------------------------
    // Value editing
    // ------------------------------------------------------------------

    /**
     * Read back the current widget values and commit them to the selected
     * control variable.
     * Mirrors [LLFloaterSettingsDebug::onCommitSettings].
     */
    fun onCommitSettings() {
        val ctrl = currentControl ?: return

        val newValue: ControlValue = when (val cv = ctrl.value) {
            is ControlValue.UIntVal  -> ControlValue.UIntVal((spinner1?.getValue() ?: 0f).toUInt())
            is ControlValue.IntVal   -> ControlValue.IntVal((spinner1?.getValue() ?: 0f).toInt())
            is ControlValue.FloatVal -> ControlValue.FloatVal(spinner1?.getValue() ?: 0f)
            is ControlValue.Bool     -> ControlValue.Bool(booleanCombo?.getValue() == "true")
            is ControlValue.StringVal -> ControlValue.StringVal(valText?.getValue().orEmpty())
            is ControlValue.Vec3     -> ControlValue.Vec3(
                spinner1?.getValue() ?: 0f,
                spinner2?.getValue() ?: 0f,
                spinner3?.getValue() ?: 0f
            )
            is ControlValue.Vec3d    -> ControlValue.Vec3d(
                (spinner1?.getValue() ?: 0f).toDouble(),
                (spinner2?.getValue() ?: 0f).toDouble(),
                (spinner3?.getValue() ?: 0f).toDouble()
            )
            is ControlValue.Quat     -> ControlValue.Quat(
                spinner1?.getValue() ?: 0f,
                spinner2?.getValue() ?: 0f,
                spinner3?.getValue() ?: 0f,
                spinner4?.getValue() ?: 0f
            )
            is ControlValue.Rect     -> ControlValue.Rect(
                (spinner1?.getValue() ?: 0f).toInt(),
                (spinner2?.getValue() ?: 0f).toInt(),
                (spinner3?.getValue() ?: 0f).toInt(),
                (spinner4?.getValue() ?: 0f).toInt()
            )
            is ControlValue.Color4   -> {
                val alpha = spinner4?.getValue() ?: cv.a
                System.err.println("Platform: read Color4 from colorSwatch, combine with alpha spinner")
                ControlValue.Color4(0f, 0f, 0f, alpha)
            }
            is ControlValue.Color3   -> {
                System.err.println("Platform: read Color3 from colorSwatch")
                ControlValue.Color3(0f, 0f, 0f)
            }
            else -> return
        }

        ctrl.set(newValue)

        if (!ctrl.isSane) {
            onSanityCheck()
        }
    }

    /**
     * Reset the selected control to its compiled-in default value.
     */
    fun onClickDefault() {
        currentControl?.resetToDefault()
        updateControl()
    }

    /**
     * Copy the selected control's name to the system clipboard.
     */
    fun onCopyToClipboard() {
        val name = currentControl?.name ?: return
        System.err.println("Platform: write '$name' to system clipboard; show ControlNameCopiedToClipboard notification")
    }

    /**
     * Run the sanity-check for the currently selected control.
     */
    fun onSanityCheck() {
        System.err.println("Platform: SanityCheck.instance().onSanity(currentControl)")
    }

    /**
     * Explicitly show the sanity-check warning even if it was previously
     * dismissed.
     */
    fun onClickSanityWarning() {
        System.err.println("Platform: SanityCheck.instance().onSanity(currentControl, forceShow = true)")
    }

    // ------------------------------------------------------------------
    // Static entry point
    // ------------------------------------------------------------------

    companion object {
        /**
         * Show the settings debug floater and pre-select [control].
         * Mirrors [LLFloaterSettingsDebug::showControl].
         */
        fun showControl(control: String) {
            System.err.println("Platform: LLFloaterReg.showTypedInstance(\"settings_debug\", \"all\"); set search input to '$control'; call onUpdateFilter()")
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Populate [settingsMap] from the control groups specified by [seed].
     * Mirrors the inner functor `f` in [postBuild].
     */
    private fun buildSettingsMap() {
        val key = seed.asString()
        if (key == "all" || key == "base") {
            System.err.println("Platform: gSavedSettings.applyToAll { name, ctrl -> settingsMap[name] = ctrl }")
        }
        if (key == "all" || key == "account") {
            System.err.println("Platform: gSavedPerAccountSettings.applyToAll { name, ctrl -> settingsMap[name] = ctrl }")
        }
    }

    /** Return the [ControlVariable] for the currently selected row, or null. */
    private fun getControlVariable(): ControlVariable? {
        return settingsList?.getFirstSelected()?.userData as? ControlVariable
    }

    /**
     * Refresh all editor widgets to reflect [currentControl]'s value and type.
     * Mirrors [LLFloaterSettingsDebug::updateControl].
     */
    fun updateControl() {
        hideAllEditors()

        val ctrl = currentControl ?: return

        oldVisibility = ctrl.isHiddenFromSettingsEditor
        val editable = !oldVisibility

        spinner1?.isEnabled    = editable
        spinner2?.isEnabled    = editable
        spinner3?.isEnabled    = editable
        spinner4?.isEnabled    = editable
        colorSwatch?.isEnabled = editable
        valText?.isEnabled     = editable
        booleanCombo?.isEnabled = editable
        defaultButton?.isEnabled = editable

        copyButton?.isEnabled  = true
        sanityButton?.isVisible = !ctrl.isSane

        commentText?.setText("${ctrl.name}: ${ctrl.comment}")

        when (val v = ctrl.value) {
            is ControlValue.UIntVal  -> showSpinner1("value", v.v.toFloat(), min = 0f, max = UInt.MAX_VALUE.toFloat(), precision = 0, increment = 1f)
            is ControlValue.IntVal   -> showSpinner1("value", v.v.toFloat(), min = Int.MIN_VALUE.toFloat(), max = Int.MAX_VALUE.toFloat(), precision = 0, increment = 1f)
            is ControlValue.FloatVal -> showSpinner1("value", v.v, precision = 3)
            is ControlValue.Bool     -> {
                booleanCombo?.isVisible = true
                booleanCombo?.setValue(if (v.v) "true" else "")
            }
            is ControlValue.StringVal -> {
                valText?.isVisible = true
                valText?.setValue(v.v)
            }
            is ControlValue.Vec3 -> {
                showSpinner1("X", v.x, precision = 3)
                showSpinner2("Y", v.y, precision = 3)
                showSpinner3("Z", v.z, precision = 3)
            }
            is ControlValue.Vec3d -> {
                showSpinner1("X", v.x.toFloat(), precision = 3)
                showSpinner2("Y", v.y.toFloat(), precision = 3)
                showSpinner3("Z", v.z.toFloat(), precision = 3)
            }
            is ControlValue.Quat -> {
                showSpinner1("X", v.x, precision = 4)
                showSpinner2("Y", v.y, precision = 4)
                showSpinner3("Z", v.z, precision = 4)
                showSpinner4("S", v.s, precision = 4)
            }
            is ControlValue.Rect -> {
                showSpinner1("Left",   v.left.toFloat(),   precision = 0, increment = 1f)
                showSpinner2("Right",  v.right.toFloat(),  precision = 0, increment = 1f)
                showSpinner3("Bottom", v.bottom.toFloat(), precision = 0, increment = 1f)
                showSpinner4("Top",    v.top.toFloat(),    precision = 0, increment = 1f)
            }
            is ControlValue.Color4 -> {
                colorSwatch?.isVisible = true
                System.err.println("Platform: set colorSwatch to Color4(r,g,b); show alpha in spinner4")
                showSpinner4("Alpha", v.a, min = 0f, max = 1f, precision = 3)
            }
            is ControlValue.Color3 -> {
                colorSwatch?.isVisible = true
                System.err.println("Platform: set colorSwatch to Color3(r,g,b)")
            }
            is ControlValue.RawLLSD -> {
                System.err.println("Platform: pretty-print LLSD to commentText (RLVa extension)")
            }
            else -> commentText?.setText("unknown")
        }
    }

    private fun hideAllEditors() {
        spinner1?.isVisible    = false
        spinner2?.isVisible    = false
        spinner3?.isVisible    = false
        spinner4?.isVisible    = false
        colorSwatch?.isVisible = false
        valText?.isVisible     = false
        booleanCombo?.isVisible = false
        sanityButton?.isVisible = false
        commentText?.setText("")
        copyButton?.isEnabled  = false
        defaultButton?.isEnabled = false
    }

    private fun showSpinner1(label: String, value: Float, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE, precision: Int = 3, increment: Float = 0.1f) {
        spinner1?.apply { isVisible = true; setLabel(label); setValue(value); setMin(min); setMax(max); setPrecision(precision); setIncrement(increment) }
    }
    private fun showSpinner2(label: String, value: Float, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE, precision: Int = 3, increment: Float = 0.1f) {
        spinner2?.apply { isVisible = true; setLabel(label); setValue(value); setMin(min); setMax(max); setPrecision(precision); setIncrement(increment) }
    }
    private fun showSpinner3(label: String, value: Float, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE, precision: Int = 3, increment: Float = 0.1f) {
        spinner3?.apply { isVisible = true; setLabel(label); setValue(value); setMin(min); setMax(max); setPrecision(precision); setIncrement(increment) }
    }
    private fun showSpinner4(label: String, value: Float, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE, precision: Int = 3, increment: Float = 0.1f) {
        spinner4?.apply { isVisible = true; setLabel(label); setValue(value); setMin(min); setMax(max); setPrecision(precision); setIncrement(increment) }
    }

    private fun <T> getSetting(key: String): T? {
        System.err.println("Platform: gSavedSettings.get<T>('$key')")
        return null
    }

    private fun <T> getChild(name: String): T? {
        System.err.println("Platform: resolve child widget '$name' from the floater's view hierarchy")
        return null
    }

    // ------------------------------------------------------------------
    // Widget stub types
    // ------------------------------------------------------------------

    class SearchEditor {
        fun getValue(): String { System.err.println("Platform: return search-editor text"); return "" }
        fun setText(v: String) { System.err.println("Platform: set search-editor text") }
        fun setFocus(focus: Boolean) { System.err.println("Platform: set keyboard focus") }
        fun setKeystrokeCallback(cb: () -> Unit) { System.err.println("Platform: fire cb on every keystroke") }
    }

    class ScrollListRow(
        val changedIndicator: String,
        val settingName: String,
        val userData: Any?
    )

    class ScrollListCtrl {
        var itemCount: Int = 0
        fun deleteAllItems() { System.err.println("Platform: clear all rows") }
        fun addRow(row: ScrollListRow) { System.err.println("Platform: append row to scroll list") }
        fun sortByColumn(columnIndex: Int, ascending: Boolean) { System.err.println("Platform: sort by column") }
        fun selectFirstItem() { System.err.println("Platform: select the topmost row") }
        fun getFirstSelected(): ScrollListRow? { System.err.println("Platform: return currently selected row or null"); return null }
    }

    class TextEditor {
        fun getText(): String { System.err.println("Platform: return text-editor content"); return "" }
        fun setText(v: String) { System.err.println("Platform: set text-editor content") }
    }

    class SpinCtrl {
        var isVisible: Boolean = true
        var isEnabled: Boolean = true
        fun getValue(): Float { System.err.println("Platform: return spinner value"); return 0f }
        fun setValue(v: Float) { System.err.println("Platform: set spinner value") }
        fun setLabel(label: String) { System.err.println("Platform: set spinner label") }
        fun setMin(v: Float) { System.err.println("Platform: set spinner minimum") }
        fun setMax(v: Float) { System.err.println("Platform: set spinner maximum") }
        fun setPrecision(p: Int) { System.err.println("Platform: set spinner decimal precision") }
        fun setIncrement(inc: Float) { System.err.println("Platform: set spinner step increment") }
        fun hasFocus(): Boolean { System.err.println("Platform: return true if spinner has keyboard focus"); return false }
    }

    class ColorSwatchCtrl {
        var isVisible: Boolean = true
        var isEnabled: Boolean = true
        fun getValue(): LLSD { System.err.println("Platform: return current colour as LLSD"); return LLSD() }
        fun set(color: Any, update: Boolean, preview: Boolean) { System.err.println("Platform: set swatch color") }
        fun setValue(v: LLSD) { System.err.println("Platform: set swatch color from LLSD") }
    }

    class LineEditor {
        var isVisible: Boolean = true
        var isEnabled: Boolean = true
        fun getValue(): String { System.err.println("Platform: return line-editor text"); return "" }
        fun setValue(v: String) { System.err.println("Platform: set line-editor text") }
    }

    class RadioGroup {
        var isVisible: Boolean = true
        var isEnabled: Boolean = true
        fun getValue(): String { System.err.println("Platform: return selected radio value string"); return "" }
        fun setValue(v: String) { System.err.println("Platform: select radio button matching value") }
        fun hasFocus(): Boolean { System.err.println("Platform: return true if radio group has focus"); return false }
    }

    class Button {
        var isEnabled: Boolean = true
        var isVisible: Boolean = true
    }
}
