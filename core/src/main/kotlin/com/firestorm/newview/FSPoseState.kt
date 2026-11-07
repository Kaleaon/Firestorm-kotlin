package com.firestorm.newview

import java.util.UUID

class FSPoseState {

    fun captureMotionStates(avatar: Any?) {
        avatar ?: return
        val avatarId = getAvatarId(avatar)
        sCaptureOrder[avatarId] = 0
        var animNumber = 0

        for ((animId, _) in getPlayingAnimations(avatar)) {
            val motion = findKeyframeMotion(avatar, animId) ?: continue
            val newState = FsMotionState(
                motionId = animId,
                lastUpdateTime = getMotionLastUpdateTime(motion),
                captureOrder = 0,
                inLayerOrder = animNumber++,
                gAgentOwnsPose = canSaveMotionId(avatar, animId),
            )
            sMotionStates.getOrPut(avatarId) { mutableListOf() }.add(newState)
        }
    }

    fun updateMotionStates(avatar: Any?, posingMotion: Any?, jointNumbersRecaptured: MutableList<Int>) {
        avatar ?: return
        posingMotion ?: return
        val avatarId = getAvatarId(avatar)
        sCaptureOrder[avatarId] = (sCaptureOrder[avatarId] ?: 0) + 1
        var animNumber = 0

        sMotionStates[avatarId]?.removeAll { state ->
            vector2IsSubsetOfVector1(jointNumbersRecaptured, state.jointNumbersAnimated)
        }

        for ((animId, _) in getPlayingAnimations(avatar)) {
            val motion = findKeyframeMotion(avatar, animId) ?: continue
            if (!otherMotionAnimatesJoints(posingMotion, motion, jointNumbersRecaptured)) continue

            val motionTime = getMotionLastUpdateTime(motion)
            val alreadyPresent = sMotionStates[avatarId]?.any { s ->
                s.motionId == animId && s.lastUpdateTime == motionTime
            } == true
            if (alreadyPresent) continue

            val newState = FsMotionState(
                motionId = animId,
                lastUpdateTime = motionTime,
                jointNumbersAnimated = jointNumbersRecaptured.toMutableList(),
                captureOrder = sCaptureOrder[avatarId] ?: 0,
                inLayerOrder = animNumber++,
                gAgentOwnsPose = canSaveMotionId(avatar, animId),
            )
            sMotionStates.getOrPut(avatarId) { mutableListOf() }.add(newState)
        }
    }

    fun purgeMotionStates(avatar: Any?) {
        avatar ?: return
        sMotionStates[getAvatarId(avatar)]?.clear()
    }

    fun writeMotionStates(avatar: Any?, ignoreOwnership: Boolean, saveRecord: MutableMap<String, Any>) {
        avatar ?: return
        val avatarId = getAvatarId(avatar)
        var animNumber = 0

        for (state in sMotionStates[avatarId] ?: return) {
            if (!ignoreOwnership && !state.gAgentOwnsPose) {
                if (state.requeriedAssetInventory) continue
                state.gAgentOwnsPose = canSaveMotionId(avatar, state.motionId)
                state.requeriedAssetInventory = true
                if (!state.gAgentOwnsPose) continue
            }

            val key = "poseState${animNumber++}"
            saveRecord[key] = mapOf(
                "animationId" to state.motionId.toString(),
                "lastUpdateTime" to state.lastUpdateTime,
                "jointNumbersAnimated" to encodeVectorToString(state.jointNumbersAnimated),
                "captureOrder" to state.captureOrder,
                "inLayerOrder" to state.inLayerOrder,
            )
        }
    }

