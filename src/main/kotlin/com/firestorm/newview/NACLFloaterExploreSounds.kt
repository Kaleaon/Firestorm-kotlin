package com.firestorm.newview

import java.util.UUID

// ============================================================================
// Collision sound asset UUIDs — 28 well-known SL physics impact sounds
// ============================================================================

private val COLLISION_SOUNDS: Set<UUID> = setOf(
    UUID.fromString("dce5fdd4-afe4-4ea1-822f-dd52cac46b08"),
    UUID.fromString("51011582-fbca-4580-ae9e-1a5593f094ec"),
    UUID.fromString("68d62208-e257-4d0c-bbe2-20c9ea9760bb"),
    UUID.fromString("75872e8c-bc39-451b-9b0b-042d7ba36cba"),
    UUID.fromString("6a45ba0b-5775-4ea8-8513-26008a17f873"),
    UUID.fromString("992a6d1b-8c77-40e0-9495-4098ce539694"),
    UUID.fromString("2de4da5a-faf8-46be-bac6-c4d74f1e5767"),
    UUID.fromString("6e3fb0f7-6d9c-42ca-b86b-1122ff562d7d"),
    UUID.fromString("14209133-4961-4acc-9649-53fc38ee1667"),
    UUID.fromString("bc4a4348-cfcc-4e5e-908e-8a52a8915fe6"),
    UUID.fromString("9e5c1297-6eed-40c0-825a-d9bcd86e3193"),
    UUID.fromString("e534761c-1894-4b61-b20c-658a6fb68157"),
    UUID.fromString("8761f73f-6cf9-4186-8aaa-0948ed002db1"),
    UUID.fromString("874a26fd-142f-4173-8c5b-890cd846c74d"),
    UUID.fromString("0e24a717-b97e-4b77-9c94-b59a5a88b2da"),
    UUID.fromString("75cf3ade-9a5b-4c4d-bb35-f9799bda7fb2"),
    UUID.fromString("153c8bf7-fb89-4d89-b263-47e58b1b4774"),
    UUID.fromString("55c3e0ce-275a-46fa-82ff-e0465f5e8703"),
    UUID.fromString("24babf58-7156-4841-9a3f-761bdbb8e237"),
    UUID.fromString("aca261d8-e145-4610-9e20-9eff990f2c12"),
    UUID.fromString("0642fba6-5dcf-4d62-8e7b-94dbb529d117"),
    UUID.fromString("25a863e8-dc42-4e8a-a357-e76422ace9b5"),
    UUID.fromString("9538f37c-456e-4047-81be-6435045608d4"),
    UUID.fromString("8c0f84c3-9afd-4396-b5f5-9bca2c911c20"),
    UUID.fromString("be582e5d-b123-41a2-a150-454c39e961c8"),
    UUID.fromString("c70141d4-ba06-41ea-bcbc-35ea81cb8335"),
    UUID.fromString("7d1826f4-24c4-4aac-8c2e-eff45df37783"),
    UUID.fromString("063c97d3-033a-4e9b-98d8-05c8074922cb")
)

// ============================================================================
// LLSoundHistoryItem — view-model for one entry in the sound history list
// ============================================================================

data class LLSoundHistoryItem(
    val id:               UUID    = UUID(0, 0),
    val assetId:          UUID    = UUID(0, 0),
    val sourceId:         UUID    = UUID(0, 0),
    val ownerId:          UUID    = UUID(0, 0),
    val position:         Vector3d = Vector3d.ZERO,
    val type:             Int     = 0,
    var isPlaying:        Boolean = false,
    val isTrigger:        Boolean = false,
    val isLooped:         Boolean = false,
    val timeStarted:      Double  = 0.0,
    var timeStopped:      Double  = 0.0,
    var reviewed:         Boolean = false,
    var reviewedCollision:Boolean = false
)

// Minimal 3-D double vector (replaces LLVector3d)
data class Vector3d(val x: Double, val y: Double, val z: Double) {
    companion object { val ZERO = Vector3d(0.0, 0.0, 0.0) }
    operator fun minus(other: Vector3d) = Vector3d(x - other.x, y - other.y, z - other.z)
    operator fun plus(other: Vector3d)  = Vector3d(x + other.x, y + other.y, z + other.z)
    operator fun times(scale: Double)   = Vector3d(x * scale, y * scale, z * scale)
    fun normalize(): Vector3d {
        val len = Math.sqrt(x * x + y * y + z * z)
        return if (len > 0.0) Vector3d(x / len, y / len, z / len) else ZERO
    }
}

// ============================================================================
// FSAssetBlacklist.eBlacklistFlag — mirrored enum (subset used here)
// ============================================================================

enum class FSBlacklistFlag { NONE, WORN, REZZED, GESTURE }

