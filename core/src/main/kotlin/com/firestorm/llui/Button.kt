package com.firestorm.llui

import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

var LLBUTTON_H_PAD: Int = 4
var BTN_HEIGHT_SMALL: Int = 23
var BTN_HEIGHT: Int = 23
var BTN_DROP_SHADOW: Int = 2

fun roundUp(grid: Int, value: Int): Int {
    val mod = value % grid
    return if (mod > 0) value + (grid - mod) else value
}

enum class HAlign { LEFT, HCENTER, RIGHT }

open class Button(
    val name: String
) {
    var labelUnselected: String = ""
    var labelSelected: String = ""
    var labelShadow: Boolean = true
    var autoResize: Boolean = false
    var useEllipses: Boolean = false
    var useFontColor: Boolean = true

    var imageUnselected: UIImage? = null
    var imageSelected: UIImage? = null
    var imageHoverSelected: UIImage? = null
    var imageHoverUnselected: UIImage? = null
    var imageDisabledSelected: UIImage? = null
    var imageDisabled: UIImage? = null
    var imageFlash: UIImage? = null
    var imagePressed: UIImage? = null
    var imagePressedSelected: UIImage? = null
    var imageOverlay: UIImage? = null
    var imageOverlayAlignment: HAlign = HAlign.HCENTER

    var labelColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var labelColorSelected: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var labelColorDisabled: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
    var labelColorDisabledSelected: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
    var imageColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var imageColorDisabled: FloatArray = floatArrayOf(1f, 1f, 1f, 0.3f)
    var imageOverlayColor: FloatArray = floatArrayOf(1f, 1f, 1f, 0.75f)
    var imageOverlaySelectedColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var imageOverlayDisabledColor: FloatArray = floatArrayOf(1f, 1f, 1f, 0.3f)
    var flashColor: FloatArray = floatArrayOf(1f, 0f, 0f, 1f)
    var flashAltColor: FloatArray = floatArrayOf(1f, 0.5f, 0f, 1f)
    var disabledImageColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var disabledLabelColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
    var disabledSelectedLabelColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f)
    var flashBgColor: FloatArray = floatArrayOf(1f, 0f, 0f, 1f)
    var flashAltBgColor: FloatArray = floatArrayOf(1f, 0.5f, 0f, 1f)

    var padRight: Int = LLBUTTON_H_PAD
    var padLeft: Int = LLBUTTON_H_PAD
    var padBottom: Int = 0
    var imageOverlayTopPad: Int = 0
    var imageOverlayBottomPad: Int = 0
    var imgOverlayLabelSpace: Int = 1

    var isToggle: Boolean = false
    var scaleImage: Boolean = true
    var commitOnReturn: Boolean = true
    var commitOnCaptureLost: Boolean = false
    var displayPressedState: Boolean = true
    var hoverGlowAmount: Float = 0f
    var heldDownDelaySeconds: Float = 0f
    var heldDownFrameDelay: Int = 0
    var useDrawContextAlpha: Boolean = true
    var handleRightMouse: Boolean = false
    var buttonFlashEnable: Boolean = false
    var buttonFlashCount: Int = 0
    var buttonFlashRate: Float = 0f
    var checkboxControl: String = ""

    var enabled: Boolean = true
    var visible: Boolean = true

    var width: Int = 0
    var height: Int = 0
    var x: Int = 0
    var y: Int = 0

    var hAlign: HAlign = HAlign.HCENTER

    var leftHPad: Int = LLBUTTON_H_PAD
    var rightHPad: Int = LLBUTTON_H_PAD
    var bottomVPad: Int = 0

    var forcePressedState: Boolean = false
    var fadeWhenDisabled: Boolean = false

    var needsHighlight: Boolean = false
    var flashing: Boolean = false
    var isAltFlashColor: Boolean = false
    var forceFlashing: Boolean = false
    var curGlowStrength: Float = 0f
    var hoverGlowStrength: Float = 0f

    var mouseDownFrame: Int = 0
    var mouseHeldDownCount: Int = 0
    var lastDrawCharsCount: Int = 0

    var toggleValue: Boolean = false

    val clickedCallbacks: MutableList<(Button) -> Unit> = mutableListOf()
    val mouseDownCallbacks: MutableList<(Button) -> Unit> = mutableListOf()
    val mouseUpCallbacks: MutableList<(Button) -> Unit> = mutableListOf()
    val heldDownCallbacks: MutableList<(Button) -> Unit> = mutableListOf()
    val isToggledCallbacks: MutableList<(Button) -> Boolean> = mutableListOf()

    val disabledSelectedLabel: String get() = labelSelected

    fun addClickedCallback(cb: (Button) -> Unit) { clickedCallbacks.add(cb) }
    fun addMouseDownCallback(cb: (Button) -> Unit) { mouseDownCallbacks.add(cb) }
    fun addMouseUpCallback(cb: (Button) -> Unit) { mouseUpCallbacks.add(cb) }
    fun addHeldDownCallback(cb: (Button) -> Unit) { heldDownCallbacks.add(cb) }
    fun addIsToggledCallback(cb: (Button) -> Boolean) { isToggledCallbacks.add(cb) }

    fun getToggleState(): Boolean = toggleValue

    fun setToggleState(b: Boolean) {
        if (b != toggleValue) {
            toggleValue = b
            if (flashing) setFlashing(false)
            autoResizeIfNeeded()
        }
    }

    fun toggleState(): Boolean {
        val flipped = !getToggleState()
        setToggleState(flipped)
        return flipped
    }

    fun setFlashing(b: Boolean, forceFlash: Boolean = false, alternateColor: Boolean = false) {
        forceFlashing = forceFlash
        flashing = b
        isAltFlashColor = alternateColor
    }

    fun getFlashing(): Boolean = flashing

    fun setLabel(label: String) {
        labelUnselected = label
        labelSelected = label
    }

    fun setLabelUnselected(label: String) { labelUnselected = label }
    fun setLabelSelected(label: String) { labelSelected = label }

    fun setLabelArg(key: String, text: String): Boolean {
        labelUnselected = labelUnselected.replace("[${key}]", text)
        labelSelected = labelSelected.replace("[${key}]", text)
        return true
    }

    fun getLabelUnselected(): String = labelUnselected
    fun getLabelSelected(): String = labelSelected

    fun getCurrentLabel(): String = if (getToggleState()) labelSelected else labelUnselected

    fun labelIsTruncated(): Boolean = getCurrentLabel().length > lastDrawCharsCount

    fun setHighlight(b: Boolean) { needsHighlight = b }

    fun setUnselectedLabelColor(c: FloatArray) { labelColor = c }
    fun setSelectedLabelColor(c: FloatArray) { labelColorSelected = c }
    fun setUseEllipses(b: Boolean) { useEllipses = b }
    fun setUseFontColor(b: Boolean) { useFontColor = b }

    fun setImageColor(c: FloatArray) { imageColor = c }
    fun setColor(c: FloatArray) { setImageColor(c) }
    fun setDisabledImageColor(c: FloatArray) { disabledImageColor = c }
    fun setDisabledSelectedLabelColor(c: FloatArray) { disabledSelectedLabelColor = c }
    fun setDisabledLabelColor(c: FloatArray) { disabledLabelColor = c }
    fun setFlashColor(c: FloatArray) { flashBgColor = c }

    fun setHeldDownDelay(seconds: Float, frames: Int = 0) {
        heldDownDelaySeconds = seconds
        heldDownFrameDelay = frames
    }

    fun setScaleImage(scale: Boolean) { scaleImage = scale }
    fun getScaleImage(): Boolean = scaleImage

    fun setDropShadowedText(b: Boolean) { labelShadow = b }
    fun setHoverGlowStrength(strength: Float) { hoverGlowStrength = strength }
    fun setAutoResize(b: Boolean) { autoResize = b }
    fun setCommitOnReturn(commit: Boolean) { commitOnReturn = commit }
    fun getCommitOnReturn(): Boolean = commitOnReturn
    fun setForcePressedState(b: Boolean) { forcePressedState = b }

    fun setImages(imageName: String, selectedName: String) {
        // no-op
    }

    fun setImageOverlay(imageName: String, alignment: HAlign = HAlign.HCENTER, color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)) {
        if (imageName.isEmpty()) {
            imageOverlay = null
        } else {
            // no-op
        }
        imageOverlayAlignment = alignment
        imageOverlayColor = color
    }

    fun setImageOverlaySelectedColor(color: FloatArray) { imageOverlaySelectedColor = color }
    fun getImageOverlay(): UIImage? = imageOverlay
    fun getImageOverlayHAlign(): HAlign = imageOverlayAlignment

    fun setImageUnselected(image: UIImage?) { imageUnselected = image }
    fun setImageSelected(image: UIImage?) { imageSelected = image }
    fun setImageHoverSelected(image: UIImage?) { imageHoverSelected = image }
    fun setImageHoverUnselected(image: UIImage?) { imageHoverUnselected = image }
    fun setImageDisabled(image: UIImage?) {
        imageDisabled = image
        disabledImageColor = imageColor.copyOf()
        fadeWhenDisabled = true
    }
    fun setImageDisabledSelected(image: UIImage?) {
        imageDisabledSelected = image
        disabledImageColor = imageColor.copyOf()
        fadeWhenDisabled = true
    }
    fun setImageFlash(image: UIImage?) { imageFlash = image }
    fun setImagePressed(image: UIImage?) { imagePressed = image }

    open fun onMouseLeave(x: Int, y: Int, mask: UInt) {
        setHighlight(false)
    }

    open fun onMouseCaptureLost() {
        if (commitOnCaptureLost) {
            mouseUpCallbacks.forEach { it(this) }
            if (isToggle) toggleState()
            onCommit()
        }
    }

    open fun onCommit() {
        mouseDownCallbacks.forEach { it(this) }
        mouseUpCallbacks.forEach { it(this) }
        if (isToggle) toggleState()
        clickedCallbacks.forEach { it(this) }
    }

    open fun handleUnicodeCharHere(uniChar: Char): Boolean {
        if (uniChar == ' ') {
            if (isToggle) toggleState()
            onCommit()
            return true
        }
        return false
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        if (commitOnReturn && key == KEY_RETURN && mask == 0u) {
            if (isToggle) toggleState()
            onCommit()
            return true
        }
        return false
    }

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        mouseDownCallbacks.forEach { it(this) }
        mouseDownFrame = currentFrameCount()
        mouseHeldDownCount = 0
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        mouseUpCallbacks.forEach { it(this) }
        if (pointInView(x, y)) {
            if (isToggle) toggleState()
            onCommit()
        }
        return true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        if (!handleRightMouse) return true
        return true
    }

    open fun handleRightMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        if (!handleRightMouse) return true
        return true
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: UInt): Boolean = handleMouseDown(x, y, mask)

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        if (enabled) setHighlight(true)
        return true
    }

    open fun draw() {
        if (isToggledCallbacks.isNotEmpty()) {
            val result = isToggledCallbacks.any { it(this) }
            setToggleState(result)
        }
        // no-op
    }

    open fun postBuild(): Boolean {
        autoResizeIfNeeded()
        return true
    }

    open fun onVisibilityChange(visible: Boolean) {}
    open fun dirtyRect() {}

    fun autoResizeIfNeeded() {
        if (!autoResize) return
        // no-op
    }

    fun resize(label: String) {
        if (!autoResize) return
        // no-op
    }

    private fun pointInView(px: Int, py: Int): Boolean =
        px >= x && px <= x + width && py >= y && py <= y + height

    private fun currentFrameCount(): Int = 0

    fun getSearchText(): String = getLabelUnselected() + name

    companion object {
        const val KEY_RETURN: Int = 0x0D

        fun toggleFloaterAndSetToggleState(ctrl: Button, name: String) {
            System.err.println("Button: toggleFloaterAndSetToggleState not yet implemented")
        }

        fun setFloaterToggle(ctrl: Button, name: String) {
            System.err.println("Button: setFloaterToggle not yet implemented")
        }

        fun setDockableFloaterToggle(ctrl: Button, name: String) {
            System.err.println("Button: setDockableFloaterToggle not yet implemented")
        }

        fun showHelp(ctrl: Button, name: String) {
            System.err.println("Button: showHelp not yet implemented")
        }
    }
}

class UIImage {
    var width: Int = 0
    var height: Int = 0
    fun getWidth(): Int = width
    fun getHeight(): Int = height
    fun draw(x: Int, y: Int, w: Int, h: Int, color: FloatArray) { /* no-op */ }
    fun draw(x: Int, y: Int, color: FloatArray) { /* no-op */ }
    fun drawSolid(x: Int, y: Int, w: Int, h: Int, color: FloatArray) { /* no-op */ }
    fun drawBorder(x: Int, y: Int, color: FloatArray, size: Int) { /* no-op */ }
}
