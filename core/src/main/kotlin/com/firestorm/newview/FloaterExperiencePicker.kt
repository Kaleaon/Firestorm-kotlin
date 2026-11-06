package com.firestorm.newview

import com.firestorm.ui.Floater
import com.firestorm.ui.Handle
import com.firestorm.ui.PanelExperiencePicker
import com.firestorm.ui.View
import com.firestorm.viewer.FloaterReg
import java.util.UUID

class FloaterExperiencePicker(key: LLSD) : Floater(key) {

    typealias SelectCallback = (List<UUID>) -> Unit
    typealias FilterFunction = (LLSD) -> Boolean
    typealias FilterList = List<FilterFunction>

    private var searchPanel: PanelExperiencePicker? = null

    private var frustumOrigin: Handle<View>? = null
    private var contextConeOpacity: Float = 0f
    private var contextConeInAlpha: Float = CONTEXT_CONE_IN_ALPHA
    private var contextConeOutAlpha: Float = CONTEXT_CONE_OUT_ALPHA
    private var contextConeFadeTime: Float = CONTEXT_CONE_FADE_TIME

    override fun postBuild(): Boolean {
        val panel = PanelExperiencePicker()
        addChild(panel)
        panel.setOrigin(0, 0)
        searchPanel = panel
        return super.postBuild()
    }

    override fun draw() {
        drawFrustum()
        super.draw()
    }

    private fun drawFrustum() {
        val maxOpacity = SavedSettings.getFloat("PickerContextOpacity", 0.4f)
        // no-op
    }

    companion object {
        private const val CONTEXT_CONE_IN_ALPHA  = 0.0f
        private const val CONTEXT_CONE_OUT_ALPHA = 1.0f
        private const val CONTEXT_CONE_FADE_TIME = 0.08f

        fun show(
            callback: SelectCallback,
            key: UUID,
            allowMultiple: Boolean,
            closeOnSelect: Boolean,
            filters: FilterList,
            frustumOrigin: View?
        ): FloaterExperiencePicker? {
            val floater = FloaterReg.showTypedInstance<FloaterExperiencePicker>(
                "experience_search", key
            ) ?: run {
                return null
            }

            floater.searchPanel?.let { panel ->
                panel.selectionCallback = callback
                panel.closeOnSelect = closeOnSelect
                panel.setAllowMultiple(allowMultiple)
                panel.setDefaultFilters()
                panel.addFilters(filters)
                panel.filterContent()
            }

            if (frustumOrigin != null) {
                floater.frustumOrigin = frustumOrigin.getHandle()
            }

            return floater
        }
    }
}
