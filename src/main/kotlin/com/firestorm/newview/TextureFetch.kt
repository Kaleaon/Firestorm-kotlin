package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val HTTP_PIPE_REQUESTS_HIGH_WATER = 100
private const val HTTP_PIPE_REQUESTS_LOW_WATER = 50
private const val HTTP_NONPIPE_REQUESTS_HIGH_WATER = 40
private const val HTTP_NONPIPE_REQUESTS_LOW_WATER = 20
private const val HTTP_REQUESTS_RANGE_END_MAX = 20_000_000
private const val MAX_CAP_MISSING_RETRIES = 720
private const val CAP_MISSING_EXPIRATION_DELAY = 1

val FETCH_STATE_NAMES = listOf(
    "INVALID", "INIT", "LOAD_FROM_TEXTURE_CACHE", "CACHE_POST",
    "LOAD_FROM_NETWORK", "LOAD_FROM_SIMULATOR",
    "WAIT_HTTP_RESOURCE", "WAIT_HTTP_RESOURCE2", "SEND_HTTP_REQ", "WAIT_HTTP_REQ",
    "DECODE_IMAGE", "DECODE_IMAGE_UPDATE", "WRITE_TO_CACHE", "WAIT_ON_WRITE", "DONE"
)

enum class TexSource { FROM_ALL, FROM_HTTP_ONLY, INVALID_SOURCE }

enum class CreateRequestError(val code: Int) {
    DEFAULT(-1), MHOSTS(-2), ABORTED(-3), TRANSITION(-4)
}

