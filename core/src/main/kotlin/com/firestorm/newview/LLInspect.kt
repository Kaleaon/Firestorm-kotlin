package com.firestorm.newview

abstract class LLInspect(key: Any?) : LLFloater(key) {

    private var closeTimerStarted: Boolean = false
    private var closeTimerStartMs: Long = 0L
    private var openTimerStarted: Boolean = false
    private var openTimerStartMs: Long = 0L
    private var openTimerPaused: Boolean = false
    private var openTimerPausedAt: Long = 0L

    companion object {
        private const val FADE_TIME_MS: Long = 500L
        private const val STAY_TIME_MS: Long = 3000L
    }

    open fun draw() {
        val nowMs = System.currentTimeMillis()
        when {
            openTimerStarted -> {
                super.draw()
                val elapsed = if (openTimerPaused) openTimerPausedAt - openTimerStartMs
                              else nowMs - openTimerStartMs
                if (elapsed > STAY_TIME_MS) {
                    openTimerStarted = false
                    closeTimerStarted = true
                    closeTimerStartMs = nowMs
                }
            }
            closeTimerStarted -> {
                val elapsed = nowMs - closeTimerStartMs
                val alpha = (1f - elapsed.toFloat() / FADE_TIME_MS.toFloat()).coerceIn(0f, 1f)
                // no-op
                if (elapsed > FADE_TIME_MS) {
                    closeFloater(false)
                }
            }
            else -> super.draw()
        }
    }

    open fun onOpen(data: Any?) {
        super.onOpen(data)
        closeTimerStarted = false
        openTimerStarted = true
        openTimerStartMs = System.currentTimeMillis()
        openTimerPaused = false
    }

    open fun onFocusLost() {
        super.onFocusLost()
        closeTimerStarted = true
        closeTimerStartMs = System.currentTimeMillis()
        openTimerStarted = false
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (openTimerStarted && !openTimerPaused) {
            openTimerPaused = true
            openTimerPausedAt = System.currentTimeMillis()
        }
        return super.handleHover(x, y, mask)
    }

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        val childHandler = childFromPoint(x, y) ?: return false
        val tip = childHandler.getToolTip()
        if (tip.isNullOrEmpty()) return false
        return false
    }

    open fun onMouseLeave(x: Int, y: Int, mask: Int) {
        if (openTimerStarted && openTimerPaused) {
            val pauseDuration = System.currentTimeMillis() - openTimerPausedAt
            openTimerStartMs += pauseDuration
            openTimerPaused = false
        }
    }

    protected open fun childHasVisiblePopupMenu(): Boolean {
        return false
    }

    fun repositionInspector(data: Any?) {
        System.err.println("LLInspect: repositionInspector not yet implemented")
    }

    protected open fun closeFloater(appQuitting: Boolean) {
        System.err.println("LLInspect: closeFloater not yet implemented")
    }

    protected open fun childFromPoint(x: Int, y: Int): LLView? {
        return null
    }
}
