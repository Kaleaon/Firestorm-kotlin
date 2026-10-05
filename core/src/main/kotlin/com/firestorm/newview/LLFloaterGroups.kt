package com.firestorm.newview

import java.util.UUID

class LLFloaterGroupPicker(private val seed: LLSD) : LLFloater(seed) {

    private var mID: UUID = seed.asUUID()
    private var mPowersMask: ULong = GP_ALL_POWERS
    private val mGroupSelectSignal: MutableList<(UUID) -> Unit> = mutableListOf()

    companion object {
        private val sInstances: MutableMap<UUID, LLFloaterGroupPicker> = mutableMapOf()
    }

    fun setSelectGroupCallback(cb: (UUID) -> Unit) {
        mGroupSelectSignal.add(cb)
    }

    fun setPowersMask(powersMask: ULong) {
        mPowersMask = powersMask
        initGroupList(getChild<LLScrollListCtrl>("group list"), gAgent.getGroupID(), mPowersMask)
    }

    override fun postBuild(): Boolean {
        val listCtrl = getChild<LLScrollListCtrl>("group list")
        initGroupList(listCtrl, gAgent.getGroupID(), mPowersMask)
        listCtrl.setDoubleClickCallback { onBtnOK() }
        listCtrl.setContextMenu(LLScrollListCtrl.MENU_GROUP)

        childSetAction("OK") { onBtnOK() }
        childSetAction("Cancel") { onBtnCancel() }
        setDefaultBtn("OK")
        getChildView("OK").setEnabled(true)
        return true
    }

    fun removeNoneOption() {
        val groupList = getChild<LLScrollListCtrl>("group list").getListInterface()
        groupList?.apply {
            selectByValue(UUID(0, 0))
            operateOnSelection(LLCtrlListInterface.OP_DELETE)
        }
    }

    private fun onBtnOK() {
        ok()
    }

    private fun onBtnCancel() {
        closeFloater()
    }

    private fun ok() {
        val groupList = childGetListInterface("group list")
        val groupId: UUID = groupList?.getCurrentID() ?: UUID(0, 0)
        mGroupSelectSignal.forEach { it(groupId) }
        closeFloater()
    }
}


class LLPanelGroups : LLPanel(), LLOldEvents.LLSimpleListener {

    init {
        gAgent.addListener(this, "new group")
    }

    override fun onDestroy() {
        gAgent.removeListener(this)
    }

    override fun handleEvent(event: LLOldEvents.LLEvent, userdata: LLSD): Boolean {
        if (event.desc() == "new group") {
            reset()
            return true
        }
        return false
    }

    fun reset() {
        childGetListInterface("group list")?.operateOnAll(LLCtrlListInterface.OP_DELETE)
        getChild<LLUICtrl>("groupcount").setValue(FSCommon.populateGroupCount())
        initGroupList(getChild<LLScrollListCtrl>("group list"), gAgent.getGroupID())
        enableButtons()
    }

    override fun postBuild(): Boolean {
        childSetCommitCallback("group list") { _, _ -> enableButtons() }
        getChild<LLUICtrl>("groupcount").setValue(FSCommon.populateGroupCount())

        val list = getChild<LLScrollListCtrl>("group list")
        initGroupList(list, gAgent.getGroupID())
        list.setDoubleClickCallback { startIM() }
        list.setContextMenu(LLScrollListCtrl.MENU_GROUP)

        childSetAction("Activate") { activate() }
        childSetAction("Info") { info() }
        childSetAction("IM") { startIM() }
        childSetAction("Leave") { leave() }
        childSetAction("Create") { create() }
        childSetAction("Search...") { search() }
        setDefaultBtn("IM")
        reset()
        return true
    }

    private fun enableButtons() {
        val groupList = childGetListInterface("group list")
        val groupId: UUID = groupList?.getCurrentID() ?: UUID(0, 0)

        getChildView("Activate").setEnabled(groupId != gAgent.getGroupID())

        val notNull = groupId != UUID(0, 0)
        getChildView("Info").setEnabled(notNull)
        getChildView("IM").setEnabled(notNull)
        getChildView("Leave").setEnabled(notNull)
        getChildView("Create").setEnabled(gAgent.canJoinGroups())
    }

    private fun create() {
        LLGroupActions.createGroup()
    }

    private fun activate() {
        val groupId = childGetListInterface("group list")?.getCurrentID() ?: UUID(0, 0)
        LLGroupActions.activate(groupId)
    }

    private fun info() {
        val groupList = childGetListInterface("group list") ?: return
        val groupId = groupList.getCurrentID()
        if (groupId != UUID(0, 0)) {
            LLGroupActions.show(groupId)
        }
    }

    private fun startIM() {
        val groupList = childGetListInterface("group list") ?: return
        val groupId = groupList.getCurrentID()
        if (groupId != UUID(0, 0)) {
            LLGroupActions.startIM(groupId)
        }
    }

    private fun leave() {
        val groupList = childGetListInterface("group list") ?: return
        val groupId = groupList.getCurrentID()
        if (groupId != UUID(0, 0)) {
            LLGroupActions.leave(groupId)
        }
    }

    private fun search() {
        LLGroupActions.search()
    }
}


fun initGroupList(groupList: LLScrollListCtrl?, highlightId: UUID, powersMask: ULong = GP_ALL_POWERS) {
    groupList ?: return
    groupList.operateOnAll(LLCtrlListInterface.OP_DELETE)

    val count = gAgent.mGroups.size
    for (i in 0 until count) {
        val groupData = gAgent.mGroups[i]
        val id = groupData.mID
        if (powersMask == GP_ALL_POWERS || (groupData.mPowers and powersMask) != 0UL) {
            val style = if (highlightId == id) "BOLD" else "NORMAL"
            val element = LLSD().apply {
                this["id"] = id
                this["columns"][0]["column"] = "name"
                this["columns"][0]["value"] = groupData.mName
                this["columns"][0]["font"]["name"] = "SANSSERIF"
                this["columns"][0]["font"]["style"] = style
            }
            groupList.addElement(element)
        }
    }

    groupList.sortOnce(0, true)

    val noneStyle = if (highlightId == UUID(0, 0)) "BOLD" else "NORMAL"
    val noneElement = LLSD().apply {
        this["id"] = UUID(0, 0)
        this["columns"][0]["column"] = "name"
        this["columns"][0]["value"] = LLTrans.getString("GroupsNone")
        this["columns"][0]["font"]["name"] = "SANSSERIF"
        this["columns"][0]["font"]["style"] = noneStyle
    }
    groupList.addElement(noneElement, ADD_TOP)

    groupList.selectByValue(highlightId)
}
