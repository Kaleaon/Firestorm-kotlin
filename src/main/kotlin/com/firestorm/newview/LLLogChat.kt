package com.firestorm.newview

import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import kotlin.concurrent.withLock

const val LOG_RECALL_SIZE = 20480

const val LL_IM_TIME = "time"
const val LL_IM_DATE_TIME = "datetime"
const val LL_IM_TEXT = "message"
const val LL_IM_FROM = "from"
const val LL_IM_FROM_ID = "from_id"
const val LL_TRANSCRIPT_FILE_EXTENSION = "txt"
const val GROUP_CHAT_SUFFIX = " (group)"

private const val IM_SYMBOL_SEPARATOR = ':'
private const val IM_SEPARATOR = ": "
private const val NEW_LINE = "\n"
private const val NEW_LINE_SPACE_PREFIX = "\n "
private const val MULTI_LINE_PREFIX = " "
private const val NAME_TEXT_DIVIDER = ": "
private const val SYSTEM_FROM = "Second Life"

private val TIMESTAMP_AND_STUFF = Regex(
    """^(\[\d{4}/\d{1,2}/\d{1,2}\s+\d{1,2}:\d{2}\s[AaPp][Mm]\]\s+|\[\d{4}/\d{1,2}/\d{1,2}\s+\d{1,2}:\d{2}\]\s+|\[\d{1,2}:\d{2}\s[AaPp][Mm]\]\s+|\[\d{1,2}:\d{2}\]\s+)?(.*)$"""
)
private val TIMESTAMP = Regex(
    """^(\[\d{4}/\d{1,2}/\d{1,2}\s+\d{1,2}:\d{2}(\s[AaPp][Mm])?\]|\[\d{1,2}:\d{2}(\s[AaPp][Mm])?\]).*"""
)
private val TIMESTAMP_AND_STUFF_SEC = Regex(
    """^(\[\d{4}/\d{1,2}/\d{1,2}\s+\d{1,2}:\d{2}:\d{2}\s[AaPp][Mm]\]\s+|\[\d{4}/\d{1,2}/\d{1,2}\s+\d{1,2}:\d{2}:\d{2}\]\s+|\[\d{1,2}:\d{2}:\d{2}\s[AaPp][Mm]\]\s+|\[\d{1,2}:\d{2}:\d{2}\]\s+)(.*)$"""
)
private val TIMESTAMP_AND_SEC = Regex(
    """^(\[\d{4}/\d{1,2}/\d{1,2}\s+\d{1,2}:\d{2}:\d{2}(\s[AaPp][Mm])?\]|\[\d{1,2}:\d{2}:\d{2}(\s[AaPp][Mm])?\]).*"""
)
private val INBOUND_CONFERENCE = Regex("""^[a-zA-Z]{1,31} [a-zA-Z]{1,31} Conference [0-9]{4}/[0-9]{2}/[0-9]{2} [0-9]{2}:[0-9]{2} [0-9a-f]{4}""")
private val OUTBOUND_CONFERENCE = Regex("""^Ad-hoc Conference hash[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}""")
private val NAME_AND_TEXT = Regex("""([^:]+[:]{1})?(\s*)(.*)""")

private fun appendToLastMessage(messages: MutableList<Map<String, Any>>, line: String) {
    if (messages.isEmpty()) return
    val last = messages.last().toMutableMap()
    val text = (last[LL_IM_TEXT] as? String) ?: ""
    last[LL_IM_TEXT] = text + line
    messages[messages.lastIndex] = last
}

abstract class LLActionThread(val name: String) : Thread(name) {
    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()
    var isFinished: Boolean = false
        private set

    fun waitFinished() {
        lock.withLock {
            if (!isFinished) {
                condition.await()
            }
        }
    }

    protected fun setFinished() {
        lock.withLock {
            isFinished = true
            condition.signalAll()
        }
    }
}

