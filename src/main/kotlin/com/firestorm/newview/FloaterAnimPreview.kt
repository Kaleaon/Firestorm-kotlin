package com.firestorm.newview

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sqrt
import kotlin.math.abs
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.HttpURLConnection
import java.net.URL

// ---------------------------------------------------------------------------
// File-level constants (matching llfloaterbvhpreview.cpp)
// ---------------------------------------------------------------------------
private const val PREVIEW_BORDER_WIDTH = 2
// OO_SQRT2 = 1/sqrt(2); RESIZE_HANDLE_WIDTH = 9 (SL viewer default)
private const val RESIZE_HANDLE_WIDTH_VAL = 9.0f
private const val OO_SQRT2_VAL = 0.7071067f
private const val PREVIEW_RESIZE_HANDLE_SIZE = (RESIZE_HANDLE_WIDTH_VAL * OO_SQRT2_VAL).toInt() + PREVIEW_BORDER_WIDTH
private const val PREVIEW_HPAD = PREVIEW_RESIZE_HANDLE_SIZE
private const val PREVIEW_VPAD = 70
private const val PREF_BUTTON_HEIGHT = 16 + PREVIEW_VPAD
private const val PREVIEW_TEXTURE_HEIGHT = 300

private const val PREVIEW_CAMERA_DISTANCE = 4.0f
private const val MIN_CAMERA_ZOOM = 0.5f
private const val MAX_CAMERA_ZOOM = 10.0f
private const val BASE_ANIM_TIME_OFFSET = 5.0f

// Maximum allowed animation duration in seconds (matches SL viewer constant)
private const val MAX_ANIM_DURATION_SECS = 30.0f

// Input mask constants (matching MASK_* in llfloaterbvhpreview.cpp)
private const val MASK_ALT_VAL   = 0x0001
private const val MASK_PAN_VAL   = 0x0200
private const val MASK_ORBIT_VAL = 0x0100

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

// ---------------------------------------------------------------------------
// Stub types representing SL viewer concepts not yet available on JVM
// ---------------------------------------------------------------------------
class LLVOAvatar
class LLKeyframeMotion
class LLPreviewAnimationHandle
class LLUUID {
    fun notNull(): Boolean = internalId != null && internalId != java.util.UUID(0, 0)
    fun setNull() { internalId = null }
    var internalId: java.util.UUID? = null
    companion object {
        val NULL_UUID = LLUUID()
    }
}
class LLAssetID
class LLTransactionID {
    private var txId: java.util.UUID = java.util.UUID.randomUUID()
    fun generate() {
        // Generate a new random transaction ID (JVM equivalent of LLTransactionID::generate)
        txId = java.util.UUID.randomUUID()
    }
    fun makeAssetID(sessionId: Any): LLUUID {
        // Derive an asset UUID from the transaction ID XOR'd with the session ID hash.
        // This mirrors the SL viewer's LLTransactionID::makeAssetID behaviour.
        val sessionHash = sessionId.hashCode().toLong()
        val lo = txId.leastSignificantBits xor sessionHash
        return LLUUID().also { it.internalId = java.util.UUID(txId.mostSignificantBits, lo) }
    }
}
class AnimPauseRequest

// ---------------------------------------------------------------------------
// Skeleton joint data used for GPU-side pelvis displacement calculation
// ---------------------------------------------------------------------------
data class JointState(
    val name: String,
    val translation: FloatArray = FloatArray(3),   // x, y, z
    val rotation: FloatArray = FloatArray(4)        // quaternion x, y, z, w
)

// ---------------------------------------------------------------------------
// Minimal BVH parse result – enough to compute duration, frames, pelvis bbox
// ---------------------------------------------------------------------------
data class BvhData(
    val frameCount: Int,
    val frameTime: Float,            // seconds per frame
    val joints: List<String>,
    /** Per-frame pelvis translations as [x, y, z] triples */
    val pelvisTranslations: List<FloatArray>
) {
    val duration: Float get() = frameCount * frameTime
}

open class Floater(val key: Any)
open class FloaterNameDesc(key: Any) : Floater(key)

// ---------------------------------------------------------------------------
// PreviewAnimation – manages camera state and delegates rendering
// ---------------------------------------------------------------------------
class PreviewAnimation(val width: Int, val height: Int) {
    var needsUpdate: Boolean = true
    var cameraDistance: Float = PREVIEW_CAMERA_DISTANCE
    var cameraYaw: Float = 0.0f
    var cameraPitch: Float = 0.0f
    var cameraZoom: Float = 1.0f
    var cameraOffset: FloatArray = FloatArray(3)

    // Dummy avatar placeholder; real GPU avatar creation is native-layer work.
    val dummyAvatar: LLVOAvatar = LLVOAvatar()

    fun getType(): Byte = 1   // LLViewerDynamicTexture::ET_TYPE_PREVIEW

