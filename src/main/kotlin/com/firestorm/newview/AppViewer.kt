package com.firestorm.newview

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
    LOGOUT_UNKNOWN
}

var gLastExecEvent: LastExecEvent = LastExecEvent.NORMAL
var gLastExecDuration: Int = -1
var gLastAgentSessionId: String = ""

var gDebugInfo: MutableMap<String, Any> = mutableMapOf()
var gShowObjectUpdates: Boolean = false

var gFrameCount: UInt = 0u
var gForegroundFrameCount: UInt = 0u
var gFrameTime: Long = 0L
var gFrameTimeSeconds: Float = 0f
var gFrameIntervalSeconds: Float = 0f
var gFpsClamped: Float = 10f
var gFrameDtClamped: Float = 0f
var gStartTime: Long = 0L
var gLogoutMaxTime: Float = 6f
var gPendingMetricsUploads: Int = 0
var gSimLastTime: Float = 0f
var gSimFrames: Float = 0f
var gDisconnected: Boolean = false
var gRestoreGl: Boolean = false
var gUseWireframe: Boolean = false
var gMemoryAllocated: Long = 0L
var gLastVersionChannel: String = ""
var gWindVec: FloatArray = floatArrayOf(3f, 3f, 0f)
var gRelativeWindVec: FloatArray = floatArrayOf(0f, 0f, 0f)
var gRandomizeFramerate: Boolean = false
var gPeriodicSlowFrame: Boolean = false
var gDoDisconnect: Boolean = false
var gSimulateMemLeak: Boolean = false

private const val LOGOUT_REQUEST_TIME = 6f

abstract class AppViewerBase {

    var isSecondInstance: Boolean = false
        protected set

    var quitRequested: Boolean = false
        protected set

    var closingFloaters: Boolean = false
        protected set

    var logoutRequestSent: Boolean = false
        protected set

    var savedFinalSnapshot: Boolean = false
        protected set

    var savePerAccountSettings: Boolean = false
        protected set

    var serialNumber: String = ""
        protected set

    var serverReleaseNotesURL: String = ""

    var currentSkin: String = ""
    var currentSkinTheme: String = ""

    var purgeCache: Boolean = false
        protected set

    var purgeCacheOnExit: Boolean = false
        protected set

    var purgeUserDataOnExit: Boolean = false
        protected set

    var purgeSettings: Boolean = false
        protected set

    var purgeTextures: Boolean = false
        protected set

    var saveSettingsOnExit: Boolean = true

    var isFirstRun: Boolean = false
        protected set

    var numSessions: Int = 0
        protected set

    private val idleCallbacks: MutableList<() -> Unit> = mutableListOf()
    val onLoginCompleted: MutableList<() -> Unit> = mutableListOf()

    abstract fun init(): Boolean
    abstract fun cleanup(): Boolean
    abstract fun frame(): Boolean
    abstract fun restoreErrorTrap(): Boolean
    abstract fun generateSerialNumber(): String

    open fun beingDebugged(): Boolean = false
    open fun initWindow(): Boolean = TODO("GPU: init viewer window")
    open fun initLoggingAndGetLastDuration() {}
    open fun initConsole() {}
    open fun initHardwareTest(): Boolean = true
    open fun overrideDetectedHardware() {}
    open fun initSLURLHandler(): Boolean = false
    open fun sendURLToOtherInstance(url: String): Boolean = false
    open fun meetsRequirementsForMaximizedStart(): Boolean = false
    open fun sendOutOfDiskSpaceNotification() {}
    open fun startCachePurge() {}

    fun forceQuit() {
        TODO("APR: use JVM equivalent - set mQuitRequested / exit immediately")
    }

    fun fastQuit(errorCode: Int = 0) {
        TODO("APR: use JVM equivalent - send logout message then exit($errorCode)")
    }

    fun requestQuit() {
        quitRequested = true
    }

    fun userQuit() {
        TODO("GPU: show confirm-quit dialog, then requestQuit()")
    }

    fun earlyExit(name: String, substitutions: Map<String, String> = emptyMap()): Nothing {
        TODO("GPU: show error dialog for $name then force-exit")
    }

    fun earlyExitNoNotify(): Nothing {
        TODO("APR: use JVM equivalent - force-exit without dialog")
    }

    fun abortQuit() {
        quitRequested = false
    }

