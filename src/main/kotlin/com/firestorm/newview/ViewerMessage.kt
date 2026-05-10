package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.*
import com.firestorm.llmessage.*

// ── Stub types ────────────────────────────────────────────────────────────────

/**
 * Minimal stub for the C++ LLMessageSystem.
 *
 * The real class wraps UDP circuit management and a binary message codec.
 * For the Kotlin port, handlers receive this stub so their signatures match;
 * real network I/O will be filled in later.
 */
class LLMessageSystem {
    fun getString(block: String, field: String): String = TODO("Read string field from message")
    fun getUUID(block: String, field: String): LLUUID   = TODO("Read UUID field from message")
    fun getInt(block: String, field: String): Int        = TODO("Read S32 field from message")
    fun getFloat(block: String, field: String): Float    = TODO("Read F32 field from message")
    fun getBoolean(block: String, field: String): Boolean = TODO("Read BOOL field from message")
    fun getNumberOfBlocks(block: String): Int             = TODO("Return block count")
}

// ── Inventory-offer response enum ─────────────────────────────────────────────

enum class InventoryOfferResponse {
    ACCEPT,
    DECLINE,
    MUTE,
    SHOW,
    ACCEPT_SILENT,
    DECLINE_SILENT,
    SHOW_SILENT,
}

// ── Offer-info data class ─────────────────────────────────────────────────────

/**
 * Kotlin equivalent of C++ `LLOfferInfo`.
 *
 * Carries all metadata needed to accept or decline an inventory offer
 * and to route the server-side response message.
 */
data class OfferInfo(
    val fromId: LLUUID,
    val transactionId: LLUUID,
    val folderId: LLUUID,
    val objectId: LLUUID,
    val fromName: String,
    val description: String,
    val fromGroup: Boolean = false,
    val fromObject: Boolean = false,
    val persist: Boolean = false,
) {
    fun forceResponse(response: InventoryOfferResponse): Unit =
        TODO("Send accept/decline message to server for response=$response")

    fun sendAutoReceiveResponse(): Unit =
        TODO("Automatically accept offer into folderId=$folderId")

    fun sendDeclineResponse(): Unit =
        TODO("Send decline message to originating host")
}

// ── Teleport-started callback type ───────────────────────────────────────────

typealias TeleportStartedCallback = () -> Unit

// ── Handler function type ─────────────────────────────────────────────────────

/**
 * Every message handler follows the C++ signature
 * `void handler(LLMessageSystem* msg, void** user_data)`.
 *
 * In Kotlin the void** user_data becomes a nullable Any? context object.
 */
typealias MessageHandler = (msg: LLMessageSystem, userData: Any?) -> Unit

// ── ViewerMessage singleton ───────────────────────────────────────────────────

/**
 * Kotlin equivalent of the C++ `LLViewerMessage` singleton plus all the
 * free `process_*` functions declared in llviewermessage.h.
 *
 * Responsibilities:
 *  - Maintain a registry of named message handlers.
 *  - Expose a [dispatch] entry-point used by the network layer.
 *  - Provide a teleport-started signal with subscriber management.
 */
object ViewerMessage {

    // ── Handler registry ─────────────────────────────────────────────────────

    private val handlers: MutableMap<String, MessageHandler> = mutableMapOf()

    fun register(messageName: String, handler: MessageHandler) {
        handlers[messageName] = handler
    }

    /**
     * Dispatch an incoming message by name.
     * Called by the network layer after decoding the message type.
     */
    fun dispatch(messageName: String, msg: LLMessageSystem, userData: Any? = null) {
        handlers[messageName]?.invoke(msg, userData)
            ?: println("ViewerMessage: no handler registered for '$messageName'")
    }

    // ── Teleport signal ──────────────────────────────────────────────────────

    private val teleportStartedCallbacks: MutableList<TeleportStartedCallback> = mutableListOf()

    fun setTeleportStartedCallback(cb: TeleportStartedCallback): AutoCloseable {
        teleportStartedCallbacks.add(cb)
        return AutoCloseable { teleportStartedCallbacks.remove(cb) }
    }

    private fun fireTeleportStarted() {
        teleportStartedCallbacks.toList().forEach { it() }
    }

    // ── One-time handler registration (mirrors C++ register_viewer_messages) ─

