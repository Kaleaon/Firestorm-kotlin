package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Stub types referenced from the C++ translation unit
// ---------------------------------------------------------------------------

open class LLGLTFMaterial {
    open fun asJSON(): String = "{}"
    open fun applyOverrideLLSD(data: Any?) {}
    open fun setBaseMaterial() {}
}

open class LLFetchedGLTFMaterial : LLGLTFMaterial() {
    var mFetching: Boolean = false
    var mActive: Boolean = true
    var mExpectedFlusTime: Double = 0.0
    var refCount: Int = 0

    open fun materialBegin() {}
    open fun materialComplete(success: Boolean) {}
    open fun isFetching(): Boolean = mFetching
    open fun getNumRefs(): Int = refCount
}

// ---------------------------------------------------------------------------
// LLGLTFMaterialOverrideDispatchHandler – internal helper (package-private)
// ---------------------------------------------------------------------------

private class LLGLTFMaterialOverrideDispatchHandler {
    private val selectionCallbacks: MutableList<(UUID, Int) -> Unit> = mutableListOf()

    fun addCallback(callback: (UUID, Int) -> Unit) {
        selectionCallbacks.add(callback)
    }

    fun doSelectionCallbacks(objectId: UUID, side: Int) {
        for (cb in selectionCallbacks) {
            cb(objectId, side)
        }
    }
}

private val handleGltfOverrideMessage = LLGLTFMaterialOverrideDispatchHandler()

// ---------------------------------------------------------------------------
// Data structures mirroring C++ inner structs
// ---------------------------------------------------------------------------

data class ModifyMaterialData(
    val objectId: UUID,
    val side: Int = -1,
    val overrideData: LLGLTFMaterial = LLGLTFMaterial(),
    val hasOverride: Boolean = false
)

data class ApplyMaterialAssetData(
    val objectId: UUID,
    val side: Int = -1,
    val assetId: UUID,
    val overrideData: LLFetchedGLTFMaterial? = null,
    val overrideJson: String = ""
)

// Mirrors std::shared_ptr<CallbackHolder>: invokes the callback on destruction
class CallbackHolder(private val callback: ((Boolean) -> Unit)?) {
    var success: Boolean = true

    fun complete() {
        callback?.invoke(success)
    }
}

// ---------------------------------------------------------------------------
// LLGLTFMaterialList
// ---------------------------------------------------------------------------

class LLGLTFMaterialList {

    private val list: MutableMap<UUID, LLFetchedGLTFMaterial> = mutableMapOf()
    private val queuedOverrides: MutableMap<UUID, MutableList<LLGLTFMaterial?>> = mutableMapOf()
    private var lastUpdateKey: UUID? = null

