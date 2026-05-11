package com.firestorm.newview

import java.util.UUID

// ============================================================================
// RlvBehaviourInfo — generic behaviour descriptor
// ============================================================================

open class RlvBehaviourInfo(
    val behaviour: String,
    val behaviourType: ERlvBehaviour,
    val paramTypeMask: Int,
    private var behaviourFlags: Int = 0
) {
    companion object {
        const val BHVR_STRICT       = 0x0001
        const val BHVR_SYNONYM      = 0x0002
        const val BHVR_EXTENDED     = 0x0004
        const val BHVR_EXPERIMENTAL = 0x0008
        const val BHVR_BLOCKED      = 0x0010
        const val BHVR_DEPRECATED   = 0x0020
        const val BHVR_GENERAL_MASK = 0x0FFF

        const val FORCEWEAR_WEAR_REPLACE   = 0x0001 shl 16
        const val FORCEWEAR_WEAR_ADD       = 0x0002 shl 16
        const val FORCEWEAR_WEAR_REMOVE    = 0x0004 shl 16
        const val FORCEWEAR_NODE           = 0x0010 shl 16
        const val FORCEWEAR_SUBTREE        = 0x0020 shl 16
        const val FORCEWEAR_CONTEXT_NONE   = 0x0100 shl 16
        const val FORCEWEAR_CONTEXT_OBJECT = 0x0200 shl 16
        const val FORCEWEAR_MASK           = 0xFFFF shl 16
    }

    // Maps sub-command suffix -> (ERlvLocalBhvrModifier, KClass, optional handler)
    val modifiers: MutableMap<String, Triple<ERlvLocalBhvrModifier, Class<*>, ((UUID, Any?) -> ERlvCmdRet)?>> = mutableMapOf()

    fun addModifier(
        eBhvrMod: ERlvLocalBhvrModifier,
        valueType: Class<*>,
        strBhvrMod: String,
        handler: ((UUID, Any?) -> ERlvCmdRet)? = null
    ) {
        modifiers[strBhvrMod] = Triple(eBhvrMod, valueType, handler)
    }

    fun getBehaviourFlags(): Int = behaviourFlags
    fun hasStrict(): Boolean = (behaviourFlags and BHVR_STRICT) != 0
    fun isBlocked(): Boolean = (behaviourFlags and BHVR_BLOCKED) != 0
    fun isExperimental(): Boolean = (behaviourFlags and BHVR_EXPERIMENTAL) != 0
    fun isExtended(): Boolean = (behaviourFlags and BHVR_EXTENDED) != 0
    fun isSynonym(): Boolean = (behaviourFlags and BHVR_SYNONYM) != 0

    fun lookupBehaviourModifier(strBhvrMod: String): ERlvLocalBhvrModifier =
        modifiers[strBhvrMod]?.first ?: ERlvLocalBhvrModifier.Unknown

    fun toggleBehaviourFlag(flag: Int, enable: Boolean) {
        behaviourFlags = if (enable) behaviourFlags or flag else behaviourFlags and flag.inv()
    }

    open fun processCommand(rlvCmd: RlvCommand): ERlvCmdRet = ERlvCmdRet.RLV_RET_NO_PROCESSOR

    open fun processModifier(rlvCmd: RlvCommand): ERlvCmdRet {
        TODO("APR: use JVM equivalent - dispatch modifier sub-command to registered handler")
    }
}

// ============================================================================
// RlvBehaviourModifier — stores a sorted list of modifier values
// ============================================================================

