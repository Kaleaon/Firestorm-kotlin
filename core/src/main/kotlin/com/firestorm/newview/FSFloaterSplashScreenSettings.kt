package com.firestorm.newview

// =============================================================================
// FSFloaterSplashScreenSettings
// =============================================================================

/**
 * Floater that exposes the splash-screen accessibility/appearance settings as
 * a set of checkboxes.  Every checkbox change is immediately persisted via
 * [saveSettings].
 *
 * Mirrors `FSFloaterSplashScreenSettings` from
 * `fsfloatersplashscreensettings.h/.cpp`.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterSplashScreenSettings(val key: Any) {

    private var hideTopBarCheck: SSCheckBoxCtrl? = null
    private var hideBlogsCheck: SSCheckBoxCtrl? = null
    private var hideDestinationsCheck: SSCheckBoxCtrl? = null
    private var useGrayModeCheck: SSCheckBoxCtrl? = null
    private var useHighContrastCheck: SSCheckBoxCtrl? = null
    private var useAllCapsCheck: SSCheckBoxCtrl? = null
    private var useLargerFontsCheck: SSCheckBoxCtrl? = null
    private var noTransparencyCheck: SSCheckBoxCtrl? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun postBuild(): Boolean {
        hideTopBarCheck      = getChild("hide_top_bar")
        hideBlogsCheck       = getChild("hide_blogs")
        hideDestinationsCheck = getChild("hide_destinations")
        useGrayModeCheck     = getChild("use_gray_mode")
        useHighContrastCheck = getChild("use_high_contrast")
        useAllCapsCheck      = getChild("use_all_caps")
        useLargerFontsCheck  = getChild("use_larger_fonts")
        noTransparencyCheck  = getChild("no_transparency")

        val onChange = { onSettingChanged() }
        hideTopBarCheck?.onCommit      = onChange
        hideBlogsCheck?.onCommit       = onChange
        hideDestinationsCheck?.onCommit = onChange
        useGrayModeCheck?.onCommit     = onChange
        useHighContrastCheck?.onCommit = onChange
        useAllCapsCheck?.onCommit      = onChange
        useLargerFontsCheck?.onCommit  = onChange
        noTransparencyCheck?.onCommit  = onChange

        loadSettings()
        return true
    }

    fun onOpen(key: Any) {
        System.err.println("FSFloaterSplashScreenSettings: onOpen not yet implemented")
        loadSettings()
    }

    // -------------------------------------------------------------------------
    // Settings persistence
    // -------------------------------------------------------------------------

    private fun loadSettings() {
        hideTopBarCheck?.value       = SplashSettings.getBool("FSSplashScreenHideTopBar")
        hideBlogsCheck?.value        = SplashSettings.getBool("FSSplashScreenHideBlogs")
        hideDestinationsCheck?.value = SplashSettings.getBool("FSSplashScreenHideDestinations")
        useGrayModeCheck?.value      = SplashSettings.getBool("FSSplashScreenUseGrayMode")
        useHighContrastCheck?.value  = SplashSettings.getBool("FSSplashScreenUseHighContrast")
        useAllCapsCheck?.value       = SplashSettings.getBool("FSSplashScreenUseAllCaps")
        useLargerFontsCheck?.value   = SplashSettings.getBool("FSSplashScreenUseLargerFonts")
        noTransparencyCheck?.value   = SplashSettings.getBool("FSSplashScreenNoTransparency")
    }

    private fun saveSettings() {
        SplashSettings.setBool("FSSplashScreenHideTopBar",        hideTopBarCheck?.value ?: false)
        SplashSettings.setBool("FSSplashScreenHideBlogs",         hideBlogsCheck?.value ?: false)
        SplashSettings.setBool("FSSplashScreenHideDestinations",  hideDestinationsCheck?.value ?: false)
        SplashSettings.setBool("FSSplashScreenUseGrayMode",       useGrayModeCheck?.value ?: false)
        SplashSettings.setBool("FSSplashScreenUseHighContrast",   useHighContrastCheck?.value ?: false)
        SplashSettings.setBool("FSSplashScreenUseAllCaps",        useAllCapsCheck?.value ?: false)
        SplashSettings.setBool("FSSplashScreenUseLargerFonts",    useLargerFontsCheck?.value ?: false)
        SplashSettings.setBool("FSSplashScreenNoTransparency",    noTransparencyCheck?.value ?: false)
    }

    private fun onSettingChanged() {
        saveSettings()
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun <T> getChild(name: String): T? {
        return null
    }
}

// =============================================================================
// Stubs local to this file
// =============================================================================

/** Stub for LLCheckBoxCtrl as used in this floater. */
class SSCheckBoxCtrl {
    var value: Boolean = false
    var onCommit: (() -> Unit)? = null
}

object SplashSettings {
    fun getBool(key: String): Boolean {
        return false
    }
    fun setBool(key: String, value: Boolean) {
        System.err.println("SplashSettings: setBool not yet implemented")
    }
}
