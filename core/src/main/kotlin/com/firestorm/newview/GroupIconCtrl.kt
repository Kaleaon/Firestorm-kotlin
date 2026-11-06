package com.firestorm.newview

import com.firestorm.ui.IconCtrl
import com.firestorm.ui.LLSD
import java.util.UUID

enum class GroupChange {
    GC_PROPERTIES,
    GC_MEMBER_DATA,
    GC_ROLE_DATA,
    GC_ROLE_MEMBER_DATA,
    GC_TITLES,
    GC_ALL
}

interface GroupMgrObserver {
    val observedGroupId: UUID
    fun changed(gc: GroupChange)
}

open class GroupIconCtrl(
    groupId: UUID? = null,
    drawTooltip: Boolean = true,
    defaultIconName: String = "",
    minWidth: Int = 32,
    minHeight: Int = 32,
) : IconCtrl(minWidth = minWidth, minHeight = minHeight), GroupMgrObserver {

    protected var groupId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        private set
    override val observedGroupId: UUID get() = groupId

    protected var groupName: String = ""
    var drawTooltip: Boolean = drawTooltip
        private set
    protected val defaultIconName: String = defaultIconName

    init {
        if (groupId != null) {
            setValue(LLSD.fromUUID(groupId))
        } else {
            super.setValue(LLSD.fromString(defaultIconName))
        }
    }

    fun getGroupName(): String = groupName
    fun getGroupId(): UUID = groupId
    fun setDrawTooltip(value: Boolean) { drawTooltip = value }

    fun setIconId(iconId: UUID?) {
        if (iconId != null && iconId != UUID.fromString("00000000-0000-0000-0000-000000000000")) {
            super.setValue(LLSD.fromUUID(iconId))
        } else {
            super.setValue(LLSD.fromString(defaultIconName))
        }
    }

    open fun setValue(value: LLSD) {
        if (value.isUUID()) {
            val gm = GroupMgr.getInstance()
            val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
            if (groupId != nullId) {
                gm.removeObserver(this)
            }
            val newId = value.asUUID()
            if (groupId != newId) {
                groupId = newId
                if (!updateFromCache()) {
                    super.setValue(LLSD.fromString(defaultIconName))
                    gm.addObserver(this)
                    gm.sendGroupPropertiesRequest(groupId)
                }
            }
        } else {
            super.setValue(value)
        }
    }

    override fun changed(gc: GroupChange) {
        if (gc == GroupChange.GC_PROPERTIES) {
            updateFromCache()
        }
    }

    protected fun updateFromCache(): Boolean {
        val groupData = GroupMgr.getInstance().getGroupData(groupId) ?: return false
        setIconId(groupData.insigniaId)
        if (drawTooltip && groupData.name.isNotEmpty()) {
            setToolTip(groupData.name)
            groupName = groupData.name
        } else {
            setToolTip("")
        }
        return true
    }

    fun dispose() {
        GroupMgr.getInstance().removeObserver(this)
    }
}
