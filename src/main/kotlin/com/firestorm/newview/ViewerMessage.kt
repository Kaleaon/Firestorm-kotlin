package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Logger
import kotlin.concurrent.thread

private val LOG: Logger = Logger.getLogger("ViewerMessage")

/**
 * Extract a LLUUID as a 16-byte big-endian array.
 * Replicates the private toBytes() logic from LLUUID using the public UUID fields.
 */
private fun LLUUID.asBytes(): ByteArray {
    val msb = uuid.mostSignificantBits
    val lsb = uuid.leastSignificantBits
    return ByteArray(16) { i ->
        if (i < 8) ((msb ushr ((7 - i) * 8)) and 0xffL).toByte()
        else       ((lsb ushr ((15 - i) * 8)) and 0xffL).toByte()
    }
}

// ─── Message transport layer ────────────────────────────────────────────────
//
// In the C++ viewer each "send" function calls gMessageSystem to pack fields
// into an LLMessageSystem buffer and then calls msg->sendReliable(host).
// Here we model the same concept with a lightweight ByteBuffer-based packer
// and a DatagramSocket sender.  A null-host guard mirrors the C++ early-return
// pattern ("if (gAgent.getRegion() == NULL) return;").

private val udpSocket: DatagramSocket by lazy { DatagramSocket() }

/**
 * Pack a minimal LLUDP-style header: 6-byte flags+sequence, then a 1-byte
 * message-ID placeholder.  Real SL packets use a variable-length encoding;
 * this is intentionally simplified for a JVM skeleton.
 */
private fun newUdpBuffer(capacity: Int = 2048): ByteBuffer =
    ByteBuffer.allocate(capacity).order(ByteOrder.BIG_ENDIAN)

/** Send [buf] (flipped) to [host] on [port] via UDP. */
private fun sendUdp(buf: ByteBuffer, host: String, port: Int = 13000) {
    buf.flip()
    val bytes = ByteArray(buf.remaining())
    buf.get(bytes)
    val addr = InetSocketAddress(host, port)
    val packet = DatagramPacket(bytes, bytes.size, addr)
    try {
        udpSocket.send(packet)
    } catch (e: Exception) {
        LOG.warning("UDP send to $host:$port failed: ${e.message}")
    }
}

// ─── Agent state stubs ───────────────────────────────────────────────────────
// In the C++ viewer these come from the singleton gAgent.  The JVM port
// exposes simple mutable state so that functions below compile and are
// wirable to a real agent model later.

object AgentState {
    var agentId: LLUUID = LLUUID.NULL
    var sessionId: LLUUID = LLUUID.NULL
    var balance: Int = 0
    var regionHost: String = "127.0.0.1"
    var regionPort: Int = 13000
    var positionX: Float = 0f
    var positionY: Float = 0f
    var positionZ: Float = 0f
    var isAfk: Boolean = false
}

// ─── Notification bus ────────────────────────────────────────────────────────
// Replaces LLNotificationsUtil::add().  Callers can register a sink for
// real UI integration; the default sink just logs to stdout.

typealias NotificationSink = (name: String, args: Map<String, String>) -> Unit

object Notifications {
    var sink: NotificationSink = { name, args -> println("[NOTIFY] $name $args") }
    fun add(name: String, args: Map<String, String> = emptyMap()) = sink(name, args)
    fun addModal(message: String) = sink("ModalAlert", mapOf("MESSAGE" to message))
}

// ─── Enums / constants ───────────────────────────────────────────────────────

enum class InventoryOfferResponse {
    ACCEPT,
    DECLINE,
    MUTE,
    SHOW,
    ACCEPT_SILENT,
    DECLINE_SILENT,
    SHOW_SILENT,
}

// Instant-message dialog constants (mirrors EInstantMessage)
const val IM_NOTHING_SPECIAL            = 0
const val IM_INVENTORY_OFFERED          = 4
const val IM_INVENTORY_ACCEPTED         = 5
const val IM_INVENTORY_DECLINED         = 6
const val IM_TASK_INVENTORY_OFFERED     = 14
const val IM_TASK_INVENTORY_ACCEPTED    = 15
const val IM_TASK_INVENTORY_DECLINED    = 16
const val IM_GROUP_NOTICE               = 22
const val IM_GROUP_NOTICE_REQUESTED     = 23
const val IM_GROUP_INVITATION_ACCEPT    = 37
const val IM_GROUP_INVITATION_DECLINE   = 38
const val IM_LURE_USER                  = 6
const val IM_LURE_DECLINED              = 7
const val IM_DO_NOT_DISTURB_AUTO_RESPONSE = 8
const val IM_ONLINE                     = 0

const val NO_TIMESTAMP: UInt = 0u

// ─── Type aliases ────────────────────────────────────────────────────────────

typealias TeleportStartedCallback = () -> Unit
typealias MessageHandler = (msg: Any, userData: Any?) -> Unit

// ─── Data classes ────────────────────────────────────────────────────────────

data class MeanCollisionData(
    val perpetratorId: LLUUID,
    val victimId: LLUUID,
    val time: Long,
    val magnitude: Float,
    val type: Int,
)

val gMeanCollisionList: MutableList<MeanCollisionData> = mutableListOf()

// ─── OfferInfo ───────────────────────────────────────────────────────────────

class OfferInfo {
    companion object {
        var responderType: String = "LLOfferInfo"

        // Respond-function map: mirrors LLOfferInfo::initRespondFunctionMap()
        // Keys are notification names; values delegate to the right callback.
        private val TASK_OFFER_NOTIFICATIONS = setOf("ObjectGiveItem", "OwnObjectGiveItem")
        private val AGENT_OFFER_NOTIFICATIONS = setOf("UserGiveItem", "UserGiveItemLegacy")
    }

    var im: Int = 0
    var fromId: LLUUID = LLUUID.NULL
    var fromGroup: Boolean = false
    var fromObject: Boolean = false
    var transactionId: LLUUID = LLUUID.NULL
    var folderId: LLUUID = LLUUID.NULL
    var objectId: LLUUID = LLUUID.NULL
    var assetType: Int = 0
    var fromName: String = ""
    var desc: String = ""
    var host: String = ""
    var persist: Boolean = false

    constructor()

    constructor(sd: Map<String, Any?>) {
        fromMap(sd)
    }

    constructor(other: OfferInfo) {
        im = other.im
        fromId = other.fromId
        fromGroup = other.fromGroup
        fromObject = other.fromObject
        transactionId = other.transactionId
        folderId = other.folderId
        objectId = other.objectId
        assetType = other.assetType
        fromName = other.fromName
        desc = other.desc
        host = other.host
        persist = other.persist
    }

    /**
     * Force a specific response, mirroring LLOfferInfo::forceResponse().
     *
     * Maps the [InventoryOfferResponse] to an accept (im+1) or decline (im+2)
     * dialog code and sends an ImprovedInstantMessage UDP packet to [host].
     */
    fun forceResponse(response: InventoryOfferResponse) {
        val accept = when (response) {
            InventoryOfferResponse.ACCEPT,
            InventoryOfferResponse.ACCEPT_SILENT,
            InventoryOfferResponse.SHOW,
            InventoryOfferResponse.SHOW_SILENT -> true
            else -> false
        }
        LOG.info("forceResponse: response=$response accept=$accept transactionId=$transactionId")
        sendReceiveResponse(accept, if (accept) folderId else LLUUID.NULL)
    }

    fun asMap(): Map<String, Any?> = mapOf(
        "im"            to im,
        "from_id"       to fromId.toString(),
        "from_group"    to fromGroup,
        "from_object"   to fromObject,
        "transaction_id" to transactionId.toString(),
        "folder_id"     to folderId.toString(),
        "object_id"     to objectId.toString(),
        "asset_type"    to assetType,
        "from_name"     to fromName,
        "desc"          to desc,
        "host"          to host,
        "persist"       to persist,
    )

    fun fromMap(params: Map<String, Any?>) {
        im = (params["im"] as? Int) ?: 0
        fromId = LLUUID.fromString(params["from_id"] as? String ?: "") ?: LLUUID.NULL
        fromGroup = (params["from_group"] as? Boolean) ?: false
        fromObject = (params["from_object"] as? Boolean) ?: false
        transactionId = LLUUID.fromString(params["transaction_id"] as? String ?: "") ?: LLUUID.NULL
        folderId = LLUUID.fromString(params["folder_id"] as? String ?: "") ?: LLUUID.NULL
        objectId = LLUUID.fromString(params["object_id"] as? String ?: "") ?: LLUUID.NULL
        assetType = (params["asset_type"] as? Int) ?: 0
        fromName = (params["from_name"] as? String) ?: ""
        desc = (params["desc"] as? String) ?: ""
        host = (params["host"] as? String) ?: ""
        persist = (params["persist"] as? Boolean) ?: false
    }

