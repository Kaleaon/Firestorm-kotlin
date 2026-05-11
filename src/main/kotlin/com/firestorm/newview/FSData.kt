package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val LEGACY_CLIENT_LIST_URL =
    "http://phoenixviewer.com/app/client_tags/client_list_v2.xml"

private val MAGIC_ID: UUID = UUID.fromString("3c115e51-04f4-523c-9fa6-98aff1034730")

// Known-viewer UUIDs for legacy client-tag resolution
private val ID_SINGULARITY: UUID = UUID.fromString("f25263b7-6167-4f34-a4ef-af65213b2e39")
private val ID_KOKUA: UUID      = UUID.fromString("4b6f6b75-bf77-d1ff-0000-000000000000")
private val ID_RADEGAST: UUID   = UUID.fromString("b748af88-58e2-995b-cf26-9486dea8e830")
private val ID_IMPRUDENCE: UUID = UUID.fromString("cc7a030f-282f-c165-44d2-b5ee572e72bf")
private val ID_TEAPOT: UUID     = UUID.fromString("07eab070-0000-0000-0000-546561706f7f")

// ---------------------------------------------------------------------------
// Agent-flag bitmask constants  (mirrors FSData::flags_t)
// ---------------------------------------------------------------------------

object FSAgentFlags {
    const val SUPPORT    = 1 shl 0  // 0x01
    const val DEVELOPER  = 1 shl 1  // 0x02
    const val QA         = 1 shl 2  // 0x04
    const val CHAT_COLOR = 1 shl 3  // 0x08
    const val NO_SUPPORT = 1 shl 4  // 0x10
    const val NO_USE     = 1 shl 5  // 0x20
    const val NO_SPAM    = 1 shl 6  // 0x40
    const val GATEWAY    = 1 shl 7  // 0x80
}

// ---------------------------------------------------------------------------
// FSData singleton  (LLSingleton<FSData> → Kotlin object)
// ---------------------------------------------------------------------------

object FSData {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private val teamAgents: MutableMap<UUID, Int> = mutableMapOf()
    private val blockedVersions: MutableMap<String, Map<String, Any>> = mutableMapOf()
    private val supportGroup: MutableSet<UUID> = mutableSetOf()
    private val testingGroup: MutableSet<UUID> = mutableSetOf()

    private var legacyClientList: Map<String, Any> = emptyMap()
    private var randomMotds: List<String> = emptyList()
    private var secondLifeMotd: String = ""
    private var openSimMotd: String = ""

    var legacySearchEnabled: Boolean = true
        private set
    var isFSDataDone: Boolean = false
        private set
    var isAgentsDone: Boolean = false
        private set

    // HTTP headers sent with every download request
    private var headers: MutableMap<String, String> = mutableMapOf()

    // URL fields populated in constructor / startDownload
    private var baseUrl: String = ""
    private var fsDataUrl: String = ""
    private var agentsUrl: String = ""
    private var assetsUrl: String = ""
    private var fsDataDefaultsUrl: String = ""

    // Filenames resolved at runtime
    private var fsDataFilename: String = ""
    private var fsDataDefaultsFilename: String = ""
    private var agentsFilename: String = ""
    private var assetsFilename: String = ""
    private var clientTagsFilename: String = ""

    // Avatar-name-cache connection slots  (nullable lambda = boost::signals2::connection)
    private val avatarNameCacheConnections: MutableMap<UUID, (() -> Unit)?> = mutableMapOf()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun init() {
        headers["User-Agent"] = TODO("APR: use JVM equivalent - LLViewerMedia::getCurrentUserAgent()") as String
        headers["viewer-version"] = TODO("APR: use JVM equivalent - LLVersionInfo::getChannelAndVersionFS()") as String
        val qaTest = TODO("GPU: gSavedSettings.getBOOL(\"FSdataQAtest\")") as Boolean
        baseUrl = if (qaTest) "http://phoenixviewer.com/app/fsdatatest" else "http://phoenixviewer.com/app/fsdata"
        fsDataUrl = "$baseUrl/data.xml"
    }

    // -----------------------------------------------------------------------
    // Download initiation
    // -----------------------------------------------------------------------

