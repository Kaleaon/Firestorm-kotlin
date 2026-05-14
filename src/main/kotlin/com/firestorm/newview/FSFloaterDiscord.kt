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
        System.err.println("FSFloaterDiscord: register commit callbacks for FSDiscord.Connect/Disconnect/Add/Rem and attach visibility change listener not yet implemented")
    }

    private fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            System.err.println("FSFloaterDiscord: subscribe to DiscordConnectState and DiscordConnectInfo event pumps not yet implemented")
        } else {
            System.err.println("FSFloaterDiscord: unsubscribe from DiscordConnectState and DiscordConnectInfo event pumps not yet implemented")
        }
    }

    private fun onDiscordConnectStateChange(data: Any): Boolean {
        System.err.println("FSFloaterDiscord: onDiscordConnectStateChange not yet implemented")
        return false
    }

    private fun onDiscordConnectInfoChange(): Boolean {
        System.err.println("FSFloaterDiscord: onDiscordConnectInfoChange not yet implemented")
        return false
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
        System.err.println("FSFloaterDiscord: onConnect not yet implemented")
    }

    private fun onDisconnect() {
        System.err.println("FSFloaterDiscord: onDisconnect not yet implemented")
    }

    private fun onAdd() {
        val name = blacklistEntry.trim()
        if (name.isEmpty()) return
        val nameLower = name.lowercase()
        if (blacklistedNames.any { it.lowercase() == nameLower }) return
        blacklistedNames.add(name)
        System.err.println("FSFloaterDiscord: persist updated blacklistedNames list to FSBlacklistedRegionNames not yet implemented")
    }

    private fun onRemove() {
        System.err.println("FSFloaterDiscord: onRemove not yet implemented")
    }

    override fun postBuild(): Boolean {
        System.err.println("FSFloaterDiscord: postBuild not yet implemented")
        return false
    }

    override fun draw() {
        statusTextVisible = false
        System.err.println("FSFloaterDiscord: draw not yet implemented")
    }

    override fun onClose(appQuitting: Boolean) {
        if (appQuitting) {
            System.err.println("FSFloaterDiscord: persist non-selected blacklistedNames items to FSBlacklistedRegionNames on app quit not yet implemented")
        }
        System.err.println("FSFloaterDiscord: onClose not yet implemented")
    }
}
