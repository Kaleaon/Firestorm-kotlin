/**
 * ViewerTexLayer.kt
 * Converted from llviewertexlayer.h / llviewertexlayer.cpp
 *
 * Baked-texture layer classes used for avatar appearance compositing.
 * LLViewerTexLayerSet    → ViewerTexLayerSet
 * LLViewerTexLayerSetBuffer → ViewerTexLayerSetBuffer
 * LLBakedUploadData     → BakedUploadData
 *
 * Heavy OpenGL / upload logic is stubbed with TODO().
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLTimer

// ---------------------------------------------------------------------------
// TextureIndex  (avatar bake targets)
// ---------------------------------------------------------------------------

/**
 * Identifies which bake texture target a layer set writes into.
 * Mirrors LLAvatarAppearanceDefines::ETextureIndex baked entries.
 */
enum class TextureIndex {
    HEAD_BAKED,
    UPPER_BAKED,
    LOWER_BAKED,
    EYES_BAKED,
    SKIRT_BAKED,
    HAIR_BAKED,
    LEFT_ARM_BAKED,
    LEFT_LEG_BAKED,
    AUX1_BAKED,
    AUX2_BAKED,
    AUX3_BAKED
}

// ---------------------------------------------------------------------------
// ViewerTexLayer  (open class — mirrors LLViewerTexLayer)
// ---------------------------------------------------------------------------

/**
 * A single texture layer that contributes to a baked composite.
 * In C++ this derives from LLTexLayer; here we model it as a standalone
 * open class so subclasses (e.g. skin, clothing overlays) can extend it.
 *
 * @param layerSet The owning [ViewerTexLayerSet], typed as [Any?] to avoid a
 *                 circular dependency until a shared base module is available.
 * @param info     The layer definition / parameter block (opaque [Any?] stub).
 */
open class ViewerTexLayer(
    val layerSet: Any?,
    val info: Any?
) {
    /**
     * Render this layer's contribution into the current bake target.
     * The full OpenGL rendering pipeline is not yet ported.
     */
    open fun renderLayer() {
        // no-op
    }
}

// ---------------------------------------------------------------------------
// ViewerTexLayerSet  (mirrors LLViewerTexLayerSet)
// ---------------------------------------------------------------------------

/**
 * An ordered collection of [ViewerTexLayer] objects that get composited into
 * a single baked texture for one body region of the avatar.
 *
 * Only exists for the agent's own avatar (self).
 *
 * @param appearance The avatar appearance object that owns this layer set
 *                   (typed [Any?] to avoid pulling in a full appearance dep).
 */
class ViewerTexLayerSet(val appearance: Any?) {

    /** Whether composite updates are permitted to run. */
    var updatesEnabled: Boolean = false
        private set

    /** The composite render target that this layer set writes into. */
    private var composite: ViewerTexLayerSetBuffer? = null

    /** The ordered list of layers in this set. */
    private val layers: MutableList<ViewerTexLayer> = mutableListOf()

    // -----------------------------------------------------------------------
    // Layer management
    // -----------------------------------------------------------------------

    fun addLayer(layer: ViewerTexLayer) {
        layers.add(layer)
    }

    fun getLayers(): List<ViewerTexLayer> = layers

    // -----------------------------------------------------------------------
    // Composite management
    // -----------------------------------------------------------------------

    /**
     * Enable or disable composite updates for this layer set.
     * When disabled, calls to [requestUpdate] are no-ops.
     */
    fun setUpdatesEnabled(enabled: Boolean) {
        updatesEnabled = enabled
    }

    /**
     * Ensure a [ViewerTexLayerSetBuffer] exists for this layer set,
     * creating one if necessary.  The width/height come from the layer-set
     * definition info (stubbed here).
     */
    fun createComposite(width: Int = 512, height: Int = 512) {
        if (composite == null) {
            composite = ViewerTexLayerSetBuffer(owner = this, width = width, height = height)
        }
    }

    fun getViewerComposite(): ViewerTexLayerSetBuffer? = composite

    fun hasComposite(): Boolean = composite != null

    // -----------------------------------------------------------------------
    // Update / upload requests
    // -----------------------------------------------------------------------

