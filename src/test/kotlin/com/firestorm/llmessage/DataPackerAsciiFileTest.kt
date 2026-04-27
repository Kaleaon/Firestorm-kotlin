package com.firestorm.llmessage

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataPackerAsciiFileTest {
    @Test
    fun roundTripPackAndUnpackAcrossSupportedTypes() {
        val sink = ByteArrayOutputStream()
        val packer = DataPackerAsciiFile(sink)
        val uuid = LLUUID.generate()

        assertTrue(packer.packU8(42u, "u8"))
        assertTrue(packer.packU16(500u, "u16"))
        assertTrue(packer.packS16((-123).toShort(), "s16"))
        assertTrue(packer.packU32(123456u, "u32"))
        assertTrue(packer.packS32(-789, "s32"))
        assertTrue(packer.packF32(1.25f, "f32"))
        assertTrue(packer.packString("hello", "str"))
        assertTrue(packer.packBinaryData(byteArrayOf(0x01, 0x02, 0x03), "bin"))
        assertTrue(packer.packBinaryDataFixed(byteArrayOf(0x0A, 0x0B, 0x0C), 2, "binFixed"))
        assertTrue(packer.packColor4(0.1f, 0.2f, 0.3f, 0.4f, "color"))
        assertTrue(packer.packVector3(Vector3(9f, 8f, 7f), "vec"))
        assertTrue(packer.packUUID(uuid, "id"))

        assertEquals(42u, packer.unpackU8("u8"))
        assertEquals(500u, packer.unpackU16("u16"))
        assertEquals((-123).toShort(), packer.unpackS16("s16"))
        assertEquals(123456u, packer.unpackU32("u32"))
        assertEquals(-789, packer.unpackS32("s32"))
        assertEquals(1.25f, packer.unpackF32("f32"))
        assertEquals("hello", packer.unpackString("str"))
        assertContentEquals(byteArrayOf(0x01, 0x02, 0x03), packer.unpackBinaryData("bin"))
        assertContentEquals(byteArrayOf(0x0A, 0x0B), packer.unpackBinaryDataFixed(2, "binFixed"))
        val color = assertNotNull(packer.unpackColor4("color"))
        assertEquals(0.1f, color[0])
        assertEquals(0.2f, color[1])
        assertEquals(0.3f, color[2])
        assertEquals(0.4f, color[3])
        assertEquals(Vector3(9f, 8f, 7f), packer.unpackVector3("vec"))
        assertEquals(uuid, packer.unpackUUID("id"))
    }
}
