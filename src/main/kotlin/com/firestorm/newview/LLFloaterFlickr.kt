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
        System.err.println("LLFlickrConnect: checkConnectionToFlickr not yet implemented")
    }
    fun disconnectFromFlickr() {
        System.err.println("LLFlickrConnect: disconnectFromFlickr not yet implemented")
    }
    fun loadFlickrInfo() {
        System.err.println("LLFlickrConnect: loadFlickrInfo not yet implemented")
    }
    fun getInfo(): Map<String, Any?> {
        System.err.println("LLFlickrConnect: getInfo not yet implemented")
        return emptyMap()
    }
}

private object ExoFlickr {
    fun uploadPhoto(
        params: Map<String, Any?>,
        image: Any?,
        cb: (Boolean, Map<String, Any?>) -> Unit
    ) {
        System.err.println("ExoFlickr: uploadPhoto not yet implemented")
    }
}

private class ExoFlickrAuth(cb: (Boolean, Map<String, Any?>) -> Unit) {
    init {
        System.err.println("ExoFlickrAuth: init not yet implemented")
    }
}

private object LLImageFiltersManager {
    fun getInstance(): LLImageFiltersManager = this
    fun getFiltersList(): List<String> {
        System.err.println("LLImageFiltersManager: getFiltersList not yet implemented")
        return emptyList()
    }
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
    fun isInSecondLife(): Boolean {
        System.err.println("LLGridManager: isInSecondLife not yet implemented")
        return false
    }
    fun getGridId(): String {
        System.err.println("LLGridManager: getGridId not yet implemented")
        return ""
    }
    fun getGridLabel(): String {
        System.err.println("LLGridManager: getGridLabel not yet implemented")
        return ""
    }
}

private object LLAgentUI {
    fun buildSLURL(slurl: SLURLStub): Boolean {
        System.err.println("LLAgentUI: buildSLURL not yet implemented")
        return false
    }
}

private class SLURLStub {
    fun getSLURLString(): String {
        System.err.println("SLURLStub: getSLURLString not yet implemented")
        return ""
    }
}

private object LLViewerParcelMgr {
    fun getInstance(): LLViewerParcelMgr = this
    fun getAgentParcelName(): String {
        System.err.println("LLViewerParcelMgr: getAgentParcelName not yet implemented")
        return ""
    }
    fun getAgentParcel(): ParcelStub? {
        System.err.println("LLViewerParcelMgr: getAgentParcel not yet implemented")
        return null
    }
}

private class ParcelStub

private object LLViewerRegionAgent {
    fun getRegion(): RegionAgentStub? {
        System.err.println("LLViewerRegionAgent: getRegion not yet implemented")
        return null
    }
    fun getPositionAgent(): Triple<Float, Float, Float> {
        System.err.println("LLViewerRegionAgent: getPositionAgent not yet implemented")
        return Triple(0f, 0f, 0f)
    }
}

private class RegionAgentStub {
    fun getName(): String {
        System.err.println("RegionAgentStub: getName not yet implemented")
        return ""
    }
}

private object LLFloaterRegStub {
    fun getInstance(name: String): Any? {
        System.err.println("LLFloaterRegStub: getInstance not yet implemented")
        return null
    }
    fun instanceVisible(name: String): Boolean {
        System.err.println("LLFloaterRegStub: instanceVisible not yet implemented")
        return false
    }
    fun hideInstance(name: String) {
        System.err.println("LLFloaterRegStub: hideInstance not yet implemented")
    }
    fun showInstance(name: String) {
        System.err.println("LLFloaterRegStub: showInstance not yet implemented")
    }
}

private object LLNotificationsUtilFlickr {
    fun add(name: String, args: Map<String, Any?> = emptyMap()) {
        System.err.println("LLNotificationsUtilFlickr: add not yet implemented")
    }
}

private object LLTrans {
    fun getString(key: String): String {
        System.err.println("LLTrans: getString not yet implemented")
        return ""
    }
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

    private fun <T> getChild(name: String): T {
        System.err.println("LLFlickrPhotoPanel: getChild not yet implemented")
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
    private fun <T> findChild(name: String): T? {
        System.err.println("LLFlickrPhotoPanel: findChild not yet implemented")
        return null
    }
    private fun setVisible(visible: Boolean) {}
    private fun hasFocus(): Boolean {
        System.err.println("LLFlickrPhotoPanel: hasFocus not yet implemented")
        return false
    }
    private fun getParentByType(): Any? {
        System.err.println("LLFlickrPhotoPanel: getParentByType not yet implemented")
        return null
    }
    private fun getRootViewRect(): Any {
        System.err.println("LLFlickrPhotoPanel: getRootViewRect not yet implemented")
        return Any()
    }
    private fun setEnabled(widget: Any?, enabled: Boolean) {}
    private fun getVisible(widget: Any?): Boolean {
        System.err.println("LLFlickrPhotoPanel: getVisible not yet implemented")
        return false
    }
    private fun setValue(widget: Any?, value: Any) {}
    private fun getValue(widget: Any?): Any {
        System.err.println("LLFlickrPhotoPanel: getValue not yet implemented")
        return Any()
    }
    private fun setVisibleCallback(cb: (Boolean) -> Unit) {}
    private fun getPreviewSnapshotUpToDate(preview: Any?): Boolean {
        System.err.println("LLFlickrPhotoPanel: getPreviewSnapshotUpToDate not yet implemented")
        return false
    }
    private fun updateSnapshotPreview(preview: Any?, updateDims: Boolean, updateFilter: Boolean = false) {}

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

        // GPU: wire resolution/filter/custom-size commit callbacks; restore saved resolution/dimensions
        // GPU: populate filterComboBox with filterList
        val filterList = LLImageFiltersManager.getInstance().getFiltersList()

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

        // GPU: toggle btnPreview toggle state; draw thumbnail if previewp->getThumbnailImage() available;
        //      update workingLabel; enable/disable postButton
        // GPU: LLPanel::draw()
    }

