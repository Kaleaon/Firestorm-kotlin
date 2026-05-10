/**
 * AppViewer.kt
 * Kotlin conversion of llappviewer.h / llappviewer.cpp
 *
 * Original: Second Life Viewer Source Code
 * Copyright (C) 2010, Linden Research, Inc.
 * Licensed under LGPL v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// Last-exec event enumeration (mirrors eLastExecEvent)
// ---------------------------------------------------------------------------

enum class LastExecEvent {
    NORMAL,
    FROZE,
    LLERROR_CRASH,
    OTHER_CRASH,
    LOGOUT_FROZE,
    LOGOUT_CRASH,
    BAD_ALLOC,
    MISSING_FILES,
    GRAPHICS_INIT,
    UNKNOWN,
    LOGOUT_UNKNOWN;
}

// ---------------------------------------------------------------------------
// Idle callback data (mirrors the on-idle callback mechanism)
// ---------------------------------------------------------------------------

/**
 * Data carrier for a one-shot idle callback.
 *
 * @param id       Stable identifier for removal.
 * @param callback The work to execute on the next idle tick.
 */
data class IdleCallbackData(
    val id: Int,
    val callback: () -> Unit
)

// ---------------------------------------------------------------------------
// AppViewer singleton  (mirrors LLAppViewer)
// ---------------------------------------------------------------------------

/**
 * Main application lifecycle singleton, equivalent to LLAppViewer.
 *
 * Complex subsystems (texture cache, network pump, joystick, etc.) are
 * stubbed with TODO() because they depend on platform-specific and
 * OpenGL-dependent infrastructure not yet ported.
 */
object AppViewer {

    // -----------------------------------------------------------------------
    // State flags
    // -----------------------------------------------------------------------

    /** True once [requestQuit] or [forceQuit] has been called. */
    var isQuitting: Boolean = false
        private set

    /** True once the viewer has begun its final exit sequence. */
    var isExiting: Boolean = false
        private set

    /** True if this is a second instance of the viewer running simultaneously. */
    var isSecondInstance: Boolean = false
        private set

    var quitRequested: Boolean = false
        private set

    var logoutRequestSent: Boolean = false
        private set

    var savedFinalSnapshot: Boolean = false
        private set

    // -----------------------------------------------------------------------
    // Identifying strings
    // -----------------------------------------------------------------------

    var serialNumber: String = ""
        private set

    var serverReleaseNotesURL: String = ""

    var currentSkin: String = ""
    var currentSkinTheme: String = ""

    // -----------------------------------------------------------------------
    // Cache/cleanup flags
    // -----------------------------------------------------------------------

    var purgeCache: Boolean = false
        private set

    var purgeUserDataOnExit: Boolean = false
        private set

    var saveSettingsOnExit: Boolean = true

    // -----------------------------------------------------------------------
    // Timing globals (convenience mirrors of C++ extern globals)
    // -----------------------------------------------------------------------

    var frameCount: UInt = 0u
    var foregroundFrameCount: UInt = 0u
    var fpsClamped: Float = 0f
    var frameDtClamped: Float = 0f
    var frameTimeSeconds: Float = 0f
    var frameIntervalSeconds: Float = 0f
    var logoutMaxTime: Float = 30f
    var simLastTime: Float = 0f
    var simFrames: Float = 0f
    var disconnected: Boolean = false
    var doDisconnect: Boolean = false
    var randomizeFramerate: Boolean = false
    var periodicSlowFrame: Boolean = false

    // -----------------------------------------------------------------------
    // Version / cache constants (companion would hold static members in C++)
    // -----------------------------------------------------------------------

    companion object {
        const val GLOBAL_SETTINGS_NAME: String = "Global"
        const val WINDOW_CLASS: String = "Second Life"

        fun getTextureCacheVersion(): UInt = TODO("texture cache versioning not yet ported")
        fun getObjectCacheVersion(): UInt  = TODO("object cache versioning not yet ported")
        fun getDiskCacheVersion(): UInt    = TODO("disk cache versioning not yet ported")
    }

    // -----------------------------------------------------------------------
    // Idle callbacks
    // -----------------------------------------------------------------------

    private val idleCallbacks: MutableList<IdleCallbackData> = mutableListOf()
    private var nextCallbackId: Int = 0

    /**
     * Registers [callback] to be executed once on the next idle tick.
     * Returns a stable ID that can be passed to [removeOnIdleCallback].
     */
    fun addOnIdleCallback(callback: () -> Unit): Int {
        val id = nextCallbackId++
        idleCallbacks.add(IdleCallbackData(id, callback))
        return id
    }

    /** Removes the idle callback registered under [id], if still present. */
    fun removeOnIdleCallback(id: Int) {
        idleCallbacks.removeAll { it.id == id }
    }

    /** Drains and executes all pending one-shot idle callbacks. */
    fun fireIdleCallbacks() {
        val snapshot = idleCallbacks.toList()
        idleCallbacks.clear()
        for (entry in snapshot) {
            entry.callback()
        }
    }

    // -----------------------------------------------------------------------
    // Login-completed signal
    // -----------------------------------------------------------------------

    private val loginCompletedListeners: MutableList<() -> Unit> = mutableListOf()

    fun setOnLoginCompletedCallback(cb: () -> Unit) {
        loginCompletedListeners.add(cb)
    }

    fun handleLoginComplete() {
        TODO("login-complete handling not yet ported")
    }

