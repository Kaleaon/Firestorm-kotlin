/**
 * VLManager.kt
 * Kotlin port of llvlmanager.h / llvlmanager.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * License: GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

/**
 * Singleton manager for viewer-layer (VL) network packets.
 *
 * The sim sends compressed terrain (land), wind, cloud, and water layers as
 * binary packets.  [VLManager] queues incoming [VLData] packets and unpacks
 * them in batches, dispatching each to the appropriate subsystem (surface
 * decompression, wind field, etc.).
 *
 * Corresponds to C++ LLVLManager / global gVLManager.
 *
 * Aurora Sim extensions (extended land, water, wind, cloud layer codes) are
 * preserved from the Firestorm fork.
 */
object VLManager {

    // -----------------------------------------------------------------------
    // Layer-type codes — mirrors C++ char constants in llvlmanager.cpp
    // -----------------------------------------------------------------------

    /** Layer codes matching the SL protocol and Aurora Sim extensions. */
    enum class LayerCode(val code: Byte) {
        LAND('L'.code.toByte()),
        WIND('7'.code.toByte()),
        CLOUD('8'.code.toByte()),
        // Firestorm/Aurora Sim extensions
        WATER('W'.code.toByte()),
        AURORA_LAND('M'.code.toByte()),
        AURORA_WATER('X'.code.toByte()),
        AURORA_WIND('9'.code.toByte()),
        AURORA_CLOUD(':'.code.toByte()),
        UNKNOWN(0);

        companion object {
            fun fromCode(code: Byte): LayerCode =
                entries.firstOrNull { it.code == code } ?: UNKNOWN
        }
    }

    // -----------------------------------------------------------------------
    // LayerSet — data class grouping a set of pending layer packets
    // -----------------------------------------------------------------------

    /**
     * Groups one or more [VLData] packets of the same semantic type.
     *
     * [type] is a human-readable label such as "land", "wind", "cloud",
     * or "water".  [layers] is a mutable list so the caller can append
     * additional packets before handing the set to [addLayerSet].
     */
    data class LayerSet(
        val type: String,
        val layers: MutableList<Any> = mutableListOf(),
    )

    // -----------------------------------------------------------------------
    // Bit-count tracking  (mirrors C++ U32Bits members)
    // -----------------------------------------------------------------------

    /** Accumulated bits received for land layer packets since last reset. */
    var landBits: UInt = 0u
        private set

    /** Accumulated bits received for wind layer packets since last reset. */
    var windBits: UInt = 0u
        private set

    /** Accumulated bits received for cloud layer packets since last reset. */
    var cloudBits: UInt = 0u
        private set

    /**
     * Accumulated bits received for water layer packets (Aurora Sim extension).
     */
    var waterBits: UInt = 0u
        private set

    // -----------------------------------------------------------------------
    // Internal packet queue
    // -----------------------------------------------------------------------

    private val packetData: MutableList<VLData> = mutableListOf()

    // -----------------------------------------------------------------------
    // LayerSet-level API  (higher-level, Kotlin-idiomatic)
    // -----------------------------------------------------------------------

    /**
     * Register a [LayerSet] with the manager.  Each item in [set.layers] is
     * expected to be a [VLData] instance and is queued for decompression.
     *
     * @param set The layer set to add.
     */
    fun addLayerSet(set: LayerSet) {
        for (item in set.layers) {
            if (item is VLData) addLayerData(item, item.size)
        }
    }

    /**
     * Remove all pending packets that belong to [set] (matched by reference).
     *
     * @param set The layer set to remove.
     */
    fun removeLayerSet(set: LayerSet) {
        val toRemove = set.layers.filterIsInstance<VLData>().toSet()
        packetData.removeAll { it in toRemove }
    }

