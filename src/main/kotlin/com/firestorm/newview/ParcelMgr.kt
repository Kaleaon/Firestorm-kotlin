// ParcelMgr.kt — converted from llviewerparcelmgr.h / llviewerparcelmgr.cpp
// Copyright (C) 2002, Linden Research, Inc. LGPL 2.1
package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// Observer interface
// ---------------------------------------------------------------------------

/** Receives notifications whenever the viewer's parcel selection changes. */
interface ParcelObserver {
    fun changed()
}

// ---------------------------------------------------------------------------
// Parcel data class
// ---------------------------------------------------------------------------

/**
 * Lightweight representation of a parcel's properties as known to the viewer.
 * Mirrors the fields of LLParcel that ViewerParcelMgr actually inspects.
 */
data class Parcel(
    val id: Int = 0,
    val ownerID: LLUUID = LLUUID.NULL,
    val groupID: LLUUID = LLUUID.NULL,
    val isGroupOwned: Boolean = false,
    val name: String = "",
    var flags: UInt = 0u,
    val area: Int = 0,
    val claimDate: Long = 0L,
    val salePrice: Int = 0,
    val dwell: Float = 0f
)

// ---------------------------------------------------------------------------
// ParcelMgr singleton
// ---------------------------------------------------------------------------

/**
 * Viewer-side manager for parcel properties and the current agent parcel.
 * Maps to the C++ LLViewerParcelMgr singleton.
 */
object ParcelMgr {

    // ------------------------------------------------------------------
    // Parcel flag constants (from llparcelflags.h, typed as UInt)
    // ------------------------------------------------------------------
    companion object {
        val PF_ALLOW_FLY              : UInt = 1u shl 0
        val PF_ALLOW_OTHER_SCRIPTS    : UInt = 1u shl 1
        val PF_FOR_SALE               : UInt = 1u shl 2
        val PF_ALLOW_LANDMARK         : UInt = 1u shl 3
        val PF_ALLOW_TERRAFORM        : UInt = 1u shl 4
        val PF_ALLOW_DAMAGE           : UInt = 1u shl 5
        val PF_CREATE_OBJECTS         : UInt = 1u shl 6
        val PF_FOR_SALE_OBJECTS       : UInt = 1u shl 7
        val PF_USE_ACCESS_GROUP       : UInt = 1u shl 8
        val PF_USE_ACCESS_LIST        : UInt = 1u shl 9
        val PF_USE_BAN_LIST           : UInt = 1u shl 10
        val PF_USE_PASS_LIST          : UInt = 1u shl 11
        val PF_SHOW_DIRECTORY         : UInt = 1u shl 12
        val PF_ALLOW_DEED_TO_GROUP    : UInt = 1u shl 13
        val PF_CONTRIBUTE_WITH_DEED   : UInt = 1u shl 14
        val PF_SOUND_LOCAL            : UInt = 1u shl 15
        val PF_SELL_PARCEL_OBJECTS    : UInt = 1u shl 16
        val PF_ALLOW_PUBLISH          : UInt = 1u shl 17
        val PF_MATURE_PUBLISH         : UInt = 1u shl 18
        val PF_URL_WEB_PAGE           : UInt = 1u shl 19
        val PF_URL_RAW_HTML           : UInt = 1u shl 20
        val PF_RESTRICT_PUSHOBJECT    : UInt = 1u shl 21
        val PF_DENY_ANONYMOUS         : UInt = 1u shl 22
        val PF_ALLOW_GROUP_SCRIPTS    : UInt = 1u shl 25
        val PF_CREATE_GROUP_OBJECTS   : UInt = 1u shl 26
        val PF_ALLOW_ALL_OBJECT_ENTRY : UInt = 1u shl 27
        val PF_ALLOW_GROUP_OBJECT_ENTRY: UInt = 1u shl 28
        val PF_ALLOW_VOICE_CHAT       : UInt = 1u shl 29
        val PF_USE_ESTATE_VOICE_CHAN   : UInt = 1u shl 30
        val PF_DENY_AGEUNVERIFIED     : UInt = 1u shl 31

        val PARCEL_BAN_LINES_HIDE         = 0
        val PARCEL_BAN_LINES_ON_COLLISION = 1
        val PARCEL_BAN_LINES_ON_PROXIMITY = 2

        val DWELL_NAN = -1f

        /**
         * Returns true if the parcel is owned (or effectively controlled) by
         * the specified agent, accounting for group proxy power.
         * Mirrors LLViewerParcelMgr::isParcelOwnedByAgent().
         */
        fun isParcelOwnedByAgent(parcel: Parcel, groupProxyPower: ULong): Boolean {
            TODO("Check parcel owner/group against agent UUID and group roles")
        }
    }

    // ------------------------------------------------------------------
    // Internal state
    // ------------------------------------------------------------------

    /** Parcel the agent is currently standing in. */
    var agentParcel: Parcel? = null
        private set

