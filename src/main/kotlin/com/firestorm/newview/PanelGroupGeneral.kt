package com.firestorm.newview

import java.util.UUID

const val MATURE_CONTENT = 1
const val NON_MATURE_CONTENT = 2
const val DECLINE_TO_STATE = 0

class PanelGroupGeneral : PanelGroupTab() {

    private var changed: Boolean = false
    private var firstUse: Boolean = true
    private var incompleteMemberDataStr: String = ""
    private var groupName: String = ""

    private var groupNameEditor: LineEditor? = null
    private var founderName: TextBox? = null
    private var insignia: TextureCtrl? = null
    private var editCharter: TextEditor? = null

    private var ctrlShowInGroupList: CheckBoxCtrl? = null
    private var ctrlOpenEnrollment: CheckBoxCtrl? = null
    private var ctrlEnrollmentFee: CheckBoxCtrl? = null
    private var spinEnrollmentFee: SpinCtrl? = null
    private var ctrlReceiveNotices: CheckBoxCtrl? = null
    private var ctrlListGroup: CheckBoxCtrl? = null
    private var activeTitleLabel: TextBox? = null
    private var comboActiveTitle: ComboBox? = null
    private var comboMature: ComboBox? = null
    private var ctrlReceiveGroupChat: CheckBoxCtrl? = null

    private var iteratorGroup: UUID = UUID(0, 0)

    private var pendingMemberUpdate: Boolean = false
    private var listVisibleMembers: NameListCtrl? = null
    private val avatarNameCacheConnections: MutableMap<UUID, Any> = mutableMapOf()

    override fun postBuild(): Boolean {
        editCharter = findChild<TextEditor>("charter")
        editCharter?.let {
            it.setCommitCallback { onCommitAny() }
            it.setFocusCallback { onFocusEdit() }
            it.setContentTrusted(false)
        }

        findChild<Button>("copy_uri")?.setCommitCallback { onCopyUri() }
        findChild<Button>("copy_name")?.let {
            it.setCommitCallback { onCopyName() }
            it.setEnabled(false)
        }

        listVisibleMembers = findChild<NameListCtrl>("visible_members")
        listVisibleMembers?.let {
            it.setDoubleClickCallback { openProfile() }
            it.setSortCallback { colIdx, i1, i2 -> sortMembersList(colIdx, i1, i2) }
        }

        ctrlShowInGroupList = findChild<CheckBoxCtrl>("show_in_group_list")
        ctrlShowInGroupList?.setCommitCallback { onCommitAny() }

        comboMature = findChild<ComboBox>("group_mature_check")
        comboMature?.let {
            it.setCurrentByIndex(0)
            it.setCommitCallback { onCommitAny() }
            if (Agent.isTeen()) {
                it.setVisible(false)
                it.setCurrentByIndex(NON_MATURE_CONTENT)
            }
        }

        ctrlOpenEnrollment = findChild<CheckBoxCtrl>("open_enrollement")
        ctrlOpenEnrollment?.setCommitCallback { onCommitAny() }

        ctrlEnrollmentFee = findChild<CheckBoxCtrl>("check_enrollment_fee")
        ctrlEnrollmentFee?.setCommitCallback { onCommitEnrollment() }

        spinEnrollmentFee = findChild<SpinCtrl>("spin_enrollment_fee")
        spinEnrollmentFee?.let {
            it.setCommitCallback { onCommitAny() }
            it.setPrecision(0)
            it.resetDirty()
        }

        var acceptNotices = false
        var listInProfile = false
        val data = Agent.getGroupData(groupId)
        if (data != null) {
            acceptNotices = data.acceptNotices
            listInProfile = data.listInProfile
        }

        ctrlReceiveNotices = findChild<CheckBoxCtrl>("receive_notices")
        ctrlReceiveNotices?.let {
            it.setCommitCallback { onCommitUserOnly() }
            it.set(acceptNotices)
            it.setEnabled(data?.id != null && data.id != UUID(0, 0))
        }

        ctrlReceiveGroupChat = findChild<CheckBoxCtrl>("receive_chat")
        ctrlReceiveGroupChat?.let {
            it.setCommitCallback { onCommitUserOnly() }
            it.setEnabled(data?.id != null && data.id != UUID(0, 0))
            if (data?.id != null && data.id != UUID(0, 0)) {
                it.set(!ExoGroupMuteList.isMuted(data.id))
            }
        }

        ctrlListGroup = findChild<CheckBoxCtrl>("list_groups_in_profile")
        ctrlListGroup?.let {
            it.setCommitCallback { onCommitUserOnly() }
            it.set(listInProfile)
            it.setEnabled(data?.id != null && data.id != UUID(0, 0))
            it.resetDirty()
        }

        activeTitleLabel = findChild<TextBox>("active_title_label")
        comboActiveTitle = findChild<ComboBox>("active_title")
        comboActiveTitle?.setCommitCallback { onCommitAny() }

        incompleteMemberDataStr = getString("incomplete_member_data_str")

        if (groupId == UUID(0, 0)) {
            editCharter?.setEnabled(true)
            ctrlShowInGroupList?.setEnabled(true)
            comboMature?.setEnabled(true)
            ctrlOpenEnrollment?.setEnabled(true)
            ctrlEnrollmentFee?.setEnabled(true)
            spinEnrollmentFee?.setEnabled(true)
        }

        return super.postBuild()
    }

