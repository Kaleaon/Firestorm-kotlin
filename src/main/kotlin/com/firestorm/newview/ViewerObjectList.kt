package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*
import com.firestorm.llinventory.*

const val CLOSE_BIN_SIZE: UInt       = 10u
const val NUM_BINS: UInt             = 128u
const val GL_NAME_LAND: UInt         = 0u
const val GL_NAME_PARCEL_WALL: UInt  = 1u
const val GL_NAME_INDEX_OFFSET: UInt = 10u

class OrphanInfo(val parentInfo: ULong, val childInfo: LLUUID) {
    override fun equals(other: Any?): Boolean =
        other is OrphanInfo && parentInfo == other.parentInfo && childInfo == other.childInfo
    override fun hashCode(): Int = 31 * parentInfo.hashCode() + childInfo.hashCode()
}

typealias NewObjectCallback = (ViewerObject) -> Boolean

object ViewerObjectList {
    private val objects: MutableList<ViewerObject>            = mutableListOf()
    private val activeObjects: MutableList<ViewerObject>      = mutableListOf()
    private val mapObjects: MutableList<ViewerObject>         = mutableListOf()
    private val uuidMap: MutableMap<LLUUID, ViewerObject>     = mutableMapOf()
    private val deadObjects: MutableSet<LLUUID>               = mutableSetOf()

    private val ipPortToIndex: MutableMap<ULong, UInt>        = mutableMapOf()
    private val indexAndLocalIdToUuid: MutableMap<ULong, LLUUID> = mutableMapOf()

    private val orphanParents: MutableList<ULong>             = mutableListOf()
    private val orphanChildren: MutableList<OrphanInfo>       = mutableListOf()

    private val staleCostObjects: MutableSet<LLUUID>          = mutableSetOf()
    private val stalePhysicsObjects: MutableSet<LLUUID>       = mutableSetOf()

    private val newObjectListeners: MutableList<NewObjectCallback> = mutableListOf()

    var curBin: UInt       = 0u
    var numNewObjects: Int = 0
    var wasPaused: Boolean = false

    val numOrphans: Int  get() = orphanChildren.size
    val numAvatars: Int  get() = objects.count { TODO("check if avatar pcode") }

    fun getNumObjects(): Int       = objects.size
    fun getNumActiveObjects(): Int = activeObjects.size

    fun addNewObjectListener(cb: NewObjectCallback) { newObjectListeners.add(cb) }

    private fun fireNewObject(obj: ViewerObject) {
        newObjectListeners.forEach { it(obj) }
    }

    fun getObject(index: Int): ViewerObject? = objects.getOrNull(index)

    fun findObject(id: LLUUID): ViewerObject? = uuidMap[id]

    fun addNewObject(obj: ViewerObject): Boolean {
        if (uuidMap.containsKey(obj.id)) return false
        objects.add(obj)
        uuidMap[obj.id] = obj
        setUUIDAndLocal(obj.id, obj.localId, 0u, 0u, obj)
        fireNewObject(obj)
        numNewObjects++
        return true
    }

    fun createObject(pCode: UInt, region: Any?, uuid: LLUUID, localId: UInt): ViewerObject {
        val obj = ViewerObject(uuid, localId, pCode)
        addNewObject(obj)
        return obj
    }

    fun removeObject(id: LLUUID): Boolean {
        val obj = uuidMap.remove(id) ?: return false
        objects.remove(obj)
        activeObjects.remove(obj)
        mapObjects.remove(obj)
        removeFromLocalIDTable(obj)
        return true
    }

    fun killObject(obj: ViewerObject): Boolean {
        deadObjects.add(obj.id)
        obj.markDead()
        return true
    }

    fun killObjects(region: Any?) {
        val toKill = objects.filter { TODO("check object region") }
        @Suppress("UNREACHABLE_CODE")
        toKill.forEach { killObject(it) }
    }

    fun killAllObjects() {
        objects.toList().forEach { killObject(it) }
        cleanDeadObjects(useTimer = false)
    }

