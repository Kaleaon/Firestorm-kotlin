package com.firestorm.newview

import com.firestorm.ui.AvatarName
import com.firestorm.ui.Floater
import com.firestorm.ui.FloaterReg
import com.firestorm.ui.LLSD
import com.firestorm.ui.ScrollListCtrl
import com.firestorm.ui.TabContainer
import java.util.UUID

typealias SelectCallback = (avatarIds: List<UUID>, avatarNames: List<AvatarName>) -> Unit
typealias ValidateCallback = (avatarIds: List<UUID>) -> Boolean

class FloaterAvatarPicker(key: LLSD) : Floater(key) {

    private var selectionCallback: SelectCallback? = null
    private var numResultsReturned: Int = 0
    private var nearMeListComplete: Boolean = false
    private var closeOnSelect: Boolean = false
    private var excludeAgentFromSearchResults: Boolean = false
    private var allowMultipleSelection: Boolean = false

    private var queryId: UUID = UUID.randomUUID()

    private var contextConeOpacity: Float = 0f
    private val contextConeInAlpha: Float = CONTEXT_CONE_IN_ALPHA
    private val contextConeOutAlpha: Float = CONTEXT_CONE_OUT_ALPHA
    private val contextConeFadeTime: Float = CONTEXT_CONE_FADE_TIME

    private val okButtonValidateListeners: MutableList<ValidateCallback> = mutableListOf()

    private var findUuidNameCacheConnection: AutoCloseable? = null

    companion object {
        private const val AVATAR_PICKER_SEARCH_TIMEOUT: Int = 180
        private const val CONTEXT_CONE_IN_ALPHA: Float = 0.0f
        private const val CONTEXT_CONE_OUT_ALPHA: Float = 1.0f
        private const val CONTEXT_CONE_FADE_TIME: Float = 0.08f

        private val avatarNameMap: MutableMap<UUID, AvatarName> = mutableMapOf()

        val queryNameMap: MutableMap<UUID, String> = mutableMapOf()

        fun show(
            callback: SelectCallback,
            allowMultiple: Boolean = false,
            closeOnSelect: Boolean = false,
            skipAgent: Boolean = false,
            name: String = "",
            frustumOrigin: Any? = null
        ): FloaterAvatarPicker? {
            val floater = FloaterReg.showTypedInstance<FloaterAvatarPicker>(
                "avatar_picker", LLSD.fromString(name)
            ) ?: return null

            floater.selectionCallback = callback
            floater.setAllowMultiple(allowMultiple)
            floater.nearMeListComplete = false
            floater.closeOnSelect = closeOnSelect
            floater.excludeAgentFromSearchResults = skipAgent

            return floater
        }

        fun processAvatarPickerReply(msg: Any) {
            TODO("APR: use JVM equivalent")
        }

        private suspend fun findByIdCoro(url: String, queryId: UUID, agentId: UUID, floaterKey: String) {
            TODO("APR: use JVM equivalent")
        }

        private suspend fun findByNameCoro(url: String, queryId: UUID, name: String) {
            TODO("APR: use JVM equivalent")
        }
    }

    fun isExcludeAgentFromSearchResults(): Boolean = excludeAgentFromSearchResults

    fun postBuild(): Boolean {
        TODO("GPU: wire up child UI controls")
    }

    fun setOkBtnEnableCb(cb: ValidateCallback) {
        okButtonValidateListeners += cb
    }

