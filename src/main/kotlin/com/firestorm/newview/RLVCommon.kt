package com.firestorm.newview

import java.util.UUID

// ============================================================================
// RlvSettings
// ============================================================================

object RlvSettings {
    private var canOoc: Boolean = true
    private var experienceMinMaturity: UByte = 0u
    private var legacyNaming: Boolean = true
    private var noSetEnv: Boolean = false
    private var tempAttach: Boolean = true
    private val blockedExperiences: MutableList<String> = mutableListOf()
    private val compatItemCreators: MutableList<UUID> = mutableListOf()
    private val compatItemNames: MutableList<String> = mutableListOf()

    fun getDebug(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.Debug}'")
    fun getCanOOC(): Boolean = canOoc
    fun getForbidGiveToRLV(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.ForbidGiveToRlv}'")
    fun getNoSetEnv(): Boolean = noSetEnv

    fun getWearAddPrefix(): String = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.WearAddPrefix}'")
    fun getWearReplacePrefix(): String = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.WearReplacePrefix}'")

    fun getDebugHideUnsetDup(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.DebugHideUnsetDup}'")
    fun getEnableIMQuery(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.EnableIMQuery}'")
    fun getEnableLegacyNaming(): Boolean = legacyNaming
    fun getEnableSharedWear(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.EnableSharedWear}'")
    fun getEnableTemporaryAttachments(): Boolean = tempAttach
    fun getHideLockedLayers(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.HideLockedLayer}'")
    fun getHideLockedAttach(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.HideLockedAttach}'")
    fun getHideLockedInventory(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.HideLockedInventory}'")
    fun getSharedInvAutoRename(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.SharedInvAutoRename}'")
    fun getSplitRedirectChat(): Boolean = TODO("APR: use JVM equivalent - read setting '${RlvSettingNames.SplitRedirectChat}'")
    fun getLoginLastLocation(): Boolean = TODO("APR: use JVM equivalent - read per-account setting '${RlvSettingNames.LoginLastLocation}'")

    fun updateLoginLastLocation() {
        TODO("APR: use JVM equivalent - persist per-account login last location based on TPLOC/canStand state")
    }

    fun initCompatibilityMode(compatList: String) {
        compatItemCreators.clear()
        compatItemNames.clear()

        val combined = "$compatList;" + TODO("APR: use JVM equivalent - read setting 'RLVaCompatibilityModeList'") as String
        for (entry in combined.split(";").filter { it.isNotEmpty() }) {
            when {
                entry.startsWith("creator:") -> {
                    val uuidStr = entry.substring(8)
                    if (uuidStr.length == 36) {
                        runCatching { UUID.fromString(uuidStr) }.getOrNull()
                            ?.takeIf { it !in compatItemCreators }
                            ?.let { compatItemCreators.add(it) }
                    }
                }
                entry.startsWith("name:") -> {
                    if (entry.length > 5) compatItemNames.add(entry.substring(5))
                }
            }
        }
    }

    fun isCompatibilityModeObject(idRlvObject: UUID): Boolean {
        if (idRlvObject == UUID(0, 0)) return false
        TODO("APR: use JVM equivalent - look up object in world, check attachment item creator/name against compat lists")
    }

    fun isAllowedExperience(idExperience: UUID, nMaturity: UByte): Boolean {
        return getEnableTemporaryAttachments() &&
            experienceMinMaturity.toInt() != 0 &&
            experienceMinMaturity <= nMaturity &&
            !blockedExperiences.contains(idExperience.toString())
    }

    fun initClass() {
        TODO("APR: use JVM equivalent - wire up settings signals, read initial values")
    }

    fun onChangedSettingMain(enabled: Boolean) {
        TODO("APR: use JVM equivalent - show notification about RLVa toggle requiring restart or being instant")
    }
}

// ============================================================================
// RlvStrings
// ============================================================================

object RlvStrings {
    private val anonyms: MutableList<String> = mutableListOf()
    private val stringMap: MutableMap<String, MutableList<String>> = mutableMapOf()
    private var stringMapPath: String = ""

    fun getStringMapPath(): String = stringMapPath

    fun initClass() {
        TODO("APR: use JVM equivalent - find and load rlva_strings.xml from skin/user paths")
    }

    fun loadFromFile(filePath: String, userOverride: Boolean) {
        TODO("APR: use JVM equivalent - parse XML file into stringMap and anonyms")
    }

    fun saveToFile(filePath: String) {
        TODO("APR: use JVM equivalent - serialize custom overrides back to XML")
    }

    fun getAnonym(avName: String): String {
        if (!RlvActions.isRlvEnabled() || anonyms.isEmpty()) return "Unknown"
        val hash = avName.sumOf { it.code }
        return anonyms[hash % anonyms.size]
    }

    fun getString(stringName: String): String {
        return stringMap[stringName]?.lastOrNull() ?: "(Missing RLVa string)"
    }

