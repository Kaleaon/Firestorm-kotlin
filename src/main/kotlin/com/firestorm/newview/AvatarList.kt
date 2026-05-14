package com.firestorm.newview

import com.firestorm.ui.EAddPosition
import com.firestorm.ui.FlatListViewEx
import com.firestorm.ui.LLAvatarName
import com.firestorm.ui.LLAvatarNameCache
import com.firestorm.ui.LLAvatarTracker
import com.firestorm.ui.LLDate
import com.firestorm.ui.LLPanel
import com.firestorm.ui.LLRecentPeople
import com.firestorm.ui.LLTimer
import com.firestorm.ui.LLSD
import com.firestorm.ui.RlvActions
import com.firestorm.ui.gSavedSettings
import com.firestorm.lggcontactsets.LGGContactSets
import java.util.UUID

private const val LIT_UPDATE_PERIOD = 5f          // seconds between last-interaction-time refreshes
private const val ADD_LIMIT = 50                  // max avatars added per frame

typealias AvatarDropCallback = (avatarId: UUID, drop: Boolean) -> Boolean

class AvatarList(
    ignoreOnlineStatus: Boolean = false,
    showLastInteractionTime: Boolean = false,
    showInfoBtn: Boolean = false,
    showProfileBtn: Boolean = true,
    showSpeakingIndicator: Boolean = true,
    showPermissionsGranted: Boolean = false,
    showIcons: Boolean = true,
    showVoiceVolume: Boolean = false,
) : FlatListViewEx() {

    private var ignoreOnlineStatus: Boolean = ignoreOnlineStatus
    private var showLastInteractionTime: Boolean = showLastInteractionTime
    private var dirty: Boolean = true   // force initial update
    private var needUpdateNames: Boolean = false
    private var showIcons: Boolean = showIcons
    private var showInfoBtn: Boolean = showInfoBtn
    private var showProfileBtn: Boolean = showProfileBtn
    private var showVoiceVolume: Boolean = showVoiceVolume
    private var showSpeakingIndicator: Boolean = showSpeakingIndicator
    private var showPermissions: Boolean = showPermissionsGranted
    var showCompleteName: Boolean = false
        private set
    var forceCompleteName: Boolean = false
        private set
    private var rlvCheckShowNames: Boolean = false
    private var showDisplayName: Boolean = gSavedSettings.getBOOL("UseDisplayNames")
    private var showUsername: Boolean = gSavedSettings.getBOOL("NameTagShowUsernames")
    private var useContactSetColors: Boolean = false
    private var useContactSetListStyle: Boolean = false

    private var litUpdateTimer: LLTimer? = null
    private var iconParamName: String = ""
    private var nameFilter: String = ""
    private val ids: MutableList<UUID> = mutableListOf()
    private var sessionId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var contextMenu: Any? = null   // LLListContextMenu equivalent

    private val refreshCompleteCallbacks: MutableList<(Any, LLSD) -> Unit> = mutableListOf()
    private val itemDoubleClickCallbacks: MutableList<(Any, Int, Int, Int) -> Unit> = mutableListOf()
    private val itemClickedCallbacks: MutableList<(Any, Int, Int, Int) -> Unit> = mutableListOf()
    private var avatarDropCallback: AvatarDropCallback? = null
    private var rlvBehaviorCallbackConnection: AutoCloseable? = null

    companion object {
        private val NAME_COMPARATOR = AvatarItemNameComparator()
        private val USERNAME_COMPARATOR = AvatarItemUserNameComparator()
        private val AGENT_ON_TOP_NAME_COMPARATOR = AvatarItemAgentOnTopComparator()

        fun getNameForDisplay(
            avatarId: UUID,
            avName: LLAvatarName,
            showDisplayname: Boolean,
            showUsername: Boolean,
            forceUseCompleteName: Boolean,
            rlvCheckShownames: Boolean,
        ): String {
            val canShowName = !rlvCheckShownames || RlvActions.canShowName(RlvActions.SNC_DEFAULT, avatarId)
            val anon = avName.getAnonymName()
            return when {
                showDisplayname && !showUsername ->
                    if (canShowName) avName.getDisplayName() else anon
                !showDisplayname && showUsername ->
                    if (canShowName) avName.getUserName() else anon
                else ->
                    if (canShowName) avName.getCompleteName(true, forceUseCompleteName) else anon
            }
        }
    }

    init {
        setCommitOnSelectionChange(true)
        setComparator(NAME_COMPARATOR)

        if (showLastInteractionTime) {
            litUpdateTimer = LLTimer().apply {
                setTimerExpirySec(0f)
                start()
            }
        }

        LLAvatarNameCache.getInstance().addUseDisplayNamesCallback { handleDisplayNamesOptionChanged() }
        gSavedSettings.getControl("NameTagShowUsernames")?.getSignal()?.connect { handleDisplayNamesOptionChanged() }
        gSavedSettings.getControl("FSContactSetsColorizeFriends")?.getSignal()?.connect { refreshNames() }
        rlvBehaviorCallbackConnection = com.firestorm.ui.gRlvHandler.setBehaviourCallback { behavior, type ->
            updateRlvRestrictions(behavior, type)
        }
    }

    fun getIDs(): MutableList<UUID> = ids
    fun getIconsVisible(): Boolean = showIcons
    fun getIconParamName(): String = iconParamName
    fun getSessionID(): UUID = sessionId

    fun contains(id: UUID): Boolean = ids.contains(id)

    fun setContextMenu(menu: Any?) { contextMenu = menu }
    fun setSessionID(sessionId: UUID) { this.sessionId = sessionId }
    fun setRlvCheckShowNames(rlvCheckShowNames: Boolean) { this.rlvCheckShowNames = rlvCheckShowNames }
    fun setAvatarDropCallback(cb: AvatarDropCallback) { avatarDropCallback = cb }

    fun setShowCompleteName(show: Boolean, force: Boolean = false) {
        showCompleteName = show
        forceCompleteName = force
    }

    fun setShowIcons(paramName: String) {
        iconParamName = paramName
        showIcons = gSavedSettings.getBOOL(iconParamName)
    }

    fun getAvatarName(avName: LLAvatarName): String =
        if (showCompleteName) avName.getCompleteName(false, forceCompleteName) else avName.getDisplayName()

    fun toggleIcons() {
        showIcons = !showIcons
        gSavedSettings.setBOOL(iconParamName, showIcons)
        forEachItem { it.setAvatarIconVisible(showIcons) }
    }

    fun setSpeakingIndicatorsVisible(visible: Boolean) {
        showSpeakingIndicator = visible
        forEachItem { it.showSpeakingIndicator(showSpeakingIndicator) }
    }

    fun showPermissions(visible: Boolean) {
        showPermissions = visible
        forEachItem { it.setShowPermissions(showPermissions) }
    }

    fun showDisplayName(visible: Boolean) {
        showDisplayName = visible
        forEachItem { it.showDisplayName(visible) }
        needUpdateNames = true
    }

    fun showUsername(visible: Boolean) {
        showUsername = visible
        forEachItem { it.showUsername(visible) }
        needUpdateNames = true
    }

    fun showVoiceVolume(visible: Boolean) {
        showVoiceVolume = visible
    }

    fun setUseContactSetColors(useColors: Boolean) {
        useContactSetColors = useColors
        needUpdateNames = true
    }

    fun setUseContactSetListStyle(useStyle: Boolean) {
        useContactSetListStyle = useStyle
        needUpdateNames = true
    }

    fun sortByName(agentOnTop: Boolean = false) {
        setComparator(if (agentOnTop) AGENT_ON_TOP_NAME_COMPARATOR else NAME_COMPARATOR)
        sort()
    }

    fun sortByUserName() {
        setComparator(USERNAME_COMPARATOR)
        sort()
    }

    fun setNameFilter(filter: String) {
        val upper = filter.uppercase()
        if (nameFilter != upper) {
            nameFilter = upper
            updateNoItemsMessage(filter)
            setDirty()
        }
    }

    fun setDirty(value: Boolean = true, forceRefresh: Boolean = false) {
        dirty = value
        if (dirty && forceRefresh) refresh()
    }

    open fun draw() {
        super.draw()
        if (needUpdateNames) updateAvatarNames()
        if (dirty) refresh()
        val timer = litUpdateTimer
        if (showLastInteractionTime && timer != null && timer.hasExpired()) {
            updateLastInteractionTimes()
            timer.setTimerExpirySec(LIT_UPDATE_PERIOD)
        }
    }

    open fun clear() {
        ids.clear()
        updateNoItemsMessage(nameFilter)
        setDirty(true)
        super.clear()
    }

    open fun setVisible(visible: Boolean) {
        if (!visible) (contextMenu as? com.firestorm.ui.LLListContextMenu)?.hide()
        super.setVisible(visible)
    }

    fun filterHasMatches(): Boolean {
        for (id in ids) {
            val avName = LLAvatarName()
            val haveName = LLAvatarNameCache.get(id, avName)
            if (haveName && !findInsensitive(
                    getNameForDisplay(id, avName, showDisplayName, showUsername, forceCompleteName, rlvCheckShowNames),
                    nameFilter
                )
            ) continue
            return true
        }
        return false
    }

    fun setRefreshCompleteCallback(cb: (Any, LLSD) -> Unit) {
        refreshCompleteCallbacks.add(cb)
    }

    fun setItemDoubleClickCallback(cb: (Any, Int, Int, Int) -> Unit) {
        itemDoubleClickCallbacks.add(cb)
    }

    fun setItemClickedCallback(cb: (Any, Int, Int, Int) -> Unit) {
        itemClickedCallbacks.add(cb)
    }

    open fun notifyParent(info: LLSD): Int {
        val currentComparator = getComparator()
        if (info.has("sort") && (
                    currentComparator === NAME_COMPARATOR ||
                    currentComparator === USERNAME_COMPARATOR ||
                    currentComparator === AGENT_ON_TOP_NAME_COMPARATOR
                    )
        ) {
            sort()
            return 1
        }
        if (info.has("select") && info["select"].isUUID()) {
            val selected = getSelectedValue()
            val itemId = info["select"].asUUID()
            if (!selected.isDefined() || (selected.isUUID() && selected.asUUID() != itemId)) {
                resetSelection()
                selectItemByUUID(itemId)
            }
        }
        return super.notifyParent(info)
    }

    fun handleDisplayNamesOptionChanged() {
        showUsername = gSavedSettings.getBOOL("NameTagShowUsernames")
        showDisplayName = gSavedSettings.getBOOL("UseDisplayNames")
        forEachItem { item ->
            item.showUsername(showUsername, false)
            item.showDisplayName(showDisplayName, false)
        }
        needUpdateNames = true
    }

    fun refreshNames() {
        needUpdateNames = true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val handled = super.handleRightMouseDown(x, y, mask)
        val menu = contextMenu as? com.firestorm.ui.LLListContextMenu
        if (menu != null && (!rlvCheckShowNames || !RlvActions.hasBehaviour(com.firestorm.ui.RLV_BHVR_SHOWNAMES))) {
            val selected = mutableListOf<UUID>()
            getSelectedUUIDs(selected)
            menu.show(this, selected, x, y)
        }
        return handled
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("AvatarList: handleMouseDown not yet implemented")
        return false
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("AvatarList: handleMouseUp not yet implemented")
        return false
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("AvatarList: handleHover not yet implemented")
        return false
    }

    fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: Int, cargoData: Any?,
        accept: IntArray, tooltipMsg: StringBuilder
    ): Boolean {
        val cb = avatarDropCallback ?: return super.handleDragAndDrop(x, y, mask, drop, cargoType, cargoData, accept, tooltipMsg)
        val avatarId: UUID = when (cargoType) {
            DAD_PERSON -> cargoData as? UUID ?: NULL_UUID
            DAD_CALLINGCARD -> (cargoData as? com.firestorm.ui.LLInventoryItem)?.getCreatorUUID() ?: NULL_UUID
            else -> { accept[0] = ACCEPT_NO; return true }
        }
        if (avatarId == NULL_UUID) { accept[0] = ACCEPT_NO; return true }
        accept[0] = if (cb(avatarId, drop)) ACCEPT_YES_MULTI else ACCEPT_NO
        return true
    }

    // -----------------------------------------------------------------------
    // Protected helpers
    // -----------------------------------------------------------------------

    protected fun refresh() {
        var haveNames = true
        var addLimitExceeded = false
        var modified = false
        val haveFilter = nameFilter.isNotEmpty()

        val selectedIds = mutableListOf<UUID>()
        getSelectedUUIDs(selectedIds)
        val currentId = getSelectedUUID()

        val added = mutableListOf<UUID>()
        val removed = mutableListOf<UUID>()
        computeDifference(ids, added, removed)

        var nAdded = 0
        for (buddyId in added) {
            val avName = LLAvatarName()
            haveNames = haveNames and LLAvatarNameCache.get(buddyId, avName)
            val displayName = getNameForDisplay(buddyId, avName, showDisplayName, showUsername, forceCompleteName, rlvCheckShowNames)
            if (!haveFilter || findInsensitive(displayName, nameFilter)) {
                if (nAdded >= ADD_LIMIT) {
                    addLimitExceeded = true
                    break
                }
                addNewItem(buddyId, avName.getCompleteName(), LLAvatarTracker.instance().isBuddyOnline(buddyId))
                modified = true
                nAdded++
            }
        }

        for (id in removed) {
            removeItemByUUID(id)
            modified = true
        }

        if (haveFilter) {
            val curValues = mutableListOf<LLSD>()
            getValues(curValues)
            for (entry in curValues) {
                val buddyId = entry.asUUID()
                val avName = LLAvatarName()
                haveNames = haveNames and LLAvatarNameCache.get(buddyId, avName)
                val displayName = getNameForDisplay(buddyId, avName, showDisplayName, showUsername, forceCompleteName, rlvCheckShowNames)
                if (!findInsensitive(displayName, nameFilter)) {
                    removeItemByUUID(buddyId)
                    modified = true
                }
            }
        }

        sort()
        selectItemByUUID(currentId)

        val dirty = addLimitExceeded || (haveFilter && !haveNames)
        setDirty(dirty)

        if (!dirty) {
            forEachItem { it.setHighlight(nameFilter) }
            val count = LLSD.fromInt(size(false))
            refreshCompleteCallbacks.forEach { it(this, count) }
        }

        if (modified) onCommit()
    }

    protected fun updateAvatarNames() {
        forEachItem { item ->
            item.setUseContactSetColors(useContactSetColors)
            item.setUseContactSetListStyle(useContactSetListStyle)
            item.setShowCompleteName(showCompleteName, forceCompleteName)
            item.updateAvatarName()
        }
        needUpdateNames = false
    }

    protected fun addNewItem(id: UUID, name: String, isOnline: Boolean, pos: EAddPosition = EAddPosition.ADD_BOTTOM) {
        val item = AvatarListItem()
        item.setShowCompleteName(showCompleteName, forceCompleteName)
        item.setRlvCheckShowNames(rlvCheckShowNames)
        item.setAvatarId(id, sessionId, ignoreOnlineStatus)
        item.setOnline(if (ignoreOnlineStatus) true else isOnline)
        item.showLastInteractionTime(showLastInteractionTime)
        item.setAvatarIconVisible(showIcons)
        item.setShowInfoBtn(showInfoBtn)
        item.setShowVoiceVolume(showVoiceVolume)
        item.setShowProfileBtn(showProfileBtn)
        item.showSpeakingIndicator(showSpeakingIndicator)
        item.setShowPermissions(showPermissions)
        item.showUsername(showUsername)
        item.showDisplayName(showDisplayName)
        item.setUseContactSetColors(useContactSetColors)
        item.setUseContactSetListStyle(useContactSetListStyle)
        item.setDoubleClickCallback { ctrl, x, y, mask -> onItemDoubleClicked(ctrl, x, y, mask) }
        item.setMouseDownCallback { ctrl, x, y, mask -> onItemClicked(ctrl, x, y, mask) }
        addItem(item, id, pos)
    }

    protected fun computeDifference(vnewUnsorted: List<UUID>, vadded: MutableList<UUID>, vremoved: MutableList<UUID>) {
        val curValues = mutableListOf<LLSD>()
        getValues(curValues)
        val vcur = curValues.map { it.asUUID() }
        computeListDifference(vnewUnsorted, vcur, vadded, vremoved)
    }

    protected fun updateLastInteractionTimes() {
        val now = (LLDate.now().secondsSinceEpoch()).toLong()
        forEachItem { item ->
            val secsSince = (now - LLRecentPeople.instance().getDate(item.getAvatarId()).secondsSinceEpoch().toLong()).toInt()
            if (secsSince >= 0) item.setLastInteractionTime(secsSince.toUInt())
        }
    }

    private fun onItemDoubleClicked(ctrl: Any, x: Int, y: Int, mask: Int) {
        if (!rlvCheckShowNames || !RlvActions.hasBehaviour(com.firestorm.ui.RLV_BHVR_SHOWNAMES)) {
            itemDoubleClickCallbacks.forEach { it(ctrl, x, y, mask) }
        }
    }

    private fun onItemClicked(ctrl: Any, x: Int, y: Int, mask: Int) {
        itemClickedCallbacks.forEach { it(ctrl, x, y, mask) }
    }

    private fun updateRlvRestrictions(behavior: com.firestorm.ui.ERlvBehaviour, type: com.firestorm.ui.ERlvParamType) {
        if (behavior == com.firestorm.ui.RLV_BHVR_SHOWNAMES) {
            forEachItem { it.updateRlvRestrictions() }
        }
    }

    private fun forEachItem(action: (AvatarListItem) -> Unit) {
        val panels = mutableListOf<LLPanel>()
        getItems(panels)
        panels.filterIsInstance<AvatarListItem>().forEach(action)
    }

    fun dispose() {
        litUpdateTimer?.stop()
        rlvBehaviorCallbackConnection?.close()
        rlvBehaviorCallbackConnection = null
    }

    // -----------------------------------------------------------------------
    // Platform stubs
    // -----------------------------------------------------------------------

    private companion object {
        val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        const val DAD_PERSON = 1
        const val DAD_CALLINGCARD = 2
        const val ACCEPT_NO = 0
        const val ACCEPT_YES_MULTI = 3

        fun findInsensitive(haystack: String, needleUpper: String): Boolean =
            haystack.uppercase().contains(needleUpper)

        fun computeListDifference(
            vnew: List<UUID>, vcur: List<UUID>,
            vadded: MutableList<UUID>, vremoved: MutableList<UUID>
        ) {
            val newSet = vnew.toHashSet()
            val curSet = vcur.toHashSet()
            vadded.addAll(vnew.filter { it !in curSet })
            vremoved.addAll(vcur.filter { it !in newSet })
        }
    }
}

