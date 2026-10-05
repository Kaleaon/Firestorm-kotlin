package com.firestorm.newview

import java.util.UUID

// ============================================================================
// ERlvBehaviourFilter
//

enum class ERlvBehaviourFilter {
    BEHAVIOURS_ONLY,
    EXCEPTIONS_ONLY,
    ALL
}

// ============================================================================
// Helper functions (package-private)
//

private fun rlvGetItemName(item: ViewerInventoryItem?): String {
    System.err.println("rlvGetItemName: not yet implemented")
    return ""
}

private fun rlvGetItemType(item: ViewerInventoryItem?): String {
    System.err.println("rlvGetItemType: not yet implemented")
    return ""
}

private fun rlvGetItemNameFromObjID(idObj: UUID, includeAttachPt: Boolean = true): String {
    System.err.println("rlvGetItemNameFromObjID: not yet implemented")
    return ""
}

private fun rlvGetShowException(behaviour: ERlvBehaviour): Boolean {
    return when (behaviour) {
        ERlvBehaviour.RLV_BHVR_RECVCHAT,
        ERlvBehaviour.RLV_BHVR_RECVEMOTE,
        ERlvBehaviour.RLV_BHVR_SENDIM,
        ERlvBehaviour.RLV_BHVR_RECVIM,
        ERlvBehaviour.RLV_BHVR_STARTIM,
        ERlvBehaviour.RLV_BHVR_TPLURE,
        ERlvBehaviour.RLV_BHVR_TPREQUEST,
        ERlvBehaviour.RLV_BHVR_ACCEPTTP,
        ERlvBehaviour.RLV_BHVR_ACCEPTTPREQUEST,
        ERlvBehaviour.RLV_BHVR_SHOWNAMES,
        ERlvBehaviour.RLV_BHVR_SHOWNAMETAGS -> true
        else -> false
    }
}

private fun rlvLockMaskToString(lockType: ERlvLockMask): String = when (lockType) {
    ERlvLockMask.RLV_LOCK_ADD -> "add"
    ERlvLockMask.RLV_LOCK_REMOVE -> "rem"
    else -> "unknown"
}

private fun rlvFolderLockPermissionToString(perm: RlvFolderLocks.ELockPermission): String = when (perm) {
    RlvFolderLocks.ELockPermission.PERM_ALLOW -> "allow"
    RlvFolderLocks.ELockPermission.PERM_DENY -> "deny"
    else -> "unknown"
}

private fun rlvFolderLockScopeToString(scope: RlvFolderLocks.ELockScope): String = when (scope) {
    RlvFolderLocks.ELockScope.SCOPE_NODE -> "node"
    RlvFolderLocks.ELockScope.SCOPE_SUBTREE -> "subtree"
    else -> "unknown"
}

private fun rlvFolderLockSourceToTarget(lockSource: RlvFolderLocks.FolderLockSource): String {
    System.err.println("rlvFolderLockSourceToTarget: not yet implemented")
    return ""
}

// ============================================================================
// RlvFloaterBehaviours
//

class RlvFloaterBehaviours {

    private var commandSlot: (() -> Unit)? = null
    private val pendingLookup: MutableList<UUID> = mutableListOf()

    fun onOpen(sdKey: Any) {
        commandSlot = RlvHandler.instance.setCommandCallback { cmd, ret -> onCommand(cmd, ret) }
        refreshAll()
    }

    fun onClose(quitting: Boolean) {
        commandSlot = null
    }

    open fun postBuild(): Boolean {
        System.err.println("RlvFloaterBehaviours: postBuild not yet implemented")
        return false
    }

    private fun onAvatarNameLookup(idAgent: UUID, avName: Any) {
        pendingLookup.remove(idAgent)
        if (isVisible()) refreshAll()
    }

    private fun isVisible(): Boolean {
        System.err.println("RlvFloaterBehaviours: isVisible not yet implemented")
        return false
    }

    private fun onBtnCopyToClipboard() {
        System.err.println("RlvFloaterBehaviours: onBtnCopyToClipboard not yet implemented")
    }

    private fun onCommand(rlvCmd: RlvCommand, ret: ERlvCmdRet) {
        if (rlvCmd.paramType == ERlvParamType.RLV_TYPE_ADD || rlvCmd.paramType == ERlvParamType.RLV_TYPE_REMOVE) {
            refreshAll()
        }
    }

    private fun refreshAll() {
        System.err.println("RlvFloaterBehaviours: refreshAll not yet implemented")
    }