    fun processResponse(queryId: UUID, content: LLSD) {
        if (queryId != this.queryId) return
        val searchResults = getScrollList("SearchResults") ?: return
        searchResults.deleteAllItems()

        if (content.has("failure_reason")) {
            searchResults.setCommentText(content["failure_reason"].asString())
            setChildEnabled("ok_btn", false)
            return
        }

        val agents = content["agents"]
        agents.asArray().forEach { row ->
            if (row["id"].asUUID() != agentId() || !excludeAgentFromSearchResults) {
                val item = LLSD.map(
                    "id" to row["id"],
                    "columns" to LLSD.array(
                        LLSD.map("column" to LLSD.fromString("name"), "value" to row["display_name"]),
                        LLSD.map("column" to LLSD.fromString("username"), "value" to row["username"])
                    )
                )
                searchResults.addElement(item)
                avatarNameMap[row["id"].asUUID()] = AvatarName.fromLLSD(row)
            }
        }

        if (searchResults.isEmpty()) {
            val editText = getChildValue("Edit").asString()
            searchResults.addElement(
                LLSD.map(
                    "id" to LLSD.fromUUID(null),
                    "columns" to LLSD.array(
                        LLSD.map("column" to LLSD.fromString("name"), "value" to LLSD.fromString("'$editText'")),
                        LLSD.map("column" to LLSD.fromString("username"), "value" to LLSD.fromString(getString("not_found_text")))
                    )
                )
            )
            searchResults.setEnabled(false)
            setChildEnabled("ok_btn", false)
        } else {
            setChildEnabled("ok_btn", true)
            searchResults.setEnabled(true)
            searchResults.sortByColumnIndex(1, ascending = true)
            val text = getChildValue("Edit").asString()
            if (!searchResults.selectItemByLabel(text, true, 1)) {
                searchResults.selectFirstItem()
            }
            onList()
            searchResults.setFocus(true)
        }
    }

    fun handleDragAndDrop(x: Int, y: Int, mask: Int, drop: Boolean, cargoType: Int, cargoData: Any?, accept: IntArray, tooltipMsg: StringBuilder): Boolean {
        TODO("GPU: handle drag and drop onto active list")
    }

    fun openFriendsTab() {
        getTabContainer("ResidentChooserTabs")?.selectTabByName("FriendsPanel")
    }

    private fun onBtnFind() {
        find()
    }

    private fun onBtnFindUUID() {
        val searchResults = getScrollList("SearchResultsUUID") ?: return
        searchResults.deleteAllItems()
        searchResults.setCommentText(getString("searching"))
        findUuidNameCacheConnection?.close()
        val uuidText = getChildValue("EditUUID").asString()
        val avId = runCatching { UUID.fromString(uuidText) }.getOrNull() ?: return
        findUuidNameCacheConnection = AvatarNameCache.get(avId) { id, avName ->
            onFindUUIDAvatarNameCache(id, avName)
        }
    }

    private fun onFindUUIDAvatarNameCache(avId: UUID, avName: AvatarName) {
        findUuidNameCacheConnection = null
        val searchResults = getScrollList("SearchResultsUUID") ?: return
        searchResults.deleteAllItems()

        if (avName.getAccountName() != "(??).(??)")  {
            val item = LLSD.map(
                "id" to LLSD.fromUUID(avId),
                "columns" to LLSD.array(
                    LLSD.map("name" to LLSD.fromString("nameUUID"), "value" to LLSD.fromString(avName.getDisplayName())),
                    LLSD.map("name" to LLSD.fromString("usernameUUID"), "value" to LLSD.fromString(avName.getUserName()))
                )
            )
            searchResults.addElement(item)
            searchResults.setEnabled(true)
            searchResults.sortByColumnIndex(1, ascending = true)
            searchResults.selectFirstItem()
            onList()
            searchResults.setFocus(true)
            setChildEnabled("ok_btn", true)
        } else {
            val editText = getChildValue("EditUUID").asString()
            searchResults.addElement(
                LLSD.map(
                    "id" to LLSD.fromUUID(null),
                    "columns" to LLSD.array(
                        LLSD.map("column" to LLSD.fromString("nameUUID"), "value" to LLSD.fromString(getString("not_found").replace("[TEXT]", editText)))
                    )
                )
            )
            searchResults.setEnabled(false)
            setChildEnabled("ok_btn", false)
        }
    }

