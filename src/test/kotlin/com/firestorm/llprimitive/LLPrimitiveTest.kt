package com.firestorm.llprimitive

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Color4
import com.firestorm.llmath.Vector3
import org.junit.Assert.*
import org.junit.Test

class LLPrimitiveTest {

    @Test
    fun testFlexibleExtraParamsRoundTrip() {
        val prim = LLPrimitive()
        val flex = FlexibleObjectData(
            simulateLOD = 3,
            gravity = 1.5f,
            airFriction = 0.8f,
            windSensitivity = 0.5f,
            tension = 2.0f,
            userForce = Vector3(1.0f, 2.0f, 3.0f)
        )
        prim.flexibleData = flex

        val packed = prim.packExtraParams()
        assertNotNull(packed)

        val unpackedPrim = LLPrimitive()
        unpackedPrim.unpackExtraParams(packed)

        val unpackedFlex = unpackedPrim.flexibleData
        assertNotNull(unpackedFlex)
        assertEquals(3, unpackedFlex!!.simulateLOD)
        assertEquals(1.5f, unpackedFlex.gravity, 0.001f)
        assertEquals(0.8f, unpackedFlex.airFriction, 0.001f)
        assertEquals(0.5f, unpackedFlex.windSensitivity, 0.001f)
        assertEquals(2.0f, unpackedFlex.tension, 0.001f)
        assertEquals(1.0f, unpackedFlex.userForce.x, 0.001f)
        assertEquals(2.0f, unpackedFlex.userForce.y, 0.001f)
        assertEquals(3.0f, unpackedFlex.userForce.z, 0.001f)
    }

    @Test
    fun testLightExtraParamsRoundTrip() {
        val prim = LLPrimitive()
        val light = LightParams(
            linearColor = Color4(0.8f, 0.6f, 0.4f, 0.9f),
            radius = 10.0f,
            falloff = 0.75f,
            cutoff = 0.2f
        )
        prim.lightParams = light

        val packed = prim.packExtraParams()
        assertNotNull(packed)

        val unpackedPrim = LLPrimitive()
        unpackedPrim.unpackExtraParams(packed)

        val unpackedLight = unpackedPrim.lightParams
        assertNotNull(unpackedLight)
        assertEquals(10.0f, unpackedLight!!.radius, 0.001f)
        assertEquals(0.75f, unpackedLight.falloff, 0.001f)
        assertEquals(0.2f, unpackedLight.cutoff, 0.001f)
    }

    @Test
    fun testSculptExtraParamsRoundTrip() {
        val prim = LLPrimitive()
        val textureId = LLUUID.generate()
        val sculpt = SculptParams(sculptTexture = textureId, sculptType = 2u)
        prim.sculptParams = sculpt

        val packed = prim.packExtraParams()
        assertNotNull(packed)

        val unpackedPrim = LLPrimitive()
        unpackedPrim.unpackExtraParams(packed)

        val unpackedSculpt = unpackedPrim.sculptParams
        assertNotNull(unpackedSculpt)
        assertEquals(textureId, unpackedSculpt!!.sculptTexture)
        assertEquals(2u.toUByte(), unpackedSculpt.sculptType)
    }

    @Test
    fun testReflectionProbeExtraParamsRoundTrip() {
        val prim = LLPrimitive()
        val probe = ReflectionProbeParams(
            ambiance = 0.5f,
            clipDistance = 25.0f,
            isBox = true,
            isDynamic = false,
            isMirror = true
        )
        prim.reflectionProbeParams = probe

        val packed = prim.packExtraParams()
        assertNotNull(packed)

        val unpackedPrim = LLPrimitive()
        unpackedPrim.unpackExtraParams(packed)

        val unpackedProbe = unpackedPrim.reflectionProbeParams
        assertNotNull(unpackedProbe)
        assertEquals(0.5f, unpackedProbe!!.ambiance, 0.001f)
        assertEquals(25.0f, unpackedProbe.clipDistance, 0.001f)
        assertTrue(unpackedProbe.isBox)
        assertFalse(unpackedProbe.isDynamic)
        assertTrue(unpackedProbe.isMirror)
    }
}
