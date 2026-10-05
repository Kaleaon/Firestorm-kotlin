package com.firestorm.newview

class LLPathfindingCharacterList : LLPathfindingObjectList {

    constructor() : super()

    constructor(characterListData: Map<String, Any>) : super() {
        parseCharacterListData(characterListData)
    }

    private fun parseCharacterListData(characterListData: Map<String, Any>) {
        val map = getObjectMap()
        for ((uuid, value) in characterListData) {
            @Suppress("UNCHECKED_CAST")
            val characterData = value as? Map<String, Any> ?: continue
            map[uuid] = LLPathfindingCharacter(uuid, characterData)
        }
    }
}
