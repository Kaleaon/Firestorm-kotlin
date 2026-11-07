package com.firestorm.newview

import java.util.UUID

typealias ConversationItemsMap = MutableMap<UUID, ConversationItem>
typealias ConversationWidgetsMap = MutableMap<UUID, Any>
typealias MenuEntryVec = MutableList<String>

// ─── ConversationItem ────────────────────────────────────────────────────────

abstract class ConversationItem(
    protected var name: String = "",
    val uuid: UUID = UUID(0, 0),
) {
    enum class ConversationType {
        CONV_UNKNOWN,
        CONV_PARTICIPANT,
        CONV_SESSION_NEARBY,
        CONV_SESSION_1_ON_1,
        CONV_SESSION_AD_HOC,
        CONV_SESSION_GROUP,
        CONV_SESSION_UNKNOWN,
    }

    protected var convType: ConversationType = ConversationType.CONV_UNKNOWN
    var needsRefresh: Boolean = true
        protected set
    protected var lastActiveTime: Double = 0.0
    protected var displayModeratorOptions: Boolean = false
    protected var displayGroupBanOptions: Boolean = false

    open val displayName: String get() = name
    open val searchableName: String get() = name

    fun getType(): ConversationType = convType

    open fun getTime(time: DoubleArray): Boolean {
        time[0] = lastActiveTime
        return lastActiveTime > 0.1
    }

    open fun getDistanceToAgent(dist: DoubleArray): Boolean = false

    fun hasSameValue(id: UUID): Boolean = id == uuid

    fun resetRefresh() { needsRefresh = false }

    open fun renameItem(newName: String): Boolean {
        name = newName
        needsRefresh = true
        return true
    }

    fun postEvent(eventType: String, session: ConversationItemSession?, participant: ConversationItemParticipant?) {
        System.err.println("ConversationItem: postEvent not yet implemented")
    }

    fun buildParticipantMenuOptions(items: MenuEntryVec, flags: UInt) {
        val multiSelect = (flags and ITEM_IN_MULTI_SELECTION) != 0u
        if (multiSelect) {
            items += listOf("im", "offer_teleport", "voice_call", "remove_friends")
        } else {
            items += listOf("view_profile", "im", "offer_teleport", "request_teleport")

            if (convType != ConversationType.CONV_SESSION_1_ON_1) {
                items += "voice_call"
            } else {
                val onActiveChannel = false
                items += if (onActiveChannel) "disconnect_from_voice" else "voice_call"
            }

            items += listOf(
                "chat_history", "separator_chat_history",
                "add_friend", "remove_friend",
                "invite_to_group", "separator_invite_to_group",
            )

            val parentIsNearby = (parent as? ConversationItem)?.getType() == ConversationType.CONV_SESSION_NEARBY
            if (parentIsNearby) items += "zoom_in"

            items += listOf("map", "share", "pay", "report_abuse", "block_unblock", "MuteText")

            if (convType != ConversationType.CONV_SESSION_1_ON_1 && displayModeratorOptions) {
                items += listOf(
                    "Moderator Options Separator", "Moderator Options", "AllowTextChat",
                    "moderate_voice_separator",
                    "ModerateVoiceMuteSelected", "ModerateVoiceUnMuteSelected",
                    "ModerateVoiceMute", "ModerateVoiceUnmute",
                )
            }

            if (convType != ConversationType.CONV_SESSION_1_ON_1 && displayGroupBanOptions) {
                items += listOf("Group Ban Separator", "BanMember")
            }
        }
    }

    fun fetchAvatarName(isParticipant: Boolean = true) {
        System.err.println("ConversationItem: fetchAvatarName not yet implemented")
    }

    protected open fun onAvatarNameCache(avName: String) {}

    var parent: Any? = null

    companion object {
        const val ITEM_IN_MULTI_SELECTION: UInt = 0x01u
    }
}

// ─── ConversationItemSession ─────────────────────────────────────────────────

