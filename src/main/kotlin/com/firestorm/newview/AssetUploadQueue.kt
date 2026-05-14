package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llinventory.AssetTypeId

// Reconstructed from indra/test/llassetuploadqueue_tut.cpp — the original
// llassetuploadqueue.h/.cpp were not present in the source tree.

// ── Upload request ────────────────────────────────────────────────────────────

data class AssetUploadRequest(
    val filename: String,
    val taskId: LLUUID,
    val itemId: LLUUID,
    val isRunning: Boolean,
    val isTargetMono: Boolean,
    val queueId: LLUUID
)

// ── Responder (HTTP callbacks) ────────────────────────────────────────────────

open class AssetUploadResponder(
    protected val postData: Map<String, Any> = emptyMap(),
    protected val vfileId: LLUUID = LLUUID.NULL,
    protected val assetType: AssetTypeId = -1,
    protected val filename: String = ""
) {
    open fun httpFailure() {}
    open fun httpSuccess() {}
    open fun uploadUpload(content: Map<String, Any>) {}
    open fun uploadComplete(content: Map<String, Any>) {}
    open fun uploadFailure(content: Map<String, Any>) {}
}

open class UpdateTaskInventoryResponder(
    postData: Map<String, Any> = emptyMap(),
    vfileId: LLUUID = LLUUID.NULL,
    assetType: AssetTypeId = -1,
    filename: String = "",
    val queueId: LLUUID = LLUUID.NULL
) : AssetUploadResponder(postData, vfileId, assetType, filename) {
    override fun uploadComplete(content: Map<String, Any>) {
        System.err.println("UpdateTaskInventoryResponder: uploadComplete not yet implemented")
    }
}

// ── Queue supplier interface ──────────────────────────────────────────────────

interface AssetUploadQueueSupplier {
    fun get(): AssetUploadQueue?
}

// ── Upload queue ──────────────────────────────────────────────────────────────

class AssetUploadQueue(
    private val uploadUrl: String,
    private val supplier: AssetUploadQueueSupplier
) {
    private val pendingUploads: ArrayDeque<AssetUploadRequest> = ArrayDeque()
    private var isUploading: Boolean = false

    fun queue(
        filename: String,
        taskId: LLUUID,
        itemId: LLUUID,
        isRunning: Boolean,
        isTargetMono: Boolean,
        queueId: LLUUID
    ) {
        val request = AssetUploadRequest(filename, taskId, itemId, isRunning, isTargetMono, queueId)
        pendingUploads.addLast(request)
        if (!isUploading) processNext()
    }

    fun isEmpty(): Boolean = pendingUploads.isEmpty() && !isUploading

    private fun processNext() {
        if (pendingUploads.isEmpty()) {
            isUploading = false
            return
        }
        val request = pendingUploads.removeFirst()
        isUploading = true
        uploadRequest(request)
    }

    private fun uploadRequest(request: AssetUploadRequest) {
        TODO("APR: use JVM equivalent - POST $uploadUrl with request payload; on success/failure call onUploadComplete/onUploadFailure")
    }

    fun onUploadComplete(content: Map<String, Any>) {
        isUploading = false
        val q = supplier.get() ?: return
        q.processNext()
    }

    fun onUploadFailure(content: Map<String, Any>) {
        isUploading = false
    }
}
