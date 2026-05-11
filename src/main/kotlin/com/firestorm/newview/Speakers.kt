package com.firestorm.newview

import com.firestorm.llui.Color4
import com.firestorm.llui.LLSD
import com.firestorm.llui.Observable
import com.firestorm.llui.Event
import com.firestorm.llui.Timer
import com.firestorm.llui.FrameTimer
import com.firestorm.llui.EventTimer
import com.firestorm.agent.Agent
import com.firestorm.voice.VoiceChannel
import com.firestorm.voice.VoiceChannelProximal
import com.firestorm.voice.VoiceClient
import com.firestorm.world.World
import com.firestorm.newview.AvatarTracker
import com.firestorm.newview.GroupMgr
import com.firestorm.newview.IMMgr
import com.firestorm.newview.IMModel
import com.firestorm.newview.ViewerObjectList
import com.firestorm.newview.AvatarNameCache
import java.util.UUID
import kotlin.math.min

private val INACTIVE_COLOR = Color4(0.3f, 0.3f, 0.3f, 0.5f)
private val ACTIVE_COLOR = Color4(0.5f, 0.5f, 0.5f, 1.0f)

class Speaker(
    val id: UUID,
    name: String = "",
    val type: ESpeakerType = ESpeakerType.SPEAKER_AGENT
) {
    enum class ESpeakerType { SPEAKER_AGENT, SPEAKER_OBJECT, SPEAKER_EXTERNAL }

    enum class ESpeakerStatus {
        STATUS_SPEAKING,
        STATUS_HAS_SPOKEN,
        STATUS_VOICE_ACTIVE,
        STATUS_TEXT_ONLY,
        STATUS_NOT_IN_CHANNEL,
        STATUS_MUTED;

        val ordinalValue: Int get() = ordinal
    }

    var status: ESpeakerStatus = ESpeakerStatus.STATUS_TEXT_ONLY
    var lastSpokeTime: Float = 0f
    var speechVolume: Float = 0f
    var displayName: String = name
    var hasSpoken: Boolean = false
    var hasLeftCurrentCall: Boolean = false
    var dotColor: Color4 = Color4.white
    var typing: Boolean = false
    var sortIndex: Int = 0
    var speakerType: ESpeakerType = type
    var isModerator: Boolean = false
    var moderatorMutedVoice: Boolean = false
    var moderatorMutedText: Boolean = false

    private var avatarNameCacheConnection: AutoCloseable? = null

    init {
        if (name.isEmpty() && type == ESpeakerType.SPEAKER_AGENT) {
            lookupName()
        } else {
            displayName = name
        }
    }

    fun lookupName() {
        if (displayName.isEmpty()) {
            avatarNameCacheConnection = AvatarNameCache.get(id) { resolvedId, avName ->
                onNameCache(resolvedId, avName)
            }
        }
    }

    private fun onNameCache(resolvedId: UUID, avName: AvatarName) {
        displayName = avName.getUserName()
    }

    fun isInVoiceChannel(): Boolean =
        status.ordinalValue <= ESpeakerStatus.STATUS_VOICE_ACTIVE.ordinalValue ||
            status == ESpeakerStatus.STATUS_MUTED
}

class SpeakerUpdateSpeakerEvent(source: Speaker) : Event(source, "Speaker update speaker event") {
    private val speakerId: UUID = source.id

    override fun getValue(): LLSD {
        val ret = LLSD()
        ret["id"] = speakerId
        return ret
    }
}

class SpeakerUpdateModeratorEvent(source: Speaker) : Event(source, "Speaker add moderator event") {
    private val speakerId: UUID = source.id
    private val isModerator: Boolean = source.isModerator

    override fun getValue(): LLSD {
        val ret = LLSD()
        ret["id"] = speakerId
        ret["is_moderator"] = isModerator
        return ret
    }
}

class SpeakerTextModerationEvent(source: Speaker) : Event(source, "Speaker text moderation event") {
    override fun getValue(): LLSD = LLSD("text")
}

class SpeakerVoiceModerationEvent(source: Speaker) : Event(source, "Speaker voice moderation event") {
    override fun getValue(): LLSD = LLSD("voice")
}

class SpeakerListChangeEvent(source: SpeakerMgr, speakerId: UUID) :
    Event(source, "Speaker added/removed from speaker mgr") {
    private val mSpeakerId: UUID = speakerId

    override fun getValue(): LLSD = LLSD(mSpeakerId)
}

