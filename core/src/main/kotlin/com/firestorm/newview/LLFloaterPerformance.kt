package com.firestorm.newview

import java.util.UUID

private const val REFRESH_INTERVAL: Float = 1.0f
private const val BAR_LEFT_PAD: Int = 2
private const val BAR_RIGHT_PAD: Int = 5
private const val BAR_BOTTOM_PAD: Int = 9
private const val RENDER_QUALITY_LEVEL: UInt = 3u

private fun findAvatar(id: UUID): LLVOAvatar? {
    var obj: LLViewerObject? = gObjectList.findObject(id)
    while (obj != null && obj.isAttachment()) {
        obj = obj.getParent() as? LLViewerObject
    }
    return if (obj != null && obj.isAvatar()) obj as LLVOAvatar else null
}

private fun isAlmAvailable(): Boolean {
    val bumpshiny = LLCubeMap.sUseCubeMaps &&
        LLFeatureManager.getInstance().isFeatureAvailable("RenderObjectBump") &&
        gSavedSettings.getBool("RenderObjectBump")
    val shaders = gSavedSettings.getBool("WindLightUseAtmosShaders")
    return LLFeatureManager.getInstance().isFeatureAvailable("RenderDeferred") && bumpshiny && shaders
}

class LLExceptionsContextMenu(private val floaterPerformance: LLFloaterPerformance) : LLListContextMenu() {
    override fun createMenu(): LLContextMenu {
        val registrar = LLUICtrl.CommitCallbackRegistry.ScopedRegistrar()
        val enableRegistrar = LLUICtrl.EnableCallbackRegistry.ScopedRegistrar()
        registrar.add("Settings.SetRendering") { _: LLUICtrl, userdata: LLSD ->
            floaterPerformance.onCustomAction(userdata, mUUIDs.first())
        }
        enableRegistrar.add("Settings.IsSelected") { _: LLUICtrl, userdata: LLSD ->
            floaterPerformance.isActionChecked(userdata, mUUIDs.first())
        }
        return createFromFile("menu_avatar_rendering_settings.xml")
    }
}

class LLFloaterPerformance(key: LLSD) : LLFloater(key) {

    private lateinit var mainPanel: LLPanel
    private lateinit var nearbyPanel: LLPanel
    private lateinit var complexityPanel: LLPanel
    private lateinit var hudsPanel: LLPanel
    private lateinit var settingsPanel: LLPanel
    private lateinit var autoadjustmentsPanel: LLPanel
    private lateinit var hudList: LLNameListCtrl
    private lateinit var objectList: LLNameListCtrl
    private lateinit var nearbyList: LLNameListCtrl

    private lateinit var startAutotuneBtn: LLButton
    private lateinit var stopAutotuneBtn: LLButton

    private var textWipDesc: LLTextBox? = null
    private var textDisplayDesc: LLTextBox? = null
    private var textFpsLabel: LLTextBox? = null
    private var textFpsValue: LLTextBox? = null

    private var checkTuneContinuous: LLCheckBoxCtrl? = null

    private val contextMenu: LLExceptionsContextMenu = LLExceptionsContextMenu(this)
    private val updateTimer: LLTimer = LLTimer()

    // -1f until the first GPU profile has run.
    private var nearbyMaxGpuTime: Float = -1f
    private var maxArtChangedSignal: (() -> Unit)? = null

