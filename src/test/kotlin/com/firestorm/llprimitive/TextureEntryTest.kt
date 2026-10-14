package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Color4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class TextureEntryTest {

    @Test
    fun testTextureEntryPropertiesAndDecoders() {
        val te = TextureEntry()
        val texId = LLUUID.generate()
        te.textureId = texId
        te.color = Color4(0.5f, 0.5f, 0.5f, 1.0f)
        te.fullbright = true
        te.shinyness = 2u

        assertEquals(texId, te.textureId)
        assertTrue(te.fullbright)
        assertEquals(2u.toUByte(), te.shinyness)
    }

    @Test
    fun testTextureEntryBinaryPackUnpack() {
        val te = TextureEntry()
        val texId = LLUUID.generate()
        te.textureId = texId
        te.color = Color4(0.5f, 0.5f, 0.5f, 0.8f)
        te.scaleS = 2.0f
        te.scaleT = 3.0f
        te.glow = 0.25f

        val buf = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)
        te.pack(buf)
        buf.flip()

        val unpackedTe = TextureEntry.unpack(buf)

        assertEquals(texId, unpackedTe.textureId)
        assertEquals(0.5f, unpackedTe.color.r, 0.02f)
        assertEquals(2.0f, unpackedTe.scaleS, 0.01f)
        assertEquals(3.0f, unpackedTe.scaleT, 0.01f)
        assertEquals(0.25f, unpackedTe.glow, 0.01f)
    }

    @Test
    fun testTextureEntryLLSDConversionRoundTrip() {
        val te = TextureEntry()
        val texId = LLUUID.generate()
        te.textureId = texId
        te.color = Color4(0.2f, 0.4f, 0.6f, 1.0f)

        val llsd = te.toSD()
        val restoredTe = TextureEntry.fromSD(llsd)

        assertEquals(texId, restoredTe.textureId)
        assertEquals(0.2f, restoredTe.color.r, 0.01f)
    }

    @Test
    fun testTextureList() {
        val list = TextureList()
        list.setSize(4)
        assertEquals(4, list.size)

        val id = LLUUID.generate()
        list.setAllIds(id)
        assertEquals(id, list[0].textureId)
        assertEquals(id, list[3].textureId)
    }
}
