package com.firestorm.newview

import java.util.UUID

fun agentCanRemoveFromRole(groupId: UUID, roleId: UUID): Boolean =
    Agent.hasPowerInGroup(groupId, GP_ROLE_REMOVE_MEMBER)

fun agentCanAddToRole(groupId: UUID, roleId: UUID): Boolean {
    if (Agent.isGodlike()) return true
    val gdatap = GroupMgr.getGroupData(groupId) ?: return false
    val memberData = gdatap.members[Agent.id] ?: return false
    if (memberData.isInRole(gdatap.ownerRole)) return true
    if (Agent.hasPowerInGroup(groupId, GP_ROLE_ASSIGN_MEMBER_LIMITED) && memberData.isInRole(roleId)) return true
    if (Agent.hasPowerInGroup(groupId, GP_ROLE_ASSIGN_MEMBER) && roleId != gdatap.ownerRole) return true
    return false
}

class PanelGroupRoles : PanelGroupTab() {

    var currentTab: PanelGroupTab? = null
    var requestedTab: PanelGroupTab? = null
    var subTabContainer: TabContainer? = null
    var firstUse: Boolean = true

    private var defaultNeedsApplyMesg: String = ""
    private var wantApplyMesg: String = ""

    override fun postBuild(): Boolean {
        subTabContainer = findChild<TabContainer>("roles_tab_container") ?: return false

        for (i in 0 until subTabContainer!!.getTabCount()) {
            val panel = subTabContainer!!.getPanelByIndex(i)
            val subtab = panel as? PanelGroupSubTab ?: return false
            if (!subtab.postBuildSubTab(this as Any)) return false
        }

        subTabContainer!!.setValidateBeforeCommit { data -> handleSubTabSwitch(data) }

        currentTab = subTabContainer!!.getCurrentPanel() as? PanelGroupTab
        if (currentTab == null) {
            subTabContainer!!.selectFirstTab()
            currentTab = subTabContainer!!.getCurrentPanel() as? PanelGroupTab
        }
        currentTab ?: return false
        currentTab!!.activate()

        defaultNeedsApplyMesg = getString("default_needs_apply_text")
        wantApplyMesg = getString("want_apply_text")

        return super.postBuild()
    }

    fun isVisibleByAgent(agent: Any): Boolean =
        allowEdit && Agent.isInGroup(groupId)

    fun handleSubTabSwitch(data: String): Boolean {
        if (requestedTab != null) return false
        requestedTab = subTabContainer?.getPanelByName(data) as? PanelGroupTab

        val mesg = StringBuilder()
        if (currentTab?.needsApply(mesg) == true) {
            val msg = if (mesg.isEmpty()) defaultNeedsApplyMesg else mesg.toString()
            NotificationsUtil.add(
                "PanelGroupApply",
                mapOf("NEEDS_APPLY_MESSAGE" to msg, "WANT_APPLY_MESSAGE" to wantApplyMesg)
            ) { notification, response -> handleNotifyCallback(notification, response) }
            hasModal = true
            return false
        }
        transitionToTab()
        return true
    }

    fun transitionToTab() {
        currentTab?.deactivate()
        requestedTab?.let {
            currentTab = it
            currentTab!!.activate()
            requestedTab = null
        }
    }

    fun handleNotifyCallback(notification: Any, response: Any): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        hasModal = false
        val transitionTab = requestedTab
        when (option) {
            0 -> {
                val applyMesg = StringBuilder()
                if (!apply(applyMesg)) {
                    if (applyMesg.isNotEmpty()) {
                        hasModal = true
                        NotificationsUtil.add("GenericAlert", mapOf("MESSAGE" to applyMesg.toString())) { n, r ->
                            onModalClose(n, r)
                        }
                    }
                    return false
                }
                transitionToTab()
                if (transitionTab != null) subTabContainer?.selectTabPanel(transitionTab)
            }
            1 -> {
                cancel()
                transitionToTab()
                if (transitionTab != null) subTabContainer?.selectTabPanel(transitionTab)
            }
            else -> requestedTab = null
        }
        return false
    }

    fun onModalClose(notification: Any, response: Any): Boolean {
        hasModal = false
        return false
    }

    override fun apply(mesg: StringBuilder): Boolean {
        val container = subTabContainer ?: return false
        val panel = container.getCurrentPanel() as? PanelGroupTab ?: return false
        val checkMesg = StringBuilder()
        if (!panel.needsApply(checkMesg)) return true
        return panel.apply(mesg)
    }

    override fun cancel() {
        val panel = subTabContainer?.getCurrentPanel() as? PanelGroupTab ?: return
        panel.cancel()
    }

    override fun update(gc: GroupChange) {
        if (groupId == UUID(0, 0)) return
        val panel = subTabContainer?.getCurrentPanel() as? PanelGroupTab ?: return
        panel.update(gc)
    }

    override fun activate() {
        if (!Agent.isInGroup(groupId)) return
        val gdatap = GroupMgr.getGroupData(groupId)
        if (gdatap == null || !gdatap.isRoleDataComplete()) {
            cancel()
            GroupMgr.sendGroupRoleDataRequest(groupId)
        }
        if (gdatap == null || !gdatap.isGroupPropertiesDataComplete()) {
            GroupMgr.sendGroupPropertiesRequest(groupId)
        }
        firstUse = false
        val panel = subTabContainer?.getCurrentPanel() as? PanelGroupTab
        panel?.activate()
    }

    fun deactivate() {
        val panel = subTabContainer?.getCurrentPanel() as? PanelGroupTab
        panel?.deactivate()
    }

    override fun needsApply(mesg: StringBuilder): Boolean {
        val panel = subTabContainer?.getCurrentPanel() as? PanelGroupTab ?: return false
        return panel.needsApply(mesg)
    }

    fun hasModal(): Boolean {
        if (hasModal) return true
        val panel = subTabContainer?.getCurrentPanel() as? PanelGroupTab ?: return false
        return panel.hasModal()
    }

    override fun draw() {}

    override fun setGroupId(id: UUID) {
        super.setGroupId(id)
        findChild<PanelGroupMembersSubTab>("members_sub_tab")?.setGroupId(id)
        findChild<PanelGroupRolesSubTab>("roles_sub_tab")?.let { it.setGroupId(id); it.firstOpen = true }
        findChild<PanelGroupActionsSubTab>("actions_sub_tab")?.setGroupId(id)
        findChild<PanelGroupBanListSubTab>("banlist_sub_tab")?.setGroupId(id)

        findChild<Button>("member_invite")?.setEnabled(Agent.hasPowerInGroup(groupId, GP_MEMBER_INVITE))
        findChild<Button>("export_list")?.setEnabled(Agent.hasPowerInGroup(groupId, GP_MEMBER_VISIBLE_IN_DIR))
        subTabContainer?.selectTab(1)
        activate()
    }

    fun getCurrentTab(): PanelGroupSubTab? =
        subTabContainer?.getCurrentPanel() as? PanelGroupSubTab

    var hasModal: Boolean = false
}


