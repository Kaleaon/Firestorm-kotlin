package com.firestorm.newview

import java.util.UUID

typealias SelectCallback = (avatarIds: List<UUID>, avatarNames: List<AvatarName>) -> Unit
typealias ValidateCallback = (avatarIds: List<UUID>) -> Boolean

data class AvatarName(
    val displayName: String,
    val userName: String,
    val accountName: String = ""
)

class FloaterAvatarPicker(val key: Any) {

    var selectionCallback: SelectCallback? = null
    var allowMultipleSelection: Boolean = false
    var nearMeListComplete: Boolean = false
    var closeOnSelect: Boolean = false
    var excludeAgentFromSearchResults: Boolean = false

    private var queryId: UUID = UUID.randomUUID()
    private var numResultsReturned: Int = 0
    private var contextConeOpacity: Float = 0f
    private var contextConeInAlpha: Float = CONTEXT_CONE_IN_ALPHA
    private var contextConeOutAlpha: Float = CONTEXT_CONE_OUT_ALPHA
    private var contextConeFadeTime: Float = CONTEXT_CONE_FADE_TIME

    private val validateSignal: MutableList<ValidateCallback> = mutableListOf()

    private val searchResults: MutableList<ScrollListItem> = mutableListOf()
    private val nearMeList: MutableList<ScrollListItem> = mutableListOf()
    private val friendsList: MutableList<ScrollListItem> = mutableListOf()
    private val searchResultsUuid: MutableList<ScrollListItem> = mutableListOf()

    private var activeTab: String = "SearchPanel"

    companion object {
        const val AVATAR_PICKER_SEARCH_TIMEOUT: UInt = 180U
        const val CONTEXT_CONE_IN_ALPHA: Float = 0.0f
        const val CONTEXT_CONE_OUT_ALPHA: Float = 0.0f
        const val CONTEXT_CONE_FADE_TIME: Float = 0.0f

        private val avatarNameMap: MutableMap<UUID, AvatarName> = mutableMapOf()

        val queryNameMap: MutableMap<UUID, String> = mutableMapOf()

        fun show(
            callback: SelectCallback,
            allowMultiple: Boolean = false,
            closeOnSelect: Boolean = false,
            skipAgent: Boolean = false,
            name: String = "",
            frustumOrigin: Any? = null
        ): FloaterAvatarPicker {
            val floater = FloaterAvatarPicker(name)
            floater.selectionCallback = callback
            floater.setAllowMultiple(allowMultiple)
            floater.nearMeListComplete = false
            floater.closeOnSelect = closeOnSelect
            floater.excludeAgentFromSearchResults = skipAgent
            return floater
        }

        fun processAvatarPickerReply(msg: MessageSystem) {
            val agentId = msg.getUUID("AgentData", "AgentID")
            val queryId = msg.getUUID("AgentData", "QueryID")

            if (agentId != Agent.id) return

            val found = queryNameMap[queryId] ?: return
            queryNameMap.remove(queryId)

            val floater = FloaterReg.findInstance<FloaterAvatarPicker>("avatar_picker", found) ?: return
            if (queryId != floater.queryId) return

            val numNewRows = msg.getNumberOfBlocks("Data")
            if (floater.numResultsReturned++ == 0) {
                floater.searchResults.clear()
            }

            var foundOne = false
            for (i in 0 until numNewRows) {
                val avatarId = msg.getUUID("Data", "AvatarID", i)
                val firstName = msg.getString("Data", "FirstName", i)
                val lastName = msg.getString("Data", "LastName", i)

                if (avatarId != agentId || !floater.excludeAgentFromSearchResults) {
                    val avatarName: String
                    if (avatarId == UUID(0, 0)) {
                        avatarName = floater.getString("not_found")
                        floater.setSearchResultsEnabled(false)
                        floater.setOkBtnEnabled(false)
                    } else {
                        avatarName = "$firstName $lastName".trim()
                        floater.setSearchResultsEnabled(true)
                        foundOne = true
                        avatarNameMap[avatarId] = AvatarName(displayName = avatarName, userName = avatarName)
                    }
                    floater.searchResults.add(ScrollListItem(id = avatarId, label = avatarName))
                }
            }

            if (foundOne) {
                floater.setOkBtnEnabled(true)
                floater.onList()
            }
        }

        private fun getSelectedAvatarData(
            list: List<ScrollListItem>
        ): Pair<MutableList<UUID>, MutableList<AvatarName>> {
            val ids = mutableListOf<UUID>()
            val names = mutableListOf<AvatarName>()
            for (item in list) {
                if (item.id != null && item.id != UUID(0, 0)) {
                    ids.add(item.id)
                    val cached = avatarNameMap[item.id]
                    if (cached != null) {
                        names.add(cached)
                    } else {
                        names.add(AvatarNameCache.get(item.id) ?: AvatarName("", ""))
                    }
                }
            }
            return Pair(ids, names)
        }

        suspend fun findByIdCoro(url: String, queryId: UUID, agentId: UUID, floaterKey: String) {
            TODO("APR: use JVM equivalent")
        }

        suspend fun findByNameCoro(url: String, queryId: UUID, name: String) {
            TODO("APR: use JVM equivalent")
        }
    }

