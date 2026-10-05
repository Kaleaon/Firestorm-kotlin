package com.firestorm.newview

import kotlin.math.*

object Noise {

    private const val B = 0x100
    private const val BM = 0xff
    private const val NF32 = 4096.0f

    val p = IntArray(B + B + 2)
    val g3 = Array(B + B + 2) { FloatArray(3) }
    val g2 = Array(B + B + 2) { FloatArray(2) }
    val g1 = FloatArray(B + B + 2)
    var noiseStart = true

    private fun normalize2(v: FloatArray) {
        val s = 1.0f / sqrt(v[0] * v[0] + v[1] * v[1])
        v[0] *= s
        v[1] *= s
    }

    private fun normalize3(v: FloatArray) {
        val s = 1.0f / sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        v[0] *= s
        v[1] *= s
        v[2] *= s
    }

    fun init() {
        // Seeded with 42 for repeatable terrain texturing; entropy re-seeded at end.
        val rng = java.util.Random(42)

        for (i in 0 until B) {
            p[i] = i
            g1[i] = ((rng.nextInt(B + B)) - B).toFloat() / B

            for (j in 0 until 2) g2[i][j] = ((rng.nextInt(B + B)) - B).toFloat() / B
            normalize2(g2[i])

            for (j in 0 until 3) g3[i][j] = ((rng.nextInt(B + B)) - B).toFloat() / B
            normalize3(g3[i])
        }

        var i = B
        while (--i > 0) {
            val j = rng.nextInt(B)
            val k = p[i]
            p[i] = p[j]
            p[j] = k
        }

        for (i in 0 until B + 2) {
            p[B + i] = p[i]
            g1[B + i] = g1[i]
            for (j in 0 until 2) g2[B + i][j] = g2[i][j]
            for (j in 0 until 3) g3[B + i][j] = g3[i][j]
        }
    }

    private fun sCurve(t: Float): Float = t * t * (3.0f - 2.0f * t)

