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
            TODO("APR: use JVM equivalent - LLFloaterReg::findTypedInstance<FloaterAboutLand> and call refresh()")
        }
    }

    // -------------------------------------------------------------------------
    // Inner panel stubs – real implementations would live in separate files;
    // kept here to mirror the single-translation-unit C++ structure.
    // -------------------------------------------------------------------------

    inner class PanelLandGeneral : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate general parcel fields from ParcelMgr selection") }
        fun onClickSetGroup() { TODO("APR: open group picker for parcel group") }
        fun onClickDeed() { TODO("APR: send DeedLandToGroup message") }
        fun onClickBuyLand() { TODO("APR: open buy-land floater") }
        fun onClickReleaseLand() { TODO("APR: send AbandonLand message after confirmation") }
        fun onClickStartAuction() { TODO("APR: open FloaterAuction") }
        fun onClickSellLand() { TODO("APR: open sell-land confirmation") }
        fun onClickStopSellLand() { TODO("APR: clear for-sale flag via ParcelPropertiesUpdate") }
        fun onCommitAny() { TODO("APR: commit changed parcel flags back to simulator") }
    }

    inner class PanelLandObjects : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate prim counts and owner lists from ParcelMgr") }
        fun onClickShowOwnerObjects() { TODO("APR: select objects owned by parcel owner") }
        fun onClickShowGroupObjects() { TODO("APR: select objects owned by parcel group") }
        fun onClickShowOtherObjects() { TODO("APR: select objects owned by others") }
        fun onClickReturnOwnerObjects() { TODO("APR: send SimWideDeletes for owner objects") }
        fun onClickReturnGroupObjects() { TODO("APR: send SimWideDeletes for group objects") }
        fun onClickReturnOtherObjects() { TODO("APR: send SimWideDeletes for other objects") }
    }

    inner class PanelLandOptions : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate option checkboxes from parcel flags") }
        fun onCommitAny() { TODO("APR: commit changed option flags to simulator") }
        fun onClickSet() { TODO("APR: open landmark picker for landing point") }
        fun onClickClear() { TODO("APR: clear landing point") }
        fun onClickPublishHelp() { TODO("APR: open help about show-in-search") }
    }

    inner class PanelLandAudio : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate music URL field from parcel data") }
        fun onCommitMusicUrl() { TODO("APR: commit music URL to simulator") }
    }

    inner class PanelLandMedia : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate media fields from parcel data") }
        fun onCommitMedia() { TODO("APR: commit media settings to simulator") }
        fun onClickSetMediaUrl() { TODO("APR: open URL entry dialog") }
    }

    inner class PanelLandAccess : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate access list from parcel data") }
        fun onClickAddAccess() { TODO("APR: open avatar picker, then add to AL_ACCESS list") }
        fun onClickRemoveAccess() { TODO("APR: send ParcelAccessListUpdate to remove selected entry") }
        fun onClickAddAllowedGroup() { TODO("APR: open group picker, then add group to access") }
        fun onClickRemoveAllowedGroup() { TODO("APR: remove selected group from access") }
    }

    inner class PanelLandBan : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate ban list from parcel data") }
        fun onClickAddBan() { TODO("APR: open avatar picker, then add to AL_BAN list") }
        fun onClickRemoveBan() { TODO("APR: send ParcelAccessListUpdate to remove ban entry") }
    }

    inner class PanelLandExperiences : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate experience allow/block lists") }
    }

    inner class PanelLandEnvironment : Panel() {
        override fun postBuild(): Boolean = true
        fun refresh() { TODO("APR: populate parcel environment override settings") }
    }

    // Lightweight placeholder so the compiler resolves getChild<T> calls above.
    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? = null
}
