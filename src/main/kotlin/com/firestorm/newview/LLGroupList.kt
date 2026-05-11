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
    TODO("APR: use JVM equivalent – gAgent.isInGroup(groupId)")
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
        TODO("APR: use JVM equivalent – inflate UI layout from XML and wire button callbacks")
    }

    fun setValue(value: Map<String, Any>) {
        if (!value.containsKey("selected")) return
        TODO("APR: use JVM equivalent – toggle selected_icon visibility")
    }

    fun onMouseEnter(x: Int, y: Int) {
        TODO("APR: use JVM equivalent – show hovered_icon and action buttons")
    }

    fun onMouseLeave(x: Int, y: Int) {
        TODO("APR: use JVM equivalent – hide hovered_icon and action buttons")
    }

    fun setName(name: String, highlight: String = "") {
        groupName = name
        TODO("APR: use JVM equivalent – highlight matching text in the group name text box")
    }

    fun setGroupID(groupId: UUID) {
        this.groupId = groupId
        // Active or shared group displayed bold
        bold = if (forAgent) {
            TODO("APR: use JVM equivalent – compare to gAgent.getGroupID()")
        } else {
            isAgentInGroup(groupId)
        }
        TODO("APR: use JVM equivalent – register with LLGroupMgr as observer")
    }

    fun setGroupIconID(iconId: UUID) {
        TODO("APR: use JVM equivalent – set icon texture on group icon control")
    }

    fun setGroupIconVisible(visible: Boolean) {
        TODO("APR: use JVM equivalent – show/hide icon and shift name rect accordingly")
    }

    fun setVisibleInProfile(visible: Boolean) {
        // Colour the group name differently when hidden from profile
        val colorKey = if (visible) "GroupVisibleInProfile" else "GroupHiddenInProfile"
        TODO("APR: use JVM equivalent – apply named UI colour to the name text box")
    }

    fun setCustomTextColor(color: LLColor4) {
        customTextColor = color
        TODO("APR: use JVM equivalent – apply color to the name text box")
    }

    override fun changed(gc: LLGroupChange) {
        if (gc == LLGroupChange.GC_ALL || gc == LLGroupChange.GC_PROPERTIES) {
            TODO("APR: use JVM equivalent – update group icon from LLGroupMgr::getGroupData")
        }
    }

    private fun setBold(bold: Boolean) {
        this.bold = bold
        TODO("APR: use JVM equivalent – rebuild text with bold/normal font descriptor")
    }

    private fun onInfoBtnClick() {
        TODO("APR: use JVM equivalent – show inspect_group floater for groupId")
    }

    private fun onProfileBtnClick() {
        LLGroupActions.show(groupId)
    }

    private fun onNoticesBtnClick() {
        LLGroupActions.show(groupId, expandNoticesTab = true)
    }

    private fun onVisibilityBtnClick(newVisibility: Boolean) {
        TODO("APR: use JVM equivalent – call gAgent.setUserGroupFlags and refresh buttons")
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
        TODO("APR: use JVM equivalent – listen to agent group changes, build context menu, wire favorites callback")
    }

    fun draw() {
        if (dirty) refresh()
        TODO("APR: use JVM equivalent – LLFlatListView::draw()")
    }

    fun handleRightMouseDown(x: Int, y: Int): Boolean {
        if (forAgent) {
            TODO("APR: use JVM equivalent – show context menu popup if a non-separator item is selected")
        }
        return false
    }

    fun handleDoubleClick(x: Int, y: Int): Boolean {
        TODO("APR: use JVM equivalent – fire double-click signal for selected item")
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
        TODO("APR: use JVM equivalent – persist to gSavedSettings and update all existing items")
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
        TODO("APR: use JVM equivalent – return name of selected LLGroupListItem or empty string")
    }

    fun getContextMenu(): Any? {
        TODO("APR: use JVM equivalent – return LLToggleableMenu handle")
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
        TODO("APR: use JVM equivalent – onCommit() to notify observers")
    }

    private fun refreshForAgent() {
        items.clear()
        val haveFilter = nameFilter.isNotEmpty()
        var hasFavorites = false
        var hasNonFavorites = false

        TODO("APR: use JVM equivalent – iterate gAgent.mGroups, filter by nameFilter, build LLGroupListItems")
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
        TODO("APR: use JVM equivalent – build LLGroupListItems for mGroups and mSecondaryGroups with separator")
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
            TODO("APR: use JVM equivalent – insert LLGroupListSeparator after the last favorite and re-sort")
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
            "copy_slurl"  -> TODO("APR: use JVM equivalent – copy SLURL for group to clipboard")
            "favorite"    -> TODO("APR: use JVM equivalent – FSFavoriteGroups.addFavorite(selectedGroup)")
            "unfavorite"  -> TODO("APR: use JVM equivalent – FSFavoriteGroups.removeFavorite(selectedGroup)")
        }
        return true
    }

    private fun onContextMenuItemEnable(action: String): Boolean {
        val selectedGroupId = getSelectedUUID()
        val realGroupSelected = selectedGroupId != UUID(0, 0)
        return when (action) {
            "activate"   -> TODO("APR: use JVM equivalent – check current group and RLV canChangeActiveGroup")
            "leave"      -> TODO("APR: use JVM equivalent – check selection and RLV canChangeActiveGroup")
            "call"       -> TODO("APR: use JVM equivalent – check voice enabled and working")
            "favorite", "unfavorite" -> realGroupSelected
            else         -> realGroupSelected
        }
    }

    private fun onContextMenuItemVisible(action: String): Boolean {
        val selectedGroupId = getSelectedUUID()
        return when (action) {
            "favorite"   -> selectedGroupId != UUID(0, 0) &&
                TODO("APR: use JVM equivalent – !FSFavoriteGroups.isFavorite(selectedGroupId)")
            "unfavorite" -> selectedGroupId != UUID(0, 0) &&
                TODO("APR: use JVM equivalent – FSFavoriteGroups.isFavorite(selectedGroupId)")
            else -> true
        }
    }

    private fun onFavoritesChanged() { setDirty() }

    private fun getSelectedUUID(): UUID {
        TODO("APR: use JVM equivalent – return UUID of selected item in flat list view")
    }
}
