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
            "up" -> JQ_UP
            "down" -> JQ_DOWN
            "left" -> JQ_LEFT
            "right" -> JQ_RIGHT
            else -> JQ_RIGHT
        }

        fun toName(quadrant: EJoystickQuadrant): String = when (quadrant) {
            JQ_ORIGIN -> "origin"
            JQ_UP -> "up"
            JQ_DOWN -> "down"
            JQ_LEFT -> "left"
            JQ_RIGHT -> "right"
        }
    }
}

data class LLCoordGL(var x: Int = 0, var y: Int = 0)

data class LLVector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    operator fun plus(other: LLVector3) = LLVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: LLVector3) = LLVector3(x - other.x, y - other.y, z - other.z)
    operator fun timesAssign(q: LLQuaternion) {
        TODO("GPU: apply quaternion rotation to this vector")
    }
    fun isNull(): Boolean = x == 0f && y == 0f && z == 0f
    fun normalize() { TODO("GPU: normalise vector in-place") }
    operator fun get(index: Int): Float = when (index) { 0 -> x; 1 -> y; 2 -> z; else -> 0f }
    operator fun set(index: Int, v: Float) = when (index) { 0 -> x = v; 1 -> y = v; else -> z = v }
}

data class LLQuaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun setAngleAxis(angle: Float, axis: LLVector3) { TODO("GPU: set this quaternion from angle-axis") }
    fun normalize() { TODO("GPU: normalise quaternion in-place") }
    fun getValue(): Any { TODO("GPU: return LLSD representation of this quaternion") }
    operator fun timesAssign(other: LLQuaternion) { TODO("GPU: multiply this quaternion by other in-place") }
}

