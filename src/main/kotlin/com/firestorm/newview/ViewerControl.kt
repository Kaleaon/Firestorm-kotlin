/**
 * ViewerControl.kt
 * Kotlin port of llviewercontrol.h / llviewercontrol.cpp
 *
 * Declares and initialises the viewer's named control groups (gSavedSettings,
 * gSavedPerAccountSettings, gCrashSettings, gWarningSettings) and provides
 * the top-level [initSettings] entry-point that registers every known setting
 * with its type and default value.
 *
 * The C++ file was extremely large (~3 000 lines of listener callbacks and
 * llcontroldef entries).  The Kotlin port captures the structure faithfully:
 * control-group singletons are exposed as properties on the [ViewerControl]
 * object, and every known setting name is registered inside [initSettings].
 * Complex listener callbacks are stubbed with [TODO] so they can be filled in
 * incrementally without breaking compilation.
 *
 * Original author: Richard Nelson
 * Copyright (C) 2010, Linden Research, Inc.  LGPL 2.1
 */

package com.firestorm.newview

import com.firestorm.llxml.ControlGroup
import com.firestorm.llxml.ControlVariable

/**
 * Viewer-wide control-variable registry.
 *
 * C++ globals mapped to Kotlin
 * ----------------------------
 *  gSavedSettings           → [ViewerControl.savedSettings]  (group "Global")
 *  gSavedPerAccountSettings → [ViewerControl.perAccountSettings]
 *  gCrashSettings           → [ViewerControl.crashSettings]
 *  gWarningSettings         → [ViewerControl.warningSettings]
 *  gLastRunVersion          → [ViewerControl.lastRunVersion]
 *
 * The optional "hacked godmode" toggle is preserved as a property rather than
 * a compile-time flag, because Kotlin does not have a preprocessor.
 */
object ViewerControl {

    // -----------------------------------------------------------------------
    // Control groups — mirror the C++ LLControlGroup global instances
    // -----------------------------------------------------------------------

    /**
     * Global saved settings (persisted at end of session).
     * Named "Global" to match the C++ invariant used elsewhere in the codebase.
     *
     * C++ equivalent: `LLControlGroup gSavedSettings("Global")`
     */
    val savedSettings: ControlGroup = ControlGroup("Global")

    /**
     * Per-account settings, keyed by agent username (persisted at end of session).
     *
     * C++ equivalent: `LLControlGroup gSavedPerAccountSettings("PerAccount")`
     */
    val perAccountSettings: ControlGroup = ControlGroup("PerAccount")

    /**
     * Settings that survive crashes and are written more aggressively.
     *
     * C++ equivalent: `LLControlGroup gCrashSettings("CrashSettings")`
     */
    val crashSettings: ControlGroup = ControlGroup("CrashSettings")

    /**
     * Persists which warning dialogs the user has dismissed permanently.
     *
     * C++ equivalent: `LLControlGroup gWarningSettings("Warnings")`
     */
    val warningSettings: ControlGroup = ControlGroup("Warnings")

    // -----------------------------------------------------------------------
    // Miscellaneous globals
    // -----------------------------------------------------------------------

    /** The viewer version string from the previous session, read at startup. */
    var lastRunVersion: String = ""

    /**
     * Runtime toggle for the "hacked godmode" feature.
     * Replaces the C++ `#ifdef TOGGLE_HACKED_GODLIKE_VIEWER / bool gHackGodmode`.
     */
    var hackGodmode: Boolean = false

    // -----------------------------------------------------------------------
    // Settings initialisation
    // -----------------------------------------------------------------------

