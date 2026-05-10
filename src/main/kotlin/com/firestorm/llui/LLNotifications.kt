package com.firestorm.llui

import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias Responder = (notification: Map<String, Any>, response: Map<String, Any>) -> Unit

interface ResponderInterface {
    fun handleRespond(notification: Map<String, Any>, response: Map<String, Any>)
    fun asMap(): Map<String, Any>
    fun fromMap(params: Map<String, Any>)
}

class NotificationContext {
    val id: UUID = UUID.randomUUID()
}

enum class NotifIgnoreType {
    CHECKBOX_ONLY,
    NO,
    WITH_DEFAULT_RESPONSE,
    WITH_DEFAULT_RESPONSE_SESSION_ONLY,
    WITH_LAST_RESPONSE,
    SHOW_AGAIN
}

data class NotifFormElement(
    val type: String,
    val name: String,
    val text: String,
    val value: Any? = null,
    val index: Int = 0,
    var enabled: Boolean = true,
    val isDefault: Boolean = false
)

class NotificationForm() {
    private val formData: MutableList<NotifFormElement> = mutableListOf()
    var ignoreType: NotifIgnoreType = NotifIgnoreType.NO
        private set
    var ignoreMessage: String = ""
        private set
    private var ignoreSetting: Boolean = true
    private var invertSetting: Boolean = false

    constructor(other: NotificationForm) : this() {
        formData.addAll(other.formData)
        ignoreType = other.ignoreType
        ignoreMessage = other.ignoreMessage
        ignoreSetting = other.ignoreSetting
        invertSetting = other.invertSetting
    }

    fun getNumElements(): Int = formData.size

    fun getElement(index: Int): NotifFormElement? = formData.getOrNull(index)

    fun getElement(elementName: String): NotifFormElement? = formData.find { it.name == elementName }

    fun getElements(offset: Int = 0): List<NotifFormElement> = formData.drop(offset)

    fun hasElement(elementName: String): Boolean = formData.any { it.name == elementName }

    fun getElementEnabled(elementName: String): Boolean =
        formData.find { it.name == elementName }?.enabled ?: false

    fun setElementEnabled(elementName: String, enabled: Boolean) {
        formData.find { it.name == elementName }?.enabled = enabled
    }

    fun addElement(type: String, name: String, value: Any? = null, enabled: Boolean = true) {
        formData.add(
            NotifFormElement(
                type = type,
                name = name,
                text = name,
                value = value,
                index = formData.size,
                enabled = enabled
            )
        )
    }

    fun formatElements(substitutions: Map<String, String>) {
        for (i in formData.indices) {
            val el = formData[i]
            var text = el.text
            for ((k, v) in substitutions) text = text.replace("[$k]", v)
            val value: Any? = if (el.type == "text" && el.value is String) {
                var v: String = el.value
                for ((k, rep) in substitutions) v = v.replace("[$k]", rep)
                v
            } else {
                el.value
            }
            formData[i] = el.copy(text = text, value = value)
        }
    }

    fun append(subForm: List<NotifFormElement>) {
        formData.addAll(subForm)
    }

    fun getDefaultOption(): String =
        formData.find { it.isDefault }?.name ?: ""

    fun getIgnored(): Boolean {
        if (ignoreType == NotifIgnoreType.NO) return false
        return if (invertSetting) ignoreSetting else !ignoreSetting
    }

    fun setIgnored(ignored: Boolean) {
        ignoreSetting = if (invertSetting) ignored else !ignored
    }
}

enum class ResponseTemplateType {
    WITHOUT_DEFAULT_BUTTON,
    WITH_DEFAULT_BUTTON
}

