package com.firestorm.newview

import java.util.UUID

abstract class LLFloaterModelUploadBase(key: LLSD) :
    LLFloater(key),
    LLUploadPermissionsObserver,
    LLWholeModelFeeObserver,
    LLWholeModelUploadObserver {

    protected var uploadModelUrl: String = ""
    protected var hasUploadPerm: Boolean = false

    abstract override fun setPermissonsErrorStatus(status: Int, reason: String)

    abstract override fun onPermissionsReceived(result: LLSD)

    abstract override fun onModelPhysicsFeeReceived(result: LLSD, uploadUrl: String)

    abstract override fun setModelPhysicsFeeErrorStatus(status: Int, reason: String, result: LLSD)

    open fun onModelUploadSuccess() {}

    open fun onModelUploadFailure() {}

    protected fun requestAgentUploadPermissions() {
        val capability = "MeshUploadFlag"
        val url = gAgent.getRegionCapability(capability)

        if (url.isNotEmpty()) {
            TODO("APR: use JVM equivalent — launch coroutine calling requestAgentUploadPermissionsCoro(url, permObserverHandle)")
        } else {
            val args = LLSD()
            args["CAPABILITY"] = capability
            LLNotificationsUtil.add("RegionCapabilityRequestError", args)
            // Server-side capability missing: grant permission locally to avoid blocking upload.
            hasUploadPerm = true
        }
    }

    protected fun requestAgentUploadPermissionsCoro(url: String, observerHandle: LLHandle<LLUploadPermissionsObserver>) {
        TODO("APR: use JVM HTTP client — GET $url, then call observer.setPermissonsErrorStatus or observer.onPermissionsReceived")
    }
}