class TextureFetch(
    private val textureCache: TextureCache,
    private val threaded: Boolean,
    val qaMode: Boolean
) {
    private val queueMutex = ReentrantLock()
    private val networkQueueMutex = ReentrantLock()

    private val requestMap: MutableMap<UUID, TextureFetchWorker> = mutableMapOf()

    private val networkQueue: MutableSet<UUID> = mutableSetOf()
    private val httpTextureQueue: MutableSet<UUID> = mutableSetOf()
    private val cancelQueue: MutableMap<String, MutableSet<UUID>> = mutableMapOf()

    private var textureBandwidth: Float = 0f
    private var maxBandwidth: Float = 0f
    private val httpSemaphore: AtomicInteger = AtomicInteger(0)
    private val httpWaitResource: MutableSet<UUID> = mutableSetOf()

    private var totalCacheReadCount: UInt = 0u
    private var totalCacheWriteCount: UInt = 0u
    private var totalResourceWaitCount: UInt = 0u

    private var totalHttpRequests: UInt = 0u

    private val commands: MutableList<TFRequest> = mutableListOf()

    private var fetchSource: TexSource = TexSource.FROM_ALL
    private val originFetchSource: TexSource = TexSource.FROM_ALL

    var debugId: UUID = NULL_UUID
    var debugCount: Int = 0
    var debugPause: Boolean = false
    var packetCount: Int = 0
    var badPacketCount: Int = 0

    abstract class TFRequest

    fun update(maxTimeMs: Float): Int {
        TODO("APR: process worker state machines, fire HTTP requests, handle responses")
    }

    fun shutDownTextureCacheThread() {
        TODO("APR: drain texture cache thread after it has stopped")
    }

    fun shutDownImageDecodeThread() {
        TODO("APR: drain image decode thread after it has stopped")
    }

    fun createRequest(
        fType: FTType,
        url: String,
        id: UUID,
        host: String,
        priority: Float,
        w: Int,
        h: Int,
        c: Int,
        discard: Int,
        needsAux: Boolean,
        canUseHttp: Boolean
    ): Int {
        TODO("APR: create or update TextureFetchWorker for texture id $id")
    }

    fun deleteRequest(id: UUID, cancel: Boolean) {
        TODO("APR: schedule removal of fetch worker for texture id $id")
    }

    fun deleteAllRequests() {
        TODO("APR: cancel and remove all pending fetch workers")
    }

    fun getRequestFinished(
        id: UUID,
        discardLevel: Int,
        workerState: Int,
        raw: Any?,
        aux: Any?,
        lastHttpGetStatus: Any?
    ): Boolean {
        TODO("APR: check if the fetch worker for texture id $id has completed")
    }

    fun updateRequestPriority(id: UUID, priority: Float): Boolean {
        return queueMutex.withLock {
            val worker = requestMap[id] ?: return false
            worker.imagePriority = priority
            true
        }
    }

    fun receiveImageHeader(host: String, id: UUID, codec: UByte, packets: UShort, totalBytes: UInt, dataSize: UShort, data: ByteArray): Boolean {
        TODO("APR: handle UDP image header packet (OpenSim compatibility)")
    }

    fun receiveImagePacket(host: String, id: UUID, packetNum: UShort, dataSize: UShort, data: ByteArray): Boolean {
        TODO("APR: handle UDP image data packet (OpenSim compatibility)")
    }

    fun setTextureBandwidth(bandwidth: Float) { textureBandwidth = bandwidth }
    fun getTextureBandwidth(): Float = textureBandwidth

    fun isFromLocalCache(id: UUID): Boolean {
        TODO("APR: check fetch worker to determine if texture came from cache")
    }

    fun getFetchState(id: UUID): Int =
        queueMutex.withLock { requestMap[id]?.fetchState ?: -1 }

    fun getFetchState(
        id: UUID,
        decodeProgressP: Float,
        requestedPriorityP: Float,
        fetchPriorityP: UInt,
        fetchDtimeP: Float,
        requestDtimeP: Float,
        canUseHttp: Boolean
    ): Int {
        TODO("APR: return detailed fetch state and fill out-params for worker id $id")
    }

    fun getLastFetchState(id: UUID, requestedDiscard: Int, decodedDiscard: Int, decoded: Boolean): Int {
        TODO("APR: return last completed fetch state for texture id $id")
    }

    fun getLastRawImage(id: UUID, raw: Any?, aux: Any?): Int {
        TODO("APR: return last decoded raw image for texture id $id")
    }

    fun dump() { TODO("GPU: log fetch worker states for debugging") }

    fun getNumRequests(): Int = queueMutex.withLock { requestMap.size }

    fun getNumHTTPRequests(): Int = networkQueueMutex.withLock { httpTextureQueue.size }

    fun getTotalNumHTTPRequests(): UInt = totalHttpRequests

    fun getPending(): Int = TODO("APR: return count of all pending queued workers")

    fun lockQueue() = queueMutex.lock()
    fun unlockQueue() = queueMutex.unlock()

    fun getWorker(id: UUID): TextureFetchWorker? = queueMutex.withLock { requestMap[id] }
    fun getWorkerAfterLock(id: UUID): TextureFetchWorker? = requestMap[id]

    fun commandSetRegion(regionHandle: ULong) {
        cmdEnqueue(object : TFRequest() {})
    }

    fun commandSendMetrics(capsUrl: String, sessionId: UUID, agentId: UUID, statsSd: Any) {
        cmdEnqueue(object : TFRequest() {})
    }

    fun commandDataBreak() {
        svMetricsDataBreak = true
        cmdEnqueue(object : TFRequest() {})
    }

    fun addHttpWaiter(tid: UUID) {
        networkQueueMutex.withLock { httpWaitResource.add(tid) }
    }

    fun removeHttpWaiter(tid: UUID) {
        networkQueueMutex.withLock { httpWaitResource.remove(tid) }
    }

    fun isHttpWaiter(tid: UUID): Boolean = networkQueueMutex.withLock { tid in httpWaitResource }

    fun releaseHttpWaiters() {
        TODO("APR: promote waiting HTTP workers to active state up to high-water mark")
    }

    fun cancelHttpWaiters() {
        networkQueueMutex.withLock { httpWaitResource.clear() }
    }

    fun getHttpWaitersCount(): Int = networkQueueMutex.withLock { httpWaitResource.size }

    fun updateStateStats(cacheRead: UInt, cacheWrite: UInt, resWait: UInt) {
        queueMutex.withLock {
            totalCacheReadCount += cacheRead
            totalCacheWriteCount += cacheWrite
            totalResourceWaitCount += resWait
        }
    }

    fun getStateStats(cacheRead: UInt, cacheWrite: UInt, resWait: UInt): Triple<UInt, UInt, UInt> =
        queueMutex.withLock { Triple(totalCacheReadCount, totalCacheWriteCount, totalResourceWaitCount) }

    protected fun addToNetworkQueue(worker: TextureFetchWorker) {
        networkQueueMutex.withLock { networkQueue.add(worker.id) }
    }

    protected fun removeFromNetworkQueue(worker: TextureFetchWorker, cancel: Boolean) {
        networkQueueMutex.withLock {
            networkQueue.remove(worker.id)
            if (cancel) cancelQueue.values.forEach { it.remove(worker.id) }
        }
    }

    protected fun addToHTTPQueue(id: UUID) {
        networkQueueMutex.withLock { httpTextureQueue.add(id) }
    }

    protected fun removeFromHTTPQueue(id: UUID, receivedSize: Int) {
        networkQueueMutex.withLock { httpTextureQueue.remove(id) }
    }

    protected fun removeRequest(worker: TextureFetchWorker, cancel: Boolean) {
        TODO("APR: remove worker from requestMap and optionally cancel I/O")
    }

    protected fun runCondition(): Boolean = requestMap.isNotEmpty() || commands.isNotEmpty()

    private fun sendRequestListToSimulators() {
        TODO("APR: batch-send pending UDP texture requests to simulator hosts")
    }

    private fun startThread() { TODO("APR: initialize HTTP library handles on fetch thread startup") }
    private fun endThread() { TODO("APR: release HTTP library handles on fetch thread shutdown") }
    private fun threadedUpdate() { commonUpdate() }

    private fun commonUpdate() {
        TODO("APR: process HTTP completions, advance worker state machines")
    }

    private fun cmdEnqueue(request: TFRequest) {
        queueMutex.withLock { commands.add(request) }
    }

    private fun cmdDequeue(): TFRequest? = queueMutex.withLock {
        if (commands.isEmpty()) null else commands.removeAt(0)
    }

    private fun cmdDoWork() {
        val req = cmdDequeue() ?: return
        TODO("APR: execute command request $req")
    }

    fun setLoadSource(source: TexSource) { fetchSource = source }
    fun resetLoadSource() { TODO("APR: restore fetch source to origin value") }
    fun canLoadFromCache(): Boolean = fetchSource != TexSource.FROM_HTTP_ONLY

    companion object {
        fun getStateString(state: Int): String =
            if (state in FETCH_STATE_NAMES.indices) FETCH_STATE_NAMES[state] else "UNKNOWN($state)"

        @Volatile var svMetricsDataBreak: Boolean = false
        var sTesterp: TextureFetchTester? = null
    }
}

