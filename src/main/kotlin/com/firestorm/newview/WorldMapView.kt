package com.firestorm.newview

import kotlin.math.*

const val DEFAULT_TRACKING_ARROW_SIZE = 16

private const val MAP_DEFAULT_SCALE = 128f
private const val MAP_ITERP_TIME_CONSTANT = 0.75f
private const val MAP_ZOOM_ACCELERATION_TIME = 0.3f
private const val MAP_ZOOM_MAX_INTERP = 0.5f
private const val MAP_SCALE_SNAP_THRESHOLD = 0.005f

private const val OCEAN_RED   = 0x1D / 255f
private const val OCEAN_GREEN = 0x47 / 255f
private const val OCEAN_BLUE  = 0x5F / 255f

private const val GODLY_TELEPORT_HEIGHT = 200.0
private const val BIG_DOT_RADIUS = 5f

private const val DRAW_TEXT_THRESHOLD = 96f
private const val DRAW_SIMINFO_THRESHOLD = 3
private const val DRAW_LANDFORSALE_THRESHOLD = 2

data class Color4(val r: Float, val g: Float, val b: Float, val a: Float) {
    companion object {
        val white  = Color4(1f, 1f, 1f, 1f)
        val yellow = Color4(1f, 1f, 0f, 1f)
        val orange = Color4(1f, 0.5f, 0f, 1f)
    }
}

data class Vector3(val x: Float, val y: Float, val z: Float)
data class Vector3d(val x: Double, val y: Double, val z: Double) {
    fun isExactlyZero() = x == 0.0 && y == 0.0 && z == 0.0
}
data class Vector2f(val x: Float, val y: Float) {
    fun isExactlyZero() = x == 0f && y == 0f
}
data class Rect(val left: Int, val bottom: Int, val right: Int, val top: Int) {
    fun getWidth()  = right - left
    fun getHeight() = top - bottom
}

class UIImage(val name: String) {
    fun getWidth()  = 0
    fun getHeight() = 0
    fun draw(x: Int, y: Int, color: Color4 = Color4.white) {
        // no-op
    }
    fun draw(x: Int, y: Int, w: Int, h: Int, color: Color4) {
        // no-op
    }
}

class ItemInfo {
    fun getGlobalPosition(): Vector3d = Vector3d(0.0, 0.0, 0.0)
    fun getRegionHandle(): ULong = 0UL
    fun getUUID(): String = ""
    fun getCount(): Int = 1
}

open class WorldMapView {

