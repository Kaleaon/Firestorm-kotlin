package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

const val MIN_VRAM_BUDGET = 768f
val NULL_UUID: UUID = UUID(0, 0)

typealias LoadedCallbackFunc = (
    success: Boolean,
    srcVi: ViewerFetchedTexture?,
    src: Any?,
    srcAux: Any?,
    discardLevel: Int,
    final: Boolean,
    userData: Any?
) -> Unit

enum class TexListType { STANDARD, SCALE }

enum class FTType(val value: Int) {
    UNKNOWN(-1),
    DEFAULT(0),
    SERVER_BAKE(1),
    HOST_BAKE(2),
    MAP_TILE(3),
    LOCAL_FILE(4);

    override fun toString(): String = when (this) {
        UNKNOWN -> "unknown"
        DEFAULT -> "default"
        SERVER_BAKE -> "server_bake"
        HOST_BAKE -> "host_bake"
        MAP_TILE -> "map_tile"
        LOCAL_FILE -> "local_file"
    }
}

data class TextureKey(
    val textureId: UUID = NULL_UUID,
    val textureType: TexListType = TexListType.STANDARD
) : Comparable<TextureKey> {
    override fun compareTo(other: TextureKey): Int {
        val cmp = textureId.compareTo(other.textureId)
        return if (cmp != 0) cmp else textureType.ordinal - other.textureType.ordinal
    }
}

class LoadedCallbackEntry(
    val callback: LoadedCallbackFunc,
    val desiredDiscard: Int,
    val needsImageRaw: Boolean,
    val userData: Any?,
    val sourceCallbackList: MutableSet<TextureKey>?,
    target: ViewerFetchedTexture,
    var paused: Boolean
) {
    var lastUsedDiscard: Int = ViewerTexture.MAX_DISCARD_LEVEL + 1

    init {
        sourceCallbackList?.add(TextureKey(target.id, TexListType.values()[target.textureListType]))
    }

    fun removeTexture(tex: ViewerFetchedTexture) {
        sourceCallbackList?.remove(TextureKey(tex.id, TexListType.values()[tex.textureListType]))
    }

    companion object {
        fun cleanUpCallbackList(callbackList: MutableSet<TextureKey>?) {
            if (callbackList.isNullOrEmpty()) return
            for (key in callbackList) {
                val tex = ViewerTextureList.instance.findImage(key)
                tex?.deleteCallbackEntry(callbackList)
            }
            callbackList.clear()
        }
    }
}

open class ViewerTexture(var useMipMaps: Boolean = true) {

    enum class TextureType {
        LOCAL_TEXTURE,
        MEDIA_TEXTURE,
        DYNAMIC_TEXTURE,
        FETCHED_TEXTURE,
        LOD_TEXTURE,
        INVALID_TEXTURE_TYPE
    }

    enum class DebugTexels {
        DEBUG_TEXELS_OFF,
        DEBUG_TEXELS_CURRENT,
        DEBUG_TEXELS_DESIRED,
        DEBUG_TEXELS_FULL
    }

    open var id: UUID = UUID.randomUUID()
    var textureListType: Int = 0
    var boostLevel: Int = BOOST_NONE
    var fullWidth: Int = 0
    var fullHeight: Int = 0
    var parcelMedia: ViewerMediaTexture? = null

    @Volatile var maxVirtualSize: Float = 0f
    var maxVirtualSizeResetCounter: Int = 1
    var maxVirtualSizeResetInterval: Int = 1

    val faceList: Array<MutableList<Any>> = Array(NUM_TEXTURE_CHANNELS) { mutableListOf() }
    val numFaces: IntArray = IntArray(NUM_TEXTURE_CHANNELS)
    val volumeList: Array<MutableList<Any>> = Array(NUM_VOLUME_TEXTURE_CHANNELS) { mutableListOf() }
    val numVolumes: IntArray = IntArray(NUM_VOLUME_TEXTURE_CHANNELS)

    val materialList: MutableList<MaterialEntry> = mutableListOf()

    data class MaterialEntry(val index: Int = -1, val asset: Any? = null)

