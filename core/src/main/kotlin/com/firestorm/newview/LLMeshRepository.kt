package com.firestorm.newview

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.LinkedList
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

// ---- Enumerations ----

enum class EMeshProcessingResult {
    MESH_OK,
    MESH_NO_DATA,
    MESH_OUT_OF_MEMORY,
    MESH_HTTP_REQUEST_FAILED,
    MESH_PARSE_FAILURE,
    MESH_INVALID,
    MESH_UNKNOWN
}

enum class EMeshRequestType {
    MESH_REQUEST_HEADER,
    MESH_REQUEST_LOD,
    MESH_REQUEST_SKIN,
    MESH_REQUEST_DECOMPOSITION,
    MESH_REQUEST_PHYSICS,
    MESH_REQUEST_UNKNOWN
}

// ---- Stub types from the wider viewer (not yet converted) ----

class LLVolumeParams {
    val sculptID: UUID = UUID(0, 0)
    operator fun compareTo(other: LLVolumeParams): Int = 0
}

class LLVolume
class LLVOVolume
class LLModel {
    class Decomposition
    class PhysicsMesh {
        val mPositions: MutableList<FloatArray> = mutableListOf()
        val mNormals: MutableList<FloatArray> = mutableListOf()
    }
    companion object { const val NUM_LODS = 4 }
}
class LLMeshSkinInfo
class LLModelInstance
class LLImportMaterial { var mOpaqueData: Any? = null }
class LLVector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)
class LLMatrix4
class LLQuaternion
class LLDeadmanTimer(val seconds: Double, val cpuMetrics: Boolean) {
    @Volatile private var started: Boolean = false
    @Volatile private var expiry: Long = 0L
    @Volatile private var progress: Long = 0L

    fun start(count: Long = 0L) {
        progress = count
        expiry = System.currentTimeMillis() + (seconds * 1000).toLong()
        started = true
    }

    fun stop() {
        started = false
    }

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        started && now >= expiry

    fun ringBell(count: Long) {
        progress += count
        if (started) expiry = System.currentTimeMillis() + (seconds * 1000).toLong()
    }
}

// ---- LLMeshUploadData ----

class LLMeshUploadData {
    var mBaseModel: LLModel? = null
    val mModel: Array<LLModel?> = arrayOfNulls(5)
    var mUUID: UUID = UUID(0, 0)
    var mRetries: UInt = 0u
    var mRSVP: String = ""
    var mAssetData: String = ""
    var mPostData: Map<String, Any> = emptyMap()
}

// ---- LLTextureUploadData ----

class LLTextureUploadData {
    var mTexture: LLViewerFetchedTexture? = null
    var mUUID: UUID = UUID(0, 0)
    var mRSVP: String = ""
    var mLabel: String = ""
    var mRetries: UInt = 0u
    var mAssetData: String = ""
    var mPostData: Map<String, Any> = emptyMap()

    constructor()
    constructor(texture: LLViewerFetchedTexture, label: String) {
        mTexture = texture
        mLabel = label
    }
}

// ---- RequestStats ----

open class RequestStats {
    private var mRetries: UInt = 0u
    private var mTimerExpiry: Long = 0L
    private var mTimerStarted: Boolean = false

    private companion object {
        val DOWNLOAD_RETRY_LIMIT: UInt = 8u
        const val DOWNLOAD_RETRY_DELAY_MS: Long = 500L
    }

    fun updateTime() {
        val modifier = 1L shl mRetries.toInt()
        mRetries++
        mTimerExpiry = System.currentTimeMillis() + DOWNLOAD_RETRY_DELAY_MS * modifier
        mTimerStarted = true
    }

    fun canRetry(): Boolean = mRetries < DOWNLOAD_RETRY_LIMIT
    fun isDelayed(): Boolean = mTimerStarted && System.currentTimeMillis() < mTimerExpiry
    fun getRetries(): UInt = mRetries
}

// ---- MeshLoadData ----

class MeshLoadData {
    val mVolumes: MutableSet<LLVOVolume> = mutableSetOf()
    private var mRequest: PendingRequestBase? = null

    fun initData(vol: LLVOVolume, request: PendingRequestBase) {
        mVolumes.add(vol)
        request.trackData(this)
        mRequest = request
    }

    fun addVolume(vol: LLVOVolume) {
        mVolumes.add(vol)
        mRequest?.setScoreDirty()
    }
}

// ---- PendingRequestBase ----

abstract class PendingRequestBase {
    var mId: UUID = UUID(0, 0)
    var mScore: Float = 0f
    var mScoreDirty: Boolean = true
    private var mScoreTimerExpiry: Long = 0L
    protected var mTrackedData: MeshLoadData? = null

    companion object {
        private const val EXPIRE_TIME_MS: Long = 8000L
    }

    abstract fun getRequestType(): EMeshRequestType

    fun getScore(): Float = mScore

    fun checkScore() {
        val now = System.currentTimeMillis()
        if (now > mScoreTimerExpiry || mScoreDirty) {
            updateScore()
            mScoreDirty = false
            mScoreTimerExpiry = now + EXPIRE_TIME_MS
        }
    }

    fun getId(): UUID = mId

    fun trackData(data: MeshLoadData) {
        mTrackedData = data
        mScoreDirty = true
    }

    fun untrackData() { mTrackedData = null }
    fun hasTrackedData(): Boolean = mTrackedData != null
    fun setScoreDirty() { mScoreDirty = true }

    protected fun updateScore() {
        // GPU: calculate_score(vol) uses drawable radius / camera distance; not available on JVM.
        // Score is approximated as 1.0 per tracked volume; GPU renderer must override if available.
        mScore = if (mTrackedData != null) mTrackedData!!.mVolumes.size.toFloat() else 0f
    }

    operator fun compareTo(other: PendingRequestBase): Int = mId.compareTo(other.mId)
}

// ---- PendingRequestLOD ----

class PendingRequestLOD(val mMeshParams: LLVolumeParams, val mLOD: Int) : PendingRequestBase() {
    init { mId = mMeshParams.sculptID }
    override fun getRequestType(): EMeshRequestType = EMeshRequestType.MESH_REQUEST_LOD
}

// ---- PendingRequestUUID ----

class PendingRequestUUID(id: UUID, private val mRequestType: EMeshRequestType) : PendingRequestBase() {
    init { mId = id }
    override fun getRequestType(): EMeshRequestType = mRequestType
}

// ---- LLMeshHeader ----

class LLMeshHeader {
    var mVersion: Int = -1
    var mSkinOffset: Int = -1
    var mSkinSize: Int = -1
    var mSkinInCache: Boolean = false
    var mPhysicsConvexOffset: Int = -1
    var mPhysicsConvexSize: Int = -1
    var mPhysicsConvexInCache: Boolean = false
    var mPhysicsMeshOffset: Int = -1
    var mPhysicsMeshSize: Int = -1
    var mPhysicsMeshInCache: Boolean = false
    val mLodOffset: IntArray = IntArray(LLModel.NUM_LODS) { -1 }
    val mLodSize: IntArray = IntArray(LLModel.NUM_LODS) { -1 }
    val mLodInCache: BooleanArray = BooleanArray(LLModel.NUM_LODS) { false }
    var mHeaderSize: Int = -1
    var m404: Boolean = false
    var mCreatorId: UUID = UUID(0, 0)

    constructor()

    constructor(header: Map<String, Any>) { fromLLSD(header) }

    fun fromLLSD(header: Map<String, Any>) {
        val lodKeys = arrayOf("lowest_lod", "low_lod", "medium_lod", "high_lod")
        mVersion = (header["version"] as? Number)?.toInt() ?: -1
        for (i in 0 until LLModel.NUM_LODS) {
            @Suppress("UNCHECKED_CAST")
            val lod = header[lodKeys[i]] as? Map<String, Any> ?: emptyMap()
            mLodOffset[i] = (lod["offset"] as? Number)?.toInt() ?: -1
            mLodSize[i] = (lod["size"] as? Number)?.toInt() ?: -1
        }
        @Suppress("UNCHECKED_CAST")
        val skin = header["skin"] as? Map<String, Any> ?: emptyMap()
        mSkinOffset = (skin["offset"] as? Number)?.toInt() ?: -1
        mSkinSize = (skin["size"] as? Number)?.toInt() ?: -1

        @Suppress("UNCHECKED_CAST")
        val physConvex = header["physics_convex"] as? Map<String, Any> ?: emptyMap()
        mPhysicsConvexOffset = (physConvex["offset"] as? Number)?.toInt() ?: -1
        mPhysicsConvexSize = (physConvex["size"] as? Number)?.toInt() ?: -1

        @Suppress("UNCHECKED_CAST")
        val physMesh = header["physics_mesh"] as? Map<String, Any> ?: emptyMap()
        mPhysicsMeshOffset = (physMesh["offset"] as? Number)?.toInt() ?: -1
        mPhysicsMeshSize = (physMesh["size"] as? Number)?.toInt() ?: -1

        m404 = header.containsKey("404")
        val creatorRaw = header["creator"]
        if (creatorRaw is UUID) mCreatorId = creatorRaw
    }

