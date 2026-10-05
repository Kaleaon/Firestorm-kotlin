package com.firestorm.newview

import java.util.UUID

// Opaque stand-ins for C++ viewer types that have no JVM equivalent yet.
interface LLSpeakerMgr {
    fun getSessionID(): UUID
    fun getSpeakerList(outList: MutableList<LLSpeaker>, includeInactive: Boolean)
    fun findSpeaker(id: UUID): LLSpeaker?
    fun update(forceUpdate: Boolean)
    fun addListener(listener: SpeakerEventListener, eventName: String)
}

interface LLAvatarList {
    fun getIDs(): MutableList<UUID>
    fun contains(id: UUID): Boolean
    fun setDirty()
    fun size(): Int
    fun setNoItemsCommentText(text: String)
    fun setSessionID(id: UUID)
    fun setSpeakingIndicatorsVisible(visible: Boolean)
    fun setContextMenu(menu: FSParticipantList.FSParticipantListMenu?)
    fun setComparator(comparator: AvatarItemComparator?)
    fun sort()
    fun sortByName(agentOnTop: Boolean = false)
    fun setShowIcons(settingName: String)
    fun getName(): String
    fun getItemByValue(id: UUID): Any?
    fun calcScreenRect(): ScreenRect
    fun toggleIcons()
}

interface AvatarItemComparator

data class LLSpeaker(
    val id: UUID,
    val isModerator: Boolean = false,
    val type: SpeakerType = SpeakerType.SPEAKER_AGENT,
    val lastSpokeTime: Double = 0.0,
    val sortIndex: Int = 0,
    val statusTextOnly: Boolean = false,
    val statusMuted: Boolean = false,
    val moderatorMutedVoice: Boolean = false,
    val moderatorMutedText: Boolean = false,
    val inVoiceChannel: Boolean = false,
)

enum class SpeakerType { SPEAKER_AGENT, SPEAKER_OBJECT, SPEAKER_EXTERNAL }

interface SpeakerEventListener {
    fun handleEvent(event: SpeakerEvent): Boolean
}

data class SpeakerEvent(
    val name: String,
    val valueUUID: UUID? = null,
    val valueString: String? = null,
    val valueMap: Map<String, Any>? = null,
    val source: LLSpeaker? = null,
)

data class ScreenRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun pointInRect(x: Int, y: Int) = x in left..right && y in bottom..top
}

