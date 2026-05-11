package com.firestorm.newview

import java.util.UUID

enum class EPoserReferenceFrame {
    POSER_FRAME_BONE,
    POSER_FRAME_WORLD,
    POSER_FRAME_AVATAR,
    POSER_FRAME_CAMERA,
}

enum class EManipPart {
    LL_NO_PART,
    LL_X_ARROW,
    LL_Y_ARROW,
    LL_Z_ARROW,
    LL_YZ_PLANE,
    LL_XZ_PLANE,
    LL_XY_PLANE,
}

data class Vector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    fun setZero() = Vector3(0f, 0f, 0f)
}

data class Vector4(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 1f)

class Quaternion

class LLJoint {
    fun getWorldPosition(): Vector3 = TODO("GPU: get world position from joint")
    fun getName(): String = TODO("GPU: get joint name")
    fun getParent(): LLJoint? = TODO("GPU: get parent joint")
}

class LLVOAvatar {
    fun isDead(): Boolean = TODO("GPU: check avatar dead state")
    fun isFullyLoaded(): Boolean = TODO("GPU: check avatar fully loaded")
}

class LLToolComposite

class FSManipTranslateJoint(composite: LLToolComposite) {

    private var mJoint: LLJoint? = null
    private var mAvatar: LLVOAvatar? = null
    private var mReferenceFrame: EPoserReferenceFrame = EPoserReferenceFrame.POSER_FRAME_BONE

    private var mHighlightedPart: EManipPart = EManipPart.LL_NO_PART
    private var mManipPart: EManipPart = EManipPart.LL_NO_PART
    private var mMouseDownX: Int = 0
    private var mMouseDownY: Int = 0
    private var mMouseOutsideSlop: Boolean = false
    private var mLastHoverMouseX: Int = 0
    private var mLastHoverMouseY: Int = 0
    private var mDragCursorLastGlobal: Vector3 = Vector3()
    private var mArrowLengthMeters: Float = 1.0f
    private var mPlaneManipOffsetMeters: Float = 0f
    private var mConeSize: Float = 0f

    private companion object {
        const val NUM_AXES = 3
        const val MOUSE_DRAG_SLOP = 2
        const val SELECTED_ARROW_SCALE = 1.3f
        const val MANIPULATOR_HOTSPOT_START = 0.2f
        const val MANIPULATOR_HOTSPOT_END = 1.2f
        const val MIN_PLANE_MANIP_DOT_PRODUCT = 0.25f
        const val PLANE_TICK_SIZE = 0.4f

        private var sGridTexName: UInt = 0u

        fun getGridTexName(): UInt {
            if (sGridTexName == 0u) restoreGL()
            return sGridTexName
        }

        fun destroyGL() {
            TODO("GPU: destroy grid texture GL resource")
        }

        fun restoreGL() {
            TODO("GPU: generate grid texture via OpenGL mipmap upload")
        }
    }

    fun setJoint(joint: LLJoint) {
        mJoint = joint
    }

    fun setAvatar(avatar: LLVOAvatar?) {
        mAvatar = avatar
    }

    fun setReferenceFrame(frame: EPoserReferenceFrame) {
        mReferenceFrame = frame
    }

    fun handleSelect() {
        TODO("GPU: save selected object transform and set status text 'move'")
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val relevant = mHighlightedPart == EManipPart.LL_X_ARROW ||
                mHighlightedPart == EManipPart.LL_Y_ARROW ||
                mHighlightedPart == EManipPart.LL_Z_ARROW ||
                mHighlightedPart == EManipPart.LL_YZ_PLANE ||
                mHighlightedPart == EManipPart.LL_XZ_PLANE ||
                mHighlightedPart == EManipPart.LL_XY_PLANE
        return if (relevant) handleMouseDownOnPart(x, y, mask) else false
    }