open class ConversationItemSession(
    displayName: String = "",
    uuid: UUID = UUID(0, 0),
) : ConversationItem(displayName, uuid) {

    private val participants: MutableList<ConversationItemParticipant> = mutableListOf()
    var isLoaded: Boolean = false
        private set

    init {
        convType = ConversationType.CONV_SESSION_UNKNOWN
    }

    fun hasChildren(): Boolean = participants.isNotEmpty()

    fun setSessionId(sessionId: UUID) {
        System.err.println("ConversationItemSession: setSessionId not yet implemented")
    }

    fun addParticipant(participant: ConversationItemParticipant) {
        if (participants.none { it == participant }) {
            participants += participant
            participant.parent = this
        }
        isLoaded = true
        needsRefresh = true
        updateName(participant)
        postEvent("add_participant", this, participant)
    }

    fun updateName(participant: ConversationItemParticipant?) {
        val type = convType
        if (type != ConversationType.CONV_SESSION_AD_HOC && type != ConversationType.CONV_SESSION_1_ON_1) return
        if (participants.isEmpty()) return

        val names = mutableListOf<String>()
        for (p in participants) {
            if (p.uuid == agentId()) continue
            val avName: String? = null
            if (avName != null) {
                names += avName
                if (type == ConversationType.CONV_SESSION_1_ON_1) break
            }
        }
        if (names.isNotEmpty()) {
            renameItem(names.joinToString(", "))
            postEvent("update_session", this, null)
        }
    }

    fun removeParticipant(participant: ConversationItemParticipant) {
        participants -= participant
        needsRefresh = true
        updateName(participant)
        postEvent("remove_participant", this, participant)
    }

    fun removeParticipant(participantId: UUID) {
        findParticipant(participantId)?.let { removeParticipant(it) }
    }

    fun clearParticipants() {
        participants.clear()
        isLoaded = false
        needsRefresh = true
    }

    fun clearAndDeparentModels() {
        participants.forEach { it.parent = null }
        participants.clear()
    }

    fun findParticipant(participantId: UUID): ConversationItemParticipant? =
        participants.firstOrNull { it.hasSameValue(participantId) }

    fun setParticipantIsMuted(participantId: UUID, isMuted: Boolean) {
        findParticipant(participantId)?.moderateVoice(isMuted)
    }

    fun setParticipantIsModerator(participantId: UUID, isModerator: Boolean) {
        findParticipant(participantId)?.setIsModerator(isModerator)
    }

    fun setTimeNow(participantId: UUID) {
        lastActiveTime = elapsedSeconds()
        needsRefresh = true
        findParticipant(participantId)?.setTimeNow()
    }

    fun setDistance(participantId: UUID, dist: Double) {
        findParticipant(participantId)?.setDistance(dist)
        needsRefresh = true
    }

    fun buildContextMenu(items: MenuEntryVec, flags: UInt) {
        val multiSelect = (flags and ConversationItem.ITEM_IN_MULTI_SELECTION) != 0u
        if (multiSelect && convType != ConversationType.CONV_SESSION_NEARBY) {
            items += "close_selected_conversations"
        }
        when (convType) {
            ConversationType.CONV_SESSION_1_ON_1 -> {
                items += listOf("close_conversation", "separator_disconnect_from_voice")
                buildParticipantMenuOptions(items, flags)
            }
            ConversationType.CONV_SESSION_GROUP -> {
                items += "close_conversation"
                addVoiceOptions(items)
                items += listOf("chat_history", "separator_chat_history", "group_profile", "activate_group", "leave_group")
            }
            ConversationType.CONV_SESSION_AD_HOC -> {
                items += "close_conversation"
                addVoiceOptions(items)
                items += "chat_history"
            }
            ConversationType.CONV_SESSION_NEARBY -> {
                items += "chat_history"
            }
            else -> {}
        }
    }

    private fun addVoiceOptions(items: MenuEntryVec) {
        val onCurrentChannel = false
        items += if (onCurrentChannel) "disconnect_from_voice" else "open_voice_conversation"
    }

    override fun getTime(time: DoubleArray): Boolean {
        var mostRecent = lastActiveTime
        var hasTime = mostRecent > 0.1
        for (p in participants) {
            val pt = DoubleArray(1)
            if (p.getTime(pt)) {
                hasTime = true
                if (pt[0] > mostRecent) mostRecent = pt[0]
            }
        }
        if (hasTime) time[0] = mostRecent
        return hasTime
    }

    override fun onAvatarNameCache(avName: String) {
        renameItem(avName)
        postEvent("update_session", this, null)
    }

    fun dumpDebugData(dumpChildren: Boolean = false) {
        println("session uuid=$uuid name=$name isLoaded=$isLoaded")
        if (dumpChildren) participants.forEach { it.dumpDebugData() }
    }
}