    fun startDownload() {
        fsDataFilename = TODO("APR: use JVM equivalent - gDirUtilp->getExpandedFilename(LL_PATH_USER_SETTINGS, \"fsdata.xml\")") as String
        fsDataDefaultsFilename = TODO("APR: use JVM equivalent - gDirUtilp path for fsdata_defaults.<version>.xml") as String
        clientTagsFilename = TODO("APR: use JVM equivalent - gDirUtilp path for client_list_v2.xml") as String

        TODO("APR: use JVM equivalent - HTTP GET fsDataUrl with If-Modified-Since; on success call processResponder(content, fsDataUrl, true/false, lastModified)")
        TODO("APR: use JVM equivalent - HTTP GET fsDataDefaultsUrl with If-Modified-Since")
    }

    fun downloadAgents() {
        agentsUrl = "$baseUrl/agents.xml"
        assetsUrl = "$baseUrl/assets.xml"

        if (agentsUrl.isNotEmpty()) {
            agentsFilename = TODO("APR: use JVM equivalent - gDirUtilp path for <prefix>_agents.xml") as String
            TODO("APR: use JVM equivalent - HTTP GET agentsUrl with If-Modified-Since; on success call processResponder")
        }
        if (assetsUrl.isNotEmpty()) {
            assetsFilename = TODO("APR: use JVM equivalent - gDirUtilp path for <prefix>_assets.xml") as String
            TODO("APR: use JVM equivalent - HTTP GET assetsUrl with If-Modified-Since; on success call processResponder")
        }
    }

    // -----------------------------------------------------------------------
    // HTTP response dispatcher
    // -----------------------------------------------------------------------

    fun processResponder(
        content: Map<String, Any>,
        url: String,
        saveToFile: Boolean,
        lastModified: Long
    ) {
        when (url) {
            fsDataUrl -> {
                if (!saveToFile) {
                    val data = loadFromFile(fsDataFilename)
                    if (data != null) processData(data)
                } else {
                    processData(content)
                    saveLLSD(content, fsDataFilename, lastModified)
                }
                isFSDataDone = true
            }
            assetsUrl -> {
                if (!saveToFile) {
                    val data = loadFromFile(assetsFilename)
                    if (data != null) processAssets(data)
                } else {
                    processAssets(content)
                    saveLLSD(content, assetsFilename, lastModified)
                }
            }
            agentsUrl -> {
                if (!saveToFile) {
                    val data = loadFromFile(agentsFilename)
                    if (data != null) processAgents(data)
                } else {
                    processAgents(content)
                    saveLLSD(content, agentsFilename, lastModified)
                }
                isAgentsDone = true
                addAgents()
            }
            LEGACY_CLIENT_LIST_URL -> {
                if (!saveToFile) updateClientTagsLocal()
                else { processClientTags(content); saveLLSD(content, clientTagsFilename, lastModified) }
            }
            fsDataDefaultsUrl -> {
                if (saveToFile) saveLLSD(content, fsDataDefaultsFilename, lastModified)
                // No processing needed – loaded during app startup.
            }
        }
    }

    // -----------------------------------------------------------------------
    // Agent-flag queries
    // -----------------------------------------------------------------------

    fun getAgentFlags(avatarId: UUID): Int = teamAgents[avatarId] ?: -1

    fun isDeveloper(avatarId: UUID): Boolean = getAgentFlags(avatarId).let { it != -1 && (it and FSAgentFlags.DEVELOPER) != 0 }
    fun isSupport(avatarId: UUID): Boolean   = getAgentFlags(avatarId).let { it != -1 && (it and FSAgentFlags.SUPPORT) != 0 }
    fun isQA(avatarId: UUID): Boolean        = getAgentFlags(avatarId).let { it != -1 && (it and FSAgentFlags.QA) != 0 }

    fun isAgentFlag(agentId: UUID, flag: Int): Boolean {
        val flags = teamAgents[agentId] ?: return false
        return (flags and flag) != 0
    }

    // -----------------------------------------------------------------------
    // Group queries
    // -----------------------------------------------------------------------

    fun isFirestormGroup(id: UUID): Boolean = isSupportGroup(id) || isTestingGroup(id)
    fun isSupportGroup(id: UUID): Boolean  = id in supportGroup
    fun isTestingGroup(id: UUID): Boolean  = id in testingGroup

