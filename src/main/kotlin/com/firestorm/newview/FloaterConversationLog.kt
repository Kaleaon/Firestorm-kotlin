package com.firestorm.newview

class FloaterConversationLog(key: LLSD) : Floater(key) {

    private var conversationLogList: ConversationLogList? = null
    private var conversationsGearBtn: MenuButton? = null

    init {
        commitCallbackRegistrar.add("CallLog.Action") { userdata -> onCustomAction(userdata) }
        enableCallbackRegistrar.add("CallLog.Check") { userdata -> isActionChecked(userdata) }
    }

    override fun postBuild(): Boolean {
        conversationLogList = getChild<ConversationLogList>("conversation_log_list")

        when (savedSettings.getUInt("CallLogSortOrder")) {
            ConversationLogList.E_SORT_BY_NAME -> conversationLogList?.sortByName()
            ConversationLogList.E_SORT_BY_DATE -> conversationLogList?.sortByDate()
        }

        conversationsGearBtn = getChild<MenuButton>("conversations_gear_btn")
        val gearMenu = conversationLogList?.getContextMenu()
        if (gearMenu != null) {
            conversationsGearBtn?.setMenu(gearMenu, MenuButton.MP_BOTTOM_LEFT)
        }

        getChild<FilterEditor>("people_filter_input")?.setCommitCallback { _, searchString ->
            onFilterEdit(searchString)
        }

        return super.postBuild()
    }

    override fun draw() {
        conversationsGearBtn?.setEnabled(conversationLogList?.getSelectedItem() != null)
        super.draw()
    }

    fun onFilterEdit(searchString: String) {
        val filter = searchString.trimStart()
        conversationLogList?.setNameFilter(filter)
    }

    override fun handleKeyHere(key: Int, mask: Int): Boolean {
        if (FSCommon.isFilterEditorKeyCombo(key, mask)) {
            getChild<FilterEditor>("people_filter_input")?.setFocus(true)
            return true
        }
        return super.handleKeyHere(key, mask)
    }

    override fun hasAccelerators(): Boolean = true

    private fun onCustomAction(userdata: LLSD) {
        when (userdata.asString()) {
            "sort_by_name" -> {
                conversationLogList?.sortByName()
                savedSettings.setUInt("CallLogSortOrder", ConversationLogList.E_SORT_BY_NAME)
            }
            "sort_by_date" -> {
                conversationLogList?.sortByDate()
                savedSettings.setUInt("CallLogSortOrder", ConversationLogList.E_SORT_BY_DATE)
            }
            "sort_friends_on_top" -> conversationLogList?.toggleSortFriendsOnTop()
            "view_nearby_chat_history" -> FloaterReg.showInstance("preview_conversation", LLSD(NULL_UUID), true)
        }
    }

    private fun isActionEnabled(userdata: LLSD): Boolean = true

    private fun isActionChecked(userdata: LLSD): Boolean {
        val commandName = userdata.asString()
        val sortOrder = savedSettings.getUInt("CallLogSortOrder")
        return when (commandName) {
            "sort_by_name" -> sortOrder == ConversationLogList.E_SORT_BY_NAME
            "sort_by_date" -> sortOrder == ConversationLogList.E_SORT_BY_DATE
            "sort_friends_on_top" -> savedSettings.getBool("SortFriendsFirst")
            else -> false
        }
    }
}
