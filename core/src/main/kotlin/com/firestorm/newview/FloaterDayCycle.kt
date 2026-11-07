package com.firestorm.newview

import java.util.UUID
import kotlin.math.abs
import kotlin.math.fmod

private val TRACK_TABS = arrayOf(
    "water_track",
    "sky1_track",
    "sky2_track",
    "sky3_track",
    "sky4_track",
)

private const val ICN_LOCK_EDIT = "icn_lock_edit"
private const val BTN_SAVE = "save_btn"
private const val BTN_FLYOUT = "btn_flyout"
private const val BTN_CANCEL = "cancel_btn"
private const val BTN_ADDFRAME = "add_frame"
private const val BTN_DELFRAME = "delete_frame"
private const val BTN_IMPORT = "btn_import"
private const val BTN_LOADFRAME = "btn_load_frame"
private const val BTN_CLONETRACK = "copy_track"
private const val BTN_LOADTRACK = "load_track"
private const val BTN_CLEARTRACK = "clear_track"
private const val SLDR_TIME = "WLTimeSlider"
private const val SLDR_KEYFRAMES = "WLDayCycleFrames"
private const val VIEW_SKY_SETTINGS = "frame_settings_sky"
private const val VIEW_WATER_SETTINGS = "frame_settings_water"
private const val LBL_CURRENT_TIME = "current_time"
private const val TXT_DAY_NAME = "day_cycle_name"
private const val TABS_SKYS = "sky_tabs"
private const val TABS_WATER = "water_tabs"
private const val BTN_PLAY = "play_btn"
private const val BTN_SKIP_BACK = "skip_back_btn"
private const val BTN_SKIP_FORWARD = "skip_forward_btn"
private const val EVNT_DAYTRACK = "DayCycle.Track"
private const val EVNT_PLAY = "DayCycle.PlayActions"
private const val ACTION_PLAY = "play"
private const val ACTION_PAUSE = "pause"
private const val ACTION_FORWARD = "forward"
private const val ACTION_BACK = "back"
private const val XML_FLYOUTMENU_FILE = "menu_save_settings.xml"
private const val ACTION_SAVE = "save_settings"
private const val ACTION_SAVEAS = "save_as_new_settings"
private const val ACTION_COMMIT = "commit_changes"
private const val ACTION_APPLY_LOCAL = "apply_local"
private const val ACTION_APPLY_PARCEL = "apply_parcel"
private const val ACTION_APPLY_REGION = "apply_region"
private const val DAY_CYCLE_PLAY_TIME_SECONDS = 60f
private const val STR_COMMIT_PARCEL = "commit_parcel"
private const val STR_COMMIT_REGION = "commit_region"

open class FloaterEditExtDayCycle(key: LLSD) : FloaterEditEnvironmentBase(key) {

    enum class EditContext {
        UNKNOWN, INVENTORY, PARCEL, REGION
    }

    data class FrameData(val frame: Float = 0f, val settings: SettingsBase? = null)

    private var editDay: SettingsDay? = null
    private var dayLength: Long = 0L
    private var currentTrack: UInt = 1u
    private var lastFrameSlider: String = ""
    private var shiftCopyEnabled: Boolean = false
    private var editContext: EditContext = EditContext.UNKNOWN
    private var isPlaying: Boolean = false
    private var playStartFrame: Float = 0f
    private var isDirty: Boolean = false

    private var skyBlender: TrackBlenderLoopingManual? = null
    private var waterBlender: TrackBlenderLoopingManual? = null
    private var scratchSky: SettingsSky = SettingsVOSky.buildDefaultSky()
    private var scratchWater: SettingsWater = SettingsVOWater.buildDefaultWater()
    private var editSky: SettingsSky = scratchSky
    private var editWater: SettingsWater = scratchWater
    private var currentEdit: SettingsBase? = null

    private val sliderKeyMap: MutableMap<String, FrameData> = mutableMapOf()
    private val commitSignal: MutableList<(SettingsDay) -> Unit> = mutableListOf()

    private var nameEditor: LineEditor? = null
    private var cancelButton: Button? = null
    private var addFrameButton: Button? = null
    private var deleteFrameButton: Button? = null
    private var importButton: Button? = null
    private var loadFrame: Button? = null
    private var cloneTrack: Button? = null
    private var loadTrack: Button? = null
    private var clearTrack: Button? = null
    private var timeSlider: MultiSliderCtrl? = null
    private var framesSlider: MultiSliderCtrl? = null
    private var skyTabLayoutContainer: View? = null
    private var waterTabLayoutContainer: View? = null
    private var currentTimeLabel: TextBox? = null
    private var flyoutControl: FlyoutComboBtnCtrl? = null
    private var trackFloater: FloaterHandle? = null

    private var playTimer: FrameTimer = FrameTimer()

    companion object {
        const val KEY_EDIT_CONTEXT = "edit_context"
        const val KEY_DAY_LENGTH = "day_length"
        const val VALUE_CONTEXT_INVENTORY = "inventory"
        const val VALUE_CONTEXT_PARCEL = "parcel"
        const val VALUE_CONTEXT_REGION = "region"

        fun onIdlePlay(userData: Any?) {
            val self = userData as? FloaterEditExtDayCycle ?: return
            if (!AppViewer.isDisconnected) {
                val sky = self.skyBlender
                val water = self.waterBlender
                if (sky == null || water == null) {
                    self.stopPlay()
                } else {
                    val prcntPlayed = self.playTimer.elapsedSeconds / DAY_CYCLE_PLAY_TIME_SECONDS
                    val newFrame = (self.playStartFrame + prcntPlayed) % 1f
                    self.timeSlider?.setCurSliderValue(newFrame)
                    sky.setPosition(newFrame)
                    water.setPosition(newFrame)
                    self.synchronizeTabs()
                    self.updateTimeAndLabel()
                    self.updateButtons()
                }
            }
        }
    }

    init {
        registerCommitCallback(EVNT_DAYTRACK) { _, data -> onTrackSelectionCallback(data) }
        registerCommitCallback(EVNT_PLAY) { _, data -> onPlayActionCallback(data) }
    }

