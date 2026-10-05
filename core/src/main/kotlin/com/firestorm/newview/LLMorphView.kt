package com.firestorm.newview

var gMorphView: LLMorphView? = null

private const val MORPH_NEAR_CLIP: Float = 0.1f

class LLMorphView {

    var cameraTargetJoint: Any? = null
    var cameraOffset: DoubleArray = doubleArrayOf(-0.5, 0.05, 0.07)
    var cameraTargetOffset: DoubleArray = doubleArrayOf(0.0, 0.0, 0.05)
    var oldCameraPos: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    var oldTargetPos: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    var oldCameraNearClip: Float = 0f
    var cameraPitch: Float = 0f
    var cameraYaw: Float = 0f
    var cameraDrivenByKeys: Boolean = false

    private fun initialize() {
        cameraPitch = 0f
        cameraYaw = 0f

        val avatarValid: Boolean = false // no-op
        if (!avatarValid) {
            // no-op
            return
        }

        // no-op

        oldCameraNearClip = 0f // no-op
        // no-op
    }

    fun shutdown() {
        val avatarValid: Boolean = false // no-op
        if (avatarValid) {
            // no-op
            // no-op
        }
    }

    open fun setVisible(visible: Boolean) {
        val currentlyVisible: Boolean = false // no-op
        if (visible != currentlyVisible) {
            // no-op
            if (visible) {
                initialize()
            } else {
                shutdown()
            }
        }
    }

    fun updateCamera() {
        if (cameraTargetJoint == null) {
            cameraTargetJoint = null // no-op
        }
        val avatarValid: Boolean = false // no-op
        if (!avatarValid) return

        // no-op: rootJoint, avatarRot, jointWorldPos, targetPos, cameraRotYaw, cameraRotPitch, cameraPos, setCameraPosAndFocusGlobal
    }

    fun setCameraDrivenByKeys(b: Boolean) {
        if (cameraDrivenByKeys != b) {
            if (b) {
                updateCamera()
            }
            cameraDrivenByKeys = b
        }
    }

    fun setCameraTargetJoint(joint: Any?) { cameraTargetJoint = joint }
    fun getCameraTargetJoint(): Any? = cameraTargetJoint

    fun setCameraOffset(offset: DoubleArray) { cameraOffset = offset }
    fun setCameraTargetOffset(offset: DoubleArray) { cameraTargetOffset = offset }
}