private class SortRecentSpeakers : Comparator<Speaker> {
    override fun compare(lhs: Speaker, rhs: Speaker): Int {
        if (lhs.status != rhs.status) return lhs.status.ordinalValue - rhs.status.ordinalValue
        if (lhs.lastSpokeTime != rhs.lastSpokeTime) return rhs.lastSpokeTime.compareTo(lhs.lastSpokeTime)
        return lhs.displayName.compareTo(rhs.displayName)
    }
}

class SpeakerActionTimer(
    actionCb: ((UUID) -> Boolean)?,
    actionPeriod: Float,
    speakerId: UUID
) : EventTimer(actionPeriod) {

    private var mActionCallback: ((UUID) -> Boolean)? = actionCb
    private val mSpeakerId: UUID = speakerId

    override fun tick(): Boolean {
        return mActionCallback?.invoke(mSpeakerId) ?: true
    }

    fun unset() {
        mActionCallback = null
    }
}

class SpeakersDelayActionsStorage(
    private val mActionCallback: ((UUID) -> Boolean)?,
    private val mActionDelay: Float
) {
    private val mActionTimersMap: MutableMap<UUID, SpeakerActionTimer> = mutableMapOf()

    fun destroy() {
        removeAllTimers()
    }

    fun setActionTimer(speakerId: UUID) {
        if (!mActionTimersMap.containsKey(speakerId)) {
            mActionTimersMap[speakerId] = SpeakerActionTimer(
                { id -> onTimerActionCallback(id) },
                mActionDelay,
                speakerId
            )
        }
    }

    fun unsetActionTimer(speakerId: UUID) {
        mActionTimersMap.remove(speakerId)?.unset()
    }

    fun removeAllTimers() {
        mActionTimersMap.values.forEach { it.unset() }
        mActionTimersMap.clear()
    }

    fun isTimerStarted(speakerId: UUID): Boolean = mActionTimersMap.containsKey(speakerId)

    private fun onTimerActionCallback(speakerId: UUID): Boolean {
        unsetActionTimer(speakerId)
        mActionCallback?.invoke(speakerId)
        return true
    }
}

open class SpeakerMgr(protected var mVoiceChannel: VoiceChannel?) : Observable() {

    protected val mSpeakers: MutableMap<UUID, Speaker> = mutableMapOf()
    protected var mSpeakerListUpdated: Boolean = false
    protected val mGetListTime: Timer = Timer()
    protected val mSpeakersSorted: MutableList<Speaker> = mutableListOf()
    protected val mSpeechTimer: FrameTimer = FrameTimer()

    protected val mSpeakerDelayRemover: SpeakersDelayActionsStorage

    protected var mVoiceModerated: Boolean = false
    protected var mModerateModeHandledFirstTime: Boolean = false

    init {
        mGetListTime.reset()
        val removeDelay = SavedSettings.getFloat("SpeakerParticipantRemoveDelay", 10.0f)
        mSpeakerDelayRemover = SpeakersDelayActionsStorage({ id -> removeSpeaker(id) }, removeDelay)
    }

    fun destroy() {
        mSpeakerDelayRemover.destroy()
    }

    fun findSpeaker(speakerId: UUID): Speaker? {
        return mSpeakers[speakerId]
    }

    fun setSpeaker(
        id: UUID,
        name: String = "",
        status: Speaker.ESpeakerStatus = Speaker.ESpeakerStatus.STATUS_TEXT_ONLY,
        type: Speaker.ESpeakerType = Speaker.ESpeakerType.SPEAKER_AGENT
    ): Speaker? {
        if (mVoiceChannel == null) return null
        val sessionId = getSessionID()
        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        if (id == nullId || id == sessionId) return null

        val speakerp: Speaker
        if (!mSpeakers.containsKey(id)) {
            speakerp = Speaker(id, name, type)
            speakerp.status = status
            mSpeakers[id] = speakerp
            mSpeakersSorted.add(speakerp)
            fireEvent(SpeakerListChangeEvent(this, speakerp.id), "add")
        } else {
            val existing = findSpeaker(id) ?: run {
                return null
            }
            existing.status = if (existing.status.ordinalValue < status.ordinalValue) existing.status else status
            if (type == Speaker.ESpeakerType.SPEAKER_AGENT) {
                existing.speakerType = Speaker.ESpeakerType.SPEAKER_AGENT
                existing.lookupName()
            }
            speakerp = existing
        }

        mSpeakerDelayRemover.unsetActionTimer(speakerp.id)
        return speakerp
    }

