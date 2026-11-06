package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID
import org.junit.Assert.*
import org.junit.Test

class LLMaterialTest {

    @Test
    fun testLLMaterialPropertiesAndLLSD() {
        val mat = LLMaterial()
        val normalId = LLUUID.generate()
        val specId = LLUUID.generate()

        mat.normalId = normalId
        mat.specularId = specId
        mat.specularLightColor = Color4U(255u, 200u, 150u, 255u)
        mat.specularLightExponent = 128u
        mat.environmentIntensity = 64u

        val llsd = mat.asLLSD()
        assertNotNull(llsd)

        val restoredMat = LLMaterial.fromLLSD(llsd)

        assertEquals(normalId, restoredMat.normalId)
        assertEquals(specId, restoredMat.specularId)
        assertEquals(128u.toUByte(), restoredMat.specularLightExponent)
        assertEquals(64u.toUByte(), restoredMat.environmentIntensity)
        assertEquals(255u.toUByte(), restoredMat.specularLightColor.r)
    }

    @Test
    fun testLLMaterialShaderMask() {
        val mat = LLMaterial()
        mat.normalId = LLUUID.generate()
        val mask = mat.getShaderMask(0u, false)
        assertTrue((mask.toInt() and 0x8) != 0)
    }
}