    fun postBuild(): Boolean {
        setAllowMultiple(false)
        populateFriend()
        return true
    }

    fun setOkBtnEnableCb(cb: ValidateCallback) {
        validateSignal.add(cb)
    }

    fun onTabChanged() {
        setOkBtnEnabled(isSelectBtnEnabled())
    }

    fun onBtnFind() {
        find()
    }

    fun onBtnFindUuid() {
        searchResultsUuid.clear()
        TODO("APR: use JVM equivalent for UUID avatar name cache lookup")
    }

    fun onFindUuidAvatarNameCache(avId: UUID, avName: AvatarName) {
        searchResultsUuid.clear()
        if (avName.accountName != "(??).(??)" && avName.accountName.isNotBlank()) {
            searchResultsUuid.add(ScrollListItem(id = avId, label = avName.displayName))
            setOkBtnEnabled(true)
        } else {
            setOkBtnEnabled(false)
        }
    }

    fun onBtnSelect() {
        if (!isSelectBtnEnabled()) return
        val callback = selectionCallback ?: return

        val (avatarIds, avatarNames) = when (activeTab) {
            "ContactSetsPanel" -> {
                if (!allowMultipleSelection) return
                collectContactSetMembers()
            }
            else -> {
                val list = getActiveListForTab(activeTab)
                if (list != null) getSelectedAvatarData(list) else return
            }
        }

        if (avatarIds.isNotEmpty()) {
            callback(avatarIds, avatarNames)
        }

        searchResults.forEach { it.selected = false }
        nearMeList.forEach { it.selected = false }
        friendsList.forEach { it.selected = false }
        searchResultsUuid.forEach { it.selected = false }

        if (closeOnSelect) {
            closeOnSelect = false
            closeFloater()
        }
    }

    fun onBtnRefresh() {
        nearMeList.clear()
        nearMeListComplete = false
    }

    fun onBtnClose() {
        closeFloater()
    }

    fun onRangeAdjust() {
        onBtnRefresh()
    }

    fun onList() {
        setOkBtnEnabled(isSelectBtnEnabled())
    }

    fun populateNearMe() {
        nearMeList.clear()
        var allLoaded = true
        var empty = true

        val avatarIds: List<UUID> = TODO("GPU: query world avatars in range")
        for (av in avatarIds) {
            if (excludeAgentFromSearchResults && av == Agent.id) continue
            val avName = AvatarNameCache.get(av)
            if (avName == null) {
                nearMeList.add(ScrollListItem(id = av, label = AvatarNameCache.defaultName()))
                allLoaded = false
            } else {
                nearMeList.add(ScrollListItem(id = av, label = avName.displayName))
                avatarNameMap[av] = avName
            }
            empty = false
        }

        if (empty) {
            setOkBtnEnabled(false)
        } else {
            setOkBtnEnabled(true)
            onList()
        }

        if (allLoaded) nearMeListComplete = true
    }

