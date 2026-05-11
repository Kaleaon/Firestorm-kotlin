package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.*

var gFrameCount: UInt = 0u
var gAgentID: UUID = UUID(0L, 0L)

object LLPerfStats {

    const val ART_UNLIMITED_NANOS: ULong = 50_000_000uL
    const val ART_MINIMUM_NANOS: ULong = 100_000uL
    const val ART_MIN_ADJUST_UP_NANOS: ULong = 5_000uL
    const val ART_MIN_ADJUST_DOWN_NANOS: ULong = 10_000uL

    const val PREFERRED_DD: Float = 180f
    const val SMOOTHING_PERIODS: UInt = 50u
    const val DD_STEP: UInt = 10u

    const val TUNE_AVATARS_ONLY: UInt = 0u
    const val TUNE_SCENE_AND_AVATARS: UInt = 1u
    const val TUNE_SCENE_ONLY: UInt = 2u

    @Volatile var cpuHertz: Double = 0.0
    val tunedAvatars: AtomicLong = AtomicLong(0L)
    val renderAvatarMaxART_ns: AtomicLong = AtomicLong(ART_UNLIMITED_NANOS.toLong())
    @Volatile var belowTargetFPS: Boolean = false
    @Volatile var lastGlobalPrefChange: UInt = 0u
    @Volatile var lastSleepedFrame: UInt = 0u
    @Volatile var meanFrameTime: ULong = 0uL
    val bufferToggleLock: ReentrantLock = ReentrantLock()

    private val sTotalAvatarTime: AtomicLong = AtomicLong(0L)
    private val sAverageAvatarTime: AtomicLong = AtomicLong(0L)
    private val sMaxAvatarTime: AtomicLong = AtomicLong(0L)

    enum class ObjType {
        OT_GENERAL,
        OT_AVATAR
    }

    enum class StatType {
        RENDER_GEOMETRY,
        RENDER_SHADOWS,
        RENDER_HUDS,
        RENDER_UI,
        RENDER_COMBINED,
        RENDER_SWAP,
        RENDER_FRAME,
        RENDER_DISPLAY,
        RENDER_SLEEP,
        RENDER_LFS,
        RENDER_MESHREPO,
        RENDER_FPSLIMIT,
        RENDER_FPS,
        RENDER_IDLE,
        RENDER_DONE,
        STATS_COUNT
    }

    private val STATS_COUNT = StatType.STATS_COUNT.ordinal

    data class StatsRecord(
        val statType: StatType,
        val objType: ObjType,
        val avID: UUID,
        val objID: UUID,
        val time: ULong,
        val isRigged: Boolean = false,
        val isHUD: Boolean = false
    )

