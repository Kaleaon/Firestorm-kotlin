/**
 * ViewerPartSource.kt
 * Converted from llviewerpartsource.h / llviewerpartsource.cpp
 *
 * Particle-source base classes for the Second Life viewer's particle system.
 * Rendering, texture-fetch, and object-list look-ups are stubbed with TODO.
 * The data model, type hierarchy, and ID-seed logic are faithfully transcribed.
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// PartSourceType  (analogous to the LL_PART_SOURCE_* enum)
// ---------------------------------------------------------------------------

/**
 * Identifies the behavioural class of a particle source.
 *
 * [code] matches the U16 wire values used in SL's particle system data.
 */
enum class PartSourceType(val code: UShort) {
    /** Null / undefined — used as a sentinel. */
    NULL_SOURCE(0u),
    /** Generic script-driven particle source (PART_SOURCE_SCRIPT). */
    SCRIPT(1u),
    /** Spiral effect (avatar customisation). */
    SPIRAL(2u),
    /** Tractor-beam / editing beam. */
    BEAM(3u),
    /** Chat bubble effect. */
    CHAT(4u),
    /** Muted: source exists but emits nothing. */
    MUTED(0xFFFFu),
}

// ---------------------------------------------------------------------------
// ViewerPartSource  (LLViewerPartSource)
// ---------------------------------------------------------------------------

/**
 * Abstract base for all particle sources.
 *
 * Subclasses implement [update] to emit particles each simulation tick.
 * The owning [ViewerPartSim] calls [update] and checks [isDead] to decide
 * whether to retire the source.
 */
abstract class ViewerPartSource(val type: PartSourceType) {

    // -- Identity -------------------------------------------------------------

    /** Unique numeric ID for this source within the current session. */
    val id: UInt = nextId()

    /** UUID of the script/object owner. */
    var ownerID: LLUUID = LLUUID.NULL

    // -- Spatial state --------------------------------------------------------

    /** Current agent-space position of the emission point. */
    var pos: Vector3 = Vector3.ZERO

    /** Agent-space position of the particle target (beam/spiral sources). */
    var targetPos: Vector3 = Vector3.ZERO

    /** Position at last update tick (used for velocity-based placement). */
    var lastUpdatePos: Vector3 = Vector3.ZERO

    // -- Lifecycle flags ------------------------------------------------------

    var isOnClipboard: Boolean = false
    var isSuspended: Boolean = false

    // -- Internal state -------------------------------------------------------

    protected var isDead: Boolean = false

    /** Time (seconds) since last update call. */
    protected var lastUpdateTime: Float = 0f
    /** Time (seconds) of the last emitted particle. */
    protected var lastPartTime: Float = 0f

    /** Maximum quota of active particles from this source (0 = unlimited). */
    var quota: UInt = 0u

    /** Bit-flags from the LLPartSysData particle description. */
    protected var partFlags: UInt = 0u

    /** Delay (in ticks) before the source starts emitting. */
    protected var delay: UInt = 0u

    // -- Texture / avatar back-references (set by concrete subclasses) --------

    /** UUID of the particle texture image. */
    var imageID: LLUUID = LLUUID.NULL
        protected set

    // -------------------------------------------------------------------------
    // Abstract interface
    // -------------------------------------------------------------------------

    /**
     * Advance the particle source by [dt] seconds.
     * Concrete subclasses emit new [ViewerPart] instances via the sim.
     */
    abstract fun update(dt: Float)

    // -------------------------------------------------------------------------
    // Lifecycle helpers
    // -------------------------------------------------------------------------

    fun isDead(): Boolean = isDead

    open fun setDead() { isDead = true }

    fun setSuspended(state: Boolean) { isSuspended = state }
    fun isSuspended(): Boolean = isSuspended

    /**
     * Cancel the startup delay so the source emits immediately.
     * Useful for short-lived sources that would otherwise miss their window.
     */
    fun setStart() { delay = 0u }

    // -------------------------------------------------------------------------
    // Companion
    // -------------------------------------------------------------------------

    companion object {
        private var idSeed: UInt = 0u

        private fun nextId(): UInt = ++idSeed

        /**
         * Base update hook called for every live particle each tick.
         * In the C++ code this is a no-op in the base class; overridden by
         * sources that continuously modify existing particles (e.g. spirals).
         */
        fun updatePart(dt: Float) {
            // No-op in base: concrete update logic in particle sim callbacks.
        }
    }
}

// ---------------------------------------------------------------------------
// ViewerPartSourceScript  (LLViewerPartSourceScript)
// ---------------------------------------------------------------------------

/**
 * Script-driven particle source attached to an in-world object.
 *
 * Parses [LLPartSysData]-equivalent parameters and continuously emits
 * particles according to the script's particle system definition.
 */
class ViewerPartSourceScript : ViewerPartSource(PartSourceType.SCRIPT) {

    // -- Particle-system description ------------------------------------------

    /**
     * Snapshot of the script's LLPartSysData.  In C++ this carries all
     * particle parameters (rate, age, pattern, etc.).  Here it is typed
     * as a generic parameter map; a richer data class can be substituted.
     */
    var template: MutableMap<String, Any> = mutableMapOf()

    // -- Rotation -------------------------------------------------------------

    /** Current rotation of the emission cone (updated each tick). */
    var rotation: Vector4 = Vector4.IDENTITY   // analogous to LLQuaternion mRotation

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun setDead() {
        super.setDead()
        // In the C++ code: release target object pointer etc.
    }

    // -------------------------------------------------------------------------
    // Emission
    // -------------------------------------------------------------------------

