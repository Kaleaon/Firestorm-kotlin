package com.firestorm.newview

import java.util.UUID

private const val PREVIEW_BORDER_WIDTH = 2
private const val PREVIEW_RESIZE_HANDLE_SIZE = (RESIZE_HANDLE_WIDTH * OO_SQRT2).toInt() + PREVIEW_BORDER_WIDTH
private const val PREVIEW_HPAD = PREVIEW_RESIZE_HANDLE_SIZE
private const val PREVIEW_VPAD = 70
private const val PREF_BUTTON_HEIGHT = 16 + PREVIEW_VPAD
private const val PREVIEW_TEXTURE_HEIGHT = 300

private const val PREVIEW_CAMERA_DISTANCE = 4f
private const val MIN_CAMERA_ZOOM = 0.5f
private const val MAX_CAMERA_ZOOM = 10f
private const val BASE_ANIM_TIME_OFFSET = 5f

private val STATUS = arrayOf(
    "E_ST_OK", "E_ST_EOF", "E_ST_NO_CONSTRAINT", "E_ST_NO_FILE", "E_ST_NO_HIER",
    "E_ST_NO_JOINT", "E_ST_NO_NAME", "E_ST_NO_OFFSET", "E_ST_NO_CHANNELS",
    "E_ST_NO_ROTATION", "E_ST_NO_AXIS", "E_ST_NO_MOTION", "E_ST_NO_FRAMES",
    "E_ST_NO_FRAME_TIME", "E_ST_NO_POS", "E_ST_NO_ROT", "E_ST_NO_XLT_FILE",
    "E_ST_NO_XLT_HEADER", "E_ST_NO_XLT_NAME", "E_ST_NO_XLT_IGNORE",
    "E_ST_NO_XLT_RELATIVE", "E_ST_NO_XLT_OUTNAME", "E_ST_NO_XLT_MATRIX",
    "E_ST_NO_XLT_MERGECHILD", "E_ST_NO_XLT_MERGEPARENT", "E_ST_NO_XLT_PRIORITY",
    "E_ST_NO_XLT_LOOP", "E_ST_NO_XLT_EASEIN", "E_ST_NO_XLT_EASEOUT",
    "E_ST_NO_XLT_HAND", "E_ST_NO_XLT_EMOTE", "E_ST_BAD_ROOT"
)

class LLPreviewAnimation(width: Int, height: Int) : LLViewerDynamicTexture(width, height, 3, ORDER_MIDDLE, false) {

    private var mNeedsUpdate: Boolean = true
    private var mCameraDistance: Float = PREVIEW_CAMERA_DISTANCE
    private var mCameraYaw: Float = 0f
    private var mCameraPitch: Float = 0f
    private var mCameraZoom: Float = 1f
    private val mCameraOffset: LLVector3 = LLVector3()
    val mDummyAvatar: LLVOAvatar

    init {
        mDummyAvatar = gObjectList.createObjectViewer(
            LL_PCODE_LEGACY_AVATAR, gAgent.getRegion(), LLViewerObject.CO_FLAG_UI_AVATAR
        ) as LLVOAvatar
        mDummyAvatar.mSpecialRenderMode = 1
        mDummyAvatar.startMotion(ANIM_AGENT_STAND, BASE_ANIM_TIME_OFFSET)
        mDummyAvatar.updateOverallAppearance()
        mDummyAvatar.hideHair()
        mDummyAvatar.hideSkirt()
        mDummyAvatar.stopMotion(ANIM_AGENT_HEAD_ROT, true)
        mDummyAvatar.stopMotion(ANIM_AGENT_EYE, true)
        mDummyAvatar.stopMotion(ANIM_AGENT_BODY_NOISE, true)
        mDummyAvatar.stopMotion(ANIM_AGENT_BREATHE_ROT, true)
    }

    override fun getType(): Byte = LLViewerDynamicTexture.LL_PREVIEW_ANIMATION

    override fun needsUpdate(): Boolean = mNeedsUpdate

    fun render(): Boolean {
        mNeedsUpdate = false
        // no-op
        return false
    }

    fun requestUpdate() {
        mNeedsUpdate = true
    }

    fun rotate(yawRadians: Float, pitchRadians: Float) {
        mCameraYaw += yawRadians
        mCameraPitch = (mCameraPitch + pitchRadians).coerceIn(-F_PI_BY_TWO * 0.8f, F_PI_BY_TWO * 0.8f)
    }

    fun zoom(zoomDelta: Float) {
        setZoom(mCameraZoom + zoomDelta)
    }