    fun getStringFromReturnCode(ret: ERlvCmdRet): String? = when (ret) {
        ERlvCmdRet.RLV_RET_SUCCESS_UNSET -> "unset"
        ERlvCmdRet.RLV_RET_SUCCESS_DUPLICATE -> "duplicate"
        ERlvCmdRet.RLV_RET_SUCCESS_DELAYED -> "delayed"
        ERlvCmdRet.RLV_RET_SUCCESS_DEPRECATED -> "deprecated"
        ERlvCmdRet.RLV_RET_FAILED_SYNTAX -> "thingy error"
        ERlvCmdRet.RLV_RET_FAILED_OPTION -> "invalid option"
        ERlvCmdRet.RLV_RET_FAILED_PARAM -> "invalid param"
        ERlvCmdRet.RLV_RET_FAILED_LOCK -> "locked command"
        ERlvCmdRet.RLV_RET_FAILED_DISABLED -> "disabled command"
        ERlvCmdRet.RLV_RET_FAILED_UNKNOWN -> "unknown command"
        ERlvCmdRet.RLV_RET_FAILED_NOSHAREDROOT -> "missing #RLV"
        ERlvCmdRet.RLV_RET_FAILED_DEPRECATED -> "deprecated and disabled"
        ERlvCmdRet.RLV_RET_FAILED_NOBEHAVIOUR -> "no active behaviours"
        ERlvCmdRet.RLV_RET_FAILED_UNHELDBEHAVIOUR -> "base behaviour not held"
        ERlvCmdRet.RLV_RET_FAILED_BLOCKED -> "blocked object"
        ERlvCmdRet.RLV_RET_FAILED_THROTTLED -> "throttled"
        ERlvCmdRet.RLV_RET_RETAINED,
        ERlvCmdRet.RLV_RET_SUCCESS,
        ERlvCmdRet.RLV_RET_FAILED -> null
        else -> null
    }

    fun getVersion(idRlvObject: UUID, legacy: Boolean = false): String {
        val compatMode = RlvSettings.isCompatibilityModeObject(idRlvObject)
        val prefix = if (!legacy) "RestrainedLove" else "RestrainedLife"
        val major = if (!compatMode) RLV_VERSION_MAJOR else RLV_VERSION_MAJOR_COMPAT
        val minor = if (!compatMode) RLV_VERSION_MINOR else RLV_VERSION_MINOR_COMPAT
        val patch = if (!compatMode) RLV_VERSION_PATCH else RLV_VERSION_PATCH_COMPAT
        return "$prefix viewer v$major.$minor.$patch (RLVa $RLVa_VERSION_MAJOR.$RLVa_VERSION_MINOR.$RLVa_VERSION_PATCH)"
    }

    fun getVersionAbout(): String =
        "RLV v$RLV_VERSION_MAJOR.$RLV_VERSION_MINOR.$RLV_VERSION_PATCH / RLVa v$RLVa_VERSION_MAJOR.$RLVa_VERSION_MINOR.$RLVa_VERSION_PATCH.${TODO("APR: use JVM equivalent - viewer build number")}"

    fun getVersionNum(idRlvObject: UUID): String {
        val compatMode = RlvSettings.isCompatibilityModeObject(idRlvObject)
        val major = if (!compatMode) RLV_VERSION_MAJOR else RLV_VERSION_MAJOR_COMPAT
        val minor = if (!compatMode) RLV_VERSION_MINOR else RLV_VERSION_MINOR_COMPAT
        val patch = if (!compatMode) RLV_VERSION_PATCH else RLV_VERSION_PATCH_COMPAT
        val build = if (!compatMode) RLV_VERSION_BUILD else RLV_VERSION_BUILD_COMPAT
        return "%d%02d%02d%02d".format(major, minor, patch, build)
    }

    fun getVersionImplNum(): String =
        "%d%02d%02d%02d".format(RLVa_VERSION_MAJOR, RLVa_VERSION_MINOR, RLVa_VERSION_PATCH, RLVa_IMPL_ID)

    fun hasString(stringName: String, checkCustom: Boolean = false): Boolean {
        val entry = stringMap[stringName] ?: return false
        return !checkCustom || entry.isNotEmpty()
    }

    fun setCustomString(stringName: String, stringValue: String) {
        if (!hasString(stringName)) return
        val list = stringMap[stringName] ?: return
        while (list.size > 1) list.removeAt(list.lastIndex)
        if (stringValue.isNotEmpty()) list.add(stringValue)
    }
}

// ============================================================================
// RlvUtil
// ============================================================================

object RlvUtil {
    var isForceTp: Boolean = false
        private set

    fun isEmote(text: String): Boolean =
        text.length > 4 && (text.startsWith("/me ") || text.startsWith("/me'"))

    fun isNearbyAgent(idAgent: UUID): Boolean {
        TODO("APR: use JVM equivalent - query nearby avatar list from world")
    }

    fun isNearbyRegion(region: String): Boolean {
        TODO("APR: use JVM equivalent - query region list from world")
    }

    fun filterLocation(text: StringBuilder) {
        TODO("APR: use JVM equivalent - replace region/parcel names in text with hidden placeholders")
    }

    fun filterNames(text: StringBuilder, filterLegacy: Boolean = true, clearMatches: Boolean = false) {
        TODO("APR: use JVM equivalent - replace nearby avatar names with anonymised values; then call filterMentions")
    }

