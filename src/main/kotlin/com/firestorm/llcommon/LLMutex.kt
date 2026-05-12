package com.firestorm.llcommon

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LLMutex {
    private val lock = ReentrantLock()

    fun lock() = lock.lock()
    fun unlock() = lock.unlock()
    fun tryLock(): Boolean = lock.tryLock()
    fun isLocked(): Boolean = lock.isLocked
    fun isHeldByCurrentThread(): Boolean = lock.isHeldByCurrentThread

    fun <T> withLock(block: () -> T): T = lock.withLock(block)

    internal fun newCondition(): java.util.concurrent.locks.Condition = lock.newCondition()
}

class LLCondition(private val mutex: LLMutex) {
    private val condition = mutex.newCondition()

    fun await() = condition.await()
    fun signal() = condition.signal()
    fun broadcast() = condition.signalAll()
}

class LLMutexLock(private val mutex: LLMutex) : AutoCloseable {
    init { mutex.lock() }
    override fun close() { mutex.unlock() }
}

fun <T> LLMutex.locked(block: () -> T): T = withLock(block)
