package com.firestorm.llcrashlogger

import com.firestorm.llcommon.llinfos
import com.firestorm.llcommon.llwarns
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

const val CRASH_BEHAVIOR_ASK = 0
const val CRASH_BEHAVIOR_ALWAYS_SEND = 1
const val CRASH_BEHAVIOR_NEVER_SEND = 2

private const val CRASH_UPLOAD_RETRIES = 3
private const val CRASH_UPLOAD_TIMEOUT = 180
private const val TRIM_SIZE = 128_000
private const val LINE_SEARCH_DIST = 500
private const val SKIP_TEXT = "\n ...Skipping... \n"
private const val CRASH_SETTINGS_FILE = "settings_crash_behavior.xml"

abstract class CrashLogger {
    protected var crashBehavior: Int = CRASH_BEHAVIOR_ASK
    protected var crashInPreviousExec: Boolean = false
    protected val fileMap: MutableMap<String, String> = mutableMapOf()
    protected var gridName: String = ""
    protected var productName: String = "Firestorm"
    protected val crashInfo: MutableMap<String, Any> = mutableMapOf()
    protected var crashLookup: CrashLookup? = null
    protected var crashHost: String = ""
    protected var altCrashHost: String = ""
    protected val debugLog: MutableMap<String, Any> = mutableMapOf()
    protected var sentCrashLogs: Boolean = false
    protected val keyMaster: CrashLock = CrashLock()

    // crash settings stored as a simple map; in C++ this was LLControlGroup
    private val crashSettings: MutableMap<String, Any> = mutableMapOf()

    abstract fun frame(): Boolean
    abstract fun cleanup(): Boolean

    open fun init(): Boolean {
        TODO("APR: initialize HTTP stack and app dirs")

        @Suppress("UNREACHABLE_CODE")
        run {
            productName = "Firestorm"

            val logDir: String = TODO("APR: resolve LL_PATH_LOGS")
            val oldLog = "$logDir/crashreport.log.old"
            val logFile = "$logDir/crashreport.log"
            File(logFile).renameTo(File(oldLog))

            llinfos("CRASHREPORT") { "Crash reporter file rotation complete." }

            var locked = keyMaster.requestMaster()
            while (!locked && keyMaster.isWaiting()) {
                llinfos("CRASHREPORT") { "Waiting for lock." }
                Thread.sleep(1000)
                locked = keyMaster.checkMaster()
            }

            if (!locked) {
                llwarns("CRASHREPORT") { "Unable to get master lock. Another crash reporter may be hung." }
                return false
            }

            crashSettings["CrashSubmitBehavior"] = CRASH_BEHAVIOR_ALWAYS_SEND

            llinfos("CRASHREPORT") { "Loading crash behavior setting" }
            crashBehavior = loadCrashBehaviorSetting()

            if (crashBehavior == CRASH_BEHAVIOR_NEVER_SEND) {
                llinfos("CRASHREPORT") { "Crash behavior is never_send, quitting" }
                return false
            }

            initCurl()
            return true
        }
    }

    open fun updateApplication(message: String = "") {
        if (message.isNotEmpty()) llinfos("CRASHREPORT") { message }
    }

    fun setUserText(text: String) {
        crashInfo["UserNotes"] = text
    }

    fun getCrashBehavior(): Int = crashBehavior

    fun loadCrashURLSetting(): String {
        loadCrashSettingsFile()
        return crashSettings["CrashHostUrl"] as? String ?: ""
    }

    fun loadCrashBehaviorSetting(): Int {
        loadCrashSettingsFile()
        return when (val v = crashSettings["CrashSubmitBehavior"] as? Int ?: CRASH_BEHAVIOR_ASK) {
            CRASH_BEHAVIOR_NEVER_SEND -> CRASH_BEHAVIOR_NEVER_SEND
            CRASH_BEHAVIOR_ALWAYS_SEND -> CRASH_BEHAVIOR_ALWAYS_SEND
            else -> CRASH_BEHAVIOR_ASK
        }
    }

    fun saveCrashBehaviorSetting(crashBehaviorValue: Int): Boolean {
        if (crashBehaviorValue !in listOf(CRASH_BEHAVIOR_ASK, CRASH_BEHAVIOR_NEVER_SEND, CRASH_BEHAVIOR_ALWAYS_SEND)) {
            return false
        }
        crashSettings["CrashSubmitBehavior"] = crashBehaviorValue
        TODO("APR: persist crashSettings to LL_PATH_USER_SETTINGS/$CRASH_SETTINGS_FILE")
    }

    fun readFromXML(dest: MutableMap<String, Any>, filename: String): Boolean {
        val path: String = TODO("APR: expand LL_PATH_DUMP/$filename")
        @Suppress("UNREACHABLE_CODE")
        return try {
            val content = File(path).readText()
            TODO("APR: parse XML content into dest map")
        } catch (_: Exception) {
            llwarns("CRASHREPORT") { "Failed to open $path" }
            false
        }
    }