    fun filterMentions(text: StringBuilder) {
        if (!RlvActions.isRlvEnabled()) return
        if (RlvActions.canShowName(RlvActions.EShowNamesContext.SNC_DEFAULT)) return
        TODO("APR: use JVM equivalent - replace agent mention URIs for hidden names with anonymised @name tokens")
    }

    fun filterScriptQuestions(nQuestions: Int, payload: MutableMap<String, Any>): Int {
        TODO("APR: use JVM equivalent - strip ATTACH/TELEPORT permission bits blocked by RLV and annotate payload")
    }

    fun forceTp(posDest: Triple<Double, Double, Double>) {
        isForceTp = true
        TODO("APR: use JVM equivalent - invoke agent teleport to posDest ignoring restrictions")
        isForceTp = false
    }

    fun notifyBlocked(notification: String, args: Map<String, String> = emptyMap(), logToChat: Boolean = false) {
        val msg = buildString {
            var s = RlvStrings.getString(notification)
            for ((k, v) in args) s = s.replace(k, v)
            append(s)
        }
        TODO("APR: use JVM equivalent - show system notification '$msg' (logToChat=$logToChat)")
    }

    fun notifyBlockedGeneric() = notifyBlocked(RlvStringKeys.Blocked.Generic)

    fun notifyFailedAssertion(assertion: String, file: String, line: Int) {
        TODO("APR: use JVM equivalent - show one-shot assertion failure notification for '$assertion' at $file:$line")
    }

    fun sendBusyMessage(idTo: UUID, msg: String, idSession: UUID = UUID(0, 0)) {
        TODO("APR: use JVM equivalent - send IM_DO_NOT_DISTURB_AUTO_RESPONSE instant message")
    }

    fun isValidReplyChannel(nChannel: Int, loopback: Boolean = false): Boolean =
        nChannel > (if (!loopback) 0 else -1)

    fun sendChatReply(nChannel: Int, text: String): Boolean {
        if (!isValidReplyChannel(nChannel)) return false
        TODO("APR: use JVM equivalent - send CHAT_TYPE_SHOUT on channel nChannel")
    }

    fun sendChatReply(channel: String, text: String): Boolean {
        val n = channel.toIntOrNull() ?: return false
        return sendChatReply(n, text)
    }

    fun sendChatReplySplit(nChannel: Int, text: String, splitChar: Char = ' '): Boolean {
        TODO("APR: use JVM equivalent - split text at splitChar honoring max message length, then sendChatReply for each chunk")
    }

    fun sendIMMessage(idRecipient: UUID, msg: String, splitChar: Char) {
        TODO("APR: use JVM equivalent - split msg and send IM_NOTHING_SPECIAL instant messages to idRecipient")
    }
}

// ============================================================================
// RlvExtCommandHandler — extensibility base
// ============================================================================

abstract class RlvExtCommandHandler {
    open fun onAddRemCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRet): Boolean = false
    open fun onClearCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRet): Boolean = false
    open fun onReplyCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRet): Boolean = false
    open fun onForceCommand(rlvCmd: RlvCommand, cmdRet: ERlvCmdRet): Boolean = false
}

// ============================================================================
// Menu enablers (UI glue — viewer-specific)
// ============================================================================

fun rlvMenuCanShowName(): Boolean {
    TODO("APR: use JVM equivalent - check canShowName for the primary selected avatar")
}

fun rlvMenuEnableIfNot(param: String): Boolean {
    if (!RlvHandler.isEnabled()) return true
    val bhvr = RlvBehaviourDictionary.getBehaviourFromString(param, ERlvParamType.RLV_TYPE_ADD)
    return if (bhvr != ERlvBehaviour.RLV_BHVR_UNKNOWN) !RlvHandler.instance.hasBehaviour(bhvr) else true
}

// ============================================================================
// Selection functors
// ============================================================================

fun rlvCanDeleteOrReturn(): Boolean {
    TODO("APR: use JVM equivalent - evaluate canDeleteOrReturn over the current selection")
}

fun rlvCanDeleteOrReturn(obj: Any?): Boolean {
    TODO("APR: use JVM equivalent - check edit/rez/unsit restrictions for the given viewer object")
}

// ============================================================================
// Predicate helpers
// ============================================================================

fun rlvPredCanWearItem(item: Any?, wearMask: ERlvWearMask): Boolean {
    TODO("APR: use JVM equivalent - check wearable/attachment lock state for item against wearMask")
}

fun rlvPredCanNotWearItem(item: Any?, wearMask: ERlvWearMask): Boolean = !rlvPredCanWearItem(item, wearMask)

fun rlvPredCanRemoveItem(idItem: UUID): Boolean {
    TODO("APR: use JVM equivalent - look up inventory item by UUID and check remove locks")
}

fun rlvPredCanRemoveItem(item: Any?): Boolean {
    TODO("APR: use JVM equivalent - check wearable/attachment remove locks for item")
}

fun rlvPredCanNotRemoveItem(item: Any?): Boolean = !rlvPredCanRemoveItem(item)
