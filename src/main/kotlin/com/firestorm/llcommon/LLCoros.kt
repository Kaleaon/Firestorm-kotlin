package com.firestorm.llcommon

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class CoroData(val name: String, val future: Future<*>)

object LLCoros {
    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r).also { it.isDaemon = true }
    }
    private val active = ConcurrentHashMap<String, CoroData>()
    private val counter = AtomicInteger(0)
    private val currentName = ThreadLocal<String?>()

    fun launch(name: String, body: () -> Unit): String {
        val unique = uniqueName(name)
        val future = executor.submit {
            currentName.set(unique)
            try {
                body()
            } finally {
                active.remove(unique)
                currentName.set(null)
            }
        }
        active[unique] = CoroData(unique, future)
        return unique
    }

    fun getName(): String? = currentName.get()

    fun yield() = Thread.yield()

    fun isRunning(name: String): Boolean {
        val coro = active[name] ?: return false
        return !coro.future.isDone
    }

    fun kill(name: String): Boolean {
        val coro = active.remove(name) ?: return false
        coro.future.cancel(true)
        return true
    }

    fun getCount(): Int = active.size

    fun printActive() {
        active.keys.forEach { println("  coroutine: $it") }
    }

    private fun uniqueName(prefix: String): String {
        val base = prefix.ifEmpty { "coro" }
        var candidate = base
        var n = 0
        while (active.containsKey(candidate)) {
            candidate = "$base#${++n}"
        }
        return candidate
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
