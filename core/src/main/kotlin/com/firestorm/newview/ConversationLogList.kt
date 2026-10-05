package com.firestorm.newview

import java.util.UUID

enum class SortOrder {
    BY_NAME,
    BY_DATE
}

interface ConversationLogObserver {
    fun changed()
    fun changed(sessionId: UUID, mask: UInt)

    companion object {
        const val CHANGED_TIME: UInt = 0x1u
        const val CHANGED_NAME: UInt = 0x2u
        const val CHANGED_OFFLINE_IMS: UInt = 0x4u
    }
}

abstract class FlatListViewEx {
    protected val items: MutableList<ConversationLogListItem> = mutableListOf()
    var noItemsCommentText: String = ""
    protected var comparator: Comparator<ConversationLogListItem>? = null

    fun addItem(item: ConversationLogListItem, id: UUID, addTop: Boolean = false) {
        if (addTop) items.add(0, item) else items.add(item)
    }

    fun clear() = items.clear()

    fun size(): Int = items.size

    fun getItems(): List<ConversationLogListItem> = items.toList()

    fun setComparator(c: Comparator<ConversationLogListItem>) {
        comparator = c
    }

    fun sort() {
        comparator?.let { items.sortWith(it) }
    }

    fun getSelectedItem(): ConversationLogListItem? = items.firstOrNull { it.isSelected }

    fun numSelected(): Int = items.count { it.isSelected }

    fun selectItemByUUID(id: UUID) {
        items.forEach { it.isSelected = (it.conversation?.sessionId == id) }
    }

    open fun draw() {}
}

class ConversationLogList : FlatListViewEx(), ConversationLogObserver {

    var isDirty: Boolean = true
        private set
    var isFriendsOnTop: Boolean = false
    private var nameFilter: String = ""
    private var contextMenu: ContextMenu? = null
    private val savedSettings: SavedSettings get() = SavedSettings.instance

    init {
        ConversationLog.instance.addObserver(this)
        isFriendsOnTop = savedSettings.getBool("SortFriendsFirst")
        contextMenu = ContextMenu("menu_conversation_log_gear")
    }

    fun dispose() {
        ConversationLog.instance.removeObserver(this)
    }

    override fun draw() {
        if (isDirty) refresh()
        super.draw()
    }

    fun handleRightMouseDown(x: Int, y: Int): Boolean {
        contextMenu?.let { menu ->
            if (size() > 0) menu.show(x, y)
        }
        return true
    }

    fun getContextMenu(): ContextMenu? = contextMenu

    fun addNewItem(conversation: Conversation) {
        val item = ConversationLogListItem(conversation)
        if (nameFilter.isNotEmpty()) item.highlightNameDate(nameFilter)
        addItem(item, conversation.sessionId, addTop = true)
    }

    fun setNameFilter(filter: String) {
        val upper = filter.uppercase()
        if (nameFilter != upper) {
            nameFilter = upper
            setDirty()
        }
    }

    fun sortByName() {
        setComparator(NameComparator(isFriendsOnTop))
        sort()
    }

    fun sortByDate() {
        setComparator(DateComparator(isFriendsOnTop))
        sort()
    }

    fun toggleSortFriendsOnTop() {
        isFriendsOnTop = !isFriendsOnTop
        savedSettings.setBool("SortFriendsFirst", isFriendsOnTop)
        sort()
    }

    override fun changed() {
        refresh()
    }

    override fun changed(sessionId: UUID, mask: UInt) {
        val item = getConversationLogListItem(sessionId) ?: return

        when {
            mask and ConversationLogObserver.CHANGED_TIME != 0u -> {
                item.updateTimestamp()
                if (getSortOrder() == SortOrder.BY_DATE) isDirty = true
            }
            mask and ConversationLogObserver.CHANGED_NAME != 0u -> {
                item.updateName()
                if (getSortOrder() == SortOrder.BY_DATE) isDirty = true
            }
            mask and ConversationLogObserver.CHANGED_OFFLINE_IMS != 0u -> {
                item.updateOfflineIMs()
            }
        }
    }

    private fun setDirty(dirty: Boolean = true) {
        isDirty = dirty
    }

    private fun refresh() {
        rebuildList()
        sort()
        isDirty = false
    }

