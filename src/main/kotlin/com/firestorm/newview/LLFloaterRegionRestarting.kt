package com.firestorm.newview

private var sSeconds: Int = 0
private var sShakeState: UInt = ShakeState.SHAKE_START

private object ShakeState {
    const val SHAKE_START: UInt = 0u
    const val SHAKE_LEFT: UInt = 1u
    const val SHAKE_UP: UInt = 2u
    const val SHAKE_RIGHT: UInt = 3u
    const val SHAKE_DOWN: UInt = 4u
    const val SHAKE_DONE: UInt = 5u
}

class LLFloaterRegionRestarting private constructor(key: Map<String, Any?>) : LLFloater(key) {

    companion object {
        fun close() {
            TODO("APR: use JVM equivalent - find region_restarting floater instance and closeFloater()")
        }

        fun updateTime(time: Int) {
            sSeconds = time
            sShakeState = ShakeState.SHAKE_START
        }
    }

    private val name: String = key["NAME"] as? String ?: ""
    private var shakeIterations: UInt = 0u
    private var shakeMagnitude: Float = 0f
    private var regionChangedConnection: (() -> Unit)? = null

    init {
        sSeconds = (key["SECONDS"] as? Int) ?: 0
    }

    override fun finalize() {
        regionChangedConnection?.invoke()
        regionChangedConnection = null
    }

    open fun postBuild(): Boolean {
        regionChangedConnection = addRegionChangedCallback { regionChange() }

        val landmarksId = findLandmarksCategoryId()
        startInventoryBackgroundFetch(landmarksId)

        setLandmarkComboPrearrangeCallback { refreshLandmarkList() }
        setTeleportBtnCommitCallback { onTeleportClicked() }

        setRegionNameText(name)

        sShakeState = ShakeState.SHAKE_START
        refresh()
        return true
    }

    private fun regionChange() {
        close()
    }

    open fun tick(): Boolean {
        refresh()
        return false
    }

    open fun refresh() {
        setRestartSecondsText(sSeconds)
        sSeconds = maxOf(sSeconds - 1, 0)
    }

    open fun onOpen(key: Map<String, Any?>) {
        refreshLandmarkList()
        TODO("APR: use JVM equivalent - flash viewer window icon for 5 seconds")
    }

    private fun onTeleportClicked() {
        val assetId = getLandmarkComboSelectedAssetId() ?: return
        TODO("APR: use JVM equivalent - gAgent.teleportViaLandmark(assetId)")
    }

    private fun refreshLandmarkList() {
        clearLandmarkComboExceptFirst()
        val landmarks = collectLandmarksFromInventory()
        for (item in landmarks.sortedBy { it.name }) {
            addLandmarkComboItem(item.name, item.assetId)
        }
        selectFirstLandmarkComboItem()
    }

    open fun draw() {
        TODO("GPU: LLFloater.draw()")

        val shakeInterval = 0.025f
        val shakeTotalDuration = 1.8f
        val shakeInitialMagnitude = 1.5f
        val shakeHorizontalBias = 0.25f

        if (isScreenShakeDisabled()) return

        if (sShakeState == ShakeState.SHAKE_START) {
            setShakeTimer(shakeInterval)
            sShakeState = ShakeState.SHAKE_LEFT
            shakeIterations = 0u
            shakeMagnitude = shakeInitialMagnitude
        }

        if (sShakeState != ShakeState.SHAKE_DONE && isShakeTimerExpired()) {
            unlockAgentCameraView()
            when (sShakeState) {
                ShakeState.SHAKE_LEFT -> {
                    TODO("GPU: gAgentCamera.setPanLeftKey(shakeMagnitude * shakeHorizontalBias)")
                    sShakeState = ShakeState.SHAKE_UP
                }
                ShakeState.SHAKE_UP -> {
                    TODO("GPU: gAgentCamera.setPanUpKey(shakeMagnitude)")
                    sShakeState = ShakeState.SHAKE_RIGHT
                }
                ShakeState.SHAKE_RIGHT -> {
                    TODO("GPU: gAgentCamera.setPanRightKey(shakeMagnitude * shakeHorizontalBias)")
                    sShakeState = ShakeState.SHAKE_DOWN
                }
                ShakeState.SHAKE_DOWN -> {
                    TODO("GPU: gAgentCamera.setPanDownKey(shakeMagnitude)")
                    shakeIterations++
                    val timeShaking = shakeInterval * (shakeIterations.toInt() * 4)
                    if (shakeTotalDuration <= timeShaking) {
                        sShakeState = ShakeState.SHAKE_DONE
                        shakeMagnitude = 0f
                    } else {
                        sShakeState = ShakeState.SHAKE_LEFT
                        val percentDone = (shakeTotalDuration - timeShaking) / shakeTotalDuration
                        shakeMagnitude = shakeInitialMagnitude * (percentDone * percentDone)
                    }
                }
            }
            setShakeTimer(shakeInterval)
        }
    }

    private fun addRegionChangedCallback(callback: () -> Unit): () -> Unit =
        TODO("APR: use JVM equivalent - gAgent.addRegionChangedCallback(callback)")

    private fun findLandmarksCategoryId(): Any =
        TODO("APR: use JVM equivalent - gInventory.findCategoryUUIDForType(FT_LANDMARK)")

    private fun startInventoryBackgroundFetch(categoryId: Any) =
        TODO("APR: use JVM equivalent - LLInventoryModelBackgroundFetch.start(categoryId)")

    private fun setLandmarkComboPrearrangeCallback(cb: () -> Unit) =
        TODO("APR: use JVM equivalent - set prearrange callback on 'landmark combo' child widget")

    private fun setTeleportBtnCommitCallback(cb: () -> Unit) =
        TODO("APR: use JVM equivalent - set commit callback on 'teleport_btn' child widget")

    private fun setRegionNameText(regionName: String) =
        TODO("APR: use JVM equivalent - format and set 'region_name' text box with regionName")

    private fun setRestartSecondsText(seconds: Int) =
        TODO("APR: use JVM equivalent - format and set 'restart_seconds' text box with countdown value")

    private fun getLandmarkComboSelectedAssetId(): Any? =
        TODO("APR: use JVM equivalent - get selected value as UUID from 'landmark combo'")

    private fun clearLandmarkComboExceptFirst() =
        TODO("APR: use JVM equivalent - delete all items except placeholder from 'landmark combo'")

    private data class LandmarkItem(val name: String, val assetId: Any)

    private fun collectLandmarksFromInventory(): List<LandmarkItem> =
        TODO("APR: use JVM equivalent - collectDescendentsIf with LLFindLandmarks from inventory root")

    private fun addLandmarkComboItem(name: String, assetId: Any) =
        TODO("APR: use JVM equivalent - add item to 'landmark combo' at bottom")

    private fun selectFirstLandmarkComboItem() =
        TODO("APR: use JVM equivalent - select first item in 'landmark combo'")

    private fun isScreenShakeDisabled(): Boolean =
        TODO("APR: use JVM equivalent - read FSNoScreenShakeOnRegionRestart cached control")

    private fun setShakeTimer(intervalSeconds: Float) =
        TODO("APR: use JVM equivalent - mShakeTimer.setTimerExpirySec(intervalSeconds)")

    private fun isShakeTimerExpired(): Boolean =
        TODO("APR: use JVM equivalent - mShakeTimer.hasExpired()")

    private fun unlockAgentCameraView() =
        TODO("GPU: gAgentCamera.unlockView()")
}
