package com.firestorm.newview

import java.util.UUID

private const val BLOCKED_PARAM_NAME = "blocked_to_select"

class PanelBlockedList : Panel() {

    enum class SortOrder {
        BY_NAME,
        BY_TYPE
    }

    private var blockedList: BlockList? = null
    private var blockLimitText: UICtrl? = null
    private var blockedGearBtn: MenuButton? = null
    private var unblockBtn: Button? = null
    private var picker: FloaterHandle? = null

    init {
        registerCommitCallback("Block.Action") { _, userdata -> onCustomAction(userdata) }
        registerEnableCallback("Block.Check") { _, userdata -> isActionChecked(userdata) }
    }

    override fun postBuild(): Boolean {
        blockedList = getChild<BlockList>("blocked")
        blockedList?.setCommitOnSelectionChange(true)
        setVisibleCallback { removePicker() }

        val sortOrder = SavedSettings.getUInt("BlockPeopleSortOrder")
        when (sortOrder) {
            SortOrder.BY_NAME.ordinal.toUInt() -> blockedList?.sortByName()
            SortOrder.BY_TYPE.ordinal.toUInt() -> blockedList?.sortByType()
        }

        blockedGearBtn = getChild<MenuButton>("blocked_gear_btn")
        val blockedGearMenu = blockedList?.getContextMenu() as? ToggleableMenu
        if (blockedGearMenu != null) {
            blockedGearBtn?.setMenu(blockedGearMenu, MenuButton.MP_BOTTOM_LEFT)
        }

        unblockBtn = getChild<Button>("unblock_btn")
        unblockBtn?.setCommitCallback { unblockItem() }

        getChild<FilterEditor>("blocked_filter_input")?.setCommitCallback { searchString ->
            onFilterEdit(searchString)
        }

        blockLimitText = getChild<UICtrl>("block_limit")

        return super.postBuild()
    }

    override fun draw() {
        updateButtons()
        super.draw()
    }

    override fun onOpen(key: LLSD) {
        if (key.has(BLOCKED_PARAM_NAME) && key[BLOCKED_PARAM_NAME].asUUID() != null) {
            selectBlocked(key[BLOCKED_PARAM_NAME].asUUID()!!)
        }
    }

    fun selectBlocked(muteId: UUID) {
        blockedList?.resetSelection()
        blockedList?.selectItemByUUID(muteId)
    }

    private fun removePicker() {
        picker?.get()?.closeFloater()
    }

    private fun updateButtons() {
        val hasSelected = blockedList?.getSelectedItem() != null
        unblockBtn?.setEnabled(hasSelected)
        blockedGearBtn?.setEnabled(hasSelected)

        val muteListLimit = SavedSettings.getInt("MuteListLimit")
        blockLimitText?.setTextArg("[COUNT]", blockedList?.getMuteListSize()?.toString() ?: "0")
        blockLimitText?.setTextArg("[LIMIT]", muteListLimit.toString())
    }

    private fun unblockItem() {
        val item = blockedList?.getBlockedItem() ?: return
        val mute = Mute(item.getUUID(), item.getName())
        MuteList.instance().remove(mute)
    }

    private fun blockResidentByName() {
        val button = findChild<Button>("plus_btn")
        val rootFloater = FloaterView.getInstance().getParentFloater(this)
        val picker = FloaterAvatarPicker.show(
            callback = { ids, names -> callbackBlockPicked(ids, names) },
            allowMultiple = false,
            closeOnSelect = true,
            showNearMe = false,
            excludeFloaterName = rootFloater?.getName() ?: "",
            positionView = button
        )
        rootFloater?.addDependentFloater(picker)
        this.picker = picker?.getHandle()
    }

    private fun blockObjectByName() {
        FloaterGetBlockedObjectName.show { text -> callbackBlockByName(text) }
    }

    private fun onFilterEdit(searchString: String) {
        val filter = searchString.trimStart()
        blockedList?.setNameFilter(filter)
    }

    private fun onCustomAction(userdata: LLSD) {
        when (userdata.asString()) {
            "block_obj_by_name" -> blockObjectByName()
            "block_res_by_name" -> blockResidentByName()
            "sort_by_name" -> {
                blockedList?.sortByName()
                SavedSettings.setUInt("BlockPeopleSortOrder", SortOrder.BY_NAME.ordinal.toUInt())
            }
            "sort_by_type" -> {
                blockedList?.sortByType()
                SavedSettings.setUInt("BlockPeopleSortOrder", SortOrder.BY_TYPE.ordinal.toUInt())
            }
        }
    }

    private fun isActionChecked(userdata: LLSD): Boolean {
        val item = userdata.asString()
        val sortOrder = SavedSettings.getUInt("BlockPeopleSortOrder")
        return when (item) {
            "sort_by_name" -> sortOrder == SortOrder.BY_NAME.ordinal.toUInt()
            "sort_by_type" -> sortOrder == SortOrder.BY_TYPE.ordinal.toUInt()
            else -> false
        }
    }

    private fun callbackBlockPicked(ids: List<UUID>, names: List<AvatarName>) {
        if (names.isEmpty() || ids.isEmpty()) return
        val mute = Mute(ids[0], names[0].getUserName(), MuteType.AGENT)
        MuteList.getInstance().add(mute)
        showPanelAndSelect(mute.id)
    }

    companion object {
        fun showPanelAndSelect(idToSelect: UUID = UUID(0, 0)) {
            FSPanelBlockList.showPanelAndSelect(idToSelect)
        }

        fun callbackBlockByName(text: String) {
            if (text.isEmpty()) return
            val mute = Mute(null, text, MuteType.BY_NAME)
            val success = MuteList.getInstance().add(mute)
            if (!success) {
                NotificationsUtil.add("MuteByNameFailed")
            }
        }
    }
}

class FloaterGetBlockedObjectName(key: LLSD) : Floater(key) {

    typealias GetObjectNameCallback = (String) -> Unit

    private var getObjectNameCallback: GetObjectNameCallback? = null

    override fun postBuild(): Boolean {
        getChild<Button>("OK")?.setCommitCallback { applyBlocking() }
        getChild<Button>("Cancel")?.setCommitCallback { cancelBlocking() }
        center()
        return super.postBuild()
    }

    override fun handleKeyHere(key: Key, mask: Mask): Boolean {
        return when {
            key == Key.RETURN && mask == Mask.NONE -> {
                applyBlocking()
                true
            }
            key == Key.ESCAPE && mask == Mask.NONE -> {
                cancelBlocking()
                true
            }
            else -> super.handleKeyHere(key, mask)
        }
    }

    private fun applyBlocking() {
        val cb = getObjectNameCallback
        if (cb != null) {
            val text = getChild<UICtrl>("object_name")?.getValue()?.asString() ?: ""
            cb(text)
        }
        closeFloater()
    }

    private fun cancelBlocking() {
        closeFloater()
    }

    companion object {
        fun show(callback: (String) -> Unit): FloaterGetBlockedObjectName? {
            val floater = FloaterReg.showTypedInstance<FloaterGetBlockedObjectName>("mute_object_by_name")
            floater?.getObjectNameCallback = callback
            return floater
        }
    }
}
