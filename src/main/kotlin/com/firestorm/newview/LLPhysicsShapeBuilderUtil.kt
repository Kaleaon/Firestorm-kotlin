package com.firestorm.newview

const val SHAPE_BUILDER_DEFAULT_VOLUME_DETAIL: Int = 1

const val SHAPE_BUILDER_IMPLICIT_THRESHOLD_HOLLOW: Float = 0.10f
const val SHAPE_BUILDER_IMPLICIT_THRESHOLD_HOLLOW_SPHERES: Float = 0.90f
const val SHAPE_BUILDER_IMPLICIT_THRESHOLD_PATH_CUT: Float = 0.05f
const val SHAPE_BUILDER_IMPLICIT_THRESHOLD_TAPER: Float = 0.05f
const val SHAPE_BUILDER_IMPLICIT_THRESHOLD_TWIST: Float = 0.09f
const val SHAPE_BUILDER_IMPLICIT_THRESHOLD_SHEAR: Float = 0.05f

const val COLLISION_TOLERANCE: Float = 0.1f

const val SHAPE_BUILDER_ENTRY_SNAP_SCALE_BIN_SIZE: Float = 0.15f
const val SHAPE_BUILDER_ENTRY_SNAP_PARAMETER_BIN_SIZE: Float = 0.010f
const val SHAPE_BUILDER_CONVEXIFICATION_SIZE: Float = 2f * COLLISION_TOLERANCE
const val SHAPE_BUILDER_MIN_GEOMETRY_SIZE: Float = 0.5f * COLLISION_TOLERANCE
const val SHAPE_BUILDER_USER_MESH_CONVEXIFICATION_SIZE: Float = 0.5f

class LLPhysicsVolumeParams(
    params: LLVolumeParams,
    private val mForceConvex: Boolean
) : LLVolumeParams(params) {

    fun shouldForceConvex(): Boolean = mForceConvex

    fun hasDecomposition(): Boolean {
        if (!isMeshSculpt()) return false
        val meshId = getSculptID()
        if (meshId == null || meshId.mostSignificantBits == 0L && meshId.leastSignificantBits == 0L) return false
        return TODO("APR: gMeshRepo.getDecomposition(meshId) != null")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLPhysicsVolumeParams) return false
        return super.equals(other) && mForceConvex == other.mForceConvex
    }

    override fun hashCode(): Int = 31 * super.hashCode() + mForceConvex.hashCode()

    operator fun compareTo(other: LLPhysicsVolumeParams): Int {
        if (super.volumeParamsNotEquals(other)) return super.volumeParamsCompareTo(other)
        return when {
            !other.mForceConvex && mForceConvex -> -1
            else -> 0
        }
    }
}

object LLPhysicsShapeBuilderUtil {

    class PhysicsShapeSpecification {
        enum class ShapeType {
            BOX,
            SPHERE,
            CYLINDER,
            USER_CONVEX,
            PRIM_CONVEX,
            SCULPT,
            USER_MESH,
            PRIM_MESH,
            INVALID
        }

        var type: ShapeType = ShapeType.INVALID
        var scale: FloatArray = floatArrayOf(0f, 0f, 0f)
        var center: FloatArray = floatArrayOf(0f, 0f, 0f)

        fun isConvex(): Boolean = type != ShapeType.USER_MESH && type != ShapeType.PRIM_MESH && type != ShapeType.INVALID
        fun isMesh(): Boolean = type == ShapeType.USER_MESH || type == ShapeType.PRIM_MESH
        fun getType(): ShapeType = type
        fun getScale(): FloatArray = scale
        fun getCenter(): FloatArray = center
    }

