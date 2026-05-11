package com.firestorm.newview

import java.util.UUID

// ============================================================================
// RlvException — exception entry stored in the handler
// ============================================================================

data class RlvException(
    val idObject: UUID,
    val behaviour: ERlvBehaviour,
    val varOption: Any   // String | UUID | Int | ERlvBehaviour
)

// ============================================================================
// RlvHandler — central RLV restriction manager (singleton in C++, object here)
// ============================================================================

object RlvHandler {

    val instance: RlvHandler get() = this

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private var enabled: Boolean = false

    private val objects: MutableMap<UUID, RlvObject> = mutableMapOf()
    private val blockedObjects: MutableList<Triple<UUID, String, Double>> = mutableListOf()
    private val exceptions: MutableMap<ERlvBehaviour, MutableList<RlvException>> = mutableMapOf()
    private val behaviours: ShortArray = ShortArray(ERlvBehaviour.RLV_BHVR_COUNT.ordinal) { 0 }

    private val retained: MutableList<RlvCommand> = mutableListOf()

    // Execution stacks — allow nested command processing
    private val curCommandStack: ArrayDeque<RlvCommand> = ArrayDeque()
    private val curObjectStack: ArrayDeque<UUID> = ArrayDeque()

    // Behaviour-change signal: listeners receive (ERlvBehaviour, ERlvParamType)
    val onBehaviour: MutableList<(ERlvBehaviour, ERlvParamType) -> Unit> = mutableListOf()
    val onBehaviourToggle: MutableList<(ERlvBehaviour, ERlvParamType) -> Unit> = mutableListOf()
    val onCommand: MutableList<(RlvCommand, ERlvCmdRet, Boolean) -> Unit> = mutableListOf()

    private val commandHandlers: MutableList<RlvExtCommandHandler> = mutableListOf()

    // Misc per-handler state
    private var canCancelTp: Boolean = true
    private var posSitSource: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
    private var pendingGroundSit: Boolean = false
    private var idPendingSitActor: UUID = UUID(0, 0)
    private var idPendingUnsitActor: UUID = UUID(0, 0)
    private var idAgentGroup: UUID = UUID(0, 0)
    private var cameraPresetRestore: String = ""

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun canEnable(): Boolean = !enabled

    fun isEnabled(): Boolean = enabled

    fun setEnabled(enable: Boolean): Boolean {
        if (enabled == enable) return false
        enabled = enable
        return true
    }

    fun cleanup() {
        if (!enabled) return
        val ids = objects.keys.toList()
        for (id in ids) processCommand(id, "clear", fromObj = true)
        TODO("APR: use JVM equivalent - remove agent listener, disconnect signals, clear retained list")
        commandHandlers.clear()
        enabled = false
    }

    // -----------------------------------------------------------------------
    // Behaviour queries
    // -----------------------------------------------------------------------

    fun findBehaviour(eBhvr: ERlvBehaviour): List<RlvObject> =
        objects.values.filter { it.hasBehaviour(eBhvr, strictOnly = false) }

    fun getObject(idRlvObj: UUID): RlvObject? = objects[idRlvObj]

    fun hasBehaviour(eBhvr: ERlvBehaviour): Boolean =
        if (eBhvr.ordinal < ERlvBehaviour.RLV_BHVR_COUNT.ordinal) behaviours[eBhvr.ordinal] != 0.toShort() else false

    fun hasBehaviour(eBhvr: ERlvBehaviour, strOption: String): Boolean =
        hasBehaviourExcept(eBhvr, strOption, UUID(0, 0))

    fun hasBehaviour(idObj: UUID): Boolean = objects.containsKey(idObj)

    fun hasBehaviour(idObj: UUID, eBhvr: ERlvBehaviour, strOption: String = ""): Boolean =
        objects[idObj]?.hasBehaviour(eBhvr, strOption, strictOnly = false) ?: false

    fun hasBehaviourExcept(eBhvr: ERlvBehaviour, idObj: UUID): Boolean =
        hasBehaviourExcept(eBhvr, "", idObj)

    fun hasBehaviourExcept(eBhvr: ERlvBehaviour, strOption: String, idObj: UUID): Boolean =
        objects.values.any { it.getObjectID() != idObj && it.hasBehaviour(eBhvr, strOption, strictOnly = false) }

    fun hasBehaviourRoot(idObjRoot: UUID, eBhvr: ERlvBehaviour, strOption: String = ""): Boolean =
        objects.values.any { it.getRootID() == idObjRoot && it.hasBehaviour(eBhvr, strOption, strictOnly = false) }

    fun ownsBehaviour(idObj: UUID, eBhvr: ERlvBehaviour): Boolean {
        var hasBhvr = false
        for ((k, v) in objects) {
            if (v.hasBehaviour(eBhvr, strictOnly = false)) {
                if (k != idObj) return false
                hasBhvr = true
            }
        }
        return hasBhvr
    }

    // -----------------------------------------------------------------------
    // Exception handling
    // -----------------------------------------------------------------------

