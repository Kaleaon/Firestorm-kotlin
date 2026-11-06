package com.firestorm.newview

import java.util.UUID

typealias ConsoleReplyListener = (output: String) -> Unit

private val sConsoleReplyListeners: MutableList<ConsoleReplyListener> = mutableListOf()

private const val PROMPT = "\n\n> "
private const val UNABLE_TO_SEND_COMMAND = "ERROR: The last command was not received by the server."
private const val CONSOLE_UNAVAILABLE = "ERROR: No console available for this region/simulator."
private const val CONSOLE_NOT_SUPPORTED = "This region does not support the simulator console."

class LLFloaterRegionDebugConsole(val key: Any) {

    var mOutput: LLTextEditor? = null

    private var mReplySignalConnection: ConsoleReplyListener? = null

    init {
        val listener: ConsoleReplyListener = { output -> onReplyReceived(output) }
        sConsoleReplyListeners.add(listener)
        mReplySignalConnection = listener
    }

    fun destroy() {
        mReplySignalConnection?.let { sConsoleReplyListeners.remove(it) }
        mReplySignalConnection = null
    }

    fun postBuild(): Boolean {
        val input = getChild<LLLineEditor>("region_debug_console_input")
        input.setEnableLineHistory(true)
        input.setCommitCallback { ctrl, param -> onInput(ctrl, param) }
        input.setFocus(true)
        input.setCommitOnFocusLost(false)

        mOutput = getChild<LLTextEditor>("region_debug_console_output")

        var url = gAgent.getRegionCapability("SimConsoleAsync")
        if (url.isEmpty()) {
            url = gAgent.getRegionCapability("SimConsole")
            if (url.isEmpty()) {
                mOutput?.appendText(CONSOLE_NOT_SUPPORTED + PROMPT, false)
                return true
            }
        }

        mOutput?.appendText("> ", false)
        return true
    }

    fun onInput(ctrl: LLUICtrl, param: Any?) {
        val input = ctrl as LLLineEditor
        val text = input.getText() + "\n"

        var url = gAgent.getRegionCapability("SimConsoleAsync")
        if (url.isEmpty()) {
            url = gAgent.getRegionCapability("SimConsole")
            if (url.isEmpty()) {
                mOutput?.appendText(text + CONSOLE_UNAVAILABLE + PROMPT, false)
            } else {
                System.err.println("LLFloaterRegionDebugConsole: onInput not yet implemented")
            }
        } else {
            System.err.println("LLFloaterRegionDebugConsole: onInput not yet implemented")
        }

        mOutput?.appendText(text, false)
        input.clear()
    }

    private fun onAsyncConsoleError(result: Any?) {
        sConsoleReplyListeners.forEach { it(UNABLE_TO_SEND_COMMAND) }
    }

    private fun onConsoleError(result: Any?) {
        mOutput?.appendText(UNABLE_TO_SEND_COMMAND + PROMPT, false)
    }

    private fun onConsoleSuccess(result: Any?) {
        System.err.println("LLFloaterRegionDebugConsole: onConsoleSuccess not yet implemented")
    }

    private fun onReplyReceived(output: String) {
        mOutput?.appendText(output + PROMPT, false)
    }

    companion object {
        fun setConsoleReplyCallback(cb: ConsoleReplyListener): ConsoleReplyListener {
            sConsoleReplyListeners.add(cb)
            return cb
        }
    }
}

class ConsoleResponseNode {
    fun post(response: Any?, context: Any?, input: Map<String, Any?>) {
        val body = input["body"]?.toString() ?: ""
        sConsoleReplyListeners.forEach { it(body) }
    }
}
