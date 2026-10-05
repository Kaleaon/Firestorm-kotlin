package com.firestorm.llaudio

// Converted from indra/llaudio/llaudiodecodemgr.h
// Singleton manager that drives async Vorbis → PCM decode for audio assets.

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.Singleton

/**
 * Singleton manager for background audio (Vorbis OGG) decode operations.
 *
 * Mirrors [LLAudioDecodeMgr] from the C++ source, which is a
 * `LLSingleton<LLAudioDecodeMgr>`.  Kotlin equivalent: an inner class
 * backed by [Singleton] so it participates in the project singleton registry
 * and can be torn down cleanly in tests.
 *
 * Typical frame-update loop:
 * ```
 * val mgr = AudioDecodeMgr.getInstance()
 * mgr.addDecodeRequest(uuid)   // enqueue a new asset for decode
 * mgr.processQueue()            // spend a bounded time slice on pending work
 * ```
 *
 * The actual Vorbis decode work is performed by a [VorbisDecodeState]
 * worker object per asset.  All codec calls inside those workers are
 * marked [TODO] ("AUDIO: Vorbis").
 */
class AudioDecodeMgr private constructor() : Singleton<AudioDecodeMgr>() {

    // ---- Singleton factory ---------------------------------------------------

    companion object : SingletonFactory<AudioDecodeMgr>({ AudioDecodeMgr() })

    // ---- Internal impl (mirrors C++ nested Impl class) ----------------------

    private val impl: Impl = Impl()

    // ---- Public API ---------------------------------------------------------

    /**
     * Process the decode queue for one frame slice.
     *
     * Called every viewer frame from the audio idle loop.  Dequeues
     * [VorbisDecodeState] workers that are ready and advances them;
     * workers that complete are forwarded to [AudioEngine.updateBufferForData].
     *
     * Mirrors `LLAudioDecodeMgr::processQueue()`.
     */
    fun processQueue() {
        impl.processQueue()
    }

    /**
     * Enqueue asset [uuid] for Vorbis decode if it is not already queued.
     *
     * Returns true if the request was newly added; false if [uuid] is
     * already pending or has already been decoded.
     *
     * Mirrors `bool LLAudioDecodeMgr::addDecodeRequest(const LLUUID&)`.
     */
    fun addDecodeRequest(uuid: LLUUID): Boolean =
        impl.addDecodeRequest(uuid)

    /**
     * Enqueue asset [uuid] for WAV load after it has been decoded.
     *
     * Mirrors `void LLAudioDecodeMgr::addAudioRequest(const LLUUID&)`.
     */
    fun addAudioRequest(uuid: LLUUID) {
        impl.addAudioRequest(uuid)
    }

    // ---- Lifecycle ----------------------------------------------------------

    override fun initSingleton() {
        // No startup work required — the impl queue starts empty.
    }

    override fun cleanupSingleton() {
        impl.clear()
    }

    // ---- Internal implementation class (opaque in C++ header) ---------------

    /**
     * Hidden implementation detail, matching the pattern of the C++ nested
     * `LLAudioDecodeMgr::Impl` class whose declaration is in the .cpp file.
     */
    private class Impl {

        /** Assets waiting for a [VorbisDecodeState] worker to be assigned. */
        private val decodeQueue: ArrayDeque<LLUUID> = ArrayDeque()

        /** Assets whose PCM decode is complete and which need a WAV buffer load. */
        private val audioQueue: ArrayDeque<LLUUID>  = ArrayDeque()

        /** Active decode workers, keyed by asset UUID. */
        private val activeWorkers: MutableMap<LLUUID, VorbisDecodeState> = LinkedHashMap()

        /** UUIDs that have already been fully decoded this session. */
        private val completed: MutableSet<LLUUID> = HashSet()

        fun addDecodeRequest(uuid: LLUUID): Boolean {
            if (uuid in completed || uuid in activeWorkers || uuid in decodeQueue) return false
            decodeQueue.addLast(uuid)
            return true
        }

        fun addAudioRequest(uuid: LLUUID) {
            if (uuid !in audioQueue) audioQueue.addLast(uuid)
        }

        /**
         * Advance decode work for the current frame.
         *
         * The C++ implementation dequeues one or more [VorbisDecodeState]
         * workers per frame and calls their `decodeSection()` method until
         * either time expires or the decode completes.
         */
        fun processQueue() {
            // Promote queued requests into active workers
            while (decodeQueue.isNotEmpty()) {
                val uuid = decodeQueue.removeFirst()
                if (uuid !in activeWorkers && uuid !in completed) {
                    activeWorkers[uuid] = VorbisDecodeState(uuid)
                }
            }

            // Advance active workers
            val done = mutableListOf<LLUUID>()
            for ((uuid, worker) in activeWorkers) {
                val finished = worker.decodeSection()
                if (finished) {
                    done.add(uuid)
                    completed.add(uuid)
                    if (worker.isValid) {
                        addAudioRequest(uuid)
                    }
                }
            }
            done.forEach { activeWorkers.remove(it) }

            // Process audio (WAV-load) queue — hand off to the audio engine
            while (audioQueue.isNotEmpty()) {
                val uuid = audioQueue.removeFirst()
                System.err.println("AudioDecodeMgr: processQueue audio notification not yet implemented")
            }
        }

        fun clear() {
            decodeQueue.clear()
            audioQueue.clear()
            activeWorkers.clear()
            completed.clear()
        }
    }
}

// ---------------------------------------------------------------------------
// VorbisDecodeState — per-asset decode worker
// ---------------------------------------------------------------------------

/**
 * State machine that drives incremental Vorbis OGG → WAV decode for a
 * single audio asset identified by [uuid].
 *
 * Mirrors the [LLVorbisDecodeState] class referenced (but not defined) in
 * `llaudiodecodemgr.h`.  All codec calls are stubbed with
 * [TODO]("AUDIO: Vorbis ...").
 *
 * [decodeSection] is called once per frame slice; it returns `true` when
 * the decode is complete (whether successfully or not).
 */
class VorbisDecodeState(val uuid: LLUUID) {

    /** True after [decodeSection] returns `true` with no errors. */
    var isValid: Boolean = false
        private set

    private var finished: Boolean = false

    /**
     * Perform one decode pass on the Vorbis stream for [uuid].
     *
     * @return `true` when the decode is complete (call [isValid] to check
     *         whether it succeeded), `false` if more work remains.
     */
    fun decodeSection(): Boolean {
        if (finished) return true
        System.err.println("VorbisDecodeState: decodeSection not yet implemented")
        return false
    }
}
