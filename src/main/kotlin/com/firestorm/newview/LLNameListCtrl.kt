package com.firestorm.newview

import java.util.UUID

abstract class LLScrollListItem(val value: Any? = null) {
    var isSelected: Boolean = false
    private val columns: MutableList<LLScrollListCell> = mutableListOf()

    fun getUUID(): UUID = value as? UUID ?: NULL_UUID

    fun getColumn(index: Int): LLScrollListCell? = columns.getOrNull(index)

    fun addColumn(cell: LLScrollListCell) {
        columns.add(cell)
    }

    companion object {
        val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
}

abstract class LLScrollListCell {
    abstract var cellValue: String
    abstract var altValue: String
    var textWidth: Int = 0
    fun setValue(v: String) { cellValue = v }
    fun setAltValue(v: String) { altValue = v }
}

class LLScrollListText(override var cellValue: String = "", override var altValue: String = "") : LLScrollListCell()

abstract class FSScrollListCtrl {
    protected val itemList: MutableList<LLScrollListItem> = mutableListOf()
    private var highlightedItemIndex: Int = -1

    fun getItemList(): MutableList<LLScrollListItem> = itemList

    fun getHighlightedItemInx(): Int = highlightedItemIndex

    fun getItemIndex(item: LLScrollListItem): Int = itemList.indexOf(item)

    fun hitItem(x: Int, y: Int): LLScrollListItem? {
        System.err.println("FSScrollListCtrl: hitItem not yet implemented")
        return null
    }

    fun getColumnIndexFromOffset(x: Int): Int {
        System.err.println("FSScrollListCtrl: getColumnIndexFromOffset not yet implemented")
        return 0
    }

    fun getCellRect(rowIndex: Int, columnIndex: Int): LLRect {
        System.err.println("FSScrollListCtrl: getCellRect not yet implemented")
        return LLRect(0, 0, 0, 0)
    }

    fun selectNthItem(index: Int) {
        itemList.getOrNull(index)?.isSelected = true
    }

    fun deleteSingleItem(index: Int) {
        if (index in itemList.indices) itemList.removeAt(index)
    }

    fun getFirstSelected(): LLScrollListItem? = itemList.firstOrNull { it.isSelected }

    fun getNumColumns(): Int {
        System.err.println("FSScrollListCtrl: getNumColumns not yet implemented")
        return 0
    }

    open fun addRow(item: LLScrollListItem, nameItem: Any, pos: EAddPosition) {
        when (pos) {
            EAddPosition.ADD_TOP -> itemList.add(0, item)
            else -> itemList.add(item)
        }
    }

    fun dirtyColumns() {
        System.err.println("FSScrollListCtrl: dirtyColumns not yet implemented")
    }

    fun setNeedsSort() {
        System.err.println("FSScrollListCtrl: setNeedsSort not yet implemented")
    }

    fun getColumn(index: Int): LLScrollListColumn? {
        System.err.println("FSScrollListCtrl: getColumn(index) not yet implemented")
        return null
    }

    fun getColumn(name: String): LLScrollListColumn? {
        System.err.println("FSScrollListCtrl: getColumn(name) not yet implemented")
        return null
    }

    fun sortByColumnIndex(columnIndex: Int, ascending: Boolean) {
        System.err.println("FSScrollListCtrl: sortByColumnIndex not yet implemented")
    }

    open fun mouseOverHighlightNthItem(index: Int) {
        highlightedItemIndex = index
    }

    open fun updateColumns(forceUpdate: Boolean) {}

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("FSScrollListCtrl: handleRightMouseDown not yet implemented")
        return false
    }

    fun getToolTip(): String = ""

    fun getName(): String = ""

    fun setContextMenu(type: ContextMenuType) {}
    fun getContextMenuType(): ContextMenuType = ContextMenuType.MENU_NONE
}

data class LLRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun pointInRect(x: Int, y: Int): Boolean = x in left..right && y in bottom..top
    val mRight: Int get() = right
    val mTop: Int get() = top
    fun getHeight(): Int = top - bottom
}

data class LLScrollListColumn(val mIndex: Int, val name: String, val mHeader: LLScrollListColumnHeader?)

class LLScrollListColumnHeader {
    fun setHasResizableElement(value: Boolean) {}
}

enum class EAddPosition { ADD_TOP, ADD_BOTTOM, ADD_SORTED }
enum class ContextMenuType { MENU_NONE, MENU_GROUP, MENU_AVATAR }


class LLNameListItem(
    value: Any? = null,
    val isGroup: Boolean = false,
    val isExperience: Boolean = false
) : LLScrollListItem(value) {

    private var specialId: UUID = NULL_UUID

    fun setSpecialID(id: UUID) { specialId = id }
    fun getSpecialID(): UUID = specialId
}


