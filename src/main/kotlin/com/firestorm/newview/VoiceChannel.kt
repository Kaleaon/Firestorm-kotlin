package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

open class VoiceChannel(
    val sessionId: LLUUID,
    val sessionName: String,
) : VoiceObserver {

    enum class State {
        NO_CHANNEL_INFO,
        ERROR,
        HUNG_UP,
        READY,
        CALL_STARTED,
        RINGING,
        CONNECTED,
    }

    enum class Direction {
        INCOMING_CALL,
        OUTGOING_CALL,
    }

    var state: State = State.NO_CHANNEL_INFO
        private set

    var callDirection: Direction = Direction.OUTGOING_CALL
    var channelInfo: Map<String, Any?> = emptyMap()
        protected set

    protected var callEndedByAgent: Boolean = false
    protected var ignoreNextSessionLeave: Boolean = false

    val notifyArgs: MutableMap<String, Any?> = mutableMapOf("VOICE_CHANNEL_NAME" to sessionName)

    private val stateChangedCallbacks: MutableList<(old: State, new: State, dir: Direction, endedByAgent: Boolean, id: LLUUID) -> Unit> =
        mutableListOf()

    init {
        if (voiceChannelMap.containsKey(sessionId)) {
            // duplicate registration; this channel will be orphaned
        } else {
            voiceChannelMap[sessionId] = this
        }
    }

    fun setStateChangedCallback(cb: (old: State, new: State, dir: Direction, endedByAgent: Boolean, id: LLUUID) -> Unit) {
        stateChangedCallbacks.add(cb)
    }

    open fun isActive(): Boolean =
        callStarted() && VoiceClient.isCurrentChannel(channelInfo)

    fun callStarted(): Boolean = state >= State.CALL_STARTED

    override fun onChange(status: VoiceObserver.StatusType, channelInfo: Map<String, Any?>, proximal: Boolean) {
        if (this.channelInfo.isEmpty()) {
            this.channelInfo = channelInfo
        }
        if (!VoiceClient.compareChannels(this.channelInfo, channelInfo)) return

        if (status < VoiceObserver.StatusType.BEGIN_ERROR_STATUS) {
            handleStatusChange(status)
        } else {
            handleError(status)
        }
    }

    open fun handleStatusChange(status: VoiceObserver.StatusType) {
        when (status) {
            VoiceObserver.StatusType.STATUS_LOGIN_RETRY -> Unit
            VoiceObserver.StatusType.STATUS_LOGGED_IN   -> Unit
            VoiceObserver.StatusType.STATUS_LEFT_CHANNEL -> {
                if (callStarted() && !sSuspended) {
                    deactivate()
                }
            }
            VoiceObserver.StatusType.STATUS_JOINING -> if (callStarted()) setState(State.RINGING)
            VoiceObserver.StatusType.STATUS_JOINED  -> if (callStarted()) setState(State.CONNECTED)
            else -> Unit
        }
    }

    open fun handleError(status: VoiceObserver.StatusType) {
        deactivate()
        setState(State.ERROR)
    }

    open fun deactivate() {
        if (state >= State.RINGING) {
            ignoreNextSessionLeave = true
        }
        if (callStarted()) {
            setState(State.HUNG_UP)
            TODO("IPC: if AutoDisengageMic and this is sCurrentVoiceChannel and userPTTState, reset PTT")
        }
        VoiceClient.removeObserver(this)
        if (sCurrentVoiceChannel === this) {
            sCurrentVoiceChannel = VoiceChannelProximal
            sCurrentVoiceChannel?.activate()
        }
    }

    open fun activate() {
        if (callStarted()) return

        if (sCurrentVoiceChannel !== this) {
            val old = sCurrentVoiceChannel
            sCurrentVoiceChannel = this
            old?.deactivate()
        }

        if (state == State.NO_CHANNEL_INFO) {
            requestChannelInfo()
        } else {
            setState(State.CALL_STARTED)
        }

        VoiceClient.addObserver(this)
        sCurrentVoiceChannelChangedCallbacks.forEach { it(sessionId) }
    }

    open fun setChannelInfo(info: Map<String, Any?>) {
        channelInfo = info
        if (state == State.NO_CHANNEL_INFO) {
            if (info.isEmpty()) {
                TODO("UI: show VoiceChannelJoinFailed notification")
                deactivate()
            } else {
                setState(State.READY)
                if (sCurrentVoiceChannel === this) activate()
            }
        }
    }

    open fun resetChannelInfo() {
        channelInfo = emptyMap()
        state = State.NO_CHANNEL_INFO
    }

    open fun requestChannelInfo() {
        if (sCurrentVoiceChannel === this) {
            setState(State.CALL_STARTED)
        }
    }

    protected open fun setState(newState: State) {
        doSetState(newState)
    }

    protected fun doSetState(newState: State) {
        val old = state
        state = newState
        stateChangedCallbacks.forEach { it(old, state, callDirection, callEndedByAgent, sessionId) }
    }

    fun updateSessionId(newId: LLUUID) {
        voiceChannelMap.remove(sessionId)
        voiceChannelMap[newId] = this
        TODO("IPC: update mSessionID field — requires mutable sessionId or reassignment")
    }

    fun isThisVoiceChannel(info: Map<String, Any?>): Boolean =
        VoiceClient.compareChannels(channelInfo, info)

    companion object {
        private val voiceChannelMap: MutableMap<LLUUID, VoiceChannel> = mutableMapOf()
        var sCurrentVoiceChannel: VoiceChannel? = null
            private set
        var sSuspendedVoiceChannel: VoiceChannel? = null
            private set
        var sSuspended: Boolean = false
            private set

        val sCurrentVoiceChannelChangedCallbacks: MutableList<(LLUUID) -> Unit> = mutableListOf()

        fun addCurrentVoiceChannelChangedCallback(cb: (LLUUID) -> Unit, atFront: Boolean = false) {
            if (atFront) sCurrentVoiceChannelChangedCallbacks.add(0, cb)
            else sCurrentVoiceChannelChangedCallbacks.add(cb)
        }

        fun getChannelById(sessionId: LLUUID): VoiceChannel? = voiceChannelMap[sessionId]

        fun getCurrentVoiceChannel(): VoiceChannel? = sCurrentVoiceChannel

        fun initClass() {
            sCurrentVoiceChannel = VoiceChannelProximal
        }

        fun suspend() {
            if (!sSuspended) {
                sSuspendedVoiceChannel = sCurrentVoiceChannel
                sSuspended = true
                sSuspendedVoiceChannel?.sessionId?.let { id ->
                    sCurrentVoiceChannelChangedCallbacks.forEach { it(id) }
                }
            }
        }

        fun resume() {
            if (sSuspended) {
                sSuspended = false
                if (VoiceClient.voiceEnabled()) {
                    val ch = sSuspendedVoiceChannel
                    if (ch != null) {
                        if (ch.callStarted()) ch.setState(State.READY)
                        ch.activate()
                    } else {
                        VoiceChannelProximal.activate()
                    }
                }
            }
        }
    }
}

