package com.firestorm.newview

class FSPrimfeedPhotoPanel {

    private var previewHandle: Any? = null

    private var resolutionComboBox: Any? = null
    private var filterComboBox: Any? = null
    private var refreshBtn: Any? = null
    private var workingLabel: Any? = null
    private var thumbnailPlaceholder: Any? = null
    private var descriptionTextBox: Any? = null
    private var locationCheckbox: Any? = null
    private var commercialCheckbox: Any? = null
    private var publicGalleryCheckbox: Any? = null
    private var ratingComboBox: Any? = null
    private var postButton: Any? = null
    private var cancelButton: Any? = null
    private var btnPreview: Any? = null
    private var storesComboBox: Any? = null
    private var customSnapshotWidth: Any? = null
    private var customSnapshotHeight: Any? = null
    private var keepAspectRatioCbx: Any? = null
    private var bigPreviewFloater: Any? = null

    init {
        TODO("APR: use JVM equivalent: register SocialSharing.SendPhoto -> onSend(), SocialSharing.RefreshPhoto -> onClickNewSnapshot(), SocialSharing.BigPreview -> onClickBigPreview() commit callbacks")
        TODO("APR: use JVM equivalent: register Primfeed.Info commit callback that opens url externally")
        TODO("APR: use JVM equivalent: listen on FSPrimfeedAuth.sPrimfeedAuthPump for primfeed_user_info responses; call loadPrimfeedInfo(data) when matched")
    }

