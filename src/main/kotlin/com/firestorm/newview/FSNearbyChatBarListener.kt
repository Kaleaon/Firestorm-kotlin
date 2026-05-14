package com.firestorm.newview

private const val CHAT_THROTTLE_PERIOD = 1.0

class FSNearbyChatBarListener {

    private var lastThrottleTime: Double = 0.0

    init {
        registerEventApi()
    }

    private fun registerEventApi() {
        System.err.println("FSNearbyChatBarListener: registerEventApi not yet implemented")
    }

    private fun sendChat(chatData: Map<String, Any>) {
        val curTime = getElapsedSeconds()
        if (curTime < lastThrottleTime + CHAT_THROTTLE_PERIOD) {
            return
        }
        lastThrottleTime = curTime

        var chatText = chatData["message"] as? String ?: ""

        var channel = 0
        if (chatData.containsKey("channel")) {
            val requested = (chatData["channel"] as? Number)?.toInt() ?: 0
            channel = if (requested < 0 || requested >= CHAT_CHANNEL_DEBUG) 0 else requested
        }

        val typeString = chatData["type"] as? String ?: "normal"
        val chatType: EChatType = when (typeString) {
            "whisper" -> EChatType.WHISPER
            "shout" -> EChatType.SHOUT
            else -> EChatType.NORMAL
        }

        if (channel != 0) {
            chatText = "/${chatData["channel"]} $chatText"
        }

        sendChatFromViewer(chatText, chatType, channel == 0 && getPlayChatAnim())
    }

    private fun sendChatFromViewer(text: String, type: EChatType, playAnim: Boolean) {
        System.err.println("FSNearbyChatBarListener: sendChatFromViewer not yet implemented")
    }

    private fun getElapsedSeconds(): Double {
        return 0.0
    }

    private fun getPlayChatAnim(): Boolean {
        return false
    }

    enum class EChatType { NORMAL, WHISPER, SHOUT }

    private companion object {
        const val CHAT_CHANNEL_DEBUG = 2147483647
    }
}
