package com.firestorm.newview

import com.firestorm.llui.Floater

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

    fun setReportType(type: ReportType) { reportType = type }

    fun postBuild(): Boolean {
        val slurl: String = TODO("APR: LLAgentUI.buildSLURL()") as String
        TODO("APR: getChild<LLUICtrl>(\"abuse_location_edit\").setValue(slurl)")
        enableControls(true)

        val pos = TODO("APR: gAgent.getPositionGlobal()") as Triple<Double, Double, Double>
        val regionName: String = TODO("APR: gAgent.getRegion()?.getName() ?: \"\"") as String
        TODO("APR: getChild<LLUICtrl>(\"sim_field\").setValue(regionName)")
        TODO("APR: subtract regionOriginGlobal from pos; call setPosBox(pos)")

        TODO("APR: clear object_name and owner_name fields; ownerName = \"\"")
        TODO("APR: getChild<LLUICtrl>(\"summary_edit\").setFocus(true)")
        defaultSummary = TODO("APR: getChild<LLUICtrl>(\"details_edit\").getValue().asString()") as String

        TODO("APR: getChild<LLUICtrl>(\"abuser_name_edit\").setEnabled(false)")
        TODO("APR: wire pick_btn, select_abuser, send_btn, cancel_btn, refresh_screenshot callbacks")

        val reporter: String = TODO("APR: LLSLURL(\"agent\", gAgent.getID(), \"inspect\").getSLURLString()") as String
        TODO("APR: getChild<LLUICtrl>(\"reporter_field\").setValue(reporter)")

        requestAbuseCategories()
        TODO("APR: center()")
        return true
    }

    fun onOpen(key: Any) {
        TODO("APR: getChildView(\"send_btn\").setEnabled(false)")
        snapshotTimerStarted = true
        snapshotTimerElapsed = 0f
    }

    fun onClose(appQuitting: Boolean) {
        if (avatarNameCacheConnected) TODO("APR: disconnect avatarNameCacheConnection")
        TODO("APR: gIdleCallbacks.deleteFunction(::onIdle, this)")
        objectId = ""
        if (picking) closePickTool()
        position = Triple(0f, 0f, 0f)
        TODO("APR: free mResourceDatap")
    }

    // Called each idle tick; fires takeNewSnapshot once the configured delay has elapsed.
    fun onIdle() {
        val screenshotDelay: Float =
            TODO("APR: gSavedSettings.getF32(\"AbuseReportScreenshotDelay\")") as Float
        if (snapshotTimerStarted && snapshotTimerElapsed > screenshotDelay) {
            snapshotTimerStarted = false
            takeNewSnapshot(refresh = false)
        }
    }

    private fun enableControls(enable: Boolean) {
        TODO("APR: set enabled state on category_combo, chat_check, pick_btn, summary_edit, details_edit, send_btn, cancel_btn; screenshot always disabled here")
    }

    fun getExperienceInfo(expId: String) {
        experienceId = expId
        if (expId.isEmpty()) return
        val experience: Map<String, Any?>? =
            TODO("APR: LLExperienceCache.instance().get(experienceId)") as Map<String, Any?>?
        val desc = if (experience != null) {
            val agentId: String = experience["agent_id"] as? String ?: ""
            setFromAvatarID(agentId)
            "Experience id: $experienceId"
        } else {
            "Unable to retrieve details for id: $experienceId"
        }
        TODO("APR: getChild<LLUICtrl>(\"details_edit\").setValue(desc)")
    }

    fun getObjectInfo(objId: String) {
        objectId = objId
        if (objId.isEmpty()) return

        val obj: ViewerObjectStub? = TODO("APR: gObjectList.findObject(objectId)") as ViewerObjectStub?
        obj ?: return

        val rootObj: ViewerObjectStub = if (obj.isAttachment()) {
            val root = TODO("APR: obj.getRoot()") as ViewerObjectStub
            objectId = root.getID()
            root
        } else obj

        val regionName: String = TODO("APR: rootObj.getRegion()?.getName() ?: \"\"") as String
        TODO("APR: getChild<LLUICtrl>(\"sim_field\").setValue(regionName)")
        TODO("APR: setPosBox(rootObj.getPositionRegion() as global)")

        if (rootObj.isAvatar()) {
            setFromAvatarID(objectId)
        } else {
            // Query the sim for object properties (name/owner) to populate the form.
            TODO("APR: send RequestObjectPropertiesFamily UDP message via gMessageSystem with COMPLAINT_REPORT_REQUEST flag")
        }
    }

    fun onClickSelectAbuser() {
        TODO("APR: show LLFloaterAvatarPicker with ::callbackAvatarID callback")
    }

    private fun callbackAvatarID(ids: List<String>, names: List<String>) {
        if (ids.isEmpty() || names.isEmpty()) return
        TODO("APR: getChild<LLUICtrl>(\"abuser_name_edit\").setValue(names[0])")
        abuserId = ids[0]
        refresh()
    }

    private fun setFromAvatarID(avatarId: String) {
        abuserId = avatarId
        objectId = avatarId
        val avatarLink: String =
            TODO("APR: LLSLURL(\"agent\", objectId, \"inspect\").getSLURLString()") as String
        TODO("APR: getChild<LLUICtrl>(\"owner_name\").setValue(avatarLink)")
        if (avatarNameCacheConnected) TODO("APR: disconnect previous avatarNameCacheConnection")
        avatarNameCacheConnected = true
        TODO("APR: LLAvatarNameCache.get(avatarId, ::onAvatarNameCache)")
    }

    private fun onAvatarNameCache(avatarId: String, completeName: String) {
        avatarNameCacheConnected = false
        if (objectId == avatarId) {
            ownerName = completeName
            TODO("APR: set object_name (value + tooltip) and abuser_name_edit to completeName")
        }
    }

    // ---- Static factory / show methods --------------------------------------

    companion object {
        fun showFromMenu(reportType: ReportType) {
            if (reportType != ReportType.COMPLAINT_REPORT) return
            TODO("APR: FloaterReg.showTypedInstance<FloaterReporter>(\"reporter\").setReportType(reportType)")
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
            TODO("APR: format chat_report_format string with time/description and set details_edit")
        }

        fun showFromExperience(experienceId: String) {
            val reporter: FloaterReporter =
                TODO("APR: FloaterReg.showTypedInstance<FloaterReporter>(\"reporter\")") as FloaterReporter
            reporter.getExperienceInfo(experienceId)
            reporter.deselectOnClose = true
        }

        private fun show(objectId: String, avatarName: String = "", experienceId: String = "") {
            TODO("APR: clear PreviousScreenshotForReport pref if floater already visible")
            val reporter: FloaterReporter =
                TODO("APR: FloaterReg.showTypedInstance<FloaterReporter>(\"reporter\")") as FloaterReporter
            if (avatarName.isEmpty()) {
                reporter.getObjectInfo(objectId)
            } else {
                reporter.setFromAvatarID(objectId)
            }
            if (experienceId.isNotEmpty()) reporter.getExperienceInfo(experienceId)
            reporter.deselectOnClose = true
        }
    }

    fun setPickedObjectProperties(objectName: String, ownerNameParam: String, ownerId: String) {
        TODO("APR: set object_name field, owner_name as SLURL link, and abuser_name_edit")
        abuserId = ownerId
        ownerName = ownerNameParam
    }

    // ---- Button handlers ----------------------------------------------------

    fun onClickSend() {
        if (picking) closePickTool()
        if (!validateReport()) return

        val categoryValue: Int =
            TODO("APR: category_combo.getSelectedValue().asInteger()") as Int
        if (!copyrightWarningSeen) {
            val detailsLc: String =
                TODO("APR: details_edit.getValue().asString().lowercase()") as String
            val summaryLc: String =
                TODO("APR: summary_edit.getValue().asString().lowercase()") as String
            if (detailsLc.contains("copyright") || summaryLc.contains("copyright") ||
                categoryValue == IP_CONTENT_REMOVAL || categoryValue == IP_PERMISSIONS_EXPLOIT
            ) {
                TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseContainsCopyright\")")
                copyrightWarningSeen = true
                return
            }
        } else if (categoryValue == IP_CONTENT_REMOVAL) {
            // IP_CONTENT_REMOVAL always triggers the dialog; report cannot be sent for this category.
            TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseContainsCopyright\")")
            return
        }

        TODO("APR: LLUploadDialog.modalUploadDialog(getString(\"uploading_abuse_report\"))")
        val url: String = TODO("APR: gAgent.getRegionCapability(\"SendUserReport\")") as String
        val sshotUrl: String =
            TODO("APR: gAgent.getRegionCapability(\"SendUserReportWithScreenshot\")") as String
        if (url.isNotEmpty() || sshotUrl.isNotEmpty()) {
            sendReportViaCaps(url, sshotUrl, gatherReport())
            TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseConfirm\")")
            TODO("APR: closeFloater()")
        } else {
            TODO("APR: disable send_btn and cancel_btn; call uploadImage()")
        }
    }

    fun onClickCancel() {
        copyrightWarningSeen = false
        if (picking) closePickTool()
        TODO("APR: closeFloater()")
    }

    fun onClickObjPicker() {
        TODO("APR: ToolObjPicker.getInstance().setExitCallback(::closePickTool)")
        ToolMgr.setTransientTool(TODO("APR: ToolObjPicker.getInstance()") as Tool)
        picking = true
        TODO("APR: clear object_name and owner_name; toggle pick_btn on")
    }

    private fun closePickTool() {
        val objId: String = TODO("APR: ToolObjPicker.getInstance().getObjectID()") as String
        getObjectInfo(objId)
        ToolMgr.clearTransientTool()
        picking = false
        TODO("APR: pick_btn.setToggleState(false)")
    }

    // ---- Validation ---------------------------------------------------------

    private fun validateReport(): Boolean {
        val category: UByte =
            TODO("APR: category_combo.getValue().asInteger().toUByte()") as UByte
        if (category == 0u.toUByte()) {
            TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseSelectCategory\")")
            return false
        }
        if (TODO("APR: abuser_name_edit.getValue().asString().isEmpty()") as Boolean) {
            TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseAbuserNameEmpty\")")
            return false
        }
        if (TODO("APR: abuse_location_edit.getValue().asString().isEmpty()") as Boolean) {
            TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseAbuserLocationEmpty\")")
            return false
        }
        if (TODO("APR: summary_edit.getValue().asString().isEmpty()") as Boolean) {
            TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseSummaryEmpty\")")
            return false
        }
        if (TODO("APR: details_edit.getValue().asString() == defaultSummary") as Boolean) {
            TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseDetailsEmpty\")")
            return false
        }
        return true
    }

    // ---- Report assembly ----------------------------------------------------

    private fun gatherReport(): Map<String, Any?> {
        val region: Any? = TODO("APR: gAgent.getRegion()")
        region ?: return emptyMap()

        copyrightWarningSeen = false

        val isBeta: Boolean = TODO("APR: LLGridManager.getInstance().isInSLBeta()") as Boolean
        val prefix = if (isBeta) "Preview " else ""
        val categoryName: String = TODO("APR: category_combo.getSelectedItemLabel()") as String
        val platform = "JVM"  // Kotlin/JVM replaces the C++ platform-detection macros

        val regionName: String = TODO("APR: region.getName()") as String
        val abuseLocation: String =
            TODO("APR: abuse_location_edit.getValue().asString()") as String
        val abuserName: String =
            TODO("APR: abuser_name_edit.getValue().asString()") as String
        val summaryText: String =
            TODO("APR: summary_edit.getValue().asString()") as String

        val summary = "$prefix |$regionName| ($abuseLocation) [$categoryName]  {$abuserName}  \"$summaryText\""

        val version: String = TODO("APR: LLVersionInfo.instance().getVersion()") as String
        val objectName: String = TODO("APR: object_name.getValue().asString()") as String
        val detailsText: String = TODO("APR: details_edit.getValue().asString()") as String
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

        val shortVersion: String = TODO("APR: LLVersionInfo.instance().getShortVersion()") as String
        val cpuFamily: String = TODO("APR: gSysCPU.getFamily()") as String
        val glRenderer: String = TODO("GPU: gGLManager.mGLRenderer") as String
        val driverVersion: String = TODO("GPU: gGLManager.mDriverVersionVendorString") as String
        val versionString = "$shortVersion $platform $cpuFamily $glRenderer $driverVersion"

        val screenshotId: String = TODO("APR: screenshot ctrl getValue() as UUID string") as String

        return mapOf(
            "report-type" to reportType.id,
            "category" to TODO("APR: category_combo.getValue()"),
            "position" to position,
            "check-flags" to 0,
            "screenshot-id" to screenshotId,
            "object-id" to objectId,
            "abuser-id" to abuserId,
            "abuse-region-name" to "",
            "abuse-region-id" to "",
            "summary" to summary,
            "version-string" to versionString,
            "details" to details,
        )
    }

    // ---- Network send -------------------------------------------------------

    private fun sendReportViaLegacy(report: Map<String, Any?>) {
        TODO("APR: compose and send UserReport UDP message via gMessageSystem")
    }

    private fun sendReportViaCaps(url: String, sshotUrl: String, report: Map<String, Any?>) {
        if (sshotUrl.isNotEmpty()) {
            TODO("APR: LLViewerAssetUpload.EnqueueInventoryUpload(sshotUrl, ARScreenShotUploader(report, assetId, assetType))")
        } else {
            TODO("APR: LLCoreHttpUtil.HttpCoroutineAdapter.callbackHttpPost(url, report, ::finishedARPost, ::finishedARPost)")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun finishedARPost(result: Map<String, Any?>) {
        TODO("APR: LLUploadDialog.modalUploadFinished()")
    }

    // ---- Screenshot ---------------------------------------------------------

    fun takeNewSnapshot(refresh: Boolean) {
        TODO("APR: getChildView(\"send_btn\").setEnabled(true)")
        imageRaw = TODO("APR: LLImageRaw()")

        TODO("APR: setVisible(false)")
        val ok: Boolean = TODO("APR: gViewerWindow.rawSnapshot(imageRaw, IMAGE_WIDTH, IMAGE_HEIGHT, keepAspect=true, noDither=false, includeUI=true, rawSnapshot=true, cropToAspect=false)") as Boolean
        TODO("APR: setVisible(true)")
        if (!ok) return

        val prevExists: Boolean =
            TODO("APR: gSavedPerAccountSettings.getBOOL(\"PreviousScreenshotForReport\")") as Boolean
        if (prevExists && !refresh) {
            val screenshotFilename: String =
                TODO("APR: gDirUtilp.getLindenUserDir() + getDirDelimiter() + SCREEN_PREV_FILENAME") as String
            prevImageRaw = TODO("APR: LLImageRaw()")
            val loaded: Boolean =
                TODO("APR: LLImagePNG.load(screenshotFilename) && decode(prevImageRaw)") as Boolean
            if (loaded) {
                TODO("APR: LLNotificationsUtil.add(\"LoadPreviousReportScreenshot\", handler=::onLoadScreenshotDialog)")
                return
            }
        }
        takeScreenshot(usePrevScreenshot = false)
    }

    private fun takeScreenshot(usePrevScreenshot: Boolean) {
        TODO("APR: gSavedPerAccountSettings.setBOOL(\"PreviousScreenshotForReport\", true)")
        if (!usePrevScreenshot) {
            val screenshotFilename: String =
                TODO("APR: build SCREEN_PREV_FILENAME path in lindenUserDir") as String
            TODO("APR: LLImagePNG.encode(imageRaw, 0f).save(screenshotFilename)")
        } else {
            imageRaw = prevImageRaw
        }

        TODO("APR: LLViewerTextureList.convertToUploadFile(imageRaw) → upload_data (J2C)")
        TODO("APR: populate mResourceDatap: inventoryType=IT_NONE, nextOwnerPerm=0, expectedUploadCost=0, generate transaction/asset IDs, set type=AT_TEXTURE")
        TODO("APR: write upload_data to LLFileSystem cache")
        TODO("GPU: image_in_list = LLViewerTextureManager.getFetchedTexture(assetUuid); image_in_list.createGLTexture(0, imageRaw, 0, true, OTHER)")
        TODO("APR: set screenshot TextureCtrl imageAssetID and defaultImageAssetID to assetUuid")
    }

    fun onLoadScreenshotDialog(selectedOption: Int) {
        takeScreenshot(usePrevScreenshot = selectedOption == 0)
    }

    private fun uploadImage() {
        TODO("APR: gAssetStorage.storeAssetData(transactionId, assetType, ::uploadDoneCallback, resourceDatap)")
    }

    private fun uploadDoneCallback(uuid: String, result: Int) {
        TODO("APR: LLUploadDialog.modalUploadFinished()")
        if (result < 0) {
            TODO("APR: show ErrorUploadingReportScreenshot notification with error reason string")
            return
        }
        screenId = uuid
        sendReportViaLegacy(gatherReport())
        TODO("APR: LLNotificationsUtil.add(\"HelpReportAbuseConfirm\")")
        TODO("APR: closeFloater()")
    }

    // ---- Helpers ------------------------------------------------------------

    private fun setPosBox(pos: Triple<Double, Double, Double>) {
        TODO("APR: format pos as region-local x/y/z and set position field text")
    }

    private fun requestAbuseCategories() {
        val region: Any? = TODO("APR: gAgent.getRegion()")
        region ?: return
        val capUrl: String =
            TODO("APR: region.getCapability(\"AbuseCategories\") ?: \"\"") as String
        if (capUrl.isEmpty()) return
        val lang: String = TODO("APR: gSavedSettings.getString(\"Language\")") as String
        val fullUrl = if (lang != "default" && lang.isNotEmpty()) "$capUrl?lc=$lang" else capUrl
        TODO("APR: launch coroutine requestAbuseCategoriesCoro(fullUrl, getHandle()); on success populate category_combo keeping first 'Select category' entry")
    }

    private fun onUpdateScreenshot() {
        takeNewSnapshot(refresh = true)
    }

    private fun refresh() {
        TODO("APR: rebuild UI fields from current report state")
    }
}