    data class Tunables(
        var tuningFlag: UInt = 0u,
        var nonImpostors: UInt = 0u,
        var reflectionDetail: Int = 0,
        var farClip: Float = 0f,
        var userMinDrawDistance: Float = 0f,
        var userTargetDrawDistance: Float = 0f,
        var userImpostorDistance: Float = 0f,
        var userImpostorDistanceTuningEnabled: Boolean = false,
        var userFPSTuningStrategy: UInt = 0u,
        var userAutoTuneEnabled: Boolean = false,
        var userAutoTuneLock: Boolean = true,
        var userTargetFPS: UInt = 0u,
        var userARTCutoffSliderValue: Float = 0f,
        var autoTuneTimeout: Boolean = true,
        var vsyncEnabled: Boolean = true
    ) {
        companion object {
            const val Nothing: UInt = 0u
            const val NonImpostors: UInt = 1u
            const val ReflectionDetail: UInt = 2u
            const val FarClip: UInt = 4u
            const val UserMinDrawDistance: UInt = 8u
            const val UserTargetDrawDistance: UInt = 16u
            const val UserImpostorDistance: UInt = 32u
            const val UserImpostorDistanceTuningEnabled: UInt = 64u
            const val UserFPSTuningStrategy: UInt = 128u
            const val UserAutoTuneEnabled: UInt = 256u
            const val UserTargetFPS: UInt = 512u
            const val UserARTCutoff: UInt = 1024u
            const val UserAutoTuneLock: UInt = 4096u
        }

        fun updateNonImposters(nv: UInt) { nonImpostors = nv; tuningFlag = tuningFlag or NonImpostors }
        fun updateReflectionDetail(nv: Int) { reflectionDetail = nv; tuningFlag = tuningFlag or ReflectionDetail }
        fun updateFarClip(nv: Float) { farClip = nv; tuningFlag = tuningFlag or FarClip }
        fun updateUserMinDrawDistance(nv: Float) { userMinDrawDistance = nv; tuningFlag = tuningFlag or UserMinDrawDistance }
        fun updateUserTargetDrawDistance(nv: Float) { userTargetDrawDistance = nv; tuningFlag = tuningFlag or UserTargetDrawDistance }
        fun updateImposterDistance(nv: Float) { userImpostorDistance = nv; tuningFlag = tuningFlag or UserImpostorDistance }
        fun updateImposterDistanceTuningEnabled(nv: Boolean) { userImpostorDistanceTuningEnabled = nv; tuningFlag = tuningFlag or UserImpostorDistanceTuningEnabled }
        fun updateUserFPSTuningStrategy(nv: UInt) { userFPSTuningStrategy = nv; tuningFlag = tuningFlag or UserFPSTuningStrategy }
        fun updateTargetFps(nv: UInt) { userTargetFPS = nv; tuningFlag = tuningFlag or UserTargetFPS }
        fun updateUserARTCutoffSlider(nv: Float) { userARTCutoffSliderValue = nv; tuningFlag = tuningFlag or UserARTCutoff }
        fun updateUserAutoTuneEnabled(nv: Boolean) { userAutoTuneEnabled = nv; tuningFlag = tuningFlag or UserAutoTuneEnabled }
        fun updateUserAutoTuneLock(nv: Boolean) { userAutoTuneLock = nv; tuningFlag = tuningFlag or UserAutoTuneLock }
        fun resetChanges() { tuningFlag = Nothing }

        fun initialiseFromSettings() {
            TODO("APR: read settings from gSavedSettings for autotune prefs")
        }

        fun updateRenderCostLimitFromSettings() {
            TODO("APR: read RenderAvatarMaxART from gSavedSettings and update renderAvatarMaxART_ns")
        }

        fun updateSettingsFromRenderCostLimit() {
            val current = renderAvatarMaxART_ns.get().toDouble()
            val expected = if (current != 0.0) log10(current / 1000.0).toFloat() else log10(ART_UNLIMITED_NANOS.toDouble() / 1000.0).toFloat()
            if (userARTCutoffSliderValue != expected) updateUserARTCutoffSlider(expected)
        }

        fun applyUpdates() {
            TODO("APR: push tuning flag changes back to gSavedSettings")
        }
    }

    var tunables: Tunables = Tunables()

    object StatsRecorder {
        private val writeBuffer = AtomicInteger(0)
        private var collectionEnabled: Boolean = true
        private var focusAv: UUID = UUID(0L, 0L)
        private var autotuneInit: Boolean = false

        private val STATS_COUNT_VAL = StatType.STATS_COUNT.ordinal
        private val OBJ_COUNT = ObjType.values().size

        private fun emptyStatsArray(): LongArray = LongArray(STATS_COUNT_VAL)
        private fun emptyStatsMap(): MutableMap<UUID, LongArray> = mutableMapOf()
        private fun emptyTypeMatrix(): Array<MutableMap<UUID, LongArray>> = Array(OBJ_COUNT) { emptyStatsMap() }
        private fun emptySummaryArray(): Array<LongArray> = Array(OBJ_COUNT) { LongArray(STATS_COUNT_VAL) }

