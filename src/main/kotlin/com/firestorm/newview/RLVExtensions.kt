package com.firestorm.newview

import java.util.UUID

// ============================================================================
// RlvExtGetSet — implements @get_XXX:<option>=<channel> and
// @set_XXX:<option>=force for a controlled whitelist of debug settings.
// ============================================================================

class RlvExtGetSet : RlvExtCommandHandler {

    companion object {
        const val DBG_READ    = 0x01
        const val DBG_WRITE   = 0x02
        const val DBG_PERSIST = 0x04
        const val DBG_PSEUDO  = 0x08

        // Allowed debug settings with their access flags (Short ~ S16)
        val dbgAllowed: MutableMap<String, Short> = mutableMapOf()

        // Pseudo-debug overrides that live only in memory (no saved-settings entry)
        val pseudoDebug: MutableMap<String, String> = mutableMapOf()

        // Case-insensitive lookup: normalises strSetting in-place (by reference
        // semantics via a wrapper) and returns the canonical key and flags.
        fun findDebugSetting(strSettingRef: SettingRef, flags: FlagsRef): Boolean {
            val lower = strSettingRef.value.lowercase()
            for ((key, value) in dbgAllowed) {
                if (key.lowercase() == lower) {
                    strSettingRef.value = key
                    flags.value = value
                    return true
                }
            }
            return false
        }

        fun getDebugSettingFlags(strSetting: String): Short =
            dbgAllowed[strSetting] ?: 0

        // Initialise the allowed-settings whitelist exactly once (mirrors the C++
        // static initialisation guard on m_DbgAllowed.size()).
        private var initialized = false

        fun initAllowedSettings() {
            if (initialized) return
            initialized = true

            dbgAllowed["AvatarSex"]             = (DBG_READ or DBG_WRITE or DBG_PSEUDO).toShort()
            dbgAllowed["AspectRatio"]            = (DBG_READ or DBG_PSEUDO).toShort()
            dbgAllowed["RenderResolutionDivisor"]= (DBG_READ or DBG_WRITE).toShort()
            dbgAllowed[RlvSettingNames.FORBID_GIVE_TO_RLV] = DBG_READ.toShort()
            dbgAllowed[RlvSettingNames.NO_SET_ENV]         = DBG_READ.toShort()
            dbgAllowed["WindLightUseAtmosShaders"]          = DBG_READ.toShort()

            // Mark persisted settings so writes can honour persistence policy.
            System.err.println("RlvExtGetSet: initAllowedSettings not yet implemented")
        }
    }

    // Mutable reference wrappers (replace C++ in/out reference parameters)
    class SettingRef(var value: String)
    class FlagsRef(var value: Short)

    init {
        initAllowedSettings()
    }

    // -------------------------------------------------------------------------
    // RlvExtCommandHandler interface
    // -------------------------------------------------------------------------

    override fun onForceCommand(rlvCmd: RlvCommand, cmdRetRef: CmdRetRef): Boolean =
        processCommand(rlvCmd, cmdRetRef)

    override fun onReplyCommand(rlvCmd: RlvCommand, cmdRetRef: CmdRetRef): Boolean =
        processCommand(rlvCmd, cmdRetRef)

    // -------------------------------------------------------------------------
    // Command dispatch
    // -------------------------------------------------------------------------

