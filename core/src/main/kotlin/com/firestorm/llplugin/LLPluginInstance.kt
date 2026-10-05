package com.firestorm.llplugin

interface LLPluginInstanceMessageListener {
    fun receivePluginMessage(message: String)
}

/**
 * LLPluginInstance manages loading a native plugin DSO and wiring its message-passing
 * entry points.  All DSO/JNI loading is stubbed because APR DSO has no direct JVM equivalent;
 * use a JVM plugin loader (e.g. URLClassLoader or JNI) as a replacement.
 */
class LLPluginInstance(private val owner: LLPluginInstanceMessageListener?) {

    private var pluginLoaded: Boolean = false

    fun load(pluginDir: String, pluginFile: String): Int {
        System.err.println("LLPluginInstance: load not yet implemented")
        return 0
    }

    fun sendMessage(message: String) {
        System.err.println("LLPluginInstance: sendMessage not yet implemented")
    }

    fun idle() {
        // No-op in C++ as well.
    }

    private fun receiveMessage(messageString: String) {
        owner?.receivePluginMessage(messageString)
    }

    companion object {
        const val PLUGIN_INIT_FUNCTION_NAME = "LLPluginInitEntryPoint"
    }
}
