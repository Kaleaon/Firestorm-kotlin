package com.firestorm.newview

import com.firestorm.llcommon.MeshModelImporter
import com.firestorm.llcommon.MeshModelImporter.MAX_RIGGED_JOINTS
import com.firestorm.llcommon.MeshModelImporter.MAX_VERTICES_PER_FACE
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class MeshIngestionViewerCapTest {

    @Test
    fun testDAEParsingAndLODDecomposition() {
        val daeContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <COLLADA xmlns="http://www.collada.org/2005/11/COLLADASchema" version="1.4.1">
              <library_geometries>
                <geometry id="cube_geom" name="Cube">
                  <mesh>
                    <source id="positions">
                      <float_array id="positions-array" count="9">0 0 0  1 0 0  0 1 0</float_array>
                    </source>
                    <source id="normals">
                      <float_array id="normals-array" count="9">0 0 1  0 0 1  0 0 1</float_array>
                    </source>
                    <triangles count="1">
                      <input semantic="VERTEX" source="#positions" offset="0"/>
                      <input semantic="NORMAL" source="#normals" offset="1"/>
                      <p>0 0 1 1 2 2</p>
                    </triangles>
                  </mesh>
                </geometry>
              </library_geometries>
            </COLLADA>
        """.trimIndent()

        val tmpFile = File.createTempFile("test_cube", ".dae")
        tmpFile.writeText(daeContent)
        try {
            val importedModel = MeshModelImporter.parseFile(tmpFile)
            assertEquals("ImportedDAE", importedModel.name)
            val highBlocks = importedModel.lodBlocks[3]
            assertNotNull(highBlocks)
            assertTrue(highBlocks!!.isNotEmpty())

            val block = highBlocks[0]
            assertEquals(1, block.faces.size)
            val face = block.faces[0]
            assertEquals(3, face.positions.size)

            // Verify LOD fallback decomposition
            assertNotNull(importedModel.lodBlocks[2])
            assertNotNull(importedModel.lodBlocks[1])
            assertNotNull(importedModel.lodBlocks[0])
            assertEquals(1, importedModel.lodBlocks[2]!!.size)
            assertEquals(1, importedModel.lodBlocks[1]!!.size)
            assertEquals(1, importedModel.lodBlocks[0]!!.size)

            // Verify dimensions calculation
            assertTrue(importedModel.dimensions[0] >= 0.99f)
            assertTrue(importedModel.dimensions[1] >= 0.99f)
        } finally {
            tmpFile.delete()
        }
    }

    @Test
    fun testJointLimitGuardrailScrubbing() {
        val skinInfo = MeshModelImporter.MeshSkinInfoData()
        for (i in 0 until 300) {
            skinInfo.jointNames.add("joint_$i")
            skinInfo.jointNums.add(i)
        }
        assertEquals(300, skinInfo.jointNames.size)

        if (skinInfo.jointNames.size > MAX_RIGGED_JOINTS) {
            skinInfo.scrubInvalidJoints()
        }

        assertTrue(skinInfo.invalidJointsScrubbed)
        assertEquals(0, skinInfo.jointNames.size)
        assertEquals(0, skinInfo.invBindMatrix.size)
    }

    @Test
    fun testLLSDPayloadGeneration() {
        val model = MeshModelImporter.ImportedModel(name = "TestRig")
        val block = MeshModelImporter.ImportedMeshBlock(name = "RigBlock", lod = 3)
        val face = MeshModelImporter.MeshFaceData()
        face.positions.add(floatArrayOf(0f, 0f, 0f))
        block.faces.add(face)
        model.lodBlocks[3]?.add(block)

        val payload = MeshModelImporter.generateLLSDPayload(model)
        assertEquals("TestRig", payload["name"])
        assertEquals("mesh", payload["asset_type"])

        @Suppress("UNCHECKED_CAST")
        val resources = payload["asset_resources"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val meshes = resources["meshes"] as List<Map<String, Any>>
        assertEquals(1, meshes.size)
        assertTrue(meshes[0].containsKey("high_lod"))
    }

    @Test
    fun testUploadThreadFeeAndUploadExecution() {
        var feeReceived = false
        var receivedFeeVal = -1
        var uploadSuccess = false

        val observer = object : FloaterModelUploadBase("test_key") {
            override fun onModelPhysicsFeeReceived(result: Any, uploadUrl: String) {
                feeReceived = true
                @Suppress("UNCHECKED_CAST")
                val resMap = result as? Map<String, Any>
                receivedFeeVal = (resMap?.get("upload_price") as? Number)?.toInt() ?: 0
            }

            override fun onModelUploadSuccess() {
                uploadSuccess = true
            }
        }

        // Test fee calculation request
        val feeThread = LLMeshUploadThread(
            instanceList = mutableListOf(),
            sourcesList = emptyMap(),
            scale = LLVector3(1f, 1f, 1f),
            uploadTextures = false,
            uploadSkin = false,
            uploadJoints = false,
            lockScaleIfJointPosition = false,
            uploadUrl = "https://capability.secondlife.com/WholeModelFee",
            destinationFolderId = UUID.randomUUID(),
            doUpload = false,
            feeObserver = observer,
            uploadObserver = observer
        )

        feeThread.run()
        assertTrue(feeThread.finished())
        assertTrue(feeReceived)
        assertEquals(10, receivedFeeVal)

        // Test whole model upload POST request
        val uploadThread = LLMeshUploadThread(
            instanceList = mutableListOf(),
            sourcesList = emptyMap(),
            scale = LLVector3(1f, 1f, 1f),
            uploadTextures = false,
            uploadSkin = false,
            uploadJoints = false,
            lockScaleIfJointPosition = false,
            uploadUrl = "https://capability.secondlife.com/WholeModelUpload",
            destinationFolderId = UUID.randomUUID(),
            doUpload = true,
            feeObserver = observer,
            uploadObserver = observer
        )

        uploadThread.run()
        assertTrue(uploadThread.finished())
        assertTrue(uploadSuccess)
    }
}
