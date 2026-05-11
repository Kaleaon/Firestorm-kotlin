package com.firestorm.newview

import java.util.UUID

// ============================================================================
// Enums
// ============================================================================

enum class EAntispamQueue {
    ANTISPAM_QUEUE_CHAT,
    ANTISPAM_QUEUE_INVENTORY,
    ANTISPAM_QUEUE_IM,
    ANTISPAM_QUEUE_CALLING_CARD,
    ANTISPAM_QUEUE_SOUND,
    ANTISPAM_QUEUE_SOUND_PRELOAD,
    ANTISPAM_QUEUE_SCRIPT_DIALOG,
    ANTISPAM_QUEUE_TELEPORT;

    companion object {
        val MAX get() = values().size
    }
}

enum class EAntispamSource {
    ANTISPAM_SOURCE_AGENT,
    ANTISPAM_SOURCE_OBJECT
}

enum class EAntispamCheckResult {
    Unblocked,
    NewBlock,
    ExistingBlock
}

// ============================================================================
// AntispamObjectData — carries context for a pending spam notification
// ============================================================================

data class AntispamObjectData(
    var name:           String         = "",
    var queue:          EAntispamQueue = EAntispamQueue.ANTISPAM_QUEUE_CHAT,
    var count:          UInt           = 0u,
    var period:         UInt           = 0u,
    var notificationId: String         = ""
)

// ============================================================================
// NACLAntiSpamQueueEntry — per-source tracking record inside a queue
// ============================================================================

class NACLAntiSpamQueueEntry {
    private var entryAmount: UInt    = 0u
    private var entryTime:   UInt    = 0u
    private var blocked:     Boolean = false

    fun getEntryAmount(): UInt = entryAmount
    fun getEntryTime():   UInt = entryTime
    fun getBlocked():     Boolean = blocked

    fun clearEntry() {
        entryTime   = 0u
        entryAmount = 0u
        blocked     = false
    }

    fun updateEntryAmount() { entryAmount++ }

    fun updateEntryTime() {
        entryTime = currentEpochSeconds()
    }

    fun setBlocked() { blocked = true }
}

// ============================================================================
// NACLAntiSpamQueue — time-windowed counter map for one message category
// ============================================================================

class NACLAntiSpamQueue(time: UInt, amount: UInt) {
    private val entries:     MutableMap<UUID, NACLAntiSpamQueueEntry> = mutableMapOf()
    private var queueAmount: UInt = amount
    private var queueTime:   UInt = time

    fun getAmount(): UInt = queueAmount
    fun getTime():   UInt = queueTime

    fun setAmount(amount: UInt) { queueAmount = amount }
    fun setTime(time: UInt)     { queueTime   = time   }

    fun getEntry(source: UUID): NACLAntiSpamQueueEntry? = entries[source]

    fun clearEntries() {
        // Only reset entries that are not currently blocked so intentional blocks survive.
        for (entry in entries.values) {
            if (!entry.getBlocked()) entry.clearEntry()
        }
    }

    fun purgeEntries() {
        entries.clear()
    }

    fun blockEntry(source: UUID) {
        entries.getOrPut(source) { NACLAntiSpamQueueEntry() }.setBlocked()
    }

    fun checkEntry(source: UUID, multiplier: UInt): EAntispamCheckResult {
        val existing = entries[source]
        if (existing != null) {
            if (existing.getBlocked()) return EAntispamCheckResult.ExistingBlock

            val now = currentEpochSeconds()
            return if ((now - existing.getEntryTime()) <= queueTime) {
                existing.updateEntryAmount()
                if (existing.getEntryAmount() > queueAmount * multiplier) {
                    existing.setBlocked()
                    EAntispamCheckResult.NewBlock
                } else {
                    EAntispamCheckResult.Unblocked
                }
            } else {
                existing.clearEntry()
                existing.updateEntryAmount()
                existing.updateEntryTime()
                EAntispamCheckResult.Unblocked
            }
        } else {
            val entry = NACLAntiSpamQueueEntry()
            entry.updateEntryAmount()
            entry.updateEntryTime()
            entries[source] = entry
            return EAntispamCheckResult.Unblocked
        }
    }
}

