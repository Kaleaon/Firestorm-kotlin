/**
 * LLBuffer.kt
 * Kotlin port of llbuffer.h / llbuffer.cpp
 *
 * Provides scattered-memory byte buffers and in-order segment lists for
 * network I/O.  The design avoids unnecessary copies by keeping data in
 * fixed-size heap slabs and walking a linked segment list instead.
 */
package com.firestorm.llmessage

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// ---------------------------------------------------------------------------
// LLChannelDescriptors
// ---------------------------------------------------------------------------

/**
 * A pair of channel indices (in/out) identifying the read and write channels
 * within an [LLBufferArray].
 *
 * The C++ class used a base integer with `in = base` and `out = base + 1`.
 * Three channel slots are reserved per descriptor set (matching `E_CHANNEL_COUNT = 3`).
 */
data class LLChannelDescriptors(val baseChannel: Int = 0) {
    val `in`: Int  get() = baseChannel
    val out: Int get() = baseChannel + 1
    // err() would be baseChannel + 2 – omitted as it was commented out in C++.

    companion object {
        const val CHANNEL_COUNT = 3
    }
}

// ---------------------------------------------------------------------------
// LLSegment
// ---------------------------------------------------------------------------

/**
 * A lightweight descriptor for a contiguous region of bytes inside an
 * [LLBuffer].  Segments do not own their data; the underlying [LLBuffer]
 * manages the memory lifetime.
 *
 * Ported from `LLSegment` in llbuffer.h.
 */
data class LLSegment(
    val channel: Int,
    /** Raw offset into the owning buffer's backing array. */
    val offset: Int,
    val size: Int
) {
    fun isOnChannel(ch: Int): Boolean = channel == ch

    companion object {
        val EMPTY = LLSegment(0, 0, 0)
    }
}

// ---------------------------------------------------------------------------
// LLBuffer (abstract)
// ---------------------------------------------------------------------------

/**
 * Abstract base for buffer implementations.
 *
 * Mirrors the C++ abstract `LLBuffer` class.  Concrete implementations
 * allocate memory in their preferred way (heap, pool, etc.) and vend
 * [LLSegment] descriptors that point into that memory.
 */
abstract class LLBuffer {
    /**
     * Request a segment of up to [size] bytes on [channel].
     *
     * @return A segment of length ≤ [size], or `null` if no space is available.
     */
    abstract fun createSegment(channel: Int, size: Int): LLSegment?

    /**
     * Notify the buffer that a segment is no longer in use.
     *
     * @return `true` if the segment was successfully reclaimed.
     */
    abstract fun reclaimSegment(segment: LLSegment): Boolean

    /** Returns `true` if [segment] lies entirely within this buffer. */
    abstract fun containsSegment(segment: LLSegment): Boolean

    /** Total bytes allocated in this buffer (may include reclaimed space). */
    abstract fun capacity(): Int

    /** Raw read access to the byte at [offset] within this buffer. */
    abstract fun byteAt(offset: Int): Byte

    /** Copy [count] bytes starting at [offset] into [dest] at [destOffset]. */
    abstract fun copyTo(offset: Int, dest: ByteArray, destOffset: Int, count: Int)
}

// ---------------------------------------------------------------------------
// LLHeapBuffer
// ---------------------------------------------------------------------------

/**
 * A fixed-size buffer allocated on the JVM heap.
 *
 * Mirrors the C++ `LLHeapBuffer`.  Allocation is bump-pointer; reclaim
 * tracking is approximate (total bytes reclaimed, not individual holes),
 * which is sufficient for the network I/O use-case where buffers are
 * short-lived.
 */
class LLHeapBuffer(size: Int = DEFAULT_SIZE) : LLBuffer() {

    private val buffer: ByteArray = ByteArray(size)
    private var nextFree: Int = 0
    private var reclaimedBytes: Int = 0

    constructor(src: ByteArray) : this(src.size) {
        src.copyInto(buffer)
        nextFree = src.size
    }

    fun bytesLeft(): Int = buffer.size - nextFree

    override fun createSegment(channel: Int, size: Int): LLSegment? {
        val available = minOf(size, bytesLeft())
        if (available <= 0) return null
        val seg = LLSegment(channel, nextFree, available)
        nextFree += available
        return seg
    }

    override fun reclaimSegment(segment: LLSegment): Boolean {
        if (!containsSegment(segment)) return false
        reclaimedBytes += segment.size
        return true
    }

    override fun containsSegment(segment: LLSegment): Boolean =
        segment.offset >= 0 && segment.offset + segment.size <= buffer.size

