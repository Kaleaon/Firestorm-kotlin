package com.firestorm.llmessage

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataPackerAsciiBufferTest {
    @Test
    fun unpackBinaryDataRejectsShortOrInvalidHexPayloads() {
        val shortHexBuffer = DataPackerAsciiBuffer("3 ff\n".toCharArray())
        assertNull(shortHexBuffer.unpackBinaryData("blob"))

        val invalidHexBuffer = DataPackerAsciiBuffer("2 zz11\n".toCharArray())
        assertNull(invalidHexBuffer.unpackBinaryData("blob"))

        val oddHexBuffer = DataPackerAsciiBuffer("2 abc\n".toCharArray())
        assertNull(oddHexBuffer.unpackBinaryData("blob"))
    }

    @Test
    fun unpackBinaryDataFixedRejectsShortOrInvalidHexPayloads() {
        val shortHexBuffer = DataPackerAsciiBuffer("aa\n".toCharArray())
        assertNull(shortHexBuffer.unpackBinaryDataFixed(2, "blob"))

        val invalidHexBuffer = DataPackerAsciiBuffer("aa0g\n".toCharArray())
        assertNull(invalidHexBuffer.unpackBinaryDataFixed(2, "blob"))
    }

    @Test
    fun roundTripBinaryDataStillWorksForValidPayloads() {
        val backing = CharArray(256)
        val writer = DataPackerAsciiBuffer(backing)
        val input = byteArrayOf(0x01, 0x23, 0x45, 0x67)

        assertTrue(writer.packBinaryData(input, "payload"))

        val reader = DataPackerAsciiBuffer(backing)
        assertContentEquals(input, reader.unpackBinaryData("payload"))
    }

    @Test
    fun unpackBinaryDataAllowsZeroLengthPayloadWithoutHex() {
        val reader = DataPackerAsciiBuffer("0\n".toCharArray())
        val payload = reader.unpackBinaryData("empty")
        assertEquals(0, payload?.size)
    }
}