// ─── ConversationItemParticipant ─────────────────────────────────────────────

class ConversationItemParticipant(
    displayNameInit: String = "",
    uuid: UUID = UUID(0, 0),
) : ConversationItem(displayNameInit, uuid) {

    private var isModeratorMuted: Boolean = false
    private var isModerator: Boolean = false
    private var showModeratorLabel: Boolean = false
    private var cachedDisplayName: String = displayNameInit
    private var distToAgent: Double = -1.0

    init {
        convType = ConversationType.CONV_PARTICIPANT
    }

    override val displayName: String get() = cachedDisplayName

    fun isVoiceMuted(): Boolean {
        val muteListMuted = false
        return isModeratorMuted || muteListMuted
    }

    fun isModeratorMuted(): Boolean = isModeratorMuted
    fun isModerator(): Boolean = isModerator

    fun moderateVoice(muteVoice: Boolean) { isModeratorMuted = muteVoice }
    fun setIsModerator(value: Boolean) { isModerator = value; needsRefresh = true }
    fun setTimeNow() { lastActiveTime = elapsedSeconds(); needsRefresh = true }
    fun setDistance(dist: Double) { distToAgent = dist; needsRefresh = true }

    override fun getDistanceToAgent(dist: DoubleArray): Boolean {
        dist[0] = distToAgent
        return distToAgent >= 0.0
    }

    fun setModeratorOptionsVisible(visible: Boolean) { displayModeratorOptions = visible }
    fun setGroupBanVisible(visible: Boolean) { displayGroupBanOptions = visible }

    fun setDisplayModeratorRole(displayRole: Boolean) {
        if (displayRole != showModeratorLabel) {
            showModeratorLabel = displayRole
            updateName()
        }
    }

    fun updateName() {
        val avName: Pair<String, String>? = null
        if (avName != null) applyAvatarName(avName.first, avName.second)
    }

    override fun onAvatarNameCache(avName: String) {
        System.err.println("ConversationItemParticipant: onAvatarNameCache not yet implemented")
    }

    private fun applyAvatarName(userName: String, displayName: String) {
        name = userName
        cachedDisplayName = if (showModeratorLabel) {
            "$displayName ${moderatorLabel()}"
        } else {
            displayName
        }
        renameItem(cachedDisplayName)

        val parentSession = parent as? ConversationItemSession ?: return
        parentSession.updateName(this)
        postEvent("update_participant", parentSession, this)
    }

    fun buildContextMenu(items: MenuEntryVec, flags: UInt) {
        buildParticipantMenuOptions(items, flags)
    }

    fun getParentSession(): ConversationItemSession? = parent as? ConversationItemSession

    fun dumpDebugData() {
        println("participant uuid=$uuid name=$name displayName=$cachedDisplayName muted=${isVoiceMuted()} moderator=$isModerator")
    }

    private fun moderatorLabel(): String {
        System.err.println("ConversationItemParticipant: moderatorLabel not yet implemented")
        return ""
    }
}

// ─── ConversationFilter ──────────────────────────────────────────────────────

class ConversationFilter {
    enum class SortOrderType(val value: UInt) {
        SO_NAME(0u),
        SO_DATE(1u),
        SO_SESSION_TYPE(2u),
        SO_DISTANCE(3u),
    }

    companion object {
        val SO_DEFAULT: UInt = (SortOrderType.SO_SESSION_TYPE.value shl 16) or SortOrderType.SO_DATE.value
    }

    private val empty: String = ""

    fun check(item: ConversationItem): Boolean = true
    fun checkFolder(folder: ConversationItem): Boolean = true
    fun getEmptyLookupMessage(isEmptyFolder: Boolean = false): String = empty
    fun showAllResults(): Boolean = true
    fun isActive(): Boolean = false
    fun isModified(): Boolean = false
    fun getName(): String = empty
    fun isDefault(): Boolean = true
    fun isNotDefault(): Boolean = false
    fun getCurrentGeneration(): Int = 0
    fun getFirstSuccessGeneration(): Int = 0
    fun getFirstRequiredGeneration(): Int = 0
}

