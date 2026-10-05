package com.firestorm.llui

import java.util.Timer
import java.util.TimerTask

class FlashTimer(
    private var callback: ((highlighted: Boolean) -> Unit)? = null,
    flashCount: Int = 0,
    period: Float = 0f
) {
    companion object {
        private const val DEFAULT_FLASH_COUNT = 6
        private const val DEFAULT_FLASH_PERIOD_MS = 500L
    }

    private var flashCount: Int = if (flashCount > 0) 2 * flashCount else 2 * DEFAULT_FLASH_COUNT
    private var periodMs: Long = if (period > 0f) (period * 1000f).toLong() else DEFAULT_FLASH_PERIOD_MS

    private var currentTickCount: Int = 0
    private var isCurrentlyHighlighted: Boolean = false
    private var isFlashingInProgress: Boolean = false
    private var unset: Boolean = false

    private var timer: Timer? = null

    fun startFlashing() {
        isFlashingInProgress = true
        isCurrentlyHighlighted = true
        timer?.cancel()
        timer = Timer(true).also {
            it.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { tick() }
            }, periodMs, periodMs)
        }
    }

    fun stopFlashing() {
        timer?.cancel()
        timer = null
        isFlashingInProgress = false
        isCurrentlyHighlighted = false
        currentTickCount = 0
    }

    fun isFlashingInProgress(): Boolean = isFlashingInProgress

    fun isCurrentlyHighlighted(): Boolean = isCurrentlyHighlighted

    fun updateFlashSettings(newCount: Int, newPeriodSeconds: Float) {
        stopFlashing()
        flashCount = 2 * maxOf(newCount, 0)
        periodMs = maxOf((newPeriodSeconds * 1000f).toLong(), 0L)
    }

    fun unset() {
        unset = true
        callback = null
        stopFlashing()
    }

    private fun tick() {
        isCurrentlyHighlighted = !isCurrentlyHighlighted
        callback?.invoke(isCurrentlyHighlighted)

        if (++currentTickCount >= flashCount) {
            stopFlashing()
        }

        if (unset) {
            stopFlashing()
        }
    }
}
