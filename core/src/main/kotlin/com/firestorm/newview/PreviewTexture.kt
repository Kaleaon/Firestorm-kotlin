package com.firestorm.newview

import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val CLIENT_RECT_VPAD = 4
private const val SECONDS_TO_SHOW_FILE_SAVED_MSG = 8f
private const val PREVIEW_TEXTURE_MAX_ASPECT = 200f
private const val PREVIEW_TEXTURE_MIN_ASPECT = 0.005f

fun checkFileExtension(filename: String, format: PreviewTexture.FileFormatType): String {
    val extension = if (format == PreviewTexture.FileFormatType.FORMAT_TGA) ".tga" else ".png"
    val lower = filename.lowercase()
    return if (lower.endsWith(extension)) filename else filename + extension
}

class PreviewTexture(private val key: Any) {

    enum class FileFormatType { FORMAT_TGA, FORMAT_PNG }

    enum class AssetStatus { PREVIEW_ASSET_UNLOADED, PREVIEW_ASSET_LOADING, PREVIEW_ASSET_LOADED }

    private var imageId: UUID = NULL_UUID
    var image: ViewerFetchedTexture? = null
        private set
    private var imageOldBoostLevel: Int = ViewerTexture.BOOST_NONE
    private var saveFileName: String = ""
    private var savedFileTimer: Float = 0f
    private var savedFileTimerExpiry: Float = 0f
    var loadingFullImage: Boolean = false
        private set
    private var showKeepDiscard: Boolean = false
    private var copyToInv: Boolean = false
    private var previewToSave: Boolean = false
    private var isCopyable: Boolean = false
    private var isFullPerm: Boolean = false
    var updateDimensions: Boolean = true
    private var lastHeight: Int = 0
    private var lastWidth: Int = 0
    var aspectRatio: Float = 0f
        private set
    private var showingButtons: Boolean = false
    private var displayNameCallback: Boolean = false
    private var uploaderDateTime: String = ""
    private val callbackTextureList: MutableSet<TextureKey> = mutableSetOf()
    private val ratiosList: MutableList<String> = mutableListOf()
    private var assetStatus: AssetStatus = AssetStatus.PREVIEW_ASSET_UNLOADED

    init {
        updateImageId()
        if (keyHas("save_as")) previewToSave = true
        if (keyHas("preview_only")) {
            showKeepDiscard = false
            copyToInv = false
            isCopyable = false
            previewToSave = false
            isFullPerm = false
        }
    }

    private fun keyHas(field: String): Boolean {
        System.err.println("PreviewTexture: check if LLSD key contains field not yet implemented")
        return false
    }

    fun destroy() {
        LoadedCallbackEntry.cleanUpCallbackList(callbackTextureList)
        if (loadingFullImage) decWindowBusyCount()
        image?.let {
            it.setBoostLevel(imageOldBoostLevel)
            // no-op
        }
        image = null
    }

    private fun decWindowBusyCount() { System.err.println("PreviewTexture: decrement viewer window busy counter not yet implemented") }
    private fun incWindowBusyCount() { System.err.println("PreviewTexture: increment viewer window busy counter not yet implemented") }

    fun populateRatioList() {
        ratiosList.clear()
        ratiosList.addAll(listOf(
            "Unconstrained", "1:1", "4:3", "10:7", "3:2", "16:10", "16:9", "2:1"
        ))
        System.err.println("PreviewTexture: populate combo_aspect_ratio UI widget not yet implemented")
    }

    fun postBuild(): Boolean {
        populateRatioList()
        System.err.println("PreviewTexture: wire up UI buttons and callbacks from XUI layout not yet implemented")
        return false
    }

    fun draw() {
        updateDimensionsInternal()
        // no-op
    }

    fun canSaveAs(): Boolean =
        isFullPerm && !loadingFullImage && image != null && image?.isMissingAsset() == false

    fun saveAs() { saveAs(emptyList<UUID>()) }

    fun saveAs(remainingIds: List<UUID>) {
        val usePng = getDefaultSaveFormatIsPng()
        saveAs(if (usePng) FileFormatType.FORMAT_PNG else FileFormatType.FORMAT_TGA, remainingIds)
    }

