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
            System.err.println("LLFloaterRegionRestarting: close not yet implemented")
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
        System.err.println("LLFloaterRegionRestarting: flash viewer window icon not yet implemented")
    }

    private fun onTeleportClicked() {
        val assetId = getLandmarkComboSelectedAssetId() ?: return
        System.err.println("LLFloaterRegionRestarting: gAgent.teleportViaLandmark not yet implemented")
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
        // no-op: GPU LLFloater.draw() not yet implemented

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
                    // no-op: GPU gAgentCamera.setPanLeftKey not yet implemented
                    sShakeState = ShakeState.SHAKE_UP
                }
                ShakeState.SHAKE_UP -> {
                    // no-op: GPU gAgentCamera.setPanUpKey not yet implemented
                    sShakeState = ShakeState.SHAKE_RIGHT
                }
                ShakeState.SHAKE_RIGHT -> {
                    // no-op: GPU gAgentCamera.setPanRightKey not yet implemented
                    sShakeState = ShakeState.SHAKE_DOWN
                }
                ShakeState.SHAKE_DOWN -> {
                    // no-op: GPU gAgentCamera.setPanDownKey not yet implemented
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

    private fun addRegionChangedCallback(callback: () -> Unit): () -> Unit {
        System.err.println("LLFloaterRegionRestarting: addRegionChangedCallback not yet implemented")
        return {}
    }

    private fun findLandmarksCategoryId(): Any {
        System.err.println("LLFloaterRegionRestarting: findLandmarksCategoryId not yet implemented")
        return Unit
    }

    private fun startInventoryBackgroundFetch(categoryId: Any) {
        System.err.println("LLFloaterRegionRestarting: startInventoryBackgroundFetch not yet implemented")
    }

    private fun setLandmarkComboPrearrangeCallback(cb: () -> Unit) {
        System.err.println("LLFloaterRegionRestarting: setLandmarkComboPrearrangeCallback not yet implemented")
    }

    private fun setTeleportBtnCommitCallback(cb: () -> Unit) {
        System.err.println("LLFloaterRegionRestarting: setTeleportBtnCommitCallback not yet implemented")
    }

    private fun setRegionNameText(regionName: String) {
        System.err.println("LLFloaterRegionRestarting: setRegionNameText not yet implemented")
    }

    private fun setRestartSecondsText(seconds: Int) {
        System.err.println("LLFloaterRegionRestarting: setRestartSecondsText not yet implemented")
    }

    private fun getLandmarkComboSelectedAssetId(): Any? = null

    private fun clearLandmarkComboExceptFirst() {
        System.err.println("LLFloaterRegionRestarting: clearLandmarkComboExceptFirst not yet implemented")
    }

    private data class LandmarkItem(val name: String, val assetId: Any)

    private fun collectLandmarksFromInventory(): List<LandmarkItem> {
        System.err.println("LLFloaterRegionRestarting: collectLandmarksFromInventory not yet implemented")
        return emptyList()
    }

    private fun addLandmarkComboItem(name: String, assetId: Any) {
        System.err.println("LLFloaterRegionRestarting: addLandmarkComboItem not yet implemented")
    }

    private fun selectFirstLandmarkComboItem() {
        System.err.println("LLFloaterRegionRestarting: selectFirstLandmarkComboItem not yet implemented")
    }

    private fun isScreenShakeDisabled(): Boolean = false

    private fun setShakeTimer(intervalSeconds: Float) {
        System.err.println("LLFloaterRegionRestarting: setShakeTimer not yet implemented")
    }

    private fun isShakeTimerExpired(): Boolean = false

    private fun unlockAgentCameraView() {
        // no-op: GPU gAgentCamera.unlockView not yet implemented
    }
}
