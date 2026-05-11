package com.firestorm.newview

import java.util.UUID

class FloaterColorPicker(
    private var swatch: ColorSwatchCtrl?,
    private val showApplyImmediate: Boolean = false
) : Floater() {

    private val components: Int = 3

    private var mouseDownInLumRegion: Boolean = false
    private var mouseDownInHueRegion: Boolean = false
    private var mouseDownInSwatch: Boolean = false

    private val rgbViewerImageLeft: Int = 140
    private val rgbViewerImageTop: Int = 356
    private val rgbViewerImageWidth: Int = 256
    private val rgbViewerImageHeight: Int = 256

    private val lumRegionLeft: Int = rgbViewerImageLeft + rgbViewerImageWidth + 16
    private val lumRegionTop: Int = rgbViewerImageTop
    private val lumRegionWidth: Int = 16
    private val lumRegionHeight: Int = rgbViewerImageHeight
    private val lumMarkerSize: Int = 6

    private val swatchRegionLeft: Int = 12
    private val swatchRegionTop: Int = 190
    private val swatchRegionWidth: Int = 116
    private val swatchRegionHeight: Int = 60

    private var swatchView: View? = null

    private val numPaletteColumns: Int = 16
    private val numPaletteRows: Int = 2
    private val palette: MutableList<Color4?> = mutableListOf()
    private var highlightEntry: Int = -1
    private val paletteRegionLeft: Int = 11
    private val paletteRegionTop: Int = 100 - 8
    private val paletteRegionWidth: Int = lumRegionLeft + lumRegionWidth - 10
    private val paletteRegionHeight: Int = 40

    private var rgbImage: ViewerTexture? = null

    private var active: Boolean = true
    private var applyImmediateCheck: CheckBoxCtrl? = null
    private var canApplyImmediately: Boolean = showApplyImmediate

    private var selectBtn: Button? = null
    private var cancelBtn: Button? = null
    private var pipetteBtn: Button? = null

    private var contextConeOpacity: Float = 0f
    private val contextConeInAlpha: Float = CONTEXT_CONE_IN_ALPHA
    private val contextConeOutAlpha: Float = CONTEXT_CONE_OUT_ALPHA
    private val contextConeFadeTime: Float = CONTEXT_CONE_FADE_TIME

    private var copyLslBtn: Button? = null
    private var hexValue: LineEditor? = null

    // original RGB
    private var origR: Float = 0f
    private var origG: Float = 0f
    private var origB: Float = 0f

    // current RGB/HSL
    private var curR: Float = 0f
    private var curG: Float = 0f
    private var curB: Float = 0f
    private var curH: Float = 0f
    private var curS: Float = 0f
    private var curL: Float = 0f

    init {
        buildFromFile("floater_color_picker.xml")
        createUI()
    }

    fun getSwatch(): ColorSwatchCtrl? = swatch
    fun setSwatch(s: ColorSwatchCtrl?) { swatch = s }

    fun getOrigR(): Float = origR
    fun getOrigG(): Float = origG
    fun getOrigB(): Float = origB

    fun getCurR(): Float = curR
    fun getCurG(): Float = curG
    fun getCurB(): Float = curB
    fun getCurH(): Float = curH
    fun getCurS(): Float = curS
    fun getCurL(): Float = curL

    fun getMouseDownInHueRegion(): Boolean = mouseDownInHueRegion
    fun getMouseDownInLumRegion(): Boolean = mouseDownInLumRegion
    fun getMouseDownInSwatch(): Boolean = mouseDownInSwatch

    fun createUI() {
        TODO("GPU: allocate LLImageRaw, fill HSL->RGB pixels, upload as local texture, bind, set clamp mode")
        for (each in 0 until numPaletteColumns * numPaletteRows) {
            palette.add(Color4.fromColorTable("ColorPaletteEntry${String.format("%02d", each + 1)}"))
        }
    }

    fun showUI() {
        openFloater(getKey())
        setVisible(true)
        setFocus(true)

        if (savedSettings.getBool("UseDefaultColorPicker")) {
            val sw = getSwatch()
            setVisible(false)
            if (sw != null) {
                TODO("APR: use JVM equivalent - invoke system color picker dialog, commit or cancel swatch accordingly")
            }
            closeFloater()
        }
    }

    override fun postBuild(): Boolean {
        cancelBtn = getChild<Button>("cancel_btn")
        cancelBtn?.setClickedCallback { onClickCancel() }

        selectBtn = getChild<Button>("select_btn")
        selectBtn?.setClickedCallback { onClickSelect() }
        selectBtn?.setFocus(true)

        pipetteBtn = getChild<Button>("color_pipette")
        pipetteBtn?.setImages("eye_button_inactive.tga", "eye_button_active.tga")
        pipetteBtn?.setCommitCallback { onClickPipette() }

        applyImmediateCheck = getChild<CheckBoxCtrl>("apply_immediate")
        applyImmediateCheck?.set(savedSettings.getBool("ApplyColorImmediately"))
        applyImmediateCheck?.setCommitCallback { onImmediateCheck() }

        if (!canApplyImmediately) {
            applyImmediateCheck?.setEnabled(false)
            applyImmediateCheck?.set(false)
        }

        for (spinName in listOf("rspin", "gspin", "bspin", "hspin", "sspin", "lspin")) {
            childSetCommitCallback(spinName) { ctrl -> onTextEntryChanged(ctrl) }
        }

        copyLslBtn = getChild<Button>("copy_lsl_btn")
        copyLslBtn?.setClickedCallback { onClickCopyLsl() }

        for (spinName in listOf("rspin_lsl", "gspin_lsl", "bspin_lsl", "hex_value")) {
            childSetCommitCallback(spinName) { ctrl -> onTextEntryChanged(ctrl) }
        }

        ToolPipette.instance.setToolSelectCallback { te -> onColorSelect(te) }

        return true
    }

    fun initUI(rValIn: Float, gValIn: Float, bValIn: Float) {
        val r = rValIn.coerceIn(0f, 1f)
        val g = gValIn.coerceIn(0f, 1f)
        val b = bValIn.coerceIn(0f, 1f)
        setOrigRgb(r, g, b)
        setCurRgb(r, g, b)
        updateTextEntry()
    }

    fun destroyUI() {
        stopUsingPipette()
        palette.clear()
        swatchView?.let {
            removeChild(it)
            it.die()
            swatchView = null
        }
    }

    fun cancelSelection() {
        setCurRgb(getOrigR(), getOrigG(), getOrigB())
        ColorSwatchCtrl.onColorChanged(getSwatch(), ColorSwatchCtrl.COLOR_CANCEL)
        setVisible(false)
    }

    fun setOrigRgb(r: Float, g: Float, b: Float) {
        origR = r; origG = g; origB = b
    }

    fun getOrigRgb(): Triple<Float, Float, Float> = Triple(origR, origG, origB)

    fun setCurRgb(rIn: Float, gIn: Float, bIn: Float) {
        curR = rIn; curG = gIn; curB = bIn
        val hsl = Color3(rIn, gIn, bIn).calcHSL()
        curH = hsl.first; curS = hsl.second; curL = hsl.third
        updateTextEntry()
    }

    fun getCurRgb(): Triple<Float, Float, Float> = Triple(curR, curG, curB)

    fun setCurHsl(hIn: Float, sIn: Float, lIn: Float) {
        curH = hIn; curS = sIn; curL = lIn
        val (r, g, b) = hslToRgb(hIn, sIn, lIn)
        curR = r; curG = g; curB = b
    }

    fun getCurHsl(): Triple<Float, Float, Float> = Triple(curH, curS, curL)

    fun isColorChanged(): Boolean =
        getOrigR() != getCurR() || getOrigG() != getCurG() || getOrigB() != getCurB()

    fun hueToRgb(val1: Float, val2: Float, valHue: Float): Float {
        var h = valHue
        if (h < 0f) h += 1f
        if (h > 1f) h -= 1f
        if (6f * h < 1f) return val1 + (val2 - val1) * 6f * h
        if (2f * h < 1f) return val2
        if (3f * h < 2f) return val1 + (val2 - val1) * (2f / 3f - h) * 6f
        return val1
    }

    fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
        if (s < 0.00001f) return Triple(l, l, l)
        val interVal2 = if (l < 0.5f) l * (1f + s) else (l + s) - (s * l)
        val interVal1 = 2f * l - interVal2
        return Triple(
            hueToRgb(interVal1, interVal2, h + 1f / 3f),
            hueToRgb(interVal1, interVal2, h),
            hueToRgb(interVal1, interVal2, h - 1f / 3f)
        )
    }

    fun updateRgbHslFromPoint(xPosIn: Int, yPosIn: Int): Boolean {
        if (xPosIn >= rgbViewerImageLeft &&
            xPosIn <= rgbViewerImageLeft + rgbViewerImageWidth &&
            yPosIn <= rgbViewerImageTop &&
            yPosIn >= rgbViewerImageTop - rgbViewerImageHeight
        ) {
            selectCurHsl(
                (xPosIn - rgbViewerImageLeft).toFloat() / rgbViewerImageWidth.toFloat(),
                (yPosIn - (rgbViewerImageTop - rgbViewerImageHeight)).toFloat() / rgbViewerImageHeight.toFloat(),
                getCurL()
            )
            return true
        }
        if (xPosIn >= lumRegionLeft &&
            xPosIn <= lumRegionLeft + lumRegionWidth &&
            yPosIn <= lumRegionTop &&
            yPosIn >= lumRegionTop - lumRegionHeight
        ) {
            selectCurHsl(
                getCurH(),
                getCurS(),
                (yPosIn - (rgbViewerImageTop - rgbViewerImageHeight)).toFloat() / rgbViewerImageHeight.toFloat()
            )
            return true
        }
        return false
    }

    fun updateTextEntry() {
        getChild<UICtrl>("rspin")?.setValue(getCurR() * 255f)
        getChild<UICtrl>("gspin")?.setValue(getCurG() * 255f)
        getChild<UICtrl>("bspin")?.setValue(getCurB() * 255f)
        getChild<UICtrl>("hspin")?.setValue(getCurH() * 360f)
        getChild<UICtrl>("sspin")?.setValue(getCurS() * 100f)
        getChild<UICtrl>("lspin")?.setValue(getCurL() * 100f)

        getChild<UICtrl>("rspin_lsl")?.setValue(getCurR())
        getChild<UICtrl>("gspin_lsl")?.setValue(getCurG())
        getChild<UICtrl>("bspin_lsl")?.setValue(getCurB())
        getChild<UICtrl>("hex_value")?.setValue(
            "%02x%02x%02x".format(
                (getCurR() * 255f).toInt(),
                (getCurG() * 255f).toInt(),
                (getCurB() * 255f).toInt()
            )
        )
    }

    fun stopUsingPipette() {
        if (ToolMgr.instance.getCurrentTool() == ToolPipette.instance) {
            ToolMgr.instance.clearTransientTool()
        }
    }

    fun setActive(active: Boolean) {
        if (!active && pipetteBtn?.getToggleState() == true) {
            stopUsingPipette()
        }
        this.active = active
    }

    fun setMouseDownInHueRegion(downInRegion: Boolean) {
        mouseDownInHueRegion = downInRegion
        if (downInRegion && focusMgr.childHasKeyboardFocus(this)) {
            selectBtn?.setFocus(true)
        }
    }

    fun setMouseDownInLumRegion(downInRegion: Boolean) {
        mouseDownInLumRegion = downInRegion
        if (downInRegion && focusMgr.childHasKeyboardFocus(this)) {
            selectBtn?.setFocus(true)
        }
    }

    fun setMouseDownInSwatch(downInSwatch: Boolean) {
        mouseDownInSwatch = downInSwatch
        if (downInSwatch && focusMgr.childHasKeyboardFocus(this)) {
            selectBtn?.setFocus(true)
        }
    }

    override fun draw() {
        val maxOpacity = savedSettings.getFloat("PickerContextOpacity", 0.4f)
        TODO("GPU: drawConeToOwner(contextConeOpacity, maxOpacity, swatch, contextConeFadeTime, contextConeInAlpha, contextConeOutAlpha)")

        pipetteBtn?.setToggleState(ToolMgr.instance.getCurrentTool() == ToolPipette.instance)
        applyImmediateCheck?.setEnabled(active && canApplyImmediately)
        selectBtn?.setEnabled(active)

        super.draw()

        val alpha = getSwatchTransparency()

        TODO("GPU: gl_draw_image rgbViewerImage, draw hue/sat crosshair lines, lum slider, lum marker triangle, swatch rect, outlines")
        drawPalette()
    }

    open fun getSwatchTransparency(): Float =
        if (getTransparencyType() == TransparencyType.ACTIVE) 1f else Floater.getCurrentTransparency()

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        floaterView.bringToFront(this)

        val rgbRect = Rect(rgbViewerImageLeft, rgbViewerImageTop,
            rgbViewerImageLeft + rgbViewerImageWidth, rgbViewerImageTop - rgbViewerImageHeight)

        if (rgbRect.pointInRect(x, y)) {
            focusMgr.setMouseCapture(this)
            setMouseDownInHueRegion(true)
            updateRgbHslFromPoint(x, y)
            return true
        }

        val lumRect = Rect(lumRegionLeft, lumRegionTop,
            lumRegionLeft + lumRegionWidth + lumMarkerSize, lumRegionTop - lumRegionHeight)

        if (lumRect.pointInRect(x, y)) {
            focusMgr.setMouseCapture(this)
            setMouseDownInLumRegion(true)
            return true
        }

        val swatchRect = Rect(swatchRegionLeft, swatchRegionTop,
            swatchRegionLeft + swatchRegionWidth, swatchRegionTop - swatchRegionHeight)

        setMouseDownInSwatch(false)
        if (swatchRect.pointInRect(x, y)) {
            setMouseDownInSwatch(true)
            return true
        }

        val paletteRect = Rect(paletteRegionLeft, paletteRegionTop,
            paletteRegionLeft + paletteRegionWidth, paletteRegionTop - paletteRegionHeight)

        if (paletteRect.pointInRect(x, y)) {
            if (focusMgr.childHasKeyboardFocus(this)) {
                selectBtn?.setFocus(true)
            }
            val c = ((x - paletteRegionLeft) * numPaletteColumns) / paletteRegionWidth
            val r = ((y - (paletteRegionTop - paletteRegionHeight)) * numPaletteRows) / paletteRegionHeight
            val index = (numPaletteRows - r - 1) * numPaletteColumns + c
            if (index <= palette.size) {
                val selected = palette[index]
                if (selected != null) {
                    selectCurRgb(selected.r, selected.g, selected.b)
                    if (applyImmediateCheck?.get() == true) {
                        ColorSwatchCtrl.onColorChanged(getSwatch(), ColorSwatchCtrl.COLOR_CHANGE)
                    }
                    updateTextEntry()
                }
            }
            return true
        }

        return super.handleMouseDown(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        if (isFrontmost()) {
            if (getMouseDownInHueRegion() || getMouseDownInLumRegion()) {
                val clampedX: Int
                val clampedY: Int
                if (getMouseDownInHueRegion()) {
                    clampedX = x.coerceIn(rgbViewerImageLeft, rgbViewerImageLeft + rgbViewerImageWidth)
                    clampedY = y.coerceIn(rgbViewerImageTop - rgbViewerImageHeight, rgbViewerImageTop)
                } else {
                    clampedX = x.coerceIn(lumRegionLeft, lumRegionLeft + lumRegionWidth)
                    clampedY = y.coerceIn(lumRegionTop - lumRegionHeight, lumRegionTop)
                }
                if (updateRgbHslFromPoint(clampedX, clampedY)) {
                    updateTextEntry()
                }
            }

            highlightEntry = -1

            if (mouseDownInSwatch) {
                getWindow().setCursor(UICursor.ARROWDRAG)

                val paletteRect = Rect(paletteRegionLeft, paletteRegionTop,
                    paletteRegionLeft + paletteRegionWidth, paletteRegionTop - paletteRegionHeight)

                if (paletteRect.pointInRect(x, y)) {
                    val xOffset = ((x - paletteRegionLeft) * numPaletteColumns) / paletteRegionWidth
                    val yOffset = ((paletteRegionTop - y - 1) * numPaletteRows) / paletteRegionHeight
                    highlightEntry = xOffset + yOffset * numPaletteColumns
                }
                return true
            }
        }
        return super.handleHover(x, y, mask)
    }

    override fun handleMouseUp(x: Int, y: Int, mask: Int): Boolean {
        getWindow().setCursor(UICursor.ARROW)

        if (getMouseDownInHueRegion() || getMouseDownInLumRegion()) {
            if (applyImmediateCheck?.get() == true) {
                ColorSwatchCtrl.onColorChanged(getSwatch(), ColorSwatchCtrl.COLOR_CHANGE)
            }
        }

        val paletteRect = Rect(paletteRegionLeft, paletteRegionTop,
            paletteRegionLeft + paletteRegionWidth, paletteRegionTop - paletteRegionHeight)

        if (paletteRect.pointInRect(x, y) && mouseDownInSwatch) {
            var curEntry = 0
            for (row in 0 until numPaletteRows) {
                for (column in 0 until numPaletteColumns) {
                    val left = paletteRegionLeft + (paletteRegionWidth * column) / numPaletteColumns
                    val top = paletteRegionTop - (paletteRegionHeight * row) / numPaletteRows
                    val right = paletteRegionLeft + (paletteRegionWidth * (column + 1)) / numPaletteColumns
                    val bottom = paletteRegionTop - (paletteRegionHeight * (row + 1)) / numPaletteRows
                    val dropRect = Rect(left, top, right, bottom)
                    if (dropRect.pointInRect(x, y) && palette[curEntry] != null) {
                        palette[curEntry] = Color4(getCurR(), getCurG(), getCurB(), 1f)
                        val key = "ColorPaletteEntry${String.format("%02d", curEntry + 1)}"
                        UIColorTable.instance.setColor(key, palette[curEntry]!!)
                    }
                    curEntry++
                }
            }
        }

        setMouseDownInHueRegion(false)
        setMouseDownInLumRegion(false)
        mouseDownInSwatch = false

        if (hasMouseCapture()) {
            focusMgr.setMouseCapture(null)
        }

        return super.handleMouseUp(x, y, mask)
    }

    override fun onMouseCaptureLost() {
        setMouseDownInHueRegion(false)
        setMouseDownInLumRegion(false)
    }

    fun onTextEntryChanged(ctrl: UICtrl) {
        val name = ctrl.getName()
        when {
            name == "rspin" || name == "gspin" || name == "bspin" -> {
                val (rVal, gVal, bVal) = getCurRgb()
                val newR = if (name == "rspin") ctrl.getValue().toFloat() / 255f else rVal
                val newG = if (name == "gspin") ctrl.getValue().toFloat() / 255f else gVal
                val newB = if (name == "bspin") ctrl.getValue().toFloat() / 255f else bVal
                selectCurRgb(newR, newG, newB)
                updateTextEntry()
            }
            name == "rspin_lsl" || name == "gspin_lsl" || name == "bspin_lsl" -> {
                val (rVal, gVal, bVal) = getCurRgb()
                val newR = if (name == "rspin_lsl") ctrl.getValue().toFloat() else rVal
                val newG = if (name == "gspin_lsl") ctrl.getValue().toFloat() else gVal
                val newB = if (name == "bspin_lsl") ctrl.getValue().toFloat() else bVal
                selectCurRgb(newR, newG, newB)
                updateTextEntry()
            }
            name == "hex_value" -> {
                val hexString = ctrl.getValue().toString()
                val hexPattern = Regex("^[0-9a-fA-F]{6}$")
                if (!hexPattern.matches(hexString)) return
                val r = hexString.substring(0, 2).toInt(16) / 255f
                val g = hexString.substring(2, 4).toInt(16) / 255f
                val b = hexString.substring(4, 6).toInt(16) / 255f
                selectCurRgb(r, g, b)
                updateTextEntry()
            }
            name == "hspin" || name == "sspin" || name == "lspin" -> {
                val (hVal, sVal, lVal) = getCurHsl()
                val newH = if (name == "hspin") ctrl.getValue().toFloat() / 360f else hVal
                val newS = if (name == "sspin") ctrl.getValue().toFloat() / 100f else sVal
                val newL = if (name == "lspin") ctrl.getValue().toFloat() / 100f else lVal
                selectCurHsl(newH, newS, newL)
                updateTextEntry()
            }
        }
    }

    private fun selectCurRgb(r: Float, g: Float, b: Float) {
        setCurRgb(r, g, b)
        if (applyImmediateCheck?.get() == true) {
            ColorSwatchCtrl.onColorChanged(getSwatch(), ColorSwatchCtrl.COLOR_CHANGE)
        }
    }

    private fun selectCurHsl(h: Float, s: Float, l: Float) {
        setCurHsl(h, s, l)
        if (applyImmediateCheck?.get() == true) {
            ColorSwatchCtrl.onColorChanged(getSwatch(), ColorSwatchCtrl.COLOR_CHANGE)
        }
    }

    private fun drawPalette() {
        TODO("GPU: iterate palette rows/columns, gl_rect_2d each entry; draw cross highlight at highlightEntry")
    }

    private fun getComplimentaryColor(backgroundColor: Color4): Color4 {
        val (_, _, lVal) = backgroundColor.calcHSL()
        return if (lVal < 0.5f) Color4.WHITE else Color4.BLACK
    }

    private fun onClickCancel() {
        cancelSelection()
        closeFloater()
    }

    private fun onClickSelect() {
        ColorSwatchCtrl.onColorChanged(getSwatch(), ColorSwatchCtrl.COLOR_SELECT)
        closeFloater()
    }

    private fun onClickPipette() {
        val pipetteActive = !(pipetteBtn?.getToggleState() ?: false)
        if (pipetteActive) {
            ToolMgr.instance.setTransientTool(ToolPipette.instance)
        } else {
            ToolMgr.instance.clearTransientTool()
        }
    }

    private fun onImmediateCheck() {
        val checked = applyImmediateCheck?.get() ?: false
        savedSettings.setBool("ApplyColorImmediately", checked)
        if (checked && isColorChanged()) {
            ColorSwatchCtrl.onColorChanged(getSwatch(), ColorSwatchCtrl.COLOR_CHANGE)
        }
    }

    private fun onColorSelect(te: TextureEntry) {
        selectCurRgb(te.getColor().r, te.getColor().g, te.getColor().b)
    }

    private fun onClickCopyLsl() {
        val text = "<%.3f, %.3f, %.3f>".format(getCurR(), getCurG(), getCurB())
        getWindow().copyTextToClipboard(text)
        NotificationsUtil.add("LSLColorCopiedToClipboard")
    }

    companion object {
        private const val CONTEXT_CONE_IN_ALPHA = 0.0f
        private const val CONTEXT_CONE_OUT_ALPHA = 1.0f
        private const val CONTEXT_CONE_FADE_TIME = 0.08f
    }
}