    fun saveAs(format: FileFormatType, remainingIds: List<UUID> = emptyList()) {
        if (loadingFullImage) return
        val callback: LoadedCallbackFunc = when (format) {
            FileFormatType.FORMAT_PNG -> ::onFileLoadedForSavePNG
            FileFormatType.FORMAT_TGA -> ::onFileLoadedForSaveTGA
        }
        val itemName = getItemName() ?: ""
        val filename = checkFileExtension(scrubFileName(itemName), format)
        System.err.println("PreviewTexture: open file picker for format not yet implemented")
    }

    fun saveTextureToFile(filenames: List<String>, format: FileFormatType, callback: LoadedCallbackFunc, remainingIds: List<UUID> = emptyList()) {
        if (previewToSave) {
            previewToSave = false
            System.err.println("PreviewTexture: show preview_texture floater for this item not yet implemented")
        }
        saveFileName = checkFileExtension(filenames[0], format)
        loadingFullImage = true
        incWindowBusyCount()
        image?.forceToSaveRawImage(0)
        image?.setLoadedCallback(callback, 0, keepImageRaw = true, needsAuxArg = false, userData = imageId, srcCallbackList = callbackTextureList)
        saveMultiple(remainingIds)
    }

    fun saveMultipleToFile(fileName: String = "") {
        val textureName = scrubFileName(if (fileName.isEmpty()) (getItemName() ?: "") else fileName)
        val extension = if (getDefaultSaveFormatIsPng()) ".png" else ".tga"
        val saveLocation = getTextureSaveLocation()
        var filepath = ""
        var i = 0
        do {
            filepath = "$saveLocation/${textureName}${if (i != 0) "_${"%03d".format(i)}" else ""}$extension"
            i++
        } while (fileExists(filepath))

        saveFileName = filepath
        loadingFullImage = true
        incWindowBusyCount()
        image?.forceToSaveRawImage(0)
        val callback: LoadedCallbackFunc = if (getDefaultSaveFormatIsPng()) ::onFileLoadedForSavePNG else ::onFileLoadedForSaveTGA
        image?.setLoadedCallback(callback, 0, keepImageRaw = true, needsAuxArg = false, userData = imageId, srcCallbackList = callbackTextureList)
    }

    private fun fileExists(path: String): Boolean {
        System.err.println("PreviewTexture: fileExists not yet implemented")
        return false
    }

    private fun getTextureSaveLocation(): String {
        System.err.println("PreviewTexture: getTextureSaveLocation not yet implemented")
        return ""
    }

    private fun getDefaultSaveFormatIsPng(): Boolean {
        System.err.println("PreviewTexture: getDefaultSaveFormatIsPng not yet implemented")
        return false
    }

    private fun getItemName(): String? {
        System.err.println("PreviewTexture: getItemName not yet implemented")
        return null
    }

    private fun scrubFileName(name: String): String {
        System.err.println("PreviewTexture: scrubFileName not yet implemented")
        return ""
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        val horizPad = 0
        // no-op
    }

    fun onFocusReceived() { System.err.println("PreviewTexture: onFocusReceived not yet implemented") }

    fun openToSave() { previewToSave = true }

    fun loadAsset() { System.err.println("PreviewTexture: loadAsset not yet implemented") }

    fun getAssetStatus(): AssetStatus = assetStatus

    fun setObjectId(objectId: UUID) {
        System.err.println("PreviewTexture: setObjectId not yet implemented")
    }

    private fun updateImageId() {
        System.err.println("PreviewTexture: updateImageId not yet implemented")
    }

    private fun updateDimensionsInternal() {
        val img = image ?: return
        if (img.fullWidth == 0 || img.fullHeight == 0) return

        val imgWidth = img.fullWidth
        val imgHeight = img.fullHeight

        if (assetStatus != AssetStatus.PREVIEW_ASSET_LOADED || lastWidth != imgWidth || lastHeight != imgHeight) {
            assetStatus = AssetStatus.PREVIEW_ASSET_LOADED
            adjustAspectRatio()
        }

        if (imgWidth != lastWidth) updateDimensionTextWidth(imgWidth)
        if (imgHeight != lastHeight) updateDimensionTextHeight(imgHeight)
        lastWidth = imgWidth
        lastHeight = imgHeight

        if (updateDimensions) {
            updateDimensions = false
            System.err.println("PreviewTexture: resize floater to match image dimensions not yet implemented")
        }
    }