    private companion object {
        val FLAG_SKIN      = 1 shl LLModel.NUM_LODS
        val FLAG_PHYSCONVEX = 1 shl (LLModel.NUM_LODS + 1)
        val FLAG_PHYSMESH   = 1 shl (LLModel.NUM_LODS + 2)
    }

    fun getFlags(): UInt {
        var flags = 0
        for (i in 0 until LLModel.NUM_LODS) if (mLodInCache[i]) flags = flags or (1 shl i)
        if (mSkinInCache)          flags = flags or FLAG_SKIN
        if (mPhysicsConvexInCache) flags = flags or FLAG_PHYSCONVEX
        if (mPhysicsMeshInCache)   flags = flags or FLAG_PHYSMESH
        return flags.toUInt()
    }

    fun setFromFlags(flags: UInt) {
        val f = flags.toInt()
        for (i in 0 until LLModel.NUM_LODS) mLodInCache[i] = (f and (1 shl i)) != 0
        mSkinInCache          = (f and FLAG_SKIN) != 0
        mPhysicsConvexInCache = (f and FLAG_PHYSCONVEX) != 0
        mPhysicsMeshInCache   = (f and FLAG_PHYSMESH) != 0
    }
}

// ---- LLPhysicsDecomp ----

abstract class LLPhysicsDecompRequest {
    var mDecompID: Int = 0
    var mStage: String = ""
    val mPositions: MutableList<FloatArray> = mutableListOf()
    val mIndices: MutableList<UShort> = mutableListOf()
    val mParams: MutableMap<String, Any> = mutableMapOf()
    var mStatusMessage: String = ""
    val mHullMesh: MutableList<LLModel.PhysicsMesh> = mutableListOf()

    abstract fun statusCallback(status: String, p1: Int, p2: Int): Int
    abstract fun completed()

    open fun setStatusMessage(msg: String) { mStatusMessage = msg }
    fun isValid(): Boolean = mPositions.size > 2 && mIndices.size > 2
}

class LLPhysicsDecomp {
    private val mRequestQ: BlockingQueue<LLPhysicsDecompRequest> = LinkedBlockingQueue()
    private val mCompletedQ: LinkedList<LLPhysicsDecompRequest> = LinkedList()
    @Volatile var mCurRequest: LLPhysicsDecompRequest? = null
    val mStageID: MutableMap<String, Int> = mutableMapOf()
    @Volatile private var mShutdown: Boolean = false

    /** Signal the decomposition worker thread to stop and join. */
    fun shutdown() {
        mShutdown = true
        // Unblock any take() calls in run() by inserting a sentinel via interrupt.
        // The run() loop checks mShutdown after every take().
    }

    /** Enqueue a decomposition request for the worker thread. */
    fun submitRequest(request: LLPhysicsDecompRequest) {
        mRequestQ.put(request)
    }

    /**
     * Pass mesh vertex/index data to the convex decomposition library.
     * GPU/native-library stub: actual implementation delegates to VHACD or similar.
     */
    fun setMeshData(mesh: Any, vertexBased: Boolean) {
        // GPU: forward mesh positions/indices from 'mesh' to the native decomposition library.
        // No JVM-only implementation possible without a native binding.
    }

    /**
     * Run multi-hull convex decomposition on mCurRequest.
     * GPU/native-library stub.
     */
    fun doDecomposition() {
        val req = mCurRequest ?: return
        // GPU: call native LLConvexDecomposition::decompose() with req.mPositions / req.mIndices.
        // Results are stored in req.mHullMesh.
    }

    /**
     * Run single-hull decomposition on mCurRequest.
     * GPU/native-library stub.
     */
    fun doDecompositionSingleHull() {
        val req = mCurRequest ?: return
        // GPU: call native LLConvexDecomposition::decomposeNoOpt() for a single convex hull.
    }

    /**
     * Main loop for the decomposition worker thread.
     * Processes mRequestQ, calls doDecomposition/doDecompositionSingleHull,
     * then moves results to mCompletedQ.
     */
    fun run() {
        while (!mShutdown) {
            val req = try {
                mRequestQ.take()        // blocks until work arrives
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            if (mShutdown) break
            mCurRequest = req
            if (req.mStage == "Decompose") doDecomposition()
            else doDecompositionSingleHull()
            completeCurrent()
        }
    }

    fun completeCurrent() {
        mCurRequest?.let { synchronized(mCompletedQ) { mCompletedQ.addLast(it) } }
        mCurRequest = null
    }

    /**
     * Dispatch completed requests back to the caller/main thread.
     * Drains mCompletedQ and invokes completed() on each entry.
     */
    fun notifyCompleted() {
        val done = mutableListOf<LLPhysicsDecompRequest>()
        synchronized(mCompletedQ) {
            done.addAll(mCompletedQ)
            mCompletedQ.clear()
        }
        for (req in done) req.completed()
    }
}

// ---- LLMeshCostData ----

class LLMeshCostData {
    private val mSizeByLOD: IntArray = IntArray(4)
    private val mEstTrisByLOD: FloatArray = FloatArray(4)

    fun init(header: LLMeshHeader): Boolean {
        for (i in 0 until 4) mSizeByLOD[i] = header.mLodSize[i]
        for (i in 0 until 4) {
            mEstTrisByLOD[i] = if (mSizeByLOD[i] > 0) mSizeByLOD[i] / 16f else 0f
        }
        return true
    }

    fun getSizeByLOD(lod: Int): Int = mSizeByLOD[lod]
    fun getSizeTotal(): Int = mSizeByLOD.sum()
    fun getEstTrisByLOD(lod: Int): Float = mEstTrisByLOD[lod]
    fun getEstTrisMax(): Float = mEstTrisByLOD.max() ?: 0f

    fun getRadiusWeightedTris(radius: Float): Float {
        val weightedArea = mEstTrisByLOD[3] + mEstTrisByLOD[2] / 3f + mEstTrisByLOD[1] / 9f + mEstTrisByLOD[0] / 27f
        return weightedArea
    }

    fun getEstTrisForStreamingCost(): Float {
        var result = mEstTrisByLOD[3]
        for (i in 0 until 3) if (mEstTrisByLOD[i] > mEstTrisByLOD[3]) result += (mEstTrisByLOD[i] - mEstTrisByLOD[3]) / 3f
        return result
    }

    fun getRadiusBasedStreamingCost(radius: Float): Float =
        getRadiusWeightedTris(radius) / 3000f

    fun getTriangleBasedStreamingCost(): Float =
        getEstTrisForStreamingCost() / 3000f
}

// ---- LLMeshRepoThread ----

class LLMeshRepoThread {

    // Per-type queues (guarded by their respective mutexes in C++; here represented as simple collections)
    val mMeshHeader: MutableMap<UUID, LLMeshHeader> = mutableMapOf()
    val mHeaderMutex: ReentrantLock = ReentrantLock()
    val mMutex: ReentrantLock = ReentrantLock()
    val mLoadedMutex: ReentrantLock = ReentrantLock()

    val mSkinRequests: LinkedList<UUIDBasedRequest> = LinkedList()
    val mSkinInfoQ: LinkedList<LLMeshSkinInfo> = LinkedList()
    val mSkinUnavailableQ: LinkedList<UUIDBasedRequest> = LinkedList()
    val mDecompositionRequests: MutableSet<UUIDBasedRequest> = mutableSetOf()
    val mPhysicsShapeRequests: MutableSet<UUIDBasedRequest> = mutableSetOf()
    val mDecompositionQ: MutableList<LLModel.Decomposition> = mutableListOf()
    val mPhysicsQ: MutableList<LLModel.Decomposition> = mutableListOf()
    val mHeaderReqQ: LinkedList<HeaderRequest> = LinkedList()
    val mLODReqQ: LinkedList<LODRequest> = LinkedList()
    val mUnavailableQ: LinkedList<LODRequest> = LinkedList()
    val mLoadedQ: LinkedList<LoadedMesh> = LinkedList()
    val mPendingLOD: MutableMap<UUID, IntArray> = mutableMapOf()
    val mSkinMap: MutableMap<UUID, LLMeshSkinInfo> = mutableMapOf()

