package com.firestorm.newview

import kotlin.math.PI
import kotlin.math.atan

private const val PREVIEW_BORDER_WIDTH = 2
private const val PREVIEW_RESIZE_HANDLE_SIZE = (RESIZE_HANDLE_WIDTH * OO_SQRT2).toInt() + PREVIEW_BORDER_WIDTH
private const val PREVIEW_HPAD = PREVIEW_RESIZE_HANDLE_SIZE
private const val PREVIEW_VPAD = 70
private const val PREF_BUTTON_HEIGHT = 16 + PREVIEW_VPAD
private const val PREVIEW_TEXTURE_HEIGHT = 300

private const val PREVIEW_CAMERA_DISTANCE = 4.0f
private const val MIN_CAMERA_ZOOM = 0.5f
private const val MAX_CAMERA_ZOOM = 10.0f
private const val BASE_ANIM_TIME_OFFSET = 5.0f

private val BVH_STATUS_NAMES = arrayOf(
    "E_ST_OK", "E_ST_EOF", "E_ST_NO_CONSTRAINT", "E_ST_NO_FILE",
    "E_ST_NO_HIER", "E_ST_NO_JOINT", "E_ST_NO_NAME", "E_ST_NO_OFFSET",
    "E_ST_NO_CHANNELS", "E_ST_NO_ROTATION", "E_ST_NO_AXIS", "E_ST_NO_MOTION",
    "E_ST_NO_FRAMES", "E_ST_NO_FRAME_TIME", "E_ST_NO_POS", "E_ST_NO_ROT",
    "E_ST_NO_XLT_FILE", "E_ST_NO_XLT_HEADER", "E_ST_NO_XLT_NAME",
    "E_ST_NO_XLT_IGNORE", "E_ST_NO_XLT_RELATIVE", "E_ST_NO_XLT_OUTNAME",
    "E_ST_NO_XLT_MATRIX", "E_ST_NO_XLT_MERGECHILD", "E_ST_NO_XLT_MERGEPARENT",
    "E_ST_NO_XLT_PRIORITY", "E_ST_NO_XLT_LOOP", "E_ST_NO_XLT_EASEIN",
    "E_ST_NO_XLT_EASEOUT", "E_ST_NO_XLT_HAND", "E_ST_NO_XLT_EMOTE",
    "E_ST_BAD_ROOT"
)

// Stub types representing SL viewer concepts not yet available on JVM
class LLVOAvatar
class LLKeyframeMotion
class LLPreviewAnimationHandle
class LLUUID {
    fun notNull(): Boolean = TODO("GPU: UUID null check")
    fun setNull() = TODO("GPU: UUID setNull")
}
class LLAssetID
class LLTransactionID {
    fun generate() = TODO("APR: use JVM equivalent")
    fun makeAssetID(sessionId: Any): LLUUID = TODO("APR: use JVM equivalent")
}
class AnimPauseRequest

open class Floater(val key: Any)
open class FloaterNameDesc(key: Any) : Floater(key)

class PreviewAnimation(val width: Int, val height: Int) {
    var needsUpdate: Boolean = true
    var cameraDistance: Float = PREVIEW_CAMERA_DISTANCE
    var cameraYaw: Float = 0.0f
    var cameraPitch: Float = 0.0f
    var cameraZoom: Float = 1.0f
    var cameraOffset: FloatArray = FloatArray(3)

    val dummyAvatar: LLVOAvatar = TODO("GPU: create dummy avatar")

    fun getType(): Byte = TODO("GPU: LLViewerDynamicTexture type")

    fun render(): Boolean {
        needsUpdate = false
        TODO("GPU: render animation preview via OpenGL")
    }

    fun requestUpdate() {
        needsUpdate = true
    }

    fun rotate(yawRadians: Float, pitchRadians: Float) {
        cameraYaw += yawRadians
        cameraPitch = (cameraPitch + pitchRadians).coerceIn(
            (-PI / 2 * 0.8).toFloat(),
            (PI / 2 * 0.8).toFloat()
        )
    }