    fun handleMouseDownOnPart(x: Int, y: Int, mask: Int): Boolean {
        if (!canAffectSelection()) return false

        highlightManipulators(x, y)
        val hitPart = mHighlightedPart

        val isArrowOrPlane = hitPart == EManipPart.LL_X_ARROW ||
                hitPart == EManipPart.LL_Y_ARROW ||
                hitPart == EManipPart.LL_Z_ARROW ||
                hitPart == EManipPart.LL_YZ_PLANE ||
                hitPart == EManipPart.LL_XZ_PLANE ||
                hitPart == EManipPart.LL_XY_PLANE
        if (!isArrowOrPlane) return true

        if (!isAvatarJointSafeToUse()) {
            TODO("APR: use JVM equivalent - log warn about lost joint/avatar, set translate cursor")
        }

        mManipPart = hitPart
        mMouseDownX = x
        mMouseDownY = y
        mMouseOutsideSlop = false
        mDragCursorLastGlobal = Vector3()

        TODO("GPU: compute manip axis/normal, project cursor onto plane, snap if needed, capture mouse")
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: handle hover - rotate camera at edge, project cursor onto manip plane, update posed bones")
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        handleHover(x, y, mask)
        TODO("GPU: reset manip part, re-enable silhouette, send position update, release mouse capture")
    }

    fun render() {
        if (!isMoveableJoint()) return
        TODO("GPU: push modelview matrix, render guidelines with depth, render translation handles, render text, pop matrix")
    }

    fun highlightManipulators(x: Int, y: Int) {
        mHighlightedPart = EManipPart.LL_NO_PART
        if (!isAvatarJointSafeToUse()) return

        TODO("GPU: project manipulator handle vertices through view/proj matrices, find closest handle under cursor via 2D line-segment test")
    }

    fun canAffectSelection(): Boolean = isAvatarJointSafeToUse()

    private fun renderArrow(whichArrow: EManipPart, selectedArrow: EManipPart, boxSize: Float, arrowSize: Float, handleSize: Float, reverseDirection: Boolean) {
        TODO("GPU: render cone arrow with color based on selection state, two-pass depth rendering")
    }

    private fun renderTranslationHandles() {
        TODO("GPU: render YZ/XZ/XY plane handles as colored triangle pairs, then render axis arrows depth-sorted by nearest camera vertex")
    }

    private fun renderText() {
        if (!isAvatarJointSafeToUse()) return
        TODO("GPU: render XYZ coordinate overlay text at joint world position")
    }

    private fun isAvatarJointSafeToUse(): Boolean {
        val joint = mJoint ?: return false
        val avatar = mAvatar ?: return false
        if (avatar.isDead() || !avatar.isFullyLoaded()) {
            setAvatar(null)
            return false
        }
        return true
    }

    private fun isMoveableJoint(): Boolean {
        val joint = mJoint ?: return false
        val jointName = joint.getName()
        if (jointName == "mPelvis" || jointName == "mRoot") return true

        val parentJoint = joint.getParent() ?: return false
        val parentName = parentJoint.getName()
        if (parentName == "mPelvis" || parentName == "mRoot") return false

        val grandParentJoint = parentJoint.getParent() ?: return false
        val grandParentName = grandParentJoint.getName()
        if (grandParentName == "mPelvis" || grandParentName == "mRoot") return false

        return true
    }

    private fun getChangeInPosition(newPosition: Vector3): Vector3 {
        var raw = newPosition - mDragCursorLastGlobal
        raw = when (mManipPart) {
            EManipPart.LL_X_ARROW  -> raw.copy(y = 0f, z = 0f)
            EManipPart.LL_Y_ARROW  -> raw.copy(x = 0f, z = 0f)
            EManipPart.LL_Z_ARROW  -> raw.copy(x = 0f, y = 0f)
            EManipPart.LL_XY_PLANE -> raw.copy(z = 0f)
            EManipPart.LL_XZ_PLANE -> raw.copy(y = 0f)
            EManipPart.LL_YZ_PLANE -> raw.copy(x = 0f)
            else                   -> raw
        }
        return raw
    }
}