    fun initVoiceModerateMode() {
        if (!mModerateModeHandledFirstTime && mVoiceChannel?.isActive() == true) {
            val speakerp = mSpeakers[gAgentID]
            if (speakerp != null) {
                mVoiceModerated = speakerp.moderatorMutedVoice
                mModerateModeHandledFirstTime = true
            }
        }
    }

    fun update(resortOk: Boolean) {
        val voiceClient = VoiceClient.instance ?: return

        val speakingColor = UIColorTable.instance.getColor("SpeakingColor")
        val overdrivenColor = UIColorTable.instance.getColor("OverdrivenColor")

        if (resortOk) updateSpeakerList()

        val voiceChannelActive = (mVoiceChannel == null && voiceClient.inProximalChannel()) ||
            (mVoiceChannel?.isActive() == true)

        for ((speakerId, speakerp) in mSpeakers) {
            if (voiceChannelActive && voiceClient.getVoiceEnabled(speakerId)) {
                speakerp.speechVolume = voiceClient.getCurrentPower(speakerId)
                val moderatorMutedVoice = voiceClient.getIsModeratorMuted(speakerId)
                if (moderatorMutedVoice != speakerp.moderatorMutedVoice) {
                    speakerp.moderatorMutedVoice = moderatorMutedVoice
                    speakerp.fireEvent(SpeakerVoiceModerationEvent(speakerp))
                }

                when {
                    voiceClient.getOnMuteList(speakerId) || speakerp.moderatorMutedVoice ->
                        speakerp.status = Speaker.ESpeakerStatus.STATUS_MUTED

                    voiceClient.getIsSpeaking(speakerId) -> {
                        if (speakerp.status != Speaker.ESpeakerStatus.STATUS_SPEAKING) {
                            speakerp.lastSpokeTime = mSpeechTimer.getElapsedTimeF32()
                            speakerp.hasSpoken = true
                            fireEvent(SpeakerUpdateSpeakerEvent(speakerp), "update_speaker")
                        }
                        speakerp.status = Speaker.ESpeakerStatus.STATUS_SPEAKING
                        speakerp.dotColor = speakingColor
                        if (speakerp.speechVolume > VoiceClient.OVERDRIVEN_POWER_LEVEL) {
                            speakerp.dotColor = overdrivenColor
                        }
                    }

                    else -> {
                        speakerp.speechVolume = 0f
                        speakerp.dotColor = ACTIVE_COLOR
                        speakerp.status = if (speakerp.hasSpoken)
                            Speaker.ESpeakerStatus.STATUS_HAS_SPOKEN
                        else
                            Speaker.ESpeakerStatus.STATUS_VOICE_ACTIVE
                    }
                }
            } else if (speakerp.status != Speaker.ESpeakerStatus.STATUS_NOT_IN_CHANNEL) {
                if (speakerp.speakerType == Speaker.ESpeakerType.SPEAKER_EXTERNAL) {
                    speakerp.status = Speaker.ESpeakerStatus.STATUS_NOT_IN_CHANNEL
                } else {
                    speakerp.status = Speaker.ESpeakerStatus.STATUS_TEXT_ONLY
                    speakerp.speechVolume = 0f
                    speakerp.dotColor = ACTIVE_COLOR
                }
            }
        }

        if (resortOk) {
            mSpeakersSorted.sortWith(SortRecentSpeakers())
        }

        var recentSpeakerCount = 0
        var sortIndex = 0
        for (speakerp in mSpeakersSorted) {
            if (speakerp.status == Speaker.ESpeakerStatus.STATUS_HAS_SPOKEN) {
                val t = clampRescale(recentSpeakerCount.toFloat(), -2f, 3f, 0f, 1f)
                speakerp.dotColor = lerp(speakingColor, ACTIVE_COLOR, t)
                recentSpeakerCount++
            }
            speakerp.sortIndex = sortIndex++
        }
    }

