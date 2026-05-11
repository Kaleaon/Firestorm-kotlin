package com.firestorm.newview

import com.firestorm.agent.Agent
import com.firestorm.camera.AgentCamera
import com.firestorm.camera.FollowCamMgr
import com.firestorm.event.EventApi
import com.firestorm.math.Vector3
import com.firestorm.math.Vector3d
import com.firestorm.math.Quaternion
import com.firestorm.object.ViewerObject
import com.firestorm.object.ViewerObjectList
import com.firestorm.avatar.VOAvatar
import com.firestorm.avatar.Character
import com.firestorm.avatar.AvatarNameCache
import com.firestorm.inventory.InventoryModel
import com.firestorm.inventory.ViewerInventoryItem
import com.firestorm.inventory.InventoryType
import com.firestorm.motion.Motion
import com.firestorm.camera.ViewerCamera
import com.firestorm.control.ViewerControl
import java.util.UUID

class AgentListener(private val agent: Agent) : EventApi("LLAgent", "LLAgent listener to (e.g.) teleport, sit, stand, etc.") {

    private var followTarget: UUID? = null

    init {
        add("requestTeleport", ::requestTeleport)
        add("requestSit", ::requestSit)
        add("requestStand", ::requestStand)
        add("requestTouch", ::requestTouch)
        add("resetAxes", ::resetAxes)
        add("getPosition", ::getPosition)
        add("startAutoPilot", ::startAutoPilot)
        add("getAutoPilot", ::getAutoPilot)
        add("startFollowPilot", ::startFollowPilot)
        add("setAutoPilotTarget", ::setAutoPilotTarget)
        add("stopAutoPilot", ::stopAutoPilot)
        add("lookAt", ::lookAt)
        add("getGroups", ::getGroups)
        add("setCameraParams", ::setFollowCamParams)
        add("setFollowCamActive", ::setFollowCamActive)
        add("removeCameraParams", ::removeFollowCamParams)
        add("playAnimation", ::playAnimation)
        add("stopAnimation", ::stopAnimation)
        add("getAnimationInfo", ::getAnimationInfo)
        add("getID", ::getID)
        add("getNearbyAvatarsList", ::getNearbyAvatarsList)
        add("getNearbyObjectsList", ::getNearbyObjectsList)
        add("getAgentScreenPos", ::getAgentScreenPos)
    }

    private fun requestTeleport(eventData: Map<String, Any?>) {
        val skipConfirmation = eventData["skip_confirmation"] as? Boolean ?: false
        if (skipConfirmation) {
            val params = listOf(
                eventData["regionname"],
                eventData["x"],
                eventData["y"],
                eventData["z"],
            )
            CommandDispatcher.dispatch(
                "teleport", params, emptyMap(), gridId = "", navType = CommandHandler.NAV_TYPE_CLICKED, trustedBrowser = true
            )
        } else {
            val regionName = eventData["regionname"] as? String ?: ""
            val x = (eventData["x"] as? Number)?.toFloat() ?: 0f
            val y = (eventData["y"] as? Number)?.toFloat() ?: 0f
            val z = (eventData["z"] as? Number)?.toFloat() ?: 0f
            TODO("APR: use JVM equivalent for LLURLDispatcher.dispatch with SLURL($regionName, $x, $y, $z)")
        }
    }

    private fun requestSit(eventData: Map<String, Any?>): Map<String, Any?> {
        val obj: ViewerObject? = when {
            eventData.containsKey("obj_uuid") -> {
                val id = eventData["obj_uuid"] as? UUID
                id?.let { ViewerObjectList.instance.findObject(it) }
            }
            eventData.containsKey("position") -> {
                @Suppress("UNCHECKED_CAST")
                val pos = eventData["position"] as? Vector3
                pos?.let { findObjectClosestTo(it, sitTarget = true) }
            }
            else -> {
                agent.setControlFlag(Agent.CONTROL_SIT_ON_GROUND)
                return emptyMap()
            }
        }

        if (obj != null && obj.isVolume()) {
            TODO("APR: use JVM equivalent for gMessageSystem AgentRequestSit to object ${obj.id}")
        }
        return mapOf("error" to "requestSit could not find the sit target")
    }

    private fun requestStand(eventData: Map<String, Any?>) {
        agent.setControlFlag(Agent.CONTROL_STAND_UP)
    }

    private fun requestTouch(eventData: Map<String, Any?>) {
        val obj: ViewerObject? = when {
            eventData.containsKey("obj_uuid") -> {
                val id = eventData["obj_uuid"] as? UUID
                id?.let { ViewerObjectList.instance.findObject(it) }
            }
            eventData.containsKey("position") -> {
                @Suppress("UNCHECKED_CAST")
                val pos = eventData["position"] as? Vector3
                pos?.let { findObjectClosestTo(it) }
            }
            else -> null
        }

        val face = (eventData["face"] as? Number)?.toInt() ?: 0

        if (obj != null && obj.isVolume()) {
            TODO("APR: use JVM equivalent for send_ObjectGrab_message / send_ObjectDeGrab_message face=$face")
        }
    }

