package com.firestorm.llui

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

private val VECTOR_ZERO = FloatArray(3) { if (it == 0) 1f else 0f }
private const val F_PI = Math.PI.toFloat()
private const val F_TWO_PI = (Math.PI * 2).toFloat()
private const val F_PI_BY_TWO = (Math.PI / 2).toFloat()
private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()
private const val RAD_TO_DEG = (180.0 / Math.PI).toFloat()

data class Quaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun normalize(): Quaternion {
        val mag = sqrt(x * x + y * y + z * z + w * w)
        return if (mag > 1e-6f) copy(x = x / mag, y = y / mag, z = z / mag, w = w / mag) else this
    }

    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            w * other.x + x * other.w + y * other.z - z * other.y,
            w * other.y - x * other.z + y * other.w + z * other.x,
            w * other.z + x * other.y - y * other.x + z * other.w,
            w * other.w - x * other.x - y * other.y - z * other.z
        )
    }

    fun setAngleAxis(angle: Float, ax: Float, ay: Float, az: Float): Quaternion {
        val half = angle / 2f
        val s = kotlin.math.sin(half)
        return copy(x = ax * s, y = ay * s, z = az * s, w = kotlin.math.cos(half))
    }

    fun rotateVector(v: FloatArray): FloatArray {
        val qv = Quaternion(v[0], v[1], v[2], 0f)
        val inv = copy(x = -x, y = -y, z = -z)
        val rotated = this * qv * inv
        return floatArrayOf(rotated.x, rotated.y, rotated.z)
    }

    fun toSdArray(): FloatArray = floatArrayOf(x, y, z, w)

    companion object {
        fun fromSdArray(arr: FloatArray): Quaternion = Quaternion(arr[0], arr[1], arr[2], arr[3])
    }
}

enum class ThumbMode { SUN, MOON }
enum class DragMode { DRAG_SET, DRAG_SCROLL }

