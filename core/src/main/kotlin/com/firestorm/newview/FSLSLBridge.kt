/**
 * FSLSLBridge.kt
 * Converted from fslslbridge.h / fslslbridge.cpp
 * Original copyright (C) 2011-2017 The Phoenix Firestorm Project, Inc.
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Well-known constants — mirrors the static/const values in fslslbridge.cpp
// ---------------------------------------------------------------------------
const val FS_BRIDGE_NAME: String = "#Firestorm LSL Bridge v"
const val FS_BRIDGE_FOLDER: String = "#LSL Bridge"
const val FS_BRIDGE_CONTAINER_FOLDER: String = "Landscaping"
const val FS_BRIDGE_MAJOR_VERSION: UInt = 2u
const val FS_BRIDGE_MINOR_VERSION: UInt = 29u
const val FS_MAX_MINOR_VERSION: UInt = 99u
const val FS_BRIDGE_POINT: UByte = 31u             // attachment point index ("Center 2")
const val FS_BRIDGE_ATTACHMENT_POINT_NAME: String = "Center 2"
const val LIB_ROCK_NAME: String = "Rock - medium, round"

// ---------------------------------------------------------------------------
// BridgeStatus — replaces the scattered boolean state in FSLSLBridge
// ---------------------------------------------------------------------------
enum class BridgeStatus {
    /** No bridge exists or has been initialised yet. */
    UNINITIALIZED,
    /** Bridge prim is being created / script uploaded. */
    CREATING,
    /** Bridge is attached and the handshake URL has been received. */
    VALID,
    /** Bridge was detached or invalidated; re-creation may be pending. */
    INVALID
}

// ---------------------------------------------------------------------------
// TimerResult — mirrors FSLSLBridge::TimerResult
// ---------------------------------------------------------------------------
enum class TimerResult {
    START_CREATION_FINISHED,
    CLEANUP_FINISHED,
    REATTACH_FINISHED,
    SCRIPT_UPLOAD_FINISHED,
    NO_TIMER
}

// ---------------------------------------------------------------------------
// FSLSLBridge singleton
// Mirrors class FSLSLBridge : public LLSingleton<FSLSLBridge>
// ---------------------------------------------------------------------------
object FSLSLBridge {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    var status: BridgeStatus = BridgeStatus.UNINITIALIZED
        private set

    /** The inventory item that represents the bridge attachment. */
    var bridgeObject: LLUUID = LLUUID.NULL
        private set

    /** The UUID of the bridge prim currently worn. */
    var attachedId: LLUUID = LLUUID.NULL
        private set

    /** The inventory folder that holds bridge assets. */
    var bridgeFolderID: LLUUID = LLUUID.NULL
        private set

    /** Full versioned name, e.g. "#Firestorm LSL Bridge v2.29". */
    val currentFullName: String =
        "$FS_BRIDGE_NAME${FS_BRIDGE_MAJOR_VERSION}.${FS_BRIDGE_MINOR_VERSION}"

    private var currentURL: String = ""
    private var isBridgeCreating: Boolean = false
    private var allowDetach: Boolean = false
    private var finishCreation: Boolean = false
    private var isFirstCallDone: Boolean = false
    private var timerResult: TimerResult = TimerResult.NO_TIMER
    private val allowedDetachables: MutableSet<LLUUID> = mutableSetOf()

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Returns true when the bridge inventory item is present and worn. */
    fun isBridgeValid(): Boolean = status == BridgeStatus.VALID && !bridgeObject.isNull()

    /** Returns true if the bridge exists AND the LSL bridge feature is enabled. */
    fun canUseBridge(): Boolean {
        return false
    }

    /**
     * Initialise the bridge on login: locate or create the bridge folder,
     * attach the prim, upload the script.
     * Mirrors FSLSLBridge::initBridge().
     */
    fun initBridge() {
        System.err.println("FSLSLBridge: initBridge not yet implemented")
    }

    /**
     * Destroy the current bridge and build a fresh one.
     * Mirrors FSLSLBridge::recreateBridge().
     */
    fun recreateBridge() {
        System.err.println("FSLSLBridge: recreateBridge not yet implemented")
    }

    /**
     * Handle an incoming XML-tagged message from the in-world LSL script.
     * Returns true if the message was recognised and handled.
     * Mirrors FSLSLBridge::lslToViewer().
     *
     * Expected format: XML tags such as <bridgeURL>…</bridgeURL>,
     * <bridgeAuth>…</bridgeAuth>, <bridgeVer>…</bridgeVer>.
     */
    fun scriptToViewer(message: String, fromId: LLUUID, ownerId: LLUUID): Boolean {
        if (message.isEmpty() || message[0] != '<') return false

        val tagEnd = minOf(
            message.indexOf('>').takeIf { it >= 0 } ?: Int.MAX_VALUE,
            message.indexOf(' ').takeIf { it >= 0 } ?: Int.MAX_VALUE
        ).takeIf { it != Int.MAX_VALUE } ?: return false

        val tag = message.substring(0, tagEnd + 1)

        return when (tag) {
            "<bridgeURL>" -> {
                handleBridgeUrl(message)
                true
            }
            "<bridgeError>" -> {
                return false
            }
            else -> {
                return false
            }
        }
    }

