package com.firestorm.newview

import java.util.UUID
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// PointAt target types
// ---------------------------------------------------------------------------

enum class PointAtType {
    NONE,
    SELECT,
    GRAB,
    CLEAR;
}

// Packet layout constants (byte offsets in the binary TypeData blob)
private const val SOURCE_AVATAR  = 0
private const val TARGET_OBJECT  = 16
private const val TARGET_POS     = 32
private const val POINTAT_TYPE   = 56
private const val PKT_SIZE       = 57

private const val MAX_SENDS_PER_SEC: Float              = 4f
private const val MIN_DELTAPOS_FOR_UPDATE_SQUARED: Float = 0.05f * 0.05f

// can't use Float.MAX_VALUE directly because we add it to current frame time
private val POINTAT_MAX_TIMEOUT: Float = Float.MAX_VALUE / 4f

private val POINTAT_TIMEOUTS = mapOf(
    PointAtType.NONE   to POINTAT_MAX_TIMEOUT,
    PointAtType.SELECT to POINTAT_MAX_TIMEOUT,
    PointAtType.GRAB   to POINTAT_MAX_TIMEOUT,
    PointAtType.CLEAR  to 0f
)

private val POINTAT_PRIORITIES = mapOf(
    PointAtType.NONE   to 0,
    PointAtType.SELECT to 1,
    PointAtType.GRAB   to 2,
    PointAtType.CLEAR  to 3
)

// ---------------------------------------------------------------------------
// HUDEffectPointAt
// ---------------------------------------------------------------------------

class HUDEffectPointAt(type: UByte) : HUDEffect(type) {

    companion object {
        var debugPointAt: Boolean = false
    }

    private var targetType: PointAtType = PointAtType.NONE
    private var targetOffsetGlobal: Vector3d = Vector3d.ZERO
    private var lastSentOffsetGlobal: Vector3 = Vector3.ZERO
    private var killTime: Float = 0f
    private var timerStartNs: Long = System.nanoTime()
    private var targetPos: Vector3 = Vector3.ZERO
    private var lastSendTime: Float = 0f

    init { clearPointAtTarget() }

    private fun elapsed(): Float = ((System.nanoTime() - timerStartNs) / 1_000_000_000.0).toFloat()

    fun getPointAtType(): PointAtType = targetType
    fun getPointAtPosAgent(): Vector3 = targetPos
    fun getPointAtPosGlobal(): Vector3d {
        val globalPos = Vector3d(targetPos.x.toDouble(), targetPos.y.toDouble(), targetPos.z.toDouble())
        val srcObj = mSourceObject ?: return globalPos
        return globalPos + srcObj.getPositionGlobal()
    }

    override fun markDead() {
        val srcObj = mSourceObject
        if (srcObj != null && srcObj.isAvatar()) {
            TODO("APR: call srcObj.removeAnimationData('PointAtPoint')")
        }
        clearPointAtTarget()
        super.markDead()
    }

    override fun setSourceObject(objectp: ViewerObject?) {
        if (objectp != null && objectp.isAvatar()) {
            super.setSourceObject(objectp)
        }
    }

    fun clearPointAtTarget() {
        mTargetObject = null
        targetOffsetGlobal = Vector3d.ZERO
        targetType = PointAtType.NONE
    }

    fun setPointAt(targetTypeParm: PointAtType, obj: ViewerObject?, position: Vector3): Boolean {
        if (mSourceObject == null) return false

        if (targetTypeParm.ordinal >= PointAtType.values().size) {
            System.err.println("WARN: Bad target_type ${targetTypeParm.ordinal} - ignoring.")
            return false
        }

        if ((POINTAT_PRIORITIES[targetTypeParm] ?: 0) < (POINTAT_PRIORITIES[targetType] ?: 0)) {
            return false
        }

        val currentTime = elapsed()
        val targetTypeChanged = (targetTypeParm != targetType) || (obj !== mTargetObject)
        val targetPosChanged = (distVecSquared(position, lastSentOffsetGlobal) > MIN_DELTAPOS_FOR_UPDATE_SQUARED) &&
            ((currentTime - lastSendTime) > (1f / MAX_SENDS_PER_SEC))

        if (targetTypeChanged || targetPosChanged) {
            lastSentOffsetGlobal = position
            setDuration(POINTAT_TIMEOUTS[targetTypeParm] ?: 0f)
            setNeedsSendToSim(true)
        }

        if (targetTypeParm == PointAtType.CLEAR) {
            clearPointAtTarget()
        } else {
            targetType = targetTypeParm
            mTargetObject = obj
            targetOffsetGlobal = if (obj != null) {
                Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
            } else {
                TODO("APR: convert agent position to global coords")
            }
            killTime = elapsed() + mDuration
            update()
        }
        return true
    }

    private fun setTargetObjectAndOffset(objp: ViewerObject, offset: Vector3d) {
        mTargetObject = objp
        targetOffsetGlobal = offset
    }

