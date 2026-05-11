package com.firestorm.newview

import java.util.UUID

private const val TARGET_OBJECT = 0
private const val RESET_ANIMATIONS = 16
private const val PKT_SIZE = 17

class LLHUDEffectResetSkeleton(type: UByte) : LLHUDEffect(type) {

    private var mResetAnimations: Boolean = false

    override fun markDead() {
        super.markDead()
    }

    override fun setSourceObject(objectp: LLViewerObject?) {
        if (objectp != null && objectp.isAvatar()) {
            super.setSourceObject(objectp)
        }
    }

    override fun setTargetObject(objp: LLViewerObject?) {
        mTargetObject = objp
    }

    fun setResetAnimations(enable: Boolean) {
        mResetAnimations = enable
    }

    override fun render() {
        // This effect is a no-op during render; all work happens in update().
    }

    override fun packData(mesgsys: LLMessageSystem) {
        super.packData(mesgsys)

        val packedData = ByteArray(PKT_SIZE)

        val targetId = mTargetObject?.mID ?: UUID(0L, 0L)
        TODO("APR: use JVM equivalent - htolememcpy targetId bytes into packedData[TARGET_OBJECT..TARGET_OBJECT+16]")

        val resetAnimByte: Byte = if (mResetAnimations) 1 else 0
        packedData[RESET_ANIMATIONS] = resetAnimByte

        TODO("APR: use JVM equivalent - mesgsys.addBinaryDataFast(_PREHASH_TypeData, packedData, PKT_SIZE)")
    }

    override fun unpackData(mesgsys: LLMessageSystem, blocknum: Int) {
        super.unpackData(mesgsys, blocknum)

        TODO("APR: use JVM equivalent - mesgsys.getUUIDFast sourceId from _PREHASH_Effect / _PREHASH_AgentID at blocknum")

        val size: Int = TODO("APR: use JVM equivalent - mesgsys.getSizeFast _PREHASH_Effect blocknum _PREHASH_TypeData")
        if (size != PKT_SIZE) {
            return
        }

        val packedData = ByteArray(PKT_SIZE)
        TODO("APR: use JVM equivalent - mesgsys.getBinaryDataFast packedData PKT_SIZE blocknum")

        // If no explicit target, the source resets itself.
        // targetId null → use sourceId. See unpackData comment in C++ regarding animesh permission concerns.
        TODO("APR: use JVM equivalent - resolve targetId UUID from packedData, fall back to sourceId if null")

        val resetAnimByte = packedData[RESET_ANIMATIONS]
        // Only bit 0 is currently meaningful; treat remaining bits as reserved flags for future use.
        mResetAnimations = (resetAnimByte.toInt() and 1) != 0

        update()
    }

    override fun update() {
        if (mTargetObject == null || mTargetObject!!.isDead()) {
            markDead()
            return
        }
        if (mSourceObject == null || mSourceObject!!.isDead()) {
            markDead()
            return
        }

        if (mTargetObject!!.isAvatar()) {
            if (mSourceObject!!.getID() == mTargetObject!!.getID() || getOriginatedHere()) {
                val avatar = mTargetObject!!.asAvatar()
                avatar?.resetSkeleton(mResetAnimations)
            }
        } else {
            TODO("APR: use JVM equivalent - log warning: sourceObject attempted to reset skeleton on non-avatar targetObject")
        }

        markDead()
    }
}
