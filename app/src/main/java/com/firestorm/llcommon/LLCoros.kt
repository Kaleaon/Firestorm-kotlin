package com.firestorm.llcommon

import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

// C++ used boost::fibers (cooperative, single-thread scheduler). The JVM equivalent
// that preserves fire-and-forget semantics without structural concurrency is a
// cached thread pool. Callers that relied on cooperative yield() should insert
// Thread.yield() explicitly; true fiber semantics require Project Loom virtual
// threads (Java 21+) and can be swapped in by replacing the executor below.

object LLCoros {
    // -------------------------------------------------------------------------
    // Stop exception hierarchy  (mirrors LLCoros::Stop in C++)
    // -------------------------------------------------------------------------
    open class Stop(message: String) : Exception(message)
    class Stopping(message: String) : Stop(message)
    class Stopped(message: String) : Stop(message)
    class Shutdown(message: String) : Stop(message)

    // -------------------------------------------------------------------------
    // Per-coroutine metadata  (mirrors LLCoros::CoroData in C++)
    // -------------------------------------------------------------------------
    data class CoroData(
        val name: String,
        val creationTimeMs: Long = System.currentTimeMillis(),
        @Volatile var consuming: Boolean = false,
        @Volatile var status: String = "",
        val future: Future<*>
    )

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------
    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "LLCoros").also { it.isDaemon = true }
    }
    private val active = ConcurrentHashMap<String, CoroData>()
    private val counter = AtomicInteger(0)
    private val currentName = ThreadLocal<String?>()
    private val exceptionQueue = ArrayDeque<Pair<String, Throwable>>()

    @Volatile private var stackSize: Int = 1024 * 1024  // 1 MiB default, matches C++

    // -------------------------------------------------------------------------
    // Main-coro detection
    // -------------------------------------------------------------------------

    fun onMainCoro(): Boolean = getName().isNullOrEmpty()

    fun onMainThreadMainCoro(): Boolean = onMainCoro() && Thread.currentThread().name == "main"

    // -------------------------------------------------------------------------
    // Launch
    // -------------------------------------------------------------------------

    fun launch(prefix: String, callable: () -> Unit): String {
        require(prefix.isNotEmpty()) { "LLCoros::launch(): pass non-empty name string" }
        val name = generateDistinctName(prefix)
        val future = executor.submit { toplevel(name, callable) }
        active[name] = CoroData(name = name, future = future)
        return name
    }

    // -------------------------------------------------------------------------
    // Name / status queries
    // -------------------------------------------------------------------------

    fun getName(): String? = currentName.get()

    fun logname(): String {
        val n = getName()
        return if (!n.isNullOrEmpty()) n else "main${Thread.currentThread().id}"
    }

    fun setStatus(status: String) {
        val name = getName() ?: return
        active[name]?.status = status
    }

    fun getStatus(): String = getName()?.let { active[it]?.status } ?: ""

    // -------------------------------------------------------------------------
    // Consuming flag
    // -------------------------------------------------------------------------

    fun setConsuming(consuming: Boolean) {
        val name = checkNotNull(getName()) { "setConsuming() called from main coroutine" }
        active[name]?.consuming = consuming
    }

    fun getConsuming(): Boolean = getName()?.let { active[it]?.consuming } ?: false

    // -------------------------------------------------------------------------
    // RAII wrappers as inline classes / lambdas (no C++ destructors in Kotlin,
    // use try/finally at the call-site or the helper functions below)
    // -------------------------------------------------------------------------

    inline fun withConsuming(consuming: Boolean, block: () -> Unit) {
        val prev = getConsuming()
        setConsuming(consuming)
        try {
            block()
        } finally {
            setConsuming(prev)
        }
    }

    inline fun withStatus(status: String, block: () -> Unit) {
        val prev = getStatus()
        setStatus(status)
        try {
            block()
        } finally {
            setStatus(prev)
        }
    }

    // -------------------------------------------------------------------------
    // Stack size
    // -------------------------------------------------------------------------

    fun setStackSize(stacksize: Int) {
        stackSize = stacksize
    }

    // -------------------------------------------------------------------------
    // Exception propagation  (mirrors rethrow() in C++)
    // -------------------------------------------------------------------------

    fun rethrow() {
        val front = synchronized(exceptionQueue) {
            if (exceptionQueue.isEmpty()) null else exceptionQueue.removeFirst()
        } ?: return
        llwarns("LLCoros") { "Rethrowing exception from coroutine ${front.first}" }
        throw front.second
    }

    // -------------------------------------------------------------------------
    // Shutdown check  (mirrors checkStop() in C++)
    // -------------------------------------------------------------------------

    fun checkStop() {
        // If the singleton has been torn down (no direct equivalent in Kotlin object),
        // throw Shutdown. We approximate by checking executor state.
        if (executor.isShutdown) throw Shutdown("LLCoros was deleted")
        val name = getName() ?: return
        if (name.isEmpty()) return
        // Callers that integrate with LLApp should override this to inspect app state.
    }

    // -------------------------------------------------------------------------
    // Diagnostics
    // -------------------------------------------------------------------------

    fun printActiveCoroutines(when_: String = "") {
        llinfos("LLCoros") { "Number of active coroutines $when_: ${active.size}" }
        if (active.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val sb = StringBuilder("-------------- List of active coroutines ------------\n")
            active.values.forEach { cd ->
                val lifeMs = now - cd.creationTimeMs
                sb.append("  ${cd.name} [${cd.status}] life: ${lifeMs / 1000.0}s\n")
            }
            sb.append("-----------------------------------------------------")
            llinfos("LLCoros") { sb.toString() }
        }
    }

    fun getCount(): Int = active.size

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    fun cleanupSingleton() {
        printActiveCoroutines("at entry to ~LLCoros()")
        // Pump remaining fibers up to 10 times; on the JVM we just join briefly.
        repeat(10) {
            if (active.isEmpty()) return@repeat
            Thread.yield()
        }
        printActiveCoroutines("after pumping")
        executor.shutdownNow()
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun generateDistinctName(prefix: String): String {
        var candidate = prefix
        var n = 0
        while (active.containsKey(candidate)) {
            candidate = "$prefix${n++}"
        }
        return candidate
    }

    private fun toplevel(name: String, callable: () -> Unit) {
        currentName.set(name)
        try {
            callable()
        } catch (e: Stop) {
            llinfos("LLCoros") { "coroutine $name terminating because ${e.message}" }
        } catch (e: CancellationException) {
            // normal cancellation from kill()
        } catch (e: Exception) {
            llwarns("LLCoros") { "Capturing uncaught exception in coroutine $name" }
            synchronized(exceptionQueue) { exceptionQueue.addLast(name to e) }
        } finally {
            active.remove(name)
            currentName.set(null)
        }
    }
}

// Top-level alias matching the C++ `llcoro::logname()` free function.
fun llcoroLogname(): String = LLCoros.logname()
