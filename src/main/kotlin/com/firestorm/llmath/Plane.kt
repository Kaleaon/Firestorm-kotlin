package com.firestorm.llmath

import kotlin.math.*

class Plane {
    private var nx: Float = 0f
    private var ny: Float = 0f
    private var nz: Float = 0f
    private var d: Float  = 1f

    constructor()

    constructor(point: Vector3, dVal: Float) {
        nx = point.x; ny = point.y; nz = point.z; d = dVal
    }

    constructor(point: Vector3, n: Vector3) {
        val dVal = -(point * n)
        nx = n.x; ny = n.y; nz = n.z; d = dVal
    }

    constructor(p0: Vector3, p1: Vector3, p2: Vector3) {
        val u = p1 - p0
        val v = p2 - p0
        val w = Vector3(u.x, u.y, u.z) % v
        w.normalize()
        val dVal = -(w * p0)
        nx = w.x; ny = w.y; nz = w.z; d = dVal
    }

    fun set(other: Plane) { nx = other.nx; ny = other.ny; nz = other.nz; d = other.d }

    fun normal(): Vector3 = Vector3(nx, ny, nz)

    fun dist(v: Vector3): Float = nx * v.x + ny * v.y + nz * v.z + d

    fun clear() { nx = 0f; ny = 0f; nz = 0f; d = 1f }

    operator fun get(idx: Int): Float = when (idx) { 0 -> nx; 1 -> ny; 2 -> nz; else -> d }

    fun equal(other: Plane): Boolean =
        nx == other.nx && ny == other.ny && nz == other.nz && d == other.d

    override fun toString(): String = "Plane($nx, $ny, $nz, $d)"
}
