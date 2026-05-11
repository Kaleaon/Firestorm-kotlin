/**
 * @file FSPose.kt
 * @brief Avatar pose / animation management.
 *
 * Ported from fspose.h / fspose.cpp
 * Original author: Cinder Roxley <cinder.roxley@phoenixviewer.com>
 * "THE BEER-WARE LICENSE" (Revision 42)
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Data classes
// ---------------------------------------------------------------------------

/**
 * Holds the transform state of a single avatar joint.
 *
 * @param jointName  Canonical SL joint name (e.g. "mWristLeft").
 * @param rotation   Joint rotation expressed as a quaternion.
 * @param position   Joint position offset in local space.
 * @param scale      Per-joint scale factor.
 */
data class FSJointState(
    val jointName: String,
    var rotation: Quaternion,
    var position: Vector3,
    var scale: Vector3,
)

/**
 * A named pose — an ordered collection of [FSJointState] records that
 * together describe a full avatar body position.
 *
 * @param name   Human-readable label used for display and persistence.
 * @param joints Mutable list so callers can append or remove joint states.
 */
data class FSPose(
    val name: String,
    val joints: MutableList<FSJointState> = mutableListOf(),
)

// ---------------------------------------------------------------------------
// Singleton manager  (C++ LLSingleton<FSPose> → Kotlin object)
// ---------------------------------------------------------------------------

/**
 * Central manager for avatar pose playback and persistence.
 *
 * Mirrors the C++ `FSPose` singleton.  The C++ implementation communicated
 * with the agent via `gAgent.sendAnimationRequest()`; those calls are
 * replaced here with `TODO()` stubs until the agent layer is ported.
 */
object FSPoseManager {

    /** All poses loaded from disk or created in the current session. */
    val poses: MutableList<FSPose> = mutableListOf()

    /** The pose currently playing on the local avatar, or `null` if idle. */
    var activePose: FSPose? = null
        private set

    // -----------------------------------------------------------------------
    // Playback
    // -----------------------------------------------------------------------

    /**
     * Start playing [pose] on the local avatar.
     *
     * Stops any currently-active pose first, then sends an animation-start
     * request to the agent (stubbed until the agent layer is ported).
     *
     * C++ equivalent: `FSPose::setPose(new_pose, save_state = true)`
     */
    fun applyPose(pose: FSPose) {
        // Stop the current pose before switching.
        activePose?.let { stopCurrentPose() }

        activePose = pose
        // TODO: sendAnimationRequest(pose.animationId, ANIM_REQUEST_START)
        TODO("Send animation-start request to LLAgent for pose '${pose.name}'")
    }

    /**
     * Stop the currently playing pose and clear [activePose].
     *
     * C++ equivalent: `FSPose::stopPose()`
     */
    fun stopCurrentPose() {
        val current = activePose ?: return
        activePose = null
        // TODO: sendAnimationRequest(current.animationId, ANIM_REQUEST_STOP)
        TODO("Send animation-stop request to LLAgent for pose '${current.name}'")
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    /**
     * Persist the current joint transforms to disk under [name].
     *
     * The C++ viewer stored poses as BVH/ANIM assets or XML; the exact
     * serialisation format is TBD for the Kotlin port.
     */
    fun savePose(name: String) {
        TODO("Serialise active joint states and write to per-account pose directory as '$name'")
    }

    /**
     * Scan the per-account pose directory and populate [poses].
     *
     * The C++ viewer used `LLAPRFile` / `gDirUtilp`; the Kotlin port should
     * use `java.nio.file.Path` once the directory layout is established.
     */
    fun loadPosesFromDisk() {
        TODO("Enumerate pose files from the per-account directory and deserialise into 'poses'")
    }
}
