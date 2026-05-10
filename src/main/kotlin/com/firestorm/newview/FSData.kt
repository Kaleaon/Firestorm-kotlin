/**
 * @file FSData.kt
 * @brief Downloadable dynamic XML data service for Firestorm viewer features.
 *
 * Ported from fsdata.h / fsdata.cpp
 * Original copyright (C) 2011-2013 Techwolf Lupindo; portions (C) Wolfspirit Magic,
 * Ansariel Hiller @ Second Life.
 * Phoenix Firestorm Project — LGPL v2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val LEGACY_CLIENT_LIST_URL =
    "http://phoenixviewer.com/app/client_tags/client_list_v2.xml"

private const val FS_BASE_URL     = "http://phoenixviewer.com/app/fsdata"
private const val FS_BASE_QA_URL  = "http://phoenixviewer.com/app/fsdatatest"

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

/**
 * Bitmask flags attached to each Firestorm team agent entry.
 *
 * Maps directly to `FSData::flags_t` in C++.
 */
object AgentFlags {
    const val SUPPORT   = 1 shl 0  // 0x01
    const val DEVELOPER = 1 shl 1  // 0x02
    const val QA        = 1 shl 2  // 0x04
    const val CHAT_COLOR = 1 shl 3 // 0x08
    const val NO_SUPPORT = 1 shl 4 // 0x10
    const val NO_USE     = 1 shl 5 // 0x20
    const val NO_SPAM    = 1 shl 6 // 0x40
    const val GATEWAY    = 1 shl 7 // 0x80
}

/**
 * Lightweight view of a cached system-info block returned to support staff
 * via the `/reqsysinfo` command.
 */
data class SystemInfoReport(val part1: String, val part2: String)

// ---------------------------------------------------------------------------
// FSData singleton
// ---------------------------------------------------------------------------

/**
 * Singleton that downloads and caches the Firestorm agent/data/assets XML
 * feeds from the Firestorm data service.
 *
 * Mirrors `class FSData : public LLSingleton<FSData>` in C++.
 */
object FSData {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Map of known Firestorm team agents to their flag bitmasks. */
    private val teamAgents: MutableMap<LLUUID, Int> = mutableMapOf()

    /** Set of UUIDs that are recognised Firestorm support groups. */
    private val supportGroups: MutableSet<LLUUID> = mutableSetOf()

    /** Set of UUIDs that are recognised Firestorm testing groups. */
    private val testingGroups: MutableSet<LLUUID> = mutableSetOf()

    /** Versions that are blocked from logging in, keyed by version string. */
    private val blockedVersions: MutableMap<String, Map<String, Any>> = mutableMapOf()

    /** Cached legacy (V1-era) client-tag list. */
    private var legacyClientList: Map<String, Any> = emptyMap()

    /** Random MOTD entries loaded from the data feed. */
    private var randomMotds: List<String> = emptyList()

    private var secondLifeMotd: String = ""
    private var openSimMotd: String = ""

    /** When `false`, legacy people-search is unavailable for this grid. */
    var legacySearchEnabled: Boolean = true
        private set

    /** `true` once the main `data.xml` download/load cycle is complete. */
    var isFSDataDone: Boolean = false
        private set

    /** `true` once the `agents.xml` download/load cycle is complete. */
    var isAgentsDone: Boolean = false
        private set

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Begin asynchronous downloads of `data.xml` (and `defaults.xml`).
     * Call this just before the login screen, after the HTTP proxy is set up.
     */
    fun startDownload() {
        TODO("Requires HTTP coroutine / FSCoreHttpUtil integration")
    }

    /**
     * Begin asynchronous download of `agents.xml` (and `assets.xml`).
     * Call this *after* the login screen so the grid URL is known.
     */
    fun downloadAgents() {
        TODO("Requires HTTP coroutine / grid-manager integration")
    }

    // -----------------------------------------------------------------------
    // Data ingestion
    // -----------------------------------------------------------------------

    /**
     * Dispatch a completed HTTP response [content] for [url] to the
     * appropriate processing method.
     *
     * @param content       Parsed LLSD payload (or empty if unavailable).
     * @param url           The URL that was fetched.
     * @param saveToFile    `true` when [content] came from the network
     *                      (should be persisted); `false` when loaded from disk.
     * @param lastModified  HTTP `Last-Modified` epoch value for file stamping.
     */
    fun processResponder(
        content: Map<String, Any>,
        url: String,
        saveToFile: Boolean,
        lastModified: Long,
    ) {
        TODO("Requires URL routing to processData / processAgents / processAssets")
    }

    /**
     * Register NO_SPAM agents with the mute-list and set up avatar-name-cache
     * callbacks.  Called after [isAgentsDone] becomes `true` and the name
     * cache (`gCacheName`) is ready.
     */
    fun addAgents() {
        TODO("Requires avatar name cache and mute-list integration")
    }

    // -----------------------------------------------------------------------
    // Agent-flag queries
    // -----------------------------------------------------------------------

    /**
     * Return the raw flag bitmask for [avatarId], or `null` if the agent is
     * not in the team list (C++ returns `-1` for "not found").
     */
    fun getAgentFlags(avatarId: LLUUID): Int? = teamAgents[avatarId]

    /**
     * Return `true` when [avatarId] has the [AgentFlags.DEVELOPER] bit set.
     */
    fun isDeveloper(avatarId: LLUUID): Boolean =
        teamAgents[avatarId]?.let { it and AgentFlags.DEVELOPER != 0 } ?: false

    /**
     * Return `true` when [avatarId] has the [AgentFlags.SUPPORT] bit set.
     */
    fun isSupport(avatarId: LLUUID): Boolean =
        teamAgents[avatarId]?.let { it and AgentFlags.SUPPORT != 0 } ?: false