// ============================================================================
// NACLFloaterExploreSounds — floater that lists all sounds heard in the region
// and lets the user play, stop, look at, or blacklist them.
//
// LLFloater and LLEventTimer inheritance is replaced by abstract UI hooks;
// implementations provide the concrete widget layer.
// ============================================================================

abstract class NACLFloaterExploreSounds {

    // Tick interval: 0.25 s (matches LLEventTimer(0.25f))
    val tickIntervalSeconds: Float = 0.25f

    // UI widget references — populated by postBuild()
    protected var historyScroller:  ScrollListCtrl? = null
    protected var collisionSounds:  CheckBoxCtrl?   = null
    protected var repeatedAssets:   CheckBoxCtrl?   = null
    protected var avatarSounds:     CheckBoxCtrl?   = null
    protected var objectSounds:     CheckBoxCtrl?   = null
    protected var paused:           CheckBoxCtrl?   = null

    private var lastHistory: MutableList<LLSoundHistoryItem> = mutableListOf()
    private val localPlayingAudioSourceIds: MutableList<UUID> = mutableListOf()

    // Nullable lambda slots replacing boost::signals2::connection
    private val blacklistAvatarNameCacheConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    open fun postBuild(): Boolean {
        historyScroller = findScrollList("sound_list")
        historyScroller?.setCommitCallback    { handleSelection() }
        historyScroller?.setDoubleClickCallback { handlePlayLocally() }
        historyScroller?.sortByColumn("playing", ascending = true)

        collisionSounds = findCheckBox("collision_chk")
        repeatedAssets  = findCheckBox("repeated_asset_chk")
        avatarSounds    = findCheckBox("avatars_chk")
        objectSounds    = findCheckBox("objects_chk")
        paused          = findCheckBox("pause_chk")

        setButtonClickCallback("play_locally_btn")              { handlePlayLocally() }
        setButtonClickCallback("look_at_btn")                   { handleLookAt() }
        setButtonClickCallback("stop_btn")                      { handleStop() }
        setButtonClickCallback("stop_locally_btn")              { handleStopLocally() }
        setButtonClickCallback("bl_btn")                        { blacklistSound(FSBlacklistFlag.NONE) }
        setButtonClickCallback("block_avatar_worn_sounds_btn")  { blacklistSound(FSBlacklistFlag.WORN) }
        setButtonClickCallback("block_avatar_rezzed_sounds_btn"){ blacklistSound(FSBlacklistFlag.REZZED) }
        setButtonClickCallback("block_avatar_gesture_sounds_btn"){ blacklistSound(FSBlacklistFlag.GESTURE) }

        return true
    }

    fun dispose() {
        blacklistAvatarNameCacheConnections.values.forEach { it?.invoke() }
        blacklistAvatarNameCacheConnections.clear()
        localPlayingAudioSourceIds.clear()
    }

    // -------------------------------------------------------------------------
    // Timer tick — rebuild the scroll list from current sound history
    // -------------------------------------------------------------------------

    fun tick(): Boolean {
        val strPlaying        = getString("Playing")
        val strNotPlaying     = getString("NotPlaying")
        val strTypeUi         = getString("Type_UI")
        val strTypeAvatar     = getString("Type_Avatar")
        val strTypeTrigger    = getString("Type_llTriggerSound")
        val strTypeLoop       = getString("Type_llLoopSound")
        val strTypePlay       = getString("Type_llPlaySound")
        val strUnknownName    = getString("AvatarNameWaiting")

        val showCollision   = collisionSounds?.isChecked() ?: true
        val showRepeated    = repeatedAssets?.isChecked()  ?: true
        val showAvatars     = avatarSounds?.isChecked()    ?: true
        val showObjects     = objectSounds?.isChecked()    ?: true

        val history: MutableList<LLSoundHistoryItem> = if (paused?.isChecked() == true) {
            lastHistory.toMutableList()
        } else {
            val live = getSoundHistory().values.toMutableList()
            live.sortWith(LLSoundHistoryItemComparator)
            lastHistory = live
            live
        }

        // Preserve scroll position and selection across rebuilds
        val scrollPos    = historyScroller?.getScrollPos() ?: 0
        val selectedIds  = historyScroller?.getSelectedIds() ?: emptyList()
        historyScroller?.clearRows()

        val uniqueAssets = mutableSetOf<UUID>()

        for (item in history) {
            val isAvatar = item.ownerId == item.sourceId

            if (isAvatar && !showAvatars)  continue
            if (!isAvatar && !showObjects) continue

            val isRepeated = item.assetId in uniqueAssets
            if (isRepeated && !showRepeated) continue

            if (!item.reviewed) {
                item.reviewed          = true
                item.reviewedCollision = item.assetId in COLLISION_SOUNDS
            }
            if (item.reviewedCollision && !showCollision) continue

            uniqueAssets += item.assetId

            val playingText = if (item.isPlaying) {
                " $strPlaying"
            } else {
                val minutesAgo = (elapsedSeconds() - item.timeStopped) / 60.0
                strNotPlaying.replace("{TIME}", "%.1f".format(minutesAgo))
            }

            val typeText = when {
                item.type == AUDIO_TYPE_UI  -> strTypeUi
                isAvatar                    -> strTypeAvatar
                item.isTrigger              -> strTypeTrigger
                item.isLooped               -> strTypeLoop
                else                        -> strTypePlay
            }

            val ownerName = resolveAvatarName(item.ownerId)?.let { avName ->
                if (gRlvHandler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWNAMES))
                    rlvAnonymise(avName)
                else
                    avName
            } ?: strUnknownName

            val assetShort = item.assetId.toString().substring(0, 16)

            historyScroller?.addRow(
                rowId    = item.id,
                playing  = playingText,
                type     = typeText,
                owner    = ownerName,
                sound    = assetShort
            )
        }