open class VoiceChannelGroup(
    sessionId: LLUUID,
    sessionName: String,
    val isP2P: Boolean,
) : VoiceChannel(sessionId, sessionName) {

    companion object {
        private const val DEFAULT_RETRIES_COUNT: UInt = 3u
    }

    private var retries: UInt = DEFAULT_RETRIES_COUNT
    private var isRetrying: Boolean = false

    override fun deactivate() {
        if (callStarted()) {
            VoiceClient.leaveNonSpatialChannel()
        }
        super.deactivate()
        if (isP2P) {
            setState(State.NO_CHANNEL_INFO)
        }
    }

    override fun activate() {
        if (callStarted()) return
        super.activate()
        if (callStarted()) {
            VoiceClient.setNonSpatialChannel(
                channelInfo,
                isP2P && callDirection == Direction.OUTGOING_CALL,
                isP2P,
            )
            if (isP2P) {
                TODO("IPC: LLIMModel.addSpeakersToRecent(sessionId)")
            } else {
                TODO("IPC: add ad-hoc call participants to recent people list")
            }
            VoiceClient.setUserPTTState(isP2P)
        }
    }

    override fun requestChannelInfo() {
        TODO("IPC: GET region capability ChatSessionRequest; launch voiceCallCapCoro with preferred voice server type")
    }

    override fun setChannelInfo(info: Map<String, Any?>) {
        channelInfo = info
        if (state == State.NO_CHANNEL_INFO) {
            if (info.isNotEmpty()) {
                setState(State.READY)
                if (VoiceChannel.sCurrentVoiceChannel === this) activate()
            } else {
                TODO("UI: notify user of invalid credentials for $sessionName")
                deactivate()
            }
        } else if (isRetrying) {
            VoiceClient.setNonSpatialChannel(info, callDirection == Direction.OUTGOING_CALL, isP2P)
        }
    }

    override fun handleStatusChange(status: VoiceObserver.StatusType) {
        if (status == VoiceObserver.StatusType.STATUS_JOINED) {
            retries = 3u
            isRetrying = false
        }
        super.handleStatusChange(status)
    }

    override fun handleError(status: VoiceObserver.StatusType) {
        val notify: String? = when (status) {
            VoiceObserver.StatusType.ERROR_CHANNEL_LOCKED,
            VoiceObserver.StatusType.ERROR_CHANNEL_FULL -> "VoiceChannelFull"
            VoiceObserver.StatusType.ERROR_NOT_AVAILABLE -> {
                if (retries > 0u) {
                    retries--
                    isRetrying = true
                    ignoreNextSessionLeave = true
                    requestChannelInfo()
                    return
                } else {
                    retries = DEFAULT_RETRIES_COUNT
                    isRetrying = false
                    "VoiceChannelJoinFailed"
                }
            }
            else -> null
        }
        if (notify != null) {
            TODO("UI: show $notify notification; echo to IM window")
        }
        super.handleError(status)
    }

    override fun setState(newState: State) {
        if (newState == State.RINGING && !isRetrying) {
            doSetState(newState)
            return
        }
        super.setState(newState)
    }
}

