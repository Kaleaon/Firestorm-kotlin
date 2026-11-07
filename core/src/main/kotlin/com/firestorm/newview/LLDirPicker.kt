package com.firestorm.newview

import java.util.concurrent.LinkedBlockingDeque

class LLDirPicker {

    private companion object {
        const val SINGLE_DIRNAME_BUFFER_SIZE = 1024
        const val DIRNAME_BUFFER_SIZE = 65000
    }

    private var mFileName: String? = null
    private var mDir: String = ""
    private var mLocked: Boolean = false
    private var mEventListener: (() -> Unit)? = null

    private fun checkLocalFileAccessEnabled(): Boolean {
        return false
    }

    fun getDir(filename: String?, blocking: Boolean = true): Boolean {
        if (mLocked) return false

        if (!checkLocalFileAccessEnabled()) return false

        return false
    }

    fun getDirName(): String = mDir

    fun reset() {
        mDir = ""
        mFileName = null
        System.err.println("LLDirPicker: reset not yet implemented")
    }
}

class LLDirPickerThread(
    private val callback: ((filenames: List<String>, proposedName: String) -> Unit),
    val mProposedName: String
) : Thread("dir picker") {

    val mResponses: MutableList<String> = mutableListOf()

    companion object {
        val sDeadQ: LinkedBlockingDeque<LLDirPickerThread> = LinkedBlockingDeque()
        @Volatile var sMutex: Any? = Any()

        fun initClass() {
            sMutex = Any()
        }

        fun cleanupClass() {
            clearDead()
            sMutex = null
        }

        fun clearDead() {
            val mutex = sMutex ?: return
            synchronized(mutex) {
                while (sDeadQ.isNotEmpty()) {
                    val thread = sDeadQ.poll() ?: break
                    thread.notify(thread.mResponses)
                }
            }
        }
    }

    fun getFile() {
        System.err.println("LLDirPickerThread: getFile not yet implemented")
    }

    override fun run() {
        val picker = LLDirPicker()
        System.err.println("LLDirPickerThread: run not yet implemented")
    }

    fun notify(filenames: List<String>) {
        if (filenames.isNotEmpty()) {
            callback(filenames, mProposedName)
        }
    }
}
