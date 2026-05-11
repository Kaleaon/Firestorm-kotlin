package com.firestorm.llcommon

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
            runCondition.wait()
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

    companion object {
        fun yield() = Thread.yield()
    }
}

class LLMutex {
    private val lock = ReentrantLock()

    fun lock() = lock.lock()
    fun unlock() = lock.unlock()
    fun tryLock(): Boolean = lock.tryLock()

    fun <T> withLock(block: () -> T): T = lock.withLock(block)

    internal fun newCondition(): java.util.concurrent.locks.Condition = lock.newCondition()
    internal fun isHeldByCurrentThread(): Boolean = lock.isHeldByCurrentThread
}

class LLCondition(private val mutex: LLMutex) {
    private val condition = mutex.newCondition()

    fun wait() = condition.await()
    fun signal() = condition.signal()
    fun broadcast() = condition.signalAll()
}
