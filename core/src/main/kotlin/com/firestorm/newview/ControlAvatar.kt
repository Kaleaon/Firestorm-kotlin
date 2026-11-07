package com.firestorm.newview

import java.util.UUID
import kotlin.math.max

typealias SignaledAnimationMap = MutableMap<UUID, Int>
typealias ObjectSignaledAnimationMap = MutableMap<UUID, SignaledAnimationMap>

object ObjectSignaledAnimationMapMgr {
    val map: ObjectSignaledAnimationMap = mutableMapOf()
    fun getMap(): ObjectSignaledAnimationMap = map
}

abstract class VOAvatar(
    val id: UUID,
    val pcode: Int,
    val region: Any?
) {
    var isDummy: Boolean = false
    var isControlAvatar: Boolean = false
    var enableDefaultMotions: Boolean = true
    var needsImpostorUpdate: Boolean = false
    var lastImpostorUpdateReason: Int = 0
    val signaledAnimations: MutableMap<UUID, Int> = mutableMapOf()

    open fun initInstance() { System.err.println("VOAvatar: initInstance not yet implemented") }
    open fun markDead() { System.err.println("VOAvatar: markDead not yet implemented") }
    open fun idleUpdate(agent: Any, time: Double) { System.err.println("VOAvatar: idleUpdate not yet implemented") }
    open fun computeNeedsUpdate(): Boolean { return false }
    open fun updateCharacter(agent: Any): Boolean { return false }
    open fun updateDebugText() { System.err.println("VOAvatar: updateDebugText not yet implemented") }
    open fun isImpostor(): Boolean { return false }
    open fun isTooComplex(): Boolean { return false }
    open fun shouldRenderRigged(): Boolean = true
    open fun getFullname(): String = ""
    open fun getAttachedAvatar(): VOAvatar? = null

    fun addDebugText(text: String) { System.err.println("VOAvatar: addDebugText not yet implemented") }
    fun computeUpdatePeriod() { System.err.println("VOAvatar: computeUpdatePeriod not yet implemented") }
    fun processAnimationStateChanges() { System.err.println("VOAvatar: processAnimationStateChanges not yet implemented") }
}

