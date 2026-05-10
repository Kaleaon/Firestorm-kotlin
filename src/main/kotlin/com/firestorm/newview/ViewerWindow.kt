package com.firestorm.newview

import com.firestorm.llmath.Vector3
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// CursorType enum  (mirrors ECursorType / UI_CURSOR_* from llcursortypes.h)
// ---------------------------------------------------------------------------

enum class CursorType {
    ARROW, WAIT, HAND, IBEAM, CROSS,
    SIZENWSE, SIZENESW, SIZEWE, SIZENS, SIZEALL,
    NO, WORKING,
    TOOLGRAB, TOOLLAND, TOOLFOCUS, TOOLCREATE,
    ARROWDRAG, ARROWCOPY, ARROWDRAGMULTI, ARROWCOPYMULTI,
    NOLOCKED, ARROWLOCKED, GRABLOCKED,
    TOOLTRANSLATE, TOOLROTATE, TOOLSCALE, TOOLCAMERA, TOOLPAN,
    TOOLZOOMIN, TOOLZOOMOUT, TOOLPICKOBJECT3,
    TOOLPLAY, TOOLPAUSE, TOOLMEDIAOPEN,
    PIPETTE, TOOLSIT, TOOLBUY, TOOLPAY, TOOLOPEN,
}

// ---------------------------------------------------------------------------
// DragNDropResult / DragNDropAction  (mirrors LLWindowCallbacks enums)
// ---------------------------------------------------------------------------

enum class DragNDropResult { NONE, MOVE, COPY, LINK }
enum class DragNDropAction { TRACK, DROP, START_TRACKING, STOP_TRACKING }

// ---------------------------------------------------------------------------
// Rect — axis-aligned 2-D integer rectangle used for window / world regions
// (minimal stand-in for LLRect; real code would import a shared type)
// ---------------------------------------------------------------------------

data class Rect(val left: Int = 0, val bottom: Int = 0, val right: Int = 0, val top: Int = 0) {
    val width: Int  get() = right - left
    val height: Int get() = top - bottom
}

// ---------------------------------------------------------------------------
// PickInfo  (mirrors LLPickInfo)
// ---------------------------------------------------------------------------

enum class PickType { OBJECT, FLORA, LAND, ICON, PARCEL_WALL, INVALID }

data class PickInfo(
    val mousePt: Pair<Int, Int> = Pair(0, 0),
    val keyMask: Int = 0,
    val pickType: PickType = PickType.INVALID,
    val pickPt: Pair<Int, Int> = Pair(0, 0),
    val posGlobal: Vector3 = Vector3.ZERO,
    val objectOffset: Vector3 = Vector3.ZERO,
    val objectId: String = "",
    val particleOwnerId: String = "",
    val particleSourceId: String = "",
    val objectFace: Int = -1,
    val gltfNodeIndex: Int = -1,
    val gltfPrimitiveIndex: Int = -1,
    val intersection: Vector3 = Vector3.ZERO,
    val uvCoords: Pair<Float, Float> = Pair(0f, 0f),
    val stCoords: Pair<Float, Float> = Pair(0f, 0f),
    val xyCoords: Pair<Int, Int> = Pair(0, 0),
    val normal: Vector3 = Vector3.ZERO,
    val tangent: FloatArray = FloatArray(4),
    val binormal: Vector3 = Vector3.ZERO,
    val pickTransparent: Boolean = false,
    val pickRigged: Boolean = false,
    val pickParticle: Boolean = false,
    val pickUnselectable: Boolean = false,
    val pickReflectionProbe: Boolean = false,
    val pickHUD: Boolean = false,
    val wantSurfaceInfo: Boolean = false
) {
    fun isValid(): Boolean = pickType != PickType.INVALID
    fun getObjectId(): String = objectId

    fun fetchResults() { TODO("GPU: read pick framebuffer and populate intersection/UV/normal") }
    fun getSurfaceInfo() { TODO("GPU: decode surface normal, UV and binormal from pick buffer") }

    companion object {
        fun isFlora(vobjPcode: Int): Boolean = vobjPcode == 0x09 // LL_PCODE_LEGACY_GRASS / tree codes
    }
}

