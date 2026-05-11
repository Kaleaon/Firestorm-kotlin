package com.firestorm.newview

private const val COMPLAINT_REPORT_REQUEST: UInt = 0x01u shl 1
private const val OBJECT_PAY_REQUEST: UInt = 0x01u shl 2

enum class ReportType(val value: Int) {
    NULL_REPORT(0),
    UNKNOWN_REPORT(1),
    COMPLAINT_REPORT(3),
    CS_REQUEST_REPORT(4)
}

class ScreenShotUploader(
    private val report: Map<String, Any>,
    private val assetId: Any,
    private val assetType: Any
) {
    fun prepareUpload(): Map<String, Any> = mapOf("success" to true)
    fun generatePostBody(): Map<String, Any> = report
    fun finishUpload(result: Map<String, Any>): Any? = null
    fun showInventoryPanel(): Boolean = false
    fun getDisplayName(): String = "Abuse Report"
}

class FloaterReporter(val key: Any) {

    private var reportType: ReportType = ReportType.COMPLAINT_REPORT
    private var objectId: Any = nullUUID()
    private var screenId: Any = nullUUID()
    private var abuserID: Any = nullUUID()
    private var experienceID: Any = nullUUID()
    private var ownerName: String = ""
    private var deselectOnClose: Boolean = false
    private var picking: Boolean = false
    private var position: FloatArray = FloatArray(3)
    private var copyrightWarningSeen: Boolean = false
    private var defaultSummary: String = ""
    private var avatarNameCacheConnection: Any? = null

    private var imageRaw: Any? = null
    private var prevImageRaw: Any? = null
    private var snapshotTimer: Any = createFrameTimer()

    init {
        registerIdleCallback(::onIdle)
    }

    fun setReportType(type: ReportType) {
        reportType = type
    }

    fun postBuild(): Boolean {
        val slurl = buildAgentSLURL()
        setChildValue("abuse_location_edit", slurl)

        enableControls(true)

        val pos = agentPositionGlobal()
        val region = agentRegion()
        if (region != null) {
            setChildValue("sim_field", regionName(region))
            subtractRegionOrigin(pos, region)
        }
        setPosBox(pos)

        setChildValue("object_name", "")
        setChildValue("owner_name", "")
        ownerName = ""

        setChildFocus("summary_edit")
        defaultSummary = getChildValue("details_edit")

        setChildEnabled("abuser_name_edit", false)

        setPosBox(position.toList())
        setPickButtonImages("tool_face.tga", "tool_face_active.tga")

        bindChildAction("select_abuser", ::onClickSelectAbuser)
        bindChildStaticAction("send_btn", ::onClickSend)
        bindChildStaticAction("cancel_btn", ::onClickCancel)

        val reporter = agentInspectSLURL()
        setChildValue("reporter_field", reporter)

        bindChildAction("refresh_screenshot") { onUpdateScreenshot() }

        requestAbuseCategories()

        center()
        return true
    }

    fun onOpen(key: Any) = Unit

    fun onClose(appQuitting: Boolean) {
        if (avatarNameCacheConnection != null) {
            disconnectSignal(avatarNameCacheConnection!!)
            avatarNameCacheConnection = null
        }
        objectId = nullUUID()
        if (picking) closePickTool()
        position.fill(0.0f)
        deleteResourceData()
    }

    private fun onIdle() {
        val screenshotDelay = savedSettingFloat("AbuseReportScreenshotDelay")
        if (isTimerStarted(snapshotTimer) && timerElapsed(snapshotTimer) > screenshotDelay) {
            stopTimer(snapshotTimer)
            takeNewSnapshot(false)
        }
    }

    fun enableControls(enable: Boolean) {
        setChildEnabled("category_combo", enable)
        setChildEnabled("chat_check", enable)
        setChildEnabled("screenshot", false)
        setChildEnabled("pick_btn", enable)
        setChildEnabled("summary_edit", enable)
        setChildEnabled("details_edit", enable)
        setChildEnabled("send_btn", enable)
        setChildEnabled("cancel_btn", enable)
    }

    fun getExperienceInfo(experienceIdParam: Any) {
        experienceID = experienceIdParam
        if (!isNullUUID(experienceID)) {
            val experience = lookupExperience(experienceID)
            val desc = if (experience != null) {
                setFromAvatarID(experienceAgentId(experience))
                "Experience id: $experienceID"
            } else {
                "Unable to retrieve details for id: $experienceID"
            }
            setChildValue("details_edit", desc)
        }
    }

