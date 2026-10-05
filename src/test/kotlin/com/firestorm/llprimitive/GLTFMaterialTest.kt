package com.firestorm.llprimitive

import com.firestorm.llmath.Vector2
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GLTFMaterialTest {

    @Test
    fun testTextureTransformPackingAlignment() {
        val transform = TextureTransform(
            offset = Vector2(0.5f, 0.25f),
            scale = Vector2(2.0f, 3.0f),
            rotation = 1.57f
        )

        // Packed layout MUST match C++ LLGLTFMaterial::TextureTransform::getPacked:
        // [0]: scale.x, [1]: scale.y, [2]: rotation, [3]: 0.0, [4]: offset.x, [5]: offset.y, [6]: 0.0, [7]: 0.0
        val packed = transform.getPacked()
        val expectedPacked = floatArrayOf(2.0f, 3.0f, 1.57f, 0.0f, 0.5f, 0.25f, 0.0f, 0.0f)
        assertArrayEquals(expectedPacked, packed, 0.0001f)

        // Tight packed layout MUST match C++ LLGLTFMaterial::TextureTransform::getPackedTight:
        // [0]: scale.x, [1]: scale.y, [2]: rotation, [3]: offset.x, [4]: offset.y
        val packedTight = transform.getPackedTight()
        val expectedPackedTight = floatArrayOf(2.0f, 3.0f, 1.57f, 0.5f, 0.25f)
        assertArrayEquals(expectedPackedTight, packedTight, 0.0001f)
    }
}
