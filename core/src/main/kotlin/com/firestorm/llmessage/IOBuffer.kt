/**
 * IOBuffer.kt
 * Kotlin port of lliobuffer.h / lliobuffer.cpp
 *
 * An automatically-resizing I/O buffer that implements the [LLIOPipe]
 * interface.  It holds a raw byte array with separate read and write
 * positions, and exposes that array to the pipeline via [LLBufferArray].
 *
 * Corresponds to the C++ `LLIOBuffer : public LLIOPipe`.
 */
package com.firestorm.llmessage

import com.firestorm.llcommon.LLSD

/**
 * Seek direction for [LLIOBuffer.seek].
 *
 * Mirrors the C++ `LLIOBuffer::EHead` enum.
 */
enum class EHead {
    /** The read position (where data is consumed by the next pipe). */
    READ,
    /** The write position (where incoming data is stored). */
    WRITE
}

/**
 * A resizing byte buffer that participates in the [LLIOPipe] processing chain.
 *
 * The buffer maintains two logical pointers into a `ByteArray`:
 *  - **write head**: appended data lands here; advances on each write.
 *  - **read head**: processing starts here; advances as data is consumed.
 *
 * [processImpl] copies all unread data from this buffer onto the *out* channel
 * of the supplied [LLBufferArray], which is then available to the next pipe
 * in the chain.
 */
class LLIOBuffer : LLIOPipe() {

    private var buffer: ByteArray = ByteArray(DEFAULT_CAPACITY)
    private var readHead: Int = 0   // index of first unconsumed byte
    private var writeHead: Int = 0  // index of first free byte

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Raw pointer to the beginning of the backing array.
     *
     * Callers may read up to [size] bytes from this array.
     * Returns a *copy* of the backing array — Kotlin does not have raw
     * pointer semantics, so callers that need direct access should use
     * [readBytes] instead.
     */
    fun data(): ByteArray = buffer.copyOf(writeHead)

    /** Total number of bytes that have been written (read + unread). */
    fun size(): Long = writeHead.toLong()

    /**
     * Slice of the backing array starting at the current read position.
     *
     * Mirrors C++ `current()` which returned `mReadHead`.
     */
    fun current(): ByteArray = buffer.copyOfRange(readHead, writeHead)

    /** Number of bytes available to read (write head minus read head). */
    fun bytesLeft(): Long = (writeHead - readHead).toLong()

    /**
     * Move the buffer heads back to zero without releasing memory.
     *
     * Equivalent to `mReadHead = mBuffer; mWriteHead = mBuffer` in C++.
     */
    fun clear() {
        readHead = 0
        writeHead = 0
    }

    /**
     * Seek [head] by [delta] bytes relative to its current position.
     *
     * A positive [delta] advances forward; negative moves backward.
     *
     * @return [EStatus.STATUS_OK] on success, [EStatus.STATUS_ERROR] if the
     *         resulting position would be out of range.
     */
    fun seek(head: EHead, delta: Long): EStatus {
        return when (head) {
            EHead.READ -> {
                val newPos = readHead + delta.toInt()
                if (newPos < 0 || newPos > writeHead) EStatus.STATUS_ERROR
                else { readHead = newPos; EStatus.STATUS_OK }
            }
            EHead.WRITE -> {
                val newPos = writeHead + delta.toInt()
                if (newPos < readHead || newPos > buffer.size) EStatus.STATUS_ERROR
                else { writeHead = newPos; EStatus.STATUS_OK }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Write helpers (not in the C++ interface, but useful in Kotlin)
    // -------------------------------------------------------------------------

    /**
     * Append [data] to this buffer, growing the backing array if necessary.
     */
    fun write(data: ByteArray) {
        ensureCapacity(writeHead + data.size)
        data.copyInto(buffer, writeHead)
        writeHead += data.size
    }

    /**
     * Read up to [maxBytes] bytes from the current read position, advancing
     * the read head.
     *
     * @return The bytes that were read (may be shorter than [maxBytes]).
     */
    fun readBytes(maxBytes: Int): ByteArray {
        val available = minOf(maxBytes, (writeHead - readHead))
        if (available <= 0) return ByteArray(0)
        val result = buffer.copyOfRange(readHead, readHead + available)
        readHead += available
        return result
    }

    // -------------------------------------------------------------------------
    // LLIOPipe implementation
    // -------------------------------------------------------------------------

    /**
     * Copy all unread bytes from this buffer onto the *out* channel of [buffer].
     *
     * This makes buffered data visible to the next pipe in the pipeline chain.
     * After a successful process call the data remains in this buffer (it is
     * not cleared), so callers should [clear] or [seek] the read head as
     * appropriate.
     *
     * @param channels  Channel descriptor for the pipeline stage.
     * @param buffer    Shared buffer array for the pipeline.
     * @param eos       End-of-stream flag.
     * @param context   Shared metadata map.
     * @param pump      The owning pump (unused in this implementation).
     * @return [EStatus.STATUS_OK] on success, [EStatus.STATUS_ERROR] on failure.
     */
    override fun processImpl(
        channels: LLChannelDescriptors,
        buffer: LLBufferArray,
        eos: Boolean,
        context: LLSD,
        pump: Any?
    ): EStatus {
        val unread = (writeHead - readHead)
        if (unread <= 0) return EStatus.STATUS_OK

        val slice = this.buffer.copyOfRange(readHead, writeHead)
        return if (buffer.append(channels.out, slice, unread)) {
            EStatus.STATUS_OK
        } else {
            EStatus.STATUS_ERROR
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun ensureCapacity(required: Int) {
        if (required <= buffer.size) return
        var newSize = buffer.size
        while (newSize < required) newSize *= 2
        buffer = buffer.copyOf(newSize)
    }

    companion object {
        /** Initial backing-array capacity (matches a typical network read size). */
        const val DEFAULT_CAPACITY: Int = 4096
    }
}
