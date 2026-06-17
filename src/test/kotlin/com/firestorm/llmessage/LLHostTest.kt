package com.firestorm.llmessage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class LLHostTest {

    @Test
    fun `fromString parses valid IP`() {
        val host = Host.fromString("192.168.1.1")
        assertNotNull(host)
        // 192=C0, 168=A8, 1=01, 1=01 -> C0A80101
        assertEquals(3232235777u, host.address)
        assertEquals(0.toUShort(), host.port)
    }

    @Test
    fun `fromString parses valid IP with port`() {
        val host = Host.fromString("192.168.1.1:8080")
        assertNotNull(host)
        assertEquals(3232235777u, host.address)
        assertEquals(8080.toUShort(), host.port)
    }

    @Test
    fun `fromString returns null for short IP`() {
        assertNull(Host.fromString("192.168.1"))
    }

    @Test
    fun `fromString returns null for long IP`() {
        assertNull(Host.fromString("192.168.1.1.1"))
    }

    @Test
    fun `fromString returns null for invalid characters`() {
        assertNull(Host.fromString("192.168.1.a"))
    }

    @Test
    fun `fromString returns null for out of range byte`() {
        assertNull(Host.fromString("192.168.1.256"))
    }
}
