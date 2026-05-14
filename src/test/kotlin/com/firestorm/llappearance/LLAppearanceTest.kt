package com.firestorm.llappearance

import com.firestorm.llcommon.AssetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LLWearableTypeTest {

    @Test
    fun `fromString returns correct type`() {
        assertEquals(LLWearableType.SHIRT, LLWearableType.fromString("shirt"))
        assertEquals(LLWearableType.SHAPE, LLWearableType.fromString("shape"))
        assertEquals(LLWearableType.INVALID, LLWearableType.fromString("nonexistent"))
    }

    @Test
    fun `fromInt returns correct type by layer order`() {
        assertEquals(LLWearableType.SHAPE, LLWearableType.fromInt(0))
        assertEquals(LLWearableType.SKIN,  LLWearableType.fromInt(1))
        assertEquals(LLWearableType.INVALID, LLWearableType.fromInt(999))
    }

    @Test
    fun `clothing and bodypart filters are non-empty`() {
        val clothing = LLWearableType.clothingTypes()
        val bodyparts = LLWearableType.bodypartTypes()
        assertTrue(clothing.isNotEmpty())
        assertTrue(bodyparts.isNotEmpty())
        assertTrue(clothing.all { it.assetType == AssetType.CLOTHING })
        assertTrue(bodyparts.all { it.assetType == AssetType.BODYPART })
    }
}

class LLVisualParamTest {

    private fun makeParam(id: Int = 1, min: Float = 0f, max: Float = 1f, default: Float = 0.5f) =
        LLVisualParam(VisualParamInfo(id, "test_param", 0, min, max, default))

    @Test
    fun `weight starts at default`() {
        val param = makeParam(default = 0.3f)
        assertEquals(0.3f, param.weight)
    }

    @Test
    fun `setWeight clamps to range`() {
        val param = makeParam(min = 0f, max = 1f)
        param.setWeight(1.5f)
        assertEquals(1f, param.weight)
        param.setWeight(-0.5f)
        assertEquals(0f, param.weight)
    }

    @Test
    fun `reset restores default weight`() {
        val param = makeParam(default = 0.7f)
        param.setWeight(0.1f)
        param.reset()
        assertEquals(0.7f, param.weight)
    }

    @Test
    fun `morph targets are tracked`() {
        val param = makeParam()
        param.addMorphTarget("morph_nose")
        param.addMorphTarget("morph_chin")
        assertEquals(2, param.getMorphTargets().size)
    }
}

class LLWearableTest {

    @Test
    fun `export and import round-trips param weights`() {
        val wearable = LLWearable(LLWearableType.SHAPE)
        val info = VisualParamInfo(1, "height", 0, 0f, 1f, 0.5f)
        val param = LLVisualParam(info)
        wearable.addVisualParam(param)
        wearable.setParamWeight(1, 0.75f)

        val exported = wearable.exportStream()
        assertTrue(exported.contains("parameters"))

        val wearable2 = LLWearable(LLWearableType.SHAPE)
        val param2 = LLVisualParam(info)
        wearable2.addVisualParam(param2)
        wearable2.importLegacyStream(exported)
        assertEquals(0.75f, wearable2.getParamWeight(1))
    }

    @Test
    fun `isModified tracks changes`() {
        val wearable = LLWearable(LLWearableType.SHIRT)
        val info = VisualParamInfo(5, "sleeve", 0, 0f, 1f, 0f)
        wearable.addVisualParam(LLVisualParam(info))
        assertFalse(wearable.isModified)
        wearable.setParamWeight(5, 0.5f)
        assertTrue(wearable.isModified)
        wearable.clearModified()
        assertFalse(wearable.isModified)
    }

    @Test
    fun `importLegacyStream rejects malformed data`() {
        val wearable = LLWearable(LLWearableType.SHIRT)
        wearable.addVisualParam(LLVisualParam(VisualParamInfo(1, "sleeve", 0, 0f, 1f, 0f)))

        assertFalse(
            wearable.importLegacyStream(
                """
                LLWearable version 22
                parameters 1
                1 nope
                """.trimIndent()
            )
        )

        assertFalse(
            wearable.importLegacyStream(
                """
                LLWearable version 22
                textures 1
                0 not-a-uuid
                """.trimIndent()
            )
        )
    }
}

class LLPolyMorphTargetTest {

    @Test
    fun `apply delta modifies vertices`() {
        val morph = LLPolyMorphTarget("fat")
        morph.addDelta(0, 0.1f, 0f, 0f)
        val verts = floatArrayOf(0f, 0f, 0f)
        val norms = floatArrayOf(0f, 0f, 1f)
        morph.apply(verts, norms, 1f)
        assertEquals(0.1f, verts[0], 0.001f)
    }

