package com.firestorm.newview

import kotlin.math.floor

enum class SnapshotFormat { PNG, JPEG, BMP }
enum class SnapshotType { LOCAL, INVENTORY, POSTCARD, PROFILE }

abstract class PanelSnapshot {
    protected var snapshotFloater: Any? = null

    abstract fun getWidthSpinnerName(): String
    abstract fun getHeightSpinnerName(): String
    abstract fun getAspectRatioCBName(): String
    abstract fun getImageSizeComboName(): String
    abstract fun getImageSizePanelName(): String
    abstract fun updateControls(info: Map<String, Any>)

    open fun getImageFormat(): SnapshotFormat = SnapshotFormat.PNG
    open fun getSnapshotType(): SnapshotType = SnapshotType.LOCAL

    open fun postBuild(): Boolean {
        TODO("GPU: bind width/height spinners, aspect ratio checkbox, size combo from UI names")
        return true
    }

    open fun onOpen(key: Map<String, Any>) {
        TODO("GPU: restore panel state from saved settings on open")
    }

    fun getTypedPreviewWidth(): Int {
        TODO("GPU: return width spinner integer value")
    }

    fun getTypedPreviewHeight(): Int {
        TODO("GPU: return height spinner integer value")
    }

    fun getWidthSpinner(): Any? {
        TODO("GPU: return width spinner control by name ${getWidthSpinnerName()}")
    }

    fun getHeightSpinner(): Any? {
        TODO("GPU: return height spinner control by name ${getHeightSpinnerName()}")
    }

    fun getImageSizeComboBox(): Any? {
        TODO("GPU: return image size combo control by name ${getImageSizeComboName()}")
    }

    fun enableAspectRatioCheckbox(enable: Boolean) {
        TODO("GPU: set aspect ratio checkbox enabled = $enable")
    }

    fun enableControls(enable: Boolean) {
        TODO("GPU: enable/disable all snapshot panel controls")
    }

    protected fun updateImageQualityLevel() {
        TODO("GPU: read quality slider value and update image_quality_level label text")
    }

    protected fun goBack() {
        TODO("GPU: switch parent SideTrayPanelContainer to default snapshot options panel")
    }

    protected open fun cancel() {
        goBack()
    }

    protected fun onCustomResolutionCommit() {
        TODO("GPU: read width/height spinners and notify snapshot floater of custom resolution change")
    }

    protected fun onResolutionComboCommit(ctrl: Any?) {
        TODO("GPU: notify snapshot floater of resolution preset change from combo")
    }

    protected fun onKeepAspectRatioCommit(ctrl: Any?) {
        TODO("GPU: notify snapshot floater of aspect ratio lock toggle")
    }
}

class PanelSnapshotLocal : PanelSnapshot() {
    private var localFormat: Int = 0

    init {
        localFormat = savedSettings.getInt("SnapshotFormat")
    }

    override fun getWidthSpinnerName(): String = "local_snapshot_width"
    override fun getHeightSpinnerName(): String = "local_snapshot_height"
    override fun getAspectRatioCBName(): String = "local_keep_aspect_check"
    override fun getImageSizeComboName(): String = "local_size_combo"
    override fun getImageSizePanelName(): String = "local_image_size_lp"

    override fun getImageFormat(): SnapshotFormat {
        val id = TODO("GPU: return local_format_combo selected value string") as String
        return when (id) {
            "JPEG" -> SnapshotFormat.JPEG
            "BMP" -> SnapshotFormat.BMP
            else -> SnapshotFormat.PNG
        }
    }

    override fun getSnapshotType(): SnapshotType = SnapshotType.LOCAL

    override fun postBuild(): Boolean {
        TODO("GPU: bind onQualitySliderCommit to image_quality_slider")
        TODO("GPU: bind onFormatComboCommit to local_format_combo")
        TODO("GPU: bind onSaveFlyoutCommit to save_btn")
        TODO("GPU: restore local_size_combo index from LastSnapshotToDiskResolution setting")
        TODO("GPU: restore width/height spinners from LastSnapshotToDiskWidth/Height settings")
        return super.postBuild()
    }

    override fun onOpen(key: Map<String, Any>) {
        val index = savedSettings.getInt("FSSnapshotLocalFormat")
        savedSettings.setInt("SnapshotFormat", index)
        TODO("GPU: set local_format_combo current index to $index")
        super.onOpen(key)
    }

    override fun updateControls(info: Map<String, Any>) {
        val fmt = SnapshotFormat.values()[savedSettings.getInt("SnapshotFormat")]
        TODO("GPU: set local_format_combo selected index to ${fmt.ordinal}")

        val showQualityControls = fmt == SnapshotFormat.JPEG
        TODO("GPU: set image_quality_slider visible = $showQualityControls")
        TODO("GPU: set image_quality_level visible = $showQualityControls if control exists")

        val quality = savedSettings.getInt("SnapshotQuality")
        TODO("GPU: set image_quality_slider value to $quality")
        updateImageQualityLevel()

        val haveSnapshot = (info["have-snapshot"] as? Boolean) ?: true
        TODO("GPU: set save_btn enabled = $haveSnapshot")
    }

    fun destroy() {
        TODO("GPU: save local_size_combo index to LastSnapshotToDiskResolution")
        TODO("GPU: save width spinner value to LastSnapshotToDiskWidth")
        TODO("GPU: save height spinner value to LastSnapshotToDiskHeight")
    }

    private fun onFormatComboCommit(ctrl: Any?) {
        localFormat = getImageFormat().ordinal
        val comboIndex = TODO("GPU: return local_format_combo current index") as Int
        savedSettings.setInt("FSSnapshotLocalFormat", comboIndex)
        SnapshotFloater.notify(mapOf("image-format-change" to true))
    }

    private fun onQualitySliderCommit(ctrl: Any?) {
        updateImageQualityLevel()
        val rawValue = TODO("GPU: return slider double value") as Double
        val qualityVal = floor(rawValue.toFloat().toDouble()).toInt()
        SnapshotFloater.notify(mapOf("image-quality-change" to qualityVal))
    }

    private fun onSaveFlyoutCommit(ctrl: Any?) {
        val value = TODO("GPU: return save_btn selected value string") as String
        if (value == "save as") {
            TODO("GPU: reset snapshot save location via viewerWindow.resetSnapshotLoc()")
        }
        SnapshotFloater.notify(mapOf("set-working" to true))
        SnapshotFloater.saveLocal(
            onSaved = { onLocalSaved() },
            onCanceled = { onLocalCanceled() }
        )
    }

    private fun onLocalSaved() {
        (snapshotFloater as? Any)?.let { TODO("APR: use JVM equivalent - snapshotFloater.postSave()") }
        SnapshotFloater.notify(mapOf("set-finished" to mapOf("ok" to true, "msg" to "local")))
    }

    private fun onLocalCanceled() {
        SnapshotFloater.notify(mapOf("set-finished" to mapOf("ok" to false, "msg" to "local")))
    }

    companion object {
        private val savedSettings = object {
            fun getInt(key: String): Int = 0
            fun setInt(key: String, value: Int) {}
        }
    }
}

object SnapshotFloater {
    fun notify(info: Map<String, Any>) {
        TODO("APR: use JVM equivalent - dispatch info map to snapshot floater observers")
    }

    fun saveLocal(onSaved: () -> Unit, onCanceled: () -> Unit) {
        TODO("APR: use JVM equivalent - trigger local file save dialog and invoke callback on completion")
    }
}
