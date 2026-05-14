package com.firestorm.newview

import java.util.UUID

// LLEventAPI base — pumps named events from the viewer event system to registered handlers.
// In this JVM port each handler is a lambda stored in the registry map.
abstract class LLEventAPI(val name: String, val description: String) {

    protected data class Handler(val description: String, val fn: (Map<String, Any?>) -> Unit)

    private val handlers: MutableMap<String, Handler> = mutableMapOf()

    protected fun add(commandName: String, description: String, fn: (Map<String, Any?>) -> Unit) {
        handlers[commandName] = Handler(description, fn)
    }

    fun dispatch(commandName: String, eventData: Map<String, Any?>) {
        handlers[commandName]?.fn?.invoke(eventData)
            ?: println("WARN $name: unknown command '$commandName'")
    }

    // Sends a reply map back through the event pump, keyed by the "reply" field in eventData.
    protected fun sendReply(reply: Map<String, Any?>, eventData: Map<String, Any?>) {
        System.err.println("LLEventAPI: sendReply not yet implemented")
    }
}

// Stub representing a multi-gesture loaded in memory.
data class LLMultiGesture(
    val mName: String,
    val mTrigger: String,
    val mPlaying: Boolean
)

// Stub for the gesture manager singleton — full implementation lives in GestureMgr.kt.
object LLGestureMgr {
    fun getActiveGestures(): Map<UUID, LLMultiGesture?> {
        return emptyMap()
    }

    fun isGesturePlaying(gestureId: UUID): Boolean {
        return false
    }

    fun playGesture(gestureId: UUID) {
        System.err.println("LLGestureMgr: playGesture not yet implemented")
    }

    fun stopGesture(gestureId: UUID) {
        System.err.println("LLGestureMgr: stopGesture not yet implemented")
    }
}

class LLGestureListener : LLEventAPI(
    "LLGesture",
    "LLGesture listener interface to control gestures"
) {

    init {
        add(
            "getActiveGestures",
            "Return information about the agent's available gestures [\"reply\"]:\n" +
            "[\"gestures\"]: a dictionary with UUID strings as keys\n" +
            "  and the following dict values for each entry:\n" +
            "     [\"name\"]: name of the gesture, may be empty\n" +
            "     [\"trigger\"]: trigger string used to invoke via user chat, may be empty\n" +
            "     [\"playing\"]: true or false indicating the playing state"
        ) { eventData -> getActiveGestures(eventData) }

        add(
            "isGesturePlaying",
            "[\"id\"]: UUID of the gesture to query.  Returns True or False in [\"playing\"] value of the result"
        ) { eventData -> isGesturePlaying(eventData) }

        add(
            "startGesture",
            "[\"id\"]: UUID of the gesture to start playing"
        ) { eventData -> startGesture(eventData) }

        add(
            "stopGesture",
            "[\"id\"]: UUID of the gesture to stop"
        ) { eventData -> stopGesture(eventData) }
    }

    private fun getActiveGestures(eventData: Map<String, Any?>) {
        val gestureMap = mutableMapOf<String, Any?>()
        val activeGestures = LLGestureMgr.getActiveGestures()
        for ((id, gesture) in activeGestures) {
            if (gesture != null) {
                gestureMap[id.toString()] = mapOf(
                    "name" to gesture.mName,
                    "trigger" to gesture.mTrigger,
                    "playing" to gesture.mPlaying
                )
            }
        }
        val reply = mapOf<String, Any?>("gestures" to gestureMap)
        sendReply(reply, eventData)
    }

    private fun isGesturePlaying(eventData: Map<String, Any?>) {
        var isPlaying = false
        val idValue = eventData["id"]
        if (idValue != null) {
            val gestureId = runCatching { UUID.fromString(idValue.toString()) }.getOrNull()
            if (gestureId != null && gestureId != UUID(0L, 0L)) {
                isPlaying = LLGestureMgr.isGesturePlaying(gestureId)
            } else {
                println("WARN LLGestureListener: isGesturePlaying did not find a gesture object for $gestureId")
            }
        } else {
            println("WARN LLGestureListener: isGesturePlaying didn't have 'id' value passed in")
        }
        val reply = mapOf<String, Any?>("playing" to isPlaying)
        sendReply(reply, eventData)
    }

    private fun startGesture(eventData: Map<String, Any?>) {
        startOrStopGesture(eventData, start = true)
    }

    private fun stopGesture(eventData: Map<String, Any?>) {
        startOrStopGesture(eventData, start = false)
    }

    private fun startOrStopGesture(eventData: Map<String, Any?>, start: Boolean) {
        val idValue = eventData["id"]
        if (idValue != null) {
            val gestureId = runCatching { UUID.fromString(idValue.toString()) }.getOrNull()
            if (gestureId != null && gestureId != UUID(0L, 0L)) {
                if (start) {
                    LLGestureMgr.playGesture(gestureId)
                } else {
                    LLGestureMgr.stopGesture(gestureId)
                }
            } else {
                println("WARN LLGestureListener: startOrStopGesture did not find a gesture object for $gestureId")
            }
        } else {
            println("WARN LLGestureListener: startOrStopGesture didn't have 'id' value passed in")
        }
    }
}