    open fun getType(): Byte = TextureType.LOCAL_TEXTURE.ordinal.toByte()
    open fun isMissingAsset(): Boolean = false
    open fun dump() {
        // GPU: dump texture info to log
    }
    open fun isViewerMediaTexture(): Boolean = false
    open fun bindDefaultImage(stage: Int = 0): Boolean {
        // GPU: bind default image to texture unit $stage
        return false
    }
    open fun bindDebugImage(stage: Int = 0): Boolean {
        // GPU: bind debug checkerboard image to texture unit $stage
        return false
    }
    open fun forceImmediateUpdate() {}
    open fun isActiveFetching(): Boolean = false
    open fun getMaxVirtualSize(): Float = maxVirtualSize

    open fun setKnownDrawSize(width: Int, height: Int) {}

    open fun setBoostLevel(level: Int) {
        if (boostLevel != level) boostLevel = level
        if (boostLevel >= BOOST_HIGH) maxVirtualSize = 2048f * 2048f
    }

    fun addTextureStats(virtualSize: Float, needsGlTexture: Boolean = true) {
        val capped = min(virtualSize, ViewerFetchedTexture.MAX_VIRTUAL_SIZE)
        if (capped > maxVirtualSize) maxVirtualSize = capped
    }

    fun resetTextureStats() {
        maxVirtualSize = 0f
        maxVirtualSizeResetCounter = 0
    }

    fun setMaxVirtualSizeResetInterval(interval: Int) { maxVirtualSizeResetInterval = interval }
    fun resetMaxVirtualSizeResetCounter() { maxVirtualSizeResetCounter = maxVirtualSizeResetInterval }
    fun getMaxVirtualSizeResetCounter(): Int = maxVirtualSizeResetCounter

    open fun addFace(channel: UInt, face: Any) {
        val ch = channel.toInt()
        faceList[ch].add(face)
        numFaces[ch]++
    }

    open fun removeFace(channel: UInt, face: Any) {
        val ch = channel.toInt()
        faceList[ch].remove(face)
        numFaces[ch] = faceList[ch].size
    }

    fun getTotalNumFaces(): Int = numFaces.sum()
    fun getNumFaces(ch: UInt): Int = numFaces[ch.toInt()]
    fun getFaceList(channel: UInt): List<Any> = faceList[channel.toInt()]

    open fun addVolume(channel: UInt, volume: Any) {
        val ch = channel.toInt()
        volumeList[ch].add(volume)
        numVolumes[ch]++
    }

    open fun removeVolume(channel: UInt, volume: Any) {
        val ch = channel.toInt()
        volumeList[ch].remove(volume)
        numVolumes[ch] = volumeList[ch].size
    }

    fun getNumVolumes(channel: UInt): Int = numVolumes[channel.toInt()]
    fun getVolumeList(channel: UInt): List<Any> = volumeList[channel.toInt()]

    fun isLargeImage(): Boolean = fullWidth.toLong() * fullHeight.toLong() >= sMinLargeImageSize.toLong()
    fun isInvisiprim(): Boolean = isInvisiprim(id)

    fun hasParcelMedia(): Boolean = parcelMedia != null

    fun updateBindStatsForTester() {
        // APR: update bind stats via tester
    }

