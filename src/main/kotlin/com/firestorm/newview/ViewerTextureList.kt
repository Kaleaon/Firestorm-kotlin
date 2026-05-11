package com.firestorm.newview

import java.util.UUID
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.ArrayDeque

const val IMAGE_REZ_LOSSLESS_CUTOFF: UInt = 128u

typealias ImageCallback = LoadedCallbackFunc

fun getElementType(priority: Int): TexListType =
    if (priority == ViewerTexture.BOOST_ICON || priority == ViewerTexture.BOOST_THUMBNAIL)
        TexListType.SCALE
    else
        TexListType.STANDARD

class ViewerTextureList private constructor() {

    val createTextureList: ArrayDeque<ViewerFetchedTexture> = ArrayDeque()
    val downScaleQueue: ArrayDeque<ViewerFetchedTexture> = ArrayDeque()
    val callbackList: MutableSet<ViewerFetchedTexture> = LinkedHashSet()
    val fastCacheList: MutableSet<ViewerFetchedTexture> = LinkedHashSet()

    var forceResetTextureStats: Boolean = false

    private val uuidMap: MutableMap<TextureKey, ViewerFetchedTexture> = mutableMapOf()
    private val imageList: MutableSet<ViewerFetchedTexture> = LinkedHashSet()
    private val imagePreloads: MutableSet<ViewerFetchedTexture> = LinkedHashSet()
    private var initialized: Boolean = false
    private var lastUpdateKey: TextureKey = TextureKey()

    fun isInitialized(): Boolean = initialized

    fun init() {
        initialized = true
        sNumImages = 0
        doPreloadImages()
    }

    fun shutdown() {
        imagePreloads.clear()
        callbackList.clear()
        while (createTextureList.isNotEmpty()) {
            createTextureList.poll()?.createPending = false
        }
        fastCacheList.clear()
        uuidMap.clear()
        imageList.clear()
        initialized = false
    }

    fun dump() {
        for (image in imageList) {
            println("priority=${image.getMaxVirtualSize()} boost=${image.boostLevel} " +
                "size=${image.fullWidth}x${image.fullHeight} discard=${image.rawDiscardLevel} " +
                "desired=${image.getDesiredDiscardLevel()}")
        }
    }

    fun destroyGL() { TODO("GPU: destroy all GL texture objects") }

    fun findTexturesByID(imageId: UUID, output: MutableList<ViewerFetchedTexture>) {
        for (type in TexListType.values()) {
            uuidMap[TextureKey(imageId, type)]?.let { output.add(it) }
        }
    }

    fun findImage(imageId: UUID, texType: TexListType): ViewerFetchedTexture? =
        uuidMap[TextureKey(imageId, texType)]

    fun findImage(searchKey: TextureKey): ViewerFetchedTexture? = uuidMap[searchKey]

    fun updateImages(maxTime: Float) {
        TODO("GPU: update fetch priorities, create pending textures, purge unreferenced")
    }

    fun forceImmediateUpdate(imagep: ViewerFetchedTexture) {
        removeImageFromList(imagep)
        addImageToList(imagep)
    }

    fun decodeAllImages(maxDecodeTime: Float) {
        TODO("GPU: decode all pending images up to time budget")
    }

    fun handleIRCallback(data: Array<Any?>, number: Int) {
        TODO("APR: handle image-received UDP callback")
    }

    fun getNumImages(): Int = imageList.size

    fun doPreloadImages() {
        TODO("APR: preload UI and default textures from local files")
    }

    fun doPrefetchImages() {
        TODO("APR: prefetch textures logged at last logout and standard world textures")
    }

    fun clearFetchingRequests() {
        TODO("GPU: cancel all outstanding texture fetch requests")
    }

    fun updateImageDecodePriority(imagep: ViewerFetchedTexture, flushImages: Boolean = true) {
        TODO("GPU: recompute decode priority and clean up unreferenced textures")
    }

