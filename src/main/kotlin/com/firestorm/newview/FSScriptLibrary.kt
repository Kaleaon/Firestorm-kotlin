package com.firestorm.newview

data class LLScriptLibraryFunction(
    val name: String,
    val desc: String,
    val sleepTime: Float,
    val energy: Float,
    val godOnly: Boolean = false,
)

class LLScriptLibrary {
    val functions: MutableList<LLScriptLibraryFunction> = mutableListOf()

    fun loadLibrary(filename: String): Boolean {
        TODO("APR: use JVM equivalent — parse XML at $filename, populate functions via addFunction()")
    }

    private fun addFunction(name: String, desc: String, sleep: Float, energy: Float, godOnly: Boolean = false) {
        functions.add(LLScriptLibraryFunction(name, desc, sleep, energy, godOnly))
    }
}

val gScriptLibrary = LLScriptLibrary()
