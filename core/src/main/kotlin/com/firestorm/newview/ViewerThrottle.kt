/**
 * ViewerThrottle.kt
 * Kotlin port of llviewerthrottle.h / llviewerthrottle.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// ThrottleGroup enum
// ---------------------------------------------------------------------------

/**
 * Enumerates the bandwidth channels managed by the throttle system.
 *
 * Corresponds to C++ enum EThrottleChannelType (TC_RESEND … TC_ASSET / TC_EOF)
 * defined in llthrottle.h and referenced throughout llviewerthrottle.cpp.
 */
enum class ThrottleGroup {
    RESEND,
    LAND,
    WIND,
    CLOUD,
    TASK,
    TEXTURE,
    ASSET,
}

// ---------------------------------------------------------------------------
// Constants (mirrors llviewerthrottle.cpp)
// ---------------------------------------------------------------------------

private const val MAX_FRACTIONAL: Float = 1.5f
private const val MIN_FRACTIONAL: Float = 0.2f
private const val STEP_FRACTIONAL: Float = 0.1f

private const val MIN_BANDWIDTH_KBPS: Float = 50.0f
private const val MAX_BANDWIDTH_KBPS: Float = 6000.0f

private const val HIGH_BUFFER_LOAD_THRESHOLD: Float = 1.0f
private const val LOW_BUFFER_LOAD_THRESHOLD: Float = 0.8f
private const val TIGHTEN_THROTTLE_THRESHOLD_PCT: Float = 3.0f
private const val EASE_THROTTLE_THRESHOLD_PCT: Float = 0.5f
private const val DYNAMIC_UPDATE_DURATION_SEC: Float = 5.0f

// Bandwidth presets (kbps per channel) — index maps to ThrottleGroup ordinal
//                                  RESEND LAND WIND CLOUD TASK TEXTURE ASSET
private val BW_PRESET_50   = floatArrayOf(  5f,  10f,  3f,   3f,  10f,   10f,   9f)
private val BW_PRESET_300  = floatArrayOf( 30f,  40f,  9f,   9f,  86f,   86f,  40f)
private val BW_PRESET_500  = floatArrayOf( 50f,  70f, 14f,  14f, 136f,  136f,  80f)
private val BW_PRESET_1000 = floatArrayOf(100f, 100f, 20f,  20f, 310f,  310f, 140f)

// ---------------------------------------------------------------------------
// ThrottleSetting — per-group configuration
// ---------------------------------------------------------------------------

/**
 * Holds the allocated bandwidth for a single [ThrottleGroup].
 *
 * @param group      The throttle channel this setting applies to.
 * @param kbps       Bandwidth allocated to this channel in kbps.
 */
data class ThrottleSetting(
    val group: ThrottleGroup,
    val kbps: Float,
)

// ---------------------------------------------------------------------------
// ThrottleGroupSet — a snapshot of all channel allocations
// ---------------------------------------------------------------------------

/**
 * An immutable snapshot of bandwidth allocations across all throttle channels.
 *
 * Mirrors C++ LLViewerThrottleGroup, including the arithmetic operators used
 * during interpolation / extrapolation.
 *
 * @param values  Per-channel values indexed by [ThrottleGroup.ordinal].
 */
data class ThrottleGroupSet(
    val values: FloatArray = FloatArray(ThrottleGroup.entries.size) { 0f },
) {
    constructor(preset: FloatArray) : this(preset.copyOf())

    /** Total bandwidth in kbps across all channels. */
    val total: Float get() = values.sum()

    operator fun times(frac: Float): ThrottleGroupSet =
        ThrottleGroupSet(FloatArray(values.size) { values[it] * frac })

    operator fun plus(other: ThrottleGroupSet): ThrottleGroupSet =
        ThrottleGroupSet(FloatArray(values.size) { values[it] + other.values[it] })

    operator fun minus(other: ThrottleGroupSet): ThrottleGroupSet =
        ThrottleGroupSet(FloatArray(values.size) { values[it] - other.values[it] })

    /** Return the allocation for a specific channel. */
    operator fun get(group: ThrottleGroup): Float = values[group.ordinal]

    /**
     * Pack this group-set into a message and send it to the sim.
     * Corresponds to C++ LLViewerThrottleGroup::sendToSim().
     * Stubbed — real implementation builds an AgentThrottle message.
     */
    fun sendToSim() {
        System.err.println("ThrottleGroupSet.sendToSim: AgentThrottle UDP message not yet implemented")
    }

    /** Human-readable dump of channel values. */
    override fun toString(): String = buildString {
        for (group in ThrottleGroup.entries) {
            append("  ${group.name}: ${values[group.ordinal]} kbps\n")
        }
        append("  Total: $total kbps")
    }

    // FloatArray-backed data classes must override equals/hashCode manually.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThrottleGroupSet) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()
}

