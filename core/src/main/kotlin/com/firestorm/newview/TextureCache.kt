package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val TEXTURE_CACHE_ENTRY_SIZE = 600
private const val TEXTURE_CACHE_PURGE_AMOUNT = 0.20f
private const val TEXTURE_CACHE_LRU_SIZE = 0.10f
private const val TEXTURE_FAST_CACHE_ENTRY_OVERHEAD = 16
private const val TEXTURE_FAST_CACHE_DATA_SIZE = 16 * 16 * 4
private const val TEXTURE_FAST_CACHE_ENTRY_SIZE = TEXTURE_FAST_CACHE_DATA_SIZE + TEXTURE_FAST_CACHE_ENTRY_OVERHEAD
private const val TEXTURE_LAZY_PURGE_TIME_LIMIT = 0.004f
private const val TEXTURE_PRUNING_MAX_TIME = 15f

data class CacheEntriesInfo(
    var version: Float = 0f,
    var addressSize: UInt = 0u,
    var encoderVersion: String = "",
    var entries: UInt = 0u
)

data class CacheEntry(
    var id: UUID = NULL_UUID,
    var imageSize: Int = 0,
    var bodySize: Int = 0,
    var time: UInt = 0u
) {
    fun init(id: UUID, time: UInt) {
        this.id = id
        this.imageSize = 0
        this.bodySize = 0
        this.time = time
    }
}

abstract class CacheResponder {
    abstract fun setData(data: ByteArray?, dataSize: Int, imageSize: Int, imageFormat: Int, imageLocal: Boolean)
    open fun completed(success: Boolean) {}
}

class ReadResponder : CacheResponder() {
    var formattedImage: Any? = null
    var imageSize: Int = 0
    var imageLocal: Boolean = false

    override fun setData(data: ByteArray?, dataSize: Int, imageSize: Int, imageFormat: Int, imageLocal: Boolean) {
        this.imageSize = imageSize
        this.imageLocal = imageLocal
        System.err.println("APR: populate formattedImage from raw data bytes")
    }

    fun setImage(image: Any?) { formattedImage = image }
}

class WriteResponder : CacheResponder() {
    override fun setData(data: ByteArray?, dataSize: Int, imageSize: Int, imageFormat: Int, imageLocal: Boolean) {
        // not used for write path
    }
}

class TextureCache(private val threaded: Boolean) {

    private val workersMutex = ReentrantLock()
    private val headerMutex = ReentrantLock()
    private val listMutex = ReentrantLock()
    private val fastCacheMutex = ReentrantLock()

    private val readers: MutableMap<Long, Any> = mutableMapOf()
    private val writers: MutableMap<Long, Any> = mutableMapOf()
    private val prioritizeWriteList: MutableList<Long> = mutableListOf()
    private val completedList: MutableList<Pair<CacheResponder, Boolean>> = mutableListOf()

    private var readOnly: Boolean = false
    private var cacheParentDirName: String = ""
    private var headerEntriesFileName: String = ""
    private var headerDataFileName: String = ""
    private var fastCacheFileName: String = ""
    private var headerEntriesInfo: CacheEntriesInfo = CacheEntriesInfo()

    private val freeList: MutableSet<Int> = mutableSetOf()
    private val lru: MutableSet<UUID> = mutableSetOf()
    private val headerIdMap: MutableMap<UUID, Int> = mutableMapOf()

    private var fastCachePadBuffer: ByteArray? = null

    private var texturesDirName: String = ""
    private val texturesSizeMap: MutableMap<UUID, Int> = mutableMapOf()
    private var texturesSizeTotal: Long = 0L
    @Volatile private var doPurge: Boolean = false

    private val updatedEntryMap: MutableMap<Int, CacheEntry> = mutableMapOf()
    private val purgeEntryList: MutableList<Pair<Int, CacheEntry>> = mutableListOf()

    fun update(maxTimeMs: Float): Int {
        System.err.println("APR: process completed read/write workers and fire responder callbacks")
        return 0
    }

    fun purgeCache(locationPath: String, removeDir: Boolean = true) {
        System.err.println("APR: delete all cached texture files under locationPath")
    }

    fun setReadOnly(readOnly: Boolean) { this.readOnly = readOnly }

    fun initCache(locationPath: String, maxSize: Long, textureCacheMismatch: Boolean): Long {
        System.err.println("APR: initialize header and body cache directories, read existing entries")
        return 0L
    }

    fun readFromCache(localFilename: String, id: UUID, offset: Int, size: Int, responder: ReadResponder): Long {
        System.err.println("APR: enqueue local-file cache read worker")
        return 0L
    }