    fun determinePhysicsShape(
        volumeParams: LLPhysicsVolumeParams,
        scale: FloatArray,
        hasDecomp: Boolean,
        specOut: PhysicsShapeSpecification
    ) {
        val profileParams = volumeParams.getProfileParams()
        val pathParams = volumeParams.getPathParams()

        specOut.scale = scale.copyOf()

        val avgScale = (scale[0] + scale[1] + scale[2]) / 3.0f

        var minSizeCounts = 0
        for (i in 0..2) {
            if (scale[i] < SHAPE_BUILDER_CONVEXIFICATION_SIZE) minSizeCounts++
        }

        val profileComplete = (profileParams.getBegin() <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_PATH_CUT / avgScale) &&
                              (profileParams.getEnd() >= (1.0f - SHAPE_BUILDER_IMPLICIT_THRESHOLD_PATH_CUT / avgScale))

        val pathComplete = (pathParams.getBegin() <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_PATH_CUT / avgScale) &&
                           (pathParams.getEnd() >= (1.0f - SHAPE_BUILDER_IMPLICIT_THRESHOLD_PATH_CUT / avgScale))

        val simpleParams = (volumeParams.getHollow() <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_HOLLOW / avgScale) &&
                           (kotlin.math.abs(pathParams.getShearX()) <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_SHEAR / avgScale) &&
                           (kotlin.math.abs(pathParams.getShearY()) <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_SHEAR / avgScale) &&
                           (!volumeParams.isMeshSculpt() && !volumeParams.isSculpt())

        if (simpleParams && profileComplete) {
            val noTaper = (kotlin.math.abs(pathParams.getScaleX() - 1.0f) <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_TAPER / avgScale) &&
                          (kotlin.math.abs(pathParams.getScaleY() - 1.0f) <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_TAPER / avgScale)

            val noTwist = (kotlin.math.abs(pathParams.getTwistBegin()) <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_TWIST / avgScale) &&
                          (kotlin.math.abs(pathParams.getTwistEnd()) <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_TWIST / avgScale)

            if (profileParams.getCurveType() == LL_PCODE_PROFILE_SQUARE &&
                pathParams.getCurveType() == LL_PCODE_PATH_LINE &&
                noTaper && noTwist
            ) {
                specOut.type = PhysicsShapeSpecification.ShapeType.BOX
                if (pathComplete) return
                specOut.scale[0] = maxOf(scale[0], SHAPE_BUILDER_MIN_GEOMETRY_SIZE)
                specOut.scale[1] = maxOf(scale[1], SHAPE_BUILDER_MIN_GEOMETRY_SIZE)
                specOut.scale[2] = maxOf(scale[2] * (pathParams.getEnd() - pathParams.getBegin()), SHAPE_BUILDER_MIN_GEOMETRY_SIZE)
                specOut.center = floatArrayOf(0f, 0f, 0.5f * scale[2] * (pathParams.getEnd() + pathParams.getBegin() - 1.0f))
                return
            }

            if (pathComplete &&
                profileParams.getCurveType() == LL_PCODE_PROFILE_CIRCLE_HALF &&
                pathParams.getCurveType() == LL_PCODE_PATH_CIRCLE &&
                kotlin.math.abs(volumeParams.getTaper()) <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_TAPER / avgScale &&
                noTwist
            ) {
                if (scale[0] == scale[2] && scale[1] == scale[2]) {
                    specOut.type = PhysicsShapeSpecification.ShapeType.SPHERE
                    specOut.scale = scale.copyOf()
                    return
                } else if (minSizeCounts > 1) {
                    for (i in 0..2) {
                        if (specOut.scale[i] < SHAPE_BUILDER_CONVEXIFICATION_SIZE) {
                            specOut.scale[i] *= 0.75f
                        }
                    }
                    specOut.type = PhysicsShapeSpecification.ShapeType.BOX
                    return
                }
            }

            if (scale[0] == scale[1] &&
                profileParams.getCurveType() == LL_PCODE_PROFILE_CIRCLE &&
                pathParams.getCurveType() == LL_PCODE_PATH_LINE &&
                volumeParams.getBeginS() <= SHAPE_BUILDER_IMPLICIT_THRESHOLD_PATH_CUT / avgScale &&
                volumeParams.getEndS() >= (1.0f - SHAPE_BUILDER_IMPLICIT_THRESHOLD_PATH_CUT / avgScale) &&
                noTaper
            ) {
                if (minSizeCounts > 1) {
                    for (i in 0..2) {
                        if (specOut.scale[i] < SHAPE_BUILDER_CONVEXIFICATION_SIZE) {
                            specOut.scale[i] *= 0.75f
                        }
                    }
                    specOut.type = PhysicsShapeSpecification.ShapeType.BOX
                } else {
                    specOut.type = PhysicsShapeSpecification.ShapeType.CYLINDER
                    val length = (volumeParams.getPathParams().getEnd() - volumeParams.getPathParams().getBegin()) * scale[2]
                    specOut.scale[1] = specOut.scale[0]
                    specOut.scale[2] = length
                    specOut.center = floatArrayOf(
                        0f, 0f,
                        0.5f * (volumeParams.getPathParams().getBegin() + volumeParams.getPathParams().getEnd() - 1f) * scale[2]
                    )
                }
                return
            }
        }

        if (minSizeCounts == 3 ||
            (pathComplete && profileComplete &&
             pathParams.getCurveType() == LL_PCODE_PATH_LINE && minSizeCounts > 1)
        ) {
            specOut.type = PhysicsShapeSpecification.ShapeType.BOX
            specOut.scale = scale.copyOf()
            return
        }

        if (minSizeCounts == 1 && avgScale > 3f) {
            for (i in 0..2) {
                specOut.scale[i] = maxOf(specOut.scale[i], COLLISION_TOLERANCE)
            }
        }

        when {
            volumeParams.shouldForceConvex() -> {
                specOut.type = PhysicsShapeSpecification.ShapeType.USER_CONVEX
            }
            volumeParams.isConvex() || minSizeCounts > 1 -> {
                specOut.type = PhysicsShapeSpecification.ShapeType.PRIM_CONVEX
            }
            volumeParams.isMeshSculpt() -> {
                val tooSmall = scale[0] < SHAPE_BUILDER_USER_MESH_CONVEXIFICATION_SIZE ||
                               scale[1] < SHAPE_BUILDER_USER_MESH_CONVEXIFICATION_SIZE ||
                               scale[2] < SHAPE_BUILDER_USER_MESH_CONVEXIFICATION_SIZE
                specOut.type = if (tooSmall) {
                    if (hasDecomp) PhysicsShapeSpecification.ShapeType.USER_MESH
                    else PhysicsShapeSpecification.ShapeType.PRIM_CONVEX
                } else {
                    PhysicsShapeSpecification.ShapeType.USER_MESH
                }
            }
            volumeParams.isSculpt() -> {
                specOut.type = PhysicsShapeSpecification.ShapeType.SCULPT
            }
            else -> {
                specOut.type = PhysicsShapeSpecification.ShapeType.PRIM_MESH
            }
        }
    }
}

private const val LL_PCODE_PROFILE_SQUARE: Int = 0x01
private const val LL_PCODE_PROFILE_CIRCLE: Int = 0x07
private const val LL_PCODE_PROFILE_CIRCLE_HALF: Int = 0x05
private const val LL_PCODE_PATH_LINE: Int = 0x10
private const val LL_PCODE_PATH_CIRCLE: Int = 0x20
