package com.firestorm.newview

import java.util.UUID

private const val BLOCKED_PARAM_NAME = "blocked_to_select"

class FSPanelBlockList : LLPanel(), LLMuteListObserver {

    private enum class SortOrder {
        BY_NAME_ASC,
        BY_TYPE_ASC,
        BY_NAME_DESC,
        BY_TYPE_DESC
    }

    private object Col {
        const val NAME     = 0
        const val TYPENAME = 1
        const val TYPE     = 2
        const val UUID_COL = 3
    }

    private var blockedList: FSScrollListCtrl? = null
    private var avatarPicker: LLHandle<LLFloater>? = null
    private var objectPicker: LLHandle<LLFloater>? = null
    private var filterSubString: String = ""
    private var filterSubStringOrig: String = ""

    init {
        registerCommitCallback("Block.Action")  { _, userdata -> onCustomAction(userdata) }
        registerEnableCallback("Block.Check")   { _, userdata -> isActionChecked(userdata) }
        registerEnableCallback("Block.Enable")  { _, userdata -> isActionEnabled(userdata) }
        registerEnableCallback("Block.Visible") { _, userdata -> isActionVisible(userdata) }
    }

    override fun postBuild(): Boolean {
        blockedList = getChild<FSScrollListCtrl>("block_list").also { list ->
            list.setCommitOnSelectionChange(true)
            list.setCommitCallback { onSelectionChanged() }
            list.setDoubleClickCallback { showProfile() }
            list.setSearchColumn(list.getColumn("item_name").index)
            list.setContextMenu(gFSBlockListMenu)
            list.setFilterColumn(Col.NAME)
            list.setSortChangedCallback { onSortChanged() }
        }

        getChild<LLButton>("unblock_btn").setCommitCallback { removeMutes() }
        getChild<LLFilterEditor>("blocked_filter_input").setCommitCallback { _, value -> onFilterEdit(value.asString()) }

        LLMuteList.getInstance().addObserver(this)
        refreshBlockedList()
        setVisibleCallback { removePicker() }
        updateButtons()

        return super.postBuild()
    }

    override fun onOpen(key: LLSD) {
        if (key.has(BLOCKED_PARAM_NAME) && key[BLOCKED_PARAM_NAME].asUUID() != null) {
            selectBlocked(key[BLOCKED_PARAM_NAME].asUUID())
        }
    }