    private fun rebuildList() {
        val selectedConversation = getSelectedConversation()
        clear()

        val haveFilter = nameFilter.isNotEmpty()
        val logInstance = ConversationLog.instance
        val conversations = logInstance.getConversations()

        for (conv in conversations) {
            if (haveFilter && !findInsensitive(conv.conversationName, nameFilter) && !findInsensitive(conv.timestamp, nameFilter)) {
                continue
            }
            addNewItem(conv)
        }

        selectedConversation?.let { selectItemByUUID(it.sessionId) }

        val loggingEnabled = logInstance.isLoggingEnabled
        val logEmpty = logInstance.isLogEmpty

        noItemsCommentText = when {
            !loggingEnabled && logEmpty -> Trans.getString("logging_calls_disabled_log_empty")
            !loggingEnabled && !logEmpty -> Trans.getString("logging_calls_disabled_log_not_empty")
            loggingEnabled && logEmpty -> Trans.getString("logging_calls_enabled_log_empty")
            else -> ""
        }
    }

    private fun findInsensitive(haystack: String, needleUpper: String): Boolean =
        haystack.uppercase().contains(needleUpper)

    fun onCustomAction(commandName: String) {
        val selectedConversation = getSelectedConversation() ?: return
        val participantId = selectedConversation.participantId
        val sessionId = selectedConversation.sessionId
        val stype = getSelectedSessionType()

        when (commandName) {
            "im" -> when (stype) {
                SessionType.P2P -> AvatarActions.startIM(participantId)
                SessionType.GROUP -> GroupActions.startIM(sessionId)
                else -> Unit
            }
            "call" -> when (stype) {
                SessionType.P2P -> AvatarActions.startCall(participantId)
                SessionType.GROUP -> GroupActions.startCall(sessionId)
                else -> Unit
            }
            "view_profile" -> when (stype) {
                SessionType.P2P -> AvatarActions.showProfile(participantId)
                SessionType.GROUP -> GroupActions.show(sessionId)
                else -> Unit
            }
            "chat_history" -> FloaterReg.showInstance("preview_conversation", sessionId)
            "offer_teleport" -> AvatarActions.offerTeleport(participantId)
            "request_teleport" -> AvatarActions.teleportRequest(participantId)
            "add_friend" -> {
                if (!AvatarActions.isFriend(participantId)) AvatarActions.requestFriendshipDialog(participantId)
            }
            "remove_friend" -> {
                if (AvatarActions.isFriend(participantId)) AvatarActions.removeFriendDialog(participantId)
            }
            "invite_to_group" -> AvatarActions.inviteToGroup(participantId)
            "show_on_map" -> AvatarActions.showOnMap(participantId)
            "share" -> AvatarActions.share(participantId)
            "pay" -> AvatarActions.pay(participantId)
            "block" -> AvatarActions.toggleBlock(participantId)
            "chat_history_external" -> {
                val logFile = LogChat.makeLogFileName(
                    ConversationLog.instance.getConversation(sessionId)?.historyFileName ?: ""
                )
                if (FileUtils.fileExists(logFile)) {
                    ViewerWindow.instance.openFile(logFile)
                } else {
                    NotificationsUtil.add("ChatHistoryIsMissing")
                }
            }
        }
    }

    fun isActionEnabled(commandName: String): Boolean {
        val selectedConversation = getSelectedConversation() ?: return false
        if (numSelected() > 1) return false

        val stype = getSelectedSessionType()
        val selectedId = selectedConversation.participantId
        val isP2p = stype == SessionType.P2P
        val isGroup = stype == SessionType.GROUP
        val isGroupMember = isGroup && Agent.instance.isInGroup(selectedId)

        return when (commandName) {
            "can_im" -> isP2p || isGroupMember
            "can_view_profile" -> isP2p || isGroup
            "can_view_chat_history" -> true
            "can_call" -> (isP2p || isGroupMember) && AvatarActions.canCall()
            "can_pay" -> isP2p && RlvActions.canPayAvatar(selectedId)
            "add_rem_friend",
            "can_invite_to_group",
            "can_share",
            "can_block",
            "report_abuse" -> isP2p
            "can_offer_teleport" -> isP2p && AvatarActions.canOfferTeleport(selectedId)
            "can_request_teleport" -> isP2p && AvatarActions.canRequestTeleport(selectedId)
            "can_show_on_map" -> isP2p && (
                (AvatarTracker.instance.isBuddyOnline(selectedId) && AvatarTracker.instance.isAgentMappable(selectedId)) ||
                Agent.instance.isGodlike
            )
            else -> false
        }
    }

