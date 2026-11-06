package com.firestorm.llcommon

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

typealias EventListener = (LLSD) -> Boolean

abstract class EventPump(val name: String) {
    protected val listeners = LinkedHashMap<String, EventListener>()
    private val lock = Any()

    abstract fun post(event: LLSD): Boolean

    fun listen(name: String, listener: EventListener): Connection {
        synchronized(lock) {
            listeners[name] = listener
        }
        return Connection(name)
    }

    fun stopListening(name: String) {
        synchronized(lock) {
            listeners.remove(name)
        }
    }

    inner class Connection(val listenerName: String) {
        fun disconnect() = stopListening(listenerName)
    }
}

class EventStream(name: String) : EventPump(name) {
    override fun post(event: LLSD): Boolean {
        val snapshot = synchronized(listeners) { listeners.values.toList() }
        var consumed = false
        for (listener in snapshot) {
            try {
                if (listener(event)) {
                    consumed = true
                    break
                }
            } catch (_: Exception) {
            }
        }
        return consumed
    }
}

class EventQueue(name: String) : EventPump(name) {
    private val queue = LinkedBlockingQueue<LLSD>()

    override fun post(event: LLSD): Boolean {
        return queue.offer(event)
    }

    fun flush() {
        val snapshot = synchronized(listeners) { listeners.values.toList() }
        val events = mutableListOf<LLSD>()
        queue.drainTo(events)
        for (event in events) {
            for (listener in snapshot) {
                try {
                    if (listener(event)) break
                } catch (_: Exception) {
                }
            }
        }
    }
}

object EventPumps {
    private val pumps = ConcurrentHashMap<String, EventPump>()

    fun obtain(name: String): EventPump =
        pumps.getOrPut(name) { EventStream(name) }

    fun make(name: String, queued: Boolean = false): EventPump {
        val pump: EventPump = if (queued) EventQueue(name) else EventStream(name)
        pumps[name] = pump
        return pump
    }

    fun post(name: String, event: LLSD): Boolean =
        pumps[name]?.post(event) ?: false
}
