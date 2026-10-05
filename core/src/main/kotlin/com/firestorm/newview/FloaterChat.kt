package com.firestorm.newview

import java.util.UUID

class FloaterChatMentionPicker(key: Any?) : Floater(key) {

    private var avatarList: AvatarListStub? = null

    init {
        setFocusStealsFrontmost(false)
        setBackgroundVisible(false)
        setAutoFocus(false)
    }

    override fun postBuild(): Boolean {
        avatarList = getChild("avatar_list")
        avatarList?.apply {
            setShowCompleteName(show = true, abbreviated = true)
            setFocusOnItemClicked(false)
            setItemClickedCallback { id -> selectResident(id) }
            setRefreshCompleteCallback {
                if (numSelected() == 0) selectFirstItem()
            }
        }
        return super.postBuild()
    }

    private fun onOpen(key: Map<String, Any?>) {
        buildAvatarList()
        avatarList?.setNameFilter(key["av_name"] as? String ?: "")
        adjustToFitScreen()
    }

    private fun buildAvatarList() {
        val ids = getParticipantIds()
        avatarList?.setIds(ids)
        updateAvatarList(ids)
        avatarList?.setDirty()
    }

    private fun selectResident(id: UUID) {
        if (id == UUID(0L, 0L)) return
        setValue("secondlife:///app/agent/${id}/mention ")
        onCommit()
        ChatMentionHelper.hideHelper()
    }

    private fun onClose(appQuitting: Boolean) {
        if (!appQuitting) ChatMentionHelper.hideHelper()
    }

    fun handleKey(key: KeyCode, mask: KeyMask): Boolean {
        if (mask == KeyMask.NONE) {
            when (key) {
                KeyCode.UP, KeyCode.DOWN -> return avatarList?.handleKey(key, mask) ?: false
                KeyCode.RETURN, KeyCode.TAB -> {
                    selectResident(avatarList?.getSelectedId() ?: return true)
                    return true
                }
                KeyCode.ESCAPE -> { ChatMentionHelper.hideHelper(); return true }
                KeyCode.LEFT, KeyCode.RIGHT -> return true
                else -> {}
            }
        }
        return super.handleKey(key, mask)
    }

    open fun handleKey(key: KeyCode, mask: KeyMask): Boolean = false

    fun goneFromFront() {
        ChatMentionHelper.hideHelper()
    }

    private fun setValue(v: String) {}
    private fun onCommit() {}
    private fun adjustToFitScreen() { // no-op }
    private fun setFocusStealsFrontmost(v: Boolean) {}
    private fun setBackgroundVisible(v: Boolean) { // no-op }
    private fun setAutoFocus(v: Boolean) {}

    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = null

    companion object {
        private var sessionId: UUID = UUID(0L, 0L)
        private var participantSource: ChatParticipantsSource? = null

        fun getParticipantIds(): List<UUID> {
            val source = participantSource ?: run {
                System.err.println("Participant list is missing")
                return emptyList()
            }
            return source.getSessionParticipants()
        }

        fun updateSessionID(id: UUID) {
            sessionId = id
        }

        fun updateAvatarList(avatarIds: List<UUID>) {
            val names = mutableListOf<String>()
            for (id in avatarIds) {
                val name = AvatarNameCache.get(id)
                names += name.accountName.lowercase()
                names += name.displayName.lowercase()
            }
            ChatMentionHelper.updateAvatarList(names)
        }

        fun updateParticipantSource(source: ChatParticipantsSource?) {
            participantSource = source
            if (source == null) {
                System.err.println("Participant list is missing")
                return
            }
            updateAvatarList(source.getSessionParticipants())
        }

        fun removeParticipantSource(source: ChatParticipantsSource) {
            if (participantSource === source) participantSource = null
        }
    }
}

class FloaterChatVoiceVolume(key: Any?) : Floater(key) {

    fun onOpen(key: Any?) {
        repositionInspector(key)
    }

    fun onDestroy() {
        TransientFloaterManager.removeControlView(this)
    }

    private fun repositionInspector(key: Any?) {
        // no-op
    }
}

object TransientFloaterManager {
    fun removeControlView(floater: Floater) {
        System.err.println("TransientFloaterManager: removeControlView not yet implemented")
    }
}

interface ChatParticipantsSource {
    fun getSessionParticipants(): List<UUID>
}

object ChatMentionHelper {
    fun hideHelper() { System.err.println("ChatMentionHelper: hideHelper not yet implemented") }
    fun updateAvatarList(names: List<String>) { System.err.println("ChatMentionHelper: updateAvatarList not yet implemented") }
}

object AvatarNameCache {
    data class AvatarName(val accountName: String, val displayName: String)
    fun get(id: UUID): AvatarName { System.err.println("AvatarNameCache: get not yet implemented"); return AvatarName("", "") }
}

class AvatarListStub {
    fun setShowCompleteName(show: Boolean, abbreviated: Boolean) {}
    fun setFocusOnItemClicked(focused: Boolean) {}
    fun setItemClickedCallback(cb: (UUID) -> Unit) {}
    fun setRefreshCompleteCallback(cb: () -> Unit) {}
    fun setNameFilter(filter: String) {}
    fun setIds(ids: List<UUID>) {}
    fun setDirty() {}
    fun numSelected(): Int = 0
    fun selectFirstItem() {}
    fun getSelectedId(): UUID = UUID(0L, 0L)
    fun handleKey(key: KeyCode, mask: KeyMask): Boolean = false
}

enum class KeyCode { UP, DOWN, LEFT, RIGHT, RETURN, TAB, ESCAPE, OTHER }
enum class KeyMask { NONE, CTRL, ALT, SHIFT }