class LLNameListCtrl(
    private val nameColumnIndex: Int = 0,
    private val nameColumn: String = "",
    private val allowCallingCardDrop: Boolean = false,
    private val shortNames: Boolean = false
) : FSScrollListCtrl() {

    enum class ENameType { INDIVIDUAL, GROUP, SPECIAL, EXPERIENCE }

    private var mNameColumnIndex: Int = nameColumnIndex
    private var mNameListType: ENameType = ENameType.INDIVIDUAL
    private var hoverIconName: String = "Info_Small"

    private val avatarNameCacheConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()
    private val groupNameCacheConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()

    private val iconClickedListeners: MutableList<(UUID) -> Unit> = mutableListOf()

    fun setIconClickedCallback(cb: (UUID) -> Unit): () -> Unit {
        iconClickedListeners.add(cb)
        return { iconClickedListeners.remove(cb) }
    }

    fun isSpecialType(): Boolean = mNameListType == ENameType.SPECIAL

    fun setNameListType(type: ENameType) { mNameListType = type }
    fun setHoverIconName(iconName: String) { hoverIconName = iconName }
    fun setAllowCallingCardDrop(allow: Boolean) {}

    fun addNameItem(
        agentId: UUID,
        pos: EAddPosition = EAddPosition.ADD_BOTTOM,
        enabled: Boolean = true,
        suffix: String = "",
        prefix: String = ""
    ): LLScrollListItem {
        val nameItem = NameItem(value = agentId, enabled = enabled, target = ENameType.INDIVIDUAL)
        return addNameItemRow(nameItem, pos, suffix, prefix)
    }

    fun addNameItem(item: NameItem, pos: EAddPosition = EAddPosition.ADD_BOTTOM): LLScrollListItem {
        val copy = item.copy(target = ENameType.INDIVIDUAL)
        return addNameItemRow(copy, pos)
    }

    fun addElement(element: Map<String, Any?>, pos: EAddPosition = EAddPosition.ADD_BOTTOM): LLScrollListItem {
        val nameItem = NameItem(
            value = element["value"] as? UUID ?: LLScrollListItem.NULL_UUID,
            name = element["name"] as? String ?: "",
            target = ENameType.INDIVIDUAL
        )
        return addNameItemRow(nameItem, pos)
    }

    fun addGroupNameItem(groupId: UUID, pos: EAddPosition = EAddPosition.ADD_BOTTOM, enabled: Boolean = true) {
        val item = NameItem(value = groupId, enabled = enabled, target = ENameType.GROUP)
        addNameItemRow(item, pos)
    }

    fun addGroupNameItem(item: NameItem, pos: EAddPosition = EAddPosition.ADD_BOTTOM) {
        addNameItemRow(item.copy(target = ENameType.GROUP), pos)
    }

    fun addNameItemRow(
        nameItem: NameItem,
        pos: EAddPosition = EAddPosition.ADD_BOTTOM,
        suffix: String = "",
        prefix: String = ""
    ): LLScrollListItem {
        val id = nameItem.value
        val item = LLNameListItem(id, nameItem.target == ENameType.GROUP, nameItem.target == ENameType.EXPERIENCE)

        addRow(item, nameItem, pos)

        var fullname = nameItem.name

        when (nameItem.target) {
            ENameType.GROUP -> {
                val (got, name) = resolveGroupName(id)
                if (got) {
                    fullname = name
                } else {
                    groupNameCacheConnections[id]?.invoke()
                    groupNameCacheConnections[id] = scheduleGroupNameLookup(id, item)
                }
            }
            ENameType.SPECIAL -> {
                item.setSpecialID(nameItem.specialId)
                return item
            }
            ENameType.INDIVIDUAL -> {
                if (id == LLScrollListItem.NULL_UUID) {
                    fullname = nobodyName()
                } else {
                    val (got, avName) = resolveAvatarName(id)
                    if (got) {
                        fullname = avName
                    } else {
                        avatarNameCacheConnections[id]?.invoke()
                        avatarNameCacheConnections[id] = scheduleAvatarNameLookup(id, suffix, prefix, item)
                    }
                }
            }
            ENameType.EXPERIENCE -> {}
        }

        val displayName = if (suffix.isNotEmpty()) fullname + suffix else fullname
        val cell = item.getColumn(mNameColumnIndex) as? LLScrollListText
        if (cell != null) {
            cell.setValue(prefix + displayName)
        }

        dirtyColumns()

        val col = getColumn(mNameColumnIndex)
        col?.mHeader?.setHasResizableElement(true)

        return item
    }

    private fun resolveGroupName(id: UUID): Pair<Boolean, String> {
        System.err.println("LLNameListCtrl: resolveGroupName not yet implemented")
        return Pair(false, "")
    }

    private fun resolveAvatarName(id: UUID): Pair<Boolean, String> {
        System.err.println("LLNameListCtrl: resolveAvatarName not yet implemented")
        return Pair(false, "")
    }

    private fun nobodyName(): String {
        System.err.println("LLNameListCtrl: nobodyName not yet implemented")
        return ""
    }

    private fun scheduleAvatarNameLookup(
        id: UUID,
        suffix: String,
        prefix: String,
        item: LLNameListItem
    ): () -> Unit {
        System.err.println("LLNameListCtrl: scheduleAvatarNameLookup not yet implemented")
        return {}
    }

    private fun scheduleGroupNameLookup(id: UUID, item: LLNameListItem): () -> Unit {
        System.err.println("LLNameListCtrl: scheduleGroupNameLookup not yet implemented")
        return {}
    }

    fun onAvatarNameCache(agentId: UUID, displayName: String, suffix: String, prefix: String, item: LLNameListItem) {
        avatarNameCacheConnections.remove(agentId)

        val name = (if (suffix.isNotEmpty()) displayName + suffix else displayName)
            .let { if (prefix.isNotEmpty()) prefix + it else it }

        if (item.getUUID() == agentId) {
            val cell = item.getColumn(mNameColumnIndex) as? LLScrollListText
            cell?.setValue(name)
            setNeedsSort()
        }

        dirtyColumns()
    }

    fun onGroupNameCache(groupId: UUID, name: String, item: LLNameListItem) {
        groupNameCacheConnections.remove(groupId)

        if (item.getUUID() == groupId) {
            val cell = item.getColumn(mNameColumnIndex) as? LLScrollListText
            cell?.setValue(name)
            setNeedsSort()
        }

        dirtyColumns()
    }

    fun removeNameItem(agentId: UUID) {
        val idx = itemList.indexOfFirst { listItem ->
            val effectiveId = if (isSpecialType())
                (listItem as? LLNameListItem)?.getSpecialID() ?: LLScrollListItem.NULL_UUID
            else
                listItem.getUUID()
            effectiveId == agentId
        }
        if (idx >= 0) {
            selectNthItem(idx)
            deleteSingleItem(idx)
        }
    }

    fun getNameItemByAgentId(agentId: UUID): LLScrollListItem? =
        itemList.firstOrNull { it.getUUID() == agentId }

    fun selectItemBySpecialId(specialId: UUID) {
        if (specialId == LLScrollListItem.NULL_UUID) return
        itemList.filterIsInstance<LLNameListItem>()
            .firstOrNull { it.getSpecialID() == specialId }
            ?.let { it.isSelected = true }
    }

    fun getSelectedSpecialId(): UUID {
        val item = getFirstSelected() as? LLNameListItem
        return item?.getSpecialID() ?: LLScrollListItem.NULL_UUID
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int,
        drop: Boolean,
        cargoType: Int, cargoData: Any?,
        tooltipMsg: StringBuilder
    ): Boolean {
        if (!allowCallingCardDrop) return false

        return if (cargoType == DAD_CALLINGCARD) {
            if (drop) {
                val creatorId = (cargoData as? Map<*, *>)?.get("creator_uuid") as? UUID
                if (creatorId != null) addNameItem(creatorId)
            }
            true
        } else {
            if (tooltipMsg.isEmpty()) {
                val tip = getToolTip()
                tooltipMsg.append(if (tip.isNotEmpty()) tip else "Drag a calling card here\nto add a resident.")
            }
            true
        }
    }

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLNameListCtrl: handleToolTip not yet implemented")
        return false
    }

    override fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val hitItem = hitItem(x, y) as? LLNameListItem
        if (hitItem != null && hitItem.isGroup) {
            val prev = getContextMenuType()
            setContextMenu(ContextMenuType.MENU_GROUP)
            val handled = super.handleRightMouseDown(x, y, mask)
            setContextMenu(prev)
            return handled
        }
        return super.handleRightMouseDown(x, y, mask)
    }

    override fun mouseOverHighlightNthItem(targetIndex: Int) {
        val curIndex = getHighlightedItemInx()
        if (curIndex != targetIndex) {
            val infoIconSize = 16

            if (curIndex in itemList.indices) {
                val cell = itemList[curIndex].getColumn(mNameColumnIndex) as? LLScrollListText
                cell?.let { it.textWidth += infoIconSize }
            }

            if (targetIndex >= 0 && targetIndex < itemList.size) {
                val cell = itemList[targetIndex].getColumn(mNameColumnIndex) as? LLScrollListText
                cell?.let { it.textWidth -= infoIconSize }
            }
        }
        super.mouseOverHighlightNthItem(targetIndex)
    }

    override fun updateColumns(forceUpdate: Boolean) {
        super.updateColumns(forceUpdate)
        if (nameColumn.isNotEmpty()) {
            val col = getColumn(nameColumn)
            if (col != null) {
                mNameColumnIndex = col.mIndex
            }
        }
    }

    fun sortByName(ascending: Boolean) {
        sortByColumnIndex(mNameColumnIndex, ascending)
    }

    private fun showInspector(avatarId: UUID, isGroup: Boolean, isExperience: Boolean = false) {
        if (isSpecialType()) {
            iconClickedListeners.forEach { it(avatarId) }
            return
        }
        System.err.println("LLNameListCtrl: showInspector not yet implemented")
    }

    data class NameItem(
        val value: UUID = LLScrollListItem.NULL_UUID,
        val name: String = "",
        val target: ENameType = ENameType.INDIVIDUAL,
        val specialId: UUID = LLScrollListItem.NULL_UUID,
        val enabled: Boolean = true
    )

    companion object {
        private const val DAD_CALLINGCARD = 2
    }
}
