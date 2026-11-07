package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import kotlin.math.log2
import kotlin.math.sqrt

const val MAX_REFLECTION_PROBE_COUNT: Int = 256
const val IRRADIANCE_MAP_RESOLUTION: Int = 16
const val REFLECTION_PROBE_MINIMUM_SCALE: Float = 1f

class ReflectionMapManager {

    enum class DetailLevel(val value: Int) {
        STATIC_ONLY(0),
        STATIC_AND_DYNAMIC(1),
        REALTIME(2)
    }

    data class ReflectionProbeData(
        val refBox: Array<FloatArray> = Array(MAX_REFLECTION_PROBE_COUNT) { FloatArray(16) },
        val heroBox: FloatArray = FloatArray(16),
        val refSphere: Array<FloatArray> = Array(MAX_REFLECTION_PROBE_COUNT) { FloatArray(4) },
        val refParams: Array<FloatArray> = Array(MAX_REFLECTION_PROBE_COUNT) { FloatArray(4) },
        val heroSphere: FloatArray = FloatArray(4),
        val refIndex: Array<IntArray> = Array(MAX_REFLECTION_PROBE_COUNT) { IntArray(4) },
        val refNeighbor: IntArray = IntArray(4096),
        val refBucket: Array<IntArray> = Array(256) { IntArray(4) },
        var refmapCount: Int = 0,
        var heroShape: Int = 0,
        var heroMipCount: Int = 0,
        var heroProbeCount: Int = 0
    )

    private val probes: MutableList<ReflectionMap> = mutableListOf()
    private val killList: MutableList<ReflectionMap> = mutableListOf()
    private val createList: MutableList<ReflectionMap> = mutableListOf()
    private val reflectionMaps: MutableList<ReflectionMap?> = mutableListOf()

    private val cubeFree: ArrayDeque<Int> = ArrayDeque()

    private var updatingProbe: ReflectionMap? = null
    private var updatingFace: UInt = 0u

    private var radiancePass: Boolean = false
    private var realtimeRadiancePass: Boolean = false

    private var defaultProbe: ReflectionMap? = null

    private var reflectionProbeCount: UInt = 0u
    private var dynamicProbeCount: UInt = MAX_REFLECTION_PROBE_COUNT.toUInt()

    private var renderReflectionProbeDetail: Int = -1
    private var renderReflectionProbeLevel: Int = 3
    private var renderReflectionProbeCount: UInt = 256u
    private var renderReflectionProbeDynamicAllocation: Int = -1

    private var probeResolution: UInt = 128u
    private var maxProbeLod: Float = 6f
    private var lightScale: Float = 1f

    private var reset: Boolean = false
    private var resetFade: Float = 1f
    private var globalFadeTarget: Float = 1f

    private var paused: Boolean = false
    private var resumeTime: Float = 0f

    private val probeData: ReflectionProbeData = ReflectionProbeData()

    private var ubo: UInt = 0u

    init {
        dynamicProbeCount = MAX_REFLECTION_PROBE_COUNT.toUInt()
        initCubeFree()
    }

    private fun initCubeFree() {
        cubeFree.clear()
        for (i in 1 until dynamicProbeCount.toInt()) {
            cubeFree.addLast(i)
        }
    }

    private fun clearCubeMaps() {
        // no-op
    }

    fun cleanup() {
        System.err.println("ReflectionMapManager: cleanup not yet implemented")
    }

    fun update() {
        // no-op
    }

    fun refreshSettings() {
        System.err.println("ReflectionMapManager: refreshSettings not yet implemented")
    }

    fun addProbe(group: Any? = null): ReflectionMap? {
        return null
    }

    fun getReflectionMaps(maps: MutableList<ReflectionMap?>) {
        System.err.println("ReflectionMapManager: getReflectionMaps not yet implemented")
    }

    fun registerSpatialGroup(group: Any?): ReflectionMap? {
        if (group == null) return null
        return null
    }

    fun registerViewerObject(vobj: Any?): ReflectionMap? {
        requireNotNull(vobj) { "vobj must not be null" }
        return null
    }

    fun reset() {
        reset = true
    }

    fun pause(duration: Float = 10f) {
        paused = true
        resumeTime = 0f
    }

    fun resume() {
        paused = false
    }

    fun shift(offset: FloatArray) {
        for (probe in probes) {
            probe.origin[0] += offset[0]
            probe.origin[1] += offset[1]
            probe.origin[2] += offset[2]
        }
    }

    fun renderDebug() {
        // no-op
    }

    fun initReflectionMaps() {
        // no-op
    }

    fun isRadiancePass(): Boolean = radiancePass

    fun doOcclusion() {
        val eye = FloatArray(4)
        for (probe in probes) {
            if (probe !== defaultProbe) {
                probe.doOcclusion(eye)
            }
        }
    }

    fun forceDefaultProbeAndUpdateUniforms(force: Boolean = true) {
        // no-op
    }

    fun probeCount(): UInt = dynamicProbeCount

    fun probeMemory(): UInt {
        val radiance = dynamicProbeCount.toInt() * 6 * (probeResolution.toInt() * probeResolution.toInt()) * 4
        val irradiance = dynamicProbeCount.toInt() * 6 * (IRRADIANCE_MAP_RESOLUTION * IRRADIANCE_MAP_RESOLUTION) * 4
        return ((radiance + irradiance) / 1024 / 1024).toUInt()
    }

    private fun initCubeFreeDefaults() = initCubeFree()

    private fun deleteProbe(i: Int) {
        val probe = probes[i]
        check(probe !== defaultProbe)
        if (probe.cubeIndex != -1) {
            cubeFree.addLast(probe.cubeIndex)
        }
        if (updatingProbe === probe) {
            updatingProbe = null
            updatingFace = 0u
        }
        for (other in probe.neighbors) {
            other.neighbors.remove(probe)
        }
        probes.removeAt(i)
    }

    private fun allocateCubeIndex(): Int {
        if (cubeFree.isEmpty()) return -1
        return cubeFree.removeFirst()
    }

    private fun updateNeighbors(probe: ReflectionMap) {
        if (probe === defaultProbe) return
        for (other in probe.neighbors) {
            other.neighbors.remove(probe)
        }
        probe.neighbors.clear()
        if (probe.isRelevant()) {
            for (other in probes) {
                if (other !== defaultProbe && other !== probe && other.isRelevant() && probe.intersects(other)) {
                    probe.neighbors.add(other)
                    other.neighbors.add(probe)
                }
            }
        }
    }

    private fun updateUniforms() {
        // no-op
    }

    private fun setUniforms() {
        // no-op
    }

    private fun doProbeUpdate() {
        // no-op
    }

    private fun updateProbeFace(probe: ReflectionMap, face: UInt) {
        // no-op
    }
}

fun renderReflectionProbe(
    probe: ReflectionMap,
    groupCount: MutableMap<Any?, Int>,
    objCount: MutableMap<Any?, Int>,
    locCount: MutableMap<FloatArray, Int>
) {
    // no-op
}
