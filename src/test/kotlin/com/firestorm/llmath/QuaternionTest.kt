package com.firestorm.llmath

import kotlin.test.*
import kotlin.math.*

class QuaternionTest {

    private val EPSILON = 0.0001f

    private fun assertQuaternionEps(expected: Quaternion, actual: Quaternion, message: String? = null) {
        assertTrue(expected.isEqualEps(actual, EPSILON), "$message - Expected $expected but was $actual")
    }

    private fun assertVector3Eps(expected: Vector3, actual: Vector3, message: String? = null) {
        assertTrue(abs(expected.x - actual.x) < EPSILON &&
                   abs(expected.y - actual.y) < EPSILON &&
                   abs(expected.z - actual.z) < EPSILON,
                   "$message - Expected { ${expected.x}, ${expected.y}, ${expected.z} } but was { ${actual.x}, ${actual.y}, ${actual.z} }")
    }

    private fun assertVector4Eps(expected: Vector4, actual: Vector4, message: String? = null) {
        assertTrue(abs(expected.x - actual.x) < EPSILON &&
                   abs(expected.y - actual.y) < EPSILON &&
                   abs(expected.z - actual.z) < EPSILON &&
                   abs(expected.w - actual.w) < EPSILON,
                   "$message - Expected { ${expected.x}, ${expected.y}, ${expected.z}, ${expected.w} } but was { ${actual.x}, ${actual.y}, ${actual.z}, ${actual.w} }")
    }

    @Test
    fun `test identity and loadIdentity`() {
        val q1 = Quaternion()
        assertTrue(q1.isIdentity())
        assertEquals(0f, q1.x)
        assertEquals(0f, q1.y)
        assertEquals(0f, q1.z)
        assertEquals(1f, q1.w)

        val q2 = Quaternion(1f, 2f, 3f, 4f)
        assertFalse(q2.isIdentity())
        q2.loadIdentity()
        assertTrue(q2.isIdentity())

        assertEquals(Quaternion(0f, 0f, 0f, 1f), Quaternion.DEFAULT)
    }

    @Test
    fun `test normalize`() {
        val q = Quaternion(2f, 0f, 0f, 0f)
        val mag = q.normalize()
        assertEquals(2f, mag)
        assertQuaternionEps(Quaternion(1f, 0f, 0f, 0f), q)

        val q2 = Quaternion(0f, 0f, 0f, 0f)
        val mag2 = q2.normalize()
        assertEquals(0f, mag2)
        assertTrue(q2.isIdentity())
    }

    @Test
    fun `test conjugate and conjugated`() {
        val q = Quaternion(1f, 2f, 3f, 4f)
        val conjugated = q.conjugated()
        assertEquals(Quaternion(-1f, -2f, -3f, 4f), conjugated)
        assertNotEquals(conjugated, q)

        val self = q.conjugate()
        assertEquals(Quaternion(-1f, -2f, -3f, 4f), q)
        assertSame(q, self)
    }

    @Test
    fun `test arithmetic operators`() {
        val q1 = Quaternion(1f, 2f, 3f, 4f)
        val q2 = Quaternion(5f, 6f, 7f, 8f)

        assertEquals(Quaternion(6f, 8f, 10f, 12f), q1 + q2)
        assertEquals(Quaternion(-4f, -4f, -4f, -4f), q1 - q2)
        assertEquals(Quaternion(-1f, -2f, -3f, -4f), -q1)
        assertEquals(Quaternion(2f, 4f, 6f, 8f), q1 * 2f)
        assertEquals(Quaternion(2f, 4f, 6f, 8f), 2f * q1)
    }

    @Test
    fun `test quaternion multiplication`() {
        val q1 = Quaternion(1f, 2f, 3f, 4f)
        val q2 = Quaternion(5f, 6f, 7f, 8f)

        val expected = Quaternion(24f, 48f, 48f, -6f)
        assertQuaternionEps(expected, q1 * q2)

        val q3 = Quaternion(1f, 2f, 3f, 4f)
        q3 *= q2
        assertQuaternionEps(expected, q3)
    }

    @Test
    fun `test vector multiplication`() {
        val q = Quaternion()
        q.setAngleAxis(F_PI_BY_TWO, 0f, 0f, 1f) // 90 degrees around Z axis

        val v3 = Vector3(1f, 0f, 0f)
        val r3 = q * v3
        assertVector3Eps(Vector3(0f, 1f, 0f), r3)

        val v4 = Vector4(1f, 0f, 0f, 1f)
        val r4 = q * v4
        assertVector4Eps(Vector4(0f, 1f, 0f, 1f), r4)
    }