// ============================================================================
// NACLAntiSpamRegistry — singleton that owns all per-category queues and the
// optional global queue, and issues UI notifications on new blocks.
// Kotlin object replaces LLSingleton<NACLAntiSpamRegistry>.
// ============================================================================

object NACLAntiSpamRegistry {

    private val queueNames = mapOf(
        EAntispamQueue.ANTISPAM_QUEUE_CHAT          to "Chat",
        EAntispamQueue.ANTISPAM_QUEUE_INVENTORY     to "Inventory",
        EAntispamQueue.ANTISPAM_QUEUE_IM            to "Instant Message",
        EAntispamQueue.ANTISPAM_QUEUE_CALLING_CARD  to "Calling Card",
        EAntispamQueue.ANTISPAM_QUEUE_SOUND         to "Sound",
        EAntispamQueue.ANTISPAM_QUEUE_SOUND_PRELOAD to "Sound Preload",
        EAntispamQueue.ANTISPAM_QUEUE_SCRIPT_DIALOG to "Script Dialog",
        EAntispamQueue.ANTISPAM_QUEUE_TELEPORT      to "Teleport"
    )

    // Settings read from saved preferences at startup
    private var globalTime:   UInt    = getSavedU32("_NACL_AntiSpamTime")
    private var globalAmount: UInt    = getSavedU32("_NACL_AntiSpamAmount")
    private var globalQueue:  Boolean = getSavedBool("_NACL_AntiSpamGlobalQueue")

    private val queues: Array<NACLAntiSpamQueue> =
        Array(EAntispamQueue.MAX) { NACLAntiSpamQueue(globalTime, globalAmount) }

    private val globalEntries: MutableMap<UUID, NACLAntiSpamQueueEntry> = mutableMapOf()

    // Pending object-property-family lookups: object UUID -> notification data
    private val objectData: MutableMap<UUID, AntispamObjectData> = mutableMapOf()

    // Avatar-name callback slots keyed by unique request UUID
    private val avatarNameCallbackConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()

    // -------------------------------------------------------------------------
    // Configuration setters
    // -------------------------------------------------------------------------

    fun setGlobalQueue(value: Boolean) {
        purgeAllQueues()
        globalQueue = value
    }

    fun setGlobalAmount(amount: UInt) { globalAmount = amount }
    fun setGlobalTime(time: UInt)     { globalTime   = time   }

    fun setRegisteredQueueTime(queue: EAntispamQueue, time: UInt) {
        requireValidQueue(queue)
        queues[queue.ordinal].setTime(time)
    }

    fun setRegisteredQueueAmount(queue: EAntispamQueue, amount: UInt) {
        requireValidQueue(queue)
        queues[queue.ordinal].setAmount(amount)
    }

    fun setAllQueueTimes(time: UInt) {
        globalTime = time
        queues.forEach { it.setTime(time) }
    }

    fun setAllQueueAmounts(amount: UInt) {
        globalAmount = amount
        queues.forEachIndexed { idx, q ->
            val aq = EAntispamQueue.values()[idx]
            // Sound queues are allowed 5× the normal threshold
            val effective = if (aq == EAntispamQueue.ANTISPAM_QUEUE_SOUND ||
                                aq == EAntispamQueue.ANTISPAM_QUEUE_SOUND_PRELOAD) amount * 5u else amount
            q.setAmount(effective)
        }
    }

    // -------------------------------------------------------------------------
    // Block / check
    // -------------------------------------------------------------------------

    fun blockOnQueue(queue: EAntispamQueue, source: UUID) {
        if (!isAntiSpamEnabled()) return
        if (globalQueue) {
            blockGlobalEntry(source)
        } else {
            requireValidQueue(queue)
            queues[queue.ordinal].blockEntry(source)
        }
    }