    /**
     * Dispatch to the appropriate callback based on the notification name,
     * mirroring LLOfferInfo::handleRespond() / initRespondFunctionMap().
     *
     * - "ObjectGiveItem" / "OwnObjectGiveItem" → inventoryTaskOfferCallback
     * - "UserGiveItem"   / "UserGiveItemLegacy" → inventoryOfferCallback
     */
    fun handleRespond(notification: Map<String, Any?>, response: Map<String, Any?>) {
        val name = notification["name"] as? String ?: ""
        when {
            name in TASK_OFFER_NOTIFICATIONS  -> inventoryTaskOfferCallback(notification, response)
            name in AGENT_OFFER_NOTIFICATIONS -> inventoryOfferCallback(notification, response)
            else -> LOG.warning("handleRespond: unexpected notification name '$name'")
        }
    }

    /**
     * Accept the offer into [folderId], mirroring send_auto_receive_response()
     * which calls sendReceiveResponse(true, mFolderID).
     */
    fun sendAutoReceiveResponse() {
        LOG.info("sendAutoReceiveResponse: accepting into folderId=$folderId")
        sendReceiveResponse(accept = true, destinationFolderId = folderId)
    }

    /**
     * Decline the offer (send to trash), mirroring send_decline_response().
     * Packs and sends an ImprovedInstantMessage with dialog = im+2.
     */
    fun sendDeclineResponse() {
        LOG.info("sendDeclineResponse: declining offer from $fromId host=$host")
        // dialog for decline = im + 2  (mirrors the C++ constant math)
        val dialogCode = (im + 2).toByte()
        val buf = newUdpBuffer()
        packImprovedInstantMessage(
            buf = buf,
            agentId = AgentState.agentId,
            sessionId = AgentState.sessionId,
            toId = fromId,
            fromAgentName = "Agent",
            message = "",
            offline = IM_ONLINE.toByte(),
            dialog = dialogCode,
            id = transactionId,
            binaryBucket = ByteArray(0),
        )
        sendUdp(buf, host)
        // Invalidate transaction id so it cannot be used again
        transactionId = LLUUID.NULL
    }

    /**
     * Handle an inventory offer response from a notification dialog.
     * Mirrors LLOfferInfo::inventory_offer_callback().
     *
     * Returns false (mirrors C++ bool return convention).
     */
    fun inventoryOfferCallback(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
        val button = (response["button"] as? String)?.let {
            runCatching { InventoryOfferResponse.valueOf(it) }.getOrNull()
        } ?: InventoryOfferResponse.DECLINE

        LOG.info("inventoryOfferCallback: button=$button im=$im fromId=$fromId")

        when (button) {
            InventoryOfferResponse.SHOW,
            InventoryOfferResponse.SHOW_SILENT -> {
                // Open the item: start an inventory fetch observer equivalent
                val offer = OpenAgentOffer(objectId, fromName, isManuallyAccepted = true)
                offer.startFetch()
                // accept silently so the server records the acceptance
                sendReceiveResponse(accept = true, destinationFolderId = folderId)
            }
            InventoryOfferResponse.ACCEPT,
            InventoryOfferResponse.ACCEPT_SILENT -> {
                // Accept to the proper folder
                Notifications.add("InvOfferGaveYou", mapOf("NAME" to fromName, "DESC" to desc))
                if (im == IM_GROUP_NOTICE || im == IM_GROUP_NOTICE_REQUESTED) {
                    sendReceiveResponse(accept = true, destinationFolderId = folderId)
                } else {
                    sendReceiveResponse(accept = true, destinationFolderId = folderId)
                }
            }
            InventoryOfferResponse.MUTE -> {
                // Mute the sender, then fall through to decline
                LOG.info("inventoryOfferCallback: muting sender $fromId")
                sendReceiveResponse(accept = false, destinationFolderId = LLUUID.NULL)
            }
            InventoryOfferResponse.DECLINE,
            InventoryOfferResponse.DECLINE_SILENT -> {
                Notifications.add("InvOfferDecline", mapOf("NAME" to fromName, "DESC" to desc))
                sendReceiveResponse(accept = false, destinationFolderId = LLUUID.NULL)
            }
        }
        return false
    }

    /**
     * Handle an object/task inventory offer response.
     * Mirrors LLOfferInfo::inventory_task_offer_callback().
     *
     * Returns false (mirrors C++ bool return convention).
     */
    fun inventoryTaskOfferCallback(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
        val button = (response["button"] as? Int) ?: 1   // 0=accept, 1=decline, 2=mute

        LOG.info("inventoryTaskOfferCallback: button=$button fromId=$fromId")

        val (accept, destination) = when (button) {
            0 -> {
                // Accept → proper folder
                if (checkOfferThrottle(fromName, checkOnly = true)) {
                    Notifications.add("InvOfferGaveYou", mapOf("NAME" to fromName, "DESC" to desc))
                }
                Pair(true, folderId)
            }
            2 -> {
                // Mute: add sender to mute list then decline
                LOG.info("inventoryTaskOfferCallback: muting $fromId")
                Pair(false, LLUUID.NULL)
            }
            else -> {
                // Decline (button==1) or close
                Notifications.add("InvOfferDecline", mapOf("NAME" to fromName, "DESC" to desc))
                Pair(false, LLUUID.NULL)
            }
        }
        sendReceiveResponse(accept, destination)
        return false
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Core accept/decline sender, mirroring LLOfferInfo::sendReceiveResponse().
     * Builds an ImprovedInstantMessage UDP packet with dialog = im+1 (accept)
     * or im+2 (decline) and sends it to [host].
     */
    private fun sendReceiveResponse(accept: Boolean, destinationFolderId: LLUUID) {
        if (transactionId == LLUUID.NULL) {
            LOG.warning("sendReceiveResponse: missing transactionId, skipping send")
            return
        }
        // Accept dialog = im+1, decline dialog = im+2  (mirrors C++ comment)
        val effectiveIm = if (im == IM_GROUP_NOTICE_REQUESTED) IM_GROUP_NOTICE else im
        val dialogCode = (effectiveIm + if (accept) 1 else 2).toByte()

        val bucket: ByteArray = if (accept) {
            // Binary bucket carries the destination folder UUID (16 bytes)
            destinationFolderId.asBytes()
        } else {
            ByteArray(0)
        }

        val buf = newUdpBuffer()
        packImprovedInstantMessage(
            buf = buf,
            agentId = AgentState.agentId,
            sessionId = AgentState.sessionId,
            toId = fromId,
            fromAgentName = "Agent",
            message = "",
            offline = IM_ONLINE.toByte(),
            dialog = dialogCode,
            id = transactionId,
            binaryBucket = bucket,
        )
        sendUdp(buf, host)
        // Invalidate so the transaction can only fire once
        transactionId = LLUUID.NULL
    }
}

// ─── OpenAgentOffer ──────────────────────────────────────────────────────────

/**
 * Fetch inventory items offered by an agent, then open them.
 * Mirrors LLOpenAgentOffer (LLInventoryFetchItemsObserver).
 *
 * In the JVM port the "inventory fetch" is represented by a simple list of
 * completed IDs that grows as items arrive.  A real implementation would hook
 * into an inventory-model observer.
 */
class OpenAgentOffer(
    private val objectId: LLUUID,
    private val fromName: String,
    private val isManuallyAccepted: Boolean,
) {
    private val completeIds: MutableList<LLUUID> = mutableListOf()

    /**
     * Start fetching.  Categories are added to the complete list immediately;
     * individual items require an async fetch (simplified here as a direct add).
     * Mirrors LLOpenAgentOffer::startFetch().
     */
    fun startFetch() {
        LOG.info("OpenAgentOffer.startFetch: objectId=$objectId")
        // Simulate: if item is already in inventory (category shortcut), mark complete
        completeIds.add(objectId)
        // A real implementation would call inventory model fetch and wait for done()
        done()
    }

    /**
     * Called when all items have arrived.
     * Mirrors LLOpenAgentOffer::done(): calls open_inventory_offer then removes
     * the observer.
     */
    fun done() {
        LOG.info("OpenAgentOffer.done: opening ${completeIds.size} item(s) for '$fromName'")
        openInventoryOffer(completeIds, fromName, isManuallyAccepted)
    }
}

// ─── ViewerMessage singleton ─────────────────────────────────────────────────

object ViewerMessage {