    companion object {
        const val BOOST_NONE = 0
        const val BOOST_AVATAR = 1
        const val BOOST_AVATAR_BAKED = 2
        const val BOOST_AVATAR_SELF = 3
        const val BOOST_AVATAR_BAKED_SELF = 4
        const val BOOST_TERRAIN = 5
        const val BOOST_SELECTED = 6
        const val BOOST_SKY = 7
        const val BOOST_TREE = 8
        const val BOOST_BUMP = 9
        const val BOOST_HIGH = 10
        const val BOOST_ICON = 11
        const val BOOST_THUMBNAIL = 12
        const val BOOST_UI = 13
        const val BOOST_PREVIEW = 14
        const val BOOST_OTHER = 15

        const val NUM_TEXTURE_CHANNELS = 3
        const val NUM_VOLUME_TEXTURE_CHANNELS = 2
        const val MAX_DISCARD_LEVEL = 5
        const val CURRENT_FILE_VERSION: UInt = 1u

        var imageCount: Int = 0
        var rawCount: Int = 0
        var auxCount: Int = 0
        var desiredDiscardBias: Float = 0f
        var biasTexturesUpdated: UInt = 0u
        var maxSculptRez: Int = 128
        var sMinLargeImageSize: UInt = 65536u
        var sMaxSmallImageSize: UInt = 4096u
        var freezeImageUpdates: Boolean = false
        var currentTime: Float = 0f
        var freeVRAMMegabytes: Float = MIN_VRAM_BUDGET
        var debugTexelsMode: DebugTexels = DebugTexels.DEBUG_TEXELS_OFF

        var nullImagep: ViewerTexture? = null
        var blackImagep: ViewerTexture? = null
        var checkerBoardImagep: ViewerTexture? = null

        val INVISIPRIM_TEXTURE_1: UUID = UUID.fromString("e97cf410-8e61-7005-ec06-629eba4cd1fb")
        val INVISIPRIM_TEXTURE_2: UUID = UUID.fromString("38b86f85-2575-52a9-a531-23108d8da837")

        fun isInvisiprim(id: UUID): Boolean =
            id == INVISIPRIM_TEXTURE_1 || id == INVISIPRIM_TEXTURE_2

        fun initClass() {
            // GPU: set default GL texture reference from sDefaultImagep
        }

        fun updateClass() {
            // GPU: compute VRAM usage, update desiredDiscardBias and freezeImageUpdates
        }

        fun isSystemMemoryLow(): Boolean {
            // APR: query available physical memory via JVM Runtime
            return false
        }
        fun isSystemMemoryCritical(): Boolean {
            // APR: query available physical memory via JVM Runtime
            return false
        }
        fun getSystemMemoryBudgetFactor(): Float {
            // APR: compute memory budget factor from free/threshold ratio
            return 0f
        }
    }
}

