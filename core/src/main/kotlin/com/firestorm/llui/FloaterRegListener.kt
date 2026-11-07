package com.firestorm.llui

typealias EventMap = Map<String, Any?>

abstract class EventApi(val name: String, val description: String) {
    private val handlers: MutableMap<String, (EventMap) -> Unit> = mutableMapOf()
    private val requiredKeys: MutableMap<String, Set<String>> = mutableMapOf()

    protected fun add(
        opName: String,
        opDescription: String,
        handler: (EventMap) -> Unit,
        required: EventMap = emptyMap()
    ) {
        handlers[opName] = handler
        requiredKeys[opName] = required.keys
    }

    fun dispatch(opName: String, event: EventMap) {
        val required = requiredKeys[opName] ?: emptySet()
        for (key in required) {
            require(event.containsKey(key)) { "Event missing required key: $key" }
        }
        handlers[opName]?.invoke(event) ?: error("Unknown operation: $opName")
    }

    protected fun sendReply(reply: EventMap, event: EventMap) {
        val replyPump = event["reply"] as? String ?: return
        System.err.println("EventApi: sendReply not yet implemented")
    }
}

class FloaterRegListener : EventApi(
    "LLFloaterReg",
    "LLFloaterReg listener to (e.g.) show/hide LLFloater instances"
) {
    init {
        add(
            "getBuildMap",
            "Return on [\"reply\"] data about all registered LLFloaterReg floater names",
            ::getBuildMap,
            mapOf("reply" to null)
        )
        val requiredName = mapOf("name" to null)
        add("showInstance",   "Ask to display the floater specified in [\"name\"]",               ::showInstance,   requiredName)
        add("hideInstance",   "Ask to hide the floater specified in [\"name\"]",                   ::hideInstance,   requiredName)
        add("toggleInstance", "Ask to toggle the state of the floater specified in [\"name\"]",    ::toggleInstance, requiredName)
        add(
            "instanceVisible",
            "Return on [\"reply\"] an event whose [\"visible\"] indicates the visibility of the floater specified in [\"name\"]",
            ::instanceVisible,
            requiredName
        )
        add(
            "clickButton",
            "Simulate clicking the named [\"button\"] in the visible floater named in [\"name\"]",
            ::clickButton,
            mapOf("name" to null, "button" to null)
        )
    }

    private fun getBuildMap(event: EventMap) {
        val reply: MutableMap<String, Any?> = mutableMapOf()
        for ((floaterName, buildData) in FloaterReg.getBuildMapEntries()) {
            reply[floaterName] = buildData.file
        }
        sendReply(reply, event)
    }

    private fun showInstance(event: EventMap) {
        val name = event["name"] as String
        val key = event["key"]
        val focus = event["focus"] as? Boolean ?: false
        FloaterReg.showInstance(name, key, focus)
    }

    private fun hideInstance(event: EventMap) {
        FloaterReg.hideInstance(event["name"] as String, event["key"])
    }

    private fun toggleInstance(event: EventMap) {
        FloaterReg.toggleInstance(event["name"] as String, event["key"])
    }

    private fun instanceVisible(event: EventMap) {
        sendReply(
            mapOf("visible" to FloaterReg.instanceVisible(event["name"] as String, event["key"])),
            event
        )
    }

    private fun clickButton(event: EventMap) {
        val name = event["name"] as String
        val key = event["key"]
        val buttonName = event["button"] as String
        val reply: MutableMap<String, Any?> = mutableMapOf()

        val floater = FloaterReg.findInstance(name, key)
        if (floater == null || !floater.isShown()) {
            reply["type"]  = "LLFloater"
            reply["name"]  = name
            reply["key"]   = key
            reply["error"] = if (floater != null) "!isShown()" else "NULL"
        } else {
            val button = floater.findButton(buttonName)
            if (button == null || !button.isButtonAvailable()) {
                reply["type"]  = "LLButton"
                reply["name"]  = buttonName
                reply["error"] = if (button != null) "!isAvailable()" else "NULL"
            } else {
                button.onCommit()
            }
        }

        val replyPump = event["reply"] as? String
        if (replyPump != null) {
            System.err.println("FloaterRegListener: clickButton reply dispatch not yet implemented")
        }
    }
}

fun FloaterReg.showInstance(name: String, key: Any?, focus: Boolean) {
    System.err.println("FloaterReg: showInstance not yet implemented")
}

fun FloaterReg.hideInstance(name: String, key: Any?) {
    System.err.println("FloaterReg: hideInstance not yet implemented")
}

fun FloaterReg.toggleInstance(name: String, key: Any?) {
    System.err.println("FloaterReg: toggleInstance not yet implemented")
}

fun FloaterReg.instanceVisible(name: String, key: Any?): Boolean {
    System.err.println("FloaterReg: instanceVisible not yet implemented")
    return false
}

fun FloaterReg.findInstance(name: String, key: Any?): Floater? {
    System.err.println("FloaterReg: findInstance not yet implemented")
    return null
}

fun FloaterReg.getBuildMapEntries(): Map<String, FloaterReg.BuildData> {
    System.err.println("FloaterReg: getBuildMapEntries not yet implemented")
    return emptyMap()
}

fun Floater.isShown(): Boolean = visible && !isMinimized

fun Floater.findButton(name: String): Button? {
    System.err.println("Floater: findButton not yet implemented")
    return null
}

fun Button.isButtonAvailable(): Boolean {
    System.err.println("Button: isButtonAvailable not yet implemented")
    return false
}
