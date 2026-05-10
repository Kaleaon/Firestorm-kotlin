package com.firestorm.llcharacter

import com.firestorm.llmath.F_PI
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.angleBetween
import com.firestorm.llmath.areParallel
import kotlin.math.acos
import kotlin.math.sqrt

private const val F_EPSILON = 0.00001f

/**
 * "Poor man's" IK for a 3-joint chain (A → B → C toward a Goal).
 * Modelled after Maya's ikRPSolver.  See lljointsolverrp3.cpp for full derivation.
 *
 * Call [setupJoints] once after the skeleton is built, then call [solve] each frame.
 */
class JointSolverRP3 {

    private var jointA:    Joint? = null
    private var jointB:    Joint? = null
    private var jointC:    Joint? = null
    private var jointGoal: Joint? = null

    private var lengthAB: Float = 1f
    private var lengthBC: Float = 1f

    var poleVector: Vector3 = Vector3(1f, 0f, 0f)
        private set
    private var bAxis:     Vector3 = Vector3()
    private var useBAxis:  Boolean = false

    var twist: Float = 0f

    private var jointABaseRotation: Quaternion = Quaternion()
    private var jointBBaseRotation: Quaternion = Quaternion()

    fun setupJoints(a: Joint, b: Joint, c: Joint, goal: Joint) {
        jointA    = a
        jointB    = b
        jointC    = c
        jointGoal = goal

        lengthAB = b.position.length()
        lengthBC = c.position.length()

        jointABaseRotation = a.rotation
        jointBBaseRotation = b.rotation
    }

    fun getPoleVector(): Vector3 = poleVector

    fun setPoleVector(v: Vector3) {
        poleVector = Vector3(v.x, v.y, v.z).also { it.normalize() }
    }

    fun setBAxis(v: Vector3) {
        bAxis    = Vector3(v.x, v.y, v.z).also { it.normalize() }
        useBAxis = true
    }

    fun getTwist(): Float = twist
    fun setTwist(t: Float) { twist = t }

    fun solve() {
        val a = jointA ?: return
        val b = jointB ?: return
        val c = jointC ?: return
        val g = jointGoal ?: return

        // Reset joints to their base rotations before computing world positions.
        a.rotation = jointABaseRotation
        b.rotation = jointBBaseRotation

        val aPos = a.getWorldPosition()
        val bPos = b.getWorldPosition()
        val cPos = c.getWorldPosition()
        val gPos = g.getWorldPosition()

        // Pole vector in world space (relative to jointA's parent frame).
        val worldParentMat = a.parent?.let { Matrix4FromJoint(it) }
        val poleVec = if (worldParentMat != null) worldParentMat.rotateVector(poleVector) else poleVector

        val abVec = bPos - aPos
        val bcVec = cPos - bPos
        var acVec = cPos - aPos
        val agVec = gPos - aPos

        val abLen  = abVec.length()
        val bcLen  = bcVec.length()
        val agLen  = agVec.length()

        val abacCompOrthoVec = abVec - acVec * ((abVec * acVec) / (acVec * acVec).coerceAtLeast(F_EPSILON))

        // Normal of the original ABC plane (or B-axis in world space if useBAxis)
        var abcNorm: Vector3 = if (useBAxis) {
            bAxis * b.getWorldRotation()   // rotate local b-axis into world space
        } else {
            when {
                areParallel(abVec, bcVec, 0.001f) -> when {
                    areParallel(poleVec, abVec, 0.001f) -> when {
                        areParallel(poleVec, agVec, 0.001f) -> return   // singular
                        else -> poleVec % agVec
                    }
                    else -> poleVec % abVec
                }
                else -> abVec % bcVec
            }
        }

        // Rotation of B: bend the knee/elbow to the correct extension angle.
        val abbcAng = angleBetween(abVec, bcVec)
        var abbcOrthoVec = abVec % bcVec
        var abacOrtho    = abacCompOrthoVec

        if (abbcOrthoVec.lengthSquared() < 0.001f) {
            abbcOrthoVec = poleVec % abVec
            abacOrtho    = poleVec
        }
        abbcOrthoVec.normalize()

        val agLenSq = agLen * agLen
        val cosTheta = ((agLenSq - abLen * abLen - bcLen * bcLen) / (2f * abLen * bcLen))
            .coerceIn(-1f, 1f)
        val theta = acos(cosTheta)
        val bRot  = Quaternion().setAngleAxis(theta - abbcAng, abbcOrthoVec)

        // Rotate bc by bRot, then find rotation that aligns new AC with AG.
        val bcRotated = bRot * bcVec
        acVec = abVec + bcRotated

        val cgRot = shortestArc(acVec, agVec)

        val abUpdated  = cgRot * abVec
        val bcUpdated  = cgRot * bcRotated
        abcNorm        = cgRot * abcNorm
        val acUpdated  = abUpdated + bcUpdated

        // Plane of the solution (APG plane normal)
        if (areParallel(agVec, poleVec, 0.001f)) return  // solution plane undefined

        val apgNorm = (poleVec % agVec).also { it.normalize() }

        if (!useBAxis) {
            abcNorm = if (areParallel(abUpdated, bcUpdated, 0.001f)) abcNorm
                      else (abUpdated % bcUpdated)
            abcNorm.normalize()
        }

        // Rotation to align the ABC plane with the APG plane.
        val pRot: Quaternion = when {
            areParallel(abcNorm, apgNorm, 0.001f) -> {
                if (abcNorm * apgNorm < 0f) Quaternion().setAngleAxis(F_PI, agVec)
                else Quaternion()
            }
            else -> shortestArc(abcNorm, apgNorm)
        }

        val twistRot = Quaternion().setAngleAxis(twist, agVec)

        // Combined rotation for A, individual rotation for B.
        val aRot = cgRot * pRot * twistRot
        b.setWorldRotation(b.getWorldRotation() * bRot)
        a.setWorldRotation(a.getWorldRotation() * aRot)
    }
}

