package com.firestorm.newview

private const val NUDGE_TIME = 0.25f
private const val ORBIT_NUDGE_RATE = 0.05f

enum class EJoystickQuadrant {
    JQ_ORIGIN,
    JQ_UP,
    JQ_DOWN,
    JQ_LEFT,
    JQ_RIGHT;

    companion object {
        fun fromName(name: String): EJoystickQuadrant = when (name) {
            "origin" -> JQ_ORIGIN
            "up"     -> JQ_UP
            "down"   -> JQ_DOWN
            "left"   -> JQ_LEFT
            "right"  -> JQ_RIGHT
            else     -> JQ_RIGHT
        }

        fun toName(quadrant: EJoystickQuadrant): String = when (quadrant) {
            JQ_ORIGIN -> "origin"
            JQ_UP     -> "up"
            JQ_DOWN   -> "down"
            JQ_LEFT   -> "left"
            JQ_RIGHT  -> "right"
        }
    }
}

data class LLCoordGL(var x: Int = 0, var y: Int = 0)

data class LLVector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    operator fun plus(other: LLVector3) = LLVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: LLVector3) = LLVector3(x - other.x, y - other.y, z - other.z)
    fun isNull(): Boolean = x == 0f && y == 0f && z == 0f
    fun normalize() { /* GPU: normalise vector in-place */ }
    operator fun get(index: Int): Float = when (index) { 0 -> x; 1 -> y; else -> z }
    operator fun set(index: Int, v: Float) { when (index) { 0 -> x = v; 1 -> y = v; else -> z = v } }
}

data class LLQuaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun setAngleAxis(angle: Float, axis: LLVector3) { /* GPU: set this quaternion from angle-axis */ }
    fun normalize() { /* GPU: normalise quaternion in-place */ }
    fun getValue(): Any { return Any() }
    operator fun timesAssign(other: LLQuaternion) { /* GPU: multiply this quaternion by other in-place */ }
}

data class LLJoystickRect(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0
) {
    fun getWidth(): Int = right - left
    fun getHeight(): Int = top - bottom
    fun getCenterX(): Int = left + getWidth() / 2
    fun getCenterY(): Int = bottom + getHeight() / 2
}

abstract class LLButton(
    protected val initialQuadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN
) {
    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        return false
    }
    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        return false
    }
    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        return false
    }
    fun hasMouseCapture(): Boolean { return false }
    fun getRect(): LLJoystickRect { return LLJoystickRect() }
    fun getLocalRect(): LLJoystickRect { return LLJoystickRect() }
    fun getHeldDownTime(): Float { return 0f }
    fun getImageUnselected(): Any { return Any() }
    fun getImageSelected(): Any { return Any() }
    fun setValue(value: Any) { /* GPU: set widget committed value */ }
    fun onCommit() { /* GPU: fire commit signal to listeners */ }
    fun setHeldDownCallback(cb: (Any) -> Unit, userdata: Any) {}
}

