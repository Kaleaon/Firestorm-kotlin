package com.firestorm.newview

import java.util.UUID

class LLFloaterChatMentionPicker(key: LLSD) : LLFloater(key) {

    private var mAvatarList: LLAvatarList? = null

    init {
        // Hover above dependent without stealing focus from it
        setFocusStealsFrontmost(false)
        setBackgroundVisible(false)
        setAutoFocus(false)
    }

    override fun postBuild(): Boolean {
        mAvatarList = getChild<LLAvatarList>("avatar_list")
        mAvatarList?.setShowCompleteName(true, true)
        mAvatarList?.setFocusOnItemClicked(false)
        mAvatarList?.setItemClickedCallback { ctrl, x, y, mask ->
            val item = ctrl as? LLAvatarListItem
            if (item != null) {
                selectResident(item.getAvatarId())
            }
        }
        mAvatarList?.setRefreshCompleteCallback { ctrl, param ->
            if (mAvatarList?.numSelected() == 0) {
                mAvatarList?.selectFirstItem()
            }
        }
        return super.postBuild()
    }

    override fun onOpen(key: LLSD) {
        buildAvatarList()
        mAvatarList?.setNameFilter(if (key.has("av_name")) key["av_name"].asString() else "")
        gFloaterView.adjustToFitScreen(this, false)
    }

    override fun onClose(appQuitting: Boolean) {
        if (!appQuitting) {
            LLChatMentionHelper.instance().hideHelper()
        }
    }

    override fun goneFromFront() {
        LLChatMentionHelper.instance().hideHelper()
    }

    override fun handleKey(key: Int, mask: Int, calledFromParent: Boolean): Boolean {
        if (mask == MASK_NONE) {
            when (key) {
                KEY_UP, KEY_DOWN -> return mAvatarList?.handleKey(key, mask, calledFromParent) ?: false
                KEY_RETURN, KEY_TAB -> {
                    selectResident(mAvatarList?.getSelectedUUID() ?: UUID(0L, 0L))
                    return true
                }
                KEY_ESCAPE -> {
                    LLChatMentionHelper.instance().hideHelper()
                    return true
                }
                KEY_LEFT, KEY_RIGHT -> return true
            }
        }
        return super.handleKey(key, mask, calledFromParent)
    }

    fun buildAvatarList() {
        val avatarIds = mAvatarList?.getIDs() ?: return
        val participants = getParticipantIds()
        avatarIds.clear()
        avatarIds.addAll(participants)
        updateAvatarList(avatarIds)
        mAvatarList?.setDirty()
    }

    private fun selectResident(id: UUID) {
        if (id == UUID(0L, 0L)) return
        setValue("secondlife:///app/agent/${id}/mention ")
        onCommit()
        LLChatMentionHelper.instance().hideHelper()
    }

    companion object {

        private var sSessionID: UUID = UUID(0L, 0L)

        private var sParticipantSource: FSChatParticipants? = null

        fun getParticipantIds(): MutableList<UUID> {
            val source = sParticipantSource
            if (source == null) {
                return mutableListOf()
            }
            return source.getSessionParticipants()
        }

        fun updateSessionID(sessionId: UUID) {
            // Session ID routing is handled via sParticipantSource for FS communication UI
        }

        fun updateAvatarList(avatarIds: MutableList<UUID>) {
            val avNames = mutableListOf<String>()
            for (id in avatarIds) {
                val avName = LLAvatarName()
                LLAvatarNameCache.get(id, avName)
                avNames.add(avName.getAccountName().toLowerCase())
                avNames.add(avName.getDisplayName().toLowerCase())
            }
            LLChatMentionHelper.instance().updateAvatarList(avNames)
        }

        fun updateParticipantSource(source: FSChatParticipants?) {
            sParticipantSource = source
            if (source == null) return
            val avatarIds = source.getSessionParticipants()
            updateAvatarList(avatarIds)
        }

        fun removeParticipantSource(source: FSChatParticipants?) {
            if (sParticipantSource === source) {
                sParticipantSource = null
            }
        }
    }
}