    fun getObjectInfo(objectIdParam: Any) {
        objectId = objectIdParam
        if (isNullUUID(objectId)) return

        val obj = findViewerObject(objectId) ?: return
        val root = if (isAttachment(obj)) rootObject(obj) else obj
        objectId = objectId(root)

        val region = objectRegion(root) ?: return
        setChildValue("sim_field", regionName(region))
        val globalPos = objectPositionRegion(root)
        setPosBox(globalPos)

        if (isAvatar(root)) {
            setFromAvatarID(objectId)
        } else {
            TODO("APR: send RequestObjectPropertiesFamily message to simulator via gMessageSystem")
        }
    }

    fun onClickSelectAbuser() {
        TODO("APR: show LLFloaterAvatarPicker and wire callbackAvatarID")
    }

    fun callbackAvatarID(ids: List<Any>, names: List<Any>) {
        if (ids.isEmpty() || names.isEmpty()) return
        setChildValue("abuser_name_edit", avatarCompleteName(names[0]))
        abuserID = ids[0]
    }

    fun setPickedObjectProperties(objectName: String, ownerNameParam: String, ownerId: Any) {
        setChildValue("object_name", objectName)
        setChildValue("owner_name", ownerNameParam)
        ownerName = ownerNameParam
    }

    fun onLoadScreenshotDialog(notification: Map<String, Any>, response: Map<String, Any>) {
        TODO("APR: handle screenshot load confirmation dialog response")
    }

    fun takeNewSnapshot(refresh: Boolean) {
        if (refresh) {
            takeScreenshot(usePrevScreenshot = true)
        } else {
            takeScreenshot(usePrevScreenshot = false)
        }
    }

    private fun takeScreenshot(usePrevScreenshot: Boolean) {
        TODO("GPU: capture viewport screenshot into imageRaw; write PNG to prev file if applicable")
    }

    private fun uploadImage() {
        TODO("APR: upload imageRaw as asset via LLViewerAssetUpload")
    }

    private fun validateReport(): Boolean {
        TODO("APR: validate required report fields; show notification if invalid")
    }

    private fun gatherReport(): Map<String, Any> {
        TODO("APR: build LLSD report map from form fields")
    }

    private fun sendReportViaLegacy(report: Map<String, Any>) {
        TODO("APR: send via gMessageSystem UserReport message")
    }

    private fun sendReportViaCaps(url: String, sshotUrl: String, report: Map<String, Any>) {
        TODO("APR: coroutine HTTP POST report + screenshot URL to cap endpoint")
    }

    private fun setPosBox(pos: Any) {
        TODO("APR: format position and set pos_field child value")
    }

    private fun setFromAvatarID(avatarId: Any) {
        TODO("APR: look up avatar name and populate abuser fields")
    }

    private fun onAvatarNameCache(avatarId: Any, avName: Any) {
        TODO("APR: set abuser_name_edit from avName.getCompleteName()")
    }

    private fun onUpdateScreenshot() {
        TODO("APR: LLFloaterReg.hideInstance(\"upload_dialog\"); takeNewSnapshot(true)")
    }

    private fun requestAbuseCategories() {
        if (agentRegion() != null && regionCapabilitiesReceived()) {
            val capUrl = regionCapability(agentRegion()!!, "AbuseCategories")
            if (capUrl.isNotEmpty()) {
                val lang = savedSettingString("Language")
                val fullUrl = if (lang != "default" && lang.isNotEmpty()) "$capUrl?lc=$lang" else capUrl
                TODO("APR: launch requestAbuseCategoriesCoro coroutine with fullUrl")
            }
        }
    }

    private fun closePickTool() = TODO("APR: LLToolObjPicker close / LLToolMgr clear")
    private fun deleteResourceData() = TODO("APR: delete mResourceDatap")
    private fun center() = TODO("APR: LLFloater.center()")

    companion object {
        fun showFromMenu(reportType: ReportType) {
            TODO("APR: LLFloaterReg.showInstance(\"reporter\", LLSD); enable controls")
        }

        fun showFromObject(objectId: Any, experienceId: Any? = null) {
            TODO("APR: show reporter floater; populate object info")
        }

        fun showFromAvatar(avatarId: Any, avatarName: String) {
            TODO("APR: show reporter floater; populate avatar info")
        }

        fun showFromChat(avatarId: Any, avatarName: String, time: String, description: String) {
            TODO("APR: show reporter floater; populate chat info")
        }

        fun showFromExperience(experienceId: Any) {
            TODO("APR: show reporter floater; populate experience info")
        }

        fun onClickSend(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            if (!floater.validateReport()) return
            TODO("APR: floater.uploadImage() then sendReportViaCaps or sendReportViaLegacy")
            floater.closeFloater()
        }

        fun onClickCancel(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            floater.closeFloater()
        }

        fun onClickObjPicker(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            floater.picking = true
            TODO("APR: LLToolMgr.getInstance().setTransientTool(LLToolObjPicker)")
        }

        fun closePickTool(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            floater.picking = false
            TODO("APR: LLToolMgr clear transient tool")
        }

        fun uploadDoneCallback(uuid: Any, userData: Any?, result: Int, extStatus: Any) {
            TODO("APR: handle asset upload completion; send report via caps")
        }

        private suspend fun requestAbuseCategoriesCoro(url: String, floaterHandle: Any) {
            TODO("APR: HTTP GET categories; populate category_combo in floater via handle")
        }

        private fun finishedARPost(result: Any) {
            TODO("APR: handle completed abuse report post")
        }
    }

