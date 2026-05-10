package com.firestorm.newview

// ---------------------------------------------------------------------------
// Flickr connection state enum
// ---------------------------------------------------------------------------

enum class FlickrConnectionState {
    NOT_CONNECTED,
    CONNECTION_IN_PROGRESS,
    CONNECTED,
    POSTING,
    POSTED,
    CONNECTION_FAILED,
    POST_FAILED,
    DISCONNECTING,
    DISCONNECT_FAILED
}

// ---------------------------------------------------------------------------
// Minimal stubs for Flickr subsystems
// ---------------------------------------------------------------------------

private object LLFlickrConnect {
    private var state: FlickrConnectionState = FlickrConnectionState.NOT_CONNECTED

    fun instance(): LLFlickrConnect = this

    fun getConnectionState(): FlickrConnectionState = state
    fun setConnectionState(s: FlickrConnectionState) { state = s }
    fun isTransactionOngoing(): Boolean = state == FlickrConnectionState.POSTING
    fun isConnected(): Boolean = state == FlickrConnectionState.CONNECTED

    fun checkConnectionToFlickr(force: Boolean = false) {
        TODO("APR: use JVM equivalent for Flickr connection check")
    }
    fun disconnectFromFlickr() {
        TODO("APR: use JVM equivalent for Flickr disconnect")
    }
    fun loadFlickrInfo() {
        TODO("APR: use JVM equivalent for loading Flickr account info")
    }
    fun getInfo(): Map<String, Any?> = TODO("APR: use JVM equivalent for Flickr info map")
}

private object ExoFlickr {
    fun uploadPhoto(
        params: Map<String, Any?>,
        image: Any?,
        cb: (Boolean, Map<String, Any?>) -> Unit
    ) {
        TODO("APR: use JVM equivalent for exoFlickr photo upload")
    }
}

private class ExoFlickrAuth(cb: (Boolean, Map<String, Any?>) -> Unit) {
    init {
        TODO("APR: use JVM equivalent for exoFlickrAuth initialisation")
    }
}

private object LLImageFiltersManager {
    fun getInstance(): LLImageFiltersManager = this
    fun getFiltersList(): List<String> = TODO("APR: use JVM equivalent for image filters list")
}

private object LLFlickrEventPumps {
    private val listeners: MutableMap<String, MutableMap<String, (Map<String, Any?>) -> Boolean>> =
        mutableMapOf()

    fun obtain(name: String): FlickrPump = FlickrPump(name, listeners)
}

private class FlickrPump(
    val name: String,
    val map: MutableMap<String, MutableMap<String, (Map<String, Any?>) -> Boolean>>
) {
    fun stopListening(listener: String) {
        map[name]?.remove(listener)
    }
    fun listen(listener: String, cb: (Map<String, Any?>) -> Boolean) {
        map.getOrPut(name) { mutableMapOf() }[listener] = cb
    }
}

private object LLGridManager {
    fun instance(): LLGridManager = this
    fun isInSecondLife(): Boolean = TODO("APR: use JVM equivalent for grid check")
    fun getGridId(): String = TODO("APR: use JVM equivalent for grid id")
    fun getGridLabel(): String = TODO("APR: use JVM equivalent for grid label")
}

private object LLAgentUI {
    fun buildSLURL(slurl: SLURLStub): Boolean = TODO("APR: use JVM equivalent for building agent SLURL")
}

private class SLURLStub {
    fun getSLURLString(): String = TODO("APR: use JVM equivalent for SLURL string")
}

private object LLViewerParcelMgr {
    fun getInstance(): LLViewerParcelMgr = this
    fun getAgentParcelName(): String = TODO("APR: use JVM equivalent for parcel name")
    fun getAgentParcel(): ParcelStub? = TODO("APR: use JVM equivalent for agent parcel")
}

private class ParcelStub

private object LLViewerRegionAgent {
    fun getRegion(): RegionAgentStub? = TODO("APR: use JVM equivalent for agent region")
    fun getPositionAgent(): Triple<Float, Float, Float> = TODO("APR: use JVM equivalent for agent position")
}

private class RegionAgentStub {
    fun getName(): String = TODO("APR: use JVM equivalent for region name")
}

private object LLFloaterRegStub {
    fun getInstance(name: String): Any? = TODO("GPU: get floater instance $name")
    fun instanceVisible(name: String): Boolean = TODO("GPU: check floater visibility $name")
    fun hideInstance(name: String) = TODO("GPU: hide floater instance $name")
    fun showInstance(name: String) = TODO("GPU: show floater instance $name")
}

