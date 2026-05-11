package com.firestorm.newview

import java.util.UUID

class FloaterClassified(key: Any) : Floater(key) {

    override fun onOpen(key: Any) {
        val panel = findChild<Panel>("main_panel", recursive = true)
        panel?.onOpen(key)

        val classifiedName = (key as? Map<*, *>)?.get("classified_name") as? String
        if (classifiedName != null) {
            setTitle(classifiedName)
        }
        super.onOpen(key)
    }

    override fun postBuild(): Boolean = true

    // Two floater instances match when they refer to the same classified UUID.
    override fun matchesKey(key: Any): Boolean {
        val mKeyMap = mKey as? Map<*, *>
        val keyMap  = key  as? Map<*, *>
        val isMKeyValid = mKeyMap?.containsKey("classified_id") == true
        val isKeyValid  = keyMap?.containsKey("classified_id")  == true
        if (isMKeyValid && isKeyValid) {
            val mId = (mKeyMap!!["classified_id"] as? UUID)
                ?: UUID.fromString(mKeyMap["classified_id"].toString())
            val id  = (keyMap!!["classified_id"] as? UUID)
                ?: UUID.fromString(keyMap["classified_id"].toString())
            return mId == id
        }
        return isMKeyValid == isKeyValid
    }
}