abstract class LLButton(
    protected val initialQuadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN
) {
    protected var heldDownCallback: ((Any) -> Unit)? = null

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: base button mouse-down")
    }
    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: base button mouse-up")
    }
    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: base button hover")
    }
    fun hasMouseCapture(): Boolean { TODO("GPU: check if this widget holds mouse capture") }
    fun getRect(): LLRect { TODO("GPU: return widget rect") }
    fun getLocalRect(): LLRect { TODO("GPU: return widget local rect") }
    fun getHeldDownTime(): Float { TODO("GPU: return seconds since mouse-down") }
    fun getImageUnselected(): Any { TODO("GPU: return unselected image") }
    fun getImageSelected(): Any { TODO("GPU: return selected image") }
    fun setValue(value: Any) { TODO("GPU: set widget value") }
    fun onCommit() { TODO("GPU: fire commit signal") }
    fun setHeldDownCallback(cb: (Any) -> Unit, userdata: Any) {
        heldDownCallback = cb
    }
    protected val mouseDownTimer: Any get() = TODO("GPU: return mouse-down frame timer")
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
            EJoystickQuadrant.JQ_ORIGIN -> { mInitialOffset.x = 0; mInitialOffset.y = 0 }
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
        val a = getLocalRect().left + getLocalRect().getWidth() / 2
        val b = getLocalRect().bottom + getLocalRect().getHeight() / 2
        if (centerDotXRad == 0 || centerDotYRad == 0) return false
        val result = (((x - a) * (x - a)).toDouble() / (centerDotXRad * centerDotXRad) +
                ((y - b) * (y - b)).toDouble() / (centerDotYRad * centerDotYRad))
        return result <= 1.0
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (pointInCircle(x, y)) {
            mLastMouse.x = x; mLastMouse.y = y
            mFirstMouse.x = x; mFirstMouse.y = y
            TODO("GPU: reset mouseDownTimer")
            return super.handleMouseDown(x, y, mask)
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

class LLJoystickAgentTurn(quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN) : LLJoystick(quadrant) {
    override fun onHeldDown() {
        val time = getElapsedHeldDownTime()
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        var m = if (dy != 0) dx.toFloat() / Math.abs(dy).toFloat() else 0f
        m = m.coerceIn(-1f, 1f)

        TODO("GPU: gAgent.moveYaw(-yawRate * m)")

        if (dy > mVertSlopFar) {
            TODO("GPU: gAgent.moveAt(1)")
        } else if (dy > mVertSlopNear) {
            if (time < NUDGE_TIME) {
                TODO("GPU: gAgent.moveAtNudge(1)")
            } else {
                TODO("GPU: gAgent.moveAt(1)")
            }
        } else if (dy < -mVertSlopFar) {
            TODO("GPU: gAgent.moveAt(-1)")
        } else if (dy < -mVertSlopNear) {
            if (time < NUDGE_TIME) {
                TODO("GPU: gAgent.moveAtNudge(-1)")
            } else {
                TODO("GPU: gAgent.moveAt(-1)")
            }
        }
    }
}

class LLJoystickAgentSlide(quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN) : LLJoystick(quadrant) {
    override fun onMouseUp() {
        val time = getElapsedHeldDownTime()
        if (time < NUDGE_TIME) {
            when (mInitialQuadrant) {
                EJoystickQuadrant.JQ_LEFT -> TODO("GPU: gAgent.moveLeftNudge(1)")
                EJoystickQuadrant.JQ_RIGHT -> TODO("GPU: gAgent.moveLeftNudge(-1)")
                else -> {}
            }
        }
    }

    override fun onHeldDown() {
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        if (dx > mHorizSlopNear) {
            TODO("GPU: gAgent.moveLeft(-1)")
        } else if (dx < -mHorizSlopNear) {
            TODO("GPU: gAgent.moveLeft(1)")
        }

        if (dy > mVertSlopFar) {
            TODO("GPU: gAgent.moveAt(1)")
        } else if (dy > mVertSlopNear) {
            TODO("GPU: gAgent.moveAtNudge(1)")
        } else if (dy < -mVertSlopFar) {
            TODO("GPU: gAgent.moveAt(-1)")
        } else if (dy < -mVertSlopNear) {
            TODO("GPU: gAgent.moveAtNudge(-1)")
        }
    }
}

open class LLJoystickCameraRotate(quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN) : LLJoystick(quadrant) {
    protected var mInLeft: Boolean = false
    protected var mInTop: Boolean = false
    protected var mInRight: Boolean = false
    protected var mInBottom: Boolean = false
    protected var mInCenter: Boolean = false
    protected var centerImageName: String = "Cam_Rotate_Center"

    override fun updateSlop() {
        mVertSlopNear = 16
        mVertSlopFar = 32
        mHorizSlopNear = 16
        mHorizSlopFar = 32
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: gAgent.setMovementLocked(true)")
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
        TODO("GPU: gAgent.setMovementLocked(false)")
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
        TODO("GPU: if !DisableCameraJoystickCenterReset then gAgentCamera.resetCameraOrbit()")
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
        TODO("GPU: draw unselected image; if mInCenter draw centerImage; else draw rotated selected images for each active quadrant")
    }

    protected fun drawRotatedImage(image: Any, rotations: Int) {
        TODO("GPU: bind texture, emit two triangles with UV coordinates rotated by 'rotations' * 90 degrees")
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
        TODO("GPU: if !DisableCameraJoystickCenterReset then gAgentCamera.resetCameraPan()")
    }
}

class LLJoystickQuaternion(quadrant: EJoystickQuadrant = EJoystickQuadrant.JQ_ORIGIN) : LLJoystick(quadrant) {
    private var mInLeft: Boolean = false
    private var mInTop: Boolean = false
    private var mInRight: Boolean = false
    private var mInBottom: Boolean = false

    private val mXAxisIndex: Int = 2
    private val mYAxisIndex: Int = 0
    private val mZAxisIndex: Int = 1

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
        var axis = LLVector3(0f, 0f, 0f)
        updateSlop()

        val dx = mLastMouse.x - mFirstMouse.x + mInitialOffset.x
        val dy = mLastMouse.y - mFirstMouse.y + mInitialOffset.y

        if (dx > mHorizSlopNear) {
            axis = axis + mUpDnAxis
        } else if (dx < -mHorizSlopNear) {
            axis = axis - mUpDnAxis
        }

        if (dy > mVertSlopNear) {
            axis = axis + mLfRtAxis
        } else if (dy < -mVertSlopNear) {
            axis = axis - mLfRtAxis
        }

        if (axis.isNull()) return

        axis.normalize()

        val delta = LLQuaternion()
        delta.setAngleAxis(0.0523599f, axis)
        mRotation.timesAssign(delta)
        setValue(mRotation.getValue())
        onCommit()
    }

    override fun draw() {
        TODO("GPU: draw unselected image; draw rotated selected images for active quadrants; draw indicator dot via gl_circle_2d at projected mVectorZero * mRotation position")
    }

    protected override fun updateSlop() {
        mVertSlopNear = 16; mVertSlopFar = 32
        mHorizSlopNear = 16; mHorizSlopFar = 32
    }

    private fun getOrbitRate(): Float = 1f

    private fun drawRotatedImage(image: Any, rotations: Int) {
        TODO("GPU: bind texture, emit two triangles with UV coordinates rotated by 'rotations' * 90 degrees")
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
