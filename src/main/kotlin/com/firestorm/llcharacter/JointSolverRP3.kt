package com.firestorm.llcharacter

import com.firestorm.llmath.F_PI
import com.firestorm.llmath.Matrix4
import com.firestorm.llmath.Quaternion
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.angleBetween
import com.firestorm.llmath.areParallel
import kotlin.math.acos
import kotlin.math.abs
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

        // Cache bone lengths from local positions (these are constant once skeleton is set up)
        lengthAB = b.position.length()
        lengthBC = c.position.length()

        jointABaseRotation = a.rotation.copy()
        jointBBaseRotation = b.rotation.copy()
    }

    fun getPoleVector(): Vector3 = poleVector

    fun setPoleVector(v: Vector3) {
        poleVector = Vector3(v.x, v.y, v.z)
        poleVector.normalize()
    }

    fun setBAxis(v: Vector3) {
        bAxis    = Vector3(v.x, v.y, v.z)
        bAxis.normalize()
        useBAxis = true
    }

    fun getTwist(): Float = twist
    fun setTwist(t: Float) { twist = t }

    fun solve() {
        val a = jointA ?: return
        val b = jointB ?: return
        val c = jointC ?: return
        val g = jointGoal ?: return

        // Reset joints to base rotations so world positions are computed from a clean state.
        a.rotation = jointABaseRotation.copy()
        b.rotation = jointBBaseRotation.copy()

        val aPos = a.getWorldPosition()
        val bPos = b.getWorldPosition()
        val cPos = c.getWorldPosition()
        val gPos = g.getWorldPosition()

        // Transform pole vector from parent-of-A local space into world space.
        val poleVec: Vector3 = a.parent?.let { parent ->
            Matrix4.fromQuaternionAndTranslation(parent.getWorldRotation(), parent.getWorldPosition())
                .rotateVector(poleVector)
        } ?: Vector3(poleVector.x, poleVector.y, poleVector.z)

        // World-space bone vectors
        var abVec = bPos - aPos
        var bcVec = cPos - bPos
        var acVec = cPos - aPos
        val agVec = gPos - aPos

        val abLen = abVec.length()
        val bcLen = bcVec.length()
        val agLen = agVec.length()

        // Component of A->B orthogonal to A->C (used when bones are parallel)
        val acLenSq = (acVec * acVec).coerceAtLeast(F_EPSILON)
        var abacCompOrthoVec = abVec - acVec * ((abVec * acVec) / acLenSq)

        // Normal of the original ABC plane (or B-axis in world space if useBAxis)
        var abcNorm: Vector3 = if (useBAxis) {
            // rotate the local b-axis into world space
            b.getWorldRotation() * bAxis
        } else {
            when {
                areParallel(abVec, bcVec, 0.001f) -> when {
                    areParallel(poleVec, abVec, 0.001f) -> when {
                        areParallel(poleVec, agVec, 0.001f) -> return   // fully singular
                        else -> poleVec % agVec
                    }
                    else -> poleVec % abVec
                }
                else -> abVec % bcVec
            }
        }

        // Rotation of B: bend to the extension angle dictated by the A-G distance.
        val abbcAng = angleBetween(abVec, bcVec)
        var abbcOrthoVec = abVec % bcVec
        if (abbcOrthoVec.lengthSquared() < 0.001f) {
            abbcOrthoVec = poleVec % abVec
            abacCompOrthoVec = Vector3(poleVec.x, poleVec.y, poleVec.z)
        }
        abbcOrthoVec.normalize()

        val agLenSq = agLen * agLen
        val cosTheta = ((agLenSq - abLen * abLen - bcLen * bcLen) / (2f * abLen * bcLen))
            .coerceIn(-1f, 1f)
        val theta = acos(cosTheta)
        val bRot = Quaternion().setAngleAxis(theta - abbcAng, abbcOrthoVec)

        // Rotate B->C by bRot, then build the rotation that aligns new A->C with A->G.
        val bcRotated = bRot * bcVec
        acVec = abVec + bcRotated

        val cgRot = shortestArc(acVec, agVec)

        val abUpdated = cgRot * abVec
        val bcUpdated = cgRot * bcRotated
        abcNorm       = cgRot * abcNorm
        val acUpdated = abUpdated + bcUpdated

        // APG plane normal
        if (areParallel(agVec, poleVec, 0.001f)) return  // solution plane undefined

        val apgNorm = (poleVec % agVec).also { it.normalize() }

        if (!useBAxis) {
            abcNorm = if (areParallel(abUpdated, bcUpdated, 0.001f)) abcNorm
                      else (abUpdated % bcUpdated)
            abcNorm.normalize()
        }

        // Rotation to align the ABC plane with the APG plane.
        val pRot: Quaternion = when {
            areParallel(abcNorm, apgNorm, 0.001f) ->
                if (abcNorm * apgNorm < 0f) Quaternion().setAngleAxis(F_PI, agVec)
                else Quaternion()
            else -> shortestArc(abcNorm, apgNorm)
        }

        val twistRot = Quaternion().setAngleAxis(twist, agVec)

        val aRot = cgRot * pRot * twistRot
        b.setWorldRotation(b.getWorldRotation() * bRot)
        a.setWorldRotation(a.getWorldRotation() * aRot)
    }
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

/** Set world rotation on a joint by back-computing the local rotation from parent. */
private fun Joint.setWorldRotation(worldRot: Quaternion) {
    val parentWorldRot = parent?.getWorldRotation() ?: Quaternion()
    rotation = parentWorldRot.conjugated() * worldRot
}

private fun Quaternion.conjugated(): Quaternion = Quaternion(-x, -y, -z, w)

private fun Quaternion.copy(): Quaternion = Quaternion(x, y, z, w)

private fun Vector3.length(): Float = sqrt(x * x + y * y + z * z)
private fun Vector3.lengthSquared(): Float = x * x + y * y + z * z

/**
 * Shortest-arc quaternion from [from] to [to].
 * Mirrors LLQuaternion::shortestArc().
 */
private fun shortestArc(from: Vector3, to: Vector3): Quaternion {
    val f = Vector3(from.x, from.y, from.z).also { it.normalize() }
    val t = Vector3(to.x,   to.y,   to.z  ).also { it.normalize() }
    val dot = f * t   // Vector3.times(Vector3) is dot product
    return when {
        dot >= 1f - F_EPSILON  -> Quaternion()   // already aligned
        dot <= -1f + F_EPSILON -> {
            // 180-degree rotation — pick any perpendicular axis
            val perp = if (abs(f.x) > 0.9f) Vector3(0f, 1f, 0f) else Vector3(1f, 0f, 0f)
            val axis = (f % perp).also { it.normalize() }
            Quaternion(axis.x, axis.y, axis.z, 0f)
        }
        else -> {
            val c = f % t   // cross product
            val q = Quaternion(c.x, c.y, c.z, 1f + dot)
            q.normalize()
            q
        }
    }
}