open class ViewerFetchedTexture(
    val fetchType: FTType = FTType.DEFAULT,
    useMipMaps: Boolean = true
) : ViewerTexture(useMipMaps) {

    var origWidth: Int = 0
    var origHeight: Int = 0
    var knownDrawWidth: Int = 0
    var knownDrawHeight: Int = 0
    var knownDrawSizeChanged: Boolean = false
    var url: String = ""

    var lastWorkerDiscardLevel: Int = -1
    var requestedDiscardLevel: Int = -1
    var requestedDownloadPriority: Float = 0f
    var fetchState: Int = 0
    var lastFetchState: Int = -1
    var fetchPriority: UInt = 0u
    var downloadProgress: Float = 0f
    var fetchDeltaTime: Float = 0f
    var requestDeltaTime: Float = 0f
    var minDiscardLevel: Int = 0
    var desiredDiscardLevel: Byte = MAX_DISCARD_LEVEL.toByte()
    var minDesiredDiscardLevel: Byte = MAX_DISCARD_LEVEL.toByte()

    var needsAux: Boolean = false
    var hasAux: Boolean = false
    var decodingAux: Boolean = false
    var isRawImageValid: Boolean = false
    var hasFetcher: Boolean = false
    var isFetching: Boolean = false
    var canUseHttp: Boolean = true
    private var _isMissingAsset: Boolean = false

    var loadedCallbackDesiredDiscardLevel: Byte = 0
    var pauseLoadedCallbacks: Boolean = false
    val loadedCallbackList: MutableList<LoadedCallbackEntry> = mutableListOf()
    var lastCallbackActiveTime: Float = 0f

    var rawImage: Any? = null
    var rawDiscardLevel: Int = -1
    var auxRawImage: Any? = null

    var forceToSaveRawImage: Boolean = false
    var saveRawImage: Boolean = false
    var savedRawImage: Any? = null
    var savedRawDiscardLevel: Int = -1
    var desiredSavedRawDiscardLevel: Int = -1
    var lastReferencedSavedRawImageTime: Float = 0f
    var keptSavedRawImageTime: Float = 0f

    var targetHost: String = ""
    var inImageList: Boolean = false
    val needsCreateTexture: AtomicBoolean = AtomicBoolean(false)
    var forSculpt: Boolean = false
    var isFetched: Boolean = false
    var createPending: Boolean = false
    var downScalePending: Boolean = false

    val comment: MutableMap<String, String> = mutableMapOf()

    private var fullLoaded: Boolean = false
    private var inFastCacheList: Boolean = false

    override fun getType(): Byte = TextureType.FETCHED_TEXTURE.ordinal.toByte()
    fun getFTType(): FTType = fetchType

    override fun isMissingAsset(): Boolean = _isMissingAsset
    fun setIsMissingAsset(missing: Boolean = true) { _isMissingAsset = missing }

    override fun forceImmediateUpdate() {
        // GPU: force immediate texture fetch and upload
    }

    override fun setKnownDrawSize(width: Int, height: Int) {
        if (knownDrawWidth != width || knownDrawHeight != height) {
            knownDrawWidth = width
            knownDrawHeight = height
            knownDrawSizeChanged = true
        }
    }

    fun hasCallbacks(): Boolean = loadedCallbackList.isNotEmpty()

    fun setLoadedCallback(
        cb: LoadedCallbackFunc,
        discardLevel: Int,
        keepImageRaw: Boolean,
        needsAuxArg: Boolean,
        userData: Any?,
        srcCallbackList: MutableSet<TextureKey>?,
        pause: Boolean = false
    ) {
        loadedCallbackList.add(
            LoadedCallbackEntry(cb, discardLevel, keepImageRaw, userData, srcCallbackList, this, pause)
        )
    }

    fun pauseLoadedCallbacks(callbackList: Set<TextureKey>?) {
        loadedCallbackList.filter { it.sourceCallbackList === callbackList }.forEach { it.paused = true }
    }

    fun unpauseLoadedCallbacks(callbackList: Set<TextureKey>?) {
        loadedCallbackList.filter { it.sourceCallbackList === callbackList }.forEach { it.paused = false }
    }

    fun deleteCallbackEntry(callbackList: Set<TextureKey>?) {
        loadedCallbackList.removeAll { it.sourceCallbackList === callbackList }
    }

    fun clearCallbackEntryList() { loadedCallbackList.clear() }

    fun doLoadedCallbacks(): Boolean {
        // GPU: invoke pending loaded callbacks
        return false
    }

    fun addToCreateTexture() {
        // GPU: enqueue texture for GL upload
    }
    fun preCreateTexture(useName: Int = 0): Boolean {
        // GPU: pre-allocate GL texture name
        return false
    }
    fun createTexture(useName: Int = 0): Boolean {
        // GPU: create and upload GL texture
        return false
    }
    fun postCreateTexture() {
        // GPU: finalize GL texture after upload
    }
    fun scheduleCreateTexture() {
        // GPU: schedule GL texture creation on image worker thread
    }
    fun destroyTexture() {
        // GPU: delete GL texture object
    }

    open fun processTextureStats() {
        // GPU: compute desired discard from virtual size
    }

    fun setMinDiscardLevel(discard: Int) {
        minDesiredDiscardLevel = minOf(minDesiredDiscardLevel, discard.toByte())
    }

    fun updateFetch(): Boolean {
        // GPU: step texture fetch state machine
        return false
    }
    fun clearFetchedResults() {
        // GPU: clear raw image buffers and reset fetch state
    }
    fun updateVirtualSize() {
        // GPU: recompute maxVirtualSize from attached faces/volumes
    }
    fun setDebugText(text: String) {
        // GPU: propagate debug text to attached viewer objects
    }

    fun setTargetHost(host: String) { targetHost = host }
    fun getTargetHost(): String = targetHost
    fun getDesiredDiscardLevel(): Int = desiredDiscardLevel.toInt()
    fun getOriginalWidth(): Int = origWidth
    fun getOriginalHeight(): Int = origHeight

    fun isInImageList(): Boolean = inImageList
    fun setInImageList(flag: Boolean) { inImageList = flag }
    fun getFetchPriority(): UInt = fetchPriority
    fun getDownloadProgress(): Float = downloadProgress

    fun destroyRawImage() { rawImage = null; rawDiscardLevel = -1 }
    fun needsToSaveRawImage(): Boolean = forceToSaveRawImage || saveRawImage
    fun getUseDiscard(): Boolean = useMipMaps

    fun setForSculpt() { forSculpt = true }
    fun isForSculptOnly(): Boolean {
        // GPU: check sculpt-only usage flag
        return false
    }

    fun getRawImage(): Any? = rawImage
    fun getRawImageLevel(): Int = rawDiscardLevel
    fun isRawImageValid(): Boolean = isRawImageValid

    fun forceToSaveRawImage(desiredDiscard: Int = 0, keptTime: Float = 0f) {
        forceToSaveRawImage = true
        desiredSavedRawDiscardLevel = desiredDiscard
        keptSavedRawImageTime = keptTime
    }

    fun readbackRawImage() {
        // GPU: readback raw pixel data from OpenGL texture
    }

    fun destroySavedRawImage() { savedRawImage = null }
    fun getSavedRawImage(): Any? = savedRawImage
    fun getSavedRawImageLevel(): Int = savedRawDiscardLevel
    fun hasSavedRawImage(): Boolean = savedRawImage != null
    fun getElapsedLastReferencedSavedRawImageTime(): Float =
        currentTime - lastReferencedSavedRawImageTime

    fun isFullyLoaded(): Boolean = fullLoaded
    fun setCanUseHTTP(canUse: Boolean) { canUseHttp = canUse }
    fun forceToDeleteRequest() {
        // GPU: cancel and remove active fetch request
    }
    fun loadFromFastCache() {
        // APR: load low-res thumbnail from fast-cache file
    }
    fun setInFastCacheList(inList: Boolean) { inFastCacheList = inList }
    fun isInFastCacheList(): Boolean = inFastCacheList

    override fun isActiveFetching(): Boolean = isFetching

    open fun scaleDown(): Boolean = false

    fun forceToRefetchTexture(desiredDiscard: Int = 0, keptTime: Float = 60f) {
        // GPU: invalidate current data and re-issue fetch request
    }

    companion object {
        const val MAX_IMAGE_SIZE_DEFAULT = 2048
        const val MAX_VIRTUAL_SIZE = 8192f * 8192f

        var missingAssetImagep: ViewerFetchedTexture? = null
        var whiteImagep: ViewerFetchedTexture? = null
        var defaultImagep: ViewerFetchedTexture? = null
        var smokeImagep: ViewerFetchedTexture? = null
        var flatNormalImagep: ViewerFetchedTexture? = null
        var defaultIrradiancePBRp: ViewerFetchedTexture? = null
        var defaultParticleImagep: ViewerFetchedTexture? = null
        var defaultDiffuseImagep: ViewerFetchedTexture? = null

        fun getSmokeImage(): ViewerFetchedTexture? = smokeImagep
    }
}