class RlvBehaviourModifier(
    val name: String,
    private val defaultValue: Any,
    val addDefaultOnEmpty: Boolean,
    private val comparator: Comparator<Any>? = null
) {
    // Each entry: (value, owning-object UUID, associated behaviour)
    private val values: MutableList<Triple<Any, UUID, ERlvBehaviour>> = mutableListOf()
    val changeSignal: MutableList<(Any) -> Unit> = mutableListOf()
    private var primaryObject: UUID = UUID(0, 0)

    fun getDefaultValue(): Any = defaultValue

    fun hasValue(): Boolean = values.isNotEmpty()

    fun hasValue(idRlvObj: UUID): Boolean = values.any { it.second == idRlvObj }

    fun getValue(): Any = if (hasValue()) values.first().first else defaultValue

    fun getPrimaryObject(): UUID = primaryObject

    fun setPrimaryObject(idPrimaryObject: UUID) {
        primaryObject = idPrimaryObject
    }

    fun addValue(modValue: Any, idRlvObj: UUID, eBhvr: ERlvBehaviour = ERlvBehaviour.RLV_BHVR_UNKNOWN): Boolean {
        val entry = Triple(modValue, idRlvObj, eBhvr)
        if (comparator != null) {
            val idx = values.indexOfFirst { comparator.compare(it.first, modValue) > 0 }
            if (idx >= 0) values.add(idx, entry) else values.add(entry)
        } else {
            values.add(entry)
        }
        onValueChange()
        for (listener in changeSignal) listener(getValue())
        return true
    }

    fun removeValue(modValue: Any, idRlvObj: UUID, eBhvr: ERlvBehaviour = ERlvBehaviour.RLV_BHVR_UNKNOWN) {
        val idx = values.indexOfFirst { it.first == modValue && it.second == idRlvObj && it.third == eBhvr }
        if (idx >= 0) {
            values.removeAt(idx)
            onValueChange()
            for (listener in changeSignal) listener(getValue())
        }
    }

    fun clearValues(idRlvObj: UUID) {
        val removed = values.removeIf { it.second == idRlvObj }
        if (removed) {
            onValueChange()
            for (listener in changeSignal) listener(getValue())
        }
    }

    fun setValue(modValue: Any, idRlvObj: UUID) {
        clearValues(idRlvObj)
        addValue(modValue, idRlvObj)
    }

    protected open fun onValueChange() {}

    companion object {
        fun convertOptionValue(optionValue: String, modType: Class<*>): Any? {
            return when (modType) {
                Float::class.java  -> optionValue.toFloatOrNull()
                Int::class.java    -> optionValue.toIntOrNull()
                Boolean::class.java -> optionValue.toBooleanStrictOrNull()
                UUID::class.java   -> runCatching { UUID.fromString(optionValue) }.getOrNull()
                String::class.java -> optionValue
                else               -> null
            }
        }
    }
}

// ============================================================================
// RlvBehaviourDictionary — singleton lookup table for all RLV commands
// ============================================================================

object RlvBehaviourDictionary {

    private val bhvrInfoList: MutableList<RlvBehaviourInfo> = mutableListOf()
    // (behaviourString, paramTypeMask) -> RlvBehaviourInfo
    private val string2InfoMap: MutableMap<Pair<String, Int>, RlvBehaviourInfo> = mutableMapOf()
    // ERlvBehaviour -> list of RlvBehaviourInfo (multiple entries allowed)
    private val bhvr2InfoMap: MutableMap<ERlvBehaviour, MutableList<RlvBehaviourInfo>> = mutableMapOf()
    private val bhvr2ModifierMap: MutableMap<ERlvBehaviour, ERlvBehaviourModifier> = mutableMapOf()
    private val behaviourModifiers: Array<RlvBehaviourModifier?> = arrayOfNulls(ERlvBehaviourModifier.RLV_MODIFIER_COUNT.ordinal)

    fun addEntry(entry: RlvBehaviourInfo) {
        bhvrInfoList.add(entry)
        string2InfoMap[Pair(entry.behaviour, entry.paramTypeMask and (ERlvParamType.RLV_TYPE_ADDREM or ERlvParamType.RLV_TYPE_FORCE.value or ERlvParamType.RLV_TYPE_REPLY.value))] = entry
        if ((entry.paramTypeMask and ERlvParamType.RLV_TYPE_ADDREM) != 0 && !entry.isSynonym()) {
            bhvr2InfoMap.getOrPut(entry.behaviourType) { mutableListOf() }.add(entry)
        }
    }

    fun addModifier(eBhvr: ERlvBehaviour, eModifier: ERlvBehaviourModifier, modifier: RlvBehaviourModifier) {
        if (eModifier.ordinal < ERlvBehaviourModifier.RLV_MODIFIER_COUNT.ordinal) {
            behaviourModifiers[eModifier.ordinal] = modifier
            bhvr2ModifierMap[eBhvr] = eModifier
        }
    }

