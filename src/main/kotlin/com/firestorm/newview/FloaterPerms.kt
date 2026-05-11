package com.firestorm.newview

import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.Floater

const val PERM_NONE:     UInt = 0x00000000u
const val PERM_TRANSFER: UInt = 1u shl 13   // 0x00002000
const val PERM_MODIFY:   UInt = 1u shl 14   // 0x00004000
const val PERM_COPY:     UInt = 1u shl 15   // 0x00008000
const val PERM_MOVE:     UInt = 1u shl 19   // 0x00080000

class FloaterPerms(seed: Any?) : Floater(seed) {

    override fun postBuild(): Boolean = true

    companion object {
        fun getGroupPerms(prefix: String = ""): UInt =
            if (gSavedSettings.getBool(prefix + "ShareWithGroup"))
                PERM_COPY or PERM_MOVE or PERM_MODIFY
            else
                PERM_NONE

        fun getEveryonePerms(prefix: String = ""): UInt =
            if (gSavedSettings.getBool(prefix + "EveryoneCopy"))
                PERM_COPY
            else
                PERM_NONE

        fun getNextOwnerPerms(prefix: String = ""): UInt {
            var flags = PERM_MOVE
            if (gSavedSettings.getBool(prefix + "NextOwnerCopy"))     flags = flags or PERM_COPY
            if (gSavedSettings.getBool(prefix + "NextOwnerModify"))   flags = flags or PERM_MODIFY
            if (gSavedSettings.getBool(prefix + "NextOwnerTransfer")) flags = flags or PERM_TRANSFER
            return flags
        }

        fun getNextOwnerPermsInverted(prefix: String = ""): UInt {
            var flags = PERM_MOVE
            if (!gSavedSettings.getBool(prefix + "NextOwnerCopy"))     flags = flags or PERM_COPY
            if (!gSavedSettings.getBool(prefix + "NextOwnerModify"))   flags = flags or PERM_MODIFY
            if (!gSavedSettings.getBool(prefix + "NextOwnerTransfer")) flags = flags or PERM_TRANSFER
            return flags
        }
    }
}

class FloaterPermsDefault(seed: Any?) : Floater(seed) {

    enum class Categories {
        OBJECTS, UPLOADS, SCRIPTS, NOTECARDS, GESTURES, WEARABLES, SETTINGS, MATERIALS;

        companion object {
            val count get() = entries.size
        }
    }

    private val shareWithGroup    = BooleanArray(Categories.count)
    private val everyoneCopy      = BooleanArray(Categories.count)
    private val nextOwnerCopy     = BooleanArray(Categories.count)
    private val nextOwnerModify   = BooleanArray(Categories.count)
    private val nextOwnerTransfer = BooleanArray(Categories.count)

    override fun postBuild(): Boolean {
        if (!gSavedSettings.getBool("DefaultUploadPermissionsConverted")) {
            gSavedSettings.setBool("UploadsEveryoneCopy",      gSavedSettings.getBool("EveryoneCopy"))
            gSavedSettings.setBool("UploadsNextOwnerCopy",     gSavedSettings.getBool("NextOwnerCopy"))
            gSavedSettings.setBool("UploadsNextOwnerModify",   gSavedSettings.getBool("NextOwnerModify"))
            gSavedSettings.setBool("UploadsNextOwnerTransfer", gSavedSettings.getBool("NextOwnerTransfer"))
            gSavedSettings.setBool("UploadsShareWithGroup",    gSavedSettings.getBool("ShareWithGroup"))
            gSavedSettings.setBool("DefaultUploadPermissionsConverted", true)
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

    fun onCommitCopy(userData: Any?) {
        val prefix = (userData as? String) ?: return
        val copyable = gSavedSettings.getBool(prefix + "NextOwnerCopy")
        if (!copyable) {
            gSavedSettings.setBool(prefix + "NextOwnerTransfer", true)
        }
        getChild<CheckBoxCtrl>(prefix + "_transfer")?.setEnabled(copyable)
    }

    fun ok() {
        refresh()
        updateCap()
    }

    fun cancel() {
        for (cat in Categories.entries) {
            val i = cat.ordinal
            val name = categoryNames[i]
            gSavedSettings.setBool(name + "NextOwnerCopy",     nextOwnerCopy[i])
            gSavedSettings.setBool(name + "NextOwnerModify",   nextOwnerModify[i])
            gSavedSettings.setBool(name + "NextOwnerTransfer", nextOwnerTransfer[i])
            gSavedSettings.setBool(name + "ShareWithGroup",    shareWithGroup[i])
            gSavedSettings.setBool(name + "EveryoneCopy",      everyoneCopy[i])
        }
    }

    override fun refresh() {
        for (cat in Categories.entries) {
            val i = cat.ordinal
            val name = categoryNames[i]
            shareWithGroup[i]    = gSavedSettings.getBool(name + "ShareWithGroup")
            everyoneCopy[i]      = gSavedSettings.getBool(name + "EveryoneCopy")
            nextOwnerCopy[i]     = gSavedSettings.getBool(name + "NextOwnerCopy")
            nextOwnerModify[i]   = gSavedSettings.getBool(name + "NextOwnerModify")
            nextOwnerTransfer[i] = gSavedSettings.getBool(name + "NextOwnerTransfer")
        }
    }

    companion object {
        private const val MAX_HTTP_RETRIES = 5
        private const val RETRY_TIMEOUT_SECONDS = 5.0f

        var capSent: Boolean = false
            private set

        val categoryNames: Array<String> = arrayOf(
            "Objects", "Uploads", "Scripts", "Notecards",
            "Gestures", "Wearables", "Settings", "Materials"
        )

        fun sendInitialPerms() {
            if (!capSent) updateCap()
        }

        fun updateCap() {
            val region = gAgent.getRegion() ?: return
            val url = region.getCapability("AgentPreferences")
            if (url.isNotEmpty()) {
                TODO("APR: use JVM equivalent — launch coroutine: POST $url with default_object_perm_masks for Objects category")
            }
        }

        fun setCapSent(value: Boolean) { capSent = value }

        @Suppress("UnusedParameter")
        private suspend fun updateCapCoro(url: String) {
            TODO("APR: use JVM equivalent — HTTP POST $url with default_object_perm_masks; retry up to MAX_HTTP_RETRIES on failure; call setCapSent(true) on success")
        }
    }
}
