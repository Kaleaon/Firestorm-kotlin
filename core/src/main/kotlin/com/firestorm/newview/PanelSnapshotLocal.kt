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
        System.err.println("PanelSnapshot: postBuild not yet implemented")
        return true
    }

    open fun onOpen(key: Map<String, Any>) {
        System.err.println("PanelSnapshot: onOpen not yet implemented")
    }

    fun getTypedPreviewWidth(): Int {
        return 0
    }

    fun getTypedPreviewHeight(): Int {
        return 0
    }

    fun getWidthSpinner(): Any? {
        return null
    }

    fun getHeightSpinner(): Any? {
        return null
    }

    fun getImageSizeComboBox(): Any? {
        return null
    }

    fun enableAspectRatioCheckbox(enable: Boolean) {
        // no-op
    }

    fun enableControls(enable: Boolean) {
        // no-op
    }

    protected fun updateImageQualityLevel() {
        // no-op
    }

    protected fun goBack() {
        System.err.println("PanelSnapshot: goBack not yet implemented")
    }

    protected open fun cancel() {
        goBack()
    }

    protected fun onCustomResolutionCommit() {
        System.err.println("PanelSnapshot: onCustomResolutionCommit not yet implemented")
    }

    protected fun onResolutionComboCommit(ctrl: Any?) {
        System.err.println("PanelSnapshot: onResolutionComboCommit not yet implemented")
    }

    protected fun onKeepAspectRatioCommit(ctrl: Any?) {
        System.err.println("PanelSnapshot: onKeepAspectRatioCommit not yet implemented")
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
        val id = ""
        return when (id) {
            "JPEG" -> SnapshotFormat.JPEG
            "BMP" -> SnapshotFormat.BMP
            else -> SnapshotFormat.PNG
        }
    }

    override fun getSnapshotType(): SnapshotType = SnapshotType.LOCAL

    override fun postBuild(): Boolean {
        System.err.println("PanelSnapshotLocal: postBuild not yet implemented")
        System.err.println("PanelSnapshotLocal: postBuild not yet implemented")
        System.err.println("PanelSnapshotLocal: postBuild not yet implemented")
        System.err.println("PanelSnapshotLocal: postBuild not yet implemented")
        System.err.println("PanelSnapshotLocal: postBuild not yet implemented")
        return super.postBuild()
    }

    override fun onOpen(key: Map<String, Any>) {
        val index = savedSettings.getInt("FSSnapshotLocalFormat")
        savedSettings.setInt("SnapshotFormat", index)
        System.err.println("PanelSnapshotLocal: onOpen not yet implemented")
        super.onOpen(key)
    }

    override fun updateControls(info: Map<String, Any>) {
        val fmt = SnapshotFormat.values()[savedSettings.getInt("SnapshotFormat")]
        System.err.println("PanelSnapshotLocal: updateControls not yet implemented")

        val showQualityControls = fmt == SnapshotFormat.JPEG
        System.err.println("PanelSnapshotLocal: updateControls not yet implemented")
        System.err.println("PanelSnapshotLocal: updateControls not yet implemented")

        val quality = savedSettings.getInt("SnapshotQuality")
        System.err.println("PanelSnapshotLocal: updateControls not yet implemented")
        updateImageQualityLevel()

        val haveSnapshot = (info["have-snapshot"] as? Boolean) ?: true
        System.err.println("PanelSnapshotLocal: updateControls not yet implemented")
    }

    fun destroy() {
        System.err.println("PanelSnapshotLocal: destroy not yet implemented")
        System.err.println("PanelSnapshotLocal: destroy not yet implemented")
        System.err.println("PanelSnapshotLocal: destroy not yet implemented")
    }

    private fun onFormatComboCommit(ctrl: Any?) {
        localFormat = getImageFormat().ordinal
        val comboIndex = 0
        savedSettings.setInt("FSSnapshotLocalFormat", comboIndex)
        SnapshotFloater.notify(mapOf("image-format-change" to true))
    }

    private fun onQualitySliderCommit(ctrl: Any?) {
        updateImageQualityLevel()
        val rawValue = 0.0
        val qualityVal = floor(rawValue.toFloat().toDouble()).toInt()
        SnapshotFloater.notify(mapOf("image-quality-change" to qualityVal))
    }

    private fun onSaveFlyoutCommit(ctrl: Any?) {
        val value = ""
        if (value == "save as") {
            System.err.println("PanelSnapshotLocal: onSaveFlyoutCommit not yet implemented")
        }
        SnapshotFloater.notify(mapOf("set-working" to true))
        SnapshotFloater.saveLocal(
            onSaved = { onLocalSaved() },
            onCanceled = { onLocalCanceled() }
        )
    }

    private fun onLocalSaved() {
        (snapshotFloater as? Any)?.let { System.err.println("PanelSnapshotLocal: onLocalSaved not yet implemented") }
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
        System.err.println("SnapshotFloater: notify not yet implemented")
    }

    fun saveLocal(onSaved: () -> Unit, onCanceled: () -> Unit) {
        System.err.println("SnapshotFloater: saveLocal not yet implemented")
    }
}