    fun isActionChecked(commandName: String): Boolean {
        val selectedConversation = getSelectedConversation() ?: return false
        val selectedId = selectedConversation.participantId
        val isP2p = getSelectedSessionType() == SessionType.P2P

        return when (commandName) {
            "is_blocked" -> isP2p && AvatarActions.isBlocked(selectedId)
            "is_friend" -> isP2p && AvatarActions.isFriend(selectedId)
            "is_not_friend" -> isP2p && !AvatarActions.isFriend(selectedId)
            else -> false
        }
    }

    private fun getSelectedSessionType(): SessionType {
        return getSelectedConversationPanel()?.conversation?.conversationType ?: SessionType.NONE
    }

    private fun getSelectedConversationPanel(): ConversationLogListItem? =
        getSelectedItem()

    private fun getSelectedConversation(): Conversation? =
        getSelectedConversationPanel()?.conversation

    fun getConversationLogListItem(sessionId: UUID): ConversationLogListItem? =
        items.firstOrNull { it.conversation?.sessionId == sessionId }

    private fun getSortOrder(): SortOrder {
        val order = savedSettings.getUInt("CallLogSortOrder")
        return if (order == 1u) SortOrder.BY_DATE else SortOrder.BY_NAME
    }
}

abstract class ConversationLogListItemComparator : Comparator<ConversationLogListItem> {
    override fun compare(a: ConversationLogListItem, b: ConversationLogListItem): Int {
        requireNotNull(a.conversation) { "conversation item a cannot be null" }
        requireNotNull(b.conversation) { "conversation item b cannot be null" }
        return doCompare(a, b)
    }

    protected abstract fun doCompare(a: ConversationLogListItem, b: ConversationLogListItem): Int
}

class NameComparator(private val friendsFirst: Boolean) : ConversationLogListItemComparator() {
    override fun doCompare(a: ConversationLogListItem, b: ConversationLogListItem): Int {
        val conv1 = a.conversation!!
        val conv2 = b.conversation!!
        val name1 = conv1.conversationName.uppercase()
        val name2 = conv2.conversationName.uppercase()
        val id1 = conv1.participantId
        val id2 = conv2.participantId

        if (friendsFirst) {
            val f1 = AvatarActions.isFriend(id1)
            val f2 = AvatarActions.isFriend(id2)
            if (f1 != f2) return if (f1) -1 else 1
        }

        return name1.compareTo(name2)
    }
}

class DateComparator(private val friendsFirst: Boolean) : ConversationLogListItemComparator() {
    override fun doCompare(a: ConversationLogListItem, b: ConversationLogListItem): Int {
        val conv1 = a.conversation!!
        val conv2 = b.conversation!!
        val id1 = conv1.participantId
        val id2 = conv2.participantId

        if (friendsFirst) {
            val f1 = AvatarActions.isFriend(id1)
            val f2 = AvatarActions.isFriend(id2)
            if (f1 != f2) return if (f1) -1 else 1
        }

        return conv2.time.compareTo(conv1.time)
    }
}

// Stubs for viewer-side types referenced above
enum class SessionType { NONE, P2P, GROUP, ADHOC }

data class Conversation(
    val sessionId: UUID,
    val participantId: UUID,
    val conversationName: String,
    val timestamp: String,
    val time: Long,
    val conversationType: SessionType,
    val historyFileName: String,
    val hasOfflineMessages: Boolean
)

object ConversationLog {
    val instance: ConversationLog get() = ConversationLog
    val isLoggingEnabled: Boolean get() = false
    val isLogEmpty: Boolean get() = true
    fun getConversations(): List<Conversation> = emptyList()
    fun getConversation(sessionId: UUID): Conversation? = null
    fun addObserver(observer: ConversationLogObserver): Unit {
        System.err.println("ConversationLog: addObserver not yet implemented")
    }
    fun removeObserver(observer: ConversationLogObserver): Unit {
        System.err.println("ConversationLog: removeObserver not yet implemented")
    }
    fun removeConversation(conversation: Conversation): Unit {
        System.err.println("ConversationLog: removeConversation not yet implemented")
    }
}