    fun setZoom(zoomAmt: Float) {
        mCameraZoom = zoomAmt.coerceIn(MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
    }

    fun pan(right: Float, up: Float) {
        mCameraOffset.mV[VY] = (mCameraOffset.mV[VY] + right * mCameraDistance / mCameraZoom).coerceIn(-1f, 1f)
        mCameraOffset.mV[VZ] = (mCameraOffset.mV[VZ] + up * mCameraDistance / mCameraZoom).coerceIn(-1f, 1f)
    }

    fun getDummyAvatar(): LLVOAvatar = mDummyAvatar

    fun getPreviewAvatar(floaterp: LLFloaterBvhPreview?): LLVOAvatar {
        if (floaterp == null) return mDummyAvatar
        return if (floaterp.mUseOwnAvatar) gAgentAvatarp as LLVOAvatar else mDummyAvatar
    }

    override fun destroy() {
        mDummyAvatar.markDead()
        super.destroy()
    }
}

class LLFloaterBvhPreview(args: LLSD) : LLFloaterNameDesc(args) {

    var mUseOwnAvatar: Boolean = gSavedSettings.getBOOL("FSUploadAnimationOnOwnAvatar")
    private var mAOEnabled: Boolean = false

    private var mAnimPreview: LLPreviewAnimation? = null
    private var mLastMouseX: Int = 0
    private var mLastMouseY: Int = 0
    private lateinit var mPlayButton: LLButton
    private lateinit var mPauseButton: LLButton
    private lateinit var mStopButton: LLButton
    private val mPreviewRect: LLRect = LLRect()
    private val mPreviewImageRect: LLRectf = LLRectf()
    private var mMotionID: LLAssetID = LLAssetID()
    private var mTransactionID: LLTransactionID = LLTransactionID()
    private var mPauseRequest: LLAnimPauseRequest? = null
    private val mIDList: MutableMap<String, UUID> = mutableMapOf()
    private val mTimer: LLFrameTimer = LLFrameTimer()
    private var mNumFrames: Int = 0

    companion object {
        var sOwnAvatarInstanceCount: Int = 0
    }

    init {
        if (mUseOwnAvatar) {
            sOwnAvatarInstanceCount++
            mAOEnabled = gSavedPerAccountSettings.getBOOL("UseAO")
            if (mAOEnabled) {
                AOEngine.getInstance().enable(false)
            }
        }

        mIDList["Standing"] = ANIM_AGENT_STAND
        mIDList["Walking"] = ANIM_AGENT_FEMALE_WALK
        mIDList["Sitting"] = ANIM_AGENT_SIT_FEMALE
        mIDList["Flying"] = ANIM_AGENT_HOVER

        mIDList["[None]"] = UUID_NULL
        mIDList["Aaaaah"] = ANIM_AGENT_EXPRESS_OPEN_MOUTH
        mIDList["Afraid"] = ANIM_AGENT_EXPRESS_AFRAID
        mIDList["Angry"] = ANIM_AGENT_EXPRESS_ANGER
        mIDList["Big Smile"] = ANIM_AGENT_EXPRESS_TOOTHSMILE
        mIDList["Bored"] = ANIM_AGENT_EXPRESS_BORED
        mIDList["Cry"] = ANIM_AGENT_EXPRESS_CRY
        mIDList["Disdain"] = ANIM_AGENT_EXPRESS_DISDAIN
        mIDList["Embarrassed"] = ANIM_AGENT_EXPRESS_EMBARRASSED
        mIDList["Frown"] = ANIM_AGENT_EXPRESS_FROWN
        mIDList["Kiss"] = ANIM_AGENT_EXPRESS_KISS
        mIDList["Laugh"] = ANIM_AGENT_EXPRESS_LAUGH
        mIDList["Plllppt"] = ANIM_AGENT_EXPRESS_TONGUE_OUT
        mIDList["Repulsed"] = ANIM_AGENT_EXPRESS_REPULSED
        mIDList["Sad"] = ANIM_AGENT_EXPRESS_SAD
        mIDList["Shrug"] = ANIM_AGENT_EXPRESS_SHRUG
        mIDList["Smile"] = ANIM_AGENT_EXPRESS_SMILE
        mIDList["Surprise"] = ANIM_AGENT_EXPRESS_SURPRISE
        mIDList["Wink"] = ANIM_AGENT_EXPRESS_WINK
        mIDList["Worry"] = ANIM_AGENT_EXPRESS_WORRY

        mTimer.stop()
    }