    // --- stubs for platform calls ---
    private fun buildAgentSLURL(): String = TODO("APR: LLAgentUI.buildSLURL(slurl)")
    private fun agentPositionGlobal(): FloatArray = TODO("APR: gAgent.getPositionGlobal()")
    private fun agentRegion(): Any? = TODO("APR: gAgent.getRegion()")
    private fun regionName(region: Any): String = TODO("APR: region.getName()")
    private fun subtractRegionOrigin(pos: FloatArray, region: Any) = TODO("APR: pos -= region.getOriginGlobal()")
    private fun regionCapabilitiesReceived(): Boolean = TODO("APR: region.capabilitiesReceived()")
    private fun regionCapability(region: Any, cap: String): String = TODO("APR: region.getCapability(cap)")
    private fun agentInspectSLURL(): String = TODO("APR: LLSLURL(\"agent\", gAgent.getID(), \"inspect\").getSLURLString()")
    private fun setPickButtonImages(normal: String, active: String) = TODO("APR: pick_btn.setImages(normal, active)")
    private fun lookupExperience(id: Any): Any? = TODO("APR: LLExperienceCache.instance().get(id)")
    private fun experienceAgentId(exp: Any): Any = TODO("APR: exp[LLExperienceCache.AGENT_ID]")
    private fun findViewerObject(id: Any): Any? = TODO("APR: gObjectList.findObject(id)")
    private fun isAttachment(obj: Any): Boolean = TODO("APR: obj.isAttachment()")
    private fun rootObject(obj: Any): Any = TODO("APR: obj.getRoot()")
    private fun objectId(obj: Any): Any = TODO("APR: obj.getID()")
    private fun objectRegion(obj: Any): Any? = TODO("APR: obj.getRegion()")
    private fun objectPositionRegion(obj: Any): FloatArray = TODO("APR: obj.getPositionRegion()")
    private fun isAvatar(obj: Any): Boolean = TODO("APR: obj.isAvatar()")
    private fun avatarCompleteName(name: Any): String = TODO("APR: name.getCompleteName()")
    private fun isNullUUID(id: Any): Boolean = TODO("APR: id == LLUUID::null")
    private fun nullUUID(): Any = TODO("APR: LLUUID::null")
    private fun createFrameTimer(): Any = TODO("APR: LLFrameTimer()")
    private fun isTimerStarted(timer: Any): Boolean = TODO("APR: timer.getStarted()")
    private fun timerElapsed(timer: Any): Float = TODO("APR: timer.getElapsedTimeF32()")
    private fun stopTimer(timer: Any) = TODO("APR: timer.stop()")
    private fun registerIdleCallback(fn: () -> Unit) = TODO("APR: gIdleCallbacks.addFunction(fn)")
    private fun disconnectSignal(conn: Any) = TODO("APR: conn.disconnect()")
    private fun bindChildAction(id: String, fn: () -> Unit) = TODO("APR: childSetAction(id, fn)")
    private fun bindChildStaticAction(id: String, fn: (Any?) -> Unit) = TODO("APR: childSetAction(id, fn)")
    private fun setChildValue(id: String, value: Any) = TODO("APR: getChild<LLUICtrl>(id).setValue(value)")
    private fun getChildValue(id: String): String = TODO("APR: getChild<LLUICtrl>(id).getValue().asString()")
    private fun setChildEnabled(id: String, enabled: Boolean) = TODO("APR: getChildView(id).setEnabled(enabled)")
    private fun setChildFocus(id: String) = TODO("APR: getChild<LLUICtrl>(id).setFocus(true)")
    private fun savedSettingFloat(key: String): Float = TODO("APR: gSavedSettings cached float control $key")
    private fun savedSettingString(key: String): String = TODO("APR: gSavedSettings.getString($key)")
    fun closeFloater() = TODO("APR: LLFloater.closeFloater()")
    fun validateReport(): Boolean = TODO("APR: validate report fields")
}