        private val statsDoubleBuffer: Array<Array<MutableMap<UUID, LongArray>>> = arrayOf(emptyTypeMatrix(), emptyTypeMatrix())
        private val max: Array<Array<LongArray>> = arrayOf(emptySummaryArray(), emptySummaryArray())
        private val sum: Array<Array<LongArray>> = arrayOf(emptySummaryArray(), emptySummaryArray())

        fun getInstance(): StatsRecorder = this

        fun setFocusAv(avID: UUID) { focusAv = avID }
        fun getFocusAv(): UUID = focusAv
        fun setAutotuneInit() { autotuneInit = true }

        fun send(upd: StatsRecord) { processUpdate(upd) }

        fun endFrame() {
            processUpdate(StatsRecord(StatType.RENDER_DONE, ObjType.OT_GENERAL, UUID(0L,0L), UUID(0L,0L), 0uL))
        }

        fun clearStats() {
            processUpdate(StatsRecord(StatType.RENDER_DONE, ObjType.OT_GENERAL, UUID(0L,0L), UUID(0L,0L), 1uL))
        }

        fun setEnabled(onOrOff: Boolean) { collectionEnabled = onOrOff }
        fun enable() { collectionEnabled = true }
        fun disable() { collectionEnabled = false }
        fun enabled(): Boolean = collectionEnabled

        fun getReadBufferIndex(): Int = writeBuffer.get() xor 1

        fun get(otype: ObjType, id: UUID, type: StatType): ULong =
            (statsDoubleBuffer[getReadBufferIndex()][otype.ordinal][id]?.get(type.ordinal) ?: 0L).toULong()

        fun getSceneStat(type: StatType): ULong =
            get(ObjType.OT_GENERAL, UUID(0L, 0L), type)

        fun getSum(otype: ObjType, type: StatType): ULong =
            sum[getReadBufferIndex()][otype.ordinal][type.ordinal].toULong()

        fun getMax(otype: ObjType, type: StatType): ULong =
            max[getReadBufferIndex()][otype.ordinal][type.ordinal].toULong()

        private fun processUpdate(upd: StatsRecord) {
            if (upd.statType == StatType.RENDER_DONE && upd.objType == ObjType.OT_GENERAL && upd.time == 0uL) {
                toggleBuffer(); return
            }
            if (upd.statType == StatType.RENDER_DONE && upd.objType == ObjType.OT_GENERAL && upd.time == 1uL) {
                clearStatsBuffers(); return
            }
            when (upd.objType) {
                ObjType.OT_GENERAL -> doUpd(upd.objID, upd.objType, upd.statType, upd.time)
                ObjType.OT_AVATAR  -> doUpd(upd.avID, upd.objType, upd.statType, upd.time)
            }
        }

        private fun doUpd(key: UUID, ot: ObjType, type: StatType, value: ULong) {
            val wb = writeBuffer.get()
            val stm = statsDoubleBuffer[wb][ot.ordinal]
            val thisAsset = stm.getOrPut(key) { LongArray(STATS_COUNT_VAL) }
            val v = value.toLong()
            thisAsset[type.ordinal] += v
            thisAsset[StatType.RENDER_COMBINED.ordinal] += v
            sum[wb][ot.ordinal][type.ordinal] += v
            sum[wb][ot.ordinal][StatType.RENDER_COMBINED.ordinal] += v
            if (max[wb][ot.ordinal][type.ordinal] < thisAsset[type.ordinal])
                max[wb][ot.ordinal][type.ordinal] = thisAsset[type.ordinal]
            if (max[wb][ot.ordinal][StatType.RENDER_COMBINED.ordinal] < thisAsset[StatType.RENDER_COMBINED.ordinal])
                max[wb][ot.ordinal][StatType.RENDER_COMBINED.ordinal] = thisAsset[StatType.RENDER_COMBINED.ordinal]
        }