private object LLNotificationsUtilFlickr {
    fun add(name: String, args: Map<String, Any?> = emptyMap()) {
        TODO("APR: use JVM equivalent for notification $name")
    }
}

private object LLTrans {
    fun getString(key: String): String = TODO("APR: use JVM equivalent for translation $key")
}

// ---------------------------------------------------------------------------
// LLFlickrPhotoPanel
// ---------------------------------------------------------------------------

class LLFlickrPhotoPanel {

    private var previewHandle: Any? = null      // LLSnapshotLivePreview handle
    private var resolutionComboBox: Any? = null
    private var filterComboBox: Any? = null
    private var refreshBtn: Any? = null
    private var workingLabel: Any? = null
    private var thumbnailPlaceholder: Any? = null
    private var titleTextBox: Any? = null
    private var descriptionTextBox: Any? = null
    private var locationCheckbox: Any? = null
    private var tagsTextBox: Any? = null
    private var ratingComboBox: Any? = null
    private var postButton: Any? = null
    private var cancelButton: Any? = null
    private var btnPreview: Any? = null
    private var bigPreviewFloater: Any? = null  // LLFloaterBigPreview

    private val DEFAULT_TAG_TEXT = "Firestorm "

    private fun <T> getChild(name: String): T = TODO("GPU: getChild<$name>")
    private fun <T> findChild(name: String): T? = TODO("GPU: findChild<$name>")
    private fun setVisible(visible: Boolean) = TODO("GPU: setVisible($visible)")
    private fun hasFocus(): Boolean = TODO("GPU: hasFocus()")
    private fun getParentByType(): Any? = TODO("GPU: getParentByType<LLFloater>()")
    private fun getRootViewRect(): Any = TODO("GPU: getRootView()->getRect()")
    private fun setEnabled(widget: Any?, enabled: Boolean) = TODO("GPU: widget.setEnabled($enabled)")
    private fun getVisible(widget: Any?): Boolean = TODO("GPU: widget.getVisible()")
    private fun setValue(widget: Any?, value: Any) = TODO("GPU: widget.setValue($value)")
    private fun getValue(widget: Any?): Any = TODO("GPU: widget.getValue()")
    private fun setVisibleCallback(cb: (Boolean) -> Unit) = TODO("GPU: setVisibleCallback")
    private fun getPreviewSnapshotUpToDate(preview: Any?): Boolean =
        TODO("GPU: previewp->getSnapshotUpToDate()")
    private fun updateSnapshotPreview(preview: Any?, updateDims: Boolean, updateFilter: Boolean = false) =
        TODO("GPU: previewp->updateSnapshot($updateDims, $updateFilter)")

    fun postBuild(): Boolean {
        setVisibleCallback { visible -> onVisibilityChange(visible) }

        resolutionComboBox = getChild<Any>("resolution_combobox")
        filterComboBox     = getChild<Any>("filters_combobox")
        refreshBtn         = getChild<Any>("new_snapshot_btn")
        btnPreview         = getChild<Any>("big_preview_btn")
        workingLabel       = getChild<Any>("working_lbl")
        thumbnailPlaceholder = getChild<Any>("thumbnail_placeholder")
        titleTextBox       = getChild<Any>("photo_title")
        descriptionTextBox = getChild<Any>("photo_description")
        locationCheckbox   = getChild<Any>("add_location_cb")
        tagsTextBox        = getChild<Any>("photo_tags")
        ratingComboBox     = getChild<Any>("rating_combobox")
        postButton         = getChild<Any>("post_photo_btn")
        cancelButton       = getChild<Any>("cancel_photo_btn")
        bigPreviewFloater  = LLFloaterRegStub.getInstance("big_preview")

        val gridId = LLGridManager.instance().getGridId()
        val tagSuffix = if (LLGridManager.instance().isInSecondLife()) "secondlife"
                        else "\"$gridId\""
        setValue(tagsTextBox, "$DEFAULT_TAG_TEXT$tagSuffix ")

        TODO("GPU: wire resolution/filter/custom-size commit callbacks; restore saved resolution/dimensions")

        val filterList = LLImageFiltersManager.getInstance().getFiltersList()
        TODO("GPU: populate filterComboBox with filterList")

        return true
    }

