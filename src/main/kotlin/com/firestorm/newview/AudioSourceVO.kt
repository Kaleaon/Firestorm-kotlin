package com.firestorm.newview

import java.util.UUID

// AudioSourceVO — an audio source whose position and mute state are
// derived from a viewer object rather than being set manually.
open class AudioSourceVO(
    soundId: UUID,
    ownerId: UUID,
    gain: Float,
    private var objectp: ViewerObject?
) : AudioSource(soundId, ownerId, gain, AudioType.SFX, objectp?.id ?: UUID(0L, 0L)) {

    init {
        update()
    }

    fun destroy() {
        objectp?.clearAttachedSound()
        objectp = null
    }

    override fun setGain(gain: Float) {
        mGain = gain.coerceIn(0f, 1f)
    }

    // Called externally to mute the source when the listener moves outside
    // the object's cut-off sphere without waiting for the next full update().
    fun checkCutOffRadius() {
        if (mSourceMuted || objectp == null) return
        val cutoff = objectp!!.soundCutOffRadius
        if (cutoff < 0.1f) return
        if (!isInCutOffRadius(getPosGlobal(), cutoff)) {
            mSourceMuted = true
        }
    }

    override fun update() {
        updateMute()
        val obj = objectp ?: return
        if (obj.isDead) {
            objectp = null
            return
        }
        if (mSourceMuted) return

        if (obj.isHUDAttachment) {
            TODO("GPU: set mPositionGlobal = agentCamera.cameraPositionGlobal")
        } else {
            mPositionGlobal = obj.positionGlobal
        }

        val subParent = obj.subParent
        mVelocity = if (subParent != null) subParent.velocity else obj.velocity

        super.update()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun getPosGlobal(): Vector3d {
        val obj = objectp ?: return Vector3d()
        if (obj.isAttachment) {
            var parent: ViewerObject? = obj
            while (parent != null && !parent.isAvatar) {
                parent = parent.parent as? ViewerObject
            }
            if (parent != null) return parent.positionGlobal
        } else {
            return obj.positionGlobal
        }
        return Vector3d()
    }

    private fun isInCutOffRadius(posGlobal: Vector3d, cutoff: Float): Boolean {
        TODO("APR: use JVM equivalent — read MediaSoundsEarLocation setting, compute distance from ear to posGlobal, return dist < cutoff")
    }

    private fun updateMute() {
        val obj = objectp
        if (obj == null || obj.isDead) {
            mSourceMuted = true
            return
        }

        var mute = false
        val posGlobal = getPosGlobal()
        val cutoff = obj.soundCutOffRadius

        if (cutoff > 0.1f && !isInCutOffRadius(posGlobal, cutoff)) {
            mute = true
        } else if (!canHearSoundAtPos(posGlobal)) {
            // Agent may be riding an object that crosses a parcel border; only
            // mute when the sound source is on a different root than the agent.
            mute = !agentIsRidingSameRoot(obj)
        }

        if (!mute) {
            mute = isMutedByMuteList(obj)
        }

        if (mute != mSourceMuted) {
            mSourceMuted = mute
            if (mSourceMuted) {
                play(UUID(0L, 0L))
            } else {
                val currentData = mCurrentDatap
                if (currentData != null) {
                    play(currentData.id)
                }
            }
        }
    }

    private fun canHearSoundAtPos(posGlobal: Vector3d): Boolean {
        TODO("APR: use JVM equivalent — call ViewerParcelMgr.canHearSound(posGlobal)")
    }

    private fun agentIsRidingSameRoot(obj: ViewerObject): Boolean {
        TODO("APR: use JVM equivalent — compare obj.root with agentAvatar.root")
    }

    private fun isMutedByMuteList(obj: ViewerObject): Boolean {
        TODO("APR: use JVM equivalent — check MuteList for obj.id, ownerId (flagObjectSounds), and avatar parent id")
    }
}