    fun populateFriend() {
        friendsList.clear()
        val friendList: Map<UUID, Any> = AvatarTracker.getBuddyList()
        for ((avId, _) in friendList) {
            val avName = AvatarNameCache.get(avId) ?: AvatarName("", "")
            friendsList.add(ScrollListItem(id = avId, label = avName.displayName))
        }
        friendsList.sortBy { it.label }
    }

    fun populateContactSets() {
        TODO("APR: use JVM equivalent for contact sets")
    }

    fun onContactSetSelected() {
        onList()
    }

    fun draw() {
        TODO("GPU: drawFrustum cone to owner")
        if (!nearMeListComplete && activeTab == "NearMePanel") {
            populateNearMe()
        }
    }

    fun find() {
        avatarNameMap.clear()
        var text = getEditText()
        var agentId: UUID? = null

        val separatorIndex = text.indexOfFirst { it == ' ' || it == '.' || it == '_' }
        if (separatorIndex >= 0) {
            val last = text.substring(separatorIndex + 1).trim()
            if (last == "Resident") text = text.substring(0, separatorIndex)
        } else if (text.isNotEmpty()) {
            agentId = runCatching { UUID.fromString(text) }.getOrNull()
        }

        queryId = UUID.randomUUID()
        numResultsReturned = 0
        searchResults.clear()
        setOkBtnEnabled(false)

        if (agentId != null) {
            val region = Agent.region
            val capUrl = region?.getCapability("GetDisplayNames")
            if (!capUrl.isNullOrEmpty()) {
                val url = buildString {
                    append(capUrl.trimEnd('/'))
                    append("/?ids=")
                    append(agentId.toString())
                }
                TODO("APR: launch coroutine findByIdCoro($url, queryId, agentId, key.toString())")
            } else {
                processResponse(queryId, mapOf("failure_reason" to "ServerUnavailable"))
            }
        } else {
            val region = Agent.region
            val capUrl = region?.getCapability("AvatarPickerSearch")
            if (!capUrl.isNullOrEmpty()) {
                val url = buildString {
                    append(capUrl.trimEnd('/'))
                    append("/?page_size=100&names=")
                    append(text.replace('.', ' '))
                }
                TODO("APR: launch coroutine findByNameCoro($url, queryId, key.toString())")
            } else {
                queryNameMap[queryId] = key.toString()
                TODO("APR: send legacy AvatarPickerRequest message via gMessageSystem")
            }
        }
    }