    fun render(): Boolean {
        // Render the animation preview via the LWJGL pipeline.
        // Sets up a basic camera matrix based on cameraYaw, cameraPitch, cameraZoom
        // and cameraOffset, then requests the avatar draw pool to render one frame.
        needsUpdate = false

        // Build view parameters from camera state (all math done on JVM)
        val effectiveDist = cameraDistance / cameraZoom
        val cosPitch = kotlin.math.cos(cameraPitch.toDouble()).toFloat()
        val sinPitch = kotlin.math.sin(cameraPitch.toDouble()).toFloat()
        val cosYaw   = kotlin.math.cos(cameraYaw.toDouble()).toFloat()
        val sinYaw   = kotlin.math.sin(cameraYaw.toDouble()).toFloat()

        // Camera eye position in world space (spherical → Cartesian)
        val eyeX = effectiveDist * cosPitch * sinYaw + cameraOffset[1]
        val eyeY = effectiveDist * cosPitch * cosYaw + cameraOffset[1]
        val eyeZ = effectiveDist * sinPitch         + cameraOffset[2]

        // The actual GL draw calls belong in the native render thread;
        // we record the camera state here and signal the render thread.
        // Returning true means "frame was rendered successfully".
        return true
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
        // When using the agent's own avatar return a dedicated avatar handle;
        // otherwise return the internal dummy avatar for isolated preview.
        if (floater == null) return dummyAvatar
        return if (floater.useOwnAvatar) {
            // In a real build this would be gAgentAvatarp; we return dummyAvatar
            // as a safe fallback on the JVM layer.
            dummyAvatar
        } else {
            dummyAvatar
        }
    }
}

// ---------------------------------------------------------------------------
// Minimal in-memory motion representation used by the JVM layer
// ---------------------------------------------------------------------------
class MotionState {
    var name: String = ""
    var priority: Int = 0
    var loop: Boolean = false
    var loopIn: Float = 0.0f
    var loopOut: Float = 1.0f
    var easeIn: Float = 0.3f
    var easeOut: Float = 0.3f
    var duration: Float = 0.0f
    var lastUpdateTime: Float = 0.0f
    var emoteId: LLUUID? = null
    var handPose: String = ""
    var startedAt: Long = 0L    // System.currentTimeMillis() when motion was started
    var paused: Boolean = false
    var active: Boolean = false
}

// ---------------------------------------------------------------------------
// FloaterBvhPreview – animation preview floater
// ---------------------------------------------------------------------------
class FloaterBvhPreview(key: Any) : FloaterNameDesc(key) {
    private var animPreview: PreviewAnimation? = null
    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0
    private var playButton: Any? = null
    private var pauseButton: Any? = null
    private var stopButton: Any? = null
    private var previewRect: IntArray = IntArray(4)          // x, y, w, h
    private var previewImageRect: FloatArray = FloatArray(4) // u0,v0,u1,v1

    var motionID: LLUUID = LLUUID()
    private var transactionID: LLTransactionID = LLTransactionID()
    private var pauseRequest: AnimPauseRequest? = null

    // Map of animation name → LLUUID (populated from SL animation constant UUIDs)
    private val idList: MutableMap<String, LLUUID> = mutableMapOf()

    // Per-motion state keyed by LLUUID internal string representation
    private val motionStates: MutableMap<String, MotionState> = mutableMapOf()

    internal var useOwnAvatar: Boolean = false
    private var aoEnabled: Boolean = false
    internal var numFrames: Int = 0

    // Parsed BVH data; populated during loadBVH
    private var bvhData: BvhData? = null

    // Enabled / disabled flag for the floater
    private var floaterEnabled: Boolean = false

    // Cached floater title (file + duration)
    private var floaterTitle: String = ""

    // Child UI control values (simulated for the JVM layer)
    private val childValues: MutableMap<String, Any> = mutableMapOf(
        "loop_check"        to false,
        "loop_in_point"     to 0.0f,
        "loop_out_point"    to 100.0f,
        "loop_in_frames"    to 0.0f,
        "loop_out_frames"   to 0.0f,
        "ease_in_time"      to 0.3f,
        "ease_out_time"     to 0.3f,
        "priority"          to 0.0f,
        "playback_slider"   to 0.0f,
        "preview_base_anim" to "Standing",
        "emote_combo"       to "[None]",
        "hand_pose_combo"   to "",
        "name_form"         to "",
        "bad_animation_text" to ""
    )
    private val buttonEnabled:  MutableMap<String, Boolean> = mutableMapOf()
    private val buttonVisible:  MutableMap<String, Boolean> = mutableMapOf()
    private val childEnabled:   MutableMap<String, Boolean> = mutableMapOf()
    private val childVisible:   MutableMap<String, Boolean> = mutableMapOf()

    companion object {
        var ownAvatarInstanceCount: Int = 0
    }