// ---------------------------------------------------------------------------
// MouseVelocityStat — lightweight stand-in for LLTrace::SampleStatHandle<F32>
// ---------------------------------------------------------------------------

class MouseVelocityStat {
    private var lastSample: Float = 0f
    fun record(pixelsPerSecond: Float) { lastSample = pixelsPerSecond }
    fun getCurrentSample(): Float = lastSample
}

// ---------------------------------------------------------------------------
// NativeWindow — opaque platform window handle
// ---------------------------------------------------------------------------

class NativeWindow

// ---------------------------------------------------------------------------
// ViewerWindow singleton  (mirrors LLViewerWindow / gViewerWindow)
// ---------------------------------------------------------------------------

object ViewerWindow {

    // ------------------------------------------------------------------
    // Creation parameters (mirrors LLViewerWindow::Params)
    // ------------------------------------------------------------------

    data class Params(
        val title: String,
        val name: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val minWidth: Int,
        val minHeight: Int,
        val fullscreen: Boolean = false,
        val ignorePixelDepth: Boolean = false,
        val firstRun: Boolean = false
    )

    // ------------------------------------------------------------------
    // Window dimensions — raw pixel values
    // ------------------------------------------------------------------

    var width: Int = 1024
        private set
    var height: Int = 768
        private set

    var displayScaleX: Float = 1.0f
        private set
    var displayScaleY: Float = 1.0f
        private set

    // Scaled dimensions used for most UI math
    val widthScaled: Int  get() = (width  / displayScaleX).roundToInt()
    val heightScaled: Int get() = (height / displayScaleY).roundToInt()

    // World-view subrect (3-D viewport within the window)
    var worldViewRectRaw: Rect = Rect(0, 0, 1024, 768)
        private set
    var worldViewRectScaled: Rect = Rect(0, 0, 1024, 768)
        private set

    var windowRectRaw: Rect = Rect(0, 0, 1024, 768)
        private set
    var windowRectScaled: Rect = Rect(0, 0, 1024, 768)
        private set

    // ------------------------------------------------------------------
    // Mouse state
    // ------------------------------------------------------------------

    var lastMouseX: Int = 0;  private set
    var lastMouseY: Int = 0;  private set
    var currentMouseX: Int = 0; private set
    var currentMouseY: Int = 0; private set
    var currentMouseDX: Int = 0; private set
    var currentMouseDY: Int = 0; private set

    var leftMouseDown:   Boolean = false; private set
    var middleMouseDown: Boolean = false; private set
    var rightMouseDown:  Boolean = false; private set

    private val mouseVelocityStatHandle = MouseVelocityStat()

    var isActive: Boolean = false
        private set

    var isMouseInWindow: Boolean = false
        private set

    // ------------------------------------------------------------------
    // Cursor state
    // ------------------------------------------------------------------

    var cursorHidden: Boolean = false
        private set
    private var hideCursorPermanent: Boolean = false
    private var currentCursor: CursorType = CursorType.ARROW

    // ------------------------------------------------------------------
    // Pick state
    // ------------------------------------------------------------------

    var lastPick: PickInfo = PickInfo()
        private set
    private val pendingPicks: MutableList<PickInfo> = mutableListOf()
    private var pickScreenRegion: Rect = Rect()

    // ------------------------------------------------------------------
    // UI flags
    // ------------------------------------------------------------------

    var uiVisible: Boolean = true
        private set
    var resDirty: Boolean = false
        private set
    var statesDirty: Boolean = false
        private set

    private var overlayTitle: String = ""
    var initAlert: String = ""; private set

    // ------------------------------------------------------------------
    // Native window
    // ------------------------------------------------------------------

