package com.firestorm.newview

import com.firestorm.llmath.Camera
import com.firestorm.llmath.Matrix4
import com.firestorm.llmath.Quaternion
import kotlin.math.tan

object ViewerCamera : Camera() {

    var fovH: Float = 1.047f
    var aspect: Float = 1.778f
    var nearClip: Float = 0.1f
    var farClip: Float = 512f
    var pixelMeterRatio: Float = 1f
    var viewHeightInPixels: Int = 1080

    private var defaultFov: Float = CameraConstants.DEFAULT_FIELD_OF_VIEW
    private var prevDefaultFov: Float = CameraConstants.DEFAULT_FIELD_OF_VIEW
    private var cosHalfFov: Float = 0f
    private var headRotation: Quaternion = Quaternion.DEFAULT

    enum class CameraID {
        WORLD,
        SUN_SHADOW0, SUN_SHADOW1, SUN_SHADOW2, SUN_SHADOW3,
        SPOT_SHADOW0, SPOT_SHADOW1,
        WATER0, WATER1;
    }

    var currentCameraId: CameraID = CameraID.WORLD

    fun getPixelAngle(): Float = pixelMeterRatio / viewHeightInPixels.toFloat()

    fun setDefaultFOV(fov: Float) {
        prevDefaultFov = defaultFov
        defaultFov = fov.coerceIn(CameraConstants.MIN_FIELD_OF_VIEW, CameraConstants.MAX_FIELD_OF_VIEW)
    }

    fun getDefaultFOV(): Float = defaultFov

    fun isDefaultFOVChanged(): Boolean = defaultFov != prevDefaultFov

    fun setViewHeightInPixels(height: Int) {
        viewHeightInPixels = height
    }

    fun setPerspective(fovY: Float, aspect: Float, nearClip: Float, farClip: Float) {
        this.fovH = fovY
        this.aspect = aspect
        this.nearClip = nearClip
        this.farClip = farClip
        cosHalfFov = kotlin.math.cos(fovY * 0.5f)
    }

    fun calcProjection(farClip: Float): Matrix4 = calcPerspMatrix(nearClip, farClip)

    fun calcPerspMatrix(nearClip: Float, farClip: Float): Matrix4 {
        val f = 1f / tan(fovH * 0.5f)
        val rangeInv = 1f / (nearClip - farClip)
        return Matrix4(
            floatArrayOf(
                f / aspect, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, (nearClip + farClip) * rangeInv, -1f,
                0f, 0f, 2f * nearClip * farClip * rangeInv, 0f
            )
        )
    }

    fun getHeadRotation(): Quaternion = headRotation

    object CameraConstants {
        const val MIN_FIELD_OF_VIEW = 0.1f
        const val MAX_FIELD_OF_VIEW = 3.0f
        const val DEFAULT_FIELD_OF_VIEW = 1.047f
    }
}