object VoiceChannelProximal : VoiceChannel(LLUUID.NULL, "") {

    override fun isActive(): Boolean =
        callStarted() && VoiceClient.inProximalChannel()

    override fun activate() {
        if (callStarted()) return
        if (sCurrentVoiceChannel !== this && sCurrentVoiceChannel?.state == State.CONNECTED) {
            VoiceClient.leaveNonSpatialChannel()
        }
        VoiceClient.activateSpatialChannel(true)
        super.activate()
    }

    override fun onChange(status: VoiceObserver.StatusType, channelInfo: Map<String, Any?>, proximal: Boolean) {
        if (!proximal) return
        if (status < VoiceObserver.StatusType.BEGIN_ERROR_STATUS) {
            handleStatusChange(status)
        } else {
            handleError(status)
        }
    }

    override fun handleStatusChange(status: VoiceObserver.StatusType) {
        when (status) {
            VoiceObserver.StatusType.STATUS_LEFT_CHANNEL -> return
            VoiceObserver.StatusType.STATUS_VOICE_DISABLED -> {
                VoiceClient.setUserPTTState(false)
                TODO("IPC: agent.setVoiceConnected(false)")
                return
            }
            else -> Unit
        }
        super.handleStatusChange(status)
    }

    override fun handleError(status: VoiceObserver.StatusType) {
        val notify = when (status) {
            VoiceObserver.StatusType.ERROR_CHANNEL_LOCKED,
            VoiceObserver.StatusType.ERROR_CHANNEL_FULL -> "ProximalVoiceChannelFull"
            else -> null
        }
        if (notify != null) {
            TODO("UI: show $notify notification")
        }
        // proximal voice provider will try to reconnect; no deactivate here
    }