abstract class PanelGroupSubTab : PanelGroupTab() {

    protected var header: Panel? = null
    protected var footer: Panel? = null
    protected var searchEditor: FilterEditor? = null
    protected var searchFilter: String = ""
    protected val actionIcons: MutableMap<String, String> = mutableMapOf()
    protected var activated: Boolean = false
    protected var hasGroupBanPower: Boolean = false

    open fun postBuildSubTab(root: Any): Boolean {
        actionIcons.clear()
        if (hasString("power_folder_icon")) actionIcons["folder"] = getString("power_folder_icon")
        if (hasString("power_all_have_icon")) actionIcons["full"] = getString("power_all_have_icon")
        if (hasString("power_partial_icon")) actionIcons["partial"] = getString("power_partial_icon")
        return true
    }

    override fun postBuild(): Boolean {
        searchEditor = findChild<FilterEditor>("filter_input")
        searchEditor?.setCommitCallback { filter -> setSearchFilter(filter) }
        return super.postBuild()
    }

    override fun setGroupId(id: UUID) {
        super.setGroupId(id)
        searchEditor?.clear()
        setSearchFilter("")
        activated = false
    }

    open fun setSearchFilter(filter: String) {
        if (searchFilter == filter) return
        searchFilter = filter.lowercase()
        update(GroupChange.GC_ALL)
        onFilterChanged()
    }

    open fun onFilterChanged() {}

    override fun activate() { setOthersVisible(true); activated = true }
    open fun deactivate() { setOthersVisible(false) }

    fun setFooterEnabled(enable: Boolean) { footer?.setAllChildrenEnabled(enable) }
    fun setSearchFilterFocus(focus: Boolean) { searchEditor?.setFocus(focus) }

    private fun setOthersVisible(b: Boolean) {
        header?.setVisible(b)
        footer?.setVisible(b)
    }

    fun matchesActionSearchFilter(action: String): Boolean {
        if (searchFilter.isEmpty()) return true
        return action.lowercase().contains(searchFilter)
    }

    protected fun buildActionsList(
        ctrl: ScrollListCtrl,
        allowedBySome: Long,
        allowedByAll: Long,
        commitCallback: CommitCallback?,
        showAll: Boolean,
        filter: Boolean,
        isOwnerRole: Boolean
    ) {
        val actionSets = GroupMgr.getRoleActionSets()
        if (actionSets.isEmpty()) return
        hasGroupBanPower = false
        for (actionSet in actionSets) {
            buildActionCategory(ctrl, allowedBySome, allowedByAll, actionSet, commitCallback, showAll, filter, isOwnerRole)
        }
    }

