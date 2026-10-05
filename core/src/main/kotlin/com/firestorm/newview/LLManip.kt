package com.firestorm.newview

import java.util.UUID

const val MIN_DIVISION_PIXEL_WIDTH: Int = 3

abstract class LLManip(val name: String, val composite: LLToolComposite?) : LLTool(name, composite) {

    enum class EManipPart {
        LL_NO_PART,
        LL_X_ARROW,
        LL_Y_ARROW,
        LL_Z_ARROW,
        LL_YZ_PLANE,
        LL_XZ_PLANE,
        LL_XY_PLANE,
        LL_CORNER_NNN,
        LL_CORNER_NNP,
        LL_CORNER_NPN,
        LL_CORNER_NPP,
        LL_CORNER_PNN,
        LL_CORNER_PNP,
        LL_CORNER_PPN,
        LL_CORNER_PPP,
        LL_FACE_POSZ,
        LL_FACE_POSX,
        LL_FACE_POSY,
        LL_FACE_NEGX,
        LL_FACE_NEGY,
        LL_FACE_NEGZ,
        LL_EDGE_NEGX_NEGY,
        LL_EDGE_NEGX_POSY,
        LL_EDGE_POSX_NEGY,
        LL_EDGE_POSX_POSY,
        LL_EDGE_NEGY_NEGZ,
        LL_EDGE_NEGY_POSZ,
        LL_EDGE_POSY_NEGZ,
        LL_EDGE_POSY_POSZ,
        LL_EDGE_NEGZ_NEGX,
        LL_EDGE_NEGZ_POSX,
        LL_EDGE_POSZ_NEGX,
        LL_EDGE_POSZ_POSX,
        LL_ROT_GENERAL,
        LL_ROT_X,
        LL_ROT_Y,
        LL_ROT_Z,
        LL_ROT_ROLL;

        companion object {
            val LL_ARROW_MIN = LL_X_ARROW
            val LL_ARROW_MAX = LL_Z_ARROW
            val LL_CORNER_MIN = LL_CORNER_NNN
            val LL_CORNER_MAX = LL_CORNER_PPP
            val LL_FACE_MIN = LL_FACE_POSZ
            val LL_FACE_MAX = LL_FACE_NEGZ
            val LL_EDGE_MIN = LL_EDGE_NEGX_NEGY
            val LL_EDGE_MAX = LL_EDGE_POSZ_POSX
        }
    }

    companion object {
        var sHelpTextVisibleTime: Float = 2f
        var sHelpTextFadeTime: Float = 2f
        var sNumTimesHelpTextShown: Int = 0
        var sMaxTimesShowHelpText: Int = 5
        var sGridMaxSubdivisionLevel: Float = 32f
        var sGridMinSubdivisionLevel: Float = 1f / 32f
        var sTickLabelSpacing: FloatArray = floatArrayOf(60f, 25f)

        fun rebuild(vobj: LLViewerObject) {
            System.err.println("LLManip: rebuild not yet implemented")
        }

        fun renderXYZ(vec: FloatArray) {
            System.err.println("LLManip: renderXYZ not yet implemented")
        }
    }

    val mHelpTextTimer: LLFrameTimer = LLFrameTimer()
    var mInSnapRegime: Boolean = false
    var mObjectSelection: LLObjectSelection? = null
    var mHighlightedPart: EManipPart = EManipPart.LL_NO_PART
    var mManipPart: EManipPart = EManipPart.LL_NO_PART

