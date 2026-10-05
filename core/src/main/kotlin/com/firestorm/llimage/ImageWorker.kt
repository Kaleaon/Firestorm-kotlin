package com.firestorm.llimage

import com.firestorm.llcommon.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// ---------------------------------------------------------------------------
// DecodeHandle — identifies an in-flight decode request
// ---------------------------------------------------------------------------

/**
 * Opaque handle returned by [ImageWorker.requestDecodePriority].
 *
 * Corresponds to the `LLImageDecodeThread::handle_t` typedef (a plain `U32`)
 * in llimageworker.h, enriched here with the originating [imageId] for
 * diagnostics and cache-miss avoidance.
 *
 * @param id      Monotonically increasing request identifier; 0 is never valid.
 * @param imageId UUID of the asset being decoded (empty UUID for synthetic data).
 */
data class DecodeHandle(
    val id: Int,
    val imageId: LLUUID
)

// ---------------------------------------------------------------------------
// Internal request record
// ---------------------------------------------------------------------------

/**
 * Full decode request record held in the pending-work queue.
 *
 * Mirrors the `ImageRequest` inner class in llimageworker.cpp, adapted to
 * Kotlin's callback model.
 *
 * @param handle     The externally visible [DecodeHandle].
 * @param image      Formatted (compressed) source image.
 * @param priority   Scheduling priority (higher = sooner); not used by the
 *                   fixed-thread-pool but stored for future priority-queue use.
 * @param discard    Desired discard level; −1 = let the codec decide.
 * @param needsAux   When true, also decode the auxiliary (alpha) channel into
 *                   a separate single-channel [ImageRaw].
 * @param callback   Invoked on the worker thread with `true` on success.
 */
private data class DecodeWorkItem(
    val handle: DecodeHandle,
    val image: ImageFormatted,
    val priority: Float,
    val discard: Int,
    val needsAux: Boolean,
    val callback: (Boolean) -> Unit
)

// ---------------------------------------------------------------------------
// ImageWorker
// ---------------------------------------------------------------------------

/**
 * Asynchronous image-decode scheduler backed by a thread pool.
 *
 * Corresponds to `LLImageDecodeThread` in the C++ viewer source
 * (llimageworker.h / llimageworker.cpp).
 *
 * The C++ implementation posts work items to an `LL::ThreadPool` named
 * "ImageDecode" with 8 threads.  This Kotlin version uses a
 * [java.util.concurrent.ExecutorService] of the same size.
 *
 * Usage:
 * ```kotlin
 * val worker = ImageWorker()
 * val handle = worker.requestDecodePriority(
 *     image    = myJ2CImage,
 *     priority = 1.0f,
 *     discard  = 0
 * ) { success -> println("decoded: $success") }
 *
 * // … later on the main thread …
 * val completed = worker.update()   // returns number of finished requests
 * worker.shutdown()
 * ```
 *
 * @param threaded When false a single-threaded executor is used (useful for
 *                 deterministic unit tests).  Matches the C++ constructor
 *                 parameter `bool threaded = true`.
 */
open class ImageWorker(private val threaded: Boolean = true) {

    // ---- thread pool --------------------------------------------------------

    /**
     * The executor that runs [processRequest] tasks.
     *
     * Mirrors `std::unique_ptr<LL::ThreadPool> mThreadPool` with 8 threads,
     * as configured in `LLImageDecodeThread::LLImageDecodeThread`.
     */
    private val executor: ExecutorService =
        if (threaded) Executors.newFixedThreadPool(DECODE_THREAD_COUNT)
        else          Executors.newSingleThreadExecutor()

    // ---- request tracking ---------------------------------------------------

    /** Atomically incrementing request ID — never wraps to 0 (mirrors C++ `++mDecodeCount`). */
    private val nextId = AtomicInteger(0)

    /**
     * Map from handle ID to the [Future] of the submitted task.
     * Entries are removed when the future completes or [cancelDecode] is called.
     */
    private val pending: ConcurrentHashMap<Int, Future<*>> = ConcurrentHashMap()

    /** Running count of successfully completed decode tasks — mirrors `mDecodeCount`. */
    private val decodeCount = AtomicInteger(0)

    // ---- public API ---------------------------------------------------------

    /**
     * Enqueue a decode request and return a handle for tracking or cancellation.
     *
     * Corresponds to `LLImageDecodeThread::decodeImage(image, discard, needs_aux, responder)`.
     * The Kotlin API consolidates the responder into a lambda [callback] and adds
     * [priority] for future priority-queue integration.
     *
     * @param image    Compressed source image; must already have [data] populated.
     * @param priority Scheduling priority hint (higher = more urgent).
     * @param discard  Target discard level (0 = full resolution).
     * @param needsAux True to also decode the auxiliary channel.
     * @param callback Invoked on the worker thread with `true` on success.
     * @return A [DecodeHandle] that uniquely identifies this request, or a handle
     *         with id = 0 if the executor has been shut down.
     */
    fun requestDecodePriority(
        image: ImageFormatted,
        priority: Float,
        discard: Int,
        needsAux: Boolean = false,
        callback: (Boolean) -> Unit
    ): DecodeHandle {
        // Generate a non-zero ID (mirrors the `if (decode_id == 0) decode_id = ++mDecodeCount` guard)
        var id = nextId.incrementAndGet()
        if (id == 0) id = nextId.incrementAndGet()

        val handle = DecodeHandle(id = id, imageId = LLUUID.NULL)
        val item   = DecodeWorkItem(handle, image, priority, discard, needsAux, callback)

        val future = runCatching {
            executor.submit { processRequest(item) }
        }.getOrNull() ?: return DecodeHandle(id = 0, imageId = LLUUID.NULL)

        pending[id] = future
        return handle
    }