class VirtualTrackball(
    val incrementMouse: Float,
    val incrementBtn: Float,
    thumbModeStr: String = "sun"
) {

    var labelN: Any? = null
    var labelE: Any? = null
    var labelS: Any? = null
    var labelW: Any? = null

    var btnRotateTop: Any? = null
    var btnRotateBottom: Any? = null
    var btnRotateLeft: Any? = null
    var btnRotateRight: Any? = null

    var touchArea: SimpleRect = SimpleRect()
    var border: ViewBorder? = null

    private var prevX: Int = 0
    private var prevY: Int = 0

    private var imgMoonBack: Any? = null
    private var imgMoonFront: Any? = null
    private var imgSunBack: Any? = null
    private var imgSunFront: Any? = null
    private var imgSphere: Any? = null
    private var imgBtnRotTop: Any? = null
    private var imgBtnRotLeft: Any? = null
    private var imgBtnRotRight: Any? = null
    private var imgBtnRotBottom: Any? = null

    private var value: Quaternion = Quaternion()
    private var thumbMode: ThumbMode = if (thumbModeStr == "moon") ThumbMode.MOON else ThumbMode.SUN
    private var dragMode: DragMode = DragMode.DRAG_SET

    private var mouseCapture: Boolean = false
    private var enabled: Boolean = true

    fun postBuild(): Boolean = true

    fun draw() {
        val drawPoint = value.rotateVector(VECTOR_ZERO)

        val halfWidth = touchArea.width / 2
        val halfHeight = touchArea.height / 2
        val px = ((drawPoint[0] + 1f) * halfWidth + touchArea.left).toInt()
        val py = ((drawPoint[1] + 1f) * halfHeight + touchArea.bottom).toInt()
        val upperHemisphere = drawPoint[2] >= 0f

        // no-op: GPU: draw sphere image at touchArea with alpha modifier if lower hemisphere
        drawThumb(px, py, thumbMode, upperHemisphere)

        val labelsVisible = enabled
        // no-op: GPU: set label visibility to $labelsVisible for N/E/S/W labels; call LLView::draw()
    }

    private fun drawThumb(x: Int, y: Int, mode: ThumbMode, upperHemi: Boolean) {
        val imgDesc = when (mode) {
            ThumbMode.SUN  -> if (upperHemi) "imgSunFront"  else "imgSunBack"
            ThumbMode.MOON -> if (upperHemi) "imgMoonFront" else "imgMoonBack"
        }
        // no-op: GPU: draw thumb image '$imgDesc' centered at ($x, $y)
    }

    private fun pointInTouchCircle(x: Int, y: Int): Boolean {
        val cx = touchArea.centerX
        val cy = touchArea.centerY
        val radius = touchArea.width / 2
        return (x - cx).toFloat().pow(2) + (y - cy).toFloat().pow(2) <= radius.toFloat().pow(2)
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (mouseCapture) {
            if (dragMode == DragMode.DRAG_SCROLL) {
                val rotX = (x - prevX).toFloat()
                val rotY = (y - prevY).toFloat()

                if (abs(rotX) > 1f) {
                    val direction = if (rotX < 0) -1f else 1f
                    val delta = Quaternion().setAngleAxis(incrementMouse * abs(rotX), 0f, direction, 0f)
                    value = (value * delta).normalize()
                }
                if (abs(rotY) > 1f) {
                    val direction = if (rotY < 0) 1f else -1f
                    val delta = Quaternion().setAngleAxis(incrementMouse * abs(rotY), direction, 0f, 0f)
                    value = (value * delta).normalize()
                }
            } else {
                if (!pointInTouchCircle(x, y)) return true

                val radius = touchArea.width / 2f
                val xx = (x - touchArea.centerX).toFloat()
                val yy = (y - touchArea.centerY).toFloat()
                val dist = sqrt(xx.pow(2) + yy.pow(2))

                var azimuth = acos((xx / dist).coerceIn(-1f, 1f))
                var altitude = acos((dist / radius).coerceIn(-1f, 1f))

                if (yy < 0f) azimuth = F_TWO_PI - azimuth

                val drawPoint = value.rotateVector(VECTOR_ZERO)
                if (drawPoint[2] >= 0f) {
                    if (altitude < 1e-6f) altitude = 1e-6f
                    altitude *= -1f
                }

                value = Quaternion().setAngleAxis(altitude, 0f, 1f, 0f)
                val azQuat = Quaternion().setAngleAxis(azimuth, 0f, 0f, 1f)
                value = value * azQuat
                value = value.normalize()
            }

            prevX = x
            prevY = y
            onCommit()
        }
        return true
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (mouseCapture) {
            prevX = 0
            prevY = 0
            mouseCapture = false
            System.err.println("VirtualTrackball: make_ui_sound(UISndClickRelease) not yet implemented")
        }
        return false
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (pointInTouchCircle(x, y)) {
            prevX = x
            prevY = y
            mouseCapture = true
            dragMode = if (mask == MASK_CONTROL) DragMode.DRAG_SCROLL else DragMode.DRAG_SET
            System.err.println("VirtualTrackball: make_ui_sound(UISndClick) not yet implemented")
        }
        return false
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean = false

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        return when (key) {
            KEY_DOWN  -> { onRotateTopClick();    true }
            KEY_LEFT  -> { onRotateRightClick();  true }
            KEY_UP    -> { onRotateBottomClick(); true }
            KEY_RIGHT -> { onRotateLeftClick();   true }
            else      -> false
        }
    }

    fun setValue(arr: FloatArray) {
        if (arr.size == 4) {
            value = Quaternion.fromSdArray(arr)
        }
    }

    fun setValue(x: Float, y: Float, z: Float, w: Float) {
        value = Quaternion(x, y, z, w)
    }

    fun getValue(): FloatArray = value.toSdArray()

    fun setRotation(q: Quaternion) { value = q }
    fun getRotation(): Quaternion = value

    private fun setValueAndCommit(q: Quaternion) {
        value = q
        onCommit()
    }

    private fun onCommit() {
        System.err.println("VirtualTrackball: onCommit not yet implemented")
    }

    private fun onRotateTopClick() {
        if (enabled) {
            val delta = Quaternion().setAngleAxis(incrementBtn, 1f, 0f, 0f)
            value = (value * delta).normalize()
            setValueAndCommit(value)
        }
    }

    private fun onRotateBottomClick() {
        if (enabled) {
            val delta = Quaternion().setAngleAxis(incrementBtn, -1f, 0f, 0f)
            value = (value * delta).normalize()
            setValueAndCommit(value)
        }
    }

    private fun onRotateLeftClick() {
        if (enabled) {
            val delta = Quaternion().setAngleAxis(incrementBtn, 0f, 1f, 0f)
            value = (value * delta).normalize()
            setValueAndCommit(value)
        }
    }

    private fun onRotateRightClick() {
        if (enabled) {
            val delta = Quaternion().setAngleAxis(incrementBtn, 0f, -1f, 0f)
            value = (value * delta).normalize()
            setValueAndCommit(value)
        }
    }

    private fun onRotateTopMouseEnter()    { /* no-op */ }
    private fun onRotateBottomMouseEnter() { /* no-op */ }
    private fun onRotateLeftMouseEnter()   { /* no-op */ }
    private fun onRotateRightMouseEnter()  { /* no-op */ }

    companion object {
        const val MASK_CONTROL = 0x01
        const val KEY_DOWN  = 0x28
        const val KEY_LEFT  = 0x25
        const val KEY_UP    = 0x26
        const val KEY_RIGHT = 0x27

        fun getAzimuthAndElevation(quat: Quaternion): Pair<Float, Float> {
            val point = quat.rotateVector(VECTOR_ZERO)
            val azimuth = if (abs(point[0]) > 1e-6f || abs(point[1]) > 1e-6f) {
                atan2(point[0], point[1])
            } else 0f

            val adjustedAzimuth = (azimuth - F_PI_BY_TWO).let {
                if (it < 0f) it + F_TWO_PI else it
            }

            val z = point[2].coerceIn(-1f, 1f)
            val elevation = asin(z)
            return Pair(adjustedAzimuth, elevation)
        }

        fun getAzimuthAndElevationDeg(quat: Quaternion): Pair<Float, Float> {
            val (az, el) = getAzimuthAndElevation(quat)
            return Pair(az * RAD_TO_DEG, el * RAD_TO_DEG)
        }
    }
}
