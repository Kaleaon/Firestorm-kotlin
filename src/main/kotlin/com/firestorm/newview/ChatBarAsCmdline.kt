package com.firestorm.newview

import java.util.UUID
import kotlin.random.Random

val cmdLineMPackagerToTake: MutableList<UInt> = mutableListOf()
var cmdLineMPackagerTargetFolderName: String = ""
var cmdLineMPackagerTargetFolder: UUID = NULL_UUID
var cmdLineMPackagerDest: UUID = NULL_UUID

private var gZDrop: JcZdrop? = null
private var gZTake: JcZtake? = null
private var gMTake: TmZtake? = null

private fun findInventoryInFolder(folderName: String): List<ViewerInventoryItem> {
    val folder = inventoryFindCategoryByName(folderName)
    return inventoryCollectDescendents(folder)
}

private class JcZdrop(
    private val stack: ArrayDeque<ViewerInventoryItem>,
    private val destination: UUID,
    private val folderName: String,
    private val dropUuid: String,
    private val isPackage: Boolean = false
) : EventTimer(1.0f) {

    var running: Boolean = false
    private var errorCode: Int = 0

    override fun close() {
        scheduleCleanup { gZDrop = null }
        if (errorCode == 1) {
            reportToNearbyChat("The object with the UUID of \"$dropUuid\" can no longer be found in-world.")
            reportToNearbyChat("This can occur if the object was returned or deleted, or if your client is no longer rendering it.")
            reportToNearbyChat("Transfer from \"$folderName\" to \"$dropUuid\" aborted.")
        } else {
            if (isPackage) {
                reportToNearbyChat("Packager finished, you may now pick up the prim that contains the objects.")
                reportToNearbyChat("Packaged what you had selected in world into the folder \"$folderName\" in your inventory and into the prim with the UUID of \"$dropUuid\"")
                reportToNearbyChat("Don't worry if you look at the contents of package right now, it may show as empty, it isn't, it's just a bug with Second Life itself.")
                reportToNearbyChat("If you take it into your inventory then rez it back out, all the contents will be there.")
            } else {
                reportToNearbyChat("Completed transfer from \"$folderName\" to \"$dropUuid\".")
            }
        }
    }

    override fun tick(): Boolean {
        if (stack.isEmpty()) return running
        val subj = stack.removeFirst()
        val objectp = objectListFindObject(destination)
        return if (objectp != null) {
            reportToNearbyChat("Transferring ${subj.name}")
            // APR: use JVM equivalent for LLToolDragAndDrop::dropInventory
            stack.isEmpty()
        } else {
            errorCode = 1
            true
        }
    }
}

private enum class ZtakeState { COUNTDOWN, SELECTION, TAKE, DROP, DONE }

private class JcZtake(
    private val target: UUID,
    private val isPackage: Boolean = false,
    private val packageDest: UUID = NULL_UUID,
    private val folderName: String = "",
    private val dest: DeRezDestination = DeRezDestination.TAKE_INTO_AGENT_INVENTORY,
    useSelection: Boolean = true,
    private val toTake: MutableList<UInt> = mutableListOf()
) : EventTimer(0.66f) {

    var running: Boolean = false
    private var countdown: Int = 5
    private var state: ZtakeState = if (useSelection) ZtakeState.COUNTDOWN else ZtakeState.TAKE
    private var packSize: Int = 0
    private val donePrims: MutableSet<UInt> = mutableSetOf()

    init {
        if (isPackage) {
            reportToNearbyChat("Packager started. Phase 1 (taking in-world objects into inventory) starting in: ")
        } else {
            reportToNearbyChat("Ztake activated. Taking selected in-world objects into inventory in: ")
        }
    }

    override fun close() {
        if (!isPackage) reportToNearbyChat("Ztake deactivated.")
    }

    override fun tick(): Boolean {
        when (state) {
            ZtakeState.COUNTDOWN -> {
                reportToNearbyChat("$countdown...")
                if (--countdown == 0) state = ZtakeState.SELECTION
            }

            ZtakeState.SELECTION -> {
                for (obj in currentSelection()) {
                    val localId = obj.localId
                    if (donePrims.add(localId)) toTake.add(localId)
                }
                if (toTake.isNotEmpty()) state = ZtakeState.TAKE
            }

            ZtakeState.TAKE -> {
                if (toTake.isNotEmpty()) {
                    val inventory = findInventoryInFolder(folderName)
                    packSize = toTake.size + inventory.size

                    // APR: use JVM equivalent for DeRezObject message

                    toTake.removeAt(0)
                    if (toTake.size % 10 == 0) {
                        when {
                            toTake.isEmpty() -> if (isPackage) {
                                if (packageDest != NULL_UUID) {
                                    state = ZtakeState.DROP
                                } else {
                                    reportToNearbyChat("Ktake has taken all selected objects.")
                                    scheduleCleanup { gZTake = null }
                                    state = ZtakeState.DONE
                                }
                            } else {
                                reportToNearbyChat("Ztake has taken all selected objects. Say \"ztake off\" to deactivate ztake or select more objects to continue.")
                            }
                            else -> reportToNearbyChat(if (isPackage) "Packager: ${toTake.size} objects left to take." else "Ztake: ${toTake.size} objects left to take.")
                        }
                    }
                } else {
                    reportToNearbyChat(if (isPackage) "Packager: no objects to take." else "Ztake: no objects to take.")
                    if (isPackage) scheduleCleanup { gZTake = null }
                }
            }

            ZtakeState.DROP -> {
                countdown--
                val itemStack = ArrayDeque(findInventoryInFolder(folderName))
                if (itemStack.size >= packSize || countdown == 0) {
                    if (itemStack.size < packSize) {
                        reportToNearbyChat("Phase 1 of the packager finished, but some items mave have been missed.")
                    } else {
                        reportToNearbyChat("Phase 1 of the packager finished.")
                    }
                    reportToNearbyChat("Do not have the destination prim selected while transfer is running to reduce the chances of \"Inventory creation on in-world object failed.\"")
                    gZDrop = JcZdrop(itemStack, packageDest, folderName, packageDest.toString(), true)
                    scheduleCleanup { gZTake = null }
                    state = ZtakeState.DONE
                }
            }

            ZtakeState.DONE -> {}
        }
        return running
    }
}

