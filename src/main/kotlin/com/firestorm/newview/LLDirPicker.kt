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
        TODO("APR: use JVM equivalent of gSavedSettings.getBOOL(\"LocalFileSystemBrowsingEnabled\"); clear mDir/mFileName and return false when disabled")
    }

    fun getDir(filename: String?, blocking: Boolean = true): Boolean {
        if (mLocked) return false

        if (!checkLocalFileAccessEnabled()) return false

        TODO("APR: use JVM equivalent of platform dir-picker dialog (JFileChooser or JVM desktop API); on Windows use IFileDialog with FOS_PICKFOLDERS; on macOS/Linux delegate to LLFilePicker with FFLOAD_DIRECTORY; store result in mDir; call sendAgentPause/Resume when blocking; update LLFrameTimer after modal")
    }

    fun getDirName(): String = mDir

    fun reset() {
        mDir = ""
        mFileName = null
        TODO("APR: use JVM equivalent — if a native dialog handle is open, close it")
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
        TODO("APR: use JVM equivalent — on Windows start() for non-blocking; on other platforms run() directly (modal)")
    }

    override fun run() {
        val picker = LLDirPicker()
        TODO("APR: use JVM equivalent of platform blocking flag; call picker.getDir(mProposedName, blocking); if successful push picker.getDirName() into mResponses; then synchronized(sMutex!!) { sDeadQ.push(this) }")
    }

    fun notify(filenames: List<String>) {
        if (filenames.isNotEmpty()) {
            callback(filenames, mProposedName)
        }
    }
}