    protected open fun updateSpeakerList() {
        val voiceClient = VoiceClient.instance ?: return
        if ((mVoiceChannel == null && voiceClient.inProximalChannel()) ||
            mVoiceChannel?.isActive() == true
        ) {
            val participants = mutableSetOf<UUID>()
            voiceClient.getParticipantList(participants)
            for (participantId in participants) {
                setSpeaker(
                    participantId,
                    voiceClient.getDisplayName(participantId),
                    Speaker.ESpeakerStatus.STATUS_VOICE_ACTIVE,
                    if (voiceClient.isParticipantAvatar(participantId))
                        Speaker.ESpeakerType.SPEAKER_AGENT
                    else
                        Speaker.ESpeakerType.SPEAKER_EXTERNAL
                )
            }
        } else if (mVoiceChannel != null) {
            val sessionId = getSessionID()
            val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
            if (sessionId != nullId && !mSpeakerListUpdated) {
                val session = IMModel.instance.findIMSession(sessionId)
                if (session != null && session.isGroupSessionType() && mSpeakers.size <= 1) {
                    val gdatap = GroupMgr.instance.getGroupData(sessionId)
                    if (gdatap != null && gdatap.isMemberDataComplete() && gdatap.members.isNotEmpty()) {
                        val loadGroupMaxMembers = SavedSettings.getInt("ChatLoadGroupMaxMembers")
                        var updated = 0
                        for ((memberId, member) in gdatap.members) {
                            if (member.getOnlineStatus() == "Online" && !mSpeakers.containsKey(memberId)) {
                                val speakerp = setSpeaker(memberId, "", Speaker.ESpeakerStatus.STATUS_VOICE_ACTIVE, Speaker.ESpeakerType.SPEAKER_AGENT)
                                speakerp?.isModerator = (member.getAgentPowers() and GP_SESSION_MODERATOR) == GP_SESSION_MODERATOR
                                updated++
                            }
                            if (updated >= loadGroupMaxMembers) break
                        }
                        mSpeakerListUpdated = true
                    }
                } else if (mSpeakers.isEmpty()) {
                    session?.initialTargetIDs?.forEach { targetId ->
                        if (!AvatarTracker.instance.isBuddy(targetId) || AvatarTracker.instance.isBuddyOnline(targetId)) {
                            setSpeaker(targetId, "", Speaker.ESpeakerStatus.STATUS_VOICE_ACTIVE, Speaker.ESpeakerType.SPEAKER_AGENT)
                        }
                    }
                    mSpeakerListUpdated = true
                } else {
                    mSpeakerListUpdated = true
                }
            }
        }
        setSpeaker(gAgentID, "", Speaker.ESpeakerStatus.STATUS_VOICE_ACTIVE, Speaker.ESpeakerType.SPEAKER_AGENT)
    }

    protected fun setSpeakerNotInChannel(speakerp: Speaker?) {
        speakerp ?: return
        speakerp.status = Speaker.ESpeakerStatus.STATUS_NOT_IN_CHANNEL
        speakerp.dotColor = INACTIVE_COLOR
        mSpeakerDelayRemover.setActionTimer(speakerp.id)
    }

    protected fun removeSpeaker(speakerId: UUID): Boolean {
        mSpeakers.remove(speakerId)
        mSpeakersSorted.removeIf { it.id == speakerId }
        fireEvent(SpeakerListChangeEvent(this, speakerId), "remove")
        update(true)
        return false
    }

    fun getSpeakerList(speakerList: MutableList<Speaker>, includeText: Boolean) {
        speakerList.clear()
        for ((_, speakerp) in mSpeakers) {
            if (includeText || speakerp.status != Speaker.ESpeakerStatus.STATUS_TEXT_ONLY) {
                speakerList.add(speakerp)
            }
        }
    }

    fun getVoiceChannel(): VoiceChannel? = mVoiceChannel

    fun setVoiceChannel(voiceChannel: VoiceChannel) {
        mVoiceChannel = voiceChannel
    }

    fun getSessionID(): UUID =
        mVoiceChannel?.getSessionID() ?: UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun isSpeakerToBeRemoved(speakerId: UUID): Boolean =
        mSpeakerDelayRemover.isTimerStarted(speakerId)

    fun setSpeakerTyping(speakerId: UUID, typing: Boolean) {
        findSpeaker(speakerId)?.typing = typing
    }

    fun speakerChatted(speakerId: UUID) {
        val speakerp = findSpeaker(speakerId) ?: return
        speakerp.lastSpokeTime = mSpeechTimer.getElapsedTimeF32()
        speakerp.hasSpoken = true
        fireEvent(SpeakerUpdateSpeakerEvent(speakerp), "update_speaker")
    }

    fun isVoiceActive(): Boolean =
        VoiceClient.instance?.voiceEnabled() == true && mVoiceChannel?.isActive() == true

    private val speakingColor get() = UIColorTable.instance.getColor("SpeakingColor")
    private val ACTIVE_COLOR get() = com.firestorm.newview.ACTIVE_COLOR

