package com.firestorm.newview

import com.firestorm.llui.Floater
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Report-type identifiers stored in the database – values are fixed; do NOT renumber.
enum class ReportType(val id: Int) {
    NULL_REPORT(0),
    UNKNOWN_REPORT(1),
    COMPLAINT_REPORT(3),
    CS_REQUEST_REPORT(4),
}

// Request-flag bits sent alongside object-info requests to the sim.
const val COMPLAINT_REPORT_REQUEST: UInt = 0x01u shl 1
const val OBJECT_PAY_REQUEST: UInt = 0x01u shl 2

private const val SCREEN_PREV_FILENAME = "screen_report_last.png"
private const val IP_CONTENT_REMOVAL = 66
private const val IP_PERMISSIONS_EXPLOIT = 37
private const val IMAGE_WIDTH = 1024
private const val IMAGE_HEIGHT = 768

// Bundles a pre-built report payload with a screenshot asset for the
// SendUserReportWithScreenshot capability endpoint.
class ARScreenShotUploader(
    private val report: Map<String, Any?>,
    private val assetId: String,
    @Suppress("unused") private val assetType: Int,
) {
    fun prepareUpload(): Map<String, Any?> = mapOf("success" to true)
    fun generatePostBody(): Map<String, Any?> = report
    @Suppress("UNUSED_PARAMETER")
    fun finishUpload(result: Map<String, Any?>): String = ""
    fun showInventoryPanel(): Boolean = false
    fun getDisplayName(): String = "Abuse Report"
}

class FloaterReporter(key: String) : Floater(key) {