    private val handlers: MutableMap<String, MessageHandler> = mutableMapOf()

    private val teleportStartedCallbacks: MutableList<TeleportStartedCallback> = mutableListOf()

    val teleportStartedSignal: MutableList<TeleportStartedCallback> = teleportStartedCallbacks

    fun setTeleportStartedCallback(cb: TeleportStartedCallback): AutoCloseable {
        teleportStartedCallbacks.add(cb)
        return AutoCloseable { teleportStartedCallbacks.remove(cb) }
    }

    private fun fireTeleportStarted() {
        teleportStartedCallbacks.toList().forEach { it() }
    }

    fun register(messageName: String, handler: MessageHandler) {
        handlers[messageName] = handler
    }

    fun dispatch(messageName: String, msg: Any, userData: Any? = null) {
        handlers[messageName]?.invoke(msg, userData)
    }
}

// ─── UDP packet packers ───────────────────────────────────────────────────────
// Each packer writes the logical fields of an LLUDP message body into a
// ByteBuffer.  Only the fields that matter for the JVM port are encoded;
// a full implementation would follow the Second Life protocol specification.

/** Write a LLUUID (16 bytes) into [buf]. */
private fun ByteBuffer.putUUID(uuid: LLUUID) {
    put(uuid.asBytes())
}

/**
 * Pack an ImprovedInstantMessage body into [buf].
 * Mirrors gMessageSystem calls in send_improved_im() / sendReceiveResponse().
 */
private fun packImprovedInstantMessage(
    buf: ByteBuffer,
    agentId: LLUUID,
    sessionId: LLUUID,
    toId: LLUUID,
    fromAgentName: String,
    message: String,
    offline: Byte,
    dialog: Byte,
    id: LLUUID,
    timestamp: UInt = NO_TIMESTAMP,
    parentEstateId: Int = 0,
    regionId: LLUUID = LLUUID.NULL,
    posX: Float = AgentState.positionX,
    posY: Float = AgentState.positionY,
    posZ: Float = AgentState.positionZ,
    binaryBucket: ByteArray = ByteArray(0),
) {
    // AgentData block
    buf.putUUID(agentId)
    buf.putUUID(sessionId)
    // MessageBlock
    buf.put(0)                              // FromGroup = false
    buf.putUUID(toId)
    buf.put(offline)
    buf.put(dialog)
    buf.putUUID(id)
    buf.putInt(timestamp.toInt())
    val nameBytes = fromAgentName.toByteArray(Charsets.UTF_8)
    buf.putShort(nameBytes.size.toShort())
    buf.put(nameBytes)
    val msgBytes = message.toByteArray(Charsets.UTF_8)
    buf.putShort(msgBytes.size.toShort())
    buf.put(msgBytes)
    buf.putInt(parentEstateId)
    buf.putUUID(regionId)
    buf.putFloat(posX)
    buf.putFloat(posY)
    buf.putFloat(posZ)
    buf.putShort(binaryBucket.size.toShort())
    buf.put(binaryBucket)
}

// ─── Offer throttle ───────────────────────────────────────────────────────────

private var offerThrottleWindowStart = System.currentTimeMillis()
private var offerThrottleCount = 0
private const val OFFER_THROTTLE_MAX_COUNT = 5
private const val OFFER_THROTTLE_WINDOW_MS = 10_000L

/**
 * Return true if we are within the throttle budget, false if we are throttled.
 * Mirrors check_offer_throttle().
 */
private fun checkOfferThrottle(fromName: String, checkOnly: Boolean): Boolean {
    val now = System.currentTimeMillis()
    if (now - offerThrottleWindowStart >= OFFER_THROTTLE_WINDOW_MS) {
        offerThrottleWindowStart = now
        offerThrottleCount = 0
    }
    if (checkOnly) return offerThrottleCount < OFFER_THROTTLE_MAX_COUNT
    if (offerThrottleCount >= OFFER_THROTTLE_MAX_COUNT) {
        LOG.info("checkOfferThrottle: throttled – too many items from '$fromName'")
        return false
    }
    offerThrottleCount++
    return true
}

// ─── Top-level functions ─────────────────────────────────────────────────────

/**
 * Return true if the agent can afford [cost] L$.
 * Mirrors can_afford_transaction().
 */
fun canAffordTransaction(cost: Int): Boolean =
    AgentState.balance >= cost

/**
 * Send a MoneyTransferRequest UDP packet to the current region.
 * Mirrors give_money().
 */
fun giveMoney(
    uuid: LLUUID,
    region: Any?,
    amount: Int,
    isGroup: Boolean = false,
    trxType: Int = 5000,
    desc: String = "",
) {
    if (amount == 0 || region == null) return
    val absAmount = kotlin.math.abs(amount)
    if (!canAffordTransaction(absAmount)) {
        Notifications.add("CannotAffordTransaction", mapOf("AMOUNT" to absAmount.toString()))
        return
    }
    LOG.info("giveMoney: uuid=$uuid amount=$absAmount isGroup=$isGroup trxType=$trxType")
    val buf = newUdpBuffer()
    // AgentData block
    buf.putUUID(AgentState.agentId)
    buf.putUUID(AgentState.sessionId)
    // MoneyData block
    buf.putUUID(AgentState.agentId)   // SourceID
    buf.putUUID(uuid)                  // DestID
    buf.put(if (isGroup) 1 else 0)     // Flags (pack_transaction_flags simplified)
    buf.putInt(absAmount)
    buf.put(0)                         // AggregatePermNextOwner
    buf.put(0)                         // AggregatePermInventory
    buf.putInt(trxType)
    val descBytes = desc.toByteArray(Charsets.UTF_8)
    buf.putShort(descBytes.size.toShort())
    buf.put(descBytes)
    sendUdp(buf, AgentState.regionHost, AgentState.regionPort)
}

/**
 * Reply to a group-join invitation (accept or decline).
 * Mirrors send_join_group_response().
 *
 * When [useOfflineCap] is true and a capability URL is available an HTTP POST
 * would be made; here we fall through to the IM-based path as a skeleton.
 */
fun sendJoinGroupResponse(
    groupId: LLUUID,
    transactionId: LLUUID,
    acceptInvite: Boolean,
    fee: Int,
    useOfflineCap: Boolean,
) {
    LOG.info("sendJoinGroupResponse: groupId=$groupId accept=$acceptInvite fee=$fee")
    if (acceptInvite && fee > 0) {
        // Prompt user about the fee before proceeding
        Notifications.add("JoinGroupCanAfford", mapOf("COST" to fee.toString()))
        return
    }
    val imType = if (acceptInvite) IM_GROUP_INVITATION_ACCEPT else IM_GROUP_INVITATION_DECLINE
    sendImprovedIm(
        toId = groupId,
        name = "Agent",
        message = "",
        offline = IM_ONLINE.toUByte(),
        dialog = imType,
        id = transactionId,
    )
}

/**
 * Handle a LogoutReply from the simulator.
 * Mirrors process_logout_reply(): validates agent/session IDs then initiates
 * viewer shutdown.
 */
fun processLogoutReply(msg: Any, userData: Any?) {
    LOG.info("processLogoutReply: server confirmed logout, initiating shutdown")
    // In the real viewer: parse AgentID/SessionID from msg, call forceQuit().
    // Here we fire a notification and let the app-level shutdown hook handle it.
    Notifications.add("LogoutConfirmed")
}

/**
 * Decode terrain/wind/cloud layer data from a LayerData message.
 * Mirrors process_layer_data(): reads a type byte and binary blob, forwards to
 * a layer-manager equivalent.
 */
fun processLayerData(msg: Any, userData: Any?) {
    // Expected msg fields: layerType (Byte), layerData (ByteArray)
    val fields = msg as? Map<*, *> ?: return
    val type = (fields["layerType"] as? Byte) ?: return
    val data = (fields["layerData"] as? ByteArray) ?: return
    if (data.isEmpty()) {
        LOG.warning("processLayerData: zero-size layer data, ignoring")
        return
    }
    LOG.info("processLayerData: type=$type size=${data.size}")
    // Forward to VL layer manager (not yet implemented)
    // VLManager.addLayerData(type, data)
}

/**
 * Handle a DeRezAck: decrement the viewer's busy-cursor reference count.
 * Mirrors process_derez_ack().
 */
fun processDerezAck(msg: Any, userData: Any?) {
    LOG.fine("processDerezAck: decrement busy count")
    // ViewerWindow.decBusyCount()
}

/**
 * Populate places/land-holdings search results from a PlacesReply message.
 * Mirrors process_places_reply(): dispatches to land-holdings or group-land panel.
 */
fun processPlacesReply(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val queryId = LLUUID.fromString(fields["queryId"] as? String ?: "") ?: LLUUID.NULL
    LOG.info("processPlacesReply: queryId=$queryId")
    when {
        queryId == LLUUID.NULL -> {
            // → LLFloaterLandHoldings
            LOG.info("processPlacesReply: routing to LandHoldings panel")
        }
        else -> {
            // → LLPanelGroupLandMoney
            LOG.info("processPlacesReply: routing to GroupLandMoney panel, groupId=$queryId")
        }
    }
}

/**
 * Send a SoundTrigger UDP message to the current region.
 * Mirrors send_sound_trigger().
 */
fun sendSoundTrigger(soundId: LLUUID, gain: Float) {
    if (soundId == LLUUID.NULL) {
        LOG.fine("sendSoundTrigger: null soundId, skipping")
        return
    }
    LOG.info("sendSoundTrigger: soundId=$soundId gain=$gain")
    val buf = newUdpBuffer()
    // SoundData block
    buf.putUUID(soundId)
    buf.putUUID(LLUUID.NULL)   // OwnerID  (set server-side)
    buf.putUUID(LLUUID.NULL)   // ObjectID (set server-side)
    buf.putUUID(LLUUID.NULL)   // ParentID (set server-side)
    buf.putLong(0L)            // RegionHandle (placeholder)
    buf.putFloat(AgentState.positionX)
    buf.putFloat(AgentState.positionY)
    buf.putFloat(AgentState.positionZ)
    buf.putFloat(gain)
    sendUdp(buf, AgentState.regionHost, AgentState.regionPort)
}

/**
 * Decode and dispatch an ImprovedInstantMessage.
 * Mirrors process_improved_im(): reads IM fields from [msg] and routes to the
 * appropriate handler (chat, inventory offer, lure, etc.) via [ViewerMessage.dispatch].
 */
fun processImprovedIm(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val imType = (fields["dialog"] as? Int) ?: IM_NOTHING_SPECIAL
    val fromId = LLUUID.fromString(fields["fromId"] as? String ?: "") ?: LLUUID.NULL
    val message = fields["message"] as? String ?: ""
    LOG.info("processImprovedIm: from=$fromId dialog=$imType")
    when (imType) {
        IM_INVENTORY_OFFERED -> {
            LOG.info("processImprovedIm: inventory offer from $fromId")
            ViewerMessage.dispatch("InventoryOffered", msg, userData)
        }
        IM_TASK_INVENTORY_OFFERED -> {
            LOG.info("processImprovedIm: task inventory offer from $fromId")
            ViewerMessage.dispatch("TaskInventoryOffered", msg, userData)
        }
        IM_GROUP_NOTICE,
        IM_GROUP_NOTICE_REQUESTED -> {
            LOG.info("processImprovedIm: group notice from $fromId")
            ViewerMessage.dispatch("GroupNotice", msg, userData)
        }
        IM_LURE_USER -> {
            LOG.info("processImprovedIm: teleport lure from $fromId")
            Notifications.add("TeleportOffered", mapOf("FROM" to fromId.toString()))
        }
        IM_DO_NOT_DISTURB_AUTO_RESPONSE -> {
            LOG.info("processImprovedIm: DND auto-response from $fromId: $message")
        }
        else -> {
            // Ordinary text IM → IM session
            LOG.info("processImprovedIm: text IM dialog=$imType from=$fromId: $message")
            ViewerMessage.dispatch("ImprovedInstantMessage", msg, userData)
        }
    }
}

/**
 * Show an LSL script-permission request dialog.
 * Mirrors process_script_question().
 */
fun processScriptQuestion(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val taskId = fields["taskId"] as? String ?: ""
    val questions = (fields["questions"] as? Int) ?: 0
    LOG.info("processScriptQuestion: taskId=$taskId permissions=0x${questions.toString(16)}")
    Notifications.add("ScriptQuestion", mapOf("TASK_ID" to taskId, "QUESTIONS" to questions.toString()))
}

/**
 * Decode chat from the simulator and post to the nearby-chat panel.
 * Mirrors process_chat_from_simulator(): reads source type, chat type, and
 * message text, then routes to the chat display system.
 */
fun processChatFromSimulator(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val fromName = fields["fromName"] as? String ?: "unknown"
    val chatType = (fields["chatType"] as? Int) ?: 1   // 1 = NORMAL
    val message  = fields["message"] as? String ?: ""
    LOG.info("processChatFromSimulator: [$chatType] $fromName: $message")
    // Post to nearby-chat panel
    Notifications.add("NearbyChat", mapOf("FROM" to fromName, "TYPE" to chatType.toString(), "MSG" to message))
}

/**
 * Pack and send an AgentUpdate UDP packet.
 * Mirrors send_agent_update(): packs agent position/orientation/camera state and
 * sends to the current region.
 */
fun sendAgentUpdate(forceSend: Boolean, sendReliable: Boolean = false) {
    LOG.fine("sendAgentUpdate: forceSend=$forceSend reliable=$sendReliable")
    val buf = newUdpBuffer()
    // AgentData block (simplified)
    buf.putUUID(AgentState.agentId)
    buf.putUUID(AgentState.sessionId)
    // BodyRotation, HeadRotation as identity quaternion placeholders (4 floats each)
    repeat(8) { buf.putFloat(0f) }
    buf.put(0)   // AgentState flags
    // Camera fields (3 Vec3 + near clip)
    repeat(10) { buf.putFloat(0f) }
    buf.putInt(0)  // ControlFlags
    buf.put(0)     // Flags byte (AU_FLAGS_NONE)
    sendUdp(buf, AgentState.regionHost, AgentState.regionPort)
}

/**
 * Decode a full ObjectUpdate message.
 * Mirrors process_object_update(): extracts object data and updates the
 * viewer's object list.
 */
fun processObjectUpdate(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = fields["objectId"] as? String ?: return
    LOG.info("processObjectUpdate: objectId=$objectId")
    ViewerMessage.dispatch("ObjectUpdate", msg, userData)
}

/**
 * Decode a CompressedObjectUpdate.
 * Mirrors process_compressed_object_update().
 */
fun processCompressedObjectUpdate(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val data = fields["data"] as? ByteArray ?: return
    val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    LOG.info("processCompressedObjectUpdate: payload size=${data.size}")
    ViewerMessage.dispatch("CompressedObjectUpdate", msg, userData)
}

/**
 * Apply cached object data.
 * Mirrors process_cached_object_update().
 */
fun processCachedObjectUpdate(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val count = (fields["count"] as? Int) ?: 0
    LOG.info("processCachedObjectUpdate: $count cached blocks")
    ViewerMessage.dispatch("CachedObjectUpdate", msg, userData)
}

/**
 * Decode a terse object update (position/velocity/rotation).
 * Mirrors process_terse_object_update_improved().
 */
fun processTerseObjectUpdateImproved(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val data = fields["data"] as? ByteArray ?: return
    val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    // Read localId (U32) as the first field
    if (buf.remaining() >= 4) {
        val localId = buf.int
        LOG.fine("processTerseObjectUpdateImproved: localId=$localId")
    }
    ViewerMessage.dispatch("TerseObjectUpdate", msg, userData)
}

/**
 * Broadcast object properties to area-search and other subscribers.
 * Mirrors process_object_properties().
 */
fun processObjectProperties(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = fields["objectId"] as? String ?: ""
    LOG.fine("processObjectProperties: objectId=$objectId")
    // Broadcast to all registered listeners (area search, selection manager, etc.)
    ViewerMessage.dispatch("ObjectProperties", msg, userData)
}

/**
 * Anti-spam-guarded object properties family handler.
 * Mirrors process_object_properties_family().
 */
fun processObjectPropertiesFamily(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = fields["objectId"] as? String ?: ""
    // Apply request-per-second throttle before dispatching
    if (checkOfferThrottle("ObjectPropertiesFamily", checkOnly = false)) {
        LOG.fine("processObjectPropertiesFamily: objectId=$objectId")
        ViewerMessage.dispatch("ObjectPropertiesFamily", msg, userData)
    }
}

/**
 * Send an AgentThrottle UDP message to [host].
 * Mirrors send_simulator_throttle_settings().
 */
fun sendSimulatorThrottleSettings(host: String) {
    LOG.info("sendSimulatorThrottleSettings: host=$host")
    val buf = newUdpBuffer()
    buf.putUUID(AgentState.agentId)
    buf.putUUID(AgentState.sessionId)
    buf.putInt(0)   // CircuitCode placeholder
    // Throttle values (7 × F32): resend, land, wind, cloud, task, texture, asset
    val defaults = floatArrayOf(10240f, 17408f, 17408f, 17408f, 40960f, 57344f, 40960f)
    defaults.forEach { buf.putFloat(it) }
    sendUdp(buf, host)
}

/**
 * Remove killed object(s) from the viewer object list.
 * Mirrors process_kill_object().
 */
fun processKillObject(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    @Suppress("UNCHECKED_CAST")
    val localIds = fields["localIds"] as? List<Int> ?: emptyList()
    LOG.info("processKillObject: removing ${localIds.size} object(s): $localIds")
    ViewerMessage.dispatch("KillObject", msg, userData)
}

/**
 * Synchronise the simulator clock.
 * Mirrors process_time_synch(): reads SecPerDay/SecPerYear/SunPhase and applies
 * to the world environment.
 */
fun processTimeSynch(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val sunPhase = (fields["sunPhase"] as? Float) ?: 0f
    val localTime = System.currentTimeMillis()
    LOG.fine("processTimeSynch: sunPhase=$sunPhase localTime=$localTime")
    // WorldEnvironment.setSunPhase(sunPhase)
}

/**
 * Play a triggered sound at a world position.
 * Mirrors process_sound_trigger().
 */
fun processSoundTrigger(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val soundId = LLUUID.fromString(fields["soundId"] as? String ?: "") ?: LLUUID.NULL
    val gain = (fields["gain"] as? Float) ?: 1f
    LOG.fine("processSoundTrigger: soundId=$soundId gain=$gain")
    // AudioEngine.playTrigger(soundId, gain)
}

/**
 * Preload a sound asset into the local cache.
 * Mirrors process_preload_sound().
 */
fun processPreloadSound(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val soundId = LLUUID.fromString(fields["soundId"] as? String ?: "") ?: LLUUID.NULL
    LOG.fine("processPreloadSound: soundId=$soundId")
    // AssetCache.prefetch(soundId, AssetType.SOUND)
}

/**
 * Attach a sound to an object and start playback.
 * Mirrors process_attached_sound().
 */
fun processAttachedSound(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val soundId  = LLUUID.fromString(fields["soundId"] as? String ?: "") ?: LLUUID.NULL
    val objectId = LLUUID.fromString(fields["objectId"] as? String ?: "") ?: LLUUID.NULL
    val gain = (fields["gain"] as? Float) ?: 1f
    LOG.fine("processAttachedSound: soundId=$soundId objectId=$objectId gain=$gain")
    // AudioEngine.attachSound(objectId, soundId, gain)
}

/**
 * Update the gain on an attached sound source.
 * Mirrors process_attached_sound_gain_change().
 */
fun processAttachedSoundGainChange(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = LLUUID.fromString(fields["objectId"] as? String ?: "") ?: LLUUID.NULL
    val gain = (fields["gain"] as? Float) ?: 1f
    LOG.fine("processAttachedSoundGainChange: objectId=$objectId gain=$gain")
    // AudioEngine.setGain(objectId, gain)
}

/**
 * Update energy/physics statistics display.
 * Mirrors process_energy_statistics().
 */
fun processEnergyStatistics(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val energy = (fields["energy"] as? Float) ?: 0f
    LOG.fine("processEnergyStatistics: energy=$energy")
    // StatsDisplay.updateEnergy(energy)
}

/**
 * Update the agent health display.
 * Mirrors process_health_message().
 */
fun processHealthMessage(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val health = (fields["health"] as? Float) ?: 1f
    LOG.fine("processHealthMessage: health=$health")
    // StatusBar.setHealth(health)
}

/**
 * Decode and apply a SimStats packet.
 * Mirrors process_sim_stats(): reads ~25 stat fields and applies them to the
 * statistics display.
 */
fun processSimStats(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val stats = fields["stats"] as? Map<*, *> ?: emptyMap<Any, Any>()
    LOG.fine("processSimStats: ${stats.size} stat entries")
    // StatsDisplay.applySimStats(stats)
}

/**
 * Handle a combat-damage notification.
 * Mirrors process_shooter_agent_hit().
 */
fun processShooterAgentHit(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val shooterId = LLUUID.fromString(fields["shooterId"] as? String ?: "") ?: LLUUID.NULL
    LOG.info("processShooterAgentHit: shooter=$shooterId")
    // AvatarModel.applyDamage(shooterId)
}

/**
 * Start or stop animations on a target avatar.
 * Mirrors process_avatar_animation().
 */
fun processAvatarAnimation(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val agentId = LLUUID.fromString(fields["agentId"] as? String ?: "") ?: LLUUID.NULL
    @Suppress("UNCHECKED_CAST")
    val animations = fields["animations"] as? List<Map<String, Any>> ?: emptyList()
    LOG.fine("processAvatarAnimation: agentId=$agentId count=${animations.size}")
    ViewerMessage.dispatch("AvatarAnimation", msg, userData)
}

/**
 * Start or stop object-level animations.
 * Mirrors process_object_animation().
 */
fun processObjectAnimation(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = LLUUID.fromString(fields["objectId"] as? String ?: "") ?: LLUUID.NULL
    LOG.fine("processObjectAnimation: objectId=$objectId")
    ViewerMessage.dispatch("ObjectAnimation", msg, userData)
}

/**
 * Apply baked texture/wearable data to an avatar.
 * Mirrors process_avatar_appearance().
 */
fun processAvatarAppearance(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val agentId = LLUUID.fromString(fields["agentId"] as? String ?: "") ?: LLUUID.NULL
    LOG.info("processAvatarAppearance: agentId=$agentId")
    ViewerMessage.dispatch("AvatarAppearance", msg, userData)
}

/**
 * Apply a camera constraint from the parcel or region.
 * Mirrors process_camera_constraint().
 */
fun processCameraConstraint(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val cameraPlane = fields["cameraPlane"] as? FloatArray ?: FloatArray(4)
    LOG.fine("processCameraConstraint: plane=${cameraPlane.toList()}")
    // AgentCamera.setConstraint(cameraPlane)
}

/**
 * Position the avatar in a sit pose on an object.
 * Mirrors process_avatar_sit_response().
 */
fun processAvatarSitResponse(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val seatId = LLUUID.fromString(fields["seatId"] as? String ?: "") ?: LLUUID.NULL
    val autoSit = (fields["autoSit"] as? Boolean) ?: false
    LOG.info("processAvatarSitResponse: seatId=$seatId autoSit=$autoSit")
    ViewerMessage.dispatch("AvatarSitResponse", msg, userData)
}

/**
 * Apply LSL follow-cam property overrides.
 * Mirrors process_set_follow_cam_properties().
 */
fun processSetFollowCamProperties(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = LLUUID.fromString(fields["objectId"] as? String ?: "") ?: LLUUID.NULL
    @Suppress("UNCHECKED_CAST")
    val params = fields["params"] as? Map<String, Float> ?: emptyMap()
    LOG.fine("processSetFollowCamProperties: objectId=$objectId params=${params.keys}")
    // AgentCamera.applyFollowCamParams(objectId, params)
}

/**
 * Clear LSL follow-cam overrides and restore defaults.
 * Mirrors process_clear_follow_cam_properties().
 */
fun processClearFollowCamProperties(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = LLUUID.fromString(fields["objectId"] as? String ?: "") ?: LLUUID.NULL
    LOG.fine("processClearFollowCamProperties: objectId=$objectId")
    // AgentCamera.clearFollowCamParams(objectId)
}

/**
 * Add or update name-value pairs on an object.
 * Mirrors process_name_value().
 */
fun processNameValue(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = LLUUID.fromString(fields["objectId"] as? String ?: "") ?: LLUUID.NULL
    @Suppress("UNCHECKED_CAST")
    val nameValues = fields["nameValues"] as? Map<String, String> ?: emptyMap()
    LOG.fine("processNameValue: objectId=$objectId nvCount=${nameValues.size}")
    ViewerMessage.dispatch("NameValue", msg, userData)
}

/**
 * Remove name-value pairs from an object.
 * Mirrors process_remove_name_value().
 */
fun processRemoveNameValue(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectId = LLUUID.fromString(fields["objectId"] as? String ?: "") ?: LLUUID.NULL
    LOG.fine("processRemoveNameValue: objectId=$objectId")
    ViewerMessage.dispatch("RemoveNameValue", msg, userData)
}

/**
 * Show a "kicked from region" notification and disconnect.
 * Mirrors process_kick_user().
 */
fun processKickUser(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val reason = fields["reason"] as? String ?: ""
    LOG.warning("processKickUser: kicked – reason='$reason'")
    Notifications.addModal("You have been disconnected from the region: $reason")
    // Initiate logout / circuit teardown
    ViewerMessage.dispatch("KickUser", msg, userData)
}

/**
 * Update economy data (upload cost, group fee, etc.).
 * Mirrors process_economy_data().
 */
fun processEconomyData(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectCount = (fields["objectCount"] as? Int) ?: 0
    val priceUpload = (fields["priceUpload"] as? Int) ?: 0
    LOG.info("processEconomyData: objectCount=$objectCount priceUpload=$priceUpload")
    // EconomyModel.update(fields)
}

/**
 * Update the displayed L$ balance and show a transaction toast.
 * Mirrors process_money_balance_reply().
 */
fun processMoneyBalanceReply(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val balance = (fields["balance"] as? Int) ?: 0
    val desc    = fields["description"] as? String ?: ""
    LOG.info("processMoneyBalanceReply: newBalance=$balance desc='$desc'")
    AgentState.balance = balance
    if (desc.isNotEmpty()) {
        Notifications.add("MoneyBalanceReply", mapOf("BALANCE" to balance.toString(), "DESC" to desc))
    }
}

/**
 * Adjust the local L$ balance display.
 * Mirrors process_adjust_balance().
 */
fun processAdjustBalance(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val delta = (fields["delta"] as? Int) ?: 0
    AgentState.balance += delta
    LOG.info("processAdjustBalance: delta=$delta newBalance=${AgentState.balance}")
}

/**
 * Attempt to show a standard notification for [msg].
 * Mirrors attempt_standard_notification(): returns true if [msg] mapped to a
 * known notification name and was shown.
 */
fun attemptStandardNotification(msg: Any): Boolean {
    val fields = msg as? Map<*, *> ?: return false
    val notifName = fields["notificationId"] as? String ?: return false
    val args = @Suppress("UNCHECKED_CAST")
    (fields["args"] as? Map<String, String>) ?: emptyMap()
    Notifications.add(notifName, args)
    LOG.info("attemptStandardNotification: showed '$notifName'")
    return true
}

/**
 * Display a modal or non-modal alert from the simulator.
 * Mirrors process_alert_message().
 */
fun processAlertMessage(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val message = fields["message"] as? String ?: ""
    val modal   = (fields["modal"] as? Boolean) ?: false
    processAlertCore(message, modal)
}

/**
 * Display an agent-targeted alert.
 * Mirrors process_agent_alert_message().
 */
fun processAgentAlertMessage(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val message = fields["message"] as? String ?: ""
    val modal   = (fields["modal"] as? Boolean) ?: false
    processAlertCore(message, modal)
}

/**
 * Route an alert message to the notification system.
 * Mirrors process_alert_core(): attempts a standard notification first, then
 * falls back to a SystemMessage notification.
 */
fun processAlertCore(message: String, modal: Boolean) {
    LOG.info("processAlertCore: modal=$modal message='$message'")
    if (!attemptStandardNotification(mapOf("notificationId" to message))) {
        if (modal) {
            Notifications.addModal(message)
        } else {
            Notifications.add("SystemMessage", mapOf("MESSAGE" to message))
        }
    }
}

/**
 * Record collision data and show a bump/push notification.
 * Mirrors process_mean_collision_alert_message().
 */
fun processMeanCollisionAlertMessage(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val perpetratorId = LLUUID.fromString(fields["perpetratorId"] as? String ?: "") ?: LLUUID.NULL
    val victimId      = LLUUID.fromString(fields["victimId"]      as? String ?: "") ?: LLUUID.NULL
    val time          = (fields["time"] as? Long) ?: System.currentTimeMillis()
    val magnitude     = (fields["magnitude"] as? Float) ?: 0f
    val type          = (fields["type"] as? Int) ?: 0

    LOG.info("processMeanCollisionAlertMessage: perp=$perpetratorId magnitude=$magnitude")
    val entry = MeanCollisionData(perpetratorId, victimId, time, magnitude, type)
    // De-duplicate by perpetratorId+victimId, then add
    gMeanCollisionList.removeIf { it.perpetratorId == perpetratorId && it.victimId == victimId }
    gMeanCollisionList.add(entry)
    Notifications.add("MeanCollision", mapOf("FROM" to perpetratorId.toString(), "MAGNITUDE" to magnitude.toString()))
}

/**
 * Show a frozen/unfrozen status indicator.
 * Mirrors process_frozen_message().
 */
fun processFrozenMessage(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val frozen = (fields["frozen"] as? Boolean) ?: false
    LOG.info("processFrozenMessage: frozen=$frozen")
    Notifications.add(if (frozen) "AgentFrozen" else "AgentUnfrozen")
}

/**
 * Handle a DerezContainer message.
 * Mirrors process_derez_container().
 */
fun processDerezContainer(msg: Any, userData: Any?) {
    LOG.info("processDerezContainer")
    ViewerMessage.dispatch("DerezContainer", msg, userData)
}

/**
 * Send a CompleteAgentMovement message to [simHost].
 * Mirrors send_complete_agent_movement(): tells the simulator the agent has
 * finished teleporting/crossing.
 */
fun sendCompleteAgentMovement(simHost: String) {
    LOG.info("sendCompleteAgentMovement: simHost=$simHost")
    val buf = newUdpBuffer()
    buf.putUUID(AgentState.agentId)
    buf.putUUID(AgentState.sessionId)
    buf.putInt(0)   // CircuitCode placeholder
    sendUdp(buf, simHost)
}

/**
 * Finalise agent position after TP or region crossing and enable the UI.
 * Mirrors process_agent_movement_complete().
 */
fun processAgentMovementComplete(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val posX = (fields["posX"] as? Float) ?: 0f
    val posY = (fields["posY"] as? Float) ?: 0f
    val posZ = (fields["posZ"] as? Float) ?: 0f
    LOG.info("processAgentMovementComplete: pos=($posX, $posY, $posZ)")
    AgentState.positionX = posX
    AgentState.positionY = posY
    AgentState.positionZ = posZ
    // Enable viewer UI, re-fetch sim stats, etc.
    ViewerMessage.dispatch("AgentMovementComplete", msg, userData)
}

/**
 * Switch to a new region circuit after a seamless border cross.
 * Mirrors process_crossed_region().
 */
fun processCrossedRegion(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val newHost = fields["simHost"] as? String ?: ""
    LOG.info("processCrossedRegion: newRegion=$newHost")
    AgentState.regionHost = newHost
    sendCompleteAgentMovement(newHost)
    ViewerMessage.dispatch("CrossedRegion", msg, userData)
}

/**
 * Display teleport progress UI and fire the teleportStartedSignal.
 * Mirrors process_teleport_start().
 */
fun processTeleportStart(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val flags = (fields["teleportFlags"] as? Int) ?: 0
    LOG.info("processTeleportStart: flags=0x${flags.toString(16)}")
    // Show progress UI
    Notifications.add("TeleportStarted", mapOf("FLAGS" to flags.toString()))
    // Fire the signal so any registered callbacks are notified
    ViewerMessage.teleportStartedSignal.toList().forEach { it() }
}

/**
 * Update the teleport progress bar text.
 * Mirrors process_teleport_progress().
 */
fun processTeleportProgress(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val message = fields["message"] as? String ?: ""
    LOG.info("processTeleportProgress: '$message'")
    Notifications.add("TeleportProgress", mapOf("MSG" to message))
}

/**
 * Show a teleport-failed notification and restore UI state.
 * Mirrors process_teleport_failed().
 */
fun processTeleportFailed(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val reason = fields["reason"] as? String ?: ""
    LOG.warning("processTeleportFailed: reason='$reason'")
    Notifications.add("TeleportFailed", mapOf("REASON" to reason))
    ViewerMessage.dispatch("TeleportFailed", msg, userData)
}

/**
 * Complete a teleport: switch regions and reinitialise objects.
 * Mirrors process_teleport_finish().
 */
fun processTeleportFinish(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val simHost = fields["simHost"] as? String ?: return
    LOG.info("processTeleportFinish: simHost=$simHost")
    AgentState.regionHost = simHost
    sendCompleteAgentMovement(simHost)
    ViewerMessage.dispatch("TeleportFinish", msg, userData)
}

/**
 * Handle an in-region teleport (position change without a region switch).
 * Mirrors process_teleport_local().
 */
fun processTeleportLocal(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val posX = (fields["posX"] as? Float) ?: AgentState.positionX
    val posY = (fields["posY"] as? Float) ?: AgentState.positionY
    val posZ = (fields["posZ"] as? Float) ?: AgentState.positionZ
    LOG.info("processTeleportLocal: newPos=($posX, $posY, $posZ)")
    AgentState.positionX = posX
    AgentState.positionY = posY
    AgentState.positionZ = posZ
    ViewerMessage.dispatch("TeleportLocal", msg, userData)
}

/**
 * Handle a UserSimLocationReply for world-map teleports.
 * Mirrors process_user_sim_location_reply().
 */
fun processUserSimLocationReply(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val queryId = LLUUID.fromString(fields["queryId"] as? String ?: "") ?: LLUUID.NULL
    val simHost = fields["simHost"] as? String ?: ""
    LOG.info("processUserSimLocationReply: queryId=$queryId simHost=$simHost")
    ViewerMessage.dispatch("UserSimLocationReply", msg, userData)
}

/**
 * Build and send a minimal ImprovedInstantMessage packet (simple variant).
 * Mirrors send_simple_im().
 */
fun sendSimpleIm(
    toId: LLUUID,
    message: String,
    dialog: Int = IM_NOTHING_SPECIAL,
    id: LLUUID = LLUUID.NULL,
) {
    sendImprovedIm(
        toId = toId,
        name = "Agent",
        message = message,
        offline = IM_ONLINE.toUByte(),
        dialog = dialog,
        id = id,
    )
}

/**
 * Send a GroupNoticeAdd message with an optional inventory attachment.
 * Mirrors send_group_notice().
 */
fun sendGroupNotice(
    groupId: LLUUID,
    subject: String,
    message: String,
    item: Any?,
) {
    LOG.info("sendGroupNotice: groupId=$groupId subject='$subject'")
    val buf = newUdpBuffer()
    buf.putUUID(AgentState.agentId)
    buf.putUUID(AgentState.sessionId)
    // GroupNoticeData block
    buf.putUUID(groupId)
    val subjectBytes = subject.toByteArray(Charsets.UTF_8)
    buf.putShort(subjectBytes.size.toShort())
    buf.put(subjectBytes)
    val msgBytes = message.toByteArray(Charsets.UTF_8)
    buf.putShort(msgBytes.size.toShort())
    buf.put(msgBytes)
    buf.put(if (item != null) 1 else 0) // HasAttachment flag
    sendUdp(buf, AgentState.regionHost, AgentState.regionPort)
}

/**
 * Send an auto-reply DND (do-not-disturb) IM.
 * Mirrors send_do_not_disturb_message().
 */
fun sendDoNotDisturbMessage(msg: Any, fromId: LLUUID, sessionId: LLUUID = LLUUID.NULL) {
    LOG.info("sendDoNotDisturbMessage: fromId=$fromId")
    sendSimpleIm(
        toId = fromId,
        message = "The Resident you messaged is in 'Do Not Disturb' mode.",
        dialog = IM_DO_NOT_DISTURB_AUTO_RESPONSE,
        id = sessionId,
    )
}

/**
 * Send an auto-reject teleport-offer IM.
 * Mirrors send_rejecting_tp_offers_message().
 */
fun sendRejectingTpOffersMessage(msg: Any, fromId: LLUUID, sessionId: LLUUID = LLUUID.NULL) {
    LOG.info("sendRejectingTpOffersMessage: fromId=$fromId")
    sendSimpleIm(
        toId = fromId,
        message = "This resident is not accepting teleport offers.",
        dialog = IM_LURE_DECLINED,
        id = sessionId,
    )
}

/**
 * Send an auto-reject friendship-request IM.
 * Mirrors send_rejecting_friendship_requests_message().
 */
fun sendRejectingFriendshipRequestsMessage(msg: Any, fromId: LLUUID, sessionId: LLUUID = LLUUID.NULL) {
    LOG.info("sendRejectingFriendshipRequestsMessage: fromId=$fromId")
    sendSimpleIm(
        toId = fromId,
        message = "This resident is not accepting friendship requests.",
        dialog = IM_NOTHING_SPECIAL,
        id = sessionId,
    )
}

/**
 * Send a teleport lure to a single avatar.
 * Mirrors handle_lure(const LLUUID&).
 */
fun handleLure(inviteeId: LLUUID) {
    LOG.info("handleLure: inviteeId=$inviteeId")
    sendSimpleIm(
        toId = inviteeId,
        message = "",
        dialog = IM_LURE_USER,
    )
}

/**
 * Send a teleport lure to multiple avatars.
 * Mirrors handle_lure(const uuid_vec_t&).
 */
fun handleLureMultiple(ids: List<LLUUID>) {
    LOG.info("handleLureMultiple: ${ids.size} invitees")
    ids.forEach { handleLure(it) }
}

/**
 * Pack and send a full ImprovedInstantMessage packet.
 * Mirrors send_improved_im(): assembles all fields into a UDP buffer and sends
 * it via the current region circuit.
 */
fun sendImprovedIm(
    toId: LLUUID,
    name: String,
    message: String,
    offline: UByte = 0u,
    dialog: Int = 0,
    id: LLUUID = LLUUID.NULL,
    timestamp: UInt = 0u,
    binaryBucket: ByteArray = ByteArray(0),
    binaryBucketSize: Int = 0,
) {
    LOG.info("sendImprovedIm: toId=$toId dialog=$dialog")
    val buf = newUdpBuffer()
    packImprovedInstantMessage(
        buf = buf,
        agentId = AgentState.agentId,
        sessionId = AgentState.sessionId,
        toId = toId,
        fromAgentName = name,
        message = message,
        offline = offline.toByte(),
        dialog = dialog.toByte(),
        id = id,
        timestamp = timestamp,
        binaryBucket = binaryBucket.take(binaryBucketSize).toByteArray(),
    )
    sendUdp(buf, AgentState.regionHost, AgentState.regionPort)
}

/**
 * Update account user-info fields from a UserInfoReply message.
 * Mirrors process_user_info_reply().
 */
fun processUserInfoReply(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val email = fields["email"] as? String ?: ""
    LOG.info("processUserInfoReply: email='$email'")
    // AccountModel.updateUserInfo(fields)
}

/**
 * Format an epoch-seconds timestamp as a human-readable local-time string.
 * Mirrors formatted_time() using java.time.
 */
fun formattedTime(epochSeconds: Long): String {
    val instant = Instant.ofEpochSecond(epochSeconds)
    val zdt = instant.atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").format(zdt)
}

/**
 * Send a PlacesQuery UDP message.
 * Mirrors send_places_query().
 */
fun sendPlacesQuery(
    queryId: LLUUID,
    transId: LLUUID,
    queryText: String,
    queryFlags: UInt,
    category: Int,
    simName: String,
) {
    LOG.info("sendPlacesQuery: queryId=$queryId text='$queryText' category=$category")
    val buf = newUdpBuffer()
    buf.putUUID(AgentState.agentId)
    buf.putUUID(AgentState.sessionId)
    buf.putUUID(queryId)
    buf.putUUID(transId)
    buf.putInt(queryFlags.toInt())
    buf.put(category.toByte())
    val simBytes = simName.toByteArray(Charsets.UTF_8)
    buf.putShort(simBytes.size.toShort())
    buf.put(simBytes)
    val textBytes = queryText.toByteArray(Charsets.UTF_8)
    buf.putShort(textBytes.size.toShort())
    buf.put(textBytes)
    sendUdp(buf, AgentState.regionHost, AgentState.regionPort)
}

/**
 * Show an LSL dialog() notification with button choices.
 * Mirrors process_script_dialog().
 */
fun processScriptDialog(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val objectName = fields["objectName"] as? String ?: ""
    val message    = fields["message"]    as? String ?: ""
    @Suppress("UNCHECKED_CAST")
    val buttons    = fields["buttons"]    as? List<String> ?: emptyList()
    LOG.info("processScriptDialog: from='$objectName' buttons=${buttons.size}")
    Notifications.add("ScriptDialog", mapOf(
        "OBJECT" to objectName,
        "MSG"    to message,
        "BTNS"   to buttons.joinToString("|"),
    ))
}

/**
 * Open a URL from llLoadURL() via the system browser or in-viewer browser.
 * Mirrors process_load_url().
 */
fun processLoadUrl(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val url = fields["url"] as? String ?: return
    LOG.info("processLoadUrl: url='$url'")
    try {
        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
    } catch (e: Exception) {
        LOG.warning("processLoadUrl: could not open '$url': ${e.message}")
    }
}

/**
 * Show a teleport-to dialog from llMapDestination().
 * Mirrors process_script_teleport_request().
 */
fun processScriptTeleportRequest(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val simName = fields["simName"] as? String ?: ""
    val posX = (fields["posX"] as? Float) ?: 0f
    val posY = (fields["posY"] as? Float) ?: 0f
    LOG.info("processScriptTeleportRequest: simName='$simName' pos=($posX,$posY)")
    Notifications.add("ScriptTeleportRequest", mapOf("SIM" to simName, "X" to posX.toString(), "Y" to posY.toString()))
}

/**
 * Decode and display parcel covenant text.
 * Mirrors process_covenant_reply().
 */
fun processCovenantReply(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val covenantId = LLUUID.fromString(fields["covenantId"] as? String ?: "") ?: LLUUID.NULL
    LOG.info("processCovenantReply: covenantId=$covenantId")
    // Fetch the covenant notecard asset and display it in the Covenant tab
    // AssetCache.fetchNotecard(covenantId) { text -> CovenantPanel.setText(text) }
}

/**
 * Show an accept/decline dialog for a calling-card offer.
 * Mirrors process_offer_callingcard().
 */
fun processOfferCallingCard(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val fromId = LLUUID.fromString(fields["fromId"] as? String ?: "") ?: LLUUID.NULL
    LOG.info("processOfferCallingCard: fromId=$fromId")
    Notifications.add("OfferCallingCard", mapOf("FROM" to fromId.toString()))
}

/**
 * Add a calling card to the agent's inventory.
 * Mirrors process_accept_callingcard().
 */
fun processAcceptCallingCard(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val fromId = LLUUID.fromString(fields["fromId"] as? String ?: "") ?: LLUUID.NULL
    LOG.info("processAcceptCallingCard: fromId=$fromId")
    // Inventory.addCallingCard(fromId)
}

/**
 * Handle remote rejection of our calling-card offer.
 * Mirrors process_decline_callingcard().
 */
fun processDeclineCallingCard(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val fromId = LLUUID.fromString(fields["fromId"] as? String ?: "") ?: LLUUID.NULL
    LOG.info("processDeclineCallingCard: fromId=$fromId")
    Notifications.add("DeclineCallingCard", mapOf("FROM" to fromId.toString()))
}

/**
 * Log or handle an invalid-message exception from the message system.
 * Mirrors invalid_message_callback().
 */
fun invalidMessageCallback(msg: Any, userData: Any?, exception: Int) {
    LOG.warning("invalidMessageCallback: exception=$exception msg=$msg")
    // exception codes mirror EMessageException (0=corrupt, 1=decode_error, …)
}

/**
 * Start an asset download from a URL carried in an InitiateDownload message.
 * Mirrors process_initiate_download(): kicks off an HTTP GET and writes the
 * result to a local file via java.net.HttpURLConnection.
 */
fun processInitiateDownload(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val url      = fields["url"]      as? String ?: return
    val filename = fields["filename"] as? String ?: "download.dat"
    LOG.info("processInitiateDownload: url='$url' filename='$filename'")
    thread(name = "asset-download") {
        try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout    = 30_000
            conn.inputStream.use { input ->
                java.io.File(filename).outputStream().use { out ->
                    input.copyTo(out)
                }
            }
            LOG.info("processInitiateDownload: saved '$filename'")
        } catch (e: Exception) {
            LOG.warning("processInitiateDownload: download failed – ${e.message}")
        }
    }
}

