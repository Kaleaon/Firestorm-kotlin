package com.firestorm.newview

import com.firestorm.ui.FlatListViewEx
import com.firestorm.ui.Panel
import com.firestorm.ui.ToggleableMenu
import com.firestorm.mutelist.Mute
import com.firestorm.mutelist.MuteList
import com.firestorm.mutelist.MuteListObserver
import java.util.UUID

enum class BlockListActionType { NONE, ADD, REMOVE }

class BlockList(params: Params) : FlatListViewEx(params), MuteListObserver {

    class Params : FlatListViewEx.Params()

    private var contextMenu: ToggleableMenu? = null
    private var nameFilter: String = ""
    private var dirty: Boolean = true
    private var shouldAddAll: Boolean = true
    private var actionType: BlockListActionType = BlockListActionType.NONE
    var muteListSize: UInt = 0u
        private set

    private var curItemId: UUID = UUID.randomUUID()
    private var curItemName: String = ""
    private var curItemType: Mute.EType = Mute.EType.BY_NAME
    private var curItemFlags: UInt = 0u
    private var prevNameFilter: String = ""

    companion object {
        private val NAME_COMPARATOR = BlockListNameComparator()
        private val NAME_TYPE_COMPARATOR = BlockListNameTypeComparator()
    }

    init {
        MuteList.instance.addObserver(this)
        muteListSize = MuteList.instance.getMutes().size.toUInt()

        contextMenu = UICtrlFactory.instance.createFromFile<ToggleableMenu>(
            "menu_people_blocked_gear.xml"
        )
    }

    fun destroy() {
        contextMenu?.die()
        MuteList.instance.removeObserver(this)
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleRightMouseDown(x, y, mask)
        val menu = contextMenu
        if (menu != null && size() > 0) {
            menu.buildDrawLabels()
            menu.updateParent(MenuGL.menuContainer)
            MenuGL.showPopup(this, menu, x, y)
        }
        return handled
    }

    fun getContextMenu(): ToggleableMenu? = contextMenu

    fun getBlockedItem(): BlockedListItem? {
        val panel = getSelectedItem()
        return panel as? BlockedListItem
    }

    override fun onChange() {}

    override fun onChangeDetailed(mute: Mute) {
        actionType = getCurrentMuteListActionType()
        curItemId = mute.id
        curItemName = mute.name
        curItemType = mute.type
        curItemFlags = mute.flags
        refresh()
    }

    fun draw() {
        if (dirty) refresh()
        super.draw()
    }

    fun setNameFilter(filter: String) {
        val filterUpper = filter.uppercase()
        if (nameFilter != filterUpper) {
            nameFilter = filterUpper
            setDirty()
        }
    }

    fun sortByName() {
        setComparator(NAME_COMPARATOR)
        sort()
    }

    fun sortByType() {
        setComparator(NAME_TYPE_COMPARATOR)
        sort()
    }

    fun refresh() {
        val haveFilter = nameFilter.isNotEmpty()

        val selected = getSelectedValue()
        var nextSelected: Any? = null

        if (shouldAddAll) {
            clear()
            createList()
            shouldAddAll = false
        } else {
            val mute = Mute(curItemId, curItemName, curItemType, curItemFlags)
            when (actionType) {
                BlockListActionType.ADD -> addNewItem(mute)
                BlockListActionType.REMOVE -> {
                    val selectedMatchesMute =
                        (mute.id != UUID.fromString("00000000-0000-0000-0000-000000000000") &&
                                selected is UUID && selected == mute.id) ||
                        (mute.id == UUID.fromString("00000000-0000-0000-0000-000000000000") &&
                                selected is String && selected == mute.name)

                    if (selectedMatchesMute) {
                        if (!selectNextItemPair(false, true)) {
                            selectNextItemPair(true, true)
                        }
                        nextSelected = getSelectedValue()
                    }
                    removeListItem(mute)
                }
                BlockListActionType.NONE -> Unit
            }
            actionType = BlockListActionType.NONE
        }

        if (haveFilter || (!haveFilter && prevNameFilter.isNotEmpty())) {
            val allItems = mutableListOf<Panel>()
            getItems(allItems)
            for (panel in allItems) {
                val item = panel as? BlockedListItem ?: continue
                hideListItem(item, findInsensitive(item.name, nameFilter))
            }
        }
        prevNameFilter = nameFilter

        if (selected != null) {
            if (getItemPair(selected) != null) {
                selectItemPair(getItemPair(selected)!!, true)
            } else if (nextSelected != null && getItemPair(nextSelected) != null) {
                selectItemPair(getItemPair(nextSelected)!!, true)
            }
        }

        muteListSize = MuteList.instance.getMutes().size.toUInt()
        sort()
        setDirty(false)
    }

    private fun createList() {
        for (mute in MuteList.instance.getMutes()) {
            addNewItem(mute)
        }
    }

    private fun getCurrentMuteListActionType(): BlockListActionType {
        val curSize = MuteList.instance.getMutes().size.toUInt()
        return when {
            curSize > muteListSize -> BlockListActionType.ADD
            curSize < muteListSize -> BlockListActionType.REMOVE
            else -> BlockListActionType.NONE
        }
    }