    private fun onBtnSelect() {
        if (!isSelectBtnEnabled()) return
        val cb = selectionCallback ?: return

        val activePanelName = getTabContainer("ResidentChooserTabs")?.getCurrentPanel()?.getName() ?: ""
        val avatarIds = mutableListOf<UUID>()
        val avatarNames = mutableListOf<AvatarName>()

        if (activePanelName == "ContactSetsPanel") {
            if (!allowMultipleSelection) return
            TODO("ContactSets: collect friends from selected set")
        } else {
            val list = when (activePanelName) {
                "SearchPanel"    -> getScrollList("SearchResults")
                "NearMePanel"    -> getScrollList("NearMe")
                "FriendsPanel"   -> getScrollList("Friends")
                "SearchPanelUUID"-> getScrollList("SearchResultsUUID")
                else             -> null
            }
            if (list != null) {
                getSelectedAvatarData(list, avatarIds, avatarNames)
                cb(avatarIds, avatarNames)
            }
        }

        getScrollList("SearchResults")?.deselectAllItems(true)
        getScrollList("NearMe")?.deselectAllItems(true)
        getScrollList("Friends")?.deselectAllItems(true)
        getScrollList("SearchResultsUUID")?.deselectAllItems(true)

        if (closeOnSelect) {
            closeOnSelect = false
            closeFloater()
        }
    }

    private fun onBtnRefresh() {
        val nearMe = getScrollList("NearMe") ?: return
        nearMe.deleteAllItems()
        nearMe.setCommentText(getString("searching"))
        nearMeListComplete = false
    }

    private fun onBtnClose() {
        closeFloater()
    }

    private fun onRangeAdjust() {
        onBtnRefresh()
    }

    private fun onList() {
        setChildEnabled("ok_btn", isSelectBtnEnabled())
    }

    private fun onTabChanged() {
        setChildEnabled("ok_btn", isSelectBtnEnabled())
    }

    private fun onContactSetSelected() {
        onList()
    }

    private fun populateNearMe() {
        TODO("APR: query nearby avatars from LLWorld and populate NearMe list")
    }

    private fun populateFriend() {
        TODO("APR: copy buddy list from AvatarTracker and populate Friends list")
    }

    private fun populateContactSets() {
        TODO("APR: load contact sets from LGGContactSets and populate ContactSetSelector combo")
    }

    private fun find() {
        avatarNameMap.clear()
        val text = getChildValue("Edit").asString()

        var normalizedText = text
        val sepIdx = text.indexOfFirst { it == ' ' || it == '.' || it == '_' }
        if (sepIdx >= 0) {
            val last = text.substring(sepIdx + 1).trim()
            if (last == "Resident") normalizedText = text.substring(0, sepIdx)
        }

        queryId = UUID.randomUUID()
        numResultsReturned = 0

        getScrollList("SearchResults")?.deleteAllItems()
        getScrollList("SearchResults")?.setCommentText(getString("searching"))
        setChildEnabled("ok_btn", false)

        val uuidCandidate = runCatching { UUID.fromString(normalizedText) }.getOrNull()
        if (uuidCandidate != null) {
            TODO("APR: launch findByIdCoro coroutine for $uuidCandidate")
        } else {
            TODO("APR: launch findByNameCoro coroutine for '$normalizedText'")
        }
    }

    private fun setAllowMultiple(allowMultiple: Boolean) {
        getScrollList("SearchResults")?.setAllowMultipleSelection(allowMultiple)
        getScrollList("NearMe")?.setAllowMultipleSelection(allowMultiple)
        getScrollList("Friends")?.setAllowMultipleSelection(allowMultiple)
        getScrollList("SearchResultsUUID")?.setAllowMultipleSelection(allowMultiple)
        allowMultipleSelection = allowMultiple

        val tabs = getTabContainer("ResidentChooserTabs") ?: return
        val contactSetsPanel = getPanel("ContactSetsPanel") ?: return
        val tabIndex = tabs.getIndexForPanel(contactSetsPanel)
        if (tabIndex >= 0) {
            tabs.enableTabButton(tabIndex, allowMultiple)
            if (!allowMultiple && tabs.getCurrentPanel() == contactSetsPanel) {
                tabs.selectTabByName("FriendsPanel")
            }
        }
    }