    override fun postBuild(): Boolean {
        nameEditor = getChild<LineEditor>(TXT_DAY_NAME)
        cancelButton = getChild<Button>(BTN_CANCEL)
        addFrameButton = getChild<Button>(BTN_ADDFRAME)
        deleteFrameButton = getChild<Button>(BTN_DELFRAME)
        timeSlider = getChild<MultiSliderCtrl>(SLDR_TIME)
        framesSlider = getChild<MultiSliderCtrl>(SLDR_KEYFRAMES)
        skyTabLayoutContainer = getChild<View>(VIEW_SKY_SETTINGS)
        waterTabLayoutContainer = getChild<View>(VIEW_WATER_SETTINGS)
        currentTimeLabel = getChild<TextBox>(LBL_CURRENT_TIME)
        importButton = getChild<Button>(BTN_IMPORT)
        loadFrame = getChild<Button>(BTN_LOADFRAME)
        cloneTrack = getChild<Button>(BTN_CLONETRACK)
        loadTrack = getChild<Button>(BTN_LOADTRACK)
        clearTrack = getChild<Button>(BTN_CLEARTRACK)

        flyoutControl = FlyoutComboBtnCtrl(this, BTN_SAVE, BTN_FLYOUT, XML_FLYOUTMENU_FILE, false)
        flyoutControl?.setAction { ctrl, _ -> onButtonApply(ctrl) }

        nameEditor?.setKeystrokeCallback { onNameKeystroke() }
        cancelButton?.setCommitCallback { onClickCloseBtn() }
        timeSlider?.setCommitCallback { onTimeSliderCallback() }
        addFrameButton?.setCommitCallback { onAddFrame() }
        deleteFrameButton?.setCommitCallback { onRemoveFrame() }
        importButton?.setCommitCallback { onButtonImport() }
        loadFrame?.setCommitCallback { onButtonLoadFrame() }
        cloneTrack?.setCommitCallback { onCloneTrack() }
        loadTrack?.setCommitCallback { onLoadTrack() }
        clearTrack?.setCommitCallback { onClearTrack() }
        framesSlider?.setCommitCallback { _, data -> onFrameSliderCallback(data) }
        framesSlider?.setDoubleClickCallback { _, x, y, mask -> onFrameSliderDoubleClick(x, y, mask) }
        framesSlider?.setMouseDownCallback { _, x, y, mask -> onFrameSliderMouseDown(x, y, mask) }
        framesSlider?.setMouseUpCallback { _, x, y, mask -> onFrameSliderMouseUp(x, y, mask) }

        timeSlider?.addSlider(0f)

        skyTabLayoutContainer?.getChild<TabContainer>("sky_tabs")?.let { tab ->
            for (idx in 0 until tab.tabCount) {
                (tab.getPanelByIndex(idx) as? SettingsEditPanel)
                    ?.setOnDirtyFlagChanged { _, v -> onPanelDirtyFlagChanged(v) }
            }
        }
        waterTabLayoutContainer?.getChild<TabContainer>("water_tabs")?.let { tab ->
            for (idx in 0 until tab.tabCount) {
                (tab.getPanelByIndex(idx) as? SettingsEditPanel)
                    ?.setOnDirtyFlagChanged { _, v -> onPanelDirtyFlagChanged(v) }
            }
        }
        return true
    }

    override fun onOpen(key: LLSD) {
        if (editDay == null) {
            Environment.instance.saveBeaconsState()
        }
        editDay = null
        editContext = EditContext.UNKNOWN

        if (key.has(KEY_EDIT_CONTEXT)) {
            editContext = when (key[KEY_EDIT_CONTEXT].asString()) {
                VALUE_CONTEXT_INVENTORY -> EditContext.INVENTORY
                VALUE_CONTEXT_PARCEL -> EditContext.PARCEL
                VALUE_CONTEXT_REGION -> EditContext.REGION
                else -> EditContext.UNKNOWN
            }
        }

        if (key.has(KEY_INVENTORY_ID)) {
            loadInventoryItem(key[KEY_INVENTORY_ID].asUUID())
        } else {
            canSave = true
            canCopy = true
            canMod = true
            canTrans = true
            setEditDefaultDayCycle()
        }

        dayLength = 0L
        if (key.has(KEY_DAY_LENGTH)) {
            dayLength = key[KEY_DAY_LENGTH].asLong()
        }

        currentTimeLabel?.setTextArg("[PRCNT]", "0")

        val maxElm = 5
        if (dayLength != 0L) {
            val formattedLabel = getString("time_label")
            for (i in 0 until maxElm) {
                val total = (dayLength / (maxElm - 1)) * i
                val hrs = total / 3600
                val minutes = (total % 3600) / 60
                val label = formattedLabel
                    .replace("[HH]", "$hrs")
                    .replace("[MM]", "${abs(minutes.toInt())}")
                getChild<TextBox>("p$i")?.setTextArg("[DSC]", label)
            }
            val hrs = dayLength / 3600
            val minutes = (dayLength % 3600) / 60
            val label = formattedLabel
                .replace("[HH]", "$hrs")
                .replace("[MM]", "${abs(minutes.toInt())}")
            currentTimeLabel?.setTextArg("[DSC]", label)
        } else {
            for (i in 0 until maxElm) {
                getChild<TextBox>("p$i")?.setTextArg("[DSC]", "")
            }
            currentTimeLabel?.setTextArg("[DSC]", "")
        }

        val labelRect = getChild<TextBox>("p0")?.rect ?: Rect()
        val sliderWidth = framesSlider?.rect?.width?.toFloat() ?: 0f
        for (i in 1 until maxElm) {
            val pcntLabel = getChild<TextBox>("p$i") ?: continue
            val newRect = pcntLabel.rect.copy()
            newRect.left = labelRect.left + (sliderWidth * i / (maxElm - 1)).toInt() - (pcntLabel.textPixelWidth / 2)
            pcntLabel.rect = newRect
        }

        val formattedLabel = getString("sky_track_label")
        val altitudes = Environment.instance.regionAltitudes
        val extendedEnv = Environment.instance.isExtendedEnvironmentEnabled
        val useAltitudes = extendedEnv && altitudes.isNotEmpty() &&
            (editContext == EditContext.PARCEL || editContext == EditContext.REGION)

        for (idx in 1..3) {
            val altStr = if (useAltitudes) "${altitudes[idx]}m" else "${idx + 1}"
            val label = formattedLabel.replace("[ALT]", altStr)
            getChild<Button>(TRACK_TABS[idx + 1])?.label = label
        }

        for (i in 2 until SettingsDay.TRACK_MAX) {
            getChild<Button>(TRACK_TABS[i])?.isEnabled = extendedEnv
        }

        when (editContext) {
            EditContext.INVENTORY -> {
                flyoutControl?.setShownBtnEnabled(true)
                flyoutControl?.setSelectedItem(ACTION_SAVE)
            }
            EditContext.PARCEL, EditContext.REGION -> {
                val commitStr = if (editContext == EditContext.PARCEL) STR_COMMIT_PARCEL else STR_COMMIT_REGION
                flyoutControl?.setMenuItemLabel(ACTION_COMMIT, getString(commitStr))
                flyoutControl?.setShownBtnEnabled(true)
                flyoutControl?.setSelectedItem(ACTION_COMMIT)
            }
            else -> flyoutControl?.setShownBtnEnabled(false)
        }
    }