    fun setupCtrls(parentPanel: Panel) {
        insignia = findChild<TextureCtrl>("insignia")
        insignia?.let {
            it.setCommitCallback { onCommitAny() }
            it.setAllowLocalTexture(false)
            it.setBakeTextureEnabled(false)
        }
        founderName = findChild<TextBox>("founder_name")
        groupNameEditor = parentPanel.findChild<LineEditor>("group_name_editor")
        groupNameEditor?.setPrevalidate { input -> input.isNotBlank() }
    }

    private fun onFocusEdit() {
        updateChanged()
        notifyObservers()
    }

    private fun onCommitAny() {
        updateChanged()
        notifyObservers()
    }

    private fun onCommitUserOnly() {
        changed = true
        notifyObservers()
    }

    private fun onCommitEnrollment() {
        onCommitAny()
        if (ctrlEnrollmentFee == null || spinEnrollmentFee == null) return
        if (!Agent.hasPowerInGroup(groupId, GP_MEMBER_OPTIONS) || !allowEdit) return

        if (ctrlEnrollmentFee!!.get()) {
            spinEnrollmentFee!!.setEnabled(true)
        } else {
            spinEnrollmentFee!!.setEnabled(false)
            spinEnrollmentFee!!.set(0f)
        }
    }

    override fun needsApply(mesg: StringBuilder): Boolean {
        updateChanged()
        mesg.clear()
        mesg.append(getString("group_info_unchanged"))
        return changed || groupId == UUID(0, 0)
    }

    override fun activate() {
        val gdatap = GroupMgr.getGroupData(groupId)
        if (groupId != UUID(0, 0) && (gdatap == null || firstUse)) {
            GroupMgr.sendGroupTitlesRequest(groupId)
            GroupMgr.sendGroupPropertiesRequest(groupId)
            if (gdatap == null || !gdatap.isMemberDataComplete()) {
                GroupMgr.sendCapGroupMembersRequest(groupId)
            }
            firstUse = false
        }
        changed = false
        update(GroupChange.GC_ALL)
    }

    override fun draw() {
        super.draw()
        if (pendingMemberUpdate) updateMembers()
    }

