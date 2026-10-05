/**
 * LLXfer.kt
 * Kotlin port of llxfer.h / llxfer.cpp
 *
 * Abstract base class representing a single file transfer (xfer) in the
 * Second Life UDP file-transfer protocol.
 */
package com.firestorm.llmessage

import com.firestorm.llcommon.*

// ── Error codes ──────────────────────────────────────────────────────────────

const val LL_XFER_LARGE_PAYLOAD: Int   = 7680
const val LL_ERR_FILE_EMPTY: Int       = -44
const val LL_ERR_FILE_NOT_FOUND: Int   = -43
const val LL_ERR_CANNOT_OPEN_FILE: Int = -42
const val LL_ERR_EOF: Int              = -39
const val LL_ERR_NOERR: Int            = 0

// ── Chunk size used when no explicit size is supplied ─────────────────────────

const val LL_XFER_CHUNK_SIZE: UInt = 1000u

// ── Transfer status ───────────────────────────────────────────────────────────

/**
 * Lifecycle state of a single xfer.
 *
 * Mirrors the `ELLXferStatus` C++ enum.
 */
enum class ELLXferStatus {
    UNINITIALIZED,
    REGISTERED,     // buffer registered and available for a request
    PENDING,        // requested but waiting for a free outgoing slot
    IN_PROGRESS,
    COMPLETE,
    ABORTED,
    NONE
}

// ── Callback type alias ───────────────────────────────────────────────────────

/** Completion callback: (result: Int) -> Unit */
typealias XferCallback = (result: Int) -> Unit

// ── Abstract base class ───────────────────────────────────────────────────────

/**
 * Abstract single xfer.
 *
 * @param chunkSize Number of bytes sent per message packet. Defaults to
 *   [LL_XFER_CHUNK_SIZE] when the supplied value is < 1.
 */
abstract class LLXfer(chunkSize: Int = LL_XFER_CHUNK_SIZE.toInt()) {

    companion object {
        const val XFER_FILE:  UInt = 1u
        const val XFER_VFILE: UInt = 2u
        const val XFER_MEM:   UInt = 3u
    }

    // ── Identity & routing ────────────────────────────────────────────────────

    var id: ULong          = 0uL
    var packetNum: Int     = -1   // pre-increment before sending packet 0
    var remoteHost: Host   = Host.INVALID
    var xferSize: Int      = 0

    // ── In-flight buffer ──────────────────────────────────────────────────────

    var buffer: ByteArray?    = null
    var bufferLength: UInt    = 0u   // valid bytes, not allocated size
    var bufferStartOffset: UInt = 0u
    var bufferContainsEOF: Boolean = false

    // ── Status ─────────────────────────────────────────────────────────────────

    var status: ELLXferStatus = ELLXferStatus.UNINITIALIZED
    var waitingForAck: Boolean = false

    // ── Callback ───────────────────────────────────────────────────────────────

    var callback: XferCallback? = null
    var callbackResult: Int     = 0

    // ── Retry tracking ────────────────────────────────────────────────────────

    var retries: Int = 0

    // ── Protected chunk size (set once from constructor) ──────────────────────

    protected val mChunkSize: Int =
        if (chunkSize < 1) LL_XFER_CHUNK_SIZE.toInt() else chunkSize

    // =========================================================================
    // Abstract interface
    // =========================================================================

    /** Sub-classes implement the resource-specific maximum buffer size. */
    abstract fun getMaxBufferSize(): Int

    /** Return a human-readable name for logging (typically the filename). */
    open fun getFileName(): String = id.toString()

    /** Return the xfer-type tag (XFER_FILE, XFER_VFILE, or XFER_MEM). */
    open fun getXferTypeTag(): UInt = 0u

    // =========================================================================
    // Concrete methods
    // =========================================================================

    /** Begin sending this xfer to [remoteHost] under [xferId]. */
    open fun startSend(xferId: ULong, host: Host): Int {
        return -1
    }

    /** Close any open file handle (sub-classes override). */
    open fun closeFileHandle() {}

    /** Re-open a previously closed file handle; returns non-zero on failure. */
    open fun reopenFileHandle(): Int = -1

    /** Start the receiving side of the download. */
    open fun startDownload(): Int = -1

    /**
     * Receive [dataSize] bytes from [data] into the internal buffer,
     * flushing to disk first if the buffer would overflow.
     */
    open fun receiveData(data: ByteArray, dataSize: Int): Int {
        if ((bufferLength.toInt() + dataSize) > getMaxBufferSize()) {
            val flushResult = flush()
            if (flushResult != 0) return flushResult
        }
        val buf = buffer ?: return -1
        data.copyInto(buf, destinationOffset = bufferLength.toInt(), endIndex = dataSize)
        bufferLength += dataSize.toUInt()
        return 0
    }

    /**
     * Flush buffered data to persistent storage.
     * Only file-backed xfers override this; returns -1 for memory xfers.
     */
    open fun flush(): Int = -1

    /**
     * Load data starting at [startPosition] into the internal buffer.
     * Returns non-zero on failure.
     */
    open fun suck(startPosition: Int): Int = -1

    /**
     * Send packet number [packetNum] to [remoteHost].
     *
     * The first packet (packet 0) prepends the total transfer size as a
     * 4-byte little-endian integer before the payload bytes, matching the
     * C++ htolememcpy(fdata_buf, &mXferSize, MVT_S32, sizeof(S32)) idiom.
     */
    open fun sendPacket(packetNum: Int) {
        // Concrete transport is provided by sub-classes / the message system.
        // This stub marks status transitions so the manager can track state.
        val isLastPacket = bufferContainsEOF &&
            ((packetNum + 1).toUInt() * mChunkSize.toUInt() >= bufferLength + bufferStartOffset)

        status = if (isLastPacket) ELLXferStatus.COMPLETE else ELLXferStatus.IN_PROGRESS
        waitingForAck = true
    }

    /** Send the next sequential packet. */
    open fun sendNextPacket() {
        retries = 0
        sendPacket(++packetNum)
    }

    /** Retransmit the last packet (timeout recovery). */
    open fun resendLastPacket() {
        retries++
        sendPacket(packetNum)
    }

    /**
     * Finalise a completed or failed download.
     *
     * Sets [status] to [ELLXferStatus.COMPLETE] and fires [callback].
     */
    open fun processEOF(): Int {
        status = ELLXferStatus.COMPLETE
        callback?.invoke(callbackResult)
        return 0
    }

    /**
     * Abort this xfer with [resultCode], marking it [ELLXferStatus.ABORTED]
     * and notifying the remote end (if the circuit is still alive).
     */
    open fun abort(resultCode: Int) {
        callbackResult = resultCode
        status = ELLXferStatus.ABORTED
        // Actual AbortXfer message is sent via the message system in real usage.
    }

    // ── Packet encoding ───────────────────────────────────────────────────────

    /**
     * Encode [packetNum] for transmission, setting the high bit when [isEof]
     * is `true` to signal the last packet in the stream.
     */
    fun encodePacketNum(packetNum: Int, isEof: Boolean): Int =
        if (isEof) packetNum or 0x80000000.toInt() else packetNum

    // ── Xfer-size setter ──────────────────────────────────────────────────────

    open fun setXferSize(dataSize: Int) {
        xferSize = dataSize
    }

    // ── toString ──────────────────────────────────────────────────────────────

    override fun toString(): String = getFileName()
}