class FSParticipantList(
    private val speakerMgr: LLSpeakerMgr,
    private val avatarList: LLAvatarList,
    useContextMenu: Boolean = true,
    private val excludeAgent: Boolean = true,
    canToggleIcons: Boolean = true,
) {
    enum class EConversationType {
        CONV_UNKNOWN,
        CONV_PARTICIPANT,
        CONV_SESSION_NEARBY,
        CONV_SESSION_1_ON_1,
        CONV_SESSION_AD_HOC,
        CONV_SESSION_GROUP,
        CONV_SESSION_UNKNOWN,
    }

    enum class EParticipantSortOrder { E_SORT_BY_NAME, E_SORT_BY_RECENT_SPEAKERS }

    var validateSpeakerCallback: ((UUID) -> Boolean)? = null
    var insertMentionCallback: ((UUID) -> Unit)? = null

    private val moderatorList: MutableSet<UUID> = mutableSetOf()
    private val moderatorToRemoveList: MutableSet<UUID> = mutableSetOf()

    private var avatarListDoubleClickConnection: (() -> Unit)? = null
    private var avatarListRefreshConnection: (() -> Unit)? = null
    private var avatarListReturnConnection: (() -> Unit)? = null
    private var avatarListToggleIconsConnection: (() -> Unit)? = null

    private var sortByRecentSpeakers: LLAvatarItemRecentSpeakerComparator? = null

    var convType: EConversationType = EConversationType.CONV_UNKNOWN
        private set

    var participantListMenu: FSParticipantListMenu? = null
        private set

    init {
        val speakerAddListener = SpeakerAddListener(this)
        val speakerRemoveListener = SpeakerRemoveListener(this)
        val speakerClearListener = SpeakerClearListener(this)
        val speakerModeratorListener = SpeakerModeratorUpdateListener(this)
        val speakerMuteListener = SpeakerMuteListener(this)

        speakerMgr.addListener(speakerAddListener, "add")
        speakerMgr.addListener(speakerRemoveListener, "remove")
        speakerMgr.addListener(speakerClearListener, "clear")
        speakerMgr.addListener(speakerModeratorListener, "update_moderator")

        System.err.println("APR: avatarList.setNoItemsCommentText(LLTrans.getString(\"LoadingData\"))")
        avatarList.setSessionID(speakerMgr.getSessionID())

        avatarListDoubleClickConnection = { onAvatarListDoubleClicked(avatarList) }
        System.err.println("APR: wire avatarList.setItemDoubleClickCallback -> onAvatarListDoubleClicked")
        System.err.println("APR: wire avatarList.setRefreshCompleteCallback -> onAvatarListRefreshed")
        System.err.println("APR: wire avatarList.setReturnCallback -> onAvatarListDoubleClicked(avatarList)")

        if (useContextMenu) {
            participantListMenu = FSParticipantListMenu(this)
            avatarList.setContextMenu(participantListMenu)
        } else {
            avatarList.setContextMenu(null)
        }

        if (useContextMenu && canToggleIcons) {
            avatarList.setShowIcons("ParticipantListShowIcons")
            avatarListToggleIconsConnection = { avatarList.toggleIcons() }
            System.err.println("APR: subscribe avatarListToggleIconsConnection to saved setting ParticipantListShowIcons signal")
        }

        val speakerList = mutableListOf<LLSpeaker>()
        speakerMgr.getSpeakerList(speakerList, true)
        for (speaker in speakerList) {
            addAvatarIDExceptAgent(speaker.id)
            if (speaker.isModerator) {
                moderatorList.add(speaker.id)
            } else {
                moderatorToRemoveList.add(speaker.id)
            }
        }

        System.err.println("APR: resolve convType from LLIMModel session: CONV_SESSION_1_ON_1 / CONV_SESSION_AD_HOC / CONV_SESSION_GROUP; or CONV_SESSION_NEARBY if no IM session found")

        sort()
    }

    fun close() {
        avatarListDoubleClickConnection = null
        avatarListRefreshConnection = null
        avatarListReturnConnection = null
        avatarListToggleIconsConnection = null

        // Hide menu before destruction to prevent enable/check handlers from firing on a stale list.
        System.err.println("APR: if participantListMenu != null && !LLApp.isExiting(), call participantListMenu.hide()")
        participantListMenu = null

        avatarList.setContextMenu(null)
        avatarList.setComparator(null)
    }

    fun setSpeakingIndicatorsVisible(visible: Boolean) {
        avatarList.setSpeakingIndicatorsVisible(visible)
    }

    fun addAvatarIDExceptAgent(avatarId: UUID) {
        System.err.println("APR: guard excludeAgent && avatarId == gAgent.getID(); guard avatarList.contains(avatarId)")
        System.err.println("APR: check LLVoiceClient.isParticipantAvatar(avatarId); if true add to avatarList.getIDs() and setDirty()")
        System.err.println("APR: adjustParticipant(avatarId)")
    }

    fun setSortOrder(order: EParticipantSortOrder = EParticipantSortOrder.E_SORT_BY_NAME) {
        System.err.println("APR: read saved setting SpeakerParticipantDefaultOrder; if changed, persist and call sort()")
    }

    fun getSortOrder(): EParticipantSortOrder {
        System.err.println("APR: return EParticipantSortOrder from saved setting SpeakerParticipantDefaultOrder")
        return EParticipantSortOrder.E_SORT_BY_NAME
    }

    fun update() {
        speakerMgr.update(true)
        if (getSortOrder() == EParticipantSortOrder.E_SORT_BY_RECENT_SPEAKERS && !isHovered()) {
            sort()
        }
    }

    fun getType(): EConversationType = convType

    fun getAvatarIds(): MutableList<UUID> = avatarList.getIDs().toMutableList()

    protected fun onAddItemEvent(event: SpeakerEvent): Boolean {
        val uuid = event.valueUUID ?: return true
        if (validateSpeakerCallback?.invoke(uuid) == false) return true
        addAvatarIDExceptAgent(uuid)
        sort()
        return true
    }

    protected fun onRemoveItemEvent(event: SpeakerEvent): Boolean {
        val uuid = event.valueUUID ?: return true
        val ids = avatarList.getIDs()
        if (ids.remove(uuid)) {
            avatarList.setDirty()
        }
        return true
    }

    protected fun onClearListEvent(event: SpeakerEvent): Boolean {
        avatarList.getIDs().clear()
        avatarList.setDirty()
        return true
    }

    protected fun onModeratorUpdateEvent(event: SpeakerEvent): Boolean {
        val data = event.valueMap ?: return true
        val id = data["id"] as? UUID ?: return true
        val isModerator = data["is_moderator"] as? Boolean ?: return true
        if (isModerator) {
            moderatorList.add(id)
        } else {
            if (moderatorList.remove(id)) {
                moderatorToRemoveList.add(id)
            }
        }
        onAvatarListRefreshed()
        return true
    }

    protected fun onSpeakerMuteEvent(event: SpeakerEvent): Boolean {
        val speaker = event.source ?: return false
        if (event.valueString == "voice") {
            updateSpeakerIndicator(avatarList, speaker.id, speaker.moderatorMutedVoice)
        }
        return true
    }

    protected fun sort() {
        when (getSortOrder()) {
            EParticipantSortOrder.E_SORT_BY_NAME -> {
                // When agent is excluded there is no need to pin agent at the top.
                avatarList.sortByName(agentOnTop = !excludeAgent)
            }
            EParticipantSortOrder.E_SORT_BY_RECENT_SPEAKERS -> {
                if (sortByRecentSpeakers == null) {
                    sortByRecentSpeakers = LLAvatarItemRecentSpeakerComparator(this)
                }
                avatarList.setComparator(sortByRecentSpeakers)
                avatarList.sort()
            }
        }
    }

    private fun onAvatarListDoubleClicked(ctrl: Any?) {
        System.err.println("APR: cast ctrl to LLAvatarListItem, get avatarId, guard null / self; LLAvatarActions.startIM(avatarId)")
    }

    private fun onAvatarListRefreshed() {
        System.err.println("APR: strip moderator indicator label from mModeratorToRemoveList items, clear list; append indicator to mModeratorList items; update voice mute state via updateSpeakerIndicator for STATUS_TEXT_ONLY speakers")
    }

    private fun adjustParticipant(speakerId: UUID) {
        System.err.println("APR: speakerMgr.findSpeaker(speakerId)?.addListener(mSpeakerMuteListener)")
    }

    private fun isHovered(): Boolean {
        System.err.println("APR: LLUI.getMousePositionScreen() and compare with avatarList.calcScreenRect()")
        return false
    }

    private fun updateSpeakerIndicator(list: LLAvatarList, avatarId: UUID, isMuted: Boolean) {
        System.err.println("APR: find avatar list item by value, get speaking_indicator child, call setIsModeratorMuted(isMuted)")
    }

    abstract inner class BaseSpeakerListener(protected val parent: FSParticipantList) : SpeakerEventListener

    inner class SpeakerAddListener(parent: FSParticipantList) : BaseSpeakerListener(parent) {
        override fun handleEvent(event: SpeakerEvent): Boolean {
            val speakerId = event.valueUUID ?: return false
            val speaker = parent.speakerMgr.findSpeaker(speakerId)
            if (speaker == null || speaker.type == SpeakerType.SPEAKER_OBJECT) return false
            return parent.onAddItemEvent(event)
        }
    }

    inner class SpeakerRemoveListener(parent: FSParticipantList) : BaseSpeakerListener(parent) {
        override fun handleEvent(event: SpeakerEvent) = parent.onRemoveItemEvent(event)
    }

    inner class SpeakerClearListener(parent: FSParticipantList) : BaseSpeakerListener(parent) {
        override fun handleEvent(event: SpeakerEvent) = parent.onClearListEvent(event)
    }

    inner class SpeakerModeratorUpdateListener(parent: FSParticipantList) : BaseSpeakerListener(parent) {
        override fun handleEvent(event: SpeakerEvent) = parent.onModeratorUpdateEvent(event)
    }

    inner class SpeakerMuteListener(parent: FSParticipantList) : BaseSpeakerListener(parent) {
        override fun handleEvent(event: SpeakerEvent) = parent.onSpeakerMuteEvent(event)
    }

    inner class FSParticipantListMenu(protected val parent: FSParticipantList) {
        private var selectedUUIDs: MutableList<UUID> = mutableListOf()

        fun show(spawningView: Any?, uuids: MutableList<UUID>, x: Int, y: Int) {
            if (uuids.isEmpty()) return
            selectedUUIDs = uuids.toMutableList()
            System.err.println("APR: call super LLListContextMenu.show(spawningView, uuids, x, y)")
            val speakerId = selectedUUIDs.first()
            val muted = isMuted(speakerId)
            System.err.println("APR: toggle visibility of ModerateVoiceMuteSelected / ModerateVoiceUnMuteSelected based on muted")
        }

        fun createMenu(): Any {
            System.err.println("APR: register all action callbacks (sort, allowTextChat, toggleMute*, moderateVoice, avatar actions, ban, mention); load menu_participant_list.xml; configure item visibility based on list size, group moderator status, icons toggle, ban ability")
            return Unit
        }

        fun enableContextMenuItem(item: String): Boolean {
            System.err.println("APR: implement per-item enable logic (can_mute_text, can_block, can_share, can_im, can_pay, can_add, can_call, can_eject, can_zoom_in, can_ban_member)")
            return false
        }

        fun enableModerateContextMenuItem(item: String): Boolean {
            if (!isGroupModerator()) return false
            val speakerId = selectedUUIDs.firstOrNull() ?: return false
            val speaker = parent.speakerMgr.findSpeaker(speakerId)
            val speakerInVoice = speaker?.inVoiceChannel == true
            if (item == "can_moderate_voice") return speakerInVoice
            System.err.println("APR: guard non-avatar (Avaline) callers")
            @Suppress("UNREACHABLE_CODE")
            return true
        }

        fun checkContextMenuItem(item: String): Boolean {
            val id = selectedUUIDs.firstOrNull() ?: return false
            return when (item) {
                "is_muted" -> {
                    System.err.println("APR: LLMuteList.isMuted(id, flagTextChat)")
                    false
                }
                "is_allowed_text_chat" -> {
                    val speaker = parent.speakerMgr.findSpeaker(id)
                    speaker?.moderatorMutedText?.let { !it } ?: false
                }
                "is_blocked" -> {
                    System.err.println("APR: LLMuteList.isMuted(id, flagVoiceChat)")
                    false
                }
                "is_sorted_by_name" -> parent.getSortOrder() == EParticipantSortOrder.E_SORT_BY_NAME
                "is_sorted_by_recent_speakers" -> parent.getSortOrder() == EParticipantSortOrder.E_SORT_BY_RECENT_SPEAKERS
                else -> false
            }
        }

        fun hide() {
            System.err.println("APR: hide context menu widget")
        }

        private fun sortParticipantList(param: String) {
            when (param) {
                "sort_by_name" -> parent.setSortOrder(EParticipantSortOrder.E_SORT_BY_NAME)
                "sort_by_recent_speakers" -> parent.setSortOrder(EParticipantSortOrder.E_SORT_BY_RECENT_SPEAKERS)
            }
        }

        private fun allowTextChat(allow: Boolean) {
            val speakerId = selectedUUIDs.firstOrNull() ?: return
            System.err.println("APR: cast speakerMgr to LLIMSpeakerMgr and call allowTextChat(speakerId, allow)")
        }

        private fun toggleMute(flags: Int) {
            val speakerId = selectedUUIDs.firstOrNull() ?: return
            System.err.println("APR: LLMuteList.isMuted(speakerId, flags); get name from LLAvatarNameCache; determine mute type from speaker type; add or remove LLMute")
        }

        private fun toggleMuteText() = toggleMute(0.also { System.err.println("APR: LLMute.flagTextChat") })
        private fun toggleMuteVoice() = toggleMute(0.also { System.err.println("APR: LLMute.flagVoiceChat") })

        private fun isGroupModerator(): Boolean {
            System.err.println("APR: gAgent.isInGroup(speakerMgr.getSessionID()); find agent speaker; return speaker.isModerator")
            return false
        }

        private fun hasAbilityToBan(): Boolean {
            System.err.println("APR: gAgent.isInGroup(groupUuid) && gAgent.hasPowerInGroup(groupUuid, GP_GROUP_BAN_ACCESS)")
            return false
        }

        private fun canBanSelectedMember(participantUuid: UUID): Boolean {
            System.err.println("APR: guard self-ban, pending ban request, owner role check, and required powers GP_ROLE_REMOVE_MEMBER + GP_GROUP_BAN_ACCESS")
            return false
        }

        private fun banSelectedMember(participantUuid: UUID) {
            System.err.println("APR: LLGroupMgr.getGroupData(groupUuid).banMemberById(participantUuid)")
        }

        private fun isMuted(avatarId: UUID): Boolean {
            val speaker = parent.speakerMgr.findSpeaker(avatarId) ?: return true
            return speaker.statusMuted
        }

        private fun moderateVoice(param: String) {
            System.err.println("APR: guard gAgent.getRegion() != null")
            System.err.println("APR: if param == 'selected' -> moderateVoiceParticipant(selectedId, isMuted(selectedId)); else moderateVoiceAllParticipants(param == 'unmute_all')")
        }

        private fun moderateVoiceParticipant(avatarId: UUID, unmute: Boolean) {
            System.err.println("APR: cast speakerMgr to LLIMSpeakerMgr and call moderateVoiceParticipant(avatarId, unmute)")
        }

        private fun moderateVoiceAllParticipants(unmute: Boolean) {
            System.err.println("APR: if !unmute show ConfirmMuteAll notification with session_id payload; else cast to LLIMSpeakerMgr and call moderateVoiceAllParticipants(unmute)")
        }

        private fun handleAddToContactSet() {
            System.err.println("APR: LLAvatarActions.addToContactSet(selectedUUIDs)")
        }

        private fun copyURLToClipboard(avatarId: UUID) {
            System.err.println("APR: LLUrlAction.copyURLToClipboard(\"secondlife:///app/agent/$avatarId/mention\")")
        }

        private fun insertMentionAtCursor(avatarId: UUID) {
            parent.insertMentionCallback?.invoke(avatarId)
        }
    }

    inner class LLAvatarItemRecentSpeakerComparator(
        private val parent: FSParticipantList,
    ) : AvatarItemComparator {
        fun doCompare(avatarId1: UUID, avatarId2: UUID): Boolean {
            val lhs = parent.speakerMgr.findSpeaker(avatarId1)
            val rhs = parent.speakerMgr.findSpeaker(avatarId2)
            return when {
                lhs != null && rhs != null -> when {
                    lhs.lastSpokeTime != rhs.lastSpokeTime -> lhs.lastSpokeTime > rhs.lastSpokeTime
                    lhs.sortIndex != rhs.sortIndex -> lhs.sortIndex < rhs.sortIndex
                    else -> false.also { System.err.println("APR: fallback to LLAvatarItemNameComparator.doCompare") }
                }
                lhs != null -> true
                rhs != null -> false
                else -> false.also { System.err.println("APR: fallback to LLAvatarItemNameComparator.doCompare") }
            }
        }
    }
}