open class ViewerLODTexture(
    fetchType: FTType = FTType.DEFAULT,
    useMipMaps: Boolean = true
) : ViewerFetchedTexture(fetchType, useMipMaps) {

    override fun getType(): Byte = TextureType.LOD_TEXTURE.ordinal.toByte()

    override fun processTextureStats() {
        // GPU: compute discard level from camera-distance-based virtual size
    }

    fun isUpdateFrozen(): Boolean = freezeImageUpdates

    override fun scaleDown(): Boolean {
        // GPU: force texture to lower discard level
        return false
    }
}

class ViewerMediaTexture(
    override var id: UUID = UUID.randomUUID(),
    useMipMaps: Boolean = true
) : ViewerTexture(useMipMaps) {

    private val mediaFaceList: MutableList<Any> = mutableListOf()
    private val replacedTextureList: MutableList<ViewerTexture> = mutableListOf()
    private var mediaImpl: Any? = null
    private var isPlayingState: Boolean = false
    private var updateVirtualSizeTime: UInt = 0u

    override fun getType(): Byte = TextureType.MEDIA_TEXTURE.ordinal.toByte()

    fun reinit(useMipMaps: Boolean = true) {
        // GPU: reinitialize media texture GL state
    }
    fun getUseMipMaps(): Boolean = useMipMaps
    fun setUseMipMaps(mipmap: Boolean) { this.useMipMaps = mipmap }

    fun setPlaying(playing: Boolean) {
        if (playing != isPlayingState) {
            isPlayingState = playing
            if (!playing) stopPlaying()
        }
    }

    fun isPlaying(): Boolean = isPlayingState
    fun setMediaImpl() {
        // GPU: bind LLViewerMediaImpl to this texture
    }
    override fun isViewerMediaTexture(): Boolean = true
    fun initVirtualSize() {
        // GPU: compute initial virtual size from media faces
    }
    fun invalidateMediaImpl() { mediaImpl = null }
    fun addMediaToFace(face: Any) {
        // GPU: attach media to face render state
    }
    fun removeMediaFromFace(face: Any) {
        // GPU: detach media from face render state
    }

    override fun addFace(channel: UInt, face: Any) {
        // GPU: add face to media texture tracking
    }
    override fun removeFace(channel: UInt, face: Any) {
        // GPU: remove face from media texture tracking
    }
    override fun getMaxVirtualSize(): Float {
        // GPU: aggregate virtual sizes from all media faces
        return 0f
    }

    private fun switchTexture(ch: UInt, face: Any) {
        // GPU: swap face's bound texture
    }
    private fun findFaces(): Boolean {
        // GPU: scan face list for references to this media texture
        return false
    }
    private fun stopPlaying() {
        // GPU: tear down media playback state
    }

    companion object {
        private val mediaMap: MutableMap<UUID, ViewerMediaTexture> = mutableMapOf()

        fun findMediaTexture(mediaId: UUID): ViewerMediaTexture? = mediaMap[mediaId]

        fun removeMediaImplFromTexture(mediaId: UUID) {
            mediaMap[mediaId]?.invalidateMediaImpl()
        }

        fun updateClass() {
            // GPU: tick all active media textures
        }
        fun cleanUpClass() { mediaMap.clear() }
    }
}