open class LLJoystick(
    quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN
) : LLButton(quadrant) {

    protected var mInitialQuadrant: EJoystickQuadrant = quadrant
    protected val mInitialOffset: LLCoordGL = LLCoordGL(0, 0)
    protected val mLastMouse: LLCoordGL = LLCoordGL(0, 0)
    protected val mFirstMouse: LLCoordGL = LLCoordGL(0, 0)
    protected var mVertSlopNear: Int = 0
    protected var mVertSlopFar: Int = 0
    protected var mHorizSlopNear: Int = 0
    protected var mHorizSlopFar: Int = 0
    protected var mHeldDown: Boolean = false

    init {
        setHeldDownCallback({ _ -> onBtnHeldDown(this) }, this)
    }

    protected open fun updateSlop() {
        val rect = getRect()
        mVertSlopNear = rect.getHeight()
        mVertSlopFar = rect.getHeight() * 2
        mHorizSlopNear = rect.getWidth()
        mHorizSlopFar = rect.getWidth() * 2

        when (mInitialQuadrant) {
            EJoystickQuadrant.JQ_ORIGIN -> {
                mInitialOffset.x = 0
                mInitialOffset.y = 0
            }
            EJoystickQuadrant.JQ_UP -> {
                mInitialOffset.x = 0
                mInitialOffset.y = (mVertSlopNear + mVertSlopFar) / 2
            }
            EJoystickQuadrant.JQ_DOWN -> {
                mInitialOffset.x = 0
                mInitialOffset.y = -((mVertSlopNear + mVertSlopFar) / 2)
            }
            EJoystickQuadrant.JQ_LEFT -> {
                mInitialOffset.x = -((mHorizSlopNear + mHorizSlopFar) / 2)
                mInitialOffset.y = 0
            }
            EJoystickQuadrant.JQ_RIGHT -> {
                mInitialOffset.x = (mHorizSlopNear + mHorizSlopFar) / 2
                mInitialOffset.y = 0
            }
        }
    }

    fun pointInCircle(x: Int, y: Int): Boolean {
        val a = getLocalRect().getWidth() / 2.0f
        val b = getLocalRect().getHeight() / 2.0f
        if (a == 0f || b == 0f) return false
        val dx = x - a
        val dy = y - b
        return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
    }

    fun pointInCenterDot(x: Int, y: Int): Boolean {
        val centerDotScale = 0.15
        val centerDotXRad = (getLocalRect().getWidth() / 2 * centerDotScale).toInt()
        val centerDotYRad = (getLocalRect().getHeight() / 2 * centerDotScale).toInt()
        val a = getLocalRect().getCenterX()
        val b = getLocalRect().getCenterY()
        if (centerDotXRad == 0 || centerDotYRad == 0) return false
        val result = (((x - a).toLong() * (x - a)) / (centerDotXRad * centerDotXRad).toDouble() +
                ((y - b).toLong() * (y - b)) / (centerDotYRad * centerDotYRad).toDouble())
        return result <= 1.0
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (pointInCircle(x, y)) {
            mLastMouse.x = x; mLastMouse.y = y
            mFirstMouse.x = x; mFirstMouse.y = y
            // GPU: reset mouseDownTimer then delegate to LLButton.handleMouseDown
        }
        return false
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            mLastMouse.x = x; mLastMouse.y = y
            mHeldDown = false
            onMouseUp()
        }
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            mLastMouse.x = x; mLastMouse.y = y
        }
        return super.handleHover(x, y, mask)
    }

    open fun onMouseUp() {}
    abstract fun onHeldDown()

    fun getElapsedHeldDownTime(): Float = if (mHeldDown) getHeldDownTime() else 0f

    fun setInitialQuadrant(initial: EJoystickQuadrant) { mInitialQuadrant = initial }

    companion object {
        fun onBtnHeldDown(self: LLJoystick) {
            self.mHeldDown = true
            self.onHeldDown()
        }

        fun nameFromQuadrant(quadrant: EJoystickQuadrant): String = EJoystickQuadrant.toName(quadrant)

        fun quadrantFromName(name: String): EJoystickQuadrant = EJoystickQuadrant.fromName(name)

        fun selectQuadrant(attributeMap: Map<String, String>): EJoystickQuadrant {
            val name = attributeMap["quadrant"] ?: return EJoystickQuadrant.JQ_RIGHT
            return quadrantFromName(name)
        }
    }
}

class LLJoystickAgentTurn(
    quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN
) : LLJoystick(quadrant) {

    override fun onHeldDown() {
        val time = getElapsedHeldDownTime()
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        var m = if (dy != 0) dx.toFloat() / Math.abs(dy).toFloat() else 0f
        m = m.coerceIn(-1f, 1f)

        // GPU: gAgent.moveYaw(-LLFloaterMove.getYawRate(time) * m)

        if (dy > mVertSlopFar) {
            // GPU: gAgent.moveAt(1)
        } else if (dy > mVertSlopNear) {
            if (time < NUDGE_TIME) {
                // GPU: gAgent.moveAtNudge(1)
            } else {
                // GPU: gAgent.moveAt(1)
            }
        } else if (dy < -mVertSlopFar) {
            // GPU: gAgent.moveAt(-1)
        } else if (dy < -mVertSlopNear) {
            if (time < NUDGE_TIME) {
                // GPU: gAgent.moveAtNudge(-1)
            } else {
                // GPU: gAgent.moveAt(-1)
            }
        }
    }
}

class LLJoystickAgentSlide(
    quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN
) : LLJoystick(quadrant) {

    override fun onMouseUp() {
        val time = getElapsedHeldDownTime()
        if (time < NUDGE_TIME) {
            when (mInitialQuadrant) {
                EJoystickQuadrant.JQ_LEFT  -> { /* GPU: gAgent.moveLeftNudge(1) */ }
                EJoystickQuadrant.JQ_RIGHT -> { /* GPU: gAgent.moveLeftNudge(-1) */ }
                else -> {}
            }
        }
    }

    override fun onHeldDown() {
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        if (dx > mHorizSlopNear) {
            // GPU: gAgent.moveLeft(-1)
        } else if (dx < -mHorizSlopNear) {
            // GPU: gAgent.moveLeft(1)
        }

        if (dy > mVertSlopFar) {
            // GPU: gAgent.moveAt(1)
        } else if (dy > mVertSlopNear) {
            // GPU: gAgent.moveAtNudge(1)
        } else if (dy < -mVertSlopFar) {
            // GPU: gAgent.moveAt(-1)
        } else if (dy < -mVertSlopNear) {
            // GPU: gAgent.moveAtNudge(-1)
        }
    }
}

