package com.firestorm.newview

import kotlin.math.acos
import kotlin.math.sqrt

const val PIE_MAX_SLICES: Int = 8

// Label text anchor positions for each of the 8 slices (indexed 0–7)
private val PIE_X = intArrayOf(64, 45,  0, -45, -63, -45,   0,  45)
private val PIE_Y = intArrayOf( 0, 44, 73,  44,   0, -44, -73, -44)

private const val PIE_INNER_SIZE: Int = 20
private const val PIE_POPUP_FACTOR: Float = 1.7f
private const val PIE_POPUP_TIME: Float = 0.25f
private const val PIE_OUTER_SIZE: Int = 96
private const val PIE_OUTER_SHADE_FACTOR: Float = 1.09f
private const val PIE_SLICE_DIVIDER_WIDTH: Float = 0.04f
private val PIE_MAX_SLICES_F: Float = PIE_MAX_SLICES.toFloat()
private const val F_PI: Float = Math.PI.toFloat()

class PieMenu(params: Params) : LLMenuGL(params), PieAutoHide(params.autohide, params.startAutohide) {

    class Params : LLMenuGL.Params() {
        var startAutohide: Boolean = false
        var autohide: Boolean = false

        init {
            visible = false
        }
    }

    // The slices owned by this menu instance
    val mySlices: MutableList<LLView> = mutableListOf()
    // Points to mySlices normally; redirected into a submenu's list while a submenu is open
    var slices: MutableList<LLView> = mySlices

    private var font: LLFontGL? = null
    // Currently highlighted view (slice or submenu); do not dereference for identity—use for rendering only
    private var slice: LLView? = null
    // Previous highlight used solely to gate the hover sound to one play per entry
    private var oldSlice: LLView? = null

    private val popupTimer = LLFrameTimer()
    private var firstClick: Boolean = true
    private var currentSegment: Int = -1

    init {
        // no-op: GPU reshape(PIE_OUTER_SIZE * 2, PIE_OUTER_SIZE * 2)
        // no-op: GPU load font SansSerif/Pie, fallback to Small
    }

    override fun addChild(child: LLView, tabGroup: Int): Boolean {
        if (slices.size >= PIE_MAX_SLICES) return false
        slices.add(child)
        super.addChild(child, tabGroup)
        // no-op: GPU reshape(PIE_OUTER_SIZE * 2, PIE_OUTER_SIZE * 2)
        return true
    }

