package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DataPackerAsciiFileTest {
    @Test
    fun `packs and unpacks scalar values`() {
        val stream = java.io.ByteArrayOutputStream()
        val packer = DataPackerAsciiFile(stream)

        packer.packU8(7u, "u8")
        packer.packU16(15u, "u16")
        packer.packS16((-3).toShort(), "s16")
        packer.packU32(99u, "u32")
        packer.packS32(-100, "s32")
        packer.packF32(1.5f, "f32")
        packer.packString("hello", "str")

        assertEquals(7u, packer.unpackU8("u8"))
        assertEquals(15u, packer.unpackU16("u16"))
        assertEquals((-3).toShort(), packer.unpackS16("s16"))
        assertEquals(99u, packer.unpackU32("u32"))
        assertEquals(-100, packer.unpackS32("s32"))
        assertEquals(1.5f, packer.unpackF32("f32"))
        assertEquals("hello", packer.unpackString("str"))
    }

    @Test
    fun `packs and unpacks structured values`() {
        val stream = java.io.ByteArrayOutputStream()
        val packer = DataPackerAsciiFile(stream)
        val uuid = LLUUID.generate()

        packer.packBinaryData(byteArrayOf(0x01, 0x02, 0x03), "binary")
        packer.packBinaryDataFixed(byteArrayOf(0x0a, 0x0b, 0x0c), 2, "fixed")
        packer.packColor4(0.1f, 0.2f, 0.3f, 0.4f, "color")
        packer.packVector3(Vector3(5f, 6f, 7f), "vec")
        packer.packUUID(uuid, "uuid")

        assertContentEquals(byteArrayOf(0x01, 0x02, 0x03), packer.unpackBinaryData("binary"))
        assertContentEquals(byteArrayOf(0x0a, 0x0b), packer.unpackBinaryDataFixed(2, "fixed"))

        val color = assertNotNull(packer.unpackColor4("color"))
        assertContentEquals(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f), color)

        assertEquals(Vector3(5f, 6f, 7f), packer.unpackVector3("vec"))
        assertEquals(uuid, packer.unpackUUID("uuid"))
    }

    @Test
    fun `does not consume unrelated fields and preserves fixed binary width`() {
        val stream = java.io.ByteArrayOutputStream()
        val packer = DataPackerAsciiFile(stream)

        packer.packString("first", "one")
        packer.packString("second", "two")
        packer.packBinaryDataFixed(byteArrayOf(0x01, 0x02), 4, "fixed")

        assertNull(packer.unpackString("unknown"))
        assertEquals("first", packer.unpackString("one"))
        assertEquals("second", packer.unpackString("two"))
        assertContentEquals(byteArrayOf(0x01, 0x02, 0x00, 0x00), packer.unpackBinaryDataFixed(4, "fixed"))
    }
}