    // Returns true if the source is blocked (existing or newly triggered).
    fun checkQueue(queue: EAntispamQueue, source: UUID, sourceType: EAntispamSource, multiplier: UInt = 1u): Boolean {
        if (!isAntiSpamEnabled()) return false
        if (source == nilUUID() || source == agentId()) return false

        if (sourceType == EAntispamSource.ANTISPAM_SOURCE_OBJECT) {
            if (isOwnedByAgent(source) && !isAntiSpamMineEnabled()) return false
        }

        val result: EAntispamCheckResult = if (globalQueue) {
            checkGlobalEntry(source, multiplier)
        } else {
            requireValidQueue(queue)
            queues[queue.ordinal].checkEntry(source, multiplier)
        }

        return when (result) {
            EAntispamCheckResult.Unblocked    -> false
            EAntispamCheckResult.ExistingBlock -> true
            EAntispamCheckResult.NewBlock -> {
                if (!isMuted(source)) {
                    val data = AntispamObjectData(
                        name           = source.toString(),
                        queue          = queue,
                        count          = multiplier * queues[queue.ordinal].getAmount(),
                        period         = queues[queue.ordinal].getTime(),
                        notificationId = "AntiSpamBlocked"
                    )
                    if (sourceType == EAntispamSource.ANTISPAM_SOURCE_OBJECT) {
                        val sent = requestObjectPropertiesFamily(source)
                        if (sent) objectData[source] = data
                    } else {
                        val requestId = UUID.randomUUID()
                        val slot = scheduleAvatarNameCallback(source) { avId, avName ->
                            onAvatarNameCallback(avId, avName, data, requestId)
                        }
                        avatarNameCallbackConnections[requestId] = slot
                    }
                }
                true
            }
        }
    }

    fun checkNewlineFlood(queue: EAntispamQueue, source: UUID, message: String): Boolean {
        if (isBlockedOnQueue(EAntispamQueue.ANTISPAM_QUEUE_IM, source)) return true

        val maxNewlines = getSavedU32("_NACL_AntiSpamNewlines").toInt()
        val count = message.count { it == '\n' }
        if (count > maxNewlines) {
            blockOnQueue(EAntispamQueue.ANTISPAM_QUEUE_IM, source)
            if (!isMuted(source)) {
                val notifId = if (queue == EAntispamQueue.ANTISPAM_QUEUE_IM)
                    "AntiSpamImNewLineFloodBlocked" else "AntiSpamChatNewLineFloodBlocked"
                val data = AntispamObjectData(
                    name           = source.toString(),
                    queue          = queue,
                    count          = maxNewlines.toUInt(),
                    period         = 0u,
                    notificationId = notifId
                )
                val requestId = UUID.randomUUID()
                val slot = scheduleAvatarNameCallback(source) { avId, avName ->
                    onAvatarNameCallback(avId, avName, data, requestId)
                }
                avatarNameCallbackConnections[requestId] = slot
            }
            return true
        }
        return false
    }

    fun isBlockedOnQueue(queue: EAntispamQueue, source: UUID): Boolean {
        if (!isAntiSpamEnabled()) return false
        return if (globalQueue) {
            globalEntries[source]?.getBlocked() ?: false
        } else {
            requireValidQueue(queue)
            queues[queue.ordinal].getEntry(source)?.getBlocked() ?: false
        }
    }

    // -------------------------------------------------------------------------
    // Clear / purge
    // -------------------------------------------------------------------------

    fun clearRegisteredQueue(queue: EAntispamQueue) {
        requireValidQueue(queue)
        queues[queue.ordinal].clearEntries()
    }

    fun purgeRegisteredQueue(queue: EAntispamQueue) {
        requireValidQueue(queue)
        queues[queue.ordinal].purgeEntries()
    }

    fun clearAllQueues() {
        if (globalQueue) clearGlobalEntries()
        else queues.forEach { it.clearEntries() }
    }

    fun purgeAllQueues() {
        avatarNameCallbackConnections.values.forEach { it?.invoke() }
        avatarNameCallbackConnections.clear()

        if (globalQueue) purgeGlobalEntries()
        else queues.forEach { it.purgeEntries() }

        objectData.clear()
    }

    // -------------------------------------------------------------------------
    // ObjectPropertiesFamily handler (called from the message handler)
    // -------------------------------------------------------------------------

