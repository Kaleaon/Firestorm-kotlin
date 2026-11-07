package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.Panel
import com.firestorm.llui.TabContainer
import com.firestorm.llui.Button
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.ComboBox
import com.firestorm.llui.LineEditor
import com.firestorm.llui.TextBox
import com.firestorm.llui.TextureCtrl
import com.firestorm.llui.SpinCtrl
import com.firestorm.llui.UICtrl
import com.firestorm.llmessage.MessageSystem
import com.firestorm.llcommon.LLUUID
import com.firestorm.newview.ParcelMgr

// Displays and edits the land/parcel properties for the selected parcel.
// Mirrors the C++ LLFloaterLand / LLFloaterAboutLand pair (floater_about_land.xml).
class FloaterAboutLand(seed: Any) : Floater(seed) {

    private var tabLand: TabContainer? = null

    private var panelGeneral: PanelLandGeneral? = null
    private var panelObjects: PanelLandObjects? = null
    private var panelOptions: PanelLandOptions? = null
    private var panelAudio: PanelLandAudio? = null
    private var panelMedia: PanelLandMedia? = null
    private var panelAccess: PanelLandAccess? = null
    private var panelBan: PanelLandBan? = null
    private var panelExperiences: PanelLandExperiences? = null
    private var panelEnvironment: PanelLandEnvironment? = null

    // Parcel selection token kept alive for the lifetime of this floater.
    private var parcelSelection: Any? = null

    override fun postBuild(): Boolean {
        tabLand = getChild("land_tab")
        panelGeneral     = getChild("land_general_panel")
        panelObjects     = getChild("land_objects_panel")
        panelOptions     = getChild("land_options_panel")
        panelAudio       = getChild("land_audio_panel")
        panelMedia       = getChild("land_media_panel")
        panelAccess      = getChild("land_access_panel")
        panelBan         = getChild("land_ban_panel")
        panelExperiences = getChild("land_experiences_panel")
        panelEnvironment = getChild("land_env_panel")
        return true
    }

    override fun onOpen(key: Any) {
        parcelSelection = ParcelMgr.getInstance().getFloatingParcelSelection()
        refresh()
    }

    override fun onClose(appQuitting: Boolean) {
        parcelSelection = null
    }

    fun refresh() {
        val parcel = ParcelMgr.getInstance().getFloatingParcelSelection()
        panelGeneral?.refresh()
        panelObjects?.refresh()
        panelOptions?.refresh()
        panelAudio?.refresh()
        panelMedia?.refresh()
        panelAccess?.refresh()
        panelBan?.refresh()
    }

    companion object {
        fun refreshAll() {
            System.err.println("FloaterAboutLand: refreshAll not yet implemented")
        }
    }

    // -------------------------------------------------------------------------
    // Inner panel stubs – real implementations would live in separate files;
    // kept here to mirror the single-translation-unit C++ structure.
    // -------------------------------------------------------------------------

    inner class PanelLandGeneral : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandGeneral: refresh not yet implemented") }
        fun onClickSetGroup() { System.err.println("PanelLandGeneral: onClickSetGroup not yet implemented") }
        fun onClickDeed() { System.err.println("PanelLandGeneral: onClickDeed not yet implemented") }
        fun onClickBuyLand() { System.err.println("PanelLandGeneral: onClickBuyLand not yet implemented") }
        fun onClickReleaseLand() { System.err.println("PanelLandGeneral: onClickReleaseLand not yet implemented") }
        fun onClickStartAuction() { System.err.println("PanelLandGeneral: onClickStartAuction not yet implemented") }
        fun onClickSellLand() { System.err.println("PanelLandGeneral: onClickSellLand not yet implemented") }
        fun onClickStopSellLand() { System.err.println("PanelLandGeneral: onClickStopSellLand not yet implemented") }
        fun onCommitAny() { System.err.println("PanelLandGeneral: onCommitAny not yet implemented") }
    }

    inner class PanelLandObjects : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandObjects: refresh not yet implemented") }
        fun onClickShowOwnerObjects() { System.err.println("PanelLandObjects: onClickShowOwnerObjects not yet implemented") }
        fun onClickShowGroupObjects() { System.err.println("PanelLandObjects: onClickShowGroupObjects not yet implemented") }
        fun onClickShowOtherObjects() { System.err.println("PanelLandObjects: onClickShowOtherObjects not yet implemented") }
        fun onClickReturnOwnerObjects() { System.err.println("PanelLandObjects: onClickReturnOwnerObjects not yet implemented") }
        fun onClickReturnGroupObjects() { System.err.println("PanelLandObjects: onClickReturnGroupObjects not yet implemented") }
        fun onClickReturnOtherObjects() { System.err.println("PanelLandObjects: onClickReturnOtherObjects not yet implemented") }
    }

    inner class PanelLandOptions : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandOptions: refresh not yet implemented") }
        fun onCommitAny() { System.err.println("PanelLandOptions: onCommitAny not yet implemented") }
        fun onClickSet() { System.err.println("PanelLandOptions: onClickSet not yet implemented") }
        fun onClickClear() { System.err.println("PanelLandOptions: onClickClear not yet implemented") }
        fun onClickPublishHelp() { System.err.println("PanelLandOptions: onClickPublishHelp not yet implemented") }
    }

    inner class PanelLandAudio : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandAudio: refresh not yet implemented") }
        fun onCommitMusicUrl() { System.err.println("PanelLandAudio: onCommitMusicUrl not yet implemented") }
    }

    inner class PanelLandMedia : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandMedia: refresh not yet implemented") }
        fun onCommitMedia() { System.err.println("PanelLandMedia: onCommitMedia not yet implemented") }
        fun onClickSetMediaUrl() { System.err.println("PanelLandMedia: onClickSetMediaUrl not yet implemented") }
    }

    inner class PanelLandAccess : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandAccess: refresh not yet implemented") }
        fun onClickAddAccess() { System.err.println("PanelLandAccess: onClickAddAccess not yet implemented") }
        fun onClickRemoveAccess() { System.err.println("PanelLandAccess: onClickRemoveAccess not yet implemented") }
        fun onClickAddAllowedGroup() { System.err.println("PanelLandAccess: onClickAddAllowedGroup not yet implemented") }
        fun onClickRemoveAllowedGroup() { System.err.println("PanelLandAccess: onClickRemoveAllowedGroup not yet implemented") }
    }

    inner class PanelLandBan : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandBan: refresh not yet implemented") }
        fun onClickAddBan() { System.err.println("PanelLandBan: onClickAddBan not yet implemented") }
        fun onClickRemoveBan() { System.err.println("PanelLandBan: onClickRemoveBan not yet implemented") }
    }

    inner class PanelLandExperiences : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandExperiences: refresh not yet implemented") }
    }

    inner class PanelLandEnvironment : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { System.err.println("PanelLandEnvironment: refresh not yet implemented") }
    }

    // Lightweight placeholder so the compiler resolves getChild<T> calls above.
    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = null
}
