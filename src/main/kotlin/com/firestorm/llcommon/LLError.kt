package com.firestorm.llcommon

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object LLError {

    // -------------------------------------------------------------------------
    // Level enum — ordinals match C++ ELevel exactly.
    // -------------------------------------------------------------------------
    // Ordinals match C++ ELevel: DEBUG=0, INFO=1, WARN=2, ERROR=3, NONE=4.
    enum class ELevel {
        DEBUG, INFO, WARN, ERROR, NONE;

        companion object {
            val ALL = DEBUG
        }
    }

    // -------------------------------------------------------------------------
    // Settings config  (mirrors anonymous SettingsConfig in llerror.cpp)
    // -------------------------------------------------------------------------
    private data class SettingsConfig(
        var defaultLevel: ELevel = ELevel.INFO,
        var logAlwaysFlush: Boolean = true,
        var enabledLogTypesMask: UInt = 0xFFFFFFFFU,
        val functionLevelMap: MutableMap<String, ELevel> = mutableMapOf(),
        val classLevelMap: MutableMap<String, ELevel> = mutableMapOf(),
        val fileLevelMap: MutableMap<String, ELevel> = mutableMapOf(),
        val tagLevelMap: MutableMap<String, ELevel> = mutableMapOf(),
        val uniqueLogMessages: MutableMap<String, Int> = mutableMapOf(),
        var crashFunction: ((String) -> Unit)? = null,
        var timeFunction: (() -> String)? = ::utcTime,
        val recorders: MutableList<Recorder> = mutableListOf(),
        var shouldLogCallCounter: Int = 0
    )

    @Volatile private var settings = SettingsConfig()
    private val recorderLock = ReentrantLock()

    // "once" tracking for WARN/INFO _ONCE macros
    private val emittedOnce: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // fatal message saved for crash reporters
    @Volatile var fatalMessage: String = ""
        private set

    // -------------------------------------------------------------------------
    // Recorder  (abstract base matching C++ LLError::Recorder)
    // -------------------------------------------------------------------------
    abstract class Recorder {
        var wantsTime: Boolean = true
        var wantsTags: Boolean = true
        var wantsLevel: Boolean = true
        var wantsLocation: Boolean = true
        var wantsFunctionName: Boolean = true
        var wantsMultiline: Boolean = false

        open fun enabled(): Boolean = true
        abstract fun recordMessage(level: ELevel, message: String)

        fun showTime(show: Boolean)         { wantsTime = show }
        fun showTags(show: Boolean)         { wantsTags = show }
        fun showLevel(show: Boolean)        { wantsLevel = show }
        fun showLocation(show: Boolean)     { wantsLocation = show }
        fun showFunctionName(show: Boolean) { wantsFunctionName = show }
        fun showMultiline(show: Boolean)    { wantsMultiline = show }
    }

    // -------------------------------------------------------------------------
    // Built-in recorders
    // -------------------------------------------------------------------------

    class StderrRecorder(private val useAnsi: Boolean = System.console() != null) : Recorder() {
        init { showMultiline(true) }
        override fun enabled(): Boolean = (settings.enabledLogTypesMask and 0x04U) != 0U
        override fun recordMessage(level: ELevel, message: String) {
            if (useAnsi) {
                val code = when (level) {
                    ELevel.ERROR -> "[38;5;160m"
                    ELevel.WARN  -> "[38;5;33m"
                    else         -> "[38;5;177m"
                }
                System.err.println("$code$message[0m")
            } else {
                System.err.println(message)
            }
        }
    }

    class FileRecorder(private val filename: String) : Recorder() {
        init { showMultiline(true) }
        private val writer: PrintWriter? = try {
            PrintWriter(OutputStreamWriter(FileOutputStream(filename, true), Charsets.UTF_8), true)
        } catch (_: Exception) { null }

        val ok: Boolean get() = writer != null
        fun getFilename(): String = filename

        override fun enabled(): Boolean = (settings.enabledLogTypesMask and 0x02U) != 0U
        override fun recordMessage(level: ELevel, message: String) {
            if (settings.logAlwaysFlush) writer?.println(message) else writer?.print("$message\n")
        }
    }

    class FixedBufferRecorder(private val addLine: (String) -> Unit) : Recorder() {
        init { showMultiline(true); showTags(false); showLocation(false) }
        override fun enabled(): Boolean = (settings.enabledLogTypesMask and 0x08U) != 0U
        override fun recordMessage(level: ELevel, message: String) = addLine(message)
    }

    // -------------------------------------------------------------------------
    // Recorder management
    // -------------------------------------------------------------------------

    fun addRecorder(recorder: Recorder) {
        recorderLock.withLock { settings.recorders.add(recorder) }
    }

    fun removeRecorder(recorder: Recorder) {
        recorderLock.withLock { settings.recorders.remove(recorder) }
    }

    fun logToFile(fileName: String) {
        recorderLock.withLock { settings.recorders.removeAll { it is FileRecorder } }
        if (fileName.isNotEmpty()) {
            val r = FileRecorder(fileName)
            if (r.ok) addRecorder(r)
        }
    }

    fun logFileName(): String =
        recorderLock.withLock { (settings.recorders.firstOrNull { it is FileRecorder } as? FileRecorder)?.getFilename() } ?: ""

    fun logToStderr() {
        recorderLock.withLock {
            if (settings.recorders.none { it is StderrRecorder }) addRecorder(StderrRecorder())
        }
    }

    fun logToFixedBuffer(addLine: ((String) -> Unit)?) {
        recorderLock.withLock { settings.recorders.removeAll { it is FixedBufferRecorder } }
        if (addLine != null) addRecorder(FixedBufferRecorder(addLine))
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    fun initForApplication(userDir: String, appDir: String, logToStderr: Boolean = true) {
        settings = SettingsConfig()
        setDefaultLevel(ELevel.INFO)
        setAlwaysFlush(true)
        setEnabledLogTypesMask(0xFFFFFFFFU)
        setTimeFunction(::utcTime)

        if (logToStderr) logToStderr()

        // Try to load logcontrol.xml from userDir, then appDir.
        // If neither exists the defaults configured above remain in effect.
        val logControlFile = listOf(
            File(userDir, "logcontrol.xml"),
            File(appDir, "logcontrol.xml")
        ).firstOrNull { it.exists() && it.isFile }

        if (logControlFile != null) {
            try {
                applyLogControlFile(logControlFile.readText())
            } catch (e: Exception) {
                llwarns("LLError") { "Failed to parse ${logControlFile.path}: ${e.message}" }
            }
        }
    }

    /**
     * Parse and apply a logcontrol.xml LLSD document.
     *
     * The expected format mirrors the Second Life logcontrol.xml:
     * ```
     * <llsd><map>
     *   <key>default</key><string>DEBUG</string>
     *   <key>classes</key><array><map>
     *     <key>name</key><string>MyClass</string>
     *     <key>level</key><string>INFO</string>
     *   </map></array>
     *   <key>tags</key><array>…</array>
     *   <key>functions</key><array>…</array>
     *   <key>files</key><array>…</array>
     * </map></llsd>
     * ```
     */
    private fun applyLogControlFile(xml: String) {
        val sd = LLSDSerialize.fromXML(xml)
        if (sd !is LLSD.LLSDMap) return
        val map = sd.value

        map["default"]?.asString()?.let { lvl ->
            levelFromString(lvl)?.let { setDefaultLevel(it) }
        }

        fun applyEntries(key: String, setter: (String, ELevel) -> Unit) {
            val entries = (map[key] as? LLSD.LLSDArray) ?: return
            for (entry in entries.value) {
                val entryMap = (entry as? LLSD.LLSDMap)?.value ?: continue
                val name  = entryMap["name"]?.asString()  ?: continue
                val level = entryMap["level"]?.asString()?.let { levelFromString(it) } ?: continue
                setter(name, level)
            }
        }

        applyEntries("classes")   { name, lvl -> setClassLevel(name, lvl) }
        applyEntries("tags")      { name, lvl -> setTagLevel(name, lvl) }
        applyEntries("functions") { name, lvl -> setFunctionLevel(name, lvl) }
        applyEntries("files")     { name, lvl -> setFileLevel(name, lvl) }
    }

    private fun levelFromString(s: String): ELevel? = when (s.uppercase()) {
        "DEBUG" -> ELevel.DEBUG
        "INFO"  -> ELevel.INFO
        "WARN", "WARNING" -> ELevel.WARN
        "ERROR" -> ELevel.ERROR
        "NONE"  -> ELevel.NONE
        else    -> null
    }

    fun setFatalFunction(f: (String) -> Unit) { settings.crashFunction = f }
    fun getFatalFunction(): ((String) -> Unit)? = settings.crashFunction
    fun getFatalMessage(): String = fatalMessage

    fun setTimeFunction(f: () -> String) { settings.timeFunction = f }
    fun setDefaultLevel(level: ELevel) { settings.defaultLevel = level }
    fun getDefaultLevel(): ELevel = settings.defaultLevel
    fun setAlwaysFlush(flush: Boolean) { settings.logAlwaysFlush = flush }
    fun getAlwaysFlush(): Boolean = settings.logAlwaysFlush
    fun setEnabledLogTypesMask(mask: UInt) { settings.enabledLogTypesMask = mask }
    fun getEnabledLogTypesMask(): UInt = settings.enabledLogTypesMask

    fun setFunctionLevel(functionName: String, level: ELevel) { settings.functionLevelMap[functionName] = level }
    fun setClassLevel(className: String, level: ELevel)       { settings.classLevelMap[className] = level }
    fun setFileLevel(fileName: String, level: ELevel)         { settings.fileLevelMap[fileName] = level }
    fun setTagLevel(tagName: String, level: ELevel)           { settings.tagLevelMap[tagName] = level }

    fun decodeLevel(name: String): ELevel = when (name.uppercase()) {
        "ALL", "DEBUG" -> ELevel.DEBUG
        "INFO"         -> ELevel.INFO
        "WARN"         -> ELevel.WARN
        "ERROR"        -> ELevel.ERROR
        "NONE"         -> ELevel.NONE
        else           -> { log(ELevel.WARN, "", "unrecognized logging level: '$name'"); ELevel.INFO }
    }

    fun shouldLogCallCount(): Int = settings.shouldLogCallCounter

    // -------------------------------------------------------------------------
    // Log
    // -------------------------------------------------------------------------

    object Log {
        // Convenience delegating to the outer object.
        fun debug(tag: String, msg: String) = LLError.log(ELevel.DEBUG, tag, msg)
        fun info(tag: String, msg: String)  = LLError.log(ELevel.INFO,  tag, msg)
        fun warn(tag: String, msg: String)  = LLError.log(ELevel.WARN,  tag, msg)
        fun error(tag: String, msg: String) = LLError.log(ELevel.ERROR, tag, msg)

        fun isEnabled(level: ELevel): Boolean = level.ordinal >= LLError.settings.defaultLevel.ordinal

        fun demangle(mangled: String): String = mangled  // GCC demangling not available on JVM
    }

    fun log(level: ELevel, tag: String, msg: String, printOnce: Boolean = false) {
        if (level == ELevel.NONE) return

        settings.shouldLogCallCounter++

        // resolve effective level
        val effectiveLevel = resolveEffectiveLevel(tag)
        if (level.ordinal < effectiveLevel.ordinal) return

        var finalMsg = msg
        if (printOnce) {
            val key = "$tag:$msg"
            val count = synchronized(settings.uniqueLogMessages) {
                val c = (settings.uniqueLogMessages[key] ?: 0) + 1
                settings.uniqueLogMessages[key] = c
                c
            }
            finalMsg = when {
                count == 1 -> "ONCE: $msg"
                count == 10 || count == 50 || count % 100 == 0 -> "ONCE (${count}th time seen): $msg"
                else -> return
            }
        }

        writeToRecorders(level, tag, finalMsg)

        if (level == ELevel.ERROR) {
            fatalMessage = finalMsg
            settings.crashFunction?.invoke(finalMsg)
            throw Error("LLError: fatal – [$tag] $finalMsg")
        }
    }

    private fun resolveEffectiveLevel(tag: String): ELevel {
        if (tag.isNotEmpty()) {
            settings.tagLevelMap[tag]?.let { return it }
        }
        return settings.defaultLevel
    }

    private fun writeToRecorders(level: ELevel, tag: String, message: String) {
        val levelStr = when (level) {
            ELevel.DEBUG -> "DEBUG"
            ELevel.INFO  -> "INFO"
            ELevel.WARN  -> "WARNING"
            ELevel.ERROR -> "ERROR"
            ELevel.NONE  -> "NONE"
        }
        val tagStr = if (tag.isNotEmpty()) "#$tag#" else "#"
        val timeStr = settings.timeFunction?.invoke() ?: ""

        recorderLock.withLock {
            settings.recorders.forEach { r ->
                if (!r.enabled()) return@forEach
                val sb = StringBuilder()
                if (r.wantsTime) sb.append(timeStr)
                sb.append(" ")
                if (r.wantsLevel) sb.append(levelStr)
                sb.append(" ")
                if (r.wantsTags) sb.append(tagStr)
                sb.append(" ")
                // location / function are not available at runtime on JVM without
                // stack-walking; skip unless caller supplies them.
                sb.append(": ")
                val escaped = if (r.wantsMultiline) message else escapeMessageLines(message)
                sb.append(escaped)
                r.recordMessage(level, sb.toString())
            }
        }
    }

    // -------------------------------------------------------------------------
    // LLCallStacks
    // -------------------------------------------------------------------------
    object LLCallStacks {
        private val buffer: MutableList<String> = mutableListOf()
        private val lock = ReentrantLock()

        fun push(function: String, line: Int) = lock.withLock {
            if (buffer.size > 511) clear()
            buffer.add("$function line $line ")
        }

        fun insert(out: StringBuilder, function: String, line: Int) {
            out.append("$function line $line ")
        }

        fun end(out: StringBuilder) = lock.withLock {
            if (buffer.size > 511) clear()
            buffer.add(out.toString())
        }

        fun print() = lock.withLock {
            if (buffer.isNotEmpty()) {
                LLError.log(ELevel.INFO, "", " ************* PRINT OUT LL CALL STACKS ************* ")
                buffer.reversed().forEach { LLError.log(ELevel.INFO, "", it) }
                LLError.log(ELevel.INFO, "", " *************** END OF LL CALL STACKS *************** ")
            }
            cleanup()
        }

        fun clear() { buffer.clear() }
        fun cleanup() = clear()
    }

    // -------------------------------------------------------------------------
    // LLUserWarningMsg
    // -------------------------------------------------------------------------
    object LLUserWarningMsg {
        enum class LastExecEvent { ERROR_OTHER, ERROR_BAD_ALLOC, ERROR_MISSING_FILES }

        private var oomTitle: String = ""
        private var oomMessage: String = ""
        private var handler: ((title: String, message: String, code: Int) -> Unit)? = null

        fun setHandler(h: (String, String, Int) -> Unit) { handler = h }

        fun setOutOfMemoryStrings(title: String, message: String) {
            oomTitle = title
            oomMessage = message
        }

        fun show(message: String, errorCode: Int = -1) {
            handler?.invoke("", message, errorCode)
        }

        fun showOutOfMemory() {
            if (handler != null && oomTitle.isNotEmpty()) {
                handler!!.invoke(oomTitle, oomMessage, LastExecEvent.ERROR_BAD_ALLOC.ordinal)
            }
        }

        fun showMissingFiles() {
            val msg = "Firestorm couldn't access some of the files it needs and will be closed." +
                "\n\nPlease reinstall viewer from https://www.firestormviewer.org/download and " +
                "contact https://www.firestormviewer.org/support if issue persists after reinstall."
            handler?.invoke("Missing Files", msg, LastExecEvent.ERROR_MISSING_FILES.ordinal)
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    fun abbreviateFile(filePath: String): String {
        var f = filePath.replace('\\', '/')
        val indraPrefix = "indra/"
        val idx = f.indexOf(indraPrefix)
        if (idx >= 0) f = f.substring(idx + indraPrefix.length)
        return f
    }

    fun utcTime(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .withZone(ZoneOffset.UTC)
        .format(Instant.now())

    private fun escapeMessageLines(message: String): String {
        val sb = StringBuilder()
        for (c in message) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    // Stacktrace helper (mirrors LLStacktrace in C++)
    fun stacktrace(): String = Thread.currentThread().stackTrace
        .drop(2)  // drop getStackTrace and stacktrace() frames
        .joinToString("\n") { "\t${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
}

// =============================================================================
// Assertion helpers
// =============================================================================

inline fun llassert(condition: Boolean, msg: String = "") {
    if (!condition) throw AssertionError(if (msg.isEmpty()) "Assertion failed" else msg)
}

// =============================================================================
// Top-level logging helpers  (inline to avoid closure allocation when suppressed)
// =============================================================================

inline fun lldebug(tag: String, block: () -> String) {
    if (LLError.Log.isEnabled(LLError.ELevel.DEBUG))
        LLError.log(LLError.ELevel.DEBUG, tag, block())
}

inline fun llinfos(tag: String, block: () -> String) {
    if (LLError.Log.isEnabled(LLError.ELevel.INFO))
        LLError.log(LLError.ELevel.INFO, tag, block())
}

inline fun llwarns(tag: String, block: () -> String) {
    if (LLError.Log.isEnabled(LLError.ELevel.WARN))
        LLError.log(LLError.ELevel.WARN, tag, block())
}

inline fun llerrs(tag: String, block: () -> String): Nothing {
    LLError.log(LLError.ELevel.ERROR, tag, block())
    throw Error("unreachable")  // log() throws; satisfies Nothing return type
}

inline fun lldebug_once(tag: String, block: () -> String) {
    LLError.log(LLError.ELevel.DEBUG, tag, block(), printOnce = true)
}

inline fun llinfos_once(tag: String, block: () -> String) {
    LLError.log(LLError.ELevel.INFO, tag, block(), printOnce = true)
}

inline fun llwarns_once(tag: String, block: () -> String) {
    LLError.log(LLError.ELevel.WARN, tag, block(), printOnce = true)
}
