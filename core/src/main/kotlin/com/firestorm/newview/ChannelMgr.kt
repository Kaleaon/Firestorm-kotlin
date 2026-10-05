/**
 * ChannelMgr.kt
 * Converted from llchannelmanager.h / llchannelmanager.cpp
 *
 * Manages screen notification channels — creates, retrieves, and removes them.
 * Equivalent to LLNotificationsUI::LLChannelManager (LLSingleton) in the C++
 * viewer.
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

/**
 * A screen channel that aggregates toast notifications.
 *
 * Models LLChannelManager::ChannelElem + the contained LLScreenChannel.
 *
 * @param id       Unique identifier for this channel, generated on creation.
 * @param channel  Logical channel number (0 = general, >0 = LSL script channels).
 * @param messages Ordered list of pending notification message strings.
 */
data class NotifyBox(
    val id: LLUUID,
    val channel: Int,
    val messages: MutableList<String> = mutableListOf(),
)

/**
 * Singleton channel manager.
 *
 * ```kotlin
 * val box = ChannelMgr.addNotifyBox(channel = 0)
 * ChannelMgr.getChannel(0)?.messages?.add("Hello world")
 * ChannelMgr.killNotifyBox(box.id)
 * ```
 *
 * C++ lineage: LLNotificationsUI::LLChannelManager : LLSingleton
 */
object ChannelMgr {

    // ── Internal storage ──────────────────────────────────────────────────

    /** All currently active notify boxes, keyed by their UUID. */
    private val channelMap: LinkedHashMap<LLUUID, NotifyBox> = LinkedHashMap()

    // ── Login / startup hooks ─────────────────────────────────────────────

    /**
     * Called after login completes so that the startup toast can be shown.
     * Mirrors LLChannelManager::onLoginCompleted().
     */
    fun onLoginCompleted() {
        // Full implementation unhides the start-up toast channel.
    }

    /**
     * Called when the startup toast is dismissed; allows other channels to
     * show their queued toasts.
     * Mirrors LLChannelManager::onStartUpToastClose().
     */
    fun onStartUpToastClose() {
        // Full implementation marks mStartUpChannel closed and flushes queues.
    }

    // ── Channel lifecycle ─────────────────────────────────────────────────

    /**
     * Create a new [NotifyBox] for the given [channel] number and register it.
     * Returns the newly created box.
     *
     * Mirrors the creation path in LLChannelManager::getChannel() and
     * LLChannelManager::addChannel().
     */
    fun addNotifyBox(channel: Int): NotifyBox {
        val box = NotifyBox(
            id      = LLUUID.generateNewID(),
            channel = channel,
        )
        channelMap[box.id] = box
        return box
    }

    /**
     * Find the (first) [NotifyBox] with the given logical [channel] number.
     * Returns null if none exists.
     *
     * Mirrors LLChannelManager::findChannelByID() (adapted for numeric channel).
     */
    fun getChannel(channel: Int): NotifyBox? =
        channelMap.values.firstOrNull { it.channel == channel }

    /**
     * Look up a [NotifyBox] directly by its UUID.
     * Mirrors LLChannelManager::findChannelByID(const LLUUID&).
     */
    fun findChannelById(id: LLUUID): NotifyBox? = channelMap[id]

    /**
     * Remove and discard the [NotifyBox] identified by [id].
     * Mirrors LLChannelManager::removeChannelByID().
     */
    fun killNotifyBox(id: LLUUID) {
        channelMap.remove(id)
    }

    // ── Bulk operations ───────────────────────────────────────────────────

    /**
     * Mute or un-mute toast display on all channels.
     * Mirrors LLChannelManager::muteAllChannels().
     */
    fun muteAllChannels(mute: Boolean) {
        // Full implementation sets a muted flag on every LLScreenChannel.
    }

    /**
     * Remove all toasts from channel [id] that satisfy [predicate].
     * Mirrors LLChannelManager::killToastsFromChannel().
     */
    fun killToastsFromChannel(id: LLUUID, predicate: (String) -> Boolean) {
        channelMap[id]?.messages?.removeAll(predicate)
    }

    // ── Convenience accessors ─────────────────────────────────────────────

    /** Snapshot of all registered notify boxes. */
    fun getChannelList(): List<NotifyBox> = channelMap.values.toList()

    /** Total number of active channels. */
    val size: Int get() = channelMap.size

    // ── Cleanup ───────────────────────────────────────────────────────────

    /**
     * Release all channels; called on viewer shutdown.
     * Mirrors LLChannelManager::cleanupSingleton().
     */
    fun cleanup() {
        channelMap.clear()
    }

    override fun toString(): String =
        "ChannelMgr(channels=${channelMap.size})"
}
