package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d
import com.firestorm.llmessage.Host

object World {

    val regions: MutableMap<ULong, ViewerRegion> = mutableMapOf()
    val objects: MutableMap<LLUUID, ViewerObject> = mutableMapOf()

    var waterHeight: Float = 0f
    var landFarClip: Float = 512f

    fun addRegion(handle: ULong, host: Host): ViewerRegion {
        val existing = regions[handle]
        if (existing != null && existing.host == host) return existing
        val region = ViewerRegion(host, LLUUID.random())
        regions[handle] = region
        return region
    }

    fun removeRegion(host: Host) {
        val iter = regions.entries.iterator()
        while (iter.hasNext()) {
            if (iter.next().value.host == host) iter.remove()
        }
    }

    fun getRegion(host: Host): ViewerRegion? =
        regions.values.firstOrNull { it.host == host }

    fun getRegionFromHandle(handle: ULong): ViewerRegion? = regions[handle]

    fun getRegionFromPosGlobal(pos: Vector3d): ViewerRegion? {
        val regionWidthMeters = 256.0
        return regions.values.firstOrNull { region ->
            val dx = pos.x - region.origin.x
            val dy = pos.y - region.origin.y
            dx >= 0.0 && dx < regionWidthMeters && dy >= 0.0 && dy < regionWidthMeters
        }
    }

    fun getRegionFromPosAgent(pos: Vector3): ViewerRegion? {
        val agentRegion = Agent.instance().getRegion() ?: return null
        val globalPos = agentRegion.localToGlobal(pos)
        return getRegionFromPosGlobal(globalPos)
    }

    fun update(dt: Float) {
        regions.values.forEach { region ->
            objects.values
                .filter { it.region == region }
                .forEach { it.update(dt) }
        }
    }

    fun getViewerObject(id: LLUUID): ViewerObject? = objects[id]

    fun addObject(obj: ViewerObject) {
        objects[obj.id] = obj
    }

    fun removeObject(id: LLUUID) {
        objects.remove(id)
    }

    fun updateObject(obj: ViewerObject) {}
}
