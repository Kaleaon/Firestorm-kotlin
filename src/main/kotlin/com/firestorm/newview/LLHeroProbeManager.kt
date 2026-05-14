package com.firestorm.newview

import java.util.UUID
import kotlin.math.log2

const val LL_MAX_HERO_PROBE_COUNT = 2

data class HeroProbeData(
    var heroBox: FloatArray = FloatArray(16),
    var heroSphere: FloatArray = FloatArray(4),
    var heroShape: Int = 0,
    var heroMipCount: Int = 0,
    var heroProbeCount: Int = 0
)

class LLHeroProbeManager {

    enum class DetailLevel {
        STATIC_ONLY,
        STATIC_AND_DYNAMIC,
        REALTIME
    }

    var mirrorPosition: FloatArray = FloatArray(3)
    var mirrorNormal: FloatArray = FloatArray(3)
    var heroData: HeroProbeData = HeroProbeData()

    private val probes: MutableList<LLReflectionMap> = mutableListOf()
    private var defaultProbe: LLReflectionMap? = null

    private var reflectionProbeCount: UInt = 0u
    private var probeResolution: UInt = 1024u
    private var maxProbeLOD: Float = 6f
    private var heroProbeStrength: Float = 1f
    private var isInTransition: Boolean = false
    private var reset: Boolean = false
    private var renderingMirror: Boolean = false

    private val heroVOList: MutableList<LLVOVolume> = mutableListOf()
    private var nearestHero: LLVOVolume? = null

    private val mipChain: MutableList<LLRenderTarget> = mutableListOf()

    private var initialized: Boolean = false

    fun isMirrorPass(): Boolean = renderingMirror

    fun cleanup() {
        // no-op
    }

    fun update() {
        // no-op
    }

    fun renderProbes() {
        // no-op
    }

    fun renderDebug() {
        // no-op
    }

    fun initReflectionMaps() {
        // no-op
    }

    fun doOcclusion() {
        // no-op
    }

    fun reset() {
        reset = true
    }

    fun registerViewerObject(drawablep: LLVOVolume): Boolean {
        if (!heroVOList.contains(drawablep)) {
            heroVOList.add(drawablep)
            return true
        }
        return false
    }

    fun unregisterViewerObject(drawablep: LLVOVolume) {
        heroVOList.remove(drawablep)
        if (drawablep === nearestHero) {
            nearestHero = null
            defaultProbe?.viewerObject = null
        }
    }

    private fun updateProbeFace(probe: LLReflectionMap, face: UInt, isDynamic: Boolean, nearClip: Float) {
        // no-op
    }

    private fun generateRadiance(probe: LLReflectionMap) {
        // no-op
    }

    private fun updateUniforms() {
        // no-op
    }
}

class LLReflectionMap {
    var origin: FloatArray = FloatArray(4)
    var radius: Float = 0f
    var cubeIndex: Int = 0
    var cubeArray: Any? = null
    var distance: Float = 0f
    var probeIndex: Int = 0
    var viewerObject: LLVOVolume? = null
    var mOccluded: Boolean = false

    fun doOcclusion(eye: FloatArray) {
        // no-op
    }

    fun autoAdjustOrigin() {
        // no-op
    }

    fun getBox(outBox: FloatArray): Boolean {
        return false
    }

    fun getNearClip(): Float = 0.01f

    fun isRelevant(): Boolean = !mOccluded
}

class LLVOVolume {
    var id: UUID = UUID.randomUUID()
    var viewerObject: Any? = null

    fun isDead(): Boolean = false
    fun isReflectionProbe(): Boolean = false
    fun getReflectionProbeIsBox(): Boolean = false
    fun getReflectionProbeIsDynamic(): Boolean = false
    fun getPositionAgent(): FloatArray = FloatArray(3)
    fun getScale(): FloatArray = FloatArray(3)
}

class LLRenderTarget {
    fun getWidth(): Int = 0
}
