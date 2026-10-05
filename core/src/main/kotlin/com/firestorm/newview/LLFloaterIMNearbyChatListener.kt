package com.firestorm.newview

private const val CHAT_THROTTLE_PERIOD: Float = 1f

class LLFloaterIMNearbyChatListener : LLEventAPI(
    "LLChatBar",
    "LLChatBar listener to (e.g.) sendChat, etc."
) {

    private var mLastThrottleTime: Double = 0.0

    init {
        add(
            "sendChat",
            "Send chat to the simulator:\n" +
                    "[\"message\"] chat message text [required]\n" +
                    "[\"channel\"] chat channel number [default = 0]\n" +
                    "[\"type\"] chat type \"whisper\", \"normal\", \"shout\" [default = \"normal\"]"
        ) { chatData -> sendChat(chatData) }
    }

    private fun sendChat(chatData: Map<String, Any>) {
        val curTime = LLTimer.getElapsedSeconds()
        if (curTime < mLastThrottleTime + CHAT_THROTTLE_PERIOD) {
            return
        }
        mLastThrottleTime = curTime

        val chatText = chatData["message"]?.toString() ?: ""

        var channel = 0
        if (chatData.containsKey("channel")) {
            val requested = chatData["channel"].toString().toIntOrNull() ?: 0
            if (requested >= 0 && requested < CHAT_CHANNEL_DEBUG) {
                channel = requested
            }
        }

        var typeOChat = EChatType.CHAT_TYPE_NORMAL
        val typeString = chatData["type"]?.toString() ?: ""
        typeOChat = when (typeString) {
            "whisper" -> EChatType.CHAT_TYPE_WHISPER
            "shout" -> EChatType.CHAT_TYPE_SHOUT
            else -> EChatType.CHAT_TYPE_NORMAL
        }

        val finalText = if (channel != 0) {
            "/$channel $chatText"
        } else {
            chatText
        }

        LLFloaterIMNearbyChat.sendChatFromViewer(
            finalText,
            typeOChat,
            channel == 0 && gSavedSettings.getBOOL("PlayChatAnim")
        )
    }
}
