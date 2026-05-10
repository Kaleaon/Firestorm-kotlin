/**
 * WorldMapView.kt
 * Converted from llworldmapview.h / llworldmapview.cpp
 *
 * Renders the Second Life world map UI panel. All OpenGL draw calls,
 * texture look-ups, and UI-framework callbacks are stubbed with TODO.
 * The coordinate-transform arithmetic and input-handling logic are preserved.
 */

package com.firestorm.newview

import com.firestorm.llmath.*
import com.firestorm.llcommon.*

// ---------------------------------------------------------------------------
// Constants (from llworldmapview.cpp)
// ---------------------------------------------------------------------------

private const val MAP_DEFAULT_SCALE: Float    = 128f
private const val MAP_ITERP_TIME_CONSTANT: Float = 0.75f
private const val MAP_ZOOM_ACCELERATION_TIME: Float = 0.3f
private const val MAP_ZOOM_MAX_INTERP: Float  = 0.5f
private const val MAP_SCALE_SNAP_THRESHOLD: Float = 0.005f
private const val DEFAULT_TRACKING_ARROW_SIZE: Int = 16

// Ocean colour (#1D475F) — kept as constants to match mapstitcher.py.
private const val OCEAN_RED:   Float = 0x1D / 255f
private const val OCEAN_GREEN: Float = 0x47 / 255f
private const val OCEAN_BLUE:  Float = 0x5F / 255f

// ---------------------------------------------------------------------------
// MapScale zoom levels
// ---------------------------------------------------------------------------

/**
 * Named zoom levels for the world map.
 * Higher ordinal → more zoomed in (larger on-screen pixels per metre).
 */
enum class MapScale {
    VERY_SMALL,
    SMALL,
    MEDIUM,
    LARGE;
}

// ---------------------------------------------------------------------------
// WorldMapView singleton  (LLWorldMapView)
// ---------------------------------------------------------------------------

/**
 * Singleton facade for the world-map panel.
 *
 * Coordinate system:
 *   - Global position: metres from the origin of the SL grid (Vector3 / x,y).
 *   - View position: pixels relative to the top-left corner of this panel.
 *   - Pan: pixel offset of the world origin from the panel centre.
 *
 * All render calls delegate to the real GL pipeline and are stubbed here.
 */
object WorldMapView {

    // -- Pan / scale state ----------------------------------------------------

    /** Current horizontal pan in pixels from panel centre. */
    var panX: Float = 0f
        private set
    /** Current vertical pan in pixels from panel centre. */
    var panY: Float = 0f
        private set

    /** Target pan — interpolated toward over multiple frames. */
    var targetPanX: Float = 0f
        private set
    var targetPanY: Float = 0f
        private set

    /** Current linear scale: screen pixels per in-world metre. */
    var mapScale: Float = MAP_DEFAULT_SCALE
        private set
    /** Scale that the view is interpolating toward. */
    var targetMapScale: Float = MAP_DEFAULT_SCALE
        private set

    /** Ratio used during zoom-pivot interpolation. */
    private var mapRatio: Float = 1f

    /** Interpolation time for map pan/scale transitions (seconds). */
    private var mapInterpTime: Float = MAP_ITERP_TIME_CONSTANT

    // -- Shared static state --------------------------------------------------

    var trackingArrowX: Int = 0
    var trackingArrowY: Int = 0
    var visibleTilesLoaded: Boolean = false
    var handledLastClick: Boolean = false

    /** Persisted scale setting applied on next login. */
    var scaleSetting: Float = MAP_DEFAULT_SCALE

    /** Background fill colour (deep ocean blue by default). */
    var backgroundColor: Color4 = Color4(OCEAN_RED, OCEAN_GREEN, OCEAN_BLUE, 1f)

    // -- Panel geometry -------------------------------------------------------

    var panelWidth: Int = 0
        private set
    var panelHeight: Int = 0
        private set

    // -- Mouse drag state -----------------------------------------------------

    var panning: Boolean = false
        private set
    private var mouseDownPanX: Int = 0
    private var mouseDownPanY: Int = 0
    private var mouseDownX: Int = 0
    private var mouseDownY: Int = 0

    // -- Items ----------------------------------------------------------------

    var itemPicked: Boolean = false

    // -- Visible region handles (set each frame after draw) -------------------

