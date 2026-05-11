package com.firestorm.newview

// ---- Stub types from other modules ----

/** 4-component float vector backed by a contiguous float array, used for vertex weights. */
class LLVector4a(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 0f) {
    val data = floatArrayOf(x, y, z, w)
    operator fun get(i: Int) = data[i]
    operator fun set(i: Int, v: Float) { data[i] = v }
}

/** 4x4 matrix used for skinning transforms. Row-major, SIMD-aligned in C++. */
class LLMatrix4a {
    val m = Array(4) { FloatArray(4) }
    fun clear() { for (row in m) row.fill(0f) }
    fun setMul(src: LLMatrix4a, scalar: Float) { for (i in 0..3) for (j in 0..3) m[i][j] = src.m[i][j] * scalar }
    fun add(src: LLMatrix4a) { for (i in 0..3) for (j in 0..3) m[i][j] += src.m[i][j] }
    fun affineTransform(pos: LLVector4a, out: LLVector4a) {
        TODO("GPU: apply 4x4 affine transform to a position vector (w=1 homogeneous)")
    }
    companion object {
        val identity: LLMatrix4a get() = LLMatrix4a().also { it.m[0][0] = 1f; it.m[1][1] = 1f; it.m[2][2] = 1f; it.m[3][3] = 1f }
    }
}

fun matMulUnsafe(a: LLMatrix4a, b: LLMatrix4a, out: LLMatrix4a) {
    TODO("GPU: multiply two 4x4 matrices and store result in out; called in a hot loop so should be optimised")
}

/** 3x3 matrix supporting quaternion extraction. */
class LLMatrix3(val mMatrix: Array<FloatArray> = Array(3) { FloatArray(3) }) {
    fun invert() { TODO("APR: invert 3x3 matrix in place") }
    fun quaternion(): LLQuaternion { TODO("APR: extract quaternion from rotation matrix") }
}

class LLMatrix4 {
    fun getMat3(): LLMatrix3 = TODO("APR: extract upper-left 3x3 sub-matrix")
}

class LLQuaternion {
    fun normalize(): LLQuaternion { TODO("APR: normalize quaternion in place and return this") }
}

/** Per-joint rigging extent info, populated during updateRiggingInfo. */
class LLJointRiggingInfo {
    private var riggedTo: Boolean = false
    private val extents = Array(2) { LLVector4a() }
    fun setIsRiggedTo(v: Boolean) { riggedTo = v }
    fun getRiggedExtents(): Array<LLVector4a> = extents
    fun isRiggedTo(): Boolean = riggedTo
}

class LLJointRiggingInfoTab {
    private val data: MutableList<LLJointRiggingInfo> = mutableListOf()
    private var needsUpdateFlag: Boolean = true

    fun needsUpdate(): Boolean = needsUpdateFlag
    fun setNeedsUpdate(v: Boolean) { needsUpdateFlag = v }
    fun size(): Int = data.size
    fun resize(n: Int) { while (data.size < n) data.add(LLJointRiggingInfo()) }
    operator fun get(i: Int): LLJointRiggingInfo = data[i]
}

/** Mesh volume face containing vertex positions, weights and rigging state. */
class LLVolumeFace {
    var mNumVertices: Int = 0
    var mPositions: Array<LLVector4a> = emptyArray()
    var mWeights: Array<LLVector4a>? = null
    val mJointRiggingInfoTab: LLJointRiggingInfoTab = LLJointRiggingInfoTab()
}

/** Mesh skin info, parallel arrays: jointNames[i] ↔ jointNums[i] ↔ invBindMatrix[i]. */
class LLMeshSkinInfo {
    val mJointNames: MutableList<String> = mutableListOf()
    val mJointNums: MutableList<Int> = mutableListOf()
    val mInvBindMatrix: MutableList<LLMatrix4a> = mutableListOf()
    val mBindPoseMatrix: MutableList<LLMatrix4a> = mutableListOf()
    var mInvalidJointsScrubbed: Boolean = false
    var mJointNumsInitialized: Boolean = false
}

/** Viewer avatar, providing joint lookup by name or number. */
abstract class LLVOAvatar {
    abstract fun getJoint(name: String): LLJoint?
    abstract fun getJoint(num: Int): LLJoint?
    abstract fun getFullname(): String
    abstract fun getDebugName(): String
    abstract fun isBuilt(): Boolean
    abstract val mInitFlags: Int
}

abstract class LLJoint {
    abstract fun getJointNum(): Int
    abstract fun getWorldMatrix4a(): LLMatrix4a
}