    fun addModifier(bhvrEntry: RlvBehaviourInfo, eModifier: ERlvBehaviourModifier, modifier: RlvBehaviourModifier) {
        addEntry(bhvrEntry)
        addModifier(bhvrEntry.behaviourType, eModifier, modifier)
    }

    fun clearModifiers(idRlvObj: UUID) {
        for (mod in behaviourModifiers) mod?.clearValues(idRlvObj)
    }

    fun getModifier(eBhvrMod: ERlvBehaviourModifier): RlvBehaviourModifier =
        behaviourModifiers[eBhvrMod.ordinal]
            ?: throw IllegalStateException("Modifier $eBhvrMod is not registered")

    fun getModifierFromBehaviour(eBhvr: ERlvBehaviour): RlvBehaviourModifier? {
        val eModifier = bhvr2ModifierMap[eBhvr] ?: return null
        return behaviourModifiers[eModifier.ordinal]
    }

    fun getBehaviourInfo(eBhvr: ERlvBehaviour, eParamType: ERlvParamType): RlvBehaviourInfo? {
        val list = bhvr2InfoMap[eBhvr] ?: return null
        val matches = list.filter { it.paramTypeMask == eParamType.value }
        return if (matches.size == 1) matches[0] else null
    }

    fun getBehaviourInfo(strBhvr: String, eParamType: ERlvParamType, strictOut: BooleanArray? = null, modifierOut: Array<ERlvLocalBhvrModifier>? = null): RlvBehaviourInfo? {
        val lastUnder = strBhvr.lastIndexOf('_')
        val lastPart = if (lastUnder >= 0 && lastUnder < strBhvr.length) strBhvr.substring(lastUnder + 1) else ""
        val fStrict = lastPart == "sec"
        if (strictOut != null && strictOut.isNotEmpty()) strictOut[0] = fStrict

        val lookupName = if (!fStrict) strBhvr else strBhvr.dropLast(4)
        val effectiveType = if ((eParamType.value and ERlvParamType.RLV_TYPE_ADDREM) != 0) ERlvParamType.RLV_TYPE_ADDREM.value else eParamType.value

        var entry = string2InfoMap[Pair(lookupName, effectiveType)]
        var eBhvrMod = ERlvLocalBhvrModifier.Unknown

        if (entry == null && !fStrict && lastPart.isNotEmpty() && eParamType == ERlvParamType.RLV_TYPE_FORCE) {
            val baseName = strBhvr.substring(0, lastUnder)
            val baseEntry = string2InfoMap[Pair(baseName, ERlvParamType.RLV_TYPE_ADDREM.value)]
            if (baseEntry != null) {
                val mod = baseEntry.lookupBehaviourModifier(lastPart)
                if (mod != ERlvLocalBhvrModifier.Unknown) {
                    entry = baseEntry
                    eBhvrMod = mod
                }
            }
        }

        if (modifierOut != null && modifierOut.isNotEmpty()) modifierOut[0] = eBhvrMod
        return if (entry != null && (!fStrict || entry.hasStrict())) entry else null
    }

    fun getBehaviourFromString(strBhvr: String, eParamType: ERlvParamType, strictOut: BooleanArray? = null): ERlvBehaviour {
        val modOut = arrayOf(ERlvLocalBhvrModifier.Unknown)
        val info = getBehaviourInfo(strBhvr, eParamType, strictOut, modOut)
        return if (info != null && modOut[0] == ERlvLocalBhvrModifier.Unknown) info.behaviourType else ERlvBehaviour.RLV_BHVR_UNKNOWN
    }

    fun getCommands(strMatch: String, eParamType: ERlvParamType): List<String> {
        val result = mutableListOf<String>()
        for (info in bhvrInfoList) {
            if ((info.paramTypeMask and eParamType.value) != 0 || eParamType == ERlvParamType.RLV_TYPE_UNKNOWN) {
                val cmd = info.behaviour
                if (strMatch.isEmpty() || cmd.contains(strMatch)) result.add(cmd)
                if (info.hasStrict()) {
                    val secCmd = "${cmd}_sec"
                    if (strMatch.isEmpty() || secCmd.contains(strMatch)) result.add(secCmd)
                }
            }
        }
        return result
    }