    protected fun buildActionCategory(
        ctrl: ScrollListCtrl,
        allowedBySome: Long,
        allowedByAll: Long,
        actionSet: RoleActionSet,
        commitCallback: CommitCallback?,
        showAll: Boolean,
        filter: Boolean,
        isOwnerRole: Boolean
    ) {
        if (!showAll && (allowedBySome and actionSet.actionSetData.powerBit) == 0L) return

        val folderIcon = actionIcons["folder"] ?: ""
        val titleRow = ctrl.addElement(
            mapOf("icon" to folderIcon, "action" to actionSet.actionSetData.name, "bold" to true)
        )

        val categoryMatchesFilter = if (filter) matchesActionSearchFilter(actionSet.actionSetData.name) else true
        val canChangeActions = !isOwnerRole && Agent.hasPowerInGroup(groupId, GP_ROLE_CHANGE_ACTIONS)
        var itemsMatchFilter = false

        for (ra in actionSet.actions) {
            if (!showAll && (allowedBySome and ra.powerBit) == 0L) continue
            if (!categoryMatchesFilter && !matchesActionSearchFilter(ra.description)) continue
            itemsMatchFilter = true

            val showFullStrength = (allowedBySome and ra.powerBit) == (allowedByAll and ra.powerBit)
            val banConstraint = ra.powerBit == GP_MEMBER_EJECT || ra.powerBit == GP_ROLE_REMOVE_MEMBER
            val enabled = if (banConstraint) !hasGroupBanPower else true

            if (commitCallback != null) {
                val checked = if (showAll) (allowedBySome and ra.powerBit) != 0L else true
                val tentative = !showAll && !showFullStrength
                ctrl.addCheckboxElement(
                    icon = "",
                    label = ra.description,
                    checked = checked,
                    tentative = tentative,
                    enabled = canChangeActions && enabled,
                    callback = commitCallback
                )
            } else {
                val icon = if (showFullStrength) actionIcons["full"] ?: "" else actionIcons["partial"] ?: ""
                ctrl.addElement(mapOf("icon" to icon, "action" to ra.description, "enabled" to enabled))
            }

            if ((allowedByAll and GP_GROUP_BAN_ACCESS) == GP_GROUP_BAN_ACCESS ||
                (allowedBySome and GP_GROUP_BAN_ACCESS) == GP_GROUP_BAN_ACCESS) {
                hasGroupBanPower = true
            }
        }

        if (!itemsMatchFilter) {
            ctrl.deleteItem(titleRow)
        }
    }

    private fun hasString(key: String): Boolean = TODO("APR: check string table")
    protected fun findChild(root: Any, name: String): Any? = TODO("GPU: find child in root view")
}

typealias CommitCallback = () -> Unit

class PanelGroupMembersSubTab : PanelGroupSubTab() {

    private var membersList: NameListCtrl? = null
    private var assignedRolesList: ScrollListCtrl? = null
    private var allowedActionsList: ScrollListCtrl? = null
    private var ejectBtn: Button? = null
    private var banBtn: Button? = null
    private var actionDescription: TextEditor? = null

    private var changed: Boolean = false
    private var pendingMemberUpdate: Boolean = false
    private var hasMatch: Boolean = false
    private var numOwnerAdditions: UInt = 0u

    private val memberRoleChangeData: MutableMap<UUID, MutableMap<UUID, RoleMemberChangeType>> = mutableMapOf()
    private val avatarNameCacheConnections: MutableMap<UUID, Any> = mutableMapOf()

    override fun postBuildSubTab(root: Any): Boolean {
        super.postBuildSubTab(root)
        header = findChildInRoot(root, "members_header") as? Panel
        footer = findChildInRoot(root, "members_footer") as? Panel
        membersList = findChildInRoot(root, "member_list") as? NameListCtrl
        assignedRolesList = findChildInRoot(root, "member_assigned_roles") as? ScrollListCtrl
        allowedActionsList = findChildInRoot(root, "member_allowed_actions") as? ScrollListCtrl

        membersList?.let {
            it.setCommitOnSelectionChange(true)
            it.setCommitCallback { handleMemberSelect() }
            it.setDoubleClickCallback { handleMemberDoubleClick() }
            it.setIsFriendCallback { id -> AvatarActions.isFriend(id) }
        }

        findChildInRoot(root, "member_invite")?.let { btn ->
            (btn as? Button)?.let {
                it.setClickedCallback { handleInviteMember() }
                it.setEnabled(Agent.hasPowerInGroup(groupId, GP_MEMBER_INVITE))
            }
        }
        findChildInRoot(root, "export_list")?.let { btn ->
            (btn as? Button)?.let {
                it.setClickedCallback { onExportMembersToXml() }
                it.setEnabled(Agent.hasPowerInGroup(groupId, GP_MEMBER_VISIBLE_IN_DIR))
            }
        }

        ejectBtn = findChildInRoot(root, "member_eject") as? Button
        ejectBtn?.let { it.setClickedCallback { handleEjectMembers() }; it.setEnabled(false) }

        banBtn = findChildInRoot(root, "member_ban") as? Button
        banBtn?.let { it.setClickedCallback { handleBanMember() }; it.setEnabled(false) }

        return true
    }

    override fun setGroupId(id: UUID) {
        membersList?.deleteAllItems()
        assignedRolesList?.deleteAllItems()
        allowedActionsList?.deleteAllItems()
        super.setGroupId(id)
    }