    fun getPreviewView(): Any? = previewHandle

    fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            val existingPreview = previewHandle
            if (existingPreview != null) {
                // GPU: previewp->updateSnapshot(true) on re-show
            } else {
                // GPU: create LLSnapshotLivePreview with full-screen rect, configure it, assign handle, call updateControls()
            }
        }
    }

    fun onClickNewSnapshot() {
        // GPU: previewp->updateSnapshot(true)
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
            // GPU: floater.closeFloater(); bigPreviewFloater?.closeOnFloaterOwnerClosing(floater)
        }
    }

    fun updateControls() {
        updateResolution(false)
    }

    fun updateResolution(doUpdate: Boolean) {
        // GPU: read resolution/filter combos; set preview size; conditionally call updateSnapshot; toggle custom-size spinners
    }

    fun checkAspectRatio(index: Int) {
        val keepAspect = when (index) {
            0    -> true         // current window size
            -1   -> false
            else -> false
        }
        // GPU: previewp.mKeepAspectRatio = keepAspect
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

    private fun isPreviewVisible(): Boolean {
        System.err.println("LLFlickrPhotoPanel: isPreviewVisible not yet implemented")
        return false
    }

    private fun attachPreview() {
        // GPU: bigPreviewFloater?.setPreview(previewp); bigPreviewFloater?.setFloaterOwner(parentFloater)
    }

    private fun checkImageSize(
        previewp: Any?,
        width: IntArray,
        height: IntArray,
        isWidthChanged: Boolean,
        maxValue: Int
    ): Boolean {
        System.err.println("LLFlickrPhotoPanel: checkImageSize not yet implemented")
        return false
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

    private fun <T> getChild(name: String): T {
        System.err.println("LLFlickrAccountPanel: getChild not yet implemented")
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
    private fun setVisible(widget: Any?, visible: Boolean) {}
    private fun setEnabled(widget: Any?, enabled: Boolean) {}
    private fun getVisible(widget: Any?): Boolean {
        System.err.println("LLFlickrAccountPanel: getVisible not yet implemented")
        return false
    }
    private fun setText(widget: Any?, text: String) {}
    private fun getString(key: String): String {
        System.err.println("LLFlickrAccountPanel: getString not yet implemented")
        return ""
    }
    private fun setVisibleCallback(cb: (Boolean) -> Unit) {}

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

        // GPU: LLPanel::draw()
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

    private fun <T> getChild(name: String): T {
        System.err.println("LLFloaterFlickr: getChild not yet implemented")
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
    private fun setVisible(widget: Any?, visible: Boolean) {}
    private fun setValue(widget: Any?, value: Any) {}
    private fun closeFloater() {}

    fun postBuild(): Boolean {
        flickrPhotoPanel       = getChild("panel_flickr_photo")
        statusErrorText        = getChild<Any>("connection_error_text")
        statusLoadingText      = getChild<Any>("connection_loading_text")
        statusLoadingIndicator = getChild<Any>("connection_loading_indicator")

        // GPU: getChild<LLTabContainer>("tabs").removeTabPanel(getChild<LLPanel>("panel_flickr_account"))

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
        // GPU: LLFloater::draw()
    }

    fun onClose(appQuitting: Boolean) {
        val bigPreview = LLFloaterRegStub.getInstance("big_preview")
        if (bigPreview != null) {
            // GPU: bigPreview.closeOnFloaterOwnerClosing(this)
        }
        // GPU: LLFloater::onClose(appQuitting)
    }

    fun onCancel() {
        val bigPreview = LLFloaterRegStub.getInstance("big_preview")
        if (bigPreview != null) {
            // GPU: bigPreview.closeOnFloaterOwnerClosing(this)
        }
        closeFloater()
    }

    fun showPhotoPanel() {
        val panel = flickrPhotoPanel ?: return
        // GPU: parent LLTabContainer of flickrPhotoPanel -> selectTabPanel(panel)
    }

    fun onOpen(key: Map<String, Any?>) {
        flickrPhotoPanel?.onOpen(key)
    }

    fun getPreviewView(): Any? = flickrPhotoPanel?.getPreviewView()

    companion object {
        fun update() {
            if (LLFloaterRegStub.instanceVisible("flickr")) {
                // GPU: LLFloaterSnapshotBase::ImplBase::updatePreviewList(true, true)
            }
        }
    }
}