    fun destroy() {
        TODO("APR: use JVM equivalent: if previewHandle is non-null, call die() on the preview")
        TODO("APR: use JVM equivalent: save FSLastSnapshotToPrimfeedResolution, FSLastSnapshotToPrimfeedWidth, FSLastSnapshotToPrimfeedHeight settings")
    }

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find all child UI controls by name and assign to fields")
        TODO("APR: use JVM equivalent: wire commit callbacks for resolutionComboBox, filterComboBox, customSnapshotWidth, customSnapshotHeight, keepAspectRatioCbx to updateResolution(true)")
        TODO("APR: use JVM equivalent: restore resolution/width/height from saved settings")
        TODO("APR: use JVM equivalent: populate filterComboBox from LLImageFiltersManager.getInstance().getFiltersList()")
        TODO("APR: use JVM equivalent: call LLPanel.postBuild()")
        return true
    }

    fun notify(info: Map<String, Any?>): Int {
        if (info.containsKey("snapshot-updating")) {
            return 1
        }
        if (info.containsKey("snapshot-updated")) {
            updateControls()
            TODO("APR: use JVM equivalent: make refreshBtn visible if it was hidden")
            return 1
        }
        return 0
    }

    fun draw() {
        TODO("APR: use JVM equivalent: cast previewHandle to LLSnapshotLivePreview")
        TODO("APR: use JVM equivalent: compute canPost = !FSPrimfeedConnect.isTransactionOngoing() && FSPrimfeedAuth.isAuthorized()")
        TODO("APR: use JVM equivalent: set enabled state on cancelButton, descriptionTextBox, ratingComboBox, storesComboBox, resolutionComboBox, filterComboBox, refreshBtn, btnPreview, locationCheckbox, publicGalleryCheckbox, commercialCheckbox based on canPost")
        TODO("APR: use JVM equivalent: if hasFocus() && isPreviewVisible(), call attachPreview()")
        TODO("APR: use JVM equivalent: set btnPreview toggle state based on preview active state")
        if (previewHandle != null) {
            TODO("GPU: gl_draw_scaled_image with thumbnail image, offset calculated from thumbnailPlaceholder rect, applying floater transparency alpha")
        }
        TODO("APR: use JVM equivalent: set workingLabel visibility based on whether snapshot is up to date")
        TODO("APR: use JVM equivalent: set postButton enabled based on canPost and snapshot up-to-date")
        TODO("APR: use JVM equivalent: call LLPanel.draw()")
    }

    fun getPreviewView(): Any? {
        return previewHandle
    }

    fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            if (previewHandle != null) {
                TODO("APR: use JVM equivalent: call preview.updateSnapshot(true)")
            } else {
                TODO("APR: use JVM equivalent: create LLSnapshotLivePreview with full-screen rect, configure snapshot type/format/flags, store handle, call updateControls()")
            }
        }
    }

    fun onClickNewSnapshot() {
        TODO("APR: use JVM equivalent: call getPreviewView().updateSnapshot(true)")
    }

    fun onClickBigPreview() {
        if (isPreviewVisible()) {
            TODO("APR: use JVM equivalent: hide big_preview floater instance")
        } else {
            attachPreview()
            TODO("APR: use JVM equivalent: show big_preview floater instance")
        }
    }

    fun onSend() {
        sendPhoto()
    }

    fun onPrimfeedConnectStateChange(data: Map<String, Any?>): Boolean {
        TODO("APR: use JVM equivalent: return FSPrimfeedAuth.isAuthorized()")
    }

    fun sendPhoto() {
        val description = TODO("APR: use JVM equivalent: read value from descriptionTextBox") as String
        val contentRating = TODO("APR: use JVM equivalent: read integer value from ratingComboBox") as Int
        val postToPublicGallery = TODO("APR: use JVM equivalent: read boolean from publicGalleryCheckbox") as Boolean
        val commercialContent = TODO("APR: use JVM equivalent: read boolean from commercialCheckbox") as Boolean
        val storeId = TODO("APR: use JVM equivalent: read string value from storesComboBox") as String

        val ratingString = when (contentRating.coerceIn(1, 4)) {
            1 -> "general"
            2 -> "moderate"
            3 -> "adult"
            else -> "adult_plus"
        }

        val params: MutableMap<String, Any?> = mutableMapOf(
            "rating" to ratingString,
            "content" to description,
            "is_commercial" to commercialContent,
            "post_to_public_gallery" to postToPublicGallery
        )

        val addLocation = TODO("APR: use JVM equivalent: read boolean from locationCheckbox") as Boolean
        if (addLocation) {
            TODO("APR: use JVM equivalent: build SLURL from LLAgentUI.buildSLURL and add 'location' key to params")
        }
        if (storeId.isNotEmpty()) {
            params["store_id"] = storeId
        }

        TODO("APR: use JVM equivalent: FSPrimfeedConnect.instance.setConnectionState(PRIMFEED_POSTING)")
        TODO("APR: use JVM equivalent: FSPrimfeedConnect.instance.uploadPhoto(params, previewp.getFormattedImage()) { success, url -> handle result, open url if FSPrimfeedOpenURLOnPost, show notification }")
        updateControls()
    }

    fun clearAndClose() {
        TODO("APR: use JVM equivalent: clear descriptionTextBox value")
        TODO("APR: use JVM equivalent: close parent floater and call bigPreviewFloater.closeOnFloaterOwnerClosing if present")
    }

    fun updateControls() {
        updateResolution(false)
    }

    fun updateResolution(doUpdate: Boolean) {
        TODO("APR: use JVM equivalent: parse resolution from resolutionComboBox selected value as LLSD [width, height]")
        TODO("APR: use JVM equivalent: get filter name from filterComboBox (empty string if index 0)")
        TODO("APR: use JVM equivalent: call checkAspectRatio(width) on preview; compare old/new sizes; call preview.setSize and optionally updateSnapshot(true,true)")
        TODO("APR: use JVM equivalent: compare old/new filter; call preview.setFilter if changed and optionally updateSnapshot(false,true)")
        TODO("APR: use JVM equivalent: enable/disable customSnapshotWidth, customSnapshotHeight, keepAspectRatioCbx based on custom resolution selection")
    }

    fun checkAspectRatio(index: Int) {
        val keepAspect = when (index) {
            0 -> true
            -1 -> TODO("APR: use JVM equivalent: read keepAspectRatioCbx checked state") as Boolean
            else -> false
        }
        TODO("APR: use JVM equivalent: set preview.mKeepAspectRatio = keepAspect")
    }

    fun loadPrimfeedInfo(data: Map<String, Any?>) {
        TODO("APR: use JVM equivalent: clear storesComboBox and add 'Personal' entry")
        val stores = data["stores"] as? List<*>
        if (stores.isNullOrEmpty()) {
            TODO("APR: use JVM equivalent: disable storesComboBox")
            return
        }
        TODO("APR: use JVM equivalent: enable storesComboBox and populate with store name/id entries from stores list")
        TODO("APR: use JVM equivalent: select index 0 in storesComboBox")
    }

    fun getRefreshBtn(): Any? = refreshBtn

    fun onOpen(key: Map<String, Any?>) {
        TODO("APR: use JVM equivalent: FSPrimfeedAuth.initiateAuthRequest()")
        onPrimfeedConnectStateChange(emptyMap())
    }

    fun uploadCallback(success: Boolean, response: Map<String, Any?>) {
        if (success && response["stat"] == "ok") {
            TODO("APR: use JVM equivalent: FSPrimfeedConnect.instance.setConnectionState(PRIMFEED_POSTED)")
            TODO("APR: use JVM equivalent: show FSPrimfeedUploadComplete notification with postUrl")
        } else {
            TODO("APR: use JVM equivalent: FSPrimfeedConnect.instance.setConnectionState(PRIMFEED_POST_FAILED)")
        }
    }

    fun primfeedAuthResponse(success: Boolean, response: Map<String, Any?>) {
        onPrimfeedConnectStateChange(response)
    }

    private fun isPreviewVisible(): Boolean {
        TODO("APR: use JVM equivalent: return bigPreviewFloater != null && bigPreviewFloater.isVisible()")
    }

    private fun attachPreview() {
        TODO("APR: use JVM equivalent: set preview on bigPreviewFloater and set floater owner to parent floater")
    }

    private fun checkImageSize(
        previewp: Any?,
        width: Int,
        height: Int,
        isWidthChanged: Boolean,
        maxValue: Int
    ): Triple<Boolean, Int, Int> {
        var w = width
        var h = height
        TODO("APR: use JVM equivalent: if previewp.mKeepAspectRatio, compute aspect ratio from window size and adjust width or height proportionally, clamping to maxValue")
        return Triple(w != width || h != height, w, h)
    }
}