    val visibleRegions: MutableList<ULong> = mutableListOf()

    // -------------------------------------------------------------------------
    // Coordinate transforms
    // -------------------------------------------------------------------------

    /**
     * Convert a global (in-world) position to a view-space pixel coordinate.
     *
     * The x/y of [pos] are in metres from the SL grid origin; z is elevation.
     * Returns a [Vector3] whose x/y are pixel coordinates within this panel
     * (origin = panel top-left). The z component carries the elevation unchanged.
     *
     * Formula mirrors LLWorldMapView::globalPosToView in the C++ source:
     *   panelCentre + (globalPos - agentGlobalPos) * scale + pan
     */
    fun globalPosToView(pos: Vector3): Vector3 {
        val cx = panelWidth  * 0.5f
        val cy = panelHeight * 0.5f
        // agentGlobalPos would come from LLAgent — stubbed as origin here.
        val agentX = 0f; val agentY = 0f
        val px = cx + (pos.x - agentX) * mapScale + panX
        val py = cy + (pos.y - agentY) * mapScale + panY
        return Vector3(px, py, pos.z)
    }

    /**
     * Convert a view-space pixel position to a global (in-world) position.
     *
     * Inverse of [globalPosToView].
     */
    fun viewToGlobalPos(x: Int, y: Int): Vector3 {
        val cx = panelWidth  * 0.5f
        val cy = panelHeight * 0.5f
        val agentX = 0f; val agentY = 0f
        val gx = agentX + (x - cx - panX) / mapScale
        val gy = agentY + (y - cy - panY) / mapScale
        return Vector3(gx, gy, 0f)
    }

    // -------------------------------------------------------------------------
    // Zoom / pan controls
    // -------------------------------------------------------------------------

    /**
     * Smoothly zoom to the given scale level (not a log-zoom; treated as a
     * direct target scale to match the C++ [zoomWithPivot] signature).
     */
    fun zoom(targetScale: Float) {
        targetMapScale = targetScale.coerceIn(MIN_SCALE, MAX_SCALE)
    }

    /**
     * Zoom with a screen-space pivot point so that the world position under
     * the cursor (x, y) stays stationary.
     */
    fun zoomWithPivot(targetScale: Float, x: Int, y: Int) {
        val oldScale = mapScale
        val newScale = targetScale.coerceIn(MIN_SCALE, MAX_SCALE)
        // Adjust pan to keep the world point at (x, y) fixed on screen.
        val cx = panelWidth * 0.5f; val cy = panelHeight * 0.5f
        val dx = x - cx; val dy = y - cy
        val factor = newScale / oldScale - 1f
        targetPanX = panX - dx * factor
        targetPanY = panY - dy * factor
        targetMapScale = newScale
    }

    fun getZoom(): Float = mapScale
    fun getScale(): Float = mapScale

    /** Pan the map by a pixel delta. */
    fun translatePan(deltaX: Int, deltaY: Int) {
        targetPanX = panX + deltaX
        targetPanY = panY + deltaY
    }

