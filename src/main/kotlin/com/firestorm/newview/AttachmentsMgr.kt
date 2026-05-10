/**
 * AttachmentsMgr.kt
 * Converted from llattachmentsmgr.h / llattachmentsmgr.cpp
 *
 * Manages batching of attachment rez/derez requests and COF link creation.
 * The original C++ singleton (LLSingleton<LLAttachmentsMgr>) is represented
 * as a Kotlin object.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLTimer

// ---------------------------------------------------------------------------
// Constants (mirror of C++ compile-time constants)
// ---------------------------------------------------------------------------
private const val COF_LINK_BATCH_TIME: Float             = 5.0f
private const val MAX_ATTACHMENT_REQUEST_LIFETIME: Float = 30.0f
private const val MIN_RETRY_REQUEST_TIME: Float          = 5.0f

// ---------------------------------------------------------------------------
// AttachmentsMgr  (singleton  →  Kotlin object)
// ---------------------------------------------------------------------------

/**
 * Batches attachment rez requests into single RezMultipleAttachmentsFromInv
 * messages and defers COF link creation until a full batch has arrived or a
 * timeout is exceeded.
 *
 * Maps to C++ class LLAttachmentsMgr (LLSingleton).
 */
object AttachmentsMgr {

    // -----------------------------------------------------------------------
    // Public data class  (C++ struct AttachmentsInfo)
    // -----------------------------------------------------------------------

    /**
     * Stores the parameters for a single pending attachment request.
     *
     * @property itemId     Inventory item UUID to attach.
     * @property attachPt   Attachment point index (0 = default point).
     * @property replace    false = add alongside existing; true = replace.
     */
    data class AttachmentRequest(
        val itemId: LLUUID,
        val attachPt: UInt,
        val replace: Boolean
    )

    // -----------------------------------------------------------------------
    // Private state
    // -----------------------------------------------------------------------

    /** Attachments queued but not yet sent to the server. */
    val pendingAttachments: MutableList<AttachmentRequest> = mutableListOf()

    /** Items requested from the server but not yet arrived (itemId → request timer). */
    private val attachmentRequests: ItemRequestTimes =
        ItemRequestTimes("attach", MIN_RETRY_REQUEST_TIME)

    /** Items requested to detach but not yet gone (itemId → request timer). */
    private val detachRequests: ItemRequestTimes =
        ItemRequestTimes("detach", MIN_RETRY_REQUEST_TIME)

    /** Attachments that have arrived but whose COF links haven't been created yet. */
    private val recentlyArrivedAttachments: MutableSet<LLUUID> = mutableSetOf()

    /** COF link creations that are in-flight. */
    private val pendingAttachLinks: MutableSet<LLUUID> = mutableSetOf()

    /** Timer used to batch COF link requests after attachments arrive. */
    private val cofLinkBatchTimer: LLTimer = LLTimer()

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Queue an attachment request to be sent on the next idle tick.
     * Silently drops duplicate requests that are still within the retry window.
     *
     * @param itemId     The inventory item to attach.
     * @param attachPt   Requested attachment point (0 for default).
     * @param replace    If true, replace existing occupant; otherwise add.
     */
    fun addAttachmentRequest(itemId: LLUUID, attachPt: UInt, replace: Boolean) {
        if (itemId.isNull()) return
        if (attachmentRequests.wasRequestedRecently(itemId)) return

        val req = AttachmentRequest(itemId = itemId, attachPt = attachPt, replace = replace)
        pendingAttachments.add(req)
        attachmentRequests.addTime(itemId)
    }

    /**
     * Called when an attachment message has been dispatched to the server so
     * that the request timer is (re-)started for retry-window tracking.
     *
     * @param itemId The inventory item that was requested.
     */
    fun onAttachmentRequested(itemId: LLUUID) {
        attachmentRequests.addTime(itemId)
    }

    /**
     * Called when an attachment object actually arrives in the scene.
     * Moves the item out of the in-flight set and into the
     * recently-arrived set so a COF link can be batched.
     *
     * @param itemId The inventory item that has arrived.
     */
    fun onAttachmentArrived(itemId: LLUUID) {
        val wasExpected = attachmentRequests.wasRequestedRecently(itemId)
        attachmentRequests.removeTime(itemId)

        if (recentlyArrivedAttachments.isEmpty()) {
            cofLinkBatchTimer.reset()
        }
        recentlyArrivedAttachments.add(itemId)

        // wasExpected is retained here so callers or subclasses can log
        // unexpected arrivals; the value is intentionally unused in the stub.
        @Suppress("UNUSED_VARIABLE")
        val unused = wasExpected
    }

