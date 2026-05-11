package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID

class GestureListener {

    init {
        register("getActiveGestures", ::getActiveGestures,
            requiredReplyKey = true)
        register("isGesturePlaying", ::isGesturePlaying)
        register("startGesture", ::startGesture)
        register("stopGesture", ::stopGesture)
    }

    private fun getActiveGestures(eventData: LLSD): LLSD {
        val activeGestures = GestureMgr.getActiveGestures()
        val gestureMap = mutableMapOf<String, LLSD>()

        for ((id, gesture) in activeGestures) {
            gesture ?: continue
            val info = LLSD.LLSDMap(
                mapOf(
                    "name"    to LLSD.LLSDString(gesture.name),
                    "trigger" to LLSD.LLSDString(gesture.trigger),
                    "playing" to LLSD.LLSDBoolean(gesture.isPlaying)
                )
            )
            gestureMap[id.toString()] = info
        }

        val reply = LLSD.LLSDMap(
            mapOf("gestures" to LLSD.LLSDMap(gestureMap))
        )
        return sendReply(reply, eventData)
    }

    private fun isGesturePlaying(eventData: LLSD): LLSD {
        var isPlaying = false

        val idValue = (eventData as? LLSD.LLSDMap)?.value?.get("id")
        if (idValue != null) {
            val gestureId = idValue.asUUID()
            if (gestureId != LLUUID.NULL) {
                isPlaying = GestureMgr.isGesturePlaying(gestureId)
            }
        }

        val reply = LLSD.LLSDMap(mapOf("playing" to LLSD.LLSDBoolean(isPlaying)))
        return sendReply(reply, eventData)
    }

    private fun startGesture(eventData: LLSD): LLSD {
        return startOrStopGesture(eventData, start = true)
    }

    private fun stopGesture(eventData: LLSD): LLSD {
        return startOrStopGesture(eventData, start = false)
    }

    private fun startOrStopGesture(eventData: LLSD, start: Boolean): LLSD {
        val idValue = (eventData as? LLSD.LLSDMap)?.value?.get("id")
        if (idValue != null) {
            val gestureId = idValue.asUUID()
            if (gestureId != LLUUID.NULL) {
                if (start) {
                    GestureMgr.playGesture(gestureId)
                } else {
                    GestureMgr.stopGesture(gestureId)
                }
            }
        }
        return LLSD.Undefined
    }

    private fun register(
        name: String,
        handler: (LLSD) -> LLSD,
        requiredReplyKey: Boolean = false
    ) {
        TODO("APR: register handler '$name' with LLEventAPI dispatch table; requiredReplyKey=$requiredReplyKey")
    }

    private fun sendReply(reply: LLSD, eventData: LLSD): LLSD {
        TODO("APR: extract reply pump name from eventData['reply'], post reply LLSD to that pump via LLEventPumps")
        @Suppress("UNREACHABLE_CODE")
        return reply
    }
}