    fun restoreMotionStates(avatar: Any?, ignoreOwnership: Boolean, pose: Map<String, Any>) {
        avatar ?: return
        val avatarId = getAvatarId(avatar)
        sCaptureOrder[avatarId] = 0

        for ((name, controlMap) in pose) {
            if (!name.startsWith("poseState")) continue
            val map = controlMap as? Map<*, *> ?: continue

            val animIdStr = map["animationId"] as? String ?: continue
            val animId = runCatching { UUID.fromString(animIdStr) }.getOrNull() ?: continue

            val agentOwns = ignoreOwnership || canSaveMotionId(avatar, animId)
            if (ignoreOwnership) sMotionStatesOwnedByMe[animId] = true

            val newState = FsMotionState(
                motionId = animId,
                lastUpdateTime = (map["lastUpdateTime"] as? Number)?.toFloat() ?: 0f,
                jointNumbersAnimated = decodeStringToVector((map["jointNumbersAnimated"] as? String) ?: ""),
                captureOrder = (map["captureOrder"] as? Number)?.toInt() ?: 0,
                inLayerOrder = (map["inLayerOrder"] as? Number)?.toInt() ?: 0,
                gAgentOwnsPose = agentOwns,
            )

            if (newState.captureOrder > (sCaptureOrder[avatarId] ?: 0))
                sCaptureOrder[avatarId] = newState.captureOrder

            sMotionStates.getOrPut(avatarId) { mutableListOf() }.add(newState)
        }
    }

    fun applyMotionStatesToPosingMotion(avatar: Any?, posingMotion: Any?): Boolean {
        avatar ?: return false
        posingMotion ?: return false
        val avatarId = getAvatarId(avatar)
        val states = sMotionStates[avatarId] ?: return false

        states.sortWith(compareBy({ it.captureOrder }, { it.inLayerOrder }))

        var allMotionsApplied = true
        var lastCaptureOrder = 0

        for (state in states) {
            val needPriorityReset = state.captureOrder > lastCaptureOrder

            if (state.motionApplied) continue

            val kfm = findKeyframeMotion(avatar, state.motionId)
            if (kfm != null) {
                if (needPriorityReset) {
                    lastCaptureOrder = state.captureOrder
                    resetPriorityForCaptureOrder(avatar, posingMotion, lastCaptureOrder)
                }
                state.motionApplied = loadOtherMotionToBase(posingMotion, kfm, state.lastUpdateTime, state.jointNumbersAnimated)
            } else {
                startMotion(avatar, state.motionId)
                stopMotion(avatar, state.motionId)
            }

            allMotionsApplied = allMotionsApplied && state.motionApplied
        }

        return allMotionsApplied
    }

    private fun resetPriorityForCaptureOrder(avatar: Any, posingMotion: Any, captureOrder: Int) {
        val avatarId = getAvatarId(avatar)
        for (state in sMotionStates[avatarId] ?: return) {
            if (state.jointNumbersAnimated.isEmpty()) continue
            if (state.motionApplied) continue
            if (state.captureOrder != captureOrder) continue
            resetBonePriority(posingMotion, state.jointNumbersAnimated)
        }
    }

    private fun canSaveMotionId(avatarPlayingMotionId: Any, motionId: UUID): Boolean {
        if (sMotionStatesOwnedByMe[motionId] == true) return true

        if (inventoryOwnsItem(motionId)) {
            sMotionStatesOwnedByMe[motionId] = true
            return true
        }

        if (avatarIsSelf(avatarPlayingMotionId))
            return motionIdIsAgentAnimationSource(motionId)

        return motionIdIsFromPrimAgentOwnsAgentIsSittingOn(avatarPlayingMotionId, motionId)
    }

    private fun motionIdIsAgentAnimationSource(motionId: UUID): Boolean {
        System.err.println("FSPoseState: motionIdIsAgentAnimationSource not yet implemented")
        return false
    }

    private fun motionIdIsFromPrimAgentOwnsAgentIsSittingOn(avatarPlayingMotionId: Any, motionId: UUID): Boolean {
        System.err.println("FSPoseState: motionIdIsFromPrimAgentOwnsAgentIsSittingOn not yet implemented")
        return false
    }

