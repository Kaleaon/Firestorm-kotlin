package com.firestorm.llcommon

import kotlin.test.Test
import kotlin.test.assertEquals

class LLCrc32Test {

    @Test
    fun testCrc32UpdateByteArray() {
        val crc = CRC32()
        crc.update("123456789".toByteArray(Charsets.UTF_8))
        assertEquals(0xCBF43926u, crc.getCRC())
    }

    @Test
    fun testCrc32UpdateByte() {
        val crc = CRC32()
        val data = "123456789".toByteArray(Charsets.UTF_8)
        for (b in data) {
            crc.update(b.toUByte())
        }
        assertEquals(0xCBF43926u, crc.getCRC())
    }

    @Test
    fun testCrc32UpdateByteArrayWithOffsetAndLength() {
        val crc = CRC32()
        val data = "0012345678900".toByteArray(Charsets.UTF_8)
        crc.update(data, 2, 9)
        assertEquals(0xCBF43926u, crc.getCRC())
    }

    @Test
    fun testCrc32Reset() {
        val crc = CRC32()
        crc.update("123456789".toByteArray(Charsets.UTF_8))
        assertEquals(0xCBF43926u, crc.getCRC())
        crc.reset()
        assertEquals(0u, crc.getCRC())
        crc.update("hello world".toByteArray(Charsets.UTF_8))
        assertEquals(0x0D4A1185u, crc.getCRC())
    }
}
