/**
 * ViewerStatsRecorder.kt
 *
 * Kotlin conversion of llviewerstatsrecorder.h / llviewerstatsrecorder.cpp
 * Original: Record viewer events to a metrics/diagnostic log file.
 * Copyright (C) 2010, Linden Research, Inc. — LGPL 2.1
 */

package com.firestorm.newview

/**
 * Immutable snapshot of one recording frame's telemetry counters.
 *
 * Mirrors the per-interval stats that `LLViewerStatsRecorder::writeToLog()`
 * would write to a CSV file.
 *
 * All counts reset to zero at the start of each new frame / flush interval.
 *
 * @property capturedAtMs            Wall-clock time (epoch millis) when this
 *                                   frame was captured.
 * @property objectUpdateCount       Number of object-update messages processed
 *                                   (full + terse combined).
 * @property objectFullUpdates       Full object-update packets received.
 * @property objectTerseUpdates      Terse (compressed) object-update packets.
 * @property objectUpdateFailures    Updates that could not be applied.
 * @property objectKills             Objects removed from the scene.
 * @property objectCacheHits         Cache hits (object already known locally).
 * @property objectCacheMissFull     Full cache misses (object not in cache).
 * @property objectCacheMissCrc      CRC-mismatch cache misses.
 * @property objectCacheMissRequests Explicit cache-miss re-request messages sent.
 * @property objectCacheUpdateDupes  Cache-update packets that were duplicates.
 * @property objectCacheUpdateChanges Cache-update packets that changed existing entries.
 * @property objectCacheUpdateAdds   Cache-update packets that added new entries.
 * @property objectCacheUpdateReplacements Cache-update packets that replaced entries.
 * @property avatarUpdateCount       Number of avatar-update packets received.
 * @property textureFetchCount       Number of texture fetches initiated.
 * @property textureFetchBytes       Cumulative bytes fetched for textures.
 * @property meshLoadedCount         Number of mesh assets fully loaded.
 */
data class StatsFrame(
    val capturedAtMs: Long,

    // Object update counters
    val objectUpdateCount: Int,
    val objectFullUpdates: Int,
    val objectTerseUpdates: Int,
    val objectUpdateFailures: Int,
    val objectKills: Int,

    // Object cache counters
    val objectCacheHits: Int,
    val objectCacheMissFull: Int,
    val objectCacheMissCrc: Int,
    val objectCacheMissRequests: Int,
    val objectCacheUpdateDupes: Int,
    val objectCacheUpdateChanges: Int,
    val objectCacheUpdateAdds: Int,
    val objectCacheUpdateReplacements: Int,

    // Avatar counters
    val avatarUpdateCount: Int,

    // Texture counters
    val textureFetchCount: Int,
    val textureFetchBytes: Int,

    // Mesh counters
    val meshLoadedCount: Int
) {
    /** Convenience: total object-cache events in this frame. */
    val totalCacheEvents: Int
        get() = objectCacheHits +
                objectCacheMissFull +
                objectCacheMissCrc +
                objectCacheMissRequests +
                objectCacheUpdateDupes +
                objectCacheUpdateChanges +
                objectCacheUpdateAdds +
                objectCacheUpdateReplacements

    /** True if every counter is zero (nothing happened in this frame). */
    val isAllZero: Boolean
        get() = objectUpdateCount == 0 &&
                objectFullUpdates == 0 &&
                objectTerseUpdates == 0 &&
                objectUpdateFailures == 0 &&
                objectKills == 0 &&
                totalCacheEvents == 0 &&
                avatarUpdateCount == 0 &&
                textureFetchCount == 0 &&
                textureFetchBytes == 0 &&
                meshLoadedCount == 0
}

// ---------------------------------------------------------------------------
// ViewerStatsRecorder singleton
// ---------------------------------------------------------------------------

/**
 * In-memory telemetry recorder for viewer events.
 *
 * Mirrors `LLViewerStatsRecorder` (`LLSimpleton<LLViewerStatsRecorder>`).
 *
 * Thread-safety note: The C++ version uses an `LLMutex` around file writes.
 * This Kotlin port is not yet thread-safe; callers on the JVM should ensure
 * access from a single thread or add `@Synchronized` / `AtomicInteger` as
 * needed.
 */
object ViewerStatsRecorder {

    // -----------------------------------------------------------------------
    // Enable flags
    // -----------------------------------------------------------------------

    /**
     * Master recording switch.
     * Maps to `LLViewerStatsRecorder::mEnableStatsRecording`.
     */
    var enabled: Boolean = false
        private set

    /**
     * Whether to write stats to a log file (separate from in-memory tracking).
     * Maps to `LLViewerStatsRecorder::mEnableStatsLogging`.
     */
    var logging: Boolean = false
        private set