    override fun postBuild(): Boolean {
        mainPanel = getChild<LLPanel>("panel_performance_main")
        nearbyPanel = getChild<LLPanel>("panel_performance_nearby")
        complexityPanel = getChild<LLPanel>("panel_performance_complexity")
        settingsPanel = getChild<LLPanel>("panel_performance_preferences")
        hudsPanel = getChild<LLPanel>("panel_performance_huds")
        autoadjustmentsPanel = getChild<LLPanel>("panel_performance_autoadjustments")

        getChild<LLPanel>("nearby_subpanel").setMouseDownCallback { showSelectedPanel(nearbyPanel) }
        getChild<LLPanel>("complexity_subpanel").setMouseDownCallback { showSelectedPanel(complexityPanel) }
        getChild<LLPanel>("settings_subpanel").setMouseDownCallback { showSelectedPanel(settingsPanel) }
        getChild<LLPanel>("huds_subpanel").setMouseDownCallback { showSelectedPanel(hudsPanel) }
        getChild<LLPanel>("autoadjustments_subpanel").setMouseDownCallback { showSelectedPanel(autoadjustmentsPanel) }

        initBackBtn(nearbyPanel)
        initBackBtn(complexityPanel)
        initBackBtn(settingsPanel)
        initBackBtn(hudsPanel)
        initBackBtn(autoadjustmentsPanel)

        hudList = hudsPanel.getChild<LLNameListCtrl>("hud_list")
        hudList.setNameListType(LLNameListCtrl.SPECIAL)
        hudList.setHoverIconName("StopReload_Off")
        hudList.setIconClickedCallback { id: UUID -> detachObject(id) }

        objectList = complexityPanel.getChild<LLNameListCtrl>("obj_list")
        objectList.setNameListType(LLNameListCtrl.SPECIAL)
        objectList.setHoverIconName("StopReload_Off")
        objectList.setIconClickedCallback { id: UUID -> detachObject(id) }

        settingsPanel.getChild<LLButton>("advanced_btn").setCommitCallback { onClickAdvanced() }
        settingsPanel.getChild<LLButton>("defaults_btn").setCommitCallback { onClickDefaults() }
        settingsPanel.getChild<LLRadioGroup>("graphics_quality").setCommitCallback { _: LLUICtrl, data: LLSD -> onChangeQuality(data) }
        settingsPanel.getChild<LLCheckBoxCtrl>("advanced_lighting_model").setMouseDownCallback { onClickAdvancedLighting() }
        settingsPanel.getChild<LLComboBox>("ShadowDetail").setMouseDownCallback { onClickShadows() }

        nearbyPanel.getChild<LLButton>("exceptions_btn").setCommitCallback { onClickExceptions() }
        nearbyPanel.getChild<LLCheckBoxCtrl>("hide_avatars").setCommitCallback { onClickHideAvatars() }
        nearbyPanel.getChild<LLCheckBoxCtrl>("hide_avatars").set(
            !LLPipeline.hasRenderTypeControl(LLPipeline.RENDER_TYPE_AVATAR)
        )
        nearbyList = nearbyPanel.getChild<LLNameListCtrl>("nearby_list")
        nearbyList.setRightMouseDownCallback { ctrl: LLUICtrl, x: Int, y: Int -> onAvatarListRightClick(ctrl, x, y) }

        maxArtChangedSignal = {
            gSavedSettings.getControl("RenderAvatarMaxART")
                ?.getCommitSignal()
                ?.connect { updateMaxRenderTime() }
        }.also { it() }
        nearbyPanel.getChild<LLSliderCtrl>("RenderAvatarMaxART").setCommitCallback { updateMaxRenderTime() }

        if (!LLPerfStats.tunables.userAutoTuneEnabled) {
            gSavedSettings.setFloat("AutoTuneRenderFarClipTarget", LLPipeline.renderFarClip)
        }

        val fpsLimit = "%d".format(gViewerWindow.getWindow().getRefreshRate())
        autoadjustmentsPanel.getChild<LLTextBox>("vsync_desc_limit").setTextArg("[FPS_LIMIT]", fpsLimit)
        autoadjustmentsPanel.getChild<LLTextBox>("display_desc").setTextArg("[FPS_LIMIT]", fpsLimit)
        autoadjustmentsPanel.getChild<LLButton>("defaults_btn").setCommitCallback { onClickDefaults() }

        startAutotuneBtn = autoadjustmentsPanel.getChild<LLButton>("start_autotune")
        stopAutotuneBtn = autoadjustmentsPanel.getChild<LLButton>("stop_autotune")
        startAutotuneBtn.setCommitCallback { startAutotune() }
        stopAutotuneBtn.setCommitCallback { stopAutotune() }

        checkTuneContinuous = autoadjustmentsPanel.getChild<LLCheckBoxCtrl>("AutoTuneContinuous")
        textWipDesc = autoadjustmentsPanel.getChild<LLTextBox>("wip_desc")
        textDisplayDesc = autoadjustmentsPanel.getChild<LLTextBox>("display_desc")

        textFpsLabel = getChild<LLTextBox>("fps_lbl")
        textFpsValue = getChild<LLTextBox>("fps_value")

        gSavedPerAccountSettings.declareBool(
            "HadEnabledAutoFPS", false,
            "User had enabled AutoFPS at least once",
            LLControlVariable.PERSIST_ALWAYS
        )

        return true
    }

