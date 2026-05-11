// Converted from llhandmotion.h / llhandmotion.cpp — Linden Research, Inc.
// LGPL 2.1; see original source for full license text.
package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const val MIN_REQUIRED_PIXEL_AREA_HAND: Float = 10000f

/** Blend time (seconds) for morphing between hand poses. */
private const val HAND_MORPH_BLEND_TIME: Float = 0.2f

// ---------------------------------------------------------------------------
// HandMotion
//
// Manages morph-based hand pose blending on the upper-body mesh.
// Mirrors C++ LLHandMotion.
//
// No joint rotations are applied; the hand poses are driven entirely through
// visual-parameter (morph) weights on the LLCharacter.
// ---------------------------------------------------------------------------

class HandMotion(id: LLUUID) : LLMotion(id) {

    // ---- hand-pose enum (mirrors eHandPose) --------------------------------

    enum class HandPose(val poseName: String) {
        SPREAD    (""),                // index 0 — default, not a morph
        RELAXED   ("Hands_Relaxed"),
        POINT     ("Hands_Point"),
        FIST      ("Hands_Fist"),
        RELAXED_L ("Hands_Relaxed_L"),
        POINT_L   ("Hands_Point_L"),
        FIST_L    ("Hands_Fist_L"),
        RELAXED_R ("Hands_Relaxed_R"),
        POINT_R   ("Hands_Point_R"),
        FIST_R    ("Hands_Fist_R"),
        SALUTE_R  ("Hands_Salute_R"),
        TYPING    ("Hands_Typing"),
        PEACE_R   ("Hands_Peace_R"),
        PALM_R    ("Hands_Spread_R");

        companion object {
            /** Look up a HandPose by its morph name; returns SPREAD if not found. */
            fun fromPoseName(name: String): HandPose =
                entries.firstOrNull { it.poseName == name } ?: SPREAD
        }
    }

    // ---- runtime state -----------------------------------------------------

    private var character: LLCharacter? = null
    private var lastTime: Float = 0f
    var currentPose: HandPose = HandPose.RELAXED
        private set
    var newPose: HandPose = HandPose.RELAXED
        private set

    companion object {
        fun create(id: LLUUID): HandMotion = HandMotion(id)
    }

    init {
        name = "hand_motion"
    }

    // ---- LLMotion overrides ------------------------------------------------

    override fun getLoop(): Boolean = true
    override fun getDuration(): Float = 0f
    override fun getEaseInDuration(): Float = 0f
    override fun getEaseOutDuration(): Float = 0f
    override fun getPriority(): JointPriority = JointPriority.MEDIUM
    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND
    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_HAND
    override fun canDeprecate(): Boolean = false

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character as? LLCharacter ?: return MotionInitStatus.FAILURE
        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean {
        val ch = character ?: return false
        // reset all non-default poses to 0, activate the current pose
        for (pose in HandPose.entries) {
            if (pose != HandPose.SPREAD && pose.poseName.isNotEmpty()) {
                ch.setVisualParamWeight(pose.poseName, 0f)
            }
        }
        if (currentPose != HandPose.SPREAD && currentPose.poseName.isNotEmpty()) {
            ch.setVisualParamWeight(currentPose.poseName, 1f)
        }
        ch.updateVisualParams()
        return true
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val ch = character ?: return true

        val timeDelta = activeTime - lastTime
        lastTime = activeTime

        // Read the requested pose from the animation-data store
        val requestedPoseOrdinal = ch.getAnimationData("Hand Pose") as? Int

        if (requestedPoseOrdinal == null) {
            // No request: revert to RELAXED if we were mid-blend
            if (newPose != HandPose.RELAXED && newPose != currentPose) {
                if (newPose != HandPose.SPREAD && newPose.poseName.isNotEmpty()) {
                    ch.setVisualParamWeight(newPose.poseName, 0f)
                }
                if (currentPose != HandPose.SPREAD && currentPose.poseName.isNotEmpty()) {
                    ch.setVisualParamWeight(currentPose.poseName, 1f)
                }
                if (currentPose == HandPose.RELAXED) {
                    ch.updateVisualParams()
                }
            }
            newPose = HandPose.RELAXED
        } else {
            val requested = HandPose.entries.getOrNull(requestedPoseOrdinal)
            if (requested != null) {
                // Mid-blend interruption: reset both old and new pose weights
                if (requested != newPose && newPose != currentPose) {
                    if (newPose != HandPose.SPREAD && newPose.poseName.isNotEmpty()) {
                        ch.setVisualParamWeight(newPose.poseName, 0f)
                    }
                    if (currentPose != HandPose.SPREAD && currentPose.poseName.isNotEmpty()) {
                        ch.setVisualParamWeight(currentPose.poseName, 1f)
                    }
                    if (currentPose == requested) {
                        ch.updateVisualParams()
                    }
                }
                newPose = requested
            } else {
                System.err.println("HandMotion: requested hand pose $requestedPoseOrdinal out of range, ignoring")
            }
        }

        ch.removeAnimationData("Hand Pose")
        ch.removeAnimationData("Hand Pose Priority")

        // Blend between currentPose and newPose
        if (currentPose != newPose) {
            var incomingWeight = 1f
            var outgoingWeight = 0f

            if (newPose != HandPose.SPREAD && newPose.poseName.isNotEmpty()) {
                incomingWeight = ch.getVisualParamWeight(newPose.poseName)
                incomingWeight = (incomingWeight + timeDelta / HAND_MORPH_BLEND_TIME).coerceIn(0f, 1f)
                ch.setVisualParamWeight(newPose.poseName, incomingWeight)
            }

            if (currentPose != HandPose.SPREAD && currentPose.poseName.isNotEmpty()) {
                outgoingWeight = ch.getVisualParamWeight(currentPose.poseName)
                outgoingWeight = (outgoingWeight - timeDelta / HAND_MORPH_BLEND_TIME).coerceIn(0f, 1f)
                ch.setVisualParamWeight(currentPose.poseName, outgoingWeight)
            }

            ch.updateVisualParams()

            if (incomingWeight == 1f && outgoingWeight == 0f) {
                currentPose = newPose
            }
        }

        return true
    }

    override fun onDeactivate() {
        // nothing to clean up
    }

    // ---- Public helpers (mirrors static C++ methods) -----------------------

    /** Return the morph name for [pose]. */
    fun getHandPoseName(pose: HandPose): String = pose.poseName

    /** Return the HandPose whose morph name matches [poseName], or SPREAD if not found. */
    fun getHandPose(poseName: String): HandPose = HandPose.fromPoseName(poseName)
}

// ---------------------------------------------------------------------------
// Extension on LLCharacter to conveniently read visual-param weight by name
// ---------------------------------------------------------------------------

private fun LLCharacter.getVisualParamWeight(name: String): Float =
    getVisualParam(name)?.weight ?: 0f