    private fun updateDimensionTextWidth(width: Int) { System.err.println("PreviewTexture: updateDimensionTextWidth not yet implemented") }
    private fun updateDimensionTextHeight(height: Int) { System.err.println("PreviewTexture: updateDimensionTextHeight not yet implemented") }

    private fun adjustAspectRatio() {
        val img = image ?: return
        if (img.fullWidth > 0 && img.fullHeight > 0) {
            setAspectRatio(img.fullWidth.toFloat(), img.fullHeight.toFloat())
        }
    }

    private fun setAspectRatio(width: Float, height: Float): Boolean {
        if (height <= 0f) return false
        val ratio = width / height
        if (ratio < PREVIEW_TEXTURE_MIN_ASPECT || ratio > PREVIEW_TEXTURE_MAX_ASPECT) return false
        aspectRatio = ratio
        return true
    }

    fun callbackLoadName(agentId: UUID, avName: Any) {
        System.err.println("PreviewTexture: callbackLoadName not yet implemented")
    }

    fun onButtonClickProfile() { System.err.println("PreviewTexture: onButtonClickProfile not yet implemented") }
    fun onButtonClickUUID() { System.err.println("PreviewTexture: onButtonClickUUID not yet implemented") }
    fun onButtonRefresh() {
        image?.forceToRefetchTexture()
        System.err.println("PreviewTexture: onButtonRefresh not yet implemented")
    }

    private fun onFileLoadedForSaveTGA(
        success: Boolean,
        srcVi: ViewerFetchedTexture?,
        src: Any?,
        auxSrc: Any?,
        discardLevel: Int,
        final: Boolean,
        userData: Any?
    ) {
        if (final || !success) {
            decWindowBusyCount()
            loadingFullImage = false
        }
        if (final && success && src != null) {
            System.err.println("PreviewTexture: encode src as TGA and write to saveFileName not yet implemented")
            saveFileName = ""
            resetSavedFileTimer()
        }
        if (!success) { System.err.println("PreviewTexture: show CannotDownloadFile notification not yet implemented") }
    }

    private fun onFileLoadedForSavePNG(
        success: Boolean,
        srcVi: ViewerFetchedTexture?,
        src: Any?,
        auxSrc: Any?,
        discardLevel: Int,
        final: Boolean,
        userData: Any?
    ) {
        if (final || !success) {
            decWindowBusyCount()
            loadingFullImage = false
        }
        if (final && success && src != null) {
            System.err.println("PreviewTexture: encode src as PNG and write to saveFileName not yet implemented")
            saveFileName = ""
            resetSavedFileTimer()
        }
        if (!success) { System.err.println("PreviewTexture: show CannotDownloadFile notification not yet implemented") }
    }

    private fun resetSavedFileTimer() {
        savedFileTimer = ViewerTexture.currentTime
        savedFileTimerExpiry = savedFileTimer + SECONDS_TO_SHOW_FILE_SAVED_MSG
    }

    private fun savedFileTimerHasExpired(): Boolean =
        ViewerTexture.currentTime >= savedFileTimerExpiry

    companion object {
        fun onSaveAsBtn(ctrl: Any, data: Any?) {
            val self = data as? PreviewTexture ?: return
            val value = ""
            when (value) {
                "format_png" -> self.saveAs(FileFormatType.FORMAT_PNG)
                "format_tga" -> self.saveAs(FileFormatType.FORMAT_TGA)
                else -> self.saveAs()
            }
        }

        fun onAspectRatioCommit(ctrl: Any, userData: Any?) {
            val self = userData as? PreviewTexture ?: return
            System.err.println("PreviewTexture: onAspectRatioCommit not yet implemented")
        }

        fun saveMultiple(ids: List<UUID>) {
            if (ids.isEmpty()) return
            System.err.println("PreviewTexture: saveMultiple not yet implemented")
        }

        fun onTextureLoaded(
            success: Boolean,
            srcVi: ViewerFetchedTexture?,
            src: Any?,
            auxSrc: Any?,
            discardLevel: Int,
            final: Boolean,
            userData: Any?
        ) {
            System.err.println("PreviewTexture: onTextureLoaded not yet implemented")
        }
    }
}
