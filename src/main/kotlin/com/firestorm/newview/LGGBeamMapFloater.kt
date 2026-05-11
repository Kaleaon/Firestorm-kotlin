package com.firestorm.newview

import kotlin.math.sqrt

data class LGGPoint(val x: Int, val y: Int, val c: Color4)

class LGGBeamMapFloater(seed: LLSD) : LLFloater(seed) {

    private val dots: MutableList<LGGPoint> = mutableListOf()
    private var contextConeOpacity: Float = 0f
    private val contextConeInAlpha: Float = CONTEXT_CONE_IN_ALPHA
    private val contextConeOutAlpha: Float = CONTEXT_CONE_OUT_ALPHA
    private val contextConeFadeTime: Float = CONTEXT_CONE_FADE_TIME
    private var fsPanel: FSPanelPrefs? = null
    private var beamshapePanel: LLPanel? = null

    fun postBuild(): Boolean {
        TODO("Wire 'beamshape_save' -> onClickSave, 'beamshape_clear' -> onClickClear, 'beamshape_load' -> onClickLoad, 'cancel' -> closeFloater; wire 'back_color_swatch' -> onBackgroundChange; set 'beam_color_swatch' to Color4.red; bind beamshapePanel to 'beamshape_draw'")
    }

    open fun draw() {
        TODO("GPU: drawConeToOwner when fsPanel != null; call super.draw(); push GL matrix; draw concentric reference circles at beamshapePanel center; draw each dot as 3 concentric filled circles (white/black/dot.c); pop GL matrix")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (y > 39 && x > 16 && x < 394 && y < 317) {
            val color = TODO("GPU: read 'beam_color_swatch' color swatch value") as Color4
            dots.add(LGGPoint(x, y, color))
        }
        return super.handleMouseDown(x, y, mask)
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        dots.removeAll { dot ->
            sqrt(((x - dot.x).toFloat().let { it * it } + (y - dot.y).toFloat().let { it * it }).toDouble()) < 7.0
        }
        return super.handleMouseDown(x, y, mask)
    }

    fun setData(panel: FSPanelPrefs) {
        fsPanel = panel
        TODO("APR: gFloaterView->getParentFloater(fsPanel)->addDependentFloater(this)")
    }

    private fun onClickSave() {
        TODO("APR: resolve 'beams/NewBeam.xml' via gDirUtilp; open LLFilePickerReplyThread for FFSAVE_BEAM -> onSaveCallback")
    }

    private fun onClickClear() {
        clearPoints()
    }

    private fun onClickLoad() {
        TODO("APR: open LLFilePickerReplyThread for FFLOAD_XML -> onLoadCallback")
    }

    private fun onBackgroundChange() {
        TODO("GPU: set beamshapePanel background color from 'back_color_swatch' value")
    }

    private fun onSaveCallback(filenames: List<String>) {
        val filename = filenames[0]
        val exportData = mutableMapOf<String, Any?>()
        val panelWidth = beamshapePanel?.rect?.width?.toFloat() ?: 1f
        exportData["scale"] = 8.0f / panelWidth
        exportData["data"] = getDataSerialized()
        TODO("APR: write exportData as pretty XML to $filename; gSavedSettings.setString('FSBeamShape', baseName); fsPanel?.refreshBeamLists()")
    }

    private fun onLoadCallback(filenames: List<String>) {
        dots.clear()
        TODO("APR: parse LLSD XML from filenames[0]; for each entry compute scaled offset relative to beamshapePanel center; reconstruct LGGPoint list and assign to dots")
    }

    private fun clearPoints() {
        dots.clear()
    }

    private fun getDataSerialized(): List<Map<String, Any?>> {
        val cx = beamshapePanel?.rect?.centerX?.toFloat() ?: 0f
        val cy = beamshapePanel?.rect?.centerY?.toFloat() ?: 0f
        return dots.map { dot ->
            mapOf(
                "offset" to mapOf("x" to 0f, "y" to (dot.x - cx), "z" to (dot.y - cy)),
                "color"  to mapOf("x" to dot.c.r, "y" to dot.c.g, "z" to dot.c.b)
            )
        }
    }
}
