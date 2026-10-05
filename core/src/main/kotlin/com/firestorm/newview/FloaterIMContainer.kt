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
        System.err.println("FloaterIMContainer: postBuild not yet implemented")
        return false
    }

    fun onOpen(key: Any?) {
        reSelectConversation()
        assignResizeLimits()
    }

    fun draw() {
        System.err.println("FloaterIMContainer: draw not yet implemented")
    }

    fun setMinimized(minimize: Boolean) {
        System.err.println("FloaterIMContainer: setMinimized not yet implemented")
    }

    open fun setVisible(visible: Boolean) {
        System.err.println("FloaterIMContainer: setVisible not yet implemented")
    }

    fun setVisibleAndFrontmost(takeFocus: Boolean = true, key: Any? = null) {
        System.err.println("FloaterIMContainer: setVisibleAndFrontmost not yet implemented")
    }

    fun updateResizeLimits() {
        assignResizeLimits()
    }

    fun handleReshape(rect: Any, byUser: Boolean) {
        System.err.println("FloaterIMContainer: handleReshape not yet implemented")
    }

    fun onCloseFloater(id: UUID) {
        sessions.remove(id)
    }

    fun addFloater(floater: Any, selectAddedFloater: Boolean) {
        System.err.println("FloaterIMContainer: addFloater not yet implemented")
    }

    fun returnFloaterToHost() {
        System.err.println("FloaterIMContainer: returnFloaterToHost not yet implemented")
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
        System.err.println("FloaterIMContainer: selectConversationPair not yet implemented")
        return false
    }

    fun clearAllFlashStates() {
        System.err.println("FloaterIMContainer: clearAllFlashStates not yet implemented")
    }

    fun selectAdjacentConversation(focusSelected: Boolean): Boolean {
        return selectNextorPreviousConversation(selectNext = true, focusSelected = focusSelected)
    }

    fun selectNextorPreviousConversation(selectNext: Boolean, focusSelected: Boolean = true): Boolean {
        System.err.println("FloaterIMContainer: selectNextorPreviousConversation not yet implemented")
        return false
    }

    fun expandConversation() {
        System.err.println("FloaterIMContainer: expandConversation not yet implemented")
    }

    fun tabClose() {
        System.err.println("FloaterIMContainer: tabClose not yet implemented")
    }

    fun showStub(visible: Boolean) {
        System.err.println("FloaterIMContainer: showStub not yet implemented")
    }

    fun collapseMessagesPane(collapse: Boolean) {
        System.err.println("FloaterIMContainer: collapseMessagesPane not yet implemented")
    }

    fun isMessagesPaneCollapsed(): Boolean {
        System.err.println("FloaterIMContainer: isMessagesPaneCollapsed not yet implemented")
        return false
    }

    fun isConversationsPaneCollapsed(): Boolean {
        System.err.println("FloaterIMContainer: isConversationsPaneCollapsed not yet implemented")
        return false
    }

    fun sessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean) {
        addConversationListItem(sessionId)
        System.err.println("FloaterIMContainer: sessionAdded not yet implemented")
    }

    fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) {
        setVisibleAndFrontmost(false)
        selectConversationPair(sessionId, true)
        collapseMessagesPane(false)
    }

    fun sessionVoiceOrIMStarted(sessionId: UUID) {
        addConversationListItem(sessionId)
        System.err.println("FloaterIMContainer: sessionVoiceOrIMStarted not yet implemented")
    }

    fun sessionRemoved(sessionId: UUID) {
        removeConversationListItem(sessionId)
    }

    fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {
        sessions.remove(oldSessionId)
        val changeFocus = removeConversationListItem(oldSessionId)
        addConversationListItem(newSessionId, changeFocus)
        System.err.println("FloaterIMContainer: sessionIDUpdated not yet implemented")
    }

    fun getSessionModel(sessionId: UUID): Any? = conversationsItems[sessionId]

    fun checkContextMenuItem(item: String, selectedIds: MutableList<UUID>): Boolean {
        System.err.println("FloaterIMContainer: checkContextMenuItem not yet implemented")
        return false
    }

    fun enableContextMenuItem(item: String, selectedIds: MutableList<UUID>): Boolean {
        System.err.println("FloaterIMContainer: enableContextMenuItem not yet implemented")
        return false
    }

    fun doToParticipants(item: String, selectedIds: MutableList<UUID>) {
        System.err.println("FloaterIMContainer: doToParticipants not yet implemented")
    }

    fun assignResizeLimits() {
        System.err.println("FloaterIMContainer: assignResizeLimits not yet implemented")
    }

    fun closeFloater(appQuitting: Boolean = false) {
        System.err.println("FloaterIMContainer: closeFloater not yet implemented")
    }

    fun closeAllConversations(appQuitting: Boolean) {
        System.err.println("FloaterIMContainer: closeAllConversations not yet implemented")
    }

    fun closeSelectedConversations(ids: List<UUID>) {
        System.err.println("FloaterIMContainer: closeSelectedConversations not yet implemented")
    }

    fun isFrontmost(): Boolean {
        System.err.println("FloaterIMContainer: isFrontmost not yet implemented")
        return false
    }

    fun removeConversationListItem(id: UUID, changeFocus: Boolean = true): Boolean {
        conversationsItems.remove(id)
        conversationsWidgets.remove(id)
        return changeFocus
    }

    fun addConversationListItem(id: UUID, isWidgetSelected: Boolean = false): Any? {
        System.err.println("FloaterIMContainer: addConversationListItem not yet implemented")
        return null
    }

    fun setTimeNow(sessionId: UUID, participantId: UUID) {
        System.err.println("FloaterIMContainer: setTimeNow not yet implemented")
    }

    fun setNearbyDistances() {
        System.err.println("FloaterIMContainer: setNearbyDistances not yet implemented")
    }

    fun reSelectConversation() {
        System.err.println("FloaterIMContainer: reSelectConversation not yet implemented")
    }

    fun updateSpeakBtnState() {
        System.err.println("FloaterIMContainer: updateSpeakBtnState not yet implemented")
    }

    fun flashConversationItemWidget(sessionId: UUID, isFlashing: Boolean, alternateColor: Boolean = false) {
        System.err.println("FloaterIMContainer: flashConversationItemWidget not yet implemented")
    }

    fun highlightConversationItemWidget(sessionId: UUID, isHighlighted: Boolean) {
        System.err.println("FloaterIMContainer: highlightConversationItemWidget not yet implemented")
    }

    fun getConversationListItemSize(): Int = conversationsWidgets.size

    fun getDetachedConversationFloaters(floaters: MutableList<Any>) {
        System.err.println("FloaterIMContainer: getDetachedConversationFloaters not yet implemented")
    }

    private fun onNewMessageReceived(data: Map<String, Any>) {
        val sessionId = data["session_id"] as? UUID ?: return
        System.err.println("FloaterIMContainer: onNewMessageReceived not yet implemented")
    }

    private fun onExpandCollapseButtonClicked() {
        System.err.println("FloaterIMContainer: onExpandCollapseButtonClicked not yet implemented")
    }

    private fun onStubCollapseButtonClicked() {
        System.err.println("FloaterIMContainer: onStubCollapseButtonClicked not yet implemented")
    }

    private fun processParticipantsStyleUpdate() {
        System.err.println("FloaterIMContainer: processParticipantsStyleUpdate not yet implemented")
    }

    private fun onSpeakButtonPressed() {
        System.err.println("FloaterIMContainer: onSpeakButtonPressed not yet implemented")
    }

    private fun onSpeakButtonReleased() {
        System.err.println("FloaterIMContainer: onSpeakButtonReleased not yet implemented")
    }

    private fun onClickCloseBtn(appQuitting: Boolean = false) {
        System.err.println("FloaterIMContainer: onClickCloseBtn not yet implemented")
    }

    private fun collapseConversationsPane(collapse: Boolean, saveIsAllowed: Boolean = true) {
        System.err.println("FloaterIMContainer: collapseConversationsPane not yet implemented")
    }

    private fun reshapeFloaterAndSetResizeLimits(collapse: Boolean, deltaWidth: Int) {
        System.err.println("FloaterIMContainer: reshapeFloaterAndSetResizeLimits not yet implemented")
    }

    private fun onAddButtonClicked() {
        System.err.println("FloaterIMContainer: onAddButtonClicked not yet implemented")
    }

    private fun onAvatarPicked(ids: List<UUID>) {
        System.err.println("FloaterIMContainer: onAvatarPicked not yet implemented")
    }

    private fun isActionChecked(userdata: Any?): Boolean {
        System.err.println("FloaterIMContainer: isActionChecked not yet implemented")
        return false
    }

    private fun onCustomAction(userdata: Any?) {
        System.err.println("FloaterIMContainer: onCustomAction not yet implemented")
    }

    private fun setSortOrderSessions(order: Int) {
        System.err.println("FloaterIMContainer: setSortOrderSessions not yet implemented")
    }

    private fun setSortOrderParticipants(order: Int) {
        System.err.println("FloaterIMContainer: setSortOrderParticipants not yet implemented")
    }

    private fun setSortOrder(order: Any) {
        System.err.println("FloaterIMContainer: setSortOrder not yet implemented")
    }

    private fun getSelectedUUIDs(selectedUuids: MutableList<UUID>, participantUuids: Boolean = true) {
        System.err.println("FloaterIMContainer: getSelectedUUIDs not yet implemented")
    }

    private fun doToSelected(userdata: Any?) {
        System.err.println("FloaterIMContainer: doToSelected not yet implemented")
    }

    private fun doToSelectedGroup(userdata: Any?) {
        System.err.println("FloaterIMContainer: doToSelectedGroup not yet implemented")
    }

    private fun doToSelectedConversation(command: String, selectedIds: MutableList<UUID>) {
        System.err.println("FloaterIMContainer: doToSelectedConversation not yet implemented")
    }

    private fun enableModerateContextMenuItem(userdata: String, isSelf: Boolean = false): Boolean {
        System.err.println("FloaterIMContainer: enableModerateContextMenuItem not yet implemented")
        return false
    }

    private fun isGroupModerator(): Boolean {
        System.err.println("FloaterIMContainer: isGroupModerator not yet implemented")
        return false
    }

    private fun haveAbilityToBan(): Boolean {
        System.err.println("FloaterIMContainer: haveAbilityToBan not yet implemented")
        return false
    }

    private fun canBanSelectedMember(participantUuid: UUID): Boolean {
        System.err.println("FloaterIMContainer: canBanSelectedMember not yet implemented")
        return false
    }

    private fun isMuted(avatarId: UUID): Boolean {
        System.err.println("FloaterIMContainer: isMuted not yet implemented")
        return false
    }

    private fun moderateVoice(command: String, userId: UUID) {
        System.err.println("FloaterIMContainer: moderateVoice not yet implemented")
    }

    private fun moderateVoiceAllParticipants(unmute: Boolean) {
        System.err.println("FloaterIMContainer: moderateVoiceAllParticipants not yet implemented")
    }

    private fun moderateVoiceParticipant(avatarId: UUID, unmute: Boolean) {
        System.err.println("FloaterIMContainer: moderateVoiceParticipant not yet implemented")
    }

    private fun toggleAllowTextChat(participantUuid: UUID) {
        System.err.println("FloaterIMContainer: toggleAllowTextChat not yet implemented")
    }

    private fun banSelectedMember(participantUuid: UUID) {
        System.err.println("FloaterIMContainer: banSelectedMember not yet implemented")
    }

    private fun openNearbyChat() {
        System.err.println("FloaterIMContainer: openNearbyChat not yet implemented")
    }

    private fun isParticipantListExpanded(): Boolean {
        System.err.println("FloaterIMContainer: isParticipantListExpanded not yet implemented")
        return false
    }

    private fun idleUpdate() {
        System.err.println("FloaterIMContainer: idleUpdate not yet implemented")
    }

    private fun idleProcessEvents() {
        System.err.println("FloaterIMContainer: idleProcessEvents not yet implemented")
    }

    private fun onConversationModelEvent(event: Map<String, Any>): Boolean {
        handleConversationModelEvent(event)
        return false
    }

    private fun handleConversationModelEvent(event: Map<String, Any>) {
        System.err.println("FloaterIMContainer: handleConversationModelEvent not yet implemented")
    }

    companion object {
        fun findInstance(): FloaterIMContainer? {
            System.err.println("FloaterIMContainer: findInstance not yet implemented")
            return null
        }

        fun getInstance(): FloaterIMContainer {
            System.err.println("FloaterIMContainer: getInstance not yet implemented")
            throw UnsupportedOperationException("FloaterIMContainer: getInstance not yet implemented")
        }

        fun onCurrentChannelChanged(sessionId: UUID) {
            if (sessionId != null) {
                getInstance().showConversation(sessionId)
            }
        }

        fun isConversationLoggingAllowed(): Boolean {
            System.err.println("FloaterIMContainer: isConversationLoggingAllowed not yet implemented")
            return false
        }

        fun idle(userData: Any?) {
            (userData as? FloaterIMContainer)?.idleUpdate()
        }

        private fun confirmMuteAllCallback(notification: Any?, response: Any?) {
            System.err.println("FloaterIMContainer: confirmMuteAllCallback not yet implemented")
        }
    }
}