    private var reportType: ReportType = ReportType.COMPLAINT_REPORT
    private var objectId: String = ""
    @Suppress("unused") private var screenId: String = ""
    private var abuserId: String = ""
    private var experienceId: String = ""
    private var ownerName: String = ""
    private var deselectOnClose: Boolean = false
    private var picking: Boolean = false
    private var position: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)
    private var copyrightWarningSeen: Boolean = false
    private var defaultSummary: String = ""
    private var avatarNameCacheConnected: Boolean = false
    private var imageRaw: Any? = null
    private var prevImageRaw: Any? = null
    private var snapshotTimerStarted: Boolean = false
    private var snapshotTimerElapsed: Float = 0f

    // In-memory UI field values (stand-ins for the real viewer UI controls).
    private val uiFields: MutableMap<String, Any?> = mutableMapOf(
        "abuse_location_edit" to "",
        "sim_field"           to "",
        "object_name"         to "",
        "owner_name"          to "",
        "summary_edit"        to "",
        "details_edit"        to "",
        "abuser_name_edit"    to "",
        "reporter_field"      to "",
        "pos_field"           to "",
        "category_combo"      to 0,
        "screenshot"          to "",
        "send_btn_enabled"    to false,
    )

    // Saved account settings (in-process substitute for gSavedPerAccountSettings).
    private val savedPerAccountSettings: MutableMap<String, Any?> = mutableMapOf(
        "PreviousScreenshotForReport" to false
    )

    fun setReportType(type: ReportType) { reportType = type }

    fun postBuild(): Boolean {
        // Build a placeholder SLURL for the current agent position.
        val slurl = buildAgentSLURL()
        uiFields["abuse_location_edit"] = slurl

        enableControls(true)

        // Populate region name and position.
        val regionName = System.getProperty("firestorm.region.name", "Unknown Region")
        uiFields["sim_field"] = regionName

        val posX = System.getProperty("firestorm.pos.x", "0").toFloatOrNull() ?: 0f
        val posY = System.getProperty("firestorm.pos.y", "0").toFloatOrNull() ?: 0f
        val posZ = System.getProperty("firestorm.pos.z", "0").toFloatOrNull() ?: 0f
        setPosBox(Triple(posX.toDouble(), posY.toDouble(), posZ.toDouble()))

        // Clear object/owner fields.
        uiFields["object_name"] = ""
        uiFields["owner_name"]  = ""
        ownerName = ""

        // Retrieve the default details text (from a bundle or hard-coded string).
        defaultSummary = "Please describe the issue in detail."
        uiFields["details_edit"] = defaultSummary

        // Disable abuser name field; it is populated via the avatar picker.
        // (In the real viewer: getChild<LLUICtrl>("abuser_name_edit").setEnabled(false))

        // Populate the reporter field with the agent's SLURL.
        val agentId = System.getProperty("firestorm.agent.id", "00000000-0000-0000-0000-000000000000")
        uiFields["reporter_field"] = "secondlife:///app/agent/$agentId/inspect"

        requestAbuseCategories()
        return true
    }

    fun onOpen(key: Any) {
        uiFields["send_btn_enabled"] = false
        snapshotTimerStarted = true
        snapshotTimerElapsed = 0f
    }

    fun onClose(appQuitting: Boolean) {
        if (avatarNameCacheConnected) {
            // Disconnect the avatar-name cache signal.
            avatarNameCacheConnected = false
        }
        objectId = ""
        if (picking) closePickTool()
        position = Triple(0f, 0f, 0f)
        // Release screenshot image data.
        imageRaw = null
        prevImageRaw = null
    }

    // Called each idle tick; fires takeNewSnapshot once the configured delay has elapsed.
    fun onIdle() {
        // Read the delay from a JVM system property (replaces gSavedSettings.getF32).
        val screenshotDelay: Float =
            System.getProperty("firestorm.abuseReportScreenshotDelay", "3.0").toFloatOrNull() ?: 3.0f
        if (snapshotTimerStarted && snapshotTimerElapsed > screenshotDelay) {
            snapshotTimerStarted = false
            takeNewSnapshot(refresh = false)
        }
    }

    private fun enableControls(enable: Boolean) {
        // Record the enabled state so callers can inspect it; real viewer would set child views.
        uiFields["category_combo_enabled"]  = enable
        uiFields["chat_check_enabled"]      = enable
        uiFields["screenshot_enabled"]      = false  // screenshot ctrl is always disabled here
        uiFields["pick_btn_enabled"]        = enable
        uiFields["summary_edit_enabled"]    = enable
        uiFields["details_edit_enabled"]    = enable
        uiFields["send_btn_enabled"]        = enable
        uiFields["cancel_btn_enabled"]      = enable
    }

    fun getExperienceInfo(expId: String) {
        experienceId = expId
        if (expId.isEmpty()) return
        // In the real viewer: LLExperienceCache.instance().get(experienceId).
        // JVM stub: try to retrieve experience details via HTTP.
        val desc = try {
            val url = "https://cap.secondlife.com/cap/ExperienceQuery/$expId"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5_000
            conn.readTimeout   = 10_000
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val data = LLSettingsVOBase.parseJsonToMap(body)
                val agentId = data["agent_id"] as? String ?: ""
                if (agentId.isNotEmpty()) setFromAvatarID(agentId)
                "Experience id: $expId"
            } else {
                "Unable to retrieve details for id: $expId"
            }
        } catch (e: Exception) {
            "Unable to retrieve details for id: $expId"
        }
        uiFields["details_edit"] = desc
    }

    fun getObjectInfo(objId: String) {
        objectId = objId
        if (objId.isEmpty()) return

        // In the real viewer: gObjectList.findObject(objectId).
        // JVM stub: query region simulator via HTTP for object properties.
        val obj: ViewerObjectStub? = null  // not available in JVM context

        // If we had an object, we would walk to root and query the simulator.
        // Without the object, we can still update the region field using system properties.
        val regionName = System.getProperty("firestorm.region.name", "")
        if (regionName.isNotEmpty()) uiFields["sim_field"] = regionName
    }

    fun onClickSelectAbuser() {
        // In the real viewer: show LLFloaterAvatarPicker with callbackAvatarID.
        // JVM stub: record that the picker was requested; real UI code wires this up.
        System.err.println("[FloaterReporter] onClickSelectAbuser: avatar picker would open here")
    }

    private fun callbackAvatarID(ids: List<String>, names: List<String>) {
        if (ids.isEmpty() || names.isEmpty()) return
        uiFields["abuser_name_edit"] = names[0]
        abuserId = ids[0]
        refresh()
    }

    private fun setFromAvatarID(avatarId: String) {
        abuserId = avatarId
        objectId = avatarId
        val avatarLink = "secondlife:///app/agent/$avatarId/inspect"
        uiFields["owner_name"] = avatarLink
        if (avatarNameCacheConnected) {
            // Disconnect previous connection (noop in JVM; tracked by flag).
            avatarNameCacheConnected = false
        }
        avatarNameCacheConnected = true
        // In the real viewer: LLAvatarNameCache.get(avatarId, ::onAvatarNameCache).
        // JVM stub: attempt to resolve the avatar name via HTTP, then call onAvatarNameCache.
        Thread {
            val completeName = fetchAvatarName(avatarId)
            onAvatarNameCache(avatarId, completeName)
        }.start()
    }

    private fun onAvatarNameCache(avatarId: String, completeName: String) {
        avatarNameCacheConnected = false
        if (objectId == avatarId) {
            ownerName = completeName
            uiFields["object_name"]      = completeName
            uiFields["abuser_name_edit"] = completeName
        }
    }

    // ---- Static factory / show methods --------------------------------------

    companion object {
        fun showFromMenu(reportType: ReportType) {
            if (reportType != ReportType.COMPLAINT_REPORT) return
            // In the real viewer: FloaterReg.showTypedInstance<FloaterReporter>("reporter").setReportType(reportType).
            val reporter = FloaterReporter("reporter")
            reporter.setReportType(reportType)
            reporter.postBuild()
        }

        fun showFromObject(objectId: String, experienceId: String = "") {
            show(objectId, "", experienceId)
        }

        fun showFromAvatar(avatarId: String, avatarName: String) {
            show(avatarId, avatarName)
        }

        fun showFromChat(
            avatarId: String,
            avatarName: String,
            time: String,
            description: String,
        ) {
            show(avatarId, avatarName)
            // In the real viewer: format chat_report_format with time/description.
            val reporter = FloaterReporter("reporter")
            reporter.uiFields["details_edit"] = "[$time] $description"
        }

        fun showFromExperience(experienceId: String) {
            val reporter = FloaterReporter("reporter")
            reporter.getExperienceInfo(experienceId)
            reporter.deselectOnClose = true
        }

        private fun show(objectId: String, avatarName: String = "", experienceId: String = "") {
            // In the real viewer: clear PreviousScreenshotForReport if floater is already visible.
            val reporter = FloaterReporter("reporter")
            if (avatarName.isEmpty()) {
                reporter.getObjectInfo(objectId)
            } else {
                reporter.setFromAvatarID(objectId)
            }
            if (experienceId.isNotEmpty()) reporter.getExperienceInfo(experienceId)
            reporter.deselectOnClose = true
        }

        // ---- HTTP helpers used by the JVM stubs ----

        /**
         * Attempt to look up an avatar display name from the AgentProfile cap.
         * Falls back to the raw UUID string on any error.
         */
        internal fun fetchAvatarName(avatarId: String): String {
            return try {
                val url = "https://api.secondlife.com/v1/avatars/$avatarId"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5_000
                conn.readTimeout   = 8_000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val data = LLSettingsVOBase.parseJsonToMap(body)
                    data["display_name"] as? String ?: data["name"] as? String ?: avatarId
                } else {
                    avatarId
                }
            } catch (e: Exception) {
                avatarId
            }
        }

        /**
         * Build a placeholder SLURL for the agent's current location using
         * JVM system properties set by the launcher (or defaults).
         */
        internal fun buildAgentSLURL(): String {
            val region = System.getProperty("firestorm.region.name", "Unknown")
            val x = System.getProperty("firestorm.pos.x", "128")
            val y = System.getProperty("firestorm.pos.y", "128")
            val z = System.getProperty("firestorm.pos.z", "0")
            return "secondlife://$region/$x/$y/$z"
        }
    }

    fun setPickedObjectProperties(objectName: String, ownerNameParam: String, ownerId: String) {
        uiFields["object_name"]      = objectName
        uiFields["owner_name"]       = "secondlife:///app/agent/$ownerId/inspect"
        uiFields["abuser_name_edit"] = ownerNameParam
        abuserId = ownerId
        ownerName = ownerNameParam
    }

    // ---- Button handlers ----------------------------------------------------

    fun onClickSend() {
        if (picking) closePickTool()
        if (!validateReport()) return

        val categoryValue: Int = (uiFields["category_combo"] as? Number)?.toInt() ?: 0
        if (!copyrightWarningSeen) {
            val detailsLc  = (uiFields["details_edit"]  as? String ?: "").lowercase()
            val summaryLc  = (uiFields["summary_edit"]  as? String ?: "").lowercase()
            if (detailsLc.contains("copyright") || summaryLc.contains("copyright") ||
                categoryValue == IP_CONTENT_REMOVAL || categoryValue == IP_PERMISSIONS_EXPLOIT
            ) {
                // In the real viewer: LLNotificationsUtil.add("HelpReportAbuseContainsCopyright").
                System.err.println("[FloaterReporter] Copyright warning shown; report not sent.")
                copyrightWarningSeen = true
                return
            }
        } else if (categoryValue == IP_CONTENT_REMOVAL) {
            // IP_CONTENT_REMOVAL always triggers the dialog; report cannot be sent.
            System.err.println("[FloaterReporter] IP_CONTENT_REMOVAL category; report not sent.")
            return
        }

        // In the real viewer: LLUploadDialog.modalUploadDialog("uploading_abuse_report").
        val url     = System.getProperty("firestorm.cap.SendUserReport", "")
        val sshotUrl = System.getProperty("firestorm.cap.SendUserReportWithScreenshot", "")
        if (url.isNotEmpty() || sshotUrl.isNotEmpty()) {
            sendReportViaCaps(url, sshotUrl, gatherReport())
            System.err.println("[FloaterReporter] Abuse report sent via capability.")
            // In the real viewer: LLNotificationsUtil.add("HelpReportAbuseConfirm"); closeFloater().
        } else {
            uiFields["send_btn_enabled"]   = false
            uiFields["cancel_btn_enabled"] = false
            uploadImage()
        }
    }

    fun onClickCancel() {
        copyrightWarningSeen = false
        if (picking) closePickTool()
        // In the real viewer: closeFloater().
    }

    fun onClickObjPicker() {
        // In the real viewer: set ToolObjPicker as transient tool with closePickTool callback.
        picking = true
        uiFields["object_name"] = ""
        uiFields["owner_name"]  = ""
        System.err.println("[FloaterReporter] Object picker activated.")
    }

    private fun closePickTool() {
        // In the real viewer: retrieve picked object ID from ToolObjPicker.
        val objId = System.getProperty("firestorm.pickedObjectId", "")
        getObjectInfo(objId)
        picking = false
    }

    // ---- Validation ---------------------------------------------------------

    private fun validateReport(): Boolean {
        val category = (uiFields["category_combo"] as? Number)?.toInt() ?: 0
        if (category == 0) {
            System.err.println("[FloaterReporter] Validation failed: no category selected.")
            return false
        }
        if ((uiFields["abuser_name_edit"] as? String ?: "").isEmpty()) {
            System.err.println("[FloaterReporter] Validation failed: abuser name empty.")
            return false
        }
        if ((uiFields["abuse_location_edit"] as? String ?: "").isEmpty()) {
            System.err.println("[FloaterReporter] Validation failed: abuse location empty.")
            return false
        }
        if ((uiFields["summary_edit"] as? String ?: "").isEmpty()) {
            System.err.println("[FloaterReporter] Validation failed: summary empty.")
            return false
        }
        if ((uiFields["details_edit"] as? String ?: "") == defaultSummary) {
            System.err.println("[FloaterReporter] Validation failed: details not changed.")
            return false
        }
        return true
    }

    // ---- Report assembly ----------------------------------------------------

    private fun gatherReport(): Map<String, Any?> {
        // Substitute for gAgent.getRegion(): use system properties.
        val regionName = System.getProperty("firestorm.region.name", "")
        if (regionName.isEmpty()) return emptyMap()

        copyrightWarningSeen = false

        val isBeta = System.getProperty("firestorm.grid.isBeta", "false").toBoolean()
        val prefix = if (isBeta) "Preview " else ""

        // Determine the OS platform to match the C++ #if LL_WINDOWS / LL_DARWIN / LL_LINUX chain.
        val osName = System.getProperty("os.name", "").lowercase()
        val platform = when {
            osName.contains("windows") -> "Win"
            osName.contains("mac")     -> "Mac"
            osName.contains("linux")   -> "Lnx"
            else                       -> "JVM"
        }

        val categoryName    = (uiFields["category_combo_label"] as? String ?: "")
        val abuseLocation   = uiFields["abuse_location_edit"] as? String ?: ""
        val abuserName      = uiFields["abuser_name_edit"]    as? String ?: ""
        val summaryText     = uiFields["summary_edit"]        as? String ?: ""

        val summary = "$prefix |$regionName| ($abuseLocation) [$categoryName]  {$abuserName}  \"$summaryText\""

        // Version info: read from system properties set by the launcher, or fall back to JVM version.
        val version       = System.getProperty("firestorm.version", System.getProperty("java.version", "0.0.0"))
        val shortVersion  = System.getProperty("firestorm.shortVersion", version.split(".").take(2).joinToString("."))
        val objectName    = uiFields["object_name"] as? String ?: ""
        val detailsText   = uiFields["details_edit"] as? String ?: ""

        val details = buildString {
            append("V$version\n\n")
            if (objectName.isNotEmpty() && ownerName.isNotEmpty()) {
                append("Object: $objectName\n")
                append("Owner: $ownerName\n")
            }
            append("Abuser name: $abuserName \n")
            append("Abuser location: $abuseLocation \n")
            append(detailsText)
        }

        // CPU family: read from system properties or fall back to the JVM os.arch.
        val cpuFamily = System.getProperty("firestorm.cpu.family",
            System.getProperty("os.arch", "unknown"))
        // GL renderer / driver: not available in standard JVM; use placeholder strings.
        val glRenderer    = System.getProperty("firestorm.gl.renderer",    "JVM-no-GL")
        val driverVersion = System.getProperty("firestorm.gl.driverVersion","JVM-no-driver")
        val versionString = "$shortVersion $platform $cpuFamily $glRenderer $driverVersion"

        val screenshotId = uiFields["screenshot"] as? String ?: ""

        return mapOf(
            "report-type"       to reportType.id,
            "category"          to (uiFields["category_combo"] ?: 0),
            "position"          to position,
            "check-flags"       to 0,
            "screenshot-id"     to screenshotId,
            "object-id"         to objectId,
            "abuser-id"         to abuserId,
            "abuse-region-name" to "",
            "abuse-region-id"   to "",
            "summary"           to summary,
            "version-string"    to versionString,
            "details"           to details,
        )
    }

    // ---- Network send -------------------------------------------------------

    private fun sendReportViaLegacy(report: Map<String, Any?>) {
        // In the real viewer: compose and send a UserReport UDP message via gMessageSystem.
        // JVM stub: POST the report as JSON to the configured legacy endpoint.
        val endpoint = System.getProperty("firestorm.legacy.reportEndpoint", "")
        if (endpoint.isEmpty()) {
            System.err.println("[FloaterReporter] No legacy report endpoint configured; report dropped.")
            return
        }
        try {
            postJsonReport(endpoint, report)
        } catch (e: Exception) {
            System.err.println("[FloaterReporter] sendReportViaLegacy failed: ${e.message}")
        }
    }

    private fun sendReportViaCaps(url: String, sshotUrl: String, report: Map<String, Any?>) {
        if (sshotUrl.isNotEmpty()) {
            // In the real viewer: LLViewerAssetUpload.EnqueueInventoryUpload with ARScreenShotUploader.
            // JVM stub: POST the report JSON to the screenshot upload endpoint.
            try {
                val uploader = ARScreenShotUploader(
                    report,
                    report["screenshot-id"] as? String ?: "",
                    1 // AT_TEXTURE type stub
                )
                val body = uploader.generatePostBody()
                postJsonReport(sshotUrl, body)
            } catch (e: Exception) {
                System.err.println("[FloaterReporter] sendReportViaCaps (screenshot) failed: ${e.message}")
            }
        } else {
            // In the real viewer: LLCoreHttpUtil.HttpCoroutineAdapter.callbackHttpPost.
            // JVM stub: synchronous POST on a background thread.
            Thread {
                try {
                    postJsonReport(url, report)
                    finishedARPost(emptyMap())
                } catch (e: Exception) {
                    System.err.println("[FloaterReporter] sendReportViaCaps failed: ${e.message}")
                    finishedARPost(mapOf("error" to e.message))
                }
            }.start()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun finishedARPost(result: Map<String, Any?>) {
        // In the real viewer: LLUploadDialog.modalUploadFinished().
        System.err.println("[FloaterReporter] Abuse report POST complete.")
    }

    // ---- Screenshot ---------------------------------------------------------

    fun takeNewSnapshot(refresh: Boolean) {
        uiFields["send_btn_enabled"] = true

        // Capture the screen using java.awt.Robot (excluding this floater, which is non-visual).
        val capturedBytes: ByteArray? = captureScreenshot()
        if (capturedBytes == null) {
            System.err.println("[FloaterReporter] takeNewSnapshot: screenshot capture failed.")
            return
        }
        imageRaw = capturedBytes

        val prevExists = savedPerAccountSettings["PreviousScreenshotForReport"] as? Boolean ?: false
        if (prevExists && !refresh) {
            val screenshotFilename = buildScreenshotPath()
            val prevFile = File(screenshotFilename)
            if (prevFile.exists()) {
                prevImageRaw = prevFile.readBytes()
                // In the real viewer: show "LoadPreviousReportScreenshot" notification.
                // JVM stub: always use the new screenshot rather than prompting.
                System.err.println("[FloaterReporter] Previous screenshot available; using new one (JVM stub).")
            }
        }
        takeScreenshot(usePrevScreenshot = false)
    }

    private fun takeScreenshot(usePrevScreenshot: Boolean) {
        savedPerAccountSettings["PreviousScreenshotForReport"] = true
        if (!usePrevScreenshot) {
            val screenshotFilename = buildScreenshotPath()
            val bytes = imageRaw as? ByteArray
            if (bytes != null) {
                try {
                    File(screenshotFilename).writeBytes(bytes)
                } catch (e: Exception) {
                    System.err.println("[FloaterReporter] Could not save screenshot: ${e.message}")
                }
            }
        } else {
            imageRaw = prevImageRaw
        }

        // In the real viewer: LLViewerTextureList.convertToUploadFile (J2C encode), then write to
        // LLFileSystem cache and register with LLViewerTextureManager.
        // JVM stub: generate a random asset UUID and store it so the screenshot field has a value.
        val assetUuid = java.util.UUID.randomUUID().toString()

        // In the real viewer: populate mResourceDatap with transaction/asset IDs.
        uiFields["screenshot"] = assetUuid
    }

    fun onLoadScreenshotDialog(selectedOption: Int) {
        takeScreenshot(usePrevScreenshot = selectedOption == 0)
    }

    private fun uploadImage() {
        // In the real viewer: gAssetStorage.storeAssetData(transactionId, assetType, uploadDoneCallback, resourceDatap).
        // JVM stub: simulate an asynchronous upload via HTTP PUT.
        val screenshotId = uiFields["screenshot"] as? String ?: ""
        Thread {
            try {
                val endpoint = System.getProperty("firestorm.asset.uploadEndpoint", "")
                val result = if (endpoint.isNotEmpty()) {
                    val bytes = (imageRaw as? ByteArray) ?: byteArrayOf()
                    uploadBytesToEndpoint(endpoint, bytes)
                } else 0
                uploadDoneCallback(screenshotId, result)
            } catch (e: Exception) {
                uploadDoneCallback("", -1)
            }
        }.start()
    }

    private fun uploadDoneCallback(uuid: String, result: Int) {
        // In the real viewer: LLUploadDialog.modalUploadFinished().
        System.err.println("[FloaterReporter] Upload done: uuid=$uuid result=$result")
        if (result < 0) {
            System.err.println("[FloaterReporter] ErrorUploadingReportScreenshot: error code $result")
            return
        }
        screenId = uuid
        sendReportViaLegacy(gatherReport())
        // In the real viewer: LLNotificationsUtil.add("HelpReportAbuseConfirm"); closeFloater().
        System.err.println("[FloaterReporter] Report sent via legacy path; floater would close.")
    }

    // ---- Helpers ------------------------------------------------------------

    private fun setPosBox(pos: Triple<Double, Double, Double>) {
        position = Triple(pos.first.toFloat(), pos.second.toFloat(), pos.third.toFloat())
        val posString = "{%.1f, %.1f, %.1f}".format(pos.first, pos.second, pos.third)
        uiFields["pos_field"] = posString
    }

    private fun requestAbuseCategories() {
        val capUrl = System.getProperty("firestorm.cap.AbuseCategories", "")
        if (capUrl.isEmpty()) return

        val lang = System.getProperty("firestorm.language", "en")
        val fullUrl = if (lang != "default" && lang.isNotEmpty()) "$capUrl?lc=$lang" else capUrl

        Thread {
            try {
                val conn = URL(fullUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8_000
                conn.readTimeout   = 12_000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val data = LLSettingsVOBase.parseJsonToMap(body)
                    @Suppress("UNCHECKED_CAST")
                    val categories = data["categories"] as? List<*> ?: return@Thread
                    // In the real viewer: populate category_combo keeping the first "Select category" entry.
                    // JVM stub: store the first category id as the default selection.
                    val firstCat = (categories.firstOrNull() as? Map<*, *>)?.get("category")
                    if (firstCat != null) uiFields["category_combo_default"] = firstCat
                }
            } catch (e: Exception) {
                System.err.println("[FloaterReporter] requestAbuseCategories failed: ${e.message}")
            }
        }.start()
    }

    private fun onUpdateScreenshot() {
        takeNewSnapshot(refresh = true)
    }

    private fun refresh() {
        // Rebuild any derived UI state from the current report fields.
        // In the real viewer this would redraw the floater; here it is a no-op placeholder.
        System.err.println("[FloaterReporter] refresh(): UI state updated.")
    }

    // ---- Internal JVM screenshot / network utilities ----

    /**
     * Capture the full primary-display screen as a PNG byte array using java.awt.Robot.
     * Returns null if the capture fails (e.g. no display available in headless mode).
     */
    private fun captureScreenshot(): ByteArray? {
        return try {
            val robot = Robot()
            val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
            val rect = Rectangle(0, 0,
                minOf(screenSize.width, IMAGE_WIDTH),
                minOf(screenSize.height, IMAGE_HEIGHT))
            val image = robot.createScreenCapture(rect)
            val baos = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(image, "png", baos)
            baos.toByteArray()
        } catch (e: Exception) {
            System.err.println("[FloaterReporter] captureScreenshot failed: ${e.message}")
            null
        }
    }

    /**
     * Resolve the path where the previous screenshot is stored.
     * Uses the "firestorm.userDataDir" system property as the base directory.
     */
    private fun buildScreenshotPath(): String {
        val userDir = System.getProperty("firestorm.userDataDir",
            System.getProperty("user.home") + File.separator + ".firestorm")
        return userDir + File.separator + SCREEN_PREV_FILENAME
    }

    /**
     * POST a report map serialized as JSON to [endpoint] via HttpURLConnection.
     * Returns the HTTP response code.
     */
    private fun postJsonReport(endpoint: String, report: Map<String, Any?>): Int {
        val body = serializeReportToJson(report)
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.connectTimeout = 10_000
        conn.readTimeout   = 20_000
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        return conn.responseCode
    }

    /**
     * Upload raw bytes to [endpoint] via HTTP PUT.
     * Returns 0 on success, -1 on failure.
     */
    private fun uploadBytesToEndpoint(endpoint: String, bytes: ByteArray): Int {
        return try {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.connectTimeout = 10_000
            conn.readTimeout   = 30_000
            conn.outputStream.use { it.write(bytes) }
            val status = conn.responseCode
            if (status in 200..299) 0 else -1
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Serialize a report map to a compact JSON string.
     * Handles String, Number, Boolean, Triple, and null values.
     */
    private fun serializeReportToJson(report: Map<String, Any?>): String {
        val sb = StringBuilder("{")
        report.entries.forEachIndexed { idx, (k, v) ->
            if (idx > 0) sb.append(',')
            sb.append('"').append(k).append("\":")
            sb.append(reportValueToJson(v))
        }
        sb.append('}')
        return sb.toString()
    }

    private fun reportValueToJson(v: Any?): String = when (v) {
        null               -> "null"
        is Boolean         -> v.toString()
        is Number          -> v.toString()
        is String          -> "\"${v.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        is Triple<*, *, *> -> "[${reportValueToJson(v.first)},${reportValueToJson(v.second)},${reportValueToJson(v.third)}]"
        is List<*>         -> "[${v.joinToString(",") { reportValueToJson(it) }}]"
        is Map<*, *>       -> {
            val entries = v.entries.joinToString(",") { (mk, mv) ->
                "\"$mk\":${reportValueToJson(mv)}"
            }
            "{$entries}"
        }
        else               -> "\"$v\""
    }
}
