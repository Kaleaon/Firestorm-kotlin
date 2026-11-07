package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.clampRescale
import com.firestorm.llmath.slerp

// VZ axis index, matching C++ VZ = 2
private const val VZ = 2

private const val MIN_TRACK_SPEED = 0.01f

class KeyframeFallMotion(id: LLUUID) : KeyframeMotion(id) {

    private var character: LLCharacter? = null
    private var velocityZ: Float = 0f
    private var pelvisState: JointState? = null
    private var rotationToGroundNormal: Quaternion = Quaternion()

    companion object {
        fun create(id: LLUUID): KeyframeFallMotion = KeyframeFallMotion(id)
    }

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character as? LLCharacter ?: return MotionInitStatus.FAILURE

        val result = super.onInitialize(character)
        if (result != MotionInitStatus.SUCCESS) return result

        val jml = jointMotionList ?: return result
        for (i in 0 until jml.numJointMotions) {
            val js = jointStates.getOrNull(i) ?: continue
            if (js.joint?.name == "mPelvis") {
                pelvisState = js
                break
            }
        }

        return result
    }

    override fun onActivate(): Boolean {
        val ch = character ?: return false

        val groundPos  = Vector3()
        val groundNorm = Vector3()
        val charVel    = ch.getCharacterVelocity()
        velocityZ = -charVel[VZ]

        ch.getGround(ch.getCharacterPosition(), groundPos, groundNorm)
        groundNorm.normalize()

        // Bring ground normal into pelvis/character space
        val invPelvisRot = ch.getCharacterRotation().conjugated()
        val localNorm = invPelvisRot * groundNorm

        // Build a rotation from standard up (Z) to ground normal.
        // fwd starts as (1,0,0), projected onto the plane perpendicular to groundNormal.
        var fwd = Vector3(1f, 0f, 0f)
        fwd = fwd - localNorm * (localNorm * fwd)   // project out ground-normal component
        val fwdLen = fwd.length()
        if (fwdLen > 1e-6f) fwd = fwd * (1f / fwdLen)

        val side = fwd % localNorm   // cross: right-hand frame
        rotationToGroundNormal = Matrix3ToQuaternion(fwd, side, localNorm)

        return super.onActivate()
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val result = super.onUpdate(activeTime)

        val dur = getDuration()
        val slerpAmt = if (dur > 0f) clampRescale(activeTime / dur, 0.5f, 0.75f, 0f, 1f) else 0f

        pelvisState?.let { ps ->
            ps.rotation = ps.rotation * slerp(slerpAmt, rotationToGroundNormal, Quaternion())
        }

        return result
    }

    override fun getEaseInDuration(): Float {
        val ch = character ?: return 0.4f
        return if (velocityZ == 0f) 0.4f else ch.getPreferredPelvisHeight() / velocityZ
    }
}

// Build a quaternion from three orthogonal basis vectors (column frame: forward, side, up).
// Mirrors the LLQuaternion(fwd, side, up) constructor used in C++.
private fun Matrix3ToQuaternion(fwd: Vector3, side: Vector3, up: Vector3): Quaternion {
    // Construct a rotation matrix from the basis vectors, then extract quaternion.
    // Row vectors: fwd = new X, side = new Y, up = new Z
    val trace = fwd.x + side.y + up.z
    return if (trace > 0f) {
        val s = kotlin.math.sqrt(trace + 1f) * 2f  // s = 4*w
        Quaternion(
            (side.z - up.y) / s,
            (up.x - fwd.z) / s,
            (fwd.y - side.x) / s,
            0.25f * s
        )
    } else if (fwd.x > side.y && fwd.x > up.z) {
        val s = kotlin.math.sqrt(1f + fwd.x - side.y - up.z) * 2f  // s = 4*x
        Quaternion(
            0.25f * s,
            (fwd.y + side.x) / s,
            (up.x + fwd.z) / s,
            (side.z - up.y) / s
        )
    } else if (side.y > up.z) {
        val s = kotlin.math.sqrt(1f + side.y - fwd.x - up.z) * 2f  // s = 4*y
        Quaternion(
            (fwd.y + side.x) / s,
            0.25f * s,
            (side.z + up.y) / s,
            (up.x - fwd.z) / s
        )
    } else {
        val s = kotlin.math.sqrt(1f + up.z - fwd.x - side.y) * 2f  // s = 4*z
        Quaternion(
            (up.x + fwd.z) / s,
            (side.z + up.y) / s,
            0.25f * s,
            (fwd.y - side.x) / s
        )
    }
}
