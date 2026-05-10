package com.firestorm.llcommon

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.ConsoleHandler
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

/**
 * Logging and error-handling system, translated from llerror.h / llerror.cpp.
 *
 * The C++ implementation uses a stream-based macro system (LL_INFOS, LL_WARNS …).
 * In Kotlin we expose an [ELevel]-gated [log] function together with inline
 * helper top-level functions that match the Firestorm macro naming convention
 * (`llinfos`, `llwarns`, `llerrs`).
 *
 * Per-tag level overrides are stored in [tagLevels] and checked before the
 * global default, mirroring the C++ `LLError::Settings::setTagLevel()` API.
 *
 * C++ counterpart: indra/llcommon/llerror.h + llerror.cpp
 */
object LLError {

    // -------------------------------------------------------------------------
    // Level enum – matches C++ ELevel ordinals exactly.
    //   LEVEL_ALL / LEVEL_DEBUG = 0
    //   LEVEL_INFO              = 1
    //   LEVEL_WARN              = 2
    //   LEVEL_ERROR             = 3  (was FATAL)
    //   LEVEL_NONE              = 4  (suppress all output)
    //
    // ERROR_ONCE is a Kotlin-side addition: it delegates to ERROR but marks
    // the message for single-emission (equivalent to C++ LL_WARNS_ONCE /
    // LL_ERRS_ONCE pattern).
    // -------------------------------------------------------------------------
    enum class ELevel(val jvmLevel: Level) {
        DEBUG(Level.FINE),
        INFO(Level.INFO),
        WARN(Level.WARNING),
        ERROR_ONCE(Level.SEVERE),   // like ERROR but emitted only once per tag+msg
        ERROR(Level.SEVERE),
        NONE(Level.OFF)
    }

    // -------------------------------------------------------------------------
    // Per-tag "once" tracking for ERROR_ONCE
    // -------------------------------------------------------------------------
    private val emittedOnce: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // -------------------------------------------------------------------------
    // Level configuration
    // -------------------------------------------------------------------------

    /** Global minimum level – messages below this threshold are suppressed. */
    @Volatile private var defaultLevel: ELevel = ELevel.DEBUG

    /** Per-tag level overrides.  A tag present here supersedes [defaultLevel]. */
    private val tagLevels: ConcurrentHashMap<String, ELevel> = ConcurrentHashMap()

    /** Set the global minimum level for all tags that have no override. */
    fun setDefaultLevel(level: ELevel) {
        defaultLevel = level
        // Keep the JVM logger in sync so external tooling reflects the change.
        Log.logger.level = level.jvmLevel
    }

    /** Override the minimum level for a specific tag. */
    fun setTagLevel(tag: String, level: ELevel) {
        tagLevels[tag] = level
    }

    /** Resolve the effective level for [tag]: tag override beats default. */
    fun effectiveLevelFor(tag: String): ELevel =
        tagLevels[tag] ?: defaultLevel

    // -------------------------------------------------------------------------
    // Handler management
    // -------------------------------------------------------------------------

    private var activeHandler: Handler = ConsoleHandler().also { h ->
        h.formatter = SimpleFormatter()
        h.level = Level.ALL
    }

    fun setHandler(h: Handler) {
        Log.logger.removeHandler(activeHandler)
        activeHandler = h
        Log.logger.addHandler(h)
    }

    // -------------------------------------------------------------------------
    // Core log method
    // -------------------------------------------------------------------------

    /**
     * Emit a log message at [level] for [tag].
     *
     * The message is suppressed when [level] is below the effective threshold
     * for [tag].  For [ELevel.ERROR_ONCE] the message is emitted at most once
     * per unique "[tag]:[msg]" combination for the lifetime of the process.
     * [ELevel.ERROR] throws an [Error] after logging, mirroring the C++
     * LL_ERRS crash behaviour.
     */
    fun log(level: ELevel, tag: String, msg: String) {
        if (level == ELevel.NONE) return

        val effective = effectiveLevelFor(tag)
        // ordinal comparison: lower ordinal == lower severity
        if (level.ordinal < effective.ordinal) return

        when (level) {
            ELevel.ERROR_ONCE -> {
                val key = "$tag:$msg"
                if (!emittedOnce.add(key)) return   // already emitted
                Log.logger.log(Level.SEVERE, "[$tag] $msg")
            }
            ELevel.ERROR -> {
                Log.logger.log(Level.SEVERE, "[$tag] $msg")
                throw Error("LLError: fatal – [$tag] $msg")
            }
            else -> Log.logger.log(level.jvmLevel, "[$tag] $msg")
        }
    }