    private fun processCommand(rlvCmd: RlvCommand, eRet: CmdRetRef): Boolean {
        val behaviour  = rlvCmd.getBehaviour()
        val idxSetting = behaviour.indexOf('_')

        if (behaviour.length >= 6 && idxSetting != -1 && behaviour.length > idxSetting + 1) {
            val strSetting = behaviour.substring(idxSetting + 1)
            val prefix     = behaviour.substring(0, idxSetting)   // e.g. "getdebug" / "setdebug"
            val strGetSet  = prefix.substring(0, 3)               // "get" / "set"
            val strType    = prefix.substring(3)                   // "debug" / …

            if (strType == "debug") {
                if (strGetSet == "get" && rlvCmd.getParamType() == ERlvParamType.RLV_TYPE_REPLY) {
                    RlvUtil.sendChatReply(rlvCmd.getParam(), onGetDebug(strSetting))
                    eRet.value = ERlvCmdRet.RLV_RET_SUCCESS
                    return true
                } else if (strGetSet == "set" && rlvCmd.getParamType() == ERlvParamType.RLV_TYPE_FORCE) {
                    eRet.value = if (!gRlvHandler.hasBehaviourExcept(ERlvBehaviour.RLV_BHVR_SETDEBUG, rlvCmd.getObjectID())) {
                        onSetDebug(strSetting, rlvCmd.getOption())
                    } else {
                        ERlvCmdRet.RLV_RET_FAILED_LOCK
                    }
                    return true
                }
            }
            return false
        }

        if (rlvCmd.getBehaviour() == "setrot") {
            val option = rlvCmd.getOption()
            val angle  = option.toFloatOrNull()
            if (angle != null) {
                val adjusted = RLV_SETROT_OFFSET - angle
                // no-op
                eRet.value = ERlvCmdRet.RLV_RET_SUCCESS
            } else {
                eRet.value = ERlvCmdRet.RLV_RET_FAILED_OPTION
            }
            return true
        }

        return false
    }

    // -------------------------------------------------------------------------
    // Get / set helpers
    // -------------------------------------------------------------------------

    private fun onGetDebug(strSetting: String): String {
        val ref   = SettingRef(strSetting)
        val flags = FlagsRef(0)
        if (findDebugSetting(ref, flags) && (flags.value.toInt() and DBG_READ) == DBG_READ) {
            return if ((flags.value.toInt() and DBG_PSEUDO) == 0) {
                ""
            } else {
                onGetPseudoDebug(ref.value)
            }
        }
        return ""
    }

    private fun onGetPseudoDebug(strSetting: String): String {
        if (strSetting == "AvatarSex") {
            val override = pseudoDebug[strSetting]
            if (override != null) return override
            return ""
        }
        if (strSetting == "AspectRatio") {
            return ""
        }
        return ""
    }

    private fun onSetDebug(strSetting: String, strValue: String): ERlvCmdRet {
        val ref   = SettingRef(strSetting)
        val flags = FlagsRef(0)
        if (!findDebugSetting(ref, flags) || (flags.value.toInt() and DBG_WRITE) != DBG_WRITE) {
            return ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
        }
        return if ((flags.value.toInt() and DBG_PSEUDO) == 0) {
            ERlvCmdRet.RLV_RET_FAILED_UNKNOWN
        } else {
            onSetPseudoDebug(ref.value, strValue)
        }
    }

    private fun onSetPseudoDebug(strSetting: String, strValue: String): ERlvCmdRet {
        if (strSetting == "AvatarSex") {
            val boolVal = strValue.toBooleanStrictOrNull()
                ?: strValue.toIntOrNull()?.let { it != 0 }
                ?: return ERlvCmdRet.RLV_RET_FAILED_OPTION
            pseudoDebug[strSetting] = if (boolVal) "1" else "0"
            return ERlvCmdRet.RLV_RET_SUCCESS
        }
        return ERlvCmdRet.RLV_RET_FAILED_OPTION
    }
}

// ============================================================================
// RlvExtCommandHandler — interface that extension objects must implement
// ============================================================================

interface RlvExtCommandHandler {
    fun onForceCommand(rlvCmd: RlvCommand, cmdRetRef: CmdRetRef): Boolean
    fun onReplyCommand(rlvCmd: RlvCommand, cmdRetRef: CmdRetRef): Boolean
}

// Mutable return-value wrapper (replaces C++ ERlvCmdRet& out parameter)
class CmdRetRef(var value: ERlvCmdRet = ERlvCmdRet.RLV_RET_FAILED_UNKNOWN)