    private fun addNewItem(mute: Mute) {
        val item = BlockedListItem(mute)
        if (nameFilter.isNotEmpty()) {
            item.highlightName(nameFilter)
        }
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (item.itemId != nullId) {
            addItem(item, item.itemId, ADD_BOTTOM)
        } else {
            addItem(item, item.name, ADD_BOTTOM)
        }
    }

    private fun removeListItem(mute: Mute) {
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (mute.id != nullId) {
            removeItemByUUID(mute.id)
        } else {
            removeItemByValue(mute.name)
        }
    }

    private fun hideListItem(item: BlockedListItem, show: Boolean) {
        item.isVisible = show
    }

    private fun setDirty(dirty: Boolean = true) {
        this.dirty = dirty
    }

    private fun findInsensitive(haystack: String, needleUpper: String): Boolean {
        return haystack.uppercase().contains(needleUpper)
    }

    private fun isActionEnabled(userdata: LLSD): Boolean {
        val commandName = userdata.asString()

        if (commandName == "profile_item" ||
            commandName == "block_voice" ||
            commandName == "block_text" ||
            commandName == "block_particles" ||
            commandName == "block_obj_sounds"
        ) {
            val panels = mutableListOf<Panel>()
            getSelectedItems(panels)
            return if (panels.size == 1) {
                val item = panels.first() as? BlockedListItem
                item != null && Mute.EType.AGENT == item.type
            } else {
                false
            }
        }

        if (commandName == "unblock_item") {
            return getSelectedItem() != null
        }

        return true
    }

    private fun onCustomAction(userdata: LLSD) {
        if (!isActionEnabled(userdata)) return

        val commandName = userdata.asString()

        when (commandName) {
            "unblock_item" -> {
                val panels = mutableListOf<Panel>()
                getSelectedItems(panels)
                for (panel in panels) {
                    val item = panel as? BlockedListItem ?: continue
                    val mute = Mute(item.itemId, item.name)
                    MuteList.instance.remove(mute)
                }
            }
            "profile_item" -> {
                val item = getBlockedItem() ?: return
                if (item.type == Mute.EType.AGENT) {
                    AvatarActions.showProfile(item.itemId)
                }
            }
            "block_voice" -> toggleMute(Mute.FLAG_VOICE_CHAT)
            "block_text" -> toggleMute(Mute.FLAG_TEXT_CHAT)
            "block_particles" -> toggleMute(Mute.FLAG_PARTICLES)
            "block_obj_sounds" -> toggleMute(Mute.FLAG_OBJECT_SOUNDS)
        }
    }

    private fun isMenuItemChecked(userdata: LLSD): Boolean {
        val item = getBlockedItem() ?: return false
        return when (userdata.asString()) {
            "block_voice" -> MuteList.instance.isMuted(item.itemId, Mute.FLAG_VOICE_CHAT)
            "block_text" -> MuteList.instance.isMuted(item.itemId, Mute.FLAG_TEXT_CHAT)
            "block_particles" -> MuteList.instance.isMuted(item.itemId, Mute.FLAG_PARTICLES)
            "block_obj_sounds" -> MuteList.instance.isMuted(item.itemId, Mute.FLAG_OBJECT_SOUNDS)
            else -> false
        }
    }

    private fun isMenuItemVisible(userdata: LLSD): Boolean {
        val item = getBlockedItem()
        return when (userdata.asString()) {
            "block_voice", "block_text", "block_particles", "block_obj_sounds" ->
                item != null && Mute.EType.AGENT == item.type
            else -> false
        }
    }

    private fun toggleMute(flags: UInt) {
        val item = getBlockedItem() ?: return
        val mute = Mute(item.itemId, item.name, item.type)
        if (!MuteList.instance.isMuted(item.itemId, flags)) {
            MuteList.instance.add(mute, flags)
        } else {
            MuteList.instance.remove(mute, flags)
        }
    }
}

abstract class BlockListItemComparator : FlatListView.ItemComparator {
    override fun compare(item1: Panel, item2: Panel): Boolean {
        val blocked1 = item1 as? BlockedListItem
            ?: error("blocked_item1 cannot be null")
        val blocked2 = item2 as? BlockedListItem
            ?: error("blocked_item2 cannot be null")
        return doCompare(blocked1, blocked2)
    }

    protected abstract fun doCompare(item1: BlockedListItem, item2: BlockedListItem): Boolean
}

class BlockListNameComparator : BlockListItemComparator() {
    override fun doCompare(item1: BlockedListItem, item2: BlockedListItem): Boolean {
        return item1.name.uppercase() < item2.name.uppercase()
    }
}

class BlockListNameTypeComparator : BlockListItemComparator() {
    private val nameComparator = BlockListNameComparator()

    override fun doCompare(item1: BlockedListItem, item2: BlockedListItem): Boolean {
        val type1 = item1.type
        val type2 = item2.type

        val bothAreObjects = (type1 == Mute.EType.OBJECT || type1 == Mute.EType.BY_NAME) &&
                             (type2 == Mute.EType.OBJECT || type2 == Mute.EType.BY_NAME)

        if (type1 != type2 && !bothAreObjects) {
            return Mute.EType.AGENT != type1
        }

        return nameComparator.compare(item1, item2)
    }
}
