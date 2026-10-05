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

    fun destroyGL() {
        // no-op
    }

    fun findTexturesByID(imageId: UUID, output: MutableList<ViewerFetchedTexture>) {
        for (type in TexListType.values()) {
            uuidMap[TextureKey(imageId, type)]?.let { output.add(it) }
        }
    }

    fun findImage(imageId: UUID, texType: TexListType): ViewerFetchedTexture? =
        uuidMap[TextureKey(imageId, texType)]

    fun findImage(searchKey: TextureKey): ViewerFetchedTexture? = uuidMap[searchKey]

    fun updateImages(maxTime: Float) {
        // no-op
    }

    fun forceImmediateUpdate(imagep: ViewerFetchedTexture) {
        removeImageFromList(imagep)
        addImageToList(imagep)
    }

    fun decodeAllImages(maxDecodeTime: Float) {
        // no-op
    }

    fun handleIRCallback(data: Array<Any?>, number: Int) {
        System.err.println("ViewerTextureList: handleIRCallback not yet implemented")
    }

    fun getNumImages(): Int = imageList.size

    fun doPreloadImages() {
        System.err.println("ViewerTextureList: doPreloadImages not yet implemented")
    }

    fun doPrefetchImages() {
        System.err.println("ViewerTextureList: doPrefetchImages not yet implemented")
    }

    fun clearFetchingRequests() {
        System.err.println("ViewerTextureList: clearFetchingRequests not yet implemented")
    }

    fun updateImageDecodePriority(imagep: ViewerFetchedTexture, flushImages: Boolean = true) {
        System.err.println("ViewerTextureList: updateImageDecodePriority not yet implemented")
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
                // no-op
            }
            addImage(imagep, getElementType(boostPriority))
            if (boostPriority != 0) imagep.setBoostLevel(boostPriority)
        }
        // no-op (GPU: mark GL texture as created)
        return imagep
    }

    fun getRawImageFromMemory(data: ByteArray, mimetype: String): Any? {
        System.err.println("ViewerTextureList: getRawImageFromMemory not yet implemented")
        return null
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
            // no-op
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
        System.err.println("ViewerTextureList: updateImagesCreateTextures not yet implemented")
        return 0f
    }

    private fun updateImagesFetchTextures(maxTime: Float): Float {
        System.err.println("ViewerTextureList: updateImagesFetchTextures not yet implemented")
        return 0f
    }

    private fun updateImagesUpdateStats() {
        System.err.println("ViewerTextureList: updateImagesUpdateStats not yet implemented")
    }

    private fun updateImagesLoadingFastCache(maxTime: Float): Float {
        System.err.println("ViewerTextureList: updateImagesLoadingFastCache not yet implemented")
        return 0f
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
        ): Boolean {
            System.err.println("ViewerTextureList: createUploadFile(rawImage) not yet implemented")
            return false
        }

        fun createUploadFile(
            filename: String,
            outFilename: String,
            codec: UByte,
            maxImageDimensions: Int = ViewerFetchedTexture.MAX_IMAGE_SIZE_DEFAULT,
            minImageDimensions: Int = 0,
            forceSquare: Boolean = false
        ): Boolean {
            System.err.println("ViewerTextureList: createUploadFile(filename) not yet implemented")
            return false
        }

        fun convertToUploadFile(
            rawImage: Any,
            maxImageDimensions: Int = ViewerFetchedTexture.MAX_IMAGE_SIZE_DEFAULT,
            forceSquare: Boolean = false,
            forceLossless: Boolean = false
        ): Any? {
            System.err.println("ViewerTextureList: convertToUploadFile not yet implemented")
            return null
        }

        fun processImageNotInDatabase(msg: Any, userData: Any?) {
            System.err.println("ViewerTextureList: processImageNotInDatabase not yet implemented")
        }

        fun receiveImageHeader(msg: Any, userData: Any?) {
            System.err.println("ViewerTextureList: receiveImageHeader not yet implemented")
        }

        fun receiveImagePacket(msg: Any, userData: Any?) {
            System.err.println("ViewerTextureList: receiveImagePacket not yet implemented")
        }
    }
}

object UIImageList {
    private val uiImages: MutableMap<String, Any> = mutableMapOf()
    private val uiTextureList: MutableList<ViewerFetchedTexture> = mutableListOf()

    fun getUIImageByID(id: UUID, priority: Int): Any? {
        System.err.println("UIImageList: getUIImageByID not yet implemented")
        return null
    }
    fun getUIImage(name: String, priority: Int): Any? = uiImages[name]
    fun cleanUp() { uiImages.clear(); uiTextureList.clear() }
    fun initFromFile(): Boolean {
        System.err.println("UIImageList: initFromFile not yet implemented")
        return false
    }

    fun preloadUIImage(name: String, filename: String, useMips: Boolean, scaleRect: Any, clipRect: Any, scaleStyle: Int): Any? {
        System.err.println("UIImageList: preloadUIImage not yet implemented")
        return null
    }

    fun onUIImageLoaded(
        success: Boolean,
        srcVi: ViewerFetchedTexture?,
        src: Any?,
        srcAux: Any?,
        discardLevel: Int,
        final: Boolean,
        userData: Any?
    ) {
        System.err.println("UIImageList: onUIImageLoaded not yet implemented")
    }
}