private class TmZtake(private val target: UUID) : EventTimer(0.33f) {

    var running: Boolean = false
    private var countdown: Int = 5
    private val donePrims: MutableSet<UInt> = mutableSetOf()
    private val toTake: MutableList<UInt> = mutableListOf()

    init {
        reportToNearbyChat("Mtake activated. Taking selected in-world objects into inventory in: ")
    }

    override fun close() {
        reportToNearbyChat("Mtake deactivated.")
    }

    override fun tick(): Boolean {
        for (obj in currentSelection()) {
            val localId = obj.localId
            if (donePrims.add(localId)) {
                val scale = obj.scale
                val px = scale.x.toFormattedString()
                val py = scale.y.toFormattedString()
                val pz = scale.z.toFormattedString()
                val name = "${px}x${py}x${pz}"
                // APR: use JVM equivalent for ObjectName message to rename prim to dimensions
                toTake.add(localId)
            }
        }

        if (countdown > 0) {
            reportToNearbyChat("$countdown...")
            countdown--
        } else if (toTake.isNotEmpty()) {
            // APR: use JVM equivalent for DeRezObject message
            toTake.removeAt(0)
            if (toTake.size % 10 == 0) {
                if (toTake.isEmpty()) {
                    reportToNearbyChat("Mtake has taken all selected objects. Say \"mtake off\" to deactivate Mtake or select more objects to continue.")
                } else {
                    reportToNearbyChat("Mtake: ${toTake.size} objects left to take.")
                }
            }
        }
        return running
    }

    private fun Float.toFormattedString(): String {
        var s = "%.6f".format(this)
        s = s.trimEnd('0').trimEnd('.')
        return s
    }
}

