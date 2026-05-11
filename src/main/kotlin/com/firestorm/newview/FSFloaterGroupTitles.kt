package com.firestorm.newview

import java.util.UUID

class FSGroupTitlesObserver(
    private val groupData: LLGroupData,
    private val parent: WeakRef<FSFloaterGroupTitles>
) : LLGroupMgrObserver(groupData.id) {

    init {
        LLGroupMgr.instance().addObserver(this)
    }

    fun destroy() {
        LLGroupMgr.instance().removeObserver(this)
    }

    override fun changed(gc: LLGroupChange) {
        if (gc == LLGroupChange.GC_TITLES) {
            parent.get()?.processGroupTitleResults(groupData)
        }
    }
}

class FSFloaterGroupTitles(key: LLSD) : LLFloater(key), LLGroupMgrObserver(UUID.randomUUID()), LLSimpleListener {

    private var activateButton: LLButton? = null
    private var refreshButton: LLButton? = null
    private var infoButton: LLButton? = null
    private var setRegionButton: LLButton? = null
    private var setRegionManualButton: LLButton? = null
    private var clearRegionButton: LLButton? = null
    private var noneOnUnassigned: LLCheckBoxCtrl? = null
    private var titleList: LLScrollListCtrl? = null
    private var filterEditor: LLFilterEditor? = null

    private var filterSubString: String = ""
    private var filterSubStringOrig: String = ""

    private var assignmentsChangedConnection: (() -> Unit)? = null
    private var clearRegionMenuHandle: LLContextMenu? = null

    private val groupTitleObserverMap: MutableMap<UUID, FSGroupTitlesObserver> = mutableMapOf()

    init {
        LLGroupMgr.getInstance().addObserver(this)
        gAgent.addListener(this, "update grouptitle list")
    }

    fun destroy() {
        gAgent.removeListener(this)
        LLGroupMgr.getInstance().removeObserver(this)
        assignmentsChangedConnection = null
        clearRegionMenuHandle?.let { menu ->
            gMenuHolder.removeChild(menu)
        }
        clearRegionMenuHandle = null
        clearObservers()
    }

    override fun postBuild(): Boolean {
        activateButton = getChild<LLButton>("btnActivate")
        refreshButton = getChild<LLButton>("btnRefresh")
        infoButton = getChild<LLButton>("btnInfo")
        setRegionButton = getChild<LLButton>("btnSetRegion")
        setRegionManualButton = getChild<LLButton>("btnSetRegionManual")
        clearRegionButton = getChild<LLButton>("btnClearRegion")
        noneOnUnassigned = getChild<LLCheckBoxCtrl>("none_on_unassigned")
        titleList = getChild<LLScrollListCtrl>("title_list")
        filterEditor = getChild<LLFilterEditor>("filter_input")

        activateButton!!.setCommitCallback { activateGroupTitle() }
        refreshButton!!.setCommitCallback { refreshGroupTitles() }
        infoButton!!.setCommitCallback { openGroupInfo() }
        setRegionButton!!.setCommitCallback { onSetRegion() }
        setRegionManualButton!!.setCommitCallback { onSetRegionManual() }
        clearRegionButton!!.setCommitCallback { onClearRegion() }
        noneOnUnassigned!!.setCommitCallback { onNoneOnUnassignedToggle() }
        titleList!!.setDoubleClickCallback { activateGroupTitle() }
        titleList!!.setCommitCallback { selectedTitleChanged() }
        filterEditor!!.setCommitCallback { _, value -> onFilterEdit(value.asString()) }

        noneOnUnassigned!!.set(FSGroupTitleRegionMgr.getInstance().getNoneOnUnassigned())
        setRegionButton!!.setEnabled(false)
        setRegionManualButton!!.setEnabled(false)
        clearRegionButton!!.setEnabled(false)
        assignmentsChangedConnection = FSGroupTitleRegionMgr.getInstance()
            .setAssignmentsChangedCallback { updateRegionColumn() }

        titleList!!.sortByColumn("title_sort_column", true)
        titleList!!.setFilterColumn(0)

        refreshGroupTitles()
        return true
    }

