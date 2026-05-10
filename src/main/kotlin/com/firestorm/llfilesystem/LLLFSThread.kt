/**
 * LLLFSThread.kt
 * Async local-filesystem I/O — converted from lllfsthread.h / lllfsthread.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * LGPL v2.1
 *
 * Provides a queued, optionally-threaded mechanism for reading and writing
 * files asynchronously.  The C++ base class `LLQueuedThread` is replaced by
 * a [java.util.concurrent.ExecutorService] backed by a single worker thread,
 * preserving the FIFO ordering guarantee of the original.
 *
 * Usage mirrors the C++ API:
 *   val handle = LLLFSThread.read(filename, buffer, offset, bytes, responder)
 *   // ... later ...
 *   LLLFSThread.cleanupClass()
 */

package com.firestorm.llfilesystem

import com.firestorm.llcommon.*
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

// ─── Responder interface ──────────────────────────────────────────────────────

/**
 * Callback invoked when an async file operation completes.
 *
 * Mirrors the C++ `LLLFSThread::Responder` abstract class.
 */
fun interface LFSResponder {
    /**
     * Called on the worker thread when the operation finishes.
     * @param bytes Number of bytes transferred, or 0 on failure.
     */
    fun completed(bytes: Int)
}

// ─── Operation type ───────────────────────────────────────────────────────────

enum class LFSOperation { FILE_READ, FILE_WRITE, FILE_RENAME, FILE_REMOVE }

// ─── Request ─────────────────────────────────────────────────────────────────

/**
 * Represents a single queued file operation.
 *
 * Mirrors `LLLFSThread::Request`.
 *
 * @param handle     Unique request handle returned to the caller.
 * @param operation  Type of operation to perform.
 * @param filename   Target file path.
 * @param buffer     Data buffer: destination for reads, source for writes.
 *                   May be `null` for non-data operations.
 * @param offset     Byte offset within the file; `-1` means append (write only).
 * @param numBytes   Bytes to read or write; `-1` means read to end-of-file.
 * @param responder  Completion callback; may be `null`.
 */
data class LFSRequest(
    val handle: Long,
    val operation: LFSOperation,
    val filename: String,
    val buffer: ByteArray?,
    val offset: Int,
    val numBytes: Int,
    val responder: LFSResponder?
) {
    var bytesRead: Int = 0
        private set

    /** Execute the request and invoke the responder. */
    internal fun process() {
        val ok = when (operation) {
            LFSOperation.FILE_READ   -> doRead()
            LFSOperation.FILE_WRITE  -> doWrite()
            LFSOperation.FILE_RENAME -> doRename()
            LFSOperation.FILE_REMOVE -> doRemove()
        }
        responder?.completed(if (ok) bytesRead else 0)
    }

    // ── Operation implementations ─────────────────────────────────────────────

    private fun doRead(): Boolean {
        if (buffer == null) return false
        return try {
            RandomAccessFile(filename, "r").use { raf ->
                val startPos = if (offset < 0) raf.length() else offset.toLong()
                raf.seek(startPos)
                val toRead = if (numBytes < 0) (raf.length() - raf.filePointer).toInt() else numBytes
                val n = raf.read(buffer, 0, minOf(toRead, buffer.size))
                bytesRead = maxOf(0, n)
                bytesRead > 0
            }
        } catch (e: Exception) {
            System.err.println("LLLFSThread read failed for $filename: ${e.message}")
            false
        }
    }

    private fun doWrite(): Boolean {
        if (buffer == null) return false
        return try {
            val mode = if (offset < 0) "rw" else "rw"  // always open rw; seek/append handled below
            val file = File(filename)
            file.parentFile?.mkdirs()
            RandomAccessFile(file, mode).use { raf ->
                if (offset < 0) {
                    raf.seek(raf.length())   // append
                } else {
                    raf.seek(offset.toLong())
                }
                val toWrite = if (numBytes < 0) buffer.size else numBytes
                raf.write(buffer, 0, minOf(toWrite, buffer.size))
                bytesRead = toWrite
            }
            true
        } catch (e: Exception) {
            System.err.println("LLLFSThread write failed for $filename: ${e.message}")
            false
        }
    }

    private fun doRename(): Boolean {
        // buffer holds the new filename as UTF-8 bytes (Firestorm convention).
        if (buffer == null) return false
        return try {
            val newName = String(buffer, 0, numBytes)
            File(filename).renameTo(File(newName))
        } catch (e: Exception) {
            System.err.println("LLLFSThread rename failed: ${e.message}")
            false
        }
    }

    private fun doRemove(): Boolean =
        try { File(filename).delete() }
        catch (e: Exception) {
            System.err.println("LLLFSThread remove failed: ${e.message}")
            false
        }
}