fun cmdLineChat(revisedText: String, type: EChatType, fromGesture: Boolean = false): Boolean {
    if (!savedSettingsBool("FSCmdLine")) return true

    val cmdPos = savedSettingsString("FSCmdLinePos")
    val cmdDrawDistance = savedSettingsString("FSCmdLineDrawDistance")
    val cmdTeleportToCam = savedSettingsString("FSCmdTeleportToCam")
    val cmdAO = savedSettingsString("FSCmdLineAO")
    val cmdKeyToName = savedSettingsString("FSCmdLineKeyToName")
    val cmdOfferTp = savedSettingsString("FSCmdLineOfferTp")
    val cmdGround = savedSettingsString("FSCmdLineGround")
    val cmdHeight = savedSettingsString("FSCmdLineHeight")
    val cmdTeleportHome = savedSettingsString("FSCmdLineTeleportHome")
    val cmdRezPlatform = savedSettingsString("FSCmdLineRezPlatform")
    val cmdMapTo = savedSettingsString("FSCmdLineMapTo")
    val cmdMapToKeepPos = savedSettingsBool("FSCmdLineMapToKeepPos")
    val cmdCalc = savedSettingsString("FSCmdLineCalc")
    val cmdTp2 = savedSettingsString("FSCmdLineTP2")
    val cmdClearChat = savedSettingsString("FSCmdLineClearChat")
    val cmdMedia = savedSettingsString("FSCmdLineMedia")
    val cmdMusic = savedSettingsString("FSCmdLineMusic")
    val cmdCopyCam = savedSettingsString("FSCmdLineCopyCam")
    val cmdRollDice = savedSettingsString("FSCmdLineRollDice")
    val cmdBandwidth = savedSettingsString("FSCmdLineBandWidth")

    val tokens = revisedText.trim().split("\\s+".toRegex())
    if (tokens.isEmpty()) return true
    val command = tokens[0]
    if (command.isEmpty()) return true

    fun nextToken(index: Int): String? = tokens.getOrNull(index)
    fun remainingAfter(cmd: String): String = if (revisedText.length > cmd.length + 1) revisedText.substring(cmd.length + 1) else ""

    return when (command) {
        cmdPos -> {
            val x = nextToken(1)?.toFloatOrNull()
            val y = nextToken(2)?.toFloatOrNull()
            if (x != null && y != null) {
                val z = nextToken(3)?.toFloatOrNull() ?: agentPositionZ()
                // APR: use JVM equivalent for gAgent.teleportViaLocation with region-relative coordinates
            }
            false
        }

        cmdDrawDistance -> {
            if (fromGesture) {
                reportToNearbyChat(trans("DrawDistanceSteppingGestureObsolete"))
                savedSettingsSetBool("FSRenderFarClipStepping", true)
                return false
            }
            val dist = nextToken(1)?.toFloatOrNull()
            if (dist != null) {
                savedSettingsSetFloat("RenderFarClip", dist)
                agentCameraSetDrawDistance(dist)
                reportToNearbyChat(trans("FSCmdLineDrawDistanceSet", mapOf("DISTANCE" to "%.0f".format(dist))))
            }
            false
        }

        cmdTeleportToCam -> {
            // APR: use JVM equivalent for gAgent.teleportViaLocation(gAgentCamera.getCameraPositionGlobal())
            false
        }

        cmdMedia -> {
            val url = nextToken(1)
            val mediaType = nextToken(2)
            if (url != null && mediaType != null) {
                // APR: use JVM equivalent for LLViewerParcelMedia play/filterMediaUrl
            }
            false
        }

        cmdMusic -> {
            val status = nextToken(1)
            if (status != null) {
                // APR: use JVM equivalent for LLViewerAudio or LLViewerParcelMedia filterAudioUrl
            }
            false
        }

        cmdBandwidth -> {
            val bw = nextToken(1)?.toIntOrNull()?.coerceIn(50, 3000)
            if (bw != null) {
                savedSettingsSetFloat("ThrottleBandwidthKBPS", bw.toFloat())
                reportToNearbyChat(trans("FSCmdLineRSP", mapOf("[VALUE]" to "$bw")))
            }
            false
        }

        cmdAO -> {
            val status = nextToken(1)
            if (status != null) {
                val aoWasEnabled = perAccountSettingsBool("UseAO")
                when (status) {
                    "on" -> {
                        perAccountSettingsSetBool("UseAO", true)
                        if (!aoWasEnabled) reportToNearbyChat(trans("FSAOEnabled"))
                    }
                    "off" -> {
                        perAccountSettingsSetBool("UseAO", false)
                        if (aoWasEnabled) reportToNearbyChat(trans("FSAODisabled"))
                    }
                    "sit" -> {
                        // APR: use JVM equivalent for AOEngine sit-override toggle
                    }
                }
            }
            false
        }

        cmdKeyToName -> {
            val key = nextToken(1)?.let { parseUUID(it) }
            if (key != null) {
                avatarNameCacheGetAsync(key) { avName ->
                    var name = avName.getCompleteName()
                    if (!rlvActionsCanShowName(key)) name = rlvStringsGetAnonym(avName)
                    reportToNearbyChat("$key: ($name)")
                }
            }
            false
        }

        "/touch" -> {
            val key = nextToken(1)?.let { parseUUID(it) }
            if (key != null) {
                val obj = objectListFindObject(key)
                if (obj == null) {
                    reportToNearbyChat("Object with key $key not found!")
                } else if (!rlvIsEnabled() || rlvActionsCanTouch(obj)) {
                    // APR: use JVM equivalent for ObjectGrab + ObjectDeGrab messages
                    reportToNearbyChat("Touched object with key $key")
                }
            }
            false
        }

        "/siton" -> {
            val key = nextToken(1)?.let { parseUUID(it) }
            if (key != null) {
                val obj = objectListFindObject(key)
                if (obj == null) {
                    reportToNearbyChat("Object with key $key not found!")
                } else if (!rlvIsEnabled() || rlvActionsCanSit(obj)) {
                    // APR: use JVM equivalent for AgentRequestSit message
                    reportToNearbyChat("Sat on object with key $key")
                }
            }
            false
        }

        "/standup" -> {
            if (!rlvIsEnabled() || rlvActionsCanStand()) {
                // APR: use JVM equivalent for gAgent.setControlFlags(AGENT_CONTROL_STAND_UP)
                reportToNearbyChat("Standing up")
            }
            false
        }

        "/zoffset_up" -> {
            val cur = perAccountSettingsFloat("AvatarHoverOffsetZ")
            val step = perAccountSettingsFloat("AvatarHoverOffsetStepSize")
            perAccountSettingsSetFloat("AvatarHoverOffsetZ", cur + step)
            false
        }

        "/zoffset_down" -> {
            val cur = perAccountSettingsFloat("AvatarHoverOffsetZ")
            val step = perAccountSettingsFloat("AvatarHoverOffsetStepSize")
            perAccountSettingsSetFloat("AvatarHoverOffsetZ", cur - step)
            false
        }

        "/zoffset_reset" -> {
            perAccountSettingsSetFloat("AvatarHoverOffsetZ", 0.0f)
            false
        }

        cmdOfferTp -> {
            val key = nextToken(1)?.let { parseUUID(it) }
            if (key != null) {
                // APR: use JVM equivalent for StartLure message
                reportToNearbyChat(trans("FSCmdLineTpOffered", mapOf("NAME" to slurl("agent", key, "inspect"))))
            }
            false
        }

        cmdGround -> {
            // APR: use JVM equivalent for gAgent.teleportViaLocation to land height below agent if RLV allows
            false
        }

        cmdHeight -> {
            val z = nextToken(1)?.toFloatOrNull()
            if (z != null) {
                // APR: use JVM equivalent for gAgent.teleportViaLocation to specified Z height if RLV allows
            }
            false
        }

        cmdTeleportHome -> {
            // APR: use JVM equivalent for gAgent.teleportHome()
            false
        }

        cmdRezPlatform -> {
            if (rlvActionsCanRez()) {
                val width = nextToken(1)?.toFloatOrNull()
                if (width != null) cmdlineRezplat(false, width) else cmdlineRezplat()
            }
            false
        }

        cmdMapTo -> {
            val rest = remainingAfter(command)
            if (rest.isNotEmpty()) {
                val pipeIdx = rest.indexOf('|')
                var regionName: String
                var agentX: Int
                var agentY: Int
                var agentZ: Int
                if (pipeIdx != -1) {
                    regionName = urlEscape(rest.substring(0, pipeIdx).trim())
                    val coordsPart = rest.substring(pipeIdx + 1).trim()
                    val coordTokens = coordsPart.split("\\s+".toRegex())
                    agentX = coordTokens.getOrNull(0)?.toIntOrNull() ?: 128
                    agentY = coordTokens.getOrNull(1)?.toIntOrNull() ?: 128
                    agentZ = coordTokens.getOrNull(2)?.toIntOrNull() ?: 0
                } else {
                    regionName = urlEscape(rest)
                    if (cmdMapToKeepPos) {
                        agentX = agentGlobalPositionX()
                        agentY = agentGlobalPositionY()
                        agentZ = agentGlobalPositionZ()
                    } else {
                        agentX = 128; agentY = 128; agentZ = 0
                    }
                }
                val url = "secondlife:///app/teleport/$regionName/$agentX/$agentY/$agentZ"
                // APR: use JVM equivalent for LLURLDispatcher::dispatch
            }
            false
        }

        cmdCalc -> {
            val expr = remainingAfter(command)
            if (expr.isNotEmpty()) {
                val resolved = resolveRandCalls(expr.uppercase())
                val result = evalMathExpression(resolved)
                val out = if (result == null) "Calculation Failed" else "${expr.uppercase()} = $result"
                reportToNearbyChat(out)
            }
            false
        }

        cmdTp2 -> {
            val name = remainingAfter(command)
            if (name.isNotEmpty()) cmdlineTp2name(name)
            false
        }

        cmdClearChat -> {
            // APR: use JVM equivalent for FSFloaterNearbyChat::clearChatHistory
            false
        }

        "zdrop" -> {
            val setting = nextToken(1)
            when (setting) {
                "on" -> {
                    if (gZDrop != null) {
                        reportToNearbyChat("Zdrop is already active.")
                    } else {
                        val dest = nextToken(2)
                        if (dest == null) {
                            reportToNearbyChat("Please specify an object UUID to copy the items in this folder to.")
                        } else if (!isValidUUID(dest)) {
                            reportToNearbyChat("Entered UUID is invalid! (Hint: use the \"copy key\" button in the build menu.)")
                        } else if (objectListFindObject(parseUUID(dest)!!) == null) {
                            reportToNearbyChat("Unable to locate object. Please verify the object is rezzed and in view, and that the UUID is correct.")
                        } else {
                            val folder = tokens.drop(3).joinToString(" ")
                            val folderId = inventoryFindCategoryByName(folder)
                            if (!isNullUUID(folderId)) {
                                val inventory = findInventoryInFolder(folder)
                                if (inventory.isNotEmpty()) {
                                    reportToNearbyChat("Found folder \"$folder\".")
                                    reportToNearbyChat("Found prim \"$dest\".")
                                    reportToNearbyChat("Transferring inventory items from \"$folder\" to prim \"$dest\".")
                                    reportToNearbyChat("WARNING: No-copy items will be moved to the destination prim!")
                                    reportToNearbyChat("Do not have the prim selected while transfer is running to reduce the chances of \"Inventory creation on in-world object failed.\"")
                                    reportToNearbyChat("Use \"zdrop off\" to stop the transfer")
                                    gZDrop = JcZdrop(ArrayDeque(inventory), parseUUID(dest)!!, folder, dest)
                                }
                            } else {
                                reportToNearbyChat("\"$folder\" folder not found. Please check the spelling.")
                                reportToNearbyChat("Zdrop cannot work if the folder is inside another folder.")
                            }
                        }
                    }
                }
                "off" -> {
                    if (gZDrop == null) {
                        reportToNearbyChat("Zdrop is already deactivated.")
                    } else {
                        gZDrop!!.running = true
                        gZDrop!!.close()
                        gZDrop = null
                    }
                }
                else -> {
                    if (setting != null) {
                        reportToNearbyChat("Invalid command: \"$setting\". Valid commands: zdrop on (source inventory folder) (rezzed prim UUID); zdrop off")
                    } else {
                        reportToNearbyChat("The Zdrop command transfers items from your inventory to a rezzed prim without the need to wait for the contents of the prim to load. No-copy items are moved to the prim. All other items are copied.")
                        reportToNearbyChat("Valid commands: zdrop on (rezzed prim UUID) (source inventory folder name); zdrop off")
                    }
                }
            }
            false
        }

        "ztake" -> {
            val setting = nextToken(1)
            when (setting) {
                "on" -> {
                    if (gZTake != null) {
                        reportToNearbyChat("Ztake is already active.")
                    } else {
                        val folderName = tokens.drop(2).joinToString(" ")
                        val folder = inventoryFindCategoryByName(folderName)
                        if (!isNullUUID(folder)) {
                            reportToNearbyChat("Found destination folder \"$folderName\".")
                            gZTake = JcZtake(folder)
                        } else {
                            reportToNearbyChat("\"$folderName\" folder not found. Please check the spelling.")
                            reportToNearbyChat("Ztake cannot work if the folder is inside another folder.")
                        }
                    }
                }
                "off" -> {
                    if (gZTake == null) {
                        reportToNearbyChat("Ztake is already deactivated.")
                    } else {
                        gZTake!!.running = true
                        gZTake!!.close()
                        gZTake = null
                    }
                }
                else -> {
                    if (setting != null) {
                        reportToNearbyChat("Invalid command: \"$setting\". Valid commands: ztake on (destination inventory folder); ztake off")
                    } else {
                        reportToNearbyChat("The Ztake command copies selected rezzed objects into the folder you specify in your inventory.")
                        reportToNearbyChat("Valid commands: ztake on (destination inventory folder name); ztake off")
                    }
                }
            }
            false
        }

        "lpackage", "cpackage" -> {
            val dest = nextToken(1)
            if (dest == null) {
                reportToNearbyChat("Packager usage: \"$command destination_prim_UUID inventory folder name\"")
            } else if (!isValidUUID(dest)) {
                reportToNearbyChat("Entered UUID is invalid! (Hint: use the \"copy key\" button in the build menu.)")
            } else if (objectListFindObject(parseUUID(dest)!!) == null) {
                reportToNearbyChat("Unable to locate object. Please verify the object is rezzed, in view, and that the UUID is correct.")
            } else {
                val folderName = tokens.drop(2).joinToString(" ")
                val folder = inventoryFindCategoryByName(folderName)
                if (!isNullUUID(folder)) {
                    val rezDest = if (command == "cpackage") DeRezDestination.ACQUIRE_TO_AGENT_INVENTORY
                    else DeRezDestination.TAKE_INTO_AGENT_INVENTORY
                    reportToNearbyChat("Found destination folder \"$folderName\".")
                    gZTake = JcZtake(folder, true, parseUUID(dest)!!, folderName, rezDest)
                } else {
                    reportToNearbyChat("\"$folderName\" folder not found. Please check the spelling.")
                    reportToNearbyChat("The packager cannot work if the folder is inside another folder.")
                }
            }
            false
        }

        "kpackage" -> {
            val dest = nextToken(1)
            if (dest == null) {
                reportToNearbyChat("Packager usage: \"$command destination_prim_UUID inventory folder name\"")
            } else if (!isValidUUID(dest)) {
                reportToNearbyChat("Entered UUID is invalid! (Hint: use the \"copy key\" button in the build menu.)")
            } else if (objectListFindObject(parseUUID(dest)!!) == null) {
                reportToNearbyChat("Unable to locate object. Please verify the object is rezzed, in view, and that the UUID is correct.")
            } else {
                val folderName = nextToken(2)
                if (folderName != null) {
                    val folder = inventoryFindCategoryByName(folderName)
                    if (!isNullUUID(folder)) {
                        val toTake = mutableListOf<UInt>()
                        for (idx in 3 until tokens.size) {
                            val take = tokens[idx]
                            if (!isValidUUID(take)) {
                                reportToNearbyChat("Entered UUID is invalid! (Hint: use the \"copy key\" button in the build menu.)")
                                return false
                            }
                            val obj = objectListFindObject(parseUUID(take)!!)
                            if (obj == null) {
                                reportToNearbyChat("Unable to locate object. Please verify the object is rezzed, in view, and that the UUID is correct.")
                                return false
                            }
                            val localId = obj.localId
                            if (!toTake.contains(localId)) toTake.add(localId)
                        }
                        if (toTake.isEmpty()) {
                            reportToNearbyChat("No objects to take.")
                        } else {
                            reportToNearbyChat("Found destination folder \"$folderName\".")
                            gZTake = JcZtake(folder, true, parseUUID(dest)!!, folderName, DeRezDestination.ACQUIRE_TO_AGENT_INVENTORY, false, toTake)
                        }
                    } else {
                        reportToNearbyChat("\"$folderName\" folder not found. Please check the spelling.")
                        reportToNearbyChat("The packager cannot work if the folder is inside another folder.")
                    }
                }
            }
            false
        }

        "kpackagerstart" -> {
            val dest = nextToken(1)
            if (dest == null) {
                reportToNearbyChat("Packager usage: \"$command destination_prim_UUID inventory folder name\"")
            } else if (!isValidUUID(dest)) {
                reportToNearbyChat("Entered UUID is invalid! (Hint: use the \"copy key\" button in the build menu.)")
            } else if (objectListFindObject(parseUUID(dest)!!) == null) {
                reportToNearbyChat("Unable to locate object. Please verify the object is rezzed, in view, and that the UUID is correct.")
            } else {
                val folderName = nextToken(2)
                if (folderName != null) {
                    val folder = inventoryFindCategoryByName(folderName)
                    if (!isNullUUID(folder)) {
                        reportToNearbyChat("kpackager started. Destination folder: \"$folderName\" Listening to object: \"$dest\"")
                        cmdLineMPackagerToTake.clear()
                        cmdLineMPackagerTargetFolderName = folderName
                        cmdLineMPackagerTargetFolder = folder
                        cmdLineMPackagerDest = parseUUID(dest)!!
                    } else {
                        reportToNearbyChat("\"$folderName\" folder not found. Please check the spelling.")
                        reportToNearbyChat("The packager cannot work if the folder is inside another folder.")
                    }
                }
            }
            false
        }

        "kpackagerstop" -> {
            if (!isNullUUID(cmdLineMPackagerDest)) {
                cmdLineMPackagerToTake.clear()
                cmdLineMPackagerTargetFolderName = ""
                cmdLineMPackagerTargetFolder = NULL_UUID
                cmdLineMPackagerDest = NULL_UUID
                reportToNearbyChat("Packager: Stopped and cleared.")
            }
            false
        }

        "ktake", "kcopy" -> {
            val folderName = nextToken(1)
            if (folderName != null) {
                val folder = inventoryFindCategoryByName(folderName)
                if (!isNullUUID(folder)) {
                    val toTake = mutableListOf<UInt>()
                    for (idx in 2 until tokens.size) {
                        val take = tokens[idx]
                        if (!isValidUUID(take)) {
                            reportToNearbyChat("Entered UUID is invalid! (Hint: use the \"copy key\" button in the build menu.)")
                            return false
                        }
                        val obj = objectListFindObject(parseUUID(take)!!)
                        if (obj == null) {
                            reportToNearbyChat("Unable to locate object. Please verify the object is rezzed, in view, and that the UUID is correct.")
                            return false
                        }
                        val localId = obj.localId
                        if (!toTake.contains(localId)) toTake.add(localId)
                    }
                    if (toTake.isEmpty()) {
                        reportToNearbyChat("No objects to take.")
                    } else {
                        reportToNearbyChat("Found destination folder \"$folderName\".")
                        val rezDest = if (command == "kcopy") DeRezDestination.ACQUIRE_TO_AGENT_INVENTORY
                        else DeRezDestination.TAKE_INTO_AGENT_INVENTORY
                        gZTake = JcZtake(folder, true, NULL_UUID, folderName, rezDest, false, toTake)
                    }
                } else {
                    reportToNearbyChat("\"$folderName\" folder not found. Please check the spelling.")
                    reportToNearbyChat("The packager cannot work if the folder is inside another folder.")
                }
            }
            false
        }

        "mtake" -> {
            val setting = nextToken(1)
            when (setting) {
                "on" -> {
                    if (gMTake != null) {
                        reportToNearbyChat("Mtake is already active.")
                    } else {
                        val folderName = tokens.drop(2).joinToString(" ")
                        val folder = inventoryFindCategoryByName(folderName)
                        if (!isNullUUID(folder)) {
                            reportToNearbyChat("Found destination folder \"$folderName\".")
                            gMTake = TmZtake(folder)
                        } else {
                            reportToNearbyChat("\"$folderName\" folder not found. Please check the spelling.")
                            reportToNearbyChat("Mtake cannot work if the folder is inside another folder.")
                        }
                    }
                }
                "off" -> {
                    if (gMTake == null) {
                        reportToNearbyChat("Mtake is already deactivated.")
                    } else {
                        gMTake!!.running = true
                        gMTake!!.close()
                        gMTake = null
                    }
                }
                else -> {
                    if (setting != null) {
                        reportToNearbyChat("Invalid command: \"$setting\". Valid commands: mtake on (destination inventory folder); mtake off")
                    } else {
                        reportToNearbyChat("The Mtake command renames selected rezzed objects to the dimensions of the prim, then copies them into the folder you specify in your inventory.")
                        reportToNearbyChat("Valid commands: mtake on (destination inventory folder name); mtake off")
                    }
                }
            }
            false
        }

        "invrepair" -> {
            // APR: use JVM equivalent for gInventory.collectDescendents full repair traversal
            true
        }

        cmdCopyCam -> {
            // APR: use JVM equivalent for gAgentCamera.getCameraPositionAgent + clipboard copy
            false
        }

        cmdRollDice -> {
            val dice = nextToken(1)?.toIntOrNull() ?: 1
            val faces = nextToken(2)?.toIntOrNull() ?: 6
            if (dice < 1 || faces < 1) {
                if (nextToken(1) != null) {
                    reportToNearbyChat(trans("FSCmdLineRollDiceLimits"))
                    return false
                }
            }
            val modifierType = nextToken(3) ?: ""
            val modifier = nextToken(4)?.toIntOrNull() ?: 0
            val result = rollDice(dice, faces, modifierType, modifier)
            reportToNearbyChat(trans("FSCmdLineRollDiceTotal", mapOf("DICE" to "$dice", "FACES" to "$faces", "RESULT" to "$result", "MODIFIER" to modifierType + if (modifierType.isNotEmpty()) "$modifier" else "")))
            false
        }

        else -> true
    }
}