    override fun apply(mesg: StringBuilder): Boolean {
        if (groupId == UUID(0, 0)) return false

        if (groupId != UUID(0, 0) && allowEdit && comboActiveTitle?.isDirty() == true) {
            GroupMgr.sendGroupTitleUpdate(groupId, comboActiveTitle!!.getCurrentId())
            update(GroupChange.GC_TITLES)
            comboActiveTitle!!.resetDirty()
        }

        val hasPowerInGroup = Agent.hasPowerInGroup(groupId, GP_GROUP_CHANGE_IDENTITY)
        if (hasPowerInGroup) {
            if (comboMature?.getCurrentIndex() == DECLINE_TO_STATE) {
                NotificationsUtil.add("SetGroupMature") { notification, response ->
                    confirmMatureApply(notification, response)
                }
                return false
            }

            val gdatap = GroupMgr.getGroupData(groupId) ?: run {
                mesg.append(LLTrans.getString("NoGroupDataFound"))
                mesg.append(groupId.toString())
                return false
            }

            val canChangeIdent = Agent.hasPowerInGroup(groupId, GP_GROUP_CHANGE_IDENTITY)
            val canChangeMemberOpts = Agent.hasPowerInGroup(groupId, GP_MEMBER_OPTIONS)

            if (canChangeIdent) {
                editCharter?.let { gdatap.charter = it.getText() }
                insignia?.let { gdatap.insigniaId = it.getImageAssetId() }
                comboMature?.let {
                    if (!Agent.isTeen()) {
                        gdatap.maturePublish = it.getCurrentIndex() == MATURE_CONTENT
                    } else {
                        gdatap.maturePublish = false
                    }
                }
                ctrlShowInGroupList?.let { gdatap.showInList = it.get() }
            }

            if (canChangeMemberOpts) {
                ctrlOpenEnrollment?.let { gdatap.openEnrollment = it.get() }
                if (ctrlEnrollmentFee != null && spinEnrollmentFee != null) {
                    gdatap.membershipFee = if (ctrlEnrollmentFee!!.get()) spinEnrollmentFee!!.get().toInt() else 0
                    spinEnrollmentFee!!.set(gdatap.membershipFee.toFloat())
                }
            }

            if (canChangeIdent || canChangeMemberOpts) {
                GroupMgr.sendUpdateGroupInfo(groupId)
            }
        }

        val receiveNotices = ctrlReceiveNotices?.get() ?: false
        val listInProfile = ctrlListGroup?.get() ?: false
        Agent.setUserGroupFlags(groupId, receiveNotices, listInProfile)

        ctrlReceiveGroupChat?.let {
            if (it.get()) ExoGroupMuteList.remove(groupId) else ExoGroupMuteList.add(groupId)
        }

        resetDirty()
        changed = false
        return true
    }

    override fun cancel() {
        changed = false
        notifyObservers()
    }