// ---------------------------------------------------------------------------
// ViewerThrottle singleton
// ---------------------------------------------------------------------------

/**
 * Manages dynamic bandwidth throttling between the viewer and the sim.
 *
 * Corresponds to C++ class LLViewerThrottle (global instance gViewerThrottle).
 *
 * The throttle system:
 * 1. Maintains a maximum bandwidth limit set by the user preference.
 * 2. Interpolates or extrapolates from four hard-coded presets to produce
 *    per-channel allocations ([ThrottleGroupSet]).
 * 3. Dynamically tightens / eases bandwidth based on packet-loss and buffer load.
 */
object ViewerThrottle {

    // -----------------------------------------------------------------------
    // Named channel labels (mirrors C++ LLViewerThrottle::sNames[])
    // -----------------------------------------------------------------------

    val channelNames: Map<ThrottleGroup, String> = mapOf(
        ThrottleGroup.RESEND  to "Resend",
        ThrottleGroup.LAND    to "Land",
        ThrottleGroup.WIND    to "Wind",
        ThrottleGroup.CLOUD   to "Cloud",
        ThrottleGroup.TASK    to "Task",
        ThrottleGroup.TEXTURE to "Texture",
        ThrottleGroup.ASSET   to "Asset",
    )

    // -----------------------------------------------------------------------
    // Bandwidth presets (in ascending total-bandwidth order)
    // -----------------------------------------------------------------------

    private val presets: List<ThrottleGroupSet> = listOf(
        ThrottleGroupSet(BW_PRESET_50),
        ThrottleGroupSet(BW_PRESET_300),
        ThrottleGroupSet(BW_PRESET_500),
        ThrottleGroupSet(BW_PRESET_1000),
    )

    // -----------------------------------------------------------------------
    // Mutable state
    // -----------------------------------------------------------------------

    /** User-set maximum bandwidth in bps (loaded from prefs as kbps × 1024). */
    private var maxBandwidth: Float = 0.0f

    /** Current effective bandwidth in bps after dynamic adjustment. */
    private var currentBandwidth: Float = 0.0f

    /** Current per-channel allocation. */
    private var current: ThrottleGroupSet = ThrottleGroupSet()

    /** Fraction of maxBandwidth actually in use (dynamic throttle). */
    private var throttleFrac: Float = MAX_FRACTIONAL

    /** Peak buffer load rate since last dynamic update tick. */
    private var bufferLoadRate: Float = 0.0f

    /** Timestamp (ms) of the last dynamic update tick. */
    private var lastUpdateTimestampMs: Long = System.currentTimeMillis()

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Return the maximum bandwidth preference value in kbps.
     * Corresponds to C++ LLViewerThrottle::getMaxBandwidthKbps() (static).
     */
    fun getMaxBandwidthKbps(): Float {
        // reads ThrottleBandwidthKBPS from gSavedSettings when settings are ported
        return maxBandwidth / 1024.0f
    }

    /** Return the current effective bandwidth in bps. */
    fun getCurrentBandwidth(): Float = currentBandwidth

    /** Return the current per-channel allocation snapshot. */
    fun getCurrentGroup(): ThrottleGroupSet = current

    /** Return the total currently-allocated bandwidth in kbps. */
    fun getTotal(): Float = current.total

    /**
     * Set a new maximum bandwidth and reload the throttle group.
     * Corresponds to C++ LLViewerThrottle::setMaxBandwidth().
     *
     * @param kbps       New limit in kbps.
     * @param fromEvent  True if called from a prefs-change listener (skips
     *                   re-writing the saved setting to avoid a feedback loop).
     */
    fun set(kbps: Float, fromEvent: Boolean = false) {
        if (!fromEvent) {
            // gSavedSettings.setF32("ThrottleBandwidthKBPS", kbps) — when settings are ported
        }
        load(kbps)
        // if gAgent.getRegion() != null → sendToSim() — when agent is ported
    }

    /**
     * (Re-)load the throttle from the given bandwidth value.
     * Corresponds to C++ LLViewerThrottle::load().
     */
    fun load(kbps: Float = getMaxBandwidthKbps()) {
        maxBandwidth = kbps * 1024.0f
        resetDynamicThrottle()
    }

    /**
     * Save the current maximum bandwidth back to the preference store.
     * Corresponds to C++ LLViewerThrottle::save().
     */
    fun save() {
        // gSavedSettings.setF32("ThrottleBandwidthKBPS", maxBandwidth / 1024f) — when settings are ported
    }