fun cmdlinePartialName2key(partialName: String): UUID {
    val lower = partialName.lowercase().replace(".", " ")
    val radarList = fsRadarGetRadarList()
    for ((_, entry) in radarList) {
        val avName = entry.getUserName().lowercase()
        if (avName.contains(lower)) return entry.getId()
    }
    return NULL_UUID
}

fun cmdlineTp2name(target: String) {
    val avKey = cmdlinePartialName2key(target)
    if (!isNullUUID(avKey) && avKey != agentId()) {
        // APR: use JVM equivalent for LLAvatarActions::teleportTo
    }
}

fun cmdlineRezplat(useSavedValue: Boolean = true, visualRadius: Float = 30.0f) {
    // APR: use JVM equivalent for ObjectAdd message to rez a flat platform prim at agent position - 2.5f Z
}

fun cmdlinePackager(message: String, fromId: UUID, ownerId: UUID): Boolean {
    if (message.isEmpty() || isNullUUID(cmdLineMPackagerDest) || fromId != cmdLineMPackagerDest) return false

    val cmd = if (message.length >= 12) message.substring(0, 12) else return false

    return when (cmd) {
        "kpackageradd" -> {
            val csv = if (message.length > 13) message.substring(13) else return false
            for (item in csv.split(",").map { it.trim() }) {
                val obj = objectListFindObject(parseUUID(item) ?: run {
                    reportToNearbyChat("Packager: Unable to locate object. Please verify the object is rezzed, in view, and that the UUID is correct: \"$item\"")
                    return false
                })
                if (obj == null) {
                    reportToNearbyChat("Packager: Unable to locate object. Please verify the object is rezzed, in view, and that the UUID is correct: \"$item\"")
                    return false
                }
                val localId = obj.localId
                if (!cmdLineMPackagerToTake.contains(localId)) cmdLineMPackagerToTake.add(localId)
            }
            reportToNearbyChat("Packager: adding objects: \"$csv\"")
            true
        }
        "kpackagerend" -> {
            reportToNearbyChat("Packager: finalizing.")
            gZTake = JcZtake(cmdLineMPackagerTargetFolder, true, cmdLineMPackagerDest, cmdLineMPackagerTargetFolderName, DeRezDestination.ACQUIRE_TO_AGENT_INVENTORY, false, cmdLineMPackagerToTake.toMutableList())
            cmdLineMPackagerToTake.clear()
            cmdLineMPackagerTargetFolderName = ""
            cmdLineMPackagerTargetFolder = NULL_UUID
            cmdLineMPackagerDest = NULL_UUID
            false
        }
        else -> false
    }
}

