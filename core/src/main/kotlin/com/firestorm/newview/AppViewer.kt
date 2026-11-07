package com.firestorm.newview

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread

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

// ── internal helpers ─────────────────────────────────────────────────────────

/** Return the user-data root for the viewer (mirrors gDirUtilp->getOSUserAppDir()). */
private fun appDataDir(): File {
    val base = System.getProperty("user.home") ?: "."
    return File(base, ".firestorm").also { it.mkdirs() }
}

/** Resolve a settings-location key to a directory on disk. */
private fun locationKeyToDir(locationKey: String): File {
    return when (locationKey.lowercase()) {
        "default" -> File(appDataDir(), "app_settings")
        "user"    -> File(appDataDir(), "user_settings")
        "account" -> File(appDataDir(), "per_account")
        else      -> File(appDataDir(), locationKey.lowercase())
    }.also { it.mkdirs() }
}

// ── abstract base ─────────────────────────────────────────────────────────────

abstract class AppViewerBase {

    // ── public state ──────────────────────────────────────────────────────────

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

    // ── private / protected infrastructure ────────────────────────────────────

    protected val idleCallbacks: MutableList<() -> Unit> = mutableListOf()
    val onLoginCompleted: MutableList<() -> Unit> = mutableListOf()

    /** Work queue for tasks to be processed on the "main" thread. */
    protected val mainloopQueue: ArrayBlockingQueue<() -> Unit> =
        ArrayBlockingQueue(1_048_576)

    /** Shared general-purpose thread pool (mirrors mGeneralThreadPool). */
    private var generalThreadPool: ScheduledExecutorService? = null

    /** Watchdog / mainloop-timeout support. */
    private var watchdogExecutor: ScheduledExecutorService? = null
    private var watchdogFuture: ScheduledFuture<*>? = null
    @Volatile private var watchdogState: String = ""
    @Volatile private var watchdogLastPing: Long = System.currentTimeMillis()
    @Volatile private var watchdogTimeoutSec: Float = 60f
    @Volatile private var watchdogPaused: Boolean = false

    /** JVM logger (mirrors LLError / LL_INFOS). */
    protected val log: Logger = Logger.getLogger(javaClass.name)

    // ── abstract interface ────────────────────────────────────────────────────

    abstract fun init(): Boolean
    abstract fun cleanup(): Boolean
    abstract fun frame(): Boolean
    abstract fun restoreErrorTrap(): Boolean
    abstract fun generateSerialNumber(): String

    // ── virtual / open hooks (GPU / platform) ─────────────────────────────────

    open fun beingDebugged(): Boolean = false
    open fun initWindow(): Boolean {
        // GPU: platform provides a real window; here we record the intent only.
        log.info("initWindow: viewer window initialisation requested (GPU subsystem not available in JVM build)")
        return true
    }
    open fun initLoggingAndGetLastDuration() {}
    open fun initConsole() {}
    open fun initHardwareTest(): Boolean = true
    open fun overrideDetectedHardware() {}
    open fun initSLURLHandler(): Boolean = false
    open fun sendURLToOtherInstance(url: String): Boolean = false
    open fun meetsRequirementsForMaximizedStart(): Boolean = false
    open fun sendOutOfDiskSpaceNotification() {
        log.warning("sendOutOfDiskSpaceNotification: disk space exhausted, notifying user")
    }
    open fun startCachePurge() {}

    // ── application control ───────────────────────────────────────────────────

