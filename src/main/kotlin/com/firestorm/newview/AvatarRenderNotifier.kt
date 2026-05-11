package com.firestorm.newview

import com.firestorm.ui.LLFrameTimer
import com.firestorm.ui.LLNotificationPtr
import com.firestorm.ui.LLNotifications
import com.firestorm.ui.LLNotification
import com.firestorm.ui.LLDate
import com.firestorm.ui.LLTrans
import com.firestorm.ui.LLSD
import com.firestorm.ui.LLSLURL
import com.firestorm.ui.LLURI
import com.firestorm.ui.gSavedSettings
import com.firestorm.ui.gAgentCamera
import com.firestorm.ui.isAgentAvatarValid
import com.firestorm.ui.gAgentWearables
import com.firestorm.ui.LLAttachmentsMgr
import com.firestorm.ui.LLAppearanceMgr
import com.firestorm.ui.gAgentAvatarp
import com.firestorm.ui.LLViewerInventoryCategory
import com.firestorm.ui.CAMERA_MODE_MOUSELOOK
import java.util.UUID
import kotlin.math.abs

private const val RENDER_ALLOWED_CHANGE_PCT = 0.1f
private const val OVER_LIMIT_UPDATE_DELAY = 70u

private const val WARN_HUD_OBJECTS_LIMIT = 1000u
private const val WARN_HUD_TEXTURES_LIMIT = 200u
private const val WARN_HUD_OVERSIZED_TEXTURES_LIMIT = 6u
private const val WARN_HUD_TEXTURE_MEMORY_LIMIT = 32_000_000u  // bytes

// ---------------------------------------------------------------------------
// Data structures
// ---------------------------------------------------------------------------

data class HUDComplexity(
    var objectId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    var objectName: String = "",
    var jointName: String = "",
    var objectsCost: UInt = 0u,
    var objectsCount: UInt = 0u,
    var texturesCost: UInt = 0u,
    var texturesCount: UInt = 0u,
    var largeTexturesCount: UInt = 0u,
    var texturesMemoryTotal: Double = 0.0,   // in bytes, F64Bytes mapped to Double
) {
    fun reset() {
        objectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        objectName = ""; jointName = ""
        objectsCost = 0u; objectsCount = 0u
        texturesCost = 0u; texturesCount = 0u
        largeTexturesCount = 0u; texturesMemoryTotal = 0.0
    }
}

data class ObjectComplexity(
    var objectId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    var objectName: String = "",
    var objectCost: UInt = 0u,
) {
    fun reset() {
        objectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        objectName = ""; objectCost = 0u
    }
}

// ---------------------------------------------------------------------------
// AvatarRenderNotifier  (C++ LLAvatarRenderNotifier singleton)
// ---------------------------------------------------------------------------

object AvatarRenderNotifier {

    private var notificationPtr: LLNotificationPtr? = null
    private val popUpDelayTimer: LLFrameTimer = LLFrameTimer().also {
        it.resetWithExpiry(OVER_LIMIT_UPDATE_DELAY.toFloat())
    }

    private var agentsCount: UInt = 0u
    private var overLimitAgents: UInt = 0u
    private var agentComplexity: UInt = 0u
    private var overLimitPct: Float = 0f

    private var latestAgentsCount: UInt = 0u
    private var latestOverLimitAgents: UInt = 0u
    private var latestAgentComplexity: UInt = 0u
    private var latestOverLimitPct: Float = 0f

    private var showOverLimitAgents: Boolean = false

    private var notifyOutfitLoading: Boolean = false
    private var lastCofVersion: Int = LLViewerInventoryCategory.VERSION_UNKNOWN
    private var lastSkeletonSerialNum: Int = -1
    private var lastOutfitRezStatus: Int = -1

    var objectComplexityList: MutableList<ObjectComplexity> = mutableListOf()