        private fun toggleBuffer() {
            val wb = writeBuffer.get()
            val sceneStats = statsDoubleBuffer[wb][ObjType.OT_GENERAL.ordinal][UUID(0L,0L)] ?: LongArray(STATS_COUNT_VAL)
            val lastStats = statsDoubleBuffer[wb xor 1][ObjType.OT_GENERAL.ordinal].getOrPut(UUID(0L,0L)) { LongArray(STATS_COUNT_VAL) }

            val unreliable = sceneStats[StatType.RENDER_FPSLIMIT.ordinal] != 0L ||
                             sceneStats[StatType.RENDER_SLEEP.ordinal] != 0L
            if (unreliable) {
                lastStats[StatType.RENDER_FPSLIMIT.ordinal] = sceneStats[StatType.RENDER_FPSLIMIT.ordinal]
                lastStats[StatType.RENDER_SLEEP.ordinal]    = sceneStats[StatType.RENDER_SLEEP.ordinal]
                lastStats[StatType.RENDER_FRAME.ordinal]    = sceneStats[StatType.RENDER_FRAME.ordinal]
            }

            if (!unreliable) {
                val toAvg = listOf(StatType.RENDER_FRAME, StatType.RENDER_DISPLAY, StatType.RENDER_HUDS,
                    StatType.RENDER_UI, StatType.RENDER_SWAP, StatType.RENDER_IDLE)
                for (statEntry in toAvg) {
                    val avg = lastStats[statEntry.ordinal]
                    val value = sceneStats[statEntry.ordinal]
                    val sp = SMOOTHING_PERIODS.toLong()
                    sceneStats[statEntry.ordinal] = avg + (value / sp) - (avg / sp)
                }
            }

            if (enabled()) {
                bufferToggleLock.lock()
                try { writeBuffer.set(wb xor 1) }
                finally { bufferToggleLock.unlock() }
            }

            val newWb = writeBuffer.get()
            for (statsMap in statsDoubleBuffer[newWb]) {
                for (entry in statsMap.values) entry.fill(0)
                statsMap.clear()
            }
            for (i in 0 until OBJ_COUNT) {
                max[newWb][i].fill(0)
                sum[newWb][i].fill(0)
            }

            if (autotuneInit && tunables.userAutoTuneEnabled) updateAvatarParams()
        }

        private fun clearStatsBuffers() {
            val wb = writeBuffer.get()
            fun clearBuf(buf: Int) {
                for (statsMap in statsDoubleBuffer[buf]) {
                    for (entry in statsMap.values) entry.fill(0)
                    statsMap.clear()
                }
                for (i in 0 until OBJ_COUNT) {
                    max[buf][i].fill(0)
                    sum[buf][i].fill(0)
                }
            }
            clearBuf(wb)
            if (enabled()) {
                bufferToggleLock.lock()
                try { writeBuffer.set(wb xor 1) }
                finally { bufferToggleLock.unlock() }
            }
            clearBuf(writeBuffer.get())
        }

        private fun countNearbyAvatars(distance: Int): Int {
            TODO("APR: use JVM equivalent for LLWorld::getAvatars within distance")
        }

        private val frameTimeDeque: ArrayDeque<ULong> = ArrayDeque()
        private const val NUM_PERIODS = 50

        private fun updateMeanFrameTime(curFrameTimeRaw: ULong) {
            frameTimeDeque.addFirst(curFrameTimeRaw)
            if (frameTimeDeque.size > NUM_PERIODS) frameTimeDeque.removeLast()
            val buf = frameTimeDeque.sortedBy { it }
            meanFrameTime = if (buf.size % 2 == 0) (buf[buf.size/2 - 1] + buf[buf.size/2]) / 2u
                            else buf[buf.size/2]
        }

        private fun getMeanTotalFrameTime(): ULong = meanFrameTime