object ViewerTextureManager {
    var testerp: TexturePipelineTester? = null

    fun createMediaTexture(id: UUID, useMipMaps: Boolean = true, glImage: Any? = null): ViewerMediaTexture =
        ViewerMediaTexture(id, useMipMaps)

    fun findFetchedTextures(id: UUID, output: MutableList<ViewerFetchedTexture>) =
        ViewerTextureList.instance.findTexturesByID(id, output)

    fun findTextures(id: UUID, output: MutableList<ViewerTexture>) {
        val fetched = mutableListOf<ViewerFetchedTexture>()
        ViewerTextureList.instance.findTexturesByID(id, fetched)
        output.addAll(fetched)
        if (output.isEmpty()) findMediaTexture(id)?.let { output.add(it) }
    }

    fun findFetchedTexture(id: UUID, texType: Int): ViewerFetchedTexture? =
        ViewerTextureList.instance.findImage(id, TexListType.values()[texType])

    fun findMediaTexture(id: UUID): ViewerMediaTexture? =
        ViewerMediaTexture.findMediaTexture(id)

    fun getMediaTexture(id: UUID, useMipMaps: Boolean = true, glImage: Any? = null): ViewerMediaTexture {
        val tex = findMediaTexture(id) ?: createMediaTexture(id, useMipMaps, glImage)
        tex.initVirtualSize()
        return tex
    }

    fun staticCastToFetchedTexture(tex: ViewerTexture?, reportError: Boolean = false): ViewerFetchedTexture? {
        if (tex == null) return null
        val type = tex.getType().toInt()
        return if (type == ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal ||
            type == ViewerTexture.TextureType.LOD_TEXTURE.ordinal) {
            tex as ViewerFetchedTexture
        } else {
            if (reportError) error("not a fetched texture type: $type")
            null
        }
    }

    fun getLocalTexture(useMipMaps: Boolean = true, generateGlTex: Boolean = true): ViewerTexture {
        val tex = ViewerTexture(useMipMaps)
        if (generateGlTex) {
            // GPU: generate GL texture name
        }
        return tex
    }

    fun getLocalTexture(id: UUID, useMipMaps: Boolean, generateGlTex: Boolean = true): ViewerTexture {
        val tex = ViewerTexture(useMipMaps).apply { this.id = id }
        if (generateGlTex) {
            // GPU: generate GL texture name
        }
        return tex
    }

    fun getLocalTexture(raw: Any, useMipMaps: Boolean): ViewerTexture = ViewerTexture(useMipMaps)

    fun getLocalTexture(width: UInt, height: UInt, components: UByte, useMipMaps: Boolean, generateGlTex: Boolean = true): ViewerTexture {
        val tex = ViewerTexture(useMipMaps).apply { fullWidth = width.toInt(); fullHeight = height.toInt() }
        if (generateGlTex) {
            // GPU: generate GL texture name
        }
        return tex
    }

    fun getFetchedTexture(
        imageId: UUID,
        fType: FTType = FTType.DEFAULT,
        useMipMap: Boolean = true,
        boostPriority: Int = ViewerTexture.BOOST_NONE,
        textureType: Byte = ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal.toByte(),
        internalFormat: Int = 0,
        primaryFormat: Int = 0,
        requestFromHost: String = ""
    ): ViewerFetchedTexture? =
        ViewerTextureList.instance.getImage(imageId, fType, useMipMap, boostPriority, textureType, internalFormat, primaryFormat, requestFromHost)

