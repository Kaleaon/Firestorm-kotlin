package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d

class Agent private constructor() {

    companion object {
        private var sInstance: Agent? = null

        fun instance(): Agent = sInstance ?: Agent().also { sInstance = it }
    }

    enum class CameraMode { MOUSELOOK, THIRDPERSON, CUSTOMIZE_AVATAR, FOLLOW }

    var id: LLUUID = LLUUID.NULL
    var sessionId: LLUUID = LLUUID.NULL
    var firstName: String = ""
    var lastName: String = ""
    var regionHandle: ULong = 0uL

    var positionAgent: Vector3 = Vector3.ZERO
    var positionGlobal: Vector3d = Vector3d.ZERO
    var velocity: Vector3 = Vector3.ZERO

    var cameraPositionGlobal: Vector3d = Vector3d.ZERO
    var cameraMode: CameraMode = CameraMode.THIRDPERSON

    var controlFlags: UInt = 0u
    var godLevel: UByte = 0u
    var isBusy: Boolean = false
    var isAway: Boolean = false

    private var currentRegion: ViewerRegion? = null

    fun moveAt(direction: Int, reset: Boolean = false) {}

    fun moveLeft(direction: Int) {}

    fun moveUp(direction: Int) {}

    fun yaw(angle: Float) {}

    fun pitch(angle: Float) {}

    fun teleportTo(region: String, position: Vector3) {}

    fun teleportViaLure(targetId: LLUUID, teleportFlags: UInt) {}

    fun sendUpdate(force: Boolean = false) {}

    fun getRegion(): ViewerRegion? = currentRegion

    fun setRegion(region: ViewerRegion) {
        currentRegion = region
        regionHandle = region.regionId.hashCode().toULong()
    }

    fun isGodlike(): Boolean = godLevel > 0u

    fun setAFK(away: Boolean) {
        isAway = away
        if (away) controlFlags = controlFlags or AgentConstants.AGENT_CONTROL_AWAY
        else controlFlags = controlFlags and AgentConstants.AGENT_CONTROL_AWAY.inv()
    }

    fun clearAFK() = setAFK(false)
}
