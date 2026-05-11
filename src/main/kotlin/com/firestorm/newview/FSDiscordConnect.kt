/**
 * @file FSDiscordConnect.kt
 * @brief Connection to Discord — Kotlin conversion of fsdiscordconnect.h / fsdiscordconnect.cpp
 *
 * Original author: liny@pinkfox.xyz (2019)
 * The Phoenix Firestorm Project, Inc.
 * http://www.firestormviewer.org
 *
 * Licensed under the GNU Lesser General Public License v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.*
import com.firestorm.llmath.*

// ---------------------------------------------------------------------------
// ConnectionState — mirrors EConnectionState in the C++ header
// ---------------------------------------------------------------------------
enum class ConnectionState {
    /** DISCORD_NOT_CONNECTED (0) */
    NOT_CONNECTED,
    /** DISCORD_CONNECTION_IN_PROGRESS (1) */
    CONNECTING,
    /** DISCORD_CONNECTED (2) */
    CONNECTED,
    /** DISCORD_CONNECTION_FAILED (3) */
    CONNECTION_FAILED,
    /** DISCORD_DISCONNECTING (4) */
    DISCONNECTING
}

// ---------------------------------------------------------------------------
// FSDiscordConnect — singleton Discord Rich Presence integration.
//
// The C++ class extends LLSingleton<FSDiscordConnect> and drives the
// discord-rpc C library via coroutines launched on the main loop.
// All native-library calls are stubbed with TODO() here.
// ---------------------------------------------------------------------------
object FSDiscordConnect {

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    var connectionState: ConnectionState = ConnectionState.NOT_CONNECTED
        private set

    val isConnected: Boolean get() = connectionState == ConnectionState.CONNECTED

    /**
     * Opaque info block received from Discord when the connection is ready
     * (contains at minimum "name" = username).  Mirrors `mInfo` (LLSD) in C++.
     */
    private var info: MutableMap<String, String> = mutableMapOf()

    private var markerFilename: String = ""
    private var connectTime: Long = 0L

    // ------------------------------------------------------------------
    // Public API — mirroring the C++ public interface
    // ------------------------------------------------------------------

    /**
     * Initiate the full Discord connection sequence.
     * Mirrors FSDiscordConnect::connectToDiscord() which launches
     * discordConnectCoro() via LLCoros.
     */
    fun connectToDiscord() {
        TODO("DISCORD: launch discordConnectCoro — call Discord_Initialize with API key and event handlers")
    }

    /**
     * Disconnect from Discord.
     * Mirrors FSDiscordConnect::disconnectFromDiscord() → discordDisconnectCoro().
     */
    fun disconnectFromDiscord() {
        setConnectionState(ConnectionState.DISCONNECTING)
        TODO("DISCORD: launch discordDisconnectCoro — call Discord_Shutdown then set NOT_CONNECTED")
    }

    /**
     * Check whether we are connected; optionally auto-connect.
     * Mirrors FSDiscordConnect::checkConnectionToDiscord(bool).
     *
     * @param autoConnect If `true` and not connected, initiate a connection.
     */
    fun checkConnectionToDiscord(autoConnect: Boolean = false) {
        TODO("DISCORD: launch discordConnectedCoro — check marker file, then connectToDiscord if autoConnect")
    }

    // ------------------------------------------------------------------
    // Rich Presence
    // ------------------------------------------------------------------

    /**
     * Push the current viewer state (region, avatar name, grid) to Discord
     * as Rich Presence data.
     * Mirrors FSDiscordConnect::updateRichPresence().
     *
     * @param details  Primary line shown in Discord (e.g. avatar name).
     * @param state    Secondary line (e.g. region name + coordinates).
     */
    fun setActivity(details: String, state: String) {
        if (!isConnected) return
        TODO("DISCORD: populate DiscordRichPresence struct and call Discord_UpdatePresence")
    }

    /** Clear the Rich Presence activity, keeping the connection alive. */
    fun clearActivity() {
        if (!isConnected) return
        TODO("DISCORD: call Discord_ClearPresence")
    }

    /**
     * Re-compute and push the full rich-presence payload from live viewer state.
     * Mirrors FSDiscordConnect::updateRichPresence() const.
     * Called on every mainloop tick via [tick].
     */
    fun updateRichPresence() {
        if (!isConnected) return
        TODO("DISCORD: read region/agent data, respect RLV + maturity + blacklist settings, call Discord_UpdatePresence")
    }

    // ------------------------------------------------------------------
    // Info store — mirrors storeInfo / getInfo / clearInfo
    // ------------------------------------------------------------------

    fun storeInfo(infoMap: Map<String, String>) {
        info.clear()
        info.putAll(infoMap)
        // In C++ this posts to sInfoWatcher event pump; stub that here.
        TODO("DISCORD: notify info watchers after storing")
    }

    fun getInfo(): Map<String, String> = info.toMap()

    fun clearInfo() {
        info.clear()
    }

    // ------------------------------------------------------------------
    // Connection state management
    // ------------------------------------------------------------------

    /**
     * Set the connection state and notify any registered observers.
     * Mirrors FSDiscordConnect::setConnectionState(EConnectionState).
     */
    fun setConnectionState(newState: ConnectionState) {
        if (newState == ConnectionState.CONNECTED) {
            setMarkerFile()
            connectTime = System.currentTimeMillis()
        } else if (newState == ConnectionState.NOT_CONNECTED) {
            clearMarkerFile()
        }

        if (connectionState != newState) {
            connectionState = newState
            TODO("DISCORD: post state-change event to sStateWatcher equivalent")
        }
    }

    // ------------------------------------------------------------------
    // Main-loop tick — mirrors FSDiscordConnect::Tick(const LLSD&)
    // ------------------------------------------------------------------

    /**
     * Called once per frame from the viewer main loop.
     * Runs pending Discord callbacks and refreshes Rich Presence.
     *
     * @return `false` so the listener stays registered (LLEventPump convention).
     */
    fun tick(): Boolean {
        TODO("DISCORD: call Discord_RunCallbacks(); then updateRichPresence()")
        @Suppress("UNREACHABLE_CODE")
        return false
    }

    // ------------------------------------------------------------------
    // Marker-file helpers — prevent two viewer instances sharing one
    // Discord connection.  Mirrors check/set/clearMarkerFile().
    // ------------------------------------------------------------------

    /**
     * Returns `true` if the marker file exists and belongs to a *different*
     * viewer instance (i.e. someone else is using Discord).
     */
    private fun checkMarkerFile(): Boolean {
        TODO("DISCORD: read marker file, compare stored UUID to gAgentID; return true if foreign instance owns it")
    }

    private fun setMarkerFile() {
        TODO("DISCORD: write gAgentID to marker file if not already owned by another instance")
    }

    private fun clearMarkerFile() {
        TODO("DISCORD: delete marker file if it belongs to this instance")
    }
}
