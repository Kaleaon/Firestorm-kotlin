package com.firestorm.llmath

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LLOctreeTest {
    @Test fun insertAndQuery() {
        val tree = LLOctreeNode<String>(Vector3(0f, 0f, 0f), 10f)
        assertTrue(tree.insert(Vector3(1f, 0f, 0f), "a"))
        val results = tree.query(Vector3(1f, 0f, 0f), 2f)
        assertTrue("a" in results)
    }
    @Test fun outsidePointNotInserted() {
        val tree = LLOctreeNode<String>(Vector3(0f, 0f, 0f), 2f)
        val inserted = tree.insert(Vector3(100f, 0f, 0f), "far")
        assertTrue(!inserted)
    }
}

class LLRayTraceTest {
    @Test fun rayHitsAABB() {
        val t = LLRayTrace.rayAABBTest(
            Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f),
            Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f)
        )
        assertNotNull(t)
        assertTrue(t!! > 0f)
    }
    @Test fun rayMissesAABB() {
        val t = LLRayTrace.rayAABBTest(
            Vector3(5f, 5f, -5f), Vector3(0f, 0f, 1f),
            Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f)
        )
        assertNull(t)
    }
    @Test fun rayHitsTriangle() {
        val hit = LLRayTrace.rayTriangleTest(
            Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f),
            Vector3(-1f, -1f, 0f), Vector3(1f, -1f, 0f), Vector3(0f, 1f, 0f)
        )
        assertNotNull(hit)
        assertEquals(5f, hit!!.t, 0.001f)
    }
    @Test fun rayMissesTriangle() {
        val hit = LLRayTrace.rayTriangleTest(
            Vector3(5f, 5f, -5f), Vector3(0f, 0f, 1f),
            Vector3(-1f, -1f, 0f), Vector3(1f, -1f, 0f), Vector3(0f, 1f, 0f)
        )
        assertNull(hit)
    }
}

class LLCalcParserTest {
    private val p = LLCalcParser(mapOf("x" to 3.0, "y" to 4.0))
    @Test fun addition() = assertEquals(5.0, p.evaluate("2 + 3"))
    @Test fun multiplication() = assertEquals(6.0, p.evaluate("2 * 3"))
    @Test fun parens() = assertEquals(10.0, p.evaluate("(2 + 3) * 2"))
    @Test fun variable() = assertEquals(3.0, p.evaluate("x"))
    @Test fun functionSqrt() = assertEquals(2.0, p.evaluate("sqrt(4)"), 0.001)
    @Test fun pi() = assertTrue(p.evaluate("pi") > 3.14)
    @Test fun power() = assertEquals(8.0, p.evaluate("2^3"), 0.001)
    @Test fun unaryMinus() = assertEquals(-5.0, p.evaluate("-5"))
}