class NotificationEntry(
    val name: String,
    val id: UUID = UUID.randomUUID(),
    val substitutions: MutableMap<String, Any> = mutableMapOf(),
    val payload: MutableMap<String, Any> = mutableMapOf(),
    val priority: NotificationPriority = NotificationPriority.UNSPECIFIED,
    val timestamp: Instant = Instant.now(),
    var expiresAt: Instant = Instant.EPOCH,
    val offerFromAgent: Boolean = false,
    var isDnd: Boolean = false,
    responderName: String = "",
    responder: ResponderInterface? = null,
    formElements: List<NotifFormElement> = emptyList()
) {
    private var cancelled: Boolean = false
    private var respondedTo: Boolean = false
    private var response: MutableMap<String, Any> = mutableMapOf()
    private var ignored: Boolean = false
    private var effectivePriority: NotificationPriority = priority
    private var form: NotificationForm = NotificationForm()
    private var responseFunctorName: String = responderName
    private var temporaryResponder: Boolean = false
    private var responderObj: ResponderInterface? = responder
    val combinedNotifications: MutableList<NotificationEntry> = mutableListOf()
    private var templatep: NotificationTemplate? = null

    init {
        initFromTemplate(name, formElements)
    }

    private fun initFromTemplate(templateName: String, extra: List<NotifFormElement>) {
        templatep = Notifications.getTemplate(templateName)
        val tmpl = templatep ?: return

        substitutions["_URL"] = getUrl()
        substitutions["_NAME"] = templateName

        form = NotificationForm(tmpl.form)
        form.append(extra)
        form.formatElements(substitutions.mapValues { it.value.toString() })

        ignored = form.getIgnored()

        if (tmpl.expireSeconds > 0u) {
            expiresAt = Instant.now().plusSeconds(tmpl.expireSeconds.toLong())
        }

        if (effectivePriority == NotificationPriority.UNSPECIFIED) {
            effectivePriority = tmpl.priority
        }
    }

    fun isCancelled(): Boolean = cancelled
    fun isRespondedTo(): Boolean = respondedTo
    fun isActive(): Boolean = !isRespondedTo() && !isCancelled() && !isExpired()
    fun isIgnored(): Boolean = ignored
    fun getResponse(): Map<String, Any> = response
    fun getForm(): NotificationForm = form
    fun updateForm(newForm: NotificationForm) { form = newForm }

    fun getName(): String = templatep?.name ?: name
    fun getIcon(): String = templatep?.icon ?: ""
    fun isPersistent(): Boolean = templatep?.persist ?: false
    fun getType(): String = templatep?.type ?: ""

    fun getMessage(): String {
        val tmpl = templatep ?: return ""
        var msg = tmpl.message
        for ((k, v) in substitutions) msg = msg.replace("[$k]", v.toString())
        return msg
    }

    fun getFooter(): String {
        val tmpl = templatep ?: return ""
        var footer = tmpl.footer
        for ((k, v) in substitutions) footer = footer.replace("[$k]", v.toString())
        return footer
    }

    fun getLabel(): String {
        val tmpl = templatep ?: return ""
        var label = tmpl.label
        for ((k, v) in substitutions) label = label.replace("[$k]", v.toString())
        return label
    }

    fun hasLabel(): Boolean = templatep?.label?.isNotEmpty() ?: false

    fun getUrl(): String {
        val tmpl = templatep ?: return ""
        var url = tmpl.url
        for ((k, v) in substitutions) url = url.replace("[$k]", v.toString())
        return url
    }

    fun getUrlOption(): Int = templatep?.urlOption?.toInt() ?: -1
    fun getUrlOpenExternally(): Boolean = templatep?.urlTarget == "_external"
    fun getForceUrlsExternal(): Boolean = templatep?.forceUrlsExternal ?: false
    fun canLogToChat(): Boolean = templatep?.logToChat ?: false
    fun canLogToIM(): Boolean = templatep?.logToIM ?: false
    fun canShowToast(): Boolean = templatep?.showToast ?: true
    fun canFadeToast(): Boolean = templatep?.fadeToast ?: true
    fun hasNotifFormElements(): Boolean = (templatep?.form?.getNumElements() ?: 0) != 0
    fun getCombineBehavior(): CombineBehavior = templatep?.combineBehavior ?: CombineBehavior.REPLACE_WITH_NEW
    fun getPriority(): NotificationPriority = effectivePriority
    fun getDate(): Instant = timestamp
    fun getExpiration(): Instant = expiresAt
    fun hasUniquenessConstraints(): Boolean = templatep?.unique ?: false

    fun isExpired(): Boolean {
        if (expiresAt == Instant.EPOCH) return false
        return Instant.now().isAfter(expiresAt)
    }

    fun matchesTag(tag: String): Boolean = templatep?.tags?.contains(tag) ?: false

    fun setIgnored(ignore: Boolean) { ignored = ignore }

    fun playSound() {
        TODO("APR: use JVM equivalent for make_ui_sound(${templatep?.soundName})")
    }

    fun getResponseTemplate(type: ResponseTemplateType = ResponseTemplateType.WITHOUT_DEFAULT_BUTTON): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
        for (i in 0 until form.getNumElements()) {
            val element = form.getElement(i) ?: continue
            result[element.name] = element.value ?: ""
            if (type == ResponseTemplateType.WITH_DEFAULT_BUTTON && element.isDefault) {
                result[element.name] = true
            }
        }
        return result
    }

    fun respond(sd: Map<String, Any>) {
        respondedTo = true
        response = sd.toMutableMap()

        if (responderObj != null) {
            responderObj!!.handleRespond(asMap(), sd)
        } else if (responseFunctorName.isNotEmpty()) {
            Notifications.invokeFunctor(responseFunctorName, asMap(), sd)
        }

        if (temporaryResponder) {
            Notifications.unregisterFunctor(responseFunctorName)
            responseFunctorName = ""
            temporaryResponder = false
        }

        for (combined in combinedNotifications) {
            combined.respond(sd)
        }

        update()
    }

    fun respondWithDefault() {
        respond(getResponseTemplate(ResponseTemplateType.WITH_DEFAULT_BUTTON))
    }

    fun repost() {
        respondedTo = false
        Notifications.update(this)
    }

    fun update() {
        Notifications.update(this)
    }

    fun updateFrom(other: NotificationEntry) {
        if (templatep?.name != other.templatep?.name) return
        payload.clear(); payload.putAll(other.payload)
        substitutions.clear(); substitutions.putAll(other.substitutions)
        expiresAt = other.expiresAt
        cancelled = other.cancelled
        ignored = other.ignored
        effectivePriority = other.effectivePriority
        form = other.form
        responseFunctorName = other.responseFunctorName
        respondedTo = other.respondedTo
        response = other.response.toMutableMap()
        temporaryResponder = other.temporaryResponder
        update()
    }

    internal fun cancel() { cancelled = true }

    fun isEquivalentTo(that: NotificationEntry): Boolean {
        val tmpl = templatep ?: return false
        if (tmpl.name != that.templatep?.name) return false
        if (!tmpl.unique) return false
        for (key in tmpl.uniqueContext) {
            if (substitutions[key]?.toString() != that.substitutions[key]?.toString() ||
                payload[key]?.toString() != that.payload[key]?.toString()) {
                return false
            }
        }
        return true
    }

    fun summarize(): String = "Notification(${getName()}): ${templatep?.message ?: ""}"

    fun asMap(excludeTemplateElements: Boolean = false): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["id"] = id.toString()
        map["name"] = getName()
        map["substitutions"] = substitutions
        map["payload"] = payload
        map["time"] = timestamp.epochSecond
        map["expiry"] = expiresAt.epochSecond
        map["priority"] = effectivePriority.name
        if (!excludeTemplateElements) {
            map["form"] = (0 until form.getNumElements()).mapNotNull { form.getElement(it) }
        }
        if (responseFunctorName.isNotEmpty()) map["responseFunctor"] = responseFunctorName
        return map
    }

    override fun equals(other: Any?): Boolean = other is NotificationEntry && id == other.id
    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int {
            @Suppress("UNCHECKED_CAST")
            val formList = notification["form"] as? List<Map<String, Any>> ?: return -1
            val form = NotificationForm()
            for (el in formList) {
                form.addElement(
                    el["type"] as? String ?: "",
                    el["name"] as? String ?: "",
                    el["value"]
                )
            }
            for (i in 0 until form.getNumElements()) {
                val element = form.getElement(i) ?: continue
                if (element.type == "button" && response[element.name] == true) {
                    return element.index
                }
            }
            return -1
        }

        fun getSelectedOptionName(response: Map<String, Any>): String {
            for ((key, value) in response) {
                if (value == true) return key
            }
            return ""
        }
    }
}