    private fun lerpM(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    private fun fastSetup(vec: Float): FastSetupResult {
        val r1pre = vec + NF32
        val tInt = r1pre.toInt()
        val b0 = (tInt and 0xFF).toUByte()
        val b1 = ((tInt + 1) and 0xFF).toUByte()
        val r0 = r1pre - tInt
        val r1 = r0 - 1.0f
        return FastSetupResult(b0, b1, r0, r1)
    }

    private data class FastSetupResult(val b0: UByte, val b1: UByte, val r0: Float, val r1: Float)

    private fun fastAt2(rx: Float, ry: Float, q: FloatArray, offset: Int = 0): Float =
        rx * q[offset] + ry * q[offset + 1]

    private fun fastAt3(rx: Float, ry: Float, rz: Float, q: FloatArray, offset: Int = 0): Float =
        rx * q[offset] + ry * q[offset + 1] + rz * q[offset + 2]

    fun noise1(arg: Float): Float {
        if (noiseStart) {
            noiseStart = false
            init()
        }

        val r1pre = arg + NF32
        val tInt = r1pre.toInt()
        val bx0 = tInt and BM
        val bx1 = (bx0 + 1) and BM
        val rx0 = r1pre - tInt
        val rx1 = rx0 - 1.0f

        val sx = sCurve(rx0)
        val u = rx0 * g1[p[bx0]]
        val v = rx1 * g1[p[bx1]]
        return lerpM(sx, u, v)
    }

    fun noise2(vec: FloatArray): Float {
        if (noiseStart) {
            noiseStart = false
            init()
        }

        val (bx0, bx1, rx0, rx1) = fastSetup(vec[0])
        val (by0, by1, ry0, ry1) = fastSetup(vec[1])

        val i = p[bx0.toInt()]
        val j = p[bx1.toInt()]

        val b00 = p[i + by0.toInt()]
        val b10 = p[j + by0.toInt()]
        val b01 = p[i + by1.toInt()]
        val b11 = p[j + by1.toInt()]

        val sx = sCurve(rx0)
        val sy = sCurve(ry0)

        val u00 = fastAt2(rx0, ry0, g2[b00])
        val v10 = fastAt2(rx1, ry0, g2[b10])
        val a = lerpM(sx, u00, v10)

        val u01 = fastAt2(rx0, ry1, g2[b01])
        val v11 = fastAt2(rx1, ry1, g2[b11])
        val b = lerpM(sx, u01, v11)

        return lerpM(sy, a, b)
    }

    fun noise3(vec: FloatArray): Float {
        if (noiseStart) {
            noiseStart = false
            init()
        }

        val (bx0, bx1, rx0, rx1) = fastSetup(vec[0])
        val (by0, by1, ry0, ry1) = fastSetup(vec[1])
        val (bz0, bz1, rz0, rz1) = fastSetup(vec[2])

        val i = p[bx0.toInt()]
        val j = p[bx1.toInt()]

        val b00 = p[i + by0.toInt()]
        val b10 = p[j + by0.toInt()]
        val b01 = p[i + by1.toInt()]
        val b11 = p[j + by1.toInt()]

        val t = sCurve(rx0)
        val sy = sCurve(ry0)
        val sz = sCurve(rz0)

        val u1 = fastAt3(rx0, ry0, rz0, g3[b00 + bz0.toInt()])
        val v1 = fastAt3(rx1, ry0, rz0, g3[b10 + bz0.toInt()])
        val a1 = lerpM(t, u1, v1)

        val u2 = fastAt3(rx0, ry1, rz0, g3[b01 + bz0.toInt()])
        val v2 = fastAt3(rx1, ry1, rz0, g3[b11 + bz0.toInt()])
        val b1 = lerpM(t, u2, v2)

        val c = lerpM(sy, a1, b1)

        val u3 = fastAt3(rx0, ry0, rz1, g3[b00 + bz1.toInt()])
        val v3 = fastAt3(rx1, ry0, rz1, g3[b10 + bz1.toInt()])
        val a2 = lerpM(t, u3, v3)

        val u4 = fastAt3(rx0, ry1, rz1, g3[b01 + bz1.toInt()])
        val v4 = fastAt3(rx1, ry1, rz1, g3[b11 + bz1.toInt()])
        val b2 = lerpM(t, u4, v4)

        val d = lerpM(sy, a2, b2)

        return lerpM(sz, c, d)
    }

    fun turbulence2(v: FloatArray, freq: Float): Float {
        var t = 0.0f
        var f = freq
        val vec = FloatArray(2)
        while (f >= 1.0f) {
            vec[0] = f * v[0]
            vec[1] = f * v[1]
            t += noise2(vec) / f
            f *= 0.5f
        }
        return t
    }

    fun turbulence3(v: FloatArray, freq: Float): Float {
        var t = 0.0f
        var f = freq
        val vec = FloatArray(3)
        while (f >= 1.0f) {
            vec[0] = f * v[0]
            vec[1] = f * v[1]
            vec[2] = f * v[2]
            t += noise3(vec) / f
            f *= 0.5f
        }
        return t
    }

    fun clouds3(v: FloatArray, freq: Float): Float {
        var t = 0.0f
        var f = freq
        val vec = FloatArray(3)
        while (f >= 1.0f) {
            vec[0] = f * v[0]
            vec[1] = f * v[1]
            vec[2] = f * v[2]
            val n = noise3(vec)
            t += (n * n) / f
            f *= 0.5f
        }
        return t
    }

    fun bias(a: Float, b: Float): Float =
        a.toDouble().pow(ln(b.toDouble()) / ln(0.5)).toFloat()

    fun gain(a: Float, b: Float): Float {
        val p = (ln(1.0 - b) / ln(0.5)).toFloat()
        return when {
            a < 0.001f -> 0.0f
            a > 0.999f -> 1.0f
            a < 0.5f   -> (2.0f * a).toDouble().pow(p.toDouble()).toFloat() / 2.0f
            else       -> 1.0f - (2.0f * (1.0f - a)).toDouble().pow(p.toDouble()).toFloat() / 2.0f
        }
    }
}