    fun handleMemberSelect() {
        assignedRolesList?.deleteAllItems()
        allowedActionsList?.deleteAllItems()

        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        val selection = membersList?.getAllSelected() ?: return
        if (selection.isEmpty()) return

        var allowedByAll: Long = GP_ALL_POWERS
        var allowedBySome: Long = 0L
        val selectedMembers = mutableListOf<UUID>()

        for (item in selection) {
            val memberId = item.id ?: continue
            selectedMembers.add(memberId)
            val powers = getAgentPowersBasedOnRoleChanges(memberId)
            allowedByAll = allowedByAll and powers
            allowedBySome = allowedBySome or powers
        }

        for ((roleId, role) in gdatap.roles) {
            if (selectedMembers.size == 1) {
                val memberId = selectedMembers.first()
                val memberData = gdatap.members[memberId] ?: continue
                if (memberData.isInRole(roleId)) {
                    assignedRolesList?.addElement(
                        mapOf("id" to roleId.toString(), "name" to role.name, "title" to role.title)
                    )
                }
            }
        }

        buildActionsList(
            ctrl = allowedActionsList ?: return,
            allowedBySome = allowedBySome,
            allowedByAll = allowedByAll,
            commitCallback = null,
            showAll = false,
            filter = false,
            isOwnerRole = false
        )

        val canEject = selectedMembers.isNotEmpty() &&
                Agent.hasPowerInGroup(groupId, GP_MEMBER_EJECT) &&
                selectedMembers.none { it == Agent.id || gdatap.members[it]?.isInRole(gdatap.ownerRole) == true }
        ejectBtn?.setEnabled(canEject)
        banBtn?.setEnabled(
            canEject && Agent.hasPowerInGroup(groupId, GP_GROUP_BAN_ACCESS)
        )
    }

    fun handleMemberDoubleClick() {
        val selected = membersList?.getFirstSelected() ?: return
        selected.id?.let { AvatarActions.showProfile(it) }
    }

    fun handleInviteMember() {
        TODO("APR: open group invite floater for $groupId")
    }

    fun handleEjectMembers() {
        TODO("APR: confirm eject for selected members")
    }

    fun confirmEjectMembers() {
        val selected = membersList?.getAllSelected()?.mapNotNull { it.id } ?: return
        NotificationsUtil.add("EjectGroupMember") { notification, response ->
            handleEjectCallback(notification, response)
        }
    }