    private fun overLimitMessage(): String {
        val key = when {
            latestOverLimitPct >= 99f -> "av_render_anyone"
            latestOverLimitPct >= 75f -> "av_render_most_of"
            latestOverLimitPct >= 50f -> "av_render_over_half"
            latestOverLimitPct > 10f  -> "av_render_not_everyone"
            else                      -> "av_render_everyone_now"
        }
        return LLTrans.getString(key)
    }

    fun displayNotification(showOverLimit: Boolean) {
        agentComplexity = latestAgentComplexity
        showOverLimitAgents = showOverLimit
        val expireDelay = gSavedSettings.getU32("ShowMyComplexityChanges", 20u)
        val expireDate = LLDate(LLDate.now().secondsSinceEpoch() + expireDelay.toDouble())

        val complexityString = LLTrans.formatInteger(latestAgentComplexity.toLong())
        val args = LLSD.mapOf("AGENT_COMPLEXITY" to LLSD.fromString(complexityString))
        val notificationName: String
        if (showOverLimitAgents) {
            notificationName = "AgentComplexityWithVisibility"
            args["OVERLIMIT_MSG"] = LLSD.fromString(overLimitMessage())
            agentsCount = latestAgentsCount
            overLimitAgents = latestOverLimitAgents
            overLimitPct = latestOverLimitPct
        } else {
            notificationName = "AgentComplexity"
        }

        val current = notificationPtr
        if (current != null && current.getName() != notificationName) {
            LLNotifications.instance().cancel(current)
        }

        if (expireDelay > 0u && gAgentCamera.getLastCameraMode() != CAMERA_MODE_MOUSELOOK) {
            notificationPtr = LLNotifications.instance().add(
                LLNotification.Params()
                    .name(notificationName)
                    .expiry(expireDate)
                    .substitutions(args)
            )
        }
    }

    fun isNotificationVisible(): Boolean =
        notificationPtr?.isActive() == true

    fun updateNotificationRegion(agentcount: UInt, overLimit: UInt) {
        if (agentcount == 0u) return
        latestAgentsCount = if (agentcount > overLimit) agentcount - 1u else agentcount
        latestOverLimitAgents = overLimit
        latestOverLimitPct = if (latestAgentsCount != 0u)
            (overLimit.toFloat() / latestAgentsCount.toFloat()) * 100f
        else 0f

        if (agentsCount == latestAgentsCount && overLimitAgents == latestOverLimitAgents) return

        if ((popUpDelayTimer.hasExpired() || (isNotificationVisible() && showOverLimitAgents))
            && (overLimitPct > 0f || latestOverLimitPct > 0f)
            && abs(overLimitPct - latestOverLimitPct) > latestOverLimitPct * RENDER_ALLOWED_CHANGE_PCT
        ) {
            displayNotification(true)
            val popUpDelay = gSavedSettings.getU32("ComplexityChangesPopUpDelay", 300u)
            popUpDelayTimer.resetWithExpiry(popUpDelay.toFloat())
        }
    }

    fun updateNotificationState() {
        if (!isAgentAvatarValid()) return
        if (lastCofVersion < 0
            && gAgentWearables.areWearablesLoaded()
            && LLAttachmentsMgr.getInstance().isAttachmentStateComplete()
        ) {
            lastCofVersion = LLAppearanceMgr.instance().getCOFVersion()
            lastSkeletonSerialNum = gAgentAvatarp!!.mLastSkeletonSerialNum
        } else if (lastCofVersion >= 0
            && (lastCofVersion != LLAppearanceMgr.instance().getCOFVersion()
                    || lastSkeletonSerialNum != gAgentAvatarp!!.mLastSkeletonSerialNum)
        ) {
            notifyOutfitLoading = true
            lastCofVersion = LLAppearanceMgr.instance().getCOFVersion()
            lastSkeletonSerialNum = gAgentAvatarp!!.mLastSkeletonSerialNum
        }

        if ((gAgentAvatarp?.mLastRezzedStatus ?: 0) < lastOutfitRezStatus) {
            notifyOutfitLoading = true
        }
        lastOutfitRezStatus = gAgentAvatarp?.mLastRezzedStatus ?: -1
    }