    override fun capacity(): Int = buffer.size

    override fun byteAt(offset: Int): Byte = buffer[offset]

    override fun copyTo(offset: Int, dest: ByteArray, destOffset: Int, count: Int) {
        buffer.copyInto(dest, destOffset, offset, offset + count)
    }

    /** Write [data] into the buffer starting at [segment]'s offset. */
    fun write(segment: LLSegment, data: ByteArray, len: Int = data.size) {
        val actual = minOf(len, segment.size, data.size)
        data.copyInto(buffer, segment.offset, 0, actual)
    }

    companion object {
        const val DEFAULT_SIZE: Int = 16 * 1024  // 16 KB, a common network MTU multiple.
    }
}

// ---------------------------------------------------------------------------
// LLBufferArray
// ---------------------------------------------------------------------------

/**
 * A linked list of [LLBuffer] slabs with an ordered [LLSegment] list.
 *
 * Data appended to the array lands in one or more heap buffers; the segment
 * list records exactly which bytes on which channel live where.  Readers walk
 * the segment list rather than a contiguous array, eliminating copies.
 *
 * Mirrors the C++ `LLBufferArray`.
 */
class LLBufferArray {

    // Internal mutable lists.
    private val buffers: MutableList<LLHeapBuffer> = mutableListOf()
    private val segments: MutableList<LLSegment> = mutableListOf()

    // Channel counter — monotonically increasing; each call to nextChannel()
    // returns a fresh descriptor pair.
    private var nextBaseChannel: Int = 0

    // Optional mutex for multi-threaded access (mirrors mMutexp in C++).
    private var lock: ReentrantLock? = null

    companion object {
        const val NPOS: Int = 0xFFFFFFFF.toInt()

        /** Build a descriptor whose *input* channel is the *output* of [channels]. */
        fun makeChannelConsumer(channels: LLChannelDescriptors): LLChannelDescriptors =
            LLChannelDescriptors(channels.out)
    }

    // -------------------------------------------------------------------------
    // Channel management
    // -------------------------------------------------------------------------

    /** Allocate and return the next available channel descriptor pair. */
    fun nextChannel(): LLChannelDescriptors {
        val descriptor = LLChannelDescriptors(nextBaseChannel)
        nextBaseChannel += LLChannelDescriptors.CHANNEL_COUNT
        return descriptor
    }

    // -------------------------------------------------------------------------
    // Capacity
    // -------------------------------------------------------------------------

    /** Sum of all buffer capacities. */
    fun capacity(): Int = buffers.sumOf { it.capacity() }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Append [len] bytes from [src] on [channel] at the end of the array.
     *
     * Allocates new [LLHeapBuffer] slabs as necessary.
     *
     * @return `true` on success.
     */
    fun append(channel: Int, src: ByteArray, len: Int = src.size): Boolean =
        withOptionalLock { copyIntoBuffers(channel, src, len) }

    /**
     * Prepend [len] bytes from [src] on [channel] at the front of the array.
     *
     * @return `true` on success.
     */
    fun prepend(channel: Int, src: ByteArray, len: Int = src.size): Boolean {
        return withOptionalLock {
            val newSegs = mutableListOf<LLSegment>()
            if (!copyIntoBuffersInternal(channel, src, len, newSegs)) return@withOptionalLock false
            segments.addAll(0, newSegs)
            true
        }
    }

    // -------------------------------------------------------------------------
    // Read / count operations
    // -------------------------------------------------------------------------

    /**
     * Count bytes on [channel] after (and including the byte pointed to by)
     * [startOffset], or count from the beginning when [startOffset] is `null`.
     */
    fun countAfter(channel: Int, startOffset: Int? = null): Int {
        return withOptionalLock {
            var counting = (startOffset == null)
            var total = 0
            for (seg in segments) {
                if (!seg.isOnChannel(channel)) continue
                if (!counting) {
                    // Look for the segment that contains startOffset.
                    if (startOffset != null &&
                        startOffset >= seg.offset &&
                        startOffset < seg.offset + seg.size) {
                        total += seg.size - (startOffset - seg.offset)
                        counting = true
                    }
                } else {
                    total += seg.size
                }
            }
            total
        }
    }

    /** Count all bytes on [channel]. */
    fun count(channel: Int): Int = countAfter(channel)