class TextureFetchWorker(
    val id: UUID,
    var imagePriority: Float,
    var fetchState: Int = 0
) {
    fun callbackCacheRead(success: Boolean, formattedImage: Any?, imageSize: Int, imageLocal: Boolean) {
        TODO("APR: handle cache read completion for texture $id")
    }

    fun callbackCacheWrite(success: Boolean) {
        TODO("APR: handle cache write completion for texture $id")
    }

    fun callbackDecoded(success: Boolean, errorMessage: String, raw: Any?, aux: Any?, requestId: UInt) {
        TODO("GPU: handle image decode completion for texture $id")
    }

    fun callbackHttpGet(response: Any, partial: Boolean, success: Boolean): Int {
        TODO("APR: handle HTTP GET response for texture $id")
    }

    fun doWork(param: Int): Boolean { TODO("APR: advance worker state machine for texture $id") }
    fun finishWork(param: Int, completed: Boolean) { TODO("APR: finalize worker on completion") }
    fun deleteOK(): Boolean { TODO("APR: check if worker can be safely deleted") }
}

class TextureFetchTester {
    private var textureFetchTime: Float = 0f
    private var skippedStatesTime: Float = 0f
    private var fileSize: Int = 0
    private val stateTimersMap: MutableMap<Int, Float> = mutableMapOf()

    fun updateStats(statesTimers: Map<Int, Float>, fetchTime: Float, otherStatesTime: Float, fileSize: Int) {
        textureFetchTime = fetchTime
        skippedStatesTime = otherStatesTime
        this.fileSize = fileSize
        stateTimersMap.clear()
        stateTimersMap.putAll(statesTimers)
    }

    private fun outputTestRecord() { TODO("APR: write fetch tester stats to LLSD log") }
}