    fun readFromCache(id: UUID, offset: Int, size: Int, responder: ReadResponder): Long {
        System.err.println("APR: enqueue remote cache read worker for texture id $id")
        return 0L
    }

    fun readComplete(handle: Long, abort: Boolean): Boolean {
        System.err.println("APR: check if read worker for handle is complete")
        return false
    }

    fun writeToCache(
        id: UUID,
        data: ByteArray,
        dataSize: Int,
        imageSize: Int,
        rawImage: Any?,
        discardLevel: Int,
        responder: WriteResponder
    ): Long {
        System.err.println("APR: enqueue cache write worker for texture id $id")
        return 0L
    }

    fun readFromFastCache(id: UUID, discardLevel: Int): Pair<Any?, Int> {
        System.err.println("APR: synchronously read fast-cache thumbnail for texture id $id")
        return Pair(null, 0)
    }

    fun writeComplete(handle: Long, abort: Boolean = false): Boolean {
        System.err.println("APR: check if write worker for handle is complete")
        return false
    }

    fun prioritizeWrite(handle: Long) {
        listMutex.withLock { prioritizeWriteList.add(handle) }
    }

    fun removeFromCache(id: UUID): Boolean {
        System.err.println("APR: remove texture entry from header and body cache files")
        return false
    }

    fun lockWorkers() = workersMutex.lock()
    fun unlockWorkers() = workersMutex.unlock()

    fun getNumReads(): Int = readers.size
    fun getNumWrites(): Int = writers.size
    fun getUsage(): Long = texturesSizeTotal
    fun getMaxUsage(): Long = sCacheMaxTexturesSize
    fun getEntries(): UInt = headerEntriesInfo.entries
    fun getMaxEntries(): UInt = sCacheMaxEntries

    fun isInCache(id: UUID): Boolean = headerMutex.withLock { id in headerIdMap }
    fun isInLocal(id: UUID): Boolean {
        System.err.println("APR: check if local texture file exists for id $id")
        return false
    }

    fun getLocalFileName(id: UUID): String {
        System.err.println("APR: compute local filesystem path for texture id $id")
        return ""
    }

    fun getTextureFileName(id: UUID): String {
        System.err.println("APR: compute body cache filesystem path for texture id $id")
        return ""
    }

    fun addCompleted(responder: CacheResponder, success: Boolean) {
        listMutex.withLock { completedList.add(Pair(responder, success)) }
    }

    private fun setDirNames(locationPath: String) {
        cacheParentDirName = locationPath
        headerEntriesFileName = "$locationPath/texture.entries"
        headerDataFileName = "$locationPath/texture.cache"
        fastCacheFileName = "$locationPath/FastCache.cache"
        texturesDirName = "$locationPath/textures"
    }

    private fun readHeaderCache() { System.err.println("APR: read header entries file into headerIdMap") }
    private fun clearCorruptedCache() { System.err.println("APR: remove and reinitialize corrupted cache files") }
    private fun purgeAllTextures(purgeDirectories: Boolean) { System.err.println("APR: delete all texture body files") }
    private fun purgeTexturesLazy(timeLimitSec: Float) { System.err.println("APR: evict LRU textures within time budget") }
    private fun purgeTextures(validate: Boolean) { System.err.println("APR: evict textures to bring cache within size limit") }

    private fun readEntriesHeader() { System.err.println("APR: read EntriesInfo header from texture.entries") }
    private fun setEntriesHeader() { System.err.println("APR: initialize in-memory EntriesInfo header") }
    private fun writeEntriesHeader() { System.err.println("APR: flush EntriesInfo header to texture.entries") }

    private fun openAndReadEntry(id: UUID, entry: CacheEntry, create: Boolean): Int {
        System.err.println("APR: look up or create header entry for texture id $id")
        return 0
    }

    private fun updateEntry(idx: Int, entry: CacheEntry, newImageSize: Int, newBodySize: Int): Boolean {
        System.err.println("APR: update cached entry sizes and timestamp")
        return false
    }

    private fun updateEntryTimeStamp(idx: Int, entry: CacheEntry) {
        System.err.println("APR: refresh LRU timestamp for entry at index $idx")
    }

    private fun openAndReadEntries(entries: MutableList<CacheEntry>): UInt {
        System.err.println("APR: read all header entries from texture.entries file")
        return 0u
    }

    private fun writeEntriesAndClose(entries: List<CacheEntry>) {
        System.err.println("APR: write all header entries back to texture.entries file")
    }

