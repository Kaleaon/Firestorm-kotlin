package com.firestorm.newview

class LLCommandDispatcherListener : LLEventAPI(
    "LLCommandDispatcher",
    "Access to LLCommandHandler commands"
) {

    init {
        add(
            "dispatch",
            "Execute a command registered as an LLCommandHandler,\n" +
            "passing any required parameters:\n" +
            "[\"cmd\"] string command name\n" +
            "[\"params\"] array of parameters, as if from components of URL path\n" +
            "[\"query\"] map of parameters, as if from ?key1=val&key2=val\n" +
            "[\"trusted\"] boolean indicating trusted browser [default true]"
        ) { dispatch(it) }
        add(
            "enumerate",
            "Post to [\"reply\"] a map of registered LLCommandHandler instances, containing\n" +
            "name key and (e.g.) untrusted flag"
        ) { enumerate(it) }
    }

    private fun dispatch(params: Map<String, Any?>) {
        // Callers are trusted by default; allow explicit opt-out for testing.
        val trustedBrowser = params["trusted"] as? Boolean ?: true
        CommandDispatcher.dispatch(
            cmd          = params["cmd"]?.toString() ?: "",
            params       = params["params"] ?: emptyList<Any>(),
            queryMap     = @Suppress("UNCHECKED_CAST") (params["query"] as? Map<String, Any?> ?: emptyMap()),
            grid         = "",
            web          = null,
            navType      = CommandHandler.NAV_TYPE_CLICKED,
            trustedBrowser = trustedBrowser
        )
    }

    private fun enumerate(params: Map<String, Any?>) {
        val reqId = LLReqID(params)
        val response = CommandDispatcher.enumerate().toMutableMap<String, Any?>()
        reqId.stamp(response)
        LLEventPumps.instance().obtain(params["reply"]?.toString() ?: "").post(response)
    }
}
