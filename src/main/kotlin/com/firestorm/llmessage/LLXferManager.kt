/**
 * LLXferManager.kt
 * Kotlin port of llxfermanager.h / llxfermanager.cpp
 *
 * Manages all in-flight file transfers (sends and receives) for a Second Life
 * viewer session. Corresponds to the C++ `LLXferManager` class and its
 * global `gXferManager` singleton.
 */
package com.firestorm.llmessage

import com.firestorm.llcommon.*
import java.util.TreeMap

// ── Tuning constants (mirrors C++ constants) ──────────────────────────────────

const val LL_XFER_REGISTRATION_TIMEOUT: Float = 60.0f   // seconds
const val LL_PACKET_TIMEOUT: Float             = 3.0f    // seconds
const val LL_PACKET_RETRY_LIMIT: Int           = 10

const val LL_DEFAULT_MAX_SIMULTANEOUS_XFERS:       Int = 10
const val LL_DEFAULT_MAX_REQUEST_FIFO_XFERS:       Int = 1000
const val LL_DEFAULT_MAX_HARD_LIMIT_SIMULTANEOUS_XFERS: Int = 500

// ── Companion data structures ─────────────────────────────────────────────────

/**
 * Tracks how many active and pending outgoing transfers exist for one remote
 * host. Mirrors the C++ `LLHostStatus` class.
 */
data class HostStatus(
    val host: Host,
    var numActive: Int  = 0,
    var numPending: Int = 0
)

/**
 * Pending ACK record used when ack-throttling is enabled.
 * Mirrors the C++ `LLXferAckInfo` class.
 */
data class XferAckInfo(
    val id: ULong          = 0uL,
    val packetNum: Int     = -1,
    val remoteHost: Host   = Host.INVALID
)

// ── Priority sentinel ─────────────────────────────────────────────────────────

/** Used with [LLXferManager.requestFile] to specify scheduling priority. */
object XferPriority {
    const val LOW  = false
    const val HIGH = true
}

// ── Manager ───────────────────────────────────────────────────────────────────

/**
 * Singleton manager for all xfer send and receive operations.
 *
 * The C++ code uses a raw global pointer (`gXferManager`); here we expose an
 * `object` so callers access the single instance as `LLXferManager`.
 */
object LLXferManager {

    // ── Limits ────────────────────────────────────────────────────────────────

    var maxOutgoingXfersPerCircuit: Int      = LL_DEFAULT_MAX_SIMULTANEOUS_XFERS
    var hardLimitOutgoingXfersPerCircuit: Int = LL_DEFAULT_MAX_HARD_LIMIT_SIMULTANEOUS_XFERS
    var maxIncomingXfers: Int                = LL_DEFAULT_MAX_REQUEST_FIFO_XFERS

    // ── Ack throttle ──────────────────────────────────────────────────────────

    var useAckThrottling: Boolean           = false
    private val xferAckQueue: ArrayDeque<XferAckInfo> = ArrayDeque()
    /** Current ack-throttle rate in bits-per-second. */
    var ackThrottleBps: Float               = 100_000f

    // ── Transfer lists (FIFO deques, front = highest priority) ────────────────

    /** Outgoing transfers: ready to send or currently sending. */
    val sendList: ArrayDeque<LLXfer>    = ArrayDeque()

    /** Incoming transfers: waiting to receive data or currently receiving. */
    val receiveList: ArrayDeque<LLXfer> = ArrayDeque()

    // ── Per-host outgoing status ───────────────────────────────────────────────

    val outgoingHosts: MutableList<HostStatus> = mutableListOf()

    // ── Authorization sets (single-use whitelist) ─────────────────────────────

    private val expectedTransfers: MutableList<String> = mutableListOf()
    private val expectedRequests:  MutableList<String> = mutableListOf()

    // ── ID counter ────────────────────────────────────────────────────────────