    private fun readEntryFromHeaderImmediately(idx: Int, entry: CacheEntry) {
        System.err.println("APR: synchronously read single entry at byte offset idx*TEXTURE_CACHE_ENTRY_SIZE")
    }

    private fun writeEntryToHeaderImmediately(idx: Int, entry: CacheEntry, writeHeader: Boolean = false) {
        System.err.println("APR: synchronously write single entry at byte offset idx*TEXTURE_CACHE_ENTRY_SIZE")
    }

    private fun removeEntry(idx: Int, entry: CacheEntry, filename: String) {
        System.err.println("APR: free header slot and delete body file for entry")
    }

    private fun removeCachedTexture(id: UUID) {
        System.err.println("APR: remove texture entry and body file for $id")
    }

    private fun getHeaderCacheEntry(id: UUID, entry: CacheEntry): Int {
        return headerMutex.withLock { headerIdMap[id] ?: -1 }
    }

    private fun setHeaderCacheEntry(id: UUID, entry: CacheEntry, imageSize: Int, dataSize: Int): Int {
        System.err.println("APR: create or update header entry for texture $id with sizes")
        return 0
    }

    private fun writeUpdatedEntries() {
        System.err.println("APR: flush all pending entry updates from updatedEntryMap to disk")
    }

    private fun updatedHeaderEntriesFile() {
        System.err.println("APR: rewrite header file with all entries from headerIdMap")
    }

    private fun lockHeaders() = headerMutex.lock()
    private fun unlockHeaders() = headerMutex.unlock()

    private fun openFastCache(firstTime: Boolean = false) {
        System.err.println("APR: open or create fast-cache file at fastCacheFileName")
    }

    private fun closeFastCache(forced: Boolean = false) {
        System.err.println("APR: close fast-cache file handle")
    }

    private fun writeToFastCache(imageId: UUID, cacheId: Int, raw: Any?, discardLevel: Int): Boolean {
        System.err.println("APR: write 16x16 thumbnail pixel data to fast-cache at position cacheId")
        return false
    }

    companion object {
        var headerCacheVersion: Float = 0f
        var headerCacheAddressSize: UInt = 0u
        var headerCacheEncoderVersion: String = ""
        var sCacheMaxEntries: UInt = 0u
        var sCacheMaxTexturesSize: Long = 0L
        const val HEADER_ENCODER_STRING_SIZE = 32
    }
}

abstract class TextureCacheWorker(
    protected val cache: TextureCache,
    protected val id: UUID,
    protected val writeData: ByteArray?,
    protected var dataSize: Int,
    protected val offset: Int,
    protected var imageSize: Int,
    protected val responder: CacheResponder?
) {
    var readData: ByteArray? = null
    var imageFormat: Int = 0
    var imageLocal: Boolean = false
    var bytesToRead: Int = 0
    @Volatile var bytesRead: Int = 0

    abstract fun doRead(): Boolean
    abstract fun doWrite(): Boolean

    fun doWork(param: Int): Boolean {
        System.err.println("APR: dispatch to doRead or doWrite based on param")
        return false
    }
    fun ioComplete(bytes: Int) { bytesRead = bytes }

    private fun startWork(param: Int) {}
    private fun finishWork(param: Int, completed: Boolean) { System.err.println("APR: notify responder on completion") }
    private fun endWork(param: Int, aborted: Boolean) { System.err.println("APR: clean up worker state") }
}

class TextureCacheLocalFileWorker(
    cache: TextureCache,
    private val fileName: String,
    id: UUID,
    data: ByteArray?,
    dataSize: Int,
    offset: Int,
    imageSize: Int,
    responder: CacheResponder?
) : TextureCacheWorker(cache, id, data, dataSize, offset, imageSize, responder) {

    override fun doRead(): Boolean {
        System.err.println("APR: read texture data from local file $fileName")
        return false
    }

    override fun doWrite(): Boolean = false
}

class TextureCacheRemoteWorker(
    cache: TextureCache,
    id: UUID,
    data: ByteArray?,
    dataSize: Int,
    offset: Int,
    imageSize: Int,
    private val rawImage: Any?,
    private val rawDiscardLevel: Int,
    responder: CacheResponder?
) : TextureCacheWorker(cache, id, data, dataSize, offset, imageSize, responder) {

    private enum class State { INIT, LOCAL, CACHE, HEADER, BODY }

    private var state: State = State.INIT

    override fun doRead(): Boolean {
        System.err.println("APR: state-machine read: check local files, then header cache, then body cache file")
        return false
    }

    override fun doWrite(): Boolean {
        System.err.println("APR: state-machine write: update header entry and write body file")
        return false
    }
}
