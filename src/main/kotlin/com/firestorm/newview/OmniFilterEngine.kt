package com.firestorm.newview

import java.util.UUID
import java.io.File
import java.nio.file.Files

object OmniFilterEngine {

    enum class EType {
        NearbyChat,
        GroupChat,
        GroupNotice,
        GroupInvite,
        InstantMessage,
        ObjectChat,
        ObjectInstantMessage,
        ScriptError,
        ScriptDialog,
        FriendshipOffer,
        InventoryOffer,
        Lure,
        TeleportRequest,
        URLRequest
    }

    enum class EMatchType {
        Exact,
        Substring,
        Regex
    }

    data class Haystack(
        val senderName: String = "",
        val content: String = "",
        val regionName: String = "",
        val ownerID: UUID? = null,
        val type: EType = EType.NearbyChat
    )

    data class Needle(
        var senderName: String = "",
        var content: String = "",
        var regionName: String = "",
        var chatReplace: String = "",
        var buttonReply: String = "",
        var textBoxReply: String = "",
        var senderNameMatchType: EMatchType = EMatchType.Substring,
        var contentMatchType: EMatchType = EMatchType.Substring,
        var ownerID: UUID? = null,
        var types: MutableSet<EType> = mutableSetOf(),
        var enabled: Boolean = false,
        var senderNameCaseInsensitive: Boolean = true,
        var contentCaseInsensitive: Boolean = true
    )

    private val needles: MutableMap<String, Needle> = mutableMapOf()
    private var needlesXmlPath: String = ""
    private var dirty: Boolean = false

    val log: MutableList<Pair<Long, String>> = mutableListOf()

    // Observers registered by the UI layer to receive log-line events.
    val logSignal: MutableList<(Long, String) -> Unit> = mutableListOf()

    // Periodic save is driven externally (e.g. a coroutine or timer calling tick()).
    private var timerRunning: Boolean = false

    fun init() {
        System.err.println("OmniFilterEngine: init not yet implemented")
        // needlesXmlPath = ...
        // loadNeedles()
    }

    fun getNeedleList(): MutableMap<String, Needle> = needles

    fun newNeedle(needleName: String): Needle {
        if (!needles.containsKey(needleName)) {
            needles[needleName] = Needle()
        }
        setDirty(true)
        return needles.getValue(needleName)
    }

    fun renameNeedle(oldName: String, newName: String) {
        val needle = needles.remove(oldName) ?: return
        needles[newName] = needle
        setDirty(true)
    }

    fun deleteNeedle(needleName: String) {
        needles.remove(needleName)
        setDirty(true)
    }

    fun match(haystack: Haystack): Needle? {
        for ((needleName, needle) in needles) {
            if (!needle.enabled) continue

            if (needle.senderName.isNotEmpty()) {
                if (!matchStrings(needle.senderName, haystack.senderName, needle.senderNameMatchType, needle.senderNameCaseInsensitive))
                    continue
            }

            if (needle.ownerID != null) {
                if (needle.ownerID != haystack.ownerID) continue
            }

            if (needle.regionName.isNotEmpty()) {
                if (needle.regionName != haystack.regionName) continue
            }

            if (needle.types.isNotEmpty() && !needle.types.contains(haystack.type)) continue

            if (needle.content.isEmpty() || matchStrings(needle.content, haystack.content, needle.contentMatchType, needle.contentCaseInsensitive)) {
                return logMatch(needleName, needle)
            }
        }
        return null
    }

    fun setDirty(value: Boolean) {
        dirty = value
        timerRunning = value
    }

    fun tick(): Boolean {
        saveNeedles()
        setDirty(false)
        return false
    }

    private fun logMatch(needleName: String, needle: Needle): Needle {
        val now = System.currentTimeMillis() / 1000L
        log.add(Pair(now, needleName))
        logSignal.forEach { it(now, needleName) }
        return needle
    }

    private fun matchStrings(
        needleString: String,
        haystackString: String,
        matchType: EMatchType,
        caseInsensitive: Boolean
    ): Boolean {
        val n = if (caseInsensitive) needleString.lowercase() else needleString
        val h = if (caseInsensitive) haystackString.lowercase() else haystackString

        return when (matchType) {
            EMatchType.Exact     -> h == n
            EMatchType.Substring -> h.contains(n)
            EMatchType.Regex     -> {
                val opts = if (caseInsensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
                Regex(needleString, opts).matches(haystackString)
            }
        }
    }

    private fun loadNeedles() {
        if (needlesXmlPath.isEmpty()) return

        val file = File(needlesXmlPath)
        if (!file.exists()) return

        if (!file.isFile) {
            System.err.println("OmniFilterEngine: loadNeedles not yet implemented")
        }

        if (file.length() == 0L) {
            System.err.println("OmniFilterEngine: loadNeedles not yet implemented")
        }

        System.err.println("OmniFilterEngine: loadNeedles not yet implemented")
    }

    private fun saveNeedles() {
        if (needlesXmlPath.isEmpty()) return
        System.err.println("OmniFilterEngine: saveNeedles not yet implemented")
    }
}