    private var nextXferId: ULong = 1uL

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /** Reset to defaults; called at startup and by [cleanup]. */
    fun init() {
        cleanup()
        maxOutgoingXfersPerCircuit       = LL_DEFAULT_MAX_SIMULTANEOUS_XFERS
        hardLimitOutgoingXfersPerCircuit = LL_DEFAULT_MAX_HARD_LIMIT_SIMULTANEOUS_XFERS
        maxIncomingXfers                 = LL_DEFAULT_MAX_REQUEST_FIFO_XFERS
        useAckThrottling                 = false
        ackThrottleBps                   = 100_000f
    }

    /** Discard all in-flight transfers and host-status state. */
    fun cleanup() {
        outgoingHosts.clear()
        sendList.clear()
        receiveList.clear()
        xferAckQueue.clear()
        expectedTransfers.clear()
        expectedRequests.clear()
        nextXferId = 1uL
    }

    // =========================================================================
    // ID generation
    // =========================================================================

    /** Returns a new unique transfer ID. Thread-safe via @Synchronized. */
    @Synchronized
    fun getNextId(): ULong = nextXferId++

    // =========================================================================
    // Packet-number encoding / decoding
    // =========================================================================

    /**
     * Encode [packetNum], setting the MSB when [isEof] is `true` so the
     * receiver knows this is the last packet in the stream.
     */
    fun encodePacketNum(packetNum: Int, isEof: Boolean): Int =
        if (isEof) packetNum or 0x80000000.toInt() else packetNum

    /**
     * Strip the MSB EOF flag, returning the real 28-bit packet sequence number.
     */
    fun decodePacketNum(packetNum: Int): Int = packetNum and 0x0FFFFFFF

    /** Returns `true` when [packetNum] has the EOF flag set. */
    fun isLastPacket(packetNum: Int): Boolean = (packetNum and 0x80000000.toInt()) != 0

    // =========================================================================
    // List management
    // =========================================================================

    /** Find an xfer by [id] in [xferList]; returns `null` if not found. */
    fun findXferById(id: ULong, xferList: ArrayDeque<LLXfer>): LLXfer? =
        xferList.firstOrNull { it.id == id }

    /**
     * Remove [xfer] from [xferList] and release its resources.
     * Invalidates any iterator over the list.
     */
    fun removeXfer(xfer: LLXfer, xferList: ArrayDeque<LLXfer>) {
        xferList.remove(xfer)
    }

    /**
     * Add [xfer] to [xferList].
     * High-priority transfers go to the back (pulled first from the back in the
     * C++ FIFO convention); low-priority go to the front.
     */
    fun addToList(xfer: LLXfer, xferList: ArrayDeque<LLXfer>, isPriority: Boolean) {
        if (isPriority) xferList.addLast(xfer) else xferList.addFirst(xfer)
    }

    // =========================================================================
    // Host-status helpers
    // =========================================================================

    /** Find the [HostStatus] record for [host], or `null` if not tracked. */
    fun findHostStatus(host: Host): HostStatus? =
        outgoingHosts.firstOrNull { it.host == host }

    /** Count pending (not yet started) outgoing transfers to [host]. */
    fun numPendingXfers(host: Host): Int = findHostStatus(host)?.numPending ?: 0

    /** Count active (in-progress) outgoing transfers to [host]. */
    fun numActiveXfers(host: Host): Int = findHostStatus(host)?.numActive ?: 0

    /** Adjust the active-transfer count for [host] by [delta]. */
    fun changeNumActiveXfers(host: Host, delta: Int) {
        findHostStatus(host)?.let { it.numActive += delta }
    }

    /**
     * Rebuild [outgoingHosts] from scratch by scanning [sendList].
     * Called every retransmit frame to keep counts accurate.
     */
    fun updateHostStatus() {
        outgoingHosts.clear()

        for (xfer in sendList) {
            var hs = findHostStatus(xfer.remoteHost)
            if (hs == null) {
                hs = HostStatus(xfer.remoteHost)
                outgoingHosts.add(hs)
            }
            when (xfer.status) {
                ELLXferStatus.PENDING     -> hs.numPending++
                ELLXferStatus.IN_PROGRESS -> hs.numActive++
                else -> {}
            }
        }
    }