    private fun setAnimCallbacks() {
        getChild<LLUICtrl>("playback_slider").setCommitCallback { onSliderMove() }
        getChild<LLUICtrl>("preview_base_anim").setCommitCallback { onCommitBaseAnim() }
        getChild<LLUICtrl>("priority").setCommitCallback { onCommitPriority() }
        getChild<LLUICtrl>("loop_check").setCommitCallback { onCommitLoop() }
        getChild<LLUICtrl>("loop_in_point").setCommitCallback { onCommitLoopIn() }
        getChild<LLUICtrl>("loop_in_point").setValidateBeforeCommit { validateLoopIn(it) }
        getChild<LLUICtrl>("loop_out_point").setCommitCallback { onCommitLoopOut() }
        getChild<LLUICtrl>("loop_out_point").setValidateBeforeCommit { validateLoopOut(it) }
        getChild<LLUICtrl>("loop_in_frames").setCommitCallback { onCommitLoopInFrames() }
        getChild<LLUICtrl>("loop_in_frames").setValidateBeforeCommit { validateLoopInFrames(it) }
        getChild<LLUICtrl>("loop_out_frames").setCommitCallback { onCommitLoopOutFrames() }
        getChild<LLUICtrl>("loop_out_frames").setValidateBeforeCommit { validateLoopOutFrames(it) }
        getChild<LLUICtrl>("hand_pose_combo").setCommitCallback { onCommitHandPose() }
        getChild<LLUICtrl>("emote_combo").setCommitCallback { onCommitEmote() }
        getChild<LLUICtrl>("ease_in_time").setCommitCallback { onCommitEaseIn() }
        getChild<LLUICtrl>("ease_in_time").setValidateBeforeCommit { validateEaseIn(it) }
        getChild<LLUICtrl>("ease_out_time").setCommitCallback { onCommitEaseOut() }
        getChild<LLUICtrl>("ease_out_time").setValidateBeforeCommit { validateEaseOut(it) }
    }

    private fun getJointAliases(): MutableMap<String, String> {
        val av = mAnimPreview!!.getDummyAvatar()
        return av.getJointAliases()
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false

        getChild<LLUICtrl>("name_form").setCommitCallback { onCommitName() }

        childSetAction("reload_btn") { onBtnReload(this) }
        childSetAction("ok_btn") { onBtnOK(this) }
        setDefaultBtn()

        if (!mUseOwnAvatar) {
            val rect = getRect()
            translate(0, PREVIEW_TEXTURE_HEIGHT - 30)
            reshape(rect.getWidth(), rect.getHeight() + PREVIEW_TEXTURE_HEIGHT - 30)
            mPreviewRect.set(
                PREVIEW_HPAD,
                PREVIEW_TEXTURE_HEIGHT + PREVIEW_VPAD,
                getRect().getWidth() - PREVIEW_HPAD,
                PREVIEW_HPAD + PREF_BUTTON_HEIGHT + PREVIEW_HPAD
            )
            mPreviewImageRect.set(0f, 1f, 1f, 0f)
        }

        mPlayButton = getChild<LLButton>("play_btn")
        mPlayButton.setClickedCallback { onBtnPlay() }

        mPauseButton = getChild<LLButton>("pause_btn")
        mPauseButton.setClickedCallback { onBtnPause() }

        mStopButton = getChild<LLButton>("stop_btn")
        mStopButton.setClickedCallback { onBtnStop() }

        val spinner = getChild<LLSpinCtrl>("priority")
        var maxValue = gSavedSettings.getS32("FSMaxAnimationPriority")
        if (maxValue > 6) maxValue = 6
        if (maxValue < 0) maxValue = 0
        spinner.setMaxValue(maxValue.toFloat())

        setAnimCallbacks()
        loadBVH()

        return true
    }

