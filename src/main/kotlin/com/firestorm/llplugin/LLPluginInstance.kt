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
        TODO("Platform: load native DSO via System.load / JNI equivalent; dir=$pluginDir file=$pluginFile")
    }

    fun sendMessage(message: String) {
        TODO("Platform: forward message string to loaded native plugin send function")
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
