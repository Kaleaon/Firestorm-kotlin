package com.firestorm.newview

open class LLPathfindingObjectList {

    private val objectMap: MutableMap<String, LLPathfindingObject> = mutableMapOf()

    fun isEmpty(): Boolean = objectMap.isEmpty()

    fun clear() {
        objectMap.clear()
    }

    fun update(pUpdateObject: LLPathfindingObject) {
        val id = pUpdateObject.uuid.toString()
        objectMap[id] = pUpdateObject
    }

    fun update(pUpdateObjectList: LLPathfindingObjectList) {
        if (!pUpdateObjectList.isEmpty()) {
            for ((_, obj) in pUpdateObjectList.objectMap) {
                update(obj)
            }
        }
    }

    fun find(pObjectId: String): LLPathfindingObject? = objectMap[pObjectId]

    fun entries(): Map<String, LLPathfindingObject> = objectMap

    protected fun getObjectMap(): MutableMap<String, LLPathfindingObject> = objectMap
}