    override fun handleKeyHere(key: KEY, mask: MASK): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            getChild<LLFilterEditor>("blocked_filter_input").setFocus(true)
            return true
        }
        return super.handleKeyHere(key, mask)
    }

    override fun hasAccelerators(): Boolean = true

    override fun onChange() {
        refreshBlockedList()
    }

    fun selectBlocked(muteId: UUID) {
        val list = blockedList ?: return
        list.deselectAllItems()
        for (item in list.getAllData()) {
            if (item.getColumn(Col.UUID_COL).getValue().asUUID() == muteId) {
                item.setSelected(true)
                break
            }
        }
        list.scrollToShowSelected()
    }

    private fun removePicker() {
        avatarPicker?.get()?.closeFloater()
        objectPicker?.get()?.closeFloater()
    }

    fun destroy() {
        LLMuteList.getInstance().removeObserver(this)
    }

    private fun refreshBlockedList() {
        val list = blockedList ?: return
        list.deleteAllItems()

        for (mute in LLMuteList.getInstance().getMutes()) {
            val params = LLScrollListItem.Params().apply {
                enabled = true
                value = UUID.randomUUID()
                columns.add(LLScrollListCell.Params("item_name",    mute.name))
                columns.add(LLScrollListCell.Params("item_type",    mute.getDisplayType()))
                columns.add(LLScrollListCell.Params("item_mute_type", mute.type))
                columns.add(LLScrollListCell.Params("item_mute_uuid", mute.id))
            }
            list.addRow(params)
        }
        list.refreshLineHeight()

        val blockLimit = getChild<LLUICtrl>("block_limit")
        blockLimit.setTextArg("[COUNT]", LLMuteList.getInstance().getMutes().size.toString())
        blockLimit.setTextArg("[LIMIT]", gSavedSettings.getInt("MuteListLimit").toString())
    }

    private fun updateButtons() {
        val hasSelection = (blockedList?.getNumSelected() ?: 0) > 0
        getChildView("blocked_gear_btn").setEnabled(hasSelection)
        getChildView("unblock_btn").setEnabled(hasSelection)
    }

    private fun removeMutes() {
        val list = blockedList ?: return
        val scrollPos = list.getScrollPos()
        val lastSelected = list.getFirstSelectedIndex()

        // Remove observer before bulk operation to avoid per-removal list refreshes that would crash.
        LLMuteList.getInstance().removeObserver(this)
        for (item in list.getAllSelected()) {
            val name = item.getColumn(Col.NAME).getValue().asString()
            val id   = item.getColumn(Col.UUID_COL).getValue().asUUID()
            LLMuteList.getInstance().remove(LLMute(id, name))
        }
        LLMuteList.getInstance().addObserver(this)
        refreshBlockedList()

        if (lastSelected == list.getItemCount()) {
            list.selectNthItem(lastSelected - 1)
        } else {
            list.selectNthItem(lastSelected)
        }
        onSelectionChanged()
        list.setScrollPos(scrollPos)
    }

    private fun onCustomAction(userdata: LLSD) {
        when (val cmd = userdata.asString()) {
            "block_obj_by_name"  -> blockObjectByName()
            "block_res_by_name"  -> blockResidentByName()
            "sort_by_name"       -> { blockedList?.sortByColumn("item_name", true);  gSavedSettings.setUInt("BlockPeopleSortOrder", SortOrder.BY_NAME_ASC.ordinal.toUInt()) }
            "sort_by_type"       -> { blockedList?.sortByColumn("item_type", true);  gSavedSettings.setUInt("BlockPeopleSortOrder", SortOrder.BY_TYPE_ASC.ordinal.toUInt()) }
            "sort_by_name_desc"  -> { blockedList?.sortByColumn("item_name", false); gSavedSettings.setUInt("BlockPeopleSortOrder", SortOrder.BY_NAME_DESC.ordinal.toUInt()) }
            "sort_by_type_desc"  -> { blockedList?.sortByColumn("item_type", false); gSavedSettings.setUInt("BlockPeopleSortOrder", SortOrder.BY_TYPE_DESC.ordinal.toUInt()) }
            "unblock_item"       -> removeMutes()
            "profile_item"       -> showProfile()
            "block_voice"        -> toggleMute(LLMute.FLAG_VOICE_CHAT)
            "block_text"         -> toggleMute(LLMute.FLAG_TEXT_CHAT)
            "block_particles"    -> toggleMute(LLMute.FLAG_PARTICLES)
            "block_obj_sounds"   -> toggleMute(LLMute.FLAG_OBJECT_SOUNDS)
        }
    }

    private fun isActionChecked(userdata: LLSD): Boolean {
        val cmd = userdata.asString()
        val sortOrder = gSavedSettings.getUInt("BlockPeopleSortOrder").toInt()

        return when (cmd) {
            "sort_by_name"      -> sortOrder == SortOrder.BY_NAME_ASC.ordinal
            "sort_by_type"      -> sortOrder == SortOrder.BY_TYPE_ASC.ordinal
            "sort_by_name_desc" -> sortOrder == SortOrder.BY_NAME_DESC.ordinal
            "sort_by_type_desc" -> sortOrder == SortOrder.BY_TYPE_DESC.ordinal
            else -> {
                val selected = blockedList?.getFirstSelected() ?: return false
                val id = selected.getColumn(Col.UUID_COL).getValue().asUUID()
                when (cmd) {
                    "block_voice"      -> LLMuteList.getInstance().isMuted(id, LLMute.FLAG_VOICE_CHAT)
                    "block_text"       -> LLMuteList.getInstance().isMuted(id, LLMute.FLAG_TEXT_CHAT)
                    "block_particles"  -> LLMuteList.getInstance().isMuted(id, LLMute.FLAG_PARTICLES)
                    "block_obj_sounds" -> LLMuteList.getInstance().isMuted(id, LLMute.FLAG_OBJECT_SOUNDS)
                    else               -> false
                }
            }
        }
    }

    private fun isActionEnabled(userdata: LLSD): Boolean {
        return when (val cmd = userdata.asString()) {
            "unblock_item" -> (blockedList?.getNumSelected() ?: 0) > 0
            "profile_item", "block_voice", "block_text", "block_particles", "block_obj_sounds" -> {
                val list = blockedList ?: return false
                list.getNumSelected() == 1 &&
                    list.getFirstSelected()?.getColumn(Col.TYPE)?.getValue()?.asInteger() == LLMute.AGENT
            }
            else -> false
        }
    }

    private fun isActionVisible(userdata: LLSD): Boolean {
        return when (userdata.asString()) {
            "block_voice", "block_text", "block_particles", "block_obj_sounds" -> {
                val list = blockedList ?: return false
                list.getNumSelected() == 1 &&
                    list.getFirstSelected()?.getColumn(Col.TYPE)?.getValue()?.asInteger() == LLMute.AGENT
            }
            else -> false
        }
    }

    private fun toggleMute(flags: UInt) {
        val item = blockedList?.getFirstSelected() ?: return
        val id    = item.getColumn(Col.UUID_COL).getValue().asUUID()
        val name  = item.getColumn(Col.NAME).getValue().asString()
        val type  = item.getColumn(Col.TYPE).getValue().asInteger()
        val mute  = LLMute(id, name, type)

        if (!LLMuteList.getInstance().isMuted(id, flags)) {
            LLMuteList.getInstance().add(mute, flags)
        } else {
            LLMuteList.getInstance().remove(mute, flags)
        }
    }

    private fun blockResidentByName() {
        val picker = LLFloaterAvatarPicker.show(
            allowMultiple = false,
            closeOnSelect = true,
            callback = { ids, names -> callbackBlockPicked(ids, names) }
        )
        val parent = getParent() as? LLFloater
        parent?.addDependentFloater(picker)
        avatarPicker = picker.getHandle()
    }

    private fun blockObjectByName() {
        val picker = LLFloaterGetBlockedObjectName.show { text -> callbackBlockByName(text) }
        val parent = getParent() as? LLFloater
        parent?.addDependentFloater(picker)
        objectPicker = picker.getHandle()
    }

    private fun onSelectionChanged() {
        updateButtons()
    }

    private fun showProfile() {
        val list = blockedList ?: return
        if (list.getNumSelected() == 1 &&
            list.getFirstSelected()?.getColumn(Col.TYPE)?.getValue()?.asInteger() == LLMute.AGENT
        ) {
            val id = list.getFirstSelected()!!.getColumn(Col.UUID_COL).getValue().asUUID()
            LLAvatarActions.showProfile(id)
        }
    }

    private fun callbackBlockPicked(ids: List<UUID>, names: List<LLAvatarName>) {
        if (ids.isEmpty() || names.isEmpty()) return
        val mute = LLMute(ids[0], names[0].getUserName(), LLMute.AGENT)
        LLMuteList.getInstance().add(mute)
        showPanelAndSelect(mute.id)
    }

    private fun callbackBlockByName(text: String) {
        if (text.isEmpty()) return
        val mute = LLMute(null, text, LLMute.BY_NAME)
        val success = LLMuteList.getInstance().add(mute)
        if (!success) {
            LLNotificationsUtil.add("MuteByNameFailed")
        } else {
            blockedList?.selectItemByLabel(text)
            blockedList?.scrollToShowSelected()
        }
    }

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val searchUpper = filterSubStringOrig.uppercase()
        if (filterSubString == searchUpper) return
        filterSubString = searchUpper
        blockedList?.setFilterString(filterSubStringOrig)
    }

    private fun onSortChanged() {
        val list = blockedList ?: return
        val ascending = list.getSortAscending()
        val column = list.getSortColumnName()

        val order = when {
            column == "item_name" && ascending  -> SortOrder.BY_NAME_ASC
            column == "item_name" && !ascending -> SortOrder.BY_NAME_DESC
            column == "item_type" && ascending  -> SortOrder.BY_TYPE_ASC
            column == "item_type" && !ascending -> SortOrder.BY_TYPE_DESC
            else -> return
        }
        gSavedSettings.setUInt("BlockPeopleSortOrder", order.ordinal.toUInt())
    }

    companion object {
        fun showPanelAndSelect(idToSelect: UUID = NULL_UUID) {
            if (gSavedSettings.getBool("FSDisableBlockListAutoOpen")) return

            if (gSavedSettings.getBool("FSUseStandaloneBlocklistFloater")) {
                LLFloaterReg.showInstance("fs_blocklist", LLSD().with(BLOCKED_PARAM_NAME, idToSelect))
            } else {
                LLFloaterSidePanelContainer.showPanel(
                    "people", "panel_people",
                    LLSD().with("people_panel_tab_name", "blocked_panel").with(BLOCKED_PARAM_NAME, idToSelect)
                )
            }
        }
    }
}
