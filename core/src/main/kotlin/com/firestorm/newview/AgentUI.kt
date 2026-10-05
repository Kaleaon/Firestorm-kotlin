package com.firestorm.newview

import com.firestorm.agent.Agent
import com.firestorm.agent.AgentAvatar
import com.firestorm.region.ViewerRegion
import com.firestorm.parcel.ViewerParcelMgr
import com.firestorm.slurl.SLURL
import com.firestorm.math.Vector3
import com.firestorm.control.ViewerControl
import kotlin.math.sqrt

object AgentUI {

    enum class LocationFormat {
        LOCATION_FORMAT_NORMAL,
        LOCATION_FORMAT_NORMAL_COORDS,
        LOCATION_FORMAT_LANDMARK,
        LOCATION_FORMAT_NO_MATURITY,
        LOCATION_FORMAT_NO_COORDS,
        LOCATION_FORMAT_FULL,
        LOCATION_FORMAT_V1,
        LOCATION_FORMAT_V1_NO_COORDS,
    }

    fun buildFullname(): String {
        return AgentAvatar.instance?.getFullname() ?: ""
    }

    fun buildSLURL(escaped: Boolean = true): SLURL {
        val region: ViewerRegion = Agent.instance.getRegion() ?: return SLURL()
        return SLURL(region.getName(), region.getOriginGlobal(), Agent.instance.getPositionGlobal())
    }

    fun checkAgentDistance(localPole: Vector3, radius: Float): Boolean {
        val agentPos = Agent.instance.getPositionAgent()
        val dx = agentPos.x - localPole.x
        val dy = agentPos.y - localPole.y
        return sqrt(dx * dx + dy * dy) < radius
    }

    fun buildLocationString(
        fmt: LocationFormat = LocationFormat.LOCATION_FORMAT_LANDMARK,
    ): Pair<Boolean, String> {
        return buildLocationString(fmt, Agent.instance.getPositionAgent())
    }

    fun buildLocationString(
        fmt: LocationFormat,
        agentPosRegion: Vector3,
    ): Pair<Boolean, String> {
        val region = Agent.instance.getRegion() ?: return Pair(false, "")
        val parcel = ViewerParcelMgr.instance.getAgentParcel() ?: return Pair(false, "")

        var posX = (agentPosRegion.x + 0.5f).toInt()
        var posY = (agentPosRegion.y + 0.5f).toInt()
        val posZ = (agentPosRegion.z + 0.5f).toInt()

        val velocityMagSq = Agent.instance.getVelocity().magVecSquared()
        val flyCutoffSq = 6.0f * 6.0f
        val walkCutoffSq = 1.5f * 1.5f

        if (velocityMagSq > flyCutoffSq) {
            posX -= posX % 4
            posY -= posY % 4
        } else if (velocityMagSq > walkCutoffSq) {
            posX -= posX % 2
            posY -= posY % 2
        }

        val showSimVersion = ViewerControl.getBoolean("FSStatusbarShowSimulatorVersion")
        val simulatorVersion: String = if (showSimVersion) {
            extractSimulatorVersion(ViewerControl.getLastVersionChannel())
        } else {
            ""
        }

        val parcelName = ViewerParcelMgr.instance.getAgentParcelName()
        val regionName = region.getName()
        val simAccess = region.getSimAccessString()
        val accessSep = if (simAccess.isEmpty()) "" else " - "

        val buffer = if (parcelName.isEmpty()) {
            when (fmt) {
                LocationFormat.LOCATION_FORMAT_LANDMARK ->
                    regionName.take(100)
                LocationFormat.LOCATION_FORMAT_NORMAL ->
                    regionName
                LocationFormat.LOCATION_FORMAT_NORMAL_COORDS ->
                    "$regionName ($posX, $posY, $posZ)"
                LocationFormat.LOCATION_FORMAT_NO_COORDS ->
                    "$regionName$accessSep$simAccess"
                LocationFormat.LOCATION_FORMAT_NO_MATURITY ->
                    "$regionName ($posX, $posY, $posZ)"
                LocationFormat.LOCATION_FORMAT_FULL ->
                    "$regionName ($posX, $posY, $posZ)$accessSep$simAccess"
                LocationFormat.LOCATION_FORMAT_V1 ->
                    if (showSimVersion && simulatorVersion.isNotEmpty())
                        "$regionName - $simulatorVersion - ($posX, $posY, $posZ)$accessSep$simAccess"
                    else
                        "$regionName ($posX, $posY, $posZ)$accessSep$simAccess"
                LocationFormat.LOCATION_FORMAT_V1_NO_COORDS ->
                    if (showSimVersion && simulatorVersion.isNotEmpty())
                        "$regionName - $simulatorVersion$accessSep$simAccess"
                    else
                        "$regionName$accessSep$simAccess"
            }
        } else {
            when (fmt) {
                LocationFormat.LOCATION_FORMAT_LANDMARK ->
                    parcelName.take(100)
                LocationFormat.LOCATION_FORMAT_NORMAL ->
                    "$parcelName, $regionName"
                LocationFormat.LOCATION_FORMAT_NORMAL_COORDS ->
                    "$parcelName ($posX, $posY, $posZ)"
                LocationFormat.LOCATION_FORMAT_NO_MATURITY ->
                    "$parcelName, $regionName ($posX, $posY, $posZ)"
                LocationFormat.LOCATION_FORMAT_NO_COORDS ->
                    "$parcelName, $regionName$accessSep$simAccess"
                LocationFormat.LOCATION_FORMAT_FULL ->
                    "$parcelName, $regionName ($posX, $posY, $posZ)$accessSep$simAccess"
                LocationFormat.LOCATION_FORMAT_V1 ->
                    if (showSimVersion && simulatorVersion.isNotEmpty())
                        "$regionName - $simulatorVersion - ($posX, $posY, $posZ)$accessSep$simAccess - $parcelName"
                    else
                        "$regionName ($posX, $posY, $posZ)$accessSep$simAccess - $parcelName"
                LocationFormat.LOCATION_FORMAT_V1_NO_COORDS ->
                    if (showSimVersion && simulatorVersion.isNotEmpty())
                        "$regionName - $simulatorVersion$accessSep$simAccess - $parcelName"
                    else
                        "$regionName$accessSep$simAccess - $parcelName"
            }
        }

        return Pair(true, buffer)
    }

    // The version channel format is "Second Life Server 2020-03-20T18:40:52.538914";
    // we want the fractional seconds suffix after the last dot as the display version.
    private fun extractSimulatorVersion(versionChannel: String): String {
        val parts = versionChannel.trim().split("\\s+".toRegex())
        if (parts.size < 4) return ""
        val datePart = parts[3]
        val dotIdx = datePart.lastIndexOf('.')
        if (dotIdx < 0 || dotIdx == datePart.length - 1) return ""
        return datePart.substring(dotIdx + 1)
    }
}