typealias NotificationFilter = (NotificationEntry) -> Boolean

object NotificationFilters {
    fun includeEverything(p: NotificationEntry): Boolean = true

    enum class Comparison { EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL }

    fun <T : Comparable<T>> filterBy(
        field: (NotificationEntry) -> T,
        value: T,
        comparison: Comparison = Comparison.EQUAL
    ): NotificationFilter = { p ->
        val f = field(p)
        when (comparison) {
            Comparison.EQUAL -> f == value
            Comparison.LESS -> f < value
            Comparison.GREATER -> f > value
            Comparison.LESS_EQUAL -> f <= value
            Comparison.GREATER_EQUAL -> f >= value
        }
    }
}

abstract class NotificationChannelBase(protected var filter: NotificationFilter) {
    protected val items: MutableList<NotificationEntry> = mutableListOf()
    protected val itemsMutex = ReentrantLock()
    protected val changedListeners: MutableList<(Map<String, Any>) -> Boolean> = mutableListOf()
    protected val passedFilterListeners: MutableList<(Map<String, Any>) -> Boolean> = mutableListOf()
    protected val failedFilterListeners: MutableList<(Map<String, Any>) -> Boolean> = mutableListOf()

    fun connectChanged(slot: (Map<String, Any>) -> Boolean) {
        itemsMutex.withLock {
            for (item in items) {
                slot(mapOf("sigtype" to "load", "id" to item.id.toString()))
            }
        }
        changedListeners.add(slot)
    }

