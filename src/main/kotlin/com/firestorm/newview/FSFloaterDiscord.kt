package com.firestorm.newview

enum class DiscordConnectionState {
    DISCORD_NOT_CONNECTED,
    DISCORD_CONNECTION_IN_PROGRESS,
    DISCORD_CONNECTED,
    DISCORD_CONNECTION_FAILED,
    DISCORD_DISCONNECTING
}

interface LLFloaterBase {
    fun postBuild(): Boolean
    fun draw()
    fun onClose(appQuitting: Boolean)
}

class FSFloaterDiscord(private val key: Any) : LLFloaterBase {

    private var accountCaptionLabel: String = ""
    private var accountNameLabel: String = ""
    private var connectButtonVisible: Boolean = true
    private var disconnectButtonVisible: Boolean = false
    private var blacklistedNames: MutableList<String> = mutableListOf()
    private var blacklistEntry: String = ""
    private var statusText: String? = null
    private var statusTextVisible: Boolean = false

    init {
        TODO("APR: use JVM equivalent — register commit callbacks for FSDiscord.Connect/Disconnect/Add/Rem and attach visibility change listener")
    }

    private fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            TODO("APR: use JVM equivalent — subscribe to DiscordConnectState and DiscordConnectInfo event pumps; query FSDiscordConnect for current info and connection state")
        } else {
            TODO("APR: use JVM equivalent — unsubscribe from DiscordConnectState and DiscordConnectInfo event pumps")
        }
    }

    private fun onDiscordConnectStateChange(data: Any): Boolean {
        TODO("APR: use JVM equivalent — check FSDiscordConnect.isConnected() and update accountCaptionLabel; call showConnectedLayout() or showDisconnectedLayout()")
    }

    private fun onDiscordConnectInfoChange(): Boolean {
        TODO("APR: use JVM equivalent — read info from FSDiscordConnect.getInfo(); if 'name' key present update accountNameLabel")
    }

    private fun showConnectButton() {
        if (!connectButtonVisible) {
            connectButtonVisible = true
            disconnectButtonVisible = false
        }
    }

    private fun hideConnectButton() {
        if (connectButtonVisible) {
            connectButtonVisible = false
            disconnectButtonVisible = true
        }
    }

    private fun showDisconnectedLayout() {
        accountCaptionLabel = "discord_disconnected"
        accountNameLabel = ""
        showConnectButton()
    }

    private fun showConnectedLayout() {
        accountCaptionLabel = "discord_connected"
        hideConnectButton()
    }

    private fun onConnect() {
        TODO("APR: use JVM equivalent — call FSDiscordConnect.checkConnectionToDiscord(true)")
    }

    private fun onDisconnect() {
        TODO("APR: use JVM equivalent — call FSDiscordConnect.disconnectFromDiscord()")
    }

    private fun onAdd() {
        val name = blacklistEntry.trim()
        if (name.isEmpty()) return
        val nameLower = name.lowercase()
        if (blacklistedNames.any { it.lowercase() == nameLower }) return
        blacklistedNames.add(name)
        TODO("APR: use JVM equivalent — persist updated blacklistedNames list to FSBlacklistedRegionNames per-account setting")
    }

    private fun onRemove() {
        TODO("APR: use JVM equivalent — remove selected items from blacklistedNames list and persist to FSBlacklistedRegionNames per-account setting")
    }

    override fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent — bind UI child widgets (accountCaptionLabel, accountNameLabel, connectButton, disconnectButton, blacklistedNames scroll list, blacklistEntry, statusText); load FSBlacklistedRegionNames from per-account settings into blacklistedNames list")
    }

    override fun draw() {
        statusTextVisible = false
        TODO("APR: use JVM equivalent — query FSDiscordConnect.getConnectionState() and set statusTextVisible + statusText string for IN_PROGRESS / FAILED / DISCONNECTING states; call super.draw()")
    }

    override fun onClose(appQuitting: Boolean) {
        if (appQuitting) {
            TODO("APR: use JVM equivalent — persist non-selected blacklistedNames items to FSBlacklistedRegionNames per-account setting on app quit")
        }
        TODO("APR: use JVM equivalent — call super.onClose(appQuitting)")
    }
}