open class LLLoadHistoryThread(
    private val fileName: String,
    private val messages: MutableList<Map<String, Any>>,
    private val loadParams: Map<String, Any>
) : LLActionThread("load chat history") {

    private var newLoad = true
    val loadEndListeners: MutableList<(MutableList<Map<String, Any>>, String) -> Unit> = mutableListOf()

    fun setLoadEndSignal(cb: (MutableList<Map<String, Any>>, String) -> Unit) {
        loadEndListeners.add(cb)
    }

    fun removeLoadEndSignal() {
        loadEndListeners.clear()
    }

    override fun run() {
        if (newLoad) {
            loadHistory(fileName, messages, loadParams)
            setFinished()
        }
    }

    open fun loadHistory(
        fileName: String,
        messages: MutableList<Map<String, Any>>,
        loadParams: Map<String, Any>
    ) {
        if (fileName.isEmpty()) return

        val loadAllHistory = loadParams["load_all_history"] as? Boolean ?: false
        val logFile = LLLogChat.makeLogFileName(fileName)

        System.err.println("LLLoadHistoryThread: loadHistory file reading not yet implemented")
    }
}

open class LLDeleteHistoryThread(
    private var messages: MutableList<Map<String, Any>>?,
    private val loadThread: LLLoadHistoryThread?
) : LLActionThread("delete chat history") {

    override fun run() {
        loadThread?.waitFinished()
        messages = null
        setFinished()
    }

    companion object {
        fun deleteHistory() {
            System.err.println("LLDeleteHistoryThread: deleteHistory not yet implemented")
        }
    }
}

object LLLogChat {

    enum class ELogLineType {
        LOG_EMPTY,
        LOG_LINE,
        LOG_LLSD,
        LOG_END
    }

    private val historyThreadsLock = ReentrantLock()
    private val loadHistoryThreads: MutableMap<UUID, LLLoadHistoryThread> = mutableMapOf()
    private val deleteHistoryThreads: MutableMap<UUID, LLDeleteHistoryThread> = mutableMapOf()
    private val saveHistoryListeners: MutableList<() -> Unit> = mutableListOf()

    fun timestamp2LogString(timestamp: UInt, withdate: Boolean): String {
        System.err.println("LLLogChat: timestamp2LogString not yet implemented")
        return ""
    }

    fun makeLogFileName(filename: String): String {
        var name = filename
        val isInbound = INBOUND_CONFERENCE.containsMatchIn(name)
        val isOutbound = OUTBOUND_CONFERENCE.containsMatchIn(name)

        if (!isInbound && !isOutbound) {
            System.err.println("LLLogChat: makeLogFileName per-account date-stamped log path expansion not yet implemented")
        }

        name = cleanFileName(name)
        System.err.println("LLLogChat: makeLogFileName getExpandedFilename not yet implemented")
        return name
    }

    fun renameLogFile(oldFilename: String, newFilename: String) {
        val newName = cleanFileName(newFilename)
        val oldName = cleanFileName(oldFilename)
        if (newName.isEmpty() || oldName.isEmpty()) return
        System.err.println("LLLogChat: renameLogFile not yet implemented")
    }

    fun oldLogFileName(filename: String): String {
        System.err.println("LLLogChat: oldLogFileName not yet implemented")
        return ""
    }

    fun saveHistory(filename: String, from: String, fromId: UUID, line: String) {
        System.err.println("LLLogChat: saveHistory not yet implemented")
    }

    fun transcriptFilesExist(): Boolean {
        System.err.println("LLLogChat: transcriptFilesExist not yet implemented")
        return false
    }

    fun findTranscriptFiles(pattern: String, listOfTranscriptions: MutableList<String>) {
        System.err.println("LLLogChat: findTranscriptFiles not yet implemented")
    }

    fun getListOfTranscriptFiles(list: MutableList<String>) {
        findTranscriptFiles("*.$LL_TRANSCRIPT_FILE_EXTENSION", list)
    }

    fun getListOfTranscriptBackupFiles(list: MutableList<String>) {
        findTranscriptFiles("*.$LL_TRANSCRIPT_FILE_EXTENSION.backup*", list)
    }