    fun zoom(delta: Float) {
        setZoom(cameraZoom + delta)
    }

    fun setZoom(amount: Float) {
        cameraZoom = amount.coerceIn(MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
    }

    fun pan(right: Float, up: Float) {
        cameraOffset[1] = (cameraOffset[1] + right * cameraDistance / cameraZoom).coerceIn(-1.0f, 1.0f)
        cameraOffset[2] = (cameraOffset[2] + up * cameraDistance / cameraZoom).coerceIn(-1.0f, 1.0f)
    }

    fun getPreviewAvatar(floater: FloaterBvhPreview?): LLVOAvatar {
        if (floater == null) return dummyAvatar
        return if (floater.useOwnAvatar) TODO("GPU: gAgentAvatarp") else dummyAvatar
    }
}

class FloaterBvhPreview(key: Any) : FloaterNameDesc(key) {
    private var animPreview: PreviewAnimation? = null
    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0
    private var playButton: Any? = null
    private var pauseButton: Any? = null
    private var stopButton: Any? = null
    private var previewRect: Any? = null
    private var previewImageRect: Any? = null
    var motionID: LLUUID = LLUUID()
    private var transactionID: LLTransactionID = LLTransactionID()
    private var pauseRequest: AnimPauseRequest? = null

    private val idList: MutableMap<String, LLUUID> = mutableMapOf()

    internal var useOwnAvatar: Boolean = false
    private var aoEnabled: Boolean = false
    private var numFrames: Int = 0

    companion object {
        var ownAvatarInstanceCount: Int = 0
    }

    init {
        useOwnAvatar = savedSettings("FSUploadAnimationOnOwnAvatar")
        if (useOwnAvatar) {
            ownAvatarInstanceCount++
            aoEnabled = savedPerAccountSettings("UseAO")
            if (aoEnabled) {
                TODO("APR: AOEngine.getInstance().enable(false)")
            }
        }

        idList["Standing"] = animAgentStand()
        idList["Walking"] = animAgentFemaleWalk()
        idList["Sitting"] = animAgentSitFemale()
        idList["Flying"] = animAgentHover()

        idList["[None]"] = nullUUID()
        idList["Aaaaah"] = animAgentExpressOpenMouth()
        idList["Afraid"] = animAgentExpressAfraid()
        idList["Angry"] = animAgentExpressAnger()
        idList["Big Smile"] = animAgentExpressToothsmile()
        idList["Bored"] = animAgentExpressBored()
        idList["Cry"] = animAgentExpressCry()
        idList["Disdain"] = animAgentExpressDisdain()
        idList["Embarrassed"] = animAgentExpressEmbarrassed()
        idList["Frown"] = animAgentExpressFrown()
        idList["Kiss"] = animAgentExpressKiss()
        idList["Laugh"] = animAgentExpressLaugh()
        idList["Plllppt"] = animAgentExpressTongueOut()
        idList["Repulsed"] = animAgentExpressRepulsed()
        idList["Sad"] = animAgentExpressSad()
        idList["Shrug"] = animAgentExpressShrug()
        idList["Smile"] = animAgentExpressSmile()
        idList["Surprise"] = animAgentExpressSurprise()
        idList["Wink"] = animAgentExpressWink()
        idList["Worry"] = animAgentExpressWorry()
    }

    fun postBuild(): Boolean {
        setAnimCallbacks()
        loadBVH()
        return true
    }

    private fun setAnimCallbacks() {
        TODO("APR: bind UI callbacks for playback_slider, priority, loop_check, etc.")
    }

    private fun getJointAliases(): MutableMap<String, String> {
        val av = animPreview?.dummyAvatar
        return TODO("GPU: avatar.getJointAliases()")
    }