    private var nativeWindow: NativeWindow? = null

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    companion object {
        const val PICK_HALF_WIDTH = 5
        const val PICK_DIAMETER = 2 * PICK_HALF_WIDTH + 1
        const val MAX_SNAPSHOT_IMAGE_SIZE: UInt = 7680u

        private const val MIN_AFK_TIME = 6.0f
        private const val MIN_UI_SCALE = 0.75f
        private const val MAX_UI_SCALE = 7.0f
        private const val MIN_DISPLAY_SCALE = 0.75f

        fun getMouseVelocityStat(): MouseVelocityStat = ViewerWindow.mouseVelocityStatHandle

        fun calcScaledRect(rect: Rect, scaleX: Float, scaleY: Float): Rect {
            return Rect(
                (rect.left   / scaleX).roundToInt(),
                (rect.bottom / scaleY).roundToInt(),
                (rect.right  / scaleX).roundToInt(),
                (rect.top    / scaleY).roundToInt()
            )
        }

        fun getLastSnapshotDir(): String {
            TODO("APR: use JVM equivalent to retrieve saved snapshot directory from settings")
        }

        fun loadUserImage(uuid: String) {
            TODO("APR: use JVM equivalent to fetch user profile image by UUID")
        }

        fun movieSize(newWidth: Int, newHeight: Int) {
            TODO("GPU: resize movie recording surface to $newWidth x $newHeight")
        }
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    fun getWindow(): NativeWindow? = nativeWindow
    fun getPlatformWindow(): Any? { TODO("APR: return native OS window handle") }
    fun getMediaWindow(): Any? { TODO("APR: return platform media window handle") }
    fun focusClient() { TODO("APR: request OS keyboard/input focus for this window") }

    fun getLastMouse(): Pair<Int,Int> = Pair(lastMouseX, lastMouseY)
    fun getCurrentMouse(): Pair<Int,Int> = Pair(currentMouseX, currentMouseY)
    fun getCurrentMouseDelta(): Pair<Int,Int> = Pair(currentMouseDX, currentMouseDY)

    fun getActive(): Boolean = isActive
    fun getUIVisibility(): Boolean = uiVisible
    fun getCursorHidden(): Boolean = cursorHidden
    fun getLastPick(): PickInfo = lastPick
    fun getInitAlert(): String = initAlert

    fun getWorldViewWidthScaled(): Int = worldViewRectScaled.width
    fun getWorldViewHeightScaled(): Int = worldViewRectScaled.height
    fun getWorldViewWidthRaw(): Int = worldViewRectRaw.width
    fun getWorldViewHeightRaw(): Int = worldViewRectRaw.height
    fun getWindowWidthScaled(): Int = windowRectScaled.width
    fun getWindowHeightScaled(): Int = windowRectScaled.height
    fun getWindowWidthRaw(): Int = windowRectRaw.width
    fun getWindowHeightRaw(): Int = windowRectRaw.height
    fun getWorldViewAspectRatio(): Float =
        if (worldViewRectRaw.height != 0) worldViewRectRaw.width.toFloat() / worldViewRectRaw.height else 1f

    // ------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------

    fun init(params: Params) {
        width  = params.width
        height = params.height
        windowRectRaw    = Rect(params.x, params.y, params.x + width, params.y + height)
        worldViewRectRaw = windowRectRaw
        calcDisplayScale()
        TODO("APR: create OS window via LLWindow equivalent; init GL, root view, fonts")
    }

    fun initGLDefaults() { TODO("GPU: set default GL state (depth test, blend, cull face, etc.)") }
    fun initBase() { TODO("GPU: create root view, progress view, popup view, debug text overlay") }
    fun initWorldUI() { TODO("GPU: instantiate HUD, status bar, toolbar, chiclet bar, nav bar") }
    fun initTextures(locationId: Int) { TODO("GPU: pre-load UI textures for location $locationId") }

    fun shutdownViews() { TODO("GPU: destroy all UI views, floaters, and popups") }
    fun shutdownGL() { TODO("GPU: tear down GL context and free all GPU resources") }

    // ------------------------------------------------------------------
    // Cursor control
    // ------------------------------------------------------------------

    fun showCursor() {
        cursorHidden = false
        TODO("GPU: restore OS cursor visibility")
    }

    fun hideCursor() {
        cursorHidden = true
        TODO("GPU: hide OS cursor")
    }

    fun setCursor(type: CursorType) {
        currentCursor = type
        TODO("GPU: apply OS cursor resource for $type")
    }

    fun moveCursorToCenter() {
        TODO("GPU: warp OS cursor to centre of window")
    }

    // ------------------------------------------------------------------
    // Reshape / resize
    // ------------------------------------------------------------------

    fun reshape(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        width  = w
        height = h
        windowRectRaw = Rect(windowRectRaw.left, windowRectRaw.bottom,
                             windowRectRaw.left + w, windowRectRaw.bottom + h)
        windowRectScaled = calcScaledRect(windowRectRaw, displayScaleX, displayScaleY)
        TODO("GPU: glViewport(0, 0, w, h); reshape root view; update world-view rect")
    }

    fun calcDisplayScale() {
        TODO("Query LLWindow pixel aspect ratio and UIScaleFactor setting; clamp to [$MIN_DISPLAY_SCALE..$MAX_UI_SCALE]; set displayScaleX/Y")
    }

    fun sendShapeToSim() {
        TODO("APR: send updated window shape to simulator via LLMessageSystem")
    }

    fun requestResolutionUpdate() { resDirty = true }
    fun checkSettings() { TODO("Apply any pending resolution/scale changes flagged by resDirty") }

    // ------------------------------------------------------------------
    // UI visibility
    // ------------------------------------------------------------------

    fun setUIVisibility(visible: Boolean) {
        uiVisible = visible
        TODO("GPU: show/hide root view children; refresh screen")
    }

    fun setNormalControlsVisible(visible: Boolean) {
        TODO("GPU: toggle top-level UI panels for $visible (used during login failure)")
    }

    fun setMenuBackgroundColor(godMode: Boolean = false, devGrid: Boolean = false) {
        TODO("GPU: apply theme color to menu bar background based on godMode=$godMode devGrid=$devGrid")
    }

    fun setBalanceVisible(visible: Boolean) {
        TODO("GPU: show/hide the L$ balance widget in status bar")
    }

    fun setTitle(winTitle: String) {
        TODO("APR: update OS window title to '$winTitle'")
    }

    // ------------------------------------------------------------------
    // Rendering setup
    // ------------------------------------------------------------------

    fun setup2DViewport(xOffset: Int = 0, yOffset: Int = 0) {
        TODO("GPU: glViewport for 2-D UI layer with offset ($xOffset, $yOffset)")
    }

    fun setup3DViewport(xOffset: Int = 0, yOffset: Int = 0) {
        TODO("GPU: glViewport for 3-D world layer with offset ($xOffset, $yOffset)")
    }

    fun setup3DRender() { TODO("GPU: configure projection/modelview matrices for 3-D render pass") }
    fun setup2DRender() { TODO("GPU: configure orthographic projection for UI render pass") }

    // ------------------------------------------------------------------
    // Per-frame update
    // ------------------------------------------------------------------

    fun updateUI() {
        updateMouseDelta()
        updateKeyboardFocus()
        updateLayout()
        updateObjectUnderCursor()
    }

    fun updateLayout() {
        TODO("Reflow UI layout: recalculate world-view rect, toolbar positions, etc.")
    }

    fun updateMouseDelta() {
        TODO("Compute currentMouseDelta from currentMouse - lastMouse; update velocity stat")
    }

    fun updateKeyboardFocus() {
        TODO("Advance keyboard focus, handle focus-cycling, tool override from modifier keys")
    }

    fun updateObjectUnderCursor() {
        TODO("GPU: schedule hover pick and update cursor icon / tooltip for hovered object")
    }

    fun updateWorldViewRect(useFullWindow: Boolean = false) {
        TODO("Recompute worldViewRectRaw / Scaled from toolbar heights and full-window flag")
    }

    fun updateDebugText() {
        TODO("Rebuild debug-text overlay lines (FPS, camera pos, memory, render stats)")
    }

    fun drawDebugText() {
        TODO("GPU: render debug-text overlay lines onto the 2-D viewport")
    }

    fun draw() {
        TODO("GPU: orchestrate full-frame draw: 3-D world scene, HUD objects, 2-D UI, debug overlays")
    }

    // ------------------------------------------------------------------
    // Mouse event handlers  (mirrors LLWindowCallbacks virtuals)
    // ------------------------------------------------------------------

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        leftMouseDown = true
        TODO("Dispatch left-down event through ViewerInput → tool manager → UI")
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        leftMouseDown = false
        TODO("Dispatch left-up event through ViewerInput → tool manager → UI")
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        rightMouseDown = true
        TODO("Dispatch right-down event through ViewerInput → tool manager → UI")
    }

    fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean {
        rightMouseDown = false
        TODO("Dispatch right-up event through ViewerInput → tool manager → UI")
    }

    fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        middleMouseDown = true
        TODO("Dispatch middle-down event through ViewerInput")
    }

    fun handleMiddleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        middleMouseDown = false
        TODO("Dispatch middle-up event through ViewerInput")
    }

    fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        TODO("Try CLICK_DOUBLELEFT dispatch; fall back to handleMouseDown")
    }

    fun handleMouseMove(x: Int, y: Int, mask: Int) {
        lastMouseX    = currentMouseX
        lastMouseY    = currentMouseY
        currentMouseX = x
        currentMouseY = y
        isMouseInWindow = true
        TODO("GPU: update hover pick, tooltip, mouselook delta")
    }

    fun handleMouseDragged(x: Int, y: Int, mask: Int) {
        handleMouseMove(x, y, mask)
    }

    fun handleMouseLeave() {
        isMouseInWindow = false
    }

    fun handleScrollWheel(clicks: Int) {
        TODO("Dispatch scroll-wheel event to focused view or tool manager")
    }

    fun handleScrollHWheel(clicks: Int) {
        TODO("Dispatch horizontal scroll-wheel event to focused view")
    }

    fun handleTranslatedKeyDown(key: Int, mask: Int, repeated: Boolean): Boolean {
        TODO("Dispatch translated key-down event; handle mouselook, pie-menu hot-keys")
    }

    fun handleTranslatedKeyUp(key: Int, mask: Int): Boolean {
        TODO("Dispatch translated key-up event; restore tool override if modifier released")
    }

    fun handleScanKey(key: Int, keyDown: Boolean, keyUp: Boolean, keyLevel: Boolean) {
        TODO("Forward raw scan-code event to ViewerInput for joystick/mouselook")
    }

    fun handleUnicodeChar(uniChar: Int, mask: Int): Boolean {
        TODO("Pass unicode character to focused text-edit widget")
    }

    fun handleResize(w: Int, h: Int) = reshape(w, h)

    fun handleFocus() {
        isActive = true
        TODO("APR: unmute audio, resume watch-dog, notify focus manager")
    }

    fun handleFocusLost() {
        isActive = false
        TODO("APR: optionally mute audio, release mouse capture")
    }

    fun handleActivate(activated: Boolean): Boolean {
        isActive = activated
        TODO("APR: update away-timer, audio muting based on activated=$activated")
    }

    fun handleCloseRequest(): Boolean {
        TODO("Show confirm-quit dialog; return true to allow close, false to cancel")
    }

    fun handleQuit() {
        TODO("APR: initiate orderly viewer shutdown sequence")
    }

    fun handleDragNDrop(x: Int, y: Int, mask: Int, action: DragNDropAction, data: String): DragNDropResult {
        TODO("Dispatch DnD event: check SLURL / prim-media targets, call pickImmediate if needed")
    }

    fun handleDPIChanged(uiScaleFactor: Float, windowWidth: Int, windowHeight: Int): Boolean {
        TODO("Update displayScale from OS DPI change; reshape and recalculate rects")
    }

    fun handleDisplayChanged(): Boolean {
        TODO("APR: handle monitor-change event; rebuild GL context if needed")
    }

    fun handleTimerEvent(): Boolean {
        TODO("APR: service pending pick timer callbacks")
    }

    fun handlePieMenu(x: Int, y: Int, mask: Int) {
        TODO("Open pie/context menu at ($x, $y) for the object under cursor")
    }

    // ------------------------------------------------------------------
    // Picking
    // ------------------------------------------------------------------

    fun pickAsync(
        x: Int,
        yFromBot: Int,
        mask: Int = 0,
        callback: (PickInfo) -> Unit,
        pickTransparent: Boolean = false,
        pickRigged: Boolean = false,
        pickUnselectable: Boolean = false,
        pickReflectionProbes: Boolean = false
    ) {
        TODO("GPU: enqueue pick pass at ($x, $yFromBot); deliver PickInfo to callback on completion")
    }

    fun pickImmediate(
        x: Int,
        y: Int,
        pickTransparent: Boolean,
        pickRigged: Boolean = false,
        pickParticle: Boolean = false,
        pickUnselectable: Boolean = true,
        pickReflectionProbe: Boolean = false
    ): PickInfo {
        TODO("GPU: render pick framebuffer synchronously and decode PickInfo from RGBA pixel")
    }

    fun performPick() {
        TODO("GPU: flush pending pick queue, read framebuffer results, invoke callbacks")
    }

    fun returnEmptyPicks() {
        pendingPicks.forEach { TODO("Invoke each pending pick callback with PICK_INVALID result") }
        pendingPicks.clear()
    }

    fun renderSelections(forGlPick: Boolean, pickParcelWalls: Boolean, forHud: Boolean) {
        TODO("GPU: draw selection boxes around currently selected objects")
    }

    fun cursorIntersect(
        mouseX: Int = -1,
        mouseY: Int = -1,
        depth: Float = 512f,
        thisObject: Any? = null,
        thisFace: Int = -1,
        pickTransparent: Boolean = false,
        pickRigged: Boolean = false,
        pickUnselectable: Boolean = true,
        pickReflectionProbe: Boolean = true
    ): Any? {
        TODO("GPU: ray-cast into scene from cursor position; return intersected ViewerObject or null")
    }

    // ------------------------------------------------------------------
    // Mouse / ray helpers
    // ------------------------------------------------------------------

    fun mouseDirectionGlobal(x: Int, y: Int): Vector3 {
        TODO("Unproject screen point ($x,$y) through inverse projection matrix to world ray dir")
    }

    fun mouseDirectionCamera(x: Int, y: Int): Vector3 {
        TODO("Unproject screen point ($x,$y) into camera space direction vector")
    }

    fun mousePointHUD(x: Int, y: Int): Vector3 {
        TODO("Unproject screen point ($x,$y) onto HUD plane")
    }

    fun mousePointOnLandGlobal(x: Int, y: Int, ignoreDistance: Boolean = false): Vector3? {
        TODO("Ray-cast against terrain mesh from ($x,$y); return global hit point or null on miss")
    }

    fun mousePointOnPlaneGlobal(x: Int, y: Int, planePoint: Vector3, planeNormal: Vector3): Vector3? {
        TODO("Ray-plane intersection for the plane through planePoint with planeNormal")
    }

    fun clickPointInWorldGlobal(x: Int, yFromBot: Int, clickedObject: Any?): Vector3 {
        TODO("Compute global world-space point for click at ($x, $yFromBot) on clickedObject surface")
    }

    fun clickPointOnSurfaceGlobal(x: Int, y: Int, objectp: Any?): Vector3? {
        TODO("Ray-cast onto specific object surface; return global point or null on miss")
    }

    // ------------------------------------------------------------------
    // Progress / startup display
    // ------------------------------------------------------------------

    fun setShowProgress(show: Boolean, fullscreen: Boolean) {
        TODO("GPU: show or hide the login/loading progress view (fullscreen=$fullscreen)")
    }

    fun getShowProgress(): Boolean {
        TODO("Return whether the progress view is currently visible")
    }

    fun setProgressString(string: String) {
        TODO("Update the primary status string on the progress view")
    }

    fun setProgressPercent(percent: Float) {
        TODO("Update the percentage bar on the progress view to $percent%%")
    }

    fun setProgressMessage(msg: String) {
        TODO("Update the secondary message string on the progress view")
    }

    fun setProgressCancelButtonVisible(visible: Boolean, label: String = "") {
        TODO("Show or hide the cancel button on the progress view")
    }

    fun revealIntroPanel() {
        TODO("GPU: transition from progress view to the main intro/login panel")
    }

    fun setStartupComplete() {
        TODO("GPU: dismiss progress view; show normal UI controls")
    }

    // ------------------------------------------------------------------
    // Popup management
    // ------------------------------------------------------------------

    fun addPopup(popup: Any) {
        TODO("GPU: add a transient popup view to the popup layer")
    }

    fun removePopup(popup: Any) {
        TODO("GPU: remove a transient popup view from the popup layer")
    }

    fun clearPopups() {
        TODO("GPU: remove all transient popup views from the popup layer")
    }

    // ------------------------------------------------------------------
    // Snapshot helpers
    // ------------------------------------------------------------------

    fun saveSnapshot(
        filename: String,
        imageWidth: Int,
        imageHeight: Int,
        showUI: Boolean = true,
        showHUD: Boolean = true,
        doRebuild: Boolean = false,
        showBalance: Boolean = true
    ): Boolean {
        TODO("GPU: render off-screen framebuffer at ${imageWidth}x${imageHeight} and encode to $filename")
    }

    fun rawSnapshot(
        imageWidth: Int,
        imageHeight: Int,
        keepWindowAspect: Boolean = true,
        isTexture: Boolean = false,
        showUI: Boolean = true,
        showHUD: Boolean = true,
        doRebuild: Boolean = false,
        noPost: Boolean = false,
        showBalance: Boolean = true
    ): ByteArray? {
        TODO("GPU: render off-screen framebuffer and return raw RGBA bytes")
    }

    fun simpleSnapshot(imageWidth: Int, imageHeight: Int, numRenderPasses: Int): Boolean {
        TODO("GPU: render $numRenderPasses passes into off-screen buffer at ${imageWidth}x${imageHeight}")
    }

    fun cubeSnapshot(
        origin: Vector3,
        index: Int,
        face: Int,
        nearClip: Float,
        renderAvatars: Boolean
    ): Boolean {
        TODO("GPU: render one face of a cubemap at origin $origin into cube array index $index face $face")
    }

    fun reflectionSnapshot(imageWidth: Int, imageHeight: Int, numRenderPasses: Int): Boolean {
        TODO("GPU: specialised simpleSnapshot for reflection map probes")
    }

    fun thumbnailSnapshot(
        previewWidth: Int,
        previewHeight: Int,
        showUI: Boolean,
        showHUD: Boolean,
        doRebuild: Boolean,
        noPost: Boolean
    ): ByteArray? {
        TODO("GPU: render thumbnail at ${previewWidth}x${previewHeight} and return raw bytes")
    }

    fun isSnapshotLocSet(): Boolean {
        TODO("Check whether a custom snapshot save directory has been set in settings")
    }

    fun resetSnapshotLoc() {
        TODO("Clear custom snapshot save directory so next save opens a directory picker")
    }

    fun playSnapshotAnimAndSound() {
        TODO("APR: trigger camera-click animation on avatar and play shutter sound")
    }

    fun saveImageNumbered(image: Any, forcePicker: Boolean, onSuccess: () -> Unit, onFailure: () -> Unit) {
        TODO("APR: write image to numbered file in snapshot dir (or open picker if forcePicker)")
    }

    // ------------------------------------------------------------------
    // Miscellaneous
    // ------------------------------------------------------------------

    fun dumpState() {
        TODO("APR: log window dimensions, GL state, and pick state to the log file")
    }

    fun saveLastMouse(x: Int, y: Int) {
        lastMouseX = x
        lastMouseY = y
    }

    fun shouldShowToolTip(mouseHandler: Any): Boolean {
        TODO("Return true if mouse has dwelt on mouseHandler long enough to show a tooltip")
    }

    fun getChatConsoleBottomPad(): Int {
        TODO("Compute vertical padding below chat console based on UI element heights")
    }

    fun getFloaterSnapRegion(): Any? {
        TODO("Return the floater snap region view reference")
    }

    fun getChicletContainer(): Any? {
        TODO("Return the chiclet container panel reference")
    }

    fun getToolBarHolder(): Any? {
        TODO("Return the toolbar holder view reference")
    }

    fun getHintHolder(): Any? {
        TODO("Return the hint holder view reference")
    }

    fun getLoginPanelHolder(): Any? {
        TODO("Return the login panel holder view reference")
    }
}

// ---------------------------------------------------------------------------
// Debug raycast globals  (mirrors extern vars in llviewerwindow.h)
// ---------------------------------------------------------------------------

object DebugRaycast {
    var objectId: String = ""
    var intersection: FloatArray = FloatArray(4)
    var particleOwnerId: String = ""
    var particleIntersection: FloatArray = FloatArray(4)
    var texCoord: Pair<Float, Float> = Pair(0f, 0f)
    var normal: FloatArray = FloatArray(4)
    var tangent: FloatArray = FloatArray(4)
    var faceHit: Int = -1
    var start: FloatArray = FloatArray(4)
    var end: FloatArray = FloatArray(4)
}

var displayCameraPos: Boolean = false
var displayWindInfo: Boolean = false
var displayFOV: Boolean = false
var displayBadge: Boolean = false