    companion object {
        var sAvatarSmallImage: UIImage?     = null
        var sAvatarYouImage: UIImage?       = null
        var sAvatarYouLargeImage: UIImage?  = null
        var sAvatarLevelImage: UIImage?     = null
        var sAvatarAboveImage: UIImage?     = null
        var sAvatarBelowImage: UIImage?     = null
        var sAvatarUnknownImage: UIImage?   = null
        var sTelehubImage: UIImage?         = null
        var sInfohubImage: UIImage?         = null
        var sHomeImage: UIImage?            = null
        var sEventImage: UIImage?           = null
        var sEventMatureImage: UIImage?     = null
        var sEventAdultImage: UIImage?      = null
        var sTrackCircleImage: UIImage?     = null
        var sTrackArrowImage: UIImage?      = null
        var sClassifiedsImage: UIImage?     = null
        var sForSaleImage: UIImage?         = null
        var sForSaleAdultImage: UIImage?    = null

        var sTrackingArrowX = 0
        var sTrackingArrowY = 0
        var sVisibleTilesLoaded = false
        var sHandledLastClick = false

        var sMapScaleSetting = MAP_DEFAULT_SCALE
        var sZoomPivot = Vector2f(0f, 0f)

        val sStringsMap: MutableMap<String, String> = mutableMapOf()

        fun initClass() {
            sAvatarSmallImage    = UIImage("map_avatar_8.tga")
            sAvatarYouImage      = UIImage("map_avatar_16.tga")
            sAvatarYouLargeImage = UIImage("map_avatar_you_32.tga")
            sAvatarLevelImage    = UIImage("map_avatar_32.tga")
            sAvatarAboveImage    = UIImage("map_avatar_above_32.tga")
            sAvatarBelowImage    = UIImage("map_avatar_below_32.tga")
            sAvatarUnknownImage  = UIImage("map_avatar_unknown.tga")
            sHomeImage           = UIImage("map_home.tga")
            sTelehubImage        = UIImage("map_telehub.tga")
            sInfohubImage        = UIImage("map_infohub.tga")
            sEventImage          = UIImage("Parcel_PG_Dark")
            sEventMatureImage    = UIImage("Parcel_M_Dark")
            sEventAdultImage     = UIImage("Parcel_R_Dark")
            sTrackCircleImage    = UIImage("map_track_16.tga")
            sTrackArrowImage     = UIImage("direction_arrow.tga")
            sClassifiedsImage    = UIImage("icon_top_pick.tga")
            sForSaleImage        = UIImage("icon_for_sale.tga")
            sForSaleAdultImage   = UIImage("icon_for_sale_adult.tga")
            sStringsMap["loading"]        = "texture_loading"
            sStringsMap["offline"]        = "worldmap_offline"
            sStringsMap["agent_position"] = "worldmap_agent_position"
        }

        fun cleanupClass() {
            sAvatarSmallImage = null; sAvatarYouImage = null; sAvatarYouLargeImage = null
            sAvatarLevelImage = null; sAvatarAboveImage = null; sAvatarBelowImage = null
            sAvatarUnknownImage = null; sTelehubImage = null; sInfohubImage = null
            sHomeImage = null; sEventImage = null; sEventMatureImage = null; sEventAdultImage = null
            sTrackCircleImage = null; sTrackArrowImage = null
            sClassifiedsImage = null; sForSaleImage = null; sForSaleAdultImage = null
        }

        fun cleanupTextures() {}

        fun clearLastClick() { sHandledLastClick = false }

        fun setScaleSetting(scaleSetting: Float) { sMapScaleSetting = scaleSetting }
        fun getScaleSetting(): Float = sMapScaleSetting

        fun drawTrackingArrow(
            viewRect: Rect, x: Int, y: Int, color: Color4,
            arrowSize: Int = DEFAULT_TRACKING_ARROW_SIZE
        ) {
            val xCenter = viewRect.getWidth() / 2f
            val yCenter = viewRect.getHeight() / 2f
            var xClamped = x.coerceIn(0, viewRect.getWidth() - arrowSize).toFloat()
            var yClamped = y.coerceIn(0, viewRect.getHeight() - arrowSize).toFloat()

            val denom = (x - xCenter).let { if (it == 0f) 1e-6f else it }
            val slope = (y - yCenter) / denom
            val windowRatio = viewRect.getHeight().toFloat() / viewRect.getWidth()

            if (abs(slope) > windowRatio && yClamped != y.toFloat()) {
                xClamped = ((yClamped - yCenter) / slope + xCenter)
                    .coerceIn(0f, (viewRect.getWidth() - arrowSize).toFloat())
            } else if (xClamped != x.toFloat()) {
                yClamped = ((xClamped - xCenter) * slope + yCenter)
                    .coerceIn(0f, (viewRect.getHeight() - arrowSize).toFloat())
            }

            val halfArrow = (0.5f * arrowSize).toInt()
            val angle = atan2(y + halfArrow - yCenter, x + halfArrow - xCenter)

            sTrackingArrowX = xClamped.toInt()
            sTrackingArrowY = yClamped.toInt()
            // no-op
        }

        fun drawTrackingDot(
            xPixels: Float, yPixels: Float, color: Color4,
            relativeZ: Float = 0f, dotRadius: Float = 5f
        ) {
            val img = sTrackCircleImage ?: return
            drawDot(xPixels, yPixels, color, relativeZ, dotRadius, img)
        }

        fun drawTrackingCircle(
            rect: Rect, x: Int, y: Int, color: Color4,
            minThickness: Int, overlap: Int
        ) {
            val piHalf = (PI / 2).toFloat()
            val twoPi  = (2 * PI).toFloat()
            var startTheta = 0f
            var endTheta   = twoPi
            var xDelta = 0f
            var yDelta = 0f

            if (x < 0) {
                xDelta = -x.toFloat()
                startTheta = PI.toFloat() + piHalf; endTheta = twoPi + piHalf
            } else if (x > rect.getWidth()) {
                xDelta = (x - rect.getWidth()).toFloat()
                startTheta = piHalf; endTheta = PI.toFloat() + piHalf
            }
            if (y < 0) {
                yDelta = -y.toFloat()
                when {
                    x < 0               -> { startTheta = 0f;             endTheta = piHalf }
                    x > rect.getWidth() -> { startTheta = piHalf;         endTheta = PI.toFloat() }
                    else                -> { startTheta = 0f;             endTheta = PI.toFloat() }
                }
            } else if (y > rect.getHeight()) {
                yDelta = (y - rect.getHeight()).toFloat()
                when {
                    x < 0               -> { startTheta = PI.toFloat() + piHalf; endTheta = twoPi }
                    x > rect.getWidth() -> { startTheta = PI.toFloat();           endTheta = PI.toFloat() + piHalf }
                    else                -> { startTheta = PI.toFloat();           endTheta = twoPi }
                }
            }

            val distance = max(0.1f, sqrt(xDelta * xDelta + yDelta * yDelta))
            val outerRadius = distance + (1f + 9f * sqrt(xDelta * yDelta) / distance) * overlap
            val innerRadius = outerRadius - minThickness

            val angleX = asin((xDelta / outerRadius).coerceIn(-1f, 1f))
            val angleY = asin((yDelta / outerRadius).coerceIn(-1f, 1f))
            val adj = when {
                angleX != 0f && angleY != 0f -> min(angleX, angleY)
                angleX != 0f -> angleX
                else         -> angleY
            }
            startTheta += adj; endTheta -= adj
            // no-op
        }

        fun drawAvatar(
            xPixels: Float, yPixels: Float, color: Color4,
            relativeZ: Float = 0f, dotRadius: Float = 3f, reachedMaxZ: Boolean = false
        ) {
            val threshold = 7f
            val img = when {
                reachedMaxZ && abs(relativeZ) > threshold -> sAvatarUnknownImage
                relativeZ < -threshold  -> sAvatarBelowImage
                relativeZ > threshold   -> sAvatarAboveImage
                else                    -> sAvatarLevelImage
            } ?: return
            val w = (dotRadius * 2f).toInt()
            img.draw((xPixels - dotRadius).toInt(), (yPixels - dotRadius).toInt(), w, w, color)
        }

        fun drawIconName(
            xPixels: Float, yPixels: Float, color: Color4,
            firstLine: String, secondLine: String
        ) {
            val vertPad = 8
            val textX = xPixels.toInt()
            val textY = (yPixels - BIG_DOT_RADIUS - vertPad).toInt()
            // no-op
        }

        private fun drawDot(
            xPixels: Float, yPixels: Float, color: Color4,
            relativeZ: Float, dotRadius: Float, dotImage: UIImage
        ) {
            val threshold = 7f
            if (relativeZ in -threshold..threshold) {
                dotImage.draw(
                    xPixels.toInt() - dotImage.getWidth() / 2,
                    yPixels.toInt() - dotImage.getHeight() / 2,
                    color
                )
            } else {
                // no-op
            }
        }

        fun scaleFromZoom(zoom: Float): Float = 2f.pow(zoom) * 256f
        fun zoomFromScale(scale: Float): Float = log2(scale / 256f)
    }