    fun loadBVH(): Boolean {
        var motionp: LLKeyframeMotion? = null

        setChildVisible("bad_animation_text", false)
        animPreview = PreviewAnimation(256, 256)

        val extension = getFileExtension(filename)
        if (extension == "bvh") {
            TODO("APR: use JVM file I/O to read BVH file; LLAPRFile → java.io.File")
        }

        if (motionp != null && isLoaderInitialized() && loaderDuration() <= MAX_ANIM_DURATION) {
            transactionID.generate()
            motionID = transactionID.makeAssetID(agentSecureSessionID())

            numFrames = loaderNumFrames()
            setSpinnerMax("loop_in_frames", numFrames.toFloat())
            setSpinnerMax("loop_out_frames", numFrames.toFloat())
            syncLoopFrameSpinners()

            val pelvisMaxDisplacement = calcPelvisMaxDisplacement()
            val defaultFov = TODO("GPU: LLViewerCamera.getInstance().getDefaultFOV()")
            val cameraZoom = (defaultFov as Float) / (2.0f * atan(pelvisMaxDisplacement / PREVIEW_CAMERA_DISTANCE).toFloat())
            animPreview!!.setZoom(cameraZoom)

            setMotionName(getChildValue("name_form"))
            onBtnPlay()

            setSliderRange("playback_slider", 0.0, 1.0)
            applyMotionSettings(motionp)
            setEnabled(true)
            val duration = motionDuration(motionp)
            setTitle("$filename - ${"%.2f".format(duration)} seconds")
        } else {
            animPreview = null
            motionID.setNull()
            setChildValue("bad_animation_text", "failed_to_initialize")
        }

        refresh()
        return true
    }

    fun unloadMotion() {
        if (motionID.notNull() && animPreview != null && useOwnAvatar) {
            resetMotion()
            animPreview!!.getPreviewAvatar(this).let { TODO("GPU: avatar.removeMotion(motionID)") }
            TODO("APR: LLKeyframeDataCache.removeKeyframeData(motionID)")
        }
        motionID.setNull()
        animPreview = null
    }

    fun draw() {
        TODO("GPU: LLFloater.draw(); render preview texture via OpenGL triangle strip")
        val avatar = animPreview?.getPreviewAvatar(this) ?: return
        if (!areAnimationsPaused(avatar)) {
            animPreview!!.requestUpdate()
        }
    }

    fun resetMotion() {
        val preview = animPreview ?: return
        val avatar = preview.getPreviewAvatar(this)
        val paused = areAnimationsPaused(avatar)

        val motionp = findKeyframeMotion(avatar, motionID)
        if (motionp != null) {
            val emote = getChildValue("emote_combo")
            setMotionEmote(motionp, idList[emote])
        }

        val baseId = idList[getChildValue("preview_base_anim")]
        deactivateAllMotions(avatar)
        startMotion(avatar, motionID, 0.0f)
        startMotion(avatar, baseId, BASE_ANIM_TIME_OFFSET)
        setChildValue("playback_slider", 0.0f)

        val handpose = getChildValue("hand_pose_combo")
        startMotion(avatar, handMotionId(), 0.0f)
        if (motionp != null) {
            setMotionHandPose(motionp, handpose)
        }

        pauseRequest = if (paused) requestPause(avatar) else null
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (useOwnAvatar) return super_handleMouseDown(x, y, mask)
        if (previewRectContains(x, y)) {
            TODO("APR: gFocusMgr.setMouseCapture(this); gViewerWindow.hideCursor()")
            lastMouseX = x
            lastMouseY = y
            return true
        }
        return super_handleMouseDown(x, y, mask)
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (useOwnAvatar) return super_handleMouseUp(x, y, mask)
        TODO("APR: gFocusMgr.setMouseCapture(null); gViewerWindow.showCursor()")
        return super_handleMouseUp(x, y, mask)
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (useOwnAvatar) return true
        val localMask = mask and MASK_ALT.inv()
        if (animPreview != null && hasMouseCapture()) {
            when {
                localMask == MASK_PAN -> animPreview!!.pan(
                    (x - lastMouseX) * -0.005f, (y - lastMouseY) * -0.005f
                )
                localMask == MASK_ORBIT -> animPreview!!.rotate(
                    (x - lastMouseX) * -0.01f, (y - lastMouseY) * 0.02f
                )
                else -> {
                    animPreview!!.rotate((x - lastMouseX) * -0.01f, 0.0f)
                    animPreview!!.zoom((y - lastMouseY) * 0.02f)
                }
            }
            animPreview!!.requestUpdate()
            TODO("APR: LLUI.getInstance().setMousePositionLocal(this, lastMouseX, lastMouseY)")
        }
        if (!previewRectContains(x, y) || animPreview == null) {
            return super_handleHover(x, y, mask)
        }
        TODO("GPU: set cursor based on mask (camera/pan/zoom)")
        return true
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (useOwnAvatar) return false
        if (animPreview == null) return false
        animPreview!!.zoom(clicks * -0.2f)
        animPreview!!.requestUpdate()
        return true
    }

