package com.firestorm.newview

// Bit-mask constants mirroring MASK_CONTROL/MASK_SHIFT/MASK_ALT from the viewer.
const val MASK_CONTROL: Int = 0x0001
const val MASK_SHIFT: Int   = 0x0002
const val MASK_ALT: Int     = 0x0004

// Rect helper (pixel coords; mBottom < mTop in SL convention).
data class Rect(val mLeft: Int, val mBottom: Int, val mRight: Int, val mTop: Int) {
    fun getCenterX(): Int = (mLeft + mRight) / 2
    fun getCenterY(): Int = (mBottom + mTop) / 2
    fun getWidth(): Int   = mRight - mLeft
    fun getHeight(): Int  = mTop - mBottom
    fun localPointInRect(x: Int, y: Int): Boolean =
        x in mLeft..mRight && y in mBottom..mTop
}

// Host-provided image abstraction.
interface UIImage {
    fun getWidth(): Int
    fun getHeight(): Int
    fun draw(rect: Rect): Unit
    fun draw(rect: Rect, alpha: Float): Unit
}

// Host-provided touch-area panel abstraction.
interface TouchAreaPanel {
    fun getRect(): Rect
    fun isInEnabledChain(): Boolean
}

class FSVirtualTrackpad(
    private val allowPinchMode: Boolean = false,
    private val infiniteScrollMode: Boolean = false,
    private val imgMoonBack: UIImage? = null,
    private val imgMoonFront: UIImage? = null,
    private val imgSunBack: UIImage? = null,
    private val imgSunFront: UIImage? = null,
    private val imgSphere: UIImage? = null,
    initialRect: Rect = Rect(0, 0, 256, 256)
) {
    private val thirdAxisQuantization: Float = 0.001f
    private val wheelClickQuanta: Int = 10

    var touchArea: TouchAreaPanel? = null

    private var mAllowPinchMode: Boolean = allowPinchMode
    private var mDoingPinchMode: Boolean = false
    private var mInfiniteScrollMode: Boolean = infiniteScrollMode
    private var mHeldDownControlBefore: Boolean = false

    private var mWheelClicksSinceLastDelta: Int = 0

    private var mValueX: Int = 0
    private var mValueY: Int = 0
    private var mValueZ: Int = 0
    private var mPinchValueX: Int = 0
    private var mPinchValueY: Int = 0
    private var mPinchValueZ: Int = 0

    private var mValueDeltaX: Int = 0
    private var mValueDeltaY: Int = 0
    private var mValueDeltaZ: Int = 0
    private var mPinchValueDeltaX: Int = 0
    private var mPinchValueDeltaY: Int = 0
    private var mPinchValueDeltaZ: Int = 0

    private var mCursorValueX: Int = 0
    private var mCursorValueY: Int = 0
    private var mCursorValueZ: Int = 0
    private var mPinchCursorValueX: Int = 0
    private var mPinchCursorValueY: Int = 0
    private var mPinchCursorValueZ: Int = 0

    private var mThumbClickOffsetX: Int = 0
    private var mThumbClickOffsetY: Int = 0
    private var mPinchThumbClickOffsetX: Int = 0
    private var mPinchThumbClickOffsetY: Int = 0
    private var mPosXwhenCtrlDown: Int = 0
    private var mPosYwhenCtrlDown: Int = 0

    init {
        val centerX = initialRect.getCenterX()
        val centerY = initialRect.getCenterY()
        mCursorValueX = centerX;  mPinchCursorValueX = centerX
        mCursorValueY = centerY;  mPinchCursorValueY = centerY
        mCursorValueZ = 0;        mPinchCursorValueZ = 0
    }

    fun postBuild(): Boolean = true

    fun drawThumb(isPinchThumb: Boolean) {
        val area = touchArea ?: return
        val thumb: UIImage? = if (area.isInEnabledChain()) {
            if (isPinchThumb) imgSunFront else imgMoonFront
        } else {
            if (isPinchThumb) imgSunBack else imgMoonBack
        }
        thumb ?: return

        var x = if (isPinchThumb) mPinchCursorValueX else mCursorValueX
        var y = if (isPinchThumb) mPinchCursorValueY else mCursorValueY
        val wrapped = wrapOrClipCursorPosition(x, y)
        x = wrapped.first; y = wrapped.second

        val hw = thumb.getWidth() / 2
        val hh = thumb.getHeight() / 2
        thumb.draw(Rect(x - hw, y - hh, x + hw, y + hh))
    }

    fun isPointInTouchArea(x: Int, y: Int): Boolean =
        touchArea?.getRect()?.localPointInRect(x, y) ?: false

    fun determineThumbClickError(x: Int, y: Int) {
        val front = imgSunFront ?: return
        mThumbClickOffsetX = 0
        mThumbClickOffsetY = 0

        val (ex, ey) = wrapOrClipCursorPosition(mCursorValueX, mCursorValueY)
        val errorX = ex - x
        val errorY = ey - y

        if (kotlin.math.abs(errorX) > front.getWidth() / 2.0) return
        if (kotlin.math.abs(errorY) > front.getHeight() / 2.0) return

        mThumbClickOffsetX = errorX
        mThumbClickOffsetY = errorY
    }

    fun updateClickErrorIfInfiniteScrolling() {
        if (!mInfiniteScrollMode) return
        val rect = touchArea?.getRect() ?: return

        var errorX = mCursorValueX
        var errorY = mCursorValueY

        while (errorX > rect.mRight)  { errorX -= rect.getWidth();  mThumbClickOffsetX += rect.getWidth() }
        while (errorX < rect.mLeft)   { errorX += rect.getWidth();  mThumbClickOffsetX -= rect.getWidth() }
        while (errorY > rect.mTop)    { errorY -= rect.getHeight(); mThumbClickOffsetY += rect.getHeight() }
        while (errorY < rect.mBottom) { errorY += rect.getHeight(); mThumbClickOffsetY -= rect.getHeight() }
    }

    fun determineThumbClickErrorForPinch(x: Int, y: Int) {
        val front = imgMoonFront ?: return
        mPinchThumbClickOffsetX = 0
        mPinchThumbClickOffsetY = 0

        val (ex, ey) = wrapOrClipCursorPosition(mPinchCursorValueX, mPinchCursorValueY)
        val errorX = ex - x
        val errorY = ey - y

        if (kotlin.math.abs(errorX) > front.getWidth() / 2.0) return
        if (kotlin.math.abs(errorY) > front.getHeight() / 2.0) return

        mPinchThumbClickOffsetX = errorX
        mPinchThumbClickOffsetY = errorY
    }

    fun updateClickErrorIfInfiniteScrollingForPinch() {
        if (!mInfiniteScrollMode) return
        val rect = touchArea?.getRect() ?: return

        var errorX = mCursorValueX
        var errorY = mCursorValueY

        while (errorX > rect.mRight)  { errorX -= rect.getWidth();  mPinchThumbClickOffsetX += rect.getWidth() }
        while (errorX < rect.mLeft)   { errorX += rect.getWidth();  mPinchThumbClickOffsetX -= rect.getWidth() }
        while (errorY > rect.mTop)    { errorY -= rect.getHeight(); mPinchThumbClickOffsetY += rect.getHeight() }
        while (errorY < rect.mBottom) { errorY += rect.getHeight(); mPinchThumbClickOffsetY -= rect.getHeight() }
    }

    fun draw() {
        val area = touchArea ?: return
        val alpha = if (area.isInEnabledChain()) 1.0f else 0.5f
        imgSphere?.draw(area.getRect(), alpha)

        if (mAllowPinchMode) drawThumb(true)
        drawThumb(false)
    }

    fun setValue(value: List<Float>) {
        when (value.size) {
            2 -> setValue(value[0], value[1], 0f)
            3 -> setValue(value[0], value[1], value[2])
        }
    }

    fun setValue(x: Float, y: Float, z: Float) {
        val (px, py, pz) = convertNormalizedToPixelPos(x, y, z)
        mCursorValueX = px; mCursorValueY = py; mCursorValueZ = pz
        mValueX = px;       mValueY = py;       mValueZ = pz
    }

    fun setPinchValue(x: Float, y: Float, z: Float) {
        val (px, py, pz) = convertNormalizedToPixelPos(x, y, z)
        mPinchCursorValueX = px; mPinchCursorValueY = py; mPinchCursorValueZ = pz
        mPinchValueX = px;       mPinchValueY = py;       mPinchValueZ = pz
    }

    fun getValue(): Triple<Float, Float, Float>       = normalizePixelPos(mValueX, mValueY, mValueZ)
    fun getValueDelta(): Triple<Float, Float, Float>  = normalizeDelta(mValueDeltaX, mValueDeltaY, mValueDeltaZ)
    fun getPinchValue(): Triple<Float, Float, Float>  = normalizePixelPos(mPinchValueX, mPinchValueY, mPinchValueZ)
    fun getPinchValueDelta(): Triple<Float, Float, Float> = normalizeDelta(mPinchValueDeltaX, mPinchValueDeltaY, mPinchValueDeltaZ)

    private fun wrapOrClipCursorPosition(x: Int, y: Int): Pair<Int, Int> {
        val rect = touchArea?.getRect() ?: return Pair(x, y)
        return if (mInfiniteScrollMode) {
            var cx = x; var cy = y
            while (cx > rect.mRight)  cx -= rect.getWidth()
            while (cx < rect.mLeft)   cx += rect.getWidth()
            while (cy > rect.mTop)    cy -= rect.getHeight()
            while (cy < rect.mBottom) cy += rect.getHeight()
            Pair(cx, cy)
        } else {
            Pair(
                x.coerceIn(rect.mLeft, rect.mRight),
                y.coerceIn(rect.mBottom, rect.mTop)
            )
        }
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (!hasMouseCapture()) return true

        val (deltaX, deltaY, deltaZ) = getHoverMovementDeltas(x, y, mask)
        applyHoverMovementDeltas(deltaX, deltaY, mask)
        applyDeltasToValues(deltaX, deltaY, mask)
        applyDeltasToDeltaValues(deltaX, deltaY, deltaZ, mask)

        onCommit()
        return true
    }

    private fun getHoverMovementDeltas(x: Int, y: Int, mask: Int): Triple<Int, Int, Int> {
        val fromX = if (mDoingPinchMode) mPinchCursorValueX else mCursorValueX
        val fromY = if (mDoingPinchMode) mPinchCursorValueY else mCursorValueY

        val deltaZ = mWheelClicksSinceLastDelta
        mWheelClicksSinceLastDelta = 0

        val deltaX: Int
        val deltaY: Int

        if (mask and MASK_CONTROL != 0) {
            if (!mHeldDownControlBefore) {
                mPosXwhenCtrlDown = x
                mPosYwhenCtrlDown = y
                mHeldDownControlBefore = true
            }
            val offsetX = if (mDoingPinchMode) mPinchThumbClickOffsetX else mThumbClickOffsetX
            val offsetY = if (mDoingPinchMode) mPinchThumbClickOffsetY else mThumbClickOffsetY
            // Ctrl held: scale movement to 1/8 of raw delta for fine-grained control
            deltaX = mPosXwhenCtrlDown - (mPosXwhenCtrlDown - x) / 8 + offsetX - fromX
            deltaY = mPosYwhenCtrlDown - (mPosYwhenCtrlDown - y) / 8 + offsetY - fromY
        } else {
            if (mHeldDownControlBefore) {
                mThumbClickOffsetX = fromX - x
                mThumbClickOffsetY = fromY - y
                mHeldDownControlBefore = false
            }
            val offsetX = if (mDoingPinchMode) mPinchThumbClickOffsetX else mThumbClickOffsetX
            val offsetY = if (mDoingPinchMode) mPinchThumbClickOffsetY else mThumbClickOffsetY
            deltaX = x + offsetX - fromX
            deltaY = y + offsetY - fromY
        }

        return Triple(deltaX, deltaY, deltaZ)
    }

    private fun applyHoverMovementDeltas(deltaX: Int, deltaY: Int, mask: Int) {
        if (mDoingPinchMode) {
            mPinchCursorValueX += deltaX
            mPinchCursorValueY += deltaY
            if (!mInfiniteScrollMode) {
                val (cx, cy) = wrapOrClipCursorPosition(mPinchCursorValueX, mPinchCursorValueY)
                mPinchCursorValueX = cx; mPinchCursorValueY = cy
            }
        } else {
            mCursorValueX += deltaX
            mCursorValueY += deltaY
            if (!mInfiniteScrollMode) {
                val (cx, cy) = wrapOrClipCursorPosition(mCursorValueX, mCursorValueY)
                mCursorValueX = cx; mCursorValueY = cy
            }
        }
    }

    private fun applyDeltasToValues(deltaX: Int, deltaY: Int, mask: Int) {
        if (mDoingPinchMode) {
            when (mask and (MASK_SHIFT or MASK_ALT)) {
                MASK_ALT   -> { mPinchValueY += deltaY; mPinchValueZ += deltaX }
                MASK_SHIFT -> { mPinchValueX += deltaX; mPinchValueZ += deltaY }
                else       -> { mPinchValueX += deltaX; mPinchValueY += deltaY }
            }
        } else {
            when (mask and (MASK_SHIFT or MASK_ALT)) {
                MASK_ALT   -> { mValueY += deltaY; mValueZ += deltaX }
                MASK_SHIFT -> { mValueX += deltaX; mValueZ += deltaY }
                else       -> { mValueX += deltaX; mValueY += deltaY }
            }
        }
    }

    private fun applyDeltasToDeltaValues(deltaX: Int, deltaY: Int, deltaZ: Int, mask: Int) {
        if (mDoingPinchMode) {
            when (mask and (MASK_SHIFT or MASK_ALT)) {
                MASK_ALT   -> { mPinchValueDeltaX = deltaZ; mPinchValueDeltaY = deltaY; mPinchValueDeltaZ = deltaX }
                MASK_SHIFT -> { mPinchValueDeltaX = deltaX; mPinchValueDeltaY = deltaZ; mPinchValueDeltaZ = deltaY }
                else       -> { mPinchValueDeltaX = deltaX; mPinchValueDeltaY = deltaY; mPinchValueDeltaZ = deltaZ }
            }
        } else {
            when (mask and (MASK_SHIFT or MASK_ALT)) {
                MASK_ALT   -> { mValueDeltaX = deltaZ; mValueDeltaY = deltaY; mValueDeltaZ = deltaX }
                MASK_SHIFT -> { mValueDeltaX = deltaX; mValueDeltaY = deltaZ; mValueDeltaZ = deltaY }
                else       -> { mValueDeltaX = deltaX; mValueDeltaY = deltaY; mValueDeltaZ = deltaZ }
            }
        }
    }

    private fun normalizePixelPos(x: Int, y: Int, z: Int): Triple<Float, Float, Float> {
        val rect = touchArea?.getRect() ?: return Triple(0f, 0f, 0f)
        val cx = rect.getCenterX(); val cy = rect.getCenterY()
        val w = rect.getWidth();    val h = rect.getHeight()
        return Triple(
            (x - cx).toFloat() / w * 2f,
            (y - cy).toFloat() / h * 2f,
            z.toFloat() * thirdAxisQuantization
        )
    }

    private fun normalizeDelta(x: Int, y: Int, z: Int): Triple<Float, Float, Float> {
        val rect = touchArea?.getRect() ?: return Triple(0f, 0f, 0f)
        val w = rect.getWidth(); val h = rect.getHeight()
        return Triple(
            x.toFloat() / w * 2f,
            y.toFloat() / h * 2f,
            z.toFloat() * thirdAxisQuantization
        )
    }

    private fun convertNormalizedToPixelPos(x: Float, y: Float, z: Float): Triple<Int, Int, Int> {
        val rect = touchArea?.getRect() ?: return Triple(0, 0, 0)
        val cx = rect.getCenterX(); val cy = rect.getCenterY()
        val w = rect.getWidth();    val h = rect.getHeight()
        val px = if (mInfiniteScrollMode) {
            cx + kotlin.math.roundToInt(x * w / 2f)
        } else {
            cx + kotlin.math.roundToInt(x.coerceIn(-1f, 1f) * w / 2f)
        }
        val py = if (mInfiniteScrollMode) {
            cy + kotlin.math.roundToInt(y * h / 2f)
        } else {
            cy + kotlin.math.roundToInt(y.coerceIn(-1f, 1f) * h / 2f)
        }
        val pz = kotlin.math.roundToInt(z / thirdAxisQuantization)
        return Triple(px, py, pz)
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            releaseMouseCapture()
            mHeldDownControlBefore = false
        }
        return false
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (isPointInTouchArea(x, y)) {
            mWheelClicksSinceLastDelta = 0
            determineThumbClickError(x, y)
            updateClickErrorIfInfiniteScrolling()
            acquireMouseCapture()
        }
        return false
    }

    fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (hasMouseCapture()) {
            mDoingPinchMode = false
            releaseMouseCapture()
        }
        return false
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (!mAllowPinchMode) return false

        if (isPointInTouchArea(x, y)) {
            determineThumbClickErrorForPinch(x, y)
            updateClickErrorIfInfiniteScrollingForPinch()
            mDoingPinchMode = true
            acquireMouseCapture()
        }
        return false
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (hasMouseCapture() || isPointInTouchArea(x, y)) {
            val mask = getCurrentKeyboardMask()

            var changeAmount = wheelClickQuanta
            if (mask and MASK_CONTROL != 0) changeAmount /= 5

            mWheelClicksSinceLastDelta = -1 * clicks * changeAmount

            mValueDeltaX = 0; mValueDeltaY = 0; mValueDeltaZ = 0
            mPinchValueDeltaX = 0; mPinchValueDeltaY = 0; mPinchValueDeltaZ = 0

            if (mDoingPinchMode) {
                when (mask and (MASK_SHIFT or MASK_ALT)) {
                    MASK_ALT   -> { mPinchValueX += mWheelClicksSinceLastDelta; mPinchValueDeltaX = mWheelClicksSinceLastDelta }
                    MASK_SHIFT -> { mPinchValueY += mWheelClicksSinceLastDelta; mPinchValueDeltaY = mWheelClicksSinceLastDelta }
                    else       -> { mPinchValueZ += mWheelClicksSinceLastDelta; mPinchValueDeltaZ = mWheelClicksSinceLastDelta }
                }
            } else {
                when (mask and (MASK_SHIFT or MASK_ALT)) {
                    MASK_ALT   -> { mValueX += mWheelClicksSinceLastDelta; mValueDeltaX = mWheelClicksSinceLastDelta }
                    MASK_SHIFT -> { mValueY += mWheelClicksSinceLastDelta; mValueDeltaY = mWheelClicksSinceLastDelta }
                    else       -> { mValueZ += mWheelClicksSinceLastDelta; mValueDeltaZ = mWheelClicksSinceLastDelta }
                }
            }

            if (!hasMouseCapture()) onCommit()
            return true
        }
        return false
    }

    private fun hasMouseCapture(): Boolean {
        return false
    }

    private fun acquireMouseCapture() {
        System.err.println("FSVirtualTrackpad: acquireMouseCapture not yet implemented")
    }

    private fun releaseMouseCapture() {
        System.err.println("FSVirtualTrackpad: releaseMouseCapture not yet implemented")
    }

    private fun getCurrentKeyboardMask(): Int {
        return 0
    }

    private fun onCommit() {
        System.err.println("FSVirtualTrackpad: onCommit not yet implemented")
    }
}

private fun kotlin.math.roundToInt(f: Float): Int = kotlin.math.round(f)