    fun updateNotificationAgent(agentComplexity: UInt) {
        latestAgentComplexity = agentComplexity
        val showChanges = gSavedSettings.getU32("ShowMyComplexityChanges", 20u)
        if (showChanges == 0u) return
        if (!isAgentAvatarValid() || !gAgentWearables.areWearablesLoaded()) return

        if (!notifyOutfitLoading) {
            updateNotificationState()
            if (latestOverLimitAgents > 0u) notifyOutfitLoading = true
            if (!notifyOutfitLoading) {
                this.agentComplexity = latestAgentComplexity
                return
            }
        }

        if (this.agentComplexity != latestAgentComplexity) {
            displayNotification(false)
            popUpDelayTimer.resetWithExpiry(OVER_LIMIT_UPDATE_DELAY.toFloat())
        }
    }
}

// ---------------------------------------------------------------------------
// HUDRenderNotifier  (C++ LLHUDRenderNotifier singleton)
// ---------------------------------------------------------------------------

object HUDRenderNotifier {

    enum class WarnLevel(val level: Int) {
        WARN_NONE(-1),
        WARN_TEXTURES(0),
        WARN_CRAMPED(1),
        WARN_HEAVY(2),
        WARN_COST(3),
        WARN_MEMORY(4);
    }

    private val hudMessages = arrayOf(
        "hud_render_textures_warning",
        "hud_render_cramped_warning",
        "hud_render_heavy_textures_warning",
        "hud_render_cost_warning",
        "hud_render_memory_warning",
    )

    private var hudNotificationPtr: LLNotificationPtr? = null
    private var reportedHUDComplexity: HUDComplexity = HUDComplexity()
    private var reportedHUDWarning: WarnLevel = WarnLevel.WARN_NONE
    private var latestHUDComplexity: HUDComplexity = HUDComplexity()
    private val hudPopUpDelayTimer: LLFrameTimer = LLFrameTimer()
    var hudComplexityList: MutableList<HUDComplexity> = mutableListOf()
    var hudsCount: Int = 0
        private set

    fun isNotificationVisible(): Boolean =
        hudNotificationPtr?.isActive() == true

    fun updateNotificationHUD(complexity: List<HUDComplexity>) {
        if (!isAgentAvatarValid() || !gAgentWearables.areWearablesLoaded()) return

        hudComplexityList = complexity.toMutableList()
        hudsCount = hudComplexityList.size

        val showChanges = gSavedSettings.getU32("ShowMyComplexityChanges", 20u)
        if (showChanges == 0u) return

        val newTotal = HUDComplexity()
        var reportComplexity = HUDComplexity()
        var warningLevel = WarnLevel.WARN_NONE

        for (obj in complexity) {
            val objLevel = getWarningType(obj, reportComplexity)
            if (objLevel.level >= 0) {
                warningLevel = objLevel
                reportComplexity = obj
            }
            newTotal.objectsCost += obj.objectsCost
            newTotal.objectsCount += obj.objectsCount
            newTotal.texturesCost += obj.texturesCost
            newTotal.texturesCount += obj.texturesCount
            newTotal.largeTexturesCount += obj.largeTexturesCount
            newTotal.texturesMemoryTotal += obj.texturesMemoryTotal
        }

        if (hudPopUpDelayTimer.hasExpired() || isNotificationVisible()) {
            if (warningLevel.level >= 0) {
                if (reportedHUDComplexity.objectId != reportComplexity.objectId
                    || reportedHUDWarning != warningLevel
                ) {
                    displayHUDNotification(warningLevel, reportComplexity.objectId, reportComplexity.objectName, reportComplexity.jointName)
                    reportedHUDComplexity = reportComplexity
                    reportedHUDWarning = warningLevel
                }
            } else {
                val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
                if (!reportedHUDComplexity.objectId.equals(nullId)) {
                    reportedHUDComplexity.reset()
                    reportedHUDWarning = WarnLevel.WARN_NONE
                }
                val totalLevel = getWarningType(newTotal, reportedHUDComplexity)
                if (totalLevel.level >= 0 && reportedHUDWarning != totalLevel) {
                    displayHUDNotification(totalLevel)
                }
                reportedHUDComplexity = newTotal.copy()
                reportedHUDWarning = totalLevel
            }
        }

        latestHUDComplexity = newTotal.copy()
    }

