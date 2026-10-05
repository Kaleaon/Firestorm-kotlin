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
        System.err.println("LLHUDEffectResetSkeleton: packData not yet implemented")

        val resetAnimByte: Byte = if (mResetAnimations) 1 else 0
        packedData[RESET_ANIMATIONS] = resetAnimByte

        System.err.println("LLHUDEffectResetSkeleton: packData not yet implemented")
    }

    override fun unpackData(mesgsys: LLMessageSystem, blocknum: Int) {
        super.unpackData(mesgsys, blocknum)

        System.err.println("LLHUDEffectResetSkeleton: unpackData not yet implemented")

        val size: Int = 0
        if (size != PKT_SIZE) {
            return
        }

        val packedData = ByteArray(PKT_SIZE)
        System.err.println("LLHUDEffectResetSkeleton: unpackData not yet implemented")

        // If no explicit target, the source resets itself.
        // targetId null → use sourceId. See unpackData comment in C++ regarding animesh permission concerns.
        System.err.println("LLHUDEffectResetSkeleton: unpackData not yet implemented")

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
            System.err.println("LLHUDEffectResetSkeleton: update not yet implemented")
        }

        markDead()
    }
}
