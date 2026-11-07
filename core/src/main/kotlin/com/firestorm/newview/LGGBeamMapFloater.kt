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
        System.err.println("LGGBeamMapFloater: postBuild not yet implemented")
        return false
    }

    open fun draw() {
        // no-op
    }

    open fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (y > 39 && x > 16 && x < 394 && y < 317) {
            // no-op: beam_color_swatch read not yet implemented
            val color = Color4(1f, 1f, 1f, 1f)
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
        System.err.println("LGGBeamMapFloater: setData addDependentFloater not yet implemented")
    }

    private fun onClickSave() {
        System.err.println("LGGBeamMapFloater: onClickSave not yet implemented")
    }

    private fun onClickClear() {
        clearPoints()
    }

    private fun onClickLoad() {
        System.err.println("LGGBeamMapFloater: onClickLoad not yet implemented")
    }

    private fun onBackgroundChange() {
        // no-op
    }

    private fun onSaveCallback(filenames: List<String>) {
        val filename = filenames[0]
        val exportData = mutableMapOf<String, Any?>()
        val panelWidth = beamshapePanel?.rect?.width?.toFloat() ?: 1f
        exportData["scale"] = 8.0f / panelWidth
        exportData["data"] = getDataSerialized()
        System.err.println("LGGBeamMapFloater: onSaveCallback not yet implemented")
    }

    private fun onLoadCallback(filenames: List<String>) {
        dots.clear()
        System.err.println("LGGBeamMapFloater: onLoadCallback not yet implemented")
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