    fun getHasStrict(eBhvr: ERlvBehaviour): Boolean {
        val list = bhvr2InfoMap[eBhvr] ?: return false
        return list.any { it.paramTypeMask == ERlvParamType.RLV_TYPE_ADDREM.value && it.hasStrict() }
    }

    fun toggleBehaviourFlag(strBhvr: String, eParamType: ERlvParamType, flag: Int, enable: Boolean) {
        getBehaviourInfo(strBhvr, eParamType)?.toggleBehaviourFlag(flag, enable)
    }

    fun getBhvrInfoList(): List<RlvBehaviourInfo> = bhvrInfoList
}

// ============================================================================
// RlvCommand — a single parsed RLV command
// ============================================================================

class RlvCommand(
    private val idObj: UUID,
    strCommand: String,
    eParamTypeOverride: ERlvParamType = ERlvParamType.RLV_TYPE_UNKNOWN
) {
    private var valid: Boolean = false
    private var strBehaviour: String = ""
    private var pBhvrInfo: RlvBehaviourInfo? = null
    private var eParamType: ERlvParamType = eParamTypeOverride
    private var eBhvrModifier: ERlvLocalBhvrModifier = ERlvLocalBhvrModifier.Unknown
    private var fStrict: Boolean = false
    private var strOption: String = ""
    private var strParam: String = ""
    private var refCounted: Boolean = false

    init {
        valid = parseCommand(strCommand)
    }

    private fun parseCommand(strCommand: String): Boolean {
        // <behaviour>[:<option>]=<param>
        val eqIdx = strCommand.lastIndexOf('=')
        if (eqIdx < 0) return false

        strParam = strCommand.substring(eqIdx + 1)
        val rest = strCommand.substring(0, eqIdx)

        val colonIdx = rest.indexOf(':')
        strBehaviour = if (colonIdx >= 0) rest.substring(0, colonIdx) else rest
        strOption = if (colonIdx >= 0) rest.substring(colonIdx + 1) else ""

        eParamType = when (strParam) {
            "n", "add" -> ERlvParamType.RLV_TYPE_ADD
            "y", "rem" -> ERlvParamType.RLV_TYPE_REMOVE
            "force"    -> ERlvParamType.RLV_TYPE_FORCE
            "clear"    -> ERlvParamType.RLV_TYPE_CLEAR
            else       -> if (strParam.toIntOrNull() != null) ERlvParamType.RLV_TYPE_REPLY else ERlvParamType.RLV_TYPE_UNKNOWN
        }
        if (eParamType == ERlvParamType.RLV_TYPE_UNKNOWN) return false

        val modOut = arrayOf(ERlvLocalBhvrModifier.Unknown)
        val strictOut = booleanArrayOf(false)
        pBhvrInfo = RlvBehaviourDictionary.getBehaviourInfo(strBehaviour, eParamType, strictOut, modOut)
        fStrict = strictOut[0]
        eBhvrModifier = modOut[0]

        return true
    }

    fun asString(): String = when {
        eParamType != ERlvParamType.RLV_TYPE_CLEAR ->
            if (strOption.isNotEmpty()) "$strBehaviour:$strOption" else strBehaviour
        else ->
            if (strParam.isNotEmpty()) "$strBehaviour:$strParam" else strBehaviour
    }

    fun getBehaviour(): String = strBehaviour
    fun getBehaviourInfo(): RlvBehaviourInfo? = pBhvrInfo
    fun getBehaviourType(): ERlvBehaviour = pBhvrInfo?.behaviourType ?: ERlvBehaviour.RLV_BHVR_UNKNOWN
    fun getBehaviourFlags(): Int = pBhvrInfo?.getBehaviourFlags() ?: 0
    fun getBehaviourModifier(): ERlvLocalBhvrModifier = eBhvrModifier
    fun getObjectID(): UUID = idObj
    fun getOption(): String = strOption
    fun getParam(): String = strParam
    fun getParamType(): ERlvParamType = eParamType
    fun hasOption(): Boolean = strOption.isNotEmpty()
    fun isBlocked(): Boolean = pBhvrInfo?.isBlocked() ?: false
    fun isModifier(): Boolean = eBhvrModifier != ERlvLocalBhvrModifier.Unknown
    fun isRefCounted(): Boolean = refCounted
    fun isStrict(): Boolean = fStrict
    fun isValid(): Boolean = valid

    fun processCommand(): ERlvCmdRet = pBhvrInfo?.processCommand(this) ?: ERlvCmdRet.RLV_RET_NO_PROCESSOR

    fun markRefCounted(): Boolean { refCounted = true; return true }

    override fun equals(other: Any?): Boolean {
        if (other !is RlvCommand) return false
        return strBehaviour == other.strBehaviour && strOption == other.strOption &&
            (if (eParamType != ERlvParamType.RLV_TYPE_UNKNOWN) eParamType == other.eParamType else strParam == other.strParam)
    }

    override fun hashCode(): Int = 31 * strBehaviour.hashCode() + strOption.hashCode()
}

