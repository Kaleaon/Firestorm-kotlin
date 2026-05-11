package com.firestorm.newview

private const val SANITY_CHECK_NOTIFICATION = "SanityCheck"

interface ControlVariable {
    val name: String
    fun isSane(): Boolean
    fun getSanityType(): Int
    fun getSanityComment(): String
    fun getValue(): Any
    fun resetToDefault(force: Boolean)
    fun getSanityValues(): List<Any>
    fun getSanityTypeName(): String
}

interface NotificationUtil {
    fun add(name: String, args: Map<String, String> = emptyMap(), payload: Map<String, String> = emptyMap(), callback: ((Map<String, Any>, Map<String, Any>) -> Unit)? = null)
    fun setIgnored(name: String, ignored: Boolean)
    fun getSelectedOption(notification: Map<String, Any>, response: Map<String, Any>): Int
}

interface ControlGroup {
    fun applyToAll(func: (String, ControlVariable) -> Unit)
}

object SanityCheck {

    lateinit var notificationUtil: NotificationUtil
    lateinit var savedSettings: ControlGroup
    lateinit var savedPerAccountSettings: ControlGroup

    private var lastControl: ControlVariable? = null

    fun init() {
        val visitor = { name: String, control: ControlVariable ->
            if (control.getSanityType() != SANITY_TYPE_NONE) {
                control.connectSanitySignal { onSanity(it, false) }
                onSanity(control)
            }
        }
        savedSettings.applyToAll(visitor)
        savedPerAccountSettings.applyToAll(visitor)
    }

    fun onSanity(controlp: ControlVariable, disregardLastControl: Boolean = false) {
        if (controlp.isSane()) return

        if (disregardLastControl) {
            notificationUtil.setIgnored(SANITY_CHECK_NOTIFICATION, false)
        } else if (controlp == lastControl) {
            return
        }

        lastControl = controlp

        val checkType = SANITY_CHECK_NOTIFICATION + controlp.getSanityTypeName()
        val sanityValues = controlp.getSanityValues()

        val args = mapOf(
            "SANITY_MESSAGE" to formatSanityMessage(checkType, sanityValues),
            "SANITY_COMMENT" to controlp.getSanityComment(),
            "CURRENT_VALUE" to controlp.getValue().toString()
        )

        notificationUtil.add(
            SANITY_CHECK_NOTIFICATION,
            args,
            emptyMap()
        ) { notification, response ->
            onFixIt(notification, response, controlp)
        }
    }

    fun onFixIt(notification: Map<String, Any>, response: Map<String, Any>, controlp: ControlVariable) {
        if (notificationUtil.getSelectedOption(notification, response) == 0) {
            controlp.resetToDefault(true)
        }
    }

    private fun formatSanityMessage(checkType: String, values: List<Any>): String {
        val v1 = values.getOrNull(0)?.toString() ?: ""
        val v2 = values.getOrNull(1)?.toString() ?: ""
        return "$checkType [VALUE_1=$v1, VALUE_2=$v2]"
    }
}

const val SANITY_TYPE_NONE = 0

fun ControlVariable.connectSanitySignal(handler: (ControlVariable) -> Unit) {
    TODO("APR: use JVM equivalent for signal/slot connection")
}