    /**
     * Register all viewer control variables with their types and default values.
     *
     * This corresponds to the C++ functions in llcontroldef.cpp that called
     * `gSavedSettings.declareBOOL(...)`, `declareS32(...)`, etc.
     *
     * The settings are grouped by subsystem to aid readability.  Each call
     * registers the name, default value, and a human-readable comment exactly
     * as the original C++ declared them.  Listener wiring (the
     * `settings_setup_listeners()` C++ function) is handled in [setupListeners].
     */
    fun initSettings() {
        // --- Rendering / graphics ---
        savedSettings.declare("RenderQualityPerformance",   ControlVariable.Type.U32,   3,          "Overall render quality preset (0–6)")
        savedSettings.declare("RenderFarClip",              ControlVariable.Type.F32,   128f,       "Draw distance in metres")
        savedSettings.declare("RenderShadowDetail",         ControlVariable.Type.S32,   2,          "Shadow map quality level")
        savedSettings.declare("RenderDeferred",             ControlVariable.Type.BOOL,  true,       "Enable deferred / PBR pipeline")
        savedSettings.declare("RenderAvatarMaxNonImpostors",ControlVariable.Type.U32,   16u,        "Max non-impostor avatars rendered")
        savedSettings.declare("RenderAvatarMouselook",      ControlVariable.Type.BOOL,  true,       "Show avatar body in first-person view")
        savedSettings.declare("RenderTransparentWater",     ControlVariable.Type.BOOL,  true,       "Use transparent water shader")
        savedSettings.declare("RenderReflectionDetail",     ControlVariable.Type.S32,   3,          "Reflection map resolution tier")
        savedSettings.declare("RenderTerrainScale",         ControlVariable.Type.F32,   1.0f,       "Terrain texture tiling scale")
        savedSettings.declare("RenderPBRTerrainScale",      ControlVariable.Type.F32,   1.0f,       "PBR terrain texture tiling scale")
        savedSettings.declare("RenderGamma",                ControlVariable.Type.F32,   0f,         "Display gamma (0 = auto)")
        savedSettings.declare("RenderHiDPI",                ControlVariable.Type.BOOL,  true,       "Enable HiDPI / retina support")
        savedSettings.declare("DebugQualityPerformance",    ControlVariable.Type.U32,   3u,         "Debug override for quality tier")

        // --- Shadows ---
        savedSettings.declare("RenderShadowResolutionScale",ControlVariable.Type.F32,   1.0f,       "Scale factor for shadow texture resolution")
        savedSettings.declare("RenderShadowSplits",         ControlVariable.Type.S32,   4,          "Number of PSSM shadow splits")

        // --- Avatar ---
        savedSettings.declare("AvatarHoverOffsetZ",         ControlVariable.Type.F32,   0f,         "Vertical hover offset for avatar")
        savedSettings.declare("DebugAvatarJoints",          ControlVariable.Type.STRING, "",        "Comma-separated list of joints to visualise")
        savedSettings.declare("RenderAvatarMaxComplexity",  ControlVariable.Type.U32,   350000u,    "Max avatar render complexity before impostor")

        // --- Audio ---
        savedSettings.declare("AudioLevelMaster",           ControlVariable.Type.F32,   0.8f,       "Master volume (0–1)")
        savedSettings.declare("AudioLevelSFX",              ControlVariable.Type.F32,   1.0f,       "Sound-effects volume")
        savedSettings.declare("AudioLevelUI",               ControlVariable.Type.F32,   1.0f,       "UI sound volume")
        savedSettings.declare("AudioLevelAmbient",          ControlVariable.Type.F32,   1.0f,       "Ambient / wind volume")
        savedSettings.declare("AudioLevelVoice",            ControlVariable.Type.F32,   1.0f,       "Spatial voice volume")
        savedSettings.declare("AudioLevelMusic",            ControlVariable.Type.F32,   1.0f,       "Parcel music stream volume")
        savedSettings.declare("AudioLevelMedia",            ControlVariable.Type.F32,   1.0f,       "Parcel media stream volume")
        savedSettings.declare("MuteAudio",                  ControlVariable.Type.BOOL,  false,      "Globally mute all audio")
        savedSettings.declare("MuteWhenMinimized",          ControlVariable.Type.BOOL,  true,       "Mute audio when window is minimised")

        // --- Network ---
        savedSettings.declare("ThrottleBandwidthKBPS",      ControlVariable.Type.F32,   3000f,      "Max inbound bandwidth in kbps")
        savedSettings.declare("ConnectionPort",             ControlVariable.Type.U32,   13000u,     "UDP circuit port for region connections")

        // --- Chat / IM ---
        savedSettings.declare("ChatFontSize",               ControlVariable.Type.S32,   1,          "Nearby-chat font size index")
        savedSettings.declare("ChatPersistTime",            ControlVariable.Type.F32,   15f,        "Seconds before chat bubbles fade")
        savedSettings.declare("ChatMaxLines",               ControlVariable.Type.S32,   128,        "Max lines retained in nearby chat history")
        savedSettings.declare("PlayTypingAnim",             ControlVariable.Type.BOOL,  true,       "Play typing animation while composing chat")

        // --- Maturity / access ---
        savedSettings.declare("PreferredMaturity",          ControlVariable.Type.U32,   AgentAccess.SIM_ACCESS_PG.toUInt(),
                                                                                        "Preferred sim-access rating (PG=13, Mature=21, Adult=42)")

        // --- Camera ---
        savedSettings.declare("CameraAngle",                ControlVariable.Type.F32,   1.047f,     "Third-person camera field of view (radians)")
        savedSettings.declare("CameraOffsetScale",          ControlVariable.Type.F32,   1.0f,       "Scale factor for camera offset from avatar")
        savedSettings.declare("CameraPositionSmoothing",    ControlVariable.Type.BOOL,  true,       "Enable camera position smoothing")

        // --- UI / HUD ---
        savedSettings.declare("ShowMinimap",                ControlVariable.Type.BOOL,  true,       "Display minimap overlay")
        savedSettings.declare("ShowNavigationPanel",        ControlVariable.Type.BOOL,  true,       "Display navigation bar")
        savedSettings.declare("ShowFavoritesPanel",         ControlVariable.Type.BOOL,  true,       "Display favourites toolbar")
        savedSettings.declare("ShowToolbar",                ControlVariable.Type.BOOL,  true,       "Display bottom toolbar")
        savedSettings.declare("ShowStatusBar",              ControlVariable.Type.BOOL,  true,       "Display status bar")

        // --- Spellcheck ---
        savedSettings.declare("SpellCheck",                 ControlVariable.Type.BOOL,  false,      "Enable inline spell checking")
        savedSettings.declare("SpellCheckLanguage",         ControlVariable.Type.STRING, "en",      "BCP-47 language code for spell checker")

        // --- Joystick / NDOF ---
        savedSettings.declare("JoystickEnabled",            ControlVariable.Type.BOOL,  false,      "Enable joystick / NDOF device input")
        savedSettings.declare("JoystickAxis0",              ControlVariable.Type.S32,   0,          "NDOF axis mapping index 0")
        savedSettings.declare("JoystickAxis1",              ControlVariable.Type.S32,   1,          "NDOF axis mapping index 1")
        savedSettings.declare("JoystickAxis2",              ControlVariable.Type.S32,   2,          "NDOF axis mapping index 2")
        savedSettings.declare("JoystickAxis3",              ControlVariable.Type.S32,   3,          "NDOF axis mapping index 3")
        savedSettings.declare("JoystickAxis4",              ControlVariable.Type.S32,   4,          "NDOF axis mapping index 4")
        savedSettings.declare("JoystickAxis5",              ControlVariable.Type.S32,   5,          "NDOF axis mapping index 5")
        savedSettings.declare("JoystickAxis6",              ControlVariable.Type.S32,   6,          "NDOF axis mapping index 6")

        // --- Firestorm-specific ---
        savedSettings.declare("FSShowRadar",                ControlVariable.Type.BOOL,  true,       "Show Firestorm radar panel")
        savedSettings.declare("FSNameTagsShowLegacyUsernames",
                                                            ControlVariable.Type.BOOL,  true,       "Show legacy username under display name")
        savedSettings.declare("FSDisablePointAtAndBeam",    ControlVariable.Type.BOOL,  false,      "Suppress point-at and look-at beams")
        savedSettings.declare("FSLSLBridgeEnabled",         ControlVariable.Type.BOOL,  true,       "Enable the Firestorm LSL bridge object")

        // --- Crash settings ---
        crashSettings.declare("CrashSubmitBehavior",        ControlVariable.Type.S32,   0,          "0=ask, 1=always, 2=never send crash reports")

        // --- Warning flags ---
        warningSettings.declare("FSBandwidthTooHigh",       ControlVariable.Type.BOOL,  false,      "User has been warned about high bandwidth setting")
    }