    private fun loadBVH(): Boolean {
        var motionp: LLKeyframeMotion? = null
        var loaderp: LLBVHLoader? = null

        mPlayButton.setVisible(true)
        mPauseButton.setVisible(false)

        getChildView("bad_animation_text").setVisible(false)

        mAnimPreview = LLPreviewAnimation(256, 256)

        val exten = gDirUtilp.getExtension(mFilename)
        if (exten == "bvh") {
            System.err.println("LLFloaterBvhPreview: loadBVH not yet implemented")
        }

        if (loaderp != null && loaderp.isInitialized() && loaderp.getDuration() <= MAX_ANIM_DURATION) {
            mTransactionID.generate()
            mMotionID = mTransactionID.makeAssetID(gAgent.getSecureSessionID())

            mNumFrames = loaderp.getNumFrames()
            getChild<LLSpinCtrl>("loop_in_frames").setMaxValue(LLSD(mNumFrames.toFloat()))
            getChild<LLSpinCtrl>("loop_out_frames").setMaxValue(LLSD(mNumFrames.toFloat()))
            getChild<LLUICtrl>("loop_in_frames").setValue(
                LLSD(getChild<LLUICtrl>("loop_in_point").getValue().asReal().toFloat() / 100f * mNumFrames.toFloat())
            )
            getChild<LLUICtrl>("loop_out_frames").setValue(
                LLSD(getChild<LLUICtrl>("loop_out_point").getValue().asReal().toFloat() / 100f * mNumFrames.toFloat())
            )

            val outStr = LLUIString(getString("FS_report_frames"))
            outStr.setArg("[F]", mNumFrames.toString())
            outStr.setArg("[S]", "%.1f".format(loaderp.getDuration()))
            outStr.setArg("[FPS]", "%.1f".format(mNumFrames.toFloat() / loaderp.getDuration()))
            getChild<LLUICtrl>("frames_label").setValue(LLSD(outStr.toString()))

            motionp = mAnimPreview!!.getPreviewAvatar(this).createMotion(mMotionID) as? LLKeyframeMotion

            val bufferSize = loaderp.getOutputSize()
            val buffer = ByteArray(bufferSize)
            val dp = LLDataPackerBinaryBuffer(buffer, bufferSize)

            loaderp.serialize(dp)
            dp.reset()
            val success = motionp != null && motionp.deserialize(dp, mMotionID, false)

            if (success) {
                val pelvisBbox = motionp!!.getPelvisBBox()
                val temp = pelvisBbox.getCenter()
                val pelvisOffset = temp.magVec()
                val tempExtent = pelvisBbox.getExtent()
                val pelvisMaxDisplacement = pelvisOffset + (tempExtent.magVec() * 0.5f) + 1f

                val cameraZoom = LLViewerCamera.getInstance().getDefaultFOV() /
                        (2f * Math.atan((pelvisMaxDisplacement / PREVIEW_CAMERA_DISTANCE).toDouble()).toFloat())
                mAnimPreview!!.setZoom(cameraZoom)

                motionp.setName(getChild<LLUICtrl>("name_form").getValue().asString())
                onBtnPlay()

                getChild<LLSliderCtrl>("playback_slider").setMinValue(0.0)
                getChild<LLSliderCtrl>("playback_slider").setMaxValue(1.0)

                motionp.setLoop(getChild<LLUICtrl>("loop_check").getValue().asBoolean())
                motionp.setLoopIn(
                    getChild<LLUICtrl>("loop_in_point").getValue().asReal().toFloat() / 100f * motionp.getDuration()
                )
                motionp.setLoopOut(
                    getChild<LLUICtrl>("loop_out_point").getValue().asReal().toFloat() / 100f * motionp.getDuration()
                )
                motionp.setPriority(getChild<LLUICtrl>("priority").getValue().asInteger())
                motionp.setHandPose(LLHandMotion.getHandPose(getChild<LLUICtrl>("hand_pose_combo").getValue().asString()))

                var easeIn = getChild<LLUICtrl>("ease_in_time").getValue().asReal().toFloat()
                var easeOut = getChild<LLUICtrl>("ease_out_time").getValue().asReal().toFloat()
                if (motionp.getDuration() != 0f &&
                    easeIn + easeOut > motionp.getDuration() &&
                    !getChild<LLUICtrl>("loop_check").getValue().asBoolean()
                ) {
                    val factor = motionp.getDuration() / (easeIn + easeOut)
                    easeIn *= factor
                    easeOut *= factor
                    getChild<LLUICtrl>("ease_in_time").setValue(LLSD(easeIn))
                    getChild<LLUICtrl>("ease_out_time").setValue(LLSD(easeOut))
                }
                motionp.setEaseIn(easeIn)
                motionp.setEaseOut(easeOut)

                setEnabled(true)
                setTitle("$mFilename - ${"%.2f".format(motionp.getDuration())} seconds")
            } else {
                mAnimPreview = null
                mMotionID.setNull()
                getChild<LLUICtrl>("bad_animation_text").setValue(getString("failed_to_initialize"))
            }
        } else {
            if (loaderp != null) {
                if (loaderp.getDuration() > MAX_ANIM_DURATION) {
                    val outStr = LLUIString(getString("anim_too_long"))
                    outStr.setArg("[LENGTH]", "%.1f".format(loaderp.getDuration()))
                    outStr.setArg("[MAX_LENGTH]", "%.1f".format(MAX_ANIM_DURATION))
                    getChild<LLUICtrl>("bad_animation_text").setValue(outStr.toString())
                } else {
                    val outStr = LLUIString(getString("failed_file_read"))
                    outStr.setArg("[STATUS]", getString(STATUS[loaderp.getStatus()]))
                    getChild<LLUICtrl>("bad_animation_text").setValue(outStr.toString())
                }
            }
            mMotionID.setNull()
            mAnimPreview = null
        }

        refresh()
        return true
    }