/**
 * Create and attach a new inventory observer for incoming items.
 * Mirrors start_new_inventory_observer(): ensures singleton observers exist.
 */
fun startNewInventoryObserver() {
    LOG.info("startNewInventoryObserver: attaching inventory observers")
    // InventoryModel.addObserver(OpenTaskOffer)
    // InventoryModel.addObserver(MoveFromWorldObserver)
    // InventoryModel.addObserver(NewInventoryHintObserver)
}

/**
 * Open received inventory items in the inventory panel.
 * Mirrors open_inventory_offer(): iterates items, throttle-checks, and opens
 * previews by asset type.
 */
fun openInventoryOffer(items: List<LLUUID>, fromName: String, fromAgentManual: Boolean = false) {
    LOG.info("openInventoryOffer: ${items.size} item(s) from '$fromName' manual=$fromAgentManual")
    for (itemId in items) {
        if (!highlightOfferedObject(itemId)) continue
        LOG.info("openInventoryOffer: highlighting/opening $itemId")
        // Real implementation: switch on asset type and open the appropriate preview floater
        // e.g.: when (assetType) { NOTECARD → showNotecard(itemId); TEXTURE → showTexture(itemId); ... }
    }
}

/**
 * Return true if [objId] is not in a quiet folder and the agent is not AFK.
 * Mirrors highlight_offered_object().
 */
