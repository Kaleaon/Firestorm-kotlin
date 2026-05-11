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
    TODO("APR: use JVM equivalent — resolve item type/wearable type label")
}

private fun rlvGetItemType(item: ViewerInventoryItem?): String {
    TODO("APR: use JVM equivalent — resolve asset type string")
}

private fun rlvGetItemNameFromObjID(idObj: UUID, includeAttachPt: Boolean = true): String {
    TODO("APR: use JVM equivalent — look up avatar name / attachment info from object list")
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
    TODO("APR: use JVM equivalent — resolve variant lock source to human-readable string")
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
        TODO("APR: use JVM equivalent — wire copy-to-clipboard button commit callback")
    }

    private fun onAvatarNameLookup(idAgent: UUID, avName: Any) {
        pendingLookup.remove(idAgent)
        if (isVisible()) refreshAll()
    }

    private fun isVisible(): Boolean {
        TODO("APR: use JVM equivalent — query floater visibility state")
    }

    private fun onBtnCopyToClipboard() {
        TODO("APR: use JVM equivalent — copy formatted behaviour string to clipboard")
    }

    private fun onCommand(rlvCmd: RlvCommand, ret: ERlvCmdRet) {
        if (rlvCmd.paramType == ERlvParamType.RLV_TYPE_ADD || rlvCmd.paramType == ERlvParamType.RLV_TYPE_REMOVE) {
            refreshAll()
        }
    }

    private fun refreshAll() {
        TODO("APR: use JVM equivalent — populate behaviour/exception/modifier list UI controls")
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
            TODO("APR: use JVM equivalent — resolve UUID option to avatar/object/group name")
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
        TODO("APR: use JVM equivalent — wire refresh button commit callback")
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
        TODO("APR: use JVM equivalent — populate lock list UI control from attachment/wearable/folder lock state")
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
        TODO("APR: use JVM equivalent — read strings XML, populate combo box, wire callbacks")
    }

    fun onClose(quitting: Boolean) {
        checkDirty(false)
        if (dirty) {
            TODO("APR: use JVM equivalent — save pending string overrides to user-settings XML file and notify user of relog requirement")
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
        TODO("APR: use JVM equivalent — read current text editor value and record pending change if modified")
    }

    private fun refresh() {
        TODO("APR: use JVM equivalent — update description and value text editors for the selected string")
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
        TODO("APR: use JVM equivalent — wire input commit/expand callbacks, focus input, initialise output with prompt")
    }

    fun onClose(quitting: Boolean) {
        TODO("APR: use JVM equivalent — clear RLV modifiers for agent and process 'clear' command")
    }

    private fun addCommandReply(command: String, reply: String) {
        TODO("APR: use JVM equivalent — append '$command: $reply' to output text editor")
    }

    private fun onInput(ctrl: Any, param: Any) {
        TODO("APR: use JVM equivalent — read input, execute RLV commands, append results and new prompt to output")
    }

    private fun reshapeLayoutPanel() {
        TODO("APR: use JVM equivalent — resize input panel to fit expanded chat entry widget")
    }
}
