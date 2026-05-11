package com.firestorm.llmath

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BBoxTest {

    @Test
    fun testBBoxDefaultConstructor() {
        val bbox = BBox()
        assertTrue(bbox.isEmpty())
        assertEquals(Vector3.ZERO, bbox.getMinLocal())
        assertEquals(Vector3.ZERO, bbox.getMaxLocal())
    }

    @Test
    fun testBBoxInitialization() {
        val pos = Vector3(1f, 2f, 3f)
        val rot = Quaternion(0f, 0f, 0f, 1f)
        val min = Vector3(-1f, -1f, -1f)
        val max = Vector3(1f, 1f, 1f)
        val bbox = BBox(pos, rot, min, max)

        assertTrue(bbox.isEmpty()) // The constructor sets empty = true
        assertEquals(pos, bbox.getPositionAgent())
        assertEquals(rot, bbox.getRotation())
        assertEquals(min, bbox.getMinLocal())
        assertEquals(max, bbox.getMaxLocal())
    }

    @Test
    fun testBBoxAddPointLocal() {
        val bbox = BBox()
        val p1 = Vector3(1f, 1f, 1f)
        bbox.addPointLocal(p1)

        assertFalse(bbox.isEmpty())
        assertEquals(p1, bbox.getMinLocal())
        assertEquals(p1, bbox.getMaxLocal())

        val p2 = Vector3(-1f, -1f, -1f)
        bbox.addPointLocal(p2)

        assertEquals(Vector3(-1f, -1f, -1f), bbox.getMinLocal())
        assertEquals(Vector3(1f, 1f, 1f), bbox.getMaxLocal())

        val p3 = Vector3(2f, -2f, 0f)
        bbox.addPointLocal(p3)

        assertEquals(Vector3(-1f, -2f, -1f), bbox.getMinLocal())
        assertEquals(Vector3(2f, 1f, 1f), bbox.getMaxLocal())
    }

    @Test
    fun testBBoxExpand() {
        val pos = Vector3(0f, 0f, 0f)
        val rot = Quaternion(0f, 0f, 0f, 1f)
        val min = Vector3(-1f, -1f, -1f)
        val max = Vector3(1f, 1f, 1f)
        val bbox = BBox(pos, rot, min, max)

        bbox.expand(1f)

        assertEquals(Vector3(-2f, -2f, -2f), bbox.getMinLocal())
        assertEquals(Vector3(2f, 2f, 2f), bbox.getMaxLocal())
    }

    @Test
    fun testBBoxContainsPointLocal() {
        val pos = Vector3(0f, 0f, 0f)
        val rot = Quaternion(0f, 0f, 0f, 1f)
        val min = Vector3(-1f, -1f, -1f)
        val max = Vector3(1f, 1f, 1f)
        val bbox = BBox(pos, rot, min, max)

        assertTrue(bbox.containsPointLocal(Vector3(0f, 0f, 0f)))
        assertTrue(bbox.containsPointLocal(Vector3(1f, 1f, 1f)))
        assertTrue(bbox.containsPointLocal(Vector3(-1f, -1f, -1f)))

        assertFalse(bbox.containsPointLocal(Vector3(2f, 0f, 0f)))
        assertFalse(bbox.containsPointLocal(Vector3(0f, 2f, 0f)))
        assertFalse(bbox.containsPointLocal(Vector3(0f, 0f, 2f)))
    }

    @Test
    fun testBBoxLocalDefaultConstructor() {
        val bboxLocal = BBoxLocal()
        assertEquals(Vector3.ZERO, bboxLocal.min)
        assertEquals(Vector3.ZERO, bboxLocal.max)
    }

    @Test
    fun testBBoxLocalAddPoint() {
        // According to the logic, addPoint doesn't initialize from an "empty" state but just checks against min/max
        // which start at 0,0,0
        val bboxLocal = BBoxLocal()
        val p1 = Vector3(-1f, -1f, -1f)
        bboxLocal.addPoint(p1)

        assertEquals(Vector3(-1f, -1f, -1f), bboxLocal.min)
        assertEquals(Vector3(0f, 0f, 0f), bboxLocal.max) // because it only updates if p > max

        val p2 = Vector3(2f, 2f, 2f)
        bboxLocal.addPoint(p2)
        assertEquals(Vector3(-1f, -1f, -1f), bboxLocal.min)
        assertEquals(Vector3(2f, 2f, 2f), bboxLocal.max)
    }

    @Test
    fun testBBoxLocalExpand() {
        val bboxLocal = BBoxLocal(Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f))

        bboxLocal.expand(1.5f)

        assertEquals(Vector3(-2.5f, -2.5f, -2.5f), bboxLocal.min)
        assertEquals(Vector3(2.5f, 2.5f, 2.5f), bboxLocal.max)
    }
}
