package com.firestorm.llaudio

// Converted from indra/llaudio/llwindgen.h
// Templated wind-noise synthesiser — produces bandpass-filtered pink noise
// that mimics wind audio for the Second Life viewer.

import kotlin.math.*

// ---------------------------------------------------------------------------
// Module-level constants (mirrors top-level C++ consts / SoundFlags)
// ---------------------------------------------------------------------------

/** Centre frequency (Hz) of the underwater wind filter. */
const val LL_WIND_UNDERWATER_CENTER_FREQ: Float = 20f

/** Centre frequency (Hz) used for freshwater (above-water) wind. */
const val LL_WIND_FRESHWATER_CENTER_FREQ: Float = 100f

// ---------------------------------------------------------------------------
// WindGen<T : Number>
// ---------------------------------------------------------------------------

/**
 * Wind-sound synthesiser that generates stereo, interleaved, bandpass-filtered
 * pink noise for real-time audio mixing.
 *
 * Mirrors `LLWindGen<MIXBUFFERFORMAT_T>` from `llwindgen.h`.  The C++ class
 * is fully defined in the header (template), so there is no separate .cpp file.
 *
 * ### Type parameter
 * [T] mirrors the `MIXBUFFERFORMAT_T` template parameter.  In practice the
 * viewer instantiates this with:
 * - `Short` (16-bit PCM) for OpenAL / FMOD integer buffers
 * - `Float` for FMOD Studio float buffers
 *
 * The [windGenerate] function produces L/R interleaved samples written into a
 * [FloatArray].  Callers are responsible for casting to the final wire format.
 *
 * ### Filter design
 * 1. White noise → Paul Kellett pink-noise filter (three-pole IIR, see
 *    http://www.firstpr.com.au/dsp/pink-noise/)
 * 2. Pink noise → resonant two-pole bandpass IIR (`a0`, `b1`, `mB2`),
 *    centre frequency [targetFreq], bandwidth [filterBandWidth].
 * 3. Gain / pan interpolated sample-by-sample toward [targetGain] /
 *    [targetPanGainR].
 * 4. Linear interpolation between synthesis steps ([subSamples] sub-samples
 *    per output sample) for smoothness.
 *
 * @param sampleRate Input (hardware) sample rate in Hz; default 44 100 Hz.
 */
