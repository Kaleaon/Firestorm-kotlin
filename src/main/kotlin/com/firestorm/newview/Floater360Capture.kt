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

        TODO("APR: enableResizeCtrls(true, true, true)")

        initialHeadingDeg = 0f

        imageSaveDir = TODO("APR: gDirUtilp.getLindenUserDir() + delimiter + 'eqrimg'; mkdir(imageSaveDir)")

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
                TODO("APR: log small snapshot size warning")
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
        TODO("APR: gDirUtilp.getDefaultSkinDir() + delimiter + 'html' + delimiter + 'common' + delimiter + 'equirectangular' + delimiter")
    }

    private fun onCapture360ImagesBtn() {
        capture360Images()
    }

    private fun makeFullPathToJS(filename: String): String {
        TODO("APR: imageSaveDir + gDirUtilp.getDirDelimiter() + filename")
    }

    private fun writeDataUrlHeader(filename: String) {
        TODO("APR: open file at filename for write, emit JS comment header line, close")
    }

    private fun writeDataUrlFooter(filename: String) {
        TODO("APR: open file at filename for append, emit cubemap_img_js array declaration referencing the 6 image vars, close")
    }

    private fun writeDataUrl(filename: String, prefix: String, data: ByteArray): Boolean {
        TODO("APR: Base64.encode(data), open file for append, write 'var img_\$prefix = data:image/jpeg;base64,...', close, return success")
    }

    private fun encodeAndSave(rawImage: Any, filename: String, prefix: String) {
        val quality = SavedSettings.getUInt("360CaptureJPEGEncodeQuality").toInt()
        TODO("GPU: encode rawImage as JPEG at quality=$quality, optionally save debug copy, then call writeDataUrl(filename, prefix, jpegBytes)")
    }

    private fun mockSnapshot(raw: Any) {
        TODO("GPU: fill raw image buffer with gradient test pattern (r=x/w, g=y/h, b=(x+y)/(2*(w+h)))")
    }

    private fun suspendForAFrame() {
        TODO("APR: yield coroutine for at least 1 rendered frame (LLFrameTimer.getFrameCount + 1)")
    }

    private fun capture360Images() {
        setSourceImageSize()

        captureBtn?.setEnabled(false)
        saveLocalBtn?.setEnabled(false)

        val renderAttachedLights = Pipeline.sRenderAttachedLights
        if (SavedSettings.getBool("360CaptureHideAvatars")) {
            TODO("GPU: LLPipeline.toggleRenderTypeControl(RENDER_TYPE_AVATAR); toggleRenderTypeControl(RENDER_TYPE_PARTICLES); sRenderAttachedLights = false")
        }

        val lookDirs = arrayOf(
            floatArrayOf(1f,0f,0f), floatArrayOf(0f,1f,0f), floatArrayOf(0f,0f,1f),
            floatArrayOf(-1f,0f,0f), floatArrayOf(0f,-1f,0f), floatArrayOf(0f,0f,-1f)
        )
        val lookUpvecs = arrayOf(
            floatArrayOf(0f,0f,1f), floatArrayOf(0f,0f,1f), floatArrayOf(0f,-1f,0f),
            floatArrayOf(0f,0f,1f), floatArrayOf(0f,0f,1f), floatArrayOf(0f,1f,0f)
        )

        val oldOcclusion = TODO("GPU: LLPipeline.sUseOcclusion.also { LLPipeline.sUseOcclusion = 0 }") as Int
        val camera = TODO("GPU: LLViewerCamera.getInstance()") as Any
        val oldFov    = TODO("GPU: camera.getView()") as Float
        val oldAspect = TODO("GPU: camera.getAspect()") as Float
        val oldYaw    = TODO("GPU: camera.getYaw()") as Float

        freezeWorld(true)

        initialHeadingDeg = TODO("GPU: ((360 + 90 - (camera.getYaw() * RAD_TO_DEG).toInt()) % 360).toFloat()") as Float

        TODO("GPU: camera.setAspect(1.0f); camera.setView(F_PI_BY_TWO); camera.yaw(0.0f)")

        val cubemapJsFilename = "cubemap_img.js"
        val cubemapJsFullPath = makeFullPathToJS(cubemapJsFilename)
        writeDataUrlHeader(cubemapJsFullPath)

        val prefixes = arrayOf("posx", "posz", "posy", "negx", "negz", "negy")
        val numRenderPasses = SavedSettings.getUInt("360CaptureNumRenderPasses").toInt()

        for (i in 0 until 6) {
            TODO("APR: LLAppViewer.instance.pauseMainloopTimeout(); LLViewerStats.instance.getRecording().stop()")
            TODO("GPU: allocate/resize rawImages[$i] = LLImageRaw(sourceImageSize, sourceImageSize, 3)")
            TODO("GPU: camera.lookDir(lookDirs[$i], lookUpvecs[$i])")
            TODO("GPU: gViewerWindow.simpleSnapshot(rawImages[$i], sourceImageSize, sourceImageSize, numRenderPasses)")
            encodeAndSave(rawImages[i]!!, cubemapJsFullPath, prefixes[i])
            TODO("APR: LLViewerStats.instance.getRecording().resume(); LLAppViewer.instance.resumeMainloopTimeout(); pingMainloopTimeout")
        }

        writeDataUrlFooter(cubemapJsFullPath)
        freezeWorld(false)

        TODO("GPU: camera.setAspect(oldAspect); camera.setView(oldFov); camera.yaw(oldYaw); LLPipeline.sUseOcclusion = oldOcclusion")

        if (SavedSettings.getBool("360CaptureHideAvatars")) {
            TODO("GPU: LLPipeline.toggleRenderTypeControl(RENDER_TYPE_AVATAR); toggleRenderTypeControl(RENDER_TYPE_PARTICLES); sRenderAttachedLights = renderAttachedLights")
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
                    val cameraYaw = TODO("GPU: LLViewerCamera.getInstance().getYaw()") as Float
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
            TODO("APR: build SLURL from LLAgentUI.buildSLURL(escaped=true); regionUrl = slurl.getSLURLString()")
        }

        val suggestedFilename = generateProposedFilename()
        val clientVersion = TODO("APR: LLVersionInfo.instance.getChannel() + ' ' + LLVersionInfo.instance.getShortVersion()") as String
        val timeStr = TODO("APR: java.time.Instant.now().toString() or locale equivalent") as String

        val selectedSourceSize = qualityRadioGroup?.getSelectedValue()?.asInteger() ?: sourceImageSize
        val xmpDetails = buildString {
            append("{ ")
            append("pano_version: '2.2.1', ")
            append("software: '${TODO("APR: LLVersionInfo.instance.getChannel()")}', ")
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
            TODO("APR: LLEnvironment.instance.pauseCloudScroll(); freeze LLCharacter.sInstances via requestPause(); SavedSettings.setBool('FreezeTime', true); LLViewerPartSim.getInstance().enable(false)")
        } else {
            TODO("APR: LLEnvironment.instance.resumeCloudScroll() if was not paused before; clear pause handles; SavedSettings.setBool('FreezeTime', false); LLViewerPartSim.getInstance().enable(true)")
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
        sb.append(TODO("APR: LLDate.now().toLocalDateString('%Y%m%d_%H%M%S')") as String)
        TODO("APR: append '.jpg' on Windows only")
        return sb.toString()
    }
}
