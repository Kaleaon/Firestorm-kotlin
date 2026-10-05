package com.firestorm.llui

enum class AddPosition { ADD_TOP, ADD_BOTTOM, ADD_SORTED }

open class ViewModel {
    protected var value: Any? = null
    var isDirty: Boolean = false
        protected set

    constructor()

    constructor(initialValue: Any?) {
        setValue(initialValue)
    }

    open fun setValue(v: Any?) {
        value = v
        isDirty = true
    }

    open fun getValue(): Any? = value

    fun resetDirty() { isDirty = false }
    fun setDirty() { isDirty = true }
}

open class TextViewModel : ViewModel {
    private var stringValue: String = ""
    private var display: String = ""
    var displayGeneration: Int = -1
        private set
    private var updateFromDisplay: Boolean = false

    constructor() : super(false)

    constructor(initialValue: Any?) : super(initialValue) {
        updateFromDisplay = false
    }

    override fun setValue(v: Any?) {
        super.setValue(v)
        val s = v?.toString() ?: ""
        stringValue = s
        display = s
        displayGeneration++
        updateFromDisplay = false
    }

    fun getDisplay(): String = display

    fun getEditableDisplay(): String {
        isDirty = true
        displayGeneration++
        updateFromDisplay = true
        return display
    }

    fun setDisplay(v: String) {
        display = v
        displayGeneration++
        isDirty = true
        // defer UTF-8 conversion until getValue() is called
        updateFromDisplay = true
    }

    override fun getValue(): Any? {
        syncFromDisplayIfNeeded()
        return value
    }

    fun getStringValue(): String {
        syncFromDisplayIfNeeded()
        return stringValue
    }

    private fun syncFromDisplayIfNeeded() {
        if (updateFromDisplay) {
            updateFromDisplay = false
            stringValue = display
            value = stringValue
        }
    }
}

open class ListViewModel : ViewModel {
    constructor() : super()
    constructor(values: Any?) : super()

    open fun addColumn(column: Any?, pos: AddPosition = AddPosition.ADD_BOTTOM) {}
    open fun clearColumns() {}
    open fun setColumnLabel(column: String, label: String) {}
    open fun addElement(value: Any?, pos: AddPosition = AddPosition.ADD_BOTTOM, userdata: Any? = null): Any? = null
    open fun addSimpleElement(value: String, pos: AddPosition, id: Any?): Any? = null
    open fun clearRows() {}
    open fun sortByColumn(name: String, ascending: Boolean) {}
}