// ---------------------------------------------------------------------------
// Comparators
// ---------------------------------------------------------------------------

abstract class AvatarItemComparator : FlatListViewEx.ItemComparator {
    override fun compare(item1: LLPanel, item2: LLPanel): Boolean {
        val av1 = item1 as? AvatarListItem ?: error("item1 must be AvatarListItem")
        val av2 = item2 as? AvatarListItem ?: error("item2 must be AvatarListItem")
        return doCompare(av1, av2)
    }

    protected fun getComparableName(avatarItem: AvatarListItem): String {
        var name = avatarItem.getAvatarName()
        if (LGGContactSets.getInstance().hasPseudonym(avatarItem.getAvatarId()) &&
            name.isNotEmpty() && (name.first() == '\'' || name.first() == '"')
        ) {
            name = name.substring(1)
        }
        return name.uppercase()
    }

    protected abstract fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean
}

open class AvatarItemNameComparator : AvatarItemComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean =
        getComparableName(item1) < getComparableName(item2)
}

class AvatarItemAgentOnTopComparator : AvatarItemNameComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean {
        val agentId = com.firestorm.newview.agentID
        if (item1.getAvatarId() == agentId) return true
        if (item2.getAvatarId() == agentId) return false
        return super.doCompare(item1, item2)
    }
}

class AvatarItemUserNameComparator : AvatarItemComparator() {
    override fun doCompare(item1: AvatarListItem, item2: AvatarListItem): Boolean =
        item1.userName.uppercase() < item2.userName.uppercase()
}