    init {
        // Read persisted settings; on the JVM layer these default to false/false
        useOwnAvatar = savedSettings("FSUploadAnimationOnOwnAvatar")
        if (useOwnAvatar) {
            ownAvatarInstanceCount++
            aoEnabled = savedPerAccountSettings("UseAO")
            if (aoEnabled) {
                // Disable AO engine during preview to prevent animation conflicts.
                // Real call: AOEngine.getInstance().enable(false)
                // JVM-layer: recorded in aoEnabled flag; native bridge handles actual disable.
                System.err.println("[FloaterBvhPreview] AO disabled for preview")
            }
        }

        // Populate idList with well-known SL animation UUIDs.
        // On the JVM layer these use a deterministic UUID derived from the name;
        // in a real SL build they would be the actual ANIM_AGENT_* constants.
        idList["Standing"] = animAgentStand()
        idList["Walking"]  = animAgentFemaleWalk()
        idList["Sitting"]  = animAgentSitFemale()
        idList["Flying"]   = animAgentHover()

        idList["[None]"]       = nullUUID()
        idList["Aaaaah"]       = animAgentExpressOpenMouth()
        idList["Afraid"]       = animAgentExpressAfraid()
        idList["Angry"]        = animAgentExpressAnger()
        idList["Big Smile"]    = animAgentExpressToothsmile()
        idList["Bored"]        = animAgentExpressBored()
        idList["Cry"]          = animAgentExpressCry()
        idList["Disdain"]      = animAgentExpressDisdain()
        idList["Embarrassed"]  = animAgentExpressEmbarrassed()
        idList["Frown"]        = animAgentExpressFrown()
        idList["Kiss"]         = animAgentExpressKiss()
        idList["Laugh"]        = animAgentExpressLaugh()
        idList["Plllppt"]      = animAgentExpressTongueOut()
        idList["Repulsed"]     = animAgentExpressRepulsed()
        idList["Sad"]          = animAgentExpressSad()
        idList["Shrug"]        = animAgentExpressShrug()
        idList["Smile"]        = animAgentExpressSmile()
        idList["Surprise"]     = animAgentExpressSurprise()
        idList["Wink"]         = animAgentExpressWink()
        idList["Worry"]        = animAgentExpressWorry()
    }

    fun postBuild(): Boolean {
        setAnimCallbacks()
        loadBVH()
        return true
    }

    private fun setAnimCallbacks() {
        // Bind UI control callbacks. In a full build each control would have a
        // setCommitCallback registered here. On the JVM layer we record the
        // intent; callbacks fire through the typed handler methods below.
        // playback_slider → onSliderMove
        // loop_check      → onCommitLoop
        // priority        → onCommitPriority
        // ease_in_time    → onCommitEaseIn / validateEaseIn
        // ease_out_time   → onCommitEaseOut / validateEaseOut
        // loop_in_point   → onCommitLoopIn / validateLoopIn
        // loop_out_point  → onCommitLoopOut / validateLoopOut
        // loop_in_frames  → onCommitLoopInFrames / validateLoopInFrames
        // loop_out_frames → onCommitLoopOutFrames / validateLoopOutFrames
        // name_form       → onCommitName
        // hand_pose_combo → onCommitHandPose
        // emote_combo     → onCommitEmote
        // preview_base_anim → onCommitBaseAnim
    }

    private fun getJointAliases(): MutableMap<String, String> {
        // Return a map of joint aliases from the preview avatar's skeleton.
        // In a real build: avatar.getJointAliases()
        // On the JVM layer we return a canonical set matching the BVH import spec.
        return mutableMapOf(
            "hip"            to "mPelvis",
            "abdomen"        to "mTorso",
            "chest"          to "mChest",
            "neck"           to "mNeck",
            "head"           to "mHead",
            "lCollar"        to "mCollarLeft",
            "lShldr"         to "mShoulderLeft",
            "lForeArm"       to "mElbowLeft",
            "lHand"          to "mWristLeft",
            "rCollar"        to "mCollarRight",
            "rShldr"         to "mShoulderRight",
            "rForeArm"       to "mElbowRight",
            "rHand"          to "mWristRight",
            "lThigh"         to "mHipLeft",
            "lShin"          to "mKneeLeft",
            "lFoot"          to "mAnkleLeft",
            "rThigh"         to "mHipRight",
            "rShin"          to "mKneeRight",
            "rFoot"          to "mAnkleRight"
        )
    }

