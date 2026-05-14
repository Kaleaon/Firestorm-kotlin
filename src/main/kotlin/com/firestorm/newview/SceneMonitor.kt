package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import kotlin.math.sqrt
import java.io.File
import java.io.PrintWriter

var gSceneMonitorView: SceneMonitorView? = null

object SceneMonitor {

    private enum class DiffState {
        WAITING_FOR_NEXT_DIFF,
        NEED_DIFF,
        EXECUTE_DIFF,
        WAIT_ON_RESULT,
        VIEWER_QUITTING
    }

    private var enabled: Boolean = false
    private var debugViewerVisible: Boolean = false
    private var diffState: DiffState = DiffState.WAITING_FOR_NEXT_DIFF

    private val avatarPauseHandles: MutableList<Any> = mutableListOf()

    private var diffResult: Float = 0f
    private var diffTolerance: Float = 0.1f
    private var diffPixelRatio: Float = 0.5f

    private var ditherMatrixWidth: Int = 0
    private var ditherScale: Float = 0f
    private var ditherScaleS: Float = 0f
    private var ditherScaleT: Float = 0f

    private var queryObject: UInt = 0u

    fun isEnabled(): Boolean = enabled
    fun getDiffResult(): Float = diffResult
    fun getDiffTolerance(): Float = diffTolerance
    fun getDiffPixelRatio(): Float = diffPixelRatio

    fun setDiffTolerance(tol: Float) {
        diffTolerance = tol
    }

    fun getDiffTarget(): Any? {
        System.err.println("SceneMonitor: getDiffTarget not yet implemented")
        return null
    }

    fun needsUpdate(): Boolean = diffState == DiffState.NEED_DIFF

    fun freezeAvatar(avatar: Any?) {
        if (enabled) {
            System.err.println("SceneMonitor: freezeAvatar not yet implemented")
        }
    }

    fun setDebugViewerVisible(visible: Boolean) {
        debugViewerVisible = visible
    }

    fun reset() {
        System.err.println("SceneMonitor: reset not yet implemented")
    }

    fun capture() {
        System.err.println("SceneMonitor: capture not yet implemented")
    }

    fun compare() {
        System.err.println("SceneMonitor: compare not yet implemented")
    }

    fun fetchQueryResult() {
        System.err.println("SceneMonitor: fetchQueryResult not yet implemented")
    }

    fun calcDiffAggregate() {
        System.err.println("SceneMonitor: calcDiffAggregate not yet implemented")
    }

    fun hasResults(): Boolean {
        System.err.println("SceneMonitor: hasResults not yet implemented")
        return false
    }

    fun dumpToFile(fileName: String) {
        if (!hasResults()) return
        System.err.println("SceneMonitor: dumpToFile not yet implemented")
    }

    private fun freezeScene() {
        System.err.println("SceneMonitor: freezeScene not yet implemented")
    }

    private fun unfreezeScene() {
        System.err.println("SceneMonitor: unfreezeScene not yet implemented")
    }

    private fun getCaptureTarget(): Any {
        System.err.println("SceneMonitor: getCaptureTarget not yet implemented")
        return Any()
    }

    private fun generateDitheringTexture(width: Int, height: Int) {
        ditherMatrixWidth = 4
        val ditherMatrix = arrayOf(
            intArrayOf(1, 9, 3, 11),
            intArrayOf(13, 5, 15, 7),
            intArrayOf(4, 12, 2, 10),
            intArrayOf(16, 8, 14, 6)
        )
        ditherScale = 255f / 17f
        ditherScaleS = width.toFloat() / ditherMatrixWidth
        ditherScaleT = height.toFloat() / ditherMatrixWidth
        System.err.println("SceneMonitor: generateDitheringTexture not yet implemented")
    }
}

open class SceneMonitorView {

    private var teleportFinishConnection: (() -> Unit)? = null

    init {
        System.err.println("SceneMonitorView: init (teleport-finish signal) not yet implemented")
    }

    open fun draw() {
        System.err.println("SceneMonitorView: draw not yet implemented")
    }

    open fun onVisibilityChange(visible: Boolean) {
        SceneMonitor.setDebugViewerVisible(visible)
    }

    open fun closeFloater(appQuitting: Boolean = false) {
        System.err.println("SceneMonitorView: closeFloater not yet implemented")
    }

    private fun onTeleportFinished() {
        if (false) {
            SceneMonitor.reset()
        }
    }
}