        historyScroller?.restoreSelection(selectedIds)
        historyScroller?.setScrollPos(scrollPos)

        // Remove audio source IDs whose sources have finished playing
        val iter = localPlayingAudioSourceIds.iterator()
        while (iter.hasNext()) {
            val srcId = iter.next()
            if (isAudioSourceDone(srcId)) iter.remove()
        }
        setChildEnabled("stop_locally_btn", localPlayingAudioSourceIds.isNotEmpty())

        return false // false means "keep ticking"
    }

    // -------------------------------------------------------------------------
    // Item lookup — checks live history first, then the paused snapshot
    // -------------------------------------------------------------------------

    fun getItem(itemId: UUID): LLSoundHistoryItem {
        getSoundHistory()[itemId]?.let { return it }
        return lastHistory.firstOrNull { it.id == itemId } ?: LLSoundHistoryItem()
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    private fun handlePlayLocally() {
        val assetsSeen = mutableSetOf<UUID>()
        for (selectedId in historyScroller?.getSelectedIds() ?: emptyList()) {
            val item = getItem(selectedId)
            if (item.id == UUID(0, 0)) continue
            if (item.assetId in assetsSeen) continue
            assetsSeen += item.assetId

            val audioSrcId = UUID.randomUUID()
            triggerUiSound(item.assetId, audioSrcId)
            localPlayingAudioSourceIds += audioSrcId
        }
        setChildEnabled("stop_locally_btn", localPlayingAudioSourceIds.isNotEmpty())
    }

    private fun handleLookAt() {
        val selection = historyScroller?.getSingleSelectedId() ?: return
        val item = getItem(selection)
        if (item.id == UUID(0, 0)) return

        var targetPos = item.position

        // Prefer the live object position when available
        getObjectPosition(item.sourceId)?.let { targetPos = it }

        // Place camera 4 m behind and 3 m above the target
        val agentPos = getAgentGlobalPosition()
        val dir = (agentPos - targetPos).normalize() * 4.0
        val camPos = dir + targetPos + Vector3d(0.0, 0.0, 3.0)

        TODO("GPU: gAgentCamera.setFocusOnAvatar(false, false); setCameraPosAndFocusGlobal(camPos, targetPos, item.sourceId); setCameraAnimating(false)")
    }

    private fun handleStop() {
        for (selectedId in historyScroller?.getSelectedIds() ?: emptyList()) {
            val item = getItem(selectedId)
            if (item.id == UUID(0, 0) || !item.isPlaying) continue

            val stopped = stopAudioSource(item.sourceId, item.type)
            if (!stopped) {
                // Audio source already gone but still marked playing — fix the record
                val liveEntry = getSoundHistory()[item.id]
                if (liveEntry != null) {
                    liveEntry.isPlaying    = false
                    liveEntry.timeStopped  = elapsedSeconds()
                } else {
                    lastHistory.firstOrNull { it.id == item.id }?.let {
                        it.isPlaying   = false
                        it.timeStopped = elapsedSeconds()
                    }
                }
            }
        }
    }

    private fun handleStopLocally() {
        localPlayingAudioSourceIds.forEach { stopLocalAudioSource(it) }
        localPlayingAudioSourceIds.clear()
    }

    private fun handleSelection() {
        val count    = historyScroller?.getSelectedCount() ?: 0
        val multiple = count > 1
        setChildEnabled("look_at_btn",                    count > 0 && !multiple)
        setChildEnabled("play_locally_btn",               count > 0)
        setChildEnabled("stop_btn",                       count > 0)
        setChildEnabled("bl_btn",                         count > 0)
        setChildEnabled("block_avatar_worn_sounds_btn",   count > 0)
        setChildEnabled("block_avatar_rezzed_sounds_btn", count > 0)
        setChildEnabled("block_avatar_gesture_sounds_btn",count > 0)
    }

    private fun blacklistSound(flag: FSBlacklistFlag) {
        for (selectedId in historyScroller?.getSelectedIds() ?: emptyList()) {
            val item = getItem(selectedId)
            if (item.id == UUID(0, 0)) continue

            val regionName = getAgentRegionName()

            val avName = resolveAvatarName(item.ownerId)
            if (avName != null) {
                val targetId = if (flag == FSBlacklistFlag.NONE) item.assetId else item.ownerId
                addToBlacklist(targetId, avName, regionName, flag)
            } else {
                // Unique request UUID so the same owner's multiple sounds each get their own callback
                val requestId = UUID.randomUUID()
                val slot = scheduleAvatarNameCallback(item.ownerId) { avId, resolvedName ->
                    onBlacklistAvatarNameCacheCallback(requestId, avId, resolvedName, item.assetId, regionName, flag)
                }
                blacklistAvatarNameCacheConnections[requestId] = slot
            }
        }
    }

    private fun onBlacklistAvatarNameCacheCallback(
        requestId:  UUID,
        avId:       UUID,
        avName:     String,
        assetId:    UUID,
        regionName: String,
        flag:       FSBlacklistFlag
    ) {
        blacklistAvatarNameCacheConnections.remove(requestId)?.invoke()
        val targetId = if (flag == FSBlacklistFlag.NONE) assetId else avId
        addToBlacklist(targetId, avName, regionName, flag)
    }

    // -------------------------------------------------------------------------
    // Abstract / platform stubs — implementations supply the viewer layer
    // -------------------------------------------------------------------------

    protected abstract fun findScrollList(name: String): ScrollListCtrl?
    protected abstract fun findCheckBox(name: String): CheckBoxCtrl?
    protected abstract fun setButtonClickCallback(name: String, cb: () -> Unit)
    protected abstract fun setChildEnabled(name: String, enabled: Boolean)
    protected abstract fun getString(key: String): String

    protected abstract fun getSoundHistory(): MutableMap<UUID, LLSoundHistoryItem>
    protected abstract fun elapsedSeconds(): Double
    protected abstract fun getAgentGlobalPosition(): Vector3d
    protected abstract fun getObjectPosition(objectId: UUID): Vector3d?
    protected abstract fun getAgentRegionName(): String
    protected abstract fun resolveAvatarName(agentId: UUID): String?
    protected abstract fun rlvAnonymise(name: String): String
    protected abstract fun isAudioSourceDone(audioSrcId: UUID): Boolean

    protected abstract fun triggerUiSound(assetId: UUID, audioSrcId: UUID)
    protected abstract fun stopAudioSource(sourceId: UUID, type: Int): Boolean
    protected abstract fun stopLocalAudioSource(audioSrcId: UUID)

    protected abstract fun addToBlacklist(targetId: UUID, ownerName: String, regionName: String, flag: FSBlacklistFlag)
    protected abstract fun scheduleAvatarNameCallback(agentId: UUID, cb: (UUID, String) -> Unit): (() -> Unit)?

    // -------------------------------------------------------------------------
    // Minimal widget interfaces (replaces LL UI class pointers)
    // -------------------------------------------------------------------------

    interface ScrollListCtrl {
        fun setCommitCallback(cb: () -> Unit)
        fun setDoubleClickCallback(cb: () -> Unit)
        fun sortByColumn(column: String, ascending: Boolean)
        fun clearRows()
        fun addRow(rowId: UUID, playing: String, type: String, owner: String, sound: String)
        fun getScrollPos(): Int
        fun setScrollPos(pos: Int)
        fun getSelectedIds(): List<UUID>
        fun getSingleSelectedId(): UUID?
        fun getSelectedCount(): Int
        fun restoreSelection(ids: List<UUID>)
    }

    interface CheckBoxCtrl {
        fun isChecked(): Boolean
    }

    // -------------------------------------------------------------------------
    // Comparator — currently-playing items before stopped ones; within each
    // group sort by most-recently-started or most-recently-stopped.
    // -------------------------------------------------------------------------

    private object LLSoundHistoryItemComparator : Comparator<LLSoundHistoryItem> {
        override fun compare(a: LLSoundHistoryItem, b: LLSoundHistoryItem): Int {
            if (a.isPlaying && b.isPlaying)  return b.timeStarted.compareTo(a.timeStarted)
            if (a.isPlaying)                 return -1
            if (b.isPlaying)                 return  1
            return b.timeStopped.compareTo(a.timeStopped)
        }
    }

    companion object {
        private const val AUDIO_TYPE_UI = 0
    }
}
