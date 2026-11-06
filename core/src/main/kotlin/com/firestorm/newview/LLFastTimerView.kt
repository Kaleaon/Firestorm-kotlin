package com.firestorm.newview

import java.io.InputStream

class LLFastTimerView(key: Any?) {

    companion object {
        var sAnalyzePerformance: Boolean = false

        private const val MAX_VISIBLE_HISTORY = 12
        private const val LINE_GRAPH_HEIGHT = 240
        private const val MIN_BAR_HEIGHT = 3
        private const val RUNNING_AVERAGE_WIDTH = 100
        private const val NUM_FRAMES_HISTORY = 200
        private const val MARGIN = 10

        fun outputAllMetrics() {
            System.err.println("LLFastTimerView: outputAllMetrics not yet implemented")
        }

        fun doAnalysis(baseline: String, target: String, output: String) {
            System.err.println("LLFastTimerView: doAnalysis not yet implemented")
        }

        private fun doAnalysisDefault(baseline: String, target: String, output: String) {
            System.err.println("LLFastTimerView: doAnalysisDefault not yet implemented")
        }

        private fun analyzePerformanceLogDefault(input: InputStream): Map<String, Any?> {
            System.err.println("LLFastTimerView: analyzePerformanceLogDefault not yet implemented")
            return emptyMap()
        }

        private fun exportCharts(base: String, target: String) {
            // no-op
        }
    }

    enum class EDisplayType {
        DISPLAY_TIME,
        DISPLAY_CALLS,
        DISPLAY_HZ
    }

    data class TimerBar(
        var totalTime: Float = 0f,
        var selfTime: Float = 0f,
        var childrenStart: Float = 0f,
        var childrenEnd: Float = 0f,
        var selfStart: Float = 0f,
        var selfEnd: Float = 0f,
        var timeBlock: Any? = null,
        var visible: Boolean = false,
        var firstChild: Boolean = false,
        var lastChild: Boolean = false,
        var startFraction: Float = 0f,
        var endFraction: Float = 1f
    )

    inner class TimerBarRow {
        var bottom: Int = 0
        var top: Int = 0
        var bars: Array<TimerBar>? = null
    }

    private val timerBarRows: ArrayDeque<TimerBarRow> = ArrayDeque<TimerBarRow>(NUM_FRAMES_HISTORY).also {
        repeat(NUM_FRAMES_HISTORY) { _ -> it.addLast(TimerBarRow()) }
    }
    private val averageTimerRow = TimerBarRow()

    private var displayType: EDisplayType = EDisplayType.DISPLAY_TIME
    private var pauseHistory: Boolean = false
    private var allTimeMax: Double = 0.0
    private var totalTimeDisplay: Double = 0.0
    private var scrollIndex: Int = 0
    private var hoverBarIndex: Int = -1
    private var statsIndex: Int = -1
    private var displayMode: Int = 0
    private var hoverId: Any? = null
    private var hoverTimer: Any? = null

    private var toolTipRectLeft = 0; private var toolTipRectTop = 0; private var toolTipRectRight = 0; private var toolTipRectBottom = 0
    private var graphRectLeft = 0; private var graphRectTop = 0; private var graphRectRight = 0; private var graphRectBottom = 0
    private var barRectLeft = 0; private var barRectTop = 0; private var barRectRight = 0; private var barRectBottom = 0
    private var legendRectLeft = 0; private var legendRectTop = 0; private var legendRectRight = 0; private var legendRectBottom = 0

    fun postBuild(): Boolean {
        System.err.println("LLFastTimerView: postBuild not yet implemented")
        return false
    }

    private fun onPause() {
        setPauseState(!pauseHistory)
    }

    private fun setPauseState(pauseState: Boolean) {
        if (pauseState == pauseHistory) return
        if (!pauseState) {
            System.err.println("LLFastTimerView: setPauseState (pause label) not yet implemented")
        } else {
            scrollIndex = 0
            System.err.println("LLFastTimerView: setPauseState (run label) not yet implemented")
        }
        pauseHistory = pauseState
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLFastTimerView: handleMouseDown not yet implemented")
        return false
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLFastTimerView: handleDoubleClick not yet implemented")
        return false
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLFastTimerView: handleRightMouseDown not yet implemented")
        return false
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLFastTimerView: handleMouseUp not yet implemented")
        return false
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLFastTimerView: handleHover not yet implemented")
        return false
    }

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("LLFastTimerView: handleToolTip not yet implemented")
        return false
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (x < barRectLeft) {
            System.err.println("LLFastTimerView: handleScrollWheel (scrollBar forward) not yet implemented")
        } else {
            setPauseState(true)
            System.err.println("LLFastTimerView: handleScrollWheel (scrollIndex update) not yet implemented")
        }
        return true
    }

    open fun draw() {
        // no-op
    }

    open fun onOpen(key: Any?) {
        setPauseState(false)
        System.err.println("LLFastTimerView: onOpen not yet implemented")
    }

    open fun onClose(appQuitting: Boolean) {
        System.err.println("LLFastTimerView: onClose not yet implemented")
    }

    fun getLegendID(y: Int): Any? {
        System.err.println("LLFastTimerView: getLegendID not yet implemented")
        return null
    }

    private fun drawTicks() {
        // no-op
    }

    private fun drawLineGraph() {
        // no-op
    }

    private fun drawLegend() {
        // no-op
    }

    private fun drawHelp(y: Int) {
        // no-op
    }

    private fun drawBorders(y: Int, xStart: Int, barHeight: Int, dy: Int) {
        // no-op
    }

    private fun drawBars() {
        // no-op
    }

    private fun printLineStats() {
        if (statsIndex >= 0) {
            System.err.println("LLFastTimerView: printLineStats not yet implemented")
        }
    }

    private fun generateUniqueColors() {
        // no-op
    }

    private fun updateTotalTime() {
        System.err.println("LLFastTimerView: updateTotalTime not yet implemented")
    }

    private fun updateTimerBarWidths(timeBlock: Any?, row: TimerBarRow, historyIndex: Int, barIndex: IntArray): Float {
        System.err.println("LLFastTimerView: updateTimerBarWidths not yet implemented")
        return 0f
    }

    private fun updateTimerBarOffsets(timeBlock: Any?, row: TimerBarRow, timerBarIndex: Int = 0): Int {
        System.err.println("LLFastTimerView: updateTimerBarOffsets not yet implemented")
        return 0
    }

    private fun drawBar(
        barRectL: Int, barRectT: Int, barRectR: Int, barRectB: Int,
        row: TimerBarRow,
        imageWidth: Int, imageHeight: Int,
        hovered: Boolean = false,
        visible: Boolean = true,
        barIndex: Int = 0
    ): Int {
        // no-op
        return 0
    }
}