    /** Parcel currently selected / highlighted in the UI. */
    var currentParcel: Parcel? = null
        private set

    /** Parcel currently hovered over by the mouse. */
    var hoverParcel: Parcel? = null
        private set

    private val observers: MutableList<ParcelObserver> = mutableListOf()

    private var teleportInProgress: Boolean = false
    private var selected: Boolean = false
    private var selectedDwell: Float = DWELL_NAN
    private var renderSelection: Boolean = true
    private var collisionBanned: Boolean = false

    // ------------------------------------------------------------------
    // Observer management
    // ------------------------------------------------------------------

    fun addObserver(observer: ParcelObserver) {
        if (!observers.contains(observer)) observers.add(observer)
    }

    fun removeObserver(observer: ParcelObserver) { observers.remove(observer) }

    fun notifyObservers() { observers.forEach { it.changed() } }

    // ------------------------------------------------------------------
    // Network requests
    // ------------------------------------------------------------------

    /**
     * Ask the simulator for the parcel properties at [pos].
     * Mirrors LLViewerParcelMgr::requestParcelProperties().
     */
    fun requestParcelProperties(pos: Vector3) {
        TODO("Send ParcelPropertiesRequest UDP message to simulator")
    }

    // ------------------------------------------------------------------
    // Ownership queries
    // ------------------------------------------------------------------

    /** Returns true if the agent owns the parcel at [pos]. */
    fun isOwnedSelf(pos: Vector3): Boolean {
        TODO("Look up parcel overlay and compare owner UUID against agent UUID")
    }

    /** Returns true if the parcel at [pos] is group-owned by a group the agent belongs to. */
    fun isOwnedGroup(pos: Vector3): Boolean {
        TODO("Look up parcel overlay and check group ownership")
    }

    // ------------------------------------------------------------------
    // Permission helpers (operate on agentParcel by default)
    // ------------------------------------------------------------------

    /** Can the agent place or edit objects on the current parcel? */
    fun canBuild(): Boolean {
        val p = agentParcel ?: return false
        return (p.flags and PF_CREATE_OBJECTS) != 0u
    }

    /** Can the agent fly on the current parcel? */
    fun canFly(): Boolean {
        val p = agentParcel ?: return true   // default: flying allowed
        return (p.flags and PF_ALLOW_FLY) != 0u
    }

    /** Can non-owner scripts run on the current parcel? */
    fun canScript(): Boolean {
        val p = agentParcel ?: return false
        return (p.flags and PF_ALLOW_OTHER_SCRIPTS) != 0u
    }

    /** Broader permission check; mirrors allowAgentBuild(). */
    fun allowAgentBuild(): Boolean = canBuild()

    /** Mirrors allowAgentVoice(). */
    fun allowAgentVoice(): Boolean {
        val p = agentParcel ?: return false
        return (p.flags and PF_ALLOW_VOICE_CHAT) != 0u
    }

    /** Can the agent be pushed by llPushObject on the current parcel? */
    fun allowAgentPush(): Boolean {
        val p = agentParcel ?: return true
        return (p.flags and PF_RESTRICT_PUSHOBJECT) == 0u
    }

    /** Can the agent be damaged on the current parcel? */
    fun allowAgentDamage(): Boolean {
        val p = agentParcel ?: return false
        return (p.flags and PF_ALLOW_DAMAGE) != 0u
    }

    // ------------------------------------------------------------------
    // Agent parcel accessors
    // ------------------------------------------------------------------

    fun getAgentParcelName(): String = agentParcel?.name ?: ""
    fun getAgentParcelId(): Int = agentParcel?.id ?: -1

    fun selectionEmpty(): Boolean = !selected

    /** True if the collision border is shown because the agent is banned. */
    fun isCollisionBanned(): Boolean = collisionBanned

    fun getTeleportInProgress(): Boolean = teleportInProgress

    fun getDwelling(): Float = selectedDwell

    // ------------------------------------------------------------------
    // Network message handlers (stubs; receive parsed simulator data)
    // ------------------------------------------------------------------

    fun processParcelProperties(data: Map<String, Any>) {
        TODO("Unpack ParcelProperties message, update agentParcel, notify observers")
    }

    fun processParcelOverlay(data: ByteArray) {
        TODO("Unpack parcel overlay bitmap, update highlight / collision segments")
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    fun render() { TODO("GPU: draw parcel selection highlight and ban-line segments") }

    fun renderParcelCollision() { TODO("GPU: draw collision border segments") }

    // ------------------------------------------------------------------
    // Teleport callbacks
    // ------------------------------------------------------------------

    fun onTeleportFinished(local: Boolean) {
        teleportInProgress = false
        TODO("Request parcel properties at new position, fire teleport-finished callbacks")
    }

    fun onTeleportFailed() {
        teleportInProgress = false
        TODO("Fire teleport-failed callbacks")
    }
}