    private fun confirmMatureApply(notification: Any, response: Any): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        when (option) {
            0 -> comboMature?.setCurrentByIndex(MATURE_CONTENT)
            1 -> comboMature?.setCurrentByIndex(NON_MATURE_CONTENT)
            else -> return false
        }
        val mesg = StringBuilder()
        return apply(mesg).also {
            if (mesg.isNotEmpty()) NotificationsUtil.add("GenericAlert", mapOf("MESSAGE" to mesg.toString()))
        }
    }

    override fun update(gc: GroupChange) {
        if (groupId == UUID(0, 0)) return
        val gdatap = GroupMgr.getGroupData(groupId) ?: return

        val agentData = Agent.getGroupData(groupId)
        val isMember = agentData != null

        comboActiveTitle?.let { combo ->
            combo.setVisible(isMember)
            combo.setEnabled(allowEdit)
            activeTitleLabel?.setVisible(isMember)

            if (isMember) {
                combo.clear()
                var hasSelectedTitle = false
                combo.setEnabled(gdatap.titles.size > 1)

                for (title in gdatap.titles) {
                    combo.add(title.title, title.roleId, if (title.selected) ADD_TOP else ADD_BOTTOM)
                    if (title.selected) {
                        combo.setCurrentById(title.roleId)
                        hasSelectedTitle = true
                    }
                }
                if (!hasSelectedTitle) combo.setCurrentById(UUID(0, 0))
            }
        }

        if (gc == GroupChange.GC_ROLE_MEMBER_DATA) GroupMgr.sendGroupTitlesRequest(groupId)
        if (gc == GroupChange.GC_TITLES) return

        val canChangeIdent = Agent.hasPowerInGroup(groupId, GP_GROUP_CHANGE_IDENTITY)
        val canChangeMemberOpts = Agent.hasPowerInGroup(groupId, GP_MEMBER_OPTIONS)

        ctrlShowInGroupList?.let { it.set(gdatap.showInList); it.setEnabled(allowEdit && canChangeIdent) }
        comboMature?.let {
            it.setCurrentByIndex(if (gdatap.maturePublish) MATURE_CONTENT else NON_MATURE_CONTENT)
            it.setEnabled(allowEdit && canChangeIdent)
            it.setVisible(!Agent.isTeen())
        }
        ctrlOpenEnrollment?.let { it.set(gdatap.openEnrollment); it.setEnabled(allowEdit && canChangeMemberOpts) }
        ctrlEnrollmentFee?.let { it.set(gdatap.membershipFee > 0); it.setEnabled(allowEdit && canChangeMemberOpts) }
        spinEnrollmentFee?.let {
            it.set(gdatap.membershipFee.toFloat())
            it.setEnabled(allowEdit && gdatap.membershipFee > 0 && canChangeMemberOpts)
        }
        ctrlReceiveNotices?.let {
            it.setVisible(isMember)
            if (isMember) it.setEnabled(allowEdit)
        }
        ctrlReceiveGroupChat?.let {
            it.setVisible(isMember)
            if (isMember) it.setEnabled(allowEdit)
        }
        insignia?.setEnabled(allowEdit && canChangeIdent)
        editCharter?.setEnabled(allowEdit && canChangeIdent)

        groupNameEditor?.setVisible(false)
        founderName?.setText(SLUrl("agent", gdatap.founderId, "inspect").getSLUrlString())
        insignia?.let {
            if (gdatap.insigniaId != UUID(0, 0)) it.setImageAssetId(gdatap.insigniaId)
            else it.setImageAssetName(it.getDefaultImageName())
        }
        editCharter?.let {
            it.setParseUrls(!allowEdit || !canChangeIdent)
            it.setText(gdatap.charter)
        }

        listVisibleMembers?.let { list ->
            list.deleteAllItems()
            if (gdatap.isMemberDataComplete()) {
                pendingMemberUpdate = true
                iteratorGroup = groupId
            } else {
                val pending = "Retrieving member list (${gdatap.members.size}\\${gdatap.memberCount})"
                list.setEnabled(false)
                list.addElement(mapOf("name" to pending))
            }
        }

        findChild<Button>("copy_name")?.setEnabled(gdatap.name.isNotEmpty())
        groupName = gdatap.name

        resetDirty()
    }

    private fun updateChanged() {
        val checkList = listOfNotNull(
            groupNameEditor, founderName, insignia, editCharter,
            ctrlShowInGroupList, comboMature, ctrlOpenEnrollment, ctrlEnrollmentFee,
            spinEnrollmentFee, ctrlReceiveNotices, ctrlListGroup, activeTitleLabel,
            comboActiveTitle, ctrlReceiveGroupChat
        )
        changed = checkList.any { it.isDirty() }
    }

    private fun reset() {
        founderName?.setVisible(false)
        ctrlReceiveNotices?.set(false)
        ctrlListGroup?.set(true)
        ctrlReceiveNotices?.setEnabled(false)
        ctrlReceiveNotices?.setVisible(true)
        ctrlListGroup?.setEnabled(false)
        groupNameEditor?.setEnabled(true)
        editCharter?.setEnabled(true)
        ctrlShowInGroupList?.setEnabled(false)
        comboMature?.setEnabled(true)
        ctrlOpenEnrollment?.setEnabled(true)
        ctrlEnrollmentFee?.setEnabled(true)
        spinEnrollmentFee?.setEnabled(true)
        spinEnrollmentFee?.set(0f)
        groupNameEditor?.setVisible(true)
        comboActiveTitle?.setVisible(false)
        insignia?.setImageAssetId(UUID(0, 0))
        insignia?.setEnabled(true)
        insignia?.setImageAssetName(insignia!!.getDefaultImageName())
        ctrlReceiveGroupChat?.set(false)
        ctrlReceiveGroupChat?.setEnabled(false)
        ctrlReceiveGroupChat?.setVisible(true)
        editCharter?.setText("")
        groupNameEditor?.setText("")

        listVisibleMembers?.let {
            it.deleteAllItems()
            it.setEnabled(false)
            it.addElement(mapOf("name" to "no members yet"))
        }

        comboMature?.let {
            it.setEnabled(true)
            it.setVisible(!Agent.isTeen())
            it.selectFirstItem()
        }
        resetDirty()
    }

    private fun resetDirty() {
        listOfNotNull(
            groupNameEditor, founderName, insignia, editCharter,
            ctrlShowInGroupList, comboMature, ctrlOpenEnrollment, ctrlEnrollmentFee,
            spinEnrollmentFee, ctrlReceiveNotices, ctrlListGroup, activeTitleLabel,
            comboActiveTitle, ctrlReceiveGroupChat
        ).forEach { it.resetDirty() }
    }

    override fun setGroupId(id: UUID) {
        super.setGroupId(id)
        val groupKeyEditor = findChild<TextEditor>("group_key")
        val copyUriButton = findChild<Button>("copy_uri")
        val copyNameButton = findChild<Button>("copy_name")

        if (id == UUID(0, 0)) {
            groupKeyEditor?.setValue(null)
            copyUriButton?.setEnabled(false)
            copyNameButton?.setEnabled(false)
            reset()
            return
        }

        groupKeyEditor?.setValue(id.toString())
        copyUriButton?.setEnabled(true)

        val data = Agent.getGroupData(groupId)
        val acceptNotices = data?.acceptNotices ?: false
        val listInProfile = data?.listInProfile ?: false

        ctrlReceiveNotices = findChild<CheckBoxCtrl>("receive_notices")
        ctrlReceiveNotices?.let {
            it.set(acceptNotices)
            it.setEnabled(data?.id != null && data.id != UUID(0, 0))
        }

        ctrlListGroup = findChild<CheckBoxCtrl>("list_groups_in_profile")
        ctrlListGroup?.let {
            it.set(listInProfile)
            it.setEnabled(data?.id != null && data.id != UUID(0, 0))
        }

        ctrlReceiveGroupChat = findChild<CheckBoxCtrl>("receive_chat")
        ctrlReceiveGroupChat?.let {
            if (data?.id != null && data.id != UUID(0, 0)) {
                it.set(!ExoGroupMuteList.isMuted(data.id))
            }
            it.setEnabled(data?.id != null && data.id != UUID(0, 0))
        }

        ctrlShowInGroupList?.setEnabled(data?.id != null && data.id != UUID(0, 0))
        activeTitleLabel = findChild<TextBox>("active_title_label")
        comboActiveTitle = findChild<ComboBox>("active_title")
        founderName?.setVisible(true)
        insignia?.setImageAssetId(UUID(0, 0))

        resetDirty()
        activate()
    }

    protected fun onCopyUri() {
        TODO("APR: use JVM clipboard — copy SLUrl for group $groupId")
    }

    protected fun onCopyName() {
        TODO("APR: use JVM clipboard — copy groupName string")
    }

    private fun openProfile() {
        val selected = listVisibleMembers?.getFirstSelected() ?: return
        AvatarActions.showProfile(selected.id)
    }

    private fun updateMembers() {
        pendingMemberUpdate = false
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        if (listVisibleMembers == null || !gdatap.isMemberDataComplete() || gdatap.members.isEmpty()) return

        if (iteratorGroup != groupId) {
            iteratorGroup = groupId
        }

        for ((id, conn) in avatarNameCacheConnections) {
            (conn as? AutoCloseable)?.close()
        }
        avatarNameCacheConnections.clear()

        for ((memberId, member) in gdatap.members) {
            if (member == null) continue
            val avName = AvatarNameCache.get(memberId)
            if (avName != null) {
                addMember(member)
            } else {
                TODO("APR: async fetch avatar name for $memberId, then addMember(member)")
            }
        }

        val allDone = true
        listVisibleMembers?.setEnabled(allDone)
        if (!allDone) pendingMemberUpdate = true
    }

    private fun addMember(member: GroupMemberData) {
        listVisibleMembers?.addNameItemRow(
            id = member.id,
            name = "",
            title = member.getTitle(),
            status = member.getOnlineStatus(),
            bold = member.isOwner()
        )
    }

    fun onNameCache(updateId: UUID, member: GroupMemberData, avName: AvatarName, avId: UUID) {
        avatarNameCacheConnections.remove(avId)
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        if (!gdatap.isMemberDataComplete() || gdatap.getMemberVersion() != updateId) return
        addMember(member)
    }

    fun refreshInsigniaTexture() {
        TODO("GPU: destroy_texture for insignia texture ID to force reload")
    }

    private fun sortMembersList(colIdx: Int, i1: ScrollListItem, i2: ScrollListItem): Int {
        val v1 = i1.getColumnValue(colIdx)
        val v2 = i2.getColumnValue(colIdx)
        if (colIdx == 2) {
            if (v1 == "Online") return 1
            if (v2 == "Online") return -1
        }
        return v1.compareTo(v2, ignoreCase = true)
    }
}

