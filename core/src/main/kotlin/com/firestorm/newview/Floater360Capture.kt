package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.RadioGroup
import com.firestorm.llui.UICtrl

class Floater360Capture private constructor(key: Any) :
    Floater(key),
    PluginClassMediaOwner
{
    private var webBrowser: MediaCtrl? = null

    private val defaultHtml: String = "default.html"
    private val eqrGenHtml: String  = "eqr_gen.html"

    private var captureBtn: UICtrl? = null
    private var saveLocalBtn: UICtrl? = null
    private var qualityRadioGroup: RadioGroup? = null

    private var sourceImageSize: Int = 0
    private var initialHeadingDeg: Float = 0f
    private var outputImageWidth: Int = 0
    private var outputImageHeight: Int = 0
    private var imageSaveDir: String = ""

    private val rawImages: Array<Any?> = arrayOfNulls(6)

    private val startILMode: String

    init {
        startILMode = Agent.instance.getInterestListMode()
        Agent.instance.set360CaptureActive(true)
        Agent.instance.changeInterestListMode(IL_MODE_360)
    }

    fun destroy() {
        webBrowser?.navigateStop()
        webBrowser?.clearCache()
        webBrowser?.unloadMediaSource()
        webBrowser = null

        if (!App.isExiting()) {
            Agent.instance.set360CaptureActive(false)
            Agent.instance.changeInterestListMode(IL_MODE_DEFAULT)
        }
    }

    override fun postBuild(): Boolean {
        captureBtn = getChild("capture_button")
        captureBtn?.setCommitCallback { onCapture360ImagesBtn() }

        saveLocalBtn = getChild("save_local_button")
        saveLocalBtn?.setCommitCallback { onSaveLocalBtn() }
        saveLocalBtn?.setEnabled(false)

        webBrowser = getChild("360capture_contents")
        webBrowser?.addObserver(this)
        webBrowser?.allowFileDownload = true

        qualityRadioGroup = getChild("360_quality_selection")
        qualityRadioGroup?.setCommitCallback { onChooseQualityRadioGroup() }

        return true
    }

    override fun onOpen(key: Any) {
        val url = "file:///" + getHTMLBaseFolder() + defaultHtml
        webBrowser?.navigateTo(url)

        setSourceImageSize()

        outputImageWidth  = SavedSettings.getUInt("360CaptureOutputImageWidth").toInt()
        outputImageHeight = outputImageWidth / 2

        System.err.println("Floater360Capture: enableResizeCtrls not yet implemented")

        initialHeadingDeg = 0f

        System.err.println("Floater360Capture: imageSaveDir not yet implemented")
        imageSaveDir = ""

        onCapture360ImagesBtn()
    }

    private fun onChooseQualityRadioGroup() {
        setSourceImageSize()
    }

    private fun setSourceImageSize() {
        sourceImageSize = qualityRadioGroup?.getSelectedValue()?.asInteger() ?: 0

        if (!Pipeline.sRenderDeferred) {
            val windowRect = ViewerWindow.instance.getWindowRectRaw()
            val windowWidth  = windowRect.width
            val windowHeight = windowRect.height

            if (sourceImageSize > windowWidth || sourceImageSize > windowHeight) {
                sourceImageSize = minOf(windowWidth, windowHeight, sourceImageSize)
            }

            val savedIndex = qualityRadioGroup?.getSelectedIndex() ?: 0
            qualityRadioGroup?.setSelectedIndex(0)
            val minSize = qualityRadioGroup?.getSelectedValue()?.asInteger() ?: 0
            qualityRadioGroup?.setSelectedIndex(savedIndex)

            if (sourceImageSize < minSize) {
                System.err.println("Floater360Capture: small snapshot size warning not yet implemented")
            }
        }
    }

    private fun getSelectedQualityTooltip(): String {
        val group = qualityRadioGroup ?: return ""
        val selectedIndex = group.getSelectedIndex()
        val children = group.getChildList()?.reversed() ?: return ""
        for ((revIdx, child) in children.withIndex()) {
            if (selectedIndex == children.size - 1 - revIdx) {
                return child.getToolTip()
            }
        }
        return ""
    }

    private fun getHTMLBaseFolder(): String {
        System.err.println("Floater360Capture: getHTMLBaseFolder not yet implemented")
        return ""
    }

    private fun onCapture360ImagesBtn() {
        capture360Images()
    }

    private fun makeFullPathToJS(filename: String): String {
        System.err.println("Floater360Capture: makeFullPathToJS not yet implemented")
        return ""
    }

    private fun writeDataUrlHeader(filename: String) {
        System.err.println("Floater360Capture: writeDataUrlHeader not yet implemented")
    }

    private fun writeDataUrlFooter(filename: String) {
        System.err.println("Floater360Capture: writeDataUrlFooter not yet implemented")
    }

    private fun writeDataUrl(filename: String, prefix: String, data: ByteArray): Boolean {
        System.err.println("Floater360Capture: writeDataUrl not yet implemented")
        return false
    }

    private fun encodeAndSave(rawImage: Any, filename: String, prefix: String) {
        val quality = SavedSettings.getUInt("360CaptureJPEGEncodeQuality").toInt()
        // no-op
    }

    private fun mockSnapshot(raw: Any) {
        // no-op
    }

    private fun suspendForAFrame() {
        System.err.println("Floater360Capture: suspendForAFrame not yet implemented")
    }

    private fun capture360Images() {
        setSourceImageSize()

        captureBtn?.setEnabled(false)
        saveLocalBtn?.setEnabled(false)

        val renderAttachedLights = Pipeline.sRenderAttachedLights
        if (SavedSettings.getBool("360CaptureHideAvatars")) {
            // no-op
        }

        val lookDirs = arrayOf(
            floatArrayOf(1f,0f,0f), floatArrayOf(0f,1f,0f), floatArrayOf(0f,0f,1f),
            floatArrayOf(-1f,0f,0f), floatArrayOf(0f,-1f,0f), floatArrayOf(0f,0f,-1f)
        )
        val lookUpvecs = arrayOf(
            floatArrayOf(0f,0f,1f), floatArrayOf(0f,0f,1f), floatArrayOf(0f,-1f,0f),
            floatArrayOf(0f,0f,1f), floatArrayOf(0f,0f,1f), floatArrayOf(0f,1f,0f)
        )

        val oldOcclusion = run { System.err.println("Floater360Capture: LLPipeline.sUseOcclusion not yet implemented"); 0 }
        val camera = run { System.err.println("Floater360Capture: LLViewerCamera.getInstance() not yet implemented"); Any() }
        val oldFov    = run { System.err.println("Floater360Capture: camera.getView() not yet implemented"); 0f }
        val oldAspect = run { System.err.println("Floater360Capture: camera.getAspect() not yet implemented"); 0f }
        val oldYaw    = run { System.err.println("Floater360Capture: camera.getYaw() not yet implemented"); 0f }

        freezeWorld(true)

        initialHeadingDeg = run { System.err.println("Floater360Capture: initialHeadingDeg from camera.getYaw() not yet implemented"); 0f }

        System.err.println("Floater360Capture: camera.setAspect/setView/yaw not yet implemented")

        val cubemapJsFilename = "cubemap_img.js"
        val cubemapJsFullPath = makeFullPathToJS(cubemapJsFilename)
        writeDataUrlHeader(cubemapJsFullPath)

        val prefixes = arrayOf("posx", "posz", "posy", "negx", "negz", "negy")
        val numRenderPasses = SavedSettings.getUInt("360CaptureNumRenderPasses").toInt()

        for (i in 0 until 6) {
            System.err.println("Floater360Capture: pauseMainloopTimeout / stop recording not yet implemented")
            System.err.println("Floater360Capture: rawImages[$i] allocation not yet implemented")
            System.err.println("Floater360Capture: camera.lookDir not yet implemented")
            System.err.println("Floater360Capture: simpleSnapshot not yet implemented")
            // encodeAndSave skipped: rawImages[i] is null until snapshot is implemented
            System.err.println("Floater360Capture: resume recording / resumeMainloopTimeout not yet implemented")
        }

        writeDataUrlFooter(cubemapJsFullPath)
        freezeWorld(false)

        System.err.println("Floater360Capture: camera restore / sUseOcclusion restore not yet implemented")

        if (SavedSettings.getBool("360CaptureHideAvatars")) {
            // no-op
        }

        val url = "file:///" + getHTMLBaseFolder() + eqrGenHtml
        webBrowser?.navigateTo(url)

        captureBtn?.setEnabled(true)
        saveLocalBtn?.setEnabled(true)
    }

    override fun handleMediaEvent(plugin: PluginClassMedia, event: MediaEvent) {
        when (event) {
            MediaEvent.NAVIGATE_COMPLETE -> {
                val navigateUrl = plugin.navigateUri
                if (navigateUrl.contains(eqrGenHtml)) {
                    val saveDir = imageSaveDir.replace('\\', '/')
                    val cameraFov   = SavedSettings.getUInt("360CaptureCameraFOV").toInt()
                    val overlayLabel = "'${getSelectedQualityTooltip()}'"
                    val cameraYaw = run { System.err.println("Floater360Capture: LLViewerCamera.getYaw() not yet implemented"); 0f }
                    val cmd = "init($outputImageWidth, $outputImageHeight, '$saveDir', $cameraFov, $cameraYaw, $overlayLabel)"
                    plugin.executeJavaScript(cmd)
                }
            }
            else -> Unit
        }
    }

    private fun onSaveLocalBtn() {
        var regionName = ""
        var regionUrl  = "https://secondlife.com"
        val region = Agent.instance.getRegion()
        if (region != null) {
            regionName = region.getName()
                .replace("'", "")
                .replace("\"", "")
            System.err.println("Floater360Capture: buildSLURL / regionUrl not yet implemented")
        }

        val suggestedFilename = generateProposedFilename()
        val clientVersion = run { System.err.println("Floater360Capture: LLVersionInfo.getChannel/getShortVersion not yet implemented"); "" }
        val timeStr = run { System.err.println("Floater360Capture: current time string not yet implemented"); "" }

        val selectedSourceSize = qualityRadioGroup?.getSelectedValue()?.asInteger() ?: sourceImageSize
        val xmpDetails = buildString {
            append("{ ")
            append("pano_version: '2.2.1', ")
            append("software: '', ")
            append("capture_software: '$clientVersion', ")
            append("stitching_software: '$clientVersion', ")
            append("width: $outputImageWidth, ")
            append("height: $outputImageHeight, ")
            append("heading: $initialHeadingDeg, ")
            append("actual_source_image_size: $selectedSourceSize, ")
            append("scaled_source_image_size: $sourceImageSize, ")
            append("first_photo_date: '$timeStr', ")
            append("last_photo_date: '$timeStr', ")
            append("region_name: '$regionName', ")
            append("region_url: '$regionUrl', ")
            append(" }")
        }

        val cmd = "saveAsEqrImage(\"$suggestedFilename\", $xmpDetails)"
        webBrowser?.getMediaPlugin()?.executeJavaScript(cmd)
    }

    private fun freezeWorld(enable: Boolean) {
        if (enable) {
            System.err.println("Floater360Capture: freezeWorld(true) not yet implemented")
        } else {
            System.err.println("Floater360Capture: freezeWorld(false) not yet implemented")
        }
    }

    private fun generateProposedFilename(): String {
        val sb = StringBuilder("sl360_")
        val region = Agent.instance.getRegion()
        if (region != null) {
            val sanitized = region.getName().replace(Regex("[^a-zA-Z0-9]"), "_")
            if (sanitized.isNotEmpty()) sb.append(sanitized).append("_")
        }
        sb.append(outputImageWidth).append("x").append(outputImageHeight).append("_")
        sb.append(sourceImageSize).append("_")
        sb.append(run { System.err.println("Floater360Capture: LLDate.now().toLocalDateString not yet implemented"); "" })
        System.err.println("Floater360Capture: append .jpg on Windows not yet implemented")
        return sb.toString()
    }
}