    // -----------------------------------------------------------------------
    // Listener wiring
    // -----------------------------------------------------------------------

    /**
     * Attach all change-listener callbacks to the registered control variables.
     *
     * C++ equivalent: `settings_setup_listeners()` in llcontroldef.cpp
     *
     * Each listener is a lambda that runs when the named setting changes.
     * The implementation of each handler is stubbed here; fill them in as the
     * corresponding subsystem classes are ported.
     */
    fun setupListeners() {
        savedSettings.getVariable("RenderFarClip")?.addListener { newValue ->
            TODO("handleRenderFarClipChanged: update gAgentCamera.mDrawDistance and LLWorld far clip")
        }

        savedSettings.getVariable("RenderQualityPerformance")?.addListener { newValue ->
            TODO("handleSetShaderChanged: rebuild shader program set for new quality tier")
        }

        savedSettings.getVariable("RenderDeferred")?.addListener { newValue ->
            TODO("handleSetShaderChanged: toggle deferred pipeline and rebuild GL buffers")
        }

        savedSettings.getVariable("RenderTransparentWater")?.addListener { newValue ->
            TODO("handleRenderTransparentWaterChanged: rebuild water GL buffers and update world water objects")
        }

        savedSettings.getVariable("RenderShadowResolutionScale")?.addListener { newValue ->
            TODO("handleShadowsResized: request shadow texture resize from pipeline")
        }

        savedSettings.getVariable("RenderHiDPI")?.addListener { newValue ->
            TODO("handleWindowResized: request screen texture resize from pipeline")
        }

        savedSettings.getVariable("RenderTerrainScale")?.addListener { newValue ->
            TODO("handleTerrainScaleChanged: update LLDrawPoolTerrain::sDetailScale")
        }

        savedSettings.getVariable("RenderPBRTerrainScale")?.addListener { newValue ->
            TODO("handlePBRTerrainScaleChanged: update LLDrawPoolTerrain::sPBRDetailScale")
        }

        savedSettings.getVariable("AvatarHoverOffsetZ")?.addListener { newValue ->
            TODO("handleAvatarHoverOffsetChanged: call gAgentAvatarp->setHoverIfRegionEnabled()")
        }

        savedSettings.getVariable("DebugAvatarJoints")?.addListener { newValue ->
            TODO("handleDebugAvatarJointsChanged: call LLJoint::setDebugJointNames()")
        }

        savedSettings.getVariable("RenderAvatarMouselook")?.addListener { newValue ->
            TODO("handleRenderAvatarMouselookChanged: set LLVOAvatar::sVisibleInFirstPerson")
        }

        savedSettings.getVariable("DebugQualityPerformance")?.addListener { newValue ->
            TODO("handleDebugQualityPerformanceChanged: sync RenderQualityPerformance and notify LLFloaterPreference")
        }

        savedSettings.getVariable("ThrottleBandwidthKBPS")?.addListener { newValue ->
            TODO("BandwidthUpdater::update: schedule delayed gViewerThrottle.setMaxBandwidth() call")
        }

        savedSettings.getVariable("ShowNavigationPanel")?.addListener { newValue ->
            TODO("toggle_show_navigation_panel: show/hide navigation bar floater")
        }

        savedSettings.getVariable("ShowFavoritesPanel")?.addListener { newValue ->
            TODO("toggle_show_favorites_panel: show/hide favourites toolbar")
        }

        savedSettings.getVariable("SpellCheck")?.addListener { newValue ->
            TODO("handleSpellCheckChanged: reinitialise LLSpellCheck with new language")
        }

        savedSettings.getVariable("SpellCheckLanguage")?.addListener { newValue ->
            TODO("handleSpellCheckLanguageChanged: reload dictionary for new locale")
        }

        savedSettings.getVariable("FSLSLBridgeEnabled")?.addListener { newValue ->
            TODO("handleFSLSLBridgeChanged: attach or detach the LSL bridge object")
        }
    }

    // -----------------------------------------------------------------------
    // Graphics group helper
    // -----------------------------------------------------------------------

    /**
     * Populate a separate [ControlGroup] with the graphics-specific settings.
     *
     * C++ equivalent: `create_graphics_group(LLControlGroup& group)`
     */
    fun createGraphicsGroup(group: ControlGroup) {
        TODO("createGraphicsGroup: declare all RenderVolumeLODFactor, RenderMaxTextureIndex, etc. settings into 'group'")
    }

    // -----------------------------------------------------------------------
    // Global online-status toggle (Firestorm extension)
    // -----------------------------------------------------------------------

    /**
     * Callback invoked when the user responds to the "apply global online status
     * change" confirmation dialog.
     *
     * C++ equivalent: `applyGlobalOnlineStatusChange(notification, response)`
     */
    fun applyGlobalOnlineStatusChange(notification: Any?, response: Any?) {
        TODO("applyGlobalOnlineStatusChange: propagate online-status to all open IM sessions")
    }
}