    /**
     * Send the current allocation to the sim.
     * Corresponds to C++ LLViewerThrottle::sendToSim().
     */
    fun sendToSim(): Unit = current.sendToSim()

    /**
     * Notify the throttle of a buffer load observation.  The peak value
     * is retained until the next dynamic update tick.
     * Corresponds to C++ LLViewerThrottle::setBufferLoadRate().
     */
    fun setBufferLoadRate(rate: Float) {
        bufferLoadRate = maxOf(bufferLoadRate, rate)
    }

    /**
     * Periodically tighten or ease the bandwidth allocation based on packet-
     * loss statistics and buffer load.
     * Corresponds to C++ LLViewerThrottle::updateDynamicThrottle().
     *
     * @param meanPacketsLostPct Recent mean packet-loss percentage (0–100).
     */
    fun updateDynamicThrottle(meanPacketsLostPct: Float) {
        val nowMs = System.currentTimeMillis()
        if ((nowMs - lastUpdateTimestampMs) / 1000.0f < DYNAMIC_UPDATE_DURATION_SEC) return
        lastUpdateTimestampMs = nowMs

        val shouldTighten = meanPacketsLostPct > TIGHTEN_THROTTLE_THRESHOLD_PCT
                || bufferLoadRate >= HIGH_BUFFER_LOAD_THRESHOLD

        val shouldEase = meanPacketsLostPct <= EASE_THROTTLE_THRESHOLD_PCT
                && bufferLoadRate < LOW_BUFFER_LOAD_THRESHOLD

        when {
            shouldTighten -> {
                if (throttleFrac <= MIN_FRACTIONAL ||
                    currentBandwidth / 1024.0f <= MIN_BANDWIDTH_KBPS
                ) return
                throttleFrac = maxOf(MIN_FRACTIONAL, throttleFrac - STEP_FRACTIONAL)
                currentBandwidth = maxBandwidth * throttleFrac
                current = getThrottleGroup(currentBandwidth / 1024.0f)
                current.sendToSim()
            }
            shouldEase -> {
                if (throttleFrac >= MAX_FRACTIONAL ||
                    currentBandwidth / 1024.0f >= MAX_BANDWIDTH_KBPS
                ) return
                throttleFrac = minOf(MAX_FRACTIONAL, throttleFrac + STEP_FRACTIONAL)
                currentBandwidth = maxBandwidth * throttleFrac
                current = getThrottleGroup(currentBandwidth / 1024.0f)
                current.sendToSim()
            }
        }

        bufferLoadRate = 0.0f
    }

    /**
     * Reset the dynamic throttle to the maximum allowed fraction.
     * Corresponds to C++ LLViewerThrottle::resetDynamicThrottle().
     */
    fun resetDynamicThrottle() {
        throttleFrac = MAX_FRACTIONAL
        currentBandwidth = maxBandwidth * MAX_FRACTIONAL
        current = getThrottleGroup(currentBandwidth / 1024.0f)
    }

    /**
     * Compute a [ThrottleGroupSet] for the given [bandwidthKbps] by
     * interpolating or extrapolating between the four hard-coded presets.
     *
     * Corresponds to C++ LLViewerThrottle::getThrottleGroup().
     */
    fun getThrottleGroup(bandwidthKbps: Float): ThrottleGroupSet {
        val clamped = bandwidthKbps.coerceIn(MIN_BANDWIDTH_KBPS, MAX_BANDWIDTH_KBPS)
        val count = presets.size

        var i = 0
        while (i < count && presets[i].total <= clamped) i++

        return when {
            i == 0 -> {
                // Below the lowest preset — return the minimum
                presets[0]
            }
            i == count -> {
                // Above the highest preset — extrapolate from the last two
                val deltaBw = clamped - presets[count - 1].total
                val delta = presets[count - 1] - presets[count - 2]
                val frac = if (delta.total != 0f) deltaBw / delta.total else 0f
                presets[count - 1] + delta * frac
            }
            else -> {
                // Between two presets — interpolate
                val deltaBw = clamped - presets[i - 1].total
                val delta = presets[i] - presets[i - 1]
                val frac = if (delta.total != 0f) deltaBw / delta.total else 0f
                presets[i - 1] + delta * frac
            }
        }
    }

    /**
     * Return per-group [ThrottleSetting] list for the current allocation.
     * Convenience accessor that exposes the snapshot as a typed list.
     */
    fun getCurrentSettings(): List<ThrottleSetting> =
        ThrottleGroup.entries.map { group ->
            ThrottleSetting(group = group, kbps = current[group])
        }
}