// ---------------------------------------------------------------------------
// Helpers — these would normally live on Joint / Matrix4 but are private here
// to avoid polluting the public API until a full IK package is ported.
// ---------------------------------------------------------------------------

private operator fun Quaternion.times(v: Vector3): Vector3 {
    // q * v * q^-1 expanded (same formula as Quaternion.times(Vector3) in the math package)
    val rw = -x * v.x - y * v.y - z * v.z
    val rx =  w * v.x + y * v.z - z * v.y
    val ry =  w * v.y + z * v.x - x * v.z
    val rz =  w * v.z + x * v.y - y * v.x
    return Vector3(
        -rw * x + rx * w - ry * z + rz * y,
        -rw * y + ry * w - rz * x + rx * z,
        -rw * z + rz * w - rx * y + ry * x
    )
}

private operator fun Quaternion.times(other: Quaternion): Quaternion = Quaternion(
    other.w * x + other.x * w + other.y * z - other.z * y,
    other.w * y + other.y * w + other.z * x - other.x * z,
    other.w * z + other.z * w + other.x * y - other.y * x,
    other.w * w - other.x * x - other.y * y - other.z * z
)

/** Rotate a Vector3 by this quaternion (pure-rotation, no translation). */
private operator fun Vector3.times(q: Quaternion): Vector3 = q * this

/** Cross product via the `%` operator already defined on Vector3. */
private operator fun Vector3.rem(b: Vector3) = this.rem(b)

/** Dot product via the `*` operator already defined on Vector3. */
private operator fun Vector3.times(b: Vector3): Float = this.x * b.x + this.y * b.y + this.z * b.z

private fun Vector3.length(): Float = sqrt(x * x + y * y + z * z)
private fun Vector3.lengthSquared(): Float = x * x + y * y + z * z
private fun Vector3.normalize(): Float {
    val len = length()
    if (len > F_EPSILON) { x /= len; y /= len; z /= len }
    return len
}

/** Build a rotation matrix (world space) from a joint's parent chain, used only for the pole-vector transform. */
private fun Matrix4FromJoint(j: Joint): com.firestorm.llmath.Matrix4 =
    com.firestorm.llmath.Matrix4.fromQuaternionAndTranslation(j.getWorldRotation(), j.getWorldPosition())

/** Set world rotation on a joint (rotation relative to parent). */
private fun Joint.setWorldRotation(worldRot: Quaternion) {
    val parentWorldRot = parent?.getWorldRotation() ?: Quaternion()
    rotation = parentWorldRot.conjugated() * worldRot
}

/** Conjugate (inverse for unit quaternions). */
private fun Quaternion.conjugated() = Quaternion(-x, -y, -z, w)
private fun Quaternion.conjugate(): Quaternion { x = -x; y = -y; z = -z; return this }

/**
 * Shortest-arc quaternion from [from] to [to].
 * Mirrors LLQuaternion::shortestArc().
 */
private fun shortestArc(from: Vector3, to: Vector3): Quaternion {
    val f = Vector3(from.x, from.y, from.z).also { it.normalize() }
    val t = Vector3(to.x,   to.y,   to.z  ).also { it.normalize() }

    val dot = f.x * t.x + f.y * t.y + f.z * t.z
    if (dot >= 1f - F_EPSILON) return Quaternion()  // already aligned
    if (dot <= -1f + F_EPSILON) {
        // 180-degree rotation — pick any perpendicular axis
        var perp = Vector3(1f, 0f, 0f)
        if (kotlin.math.abs(f.x) > 0.9f) perp = Vector3(0f, 1f, 0f)
        val axis = Vector3(
            f.y * perp.z - f.z * perp.y,
            f.z * perp.x - f.x * perp.z,
            f.x * perp.y - f.y * perp.x
        ).also { it.normalize() }
        return Quaternion(axis.x, axis.y, axis.z, 0f)
    }
    val c = Vector3(
        f.y * t.z - f.z * t.y,
        f.z * t.x - f.x * t.z,
        f.x * t.y - f.y * t.x
    )
    val q = Quaternion(c.x, c.y, c.z, 1f + dot)
    q.normalize()
    return q
}

private fun Quaternion.normalize() {
    val mag = sqrt(x * x + y * y + z * z + w * w)
    if (mag > F_EPSILON) { x /= mag; y /= mag; z /= mag; w /= mag }
    else { x = 0f; y = 0f; z = 0f; w = 1f }
}