    // -----------------------------------------------------------------------
    // Login gating
    // -----------------------------------------------------------------------

    fun allowedLogin(): Map<String, Any>? {
        val versionKey = TODO("APR: use JVM equivalent - LLVersionInfo::getChannelAndVersionFS()") as String
        val block = blockedVersions[versionKey] ?: return null

        var blocked = true
        if (block.containsKey("gridtype")) {
            blocked = false
            val gridType = block["gridtype"] as? String ?: ""
            val isSecondLife = TODO("APR: use JVM equivalent - LLGridManager::isInSecondLife()") as Boolean
            if (gridType == "secondlife" && isSecondLife) return block
        }
        if (block.containsKey("grids")) {
            blocked = false
            @Suppress("UNCHECKED_CAST")
            val grids = block["grids"] as? List<String> ?: emptyList()
            val currentGrid = TODO("APR: use JVM equivalent - LLGridManager::getGrid()") as String
            if (currentGrid in grids) return block
        }
        return if (blocked) block else null
    }

    // -----------------------------------------------------------------------
    // MOTD
    // -----------------------------------------------------------------------

    fun getOpenSimMOTD(): String = openSimMotd

    fun selectNextMOTD() {
        val isInSLMain = TODO("APR: use JVM equivalent - LLGridManager::instance().isInSLMain()") as Boolean
        if (!isInSLMain) return
        if (secondLifeMotd.isNotEmpty()) {
            TODO("GPU: gAgent.mMOTD = secondLifeMotd")
        } else if (randomMotds.isNotEmpty()) {
            val motd = randomMotds.random()
            TODO("GPU: gAgent.mMOTD = motd")
        }
    }

    // -----------------------------------------------------------------------
    // Legacy client-tag resolution
    // -----------------------------------------------------------------------

    fun resolveClientTag(id: UUID, newSystem: Boolean, newSystemColor: FloatArray): MutableMap<String, Any> {
        val tag: MutableMap<String, Any> = mutableMapOf(
            "uuid"      to id.toString(),
            "id_based"  to newSystem,
            "tex_color" to newSystemColor
        )

        val clientTagVisibility = TODO("GPU: gSavedSettings.getU32(\"FSClientTagsVisibility\")") as Int
        if (clientTagVisibility == 0) return tag

        val useLegacyClientTags = TODO("GPU: gSavedSettings.getU32(\"FSUseLegacyClienttags\")") as Int
        if (useLegacyClientTags != 0) {
            val idStr = id.toString()
            if (legacyClientList.containsKey(idStr)) {
                @Suppress("UNCHECKED_CAST")
                val entry = legacyClientList[idStr] as? Map<String, Any>
                if (entry != null) tag.putAll(entry)
            } else {
                val knownName = when (id) {
                    ID_SINGULARITY -> "Singularity"
                    ID_KOKUA       -> "Kokua"
                    ID_RADEGAST    -> "Radegast"
                    ID_IMPRUDENCE  -> "Imprudence"
                    ID_TEAPOT      -> "Teapot"
                    else           -> null
                }
                if (knownName != null) {
                    tag["name"] = knownName
                    tag["tpvd"] = true
                }
            }
        }

        if (newSystem) {
            if (clientTagVisibility >= 3) {
                TODO("GPU: extract null-terminated string from id UUID bytes and store in tag[\"name\"]")
            }
            val colorClientTags = TODO("GPU: gSavedSettings.getU32(\"FSColorClienttags\")") as Int
            val isTpvd = tag["tpvd"] as? Boolean ?: false
            if (colorClientTags >= 3 || isTpvd) {
                if (isTpvd && colorClientTags < 3) {
                    TODO("GPU: conditionally copy newSystemColor into tag[\"color\"] for allowed TPVD colour values")
                } else {
                    tag["color"] = newSystemColor
                }
            }
        }

        if (clientTagVisibility <= 1 && (tag["tpvd"] as? Boolean != true)) {
            tag.clear()
        }
        tag["uuid"]      = id.toString()
        tag["id_based"]  = newSystem
        tag["tex_color"] = newSystemColor
        return tag
    }

