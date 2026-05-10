package com.firestorm.newview

import java.util.UUID

open class LLFloaterPathfindingObjects(val seed: Map<String, Any?>)

class LLPathfindingCharacterList

open class LLPathfindingObject {
    open fun hasOwner(): Boolean = TODO("stub")
    open fun hasOwnerName(): Boolean = TODO("stub")
    open fun isGroupOwned(): Boolean = TODO("stub")
    open fun getOwnerName(): String = TODO("stub")
    open fun getUUID(): UUID = TODO("stub")
}

open class LLPathfindingCharacter : LLPathfindingObject() {
    fun getName(): String = TODO("stub")
    fun getDescription(): String = TODO("stub")
    fun getCPUTime(): Float = TODO("stub")
    fun getLocation(): FloatArray = TODO("stub")
    fun getLength(): Float = TODO("stub")
    fun getRadius(): Float = TODO("stub")
    fun isHorizontal(): Boolean = TODO("stub")
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
            TODO("GPU: createPhysicsCapsuleRep(character.getLength(), character.getRadius(), character.isHorizontal(), charId)")
        }
        TODO("GPU: gPipeline.hideObject(charId)")
    }

    private fun hideCapsule() {
        val charId = selectedCharacterId
        if (charId != null) {
            TODO("GPU: gPipeline.restoreHiddenObject(charId)")
        }
        if (isPathingLibAvailable()) {
            TODO("GPU: LLPathingLib.cleanupPhysicsCapsuleRepResiduals()")
        }
    }

    private fun getCapsuleRenderData(position: LLVector3, rot: LLQuaternion): Boolean {
        val charId = selectedCharacterId ?: return false
        TODO("GPU: find viewer object by charId and return its render position/rotation")
    }

    private fun isPathingLibAvailable(): Boolean = TODO("stub: check if LLPathingLib singleton exists")
    private fun getUiColor(name: String): LLColor4 = TODO("stub: LLUIColorTable.getColor($name)")
    private fun getString(key: String, args: Map<String, String> = emptyMap()): String = TODO("stub")
    private fun getNumSelectedObjects(): Int = TODO("stub")
    private fun getFirstSelectedObject(): LLPathfindingObject? = TODO("stub")
    private fun addObjectToScrollList(obj: LLPathfindingObject, data: Map<String, Any>) = TODO("stub")
    private fun handleNewObjectList(requestId: Int, status: Any, objectList: Any) = TODO("stub")
    private fun requestGetCharacters(callback: (Int, Any, Any) -> Unit) = TODO("stub")
    private fun showFloaterWithSelectionObjects() = TODO("stub")
}