    companion object {
        fun getFormattedBehaviourString(filter: ERlvBehaviourFilter): String {
            val sb = StringBuilder()
            sb.appendLine(RlvStrings.getVersion(null))

            for ((objId, objEntry) in RlvHandler.instance.objectMap) {
                sb.appendLine()
                sb.appendLine("${rlvGetItemNameFromObjID(objId)}:")
                for (rlvCmd in objEntry.commandList) {
                    val isException = rlvCmd.hasOption() && rlvGetShowException(rlvCmd.behaviourType)
                    if (filter == ERlvBehaviourFilter.BEHAVIOURS_ONLY && isException) continue
                    if (filter == ERlvBehaviourFilter.EXCEPTIONS_ONLY && !isException) continue

                    val strOption = resolveOptionString(rlvCmd)
                    sb.append("  -> ${rlvCmd.asString()}")
                    if (strOption.isNotEmpty() && strOption != rlvCmd.option) {
                        sb.append("  [$strOption]")
                    }
                    sb.appendLine()
                }
            }
            return sb.toString()
        }

        private fun resolveOptionString(rlvCmd: RlvCommand): String {
            System.err.println("RlvFloaterBehaviours: resolveOptionString not yet implemented")
            return ""
        }
    }
}

// ============================================================================
// RlvFloaterLocks
//

class RlvFloaterLocks {

    private var commandSlot: (() -> Unit)? = null

    fun onOpen(sdKey: Any) {
        commandSlot = RlvHandler.instance.setCommandCallback { cmd, ret -> onRlvCommand(cmd, ret) }
        refreshAll()
    }

    fun onClose(quitting: Boolean) {
        commandSlot = null
    }

    open fun postBuild(): Boolean {
        System.err.println("RlvFloaterLocks: postBuild not yet implemented")
        return false
    }

    private fun onRlvCommand(rlvCmd: RlvCommand, ret: ERlvCmdRet) {
        if (ret == ERlvCmdRet.RLV_RET_SUCCESS &&
            (rlvCmd.paramType == ERlvParamType.RLV_TYPE_ADD || rlvCmd.paramType == ERlvParamType.RLV_TYPE_REMOVE)
        ) {
            when (rlvCmd.behaviourType) {
                ERlvBehaviour.RLV_BHVR_DETACH,
                ERlvBehaviour.RLV_BHVR_ADDATTACH,
                ERlvBehaviour.RLV_BHVR_REMATTACH,
                ERlvBehaviour.RLV_BHVR_ADDOUTFIT,
                ERlvBehaviour.RLV_BHVR_REMOUTFIT -> refreshAll()
                else -> Unit
            }
        }
    }

    private fun refreshAll() {
        System.err.println("RlvFloaterLocks: refreshAll not yet implemented")
    }
}

// ============================================================================
// RlvFloaterStrings
//

class RlvFloaterStrings {

    private var dirty: Boolean = false
    private var currentString: String = ""
    private var stringList: Any? = null       // placeholder for combo-box widget reference
    private var stringsInfo: Map<String, Any> = emptyMap()
    private var customStrings: Map<String, Any> = emptyMap()
    private var pendingStrings: MutableMap<String, String> = mutableMapOf()

    open fun postBuild(): Boolean {
        System.err.println("RlvFloaterStrings: postBuild not yet implemented")
        return false
    }

    fun onClose(quitting: Boolean) {
        checkDirty(false)
        if (dirty) {
            System.err.println("RlvFloaterStrings: onClose dirty-save not yet implemented")
        }
    }

    private fun onStringRevertDefault() {
        if (currentString.isNotEmpty()) {
            pendingStrings[currentString] = ""
            dirty = true
        }
        refresh()
    }

    private fun checkDirty(doRefresh: Boolean) {
        System.err.println("RlvFloaterStrings: checkDirty not yet implemented")
    }

    private fun refresh() {
        System.err.println("RlvFloaterStrings: refresh not yet implemented")
    }
}

// ============================================================================
// RlvFloaterConsole
//

class RlvFloaterConsole {

    private var outputText: Any? = null     // placeholder for output text editor widget
    private var inputPanel: Any? = null     // placeholder for layout panel widget
    private var inputEdit: Any? = null      // placeholder for chat entry widget
    private var inputEditPad: Int = 0

    open fun postBuild(): Boolean {
        System.err.println("RlvFloaterConsole: postBuild not yet implemented")
        return false
    }

    fun onClose(quitting: Boolean) {
        System.err.println("RlvFloaterConsole: onClose not yet implemented")
    }

    private fun addCommandReply(command: String, reply: String) {
        System.err.println("RlvFloaterConsole: addCommandReply not yet implemented")
    }

    private fun onInput(ctrl: Any, param: Any) {
        System.err.println("RlvFloaterConsole: onInput not yet implemented")
    }

    private fun reshapeLayoutPanel() {
        System.err.println("RlvFloaterConsole: reshapeLayoutPanel not yet implemented")
    }
}