    abstract fun handleMouseDownOnPart(x: Int, y: Int, mask: Int): Boolean
    abstract fun highlightManipulators(x: Int, y: Int)
    abstract fun canAffectSelection(): Boolean

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        var handled = false
        if (hasMouseCapture()) {
            handled = true
            setMouseCapture(false)
        }
        return handled
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            if (mObjectSelection?.isEmpty() == true) {
                setMouseCapture(false)
            }
        }
        // no-op: set cursor to arrow cursor via gViewerWindow
        return true
    }

    open fun handleSelect() {
        mObjectSelection = LLSelectMgr.getInstance().getEditSelection()
    }

    open fun handleDeselect() {
        mHighlightedPart = EManipPart.LL_NO_PART
        mManipPart = EManipPart.LL_NO_PART
        mObjectSelection = null
    }

    fun getHighlightedPart(): EManipPart = mHighlightedPart

    fun getSelection(): LLObjectSelection? = mObjectSelection

    fun renderGuidelines(drawX: Boolean = true, drawY: Boolean = true, drawZ: Boolean = true) {
        System.err.println("LLManip: renderGuidelines not yet implemented")
    }

    protected fun getSavedPivotPoint(): FloatArray {
        return LLSelectMgr.getInstance().getSavedBBoxOfSelection().getCenterAgent()
    }

    protected fun getPivotPoint(): FloatArray {
        return FloatArray(3)
    }

    protected fun getManipNormal(obj: LLViewerObject, manip: EManipPart, normal: FloatArray) {
        val (gridOrigin, gridRotation, gridScale) = LLSelectMgr.getInstance().getGrid()
        val ordinal = manip.ordinal
        val xArrow = EManipPart.LL_X_ARROW.ordinal
        val zArrow = EManipPart.LL_Z_ARROW.ordinal
        val yzPlane = EManipPart.LL_YZ_PLANE.ordinal
        val xyPlane = EManipPart.LL_XY_PLANE.ordinal

        if (ordinal in xArrow..zArrow) {
            val arrowAxis = FloatArray(3)
            getManipAxis(obj, manip, arrowAxis)
            // no-op: compute cross product of arrowAxis and camera at-axis, then cross with arrowAxis, normalise
        } else if (ordinal in yzPlane..xyPlane) {
            when (manip) {
                EManipPart.LL_YZ_PLANE -> { normal[0] = 1f; normal[1] = 0f; normal[2] = 0f }
                EManipPart.LL_XZ_PLANE -> { normal[0] = 0f; normal[1] = 1f; normal[2] = 0f }
                EManipPart.LL_XY_PLANE -> { normal[0] = 0f; normal[1] = 0f; normal[2] = 1f }
                else -> {}
            }
            // no-op: rotate normal by gridRotation
        } else {
            normal[0] = 0f; normal[1] = 0f; normal[2] = 0f
        }
    }

    protected fun getManipAxis(obj: LLViewerObject, manip: EManipPart, axis: FloatArray): Boolean {
        val (gridOrigin, gridRotation, gridScale) = LLSelectMgr.getInstance().getGrid()
        return when (manip) {
            EManipPart.LL_X_ARROW -> { axis[0] = 1f; axis[1] = 0f; axis[2] = 0f; /* no-op: rotate axis by gridRotation */; true }
            EManipPart.LL_Y_ARROW -> { axis[0] = 0f; axis[1] = 1f; axis[2] = 0f; /* no-op: rotate axis by gridRotation */; true }
            EManipPart.LL_Z_ARROW -> { axis[0] = 0f; axis[1] = 0f; axis[2] = 1f; /* no-op: rotate axis by gridRotation */; true }
            else -> false
        }
    }

    protected fun getSubdivisionLevel(
        referencePoint: FloatArray,
        translateAxis: FloatArray,
        gridScale: Float,
        minPixelSpacing: Int = MIN_DIVISION_PIXEL_WIDTH,
        minSubdivisions: Float = sGridMinSubdivisionLevel,
        maxSubdivisions: Float = sGridMaxSubdivisionLevel
    ): Float {
        return 0f
    }

    protected fun renderTickValue(pos: FloatArray, value: Float, suffix: String, color: FloatArray) {
        System.err.println("LLManip: renderTickValue not yet implemented")
    }

    protected fun renderTickText(pos: FloatArray, text: String, color: FloatArray) {
        System.err.println("LLManip: renderTickText not yet implemented")
    }

    protected fun updateGridSettings() {
        System.err.println("LLManip: updateGridSettings not yet implemented")
    }

    protected fun getMousePointOnPlaneGlobal(point: DoubleArray, x: Int, y: Int, origin: DoubleArray, normal: FloatArray): Boolean {
        return false
    }

    protected fun getMousePointOnPlaneAgent(point: FloatArray, x: Int, y: Int, origin: FloatArray, normal: FloatArray): Boolean {
        return false
    }

    protected fun nearestPointOnLineFromMouse(x: Int, y: Int, b1: FloatArray, b2: FloatArray, aParam: FloatArray, bParam: FloatArray): Boolean {
        return false
    }

    protected fun setupSnapGuideRenderPass(pass: Int): FloatArray {
        return FloatArray(4)
    }
}