    override fun onClose(appQuitting: Boolean) {
        doCloseInventoryFloater(appQuitting)
        doCloseTrackFloater(appQuitting)
        stopPlay()
        Environment.instance.revertBeaconsState()
        if (!appQuitting) {
            Environment.instance.setSelectedEnvironment(Environment.ENV_LOCAL, Environment.TRANSITION_FAST)
            Environment.instance.clearEnvironment(Environment.ENV_EDIT)
            editDay = null
        }
    }

    override fun onVisibilityChange(newVisibility: Boolean) {}

    fun setEditCommitSignal(cb: (SettingsDay) -> Unit) {
        commitSignal += cb
    }

    override fun refresh() {
        editDay?.let { day ->
            nameEditor?.text = day.name
            nameEditor?.isEnabled = canMod && canSave && inventoryId != UUID(0, 0)
        }

        val isInventoryAvail = canUseInventory()
        val showCommit = editContext == EditContext.PARCEL || editContext == EditContext.REGION
        val showApply = editContext == EditContext.INVENTORY

        if (showCommit) {
            val commitText = getString(if (editContext == EditContext.PARCEL) STR_COMMIT_PARCEL else STR_COMMIT_REGION)
            flyoutControl?.setMenuItemLabel(ACTION_COMMIT, commitText)
        }

        flyoutControl?.setMenuItemVisible(ACTION_COMMIT, showCommit)
        flyoutControl?.setMenuItemVisible(ACTION_SAVE, isInventoryAvail)
        flyoutControl?.setMenuItemVisible(ACTION_SAVEAS, isInventoryAvail)
        flyoutControl?.setMenuItemVisible(ACTION_APPLY_LOCAL, true)
        flyoutControl?.setMenuItemVisible(ACTION_APPLY_PARCEL, showApply)
        flyoutControl?.setMenuItemVisible(ACTION_APPLY_REGION, showApply)

        flyoutControl?.setMenuItemEnabled(ACTION_COMMIT, showCommit && commitSignal.isNotEmpty())
        flyoutControl?.setMenuItemEnabled(ACTION_SAVE, isInventoryAvail && canMod && canSave && inventoryId != UUID(0, 0))
        flyoutControl?.setMenuItemEnabled(ACTION_SAVEAS, isInventoryAvail && canCopy && canSave)
        flyoutControl?.setMenuItemEnabled(ACTION_APPLY_LOCAL, true)
        flyoutControl?.setMenuItemEnabled(ACTION_APPLY_PARCEL, canApplyParcel() && showApply)
        flyoutControl?.setMenuItemEnabled(ACTION_APPLY_REGION, canApplyRegion() && showApply)

        importButton?.isEnabled = canMod
        super.refresh()
    }

    override fun setEditSettingsAndUpdate(settings: SettingsBase) {
        val day = settings as? SettingsDay ?: return
        setEditDayCycle(day)
        showHdrNotification(day)
    }

    fun setEditDayCycle(pday: SettingsDay) {
        expectingAssetId = UUID(0, 0)
        editDay = pday.buildDeepCloneAndUncompress()

        val day = editDay ?: return
        if (day.isTrackEmpty(SettingsDay.TRACK_WATER)) {
            day.setWaterAtKeyframe(SettingsVOWater.buildDefaultWater(), 0.5f)
        }
        if (day.isTrackEmpty(SettingsDay.TRACK_GROUND_LEVEL)) {
            day.setSkyAtKeyframe(SettingsVOSky.buildDefaultSky(), 0.5f, SettingsDay.TRACK_GROUND_LEVEL)
        }

        canSave = !pday.getFlag(SettingsBase.FLAG_NOSAVE)
        canCopy = !pday.getFlag(SettingsBase.FLAG_NOCOPY) && canSave
        canMod = !pday.getFlag(SettingsBase.FLAG_NOMOD) && canSave
        canTrans = !pday.getFlag(SettingsBase.FLAG_NOTRANS) && canSave

        updateEditEnvironment()
        Environment.instance.setSelectedEnvironment(Environment.ENV_EDIT, Environment.TRANSITION_INSTANT)
        synchronizeTabs()
        updateTabs()
        refresh()
    }

    fun setEditDefaultDayCycle() {
        inventoryItem = null
        inventoryId = UUID(0, 0)
        expectingAssetId = SettingsDay.getDefaultAssetId()
        SettingsVOBase.getSettingsAsset(SettingsDay.getDefaultAssetId()) { assetId, settings, status, _ ->
            onAssetLoaded(assetId, settings, status)
        }
    }

    fun getEditName(): String = editDay?.name ?: "new"

    fun setEditName(name: String) {
        editDay?.name = name
        getChild<LineEditor>(TXT_DAY_NAME)?.text = name
    }

    fun getEditingAssetId(): UUID = editDay?.assetId ?: UUID(0, 0)
    fun getEditingInventoryId(): UUID = inventoryId

    override fun getEditSettings(): SettingsBase? = editDay

    override fun handleKeyUp(key: Key, mask: Mask, calledFromParent: Boolean): Boolean {
        if (editDay == null) {
            shiftCopyEnabled = false
        } else if (mask == MASK_SHIFT && shiftCopyEnabled) {
            shiftCopyEnabled = false
            val curSlider = framesSlider?.getCurSlider() ?: ""
            if (curSlider.isNotEmpty()) {
                val sliderPos = framesSlider?.getCurSliderValue() ?: 0f
                val it = sliderKeyMap[curSlider]
                if (it != null) {
                    if (editDay?.moveTrackKeyframe(currentTrack, it.frame, sliderPos) == true) {
                        sliderKeyMap[curSlider] = it.copy(frame = sliderPos)
                    } else {
                        framesSlider?.setCurSliderValue(it.frame)
                    }
                }
            }
        }
        return super.handleKeyUp(key, mask, calledFromParent)
    }

