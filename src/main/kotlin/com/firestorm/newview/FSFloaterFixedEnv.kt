/**
 * FSFloaterFixedEnv.kt
 * Kotlin conversion of llfloaterfixedenvironment.h / llfloaterfixedenvironment.cpp
 * (no FS-specific fsfloaterfixedenv.* exists; LL upstream used directly)
 *
 * Floaters for creating and editing fixed (non-day-cycle) sky and water
 * environment settings.  The class hierarchy mirrors the C++ tree:
 *
 *   FloaterEditEnvironmentBase  (abstract)
 *     └─ FSFloaterFixedEnv      (open)
 *          ├─ FSFloaterFixedEnvWater
 *          └─ FSFloaterFixedEnvSky
 *
 * Phoenix Firestorm Project — LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLUUID

// ---------------------------------------------------------------------------
// Sealed type for the environment layer selection
// ---------------------------------------------------------------------------

enum class EnvLayer { LOCAL, PARCEL, REGION, EDIT, CURRENT }

// ---------------------------------------------------------------------------
// Minimal surface for a settings object (mirrors LLSettingsBase)
// ---------------------------------------------------------------------------

interface SettingsBase {
    val settingsType: String   // "sky" | "water"
    var name: String
    fun buildClone(): SettingsBase
}

interface SettingsSky  : SettingsBase
interface SettingsWater: SettingsBase

// ---------------------------------------------------------------------------
// Settings-edit panel interface (mirrors LLSettingsEditPanel)
// ---------------------------------------------------------------------------

interface SettingsEditPanel {
    var isDirty: Boolean
    var canChangeSettings: Boolean
    fun setSettings(settings: SettingsBase?)
    fun refresh()
    fun clearIsDirty()
    var onDirtyFlagChanged: ((Boolean) -> Unit)?
}

// ---------------------------------------------------------------------------
// Abstract base — mirrors LLFloaterEditEnvironmentBase
// ---------------------------------------------------------------------------

/**
 * Abstract base floater for environment editing.
 *
 * Manages inventory loading, permission flags, and dirty-state bookkeeping.
 * Concrete subclasses supply the sky- or water-specific tab panel and
 * environment-update logic.
 *
 * @param seed LLSD key supplied by the floater registry.
 */
abstract class FloaterEditEnvironmentBase(val seed: LLSD) {

    companion object {
        const val KEY_INVENTORY_ID = "inventory_id"
    }

    // ------------------------------------------------------------------
    // Inventory / permission state
    // ------------------------------------------------------------------

    protected var inventoryId: LLUUID = LLUUID.NULL
    protected var canCopy:  Boolean = false
    protected var canMod:   Boolean = false
    protected var canTrans: Boolean = false
    protected var canSave:  Boolean = false

    protected var isDirty: Boolean = false
        private set

    protected fun setDirtyFlag()  { isDirty = true }
    protected open fun clearDirtyFlag() { isDirty = false }

    // ------------------------------------------------------------------
    // Abstract surface
    // ------------------------------------------------------------------

    abstract fun getEditSettings(): SettingsBase?
    abstract fun setEditSettingsAndUpdate(settings: SettingsBase)
    abstract fun updateEditEnvironment()
    abstract fun doImportFromDisk()
    protected abstract fun getSettingsPicker(): Any

    // ------------------------------------------------------------------
    // Lifecycle hooks
    // ------------------------------------------------------------------

    open fun postBuild(): Boolean = true

    open fun onOpen(key: LLSD) {}

    open fun onClose(appQuitting: Boolean) {
        doCloseInventoryFloater(appQuitting)
    }

    open fun onFocusReceived() {
        // Platform: re-apply ENV_EDIT environment when floater regains focus
    }

    open fun onFocusLost() {
        // Platform: handle focus loss (no-op in upstream)
    }

    // ------------------------------------------------------------------
    // Shared actions
    // ------------------------------------------------------------------

    protected fun loadInventoryItem(inventoryItemId: LLUUID, canTrans: Boolean = true) {
        System.err.println("FloaterEditEnvironmentBase: loadInventoryItem not yet implemented")
    }

    protected fun checkAndConfirmSettingsLoss(onConfirm: () -> Unit) {
        if (isDirty) {
            // Platform: show 'unsaved changes' notification; call onConfirm on OK
        } else {
            onConfirm()
        }
    }

    protected fun doApplyUpdateInventory(settings: SettingsBase) {
        System.err.println("FloaterEditEnvironmentBase: doApplyUpdateInventory not yet implemented")
    }