    /** Log per-host active/pending counts (debug aid). */
    fun printHostStatus() {
        if (outgoingHosts.isNotEmpty()) {
            println("Outgoing Xfers:")
            for (hs in outgoingHosts) {
                println("    ${hs.host}  active: ${hs.numActive}  pending: ${hs.numPending}")
            }
        }
    }

    // =========================================================================
    // File request (download to local file)
    // =========================================================================

    /**
     * Request that [remoteFilename] (at [remotePath]) be downloaded from
     * [host] and saved to [localFilename].
     *
     * Mirrors `LLXferManager::requestFile` in C++. The returned [ULong] is the
     * assigned xfer ID (0 on failure).
     *
     * @param localFilename         Destination path on this machine.
     * @param remoteFilename        Source filename on the remote host.
     * @param remotePath            ELLPath enum ordinal (0 = LL_PATH_NONE).
     * @param host                  Remote host to fetch from.
     * @param deleteRemoteOnCompletion If true, the remote end deletes its copy.
     * @param callback              Invoked with the result code when done.
     * @param isPriority            HIGH_PRIORITY jumps the queue.
     * @param useBigPackets         Use [LL_XFER_LARGE_PAYLOAD] instead of the
     *                              default chunk size.
     */
    fun requestFile(
        localFilename: String,
        remoteFilename: String,
        remotePath: Int,
        host: Host,
        deleteRemoteOnCompletion: Boolean,
        callback: XferCallback,
        isPriority: Boolean   = XferPriority.LOW,
        useBigPackets: Boolean = false
    ): ULong {
        // Return existing ID if an identical request is already in the list.
        for (xfer in receiveList) {
            if (xfer.getXferTypeTag() == LLXfer.XFER_FILE
                && xfer.remoteHost == host
                && xfer.callback === callback
            ) {
                return xfer.id
            }
        }

        val chunkSize = if (useBigPackets) LL_XFER_LARGE_PAYLOAD else -1
        val xferId = getNextId()

        // A concrete LLXfer_File sub-class would be instantiated here; for
        // this port we record the intent as a minimal stub so the manager
        // can track the transfer.
        val xfer = object : LLXfer(chunkSize) {
            override fun getMaxBufferSize(): Int = xferSize
            override fun getFileName(): String   = localFilename
            override fun getXferTypeTag(): UInt  = XFER_FILE
        }.also {
            it.id         = xferId
            it.remoteHost = host
            it.status     = ELLXferStatus.PENDING
            it.callback   = callback
        }

        addToList(xfer, receiveList, isPriority)
        startPendingDownloads()
        return xferId
    }

    // =========================================================================
    // File send (upload from local file)
    // =========================================================================

    /**
     * Offer [filename] for upload to [host] with the given asset [perms].
     *
     * This is the sending counterpart to [requestFile]. In the full
     * implementation a concrete LLXfer_File is created and queued on
     * [sendList]; here we provide the skeletal stub that the message-system
     * callbacks drive.
     *
     * @param filename Path of the file to send.
     * @param host     Destination host.
     * @param perms    Asset permission bits.
     */
    fun sendFile(filename: String, host: Host, perms: Int) {
        val xferId = getNextId()
        val xfer = object : LLXfer() {
            override fun getMaxBufferSize(): Int = xferSize
            override fun getFileName(): String   = filename
            override fun getXferTypeTag(): UInt  = XFER_FILE
        }.also {
            it.id         = xferId
            it.remoteHost = host
            it.status     = ELLXferStatus.REGISTERED
        }
        sendList.addFirst(xfer)
    }

    // =========================================================================
    // Receive-data processing
    // =========================================================================