    fun handleEjectCallback(notification: Any, response: Any): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option != 0) return false
        val selected = membersList?.getAllSelected()?.mapNotNull { it.id } ?: return false
        sendEjectNotifications(groupId, selected)
        TODO("APR: send eject member message")
    }

    fun sendEjectNotifications(groupId: UUID, selectedMembers: List<UUID>) {
        TODO("APR: send eject IM notifications to ejected members")
    }

    fun handleBanMember() {
        TODO("APR: open bulk ban floater for selected members")
    }

    fun handleBanCallback(notification: Any, response: Any): Boolean {
        TODO("APR: process ban confirmation response")
    }

    fun confirmBanMembers() {
        TODO("APR: show ban confirmation dialog")
    }

    fun handleRoleCheck(roleId: UUID, type: RoleMemberChangeType) {
        val selection = membersList?.getAllSelected()?.mapNotNull { it.id } ?: return
        for (memberId in selection) {
            val roleChanges = memberRoleChangeData.getOrPut(memberId) { mutableMapOf() }
            val existingChange = roleChanges[roleId]
            when {
                existingChange == null -> roleChanges[roleId] = type
                existingChange == RoleMemberChangeType.ADD && type == RoleMemberChangeType.REMOVE ->
                    roleChanges.remove(roleId)
                existingChange == RoleMemberChangeType.REMOVE && type == RoleMemberChangeType.ADD ->
                    roleChanges.remove(roleId)
                else -> roleChanges[roleId] = type
            }
            if (roleId == GroupMgr.getGroupData(groupId)?.ownerRole && type == RoleMemberChangeType.ADD) {
                numOwnerAdditions++
            }
        }
        changed = true
    }

    fun applyMemberChanges() {
        for ((memberId, roleChanges) in memberRoleChangeData) {
            for ((roleId, changeType) in roleChanges) {
                when (changeType) {
                    RoleMemberChangeType.ADD -> TODO("APR: send add member to role message")
                    RoleMemberChangeType.REMOVE -> TODO("APR: send remove member from role message")
                }
            }
        }
        memberRoleChangeData.clear()
        numOwnerAdditions = 0u
        changed = false
    }

    fun addOwnerCb(notification: Any, response: Any): Boolean {
        val option = NotificationsUtil.getSelectedOption(notification, response)
        if (option == 0) applyMemberChanges()
        return false
    }

    override fun activate() {
        super.activate()
        if (!pendingMemberUpdate) updateMembers()
    }

    override fun deactivate() {
        super.deactivate()
        applyMemberChanges()
    }

    override fun cancel() {
        memberRoleChangeData.clear()
        numOwnerAdditions = 0u
        changed = false
    }

    override fun needsApply(mesg: StringBuilder): Boolean = changed

    override fun apply(mesg: StringBuilder): Boolean {
        if (numOwnerAdditions > 0u) {
            NotificationsUtil.add("AddGroupOwner") { notification, response -> addOwnerCb(notification, response) }
            return false
        }
        applyMemberChanges()
        return true
    }

    override fun update(gc: GroupChange) {
        if (gc == GroupChange.GC_MEMBER_DATA || gc == GroupChange.GC_ALL) {
            updateMembers()
        } else if (gc == GroupChange.GC_ROLE_MEMBER_DATA) {
            handleMemberSelect()
        }
    }

    fun updateMembers() {
        pendingMemberUpdate = false
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        if (!gdatap.isMemberDataComplete()) return

        for ((id, conn) in avatarNameCacheConnections) {
            (conn as? AutoCloseable)?.close()
        }
        avatarNameCacheConnections.clear()

        hasMatch = false
        membersList?.deleteAllItems()

        for ((memberId, member) in gdatap.members) {
            member ?: continue
            val avName = AvatarNameCache.get(memberId)
            if (avName != null) {
                addMemberToList(member)
            } else {
                TODO("APR: async fetch avatar name for $memberId, then addMemberToList(member)")
            }
        }

        if (!hasMatch) membersList?.setCommentText("no_results")
        ejectBtn?.setEnabled(false)
        banBtn?.setEnabled(false)
    }

    fun addMemberToList(data: GroupMemberData) {
        if (!matchesSearchFilter(data.id.toString())) return
        hasMatch = true
        membersList?.addNameItemRow(
            id = data.id,
            name = "",
            title = data.getTitle(),
            status = data.getOnlineStatus(),
            bold = data.isOwner()
        )
    }

    fun onNameCache(updateId: UUID, member: GroupMemberData, avName: AvatarName, avId: UUID) {
        avatarNameCacheConnections.remove(avId)
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        if (!gdatap.isMemberDataComplete() || gdatap.getMemberVersion() != updateId) return
        addMemberToList(member)
    }

    override fun draw() {
        if (pendingMemberUpdate) updateMembers()
    }

    private fun matchesSearchFilter(fullname: String): Boolean {
        if (searchFilter.isEmpty()) return true
        return fullname.lowercase().contains(searchFilter)
    }

    private fun getAgentPowersBasedOnRoleChanges(agentId: UUID): Long {
        val gdatap = GroupMgr.getGroupData(groupId) ?: return 0L
        val memberData = gdatap.members[agentId] ?: return 0L
        var powers: Long = 0L
        for ((roleId, role) in gdatap.roles) {
            val inRole = memberData.isInRole(roleId)
            val changes = memberRoleChangeData[agentId]
            val changeType = changes?.get(roleId)
            val effectivelyInRole = when (changeType) {
                RoleMemberChangeType.ADD -> true
                RoleMemberChangeType.REMOVE -> false
                null -> inRole
            }
            if (effectivelyInRole) powers = powers or role.rolePowers
        }
        return powers
    }

    private fun getRoleChangeType(memberId: UUID, roleId: UUID): RoleMemberChangeType? =
        memberRoleChangeData[memberId]?.get(roleId)

    private fun onExportMembersToXml() {
        TODO("APR: open file picker and export member list to XML")
    }

    private fun findChildInRoot(root: Any, name: String): Any? = TODO("GPU: find child in root view")
    private fun ScrollListCtrl.addCheckboxElement(icon: String, label: String, checked: Boolean, tentative: Boolean, enabled: Boolean, callback: CommitCallback) = TODO("GPU: add checkbox row to list")
    private fun NameListCtrl.setCommitOnSelectionChange(v: Boolean) = TODO("GPU: list config")
    private fun NameListCtrl.setIsFriendCallback(fn: (UUID) -> Boolean) = TODO("GPU: list config")
    private fun NameListCtrl.getAllSelected(): List<ScrollListItem> = TODO("GPU: selected items")
    private fun NameListCtrl.setCommentText(text: String) = TODO("GPU: set placeholder")
    private fun Button.setClickedCallback(fn: () -> Unit) = TODO("GPU: button callback")
    private fun ScrollListCtrl.addElement(data: Map<String, Any>): Any = TODO("GPU: add list row")
    private fun ScrollListCtrl.deleteItem(item: Any) = TODO("GPU: remove list row")
    private fun ScrollListCtrl.addCheckboxElement(icon: String, label: String, checked: Boolean, tentative: Boolean, enabled: Boolean, callback: CommitCallback) = TODO("GPU: add checkbox row")
    private fun ScrollListCtrl.getAllSelected(): List<ScrollListItem> = TODO("GPU: selected items")
    private fun ScrollListCtrl.deleteAllItems() = TODO("GPU: clear list")
    private fun ScrollListCtrl.addElement(data: Map<String, String>): Any = TODO("GPU: add row")
    private fun Panel.setAllChildrenEnabled(v: Boolean) = TODO("GPU: enable/disable children")
    private fun FilterEditor.setCommitCallback(fn: (String) -> Unit) = TODO("GPU: filter callback")
    private fun FilterEditor.clear() = TODO("GPU: clear filter")
    private fun FilterEditor.setFocus(v: Boolean) = TODO("GPU: set filter focus")
}

enum class RoleMemberChangeType { ADD, REMOVE }

class PanelGroupRolesSubTab : PanelGroupSubTab() {

    var firstOpen: Boolean = false

    private var rolesList: ScrollListCtrl? = null
    private var assignedMembersList: NameListCtrl? = null
    private var allowedActionsList: ScrollListCtrl? = null
    private var actionDescription: TextEditor? = null
    private var roleName: LineEditor? = null
    private var roleTitle: LineEditor? = null
    private var roleDescription: TextEditor? = null
    private var memberVisibleCheck: CheckBoxCtrl? = null
    private var deleteRoleButton: Button? = null
    private var createRoleButton: Button? = null
    private var copyRoleButton: Button? = null
    private var selectedRole: UUID = UUID(0, 0)
    private var hasRoleChange: Boolean = false
    private var removeEveryoneTxt: String = ""

