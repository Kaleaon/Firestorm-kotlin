package com.firestorm.llcommon

/**
 * High-resolution timer, translated from lltimer.h / lltimer.cpp.
 *
 * Internally uses [System.nanoTime] for sub-millisecond precision, matching
 * the C++ implementation that reads the CPU clock counter via RDTSC or
 * platform equivalents.  Epoch-relative helpers use [System.currentTimeMillis]
 * to stay consistent with wall-clock time.
 *
 * C++ counterpart: indra/llcommon/lltimer.h + lltimer.cpp
 */
class LLTimer {

    // Nanosecond timestamp captured at the last reset() / start()
    private var startNanos: Long = System.nanoTime()

    // Expiry timestamp used by hasExpired() / checkExpirationAndReset()
    private var expiryNanos: Long = Long.MAX_VALUE

    // Whether the timer is currently running (mirrors C++ mStarted)
    private var started: Boolean = true

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Reset and mark the timer as running. */
    fun start() {
        reset()
        started = true
    }

    /** Mark the timer as stopped (does not clear the accumulated time). */
    fun stop() {
        started = false
    }

    /** Reset the origin of this timer to right now. */
    fun reset() {
        startNanos = System.nanoTime()
    }

    fun getStarted(): Boolean = started

    // -------------------------------------------------------------------------
    // Elapsed time
    // -------------------------------------------------------------------------

    /** Elapsed seconds since last reset, as a Float (F32 in C++). */
    fun getElapsedTimeF32(): Float =
        ((System.nanoTime() - startNanos) / 1_000_000_000.0).toFloat()

    /** Elapsed seconds since last reset, as a Double (F64 in C++). */
    fun getElapsedTimeF64(): Double =
        (System.nanoTime() - startNanos) / 1_000_000_000.0

    /** Elapsed time as Float, then immediately reset. */
    fun getElapsedTimeAndResetF32(): Float {
        val elapsed = getElapsedTimeF32()
        reset()
        return elapsed
    }

    /** Elapsed time as Double, then immediately reset. */
    fun getElapsedTimeAndResetF64(): Double {
        val elapsed = getElapsedTimeF64()
        reset()
        return elapsed
    }

    // -------------------------------------------------------------------------
    // Expiration
    // -------------------------------------------------------------------------

    /**
     * Returns true if [maxTime] seconds have elapsed since the last reset.
     * This variant accepts the maximum time directly, matching the spec's
     * `hasExpired(maxTime: Float): Boolean` signature.
     */
    fun hasExpired(maxTime: Float): Boolean =
        getElapsedTimeF32() >= maxTime

    /**
     * Returns true if the timer has passed the expiry set by [setTimerExpirySec].
     * Equivalent to the no-arg C++ `hasExpired()` that checks the internal expiry tick.
     */
    fun hasExpired(): Boolean = System.nanoTime() >= expiryNanos

    /** Set an absolute expiry [secs] seconds from now. */
    fun setTimerExpirySec(secs: Double) {
        expiryNanos = System.nanoTime() + (secs * 1_000_000_000.0).toLong()
    }

    /** Convenience overload accepting Float seconds. */
    fun setTimerExpirySec(secs: Float) = setTimerExpirySec(secs.toDouble())

    /**
     * If the timer has expired, reset the expiry to [expiration] seconds from
     * now, reset the elapsed counter, and return true.  Otherwise return false.
     */
    fun checkExpirationAndReset(expiration: Float): Boolean {
        if (hasExpired()) {
            setTimerExpirySec(expiration.toDouble())
            reset()
            return true
        }
        return false
    }

    /** Remaining seconds until expiry, clamped to zero. */
    fun getRemainingTimeF32(): Float {
        val remaining = (expiryNanos - System.nanoTime()) / 1_000_000_000.0f
        return if (remaining < 0f) 0f else remaining
    }

    // -------------------------------------------------------------------------
    // Companion – static helpers matching the C++ static LLTimer API
    // -------------------------------------------------------------------------
    companion object {
        /**
         * Global timer instance – equivalent to `LLTimer* LLTimer::sTimer`.
         * Initialised eagerly so that [getTotalSeconds] / [getElapsedSeconds]
         * work from program start without an explicit `initClass()` call.
         */
        @JvmStatic var sTimer: LLTimer? = LLTimer()

        /**
         * Seconds since the JVM epoch (1970-01-01T00:00:00Z).
         * Maps to C++ `LLTimer::getTotalSeconds()`.
         */
        @JvmStatic fun getTotalSeconds(): Double =
            System.currentTimeMillis() / 1_000.0

        /**
         * Seconds since the JVM epoch as a Double, matching the spec's
         * `getEpochSeconds(): Double`.
         */
        @JvmStatic fun getEpochSeconds(): Double = getTotalSeconds()

        /**
         * Current time in nanoseconds via [System.nanoTime].
         * Maps to the spec's `getCurrentTime(): Long`.
         */
        @JvmStatic fun getCurrentTime(): Long = System.nanoTime()

        /**
         * Microseconds since epoch – mirrors C++ `LLTimer::getTotalTime()`
         * which returns `U64MicrosecondsImplicit`.
         */
        @JvmStatic fun getTotalTime(): Long = System.currentTimeMillis() * 1_000L

        /**
         * Seconds elapsed since program start, delegating to the global
         * [sTimer] instance.
         */
        @JvmStatic fun getElapsedSeconds(): Double =
            sTimer?.getElapsedTimeF64() ?: 0.0

        /** No-op stub – C++ implementation parks the CPU; JVM has no equivalent. */
        @JvmStatic fun knifeCPU() { /* JVM scheduler manages CPU yielding automatically */ }
    }
}

// =============================================================================
// LLFrameTimer – frame-coherent timer that advances only when update() is called
// =============================================================================

/**
 * Frame-coherent timer analogous to the C++ LLFrameTimer.
 *
 * [frameTimeNanos] is a shared, monotonically advancing timestamp updated once
 * per frame by calling [LLFrameTimer.update].  Individual [LLFrameTimer]
 * instances measure elapsed time relative to that shared reference, so all
 * timers agree on "now" within a single frame.
 */
class LLFrameTimer {

    private var startNanos: Long = frameTimeNanos
    private var expiryNanos: Long = Long.MAX_VALUE

    fun reset() { startNanos = frameTimeNanos }

    fun getElapsedSeconds(): Double =
        (frameTimeNanos - startNanos) / 1_000_000_000.0

    fun setTimerExpirySec(secs: Double) {
        expiryNanos = frameTimeNanos + (secs * 1_000_000_000.0).toLong()
    }

    fun hasExpired(): Boolean = frameTimeNanos >= expiryNanos

    companion object {
        @Volatile private var frameTimeNanos: Long = System.nanoTime()
        @Volatile private var frameCount: Long = 0L

        /** Must be called once per frame to advance the shared frame clock. */
        @JvmStatic fun update() {
            frameTimeNanos = System.nanoTime()
            frameCount++
        }

        @JvmStatic fun getFrameTime(): Double  = frameTimeNanos / 1_000_000_000.0
        @JvmStatic fun getFrameCount(): Long   = frameCount
    }
}
