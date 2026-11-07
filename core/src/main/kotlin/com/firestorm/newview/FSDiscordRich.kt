package com.firestorm.newview

import java.util.UUID

enum class DiscordConnectionState {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    CONNECTION_FAILED,
    DISCONNECTING,
}

data class RichPresence(
    val state: String = "",
    val details: String = "",
    val startTimestamp: Long = 0L,
    val largeImageKey: String = "",
    val largeImageText: String = "",
    val smallImageKey: String = "",
    val smallImageText: String = "",
    val partyId: String = "",
    val partySize: Int = 0,
    val partyMax: Int = 0,
)

object FSDiscordRich {

    var connectionState: DiscordConnectionState = DiscordConnectionState.NOT_CONNECTED
        private set

    val isConnected: Boolean get() = connectionState == DiscordConnectionState.CONNECTED

    private val info: MutableMap<String, String> = mutableMapOf()
    private var connectTime: Long = 0L
    private var markerFilename: String = ""

    val onConnectionStateChanged: MutableList<(DiscordConnectionState) -> Unit> = mutableListOf()
    val onInfoChanged: MutableList<(Map<String, String>) -> Unit> = mutableListOf()

    fun init(userSettingsDir: String) {
        markerFilename = "$userSettingsDir/discord_in_use_marker"
    }

    fun connectToDiscord() {
        System.err.println("FSDiscordRich: connectToDiscord not yet implemented")
    }

    fun disconnectFromDiscord() {
        setConnectionState(DiscordConnectionState.DISCONNECTING)
        System.err.println("FSDiscordRich: disconnectFromDiscord not yet implemented")
    }

    fun checkConnectionToDiscord(autoConnect: Boolean = false) {
        setConnectionState(DiscordConnectionState.CONNECTING)
        if (autoConnect) {
            if (!checkMarkerFile()) {
                connectToDiscord()
            } else {
                setConnectionState(DiscordConnectionState.CONNECTION_FAILED)
            }
        }
    }

    fun tick(): Boolean {
        return false
    }

    fun updateRichPresence(
        agentId: UUID,
        agentName: String?,
        agentUsername: String,
        regionName: String?,
        regionId: UUID?,
        posX: Float,
        posY: Float,
        posZ: Float,
        gridLabel: String,
        partySize: Int,
        partyMax: Int,
        maxMaturity: UInt,
        regionMaturity: UByte,
        blacklistedRegions: List<String>,
        rlvHidesLocation: Boolean,
        rlvHidesName: Boolean,
        shareNameToDiscord: Boolean,
        isOpenSim: Boolean,
    ) {
        if (!isConnected) return

        val regionVisible = !rlvHidesLocation
            && regionMaturity.toUInt() <= maxMaturity
            && blacklistedRegions.none { it.equals(regionName, ignoreCase = true) }

        val state = if (regionVisible && regionName != null) {
            "$regionName (%.0f, %.0f, %.0f)".format(posX, posY, posZ)
        } else {
            "Hidden Region"
        }

        val details = if (!rlvHidesName && shareNameToDiscord) {
            agentName ?: agentUsername
        } else {
            ""
        }

        val largeImageKey = if (isOpenSim) "opensimulator_512" else "secondlife_512"

        val presence = RichPresence(
            state = state,
            details = details,
            startTimestamp = connectTime,
            largeImageKey = largeImageKey,
            largeImageText = gridLabel,
            smallImageKey = "firestorm_512",
            smallImageText = "via Firestorm",
            partyId = regionId?.toString() ?: "",
            partySize = partySize,
            partyMax = partyMax,
        )

        applyPresence(presence)
    }

    private fun applyPresence(presence: RichPresence) {
        System.err.println("FSDiscordRich: applyPresence not yet implemented")
    }

    fun storeInfo(infoMap: Map<String, String>) {
        info.clear()
        info.putAll(infoMap)
        onInfoChanged.forEach { it(info.toMap()) }
    }

    fun getInfo(): Map<String, String> = info.toMap()

    fun clearInfo() { info.clear() }