    @Test
    fun `reset zeroes out applied delta`() {
        val morph = LLPolyMorphTarget("thin")
        morph.addDelta(0, 0.5f, 0f, 0f)
        val verts = floatArrayOf(0f, 0f, 0f)
        val norms = floatArrayOf(0f, 0f, 1f)
        morph.apply(verts, norms, 1f)
        morph.reset(verts, norms)
        assertEquals(0f, verts[0], 0.001f)
    }
}

class LLPolyMeshTest {

    @Test
    fun `buildFromArrays stores geometry`() {
        val mesh = LLPolyMesh("body")
        val verts = floatArrayOf(0f,0f,0f, 1f,0f,0f, 0f,1f,0f)
        val norms = floatArrayOf(0f,0f,1f, 0f,0f,1f, 0f,0f,1f)
        val uvs   = floatArrayOf(0f,0f, 1f,0f, 0f,1f)
        val idx   = shortArrayOf(0, 1, 2)
        assertTrue(mesh.buildFromArrays(verts, norms, uvs, idx))
        assertEquals(3, mesh.numVertices)
        assertEquals(1, mesh.numTriangles)
    }

    @Test
    fun `applyMorph changes vertex positions`() {
        val mesh = LLPolyMesh("test")
        mesh.buildFromArrays(
            floatArrayOf(0f,0f,0f, 1f,0f,0f),
            floatArrayOf(0f,0f,1f, 0f,0f,1f),
            floatArrayOf(0f,0f, 1f,0f),
            shortArrayOf(0,1,0),
        )
        val morph = LLPolyMorphTarget("puff")
        morph.addDelta(0, 0.2f, 0f, 0f)
        mesh.addMorphTarget(morph)
        mesh.applyMorph("puff", 1f)
        assertTrue(mesh.getVertex(0).x > 0f)
    }
}

class LLAvatarAppearanceTest {

    @Test
    fun `buildSkeleton creates standard joints`() {
        val avatar = LLAvatarAppearance()
        assertTrue(avatar.buildSkeleton())
        assertNotNull(avatar.getJoint("mPelvis"))
        assertNotNull(avatar.getJoint("mHead"))
        assertTrue(avatar.getJointCount() >= 9)
    }

    @Test
    fun `wearables can be added and retrieved`() {
        val avatar = LLAvatarAppearance()
        val shirt = LLWearable(LLWearableType.SHIRT, "My Shirt")
        avatar.addWearable(LLWearableType.SHIRT, shirt)
        assertEquals(1, avatar.getWearableCount(LLWearableType.SHIRT))
        assertEquals(shirt, avatar.getWearable(LLWearableType.SHIRT, 0))
        assertNull(avatar.getWearable(LLWearableType.PANTS, 0))
    }

    @Test
    fun `visual params are registered and retrievable`() {
        val avatar = LLAvatarAppearance()
        val info = VisualParamInfo(100, "height", 0, 0f, 2f, 1f)
        val param = LLVisualParam(info)
        avatar.registerVisualParam(param)
        val retrieved = avatar.getVisualParam(100)
        assertNotNull(retrieved)
        assertEquals("height", retrieved.name)
    }
}

class LLBakedTextureTest {

    @Test
    fun `initial state is invalid and dirty`() {
        val bake = LLBakedTexture(BakedTextureIndex.HEAD)
        assertFalse(bake.isValid())
        assertTrue(bake.isLocallyDirty)
        assertTrue(bake.needsUpdate)
    }

    @Test
    fun `bake produces a result id`() {
        val bake = LLBakedTexture(BakedTextureIndex.UPPER)
        bake.addLayer("skin_layer")
        val ok = bake.bake(256, 256)
        assertTrue(ok)
        assertTrue(bake.isValid())
        assertFalse(bake.isLocallyDirty)
        assertNotNull(bake.getBakedImage())
    }

    @Test
    fun `invalidate resets state`() {
        val bake = LLBakedTexture(BakedTextureIndex.LOWER)
        bake.bake()
        assertTrue(bake.isValid())
        bake.invalidate()
        assertFalse(bake.isValid())
        assertTrue(bake.isLocallyDirty)
        @Suppress("USELESS_IS_CHECK")
        assertTrue(bake.getBakedImage() == null)
    }

    @Test
    fun `bake rejects invalid dimensions`() {
        val bake = LLBakedTexture(BakedTextureIndex.HEAD)
        assertFalse(bake.bake(0, 128))
        assertFalse(bake.isValid())
    }
}