class FSPrimfeedAccountPanel {

    private var accountConnectedAsLabel: Any? = null
    private var accountNameLink: Any? = null
    private var accountPlan: Any? = null
    private var connectButton: Any? = null
    private var disconnectButton: Any? = null

    init {
        TODO("APR: use JVM equivalent: register SocialSharing.Connect -> onConnect() and SocialSharing.Disconnect -> onDisconnect() commit callbacks")
        TODO("APR: use JVM equivalent: listen on FSPrimfeedAuth.sPrimfeedAuthPump for primfeed_auth_response and primfeed_auth_reset events; call primfeedAuthResponse(success, data)")
        TODO("APR: use JVM equivalent: register visibility-change callback to onVisibilityChange")
    }

    fun destroy() {
        TODO("APR: use JVM equivalent: stop listening on FSPrimfeedAuth.sPrimfeedAuthPump for FSPrimfeedAccountPanel")
    }

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find child UI controls by name and assign to fields")
        onPrimfeedConnectStateChange(emptyMap<String, Any?>())
        TODO("APR: use JVM equivalent: call LLPanel.postBuild()")
        return true
    }

    fun draw() {
        TODO("APR: use JVM equivalent: get FSPrimfeedConnect.instance.connectionState; if changed since last frame, call onPrimfeedConnectStateChange()")
        TODO("APR: use JVM equivalent: call LLPanel.draw()")
    }

    fun onVisibilityChange(visible: Boolean) {
        if (visible) {
            if (TODO("APR: use JVM equivalent: FSPrimfeedAuth.isAuthorized()") as Boolean) {
                showConnectedLayout()
            } else {
                showDisconnectedLayout()
            }
        }
    }

    fun onPrimfeedConnectStateChange(data: Map<String, Any?>): Boolean {
        val isAuthorized = TODO("APR: use JVM equivalent: FSPrimfeedAuth.isAuthorized()") as Boolean
        val isConnecting = TODO("APR: use JVM equivalent: FSPrimfeedConnect.instance.connectionState == PRIMFEED_CONNECTING") as Boolean
        if (isAuthorized || isConnecting) {
            showConnectedLayout()
        } else {
            showDisconnectedLayout()
        }
        onPrimfeedConnectInfoChange()
        return false
    }

    fun onPrimfeedConnectInfoChange(): Boolean {
        TODO("APR: use JVM equivalent: read FSPrimfeedUsername, FSPrimfeedProfileLink, FSPrimfeedPlan from saved per-account settings")
        TODO("APR: use JVM equivalent: build clickable name string as '[profileLink username]' and set on accountNameLink; set accountPlan text")
        return false
    }

    private fun showConnectButton() {
        TODO("APR: use JVM equivalent: if connectButton not visible, set connectButton visible and disconnectButton not visible")
    }

    private fun hideConnectButton() {
        TODO("APR: use JVM equivalent: if connectButton visible, set connectButton not visible and disconnectButton visible")
    }

    private fun showDisconnectedLayout() {
        TODO("APR: use JVM equivalent: set accountConnectedAsLabel to primfeed_disconnected string, clear accountNameLink, set accountPlan to primfeed_plan_unknown string")
        showConnectButton()
    }

    private fun showConnectedLayout() {
        TODO("APR: use JVM equivalent: set accountConnectedAsLabel to primfeed_connected string")
        hideConnectButton()
    }

    private fun onConnect() {
        TODO("APR: use JVM equivalent: FSPrimfeedAuth.initiateAuthRequest()")
        onPrimfeedConnectStateChange(emptyMap())
    }

    private fun onDisconnect() {
        TODO("APR: use JVM equivalent: FSPrimfeedAuth.resetAuthStatus()")
        onPrimfeedConnectStateChange(emptyMap())
    }

    private fun primfeedAuthResponse(success: Boolean, response: Map<String, Any?>) {
        if (!success) {
            TODO("APR: use JVM equivalent: open https://www.primfeed.com/login externally")
        }
        onPrimfeedConnectStateChange(response)
    }
}

