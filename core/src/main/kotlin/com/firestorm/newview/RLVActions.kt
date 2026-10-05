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
                  else 0f
        val max = if (hasMax) RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SETCAM_FOVMAX).getValue() as Float
                  else 0f
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
        val showTyping = false
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
                    false
                else -> false
            }
        }
        return false
    }

    fun canShowNameTag(avatarId: UUID, avatarPosition: Triple<Double, Double, Double>): Boolean {
        val handler = RlvHandler.instance
        if (!handler.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWNAMETAGS) ||
            handler.isException(ERlvBehaviour.RLV_BHVR_SHOWNAMETAGS, avatarId) ||
            false) return true
        val distMod = RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_SHOWNAMETAGSDIST)
        val tagDist = distMod.getValue() as? Float ?: 0f
        if (tagDist == 0f) return false
        return false
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
        System.err.println("RlvActions: canPasteInventory(sourceCat, destCat) not yet implemented")
        return false
    }

    fun canPasteInventory(sourceItem: Any?, destCat: Any?): Boolean {
        if (!isRlvEnabled()) return true
        System.err.println("RlvActions: canPasteInventory(sourceItem, destCat) not yet implemented")
        return false
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
            canTp = false
        }
        if (canTp && RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_TPLOCAL, idRlvObjExcept)) {
            val tpLocalDist = minOf(
                RlvBehaviourDictionary.getModifier(ERlvBehaviourModifier.RLV_MODIFIER_TPLOCALDIST).getValue() as Float,
                RLV_MODIFIER_TPLOCAL_DEFAULT
            )
            canTp = false
        }
        return canTp
    }

    fun canTeleportToLocation(): Boolean {
        val idRlvObjExcept = RlvHandler.instance.getCurrentObject()
        return !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_TPLOC, idRlvObjExcept) &&
               canStand(idRlvObjExcept)
    }

    fun isLocalTp(posGlobal: Triple<Double, Double, Double>): Boolean {
        return false
    }

    // =========
    // WindLight
    // =========

    fun canChangeEnvironment(idRlvObject: UUID = UUID(0, 0)): Boolean =
        if (idRlvObject == UUID(0, 0)) !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SETENV)
        else !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_SETENV, idRlvObject)

    fun hasPostProcess(): Boolean {
        // no-op
        return false
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
        System.err.println("RlvActions: canEdit(obj) not yet implemented")
        return false
    }

    fun canGroundSit(): Boolean =
        !hasBehaviour(ERlvBehaviour.RLV_BHVR_SIT) && canStand()

    fun canGroundSit(idRlvObjExcept: UUID): Boolean =
        !RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_SIT, idRlvObjExcept) &&
        canStand(idRlvObjExcept)

    fun canInteract(obj: Any?, posOffset: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)): Boolean {
        if (obj == null) return true
        System.err.println("RlvActions: canInteract(obj, posOffset) not yet implemented")
        return false
    }

    fun canPayAvatar(idAvatar: UUID): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_PAY)

    fun canPayObject(idObj: UUID): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_BUY)

    fun canRez(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_REZ)

    fun canShowHoverText(obj: Any?): Boolean {
        if (obj == null) return true
        System.err.println("RlvActions: canShowHoverText(obj) not yet implemented")
        return false
    }

    fun canSit(obj: Any?, posOffset: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)): Boolean {
        if (obj == null) return false
        System.err.println("RlvActions: canSit(obj, posOffset) not yet implemented")
        return false
    }

    fun canShowLocation(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)

    fun canStand(): Boolean {
        if (!RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_UNSIT)) return true
        return false
    }

    fun canStand(idRlvObjExcept: UUID): Boolean {
        if (!RlvHandler.instance.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_UNSIT, idRlvObjExcept)) return true
        return false
    }

    fun canTouch(obj: Any?, posOffset: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)): Boolean {
        if (obj == null) return false
        System.err.println("RlvActions: canTouch(obj, posOffset) not yet implemented")
        return false
    }

    // ===============
    // World (General)
    // ===============

    fun canHighlightTransparent(): Boolean =
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_EDIT) &&
        !RlvHandler.instance.hasBehaviour(ERlvBehaviour.RLV_BHVR_SHOWLOC)

    fun canViewWireframe(): Boolean {
        System.err.println("RlvActions: canViewWireframe not yet implemented")
        return false
    }

    // ================
    // Helper functions
    // ================

    fun getModifierValue(eBhvrMod: ERlvBehaviourModifier): Any =
        RlvBehaviourDictionary.getModifier(eBhvrMod).getValue()

    fun hasBehaviour(eBhvr: ERlvBehaviour): Boolean =
        RlvHandler.instance.hasBehaviour(eBhvr)

    fun hasOpenP2PSession(idAgent: UUID): Boolean {
        System.err.println("RlvActions: hasOpenP2PSession not yet implemented")
        return false
    }

    fun hasOpenGroupSession(idGroup: UUID): Boolean {
        System.err.println("RlvActions: hasOpenGroupSession not yet implemented")
        return false
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
        System.err.println("RlvActions: rlvCheckAvatarIMDistance not yet implemented")
        return false
    }

    // Chat type constants — match the viewer's EChatType ordinals
    private const val CHAT_TYPE_WHISPER = 0
    private const val CHAT_TYPE_NORMAL = 1
    private const val CHAT_TYPE_SHOUT = 2
}