fun highlightOfferedObject(objId: LLUUID): Boolean {
    if (AgentState.isAfk) {
        LOG.fine("highlightOfferedObject: agent is AFK, suppressing $objId")
        return false
    }
    // A real implementation would check the item's parent folder type
    // against "quiet" types (Trash, COF, Lost-and-Found).
    LOG.fine("highlightOfferedObject: ok for $objId")
    return true
}

/**
 * Set the drag-and-drop inventory-item target folder.
 * Mirrors set_dad_inventory_item().
 */
fun setDadInventoryItem(invItem: Any?, intoFolderUuid: LLUUID) {
    LOG.info("setDadInventoryItem: folder=$intoFolderUuid item=$invItem")
    startNewInventoryObserver()
    // InventoryMoveObserver.setMoveIntoFolderID(intoFolderUuid)
    // InventoryMoveObserver.watchAsset(invItem.assetUUID)
}

/**
 * Set the drag-and-drop inbox object ID.
 * Mirrors set_dad_inbox_object().
 */
fun setDadInboxObject(objectId: LLUUID) {
    LOG.info("setDadInboxObject: objectId=$objectId")
    // InventoryMoveObserver = new ViewerInventoryMoveObserver(objectId)
    // InventoryModel.addObserver(InventoryMoveObserver)
}