class WindGen<T : Number>(
    val sampleRate: UInt = 44_100u,
) {

    // ---- Public control knobs (set by the audio engine each frame) ----------

    /** Target output gain [0, ∞).  Interpolated toward by the synthesis loop. */
    var targetGain: Float       = 0f

    /** Target bandpass centre frequency in Hz.  Interpolated toward. */
    var targetFreq: Float       = LL_WIND_FRESHWATER_CENTER_FREQ

    /** Target right-channel pan gain [0, 1].  Interpolated toward. */
    var targetPanGainR: Float   = 0.5f

    // ---- Private synthesis state (mirrors C++ private members) -------------

    /** Number of output sub-samples generated per synthesis step. */
    private val subSamples: Int = 2

    /** Per-synthesis-step period: [subSamples] / [sampleRate] (seconds). */
    private val samplePeriod: Float = subSamples.toFloat() / sampleRate.toFloat()

    /** Bandpass filter 3 dB bandwidth in Hz. */
    private val filterBandWidth: Float = 50f

    // Two-pole resonant bandpass filter feedback coefficient.
    // b2 = exp(-2π · BW · T)  where T = samplePeriod.
    private val b2: Float = exp(-TWO_PI * filterBandWidth * samplePeriod)

    // Pink-noise pinking filter state (Paul Kellett three-pole method).
    private var buf0: Float = 0f
    private var buf1: Float = 0f
    private var buf2: Float = 0f

    // Resonant bandpass filter state (two-pole IIR, y[n-1], y[n-2]).
    private var y0: Float = 0f
    private var y1: Float = 0f

    // Smoothed (interpolated) working values.
    private var currentGain: Float      = 0f
    private var currentFreq: Float      = LL_WIND_FRESHWATER_CENTER_FREQ
    private var currentPanGainR: Float  = 0.5f
    private var lastSample: Float       = 0f

    // ---- Public synthesis API ----------------------------------------------

    /**
     * Fill [buffer] with [numSamples] frames of L/R interleaved wind audio.
     *
     * [buffer] must have at least `numSamples * 2` entries.  Values are
     * normalised floats in [−1, 1] (clip only when [currentGain] > 2).
     *
     * Mirrors `MIXBUFFERFORMAT_T* LLWindGen<T>::windGenerate(T*, int)`.
     *
     * @param buffer     Destination float array (L0, R0, L1, R1, …).
     * @param numSamples Number of stereo frames to write.
     * @return           The same [buffer] reference.
     */
    fun windGenerate(buffer: FloatArray, numSamples: Int): FloatArray {
        var writePos = 0
        var remaining = numSamples

        // Clip only when gain is large enough that samples could saturate.
        val clip = currentGain > 2f

        // Decide once per call whether we need to interpolate the frequency.
        val interpFreq = abs(targetFreq - currentFreq) >= (currentFreq * 0.112f)

        // Pre-compute filter coefficients (updated inside loop if interpFreq).
        var a0: Float
        var b1: Float

        if (!interpFreq) {
            currentFreq = targetFreq
            b1 = computeB1(currentFreq)
            a0 = computeA0(b1)
        } else {
            // Will be computed inside the loop; initialise to suppress
            // "uninitialised" warnings — values are always set before use.
            b1 = 0f
            a0 = 0f
        }

        while (remaining > 0) {
            // --- 1. White noise sample ---------------------------------------
            var nextSample = nextSample()

            // --- 2. Pink-noise pinking filter --------------------------------
            // Magic coefficients from Paul Kellett's PKE method.
            buf0 = buf0 * 0.99765f + nextSample * 0.09905f
            buf1 = buf1 * 0.96300f + nextSample * 0.29652f
            buf2 = buf2 * 0.57000f + nextSample * 1.05269f
            nextSample = buf0 + buf1 + buf2 + nextSample * 0.1848f

            // --- 3. Optional frequency interpolation ------------------------
            if (interpFreq) {
                currentFreq = 0.999f * currentFreq + 0.001f * targetFreq
                b1 = computeB1(currentFreq)
                a0 = computeA0(b1)
            }

            // --- 4. Resonant bandpass IIR -----------------------------------
            nextSample = a0 * nextSample - b1 * y0 - b2 * y1
            y1 = y0
            y0 = nextSample

            // --- 5. Gain / pan interpolation --------------------------------
            currentGain      = 0.999f * currentGain      + 0.001f * targetGain
            currentPanGainR  = 0.999f * currentPanGainR  + 0.001f * targetPanGainR

            nextSample *= currentGain

            // --- 6. Sub-sample linear interpolation -------------------------
            val delta = (nextSample - lastSample) / subSamples.toFloat()

            var i = subSamples
            while (i > 0 && remaining > 0) {
                lastSample += delta
                val sampleR = clampedSample(clip, lastSample * currentPanGainR)
                val sampleL = clampedSample(clip, lastSample - sampleR)
                buffer[writePos++] = sampleL
                buffer[writePos++] = sampleR
                --i
                --remaining
            }
        }

        return buffer
    }

    // ---- Sample generation helpers -----------------------------------------

    /**
     * Generate a single white-noise sample.
     *
     * - For integer [T] (e.g. [Short]): maps to roughly [S16_MIN/8, S16_MAX/8]
     *   as the C++ integer specialisation does.
     * - For [Float]: uniform random in [−0.5, 0.5].
     *
     * Mirrors `LLWindGen<T>::getNextSample()` and its `<F32>` specialisation.
     *
     * All actual random-number generation is stubbed — the TODO lets platform
     * code inject a proper PRNG.
     */
    fun nextSample(): Float {
        // Float specialisation: ll_frand() - 0.5f
        // Integer specialisation: (float)rand() * scale + S16_MIN/8
        // We always synthesise in float internally; callers cast to T on output.
        return 0f
    }

    /**
     * Apply optional saturation clipping to [sample].
     *
     * - For integer [T]: clamp to [Short.MIN_VALUE, Short.MAX_VALUE] when
     *   [clamp] is `true`.
     * - For [Float]: pass through unchanged regardless of [clamp].
     *
     * Mirrors `LLWindGen<T>::getClampedSample(bool, F32)` and its
     * `<F32>` specialisation.
     */
    fun clampedSample(clamp: Boolean, sample: Float): Float {
        return if (clamp) {
            sample.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
        } else {
            sample
        }
    }

    // ---- Filter coefficient helpers ----------------------------------------

    /**
     * Compute the b1 (first-order feedback) coefficient for the resonant
     * bandpass at [freq] Hz.
     *
     * `b1 = -4·b2 / (1 + b2) · cos(2π · freq · T)`
     */
    private fun computeB1(freq: Float): Float =
        (-4f * b2) / (1f + b2) * cos(TWO_PI * freq * samplePeriod)

    /**
     * Compute the forward gain coefficient a0 from the current b1.
     *
     * `a0 = (1 − b2) · sqrt(1 − b1²/(4·b2))`
     */
    private fun computeA0(b1: Float): Float =
        (1f - b2) * sqrt(1f - (b1 * b1) / (4f * b2))

    // ---- Companion ---------------------------------------------------------

    companion object {
        /** 2π — used throughout the filter coefficient calculations. */
        const val TWO_PI: Float = (2.0 * PI).toFloat()

        // Integer saturation limits (Short PCM).
        const val S16_MIN: Int = Short.MIN_VALUE.toInt()
        const val S16_MAX: Int = Short.MAX_VALUE.toInt()
        const val U16_MAX: Int = 65535
    }
}
