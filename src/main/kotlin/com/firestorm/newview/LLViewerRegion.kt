package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d
import com.firestorm.llmessage.Host

class ViewerRegion(
    val host: Host,
    val regionId: LLUUID,
) {
    enum class AccessLevel { PUBLIC, MATURE, ADULT }

    var name: String = ""
    var zoning: String = ""
    var origin: Vector3d = Vector3d.ZERO
    var waterHeight: Float = 0f
    var isAlive: Boolean = true
    var simAccess: UByte = 0u
    var regionFlags: ULong = 0uL
    var billableFactor: Float = 1f
    var cpuRatio: Float = 1f
    var maxTasks: Int = 0

    private val capabilities: MutableMap<String, String> = mutableMapOf()

    fun localToGlobal(local: Vector3): Vector3d =
        Vector3d(origin.x + local.x, origin.y + local.y, origin.z + local.z)

    fun globalToLocal(global: Vector3d): Vector3 =
        Vector3(
            (global.x - origin.x).toFloat(),
            (global.y - origin.y).toFloat(),
            (global.z - origin.z).toFloat()
        )

    fun isCapabilityAvailable(capName: String): Boolean = capName in capabilities

    fun getCapability(capName: String): String? = capabilities[capName]

    fun setCapability(capName: String, url: String) {
        capabilities[capName] = url
    }

    fun canManageEstate(): Boolean = false
}