    fun mergeLogs(srcSd: Map<String, Any>) {
        debugLog.putAll(srcSd)
    }

    open fun gatherPlatformSpecificFiles() {}

    fun gatherFiles() {
        updateApplication("Gathering logs...")

        val staticSd: MutableMap<String, Any> = mutableMapOf()
        val dynamicSd: MutableMap<String, Any> = mutableMapOf()
        var hasLogs = readFromXML(staticSd, "static_debug_info.log")
        hasLogs = readFromXML(dynamicSd, "dynamic_debug_info.log") || hasLogs

        if (hasLogs) {
            debugLog.putAll(staticSd)
            mergeLogs(dynamicSd)
            crashInPreviousExec = debugLog["CrashNotHandled"] as? Boolean ?: false

            fileMap["SecondLifeLog"] = debugLog["SLLog"] as? String ?: ""
            fileMap["SettingsXml"] = debugLog["SettingsFilename"] as? String ?: ""
            fileMap["CrashHostUrl"] = loadCrashURLSetting()

            llinfos("CRASHREPORT") { "Using log file from debug log ${fileMap["SecondLifeLog"]}" }
            llinfos("CRASHREPORT") { "Using settings file from debug log ${fileMap["SettingsXml"]}" }
        }

        gatherPlatformSpecificFiles()

        // Do not send the full log — it may be huge and contains OS username in path.
        fileMap.remove("SecondLifeLog")
        debugLog.remove("SLLog")

        if (hasLogs && (fileMap["CrashHostUrl"] ?: "").isNotEmpty()) {
            crashHost = fileMap["CrashHostUrl"]!!
        }

        crashInfo["DebugLog"] = debugLog.toMap()

        val dumpDir: String = TODO("APR: expand LL_PATH_DUMP/stats.log")
        @Suppress("UNREACHABLE_CODE")
        fileMap["StatsLog"] = dumpDir

        updateApplication("Encoding files...")

        var hasMinidump = debugLog.containsKey("MinidumpPath")
        var minidumpPath = ""

        if (hasMinidump) {
            minidumpPath = debugLog["MinidumpPath"] as? String ?: ""
            hasMinidump = readMinidump(minidumpPath)
        } else {
            llwarns("CRASHREPORT") { "DebugLog does not have MinidumpPath" }
        }

        if (!hasMinidump) {
            val pathname: String = TODO("APR: expand LL_PATH_DUMP/")
            @Suppress("UNREACHABLE_CODE")
            run {
                llwarns("CRASHREPORT") { "Searching for minidump in $pathname" }
                File(pathname).listFiles()?.forEach { file ->
                    if (!hasMinidump && file.name.length > 30 && file.name.endsWith(".dmp")) {
                        try {
                            val header = file.inputStream().use { it.readNBytes(4) }
                            if (header.size == 4 && String(header) == "MDMP") {
                                minidumpPath = file.name
                                hasMinidump = readMinidump(file.absolutePath)
                                debugLog["MinidumpPath"] = file.absolutePath
                            }
                        } catch (_: Exception) {
                            // skip unreadable files
                        }
                    }
                }
            }
        } else {
            llwarns("CRASHREPORT") { "readMinidump returned no minidump" }
        }

        if (hasMinidump) {
            val fullName = debugLog["MinidumpPath"] as? String ?: ""
            val dmpName = fullName.substringAfterLast('/').substringAfterLast('\\')
            if (dmpName.isNotEmpty()) {
                fileMap[dmpName] = fullName
            }
        }
    }

    fun readMinidump(minidumpPath: String): Boolean {
        return try {
            val bytes = File(minidumpPath).readBytes()
            llwarns("CRASHREPORT") { "minidump length ${bytes.size}" }
            crashInfo["Minidump"] = bytes
            bytes.isNotEmpty()
        } catch (_: Exception) {
            llwarns("CRASHREPORT") { "failed to open minidump $minidumpPath" }
            false
        }
    }

    fun constructPostData(): Map<String, Any> = crashInfo.toMap()