    fun connectAtFrontChanged(slot: (Map<String, Any>) -> Boolean) {
        for (item in items) {
            slot(mapOf("sigtype" to "load", "id" to item.id.toString()))
        }
        changedListeners.add(0, slot)
    }

    fun connectPassedFilter(slot: (Map<String, Any>) -> Boolean) {
        passedFilterListeners.add(slot)
    }

    fun connectFailedFilter(slot: (Map<String, Any>) -> Boolean) {
        failedFilterListeners.add(slot)
    }

    fun getFilter(): NotificationFilter = filter

    fun updateItem(payload: Map<String, Any>): Boolean {
        val idStr = payload["id"] as? String ?: return false
        val pNotification = Notifications.find(UUID.fromString(idStr)) ?: return false
        return updateItem(payload, pNotification)
    }

    fun updateItem(payload: Map<String, Any>, pNotification: NotificationEntry): Boolean {
        val cmd = payload["sigtype"] as? String ?: return false
        val wasFound = items.contains(pNotification)
        val passesFilter = filter(pNotification)

        var abortProcessing = false
        if (passesFilter) {
            onFilterPass(pNotification)
            abortProcessing = passedFilterListeners.any { it(payload) }
        } else {
            onFilterFail(pNotification)
            abortProcessing = failedFilterListeners.any { it(payload) }
        }

        if (abortProcessing) return true

        when (cmd) {
            "load" -> {
                if (passesFilter) {
                    items.add(pNotification)
                    onLoad(pNotification)
                    abortProcessing = changedListeners.any { it(payload) }
                }
            }
            "change" -> {
                if (passesFilter) {
                    if (wasFound) {
                        onChange(pNotification)
                        abortProcessing = changedListeners.any { it(payload) }
                    } else {
                        items.add(pNotification)
                        onChange(pNotification)
                        val newPayload = payload.toMutableMap().also { it["sigtype"] = "add" }
                        abortProcessing = changedListeners.any { it(newPayload) }
                    }
                } else {
                    if (wasFound) {
                        items.remove(pNotification)
                        onChange(pNotification)
                        val newPayload = payload.toMutableMap().also { it["sigtype"] = "delete" }
                        abortProcessing = changedListeners.any { it(newPayload) }
                    }
                }
            }
            "add" -> {
                if (passesFilter) {
                    items.add(pNotification)
                    onAdd(pNotification)
                    abortProcessing = changedListeners.any { it(payload) }
                }
            }
            "delete" -> {
                if (wasFound) {
                    onDelete(pNotification)
                    abortProcessing = changedListeners.any { it(payload) }
                    items.remove(pNotification)
                }
            }
        }
        return abortProcessing
    }

