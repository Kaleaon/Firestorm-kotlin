package com.firestorm.newview

import com.firestorm.floater.Floater
import com.firestorm.ui.LLSD
import com.firestorm.viewer.ViewerControl
import com.firestorm.viewer.permissions.PERM_COPY
import com.firestorm.viewer.permissions.PERM_MODIFY
import com.firestorm.viewer.permissions.PERM_MOVE
import com.firestorm.viewer.permissions.PERM_NONE
import com.firestorm.viewer.permissions.PERM_TRANSFER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloaterPerms(seed: LLSD) : Floater(seed) {

    override fun postBuild(): Boolean = true

    companion object {
        fun getGroupPerms(prefix: String = ""): UInt =
            if (ViewerControl.savedSettings.getBool(prefix + "ShareWithGroup"))
                PERM_COPY or PERM_MOVE or PERM_MODIFY
            else
                PERM_NONE

        fun getEveryonePerms(prefix: String = ""): UInt =
            if (ViewerControl.savedSettings.getBool(prefix + "EveryoneCopy"))
                PERM_COPY
            else
                PERM_NONE

        fun getNextOwnerPerms(prefix: String = ""): UInt {
            var flags = PERM_MOVE
            if (ViewerControl.savedSettings.getBool(prefix + "NextOwnerCopy"))     flags = flags or PERM_COPY
            if (ViewerControl.savedSettings.getBool(prefix + "NextOwnerModify"))   flags = flags or PERM_MODIFY
            if (ViewerControl.savedSettings.getBool(prefix + "NextOwnerTransfer")) flags = flags or PERM_TRANSFER
            return flags
        }

        fun getNextOwnerPermsInverted(prefix: String = ""): UInt {
            var flags = PERM_MOVE
            if (!ViewerControl.savedSettings.getBool(prefix + "NextOwnerCopy"))     flags = flags or PERM_COPY
            if (!ViewerControl.savedSettings.getBool(prefix + "NextOwnerModify"))   flags = flags or PERM_MODIFY
            if (!ViewerControl.savedSettings.getBool(prefix + "NextOwnerTransfer")) flags = flags or PERM_TRANSFER
            return flags
        }
    }
}

class FloaterPermsDefault(seed: LLSD) : Floater(seed) {

    enum class Categories {
        OBJECTS, UPLOADS, SCRIPTS, NOTECARDS, GESTURES, WEARABLES, SETTINGS, MATERIALS;

        companion object {
            val count get() = values().size
        }
    }

    private val shareWithGroup  = BooleanArray(Categories.count)
    private val everyoneCopy    = BooleanArray(Categories.count)
    private val nextOwnerCopy   = BooleanArray(Categories.count)
    private val nextOwnerModify = BooleanArray(Categories.count)
    private val nextOwnerTransfer = BooleanArray(Categories.count)

    override fun postBuild(): Boolean {
        val settings = ViewerControl.savedSettings
        if (!settings.getBool("DefaultUploadPermissionsConverted")) {
            settings.setBool("UploadsEveryoneCopy",        settings.getBool("EveryoneCopy"))
            settings.setBool("UploadsNextOwnerCopy",       settings.getBool("NextOwnerCopy"))
            settings.setBool("UploadsNextOwnerModify",     settings.getBool("NextOwnerModify"))
            settings.setBool("UploadsNextOwnerTransfer",   settings.getBool("NextOwnerTransfer"))
            settings.setBool("UploadsShareWithGroup",      settings.getBool("ShareWithGroup"))
            settings.setBool("DefaultUploadPermissionsConverted", true)
        }

        onCloseSignal { cancel() }
        refresh()
        return true
    }

    fun onClickOk() {
        ok()
        closeFloater()
    }

    fun onClickCancel() {
        cancel()
        closeFloater()
    }

    fun onCommitCopy(userData: LLSD) {
        val prefix = userData.asString()
        val copyable = ViewerControl.savedSettings.getBool(prefix + "NextOwnerCopy")
        if (!copyable) {
            ViewerControl.savedSettings.setBool(prefix + "NextOwnerTransfer", true)
        }
        getChild<CheckBoxCtrl>(prefix + "_transfer").setEnabled(copyable)
    }

    fun ok() {
        refresh()
        updateCap()
    }

    fun cancel() {
        for (iter in Categories.values()) {
            val i = iter.ordinal
            val name = categoryNames[i]
            ViewerControl.savedSettings.setBool(name + "NextOwnerCopy",     nextOwnerCopy[i])
            ViewerControl.savedSettings.setBool(name + "NextOwnerModify",   nextOwnerModify[i])
            ViewerControl.savedSettings.setBool(name + "NextOwnerTransfer", nextOwnerTransfer[i])
            ViewerControl.savedSettings.setBool(name + "ShareWithGroup",    shareWithGroup[i])
            ViewerControl.savedSettings.setBool(name + "EveryoneCopy",      everyoneCopy[i])
        }
    }

    override fun refresh() {
        for (iter in Categories.values()) {
            val i = iter.ordinal
            val name = categoryNames[i]
            shareWithGroup[i]    = ViewerControl.savedSettings.getBool(name + "ShareWithGroup")
            everyoneCopy[i]      = ViewerControl.savedSettings.getBool(name + "EveryoneCopy")
            nextOwnerCopy[i]     = ViewerControl.savedSettings.getBool(name + "NextOwnerCopy")
            nextOwnerModify[i]   = ViewerControl.savedSettings.getBool(name + "NextOwnerModify")
            nextOwnerTransfer[i] = ViewerControl.savedSettings.getBool(name + "NextOwnerTransfer")
        }
    }

    companion object {
        private const val MAX_HTTP_RETRIES = 5
        private const val RETRY_TIMEOUT_MS = 5_000L

        var capSent: Boolean = false
            private set

        val categoryNames = arrayOf(
            "Objects", "Uploads", "Scripts", "Notecards",
            "Gestures", "Wearables", "Settings", "Materials"
        )

        fun sendInitialPerms() {
            if (!capSent) updateCap()
        }

        fun updateCap() {
            val region = Agent.region ?: run {
                // Region not set, cannot request capability update
                return
            }
            val url = region.getCapability("AgentPreferences")
            if (url.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch { updateCapCoro(url) }
            }
        }

        fun setCapSent(value: Boolean) { capSent = value }

        private suspend fun updateCapCoro(url: String) {
            TODO("APR: use JVM equivalent — POST AgentPreferences capability with default object perm masks")
        }
    }
}