    fun processObjectPropertiesFamily(objectId: UUID, ownerId: UUID, name: String) {
        if (!isAntiSpamEnabled()) return

        val data = objectData.remove(objectId) ?: return
        if (!isBlockedOnQueue(data.queue, ownerId)) {
            data.name = buildObjectSLURL(objectId, name, ownerId)
            notify(data)
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun blockGlobalEntry(source: UUID) {
        globalEntries.getOrPut(source) { NACLAntiSpamQueueEntry() }.setBlocked()
    }

    private fun checkGlobalEntry(source: UUID, multiplier: UInt): EAntispamCheckResult {
        val existing = globalEntries[source]
        if (existing != null) {
            if (existing.getBlocked()) return EAntispamCheckResult.ExistingBlock
            val now = currentEpochSeconds()
            return if ((now - existing.getEntryTime()) <= globalTime) {
                existing.updateEntryAmount()
                if (existing.getEntryAmount() > globalAmount * multiplier)
                    EAntispamCheckResult.NewBlock
                else
                    EAntispamCheckResult.Unblocked
            } else {
                existing.clearEntry()
                existing.updateEntryAmount()
                existing.updateEntryTime()
                EAntispamCheckResult.Unblocked
            }
        } else {
            val entry = NACLAntiSpamQueueEntry()
            entry.updateEntryAmount()
            entry.updateEntryTime()
            globalEntries[source] = entry
            return EAntispamCheckResult.Unblocked
        }
    }

    private fun clearGlobalEntries() {
        globalEntries.values.forEach { it.clearEntry() }
    }

    private fun purgeGlobalEntries() {
        globalEntries.clear()
    }

    private fun onAvatarNameCallback(avId: UUID, avName: String, data: AntispamObjectData, requestId: UUID) {
        avatarNameCallbackConnections.remove(requestId)
        data.name = buildAgentInspectSLURL(avId)
        notify(data)
    }

    private fun notify(data: AntispamObjectData) {
        TODO("APR: use JVM equivalent to LLNotificationsUtil::add(${data.notificationId}, args with SOURCE/QUEUE/COUNT/PERIOD)")
    }

    private fun requireValidQueue(queue: EAntispamQueue) {
        require(queue.ordinal < EAntispamQueue.MAX) {
            "Attempting to use antispam queue outside valid range: ${queueName(queue)}"
        }
    }

    private fun queueName(queue: EAntispamQueue): String =
        queueNames[queue] ?: "Unknown"

    // -------------------------------------------------------------------------
    // Platform-integration stubs
    // -------------------------------------------------------------------------

    private fun currentEpochSeconds(): UInt =
        (System.currentTimeMillis() / 1000L).toUInt()

    private fun isAntiSpamEnabled(): Boolean {
        TODO("APR: use JVM equivalent to gSavedSettings.getBOOL(\"UseAntiSpam\")")
    }

    private fun isAntiSpamMineEnabled(): Boolean {
        TODO("APR: use JVM equivalent to gSavedSettings.getBOOL(\"FSUseAntiSpamMine\")")
    }

    private fun nilUUID(): UUID = UUID(0, 0)

    private fun agentId(): UUID {
        TODO("APR: use JVM equivalent to gAgentID")
    }

    private fun isOwnedByAgent(objectId: UUID): Boolean {
        TODO("APR: use JVM equivalent to gObjectList.findObject(objectId)?.permYouOwner()")
    }

    private fun isMuted(source: UUID): Boolean {
        TODO("APR: use JVM equivalent to LLMuteList::getInstance()->isMuted(source)")
    }

    private fun requestObjectPropertiesFamily(objectId: UUID): Boolean {
        TODO("APR: use JVM equivalent to send RequestObjectPropertiesFamily UDP message to all live regions")
    }

    private fun scheduleAvatarNameCallback(agentId: UUID, callback: (UUID, String) -> Unit): () -> Unit {
        TODO("APR: use JVM equivalent to LLAvatarNameCache::get(agentId, callback); return disconnect lambda")
    }

    private fun getSavedU32(key: String): UInt {
        TODO("APR: use JVM equivalent to gSavedSettings.getU32($key)")
    }

    private fun getSavedBool(key: String): Boolean {
        TODO("APR: use JVM equivalent to gSavedSettings.getBOOL($key)")
    }

    private fun buildObjectSLURL(objectId: UUID, name: String, ownerId: UUID): String {
        TODO("APR: use JVM equivalent to LLSLURL(\"objectim\", objectId, \"\").getSLURLString() + \"?name=\" + escape(name) + \"&owner=\" + ownerId")
    }

    private fun buildAgentInspectSLURL(agentId: UUID): String {
        TODO("APR: use JVM equivalent to LLSLURL(\"agent\", agentId, \"inspect\").getSLURLString()")
    }
}
