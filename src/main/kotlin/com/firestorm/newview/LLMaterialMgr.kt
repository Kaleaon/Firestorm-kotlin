package com.firestorm.newview

import java.util.UUID

typealias LLMaterialID = UUID
typealias LLMaterialPtr = LLMaterial?

class LLMaterial(val data: Map<String, Any> = emptyMap()) {
    fun isNull(): Boolean = false
    fun asLLSD(): Map<String, Any> = data
    companion object {
        val null_ = LLMaterial()
    }
}

object LLMaterialMgr {

    private const val MATERIALS_CAPABILITY_NAME = "RenderMaterials"
    private const val MATERIALS_GET_TIMEOUT = 60f * 20
    private const val MATERIALS_POST_TIMEOUT = 60f * 5
    private const val MATERIALS_GET_MAX_ENTRIES = 50
    private const val MATERIALS_PUT_MAX_ENTRIES = 50

    private data class TEMaterialPair(val te: UInt, val materialID: LLMaterialID)

    private val mMaterials: MutableMap<LLMaterialID, LLMaterialPtr> = mutableMapOf(UUID(0, 0) to null)

    private val mGetQueue: MutableMap<UUID, MutableSet<LLMaterialID>> = mutableMapOf()
    private val mRegionGets: MutableSet<UUID> = mutableSetOf()
    private val mGetPending: MutableMap<Pair<UUID, LLMaterialID>, Double> = mutableMapOf()
    private val mGetCallbacks: MutableMap<LLMaterialID, MutableList<(LLMaterialID, LLMaterialPtr) -> Unit>> = mutableMapOf()
    private val mGetTECallbacks: MutableMap<TEMaterialPair, MutableList<(LLMaterialID, LLMaterialPtr, UInt) -> Unit>> = mutableMapOf()
    private val mGetAllQueue: MutableSet<UUID> = mutableSetOf()
    private val mGetAllRequested: MutableSet<UUID> = mutableSetOf()
    private val mGetAllPending: MutableMap<UUID, Double> = mutableMapOf()
    private val mGetAllCallbacks: MutableMap<UUID, MutableList<(UUID, MutableMap<LLMaterialID, LLMaterialPtr>) -> Unit>> = mutableMapOf()
    private val mPutQueue: MutableMap<UUID, MutableMap<UByte, LLMaterial>> = mutableMapOf()

    fun get(regionId: UUID, materialId: LLMaterialID): LLMaterialPtr {
        if (mMaterials.containsKey(materialId)) return mMaterials[materialId]
        if (!isGetPending(regionId, materialId)) {
            mGetQueue.getOrPut(regionId) { mutableSetOf() }.add(materialId)
            markGetPending(regionId, materialId)
        }
        return null
    }

    fun get(
        regionId: UUID,
        materialId: LLMaterialID,
        cb: (LLMaterialID, LLMaterialPtr) -> Unit
    ): (() -> Unit)? {
        if (mMaterials.containsKey(materialId)) {
            cb(materialId, mMaterials[materialId])
            return null
        }
        if (!isGetPending(regionId, materialId)) {
            mGetQueue.getOrPut(regionId) { mutableSetOf() }.add(materialId)
            markGetPending(regionId, materialId)
        }
        val list = mGetCallbacks.getOrPut(materialId) { mutableListOf() }
        list.add(cb)
        return { list.remove(cb) }
    }

    fun getTE(
        regionId: UUID,
        materialId: LLMaterialID,
        te: UInt,
        cb: (LLMaterialID, LLMaterialPtr, UInt) -> Unit
    ): (() -> Unit)? {
        if (mMaterials.containsKey(materialId)) {
            cb(materialId, mMaterials[materialId], te)
            return null
        }
        if (!isGetPending(regionId, materialId)) {
            mGetQueue.getOrPut(regionId) { mutableSetOf() }.add(materialId)
            markGetPending(regionId, materialId)
        }
        val key = TEMaterialPair(te, materialId)
        val list = mGetTECallbacks.getOrPut(key) { mutableListOf() }
        list.add(cb)
        return { list.remove(cb) }
    }

    fun getAll(regionId: UUID) {
        if (!isGetAllPending(regionId)) {
            mGetAllQueue.add(regionId)
        }
    }

    fun getAll(regionId: UUID, cb: (UUID, MutableMap<LLMaterialID, LLMaterialPtr>) -> Unit): (() -> Unit)? {
        if (!isGetAllPending(regionId)) {
            mGetAllQueue.add(regionId)
        }
        val list = mGetAllCallbacks.getOrPut(regionId) { mutableListOf() }
        list.add(cb)
        return { list.remove(cb) }
    }

