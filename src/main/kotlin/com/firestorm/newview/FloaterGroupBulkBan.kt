package com.firestorm.newview

import com.firestorm.ui.Floater
import com.firestorm.ui.PanelGroupBulkBan
import java.util.UUID

class FloaterGroupBulkBan private constructor(
    groupId: UUID = NULL_UUID
) : Floater(groupId) {

    private val impl: Impl = Impl(groupId)

    init {
        val headerSize = headerHeight
        val bulkBanPanel = impl.bulkBanPanel
        val contents = bulkBanPanel.getRect().apply { top -= headerSize }

        setTitle(bulkBanPanel.getString("GroupBulkBan"))
        bulkBanPanel.setCloseCallback { closeFloater() }
        bulkBanPanel.setRect(contents)
        addChild(bulkBanPanel)
    }

    override fun onDestroy() {
        if (impl.groupId != NULL_UUID) {
            Impl.instances.remove(impl.groupId)
        }
        super.onDestroy()
    }

    private inner class Impl(val groupId: UUID) {
        val bulkBanPanel: PanelGroupBulkBan = PanelGroupBulkBan(groupId)

        companion object {
            val instances: MutableMap<UUID, FloaterGroupBulkBan> = mutableMapOf()
        }
    }

    companion object {
        private val NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        fun showForGroup(groupId: UUID, agentIds: List<UUID>? = null) {
            if (groupId == NULL_UUID) return

            var floater = Impl.instances[groupId]
            if (floater == null) {
                floater = FloaterGroupBulkBan(groupId)
                val floaterHeaderSize = Floater.defaultParams.headerHeight
                val contents = floater.impl.bulkBanPanel.getRect()
                contents.top += floaterHeaderSize
                floater.setRect(contents)
                floater.dragHandle?.setRect(contents)
                floater.dragHandle?.setTitle(floater.impl.bulkBanPanel.getString("GroupBulkBan"))

                Impl.instances[groupId] = floater
                floater.impl.bulkBanPanel.clear()
            }

            if (agentIds != null) {
                floater.impl.bulkBanPanel.addUsers(agentIds)
            }

            floater.center()
            floater.openFloater()
            floater.impl.bulkBanPanel.update()
        }
    }
}