    override fun onOpen(key: LLSD) {
        super.onOpen(key)
        titleList!!.setFocus(true)
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            filterEditor!!.setFocus(true)
            return true
        }
        return super.handleKeyHere(key, mask)
    }

    override fun hasAccelerators(): Boolean = true

    override fun changed(gc: LLGroupChange) {
        when (gc) {
            LLGroupChange.GC_MEMBER_DATA,
            LLGroupChange.GC_ROLE_MEMBER_DATA,
            LLGroupChange.GC_TITLES -> refreshGroupTitles()
            else -> Unit
        }
    }

    override fun handleEvent(event: LLEvent, userdata: LLSD): Boolean {
        if (event.desc() == "update grouptitle list") {
            refreshGroupTitles()
            return true
        }
        return false
    }

    fun processGroupTitleResults(groupData: LLGroupData) {
        val gmgrData = LLGroupMgr.getInstance().getGroupData(groupData.id) ?: return
        if (gmgrData.titles.isEmpty()) return

        val groupName = groupData.name
        val groupTitles = gmgrData.titles

        for (groupTitle in groupTitles) {
            val isActiveTitle = groupTitle.selected && groupData.id == gAgent.getGroupID()
            addListItem(groupData.id, groupTitle.roleId, groupTitle.title, groupName, isActiveTitle)
        }

        titleList!!.scrollToShowSelected()

        val found = groupTitleObserverMap.remove(groupData.id)
        found?.destroy()
    }

    private fun clearObservers() {
        for ((_, observer) in groupTitleObserverMap) {
            observer.destroy()
        }
        groupTitleObserverMap.clear()
    }

    private fun addListItem(
        groupId: UUID,
        roleId: UUID,
        title: String,
        groupName: String,
        isActive: Boolean,
        isGroup: Boolean = true
    ) {
        val fontStyle = if (isActive) "BOLD" else "NORMAL"
        val regionName = FSGroupTitleRegionMgr.getInstance().getRegionForTitle(groupId, roleId)

        val item = LLSD()
        // Combination of group_id + role_id is unique; use it as row key.
        item["id"] = groupId.toString() + roleId.toString()
        item["columns"][0]["column"] = "grouptitle"
        item["columns"][0]["type"] = "text"
        item["columns"][0]["font"]["style"] = fontStyle
        item["columns"][0]["value"] = title
        item["columns"][1]["column"] = "groupname"
        item["columns"][1]["type"] = "text"
        item["columns"][1]["font"]["style"] = fontStyle
        item["columns"][1]["value"] = groupName
        item["columns"][2]["column"] = "regionname"
        item["columns"][2]["type"] = "text"
        item["columns"][2]["font"]["style"] = fontStyle
        item["columns"][2]["value"] = regionName
        item["columns"][3]["column"] = "role_id"
        item["columns"][3]["type"] = "text"
        item["columns"][3]["value"] = roleId.toString()
        item["columns"][4]["column"] = "group_id"
        item["columns"][4]["type"] = "text"
        item["columns"][4]["value"] = groupId.toString()
        item["columns"][5]["column"] = "title_sort_column"
        item["columns"][5]["type"] = "text"
        item["columns"][5]["value"] = if (isGroup) "1_$title" else "0"
        item["columns"][6]["column"] = "name_sort_column"
        item["columns"][6]["type"] = "text"
        item["columns"][6]["value"] = if (isGroup) "1_$groupName" else "0"

        titleList!!.addElement(item)

        if (isActive) {
            // selectByValue avoids duplicate selections on login.
            titleList!!.selectByValue(groupId.toString() + roleId.toString())
        }
    }

    private fun refreshGroupTitles() {
        clearObservers()
        titleList!!.clearRows()

        addListItem(
            UUID(0, 0), UUID(0, 0),
            getString("NoGroupTitle"),
            LLTrans.getString("GroupsNone"),
            gAgent.getGroupID() == UUID(0, 0),
            isGroup = false
        )

        for (groupData in gAgent.groups) {
            val observer = FSGroupTitlesObserver(groupData, WeakRef(this))
            groupTitleObserverMap[groupData.id] = observer
            LLGroupMgr.getInstance().sendGroupTitlesRequest(groupData.id)
        }
    }

    private fun activateGroupTitle() {
        val selectedItem = titleList!!.getFirstSelected() ?: return
        val groupId = selectedItem.getColumn(titleList!!.getColumn("group_id").index).getValue().asUUID()
        val roleId = selectedItem.getColumn(titleList!!.getColumn("role_id").index).getValue().asUUID()

        if (groupId != UUID(0, 0)) {
            LLGroupMgr.getInstance().sendGroupTitleUpdate(groupId, roleId)
        }

        if (gAgent.getGroupID() != groupId) {
            LLGroupActions.activate(groupId)
        }
    }

    private fun selectedTitleChanged() {
        val selectedItem = titleList!!.getFirstSelected()
        if (selectedItem != null) {
            val groupId = selectedItem.getColumn(titleList!!.getColumn("group_id").index).getValue().asUUID()
            val roleId = selectedItem.getColumn(titleList!!.getColumn("role_id").index).getValue().asUUID()
            infoButton!!.setEnabled(groupId != UUID(0, 0))
            val hasRegion = FSGroupTitleRegionMgr.getInstance().getRegionForTitle(groupId, roleId).isNotEmpty()
            setRegionButton!!.setEnabled(true)
            setRegionManualButton!!.setEnabled(true)
            clearRegionButton!!.setEnabled(hasRegion)
        } else {
            setRegionButton!!.setEnabled(false)
            setRegionManualButton!!.setEnabled(false)
            clearRegionButton!!.setEnabled(false)
        }
    }

    private fun openGroupInfo() {
        val selectedItem = titleList!!.getFirstSelected() ?: return
        val groupId = selectedItem.getColumn(titleList!!.getColumn("group_id").index).getValue().asUUID()
        LLGroupActions.show(groupId)
    }

    private fun onFilterEdit(searchString: String) {
        filterSubStringOrig = searchString.trimStart()
        val searchUpper = filterSubStringOrig.uppercase()

        if (filterSubString == searchUpper) return

        filterSubString = searchUpper
        titleList!!.setFilterString(filterSubStringOrig)
    }

    private fun onSetRegion() {
        val selectedItem = titleList!!.getFirstSelected() ?: return
        val groupId = selectedItem.getColumn(titleList!!.getColumn("group_id").index).getValue().asUUID()
        val roleId = selectedItem.getColumn(titleList!!.getColumn("role_id").index).getValue().asUUID()
        FSGroupTitleRegionMgr.getInstance().setAssignmentForCurrentRegion(groupId, roleId)
        selectedTitleChanged()
    }

    private fun onSetRegionManual() {
        val selectedItem = titleList!!.getFirstSelected() ?: return
        val groupId = selectedItem.getColumn(titleList!!.getColumn("group_id").index).getValue().asUUID()
        val roleId = selectedItem.getColumn(titleList!!.getColumn("role_id").index).getValue().asUUID()
        FSGroupTitleRegionMgr.getInstance().showRegionInputDialog(groupId, roleId)
    }

    private fun onClearRegion() {
        val selectedItem = titleList!!.getFirstSelected() ?: return
        val groupId = selectedItem.getColumn(titleList!!.getColumn("group_id").index).getValue().asUUID()
        val roleId = selectedItem.getColumn(titleList!!.getColumn("role_id").index).getValue().asUUID()
        val mgr = FSGroupTitleRegionMgr.getInstance()
        val regions = mgr.getRegionDisplayNamesForTitle(groupId, roleId)

        if (regions.isEmpty()) return

        if (regions.size == 1) {
            mgr.clearAssignmentByRegion(regions[0])
            selectedTitleChanged()
            return
        }

        // Multiple regions: build a context menu so the user can pick which to clear.
        clearRegionMenuHandle?.let { old ->
            gMenuHolder.removeChild(old)
        }

        val menu = LLContextMenu()
        for (region in regions) {
            menu.addItem(region) {
                mgr.clearAssignmentByRegion(region)
            }
        }
        menu.addSeparator()
        menu.addItem(getString("ClearAllRegions")) {
            mgr.clearAssignment(groupId, roleId)
        }

        clearRegionMenuHandle = menu
        gMenuHolder.addChild(menu)
        val (screenX, screenY) = clearRegionButton!!.localPointToScreen(0, clearRegionButton!!.getRect().height)
        menu.show(screenX, screenY, clearRegionButton!!)
    }

    private fun onNoneOnUnassignedToggle() {
        FSGroupTitleRegionMgr.getInstance().setNoneOnUnassigned(noneOnUnassigned!!.get())
    }

    private fun updateRegionColumn() {
        val regionColIdx = titleList!!.getColumn("regionname").index
        val groupIdColIdx = titleList!!.getColumn("group_id").index
        val roleIdColIdx = titleList!!.getColumn("role_id").index
        val mgr = FSGroupTitleRegionMgr.getInstance()

        for (item in titleList!!.getAllData()) {
            val groupId = item.getColumn(groupIdColIdx).getValue().asUUID()
            val roleId = item.getColumn(roleIdColIdx).getValue().asUUID()
            item.getColumn(regionColIdx)?.setValue(mgr.getRegionForTitle(groupId, roleId))
        }
    }
}
