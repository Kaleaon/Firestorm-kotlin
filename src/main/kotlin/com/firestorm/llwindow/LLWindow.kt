package com.firestorm.llwindow

typealias LLWString = String

data class LLCoordScreen(var mX: Int = 0, var mY: Int = 0)
data class LLCoordWindow(var mX: Int = 0, var mY: Int = 0)
data class LLCoordGL(var mX: Int = 0, var mY: Int = 0)
data class LLCoordCommon(var mX: Int = 0, var mY: Int = 0)

interface LLWindowCallbacks {
    fun handleUnicodeChar(unicodeChar: Int, mask: UInt)
    fun handleTranslatedKeyDown(translatedKey: UByte, translatedMask: UInt, repeated: Boolean): Boolean
    fun handleTranslatedKeyUp(translatedKey: UByte, translatedMask: UInt): Boolean
}

interface LLPreeditor

abstract class LLWindow protected constructor(
    protected var mCallbacks: LLWindowCallbacks?,
    protected var mFullscreen: Boolean,
    protected var mFlags: UInt,
) {
    data class LLWindowResolution(val mWidth: Int, val mHeight: Int)

    enum class ESwapMethod {
        SWAP_METHOD_UNDEFINED,
        SWAP_METHOD_EXCHANGE,
        SWAP_METHOD_COPY,
    }

    protected var mPostQuit: Boolean = true
    protected var mFullscreenWidth: Int = 0
    protected var mFullscreenHeight: Int = 0
    protected var mFullscreenRefresh: Int = 0
    protected var mSupportedResolutions: Array<LLWindowResolution>? = null
    protected var mNumSupportedResolutions: Int = 0
    protected var mCurrentCursor: ECursorType = ECursorType.UI_CURSOR_ARROW
    protected var mNextCursor: ECursorType = ECursorType.UI_CURSOR_ARROW
    protected var mCursorHidden: Boolean = false
    protected var mBusyCount: Int = 0
    protected var mIsMouseClipping: Boolean = false
    protected var mSwapMethod: ESwapMethod = ESwapMethod.SWAP_METHOD_UNDEFINED
    protected var mHideCursorPermanent: Boolean = false
    protected var mHighSurrogate: UShort = 0u
    protected var mMinWindowWidth: Int = 0
    protected var mMinWindowHeight: Int = 0
    protected var mRefreshRate: Int = 0

    abstract fun show()
    abstract fun hide()
    abstract fun close()
    abstract fun getVisible(): Boolean
    abstract fun getMinimized(): Boolean
    abstract fun getMaximized(): Boolean
    abstract fun maximize(): Boolean
    abstract fun minimize()
    abstract fun restore()

    open fun getFullscreen(): Boolean = mFullscreen

    abstract fun getPosition(position: LLCoordScreen): Boolean
    abstract fun getSize(size: LLCoordScreen): Boolean
    abstract fun getSize(size: LLCoordWindow): Boolean
    abstract fun setPosition(position: LLCoordScreen): Boolean

    fun setSize(size: LLCoordScreen): Boolean {
        val clamped = if (!getMaximized()) {
            size.copy(mX = maxOf(size.mX, mMinWindowWidth), mY = maxOf(size.mY, mMinWindowHeight))
        } else size
        return setSizeImpl(clamped)
    }

    fun setSize(size: LLCoordWindow): Boolean {
        val clamped = if (!getMaximized()) {
            size.copy(mX = maxOf(size.mX, mMinWindowWidth), mY = maxOf(size.mY, mMinWindowHeight))
        } else size
        return setSizeImpl(clamped)
    }

    open fun setMinSize(minWidth: UInt, minHeight: UInt, enforceImmediately: Boolean = true) {
        mMinWindowWidth = minWidth.toInt()
        mMinWindowHeight = minHeight.toInt()
        if (enforceImmediately) {
            val curSize = LLCoordScreen()
            if (!getMaximized() && getSize(curSize)) {
                if (curSize.mX < mMinWindowWidth || curSize.mY < mMinWindowHeight) {
                    setSizeImpl(LLCoordScreen(
                        maxOf(curSize.mX, mMinWindowWidth),
                        maxOf(curSize.mY, mMinWindowHeight)
                    ))
                }
            }
        }
    }

    abstract fun switchContext(fullscreen: Boolean, size: LLCoordScreen, enableVsync: Boolean, pos: LLCoordScreen? = null): Boolean

    abstract fun createSharedContext(): Any?
    abstract fun makeContextCurrent(context: Any?)
    abstract fun destroySharedContext(context: Any?)

    abstract fun toggleVSync(enableVsync: Boolean)

    abstract fun setCursorPosition(position: LLCoordWindow): Boolean
    abstract fun getCursorPosition(position: LLCoordWindow): Boolean
    abstract fun isWrapMouse(): Boolean
    abstract fun showCursor()
    abstract fun hideCursor()
    abstract fun isCursorHidden(): Boolean
    abstract fun showCursorFromMouseMove()
    abstract fun hideCursorUntilMouseMove()

    open fun setTitle(title: String) {}

    open fun incBusyCount() { mBusyCount++ }
    open fun decBusyCount() { if (mBusyCount > 0) mBusyCount-- }
    open fun resetBusyCount() { mBusyCount = 0 }
    open fun getBusyCount(): Int = mBusyCount

    open fun setCursor(cursor: ECursorType) { mNextCursor = cursor }
    open fun getCursor(): ECursorType = mCurrentCursor
    open fun getNextCursor(): ECursorType = mNextCursor
    abstract fun updateCursor()

    abstract fun captureMouse()
    abstract fun releaseMouse()
    abstract fun setMouseClipping(b: Boolean)

    abstract fun isClipboardTextAvailable(): Boolean
    abstract fun pasteTextFromClipboard(dst: StringBuilder): Boolean
    abstract fun copyTextToClipboard(src: LLWString): Boolean

    open fun isPrimaryTextAvailable(): Boolean = false
    open fun pasteTextFromPrimary(dst: StringBuilder): Boolean = false
    open fun copyTextToPrimary(src: LLWString): Boolean = false

    abstract fun flashIcon(seconds: Float)
    abstract fun getGamma(): Float
    abstract fun setGamma(gamma: Float): Boolean
    abstract fun setFSAASamples(fsaaSamples: UInt)
    abstract fun getFSAASamples(): UInt
    abstract fun restoreGamma(): Boolean

    open fun getSwapMethod(): ESwapMethod = mSwapMethod
    open fun processMiscNativeEvents() {}
    abstract fun gatherInput()
    abstract fun delayInputProcessing()
    abstract fun swapBuffers()
    abstract fun bringToFront()
    open fun focusClient() {}

    abstract fun convertCoords(from: LLCoordScreen, to: LLCoordWindow): Boolean
    abstract fun convertCoords(from: LLCoordWindow, to: LLCoordScreen): Boolean
    abstract fun convertCoords(from: LLCoordWindow, to: LLCoordGL): Boolean
    abstract fun convertCoords(from: LLCoordGL, to: LLCoordWindow): Boolean
    abstract fun convertCoords(from: LLCoordScreen, to: LLCoordGL): Boolean
    abstract fun convertCoords(from: LLCoordGL, to: LLCoordScreen): Boolean

    abstract fun getSupportedResolutions(): Array<LLWindowResolution>?
    abstract fun getNativeAspectRatio(): Float
    abstract fun getPixelAspectRatio(): Float
    abstract fun setNativeAspectRatio(aspect: Float)

    open fun beforeDialog() {}
    open fun afterDialog() {}

    open fun dialogColorPicker(r: FloatArray, g: FloatArray, b: FloatArray): Boolean = false

    abstract fun getPlatformWindow(): Any?
    open fun getMediaWindow(): Any? = getPlatformWindow()

    open fun allowLanguageTextInput(preeditor: LLPreeditor?, b: Boolean) {}
    open fun setLanguageTextInput(pos: LLCoordGL) {}
    open fun updateLanguageTextInputArea() {}
    open fun interruptLanguageTextInput() {}
    open fun spawnWebBrowser(escapedUrl: String, async: Boolean) {}
    open fun openFile(fileName: String) {}

    open fun getNativeKeyData(): Map<String, Any> = emptyMap()
    open fun getSystemUISize(): Float = 1.0f
    open fun getDirectInput8(): Any? = null
    open fun getInputDevices(deviceTypeFilter: UInt): Boolean = false
    open fun getRefreshRate(): Int = mRefreshRate
    open fun initWatchdog() {}

    open fun getWindowChrome(chromeW: IntArray, chromeH: IntArray) {
        chromeW[0] = 0
        chromeH[0] = 0
    }

    protected open fun isValid(): Boolean = true
    protected open fun canDelete(): Boolean = true

    protected abstract fun setSizeImpl(size: LLCoordScreen): Boolean
    protected abstract fun setSizeImpl(size: LLCoordWindow): Boolean

    fun handleUnicodeUTF16(utf16: UShort, mask: UInt) {
        fun isHighSurrogate(u: UShort) = (u - 0xD800u).toUShort() < 0x0400u
        fun isLowSurrogate(u: UShort) = (u - 0xDC00u).toUShort() < 0x0400u
        fun surrogatePairToUtf32(high: UShort, low: UShort): Int =
            ((high.toInt() shl 10) + low.toInt() - (0xD800 shl 10) - 0xDC00 + 0x00010000)

        if (mHighSurrogate == 0.toUShort()) {
            if (isHighSurrogate(utf16)) {
                mHighSurrogate = utf16
            } else {
                mCallbacks?.handleUnicodeChar(utf16.toInt(), mask)
            }
        } else {
            when {
                isLowSurrogate(utf16) -> {
                    mCallbacks?.handleUnicodeChar(surrogatePairToUtf32(mHighSurrogate, utf16), mask)
                    mHighSurrogate = 0u
                }
                isHighSurrogate(utf16) -> {
                    mCallbacks?.handleUnicodeChar(mHighSurrogate.toInt(), mask)
                    mHighSurrogate = utf16
                }
                else -> {
                    mCallbacks?.handleUnicodeChar(mHighSurrogate.toInt(), mask)
                    mHighSurrogate = 0u
                    mCallbacks?.handleUnicodeChar(utf16.toInt(), mask)
                }
            }
        }
    }

    companion object {
        fun getDynamicFallbackFontList(): MutableList<String> {
            TODO("Platform: use JVM equivalent for platform font list")
        }

        fun getDisplaysResolutionList(): MutableList<String> {
            TODO("Platform: use JVM equivalent for display resolution enumeration")
        }
    }
}