    fun getFetchedTextureFromFile(
        filename: String,
        fType: FTType = FTType.LOCAL_FILE,
        useMipMap: Boolean = true,
        boostPriority: Int = ViewerTexture.BOOST_NONE,
        textureType: Byte = ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal.toByte(),
        internalFormat: Int = 0,
        primaryFormat: Int = 0,
        forceId: UUID = NULL_UUID
    ): ViewerFetchedTexture? =
        ViewerTextureList.instance.getImageFromFile(filename, fType, useMipMap, boostPriority, textureType, internalFormat, primaryFormat, forceId)

    fun getFetchedTextureFromUrl(
        url: String,
        fType: FTType,
        useMipMap: Boolean = true,
        boostPriority: Int = ViewerTexture.BOOST_NONE,
        textureType: Byte = ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal.toByte(),
        internalFormat: Int = 0,
        primaryFormat: Int = 0,
        forceId: UUID = NULL_UUID
    ): ViewerFetchedTexture? =
        ViewerTextureList.instance.getImageFromUrl(url, fType, useMipMap, boostPriority, textureType, internalFormat, primaryFormat, forceId)

    fun getFetchedTextureFromHost(imageId: UUID, fType: FTType, host: String): ViewerFetchedTexture? =
        ViewerTextureList.instance.getImageFromHost(imageId, fType, host)

    fun getFetchedTextureFromMemory(data: ByteArray, mimetype: String): ViewerFetchedTexture? =
        ViewerTextureList.instance.getImageFromMemory(data, mimetype)

    fun getRawImageFromMemory(data: ByteArray, mimetype: String): Any? =
        ViewerTextureList.instance.getRawImageFromMemory(data, mimetype)

    fun init() {
        // GPU: create null/black/checkerboard/default textures and texture manager bridge
    }
    fun cleanup() {
        // GPU: release all static texture pointers and GL resources
    }
}

class TexturePipelineTester {
    private var pause: Boolean = false
    private var usingDefaultTexture: Boolean = false
    private var totalBytesUsed: Long = 0L
    private var totalBytesUsedForLargeImage: Long = 0L
    private var lastTotalBytesUsed: Long = 0L
    private var lastTotalBytesUsedForLargeImage: Long = 0L
    private var totalBytesLoaded: Long = 0L
    private var totalBytesLoadedFromCache: Long = 0L
    private var totalBytesLoadedForLargeImage: Long = 0L
    private var totalBytesLoadedForSculpties: Long = 0L
    private var startFetchingTime: Float = 0f
    private var totalGrayTime: Float = 0f
    private var totalStabilizingTime: Float = 0f
    private var startTimeLoadingSculpties: Float = 0f
    private var endTimeLoadingSculpties: Float = 0f
    private var startStabilizingTime: Float = 0f
    private var endStabilizingTime: Float = 0f

    fun update() {
        // APR: aggregate per-frame texture pipeline metrics
    }
    fun updateTextureBindingStats(imagep: ViewerTexture) {
        // APR: record bytes bound for this frame
    }
    fun updateTextureLoadingStats(imagep: ViewerFetchedTexture, rawImagep: Any?, fromCache: Boolean) {
        // APR: record bytes loaded, distinguish cache vs network
    }
    fun updateGrayTextureBinding() { usingDefaultTexture = true }
    fun setStabilizingTime() {
        // APR: record the point at which the texture pipeline stabilized
    }

    private fun reset() {
        usingDefaultTexture = false
        totalBytesUsed = 0L
        totalBytesUsedForLargeImage = 0L
        totalBytesLoaded = 0L
        totalBytesLoadedFromCache = 0L
        totalBytesLoadedForLargeImage = 0L
        totalBytesLoadedForSculpties = 0L
    }

    private fun updateStabilizingTime() {
        // APR: compute how long the texture pipeline was stabilizing
    }
    private fun outputTestRecord() {
        // APR: write metrics to LLSD output session log
    }

    companion object {
        const val MIN_LARGE_IMAGE_AREA = 262144
    }
}
