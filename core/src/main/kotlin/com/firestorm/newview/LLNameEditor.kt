package com.firestorm.newview

import java.util.UUID

abstract class LLLineEditor {
    protected var text: String = ""

    open fun setText(value: String) {
        text = value
    }

    fun getText(): String = text

    open fun setValue(value: Any?) {}
    open fun getValue(): Any? = null
}

class LLNameEditor(
    nameId: UUID? = null,
    isGroup: Boolean = false
) : LLLineEditor() {

    private var mNameId: UUID = NULL_UUID

    init {
        sInstances.add(this)
        if (nameId != null && nameId != NULL_UUID) {
            setNameID(nameId, isGroup)
        }
    }

    fun destroy() {
        sInstances.remove(this)
    }

    fun setNameID(nameId: UUID, isGroup: Boolean) {
        mNameId = nameId

        val name: String = if (!isGroup) {
            System.err.println("LLNameEditor: setNameID (avatar) not yet implemented")
            ""
        } else {
            System.err.println("LLNameEditor: setNameID (group) not yet implemented")
            ""
        }

        setText(name)
    }

    fun refresh(id: UUID, fullName: String, isGroup: Boolean) {
        if (id == mNameId) {
            setText(fullName)
        }
    }

    override fun setValue(value: Any?) {
        val uuid = value as? UUID ?: return
        setNameID(uuid, false)
    }

    override fun getValue(): UUID = mNameId

    companion object {
        private val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private val sInstances: MutableSet<LLNameEditor> = mutableSetOf()

        fun refreshAll(id: UUID, fullName: String, isGroup: Boolean) {
            sInstances.forEach { it.refresh(id, fullName, isGroup) }
        }
    }
}