    override fun removeChild(child: LLView) {
        slices.remove(child)
        super.removeChild(child)
        // no-op: GPU reshape(PIE_OUTER_SIZE * 2, PIE_OUTER_SIZE * 2)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        val factor = getScaleFactor()
        currentSegment = -1

        val mx = (x - PIE_OUTER_SIZE).toFloat()
        val my = (y - PIE_OUTER_SIZE).toFloat()
        val distance = sqrt(mx * mx + my * my)

        if (distance > PIE_INNER_SIZE && (distance < PIE_OUTER_SIZE * factor || firstClick)) {
            var angle = acos((mx / distance).coerceIn(-1f, 1f))
            if (my < 0f) {
                angle = F_PI * 2f - angle
            }
            angle += F_PI / PIE_MAX_SLICES_F
            currentSegment = ((PIE_MAX_SLICES_F * angle / (F_PI * 2f)).toInt()) % PIE_MAX_SLICES
        }

        return true
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean = handleMouseButtonUp(x, y, mask)

    override fun handleRightMouseUp(x: Int, y: Int, mask: Int): Boolean = handleMouseButtonUp(x, y, mask)

    // Left and right mouse buttons both commit or descend into a submenu
    private fun handleMouseButtonUp(x: Int, y: Int, mask: Int): Boolean {
        if (firstClick && slice == null) {
            firstClick = false
            popupTimer.start()
        } else {
            var keepVisible = false

            val currentSlice = slice
            when (currentSlice) {
                is PieSlice -> {
                    System.err.println("PieMenu: make_ui_sound(UISndClickRelease) not yet implemented")
                    currentSlice.onCommit()
                }
                is PieMenu -> {
                    firstClick = false
                    slices = currentSlice.mySlices
                    slices.filterIsInstance<PieSlice>().forEach { it.resetUpdateEnabledCheck() }
                    keepVisible = true
                    popupTimer.reset()
                    popupTimer.start()
                    System.err.println("PieMenu: make_ui_sound(UISndPieMenuAppear) not yet implemented")
                }
            }
            setVisible(keepVisible)
        }

        if (hasMouseCapture()) {
            System.err.println("PieMenu: gFocusMgr.setMouseCapture(null) not yet implemented")
        }
        return super.handleMouseUp(x, y, mask)
    }

    override fun setVisible(visible: Boolean) {
        if (!visible) {
            hide()
            System.err.println("PieMenu: sMenuContainer.hideMenus() not yet implemented")
        }
    }

    fun show(x: Int, y: Int, spawningView: LLView? = null) {
        if (getVisible()) return

        currentSegment = -1
        System.err.println("PieMenu: make_ui_sound(UISndPieMenuAppear) not yet implemented")
        // no-op: GPU reshape(PIE_OUTER_SIZE * 2, PIE_OUTER_SIZE * 2)

        // Clamp menu position so it stays within the 3-D view rectangle
        var cx = x
        var cy = y
        System.err.println("PieMenu: LLMenuGL.sMenuContainer.getMenuRect() bounds-clamping of cx/cy not yet implemented")

        System.err.println("PieMenu: LLUI.setMousePositionLocal and setOrigin not yet implemented")
        System.err.println("PieMenu: gFocusMgr.setMouseCapture(this) not yet implemented")

        firstClick = true
        slices = mySlices
        slices.filterIsInstance<PieSlice>().forEach { it.resetUpdateEnabledCheck() }
        slice = null
        oldSlice = null

        super.setVisible(true)
    }

    fun hide() {
        if (!getVisible()) return

        System.err.println("PieMenu: make_ui_sound(UISndPieMenuHide) not yet implemented")
        currentSegment = -1
        slices = mySlices
        popupTimer.stop()
        super.setVisible(false)
    }

    override fun draw() {
        // no-op: GPU gGL.pushMatrix()
        val factor = getScaleFactor()

        // Resolve all active colors from the UI color table
        // no-op: GPU fetch PieMenuLineColor, PieMenuSelectedColor, PieMenuTextColor, PieMenuBgColor from LLUIColorTable
        // no-op: GPU apply OverridePieColors / PieMenuOpacity / PieMenuFade user overrides
        // no-op: GPU if firstClick, set borderColor alpha = 0 for borderless appearance

        // no-op: GPU translate origin to rect center
        // no-op: GPU gl_washer_2d(PIE_OUTER_SIZE * factor, PIE_INNER_SIZE, 100, bgColor, borderColor)

        slice = null
        var num = 0
        var wasAutohide = false

        val iter = slices.iterator()

        do {
            val segmentStart = F_PI / (PIE_MAX_SLICES_F / 2f) * num.toFloat() - F_PI / PIE_MAX_SLICES_F
            var label = ""
            var itemColorAlpha = 1f

            if (iter.hasNext()) {
                val item = iter.next()
                var isSliceOrSubmenu = false

                val skipDueToAutohide: Boolean = run {
                    val autohideSlice: PieAutoHide? = item as? PieAutoHide
                    if (autohideSlice == null) return@run false

                    if (autohideSlice.getStartAutohide()) wasAutohide = false

                    if (autohideSlice.getAutohide()) {
                        if (wasAutohide) return@run true

                        // Peek at the next item: if it is enabled/visible and in the same
                        // chain, the current item loses the autohide contest
                        val lookAhead = if (iter.hasNext()) slices[num + 1] else null
                        when (lookAhead) {
                            is PieSlice -> {
                                if (lookAhead.getAutohide() && !lookAhead.getStartAutohide()) {
                                    lookAhead.updateEnabled()
                                    lookAhead.updateVisible()
                                    if (lookAhead.getVisible() && lookAhead.getEnabled()) return@run true
                                    wasAutohide = true
                                }
                            }
                            is PieMenu -> {
                                if (lookAhead.getAutohide() && !lookAhead.getStartAutohide()) {
                                    if (lookAhead.getEnabled()) return@run true
                                    wasAutohide = true
                                }
                            }
                        }
                        false
                    } else {
                        wasAutohide = false
                        false
                    }
                }

                if (skipDueToAutohide) {
                    num++
                    continue
                }

                when (item) {
                    is PieSlice -> {
                        isSliceOrSubmenu = true
                        label = item.label
                        item.updateVisible()
                        val sliceVisible = item.getVisible()
                        item.setEnabled(sliceVisible)
                        if (!sliceVisible) label = ""

                        item.updateEnabled()
                        if (!item.getEnabled()) {
                            // Fade alpha to communicate disabled state visually
                            itemColorAlpha = 0.3f
                        }
                    }
                    is PieMenu -> {
                        isSliceOrSubmenu = true
                        label = item.getLabel()
                        // no-op: GPU gl_washer_segment_2d for submenu outer-ring shade if PieMenuOuterRingShade
                    }
                }

                if (isSliceOrSubmenu && currentSegment == num && item.getEnabled()) {
                    slice = item
                    if (oldSlice != slice) {
                        System.err.println("PieMenu: make_ui_sound(UISndPieMenuSliceHighlight{num}) not yet implemented")
                        oldSlice = slice
                    }
                    // no-op: GPU gl_washer_segment_2d highlight for selected slice
                }
            }

            // no-op: GPU gl_washer_segment_2d divider line at segmentStart
            // no-op: GPU mFont.renderUTF8(label, PIE_X[num]*factor, PIE_Y[num]*factor, itemColor*itemColorAlpha)

            num++
        } while (num < PIE_MAX_SLICES)

        if (!firstClick) {
            // no-op: GPU gl_washer_2d outer border ring
            // no-op: GPU gl_washer_2d outer shade ring if PieMenuOuterRingShade
        }
        // no-op: GPU gl_washer_2d inner circle border
        // no-op: GPU gGL.popMatrix()

        super.draw()
    }

    fun appendContextSubMenu(menu: PieMenu): Boolean {
        if (slices.size >= PIE_MAX_SLICES) return false
        slices.add(menu)
        super.addChild(menu)
        return true
    }

    // Arranging is a no-op: pie layout is purely geometric, not flow-based
    override fun needsArrange() {}
    override fun arrange() {}
    override fun arrangeAndClear() {}

    private fun getScaleFactor(): Float {
        if (firstClick) return PIE_POPUP_FACTOR

        if (popupTimer.isStarted()) {
            val elapsed = popupTimer.elapsedSeconds()
            if (elapsed > PIE_POPUP_TIME) {
                popupTimer.stop()
                return 1f
            }
            return PIE_POPUP_FACTOR - (PIE_POPUP_FACTOR - 1f) * elapsed / PIE_POPUP_TIME
        }

        return 1f
    }
}