    fun notify(info: Map<String, Any?>): Int {
        if (info.containsKey("snapshot-updating")) return 1
        if (info.containsKey("snapshot-updated")) {
            updateControls()
            if (!getVisible(refreshBtn)) setValue(refreshBtn, true) // setVisible
            return 1
        }
        return 0
    }

    fun draw() {
        val connState = LLFlickrConnect.instance().getConnectionState()
        val noOngoingConnection = !LLFlickrConnect.instance().isTransactionOngoing() &&
            connState != FlickrConnectionState.CONNECTION_IN_PROGRESS &&
            connState != FlickrConnectionState.CONNECTION_FAILED &&
            connState != FlickrConnectionState.NOT_CONNECTED

        setEnabled(cancelButton, noOngoingConnection)
        setEnabled(titleTextBox, noOngoingConnection)
        setEnabled(descriptionTextBox, noOngoingConnection)
        setEnabled(tagsTextBox, noOngoingConnection)
        setEnabled(ratingComboBox, noOngoingConnection)
        setEnabled(resolutionComboBox, noOngoingConnection)
        setEnabled(filterComboBox, noOngoingConnection)
        setEnabled(refreshBtn, noOngoingConnection)
        setEnabled(btnPreview, noOngoingConnection)
        setEnabled(locationCheckbox, noOngoingConnection)

        if (hasFocus() && isPreviewVisible()) attachPreview()

        TODO("GPU: toggle btnPreview toggle state; draw thumbnail if previewp->getThumbnailImage() available; update workingLabel; enable/disable postButton")

        TODO("GPU: LLPanel::draw()")
    }

    fun getPreviewView(): Any? = previewHandle

    fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            val existingPreview = previewHandle
            if (existingPreview != null) {
                TODO("GPU: previewp->updateSnapshot(true) on re-show")
            } else {
                TODO("GPU: create LLSnapshotLivePreview with full-screen rect, configure it, assign handle, call updateControls()")
            }
        }
    }

    fun onClickNewSnapshot() {
        TODO("GPU: previewp->updateSnapshot(true)")
    }

    fun onClickBigPreview() {
        if (isPreviewVisible()) {
            LLFloaterRegStub.hideInstance("big_preview")
        } else {
            attachPreview()
            LLFloaterRegStub.showInstance("big_preview")
        }
    }

    fun onSend() {
        LLFlickrEventPumps.obtain("FlickrConnectState").stopListening("LLFlickrPhotoPanel")
        LLFlickrEventPumps.obtain("FlickrConnectState").listen("LLFlickrPhotoPanel") { data ->
            onFlickrConnectStateChange(data)
        }
        sendPhoto()
    }

    fun onFlickrConnectStateChange(data: Map<String, Any?>): Boolean {
        val enumVal = (data["enum"] as? Int) ?: return false
        when (FlickrConnectionState.entries.getOrNull(enumVal)) {
            FlickrConnectionState.CONNECTED -> sendPhoto()
            FlickrConnectionState.POSTED    ->
                LLFlickrEventPumps.obtain("FlickrConnectState").stopListening("LLFlickrPhotoPanel")
            else -> {}
        }
        return false
    }

    fun sendPhoto() {
        val title       = getValue(titleTextBox).toString()
        val description = getValue(descriptionTextBox).toString()
        var tags        = getValue(tagsTextBox).toString()
        val addLocation = getValue(locationCheckbox) as? Boolean ?: false

        var finalDescription = description

        if (addLocation) {
            val slurl = SLURLStub()
            LLAgentUI.buildSLURL(slurl)
            val slurlString = slurl.getSLURLString()

            var photoLinkText = "Visit this location"
            val parcelName = LLViewerParcelMgr.getInstance().getAgentParcelName()
            if (parcelName.isNotEmpty()) {
                // Only append parcel name if it doesn't look like a domain (no TLD dot)
                if (!parcelName.contains(Regex("\\S\\.[a-zA-Z]{2,}"))) {
                    photoLinkText += " at $parcelName"
                }
            }
            photoLinkText += if (LLGridManager.instance().isInSecondLife()) {
                " in Second Life"
            } else {
                " in \"${LLGridManager.instance().getGridLabel()}\""
            }

            val linkedSlurl = "<a href=\"$slurlString\">$photoLinkText</a>"
            finalDescription = if (description.isEmpty()) linkedSlurl else "$description\n\n$linkedSlurl"

            val (px, py, pz) = LLViewerRegionAgent.getPositionAgent()
            val region = LLViewerRegionAgent.getRegion()
            val parcel = LLViewerParcelMgr.getInstance().getAgentParcel()
            if (region != null && parcel != null) {
                val posX = px.toInt(); val posY = py.toInt(); val posZ = pz.toInt()
                val namespace = if (LLGridManager.instance().isInSecondLife()) "secondlife"
                                else LLGridManager.instance().getGridId()
                val regionName = region.getName()
                if (regionName.isNotEmpty()) tags += " \"$namespace:region=$regionName\""
                if (parcelName.isNotEmpty()) tags += " \"$namespace:parcel=$parcelName\""
                tags += " \"$namespace:x=$posX\" \"$namespace:y=$posY\" \"$namespace:z=$posZ\""
            }
        }

        val contentRating = getValue(ratingComboBox) as? Int ?: 1
        val image = getPreviewView()

        LLFlickrConnect.instance().setConnectionState(FlickrConnectionState.POSTING)
        val params = mapOf<String, Any?>(
            "title"        to title,
            "safety_level" to contentRating,
            "tags"         to tags,
            "description"  to finalDescription
        )
        ExoFlickr.uploadPhoto(params, image) { success, response ->
            uploadCallback(success, response)
        }
        updateControls()
    }

    fun clearAndClose() {
        setValue(titleTextBox, "")
        setValue(descriptionTextBox, "")
        val floater = getParentByType()
        if (floater != null) {
            TODO("GPU: floater.closeFloater(); bigPreviewFloater?.closeOnFloaterOwnerClosing(floater)")
        }
    }

    fun updateControls() {
        updateResolution(false)
    }

    fun updateResolution(doUpdate: Boolean) {
        TODO("GPU: read resolution/filter combos; set preview size; conditionally call updateSnapshot; toggle custom-size spinners")
    }

    fun checkAspectRatio(index: Int) {
        val keepAspect = when (index) {
            0    -> true         // current window size
            -1   -> TODO<Boolean>("GPU: getChild<LLCheckBoxCtrl>(\"keep_aspect_ratio\").get()")
            else -> false
        }
        TODO("GPU: previewp.mKeepAspectRatio = $keepAspect")
    }

    fun getRefreshBtn(): Any? = refreshBtn

    fun onOpen(key: Map<String, Any?>) {
        LLFlickrConnect.instance().setConnectionState(FlickrConnectionState.CONNECTION_IN_PROGRESS)
        ExoFlickrAuth { success, response -> flickrAuthResponse(success, response) }
    }

    fun uploadCallback(success: Boolean, response: Map<String, Any?>) {
        val args = mutableMapOf<String, Any?>()
        if (success && response["stat"]?.toString() == "ok") {
            LLFlickrConnect.instance().setConnectionState(FlickrConnectionState.POSTED)
            args["ID"] = response["photoid"]
            LLNotificationsUtilFlickr.add("ExodusFlickrUploadComplete", args)
        } else {
            LLFlickrConnect.instance().setConnectionState(FlickrConnectionState.POST_FAILED)
        }
    }

    fun flickrAuthResponse(success: Boolean, response: Map<String, Any?>) {
        if (!success) {
            LLFlickrConnect.instance().setConnectionState(FlickrConnectionState.CONNECTION_FAILED)
        } else {
            LLFlickrConnect.instance().setConnectionState(FlickrConnectionState.CONNECTED)
        }
    }

    private fun isPreviewVisible(): Boolean =
        TODO("GPU: bigPreviewFloater != null && bigPreviewFloater.getVisible()")

    private fun attachPreview() {
        TODO("GPU: bigPreviewFloater?.setPreview(previewp); bigPreviewFloater?.setFloaterOwner(parentFloater)")
    }

    private fun checkImageSize(
        previewp: Any?,
        width: IntArray,
        height: IntArray,
        isWidthChanged: Boolean,
        maxValue: Int
    ): Boolean {
        TODO("GPU: aspect-ratio clamping for custom snapshot resolution")
    }
}

// ---------------------------------------------------------------------------
// LLFlickrAccountPanel
// ---------------------------------------------------------------------------

class LLFlickrAccountPanel {

    private var accountCaptionLabel: Any? = null
    private var accountNameLabel: Any? = null
    private var panelButtons: Any? = null
    private var connectButton: Any? = null
    private var disconnectButton: Any? = null