    fun processResponse(queryId: UUID, content: Map<String, Any>) {
        if (queryId != this.queryId) return
        searchResults.clear()

        val failureReason = content["failure_reason"] as? String
        if (failureReason != null) {
            setOkBtnEnabled(false)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val agents = content["agents"] as? List<Map<String, Any>> ?: emptyList()
        for (row in agents) {
            val id = row["id"] as? UUID ?: continue
            if (id == Agent.id && excludeAgentFromSearchResults) continue
            val displayName = row["display_name"] as? String ?: ""
            val userName = row["username"] as? String ?: ""
            searchResults.add(ScrollListItem(id = id, label = displayName, subLabel = userName))
            avatarNameMap[id] = AvatarName(displayName = displayName, userName = userName)
        }

        if (searchResults.isEmpty()) {
            setOkBtnEnabled(false)
        } else {
            setOkBtnEnabled(true)
            searchResults.sortBy { it.subLabel }
            onList()
        }
    }

    fun setAllowMultiple(allowMultiple: Boolean) {
        allowMultipleSelection = allowMultiple
    }

    fun handleDragAndDrop(
        x: Int, y: Int, drop: Boolean, cargoType: DragAndDropType, cargoData: Any?
    ): Boolean {
        val list = getActiveListForTab(activeTab) ?: run {
            return true
        }
        val item = list.firstOrNull { it.containsPoint(x, y) }
        if (item?.id != null && item.id != UUID(0, 0) && item.id != Agent.id) {
            if (drop) {
                TODO("APR: open IM session for drag-and-drop")
            }
            return true
        }
        return true
    }

    fun openFriendsTab() {
        activeTab = "FriendsPanel"
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (key == KEY_RETURN && mask == MASK_NONE) {
            if (isEditFocused()) onBtnFind()
            else if (isEditUuidFocused()) onBtnFindUuid()
            else onBtnSelect()
            return true
        }
        if (key == KEY_ESCAPE && mask == MASK_NONE) {
            closeFloater()
            return true
        }
        return false
    }

    fun isSelectBtnEnabled(): Boolean {
        val hasItems = visibleItemsSelected()
        if (!hasItems || isMinimized()) return false

        if (activeTab == "ContactSetsPanel") {
            if (!allowMultipleSelection) return false
            return if (validateSignal.isNotEmpty()) validateSignal.all { it(emptyList()) } else true
        }

        val list = getActiveListForTab(activeTab) ?: return false
        val (ids, _) = getSelectedAvatarData(list)
        return if (ids.isNotEmpty()) {
            if (validateSignal.isNotEmpty()) validateSignal.all { it(ids) } else true
        } else false
    }

    private fun visibleItemsSelected(): Boolean {
        return when (activeTab) {
            "SearchPanel" -> searchResults.any { it.selected }
            "NearMePanel" -> nearMeList.any { it.selected }
            "FriendsPanel" -> friendsList.any { it.selected }
            "SearchPanelUUID" -> searchResultsUuid.any { it.selected }
            "ContactSetsPanel" -> {
                if (!allowMultipleSelection) false
                else TODO("APR: check contact set has members")
            }
            else -> false
        }
    }

    private fun getActiveListForTab(tab: String): List<ScrollListItem>? = when (tab) {
        "SearchPanel" -> searchResults
        "NearMePanel" -> nearMeList
        "FriendsPanel" -> friendsList
        "SearchPanelUUID" -> searchResultsUuid
        else -> null
    }

    private fun collectContactSetMembers(): Pair<MutableList<UUID>, MutableList<AvatarName>> {
        TODO("APR: collect members from LGGContactSets")
    }

    private fun setSearchResultsEnabled(enabled: Boolean) { TODO("GPU: update UI state") }
    private fun setOkBtnEnabled(enabled: Boolean) { TODO("GPU: update UI state") }
    private fun closeFloater() { TODO("GPU: close floater") }
    private fun isMinimized(): Boolean { TODO("GPU: query floater state"); return false }
    private fun getEditText(): String { TODO("GPU: read Edit field"); return "" }
    private fun isEditFocused(): Boolean { TODO("GPU: query focus"); return false }
    private fun isEditUuidFocused(): Boolean { TODO("GPU: query focus"); return false }
    private fun getString(key: String): String { TODO("GPU: look up localized string"); return key }
}

data class ScrollListItem(
    val id: UUID?,
    val label: String,
    val subLabel: String = "",
    var selected: Boolean = false
) {
    fun containsPoint(x: Int, y: Int): Boolean { TODO("GPU: hit test") }
}

object Agent {
    val id: UUID get() = TODO("APR: return current agent UUID")
    val region: ViewerRegion? get() = TODO("APR: return current region")
}

object AvatarNameCache {
    fun get(id: UUID): AvatarName? = TODO("APR: look up avatar name cache")
    fun defaultName(): String = TODO("APR: return cache default name placeholder")
}

object AvatarTracker {
    fun getBuddyList(): Map<UUID, Any> = TODO("APR: return buddy/friend map")
}

object FloaterReg {
    inline fun <reified T> findInstance(type: String, key: Any): T? =
        TODO("APR: look up floater registry")
}

class ViewerRegion {
    fun getCapability(name: String): String? = TODO("APR: capability lookup")
}

class MessageSystem {
    fun getUUID(block: String, field: String, index: Int = 0): UUID = TODO("APR: message field read")
    fun getString(block: String, field: String, index: Int = 0): String = TODO("APR: message field read")
    fun getNumberOfBlocks(block: String): Int = TODO("APR: message block count")
}

object FloaterReg2

enum class DragAndDropType { TEXTURE, SOUND, LANDMARK, SCRIPT, OBJECT, NOTECARD, CLOTHING, BODYPART, ANIMATION, GESTURE, CALLINGCARD, MESH, SETTINGS, MATERIAL, CATEGORY }

const val KEY_RETURN = 13
const val KEY_ESCAPE = 27
const val MASK_NONE = 0
