package com.firestorm.llmessage

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals

class LLHostTest {
    @Test
    fun testParseIpStringOverflow() {
        assertNull(Host.fromString("256.0.0.1"), "Expected null for IP with segment > 255")
        assertNull(Host.fromString("192.168.1.256"), "Expected null for IP with segment > 255")
        assertNull(Host.fromString("192.168.1.1:65536"), "Expected null for port > 65535")

        // Also add a test for valid cases to ensure we didn't break things
        val validHost = Host.fromString("192.168.1.1")
        assertEquals("192.168.1.1:0", validHost?.toString())

        val validHostWithPort = Host.fromString("192.168.1.1:8080")
        assertEquals("192.168.1.1:8080", validHostWithPort?.toString())
    }
}
