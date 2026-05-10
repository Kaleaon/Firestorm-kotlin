package com.firestorm.newview

private const val PERM_NONE: UInt = 0u
private const val PERM_COPY: UInt = 0x00008000u
private const val PERM_MODIFY: UInt = 0x00004000u
private const val PERM_MOVE: UInt = 0x00002000u
private const val PERM_TRANSFER: UInt = 0x00001000u

private const val MAX_HTTP_RETRIES = 5
private const val RETRY_TIMEOUT = 5.0f

open class FloaterPerms(val key: Any) {

    fun postBuild(): Boolean = true

    companion object {
        fun getGroupPerms(prefix: String = ""): UInt =
            if (savedSettingBool("${prefix}ShareWithGroup")) PERM_COPY or PERM_MOVE or PERM_MODIFY
            else PERM_NONE

        fun getEveryonePerms(prefix: String = ""): UInt =
            if (savedSettingBool("${prefix}EveryoneCopy")) PERM_COPY
            else PERM_NONE

        fun getNextOwnerPerms(prefix: String = ""): UInt {
            var flags = PERM_MOVE
            if (savedSettingBool("${prefix}NextOwnerCopy")) flags = flags or PERM_COPY
            if (savedSettingBool("${prefix}NextOwnerModify")) flags = flags or PERM_MODIFY
            if (savedSettingBool("${prefix}NextOwnerTransfer")) flags = flags or PERM_TRANSFER
            return flags
        }

        fun getNextOwnerPermsInverted(prefix: String = ""): UInt {
            var flags = PERM_MOVE
            if (!savedSettingBool("${prefix}NextOwnerCopy")) flags = flags or PERM_COPY
            if (!savedSettingBool("${prefix}NextOwnerModify")) flags = flags or PERM_MODIFY
            if (!savedSettingBool("${prefix}NextOwnerTransfer")) flags = flags or PERM_TRANSFER
            return flags
        }

        private fun savedSettingBool(key: String): Boolean = TODO("APR: gSavedSettings.getBOOL($key)")
    }
}

class FloaterPermsDefault(key: Any) : FloaterPerms(key) {

    enum class Category(val settingPrefix: String) {
        OBJECTS("Objects"),
        UPLOADS("Uploads"),
        SCRIPTS("Scripts"),
        NOTECARDS("Notecards"),
        GESTURES("Gestures"),
        WEARABLES("Wearables"),
        SETTINGS("Settings"),
        MATERIALS("Materials");

        companion object {
            val all: List<Category> get() = values().toList()
        }
    }

    private val shareWithGroup = BooleanArray(Category.values().size)
    private val everyoneCopy = BooleanArray(Category.values().size)
    private val nextOwnerCopy = BooleanArray(Category.values().size)
    private val nextOwnerModify = BooleanArray(Category.values().size)
    private val nextOwnerTransfer = BooleanArray(Category.values().size)

    companion object {
        var capSent: Boolean = false

        fun sendInitialPerms() {
            if (!capSent) updateCap()
        }

        fun updateCap() {
            val region = agentRegion() ?: run {
                logWarning("Region not set, cannot request capability update")
                return
            }
            val url = regionCapability(region, "AgentPreferences")
            if (url.isNotEmpty()) {
                launchCoroutine("FloaterPermsDefault::updateCapCoro") { updateCapCoro(url) }
            }
        }

        fun setCapSent(sent: Boolean) {
            capSent = sent
        }

        private suspend fun updateCapCoro(url: String) {
            var retryCount = 0
            var previousReason = ""

            val postData = buildPermissionsPayload()

            while (true) {
                retryCount++
                val result = httpPost(url, postData)

                if (!result.isSuccess()) {
                    val reason = result.statusString()
                    if (reason != previousReason && capSent) {
                        previousReason = reason
                        TODO("APR: LLNotificationsUtil.add(\"DefaultObjectPermissions\", args)")
                    }
                    suspendTimeout(RETRY_TIMEOUT)
                    if (retryCount < MAX_HTTP_RETRIES) continue
                    logWarning("Unable to send default permissions. Giving up for now.")
                    return
                }
                break
            }

            previousReason = ""
            setCapSent(true)
            logInfo("Default permissions successfully sent to simulator")
        }

        private fun buildPermissionsPayload(): Map<String, Any> {
            val objectPrefix = Category.OBJECTS.settingPrefix
            return mapOf(
                "default_object_perm_masks" to mapOf(
                    "Group" to FloaterPerms.getGroupPerms(objectPrefix).toInt(),
                    "Everyone" to FloaterPerms.getEveryonePerms(objectPrefix).toInt(),
                    "NextOwner" to FloaterPerms.getNextOwnerPerms(objectPrefix).toInt()
                )
            )
        }

        private fun agentRegion(): Any? = TODO("APR: gAgent.getRegion()")
        private fun regionCapability(region: Any, cap: String): String = TODO("APR: region.getCapability(cap)")
        private fun launchCoroutine(name: String, block: suspend () -> Unit) = TODO("APR: LLCoros.instance().launch(name, block)")
        private suspend fun httpPost(url: String, data: Any): HttpResult = TODO("APR: LLCoreHttpUtil post and suspend")
        private suspend fun suspendTimeout(seconds: Float) = TODO("APR: llcoro::suspendUntilTimeout(seconds)")
        private fun logWarning(msg: String) = TODO("APR: LL_WARNS")
        private fun logInfo(msg: String) = TODO("APR: LL_INFOS")
    }