    /**
     * When true, frames where every counter is zero are silently discarded.
     * Maps to `LLViewerStatsRecorder::mSkipSaveIfZeros`.
     */
    var skipZeroFrames: Boolean = false

    // -----------------------------------------------------------------------
    // Recording interval
    // -----------------------------------------------------------------------

    /**
     * Minimum seconds between consecutive frame flushes.
     * Default matches the C++ initialiser (0.2 s).
     */
    var intervalSeconds: Float = 0.2f

    /**
     * Maximum total recording duration in seconds before logging stops.
     * Default matches the C++ initialiser (300 s = 5 minutes).
     */
    var maxDurationSeconds: Float = 300f

    // -----------------------------------------------------------------------
    // Mutable counters (all reset by [clearStats])
    // -----------------------------------------------------------------------

    private var objectFullUpdates: Int = 0
    private var objectTerseUpdates: Int = 0
    private var objectUpdateFailures: Int = 0
    private var objectKills: Int = 0

    private var objectCacheHits: Int = 0
    private var objectCacheMissFull: Int = 0
    private var objectCacheMissCrc: Int = 0
    private var objectCacheMissRequests: Int = 0
    private var objectCacheUpdateDupes: Int = 0
    private var objectCacheUpdateChanges: Int = 0
    private var objectCacheUpdateAdds: Int = 0
    private var objectCacheUpdateReplacements: Int = 0

    private var avatarUpdateCount: Int = 0

    private var textureFetchCount: Int = 0
    private var textureFetchBytes: Int = 0

    private var meshLoadedCount: Int = 0

    // History of captured frames (newest last).
    private val frameHistory: MutableList<StatsFrame> = mutableListOf()

    // -----------------------------------------------------------------------
    // Enable / disable
    // -----------------------------------------------------------------------

    /**
     * Enable or disable recording (and optionally logging).
     * Maps to `LLViewerStatsRecorder::enableObjectStatsRecording()`.
     *
     * When [logToFile] is false and logging was previously active, the current
     * frame is flushed and logging stops.
     */
    fun enableObjectStatsRecording(enable: Boolean, logToFile: Boolean = false) {
        if (!logToFile && logging) {
            // Flush any pending data before stopping the log.
            flush()
        }
        enabled = enable
        logging = logToFile
        if (!enable) clearStats()
    }

    // -----------------------------------------------------------------------
    // Counter-increment helpers
    // -----------------------------------------------------------------------

    /**
     * Record that [objectCount] object-update packets were processed.
     *
     * In the C++ viewer the distinction between full and terse updates is
     * tracked separately via `objectUpdateEvent(EObjectUpdateType)`.  This
     * higher-level helper increments the combined total; use
     * [recordObjectFullUpdate] / [recordObjectTerseUpdate] for finer control.
     *
     * Maps to `LLViewerStatsRecorder::objectUpdateEvent()`.
     */
    fun recordObjectUpdate(objectCount: Int) {
        if (!enabled) return
        objectFullUpdates += objectCount // treat as full updates by default
    }

    /** Record one full object-update packet. */
    fun recordObjectFullUpdate() {
        if (!enabled) return
        objectFullUpdates++
    }

    /** Record one terse (compressed) object-update packet. */
    fun recordObjectTerseUpdate() {
        if (!enabled) return
        objectTerseUpdates++
    }

    /** Record [count] failed object-update attempts. */
    fun recordObjectUpdateFailure(count: Int = 1) {
        if (!enabled) return
        objectUpdateFailures += count
    }

    /** Record [numObjects] objects removed from the scene. */
    fun recordObjectKills(numObjects: Int) {
        if (!enabled) return
        objectKills += numObjects
    }

    // Cache

    /** Record a cache hit. Maps to `LLViewerStatsRecorder::cacheHitEvent()`. */
    fun recordCacheHit() {
        if (!enabled) return
        objectCacheHits++
    }

    /**
     * Record a cache miss of the given type.
     * Maps to `LLViewerStatsRecorder::cacheMissEvent()`.
     *
     * @param fullMiss true = full/total miss; false = CRC mismatch.
     */
    fun recordCacheMiss(fullMiss: Boolean) {
        if (!enabled) return
        if (fullMiss) objectCacheMissFull++ else objectCacheMissCrc++
    }

    /** Record [count] explicit cache-miss re-request messages. */
    fun recordCacheMissRequests(count: Int) {
        if (!enabled) return
        objectCacheMissRequests += count
    }