    fun loadChatHistory(
        fileName: String,
        messages: MutableList<Map<String, Any>>,
        loadParams: Map<String, Any> = emptyMap(),
        isGroup: Boolean = false
    ) {
        if (fileName.isEmpty()) return
        System.err.println("LLLogChat: loadChatHistory not yet implemented")
    }

    fun setSaveHistorySignal(cb: () -> Unit) {
        saveHistoryListeners.add(cb)
    }

    fun moveTranscripts(
        currentDirectory: String,
        newDirectory: String,
        listOfFilesToMove: MutableList<String>,
        listOfFilesMoved: MutableList<String>
    ): Boolean {
        System.err.println("LLLogChat: moveTranscripts not yet implemented")
        return false
    }

    fun moveTranscripts(
        currentDirectory: String,
        newDirectory: String,
        listOfFilesToMove: MutableList<String>
    ): Boolean {
        val moved = mutableListOf<String>()
        return moveTranscripts(currentDirectory, newDirectory, listOfFilesToMove, moved)
    }

    fun deleteTranscripts() {
        val files = mutableListOf<String>()
        getListOfTranscriptFiles(files)
        getListOfTranscriptBackupFiles(files)
        System.err.println("LLLogChat: deleteTranscripts file delete not yet implemented")
    }

    fun isTranscriptExist(avatarId: UUID, isGroup: Boolean = false): Boolean {
        System.err.println("LLLogChat: isTranscriptExist not yet implemented")
        return false
    }

    fun isNearbyTranscriptExist(): Boolean {
        return isTranscriptFileFound(makeLogFileName("chat"))
    }

    fun isAdHocTranscriptExist(fileName: String): Boolean {
        return isTranscriptFileFound(makeLogFileName(fileName))
    }

    fun isTranscriptFileFound(fullname: String): Boolean {
        System.err.println("LLLogChat: isTranscriptFileFound not yet implemented")
        return false
    }

    fun getGroupChatSuffix(): String = GROUP_CHAT_SUFFIX

    fun historyThreadsFinished(sessionId: UUID): Boolean {
        historyThreadsLock.withLock {
            val loadFinished = loadHistoryThreads[sessionId]?.isFinished ?: true
            if (!loadFinished) return false
            val deleteFinished = deleteHistoryThreads[sessionId]?.isFinished ?: true
            return deleteFinished
        }
    }

    fun getLoadHistoryThread(sessionId: UUID): LLLoadHistoryThread? =
        historyThreadsLock.withLock { loadHistoryThreads[sessionId] }

    fun getDeleteHistoryThread(sessionId: UUID): LLDeleteHistoryThread? =
        historyThreadsLock.withLock { deleteHistoryThreads[sessionId] }

    fun addLoadHistoryThread(sessionId: UUID, thread: LLLoadHistoryThread): Boolean {
        historyThreadsLock.withLock {
            if (loadHistoryThreads.containsKey(sessionId)) return false
            loadHistoryThreads[sessionId] = thread
            return true
        }
    }

    fun addDeleteHistoryThread(sessionId: UUID, thread: LLDeleteHistoryThread): Boolean {
        historyThreadsLock.withLock {
            if (deleteHistoryThreads.containsKey(sessionId)) return false
            deleteHistoryThreads[sessionId] = thread
            return true
        }
    }

    fun cleanupHistoryThreads() {
        historyThreadsLock.withLock {
            val toRemove = loadHistoryThreads.keys.filter { id ->
                val loadDone = loadHistoryThreads[id]?.isFinished == true
                val deleteDone = deleteHistoryThreads[id]?.isFinished == true
                loadDone && deleteDone
            }
            toRemove.forEach { id ->
                loadHistoryThreads.remove(id)
                deleteHistoryThreads.remove(id)
            }
        }
    }

    private fun triggerHistorySignal() {
        saveHistoryListeners.forEach { it() }
    }

    private fun cleanFileName(filename: String): String {
        val invalidChars = setOf('"', '\'', '\\', '/', '?', '*', ':', '.', '<', '>', '|', '[', ']', '{', '}', '~')
        return filename.map { if (it in invalidChars) '_' else it }.joinToString("")
    }
}

class LLChatLogFormatter(private val im: Map<String, Any>) {

