package com.firestorm.llui

import java.util.UUID

// CommandId is a stable identifier for a command, derived from a name-seeded UUID.
data class CommandId(val uuid: UUID) {
    constructor(name: String) : this(UUID.nameUUIDFromBytes(name.toByteArray(Charsets.UTF_8)))

    companion object {
        val NULL = CommandId("null command")
    }
}

typealias CommandIdList = MutableList<CommandId>

class Command(params: Params) {

    data class Params(
        val availableInToybox: Boolean = false,
        val icon: String = "",
        val labelRef: String = "",
        val name: String = "",
        val tooltipRef: String = "",
        val executeFunction: String = "",
        val executeParameters: Any? = null,
        val executeStopFunction: String = "",
        val executeStopParameters: Any? = null,
        val isEnabledFunction: String = "",
        val isEnabledParameters: Any? = null,
        val isRunningFunction: String = "",
        val isRunningParameters: Any? = null,
        val isStartingFunction: String = "",
        val isStartingParameters: Any? = null,
        val isFlashingAllowed: Boolean = false,
        val controlName: String = "",
        val checkboxControl: String = ""
    )

    val id: CommandId = CommandId(params.name)

    val availableInToybox: Boolean    = params.availableInToybox
    val icon: String                  = params.icon
    val labelRef: String              = params.labelRef
    val name: String                  = params.name
    val tooltipRef: String            = params.tooltipRef

    val executeFunctionName: String   = params.executeFunction
    val executeParameters: Any?       = params.executeParameters

    val executeStopFunctionName: String  = params.executeStopFunction
    val executeStopParameters: Any?      = params.executeStopParameters

    val isEnabledFunctionName: String = params.isEnabledFunction
    val isEnabledParameters: Any?     = params.isEnabledParameters

    val isRunningFunctionName: String = params.isRunningFunction
    val isRunningParameters: Any?     = params.isRunningParameters

    val isStartingFunctionName: String = params.isStartingFunction
    val isStartingParameters: Any?     = params.isStartingParameters

    val isFlashingAllowed: Boolean    = params.isFlashingAllowed

    val controlVariableName: String        = params.controlName
    val checkboxControlVariableName: String = params.checkboxControl
}

object CommandManager {

    private val commands: MutableList<Command> = mutableListOf()
    private val commandIndices: MutableMap<UUID, Int> = mutableMapOf()

    fun commandCount(): UInt = commands.size.toUInt()

    fun getCommand(commandIndex: UInt): Command = commands[commandIndex.toInt()]

    fun getCommand(commandId: CommandId): Command? {
        val index = commandIndices[commandId.uuid] ?: return null
        return commands[index]
    }

    fun getCommand(name: String): Command? = commands.firstOrNull { it.name == name }

    fun addCommand(command: Command) {
        commandIndices[command.id.uuid] = commands.size
        commands.add(command)
    }

    fun load(): Boolean {
        TODO("APR: use JVM equivalent — read commands.xml from app settings path and populate CommandManager")
    }
}