open class LLJoystickCameraRotate(
    quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN
) : LLJoystick(quadrant) {

    protected var mInLeft: Boolean = false
    protected var mInTop: Boolean = false
    protected var mInRight: Boolean = false
    protected var mInBottom: Boolean = false
    protected var mInCenter: Boolean = false
    protected var centerImageName: String = "Cam_Rotate_Center"

    override fun updateSlop() {
        mVertSlopNear = 16;  mVertSlopFar = 32
        mHorizSlopNear = 16; mHorizSlopFar = 32
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        // GPU: gAgent.setMovementLocked(true)
        updateSlop()

        val horizCenter = getRect().getWidth() / 2
        val vertCenter = getRect().getHeight() / 2
        val dx = x - horizCenter
        val dy = y - vertCenter

        if (pointInCenterDot(x, y)) {
            mInitialOffset.x = 0; mInitialOffset.y = 0
            mInitialQuadrant = EJoystickQuadrant.JQ_ORIGIN
            mInCenter = true
            resetJoystickCamera()
        } else if (dy > dx && dy > -dx) {
            mInitialOffset.x = 0
            mInitialOffset.y = (mVertSlopNear + mVertSlopFar) / 2
            mInitialQuadrant = EJoystickQuadrant.JQ_UP
        } else if (dy > dx && dy <= -dx) {
            mInitialOffset.x = -((mHorizSlopNear + mHorizSlopFar) / 2)
            mInitialOffset.y = 0
            mInitialQuadrant = EJoystickQuadrant.JQ_LEFT
        } else if (dy <= dx && dy <= -dx) {
            mInitialOffset.x = 0
            mInitialOffset.y = -((mVertSlopNear + mVertSlopFar) / 2)
            mInitialQuadrant = EJoystickQuadrant.JQ_DOWN
        } else {
            mInitialOffset.x = (mHorizSlopNear + mHorizSlopFar) / 2
            mInitialOffset.y = 0
            mInitialQuadrant = EJoystickQuadrant.JQ_RIGHT
        }

        return super.handleMouseDown(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        // GPU: gAgent.setMovementLocked(false)
        mInCenter = false
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (!pointInCenterDot(x, y)) mInCenter = false
        return super.handleHover(x, y, mask)
    }

    override fun onHeldDown() {
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        if (dx > mHorizSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setOrbitLeftKey(getOrbitRate())")
        } else if (dx < -mHorizSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setOrbitRightKey(getOrbitRate())")
        }

        if (dy > mVertSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setOrbitUpKey(getOrbitRate())")
        } else if (dy < -mVertSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setOrbitDownKey(getOrbitRate())")
        }
    }

    open fun resetJoystickCamera() {
        TODO("GPU: if !gSavedSettings.getBOOL(DisableCameraJoystickCenterReset) then gAgentCamera.resetCameraOrbit()")
    }

    protected fun getOrbitRate(): Float {
        val time = getElapsedHeldDownTime()
        return if (time < NUDGE_TIME) {
            ORBIT_NUDGE_RATE + time * (1f - ORBIT_NUDGE_RATE) / NUDGE_TIME
        } else {
            1f
        }
    }

    open fun setToggleState(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean) {
        mInLeft = left; mInTop = top; mInRight = right; mInBottom = bottom
    }

    open fun draw() {
        TODO("GPU: draw unselected base image; if mInCenter draw center image rotated 0; else draw selected image rotated per active quadrant flags (top=0, right=1, bottom=2, left=3)")
    }

    protected fun drawRotatedImage(image: Any, rotations: Int) {
        TODO("GPU: bind texture, scale UVs to handle image vs texture size difference (EXT-2023), emit two triangles with UV array rotated by 'rotations' positions")
    }
}

class LLJoystickCameraTrack : LLJoystickCameraRotate() {

    init {
        centerImageName = "Cam_Tracking_Center"
    }

    override fun onHeldDown() {
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        if (dx > mVertSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setPanRightKey(getOrbitRate())")
        } else if (dx < -mVertSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setPanLeftKey(getOrbitRate())")
        }

        if (dy > mVertSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setPanUpKey(getOrbitRate())")
        } else if (dy < -mVertSlopNear) {
            TODO("GPU: gAgentCamera.unlockView(); gAgentCamera.setPanDownKey(getOrbitRate())")
        }
    }

    override fun resetJoystickCamera() {
        TODO("GPU: if !gSavedSettings.getBOOL(DisableCameraJoystickCenterReset) then gAgentCamera.resetCameraPan()")
    }
}

class LLJoystickQuaternion(
    quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN
) : LLJoystick(quadrant) {

    private var mInLeft: Boolean = false
    private var mInTop: Boolean = false
    private var mInRight: Boolean = false
    private var mInBottom: Boolean = false

    private val mXAxisIndex: Int = 2  // left & right across control maps to Z world axis
    private val mYAxisIndex: Int = 0  // up & down across control maps to X world axis
    private val mZAxisIndex: Int = 1  // tested for front/back hemisphere

    private val mVectorZero: LLVector3 = LLVector3(0f, 0f, 1f)
    private var mRotation: LLQuaternion = LLQuaternion()
    private var mUpDnAxis: LLVector3 = LLVector3(1f, 0f, 0f)
    private var mLfRtAxis: LLVector3 = LLVector3(0f, 0f, 1f)

    init {
        for (i in 0..2) {
            mLfRtAxis[i] = if (mXAxisIndex == i) 1f else 0f
            mUpDnAxis[i] = if (mYAxisIndex == i) 1f else 0f
        }
    }

    fun setToggleState(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean) {
        mInLeft = left; mInTop = top; mInRight = right; mInBottom = bottom
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        updateSlop()

        val horizCenter = getRect().getWidth() / 2
        val vertCenter = getRect().getHeight() / 2
        val dx = x - horizCenter
        val dy = y - vertCenter

        if (dy > dx && dy > -dx) {
            mInitialOffset.x = 0
            mInitialOffset.y = (mVertSlopNear + mVertSlopFar) / 2
            mInitialQuadrant = EJoystickQuadrant.JQ_UP
        } else if (dy > dx && dy <= -dx) {
            mInitialOffset.x = -((mHorizSlopNear + mHorizSlopFar) / 2)
            mInitialOffset.y = 0
            mInitialQuadrant = EJoystickQuadrant.JQ_LEFT
        } else if (dy <= dx && dy <= -dx) {
            mInitialOffset.x = 0
            mInitialOffset.y = -((mVertSlopNear + mVertSlopFar) / 2)
            mInitialQuadrant = EJoystickQuadrant.JQ_DOWN
        } else {
            mInitialOffset.x = (mHorizSlopNear + mHorizSlopFar) / 2
            mInitialOffset.y = 0
            mInitialQuadrant = EJoystickQuadrant.JQ_RIGHT
        }

        return super.handleMouseDown(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = super.handleMouseUp(x, y, mask)

    override fun onHeldDown() {
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        var axis = LLVector3(0f, 0f, 0f)
        if (dx > mHorizSlopNear)       axis = axis + mUpDnAxis
        else if (dx < -mHorizSlopNear) axis = axis - mUpDnAxis
        if (dy > mVertSlopNear)        axis = axis + mLfRtAxis
        else if (dy < -mVertSlopNear)  axis = axis - mLfRtAxis

        if (axis.isNull()) return

        axis.normalize()

        val delta = LLQuaternion()
        delta.setAngleAxis(0.0523599f, axis)   // ~3 degrees
        mRotation.timesAssign(delta)
        setValue(mRotation.getValue())
        onCommit()
    }

    override fun draw() {
        TODO("GPU: draw unselected image at (0,0); draw rotated selected images for active quadrant flags; project mVectorZero*mRotation into widget space and draw indicator dot via gl_circle_2d")
    }

    override fun updateSlop() {
        mVertSlopNear = 16;  mVertSlopFar = 32
        mHorizSlopNear = 16; mHorizSlopFar = 32
    }

    private fun drawRotatedImage(image: Any, rotations: Int) {
        TODO("GPU: bind texture, scale UVs for image vs texture size, emit two triangles with UV array rotated by 'rotations' positions")
    }

    fun setRotation(value: LLQuaternion) {
        if (value != mRotation) {
            mRotation = value
            mRotation.normalize()
            super.setValue(mRotation.getValue())
        }
    }

    fun getRotation(): LLQuaternion = mRotation
}