    private fun lerp(a: Color4, b: Color4, t: Float): Color4 = Color4(
        a.r + (b.r - a.r) * t,
        a.g + (b.g - a.g) * t,
        a.b + (b.b - a.b) * t,
        a.a + (b.a - a.a) * t
    )

    private fun clampRescale(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        val clamped = value.coerceIn(inMin, inMax)
        return outMin + (clamped - inMin) / (inMax - inMin) * (outMax - outMin)
    }

    companion object {
        private const val GP_SESSION_MODERATOR: Long = 0x0000000000000200L
    }
}

open class IMSpeakerMgr(channel: VoiceChannel?) : SpeakerMgr(channel) {

    override fun updateSpeakerList() {
        super.updateSpeakerList()
    }

    fun setSpeakers(speakers: LLSD) {
        if (!speakers.isMap()) return

        if (speakers.has("agent_info") && speakers["agent_info"].isMap()) {
            for ((agentIdStr, agentData) in speakers["agent_info"].asMap()) {
                val agentId = UUID.fromString(agentIdStr)
                val speakerp = setSpeaker(agentId, "", Speaker.ESpeakerStatus.STATUS_TEXT_ONLY) ?: continue

                if (agentData.isMap()) {
                    val wasModerator = speakerp.isModerator
                    speakerp.isModerator = agentData["is_moderator"].asBoolean()
                    speakerp.moderatorMutedText = agentData["mutes"]["text"].asBoolean()
                    if (wasModerator != speakerp.isModerator) {
                        fireEvent(SpeakerUpdateModeratorEvent(speakerp), "update_moderator")
                    }
                }
            }
        } else if (speakers.has("agents") && speakers["agents"].isArray()) {
            for (agentEntry in speakers["agents"].asArray()) {
                val agentId = agentEntry.asUUID()
                setSpeaker(agentId, "", Speaker.ESpeakerStatus.STATUS_TEXT_ONLY)
            }
        }
    }

    fun updateSpeakers(update: LLSD) {
        if (!update.isMap()) return

        if (update.has("agent_updates") && update["agent_updates"].isMap()) {
            for ((agentIdStr, agentData) in update["agent_updates"].asMap()) {
                val agentId = UUID.fromString(agentIdStr)
                var speakerp = findSpeaker(agentId)

                if (agentData.isMap() && agentData.has("transition")) {
                    when (agentData["transition"].asString()) {
                        "LEAVE" -> setSpeakerNotInChannel(speakerp)
                        "ENTER" -> {
                            speakerp = setSpeaker(agentId)
                            if (speakerp != null && agentData.has("has_spoken")) {
                                speakerp.lastSpokeTime = mSpeechTimer.getElapsedTimeF32()
                                speakerp.hasSpoken = true
                            }
                        }
                    }
                }

                speakerp ?: continue

                if (agentData.isMap() && agentData.has("info")) {
                    val agentInfo = agentData["info"]
                    if (agentInfo.has("is_moderator")) {
                        val wasModerator = speakerp.isModerator
                        speakerp.isModerator = agentInfo["is_moderator"].asBoolean()
                        if (wasModerator != speakerp.isModerator) {
                            fireEvent(SpeakerUpdateModeratorEvent(speakerp), "update_moderator")
                        }
                    }
                    if (agentInfo.has("mutes")) {
                        speakerp.moderatorMutedText = agentInfo["mutes"]["text"].asBoolean()
                    }
                }
            }
        } else if (update.has("updates") && update["updates"].isMap()) {
            for ((agentIdStr, transitionData) in update["updates"].asMap()) {
                val agentId = UUID.fromString(agentIdStr)
                val speakerp = findSpeaker(agentId)
                when (transitionData.asString()) {
                    "LEAVE" -> setSpeakerNotInChannel(speakerp)
                    "ENTER" -> setSpeaker(agentId)
                }
            }
        }
    }

    fun allowTextChat(speakerId: UUID, allow: Boolean) {
        if (mVoiceChannel == null) return
        val url = gAgent.getRegionCapability("ChatSessionRequest")
        val data = LLSD()
        data["method"] = "mute update"
        data["session-id"] = getSessionID()
        data["params"] = LLSD.emptyMap()
        data["params"]["agent_id"] = speakerId
        data["params"]["mute_info"] = LLSD.emptyMap()
        data["params"]["mute_info"]["text"] = !allow
        TODO("APR: use JVM equivalent for coroutine HTTP POST to $url with data=$data")
    }