// Maximum animated joints; mirrors LL_CHARACTER_MAX_ANIMATED_JOINTS
const val LL_CHARACTER_MAX_ANIMATED_JOINTS = 256
// Maximum joints per mesh object; mirrors LL_MAX_JOINTS_PER_MESH_OBJECT
const val LL_MAX_JOINTS_PER_MESH_OBJECT = 110

fun updateMinMax(minOut: LLVector4a, maxOut: LLVector4a, point: LLVector4a) {
    for (i in 0..2) {
        if (point[i] < minOut[i]) minOut[i] = point[i]
        if (point[i] > maxOut[i]) maxOut[i] = point[i]
    }
}

// ---- LLSkinningUtil ----

object LLSkinningUtil {

    fun getMaxJointCount(): Int = LL_MAX_JOINTS_PER_MESH_OBJECT

    fun getMaxGLTFJointCount(): Int {
        TODO("GPU: query GL_MAX_UNIFORM_BLOCK_SIZE via OpenGL; divide by 48 (size of one 3x4 float matrix)")
    }

    fun getMeshJointCount(skin: LLMeshSkinInfo): UInt =
        minOf(getMaxJointCount().toUInt(), skin.mJointNames.size.toUInt())

    fun scrubInvalidJoints(avatar: LLVOAvatar, skin: LLMeshSkinInfo) {
        if (skin.mInvalidJointsScrubbed) return
        for (j in skin.mJointNames.indices) {
            if (avatar.getJoint(skin.mJointNames[j]) == null) {
                skin.mJointNames[j] = "mPelvis"
                skin.mJointNumsInitialized = false
            }
        }
        skin.mInvalidJointsScrubbed = true
    }

    fun initSkinningMatrixPalette(
        mat: Array<LLMatrix4a>,
        count: Int,
        skin: LLMeshSkinInfo,
        avatar: LLVOAvatar
    ) {
        initJointNums(skin, avatar)
        if (skin.mInvBindMatrix.size < count) return

        val world = Array(LL_CHARACTER_MAX_ANIMATED_JOINTS) { LLMatrix4a() }

        for (j in 0 until count) {
            val jointNum = skin.mJointNums[j]
            val joint = avatar.getJoint(jointNum)
            if (joint != null) {
                world[j] = joint.getWorldMatrix4a()
            } else {
                // Joint not found; leave mat[j] as invBindMatrix so the mesh is not
                // corrupted entirely — meshes with invalid joints should be rejected
                // at upload but legacy data may slip through.
                mat[j] = skin.mInvBindMatrix[j]
            }
        }

        for (j in 0 until count) {
            matMulUnsafe(skin.mInvBindMatrix[j], world[j], mat[j])
        }
    }

    fun checkSkinWeights(weights: Array<LLVector4a>, numVertices: UInt, skin: LLMeshSkinInfo) {
        val maxJoints = skin.mJointNames.size
        for (j in 0 until numVertices.toInt()) {
            var wsum = 0f
            for (k in 0..3) {
                val i = weights[j][k].toInt()
                require(i >= 0 && i < maxJoints) { "Joint index $i out of range [0, $maxJoints)" }
                wsum += weights[j][k] - i
            }
            require(wsum > 0f) { "Zero weight sum at vertex $j" }
        }
    }

    fun scrubSkinWeights(weights: Array<LLVector4a>, numVertices: UInt, skin: LLMeshSkinInfo) {
        val maxJoints = skin.mJointNames.size
        for (j in 0 until numVertices.toInt()) {
            for (k in 0..3) {
                val raw = weights[j][k]
                val i = raw.toInt().coerceIn(0, maxJoints - 1)
                val f = raw - raw.toInt()
                weights[j][k] = i + f
            }
        }
        checkSkinWeights(weights, numVertices, skin)
    }

    fun getPerVertexSkinMatrix(
        weights: FloatArray,
        mat: Array<LLMatrix4a>,
        handleBadScale: Boolean,
        finalMat: LLMatrix4a,
        maxJoints: UInt
    ) {
        finalMat.clear()
        val idx = IntArray(4)
        val wght = FloatArray(4)
        var scale = 0f

        for (k in 0..3) {
            val w = weights[k]
            idx[k] = w.toInt().coerceIn(0, maxJoints.toInt() - 1)
            wght[k] = w - w.toInt()
            scale += wght[k]
        }

        if (handleBadScale && scale <= 0f) {
            wght[0] = 1f; wght[1] = 0f; wght[2] = 0f; wght[3] = 0f
        } else {
            val invScale = 1f / scale
            for (k in 0..3) wght[k] *= invScale
        }

        for (k in 0..3) {
            val src = LLMatrix4a()
            src.setMul(mat[idx[k]], wght[k])
            finalMat.add(src)
        }
    }