        fun updateAvatarParams() {
            if (tunables.autoTuneTimeout) {
                lastSleepedFrame = gFrameCount
                tunables.autoTuneTimeout = false
                return
            }
            val totSleepTimeRaw = getSceneStat(StatType.RENDER_SLEEP)
            val totLimitTimeRaw = getSceneStat(StatType.RENDER_FPSLIMIT)
            var totFrameTimeRaw = getSceneStat(StatType.RENDER_FRAME)

            if (totSleepTimeRaw != 0uL) {
                lastSleepedFrame = gFrameCount
                return
            }

            val vsyncMaxFps: UInt = TODO("APR: query display refresh rate from window system")
            val targetFps = if (tunables.vsyncEnabled) minOf(vsyncMaxFps, tunables.userTargetFPS)
                            else tunables.userTargetFPS

            if (lastSleepedFrame != 0u) {
                if ((gFrameCount - lastSleepedFrame) > targetFps * 5u) {
                    lastSleepedFrame = 0u
                } else {
                    return
                }
            }

            updateMeanFrameTime(totFrameTimeRaw)

            if (tunables.userImpostorDistanceTuningEnabled) {
                val renderFarClip: Float = TODO("GPU: read LLPipeline::RenderFarClip")
                val count = countNearbyAvatars(minOf(renderFarClip, tunables.userImpostorDistance).toInt())
                if (count.toUInt() != tunables.nonImpostors) {
                    val maxSlider: UInt = TODO("GPU: LLVOAvatar::NON_IMPOSTORS_MAX_SLIDER")
                    tunables.updateNonImposters(if (count.toUInt() < maxSlider) count.toUInt() else 0u)
                }
            }

            val avRenderMaxRaw = msToRaw(sMaxAvatarTime.get().toDouble() / 1_000_000.0)
            val targetFrameTimeRaw = (cpuHertz / (if (targetFps == 0u) 1.0 else targetFps.toDouble())).toLong().toULong()
            val inferredFPS = (1000.0 / maxOf(rawToMs(totFrameTimeRaw), 1.0)).toUInt()
            val settingsChangeFrequency = if (inferredFPS > 50u) inferredFPS else 50u
            val timeBuf = (targetFrameTimeRaw.toLong() * 0.1).toLong().toULong()

            if (totLimitTimeRaw != 0uL) {
                totFrameTimeRaw = if (totFrameTimeRaw > totLimitTimeRaw) totFrameTimeRaw - totLimitTimeRaw else 0uL
            }

            if ((targetFrameTimeRaw + timeBuf) <= totFrameTimeRaw) {
                if (targetFrameTimeRaw >= timeBuf && (targetFrameTimeRaw - timeBuf) >= getMeanTotalFrameTime()) {
                    belowTargetFPS = false
                    lastGlobalPrefChange = gFrameCount
                    return
                }
                if (!belowTargetFPS) {
                    belowTargetFPS = true
                    lastGlobalPrefChange = gFrameCount
                }
                val totAvatarTimeRaw = msToRaw(sMaxAvatarTime.get().toDouble() / 1_000_000.0)
                val nonAvatarTimeRaw = if (totFrameTimeRaw > totAvatarTimeRaw) totFrameTimeRaw - totAvatarTimeRaw else 0uL

                val targetAvatarTimeRaw: ULong
                if (targetFrameTimeRaw < nonAvatarTimeRaw) {
                    if ((gFrameCount - lastGlobalPrefChange) > settingsChangeFrequency) {
                        if (tunables.userFPSTuningStrategy != TUNE_AVATARS_ONLY) {
                            val renderFarClip: Float = TODO("GPU: read LLPipeline::RenderFarClip")
                            val newDd = if (renderFarClip - DD_STEP.toFloat() > tunables.userMinDrawDistance)
                                renderFarClip - DD_STEP.toFloat() else tunables.userMinDrawDistance
                            if (newDd != renderFarClip) {
                                tunables.updateFarClip(newDd)
                                lastGlobalPrefChange = gFrameCount
                                return
                            }
                        }
                        targetAvatarTimeRaw = 0uL
                    } else {
                        return
                    }
                } else {
                    targetAvatarTimeRaw = targetFrameTimeRaw - nonAvatarTimeRaw
                }

                if (targetAvatarTimeRaw < totAvatarTimeRaw && tunables.userFPSTuningStrategy != TUNE_SCENE_ONLY) {
                    var newRenderLimitNs = rawToNs(avRenderMaxRaw)
                    if (newRenderLimitNs > renderAvatarMaxART_ns.get().toDouble()) {
                        newRenderLimitNs = renderAvatarMaxART_ns.get().toDouble()
                    }
                    if (newRenderLimitNs > ART_MIN_ADJUST_DOWN_NANOS.toDouble()) {
                        newRenderLimitNs -= ART_MIN_ADJUST_DOWN_NANOS.toDouble()
                    }
                    val bottom = ART_MINIMUM_NANOS.toDouble()
                    renderAvatarMaxART_ns.set(maxOf(newRenderLimitNs, bottom).toLong())
                }
            } else {
                belowTargetFPS = false
                val totAvatarTimeRaw = msToRaw(sMaxAvatarTime.get().toDouble() / 1_000_000.0)
                if (totFrameTimeRaw < targetFrameTimeRaw && tunables.userFPSTuningStrategy != TUNE_SCENE_ONLY) {
                    var newRenderLimitNs = renderAvatarMaxART_ns.get().toDouble() + ART_MIN_ADJUST_UP_NANOS.toDouble()
                    if (newRenderLimitNs > ART_UNLIMITED_NANOS.toDouble()) newRenderLimitNs = ART_UNLIMITED_NANOS.toDouble()
                    renderAvatarMaxART_ns.set(newRenderLimitNs.toLong())
                }
            }
        }