    // -----------------------------------------------------------------------
    // System-info / support-request
    // -----------------------------------------------------------------------

    fun processRequestForInfo(
        requester: UUID,
        message: String,
        name: String,
        sessionId: UUID
    ): String {
        val detectString = "/reqsysinfo"
        if (!message.startsWith(detectString)) return message
        if (!isSupport(requester) && !isDeveloper(requester) && !isQA(requester)) return message

        val reason = if (message.length > detectString.length) message.substring(detectString.length) else ""
        val outMessage = if (reason.isEmpty())
            TODO("GPU: LLTrans.getString(\"Reqsysinfo_Chat_NoReason\")") as String
        else
            TODO("GPU: LLTrans.getString(\"Reqsysinfo_Chat_Reason\", reason)") as String

        TODO("GPU: LLNotifications.instance().add(\"FireStormReqInfo\", args, payload, callbackReqInfo)")
        @Suppress("UNREACHABLE_CODE")
        return outMessage
    }

    fun callbackReqInfo(notification: Map<String, Any>, response: Map<String, Any>) {
        val option = TODO("GPU: LLNotification.getSelectedOption(notification, response)") as Int
        val fromId = TODO("GPU: notification[\"payload\"][\"from_id\"].asUUID()") as UUID
        val sessionId = TODO("GPU: notification[\"payload\"][\"session_id\"].asUUID()") as UUID
        val myName = TODO("GPU: LLAgentUI.buildFullname()") as String
        if (option == 0) {
            sendInfo(fromId, sessionId, myName)
        } else {
            TODO("APR: use JVM equivalent - pack and send 'Request Denied.' IM to fromId")
        }
    }

    fun getSystemInfo(): Map<String, String> {
        val info = TODO("APR: use JVM equivalent - LLAppViewer::instance()->getViewerInfo()") as Map<String, Any>
        val part1 = buildString {
            TODO("GPU: format viewer version, build date, CPU, memory, OS, graphics info lines")
        }
        val part2 = buildString {
            TODO("GPU: format OpenGL, libcurl, J2C, audio, libvlc, Vivox, packets, RLVa, mode/skin, font, UI scale, draw distance, LOD lines")
        }
        return mapOf("Part1" to part1, "Part2" to part2)
    }

    // -----------------------------------------------------------------------
    // addAgents – called after agents.xml loads and gCacheName is available
    // -----------------------------------------------------------------------