abstract class PanelGroupTab {
    var groupId: UUID = UUID(0, 0)
    var allowEdit: Boolean = true

    abstract fun postBuild(): Boolean
    abstract fun activate()
    abstract fun draw()
    abstract fun apply(mesg: StringBuilder): Boolean
    abstract fun cancel()
    abstract fun needsApply(mesg: StringBuilder): Boolean
    abstract fun update(gc: GroupChange)

    open fun setGroupId(id: UUID) { groupId = id }
    fun notifyObservers() { TODO("APR: observer notification") }
    fun getString(key: String): String { TODO("APR: localized string lookup"); return key }
    fun <T> findChild(name: String): T? { TODO("GPU: UI child widget lookup"); return null }
}

enum class GroupChange { GC_ALL, GC_TITLES, GC_ROLE_MEMBER_DATA, GC_MEMBER_DATA, GC_PROPERTIES }

object GroupMgr {
    fun getGroupData(id: UUID): GroupMgrGroupData? = TODO("APR: group manager lookup")
    fun sendGroupTitlesRequest(id: UUID) = TODO("APR: send message")
    fun sendGroupPropertiesRequest(id: UUID) = TODO("APR: send message")
    fun sendCapGroupMembersRequest(id: UUID) = TODO("APR: send message")
    fun sendGroupTitleUpdate(groupId: UUID, roleId: UUID) = TODO("APR: send message")
    fun sendUpdateGroupInfo(groupId: UUID) = TODO("APR: send message")
    fun sendGroupRoleDataRequest(groupId: UUID) = TODO("APR: send message")
}