    /**
     * Ask the composite buffer to schedule a local re-render.
     * No-op when [updatesEnabled] is false.
     */
    fun requestUpdate() {
        if (updatesEnabled) {
            createComposite()
            composite?.requestUpdate()
        }
    }

    /** Force an immediate synchronous update of the composite. */
    fun updateComposite() {
        createComposite()
        composite?.requestUpdateImmediate()
    }

    /**
     * Ask the composite buffer to upload the baked texture to the server
     * (legacy bake path).
     */
    fun requestUpload() {
        createComposite()
        composite?.requestUpload()
    }

    /** Cancel any pending upload for this layer set. */
    fun cancelUpload() {
        composite?.cancelUpload()
    }

    // -----------------------------------------------------------------------
    // Texture data readiness queries
    // -----------------------------------------------------------------------

    /**
     * Returns true if at least one packet of data has been received for each
     * local texture that this layer set depends on.
     */
    fun isLocalTextureDataAvailable(): Boolean {
        return false
    }

    /**
     * Returns true if all texture data that this layer set depends on has
     * fully arrived at the highest available LOD.
     */
    fun isLocalTextureDataFinal(): Boolean {
        return false
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Render all layers in this set into the composite buffer.
     */
    fun renderLayerSet() {
        // no-op
    }
}

// ---------------------------------------------------------------------------
// ViewerTexLayerSetBuffer  (mirrors LLViewerTexLayerSetBuffer)
// ---------------------------------------------------------------------------

/**
 * The off-screen render target (GL texture) that a [ViewerTexLayerSet] writes
 * into.  Handles both local updates (for mesh texture display) and server
 * uploads (legacy bake path).
 *
 * @param owner  The [ViewerTexLayerSet] that owns this buffer.
 * @param width  Pixel width of the composite texture.
 * @param height Pixel height of the composite texture.
 */
class ViewerTexLayerSetBuffer(
    val owner: ViewerTexLayerSet,
    val width: Int,
    val height: Int
) {
    // -----------------------------------------------------------------------
    // Update state  (mirrors mNeedsUpdate, mNumLowresUpdates, etc.)
    // -----------------------------------------------------------------------

    private var needsUpdate: Boolean = true
    private var numLowresUpdates: UInt = 0u
    private val needsUpdateTimer: LLTimer = LLTimer()

    // -----------------------------------------------------------------------
    // Upload state  (legacy bake path; mirrors mNeedsUpload, mUploadID, etc.)
    // -----------------------------------------------------------------------

    private var needsUpload: Boolean = false
    private var uploadPendingFlag: Boolean = false
    private var numLowresUploads: UInt = 0u
    private var uploadFailCount: Int = 0
    private var uploadId: LLUUID = LLUUID.NULL
    private val needsUploadTimer: LLTimer = LLTimer()
    private val uploadRetryTimer: LLTimer = LLTimer()

    // -----------------------------------------------------------------------
    // GL resource tracking  (static byte counter mirrors sGLByteCount)
    // -----------------------------------------------------------------------

    companion object {
        private var glByteCount: Int = 0

        fun dumpTotalByteCount() {
            println("Composite System GL Buffers: ${glByteCount / 1024} KB")
        }

        /**
         * Callback invoked when a baked texture upload completes on the server.
         * Mirrors LLViewerTexLayerSetBuffer::onTextureUploadComplete.
         */
        fun onTextureUploadComplete(
            uuid: LLUUID,
            uploadData: BakedUploadData?,
            result: Int
        ) {
            System.err.println("ViewerTexLayerSetBuffer: onTextureUploadComplete not yet implemented")
        }
    }

    // -----------------------------------------------------------------------
    // Update path
    // -----------------------------------------------------------------------

    /** Schedule a local re-render of this composite buffer. */
    fun requestUpdate() {
        needsUpdate = true
        numLowresUpdates = 0u
        uploadId = LLUUID.NULL        // discard any in-flight upload
        restartUpdateTimer()
    }

    /** Attempt an immediate synchronous render/update. Returns true on success. */
    fun requestUpdateImmediate(): Boolean {
        needsUpdate = true
        if (!needsRender()) return false
        return renderLayerSetBuffer()
    }

    private fun restartUpdateTimer() {
        needsUpdateTimer.reset()
    }

    /**
     * Returns true if the buffer is ready to perform a local update render.
     * Mirrors isReadyToUpdate() logic from C++.
     */
    fun isReadyToUpdate(): Boolean {
        if (owner.isLocalTextureDataFinal()) return true
        if (numLowresUpdates == 0u) return true
        val textureTimeout = 10u
        val timedOut = needsUpdateTimer.getElapsedTimeF32() >= textureTimeout.toFloat()
        return timedOut
    }

    // -----------------------------------------------------------------------
    // Upload path  (legacy bake)
    // -----------------------------------------------------------------------

    /** Request a new baked texture upload to the server. */
    fun requestUpload() {
        conditionalRestartUploadTimer()
        needsUpload = true
        numLowresUploads = 0u
        uploadPendingFlag = true
    }

    /** Abandon any pending or in-progress upload. */
    fun cancelUpload() {
        needsUpload = false
        uploadPendingFlag = false
        needsUploadTimer.pause()
        uploadRetryTimer.reset()
    }

    fun uploadNeeded(): Boolean = needsUpload
    fun uploadInProgress(): Boolean = !uploadId.isNull()
    fun uploadPending(): Boolean = uploadPendingFlag

    private fun conditionalRestartUploadTimer() {
        if (needsUpload && numLowresUploads == 0u) {
            needsUploadTimer.unpause()
        } else {
            needsUploadTimer.reset()
        }
    }

    /**
     * Performs the actual GL read-back, J2C compression, and asset upload.
     * The full implementation requires access to the OpenGL pipeline.
     */
    fun doUpload() {
        System.err.println("ViewerTexLayerSetBuffer: doUpload not yet implemented")
    }

    // -----------------------------------------------------------------------
    // Dynamic texture / render interface
    // -----------------------------------------------------------------------

    /**
     * Returns true if the buffer should be re-rendered this frame.
     * Mirrors LLViewerTexLayerSetBuffer::needsRender().
     */
    fun needsRender(): Boolean {
        val uploadNow = needsUpload && isReadyToUpload()
        val updateNow = needsUpdate && isReadyToUpdate()
        return uploadNow || updateNow
    }

    private fun isReadyToUpload(): Boolean {
        if (!owner.isLocalTextureDataFinal()) return false
        return uploadFailCount == 0 ||
            uploadRetryTimer.getElapsedTimeF32() >= 2.0f * (1 shl (uploadFailCount - 1))
    }

    /** Composite render — returns true on success. */
    fun renderLayerSetBuffer(): Boolean {
        return false
    }

    fun isInitialized(): Boolean {
        return false
    }

    fun restoreGLTexture() {
        // no-op
    }

    fun destroyGLTexture() {
        // no-op
    }

    fun dumpTextureInfo(): String {
        val status = when {
            uploadInProgress() -> "UPLOADING"
            !uploadNeeded()    -> "DONE     "
            else               -> "CREATING "
        }
        return "[$status] [HiRes:${!needsUpload} LoRes:$numLowresUploads]"
    }
}

// ---------------------------------------------------------------------------
// BakedUploadData  (mirrors LLBakedUploadData)
// ---------------------------------------------------------------------------

/**
 * Payload attached to an in-flight baked texture upload so that the
 * completion callback can route the result back to the correct layer set.
 *
 * @param avatar       Weak back-reference to the avatar self (not owned).
 * @param texLayerSet  The layer set that initiated this upload.
 * @param id           Asset ID of the upload in progress.
 * @param isHighestRes Whether this is the final full-resolution bake.
 * @param startTime    Wall-clock timestamp when the upload was initiated.
 */
data class BakedUploadData(
    val avatar: Any?,                      // LLVOAvatarSelf* — typed Any? to avoid dep
    val texLayerSet: ViewerTexLayerSet,
    val id: LLUUID,
    val isHighestRes: Boolean,
    val startTime: Long = System.currentTimeMillis()
)