object SavedSettings {
    val instance: SavedSettings get() = SavedSettings
    fun getBool(key: String): Boolean = false
    fun setBool(key: String, value: Boolean): Unit {
        System.err.println("SavedSettings: setBool not yet implemented")
    }
    fun getUInt(key: String): UInt = 0u
    fun getInt(key: String): Int = 0
}

object AvatarActions {
    fun startIM(id: UUID): Unit {
        System.err.println("AvatarActions: startIM not yet implemented")
    }
    fun startCall(id: UUID): Unit {
        System.err.println("AvatarActions: startCall not yet implemented")
    }
    fun showProfile(id: UUID): Unit {
        System.err.println("AvatarActions: showProfile not yet implemented")
    }
    fun offerTeleport(id: UUID): Unit {
        System.err.println("AvatarActions: offerTeleport not yet implemented")
    }
    fun teleportRequest(id: UUID): Unit {
        System.err.println("AvatarActions: teleportRequest not yet implemented")
    }
    fun requestFriendshipDialog(id: UUID): Unit {
        System.err.println("AvatarActions: requestFriendshipDialog not yet implemented")
    }
    fun removeFriendDialog(id: UUID): Unit {
        System.err.println("AvatarActions: removeFriendDialog not yet implemented")
    }
    fun inviteToGroup(id: UUID): Unit {
        System.err.println("AvatarActions: inviteToGroup not yet implemented")
    }
    fun showOnMap(id: UUID): Unit {
        System.err.println("AvatarActions: showOnMap not yet implemented")
    }
    fun share(id: UUID): Unit {
        System.err.println("AvatarActions: share not yet implemented")
    }
    fun pay(id: UUID): Unit {
        System.err.println("AvatarActions: pay not yet implemented")
    }
    fun toggleBlock(id: UUID): Unit {
        System.err.println("AvatarActions: toggleBlock not yet implemented")
    }
    fun isFriend(id: UUID): Boolean = false
    fun isBlocked(id: UUID): Boolean = false
    fun canCall(): Boolean = false
    fun canOfferTeleport(id: UUID): Boolean = false
    fun canRequestTeleport(id: UUID): Boolean = false
}

object GroupActions {
    fun startIM(id: UUID): Unit {
        System.err.println("GroupActions: startIM not yet implemented")
    }
    fun startCall(id: UUID): Unit {
        System.err.println("GroupActions: startCall not yet implemented")
    }
    fun show(id: UUID): Unit {
        System.err.println("GroupActions: show not yet implemented")
    }
}

object Agent {
    val instance: Agent get() = Agent
    fun isInGroup(id: UUID): Boolean = false
    val isGodlike: Boolean get() = false
}

object AvatarTracker {
    val instance: AvatarTracker get() = AvatarTracker
    fun isBuddyOnline(id: UUID): Boolean = false
    fun isAgentMappable(id: UUID): Boolean = false
}

object RlvActions {
    fun canPayAvatar(id: UUID): Boolean = false
}

object FloaterReg {
    fun showInstance(name: String, id: UUID): Unit {
        System.err.println("FloaterReg: showInstance not yet implemented")
    }
}

object LogChat {
    fun makeLogFileName(name: String): String = ""
}

object FileUtils {
    fun fileExists(path: String): Boolean = false
}

object ViewerWindow {
    val instance: ViewerWindow get() = ViewerWindow
    val isInitialized: Boolean get() = false
    fun openFile(path: String): Unit {
        System.err.println("ViewerWindow: openFile not yet implemented")
    }
}

object NotificationsUtil {
    fun add(name: String): Unit {
        System.err.println("NotificationsUtil: add not yet implemented")
    }
}

object Trans {
    fun getString(key: String): String = ""
}

class ContextMenu(val name: String) {
    fun show(x: Int, y: Int): Unit {
        // no-op: render context menu popup not yet implemented
    }
}
