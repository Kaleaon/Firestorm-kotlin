// ViewerWindow.kt — converted from llviewerwindow.h / llviewerwindow.cpp
// Copyright (C) 2001, Linden Research, Inc. LGPL 2.1
package com.firestorm.newview

import com.firestorm.llmath.Vector3

// ---------------------------------------------------------------------------
// CursorType enum  (mirrors ECursorType / UI_CURSOR_* from llcursortypes.h)
// ---------------------------------------------------------------------------

enum class CursorType {
    ARROW,
    WAIT,
    HAND,
    IBEAM,
    CROSS,
    SIZENWSE,
    SIZENESW,
    SIZEWE,
    SIZENS,
    SIZEALL,
    NO,
    WORKING,
    TOOLGRAB,
    TOOLLAND,
    TOOLFOCUS,
    TOOLCREATE,
    ARROWDRAG,
    ARROWCOPY,
    ARROWDRAGMULTI,
    ARROWCOPYMULTI,
    NOLOCKED,
    ARROWLOCKED,
    GRABLOCKED,
    TOOLTRANSLATE,
    TOOLROTATE,
    TOOLSCALE,
    TOOLCAMERA,
    TOOLPAN,
    TOOLZOOMIN,
    TOOLZOOMOUT,
    TOOLPICKOBJECT3,
    TOOLPLAY,
    TOOLPAUSE,
    TOOLMEDIAOPEN,
    PIPETTE,
    TOOLSIT,
    TOOLBUY,
    TOOLPAY,
    TOOLOPEN,
}

// ---------------------------------------------------------------------------
// Pick result (mirrors LLPickInfo)
// ---------------------------------------------------------------------------

/** Result of a single scene pick operation. */
data class PickInfo(
    val mouseX: Int = 0,
    val mouseY: Int = 0,
    val objectId: com.firestorm.llcommon.LLUUID = com.firestorm.llcommon.LLUUID.NULL,
    val posGlobal: Vector3 = Vector3.ZERO,
    val objectFace: Int = -1,
    val isValid: Boolean = false
) {
    enum class PickType { OBJECT, FLORA, LAND, ICON, PARCEL_WALL, INVALID }

    val pickType: PickType = if (isValid) PickType.OBJECT else PickType.INVALID
}

// ---------------------------------------------------------------------------
// MouseVelocityStat — lightweight stand-in for LLTrace::SampleStatHandle
// ---------------------------------------------------------------------------

/**
 * Accumulates mouse speed samples so callers can read average velocity.
 * The C++ side uses LLTrace::SampleStatHandle<F32>.
 */
class MouseVelocityStat {
    private var lastSample: Float = 0f

    fun record(pixelsPerSecond: Float) { lastSample = pixelsPerSecond }
    fun getCurrentSample(): Float = lastSample
}

// ---------------------------------------------------------------------------
// Opaque window handle
// ---------------------------------------------------------------------------

/** Platform-level window handle; wraps whatever the OS provides. */
class NativeWindow

// ---------------------------------------------------------------------------
// ViewerWindow singleton
// ---------------------------------------------------------------------------

/**
 * Manages the viewer's OS window: dimensions, cursor, picking, and rendering
 * setup. Maps to the C++ LLViewerWindow singleton (gViewerWindow).
 */
object ViewerWindow {

    // ------------------------------------------------------------------
    // Window dimensions — raw pixel values
    // ------------------------------------------------------------------

    /** Raw pixel width of the entire OS window. */
    var width: Int = 1024
        private set

    /** Raw pixel height of the entire OS window. */
    var height: Int = 768
        private set

    /**
     * Display scale factor applied on HiDPI / Retina screens.
     * Mirrors mDisplayScale (X component; X == Y for square pixels).
     */
    var displayScale: Float = 1.0f
        private set

    // Scaled dimensions (pixels / displayScale), used for most UI math
    val widthScaled: Int  get() = (width  / displayScale).toInt()
    val heightScaled: Int get() = (height / displayScale).toInt()

    // ------------------------------------------------------------------
    // Mouse state
    // ------------------------------------------------------------------

    var lastMouseX: Int = 0; private set
    var lastMouseY: Int = 0; private set
    var currentMouseX: Int = 0; private set
    var currentMouseY: Int = 0; private set

    var leftMouseDown:   Boolean = false; private set
    var middleMouseDown: Boolean = false; private set
    var rightMouseDown:  Boolean = false; private set

    private val _mouseVelocityStat = MouseVelocityStat()

    var isActive: Boolean = false
        private set

    // ------------------------------------------------------------------
    // Cursor state
    // ------------------------------------------------------------------

    private var cursorHidden: Boolean = false
    private var currentCursor: CursorType = CursorType.ARROW

    // ------------------------------------------------------------------
    // Pick state
    // ------------------------------------------------------------------

    var lastPick: PickInfo = PickInfo()
        private set

    // ------------------------------------------------------------------
    // Native window handle
    // ------------------------------------------------------------------