    override fun postBuild(): Boolean {
        if (!savedSettingBool("DefaultUploadPermissionsConverted")) {
            val pairs = listOf(
                "EveryoneCopy" to "UploadsEveryoneCopy",
                "NextOwnerCopy" to "UploadsNextOwnerCopy",
                "NextOwnerModify" to "UploadsNextOwnerModify",
                "NextOwnerTransfer" to "UploadsNextOwnerTransfer",
                "ShareWithGroup" to "UploadsShareWithGroup"
            )
            for ((src, dst) in pairs) {
                setSavedSettingBool(dst, savedSettingBool(src))
            }
            setSavedSettingBool("DefaultUploadPermissionsConverted", true)
        }

        connectCloseSignal { cancel() }
        refresh()
        return true
    }

    fun ok() {
        refresh()
        updateCap()
    }

    fun cancel() {
        for ((i, cat) in Category.values().withIndex()) {
            setSavedSettingBool("${cat.settingPrefix}NextOwnerCopy", nextOwnerCopy[i])
            setSavedSettingBool("${cat.settingPrefix}NextOwnerModify", nextOwnerModify[i])
            setSavedSettingBool("${cat.settingPrefix}NextOwnerTransfer", nextOwnerTransfer[i])
            setSavedSettingBool("${cat.settingPrefix}ShareWithGroup", shareWithGroup[i])
            setSavedSettingBool("${cat.settingPrefix}EveryoneCopy", everyoneCopy[i])
        }
    }

    fun onClickOK() {
        ok()
        closeFloater()
    }

    fun onClickCancel() {
        cancel()
        closeFloater()
    }

    fun onCommitCopy(prefix: String) {
        val copyable = savedSettingBool("${prefix}NextOwnerCopy")
        if (!copyable) {
            setSavedSettingBool("${prefix}NextOwnerTransfer", true)
        }
        setTransferCheckboxEnabled(prefix, copyable)
    }

    override fun refresh() {
        for ((i, cat) in Category.values().withIndex()) {
            shareWithGroup[i] = savedSettingBool("${cat.settingPrefix}ShareWithGroup")
            everyoneCopy[i] = savedSettingBool("${cat.settingPrefix}EveryoneCopy")
            nextOwnerCopy[i] = savedSettingBool("${cat.settingPrefix}NextOwnerCopy")
            nextOwnerModify[i] = savedSettingBool("${cat.settingPrefix}NextOwnerModify")
            nextOwnerTransfer[i] = savedSettingBool("${cat.settingPrefix}NextOwnerTransfer")
        }
    }

    // --- stubs for platform calls ---
    private fun savedSettingBool(key: String): Boolean = TODO("APR: gSavedSettings.getBOOL($key)")
    private fun setSavedSettingBool(key: String, value: Boolean) = TODO("APR: gSavedSettings.setBOOL($key, value)")
    private fun connectCloseSignal(block: () -> Unit) = TODO("APR: mCloseSignal.connect(block)")
    private fun closeFloater() = TODO("APR: closeFloater()")
    private fun setTransferCheckboxEnabled(prefix: String, enabled: Boolean) = TODO("APR: getChild<LLCheckBoxCtrl>(prefix+\"_transfer\").setEnabled(enabled)")
    open fun refresh() = Unit
}

private class HttpResult {
    fun isSuccess(): Boolean = TODO("APR: HTTP result success check")
    fun statusString(): String = TODO("APR: HTTP result status string")
}