    /**
     * Send a message string to the in-world bridge script via its capability URL.
     * [callback] is invoked with the HTTP response LLSD when the request completes.
     * Mirrors FSLSLBridge::viewerToLSL().
     */
    fun viewerToScript(message: String, callback: ((Any?) -> Unit)? = null): Boolean {
        if (!isBridgeValid()) return false
        if (currentURL.isEmpty()) return false
        return false
    }

    /**
     * Convenience: update a boolean viewer setting via an LSL message value.
     * Mirrors FSLSLBridge::updateBoolSettingValue().
     */
    fun updateBoolSettingValue(msgVal: String, contentVal: Boolean? = null): Boolean {
        return false
    }

    /**
     * Refresh all feature integrations that depend on the bridge (e.g. AO,
     * radar coarse offsets, windlight sync).
     * Mirrors FSLSLBridge::updateIntegrations().
     */
    fun updateIntegrations() {
        System.err.println("FSLSLBridge: updateIntegrations not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Attachment lifecycle
    // -----------------------------------------------------------------------

    /**
     * Called when a viewer object is attached at [FS_BRIDGE_POINT].
     * Mirrors FSLSLBridge::processAttach().
     */
    fun processAttach(objectId: LLUUID, attachmentPointName: String) {
        if (attachmentPointName != FS_BRIDGE_ATTACHMENT_POINT_NAME) return
        attachedId = objectId
        System.err.println("FSLSLBridge: processAttach handshake not yet implemented")
    }

    /**
     * Called when the bridge prim is detached.
     * Mirrors FSLSLBridge::processDetach().
     */
    fun processDetach(objectId: LLUUID, attachmentPointName: String) {
        if (objectId != attachedId) return
        attachedId = LLUUID.NULL
        status = BridgeStatus.INVALID
        currentURL = ""
        System.err.println("FSLSLBridge: processDetach observer notify not yet implemented")
    }

    /** Returns true if the inventory item [itemId] is allowed to be detached. */
    fun canDetach(itemId: LLUUID): Boolean =
        itemId in allowedDetachables || itemId == bridgeObject

    // -----------------------------------------------------------------------
    // Timer / idle processing
    // -----------------------------------------------------------------------

    /**
     * Idle callback — dispatches deferred timer results on the main thread.
     * Mirrors FSLSLBridge::onIdle().
     */
    fun onIdle() {
        when (timerResult) {
            TimerResult.START_CREATION_FINISHED -> {
                finishCleanUpPreCreation()
                timerResult = TimerResult.NO_TIMER
            }
            TimerResult.CLEANUP_FINISHED -> {
                finishBridge()
                timerResult = TimerResult.NO_TIMER
            }
            TimerResult.REATTACH_FINISHED -> {
                System.err.println("FSLSLBridge: reattach not yet implemented")
                timerResult = TimerResult.NO_TIMER
            }
            TimerResult.SCRIPT_UPLOAD_FINISHED -> {
                checkBridgeScriptName()
                timerResult = TimerResult.NO_TIMER
            }
            TimerResult.NO_TIMER -> { /* nothing to do */ }
        }
    }

    fun setTimerResult(result: TimerResult) {
        timerResult = result
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Parse the <bridgeURL> handshake from the LSL script and store the URL.
     * Mirrors the <bridgeURL> branch in FSLSLBridge::lslToViewer().
     */
    private fun handleBridgeUrl(message: String) {
        System.err.println("FSLSLBridge: handleBridgeUrl not yet implemented")
    }

    private fun startCreation() {
        isBridgeCreating = true
        System.err.println("FSLSLBridge: startCreation not yet implemented")
    }

    private fun finishBridge() {
        System.err.println("FSLSLBridge: finishBridge not yet implemented")
    }

    private fun cleanUpBridge() {
        System.err.println("FSLSLBridge: cleanUpBridge not yet implemented")
    }

    private fun cleanUpPreCreation() {
        System.err.println("FSLSLBridge: cleanUpPreCreation not yet implemented")
    }

    private fun finishCleanUpPreCreation() {
        cleanUpPreCreation()
        startCreation()
    }

    private fun cleanUpOldVersions() {
        System.err.println("FSLSLBridge: cleanUpOldVersions not yet implemented")
    }

    private fun checkBridgeScriptName() {
        System.err.println("FSLSLBridge: checkBridgeScriptName not yet implemented")
    }

    private fun findFSCategory(): LLUUID {
        return LLUUID.NULL
    }
}