    fun initJointNums(skin: LLMeshSkinInfo, avatar: LLVOAvatar) {
        if (skin.mJointNumsInitialized) return
        for (j in skin.mJointNames.indices) {
            val joint = if (skin.mJointNums[j] == -1)
                avatar.getJoint(skin.mJointNames[j])
            else
                avatar.getJoint(skin.mJointNums[j])
            skin.mJointNums[j] = joint?.getJointNum() ?: 0
        }
        skin.mJointNumsInitialized = true
    }

    fun updateRiggingInfo(skin: LLMeshSkinInfo, avatar: LLVOAvatar, volFace: LLVolumeFace) {
        if (!volFace.mJointRiggingInfoTab.needsUpdate()) return
        val numVerts = volFace.mNumVertices
        val numJoints = skin.mJointNames.size
        val weights = volFace.mWeights ?: return
        if (numVerts <= 0 || numJoints <= 0) return

        initJointNums(skin, avatar)
        if (volFace.mJointRiggingInfoTab.size() == 0) {
            volFace.mJointRiggingInfoTab.resize(LL_CHARACTER_MAX_ANIMATED_JOINTS)
            val rigInfoTab = volFace.mJointRiggingInfoTab

            for (i in 0 until volFace.mNumVertices) {
                val pos = volFace.mPositions[i]
                val w = weights[i]
                val idx = IntArray(4)
                val wght = FloatArray(4)

                for (k in 0..3) {
                    val raw = w[k]
                    idx[k] = raw.toInt().coerceIn(0, LL_CHARACTER_MAX_ANIMATED_JOINTS - 1)
                    wght[k] = raw - idx[k]
                }

                for (k in 0..3) {
                    val jointIndex = idx[k]
                    if (wght[k] > 0.2f && numJoints > jointIndex) {
                        val jointNum = skin.mJointNums[jointIndex]
                        if (jointNum >= 0 && jointNum < LL_CHARACTER_MAX_ANIMATED_JOINTS) {
                            rigInfoTab[jointNum].setIsRiggedTo(true)
                            val bindMat = if (skin.mBindPoseMatrix.size > jointIndex)
                                skin.mBindPoseMatrix[jointIndex]
                            else
                                LLMatrix4a.identity
                            val posJointSpace = LLVector4a()
                            bindMat.affineTransform(pos, posJointSpace)
                            val extents = rigInfoTab[jointNum].getRiggedExtents()
                            updateMinMax(extents[0], extents[1], posJointSpace)
                        }
                    }
                }
            }
            volFace.mJointRiggingInfoTab.setNeedsUpdate(false)
        }
    }

    // Strips scale components from a bind-shape matrix before extracting rotation.
    // Needed because bind-shape matrices from exporters often bake non-uniform scale.
    fun getUnscaledQuaternion(mat4: LLMatrix4): LLQuaternion {
        val bindMat = mat4.getMat3()
        for (i in 0..2) {
            var len = 0f
            for (j in 0..2) len += bindMat.mMatrix[i][j] * bindMat.mMatrix[i][j]
            if (len > 0f) {
                val invLen = 1f / kotlin.math.sqrt(len)
                for (j in 0..2) bindMat.mMatrix[i][j] *= invLen
            }
        }
        bindMat.invert()
        return bindMat.quaternion().normalize()
    }
}

// ---- FSSkinningUtil ----

object FSSkinningUtil {
    /**
     * SSE-accelerated per-vertex skinning matrix accumulation (FireStorm extension).
     * The SIMD path (intrinsics) is replaced with a scalar fallback; the assertion
     * that handle_bad_scale is always false is preserved as a precondition check.
     */
    fun getPerVertexSkinMatrixSSE(
        weights: LLVector4a,
        mat: Array<LLMatrix4a>,
        handleBadScale: Boolean,
        finalMat: LLMatrix4a,
        maxJoints: UInt
    ) {
        require(!handleBadScale) { "FSSkinningUtil: handleBadScale must be false in SSE path" }
        finalMat.clear()

        val idx = IntArray(4)
        val wght = FloatArray(4)
        var scale = 0f

        for (k in 0..3) {
            idx[k] = weights[k].toInt().coerceIn(0, maxJoints.toInt() - 1)
            wght[k] = weights[k] - weights[k].toInt()
            scale += wght[k]
        }

        if (scale > 0f) {
            val invScale = 1f / scale
            for (k in 0..3) wght[k] *= invScale
        }

        for (k in 0..3) {
            val src = LLMatrix4a()
            src.setMul(mat[idx[k]], wght[k])
            finalMat.add(src)
        }
    }
}
