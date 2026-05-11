package com.firestorm.newview

import java.util.UUID

enum class ERlvCheckType { All, Some, Nothing }

// ============================================================================
// RlvActions — developer-facing API; prefer this over RlvHandler at call sites
// ============================================================================

object RlvActions {

    // Backwards logic: initialised false so nothing is blocked when RLVa is disabled
    enum class EShowNamesContext { SNC_DEFAULT, SNC_TELEPORTOFFER, SNC_TELEPORTREQUEST, SNC_COUNT }
    private val blockNamesContexts = BooleanArray(EShowNamesContext.SNC_COUNT.ordinal) { false }

    // ======
    // Camera
    // ======

    fun canChangeCameraFOV(idRlvObject: UUID): Boolean {
        return !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM) ||
            RlvHandler.instance.hasBehaviour(idRlvObject, ERlvBehaviour.RLV_BHVR_SETCAM)
    }

    fun canChangeCameraPreset(idRlvObject: UUID): Boolean {
        return (
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM) ||
            RlvHandler.instance.hasBehaviour(idRlvObject, ERlvBehaviour.RLV_BHVR_SETCAM)
        ) &&
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_EYEOFFSET) &&
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_EYEOFFSETSCALE) &&
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_FOCUSOFFSET)
    }

    fun canChangeToMouselook(): Boolean {
        val camDistMinMod = RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_AVDISTMIN)
        val mouselookBlocked = if (!RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM))
            RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_MOUSELOOK)
        else
            RlvHandler.instance.hasBehaviour(camDistMinMod.getPrimaryObject(), ERlvBehaviour.RLV_BHVR_SETCAM_MOUSELOOK)
        return !mouselookBlocked &&
            (!camDistMinMod.hasValue() || (camDistMinMod.getValue() as? Float) == 0f)
    }

    fun isCameraDistanceClamped(): Boolean =
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_AVDISTMIN) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_AVDISTMAX) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_ORIGINDISTMIN) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_ORIGINDISTMAX)

    fun isCameraFOVClamped(): Boolean =
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_FOVMIN) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_FOVMAX)

    fun isCameraPresetLocked(): Boolean =
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_EYEOFFSET) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_EYEOFFSETSCALE) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_FOCUSOFFSET)

    fun getCameraAvatarDistanceLimits(): Pair<Float, Float>? {
        val hasMin = RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_AVDISTMIN)
        val hasMax = RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_AVDISTMAX)
        if (!hasMin && !hasMax) return null
        val min = if (hasMin) RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_AVDISTMIN).getValue() as Float else 0f
        val max = if (hasMax) RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_AVDISTMAX).getValue() as Float else Float.MAX_VALUE
        return Pair(min, max)
    }

    fun getCameraOriginDistanceLimits(): Pair<Float, Float>? {
        val hasMin = RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_ORIGINDISTMIN)
        val hasMax = RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_ORIGINDISTMAX)
        if (!hasMin && !hasMax) return null
        val min = if (hasMin) RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_ORIGINDISTMIN).getValue() as Float else 0f
        val max = if (hasMax) RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_ORIGINDISTMAX).getValue() as Float else Float.MAX_VALUE
        return Pair(min, max)
    }

    fun getCameraFOVLimits(): Pair<Float, Float>? {
        val hasMin = RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_FOVMIN)
        val hasMax = RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETCAM_FOVMAX)
        if (!hasMin && !hasMax) return null
        val min = if (hasMin) RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_FOVMIN).getValue() as Float
                  else TODO("GPU: return viewer minimum FOV")
        val max = if (hasMax) RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_FOVMAX).getValue() as Float
                  else TODO("GPU: return viewer maximum FOV")
        return Pair(min, max)
    }

    // ================================
    // Communication / Avatar interaction
    // ================================

    fun canChangeActiveGroup(idRlvObject: UUID = UUID(0, 0)): Boolean =
        if (idRlvObject == UUID(0, 0)) !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETGROUP)
        else !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_SETGROUP, idRlvObject)

    fun canGiveInventory(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHARE) ||
        RlvHandler.instance.hasException(ERlvBehaviour.RLV_BHVR_SHARE)

    fun canGiveInventory(idAgent: UUID): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHARE) ||
        RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_SHARE, idAgent)

    fun canReceiveIM(idSender: UUID): Boolean {
        if (!isRlvEnabled()) return true
        return (
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_RECVIM) ||
            RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_RECVIM, idSender) ||
            rlvCheckAvatarIMDistance(idSender, ERlvBehaviourModifier.RLV_MODIFIER_RECVIMDISTMIN, ERlvBehaviourModifier.RLV_MODIFIER_RECVIMDISTMAX)
        ) && (
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_RECVIMFROM) ||
            !RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_RECVIMFROM, idSender)
        )
    }

    fun canPlayGestures(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SENDGESTURE)

    fun canSendChannel(nChannel: Int): Boolean =
        (!RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SENDCHANNEL) ||
         RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_SENDCHANNEL, nChannel)) &&
        (!RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SENDCHANNELEXCEPT) ||
         !RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_SENDCHANNELEXCEPT, nChannel))

    fun canSendIM(idRecipient: UUID): Boolean {
        if (!isRlvEnabled()) return true
        return (
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SENDIM) ||
            RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_SENDIM, idRecipient) ||
            rlvCheckAvatarIMDistance(idRecipient, ERlvBehaviourModifier.RLV_MODIFIER_SENDIMDISTMIN, ERlvBehaviourModifier.RLV_MODIFIER_SENDIMDISTMAX)
        ) && (
            !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SENDIMTO) ||
            !RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_SENDIMTO, idRecipient)
        )
    }

    fun canSendTypingStart(): Boolean {
        val showTyping = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.ShowRedirectChatTyping}'") as Boolean
        return !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_REDIRCHAT) || showTyping
    }

    fun canStartIM(idRecipient: UUID, ignoreOpen: Boolean = false): Boolean {
        if (!isRlvEnabled()) return true
        return (
            (
                !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_STARTIM) ||
                RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_STARTIM, idRecipient) ||
                rlvCheckAvatarIMDistance(idRecipient, ERlvBehaviourModifier.RLV_MODIFIER_STARTIMDISTMIN, ERlvBehaviourModifier.RLV_MODIFIER_STARTIMDISTMAX)
            ) && (
                !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_STARTIMTO) ||
                !RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_STARTIMTO, idRecipient)
            )
        ) || (
            !ignoreOpen && (hasOpenP2PSession(idRecipient) || hasOpenGroupSession(idRecipient))
        )
    }

    fun canShowName(eContext: EShowNamesContext, idAgent: UUID = UUID(0, 0)): Boolean {
        if (!blockNamesContexts[eContext.ordinal]) return true
        if (idAgent != UUID(0, 0)) {
            return when (eContext) {
                EShowNamesContext.SNC_DEFAULT,
                EShowNamesContext.SNC_TELEPORTOFFER,
                EShowNamesContext.SNC_TELEPORTREQUEST ->
                    RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_SHOWNAMES, idAgent) ||
                    TODO("APR: use JVM equivalent - compare idAgent with local agent UUID") as Boolean
                else -> false
            }
        }
        return false
    }

    fun canShowNameTag(avatarId: UUID, avatarPosition: Triple<Double, Double, Double>): Boolean {
        val handler = RlvHandler.instance
        if (!handler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWNAMETAGS) ||
            handler.isException(ERlvBehaviour.RLV_BHVR_SHOWNAMETAGS, avatarId) ||
            TODO("APR: use JVM equivalent - compare avatarId with local agent UUID") as Boolean) return true
        val distMod = RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SHOWNAMETAGSDIST)
        val tagDist = distMod.getValue() as? Float ?: 0f
        if (tagDist == 0f) return false
        TODO("APR: use JVM equivalent - compute squared distance from agent to avatarPosition and compare against tagDist^2")
    }

    fun canShowNearbyAgents(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWNEARBY)

    fun checkChatVolume(chatType: Int): Int {
        val handler = RlvHandler.instance
        return when {
            (chatType == CHAT_TYPE_SHOUT || chatType == CHAT_TYPE_NORMAL) &&
            handler.hasBehaviour(ERlvBehaviour.RLV_BHVR_CHATNORMAL) -> CHAT_TYPE_WHISPER

            chatType == CHAT_TYPE_SHOUT &&
            handler.hasBehaviour(ERlvBehaviour.RLV_BHVR_CHATSHOUT) -> CHAT_TYPE_NORMAL

            chatType == CHAT_TYPE_WHISPER &&
            handler.hasBehaviour(ERlvBehaviour.RLV_BHVR_CHATWHISPER) -> CHAT_TYPE_NORMAL

            else -> chatType
        }
    }

    fun setShowName(eContext: EShowNamesContext, canShowName: Boolean) {
        if (eContext.ordinal < EShowNamesContext.SNC_COUNT.ordinal && isRlvEnabled())
            blockNamesContexts[eContext.ordinal] = !canShowName
    }

    // =========
    // Inventory
    // =========

    fun canPasteInventory(sourceCat: Any?, destCat: Any?): Boolean {
        if (!isRlvEnabled()) return true
        TODO("APR: use JVM equivalent - check RlvFolderLocks canMoveFolder for source/dest category UUIDs")
    }

    fun canPasteInventory(sourceItem: Any?, destCat: Any?): Boolean {
        if (!isRlvEnabled()) return true
        TODO("APR: use JVM equivalent - check RlvFolderLocks canMoveItem for sourceItem/destCat UUIDs")
    }

    fun canPreviewTextures(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_VIEWTEXTURE)

    // ========
    // Movement
    // ========

    fun canAcceptTpOffer(idSender: UUID): Boolean =
        (!RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_TPLURE) ||
         RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_TPLURE, idSender)) && canStand()

    fun autoAcceptTeleportOffer(idSender: UUID): Boolean =
        (idSender != UUID(0, 0) && RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_ACCEPTTP, idSender)) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_ACCEPTTP)

    fun canAcceptTpRequest(idSender: UUID): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_TPREQUEST) ||
        RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_TPREQUEST, idSender)

    fun autoAcceptTeleportRequest(idRequester: UUID): Boolean =
        (idRequester != UUID(0, 0) && RlvHandler.instance.isException(ERlvBehaviour.RLV_BHVR_ACCEPTTPREQUEST, idRequester)) ||
        RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_ACCEPTTPREQUEST)

    fun canFly(): Boolean {
        val cmd = RlvHandler.instance.getCurrentCommand()
        return if (cmd == null) !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_FLY)
               else !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_FLY, RlvHandler.instance.getCurrentObject())
    }

    fun canFly(idRlvObjExcept: UUID): Boolean =
        !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_FLY, idRlvObjExcept)

    fun canJump(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_JUMP)

    // ===========
    // Teleporting
    // ===========

    fun canTeleportToLocal(posGlobal: Triple<Double, Double, Double>): Boolean {
        val idRlvObjExcept = RlvHandler.instance.getCurrentObject()
        var canTp = canStand(idRlvObjExcept)
        if (canTp && RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_SITTP, idRlvObjExcept)) {
            val sitTpDist = RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SITTPDIST).getValue() as Float
            canTp = TODO("APR: use JVM equivalent - compute 3D squared distance from agent to posGlobal < sitTpDist^2") as Boolean
        }
        if (canTp && RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_TPLOCAL, idRlvObjExcept)) {
            val tpLocalDist = minOf(
                RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_TPLOCALDIST).getValue() as Float,
                RLV_MODIFIER_TPLOCAL_DEFAULT
            )
            canTp = TODO("APR: use JVM equivalent - compute 2D XY squared distance from agent to posGlobal < tpLocalDist^2") as Boolean
        }
        return canTp
    }

    fun canTeleportToLocation(): Boolean {
        val idRlvObjExcept = RlvHandler.instance.getCurrentObject()
        return !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_TPLOC, idRlvObjExcept) &&
               canStand(idRlvObjExcept)
    }

    fun isLocalTp(posGlobal: Triple<Double, Double, Double>): Boolean {
        TODO("APR: use JVM equivalent - compute 2D XY squared distance from agent to posGlobal < RLV_MODIFIER_TPLOCAL_DEFAULT^2")
    }

    // =========
    // WindLight
    // =========

    fun canChangeEnvironment(idRlvObject: UUID = UUID(0, 0)): Boolean =
        if (idRlvObject == UUID(0, 0)) !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETENV)
        else !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_SETENV, idRlvObject)

    fun hasPostProcess(): Boolean {
        TODO("GPU: check if RlvSphere visual effect is active in LLVfxManager")
    }

    // =================
    // World interaction
    // =================

    fun canBuild(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDIT) ||
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_REZ)

    fun canBuyObject(idObj: UUID): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_BUY)

    fun canEdit(eCheckType: ERlvCheckType): Boolean {
        val h = RlvHandler.instance
        return when (eCheckType) {
            ERlvCheckType.All ->
                !h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDIT) && !h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDITOBJ) &&
                !h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDITATTACH) && !h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDITWORLD)
            ERlvCheckType.Some ->
                (!h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDIT) || h.hasException(ERlvBehaviour.RLV_BHVR_EDIT)) &&
                (!h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDITATTACH) || h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDITWORLD))
            ERlvCheckType.Nothing ->
                (h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDIT) && !h.hasException(ERlvBehaviour.RLV_BHVR_EDIT)) ||
                (h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDITATTACH) && h.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDITWORLD))
        }
    }

    fun canEdit(obj: Any?): Boolean {
        if (obj == null) return false
        TODO("APR: use JVM equivalent - evaluate edit/editobj/editattach/editworld restrictions for the given object")
    }

    fun canGroundSit(): Boolean =
        !hasBehaviour(ERlvBehaviour.RLV_BHVR_SIT) && canStand()

    fun canGroundSit(idRlvObjExcept: UUID): Boolean =
        !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_SIT, idRlvObjExcept) &&
        canStand(idRlvObjExcept)

    fun canInteract(obj: Any?, posOffset: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)): Boolean {
        if (obj == null) return true
        TODO("APR: use JVM equivalent - check interact/fartouch restrictions and HUD attachment status")
    }

    fun canPayAvatar(idAvatar: UUID): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_PAY)

    fun canPayObject(idObj: UUID): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_BUY)

    fun canRez(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_REZ)

    fun canShowHoverText(obj: Any?): Boolean {
        if (obj == null) return true
        TODO("APR: use JVM equivalent - check showhovertextall/world/hud/showhovertext exception for the object")
    }

    fun canSit(obj: Any?, posOffset: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)): Boolean {
        if (obj == null) return false
        TODO("APR: use JVM equivalent - check sit/unsit/standtp/sittp/fartouch restrictions and sitting state")
    }

    fun canShowLocation(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)

    fun canStand(): Boolean {
        if (!RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_UNSIT)) return true
        return TODO("APR: use JVM equivalent - return true if agent avatar is not currently sitting") as Boolean
    }

    fun canStand(idRlvObjExcept: UUID): Boolean {
        if (!RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_UNSIT, idRlvObjExcept)) return true
        return TODO("APR: use JVM equivalent - return true if agent avatar is not currently sitting") as Boolean
    }

    fun canTouch(obj: Any?, posOffset: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)): Boolean {
        if (obj == null) return false
        TODO("APR: use JVM equivalent - evaluate all touch restriction checks (touchall/touchthis/touchworld/touchattach/touchhud/fartouch/touchme)")
    }

    // ===============
    // World (General)
    // ===============

    fun canHighlightTransparent(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDIT) &&
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)

    fun canViewWireframe(): Boolean {
        TODO("APR: use JVM equivalent - check lockedHUD attachments then viewwireframe behaviour")
    }

    // ================
    // Helper functions
    // ================

    fun getModifierValue(eBhvrMod: ERlvBehaviourModifier): Any =
        RlvBehaviourDictionary.getModifier(eBhvrMod).getValue()

    fun hasBehaviour(eBhvr: ERlvBehaviour): Boolean =
        RlvHandler.instance.hasBehaviour(eBhvr)

    fun hasOpenP2PSession(idAgent: UUID): Boolean {
        TODO("APR: use JVM equivalent - check LLIMMgr for an existing P2P IM session with idAgent")
    }

    fun hasOpenGroupSession(idGroup: UUID): Boolean {
        TODO("APR: use JVM equivalent - check LLIMMgr for an existing group IM session with idGroup")
    }

    fun isRlvEnabled(): Boolean = RlvHandler.isEnabled()

    fun notifyBlocked(notification: String, args: Map<String, String> = emptyMap()) {
        RlvUtil.notifyBlocked(notification, args)
    }

    // Internal helper: checks IM distance modifier pair for a given avatar
    private fun rlvCheckAvatarIMDistance(idAvatar: UUID, eModDistMin: ERlvBehaviourModifier, eModDistMax: ERlvBehaviourModifier): Boolean {
        val modMin = RlvBehaviourDictionary.getModifier(eModDistMin)
        if (!modMin.hasValue()) return false
        val hasMax = RlvBehaviourDictionary.getModifier(eModDistMax).hasValue()
        val nMinDist = modMin.getValue() as Float
        val nMaxDist = if (hasMax) RlvBehaviourDictionary.getModifier(eModDistMax).getValue() as Float else Float.MAX_VALUE
        TODO("APR: use JVM equivalent - resolve avatar position from world, compute squared distance from agent, evaluate nMinDist <= dist <= nMaxDist")
    }

    // Chat type constants — match the viewer's EChatType ordinals
    private const val CHAT_TYPE_WHISPER = 0
    private const val CHAT_TYPE_NORMAL = 1
    private const val CHAT_TYPE_SHOUT = 2
}