    private fun resetAxes(eventData: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val lookAt = eventData["lookat"] as? Vector3
        if (lookAt != null) {
            agent.resetAxes(lookAt)
        } else {
            agent.resetAxes()
        }
    }

    private fun getPosition(eventData: Map<String, Any?>): Map<String, Any?> {
        val quat = agent.getQuat()
        val (roll, pitch, yaw) = quat.getEulerAngles()
        return mapOf(
            "quat" to listOf(quat.x, quat.y, quat.z, quat.w),
            "euler" to mapOf("roll" to roll, "pitch" to pitch, "yaw" to yaw),
            "region" to agent.getPositionAgent().toList(),
            "global" to agent.getPositionGlobal().toList(),
        )
    }

    private fun startAutoPilot(eventData: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val targetGlobal = eventData["target_global"] as? Vector3d
            ?: return

        @Suppress("UNCHECKED_CAST")
        val targetRotation = eventData["target_rotation"] as? Quaternion

        val rotationThreshold = (eventData["rotation_threshold"] as? Number)?.toFloat() ?: 0.03f
        val allowFlying = eventData["allow_flying"] as? Boolean ?: true
        val stopDistance = (eventData["stop_distance"] as? Number)?.toFloat() ?: 0f
        val behaviorName = eventData["behavior_name"] as? String ?: ""

        if (eventData.containsKey("allow_flying")) {
            agent.setFlying(allowFlying)
        }

        followTarget = null

        val finishCb: (Boolean) -> Unit = { success ->
            TODO("APR: post {success=$success} to LLAutopilot event pump")
        }

        agent.startAutoPilotGlobal(
            targetGlobal, behaviorName, targetRotation, finishCb,
            stopDistance, rotationThreshold, allowFlying
        )
    }

    private fun getAutoPilot(eventData: Map<String, Any?>): Map<String, Any?> {
        val enabled = agent.getAutoPilot()
        val reply = mutableMapOf<String, Any?>(
            "enabled" to enabled,
            "target_global" to agent.getAutoPilotTargetGlobal().toList(),
            "leader_id" to agent.getAutoPilotLeaderID(),
            "stop_distance" to agent.getAutoPilotStopDistance(),
            "target_distance" to agent.getAutoPilotTargetDist(),
            "use_rotation" to agent.getAutoPilotUseRotation(),
            "target_facing" to agent.getAutoPilotTargetFacing().toList(),
            "rotation_threshold" to agent.getAutoPilotRotationThreshold(),
            "behavior_name" to agent.getAutoPilotBehaviorName(),
            "fly" to agent.getFlying(),
        )

        val ft = followTarget
        if (!enabled && ft != null) {
            val target = ViewerObjectList.instance.findObject(ft)
            if (target != null) {
                val diff = target.getPositionRegion() - agent.getPositionAgent()
                reply["target_distance"] = diff.length()
                reply["leader_id"] = ft
            }
        }
        return reply
    }

    private fun startFollowPilot(eventData: Map<String, Any?>): Map<String, Any?> {
        val allowFlying = eventData["allow_flying"] as? Boolean ?: true
        val stopDistance = (eventData["stop_distance"] as? Number)?.toFloat() ?: 0f

        val targetId: UUID? = when {
            eventData.containsKey("leader_id") -> eventData["leader_id"] as? UUID
            eventData.containsKey("avatar_name") -> {
                val name = eventData["avatar_name"] as? String ?: ""
                if (name.isNotEmpty()) {
                    Character.instances.firstOrNull { ch ->
                        val av = ch as? VOAvatar
                        av != null && !av.isDead() && !av.isControlAvatar() && av.getFullname() == name
                    }?.getID()
                } else null
            }
            else -> return mapOf("error" to "'leader_id' or 'avatar_name' should be specified")
        }

        if (targetId == null || ViewerObjectList.instance.findObject(targetId) == null) {
            val info = eventData["leader_id"] ?: eventData["avatar_name"]
            return mapOf("error" to "Target \"$info\" was not found")
        }

        agent.setFlying(allowFlying)
        followTarget = targetId
        agent.startFollowPilot(targetId, allowFlying, stopDistance)
        return emptyMap()
    }

    private fun setAutoPilotTarget(eventData: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val target = eventData["target_global"] as? Vector3d ?: return
        agent.setAutoPilotTargetGlobal(target)
    }