    protected open fun onLoad(p: NotificationEntry) {}
    protected open fun onAdd(p: NotificationEntry) {}
    protected open fun onDelete(p: NotificationEntry) {}
    protected open fun onChange(p: NotificationEntry) {}
    protected open fun onFilterPass(p: NotificationEntry) {}
    protected open fun onFilterFail(p: NotificationEntry) {}
}

open class NotificationChannel(
    val channelName: String,
    parentName: String,
    filter: NotificationFilter
) : NotificationChannelBase(filter) {
    private val parents: MutableList<String> = mutableListOf()

    init {
        connectToChannel(parentName)
    }

    fun isEmpty(): Boolean = items.isEmpty()
    fun size(): Int = items.size

    fun forEachNotification(process: (NotificationEntry) -> Unit) {
        itemsMutex.withLock { items.forEach(process) }
    }

    fun summarize(): String {
        val sb = StringBuilder("Channel '$channelName'\n  ")
        itemsMutex.withLock { items.forEach { sb.append(it.summarize()).append("\n  ") } }
        return sb.toString()
    }

    protected fun connectToChannel(channelName: String) {
        if (channelName.isEmpty()) {
            Notifications.connectChanged { updateItem(it) }
        } else {
            parents.add(channelName)
            Notifications.getChannel(channelName)?.connectChanged { updateItem(it) }
        }
    }

    fun getParents(): List<String> = parents
}

class PersistentNotificationChannel : NotificationChannel(
    "Persistent", "Visible",
    { it.isPersistent() && !it.isCancelled() }
) {
    private val history: MutableList<NotificationEntry> = mutableListOf()

    fun history(): List<NotificationEntry> = history.sortedBy { it.getDate() }

    override fun onAdd(p: NotificationEntry) { history.add(p) }
    override fun onLoad(p: NotificationEntry) { history.add(p) }
}

data class VisibilityRule(
    val type: String = "",
    val tag: String = "",
    val name: String = "",
    val visible: Boolean = true,
    val response: String = ""
)

object Notifications : NotificationChannelBase(NotificationFilters::includeEverything) {
    private val templates: MutableMap<String, NotificationTemplate> = mutableMapOf()
    private val visibilityRules: MutableList<VisibilityRule> = mutableListOf()
    private val uniqueNotifications: MutableMap<String, MutableList<NotificationEntry>> = mutableMapOf()
    private val globalStrings: MutableMap<String, String> = mutableMapOf()
    private var ignoreAllNotifications: Boolean = false
    private val channels: MutableMap<String, NotificationChannel> = mutableMapOf()
    private val functorRegistry: MutableMap<String, Responder> = mutableMapOf()
    private val defaultChannels: MutableList<NotificationChannel> = mutableListOf()

    fun init() {
        loadTemplates()
        loadVisibilityRules()
        createDefaultChannels()
    }