class GroupMgrGroupData {
    var charter: String = ""
    var insigniaId: UUID = UUID(0, 0)
    var maturePublish: Boolean = false
    var showInList: Boolean = true
    var openEnrollment: Boolean = false
    var membershipFee: Int = 0
    var founderId: UUID = UUID(0, 0)
    var name: String = ""
    var memberCount: Int = 0
    val titles: List<GroupTitle> = emptyList()
    val members: Map<UUID, GroupMemberData?> = emptyMap()
    val ownerRole: UUID = UUID(0, 0)
    val mRoleActionSets: List<Any> = emptyList()

    fun isMemberDataComplete(): Boolean = TODO("APR: check completeness")
    fun isRoleDataComplete(): Boolean = TODO("APR: check completeness")
    fun isGroupPropertiesDataComplete(): Boolean = TODO("APR: check completeness")
    fun getMemberVersion(): UUID = TODO("APR: member version UUID")
}

data class GroupTitle(val title: String, val roleId: UUID, val selected: Boolean)
data class GroupData(val id: UUID, val acceptNotices: Boolean, val listInProfile: Boolean)

class GroupMemberData {
    val id: UUID = UUID(0, 0)
    fun getTitle(): String = TODO("APR: member title")
    fun getOnlineStatus(): String = TODO("APR: online status")
    fun isOwner(): Boolean = TODO("APR: ownership check")
    fun isInRole(roleId: UUID): Boolean = TODO("APR: role membership check")
}

object Agent {
    val id: UUID get() = TODO("APR: current agent UUID")
    val region: ViewerRegion? get() = TODO("APR: current region")
    fun isTeen(): Boolean = TODO("APR: teen check")
    fun isGodlike(): Boolean = TODO("APR: god check")
    fun isInGroup(groupId: UUID): Boolean = TODO("APR: group membership check")
    fun hasPowerInGroup(groupId: UUID, power: Long): Boolean = TODO("APR: power check")
    fun getGroupData(groupId: UUID): GroupData? = TODO("APR: group data for agent")
    fun setUserGroupFlags(groupId: UUID, receiveNotices: Boolean, listInProfile: Boolean) = TODO("APR: set flags")
}

object AvatarActions {
    fun showProfile(id: UUID) = TODO("APR: open avatar profile")
    fun isFriend(id: UUID): Boolean = TODO("APR: friendship check")
}

object ExoGroupMuteList {
    fun isMuted(groupId: UUID): Boolean = TODO("APR: check group mute")
    fun add(groupId: UUID) = TODO("APR: add group to mute list")
    fun remove(groupId: UUID) = TODO("APR: remove group from mute list")
}

