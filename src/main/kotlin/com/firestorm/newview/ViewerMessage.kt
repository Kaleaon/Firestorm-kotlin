package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

enum class InventoryOfferResponse {
    ACCEPT,
    DECLINE,
    MUTE,
    SHOW,
    ACCEPT_SILENT,
    DECLINE_SILENT,
    SHOW_SILENT,
}

typealias TeleportStartedCallback = () -> Unit

typealias MessageHandler = (msg: Any, userData: Any?) -> Unit

data class MeanCollisionData(
    val perpetratorId: LLUUID,
    val victimId: LLUUID,
    val time: Long,
    val magnitude: Float,
    val type: Int,
)

val gMeanCollisionList: MutableList<MeanCollisionData> = mutableListOf()

class OfferInfo {
    companion object {
        var responderType: String = "LLOfferInfo"
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

    fun forceResponse(response: InventoryOfferResponse) {
        TODO("APR: use JVM equivalent — send accept/decline inventory offer message for response=$response")
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

    fun handleRespond(notification: Map<String, Any?>, response: Map<String, Any?>) {
        TODO("APR: use JVM equivalent — dispatch to inventory_offer_callback or inventory_task_offer_callback based on IM type")
    }

    fun sendAutoReceiveResponse() {
        TODO("APR: use JVM equivalent — send accept message for folder folderId=$folderId")
    }

    fun sendDeclineResponse() {
        TODO("APR: use JVM equivalent — send decline message to originating host")
    }

    fun inventoryOfferCallback(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
        TODO("APR: use JVM equivalent — accept/decline/mute/show based on notification response button")
    }

    fun inventoryTaskOfferCallback(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
        TODO("APR: use JVM equivalent — handle object inventory offer response")
    }
}

class OpenAgentOffer(
    private val objectId: LLUUID,
    private val fromName: String,
    private val isManuallyAccepted: Boolean,
) {
    fun startFetch() {
        TODO("APR: use JVM equivalent — fetch inventory items for objectId; add categories to complete list immediately")
    }

    fun done() {
        TODO("APR: use JVM equivalent — open_inventory_offer(complete, fromName, isManuallyAccepted); remove observer")
    }
}

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

fun canAffordTransaction(cost: Int): Boolean {
    TODO("APR: use JVM equivalent — return agent.balance >= cost")
}

fun giveMoney(
    uuid: LLUUID,
    region: Any?,
    amount: Int,
    isGroup: Boolean = false,
    trxType: Int = 5000,
    desc: String = "",
) {
    TODO("APR: use JVM equivalent — build and send MoneyTransfer UDP message")
}

fun sendJoinGroupResponse(
    groupId: LLUUID,
    transactionId: LLUUID,
    acceptInvite: Boolean,
    fee: Int,
    useOfflineCap: Boolean,
) {
    TODO("APR: use JVM equivalent — send JoinGroupRequest message or use capability URL")
}

fun processLogoutReply(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — clean up session state and close viewer")
}

fun processLayerData(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode terrain/wind/cloud layer data")
}

fun processDerezAck(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — handle derez acknowledgement")
}

fun processPlacesReply(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — populate places search results")
}

fun sendSoundTrigger(soundId: LLUUID, gain: Float) {
    TODO("APR: use JVM equivalent — send SoundTrigger UDP message")
}

fun processImprovedIm(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode and dispatch instant message")
}

fun processScriptQuestion(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show LSL script permission request dialog")
}

fun processChatFromSimulator(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode chat and post to nearby-chat panel")
}

fun sendAgentUpdate(forceSend: Boolean, sendReliable: Boolean = false) {
    TODO("APR: use JVM equivalent — pack and send AgentUpdate UDP packet")
}

fun processObjectUpdate(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode full object update")
}

fun processCompressedObjectUpdate(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode compressed object update")
}

fun processCachedObjectUpdate(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — apply cached object data")
}

fun processTerseObjectUpdateImproved(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode terse object update (position/velocity)")
}

fun processObjectProperties(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — broadcast object properties to area search and other subscribers")
}

fun processObjectPropertiesFamily(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — anti-spam guarded object properties family handler")
}

fun sendSimulatorThrottleSettings(host: String) {
    TODO("APR: use JVM equivalent — send AgentThrottle UDP message to host=$host")
}

fun processKillObject(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — remove killed object(s) from viewer object list")
}

fun processTimeSynch(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — synchronise simulator clock")
}

fun processSoundTrigger(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — play triggered sound at world position")
}

fun processPreloadSound(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — preload sound asset into cache")
}

fun processAttachedSound(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — attach sound to object and start playback")
}

fun processAttachedSoundGainChange(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — update gain on attached sound source")
}

fun processEnergyStatistics(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — update energy / physics stats display")
}

fun processHealthMessage(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — update agent health display")
}

fun processSimStats(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode and apply simulator statistics packet")
}

fun processShooterAgentHit(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — handle combat damage notification")
}

fun processAvatarAnimation(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — start/stop animations on target avatar")
}

fun processObjectAnimation(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — start/stop object-level animations")
}

fun processAvatarAppearance(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — apply baked texture/wearable data to avatar")
}

fun processCameraConstraint(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — apply camera constraint from parcel/region")
}

fun processAvatarSitResponse(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — position avatar in sit pose on object")
}

fun processSetFollowCamProperties(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — apply LSL follow-cam property overrides")
}

fun processClearFollowCamProperties(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — clear LSL follow-cam overrides and restore defaults")
}

fun processNameValue(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — add/update name-value pairs on object")
}

fun processRemoveNameValue(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — remove name-value pairs from object")
}

fun processKickUser(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show kicked-from-region notification and disconnect")
}

fun processEconomyData(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — update economy data (upload cost, group fee, etc.)")
}

fun processMoneyBalanceReply(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — update displayed L$ balance and show transaction toast")
}

fun processAdjustBalance(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — adjust local L$ balance display")
}

fun attemptStandardNotification(msg: Any): Boolean {
    TODO("APR: use JVM equivalent — try to show a standard notification for msg; return true if handled")
}

fun processAlertMessage(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — display modal or non-modal alert from simulator")
}

fun processAgentAlertMessage(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — display agent-targeted alert")
}

fun processAlertCore(message: String, modal: Boolean) {
    TODO("APR: use JVM equivalent — route alert message to notification system, modal=$modal")
}

fun processMeanCollisionAlertMessage(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — record collision data and show bump/push notification")
}

fun processFrozenMessage(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show frozen/unfrozen status indicator")
}

fun processDerezContainer(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — handle derez container message")
}

fun sendCompleteAgentMovement(simHost: String) {
    TODO("APR: use JVM equivalent — send CompleteAgentMovement message to simHost=$simHost")
}

fun processAgentMovementComplete(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — finalise agent position after TP/crossing; enable UI")
}

fun processCrossedRegion(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — switch to new region circuit after seamless border cross")
}

fun processTeleportStart(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — display teleport progress UI; fire teleportStartedSignal")
}

fun processTeleportProgress(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — update teleport progress bar text")
}

fun processTeleportFailed(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show teleport-failed notification and restore UI state")
}

fun processTeleportFinish(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — complete teleport: switch regions and reinitialise objects")
}

fun processTeleportLocal(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — handle in-region teleport (position change without region switch)")
}

fun processUserSimLocationReply(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — handle user sim location reply for world map teleport")
}

fun sendSimpleIm(
    toId: LLUUID,
    message: String,
    dialog: Int = 0,
    id: LLUUID = LLUUID.NULL,
) {
    TODO("APR: use JVM equivalent — build and send ImprovedInstantMessage UDP packet")
}

fun sendGroupNotice(
    groupId: LLUUID,
    subject: String,
    message: String,
    item: Any?,
) {
    TODO("APR: use JVM equivalent — send GroupNoticeAdd message with optional inventory attachment")
}

fun sendDoNotDisturbMessage(msg: Any, fromId: LLUUID, sessionId: LLUUID = LLUUID.NULL) {
    TODO("APR: use JVM equivalent — send auto-reply do-not-disturb IM")
}

fun sendRejectingTpOffersMessage(msg: Any, fromId: LLUUID, sessionId: LLUUID = LLUUID.NULL) {
    TODO("APR: use JVM equivalent — send auto-reject teleport offer IM")
}

fun sendRejectingFriendshipRequestsMessage(msg: Any, fromId: LLUUID, sessionId: LLUUID = LLUUID.NULL) {
    TODO("APR: use JVM equivalent — send auto-reject friendship request IM")
}

fun handleLure(inviteeId: LLUUID) {
    TODO("APR: use JVM equivalent — send teleport lure to single inviteeId=$inviteeId")
}

fun handleLureMultiple(ids: List<LLUUID>) {
    TODO("APR: use JVM equivalent — send teleport lure to multiple avatars")
}

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
    TODO("APR: use JVM equivalent — pack and send full ImprovedInstantMessage packet")
}

fun processUserInfoReply(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — update account user info fields")
}

fun formattedTime(epochSeconds: Long): String {
    TODO("APR: use JVM equivalent — java.time.Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(...)")
}

fun sendPlacesQuery(
    queryId: LLUUID,
    transId: LLUUID,
    queryText: String,
    queryFlags: UInt,
    category: Int,
    simName: String,
) {
    TODO("APR: use JVM equivalent — send PlacesQuery UDP message")
}

fun processScriptDialog(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show LSL dialog() notification with button choices")
}

fun processLoadUrl(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — open URL from llLoadURL() via browser or in-viewer")
}

fun processScriptTeleportRequest(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show teleport-to dialog from llMapDestination()")
}

fun processCovenantReply(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — decode and display parcel covenant text")
}

fun processOfferCallingCard(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show accept/decline dialog for calling card offer")
}

fun processAcceptCallingCard(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — add calling card to agent inventory")
}

fun processDeclineCallingCard(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — handle remote rejection of our calling card offer")
}

fun invalidMessageCallback(msg: Any, userData: Any?, exception: Int) {
    TODO("APR: use JVM equivalent — log or handle invalid message exception")
}

fun processInitiateDownload(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — start asset download from URL in message")
}

fun startNewInventoryObserver() {
    TODO("APR: use JVM equivalent — create and attach a new inventory observer for incoming items")
}

fun openInventoryOffer(items: List<LLUUID>, fromName: String, fromAgentManual: Boolean = false) {
    TODO("APR: use JVM equivalent — open received inventory items in inventory panel, fromName=$fromName")
}

fun highlightOfferedObject(objId: LLUUID): Boolean {
    TODO("APR: use JVM equivalent — highlight object in inventory; return false if in quiet folder or agent AFK")
}

fun setDadInventoryItem(invItem: Any?, intoFolderUuid: LLUUID) {
    TODO("APR: use JVM equivalent — set drag-and-drop inventory item target folder")
}

fun setDadInboxObject(objectId: LLUUID) {
    TODO("APR: use JVM equivalent — set drag-and-drop inbox object id")
}

fun processFeatureDisabledMessage(msg: Any, userData: Any?) {
    TODO("APR: use JVM equivalent — show feature-disabled notification to user")
}

fun fsReportRegionRestartToChannel(seconds: Int) {
    TODO("APR: use JVM equivalent — announce region restart in seconds=$seconds to configured chat channel")
}
