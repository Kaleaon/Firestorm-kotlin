package com.firestorm.newview

class LLFloaterAvatarWelcomePack(key: LLSD) : LLFloater(key) {

    private var mAvatarPicker: LLMediaCtrl? = null

    // Nullable lambda replaces boost::signals2::connection; non-null means connected.
    private var mAvatarPickerUrlChangedSignal: (() -> Unit)? = null

    override fun postBuild(): Boolean {
        mAvatarPicker = findChild<LLMediaCtrl>("avatar_picker_contents")
        mAvatarPicker?.let { picker ->
            picker.clearCache()
            picker.setErrorPageURL(gSavedSettings.getString("GenericErrorPageURL"))

            var avatarPickerUrl: String = run {
                if (LLGridManager.getInstance().isInOpenSim()) {
                    LLLoginInstance.getInstance()
                        .takeIf { it.hasResponse("avatar_picker_url") }
                        ?.getResponse("avatar_picker_url")?.asString() ?: ""
                } else {
                    gSavedSettings.getString("AvatarWelcomePack")
                }
            }

            if (avatarPickerUrl.isNotEmpty()) {
                avatarPickerUrl = LLWeb.expandURLSubstitutions(avatarPickerUrl, LLSD())
                picker.navigateTo(avatarPickerUrl, HTTP_CONTENT_TEXT_HTML)
            }
        }
        return true
    }

    override fun onOpen(key: LLSD) {
        // Connect during onOpen rather than the constructor: LFSimFeatureHandler may not
        // be safe to instantiate at construction time, and the initial URL is already
        // provided by the login response for the first region.
        if (mAvatarPickerUrlChangedSignal == null) {
            mAvatarPickerUrlChangedSignal = LFSimFeatureHandler.instance()
                .setAvatarPickerCallback { url -> handleUrlChanged(url) }
        }
    }

    override fun onClose(appQuitting: Boolean) {
        mAvatarPicker?.let { picker ->
            picker.navigateStop()
            picker.clearCache()
            picker.unloadMediaSource()
        }
        mAvatarPickerUrlChangedSignal?.let {
            LFSimFeatureHandler.instance().removeAvatarPickerCallback(it)
            mAvatarPickerUrlChangedSignal = null
        }
        super.onClose(appQuitting)
    }

    private fun handleUrlChanged(url: String) {
        getChild<LLMediaCtrl>("avatar_picker_contents")
            .navigateTo(LLWeb.expandURLSubstitutions(url, LLSD()), HTTP_CONTENT_TEXT_HTML)
    }
}