    private fun <T> getChild(name: String): T = TODO("GPU: getChild<$name>")
    private fun setVisible(widget: Any?, visible: Boolean) = TODO("GPU: widget.setVisible($visible)")
    private fun setEnabled(widget: Any?, enabled: Boolean) = TODO("GPU: widget.setEnabled($enabled)")
    private fun getVisible(widget: Any?): Boolean = TODO("GPU: widget.getVisible()")
    private fun setText(widget: Any?, text: String) = TODO("GPU: widget.setText($text)")
    private fun getString(key: String): String = TODO("GPU: getString($key)")
    private fun setVisibleCallback(cb: (Boolean) -> Unit) = TODO("GPU: setVisibleCallback")

    fun postBuild(): Boolean {
        accountCaptionLabel = getChild<Any>("account_caption_label")
        accountNameLabel    = getChild<Any>("account_name_label")
        panelButtons        = getChild<Any>("panel_buttons")
        connectButton       = getChild<Any>("connect_btn")
        disconnectButton    = getChild<Any>("disconnect_btn")

        setVisibleCallback { visible -> onVisibilityChange(visible) }
        return true
    }

    fun draw() {
        val connState = LLFlickrConnect.instance().getConnectionState()
        val disconnecting = connState == FlickrConnectionState.DISCONNECTING
        setEnabled(disconnectButton, !disconnecting)

        val connecting = connState == FlickrConnectionState.CONNECTION_IN_PROGRESS
        setEnabled(connectButton, !connecting)

        TODO("GPU: LLPanel::draw()")
    }

    private fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            LLFlickrEventPumps.obtain("FlickrConnectState").stopListening("LLFlickrAccountPanel")
            LLFlickrEventPumps.obtain("FlickrConnectState").listen("LLFlickrAccountPanel") { data ->
                onFlickrConnectStateChange(data)
            }
            LLFlickrEventPumps.obtain("FlickrConnectInfo").stopListening("LLFlickrAccountPanel")
            LLFlickrEventPumps.obtain("FlickrConnectInfo").listen("LLFlickrAccountPanel") { _ ->
                onFlickrConnectInfoChange()
            }

            if (LLFlickrConnect.instance().isConnected()) {
                showConnectedLayout()
            } else {
                showDisconnectedLayout()
            }

            val state = LLFlickrConnect.instance().getConnectionState()
            if (state == FlickrConnectionState.NOT_CONNECTED ||
                state == FlickrConnectionState.CONNECTION_FAILED) {
                LLFlickrConnect.instance().checkConnectionToFlickr()
            }
        } else {
            LLFlickrEventPumps.obtain("FlickrConnectState").stopListening("LLFlickrAccountPanel")
            LLFlickrEventPumps.obtain("FlickrConnectInfo").stopListening("LLFlickrAccountPanel")
        }
    }

    private fun onFlickrConnectStateChange(data: Map<String, Any?>): Boolean {
        if (LLFlickrConnect.instance().isConnected()) {
            val enumVal = (data["enum"] as? Int) ?: -1
            if (FlickrConnectionState.entries.getOrNull(enumVal) != FlickrConnectionState.DISCONNECTING) {
                showConnectedLayout()
            }
        } else {
            showDisconnectedLayout()
        }
        return false
    }

    private fun onFlickrConnectInfoChange(): Boolean {
        val info = LLFlickrConnect.instance().getInfo()
        val link = info["link"]?.toString() ?: ""
        val name = info["name"]?.toString() ?: ""
        val clickableName = if (link.isNotEmpty() && name.isNotEmpty()) "[$link $name]" else ""
        setText(accountNameLabel, clickableName)
        return false
    }

    private fun showConnectButton() {
        if (!getVisible(connectButton)) {
            setVisible(connectButton, true)
            setVisible(disconnectButton, false)
        }
    }

    private fun hideConnectButton() {
        if (getVisible(connectButton)) {
            setVisible(connectButton, false)
            setVisible(disconnectButton, true)
        }
    }

    private fun showDisconnectedLayout() {
        setText(accountCaptionLabel, getString("flickr_disconnected"))
        setText(accountNameLabel, "")
        showConnectButton()
    }

    private fun showConnectedLayout() {
        LLFlickrConnect.instance().loadFlickrInfo()
        setText(accountCaptionLabel, getString("flickr_connected"))
        hideConnectButton()
    }

    private fun onConnect() {
        LLFlickrConnect.instance().checkConnectionToFlickr(true)
    }

    private fun onDisconnect() {
        LLFlickrConnect.instance().disconnectFromFlickr()
    }
}

