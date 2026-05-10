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
        TODO("APR: register ExperienceEvent dispatch handler via JVM equivalent of gGenericDispatcher")
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
        TODO("APR: use JVM equivalent of gDirUtilp->getExpandedFilename(LL_PATH_PER_SL_ACCOUNT, \"experience_events.xml\")")
    }

    fun notify(message: MutableMap<String, Any?>) {
        message["EventType"] = getPermissionString(message, "ExperiencePermission")
        val isAttachment = (message["IsAttachment"] as? Boolean) == true
        if (isAttachment) {
            TODO("APR: LLNotificationsUtil::add(\"ExperienceEventAttachment\", message)")
        } else {
            TODO("APR: LLNotificationsUtil::add(\"ExperienceEvent\", message)")
        }
        message.remove("EventType")
    }

    fun getPermissionString(message: MutableMap<String, Any?>, base: String): String {
        val permission = message["Permission"] as? Int
        if (permission != null) {
            val key = "$base$permission"
            TODO("APR: look up translated string for key \"$key\" via LLTrans equivalent")
        }
        TODO("APR: look up translated string for \"${base}Unknown\" via LLTrans equivalent")
    }

    protected fun loadEvents() {
        TODO("APR: use JVM equivalent of llifstream + LLSDSerialize::fromXMLDocument to load experience_events.xml")
        // After loading, call eraseExpired() and assign eventsToSave = events
    }

    protected fun saveEvents() {
        TODO("APR: use JVM equivalent of llofstream + LLSDSerialize::toPrettyXML to persist events, maxDays, notifyNewEvent, pageSize")
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
            TODO("APR: deserialize LLSD from llsdRaw string into message map")
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