    fun showSelectedPanel(selectedPanel: LLPanel) {
        hidePanels()
        mainPanel.setVisible(false)
        selectedPanel.setVisible(true)

        when (selectedPanel) {
            hudsPanel -> populateHudList()
            nearbyPanel -> populateNearbyList()
            complexityPanel -> populateObjectList()
        }
    }

    fun showMainPanel() {
        hidePanels()
        mainPanel.setVisible(true)
    }

    fun hidePanels() {
        nearbyPanel.setVisible(false)
        complexityPanel.setVisible(false)
        hudsPanel.setVisible(false)
        settingsPanel.setVisible(false)
        autoadjustmentsPanel.setVisible(false)
    }

    fun showAutoadjustmentsPanel() {
        showSelectedPanel(autoadjustmentsPanel)
    }

    open fun draw() {
        enableAutotuneWarning()

        if (updateTimer.hasExpired() &&
            !LLFloaterReg.instanceVisible("save_pref_preset", PRESETS_GRAPHIC)
        ) {
            setFpsText()
            when {
                hudsPanel.getVisible() -> populateHudList()
                nearbyPanel.getVisible() -> {
                    populateNearbyList()
                    nearbyPanel.getChild<LLCheckBoxCtrl>("hide_avatars").set(
                        !LLPipeline.hasRenderTypeControl(LLPipeline.RENDER_TYPE_AVATAR)
                    )
                }
                complexityPanel.getVisible() -> populateObjectList()
            }
            updateTimer.setTimerExpirySec(REFRESH_INTERVAL)
        }
        updateAutotuneCtrls(LLPerfStats.tunables.userAutoTuneEnabled)

        super.draw()
    }

    fun detachObject(objId: UUID) {
        val obj = gObjectList.findObject(objId)
        if (obj != null) {
            LLAppearanceMgr.instance().removeItemFromAvatar(obj.getAttachmentItemId())
        }
    }

    fun onAvatarListRightClick(ctrl: LLUICtrl, x: Int, y: Int) {
        val list = ctrl as? LLNameListCtrl ?: return
        list.selectItemAt(x, y, MASK_NONE)

        if (list.getCurrentId() != LLUUIDNull && list.getCurrentId() != gAgentId) {
            val selectedUuids = mutableListOf(list.getCurrentId())
            contextMenu.show(ctrl, selectedUuids, x, y)
        }
    }

    fun onCustomAction(userdata: LLSD, avId: UUID) {
        val commandName = userdata.asString()
        val newSetting: Int = when (commandName) {
            "default" -> LLVOAvatar.AV_RENDER_NORMALLY.value
            "never" -> LLVOAvatar.AV_DO_NOT_RENDER.value
            "always" -> LLVOAvatar.AV_ALWAYS_RENDER.value
            else -> 0
        }

        val avatarp = findAvatar(avId)
        if (avatarp != null) {
            avatarp.setVisualMuteSettings(LLVOAvatar.VisualMuteSettings.fromInt(newSetting))
        } else {
            LLRenderMuteList.getInstance().saveVisualMuteSetting(avId, newSetting)
        }
    }

    fun isActionChecked(userdata: LLSD, avId: UUID): Boolean {
        val commandName = userdata.asString()
        val visualSetting = LLRenderMuteList.getInstance().getSavedVisualMuteSetting(avId)
        return when (commandName) {
            "default" -> visualSetting == LLVOAvatar.AV_RENDER_NORMALLY.value
            "non_default" -> visualSetting != LLVOAvatar.AV_RENDER_NORMALLY.value
            "never" -> visualSetting == LLVOAvatar.AV_DO_NOT_RENDER.value
            "always" -> visualSetting == LLVOAvatar.AV_ALWAYS_RENDER.value
            else -> false
        }
    }