    fun getImage(
        imageId: UUID,
        fType: FTType = FTType.DEFAULT,
        useMipMaps: Boolean = true,
        boostPriority: Int = ViewerTexture.BOOST_NONE,
        textureType: Byte = ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal.toByte(),
        internalFormat: Int = 0,
        primaryFormat: Int = 0,
        requestFromHost: String = ""
    ): ViewerFetchedTexture? {
        if (!initialized) return null
        if (imageId == NULL_UUID) {
            return ViewerTextureManager.getFetchedTexture(
                imageId, FTType.DEFAULT, true, ViewerTexture.BOOST_UI
            )
        }

        val elemType = getElementType(boostPriority)
        var imagep = findImage(imageId, elemType)
        if (imagep == null) {
            imagep = createImage(imageId, fType, useMipMaps, boostPriority, textureType, internalFormat, primaryFormat, requestFromHost)
        }
        return imagep
    }

    fun getImageFromFile(
        filename: String,
        fType: FTType = FTType.LOCAL_FILE,
        useMipMaps: Boolean = true,
        boostPriority: Int = ViewerTexture.BOOST_NONE,
        textureType: Byte = ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal.toByte(),
        internalFormat: Int = 0,
        primaryFormat: Int = 0,
        forceId: UUID = NULL_UUID
    ): ViewerFetchedTexture? {
        if (!initialized) return null
        val url = "file://$filename"
        return getImageFromUrl(url, fType, useMipMaps, boostPriority, textureType, internalFormat, primaryFormat, forceId)
    }

    fun getImageFromUrl(
        url: String,
        fType: FTType,
        useMipMaps: Boolean = true,
        boostPriority: Int = ViewerTexture.BOOST_NONE,
        textureType: Byte = ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal.toByte(),
        internalFormat: Int = 0,
        primaryFormat: Int = 0,
        forceId: UUID = NULL_UUID
    ): ViewerFetchedTexture? {
        if (!initialized) return null
        val newId = if (forceId != NULL_UUID) forceId else UUID.nameUUIDFromBytes(url.toByteArray())
        var imagep = findImage(newId, getElementType(boostPriority))
        if (imagep == null) {
            imagep = when (textureType.toInt()) {
                ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal ->
                    ViewerFetchedTexture(fType, useMipMaps).apply { this.id = newId; this.url = url }
                ViewerTexture.TextureType.LOD_TEXTURE.ordinal ->
                    ViewerLODTexture(fType, useMipMaps).apply { this.id = newId; this.url = url }
                else -> error("invalid texture type: $textureType")
            }
            if (internalFormat != 0 && primaryFormat != 0) {
                TODO("GPU: set explicit GL internal/primary format")
            }
            addImage(imagep, getElementType(boostPriority))
            if (boostPriority != 0) imagep.setBoostLevel(boostPriority)
        }
        TODO("GPU: mark GL texture as created")
        @Suppress("UNREACHABLE_CODE")
        return imagep
    }

    fun getRawImageFromMemory(data: ByteArray, mimetype: String): Any? {
        TODO("APR: decode raw image bytes for mimetype $mimetype using JVM image library")
    }

    fun getImageFromMemory(data: ByteArray, mimetype: String): ViewerFetchedTexture? {
        val raw = getRawImageFromMemory(data, mimetype) ?: return null
        val imagep = ViewerFetchedTexture(FTType.LOCAL_FILE, true)
        addImage(imagep, TexListType.STANDARD)
        imagep.setBoostLevel(ViewerTexture.BOOST_PREVIEW)
        return imagep
    }

    fun getImageFromHost(imageId: UUID, fType: FTType, host: String): ViewerFetchedTexture? =
        getImage(imageId, fType, true, ViewerTexture.BOOST_NONE,
            ViewerTexture.TextureType.LOD_TEXTURE.ordinal.toByte(), 0, 0, host)

    private fun createImage(
        imageId: UUID,
        fType: FTType,
        useMipMaps: Boolean = true,
        boostPriority: Int = ViewerTexture.BOOST_NONE,
        textureType: Byte = ViewerTexture.TextureType.FETCHED_TEXTURE.ordinal.toByte(),
        internalFormat: Int = 0,
        primaryFormat: Int = 0,
        requestFromHost: String = ""
    ): ViewerFetchedTexture {
        val imagep: ViewerFetchedTexture = when (textureType.toInt()) {
            ViewerTexture.TextureType.LOD_TEXTURE.ordinal ->
                ViewerLODTexture(fType, useMipMaps).apply { id = imageId; targetHost = requestFromHost }
            else ->
                ViewerFetchedTexture(fType, useMipMaps).apply { id = imageId; targetHost = requestFromHost }
        }
        if (internalFormat != 0 && primaryFormat != 0) {
            TODO("GPU: set explicit GL texture format")
        }
        addImage(imagep, getElementType(boostPriority))
        if (boostPriority != 0) imagep.setBoostLevel(boostPriority)
        return imagep
    }