        fun updateAvatarParamsPublic() = updateAvatarParams()
    }

    fun updateClass() {
        sTotalAvatarTime.set(getTotalAvatarRenderTimeRaw())
        sAverageAvatarTime.set(getAverageAvatarRenderTimeRaw())
        sMaxAvatarTime.set(getMaxAvatarRenderTimeRaw())
    }

    fun rawToNs(raw: ULong): Double = raw.toDouble() * 1_000_000_000.0 / cpuHertz
    fun rawToUs(raw: ULong): Double = raw.toDouble() *     1_000_000.0 / cpuHertz
    fun rawToMs(raw: ULong): Double = raw.toDouble() *         1_000.0 / cpuHertz
    fun nsToRaw(ns: Double): ULong = (cpuHertz * (ns / 1_000_000_000.0)).toLong().toULong()
    fun usToRaw(us: Double): ULong = (cpuHertz * (us / 1_000_000.0)).toLong().toULong()
    fun msToRaw(ms: Double): ULong = (cpuHertz * (ms / 1_000.0)).toLong().toULong()
    fun rawToNs(raw: Long): Double = rawToNs(raw.toULong())
    fun rawToMs(raw: ULong): Double = raw.toDouble() * 1_000.0 / cpuHertz
    fun msToRaw(raw: ULong): ULong = msToRaw(raw.toDouble()).toLong().toULong()

    typealias RecordSceneTime = RecordTime<Nothing>
}

class RecordTime<T>(
    av: UUID,
    id: UUID,
    type: LLPerfStats.StatType,
    isRiggedAtt: Boolean = false,
    isHUDAtt: Boolean = false
) : AutoCloseable {
    private val start: Long = System.nanoTime()
    val stat: LLPerfStats.StatsRecord = LLPerfStats.StatsRecord(type, LLPerfStats.ObjType.OT_GENERAL, av, id, 0uL, isRiggedAtt, isHUDAtt)

    override fun close() {
        if (!LLPerfStats.StatsRecorder.enabled()) return
        val elapsed = (System.nanoTime() - start).toULong()
        LLPerfStats.StatsRecorder.send(stat.copy(time = elapsed))
    }
}

private fun getTotalAvatarRenderTimeRaw(): Long = TODO("GPU: LLVOAvatar::getTotalGPURenderTime raw")
private fun getAverageAvatarRenderTimeRaw(): Long = TODO("GPU: LLVOAvatar::getAverageGPURenderTime raw")
private fun getMaxAvatarRenderTimeRaw(): Long = TODO("GPU: LLVOAvatar::getMaxGPURenderTime raw")
