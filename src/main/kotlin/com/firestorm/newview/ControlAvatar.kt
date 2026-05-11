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

    open fun initInstance() { TODO("APR: use JVM equivalent — LLVOAvatar::initInstance") }
    open fun markDead() { TODO("APR: use JVM equivalent — LLVOAvatar::markDead") }
    open fun idleUpdate(agent: Any, time: Double) { TODO("APR: use JVM equivalent — LLVOAvatar::idleUpdate") }
    open fun computeNeedsUpdate(): Boolean { TODO("APR: use JVM equivalent — LLVOAvatar::computeNeedsUpdate") }
    open fun updateCharacter(agent: Any): Boolean { TODO("APR: use JVM equivalent — LLVOAvatar::updateCharacter") }
    open fun updateDebugText() { TODO("APR: use JVM equivalent — LLVOAvatar::updateDebugText") }
    open fun isImpostor(): Boolean { TODO("APR: use JVM equivalent — LLVOAvatar::isImpostor") }
    open fun isTooComplex(): Boolean { TODO("APR: use JVM equivalent — LLVOAvatar::isTooComplex") }
    open fun shouldRenderRigged(): Boolean = true
    open fun getFullname(): String = ""
    open fun getAttachedAvatar(): VOAvatar? = null

    fun addDebugText(text: String) { TODO("APR: use JVM equivalent — overlay debug text on avatar") }
    fun computeUpdatePeriod() { TODO("APR: use JVM equivalent — compute render update cadence") }
    fun processAnimationStateChanges() { TODO("APR: use JVM equivalent — process signaled animation state changes") }
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
            TODO("APR: use JVM equivalent — gObjectList.createObjectViewer with CO_FLAG_CONTROL_AVATAR, set rootVolp, matchVolumeTransform")
        }

        fun onRegionChanged() {
            TODO("APR: use JVM equivalent — iterate LLCharacter::sInstances, set mRegionChanged on each ControlAvatar")
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
        TODO("GPU: createDrawable, updateJointLODs, updateGeometry, hideSkirt, set initFlags bit 4")
    }

    override fun getAttachedAvatar(): VOAvatar? {
        TODO("APR: use JVM equivalent — return rootVolp?.getAvatarAncestor() if rootVolp is an attachment")
    }

    override fun markDead() {
        rootVolp = null
        super.markDead()
        controlAVBridge = null
    }

    fun markForDeath() {
        markedForDeath = true
        rootVolp = null
        TODO("APR: use JVM equivalent — clear mVolumep reference")
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
        TODO("APR: use JVM equivalent — read AnimatedObjectsMaxLegalOffset/Size settings, compute bounding-box constraint fixups from lastAnimExtents")
    }

    fun matchVolumeTransform() {
        if (rootVolp == null) return
        TODO("APR: use JVM equivalent — compute constraint fixups, sync position/rotation of control avatar to root volume drawable")
    }

    fun setGlobalScale(scale: Float) {
        if (scale <= 0f) return
        if (scale != globalScale) {
            val adjustScale = scale / globalScale
            TODO("APR: use JVM equivalent — recursiveScaleJoint(mPelvisp, adjustScale)")
            globalScale = scale
        }
    }

    fun recursiveScaleJoint(joint: Any, factor: Float) {
        TODO("APR: use JVM equivalent — joint.setScale(factor * joint.getScale()), recurse into joint.mChildren")
    }

    fun updateVolumeGeom() {
        TODO("GPU: makeActive on drawable, gPipeline.markMoved/markTextured, markRebuild, matchVolumeTransform")
    }

    fun getAnimatedVolumes(volumes: MutableList<Any>) {
        val root = rootVolp ?: return
        volumes.add(root)
        TODO("APR: use JVM equivalent — add child volumes that are animated objects")
    }

    fun updateAnimations() {
        val root = rootVolp ?: return
        val volumes = mutableListOf<Any>()
        getAnimatedVolumes(volumes)

        val anims: MutableMap<UUID, Int> = mutableMapOf()
        for (vol in volumes) {
            TODO("APR: use JVM equivalent — merge signaledAnimations from ObjectSignaledAnimationMapMgr.map[vol.id]")
        }

        if (!playing) {
            playing = true
            updateVolumeGeom()
            TODO("APR: use JVM equivalent — rootVolp.recursiveMarkForUpdate()")
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
        TODO("GPU: lineSegmentBoundingBox check, then lineSegmentIntersect against rootVolp and animated child volumes")
    }

    override fun updateDebugText() {
        TODO("APR: use JVM equivalent — collect triangle/vert/lod/stream stats from animated volumes if DebugAnimatedObjects is set")
    }

    override fun getFullname(): String {
        val root = rootVolp ?: return "AO_no_root_vol"
        TODO("APR: use JVM equivalent — return \"AO_\" + rootVolp.getID().asString()")
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
        TODO("APR: use JVM equivalent — return false if rootVolp is not an attachment, else super.isTooComplex()")
    }
}