    // -----------------------------------------------------------------------
    // Main application lifecycle stubs
    // -----------------------------------------------------------------------

    /**
     * Application initialisation entry point.
     * Mirrors `LLAppViewer::init()`.
     */
    fun init(): Boolean {
        TODO("Application init not yet ported")
    }

    /**
     * Per-frame body logic.
     * Mirrors `LLAppViewer::frame()`.
     */
    fun mainLoop(): Boolean {
        TODO("Main loop frame not yet ported")
    }

    /** Graceful quit request; the user may be prompted to confirm. */
    fun requestQuit() {
        quitRequested = true
        isQuitting = true
    }

    /** Immediate, unconditional shutdown without error state. */
    fun forceQuit() {
        isQuitting = true
        isExiting = true
    }

    /** Faster quit that first sends a logout message. */
    fun fastQuit(errorCode: Int = 0) {
        TODO("fastQuit not yet ported (errorCode=$errorCode)")
    }

    /** Interactive quit — prompt the user, then call [requestQuit]. */
    fun userQuit() {
        TODO("userQuit dialog not yet ported")
    }

    /** Abort a pending quit request. */
    fun abortQuit() {
        quitRequested = false
        isQuitting = false
    }

    /** Display an error dialog and forcibly quit. */
    fun earlyExit(name: String, substitutions: Map<String, String> = emptyMap()): Nothing {
        TODO("earlyExit dialog not yet ported (name=$name)")
    }

    /** Forcibly quit without showing a dialog. */
    fun earlyExitNoNotify(): Nothing {
        TODO("earlyExitNoNotify not yet ported")
    }

    // -----------------------------------------------------------------------
    // Misc. application state
    // -----------------------------------------------------------------------

    fun getSecondLifeTitle(): String = TODO("title not yet ported")
    fun getWindowTitle(): String     = TODO("window title not yet ported")

    fun forceDisconnect(msg: String) {
        TODO("forceDisconnect not yet ported (msg=$msg)")
    }

    fun sendSimpleLogoutRequest() {
        TODO("logout request not yet ported")
    }

    fun saveFinalSnapshot() {
        TODO("saveFinalSnapshot not yet ported")
    }

    fun purgeUserDataOnExit() {
        purgeUserDataOnExit = true
    }

    fun loadNameCache() { TODO("name cache load not yet ported") }
    fun saveNameCache() { TODO("name cache save not yet ported") }

    fun removeMarkerFiles()    { TODO("marker files not yet ported") }
    fun recordSessionToMarker(){ TODO("session marker not yet ported") }

    fun outOfMemorySoftQuit()  { TODO("OOM soft quit not yet ported") }

    // -----------------------------------------------------------------------
    // Mainloop timeout helpers
    // -----------------------------------------------------------------------

    fun initMainloopTimeout(state: String)  { TODO("mainloop timeout not yet ported") }
    fun destroyMainloopTimeout()            { TODO("mainloop timeout not yet ported") }
    fun pauseMainloopTimeout()              { TODO("mainloop timeout not yet ported") }
    fun resumeMainloopTimeout(state: String = "") { TODO("mainloop timeout not yet ported") }
    fun pingMainloopTimeout(state: String)  { TODO("mainloop timeout not yet ported") }
    fun getMainloopTimeoutSec(): Float      = TODO("mainloop timeout not yet ported")

    // -----------------------------------------------------------------------
    // Settings helpers
    // -----------------------------------------------------------------------

    fun loadSettingsFromDirectory(locationKey: String, setDefaults: Boolean = false): Boolean {
        TODO("settings loading not yet ported (key=$locationKey)")
    }

    fun getSettingsFilename(locationKey: String, file: String): String {
        TODO("settings filename not yet ported")
    }

    fun loadColorSettings() { TODO("color settings not yet ported") }
    fun loadKeyBindings()   { TODO("key bindings not yet ported") }

    // -----------------------------------------------------------------------
    // Debug / crash helpers
    // -----------------------------------------------------------------------

    fun writeDebugInfo(isStatic: Boolean = true) { TODO("debug info not yet ported") }
    fun checkForCrash()                          { TODO("crash check not yet ported") }
    fun badNetworkHandler()                      { TODO("bad network handler not yet ported") }
    fun writeSystemInfo()                        { TODO("system info not yet ported") }

    // -----------------------------------------------------------------------
    // Audio
    // -----------------------------------------------------------------------

    fun setMasterSystemAudioMute(mute: Boolean) { TODO("audio mute not yet ported") }
    fun getMasterSystemAudioMute(): Boolean     = TODO("audio mute not yet ported")

    // -----------------------------------------------------------------------
    // Metrics
    // -----------------------------------------------------------------------

    fun metricsUpdateRegion(regionHandle: ULong) { TODO("metrics not yet ported") }
    fun metricsSend(enableReporting: Boolean)    { TODO("metrics not yet ported") }

    // -----------------------------------------------------------------------
    // ServerReleaseNotesURLFetcher inner object (mirrors nested class)
    // -----------------------------------------------------------------------

    /**
     * Fetches the server release-notes URL asynchronously.
     * Mirrors `LLAppViewer::LLAppViewer::ServerReleaseNotesURLFetcher`.
     */
    object ServerReleaseNotesURLFetcher {
        fun fetchURL(url: String) {
            TODO("ServerReleaseNotesURLFetcher not yet ported (url=$url)")
        }
    }
}
