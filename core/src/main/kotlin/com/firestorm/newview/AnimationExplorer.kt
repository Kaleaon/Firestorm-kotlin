package com.firestorm.newview

import java.util.ArrayDeque
import java.util.UUID

data class AnimationEntry(
    val animationID: UUID,
    val playedBy: UUID,
    val time: Double
)

object RecentAnimationList {

    private const val MAX_ANIMATIONS = 100

    val animationList: ArrayDeque<AnimationEntry> = ArrayDeque()

    fun addAnimation(id: UUID, playedBy: UUID) {
        val entry = AnimationEntry(
            animationID = id,
            playedBy = playedBy,
            time = elapsedSeconds()
        )

        val explorer = FloaterRegistry.findTypedInstance<AnimationExplorer>("animation_explorer")

        if (playedBy != agentAvatarId() || explorer != null) {
            animationList.addLast(entry)
            if (animationList.size > MAX_ANIMATIONS) {
                animationList.removeFirst()
            }
        }

        explorer?.addAnimation(id, playedBy, elapsedSeconds())
    }

    fun requestList(explorer: AnimationExplorer) {
        for (entry in animationList) {
            explorer.addAnimation(entry.animationID, entry.playedBy, entry.time)
        }
    }

    private fun elapsedSeconds(): Double = 0.0
    private fun agentAvatarId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
}

class AnimationExplorer(key: LLSD) : LLFloater(key) {

    private var animationScrollList: LLScrollListCtrl? = null
    private var stopButton: LLButton? = null
    private var blacklistButton: LLButton? = null
    private var stopAndRevokeButton: LLButton? = null
    private var noOwnedAnimationsCheckBox: LLCheckBoxCtrl? = null

    private var previewCtrl: LLView? = null
    private var animationPreview: LLPreviewAnimation? = null

    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0

    private var currentAnimationID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private var currentObject: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val requestedIDs: MutableList<UUID> = mutableListOf()
    private val knownIDs: MutableMap<UUID, String> = mutableMapOf()
    private val avatarNameCacheConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()

    override fun postBuild(): Boolean {
        animationScrollList = getChild("animation_list")
        stopButton = getChild("stop_btn")
        blacklistButton = getChild("blacklist_btn")
        stopAndRevokeButton = getChild("stop_and_revoke_btn")
        noOwnedAnimationsCheckBox = getChild("no_owned_animations_check")

        animationScrollList?.setCommitCallback { onSelectAnimation() }
        stopButton?.setCommitCallback { onStopPressed() }
        blacklistButton?.setCommitCallback { onBlacklistPressed() }
        stopAndRevokeButton?.setCommitCallback { onStopAndRevokePressed() }
        noOwnedAnimationsCheckBox?.setCommitCallback { onOwnedCheckToggled() }

        previewCtrl = findChild("animation_preview")
        if (previewCtrl != null) {
            if (isAgentAvatarValid()) {
                animationPreview = LLPreviewAnimation(
                    previewCtrl!!.rect.width,
                    previewCtrl!!.rect.height
                )
                animationPreview!!.setZoom(2.0f)
                startMotion(null)
            }
        } else {
            return false
        }

        update()
        return true
    }

    fun addAnimation(id: UUID, playedBy: UUID, time: Double) {
        if (playedBy == agentAvatarId()) {
            if (noOwnedAnimationsCheckBox?.value?.asBoolean() == true) {
                return
            }
        }

        var playedByName = playedBy.toString()

        val vo = objectList.findObject(playedBy)
        if (vo != null) {
            if (vo.isAvatar()) {
                val avName = avatarNameCache.get(playedBy)
                if (avName != null) {
                    playedByName = avName.getCompleteName()
                } else {
                    if (!avatarNameCacheConnections.containsKey(playedBy)) {
                        avatarNameCacheConnections[playedBy] = avatarNameCache.getAsync(playedBy) { avId, avN ->
                            onAvatarNameCallback(avId, avN)
                        }
                    }
                    playedByName = trans("AvatarNameWaiting")
                }
            } else {
                if (!knownIDs.containsKey(playedBy)) {
                    if (!requestedIDs.contains(playedBy)) {
                        requestedIDs.add(playedBy)
                        System.err.println("AnimationExplorer: object name lookup not yet implemented")
                    }
                } else {
                    playedByName = knownIDs[playedBy]!!
                }
            }
        }

        val item = ScrollListItem(
            columns = mapOf(
                "played_by" to playedByName,
                "played" to trans("animation_explorer_still_playing"),
                "priority" to trans("animation_explorer_unknown_priority"),
                "timestamp" to time,
                "animation_id" to id,
                "object_id" to playedBy
            )
        )
        animationScrollList?.addElement(item, AddPosition.TOP)
    }