// ─── LLLFSThread ─────────────────────────────────────────────────────────────

/**
 * Threaded local filesystem I/O queue.
 *
 * When [threaded] is `true` (the default), requests are processed on a
 * dedicated background thread, matching the C++ `LLQueuedThread` behaviour.
 * When `false`, [processRequest] runs synchronously on the calling thread.
 *
 * The class follows the C++ pattern of a single global instance ([sLocal])
 * managed by [initClass] / [cleanupClass].
 */
class LLLFSThread(private val threaded: Boolean = true) {

    private val handleCounter = AtomicLong(0)
    private val executor = if (threaded)
        Executors.newSingleThreadExecutor { r -> Thread(r, "LFS").also { it.isDaemon = true } }
    else
        null

    private val pendingFutures: MutableMap<Long, Future<*>> = mutableMapOf()
    private val pendingLock = Any()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Enqueue an asynchronous read.
     *
     * @param filename  Path to read.
     * @param buffer    Destination byte array; must be large enough for [numBytes].
     * @param offset    Start offset within the file; negative means from end-of-file.
     * @param numBytes  Number of bytes to read; negative means read all remaining.
     * @param responder Called on the worker thread when the read is done.
     * @return Opaque handle that can be used to track completion.
     */
    fun read(
        filename: String,
        buffer: ByteArray,
        offset: Int,
        numBytes: Int,
        responder: LFSResponder?
    ): Long = submit(LFSOperation.FILE_READ, filename, buffer, offset, numBytes, responder)

    /**
     * Enqueue an asynchronous write.
     *
     * @param offset    Start offset; negative means append.
     * @param numBytes  Bytes from [buffer] to write.
     */
    fun write(
        filename: String,
        buffer: ByteArray,
        offset: Int,
        numBytes: Int,
        responder: LFSResponder?
    ): Long = submit(LFSOperation.FILE_WRITE, filename, buffer, offset, numBytes, responder)

    /** Number of requests currently queued or in-progress. */
    fun getPending(): Int = synchronized(pendingLock) { pendingFutures.size }

    /** Drain all pending requests (blocks until the queue is empty). */
    fun drain() {
        val futures = synchronized(pendingLock) { pendingFutures.values.toList() }
        futures.forEach { runCatching { it.get() } }
    }

    /** Shut down the worker thread gracefully. */
    fun shutdown() {
        executor?.shutdown()
    }

    /** Set the thread into quitting state (alias for [shutdown] on the JVM). */
    fun setQuitting() = shutdown()

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun submit(
        op: LFSOperation,
        filename: String,
        buffer: ByteArray?,
        offset: Int,
        numBytes: Int,
        responder: LFSResponder?
    ): Long {
        val handle = handleCounter.incrementAndGet()
        val request = LFSRequest(handle, op, filename, buffer, offset, numBytes, responder)

        if (executor != null) {
            val future = executor.submit {
                try { request.process() }
                finally { synchronized(pendingLock) { pendingFutures.remove(handle) } }
            }
            synchronized(pendingLock) { pendingFutures[handle] = future }
        } else {
            // Non-threaded: run synchronously
            request.process()
        }

        return handle
    }

    // ── Companion (static class management) ───────────────────────────────────

    companion object {
        /** Global singleton; mirrors C++ `LLLFSThread* sLocal`. */
        @Volatile var sLocal: LLLFSThread? = null
            private set

        /**
         * Create the global instance.  Must be called once at startup.
         * @param localIsThreaded `true` to run I/O on a background thread.
         */
        fun initClass(localIsThreaded: Boolean = true) {
            check(sLocal == null) { "LLLFSThread::initClass called twice" }
            sLocal = LLLFSThread(localIsThreaded)
        }

        /**
         * Drain pending requests and destroy the global instance.
         * Must be called once at shutdown.
         */
        fun cleanupClass() {
            val local = checkNotNull(sLocal) { "LLLFSThread::cleanupClass: sLocal is null" }
            local.drain()
            local.shutdown()
            sLocal = null
        }

        /**
         * Process pending requests.  On the JVM this is a no-op for threaded
         * mode (the executor handles its own queue), but is provided for API
         * compatibility with the C++ `updateClass(ms_elapsed)` call.
         *
         * @return Number of requests that were pending at call time.
         */
        fun updateClass(msElapsed: UInt): Int = sLocal?.getPending() ?: 0
    }
}
