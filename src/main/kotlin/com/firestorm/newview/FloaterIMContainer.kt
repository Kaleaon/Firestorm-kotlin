package com.firestorm.newview

import java.util.ArrayDeque
import java.util.UUID

private const val EVENTS_PER_IDLE_LOOP_CURRENT_SESSION = 80
private const val EVENTS_PER_IDLE_LOOP_BACKGROUND = 40
private const val EVENTS_PER_IDLE_LOOP_MIN_PERCENTAGE = 0.01f

class FloaterIMContainer(private val seed: Map<String, Any>) {

    private val sessions: MutableMap<UUID, Any> = mutableMapOf()
    private var newMessageSlot: (() -> Unit)? = null
    private var microChangedSlot: (() -> Unit)? = null

    private var expandCollapseBtn: Any? = null
    private var stubCollapseBtn: Any? = null
    private var speakBtn: Any? = null
    private var stubPanel: Any? = null
    private var stubTextBox: Any? = null
    private var messagesPane: Any? = null
    private var conversationsPane: Any? = null
    private var conversationsStack: Any? = null

    private var initialized: Boolean = false
    private var isFirstLaunch: Boolean = true
    private var isFirstOpen: Boolean = false

    var selectedSession: UUID? = null
    private var generalTitle: String = ""
    private var generalTitleInUse: Boolean = true

    private val conversationsItems: MutableMap<UUID, Any> = mutableMapOf()
    private val conversationsWidgets: MutableMap<UUID, Any> = mutableMapOf()
    private var conversationsListPanel: Any? = null
    private var conversationsRoot: Any? = null

    private val conversationEventQueue: MutableMap<UUID, ArrayDeque<Map<String, Any>>> = mutableMapOf()

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun onOpen(key: Any?) {
        reSelectConversation()
        assignResizeLimits()
    }

    fun draw() {
        TODO("APR: use JVM equivalent")
    }

