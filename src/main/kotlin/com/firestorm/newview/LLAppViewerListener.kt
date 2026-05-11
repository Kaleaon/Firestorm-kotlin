package com.firestorm.newview

class LLAppViewerListener(
    private val appViewerGetter: () -> AppViewerBase
) : LLEventAPI(
    "LLAppViewer",
    "LLAppViewer listener to (e.g.) request shutdown"
) {

    init {
        add("requestQuit", "Ask to quit nicely") { requestQuit(it) }
        add("forceQuit",   "Quit abruptly")      { forceQuit(it) }
    }

    private fun requestQuit(event: Map<String, Any?>) {
        appViewerGetter().requestQuit()
    }

    private fun forceQuit(event: Map<String, Any?>) {
        appViewerGetter().forceQuit()
    }
}