    fun setConnectionState(newState: DiscordConnectionState) {
        if (newState == DiscordConnectionState.CONNECTED) {
            setMarkerFile()
            connectTime = System.currentTimeMillis() / 1000L
        } else if (newState == DiscordConnectionState.NOT_CONNECTED) {
            clearMarkerFile()
        }

        if (connectionState != newState) {
            connectionState = newState
            onConnectionStateChanged.forEach { it(newState) }
        }
    }

    private fun checkMarkerFile(): Boolean {
        return false
    }

    private fun setMarkerFile() {
        System.err.println("FSDiscordRich: setMarkerFile not yet implemented")
    }

    private fun clearMarkerFile() {
        System.err.println("FSDiscordRich: clearMarkerFile not yet implemented")
    }
}

class FSFloaterDiscord {

    private var accountCaptionText: String = ""
    private var accountNameText: String = ""
    private var connectButtonVisible: Boolean = true
    private var statusText: String = ""
    private var blacklistedRegions: MutableList<String> = mutableListOf()
    private var blacklistEntry: String = ""

    fun postBuild(): Boolean {
        val savedList = loadBlacklistedRegionNames()
        blacklistedRegions.clear()
        blacklistedRegions.addAll(savedList)

        FSDiscordRich.onConnectionStateChanged.add { _ -> onDiscordConnectStateChange() }
        FSDiscordRich.onInfoChanged.add { _ -> onDiscordConnectInfoChange() }

        return true
    }

    fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            val info = FSDiscordRich.getInfo()
            if (info.containsKey("name")) accountNameText = info["name"]!!

            if (FSDiscordRich.isConnected) showConnectedLayout() else showDisconnectedLayout()

            val state = FSDiscordRich.connectionState
            if (state == DiscordConnectionState.NOT_CONNECTED || state == DiscordConnectionState.CONNECTION_FAILED) {
                FSDiscordRich.checkConnectionToDiscord()
            }
        }
    }

    private fun onDiscordConnectStateChange() {
        if (FSDiscordRich.isConnected) {
            accountCaptionText = "Connected"
            showConnectedLayout()
        } else {
            accountCaptionText = "Disconnected"
            showDisconnectedLayout()
        }
    }

    private fun onDiscordConnectInfoChange() {
        val info = FSDiscordRich.getInfo()
        if (info.containsKey("name")) accountNameText = info["name"]!!
    }

    private fun showConnectButton() {
        connectButtonVisible = true
    }

    private fun hideConnectButton() {
        connectButtonVisible = false
    }

    private fun showDisconnectedLayout() {
        accountCaptionText = "Disconnected"
        accountNameText = ""
        showConnectButton()
    }

    private fun showConnectedLayout() {
        accountCaptionText = "Connected"
        hideConnectButton()
    }

    fun onConnect() {
        FSDiscordRich.checkConnectionToDiscord(autoConnect = true)
    }

    fun onDisconnect() {
        FSDiscordRich.disconnectFromDiscord()
    }

    fun onAddBlacklistEntry(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val lower = trimmed.lowercase()
        if (blacklistedRegions.any { it.lowercase() == lower }) return
        blacklistedRegions.add(trimmed)
        saveBlacklistedRegionNames(blacklistedRegions)
    }

    fun onRemoveSelectedBlacklistEntries(selectedIndices: Set<Int>) {
        val kept = blacklistedRegions.filterIndexed { i, _ -> i !in selectedIndices }
        blacklistedRegions.clear()
        blacklistedRegions.addAll(kept)
        saveBlacklistedRegionNames(blacklistedRegions)
    }

    fun draw(): String {
        return when (FSDiscordRich.connectionState) {
            DiscordConnectionState.NOT_CONNECTED -> ""
            DiscordConnectionState.CONNECTING -> "Connecting to Discord…"
            DiscordConnectionState.CONNECTED -> ""
            DiscordConnectionState.CONNECTION_FAILED -> "Error connecting to Discord"
            DiscordConnectionState.DISCONNECTING -> "Disconnecting from Discord…"
        }
    }

    fun onClose(appQuitting: Boolean) {
        if (appQuitting) saveBlacklistedRegionNames(blacklistedRegions)
    }

    private fun loadBlacklistedRegionNames(): List<String> {
        return emptyList()
    }

    private fun saveBlacklistedRegionNames(names: List<String>) {
        System.err.println("FSFloaterDiscord: saveBlacklistedRegionNames not yet implemented")
    }
}
