/**
 * LLPumps.kt
 * Kotlin port of the Linden Lab event-pump system (llpumps.h / llpumps.cpp).
 *
 * The event-pump system provides a named, loosely-coupled publish/subscribe
 * bus used throughout the viewer codebase.  `LLPumps` is the singleton
 * registry; individual pumps are subclasses of `LLPump`.
 *
 * NOTE: llpumps.h was not present in the indra/llmessage/ source tree;
 * this file is authored from the task specification and the patterns visible
 * in the rest of the codebase.
 */
package com.firestorm.llmessage

import com.firestorm.llcommon.LLSD

// ---------------------------------------------------------------------------
// Listener handle
// ---------------------------------------------------------------------------

/**
 * A disconnect handle returned by [LLPump.listen].
 *
 * Calling [disconnect] removes the listener from its pump, mirroring the
 * C++ `LLBoundListener` (a `boost::signals2::connection` wrapper).
 */
class LLBoundListener(
    private val pump: LLPump,
    private val listenerName: String
) {
    /** Remove this listener from its pump. Safe to call more than once. */
    fun disconnect() {
        pump.stopListening(listenerName)
    }

    /** Whether the listener is still connected. */
    val connected: Boolean get() = pump.hasListener(listenerName)
}

// ---------------------------------------------------------------------------
// Base pump class
// ---------------------------------------------------------------------------

/**
 * `LLPump` — base class for all event pumps.
 *
 * Listeners are keyed by an arbitrary [String] name, allowing them to be
 * removed by name later.  Dispatch order is determined by the order in which
 * listeners were registered (FIFO, matching the C++ `boost::signals2` default
 * for `at_back`).
 *
 * A listener returns `true` to stop further propagation (consume the event),
 * or `false` to pass it on to subsequent listeners.
 */
open class LLPump(val name: String) {

    protected val listeners: LinkedHashMap<String, (LLSD) -> Boolean> = LinkedHashMap()

    /**
     * Register a named listener on this pump.
     *
     * If a listener with [listenerName] already exists it is silently replaced
     * (matches C++ `connect` behaviour for duplicate names when the pump was
     * built with `allow_same_name`).
     *
     * @return An [LLBoundListener] that can be used to disconnect the listener.
     */
    fun listen(listenerName: String, callable: (LLSD) -> Boolean): LLBoundListener {
        listeners[listenerName] = callable
        return LLBoundListener(this, listenerName)
    }

    /**
     * Remove a listener by name.  No-op if no listener with that name exists.
     */
    fun stopListening(listenerName: String) {
        listeners.remove(listenerName)
    }

    /** Returns true if a listener with [listenerName] is currently registered. */
    fun hasListener(listenerName: String): Boolean = listeners.containsKey(listenerName)

    /**
     * Post an event to this pump.
     *
     * The default implementation dispatches [event] to all registered
     * listeners in registration order.  A listener that returns `true`
     * stops propagation; the method then returns `true` to signal that the
     * event was consumed.
     *
     * @return `true` if any listener consumed the event, `false` otherwise.
     */
    open fun post(event: LLSD): Boolean {
        for (listener in listeners.values) {
            if (listener(event)) return true
        }
        return false
    }
}

// ---------------------------------------------------------------------------
// Concrete pump types
// ---------------------------------------------------------------------------

/**
 * `LLEventStream` — fire-and-forget pump.
 *
 * Events are dispatched immediately and synchronously in [post].  This is the
 * most common pump type; it is equivalent to the C++ `LLEventStream`.
 */
class LLEventStream(name: String) : LLPump(name) {
    // Inherits synchronous dispatch from LLPump.post() with no modifications.
}

/**
 * `LLEventQueue` — queued-dispatch pump.
 *
 * Events posted via [post] are enqueued rather than dispatched immediately.
 * Call [pump] to drain one event from the head of the queue, or [flush] to
 * drain all pending events.  Mirrors the C++ `LLEventQueue`.
 */
class LLEventQueue(name: String) : LLPump(name) {

    private val queue: ArrayDeque<LLSD> = ArrayDeque()

    /**
     * Enqueue [event] without dispatching it yet.
     * Always returns `false` (the event has not been consumed yet).
     */
    override fun post(event: LLSD): Boolean {
        queue.addLast(event)
        return false
    }

    /**
     * Dispatch the next queued event to all listeners.
     *
     * @return `true` if an event was dequeued and dispatched, `false` if the
     *         queue was empty.
     */
    fun pump(): Boolean {
        val event = queue.removeFirstOrNull() ?: return false
        for (listener in listeners.values) {
            if (listener(event)) break
        }
        return true
    }

    /**
     * Drain all queued events.  Returns the number of events dispatched.
     */
    fun flush(): Int {
        var count = 0
        while (pump()) count++
        return count
    }

    /** Number of events currently in the queue. */
    val queueSize: Int get() = queue.size
}

// ---------------------------------------------------------------------------
// Singleton pump registry
// ---------------------------------------------------------------------------

/**
 * `LLPumps` — global registry of named event pumps.
 *
 * Mirrors the C++ `LLEventPumps` / `LLPumps` singleton.  Pumps are created
 * on first access via [obtain].
 */
object LLPumps {

    private val pumps: MutableMap<String, LLPump> = mutableMapOf()

    /**
     * Return the pump named [name], creating an [LLEventStream] for it if it
     * does not yet exist.
     *
     * Equivalent to C++ `LLEventPumps::instance().obtain(name)`.
     */
    fun obtain(name: String): LLPump = pumps.getOrPut(name) { LLEventStream(name) }

    /**
     * Return an existing pump by [name], or `null` if none has been created.
     */
    fun get(name: String): LLPump? = pumps[name]

    /**
     * Register an externally-constructed pump.
     *
     * Throws [IllegalArgumentException] if a pump with that name is already
     * registered (prevents accidental shadowing).
     */
    fun registerPump(pump: LLPump) {
        require(pump.name !in pumps) {
            "A pump named '${pump.name}' is already registered"
        }
        pumps[pump.name] = pump
    }

    /**
     * Remove a pump from the registry by name.
     *
     * The pump's listeners are left intact; they just will no longer receive
     * events posted through the registry.
     */
    fun unregisterPump(name: String) {
        pumps.remove(name)
    }

    /**
     * Log a status summary of all registered pumps and their listener counts.
     *
     * Mirrors C++ `LLEventPumps::status()`.
     */
    fun status() {
        println("LLPumps status: ${pumps.size} pump(s) registered")
        for ((name, pump) in pumps) {
            println("  pump '$name': ${pump.listeners.size} listener(s)")
        }
    }

    /** Number of pumps currently registered. */
    val count: Int get() = pumps.size
}
