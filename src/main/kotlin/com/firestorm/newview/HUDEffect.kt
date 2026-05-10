package com.firestorm.newview

import java.util.UUID

const val HUD_DUR_SHORT: Float = 1f

// ---------------------------------------------------------------------------
// HUDObject base – minimal stub; real implementation lives in llhudobject.h
// ---------------------------------------------------------------------------

abstract class HUDObject(val objectType: UByte) {
    protected var mDead: Boolean = false
    protected var mSourceObject: ViewerObject? = null
    protected var mTargetObject: ViewerObject? = null

    open fun markDead() { mDead = true }
    open fun setSourceObject(objectp: ViewerObject?) { mSourceObject = objectp }
}

// ---------------------------------------------------------------------------
// HUDEffect
// ---------------------------------------------------------------------------

abstract class HUDEffect(type: UByte) : HUDObject(type) {

    protected var mID: UUID = UUID(0, 0)
    protected var mDuration: Float = 1f
    protected var mColor: Color4U = Color4U()
    protected var mNeedsSendToSim: Boolean = false
    protected var mOriginatedHere: Boolean = false

    fun setNeedsSendToSim(sendToSim: Boolean) { mNeedsSendToSim = sendToSim }
    fun getNeedsSendToSim(): Boolean = mNeedsSendToSim

    fun setOriginatedHere(origHere: Boolean) { mOriginatedHere = origHere }
    fun getOriginatedHere(): Boolean = mOriginatedHere

    fun setDuration(duration: Float) { mDuration = duration }

    fun setColor(color: Color4U) { mColor = color }

    fun setID(id: UUID) { mID = id }
    fun getID(): UUID = mID

    fun isDead(): Boolean = mDead

    open fun packData(mesgsys: Any) {
        TODO("APR: pack mID, agent ID, mType, mDuration, mColor into message system")
    }

    open fun unpackData(mesgsys: Any, blocknum: Int) {
        TODO("APR: unpack mID, mType, mDuration, mColor from message block $blocknum")
    }

    open fun render() {
        error("HUDEffect.render() must be overridden – never call the base")
    }

    open fun update() {
        // base no-op
    }

    companion object {
        fun getIDType(mesgsys: Any, blocknum: Int): Pair<UUID, UByte> {
            TODO("APR: unpack ID and type from Effect block $blocknum in message system")
        }
    }
}