    private fun unloadMotion() {
        if (mMotionID.notNull() && mAnimPreview != null && mUseOwnAvatar) {
            resetMotion()
            mAnimPreview!!.getPreviewAvatar(this).removeMotion(mMotionID)
            LLKeyframeDataCache.removeKeyframeData(mMotionID)
        }
        mMotionID.setNull()
        mAnimPreview = null
    }

    override fun destroy() {
        mAnimPreview = null

        if (mUseOwnAvatar) {
            sOwnAvatarInstanceCount--

            if (mMotionID.notNull()) {
                LLKeyframeDataCache.removeKeyframeData(mMotionID)
            }

            if (sOwnAvatarInstanceCount == 0 && isAgentAvatarValid()) {
                gAgentAvatarp!!.deactivateAllMotions()
                gAgentAvatarp!!.startDefaultMotions()
                gAgentAvatarp!!.startMotion(ANIM_AGENT_STAND)
            }

            if (mAOEnabled) {
                AOEngine.getInstance().enable(true)
            }
        }

        setEnabled(false)
        super.destroy()
    }

    protected fun draw() {
        super.draw()
        val r = getRect()

        refresh()

        val preview = mAnimPreview
        if (mMotionID.notNull() && preview != null) {
            if (!mUseOwnAvatar) {
                // no-op
            }
            val avatarp = preview.getPreviewAvatar(this)
            if (!avatarp.areAnimationsPaused()) {
                preview.requestUpdate()
            }
            if (mTimer.getStarted() && mTimer.hasExpired()) {
                mTimer.stop()
                mPauseRequest = avatarp.requestPause()
            }
        }
    }

