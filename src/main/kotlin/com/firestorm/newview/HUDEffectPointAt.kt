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
            System.err.println("HUDEffectPointAt: markDead removeAnimationData not yet implemented")
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
                System.err.println("HUDEffectPointAt: setPointAt agent position to global coords not yet implemented")
                Vector3d.ZERO
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

        System.err.println("HUDEffectPointAt: packData isSelf check not yet implemented")
        val isSelf: Boolean = false
        if (!isSelf) { markDead(); return }

        super.packData(mesgsys)

        System.err.println("HUDEffectPointAt: packData binary blob packing not yet implemented")

        lastSendTime = elapsed()
    }

    override fun unpackData(mesgsys: Any, blocknum: Int) {
        System.err.println("HUDEffectPointAt: unpackData UUID read not yet implemented")
        val dataId: UUID = java.util.UUID.randomUUID()
        System.err.println("HUDEffectPointAt: unpackData gAgentCamera.mPointAt not yet implemented")
        val ownPointAt: HUDEffectPointAt? = null
        if (ownPointAt != null && dataId == ownPointAt.getID()) return

        super.unpackData(mesgsys, blocknum)

        System.err.println("HUDEffectPointAt: unpackData binary blob unpacking not yet implemented")
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
                System.err.println("HUDEffectPointAt: update removeAnimationData not yet implemented")
            } else {
                if (calcTargetPosition()) {
                    System.err.println("HUDEffectPointAt: update startMotion ANIM_AGENT_EDITING not yet implemented")
                }
            }
        }
    }

    override fun render() {
        update()
        if (!debugPointAt || targetType == PointAtType.NONE) return

        // no-op
    }

    fun calcTargetPosition(): Boolean {
        val targetObj = mTargetObject
        val localOffset: Vector3 = if (targetObj != null) {
            Vector3(targetOffsetGlobal.x.toFloat(), targetOffsetGlobal.y.toFloat(), targetOffsetGlobal.z.toFloat())
        } else {
            System.err.println("HUDEffectPointAt: calcTargetPosition global-to-agent coords not yet implemented")
            Vector3.ZERO
        }

        if (targetObj != null && targetObj.mDrawable != null) {
            val objRot: Quaternion
            if (targetObj.isAvatar()) {
                System.err.println("HUDEffectPointAt: calcTargetPosition avatar head position not yet implemented")
                targetPos = Vector3.ZERO
                System.err.println("HUDEffectPointAt: calcTargetPosition avatar pelvis rotation not yet implemented")
                objRot = Quaternion.IDENTITY
            } else {
                System.err.println("HUDEffectPointAt: calcTargetPosition drawable generation not yet implemented")
                val generation: Int = -1
                if (generation == -1) {
                    targetPos = targetObj.getPositionAgent()
                    objRot = targetObj.getWorldRotation()
                } else {
                    targetPos = targetObj.getRenderPosition()
                    objRot = targetObj.getRenderRotation()
                }
            }
            System.err.println("HUDEffectPointAt: calcTargetPosition targetPos + (localOffset * objRot) not yet implemented")
            // targetPos already set above; leave as-is
        } else {
            targetPos = localOffset
        }

        val srcRenderPos: Vector3 = mSourceObject?.getRenderPosition() ?: Vector3.ZERO
        targetPos = targetPos - srcRenderPos

        val lenSq = targetPos.x * targetPos.x + targetPos.y * targetPos.y + targetPos.z * targetPos.z
        if (!lenSq.isFinite()) return false

        if (mSourceObject?.isAvatar() == true) {
            System.err.println("HUDEffectPointAt: calcTargetPosition setAnimationData not yet implemented")
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
