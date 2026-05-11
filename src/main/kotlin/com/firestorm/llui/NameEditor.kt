package com.firestorm.llui

import com.firestorm.llmath.Rect
import java.util.UUID

class NameEditor(
    name: String,
    rect: Rect = Rect(),
    nameId: UUID? = null,
    isGroup: Boolean = false
) : LineEditor(name, rect) {

    private var nameId: UUID = UUID(0L, 0L)

    companion object {
        private val instances: MutableSet<NameEditor> = mutableSetOf()

        fun refreshAll(id: UUID, fullName: String, isGroup: Boolean) {
            instances.forEach { it.refresh(id, fullName, isGroup) }
        }
    }

    init {
        instances.add(this)
        if (nameId != null && nameId != UUID(0L, 0L)) {
            setNameId(nameId, isGroup)
        }
    }

    fun dispose() {
        instances.remove(this)
    }

    fun setNameId(id: UUID, isGroup: Boolean) {
        nameId = id
        val resolvedName: String = if (!isGroup) {
            TODO("APR: use JVM equivalent — fetch avatar display name from AvatarNameCache for UUID $id")
        } else {
            TODO("APR: use JVM equivalent — fetch group name from CacheName for UUID $id")
        }
        setText(resolvedName)
    }

    fun refresh(id: UUID, fullName: String, isGroup: Boolean) {
        if (id == nameId) {
            setText(fullName)
        }
    }

    fun setValue(value: UUID) {
        setNameId(value, false)
    }

    fun getValue(): UUID = nameId
}