    private fun stopAutoPilot(eventData: Map<String, Any?>) {
        val userCancel = eventData["user_cancel"] as? Boolean ?: false
        agent.stopAutoPilot(userCancel)
    }

    private fun lookAt(eventData: Map<String, Any?>) {
        val obj: ViewerObject? = when {
            eventData.containsKey("obj_uuid") -> {
                val id = eventData["obj_uuid"] as? UUID
                id?.let { ViewerObjectList.instance.findObject(it) }
            }
            eventData.containsKey("position") -> {
                @Suppress("UNCHECKED_CAST")
                val pos = eventData["position"] as? Vector3
                pos?.let { findObjectClosestTo(it) }
            }
            else -> null
        }

        val lookAtType = (eventData["type"] as? Number)?.toInt() ?: LookAtTarget.NONE.ordinal
        if (lookAtType in LookAtTarget.NONE.ordinal until LookAtTarget.NUM_TARGETS.ordinal) {
            AgentCamera.instance.setLookAt(LookAtTarget.entries[lookAtType], obj)
        }
    }

    private fun getGroups(eventData: Map<String, Any?>): Map<String, Any?> {
        val groups = agent.groups.map { g ->
            mapOf(
                "id" to g.id,
                "name" to g.name,
                "insignia" to g.insigniaId,
                "notices" to g.acceptNotices,
                "display" to g.listInProfile,
                "contrib" to g.contribution,
            )
        }
        return mapOf("groups" to groups)
    }

    private fun setFollowCamParams(eventData: Map<String, Any?>) {
        val followCam = FollowCamMgr.instance
        val agentId = agent.getID()

        camParams.forEach { (key, setter) ->
            if (eventData.containsKey(key)) {
                setter(followCam, agentId, eventData[key])
            }
        }
        followCam.setCameraActive(agentId, true)
    }

    private fun setFollowCamActive(eventData: Map<String, Any?>) {
        val active = eventData["active"] as? Boolean ?: false
        FollowCamMgr.instance.setCameraActive(agent.getID(), active)
    }

    private fun removeFollowCamParams(eventData: Map<String, Any?>) {
        FollowCamMgr.instance.removeFollowCamParams(agent.getID())
    }

    private fun playAnimation(eventData: Map<String, Any?>): Map<String, Any?> {
        val item = getAnimItem(eventData) ?: return mapOf("error" to "animation item not found")
        val inworld = eventData["inworld"] as? Boolean ?: false
        if (inworld) {
            agent.sendAnimationRequest(item.getAssetUUID(), AnimRequest.START)
        } else {
            TODO("APR: use JVM equivalent for gAgentAvatarp->startMotion(${item.getAssetUUID()})")
        }
        return emptyMap()
    }

    private fun stopAnimation(eventData: Map<String, Any?>): Map<String, Any?> {
        val item = getAnimItem(eventData) ?: return mapOf("error" to "animation item not found")
        TODO("APR: use JVM equivalent for gAgentAvatarp->stopMotion / sendAnimationRequest STOP ${item.getAssetUUID()}")
    }

    private fun getAnimationInfo(eventData: Map<String, Any?>): Map<String, Any?> {
        val item = getAnimItem(eventData) ?: return mapOf("error" to "animation item not found")
        val motion: Motion = TODO("APR: use JVM equivalent for gAgentAvatarp->createMotion(${item.getAssetUUID()})")
        return mapOf(
            "anim_info" to mapOf(
                "duration" to motion.getDuration(),
                "is_loop" to motion.getLoop(),
                "num_joints" to motion.getNumJointMotions(),
                "asset_id" to item.getAssetUUID(),
                "priority" to motion.getPriority(),
            )
        )
    }

    private fun getID(eventData: Map<String, Any?>): Map<String, Any?> {
        return mapOf("id" to agent.getID())
    }

    private fun getNearbyAvatarsList(eventData: Map<String, Any?>): Map<String, Any?> {
        val radius = getSearchRadiusSq(eventData)
        val agentPos = agent.getPositionGlobal()
        val result = Character.instances
            .mapNotNull { it as? VOAvatar }
            .filter { av -> !av.isDead() && !av.isControlAvatar() && !av.isSelf() }
            .filter { av -> distVecSquared(av.getPositionGlobal(), agentPos) <= radius }
            .map { av ->
                val avName = AvatarNameCache.get(av.getID())
                val regionPos = av.getCharacterPosition()
                mapOf(
                    "id" to av.getID(),
                    "global_pos" to av.getPosGlobalFromAgent(regionPos).toList(),
                    "region_pos" to regionPos.toList(),
                    "name" to avName.getUserName(),
                    "region_id" to av.getRegion()?.getRegionID(),
                )
            }
        return mapOf("result" to result)
    }