    fun onMouseCaptureLost() {
        if (!useOwnAvatar) {
            TODO("APR: gViewerWindow.showCursor()")
        }
    }

    fun onBtnPlay() {
        if (!isEnabled()) return
        if (motionID.notNull() && animPreview != null) {
            val avatar = animPreview!!.getPreviewAvatar(this)
            if (!isMotionActive(avatar, motionID)) {
                resetMotion()
                pauseRequest = null
            } else if (areAnimationsPaused(avatar)) {
                pauseRequest = null
            }
        }
    }

    fun onBtnPause() {
        if (!isEnabled()) return
        if (motionID.notNull() && animPreview != null) {
            val avatar = animPreview!!.getPreviewAvatar(this)
            if (isMotionActive(avatar, motionID) && !areAnimationsPaused(avatar)) {
                pauseRequest = requestPause(avatar)
            }
        }
    }

    fun onBtnStop() {
        if (!isEnabled()) return
        if (motionID.notNull() && animPreview != null) {
            val avatar = animPreview!!.getPreviewAvatar(this)
            resetMotion()
            pauseRequest = requestPause(avatar)
        }
    }

    fun onSliderMove() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val sliderValue = getChildValueFloat("playback_slider")
        val baseId = idList[getChildValue("preview_base_anim")]
        val motionp = findMotion(avatar, motionID) ?: return
        val duration = motionDuration(motionp)
        val deltaTime = duration * sliderValue
        deactivateAllMotions(avatar)
        startMotion(avatar, baseId, deltaTime + BASE_ANIM_TIME_OFFSET)
        startMotion(avatar, motionID, deltaTime)
        pauseRequest = null
        TODO("APR: start timer for 0.001s then pause avatar (FrameTimer equivalent)")
        refresh()
    }