    private fun getCurrentFrame(): Float = timeSlider?.getCurSliderValue() ?: 0f

    private fun onButtonApply(ctrl: UiCtrl) {
        val ctrlAction = ctrl.name
        val day = editDay ?: run {
            NotificationsUtil.add("EnvironmentApplyFailed")
            closeFloater()
            return
        }
        val dayclone = day.buildClone() ?: return

        for (i in 0..SettingsDay.TRACK_MAX) {
            val dayTrack = dayclone.getCycleTrack(i)
            var frameNum = 0
            for ((pos, setting) in dayTrack) {
                frameNum++
                var desc = ""
                var isLocal = false
                if (i == SettingsDay.TRACK_WATER) {
                    val water = setting as? SettingsWater
                    if (water != null) {
                        when {
                            LocalBitmapMgr.instance.isLocal(water.normalMapId) -> {
                                desc = Trans.getString("EnvironmentNormalMap"); isLocal = true
                            }
                            LocalBitmapMgr.instance.isLocal(water.transparentTextureId) -> {
                                desc = Trans.getString("EnvironmentTransparent"); isLocal = true
                            }
                        }
                    }
                } else {
                    val sky = setting as? SettingsSky
                    if (sky != null) {
                        when {
                            LocalBitmapMgr.instance.isLocal(sky.sunTextureId) -> {
                                desc = Trans.getString("EnvironmentSun"); isLocal = true
                            }
                            LocalBitmapMgr.instance.isLocal(sky.moonTextureId) -> {
                                desc = Trans.getString("EnvironmentMoon"); isLocal = true
                            }
                            LocalBitmapMgr.instance.isLocal(sky.cloudNoiseTextureId) -> {
                                desc = Trans.getString("EnvironmentCloudNoise"); isLocal = true
                            }
                            LocalBitmapMgr.instance.isLocal(sky.bloomTextureId) -> {
                                desc = Trans.getString("EnvironmentBloom"); isLocal = true
                            }
                        }
                    }
                }
                if (isLocal) {
                    val button = getChild<Button>(TRACK_TABS[i])
                    val args = LLSD().apply {
                        put("TRACK", button?.currentLabel ?: "")
                        put("FRAME", pos * 100)
                        put("FIELD", desc)
                        put("FRAMENO", frameNum)
                    }
                    NotificationsUtil.add("WLLocalTextureDayBlock", args)
                    return
                }
            }
        }

        when (ctrlAction) {
            ACTION_SAVE -> {
                doApplyUpdateInventory(dayclone)
                clearDirtyFlag()
            }
            ACTION_SAVEAS -> {
                val args = LLSD().apply { put("DESC", dayclone.name) }
                NotificationsUtil.add("SaveSettingAs", args, LLSD()) { notif, resp ->
                    onSaveAsCommit(notif, resp, dayclone)
                }
            }
            ACTION_APPLY_LOCAL, ACTION_APPLY_PARCEL, ACTION_APPLY_REGION ->
                doApplyEnvironment(ctrlAction, dayclone)
            ACTION_COMMIT -> doApplyCommit(dayclone)
        }
    }

    private fun onButtonLoadFrame() {
        val type = if (currentTrack == SettingsDay.TRACK_WATER.toUInt())
            SettingsType.ST_WATER else SettingsType.ST_SKY
        doOpenInventoryFloater(type, UUID(0, 0))
    }

    private fun onAddFrame() {
        val day = editDay ?: return
        val frame = timeSlider?.getCurSliderValue() ?: return
        if (day.getSettingsNearKeyframe(frame, currentTrack, SettingsDay.DEFAULT_FRAME_SLOP_FACTOR).second != null) return
        if (framesSlider?.canAddSliders() == false) return

        val setting: SettingsBase = if (currentTrack == SettingsDay.TRACK_WATER.toUInt()) {
            val water = scratchWater.buildClone()
            day.setWaterAtKeyframe(water, frame)
            water
        } else {
            val sky = scratchSky.buildClone()
            day.setSkyAtKeyframe(sky, frame, currentTrack)
            sky
        }
        setDirtyFlag()
        addSliderFrame(frame, setting)
        updateTabs()
    }

    private fun onRemoveFrame() {
        val sldrKey = framesSlider?.getCurSlider() ?: ""
        if (sldrKey.isEmpty()) return
        setDirtyFlag()
        removeCurrentSliderFrame()
        updateTabs()
    }

    private fun onCloneTrack() {
        val day = editDay ?: return
        val altitudes = Environment.instance.regionAltitudes
        val useAltitudes = altitudes.isNotEmpty() &&
            (editContext == EditContext.PARCEL || editContext == EditContext.REGION)

        val args = LLSD.emptyArray()
        var populatedCounter = 0

        for (i in 1u until SettingsDay.TRACK_MAX.toUInt()) {
            val populated = !day.isTrackEmpty(i) && i != currentTrack
            if (populated) populatedCounter++
            val track = LLSD().apply {
                put("id", i.toInt())
                put("enabled", populated)
                if (useAltitudes) put("altitude", altitudes[(i - 1u).toInt()])
            }
            args.append(track)
        }

        if (populatedCounter > 0) {
            doOpenTrackFloater(args)
        }
    }

    private fun onLoadTrack() {
        var curItemId = inventoryId
        val curEdit = currentEdit
        if (curEdit != null && curItemId != UUID(0, 0)) {
            curItemId = FloaterSettingsPicker.findItemId(curEdit.assetId, false, false)
        }
        doOpenInventoryFloater(SettingsType.ST_DAYCYCLE, curItemId)
    }

    private fun onClearTrack() {
        val day = editDay ?: return
        if (currentTrack > 1u) {
            day.getCycleTrack(currentTrack).clear()
        } else {
            val track = day.getCycleTrack(currentTrack)
            val entries = track.entries.toList()
            if (entries.size > 1) {
                entries.drop(1).forEach { (k, _) -> track.remove(k) }
            }
        }
        updateEditEnvironment()
        Environment.instance.setSelectedEnvironment(Environment.ENV_EDIT, Environment.TRANSITION_INSTANT)
        synchronizeTabs()
        updateTabs()
        refresh()
    }

    private fun onNameKeystroke() {
        val day = editDay ?: return
        day.name = nameEditor?.text ?: ""
    }