// ---------------------------------------------------------------------------
// LLFloaterFlickr
// ---------------------------------------------------------------------------

class LLFloaterFlickr(key: Any) {

    private var flickrPhotoPanel: LLFlickrPhotoPanel? = null
    private var statusErrorText: Any? = null
    private var statusLoadingText: Any? = null
    private var statusLoadingIndicator: Any? = null

    private fun <T> getChild(name: String): T = TODO("GPU: getChild<$name>")
    private fun setVisible(widget: Any?, visible: Boolean) = TODO("GPU: widget.setVisible($visible)")
    private fun setValue(widget: Any?, value: Any) = TODO("GPU: widget.setValue($value)")
    private fun closeFloater() = TODO("GPU: closeFloater()")

    fun postBuild(): Boolean {
        flickrPhotoPanel       = getChild("panel_flickr_photo")
        statusErrorText        = getChild<Any>("connection_error_text")
        statusLoadingText      = getChild<Any>("connection_loading_text")
        statusLoadingIndicator = getChild<Any>("connection_loading_indicator")

        // Remove the account tab since the Exodus upload path handles auth inline
        TODO("GPU: getChild<LLTabContainer>(\"tabs\").removeTabPanel(getChild<LLPanel>(\"panel_flickr_account\"))")

        return true
    }

    fun draw() {
        val errText    = statusErrorText
        val loadText   = statusLoadingText
        val loadInd    = statusLoadingIndicator
        if (errText != null && loadText != null && loadInd != null) {
            setVisible(errText,  false)
            setVisible(loadText, false)
            setVisible(loadInd,  false)

            when (LLFlickrConnect.instance().getConnectionState()) {
                FlickrConnectionState.NOT_CONNECTED,
                FlickrConnectionState.CONNECTED,
                FlickrConnectionState.POSTED -> {}

                FlickrConnectionState.CONNECTION_IN_PROGRESS -> {
                    setVisible(loadText, true)
                    setValue(loadText, LLTrans.getString("SocialFlickrConnecting"))
                    setVisible(loadInd, true)
                }
                FlickrConnectionState.POSTING -> {
                    setVisible(loadText, true)
                    setValue(loadText, LLTrans.getString("SocialFlickrPosting"))
                    setVisible(loadInd, true)
                }
                FlickrConnectionState.CONNECTION_FAILED -> {
                    setVisible(errText, true)
                    setValue(errText, LLTrans.getString("SocialFlickrErrorConnecting"))
                }
                FlickrConnectionState.POST_FAILED -> {
                    setVisible(errText, true)
                    setValue(errText, LLTrans.getString("SocialFlickrErrorPosting"))
                }
                FlickrConnectionState.DISCONNECTING -> {
                    setVisible(loadText, true)
                    setValue(loadText, LLTrans.getString("SocialFlickrDisconnecting"))
                    setVisible(loadInd, true)
                }
                FlickrConnectionState.DISCONNECT_FAILED -> {
                    setVisible(errText, true)
                    setValue(errText, LLTrans.getString("SocialFlickrErrorDisconnecting"))
                }
            }
        }
        TODO("GPU: LLFloater::draw()")
    }

    fun onClose(appQuitting: Boolean) {
        val bigPreview = LLFloaterRegStub.getInstance("big_preview")
        if (bigPreview != null) {
            TODO("GPU: bigPreview.closeOnFloaterOwnerClosing(this)")
        }
        TODO("GPU: LLFloater::onClose($appQuitting)")
    }

    fun onCancel() {
        val bigPreview = LLFloaterRegStub.getInstance("big_preview")
        if (bigPreview != null) {
            TODO("GPU: bigPreview.closeOnFloaterOwnerClosing(this)")
        }
        closeFloater()
    }

    fun showPhotoPanel() {
        val panel = flickrPhotoPanel ?: return
        TODO("GPU: parent LLTabContainer of flickrPhotoPanel -> selectTabPanel(panel)")
    }

    fun onOpen(key: Map<String, Any?>) {
        flickrPhotoPanel?.onOpen(key)
    }

    fun getPreviewView(): Any? = flickrPhotoPanel?.getPreviewView()

    companion object {
        fun update() {
            if (LLFloaterRegStub.instanceVisible("flickr")) {
                TODO("GPU: LLFloaterSnapshotBase::ImplBase::updatePreviewList(true, true)")
            }
        }
    }
}