    /**
     * Handle an incoming `SendXferPacket` UDP message.
     *
     * Extracts the xfer ID and packet number, locates the matching entry in
     * [receiveList], feeds the payload to its [LLXfer.receiveData] method, and
     * fires [LLXfer.processEOF] when the last packet arrives.
     *
     * @param buffer Raw UDP payload (may include a leading 4-byte size field on
     *               packet 0).
     * @param host   Source host of the incoming datagram.
     */
    fun processReceiveData(buffer: ByteArray, host: Host) {
        if (buffer.size < 12) return   // need at least ID(8) + packetNum(4)

        // Decode wire format: first 8 bytes = U64 ID, next 4 bytes = S32 packet#
        val id = buffer.toLongAt(0).toULong()
        val packetNum = buffer.toIntAt(8)

        val xfer = findXferById(id, receiveList) ?: return

        val decodedNum = decodePacketNum(packetNum)
        if (decodedNum != xfer.packetNum) return   // out-of-order; ignore

        val payloadOffset: Int
        val payload: ByteArray

        if (xfer.packetNum == 0) {
            // First packet: bytes 12..15 = S32 xfer size, rest = data
            val xferSize = buffer.toIntAt(12)
            xfer.setXferSize(xferSize)
            payloadOffset = 16
            payload = buffer.copyOfRange(payloadOffset, buffer.size)
        } else {
            payloadOffset = 12
            payload = buffer.copyOfRange(payloadOffset, buffer.size)
        }

        val result = xfer.receiveData(payload, payload.size)
        if (result == LL_ERR_CANNOT_OPEN_FILE) {
            xfer.abort(LL_ERR_CANNOT_OPEN_FILE)
            removeXfer(xfer, receiveList)
            startPendingDownloads()
            return
        }

        xfer.packetNum++

        if (isLastPacket(packetNum)) {
            xfer.processEOF()
            removeXfer(xfer, receiveList)
            startPendingDownloads()
        }
    }

    // =========================================================================
    // Confirmation (send-side ACK received)
    // =========================================================================

    /**
     * Handle a `ConfirmXferPacket` ACK from the remote end.
     *
     * Locates the xfer in [sendList] and advances it to the next packet, or
     * removes it if the transfer is complete.
     *
     * @param id        Xfer ID from the confirmation message.
     * @param packetNum The packet sequence number being confirmed.
     */
    fun processConfirmation(id: ULong, packetNum: Int) {
        val xfer = findXferById(id, sendList) ?: return
        xfer.waitingForAck = false
        if (xfer.status == ELLXferStatus.IN_PROGRESS) {
            xfer.sendNextPacket()
        } else {
            removeXfer(xfer, sendList)
        }
    }

    // =========================================================================
    // Abort handling
    // =========================================================================

    /**
     * Abort a specific receive-side xfer by [xferId] with [resultCode].
     */
    fun abortRequestById(xferId: ULong, resultCode: Int) {
        val xfer = findXferById(xferId, receiveList) ?: return
        if (xfer.status == ELLXferStatus.IN_PROGRESS) {
            xfer.abort(resultCode)
        } else {
            xfer.callbackResult = resultCode
            xfer.processEOF()
            removeXfer(xfer, receiveList)
        }
        startPendingDownloads()
    }

    /**
     * Handle an incoming `AbortXfer` message from the remote side.
     *
     * @param id         Xfer ID the remote peer is aborting.
     * @param resultCode Error code supplied by the remote peer.
     */
    fun processAbort(id: ULong, resultCode: Int) {
        val xfer = findXferById(id, receiveList) ?: return
        xfer.callbackResult = resultCode
        xfer.processEOF()
        removeXfer(xfer, receiveList)
        startPendingDownloads()
    }

    // =========================================================================
    // Retransmit / throttle processing (called once per frame from message loop)
    // =========================================================================