    override fun deactivate() {
        if (callStarted()) setState(State.HUNG_UP)
        VoiceClient.removeObserver(this)
        VoiceClient.activateSpatialChannel(false)
    }
}

class VoiceChannelP2P(
    sessionId: LLUUID,
    sessionName: String,
    private val otherUserId: LLUUID,
    private val outgoingCallInterface: VoiceP2POutgoingCallInterface,
) : VoiceChannelGroup(sessionId, sessionName, true) {

    private var receivedCall: Boolean = false
    private var incomingCallInterface: VoiceP2PIncomingCallInterface? = null

    init {
        channelInfo = VoiceClient.getP2PChannelInfoTemplate(otherUserId)
    }

    override fun handleStatusChange(status: VoiceObserver.StatusType) {
        when (status) {
            VoiceObserver.StatusType.STATUS_LEFT_CHANNEL -> {
                if (callStarted() && !ignoreNextSessionLeave && !sSuspended) {
                    if (state == State.RINGING) {
                        TODO("UI: show P2PCallDeclined notification")
                    } else {
                        callEndedByAgent = false
                    }
                    deactivate()
                }
                ignoreNextSessionLeave = false
                return
            }
            VoiceObserver.StatusType.STATUS_JOINING -> {
                ignoreNextSessionLeave = false
            }
            else -> Unit
        }
        super.handleStatusChange(status)
    }

    override fun handleError(status: VoiceObserver.StatusType) {
        if (status == VoiceObserver.StatusType.ERROR_NOT_AVAILABLE) {
            TODO("UI: show P2PCallNoAnswer notification")
        }
        super.handleError(status)
    }

    override fun activate() {
        if (callStarted()) return
        callEndedByAgent = true
        super.activate()
        if (callStarted()) {
            if (incomingCallInterface == null) {
                receivedCall = false
                outgoingCallInterface.callUser(otherUserId)
            } else {
                if (!incomingCallInterface!!.answerInvite()) {
                    callEndedByAgent = false
                    incomingCallInterface = null
                    handleError(VoiceObserver.StatusType.ERROR_UNKNOWN)
                    return
                }
                incomingCallInterface = null
            }
            addToRecentPeopleList()
            if (!VoiceClient.getUserPTTState() && VoiceClient.getPTTIsToggle()) {
                VoiceClient.inputUserControlState(true)
            }
        }
    }

    override fun deactivate() {
        if (callStarted()) {
            outgoingCallInterface.hangup()
        }
        super.deactivate()
    }

    override fun requestChannelInfo() {
        if (sCurrentVoiceChannel === this) {
            setState(State.CALL_STARTED)
        }
    }

    override fun setChannelInfo(info: Map<String, Any?>) {
        channelInfo = info
        var needsActivate = false
        if (callStarted()) {
            TODO("IPC: compare otherUserId to gAgent.getID()")
            // if otherUserId < agentId: deactivate(); needsActivate = true
            // else: outgoingCallInterface.callUser(otherUserId); return
        }
        receivedCall = true
        if (info.isNotEmpty()) {
            incomingCallInterface = VoiceClient.getIncomingCallInterface(info)
        }
        if (needsActivate) activate()
    }

    override fun resetChannelInfo() {
        channelInfo = VoiceClient.getP2PChannelInfoTemplate(otherUserId)
        // template is not full info, so stay in NO_CHANNEL_INFO
    }

    override fun setState(newState: State) {
        if (receivedCall && newState == State.RINGING) {
            doSetState(newState)
            return
        }
        super.setState(newState)
    }

    private fun addToRecentPeopleList() {
        TODO("IPC: RecentPeople.add(otherUserId)")
    }
}