class ControlAvatar(
    id: UUID,
    pcode: Int,
    region: Any?
) : VOAvatar(id, pcode, region) {

    var playing: Boolean = false
    var globalScale: Float = 1.0f
    var rootVolp: Any? = null
    var controlAVBridge: Any? = null
    var markedForDeath: Boolean = false
    var positionConstraintFixup: FloatArray = FloatArray(3)
    var scaleConstraintFixup: Float = 1.0f
    var regionChanged: Boolean = false

    companion object {
        const val MAX_LEGAL_OFFSET = 3.0f
        const val MAX_LEGAL_SIZE = 64.0f

        private val regionChangedListeners: MutableList<() -> Unit> = mutableListOf()

        fun createControlAvatar(obj: Any): ControlAvatar? {
            return null
        }

        fun onRegionChanged() {
            System.err.println("ControlAvatar: onRegionChanged not yet implemented")
        }

        fun addRegionChangedListener(listener: () -> Unit) {
            regionChangedListeners += listener
        }
    }

    init {
        isDummy = true
        isControlAvatar = true
        enableDefaultMotions = false
    }

    override fun initInstance() {
        super.initInstance()
        System.err.println("ControlAvatar: initInstance not yet implemented")
    }

    override fun getAttachedAvatar(): VOAvatar? {
        return null
    }

    override fun markDead() {
        rootVolp = null
        super.markDead()
        controlAVBridge = null
    }

    fun markForDeath() {
        markedForDeath = true
        rootVolp = null
        System.err.println("ControlAvatar: markForDeath not yet implemented")
    }

    override fun idleUpdate(agent: Any, time: Double) {
        if (markedForDeath) {
            markDead()
            markedForDeath = false
        } else {
            super.idleUpdate(agent, time)
        }
    }

    override fun computeNeedsUpdate(): Boolean {
        computeUpdatePeriod()
        val attachedAv = getAttachedAvatar()
        if (attachedAv != null) {
            attachedAv.computeNeedsUpdate()
            needsImpostorUpdate = attachedAv.needsImpostorUpdate
            if (needsImpostorUpdate) lastImpostorUpdateReason = 12
            return needsImpostorUpdate
        }
        return super.computeNeedsUpdate()
    }

    override fun updateCharacter(agent: Any): Boolean = super.updateCharacter(agent)

    fun getNewConstraintFixups(newPosFixup: FloatArray, newScaleFixup: FloatArray) {
        System.err.println("ControlAvatar: getNewConstraintFixups not yet implemented")
    }

    fun matchVolumeTransform() {
        if (rootVolp == null) return
        System.err.println("ControlAvatar: matchVolumeTransform not yet implemented")
    }

    fun setGlobalScale(scale: Float) {
        if (scale <= 0f) return
        if (scale != globalScale) {
            val adjustScale = scale / globalScale
            System.err.println("ControlAvatar: setGlobalScale recursiveScaleJoint not yet implemented")
            globalScale = scale
        }
    }

    fun recursiveScaleJoint(joint: Any, factor: Float) {
        System.err.println("ControlAvatar: recursiveScaleJoint not yet implemented")
    }

    fun updateVolumeGeom() {
        System.err.println("ControlAvatar: updateVolumeGeom not yet implemented")
    }

    fun getAnimatedVolumes(volumes: MutableList<Any>) {
        val root = rootVolp ?: return
        volumes.add(root)
        System.err.println("ControlAvatar: getAnimatedVolumes not yet implemented")
    }

    fun updateAnimations() {
        val root = rootVolp ?: return
        val volumes = mutableListOf<Any>()
        getAnimatedVolumes(volumes)

        val anims: MutableMap<UUID, Int> = mutableMapOf()
        for (vol in volumes) {
            System.err.println("ControlAvatar: updateAnimations merge signaledAnimations from ObjectSignaledAnimationMapMgr not yet implemented")
        }

        if (!playing) {
            playing = true
            updateVolumeGeom()
            System.err.println("ControlAvatar: updateAnimations rootVolp.recursiveMarkForUpdate not yet implemented")
        }

        signaledAnimations.clear()
        signaledAnimations.putAll(anims)
        processAnimationStateChanges()
    }

    fun lineSegmentIntersectRiggedAttachments(
        start: FloatArray,
        end: FloatArray,
        face: Int = -1,
        pickTransparent: Boolean = false,
        pickRigged: Boolean = false,
        pickUnselectable: Boolean = true,
        faceHit: IntArray? = null,
        intersection: FloatArray? = null,
        texCoord: FloatArray? = null,
        normal: FloatArray? = null,
        tangent: FloatArray? = null
    ): Any? {
        if (rootVolp == null) return null
        System.err.println("ControlAvatar: lineSegmentIntersectRiggedAttachments not yet implemented")
        return null
    }

    override fun updateDebugText() {
        System.err.println("ControlAvatar: updateDebugText collect triangle/vert/lod/stream stats not yet implemented")
    }

    override fun getFullname(): String {
        val root = rootVolp ?: return "AO_no_root_vol"
        System.err.println("ControlAvatar: getFullname rootVolp.getID().asString() not yet implemented")
        return ""
    }

    override fun shouldRenderRigged(): Boolean {
        val attachedAv = getAttachedAvatar()
        return attachedAv?.shouldRenderRigged() ?: true
    }

    override fun isImpostor(): Boolean {
        val attachedAv = getAttachedAvatar()
        if (attachedAv != null) return attachedAv.isImpostor()
        return super.isImpostor()
    }

    override fun isTooComplex(): Boolean {
        System.err.println("ControlAvatar: isTooComplex not yet implemented")
        return false
    }
}