    protected fun doApplyCreateNewInventory(settingsName: String, settings: SettingsBase) {
        System.err.println("FloaterEditEnvironmentBase: doApplyCreateNewInventory not yet implemented")
    }

    protected fun doApplyEnvironment(where: String, settings: SettingsBase) {
        System.err.println("FloaterEditEnvironmentBase: doApplyEnvironment not yet implemented")
    }

    protected fun doCloseInventoryFloater(quitting: Boolean) {
        System.err.println("FloaterEditEnvironmentBase: doCloseInventoryFloater not yet implemented")
    }

    protected fun canUseInventory(): Boolean {
        return false
    }

    protected fun canApplyRegion(): Boolean {
        return false
    }

    protected fun canApplyParcel(): Boolean {
        return false
    }

    protected open fun onClickCloseBtn(appQuitting: Boolean = false) {
        if (!appQuitting) {
            checkAndConfirmSettingsLoss { closeFloater(); clearDirtyFlag() }
        } else {
            closeFloater()
        }
    }

    protected fun onSaveAsCommit(notification: LLSD, response: LLSD, settings: SettingsBase) {
        System.err.println("FloaterEditEnvironmentBase: onSaveAsCommit not yet implemented")
    }

    protected fun onPanelDirtyFlagChanged(value: Boolean) {
        if (value) setDirtyFlag()
    }

    protected fun onAssetLoaded(assetId: LLUUID, settings: SettingsBase?, status: Int) {
        System.err.println("FloaterEditEnvironmentBase: onAssetLoaded not yet implemented")
    }

    private fun closeFloater() {
        System.err.println("FloaterEditEnvironmentBase: closeFloater not yet implemented")
    }

    private fun <T> getChild(name: String): T? {
        return null
    }
}

// ---------------------------------------------------------------------------
// FSFloaterFixedEnv — open middle class, mirrors LLFloaterFixedEnvironment
// ---------------------------------------------------------------------------

/**
 * Floater container for creating and editing fixed environment settings.
 *
 * Adds a tab-container, a name editor, an import button, and a flyout
 * commit/save button on top of the base floater machinery.
 *
 * @param seed LLSD key supplied by the floater registry.
 */
open class FSFloaterFixedEnv(seed: LLSD) : FloaterEditEnvironmentBase(seed) {

    // ------------------------------------------------------------------
    // Action string constants — mirror the anonymous namespace in the .cpp
    // ------------------------------------------------------------------

    protected companion object {
        const val FIELD_SETTINGS_NAME = "settings_name"
        const val CONTROL_TAB_AREA   = "tab_settings"
        const val BTN_IMPORT  = "btn_import"
        const val BTN_COMMIT  = "btn_commit"
        const val BTN_CANCEL  = "btn_cancel"
        const val BTN_FLYOUT  = "btn_flyout"
        const val BTN_LOAD    = "btn_load"

        const val ACTION_SAVE         = "save_settings"
        const val ACTION_SAVEAS       = "save_as_new_settings"
        const val ACTION_COMMIT       = "commit_changes"
        const val ACTION_APPLY_LOCAL  = "apply_local"
        const val ACTION_APPLY_PARCEL = "apply_parcel"
        const val ACTION_APPLY_REGION = "apply_region"

        const val XML_FLYOUTMENU_FILE = "menu_save_settings.xml"
    }

    // ------------------------------------------------------------------
    // Child widget stubs — populated in [postBuild]
    // ------------------------------------------------------------------

    protected var tabContainer:  TabContainer?  = null
    protected var nameEditor:    LineEditor?     = null
    private   var flyoutControl: FlyoutComboBtn? = null

    // ------------------------------------------------------------------
    // Settings state
    // ------------------------------------------------------------------

    protected var settings: SettingsBase? = null

    override fun getEditSettings(): SettingsBase? = settings

    fun setEditSettings(s: SettingsBase) {
        settings = s
        clearDirtyFlag()
        synchronizeTabs()
        refresh()
    }

