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

        val avatarValid: Boolean = TODO("GPU: check isAgentAvatarValid() && !gAgentAvatarp->isDead()")
        if (!avatarValid) {
            TODO("GPU: gAgentCamera.changeCameraToDefault()")
            return
        }

        TODO("GPU: gAgentAvatarp->stopMotion(ANIM_AGENT_BODY_NOISE)")

        oldCameraNearClip = TODO("GPU: LLViewerCamera::getInstance()->getNear()")
        TODO("GPU: LLViewerCamera::getInstance()->setNear(MORPH_NEAR_CLIP)")
    }

    fun shutdown() {
        val avatarValid: Boolean = TODO("GPU: check isAgentAvatarValid()")
        if (avatarValid) {
            TODO("GPU: gAgentAvatarp->startMotion(ANIM_AGENT_BODY_NOISE)")
            TODO("GPU: LLViewerCamera::getInstance()->setNear(oldCameraNearClip)")
        }
    }

    open fun setVisible(visible: Boolean) {
        val currentlyVisible: Boolean = TODO("GPU: getVisible()")
        if (visible != currentlyVisible) {
            TODO("GPU: LLView::setVisible(visible)")
            if (visible) {
                initialize()
            } else {
                shutdown()
            }
        }
    }

    fun updateCamera() {
        if (cameraTargetJoint == null) {
            cameraTargetJoint = TODO("GPU: gAgentAvatarp->getJoint(\"mHead\")")
        }
        val avatarValid: Boolean = TODO("GPU: isAgentAvatarValid()")
        if (!avatarValid) return

        val rootJoint: Any = TODO("GPU: gAgentAvatarp->getRootJoint()") ?: return
        val avatarRot: Any = TODO("GPU: rootJoint.getWorldRotation()")
        val jointWorldPos: DoubleArray = TODO("GPU: gAgent.getPosGlobalFromAgent(cameraTargetJoint->getWorldPosition())")
        val targetPos: DoubleArray = TODO("GPU: jointWorldPos + cameraTargetOffset * avatarRot")
        val cameraRotYaw: Any = TODO("GPU: LLQuaternion(cameraYaw, LLVector3::z_axis)")
        val cameraRotPitch: Any = TODO("GPU: LLQuaternion(cameraPitch, LLVector3::y_axis)")
        val cameraPos: DoubleArray = TODO("GPU: jointWorldPos + cameraOffset * cameraRotPitch * cameraRotYaw * avatarRot")
        TODO("GPU: gAgentCamera.setCameraPosAndFocusGlobal(cameraPos, targetPos, gAgent.getID())")
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
