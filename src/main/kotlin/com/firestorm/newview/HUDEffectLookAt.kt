package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// LookAt target types
// ---------------------------------------------------------------------------

enum class LookAtType {
    NONE,
    IDLE,
    AUTO_LISTEN,
    FREELOOK,
    RESPOND,
    HOVER,
    CONVERSATION,
    SELECT,
    FOCUS,
    MOUSELOOK,
    CLEAR;
}

// ---------------------------------------------------------------------------
// Attention data – timeout and priority for each lookat type
// ---------------------------------------------------------------------------

data class Attention(
    val timeout: Float,
    val priority: Float,
    val name: String,
    val color: Color3
)

class AttentionSet(attentions: Array<Attention>) {
    private val data: Array<Attention> = attentions.copyOf()
    operator fun get(type: LookAtType): Attention = data[type.ordinal]
}

private val MAX_TIMEOUT: Float = Float.MAX_VALUE / 2f

private val BOY_ATTENTIONS = AttentionSet(arrayOf(
    Attention(MAX_TIMEOUT, 0f, "None",         Color3(0.3f, 0.3f, 0.3f)),
    Attention(3f,          1f, "Idle",          Color3(0.5f, 0.5f, 0.5f)),
    Attention(4f,          3f, "AutoListen",    Color3(0.5f, 0.5f, 0.5f)),
    Attention(2f,          2f, "FreeLook",      Color3(0.5f, 0.5f, 0.9f)),
    Attention(4f,          3f, "Respond",       Color3(0.0f, 0.0f, 0.0f)),
    Attention(1f,          4f, "Hover",         Color3(0.5f, 0.9f, 0.5f)),
    Attention(MAX_TIMEOUT, 0f, "Conversation",  Color3(0.1f, 0.1f, 0.5f)),
    Attention(MAX_TIMEOUT, 6f, "Select",        Color3(0.9f, 0.5f, 0.5f)),
    Attention(MAX_TIMEOUT, 6f, "Focus",         Color3(0.9f, 0.5f, 0.9f)),
    Attention(MAX_TIMEOUT, 7f, "Mouselook",     Color3(0.9f, 0.9f, 0.5f)),
    Attention(0f,          8f, "Clear",         Color3(1.0f, 1.0f, 1.0f)),
))

private val GIRL_ATTENTIONS = AttentionSet(arrayOf(
    Attention(MAX_TIMEOUT, 0f, "None",         Color3(0.3f, 0.3f, 0.3f)),
    Attention(3f,          1f, "Idle",          Color3(0.5f, 0.5f, 0.5f)),
    Attention(4f,          3f, "AutoListen",    Color3(0.5f, 0.5f, 0.5f)),
    Attention(2f,          2f, "FreeLook",      Color3(0.5f, 0.5f, 0.9f)),
    Attention(4f,          3f, "Respond",       Color3(0.0f, 0.0f, 0.0f)),
    Attention(1f,          4f, "Hover",         Color3(0.5f, 0.9f, 0.5f)),
    Attention(MAX_TIMEOUT, 0f, "Conversation",  Color3(0.1f, 0.1f, 0.5f)),
    Attention(MAX_TIMEOUT, 6f, "Select",        Color3(0.9f, 0.5f, 0.5f)),
    Attention(MAX_TIMEOUT, 6f, "Focus",         Color3(0.9f, 0.5f, 0.9f)),
    Attention(MAX_TIMEOUT, 7f, "Mouselook",     Color3(0.9f, 0.9f, 0.5f)),
    Attention(0f,          8f, "Clear",         Color3(1.0f, 1.0f, 1.0f)),
))

// Packet layout constants (byte offsets in the binary TypeData blob)
private const val SOURCE_AVATAR = 0
private const val TARGET_OBJECT = 16
private const val TARGET_POS    = 32
private const val LOOKAT_TYPE   = 56
private const val PKT_SIZE      = 57

private const val MAX_SENDS_PER_SEC: Float              = 4f
private const val MIN_DELTAPOS_FOR_UPDATE_SQUARED: Float = 0.05f * 0.05f
private const val MIN_TARGET_OFFSET_SQUARED: Float       = 0.0001f

// ---------------------------------------------------------------------------
// HUDEffectLookAt
// ---------------------------------------------------------------------------

class HUDEffectLookAt(type: UByte) : HUDEffect(type) {