    override fun setEditSettingsAndUpdate(s: SettingsBase) {
        settings = s
        updateEditEnvironment()
        synchronizeTabs()
        refresh()
        // Platform: LLEnvironment.updateEnvironment(TRANSITION_INSTANT)
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    override fun postBuild(): Boolean {
        tabContainer = getChild<TabContainer>(CONTROL_TAB_AREA)
        nameEditor   = getChild<LineEditor>(FIELD_SETTINGS_NAME)?.also { ed ->
            ed.commitOnFocusLost = true
            ed.setCommitCallback { onNameChanged(it) }
        }

        getChild<Button>(BTN_IMPORT)?.setClickedCallback { onButtonImport() }
        getChild<Button>(BTN_CANCEL)?.setClickedCallback { onClickCloseBtn() }
        getChild<Button>(BTN_LOAD)?.setClickedCallback   { onButtonLoad() }

        flyoutControl = FlyoutComboBtn(BTN_COMMIT, BTN_FLYOUT, XML_FLYOUTMENU_FILE).also { fc ->
            fc.setAction { ctrl, data -> onButtonApply(ctrl, data) }
            fc.setMenuItemVisible(ACTION_COMMIT, false)
        }

        return true
    }

    override fun onOpen(key: LLSD) {
        val invId = if (key.has(KEY_INVENTORY_ID)) key.getUUID(KEY_INVENTORY_ID) else LLUUID.NULL
        loadInventoryItem(invId)
        updateEditEnvironment()
        synchronizeTabs()
        refresh()
        // Platform: LLEnvironment.setSelectedEnvironment(ENV_EDIT, TRANSITION_INSTANT)
    }

    override fun onClose(appQuitting: Boolean) {
        doCloseInventoryFloater(appQuitting)
        if (!appQuitting) {
            // Platform: restore ENV_LOCAL, clearEnvironment(ENV_EDIT)
            settings = null
            synchronizeTabs()
        }
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------

    open fun refresh() {
        val s = settings ?: return

        val invAvail = canUseInventory()
        flyoutControl?.setMenuItemEnabled(ACTION_SAVE,         invAvail && canMod && !inventoryId.isNull())
        flyoutControl?.setMenuItemEnabled(ACTION_SAVEAS,       invAvail && canCopy)
        flyoutControl?.setMenuItemEnabled(ACTION_APPLY_PARCEL, canApplyParcel())
        flyoutControl?.setMenuItemEnabled(ACTION_APPLY_REGION, canApplyRegion())

        nameEditor?.setValue(s.name)
        nameEditor?.isEnabled = canMod

        val panels = tabContainer?.allPanels().orEmpty()
        for (panel in panels) {
            panel.canChangeSettings = canMod
            panel.refresh()
        }
    }

    protected open fun synchronizeTabs() {
        val panels = tabContainer?.allPanels().orEmpty()
        for (panel in panels) {
            panel.setSettings(settings)
        }
    }

    override fun clearDirtyFlag() {
        super.clearDirtyFlag()
        tabContainer?.allPanels()?.forEach { it.clearIsDirty() }
    }

    // ------------------------------------------------------------------
    // Button handlers
    // ------------------------------------------------------------------

    private fun onNameChanged(name: String) {
        settings?.name = name
        setDirtyFlag()
    }

    private fun onButtonImport() {
        checkAndConfirmSettingsLoss { doImportFromDisk() }
    }

    private fun onButtonApply(ctrl: String, data: LLSD) {
        val s = settings ?: return
        val clone = s.buildClone()

        if (hasLocalTexture(s)) {
            // Platform: show 'WLLocalTextureFixedBlock' notification and return
            return
        }

        when (ctrl) {
            ACTION_SAVE   -> { doApplyUpdateInventory(clone); clearDirtyFlag() }
            ACTION_SAVEAS -> {
                // Platform: show 'SaveSettingAs' notification; on OK call doApplyCreateNewInventory
            }
            ACTION_APPLY_LOCAL,
            ACTION_APPLY_PARCEL,
            ACTION_APPLY_REGION -> doApplyEnvironment(ctrl, clone)
            else -> System.err.println("FSFloaterFixedEnv: unknown settings action '$ctrl' not yet implemented")
        }
    }

    override fun onClickCloseBtn(appQuitting: Boolean) {
        if (!appQuitting) {
            checkAndConfirmSettingsLoss {
                // Platform: closeFloater(); clearDirtyFlag()
            }
        } else {
            // Platform: closeFloater()
        }
    }

    private fun onButtonLoad() {
        checkAndConfirmSettingsLoss { doSelectFromInventory() }
    }

    private fun doSelectFromInventory() {
        val picker = getSettingsPicker()
        // Platform: open settings picker filtered to ${settings?.settingsType}
    }

    private fun onPickerCommitSetting(itemId: LLUUID) {
        loadInventoryItem(itemId)
    }

    override fun getSettingsPicker(): Any {
        System.err.println("FSFloaterFixedEnv: getSettingsPicker not yet implemented")
        return object {}
    }

    // FS-specific: Firestorm checks for local-bitmap textures before applying
    private fun hasLocalTexture(s: SettingsBase): Boolean {
        return false
    }

    override fun doImportFromDisk() {
        System.err.println("FSFloaterFixedEnv: doImportFromDisk not yet implemented")
    }

    override fun updateEditEnvironment() {
        System.err.println("FSFloaterFixedEnv: updateEditEnvironment not yet implemented")
    }

    // ------------------------------------------------------------------
    // Widget stub helpers
    // ------------------------------------------------------------------

    private fun <T> getChild(name: String): T? {
        return null
    }

    // ------------------------------------------------------------------
    // Nested widget stubs
    // ------------------------------------------------------------------

    class TabContainer {
        fun allPanels(): List<SettingsEditPanel> {
            System.err.println("TabContainer: allPanels not yet implemented")
            return emptyList()
        }
    }
    class LineEditor {
        var commitOnFocusLost: Boolean = true
        var isEnabled: Boolean = true
        fun setValue(v: String) {
            // Platform: set editor text
        }
        fun setCommitCallback(cb: (String) -> Unit) {
            // Platform: wire commit callback
        }
    }
    class Button {
        fun setClickedCallback(cb: () -> Unit) {
            // Platform: wire click callback
        }
    }
    class FlyoutComboBtn(val commitBtn: String, val flyoutBtn: String, val menuXml: String) {
        fun setAction(cb: (String, LLSD) -> Unit) {
            // Platform: wire flyout action callback
        }
        fun setMenuItemVisible(action: String, visible: Boolean) {
            // Platform: toggle menu-item visibility
        }
        fun setMenuItemEnabled(action: String, enabled: Boolean) {
            // Platform: toggle menu-item enabled state
        }
    }
}

// ---------------------------------------------------------------------------
// FSFloaterFixedEnvWater — mirrors LLFloaterFixedEnvironmentWater
// ---------------------------------------------------------------------------

/**
 * Variant of [FSFloaterFixedEnv] for water settings.
 *
 * Adds a single `LLPanelSettingsWaterMainTab` tab built from the FS-specific
 * `panel_fs_settings_water.xml` layout file.
 */
class FSFloaterFixedEnvWater(seed: LLSD) : FSFloaterFixedEnv(seed) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        System.err.println("FSFloaterFixedEnvWater: postBuild not yet implemented")
        return false
    }