    /**
     * Advance the script particle source by [dt] seconds and emit new
     * particles according to [template].
     *
     * Full emission logic (burst count, interpolation, pattern-based
     * velocity) requires the particle simulator; stubbed here.
     */
    override fun update(dt: Float) {
        if (isDead || isSuspended) return
        if (delay > 0u) { delay--; return }
        lastUpdateTime += dt
        updatePart()
    }

    /**
     * Emit one particle from the current source configuration.
     * Called by [update] and by the external particle simulator.
     */
    fun updatePart() {
        TODO("PARTICLE: emit particle from script source using template=$template")
    }

    // -------------------------------------------------------------------------
    // Message deserialization stubs
    // -------------------------------------------------------------------------

    /** Apply a particle system data update received over the network. */
    fun updateFromMesg(): Boolean {
        TODO("PARTICLE: unpack LLPartSysData from network message")
    }

    /** Set the object that script particles track as a target. */
    fun setTargetObject(targetId: LLUUID) {
        TODO("PARTICLE: resolve target object from ID=$targetId and store reference")
    }

    companion object {
        /**
         * Factory: unpack a [ViewerPartSourceScript] from a network block.
         * [blockNum] is the index of the ObjectUpdate block.
         */
        fun unpackPSS(sourceObjectId: LLUUID, existing: ViewerPartSourceScript?, blockNum: Int): ViewerPartSourceScript {
            TODO("PARTICLE: unpack LLPartSysData from ObjectUpdate block $blockNum for object $sourceObjectId")
        }

        /** Factory: create from an explicit [LLPartSysData]-equivalent parameter map. */
        fun createPSS(sourceObjectId: LLUUID, params: Map<String, Any>): ViewerPartSourceScript {
            return ViewerPartSourceScript().also { src ->
                src.template.putAll(params)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ViewerPartSourceSpiral  (LLViewerPartSourceSpiral)
// ---------------------------------------------------------------------------

/**
 * Spiral particle effect — used during avatar customisation (colour changes,
 * wearable attachment, etc.).
 */
class ViewerPartSourceSpiral(initialPos: Vector3) : ViewerPartSource(PartSourceType.SPIRAL) {

    var color: Color4 = Color4(1f, 1f, 1f, 1f)

    private var lkgSourcePosGlobal: Vector3 = initialPos

    init { pos = initialPos }

    override fun setDead() { super.setDead() }

    override fun update(dt: Float) {
        if (isDead || isSuspended) return
        lastUpdateTime += dt
        TODO("PARTICLE: emit spiral particle arc around source at pos=$pos")
    }

    fun setSourceObject(objectId: LLUUID) {
        TODO("PARTICLE: bind spiral source to object $objectId for position tracking")
    }

    fun setColor(c: Color4) { color = c }

    companion object {
        fun updatePart(dt: Float) {
            TODO("PARTICLE: update spiral particle trajectory for dt=$dt")
        }
    }
}

// ---------------------------------------------------------------------------
// ViewerPartSourceBeam  (LLViewerPartSourceBeam)
// ---------------------------------------------------------------------------

/**
 * Tractor-beam / editing beam particle source — particles stream from the
 * agent's hand to the selected object.
 */
class ViewerPartSourceBeam : ViewerPartSource(PartSourceType.BEAM) {

    var color: Color4 = Color4(1f, 1f, 1f, 1f)
    var targetObjectId: LLUUID = LLUUID.NULL
    var lkgTargetPosGlobal: Vector3 = Vector3.ZERO

    override fun setDead() { super.setDead() }

    override fun update(dt: Float) {
        if (isDead || isSuspended) return
        lastUpdateTime += dt
        TODO("PARTICLE: emit beam particles from pos=$pos toward target=$lkgTargetPosGlobal")
    }

    fun setSourceObject(objectId: LLUUID) {
        TODO("PARTICLE: bind beam source to object $objectId")
    }

    fun setTargetObject(targetId: LLUUID) {
        targetObjectId = targetId
        TODO("PARTICLE: resolve beam target object $targetId for position tracking")
    }

    fun setSourcePosGlobal(posGlobal: Vector3) { pos = posGlobal }
    fun setTargetPosGlobal(posGlobal: Vector3) { lkgTargetPosGlobal = posGlobal }
    fun setColor(c: Color4) { color = c }

    companion object {
        fun updatePart(dt: Float) {
            TODO("PARTICLE: update beam particle trajectory for dt=$dt")
        }
    }
}

// ---------------------------------------------------------------------------
// ViewerPartSourceChat  (LLViewerPartSourceChat)
// ---------------------------------------------------------------------------

/**
 * Chat-bubble particle effect — spawned around an avatar when they speak
 * in local chat.
 */
class ViewerPartSourceChat(initialPos: Vector3) : ViewerPartSource(PartSourceType.CHAT) {

    var color: Color4 = Color4(1f, 1f, 1f, 1f)
    private var lkgSourcePosGlobal: Vector3 = initialPos

    init { pos = initialPos }

    override fun setDead() { super.setDead() }

    override fun update(dt: Float) {
        if (isDead || isSuspended) return
        lastUpdateTime += dt
        TODO("PARTICLE: emit chat bubble particles around pos=$pos")
    }

    fun setSourceObject(objectId: LLUUID) {
        TODO("PARTICLE: bind chat source to object/avatar $objectId")
    }

    fun setColor(c: Color4) { color = c }

    companion object {
        fun updatePart(dt: Float) {
            TODO("PARTICLE: update chat bubble particle trajectory for dt=$dt")
        }
    }
}