// ============================================================================
// RlvCommandOptionHelper — option-string parsing utilities
// ============================================================================

object RlvCommandOptionHelper {
    fun parseStringList(strOption: String, separator: String = RLV_OPTION_SEPARATOR): List<String> =
        strOption.split(separator).filter { it.isNotEmpty() }

    fun parseFloat(strOption: String): Float? = strOption.toFloatOrNull()
    fun parseInt(strOption: String): Int? = strOption.toIntOrNull()
    fun parseUUID(strOption: String): UUID? = runCatching { UUID.fromString(strOption) }.getOrNull()
}

// ============================================================================
// RlvObject — tracks restrictions held by a single in-world object
// ============================================================================

class RlvObject(private val idObj: UUID) {
    private var idxAttachPt: Int = 0
    private var idRoot: UUID = idObj
    private var fLookup: Boolean = false
    private var nLookupMisses: Short = 0
    private val commands: MutableList<RlvCommand> = mutableListOf()
    private val modifiers: MutableMap<ERlvLocalBhvrModifier, Any> = mutableMapOf()

    fun getObjectID(): UUID = idObj
    fun getRootID(): UUID = idRoot
    fun getAttachPt(): Int = idxAttachPt
    fun hasLookup(): Boolean = fLookup
    fun getCommandList(): List<RlvCommand> = commands

    fun addCommand(rlvCmd: RlvCommand): Pair<RlvCommand, Boolean> {
        val existing = commands.find { it == rlvCmd }
        if (existing != null) {
            existing.markRefCounted()
            return Pair(existing, false)
        }
        commands.add(rlvCmd)
        return Pair(rlvCmd, true)
    }

    fun removeCommand(rlvCmd: RlvCommand): Boolean {
        val idx = commands.indexOfFirst { it == rlvCmd }
        if (idx < 0) return false
        val cmd = commands[idx]
        if (cmd.isRefCounted()) {
            // ref-counted: only mark un-counted on first remove
            return true
        }
        commands.removeAt(idx)
        return true
    }

    fun hasBehaviour(eBehaviour: ERlvBehaviour, strictOnly: Boolean): Boolean =
        commands.any { it.getBehaviourType() == eBehaviour && (!strictOnly || it.isStrict()) }

    fun hasBehaviour(eBehaviour: ERlvBehaviour, strOption: String, strictOnly: Boolean): Boolean =
        commands.any { it.getBehaviourType() == eBehaviour && it.getOption() == strOption && (!strictOnly || it.isStrict()) }

    fun getStatusString(strFilter: String, strSeparator: String): String {
        val sb = StringBuilder()
        for (cmd in commands) {
            val str = cmd.asString()
            if (strFilter.isEmpty() || str.contains(strFilter)) {
                if (sb.isNotEmpty()) sb.append(strSeparator)
                sb.append(str)
            }
        }
        return sb.toString()
    }

    fun clearModifiers(eBhvr: ERlvBehaviour) {
        val toRemove = commands.filter { it.getBehaviourType() == eBhvr }
            .mapNotNull { cmd -> cmd.getBehaviourModifier().takeIf { it != ERlvLocalBhvrModifier.Unknown } }
        for (mod in toRemove) modifiers.remove(mod)
    }

    fun clearModifierValue(eBhvrMod: ERlvLocalBhvrModifier) {
        modifiers.remove(eBhvrMod)
    }

    fun <T> getModifierValue(eBhvrModifier: ERlvLocalBhvrModifier): T? {
        @Suppress("UNCHECKED_CAST")
        return modifiers[eBhvrModifier] as? T
    }