    var backgroundColor = Color4(OCEAN_RED, OCEAN_GREEN, OCEAN_BLUE, 1f)
    var itemPicked = false
    var panX = 0f
    var panY = 0f
    var targetPanX = 0f
    var targetPanY = 0f
    var panning = false
    var mouseDownPanX = 0
    var mouseDownPanY = 0
    var mouseDownX = 0
    var mouseDownY = 0
    var selectIDStart = 0

    val visibleRegions: MutableList<ULong> = mutableListOf()

    private var mapScale      = 0f
    private var targetMapScale = 0f
    private var mapRatio      = 0.5f
    private var mapIterpTime  = MAP_ITERP_TIME_CONSTANT

    private var zoomTimerStarted = false
    private var zoomTimerElapsed = 0f

    private var rectWidth  = 0
    private var rectHeight = 0

    fun postBuild(): Boolean {
        setScale(sMapScaleSetting, snap = true)
        return true
    }

    open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        rectWidth  = width
        rectHeight = height
    }

    open fun setVisible(visible: Boolean) {
        if (!visible) {
            System.err.println("WorldMapView: setVisible not yet implemented")
        }
    }

    fun zoom(zoom: Float) {
        targetMapScale = scaleFromZoom(zoom)
        if (!zoomTimerStarted && mapScale != targetMapScale) {
            sZoomPivot = Vector2f(0f, 0f)
            zoomTimerStarted = true
            zoomTimerElapsed = 0f
        }
    }

    fun zoomWithPivot(zoom: Float, x: Int, y: Int) {
        targetMapScale = scaleFromZoom(zoom)
        sZoomPivot = Vector2f(x.toFloat(), y.toFloat())
        if (!zoomTimerStarted && mapScale != targetMapScale) {
            zoomTimerStarted = true
            zoomTimerElapsed = 0f
        }
    }

    fun getZoom(): Float = zoomFromScale(mapScale)
    fun getScale(): Float = mapScale

    fun translatePan(deltaX: Int, deltaY: Int) {
        panX += deltaX; panY += deltaY
        targetPanX = panX; targetPanY = panY
        sVisibleTilesLoaded = false
    }

    fun setPan(x: Int, y: Int, snap: Boolean = true) {
        mapIterpTime = MAP_ITERP_TIME_CONSTANT
        targetPanX = x.toFloat(); targetPanY = y.toFloat()
        if (snap) { panX = targetPanX; panY = targetPanY }
        sVisibleTilesLoaded = false
    }

    fun setPanWithInterpTime(x: Int, y: Int, snap: Boolean, interpTime: Float) {
        setPan(x, y, snap)
        mapIterpTime = interpTime
    }

    fun showRegionInfo(): Boolean {
        return false
    }

    fun globalPosToView(globalPos: Vector3d): Vector3 {
        return Vector3(0f, 0f, 0f)
    }

    fun viewPosToGlobal(x: Int, y: Int): Vector3d {
        return Vector3d(0.0, 0.0, 0.0)
    }

    open fun draw() {
        // no-op
    }

    fun drawGenericItems(items: List<ItemInfo>, image: UIImage) {
        items.forEach { drawGenericItem(it, image) }
    }

    fun drawGenericItem(item: ItemInfo, image: UIImage) {
        drawImage(item.getGlobalPosition(), image)
    }

    fun drawImage(globalPos: Vector3d, image: UIImage, color: Color4 = Color4.white) {
        // no-op
    }

    fun drawImageStack(
        globalPos: Vector3d, image: UIImage, count: UInt, offset: Float, color: Color4
    ) {
        // no-op
    }

    fun drawAgents() {
        // no-op
    }

    fun drawItems() {
        // no-op
    }

    fun drawFrustum() {
        // no-op
    }

    fun drawMipmap(width: Int, height: Int) {
        // no-op
    }

    fun drawMipmapLevel(width: Int, height: Int, level: Int, load: Boolean = true): Boolean {
        return false
    }

    fun drawTracking(
        posGlobal: Vector3d, color: Color4, drawArrow: Boolean = true,
        label: String = "", tooltip: String = "", vertOffset: Int = 0
    ) {
        // no-op
    }

    fun checkItemHit(x: Int, y: Int, item: ItemInfo, outId: Array<String>, track: Boolean): Boolean {
        return false
    }

    fun handleClick(x: Int, y: Int, mask: Int, outHitType: IntArray, outId: Array<String>) {
        System.err.println("WorldMapView: handleClick not yet implemented")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        mouseDownPanX = panX.toInt(); mouseDownPanY = panY.toInt()
        mouseDownX = x; mouseDownY = y
        sHandledLastClick = true
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        return false
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        if (!sHandledLastClick) return false
        return false
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        return false
    }

    open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        return false
    }

    fun updateVisibleBlocks() {
        System.err.println("WorldMapView: updateVisibleBlocks not yet implemented")
    }

    private fun setScale(scale: Float, snap: Boolean = true) {
        if (scale != mapScale) {
            val old = mapScale.let { if (it == 0f) 1f else it }
            mapScale = max(0.1f, scale)
            sMapScaleSetting = mapScale
            mapRatio = mapScale / 256f
            mapIterpTime = MAP_ITERP_TIME_CONSTANT
            val ratio = scale / old
            panX *= ratio; panY *= ratio
            targetPanX = panX; targetPanY = panY
            sVisibleTilesLoaded = false
            if (!sZoomPivot.isExactlyZero()) {
                val relX = sZoomPivot.x - rectWidth / 2f
                val relY = sZoomPivot.y - rectHeight / 2f
                val offX = relX - relX * scale / old
                val offY = relY - relY * scale / old
                panX += offX; panY += offY
                targetPanX += offX; targetPanY += offY
            }
        }
        if (snap) targetMapScale = scale
    }

    private fun updateDirections() {
        // no-op
    }

    private fun drawTileOutline(level: Int, top: Float, left: Float, bottom: Float, right: Float) {
        // no-op
    }
}