abstract class LLSplashScreen {
    protected abstract fun showImpl()
    protected abstract fun updateImpl(message: String)
    protected abstract fun hideImpl()

    companion object {
        private var instance: LLSplashScreen? = null

        fun isVisible(): Boolean = instance != null

        fun create(): LLSplashScreen? {
            TODO("Platform: return platform-specific LLSplashScreen subclass")
        }

        fun show() {
            if (instance == null) {
                instance = create()?.also { it.showImpl() }
            }
        }

        fun update(message: String) {
            show()
            instance?.updateImpl(message)
        }

        fun hide() {
            instance?.hideImpl()
            instance = null
        }
    }
}

object OSMessageBoxType {
    const val OSMB_OK: UInt = 0u
    const val OSMB_OKCANCEL: UInt = 1u
    const val OSMB_YESNO: UInt = 2u
}

object OSMessageBoxButton {
    const val OSBTN_YES: Int = 0
    const val OSBTN_NO: Int = 1
    const val OSBTN_OK: Int = 2
    const val OSBTN_CANCEL: Int = 3
}

fun osMessageBox(text: String, caption: String, type: UInt): Int {
    val wasVisible = LLSplashScreen.isVisible()
    if (wasVisible) LLSplashScreen.hide()
    val result = TODO("Platform: show a native message-box dialog") as Int
    if (wasVisible) LLSplashScreen.show()
    return result
}