    fun setModifierValue(eBhvrMod: ERlvLocalBhvrModifier, modValue: Any) {
        modifiers[eBhvrMod] = modValue
    }
}

// ============================================================================
// RlvForceWear — wear/remove inventory items on behalf of RLV commands
// ============================================================================

object RlvForceWear {

    enum class EWearAction { ACTION_WEAR_REPLACE, ACTION_WEAR_ADD, ACTION_REMOVE }
    enum class EWearFlags(val value: Int) { FLAG_NONE(0x00), FLAG_MATCHALL(0x01) }

    fun isWearAction(action: EWearAction): Boolean =
        action == EWearAction.ACTION_WEAR_REPLACE || action == EWearAction.ACTION_WEAR_ADD

    fun isWearableItem(item: Any?): Boolean {
        TODO("APR: use JVM equivalent - check if item is bodypart, clothing, object, or gesture asset type")
    }

    fun isWearingItem(item: Any?): Boolean {
        TODO("APR: use JVM equivalent - check agent wearables/attachments for item")
    }

    fun isStrippable(idItem: UUID): Boolean {
        TODO("APR: use JVM equivalent - look up item and check for nostrip folder flag")
    }

    fun isStrippable(item: Any?): Boolean {
        TODO("APR: use JVM equivalent - check item name/folder for nostrip flag")
    }

    fun isForceDetachable(attachObj: Any?, checkComposite: Boolean = true, idExcept: UUID = UUID(0, 0)): Boolean {
        TODO("APR: use JVM equivalent - check attachment lock state for the given attachment object")
    }

    fun forceDetach(attachObj: Any?) {
        TODO("APR: use JVM equivalent - request detach of the given attachment object via AppearanceMgr")
    }

    fun forceDetach(attachPt: Any?) {
        TODO("APR: use JVM equivalent - request detach of all items at the given attachment point")
    }

    fun isForceRemovable(wearable: Any?, checkComposite: Boolean = true, idExcept: UUID = UUID(0, 0)): Boolean {
        TODO("APR: use JVM equivalent - check wearable lock state")
    }

    fun forceRemove(wearable: Any?) {
        TODO("APR: use JVM equivalent - request removal of the given wearable via AppearanceMgr")
    }

    fun forceFolder(folder: Any?, action: EWearAction, flags: EWearFlags) {
        TODO("APR: use JVM equivalent - enumerate folder contents and queue wear/remove operations")
    }

    fun done() {
        TODO("APR: use JVM equivalent - flush pending add/remove wearable and attachment queues to AppearanceMgr")
    }

    fun updatePendingAttachments() {
        TODO("APR: use JVM equivalent - check pending attachment requests and trigger attachment via AttachmentsMgr")
    }
}

// ============================================================================
// RlvBehaviourNotifyHandler — @notify channel broadcasting
// ============================================================================

object RlvBehaviourNotifyHandler {

    private data class NotifyEntry(val idObj: UUID, val nChannel: Int, val strFilter: String)
    private val notifications: MutableList<NotifyEntry> = mutableListOf()
    private var commandSlot: ((RlvCommand, ERlvCmdRet, Boolean) -> Unit)? = null

    fun addNotify(idObj: UUID, nChannel: Int, strFilter: String) {
        if (notifications.isEmpty()) {
            val slot: (RlvCommand, ERlvCmdRet, Boolean) -> Unit = { cmd, ret, internal -> onCommand(cmd, ret, internal) }
            commandSlot = slot
            RlvHandler.instance.onCommand.add(slot)
        }
        notifications.add(NotifyEntry(idObj, nChannel, strFilter))
    }

    fun removeNotify(idObj: UUID, nChannel: Int, strFilter: String) {
        notifications.removeIf { it.idObj == idObj && it.nChannel == nChannel && it.strFilter == strFilter }
        if (notifications.isEmpty()) {
            commandSlot?.let { RlvHandler.instance.onCommand.remove(it) }
            commandSlot = null
        }
    }

    fun sendNotification(text: String, suffix: String = "") {
        val fullText = if (suffix.isEmpty()) text else "$text$suffix"
        for (entry in notifications) {
            if (entry.strFilter.isEmpty() || fullText.contains(entry.strFilter)) {
                RlvUtil.sendChatReply(entry.nChannel, fullText)
            }
        }
    }

