package com.firestorm.newview

// =============================================================================
// FSFloaterVolumeControls
// =============================================================================

/**
 * Standalone volume-controls floater.
 *
 * Mirrors `FSFloaterVolumeControls` from `fsfloatervolumecontrols.h/.cpp`.
 *
 * @param key LLSD construction key from the floater registry.
 */
class FSFloaterVolumeControls(val key: Any) {

    fun postBuild(): Boolean {
        val muteSoundEffects = SavedSettings.getBool("MuteSounds")
        val muteAllSounds = SavedSettings.getBool("MuteAudio")
        val disableAudio = muteSoundEffects || muteAllSounds
        setChildEnabled("gesture_audio_play_btn", !disableAudio)
        setChildEnabled("collisions_audio_play_btn", !disableAudio)
        return true
    }

    open fun onVisibilityChange(newVisibility: Boolean) {
        UtilityBar.setVolumeControlsButtonExpanded(newVisibility)
        TODO("Platform: LLFloater::onVisibilityChange(newVisibility)")
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun setChildEnabled(name: String, enabled: Boolean): Unit =
        TODO("Platform: getChild<LLCheckBoxCtrl>(\"$name\").setEnabled($enabled)")
}

// =============================================================================
// Stubs local to this file (prefixed "VC" to avoid collision)
// =============================================================================

object UtilityBar {
    fun setVolumeControlsButtonExpanded(expanded: Boolean): Unit =
        TODO("Platform: UtilityBar::instance().setVolumeControlsButtonExpanded($expanded)")
}

object SavedSettings {
    fun getBool(key: String): Boolean =
        TODO("Platform: gSavedSettings.getBOOL(\"$key\")")
}