    /**
     * Trigger decompression of all currently queued layer packets.
     * Stub — full implementation calls [unpackData].
     */
    fun updateLayerSets() {
        System.err.println("VLManager: updateLayerSets not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Packet-level API  (mirrors C++ LLVLManager member functions)
    // -----------------------------------------------------------------------

    /**
     * Enqueue one incoming layer packet and accumulate its bit count in the
     * appropriate bucket.
     *
     * @param vlData     The incoming packet.
     * @param msgSizeBytes Size of the network message in bytes.
     *
     * Corresponds to C++ addLayerData().
     */
    fun addLayerData(vlData: VLData, msgSizeBytes: Int) {
        val bits = (msgSizeBytes * 8).toUInt()
        when (LayerCode.fromCode(vlData.type)) {
            LayerCode.LAND,
            LayerCode.AURORA_LAND  -> landBits += bits
            LayerCode.WIND,
            LayerCode.AURORA_WIND  -> windBits += bits
            LayerCode.CLOUD,
            LayerCode.AURORA_CLOUD -> cloudBits += bits
            LayerCode.WATER,
            LayerCode.AURORA_WATER -> waterBits += bits
            LayerCode.UNKNOWN      -> error("Unknown layer type: ${vlData.type}")
        }
        packetData.add(vlData)
    }

    /**
     * Decompress and dispatch up to [numPackets] queued layer packets.
     *
     * Each packet is decoded with the appropriate DCT / patch decompressor
     * and forwarded to the owning region's land, wind, or cloud subsystem.
     * The GPU/network decompression is stubbed with [TODO].
     *
     * @param numPackets Maximum number of packets to process (default 10).
     *
     * Corresponds to C++ unpackData().
     */
    fun unpackData(numPackets: Int = 10) {
        val toProcess = packetData.take(numPackets).toList()
        for (packet in toProcess) {
            when (LayerCode.fromCode(packet.type)) {
                LayerCode.LAND         ->
                    System.err.println("VLManager: unpackData not yet implemented")
                LayerCode.AURORA_LAND  ->
                    System.err.println("VLManager: unpackData not yet implemented")
                LayerCode.WIND,
                LayerCode.AURORA_WIND  ->
                    System.err.println("VLManager: unpackData not yet implemented")
                LayerCode.CLOUD,
                LayerCode.AURORA_CLOUD -> { /* cloud layer — currently no-op in C++ */ }
                LayerCode.WATER,
                LayerCode.AURORA_WATER -> { /* water layer — currently no-op in C++ */ }
                LayerCode.UNKNOWN      -> error("Unknown layer type: ${packet.type}")
            }
        }
        packetData.removeAll(toProcess.toSet())
    }

    // -----------------------------------------------------------------------
    // Statistics
    // -----------------------------------------------------------------------

    /** Total bytes received across all layer types since last reset. */
    fun getTotalBytes(): Int = ((landBits + windBits + cloudBits) / 8u).toInt()

    /** Reset per-layer bit counters. Corresponds to C++ resetBitCounts(). */
    fun resetBitCounts() {
        landBits  = 0u
        windBits  = 0u
        cloudBits = 0u
        waterBits = 0u
    }

    // -----------------------------------------------------------------------
    // Region cleanup
    // -----------------------------------------------------------------------

    /**
     * Discard all queued packets that belong to [regionTag], e.g. when a
     * region is being torn down.  Corresponds to C++ cleanupData().
     *
     * @param regionTag Opaque tag matching [VLData.regionTag].
     */
    fun cleanupData(regionTag: String) {
        packetData.removeAll { it.regionTag == regionTag }
    }
}

// ---------------------------------------------------------------------------
// VLData — a single incoming viewer-layer network packet
// ---------------------------------------------------------------------------

/**
 * Holds the raw bytes of a single viewer-layer packet together with routing
 * metadata.
 *
 * Corresponds to C++ LLVLData.
 *
 * @param regionTag Identifies the region that sent this packet (replaces the
 *                  raw C++ LLViewerRegion pointer with a safe tag string).
 * @param type      One-byte layer type code (see [VLManager.LayerCode]).
 * @param data      Raw packet payload.
 * @param size      Byte length of [data].
 */
class VLData(
    val regionTag: String,
    val type: Byte,
    val data: ByteArray,
    val size: Int,
)