    private fun initBackBtn(panel: LLPanel) {
        panel.getChild<LLButton>("back_btn").setCommitCallback { showMainPanel() }
        panel.getChild<LLTextBox>("back_lbl").setShowCursorHand(false)
        panel.getChild<LLTextBox>("back_lbl").setSoundFlags(LLView.MOUSE_UP)
        panel.getChild<LLTextBox>("back_lbl").setClickedCallback { showMainPanel() }
    }

    private fun populateHudList() {
        val prevPos = hudList.getScrollPos()
        val prevSelectedId = hudList.getSelectedSpecialId()
        hudList.clearRows()
        hudList.updateColumns(true)

        val avatar = gAgentAvatarp
        // no-op

        val begin = avatar.mAttachmentPoints.iterator()

        var maxGpuTime = -1f
        for (entry in avatar.mAttachmentPoints) {
            val attachment = entry.value
            for (attachedObject in attachment.mAttachedObjects) {
                if (attachedObject != null && attachedObject.isHudAttachment()) {
                    if (attachedObject.gpuRenderTime > maxGpuTime) maxGpuTime = attachedObject.gpuRenderTime
                }
            }
        }

        for (entry in avatar.mAttachmentPoints) {
            val attachment = entry.value ?: continue
            for (attachedObject in attachment.mAttachedObjects) {
                if (attachedObject != null && attachedObject.isHudAttachment()) {
                    val gpuTime = attachedObject.gpuRenderTime

                    val item = LLSD()
                    item["special_id"] = attachedObject.id
                    item["target"] = LLNameListCtrl.SPECIAL
                    val row = item["columns"]
                    row[0]["column"] = "complex_visual"
                    row[0]["type"] = "bar"
                    val value = row[0]["value"]
                    value["ratio"] = if (maxGpuTime > 0f) gpuTime / maxGpuTime else 0f
                    value["bottom"] = BAR_BOTTOM_PAD
                    value["left_pad"] = BAR_LEFT_PAD
                    value["right_pad"] = BAR_RIGHT_PAD

                    row[1]["column"] = "complex_value"
                    row[1]["type"] = "text"
                    row[1]["value"] = "%.f".format(gpuTime * 1000f)
                    row[1]["font"]["name"] = "SANSSERIF"

                    row[2]["column"] = "name"
                    row[2]["type"] = "text"
                    row[2]["value"] = attachedObject.getAttachmentItemName()
                    row[2]["font"]["name"] = "SANSSERIF"

                    val obj = hudList.addElement(item)
                    val valueText = obj?.getColumn(1) as? LLScrollListText
                    // no-op
                }
            }
        }
        hudList.sortByColumnIndex(1, false)
        hudList.setScrollPos(prevPos)
        hudList.selectItemBySpecialId(prevSelectedId)
    }

    private fun populateObjectList() {
        val prevPos = objectList.getScrollPos()
        val prevSelectedId = objectList.getSelectedSpecialId()
        objectList.clearRows()
        objectList.updateColumns(true)

        val avatar = gAgentAvatarp
        // no-op

        var maxGpuTime = -1f
        for (entry in avatar.mAttachmentPoints) {
            val attachment = entry.value
            for (attachedObject in attachment.mAttachedObjects) {
                if (attachedObject != null && !attachedObject.isHudAttachment()) {
                    if (attachedObject.gpuRenderTime > maxGpuTime) maxGpuTime = attachedObject.gpuRenderTime
                }
            }
        }

        for (entry in avatar.mAttachmentPoints) {
            val attachment = entry.value ?: continue
            for (attachedObject in attachment.mAttachedObjects) {
                if (attachedObject != null && !attachedObject.isHudAttachment()) {
                    val gpuTime = attachedObject.gpuRenderTime

                    val item = LLSD()
                    item["special_id"] = attachedObject.id
                    item["target"] = LLNameListCtrl.SPECIAL
                    val row = item["columns"]
                    row[0]["column"] = "complex_visual"
                    row[0]["type"] = "bar"
                    val value = row[0]["value"]
                    value["ratio"] = if (maxGpuTime > 0f) gpuTime / maxGpuTime else 0f
                    value["bottom"] = BAR_BOTTOM_PAD
                    value["left_pad"] = BAR_LEFT_PAD
                    value["right_pad"] = BAR_RIGHT_PAD

                    row[1]["column"] = "complex_value"
                    row[1]["type"] = "text"
                    row[1]["value"] = "%.f".format(gpuTime * 1000f)
                    row[1]["font"]["name"] = "SANSSERIF"

                    row[2]["column"] = "name"
                    row[2]["type"] = "text"
                    row[2]["value"] = attachedObject.getAttachmentItemName()
                    row[2]["font"]["name"] = "SANSSERIF"

                    val obj = objectList.addElement(item)
                    val valueText = obj?.getColumn(1) as? LLScrollListText
                    // no-op
                }
            }
        }
        objectList.sortByColumnIndex(1, false)
        objectList.setScrollPos(prevPos)
        objectList.selectItemBySpecialId(prevSelectedId)
    }