    fun put(objectId: UUID, te: UByte, material: LLMaterial) {
        mPutQueue.getOrPut(objectId) { mutableMapOf() }[te] = material
    }

    fun remove(objectId: UUID, te: UByte) {
        put(objectId, te, LLMaterial.null_)
    }

    fun setLocalMaterial(regionId: UUID, materialPtr: LLMaterialPtr) {
        var materialId: LLMaterialID
        do { materialId = UUID.randomUUID() } while (mMaterials.containsKey(materialId))
        mMaterials[materialId] = materialPtr
        setMaterialCallbacks(materialId, materialPtr)
        mGetPending.remove(Pair(regionId, materialId))
    }

    fun onIdle() {
        if (mGetQueue.isNotEmpty()) processGetQueue()
        if (mGetAllQueue.isNotEmpty()) processGetAllQueue()
        if (mPutQueue.isNotEmpty()) processPutQueue()
        TODO("APR: use JVM equivalent — mHttpRequest.update(0)")
    }

    private fun isGetPending(regionId: UUID, materialId: LLMaterialID): Boolean {
        val timestamp = mGetPending[Pair(regionId, materialId)] ?: return false
        return currentTimeSeconds() < timestamp + MATERIALS_POST_TIMEOUT
    }

    private fun isGetAllPending(regionId: UUID): Boolean {
        val timestamp = mGetAllPending[regionId] ?: return false
        return currentTimeSeconds() < timestamp + MATERIALS_GET_TIMEOUT
    }

    private fun markGetPending(regionId: UUID, materialId: LLMaterialID) {
        mGetPending[Pair(regionId, materialId)] = currentTimeSeconds()
    }

    private fun setMaterial(regionId: UUID, materialId: LLMaterialID, materialData: Map<String, Any>): LLMaterialPtr {
        val ptr = mMaterials.getOrPut(materialId) { LLMaterial(materialData) }
        setMaterialCallbacks(materialId, ptr)
        mGetPending.remove(Pair(regionId, materialId))
        return ptr
    }

    private fun setMaterialCallbacks(materialId: LLMaterialID, materialPtr: LLMaterialPtr) {
        val maxTes = 45
        for (i in 0 until maxTes) {
            val key = TEMaterialPair(i.toUInt(), materialId)
            val cbs = mGetTECallbacks.remove(key) ?: continue
            for (cb in cbs) cb(materialId, materialPtr, i.toUInt())
            if (mGetTECallbacks.isEmpty()) break
        }
        val cbs = mGetCallbacks.remove(materialId) ?: return
        for (cb in cbs) cb(materialId, materialPtr)
    }

    private fun processGetQueue() {
        TODO("APR: use JVM equivalent — HTTP POST to RenderMaterials cap for each pending region")
    }

    private fun processGetAllQueue() {
        for (regionId in mGetAllQueue.toList()) {
            mGetAllPending[regionId] = currentTimeSeconds()
            mGetAllQueue.remove(regionId)
            TODO("APR: use JVM equivalent — HTTP GET all materials for region $regionId via coroutine")
        }
    }

    private fun processPutQueue() {
        TODO("APR: use JVM equivalent — HTTP PUT material faces to RenderMaterials cap per region")
    }

    fun onGetResponse(success: Boolean, content: Map<String, Any>, regionId: UUID) {
        if (!success) return
        TODO("APR: use JVM equivalent — unzip LLSD binary, iterate materials, call setMaterial")
    }

    fun onGetAllResponse(success: Boolean, content: Map<String, Any>, regionId: UUID) {
        if (!success) return
        TODO("APR: use JVM equivalent — unzip LLSD binary, set all materials, fire getAll callbacks")
    }

    fun onPutResponse(success: Boolean, content: Map<String, Any>) {
        if (!success) return
        TODO("APR: use JVM equivalent — unzip LLSD binary, validate face material response")
    }

    fun onRegionRemoved(regionId: UUID) {
        clearGetQueues(regionId)
    }

    private fun clearGetQueues(regionId: UUID) {
        mGetQueue.remove(regionId)
        mGetPending.keys.removeAll { it.first == regionId }
        mGetAllQueue.remove(regionId)
        mGetAllRequested.remove(regionId)
        mGetAllPending.remove(regionId)
        mGetAllCallbacks.remove(regionId)
    }

    private fun getMaxEntries(regionId: UUID): UInt = 50u

    private fun currentTimeSeconds(): Double =
        System.currentTimeMillis().toDouble() / 1000.0
}
