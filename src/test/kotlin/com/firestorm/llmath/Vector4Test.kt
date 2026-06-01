package com.firestorm.llmath

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class Vector4Test {
    @Test
    fun testConstructors() {
        val v1 = Vector4()
        assertEquals(0f, v1.x)
        assertEquals(0f, v1.y)
        assertEquals(0f, v1.z)
        assertEquals(1f, v1.w)

        val v2 = Vector4(1f, 2f, 3f, 4f)
        assertEquals(1f, v2.x)
        assertEquals(2f, v2.y)
        assertEquals(3f, v2.z)
        assertEquals(4f, v2.w)

        val v3 = Vector4(1f, 2f, 3f)
        assertEquals(1f, v3.x)
        assertEquals(2f, v3.y)
        assertEquals(3f, v3.z)
        assertEquals(1f, v3.w)

        val v4 = Vector4(Vector3(1f, 2f, 3f))
        assertEquals(1f, v4.x)
        assertEquals(2f, v4.y)
        assertEquals(3f, v4.z)
        assertEquals(1f, v4.w)

        val v5 = Vector4(Vector3(1f, 2f, 3f), 4f)
        assertEquals(1f, v5.x)
        assertEquals(2f, v5.y)
        assertEquals(3f, v5.z)
        assertEquals(4f, v5.w)
    }

    @Test
    fun testSetters() {
        val v = Vector4()

        v.set(1f, 2f, 3f, 4f)
        assertEquals(1f, v.x)
        assertEquals(2f, v.y)
        assertEquals(3f, v.z)
        assertEquals(4f, v.w)

        v.set(5f, 6f, 7f)
        assertEquals(5f, v.x)
        assertEquals(6f, v.y)
        assertEquals(7f, v.z)
        assertEquals(1f, v.w)

        v.set(Vector4(8f, 9f, 10f, 11f))
        assertEquals(8f, v.x)
        assertEquals(9f, v.y)
        assertEquals(10f, v.z)
        assertEquals(11f, v.w)

        v.set(Vector3(12f, 13f, 14f))
        assertEquals(12f, v.x)
        assertEquals(13f, v.y)
        assertEquals(14f, v.z)
        assertEquals(1f, v.w)

        v.set(Vector3(15f, 16f, 17f), 18f)
        assertEquals(15f, v.x)
        assertEquals(16f, v.y)
        assertEquals(17f, v.z)
        assertEquals(18f, v.w)
    }

    @Test
    fun testClear() {
        val v = Vector4(1f, 2f, 3f, 4f)
        v.clear()
        assertEquals(0f, v.x)
        assertEquals(0f, v.y)
        assertEquals(0f, v.z)
        assertEquals(1f, v.w)
    }
    @Test
    fun testIsFinite() {
        assertTrue(Vector4(1f, 2f, 3f, 4f).isFinite())
        assertFalse(Vector4(Float.NaN, 2f, 3f, 4f).isFinite())
        assertFalse(Vector4(1f, Float.POSITIVE_INFINITY, 3f, 4f).isFinite())
        assertFalse(Vector4(1f, 2f, Float.NEGATIVE_INFINITY, 4f).isFinite())
        assertFalse(Vector4(1f, 2f, 3f, Float.NaN).isFinite())
    }

    @Test
    fun testIsExactlyClear() {
        assertTrue(Vector4().isExactlyClear())
        assertTrue(Vector4(0f, 0f, 0f, 1f).isExactlyClear())
        assertFalse(Vector4(0.00001f, 0f, 0f, 1f).isExactlyClear())
        assertFalse(Vector4(0f, 0f, 0f, 0.99999f).isExactlyClear())
    }

    @Test
    fun testIsExactlyZero() {
        assertTrue(Vector4(0f, 0f, 0f, 0f).isExactlyZero())
        assertFalse(Vector4().isExactlyZero()) // default w is 1
        assertFalse(Vector4(0.00001f, 0f, 0f, 0f).isExactlyZero())
    }

    @Test
    fun testLength() {
        val v = Vector4(1f, 2f, 2f, 10f) // Note: w is ignored in length calculation
        assertEquals(9f, v.lengthSquared())
        assertEquals(3f, v.length())
    }

    @Test
    fun testNormalize() {
        val v = Vector4(3f, 4f, 0f, 5f) // mag of (3,4,0) is 5
        val mag = v.normalize()
        assertEquals(5f, mag)
        assertEquals(0.6f, v.x)
        assertEquals(0.8f, v.y)
        assertEquals(0f, v.z)
        assertEquals(5f, v.w) // w is unaffected

        val vZero = Vector4(0f, 0f, 0f, 1f)
        val magZero = vZero.normalize()
        assertEquals(0f, magZero)
        assertEquals(0f, vZero.x)
        assertEquals(1f, vZero.w)
    }

    @Test
    fun testNormalized() {
        val v = Vector4(0f, 3f, 4f, 10f)
        val n = v.normalized()
        assertEquals(0f, n.x)
        assertEquals(0.6f, n.y)
        assertEquals(0.8f, n.z)
        assertEquals(10f, n.w)

        // Original should be unchanged
        assertEquals(3f, v.y)
    }

    @Test
    fun testAbs() {
        val v = Vector4(-1f, 2f, -3f, -4f)
        assertTrue(v.abs())
        assertEquals(1f, v.x)
        assertEquals(2f, v.y)
        assertEquals(3f, v.z)
        assertEquals(4f, v.w)

        assertFalse(Vector4(1f, 2f, 3f, 4f).abs())
    }

    @Test
    fun testScaleVec() {
        val v = Vector4(1f, 2f, 3f, 4f)
        v.scaleVec(Vector4(2f, 3f, -1f, 0.5f))
        assertEquals(2f, v.x)
        assertEquals(6f, v.y)
        assertEquals(-3f, v.z)
        assertEquals(2f, v.w)
    }
    @Test
    fun testIndexers() {
        val v = Vector4(1f, 2f, 3f, 4f)
        assertEquals(1f, v[0])
        assertEquals(2f, v[1])
        assertEquals(3f, v[2])
        assertEquals(4f, v[3])

        assertFailsWith<IndexOutOfBoundsException> { v[-1] }
        assertFailsWith<IndexOutOfBoundsException> { v[4] }

        v[0] = 5f
        v[1] = 6f
        v[2] = 7f
        v[3] = 8f
        assertEquals(5f, v.x)
        assertEquals(6f, v.y)
        assertEquals(7f, v.z)
        assertEquals(8f, v.w)

        assertFailsWith<IndexOutOfBoundsException> { v[-1] = 0f }
        assertFailsWith<IndexOutOfBoundsException> { v[4] = 0f }
    }

    @Test
    fun testUnaryMinus() {
        val v = Vector4(1f, -2f, 3f, 4f)
        val neg = -v
        assertEquals(-1f, neg.x)
        assertEquals(2f, neg.y)
        assertEquals(-3f, neg.z)
        assertEquals(4f, neg.w) // w is unchanged!
    }

    @Test
    fun testArithmeticOperators() {
        val v1 = Vector4(1f, 2f, 3f, 4f)
        val v2 = Vector4(5f, 6f, 7f, 8f)

        val sum = v1 + v2
        assertEquals(6f, sum.x)
        assertEquals(8f, sum.y)
        assertEquals(10f, sum.z)
        assertEquals(12f, sum.w)

        val diff = v1 - v2
        assertEquals(-4f, diff.x)
        assertEquals(-4f, diff.y)
        assertEquals(-4f, diff.z)
        assertEquals(-4f, diff.w)

        val mul = v1 * 2f
        assertEquals(2f, mul.x)
        assertEquals(4f, mul.y)
        assertEquals(6f, mul.z)
        assertEquals(4f, mul.w) // w is unchanged

        val div = v1 / 2f
        assertEquals(0.5f, div.x)
        assertEquals(1f, div.y)
        assertEquals(1.5f, div.z)
        assertEquals(4f, div.w) // w is unchanged
    }

    @Test
    fun testAssignOperators() {
        var v = Vector4(1f, 2f, 3f, 4f)
        v += Vector4(5f, 6f, 7f, 8f)
        assertEquals(6f, v.x)
        assertEquals(8f, v.y)
        assertEquals(10f, v.z)
        assertEquals(4f, v.w) // w is unchanged in +=

        v = Vector4(5f, 6f, 7f, 8f)
        v -= Vector4(1f, 2f, 3f, 4f)
        assertEquals(4f, v.x)
        assertEquals(4f, v.y)
        assertEquals(4f, v.z)
        assertEquals(8f, v.w) // w is unchanged in -=

        v = Vector4(1f, 2f, 3f, 4f)
        v *= 2f
        assertEquals(2f, v.x)
        assertEquals(4f, v.y)
        assertEquals(6f, v.z)
        assertEquals(4f, v.w) // w is unchanged in *=

        v = Vector4(2f, 4f, 6f, 4f)
        v /= 2f
        assertEquals(1f, v.x)
        assertEquals(2f, v.y)
        assertEquals(3f, v.z)
        assertEquals(4f, v.w) // w is unchanged in /=
    }

    @Test
    fun testDotAndCross() {
        val v1 = Vector4(1f, 0f, 0f, 1f)
        val v2 = Vector4(0f, 1f, 0f, 1f)

        // Dot product ignores w
        assertEquals(0f, v1.dot(v2))
        assertEquals(1f, v1.dot(v1))

        val v3 = Vector4(2f, 3f, 4f, 1f)
        val v4 = Vector4(5f, 6f, 7f, 1f)
        assertEquals(10f + 18f + 28f, v3.dot(v4))

        // Cross product ignores w in calculation but keeps original w
        val cross12 = v1.cross(v2)
        assertEquals(0f, cross12.x)
        assertEquals(0f, cross12.y)
        assertEquals(1f, cross12.z)
        assertEquals(1f, cross12.w)
    }

    @Test
    fun testEqualsAndHashCode() {
        val v1 = Vector4(1f, 2f, 3f, 4f)
        val v2 = Vector4(1f, 2f, 3f, 4f)
        val v3 = Vector4(1f, 2f, 3f, 5f) // Note equals in Vector4 only checks x, y, z!
        val v4 = Vector4(1f, 2f, 0f, 4f)

        assertTrue(v1 == v2)
        assertTrue(v1 == v3) // equals only checks x,y,z
        assertFalse(v1 == v4)
        assertFalse(v1.equals(Any()))

        assertEquals(v1.hashCode(), v2.hashCode())
        // hashcode DOES include w, so v1 and v3 have different hashcodes even though equals is true
        // This is a known issue in the original code, we test its current behavior
        assertTrue(v1.hashCode() != v3.hashCode())
    }

    @Test
    fun testToString() {
        val v = Vector4(1.5f, 2.5f, 3.5f, 4.5f)
        assertEquals("(1.5, 2.5, 3.5, 4.5)", v.toString())
    }
    @Test
    fun testFloatTimesVector4() {
        val v = Vector4(1f, 2f, 3f, 4f)
        val result = 2f * v
        assertEquals(2f, result.x)
        assertEquals(4f, result.y)
        assertEquals(6f, result.z)
        assertEquals(4f, result.w) // w is unchanged
    }

    @Test
    fun testAngleBetween() {
        val v1 = Vector4(1f, 0f, 0f, 1f)
        val v2 = Vector4(0f, 1f, 0f, 1f)
        // angleBetween only uses x, y, z as per length() and dot()
        assertEquals(kotlin.math.PI.toFloat() / 2f, angleBetween(v1, v2), 0.0001f)

        val v3 = Vector4(1f, 0f, 0f, 1f)
        assertEquals(0f, angleBetween(v1, v3), 0.0001f)
    }

    @Test
    fun testDistVec() {
        val v1 = Vector4(0f, 0f, 0f, 1f)
        val v2 = Vector4(3f, 4f, 0f, 1f)

        // distVec uses length of (a - b)
        // (a-b) = (-3, -4, 0, 0)
        // length of (a-b) = sqrt((-3)^2 + (-4)^2) = 5
        assertEquals(5f, distVec(v1, v2))
        assertEquals(25f, distVecSquared(v1, v2))
    }

    @Test
    fun testLerp() {
        val v1 = Vector4(0f, 0f, 0f, 0f)
        val v2 = Vector4(10f, 20f, 30f, 40f)

        val l1 = lerp(v1, v2, 0.5f)
        assertEquals(5f, l1.x)
        assertEquals(10f, l1.y)
        assertEquals(15f, l1.z)
        assertEquals(20f, l1.w)

        val l2 = lerp(v1, v2, 0f)
        assertEquals(0f, l2.x)
        assertEquals(0f, l2.y)
        assertEquals(0f, l2.z)
        assertEquals(0f, l2.w)

        val l3 = lerp(v1, v2, 1f)
        assertEquals(10f, l3.x)
        assertEquals(20f, l3.y)
        assertEquals(30f, l3.z)
        assertEquals(40f, l3.w)
    }

    @Test
    fun testVecConversions() {
        val v4 = Vector4(1f, 2f, 3f, 4f)
        val v3 = vec4to3(v4)
        assertEquals(1f, v3.x)
        assertEquals(2f, v3.y)
        assertEquals(3f, v3.z)

        val newV4 = vec3to4(v3)
        assertEquals(1f, newV4.x)
        assertEquals(2f, newV4.y)
        assertEquals(3f, newV4.z)
        assertEquals(1f, newV4.w) // w becomes 1f
    }
}