    private fun setTargetPosGlobal(targetPosGlobal: Vector3d) {
        mTargetObject = null
        targetOffsetGlobal = targetPosGlobal
    }

    override fun packData(mesgsys: Any) {
        val sourceObj = mSourceObject
        if (sourceObj == null) { markDead(); return }
        if (!sourceObj.isAvatar()) { markDead(); return }

        val isSelf: Boolean = TODO("APR: check if sourceObj is self avatar")
        @Suppress("UNREACHABLE_CODE")
        if (!isSelf) { markDead(); return }

        super.packData(mesgsys)

        TODO("APR: pack PKT_SIZE binary blob into mesgsys TypeData: " +
             "source UUID @ SOURCE_AVATAR, target UUID @ TARGET_OBJECT, " +
             "targetOffsetGlobal @ TARGET_POS, targetType ordinal @ POINTAT_TYPE")

        lastSendTime = elapsed()
    }

    override fun unpackData(mesgsys: Any, blocknum: Int) {
        val dataId: UUID = TODO("APR: read UUID from Effect/ID field in block $blocknum")
        @Suppress("UNREACHABLE_CODE")
        val ownPointAt: HUDEffectPointAt? = TODO("APR: get gAgentCamera.mPointAt")
        @Suppress("UNREACHABLE_CODE")
        if (ownPointAt != null && dataId == ownPointAt.getID()) return

        super.unpackData(mesgsys, blocknum)

        TODO("APR: unpack binary blob: " +
             "sourceId @ SOURCE_AVATAR, targetId @ TARGET_OBJECT, " +
             "new_target Vector3d @ TARGET_POS, pointAtType U8 @ POINTAT_TYPE; " +
             "find source object; call setSourceObject; " +
             "find target object; call setTargetObjectAndOffset or setTargetPosGlobal; " +
             "set targetType; call update()")
    }

    fun update() {
        val tgtObj = mTargetObject
        if (tgtObj != null && tgtObj.isDead()) clearPointAtTarget()

        val srcObj = mSourceObject
        if (srcObj == null || srcObj.isDead()) { markDead(); return }

        val time = elapsed()
        if (killTime != 0f && time > killTime) {
            targetType = PointAtType.NONE
        }

        if (srcObj.isAvatar()) {
            if (targetType == PointAtType.NONE) {
                TODO("APR: call srcObj.removeAnimationData('PointAtPoint')")
            } else {
                if (calcTargetPosition()) {
                    TODO("APR: call srcObj.startMotion(ANIM_AGENT_EDITING)")
                }
            }
        }
    }

    override fun render() {
        update()
        if (!debugPointAt || targetType == PointAtType.NONE) return

        TODO("GPU: render red crosshair lines at (targetPos + sourceObject renderPosition); " +
             "scale 0.3; use gGL lines with color (1,0,0)")
    }

    fun calcTargetPosition(): Boolean {
        val targetObj = mTargetObject
        val localOffset: Vector3 = if (targetObj != null) {
            Vector3(targetOffsetGlobal.x.toFloat(), targetOffsetGlobal.y.toFloat(), targetOffsetGlobal.z.toFloat())
        } else {
            TODO("APR: convert targetOffsetGlobal from global to agent coords")
        }

        if (targetObj != null && targetObj.mDrawable != null) {
            val objRot: Quaternion
            if (targetObj.isAvatar()) {
                targetPos = TODO("APR: get avatar head world position")
                objRot = TODO("APR: get avatar pelvis world rotation")
            } else {
                val generation: Int = TODO("APR: get drawable generation")
                @Suppress("UNREACHABLE_CODE")
                if (generation == -1) {
                    targetPos = targetObj.getPositionAgent()
                    objRot = targetObj.getWorldRotation()
                } else {
                    targetPos = targetObj.getRenderPosition()
                    objRot = targetObj.getRenderRotation()
                }
            }
            targetPos = TODO("APR: targetPos + (localOffset * objRot)")
        } else {
            targetPos = localOffset
        }

        val srcRenderPos: Vector3 = mSourceObject?.getRenderPosition() ?: Vector3.ZERO
        targetPos = targetPos - srcRenderPos

        val lenSq = targetPos.x * targetPos.x + targetPos.y * targetPos.y + targetPos.z * targetPos.z
        if (!lenSq.isFinite()) return false

        if (mSourceObject?.isAvatar() == true) {
            TODO("APR: call sourceObject.setAnimationData('PointAtPoint', targetPos)")
        }

        return true
    }
}

// ---------------------------------------------------------------------------
// Distance helper
// ---------------------------------------------------------------------------

private fun distVecSquared(a: Vector3, b: Vector3): Float {
    val dx = a.x - b.x; val dy = a.y - b.y; val dz = a.z - b.z
    return dx * dx + dy * dy + dz * dz
}