    fun onWear(eType: Int, allowed: Boolean) {
        TODO("APR: use JVM equivalent - send @wear notification on registered channels")
    }

    fun onTakeOff(eType: Int, allowed: Boolean) {
        TODO("APR: use JVM equivalent - send @takeoff notification on registered channels")
    }

    fun onAttach(attachPt: Any?, allowed: Boolean) {
        TODO("APR: use JVM equivalent - send @attach notification on registered channels")
    }

    fun onDetach(attachPt: Any?, allowed: Boolean) {
        TODO("APR: use JVM equivalent - send @detach notification on registered channels")
    }

    fun onReattach(attachPt: Any?, allowed: Boolean) {
        TODO("APR: use JVM equivalent - send @reattach notification on registered channels")
    }

    fun onSit(idObj: UUID, allowed: Boolean) {
        TODO("APR: use JVM equivalent - send @sit notification on registered channels")
    }

    fun onStand(idObj: UUID, allowed: Boolean) {
        TODO("APR: use JVM equivalent - send @stand notification on registered channels")
    }

    private fun onCommand(rlvCmd: RlvCommand, eRet: ERlvCmdRet, internal: Boolean) {
        TODO("APR: use JVM equivalent - format command result string and sendNotification to matching channels")
    }
}

// ============================================================================
// RlvGCTimer — periodic garbage collection trigger (30-second tick)
// ============================================================================

object RlvGCTimer {
    private const val PERIOD_MS = 30_000L
    private var task: Any? = null   // platform timer handle

    fun start() {
        TODO("APR: use JVM equivalent - schedule a recurring 30 s timer that calls RlvHandler.instance.onGC()")
    }

    fun stop() {
        TODO("APR: use JVM equivalent - cancel the scheduled GC timer task")
    }
}

// ============================================================================
// Standalone helper functions
// ============================================================================

fun rlvAttachGroupFromIndex(idxGroup: Int): ERlvAttachGroupType =
    ERlvAttachGroupType.values().firstOrNull { it.ordinal == idxGroup } ?: ERlvAttachGroupType.RLV_ATTACHGROUP_INVALID

fun rlvAttachGroupFromString(strGroup: String): ERlvAttachGroupType = when (strGroup.lowercase()) {
    "head"  -> ERlvAttachGroupType.RLV_ATTACHGROUP_HEAD
    "torso" -> ERlvAttachGroupType.RLV_ATTACHGROUP_TORSO
    "arms"  -> ERlvAttachGroupType.RLV_ATTACHGROUP_ARMS
    "legs"  -> ERlvAttachGroupType.RLV_ATTACHGROUP_LEGS
    "hud"   -> ERlvAttachGroupType.RLV_ATTACHGROUP_HUD
    else    -> ERlvAttachGroupType.RLV_ATTACHGROUP_INVALID
}

fun rlvGetFirstParenthesisedText(strText: String): Pair<String, Int> {
    val start = strText.indexOf('(')
    if (start < 0) return Pair("", -1)
    val end = strText.indexOf(')', start)
    if (end < 0) return Pair("", -1)
    return Pair(strText.substring(start + 1, end), start)
}

fun rlvGetLastParenthesisedText(strText: String): Pair<String, Int> {
    val end = strText.lastIndexOf(')')
    if (end < 0) return Pair("", -1)
    val start = strText.lastIndexOf('(', end)
    if (start < 0) return Pair("", -1)
    return Pair(strText.substring(start + 1, end), start)
}

object Rlv {
    fun forceAtmosphericShadersIfAvailable() {
        TODO("GPU: enable atmospheric shaders if the GPU supports them")
    }

    fun getObjectLinkNumber(idObj: UUID): Int {
        TODO("APR: use JVM equivalent - return link number of object within its linkset")
    }

    fun getObjectRootId(idObj: UUID): UUID {
        TODO("APR: use JVM equivalent - return root object UUID for the given object")
    }
}

// ============================================================================
// ERlvParamType extension for ADDREM bitmask (used in dictionary lookups)
// ============================================================================
private val ERlvParamType.Companion.RLV_TYPE_ADDREM: Int
    get() = ERlvParamType.RLV_TYPE_ADD.value or ERlvParamType.RLV_TYPE_REMOVE.value
