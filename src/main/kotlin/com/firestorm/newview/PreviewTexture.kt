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
        TODO("APR: check if LLSD key contains field $field")
    }

    fun destroy() {
        LoadedCallbackEntry.cleanUpCallbackList(callbackTextureList)
        if (loadingFullImage) decWindowBusyCount()
        image?.let {
            it.setBoostLevel(imageOldBoostLevel)
            TODO("GPU: call forceActive() to clear NO_DELETE texture state")
        }
        image = null
    }

    private fun decWindowBusyCount() { TODO("APR: decrement viewer window busy counter") }
    private fun incWindowBusyCount() { TODO("APR: increment viewer window busy counter") }

    fun populateRatioList() {
        ratiosList.clear()
        ratiosList.addAll(listOf(
            "Unconstrained", "1:1", "4:3", "10:7", "3:2", "16:10", "16:9", "2:1"
        ))
        TODO("APR: populate combo_aspect_ratio UI widget with ratiosList")
    }

    fun postBuild(): Boolean {
        populateRatioList()
        TODO("APR: wire up UI buttons and callbacks from XUI layout")
    }

    fun draw() {
        updateDimensionsInternal()
        TODO("GPU: draw texture preview with checkerboard background and progress bar")
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
        TODO("APR: open file picker for format $format, then call saveTextureToFile")
    }

    fun saveTextureToFile(filenames: List<String>, format: FileFormatType, callback: LoadedCallbackFunc, remainingIds: List<UUID> = emptyList()) {
        if (previewToSave) {
            previewToSave = false
            TODO("APR: show preview_texture floater for this item")
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
        TODO("APR: use java.io.File(path).exists()")
    }

    private fun getTextureSaveLocation(): String {
        TODO("APR: read TextureSaveLocation from saved settings")
    }

    private fun getDefaultSaveFormatIsPng(): Boolean {
        TODO("APR: read FSTextureDefaultSaveAsFormat from saved settings")
    }

    private fun getItemName(): String? {
        TODO("APR: retrieve inventory item name for imageId")
    }

    private fun scrubFileName(name: String): String {
        TODO("APR: remove filesystem-unsafe characters from name")
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        val horizPad = TODO("APR: compute horizontal padding from border widths") as Int
        TODO("GPU: recompute mClientRect for aspect ratio and resize floater")
    }

    fun onFocusReceived() { TODO("APR: forward focus to LLPreview base class") }

    fun openToSave() { previewToSave = true }

    fun loadAsset() { TODO("APR: request texture fetch for imageId with preview boost level") }

    fun getAssetStatus(): AssetStatus = assetStatus

    fun setObjectId(objectId: UUID) {
        TODO("APR: update object context for permissions check")
    }

    private fun updateImageId() {
        TODO("APR: extract UUID from LLSD key and assign to imageId, then find/create image")
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
            TODO("APR: resize floater to match image dimensions and fit within viewer window")
        }
    }

    private fun updateDimensionTextWidth(width: Int) { TODO("APR: set [WIDTH] text arg to $width") }
    private fun updateDimensionTextHeight(height: Int) { TODO("APR: set [HEIGHT] text arg to $height") }

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
        TODO("APR: update uploader name field in UI from avatar name cache callback")
    }

    fun onButtonClickProfile() { TODO("APR: open profile for uploader UUID from image comment") }
    fun onButtonClickUUID() { TODO("APR: copy image UUID to clipboard") }
    fun onButtonRefresh() {
        image?.forceToRefetchTexture()
        TODO("APR: reset preview state to trigger re-download")
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
            TODO("APR: encode src as TGA and write to saveFileName using java.io")
            saveFileName = ""
            resetSavedFileTimer()
        }
        if (!success) { TODO("APR: show CannotDownloadFile notification") }
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
            TODO("APR: encode src as PNG and write to saveFileName using javax.imageio")
            saveFileName = ""
            resetSavedFileTimer()
        }
        if (!success) { TODO("APR: show CannotDownloadFile notification") }
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
            val value = TODO("APR: read string value from ctrl") as String
            when (value) {
                "format_png" -> self.saveAs(FileFormatType.FORMAT_PNG)
                "format_tga" -> self.saveAs(FileFormatType.FORMAT_TGA)
                else -> self.saveAs()
            }
        }

        fun onAspectRatioCommit(ctrl: Any, userData: Any?) {
            val self = userData as? PreviewTexture ?: return
            TODO("APR: read aspect ratio choice from combo box and update self.aspectRatio")
        }

        fun saveMultiple(ids: List<UUID>) {
            if (ids.isEmpty()) return
            TODO("APR: open file picker for each remaining UUID and trigger saveMultipleToFile")
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
            TODO("APR: decode texture comment metadata (uploader UUID and upload timestamp)")
        }
    }
}