    fun addException(idObj: UUID, eBhvr: ERlvBehaviour, varOption: Any) {
        exceptions.getOrPut(eBhvr) { mutableListOf() }.add(RlvException(idObj, eBhvr, varOption))
    }

    fun removeException(idObj: UUID, eBhvr: ERlvBehaviour, varOption: Any) {
        exceptions[eBhvr]?.removeIf { it.idObject == idObj && it.varOption == varOption }
    }

    fun hasException(eBhvr: ERlvBehaviour): Boolean = exceptions[eBhvr]?.isNotEmpty() == true

    fun isException(eBhvr: ERlvBehaviour, varOption: Any, eCheckType: ERlvExceptionCheck = ERlvExceptionCheck.Default): Boolean {
        val resolved = if (eCheckType == ERlvExceptionCheck.Default) {
            if (hasBehaviour(eBhvr) && !isPermissive(eBhvr)) ERlvExceptionCheck.Strict else ERlvExceptionCheck.Permissive
        } else eCheckType

        val list = exceptions[eBhvr] ?: return false

        if (resolved == ERlvExceptionCheck.Permissive) {
            return list.any { it.varOption == varOption }
        }

        // Strict: every object holding the behaviour must also have the exception
        val objsWithBhvr = objects.values
            .filter { it.hasBehaviour(eBhvr, !hasBehaviour(ERlvBehaviour.RLV_BHVR_PERMISSIVE)) }
            .map { it.getObjectID() }
            .toMutableList()

        for (ex in list) {
            if (ex.varOption == varOption) {
                objsWithBhvr.remove(ex.idObject)
                if (objsWithBhvr.isEmpty()) return true
            }
        }
        return false
    }

    fun isPermissive(eBhvr: ERlvBehaviour): Boolean {
        return if (RlvBehaviourDictionary.getHasStrict(eBhvr))
            !(hasBehaviour(ERlvBehaviour.RLV_BHVR_PERMISSIVE) ||
              isException(ERlvBehaviour.RLV_BHVR_PERMISSIVE, eBhvr, ERlvExceptionCheck.Permissive))
        else true
    }

    // -----------------------------------------------------------------------
    // Blocked object handling
    // -----------------------------------------------------------------------

    fun addBlockedObject(idObj: UUID, strObjName: String) {
        blockedObjects.add(Triple(idObj, strObjName, System.currentTimeMillis() / 1000.0))
    }

    fun hasUnresolvedBlockedObject(): Boolean =
        blockedObjects.any { it.first == UUID(0, 0) }

    fun isBlockedObject(idObj: UUID): Boolean =
        blockedObjects.any { it.first == idObj }

    fun removeBlockedObject(idObj: UUID) {
        if (idObj != UUID(0, 0))
            blockedObjects.removeIf { it.first == idObj }
    }

    // -----------------------------------------------------------------------
    // Command processing
    // -----------------------------------------------------------------------

    fun processCommand(idObj: UUID, strCommand: String, fromObj: Boolean): ERlvCmdRet {
        val rlvCmd = RlvCommand(idObj, strCommand)
        return processCommand(rlvCmd, fromObj)
    }

    private fun processCommand(rlvCmd: RlvCommand, fromObj: Boolean): ERlvCmdRet {
        if (isBlockedObject(rlvCmd.getObjectID()) &&
            rlvCmd.getParamType() != ERlvParamType.RLV_TYPE_REMOVE &&
            rlvCmd.getParamType() != ERlvParamType.RLV_TYPE_CLEAR) {
            return ERlvCmdRet.RLV_RET_FAILED_BLOCKED
        }
        if (!rlvCmd.isValid()) return ERlvCmdRet.RLV_RET_FAILED_SYNTAX
        if (rlvCmd.isBlocked()) return ERlvCmdRet.RLV_RET_FAILED_DISABLED

        curCommandStack.addLast(rlvCmd)
        curObjectStack.addLast(rlvCmd.getObjectID())

        val eRet: ERlvCmdRet = when (rlvCmd.getParamType()) {
            ERlvParamType.RLV_TYPE_ADD,
            ERlvParamType.RLV_TYPE_REMOVE -> processAddRemCommand(rlvCmd)
            ERlvParamType.RLV_TYPE_FORCE  -> processForceCommand(rlvCmd)
            ERlvParamType.RLV_TYPE_REPLY  -> processReplyCommand(rlvCmd)
            ERlvParamType.RLV_TYPE_CLEAR  -> processClearCommand(rlvCmd)
            else -> ERlvCmdRet.RLV_RET_FAILED_PARAM
        }

        for (listener in onCommand) listener(rlvCmd, eRet, fromObj)
        curCommandStack.removeLastOrNull()
        curObjectStack.removeLastOrNull()
        return eRet
    }

    private fun processAddRemCommand(rlvCmd: RlvCommand): ERlvCmdRet {
        TODO("APR: use JVM equivalent - look up behaviour handler in dictionary and execute add/rem command logic")
    }

    private fun processForceCommand(rlvCmd: RlvCommand): ERlvCmdRet {
        TODO("APR: use JVM equivalent - look up force handler in dictionary and execute force command logic")
    }