    protected fun resetMotion() {
        val preview = mAnimPreview ?: return
        val avatarp = preview.getPreviewAvatar(this)
        val paused = avatarp.areAnimationsPaused()

        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion
        if (motionp != null) {
            val emote = getChild<LLUICtrl>("emote_combo").getValue().asString()
            motionp.setEmote(mIDList[emote] ?: UUID_NULL)
        }

        val baseId = mIDList[getChild<LLUICtrl>("preview_base_anim").getValue().asString()] ?: UUID_NULL
        avatarp.deactivateAllMotions()
        avatarp.startMotion(mMotionID, 0f)
        avatarp.startMotion(baseId, BASE_ANIM_TIME_OFFSET)
        getChild<LLUICtrl>("playback_slider").setValue(0f)

        val handpose = getChild<LLUICtrl>("hand_pose_combo").getValue().asString()
        avatarp.startMotion(ANIM_AGENT_HAND_MOTION, 0f)
        motionp?.setHandPose(LLHandMotion.getHandPose(handpose))

        mPauseRequest = if (paused) avatarp.requestPause() else null
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (mUseOwnAvatar) return super.handleMouseDown(x, y, mask)
        if (mPreviewRect.pointInRect(x, y)) {
            bringToFront(x, y)
            gFocusMgr.setMouseCapture(this)
            gViewerWindow.hideCursor()
            mLastMouseX = x
            mLastMouseY = y
            return true
        }
        return super.handleMouseDown(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (mUseOwnAvatar) return super.handleMouseUp(x, y, mask)
        gFocusMgr.setMouseCapture(null)
        gViewerWindow.showCursor()
        return super.handleMouseUp(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (!mUseOwnAvatar) {
            val localMask = mask and MASK_ALT.inv()
            val preview = mAnimPreview
            if (preview != null && hasMouseCapture()) {
                when {
                    localMask == MASK_PAN -> preview.pan(
                        (x - mLastMouseX) * -0.005f,
                        (y - mLastMouseY) * -0.005f
                    )
                    localMask == MASK_ORBIT -> {
                        preview.rotate((x - mLastMouseX) * -0.01f, (y - mLastMouseY) * 0.02f)
                    }
                    else -> {
                        preview.rotate((x - mLastMouseX) * -0.01f, 0f)
                        preview.zoom((y - mLastMouseY) * 0.02f)
                    }
                }
                preview.requestUpdate()
                LLUI.getInstance().setMousePositionLocal(this, mLastMouseX, mLastMouseY)
            }

            if (!mPreviewRect.pointInRect(x, y) || preview == null) {
                return super.handleHover(x, y, mask)
            } else {
                val localMask = mask and MASK_ALT.inv()
                when {
                    localMask == MASK_ORBIT -> gViewerWindow.setCursor(UI_CURSOR_TOOLCAMERA)
                    localMask == MASK_PAN -> gViewerWindow.setCursor(UI_CURSOR_TOOLPAN)
                    else -> gViewerWindow.setCursor(UI_CURSOR_TOOLZOOMIN)
                }
            }
        }
        return true
    }

    override fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (mUseOwnAvatar) return false
        val preview = mAnimPreview ?: return false
        preview.zoom(clicks * -0.2f)
        preview.requestUpdate()
        return true
    }

    override fun onMouseCaptureLost() {
        if (!mUseOwnAvatar) {
            gViewerWindow.showCursor()
        }
    }

    fun onBtnPlay() {
        if (!getEnabled()) return
        val preview = mAnimPreview ?: return
        if (!mMotionID.notNull()) return
        val avatarp = preview.getPreviewAvatar(this)
        if (!avatarp.isMotionActive(mMotionID)) {
            resetMotion()
            mPauseRequest = null
        } else if (avatarp.areAnimationsPaused()) {
            mPauseRequest = null
        }
    }

    fun onBtnPause() {
        if (!getEnabled()) return
        val preview = mAnimPreview ?: return
        if (!mMotionID.notNull()) return
        val avatarp = preview.getPreviewAvatar(this)
        if (avatarp.isMotionActive(mMotionID) && !avatarp.areAnimationsPaused()) {
            mPauseRequest = avatarp.requestPause()
        }
    }

    fun onBtnStop() {
        if (!getEnabled()) return
        val preview = mAnimPreview ?: return
        if (!mMotionID.notNull()) return
        val avatarp = preview.getPreviewAvatar(this)
        resetMotion()
        mPauseRequest = avatarp.requestPause()
    }

    fun onSliderMove() {
        if (!getEnabled()) return
        val preview = mAnimPreview ?: return
        val avatarp = preview.getPreviewAvatar(this)
        val sliderValue = getChild<LLUICtrl>("playback_slider").getValue().asReal().toFloat()
        val baseId = mIDList[getChild<LLUICtrl>("preview_base_anim").getValue().asString()] ?: UUID_NULL
        val motionp = avatarp.findMotion(mMotionID) ?: return
        val duration = motionp.getDuration()
        val deltaTime = duration * sliderValue
        avatarp.deactivateAllMotions()
        avatarp.startMotion(baseId, deltaTime + BASE_ANIM_TIME_OFFSET)
        avatarp.startMotion(mMotionID, deltaTime)
        mPauseRequest = null
        mTimer.resetWithExpiry(0.001f)
        mTimer.start()
        refresh()
    }

    fun onCommitBaseAnim() {
        if (!getEnabled()) return
        val preview = mAnimPreview ?: return
        val avatarp = preview.getPreviewAvatar(this)
        val paused = avatarp.areAnimationsPaused()
        avatarp.stopMotion(mIDList["Standing"] ?: UUID_NULL, true)
        avatarp.stopMotion(mIDList["Walking"] ?: UUID_NULL, true)
        avatarp.stopMotion(mIDList["Sitting"] ?: UUID_NULL, true)
        avatarp.stopMotion(mIDList["Flying"] ?: UUID_NULL, true)
        resetMotion()
        if (!paused) {
            mPauseRequest = null
        }
    }

    fun onCommitLoop() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        motionp.setLoop(getChild<LLUICtrl>("loop_check").getValue().asBoolean())
        motionp.setLoopIn(getChild<LLUICtrl>("loop_in_point").getValue().asReal().toFloat() * 0.01f * motionp.getDuration())
        motionp.setLoopOut(getChild<LLUICtrl>("loop_out_point").getValue().asReal().toFloat() * 0.01f * motionp.getDuration())
    }

    fun onCommitLoopIn() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        getChild<LLUICtrl>("loop_in_frames").setValue(
            LLSD(getChild<LLUICtrl>("loop_in_point").getValue().asReal().toFloat() / 100f * mNumFrames.toFloat())
        )
        resetMotion()
        getChild<LLUICtrl>("loop_check").setValue(LLSD(true))
        onCommitLoop()
    }