    /**
     * Return `true` when [avatarId] has the [AgentFlags.QA] bit set.
     */
    fun isQA(avatarId: LLUUID): Boolean =
        teamAgents[avatarId]?.let { it and AgentFlags.QA != 0 } ?: false

    /**
     * Return `true` when [avatarId] has [flag] set in their flag bitmask.
     */
    fun isAgentFlag(avatarId: LLUUID, flag: Int): Boolean =
        teamAgents[avatarId]?.let { it and flag != 0 } ?: false

    // -----------------------------------------------------------------------
    // Group queries
    // -----------------------------------------------------------------------

    /** `true` when [id] is either a support or a testing group. */
    fun isFirestormGroup(id: LLUUID): Boolean = isSupportGroup(id) || isTestingGroup(id)

    /** `true` when [id] is in the support-group set. */
    fun isSupportGroup(id: LLUUID): Boolean = id in supportGroups

    /** `true` when [id] is in the testing-group set. */
    fun isTestingGroup(id: LLUUID): Boolean = id in testingGroups

    // -----------------------------------------------------------------------
    // Login gating
    // -----------------------------------------------------------------------

    /**
     * Return the blocked-version data for the current viewer version, or
     * `null` if the viewer is allowed to log in.
     *
     * C++ returns an empty `LLSD()` for "allowed"; a non-empty map means
     * the version is blocked and the map describes why / for which grids.
     */
    fun allowedLogin(): Map<String, Any>? {
        TODO("Requires LLVersionInfo and grid-manager integration")
    }

    // -----------------------------------------------------------------------
    // MOTD
    // -----------------------------------------------------------------------

    /** The current OpenSim message-of-the-day string. */
    fun getOpenSimMOTD(): String = openSimMotd

    /**
     * Rotate to the next random MOTD entry (used on teleport when no fixed
     * MOTD is configured).
     */
    fun selectNextMOTD() {
        if (secondLifeMotd.isNotEmpty()) {
            // Fixed MOTD takes priority — nothing to rotate.
            return
        }
        if (randomMotds.isNotEmpty()) {
            val next = randomMotds.random()
            // TODO: assign next MOTD to gAgent.mMOTD equivalent
            println("Next MOTD: $next")
        }
    }

    // -----------------------------------------------------------------------
    // Legacy client-tag resolution
    // -----------------------------------------------------------------------

    /**
     * Build a tag descriptor map for [id] based on the legacy client-tag list
     * and the new-system colour/name data.
     *
     * @param id             Client-tag UUID embedded in the agent's appearance.
     * @param newSystem      `true` when using the new tag-in-UUID system.
     * @param newSystemColor Colour hint from the new tag system.
     * @return A map suitable for rendering the client tag badge.
     */
    fun resolveClientTag(
        id: LLUUID,
        newSystem: Boolean,
        newSystemColor: FloatArray,
    ): MutableMap<String, Any> {
        val tag: MutableMap<String, Any> = mutableMapOf(
            "uuid" to id.toString(),
            "id_based" to newSystem,
            "tex_color" to newSystemColor,
        )
        // TODO: implement full legacy-tag lookup and new-system filtering
        return tag
    }

    // -----------------------------------------------------------------------
    // System-info / support-request helpers
    // -----------------------------------------------------------------------

    /**
     * If [message] starts with `/reqsysinfo` and [requester] is a team
     * member, show the system-info request notification.
     *
     * @return The original [message], or a localised acknowledgement string
     *         when the request is accepted.
     */
    fun processRequestForInfo(
        requester: LLUUID,
        message: String,
        name: String,
        sessionId: LLUUID,
    ): String {
        val detectString = "/reqsysinfo"
        if (!message.startsWith(detectString)) return message
        if (!isSupport(requester) && !isDeveloper(requester) && !isQA(requester)) return message

        // TODO: show notification and collect user response
        TODO("Requires notification system integration")
    }

    /**
     * Gather viewer hardware/software diagnostics and return a two-part
     * system-info report for sharing with support staff.
     */
    fun getSystemInfo(): SystemInfoReport {
        TODO("Requires LLAppViewer::getViewerInfo() equivalent")
    }

    // -----------------------------------------------------------------------
    // Internal helpers (private in C++)
    // -----------------------------------------------------------------------

    private fun processData(fsData: Map<String, Any>) {
        TODO("Process MOTD, blocked versions, agents, assets, client tags, RLVa compat list")
    }

    private fun processAgents(data: Map<String, Any>) {
        // Supports both new "Agents" format and legacy "SupportAgents" format.
        TODO("Populate teamAgents, supportGroups, testingGroups; set legacySearchEnabled")
    }

    private fun processAssets(assets: Map<String, Any>) {
        TODO("Decrypt asset UUIDs with MAGIC_ID XOR cipher and add to asset blacklist")
    }

    private fun processClientTags(tags: Map<String, Any>) {
        if (tags.containsKey("isComplete")) {
            @Suppress("UNCHECKED_CAST")
            legacyClientList = tags
        }
    }

    private fun saveLLSD(data: Map<String, Any>, filename: String, lastModified: Long) {
        TODO("Serialize data as XML and set file modification timestamp")
    }

    private fun loadFromFile(filename: String): Map<String, Any>? {
        TODO("Deserialize LLSD XML from disk; return null on failure")
    }

    private fun updateClientTagsLocal() {
        TODO("Load legacy client tags from the cached file on disk")
    }

    private fun onNameCache(avId: LLUUID, avName: AvatarName) {
        TODO("Add NO_SPAM agent to mute list when their name resolves")
    }

    private fun sendInfo(
        destination: LLUUID,
        sessionId: LLUUID,
        myName: String,
    ) {
        TODO("Pack and send two-part system-info IM to destination")
    }
}