    private var targetType: LookAtType = LookAtType.NONE
    private var targetOffsetGlobal: Vector3d = Vector3d.ZERO
    private var lastSentOffsetGlobal: Vector3 = Vector3.ZERO
    private var killTime: Float = 0f
    private var timerStartNs: Long = System.nanoTime()
    private var targetPos: Vector3 = Vector3.ZERO
    private var lastSendTime: Float = 0f
    private var attentions: AttentionSet = GIRL_ATTENTIONS

    // Mirrors FS LLCachedControl<S32> mDebugLookAt – 0 = off
    var debugLookAt: Int = TODO("APR: read 'DebugLookAt' from saved per-account settings")

    init {
        clearLookAtTarget()
        loadAttentions()
    }

    private fun elapsed(): Float = ((System.nanoTime() - timerStartNs) / 1_000_000_000.0).toFloat()

    fun getLookAtType(): LookAtType = targetType
    fun getTargetPos(): Vector3 = targetPos
    fun getTargetOffset(): Vector3d = targetOffsetGlobal

    override fun markDead() {
        mSourceObject?.let {
            TODO("APR: remove animation data 'LookAtPoint' from source avatar")
        }
        mSourceObject = null
        clearLookAtTarget()
        super.markDead()
    }

    override fun setSourceObject(objectp: ViewerObject?) {
        if (objectp != null && objectp.isAvatar()) {
            super.setSourceObject(objectp)
        }
    }

    fun clearLookAtTarget() {
        mTargetObject = null
        targetOffsetGlobal = Vector3d.ZERO
        targetType = LookAtType.NONE
        mSourceObject?.let {
            TODO("APR: stop ANIM_AGENT_HEAD_ROT motion on source avatar")
        }
    }