    private fun getActiveList(): ScrollListCtrl? {
        val panelName = getTabContainer("ResidentChooserTabs")?.getCurrentPanel()?.getName() ?: return null
        return when (panelName) {
            "SearchPanel"     -> getScrollList("SearchResults")
            "NearMePanel"     -> getScrollList("NearMe")
            "FriendsPanel"    -> getScrollList("Friends")
            "SearchPanelUUID" -> getScrollList("SearchResultsUUID")
            else              -> null
        }
    }

    private fun visibleItemsSelected(): Boolean {
        val panelName = getTabContainer("ResidentChooserTabs")?.getCurrentPanel()?.getName() ?: return false
        return when (panelName) {
            "SearchPanel"     -> (getScrollList("SearchResults")?.getFirstSelectedIndex() ?: -1) >= 0
            "NearMePanel"     -> (getScrollList("NearMe")?.getFirstSelectedIndex() ?: -1) >= 0
            "FriendsPanel"    -> (getScrollList("Friends")?.getFirstSelectedIndex() ?: -1) >= 0
            "SearchPanelUUID" -> (getScrollList("SearchResultsUUID")?.getFirstSelectedIndex() ?: -1) >= 0
            "ContactSetsPanel" -> {
                if (!allowMultipleSelection) return false
                TODO("ContactSets: check if selected set has members")
            }
            else              -> false
        }
    }

    private fun isSelectBtnEnabled(): Boolean {
        if (!visibleItemsSelected()) return false
        if (isMinimized()) return false

        val panelName = getTabContainer("ResidentChooserTabs")?.getCurrentPanel()?.getName() ?: return false
        if (panelName == "ContactSetsPanel") {
            if (!allowMultipleSelection) return false
            return if (okButtonValidateListeners.isNotEmpty()) okButtonValidateListeners.all { it(emptyList()) } else true
        }

        val list = getActiveList() ?: return false
        val ids = mutableListOf<UUID>()
        val names = mutableListOf<AvatarName>()
        getSelectedAvatarData(list, ids, names)
        if (ids.isEmpty()) return false
        return if (okButtonValidateListeners.isNotEmpty()) okButtonValidateListeners.all { it(ids) } else true
    }

    open fun draw() {
        drawFrustum()
        TODO("GPU: periodic ok-btn validation and near-me refresh")
    }

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("GPU: handle KEY_RETURN / KEY_ESCAPE")
    }

    private fun drawFrustum() {
        TODO("GPU: drawConeToOwner")
    }

    private fun getSelectedAvatarData(list: ScrollListCtrl, outIds: MutableList<UUID>, outNames: MutableList<AvatarName>) {
        list.getAllSelected().forEach { item ->
            val id = item.getUUID() ?: return@forEach
            outIds += id
            val avName = avatarNameMap[id] ?: AvatarNameCache.getCached(id) ?: AvatarName.unknown()
            outNames += avName
        }
    }

    private fun agentId(): UUID = TODO("APR: return gAgent.getID()")

    private fun getScrollList(name: String): ScrollListCtrl? = TODO("GPU: getChild<ScrollListCtrl>($name)")
    private fun getTabContainer(name: String): TabContainer? = TODO("GPU: getChild<TabContainer>($name)")
    private fun getPanel(name: String): Any? = TODO("GPU: getChild<Panel>($name)")
    private fun getChildValue(name: String): LLSD = TODO("GPU: getChild<UICtrl>($name).getValue()")
    private fun setChildEnabled(name: String, enabled: Boolean) { TODO("GPU: getChildView($name).setEnabled($enabled)") }
    private fun isMinimized(): Boolean = TODO("GPU: floater.isMinimized()")
    private fun getString(key: String): String = TODO("GPU: LLTrans.getString($key)")
    private fun getString(key: String, args: Map<String, String>): String = TODO("GPU: LLTrans.getString($key, args)")
    private fun closeFloater() { TODO("GPU: close this floater") }

    fun dispose() {
        findUuidNameCacheConnection?.close()
        findUuidNameCacheConnection = null
        queryNameMap.remove(queryId)
    }
}
