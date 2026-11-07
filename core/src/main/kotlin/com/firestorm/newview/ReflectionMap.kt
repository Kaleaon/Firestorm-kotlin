package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import kotlin.math.sqrt

class ReflectionMap {

    enum class ProbeType {
        ALL, RADIANCE, IRRADIANCE, REFLECTION
    }

    enum class ProbeLevel(val value: Int) {
        NONE(0),
        MANUAL_ONLY(1),
        MANUAL_AND_TERRAIN(2),
        FULL_SCENE_WITH_AUTO(3)
    }

    var origin: FloatArray = FloatArray(4)

    var distance: Float = -1f
    var minDepth: Float = -1f
    var maxDepth: Float = -1f
    var radius: Float = 16f
    var lastUpdateTime: Float = 0f
    var lastBindTime: Float = 0f

    var cubeIndex: Int = -1

    var complete: Boolean = false
    var fadeIn: Float = 0f
    var probeIndex: Int = -1

    val neighbors: MutableList<ReflectionMap> = mutableListOf()

    var group: Any? = null
    var viewerObject: Any? = null

    var priority: UInt = 0u

    var occlusionQuery: UInt = 0u
    var occluded: Boolean = false
    var occlusionPendingFrames: UInt = 0u

    var type: ProbeType = ProbeType.ALL

    fun update(
        resolution: UInt,
        face: UInt,
        forceDynamic: Boolean = false,
        nearClip: Float = -1f,
        useClipPlane: Boolean = false,
        clipPlaneNormal: FloatArray = floatArrayOf(0f, 0f, 1f),
        clipPlaneD: Float = 0f
    ) {
        // no-op
    }

    fun autoAdjustOrigin() {
        // no-op
    }

    fun intersects(other: ReflectionMap): Boolean {
        val dx = other.origin[0] - origin[0]
        val dy = other.origin[1] - origin[1]
        val dz = other.origin[2] - origin[2]
        val dist = dx * dx + dy * dy + dz * dz
        val r2 = (radius + other.radius).let { it * it }
        return dist < r2
    }

    fun getAmbiance(): Float = 0f

    fun getNearClip(): Float {
        val minimumNearClip = 0.1f
        val ret: Float = when {
            viewerObject != null -> 0f
            group != null -> radius * 0.5f
            else -> 1f
        }
        return maxOf(ret, minimumNearClip)
    }

    fun getIsDynamic(): Boolean = false

    fun getBox(box: FloatArray): Boolean = false

    fun isActive(): Boolean = cubeIndex != -1

    fun isRelevant(): Boolean {
        val probeLevel: Int = 0
        val isManual = viewerObject != null
        val isAutomatic = group != null && !isManual
        return when (probeLevel) {
            ProbeLevel.NONE.value -> false
            ProbeLevel.MANUAL_ONLY.value -> isManual
            ProbeLevel.MANUAL_AND_TERRAIN.value -> !isAutomatic
            ProbeLevel.FULL_SCENE_WITH_AUTO.value -> true
            else -> false
        }
    }

    fun doOcclusion(eye: FloatArray) {
        // no-op
    }
}