    fun clear() {
        defaultChannels.clear()
    }

    fun loadTemplates(): Boolean {
        TODO("APR: use JVM equivalent to load notifications.xml from skin directory")
    }

    fun loadVisibilityRules(): Boolean {
        TODO("APR: use JVM equivalent to load notification_visibility.xml")
    }

    fun createDefaultChannels() {
        fun addChannel(ch: NotificationChannel) {
            channels[ch.channelName] = ch
            defaultChannels.add(ch)
        }

        addChannel(NotificationChannel("Enabled", "") { !ignoreAllNotifications })
        addChannel(NotificationChannel("Expiration", "Enabled") { expirationFilter(it) })
        addChannel(NotificationChannel("Unexpired", "Enabled") { !expirationFilter(it) })
        addChannel(NotificationChannel("Unique", "Unexpired") { uniqueFilter(it) })
        addChannel(NotificationChannel("Ignore", "Unique") { filterIgnoredNotifications(it) })
        addChannel(NotificationChannel("VisibilityRules", "Ignore") { isVisibleByRules(it) })
        addChannel(NotificationChannel("Visible", "VisibilityRules") { true })
        val persistent = PersistentNotificationChannel()
        channels[persistent.channelName] = persistent
        defaultChannels.add(persistent)

        getChannel("Enabled")?.connectFailedFilter { defaultResponse(it) }
        getChannel("Expiration")?.connectChanged { expirationHandler(it) }
        getChannel("Unique")?.connectAtFrontChanged { uniqueHandler(it) }
        getChannel("Unique")?.connectFailedFilter { failedUniquenessTest(it) }
        getChannel("Ignore")?.connectFailedFilter { handleIgnoredNotification(it) }
        getChannel("VisibilityRules")?.connectFailedFilter { true }
    }

    fun add(
        name: String,
        substitutions: Map<String, Any> = emptyMap(),
        payload: Map<String, Any> = emptyMap()
    ): NotificationEntry {
        val n = NotificationEntry(name, substitutions = substitutions.toMutableMap(), payload = payload.toMutableMap())
        add(n)
        return n
    }

    fun add(
        name: String,
        substitutions: Map<String, Any>,
        payload: Map<String, Any>,
        functorName: String
    ): NotificationEntry {
        val n = NotificationEntry(name, substitutions = substitutions.toMutableMap(), payload = payload.toMutableMap(), responderName = functorName)
        add(n)
        return n
    }

    fun add(
        name: String,
        substitutions: Map<String, Any>,
        payload: Map<String, Any>,
        functor: Responder
    ): NotificationEntry {
        val key = UUID.randomUUID().toString()
        registerFunctor(key, functor)
        val n = NotificationEntry(name, substitutions = substitutions.toMutableMap(), payload = payload.toMutableMap(), responderName = key)
        add(n)
        return n
    }

    fun add(pNotif: NotificationEntry) {
        if (items.contains(pNotif)) return
        updateItem(mapOf("sigtype" to "add", "id" to pNotif.id.toString()), pNotif)
    }

    fun load(pNotif: NotificationEntry) {
        if (items.contains(pNotif)) return
        updateItem(mapOf("sigtype" to "load", "id" to pNotif.id.toString()), pNotif)
    }

    fun cancel(pNotif: NotificationEntry?) {
        pNotif ?: return
        if (pNotif.isCancelled()) return
        if (items.contains(pNotif)) {
            pNotif.cancel()
            updateItem(mapOf("sigtype" to "delete", "id" to pNotif.id.toString()), pNotif)
        }
    }

    fun cancelByName(name: String) {
        val toCancel = itemsMutex.withLock { items.filter { it.getName() == name }.toList() }
        for (n in toCancel) {
            n.cancel()
            updateItem(mapOf("sigtype" to "delete", "id" to n.id.toString()), n)
        }
    }

