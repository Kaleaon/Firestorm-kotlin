package com.firestorm.newview

import java.util.UUID

class ConversationLogListItem(val conversation: Conversation?) {

    var isSelected: Boolean = false
    private var conversationName: TextBox? = null
    private var conversationDate: TextBox? = null
    private var hoveredIconVisible: Boolean = false
    private var selectedIconVisible: Boolean = false
    private var unreadImsIconVisible: Boolean = false
    private var imFloaterShownCallbacks: MutableList<(UUID) -> Unit> = mutableListOf()

    init {
        val sessionId = conversation?.sessionId
        val floater = sessionId?.let { IMFloater.findInstance(it) }
        val imsAreRead = floater != null && IMFloater.isVisible(floater) && floater.hasFocus()

        if (conversation?.hasOfflineMessages == true && !imsAreRead) {
            val callback: (UUID) -> Unit = { id -> onIMFloaterShown(id) }
            imFloaterShownCallbacks.add(callback)
            IMFloater.addImFloaterShownCallback(callback)
        }
    }

    fun dispose() {
        imFloaterShownCallbacks.forEach { IMFloater.removeImFloaterShownCallback(it) }
        imFloaterShownCallbacks.clear()
    }

    fun postBuild(): Boolean {
        initIcons()
        conversationName = TextBox()
        conversationName?.value = conversation?.conversationName ?: ""
        conversationDate = TextBox()
        conversationDate?.value = conversation?.timestamp ?: ""
        return true
    }

    private fun initIcons() {
        when (conversation?.conversationType) {
            SessionType.P2P,
            SessionType.ADHOC -> {
                TODO("GPU: show avatar icon for ${conversation.participantId}")
            }
            SessionType.GROUP -> {
                TODO("GPU: show group icon for ${conversation.sessionId}")
            }
            else -> Unit
        }
        unreadImsIconVisible = conversation?.hasOfflineMessages == true
    }

    fun updateTimestamp() {
        conversationDate?.value = conversation?.timestamp ?: ""
    }

    fun updateName() {
        conversationName?.value = conversation?.conversationName ?: ""
    }

    fun updateOfflineIMs() {
        unreadImsIconVisible = conversation?.hasOfflineMessages == true
    }

    fun onMouseEnter(x: Int, y: Int) {
        hoveredIconVisible = true
    }

    fun onMouseLeave(x: Int, y: Int) {
        hoveredIconVisible = false
    }

    fun setValue(selected: Boolean) {
        selectedIconVisible = selected
    }

    fun onIMFloaterShown(sessionId: UUID) {
        if (conversation?.sessionId == sessionId) {
            unreadImsIconVisible = false
        }
    }

    fun onRemoveBtnClicked() {
        conversation?.let { ConversationLog.instance.removeConversation(it) }
    }

    fun highlightNameDate(highlightedText: String) {
        TODO("GPU: highlight '$highlightedText' in name and date text boxes")
    }

    fun onDoubleClick() {
        when (conversation?.conversationType) {
            SessionType.P2P -> conversation.participantId.let { AvatarActions.startIM(it) }
            SessionType.GROUP -> conversation.sessionId.let { GroupActions.startIM(it) }
            else -> Unit
        }
    }
}

class TextBox {
    var value: String = ""
}

class IMFloater {
    val hasFocus: Boolean get() = TODO("GPU: check focus state")

    companion object {
        fun findInstance(sessionId: UUID): IMFloater? = TODO("APR: use JVM equivalent")
        fun isVisible(floater: IMFloater?): Boolean = TODO("APR: use JVM equivalent")
        fun addImFloaterShownCallback(callback: (UUID) -> Unit): Unit = TODO("APR: use JVM equivalent")
        fun removeImFloaterShownCallback(callback: (UUID) -> Unit): Unit = TODO("APR: use JVM equivalent")
    }
}