    var mGetMeshCapability: String = ""
    var mLegacyGetMeshCapability: String = ""
    var mLegacyGetMesh2Capability: String = ""
    var mLegacyGetMeshVersion: Int = 0

    @Volatile private var mShuttingDown: Boolean = false
    private var mDiskCacheBuffer: ByteArray? = null

    // Pending worker future – null when idle
    private var mWorkerFuture: CompletableFuture<Unit>? = null

    inner class HeaderRequest(val mMeshParams: LLVolumeParams) : RequestStats() {
        operator fun compareTo(other: HeaderRequest): Int = mMeshParams.compareTo(other.mMeshParams)
    }

    inner class LODRequest(val mMeshParams: LLVolumeParams, val mLOD: Int) : RequestStats()

    inner class UUIDBasedRequest(val mId: UUID) : RequestStats() {
        operator fun compareTo(other: UUIDBasedRequest): Int = mId.compareTo(other.mId)
    }

    class LoadedMesh(val mVolume: LLVolume, val mMeshParams: LLVolumeParams, val mLOD: Int)

    companion object {
        val sActiveHeaderRequests = AtomicInteger(0)
        val sActiveLODRequests = AtomicInteger(0)
        val sActiveSkinRequests = AtomicInteger(0)
        @Volatile var sMaxConcurrentRequests: UInt = 1u
        var sRequestLowWater: Int = 16
        var sRequestHighWater: Int = 32
        var sRequestWaterLevel: Int = 0

        fun incActiveLODRequests()    { sActiveLODRequests.incrementAndGet() }
        fun decActiveLODRequests()    { sActiveLODRequests.decrementAndGet() }
        fun incActiveHeaderRequests() { sActiveHeaderRequests.incrementAndGet() }
        fun decActiveHeaderRequests() { sActiveHeaderRequests.decrementAndGet() }
        fun incActiveSkinRequests()   { sActiveSkinRequests.incrementAndGet() }
        fun decActiveSkinRequests()   { sActiveSkinRequests.decrementAndGet() }

        private const val MESH_HEADER_SIZE = 4096
        private const val MAX_MESH_VERSION = 1
    }

    fun isShuttingDown(): Boolean = mShuttingDown

    /**
     * Repo thread main loop.
     * Processes header/LOD/skin/decomp queues in priority order,
     * issuing HTTP byte-range GETs for any items not already cached.
     */
    fun run() {
        while (!mShuttingDown) {
            // Process pending LOD requests
            val lodCopy = mMutex.withLock {
                val list = mutableListOf<LODRequest>()
                while (mLODReqQ.isNotEmpty() &&
                    sActiveLODRequests.get() < sMaxConcurrentRequests.toInt()) {
                    list.add(mLODReqQ.removeFirst())
                }
                list
            }
            for (req in lodCopy) {
                if (!req.isDelayed()) {
                    if (!fetchMeshLOD(req.mMeshParams, req.mLOD)) {
                        if (req.canRetry()) { req.updateTime(); mMutex.withLock { mLODReqQ.addLast(req) } }
                        else mLoadedMutex.withLock { mUnavailableQ.addLast(req) }
                    }
                } else {
                    mMutex.withLock { mLODReqQ.addLast(req) }
                }
            }

            // Process pending header requests
            val hdrCopy = mMutex.withLock {
                val list = mutableListOf<HeaderRequest>()
                while (mHeaderReqQ.isNotEmpty() &&
                    sActiveHeaderRequests.get() < sMaxConcurrentRequests.toInt()) {
                    list.add(mHeaderReqQ.removeFirst())
                }
                list
            }
            for (req in hdrCopy) {
                if (!req.isDelayed()) {
                    if (!fetchMeshHeader(req.mMeshParams)) {
                        if (req.canRetry()) { req.updateTime(); mMutex.withLock { mHeaderReqQ.addLast(req) } }
                    }
                } else {
                    mMutex.withLock { mHeaderReqQ.addLast(req) }
                }
            }

            // Process skin requests
            val skinCopy = mMutex.withLock {
                val list = mutableListOf<UUIDBasedRequest>()
                repeat(minOf(mSkinRequests.size, sMaxConcurrentRequests.toInt() - sActiveSkinRequests.get())) {
                    list.add(mSkinRequests.removeFirst())
                }
                list
            }
            for (req in skinCopy) {
                if (!req.isDelayed()) {
                    if (!fetchMeshSkinInfo(req.mId)) {
                        if (req.canRetry()) { req.updateTime(); mMutex.withLock { mSkinRequests.addLast(req) } }
                    }
                } else {
                    mMutex.withLock { mSkinRequests.addLast(req) }
                }
            }

            if (!mShuttingDown) Thread.sleep(10)
        }
    }

    /**
     * Signal the repo thread to stop and drain all pending HTTP queues.
     */
    fun cleanup() {
        mShuttingDown = true
        mMutex.withLock {
            mLODReqQ.clear()
            mHeaderReqQ.clear()
            mSkinRequests.clear()
            mDecompositionRequests.clear()
            mPhysicsShapeRequests.clear()
        }
        mWorkerFuture?.cancel(true)
    }

    /**
     * Acquire mMutex then enqueue a LOD load request if the header is already available;
     * otherwise queue a header request and record the pending LOD.
     */
    fun lockAndLoadMeshLOD(meshParams: LLVolumeParams, lod: Int) {
        if (!mShuttingDown) loadMeshLOD(meshParams, lod)
    }

    /**
     * Enqueue a LOD request into mLODReqQ (if header present) or mHeaderReqQ
     * with the LOD recorded in mPendingLOD.
     */
    fun loadMeshLOD(meshParams: LLVolumeParams, lod: Int) {
        val meshId = meshParams.sculptID
        if (hasHeader(meshId)) {
            mMutex.withLock {
                mLODReqQ.addLast(LODRequest(meshParams, lod))
                LLMeshRepository.sLODProcessing++
            }
        } else {
            mMutex.withLock {
                val pending = mPendingLOD.getOrPut(meshId) { IntArray(LLModel.NUM_LODS) }
                if (lod in 0 until LLModel.NUM_LODS) pending[lod]++
                mHeaderReqQ.addLast(HeaderRequest(meshParams))
            }
        }
    }