    companion object {
        private const val MAX_TASK_UPDATES = 255

        val sModifyQueue: ArrayDeque<ModifyMaterialData> = ArrayDeque()
        val sApplyQueue: ArrayDeque<ApplyMaterialAssetData> = ArrayDeque()

        // Represents the accumulated LLSD array of pending updates
        val sUpdates: MutableList<Map<String, Any>> = mutableListOf()

        @JvmStatic
        fun queueModify(obj: Any?, side: Int, mat: LLGLTFMaterial?) {
            TODO("APR: use JVM equivalent – send via capability HTTP POST")
        }

        @JvmStatic
        fun queueApply(obj: Any?, side: Int, assetId: UUID) {
            TODO("APR: use JVM equivalent – queue apply to simulator")
        }

        @JvmStatic
        fun queueApply(obj: Any?, side: Int, assetId: UUID, overrideJson: String) {
            if (assetId == UUID(0, 0) || overrideJson.isEmpty()) {
                queueApply(obj, side, assetId)
                return
            }
            TODO("APR: use JVM equivalent – queue apply with JSON override")
        }

        @JvmStatic
        fun queueApply(obj: Any?, side: Int, assetId: UUID, materialOverride: LLGLTFMaterial?) {
            if (assetId == UUID(0, 0) || materialOverride == null) {
                queueApply(obj, side, assetId)
                return
            }
            TODO("APR: use JVM equivalent – queue apply with material override")
        }

        @JvmStatic
        fun flushUpdates(doneCallback: ((Boolean) -> Unit)? = null) {
            val holder = CallbackHolder(doneCallback)
            while (sModifyQueue.isNotEmpty() || sApplyQueue.isNotEmpty()) {
                flushUpdatesOnce(holder)
            }
            holder.complete()
        }

        @JvmStatic
        fun addSelectionUpdateCallback(updateCallback: (UUID, Int) -> Unit) {
            handleGltfOverrideMessage.addCallback(updateCallback)
        }

        @JvmStatic
        fun queueUpdate(data: Map<String, Any>) {
            sUpdates.add(data)
            if (sUpdates.size >= MAX_TASK_UPDATES) {
                modifyMaterialCoro(null, sUpdates.toList(), null)
                sUpdates.clear()
            }
        }

        private fun flushUpdatesOnce(callbackHolder: CallbackHolder?) {
            val batch: MutableList<Map<String, Any>> = mutableListOf()

            while (sModifyQueue.isNotEmpty() && batch.size < MAX_TASK_UPDATES) {
                val e = sModifyQueue.removeFirst()
                val entry = mutableMapOf<String, Any>(
                    "object_id" to e.objectId,
                    "side" to e.side,
                    "gltf_json" to if (e.hasOverride) e.overrideData.asJSON() else ""
                )
                batch.add(entry)
            }

            while (sApplyQueue.isNotEmpty() && batch.size < MAX_TASK_UPDATES) {
                val e = sApplyQueue.removeFirst()
                val gltfJson: String = when {
                    e.overrideData != null -> e.overrideData.asJSON()
                    e.overrideJson.isNotEmpty() -> e.overrideJson
                    else -> ""
                }
                val entry = mutableMapOf<String, Any>(
                    "object_id" to e.objectId,
                    "side" to e.side,
                    "asset_id" to e.assetId,
                    "gltf_json" to gltfJson
                )
                batch.add(entry)
            }

            if (batch.isNotEmpty()) {
                modifyMaterialCoro(null, batch, callbackHolder)
                sUpdates.clear()
            }
        }

        private fun modifyMaterialCoro(capUrl: String?, overrides: List<Map<String, Any>>, callbackHolder: CallbackHolder?) {
            TODO("APR: use JVM equivalent – HTTP POST to ModifyMaterialParams capability")
        }

        private fun onAssetLoadComplete(id: UUID, assetType: Int, userData: Any?, status: Int) {
            TODO("APR: use JVM equivalent – load asset from VFS/cache and deserialize GLTF")
        }
    }

    fun getMaterial(id: UUID): LLFetchedGLTFMaterial {
        list[id]?.let { return it }
        val mat = LLFetchedGLTFMaterial()
        list[id] = mat
        if (!mat.mFetching) {
            mat.materialBegin()
            TODO("APR: use JVM equivalent – request asset via gAssetStorage")
        }
        return mat
    }

    fun addMaterial(id: UUID, material: LLFetchedGLTFMaterial) {
        list[id] = material
    }

    fun removeMaterial(id: UUID) {
        list.remove(id)
    }

    fun flushMaterials() {
        val minUpdateCount = 32
        val updateCount = maxOf(minUpdateCount, list.size / 20).coerceAtMost(list.size)
        val maxInactiveTime = 30.0
        val curTime = System.currentTimeMillis() / 1000.0

        var remaining = updateCount
        val iter = list.entries.iterator()
        var startedAfterLastKey = (lastUpdateKey == null)

        // Advance to just past the last key processed
        val entries = list.entries.toList()
        var startIdx = 0
        if (lastUpdateKey != null) {
            val idx = entries.indexOfFirst { it.key == lastUpdateKey }
            if (idx >= 0) startIdx = (idx + 1) % entries.size
        }

        var i = startIdx
        while (remaining-- > 0) {
            if (i >= entries.size) i = 0
            val (uuid, material) = entries[i]

            if (material.getNumRefs() == 2) {
                if (!material.mActive && curTime > material.mExpectedFlusTime) {
                    list.remove(uuid)
                } else {
                    if (material.mActive) {
                        material.mExpectedFlusTime = curTime + maxInactiveTime
                        material.mActive = false
                    }
                }
            } else {
                material.mActive = true
            }
            i++
        }

        lastUpdateKey = if (i < entries.size) entries[i].key else null
    }

    fun applyQueuedOverrides(obj: Any?) {
        TODO("APR: use JVM equivalent – apply cached override data from region cache")
    }

    fun applyOverrideMessage(data: String) {
        TODO("APR: use JVM equivalent – parse LLSD notation and apply material overrides")
    }

    private fun queueOverrideUpdate(id: UUID, side: Int, overrideData: LLGLTFMaterial) {
        // Intentionally no-op: the region override cache is the authoritative source
    }
}

// Global singleton instance mirroring C++ `gGLTFMaterialList`
val gGLTFMaterialList = LLGLTFMaterialList()