    private fun populateNearbyList() {
        val showTunedArt = gSavedSettings.getBool("ShowTunedART")
        val prevPos = nearbyList.getScrollPos()
        val prevSelectedId = nearbyList.getStringUUIDSelectedItem()
        nearbyList.clearRows()
        nearbyList.updateColumns(true)

        val validNearbyAvs = mutableListOf<LLVOAvatar>()
        nearbyMaxGpuTime = LLWorld.getInstance().getNearbyAvatarsAndMaxGpuTime(validNearbyAvs)

        for (avatar in validNearbyAvs) {
            if (LLVOAvatar.AOA_INVISIBLE != avatar.getOverallAppearance()) {
                val renderAvGpuMs = avatar.getGpuRenderTime()
                val isSlow = avatar.isTooSlow()

                val item = LLSD()
                item["id"] = avatar.id
                val row = item["columns"]
                row[0]["column"] = "complex_visual"
                row[0]["type"] = "bar"
                val value = row[0]["value"]
                value["ratio"] = if (nearbyMaxGpuTime > 0f) renderAvGpuMs / nearbyMaxGpuTime else 0f
                value["bottom"] = BAR_BOTTOM_PAD
                value["left_pad"] = BAR_LEFT_PAD
                value["right_pad"] = BAR_RIGHT_PAD

                row[1]["column"] = "complex_value"
                row[1]["type"] = "text"
                row[1]["value"] = "%.f".format(renderAvGpuMs * 1000f)
                row[1]["font"]["name"] = "SANSSERIF"

                row[3]["column"] = "name"
                row[3]["type"] = "text"
                row[3]["value"] = avatar.getFullname()
                row[3]["font"]["name"] = "SANSSERIF"

                val avItem = nearbyList.addElement(item)
                if (avItem != null) {
                    val valueText = avItem.getColumn(1) as? LLScrollListText
                    // no-op

                    val nameText = avItem.getColumn(2) as? LLScrollListText
                    if (nameText != null) {
                        if (avatar.isSelf()) {
                            nameText.setColor(LLUIColorTable.instance().getColor("DrYellow"))
                        } else {
                            var color = "white"
                            if (isSlow || LLVOAvatar.AOA_JELLYDOLL == avatar.getOverallAppearance()) {
                                color = "LabelDisabledColor"
                                val bar = avItem.getColumn(0) as? LLScrollListBar
                                bar?.setColor(LLUIColorTable.instance().getColor(color))
                            } else if (LLVOAvatar.AOA_NORMAL == avatar.getOverallAppearance()) {
                                color = if (LLAvatarActions.isFriend(avatar.id)) "ConversationFriendColor" else "white"
                            }
                            nameText.setColor(LLUIColorTable.instance().getColor(color))
                        }
                    }
                }
            }
        }
        nearbyList.sortByColumnIndex(1, false)
        nearbyList.setScrollPos(prevPos)
        nearbyList.selectById(prevSelectedId)
    }

