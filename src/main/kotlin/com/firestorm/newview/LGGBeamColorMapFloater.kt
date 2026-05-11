package com.firestorm.newview

private const val CORRECTION_X = 0
private const val CORRECTION_Y = -40

private fun convertXToHue(place: Int): Float = ((place - 6) / 396.0f) * 720.0f

private fun convertHueToX(place: Float): Int = Math.round((place / 720.0f) * 396.0f) + 6

class LGGBeamColorMapFloater(seed: LLSD) : LLFloater(seed) {

    private var contextConeOpacity: Float = 0f
    private val contextConeInAlpha: Float = CONTEXT_CONE_IN_ALPHA
    private val contextConeOutAlpha: Float = CONTEXT_CONE_OUT_ALPHA
    private val contextConeFadeTime: Float = CONTEXT_CONE_FADE_TIME
    private var fsPanel: FSPanelPrefs? = null
    private var data: LGGBeamColors = LGGBeamColors()
    private var colorSlider: LLSliderCtrl? = null
    private var beamColorPreview: LLColorSwatchCtrl? = null

    fun postBuild(): Boolean {
        TODO("Wire 'BeamColor_Save' commit -> onClickSave, 'BeamColor_Load' -> onClickLoad, 'BeamColor_Cancel' -> closeFloater; bind mColorSlider to 'BeamColor_Speed' -> onClickSlider; bind mBeamColorPreview to 'BeamColor_Preview'; call fixOrder()")
    }

    open fun draw() {
        val bColor = LGGBeamMaps.beamColorFromData(data)
        TODO("GPU: set beamColorPreview to bColor; drawConeToOwner when fsPanel != null; call super.draw(); then draw hue strip and crosshair markers via OpenGL")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val hue = getHueFromLocation(x, y)
        if (hue != -1f) {
            data = data.copy(startHue = hue)
            fixOrder()
            return true
        }
        return super.handleMouseDown(x, y, mask)
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        val hue = getHueFromLocation(x, y)
        if (hue != -1f) {
            data = data.copy(endHue = hue)
            fixOrder()
            return true
        }
        return super.handleRightMouseDown(x, y, mask)
    }

    fun setData(panel: FSPanelPrefs) {
        fsPanel = panel
        TODO("APR: gFloaterView->getParentFloater(fsPanel)->addDependentFloater(this)")
    }

    private fun onClickSlider() {
        fixOrder()
    }

    private fun onClickSave() {
        TODO("APR: resolve 'beamsColors/NewBeamColor.xml' via gDirUtilp; open LLFilePickerReplyThread for FFSAVE_BEAM -> onSaveCallback")
    }

    private fun onClickLoad() {
        TODO("APR: open LLFilePickerReplyThread for FFLOAD_XML -> onLoadCallback")
    }

    private fun onSaveCallback(filenames: List<String>) {
        val filename = filenames[0]
        val exportData = getDataSerialized()
        TODO("APR: write exportData as pretty XML to $filename; gSavedSettings.setString('FSBeamColorFile', baseName); fsPanel?.refreshBeamLists(); closeFloater()")
    }

    private fun onLoadCallback(filenames: List<String>) {
        TODO("APR: parse LLSD XML from filenames[0]; data = LGGBeamColors.fromLLSD(importData); set 'BeamColor_Speed' child value to data.rotateSpeed * 100f")
    }

    private fun getHueFromLocation(x: Int, y: Int): Float {
        if (y > (201 + CORRECTION_Y) && y < (277 + CORRECTION_Y)) {
            return when {
                x < (6 + CORRECTION_X) -> 0f
                x > (402 + CORRECTION_X) -> 720f
                else -> convertXToHue(x + CORRECTION_X)
            }
        }
        return -1f
    }

    private fun fixOrder() {
        val sliderValue = colorSlider?.valueF32 ?: data.rotateSpeed * 100f
        var speed = sliderValue / 100f
        var start = data.startHue
        var end = data.endHue
        if (end < start) {
            val tmp = start; start = end; end = tmp
        }
        data = data.copy(rotateSpeed = speed, startHue = start, endHue = end)
    }

    private fun getDataSerialized(): Map<String, Any?> = data.toLLSD()
}
