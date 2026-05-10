package com.firestorm.newview

// Source: llfloatermediasettings.h / llfloatermediasettings.cpp
// (llfloatermediabrowser was absent from this fork; the closest equivalent is
// LLFloaterMediaSettings, which provides the tabbed UI for configuring parcel /
// object media — general settings, security (whitelist), and permissions /
// controls panels.)

// Stub panel types matching the three C++ panels
open class PanelMediaSettingsGeneral {
    fun updateMediaPreview() = TODO("GPU: resume/unpause embedded media plugin for preview")
    fun preApply()           = TODO("APR: validate URL and home-URL fields before applying")
    fun postApply()          = TODO("APR: refresh in-world media plugin after settings committed")
    fun getValues(out: MutableMap<String, Any>, includeAlt: Boolean) =
        TODO("APR: read all general-tab control values into out map")
    fun clearValues(parent: Any?, editable: Boolean, updatePreview: Boolean) =
        TODO("APR: reset general-tab controls to blank/defaults; optionally stop media preview")
    fun initValues(mediaSettings: Map<String, Any>, editable: Boolean) =
        TODO("APR: populate general-tab controls from mediaSettings LLSD map")
    fun onClose(appQuitting: Boolean) =
        TODO("APR: stop in-world media plugin preview on panel close")
}

open class PanelMediaSettingsSecurity {
    fun preApply()  = TODO("APR: validate whitelist URL entries before applying")
    fun postApply() = TODO("APR: refresh whitelist state after settings committed")
    fun getValues(out: MutableMap<String, Any>, includeAlt: Boolean) =
        TODO("APR: read security-tab (whitelist) control values into out map")
    fun clearValues(parent: Any?, editable: Boolean) =
        TODO("APR: clear whitelist entries and reset editable state")
    fun initValues(mediaSettings: Map<String, Any>, editable: Boolean) =
        TODO("APR: populate security-tab whitelist from mediaSettings LLSD map")
}

open class PanelMediaSettingsPermissions {
    fun preApply()  = TODO("APR: validate permissions/controls tab fields before applying")
    fun postApply() = TODO("APR: refresh controls state after settings committed")
    fun getValues(out: MutableMap<String, Any>, includeAlt: Boolean) =
        TODO("APR: read permissions-tab (controls) control values into out map")
    fun clearValues(parent: Any?, editable: Boolean) =
        TODO("APR: reset permissions-tab controls to defaults")
    fun initValues(mediaSettings: Map<String, Any>, editable: Boolean) =
        TODO("APR: populate permissions-tab controls from mediaSettings LLSD map")
}

class FloaterMediaSettings(val key: Any?) {

    companion object {
        private var instance: FloaterMediaSettings? = null

        fun getInstance(): FloaterMediaSettings {
            return instance ?: FloaterMediaSettings(null).also { instance = it }
        }

        fun instanceExists(): Boolean = instance != null

        fun apply() {
            val inst = instance ?: return
            if (inst.haveValuesChanged()) {
                val settings = mutableMapOf<String, Any>()
                inst.panelGeneral?.also {
                    it.preApply()
                    it.getValues(settings, false)
                }
                inst.panelSecurity?.also {
                    it.preApply()
                    it.getValues(settings, false)
                }
                inst.panelPermissions?.also {
                    it.preApply()
                    it.getValues(settings, false)
                }
                TODO("APR: LLSelectMgr.selectionSetMedia(MF_HAS_MEDIA, settings)")
                inst.panelGeneral?.postApply()
                inst.panelSecurity?.postApply()
                inst.panelPermissions?.postApply()
            }
        }

        fun initValues(
            mediaSettings: Map<String, Any>,
            editable: Boolean,
            hasMediaInfo: Boolean,
            multipleMedia: Boolean,
            multipleValidMedia: Boolean
        ) {
            val inst = instance ?: return
            inst.identicalHasMediaInfo = hasMediaInfo
            inst.multipleMedia         = multipleMedia
            inst.multipleValidMedia    = multipleValidMedia
            if (TODO("APR: inst.hasFocus()") as Boolean) return

            inst.panelGeneral?.clearValues(inst.panelGeneral, editable, false)
            inst.panelSecurity?.clearValues(inst.panelSecurity, editable)
            inst.panelPermissions?.clearValues(inst.panelPermissions, editable)

            inst.panelGeneral?.initValues(mediaSettings, editable)
            inst.panelSecurity?.initValues(mediaSettings, editable)
            inst.panelPermissions?.initValues(mediaSettings, editable)

            inst.initialValues = mediaSettings.toMutableMap()
        }

        fun clearValues(editable: Boolean) {
            val inst = instance ?: return
            inst.panelGeneral?.clearValues(inst.panelGeneral, editable, true)
            inst.panelSecurity?.clearValues(inst.panelSecurity, editable)
            inst.panelPermissions?.clearValues(inst.panelPermissions, editable)
        }
    }

    var identicalHasMediaInfo: Boolean = true
    var multipleMedia: Boolean         = false
    var multipleValidMedia: Boolean    = false

    private var panelGeneral: PanelMediaSettingsGeneral?     = null
    private var panelSecurity: PanelMediaSettingsSecurity?   = null
    private var panelPermissions: PanelMediaSettingsPermissions? = null
    private var initialValues: MutableMap<String, Any>       = mutableMapOf()

    fun postBuild(): Boolean {
        panelGeneral     = PanelMediaSettingsGeneral()
        panelSecurity    = PanelMediaSettingsSecurity()
        panelPermissions = PanelMediaSettingsPermissions()

        TODO("APR: get tab_container child; add three panel tabs; restore LastMediaSettingsTab from saved settings; bind OK/Cancel/Apply button click callbacks")

        instance = this
        return true
    }

    fun onOpen(key: Any?) {
        panelGeneral?.updateMediaPreview()
    }

    fun onClose(appQuitting: Boolean) {
        panelGeneral?.onClose(appQuitting)
        TODO("APR: FloaterReg.hideInstance('whitelist_entry')")
    }

    fun draw() {
        TODO("APR: update Apply button enabled state from haveValuesChanged(); delegate to super.draw()")
    }

    fun getHomeUrl(): String {
        return panelGeneral?.let { TODO("APR: read home URL field value from panelGeneral") as String } ?: ""
    }

    fun getPanelSecurity(): PanelMediaSettingsSecurity? = panelSecurity

    private fun onBtnOK() {
        apply()
        TODO("APR: save LastMediaSettingsTab; close this floater")
    }

    private fun onBtnCancel() {
        TODO("APR: revert any uncommitted UI changes; close this floater")
    }

    private fun onBtnApply() {
        apply()
    }

    private fun onTabChanged(fromClick: Boolean) {
        TODO("APR: persist current tab index to LastMediaSettingsTab setting")
    }

    private fun commitFields() {
        TODO("APR: force-commit any in-progress text edits in all three panels so haveValuesChanged() sees latest values")
    }

    private fun haveValuesChanged(): Boolean {
        val currentValues = mutableMapOf<String, Any>()
        panelGeneral?.getValues(currentValues, true)
        panelSecurity?.getValues(currentValues, true)
        panelPermissions?.getValues(currentValues, true)
        return currentValues != initialValues
    }
}