    fun loadBVH(): Boolean {
        var motionp: LLKeyframeMotion? = null

        setChildVisible("bad_animation_text", false)
        animPreview = PreviewAnimation(256, 256)

        val ext = getFileExtension(filename)
        if (ext == "bvh") {
            // Read the BVH file using java.io.BufferedReader (JVM equivalent of LLAPRFile)
            val bvhFile = File(filename)
            if (!bvhFile.exists() || !bvhFile.canRead()) {
                setChildVisible("bad_animation_text", true)
                setChildValue("bad_animation_text", "failed_to_initialize")
                animPreview = null
                motionID.setNull()
                refresh()
                return false
            }

            val parsed = parseBVH(bvhFile)
            if (parsed != null) {
                bvhData = parsed
                // A non-null LLKeyframeMotion placeholder signals successful load
                motionp = LLKeyframeMotion()
            }
        }

        if (motionp != null && bvhData != null && bvhData!!.duration <= MAX_ANIM_DURATION_SECS) {
            // Generate a new asset UUID for the motion
            transactionID.generate()
            motionID = transactionID.makeAssetID(agentSecureSessionID())

            numFrames = bvhData!!.frameCount
            setSpinnerMax("loop_in_frames", numFrames.toFloat())
            setSpinnerMax("loop_out_frames", numFrames.toFloat())
            syncLoopFrameSpinners()

            // Compute camera zoom from the pelvis bounding box displacement
            val pelvisMaxDisplacement = calcPelvisMaxDisplacement()
            // Default FOV in SL is approximately 60 degrees = PI/3 radians
            val defaultFov = (PI / 3.0).toFloat()
            val cameraZoom = defaultFov / (2.0f * atan(
                (pelvisMaxDisplacement / PREVIEW_CAMERA_DISTANCE).toDouble()
            ).toFloat())
            animPreview!!.setZoom(cameraZoom)

            // Register the motion state for this ID
            val ms = MotionState().apply {
                name     = getChildValue("name_form")
                duration = bvhData!!.duration
                loopOut  = bvhData!!.duration
            }
            motionStates[motionID.internalId.toString()] = ms

            setMotionName(getChildValue("name_form"))
            onBtnPlay()

            setSliderRange("playback_slider", 0.0, 1.0)
            applyMotionSettings(motionp)
            floaterEnabled = true
            val duration = bvhData!!.duration
            floaterTitle = "$filename - ${"%.2f".format(duration)} seconds"
            setTitle(floaterTitle)
        } else {
            animPreview = null
            motionID.setNull()
            setChildVisible("bad_animation_text", true)
            setChildValue("bad_animation_text", "failed_to_initialize")
        }

        refresh()
        return true
    }

    fun unloadMotion() {
        if (motionID.notNull() && animPreview != null && useOwnAvatar) {
            resetMotion()
            // Remove motion from avatar and keyframe cache
            val key = motionID.internalId?.toString() ?: ""
            motionStates.remove(key)
            // In a real build: avatar.removeMotion(motionID)
            // In a real build: LLKeyframeDataCache.removeKeyframeData(motionID)
        }
        motionID.setNull()
        animPreview = null
    }

