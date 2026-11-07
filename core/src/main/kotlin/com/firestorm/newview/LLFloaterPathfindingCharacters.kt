package com.firestorm.newview

import java.util.UUID

open class LLFloaterPathfindingObjects(val seed: Map<String, Any?>)

class LLPathfindingCharacterList

open class LLPathfindingObject {
    open fun hasOwner(): Boolean = false
    open fun hasOwnerName(): Boolean = false
    open fun isGroupOwned(): Boolean = false
    open fun getOwnerName(): String = ""
    open fun getUUID(): UUID = UUID(0, 0)
}

open class LLPathfindingCharacter : LLPathfindingObject() {
    fun getName(): String = ""
    fun getDescription(): String = ""
    fun getCPUTime(): Float = 0f
    fun getLocation(): FloatArray = floatArrayOf(0f, 0f, 0f)
    fun getLength(): Float = 0f
    fun getRadius(): Float = 0f
    fun isHorizontal(): Boolean = false
}

data class LLVector3(val x: Float, val y: Float, val z: Float)
data class LLQuaternion(val x: Float, val y: Float, val z: Float, val w: Float)
data class LLColor4(val r: Float, val g: Float, val b: Float, val a: Float)

class LLFloaterPathfindingCharacters(seed: Map<String, Any?>) : LLFloaterPathfindingObjects(seed) {

    companion object {
        private var sInstanceHandle: LLFloaterPathfindingCharacters? = null

        fun openCharactersWithSelectedObjects() {
            val floater = getInstanceHandle() ?: return
            floater.showFloaterWithSelectionObjects()
        }

        fun getInstanceHandle(): LLFloaterPathfindingCharacters? {
            return sInstanceHandle
        }
    }

    private var showPhysicsCapsuleCheckBox: Boolean = false
    private var selectedCharacterId: UUID? = null
    private var beaconColor: LLColor4 = LLColor4(0f, 0f, 0f, 1f)

    init {
        sInstanceHandle = this
    }

    open fun onClose(isAppQuitting: Boolean) {
        hideCapsule()
        super.toString()
    }

    fun isShowPhysicsCapsule(): Boolean = showPhysicsCapsuleCheckBox

    fun setShowPhysicsCapsule(isShowPhysicsCapsule: Boolean) {
        showPhysicsCapsuleCheckBox = isShowPhysicsCapsule && isPathingLibAvailable()
    }

    fun isPhysicsCapsuleEnabled(id: UUID, pos: LLVector3, rot: LLQuaternion): Boolean {
        return isShowPhysicsCapsule() && getCapsuleRenderData(pos, rot)
    }

    fun getSelectedCharacterId(): UUID? = selectedCharacterId

    open fun postBuild(): Boolean {
        beaconColor = getUiColor("PathfindingCharacterBeaconColor")
        setShowPhysicsCapsule(false)
        return true
    }

    open fun requestGetObjects() {
        requestGetCharacters { requestId, status, objectList ->
            handleNewObjectList(requestId, status, objectList)
        }
    }

    open fun buildObjectsScrollList(objectList: List<LLPathfindingObject>) {
        for (obj in objectList) {
            val character = obj as? LLPathfindingCharacter
                ?: error("Expected LLPathfindingCharacter")
            val itemData = buildCharacterScrollListItemData(character)
            addObjectToScrollList(obj, itemData)
        }
    }

    open fun updateControlsOnScrollListChange() {
        updateStateOnDisplayControls()
        showSelectedCharacterCapsules()
    }

    open fun getNameColumnIndex(): Int = 0

    open fun getOwnerNameColumnIndex(): Int = 2

    open fun getOwnerName(obj: LLPathfindingObject): String {
        return when {
            !obj.hasOwner() -> getString("character_owner_unknown")
            !obj.hasOwnerName() -> getString("character_owner_loading")
            obj.isGroupOwned() -> obj.getOwnerName() + " " + getString("character_owner_group")
            else -> obj.getOwnerName()
        }
    }

    open fun getBeaconColor(): LLColor4 = beaconColor

    open fun getEmptyObjectList(): LLPathfindingCharacterList = LLPathfindingCharacterList()

    private fun onShowPhysicsCapsuleClicked() {
        if (!isPathingLibAvailable()) {
            if (isShowPhysicsCapsule()) setShowPhysicsCapsule(false)
        } else {
            if (selectedCharacterId != null && isShowPhysicsCapsule()) showCapsule()
            else hideCapsule()
        }
    }

    private fun buildCharacterScrollListItemData(character: LLPathfindingCharacter): Map<String, Any> {
        val cpuTime = Math.round(character.getCPUTime())
        return mapOf(
            "name" to character.getName(),
            "description" to character.getDescription(),
            "owner" to getOwnerName(character),
            "cpu_time" to getString("character_cpu_time", mapOf("[CPU_TIME]" to cpuTime.toString())),
            "altitude" to "%.0f m".format(character.getLocation()[2])
        )
    }

    private fun updateStateOnDisplayControls() {
        val numSelected = getNumSelectedObjects()
        val isEditEnabled = numSelected == 1 && isPathingLibAvailable()
        if (!isEditEnabled) setShowPhysicsCapsule(false)
    }

    private fun showSelectedCharacterCapsules() {
        hideCapsule()
        if (getNumSelectedObjects() == 1) {
            selectedCharacterId = getFirstSelectedObject()?.getUUID()
        } else {
            selectedCharacterId = null
        }
        showCapsule()
    }

    private fun showCapsule() {
        val charId = selectedCharacterId ?: return
        if (!isShowPhysicsCapsule()) return
        val obj = getFirstSelectedObject() ?: return
        val character = obj as? LLPathfindingCharacter ?: return
        if (isPathingLibAvailable()) {
            // no-op
        }
        // no-op
    }

    private fun hideCapsule() {
        val charId = selectedCharacterId
        if (charId != null) {
            // no-op
        }
        if (isPathingLibAvailable()) {
            // no-op
        }
    }

    private fun getCapsuleRenderData(position: LLVector3, rot: LLQuaternion): Boolean {
        val charId = selectedCharacterId ?: return false
        System.err.println("LLFloaterPathfindingCharacters: find viewer object by charId and return its render position/rotation not yet implemented")
        return false
    }

    private fun isPathingLibAvailable(): Boolean = false
    private fun getUiColor(name: String): LLColor4 = LLColor4(0f, 0f, 0f, 1f)
    private fun getString(key: String, args: Map<String, String> = emptyMap()): String = ""
    private fun getNumSelectedObjects(): Int = 0
    private fun getFirstSelectedObject(): LLPathfindingObject? = null
    private fun addObjectToScrollList(obj: LLPathfindingObject, data: Map<String, Any>) {}
    private fun handleNewObjectList(requestId: Int, status: Any, objectList: Any) {}
    private fun requestGetCharacters(callback: (Int, Any, Any) -> Unit) {}
    private fun showFloaterWithSelectionObjects() {}
}