    private fun vector2IsSubsetOfVector1(superSet: MutableList<Int>, subSet: MutableList<Int>): Boolean {
        if (superSet.isEmpty() || subSet.isEmpty()) return false
        if (superSet.size < subSet.size) return false
        return subSet.all { it in superSet }
    }

    internal fun encodeVectorToString(vector: MutableList<Int>): String {
        if (vector.isEmpty()) return ""
        val sb = StringBuilder()
        for (numberToEncode in vector) {
            if (numberToEncode > 251) continue
            var number = numberToEncode
            if (number >= 189) { sb.append('~'); number -= 189 }
            if (number >= 126) { sb.append('}'); number -= 126 }
            if (number >= 63)  { sb.append('|'); number -= 63  }
            sb.append(('?' + number).toChar())
        }
        return sb.toString()
    }

    internal fun decodeStringToVector(encoded: String): MutableList<Int> {
        val decoded = mutableListOf<Int>()
        if (encoded.isEmpty()) return decoded
        var number = 0
        for (ch in encoded) {
            if (ch < '?' || ch > '~') continue
            when (ch) {
                '~' -> { number += 189; continue }
                '}' -> { number += 126; continue }
                '|' -> { number += 63;  continue }
                else -> {
                    number += (ch - '?')
                    decoded.add(number)
                    number = 0
                }
            }
        }
        return decoded
    }

    private fun getAvatarId(avatar: Any): UUID {
        System.err.println("FSPoseState: getAvatarId not yet implemented")
        return UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
    private fun getPlayingAnimations(avatar: Any): Map<UUID, Any> {
        System.err.println("FSPoseState: getPlayingAnimations not yet implemented")
        return emptyMap()
    }
    private fun findKeyframeMotion(avatar: Any, animId: UUID): Any? {
        System.err.println("FSPoseState: findKeyframeMotion not yet implemented")
        return null
    }
    private fun getMotionLastUpdateTime(motion: Any): Float {
        System.err.println("FSPoseState: getMotionLastUpdateTime not yet implemented")
        return 0f
    }
    private fun otherMotionAnimatesJoints(posingMotion: Any, motion: Any, joints: MutableList<Int>): Boolean {
        System.err.println("FSPoseState: otherMotionAnimatesJoints not yet implemented")
        return false
    }
    private fun startMotion(avatar: Any, motionId: UUID) {
        System.err.println("FSPoseState: startMotion not yet implemented")
    }
    private fun stopMotion(avatar: Any, motionId: UUID) {
        System.err.println("FSPoseState: stopMotion not yet implemented")
    }
    private fun loadOtherMotionToBase(posingMotion: Any, kfm: Any, time: Float, joints: MutableList<Int>): Boolean {
        System.err.println("FSPoseState: loadOtherMotionToBase not yet implemented")
        return false
    }
    private fun resetBonePriority(posingMotion: Any, joints: MutableList<Int>) {
        System.err.println("FSPoseState: resetBonePriority not yet implemented")
    }
    private fun inventoryOwnsItem(motionId: UUID): Boolean {
        System.err.println("FSPoseState: inventoryOwnsItem not yet implemented")
        return false
    }
    private fun avatarIsSelf(avatar: Any): Boolean {
        System.err.println("FSPoseState: avatarIsSelf not yet implemented")
        return false
    }

    private data class FsMotionState(
        val motionId: UUID,
        var lastUpdateTime: Float = 0f,
        var motionApplied: Boolean = false,
        var requeriedAssetInventory: Boolean = false,
        var gAgentOwnsPose: Boolean = false,
        var captureOrder: Int = 0,
        var inLayerOrder: Int = 0,
        var jointNumbersAnimated: MutableList<Int> = mutableListOf(),
    )

    companion object {
        private val sMotionStates: MutableMap<UUID, MutableList<FsMotionState>> = mutableMapOf()
        private val sCaptureOrder: MutableMap<UUID, Int> = mutableMapOf()
        private val sMotionStatesOwnedByMe: MutableMap<UUID, Boolean> = mutableMapOf()
    }
}
