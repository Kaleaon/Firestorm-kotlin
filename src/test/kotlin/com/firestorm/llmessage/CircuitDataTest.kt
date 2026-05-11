package com.firestorm.llmessage

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class CircuitDataTest {
    @Test
    fun nextPacketOutIdWrapAround() {
        val circuit = CircuitData(Host(0u, 0.toUShort()))
        val field = CircuitData::class.java.getDeclaredField("packetsOutId")
        field.isAccessible = true
        // UInt is represented as int in JVM
        field.set(circuit, -1) // UInt.MAX_VALUE corresponds to -1 in Int

        val result = circuit.nextPacketOutId()
        assertEquals(1u, result)
    }
}
