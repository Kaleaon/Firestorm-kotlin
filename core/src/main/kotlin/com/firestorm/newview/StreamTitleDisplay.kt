package com.firestorm.newview

interface StreamingAudio {
    fun getCurrentMetadata(): Map<String, String>
    fun setMetadataUpdateCallback(callback: (Map<String, String>) -> Unit): (() -> Unit)?
}

interface ChatSender {
    fun sendChatOnChannel(message: String, channel: Int, chatType: Int)
}

interface NotificationAdder {
    fun add(name: String, substitutions: Map<String, String> = emptyMap())
}

interface TranslationProvider {
    fun getString(key: String): String
}

interface NearbyChat {
    fun reportToNearbyChat(message: String)
}

const val CHAT_TYPE_WHISPER = 2

object StreamTitleDisplay {

    private var metadataUpdateConnection: (() -> Unit)? = null

    lateinit var savedSettings: (String) -> Any?
    lateinit var chatSender: ChatSender
    lateinit var notificationAdder: NotificationAdder
    lateinit var trans: TranslationProvider
    lateinit var nearbyChat: NearbyChat

    fun initSingleton(streamingAudio: StreamingAudio?) {
        if (streamingAudio == null) return

        metadataUpdateConnection = streamingAudio.setMetadataUpdateCallback { metadata ->
            checkMetadata(metadata)
        }
        checkMetadata(streamingAudio.getCurrentMetadata())
    }

    fun destroy() {
        metadataUpdateConnection?.invoke()
        metadataUpdateConnection = null
    }

    protected fun checkMetadata(metadata: Map<String, String>) {
        val showStreamMetadata = (savedSettings("ShowStreamMetadata") as? Int) ?: 1
        val announceToChat = (savedSettings("StreamMetadataAnnounceToChat") as? Boolean) ?: false

        if (showStreamMetadata > 0 || announceToChat) {
            var chat = ""
            metadata["ARTIST"]?.let { chat = it }
            metadata["TITLE"]?.let {
                if (chat.isNotEmpty()) chat += " - "
                chat += it
            }

            if (chat.isNotEmpty()) {
                if (announceToChat) {
                    sendStreamTitleToChat(chat)
                }

                when {
                    showStreamMetadata > 1 -> {
                        val msg = trans.getString("StreamtitleNowPlaying") + " " + chat
                        nearbyChat.reportToNearbyChat(msg)
                    }
                    showStreamMetadata == 1 && (metadata.containsKey("TITLE") || metadata.containsKey("ARTIST")) -> {
                        val substitutions = metadata.toMutableMap()
                        if (!substitutions.containsKey("TITLE")) substitutions["TITLE"] = ""
                        val notifName = if (metadata.containsKey("ARTIST")) "StreamMetadata" else "StreamMetadataNoArtist"
                        notificationAdder.add(notifName, substitutions)
                    }
                }
            }
        }
    }

    protected fun sendStreamTitleToChat(title: String) {
        val channel = (savedSettings("StreamMetadataAnnounceChannel") as? Int) ?: 0
        if (channel != 0) {
            chatSender.sendChatOnChannel(title, channel, CHAT_TYPE_WHISPER)
        }
    }
}