    /**
     * Puts the viewer into "shutting down without error" mode.
     * Mirrors LLAppViewer::forceQuit() which calls apr_pool_destroy / LLApp::setQuitting().
     */
    fun forceQuit() {
        log.info("forceQuit: requesting JVM shutdown")
        quitRequested = true
        // Register a shutdown hook so any in-flight cleanup still runs.
        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "forceQuit-hook") {
            log.info("forceQuit-hook: shutdown hook executing")
        })
        Runtime.getRuntime().halt(0)
    }

    /**
     * Sends a logout message then exits immediately with [errorCode].
     * Mirrors LLAppViewer::fastQuit().
     */
    fun fastQuit(errorCode: Int = 0) {
        log.info("fastQuit($errorCode): sending logout request then exiting")
        try {
            sendSimpleLogoutRequest()
        } catch (t: Throwable) {
            log.log(Level.WARNING, "fastQuit: error during sendSimpleLogoutRequest", t)
        }
        Runtime.getRuntime().halt(errorCode)
    }

    fun requestQuit() {
        quitRequested = true
    }

    /**
     * Shows a confirm-quit dialog (headless: logs a warning) then calls requestQuit().
     * GPU-side callers should override to display the actual dialog.
     */
    fun userQuit() {
        log.info("userQuit: user requested quit – no GPU dialog available, calling requestQuit()")
        requestQuit()
    }

    /**
     * Displays an error dialog (headless: logs + throws) then force-exits.
     * Mirrors LLAppViewer::earlyExit().
     */
    fun earlyExit(name: String, substitutions: Map<String, String> = emptyMap()): Nothing {
        val msg = substitutions.entries.fold(name) { acc, (k, v) -> acc.replace("[$k]", v) }
        log.severe("earlyExit: $msg")
        writeDebugInfo(isStatic = true)
        Runtime.getRuntime().halt(1)
        // unreachable – satisfies Kotlin's Nothing return type
        throw IllegalStateException("earlyExit reached unreachable code")
    }

    /**
     * Force-exits without displaying any dialog.
     * Mirrors LLAppViewer::earlyExitNoNotify().
     */
    fun earlyExitNoNotify(): Nothing {
        log.severe("earlyExitNoNotify: forcing JVM exit without user notification")
        Runtime.getRuntime().halt(1)
        throw IllegalStateException("earlyExitNoNotify reached unreachable code")
    }

    fun abortQuit() {
        quitRequested = false
    }

    /**
     * Waits for large-file-system (LFS) transfers to complete.
     * JVM equivalent: drain the mainloop work queue and flush any pending I/O threads.
     */
    fun flushLFSIO() {
        log.fine("flushLFSIO: draining mainloop work queue")
        val pending = mutableListOf<() -> Unit>()
        mainloopQueue.drainTo(pending)
        pending.forEach { work ->
            try { work() } catch (t: Throwable) {
                log.log(Level.WARNING, "flushLFSIO: error executing pending work", t)
            }
        }
    }

    /**
     * Writes diagnostic state to [appDataDir]/logs/static_debug_info.log.
     * Mirrors LLAppViewer::writeDebugInfo().
     */
    fun writeDebugInfo(isStatic: Boolean = true) {
        val logDir = File(appDataDir(), "logs").also { it.mkdirs() }
        val fileName = if (isStatic) "static_debug_info.log" else "dynamic_debug_info.log"
        val target = File(logDir, fileName)
        try {
            target.bufferedWriter().use { w ->
                w.write("# Firestorm debug info  isStatic=$isStatic\n")
                w.write("timestamp=${System.currentTimeMillis()}\n")
                w.write("javaVersion=${System.getProperty("java.version")}\n")
                w.write("os=${System.getProperty("os.name")} ${System.getProperty("os.version")}\n")
                w.write("arch=${System.getProperty("os.arch")}\n")
                w.write("freeMemory=${Runtime.getRuntime().freeMemory()}\n")
                w.write("maxMemory=${Runtime.getRuntime().maxMemory()}\n")
                w.write("processors=${Runtime.getRuntime().availableProcessors()}\n")
                w.write("gLastExecEvent=$gLastExecEvent\n")
                w.write("gLastExecDuration=$gLastExecDuration\n")
                w.write("gLastAgentSessionId=$gLastAgentSessionId\n")
                for ((k, v) in gDebugInfo) {
                    w.write("$k=$v\n")
                }
            }
            log.fine("writeDebugInfo: wrote $target")
        } catch (e: Exception) {
            log.log(Level.WARNING, "writeDebugInfo: failed to write $target", e)
        }
    }

    /**
     * Collects version/platform/JVM info, mirroring LLAppViewer::getViewerInfo().
     */
    fun getViewerInfo(): Map<String, Any> {
        val rt = Runtime.getRuntime()
        return mapOf(
            "VIEWER_CHANNEL"  to (gLastVersionChannel.ifEmpty { "Firestorm" }),
            "JAVA_VERSION"    to (System.getProperty("java.version") ?: "unknown"),
            "JVM_VENDOR"      to (System.getProperty("java.vendor") ?: "unknown"),
            "OS_NAME"         to (System.getProperty("os.name") ?: "unknown"),
            "OS_VERSION"      to (System.getProperty("os.version") ?: "unknown"),
            "OS_ARCH"         to (System.getProperty("os.arch") ?: "unknown"),
            "CPU_CORES"       to rt.availableProcessors(),
            "MAX_MEMORY_MB"   to (rt.maxMemory() / 1_048_576L),
            "FREE_MEMORY_MB"  to (rt.freeMemory() / 1_048_576L),
            "SERIAL_NUMBER"   to serialNumber,
            "SECOND_INSTANCE" to isSecondInstance,
            "NUM_SESSIONS"    to numSessions,
            "SERVER_RELEASE_NOTES_URL" to serverReleaseNotesURL
        )
    }

    /**
     * Formats [getViewerInfo] as a human-readable string.
     * Mirrors LLAppViewer::getViewerInfoString().
     */
    fun getViewerInfoString(defaultString: Boolean = false): String {
        val info = getViewerInfo()
        return buildString {
            appendLine("=== Firestorm Viewer Info ===")
            for ((k, v) in info) appendLine("  $k: $v")
            appendLine("timestamp: ${java.util.Date()}")
        }
    }

    /**
     * Examines marker files from the previous run and sets [gLastExecEvent] / [gLastExecDuration].
     * Mirrors LLAppViewer::checkForCrash() / processMarkerFiles().
     */
    fun checkForCrash() {
        val logDir = File(appDataDir(), "logs")
        val markerFile   = File(logDir, "firestorm.exec_marker")
        val logoutMarker = File(logDir, "firestorm.logout_marker")
        val errorMarker  = File(logDir, "firestorm.error_marker")
        val startMarker  = File(logDir, "firestorm.start_marker")

        when {
            errorMarker.exists() -> {
                val code = try { errorMarker.readText().trim() } catch (e: Exception) { "" }
                gLastExecEvent = when (code) {
                    "FROZE"          -> LastExecEvent.FROZE
                    "LLERROR_CRASH"  -> LastExecEvent.LLERROR_CRASH
                    "BAD_ALLOC"      -> LastExecEvent.BAD_ALLOC
                    "MISSING_FILES"  -> LastExecEvent.MISSING_FILES
                    "GRAPHICS_INIT"  -> LastExecEvent.GRAPHICS_INIT
                    else             -> LastExecEvent.OTHER_CRASH
                }
                log.warning("checkForCrash: previous run ended with $gLastExecEvent (error_marker)")
                errorMarker.delete()
            }
            markerFile.exists() && !logoutMarker.exists() -> {
                gLastExecEvent = LastExecEvent.FROZE
                log.warning("checkForCrash: exec_marker present without logout_marker – previous run froze")
            }
            logoutMarker.exists() -> {
                gLastExecEvent = LastExecEvent.NORMAL
                log.fine("checkForCrash: clean logout detected")
                logoutMarker.delete()
            }
            else -> {
                gLastExecEvent = LastExecEvent.NORMAL
            }
        }

        // Estimate duration from start_marker → most recent log file mtime
        if (startMarker.exists()) {
            try {
                val logsDir  = File(appDataDir(), "logs")
                val lastLog  = logsDir.listFiles { f -> f.extension == "log" }
                    ?.maxByOrNull { it.lastModified() }
                if (lastLog != null) {
                    gLastExecDuration = ((lastLog.lastModified() - startMarker.lastModified()) / 1000L).toInt()
                }
            } catch (e: Exception) {
                log.log(Level.FINE, "checkForCrash: could not compute last exec duration", e)
                gLastExecDuration = -1
            }
        }
    }

    fun getSerialNumber(): String = serialNumber

    fun getPurgeCache(): Boolean = purgeCache

    /**
     * Returns the viewer channel + version string (mirrors LLAppViewer::getSecondLifeTitle()).
     */
    fun getSecondLifeTitle(): String {
        val channel = gLastVersionChannel.ifEmpty { "Firestorm" }
        val version = System.getProperty("firestorm.version", "7.0.0.0")
        return "$channel $version"
    }

    /**
     * Returns the window display title (mirrors gWindowTitle / LLAppViewer::getWindowTitle()).
     */
    fun getWindowTitle(): String {
        val title = getSecondLifeTitle()
        val extra = System.getProperty("firestorm.window.extra", "")
        return if (extra.isBlank()) title else "$title $extra"
    }

    /**
     * Forces a disconnect with a user-visible message.
     * Mirrors LLAppViewer::forceDisconnect().
     */
    fun forceDisconnect(msg: String) {
        log.warning("forceDisconnect: $msg")
        gDoDisconnect = true
        gDisconnected = true
        gDebugInfo["DISCONNECT_REASON"] = msg
    }

    /**
     * Attempts a best-effort UDP LogoutRequest by POSTing to a local diagnostics endpoint.
     * In a full viewer this sends an actual UDP circuit message.
     */
    fun sendSimpleLogoutRequest() {
        if (logoutRequestSent) return
        logoutRequestSent = true
        log.info("sendSimpleLogoutRequest: sending logout request")
        thread(name = "logout-request") {
            try {
                // Best-effort HTTP notification (mirrors the UDP LogoutRequest intent).
                val url = System.getProperty("firestorm.logout.url", "")
                if (url.isNotBlank()) {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 3_000
                    conn.readTimeout = 3_000
                    val body = """{"type":"LogoutRequest","agent":"$gLastAgentSessionId"}"""
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    val rc = conn.responseCode
                    log.fine("sendSimpleLogoutRequest: HTTP response $rc")
                    conn.disconnect()
                }
            } catch (e: Exception) {
                log.log(Level.FINE, "sendSimpleLogoutRequest: network error (non-fatal)", e)
            }
        }
    }

    /**
     * Triggers a crash diagnostic for a bad network packet.
     * Mirrors LLAppViewer::badNetworkHandler().
     */
    fun badNetworkHandler() {
        log.severe("badNetworkHandler: bad network packet detected – recording crash state")
        createErrorMarker(LastExecEvent.OTHER_CRASH)
        writeDebugInfo(isStatic = false)
        throw IllegalStateException("badNetworkHandler: fatal network error")
    }

    fun hasSavedFinalSnapshot(): Boolean = savedFinalSnapshot

    /**
     * Takes a final screenshot to [appDataDir]/screen_last.png (GPU stub: logs intent).
     * Mirrors LLAppViewer::saveFinalSnapshot().
     */
    fun saveFinalSnapshot() {
        if (savedFinalSnapshot) return
        savedFinalSnapshot = true
        log.info("saveFinalSnapshot: final snapshot would be written to ${appDataDir()}/screen_last.png (GPU not available)")
    }

    /**
     * Loads the avatar name cache from [appDataDir]/user_settings/name_cache.properties.
     * Mirrors LLAppViewer::loadNameCache().
     */
    fun loadNameCache() {
        val cacheFile = File(File(appDataDir(), "user_settings"), "name_cache.properties")
        if (!cacheFile.exists()) {
            log.fine("loadNameCache: no name cache file found at $cacheFile")
            return
        }
        try {
            val props = Properties()
            FileInputStream(cacheFile).use { props.load(it) }
            for ((k, v) in props) {
                gDebugInfo["name_cache_$k"] = v as String
            }
            log.fine("loadNameCache: loaded ${props.size} entries from $cacheFile")
        } catch (e: Exception) {
            log.log(Level.WARNING, "loadNameCache: failed to load $cacheFile", e)
        }
    }

    /**
     * Saves the avatar name cache to [appDataDir]/user_settings/name_cache.properties.
     * Mirrors LLAppViewer::saveNameCache().
     */
    fun saveNameCache() {
        val settingsDir = File(appDataDir(), "user_settings").also { it.mkdirs() }
        val cacheFile   = File(settingsDir, "name_cache.properties")
        try {
            val props = Properties()
            for ((k, v) in gDebugInfo) {
                if (k.startsWith("name_cache_")) props[k.removePrefix("name_cache_")] = v.toString()
            }
            FileOutputStream(cacheFile).use { props.store(it, "Firestorm name cache") }
            log.fine("saveNameCache: saved ${props.size} entries to $cacheFile")
        } catch (e: Exception) {
            log.log(Level.WARNING, "saveNameCache: failed to save $cacheFile", e)
        }
    }

    /**
     * Deletes the exec-marker and logout-marker files.
     * Mirrors LLAppViewer::removeMarkerFiles().
     */
    fun removeMarkerFiles() {
        val logDir = File(appDataDir(), "logs").also { it.mkdirs() }
        listOf("firestorm.exec_marker", "firestorm.logout_marker").forEach { name ->
            val f = File(logDir, name)
            if (f.exists()) {
                f.delete()
                log.fine("removeMarkerFiles: deleted $f")
            }
        }
    }

    /**
     * Writes session info (session-id, version) to the exec-marker file.
     * Mirrors LLAppViewer::recordSessionToMarker() / recordMarkerVersion().
     */
    fun recordSessionToMarker() {
        val logDir = File(appDataDir(), "logs").also { it.mkdirs() }
        val marker = File(logDir, "firestorm.exec_marker")
        try {
            marker.bufferedWriter().use { w ->
                w.write("version=${System.getProperty("firestorm.version", "7.0.0.0")}\n")
                w.write("channel=${gLastVersionChannel.ifEmpty { "Firestorm" }}\n")
                w.write("session=$gLastAgentSessionId\n")
                w.write("timestamp=${System.currentTimeMillis()}\n")
            }
            log.fine("recordSessionToMarker: wrote $marker")
        } catch (e: Exception) {
            log.log(Level.WARNING, "recordSessionToMarker: failed to write $marker", e)
        }
    }

    /**
     * Removes the minidump / crash-dump directory.
     * Mirrors LLAppViewer::removeDumpDir().
     */
    fun removeDumpDir() {
        val dumpDir = File(appDataDir(), "dump")
        if (dumpDir.exists() && dumpDir.isDirectory) {
            dumpDir.walkBottomUp().forEach { it.delete() }
            log.fine("removeDumpDir: removed $dumpDir")
        }
    }

    // ── forced-error test helpers ─────────────────────────────────────────────

    /** Triggers a JVM Error for testing crash-reporter integration (mirrors LL_ERRS). */
    fun forceErrorLLError() {
        log.severe("forceErrorLLError: deliberately throwing Error for crash test")
        throw Error("forceErrorLLError: intentional LL_ERRS equivalent")
    }

    /** Triggers a JVM Error with a message for testing (mirrors LL_ERRS with message). */
    fun forceErrorLLErrorMsg() {
        log.severe("forceErrorLLErrorMsg: deliberately throwing Error with message")
        throw Error("forceErrorLLErrorMsg: intentional LL_ERRS-with-message equivalent")
    }

    /** Signals SIGTRAP equivalent: sets a JVM breakpoint flag and throws (mirrors SIGTRAP). */
    fun forceErrorBreakpoint() {
        log.severe("forceErrorBreakpoint: SIGTRAP equivalent – raising AssertionError")
        throw AssertionError("forceErrorBreakpoint: intentional SIGTRAP equivalent")
    }

    /** Dereferences a null pointer (JVM: NullPointerException) for testing. */
    fun forceErrorBadMemoryAccess() {
        log.severe("forceErrorBadMemoryAccess: dereferencing null for crash test")
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val s: String? = null
        s!!.length // throws NullPointerException
    }

    /** Enters an infinite loop for testing the watchdog / deadlock detector. */
    fun forceErrorInfiniteLoop() {
        log.severe("forceErrorInfiniteLoop: entering infinite loop for crash test")
        @Suppress("ControlFlowWithEmptyBody")
        while (true) { /* intentional */ }
    }

    /** Throws an unhandled RuntimeException for testing the UncaughtExceptionHandler. */
    fun forceErrorSoftwareException() {
        log.severe("forceErrorSoftwareException: throwing unhandled RuntimeException")
        throw RuntimeException("forceErrorSoftwareException: intentional software exception")
    }

    /** Simulates an OS-level exception via a JVM StackOverflowError. */
    fun forceErrorOSSpecificException() {
        log.severe("forceErrorOSSpecificException: inducing StackOverflowError")
        forceErrorOSSpecificException() // intentional infinite recursion
    }

    /** GPU driver crash simulation: logs intent (actual GPU crash requires native code). */
    fun forceErrorDriverCrash() {
        log.severe("forceErrorDriverCrash: GPU driver crash requested (JVM stub – native call not available)")
        throw UnsupportedOperationException("forceErrorDriverCrash: GPU driver crash not reachable from JVM")
    }

    /** Crashes a coroproc-equivalent background thread. */
    fun forceErrorCoroprocedureCrash() {
        log.severe("forceErrorCoroprocedureCrash: crashing background coroproc thread")
        thread(name = "coroproc-crash-test") {
            throw RuntimeException("forceErrorCoroprocedureCrash: intentional coroproc crash")
        }
    }

    /** Crashes the mainloop work queue by poisoning it. */
    fun forceErrorWorkQueueCrash() {
        log.severe("forceErrorWorkQueueCrash: injecting exception into work queue")
        postToMainCoro { throw RuntimeException("forceErrorWorkQueueCrash: intentional work queue crash") }
    }

    /** Crashes a background thread for testing the UncaughtExceptionHandler. */
    fun forceErrorThreadCrash() {
        log.severe("forceErrorThreadCrash: crashing background thread")
        thread(name = "thread-crash-test") {
            error("forceErrorThreadCrash: intentional background thread crash")
        }
    }

    /** Throws from a background thread to test cross-thread exception propagation. */
    fun forceExceptionThreadCrash() {
        log.severe("forceExceptionThreadCrash: throwing from background thread")
        thread(name = "exception-thread-crash-test") {
            throw Exception("forceExceptionThreadCrash: intentional thread exception")
        }
    }

    // ── settings ──────────────────────────────────────────────────────────────

    /**
     * Loads settings from all XML files in the directory mapped by [locationKey].
     * Mirrors LLAppViewer::loadSettingsFromDirectory().
     */
    fun loadSettingsFromDirectory(locationKey: String, setDefaults: Boolean = false): Boolean {
        val dir = locationKeyToDir(locationKey)
        if (!dir.exists()) {
            log.warning("loadSettingsFromDirectory: directory not found for key '$locationKey': $dir")
            return false
        }
        val xmlFiles = dir.listFiles { f -> f.extension == "xml" } ?: emptyArray()
        if (xmlFiles.isEmpty()) {
            log.fine("loadSettingsFromDirectory: no XML files found in $dir")
            return true
        }
        var anyLoaded = false
        for (file in xmlFiles) {
            try {
                val props = Properties()
                // Treat each <key>value</key> pair as a property entry via Properties XML format.
                FileInputStream(file).use { props.loadFromXML(it) }
                for ((k, v) in props) {
                    if (setDefaults || !gDebugInfo.containsKey(k.toString())) {
                        gDebugInfo[k.toString()] = v as String
                    }
                }
                log.fine("loadSettingsFromDirectory: loaded ${props.size} entries from $file")
                anyLoaded = true
            } catch (e: Exception) {
                log.log(Level.WARNING, "loadSettingsFromDirectory: failed to load $file", e)
            }
        }
        return anyLoaded
    }

    /**
     * Resolves the on-disk filename for a given settings location + file name.
     * Mirrors LLAppViewer::getSettingsFilename().
     */
    fun getSettingsFilename(locationKey: String, file: String): String {
        return File(locationKeyToDir(locationKey), "$file.xml").absolutePath
    }

    /**
     * Loads colour settings from colors.xml into the colour table.
     * Mirrors LLAppViewer::loadColorSettings() → LLUIColorTable::loadFromSettings().
     */
    fun loadColorSettings() {
        val colorsFile = File(locationKeyToDir("default"), "colors.xml")
        if (!colorsFile.exists()) {
            log.fine("loadColorSettings: no colors.xml found at $colorsFile")
            return
        }
        try {
            val props = Properties()
            FileInputStream(colorsFile).use { props.loadFromXML(it) }
            for ((k, v) in props) {
                gDebugInfo["color_$k"] = v as String
            }
            log.fine("loadColorSettings: loaded ${props.size} colour entries")
        } catch (e: Exception) {
            log.log(Level.WARNING, "loadColorSettings: failed to load $colorsFile", e)
        }
    }

    // ── mainloop watchdog ─────────────────────────────────────────────────────

    /**
     * Starts the watchdog timer for the given [state].
     * Mirrors LLAppViewer::initMainloopTimeout() / LLWatchdogTimeout.
     */
    fun initMainloopTimeout(state: String) {
        log.fine("initMainloopTimeout: starting watchdog for state '$state'")
        watchdogState = state
        watchdogLastPing = System.currentTimeMillis()
        watchdogPaused = false
        val executor = Executors.newSingleThreadScheduledExecutor { r ->
            thread(start = false, isDaemon = true, name = "watchdog") { r.run() }
        }
        watchdogExecutor = executor
        watchdogFuture = executor.scheduleAtFixedRate({
            if (!watchdogPaused && !quitRequested) {
                val elapsed = (System.currentTimeMillis() - watchdogLastPing) / 1000f
                if (elapsed > watchdogTimeoutSec) {
                    log.severe("initMainloopTimeout: WATCHDOG TIMEOUT in state '$watchdogState' (${elapsed}s > ${watchdogTimeoutSec}s)")
                    createErrorMarker(LastExecEvent.FROZE)
                    writeDebugInfo(isStatic = false)
                    Runtime.getRuntime().halt(1)
                }
            }
        }, 5L, 5L, TimeUnit.SECONDS)
    }

    /**
     * Stops the watchdog timer.
     * Mirrors LLAppViewer::destroyMainloopTimeout().
     */
    fun destroyMainloopTimeout() {
        watchdogFuture?.cancel(false)
        watchdogExecutor?.shutdown()
        watchdogExecutor = null
        watchdogFuture = null
        log.fine("destroyMainloopTimeout: watchdog stopped")
    }

    /**
     * Pauses the watchdog timer (e.g. during long blocking operations).
     * Mirrors LLAppViewer::pauseMainloopTimeout().
     */
    fun pauseMainloopTimeout() {
        watchdogPaused = true
        log.finest("pauseMainloopTimeout: watchdog paused")
    }

    /**
     * Resumes the watchdog timer with an optional new [state].
     * Mirrors LLAppViewer::resumeMainloopTimeout().
     */
    fun resumeMainloopTimeout(state: String = "") {
        if (state.isNotEmpty()) watchdogState = state
        watchdogLastPing = System.currentTimeMillis()
        watchdogPaused = false
        log.finest("resumeMainloopTimeout: watchdog resumed for state '$watchdogState'")
    }

    /**
     * Resets the watchdog deadline for [state].
     * Mirrors LLAppViewer::pingMainloopTimeout().
     */
    fun pingMainloopTimeout(state: String) {
        watchdogState = state
        watchdogLastPing = System.currentTimeMillis()
        log.finest("pingMainloopTimeout: ping for state '$state'")
    }

    /**
     * Returns the current watchdog timeout in seconds.
     * Mirrors LLAppViewer::getMainloopTimeoutSec().
     */
    fun getMainloopTimeoutSec(): Float = watchdogTimeoutSec

    // ── login completion ──────────────────────────────────────────────────────

    fun handleLoginComplete() {
        for (cb in onLoginCompleted) cb()
        log.info("handleLoginComplete: post-login setup finished")
        savePerAccountSettings = true
    }

    fun setOnLoginCompletedCallback(cb: () -> Unit) {
        onLoginCompleted.add(cb)
    }

    // ── idle callbacks ────────────────────────────────────────────────────────

    fun addOnIdleCallback(cb: () -> Unit) {
        idleCallbacks.add(cb)
    }

    fun fireIdleCallbacks() {
        val snapshot = idleCallbacks.toList()
        idleCallbacks.clear()
        snapshot.forEach { it() }
    }

    // ── thread pool ───────────────────────────────────────────────────────────

    /**
     * Starts the general-purpose thread pool (mirrors LLAppViewer::initGeneralThread() /
     * mGeneralThreadPool = new LL::ThreadPool("General", 3)).
     */
    fun initGeneralThread() {
        val cores = Runtime.getRuntime().availableProcessors()
        val poolSize = maxOf(3, cores)
        generalThreadPool = Executors.newScheduledThreadPool(poolSize) { r ->
            thread(start = false, isDaemon = true, name = "general-pool") { r.run() }
        }
        // Install a JVM-wide default uncaught exception handler so crashes in
        // background threads are recorded before the process exits.
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.log(Level.SEVERE, "Uncaught exception in thread '${t.name}'", e)
            createErrorMarker(LastExecEvent.OTHER_CRASH)
            writeDebugInfo(isStatic = false)
        }
        log.info("initGeneralThread: started general-purpose thread pool (size=$poolSize)")
    }

    fun purgeUserDataOnExit() {
        purgeUserDataOnExit = true
    }

    /**
     * Removes stale CEF cache folders whose names end with "_old", in a background thread.
     * Mirrors LLAppViewer::purgeCefStaleCaches() / FSCefCachePurgeThread.
     */
    fun purgeCefStaleCaches() {
        val cefCacheDir = File(appDataDir(), "cef_cache")
        if (!cefCacheDir.exists()) return
        thread(isDaemon = true, name = "cef-cache-purge") {
            try {
                cefCacheDir.listFiles()
                    ?.filter { it.isDirectory && it.name.endsWith("_old") }
                    ?.forEach { stale ->
                        stale.walkBottomUp().forEach { f -> f.delete() }
                        log.fine("purgeCefStaleCaches: removed $stale")
                    }
            } catch (e: Exception) {
                log.log(Level.WARNING, "purgeCefStaleCaches: error during purge", e)
            }
        }
    }

    /**
     * Asynchronously clears the local disk cache directory.
     * Mirrors LLAppViewer::purgeCache().
     */
    fun purgeCache() {
        thread(isDaemon = true, name = "cache-purge") {
            purgeCacheImmediate()
        }
    }

    /**
     * Synchronously clears the local disk cache directory.
     * Mirrors LLAppViewer::purgeCacheImmediate().
     */
    fun purgeCacheImmediate() {
        val cacheDir = File(appDataDir(), "cache")
        if (!cacheDir.exists()) return
        try {
            cacheDir.walkBottomUp().filter { it != cacheDir }.forEach { it.delete() }
            log.info("purgeCacheImmediate: cleared $cacheDir")
        } catch (e: Exception) {
            log.log(Level.WARNING, "purgeCacheImmediate: error clearing cache", e)
        }
    }

    /**
     * Pumps texture decode/fetch threads for up to [maxTimeSec] seconds.
     * Returns the number of work items processed (GPU stub: returns 0).
     * Mirrors LLAppViewer::updateTextureThreads().
     */
    fun updateTextureThreads(maxTimeSec: Float): Int {
        // GPU subsystem not available in JVM build; drain any pending work-queue items.
        var processed = 0
        val deadlineNs = System.nanoTime() + (maxTimeSec * 1_000_000_000L).toLong()
        while (System.nanoTime() < deadlineNs) {
            val work = mainloopQueue.poll() ?: break
            try { work(); processed++ } catch (t: Throwable) {
                log.log(Level.WARNING, "updateTextureThreads: error in queued work", t)
            }
        }
        return processed
    }

    /**
     * Loads key bindings from [appDataDir]/user_settings/key_bindings.properties.
     * Mirrors LLAppViewer::loadKeyBindings().
     */
    fun loadKeyBindings() {
        val bindingsFile = File(File(appDataDir(), "user_settings"), "key_bindings.properties")
        if (!bindingsFile.exists()) {
            log.fine("loadKeyBindings: no key bindings file at $bindingsFile")
            return
        }
        try {
            val props = Properties()
            FileInputStream(bindingsFile).use { props.load(it) }
            for ((k, v) in props) {
                gDebugInfo["keybind_$k"] = v as String
            }
            log.fine("loadKeyBindings: loaded ${props.size} bindings from $bindingsFile")
        } catch (e: Exception) {
            log.log(Level.WARNING, "loadKeyBindings: failed to load $bindingsFile", e)
        }
    }

    // ── audio ─────────────────────────────────────────────────────────────────

    /**
     * Mutes / unmutes the OS master audio.
     * Mirrors LLAppViewer::setMasterSystemAudioMute().
     * JVM implementation: delegates to the platform CLI via ProcessBuilder.
     */
    open fun setMasterSystemAudioMute(mute: Boolean) {
        val os = System.getProperty("os.name", "").lowercase()
        try {
            when {
                os.contains("linux") -> {
                    val level = if (mute) "0%" else "100%"
                    ProcessBuilder("amixer", "-q", "sset", "Master", level)
                        .redirectErrorStream(true).start().waitFor()
                }
                os.contains("mac") -> {
                    val level = if (mute) "0" else "50"
                    ProcessBuilder("osascript", "-e", "set volume output volume $level")
                        .redirectErrorStream(true).start().waitFor()
                }
                os.contains("win") -> {
                    // Windows: use nircmd if available; otherwise log only.
                    val arg = if (mute) "mutesysvolume 1" else "mutesysvolume 0"
                    ProcessBuilder("nircmd.exe", *arg.split(" ").toTypedArray())
                        .redirectErrorStream(true).start().waitFor()
                }
            }
            gDebugInfo["audio_muted"] = mute
            log.fine("setMasterSystemAudioMute($mute): audio ${if (mute) "muted" else "unmuted"}")
        } catch (e: Exception) {
            log.log(Level.WARNING, "setMasterSystemAudioMute: could not change audio state", e)
        }
    }

    /**
     * Queries whether the OS master audio is muted.
     * Mirrors LLAppViewer::getMasterSystemAudioMute().
     */
    open fun getMasterSystemAudioMute(): Boolean {
        return gDebugInfo["audio_muted"] as? Boolean ?: false
    }

    // ── HTTP core ─────────────────────────────────────────────────────────────

    /**
     * Returns a lazily-constructed HttpURLConnection factory (mirrors LLAppCoreHttp).
     * Callers should cast the returned value to AppCoreHttp.
     */
    fun getAppCoreHttp(): Any = AppCoreHttp

    // ── region / metrics ──────────────────────────────────────────────────────

    /**
     * Updates the avatar name-lookup URL from region capabilities.
     * Mirrors LLAppViewer::updateNameLookupUrl().
     */
    fun updateNameLookupUrl(region: Any?) {
        if (region == null) {
            log.fine("updateNameLookupUrl: region is null – keeping existing URL")
            return
        }
        // In a real viewer this would call regionp->getCapability("AvatarNameLookup").
        val url = (region as? Map<*, *>)?.get("AvatarNameLookup") as? String
        if (url != null) {
            gDebugInfo["avatar_name_lookup_url"] = url
            log.fine("updateNameLookupUrl: updated to $url")
        }
    }

    /**
     * Enqueues [work] on the mainloop work queue for processing on the main thread.
     * Mirrors LLAppViewer::postToMainCoro() / gMainloopWork.post().
     */
    fun postToMainCoro(work: () -> Unit) {
        if (!mainloopQueue.offer(work)) {
            log.warning("postToMainCoro: mainloop queue is full – work dropped")
        }
    }

    // ── marker files ──────────────────────────────────────────────────────────

    /**
     * Writes [errorCode] to [appDataDir]/logs/firestorm.error_marker.
     * Mirrors LLAppViewer::createErrorMarker().
     */
    fun createErrorMarker(errorCode: LastExecEvent) {
        val logDir = File(appDataDir(), "logs").also { it.mkdirs() }
        val marker = File(logDir, "firestorm.error_marker")
        try {
            marker.writeText(errorCode.name)
            log.fine("createErrorMarker: wrote $errorCode to $marker")
        } catch (e: Exception) {
            log.log(Level.WARNING, "createErrorMarker: failed to write $marker", e)
        }
    }

    /**
     * Returns true if [appDataDir]/logs/firestorm.error_marker exists.
     * Mirrors LLAppViewer::errorMarkerExists().
     */
    fun errorMarkerExists(): Boolean {
        return File(File(appDataDir(), "logs"), "firestorm.error_marker").exists()
    }

    // ── OOM handling ──────────────────────────────────────────────────────────

    /**
     * Attempts a soft quit on OOM: disconnects, saves settings, then exits.
     * Mirrors LLAppViewer::outOfMemorySoftQuit().
     */
    fun outOfMemorySoftQuit() {
        log.severe("outOfMemorySoftQuit: OOM condition detected – attempting soft quit")
        try {
            createErrorMarker(LastExecEvent.BAD_ALLOC)
            gDoDisconnect = true
            writeDebugInfo(isStatic = false)
        } finally {
            Runtime.getRuntime().halt(1)
        }
    }

    fun setSaveSettingsOnExit(state: Boolean) {
        saveSettingsOnExit = state
    }

    // ── companion ─────────────────────────────────────────────────────────────

    companion object {
        var instance: AppViewerBase? = null
            private set

        const val GLOBAL_SETTINGS_NAME: String = "Global"
        const val WINDOW_CLASS: String = "Second Life"

        /** Texture cache version constant (mirrors LLAppViewer::getTextureCacheVersion()). */
        fun getTextureCacheVersion(): UInt = 8u

        /** Object cache version constant (mirrors LLAppViewer::getObjectCacheVersion()). */
        fun getObjectCacheVersion(): UInt = 4u

        /** Disk cache version constant (mirrors LLAppViewer::getDiskCacheVersion()). */
        fun getDiskCacheVersion(): UInt = 1u

        /**
         * Records the region handle for viewer metrics.
         * Mirrors LLAppViewer::metricsUpdateRegion().
         */
        fun metricsUpdateRegion(regionHandle: ULong) {
            gDebugInfo["metrics_region_handle"] = regionHandle.toLong()
        }

        /**
         * Flushes viewer metrics to the server via HTTP POST.
         * Mirrors LLAppViewer::metricsSend().
         */
        fun metricsSend(enableReporting: Boolean) {
            if (!enableReporting) return
            gPendingMetricsUploads++
            thread(isDaemon = true, name = "metrics-send") {
                try {
                    val endpoint = System.getProperty("firestorm.metrics.url", "")
                    if (endpoint.isBlank()) return@thread
                    val conn = URL(endpoint).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 5_000
                    conn.readTimeout = 5_000
                    val payload = buildString {
                        append("{")
                        append("\"region\":${gDebugInfo["metrics_region_handle"] ?: 0},")
                        append("\"frames\":${gFrameCount},")
                        append("\"session\":\"$gLastAgentSessionId\"")
                        append("}")
                    }
                    conn.outputStream.use { it.write(payload.toByteArray()) }
                    val rc = conn.responseCode
                    conn.disconnect()
                    gPendingMetricsUploads--
                    Logger.getLogger(AppViewerBase::class.java.name)
                        .fine("metricsSend: HTTP $rc for $endpoint")
                } catch (e: Exception) {
                    gPendingMetricsUploads--
                    Logger.getLogger(AppViewerBase::class.java.name)
                        .log(Level.WARNING, "metricsSend: failed to send metrics", e)
                }
            }
        }

        internal fun setInstance(viewer: AppViewerBase) {
            check(instance == null) { "AppViewerBase instance already set" }
            instance = viewer
        }
    }
}