class FSFloaterPrimfeed(key: Map<String, Any?>) {

    private var primfeedPhotoPanel: FSPrimfeedPhotoPanel? = null
    private var primfeedAccountPanel: FSPrimfeedAccountPanel? = null
    private var statusErrorText: Any? = null
    private var statusLoadingText: Any? = null
    private var statusLoadingIndicator: Any? = null

    init {
        TODO("APR: use JVM equivalent: register SocialSharing.Cancel -> onCancel() commit callback")
    }

    fun onClose(appQuitting: Boolean) {
        TODO("APR: use JVM equivalent: close big_preview floater on owner closing if it exists")
        TODO("APR: use JVM equivalent: call LLFloater.onClose(appQuitting)")
    }

    fun onCancel() {
        TODO("APR: use JVM equivalent: close big_preview floater on owner closing if it exists")
        TODO("APR: use JVM equivalent: closeFloater()")
    }

    fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent: find panel_primfeed_photo and panel_primfeed_account child panels")
        TODO("APR: use JVM equivalent: find connection_error_text, connection_loading_text, connection_loading_indicator children")
        TODO("APR: use JVM equivalent: call LLFloater.postBuild()")
        return true
    }

    fun showPhotoPanel() {
        TODO("APR: use JVM equivalent: get parent tab container of primfeedPhotoPanel and select that tab")
    }

    fun draw() {
        if (statusErrorText != null && statusLoadingText != null && statusLoadingIndicator != null) {
            TODO("APR: use JVM equivalent: hide all three status widgets initially")
            val isAuthorized = TODO("APR: use JVM equivalent: FSPrimfeedAuth.isAuthorized()") as Boolean
            val isPendingAuth = TODO("APR: use JVM equivalent: FSPrimfeedAuth.isPendingAuth()") as Boolean
            val connectionState = TODO("APR: use JVM equivalent: FSPrimfeedConnect.instance.connectionState") as Int

            if (isAuthorized) {
                when (connectionState) {
                    TODO("APR: PRIMFEED_POSTING state value") as Int -> {
                        TODO("APR: use JVM equivalent: show statusLoadingText with SocialPrimfeedPosting string and show statusLoadingIndicator")
                    }
                    TODO("APR: PRIMFEED_POST_FAILED state value") as Int -> {
                        TODO("APR: use JVM equivalent: show statusErrorText with SocialPrimfeedErrorPosting string")
                    }
                    else -> {}
                }
            } else if (isPendingAuth) {
                TODO("APR: use JVM equivalent: show statusLoadingText with SocialPrimfeedConnecting string")
            } else {
                TODO("APR: use JVM equivalent: show statusErrorText with SocialPrimfeedNotAuthorized string")
            }
        }
        TODO("APR: use JVM equivalent: call LLFloater.draw()")
    }

    fun onOpen(key: Map<String, Any?>) {
        primfeedPhotoPanel?.onOpen(key)
    }

    fun getPreviewView(): Any? {
        return primfeedPhotoPanel?.getPreviewView()
    }

    companion object {
        fun update() {
            TODO("APR: use JVM equivalent: if primfeed floater instance is visible, call LLFloaterSnapshotBase.ImplBase.updatePreviewList(true, true)")
        }
    }
}