    override fun postBuildSubTab(root: Any): Boolean {
        super.postBuildSubTab(root)
        header = findChildInRoot(root, "roles_header") as? Panel
        footer = findChildInRoot(root, "roles_footer") as? Panel
        rolesList = findChildInRoot(root, "role_list") as? ScrollListCtrl
        assignedMembersList = findChildInRoot(root, "role_assigned_members") as? NameListCtrl
        allowedActionsList = findChildInRoot(root, "role_allowed_actions") as? ScrollListCtrl
        actionDescription = findChildInRoot(root, "role_action_description") as? TextEditor
        roleName = findChildInRoot(root, "role_name") as? LineEditor
        roleTitle = findChildInRoot(root, "role_title") as? LineEditor
        roleDescription = findChildInRoot(root, "role_description") as? TextEditor
        memberVisibleCheck = findChildInRoot(root, "role_visible_check") as? CheckBoxCtrl
        deleteRoleButton = findChildInRoot(root, "role_delete") as? Button
        createRoleButton = findChildInRoot(root, "role_create") as? Button
        copyRoleButton = findChildInRoot(root, "role_copy") as? Button

        rolesList?.let {
            it.setCommitOnSelectionChange(true)
            it.setCommitCallback { handleRoleSelect() }
        }
        roleName?.setKeystrokeCallback { onPropertiesKey() }
        roleTitle?.setKeystrokeCallback { onPropertiesKey() }
        roleDescription?.setKeystrokeCallback { onDescriptionKeyStroke() }
        memberVisibleCheck?.setCommitCallback { handleMemberVisibilityChange(memberVisibleCheck!!.get()) }
        createRoleButton?.setClickedCallback { handleCreateRole() }
        copyRoleButton?.setClickedCallback { handleCopyRole() }
        deleteRoleButton?.setClickedCallback { handleDeleteRole() }

        removeEveryoneTxt = getString("role_everyone_text")
        return true
    }

    override fun activate() {
        super.activate()
        if (firstOpen) {
            updateRoleList()
            firstOpen = false
        }
    }

    override fun deactivate() {
        super.deactivate()
        saveRoleChanges(true)
    }

    override fun needsApply(mesg: StringBuilder): Boolean = hasRoleChange

    override fun apply(mesg: StringBuilder): Boolean {
        saveRoleChanges(true)
        return true
    }

    override fun cancel() {
        hasRoleChange = false
    }

    fun matchesSearchFilter(roleName: String, roleTitle: String): Boolean {
        if (searchFilter.isEmpty()) return true
        return roleName.lowercase().contains(searchFilter) || roleTitle.lowercase().contains(searchFilter)
    }

    override fun update(gc: GroupChange) {
        if (gc == GroupChange.GC_ROLE_MEMBER_DATA || gc == GroupChange.GC_ALL) {
            updateRoleList()
            handleRoleSelect()
        }
    }

    fun handleRoleSelect() {
        val selected = rolesList?.getFirstSelectedId() ?: return
        selectedRole = selected
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        val role = gdatap.roles[selectedRole] ?: return

        roleName?.setText(role.name)
        roleTitle?.setText(role.title)
        roleDescription?.setText(role.description)
        memberVisibleCheck?.set(role.memberVisible)

        val canEdit = role.roleId != gdatap.ownerRole && Agent.hasPowerInGroup(groupId, GP_ROLE_CHANGE_ACTIONS)
        roleName?.setEnabled(canEdit)
        roleTitle?.setEnabled(canEdit)
        roleDescription?.setEnabled(canEdit)
        memberVisibleCheck?.setEnabled(canEdit)
        deleteRoleButton?.setEnabled(canEdit && selectedRole != gdatap.ownerRole)

        buildMembersList()
        buildActionsList(
            ctrl = allowedActionsList ?: return,
            allowedBySome = role.rolePowers,
            allowedByAll = role.rolePowers,
            commitCallback = if (canEdit) ({ handleActionCheck() }) else null,
            showAll = true,
            filter = true,
            isOwnerRole = selectedRole == gdatap.ownerRole
        )
    }

    fun buildMembersList() {
        assignedMembersList?.deleteAllItems()
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        for ((memberId, member) in gdatap.members) {
            if (member == null) continue
            if (member.isInRole(selectedRole)) {
                assignedMembersList?.addNameItemRow(id = memberId, name = "", title = "", status = "", bold = false)
            }
        }
    }

    private fun handleActionCheck() {
        TODO("APR: update role power bits based on checkbox state")
    }

    fun addActionCb(notification: Any, response: Any, check: Any): Boolean {
        TODO("APR: confirm power change dialog response")
    }

    private fun onPropertiesKey() { hasRoleChange = true }

    fun onDescriptionKeyStroke() { hasRoleChange = true }

    fun handleMemberVisibilityChange(value: Boolean) {
        hasRoleChange = true
    }

    fun handleCreateRole() {
        TODO("APR: create a new group role")
    }

    fun handleCopyRole() {
        TODO("APR: copy the selected role")
    }

    fun handleDeleteRole() {
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        if (selectedRole == gdatap.ownerRole || selectedRole == UUID(0, 0)) return
        TODO("APR: send delete role message")
    }

    fun updateActionDescription() {
        TODO("APR: update action description text from selected action")
    }

    fun saveRoleChanges(selectSavedRole: Boolean) {
        if (!hasRoleChange) return
        TODO("APR: save role name/title/description/powers to GroupMgr")
        hasRoleChange = false
    }