    private fun onTrackSelectionCallback(userData: LLSD) {
        val trackIndex = userData.asInteger().toUInt()
        selectTrack(trackIndex)
    }

    private fun onPlayActionCallback(userData: LLSD) {
        val action = userData.asString()
        val frame = timeSlider?.getCurSliderValue() ?: 0f
        when (action) {
            ACTION_PLAY -> startPlay()
            ACTION_PAUSE -> stopPlay()
            else -> if (sliderKeyMap.isNotEmpty()) {
                val increment = timeSlider?.increment ?: 0f
                val newFrame = when (action) {
                    ACTION_FORWARD -> editDay?.getUpperBoundFrame(currentTrack, frame + increment / 2) ?: frame
                    ACTION_BACK -> editDay?.getLowerBoundFrame(currentTrack, frame - increment / 2) ?: frame
                    else -> frame
                }
                selectFrame(newFrame, 0f)
                stopPlay()
            }
        }
    }

    private fun onTimeSliderCallback() {
        stopPlay()
        selectFrame(timeSlider?.getCurSliderValue() ?: 0f, SettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
    }

    private fun onFrameSliderCallback(data: LLSD) {
        val curSlider = framesSlider?.getCurSlider() ?: ""
        val day = editDay ?: return
        if (curSlider.isEmpty()) return

        val sliderPos = framesSlider?.getCurSliderValue() ?: 0f
        val it = sliderKeyMap[curSlider] ?: return

        if (Keyboard.currentMask(true) == MASK_SHIFT && shiftCopyEnabled && canMod) {
            if (day.getSettingsNearKeyframe(sliderPos, currentTrack, SettingsDay.DEFAULT_FRAME_SLOP_FACTOR).second == null) {
                val newSettings: SettingsBase = if (currentTrack == SettingsDay.TRACK_WATER.toUInt()) {
                    val water = (it.settings as SettingsWater).buildClone()
                    day.setWaterAtKeyframe(water, sliderPos)
                    water
                } else {
                    val sky = (it.settings as SettingsSky).buildClone()
                    day.setSkyAtKeyframe(sky, sliderPos, currentTrack)
                    sky
                }
                val oldFrame = it.frame
                sliderKeyMap[curSlider] = it.copy(frame = sliderPos)
                addSliderFrame(oldFrame, newSettings, updateUi = false)
                framesSlider?.setCurSlider(curSlider)
                shiftCopyEnabled = false
                setDirtyFlag()
            }
        } else {
            val nearestIncrement = framesSlider?.getNearestIncrement(it.frame) ?: it.frame
            if (abs(nearestIncrement - sliderPos) < F_APPROXIMATELY_ZERO) {
                framesSlider?.setCurSliderValue(it.frame)
            } else if (day.moveTrackKeyframe(currentTrack, it.frame, sliderPos) && canMod) {
                sliderKeyMap[curSlider] = it.copy(frame = sliderPos)
                setDirtyFlag()
            } else {
                framesSlider?.setCurSliderValue(it.frame)
            }
            shiftCopyEnabled = false
        }
    }

    private fun onFrameSliderDoubleClick(x: Int, y: Int, mask: Mask) {
        stopPlay()
        onAddFrame()
    }

    private fun onFrameSliderMouseDown(x: Int, y: Int, mask: Mask) {
        stopPlay()
        val sliderPos = framesSlider?.getSliderValueFromPos(x, y) ?: 0f
        val sliderName = framesSlider?.getCurSlider() ?: ""

        shiftCopyEnabled = sliderName.isNotEmpty() && Keyboard.currentMask(true) == MASK_SHIFT

        if (sliderName.isNotEmpty()) {
            val thumbRect = framesSlider?.getSliderThumbRect(sliderName)
            if (thumbRect != null && (x >= thumbRect.right || x <= thumbRect.left)) {
                framesSlider?.resetCurSlider()
            }
        }
        timeSlider?.setCurSliderValue(sliderPos)
        updateTabs()
        Environment.instance.updateEnvironment(Environment.TRANSITION_INSTANT)
    }

    private fun onFrameSliderMouseUp(x: Int, y: Int, mask: Mask) {
        val sliderPos = framesSlider?.getSliderValueFromPos(x, y) ?: 0f
        timeSlider?.setCurSliderValue(sliderPos)
        selectFrame(sliderPos, SettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
    }

    private fun cloneTrackFrom(sourceIndex: UInt, destIndex: UInt) {
        cloneTrackFrom(editDay ?: return, sourceIndex, destIndex)
    }

    private fun cloneTrackFrom(sourceDay: SettingsDay, sourceIndex: UInt, destIndex: UInt) {
        val waterTrack = SettingsDay.TRACK_WATER.toUInt()
        if ((sourceIndex == waterTrack || destIndex == waterTrack) && sourceIndex != destIndex) {
            val args = LLSD().apply {
                put("TRACK1", getChild<Button>(TRACK_TABS[sourceIndex.toInt()])?.currentLabel ?: "")
                put("TRACK2", getChild<Button>(TRACK_TABS[destIndex.toInt()])?.currentLabel ?: "")
            }
            NotificationsUtil.add("TrackLoadMismatch", args)
            return
        }

        val day = editDay ?: return
        val backupTrack = day.getCycleTrack(destIndex).toMap()
        day.clearCycleTrack(destIndex)
        val sourceTrack = sourceDay.getCycleTrack(sourceIndex)
        var addCount = 0

        for ((pos, frame) in sourceTrack) {
            val clone = frame.buildDerivedClone()
            if (clone != null) {
                addCount++
                day.setSettingsAtKeyframe(clone, pos, destIndex)
            }
        }

        if (addCount == 0) {
            day.replaceCycleTrack(destIndex, backupTrack)
            val args = LLSD().apply {
                put("TRACK", getChild<Button>(TRACK_TABS[destIndex.toInt()])?.currentLabel ?: "")
            }
            NotificationsUtil.add("TrackLoadFailed", args)
        }
        setDirtyFlag()
        updateSlider()
        updateTabs()
        updateButtons()
    }

    private fun selectTrack(trackIndex: UInt, force: Boolean = false) {
        if (trackIndex < SettingsDay.TRACK_MAX.toUInt()) currentTrack = trackIndex

        val button = getChild<Button>(TRACK_TABS[currentTrack.toInt()])
        if (button?.toggleState == true && !force) return

        for (i in 0u until SettingsDay.TRACK_MAX.toUInt()) {
            getChild<Button>(TRACK_TABS[i.toInt()])?.toggleState = i == currentTrack
        }

        val showWater = currentTrack == SettingsDay.TRACK_WATER.toUInt()
        skyTabLayoutContainer?.isVisible = !showWater
        waterTabLayoutContainer?.isVisible = showWater

        updateSlider()
        updateLabels()
    }

    private fun selectFrame(frame: Float, slopFactor: Float) {
        framesSlider?.resetCurSlider()
        val iter = sliderKeyMap.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val keyframe = entry.value.frame
            val frameDif = abs(keyframe - frame)
            if (frameDif <= slopFactor) {
                val nextEntry = sliderKeyMap.entries.firstOrNull { it.key > entry.key }
                if (frameDif != 0f && nextEntry != null) {
                    if (abs(nextEntry.value.frame - frame) < frameDif) {
                        framesSlider?.setCurSlider(nextEntry.key)
                        break
                    }
                }
                framesSlider?.setCurSlider(entry.key)
                break
            }
        }
        timeSlider?.setCurSliderValue(frame)
        updateTabs()
    }

    private fun clearTabs() {
        if (currentTrack == SettingsDay.TRACK_WATER.toUInt()) {
            updateWaterTabs(null)
        } else {
            updateSkyTabs(null)
        }
        updateButtons()
        updateTimeAndLabel()
    }

    private fun updateTabs() {
        reblendSettings()
        synchronizeTabs()
        updateButtons()
        updateTimeAndLabel()
    }

    private fun updateWaterTabs(water: SettingsWater?) {
        waterTabLayoutContainer?.getChild<View>(TABS_WATER)
            ?.findChildView("water_panel")
            ?.let { it as? PanelSettingsWaterMainTab }
            ?.setWater(water)
    }

    private fun updateSkyTabs(sky: SettingsSky?) {
        skyTabLayoutContainer?.getChild<TabContainer>(TABS_SKYS)?.let { tab ->
            (tab.findChildView("atmosphere_panel") as? PanelSettingsSky)?.setSky(sky)
            (tab.findChildView("clouds_panel") as? PanelSettingsSky)?.setSky(sky)
            (tab.findChildView("moon_panel") as? PanelSettingsSky)?.setSky(sky)
        }
    }

    private fun updateLabels() {
        val labelArg = if (currentTrack == SettingsDay.TRACK_WATER.toUInt()) "water_label" else "sky_label"
        addFrameButton?.setLabelArg("[FRAME]", getString(labelArg))
        deleteFrameButton?.setLabelArg("[FRAME]", getString(labelArg))
        loadFrame?.setLabelArg("[FRAME]", getString(labelArg))
    }

    private fun updateButtons() {
        val day = editDay
        val canManipulate = day != null && !isPlaying && canMod
        var canClone = false
        var canClearTrack = false

        if (canManipulate && day != null) {
            if (currentTrack != 0u) {
                for (track in 1u until SettingsDay.TRACK_MAX.toUInt()) {
                    if (track == currentTrack) continue
                    canClone = canClone || !day.getCycleTrack(track).isEmpty()
                }
            }
            canClearTrack = if (currentTrack > 1u) {
                !day.getCycleTrack(currentTrack).isEmpty()
            } else {
                day.getCycleTrack(currentTrack).size > 1
            }
        }

        cloneTrack?.isEnabled = canClone
        loadTrack?.isEnabled = canManipulate
        clearTrack?.isEnabled = canClearTrack
        addFrameButton?.isEnabled = canManipulate && isAddingFrameAllowed()
        deleteFrameButton?.isEnabled = canManipulate && isRemovingFrameAllowed()
        loadFrame?.isEnabled = canManipulate

        val enablePlay = editDay != null
        setChildEnabled(BTN_PLAY, enablePlay)
        setChildEnabled(BTN_SKIP_BACK, enablePlay)
        setChildEnabled(BTN_SKIP_FORWARD, enablePlay)

        val extendedEnv = Environment.instance.isExtendedEnvironmentEnabled
        for (track in 0u until SettingsDay.TRACK_MAX.toUInt()) {
            val btn = getChild<Button>(TRACK_TABS[track.toInt()])
            btn?.isEnabled = extendedEnv
            btn?.toggleState = track == currentTrack
        }
    }

    private fun updateSlider() {
        val framePosition = timeSlider?.getCurSliderValue() ?: 0f
        framesSlider?.clear()
        sliderKeyMap.clear()

        val day = editDay ?: return
        val track = day.getCycleTrack(currentTrack)
        for ((pos, setting) in track) {
            addSliderFrame(pos, setting, updateUi = false)
        }

        if (sliderKeyMap.isNotEmpty()) {
            lastFrameSlider = framesSlider?.getCurSlider() ?: ""
        } else {
            clearTabs()
            lastFrameSlider = ""
        }
        selectFrame(framePosition, SettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
    }

    private fun updateTimeAndLabel() {
        val time = timeSlider?.getCurSliderValue() ?: 0f
        currentTimeLabel?.setTextArg("[PRCNT]", "%.0f".format(time * 100))
        if (dayLength != 0L) {
            val total = (dayLength * time).toLong()
            val hrs = total / 3600
            val minutes = (total % 3600) / 60
            val label = getString("time_label")
                .replace("[HH]", "$hrs")
                .replace("[MM]", "${abs(minutes.toInt())}")
            currentTimeLabel?.setTextArg("[DSC]", label)
        } else {
            currentTimeLabel?.setTextArg("[DSC]", "")
        }
    }

    private fun addSliderFrame(frame: Float, setting: SettingsBase?, updateUi: Boolean = true) {
        val newSlider = framesSlider?.addSlider(frame) ?: return
        if (newSlider.isNotEmpty()) {
            sliderKeyMap[newSlider] = FrameData(frame, setting)
            if (updateUi) {
                lastFrameSlider = newSlider
                timeSlider?.setCurSliderValue(frame)
                updateTabs()
            }
        }
    }

    private fun removeCurrentSliderFrame() {
        val sldr = framesSlider?.getCurSlider() ?: ""
        if (sldr.isEmpty()) return
        framesSlider?.deleteCurSlider()
        val entry = sliderKeyMap.remove(sldr)
        if (entry != null) {
            editDay?.removeTrackKeyframe(currentTrack, entry.frame)
        }
        lastFrameSlider = framesSlider?.getCurSlider() ?: ""
        timeSlider?.setCurSliderValue(framesSlider?.getCurSliderValue() ?: 0f)
        updateTabs()
    }

    private fun removeSliderFrame(frame: Float) {
        val key = sliderKeyMap.entries.firstOrNull {
            abs(it.value.frame - frame) < SettingsDay.DEFAULT_FRAME_SLOP_FACTOR
        }?.key ?: return
        framesSlider?.deleteSlider(key)
        sliderKeyMap.remove(key)
    }

    override fun updateEditEnvironment() {
        val day = editDay ?: return
        val skyTrack = if (currentTrack != 0u) currentTrack else 1u
        skyBlender = TrackBlenderLoopingManual(scratchSky, day, skyTrack)
        waterBlender = TrackBlenderLoopingManual(scratchWater, day, SettingsDay.TRACK_WATER.toUInt())

        if (Environment.instance.isExtendedEnvironmentEnabled) {
            selectTrack(SettingsDay.TRACK_MAX.toUInt(), force = true)
        } else {
            selectTrack(1u, force = true)
        }
        reblendSettings()
        Environment.instance.setEnvironment(Environment.ENV_EDIT, editSky, editWater)
        Environment.instance.updateEnvironment(Environment.TRANSITION_INSTANT)
    }

    private fun synchronizeTabs() {
        val curSlider = framesSlider?.getCurSlider() ?: ""
        var canEdit = false

        var settingW: SettingsWater? = null
        val waterTabs = waterTabLayoutContainer?.getChild<TabContainer>(TABS_WATER)
        if (currentTrack == SettingsDay.TRACK_WATER.toUInt()) {
            if (editDay != null && curSlider.isNotEmpty()) {
                canEdit = !isPlaying
                settingW = sliderKeyMap[curSlider]?.settings as? SettingsWater
            }
            currentEdit = settingW
            if (settingW == null) {
                canEdit = false
                settingW = scratchWater
            }
            getChild<UiCtrl>(ICN_LOCK_EDIT)?.isVisible = !canEdit
        } else {
            settingW = scratchWater
        }
        editWater = settingW
        if (waterTabs != null) setTabsData(waterTabs, settingW, canEdit)

        canEdit = false
        var settingS: SettingsSky? = null
        val skyTabs = skyTabLayoutContainer?.getChild<TabContainer>(TABS_SKYS)
        if (currentTrack != SettingsDay.TRACK_WATER.toUInt()) {
            if (editDay != null && curSlider.isNotEmpty()) {
                canEdit = !isPlaying
                settingS = sliderKeyMap[curSlider]?.settings as? SettingsSky
            }
            currentEdit = settingS
            if (settingS == null) {
                canEdit = false
                settingS = scratchSky
            }
            getChild<UiCtrl>(ICN_LOCK_EDIT)?.isVisible = !canEdit
        } else {
            settingS = scratchSky
        }
        editSky = settingS
        doCloseInventoryFloater()
        doCloseTrackFloater()
        if (skyTabs != null) setTabsData(skyTabs, settingS, canEdit)
        Environment.instance.setEnvironment(Environment.ENV_EDIT, editSky, editWater)
        Environment.instance.updateEnvironment(Environment.TRANSITION_INSTANT)
    }

    private fun setTabsData(tabContainer: TabContainer, settings: SettingsBase?, editable: Boolean) {
        for (idx in 0 until tabContainer.tabCount) {
            (tabContainer.getPanelByIndex(idx) as? SettingsEditPanel)?.let { panel ->
                panel.setCanChangeSettings(editable && canMod)
                panel.setSettings(settings)
            }
        }
    }

    private fun reblendSettings() {
        val position = timeSlider?.getCurSliderValue()?.toDouble() ?: 0.0
        skyBlender?.let { blender ->
            if (blender.track != currentTrack && currentTrack != SettingsDay.TRACK_WATER.toUInt()) {
                blender.switchTrack(currentTrack, position.toFloat())
            } else {
                blender.setPosition(position.toFloat())
            }
        }
        waterBlender?.setPosition(position.toFloat())
    }

    private fun doApplyCommit(day: SettingsDay) {
        if (commitSignal.isNotEmpty()) {
            for (cb in commitSignal) cb(day)
            closeFloater()
        }
    }

    private fun isRemovingFrameAllowed(): Boolean {
        if (framesSlider?.getCurSlider().isNullOrEmpty()) return false
        return if (currentTrack <= SettingsDay.TRACK_GROUND_LEVEL.toUInt()) {
            sliderKeyMap.size > 1
        } else {
            sliderKeyMap.isNotEmpty()
        }
    }

    private fun isAddingFrameAllowed(): Boolean {
        if (!framesSlider?.getCurSlider().isNullOrEmpty() || editDay == null) return false
        val frame = timeSlider?.getCurSliderValue() ?: 0f
        if (editDay?.getSettingsNearKeyframe(frame, currentTrack, SettingsDay.DEFAULT_FRAME_SLOP_FACTOR)?.second != null) return false
        return framesSlider?.canAddSliders() == true
    }

    override fun doImportFromDisk() {
        FilePickerReplyThread.startPicker(
            callback = { filenames -> loadSettingFromFile(filenames) },
            filter = FilePicker.FFLOAD_XML,
            multi = false
        )
    }

    private fun loadSettingFromFile(filenames: List<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        val messages = LLSD()
        val legacyDay = Environment.createDayCycleFromLegacyPreset(filename, messages) ?: run {
            NotificationsUtil.add("WLImportFail", messages)
            return
        }
        loadInventoryItem(UUID(0, 0))
        currentTrack = 1u
        setDirtyFlag()
        setEditDayCycle(legacyDay)
    }

    private fun startPlay() {
        doCloseInventoryFloater()
        doCloseTrackFloater()
        isPlaying = true
        framesSlider?.resetCurSlider()
        playTimer.reset()
        playTimer.start()
        IdleCallbacks.addFunction(::onIdlePlay, this)
        playStartFrame = timeSlider?.getCurSliderValue() ?: 0f
        getChild<View>("play_layout")?.isVisible = false
        getChild<View>("pause_layout")?.isVisible = true
    }

    private fun stopPlay() {
        if (!isPlaying) return
        isPlaying = false
        IdleCallbacks.deleteFunction(::onIdlePlay, this)
        playTimer.stop()
        val frame = timeSlider?.getCurSliderValue() ?: 0f
        selectFrame(frame, SettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
        getChild<View>("play_layout")?.isVisible = true
        getChild<View>("pause_layout")?.isVisible = false
    }

    private fun setDirtyFlag() { isDirty = true }

    override fun clearDirtyFlag() {
        isDirty = false
        skyTabLayoutContainer?.getChild<TabContainer>("sky_tabs")?.let { tab ->
            for (idx in 0 until tab.tabCount) {
                (tab.getPanelByIndex(idx) as? SettingsEditPanel)?.clearIsDirty()
            }
        }
        waterTabLayoutContainer?.getChild<TabContainer>("water_tabs")?.let { tab ->
            for (idx in 0 until tab.tabCount) {
                (tab.getPanelByIndex(idx) as? SettingsEditPanel)?.clearIsDirty()
            }
        }
    }

    private fun doOpenTrackFloater(args: LLSD) {
        var picker = trackFloater?.get() as? FloaterTrackPicker
        if (picker == null) {
            picker = FloaterTrackPicker(this)
            trackFloater = picker.handle
            picker.setCommitCallback { _, data -> onPickerCommitTrackId(data.asInteger().toUInt()) }
        }
        picker.showPicker(args)
    }

    private fun doCloseTrackFloater(quitting: Boolean = false) {
        trackFloater?.get()?.closeFloater(quitting)
    }

    override fun getSettingsPicker(): FloaterSettingsPicker {
        var picker = inventoryFloater?.get() as? FloaterSettingsPicker
        if (picker == null) {
            picker = FloaterSettingsPicker(this, UUID(0, 0))
            inventoryFloater = picker.handle
            picker.setCommitCallback { _, data ->
                onPickerCommitSetting(data["ItemId"].asUUID(), data["Track"].asInteger())
            }
        }
        return picker
    }

    private fun onPickerCommitTrackId(trackId: UInt) {
        cloneTrackFrom(trackId, currentTrack)
    }

    private fun doOpenInventoryFloater(type: SettingsType, currItem: UUID) {
        val picker = getSettingsPicker()
        picker.setSettingsFilter(type)
        picker.setSettingsItemId(currItem)
        picker.setTrackMode(
            if (type == SettingsType.ST_DAYCYCLE) {
                if (currentTrack == SettingsDay.TRACK_WATER.toUInt())
                    FloaterSettingsPicker.TRACK_WATER else FloaterSettingsPicker.TRACK_SKY
            } else {
                FloaterSettingsPicker.TRACK_NONE
            }
        )
        picker.openFloater()
        picker.setFocus(true)
    }

    private fun onPickerCommitSetting(itemId: UUID, track: Int) {
        val frame = timeSlider?.getCurSliderValue() ?: 0f
        val item = Inventory.instance.getItem(itemId) ?: return
        SettingsVOBase.getSettingsAsset(item.assetUuid) { assetId, settings, status, _ ->
            onAssetLoadedForInsertion(itemId, assetId, settings, status, track, currentTrack.toInt(), frame)
        }
    }

    private fun showHdrNotification(pday: SettingsDay) {
        val shouldAutoAdjust = SavedSettings.getBoolean("RenderSkyAutoAdjustLegacy") ?: false
        for (i in SettingsDay.TRACK_GROUND_LEVEL..SettingsDay.TRACK_MAX) {
            for ((_, setting) in pday.getCycleTrack(i.toUInt())) {
                val sky = setting as? SettingsSky
                if (shouldAutoAdjust && sky != null && sky.canAutoAdjust() && sky.getReflectionProbeAmbiance(true) != 0f) {
                    NotificationsUtil.add("AutoAdjustHDRSky")
                    return
                }
            }
        }
    }

    private fun onAssetLoadedForInsertion(
        itemId: UUID,
        assetId: UUID,
        settings: SettingsBase?,
        status: Int,
        sourceTrack: Int,
        destTrack: Int,
        frame: Float
    ) {
        if (settings == null || status != 0) return
        if (editDay == null) return

        val cb: () -> Unit = {
            val day = editDay ?: return@cb
            if (settings.settingsType == "daycycle") {
                val pday = settings as? SettingsDay ?: return@cb
                if (destTrack == SettingsDay.TRACK_WATER) {
                    cloneTrackFrom(pday, SettingsDay.TRACK_WATER.toUInt(), SettingsDay.TRACK_WATER.toUInt())
                } else {
                    cloneTrackFrom(pday, sourceTrack.toUInt(), destTrack.toUInt())
                }
            } else {
                if (framesSlider?.canAddSliders() == false) return@cb
                val nearest = day.getSettingsNearKeyframe(frame, destTrack.toUInt(), SettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
                if (nearest.first != SettingsDay.INVALID_TRACKPOS) {
                    day.removeTrackKeyframe(destTrack.toUInt(), nearest.first)
                    removeSliderFrame(nearest.first)
                }
                when (settings.settingsType) {
                    "sky" -> if (destTrack != SettingsDay.TRACK_WATER) {
                        day.setSettingsAtKeyframe(settings.buildDerivedClone(), frame, destTrack.toUInt())
                        addSliderFrame(frame, settings, updateUi = false)
                    }
                    "water" -> if (destTrack == SettingsDay.TRACK_WATER) {
                        day.setSettingsAtKeyframe(settings.buildDerivedClone(), frame, destTrack.toUInt())
                        addSliderFrame(frame, settings, updateUi = false)
                    }
                }
            }
            reblendSettings()
            synchronizeTabs()
        }

        val invItem = Inventory.instance.getItem(itemId)
        if (invItem != null &&
            (!invItem.permissions.allowOperationBy(PERM_TRANSFER, Agent.instance.id) ||
             !invItem.permissions.allowOperationBy(PERM_COPY, Agent.instance.id))
        ) {
            val noTransfer = inventoryItem?.let {
                !it.permissions.allowOperationBy(PERM_TRANSFER, Agent.instance.id)
            } ?: editDay?.getFlag(SettingsBase.FLAG_NOTRANS) ?: false

            if (!noTransfer) {
                NotificationsUtil.add("SettingsMakeNoTrans", LLSD(), LLSD()) { _, resp ->
                    if (NotificationsUtil.getSelectedOption(LLSD(), resp) == 0) {
                        canTrans = false
                        editDay?.setFlag(SettingsBase.FLAG_NOTRANS)
                        cb()
                    }
                }
                return
            }
        }

        cb()
    }
}