    /**
     * Scan [sendList] and [receiveList] for stale transfers, resend unacked
     * packets that have timed out, and advance any PENDING sends that now have
     * a free slot.
     *
     * Mirrors `LLXferManager::retransmitUnackedPackets` in C++.
     */
    fun retransmitUnackedPackets() {
        // --- Receive side: drop transfers whose circuit has died ---------------
        val rxIter = receiveList.iterator()
        while (rxIter.hasNext()) {
            val xfer = rxIter.next()
            if (xfer.status == ELLXferStatus.IN_PROGRESS) {
                // In a real implementation we would check the circuit table here.
                // Stub: nothing to do.
            }
        }

        // Rebuild per-host counters.
        updateHostStatus()

        // --- Send side: retransmit or advance ----------------------------------
        val txIter = sendList.iterator()
        while (txIter.hasNext()) {
            val xfer = txIter.next()
            when {
                xfer.status == ELLXferStatus.ABORTED -> {
                    txIter.remove()
                }
                xfer.status == ELLXferStatus.PENDING
                    && numActiveXfers(xfer.remoteHost) < maxOutgoingXfersPerCircuit -> {
                    val err = xfer.reopenFileHandle()
                    if (err != 0) {
                        xfer.abort(LL_ERR_CANNOT_OPEN_FILE)
                        txIter.remove()
                    } else {
                        xfer.sendNextPacket()
                        changeNumActiveXfers(xfer.remoteHost, 1)
                    }
                }
                else -> {}   // in-progress or registered; handled by ACK path
            }
        }
    }

    // =========================================================================
    // Authorization helpers
    // =========================================================================

    /** Pre-authorize [filename] for a single outgoing transfer request. */
    fun expectFileForTransfer(filename: String) { expectedTransfers.add(filename) }

    /**
     * Check (and consume) an authorization for [filename].
     * Returns `true` if the file was on the whitelist.
     */
    fun validateFileForTransfer(filename: String): Boolean =
        expectedTransfers.remove(filename)

    /** Pre-authorize [filename] for a single incoming download request. */
    fun expectFileForRequest(filename: String) { expectedRequests.add(filename) }

    /**
     * Check (and consume) an authorization for [filename].
     * Returns `true` if the file was on the whitelist.
     */
    fun validateFileForRequest(filename: String): Boolean =
        expectedRequests.remove(filename)

    // =========================================================================
    // Flood check
    // =========================================================================

    /**
     * Returns `true` if [host] has so many queued transfers that it should be
     * considered flooded (80 % of the hard limit).
     */
    fun isHostFlooded(host: Host): Boolean {
        if (hardLimitOutgoingXfersPerCircuit <= 0) return false
        val hs = findHostStatus(host) ?: return false
        return (hs.numActive + hs.numPending) >= (hardLimitOutgoingXfersPerCircuit * 0.8f).toInt()
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Start as many pending downloads as the [maxIncomingXfers] limit allows.
     * Mirrors `LLXferManager::startPendingDownloads` in C++.
     */
    private fun startPendingDownloads() {
        var inProgress = receiveList.count { it.status == ELLXferStatus.IN_PROGRESS }
        val pending    = receiveList.filter   { it.status == ELLXferStatus.PENDING }

        for (xfer in pending) {
            if (inProgress >= maxIncomingXfers) break
            val result = xfer.startDownload()
            if (result != 0) {
                xfer.abort(result)
            } else {
                inProgress++
            }
        }
    }
}

// ── ByteArray helpers (little-endian wire decoding) ──────────────────────────

private fun ByteArray.toIntAt(offset: Int): Int =
    ((this[offset].toInt() and 0xFF)) or
    ((this[offset + 1].toInt() and 0xFF) shl 8) or
    ((this[offset + 2].toInt() and 0xFF) shl 16) or
    ((this[offset + 3].toInt() and 0xFF) shl 24)

private fun ByteArray.toLongAt(offset: Int): Long {
    var v = 0L
    for (i in 0 until 8) v = v or ((this[offset + i].toLong() and 0xFF) shl (i * 8))
    return v
}