    fun onCommitLoopOut() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        getChild<LLUICtrl>("loop_out_frames").setValue(
            LLSD(getChild<LLUICtrl>("loop_out_point").getValue().asReal().toFloat() / 100f * mNumFrames.toFloat())
        )
        resetMotion()
        getChild<LLUICtrl>("loop_check").setValue(LLSD(true))
        onCommitLoop()
    }

    fun onCommitLoopInFrames() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        getChild<LLUICtrl>("loop_in_point").setValue(
            LLSD(if (mNumFrames == 0) 0f else 100f * getChild<LLUICtrl>("loop_in_frames").getValue().asReal().toFloat() / mNumFrames.toFloat())
        )
        resetMotion()
        getChild<LLUICtrl>("loop_check").setValue(LLSD(true))
        onCommitLoop()
    }

    fun onCommitLoopOutFrames() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        getChild<LLUICtrl>("loop_out_point").setValue(
            LLSD(if (mNumFrames == 0) 100f else 100f * getChild<LLUICtrl>("loop_out_frames").getValue().asReal().toFloat() / mNumFrames.toFloat())
        )
        resetMotion()
        getChild<LLUICtrl>("loop_check").setValue(LLSD(true))
        onCommitLoop()
    }

    fun onCommitName() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion
        motionp?.setName(getChild<LLUICtrl>("name_form").getValue().asString())
        doCommit()
    }

    fun onCommitHandPose() {
        if (!getEnabled()) return
        resetMotion()
    }

    fun onCommitEmote() {
        if (!getEnabled()) return
        resetMotion()
    }

    fun onCommitPriority() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        motionp.setPriority(Math.floor(getChild<LLUICtrl>("priority").getValue().asReal()).toInt())
    }

    fun onCommitEaseIn() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        motionp.setEaseIn(getChild<LLUICtrl>("ease_in_time").getValue().asReal().toFloat())
        resetMotion()
    }

    fun onCommitEaseOut() {
        if (!getEnabled() || mAnimPreview == null) return
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return
        motionp.setEaseOut(getChild<LLUICtrl>("ease_out_time").getValue().asReal().toFloat())
        resetMotion()
    }

    fun validateEaseIn(data: LLSD): Boolean {
        if (!getEnabled() || mAnimPreview == null) return false
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return true
        if (!motionp.getLoop()) {
            val newEaseIn = getChild<LLUICtrl>("ease_in_time").getValue().asReal().toFloat()
                .coerceIn(0f, motionp.getDuration() - motionp.getEaseOutDuration())
            getChild<LLUICtrl>("ease_in_time").setValue(LLSD(newEaseIn))
        }
        return true
    }

    fun validateEaseOut(data: LLSD): Boolean {
        if (!getEnabled() || mAnimPreview == null) return false
        val avatarp = mAnimPreview!!.getPreviewAvatar(this)
        val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion ?: return true
        if (!motionp.getLoop()) {
            val newEaseOut = getChild<LLUICtrl>("ease_out_time").getValue().asReal().toFloat()
                .coerceIn(0f, motionp.getDuration() - motionp.getEaseInDuration())
            getChild<LLUICtrl>("ease_out_time").setValue(LLSD(newEaseOut))
        }
        return true
    }

    fun validateLoopIn(data: LLSD): Boolean {
        if (!getEnabled()) return false
        var loopIn = getChild<LLUICtrl>("loop_in_point").getValue().asReal().toFloat()
        val loopOut = getChild<LLUICtrl>("loop_out_point").getValue().asReal().toFloat()
        loopIn = when {
            loopIn < 0f -> 0f
            loopIn > 100f -> 100f
            loopIn > loopOut -> loopOut
            else -> loopIn
        }
        getChild<LLUICtrl>("loop_in_point").setValue(LLSD(loopIn))
        getChild<LLUICtrl>("loop_in_frames").setValue(LLSD(Math.round(loopIn / 100f * mNumFrames.toFloat())))
        return true
    }

    fun validateLoopOut(data: LLSD): Boolean {
        if (!getEnabled()) return false
        var loopOut = getChild<LLUICtrl>("loop_out_point").getValue().asReal().toFloat()
        val loopIn = getChild<LLUICtrl>("loop_in_point").getValue().asReal().toFloat()
        loopOut = when {
            loopOut < 0f -> 0f
            loopOut > 100f -> 100f
            loopOut < loopIn -> loopIn
            else -> loopOut
        }
        getChild<LLUICtrl>("loop_out_point").setValue(LLSD(loopOut))
        getChild<LLUICtrl>("loop_out_frames").setValue(LLSD(Math.round(loopOut / 100f * mNumFrames.toFloat())))
        return true
    }

    fun validateLoopInFrames(data: LLSD): Boolean {
        if (!getEnabled()) return false
        var loopIn = getChild<LLUICtrl>("loop_in_frames").getValue().asReal().toFloat()
        val loopOut = getChild<LLUICtrl>("loop_out_frames").getValue().asReal().toFloat()
        loopIn = when {
            loopIn < 0f -> 0f
            loopIn > 1000f -> 1000f
            loopIn > loopOut -> loopOut
            else -> loopIn
        }
        getChild<LLUICtrl>("loop_in_frames").setValue(LLSD(loopIn))
        getChild<LLUICtrl>("loop_in_point").setValue(LLSD(if (mNumFrames == 0) 0f else 100f * loopIn / mNumFrames.toFloat()))
        return true
    }

    fun validateLoopOutFrames(data: LLSD): Boolean {
        if (!getEnabled()) return false
        var loopOut = getChild<LLUICtrl>("loop_out_frames").getValue().asReal().toFloat()
        val loopIn = getChild<LLUICtrl>("loop_in_frames").getValue().asReal().toFloat()
        loopOut = when {
            loopOut < 0f -> 0f
            loopOut > 1000f -> 1000f
            loopOut < loopIn -> loopIn
            else -> loopOut
        }
        getChild<LLUICtrl>("loop_out_frames").setValue(LLSD(loopOut))
        getChild<LLUICtrl>("loop_out_point").setValue(LLSD(if (mNumFrames == 0) 100f else 100f * loopOut / mNumFrames.toFloat()))
        return true
    }

    fun refresh() {
        var showPlay = true
        val preview = mAnimPreview
        if (preview == null) {
            getChildView("bad_animation_text").setVisible(true)
            mPlayButton.setEnabled(false)
            mStopButton.setEnabled(false)
            getChildView("ok_btn").setEnabled(false)
        } else {
            getChildView("bad_animation_text").setVisible(false)
            mPlayButton.setEnabled(true)
            mStopButton.setEnabled(true)
            val avatarp = preview.getPreviewAvatar(this)
            if (avatarp.isMotionActive(mMotionID)) {
                mStopButton.setEnabled(true)
                val motionp = avatarp.findMotion(mMotionID) as? LLKeyframeMotion
                if (!avatarp.areAnimationsPaused()) {
                    if (motionp != null) {
                        val fractionComplete = motionp.getLastUpdateTime() / motionp.getDuration()
                        getChild<LLUICtrl>("playback_slider").setValue(fractionComplete)
                    }
                    showPlay = false
                }
            } else {
                mPauseRequest = avatarp.requestPause()
            }
            getChildView("ok_btn").setEnabled(true)
            preview.requestUpdate()
        }
        mPlayButton.setVisible(showPlay)
        mPauseButton.setVisible(!showPlay)
    }

    companion object {
        fun onBtnOK(floaterp: LLFloaterBvhPreview) {
            if (!floaterp.getEnabled()) return
            val preview = floaterp.mAnimPreview ?: return
            val motionp = preview.getPreviewAvatar(floaterp).findMotion(floaterp.mMotionID) as? LLKeyframeMotion
            if (motionp == null) {
                LLNotificationsUtil.add("WriteAnimationFail")
                return
            }

            val fileSize = motionp.getFileSize()
            val buffer = ByteArray(fileSize)
            val dp = LLDataPackerBinaryBuffer(buffer, fileSize)

            if (motionp.serialize(dp)) {
                val file = LLFileSystem(motionp.getID(), LLAssetType.AT_ANIMATION, LLFileSystem.APPEND)
                val size = dp.getCurrentSize()
                if (file.write(buffer, size)) {
                    val name = floaterp.getChild<LLUICtrl>("name_form").getValue().asString()
                    val desc = floaterp.getChild<LLUICtrl>("description_form").getValue().asString()
                    val expectedUploadCost = LLAgentBenefitsMgr.current().getAnimationUploadCost()

                    val assetUploadInfo = LLResourceUploadInfo(
                        floaterp.mTransactionID, LLAssetType.AT_ANIMATION,
                        name, desc, 0,
                        LLFolderType.FT_NONE, LLInventoryType.IT_ANIMATION,
                        LLFloaterPerms.getNextOwnerPerms("Uploads"),
                        LLFloaterPerms.getGroupPerms("Uploads"),
                        LLFloaterPerms.getEveryonePerms("Uploads"),
                        expectedUploadCost,
                        floaterp.mDestinationFolderId
                    )
                    uploadNewResource(assetUploadInfo)
                } else {
                    LLNotificationsUtil.add("WriteAnimationFail")
                }
            }

            floaterp.resetMotion()
            preview.getPreviewAvatar(floaterp).removeMotion(floaterp.mMotionID)
            LLKeyframeDataCache.removeKeyframeData(floaterp.mMotionID)
            floaterp.mMotionID.setNull()

            floaterp.closeFloater(false)
        }

        fun onBtnReload(floaterp: LLFloaterBvhPreview) {
            if (!floaterp.getEnabled()) return
            floaterp.unloadMotion()
            floaterp.loadBVH()
            floaterp.resetMotion()
        }
    }
}
