package com.firestorm.llui

typealias LLSD = Map<String, Any?>

abstract class EventApi(val name: String, val description: String) {
    private val handlers: MutableMap<String, (LLSD) -> Unit> = mutableMapOf()
    private val requiredKeys: MutableMap<String, Set<String>> = mutableMapOf()

    protected fun add(
        opName: String,
        opDescription: String,
        handler: (LLSD) -> Unit,
        required: LLSD = emptyMap()
    ) {
        handlers[opName] = handler
        requiredKeys[opName] = required.keys
    }

    fun dispatch(opName: String, event: LLSD) {
        val required = requiredKeys[opName] ?: emptySet()
        for (key in required) {
            require(event.containsKey(key)) { "Event missing required key: $key" }
        }
        handlers[opName]?.invoke(event) ?: error("Unknown operation: $opName")
    }

    protected fun sendReply(reply: LLSD, event: LLSD) {
        val replyPump = event["reply"] as? String ?: return
        TODO("APR: LLEventPumps.obtain($replyPump).post($reply)")
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
        add("showInstance",   "Ask to display the floater specified in [\"name\"]",       ::showInstance,   requiredName)
        add("hideInstance",   "Ask to hide the floater specified in [\"name\"]",           ::hideInstance,   requiredName)
        add("toggleInstance", "Ask to toggle the state of the floater specified in [\"name\"]", ::toggleInstance, requiredName)
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

    private fun getBuildMap(event: LLSD) {
        val reply: MutableMap<String, Any?> = mutableMapOf()
        for ((floaterName, buildData) in FloaterReg.buildMap) {
            reply[floaterName] = buildData.file
        }
        sendReply(reply, event)
    }

    private fun showInstance(event: LLSD) {
        val name = event["name"] as String
        val key = event["key"]
        val focus = event["focus"] as? Boolean ?: false
        FloaterReg.showInstance(name, key, focus)
    }

    private fun hideInstance(event: LLSD) {
        val name = event["name"] as String
        val key = event["key"]
        FloaterReg.hideInstance(name, key)
    }

    private fun toggleInstance(event: LLSD) {
        val name = event["name"] as String
        val key = event["key"]
        FloaterReg.toggleInstance(name, key)
    }

    private fun instanceVisible(event: LLSD) {
        val name = event["name"] as String
        val key = event["key"]
        sendReply(mapOf("visible" to FloaterReg.instanceVisible(name, key)), event)
    }

    private fun clickButton(event: LLSD) {
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
            if (button == null || !button.isAvailable()) {
                reply["type"]  = "LLButton"
                reply["name"]  = buttonName
                reply["error"] = if (button != null) "!isAvailable()" else "NULL"
            } else {
                button.onCommit()
            }
        }

        val replyPump = event["reply"] as? String
        if (replyPump != null) {
            TODO("APR: LLEventPumps.obtain($replyPump).post($reply)")
        }
    }
}

fun FloaterReg.showInstance(name: String, key: Any?, focus: Boolean) {
    TODO("APR: FloaterReg.showInstance($name, $key, $focus)")
}

fun FloaterReg.hideInstance(name: String, key: Any?) {
    TODO("APR: FloaterReg.hideInstance($name, $key)")
}

fun FloaterReg.toggleInstance(name: String, key: Any?) {
    TODO("APR: FloaterReg.toggleInstance($name, $key)")
}

fun FloaterReg.instanceVisible(name: String, key: Any?): Boolean =
    TODO("APR: FloaterReg.instanceVisible($name, $key)")

fun FloaterReg.findInstance(name: String, key: Any?): Floater? =
    TODO("APR: FloaterReg.findInstance($name, $key)")

val FloaterReg.buildMap: Map<String, FloaterReg.BuildData>
    get() = TODO("APR: expose internal build map for iteration")

fun Floater.isShown(): Boolean = visible && !isMinimized

fun Floater.findButton(name: String): Button? =
    TODO("APR: find child Button named $name in this floater")

class Button(val name: String) {
    fun isAvailable(): Boolean = TODO("APR: return enabled && visible for this button")
    fun onCommit() { TODO("APR: fire button commit callback") }
}
