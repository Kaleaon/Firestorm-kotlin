/**
 * CompileQueue.kt
 * LSL script compilation queue — Kotlin port of llcompilequeue.h / llcompilequeue.cpp
 *
 * Original: Copyright (C) 2002-2010 Linden Research, Inc.
 * Ported to Kotlin for the Firestorm viewer project.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Supporting data types
// ---------------------------------------------------------------------------

/** Compile lifecycle state for a single script entry. */
enum class CompileStatus {
    QUEUED,
    COMPILING,
    COMPLETE,
    FAILED
}

/**
 * Represents a single script waiting in (or processed by) the compile queue.
 *
 * Maps from C++ [LLScriptQueueData] / [LLCompileQueueData] pair. The C++
 * code separates queue-data from compile-data; here we unify them for
 * clarity while keeping all relevant IDs.
 *
 * @property itemId     UUID of the inventory item (script asset).
 * @property objectId   UUID of the in-world object that owns the script.
 * @property itemName   Human-readable name of the script.
 * @property experienceId Optional experience UUID associated with the script.
 * @property status     Current compile lifecycle state.
 */
data class ScriptEntry(
    val itemId: LLUUID,
    val objectId: LLUUID,
    val itemName: String,
    val experienceId: LLUUID = LLUUID.NULL,
    var status: CompileStatus = CompileStatus.QUEUED
)

/**
 * A named group of objects to be processed by the queue, mirroring the C++
 * [LLFloaterScriptQueue::ObjectData] inner struct.
 */
data class ObjectData(
    val objectId: LLUUID,
    val objectName: String
)

// ---------------------------------------------------------------------------
// CompileQueue singleton
// ---------------------------------------------------------------------------

/**
 * Manages LSL script recompilation across a set of in-world objects.
 *
 * This is the Kotlin equivalent of the C++ [LLFloaterCompileQueue] /
 * [LLFloaterScriptQueue] hierarchy, collapsed into a single singleton
 * because the UI floater layer is handled separately in the Kotlin port.
 *
 * Script processing is inherently async in the viewer (coroutine-based in
 * C++); network/asset calls here are stubbed with [TODO] and should be wired
 * to the appropriate HTTP/asset layer.
 */
object CompileQueue {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Ordered list of objects to process. */
    val objectList: MutableList<ObjectData> = mutableListOf()

    /** Flat list of all script entries across all queued objects. */
    val scriptQueue: MutableList<ScriptEntry> = mutableListOf()

    /** UUID of the object currently being processed. */
    var currentObjectId: LLUUID = LLUUID.NULL
        private set

    /** Whether LSL should be compiled in Mono (true) vs. LSO2 (false). */
    var mono: Boolean = true

    private var done: Boolean = false

    // -----------------------------------------------------------------------
    // Queue management
    // -----------------------------------------------------------------------

    /** Adds an object (and its scripts) to the pending queue. */
    fun addObject(objectId: LLUUID, objectName: String) {
        objectList += ObjectData(objectId, objectName)
    }

    /**
     * Enqueues a script entry for compilation.
     *
     * Mirrors [LLFloaterCompileQueue::processScript] — callers typically
     * enumerate the inventory of each queued object and call this once per
     * script item found.
     */
    fun addScript(entry: ScriptEntry) {
        scriptQueue += entry
    }

    /**
     * Starts the queue.  Returns false if there are no objects to process.
     * In the C++ viewer this kicks off a coroutine; here it simply marks the
     * first object as active and triggers [processNextScript].
     */
    fun start(): Boolean {
        if (objectList.isEmpty()) return false
        done = false
        currentObjectId = objectList.first().objectId
        processNextScript()
        return true
    }

    /**
     * Advances to the next queued script and initiates compilation.
     *
     * Network / asset upload calls are stubbed — wire to the viewer's asset
     * upload service ([LLViewerAssetUpload] equivalent) before use.
     */
    fun processNextScript() {
        val next = scriptQueue.firstOrNull { it.status == CompileStatus.QUEUED }
        if (next == null) {
            done = true
            return
        }
        next.status = CompileStatus.COMPILING
        currentObjectId = next.objectId
        TODO("Upload/recompile asset for itemId=${next.itemId} on objectId=${next.objectId} via HTTP asset upload service")
    }

    /**
     * Called by the network layer when a compile result arrives.
     *
     * Mirrors the C++ [LLFloaterCompileQueue::finishLSLUpload] static and the
     * [handleHTTPResponse] pump callback.
     *
     * @param itemId  The script item whose compilation finished.
     * @param success Whether the compile succeeded on the simulator.
     */
    fun onCompileComplete(itemId: LLUUID, success: Boolean) {
        val entry = scriptQueue.find { it.itemId == itemId } ?: return
        entry.status = if (success) CompileStatus.COMPLETE else CompileStatus.FAILED
        if (!isDone()) {
            processNextScript()
        }
    }

    /** Returns true when every script in the queue has a terminal status. */
    fun isDone(): Boolean =
        scriptQueue.all { it.status == CompileStatus.COMPLETE || it.status == CompileStatus.FAILED }

    /** Clears all queued objects and scripts, resetting the queue. */
    fun reset() {
        objectList.clear()
        scriptQueue.clear()
        currentObjectId = LLUUID.NULL
        done = false
    }

    // -----------------------------------------------------------------------
    // Script-queue actions (mirrors C++ action-queue variants)
    // -----------------------------------------------------------------------

    /**
     * Resets (rather than recompiles) all queued scripts.
     * Stub — send [ScriptReset] message to the simulator for each entry.
     */
    fun resetAllScripts() {
        TODO("Send ScriptReset UDP message for each objectId/itemId pair in scriptQueue")
    }

    /**
     * Sets the running state of all queued scripts.
     * @param running true → run, false → stop.
     */
    fun setAllScriptsRunning(running: Boolean) {
        TODO("Send SetScriptRunning UDP message (running=$running) for each entry in scriptQueue")
    }

    /**
     * Deletes all queued scripts from their host objects.
     */
    fun deleteAllScripts() {
        TODO("Send RemoveInventory UDP message for each objectId/itemId pair in scriptQueue")
    }

    // -----------------------------------------------------------------------
    // Companion: experience support
    // -----------------------------------------------------------------------

    companion object {
        /** Known experience UUIDs received from the server, used during compile. */
        val experienceIds: MutableSet<LLUUID> = mutableSetOf()

        /** Records server-provided experience IDs for use in script uploads. */
        fun experienceIdsReceived(ids: Collection<LLUUID>) {
            experienceIds.addAll(ids)
        }

        /** Returns true if [id] is among the experiences known to this session. */
        fun hasExperience(id: LLUUID): Boolean = id in experienceIds
    }
}