    /**
     * Record the result of a full cache-update.
     * Maps to `LLViewerStatsRecorder::cacheFullUpdate()`.
     */
    fun recordCacheFullUpdate(result: CacheUpdateResult) {
        if (!enabled) return
        when (result) {
            CacheUpdateResult.DUPE        -> objectCacheUpdateDupes++
            CacheUpdateResult.CHANGED     -> objectCacheUpdateChanges++
            CacheUpdateResult.ADDED       -> objectCacheUpdateAdds++
            CacheUpdateResult.REPLACED    -> objectCacheUpdateReplacements++
        }
    }

    // Avatar

    /**
     * Record that [count] avatar-update packets were received.
     * Maps to the C++ `avatarUpdate` tracking (implicit in object-update path).
     */
    fun recordAvatarUpdate(count: Int) {
        if (!enabled) return
        avatarUpdateCount += count
    }

    // Texture

    /**
     * Record that [count] texture fetches were initiated, transferring [bytes]
     * total bytes.
     * Expands `LLViewerStatsRecorder::textureFetch()` with byte accounting.
     */
    fun recordTextureFetch(count: Int, bytes: Int) {
        if (!enabled) return
        textureFetchCount += count
        textureFetchBytes += bytes
    }

    /** Record one texture fetch (no byte count). */
    fun recordTextureFetch() {
        if (!enabled) return
        textureFetchCount++
    }

    // Mesh

    /** Record that one mesh asset was fully loaded.
     * Maps to `LLViewerStatsRecorder::meshLoaded()`. */
    fun recordMeshLoaded() {
        if (!enabled) return
        meshLoadedCount++
    }

    // -----------------------------------------------------------------------
    // Frame capture
    // -----------------------------------------------------------------------

    /**
     * Snapshot the current counters into a [StatsFrame], append it to the
     * history, and reset all counters.
     *
     * Maps to the periodic-flush logic inside
     * `LLViewerStatsRecorder::writeToLog()` / `idle()`.
     *
     * If [skipZeroFrames] is true and every counter is zero, no frame is
     * appended and the function returns null.
     */
    fun flush(): StatsFrame? {
        val frame = buildFrame()
        if (skipZeroFrames && frame.isAllZero) return null
        frameHistory.add(frame)
        clearStats()
        return frame
    }

    /**
     * Return a snapshot of the current (unflushed) counters without resetting
     * them.
     * Maps to the caller's need to inspect live stats at any point.
     */
    fun getFrameStats(): StatsFrame = buildFrame()

    /** Return a read-only view of all flushed frames since recording started. */
    fun getFrameHistory(): List<StatsFrame> = frameHistory.toList()

    /** Clear the frame history. */
    fun clearHistory() {
        frameHistory.clear()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun buildFrame(): StatsFrame = StatsFrame(
        capturedAtMs = System.currentTimeMillis(),
        objectUpdateCount = objectFullUpdates + objectTerseUpdates,
        objectFullUpdates = objectFullUpdates,
        objectTerseUpdates = objectTerseUpdates,
        objectUpdateFailures = objectUpdateFailures,
        objectKills = objectKills,
        objectCacheHits = objectCacheHits,
        objectCacheMissFull = objectCacheMissFull,
        objectCacheMissCrc = objectCacheMissCrc,
        objectCacheMissRequests = objectCacheMissRequests,
        objectCacheUpdateDupes = objectCacheUpdateDupes,
        objectCacheUpdateChanges = objectCacheUpdateChanges,
        objectCacheUpdateAdds = objectCacheUpdateAdds,
        objectCacheUpdateReplacements = objectCacheUpdateReplacements,
        avatarUpdateCount = avatarUpdateCount,
        textureFetchCount = textureFetchCount,
        textureFetchBytes = textureFetchBytes,
        meshLoadedCount = meshLoadedCount
    )

    /**
     * Reset all mutable counters to zero.
     * Maps to `LLViewerStatsRecorder::clearStats()`.
     */
    fun clearStats() {
        objectFullUpdates = 0
        objectTerseUpdates = 0
        objectUpdateFailures = 0
        objectKills = 0
        objectCacheHits = 0
        objectCacheMissFull = 0
        objectCacheMissCrc = 0
        objectCacheMissRequests = 0
        objectCacheUpdateDupes = 0
        objectCacheUpdateChanges = 0
        objectCacheUpdateAdds = 0
        objectCacheUpdateReplacements = 0
        avatarUpdateCount = 0
        textureFetchCount = 0
        textureFetchBytes = 0
        meshLoadedCount = 0
    }

    // -----------------------------------------------------------------------
    // Cache-update result enum (mirrors LLViewerRegion::eCacheUpdateResult)
    // -----------------------------------------------------------------------

    enum class CacheUpdateResult {
        DUPE,
        CHANGED,
        ADDED,
        REPLACED
    }
}
