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
        System.err.println("FSFloaterVolumeControls: onVisibilityChange not yet implemented")
    }

    // -------------------------------------------------------------------------
    // Platform stubs
    // -------------------------------------------------------------------------

    private fun setChildEnabled(name: String, enabled: Boolean) {
        System.err.println("FSFloaterVolumeControls: setChildEnabled not yet implemented")
    }
}

// =============================================================================
// Stubs local to this file (prefixed "VC" to avoid collision)
// =============================================================================

object UtilityBar {
    fun setVolumeControlsButtonExpanded(expanded: Boolean) {
        System.err.println("UtilityBar: setVolumeControlsButtonExpanded not yet implemented")
    }
}

object SavedSettings {
    fun getBool(key: String): Boolean {
        return false
    }
}