    fun onCommitBaseAnim() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val paused = areAnimationsPaused(avatar)
        stopMotion(avatar, idList["Standing"]!!, true)
        stopMotion(avatar, idList["Walking"]!!, true)
        stopMotion(avatar, idList["Sitting"]!!, true)
        stopMotion(avatar, idList["Flying"]!!, true)
        resetMotion()
        if (!paused) pauseRequest = null
    }

    fun onCommitLoop() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        setMotionLoop(motionp, getChildValueBool("loop_check"))
        setMotionLoopIn(motionp, getChildValueFloat("loop_in_point") * 0.01f * motionDuration(motionp))
        setMotionLoopOut(motionp, getChildValueFloat("loop_out_point") * 0.01f * motionDuration(motionp))
    }

    fun onCommitLoopIn() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        val loopInPct = getChildValueFloat("loop_in_point")
        setChildValue("loop_in_frames", loopInPct / 100.0f * numFrames.toFloat())
        resetMotion()
        setChildValue("loop_check", true)
        onCommitLoop()
    }

    fun onCommitLoopOut() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        val loopOutPct = getChildValueFloat("loop_out_point")
        setChildValue("loop_out_frames", loopOutPct / 100.0f * numFrames.toFloat())
        resetMotion()
        setChildValue("loop_check", true)
        onCommitLoop()
    }

    fun onCommitLoopInFrames() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        val frames = getChildValueFloat("loop_in_frames")
        setChildValue("loop_in_point", if (numFrames == 0) 0.0f else 100.0f * frames / numFrames.toFloat())
        resetMotion()
        setChildValue("loop_check", true)
        onCommitLoop()
    }

    fun onCommitLoopOutFrames() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        val frames = getChildValueFloat("loop_out_frames")
        setChildValue("loop_out_point", if (numFrames == 0) 100.0f else 100.0f * frames / numFrames.toFloat())
        resetMotion()
        setChildValue("loop_check", true)
        onCommitLoop()
    }

    fun onCommitName() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        setMotionName(motionp, getChildValue("name_form"))
        doCommit()
    }

    fun onCommitHandPose() {
        if (!isEnabled()) return
        resetMotion()
    }

    fun onCommitEmote() {
        if (!isEnabled()) return
        resetMotion()
    }

    fun onCommitPriority() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        setMotionPriority(motionp, getChildValueFloat("priority").toInt())
    }

    fun onCommitEaseIn() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        setMotionEaseIn(motionp, getChildValueFloat("ease_in_time"))
        resetMotion()
    }

    fun onCommitEaseOut() {
        if (!isEnabled() || animPreview == null) return
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return
        setMotionEaseOut(motionp, getChildValueFloat("ease_out_time"))
        resetMotion()
    }

    fun validateEaseIn(data: Any): Boolean {
        if (!isEnabled() || animPreview == null) return false
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return true
        if (!motionGetLoop(motionp)) {
            val clamped = getChildValueFloat("ease_in_time")
                .coerceIn(0.0f, motionDuration(motionp) - motionEaseOutDuration(motionp))
            setChildValue("ease_in_time", clamped)
        }
        return true
    }

    fun validateEaseOut(data: Any): Boolean {
        if (!isEnabled() || animPreview == null) return false
        val avatar = animPreview!!.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: return true
        if (!motionGetLoop(motionp)) {
            val clamped = getChildValueFloat("ease_out_time")
                .coerceIn(0.0f, motionDuration(motionp) - motionEaseInDuration(motionp))
            setChildValue("ease_out_time", clamped)
        }
        return true
    }

    fun validateLoopIn(data: Any): Boolean {
        if (!isEnabled()) return false
        var loopIn = getChildValueFloat("loop_in_point")
        val loopOut = getChildValueFloat("loop_out_point")
        loopIn = loopIn.coerceIn(0.0f, minOf(100.0f, loopOut))
        setChildValue("loop_in_point", loopIn)
        setChildValue("loop_in_frames", Math.round(loopIn / 100.0f * numFrames.toFloat()))
        return true
    }

    fun validateLoopOut(data: Any): Boolean {
        if (!isEnabled()) return false
        val loopIn = getChildValueFloat("loop_in_point")
        var loopOut = getChildValueFloat("loop_out_point")
        loopOut = loopOut.coerceIn(maxOf(0.0f, loopIn), 100.0f)
        setChildValue("loop_out_point", loopOut)
        setChildValue("loop_out_frames", Math.round(loopOut / 100.0f * numFrames.toFloat()))
        return true
    }

    fun validateLoopInFrames(data: Any): Boolean {
        if (!isEnabled()) return false
        var loopIn = getChildValueFloat("loop_in_frames")
        val loopOut = getChildValueFloat("loop_out_frames")
        loopIn = loopIn.coerceIn(0.0f, minOf(1000.0f, loopOut))
        setChildValue("loop_in_frames", loopIn)
        setChildValue("loop_in_point", if (numFrames == 0) 0.0f else 100.0f * loopIn / numFrames.toFloat())
        return true
    }

    fun validateLoopOutFrames(data: Any): Boolean {
        if (!isEnabled()) return false
        val loopIn = getChildValueFloat("loop_in_frames")
        var loopOut = getChildValueFloat("loop_out_frames")
        loopOut = loopOut.coerceIn(maxOf(0.0f, loopIn), 1000.0f)
        setChildValue("loop_out_frames", loopOut)
        setChildValue("loop_out_point", if (numFrames == 0) 100.0f else 100.0f * loopOut / numFrames.toFloat())
        return true
    }

    fun refresh() {
        var showPlay = true
        if (animPreview == null) {
            setChildVisible("bad_animation_text", true)
            setButtonEnabled("play_btn", false)
            setButtonEnabled("stop_btn", false)
            setChildEnabled("ok_btn", false)
        } else {
            setChildVisible("bad_animation_text", false)
            setButtonEnabled("play_btn", true)
            setButtonEnabled("stop_btn", true)
            val avatar = animPreview!!.getPreviewAvatar(this)
            if (isMotionActive(avatar, motionID)) {
                setButtonEnabled("stop_btn", true)
                val motionp = findKeyframeMotion(avatar, motionID)
                if (!areAnimationsPaused(avatar) && motionp != null) {
                    val fraction = motionLastUpdateTime(motionp) / motionDuration(motionp)
                    setChildValue("playback_slider", fraction)
                    showPlay = false
                }
            } else {
                pauseRequest = requestPause(avatar)
            }
            setChildEnabled("ok_btn", true)
            animPreview!!.requestUpdate()
        }
        setButtonVisible("play_btn", showPlay)
        setButtonVisible("pause_btn", !showPlay)
    }

    fun onBtnOK() {
        if (!isEnabled()) return
        val preview = animPreview ?: return
        val avatar = preview.getPreviewAvatar(this)
        val motionp = findKeyframeMotion(avatar, motionID) ?: run {
            TODO("APR: notify user WriteAnimationFail")
            return
        }

        TODO("APR: serialize motion to LLFileSystem then call upload_new_resource with LLFloaterPerms upload permissions")

        resetMotion()
        removeMotion(avatar, motionID)
        removeKeyframeData(motionID)
        motionID.setNull()
        closeFloater(false)
    }

    fun onBtnReload() {
        if (!isEnabled()) return
        unloadMotion()
        loadBVH()
        resetMotion()
    }

    fun destroy() {
        animPreview = null
        if (useOwnAvatar) {
            ownAvatarInstanceCount--
            if (motionID.notNull()) {
                TODO("APR: LLKeyframeDataCache.removeKeyframeData(motionID)")
            }
            if (ownAvatarInstanceCount == 0) {
                TODO("GPU: gAgentAvatarp.deactivateAllMotions(); startDefaultMotions(); startMotion(ANIM_AGENT_STAND)")
            }
            if (aoEnabled) {
                TODO("APR: AOEngine.getInstance().enable(true)")
            }
        }
        setEnabled(false)
    }

    // --- stub helpers for platform calls not yet implemented ---
    private fun savedSettings(key: String): Boolean = TODO("APR: gSavedSettings.getBOOL($key)")
    private fun savedPerAccountSettings(key: String): Boolean = TODO("APR: gSavedPerAccountSettings.getBOOL($key)")
    private fun nullUUID(): LLUUID = TODO("APR: LLUUID::null")
    private fun agentSecureSessionID(): Any = TODO("APR: gAgent.getSecureSessionID()")
    private fun animAgentStand(): LLUUID = TODO("APR: ANIM_AGENT_STAND")
    private fun animAgentFemaleWalk(): LLUUID = TODO("APR: ANIM_AGENT_FEMALE_WALK")
    private fun animAgentSitFemale(): LLUUID = TODO("APR: ANIM_AGENT_SIT_FEMALE")
    private fun animAgentHover(): LLUUID = TODO("APR: ANIM_AGENT_HOVER")
    private fun animAgentExpressOpenMouth(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_OPEN_MOUTH")
    private fun animAgentExpressAfraid(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_AFRAID")
    private fun animAgentExpressAnger(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_ANGER")
    private fun animAgentExpressToothsmile(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_TOOTHSMILE")
    private fun animAgentExpressBored(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_BORED")
    private fun animAgentExpressCry(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_CRY")
    private fun animAgentExpressDisdain(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_DISDAIN")
    private fun animAgentExpressEmbarrassed(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_EMBARRASSED")
    private fun animAgentExpressFrown(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_FROWN")
    private fun animAgentExpressKiss(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_KISS")
    private fun animAgentExpressLaugh(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_LAUGH")
    private fun animAgentExpressTongueOut(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_TONGUE_OUT")
    private fun animAgentExpressRepulsed(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_REPULSED")
    private fun animAgentExpressSad(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_SAD")
    private fun animAgentExpressShrug(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_SHRUG")
    private fun animAgentExpressSmile(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_SMILE")
    private fun animAgentExpressSurprise(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_SURPRISE")
    private fun animAgentExpressWink(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_WINK")
    private fun animAgentExpressWorry(): LLUUID = TODO("APR: ANIM_AGENT_EXPRESS_WORRY")
    private fun handMotionId(): LLUUID = TODO("APR: ANIM_AGENT_HAND_MOTION")
    private fun getFileExtension(name: String): String = name.substringAfterLast('.', "")
    private fun filename: String get() = TODO("APR: mFilename")
    private fun isLoaderInitialized(): Boolean = TODO("APR: loaderp.isInitialized()")
    private fun loaderDuration(): Float = TODO("APR: loaderp.getDuration()")
    private fun loaderNumFrames(): Int = TODO("APR: loaderp.getNumFrames()")
    private fun calcPelvisMaxDisplacement(): Float = TODO("GPU: compute from pelvis bbox")
    private fun setSpinnerMax(id: String, max: Float) = TODO("APR: set spinner max value")
    private fun syncLoopFrameSpinners() = TODO("APR: sync loop point spinners from percentages")
    private fun setSliderRange(id: String, min: Double, max: Double) = TODO("APR: set slider range")
    private fun applyMotionSettings(motionp: LLKeyframeMotion?) = TODO("APR: apply settings from UI to motion")
    private fun setMotionName(value: String) = TODO("APR: motionp.setName(value)")
    private fun setMotionName(motionp: LLKeyframeMotion, name: String) = TODO("APR: motionp.setName(name)")
    private fun motionDuration(m: Any?): Float = TODO("APR: motion.getDuration()")
    private fun motionLastUpdateTime(m: Any?): Float = TODO("APR: motion.getLastUpdateTime()")
    private fun motionEaseInDuration(m: Any?): Float = TODO("APR: motion.getEaseInDuration()")
    private fun motionEaseOutDuration(m: Any?): Float = TODO("APR: motion.getEaseOutDuration()")
    private fun motionGetLoop(m: Any?): Boolean = TODO("APR: motion.getLoop()")
    private fun setMotionLoop(m: LLKeyframeMotion, v: Boolean) = TODO("APR: motion.setLoop(v)")
    private fun setMotionLoopIn(m: LLKeyframeMotion, v: Float) = TODO("APR: motion.setLoopIn(v)")
    private fun setMotionLoopOut(m: LLKeyframeMotion, v: Float) = TODO("APR: motion.setLoopOut(v)")
    private fun setMotionEmote(m: LLKeyframeMotion, id: LLUUID?) = TODO("APR: motion.setEmote(id)")
    private fun setMotionHandPose(m: LLKeyframeMotion, pose: String) = TODO("APR: motion.setHandPose(...)")
    private fun setMotionPriority(m: LLKeyframeMotion, p: Int) = TODO("APR: motion.setPriority(p)")
    private fun setMotionEaseIn(m: LLKeyframeMotion, v: Float) = TODO("APR: motion.setEaseIn(v)")
    private fun setMotionEaseOut(m: LLKeyframeMotion, v: Float) = TODO("APR: motion.setEaseOut(v)")
    private fun findMotion(av: LLVOAvatar, id: LLUUID): Any? = TODO("GPU: avatar.findMotion(id)")
    private fun findKeyframeMotion(av: LLVOAvatar, id: LLUUID): LLKeyframeMotion? = TODO("GPU: avatar.findMotion(id) as keyframe")
    private fun isMotionActive(av: LLVOAvatar, id: LLUUID): Boolean = TODO("GPU: avatar.isMotionActive(id)")
    private fun areAnimationsPaused(av: LLVOAvatar): Boolean = TODO("GPU: avatar.areAnimationsPaused()")
    private fun requestPause(av: LLVOAvatar): AnimPauseRequest = TODO("GPU: avatar.requestPause()")
    private fun startMotion(av: LLVOAvatar, id: LLUUID?, offset: Float) = TODO("GPU: avatar.startMotion(id, offset)")
    private fun stopMotion(av: LLVOAvatar, id: LLUUID, immediate: Boolean) = TODO("GPU: avatar.stopMotion(id, immediate)")
    private fun deactivateAllMotions(av: LLVOAvatar) = TODO("GPU: avatar.deactivateAllMotions()")
    private fun removeMotion(av: LLVOAvatar, id: LLUUID) = TODO("GPU: avatar.removeMotion(id)")
    private fun removeKeyframeData(id: LLUUID) = TODO("APR: LLKeyframeDataCache.removeKeyframeData(id)")
    private fun doCommit() = TODO("APR: LLFloaterNameDesc.doCommit()")
    private fun closeFloater(appQuitting: Boolean) = TODO("APR: close floater")
    private fun setChildVisible(id: String, visible: Boolean) = TODO("APR: UI child visibility")
    private fun setButtonVisible(id: String, visible: Boolean) = TODO("APR: button visibility")
    private fun setButtonEnabled(id: String, enabled: Boolean) = TODO("APR: button enabled")
    private fun setChildEnabled(id: String, enabled: Boolean) = TODO("APR: child enabled")
    private fun setChildValue(id: String, value: Any) = TODO("APR: UI child value set")
    private fun getChildValue(id: String): String = TODO("APR: UI child value get")
    private fun getChildValueFloat(id: String): Float = TODO("APR: UI child float value")
    private fun getChildValueBool(id: String): Boolean = TODO("APR: UI child bool value")
    private fun previewRectContains(x: Int, y: Int): Boolean = TODO("APR: preview rect hit test")
    private fun hasMouseCapture(): Boolean = TODO("APR: mouse capture check")
    private fun isEnabled(): Boolean = TODO("APR: floater enabled check")
    private fun setEnabled(v: Boolean) = TODO("APR: floater set enabled")
    private fun setTitle(title: String) = TODO("APR: floater set title")
    private fun super_handleMouseDown(x: Int, y: Int, mask: Int): Boolean = TODO("APR: LLFloater.handleMouseDown")
    private fun super_handleMouseUp(x: Int, y: Int, mask: Int): Boolean = TODO("APR: LLFloater.handleMouseUp")
    private fun super_handleHover(x: Int, y: Int, mask: Int): Boolean = TODO("APR: LLFloater.handleHover")

    private val MAX_ANIM_DURATION: Float get() = TODO("APR: MAX_ANIM_DURATION constant")
    private val MASK_ALT: Int get() = TODO("APR: MASK_ALT")
    private val MASK_PAN: Int get() = TODO("APR: MASK_PAN")
    private val MASK_ORBIT: Int get() = TODO("APR: MASK_ORBIT")
    private val RESIZE_HANDLE_WIDTH: Float get() = TODO("APR: RESIZE_HANDLE_WIDTH")
    private val OO_SQRT2: Float get() = TODO("APR: OO_SQRT2 = 1/sqrt(2)")
}