    fun cancelByOwner(ownerId: UUID) {
        val toCancel = itemsMutex.withLock {
            items.filter { it.payload["owner_id"]?.toString() == ownerId.toString() }.toList()
        }
        for (n in toCancel) {
            n.cancel()
            updateItem(mapOf("sigtype" to "delete", "id" to n.id.toString()), n)
        }
    }

    fun update(pNotif: NotificationEntry) {
        if (items.contains(pNotif)) {
            updateItem(mapOf("sigtype" to "change", "id" to pNotif.id.toString()), pNotif)
        }
    }

    fun find(uuid: UUID): NotificationEntry? = items.find { it.id == uuid }

    fun getTemplate(name: String): NotificationTemplate? =
        templates[name] ?: templates["MissingAlert"]

    fun getTemplateNames(): List<String> = templates.keys.toList()

    fun templateExists(name: String): Boolean = templates.containsKey(name)

    fun getChannel(channelName: String): NotificationChannel? = channels[channelName]

    fun getGlobalString(key: String): String = globalStrings[key] ?: key

    fun setIgnoreAllNotifications(ignore: Boolean) { ignoreAllNotifications = ignore }
    fun getIgnoreAllNotifications(): Boolean = ignoreAllNotifications

    fun setIgnored(name: String, ignored: Boolean) {
        templates[name]?.form?.setIgnored(ignored)
    }

    fun getIgnored(name: String): Boolean {
        if (ignoreAllNotifications) return true
        val tmpl = templates[name] ?: return false
        return tmpl.form.ignoreType != NotifIgnoreType.NO && tmpl.form.getIgnored()
    }

    fun forceResponse(name: String, option: Int) {
        val tmpNotif = NotificationEntry(name)
        val form = tmpNotif.getForm()
        val response = tmpNotif.getResponseTemplate()
        val element = form.getElement(option) ?: return
        response[element.name] = true
        tmpNotif.respond(response)
    }

    fun isVisibleByRules(n: NotificationEntry): Boolean {
        if (n.isRespondedTo()) return true

        for (rule in visibilityRules) {
            if (rule.type.isNotEmpty() && rule.type != n.getType()) continue
            if (rule.tag.isNotEmpty() && !n.matchesTag(rule.tag)) continue
            if (rule.name.isNotEmpty() && rule.name != n.getName()) continue

            if (!rule.visible) {
                if (rule.response.isEmpty()) {
                    cancel(n)
                } else {
                    val response = n.getResponseTemplate(ResponseTemplateType.WITHOUT_DEFAULT_BUTTON)
                    response[rule.response] = true
                    n.respond(response)
                }
                return false
            }
            break
        }
        return true
    }

    fun registerFunctor(name: String, functor: Responder) {
        functorRegistry[name] = functor
    }

    fun unregisterFunctor(name: String) {
        functorRegistry.remove(name)
    }

    fun invokeFunctor(name: String, notification: Map<String, Any>, response: Map<String, Any>) {
        functorRegistry[name]?.invoke(notification, response)
    }

    private fun expirationFilter(p: NotificationEntry): Boolean =
        p.isCancelled() || p.isRespondedTo()

    private fun expirationHandler(payload: Map<String, Any>): Boolean {
        if (payload["sigtype"] != "delete") {
            cancel(find(UUID.fromString(payload["id"] as String)))
            return true
        }
        return false
    }

    private fun uniqueFilter(pNotif: NotificationEntry): Boolean {
        if (!pNotif.hasUniquenessConstraints()) return true
        val existing = uniqueNotifications[pNotif.getName()] ?: return true
        for (existingNotif in existing) {
            if (pNotif != existingNotif && pNotif.isEquivalentTo(existingNotif)) {
                if (pNotif.getCombineBehavior() == CombineBehavior.CANCEL_OLD) {
                    cancel(existingNotif)
                    return true
                }
                return false
            }
        }
        return true
    }