private fun rollDice(dice: Int, faces: Int, modifierType: String, modifier: Int): Int {
    if (dice < 1 || dice > 100 || faces < 1 || faces > 1000) {
        reportToNearbyChat(trans("FSCmdLineRollDiceLimits"))
        return 0
    }
    var result = 0
    var dieIter = 1
    var successes = 0
    var diePenetrated = false
    var freezeGuard = 0

    while (dieIter <= dice) {
        var roll = 1 + Random.nextInt(faces)
        if (diePenetrated) {
            roll -= 1
            diePenetrated = false
            reportToNearbyChat("#$dieIter 1d${faces}-1: $roll.")
        } else {
            reportToNearbyChat("#$dieIter 1d$faces: $roll.")
        }
        result += roll
        dieIter++

        when {
            modifierType == "<" -> {
                if (roll <= modifier) { reportToNearbyChat("  ^-- ${trans("FSCmdLineRollDiceSuccess")}"); successes++ }
                else result -= roll
            }
            modifierType == ">" -> {
                if (roll >= modifier) { reportToNearbyChat("  ^-- ${trans("FSCmdLineRollDiceSuccess")}"); successes++ }
                else result -= roll
            }
            (modifierType == "!" && roll == modifier) || (modifierType == "!>" && roll >= modifier) || (modifierType == "!<" && roll <= modifier) -> {
                reportToNearbyChat("  ^-- ${trans("FSCmdLineRollDiceExploded")}")
                dieIter--
            }
            (modifierType == "!p" && roll == modifier) || (modifierType == "!p>" && roll >= modifier) || (modifierType == "!p<" && roll <= modifier) -> {
                reportToNearbyChat("  ^-- ${trans("FSCmdLineRollDicePenetrated")}")
                diePenetrated = true
                dieIter--
            }
            (modifierType == "r" && roll == modifier) || (modifierType == "r>" && roll >= modifier) || (modifierType == "r<" && roll <= modifier) -> {
                result -= roll
                reportToNearbyChat("  ^-- ${trans("FSCmdLineRollDiceReroll")}")
                dieIter--
            }
        }

        if (++freezeGuard > 1000) {
            reportToNearbyChat(trans("FSCmdLineRollDiceFreezeGuard"))
            return result
        }
    }

    when (modifierType) {
        "+" -> result += modifier
        "-" -> result -= modifier
        ">", "<" -> reportToNearbyChat("${trans("FSCmdLineRollDiceSuccess")}: $successes")
    }
    return result
}