    @Test
    fun `test setAngleAxis and getAngleAxis`() {
        val q = Quaternion()
        q.setAngleAxis(F_PI_BY_TWO, 1f, 0f, 0f)

        val (angle, axis) = q.getAngleAxis()
        assertEquals(F_PI_BY_TWO, angle, EPSILON)
        assertVector3Eps(Vector3(1f, 0f, 0f), axis)

        val q2 = Quaternion()
        q2.setAngleAxis(F_PI_BY_TWO, Vector3(0f, 1f, 0f))
        val (angle2, axis2) = q2.getAngleAxis()
        assertEquals(F_PI_BY_TWO, angle2, EPSILON)
        assertVector3Eps(Vector3(0f, 1f, 0f), axis2)

        val q3 = Quaternion()
        q3.setAngleAxis(F_PI_BY_TWO, Vector4(0f, 0f, 1f, 0f))
        val (angle3, axis3) = q3.getAngleAxis()
        assertEquals(F_PI_BY_TWO, angle3, EPSILON)
        assertVector3Eps(Vector3(0f, 0f, 1f), axis3)
    }

    @Test
    fun `test setEulerAngles and getEulerAngles`() {
        val q = Quaternion()
        val roll = F_PI / 4f
        val pitch = F_PI / 3f
        val yaw = F_PI / 6f
        q.setEulerAngles(roll, pitch, yaw)

        val (r, p, y) = q.getEulerAngles()
        assertEquals(roll, r, EPSILON)
        assertEquals(pitch, p, EPSILON)
        assertEquals(yaw, y, EPSILON)
    }

    @Test
    fun `test dot product`() {
        val q1 = Quaternion(1f, 2f, 3f, 4f)
        val q2 = Quaternion(5f, 6f, 7f, 8f)
        assertEquals(70f, dot(q1, q2))
    }

    @Test
    fun `test lerp`() {
        val q1 = Quaternion(1f, 0f, 0f, 0f)
        val q2 = Quaternion(0f, 1f, 0f, 0f)

        val l = lerp(0.5f, q1, q2)
        val expected = Quaternion(1f/sqrt(2f), 1f/sqrt(2f), 0f, 0f)
        assertQuaternionEps(expected, l)

        val lSingle = lerp(0.5f, q1)
        val expectedSingle = Quaternion(0.5f, 0f, 0f, 0.5f)
        expectedSingle.normalize()
        assertQuaternionEps(expectedSingle, lSingle)
    }

    @Test
    fun `test slerp`() {
        val q1 = Quaternion()
        q1.setAngleAxis(0f, 1f, 0f, 0f) // Identity
        val q2 = Quaternion()
        q2.setAngleAxis(F_PI_BY_TWO, 1f, 0f, 0f) // 90 deg around X

        val s = slerp(0.5f, q1, q2)
        val expected = Quaternion()
        expected.setAngleAxis(F_PI / 4f, 1f, 0f, 0f) // 45 deg around X
        assertQuaternionEps(expected, s)

        val sSingle = slerp(0.5f, q2)
        assertQuaternionEps(expected, sSingle)
    }

    @Test
    fun `test nlerp`() {
        val q1 = Quaternion(1f, 0f, 0f, 0f)
        val q2 = Quaternion(0f, 1f, 0f, 0f)

        val n = nlerp(0.5f, q1, q2)
        val expected = Quaternion(1f/sqrt(2f), 1f/sqrt(2f), 0f, 0f)
        assertQuaternionEps(expected, n)

        val q3 = Quaternion()
        q3.setAngleAxis(F_PI_BY_TWO, 1f, 0f, 0f)
        val nSingle = nlerp(0.5f, q3)
        // dot product implies lerp for this case (q3.w is positive)
        val expectedSingle = lerp(0.5f, q3)
        assertQuaternionEps(expectedSingle, nSingle)
    }

    @Test
    fun `test equality and hashcode`() {
        val q1 = Quaternion(1f, 2f, 3f, 4f)
        val q2 = Quaternion(1f, 2f, 3f, 4f)
        val q3 = Quaternion(0f, 2f, 3f, 4f)

        assertEquals(q1, q2)
        assertNotEquals(q1, q3)
        assertEquals(q1.hashCode(), q2.hashCode())
        assertNotEquals(q1.hashCode(), q3.hashCode())

        assertTrue(q1.isEqualEps(q2, EPSILON))
        val q4 = Quaternion(1f + EPSILON/2, 2f, 3f, 4f)
        assertTrue(q1.isEqualEps(q4, EPSILON))
        val q5 = Quaternion(1f + EPSILON*2, 2f, 3f, 4f)
        assertFalse(q1.isEqualEps(q5, EPSILON))
    }

    @Test
    fun `test get`() {
        val q1 = Quaternion(1f, 2f, 3f, 4f)
        assertEquals(1f, q1[0])
        assertEquals(2f, q1[1])
        assertEquals(3f, q1[2])
        assertEquals(4f, q1[3])
    }

    @Test
    fun `test toString`() {
        val q1 = Quaternion(1f, 2f, 3f, 4f)
        assertEquals("{ 1.0, 2.0, 3.0, 4.0 }", q1.toString())
    }
}