    fun addAgents() {
        val cacheNameReady = TODO("GPU: gCacheName != null") as Boolean
        if (!cacheNameReady) return

        for ((id, flags) in teamAgents) {
            if ((flags and FSAgentFlags.NO_SPAM) != 0) {
                val avName = TODO("APR: use JVM equivalent - LLAvatarNameCache::get(id)") as AvatarName?
                if (avName != null) {
                    onNameCache(id, avName)
                } else {
                    avatarNameCacheConnections[id]?.invoke()
                    avatarNameCacheConnections[id] = TODO("APR: use JVM equivalent - LLAvatarNameCache::get(id, callback -> onNameCache)") as () -> Unit
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun processData(fsData: Map<String, Any>) {
        val motd = fsData["MOTD"] as? String
        if (!motd.isNullOrEmpty()) {
            secondLifeMotd = motd
            TODO("GPU: gAgent.mMOTD = motd")
        } else {
            @Suppress("UNCHECKED_CAST")
            val randomList = fsData["RandomMOTD"] as? List<String> ?: emptyList()
            if (randomList.isNotEmpty()) {
                randomMotds = randomList
                TODO("GPU: gAgent.mMOTD = randomMotds.random()")
            }
        }

        @Suppress("UNCHECKED_CAST")
        val eventsMOTD = fsData["EventsMOTD"] as? Map<String, Map<String, Any>>
        if (eventsMOTD != null) {
            for ((_, content) in eventsMOTD) {
                val startDate = TODO("APR: use JVM equivalent - parse content[\"startDate\"] to Instant") as Long
                val endDate   = TODO("APR: use JVM equivalent - parse content[\"endDate\"] to Instant") as Long
                val now       = System.currentTimeMillis()
                if (startDate < now && endDate > now) {
                    TODO("GPU: gAgent.mMOTD = content[\"EventMOTD\"]")
                    break
                }
            }
        }

        (fsData["OpensimMOTD"] as? String)?.let { openSimMotd = it }

        @Suppress("UNCHECKED_CAST")
        val blocked = fsData["BlockedReleases"] as? Map<String, Map<String, Any>>
        blocked?.forEach { (version, content) -> blockedVersions[version] = content }

        processAgents(fsData)
        processAssets(fsData)

        val useLegacyTags = TODO("GPU: gSavedSettings.getU32(\"FSUseLegacyClienttags\")") as Int
        when {
            useLegacyTags > 1 -> TODO("APR: use JVM equivalent - HTTP GET LEGACY_CLIENT_LIST_URL")
            useLegacyTags > 0 -> updateClientTagsLocal()
        }

        TODO("GPU: if RlvActions.isRlvEnabled() and fsData has rlva_compat_list, call RlvSettings.initCompatibilityMode")
    }

    private fun processAgents(data: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        when {
            data.containsKey("Agents") -> {
                val agents = data["Agents"] as? Map<String, Number> ?: emptyMap()
                for ((key, value) in agents) {
                    teamAgents[UUID.fromString(key)] = value.toInt()
                }
            }
            data.containsKey("SupportAgents") -> {
                // Legacy format: "support"/"developer" keys within each agent entry
                @Suppress("UNCHECKED_CAST")
                val supportAgents = data["SupportAgents"] as? Map<String, Map<String, Any>> ?: emptyMap()
                for ((key, content) in supportAgents) {
                    val id = UUID.fromString(key)
                    var flags = 0
                    if (content.containsKey("support")) flags = flags or FSAgentFlags.SUPPORT
                    if (content.containsKey("developer")) flags = flags or FSAgentFlags.DEVELOPER
                    teamAgents[id] = flags
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        (data["SupportGroups"] as? Map<String, Any>)?.keys?.forEach {
            supportGroup.add(UUID.fromString(it))
        }
        @Suppress("UNCHECKED_CAST")
        (data["TestingGroups"] as? Map<String, Any>)?.keys?.forEach {
            testingGroup.add(UUID.fromString(it))
        }

        if (data.containsKey("DisableLegacySearch")) {
            legacySearchEnabled = false
        }
    }

    private fun processAssets(assets: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val assetMap = assets["assets"] as? Map<String, Map<String, Any>> ?: return
        for ((key, rawData) in assetMap) {
            val uid = xorDecryptUUID(UUID.fromString(key))
            if (uid == UUID(0L, 0L)) continue
            val data = rawData.toMutableMap()
            data["asset_permanent"] = false
            FSAssetBlacklist.addNewItemToBlacklistData(
                uid, FSAssetBlacklistData.fromLLSD(data), false
            )
        }
    }

    private fun processClientTags(tags: Map<String, Any>) {
        if (tags.containsKey("isComplete")) {
            legacyClientList = tags
        }
    }

    private fun saveLLSD(data: Map<String, Any>, filename: String, lastModified: Long) {
        TODO("APR: use JVM equivalent - serialize data to pretty XML, write to filename, then set file mtime to lastModified")
    }

    private fun loadFromFile(filename: String): Map<String, Any>? {
        TODO("APR: use JVM equivalent - parse LLSD XML from filename; return null on missing file or parse error")
    }

    private fun updateClientTagsLocal() {
        val data = loadFromFile(clientTagsFilename) ?: return
        processClientTags(data)
    }

    private fun onNameCache(avId: UUID, avName: AvatarName) {
        avatarNameCacheConnections.remove(avId)
        TODO("APR: use JVM equivalent - LLMuteList.add LLMute(avId, avName.getUserName(), EXTERNAL)")
    }

    private fun sendInfo(destination: UUID, sessionId: UUID, myName: String) {
        val info = getSystemInfo()
        TODO("APR: use JVM equivalent - pack and send two IM packets (Part1, Part2) to destination; echo to local IM window")
    }

    // XOR-decrypt a UUID key from the on-disk file (mirrors LLXORCipher with MAGIC_ID)
    private fun xorDecryptUUID(id: UUID): UUID {
        TODO("APR: use JVM equivalent - XOR id bytes with MAGIC_ID bytes")
    }
}
