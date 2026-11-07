package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Colour stub (mirrors LLColor4 used by Contact Sets feature)
// ---------------------------------------------------------------------------

data class LLColor4(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {
    companion object {
        val red = LLColor4(1f, 0f, 0f)
    }
}

// ---------------------------------------------------------------------------
// Group-change notification enum (shared with LLGroupActions)
// (Redeclared here as a convenience; in the real build both files
//  share the same declaration from LLGroupActions.kt.)
// ---------------------------------------------------------------------------
// (LLGroupChange is declared in LLGroupActions.kt – reuse it here)

// ---------------------------------------------------------------------------
// LLGroupComparator – sorts list items: favorites first, then alphabetical
// ---------------------------------------------------------------------------

class LLGroupComparator : Comparator<LLGroupListItem> {
    override fun compare(a: LLGroupListItem, b: LLGroupListItem): Int {
        val fav1 = a.isFavorite
        val fav2 = b.isFavorite
        if (fav1 != fav2) return if (fav1) -1 else 1
        val name1 = a.groupName.toUpperCase()
        val name2 = b.groupName.toUpperCase()
        return name1.compareTo(name2)
    }
}

// ---------------------------------------------------------------------------
// LLSharedGroupComparator – shared groups first, then alphabetical
// ---------------------------------------------------------------------------

class LLSharedGroupComparator : Comparator<LLGroupListItem> {
    override fun compare(a: LLGroupListItem, b: LLGroupListItem): Int {
        val shared1 = isAgentInGroup(a.groupId)
        val shared2 = isAgentInGroup(b.groupId)
        if (shared1 != shared2) return if (shared1) -1 else 1
        val name1 = a.groupName.toUpperCase()
        val name2 = b.groupName.toUpperCase()
        return name1.compareTo(name2)
    }
}

// Stub: check whether the local agent belongs to a group
private fun isAgentInGroup(groupId: UUID): Boolean {
    System.err.println("LLGroupList: isAgentInGroup not yet implemented")
    return false
}

// ---------------------------------------------------------------------------
// LLGroupListSeparator – visual divider between favorites and other groups
// ---------------------------------------------------------------------------

class LLGroupListSeparator {
    fun postBuild(): Boolean = true

    // Absorb all mouse input so the separator is not interactive
    fun handleMouseDown(x: Int, y: Int): Boolean = true
    fun handleMouseUp(x: Int, y: Int): Boolean = true
    fun handleRightMouseDown(x: Int, y: Int): Boolean = true
    fun handleRightMouseUp(x: Int, y: Int): Boolean = true
    fun handleDoubleClick(x: Int, y: Int): Boolean = true
}

// ---------------------------------------------------------------------------
// LLGroupListItem – single row in the group list
// ---------------------------------------------------------------------------

class LLGroupListItem(
    private val forAgent: Boolean,
    private val showIcons: Boolean
) : LLGroupMgrObserver {

    var groupId: UUID = UUID(0, 0)
        private set
    var groupName: String = ""
        private set
    var isFavorite: Boolean = false

    private var customTextColor: LLColor4? = null
    private var bold: Boolean = false
    private var iconWidth: Int = 0

    fun postBuild(): Boolean {
        System.err.println("LLGroupListItem: postBuild not yet implemented")
        return false
    }

    fun setValue(value: Map<String, Any>) {
        if (!value.containsKey("selected")) return
        System.err.println("LLGroupListItem: setValue not yet implemented")
    }

    fun onMouseEnter(x: Int, y: Int) {
        System.err.println("LLGroupListItem: onMouseEnter not yet implemented")
    }

    fun onMouseLeave(x: Int, y: Int) {
        System.err.println("LLGroupListItem: onMouseLeave not yet implemented")
    }

    fun setName(name: String, highlight: String = "") {
        groupName = name
        System.err.println("LLGroupListItem: setName not yet implemented")
    }

    fun setGroupID(groupId: UUID) {
        this.groupId = groupId
        // Active or shared group displayed bold
        bold = if (forAgent) {
            System.err.println("LLGroupListItem: setGroupID (compare to gAgent.getGroupID) not yet implemented")
            false
        } else {
            isAgentInGroup(groupId)
        }
        System.err.println("LLGroupListItem: setGroupID (register with LLGroupMgr) not yet implemented")
    }

    fun setGroupIconID(iconId: UUID) {
        System.err.println("LLGroupListItem: setGroupIconID not yet implemented")
    }

    fun setGroupIconVisible(visible: Boolean) {
        System.err.println("LLGroupListItem: setGroupIconVisible not yet implemented")
    }

    fun setVisibleInProfile(visible: Boolean) {
        // Colour the group name differently when hidden from profile
        val colorKey = if (visible) "GroupVisibleInProfile" else "GroupHiddenInProfile"
        System.err.println("LLGroupListItem: setVisibleInProfile not yet implemented")
    }

    fun setCustomTextColor(color: LLColor4) {
        customTextColor = color
        System.err.println("LLGroupListItem: setCustomTextColor not yet implemented")
    }

    override fun changed(gc: LLGroupChange) {
        if (gc == LLGroupChange.GC_ALL || gc == LLGroupChange.GC_PROPERTIES) {
            System.err.println("LLGroupListItem: changed not yet implemented")
        }
    }

    private fun setBold(bold: Boolean) {
        this.bold = bold
        System.err.println("LLGroupListItem: setBold not yet implemented")
    }

    private fun onInfoBtnClick() {
        System.err.println("LLGroupListItem: onInfoBtnClick not yet implemented")
    }

    private fun onProfileBtnClick() {
        LLGroupActions.show(groupId)
    }

    private fun onNoticesBtnClick() {
        LLGroupActions.show(groupId, expandNoticesTab = true)
    }

    private fun onVisibilityBtnClick(newVisibility: Boolean) {
        System.err.println("LLGroupListItem: onVisibilityBtnClick not yet implemented")
    }
}

// ---------------------------------------------------------------------------
// LLGroupList – auto-updating flat list of agent groups
// ---------------------------------------------------------------------------

class LLGroupList(forAgent: Boolean = true) {

    private var forAgent: Boolean = forAgent
    private var dirty: Boolean = true
    private var showIcons: Boolean = false
    private var showNone: Boolean = true
    private var showFavoritesSeparator: Boolean = true
    private var nameFilter: String = ""

    private val items: MutableList<LLGroupListItem> = mutableListOf()
    private val groups: MutableMap<String, UUID> = mutableMapOf()

    private val secondaryGroups: MutableList<String> = mutableListOf()
    private val secondaryGroupColors: MutableMap<String, LLColor4> = mutableMapOf()

    private var favoritesChangedConnection: (() -> Unit)? = null

    private val comparator: Comparator<LLGroupListItem> =
        if (forAgent) LLGroupComparator() else LLSharedGroupComparator()

    init {
        if (forAgent) enableForAgent(showIcons = true)
    }

    fun enableForAgent(showIcons: Boolean) {
        this.forAgent = true
        this.showIcons = showIcons
        System.err.println("LLGroupList: enableForAgent not yet implemented")
    }

    fun draw() {
        if (dirty) refresh()
        System.err.println("LLGroupList: draw not yet implemented")
    }

    fun handleRightMouseDown(x: Int, y: Int): Boolean {
        if (forAgent) {
            System.err.println("LLGroupList: handleRightMouseDown not yet implemented")
        }
        return false
    }

    fun handleDoubleClick(x: Int, y: Int): Boolean {
        System.err.println("LLGroupList: handleDoubleClick not yet implemented")
        return false
    }

    fun setNameFilter(filter: String) {
        val upper = filter.toUpperCase()
        if (nameFilter != upper) {
            nameFilter = upper
            dirty = true
        }
    }

    fun toggleIcons() {
        showIcons = !showIcons
        System.err.println("LLGroupList: toggleIcons not yet implemented")
    }

    fun getIconsVisible(): Boolean = showIcons
    fun setIconsVisible(showIcons: Boolean) { this.showIcons = showIcons }
    fun setShowNone(showNone: Boolean) { this.showNone = showNone }
    fun setShowFavoritesSeparator(show: Boolean) { showFavoritesSeparator = show; dirty = true }

    fun setGroups(groupList: Map<String, UUID>) {
        groups.clear()
        groups.putAll(groupList)
        dirty = true
    }

    fun setSecondaryGroups(groupNames: List<String>, groupColors: Map<String, LLColor4> = emptyMap()) {
        secondaryGroups.clear()
        secondaryGroups.addAll(groupNames)
        secondaryGroupColors.clear()
        secondaryGroupColors.putAll(groupColors)
        dirty = true
    }

    fun getSelectedGroupName(): String {
        System.err.println("LLGroupList: getSelectedGroupName not yet implemented")
        return ""
    }

    fun getContextMenu(): Any? {
        System.err.println("LLGroupList: getContextMenu not yet implemented")
        return null
    }

    fun refreshFavorites() { dirty = true }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun setDirty(value: Boolean = true) { dirty = value }

    private fun refresh() {
        if (forAgent) {
            refreshForAgent()
        } else {
            refreshSharedGroups()
        }
        setDirty(false)
        System.err.println("LLGroupList: refresh (onCommit) not yet implemented")
    }

    private fun refreshForAgent() {
        items.clear()
        val haveFilter = nameFilter.isNotEmpty()
        var hasFavorites = false
        var hasNonFavorites = false

        System.err.println("LLGroupList: refreshForAgent not yet implemented")
        // Pseudocode outline preserved as comments so the sort / separator logic is clear:
        //   for each groupData in gAgent.mGroups:
        //     if haveFilter && name doesn't contain nameFilter: continue
        //     isFavorite = FSFavoriteGroups.isFavorite(id)
        //     hasFavorites ||= isFavorite; hasNonFavorites ||= !isFavorite
        //     addNewItem(id, name, iconId, isFavorite)
        //   sort(comparator)
        //   if showFavoritesSeparator && hasFavorites && hasNonFavorites && !haveFilter:
        //     addFavoritesSeparator()
        //   if !haveFilter && groupCount > 0 && showNone:
        //     addNewItem(UUID_NULL, locNone, UUID_NULL, ADD_TOP, visible=true, favorite=false)
        //   selectItemByUUID(gAgent.getGroupID())
    }

    private fun refreshSharedGroups() {
        items.clear()
        System.err.println("LLGroupList: refreshSharedGroups not yet implemented")
    }

    private fun addNewItem(
        id: UUID,
        name: String,
        iconId: UUID,
        addToTop: Boolean = false,
        visibleInProfile: Boolean = true,
        isFavorite: Boolean = false
    ): LLGroupListItem {
        val item = LLGroupListItem(forAgent, showIcons)
        item.setGroupID(id)
        item.setName(name, nameFilter)
        item.setGroupIconID(iconId)
        item.isFavorite = isFavorite
        item.setGroupIconVisible(showIcons)
        item.setVisibleInProfile(visibleInProfile)
        if (addToTop) items.add(0, item) else items.add(item)
        return item
    }

    private fun addFavoritesSeparator() {
        val favoriteCount = items.count { it.isFavorite }
        if (favoriteCount > 0) {
            System.err.println("LLGroupList: addFavoritesSeparator not yet implemented")
        }
    }

    private fun handleEvent(eventDesc: String, value: Map<String, Any>?): Boolean {
        return when (eventDesc) {
            "new group" -> {
                setDirty()
                true
            }
            "value_changed" -> {
                val groupId = value?.get("group_id") as? UUID ?: return true
                val visible = value["visible"] as? Boolean ?: return true
                items.find { it.groupId == groupId }?.setVisibleInProfile(visible)
                true
            }
            else -> false
        }
    }

    private fun onContextMenuItemClick(action: String): Boolean {
        val selectedGroup = getSelectedUUID()
        when (action) {
            "view_info"   -> LLGroupActions.show(selectedGroup)
            "chat"        -> LLGroupActions.startIM(selectedGroup)
            "call"        -> LLGroupActions.startCall(selectedGroup)
            "activate"    -> LLGroupActions.activate(selectedGroup)
            "leave"       -> LLGroupActions.leave(selectedGroup)
            "copy_slurl"  -> System.err.println("LLGroupList: onContextMenuItemClick copy_slurl not yet implemented")
            "favorite"    -> System.err.println("LLGroupList: onContextMenuItemClick favorite not yet implemented")
            "unfavorite"  -> System.err.println("LLGroupList: onContextMenuItemClick unfavorite not yet implemented")
        }
        return true
    }

    private fun onContextMenuItemEnable(action: String): Boolean {
        val selectedGroupId = getSelectedUUID()
        val realGroupSelected = selectedGroupId != UUID(0, 0)
        return when (action) {
            "activate"   -> { System.err.println("LLGroupList: onContextMenuItemEnable activate not yet implemented"); false }
            "leave"      -> { System.err.println("LLGroupList: onContextMenuItemEnable leave not yet implemented"); false }
            "call"       -> { System.err.println("LLGroupList: onContextMenuItemEnable call not yet implemented"); false }
            "favorite", "unfavorite" -> realGroupSelected
            else         -> realGroupSelected
        }
    }

    private fun onContextMenuItemVisible(action: String): Boolean {
        val selectedGroupId = getSelectedUUID()
        return when (action) {
            "favorite"   -> { System.err.println("LLGroupList: onContextMenuItemVisible favorite not yet implemented"); false }
            "unfavorite" -> { System.err.println("LLGroupList: onContextMenuItemVisible unfavorite not yet implemented"); false }
            else -> true
        }
    }

    private fun onFavoritesChanged() { setDirty() }

    private fun getSelectedUUID(): UUID {
        System.err.println("LLGroupList: getSelectedUUID not yet implemented")
        return UUID(0, 0)
    }
}