    /**
     * Called when a detach has been requested for an item.
     *
     * @param itemId The inventory item to be detached.
     */
    fun onDetachRequested(itemId: LLUUID) {
        detachRequests.addTime(itemId)
    }

    /**
     * Called when a detach has completed (object left the scene).
     *
     * @param itemId The inventory item that was detached.
     */
    fun onDetachCompleted(itemId: LLUUID) {
        clearPendingAttachmentLink(itemId)
        detachRequests.removeTime(itemId)
    }

    /**
     * Returns true only when every pending/in-flight attachment and detach
     * operation has settled — i.e. no more work remains.
     */
    fun isAttachmentStateComplete(): Boolean =
        pendingAttachments.isEmpty() &&
        attachmentRequests.isEmpty() &&
        detachRequests.isEmpty() &&
        recentlyArrivedAttachments.isEmpty() &&
        pendingAttachLinks.isEmpty()

    /**
     * Fills [ids] with the union of recently-arrived and pending-COF-link
     * attachment IDs.
     *
     * @return true if [ids] is non-empty.
     */
    fun getPendingAttachments(ids: MutableSet<LLUUID>): Boolean {
        ids.clear()
        ids.addAll(recentlyArrivedAttachments)
        ids.addAll(pendingAttachLinks)
        return ids.isNotEmpty()
    }

    /** Remove a single item from the pending-COF-link set. */
    fun clearPendingAttachmentLink(itemId: LLUUID) {
        pendingAttachLinks.remove(itemId)
    }

    /**
     * Send all queued pending attachments to the server as a
     * RezMultipleAttachmentsFromInv message batch.
     * The full network messaging logic is stubbed; the real implementation
     * would consume [pendingAttachments] in chunks of up to 40 items.
     */
    fun firePendingAttachments() {
        TODO("Send RezMultipleAttachmentsFromInv for each item in pendingAttachments")
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Drive the idle-loop logic: flush pending, link arrived, expire stale. */
    internal fun onIdle() {
        requestPendingAttachments()
        linkRecentlyArrivedAttachments()
        expireOldAttachmentRequests()
        expireOldDetachRequests()
    }

    private fun requestPendingAttachments() {
        if (pendingAttachments.isNotEmpty()) {
            firePendingAttachments()
        }
    }

    private fun linkRecentlyArrivedAttachments() {
        if (recentlyArrivedAttachments.isEmpty()) return
        val allRequestsDone = attachmentRequests.isEmpty()
        val timedOut = cofLinkBatchTimer.getElapsedTimeF32() > COF_LINK_BATCH_TIME
        if (!allRequestsDone && !timedOut) return

        // TODO: call LLAppearanceMgr.addCOFItemLink for each item in
        //       recentlyArrivedAttachments that is worn but not yet in COF.
        recentlyArrivedAttachments.clear()
    }

    private fun expireOldAttachmentRequests() {
        attachmentRequests.expireOlderThan(MAX_ATTACHMENT_REQUEST_LIFETIME)
    }

    private fun expireOldDetachRequests() {
        detachRequests.expireOlderThan(MAX_ATTACHMENT_REQUEST_LIFETIME)
    }

    // -----------------------------------------------------------------------
    // ItemRequestTimes  (inner helper; mirrors C++ LLItemRequestTimes)
    // -----------------------------------------------------------------------

    /**
     * Tracks the wall-clock time at which each item was most recently
     * requested so we can detect duplicates and expire stale entries.
     *
     * @property opName  Human-readable label used for log messages.
     * @property timeout Seconds within which a request is considered "recent".
     */
    class ItemRequestTimes(
        private val opName: String,
        private val timeout: Float
    ) {
        private val times: MutableMap<LLUUID, LLTimer> = mutableMapOf()

        fun addTime(itemId: LLUUID) {
            times[itemId] = LLTimer()
        }

        fun removeTime(itemId: LLUUID) {
            times.remove(itemId)
        }

        fun wasRequestedRecently(itemId: LLUUID): Boolean {
            val timer = times[itemId] ?: return false
            return timer.getElapsedTimeF32() < timeout
        }

        fun getTime(itemId: LLUUID): LLTimer? = times[itemId]

        fun isEmpty(): Boolean = times.isEmpty()

        fun expireOlderThan(maxAge: Float) {
            val iter = times.entries.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                if (entry.value.getElapsedTimeF32() > maxAge) {
                    iter.remove()
                }
            }
        }
    }
}
