package com.firestorm.llcommon

import java.util.concurrent.atomic.AtomicReference

/**
 * Application lifecycle base class, translated from llapp.h / llapp.cpp.
 *
 * Subclasses must implement [init], [mainLoop], and [cleanup].  The static
 * helper methods on [LLApp.Companion] mirror the C++ static API so that any
 * thread can query or change the global application status without holding a
 * reference to the concrete instance.
 *
 * Signal handling is mapped to JVM shutdown hooks and a simple handler map;
 * the POSIX signal numbers are preserved as Int keys for source compatibility.
 *
 * C++ counterpart: indra/llcommon/llapp.h + llapp.cpp
 */
abstract class LLApp {

    // -------------------------------------------------------------------------
    // Status enum – matches EAppStatus in the C++ source exactly, plus
    // UNINITIALIZED which is used before the app has been started.
    // -------------------------------------------------------------------------
    enum class AppStatus {
        UNINITIALIZED,  // Before init() has been called
        RUNNING,        // Normal operation
        QUITTING,       // Clean shutdown in progress
        STOPPED,        // Fully stopped
        ERROR           // Fatal error; error handler should run
    }

    // -------------------------------------------------------------------------
    // Per-instance status (kept in sync with the global companion status)
    // -------------------------------------------------------------------------
    private val _status: AtomicReference<AppStatus> = AtomicReference(AppStatus.UNINITIALIZED)

    @get:JvmName("instanceStatus")
    val status: AppStatus get() = _status.get()

    @get:JvmName("instanceIsRunning")
    val isRunning: Boolean  get() = _status.get() == AppStatus.RUNNING
    @get:JvmName("instanceIsQuitting")
    val isQuitting: Boolean get() = _status.get() == AppStatus.QUITTING
    @get:JvmName("instanceIsStopped")
    val isStopped: Boolean  get() = _status.get() == AppStatus.STOPPED
    @get:JvmName("instanceIsError")
    val isError: Boolean    get() = _status.get() == AppStatus.ERROR
    @get:JvmName("instanceIsExiting")
    val isExiting: Boolean  get() = isQuitting || isError

    // -------------------------------------------------------------------------
    // Abstract lifecycle methods (override in concrete subclasses)
    // -------------------------------------------------------------------------
    abstract fun init(): Boolean
    abstract fun mainLoop(): Boolean
    abstract fun cleanup(): Boolean

    // -------------------------------------------------------------------------
    // Signal handling
    // JVM does not expose raw POSIX signals, so we store user-supplied handlers
    // keyed by signal number and register a single JVM shutdown hook the first
    // time any signal handler is installed.  The shutdown hook fires SIGTERM (15)
    // semantics by calling every registered handler.
    // -------------------------------------------------------------------------
    private val signalHandlers: MutableMap<Int, (Int) -> Unit> = mutableMapOf()

    fun setSignal(signal: Int, handler: (Int) -> Unit) {
        signalHandlers[signal] = handler
        // Register a JVM shutdown hook that will invoke this handler so that
        // Ctrl-C / SIGTERM from the OS triggers the registered logic.
        Runtime.getRuntime().addShutdownHook(Thread({
            signalHandlers[signal]?.invoke(signal)
        }, "LLApp-signal-$signal").also { it.isDaemon = false })
    }

    // -------------------------------------------------------------------------
    // Full run loop – blocks until the app exits.
    // -------------------------------------------------------------------------
    fun run() {
        Companion.setStatus(AppStatus.RUNNING)
        _status.set(AppStatus.RUNNING)
        try {
            if (!init()) {
                Companion.setStatus(AppStatus.ERROR)
                _status.set(AppStatus.ERROR)
                return
            }
            while (isRunning) {
                if (!mainLoop()) break
            }
        } catch (t: Throwable) {
            Companion.setStatus(AppStatus.ERROR)
            _status.set(AppStatus.ERROR)
            throw t
        } finally {
            try {
                cleanup()
            } finally {
                Companion.setStatus(AppStatus.STOPPED)
                _status.set(AppStatus.STOPPED)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Companion – mirrors C++ static LLApp members
    // -------------------------------------------------------------------------
    companion object {
        private val globalStatus: AtomicReference<AppStatus> =
            AtomicReference(AppStatus.UNINITIALIZED)

        @Volatile private var instance: LLApp? = null

        // --- Static status helpers -------------------------------------------

        @JvmStatic fun getStatus(): AppStatus = globalStatus.get()

        @JvmStatic fun setStatus(s: AppStatus) { globalStatus.set(s) }

        @JvmStatic fun isRunning():  Boolean = globalStatus.get() == AppStatus.RUNNING
        @JvmStatic fun isQuitting(): Boolean = globalStatus.get() == AppStatus.QUITTING
        @JvmStatic fun isStopped():  Boolean = globalStatus.get() == AppStatus.STOPPED
        @JvmStatic fun isError():    Boolean = globalStatus.get() == AppStatus.ERROR
        @JvmStatic fun isExiting():  Boolean = isQuitting() || isError()

        @JvmStatic fun setQuitting() { globalStatus.set(AppStatus.QUITTING) }
        @JvmStatic fun setStopped()  { globalStatus.set(AppStatus.STOPPED) }
        @JvmStatic fun setError()    { globalStatus.set(AppStatus.ERROR) }

        @JvmStatic fun getPid(): Int {
            // ProcessHandle (Java 9 / Android API 26+) is not universally
            // available.  Read /proc/self on Linux/Android; return 0 elsewhere.
            return try {
                java.io.File("/proc/self").canonicalFile.name.toIntOrNull() ?: 0
            } catch (_: Exception) {
                0
            }
        }

        // --- Singleton instance management ------------------------------------

        @JvmStatic fun getInstance(): LLApp? = instance

        @JvmStatic fun setInstance(app: LLApp) { instance = app }

        // --- Crash-logger flag (stub; JVM crash handling differs from C++) ---
        @Volatile private var crashloggerDisabled: Boolean = false

        @JvmStatic fun isCrashloggerDisabled(): Boolean = crashloggerDisabled

        @JvmStatic fun disableCrashlogger() { crashloggerDisabled = true }
    }

    // -------------------------------------------------------------------------
    // Singleton placeholder object (required by spec, analogous to the C++
    // static sApplication pointer exposed for global access).
    // Concrete apps should replace this by calling setInstance().
    // -------------------------------------------------------------------------
    object App : LLApp() {
        override fun init():     Boolean { TODO("Override App.init() in a concrete subclass") }
        override fun mainLoop(): Boolean { TODO("Override App.mainLoop() in a concrete subclass") }
        override fun cleanup():  Boolean { TODO("Override App.cleanup() in a concrete subclass") }
    }
}
