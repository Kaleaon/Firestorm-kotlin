package com.firestorm.newview

import java.util.UUID

abstract class LLTextBox {
    protected var text: String = ""
    var parseHTML: Boolean = false

    open fun setText(value: String) {
        text = value
    }

    fun getText(): String = text
}

class LLNameBox(
    val link: Boolean = false,
    val initialValue: String = ""
) : LLTextBox() {

    var nameId: UUID = NULL_UUID
        private set

    init {
        parseHTML = link
        setText("")
        sInstances.add(this)
    }

    fun destroy() {
        sInstances.remove(this)
    }

    fun setNameID(nameId: UUID, isGroup: Boolean) {
        this.nameId = nameId

        val (gotName, name) = if (!isGroup) {
            TODO("APR: use JVM equivalent of LLAvatarNameCache::get")
        } else {
            TODO("APR: use JVM equivalent of gCacheName->getGroupName")
        }

        @Suppress("UNREACHABLE_CODE")
        if (gotName) setName(name, isGroup) else setText(initialValue)
    }

    fun refresh(id: UUID, fullName: String, isGroup: Boolean) {
        if (id == nameId) {
            setName(fullName, isGroup)
        }
    }

    private fun setName(name: String, isGroup: Boolean) {
        if (link) {
            val url = if (isGroup)
                "[secondlife:///app/group/${nameId}/about $name]"
            else
                "[secondlife:///app/agent/${nameId}/about $name]"
            setText(url)
        } else {
            setText(name)
        }
    }

    companion object {
        private val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private val sInstances: MutableSet<LLNameBox> = mutableSetOf()

        fun refreshAll(id: UUID, fullName: String, isGroup: Boolean) {
            sInstances.forEach { it.refresh(id, fullName, isGroup) }
        }
    }
}