    override fun onOpen(key: LLSD) {
        if (settings == null) {
            // Platform: clone current water from LLEnvironment.getEnvironmentFixedWater(ENV_CURRENT)
            // settings would be set here with name "Snapshot water (new)"
        }
        super.onOpen(key)
    }

    override fun updateEditEnvironment() {
        System.err.println("FSFloaterFixedEnvWater: updateEditEnvironment not yet implemented")
    }

    override fun doImportFromDisk() {
        System.err.println("FSFloaterFixedEnvWater: doImportFromDisk not yet implemented")
    }

    private fun loadWaterSettingFromFile(filenames: List<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        // Platform: LLEnvironment.createWaterFromLegacyPreset(filename); on success setEditSettings(legacyWater)
    }
}

// ---------------------------------------------------------------------------
// FSFloaterFixedEnvSky — mirrors LLFloaterFixedEnvironmentSky
// ---------------------------------------------------------------------------

/**
 * Variant of [FSFloaterFixedEnv] for sky settings.
 *
 * Adds three tabs (atmosphere, clouds, sun/moon) each built from FS-specific
 * XML layout files.  Also saves/restores beacon state on open/close to avoid
 * the environment-beacon display being left in an inconsistent state.
 */
class FSFloaterFixedEnvSky(seed: LLSD) : FSFloaterFixedEnv(seed) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        System.err.println("FSFloaterFixedEnvSky: postBuild not yet implemented")
        return false
    }

    override fun onOpen(key: LLSD) {
        if (settings == null) {
            // Platform: clone current sky from LLEnvironment.getEnvironmentFixedSky(ENV_CURRENT); saveBeaconsState()
            // settings would be set here with name "Snapshot sky (new)"
        }
        super.onOpen(key)
    }

    override fun onClose(appQuitting: Boolean) {
        // Platform: LLEnvironment.revertBeaconsState()
        super.onClose(appQuitting)
    }

    override fun updateEditEnvironment() {
        System.err.println("FSFloaterFixedEnvSky: updateEditEnvironment not yet implemented")
    }

    override fun doImportFromDisk() {
        System.err.println("FSFloaterFixedEnvSky: doImportFromDisk not yet implemented")
    }

    private fun loadSkySettingFromFile(filenames: List<String>) {
        if (filenames.isEmpty()) return
        val filename = filenames[0]
        // Platform: LLEnvironment.createSkyFromLegacyPreset(filename); on success setEditSettings(legacySky)
    }
}
