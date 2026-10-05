package com.firestorm.newview

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.UUID

typealias LLSDMap = MutableMap<String, Any?>

object LLExperienceLog {

    private val SECONDS_IN_DAY = 24 * 60 * 60L
    private val DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var maxDays: UInt = 7u
        set(value) { field = value }

    var notifyNewEvent: Boolean = false
        set(value) {
            field = value
            if (!value) {
                notifyConnection = null
            } else if (notifyConnection == null) {
                notifyConnection = { msg: MutableMap<String, Any?> -> notify(msg) }
                signals.add(notifyConnection!!)
            }
        }

    var pageSize: UInt = 25u

    private val signals: MutableList<(MutableMap<String, Any?>) -> Unit> = mutableListOf()
    private var notifyConnection: ((MutableMap<String, Any?>) -> Unit)? = null

    var events: MutableMap<String, MutableList<MutableMap<String, Any?>>> = mutableMapOf()
        private set
    private var eventsToSave: MutableMap<String, MutableList<MutableMap<String, Any?>>> = mutableMapOf()

    fun addUpdateSignal(cb: (MutableMap<String, Any?>) -> Unit): (MutableMap<String, Any?>) -> Unit {
        signals.add(cb)
        return cb
    }

    fun removeUpdateSignal(cb: (MutableMap<String, Any?>) -> Unit) {
        signals.remove(cb)
    }

    fun initialize() {
        loadEvents()
        System.err.println("LLExperienceLog: initialize dispatch handler registration not yet implemented")
    }

    fun handleExperienceMessage(message: MutableMap<String, Any?>) {
        val now = java.time.LocalDateTime.now()
        val day = now.format(DAY_FORMATTER)
        val timeOfDay = now.format(DateTimeFormatter.ofPattern(" HH:mm:ss"))
        message["Time"] = timeOfDay

        val dayEvents = events.getOrPut(day) { mutableListOf() }

        if (dayEvents.isNotEmpty()) {
            val last = dayEvents.last()
            val sameExperience =
                (last["public_id"] as? UUID) == (message["public_id"] as? UUID) &&
                (last["ObjectName"] as? String) == (message["ObjectName"] as? String) &&
                (last["OwnerID"] as? UUID) == (message["OwnerID"] as? UUID) &&
                (last["ParcelName"] as? String) == (message["ParcelName"] as? String) &&
                (last["Permission"] as? Int) == (message["Permission"] as? Int)

            if (sameExperience) {
                last["Count"] = ((last["Count"] as? Int) ?: 0) + 1
                last["Time"] = timeOfDay
                signals.forEach { it(last) }
                return
            }
        }

        message["Time"] = timeOfDay
        dayEvents.add(message)
        eventsToSave.getOrPut(day) { mutableListOf() }.add(message)
        signals.forEach { it(message) }
    }

    fun clear() {
        events.clear()
    }

    fun setEventsToSave(newEvents: MutableMap<String, MutableList<MutableMap<String, Any?>>>) {
        eventsToSave = newEvents
    }

    fun isNotExpired(date: String): Boolean {
        return try {
            val eventDate = LocalDate.parse(date, DAY_FORMATTER)
            val today = LocalDate.now()
            val boundaryDate = today.minusDays(maxDays.toLong())
            !eventDate.isBefore(boundaryDate)
        } catch (e: DateTimeParseException) {
            false
        }
    }

    fun getFilename(): String {
        System.err.println("LLExperienceLog: getFilename not yet implemented")
        return ""
    }

    fun notify(message: MutableMap<String, Any?>) {
        message["EventType"] = getPermissionString(message, "ExperiencePermission")
        val isAttachment = (message["IsAttachment"] as? Boolean) == true
        if (isAttachment) {
            System.err.println("LLExperienceLog: notify ExperienceEventAttachment not yet implemented")
        } else {
            System.err.println("LLExperienceLog: notify ExperienceEvent not yet implemented")
        }
        message.remove("EventType")
    }

    fun getPermissionString(message: MutableMap<String, Any?>, base: String): String {
        val permission = message["Permission"] as? Int
        if (permission != null) {
            System.err.println("LLExperienceLog: getPermissionString translation not yet implemented")
            return ""
        }
        System.err.println("LLExperienceLog: getPermissionString unknown-permission translation not yet implemented")
        return ""
    }

    protected fun loadEvents() {
        System.err.println("LLExperienceLog: loadEvents not yet implemented")
        // After loading, call eraseExpired() and assign eventsToSave = events
    }

    protected fun saveEvents() {
        System.err.println("LLExperienceLog: saveEvents not yet implemented")
    }

    protected fun eraseExpired() {
        if (maxDays > 0u) {
            val toRemove = events.keys.filter { date -> !isNotExpired(date) }
            toRemove.forEach { events.remove(it) }
        }
    }

    fun shutdown() {
        saveEvents()
    }
}

class LLExperienceLogDispatchHandler {
    operator fun invoke(
        dispatcher: Any?,
        key: String,
        invoice: UUID,
        strings: List<String>
    ): Boolean {
        val message: MutableMap<String, Any?> = mutableMapOf()
        val iter = strings.iterator()

        if (iter.hasNext()) {
            val llsdRaw = iter.next()
            System.err.println("LLExperienceLogDispatchHandler: LLSD deserialization not yet implemented")
        }

        message["public_id"] = invoice

        if (iter.hasNext()) {
            message["ObjectName"] = iter.next()
        }
        if (iter.hasNext()) {
            message["ParcelName"] = iter.next()
        }

        message["Count"] = 1
        LLExperienceLog.handleExperienceMessage(message)
        return true
    }
}