    fun format(): String {
        val sb = StringBuilder()

        val time = im[LL_IM_TIME] as? String
        if (!time.isNullOrBlank()) {
            sb.append('[').append(time.trim()).append(']').append("  ")
        }

        val from = im[LL_IM_FROM] as? String
        if (!from.isNullOrBlank()) {
            val trimmed = from.trim()
            val encoded = trimmed.replace(IM_SYMBOL_SEPARATOR.toString(), "%3A")
            if (encoded.isNotEmpty()) {
                sb.append(encoded).append(IM_SEPARATOR)
            }
        }

        val text = im[LL_IM_TEXT] as? String
        if (text != null) {
            sb.append(text.replace(NEW_LINE, NEW_LINE_SPACE_PREFIX))
        }

        return sb.toString()
    }
}

object LLChatLogParser {

    fun parse(raw: String, parseParams: Map<String, Any> = emptyMap()): Map<String, Any>? {
        if (raw.isEmpty()) return null

        val cutOffTodaysDate = parseParams["cut_off_todays_date"] as? Boolean ?: true
        val im = mutableMapOf<String, Any>()

        var hasSec = false
        var matchResult = TIMESTAMP_AND_STUFF_SEC.matchEntire(raw)
        if (matchResult != null) {
            hasSec = true
        } else {
            matchResult = TIMESTAMP_AND_STUFF.matchEntire(raw) ?: return null
        }

        val timestampGroup = matchResult.groupValues[1]
        val stuffGroup = matchResult.groupValues[2]

        if (timestampGroup.isNotEmpty()) {
            var timestamp = timestampGroup.trim().trimStart('[').trimEnd(']')
            im[LL_IM_DATE_TIME] = timestamp

            if (cutOffTodaysDate) {
                timestamp = LLLogChatTimeScanner.checkAndCutOffDate(timestamp, hasSec)
            }
            im[LL_IM_TIME] = timestamp
        } else {
            im[LL_IM_DATE_TIME] = ""
            im[LL_IM_TIME] = ""
        }

        if (stuffGroup.isEmpty()) return null

        val nameAndText = NAME_AND_TEXT.matchEntire(stuffGroup) ?: return null
        val nameGroup = nameAndText.groupValues[1]
        val textGroup = nameAndText.groupValues[3]

        val hasName = nameGroup.isNotEmpty()
        var name = nameGroup.removeSuffix(":")

        if (name == "IM:") {
            val dividerPos = stuffGroup.indexOf(NAME_TEXT_DIVIDER, 3)
            if (dividerPos != -1 && dividerPos < stuffGroup.length - NAME_TEXT_DIVIDER.length) {
                im[LL_IM_FROM] = stuffGroup.substring(0, dividerPos)
                im[LL_IM_TEXT] = stuffGroup.substring(dividerPos + NAME_TEXT_DIVIDER.length)
                return im
            }
        }

        if (!hasName || name == SYSTEM_FROM) {
            im[LL_IM_FROM] = SYSTEM_FROM
            im[LL_IM_FROM_ID] = "00000000-0000-0000-0000-000000000000"
        }

        if (!hasName) {
            val dividerPos = stuffGroup.indexOf(NAME_TEXT_DIVIDER)
            if (dividerPos != -1 && dividerPos < stuffGroup.length - NAME_TEXT_DIVIDER.length) {
                im[LL_IM_FROM] = stuffGroup.substring(0, dividerPos)
                im[LL_IM_TEXT] = stuffGroup.substring(dividerPos + NAME_TEXT_DIVIDER.length)
                return im
            }
            im[LL_IM_TEXT] = stuffGroup
            return im
        }

        if (textGroup.isEmpty()) return null

        im[LL_IM_FROM] = name
        im[LL_IM_TEXT] = textGroup
        return im
    }
}

private object LLLogChatTimeScanner {
    fun checkAndCutOffDate(timeStr: String, hasSec: Boolean): String {
        if (timeStr.length < 10) return timeStr
        System.err.println("LLLogChatTimeScanner: checkAndCutOffDate not yet implemented")
        return timeStr
    }
}