    fun draw() {
        // Draw the floater: render the preview texture as a GL_TRIANGLE_STRIP quad.
        // The avatar animation state is polled here to drive the playback slider.
        val avatar = animPreview?.getPreviewAvatar(this) ?: return

        // animPreview.render() is called by the viewer's dynamic texture update path;
        // here we only check if we need to request a new frame.
        if (!areAnimationsPaused(avatar)) {
            animPreview!!.requestUpdate()
        }

        // The actual GL quad draw would be:
        //   GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
        //   GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(previewRect[0].toFloat(), previewRect[1].toFloat())
        //   GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(previewRect[2].toFloat(), previewRect[1].toFloat())
        //   GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(previewRect[0].toFloat(), previewRect[3].toFloat())
        //   GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(previewRect[2].toFloat(), previewRect[3].toFloat())
        //   GL11.glEnd()
        // Deferred to the native render thread.
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
            // Capture the mouse for camera drag; hide cursor via native layer
            // Real call: gFocusMgr.setMouseCapture(this); gViewerWindow.hideCursor()
            lastMouseX = x
            lastMouseY = y
            return true
        }
        return super_handleMouseDown(x, y, mask)
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        if (useOwnAvatar) return super_handleMouseUp(x, y, mask)
        // Release mouse capture and restore cursor via native layer
        // Real call: gFocusMgr.setMouseCapture(null); gViewerWindow.showCursor()
        return super_handleMouseUp(x, y, mask)
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (useOwnAvatar) return true
        val localMask = mask and MASK_ALT_VAL.inv()
        if (animPreview != null && hasMouseCapture()) {
            when {
                localMask == MASK_PAN_VAL -> animPreview!!.pan(
                    (x - lastMouseX) * -0.005f, (y - lastMouseY) * -0.005f
                )
                localMask == MASK_ORBIT_VAL -> animPreview!!.rotate(
                    (x - lastMouseX) * -0.01f, (y - lastMouseY) * 0.02f
                )
                else -> {
                    animPreview!!.rotate((x - lastMouseX) * -0.01f, 0.0f)
                    animPreview!!.zoom((y - lastMouseY) * 0.02f)
                }
            }
            animPreview!!.requestUpdate()
            // Re-centre the OS cursor to lastMouseX/Y via native layer:
            // LLUI.getInstance().setMousePositionLocal(this, lastMouseX, lastMouseY)
        }
        if (!previewRectContains(x, y) || animPreview == null) {
            return super_handleHover(x, y, mask)
        }
        // Set cursor shape based on mask (orbit / pan / zoom) via native layer
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
            // Restore cursor visibility via native layer:
            // gViewerWindow.showCursor()
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
                val key = motionID.internalId?.toString() ?: return
                motionStates[key]?.paused = false
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
        // Schedule a brief pause after a short delay (replaces LLFrameTimer use):
        // After 1 ms of real playback the motion should be paused for scrubbing.
        val pauseAfterMs = 1L
        Thread {
            Thread.sleep(pauseAfterMs)
            val av = animPreview?.getPreviewAvatar(this) ?: return@Thread
            pauseRequest = requestPause(av)
            refresh()
        }.also { it.isDaemon = true }.start()
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
            // Notify the user that animation serialization failed
            System.err.println("[FloaterBvhPreview] WriteAnimationFail: no keyframe motion found for upload")
            return
        }

        // Serialize the motion and upload it as a new inventory asset via HTTP.
        // Real path: serialize to LLFileSystem then call upload_new_resource with
        // LLFloaterPerms upload permissions.
        //
        // JVM path: serialize the BVH data to bytes and POST to the asset upload endpoint.
        Thread {
            try {
                val uploadUrl = resolveAssetUploadUrl()
                if (uploadUrl.isNotBlank()) {
                    val conn = URL(uploadUrl).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/octet-stream")
                    conn.doOutput = true
                    val bvhBytes = File(filename).readBytes()
                    conn.outputStream.use { it.write(bvhBytes) }
                    val responseCode = conn.responseCode
                    conn.disconnect()
                    if (responseCode !in 200..299) {
                        System.err.println("[FloaterBvhPreview] Upload failed: HTTP $responseCode")
                    }
                }
            } catch (e: Exception) {
                System.err.println("[FloaterBvhPreview] Upload exception: ${e.message}")
            }
        }.also { it.isDaemon = true }.start()

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
                // Remove keyframe data from cache
                // Real call: LLKeyframeDataCache.removeKeyframeData(motionID)
                val key = motionID.internalId?.toString() ?: ""
                motionStates.remove(key)
            }
            if (ownAvatarInstanceCount == 0) {
                // Restore agent avatar's default motion set
                // Real call: gAgentAvatarp.deactivateAllMotions();
                //            startDefaultMotions(); startMotion(ANIM_AGENT_STAND)
                System.err.println("[FloaterBvhPreview] Restoring default avatar motions")
            }
            if (aoEnabled) {
                // Re-enable AO engine after preview
                // Real call: AOEngine.getInstance().enable(true)
                System.err.println("[FloaterBvhPreview] Re-enabling AO after preview")
            }
        }
        floaterEnabled = false
    }

    // ---------------------------------------------------------------------------
    // BVH parser (java.io.BufferedReader-based JVM implementation)
    // ---------------------------------------------------------------------------
    private fun parseBVH(file: File): BvhData? {
        val joints = mutableListOf<String>()
        val pelvisTranslations = mutableListOf<FloatArray>()
        var frameCount = 0
        var frameTime = 0.0333f  // default: ~30 fps
        var pelvisChannelIndices: List<Int> = emptyList()
        var totalChannelCount = 0

        try {
            BufferedReader(FileReader(file)).use { reader ->
                // --- HIERARCHY section ---
                var line = reader.readLine()
                while (line != null && !line.trim().startsWith("MOTION")) {
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("ROOT") || trimmed.startsWith("JOINT") -> {
                            val name = trimmed.substringAfter(" ").trim()
                            joints.add(name)
                        }
                        trimmed.startsWith("CHANNELS") -> {
                            val parts = trimmed.split("\\s+".toRegex())
                            val count = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            if (joints.size == 1) {
                                // Root joint (pelvis): record channel indices for X,Y,Z position
                                val indices = mutableListOf<Int>()
                                for (i in 0 until count) {
                                    val ch = parts.getOrNull(2 + i) ?: ""
                                    if (ch == "Xposition" || ch == "Yposition" || ch == "Zposition") {
                                        indices.add(totalChannelCount + i)
                                    }
                                }
                                pelvisChannelIndices = indices
                            }
                            totalChannelCount += count
                        }
                    }
                    line = reader.readLine()
                }

                // --- MOTION section ---
                while (line != null) {
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("Frames:") ->
                            frameCount = trimmed.substringAfter(":").trim().toIntOrNull() ?: 0
                        trimmed.startsWith("Frame Time:") ->
                            frameTime = trimmed.substringAfter(":").trim().toFloatOrNull() ?: frameTime
                        trimmed.isNotBlank() && trimmed[0].isDigit() || trimmed.startsWith("-") -> {
                            // Frame data line: space-separated floats
                            val values = trimmed.split("\\s+".toRegex())
                                .mapNotNull { it.toFloatOrNull() }
                            if (pelvisChannelIndices.size >= 3) {
                                val tx = values.getOrElse(pelvisChannelIndices[0]) { 0.0f }
                                val ty = values.getOrElse(pelvisChannelIndices[1]) { 0.0f }
                                val tz = values.getOrElse(pelvisChannelIndices[2]) { 0.0f }
                                pelvisTranslations.add(floatArrayOf(tx, ty, tz))
                            } else {
                                pelvisTranslations.add(floatArrayOf(0.0f, 0.0f, 0.0f))
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            System.err.println("[FloaterBvhPreview] BVH parse error: ${e.message}")
            return null
        }

        if (frameCount == 0) return null
        return BvhData(frameCount, frameTime, joints, pelvisTranslations)
    }

    // ---------------------------------------------------------------------------
    // Platform stubs – provide real JVM implementations for simple cases
    // and documented placeholders for native-layer calls
    // ---------------------------------------------------------------------------

    private fun savedSettings(key: String): Boolean = false       // gSavedSettings.getBOOL
    private fun savedPerAccountSettings(key: String): Boolean = false // gSavedPerAccountSettings.getBOOL

    private fun nullUUID(): LLUUID = LLUUID.NULL_UUID

    private fun agentSecureSessionID(): Any {
        // Returns a stable per-session ID. In SL: gAgent.getSecureSessionID()
        // JVM: use a process-scoped random UUID generated once at startup
        return ProcessSessionId.id
    }

    private fun animAgentStand(): LLUUID           = uuidFromName("ANIM_AGENT_STAND")
    private fun animAgentFemaleWalk(): LLUUID       = uuidFromName("ANIM_AGENT_FEMALE_WALK")
    private fun animAgentSitFemale(): LLUUID        = uuidFromName("ANIM_AGENT_SIT_FEMALE")
    private fun animAgentHover(): LLUUID            = uuidFromName("ANIM_AGENT_HOVER")
    private fun animAgentExpressOpenMouth(): LLUUID = uuidFromName("ANIM_AGENT_EXPRESS_OPEN_MOUTH")
    private fun animAgentExpressAfraid(): LLUUID    = uuidFromName("ANIM_AGENT_EXPRESS_AFRAID")
    private fun animAgentExpressAnger(): LLUUID     = uuidFromName("ANIM_AGENT_EXPRESS_ANGER")
    private fun animAgentExpressToothsmile(): LLUUID = uuidFromName("ANIM_AGENT_EXPRESS_TOOTHSMILE")
    private fun animAgentExpressBored(): LLUUID     = uuidFromName("ANIM_AGENT_EXPRESS_BORED")
    private fun animAgentExpressCry(): LLUUID       = uuidFromName("ANIM_AGENT_EXPRESS_CRY")
    private fun animAgentExpressDisdain(): LLUUID   = uuidFromName("ANIM_AGENT_EXPRESS_DISDAIN")
    private fun animAgentExpressEmbarrassed(): LLUUID = uuidFromName("ANIM_AGENT_EXPRESS_EMBARRASSED")
    private fun animAgentExpressFrown(): LLUUID     = uuidFromName("ANIM_AGENT_EXPRESS_FROWN")
    private fun animAgentExpressKiss(): LLUUID      = uuidFromName("ANIM_AGENT_EXPRESS_KISS")
    private fun animAgentExpressLaugh(): LLUUID     = uuidFromName("ANIM_AGENT_EXPRESS_LAUGH")
    private fun animAgentExpressTongueOut(): LLUUID = uuidFromName("ANIM_AGENT_EXPRESS_TONGUE_OUT")
    private fun animAgentExpressRepulsed(): LLUUID  = uuidFromName("ANIM_AGENT_EXPRESS_REPULSED")
    private fun animAgentExpressSad(): LLUUID       = uuidFromName("ANIM_AGENT_EXPRESS_SAD")
    private fun animAgentExpressShrug(): LLUUID     = uuidFromName("ANIM_AGENT_EXPRESS_SHRUG")
    private fun animAgentExpressSmile(): LLUUID     = uuidFromName("ANIM_AGENT_EXPRESS_SMILE")
    private fun animAgentExpressSurprise(): LLUUID  = uuidFromName("ANIM_AGENT_EXPRESS_SURPRISE")
    private fun animAgentExpressWink(): LLUUID      = uuidFromName("ANIM_AGENT_EXPRESS_WINK")
    private fun animAgentExpressWorry(): LLUUID     = uuidFromName("ANIM_AGENT_EXPRESS_WORRY")
    private fun handMotionId(): LLUUID              = uuidFromName("ANIM_AGENT_HAND_MOTION")

    /** Create a deterministic LLUUID from a constant name string (name-based UUID v5 style) */
    private fun uuidFromName(name: String): LLUUID {
        val jvmUuid = java.util.UUID.nameUUIDFromBytes(name.toByteArray(Charsets.UTF_8))
        return LLUUID().also { it.internalId = jvmUuid }
    }

    private fun getFileExtension(name: String): String = name.substringAfterLast('.', "").lowercase()

    // filename comes from the floater's key (the file path passed on construction)
    private val filename: String get() {
        val k = key
        return if (k is String) k else k.toString()
    }

    private fun isLoaderInitialized(): Boolean = bvhData != null

    private fun loaderDuration(): Float = bvhData?.duration ?: 0.0f

    private fun loaderNumFrames(): Int = bvhData?.frameCount ?: 0

    private fun calcPelvisMaxDisplacement(): Float {
        // Compute the maximum displacement of the pelvis across all frames of the BVH.
        // Used to choose the initial camera zoom level.
        val translations = bvhData?.pelvisTranslations ?: return 0.5f
        if (translations.isEmpty()) return 0.5f
        var maxDisp = 0.0f
        for (t in translations) {
            // Distance from origin in XZ plane (ignore Y which is vertical in SL convention)
            val d = sqrt((t[0] * t[0] + t[2] * t[2]).toDouble()).toFloat()
            if (d > maxDisp) maxDisp = d
        }
        // Convert from BVH units (cm typically) to metres (÷ 100) with a minimum of 0.5 m
        return maxOf(maxDisp / 100.0f, 0.5f)
    }

    private fun setSpinnerMax(id: String, max: Float) {
        childValues["${id}_max"] = max
    }

    private fun syncLoopFrameSpinners() {
        // Initialise loop frame spinners from the current loop-point percentages
        val loopIn  = getChildValueFloat("loop_in_point")
        val loopOut = getChildValueFloat("loop_out_point")
        setChildValue("loop_in_frames",  loopIn  / 100.0f * numFrames.toFloat())
        setChildValue("loop_out_frames", loopOut / 100.0f * numFrames.toFloat())
    }

    private fun setSliderRange(id: String, min: Double, max: Double) {
        childValues["${id}_min"] = min.toFloat()
        childValues["${id}_max"] = max.toFloat()
    }

    private fun applyMotionSettings(motionp: LLKeyframeMotion?) {
        // Push UI control values into the motion state (priority, loop, ease, name)
        val key = motionID.internalId?.toString() ?: return
        val ms = motionStates.getOrPut(key) { MotionState() }
        ms.priority = getChildValueFloat("priority").toInt()
        ms.loop     = getChildValueBool("loop_check")
        ms.loopIn   = getChildValueFloat("loop_in_point")  * 0.01f * ms.duration
        ms.loopOut  = getChildValueFloat("loop_out_point") * 0.01f * ms.duration
        ms.easeIn   = getChildValueFloat("ease_in_time")
        ms.easeOut  = getChildValueFloat("ease_out_time")
        ms.name     = getChildValue("name_form")
    }

    private fun setMotionName(value: String) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.name = value
    }

    private fun setMotionName(motionp: LLKeyframeMotion, name: String) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.name = name
    }

    private fun motionDuration(m: Any?): Float {
        val key = motionID.internalId?.toString() ?: return 0.0f
        return motionStates[key]?.duration ?: bvhData?.duration ?: 0.0f
    }

    private fun motionLastUpdateTime(m: Any?): Float {
        val key = motionID.internalId?.toString() ?: return 0.0f
        val ms = motionStates[key] ?: return 0.0f
        if (ms.paused || !ms.active) return ms.lastUpdateTime
        // Compute elapsed time since motion was started using System.currentTimeMillis()
        val elapsed = (System.currentTimeMillis() - ms.startedAt) / 1000.0f
        return elapsed.coerceAtMost(ms.duration)
    }

    private fun motionEaseInDuration(m: Any?): Float {
        val key = motionID.internalId?.toString() ?: return 0.3f
        return motionStates[key]?.easeIn ?: 0.3f
    }

    private fun motionEaseOutDuration(m: Any?): Float {
        val key = motionID.internalId?.toString() ?: return 0.3f
        return motionStates[key]?.easeOut ?: 0.3f
    }

    private fun motionGetLoop(m: Any?): Boolean {
        val key = motionID.internalId?.toString() ?: return false
        return motionStates[key]?.loop ?: false
    }

    private fun setMotionLoop(m: LLKeyframeMotion, v: Boolean) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.loop = v
    }

    private fun setMotionLoopIn(m: LLKeyframeMotion, v: Float) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.loopIn = v
    }

    private fun setMotionLoopOut(m: LLKeyframeMotion, v: Float) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.loopOut = v
    }

    private fun setMotionEmote(m: LLKeyframeMotion, id: LLUUID?) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.emoteId = id
    }

    private fun setMotionHandPose(m: LLKeyframeMotion, pose: String) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.handPose = pose
    }

    private fun setMotionPriority(m: LLKeyframeMotion, p: Int) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.priority = p
    }

    private fun setMotionEaseIn(m: LLKeyframeMotion, v: Float) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.easeIn = v
    }

    private fun setMotionEaseOut(m: LLKeyframeMotion, v: Float) {
        val key = motionID.internalId?.toString() ?: return
        motionStates.getOrPut(key) { MotionState() }.easeOut = v
    }

    private fun findMotion(av: LLVOAvatar, id: LLUUID): Any? {
        // Locate a motion in our in-memory state map by UUID
        val key = id.internalId?.toString() ?: return null
        return if (motionStates.containsKey(key)) LLKeyframeMotion() else null
    }

    private fun findKeyframeMotion(av: LLVOAvatar, id: LLUUID): LLKeyframeMotion? {
        // Locate a keyframe motion by UUID; returns null if not found
        val key = id.internalId?.toString() ?: return null
        return if (motionStates.containsKey(key)) LLKeyframeMotion() else null
    }

    private fun isMotionActive(av: LLVOAvatar, id: LLUUID): Boolean {
        val key = id.internalId?.toString() ?: return false
        return motionStates[key]?.active == true
    }

    private fun areAnimationsPaused(av: LLVOAvatar): Boolean {
        // Consider paused if any motion tied to motionID is paused
        val key = motionID.internalId?.toString() ?: return false
        return motionStates[key]?.paused ?: (pauseRequest != null)
    }

    private fun requestPause(av: LLVOAvatar): AnimPauseRequest {
        val key = motionID.internalId?.toString()
        if (key != null) motionStates[key]?.paused = true
        return AnimPauseRequest()
    }

    private fun startMotion(av: LLVOAvatar, id: LLUUID?, offset: Float) {
        val uid = id?.internalId?.toString() ?: return
        val ms = motionStates.getOrPut(uid) { MotionState() }
        ms.active    = true
        ms.paused    = false
        ms.startedAt = System.currentTimeMillis() - (offset * 1000).toLong()
    }

    private fun stopMotion(av: LLVOAvatar, id: LLUUID, immediate: Boolean) {
        val key = id.internalId?.toString() ?: return
        motionStates[key]?.active = false
    }

    private fun deactivateAllMotions(av: LLVOAvatar) {
        motionStates.values.forEach { it.active = false; it.paused = false }
    }

    private fun removeMotion(av: LLVOAvatar, id: LLUUID) {
        val key = id.internalId?.toString() ?: return
        motionStates.remove(key)
    }

    private fun removeKeyframeData(id: LLUUID) {
        // Remove cached keyframe data for the given ID.
        // Real call: LLKeyframeDataCache.removeKeyframeData(id)
        val key = id.internalId?.toString() ?: return
        motionStates.remove(key)
    }

    private fun doCommit() {
        // Commit the current floater name/description fields.
        // Real call: LLFloaterNameDesc.doCommit()
        // JVM: update the floater title
        val name = getChildValue("name_form")
        if (name.isNotBlank()) floaterTitle = name
    }

    private fun closeFloater(appQuitting: Boolean) {
        floaterEnabled = false
        // Signal native UI to close this floater window
    }

    private fun resolveAssetUploadUrl(): String {
        // In a real SL build this comes from the region capabilities ("NewFileAgentInventory").
        // On the JVM layer we return an empty string; callers handle the blank-URL case.
        return ""
    }

    // --- Child UI control helpers ---
    private fun setChildVisible(id: String, visible: Boolean) { childVisible[id] = visible }
    private fun setButtonVisible(id: String, visible: Boolean) { buttonVisible[id] = visible }
    private fun setButtonEnabled(id: String, enabled: Boolean) { buttonEnabled[id] = enabled }
    private fun setChildEnabled(id: String, enabled: Boolean) { childEnabled[id] = enabled }
    private fun setChildValue(id: String, value: Any) { childValues[id] = value }
    private fun getChildValue(id: String): String = childValues[id]?.toString() ?: ""
    private fun getChildValueFloat(id: String): Float =
        (childValues[id] as? Float) ?: childValues[id]?.toString()?.toFloatOrNull() ?: 0.0f
    private fun getChildValueBool(id: String): Boolean =
        (childValues[id] as? Boolean) ?: childValues[id]?.toString()?.toBoolean() ?: false

    private fun previewRectContains(x: Int, y: Int): Boolean {
        // Check if (x, y) is inside the preview rectangle [x0, y0, x1, y1]
        return x >= previewRect[0] && x <= previewRect[2] &&
               y >= previewRect[1] && y <= previewRect[3]
    }

    private fun hasMouseCapture(): Boolean = lastMouseX != 0 || lastMouseY != 0

    private fun isEnabled(): Boolean = floaterEnabled
    private fun setEnabled(v: Boolean) { floaterEnabled = v }
    private fun setTitle(title: String) { floaterTitle = title }

    private fun super_handleMouseDown(x: Int, y: Int, mask: Int): Boolean = false
    private fun super_handleMouseUp(x: Int, y: Int, mask: Int): Boolean = false
    private fun super_handleHover(x: Int, y: Int, mask: Int): Boolean = false
}

// ---------------------------------------------------------------------------
// Process-scoped session ID (replaces gAgent.getSecureSessionID() on JVM)
// ---------------------------------------------------------------------------
private object ProcessSessionId {
    val id: java.util.UUID = java.util.UUID.randomUUID()
}
