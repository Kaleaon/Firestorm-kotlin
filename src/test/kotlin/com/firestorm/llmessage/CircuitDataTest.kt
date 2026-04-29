package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircuitDataTest {

    private fun createCircuit(initialPacketInId: UInt = 0u): CircuitData {
        val host = Host(0u, 0.toUShort())
        return CircuitData(host, initialPacketInId)
    }

    @Test
    fun testInitialState() {
        val circuit = createCircuit()
        assertTrue(circuit.isAlive)
        assertFalse(circuit.isBlocked)
        assertTrue(circuit.allowTimeout)
        assertFalse(circuit.isTrusted)

        assertEquals(0L, circuit.packetsSent)
        assertEquals(0L, circuit.packetsReceived)
        assertEquals(0L, circuit.packetsLost)
        assertEquals(0L, circuit.bytesSent)
        assertEquals(0L, circuit.bytesReceived)

        assertEquals(0.toUByte(), circuit.lastPingId)
        assertEquals(1000.toUInt(), circuit.pingDelay)
        assertEquals(1000f, circuit.pingDelayAveraged)
        assertEquals(0, circuit.pingsInTransit)
    }

    @Test
    fun testSetters() {
        val circuit = createCircuit()

        circuit.setAlive(false)
        assertFalse(circuit.isAlive)

        circuit.setBlocked(true)
        assertTrue(circuit.isBlocked)

        circuit.setTrusted(true)
        assertTrue(circuit.isTrusted)

        circuit.setAllowTimeout(false)
        assertFalse(circuit.allowTimeout)
    }

    @Test
    fun testMetricsTracking() {
        val circuit = createCircuit()

        circuit.addBytesIn(100L)
        circuit.addBytesIn(50L)
        assertEquals(150L, circuit.bytesReceived)

        circuit.addBytesOut(200L)
        assertEquals(200L, circuit.bytesSent)

        circuit.addPacketsOut()
        circuit.addPacketsOut()
        assertEquals(2L, circuit.packetsSent)
    }

    @Test
    fun testNextPacketOutId() {
        val circuit = createCircuit()

        assertEquals(1u, circuit.nextPacketOutId())
        assertEquals(2u, circuit.nextPacketOutId())

        // Use reflection to fast-forward the private packetsOutId property
        val field = CircuitData::class.java.getDeclaredField("packetsOutId")
        field.isAccessible = true
        field.set(circuit, 0x00FFFFFE.toUInt())

        // Verify it wraps at 0x00FFFFFF and skips 0
        assertEquals(0x00FFFFFF.toUInt(), circuit.nextPacketOutId())
        assertEquals(1u, circuit.nextPacketOutId())
    }

    @Test
    fun testPingTracking() {
        val circuit = createCircuit()

        circuit.pingTimerStart()
        assertEquals(1, circuit.pingsInTransit)
        assertEquals(1.toUByte(), circuit.lastPingId)

        // Use reflection to simulate time passage on pingStartTimeMs
        val startField = CircuitData::class.java.getDeclaredField("pingStartTimeMs")
        startField.isAccessible = true
        val currentStart = startField.get(circuit) as Long
        startField.set(circuit, currentStart - 100L) // Simulate 100ms passed

        circuit.pingTimerStop(1.toUByte())
        assertEquals(0, circuit.pingsInTransit)

        // pingDelay should have updated from the 1000u default
        assertTrue(circuit.pingDelay < 1000u)

        assertEquals(2.toUByte(), circuit.nextPingId())
    }

    @Test
    fun testAcknowledge() {
        val circuit = createCircuit()

        val initialTime = circuit.lastPacketInTimeMs

        // Acknowledgment time update only uses currentTimeMillis without affecting
        // sequence states heavily, sleep a minimum amount.
        Thread.sleep(2)
        circuit.acknowledge(1u)

        assertEquals(1L, circuit.packetsReceived)
        assertTrue(circuit.lastPacketInTimeMs >= initialTime)
    }
}