private fun resolveRandCalls(expr: String): String {
    var result = expr
    val pattern = Regex("RAND\\((-?[0-9]+),(-?[0-9]+)\\)")
    var loopAttempts = 0
    while (loopAttempts < 5) {
        val match = pattern.find(result) ?: break
        loopAttempts++
        val min = match.groupValues[1].toInt()
        val max = match.groupValues[2].toInt()
        val randomNumber = if (max > min && min >= -10000 && max <= 10000) {
            min + Random.nextInt(max - min + 1)
        } else {
            reportToNearbyChat(trans("FSCmdLineCalcRandError", mapOf("RAND" to match.value)))
            0
        }
        result = result.replace(match.value, "$randomNumber")
    }
    return result
}

private fun evalMathExpression(expr: String): Float? {
    System.err.println("ChatBarAsCmdline: evalMathExpression not yet implemented")
    return null
}

private fun scheduleCleanup(action: () -> Unit) {
    System.err.println("ChatBarAsCmdline: scheduleCleanup not yet implemented")
}

private fun reportToNearbyChat(msg: String) {
    System.err.println("ChatBarAsCmdline: reportToNearbyChat not yet implemented")
}

private fun savedSettingsBool(key: String): Boolean {
    System.err.println("ChatBarAsCmdline: savedSettingsBool not yet implemented")
    return false
}