    fun setMinimized(minimize: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    open fun setVisible(visible: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    fun setVisibleAndFrontmost(takeFocus: Boolean = true, key: Any? = null) {
        TODO("APR: use JVM equivalent")
    }

    fun updateResizeLimits() {
        assignResizeLimits()
    }

    fun handleReshape(rect: Any, byUser: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    fun onCloseFloater(id: UUID) {
        sessions.remove(id)
    }

    fun addFloater(floater: Any, selectAddedFloater: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    fun returnFloaterToHost() {
        TODO("APR: use JVM equivalent")
    }

    fun showConversation(sessionId: UUID) {
        selectConversationPair(sessionId, selectWidget = false, focusFloater = false)
    }

    fun selectConversation(sessionId: UUID) {
        selectConversationPair(sessionId, selectWidget = true)
    }

    fun selectNextConversationByID(sessionId: UUID) {
        selectAdjacentConversation(false)
    }

    fun selectConversationPair(sessionId: UUID, selectWidget: Boolean, focusFloater: Boolean = true): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun clearAllFlashStates() {
        TODO("APR: use JVM equivalent")
    }

    fun selectAdjacentConversation(focusSelected: Boolean): Boolean {
        return selectNextorPreviousConversation(selectNext = true, focusSelected = focusSelected)
    }

    fun selectNextorPreviousConversation(selectNext: Boolean, focusSelected: Boolean = true): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun expandConversation() {
        TODO("APR: use JVM equivalent")
    }

    fun tabClose() {
        TODO("APR: use JVM equivalent")
    }

    fun showStub(visible: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    fun collapseMessagesPane(collapse: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    fun isMessagesPaneCollapsed(): Boolean = TODO("APR: use JVM equivalent")
    fun isConversationsPaneCollapsed(): Boolean = TODO("APR: use JVM equivalent")

    fun sessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean) {
        addConversationListItem(sessionId)
        TODO("APR: use JVM equivalent")
    }

    fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) {
        setVisibleAndFrontmost(false)
        selectConversationPair(sessionId, true)
        collapseMessagesPane(false)
    }

    fun sessionVoiceOrIMStarted(sessionId: UUID) {
        addConversationListItem(sessionId)
        TODO("APR: use JVM equivalent")
    }

    fun sessionRemoved(sessionId: UUID) {
        removeConversationListItem(sessionId)
    }

    fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {
        sessions.remove(oldSessionId)
        val changeFocus = removeConversationListItem(oldSessionId)
        addConversationListItem(newSessionId, changeFocus)
        TODO("APR: use JVM equivalent")
    }

    fun getSessionModel(sessionId: UUID): Any? = conversationsItems[sessionId]

    fun checkContextMenuItem(item: String, selectedIds: MutableList<UUID>): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun enableContextMenuItem(item: String, selectedIds: MutableList<UUID>): Boolean {
        TODO("APR: use JVM equivalent")
    }

    fun doToParticipants(item: String, selectedIds: MutableList<UUID>) {
        TODO("APR: use JVM equivalent")
    }

    fun assignResizeLimits() {
        TODO("APR: use JVM equivalent")
    }

    fun closeFloater(appQuitting: Boolean = false) {
        TODO("APR: use JVM equivalent")
    }

    fun closeAllConversations(appQuitting: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    fun closeSelectedConversations(ids: List<UUID>) {
        TODO("APR: use JVM equivalent")
    }

    fun isFrontmost(): Boolean = TODO("APR: use JVM equivalent")

    fun removeConversationListItem(id: UUID, changeFocus: Boolean = true): Boolean {
        conversationsItems.remove(id)
        conversationsWidgets.remove(id)
        return changeFocus
    }

    fun addConversationListItem(id: UUID, isWidgetSelected: Boolean = false): Any? {
        TODO("APR: use JVM equivalent")
    }

    fun setTimeNow(sessionId: UUID, participantId: UUID) {
        TODO("APR: use JVM equivalent")
    }

    fun setNearbyDistances() {
        TODO("APR: use JVM equivalent")
    }

    fun reSelectConversation() {
        TODO("APR: use JVM equivalent")
    }

    fun updateSpeakBtnState() {
        TODO("APR: use JVM equivalent")
    }

    fun flashConversationItemWidget(sessionId: UUID, isFlashing: Boolean, alternateColor: Boolean = false) {
        TODO("APR: use JVM equivalent")
    }

    fun highlightConversationItemWidget(sessionId: UUID, isHighlighted: Boolean) {
        TODO("APR: use JVM equivalent")
    }

    fun getConversationListItemSize(): Int = conversationsWidgets.size

    fun getDetachedConversationFloaters(floaters: MutableList<Any>) {
        TODO("APR: use JVM equivalent")
    }

    private fun onNewMessageReceived(data: Map<String, Any>) {
        val sessionId = data["session_id"] as? UUID ?: return
        TODO("APR: use JVM equivalent")
    }

    private fun onExpandCollapseButtonClicked() {
        TODO("APR: use JVM equivalent")
    }

    private fun onStubCollapseButtonClicked() {
        TODO("APR: use JVM equivalent")
    }

    private fun processParticipantsStyleUpdate() {
        TODO("APR: use JVM equivalent")
    }

    private fun onSpeakButtonPressed() {
        TODO("APR: use JVM equivalent")
    }

    private fun onSpeakButtonReleased() {
        TODO("APR: use JVM equivalent")
    }

    private fun onClickCloseBtn(appQuitting: Boolean = false) {
        TODO("APR: use JVM equivalent")
    }

    private fun collapseConversationsPane(collapse: Boolean, saveIsAllowed: Boolean = true) {
        TODO("APR: use JVM equivalent")
    }

    private fun reshapeFloaterAndSetResizeLimits(collapse: Boolean, deltaWidth: Int) {
        TODO("APR: use JVM equivalent")
    }

    private fun onAddButtonClicked() {
        TODO("APR: use JVM equivalent")
    }

    private fun onAvatarPicked(ids: List<UUID>) {
        TODO("APR: use JVM equivalent")
    }

    private fun isActionChecked(userdata: Any?): Boolean = TODO("APR: use JVM equivalent")
    private fun onCustomAction(userdata: Any?) { TODO("APR: use JVM equivalent") }

    private fun setSortOrderSessions(order: Int) { TODO("APR: use JVM equivalent") }
    private fun setSortOrderParticipants(order: Int) { TODO("APR: use JVM equivalent") }
    private fun setSortOrder(order: Any) { TODO("APR: use JVM equivalent") }

    private fun getSelectedUUIDs(selectedUuids: MutableList<UUID>, participantUuids: Boolean = true) {
        TODO("APR: use JVM equivalent")
    }

    private fun doToSelected(userdata: Any?) { TODO("APR: use JVM equivalent") }
    private fun doToSelectedGroup(userdata: Any?) { TODO("APR: use JVM equivalent") }
    private fun doToSelectedConversation(command: String, selectedIds: MutableList<UUID>) { TODO("APR: use JVM equivalent") }

    private fun enableModerateContextMenuItem(userdata: String, isSelf: Boolean = false): Boolean = TODO("APR: use JVM equivalent")
    private fun isGroupModerator(): Boolean = TODO("APR: use JVM equivalent")
    private fun haveAbilityToBan(): Boolean = TODO("APR: use JVM equivalent")
    private fun canBanSelectedMember(participantUuid: UUID): Boolean = TODO("APR: use JVM equivalent")
    private fun isMuted(avatarId: UUID): Boolean = TODO("APR: use JVM equivalent")
    private fun moderateVoice(command: String, userId: UUID) { TODO("APR: use JVM equivalent") }
    private fun moderateVoiceAllParticipants(unmute: Boolean) { TODO("APR: use JVM equivalent") }
    private fun moderateVoiceParticipant(avatarId: UUID, unmute: Boolean) { TODO("APR: use JVM equivalent") }
    private fun toggleAllowTextChat(participantUuid: UUID) { TODO("APR: use JVM equivalent") }
    private fun banSelectedMember(participantUuid: UUID) { TODO("APR: use JVM equivalent") }
    private fun openNearbyChat() { TODO("APR: use JVM equivalent") }
    private fun isParticipantListExpanded(): Boolean = TODO("APR: use JVM equivalent")

    private fun idleUpdate() { TODO("APR: use JVM equivalent") }
    private fun idleProcessEvents() { TODO("APR: use JVM equivalent") }

    private fun onConversationModelEvent(event: Map<String, Any>): Boolean {
        handleConversationModelEvent(event)
        return false
    }

    private fun handleConversationModelEvent(event: Map<String, Any>) {
        TODO("APR: use JVM equivalent")
    }

    companion object {
        fun findInstance(): FloaterIMContainer? = TODO("APR: use JVM equivalent")
        fun getInstance(): FloaterIMContainer = TODO("APR: use JVM equivalent")

        fun onCurrentChannelChanged(sessionId: UUID) {
            if (sessionId != null) {
                getInstance().showConversation(sessionId)
            }
        }

        fun isConversationLoggingAllowed(): Boolean = TODO("APR: use JVM equivalent")

        fun idle(userData: Any?) {
            (userData as? FloaterIMContainer)?.idleUpdate()
        }

        private fun confirmMuteAllCallback(notification: Any?, response: Any?) {
            TODO("APR: use JVM equivalent")
        }
    }
}
