package com.firestorm.llcommon

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class WorkQueue(val name: String, capacity: Int = 1024) {
    private val queue = LinkedBlockingQueue<() -> Unit>(capacity)
    @Volatile private var closed = false

    init {
        WorkQueues.register(name, this)
    }

    fun post(work: () -> Unit): Boolean {
        if (closed) return false
        return queue.offer(work)
    }

    fun postEvery(period: Duration, work: () -> Boolean) {
        fun schedule() {
            post {
                if (work()) {
                    Thread.sleep(period.toMillis())
                    schedule()
                }
            }
        }
        schedule()
    }

    fun runFor(ms: Long) {
        val deadline = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < deadline) {
            val work = queue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                ?: break
            try { work() } catch (_: Exception) {}
        }
    }

    fun runOne(): Boolean {
        val work = queue.poll() ?: return false
        try { work() } catch (_: Exception) {}
        return true
    }

    fun runUntilClose() {
        while (!closed || queue.isNotEmpty()) {
            val work = queue.poll(10, TimeUnit.MILLISECONDS) ?: continue
            try { work() } catch (_: Exception) {}
        }
    }

    fun runPending(): Boolean {
        val pending = mutableListOf<() -> Unit>()
        queue.drainTo(pending)
        for (work in pending) {
            try { work() } catch (_: Exception) {}
        }
        return !closed
    }

    fun isClosed(): Boolean = closed

    fun close() {
        closed = true
        WorkQueues.unregister(name)
    }

    fun size(): Int = queue.size
    fun isEmpty(): Boolean = queue.isEmpty()
}

object WorkQueues {
    private val registry = ConcurrentHashMap<String, WorkQueue>()

    internal fun register(name: String, queue: WorkQueue) {
        registry[name] = queue
    }

    internal fun unregister(name: String) {
        registry.remove(name)
    }

    fun get(name: String): WorkQueue? = registry[name]

    fun getOrCreate(name: String, capacity: Int = 1024): WorkQueue =
        registry.getOrPut(name) { WorkQueue(name, capacity) }
}
