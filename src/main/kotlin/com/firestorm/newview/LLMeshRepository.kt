package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger

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
class LLDeadmanTimer(val seconds: Double, val cpuMetrics: Boolean)

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
        const val DOWNLOAD_RETRY_LIMIT: UInt = 8u
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
        mScore = 0f
        mTrackedData?.mVolumes?.forEach { vol ->
            TODO("GPU: calculate_score(vol) and update mScore")
        }
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
    private val mRequestQ: ArrayDeque<LLPhysicsDecompRequest> = ArrayDeque()
    private val mCompletedQ: ArrayDeque<LLPhysicsDecompRequest> = ArrayDeque()
    var mCurRequest: LLPhysicsDecompRequest? = null
    val mStageID: MutableMap<String, Int> = mutableMapOf()

    fun shutdown() {
        TODO("APR: use JVM equivalent — signal decomp thread to stop and join")
    }

    fun submitRequest(request: LLPhysicsDecompRequest) {
        mRequestQ.addLast(request)
        TODO("APR: use JVM equivalent — signal decomp thread condition variable")
    }

    fun setMeshData(mesh: Any, vertexBased: Boolean) {
        TODO("APR: use JVM equivalent — pass mesh data to convex decomposition library")
    }

    fun doDecomposition() {
        TODO("APR: use JVM equivalent — run convex decomposition on current request")
    }

    fun doDecompositionSingleHull() {
        TODO("APR: use JVM equivalent — run single-hull decomposition on current request")
    }

    fun run() {
        TODO("APR: use JVM equivalent — decomposition thread main loop processing mRequestQ")
    }

    fun completeCurrent() {
        mCurRequest?.let { mCompletedQ.addLast(it) }
        mCurRequest = null
    }

    fun notifyCompleted() {
        TODO("APR: use JVM equivalent — dispatch completed requests back to main thread")
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
    fun getEstTrisMax(): Float = mEstTrisByLOD.max()

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
    val mSkinRequests: ArrayDeque<UUIDBasedRequest> = ArrayDeque()
    val mSkinInfoQ: ArrayDeque<LLMeshSkinInfo> = ArrayDeque()
    val mSkinUnavailableQ: ArrayDeque<UUIDBasedRequest> = ArrayDeque()
    val mDecompositionRequests: MutableSet<UUIDBasedRequest> = mutableSetOf()
    val mPhysicsShapeRequests: MutableSet<UUIDBasedRequest> = mutableSetOf()
    val mDecompositionQ: MutableList<LLModel.Decomposition> = mutableListOf()
    val mPhysicsQ: MutableList<LLModel.Decomposition> = mutableListOf()
    val mHeaderReqQ: ArrayDeque<HeaderRequest> = ArrayDeque()
    val mLODReqQ: ArrayDeque<LODRequest> = ArrayDeque()
    val mUnavailableQ: ArrayDeque<LODRequest> = ArrayDeque()
    val mLoadedQ: ArrayDeque<LoadedMesh> = ArrayDeque()
    val mPendingLOD: MutableMap<UUID, IntArray> = mutableMapOf()
    val mSkinMap: MutableMap<UUID, LLMeshSkinInfo> = mutableMapOf()

    var mGetMeshCapability: String = ""
    var mLegacyGetMeshCapability: String = ""
    var mLegacyGetMesh2Capability: String = ""
    var mLegacyGetMeshVersion: Int = 0

    private var mShuttingDown: Boolean = false
    private var mDiskCacheBuffer: ByteArray? = null

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
    }

    fun isShuttingDown(): Boolean = mShuttingDown

    fun run() {
        TODO("APR: use JVM equivalent — repo thread main loop: process header/LOD/skin/decomp queues")
    }

    fun cleanup() {
        mShuttingDown = true
        TODO("APR: use JVM equivalent — cancel pending HTTP requests and drain queues")
    }

    fun lockAndLoadMeshLOD(meshParams: LLVolumeParams, lod: Int) {
        TODO("APR: use JVM equivalent — acquire mMutex then enqueue LOD load request")
    }

    fun loadMeshLOD(meshParams: LLVolumeParams, lod: Int) {
        TODO("APR: use JVM equivalent — enqueue LOD request into mLODReqQ")
    }

    fun fetchMeshHeader(meshParams: LLVolumeParams): Boolean {
        TODO("APR: use JVM equivalent — issue HTTP byte-range GET for 4096-byte mesh header")
    }

    fun fetchMeshLOD(meshParams: LLVolumeParams, lod: Int): Boolean {
        TODO("APR: use JVM equivalent — issue HTTP byte-range GET for LOD data from header offsets")
    }

    fun headerReceived(meshParams: LLVolumeParams, data: ByteArray, flags: UInt = 0u): EMeshProcessingResult {
        TODO("APR: use JVM equivalent — parse LLSD mesh header and populate mMeshHeader")
    }

    fun lodReceived(meshParams: LLVolumeParams, lod: Int, data: ByteArray): EMeshProcessingResult {
        TODO("APR: use JVM equivalent — unpack LOD mesh data into LLVolume and enqueue into mLoadedQ")
    }

    fun skinInfoReceived(meshId: UUID, data: ByteArray): Boolean {
        TODO("APR: use JVM equivalent — parse skin info LLSD and enqueue into mSkinInfoQ")
    }

    fun decompositionReceived(meshId: UUID, data: ByteArray): Boolean {
        TODO("APR: use JVM equivalent — parse decomposition LLSD and enqueue into mDecompositionQ")
    }

    fun physicsShapeReceived(meshId: UUID, data: ByteArray): EMeshProcessingResult {
        TODO("APR: use JVM equivalent — parse physics shape LLSD and enqueue into mPhysicsQ")
    }

    fun hasPhysicsShapeInHeader(meshId: UUID): Boolean =
        mMeshHeader[meshId]?.let { it.mPhysicsMeshSize > 0 } ?: false

    fun hasSkinInfoInHeader(meshId: UUID): Boolean =
        mMeshHeader[meshId]?.let { it.mSkinSize > 0 } ?: false

    fun hasHeader(meshId: UUID): Boolean = mMeshHeader.containsKey(meshId)

    fun notifyLoadedMeshes() {
        TODO("APR: use JVM equivalent — transfer mLoadedQ entries to main thread")
    }

    fun getActualMeshLOD(meshParams: LLVolumeParams, lod: Int): Int {
        TODO("APR: use JVM equivalent — find highest available LOD at or below requested lod")
    }

    fun loadMeshSkinInfo(meshId: UUID) { mSkinRequests.addLast(UUIDBasedRequest(meshId)) }
    fun loadMeshDecomposition(meshId: UUID) { mDecompositionRequests.add(UUIDBasedRequest(meshId)) }
    fun loadMeshPhysicsShape(meshId: UUID) { mPhysicsShapeRequests.add(UUIDBasedRequest(meshId)) }

    fun fetchMeshSkinInfo(meshId: UUID): Boolean {
        TODO("APR: use JVM equivalent — issue HTTP byte-range GET for skin info if header available")
    }

    fun fetchMeshDecomposition(meshId: UUID): Boolean {
        TODO("APR: use JVM equivalent — issue HTTP byte-range GET for decomposition if header available")
    }

    fun fetchMeshPhysicsShape(meshId: UUID): Boolean {
        TODO("APR: use JVM equivalent — issue HTTP byte-range GET for physics shape if header available")
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

    fun constructUrl(meshId: UUID): Pair<String, Int> {
        TODO("APR: use JVM equivalent — select appropriate cap URL and version for mesh ID")
    }

    fun getCreatorFromHeader(meshId: UUID): UUID =
        mMeshHeader[meshId]?.mCreatorId ?: UUID(0, 0)
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

    fun preStart() {
        TODO("APR: use JVM equivalent — initialise HTTP client for upload thread")
    }

    fun discard() {
        mDiscarded = true
    }

    fun isDiscarded(): Boolean = mDiscarded

    fun generateHulls() {
        TODO("APR: use JVM equivalent — run physics decomposition for each model instance hull")
    }

    fun doWholeModelUpload() {
        TODO("APR: use JVM equivalent — POST model data to mWholeModelUploadURL")
    }

    fun requestWholeModelFee() {
        TODO("APR: use JVM equivalent — POST to mWholeModelFeeCapability to get upload cost")
    }

    fun wholeModelToLLSD(textureListDest: MutableList<String>, includeTextures: Boolean): Map<String, Any> {
        TODO("APR: use JVM equivalent — serialise all model instances to LLSD for upload POST")
    }

    fun decomposeMeshMatrix(transformation: LLMatrix4): Triple<LLVector3, LLQuaternion, LLVector3> {
        TODO("APR: use JVM equivalent — decompose 4x4 matrix into position/rotation/scale")
    }

    fun onCompleted(response: Any) {
        TODO("APR: use JVM equivalent — handle HTTP response for fee query or upload")
    }

    companion object {
        fun findViewerTexture(material: LLImportMaterial): LLViewerFetchedTexture? {
            TODO("GPU: look up viewer texture from import material opaque data")
        }
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

        fun metricsStart() {
            TODO("APR: use JVM equivalent — start sQuiescentTimer for mesh download telemetry")
        }

        fun metricsStop() {
            TODO("APR: use JVM equivalent — stop sQuiescentTimer")
        }

        fun metricsProgress(count: UInt) {
            TODO("APR: use JVM equivalent — update quiescent timer progress count")
        }

        fun metricsUpdate() {
            TODO("APR: use JVM equivalent — check quiescent timer and log if expired")
        }
    }

    // Per-LOD loading maps — index 0=lowest, 3=highest
    val mLoadingMeshes: Array<MutableMap<UUID, MeshLoadData>> = Array(4) { mutableMapOf() }
    val mSkinMap: MutableMap<UUID, LLMeshSkinInfo> = mutableMapOf()
    val mDecompositionMap: MutableMap<UUID, LLModel.Decomposition> = mutableMapOf()
    val mPendingRequests: MutableList<PendingRequestBase> = mutableListOf()
    val mLoadingSkins: MutableMap<UUID, MeshLoadData> = mutableMapOf()
    val mLoadingDecompositions: MutableSet<UUID> = mutableSetOf()
    val mPendingDecompositionRequests: ArrayDeque<UUID> = ArrayDeque()
    val mLoadingPhysicsShapes: MutableSet<UUID> = mutableSetOf()
    val mPendingPhysicsShapeRequests: ArrayDeque<UUID> = ArrayDeque()
    var mMeshThreadCount: UInt = 0u
    var mThread: LLMeshRepoThread? = null
    val mUploads: MutableList<LLMeshUploadThread> = mutableListOf()
    val mUploadWaitList: MutableList<LLMeshUploadThread> = mutableListOf()
    var mDecompThread: LLPhysicsDecomp? = null
    var mLegacyGetMeshVersion: Int = 0

    data class InventoryData(val mPostData: Map<String, Any>, val mResponse: Map<String, Any>)

    val mInventoryQ: ArrayDeque<InventoryData> = ArrayDeque()
    val mUploadErrorQ: ArrayDeque<Map<String, Any>> = ArrayDeque()

    fun init() {
        TODO("APR: use JVM equivalent — start LLMeshRepoThread and LLPhysicsDecomp thread")
    }

    fun unregisterAllMeshes() {
        TODO("APR: use JVM equivalent — clear all loading maps and pending requests")
    }

    fun shutdown() {
        TODO("APR: use JVM equivalent — signal threads to stop, join, cleanup HTTP resources")
    }

    fun update(): Int {
        TODO("APR: use JVM equivalent — transfer completed meshes/skins/decomps from repo thread to viewer scene")
    }

    fun unregisterMesh(vobj: LLVOVolume, meshParams: LLVolumeParams, detail: Int) {
        TODO("APR: use JVM equivalent — remove vobj from loading map for given params/lod")
    }

    fun unregisterSkinInfo(meshId: UUID, vobj: LLVOVolume) {
        TODO("APR: use JVM equivalent — remove vobj from mLoadingSkins")
    }

    fun loadMesh(volume: LLVOVolume, meshParams: LLVolumeParams, newLod: Int = 0, lastLod: Int = -1): Int {
        TODO("APR: use JVM equivalent — enqueue PendingRequestLOD if not already loading, return lod")
    }

    fun notifyLoadedMeshes() {
        TODO("APR: use JVM equivalent — process mThread queues on main thread")
    }

    fun notifyMeshLoaded(meshParams: LLVolumeParams, volume: LLVolume, lod: Int) {
        TODO("APR: use JVM equivalent — setMeshAssetLoaded on all volumes waiting for this lod")
    }

    fun notifyMeshUnavailable(meshParams: LLVolumeParams, requestLod: Int, volumeLod: Int) {
        TODO("APR: use JVM equivalent — set appropriate LOD fallback for waiting volumes")
    }

    fun notifySkinInfoReceived(info: LLMeshSkinInfo) {
        TODO("APR: use JVM equivalent — store in mSkinMap and notify waiting VOVolumes")
    }

    fun notifySkinInfoUnavailable(info: UUID) {
        TODO("APR: use JVM equivalent — notify waiting VOVolumes that skin info is unavailable")
    }

    fun notifyDecompositionReceived(info: LLModel.Decomposition, physicsMesh: Boolean) {
        TODO("APR: use JVM equivalent — store in mDecompositionMap and build physics mesh if needed")
    }

    fun getActualMeshLOD(meshParams: LLVolumeParams, lod: Int): Int {
        TODO("APR: use JVM equivalent — delegate to mThread header lookup")
    }

    fun getSkinInfo(meshId: UUID, requestingObj: LLVOVolume? = null): LLMeshSkinInfo? {
        return mSkinMap[meshId] ?: run {
            TODO("APR: use JVM equivalent — enqueue skin info load request if not already pending")
        }
    }

    fun getDecomposition(meshId: UUID): LLModel.Decomposition? = mDecompositionMap[meshId]

    fun fetchPhysicsShape(meshId: UUID) {
        if (!mLoadingPhysicsShapes.contains(meshId)) {
            mPendingPhysicsShapeRequests.addLast(meshId)
            mLoadingPhysicsShapes.add(meshId)
        }
    }

    fun hasPhysicsShape(meshId: UUID): Boolean {
        TODO("APR: use JVM equivalent — check header then decomposition map")
    }

    fun hasSkinInfo(meshId: UUID): Boolean = mSkinMap.containsKey(meshId)

    fun hasHeader(meshId: UUID): Boolean = mThread?.hasHeader(meshId) ?: false

    fun buildHull(params: LLVolumeParams, detail: Int) {
        TODO("APR: use JVM equivalent — submit hull build request to mDecompThread")
    }

    fun buildPhysicsMesh(decomp: LLModel.Decomposition) {
        TODO("APR: use JVM equivalent — submit physics mesh build to mDecompThread")
    }

    fun meshUploadEnabled(): Boolean {
        TODO("APR: use JVM equivalent — check region capabilities for mesh upload")
    }

    fun meshRezEnabled(): Boolean {
        TODO("APR: use JVM equivalent — check region capabilities for mesh rez")
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
        TODO("APR: use JVM equivalent — start upload thread")
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
}