    fun setLookAt(targetTypeParm: LookAtType, obj: ViewerObject?, position: Vector3): Boolean {
        if (mSourceObject == null) return false

        if (targetTypeParm.ordinal >= LookAtType.values().size) {
            System.err.println("WARN: Bad target_type ${targetTypeParm.ordinal} - ignoring.")
            return false
        }

        if (attentions[targetTypeParm].priority < attentions[targetType].priority) return false

        val currentTime = elapsed()

        val fsLimitEnabled: Boolean = TODO("APR: read 'FSLookAtTargetLimitDistance' from settings")
        @Suppress("UNREACHABLE_CODE")
        val lookAtShouldClamp = fsLimitEnabled &&
            attentions[targetType].name !in setOf("None", "Idle", "Respond", "Conversation", "FreeLook", "AutoListen")

        if (!lookAtShouldClamp) {
            val lookAtChanged = (targetTypeParm != targetType) || (obj !== mTargetObject) ||
                ((distVecSquared(position, lastSentOffsetGlobal) > MIN_DELTAPOS_FOR_UPDATE_SQUARED) &&
                    ((currentTime - lastSendTime) > (1f / MAX_SENDS_PER_SEC)))
            if (lookAtChanged) {
                lastSentOffsetGlobal = position
                setDuration(attentions[targetTypeParm].timeout)
                setNeedsSendToSim(true)
            }
        }

        if (targetTypeParm == LookAtType.CLEAR) {
            clearLookAtTarget()
        } else {
            targetType = targetTypeParm

            if (obj != null) {
                if (lookAtShouldClamp) {
                    if (obj.isAvatar()) {
                        val isSelf: Boolean = TODO("APR: check if obj is self avatar")
                        @Suppress("UNREACHABLE_CODE")
                        if (!isSelf) {
                            val headPos: Vector3 = TODO("APR: get avatar head world position")
                            @Suppress("UNREACHABLE_CODE")
                            targetOffsetGlobal = TODO("APR: convert headPos to global coords")
                            @Suppress("UNREACHABLE_CODE")
                            mTargetObject = null
                        } else {
                            targetOffsetGlobal = Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
                        }
                    } else {
                        targetOffsetGlobal = TODO("APR: compute global offset from obj position + position * objRotation")
                        @Suppress("UNREACHABLE_CODE")
                        mTargetObject = null
                    }
                } else {
                    mTargetObject = obj
                    targetOffsetGlobal = Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
                }
            } else {
                mTargetObject = null
                targetOffsetGlobal = TODO("APR: convert agent position to global coords")
            }

            if (lookAtShouldClamp && mTargetObject == null) {
                val maxRadius: Float = TODO("APR: read 'FSLookAtTargetMaxDistance' from settings")
                @Suppress("UNREACHABLE_CODE")
                val headPosGlobal: Vector3d = TODO("APR: get agent head position in global coords")
                @Suppress("UNREACHABLE_CODE")
                val distance: Float = distVec(targetOffsetGlobal, headPosGlobal)
                if (distance > maxRadius) {
                    val vec = (targetOffsetGlobal - headPosGlobal) * (maxRadius / distance).toDouble()
                    targetOffsetGlobal = headPosGlobal + vec
                }

                val lookAtChanged = (targetTypeParm != targetType) ||
                    ((distVecSquared(
                        TODO("APR: convert targetOffsetGlobal to agent coords"),
                        lastSentOffsetGlobal
                    ) > MIN_DELTAPOS_FOR_UPDATE_SQUARED) &&
                        ((currentTime - lastSendTime) > (1f / MAX_SENDS_PER_SEC)))
                if (lookAtChanged) {
                    lastSentOffsetGlobal = TODO("APR: convert targetOffsetGlobal to agent coords")
                    setDuration(attentions[targetTypeParm].timeout)
                    setNeedsSendToSim(true)
                }
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

        val isPrivate: Boolean = TODO("APR: read 'PrivateLookAtTarget' from settings")
        @Suppress("UNREACHABLE_CODE")
        val isLocalPrivate: Boolean = TODO("APR: read 'PrivateLocalLookAtTarget' from settings")
        @Suppress("UNREACHABLE_CODE")
        if (isLocalPrivate && isPrivate) { markDead(); return }

        var effectiveType = targetType
        var effectiveOffset = targetOffsetGlobal
        var effectiveTarget = mTargetObject

        if (isPrivate && targetType != LookAtType.AUTO_LISTEN) {
            effectiveType = LookAtType.AUTO_LISTEN
            effectiveOffset = Vector3d(2.5, 0.0, 0.0)
            effectiveTarget = mSourceObject
        }

        super.packData(mesgsys)

        TODO("APR: pack PKT_SIZE binary blob into mesgsys TypeData: " +
             "source UUID @ SOURCE_AVATAR, target UUID @ TARGET_OBJECT, " +
             "effectiveOffset @ TARGET_POS, effectiveType ordinal @ LOOKAT_TYPE")

        lastSendTime = elapsed()
    }

    override fun unpackData(mesgsys: Any, blocknum: Int) {
        val dataId: UUID = TODO("APR: read UUID from Effect/ID field in block $blocknum")
        @Suppress("UNREACHABLE_CODE")
        val ownLookAt: HUDEffectLookAt? = TODO("APR: get gAgentCamera.mLookAt")
        @Suppress("UNREACHABLE_CODE")
        if (ownLookAt != null && dataId == ownLookAt.getID()) return

        super.unpackData(mesgsys, blocknum)

        TODO("APR: unpack binary blob: " +
             "sourceId @ SOURCE_AVATAR, targetId @ TARGET_OBJECT, " +
             "new_target Vector3d @ TARGET_POS, lookAtType U8 @ LOOKAT_TYPE; " +
             "find source/target objects in gObjectList; " +
             "call setTargetObjectAndOffset or setTargetPosGlobal; " +
             "set targetType; call clearLookAtTarget if NONE")
    }

    override fun update() {
        val tgtObj = mTargetObject
        if (tgtObj != null && tgtObj.isDead()) clearLookAtTarget()

        val srcObj = mSourceObject
        if (srcObj == null || srcObj.isDead()) { markDead(); return }

        val isMale: Boolean = TODO("APR: check source avatar sex (SEX_MALE)")
        @Suppress("UNREACHABLE_CODE")
        attentions = if (isMale) BOY_ATTENTIONS else GIRL_ATTENTIONS

        val time = elapsed()
        if (killTime != 0f && time > killTime && targetType != LookAtType.NONE) {
            clearLookAtTarget()
            setNeedsSendToSim(true)
        }

        if (targetType != LookAtType.NONE) {
            if (calcTargetPosition()) {
                val disableLookAt: Boolean = TODO("APR: read 'DisableLookAtAnimation' from settings")
                @Suppress("UNREACHABLE_CODE")
                if (disableLookAt) {
                    TODO("APR: stop ANIM_AGENT_HEAD_ROT motion on source avatar")
                } else {
                    TODO("APR: start ANIM_AGENT_HEAD_ROT motion on source avatar if stopped")
                }
            }
        }
    }

    override fun render() {
        if (debugLookAt == 0 || mSourceObject == null) return
        val hideOwn: Boolean = TODO("APR: read 'DebugLookAtHideOwn' from per-account settings")
        @Suppress("UNREACHABLE_CODE")
        val isPrivate: Boolean = TODO("APR: read 'PrivateLookAtTarget' from settings")
        @Suppress("UNREACHABLE_CODE")
        val isSelf: Boolean = TODO("APR: check if mSourceObject is self avatar")
        @Suppress("UNREACHABLE_CODE")
        if ((hideOwn || isPrivate) && isSelf) return

        TODO("GPU: render crosshair lines at targetPos + sourceAvatar head position; " +
             "optionally draw line back to source object (ExodusLookAtLines setting); " +
             "optionally render avatar name label (DebugLookAtShowNames setting)")
    }

    fun calcTargetPosition(): Boolean {
        val targetObj = mTargetObject
        val localOffset: Vector3 = if (targetObj != null) {
            Vector3(targetOffsetGlobal.x.toFloat(), targetOffsetGlobal.y.toFloat(), targetOffsetGlobal.z.toFloat())
        } else {
            TODO("APR: convert targetOffsetGlobal from global to agent coords")
        }

        val sourceAvatar = mSourceObject ?: return false
        val isBuilt: Boolean = TODO("APR: check source avatar isBuilt()")
        @Suppress("UNREACHABLE_CODE")
        if (!isBuilt) return false

        if (targetObj != null) {
            val drawable = targetObj.mDrawable
            if (drawable != null) {
                val targetRot: Quaternion
                if (targetObj.isAvatar()) {
                    val isSelfLookingSelf: Boolean = TODO("APR: check both source and target are self")
                    @Suppress("UNREACHABLE_CODE")
                    if (isSelfLookingSelf && targetOffsetGlobal.magnitudeSquared() < MIN_TARGET_OFFSET_SQUARED) {
                        targetOffsetGlobal = Vector3d(1.0, 0.0, 0.0)
                    }
                    targetPos = TODO("APR: get target avatar head world position")
                    targetRot = when (targetType) {
                        LookAtType.MOUSELOOK, LookAtType.FREELOOK -> Quaternion.DEFAULT
                        else -> TODO("APR: get appropriate root/pelvis world rotation from target avatar")
                    }
                } else {
                    val generation: Int = TODO("APR: get drawable generation")
                    @Suppress("UNREACHABLE_CODE")
                    if (generation == -1) {
                        targetPos = targetObj.getPositionAgent()
                        targetRot = targetObj.getWorldRotation()
                    } else {
                        targetPos = targetObj.getRenderPosition()
                        targetRot = targetObj.getRenderRotation()
                    }
                }
                targetPos = TODO("APR: targetPos + (localOffset * targetRot)")
            } else {
                targetPos = localOffset
            }
        } else {
            targetPos = localOffset
        }

        val headPos: Vector3 = TODO("APR: get source avatar head world position")
        @Suppress("UNREACHABLE_CODE")
        targetPos = targetPos - headPos

        if (!targetPos.isFinite()) return false

        val disableLookAt: Boolean = TODO("APR: read 'DisableLookAtAnimation' from settings")
        @Suppress("UNREACHABLE_CODE")
        if (disableLookAt) {
            TODO("APR: call sourceAvatar.removeAnimationData('LookAtPoint')")
        } else {
            TODO("APR: call sourceAvatar.setAnimationData('LookAtPoint', targetPos)")
        }

        return true
    }

    companion object {
        private var attentionsLoaded = false

        private fun loadAttentions() {
            if (attentionsLoaded) return
            attentionsLoaded = true
            TODO("APR: parse attentions.xml from LL_PATH_CHARACTER directory; " +
                 "override BOY_ATTENTIONS and GIRL_ATTENTIONS timeout/priority values")
        }
    }
}

// ---------------------------------------------------------------------------
// Small distance helpers used in this file
// ---------------------------------------------------------------------------

private fun distVecSquared(a: Vector3, b: Vector3): Float {
    val dx = a.x - b.x; val dy = a.y - b.y; val dz = a.z - b.z
    return dx * dx + dy * dy + dz * dz
}

private fun distVec(a: Vector3d, b: Vector3d): Float {
    val dx = a.x - b.x; val dy = a.y - b.y; val dz = a.z - b.z
    return kotlin.math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
}
