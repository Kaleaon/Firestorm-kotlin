package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Constants (formerly anonymous-namespace statics in the .cpp)
// ---------------------------------------------------------------------------

private val TRACK_TABS = arrayOf(
    "water_track",
    "sky1_track",
    "sky2_track",
    "sky3_track",
    "sky4_track"
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

// ---------------------------------------------------------------------------
// Data holder for a single keyframe in the slider map
// ---------------------------------------------------------------------------

data class FrameData(
    var mFrame: Float = 0f,
    val pSettings: LLSettingsBase? = null
)

// ---------------------------------------------------------------------------
// LLFloaterEditExtDayCycle
// ---------------------------------------------------------------------------

class LLFloaterEditExtDayCycle(key: LLSD) : LLFloaterEditEnvironmentBase(key) {

    companion object {
        const val KEY_EDIT_CONTEXT = "edit_context"
        const val KEY_DAY_LENGTH = "day_length"

        const val VALUE_CONTEXT_INVENTORY = "inventory"
        const val VALUE_CONTEXT_PARCEL = "parcel"
        const val VALUE_CONTEXT_REGION = "region"

        fun onIdlePlay(userData: Any?) {
            val self = userData as? LLFloaterEditExtDayCycle ?: return
            if (!gDisconnected) {
                if (self.mSkyBlender == null || self.mWaterBlender == null) {
                    self.stopPlay()
                } else {
                    val prcntPlayed = self.mPlayTimer.getElapsedTimeF32() / DAY_CYCLE_PLAY_TIME_SECONDS
                    val newFrame = (self.mPlayStartFrame + prcntPlayed) % 1f
                    self.mTimeSlider?.setCurSliderValue(newFrame)
                    self.mSkyBlender?.setPosition(newFrame)
                    self.mWaterBlender?.setPosition(newFrame)
                    self.synchronizeTabs()
                    self.updateTimeAndLabel()
                    self.updateButtons()
                }
            }
        }
    }

    enum class EditContext {
        CONTEXT_UNKNOWN,
        CONTEXT_INVENTORY,
        CONTEXT_PARCEL,
        CONTEXT_REGION
    }

    // boost::signals2 → MutableList of listener lambdas
    val mCommitSignal: MutableList<(LLSettingsDay?) -> Unit> = mutableListOf()

    // connection token: a reference to the stored lambda so callers can disconnect
    fun setEditCommitSignal(cb: (LLSettingsDay?) -> Unit): (LLSettingsDay?) -> Unit {
        mCommitSignal.add(cb)
        return cb
    }

    fun removeEditCommitSignal(cb: (LLSettingsDay?) -> Unit) {
        mCommitSignal.remove(cb)
    }

    // State
    var mEditDay: LLSettingsDay? = null
    var mDayLength: Long = 0L          // seconds
    var mCurrentTrack: UInt = 1u
    var mLastFrameSlider: String = ""
    var mShiftCopyEnabled: Boolean = false

    // UI widget references (populated in postBuild)
    var mNameEditor: LLLineEditor? = null
    var mCancelButton: LLButton? = null
    var mAddFrameButton: LLButton? = null
    var mDeleteFrameButton: LLButton? = null
    var mImportButton: LLButton? = null
    var mLoadFrame: LLButton? = null
    var mCloneTrack: LLButton? = null
    var mLoadTrack: LLButton? = null
    var mClearTrack: LLButton? = null
    var mTimeSlider: LLMultiSliderCtrl? = null
    var mFramesSlider: LLMultiSliderCtrl? = null
    var mSkyTabLayoutContainer: LLView? = null
    var mWaterTabLayoutContainer: LLView? = null
    var mCurrentTimeLabel: LLTextBox? = null
    var mFlyoutControl: LLFlyoutComboBtnCtrl? = null

    var mTrackFloater: LLHandle<LLFloater>? = null

    var mSkyBlender: LLTrackBlenderLoopingManual? = null
    var mWaterBlender: LLTrackBlenderLoopingManual? = null
    var mScratchSky: LLSettingsSky? = null
    var mScratchWater: LLSettingsWater? = null
    var mCurrentEdit: LLSettingsBase? = null
    var mEditSky: LLSettingsSky? = null
    var mEditWater: LLSettingsWater? = null

    val mPlayTimer: LLFrameTimer = LLFrameTimer()
    var mPlayStartFrame: Float = 0f
    var mIsPlaying: Boolean = false

    var mEditContext: EditContext = EditContext.CONTEXT_UNKNOWN

    // slider-name → FrameData; shadows the contents of mFramesSlider
    val mSliderKeyMap: MutableMap<String, FrameData> = mutableMapOf()

    init {
        mCommitCallbackRegistrar.add(EVNT_DAYTRACK) { _, data -> onTrackSelectionCallback(data) }
        mCommitCallbackRegistrar.add(EVNT_PLAY)     { _, data -> onPlayActionCallback(data) }

        mScratchSky   = LLSettingsVOSky.buildDefaultSky()
        mScratchWater = LLSettingsVOWater.buildDefaultWater()
        mEditSky      = mScratchSky
        mEditWater    = mScratchWater
    }

    override fun postBuild(): Boolean {
        mNameEditor             = getChild<LLLineEditor>(TXT_DAY_NAME, true)
        mCancelButton           = getChild<LLButton>(BTN_CANCEL, true)
        mAddFrameButton         = getChild<LLButton>(BTN_ADDFRAME, true)
        mDeleteFrameButton      = getChild<LLButton>(BTN_DELFRAME, true)
        mTimeSlider             = getChild<LLMultiSliderCtrl>(SLDR_TIME)
        mFramesSlider           = getChild<LLMultiSliderCtrl>(SLDR_KEYFRAMES)
        mSkyTabLayoutContainer  = getChild<LLView>(VIEW_SKY_SETTINGS, true)
        mWaterTabLayoutContainer = getChild<LLView>(VIEW_WATER_SETTINGS, true)
        mCurrentTimeLabel       = getChild<LLTextBox>(LBL_CURRENT_TIME, true)
        mImportButton           = getChild<LLButton>(BTN_IMPORT, true)
        mLoadFrame              = getChild<LLButton>(BTN_LOADFRAME, true)
        mCloneTrack             = getChild<LLButton>(BTN_CLONETRACK, true)
        mLoadTrack              = getChild<LLButton>(BTN_LOADTRACK, true)
        mClearTrack             = getChild<LLButton>(BTN_CLEARTRACK, true)

        mFlyoutControl = LLFlyoutComboBtnCtrl(this, BTN_SAVE, BTN_FLYOUT, XML_FLYOUTMENU_FILE, false)
        mFlyoutControl?.setAction { ctrl, _ -> onButtonApply(ctrl) }

        mNameEditor?.setKeystrokeCallback { _, _ -> onNameKeystroke() }
        mCancelButton?.setCommitCallback   { _, _ -> onClickCloseBtn() }
        mTimeSlider?.setCommitCallback     { _, _ -> onTimeSliderCallback() }
        mAddFrameButton?.setCommitCallback { _, _ -> onAddFrame() }
        mDeleteFrameButton?.setCommitCallback { _, _ -> onRemoveFrame() }
        mImportButton?.setCommitCallback   { _, _ -> onButtonImport() }
        mLoadFrame?.setCommitCallback      { _, _ -> onButtonLoadFrame() }
        mCloneTrack?.setCommitCallback     { _, _ -> onCloneTrack() }
        mLoadTrack?.setCommitCallback      { _, _ -> onLoadTrack() }
        mClearTrack?.setCommitCallback     { _, _ -> onClearTrack() }

        mFramesSlider?.setCommitCallback      { _, data -> onFrameSliderCallback(data) }
        mFramesSlider?.setDoubleClickCallback { _, x, y, mask -> onFrameSliderDoubleClick(x, y, mask) }
        mFramesSlider?.setMouseDownCallback   { _, x, y, mask -> onFrameSliderMouseDown(x, y, mask) }
        mFramesSlider?.setMouseUpCallback     { _, x, y, mask -> onFrameSliderMouseUp(x, y, mask) }

        mTimeSlider?.addSlider(0f)

        val skyTabContainer = mSkyTabLayoutContainer?.getChild<LLTabContainer>("sky_tabs")
        val skyTabCount = skyTabContainer?.getTabCount() ?: 0
        for (idx in 0 until skyTabCount) {
            val panel = skyTabContainer?.getPanelByIndex(idx) as? LLSettingsEditPanel
            panel?.setOnDirtyFlagChanged { _, v -> onPanelDirtyFlagChanged(v) }
        }

        val waterTabContainer = mWaterTabLayoutContainer?.getChild<LLTabContainer>("water_tabs")
        val waterTabCount = waterTabContainer?.getTabCount() ?: 0
        for (idx in 0 until waterTabCount) {
            val panel = waterTabContainer?.getPanelByIndex(idx) as? LLSettingsEditPanel
            panel?.setOnDirtyFlagChanged { _, v -> onPanelDirtyFlagChanged(v) }
        }

        return true
    }

    override fun onOpen(key: LLSD) {
        if (mEditDay == null) {
            LLEnvironment.instance().saveBeaconsState()
        }
        mEditDay = null
        mEditContext = EditContext.CONTEXT_UNKNOWN

        if (key.has(KEY_EDIT_CONTEXT)) {
            when (key[KEY_EDIT_CONTEXT].asString()) {
                VALUE_CONTEXT_INVENTORY -> mEditContext = EditContext.CONTEXT_INVENTORY
                VALUE_CONTEXT_PARCEL    -> mEditContext = EditContext.CONTEXT_PARCEL
                VALUE_CONTEXT_REGION    -> mEditContext = EditContext.CONTEXT_REGION
            }
        }

        if (key.has(KEY_INVENTORY_ID)) {
            loadInventoryItem(key[KEY_INVENTORY_ID].asUUID())
        } else {
            mCanSave  = true
            mCanCopy  = true
            mCanMod   = true
            mCanTrans = true
            setEditDefaultDayCycle()
        }

        mDayLength = 0L
        if (key.has(KEY_DAY_LENGTH)) {
            mDayLength = key[KEY_DAY_LENGTH].asLong()
        }

        mCurrentTimeLabel?.setTextArg("[PRCNT]", "0")
        val maxElm = 5
        if (mDayLength != 0L) {
            val formattedLabel = getString("time_label")
            for (i in 0 until maxElm) {
                val total = (mDayLength / (maxElm - 1)) * i
                val hrs = total / 3600
                val minutes = (total % 3600) / 60
                getChild<LLTextBox>("p$i", true)?.setTextArg(
                    "[DSC]",
                    formattedLabel
                        .replace("[HH]", hrs.toString())
                        .replace("[MM]", Math.abs(minutes.toInt()).toString())
                )
            }
            val hrs = mDayLength / 3600
            val minutes = (mDayLength % 3600) / 60
            mCurrentTimeLabel?.setTextArg(
                "[DSC]",
                formattedLabel
                    .replace("[HH]", hrs.toString())
                    .replace("[MM]", Math.abs(minutes.toInt()).toString())
            )
        } else {
            for (i in 0 until maxElm) {
                getChild<LLTextBox>("p$i", true)?.setTextArg("[DSC]", "")
            }
            mCurrentTimeLabel?.setTextArg("[DSC]", "")
        }

        val labelRect = getChild<LLTextBox>("p0", true)?.getRect()
        val sliderWidth = mFramesSlider?.getRect()?.getWidth()?.toFloat() ?: 0f
        for (i in 1 until maxElm) {
            val pcntLabel = getChild<LLTextBox>("p$i", true)
            if (pcntLabel != null && labelRect != null) {
                val newRect = pcntLabel.getRect()
                newRect.mLeft = labelRect.mLeft + (sliderWidth * i.toFloat() / (maxElm - 1).toFloat()).toInt() -
                    (pcntLabel.getTextPixelWidth() / 2)
                pcntLabel.setRect(newRect)
            }
        }

        val formattedSkyLabel = getString("sky_track_label")
        val altitudes = LLEnvironment.instance().getRegionAltitudes()
        val extendedEnv = LLEnvironment.instance().isExtendedEnvironmentEnabled()
        val useAltitudes = extendedEnv && altitudes.isNotEmpty() &&
            (mEditContext == EditContext.CONTEXT_PARCEL || mEditContext == EditContext.CONTEXT_REGION)

        for (idx in 1..3) {
            val alt = if (useAltitudes) "${altitudes[idx]}m" else "${idx + 1}"
            getChild<LLButton>(TRACK_TABS[idx + 1], true)?.setLabel(
                formattedSkyLabel.replace("[ALT]", alt)
            )
        }

        for (i in 2u until LLSettingsDay.TRACK_MAX) {
            getChild<LLButton>(TRACK_TABS[i.toInt()])?.setEnabled(extendedEnv)
        }

        when (mEditContext) {
            EditContext.CONTEXT_INVENTORY -> {
                mFlyoutControl?.setShownBtnEnabled(true)
                mFlyoutControl?.setSelectedItem(ACTION_SAVE)
            }
            EditContext.CONTEXT_REGION, EditContext.CONTEXT_PARCEL -> {
                val commitStr = if (mEditContext == EditContext.CONTEXT_PARCEL) STR_COMMIT_PARCEL else STR_COMMIT_REGION
                mFlyoutControl?.setMenuItemLabel(ACTION_COMMIT, getString(commitStr))
                mFlyoutControl?.setShownBtnEnabled(true)
                mFlyoutControl?.setSelectedItem(ACTION_COMMIT)
            }
            else -> mFlyoutControl?.setShownBtnEnabled(false)
        }
    }

    override fun onClose(appQuitting: Boolean) {
        doCloseInventoryFloater(appQuitting)
        doCloseTrackFloater(appQuitting)
        stopPlay()
        LLEnvironment.instance().revertBeaconsState()
        if (!appQuitting) {
            LLEnvironment.instance().setSelectedEnvironment(LLEnvironment.ENV_LOCAL, LLEnvironment.TRANSITION_FAST)
            LLEnvironment.instance().clearEnvironment(LLEnvironment.ENV_EDIT)
            mEditDay = null
        }
    }

    override fun onVisibilityChange(newVisibility: Boolean) {
        // nothing to do – retained for override completeness
    }

    override fun refresh() {
        mEditDay?.let { day ->
            mNameEditor?.setText(day.getName())
            mNameEditor?.setEnabled(mCanMod && mCanSave && mInventoryId != null)
        }

        val isInventoryAvail = canUseInventory()
        val showCommit = mEditContext == EditContext.CONTEXT_PARCEL || mEditContext == EditContext.CONTEXT_REGION
        val showApply  = mEditContext == EditContext.CONTEXT_INVENTORY

        if (showCommit) {
            val commitText = if (mEditContext == EditContext.CONTEXT_PARCEL) getString(STR_COMMIT_PARCEL) else getString(STR_COMMIT_REGION)
            mFlyoutControl?.setMenuItemLabel(ACTION_COMMIT, commitText)
        }

        mFlyoutControl?.setMenuItemVisible(ACTION_COMMIT,       showCommit)
        mFlyoutControl?.setMenuItemVisible(ACTION_SAVE,         isInventoryAvail)
        mFlyoutControl?.setMenuItemVisible(ACTION_SAVEAS,       isInventoryAvail)
        mFlyoutControl?.setMenuItemVisible(ACTION_APPLY_LOCAL,  true)
        mFlyoutControl?.setMenuItemVisible(ACTION_APPLY_PARCEL, showApply)
        mFlyoutControl?.setMenuItemVisible(ACTION_APPLY_REGION, showApply)

        mFlyoutControl?.setMenuItemEnabled(ACTION_COMMIT,       showCommit && mCommitSignal.isNotEmpty())
        mFlyoutControl?.setMenuItemEnabled(ACTION_SAVE,         isInventoryAvail && mCanMod && mCanSave && mInventoryId != null)
        mFlyoutControl?.setMenuItemEnabled(ACTION_SAVEAS,       isInventoryAvail && mCanCopy && mCanSave)
        mFlyoutControl?.setMenuItemEnabled(ACTION_APPLY_LOCAL,  true)
        mFlyoutControl?.setMenuItemEnabled(ACTION_APPLY_PARCEL, canApplyParcel() && showApply)
        mFlyoutControl?.setMenuItemEnabled(ACTION_APPLY_REGION, canApplyRegion() && showApply)

        mImportButton?.setEnabled(mCanMod)

        super.refresh()
    }

    override fun setEditSettingsAndUpdate(settings: LLSettingsBase?) {
        val day = settings as? LLSettingsDay
        setEditDayCycle(day)
        showHDRNotification(day)
    }

    fun setEditDayCycle(pday: LLSettingsDay?) {
        mExpectingAssetId = null
        mEditDay = pday?.buildDeepCloneAndUncompress()

        if (mEditDay?.isTrackEmpty(LLSettingsDay.TRACK_WATER) == true) {
            mEditDay?.setWaterAtKeyframe(LLSettingsVOWater.buildDefaultWater(), 0.5f)
        }
        if (mEditDay?.isTrackEmpty(LLSettingsDay.TRACK_GROUND_LEVEL) == true) {
            mEditDay?.setSkyAtKeyframe(LLSettingsVOSky.buildDefaultSky(), 0.5f, LLSettingsDay.TRACK_GROUND_LEVEL)
        }

        if (pday != null) {
            mCanSave  = !pday.getFlag(LLSettingsBase.FLAG_NOSAVE)
            mCanCopy  = !pday.getFlag(LLSettingsBase.FLAG_NOCOPY) && mCanSave
            mCanMod   = !pday.getFlag(LLSettingsBase.FLAG_NOMOD)  && mCanSave
            mCanTrans = !pday.getFlag(LLSettingsBase.FLAG_NOTRANS) && mCanSave
        }

        updateEditEnvironment()
        LLEnvironment.instance().setSelectedEnvironment(LLEnvironment.ENV_EDIT, LLEnvironment.TRANSITION_INSTANT)
        synchronizeTabs()
        updateTabs()
        refresh()
    }

    fun setEditDefaultDayCycle() {
        mInventoryItem = null
        mInventoryId   = null
        mExpectingAssetId = LLSettingsDay.getDefaultAssetId()
        LLSettingsVOBase.getSettingsAsset(LLSettingsDay.getDefaultAssetId()) { assetId, settings, status, _ ->
            onAssetLoaded(assetId, settings, status)
        }
    }

    fun getEditName(): String = mEditDay?.getName() ?: "new"

    fun setEditName(name: String) {
        mEditDay?.setName(name)
        getChild<LLLineEditor>(TXT_DAY_NAME)?.setText(name)
    }

    fun getEditingAssetId(): UUID? = mEditDay?.getAssetId()
    fun getEditingInventoryId(): UUID? = mInventoryId

    override fun getEditSettings(): LLSettingsBase? = mEditDay

    override fun handleKeyUp(key: Int, mask: Int, calledFromParent: Boolean): Boolean {
        if (mEditDay == null) {
            mShiftCopyEnabled = false
        } else if (mask == MASK_SHIFT && mShiftCopyEnabled) {
            mShiftCopyEnabled = false
            val curSlider = mFramesSlider?.getCurSlider() ?: ""
            if (curSlider.isNotEmpty()) {
                val sliderPos = mFramesSlider?.getCurSliderValue() ?: 0f
                val entry = mSliderKeyMap[curSlider]
                if (entry != null) {
                    if (mEditDay?.moveTrackKeyframe(mCurrentTrack, entry.mFrame, sliderPos) == true) {
                        entry.mFrame = sliderPos
                    } else {
                        mFramesSlider?.setCurSliderValue(entry.mFrame)
                    }
                }
            }
        }
        return super.handleKeyUp(key, mask, calledFromParent)
    }

    private fun getCurrentFrame(): Float = mTimeSlider?.getCurSliderValue() ?: 0f

    private fun onButtonApply(ctrl: LLUICtrl?) {
        val ctrlAction = ctrl?.getName() ?: return

        val day = mEditDay
        if (day == null) {
            LLNotificationsUtil.add("EnvironmentApplyFailed")
            closeFloater()
            return
        }

        val dayclone = day.buildClone() ?: return

        // Scan for local textures that cannot be saved to inventory
        for (i in 0u..LLSettingsDay.TRACK_MAX) {
            val dayTrack = dayclone.getCycleTrack(i)
            var frameNum = 0
            for ((position, setting) in dayTrack) {
                frameNum++
                var desc = ""
                var isLocal = false
                if (i == LLSettingsDay.TRACK_WATER) {
                    val water = setting as? LLSettingsWater
                    if (water != null) {
                        if (LLLocalBitmapMgr.getInstance().isLocal(water.getNormalMapID())) {
                            desc = LLTrans.getString("EnvironmentNormalMap"); isLocal = true
                        } else if (LLLocalBitmapMgr.getInstance().isLocal(water.getTransparentTextureID())) {
                            desc = LLTrans.getString("EnvironmentTransparent"); isLocal = true
                        }
                    }
                } else {
                    val sky = setting as? LLSettingsSky
                    if (sky != null) {
                        when {
                            LLLocalBitmapMgr.getInstance().isLocal(sky.getSunTextureId())       -> { desc = LLTrans.getString("EnvironmentSun");        isLocal = true }
                            LLLocalBitmapMgr.getInstance().isLocal(sky.getMoonTextureId())      -> { desc = LLTrans.getString("EnvironmentMoon");       isLocal = true }
                            LLLocalBitmapMgr.getInstance().isLocal(sky.getCloudNoiseTextureId()) -> { desc = LLTrans.getString("EnvironmentCloudNoise"); isLocal = true }
                            LLLocalBitmapMgr.getInstance().isLocal(sky.getBloomTextureId())     -> { desc = LLTrans.getString("EnvironmentBloom");      isLocal = true }
                        }
                    }
                }
                if (isLocal) {
                    val args = LLSD()
                    args["TRACK"]   = getChild<LLButton>(TRACK_TABS[i.toInt()], true)?.getCurrentLabel()
                    args["FRAME"]   = position * 100f
                    args["FIELD"]   = desc
                    args["FRAMENO"] = frameNum
                    LLNotificationsUtil.add("WLLocalTextureDayBlock", args)
                    return
                }
            }
        }

        when (ctrlAction) {
            ACTION_SAVE   -> { doApplyUpdateInventory(dayclone); clearDirtyFlag() }
            ACTION_SAVEAS -> {
                val args = LLSD()
                args["DESC"] = dayclone.getName()
                LLNotificationsUtil.add("SaveSettingAs", args, LLSD()) { notif, resp ->
                    onSaveAsCommit(notif, resp, dayclone)
                }
            }
            ACTION_APPLY_LOCAL, ACTION_APPLY_PARCEL, ACTION_APPLY_REGION ->
                doApplyEnvironment(ctrlAction, dayclone)
            ACTION_COMMIT -> doApplyCommit(dayclone)
        }
    }

    private fun onButtonLoadFrame() {
        val type = if (mCurrentTrack == LLSettingsDay.TRACK_WATER) LLSettingsType.ST_WATER else LLSettingsType.ST_SKY
        doOpenInventoryFloater(type, null)
    }

    private fun onAddFrame() {
        val frame = mTimeSlider?.getCurSliderValue() ?: return
        val day = mEditDay ?: return

        if (day.getSettingsNearKeyframe(frame, mCurrentTrack, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR).second != null) return
        if (mFramesSlider?.canAddSliders() == false) return

        val setting: LLSettingsBase?
        if (mCurrentTrack == LLSettingsDay.TRACK_WATER) {
            val water = mScratchWater?.buildClone()
            setting = water
            if (water != null) day.setWaterAtKeyframe(water, frame)
        } else {
            val sky = mScratchSky?.buildClone() as? LLSettingsSky
            setting = sky
            if (sky != null) day.setSkyAtKeyframe(sky, frame, mCurrentTrack)
        }
        setDirtyFlag()
        if (setting != null) addSliderFrame(frame, setting)
        updateTabs()
    }

    private fun onRemoveFrame() {
        val slrKey = mFramesSlider?.getCurSlider() ?: return
        if (slrKey.isEmpty()) return
        setDirtyFlag()
        removeCurrentSliderFrame()
        updateTabs()
    }

    private fun onCloneTrack() {
        val day = mEditDay ?: return
        val altitudes = LLEnvironment.instance().getRegionAltitudes()
        val useAltitudes = altitudes.isNotEmpty() &&
            (mEditContext == EditContext.CONTEXT_PARCEL || mEditContext == EditContext.CONTEXT_REGION)

        val args = LLSD.emptyArray()
        var populatedCounter = 0
        for (i in 1u until LLSettingsDay.TRACK_MAX) {
            val track = LLSD()
            track["id"] = i.toInt()
            val populated = !day.isTrackEmpty(i) && i != mCurrentTrack
            track["enabled"] = populated
            if (populated) populatedCounter++
            if (useAltitudes) track["altitude"] = altitudes[(i - 1u).toInt()]
            args.append(track)
        }

        if (populatedCounter > 0) {
            doOpenTrackFloater(args)
        }
    }

    private fun onLoadTrack() {
        var curitemId = mInventoryId
        if (mCurrentEdit != null && curitemId != null) {
            curitemId = LLFloaterSettingsPicker.findItemID(mCurrentEdit!!.getAssetId(), false, false)
        }
        doOpenInventoryFloater(LLSettingsType.ST_DAYCYCLE, curitemId)
    }

    private fun onClearTrack() {
        val day = mEditDay ?: return
        if (mCurrentTrack > 1u) {
            day.getCycleTrack(mCurrentTrack).clear()
        } else {
            val track = day.getCycleTrack(mCurrentTrack)
            val iter = track.entries.iterator()
            if (iter.hasNext()) iter.next() // keep first entry
            while (iter.hasNext()) { iter.next(); iter.remove() }
        }
        updateEditEnvironment()
        LLEnvironment.instance().setSelectedEnvironment(LLEnvironment.ENV_EDIT, LLEnvironment.TRANSITION_INSTANT)
        synchronizeTabs()
        updateTabs()
        refresh()
    }

    private fun onNameKeystroke() {
        mEditDay?.setName(mNameEditor?.getText() ?: return)
    }

    private fun onTrackSelectionCallback(userData: LLSD) {
        selectTrack(userData.asInteger().toUInt())
    }

    private fun onPlayActionCallback(userData: LLSD) {
        val action = userData.asString()
        val frame = mTimeSlider?.getCurSliderValue() ?: 0f
        when (action) {
            ACTION_PLAY  -> startPlay()
            ACTION_PAUSE -> stopPlay()
            else -> {
                if (mSliderKeyMap.isNotEmpty()) {
                    val increment = mTimeSlider?.getIncrement() ?: 0f
                    val newFrame = when (action) {
                        ACTION_FORWARD -> mEditDay?.getUpperBoundFrame(mCurrentTrack, frame + increment / 2f) ?: frame
                        ACTION_BACK    -> mEditDay?.getLowerBoundFrame(mCurrentTrack, frame - increment / 2f) ?: frame
                        else           -> frame
                    }
                    selectFrame(newFrame, 0f)
                    stopPlay()
                }
            }
        }
    }

    private fun onTimeSliderCallback() {
        stopPlay()
        selectFrame(mTimeSlider?.getCurSliderValue() ?: 0f, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
    }

    private fun onFrameSliderCallback(data: LLSD) {
        val curSlider = mFramesSlider?.getCurSlider() ?: ""
        val day = mEditDay ?: return
        if (curSlider.isEmpty()) return

        val sliderPos = mFramesSlider?.getCurSliderValue() ?: 0f
        val entry = mSliderKeyMap[curSlider] ?: return

        if (gKeyboard.currentMask(true) == MASK_SHIFT && mShiftCopyEnabled && mCanMod) {
            if (day.getSettingsNearKeyframe(sliderPos, mCurrentTrack, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR).second == null) {
                val newSettings: LLSettingsBase?
                if (mCurrentTrack == LLSettingsDay.TRACK_WATER) {
                    val water = (entry.pSettings as? LLSettingsWater)?.buildClone() as? LLSettingsWater
                    day.setWaterAtKeyframe(water, sliderPos)
                    newSettings = water
                } else {
                    val sky = (entry.pSettings as? LLSettingsSky)?.buildClone() as? LLSettingsSky
                    day.setSkyAtKeyframe(sky, sliderPos, mCurrentTrack)
                    newSettings = sky
                }
                val oldFrame = entry.mFrame
                entry.mFrame = sliderPos
                if (newSettings != null) addSliderFrame(oldFrame, newSettings, false)
                mFramesSlider?.setCurSlider(curSlider)
                mShiftCopyEnabled = false
                setDirtyFlag()
            }
        } else {
            val nearestIncrement = mFramesSlider?.getNearestIncrement(entry.mFrame) ?: entry.mFrame
            when {
                Math.abs(nearestIncrement - sliderPos) < F_APPROXIMATELY_ZERO ->
                    mFramesSlider?.setCurSliderValue(entry.mFrame)
                day.moveTrackKeyframe(mCurrentTrack, entry.mFrame, sliderPos) && mCanMod -> {
                    entry.mFrame = sliderPos
                    setDirtyFlag()
                }
                else -> mFramesSlider?.setCurSliderValue(entry.mFrame)
            }
            mShiftCopyEnabled = false
        }
    }

    private fun onFrameSliderDoubleClick(x: Int, y: Int, mask: Int) {
        stopPlay()
        onAddFrame()
    }

    private fun onFrameSliderMouseDown(x: Int, y: Int, mask: Int) {
        stopPlay()
        val sliderPos = mFramesSlider?.getSliderValueFromPos(x, y) ?: 0f
        val sliderName = mFramesSlider?.getCurSlider() ?: ""

        mShiftCopyEnabled = sliderName.isNotEmpty() && gKeyboard.currentMask(true) == MASK_SHIFT

        if (sliderName.isNotEmpty()) {
            val thumbRect = mFramesSlider?.getSliderThumbRect(sliderName)
            if (thumbRect != null && (x >= thumbRect.mRight || x <= thumbRect.mLeft)) {
                mFramesSlider?.resetCurSlider()
            }
        }

        mTimeSlider?.setCurSliderValue(sliderPos)
        updateTabs()
        LLEnvironment.instance().updateEnvironment(LLEnvironment.TRANSITION_INSTANT)
    }

    private fun onFrameSliderMouseUp(x: Int, y: Int, mask: Int) {
        val sliderPos = mFramesSlider?.getSliderValueFromPos(x, y) ?: 0f
        mTimeSlider?.setCurSliderValue(sliderPos)
        selectFrame(sliderPos, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
    }

    private fun cloneTrack(sourceIndex: UInt, destIndex: UInt) {
        cloneTrack(mEditDay ?: return, sourceIndex, destIndex)
    }

    private fun cloneTrack(sourceDay: LLSettingsDay, sourceIndex: UInt, destIndex: UInt) {
        if ((sourceIndex == LLSettingsDay.TRACK_WATER || destIndex == LLSettingsDay.TRACK_WATER) &&
            sourceIndex != destIndex) {
            val args = LLSD()
            args["TRACK1"] = getChild<LLButton>(TRACK_TABS[sourceIndex.toInt()], true)?.getCurrentLabel()
            args["TRACK2"] = getChild<LLButton>(TRACK_TABS[destIndex.toInt()], true)?.getCurrentLabel()
            LLNotificationsUtil.add("TrackLoadMismatch", args)
            return
        }

        val day = mEditDay ?: return
        val backupTrack = day.getCycleTrack(destIndex).toMap()
        day.clearCycleTrack(destIndex)

        val sourceTrack = sourceDay.getCycleTrack(sourceIndex)
        var addCount = 0
        for ((position, frame) in sourceTrack) {
            val clone = frame?.buildDerivedClone()
            if (clone != null) {
                addCount++
                day.setSettingsAtKeyframe(clone, position, destIndex)
            }
        }

        if (addCount == 0) {
            day.replaceCycleTrack(destIndex, backupTrack)
            val args = LLSD()
            args["TRACK"] = getChild<LLButton>(TRACK_TABS[destIndex.toInt()], true)?.getCurrentLabel()
            LLNotificationsUtil.add("TrackLoadFailed", args)
        }
        setDirtyFlag()
        updateSlider()
        updateTabs()
        updateButtons()
    }

    private fun selectTrack(trackIndex: UInt, force: Boolean = false) {
        if (trackIndex < LLSettingsDay.TRACK_MAX) mCurrentTrack = trackIndex

        val button = getChild<LLButton>(TRACK_TABS[mCurrentTrack.toInt()], true)
        if (button?.getToggleState() == true && !force) return

        for (i in 0u until LLSettingsDay.TRACK_MAX) {
            getChild<LLButton>(TRACK_TABS[i.toInt()], true)?.setToggleState(i == mCurrentTrack)
        }

        val showWater = mCurrentTrack == LLSettingsDay.TRACK_WATER
        mSkyTabLayoutContainer?.setVisible(!showWater)
        mWaterTabLayoutContainer?.setVisible(showWater)

        updateSlider()
        updateLabels()
    }

    private fun selectFrame(frame: Float, slopFactor: Float) {
        mFramesSlider?.resetCurSlider()
        var selectedFrame = frame

        val iter = mSliderKeyMap.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val keyframe = entry.value.mFrame
            val frameDif = Math.abs(keyframe - frame)
            if (frameDif <= slopFactor) {
                // Prefer the closer of this and the next entry when not exact
                val nextEntry = mSliderKeyMap.entries.firstOrNull { it.key != entry.key &&
                    Math.abs(it.value.mFrame - frame) < frameDif }
                if (frameDif != 0f && nextEntry != null) {
                    mFramesSlider?.setCurSlider(nextEntry.key)
                    selectedFrame = nextEntry.value.mFrame
                } else {
                    mFramesSlider?.setCurSlider(entry.key)
                    selectedFrame = entry.value.mFrame
                }
                break
            }
        }

        mTimeSlider?.setCurSliderValue(selectedFrame)
        updateTabs()
    }

    private fun clearTabs() {
        if (mCurrentTrack == LLSettingsDay.TRACK_WATER) {
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

    private fun updateWaterTabs(pWater: LLSettingsWater?) {
        val tabContainer = mWaterTabLayoutContainer?.getChild<LLView>(TABS_WATER)
        val panel = tabContainer?.findChildView("water_panel") as? LLPanelSettingsWaterMainTab
        panel?.setWater(pWater)
    }

    private fun updateSkyTabs(pSky: LLSettingsSky?) {
        val tabContainer = mSkyTabLayoutContainer?.getChild<LLTabContainer>(TABS_SKYS)
        (tabContainer?.findChildView("atmosphere_panel") as? LLPanelSettingsSky)?.setSky(pSky)
        (tabContainer?.findChildView("clouds_panel")     as? LLPanelSettingsSky)?.setSky(pSky)
        (tabContainer?.findChildView("moon_panel")       as? LLPanelSettingsSky)?.setSky(pSky)
    }

    private fun updateLabels() {
        val labelArg = if (mCurrentTrack == LLSettingsDay.TRACK_WATER) "water_label" else "sky_label"
        mAddFrameButton?.setLabelArg("[FRAME]", getString(labelArg))
        mDeleteFrameButton?.setLabelArg("[FRAME]", getString(labelArg))
        mLoadFrame?.setLabelArg("[FRAME]", getString(labelArg))
    }

    private fun updateButtons() {
        val canManipulate = mEditDay != null && !mIsPlaying && mCanMod
        var canClone = false
        var canClear = false

        if (canManipulate) {
            if (mCurrentTrack != 0u) {
                for (track in 1u until LLSettingsDay.TRACK_MAX) {
                    if (track == mCurrentTrack) continue
                    if (mEditDay?.getCycleTrack(track)?.isNotEmpty() == true) canClone = true
                }
            }
            canClear = if (mCurrentTrack > 1u)
                mEditDay?.getCycleTrack(mCurrentTrack)?.isNotEmpty() == true
            else
                (mEditDay?.getCycleTrack(mCurrentTrack)?.size ?: 0) > 1
        }

        mCloneTrack?.setEnabled(canClone)
        mLoadTrack?.setEnabled(canManipulate)
        mClearTrack?.setEnabled(canClear)
        mAddFrameButton?.setEnabled(canManipulate && isAddingFrameAllowed())
        mDeleteFrameButton?.setEnabled(canManipulate && isRemovingFrameAllowed())
        mLoadFrame?.setEnabled(canManipulate)

        val enablePlay = mEditDay != null
        childSetEnabled(BTN_PLAY,         enablePlay)
        childSetEnabled(BTN_SKIP_BACK,    enablePlay)
        childSetEnabled(BTN_SKIP_FORWARD, enablePlay)

        val extendedEnv = LLEnvironment.instance().isExtendedEnvironmentEnabled()
        for (track in 0u until LLSettingsDay.TRACK_MAX) {
            val btn = getChild<LLButton>(TRACK_TABS[track.toInt()], true)
            btn?.setEnabled(extendedEnv)
            btn?.setToggleState(track == mCurrentTrack)
        }
    }

    private fun updateSlider() {
        val framePosition = mTimeSlider?.getCurSliderValue() ?: 0f
        mFramesSlider?.clear()
        mSliderKeyMap.clear()

        val day = mEditDay ?: return

        for ((pos, setting) in day.getCycleTrack(mCurrentTrack)) {
            addSliderFrame(pos, setting, false)
        }

        if (mSliderKeyMap.isNotEmpty()) {
            mLastFrameSlider = mFramesSlider?.getCurSlider() ?: ""
        } else {
            clearTabs()
            mLastFrameSlider = ""
        }
        selectFrame(framePosition, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
    }

    private fun updateTimeAndLabel() {
        val time = mTimeSlider?.getCurSliderValue() ?: 0f
        mCurrentTimeLabel?.setTextArg("[PRCNT]", "%.0f".format(time * 100f))
        if (mDayLength != 0L) {
            val total = (mDayLength * time).toLong()
            val hrs = total / 3600
            val minutes = Math.abs(((total % 3600) / 60).toInt())
            val label = getString("time_label")
                .replace("[HH]", hrs.toString())
                .replace("[MM]", minutes.toString())
            mCurrentTimeLabel?.setTextArg("[DSC]", label)
        } else {
            mCurrentTimeLabel?.setTextArg("[DSC]", "")
        }
    }

    private fun addSliderFrame(frame: Float, setting: LLSettingsBase, updateUi: Boolean = true) {
        val newSlider = mFramesSlider?.addSlider(frame) ?: ""
        if (newSlider.isNotEmpty()) {
            mSliderKeyMap[newSlider] = FrameData(frame, setting)
            if (updateUi) {
                mLastFrameSlider = newSlider
                mTimeSlider?.setCurSliderValue(frame)
                updateTabs()
            }
        }
    }

    private fun removeCurrentSliderFrame() {
        val sldr = mFramesSlider?.getCurSlider() ?: return
        if (sldr.isEmpty()) return
        mFramesSlider?.deleteCurSlider()
        val entry = mSliderKeyMap.remove(sldr)
        if (entry != null) {
            mEditDay?.removeTrackKeyframe(mCurrentTrack, entry.mFrame)
        }
        mLastFrameSlider = mFramesSlider?.getCurSlider() ?: ""
        mTimeSlider?.setCurSliderValue(mFramesSlider?.getCurSliderValue() ?: 0f)
        updateTabs()
    }

    private fun removeSliderFrame(frame: Float) {
        val entry = mSliderKeyMap.entries.firstOrNull { Math.abs(it.value.mFrame - frame) < LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR }
        if (entry != null) {
            mFramesSlider?.deleteSlider(entry.key)
            mSliderKeyMap.remove(entry.key)
        }
    }

    override fun updateEditEnvironment() {
        val day = mEditDay ?: return
        val skyTrack = if (mCurrentTrack != 0u) mCurrentTrack else 1u
        mSkyBlender   = LLTrackBlenderLoopingManual(mScratchSky, day, skyTrack)
        mWaterBlender  = LLTrackBlenderLoopingManual(mScratchWater, day, LLSettingsDay.TRACK_WATER)

        if (LLEnvironment.instance().isExtendedEnvironmentEnabled()) {
            selectTrack(LLSettingsDay.TRACK_MAX, true)
        } else {
            selectTrack(1u, true)
        }

        reblendSettings()
        LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_EDIT, mEditSky, mEditWater)
        LLEnvironment.instance().updateEnvironment(LLEnvironment.TRANSITION_INSTANT)
    }

    private fun synchronizeTabs() {
        val curSlider = mFramesSlider?.getCurSlider() ?: ""
        var canEdit = false

        var psettingW: LLSettingsWater? = null
        val waterTabs = mWaterTabLayoutContainer?.getChild<LLTabContainer>(TABS_WATER)
        if (mCurrentTrack == LLSettingsDay.TRACK_WATER) {
            val day = mEditDay
            if (day != null && curSlider.isNotEmpty()) {
                canEdit = !mIsPlaying
                psettingW = mSliderKeyMap[curSlider]?.pSettings as? LLSettingsWater
            }
            mCurrentEdit = psettingW
            if (psettingW == null) {
                canEdit   = false
                psettingW = mScratchWater
            }
            getChild<LLUICtrl>(ICN_LOCK_EDIT)?.setVisible(!canEdit)
        } else {
            psettingW = mScratchWater
        }
        mEditWater = psettingW
        if (waterTabs != null) setTabsData(waterTabs, psettingW, canEdit)

        canEdit = false
        var psettingS: LLSettingsSky? = null
        val skyTabs = mSkyTabLayoutContainer?.getChild<LLTabContainer>(TABS_SKYS)
        if (mCurrentTrack != LLSettingsDay.TRACK_WATER) {
            val day = mEditDay
            if (day != null && curSlider.isNotEmpty()) {
                canEdit = !mIsPlaying
                psettingS = mSliderKeyMap[curSlider]?.pSettings as? LLSettingsSky
            }
            mCurrentEdit = psettingS
            if (psettingS == null) {
                canEdit   = false
                psettingS = mScratchSky
            }
            getChild<LLUICtrl>(ICN_LOCK_EDIT)?.setVisible(!canEdit)
        } else {
            psettingS = mScratchSky
        }
        mEditSky = psettingS

        doCloseInventoryFloater()
        doCloseTrackFloater()

        if (skyTabs != null) setTabsData(skyTabs, psettingS, canEdit)
        LLEnvironment.instance().setEnvironment(LLEnvironment.ENV_EDIT, mEditSky, mEditWater)
        LLEnvironment.instance().updateEnvironment(LLEnvironment.TRANSITION_INSTANT)
    }

    private fun setTabsData(tabcontainer: LLTabContainer, settings: LLSettingsBase?, editable: Boolean) {
        val count = tabcontainer.getTabCount()
        for (idx in 0 until count) {
            val panel = tabcontainer.getPanelByIndex(idx) as? LLSettingsEditPanel
            panel?.setCanChangeSettings(editable && mCanMod)
            panel?.setSettings(settings)
        }
    }

    private fun reblendSettings() {
        val position = mTimeSlider?.getCurSliderValue()?.toDouble() ?: 0.0
        val skyBlender = mSkyBlender
        if (skyBlender != null) {
            if (skyBlender.getTrack() != mCurrentTrack && mCurrentTrack != LLSettingsDay.TRACK_WATER) {
                skyBlender.switchTrack(mCurrentTrack, position.toFloat())
            } else {
                skyBlender.setPosition(position.toFloat())
            }
        }
        mWaterBlender?.setPosition(position.toFloat())
    }

    private fun doApplyCommit(day: LLSettingsDay) {
        if (mCommitSignal.isNotEmpty()) {
            mCommitSignal.forEach { it(day) }
            closeFloater()
        }
    }

    private fun isRemovingFrameAllowed(): Boolean {
        if (mFramesSlider?.getCurSlider().isNullOrEmpty()) return false
        return if (mCurrentTrack <= LLSettingsDay.TRACK_GROUND_LEVEL)
            mSliderKeyMap.size > 1
        else
            mSliderKeyMap.isNotEmpty()
    }

    private fun isAddingFrameAllowed(): Boolean {
        if (!mFramesSlider?.getCurSlider().isNullOrEmpty() || mEditDay == null) return false
        val frame = mTimeSlider?.getCurSliderValue() ?: 0f
        if (mEditDay?.getSettingsNearKeyframe(frame, mCurrentTrack, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR)?.second != null) return false
        return mFramesSlider?.canAddSliders() == true
    }

    override fun doImportFromDisk() {
        TODO("APR: use JVM equivalent – open a file chooser for legacy Windlight XML files")
    }

    private fun loadSettingFromFile(filenames: MutableList<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        val messages = LLSD()
        val legacyday = LLEnvironment.createDayCycleFromLegacyPreset(filename, messages) ?: run {
            LLNotificationsUtil.add("WLImportFail", messages)
            return
        }
        loadInventoryItem(null)
        mCurrentTrack = 1u
        setDirtyFlag()
        setEditDayCycle(legacyday)
    }

    private fun startPlay() {
        doCloseInventoryFloater()
        doCloseTrackFloater()
        mIsPlaying       = true
        mFramesSlider?.resetCurSlider()
        mPlayTimer.reset()
        mPlayTimer.start()
        gIdleCallbacks.addFunction(::onIdlePlay, this)
        mPlayStartFrame = mTimeSlider?.getCurSliderValue() ?: 0f
        getChild<LLView>("play_layout", true)?.setVisible(false)
        getChild<LLView>("pause_layout", true)?.setVisible(true)
    }

    private fun stopPlay() {
        if (!mIsPlaying) return
        mIsPlaying = false
        gIdleCallbacks.deleteFunction(::onIdlePlay, this)
        mPlayTimer.stop()
        val frame = mTimeSlider?.getCurSliderValue() ?: 0f
        selectFrame(frame, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
        getChild<LLView>("play_layout",  true)?.setVisible(true)
        getChild<LLView>("pause_layout", true)?.setVisible(false)
    }

    override fun clearDirtyFlag() {
        mIsDirty = false
        val skyTabContainer = mSkyTabLayoutContainer?.getChild<LLTabContainer>("sky_tabs")
        val skyCount = skyTabContainer?.getTabCount() ?: 0
        for (idx in 0 until skyCount) {
            (skyTabContainer?.getPanelByIndex(idx) as? LLSettingsEditPanel)?.clearIsDirty()
        }
        val waterTabContainer = mWaterTabLayoutContainer?.getChild<LLTabContainer>("water_tabs")
        val waterCount = waterTabContainer?.getTabCount() ?: 0
        for (idx in 0 until waterCount) {
            (waterTabContainer?.getPanelByIndex(idx) as? LLSettingsEditPanel)?.clearIsDirty()
        }
    }

    private fun doOpenTrackFloater(args: LLSD) {
        var picker = mTrackFloater?.get() as? LLFloaterTrackPicker
        if (picker == null) {
            picker = LLFloaterTrackPicker(this)
            mTrackFloater = picker.getHandle()
            picker.setCommitCallback { _, data -> onPickerCommitTrackId(data.asInteger().toUInt()) }
        }
        picker.showPicker(args)
    }

    private fun doCloseTrackFloater(quitting: Boolean = false) {
        mTrackFloater?.get()?.closeFloater(quitting)
    }

    override fun getSettingsPicker(): LLFloaterSettingsPicker {
        var picker = mInventoryFloater?.get() as? LLFloaterSettingsPicker
        if (picker == null) {
            picker = LLFloaterSettingsPicker(this, null)
            mInventoryFloater = picker.getHandle()
            picker.setCommitCallback { _, data ->
                onPickerCommitSetting(data["ItemId"].asUUID(), data["Track"].asInteger())
            }
        }
        return picker
    }

    private fun onPickerCommitTrackId(trackId: UInt) {
        cloneTrack(trackId, mCurrentTrack)
    }

    private fun doOpenInventoryFloater(type: LLSettingsType.TypeE, curritem: UUID?) {
        val picker = getSettingsPicker()
        picker.setSettingsFilter(type)
        picker.setSettingsItemId(curritem)
        if (type == LLSettingsType.ST_DAYCYCLE) {
            picker.setTrackMode(
                if (mCurrentTrack == LLSettingsDay.TRACK_WATER) LLFloaterSettingsPicker.TRACK_WATER
                else LLFloaterSettingsPicker.TRACK_SKY
            )
        } else {
            picker.setTrackMode(LLFloaterSettingsPicker.TRACK_NONE)
        }
        picker.openFloater()
        picker.setFocus(true)
    }

    private fun onPickerCommitSetting(itemId: UUID?, track: Int) {
        val frame = mTimeSlider?.getCurSliderValue() ?: 0f
        val itemp = gInventory.getItem(itemId) ?: return
        LLSettingsVOBase.getSettingsAsset(itemp.getAssetUUID()) { assetId, settings, status, _ ->
            onAssetLoadedForInsertion(itemId, assetId, settings, status, track, mCurrentTrack.toInt(), frame)
        }
    }

    private fun showHDRNotification(pday: LLSettingsDay?) {
        pday ?: return
        val shouldAutoAdjust = gSavedSettings.getBool("RenderSkyAutoAdjustLegacy") ?: false
        for (i in LLSettingsDay.TRACK_GROUND_LEVEL..LLSettingsDay.TRACK_MAX.toInt()) {
            for ((_, setting) in pday.getCycleTrack(i.toUInt())) {
                val sky = setting as? LLSettingsSky ?: continue
                if (shouldAutoAdjust && sky.canAutoAdjust() && sky.getReflectionProbeAmbiance(true) != 0f) {
                    LLNotificationsUtil.add("AutoAdjustHDRSky")
                    return
                }
            }
        }
    }

    private fun onAssetLoadedForInsertion(
        itemId: UUID?,
        assetId: UUID?,
        settings: LLSettingsBase?,
        status: Int,
        sourceTrack: Int,
        destTrack: Int,
        frame: Float
    ) {
        val cb = {
            if (settings?.getSettingsType() == "daycycle") {
                val pday = settings as? LLSettingsDay
                if (pday != null) {
                    if (destTrack.toUInt() == LLSettingsDay.TRACK_WATER) {
                        cloneTrack(pday, LLSettingsDay.TRACK_WATER, LLSettingsDay.TRACK_WATER)
                    } else {
                        cloneTrack(pday, sourceTrack.toUInt(), destTrack.toUInt())
                    }
                }
            } else {
                if (mFramesSlider?.canAddSliders() == false) return@let
                val destTrackU = destTrack.toUInt()
                val nearest = mEditDay?.getSettingsNearKeyframe(frame, destTrackU, LLSettingsDay.DEFAULT_FRAME_SLOP_FACTOR)
                if (nearest?.first != LLSettingsDay.INVALID_TRACKPOS) {
                    nearest?.first?.let { pos ->
                        mEditDay?.removeTrackKeyframe(destTrackU, pos)
                        removeSliderFrame(pos)
                    }
                }
                when (settings?.getSettingsType()) {
                    "sky" -> if (destTrackU != LLSettingsDay.TRACK_WATER) {
                        val clone = settings.buildDerivedClone()
                        mEditDay?.setSettingsAtKeyframe(clone, frame, destTrackU)
                        addSliderFrame(frame, clone, false)
                    }
                    "water" -> if (destTrackU == LLSettingsDay.TRACK_WATER) {
                        val clone = settings.buildDerivedClone()
                        mEditDay?.setSettingsAtKeyframe(clone, frame, destTrackU)
                        addSliderFrame(frame, clone, false)
                    }
                }
            }
            reblendSettings()
            synchronizeTabs()
        }

        if (settings == null || status != 0) return
        if (mEditDay == null) return

        val invItem = gInventory.getItem(itemId)
        if (invItem != null &&
            (!invItem.getPermissions().allowOperationBy(PERM_TRANSFER, gAgent.getID()) ||
             !invItem.getPermissions().allowOperationBy(PERM_COPY, gAgent.getID()))) {

            val noTransfer = if (mInventoryItem != null)
                !mInventoryItem!!.getPermissions().allowOperationBy(PERM_TRANSFER, gAgent.getID())
            else
                mEditDay?.getFlag(LLSettingsBase.FLAG_NOTRANS) == true

            if (!noTransfer) {
                LLNotificationsUtil.add("SettingsMakeNoTrans", LLSD(), LLSD()) { _, resp ->
                    if (LLNotificationsUtil.getSelectedOption(LLSD(), resp) == 0) {
                        mCanTrans = false
                        mEditDay?.setFlag(LLSettingsBase.FLAG_NOTRANS)
                        cb()
                    }
                }
                return
            }
        }

        cb()
    }

    // Suppress IDE warning – `let` label used inside lambda above
    private operator fun Unit.let(block: () -> Unit) = block()
}