    private fun getNearbyObjectsList(eventData: Map<String, Any?>): Map<String, Any?> {
        val radius = getSearchRadiusSq(eventData)
        val agentPos = agent.getPositionGlobal()
        val result = ViewerObjectList.instance.allObjects()
            .filter { obj -> obj.getVolume() != null && !obj.isAttachment() }
            .filter { obj -> distVecSquared(obj.getPositionGlobal(), agentPos) <= radius }
            .map { obj ->
                mapOf(
                    "id" to obj.getID(),
                    "global_pos" to obj.getPositionGlobal().toList(),
                    "region_pos" to obj.getPositionRegion().toList(),
                    "region_id" to obj.getRegion()?.getRegionID(),
                )
            }
        return mapOf("result" to result)
    }

    private fun getAgentScreenPos(eventData: Map<String, Any?>): Map<String, Any?> {
        val avatarIdParam = eventData["avatar_id"] as? UUID
        val renderPos: Vector3 = if (avatarIdParam != null && avatarIdParam != agent.getID()) {
            Character.instances
                .mapNotNull { it as? VOAvatar }
                .firstOrNull { av -> !av.isDead() && av.getID() == avatarIdParam }
                ?.getRenderPosition() ?: Vector3()
        } else {
            TODO("APR: use JVM equivalent for gAgentAvatarp->getRenderPosition()")
        }

        val (onScreen, screenX, screenY) = TODO("GPU: ViewerCamera.projectPosAgentToScreen($renderPos)")
        return mapOf("onscreen" to onScreen, "x" to screenX, "y" to screenY)
    }

    private fun findObjectClosestTo(position: Vector3, sitTarget: Boolean = false): ViewerObject? {
        var minDistance = 10000.0f
        var closest: ViewerObject? = null
        for (obj in ViewerObjectList.instance.allObjects()) {
            if (obj.isAttachment()) continue
            if (sitTarget && !obj.isVolume()) continue
            val diff = obj.getPositionRegion() - position
            val dist = diff.length()
            if (dist < minDistance) {
                minDistance = dist
                closest = obj
            }
        }
        return closest
    }

    private fun getAnimItem(eventData: Map<String, Any?>): ViewerInventoryItem? {
        val itemId = eventData["item_id"] as? UUID ?: return null
        val item = InventoryModel.instance.getItem(itemId) ?: return null
        if (item.getInventoryType() != InventoryType.IT_ANIMATION) return null
        return item
    }

    private fun getSearchRadiusSq(eventData: Map<String, Any?>): Float {
        val renderFarClip = ViewerControl.getFloat("RenderFarClip") ?: 64f
        val dist = if (eventData.containsKey("dist")) {
            val raw = (eventData["dist"] as? Number)?.toFloat() ?: renderFarClip
            raw.coerceIn(1f, 512f)
        } else {
            renderFarClip
        }
        return dist * dist
    }

    private fun distVecSquared(a: Vector3d, b: Vector3d): Float {
        val dx = (a.x - b.x).toFloat()
        val dy = (a.y - b.y).toFloat()
        val dz = (a.z - b.z).toFloat()
        return dx * dx + dy * dy + dz * dz
    }

    companion object {
        private val camParams: List<Pair<String, (FollowCamMgr, UUID, Any?) -> Unit>> = listOf(
            "camera_pos" to { fc, src, v -> fc.setPosition(src, v as Vector3) },
            "focus_pos" to { fc, src, v -> fc.setFocus(src, v as Vector3) },
            "focus_offset" to { fc, src, v -> fc.setFocusOffset(src, v as Vector3) },
            "camera_locked" to { fc, src, v -> fc.setPositionLocked(src, v as Boolean) },
            "focus_locked" to { fc, src, v -> fc.setFocusLocked(src, v as Boolean) },
            "distance" to { fc, src, v -> fc.setDistance(src, (v as Number).toFloat()) },
            "focus_threshold" to { fc, src, v -> fc.setFocusThreshold(src, (v as Number).toFloat()) },
            "camera_threshold" to { fc, src, v -> fc.setPositionThreshold(src, (v as Number).toFloat()) },
            "focus_lag" to { fc, src, v -> fc.setFocusLag(src, (v as Number).toFloat()) },
            "camera_lag" to { fc, src, v -> fc.setPositionLag(src, (v as Number).toFloat()) },
            "camera_pitch" to { fc, src, v -> fc.setPitch(src, (v as Number).toFloat()) },
            "behindness_lag" to { fc, src, v -> fc.setBehindnessLag(src, (v as Number).toFloat()) },
            "behindness_angle" to { fc, src, v -> fc.setBehindnessAngle(src, (v as Number).toFloat()) },
        )
    }
}