    private var nativeWindow: NativeWindow? = null

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    /** Scaled display width; used by most UI layout code. */
    fun getWindowDisplayWidth(): Int  = widthScaled

    /** Scaled display height; used by most UI layout code. */
    fun getWindowDisplayHeight(): Int = heightScaled

    /** Raw pixel width (matches mWindowRectRaw). */
    fun getWindowWidthRaw(): Int  = width

    /** Raw pixel height (matches mWindowRectRaw). */
    fun getWindowHeightRaw(): Int = height

    /** Returns the mouse-velocity sample handle (read-only). */
    fun getMouseVelocityStat(): MouseVelocityStat = _mouseVelocityStat

    /** Returns the platform native window object. */
    fun getWindow(): NativeWindow? = nativeWindow

    fun isCursorHidden(): Boolean = cursorHidden

    // ------------------------------------------------------------------
    // Cursor control
    // ------------------------------------------------------------------

    fun showCursor() {
        cursorHidden = false
        TODO("GPU: show OS cursor")
    }

    fun hideCursor() {
        cursorHidden = true
        TODO("GPU: hide OS cursor")
    }

    fun setCursor(type: CursorType) {
        currentCursor = type
        TODO("GPU: set OS cursor to $type")
    }

    fun moveCursorToCenter() {
        TODO("GPU: warp OS cursor to window centre")
    }

    // ------------------------------------------------------------------
    // Reshape / resize
    // ------------------------------------------------------------------

    /**
     * Called when the OS window is resized.
     * Mirrors LLViewerWindow::reshape(S32 width, S32 height).
     */
    fun reshape(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        width = w
        height = h
        TODO("GPU: update GL viewport, recalculate scaled rects, reshape root view")
    }

    /**
     * Recalculate [displayScale] from the OS pixel-aspect and UI scale factor.
     * Mirrors LLViewerWindow::calcDisplayScale().
     */
    fun calcDisplayScale() {
        TODO("Query LLWindow pixel aspect ratio and gSavedSettings UIScaleFactor, update displayScale")
    }

    // ------------------------------------------------------------------
    // Rendering setup
    // ------------------------------------------------------------------

    fun setup2DViewport(xOffset: Int = 0, yOffset: Int = 0) {
        TODO("GPU: glViewport for 2-D UI layer")
    }

    fun setup3DViewport(xOffset: Int = 0, yOffset: Int = 0) {
        TODO("GPU: glViewport for 3-D world layer")
    }

    fun setup3DRender() { TODO("GPU: configure projection / modelview for 3-D render") }
    fun setup2DRender() { TODO("GPU: configure orthographic projection for UI render") }

    // ------------------------------------------------------------------
    // Per-frame update
    // ------------------------------------------------------------------

    /** Main per-frame UI update; mirrors LLViewerWindow::updateUI(). */
    fun updateUI() {
        TODO("Update mouse delta, keyboard focus, tooltips, and layout")
    }

    /** Draw the full frame: world, HUD, UI panels, debug overlays. */
    fun draw() {
        TODO("GPU: orchestrate full-frame draw: 3-D scene, HUD, 2-D UI")
    }

    // ------------------------------------------------------------------
    // Picking
    // ------------------------------------------------------------------

    /**
     * Schedule an async pick at the given screen coords.
     * [callback] receives the [PickInfo] when the GPU readback completes.
     */
    fun pickAsync(
        x: Int,
        yFromBot: Int,
        callback: (PickInfo) -> Unit,
        pickTransparent: Boolean = false,
        pickRigged: Boolean = false,
        pickUnselectable: Boolean = false
    ) {
        TODO("GPU: schedule GPU pick pass and deliver result to callback")
    }

    /**
     * Synchronous scene pick — blocks until the GPU result is available.
     */
    fun pickImmediate(
        x: Int,
        y: Int,
        pickTransparent: Boolean,
        pickRigged: Boolean = false
    ): PickInfo {
        TODO("GPU: render pick pass synchronously and decode result")
    }

    // ------------------------------------------------------------------
    // Mouse helpers
    // ------------------------------------------------------------------

    /** Returns a world-space ray direction for screen coords (x, y). */
    fun mouseDirectionGlobal(x: Int, y: Int): Vector3 {
        TODO("Unproject screen point through inverse projection matrix")
    }

    fun mousePointOnLandGlobal(x: Int, y: Int, ignoreDistance: Boolean = false): Vector3? {
        TODO("Ray-cast against terrain mesh and return hit point, or null if miss")
    }

    // ------------------------------------------------------------------
    // Snapshot helpers (stubs)
    // ------------------------------------------------------------------

    fun saveSnapshot(
        filename: String,
        imageWidth: Int,
        imageHeight: Int,
        showUI: Boolean = true,
        showHUD: Boolean = true
    ): Boolean {
        TODO("GPU: render off-screen framebuffer and encode to file")
    }
}
