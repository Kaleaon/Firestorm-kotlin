package com.firestorm.llcommon

// Top-level alias so tests (and callers) can refer to the thread-status enum
// without needing to know the nesting path LLThread.Status.
typealias LLThreadStatus = LLThread.Status

abstract class LLThread(var name: String) {
    enum class Status { STOPPED, RUNNING, QUEUED, PAUSED, QUITTING }

    @Volatile private var status: Status = Status.STOPPED
    @Volatile private var paused: Boolean = false

    private val dataLock = LLMutex()
    private val runCondition = LLCondition(dataLock)
    private var thread: Thread? = null

    abstract fun run()

    open fun runCondition(): Boolean = true

    fun start() {
        if (status != Status.STOPPED) return
        status = Status.RUNNING
        thread = Thread({
            try {
                checkPause()
                run()
            } finally {
                status = Status.STOPPED
            }
        }, name).also { it.isDaemon = true; it.start() }
    }

    fun pause() {
        dataLock.lock()
        paused = true
        dataLock.unlock()
    }

    fun resume() {
        dataLock.lock()
        paused = false
        runCondition.broadcast()
        dataLock.unlock()
    }

    fun stop() {
        dataLock.lock()
        status = Status.QUITTING
        runCondition.broadcast()
        dataLock.unlock()
    }

    fun isRunning(): Boolean = status == Status.RUNNING
    fun isStopped(): Boolean = status == Status.STOPPED
    fun isPaused(): Boolean = paused || status == Status.STOPPED
    fun isQuitting(): Boolean = status == Status.QUITTING

    fun checkPause() {
        dataLock.lock()
        while (shouldSleep()) {
            runCondition.await()
        }
        dataLock.unlock()
    }

    fun wake() {
        dataLock.lock()
        if (!shouldSleep()) runCondition.signal()
        dataLock.unlock()
    }

    fun wakeLocked() = runCondition.signal()

    private fun shouldSleep(): Boolean =
        (status == Status.RUNNING) && (paused || !runCondition())

    protected fun setQuitting() {
        dataLock.lock()
        if (status == Status.RUNNING) status = Status.QUITTING
        dataLock.unlock()
    }

    protected fun shouldRun(): Boolean = status != Status.QUITTING

    companion object {
        fun yield() = Thread.yield()
    }
}