    fun flushLFSIO() {
        TODO("APR: use JVM equivalent - wait for LFS transfers to complete")
    }

    fun writeDebugInfo(isStatic: Boolean = true) {
        TODO("APR: use JVM equivalent - write static_debug_info.log")
    }

    fun getViewerInfo(): Map<String, Any> = TODO("APR: use JVM equivalent - collect version/platform/GPU info")

    fun getViewerInfoString(defaultString: Boolean = false): String = TODO("APR: use JVM equivalent - format viewer info")

    fun checkForCrash() {
        TODO("APR: use JVM equivalent - process marker files from previous run")
    }

    fun getSerialNumber(): String = serialNumber

    fun getPurgeCache(): Boolean = purgeCache

    fun getSecondLifeTitle(): String = TODO("APR: use JVM equivalent - LLVersionInfo channel/version string")

    fun getWindowTitle(): String = TODO("APR: use JVM equivalent - gWindowTitle")

    fun forceDisconnect(msg: String) {
        TODO("APR: use JVM equivalent - show DisconnectedRegion notification and set gDoDisconnect")
    }

    fun sendSimpleLogoutRequest() {
        TODO("APR: use JVM equivalent - send LogoutRequest UDP message")
    }

    fun badNetworkHandler() {
        TODO("APR: use JVM equivalent - trigger crash for bad network packet")
    }

    fun hasSavedFinalSnapshot(): Boolean = savedFinalSnapshot

    fun saveFinalSnapshot() {
        TODO("GPU: take final screenshot to screen_last.png")
    }

    fun loadNameCache() {
        TODO("APR: use JVM equivalent - load name cache from disk")
    }

    fun saveNameCache() {
        TODO("APR: use JVM equivalent - save name cache to disk")
    }

    fun removeMarkerFiles() {
        TODO("APR: use JVM equivalent - delete .exec_marker and .logout_marker files")
    }

    fun recordSessionToMarker() {
        TODO("APR: use JVM equivalent - write session info to marker file")
    }

    fun removeDumpDir() {
        TODO("APR: use JVM equivalent - remove minidump directory")
    }

    fun forceErrorLLError() { TODO("APR: trigger LL_ERRS for testing") }
    fun forceErrorLLErrorMsg() { TODO("APR: trigger LL_ERRS with message for testing") }
    fun forceErrorBreakpoint() { TODO("APR: raise SIGTRAP for testing") }
    fun forceErrorBadMemoryAccess() { TODO("APR: dereference null for testing") }
    fun forceErrorInfiniteLoop() { TODO("APR: enter infinite loop for testing") }
    fun forceErrorSoftwareException() { TODO("APR: throw unhandled exception for testing") }
    fun forceErrorOSSpecificException() { TODO("APR: raise OS exception for testing") }
    fun forceErrorDriverCrash() { TODO("GPU: provoke GPU driver crash for testing") }
    fun forceErrorCoroprocedureCrash() { TODO("APR: crash a coroproc for testing") }
    fun forceErrorWorkQueueCrash() { TODO("APR: crash work queue for testing") }
    fun forceErrorThreadCrash() { TODO("APR: crash background thread for testing") }
    fun forceExceptionThreadCrash() { TODO("APR: throw from thread for testing") }

    fun loadSettingsFromDirectory(locationKey: String, setDefaults: Boolean = false): Boolean {
        TODO("APR: use JVM equivalent - load settings XML from location_key path")
    }

    fun getSettingsFilename(locationKey: String, file: String): String {
        TODO("APR: use JVM equivalent - resolve settings file path from location key")
    }

    fun loadColorSettings() {
        TODO("APR: use JVM equivalent - load colors.xml into LLUIColorTable")
    }

    fun initMainloopTimeout(state: String) {
        TODO("APR: use JVM equivalent - start watchdog timer for $state")
    }

    fun destroyMainloopTimeout() {
        TODO("APR: use JVM equivalent - stop watchdog timer")
    }

    fun pauseMainloopTimeout() {
        TODO("APR: use JVM equivalent - pause watchdog timer")
    }

    fun resumeMainloopTimeout(state: String = "") {
        TODO("APR: use JVM equivalent - resume watchdog timer for $state")
    }

    fun pingMainloopTimeout(state: String) {
        TODO("APR: use JVM equivalent - ping watchdog timer for $state")
    }