    private fun addImage(image: ViewerFetchedTexture, texType: TexListType) {
        val key = TextureKey(image.id, texType)
        image.textureListType = texType.ordinal
        uuidMap[key] = image
        addImageToList(image)
        sNumImages++
    }

    private fun deleteImage(image: ViewerFetchedTexture) {
        val key = TextureKey(image.id, TexListType.values()[image.textureListType])
        uuidMap.remove(key)
        removeImageFromList(image)
        sNumImages--
    }

    private fun addImageToList(image: ViewerFetchedTexture) {
        image.setInImageList(true)
        imageList.add(image)
    }

    private fun removeImageFromList(image: ViewerFetchedTexture) {
        image.setInImageList(false)
        imageList.remove(image)
    }

    private fun updateImagesCreateTextures(maxTime: Float): Float {
        TODO("GPU: upload pending textures to GL within time budget, return elapsed time")
    }

    private fun updateImagesFetchTextures(maxTime: Float): Float {
        TODO("GPU: process fetch state machines within time budget, return elapsed time")
    }

    private fun updateImagesUpdateStats() {
        TODO("GPU: update per-image stats and remove stale references")
    }

    private fun updateImagesLoadingFastCache(maxTime: Float): Float {
        TODO("APR: load fast-cache thumbnails within time budget, return elapsed time")
    }

    operator fun iterator(): Iterator<ViewerFetchedTexture> = imageList.iterator()

    companion object {
        val instance: ViewerTextureList = ViewerTextureList()

        var sNumImages: Int = 0
        var sNumFastCacheReads: UInt = 0u

        fun createUploadFile(
            rawImage: Any,
            outFilename: String,
            maxImageDimensions: Int = ViewerFetchedTexture.MAX_IMAGE_SIZE_DEFAULT,
            minImageDimensions: Int = 0
        ): Boolean { TODO("APR: encode raw image as J2C and write to outFilename") }

        fun createUploadFile(
            filename: String,
            outFilename: String,
            codec: UByte,
            maxImageDimensions: Int = ViewerFetchedTexture.MAX_IMAGE_SIZE_DEFAULT,
            minImageDimensions: Int = 0,
            forceSquare: Boolean = false
        ): Boolean { TODO("APR: transcode image file to J2C upload format") }

        fun convertToUploadFile(
            rawImage: Any,
            maxImageDimensions: Int = ViewerFetchedTexture.MAX_IMAGE_SIZE_DEFAULT,
            forceSquare: Boolean = false,
            forceLossless: Boolean = false
        ): Any? { TODO("APR: convert raw image to J2C for upload") }

        fun processImageNotInDatabase(msg: Any, userData: Any?) {
            TODO("APR: handle ImageNotInDatabase UDP message")
        }

        fun receiveImageHeader(msg: Any, userData: Any?) {
            TODO("APR: handle incoming image header UDP packet (OpenSim compatibility)")
        }

        fun receiveImagePacket(msg: Any, userData: Any?) {
            TODO("APR: handle incoming image data UDP packet (OpenSim compatibility)")
        }
    }
}

object UIImageList {
    private val uiImages: MutableMap<String, Any> = mutableMapOf()
    private val uiTextureList: MutableList<ViewerFetchedTexture> = mutableListOf()

    fun getUIImageByID(id: UUID, priority: Int): Any? { TODO("GPU: retrieve UI image by asset UUID") }
    fun getUIImage(name: String, priority: Int): Any? = uiImages[name]
    fun cleanUp() { uiImages.clear(); uiTextureList.clear() }
    fun initFromFile(): Boolean { TODO("APR: load UI image definitions from XML skin file") }

    fun preloadUIImage(name: String, filename: String, useMips: Boolean, scaleRect: Any, clipRect: Any, scaleStyle: Int): Any? {
        TODO("GPU: preload named UI image from skin file")
    }

    fun onUIImageLoaded(
        success: Boolean,
        srcVi: ViewerFetchedTexture?,
        src: Any?,
        srcAux: Any?,
        discardLevel: Int,
        final: Boolean,
        userData: Any?
    ) { TODO("GPU: finalize UI image after texture load callback") }
}