/**
 * Show a feature-disabled notification to the user.
 * Mirrors process_feature_disabled_message().
 */
fun processFeatureDisabledMessage(msg: Any, userData: Any?) {
    val fields = msg as? Map<*, *> ?: return
    val message = fields["message"] as? String ?: ""
    LOG.info("processFeatureDisabledMessage: '$message'")
    Notifications.add("FeatureDisabled", mapOf("MESSAGE" to message))
}

/**
 * Announce a region restart in [seconds] seconds to a configured chat channel.
 * Mirrors fs_report_region_restart_to_channel().
 */
fun fsReportRegionRestartToChannel(seconds: Int) {
    LOG.info("fsReportRegionRestartToChannel: $seconds seconds until restart")
    val message = "Region will restart in $seconds seconds."
    // Send to the configured chat channel (channel number from settings)
    val buf = newUdpBuffer()
    buf.putUUID(AgentState.agentId)
    buf.putUUID(AgentState.sessionId)
    // ChatFromViewer block: channel, type, message
    buf.putInt(0)    // channel 0 = public chat
    buf.put(1)       // NORMAL chat type
    val msgBytes = message.toByteArray(Charsets.UTF_8)
    buf.putShort(msgBytes.size.toShort())
    buf.put(msgBytes)
    sendUdp(buf, AgentState.regionHost, AgentState.regionPort)
    Notifications.add("RegionRestartAnnounced", mapOf("SECONDS" to seconds.toString()))
}
