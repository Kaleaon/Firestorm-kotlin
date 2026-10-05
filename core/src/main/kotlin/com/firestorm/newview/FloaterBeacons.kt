package com.firestorm.newview

import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.Floater
import com.firestorm.llui.UICtrl

// Front-end to Pipeline beacon/highlight controls.
// FloaterReg is the sole factory; the constructor is private to enforce that.
class FloaterBeacons private constructor(seed: Any) : Floater(seed) {

    init {
        // Synchronise pipeline state from saved settings at construction time.
        // Beacons do not render unless the floater is open, so this is safe to
        // defer until first instantiation rather than at viewer start-up.
        Pipeline.setRenderScriptedTouchBeacons(SavedSettings.getBool("scripttouchbeacon"))
        Pipeline.setRenderScriptedBeacons(     SavedSettings.getBool("scriptsbeacon"))
        Pipeline.setRenderPhysicalBeacons(     SavedSettings.getBool("physicalbeacon"))
        Pipeline.setRenderSoundBeacons(        SavedSettings.getBool("soundsbeacon"))
        Pipeline.setRenderParticleBeacons(     SavedSettings.getBool("particlesbeacon"))
        Pipeline.setRenderHighlights(          SavedSettings.getBool("renderhighlights"))
        Pipeline.setRenderBeacons(             SavedSettings.getBool("renderbeacons"))
        Pipeline.setRenderMOAPBeacons(         SavedSettings.getBool("moapbeacon"))

        registerCommitCallback("Beacons.UICheck") { ctrl -> onClickUICheck(ctrl) }
    }

    override fun postBuild(): Boolean = true

    // Each checkbox in the beacons floater routes through this single callback.
    // "touch_only" and "scripted" are mutually exclusive; the rest are independent.
    fun onClickUICheck(ctrl: UICtrl?) {
        val check = ctrl as? CheckBoxCtrl ?: return
        when (check.getName()) {
            "touch_only" -> {
                Pipeline.toggleRenderScriptedTouchBeacons()
                // Enforce mutual exclusion: if both are now ON, turn scripted OFF.
                if (Pipeline.getRenderScriptedTouchBeacons() && Pipeline.getRenderScriptedBeacons()) {
                    Pipeline.setRenderScriptedBeacons(false)
                    getChild<CheckBoxCtrl>("scripted").setControlValue(false)
                    getChild<CheckBoxCtrl>("scripted").setValue(false)
                    getChild<CheckBoxCtrl>("touch_only").setControlValue(true)
                    getChild<CheckBoxCtrl>("touch_only").setValue(true)
                }
            }
            "scripted" -> {
                Pipeline.toggleRenderScriptedBeacons()
                // Enforce mutual exclusion: if both are now ON, turn touch_only OFF.
                if (Pipeline.getRenderScriptedTouchBeacons() && Pipeline.getRenderScriptedBeacons()) {
                    Pipeline.setRenderScriptedTouchBeacons(false)
                    getChild<CheckBoxCtrl>("touch_only").setControlValue(false)
                    getChild<CheckBoxCtrl>("touch_only").setValue(false)
                    getChild<CheckBoxCtrl>("scripted").setControlValue(true)
                    getChild<CheckBoxCtrl>("scripted").setValue(true)
                }
            }
            "physical"   -> Pipeline.setRenderPhysicalBeacons(check.get())
            "sounds"     -> Pipeline.setRenderSoundBeacons(check.get())
            "particles"  -> Pipeline.setRenderParticleBeacons(check.get())
            "moapbeacon" -> Pipeline.setRenderMOAPBeacons(check.get())
            "highlights" -> Pipeline.toggleRenderHighlights()
            "beacons"    -> Pipeline.toggleRenderBeacons()
        }
    }

    companion object {
        // Expose a factory entry point matching the FloaterReg pattern used elsewhere.
        fun create(seed: Any): FloaterBeacons = FloaterBeacons(seed)
    }
}