    override fun setGroupId(id: UUID) {
        super.setGroupId(id)
        selectedRole = UUID(0, 0)
        hasRoleChange = false
    }

    override fun draw() {}

    private fun updateRoleList() {
        rolesList?.deleteAllItems()
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        for ((roleId, role) in gdatap.roles) {
            if (!matchesSearchFilter(role.name, role.title)) continue
            rolesList?.addElement(createRoleItem(roleId, role.name, role.title, role.memberCount))
        }
    }

    private fun createRoleItem(roleId: UUID, name: String, title: String, members: Int): Map<String, Any> =
        mapOf("id" to roleId, "name" to name, "title" to title, "members" to members)

    private fun findChildInRoot(root: Any, name: String): Any? = TODO("GPU: find child in root view")
    private fun ScrollListCtrl.setCommitOnSelectionChange(v: Boolean) = TODO("GPU: config")
    private fun ScrollListCtrl.setCommitCallback(fn: () -> Unit) = TODO("GPU: callback")
    private fun ScrollListCtrl.getFirstSelectedId(): UUID? = TODO("GPU: selection query")
    private fun ScrollListCtrl.deleteAllItems() = TODO("GPU: clear items")
    private fun ScrollListCtrl.addElement(data: Map<String, Any>): Any = TODO("GPU: add row")
    private fun LineEditor.setKeystrokeCallback(fn: () -> Unit) = TODO("GPU: key callback")
    private fun TextEditor.setKeystrokeCallback(fn: () -> Unit) = TODO("GPU: key callback")
    private fun Button.setClickedCallback(fn: () -> Unit) = TODO("GPU: button callback")
    private fun NameListCtrl.deleteAllItems() = TODO("GPU: clear items")
}

class PanelGroupActionsSubTab : PanelGroupSubTab() {

    private var actionList: ScrollListCtrl? = null
    private var actionRoles: ScrollListCtrl? = null
    private var actionMembers: NameListCtrl? = null
    private var actionDescription: TextEditor? = null

    override fun postBuildSubTab(root: Any): Boolean {
        super.postBuildSubTab(root)
        header = findChildInRoot(root, "actions_header") as? Panel
        footer = findChildInRoot(root, "actions_footer") as? Panel
        actionList = findChildInRoot(root, "action_list") as? ScrollListCtrl
        actionRoles = findChildInRoot(root, "action_roles") as? ScrollListCtrl
        actionMembers = findChildInRoot(root, "action_members") as? NameListCtrl
        actionDescription = findChildInRoot(root, "action_description") as? TextEditor

        actionList?.let {
            it.setCommitOnSelectionChange(true)
            it.setCommitCallback { handleActionSelect() }
        }
        return true
    }

    override fun activate() {
        super.activate()
        update(GroupChange.GC_ALL)
    }

    override fun deactivate() { super.deactivate() }
    override fun needsApply(mesg: StringBuilder): Boolean = false
    override fun apply(mesg: StringBuilder): Boolean = true

    override fun update(gc: GroupChange) {
        actionList?.deleteAllItems()
        buildActionsList(
            ctrl = actionList ?: return,
            allowedBySome = GP_ALL_POWERS,
            allowedByAll = GP_ALL_POWERS,
            commitCallback = null,
            showAll = true,
            filter = true,
            isOwnerRole = false
        )
    }

    override fun onFilterChanged() { update(GroupChange.GC_ALL) }

    fun handleActionSelect() {
        actionRoles?.deleteAllItems()
        actionMembers?.deleteAllItems()
        val selectedAction = actionList?.getFirstSelectedPowerBit() ?: return
        val gdatap = GroupMgr.getGroupData(groupId) ?: return

        for ((roleId, role) in gdatap.roles) {
            if ((role.rolePowers and selectedAction) != 0L) {
                actionRoles?.addElement(mapOf("id" to roleId.toString(), "name" to role.name))
                for ((memberId, member) in gdatap.members) {
                    if (member?.isInRole(roleId) == true) {
                        actionMembers?.addNameItemRow(id = memberId, name = "", title = "", status = "", bold = false)
                    }
                }
            }
        }
    }

    override fun setGroupId(id: UUID) { super.setGroupId(id) }
    override fun draw() {}

    private fun findChildInRoot(root: Any, name: String): Any? = TODO("GPU: find child in root view")
    private fun ScrollListCtrl.setCommitOnSelectionChange(v: Boolean) = TODO("GPU: config")
    private fun ScrollListCtrl.setCommitCallback(fn: () -> Unit) = TODO("GPU: callback")
    private fun ScrollListCtrl.deleteAllItems() = TODO("GPU: clear")
    private fun ScrollListCtrl.addElement(data: Map<String, Any>): Any = TODO("GPU: add row")
    private fun ScrollListCtrl.getFirstSelectedPowerBit(): Long? = TODO("GPU: selection")
    private fun NameListCtrl.deleteAllItems() = TODO("GPU: clear")
}

class PanelGroupBanListSubTab : PanelGroupSubTab() {

    private var banList: NameListCtrl? = null
    private var createBanButton: Button? = null
    private var deleteBanButton: Button? = null
    private var refreshBanListButton: Button? = null
    private var banCountText: TextBase? = null