    /**
     * Cancel a pending decode request identified by [handle].
     *
     * If the task is already running it will not be interrupted (best-effort
     * cancellation).  The [DecodeWorkItem.callback] will NOT be invoked for
     * cancelled requests.
     *
     * Corresponds to the implicit cancellation support in the C++ thread pool's
     * task queue (tasks are discarded when the pool is shut down).
     *
     * @param handle Handle returned by [requestDecodePriority].
     */
    fun cancelDecode(handle: DecodeHandle) {
        pending.remove(handle.id)?.cancel(false)
    }

    /**
     * Sweep the pending map and return the count of requests that have
     * completed since the last call.
     *
     * Corresponds to `LLImageDecodeThread::update(F32 max_time_ms)`, which
     * in the C++ implementation simply delegates to [getPending].
     * This version additionally prunes completed futures from [pending] so
     * that [getPending] stays accurate.
     *
     * @return Number of requests that finished (successfully or not) since
     *         the previous call to [update].
     */
    fun update(): Int {
        var completed = 0
        val iter = pending.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.value.isDone || entry.value.isCancelled) {
                iter.remove()
                completed++
            }
        }
        return completed
    }

    /**
     * Current number of requests that are queued or actively being decoded.
     *
     * Corresponds to `LLImageDecodeThread::getPending()`.
     */
    fun getPending(): Int = pending.size

    /**
     * Total number of decode tasks that have completed successfully.
     *
     * Corresponds to `LLImageDecodeThread::getTotalDecodeCount()`.
     */
    fun getTotalDecodeCount(): Int = decodeCount.get()

    /**
     * Drain the executor and wait for all pending tasks to finish.
     *
     * Corresponds to `LLImageDecodeThread::shutdown()` / `mThreadPool->close()`.
     */
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    // ---- private task execution ---------------------------------------------

    /**
     * Run a single [DecodeWorkItem] on a pool thread.
     *
     * This mirrors `ImageRequest::processRequest()` +
     * `ImageRequest::finishRequest(bool)` from llimageworker.cpp.
     *
     * Steps:
     *  1. Call [ImageFormatted.updateData] to parse the header.
     *  2. If [DecodeWorkItem.discard] ≥ 0, set [ImageFormatted.discardLevel].
     *  3. Allocate a destination [ImageRaw] and call [ImageFormatted.decode].
     *  4. Optionally decode the aux (alpha) channel.
     *  5. Invoke [DecodeWorkItem.callback] with the result.
     *  6. Remove the task from [pending] and increment [decodeCount] on success.
     */
    private fun processRequest(item: DecodeWorkItem) {
        var success = false
        try {
            // Step 1: parse header
            if (!item.image.updateData()) return

            val w = TODO_int("CODEC: read width from image")
            val h = TODO_int("CODEC: read height from image")
            val c = TODO_int("CODEC: read components from image")
            @Suppress("UNREACHABLE_CODE")
            if (w * h * c == 0) return

            // Step 2: apply discard level
            if (item.discard >= 0) {
                item.image.discardLevel = item.discard.toByte()
            }

            // Step 3: allocate raw image and decode
            // val rawImage = ImageRaw(w, h, c)
            // success = item.image.decode(rawImage, 0f) && rawImage.data.isNotEmpty()
            System.err.println("ImageWorker: processRequest not yet implemented")
        } catch (e: Exception) {
            LLImage.setLastError(e.message ?: "unknown decode error")
            success = false
        } finally {
            pending.remove(item.handle.id)
            if (success) decodeCount.incrementAndGet()
            item.callback(success)
        }
    }

    // ---- companion ----------------------------------------------------------

    companion object {
        /** Number of decode worker threads; matches the C++ ThreadPool("ImageDecode", 8). */
        const val DECODE_THREAD_COUNT: Int = 8

        /** Seconds to wait for graceful executor shutdown before forcing termination. */
        const val SHUTDOWN_TIMEOUT_SECONDS: Long = 5L
    }
}

// ---------------------------------------------------------------------------
// Convenience variant — synchronous, single-threaded worker for tests
// ---------------------------------------------------------------------------

/** Single-threaded [ImageWorker] for deterministic unit testing. */
class SyncImageWorker : ImageWorker(threaded = false)

// ---------------------------------------------------------------------------
// Placeholder — remove when ImageFormatted exposes width/height/components
// ---------------------------------------------------------------------------

/** Temporary stand-in for a missing property; replaced once the base class exposes it. */
@Suppress("NOTHING_TO_INLINE")
private inline fun TODO_int(message: String): Int = 0