    private fun setFpsText() {
        val numPeriods = 50
        val currentFps = LLTrace.getFrameRecording().getPeriodMedianPerSec(LLStatViewer.FPS, numPeriods).toInt()
        textFpsValue?.setValue(currentFps)

        var fpsText = getString("fps_text")
        val vsyncEnabled = gSavedSettings.getBool("RenderVSyncEnable")
        val refreshRate = gViewerWindow.getWindow().getRefreshRate()
        if (vsyncEnabled && refreshRate > 0 && currentFps >= refreshRate) {
            fpsText += getString("max_text")
        }
        textFpsLabel?.setValue(fpsText)
    }

    private fun onClickAdvanced() {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        instance?.saveSettings()
        LLFloaterReg.showInstance("prefs_graphics_advanced")
    }

    private fun onClickDefaults() {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        instance?.setRecommendedSettings()
    }

    private fun onChangeQuality(data: LLSD) {
        val instance = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
        instance?.onChangeQuality(data)
    }

    private fun onClickHideAvatars() {
        LLPipeline.toggleRenderTypeControl(LLPipeline.RENDER_TYPE_AVATAR)
    }

    private fun onClickExceptions() {
        LLFloaterReg.showInstance("avatar_render_settings")
    }

    private fun updateMaxRenderTime() {
        LLAvatarComplexityControls.updateMaxRenderTime(
            nearbyPanel.getChild<LLSliderCtrl>("RenderAvatarMaxART"),
            nearbyPanel.getChild<LLTextBox>("RenderAvatarMaxARTText"),
            true
        )
    }

    private fun startAutotune() {
        LLPerfStats.tunables.userAutoTuneEnabled = true
    }

    private fun stopAutotune() {
        LLPerfStats.tunables.userAutoTuneEnabled = false
    }

    private fun updateAutotuneCtrls(autotuneEnabled: Boolean) {
        val autoTuneLocked = gSavedSettings.getBool("AutoTuneLock")
        startAutotuneBtn.setEnabled(!autotuneEnabled && !autoTuneLocked)
        stopAutotuneBtn.setEnabled(autotuneEnabled && !autoTuneLocked)
        checkTuneContinuous?.setEnabled(!autotuneEnabled || (autotuneEnabled && autoTuneLocked))

        textWipDesc?.setVisible(autotuneEnabled && !autoTuneLocked)
        textDisplayDesc?.setVisible(LLPerfStats.tunables.vsyncEnabled)
    }

    private fun enableAutotuneWarning() {
        if (!gSavedPerAccountSettings.getBool("HadEnabledAutoFPS") && LLPerfStats.tunables.userAutoTuneEnabled) {
            gSavedPerAccountSettings.setBool("HadEnabledAutoFPS", true)
            LLNotificationsUtil.add("EnableAutoFPSWarning", LLSD(), LLSD()) { notif: LLSD, resp: LLSD ->
                val opt = LLNotificationsUtil.getSelectedOption(notif, resp)
                if (opt == 0) {
                    LLFloaterReg.showInstance("save_pref_preset", PRESETS_GRAPHIC)
                }
            }
        }
    }

    private fun onClickAdvancedLighting() {
        if (!isAlmAvailable()) {
            changeQualityLevel("AdvancedLightingConfirm")
        }
    }

    private fun onClickShadows() {
        if (!isAlmAvailable() || !gSavedSettings.getBool("RenderDeferred")) {
            changeQualityLevel("ShadowsConfirm")
        }
    }

    companion object {
        fun changeQualityLevel(notif: String) {
            LLNotificationsUtil.add(notif, LLSD(), LLSD()) { notifResult: LLSD, resp: LLSD ->
                val opt = LLNotificationsUtil.getSelectedOption(notifResult, resp)
                if (opt == 0) {
                    val instance = LLFloaterReg.getTypedInstance<LLFloaterPreference>("preferences")
                    if (instance != null) {
                        gSavedSettings.setUInt("RenderQualityPerformance", RENDER_QUALITY_LEVEL)
                        instance.onChangeQuality(LLSD(RENDER_QUALITY_LEVEL.toInt()))
                    }
                }
            }
        }
    }
}