private fun savedSettingsString(key: String): String {
    System.err.println("ChatBarAsCmdline: savedSettingsString not yet implemented")
    return ""
}

private fun savedSettingsSetBool(key: String, value: Boolean) {
    System.err.println("ChatBarAsCmdline: savedSettingsSetBool not yet implemented")
}

private fun savedSettingsSetFloat(key: String, value: Float) {
    System.err.println("ChatBarAsCmdline: savedSettingsSetFloat not yet implemented")
}

private fun perAccountSettingsBool(key: String): Boolean {
    System.err.println("ChatBarAsCmdline: perAccountSettingsBool not yet implemented")
    return false
}

private fun perAccountSettingsSetBool(key: String, value: Boolean) {
    System.err.println("ChatBarAsCmdline: perAccountSettingsSetBool not yet implemented")
}

private fun perAccountSettingsFloat(key: String): Float {
    System.err.println("ChatBarAsCmdline: perAccountSettingsFloat not yet implemented")
    return 0f
}

private fun perAccountSettingsSetFloat(key: String, value: Float) {
    System.err.println("ChatBarAsCmdline: perAccountSettingsSetFloat not yet implemented")
}

private fun agentPositionZ(): Float {
    System.err.println("ChatBarAsCmdline: agentPositionZ not yet implemented")
    return 0f
}