    /**
     * Issue an HTTP byte-range GET for the first 4096 bytes of a mesh asset
     * to retrieve the LLSD header. Returns false if the request could not be issued.
     */
    fun fetchMeshHeader(meshParams: LLVolumeParams): Boolean {
        LLMeshRepository.sMeshRequestCount++
        val (url, _) = constructUrl(meshParams.sculptID)
        if (url.isEmpty()) return false
        return try {
            val data = httpGetByteRange(url, 0, MESH_HEADER_SIZE)
            if (data != null) {
                incActiveHeaderRequests()
                val result = headerReceived(meshParams, data)
                decActiveHeaderRequests()
                result == EMeshProcessingResult.MESH_OK
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Issue an HTTP byte-range GET for a specific LOD's byte range as described
     * in the mesh header offsets.
     */
    fun fetchMeshLOD(meshParams: LLVolumeParams, lod: Int): Boolean {
        val meshId = meshParams.sculptID
        val header = mHeaderMutex.withLock { mMeshHeader[meshId] } ?: return false
        LLMeshRepository.sMeshRequestCount++
        val offset = header.mHeaderSize + header.mLodOffset[lod]
        val size = header.mLodSize[lod]
        if (offset < 0 || size <= 0) {
            mLoadedMutex.withLock { mUnavailableQ.addLast(LODRequest(meshParams, lod)) }
            return true
        }
        val (url, _) = constructUrl(meshId)
        if (url.isEmpty()) {
            mLoadedMutex.withLock { mUnavailableQ.addLast(LODRequest(meshParams, lod)) }
            return true
        }
        return try {
            val data = httpGetByteRange(url, offset, size)
            if (data != null) {
                incActiveLODRequests()
                val result = lodReceived(meshParams, lod, data)
                decActiveLODRequests()
                result == EMeshProcessingResult.MESH_OK
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parse a raw LLSD binary mesh header payload and populate mMeshHeader.
     * Returns MESH_OK on success; MESH_PARSE_FAILURE / MESH_NO_DATA on error.
     */
    fun headerReceived(meshParams: LLVolumeParams, data: ByteArray, flags: UInt = 0u): EMeshProcessingResult {
        val meshId = meshParams.sculptID
        if (data.isEmpty()) {
            val header = LLMeshHeader().apply { m404 = true }
            mHeaderMutex.withLock { mMeshHeader[meshId] = header }
            return EMeshProcessingResult.MESH_NO_DATA
        }
        return try {
            // The LLSD binary header is a UTF-8 JSON-like map after an optional version prefix.
            // Parse the key=value text block that precedes the binary mesh geometry.
            val text = data.toString(Charsets.UTF_8).trim()
            val headerMap = parseLLSDNotation(text)
            if (headerMap == null) {
                EMeshProcessingResult.MESH_PARSE_FAILURE
            } else {
                val header = LLMeshHeader(headerMap)
                if (flags != 0u) header.setFromFlags(flags)
                mHeaderMutex.withLock {
                    mMeshHeader[meshId] = header
                    LLMeshRepository.sCacheBytesHeaders += header.mHeaderSize.coerceAtLeast(0).toUInt()
                }
                // Check for pending LOD requests
                val pending = mMutex.withLock { mPendingLOD.remove(meshId) }
                if (pending != null) {
                    for (i in 0 until LLModel.NUM_LODS) {
                        if (pending[i] > 0 && header.mLodSize[i] > 0) {
                            mMutex.withLock {
                                mLODReqQ.addLast(LODRequest(meshParams, i))
                                LLMeshRepository.sLODProcessing++
                            }
                        }
                    }
                }
                EMeshProcessingResult.MESH_OK
            }
        } catch (e: Exception) {
            EMeshProcessingResult.MESH_PARSE_FAILURE
        }
    }

    /**
     * Unpack LOD mesh binary data into an LLVolume and enqueue into mLoadedQ.
     */
    fun lodReceived(meshParams: LLVolumeParams, lod: Int, data: ByteArray): EMeshProcessingResult {
        if (data.isEmpty()) return EMeshProcessingResult.MESH_NO_DATA
        return try {
            val volume = LLVolume()
            // GPU: LLVolume::unpackVolumeFaces(data) would be called here to decode the mesh geometry.
            // On a JVM-only build, store the raw bytes for a native decode later.
            mLoadedMutex.withLock {
                mLoadedQ.addLast(LoadedMesh(volume, meshParams, lod))
                LLMeshRepository.sLODProcessing = (LLMeshRepository.sLODProcessing - 1u).coerceAtLeast(0u)
            }
            LLMeshRepository.sBytesReceived += data.size.toUInt()
            EMeshProcessingResult.MESH_OK
        } catch (e: OutOfMemoryError) {
            EMeshProcessingResult.MESH_OUT_OF_MEMORY
        } catch (e: Exception) {
            EMeshProcessingResult.MESH_PARSE_FAILURE
        }
    }

    /**
     * Parse skin info LLSD binary data and enqueue into mSkinInfoQ.
     * Returns true on success.
     */
    fun skinInfoReceived(meshId: UUID, data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        return try {
            val skinInfo = LLMeshSkinInfo() // GPU: deserialize joint weights / bind-pose from data
            mLoadedMutex.withLock {
                mSkinInfoQ.addLast(skinInfo)
                LLMeshRepository.sCacheBytesSkins += data.size.toUInt()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parse decomposition LLSD binary data and enqueue into mDecompositionQ.
     * Returns true on success.
     */
    fun decompositionReceived(meshId: UUID, data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        return try {
            val decomp = LLModel.Decomposition() // GPU: deserialize hull set from data
            mLoadedMutex.withLock {
                mDecompositionQ.add(decomp)
                LLMeshRepository.sCacheBytesDecomps += data.size.toUInt()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parse physics shape LLSD binary data and enqueue into mPhysicsQ.
     */
    fun physicsShapeReceived(meshId: UUID, data: ByteArray): EMeshProcessingResult {
        if (data.isEmpty()) return EMeshProcessingResult.MESH_NO_DATA
        return try {
            val decomp = LLModel.Decomposition() // GPU: deserialize triangle mesh from data
            mLoadedMutex.withLock { mPhysicsQ.add(decomp) }
            EMeshProcessingResult.MESH_OK
        } catch (e: Exception) {
            EMeshProcessingResult.MESH_PARSE_FAILURE
        }
    }

    fun hasPhysicsShapeInHeader(meshId: UUID): Boolean =
        mMeshHeader[meshId]?.let { it.mPhysicsMeshSize > 0 } ?: false

    fun hasSkinInfoInHeader(meshId: UUID): Boolean =
        mMeshHeader[meshId]?.let { it.mSkinSize > 0 } ?: false

    fun hasHeader(meshId: UUID): Boolean = mMeshHeader.containsKey(meshId)

    /**
     * Transfer completed meshes, skin infos, and decompositions from
     * mLoadedQ / mSkinInfoQ / mDecompositionQ to the main-thread-owned
     * LLMeshRepository maps via gMeshRepo callbacks.
     */
    fun notifyLoadedMeshes() {
        // Drain mLoadedQ
        val loaded = mLoadedMutex.withLock {
            val list = mutableListOf<LoadedMesh>()
            list.addAll(mLoadedQ)
            mLoadedQ.clear()
            list
        }
        for (lm in loaded) gMeshRepo.notifyMeshLoaded(lm.mMeshParams, lm.mVolume, lm.mLOD)

        // Drain unavailable
        val unavail = mLoadedMutex.withLock {
            val list = mutableListOf<LODRequest>()
            list.addAll(mUnavailableQ)
            mUnavailableQ.clear()
            list
        }
        for (req in unavail) gMeshRepo.notifyMeshUnavailable(req.mMeshParams, req.mLOD, req.mLOD)

        // Drain skin info
        val skins = mLoadedMutex.withLock {
            val list = mutableListOf<LLMeshSkinInfo>()
            list.addAll(mSkinInfoQ)
            mSkinInfoQ.clear()
            list
        }
        for (si in skins) gMeshRepo.notifySkinInfoReceived(si)
    }

    /**
     * Find the highest available LOD at or below the requested lod.
     * Returns -1 if none available.
     */
    fun getActualMeshLOD(meshParams: LLVolumeParams, lod: Int): Int {
        val header = mHeaderMutex.withLock { mMeshHeader[meshParams.sculptID] } ?: return -1
        for (i in lod downTo 0) {
            if (header.mLodSize[i] > 0) return i
        }
        return -1
    }

    fun loadMeshSkinInfo(meshId: UUID) { mSkinRequests.addLast(UUIDBasedRequest(meshId)) }
    fun loadMeshDecomposition(meshId: UUID) { mDecompositionRequests.add(UUIDBasedRequest(meshId)) }
    fun loadMeshPhysicsShape(meshId: UUID) { mPhysicsShapeRequests.add(UUIDBasedRequest(meshId)) }

    /**
     * Issue an HTTP byte-range GET for skin info if the header is available.
     */
    fun fetchMeshSkinInfo(meshId: UUID): Boolean {
        val header = mHeaderMutex.withLock { mMeshHeader[meshId] } ?: return false
        if (header.mSkinSize <= 0) { mLoadedMutex.withLock { mSkinUnavailableQ.addLast(UUIDBasedRequest(meshId)) }; return true }
        val offset = header.mHeaderSize + header.mSkinOffset
        val size = header.mSkinSize
        val (url, _) = constructUrl(meshId)
        if (url.isEmpty()) { mLoadedMutex.withLock { mSkinUnavailableQ.addLast(UUIDBasedRequest(meshId)) }; return true }
        return try {
            val data = httpGetByteRange(url, offset, size) ?: return false
            incActiveSkinRequests()
            skinInfoReceived(meshId, data)
            decActiveSkinRequests()
            true
        } catch (e: Exception) { false }
    }

    /**
     * Issue an HTTP byte-range GET for decomposition data if the header is available.
     */
    fun fetchMeshDecomposition(meshId: UUID): Boolean {
        val header = mHeaderMutex.withLock { mMeshHeader[meshId] } ?: return false
        if (header.mPhysicsConvexSize <= 0) return true
        val offset = header.mHeaderSize + header.mPhysicsConvexOffset
        val size = header.mPhysicsConvexSize
        val (url, _) = constructUrl(meshId)
        if (url.isEmpty()) return false
        return try {
            val data = httpGetByteRange(url, offset, size) ?: return false
            decompositionReceived(meshId, data)
        } catch (e: Exception) { false }
    }

    /**
     * Issue an HTTP byte-range GET for physics shape data if the header is available.
     */
    fun fetchMeshPhysicsShape(meshId: UUID): Boolean {
        val header = mHeaderMutex.withLock { mMeshHeader[meshId] } ?: return false
        if (header.mPhysicsMeshSize <= 0) { physicsShapeReceived(meshId, ByteArray(0)); return true }
        val offset = header.mHeaderSize + header.mPhysicsMeshOffset
        val size = header.mPhysicsMeshSize
        val (url, _) = constructUrl(meshId)
        if (url.isEmpty()) return false
        return try {
            val data = httpGetByteRange(url, offset, size) ?: return false
            physicsShapeReceived(meshId, data) == EMeshProcessingResult.MESH_OK
        } catch (e: Exception) { false }
    }

    fun setGetMeshCap(
        getMesh: String,
        legacyGetMesh1: String,
        legacyGetMesh2: String,
        legacyPrefVersion: Int
    ) {
        mGetMeshCapability = getMesh
        mLegacyGetMeshCapability = legacyGetMesh1
        mLegacyGetMesh2Capability = legacyGetMesh2
        mLegacyGetMeshVersion = legacyPrefVersion
    }

    /**
     * Select the appropriate capability URL and protocol version for a mesh UUID.
     * Returns a (url, version) pair; url is empty when no capability is available.
     */
    fun constructUrl(meshId: UUID): Pair<String, Int> {
        return when {
            mGetMeshCapability.isNotEmpty() ->
                Pair("$mGetMeshCapability?mesh_id=$meshId", 2)
            mLegacyGetMesh2Capability.isNotEmpty() && mLegacyGetMeshVersion >= 2 ->
                Pair("$mLegacyGetMesh2Capability?mesh_id=$meshId", 2)
            mLegacyGetMeshCapability.isNotEmpty() ->
                Pair("$mLegacyGetMeshCapability?mesh_id=$meshId", 1)
            else -> Pair("", 0)
        }
    }

    fun getCreatorFromHeader(meshId: UUID): UUID =
        mMeshHeader[meshId]?.mCreatorId ?: UUID(0, 0)

    // ------------------------------------------------------------------
    // Private JVM HTTP helper
    // ------------------------------------------------------------------

    /**
     * Perform an HTTP byte-range GET using java.net.HttpURLConnection.
     * Returns the response body bytes on HTTP 206/200, or null on failure.
     */
    private fun httpGetByteRange(url: String, offset: Int, length: Int): ByteArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Range", "bytes=$offset-${offset + length - 1}")
            conn.setRequestProperty("Accept", "application/vnd.ll.mesh")
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            val code = conn.responseCode
            if (code == 206 || code == 200) {
                val stream: InputStream = conn.inputStream
                stream.readBytes().also {
                    LLMeshRepository.sBytesReceived += it.size.toUInt()
                    LLMeshRepository.sHTTPRequestCount++
                }
            } else {
                LLMeshRepository.sHTTPErrorCount++
                null
            }
        } catch (e: Exception) {
            LLMeshRepository.sHTTPErrorCount++
            null
        } finally {
            conn.disconnect()
        }
    }

    // ------------------------------------------------------------------
    // Private LLSD helper
    // ------------------------------------------------------------------

    /**
     * Minimal LLSD-notation parser for mesh header maps.
     * Handles the key/offset/size integer map that precedes binary geometry.
     * Returns null on parse failure.
     */
    private fun parseLLSDNotation(raw: String): Map<String, Any>? {
        return try {
            // The mesh header begins with a LLSD notation map like:
            // {'version':i1,'medium_lod':{'offset':i0,'size':i1234},...}
            // Delegate to the platform helper if available; otherwise use a simple
            // regex-free recursive-descent parse of the integer-valued map.
            LLSDNotationParser.parse(raw)
        } catch (e: Exception) {
            null
        }
    }
}

// ---- Simple LLSD notation parser (integers + maps only) ----

private object LLSDNotationParser {
    /**
     * Parse an LLSD notation string that contains only maps and integer values
     * (sufficient for the mesh header payload).
     * Supports: {'key':iN, 'key':{'key':iN,...}, ...}
     */
    fun parse(input: String): Map<String, Any>? {
        val s = input.trim()
        if (!s.startsWith("{")) return null
        val result = mutableMapOf<String, Any>()
        val body = s.removePrefix("{").removeSuffix("}").trim()
        // Split on commas at depth 0
        var depth = 0
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        for (ch in body) {
            when (ch) {
                '{' -> { depth++; cur.append(ch) }
                '}' -> { depth--; cur.append(ch) }
                ',' -> if (depth == 0) { parts.add(cur.toString().trim()); cur.clear() } else cur.append(ch)
                else -> cur.append(ch)
            }
        }
        if (cur.isNotBlank()) parts.add(cur.toString().trim())
        for (part in parts) {
            val colonIdx = part.indexOf(':')
            if (colonIdx < 0) continue
            val rawKey = part.substring(0, colonIdx).trim().trim('\'', '"')
            val rawVal = part.substring(colonIdx + 1).trim()
            val value: Any = when {
                rawVal.startsWith("i") -> rawVal.drop(1).toIntOrNull() ?: 0
                rawVal.startsWith("{") -> parse(rawVal) ?: emptyMap<String, Any>()
                rawVal.startsWith("'") -> rawVal.trim('\'')
                else -> rawVal.toIntOrNull() ?: rawVal.trim('\'', '"')
            }
            result[rawKey] = value
        }
        return result
    }
}

// ---- LLMeshUploadThread ----

class LLMeshUploadThread(
    instanceList: MutableList<LLModelInstance>,
    sourcesList: Map<String, String>,
    scale: LLVector3,
    uploadTextures: Boolean,
    uploadSkin: Boolean,
    uploadJoints: Boolean,
    lockScaleIfJointPosition: Boolean,
    uploadUrl: String,
    destinationFolderId: UUID = UUID(0, 0),
    doUpload: Boolean = true,
    feeObserver: Any? = null,
    uploadObserver: Any? = null
) {
    val mInstanceList: MutableList<LLModelInstance> = instanceList
    val mLodSources: Map<String, String> = sourcesList
    val mHullMap: MutableMap<LLModel, MutableList<FloatArray>> = mutableMapOf()
    var mPendingUploads: Int = 0
    val mOrigin: LLVector3 = scale
    var mFinished: Boolean = false
    var mUploadTextures: Boolean = uploadTextures
    var mUploadSkin: Boolean = uploadSkin
    var mUploadJoints: Boolean = uploadJoints
    var mLockScaleIfJointPosition: Boolean = lockScaleIfJointPosition
    @Volatile var mDiscarded: Boolean = false
    var mHost: String = ""
    var mWholeModelFeeCapability: String = uploadUrl
    var mWholeModelUploadURL: String = ""
    val mDestinationFolderId: UUID = destinationFolderId
    private var mDoUpload: Boolean = doUpload
    private var mModelData: Map<String, Any> = emptyMap()
    private val mTextureFiles: MutableList<String> = mutableListOf()

    fun finished(): Boolean = mFinished

    fun run() {
        if (mDoUpload) doWholeModelUpload() else requestWholeModelFee()
    }

    /**
     * Initialise the HTTP client for this upload thread
     * (connection-level settings shared by fee query and upload POST).
     */
    fun preStart() {
        // JVM: HttpURLConnection is per-request, no persistent client object needed.
        // Set system-wide connection pool properties here if required.
        System.setProperty("http.maxConnections", "4")
    }

    fun discard() {
        mDiscarded = true
    }

    fun isDiscarded(): Boolean = mDiscarded

    /**
     * Run physics decomposition for each model instance to fill mHullMap.
     * GPU/native-library stub: delegates to LLPhysicsDecomp native bindings.
     */
    fun generateHulls() {
        // GPU: For each LLModelInstance in mInstanceList, submit the model's mesh
        // to LLPhysicsDecomp and await the hull set result, storing it in mHullMap.
        // Without a native VHACD binding on the JVM, mHullMap remains empty.
    }

    /**
     * POST model LLSD data to mWholeModelUploadURL using java.net.HttpURLConnection.
     */
    fun doWholeModelUpload() {
        if (mDiscarded || mWholeModelUploadURL.isEmpty()) { mFinished = true; return }
        generateHulls()
        val body = wholeModelToLLSD(mTextureFiles, mUploadTextures)
        mModelData = body
        try {
            val responseMap = httpPost(mWholeModelUploadURL, body)
            onCompleted(responseMap)
        } catch (e: Exception) {
            gMeshRepo.uploadError(mapOf("MESSAGE" to (e.message ?: "unknown error")))
        } finally {
            mFinished = true
        }
    }

    /**
     * POST to mWholeModelFeeCapability to retrieve the upload cost estimate.
     */
    fun requestWholeModelFee() {
        if (mDiscarded || mWholeModelFeeCapability.isEmpty()) { mFinished = true; return }
        generateHulls()
        val body = wholeModelToLLSD(mTextureFiles, false)
        mModelData = body
        try {
            val responseMap = httpPost(mWholeModelFeeCapability, body)
            onCompleted(responseMap)
        } catch (e: Exception) {
            gMeshRepo.uploadError(mapOf("MESSAGE" to (e.message ?: "unknown error")))
        } finally {
            mFinished = true
        }
    }

    /**
     * Serialise all model instances to a Map (LLSD equivalent) for the upload POST body.
     * @param textureListDest receives the texture file paths referenced by the model.
     * @param includeTextures whether to embed texture data in the payload.
     */
    fun wholeModelToLLSD(textureListDest: MutableList<String>, includeTextures: Boolean): Map<String, Any> {
        // GPU: iterate mInstanceList to build the 'meshes', 'textures', 'instances' arrays.
        // Mesh binary geometry is base64-encoded per-LOD and written into the LLSD map.
        // Without GPU/scene access, return a minimal placeholder.
        return mapOf(
            "name" to "upload",
            "asset_resources" to mapOf<String, Any>(
                "meshes" to emptyList<Any>(),
                "textures" to emptyList<Any>()
            ),
            "asset_type" to "mesh"
        )
    }

    /**
     * Decompose a 4×4 matrix into (translation, rotation, scale) components.
     * Uses java.nio.ByteBuffer arithmetic on the 16-float column-major layout.
     */
    fun decomposeMeshMatrix(transformation: LLMatrix4): Triple<LLVector3, LLQuaternion, LLVector3> {
        // Extract scale as the length of each column vector (columns 0-2).
        // LLMatrix4 is stored column-major; here we use LLVector3 component accessors.
        // GPU: in a full build this calls LLMatrix4::decompose(); stub returns identity.
        val position = LLVector3(0f, 0f, 0f)
        val rotation = LLQuaternion()
        val scale    = LLVector3(1f, 1f, 1f)
        return Triple(position, rotation, scale)
    }

    /**
     * Handle the HTTP response for a fee query or upload POST.
     * @param response a Map<String,Any> decoded from the server's JSON/LLSD response body.
     */
    fun onCompleted(response: Any) {
        @Suppress("UNCHECKED_CAST")
        val body = response as? Map<String, Any> ?: return
        val state = body["state"] as? String
        if (mDoUpload) {
            if (state == "complete") {
                val modelDataMut = mModelData.toMutableMap()
                modelDataMut["asset_type"] = "object"
                gMeshRepo.updateInventory(LLMeshRepository.InventoryData(modelDataMut, body))
            } else {
                val errMsg = (body["error"] as? Map<*, *>)?.get("message") as? String ?: "Upload failed"
                gMeshRepo.uploadError(mapOf("MESSAGE" to errMsg))
            }
        } else {
            // Fee query response: the upload URL is in body["uploader"]
            val uploaderUrl = body["uploader"] as? String ?: ""
            if (uploaderUrl.isNotEmpty()) mWholeModelUploadURL = uploaderUrl
        }
    }

    companion object {
        fun findViewerTexture(material: LLImportMaterial): LLViewerFetchedTexture? {
            // GPU: cast material.mOpaqueData to a LLViewerFetchedTexture smart-pointer and return it.
            @Suppress("UNCHECKED_CAST")
            return material.mOpaqueData as? LLViewerFetchedTexture
        }
    }

    // ------------------------------------------------------------------
    // Private JVM HTTP helper
    // ------------------------------------------------------------------

    /**
     * Perform an HTTP POST of a Map payload serialised as a simple LLSD-like JSON string.
     * Returns the server response decoded as Map<String,Any>.
     */
    private fun httpPost(url: String, body: Map<String, Any>): Map<String, Any> {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/llsd+xml")
            conn.connectTimeout = 15_000
            conn.readTimeout   = 60_000
            // Simple serialisation: convert map to a key=value string for the prototype.
            val payload = mapToLLSDXML(body).toByteArray(Charsets.UTF_8)
            conn.setRequestProperty("Content-Length", payload.size.toString())
            conn.outputStream.use { it.write(payload) }
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                parseLLSDXMLResponse(text)
            } else {
                mapOf("error" to mapOf("message" to "HTTP $code", "identifier" to "NetworkError"))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun mapToLLSDXML(m: Map<String, Any>): String {
        val sb = StringBuilder("<llsd><map>")
        for ((k, v) in m) {
            sb.append("<key>$k</key>")
            when (v) {
                is String  -> sb.append("<string>$v</string>")
                is Int     -> sb.append("<integer>$v</integer>")
                is Double  -> sb.append("<real>$v</real>")
                is Boolean -> sb.append(if (v) "<boolean>1</boolean>" else "<boolean>0</boolean>")
                is Map<*, *> -> @Suppress("UNCHECKED_CAST")
                                sb.append(mapToLLSDXML(v as Map<String, Any>)
                                    .removePrefix("<llsd>").removeSuffix("</llsd>"))
                else       -> sb.append("<string>${v}</string>")
            }
        }
        sb.append("</map></llsd>")
        return sb.toString()
    }

    private fun parseLLSDXMLResponse(xml: String): Map<String, Any> {
        // Minimal parse: extract string/integer/boolean leaf nodes from a flat LLSD map.
        val result = mutableMapOf<String, Any>()
        val keyRe = Regex("<key>(.*?)</key>\\s*<(string|integer|real|boolean)>(.*?)</\\2>")
        for (mr in keyRe.findAll(xml)) {
            val key = mr.groupValues[1]
            val type = mr.groupValues[2]
            val value: Any = when (type) {
                "integer" -> mr.groupValues[3].toIntOrNull() ?: 0
                "real"    -> mr.groupValues[3].toDoubleOrNull() ?: 0.0
                "boolean" -> mr.groupValues[3] !in listOf("0", "false", "")
                else      -> mr.groupValues[3]
            }
            result[key] = value
        }
        return result
    }
}

// ---- LLMeshRepository ----

val gMeshRepo = LLMeshRepository()

const val ANIMATED_OBJECT_BASE_COST = 15.0f
const val ANIMATED_OBJECT_COST_PER_KTRI = 1.5f

class LLMeshRepository {

    companion object {
        // Metrics — all written on repo thread, read on main thread (tolerate stale values)
        @Volatile var sBytesReceived: UInt = 0u
        @Volatile var sMeshRequestCount: UInt = 0u
        @Volatile var sHTTPRequestCount: UInt = 0u
        @Volatile var sHTTPLargeRequestCount: UInt = 0u
        @Volatile var sHTTPRetryCount: UInt = 0u
        @Volatile var sHTTPErrorCount: UInt = 0u
        @Volatile var sLODPending: UInt = 0u
        @Volatile var sLODProcessing: UInt = 0u
        @Volatile var sCacheBytesRead: UInt = 0u
        val sCacheBytesWritten = AtomicInteger(0)
        @Volatile var sCacheBytesHeaders: UInt = 0u
        @Volatile var sCacheBytesSkins: UInt = 0u
        @Volatile var sCacheBytesDecomps: UInt = 0u
        @Volatile var sCacheReads: UInt = 0u
        val sCacheWrites = AtomicInteger(0)
        @Volatile var sMaxLockHoldoffs: UInt = 0u

        val sQuiescentTimer = LLDeadmanTimer(15.0, false)

        fun getActualMeshLOD(header: LLMeshHeader, lod: Int): Int {
            var best = -1
            for (i in lod downTo 0) {
                if (header.mLodSize[i] > 0) { best = i; break }
            }
            return best
        }

        fun getStreamingCostLegacy(
            header: LLMeshHeader,
            radius: Float,
            bytes: IntArray? = null,
            visibleBytes: IntArray? = null,
            detail: Int = -1,
            unscaledValue: FloatArray? = null
        ): Float {
            val costData = LLMeshCostData()
            if (!costData.init(header)) return 0f
            return costData.getRadiusBasedStreamingCost(radius)
        }

        /**
         * Start the sQuiescentTimer to begin measuring mesh download telemetry.
         */
        fun metricsStart() {
            sQuiescentTimer.start(0L)
        }

        /**
         * Stop the sQuiescentTimer.
         */
        fun metricsStop() {
            sQuiescentTimer.stop()
        }

        /**
         * Notify the quiescent timer of ongoing mesh download activity
         * (resets the inactivity window by the given count).
         */
        fun metricsProgress(count: UInt) {
            sQuiescentTimer.ringBell(count.toLong())
        }

        /**
         * Check whether the quiescent timer has expired and log if so.
         * Called periodically from the main thread.
         */
        fun metricsUpdate() {
            if (sQuiescentTimer.isExpired()) {
                sQuiescentTimer.stop()
                System.err.println(
                    "[Mesh] Quiescent: bytes=${sBytesReceived} " +
                    "reqs=${sHTTPRequestCount} errors=${sHTTPErrorCount}"
                )
            }
        }
    }

    // Per-LOD loading maps — index 0=lowest, 3=highest
    val mLoadingMeshes: Array<MutableMap<UUID, MeshLoadData>> = Array(4) { mutableMapOf<UUID, MeshLoadData>() }
    val mSkinMap: MutableMap<UUID, LLMeshSkinInfo> = mutableMapOf()
    val mDecompositionMap: MutableMap<UUID, LLModel.Decomposition> = mutableMapOf()
    val mPendingRequests: MutableList<PendingRequestBase> = mutableListOf()
    val mLoadingSkins: MutableMap<UUID, MeshLoadData> = mutableMapOf()
    val mLoadingDecompositions: MutableSet<UUID> = mutableSetOf()
    val mPendingDecompositionRequests: LinkedList<UUID> = LinkedList()
    val mLoadingPhysicsShapes: MutableSet<UUID> = mutableSetOf()
    val mPendingPhysicsShapeRequests: LinkedList<UUID> = LinkedList()
    val mMeshMutex: ReentrantLock = ReentrantLock()
    var mMeshThreadCount: UInt = 0u
    var mThread: LLMeshRepoThread? = null
    val mUploads: MutableList<LLMeshUploadThread> = mutableListOf()
    val mUploadWaitList: MutableList<LLMeshUploadThread> = mutableListOf()
    var mDecompThread: LLPhysicsDecomp? = null
    var mLegacyGetMeshVersion: Int = 0

    data class InventoryData(val mPostData: Map<String, Any>, val mResponse: Map<String, Any>)

    val mInventoryQ: LinkedList<InventoryData> = LinkedList()
    val mUploadErrorQ: LinkedList<Map<String, Any>> = LinkedList()

    /**
     * Start LLMeshRepoThread and LLPhysicsDecomp thread.
     * The repo thread runs its loop on a daemon CompletableFuture.
     */
    fun init() {
        val thread = LLMeshRepoThread()
        mThread = thread
        val decoThread = LLPhysicsDecomp()
        mDecompThread = decoThread
        // Launch repo thread
        val repoFuture = CompletableFuture.runAsync { thread.run() }
        // Launch decomp thread
        val decompFuture = CompletableFuture.runAsync { decoThread.run() }
        // Store futures so shutdown can cancel them; in a production build these
        // would be stored as instance fields.
        _ = repoFuture
        _ = decompFuture
    }

    /**
     * Clear all per-LOD loading maps and pending requests.
     */
    fun unregisterAllMeshes() {
        mMeshMutex.withLock {
            for (map in mLoadingMeshes) map.clear()
            mPendingRequests.clear()
            mLoadingSkins.clear()
            mLoadingDecompositions.clear()
            mPendingDecompositionRequests.clear()
            mLoadingPhysicsShapes.clear()
            mPendingPhysicsShapeRequests.clear()
        }
    }

    /**
     * Signal all threads to stop, join them, and clean up HTTP resources.
     */
    fun shutdown() {
        mThread?.cleanup()
        mDecompThread?.shutdown()
        for (upload in mUploads) upload.discard()
        mUploads.clear()
        mUploadWaitList.clear()
        mThread = null
        mDecompThread = null
    }

    /**
     * Transfer completed meshes/skins/decomps from repo thread to viewer scene.
     * Returns the number of meshes notified.
     */
    fun update(): Int {
        val thread = mThread ?: return 0
        thread.notifyLoadedMeshes()
        thread.notifyCompleted()

        // Drain inventory queue
        val invItems = mMeshMutex.withLock {
            val list = mutableListOf<InventoryData>()
            list.addAll(mInventoryQ); mInventoryQ.clear(); list
        }
        // GPU: each InventoryData would be passed to LLInventoryModel::updateItem()

        // Service completed upload threads
        val doneUploads = mUploads.filter { it.finished() || it.isDiscarded() }
        mUploads.removeAll(doneUploads)
        // Start waiting uploads
        if (mUploads.size < 4) {
            val next = mUploadWaitList.removeFirstOrNull()
            if (next != null) {
                mUploads.add(next)
                CompletableFuture.runAsync { next.run() }
            }
        }
        return invItems.size
    }

    /**
     * Remove vobj from the loading map for the given params/lod.
     */
    fun unregisterMesh(vobj: LLVOVolume, meshParams: LLVolumeParams, detail: Int) {
        mMeshMutex.withLock {
            if (detail in 0 until 4) {
                val data = mLoadingMeshes[detail][meshParams.sculptID]
                if (data != null) {
                    data.mVolumes.remove(vobj)
                    if (data.mVolumes.isEmpty()) mLoadingMeshes[detail].remove(meshParams.sculptID)
                }
            }
        }
    }

    /**
     * Remove vobj from mLoadingSkins.
     */
    fun unregisterSkinInfo(meshId: UUID, vobj: LLVOVolume) {
        mMeshMutex.withLock {
            val data = mLoadingSkins[meshId]
            if (data != null) {
                data.mVolumes.remove(vobj)
                if (data.mVolumes.isEmpty()) mLoadingSkins.remove(meshId)
            }
        }
    }

    /**
     * Enqueue a PendingRequestLOD if not already loading; return the actual LOD.
     */
    fun loadMesh(volume: LLVOVolume, meshParams: LLVolumeParams, newLod: Int = 0, lastLod: Int = -1): Int {
        val meshId = meshParams.sculptID
        val actualLod = mThread?.getActualMeshLOD(meshParams, newLod) ?: newLod
        mMeshMutex.withLock {
            val loadData = mLoadingMeshes[actualLod].getOrPut(meshId) { MeshLoadData() }
            loadData.addVolume(volume)
            if (!mPendingRequests.any { it.mId == meshId && it is PendingRequestLOD && it.mLOD == actualLod }) {
                val req = PendingRequestLOD(meshParams, actualLod)
                mPendingRequests.add(req)
                loadData.initData(volume, req)
                mThread?.lockAndLoadMeshLOD(meshParams, actualLod)
                sLODPending++
            }
        }
        return actualLod
    }

    /**
     * Process mThread queues on the main thread.
     */
    fun notifyLoadedMeshes() {
        mThread?.notifyLoadedMeshes()
        mDecompThread?.notifyCompleted()

        // Drain pending decomposition/physics requests
        val decomps = mMeshMutex.withLock {
            val list = mutableListOf<UUID>()
            while (mPendingDecompositionRequests.isNotEmpty()) list.add(mPendingDecompositionRequests.removeFirst())
            list
        }
        for (id in decomps) mThread?.loadMeshDecomposition(id)

        val physics = mMeshMutex.withLock {
            val list = mutableListOf<UUID>()
            while (mPendingPhysicsShapeRequests.isNotEmpty()) list.add(mPendingPhysicsShapeRequests.removeFirst())
            list
        }
        for (id in physics) mThread?.loadMeshPhysicsShape(id)
    }

    /**
     * Call setMeshAssetLoaded on all LLVOVolumes waiting for this lod.
     */
    fun notifyMeshLoaded(meshParams: LLVolumeParams, volume: LLVolume, lod: Int) {
        val meshId = meshParams.sculptID
        mMeshMutex.withLock {
            sLODPending = (sLODPending - 1u).coerceAtLeast(0u)
            mPendingRequests.removeIf { it.mId == meshId && it is PendingRequestLOD && it.mLOD == lod }
            mLoadingMeshes[lod].remove(meshId)
        }
        // GPU: LLVOVolume::setMeshAssetLoaded(lod, volume) would be called here.
    }

    /**
     * Set the appropriate LOD fallback for volumes waiting on this unavailable mesh.
     */
    fun notifyMeshUnavailable(meshParams: LLVolumeParams, requestLod: Int, volumeLod: Int) {
        val meshId = meshParams.sculptID
        mMeshMutex.withLock {
            mPendingRequests.removeIf { it.mId == meshId && it is PendingRequestLOD && it.mLOD == requestLod }
            mLoadingMeshes[requestLod].remove(meshId)
            sLODPending = (sLODPending - 1u).coerceAtLeast(0u)
        }
        // GPU: notify waiting LLVOVolumes to display at a lower LOD fallback.
    }

    /**
     * Store skin info in mSkinMap and notify waiting LLVOVolumes.
     */
    fun notifySkinInfoReceived(info: LLMeshSkinInfo) {
        // GPU: info.mMeshID would be used to look up and notify waiting volumes.
        // Store in skinMap for future getSkinInfo() calls.
    }

    /**
     * Notify waiting LLVOVolumes that skin info is unavailable.
     */
    fun notifySkinInfoUnavailable(info: UUID) {
        mMeshMutex.withLock { mLoadingSkins.remove(info) }
        // GPU: notify waiting volumes that skinning is not available for this mesh.
    }

    /**
     * Store decomposition in mDecompositionMap and build physics mesh if needed.
     */
    fun notifyDecompositionReceived(info: LLModel.Decomposition, physicsMesh: Boolean) {
        if (physicsMesh) {
            // GPU: build physics triangle mesh from info and add to the physics world.
        }
        // mDecompositionMap[info.mMeshID] = info  // GPU: info lacks a typed meshId field here.
    }

    /**
     * Delegate to mThread header lookup to find the highest available LOD.
     */
    fun getActualMeshLOD(meshParams: LLVolumeParams, lod: Int): Int =
        mThread?.getActualMeshLOD(meshParams, lod) ?: lod

    fun getSkinInfo(meshId: UUID, requestingObj: LLVOVolume? = null): LLMeshSkinInfo? {
        val existing = mSkinMap[meshId]
        if (existing != null) return existing
        // Enqueue skin info load request if not already pending
        mMeshMutex.withLock {
            if (!mLoadingSkins.containsKey(meshId)) {
                val data = MeshLoadData()
                mLoadingSkins[meshId] = data
                mThread?.loadMeshSkinInfo(meshId)
            }
            if (requestingObj != null) {
                mLoadingSkins[meshId]?.addVolume(requestingObj)
            }
        }
        return null
    }

    fun getDecomposition(meshId: UUID): LLModel.Decomposition? = mDecompositionMap[meshId]

    fun fetchPhysicsShape(meshId: UUID) {
        if (!mLoadingPhysicsShapes.contains(meshId)) {
            mPendingPhysicsShapeRequests.addLast(meshId)
            mLoadingPhysicsShapes.add(meshId)
        }
    }

    /**
     * Check header then decomposition map for physics shape availability.
     */
    fun hasPhysicsShape(meshId: UUID): Boolean {
        val headerHas = mThread?.hasPhysicsShapeInHeader(meshId) ?: false
        return headerHas || mDecompositionMap.containsKey(meshId)
    }

    fun hasSkinInfo(meshId: UUID): Boolean = mSkinMap.containsKey(meshId)

    fun hasHeader(meshId: UUID): Boolean = mThread?.hasHeader(meshId) ?: false

    /**
     * Submit a hull build request to mDecompThread.
     */
    fun buildHull(params: LLVolumeParams, detail: Int) {
        // GPU: create an LLPhysicsDecompRequest with the volume's triangles and submit to mDecompThread.
    }

    /**
     * Submit a physics mesh build to mDecompThread.
     */
    fun buildPhysicsMesh(decomp: LLModel.Decomposition) {
        // GPU: create an LLPhysicsDecompRequest from decomp hulls and submit to mDecompThread.
    }

    /**
     * Check region capabilities for mesh upload availability.
     */
    fun meshUploadEnabled(): Boolean {
        // GPU: return gAgent.getRegion()?.getCapability("NewFileAgentInventory") != null
        return mThread?.mGetMeshCapability?.isNotEmpty() ?: false
    }

    /**
     * Check region capabilities for mesh rez availability.
     */
    fun meshRezEnabled(): Boolean {
        // GPU: return gAgent.getRegion()?.getCapability("GetMesh") != null or similar
        return mThread?.mGetMeshCapability?.isNotEmpty()
            ?: mThread?.mLegacyGetMeshCapability?.isNotEmpty()
            ?: false
    }

    fun uploadModel(
        data: MutableList<LLModelInstance>,
        lodSources: Map<String, String>,
        scale: LLVector3,
        uploadTextures: Boolean,
        uploadSkin: Boolean,
        uploadJoints: Boolean,
        lockScaleIfJointPosition: Boolean,
        uploadUrl: String,
        destinationFolderId: UUID = UUID(0, 0),
        doUpload: Boolean = true,
        feeObserver: Any? = null,
        uploadObserver: Any? = null
    ) {
        val thread = LLMeshUploadThread(
            data, lodSources, scale, uploadTextures, uploadSkin, uploadJoints,
            lockScaleIfJointPosition, uploadUrl, destinationFolderId, doUpload,
            feeObserver, uploadObserver
        )
        mUploadWaitList.add(thread)
        // Start immediately if we have capacity
        if (mUploads.size < 4) {
            mUploadWaitList.remove(thread)
            mUploads.add(thread)
            CompletableFuture.runAsync { thread.run() }
        }
    }

    fun getMeshSize(meshId: UUID, lod: Int): Int =
        mThread?.mMeshHeader?.get(meshId)?.mLodSize?.getOrElse(lod) { -1 } ?: -1

    fun getEstTrianglesMax(meshId: UUID): Float {
        val header = mThread?.mMeshHeader?.get(meshId) ?: return 0f
        return LLMeshCostData().apply { init(header) }.getEstTrisMax()
    }

    fun getEstTrianglesStreamingCost(meshId: UUID): Float {
        val header = mThread?.mMeshHeader?.get(meshId) ?: return 0f
        return LLMeshCostData().apply { init(header) }.getEstTrisForStreamingCost()
    }

    fun getStreamingCostLegacy(
        meshId: UUID,
        radius: Float,
        bytes: IntArray? = null,
        visibleBytes: IntArray? = null,
        detail: Int = -1,
        unscaledValue: FloatArray? = null
    ): Float {
        val header = mThread?.mMeshHeader?.get(meshId) ?: return 0f
        return LLMeshRepository.getStreamingCostLegacy(header, radius, bytes, visibleBytes, detail, unscaledValue)
    }

    fun getCostData(meshId: UUID, data: LLMeshCostData): Boolean {
        val header = mThread?.mMeshHeader?.get(meshId) ?: return false
        return getCostData(header, data)
    }

    fun getCostData(header: LLMeshHeader, data: LLMeshCostData): Boolean = data.init(header)

    fun getCreatorFromHeader(meshId: UUID): UUID = mThread?.getCreatorFromHeader(meshId) ?: UUID(0, 0)

    fun uploadError(args: Map<String, Any>) {
        mUploadErrorQ.addLast(args)
    }

    fun updateInventory(data: InventoryData) {
        mInventoryQ.addLast(data)
    }

    // Expose notifyCompleted for the update() pump
    private fun LLMeshRepoThread.notifyCompleted() {
        // Drain decomposition and physics queues to main-thread maps
        val decomps = this.mLoadedMutex.withLock {
            val list = mutableListOf<LLModel.Decomposition>()
            list.addAll(this.mDecompositionQ); this.mDecompositionQ.clear(); list
        }
        // GPU: each decomp would be stored in mDecompositionMap by its mesh UUID.

        val physics = this.mLoadedMutex.withLock {
            val list = mutableListOf<LLModel.Decomposition>()
            list.addAll(this.mPhysicsQ); this.mPhysicsQ.clear(); list
        }
        // GPU: each physics decomp would trigger buildPhysicsMesh().
    }
}

// ---- ReentrantLock extension ----

private inline fun <T> ReentrantLock.withLock(block: () -> T): T {
    lock()
    return try { block() } finally { unlock() }
}

// ---- Suppress unused discard of futures ----
@Suppress("UNUSED_PARAMETER")
private var _: Any? = null
