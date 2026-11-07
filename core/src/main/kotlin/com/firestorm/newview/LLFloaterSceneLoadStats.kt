package com.firestorm.newview

class LLFloaterSceneLoadStats private constructor(key: Any) : LLFloater(key) {

    override fun postBuild(): Boolean {
        return true
    }

    companion object {
        fun create(key: Any): LLFloaterSceneLoadStats = LLFloaterSceneLoadStats(key)
    }
}