    /**
     * Read bytes from [channel] starting after [startOffset] into [dest].
     *
     * @param channel  The channel to read.
     * @param startOffset  Byte offset to begin reading from, or `null` for the start.
     * @param dest     Destination array; must be at least [len] bytes long.
     * @param len      Maximum number of bytes to read.
     * @return Actual number of bytes copied into [dest].
     */
    fun readAfter(channel: Int, startOffset: Int?, dest: ByteArray, len: Int): Int {
        return withOptionalLock {
            var destPos = 0
            var remaining = minOf(len, dest.size)
            var started = (startOffset == null)
            for (seg in segments) {
                if (remaining <= 0) break
                if (!seg.isOnChannel(channel)) continue
                val buf = bufferFor(seg) ?: continue
                if (!started) {
                    if (startOffset != null &&
                        startOffset >= seg.offset &&
                        startOffset < seg.offset + seg.size) {
                        val skip = startOffset - seg.offset
                        val available = minOf(seg.size - skip, remaining)
                        buf.copyTo(seg.offset + skip, dest, destPos, available)
                        destPos += available
                        remaining -= available
                        started = true
                    }
                } else {
                    val available = minOf(seg.size, remaining)
                    buf.copyTo(seg.offset, dest, destPos, available)
                    destPos += available
                    remaining -= available
                }
            }
            destPos
        }
    }

    // -------------------------------------------------------------------------
    // Segment iteration helpers (mirrors C++ segment iterator API)
    // -------------------------------------------------------------------------

    /** Index of the first segment, or -1 if the array is empty. */
    fun beginSegmentIndex(): Int = if (segments.isEmpty()) -1 else 0

    /** One past the last segment index. */
    fun endSegmentIndex(): Int = segments.size

    /** Return the segment at [index], or `null` if out of range. */
    fun segmentAt(index: Int): LLSegment? = segments.getOrNull(index)

    /** All segments as an immutable snapshot. */
    fun allSegments(): List<LLSegment> = segments.toList()

    // -------------------------------------------------------------------------
    // Buffer modification
    // -------------------------------------------------------------------------

    /**
     * Strip all content from [source] and append it to this array.
     *
     * @return `true` on success.
     */
    fun takeContents(source: LLBufferArray): Boolean {
        return withOptionalLock {
            buffers.addAll(source.buffers)
            segments.addAll(source.segments)
            source.buffers.clear()
            source.segments.clear()
            true
        }
    }

    /**
     * Erase the segment at [index] from the segment list.
     *
     * The underlying buffer memory is *not* freed immediately (matches C++
     * behaviour where reclaim is deferred to buffer destruction).
     *
     * @return `true` on success.
     */
    fun eraseSegment(index: Int): Boolean {
        if (index < 0 || index >= segments.size) return false
        segments.removeAt(index)
        return true
    }

    // -------------------------------------------------------------------------
    // Thread-safety
    // -------------------------------------------------------------------------

    /** Enable or disable mutex protection for multi-threaded access. */
    fun setThreaded(threaded: Boolean) {
        lock = if (threaded) ReentrantLock() else null
    }

    fun getLock(): ReentrantLock? = lock

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Copy [len] bytes from [src] into one or more [LLHeapBuffer] slabs,
     * appending resulting segments to [this.segments].
     */
    private fun copyIntoBuffers(channel: Int, src: ByteArray, len: Int): Boolean {
        val newSegs = mutableListOf<LLSegment>()
        if (!copyIntoBuffersInternal(channel, src, len, newSegs)) return false
        segments.addAll(newSegs)
        return true
    }

    private fun copyIntoBuffersInternal(
        channel: Int,
        src: ByteArray,
        len: Int,
        outSegments: MutableList<LLSegment>
    ): Boolean {
        var written = 0
        while (written < len) {
            // Try to fit into the last buffer; allocate a new one if needed.
            val buf = buffers.lastOrNull()?.takeIf { it.bytesLeft() > 0 }
                ?: LLHeapBuffer(LLHeapBuffer.DEFAULT_SIZE).also { buffers.add(it) }

            val seg = buf.createSegment(channel, len - written) ?: return false
            buf.write(seg, src, src.size - written)

            // Adjust segment offset to be relative to source start for consistency.
            outSegments.add(seg)
            written += seg.size
        }
        return true
    }

    /**
     * Find the [LLHeapBuffer] that owns [segment].
     *
     * Linear scan; acceptable because the buffer list is short in practice.
     */
    private fun bufferFor(segment: LLSegment): LLHeapBuffer? =
        buffers.firstOrNull { it.containsSegment(segment) }

    private fun <T> withOptionalLock(block: () -> T): T {
        val l = lock
        return if (l != null) l.withLock(block) else block()
    }
}
