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

    fun fetchResults() {
        // ViewerWindow: read pick framebuffer and populate intersection/UV/normal not yet implemented
    }
    fun getSurfaceInfo() {
        // ViewerWindow: decode surface normal, UV and binormal from pick buffer not yet implemented
    }

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
            System.err.println("ViewerWindow: getLastSnapshotDir not yet implemented")
            return ""
        }

        fun loadUserImage(uuid: String) {
            System.err.println("ViewerWindow: loadUserImage not yet implemented")
        }

        fun movieSize(newWidth: Int, newHeight: Int) {
            // ViewerWindow: resize movie recording surface not yet implemented
        }
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    fun getWindow(): NativeWindow? = nativeWindow
    fun getPlatformWindow(): Any? {
        System.err.println("ViewerWindow: getPlatformWindow not yet implemented")
        return null
    }
    fun getMediaWindow(): Any? {
        System.err.println("ViewerWindow: getMediaWindow not yet implemented")
        return null
    }
    fun focusClient() {
        System.err.println("ViewerWindow: focusClient not yet implemented")
    }

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
        System.err.println("ViewerWindow: create OS window via LLWindow equivalent; init GL, root view, fonts not yet implemented")
    }

    fun initGLDefaults() {
        // ViewerWindow: set default GL state (depth test, blend, cull face, etc.) not yet implemented
    }
    fun initBase() {
        // ViewerWindow: create root view, progress view, popup view, debug text overlay not yet implemented
    }
    fun initWorldUI() {
        // ViewerWindow: instantiate HUD, status bar, toolbar, chiclet bar, nav bar not yet implemented
    }
    fun initTextures(locationId: Int) {
        // ViewerWindow: pre-load UI textures not yet implemented
    }

    fun shutdownViews() {
        // ViewerWindow: destroy all UI views, floaters, and popups not yet implemented
    }
    fun shutdownGL() {
        // ViewerWindow: tear down GL context and free all GPU resources not yet implemented
    }

    // ------------------------------------------------------------------
    // Cursor control
    // ------------------------------------------------------------------

    fun showCursor() {
        cursorHidden = false
        // ViewerWindow: restore OS cursor visibility not yet implemented
    }

    fun hideCursor() {
        cursorHidden = true
        // ViewerWindow: hide OS cursor not yet implemented
    }

    fun setCursor(type: CursorType) {
        currentCursor = type
        // ViewerWindow: apply OS cursor resource not yet implemented
    }

    fun moveCursorToCenter() {
        // ViewerWindow: warp OS cursor to centre of window not yet implemented
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
        // ViewerWindow: glViewport(0, 0, w, h); reshape root view; update world-view rect not yet implemented
    }

    fun calcDisplayScale() {
        // ViewerWindow: Query LLWindow pixel aspect ratio and UIScaleFactor setting; clamp and set displayScaleX/Y not yet implemented
    }

    fun sendShapeToSim() {
        System.err.println("ViewerWindow: sendShapeToSim not yet implemented")
    }

    fun requestResolutionUpdate() { resDirty = true }
    fun checkSettings() {
        // ViewerWindow: apply any pending resolution/scale changes flagged by resDirty not yet implemented
    }

    // ------------------------------------------------------------------
    // UI visibility
    // ------------------------------------------------------------------

    fun setUIVisibility(visible: Boolean) {
        uiVisible = visible
        // ViewerWindow: show/hide root view children; refresh screen not yet implemented
    }

    fun setNormalControlsVisible(visible: Boolean) {
        // ViewerWindow: toggle top-level UI panels not yet implemented
    }

    fun setMenuBackgroundColor(godMode: Boolean = false, devGrid: Boolean = false) {
        // ViewerWindow: apply theme color to menu bar background not yet implemented
    }

    fun setBalanceVisible(visible: Boolean) {
        // ViewerWindow: show/hide the L$ balance widget in status bar not yet implemented
    }

    fun setTitle(winTitle: String) {
        System.err.println("ViewerWindow: setTitle not yet implemented")
    }

    // ------------------------------------------------------------------
    // Rendering setup
    // ------------------------------------------------------------------

    fun setup2DViewport(xOffset: Int = 0, yOffset: Int = 0) {
        // ViewerWindow: glViewport for 2-D UI layer not yet implemented
    }

    fun setup3DViewport(xOffset: Int = 0, yOffset: Int = 0) {
        // ViewerWindow: glViewport for 3-D world layer not yet implemented
    }

    fun setup3DRender() {
        // ViewerWindow: configure projection/modelview matrices for 3-D render pass not yet implemented
    }
    fun setup2DRender() {
        // ViewerWindow: configure orthographic projection for UI render pass not yet implemented
    }

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
        // ViewerWindow: reflow UI layout not yet implemented
    }

    fun updateMouseDelta() {
        // ViewerWindow: compute currentMouseDelta from currentMouse - lastMouse; update velocity stat not yet implemented
    }

    fun updateKeyboardFocus() {
        // ViewerWindow: advance keyboard focus, handle focus-cycling, tool override from modifier keys not yet implemented
    }

    fun updateObjectUnderCursor() {
        // ViewerWindow: schedule hover pick and update cursor icon / tooltip for hovered object not yet implemented
    }

    fun updateWorldViewRect(useFullWindow: Boolean = false) {
        // ViewerWindow: recompute worldViewRectRaw / Scaled from toolbar heights not yet implemented
    }

    fun updateDebugText() {
        // ViewerWindow: rebuild debug-text overlay lines (FPS, camera pos, memory, render stats) not yet implemented
    }

    fun drawDebugText() {
        // ViewerWindow: render debug-text overlay lines onto the 2-D viewport not yet implemented
    }

    fun draw() {
        // ViewerWindow: orchestrate full-frame draw: 3-D world scene, HUD objects, 2-D UI, debug overlays not yet implemented
    }

    // ------------------------------------------------------------------
    // Mouse event handlers  (mirrors LLWindowCallbacks virtuals)
    // ------------------------------------------------------------------

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        leftMouseDown = true
        System.err.println("ViewerWindow: handleMouseDown not yet implemented")
        return false
    }

    fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        leftMouseDown = false
        System.err.println("ViewerWindow: handleMouseUp not yet implemented")
        return false
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        rightMouseDown = true
        System.err.println("ViewerWindow: handleRightMouseDown not yet implemented")
        return false
    }

    fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean {
        rightMouseDown = false
        System.err.println("ViewerWindow: handleRightMouseUp not yet implemented")
        return false
    }

    fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        middleMouseDown = true
        System.err.println("ViewerWindow: handleMiddleMouseDown not yet implemented")
        return false
    }

    fun handleMiddleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        middleMouseDown = false
        System.err.println("ViewerWindow: handleMiddleMouseUp not yet implemented")
        return false
    }

    fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        System.err.println("ViewerWindow: handleDoubleClick not yet implemented")
        return false
    }

    fun handleMouseMove(x: Int, y: Int, mask: Int) {
        lastMouseX    = currentMouseX
        lastMouseY    = currentMouseY
        currentMouseX = x
        currentMouseY = y
        isMouseInWindow = true
        // ViewerWindow: update hover pick, tooltip, mouselook delta not yet implemented
    }

    fun handleMouseDragged(x: Int, y: Int, mask: Int) {
        handleMouseMove(x, y, mask)
    }

    fun handleMouseLeave() {
        isMouseInWindow = false
    }

    fun handleScrollWheel(clicks: Int) {
        System.err.println("ViewerWindow: handleScrollWheel not yet implemented")
    }

    fun handleScrollHWheel(clicks: Int) {
        System.err.println("ViewerWindow: handleScrollHWheel not yet implemented")
    }

    fun handleTranslatedKeyDown(key: Int, mask: Int, repeated: Boolean): Boolean {
        System.err.println("ViewerWindow: handleTranslatedKeyDown not yet implemented")
        return false
    }

    fun handleTranslatedKeyUp(key: Int, mask: Int): Boolean {
        System.err.println("ViewerWindow: handleTranslatedKeyUp not yet implemented")
        return false
    }

    fun handleScanKey(key: Int, keyDown: Boolean, keyUp: Boolean, keyLevel: Boolean) {
        System.err.println("ViewerWindow: handleScanKey not yet implemented")
    }

    fun handleUnicodeChar(uniChar: Int, mask: Int): Boolean {
        System.err.println("ViewerWindow: handleUnicodeChar not yet implemented")
        return false
    }

    fun handleResize(w: Int, h: Int) = reshape(w, h)

    fun handleFocus() {
        isActive = true
        System.err.println("ViewerWindow: handleFocus not yet implemented")
    }

    fun handleFocusLost() {
        isActive = false
        System.err.println("ViewerWindow: handleFocusLost not yet implemented")
    }

    fun handleActivate(activated: Boolean): Boolean {
        isActive = activated
        System.err.println("ViewerWindow: handleActivate not yet implemented")
        return false
    }

    fun handleCloseRequest(): Boolean {
        System.err.println("ViewerWindow: handleCloseRequest not yet implemented")
        return false
    }

    fun handleQuit() {
        System.err.println("ViewerWindow: handleQuit not yet implemented")
    }

    fun handleDragNDrop(x: Int, y: Int, mask: Int, action: DragNDropAction, data: String): DragNDropResult {
        System.err.println("ViewerWindow: handleDragNDrop not yet implemented")
        return DragNDropResult.NONE
    }

    fun handleDPIChanged(uiScaleFactor: Float, windowWidth: Int, windowHeight: Int): Boolean {
        System.err.println("ViewerWindow: handleDPIChanged not yet implemented")
        return false
    }

    fun handleDisplayChanged(): Boolean {
        System.err.println("ViewerWindow: handleDisplayChanged not yet implemented")
        return false
    }

    fun handleTimerEvent(): Boolean {
        System.err.println("ViewerWindow: handleTimerEvent not yet implemented")
        return false
    }

    fun handlePieMenu(x: Int, y: Int, mask: Int) {
        System.err.println("ViewerWindow: handlePieMenu not yet implemented")
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
        // ViewerWindow: enqueue pick pass; deliver PickInfo to callback on completion not yet implemented
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
        // ViewerWindow: render pick framebuffer synchronously and decode PickInfo not yet implemented
        return PickInfo()
    }

    fun performPick() {
        // ViewerWindow: flush pending pick queue, read framebuffer results, invoke callbacks not yet implemented
    }

    fun returnEmptyPicks() {
        pendingPicks.forEach { _ ->
            // ViewerWindow: invoke each pending pick callback with PICK_INVALID result not yet implemented
        }
        pendingPicks.clear()
    }

    fun renderSelections(forGlPick: Boolean, pickParcelWalls: Boolean, forHud: Boolean) {
        // ViewerWindow: draw selection boxes around currently selected objects not yet implemented
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
        // ViewerWindow: ray-cast into scene from cursor position not yet implemented
        return null
    }

    // ------------------------------------------------------------------
    // Mouse / ray helpers
    // ------------------------------------------------------------------

    fun mouseDirectionGlobal(x: Int, y: Int): Vector3 {
        System.err.println("ViewerWindow: mouseDirectionGlobal not yet implemented")
        return Vector3.ZERO
    }

    fun mouseDirectionCamera(x: Int, y: Int): Vector3 {
        System.err.println("ViewerWindow: mouseDirectionCamera not yet implemented")
        return Vector3.ZERO
    }

    fun mousePointHUD(x: Int, y: Int): Vector3 {
        System.err.println("ViewerWindow: mousePointHUD not yet implemented")
        return Vector3.ZERO
    }

    fun mousePointOnLandGlobal(x: Int, y: Int, ignoreDistance: Boolean = false): Vector3? {
        System.err.println("ViewerWindow: mousePointOnLandGlobal not yet implemented")
        return null
    }

    fun mousePointOnPlaneGlobal(x: Int, y: Int, planePoint: Vector3, planeNormal: Vector3): Vector3? {
        System.err.println("ViewerWindow: mousePointOnPlaneGlobal not yet implemented")
        return null
    }

    fun clickPointInWorldGlobal(x: Int, yFromBot: Int, clickedObject: Any?): Vector3 {
        System.err.println("ViewerWindow: clickPointInWorldGlobal not yet implemented")
        return Vector3.ZERO
    }

    fun clickPointOnSurfaceGlobal(x: Int, y: Int, objectp: Any?): Vector3? {
        System.err.println("ViewerWindow: clickPointOnSurfaceGlobal not yet implemented")
        return null
    }

    // ------------------------------------------------------------------
    // Progress / startup display
    // ------------------------------------------------------------------

    fun setShowProgress(show: Boolean, fullscreen: Boolean) {
        // ViewerWindow: show or hide the login/loading progress view not yet implemented
    }

    fun getShowProgress(): Boolean {
        // ViewerWindow: return whether the progress view is currently visible not yet implemented
        return false
    }

    fun setProgressString(string: String) {
        // ViewerWindow: update the primary status string on the progress view not yet implemented
    }

    fun setProgressPercent(percent: Float) {
        // ViewerWindow: update the percentage bar on the progress view not yet implemented
    }

    fun setProgressMessage(msg: String) {
        // ViewerWindow: update the secondary message string on the progress view not yet implemented
    }

    fun setProgressCancelButtonVisible(visible: Boolean, label: String = "") {
        // ViewerWindow: show or hide the cancel button on the progress view not yet implemented
    }

    fun revealIntroPanel() {
        // ViewerWindow: transition from progress view to the main intro/login panel not yet implemented
    }

    fun setStartupComplete() {
        // ViewerWindow: dismiss progress view; show normal UI controls not yet implemented
    }

    // ------------------------------------------------------------------
    // Popup management
    // ------------------------------------------------------------------

    fun addPopup(popup: Any) {
        // ViewerWindow: add a transient popup view to the popup layer not yet implemented
    }

    fun removePopup(popup: Any) {
        // ViewerWindow: remove a transient popup view from the popup layer not yet implemented
    }

    fun clearPopups() {
        // ViewerWindow: remove all transient popup views from the popup layer not yet implemented
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
        // ViewerWindow: render off-screen framebuffer and encode to file not yet implemented
        return false
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
        // ViewerWindow: render off-screen framebuffer and return raw RGBA bytes not yet implemented
        return null
    }

    fun simpleSnapshot(imageWidth: Int, imageHeight: Int, numRenderPasses: Int): Boolean {
        // ViewerWindow: render passes into off-screen buffer not yet implemented
        return false
    }

    fun cubeSnapshot(
        origin: Vector3,
        index: Int,
        face: Int,
        nearClip: Float,
        renderAvatars: Boolean
    ): Boolean {
        // ViewerWindow: render one face of a cubemap not yet implemented
        return false
    }

    fun reflectionSnapshot(imageWidth: Int, imageHeight: Int, numRenderPasses: Int): Boolean {
        // ViewerWindow: specialised simpleSnapshot for reflection map probes not yet implemented
        return false
    }

    fun thumbnailSnapshot(
        previewWidth: Int,
        previewHeight: Int,
        showUI: Boolean,
        showHUD: Boolean,
        doRebuild: Boolean,
        noPost: Boolean
    ): ByteArray? {
        // ViewerWindow: render thumbnail and return raw bytes not yet implemented
        return null
    }

    fun isSnapshotLocSet(): Boolean {
        // ViewerWindow: check whether a custom snapshot save directory has been set not yet implemented
        return false
    }

    fun resetSnapshotLoc() {
        // ViewerWindow: clear custom snapshot save directory not yet implemented
    }

    fun playSnapshotAnimAndSound() {
        System.err.println("ViewerWindow: playSnapshotAnimAndSound not yet implemented")
    }

    fun saveImageNumbered(image: Any, forcePicker: Boolean, onSuccess: () -> Unit, onFailure: () -> Unit) {
        System.err.println("ViewerWindow: saveImageNumbered not yet implemented")
    }

    // ------------------------------------------------------------------
    // Miscellaneous
    // ------------------------------------------------------------------

    fun dumpState() {
        System.err.println("ViewerWindow: dumpState not yet implemented")
    }

    fun saveLastMouse(x: Int, y: Int) {
        lastMouseX = x
        lastMouseY = y
    }

    fun shouldShowToolTip(mouseHandler: Any): Boolean {
        // ViewerWindow: return true if mouse has dwelt on mouseHandler long enough not yet implemented
        return false
    }

    fun getChatConsoleBottomPad(): Int {
        // ViewerWindow: compute vertical padding below chat console not yet implemented
        return 0
    }

    fun getFloaterSnapRegion(): Any? {
        // ViewerWindow: return the floater snap region view reference not yet implemented
        return null
    }

    fun getChicletContainer(): Any? {
        // ViewerWindow: return the chiclet container panel reference not yet implemented
        return null
    }

    fun getToolBarHolder(): Any? {
        // ViewerWindow: return the toolbar holder view reference not yet implemented
        return null
    }

    fun getHintHolder(): Any? {
        // ViewerWindow: return the hint holder view reference not yet implemented
        return null
    }

    fun getLoginPanelHolder(): Any? {
        // ViewerWindow: return the login panel holder view reference not yet implemented
        return null
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
