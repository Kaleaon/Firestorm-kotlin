package com.firestorm.newview

import java.util.UUID

object FloaterDisplayNameUtil {
    fun registerFloater() {
        FloaterReg.add("display_name", "floater_display_name.xml") {
            key -> FloaterDisplayName(key)
        }
    }
}

private class FloaterDisplayName(key: LLSD) : Floater(key) {

    private var isLockedOut: Boolean = false

    override fun postBuild(): Boolean {
        getChild<UICtrl>("reset_btn")?.setCommitCallback { onReset() }
        getChild<UICtrl>("cancel_btn")?.setCommitCallback { onCancel() }
        getChild<UICtrl>("save_btn")?.setCommitCallback { onSave() }
        center()
        return true
    }

    override fun onOpen(key: LLSD) {
        getChild<UICtrl>("display_name_editor")?.clear()
        getChild<UICtrl>("display_name_confirm")?.clear()

        val avName = AvatarNameCache.get(Agent.instance.getID()) ?: return

        val nowSecs = LLDate.now().secondsSinceEpoch()
        isLockedOut = nowSecs < avName.nextUpdate
        if (isLockedOut) {
            var nextUpdateString = getString("LockOutDateFormat")
            nextUpdateString = StringUtil.format(nextUpdateString, LLSD().with("datetime", avName.nextUpdate.toInt()))
            getChild<UICtrl>("lockout_text")?.setTextArg("[TIME]", nextUpdateString)
            getChild<UICtrl>("lockout_text")?.setVisible(true)
            getChild<UICtrl>("save_btn")?.setEnabled(false)
            getChild<UICtrl>("display_name_editor")?.setEnabled(false)
            getChild<UICtrl>("display_name_confirm")?.setEnabled(false)
            getChild<UICtrl>("cancel_btn")?.setFocus(true)
        } else {
            getChild<UICtrl>("lockout_text")?.setVisible(false)
            getChild<UICtrl>("save_btn")?.setEnabled(true)
            getChild<UICtrl>("display_name_editor")?.setEnabled(true)
            getChild<UICtrl>("display_name_confirm")?.setEnabled(true)
        }
    }

    private fun onCancel() {
        setVisible(false)
    }

    private fun onReset() {
        if (AvatarNameCache.instance.hasNameLookupURL()) {
            ViewerDisplayName.set("") { success, reason, content ->
                onCacheSetName(success, reason, content)
            }
        } else {
            NotificationsUtil.add("SetDisplayNameFailedGeneric")
        }
        setVisible(false)
    }

    private fun onSave() {
        val displayNameUtf8 = getChild<UICtrl>("display_name_editor")?.getValue()?.toString() ?: return
        val displayNameConfirm = getChild<UICtrl>("display_name_confirm")?.getValue()?.toString() ?: return

        if (displayNameUtf8 != displayNameConfirm) {
            NotificationsUtil.add("SetDisplayNameMismatch")
            return
        }

        val avName = AvatarNameCache.get(Agent.instance.getID()) ?: return
        val userName = avName.getUserName()

        if (displayNameUtf8 == userName && AvatarNameCache.instance.hasNameLookupURL()) {
            ViewerDisplayName.set("") { success, reason, content ->
                onCacheSetName(success, reason, content)
            }
            return
        }

        val displayNameMaxLength = 31
        if (displayNameUtf8.length > displayNameMaxLength) {
            NotificationsUtil.add("SetDisplayNameFailedLength", LLSD().with("LENGTH", displayNameMaxLength.toString()))
            return
        }

        if (AvatarNameCache.instance.hasNameLookupURL()) {
            ViewerDisplayName.set(displayNameUtf8) { success, reason, content ->
                onCacheSetName(success, reason, content)
            }
        } else {
            NotificationsUtil.add("SetDisplayNameFailedGeneric")
        }

        setVisible(false)
    }

    private fun onCacheSetName(success: Boolean, reason: String, content: LLSD) {
        if (success) {
            NotificationsUtil.add("SetDisplayNameSuccess", LLSD().with("DISPLAY_NAME", content["display_name"]))
            return
        }

        val errorTag = content["error_tag"].asString()
        if (errorTag.isNotEmpty() && Notifications.instance.templateExists(errorTag)) {
            NotificationsUtil.add(errorTag)
            return
        }

        val langCode = LLUI.getLanguage()
        val errorDesc = content["error_description"]
        if (errorDesc.has(langCode)) {
            NotificationsUtil.add("GenericAlert", LLSD().with("MESSAGE", errorDesc[langCode].asString()))
            return
        }

        NotificationsUtil.add("SetDisplayNameFailedGeneric")
    }
}