private fun agentCameraSetDrawDistance(dist: Float) {
    System.err.println("ChatBarAsCmdline: agentCameraSetDrawDistance not yet implemented")
}

private fun agentGlobalPositionX(): Int {
    System.err.println("ChatBarAsCmdline: agentGlobalPositionX not yet implemented")
    return 0
}

private fun agentGlobalPositionY(): Int {
    System.err.println("ChatBarAsCmdline: agentGlobalPositionY not yet implemented")
    return 0
}

private fun agentGlobalPositionZ(): Int {
    System.err.println("ChatBarAsCmdline: agentGlobalPositionZ not yet implemented")
    return 0
}

private fun agentId(): UUID {
    System.err.println("ChatBarAsCmdline: agentId not yet implemented")
    return NULL_UUID
}

private fun inventoryFindCategoryByName(name: String): UUID {
    System.err.println("ChatBarAsCmdline: inventoryFindCategoryByName not yet implemented")
    return NULL_UUID
}

private fun inventoryCollectDescendents(folderId: UUID): List<ViewerInventoryItem> {
    System.err.println("ChatBarAsCmdline: inventoryCollectDescendents not yet implemented")
    return emptyList()
}

private fun objectListFindObject(id: UUID): ViewerObject? {
    System.err.println("ChatBarAsCmdline: objectListFindObject not yet implemented")
    return null
}

private fun currentSelection(): List<ViewerObject> =
    TODO("APR: use JVM equivalent for LLSelectMgr::getSelection root iterator")

private fun rlvIsEnabled(): Boolean =
    TODO("APR: use JVM equivalent for RlvActions::isRlvEnabled")

private fun rlvActionsCanShowName(id: UUID): Boolean =
    TODO("APR: use JVM equivalent for RlvActions::canShowName")

private fun rlvActionsCanTouch(obj: ViewerObject): Boolean =
    TODO("APR: use JVM equivalent for RlvActions::canTouch")

private fun rlvActionsCanSit(obj: ViewerObject): Boolean =
    TODO("APR: use JVM equivalent for RlvActions::canSit")

private fun rlvActionsCanStand(): Boolean =
    TODO("APR: use JVM equivalent for RlvActions::canStand")

private fun rlvActionsCanRez(): Boolean =
    TODO("APR: use JVM equivalent for RlvActions::canRez")

private fun rlvActionsCanTeleportToLocal(pos: Any): Boolean =
    TODO("APR: use JVM equivalent for RlvActions::canTeleportToLocal")

private fun rlvStringsGetAnonym(avName: AvatarName): String =
    TODO("APR: use JVM equivalent for RlvStrings::getAnonym")

private fun avatarNameCacheGetAsync(id: UUID, callback: (AvatarName) -> Unit): Unit =
    TODO("APR: use JVM equivalent for LLAvatarNameCache::get with callback")

private fun fsRadarGetRadarList(): Map<UUID, RadarEntry> =
    TODO("APR: use JVM equivalent for FSRadar::getInstance()->getRadarList()")

private fun isValidUUID(s: String): Boolean = runCatching { UUID.fromString(s) }.isSuccess

private fun parseUUID(s: String): UUID? = runCatching { UUID.fromString(s) }.getOrNull()

private fun isNullUUID(id: UUID): Boolean =
    id == UUID.fromString("00000000-0000-0000-0000-000000000000")

private fun urlEscape(s: String): String =
    TODO("APR: use JVM equivalent for LLWeb::escapeURL")

private fun slurl(scheme: String, id: UUID, action: String): String =
    TODO("APR: use JVM equivalent for LLSLURL(...).getSLURLString()")

private fun trans(key: String, args: Map<String, String> = emptyMap()): String =
    TODO("APR: use JVM equivalent for LLTrans::getString")
