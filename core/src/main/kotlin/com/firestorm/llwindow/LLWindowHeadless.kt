package com.firestorm.llwindow

class LLWindowHeadless(
    callbacks: LLWindowCallbacks,
    title: String,
    name: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    flags: UInt,
    fullscreen: Boolean,
    clearBackground: Boolean,
    enableVsync: Boolean,
    useGl: Boolean,
    ignorePixelDepth: Boolean,
) : LLWindow(callbacks, fullscreen, flags) {

    init {
        gKeyboard = LLKeyboardHeadless().also { it.setCallbacks(callbacks) }
    }

    override fun show() {}
    override fun hide() {}
    override fun close() {}
    override fun getVisible(): Boolean = false
    override fun getMinimized(): Boolean = false
    override fun getMaximized(): Boolean = false
    override fun maximize(): Boolean = false
    override fun minimize() {}
    override fun restore() {}
    override fun getFullscreen(): Boolean = false

    override fun getPosition(position: LLCoordScreen): Boolean = false
    override fun getSize(size: LLCoordScreen): Boolean = false
    override fun getSize(size: LLCoordWindow): Boolean = false
    override fun setPosition(position: LLCoordScreen): Boolean = false
    override fun setSizeImpl(size: LLCoordScreen): Boolean = false
    override fun setSizeImpl(size: LLCoordWindow): Boolean = false

    override fun switchContext(fullscreen: Boolean, size: LLCoordScreen, enableVsync: Boolean, pos: LLCoordScreen?): Boolean = false

    override fun createSharedContext(): Any? = null
    override fun makeContextCurrent(context: Any?) {}
    override fun destroySharedContext(context: Any?) {}

    override fun toggleVSync(enableVsync: Boolean) {}

    override fun setCursorPosition(position: LLCoordWindow): Boolean = false
    override fun getCursorPosition(position: LLCoordWindow): Boolean = false
    override fun isWrapMouse(): Boolean = true
    override fun showCursor() {}
    override fun hideCursor() {}
    override fun showCursorFromMouseMove() {}
    override fun hideCursorUntilMouseMove() {}
    override fun isCursorHidden(): Boolean = false
    override fun updateCursor() {}

    override fun captureMouse() {}
    override fun releaseMouse() {}
    override fun setMouseClipping(b: Boolean) {}

    override fun isClipboardTextAvailable(): Boolean = false
    override fun pasteTextFromClipboard(dst: StringBuilder): Boolean = false
    override fun copyTextToClipboard(src: LLWString): Boolean = false

    override fun flashIcon(seconds: Float) {}
    override fun getGamma(): Float = 1.0f
    override fun setGamma(gamma: Float): Boolean = false
    override fun setFSAASamples(fsaaSamples: UInt) {}
    override fun getFSAASamples(): UInt = 0u
    override fun restoreGamma(): Boolean = false

    override fun gatherInput() {}
    override fun delayInputProcessing() {}
    override fun swapBuffers() {}

    override fun convertCoords(from: LLCoordScreen, to: LLCoordWindow): Boolean = false
    override fun convertCoords(from: LLCoordWindow, to: LLCoordScreen): Boolean = false
    override fun convertCoords(from: LLCoordWindow, to: LLCoordGL): Boolean = false
    override fun convertCoords(from: LLCoordGL, to: LLCoordWindow): Boolean = false
    override fun convertCoords(from: LLCoordScreen, to: LLCoordGL): Boolean = false
    override fun convertCoords(from: LLCoordGL, to: LLCoordScreen): Boolean = false

    override fun getSupportedResolutions(): Array<LLWindowResolution>? = null
    override fun getNativeAspectRatio(): Float = 1.0f
    override fun getPixelAspectRatio(): Float = 1.0f
    override fun setNativeAspectRatio(aspect: Float) {}

    override fun getPlatformWindow(): Any? = null
    override fun bringToFront() {}
}

class LLKeyboardHeadless : LLKeyboard() {
    override fun handleKeyUp(key: NativeKeyType, mask: MASK): Boolean = false
    override fun handleKeyDown(key: NativeKeyType, mask: MASK): Boolean = false
    override fun resetMaskKeys() {}
    override fun scanKeyboard() {}
    override fun currentMask(forMouseEvent: Boolean): MASK = KeyConstants.MASK_NONE
}

class LLSplashScreenHeadless : LLSplashScreen() {
    override fun showImpl() {}
    override fun updateImpl(message: String) {}
    override fun hideImpl() {}
}