object NotificationsUtil {
    fun add(name: String, args: Map<String, String> = emptyMap(), callback: ((Any, Any) -> Boolean)? = null) = TODO("APR: notification")
    fun getSelectedOption(notification: Any, response: Any): Int = TODO("APR: response option")
}

object LLTrans {
    fun getString(key: String): String = TODO("APR: localized string")
}

class SLUrl(type: String, id: UUID, action: String) {
    fun getSLUrlString(): String = TODO("APR: build SLURL string")
}

abstract class Panel {
    fun <T> findChild(name: String): T? = TODO("GPU: find UI child")
}
abstract class TextBox : UiCtrl() { fun setText(s: String) {} }
abstract class LineEditor : UiCtrl() { fun setPrevalidate(fn: (String) -> Boolean) {} fun setText(s: String) {} }
abstract class TextEditor : UiCtrl() { fun getText(): String = ""; fun setText(s: String) {} fun setContentTrusted(v: Boolean) {} fun setParseUrls(v: Boolean) {} fun setFocusCallback(fn: () -> Unit) {} fun clear() {} fun setValue(v: Any?) {} }
abstract class CheckBoxCtrl : UiCtrl() { fun get(): Boolean = false; fun set(v: Boolean) {} }
abstract class SpinCtrl : UiCtrl() { fun get(): Float = 0f; fun set(v: Float) {} fun setPrecision(p: Int) {} }
abstract class ComboBox : UiCtrl() { fun getCurrentIndex(): Int = 0; fun setCurrentByIndex(i: Int) {} fun getCurrentId(): UUID = UUID(0, 0); fun setCurrentById(id: UUID) {} fun add(label: String, id: UUID, position: Int = ADD_BOTTOM) {} fun clear() {} fun removeall() {} fun isDirty(): Boolean = false; fun selectFirstItem() {} }
abstract class TextureCtrl : UiCtrl() { fun getImageAssetId(): UUID = UUID(0, 0); fun setImageAssetId(id: UUID) {} fun getDefaultImageName(): String = ""; fun setImageAssetName(name: String) {} fun getTexture(): Any? = null; fun setAllowLocalTexture(v: Boolean) {} fun setBakeTextureEnabled(v: Boolean) {} }
abstract class NameListCtrl : UiCtrl() { fun deleteAllItems() {} fun addElement(data: Map<String, String>) {} fun addNameItemRow(id: UUID, name: String, title: String, status: String, bold: Boolean) {} fun setEnabled(v: Boolean) {} fun getFirstSelected(): ScrollListItem? = null; fun setSortCallback(fn: (Int, ScrollListItem, ScrollListItem) -> Int) {} }
abstract class Button : UiCtrl() { fun setCommitCallback(fn: () -> Unit) {} }

abstract class UiCtrl {
    fun setEnabled(v: Boolean) {}
    fun setVisible(v: Boolean) {}
    fun setCommitCallback(fn: () -> Unit) {}
    fun isDirty(): Boolean = false
    fun resetDirty() {}
    fun setFocus(v: Boolean) {}
}

const val ADD_TOP = 0
const val ADD_BOTTOM = 1
const val GP_MEMBER_OPTIONS: Long = 1L shl 0
const val GP_GROUP_CHANGE_IDENTITY: Long = 1L shl 1
const val GP_MEMBER_INVITE: Long = 1L shl 2
const val GP_MEMBER_EJECT: Long = 1L shl 3
const val GP_NOTICES_SEND: Long = 1L shl 4
const val GP_NOTICES_RECEIVE: Long = 1L shl 5
const val GP_ROLE_CHANGE_ACTIONS: Long = 1L shl 6
const val GP_ROLE_REMOVE_MEMBER: Long = 1L shl 7
const val GP_ROLE_ASSIGN_MEMBER: Long = 1L shl 8
const val GP_ROLE_ASSIGN_MEMBER_LIMITED: Long = 1L shl 9
const val GP_GROUP_BAN_ACCESS: Long = 1L shl 10
const val GP_MEMBER_VISIBLE_IN_DIR: Long = 1L shl 11
const val GP_ALL_POWERS: Long = -1L
