package com.firestorm.newview

import com.firestorm.ui.LLFrameTimer
import com.firestorm.ui.LLWorld
import com.firestorm.ui.LLViewerRegion
import com.firestorm.ui.LLCharacter
import com.firestorm.ui.LLVOAvatar
import com.firestorm.ui.gObjectList
import java.util.UUID

private const val KEY_AGENTS = "agents"
private const val KEY_WEIGHT = "weight"
private const val KEY_TOO_COMPLEX = "tooComplex"
private const val KEY_OVER_COMPLEXITY_LIMIT = "overlimit"
private const val KEY_REPORTING_COMPLEXITY_LIMIT = "reportinglimit"
private const val KEY_IDENTIFIER = "identifier"
private const val KEY_MESSAGE = "message"
private const val KEY_ERROR = "error"

private const val SECS_BETWEEN_REGION_SCANS = 5f
private const val SECS_BETWEEN_REGION_REQUEST = 15f
private const val SECS_BETWEEN_REGION_REPORTS = 60f

object AvatarRenderInfoAccountant {

    private val renderInfoScanTimer: LLFrameTimer = LLFrameTimer()

    fun sendRenderInfoToRegion(regionp: LLViewerRegion) {
        val url = regionp.getCapability("AvatarRenderInfo")
        if (url.isNullOrEmpty()) return
        if (!regionp.getRenderInfoReportTimer().hasExpired()) return
        regionp.getRenderInfoReportTimer().resetWithExpiry(SECS_BETWEEN_REGION_REPORTS)
        launchCoroutine { avatarRenderInfoReportCoro(url, regionp.getHandle()) }
    }

    fun getRenderInfoFromRegion(regionp: LLViewerRegion) {
        val url = regionp.getCapability("AvatarRenderInfo")
        if (url.isNullOrEmpty()) return
        if (!regionp.getRenderInfoRequestTimer().hasExpired()) return
        regionp.getRenderInfoRequestTimer().resetWithExpiry(SECS_BETWEEN_REGION_REQUEST)
        launchCoroutine { avatarRenderInfoGetCoro(url, regionp.getHandle()) }
    }

    fun idle() {
        if (!renderInfoScanTimer.hasExpired()) return
        val world = LLWorld.getInstance() ?: return
        for (regionp in world.getRegionList()) {
            if (regionp != null && regionp.isAlive() && regionp.capabilitiesReceived()) {
                sendRenderInfoToRegion(regionp)
                getRenderInfoFromRegion(regionp)
            }
        }
        renderInfoScanTimer.resetWithExpiry(SECS_BETWEEN_REGION_SCANS)
    }

    fun resetRenderInfoScanTimer() {
        renderInfoScanTimer.reset()
    }

    fun scanNewRegion(regionId: UUID) {
        resetRenderInfoScanTimer()
        val regionp = LLWorld.instance().getRegionFromID(regionId)
        if (regionp != null) {
            regionp.getRenderInfoRequestTimer().reset()
            regionp.getRenderInfoReportTimer().resetWithExpiry(SECS_BETWEEN_REGION_SCANS)
        }
    }

    private fun avatarRenderInfoGetCoro(url: String, regionHandle: ULong) {
        TODO("APR: use JVM equivalent - issue HTTP GET to $url, parse agent weights, call AvatarRenderNotifier.updateNotificationRegion")
        // Pseudocode of the original coroutine:
        //   val result = httpGet(url, timeout = SECS_BETWEEN_REGION_REQUEST, retries = 0)
        //   val world = LLWorld.getInstance() ?: return
        //   val regionp = world.getRegionFromHandle(regionHandle) ?: return
        //   regionp.getRenderInfoRequestTimer().resetWithExpiry(SECS_BETWEEN_REGION_REQUEST)
        //   if (!result.ok) return
        //   val agents = result[KEY_AGENTS]  // map of agentId -> {weight, tooComplex}
        //   for ((agentIdStr, infoMap) in agents) {
        //       val avatarp = gObjectList.findObject(UUID.fromString(agentIdStr))?.asAvatar() ?: continue
        //       if (avatarp.isControlAvatar()) continue
        //       avatarp.setReportedVisualComplexity(infoMap[KEY_WEIGHT].asInteger())
        //   }
        //   val reporting = result[KEY_REPORTING_COMPLEXITY_LIMIT].asInteger().toUInt()
        //   val overlimit = result[KEY_OVER_COMPLEXITY_LIMIT].asInteger().toUInt()
        //   AvatarRenderNotifier.updateNotificationRegion(reporting, overlimit)
    }

    private fun avatarRenderInfoReportCoro(url: String, regionHandle: ULong) {
        TODO("APR: use JVM equivalent - gather local avatar complexities, HTTP POST to $url")
        // Pseudocode of the original coroutine:
        //   val world = LLWorld.getInstance() ?: return
        //   val regionp = world.getRegionFromHandle(regionHandle) ?: return
        //   val agents = mutableMapOf<String, Map<String, Any>>()
        //   var numAvs = 0
        //   for (character in LLCharacter.sInstances) {
        //       val avatar = character as LLVOAvatar
        //       if (avatar.isDead() || avatar.isControlAvatar() || avatar.getRezzedStatus() < 2) continue
        //       if (avatar.getObjectHost() != regionp.getHost()) continue
        //       val complexity = avatar.getVisualComplexity()
        //       if (complexity > 0) {
        //           agents[avatar.getID().toString()] = mapOf(
        //               KEY_WEIGHT to minOf(complexity, Int.MAX_VALUE.toUInt()).toInt(),
        //               KEY_TOO_COMPLEX to avatar.isTooComplex()
        //           )
        //           numAvs++
        //       }
        //   }
        //   regionp.getRenderInfoReportTimer().resetWithExpiry(SECS_BETWEEN_REGION_REPORTS + 2f * numAvs)
        //   if (numAvs == 0) return
        //   val result = httpPost(url, mapOf(KEY_AGENTS to agents), retries = 1)
        //   if (!result.ok) return
        //   if (result[KEY_ERROR] != null) { log error } else { log debug }
    }

    private fun launchCoroutine(block: suspend () -> Unit) {
        TODO("APR: use JVM equivalent - launch a coroutine/thread for $block")
    }
}