    /** Jump the pan to an absolute pixel position (optionally snapping immediately). */
    fun setPan(x: Int, y: Int, snap: Boolean = true) {
        targetPanX = x.toFloat(); targetPanY = y.toFloat()
        if (snap) { panX = targetPanX; panY = targetPanY }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /** Full panel draw — GL rendering is deferred to the native pipeline. */
    fun draw() {
        TODO("GL: world map draw requires OpenGL context and texture pipeline")
    }

    /** Draw generic map-item icons (telehubs, events, etc.). */
    fun drawGenericItems() {
        TODO("GL: generic item rendering not implemented")
    }

    /** Draw the avatar dots for nearby agents. */
    fun drawAgents() {
        TODO("GL: agent rendering requires GL draw and avatar list")
    }

    /** Draw map items (events, classifieds, etc.). */
    fun drawItems() {
        TODO("GL: item rendering requires GL draw and world-map data model")
    }

    /** Draw camera frustum overlay. */
    fun drawFrustum() {
        TODO("GL: frustum overlay requires camera matrix and GL draw calls")
    }

    /**
     * Draw mipmap tiles at the given panel dimensions.
     * Returns true when all visible tiles are loaded.
     */
    fun drawMipmap(width: Int, height: Int): Boolean {
        TODO("GL: mipmap tile rendering requires texture cache and GL draw calls")
    }

    // -------------------------------------------------------------------------
    // Tracking / direction indicators
    // -------------------------------------------------------------------------

    fun drawTracking(
        posGlobal: Vector3,
        color: Color4,
        drawArrow: Boolean = true,
        label: String = "",
        tooltip: String = "",
        vertOffset: Int = 0
    ) {
        TODO("GL: tracking indicator rendering not implemented")
    }

    fun drawTrackingArrow(x: Int, y: Int, color: Color4, arrowSize: Int = DEFAULT_TRACKING_ARROW_SIZE) {
        TODO("GL: tracking arrow rendering not implemented")
    }

    fun drawTrackingDot(xPixels: Float, yPixels: Float, color: Color4, relativeZ: Float = 0f, dotRadius: Float = 5f) {
        TODO("GL: tracking dot rendering not implemented")
    }

    fun drawAvatar(xPixels: Float, yPixels: Float, color: Color4, relativeZ: Float = 0f, dotRadius: Float = 3f, reachedMaxZ: Boolean = false) {
        TODO("GL: avatar icon rendering not implemented")
    }

    fun cleanupTextures() {
        TODO("GL: texture cleanup not implemented")
    }

    // -------------------------------------------------------------------------
    // Input handling
    // -------------------------------------------------------------------------

    /**
     * Primary click handler.  Returns true if the click was consumed.
     * Hit-testing against live map items requires the data model (LLWorldMap)
     * which is not available here; a full hit-test is stubbed.
     */
    fun handleClick(x: Int, y: Int): Boolean {
        handledLastClick = false
        // Placeholder: convert to global pos and check against known regions.
        val globalPos = viewToGlobalPos(x, y)
        println("WorldMapView.handleClick: viewPos=($x,$y) globalPos=$globalPos")
        // Real implementation would hit-test against LLWorldMap::sInstance regions.
        handledLastClick = true
        return true
    }

    fun handleMouseDown(x: Int, y: Int): Boolean {
        mouseDownX = x; mouseDownY = y
        mouseDownPanX = panX.toInt(); mouseDownPanY = panY.toInt()
        panning = true
        return true
    }

    fun handleMouseUp(x: Int, y: Int): Boolean {
        panning = false
        return true
    }

    fun handleDoubleClick(x: Int, y: Int): Boolean {
        val globalPos = viewToGlobalPos(x, y)
        println("WorldMapView.handleDoubleClick: teleport request to $globalPos (stub)")
        return true
    }

    fun handleHover(x: Int, y: Int): Boolean {
        if (panning) {
            val dx = x - mouseDownX; val dy = y - mouseDownY
            panX = mouseDownPanX + dx.toFloat()
            panY = mouseDownPanY + dy.toFloat()
            targetPanX = panX; targetPanY = panY
        }
        return true
    }

    // -------------------------------------------------------------------------
    // Region visibility
    // -------------------------------------------------------------------------

    /** Check whether the current zoom allows reading sim info from the server. */
    fun showRegionInfo(): Boolean = mapScale >= MIN_REGION_INFO_SCALE

    /** Fetch additional sim info for newly visible blocks. Stubbed. */
    fun updateVisibleBlocks() {
        TODO("NET: region info fetch not implemented")
    }

    // -------------------------------------------------------------------------
    // Resize
    // -------------------------------------------------------------------------

    fun reshape(width: Int, height: Int) {
        panelWidth = width; panelHeight = height
    }

    // -------------------------------------------------------------------------
    // Companion / class-level helpers
    // -------------------------------------------------------------------------

    private const val MIN_SCALE: Float = 1f
    private const val MAX_SCALE: Float = 4096f
    private const val MIN_REGION_INFO_SCALE: Float = 128f / 256f  // half a region per pixel

    /** Convert from a UI zoom value to a linear map scale. */
    fun scaleFromZoom(zoom: Float): Float = MAP_DEFAULT_SCALE * kotlin.math.exp(zoom)

    /** Convert from a linear map scale back to a UI zoom value. */
    fun zoomFromScale(scale: Float): Float = kotlin.math.ln(scale / MAP_DEFAULT_SCALE)

    fun clearLastClick() { handledLastClick = false }
}