    fun cleanDeadObjects(useTimer: Boolean = true) {
        val toRemove = deadObjects.toSet()
        for (id in toRemove) {
            removeObject(id)
            deadObjects.remove(id)
        }
    }

    fun updateActive(obj: ViewerObject) {
        val isActive = TODO("check if object needs per-frame update") as Boolean
        @Suppress("UNREACHABLE_CODE")
        if (isActive) {
            if (!activeObjects.contains(obj)) activeObjects.add(obj)
        } else {
            activeObjects.remove(obj)
        }
    }

    fun removeFromActiveList(obj: ViewerObject) { activeObjects.remove(obj) }

    fun update() {
        TODO("Process object updates, drive active object ticks, manage bins")
    }

    fun shiftObjects(offset: Vector3) {
        TODO("Shift all object positions by offset after region origin shift")
    }

    fun updateObjectCost(objectId: LLUUID, objectCost: Float, linkCost: Float,
                         physicsCost: Float, linkPhysicsCost: Float) {
        staleCostObjects.remove(objectId)
        val obj = findObject(objectId) ?: return
        TODO("Apply cost data to obj")
    }

    fun onObjectCostFetchFailure(objectId: LLUUID) { staleCostObjects.remove(objectId) }

    fun updatePhysicsShapeType(objectId: LLUUID, type: Int) {
        val obj = findObject(objectId) ?: return
        TODO("Set physics shape type on obj")
    }

    fun updatePhysicsProperties(objectId: LLUUID, density: Float, friction: Float,
                                restitution: Float, gravityMultiplier: Float) {
        val obj = findObject(objectId) ?: return
        TODO("Set physics properties on obj")
    }

    fun setUUIDAndLocal(id: LLUUID, localId: UInt, ip: UInt, port: UInt, obj: ViewerObject) {
        val simIndex = ipPortToIndex.getOrPut((ip.toULong() shl 32) or port.toULong()) {
            (ipPortToIndex.size + 1u).toUInt()
        }
        val key = (simIndex.toULong() shl 32) or localId.toULong()
        indexAndLocalIdToUuid[key] = id
        uuidMap[id] = obj
    }

    fun getUUIDFromLocal(localId: UInt, ip: UInt, port: UInt): LLUUID? {
        val simIndex = ipPortToIndex[(ip.toULong() shl 32) or port.toULong()] ?: return null
        val key = (simIndex.toULong() shl 32) or localId.toULong()
        return indexAndLocalIdToUuid[key]
    }

    fun removeFromLocalIDTable(obj: ViewerObject): Boolean {
        val keysToRemove = indexAndLocalIdToUuid.filter { it.value == obj.id }.keys
        keysToRemove.forEach { indexAndLocalIdToUuid.remove(it) }
        return keysToRemove.isNotEmpty()
    }

    fun cleanupReferences(obj: ViewerObject) {
        deadObjects.remove(obj.id)
        activeObjects.remove(obj)
        mapObjects.remove(obj)
    }

    fun orphanize(child: ViewerObject, parentId: UInt, ip: UInt, port: UInt) {
        val key = (ip.toULong() shl 32) or port.toULong()
        orphanParents.add(key)
        orphanChildren.add(OrphanInfo(key, child.id))
    }

    fun findOrphans(obj: ViewerObject, ip: UInt, port: UInt) {
        TODO("Scan orphan list and re-parent matching children to obj")
    }

    fun addToMap(obj: ViewerObject) { mapObjects.add(obj) }
    fun removeFromMap(obj: ViewerObject) { mapObjects.remove(obj) }

    fun renderObjectsForMap() { TODO("GPU: render minimap dots for map objects") }
    fun renderObjectBeacons() { TODO("GPU: render debug beacons") }

    fun dirtyAllObjectInventory() { objects.forEach { TODO("mark inventory dirty on it") } }

    fun getOrphanParentCount(): Int = orphanParents.size

    fun destroy() {
        killAllObjects()
        uuidMap.clear()
        orphanParents.clear()
        orphanChildren.clear()
    }
}