    /**
     * Wire all well-known message names to their handler functions.
     * In C++ this is done by calling msg->addHandlerFunc() in
     * `register_viewer_callbacks()`.
     */
    fun registerHandlers() {
        register("ObjectUpdate",         ::processObjectUpdate)
        register("ImprovedTerseObjectUpdate", ::processTerseObjectUpdate)
        register("CompressedObjectUpdate", ::processCompressedObjectUpdate)
        register("KillObject",           ::processKillObject)
        register("AvatarAppearance",     ::processAvatarAppearance)
        register("AvatarAnimation",      ::processAvatarAnimation)
        register("ChatFromSimulator",    ::processChatFromSimulator)
        register("ImprovedInstantMessage", ::processInstantMessage)
        register("TeleportStart",        ::processTeleportStart)
        register("TeleportProgress",     ::processTeleportProgress)
        register("TeleportFailed",       ::processTeleportFailed)
        register("TeleportFinish",       ::processTeleportFinish)
        register("OfferCallingCard",     ::processOfferCallingCard)
        register("AcceptCallingCard",    ::processAcceptCallingCard)
        register("DeclineCallingCard",   ::processDeclineCallingCard)
        register("MoneyBalanceReply",    ::processMoneyBalanceReply)
        register("LogoutReply",          ::processLogoutReply)
        register("AgentMovementComplete", ::processAgentMovementComplete)
        register("CrossedRegion",        ::processCrossedRegion)
        register("ScriptDialog",         ::processScriptDialog)
        register("LoadURL",              ::processLoadUrl)
        register("AlertMessage",         ::processAlertMessage)
    }

    // ── Object update handlers ───────────────────────────────────────────────

    fun processObjectUpdate(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Decode full object update and refresh LLViewerObjectList")

    fun processTerseObjectUpdate(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Decode terse (position/velocity only) object update")

    fun processCompressedObjectUpdate(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Decode compressed object update packet")

    fun processKillObject(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Remove killed object(s) from the viewer object list")

    // ── Avatar handlers ──────────────────────────────────────────────────────

    fun processAvatarAppearance(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Apply baked texture / wearable data to the target avatar")

    fun processAvatarAnimation(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Start or stop animations on the target avatar")

    // ── Chat / IM handlers ───────────────────────────────────────────────────

    fun processChatFromSimulator(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Decode in-world chat message and post to chat floater")

    fun processInstantMessage(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Decode IM, dispatch to IM session manager or auto-respond")

    // ── Teleport handlers ────────────────────────────────────────────────────

    fun processTeleportStart(msg: LLMessageSystem, userData: Any?) {
        TODO("Display teleport-in-progress UI")
        // fireTeleportStarted() would be called here after the TODO is implemented
    }

    fun processTeleportProgress(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Update teleport progress bar")

    fun processTeleportFailed(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Show teleport-failed notification and restore UI")

    fun processTeleportFinish(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Complete teleport: switch regions, re-init objects")

    // ── Calling card handlers ────────────────────────────────────────────────

    fun processOfferCallingCard(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Show accept/decline dialog for incoming calling card offer")

    fun processAcceptCallingCard(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Add new calling card to agent inventory")

    fun processDeclineCallingCard(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Handle remote rejection of our outgoing calling card offer")

    // ── Economy / money handlers ─────────────────────────────────────────────

    fun processMoneyBalanceReply(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Update displayed L$ balance and show transaction toast if needed")

    // ── Session / region handlers ────────────────────────────────────────────

    fun processLogoutReply(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Clean up session state and close viewer on server-confirmed logout")

    fun processAgentMovementComplete(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Finalise agent position after region crossing or teleport")

    fun processCrossedRegion(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Switch to new region circuit after seamless border cross")

    // ── UI / script handlers ─────────────────────────────────────────────────

    fun processScriptDialog(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Show LSL dialog() notification with button choices")

    fun processLoadUrl(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Open external URL from llLoadURL() script call")

    fun processAlertMessage(msg: LLMessageSystem, userData: Any?): Unit =
        TODO("Display modal or non-modal alert from the simulator")

    // ── Utility functions ─────────────────────────────────────────────────────

    fun canAffordTransaction(cost: Int): Boolean =
        TODO("Return true when agent's L$ balance >= cost")

    fun formattedTime(epochSeconds: Long): String =
        TODO("Format epoch timestamp as locale-appropriate string")

    fun sendSimpleIm(
        toId: LLUUID,
        message: String,
        dialog: Int = 0,          // IM_NOTHING_SPECIAL
        id: LLUUID = LLUUID.NULL,
    ): Unit = TODO("Build and send ImprovedInstantMessage UDP packet")

    fun sendGroupNotice(
        groupId: LLUUID,
        subject: String,
        message: String,
        attachmentItemId: LLUUID? = null,
    ): Unit = TODO("Send group notice with optional inventory attachment")
}
