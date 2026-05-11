package com.firestorm.llui

import com.firestorm.llmath.Rect
import kotlin.math.max

// Perpetual spinner — cycles through a list of images at a configurable rate.
// Mirrors the MacOS / YouTube style indicator from LLLoadingIndicator.
class LoadingIndicator(
    name: String,
    rect: Rect = Rect(),
    imagesPerSec: Float = 1.0f
) : UICtrl(name, rect) {

    // Guard against zero/negative rates: at minimum one image per second.
    private val imagesPerSec: Float = max(imagesPerSec, 1e-6f)

    // Names / resource keys of the animation frames, in order.
    val images: MutableList<String> = mutableListOf()

    private var curImageIdx: Int = 0

    // Frame timer: tracks how much time has elapsed since the last frame switch.
    private val imageSwitchTimer: FrameTimer = FrameTimer()

    // Start spinning as soon as images have been loaded.
    fun initImages(imageKeys: List<String>) {
        images.clear()
        images.addAll(imageKeys)
        start()
    }

    fun stop() {
        imageSwitchTimer.stop()
    }

    fun start() {
        imageSwitchTimer.start()
        if (images.isNotEmpty()) {
            val period = 1.0f / (images.size * imagesPerSec)
            imageSwitchTimer.setExpirySeconds(period)
        }
    }

    fun reset() {
        curImageIdx = 0
    }

    override fun draw() {
        if (!visible) return

        if (imageSwitchTimer.isStarted && imageSwitchTimer.hasExpired) {
            if (images.isNotEmpty()) {
                curImageIdx = (curImageIdx + 1) % images.size
            }
            start()
        }

        if (images.isNotEmpty()) {
            val imageKey = images[curImageIdx]
            TODO("GPU: draw image '$imageKey' into rect $rect with alpha $drawContextAlpha")
        }

        super.draw()
    }
}

// Minimal frame-timer abstraction so the indicator can track elapsed time without
// a dependency on the full viewer timer infrastructure.
class FrameTimer {
    var isStarted: Boolean = false
        private set

    private var startTimeMs: Long = 0L
    private var expiryMs: Long = Long.MAX_VALUE

    val hasExpired: Boolean
        get() = isStarted && (System.currentTimeMillis() - startTimeMs) >= expiryMs

    fun start() {
        startTimeMs = System.currentTimeMillis()
        isStarted = true
    }

    fun stop() {
        isStarted = false
    }

    fun setExpirySeconds(seconds: Float) {
        expiryMs = (seconds * 1000f).toLong()
    }

    fun getElapsedSeconds(): Float =
        if (!isStarted) 0f else (System.currentTimeMillis() - startTimeMs) / 1000f
}