    private fun processReplyCommand(rlvCmd: RlvCommand): ERlvCmdRet {
        TODO("APR: use JVM equivalent - look up reply handler in dictionary and send reply via sendChatReply")
    }

    private fun processClearCommand(rlvCmd: RlvCommand): ERlvCmdRet {
        TODO("APR: use JVM equivalent - remove all restrictions from the specified object and clean up state")
    }

    fun processRetainedCommands(eBhvrFilter: ERlvBehaviour = ERlvBehaviour.RLV_BHVR_UNKNOWN, eTypeFilter: ERlvParamType = ERlvParamType.RLV_TYPE_UNKNOWN) {
        TODO("APR: use JVM equivalent - replay commands from retained list matching the given filters")
    }

    fun processIMQuery(idSender: UUID, strCommand: String): Boolean {
        TODO("APR: use JVM equivalent - handle RLVa IM query protocol (@list/@clear style commands via IM)")
    }

    // -----------------------------------------------------------------------
    // Current command/object accessors
    // -----------------------------------------------------------------------

    fun getCurrentCommand(): RlvCommand? = curCommandStack.lastOrNull()
    fun getCurrentObject(): UUID = curObjectStack.lastOrNull() ?: UUID(0, 0)

    // -----------------------------------------------------------------------
    // Accessors / mutators
    // -----------------------------------------------------------------------

    fun getAgentGroup(): UUID = idAgentGroup
    fun getCanCancelTp(): Boolean = canCancelTp
    fun setCanCancelTp(allow: Boolean) { canCancelTp = allow }
    fun getSitSource(): Triple<Double, Double, Double> = posSitSource
    fun setSitSource(pos: Triple<Double, Double, Double>) { posSitSource = pos }

    // -----------------------------------------------------------------------
    // Command handlers registration
    // -----------------------------------------------------------------------

    fun addCommandHandler(handler: RlvExtCommandHandler) {
        if (!commandHandlers.contains(handler)) commandHandlers.add(handler)
    }

    fun removeCommandHandler(handler: RlvExtCommandHandler) {
        commandHandlers.remove(handler)
    }

    private fun clearCommandHandlers() {
        commandHandlers.clear()
    }

    private fun notifyCommandHandlers(
        invoke: (RlvExtCommandHandler, RlvCommand, ERlvCmdRet) -> Boolean,
        rlvCmd: RlvCommand,
        notifyAll: Boolean
    ): Pair<Boolean, ERlvCmdRet> {
        var ret = ERlvCmdRet.RLV_RET_UNKNOWN
        var handled = false
        for (handler in commandHandlers) {
            if (invoke(handler, rlvCmd, ret)) {
                handled = true
                if (!notifyAll) break
            }
        }
        return Pair(handled, ret)
    }

    // -----------------------------------------------------------------------
    // Chat filtering helpers
    // -----------------------------------------------------------------------

    fun filterChat(text: StringBuilder, filterEmote: Boolean): Boolean {
        TODO("APR: use JVM equivalent - apply sendchat/recvchat/redirchat filters to text")
    }

    fun redirectChatOrEmote(text: String): Boolean {
        TODO("APR: use JVM equivalent - redirect chat/emote to registered notify channels")
    }

    // -----------------------------------------------------------------------
    // Event callbacks (viewer lifecycle hooks)
    // -----------------------------------------------------------------------

    fun onActiveGroupChanged() {
        TODO("APR: use JVM equivalent - handle active group change, update idAgentGroup and notify @setgroup watchers")
    }

    fun onAttach(attachObj: Any?, attachPt: Any?) {
        TODO("APR: use JVM equivalent - process retained commands and check attachment locks on attach")
    }

    fun onDetach(attachObj: Any?, attachPt: Any?) {
        TODO("APR: use JVM equivalent - clear all restrictions held by the detaching object")
    }

    fun onExperienceAttach(sdExperience: Map<String, Any>, strObjName: String) {
        TODO("APR: use JVM equivalent - check if the experience is allowed and add to blocked objects if not")
    }

    fun onExperienceEvent(sdEvent: Map<String, Any>) {
        TODO("APR: use JVM equivalent - fetch attachment resources for experience-owned objects and resolve blocked list")
    }

    fun onGC(): Boolean {
        TODO("APR: use JVM equivalent - garbage-collect RlvObjects whose source objects have left the world")
    }

    fun onLoginComplete() {
        TODO("APR: use JVM equivalent - process retained commands after login is complete")
    }

    fun onSitOrStand(sitting: Boolean) {
        TODO("APR: use JVM equivalent - update canCancelTp and handle standtp/sitground state")
    }

    fun onTeleportFailed() {
        TODO("APR: use JVM equivalent - restore sit source and notify pending unsit actor")
    }

    fun onTeleportFinished(posArrival: Triple<Double, Double, Double>) {
        TODO("APR: use JVM equivalent - clear tplm/tploc retained state and trigger post-tp behaviour logic")
    }

    // -----------------------------------------------------------------------
    // Object map accessor (needed by RlvFloaterBehaviours)
    // -----------------------------------------------------------------------

    fun getObjectMap(): Map<UUID, RlvObject> = objects
}
