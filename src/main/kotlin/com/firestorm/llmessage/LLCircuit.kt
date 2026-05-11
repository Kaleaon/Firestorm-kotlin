/**
 * LLCircuit.kt
 * Kotlin port of llcircuit.h / llcircuit.cpp
 *
 * Manages the set of UDP circuit connections to Second Life simulators.
 * Each circuit tracks one remote host and its packet-level state.
 */
package com.firestorm.llmessage

// Constants ported from llcircuit.h
const val LL_AVERAGED_PING_ALPHA: Float = 0.2f
const val LL_AVERAGED_PING_MAX_MS: Float = 2000f
const val LL_AVERAGED_PING_MIN_MS: Float = 100f
const val INITIAL_PING_VALUE_MSEC: UInt = 1000u
const val LL_MAX_OUT_PACKET_ID: UInt = 0x01000000u
const val LL_ERR_CIRCUIT_GONE: Int = -23017
const val LL_ERR_TCP_TIMEOUT: Int = -23016
const val LL_PACKET_ID_SIZE: UByte = 6u
const val LL_MAX_RESENT_PACKETS_PER_FRAME: Int = 100
const val LL_MAX_ACKED_PACKETS_PER_FRAME: Int = 200
const val LL_COLLECT_ACK_TIME_MAX: Float = 2f

/**
 * LLCircuit manages the full set of circuit connections.
 *
 * Corresponds to the C++ `LLCircuit` class. The global MessageSystem owns a
 * single instance of this class.
 *
 * @param heartbeatInterval Seconds between heartbeat pings sent on each circuit.
 * @param heartbeatTimeout  Seconds without a response before a circuit is considered dead.
 */
class LLCircuit(
    val heartbeatInterval: Float = 5f,
    val heartbeatTimeout: Float = 60f
) {
    // Primary map: remote host -> circuit state.
    private val circuitData: MutableMap<Host, CircuitData> = mutableMapOf()

    // Secondary maps used by the message system to find circuits that need
    // attention without iterating the whole map every frame.
    val unackedCircuitMap: MutableMap<Host, CircuitData> = mutableMapOf()
    val sendAckMap: MutableMap<Host, CircuitData> = mutableMapOf()

    // Cache of the last circuit looked up – avoids repeated map lookups for
    // back-to-back calls with the same host (mirrors mLastCircuit in C++).
    private var lastCircuit: CircuitData? = null

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Find the circuit for [host], returning `null` if none exists.
     *
     * Uses a one-entry cache identical to the C++ implementation so that the
     * hot path (checking the same host repeatedly in one frame) is O(1).
     */
    fun findCircuit(host: Host): CircuitData? {
        lastCircuit?.let { if (it.host == host) return it }
        val cd = circuitData[host]
        lastCircuit = cd
        return cd
    }

    /** Returns true if a circuit exists for [host] and it is currently alive. */
    fun isCircuitAlive(host: Host): Boolean = findCircuit(host)?.isAlive == true

    // -------------------------------------------------------------------------
    // Manipulators
    // -------------------------------------------------------------------------

    /**
     * Create and register a new circuit for [host].
     *
     * If a circuit already exists it is returned as-is (matches C++ behaviour
     * where the existing data is returned without re-initialising).
     */
    fun addCircuitData(host: Host, initialPacketInId: UInt = 0u): CircuitData {
        return circuitData.getOrPut(host) {
            CircuitData(
                host = host,
                initialPacketInId = initialPacketInId,
                heartbeatInterval = heartbeatInterval,
                heartbeatTimeout = heartbeatTimeout
            )
        }
    }

    /**
     * Remove the circuit for [host], cleaning up all secondary maps too.
     */
    fun removeCircuitData(host: Host) {
        circuitData.remove(host)
        unackedCircuitMap.remove(host)
        sendAckMap.remove(host)
        if (lastCircuit?.host == host) lastCircuit = null
    }

    /**
     * Returns the [CircuitData] for [host], or `null` if it does not exist.
     *
     * Convenience wrapper that mirrors the field-name used in the task spec.
     */
    fun getCircuitData(host: Host): CircuitData? = findCircuit(host)

    // -------------------------------------------------------------------------
    // Ping helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if a ping should be sent on the circuit identified by
     * [host] given the supplied [ping] sequence id.
     *
     * The C++ implementation checks whether we are beyond the expected send
     * window before committing to a new ping; we stub the detail here.
     */
    fun pingReady(host: Host, ping: UByte): Boolean {
        TODO("Implement ping-window check against CircuitData.nextPingSendTimeMs")
    }

    // -------------------------------------------------------------------------
    // Per-frame processing
    // -------------------------------------------------------------------------

    /**
     * Iterate all circuits and check watchdog / heartbeat timers.
     *
     * Circuits that have timed out are marked dead (and may trigger a
     * registered timeout callback). Should be called once per frame by the
     * message system.
     */
    fun updateWatchDogTimers() {
        TODO("Drive CircuitData heartbeat/watchdog logic per circuit")
    }

    /**
     * Resend unacknowledged reliable packets on every circuit that has them.
     *
     * Fills [unackedListLength] and [unackedListSize] with aggregate counts so
     * the caller can log bandwidth pressure.
     */
    fun resendUnackedPackets(unackedListLength: IntArray, unackedListSize: IntArray) {
        TODO("Walk unackedCircuitMap and resend per-circuit unacked packets")
    }

    /**
     * Flush pending ACK packets for all circuits in [sendAckMap].
     *
     * [collectTime] is the maximum age (seconds) of an ACK before it must be
     * sent even if the batch has not reached its size limit.
     */
    fun sendAcks(collectTime: Float) {
        TODO("Send collected ACKs for each circuit in sendAckMap")
    }

    /**
     * Dump resend statistics for all circuits to the log and reset per-circuit
     * resend counters.
     */
    fun dumpResends() {
        for (cd in circuitData.values) {
            // In C++ this calls cd->dumpResendCountAndReset(); stub here.
            TODO("Call per-circuit resend count dump")
        }
    }

    // -------------------------------------------------------------------------
    // Iteration helpers
    // -------------------------------------------------------------------------

    /**
     * Provides an iterator range over circuits whose key is strictly after
     * [key] in map order – mirrors `getCircuitRange()` in C++.
     *
     * Returns a sub-sequence of the internal map entries following [key].
     */
    fun getCircuitRange(key: Host): Sequence<Map.Entry<Host, CircuitData>> {
        return circuitData.entries.asSequence().dropWhile { it.key <= key }.drop(1)
    }

    /** Read-only snapshot of all circuits, for diagnostic use. */
    fun allCircuits(): Map<Host, CircuitData> = circuitData.toMap()
}