// ─── ConversationSort ────────────────────────────────────────────────────────

class ConversationSort(private var sortOrder: UInt = ConversationFilter.SO_DEFAULT) {

    fun getSortOrderSessions(): UInt = (sortOrder shr 16) and 0xFFFFu
    fun getSortOrderParticipants(): UInt = sortOrder and 0xFFFFu

    fun setSortOrderSessions(session: ConversationFilter.SortOrderType) {
        sortOrder = ((session.value and 0xFFFFu) shl 16) or (sortOrder and 0xFFFFu)
    }

    fun setSortOrderParticipants(participant: ConversationFilter.SortOrderType) {
        sortOrder = (sortOrder and 0xFFFF0000u) or (participant.value and 0xFFFFu)
    }

    fun compare(a: ConversationItem, b: ConversationItem): Boolean {
        val typeA = a.getType()
        val typeB = b.getType()

        val aIsParticipant = typeA == ConversationItem.ConversationType.CONV_PARTICIPANT
        val bIsParticipant = typeB == ConversationItem.ConversationType.CONV_PARTICIPANT

        return when {
            aIsParticipant && bIsParticipant -> compareParticipants(a, b)
            !aIsParticipant && !bIsParticipant -> compareSessions(a, b, typeA, typeB)
            else -> typeA.ordinal > typeB.ordinal
        }
    }

    private fun compareParticipants(a: ConversationItem, b: ConversationItem): Boolean {
        return when (getSortOrderParticipants()) {
            ConversationFilter.SortOrderType.SO_DATE.value -> {
                val ta = DoubleArray(1); val tb = DoubleArray(1)
                val ha = a.getTime(ta); val hb = b.getTime(tb)
                when {
                    ha && hb -> ta[0] > tb[0]
                    ha || hb -> ha
                    else -> compareByName(a, b)
                }
            }
            ConversationFilter.SortOrderType.SO_DISTANCE.value -> {
                val da = DoubleArray(1); val db = DoubleArray(1)
                val ha = a.getDistanceToAgent(da); val hb = b.getDistanceToAgent(db)
                when {
                    ha && hb -> da[0] < db[0]
                    ha || hb -> ha
                    else -> compareByName(a, b)
                }
            }
            else -> compareByName(a, b)
        }
    }

    private fun compareSessions(
        a: ConversationItem, b: ConversationItem,
        typeA: ConversationItem.ConversationType, typeB: ConversationItem.ConversationType,
    ): Boolean {
        return when (getSortOrderSessions()) {
            ConversationFilter.SortOrderType.SO_DATE.value -> {
                val ta = DoubleArray(1); val tb = DoubleArray(1)
                val ha = a.getTime(ta); val hb = b.getTime(tb)
                when {
                    ha && hb -> ta[0] > tb[0]
                    ha || hb -> ha
                    else -> compareByName(a, b)
                }
            }
            else -> {
                val nearbyA = typeA == ConversationItem.ConversationType.CONV_SESSION_NEARBY
                val nearbyB = typeB == ConversationItem.ConversationType.CONV_SESSION_NEARBY
                when {
                    nearbyA || nearbyB -> nearbyB
                    getSortOrderSessions() == ConversationFilter.SortOrderType.SO_SESSION_TYPE.value ->
                        if (typeA != typeB) typeA.ordinal < typeB.ordinal else compareByName(a, b)
                    else -> compareByName(a, b)
                }
            }
        }
    }

    private fun compareByName(a: ConversationItem, b: ConversationItem): Boolean =
        a.searchableName.compareTo(b.searchableName, ignoreCase = true) < 0
}

// ─── ConversationViewModel ───────────────────────────────────────────────────

class ConversationViewModel {
    private val sort = ConversationSort()
    private val filter = ConversationFilter()

    fun sort(items: MutableList<ConversationItem>) {
        items.sortWith { a, b -> if (sort.compare(a, b)) -1 else 1 }
    }

    fun contentsReady(): Boolean = true
    fun startDrag(items: List<ConversationItem>): Boolean = false
}

// ─── Helpers (platform stubs) ────────────────────────────────────────────────

private fun elapsedSeconds(): Double {
    return 0.0
}

private fun agentId(): UUID {
    return UUID(0L, 0L)
}
