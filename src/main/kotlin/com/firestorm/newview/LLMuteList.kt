package com.firestorm.newview

import java.util.UUID

class LLMute(
    var id: UUID,
    val name: String = "",
    val type: EType = EType.BY_NAME,
    var flags: UInt = 0u
) {
    enum class EType(val value: Int) {
        BY_NAME(0),
        AGENT(1),
        OBJECT(2),
        GROUP(3),
        EXTERNAL(4),
        COUNT(5)
    }

    companion object {
        const val FLAG_TEXT_CHAT: UInt = 0x00000001u
        const val FLAG_VOICE_CHAT: UInt = 0x00000002u
        const val FLAG_PARTICLES: UInt = 0x00000004u
        const val FLAG_OBJECT_SOUNDS: UInt = 0x00000008u
        const val FLAG_ALL: UInt = 0x0000000Fu
    }

    fun getDisplayType(): String = when (type) {
        EType.BY_NAME -> { System.err.println("LLMute: LLTrans::getString(\"MuteByName\") not yet implemented"); "" }
        EType.AGENT -> { System.err.println("LLMute: LLTrans::getString(\"MuteAgent\") not yet implemented"); "" }
        EType.OBJECT -> { System.err.println("LLMute: LLTrans::getString(\"MuteObject\") not yet implemented"); "" }
        EType.GROUP -> { System.err.println("LLMute: LLTrans::getString(\"MuteGroup\") not yet implemented"); "" }
        EType.EXTERNAL -> { System.err.println("LLMute: LLTrans::getString(\"MuteExternal\") not yet implemented"); "" }
        EType.COUNT -> ""
    }
}

interface LLMuteListObserver {
    fun onChange()
    fun onChangeDetailed(mute: LLMute) {}
}

object LLMuteList {

    enum class EMuteListState {
        ML_INITIAL,
        ML_REQUESTED,
        ML_LOADED,
        ML_FAILED
    }

    enum class EAutoReason(val value: Int) {
        AR_IM(0),
        AR_MONEY(1),
        AR_INVENTORY(2),
        AR_COUNT(3)
    }

    private val mMutes: MutableSet<LLMute> = mutableSetOf()
    private val mLegacyMutes: MutableSet<String> = mutableSetOf()
    private val mObservers: MutableSet<LLMuteListObserver> = mutableSetOf()
    private val mPendingAgentNameUpdates: MutableMap<UUID, String> = mutableMapOf()

    private var mLoadState: EMuteListState = EMuteListState.ML_INITIAL
    private var mRequestStartTime: Double = 0.0

    private val MUTE_LIST_LIMIT = 1000

    fun addObserver(observer: LLMuteListObserver) {
        mObservers.add(observer)
    }

    fun removeObserver(observer: LLMuteListObserver) {
        mObservers.remove(observer)
    }

    fun isLoaded(): Boolean = mLoadState == EMuteListState.ML_LOADED

    fun getLoadFailed(): Boolean {
        if (mLoadState == EMuteListState.ML_FAILED) return true
        if (mLoadState == EMuteListState.ML_REQUESTED) {
            val waitSeconds = 30.0
            System.err.println("LLMuteList: LLTimer::getTotalSeconds() not yet implemented")
            val totalSeconds = 0.0
            if (mRequestStartTime + waitSeconds < totalSeconds) {
                return true
            }
        }
        return false
    }

    fun add(mute: LLMute, flags: UInt = 0u): Boolean {
        if (mute.type == LLMute.EType.AGENT && isLinden(mute.name) &&
            (flags and LLMute.FLAG_TEXT_CHAT != 0u || flags == 0u)) {
            return false
        }

        System.err.println("LLMuteList: gAgent.getID() not yet implemented")
        val agentId: UUID = UUID(0, 0)
        if (mute.type == LLMute.EType.AGENT && mute.id == agentId) {
            return false
        }

        if (getMutes().size >= MUTE_LIST_LIMIT) {
            return false
        }

        if (mute.type == LLMute.EType.BY_NAME) {
            if (mute.name.isEmpty()) return false
            if (mute.id != UUID(0, 0)) return false
            if (mLegacyMutes.add(mute.name)) {
                updateAdd(mute)
                notifyObservers()
                notifyObserversDetailed(mute)
                return true
            }
            return false
        } else {
            var showMessage = false
            val localMute = LLMute(mute.id, mute.name, mute.type, mute.flags)
            val existing = mMutes.find { it.id == localMute.id }
            if (existing != null) {
                localMute.flags = existing.flags
                mMutes.remove(existing)
                showMessage = false
            } else {
                localMute.flags = LLMute.FLAG_ALL
                showMessage = true
            }

            localMute.flags = if (flags != 0u) {
                localMute.flags and flags.inv()
            } else {
                0u
            }

            if (mMutes.add(localMute)) {
                updateAdd(localMute, showMessage)
                notifyObservers()
                notifyObserversDetailed(localMute)
                System.err.println("LLMuteList: LLPipeline::removeMutedAVsLights and LLNotifications::cancelByOwner not yet implemented")
                return true
            }
        }
        return false
    }