    override fun postBuildSubTab(root: Any): Boolean {
        super.postBuildSubTab(root)
        banList = findChildInRoot(root, "ban_list") as? NameListCtrl
        createBanButton = findChildInRoot(root, "ban_create") as? Button
        deleteBanButton = findChildInRoot(root, "ban_delete") as? Button
        refreshBanListButton = findChildInRoot(root, "ban_refresh") as? Button
        banCountText = findChildInRoot(root, "ban_count") as? TextBase

        banList?.let {
            it.setCommitOnSelectionChange(true)
            it.setCommitCallback { handleBanEntrySelect() }
        }
        createBanButton?.setClickedCallback { handleCreateBanEntry() }
        deleteBanButton?.let { it.setClickedCallback { handleDeleteBanEntry() }; it.setEnabled(false) }
        refreshBanListButton?.setClickedCallback { handleRefreshBanList() }
        return true
    }

    override fun activate() {
        super.activate()
        populateBanList()
    }

    override fun update(gc: GroupChange) {
        if (gc == GroupChange.GC_PROPERTIES || gc == GroupChange.GC_ALL) populateBanList()
    }

    override fun onFilterChanged() { populateBanList() }
    override fun draw() {}
    override fun needsApply(mesg: StringBuilder): Boolean = false
    override fun apply(mesg: StringBuilder): Boolean = true
    override fun cancel() {}

    fun handleBanEntrySelect() {
        deleteBanButton?.setEnabled(banList?.getFirstSelected() != null)
    }

    fun handleCreateBanEntry() {
        TODO("APR: open avatar picker to add ban entry")
    }

    fun handleDeleteBanEntry() {
        val selected = banList?.getAllSelected()?.mapNotNull { it.id } ?: return
        TODO("APR: send remove ban entry message for selected UUIDs")
    }

    fun handleRefreshBanList() { populateBanList() }

    override fun setGroupId(id: UUID) {
        super.setGroupId(id)
        banList?.deleteAllItems()
        banCountText?.setText("")
    }

    private fun setBanCount(banCount: UInt) {
        banCountText?.setText("Banned: $banCount")
    }

    private fun populateBanList() {
        banList?.deleteAllItems()
        val gdatap = GroupMgr.getGroupData(groupId) ?: return
        for ((bannedId, banData) in gdatap.banList) {
            if (searchFilter.isNotEmpty()) {
                val name = AvatarNameCache.get(bannedId)?.displayName ?: continue
                if (!name.lowercase().contains(searchFilter)) continue
            }
            banList?.addNameItemRow(id = bannedId, name = "", title = banData.banDate, status = "", bold = false)
        }
        setBanCount(gdatap.banList.size.toUInt())
    }

    private fun findChildInRoot(root: Any, name: String): Any? = TODO("GPU: find child in root view")
    private fun NameListCtrl.setCommitOnSelectionChange(v: Boolean) = TODO("GPU: config")
    private fun NameListCtrl.setCommitCallback(fn: () -> Unit) = TODO("GPU: callback")
    private fun NameListCtrl.deleteAllItems() = TODO("GPU: clear")
    private fun NameListCtrl.getFirstSelected(): ScrollListItem? = TODO("GPU: first selected item")
    private fun NameListCtrl.getAllSelected(): List<ScrollListItem> = TODO("GPU: all selected items")
    private fun Button.setClickedCallback(fn: () -> Unit) = TODO("GPU: button callback")
    private fun TextBase.setText(s: String) = TODO("GPU: set label text")
}

abstract class TextBase : UiCtrl()

class ScrollListCtrl : UiCtrl() {
    fun addElement(data: Map<String, Any>): Any = TODO("GPU: add list row")
    fun deleteAllItems() = TODO("GPU: clear list")
    fun getFirstSelected(): ScrollListItem? = TODO("GPU: first selected item")
    fun getAllSelected(): List<ScrollListItem> = TODO("GPU: all selected items")
}

class TabContainer : UiCtrl() {
    fun getTabCount(): Int = TODO("GPU: tab count")
    fun getPanelByIndex(i: Int): Any? = TODO("GPU: panel by index")
    fun getPanelByName(name: String): Any? = TODO("GPU: panel by name")
    fun getCurrentPanel(): Any? = TODO("GPU: current panel")
    fun selectFirstTab() = TODO("GPU: select first tab")
    fun selectTab(index: Int) = TODO("GPU: select tab by index")
    fun selectTabPanel(panel: PanelGroupTab) = TODO("GPU: select tab panel")
    fun setValidateBeforeCommit(fn: (String) -> Boolean) = TODO("GPU: tab switch validation")
}

class FilterEditor : UiCtrl()

data class RoleData(
    val roleId: UUID,
    val name: String,
    val title: String,
    val description: String,
    val rolePowers: Long,
    val memberVisible: Boolean,
    val memberCount: Int
)

data class BanData(val banDate: String)

data class RoleActionSet(
    val actionSetData: ActionSetData,
    val actions: List<RoleAction>
)

data class ActionSetData(val name: String, val powerBit: Long)
data class RoleAction(val powerBit: Long, val description: String)

val GroupMgrGroupData.roles: Map<UUID, RoleData> get() = TODO("APR: roles map")
val GroupMgrGroupData.banList: Map<UUID, BanData> get() = TODO("APR: ban list map")

fun GroupMgr.getRoleActionSets(): List<RoleActionSet> = TODO("APR: role action sets from GroupMgr")