    fun getMainloopTimeoutSec(): Float = TODO("APR: use JVM equivalent - return watchdog timeout seconds")

    fun handleLoginComplete() {
        for (cb in onLoginCompleted) cb()
        TODO("APR: use JVM equivalent - finish post-login setup")
    }

    fun setOnLoginCompletedCallback(cb: () -> Unit) {
        onLoginCompleted.add(cb)
    }

    fun addOnIdleCallback(cb: () -> Unit) {
        idleCallbacks.add(cb)
    }

    fun fireIdleCallbacks() {
        val snapshot = idleCallbacks.toList()
        idleCallbacks.clear()
        snapshot.forEach { it() }
    }

    fun initGeneralThread() {
        TODO("APR: use JVM equivalent - start general-purpose thread pool")
    }

    fun purgeUserDataOnExit() {
        purgeUserDataOnExit = true
    }

    fun purgeCefStaleCaches() {
        TODO("APR: use JVM equivalent - remove stale CEF cache folders in background")
    }

    fun purgeCache() {
        TODO("APR: use JVM equivalent - clear local disk cache")
    }

    fun purgeCacheImmediate() {
        TODO("APR: use JVM equivalent - synchronously clear local disk cache")
    }

    fun updateTextureThreads(maxTimeSec: Float): Int = TODO("GPU: pump texture decode/fetch threads")

    fun loadKeyBindings() {
        TODO("APR: use JVM equivalent - load key_bindings.xml")
    }

    open fun setMasterSystemAudioMute(mute: Boolean) {
        TODO("APR: use JVM equivalent - mute/unmute OS audio")
    }

    open fun getMasterSystemAudioMute(): Boolean = TODO("APR: use JVM equivalent - query OS audio mute state")

    fun getAppCoreHttp(): Any = TODO("APR: use JVM equivalent - return LLAppCoreHttp instance")

    fun updateNameLookupUrl(region: Any?) {
        TODO("APR: use JVM equivalent - update avatar name lookup URL from region capabilities")
    }

    fun postToMainCoro(work: () -> Unit) {
        TODO("APR: use JVM equivalent - enqueue work on main-thread WorkQueue")
    }

    fun createErrorMarker(errorCode: LastExecEvent) {
        TODO("APR: use JVM equivalent - write $errorCode to .error_marker file")
    }

    fun errorMarkerExists(): Boolean = TODO("APR: use JVM equivalent - check for .error_marker file")

    fun outOfMemorySoftQuit() {
        TODO("APR: use JVM equivalent - attempt soft quit on OOM condition")
    }

    fun setSaveSettingsOnExit(state: Boolean) {
        saveSettingsOnExit = state
    }

    companion object {
        var instance: AppViewerBase? = null
            private set

        const val GLOBAL_SETTINGS_NAME: String = "Global"
        const val WINDOW_CLASS: String = "Second Life"

        fun getTextureCacheVersion(): UInt = TODO("APR: texture cache version constant")
        fun getObjectCacheVersion(): UInt  = TODO("APR: object cache version constant")
        fun getDiskCacheVersion(): UInt    = TODO("APR: disk cache version constant")

        fun metricsUpdateRegion(regionHandle: ULong) {
            TODO("APR: use JVM equivalent - update metrics region handle")
        }

        fun metricsSend(enableReporting: Boolean) {
            TODO("APR: use JVM equivalent - flush viewer metrics to server")
        }

        internal fun setInstance(viewer: AppViewerBase) {
            check(instance == null) { "AppViewerBase instance already set" }
            instance = viewer
        }
    }
}

object AppViewer : AppViewerBase() {

    init {
        setInstance(this)
    }

    override fun init(): Boolean {
        TODO("APR: use JVM equivalent - full application initialisation sequence")
    }

    override fun cleanup(): Boolean {
        TODO("APR: use JVM equivalent - full application cleanup sequence")
    }

    override fun frame(): Boolean {
        TODO("GPU: execute one frame: idle callbacks, network pump, render")
    }

    override fun restoreErrorTrap(): Boolean {
        TODO("APR: use JVM equivalent - platform-specific error handler reset")
    }

    override fun generateSerialNumber(): String {
        TODO("APR: use JVM equivalent - platform-specific machine serial number")
    }
}