    fun remove(mute: LLMute, flags: UInt = 0u): Boolean {
        var found = false
        val existing = mMutes.find { it.id == mute.id }
        if (existing != null) {
            val localMute = LLMute(existing.id, existing.name, existing.type, existing.flags)
            var doRemove = true
            if (flags != 0u) {
                localMute.flags = localMute.flags or flags
                if (localMute.flags == LLMute.FLAG_ALL) {
                    // all properties unblocked -> fully remove
                } else {
                    doRemove = false
                }
            } else {
                localMute.flags = LLMute.FLAG_ALL
            }

            mMutes.remove(existing)
            if (doRemove) {
                updateRemove(localMute)
            } else {
                mMutes.add(localMute)
                updateAdd(localMute, false)
            }
            notifyObservers()
            notifyObserversDetailed(localMute)
            found = true
        } else {
            if (mLegacyMutes.remove(mute.name)) {
                val legacyMute = LLMute(UUID(0, 0), mute.name, LLMute.EType.BY_NAME)
                updateRemove(legacyMute)
                notifyObservers()
                notifyObserversDetailed(legacyMute)
                found = true
            }
        }
        return found
    }

    fun autoRemove(agentId: UUID, reason: EAutoReason): Boolean {
        if (isMuted(agentId)) {
            val automute = LLMute(agentId, "", LLMute.EType.AGENT)
            remove(automute)
            System.err.println("LLMuteList: LLAvatarNameCache::get and notify_automute_callback not yet implemented")
            return true
        }
        return false
    }

    fun isMuted(id: UUID, name: String = "", flags: UInt = 0u): Boolean {
        if (mMutes.isEmpty() && mLegacyMutes.isEmpty()) return false

        System.err.println("LLMuteList: get_object_to_mute_from_id and getID() not yet implemented")
        val idToCheck: UUID = id

        System.err.println("LLMuteList: gAgentID not yet implemented")
        val agentId: UUID = UUID(0, 0)
        if (idToCheck == agentId) return false

        val found = mMutes.find { it.id == idToCheck }
        if (found != null) {
            if (flags and found.flags != 0u) return false
            return true
        }

        if (name.isEmpty()) return false
        return mLegacyMutes.contains(name)
    }

    fun isMuted(id: UUID, flags: UInt): Boolean = isMuted(id, "", flags)

    fun isMuted(username: String, flags: UInt = 0u): Boolean {
        return mMutes.any { it.type == LLMute.EType.AGENT && buildUsername(it.name) == username }
    }

    fun isLinden(name: String): Boolean {
        val normalised = name.replace(".", " ")
        val parts = normalised.trim().split(" ")
        if (parts.size < 2) return false
        return parts.last().lowercase() == "linden"
    }

    fun getMutes(): List<LLMute> {
        val result = mutableListOf<LLMute>()
        result.addAll(mMutes)
        mLegacyMutes.forEach { result.add(LLMute(UUID(0, 0), it)) }
        result.sortWith(compareBy { it.name.uppercase() })
        return result
    }

    fun requestFromServer(agentId: UUID) {
        System.err.println("LLMuteList: LLMessageSystem MuteListRequest and LLDir cache path not yet implemented")
    }

    fun cache(agentId: UUID) {
        if (isLoaded()) {
            System.err.println("LLMuteList: gDirUtilp->getExpandedFilename and saveToFile not yet implemented")
        }
    }

    private fun loadFromFile(filename: String): Boolean {
        if (filename.isEmpty()) {
            mLoadState = EMuteListState.ML_FAILED
            return false
        }
        System.err.println("LLMuteList: file I/O: parse mute list lines, populate mMutes/mLegacyMutes, call setLoaded() not yet implemented")
        return false
    }

    private fun saveToFile(filename: String): Boolean {
        if (filename.isEmpty()) return false
        System.err.println("LLMuteList: file I/O: write mLegacyMutes then mMutes in legacy format not yet implemented")
        return false
    }

    private fun setLoaded() {
        mLoadState = EMuteListState.ML_LOADED
        notifyObservers()
    }

    private fun notifyObservers() {
        val snapshot = mObservers.toList()
        snapshot.forEach { it.onChange() }
    }

    private fun notifyObserversDetailed(mute: LLMute) {
        val snapshot = mObservers.toList()
        snapshot.forEach { it.onChangeDetailed(mute) }
    }

    private fun updateAdd(mute: LLMute, showMessage: Boolean = true) {
        if (mute.type == LLMute.EType.EXTERNAL) return
        System.err.println("LLMuteList: gMessageSystem UpdateMuteListEntry and gAgent.sendReliableMessage() not yet implemented")
    }

    private fun updateRemove(mute: LLMute) {
        if (mute.type == LLMute.EType.EXTERNAL) return
        System.err.println("LLMuteList: gMessageSystem RemoveMuteListEntry and gAgent.sendReliableMessage() not yet implemented")
    }

    fun processMuteListUpdate() {
        System.err.println("LLMuteList: LLMessageSystem MuteListUpdate handler and xfer manager file request not yet implemented")
    }

    fun processUseCachedMuteList() {
        System.err.println("LLMuteList: loading cached mute file from LL_PATH_CACHE not yet implemented")
    }

    fun onFileMuteList(localFilename: String?, errorCode: Int) {
        if (localFilename != null && localFilename.isNotEmpty() && errorCode == 0) {
            loadFromFile(localFilename)
            System.err.println("LLMuteList: LLFile::remove after loading not yet implemented")
        } else {
            mLoadState = EMuteListState.ML_FAILED
        }
    }

    fun onAccountNameChanged(id: UUID, username: String) {
        if (isLoaded()) {
            val existing = mMutes.find { it.id == id && it.type == LLMute.EType.AGENT }
            if (existing != null && existing.name != username) {
                val updated = LLMute(id, username, LLMute.EType.AGENT, existing.flags)
                mMutes.remove(existing)
                if (mMutes.add(updated)) {
                    updateAdd(updated)
                }
            }
        } else {
            mPendingAgentNameUpdates[id] = username
        }
    }

    private fun buildUsername(displayName: String): String =
        displayName.trim().lowercase().replace(" ", ".")
}