    fun moderateVoiceParticipant(avatarId: UUID, unmute: Boolean) {
        val speakerp = findSpeaker(avatarId) ?: return
        if (mVoiceChannel == null) return
        val isInVoice = speakerp.status.ordinalValue <= Speaker.ESpeakerStatus.STATUS_VOICE_ACTIVE.ordinalValue ||
            speakerp.status == Speaker.ESpeakerStatus.STATUS_MUTED
        if (!isInVoice) return

        val url = gAgent.getRegionCapability("ChatSessionRequest")
        val data = LLSD()
        data["method"] = "mute update"
        data["session-id"] = getSessionID()
        data["params"] = LLSD.emptyMap()
        data["params"]["agent_id"] = avatarId
        data["params"]["mute_info"] = LLSD.emptyMap()
        data["params"]["mute_info"]["voice"] = !unmute
        TODO("APR: use JVM equivalent for coroutine HTTP POST to $url with data=$data")
    }

    fun moderateVoiceAllParticipants(unmuteEveryone: Boolean) {
        if (mVoiceModerated == !unmuteEveryone) {
            forceVoiceModeratedMode(mVoiceModerated)
        } else {
            moderateVoiceSession(getSessionID(), !unmuteEveryone)
        }
    }

    fun processSessionUpdate(sessionUpdate: LLSD) {
        if (sessionUpdate.has("moderated_mode") && sessionUpdate["moderated_mode"].has("voice")) {
            mVoiceModerated = sessionUpdate["moderated_mode"]["voice"].asBoolean()
        }
    }

    private fun moderateVoiceSession(sessionId: UUID, disallowVoice: Boolean) {
        val url = gAgent.getRegionCapability("ChatSessionRequest")
        val data = LLSD()
        data["method"] = "session update"
        data["session-id"] = sessionId
        data["params"] = LLSD.emptyMap()
        data["params"]["update_info"] = LLSD.emptyMap()
        data["params"]["update_info"]["moderated_mode"] = LLSD.emptyMap()
        data["params"]["update_info"]["moderated_mode"]["voice"] = disallowVoice
        TODO("APR: use JVM equivalent for coroutine HTTP POST to $url with data=$data")
    }

    private fun forceVoiceModeratedMode(shouldBeMuted: Boolean) {
        for ((speakerId, speakerp) in mSpeakers) {
            if (shouldBeMuted != speakerp.moderatorMutedVoice) {
                moderateVoiceParticipant(speakerId, !shouldBeMuted)
            }
        }
    }
}

object ActiveSpeakerMgr : SpeakerMgr(null) {

    override fun updateSpeakerList() {
        mVoiceChannel = VoiceChannel.getCurrentVoiceChannel()

        fireEvent(SpeakerListChangeEvent(this, UUID.fromString("00000000-0000-0000-0000-000000000000")), "clear")
        mSpeakers.clear()
        mSpeakersSorted.clear()
        mSpeakerDelayRemover.removeAllTimers()

        super.updateSpeakerList()

        for ((_, speakerp) in mSpeakers) {
            if (speakerp.status == Speaker.ESpeakerStatus.STATUS_TEXT_ONLY) {
                speakerp.status = Speaker.ESpeakerStatus.STATUS_NOT_IN_CHANNEL
            }
        }
    }
}

object LocalSpeakerMgr : SpeakerMgr(VoiceChannelProximal.instance) {

    override fun updateSpeakerList() {
        super.updateSpeakerList()

        if (gDisconnected) return

        val avatarIds = mutableListOf<UUID>()
        val positions = mutableListOf<Vector3d>()
        val sayRange = LFSimFeatureHandler.instance.sayRange()
        World.instance.getAvatars(avatarIds, positions, gAgent.getPositionGlobal(), sayRange.toFloat())

        for (avatarId in avatarIds) {
            setSpeaker(avatarId)
        }

        val sayDistanceSquared = sayRange * sayRange
        for ((speakerId, speakerp) in mSpeakers) {
            if (speakerp.status == Speaker.ESpeakerStatus.STATUS_TEXT_ONLY) {
                val avatarp = ViewerObjectList.findObject(speakerId) as? VOAvatar
                if (avatarp == null || distVecSquared(avatarp.getPositionAgent(), gAgent.getPositionAgent()) > sayDistanceSquared) {
                    setSpeakerNotInChannel(speakerp)
                }
            }
        }
    }

    private fun distVecSquared(a: Vector3, b: Vector3): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        val dz = (a.z - b.z).toDouble()
        return dx * dx + dy * dy + dz * dz
    }
}