    private fun onAvatarNameCallback(id: UUID, avName: AvatarName) {
        avatarNameCacheConnections[id]?.invoke()
        avatarNameCacheConnections.remove(id)
        updateListEntry(id, avName.getCompleteName())
    }

    fun requestNameCallback(msg: MessageSystem) {
        if (requestedIDs.isEmpty()) return

        val count = msg.getNumberOfBlocks("ObjectData")
        for (index in 0 until count) {
            val objectId = msg.getUUID("ObjectData", "ObjectID", index)
            if (requestedIDs.contains(objectId)) {
                val objectName = msg.getString("ObjectData", "Name", index)
                requestedIDs.remove(objectId)
                knownIDs[objectId] = objectName
                updateListEntry(objectId, objectName)
            }
        }
    }

    private fun updateListEntry(id: UUID, name: String) {
        val objectIdColumn = animationScrollList?.getColumnIndex("object_id") ?: return
        val playedByColumn = animationScrollList?.getColumnIndex("played_by") ?: return

        animationScrollList?.getAllData()?.forEach { item ->
            val listObjectId = item.getColumn(objectIdColumn).valueAsUUID()
            if (id == listObjectId) {
                item.getColumn(playedByColumn).setText(name)
            }
        }
    }

    override fun draw() {
        super.draw()
        val r = previewCtrl?.rect ?: return

        if (animationPreview != null) {
            animationPreview!!.requestUpdate()
            // no-op
        }

        val time = elapsedSeconds()
        if (time - lastUpdateTimestamp > 5.0) {
            lastUpdateTimestamp = time
            updateList(time)
        }
    }

    private var lastUpdateTimestamp: Double = 0.0

    private fun update() {
        startMotion(null)
        animationScrollList?.deleteAllItems()
        RecentAnimationList.requestList(this)
    }

    private fun updateList(currentTimestamp: Double) {
        val playedColumn = animationScrollList?.getColumnIndex("played") ?: return
        val timestampColumn = animationScrollList?.getColumnIndex("timestamp") ?: return
        val priorityColumn = animationScrollList?.getColumnIndex("priority") ?: return
        val objectIdColumn = animationScrollList?.getColumnIndex("object_id") ?: return
        val animIdColumn = animationScrollList?.getColumnIndex("animation_id") ?: return

        animationScrollList?.getAllData()?.forEach { item ->
            val objectId = item.getColumn(objectIdColumn).valueAsUUID()
            val animId = item.getColumn(animIdColumn).valueAsUUID()

            val isRunning = agentAvatarAnimationSources().any { (srcObject, srcAnim) ->
                srcObject == objectId && srcAnim == animId
            }

            if (isRunning) {
                item.getColumn(playedColumn).setText(trans("animation_explorer_still_playing"))
            } else {
                val timestamp = item.getColumn(timestampColumn).valueAsDouble()
                val seconds = (currentTimestamp - timestamp).toInt()
                item.getColumn(playedColumn).setText(
                    trans("animation_explorer_seconds_ago", mapOf("SECONDS" to "$seconds"))
                )
            }

            val prioText = agentAvatarFindMotion(animId)?.let { motion ->
                "${motion.getPriority()}"
            } ?: trans("animation_explorer_unknown_priority")
            item.getColumn(priorityColumn).setText(prioText)
        }
    }

    private fun startMotion(motionId: UUID?) {
        val preview = animationPreview ?: return
        val avatar = preview.getDummyAvatar()
        avatar.deactivateAllMotions()
        avatar.startMotion(ANIM_AGENT_STAND, 0.0f)
        if (motionId != null) {
            avatar.startMotion(motionId, 0.0f)
        }
    }

