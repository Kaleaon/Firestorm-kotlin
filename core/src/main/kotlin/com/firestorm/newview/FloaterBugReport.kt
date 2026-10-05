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
            System.err.println("FloaterReporter: getObjectInfo not yet implemented")
        }
    }

    fun onClickSelectAbuser() {
        System.err.println("FloaterReporter: onClickSelectAbuser not yet implemented")
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
        System.err.println("FloaterReporter: onLoadScreenshotDialog not yet implemented")
    }

    fun takeNewSnapshot(refresh: Boolean) {
        if (refresh) {
            takeScreenshot(usePrevScreenshot = true)
        } else {
            takeScreenshot(usePrevScreenshot = false)
        }
    }

    private fun takeScreenshot(usePrevScreenshot: Boolean) {
        System.err.println("FloaterReporter: takeScreenshot not yet implemented")
    }

    private fun uploadImage() {
        System.err.println("FloaterReporter: uploadImage not yet implemented")
    }

    private fun validateReport(): Boolean {
        System.err.println("FloaterReporter: validateReport not yet implemented")
        return false
    }

    private fun gatherReport(): Map<String, Any> {
        System.err.println("FloaterReporter: gatherReport not yet implemented")
        return emptyMap()
    }

    private fun sendReportViaLegacy(report: Map<String, Any>) {
        System.err.println("FloaterReporter: sendReportViaLegacy not yet implemented")
    }

    private fun sendReportViaCaps(url: String, sshotUrl: String, report: Map<String, Any>) {
        System.err.println("FloaterReporter: sendReportViaCaps not yet implemented")
    }

    private fun setPosBox(pos: Any) {
        System.err.println("FloaterReporter: setPosBox not yet implemented")
    }

    private fun setFromAvatarID(avatarId: Any) {
        System.err.println("FloaterReporter: setFromAvatarID not yet implemented")
    }

    private fun onAvatarNameCache(avatarId: Any, avName: Any) {
        System.err.println("FloaterReporter: onAvatarNameCache not yet implemented")
    }

    private fun onUpdateScreenshot() {
        System.err.println("FloaterReporter: onUpdateScreenshot not yet implemented")
    }

    private fun requestAbuseCategories() {
        if (agentRegion() != null && regionCapabilitiesReceived()) {
            val capUrl = regionCapability(agentRegion()!!, "AbuseCategories")
            if (capUrl.isNotEmpty()) {
                val lang = savedSettingString("Language")
                val fullUrl = if (lang != "default" && lang.isNotEmpty()) "$capUrl?lc=$lang" else capUrl
                System.err.println("FloaterReporter: requestAbuseCategories not yet implemented")
            }
        }
    }

    private fun closePickTool() { System.err.println("FloaterReporter: closePickTool not yet implemented") }
    private fun deleteResourceData() { System.err.println("FloaterReporter: deleteResourceData not yet implemented") }
    private fun center() { System.err.println("FloaterReporter: center not yet implemented") }

    companion object {
        fun showFromMenu(reportType: ReportType) {
            System.err.println("FloaterReporter: showFromMenu not yet implemented")
        }

        fun showFromObject(objectId: Any, experienceId: Any? = null) {
            System.err.println("FloaterReporter: showFromObject not yet implemented")
        }

        fun showFromAvatar(avatarId: Any, avatarName: String) {
            System.err.println("FloaterReporter: showFromAvatar not yet implemented")
        }

        fun showFromChat(avatarId: Any, avatarName: String, time: String, description: String) {
            System.err.println("FloaterReporter: showFromChat not yet implemented")
        }

        fun showFromExperience(experienceId: Any) {
            System.err.println("FloaterReporter: showFromExperience not yet implemented")
        }

        fun onClickSend(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            if (!floater.validateReport()) return
            System.err.println("FloaterReporter: onClickSend not yet implemented")
            floater.closeFloater()
        }

        fun onClickCancel(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            floater.closeFloater()
        }

        fun onClickObjPicker(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            floater.picking = true
            System.err.println("FloaterReporter: onClickObjPicker not yet implemented")
        }

        fun closePickTool(userData: Any?) {
            val floater = userData as? FloaterReporter ?: return
            floater.picking = false
            System.err.println("FloaterReporter: closePickTool not yet implemented")
        }

        fun uploadDoneCallback(uuid: Any, userData: Any?, result: Int, extStatus: Any) {
            System.err.println("FloaterReporter: uploadDoneCallback not yet implemented")
        }

        private suspend fun requestAbuseCategoriesCoro(url: String, floaterHandle: Any) {
            System.err.println("FloaterReporter: requestAbuseCategoriesCoro not yet implemented")
        }

        private fun finishedARPost(result: Any) {
            System.err.println("FloaterReporter: finishedARPost not yet implemented")
        }
    }

    // --- stubs for platform calls ---
    private fun buildAgentSLURL(): String { System.err.println("FloaterReporter: buildAgentSLURL not yet implemented"); return "" }
    private fun agentPositionGlobal(): FloatArray { System.err.println("FloaterReporter: agentPositionGlobal not yet implemented"); return FloatArray(3) }
    private fun agentRegion(): Any? { System.err.println("FloaterReporter: agentRegion not yet implemented"); return null }
    private fun regionName(region: Any): String { System.err.println("FloaterReporter: regionName not yet implemented"); return "" }
    private fun subtractRegionOrigin(pos: FloatArray, region: Any) { System.err.println("FloaterReporter: subtractRegionOrigin not yet implemented") }
    private fun regionCapabilitiesReceived(): Boolean { System.err.println("FloaterReporter: regionCapabilitiesReceived not yet implemented"); return false }
    private fun regionCapability(region: Any, cap: String): String { System.err.println("FloaterReporter: regionCapability not yet implemented"); return "" }
    private fun agentInspectSLURL(): String { System.err.println("FloaterReporter: agentInspectSLURL not yet implemented"); return "" }
    private fun setPickButtonImages(normal: String, active: String) { System.err.println("FloaterReporter: setPickButtonImages not yet implemented") }
    private fun lookupExperience(id: Any): Any? { System.err.println("FloaterReporter: lookupExperience not yet implemented"); return null }
    private fun experienceAgentId(exp: Any): Any { System.err.println("FloaterReporter: experienceAgentId not yet implemented"); return "" }
    private fun findViewerObject(id: Any): Any? { System.err.println("FloaterReporter: findViewerObject not yet implemented"); return null }
    private fun isAttachment(obj: Any): Boolean { System.err.println("FloaterReporter: isAttachment not yet implemented"); return false }
    private fun rootObject(obj: Any): Any { System.err.println("FloaterReporter: rootObject not yet implemented"); return obj }
    private fun objectId(obj: Any): Any { System.err.println("FloaterReporter: objectId not yet implemented"); return "" }
    private fun objectRegion(obj: Any): Any? { System.err.println("FloaterReporter: objectRegion not yet implemented"); return null }
    private fun objectPositionRegion(obj: Any): FloatArray { System.err.println("FloaterReporter: objectPositionRegion not yet implemented"); return FloatArray(3) }
    private fun isAvatar(obj: Any): Boolean { System.err.println("FloaterReporter: isAvatar not yet implemented"); return false }
    private fun avatarCompleteName(name: Any): String { System.err.println("FloaterReporter: avatarCompleteName not yet implemented"); return "" }
    private fun isNullUUID(id: Any): Boolean { System.err.println("FloaterReporter: isNullUUID not yet implemented"); return false }
    private fun nullUUID(): Any { System.err.println("FloaterReporter: nullUUID not yet implemented"); return "" }
    private fun createFrameTimer(): Any { System.err.println("FloaterReporter: createFrameTimer not yet implemented"); return Object() }
    private fun isTimerStarted(timer: Any): Boolean { System.err.println("FloaterReporter: isTimerStarted not yet implemented"); return false }
    private fun timerElapsed(timer: Any): Float { System.err.println("FloaterReporter: timerElapsed not yet implemented"); return 0f }
    private fun stopTimer(timer: Any) { System.err.println("FloaterReporter: stopTimer not yet implemented") }
    private fun registerIdleCallback(fn: () -> Unit) { System.err.println("FloaterReporter: registerIdleCallback not yet implemented") }
    private fun disconnectSignal(conn: Any) { System.err.println("FloaterReporter: disconnectSignal not yet implemented") }
    private fun bindChildAction(id: String, fn: () -> Unit) { System.err.println("FloaterReporter: bindChildAction not yet implemented") }
    private fun bindChildStaticAction(id: String, fn: (Any?) -> Unit) { System.err.println("FloaterReporter: bindChildStaticAction not yet implemented") }
    private fun setChildValue(id: String, value: Any) { System.err.println("FloaterReporter: setChildValue not yet implemented") }
    private fun getChildValue(id: String): String { System.err.println("FloaterReporter: getChildValue not yet implemented"); return "" }
    private fun setChildEnabled(id: String, enabled: Boolean) { System.err.println("FloaterReporter: setChildEnabled not yet implemented") }
    private fun setChildFocus(id: String) { System.err.println("FloaterReporter: setChildFocus not yet implemented") }
    private fun savedSettingFloat(key: String): Float { System.err.println("FloaterReporter: savedSettingFloat not yet implemented"); return 0f }
    private fun savedSettingString(key: String): String { System.err.println("FloaterReporter: savedSettingString not yet implemented"); return "" }
    fun closeFloater() { System.err.println("FloaterReporter: closeFloater not yet implemented") }
    fun validateReport(): Boolean { System.err.println("FloaterReporter: validateReport not yet implemented"); return false }
}