    fun runCrashLogPost(host: String, data: Map<String, Any>, msg: String, retries: Int, timeout: Int): Boolean {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeout.toLong()))
            .build()

        repeat(retries) { attempt ->
            updateApplication("$msg, try ${attempt + 1}...")
            llinfos("CRASHREPORT") { "POST crash data to $host" }

            return try {
                val body = TODO("APR: serialize data map to LLSD XML/JSON body string") as String
                @Suppress("UNREACHABLE_CODE")
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(host))
                    .timeout(Duration.ofSeconds(timeout.toLong()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() in 200..299) {
                    return true
                } else {
                    llwarns("CRASHREPORT") { "Failed to send crash report to \"$host\": HTTP ${response.statusCode()}" }
                }
            } catch (e: Exception) {
                llwarns("CRASHREPORT") { "Request POST failed to $host: ${e.message}" }
                return false
            }
        }
        return false
    }

    fun sendCrashLog(dumpDir: String): Boolean {
        TODO("APR: set dump dir via gDirUtilp equivalent")
        @Suppress("UNREACHABLE_CODE")
        run {
            val reportFile: String = TODO("APR: expand LL_PATH_LOGS/FirestormCrashReport.log")

            @Suppress("UNREACHABLE_CODE")
            gatherFiles()

            val postData = constructPostData()

            updateApplication("Sending reports...")

            try {
                val xml = TODO("APR: serialize postData to pretty XML string") as String
                @Suppress("UNREACHABLE_CODE")
                File(reportFile).writeText(xml)
            } catch (_: Exception) {}

            var sent = false
            if (crashHost.isNotEmpty()) {
                llwarns("CRASHREPORT") { "Sending crash data to server from CrashHostUrl '$crashHost'" }
                updateApplication("Using override crash server... $crashHost")
                sent = runCrashLogPost(crashHost, postData, "Sending to server", CRASH_UPLOAD_RETRIES, CRASH_UPLOAD_TIMEOUT)
            }

            sentCrashLogs = sent
            return sent
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun sendCrashLogs(): Boolean {
        val locks = getProcessList()
        val newLocks: MutableList<Map<String, Any>> = mutableListOf()

        val opts = getOptionData()
        var rec: MutableMap<String, Any>? = null

        if (opts.containsKey("pid") && opts.containsKey("dumpdir") && opts.containsKey("procname")) {
            rec = mutableMapOf(
                "pid" to opts["pid"]!!,
                "dumpdir" to opts["dumpdir"]!!,
                "procname" to opts["procname"]!!
            )
        }

        if (rec != null && rec.containsKey("dumpdir")) {
            if (!sendCrashLog(rec["dumpdir"] as String)) {
                newLocks.add(rec)
            }
        }

        val locksArray = locks["array"] as? List<Map<String, Any>> ?: emptyList()
        for (lock in locksArray) {
            if (lock.containsKey("pid") && lock.containsKey("dumpdir") && lock.containsKey("procname")) {
                val pid = (lock["pid"] as? Long)?.toUInt() ?: continue
                val procname = lock["procname"] as? String ?: continue
                val dumpdir = lock["dumpdir"] as? String ?: continue

                if (keyMaster.isProcessAlive(pid, procname)) {
                    newLocks.add(lock)
                } else if (CrashLock.fileExists(dumpdir)) {
                    if (!sendCrashLog(dumpdir)) {
                        newLocks.add(lock)
                    } else {
                        keyMaster.cleanupProcess(dumpdir)
                    }
                }
            } else {
                llinfos("CRASHREPORT") { "Discarding corrupted entry from lock table." }
            }
        }

        keyMaster.putProcessList(mapOf("array" to newLocks))
        return true
    }

    fun commonCleanup() {
        termCurl()
        TODO("APR: close crashreport.log via LLError.logToFile equivalent")
    }

    private fun loadCrashSettingsFile() {
        TODO("APR: load CRASH_SETTINGS_FILE from LL_PATH_USER_SETTINGS or LL_PATH_APP_SETTINGS into crashSettings")
    }

    // Returns command-line option data; implementation depends on the app framework.
    protected open fun getOptionData(): Map<String, Any> = emptyMap()

    // Returns process list as a map with "array" key containing list of entries.
    private fun getProcessList(): Map<String, Any> = keyMaster.getProcessList()

    companion object {
        private fun initCurl() {
            TODO("APR: initialize libcurl equivalent / JVM TLS setup")
        }

        private fun termCurl() {
            TODO("APR: tear down libcurl equivalent / JVM TLS cleanup")
        }
    }
}

internal fun trimSlLog(sllog: String): String {
    if (sllog.length <= TRIM_SIZE * 2) return sllog

    var head = TRIM_SIZE
    var tail = sllog.length - TRIM_SIZE

    val newHead = sllog.lastIndexOf('\n', head)
    if (newHead != -1 && head - newHead <= LINE_SEARCH_DIST) head = newHead

    val newTail = sllog.indexOf('\n', tail)
    if (newTail != -1 && newTail - tail <= LINE_SEARCH_DIST) tail = newTail

    return sllog.substring(0, head) + SKIP_TEXT + sllog.substring(tail)
}

internal fun getStartupStateFromLog(sllog: String): String {
    val token = "Startup state changing from "
    val idx = sllog.lastIndexOf(token)
    if (idx == -1 || idx + token.length >= sllog.length) return "STATE_FIRST"

    val lineEnd = sllog.indexOf('\n', idx + token.length).let { if (it == -1) sllog.length else it }
    val line = sllog.substring(idx, lineEnd)
    val toIdx = line.indexOf(" to ")
    if (toIdx == -1) return "STATE_FIRST"
    return line.substring(toIdx + 4)
}

internal fun getFormDataField(fieldName: String, fieldValue: String, boundary: String): String =
    "--$boundary\r\nContent-Disposition: form-data; name=\"$fieldName\"\r\n\r\n$fieldValue\r\n"
