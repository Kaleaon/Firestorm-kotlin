package com.firestorm.newview

import java.util.UUID

typealias Signal<T> = (T) -> Unit

fun initGroupList(groupList: ScrollListCtrl, highlightId: UUID, powersMask: ULong = GP_ALL_POWERS) {
    groupList.clearAll()
    val agent = Agent.instance
    for (groupData in agent.groups) {
        if (powersMask == GP_ALL_POWERS || (groupData.powers and powersMask) != 0UL) {
            val style = if (highlightId == groupData.id) "BOLD" else "NORMAL"
            groupList.addElement(
                id = groupData.id,
                name = groupData.name,
                fontName = "SANSSERIF",
                fontStyle = style
            )
        }
    }
    groupList.sortOnce(columnIndex = 0, ascending = true)

    val noneStyle = if (highlightId == UUID(0, 0)) "BOLD" else "NORMAL"
    groupList.addElementAtTop(
        id = UUID(0, 0),
        name = Trans.getString("GroupsNone"),
        fontName = "SANSSERIF",
        fontStyle = noneStyle
    )
    groupList.selectByValue(highlightId)
}

open class FloaterGroupPicker(val seed: LLSD) : Floater(seed) {

    var id: UUID = seed.asUUID()
    var powersMask: ULong = GP_ALL_POWERS
        set(value) {
            field = value
            getChild<ScrollListCtrl>("group list")?.let {
                initGroupList(it, Agent.instance.groupId, powersMask)
            }
        }

    private val groupSelectSignal = mutableListOf<Signal<UUID>>()

    fun setSelectGroupCallback(cb: Signal<UUID>) {
        groupSelectSignal += cb
    }

    override fun postBuild(): Boolean {
        val listCtrl = getChild<ScrollListCtrl>("group list")
        if (listCtrl != null) {
            initGroupList(listCtrl, Agent.instance.groupId, powersMask)
            listCtrl.setDoubleClickCallback { onBtnOK() }
            listCtrl.setContextMenu(ScrollListCtrl.MENU_GROUP)
        }
        setChildAction("OK") { onBtnOK() }
        setChildAction("Cancel") { closeFloater() }
        setDefaultBtn("OK")
        getChildView("OK")?.isEnabled = true
        return true
    }

    fun removeNoneOption() {
        val groupList = getChild<ScrollListCtrl>("group list")?.getListInterface() ?: return
        groupList.selectByValue(UUID(0, 0))
        groupList.operateOnSelection(ListInterface.OP_DELETE)
    }

    private fun ok() {
        val groupList = getChildListInterface("group list")
        val groupId = groupList?.getCurrentId() ?: UUID(0, 0)
        for (cb in groupSelectSignal) cb(groupId)
        closeFloater()
    }

    private fun onBtnOK() = ok()

    companion object {
        val instances: MutableMap<UUID, FloaterGroupPicker> = mutableMapOf()
    }
}

open class PanelGroups : Panel(), SimpleListener {

    init {
        Agent.instance.addListener(this, "new group")
    }

    override fun handleEvent(event: Event, userData: LLSD): Boolean {
        if (event.desc() == "new group") {
            reset()
            return true
        }
        return false
    }

    fun reset() {
        getChildListInterface("group list")?.operateOnAll(ListInterface.OP_DELETE)
        getChild<UiCtrl>("groupcount")?.setValue(FSCommon.populateGroupCount())
        getChild<ScrollListCtrl>("group list")?.let {
            initGroupList(it, Agent.instance.groupId)
        }
        enableButtons()
    }

    override fun postBuild(): Boolean {
        setChildCommitCallback("group list") { enableButtons() }
        getChild<UiCtrl>("groupcount")?.setValue(FSCommon.populateGroupCount())

        getChild<ScrollListCtrl>("group list")?.let { list ->
            initGroupList(list, Agent.instance.groupId)
            list.setDoubleClickCallback { startIM() }
            list.setContextMenu(ScrollListCtrl.MENU_GROUP)
        }
        setChildAction("Activate") { activate() }
        setChildAction("Info") { info() }
        setChildAction("IM") { startIM() }
        setChildAction("Leave") { leave() }
        setChildAction("Create") { create() }
        setChildAction("Search...") { search() }
        setDefaultBtn("IM")
        reset()
        return true
    }

    fun enableButtons() {
        val groupId = getChildListInterface("group list")?.getCurrentId() ?: UUID(0, 0)
        val agent = Agent.instance
        getChildView("Activate")?.isEnabled = groupId != agent.groupId
        val nonNull = groupId != UUID(0, 0)
        getChildView("Info")?.isEnabled = nonNull
        getChildView("IM")?.isEnabled = nonNull
        getChildView("Leave")?.isEnabled = nonNull
        getChildView("Create")?.isEnabled = agent.canJoinGroups()
    }

    private fun create() = GroupActions.createGroup()

    private fun activate() {
        val groupId = getChildListInterface("group list")?.getCurrentId() ?: UUID(0, 0)
        GroupActions.activate(groupId)
    }

    private fun info() {
        val groupId = getChildListInterface("group list")?.getCurrentId() ?: return
        if (groupId != UUID(0, 0)) GroupActions.show(groupId)
    }

    private fun startIM() {
        val groupId = getChildListInterface("group list")?.getCurrentId() ?: return
        if (groupId != UUID(0, 0)) GroupActions.startIM(groupId)
    }

    private fun leave() {
        val groupId = getChildListInterface("group list")?.getCurrentId() ?: return
        if (groupId != UUID(0, 0)) GroupActions.leave(groupId)
    }

    private fun search() = GroupActions.search()

    override fun onDestroy() {
        Agent.instance.removeListener(this)
    }
}