// ── HTTP core singleton ────────────────────────────────────────────────────────

/**
 * Minimal JVM equivalent of LLAppCoreHttp.
 * Provides a factory for configured HttpURLConnection objects.
 */
object AppCoreHttp {
    private val log = Logger.getLogger(AppCoreHttp::class.java.name)

    var connectTimeoutMs: Int = 10_000
    var readTimeoutMs: Int    = 30_000

    fun openConnection(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = connectTimeoutMs
        conn.readTimeout    = readTimeoutMs
        return conn
    }

    fun init() {
        log.info("AppCoreHttp.init: HTTP core initialised (connectTimeout=${connectTimeoutMs}ms, readTimeout=${readTimeoutMs}ms)")
    }

    fun cleanup() {
        log.info("AppCoreHttp.cleanup: HTTP core shut down")
    }
}

// ── concrete singleton viewer ─────────────────────────────────────────────────

object AppViewer : AppViewerBase() {

    init {
        setInstance(this)
        // Install JVM shutdown hook to mirror apr_pool_destroy / LLApp cleanup.
        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "AppViewer-shutdown-hook") {
            try { cleanup() } catch (t: Throwable) {
                log.log(Level.WARNING, "shutdown hook: error during cleanup", t)
            }
        })
    }

    /**
     * Full application initialisation sequence.
     * Mirrors LLAppViewer::init().
     */
    override fun init(): Boolean {
        gStartTime = System.currentTimeMillis()
        log.info("AppViewer.init: starting Firestorm viewer")

        // Set a JVM-wide uncaught exception handler before anything else.
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.log(Level.SEVERE, "Uncaught exception in thread '${t.name}'", e)
            createErrorMarker(LastExecEvent.OTHER_CRASH)
            writeDebugInfo(isStatic = false)
        }

        // Check for previous crash.
        checkForCrash()

        // Collect basic platform info into gDebugInfo.
        gDebugInfo["java.version"]  = System.getProperty("java.version", "unknown")
        gDebugInfo["os.name"]       = System.getProperty("os.name", "unknown")
        gDebugInfo["os.arch"]       = System.getProperty("os.arch", "unknown")
        gDebugInfo["os.version"]    = System.getProperty("os.version", "unknown")
        gDebugInfo["user.home"]     = System.getProperty("user.home", "unknown")
        gDebugInfo["available.processors"] = Runtime.getRuntime().availableProcessors()
        gDebugInfo["max.memory.mb"] = Runtime.getRuntime().maxMemory() / 1_048_576L

        // Generate machine serial number.
        serialNumber = generateSerialNumber()
        gDebugInfo["serial_number"] = serialNumber

        // Initialise HTTP core.
        AppCoreHttp.init()

        // Load settings from disk.
        loadSettingsFromDirectory("default", setDefaults = true)
        loadSettingsFromDirectory("user",    setDefaults = false)

        // Load colour table.
        loadColorSettings()

        // Load key bindings.
        loadKeyBindings()

        // Load name cache.
        loadNameCache()

        // Start general-purpose thread pool.
        initGeneralThread()

        // Write a start marker so we can compute session duration on next run.
        val logDir = File(appDataDir(), "logs").also { it.mkdirs() }
        try {
            File(logDir, "firestorm.start_marker").writeText(
                "version=${System.getProperty("firestorm.version", "7.0.0.0")}\ntimestamp=${System.currentTimeMillis()}\n"
            )
        } catch (e: Exception) {
            log.log(Level.WARNING, "init: could not write start marker", e)
        }

        // Record exec marker (presence indicates viewer is running).
        recordSessionToMarker()

        // Track session count.
        numSessions++
        gDebugInfo["num_sessions"] = numSessions

        // Start watchdog.
        initMainloopTimeout("init")

        log.info("AppViewer.init: initialisation complete (serialNumber=$serialNumber)")
        return true
    }

    /**
     * Full application cleanup sequence.
     * Mirrors LLAppViewer::cleanup().
     */
    override fun cleanup(): Boolean {
        log.info("AppViewer.cleanup: beginning cleanup")

        destroyMainloopTimeout()

        // Flush any pending I/O.
        flushLFSIO()

        // Save name cache.
        saveNameCache()

        // Take a final snapshot (GPU stub).
        if (!hasSavedFinalSnapshot()) saveFinalSnapshot()

        // Send logout if we haven't yet.
        if (!logoutRequestSent) {
            try { sendSimpleLogoutRequest() } catch (t: Throwable) {
                log.log(Level.WARNING, "cleanup: sendSimpleLogoutRequest failed", t)
            }
        }

        // Purge caches if requested.
        if (purgeCacheOnExit) purgeCacheImmediate()
        purgeCefStaleCaches()
        removeDumpDir()

        // Remove marker files for a clean exit.
        removeMarkerFiles()

        // Flush HTTP core.
        AppCoreHttp.cleanup()

        log.info("AppViewer.cleanup: done")
        return true
    }

    /**
     * Executes one viewer frame: drains idle callbacks, processes queued work, pumps network.
     * Mirrors LLAppViewer::frame() / doFrame().
     */
    override fun frame(): Boolean {
        if (quitRequested) return false

        val frameStart = System.nanoTime()

        // Advance frame counters.
        gFrameCount++
        gForegroundFrameCount++
        val now = System.currentTimeMillis()
        val prevFrameTime = gFrameTime
        gFrameTime = now
        gFrameTimeSeconds = (now - gStartTime) / 1000f
        gFrameIntervalSeconds = (now - (gStartTime + (prevFrameTime - gStartTime))) / 1000f

        // Fire one-shot idle callbacks.
        fireIdleCallbacks()

        // Drain a chunk of the mainloop work queue.
        val workDeadline = frameStart + 8_000_000L // 8 ms budget
        while (System.nanoTime() < workDeadline) {
            val work = mainloopQueue.poll() ?: break
            try { work() } catch (t: Throwable) {
                log.log(Level.WARNING, "frame: error in mainloop work", t)
            }
        }

        // Ping watchdog.
        pingMainloopTimeout("frame")

        // Update frame timing globals.
        val frameDurationMs = (System.nanoTime() - frameStart) / 1_000_000f
        if (frameDurationMs > 0f) {
            gFpsClamped = (gFpsClamped * 0.9f + (1000f / frameDurationMs) * 0.1f).coerceIn(1f, 200f)
            gFrameDtClamped = frameDurationMs / 1000f
        }

        return !quitRequested
    }

    /**
     * Resets the JVM's default uncaught exception handler (platform error trap equivalent).
     * Mirrors LLAppViewer::restoreErrorTrap().
     */
    override fun restoreErrorTrap(): Boolean {
        val current = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.log(Level.SEVERE, "restoreErrorTrap: uncaught exception in '${t.name}'", e)
            createErrorMarker(LastExecEvent.OTHER_CRASH)
            writeDebugInfo(isStatic = false)
        }
        log.fine("restoreErrorTrap: error handler ${if (current == null) "installed" else "reset"}")
        return current == null // true means trap needed restoration
    }

    /**
     * Generates a platform-specific machine serial number.
     * Mirrors LLAppViewer::generateSerialNumber() on each platform.
     * JVM: derives a stable UUID from hardware properties exposed by the JVM / OS.
     */
    override fun generateSerialNumber(): String {
        val os   = System.getProperty("os.name", "")
        val arch = System.getProperty("os.arch", "")
        val home = System.getProperty("user.home", "")
        val name = System.getProperty("user.name", "")
        val seed = "$os|$arch|$home|$name"
        return UUID.nameUUIDFromBytes(seed.toByteArray()).toString()
    }
}