    private fun onSelectAnimation() {
        val item = animationScrollList?.getFirstSelected() ?: return
        currentAnimationID = item.getColumn(animationScrollList!!.getColumnIndex("animation_id")).valueAsUUID()
        currentObject = item.getColumn(animationScrollList!!.getColumnIndex("object_id")).valueAsUUID()
        startMotion(currentAnimationID)
    }

    private fun onStopPressed() {
        if (currentAnimationID != NULL_UUID) {
            agentAvatarStopMotion(currentAnimationID)
            agentSendAnimationRequest(currentAnimationID, ANIM_REQUEST_STOP)
        }
    }

    private fun onBlacklistPressed() {
        onStopPressed()
        val item = animationScrollList?.getFirstSelected() ?: return
        val regionName = agentRegionName()
        FSAssetBlacklist.getInstance().addNewItemToBlacklist(
            currentAnimationID,
            item.getColumn(animationScrollList!!.getColumnIndex("played_by")).value(),
            regionName,
            AssetType.AT_ANIMATION
        )
    }

    private fun onStopAndRevokePressed() {
        onStopPressed()
        if (currentObject != NULL_UUID) {
            val vo = objectList.findObject(currentObject)
            if (vo != null) {
                agentAvatarRevokePermissionsOnObject(vo)
            }
        }
    }

    private fun onOwnedCheckToggled() {
        update()
        updateList(elapsedSeconds())
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (previewCtrl != null && previewCtrl!!.rect.contains(x, y)) {
            bringToFront(x, y)
            System.err.println("AnimationExplorer: mouse capture / hide cursor not yet implemented")
            lastMouseX = x
            lastMouseY = y
            return true
        }
        return super.handleMouseDown(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("AnimationExplorer: release mouse capture / show cursor not yet implemented")
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (previewCtrl == null || animationPreview == null || !previewCtrl!!.rect.contains(x, y)) {
            return super.handleHover(x, y, mask)
        }

        val localMask = mask and MASK_ALT.inv()
        if (hasMouseCapture()) {
            when {
                localMask == MASK_PAN -> {
                    animationPreview!!.pan((x - lastMouseX) * -0.005f, (y - lastMouseY) * -0.005f)
                }
                localMask == MASK_ORBIT -> {
                    val yawRadians = (x - lastMouseX) * -0.01f
                    val pitchRadians = (y - lastMouseY) * 0.02f
                    animationPreview!!.rotate(yawRadians, pitchRadians)
                }
                else -> {
                    val yawRadians = (x - lastMouseX) * -0.01f
                    val zoomAmt = (y - lastMouseY) * 0.02f
                    animationPreview!!.rotate(yawRadians, 0.0f)
                    animationPreview!!.zoom(zoomAmt)
                }
            }
            animationPreview!!.requestUpdate()
            System.err.println("AnimationExplorer: setMousePositionLocal not yet implemented")
        } else {
            System.err.println("AnimationExplorer: cursor change not yet implemented")
        }
        return true
    }

    override fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (previewCtrl != null && previewCtrl!!.rect.contains(x, y)) {
            animationPreview!!.zoom(clicks * -0.2f)
            animationPreview!!.requestUpdate()
            return true
        }
        return super.handleScrollWheel(x, y, clicks)
    }

    override fun onMouseCaptureLost() {
        System.err.println("AnimationExplorer: onMouseCaptureLost show cursor not yet implemented")
    }

    private fun elapsedSeconds(): Double = 0.0
    private fun agentAvatarId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private fun agentAvatarAnimationSources(): List<Pair<UUID, UUID>> = emptyList()
    private fun agentAvatarFindMotion(id: UUID): LLKeyframeMotion? = null
    private fun agentAvatarStopMotion(id: UUID): Unit { System.err.println("AnimationExplorer: agentAvatarStopMotion not yet implemented") }
    private fun agentAvatarRevokePermissionsOnObject(vo: Any): Unit { System.err.println("AnimationExplorer: agentAvatarRevokePermissionsOnObject not yet implemented") }
    private fun agentSendAnimationRequest(id: UUID, request: Int): Unit { System.err.println("AnimationExplorer: agentSendAnimationRequest not yet implemented") }
    private fun agentRegionName(): String = ""
    private fun isAgentAvatarValid(): Boolean = false
    private fun trans(key: String, args: Map<String, String> = emptyMap()): String = ""
}