    // -------------------------------------------------------------------------
    // Convenience typed helpers (used by LLApp and other internal callers)
    // -------------------------------------------------------------------------
    object Log {
        internal val logger: Logger = Logger.getLogger("LLError").also { l ->
            l.useParentHandlers = false
            l.addHandler(LLError.activeHandler)
            l.level = Level.ALL
        }

        fun debug(tag: String, msg: String) = LLError.log(ELevel.DEBUG, tag, msg)
        fun info(tag: String, msg: String)  = LLError.log(ELevel.INFO,  tag, msg)
        fun warn(tag: String, msg: String)  = LLError.log(ELevel.WARN,  tag, msg)
        fun error(tag: String, msg: String) = LLError.log(ELevel.ERROR, tag, msg)

        fun isEnabled(level: ELevel): Boolean = level.ordinal >= LLError.defaultLevel.ordinal
    }
}

// =============================================================================
// Assertion helpers
// =============================================================================

/** Hard assertion – always active, equivalent to `llassert_always`. */
inline fun llassert(condition: Boolean, msg: String = "") {
    if (!condition) throw AssertionError(if (msg.isEmpty()) "Assertion failed" else msg)
}

// =============================================================================
// Macro-style top-level logging functions.
//
// Names follow the Firestorm/SL convention: llinfos, llwarns, llerrs.
// Each is inline so the lambda is inlined at the call-site, meaning no
// closure allocation occurs when the message is suppressed.
// =============================================================================

/** Equivalent to `LL_DEBUGS(tag) << msg << LL_ENDL`. */
inline fun lldebug(tag: String, block: () -> String) {
    if (LLError.Log.isEnabled(LLError.ELevel.DEBUG)) {
        LLError.log(LLError.ELevel.DEBUG, tag, block())
    }
}

/** Equivalent to `LL_INFOS(tag) << msg << LL_ENDL`. */
inline fun llinfos(tag: String, block: () -> String) {
    if (LLError.Log.isEnabled(LLError.ELevel.INFO)) {
        LLError.log(LLError.ELevel.INFO, tag, block())
    }
}

/** Equivalent to `LL_WARNS(tag) << msg << LL_ENDL`. */
inline fun llwarns(tag: String, block: () -> String) {
    if (LLError.Log.isEnabled(LLError.ELevel.WARN)) {
        LLError.log(LLError.ELevel.WARN, tag, block())
    }
}

/**
 * Equivalent to `LL_ERRS(tag) << msg << LL_ENDL`.
 * Logs at ERROR level and throws an [Error], mirroring the C++ crash.
 */
inline fun llerrs(tag: String, block: () -> String): Nothing {
    LLError.log(LLError.ELevel.ERROR, tag, block())
    // log() already throws; this satisfies the Nothing return for the compiler.
    throw Error("llerrs reached unreachable continuation")
}

/** Emit a WARN once per unique tag+message combination. */
inline fun llwarns_once(tag: String, block: () -> String) {
    LLError.log(LLError.ELevel.ERROR_ONCE, tag, block())
}

// Camel-case aliases kept for callers that used the previous naming convention.
@Deprecated("Use llinfos()", ReplaceWith("llinfos(tag, block)"))
inline fun llInfo(tag: String, block: () -> String)  = llinfos(tag, block)

@Deprecated("Use llwarns()", ReplaceWith("llwarns(tag, block)"))
inline fun llWarn(tag: String, block: () -> String)  = llwarns(tag, block)

@Deprecated("Use llerrs()", ReplaceWith("llerrs(tag, block)"))
inline fun llError(tag: String, block: () -> String): Nothing = llerrs(tag, block)

@Deprecated("Use lldebug()", ReplaceWith("lldebug(tag, block)"))
inline fun llDebug(tag: String, block: () -> String) = lldebug(tag, block)