    private fun uniqueHandler(payload: Map<String, Any>): Boolean {
        val cmd = payload["sigtype"] as? String ?: return false
        val pNotif = find(UUID.fromString(payload["id"] as String)) ?: return false
        if (pNotif.hasUniquenessConstraints()) {
            when (cmd) {
                "add" -> uniqueNotifications.getOrPut(pNotif.getName()) { mutableListOf() }.add(pNotif)
                "delete" -> uniqueNotifications[pNotif.getName()]?.remove(pNotif)
            }
        }
        return false
    }

    private fun failedUniquenessTest(payload: Map<String, Any>): Boolean {
        val cmd = payload["sigtype"] as? String ?: return false
        if (cmd != "add") return false
        val pNotif = find(UUID.fromString(payload["id"] as String)) ?: return false

        when (pNotif.getCombineBehavior()) {
            CombineBehavior.REPLACE_WITH_NEW -> {
                val existing = uniqueNotifications[pNotif.getName()] ?: return false
                for (existingNotif in existing) {
                    if (pNotif != existingNotif && pNotif.isEquivalentTo(existingNotif)) {
                        existingNotif.updateFrom(pNotif)
                        cancel(pNotif)
                    }
                }
            }
            CombineBehavior.COMBINE_WITH_NEW -> {
                val existing = uniqueNotifications[pNotif.getName()] ?: return false
                for (existingNotif in existing) {
                    if (pNotif != existingNotif && pNotif.isEquivalentTo(existingNotif)) {
                        existingNotif.combinedNotifications.add(pNotif)
                        existingNotif.combinedNotifications.addAll(pNotif.combinedNotifications)
                        existingNotif.update()
                    }
                }
            }
            CombineBehavior.KEEP_OLD -> {}
            CombineBehavior.CANCEL_OLD -> {}
        }
        return false
    }

    private fun filterIgnoredNotifications(notification: NotificationEntry): Boolean =
        !notification.getForm().getIgnored()

    private fun handleIgnoredNotification(payload: Map<String, Any>): Boolean {
        if (payload["sigtype"] != "add") return false
        val pNotif = find(UUID.fromString(payload["id"] as String)) ?: return false
        val form = pNotif.getForm()
        val response: MutableMap<String, Any> = when (form.ignoreType) {
            NotifIgnoreType.WITH_DEFAULT_RESPONSE, NotifIgnoreType.WITH_DEFAULT_RESPONSE_SESSION_ONLY ->
                pNotif.getResponseTemplate(ResponseTemplateType.WITH_DEFAULT_BUTTON)
            NotifIgnoreType.WITH_LAST_RESPONSE ->
                pNotif.getResponseTemplate()
            NotifIgnoreType.SHOW_AGAIN -> return false
            else -> return false
        }
        pNotif.setIgnored(true)
        pNotif.respond(response)
        return true
    }

    private fun defaultResponse(payload: Map<String, Any>): Boolean {
        if (payload["sigtype"] == "add") {
            val pNotif = find(UUID.fromString(payload["id"] as String))
            pNotif?.respond(pNotif.getResponseTemplate(ResponseTemplateType.WITH_DEFAULT_BUTTON))
        }
        return false
    }
}

abstract class PostponedNotification {
    protected var notificationName: String = ""
    protected var notificationParams: Map<String, Any> = emptyMap()
    protected var fromId: UUID? = null

    abstract fun modifyNotificationParams()

    fun lookupName(id: UUID, isGroup: Boolean) {
        fromId = id
        if (isGroup) {
            TODO("APR: use JVM equivalent for gCacheName->getGroup")
        } else {
            fetchAvatarName(id)
        }
    }

    private fun fetchAvatarName(id: UUID) {
        TODO("APR: use JVM equivalent for LLAvatarNameCache::get")
    }

    protected fun finalizeName(name: String) {
        notificationName = name
        modifyNotificationParams()
        Notifications.add(notificationName)
    }
}
