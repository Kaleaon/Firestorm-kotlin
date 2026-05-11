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
    val instance: ConversationLog get() = TODO("APR: use JVM equivalent")
    val isLoggingEnabled: Boolean get() = TODO("APR: use JVM equivalent")
    val isLogEmpty: Boolean get() = TODO("APR: use JVM equivalent")
    fun getConversations(): List<Conversation> = TODO("APR: use JVM equivalent")
    fun getConversation(sessionId: UUID): Conversation? = TODO("APR: use JVM equivalent")
    fun addObserver(observer: ConversationLogObserver): Unit = TODO("APR: use JVM equivalent")
    fun removeObserver(observer: ConversationLogObserver): Unit = TODO("APR: use JVM equivalent")
    fun removeConversation(conversation: Conversation): Unit = TODO("APR: use JVM equivalent")
}

object SavedSettings {
    val instance: SavedSettings get() = TODO("APR: use JVM equivalent")
    fun getBool(key: String): Boolean = TODO("APR: use JVM equivalent")
    fun setBool(key: String, value: Boolean): Unit = TODO("APR: use JVM equivalent")
    fun getUInt(key: String): UInt = TODO("APR: use JVM equivalent")
    fun getInt(key: String): Int = TODO("APR: use JVM equivalent")
}

object AvatarActions {
    fun startIM(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun startCall(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun showProfile(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun offerTeleport(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun teleportRequest(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun requestFriendshipDialog(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun removeFriendDialog(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun inviteToGroup(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun showOnMap(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun share(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun pay(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun toggleBlock(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun isFriend(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    fun isBlocked(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    fun canCall(): Boolean = TODO("APR: use JVM equivalent")
    fun canOfferTeleport(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    fun canRequestTeleport(id: UUID): Boolean = TODO("APR: use JVM equivalent")
}

object GroupActions {
    fun startIM(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun startCall(id: UUID): Unit = TODO("APR: use JVM equivalent")
    fun show(id: UUID): Unit = TODO("APR: use JVM equivalent")
}

object Agent {
    val instance: Agent get() = TODO("APR: use JVM equivalent")
    fun isInGroup(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    val isGodlike: Boolean get() = TODO("APR: use JVM equivalent")
}

object AvatarTracker {
    val instance: AvatarTracker get() = TODO("APR: use JVM equivalent")
    fun isBuddyOnline(id: UUID): Boolean = TODO("APR: use JVM equivalent")
    fun isAgentMappable(id: UUID): Boolean = TODO("APR: use JVM equivalent")
}

object RlvActions {
    fun canPayAvatar(id: UUID): Boolean = TODO("APR: use JVM equivalent")
}

object FloaterReg {
    fun showInstance(name: String, id: UUID): Unit = TODO("APR: use JVM equivalent")
}

object LogChat {
    fun makeLogFileName(name: String): String = TODO("APR: use JVM equivalent")
}

object FileUtils {
    fun fileExists(path: String): Boolean = TODO("APR: use JVM equivalent")
}

object ViewerWindow {
    val instance: ViewerWindow get() = TODO("APR: use JVM equivalent")
    val isInitialized: Boolean get() = TODO("APR: use JVM equivalent")
    fun openFile(path: String): Unit = TODO("APR: use JVM equivalent")
}

object NotificationsUtil {
    fun add(name: String): Unit = TODO("APR: use JVM equivalent")
}

object Trans {
    fun getString(key: String): String = TODO("APR: use JVM equivalent")
}

class ContextMenu(val name: String) {
    fun show(x: Int, y: Int): Unit = TODO("GPU: render context menu popup")
}