object LLWindowManager {
    private val windowSet: MutableSet<LLWindow> = mutableSetOf()

    val urlProtocolWhitelist: List<String> = listOf(
        "secondlife:", "http:", "https:", "ftp:", "data:", "mailto:"
    )

    var debugWindowProc: Boolean = false

    fun createWindow(
        callbacks: LLWindowCallbacks,
        title: String,
        name: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        flags: UInt = 0u,
        fullscreen: Boolean = false,
        clearBg: Boolean = false,
        enableVsync: Boolean = false,
        useGl: Boolean = true,
        ignorePixelDepth: Boolean = false,
        fsaaSamples: UInt = 0u,
        maxCores: UInt = 0u,
        maxGlVersion: Float = 4.6f,
        useLegacyCursors: Boolean = false,
    ): LLWindow? {
        val newWindow: LLWindow = if (useGl) {
            TODO("Platform: construct platform-specific LLWindow (Win32 / macOS / SDL)")
        } else {
            LLWindowHeadless(callbacks, title, name, x, y, width, height, flags, fullscreen, clearBg, enableVsync, useGl, ignorePixelDepth)
        }
        if (!newWindow.isValid()) {
            return null
        }
        windowSet.add(newWindow)
        return newWindow
    }

    fun destroyWindow(window: LLWindow): Boolean {
        require(window in windowSet) {
            "LLWindowManager.destroyWindow(): window pointer not valid"
        }
        window.close()
        windowSet.remove(window)
        return true
    }

    fun isWindowValid(window: LLWindow): Boolean = window in windowSet
}
