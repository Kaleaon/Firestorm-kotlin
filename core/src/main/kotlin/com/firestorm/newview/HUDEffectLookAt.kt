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
    var debugLookAt: Int = 0 // APR: read 'DebugLookAt' from saved per-account settings

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
            System.err.println("STUB: APR: remove animation data 'LookAtPoint' from source avatar")
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
            System.err.println("STUB: APR: stop ANIM_AGENT_HEAD_ROT motion on source avatar")
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

        val fsLimitEnabled: Boolean = false // APR: read 'FSLookAtTargetLimitDistance' from settings
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
                        val isSelf: Boolean = false // APR: check if obj is self avatar
                        if (!isSelf) {
                            val headPos: Vector3 = Vector3.ZERO // APR: get avatar head world position
                            targetOffsetGlobal = Vector3d(headPos.x.toDouble(), headPos.y.toDouble(), headPos.z.toDouble()) // APR: convert headPos to global coords
                            mTargetObject = null
                        } else {
                            targetOffsetGlobal = Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
                        }
                    } else {
                        targetOffsetGlobal = Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble()) // APR: compute global offset from obj position + position * objRotation
                        mTargetObject = null
                    }
                } else {
                    mTargetObject = obj
                    targetOffsetGlobal = Vector3d(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
                }
            } else {
                mTargetObject = null
                targetOffsetGlobal = Vector3d.ZERO // APR: convert agent position to global coords
            }

            if (lookAtShouldClamp && mTargetObject == null) {
                val maxRadius: Float = 0f // APR: read 'FSLookAtTargetMaxDistance' from settings
                val headPosGlobal: Vector3d = Vector3d.ZERO // APR: get agent head position in global coords
                val distance: Float = distVec(targetOffsetGlobal, headPosGlobal)
                if (distance > maxRadius) {
                    val vec = (targetOffsetGlobal - headPosGlobal) * (maxRadius / distance).toDouble()
                    targetOffsetGlobal = headPosGlobal + vec
                }

                val lookAtChanged = (targetTypeParm != targetType) ||
                    ((distVecSquared(
                        Vector3.ZERO, // APR: convert targetOffsetGlobal to agent coords
                        lastSentOffsetGlobal
                    ) > MIN_DELTAPOS_FOR_UPDATE_SQUARED) &&
                        ((currentTime - lastSendTime) > (1f / MAX_SENDS_PER_SEC)))
                if (lookAtChanged) {
                    lastSentOffsetGlobal = Vector3.ZERO // APR: convert targetOffsetGlobal to agent coords
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

        val isSelf: Boolean = false // APR: check if sourceObj is self avatar
        if (!isSelf) { markDead(); return }

        val isPrivate: Boolean = false // APR: read 'PrivateLookAtTarget' from settings
        val isLocalPrivate: Boolean = false // APR: read 'PrivateLocalLookAtTarget' from settings
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

        // APR: pack PKT_SIZE binary blob into mesgsys TypeData:
        // source UUID @ SOURCE_AVATAR, target UUID @ TARGET_OBJECT,
        // effectiveOffset @ TARGET_POS, effectiveType ordinal @ LOOKAT_TYPE
        System.err.println("STUB: APR: packData binary blob not implemented")

        lastSendTime = elapsed()
    }

    override fun unpackData(mesgsys: Any, blocknum: Int) {
        val dataId: UUID = UUID(0, 0) // APR: read UUID from Effect/ID field in block $blocknum
        val ownLookAt: HUDEffectLookAt? = null // APR: get gAgentCamera.mLookAt
        if (ownLookAt != null && dataId == ownLookAt.getID()) return

        super.unpackData(mesgsys, blocknum)

        // APR: unpack binary blob:
        // sourceId @ SOURCE_AVATAR, targetId @ TARGET_OBJECT,
        // new_target Vector3d @ TARGET_POS, lookAtType U8 @ LOOKAT_TYPE;
        // find source/target objects in gObjectList;
        // call setTargetObjectAndOffset or setTargetPosGlobal;
        // set targetType; call clearLookAtTarget if NONE
        System.err.println("STUB: APR: unpackData binary blob not implemented")
    }

    override fun update() {
        val tgtObj = mTargetObject
        if (tgtObj != null && tgtObj.isDead()) clearLookAtTarget()

        val srcObj = mSourceObject
        if (srcObj == null || srcObj.isDead()) { markDead(); return }

        val isMale: Boolean = false // APR: check source avatar sex (SEX_MALE)
        attentions = if (isMale) BOY_ATTENTIONS else GIRL_ATTENTIONS

        val time = elapsed()
        if (killTime != 0f && time > killTime && targetType != LookAtType.NONE) {
            clearLookAtTarget()
            setNeedsSendToSim(true)
        }

        if (targetType != LookAtType.NONE) {
            if (calcTargetPosition()) {
                val disableLookAt: Boolean = false // APR: read 'DisableLookAtAnimation' from settings
                if (disableLookAt) {
                    System.err.println("STUB: APR: stop ANIM_AGENT_HEAD_ROT motion on source avatar")
                } else {
                    System.err.println("STUB: APR: start ANIM_AGENT_HEAD_ROT motion on source avatar if stopped")
                }
            }
        }
    }

    override fun render() {
        if (debugLookAt == 0 || mSourceObject == null) return
        val hideOwn: Boolean = false // APR: read 'DebugLookAtHideOwn' from per-account settings
        val isPrivate: Boolean = false // APR: read 'PrivateLookAtTarget' from settings
        val isSelf: Boolean = false // APR: check if mSourceObject is self avatar
        if ((hideOwn || isPrivate) && isSelf) return

        // GPU: render crosshair lines at targetPos + sourceAvatar head position;
        // optionally draw line back to source object (ExodusLookAtLines setting);
        // optionally render avatar name label (DebugLookAtShowNames setting)
    }

    fun calcTargetPosition(): Boolean {
        val targetObj = mTargetObject
        val localOffset: Vector3 = if (targetObj != null) {
            Vector3(targetOffsetGlobal.x.toFloat(), targetOffsetGlobal.y.toFloat(), targetOffsetGlobal.z.toFloat())
        } else {
            Vector3.ZERO // APR: convert targetOffsetGlobal from global to agent coords
        }

        val sourceAvatar = mSourceObject ?: return false
        val isBuilt: Boolean = false // APR: check source avatar isBuilt()
        if (!isBuilt) return false

        if (targetObj != null) {
            val drawable = targetObj.mDrawable
            if (drawable != null) {
                val targetRot: Quaternion
                if (targetObj.isAvatar()) {
                    val isSelfLookingSelf: Boolean = false // APR: check both source and target are self
                    if (isSelfLookingSelf && targetOffsetGlobal.magnitudeSquared() < MIN_TARGET_OFFSET_SQUARED) {
                        targetOffsetGlobal = Vector3d(1.0, 0.0, 0.0)
                    }
                    targetPos = Vector3.ZERO // APR: get target avatar head world position
                    targetRot = when (targetType) {
                        LookAtType.MOUSELOOK, LookAtType.FREELOOK -> Quaternion.DEFAULT
                        else -> Quaternion.DEFAULT // APR: get appropriate root/pelvis world rotation from target avatar
                    }
                } else {
                    val generation: Int = 0 // APR: get drawable generation
                    if (generation == -1) {
                        targetPos = targetObj.getPositionAgent()
                        targetRot = targetObj.getWorldRotation()
                    } else {
                        targetPos = targetObj.getRenderPosition()
                        targetRot = targetObj.getRenderRotation()
                    }
                }
                targetPos = Vector3.ZERO // APR: targetPos + (localOffset * targetRot)
            } else {
                targetPos = localOffset
            }
        } else {
            targetPos = localOffset
        }

        val headPos: Vector3 = Vector3.ZERO // APR: get source avatar head world position
        targetPos = targetPos - headPos

        if (!targetPos.isFinite()) return false

        val disableLookAt: Boolean = false // APR: read 'DisableLookAtAnimation' from settings
        if (disableLookAt) {
            System.err.println("STUB: APR: call sourceAvatar.removeAnimationData('LookAtPoint')")
        } else {
            System.err.println("STUB: APR: call sourceAvatar.setAnimationData('LookAtPoint', targetPos)")
        }

        return true
    }

    companion object {
        private var attentionsLoaded = false

        private fun loadAttentions() {
            if (attentionsLoaded) return
            attentionsLoaded = true
            // APR: parse attentions.xml from LL_PATH_CHARACTER directory;
            // override BOY_ATTENTIONS and GIRL_ATTENTIONS timeout/priority values
            System.err.println("STUB: APR: loadAttentions from attentions.xml not implemented")
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
