package com.firestorm.llrender

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Vector2
import com.firestorm.llmath.Vector3
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VertexBufferTest {
    @Test
    fun flushCapturesCurrentCpuBuffers() {
        val buffer = VertexBuffer(
            VertexBuffer.MAP_VERTEX or VertexBuffer.MAP_NORMAL or VertexBuffer.MAP_TEXCOORD0 or VertexBuffer.MAP_COLOR,
            numVerts = 1,
            numIndices = 1
        )

        buffer.putVertex(0, Vector3(1f, 2f, 3f))
        buffer.putNormal(0, Vector3(4f, 5f, 6f))
        buffer.putTexCoord(0, 0, Vector2(0.25f, 0.75f))
        buffer.putColor(0, Color4(1f, 0.5f, 0f, 1f))
        buffer.putIndex(0, 7)

        buffer.flush()
        val snapshot = assertNotNull(buffer.getUploadedSnapshot())

        val expectedVertex = ByteArray(12).also {
            java.nio.ByteBuffer.wrap(it).order(java.nio.ByteOrder.nativeOrder())
                .putFloat(1f).putFloat(2f).putFloat(3f)
        }
        assertContentEquals(expectedVertex, snapshot.vertexBytes)
        assertEquals(buffer.getTypeMask(), snapshot.typeMask)
        assertNotNull(snapshot.normalBytes)
        assertNotNull(snapshot.texCoordBytes[0])
        assertNotNull(snapshot.colorBytes)
        assertNotNull(snapshot.indexBytes)
    }

    @Test
    fun bindFlushesLazilyAndTogglesBoundState() {
        val buffer = VertexBuffer(VertexBuffer.MAP_VERTEX, numVerts = 1, numIndices = 0)

        assertFalse(buffer.isBound())
        assertEquals(null, buffer.getUploadedSnapshot())

        buffer.putVertex(0, Vector3(9f, 8f, 7f))
        buffer.bind()

        assertTrue(buffer.isBound())
        assertNotNull(buffer.getUploadedSnapshot())

        buffer.unbind()
        assertFalse(buffer.isBound())
    }
}
