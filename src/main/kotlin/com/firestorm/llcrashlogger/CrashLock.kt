package com.firestorm.llcrashlogger

import java.io.File

class CrashLock {
    private var cleanUp: Boolean = true
    private var master: String = ""
    private var dumpTable: String = ""
    private var waitingPid: UInt = 0U
    private var expiryMs: Long = 0L

    fun setCleanUp(cleanup: Boolean = true) {
        cleanUp = cleanup
    }

    fun setSaveName(savename: String) {
        dumpTable = savename
    }

    fun requestMaster(timeout: Float = 300.0f): Boolean {
        if (master.isEmpty()) {
            master = TODO("APR: use JVM equivalent for LL_PATH_LOGS/crash_master.lock")
        }

        val lockSd = getLockFile(master)
        if (lockSd.containsKey("pid")) {
            waitingPid = (lockSd["pid"] as? Long)?.toUInt() ?: 0U
            if (isProcessAlive(waitingPid, executableFilename())) {
                expiryMs = System.currentTimeMillis() + (timeout * 1000).toLong()
                return false
            }
        }

        val pid = ProcessHandle.current().pid().toUInt()
        val data = mutableMapOf<String, Any>("pid" to pid.toLong())
        return putLockFile(master, data)
    }

    fun checkMaster(): Boolean {
        if (waitingPid != 0U) {
            return !isProcessAlive(waitingPid, executableFilename())
        }
        return false
    }

    fun releaseMaster() {
        File(master).delete()
    }

    fun isLockPresent(filename: String): Boolean = fileExists(filename)

    fun isProcessAlive(pid: UInt, pname: String): Boolean {
        return ProcessHandle.allProcesses()
            .filter { it.pid() == pid.toLong() }
            .anyMatch { h -> h.info().command().orElse("").endsWith(pname) }
    }

    fun isWaiting(): Boolean = System.currentTimeMillis() < expiryMs

    fun getProcessList(): Map<String, Any> {
        if (dumpTable.isEmpty()) {
            dumpTable = TODO("APR: use JVM equivalent for LL_PATH_LOGS/crash_table.lock")
        }
        return getLockFile(dumpTable)
    }

    fun cleanupProcess(procDir: String) {
        val dir = File(procDir)
        if (dir.exists()) dir.deleteRecursively()
    }

    fun putProcessList(processList: Map<String, Any>): Boolean {
        return putLockFile(dumpTable, processList)
    }

    fun getPid(): Int = ProcessHandle.current().pid().toInt()

    private fun getLockFile(filename: String): MutableMap<String, Any> {
        val file = File(filename)
        if (!file.exists()) return mutableMapOf()
        return try {
            TODO("APR: parse XML lock file into map from $filename")
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun putLockFile(filename: String, data: Map<String, Any>): Boolean {
        return try {
            TODO("APR: serialize map to XML lock file at $filename")
        } catch (_: Exception) {
            false
        }
    }

    private fun executableFilename(): String {
        return ProcessHandle.current().info().command().orElse("")
            .substringAfterLast(File.separator)
    }

    companion object {
        fun fileExists(filename: String): Boolean = File(filename).exists()
    }
}
