/**
 * PacketBuffer.kt
 * Kotlin port of llpacketbuffer.h
 *
 * Holds a single UDP datagram together with its source/destination host.
 * Used by the message system to queue packets for resend, drop, or delay.
 */
package com.firestorm.llmessage

/**
 * A single UDP packet and its associated metadata.
 *
 * Mirrors the C++ `LLPacketBuffer` class.  The packet payload is stored as a
 * heap-allocated [ByteArray] (the C++ class uses a fixed-size stack array of
 * `NET_BUFFER_SIZE` bytes; here we use [MAX_PACKET_SIZE] as that constant).
 *
 * Two construction modes are provided, matching the two C++ constructors:
 *
 * 1. **Outgoing** – supply a [Host] and [ByteArray] of data to be sent.
 * 2. **Incoming** – supply raw bytes received from a socket (the receiving
 *    interface may differ from the sender, so [receivingInterface] may be set
 *    separately).
 *
 * @param host The remote host this packet is addressed to or received from.
 * @param data The raw packet payload; copied into an internal fixed-size buffer.
 */
class LLPacketBuffer(
    val host: Host,
    data: ByteArray
) {

    companion object {
        /**
         * Maximum UDP packet payload size in bytes.
         *
         * Mirrors `NET_BUFFER_SIZE` from `net.h`.  Standard SL UDP messages
         * fit within 4 096 bytes; the large-payload xfer packets stay just
         * under this limit (7 680 bytes → see [LL_XFER_LARGE_PAYLOAD] which is
         * capped to MTU in the real stack).
         */
        const val MAX_PACKET_SIZE: Int = 4096
    }

    // ── Payload ───────────────────────────────────────────────────────────────

    /**
     * Raw packet bytes, zero-padded to [MAX_PACKET_SIZE] in the internal
     * buffer.  Only the first [size] bytes are valid data.
     */
    val data: ByteArray = ByteArray(MAX_PACKET_SIZE).also { buf ->
        val copyLen = minOf(data.size, MAX_PACKET_SIZE)
        data.copyInto(buf, endIndex = copyLen)
    }

    /** Number of valid bytes in [data]. */
    val size: Int = minOf(data.size, MAX_PACKET_SIZE)

    // ── Network interface tracking ────────────────────────────────────────────

    /**
     * The local network interface on which this packet was received.
     *
     * Only meaningful for incoming packets; defaults to [Host.INVALID] for
     * outgoing ones.  Mirrors `mReceivingIF` in C++.
     */
    var receivingInterface: Host = Host.INVALID

    // =========================================================================
    // Secondary constructor: build from a raw socket receive buffer
    // =========================================================================

    companion object {
        /**
         * Construct an [LLPacketBuffer] from a raw receive buffer and a source
         * [host], optionally recording the [receivingInterface].
         *
         * This covers the `LLPacketBuffer(S32 hSocket)` C++ constructor, but
         * in the Kotlin port the actual socket read is done outside this class;
         * callers pass in the already-read bytes.
         */
        fun fromReceive(
            data: ByteArray,
            sender: Host,
            receivingInterface: Host = Host.INVALID
        ): LLPacketBuffer = LLPacketBuffer(sender, data).also {
            it.receivingInterface = receivingInterface
        }
    }

    // =========================================================================
    // Accessors (mirrors C++ inline getters)
    // =========================================================================

    fun getSize(): Int              = size
    fun getData(): ByteArray        = data
    fun getHost(): Host             = host
    fun getReceivingInterface(): Host = receivingInterface

    // =========================================================================
    // toString (for debug logging)
    // =========================================================================

    override fun toString(): String =
        "LLPacketBuffer(host=$host, size=$size)"
}