    private fun getWarningType(obj: HUDComplexity, cmp: HUDComplexity): WarnLevel {
        val maxCost = gSavedSettings.getU32("RenderAvatarMaxComplexity", 0u)
        val maxObjects = gSavedSettings.getU32("RenderHUDObjectsWarning", WARN_HUD_OBJECTS_LIMIT)
        val maxTextures = gSavedSettings.getU32("RenderHUDTexturesWarning", WARN_HUD_TEXTURES_LIMIT)
        val maxOversized = gSavedSettings.getU32("RenderHUDOversizedTexturesWarning", WARN_HUD_OVERSIZED_TEXTURES_LIMIT)
        val maxMemory = gSavedSettings.getU32("RenderHUDTexturesMemoryWarning", WARN_HUD_TEXTURE_MEMORY_LIMIT)

        return when {
            cmp.texturesMemoryTotal < obj.texturesMemoryTotal
                    && obj.texturesMemoryTotal > maxMemory.toDouble() ->
                WarnLevel.WARN_MEMORY

            (cmp.objectsCost < obj.objectsCost || cmp.texturesCost < obj.texturesCost)
                    && maxCost > 0u
                    && obj.objectsCost + obj.texturesCost > maxCost ->
                WarnLevel.WARN_COST

            cmp.largeTexturesCount < obj.largeTexturesCount
                    && obj.largeTexturesCount > maxOversized ->
                WarnLevel.WARN_HEAVY

            cmp.texturesCount < obj.texturesCount
                    && obj.texturesCount > maxTextures ->
                WarnLevel.WARN_CRAMPED

            cmp.objectsCount < obj.objectsCount
                    && obj.objectsCount > maxObjects ->
                WarnLevel.WARN_TEXTURES

            else -> WarnLevel.WARN_NONE
        }
    }

    private fun displayHUDNotification(
        warnType: WarnLevel,
        objId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
        objName: String = "",
        jointName: String = "",
    ) {
        val popUpDelay = gSavedSettings.getU32("ComplexityChangesPopUpDelay", 300u)
        val expireDelay = gSavedSettings.getU32("ShowMyComplexityChanges", 20u)
        val expireDate = LLDate(LLDate.now().secondsSinceEpoch() + expireDelay.toDouble())

        val nullId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val hudDetails: String = if (objId == nullId) {
            LLTrans.getString("hud_description_total")
        } else {
            if (jointName.isEmpty()) {
                val verb = "select?name=" + LLURI.escape(objName)
                LLSLURL("inventory", objId, verb).getSLURLString()
            } else {
                val verb = "select?name=" + LLURI.escape(objName)
                val objArgs = LLSD.mapOf(
                    "OBJ_NAME" to LLSD.fromString(LLSLURL("inventory", objId, verb).getSLURLString()),
                    "JNT_NAME" to LLSD.fromString(LLTrans.getString(jointName))
                )
                LLTrans.getString("hud_name_with_joint", objArgs)
            }
        }

        val reasonArgs = LLSD.mapOf("HUD_DETAILS" to LLSD.fromString(hudDetails))
        val msgArgs = LLSD.mapOf(
            "HUD_REASON" to LLSD.fromString(LLTrans.getString(hudMessages[warnType.level], reasonArgs))
        )

        hudNotificationPtr = LLNotifications.instance().add(
            LLNotification.Params()
                .name("HUDComplexityWarning")
                .expiry(expireDate)
                .substitutions(msgArgs)
        )
        hudPopUpDelayTimer.resetWithExpiry(popUpDelay.toFloat())
    }
}
